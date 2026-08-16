Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Get-ItoevaSha256 {
    [CmdletBinding()]
    param([Parameter(Mandatory)][string]$Path)

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "Datei nicht gefunden: $Path"
    }
    return (Get-FileHash -Algorithm SHA256 -LiteralPath $Path).Hash.ToLowerInvariant()
}

function Test-ItoevaBranchName {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$Branch,
        [Parameter(Mandatory)]$Config
    )

    if ($Branch.Length -gt [int]$Config.repository.maximumBranchLength) { return $false }
    if ($Branch -notmatch [string]$Config.repository.branchPattern) { return $false }
    & git check-ref-format --branch $Branch *> $null
    return $LASTEXITCODE -eq 0
}

function Assert-ItoevaAllowedPaths {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string[]]$Paths,
        [Parameter(Mandatory)]$Config
    )

    foreach ($rawPath in $Paths) {
        if ([string]::IsNullOrWhiteSpace($rawPath) -or [IO.Path]::IsPathRooted($rawPath)) {
            throw "Unsicherer oder leerer Pfad: $rawPath"
        }
        $path = $rawPath.Replace('\', '/')
        $segments = @($path.Split('/'))
        if ($path.StartsWith('/') -or $path.Contains(':') -or
            $segments.Count -eq 0 -or
            ($segments | Where-Object { $_ -in @('', '.', '..') })) {
            throw "Pfad muss strikt repository-relativ sein: $rawPath"
        }
        foreach ($prefix in $Config.scope.forbiddenPathPrefixes) {
            if ($path.StartsWith([string]$prefix, [StringComparison]::OrdinalIgnoreCase)) {
                throw "Pfad liegt ausserhalb des autonomen Scopes: $path"
            }
        }
        $name = [IO.Path]::GetFileName($path)
        if ($Config.scope.forbiddenFileNames -contains $name) {
            throw "Datei liegt ausserhalb des autonomen Scopes: $path"
        }
        if ($Config.scope.forbiddenExtensions -contains [IO.Path]::GetExtension($name).ToLowerInvariant()) {
            throw "Ausführbare oder infrastrukturelle Datei ist verboten: $path"
        }
        if ($name -match '(?i)(keystore|\.jks$|\.keystore$|google-services\.json$)') {
            throw "Secret-/Produktionspfad ist verboten: $path"
        }
    }
}

function New-ItoevaPushArguments {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$Branch,
        [Parameter(Mandatory)][string]$CommitSha,
        [Parameter(Mandatory)]$Config
    )

    if (-not (Test-ItoevaBranchName -Branch $Branch -Config $Config)) {
        throw "Ungueltiger Evolution-Branch: $Branch"
    }
    $ref = "refs/heads/$Branch"
    if (-not $ref.StartsWith([string]$Config.publication.allowedRefPrefix, [StringComparison]::Ordinal)) {
        throw "Push-Ziel ist nicht erlaubt: $ref"
    }
    if ($CommitSha -notmatch '^[0-9a-f]{40,64}$') { throw 'Commit-SHA ist ungültig.' }
    return @('push', '--porcelain', [string]$Config.publication.allowedRemote, "$CommitSha`:$ref")
}

function Test-ItoevaGate {
    [CmdletBinding()]
    param([Parameter(Mandatory)]$Gate)

    foreach($requiredName in @('planReview','mandatoryTests','finalReview','claudeFinalReview','diffCheck','baseUnchanged','baseSha','proposedTreeOid','testManifestHash','planHash','reviewBaseSha','reviewPlanHash','reviewTreeOid','reviewTestManifestHash','claudeReviewBaseSha','claudeReviewTreeOid','claudeReviewTestManifestHash','claudeReviewContextHash')){if(-not($Gate.PSObject.Properties.Name -contains $requiredName)){return $false}}
    $requiredPasses = @(
        $Gate.planReview,
        $Gate.mandatoryTests,
        $Gate.finalReview,
        $Gate.claudeFinalReview,
        $Gate.diffCheck
    )
    if ($requiredPasses | Where-Object { $_ -ne 'PASS' }) { return $false }
    if (-not $Gate.baseUnchanged) { return $false }
    if ([string]$Gate.baseSha -notmatch '^[0-9a-f]{40,64}$') { return $false }
    if ([string]$Gate.proposedTreeOid -notmatch '^[0-9a-f]{40,64}$') { return $false }
    if ([string]$Gate.testManifestHash -notmatch '^[0-9a-f]{64}$') { return $false }
    if ([string]$Gate.planHash -notmatch '^[0-9a-f]{64}$') { return $false }
    if ($Gate.reviewBaseSha -ne $Gate.baseSha) { return $false }
    if ($Gate.reviewPlanHash -ne $Gate.planHash) { return $false }
    if ($Gate.reviewTreeOid -ne $Gate.proposedTreeOid) { return $false }
    if ($Gate.reviewTestManifestHash -ne $Gate.testManifestHash) { return $false }
    if ($Gate.claudeReviewBaseSha -ne $Gate.baseSha) { return $false }
    if ($Gate.claudeReviewTreeOid -ne $Gate.proposedTreeOid) { return $false }
    if ($Gate.claudeReviewTestManifestHash -ne $Gate.testManifestHash) { return $false }
    if ([string]$Gate.claudeReviewContextHash -notmatch '^[0-9a-f]{64}$') { return $false }
    return $true
}

function Write-ItoevaAtomicJson {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)]$Value,
        [Parameter(Mandatory)][string]$Path
    )

    $directory = Split-Path -Parent $Path
    if (-not (Test-Path -LiteralPath $directory)) {
        New-Item -ItemType Directory -Path $directory -Force | Out-Null
    }
    $temporary = "$Path.$([Guid]::NewGuid().ToString('N')).tmp"
    try {
        $json = $Value | ConvertTo-Json -Depth 20
        [IO.File]::WriteAllText($temporary, $json, [Text.UTF8Encoding]::new($false))
        Move-Item -LiteralPath $temporary -Destination $Path -Force
    } finally {
        if (Test-Path -LiteralPath $temporary) { Remove-Item -LiteralPath $temporary -Force }
    }
}

function Enter-ItoevaRunLock {
    [CmdletBinding()]
    param([Parameter(Mandatory)][string]$Path)

    $directory = Split-Path -Parent $Path
    if (-not (Test-Path -LiteralPath $directory)) {
        New-Item -ItemType Directory -Path $directory -Force | Out-Null
    }
    try {
        $stream = [IO.File]::Open($Path, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::None)
        $payload = [Text.Encoding]::UTF8.GetBytes("$PID`n$([DateTimeOffset]::UtcNow.ToString('O'))")
        $stream.Write($payload, 0, $payload.Length)
        $stream.Flush()
        return $stream
    } catch [IO.IOException] {
        throw "Ein Itoeva-Evolution-Lauf ist bereits gesperrt: $Path"
    }
}

function Exit-ItoevaRunLock {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)]$Handle,
        [Parameter(Mandatory)][string]$Path
    )
    $Handle.Dispose()
    if (Test-Path -LiteralPath $Path) { Remove-Item -LiteralPath $Path -Force }
}

function Get-ItoevaDangerousGitConfig {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$Repository,
        [Parameter(Mandatory)]$Config
    )

    $patterns = @(
        '^core\.hookspath$', '^core\.sshcommand$', '^http\..*proxy$',
        '^url\..*\.insteadof$', '^remote\..*\.pushurl$',
        '^include\.path$', '^includeif\..*\.path$',
        '^filter\..*\.(clean|smudge|process)$'
    )
    $lines = & git -C $Repository config --show-origin --get-regexp '.*' 2>$null
    if ($LASTEXITCODE -notin @(0, 1)) { throw 'Git-Konfiguration konnte nicht gelesen werden.' }
    $findings = @()
    foreach ($line in $lines) {
        $parts = $line -split '\s+', 3
        if ($parts.Count -lt 2) { continue }
        $key = $parts[1].ToLowerInvariant()
        if ($patterns | Where-Object { $key -match $_ }) { $findings += $line }
    }
    $helpers = @(& git -C $Repository config --get-all credential.helper 2>$null)
    if ($LASTEXITCODE -notin @(0, 1)) { throw 'Credential Helper konnte nicht gelesen werden.' }
    foreach ($helper in $helpers) {
        if ($helper -and $Config.git.allowedCredentialHelpers -notcontains $helper.Trim()) {
            $findings += "credential.helper $helper"
        }
    }
    return $findings
}

function Get-ItoevaRemoteSha {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$Repository,
        [string]$Remote = 'origin',
        [string]$Ref = 'refs/heads/main'
    )
    $line = @(& git -C $Repository ls-remote --heads $Remote $Ref)
    if ($LASTEXITCODE -ne 0) { throw "Remote-Ref konnte nicht gelesen werden: $Remote $Ref" }
    if ($line.Count -eq 0) { return $null }
    if ($line.Count -ne 1) { throw "Remote-Ref ist nicht eindeutig: $Ref" }
    $sha = ($line[0] -split '\s+')[0]
    if ($sha -notmatch '^[0-9a-f]{40,64}$') { throw "Ungültiger Remote-SHA: $sha" }
    return $sha
}

function Assert-ItoevaBaseUnchanged {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$Repository,
        [Parameter(Mandatory)][string]$BaseSha
    )
    $actual = Get-ItoevaRemoteSha -Repository $Repository -Ref 'refs/heads/main'
    if ($actual -ne $BaseSha) { throw "origin/main hat sich verändert: base=$BaseSha actual=$actual" }
    return $actual
}

function Get-ItoevaChangedPaths {
    [CmdletBinding()]
    param([Parameter(Mandatory)][string]$Repository)

    $tracked = @(& git -C $Repository diff --name-only --diff-filter=ACDMRTUXB HEAD)
    if ($LASTEXITCODE -ne 0) { throw 'Tracked Diff konnte nicht gelesen werden.' }
    $untracked = @(& git -C $Repository ls-files --others --exclude-standard)
    if ($LASTEXITCODE -ne 0) { throw 'Untracked Files konnten nicht gelesen werden.' }
    return @($tracked + $untracked | Where-Object { $_ } | Sort-Object -Unique)
}

function Get-ItoevaSelectedTests {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)]$Config,
        [string[]]$ChangedPaths = @()
    )
    $selected = @($Config.tests.mandatory)
    foreach ($androidTest in $Config.tests.android) {
        $matches = $false
        foreach ($prefix in $androidTest.pathPrefixes) {
            if ($ChangedPaths | Where-Object { $_.Replace('\','/').StartsWith([string]$prefix) }) { $matches = $true; break }
        }
        if ($matches) { $selected += $androidTest }
    }
    return $selected
}

function ConvertTo-ItoevaWindowsCommandLineArgument {
    [CmdletBinding()]
    param([AllowEmptyString()][Parameter(Mandatory)][string]$Value)

    if ($Value.Length -gt 0 -and $Value -notmatch '[\s"]') { return $Value }
    $quoted = [Text.StringBuilder]::new()
    [void]$quoted.Append('"')
    $backslashes = 0
    foreach ($character in $Value.ToCharArray()) {
        if ($character -eq '\') { $backslashes++; continue }
        if ($character -eq '"') {
            [void]$quoted.Append(('\' * (2 * $backslashes + 1)))
            [void]$quoted.Append('"')
            $backslashes = 0
            continue
        }
        if ($backslashes) { [void]$quoted.Append(('\' * $backslashes)); $backslashes = 0 }
        [void]$quoted.Append($character)
    }
    if ($backslashes) { [void]$quoted.Append(('\' * (2 * $backslashes))) }
    [void]$quoted.Append('"')
    return $quoted.ToString()
}

function New-ItoevaCmdShimCommand {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$BatchPath,
        [string[]]$Arguments = @()
    )

    $values = @($BatchPath) + @($Arguments)
    foreach ($value in $values) {
        if ([string]::IsNullOrWhiteSpace($value) -or $value -match '["%!^&|<>\r\n]') {
            throw "Wert kann nicht sicher an den codex.cmd-Fallback uebergeben werden: $value"
        }
    }
    return (@($values | ForEach-Object { '"' + $_ + '"' }) -join ' ')
}

function Invoke-ItoevaProcessWithTimeout {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$Executable,
        [string[]]$Arguments = @(),
        [Parameter(Mandatory)][string]$WorkingDirectory,
        [Parameter(Mandatory)][int]$TimeoutSeconds,
        [string]$StandardInputPath,
        [string]$ValidatedWindowsArgumentString
    )
    $stdout = Join-Path ([IO.Path]::GetTempPath()) "itoeva-out-$([Guid]::NewGuid().ToString('N')).log"
    $stderr = Join-Path ([IO.Path]::GetTempPath()) "itoeva-err-$([Guid]::NewGuid().ToString('N')).log"
    try {
        $processArguments = if ($ValidatedWindowsArgumentString) {
            if ($env:OS -ne 'Windows_NT') { throw 'Vorvalidierte Windows-Argumente sind nur unter Windows erlaubt.' }
            $ValidatedWindowsArgumentString
        } elseif ($env:OS -eq 'Windows_NT') {
            (@($Arguments | ForEach-Object { ConvertTo-ItoevaWindowsCommandLineArgument ([string]$_) }) -join ' ')
        } else { $Arguments }
        $start = @{
            FilePath=$Executable; ArgumentList=$processArguments; WorkingDirectory=$WorkingDirectory
            RedirectStandardOutput=$stdout; RedirectStandardError=$stderr; WindowStyle='Hidden'; PassThru=$true
        }
        if ($StandardInputPath) { $start.RedirectStandardInput = $StandardInputPath }
        $process = Start-Process @start
        # Materialize the native handle before a very short-lived process can exit;
        # otherwise Windows PowerShell 5.1 may expose an empty ExitCode.
        $null = $process.Handle
        $finished = $process.WaitForExit($TimeoutSeconds * 1000)
        if (-not $finished) {
            & taskkill.exe /PID $process.Id /T /F 2>$null | Out-Null
            $process.WaitForExit()
        } else {
            $process.WaitForExit()
        }
        $process.Refresh()
        $output = @()
        if (Test-Path $stdout) { $output += Get-Content -LiteralPath $stdout }
        if (Test-Path $stderr) { $output += Get-Content -LiteralPath $stderr }
        return [pscustomobject]@{ exitCode=if ($finished) {$process.ExitCode} else {-1}; timedOut=(-not $finished); output=($output -join "`n") }
    } finally {
        Remove-Item -LiteralPath $stdout,$stderr -Force -ErrorAction SilentlyContinue
    }
}

function Resolve-ItoevaCodexLauncher {
    [CmdletBinding()]
    param()

    if ($env:OS -eq 'Windows_NT') {
        $native = Get-Command codex.exe -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($native) {
            return [pscustomobject]@{ executable=$native.Source; prefixArguments=@(); kind='NATIVE_EXE' }
        }

        $batch = Get-Command codex.cmd -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($batch) {
            $commandProcessor = (Get-Command cmd.exe -CommandType Application -ErrorAction Stop | Select-Object -First 1).Source
            return [pscustomobject]@{
                executable=$commandProcessor
                batchPath=$batch.Source
                prefixArguments=@()
                kind='CMD_SHIM'
            }
        }

        throw 'Weder eine native codex.exe noch ein ueber cmd.exe startbarer codex.cmd-Launcher wurde gefunden.'
    }

    $command = Get-Command codex -ErrorAction Stop
    return [pscustomobject]@{ executable=$command.Source; prefixArguments=@(); kind='DIRECT' }
}

$script:ItoevaClaudePinnedCliVersion='2.1.232'
$script:ItoevaClaudeDefaultModel='claude-sonnet-5'
$script:ItoevaClaudeHighRiskModel='claude-opus-5'

function ConvertTo-ItoevaNormalizedVersion {
    [CmdletBinding()]param([Parameter(Mandatory)][string]$Value)
    $parsed=[version]$Value
    return [version]::new($parsed.Major,[Math]::Max($parsed.Minor,0),[Math]::Max($parsed.Build,0),[Math]::Max($parsed.Revision,0))
}

# Reine Offline-Pruefung der eingecheckten Pins. Beruehrt bewusst keine installierte CLI,
# damit PublishDryRun ohne Claude-Binary auskommt.
function Assert-ItoevaClaudeConfigPins {
    [CmdletBinding()]param([Parameter(Mandatory)]$Config)
    if(-not($Config.PSObject.Properties.Name -contains 'claudeReview')){throw 'Claude-Reviewkonfiguration fehlt.'}
    $section=$Config.claudeReview
    foreach($name in @('enabled','defaultModel','highRiskModel','pinnedCliVersion','timeoutSeconds')){if(-not($section.PSObject.Properties.Name -contains $name)){throw "Claude-Reviewkonfiguration ist unvollstaendig: $name"}}
    if(-not $section.enabled){throw 'Claude-Review ist nicht aktiviert.'}
    if([string]$section.defaultModel -cne $script:ItoevaClaudeDefaultModel -or [string]$section.highRiskModel -cne $script:ItoevaClaudeHighRiskModel){throw 'Claude-v0.1-Modellpins sind nicht freigegeben.'}
    if([string]$section.pinnedCliVersion -cne $script:ItoevaClaudePinnedCliVersion){throw 'Claude-v0.1-CLI-Versionspin ist nicht freigegeben.'}
    return [pscustomobject]@{defaultModel=$script:ItoevaClaudeDefaultModel;highRiskModel=$script:ItoevaClaudeHighRiskModel;pinnedCliVersion=$script:ItoevaClaudePinnedCliVersion}
}

function Resolve-ItoevaClaudeLauncher {
    [CmdletBinding()] param([string]$ExpectedVersion=$script:ItoevaClaudePinnedCliVersion)
    if($env:OS -ne 'Windows_NT'){throw 'Claude-Final-Review ist ausschliesslich unter Windows mit gepinnter nativer CLI freigegeben.'}
    $native=Get-Command claude.exe -CommandType Application -ErrorAction SilentlyContinue|Select-Object -First 1
    if($native){$path=$native.Source}
    else{
        $batch=Get-Command claude.cmd -CommandType Application -ErrorAction SilentlyContinue|Select-Object -First 1
        if(-not $batch){throw 'Keine native Claude-Code-EXE wurde gefunden.'}
        $launcherLines=@(Get-Content -LiteralPath $batch.Source|Where-Object{$_ -match '^"%dp0%\\node_modules\\@anthropic-ai\\claude-code\\bin\\claude\.exe"\s+%\*$'})
        if($launcherLines.Count -ne 1){throw 'claude.cmd entspricht nicht dem erwarteten fail-closed npm-Launcherformat.'}
        $path=Join-Path (Split-Path -Parent $batch.Source) 'node_modules\@anthropic-ai\claude-code\bin\claude.exe'
    }
    if(-not(Test-Path -LiteralPath $path -PathType Leaf)){throw 'Native Claude-Code-EXE fehlt.'}
    $fileVersion=[string](Get-Item -LiteralPath $path).VersionInfo.FileVersion
    if([string]::IsNullOrWhiteSpace($fileVersion)){throw 'Claude-Code-EXE meldet keine Dateiversion.'}
    $actual=ConvertTo-ItoevaNormalizedVersion $fileVersion;$expected=ConvertTo-ItoevaNormalizedVersion $ExpectedVersion
    if($actual -ne $expected){throw "Nicht freigegebene Claude-Code-Version: $actual (gepinnt: $expected)"}
    return [pscustomobject]@{executable=$path;prefixArguments=@();kind='NATIVE_EXE';version=$actual.ToString()}
}

# Nur fuer echte Claude-Aufrufe (DryRun/Run/Revalidate). PublishDryRun ruft das bewusst nie auf.
function Assert-ItoevaClaudeCapabilityPreflight {
    [CmdletBinding()]param([Parameter(Mandatory)]$Config)
    $pins=Assert-ItoevaClaudeConfigPins $Config
    $launcher=Resolve-ItoevaClaudeLauncher -ExpectedVersion $pins.pinnedCliVersion
    if($launcher.kind -cne 'NATIVE_EXE' -or (ConvertTo-ItoevaNormalizedVersion $launcher.version) -ne (ConvertTo-ItoevaNormalizedVersion $pins.pinnedCliVersion)){throw 'Claude-v0.1-Capabilities sind nicht durch die freigegebene native CLI-Version belegt.'}
    return $launcher
}

# Offline-Provenienzpruefung gespeicherter Evidence: vergleicht die im Kontext festgehaltene
# Modell-/CLI-Herkunft gegen die eingecheckten Pins, nie gegen eine installierte Binary.
function Assert-ItoevaClaudeStoredProvenance {
    [CmdletBinding()]param([Parameter(Mandatory)]$Context,[Parameter(Mandatory)][string]$ReportModel,[Parameter(Mandatory)]$Pins)
    foreach($name in @('reviewModel','claudeCliVersion','claudeLauncherKind')){if(-not($Context.PSObject.Properties.Name -contains $name)){throw "Claude-Kontext enthaelt keine Modell-/CLI-Provenienz: $name"}}
    if([string]$Context.reviewModel -cne $ReportModel){throw 'Claude-Kontextmodell weicht vom Report ab.'}
    if([string]$Context.reviewModel -cne $Pins.defaultModel -and [string]$Context.reviewModel -cne $Pins.highRiskModel){throw 'Claude-Kontextmodell ist nicht freigegeben.'}
    if([string]$Context.claudeLauncherKind -cne 'NATIVE_EXE'){throw 'Claude-Provenienz weist keine gepinnte native CLI aus.'}
    if((ConvertTo-ItoevaNormalizedVersion ([string]$Context.claudeCliVersion)) -ne (ConvertTo-ItoevaNormalizedVersion $Pins.pinnedCliVersion)){throw 'Claude-Provenienz weist eine nicht freigegebene CLI-Version aus.'}
}

function ConvertTo-ItoevaRedactedDiagnosticText {
    [CmdletBinding()]param([Parameter(Mandatory)][AllowEmptyString()][AllowNull()][string]$Text,[int]$MaximumLength=8192)
    if([string]::IsNullOrEmpty($Text)){return ''}
    $value=$Text
    $value=[regex]::Replace($value,'(?i)\bsk-[A-Za-z0-9_\-]{8,}','[REDACTED]')
    $value=[regex]::Replace($value,'(?i)\b(bearer|basic)\s+[A-Za-z0-9_\-\.=+/]{8,}','${1} [REDACTED]')
    $value=[regex]::Replace($value,'\beyJ[A-Za-z0-9_\-]{6,}\.[A-Za-z0-9_\-]{6,}(?:\.[A-Za-z0-9_\-]+)?','[REDACTED]')
    $value=[regex]::Replace($value,'(?i)("?\b(?:api[_\-]?key|auth[_\-]?token|access[_\-]?token|refresh[_\-]?token|oauth[_\-]?token|id[_\-]?token|session[_\-]?key|client[_\-]?secret|secret|password|passwd|credentials?)\b"?\s*[:=]\s*"?)([^"\s,;}]{4,})','${1}[REDACTED]')
    # Lange undurchsichtige Tokens redigieren, reine Hex-SHAs/OIDs aber als Diagnosewert erhalten.
    $value=[regex]::Replace($value,'(?<![0-9A-Za-z_\-])(?![0-9a-fA-F]{40,64}(?![0-9A-Za-z_\-]))[A-Za-z0-9_\-]{64,}(?![0-9A-Za-z_\-])','[REDACTED]')
    if($value.Length -gt $MaximumLength){$value=$value.Substring(0,$MaximumLength)+"`n[TRUNCATED]"}
    return $value
}

function New-ItoevaClaudeEnvelopeSummary {
    [CmdletBinding()]param([Parameter(Mandatory)][AllowEmptyString()][AllowNull()][string]$Stdout)
    $summary=[ordered]@{parsed=$false;type='';subtype='';isError='';durationMs='';numTurns='';permissionDenialCount='';resultKind='';resultLength=''}
    if([string]::IsNullOrWhiteSpace($Stdout)){return $summary}
    try{$envelope=$Stdout|ConvertFrom-Json}catch{return $summary}
    if($envelope -isnot [pscustomobject]){return $summary}
    $summary.parsed=$true;$names=@($envelope.PSObject.Properties.Name)
    foreach($pair in @(@('type','type'),@('subtype','subtype'),@('is_error','isError'),@('duration_ms','durationMs'),@('num_turns','numTurns'))){
        if($pair[0] -in $names){$summary[$pair[1]]=[string]$envelope.($pair[0])}
    }
    if('permission_denials' -in $names){$summary.permissionDenialCount=[string]@($envelope.permission_denials).Count}
    if('result' -in $names){
        $inner=$envelope.result
        $summary.resultKind=$(if($null -eq $inner){'null'}elseif($inner -is [string]){'string'}elseif($inner -is [pscustomobject]){'object'}else{$inner.GetType().Name})
        $summary.resultLength=[string]$(if($inner -is [string]){$inner.Length}elseif($null -eq $inner){0}else{($inner|ConvertTo-Json -Depth 20 -Compress).Length})
    }
    return $summary
}

# Wird bei jedem echten Claude-Aufruf geschrieben, bevor irgendeine semantische Pruefung
# fehlschlagen kann. Enthaelt ausschliesslich redigierte, laengenbegrenzte Auszuege.
function Write-ItoevaClaudeDiagnostics {
    [CmdletBinding()]param(
        [Parameter(Mandatory)][string]$RunRoot,[Parameter(Mandatory)][string]$Stage,
        [Parameter(Mandatory)][string]$Model,[Parameter(Mandatory)][AllowEmptyString()][string]$CliVersion,
        $Result,[AllowEmptyString()][string]$LaunchError=''
    )
    $hasResult=$null -ne $Result
    $stdout=$(if($hasResult){[string]$Result.stdout}else{''});$stderr=$(if($hasResult){[string]$Result.stderr}else{''})
    $diagnostics=[ordered]@{
        stage=$Stage;recordedAt=[DateTimeOffset]::UtcNow.ToString('O');model=$Model;claudeCliVersion=$CliVersion
        launched=$hasResult;launchError=(ConvertTo-ItoevaRedactedDiagnosticText $LaunchError 1024)
        exitCode=[string]$(if($hasResult){$Result.exitCode}else{''});timedOut=[string]$(if($hasResult){$Result.timedOut}else{''})
        stdoutLength=$stdout.Length;stderrLength=$stderr.Length
        stdoutTruncated=[string]$(if($hasResult -and ($Result.PSObject.Properties.Name -contains 'stdoutTruncated')){$Result.stdoutTruncated}else{''})
        stderrTruncated=[string]$(if($hasResult -and ($Result.PSObject.Properties.Name -contains 'stderrTruncated')){$Result.stderrTruncated}else{''})
        envelopeSummary=(New-ItoevaClaudeEnvelopeSummary $stdout)
        stdoutExcerpt=(ConvertTo-ItoevaRedactedDiagnosticText $stdout 8192)
        stderrExcerpt=(ConvertTo-ItoevaRedactedDiagnosticText $stderr 4096)
    }
    $path=Join-Path $RunRoot 'claude-final-review.diagnostics.json'
    Write-ItoevaAtomicJson $diagnostics $path;$hash=Get-ItoevaSha256 $path;Write-ItoevaAtomicText "$path.sha256" $hash
    return [pscustomobject]@{path=$path;hash=$hash}
}

function Get-ItoevaClaudeReviewModel {
    [CmdletBinding()]param([Parameter(Mandatory)]$Plan,[Parameter(Mandatory)]$Config)
    $pins=Assert-ItoevaClaudeConfigPins $Config
    $paths=@($Plan.paths|ForEach-Object{([string]$_).Replace('\','/').ToLowerInvariant()}|Sort-Object -Unique)
    $text=((@([string]$Plan.title)+@($Plan.plan|ForEach-Object{[string]$_})+$paths)-join "`n").ToLowerInvariant()
    $riskPattern='(?i)(data\s*integrity|migration|schema\s*change|database|\bsql\b|progression|\bxp\b|economy|currency|reward|auth(?:entication|orization)?|security|credential|permission|runner|publish(?:ing)?|architecture|architectural)'
    $components=@($paths|ForEach-Object{($_ -split '/')[0]}|Sort-Object -Unique)
    $highRisk=($text-match$riskPattern)-or@($paths|Where-Object{$_ -match '^(runner|migrations?|database|auth|security|economy|progression)/'}).Count-gt0-or$paths.Count-ge8-or$components.Count-ge4
    return $(if($highRisk){$pins.highRiskModel}else{$pins.defaultModel})
}

function Invoke-ItoevaProcessSeparated {
    [CmdletBinding()] param([Parameter(Mandatory)][string]$Executable,[string[]]$Arguments=@(),[Parameter(Mandatory)][string]$WorkingDirectory,[Parameter(Mandatory)][int]$TimeoutSeconds,[string]$StandardInputPath,[string]$ValidatedWindowsArgumentString)
    $processArguments=if($ValidatedWindowsArgumentString){$ValidatedWindowsArgumentString}else{@($Arguments|ForEach-Object{ConvertTo-ItoevaWindowsCommandLineArgument ([string]$_)})-join ' '}
    $start=[Diagnostics.ProcessStartInfo]::new();$start.FileName=$Executable;$start.Arguments=$processArguments;$start.WorkingDirectory=$WorkingDirectory;$start.UseShellExecute=$false;$start.CreateNoWindow=$true;$start.RedirectStandardOutput=$true;$start.RedirectStandardError=$true;$start.RedirectStandardInput=[bool]$StandardInputPath
    foreach($name in @($start.EnvironmentVariables.Keys)){if([string]$name-match'(?i)^(ANTHROPIC_|CLAUDE_|CLAUDECODE|AWS_|AMAZON_|GOOGLE_|CLOUDSDK_|AZURE_|FOUNDRY_|VERTEX_)'){[void]$start.EnvironmentVariables.Remove([string]$name)}}
    $process=[Diagnostics.Process]::new();$process.StartInfo=$start
    try{
        if(-not $process.Start()){throw 'Claude-Kindprozess konnte nicht gestartet werden.'}
        $stdoutTask=$process.StandardOutput.ReadToEndAsync();$stderrTask=$process.StandardError.ReadToEndAsync()
        if($StandardInputPath){$process.StandardInput.Write([IO.File]::ReadAllText($StandardInputPath));$process.StandardInput.Close()}
        $finished=$process.WaitForExit($TimeoutSeconds*1000)
        if(-not $finished){& taskkill.exe /PID $process.Id /T /F 2>$null|Out-Null;if(-not $process.WaitForExit(10000)){throw 'Claude-Prozessbaum endete nach taskkill nicht.'}}
        $stdout=$stdoutTask.GetAwaiter().GetResult();$stderr=$stderrTask.GetAwaiter().GetResult()
        # Groessenverletzungen werden hier gekappt und gemeldet statt geworfen, damit der Aufrufer
        # zuerst Diagnoseartefakte schreiben und danach fail-closed abbrechen kann.
        $stdoutTruncated=$false;$stderrTruncated=$false
        if($stdout.Length-gt1048576){$stdout=$stdout.Substring(0,1048576);$stdoutTruncated=$true}
        if($stderr.Length-gt262144){$stderr=$stderr.Substring(0,262144);$stderrTruncated=$true}
        return [pscustomobject]@{exitCode=if($finished){$process.ExitCode}else{-1};timedOut=(-not $finished);stdout=$stdout;stderr=$stderr;stdoutTruncated=$stdoutTruncated;stderrTruncated=$stderrTruncated}
    }finally{$process.Dispose()}
}

function ConvertFrom-ItoevaClaudeEnvelope {
    [CmdletBinding()] param([Parameter(Mandatory)][string]$Text)
    if($Text.Length -gt 1048576){throw 'Claude-Envelope überschreitet das Größenlimit.'}
    try{$envelope=$Text|ConvertFrom-Json}catch{throw 'Claude-Envelope ist kein gültiges JSON.'}
    if($envelope -isnot [pscustomobject] -or -not($envelope.PSObject.Properties.Name -contains 'result')){throw 'Claude-Envelope enthält kein result.'}
    if(($envelope.PSObject.Properties.Name -contains 'is_error') -and $envelope.is_error){throw 'Claude-Envelope meldet einen Fehler.'}
    if(($envelope.PSObject.Properties.Name -contains 'error') -and $null-ne $envelope.error){throw 'Claude-Envelope enthaelt einen Fehler.'}
    if(($envelope.PSObject.Properties.Name -contains 'permission_denials') -and @($envelope.permission_denials).Count){throw 'Claude-Envelope meldet verweigerte Berechtigungen.'}
    if(($envelope.PSObject.Properties.Name -contains 'subtype') -and [string]$envelope.subtype -match '(?i)error|limit|denied'){throw 'Claude-Envelope meldet keinen erfolgreichen Result-Typ.'}
    $inner=$envelope.result
    if($inner -is [string]){
        $candidate=$inner.Trim();$match=[regex]::Match($candidate,'\A```(?:json)?\s*\r?\n(?<json>[\s\S]*?)\r?\n```\z',[Text.RegularExpressions.RegexOptions]::IgnoreCase)
        if($candidate.StartsWith('```')){if(-not $match.Success){throw 'Claude-result enthält keine einzelne vollständige JSON-Codefence.'};$candidate=$match.Groups['json'].Value}
        try{$inner=$candidate|ConvertFrom-Json}catch{throw 'Claude-result ist kein gültiges JSON.'}
    }
    if($inner -isnot [pscustomobject]){throw 'Claude-result ist kein JSON-Objekt.'}
    return [pscustomobject]@{envelope=$envelope;review=$inner}
}

function Assert-ItoevaClaudeReview {
    [CmdletBinding()] param([Parameter(Mandatory)]$Review,[Parameter(Mandatory)][string]$BaseSha,[Parameter(Mandatory)][string]$TreeOid,[Parameter(Mandatory)][string]$TestManifestHash,[Parameter(Mandatory)][string]$ContextHash)
    $required=@('status','findings','baseSha','treeOid','testManifestHash','contextHash');foreach($name in $required){if(-not($Review.PSObject.Properties.Name -contains $name)){throw "Claude-Reviewfeld fehlt: $name"}}
    if(@($Review.PSObject.Properties.Name|Where-Object{$_ -notin $required}).Count){throw 'Claude-Review enthaelt unbekannte Felder.'}
    foreach($name in @('status','baseSha','treeOid','testManifestHash','contextHash')){if($Review.$name -isnot [string]){throw "Claude-Reviewfeld hat falschen Typ: $name"}}
    if($Review.findings -isnot [array]){throw 'Claude-findings ist kein Array.'}
    foreach($finding in @($Review.findings)){if($finding -isnot [pscustomobject]){throw 'Claude-Finding ist kein Objekt.'};$names=@($finding.PSObject.Properties.Name);if(@($names|Where-Object{$_ -notin @('severity','message','path')}).Count -or 'severity' -notin $names -or 'message' -notin $names -or $finding.severity -isnot [string] -or [string]$finding.severity -notin @('CRITICAL','HIGH','MEDIUM','LOW','UNVERIFIED') -or $finding.message -isnot [string] -or [string]::IsNullOrWhiteSpace($finding.message) -or ('path' -in $names -and $finding.path -isnot [string])){throw 'Claude-Finding ist strukturell ungueltig.'}}
    if([string]$Review.status -notin @('PASS','FAIL') -or [string]$Review.baseSha -cne $BaseSha -or [string]$Review.treeOid -cne $TreeOid -or [string]$Review.testManifestHash -cne $TestManifestHash -or [string]$Review.contextHash -cne $ContextHash){throw 'Claude-Review ist nicht an Base, Tree, Tests und Kontext gebunden.'}
    $findings=@($Review.findings);if([string]$Review.status -eq 'PASS' -and $findings.Count){throw 'Claude-PASS darf keine Findings enthalten.'}
    if([string]$Review.status -ne 'PASS'){throw 'Claude-Final-Review ist FAIL.'}
}

function Invoke-ItoevaClaudeFinalReview {
    [CmdletBinding()] param([Parameter(Mandatory)][string]$Repository,[Parameter(Mandatory)][string]$RunRoot,[Parameter(Mandatory)][string]$BaseSha,[Parameter(Mandatory)][string]$TreeOid,[Parameter(Mandatory)][string]$PlanHash,[Parameter(Mandatory)][string]$TestManifestHash,[Parameter(Mandatory)][string[]]$ChangedPaths,[Parameter(Mandatory)][string]$PlanPath,[Parameter(Mandatory)][string]$TestsPath,[Parameter(Mandatory)][string]$CodexReviewPath,[Parameter(Mandatory)][string]$Model,[Parameter(Mandatory)]$Config)
    $pins=Assert-ItoevaClaudeConfigPins $Config;$launcher=Assert-ItoevaClaudeCapabilityPreflight $Config
    $model=$Model;if($model -cne $pins.defaultModel -and $model -cne $pins.highRiskModel){throw 'Claude-Reviewmodell ist nicht explizit freigegeben.'}
    $headBefore=(& git -C $Repository rev-parse HEAD).Trim();$indexBefore=(& git -C $Repository write-tree).Trim();$originBefore=Get-ItoevaRemoteSha $Repository;$treeBefore=Get-ItoevaProposedTreeOid $Repository $BaseSha $ChangedPaths
    if($headBefore -ne $BaseSha -or $treeBefore -ne $TreeOid){throw 'Repositoryzustand stimmt vor Claude nicht mit Base und Proposed Tree ueberein.'}
    $bundle=Assert-ItoevaRuntimePath ([IO.Path]::GetFullPath([string]$Config.runtimeRoot)) (Join-Path $RunRoot 'claude-review-input');New-Item -ItemType Directory -Path $bundle -Force|Out-Null
    $copies=[ordered]@{plan=$PlanPath;tests=$TestsPath;codexReview=$CodexReviewPath};foreach($entry in $copies.GetEnumerator()){Copy-ItoevaHashedEvidence $entry.Value (Join-Path $bundle "$($entry.Key).json")|Out-Null}
    $patchPath=Join-Path $bundle 'proposed.patch';$patch=(& git -C $Repository diff-tree --binary --no-ext-diff --no-textconv --root $BaseSha $TreeOid -- @ChangedPaths|Out-String);if($LASTEXITCODE -ne 0){throw 'Claude-Review-Patch konnte nicht aus den gebundenen Trees erzeugt werden.'};Write-ItoevaAtomicText $patchPath $patch;$patchHash=Get-ItoevaSha256 $patchPath
    $context=[ordered]@{baseSha=$BaseSha;treeOid=$TreeOid;planHash=$PlanHash;testManifestHash=$TestManifestHash;changedPaths=@($ChangedPaths);patchSha256=$patchHash;planSha256=Get-ItoevaSha256 $PlanPath;testsSha256=Get-ItoevaSha256 $TestsPath;codexReviewSha256=Get-ItoevaSha256 $CodexReviewPath;reviewModel=$model;claudeCliVersion=$launcher.version;claudeLauncherKind=$launcher.kind}
    $contextPath=Join-Path $bundle 'context.json';Write-ItoevaAtomicJson $context $contextPath;$contextHash=Get-ItoevaSha256 $contextPath;Write-ItoevaAtomicText "$contextPath.sha256" $contextHash
    # Die CLI validiert --json-schema mit einem Validator, der die 2020-12-Meta-Schema-Referenz nicht
    # aufloest. Die Deklaration wird daher nur fuer das Argument entfernt; die Datei behaelt sie.
    $schemaPath=Join-Path $PSScriptRoot 'schemas\claude-review.schema.json';$schemaObject=Get-Content -Raw $schemaPath|ConvertFrom-Json
    if($schemaObject.PSObject.Properties.Name -contains '$schema'){$schemaObject.PSObject.Properties.Remove('$schema')}
    $schema=($schemaObject|ConvertTo-Json -Depth 20 -Compress);if($schema -match '\$schema'){throw 'Claude-Reviewschema enthaelt weiterhin eine nicht aufloesbare Meta-Schema-Referenz.'}
    $mcpPath=Join-Path $bundle 'empty-mcp.json';Write-ItoevaAtomicText $mcpPath '{"mcpServers":{}}'
    $promptPath=Join-Path $RunRoot 'claude-final-review.prompt.txt';$prompt=(Get-Content -Raw (Join-Path $PSScriptRoot 'prompts\review-final-claude.md'))+"`nContext: $contextPath`nBase: $BaseSha`nTree: $TreeOid`nTestmanifest-Hash: $TestManifestHash`nContext-Hash: $contextHash";Write-ItoevaAtomicText $promptPath $prompt
    $claudeArguments=@('-p','--model',$model,'--permission-mode','plan','--tools','Read,Glob,Grep','--safe-mode','--disable-slash-commands','--strict-mcp-config','--mcp-config',$mcpPath,'--no-chrome','--no-session-persistence','--output-format','json','--json-schema',$schema,'--add-dir',$bundle)
    $validated=$null;$processArgs=@($launcher.prefixArguments)+$claudeArguments
    # Genau ein Aufruf pro Run/Revalidate, kein Retry, kein Fallback.
    $result=$null;$launchError=''
    try{$result=Invoke-ItoevaProcessSeparated $launcher.executable $processArgs $Repository ([int]$Config.claudeReview.timeoutSeconds) $promptPath $validated}
    catch{$launchError=[string]$_.Exception.Message}
    Write-ItoevaClaudeDiagnostics -RunRoot $RunRoot -Stage 'CLAUDE_FINAL_REVIEW' -Model $model -CliVersion $launcher.version -Result $result -LaunchError $launchError|Out-Null
    if($launchError){throw "Claude-Final-Review konnte nicht ausgefuehrt werden: $launchError"}
    if($result.timedOut){throw 'Claude-Final-Review hat das Zeitlimit ueberschritten.'}
    if($result.stdoutTruncated -or $result.stderrTruncated){throw 'Claude-Ausgabe ueberschreitet das Groessenlimit.'}
    if($result.exitCode -ne 0){throw "Claude-Final-Review nicht verfuegbar (Account-/Rate-Limit, Authentifizierung, Modell oder CLI), Exitcode $($result.exitCode)."}
    $parsed=ConvertFrom-ItoevaClaudeEnvelope $result.stdout;$outputPath=Join-Path $RunRoot 'claude-final-review.json';Write-ItoevaAtomicJson $parsed.review $outputPath;Write-ItoevaAtomicText "$outputPath.sha256" (Get-ItoevaSha256 $outputPath)
    $envelopePath=Join-Path $RunRoot 'claude-final-review-envelope.json';Write-ItoevaAtomicJson $parsed.envelope $envelopePath;Write-ItoevaAtomicText "$envelopePath.sha256" (Get-ItoevaSha256 $envelopePath)
    Assert-ItoevaClaudeReview $parsed.review $BaseSha $TreeOid $TestManifestHash $contextHash
    foreach($path in @($contextPath,$patchPath,$PlanPath,$TestsPath,$CodexReviewPath)){
        if(-not(Test-Path $path)){throw 'Claude-Review-Evidence fehlt nach dem Aufruf.'}
    }
    if((Get-ItoevaSha256 $contextPath)-ne $contextHash -or (Get-ItoevaSha256 $patchPath)-ne $patchHash -or (Get-ItoevaSha256 $PlanPath)-ne $context.planSha256 -or (Get-ItoevaSha256 $TestsPath)-ne $context.testsSha256 -or (Get-ItoevaSha256 $CodexReviewPath)-ne $context.codexReviewSha256){throw 'Claude-Review-Evidence wurde waehrend des Aufrufs veraendert.'}
    $headAfter=(& git -C $Repository rev-parse HEAD).Trim();$indexAfter=(& git -C $Repository write-tree).Trim();$originAfter=Get-ItoevaRemoteSha $Repository;$treeAfter=Get-ItoevaProposedTreeOid $Repository $BaseSha $ChangedPaths
    if($headAfter-ne$headBefore -or $indexAfter-ne$indexBefore -or $originAfter-ne$originBefore -or $treeAfter-ne$TreeOid){throw 'Repository, Index, Base oder Proposed Tree wurde waehrend des Claude-Reviews veraendert.'}
    return [pscustomobject]@{status='PASS';model=$model;cliVersion=$launcher.version;reviewPath=$outputPath;reviewHash=Get-ItoevaSha256 $outputPath;contextPath=$contextPath;contextHash=$contextHash;patchPath=$patchPath;patchHash=$patchHash;bindings=$parsed.review}
}

function New-ItoevaCodexArguments {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$Repository,
        [Parameter(Mandatory)][string]$Sandbox,
        [Parameter(Mandatory)][string]$SchemaPath,
        [Parameter(Mandatory)][string]$OutputPath,
        [string]$SessionId
    )

    if ($SessionId) {
        return @('-a', 'never', 'exec', 'resume', '-c', "sandbox_mode=$Sandbox", $SessionId, '-', '--output-schema', $SchemaPath, '--json', '-o', $OutputPath)
    }
    return @('-a', 'never', 'exec', '-C', $Repository, '-s', $Sandbox, '--output-schema', $SchemaPath, '--json', '-o', $OutputPath, '-')
}

function Assert-ItoevaCandidateAnalysis {
    [CmdletBinding()]
    param([Parameter(Mandatory)]$Analysis)

    $candidateCount = @($Analysis.candidates).Count
    switch ([string]$Analysis.status) {
        'CANDIDATES' {
            if ($candidateCount -lt 1) { throw 'CANDIDATES erfordert mindestens einen Kandidaten.' }
        }
        'NO_SAFE_EVOLUTION' {
            if ($candidateCount -ne 0) { throw 'NO_SAFE_EVOLUTION erfordert eine leere Kandidatenliste.' }
        }
        default { throw "Unbekannter Analyse-Status: $($Analysis.status)" }
    }
}

function Format-ItoevaEvolutionNumber {
    [CmdletBinding()]
    param([AllowNull()][Parameter(Mandatory)]$Value)

    $text = if ($Value -is [IFormattable]) {
        $Value.ToString($null, [Globalization.CultureInfo]::InvariantCulture)
    } else {
        [string]$Value
    }
    [decimal]$number = 0
    $style = [Globalization.NumberStyles]::Float
    if (-not [decimal]::TryParse($text, $style, [Globalization.CultureInfo]::InvariantCulture, [ref]$number) -or
        $number -ne [decimal]::Truncate($number) -or $number -lt 1 -or $number -gt 999) {
        throw "Ungueltige Evolutionsnummer: $Value"
    }
    return ([int]$number).ToString('D3', [Globalization.CultureInfo]::InvariantCulture)
}

function Test-ItoevaRunId {
    [CmdletBinding()]
    param([AllowEmptyString()][Parameter(Mandatory)][string]$RunId)
    return $RunId -cmatch '^[0-9a-f]{32}$'
}

function Get-ItoevaBytesSha256 {
    [CmdletBinding()]
    param([Parameter(Mandatory)][byte[]]$Bytes)
    $sha = [Security.Cryptography.SHA256]::Create()
    try { return ([BitConverter]::ToString($sha.ComputeHash($Bytes))).Replace('-', '').ToLowerInvariant() }
    finally { $sha.Dispose() }
}

function Read-ItoevaHashedJson {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][string]$HashPath
    )
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf) -or -not (Test-Path -LiteralPath $HashPath -PathType Leaf)) {
        throw "Hashgebundenes JSON-Artefakt fehlt: $Path"
    }
    $expected = (Get-Content -Raw -LiteralPath $HashPath).Trim().ToLowerInvariant()
    if ($expected -notmatch '^[0-9a-f]{64}$' -or (Get-ItoevaSha256 $Path) -ne $expected) {
        throw "SHA-256 stimmt nicht: $Path"
    }
    $value = Get-Content -Raw -LiteralPath $Path | ConvertFrom-Json
    return [pscustomobject]@{ value=$value; hash=$expected }
}

function Initialize-ItoevaEvidenceSnapshot {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$SourcePath,
        [Parameter(Mandatory)][string]$SnapshotPath,
        [bool]$RequireSourceMatch = $true
    )
    if (-not (Test-Path -LiteralPath $SourcePath -PathType Leaf)) { throw "Evidence-Artefakt fehlt: $SourcePath" }
    $sourceHash=Get-ItoevaSha256 $SourcePath; $hashPath="$SnapshotPath.sha256"
    if (Test-Path -LiteralPath $SnapshotPath) {
        $snapshot=Read-ItoevaHashedJson $SnapshotPath $hashPath
        if ($RequireSourceMatch -and $snapshot.hash -ne $sourceHash) { throw "Evidence-Artefakt wurde nach PREPARED veraendert: $SourcePath" }
        return $snapshot
    }
    $temporary=Join-Path (Split-Path -Parent $SnapshotPath) "e.$([Guid]::NewGuid().ToString('N'))"
    try {
        [IO.File]::WriteAllBytes($temporary,[IO.File]::ReadAllBytes($SourcePath))
        Move-Item -LiteralPath $temporary -Destination $SnapshotPath
    } finally { if(Test-Path -LiteralPath $temporary){Remove-Item -LiteralPath $temporary -Force} }
    Write-ItoevaAtomicText $hashPath $sourceHash
    return Read-ItoevaHashedJson $SnapshotPath $hashPath
}

function Assert-ItoevaRuntimePath {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$RuntimeRoot,
        [Parameter(Mandatory)][string]$Path
    )
    $root = [IO.Path]::GetFullPath($RuntimeRoot).TrimEnd('\') + '\'
    $full = [IO.Path]::GetFullPath($Path)
    if (-not $full.StartsWith($root, [StringComparison]::OrdinalIgnoreCase)) { throw "Runtime-Pfad liegt ausserhalb des Runtime-Roots: $full" }
    if (Test-Path -LiteralPath $full) {
        $target = Get-Item -LiteralPath $full -Force
        if ($target.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw "Reparse-Point im Runtime-Pfad ist nicht erlaubt: $full" }
    }
    $cursor = Split-Path -Parent $full
    while ($cursor -and $cursor.StartsWith($root.TrimEnd('\'), [StringComparison]::OrdinalIgnoreCase)) {
        if (Test-Path -LiteralPath $cursor) {
            $item = Get-Item -LiteralPath $cursor -Force
            if ($item.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw "Reparse-Point im Runtime-Pfad ist nicht erlaubt: $cursor" }
        }
        if ($cursor.TrimEnd('\') -eq $root.TrimEnd('\')) { break }
        $cursor = Split-Path -Parent $cursor
    }
    return $full
}

function Write-ItoevaPublishJournal {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$JournalRoot,
        [Parameter(Mandatory)][ValidateSet('PREPARED','COMMITTED','PUSHED','REPORTED')][string]$Phase,
        [Parameter(Mandatory)]$Record
    )
    New-Item -ItemType Directory -Path $JournalRoot -Force | Out-Null
    $json = $Record | ConvertTo-Json -Depth 20 -Compress
    $bytes = [Text.UTF8Encoding]::new($false).GetBytes($json)
    $hash = Get-ItoevaBytesSha256 $bytes
    $rank = @{ PREPARED=1; COMMITTED=2; PUSHED=3; REPORTED=4 }
    $path = Join-Path $JournalRoot "p.$($rank[$Phase]).$hash.json"
    if (Test-Path -LiteralPath $path) {
        if ((Get-ItoevaSha256 $path) -ne $hash) { throw "Publish-Journal ist beschaedigt: $path" }
        return [pscustomobject]@{ path=$path; phase=$Phase; hash=$hash; record=(Get-Content -Raw -LiteralPath $path | ConvertFrom-Json) }
    }
    $temporary = Join-Path $JournalRoot "t.$([Guid]::NewGuid().ToString('N'))"
    try {
        [IO.File]::WriteAllBytes($temporary, $bytes)
        Move-Item -LiteralPath $temporary -Destination $path
    } finally {
        if (Test-Path -LiteralPath $temporary) { Remove-Item -LiteralPath $temporary -Force }
    }
    return [pscustomobject]@{ path=$path; phase=$Phase; hash=$hash; record=$Record }
}

function Get-ItoevaPublishJournal {
    [CmdletBinding()]
    param([Parameter(Mandatory)][string]$JournalRoot)
    if (-not (Test-Path -LiteralPath $JournalRoot -PathType Container)) { return $null }
    $order = @{ PREPARED=1; COMMITTED=2; PUSHED=3; REPORTED=4 }
    $records = @()
    $phaseByRank = @{ '1'='PREPARED'; '2'='COMMITTED'; '3'='PUSHED'; '4'='REPORTED' }
    foreach ($file in @(Get-ChildItem -LiteralPath $JournalRoot -Filter 'p.*.json' -File)) {
        if ($file.Name -notmatch '^p\.([1-4])\.([0-9a-f]{64})\.json$') { throw "Unerwartete Publish-Journaldatei: $($file.Name)" }
        $phase=$phaseByRank[$Matches[1]]; $hash=$Matches[2]
        if ((Get-ItoevaSha256 $file.FullName) -ne $hash) { throw "Publish-Journal-Hash stimmt nicht: $($file.FullName)" }
        $record = Get-Content -Raw -LiteralPath $file.FullName | ConvertFrom-Json
        if ([string]$record.phase -ne $phase) { throw 'Publish-Journalphase stimmt nicht mit dem Dateinamen ueberein.' }
        $records += [pscustomobject]@{ phase=$phase; rank=$order[$phase]; hash=$hash; record=$record }
    }
    if (-not $records.Count) { return $null }
    foreach ($group in @($records | Group-Object phase)) { if ($group.Count -ne 1) { throw "Mehrdeutige Publish-Journalphase: $($group.Name)" } }
    $sorted = @($records | Sort-Object rank)
    if ($sorted[0].rank -ne 1 -or -not [string]::IsNullOrEmpty([string]$sorted[0].record.previousJournalHash) -or $sorted.Count -ne $sorted[-1].rank) {
        throw 'Publish-Journal beginnt nicht mit einem eindeutigen PREPARED-Genesis-Eintrag.'
    }
    for ($index=1; $index -lt $sorted.Count; $index++) {
        if ($sorted[$index].rank -ne ($sorted[$index-1].rank + 1) -or [string]$sorted[$index].record.previousJournalHash -ne $sorted[$index-1].hash) {
            throw 'Publish-Journalkette ist unvollstaendig oder nicht hashgebunden.'
        }
    }
    foreach($item in $sorted){
        foreach($name in @('runId','dryRunReportSha256','stateEvidenceSha256','planReviewEvidenceSha256','finalReviewEvidenceSha256','claudeReviewEvidenceSha256','claudeContextEvidenceSha256','claudePatchEvidenceSha256','branch','baseSha','proposedTreeOid','planHash','testManifestHash','title','allowedPaths')){
            if(-not ($item.record.PSObject.Properties.Name -contains $name)){throw "Publish-Journalfeld fehlt in $($item.phase): $name"}
        }
        if($item.rank -ge 2 -and [string]$item.record.commitSha -notmatch '^[0-9a-f]{40,64}$'){throw "Commit-SHA fehlt in $($item.phase)."}
        if($item.rank -ge 3 -and ([string]$item.record.remoteBranchSha -ne [string]$item.record.commitSha -or [string]::IsNullOrWhiteSpace([string]$item.record.publishedAt))){throw "Push-Bindung fehlt in $($item.phase)."}
    }
    return $sorted[-1]
}

function Write-ItoevaAtomicText {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$Path,
        [AllowEmptyString()][Parameter(Mandatory)][string]$Value
    )
    $directory = Split-Path -Parent $Path
    if (-not (Test-Path -LiteralPath $directory)) { New-Item -ItemType Directory -Path $directory | Out-Null }
    $temporary = "$Path.$([Guid]::NewGuid().ToString('N')).tmp"
    try {
        [IO.File]::WriteAllText($temporary, $Value, [Text.UTF8Encoding]::new($false))
        Move-Item -LiteralPath $temporary -Destination $Path -Force
    } finally {
        if (Test-Path -LiteralPath $temporary) { Remove-Item -LiteralPath $temporary -Force }
    }
}

function Copy-ItoevaHashedEvidence {
    [CmdletBinding()]
    param([Parameter(Mandatory)][string]$SourcePath,[Parameter(Mandatory)][string]$DestinationPath,[string]$ExpectedHash)
    if (-not (Test-Path -LiteralPath $SourcePath -PathType Leaf)) { throw "Source-Evidence fehlt: $SourcePath" }
    $sourceHash=Get-ItoevaSha256 $SourcePath
    if ($ExpectedHash -and $sourceHash -ne $ExpectedHash) { throw "Source-Evidence-Hash stimmt nicht: $SourcePath" }
    $directory=Split-Path -Parent $DestinationPath
    if (-not (Test-Path -LiteralPath $directory)) { New-Item -ItemType Directory -Path $directory -Force | Out-Null }
    $temporary="$DestinationPath.$([Guid]::NewGuid().ToString('N')).tmp"
    try { [IO.File]::WriteAllBytes($temporary,[IO.File]::ReadAllBytes($SourcePath)); Move-Item -LiteralPath $temporary -Destination $DestinationPath }
    finally { if(Test-Path -LiteralPath $temporary){Remove-Item -LiteralPath $temporary -Force} }
    Write-ItoevaAtomicText "$DestinationPath.sha256" $sourceHash
    return Read-ItoevaHashedJson $DestinationPath "$DestinationPath.sha256"
}

function Copy-ItoevaHashedFileEvidence {
    [CmdletBinding()]param([Parameter(Mandatory)][string]$SourcePath,[Parameter(Mandatory)][string]$DestinationPath)
    $hash=Get-ItoevaSha256 $SourcePath;$sidecar="$DestinationPath.sha256"
    if(Test-Path -LiteralPath $DestinationPath){if((Get-ItoevaSha256 $DestinationPath)-ne $hash -or -not(Test-Path $sidecar) -or ([IO.File]::ReadAllText($sidecar).Trim()-ne$hash)){throw 'Datei-Evidence stimmt nicht mit dem Snapshot ueberein.'}}
    else{[IO.File]::WriteAllBytes($DestinationPath,[IO.File]::ReadAllBytes($SourcePath));Write-ItoevaAtomicText $sidecar $hash}
    return [pscustomobject]@{path=$DestinationPath;hash=$hash}
}

function Invoke-ItoevaGitProcess {
    [CmdletBinding()]
    param([Parameter(Mandatory)][string]$Repository,[Parameter(Mandatory)][string[]]$Arguments,[string]$IndexPath)
    $git=Get-Command git.exe -CommandType Application -ErrorAction SilentlyContinue|Select-Object -First 1
    if(-not $git){$git=Get-Command git -CommandType Application -ErrorAction Stop|Select-Object -First 1}
    $all=@('-C',$Repository)+@($Arguments); $start=[Diagnostics.ProcessStartInfo]::new()
    $start.FileName=$git.Source; $start.Arguments=(@($all|ForEach-Object{ConvertTo-ItoevaWindowsCommandLineArgument ([string]$_)}) -join ' ')
    $start.WorkingDirectory=$Repository; $start.UseShellExecute=$false; $start.CreateNoWindow=$true
    $start.RedirectStandardOutput=$true; $start.RedirectStandardError=$true
    if($IndexPath){$start.EnvironmentVariables['GIT_INDEX_FILE']=$IndexPath}
    $process=[Diagnostics.Process]::new(); $process.StartInfo=$start
    try {
        if(-not $process.Start()){throw 'Git-Prozess konnte nicht gestartet werden.'}
        $stdoutTask=$process.StandardOutput.ReadToEndAsync(); $stderrTask=$process.StandardError.ReadToEndAsync(); $process.WaitForExit()
        return [pscustomobject]@{exitCode=$process.ExitCode;stdout=$stdoutTask.Result;stderr=$stderrTask.Result}
    } finally {$process.Dispose()}
}

function Get-ItoevaProposedTreeOid {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$Repository,
        [Parameter(Mandatory)][string]$BaseSha,
        [Parameter(Mandatory)][string[]]$AllowedPaths
    )

    $indexPath = Join-Path ([IO.Path]::GetTempPath()) "itoeva-index-$([Guid]::NewGuid().ToString('N'))"
    try {
        $result=Invoke-ItoevaGitProcess $Repository @('read-tree',$BaseSha) $indexPath
        if ($result.exitCode -ne 0) { throw 'Temporärer Git-Index konnte nicht initialisiert werden.' }
        $result=Invoke-ItoevaGitProcess $Repository (@('add','--all','--')+@($AllowedPaths)) $indexPath
        if ($result.exitCode -ne 0) { throw 'Proposed Tree konnte nicht aufgebaut werden.' }
        $result=Invoke-ItoevaGitProcess $Repository @('write-tree') $indexPath; $oid=$result.stdout.Trim()
        if ($result.exitCode -ne 0 -or $oid -notmatch '^[0-9a-f]{40,64}$') {
            throw 'Git Tree OID konnte nicht bestimmt werden.'
        }
        return $oid
    } finally {
        if (Test-Path -LiteralPath $indexPath) { Remove-Item -LiteralPath $indexPath -Force }
    }
}

function Get-ItoevaExpectedRebasedTree {
    [CmdletBinding()]
    param([Parameter(Mandatory)][string]$Repository,[Parameter(Mandatory)][string]$SourceBaseSha,[Parameter(Mandatory)][string]$SourceTreeOid,[Parameter(Mandatory)][string]$NewBaseSha,[Parameter(Mandatory)][string[]]$Paths,[Parameter(Mandatory)][string]$RuntimeRoot)
    foreach($object in @("$SourceBaseSha^{commit}","$SourceTreeOid^{tree}","$NewBaseSha^{commit}")){
        $check=Invoke-ItoevaGitProcess $Repository @('cat-file','-e',$object)
        if($check.exitCode -ne 0){throw "Git-Objekt fuer Revalidierung fehlt: $object"}
    }
    $tempRoot=Assert-ItoevaRuntimePath $RuntimeRoot (Join-Path $RuntimeRoot 'temp')
    if(-not(Test-Path -LiteralPath $tempRoot)){New-Item -ItemType Directory -Path $tempRoot|Out-Null}
    Assert-ItoevaRuntimePath $RuntimeRoot $tempRoot|Out-Null
    $token=[Guid]::NewGuid().ToString('N'); $indexPath=Join-Path $tempRoot "revalidate-$token.index"; $patchPath=Join-Path $tempRoot "revalidate-$token.patch"
    Assert-ItoevaRuntimePath $RuntimeRoot $indexPath|Out-Null; Assert-ItoevaRuntimePath $RuntimeRoot $patchPath|Out-Null
    try {
        $result=Invoke-ItoevaGitProcess $Repository (@('diff','--binary','--full-index',"--output=$patchPath",$SourceBaseSha,$SourceTreeOid,'--')+@($Paths))
        if($result.exitCode -ne 0 -or -not(Test-Path -LiteralPath $patchPath -PathType Leaf)){throw 'Alter Evolution-Diff konnte nicht bytegenau erzeugt werden.'}
        $result=Invoke-ItoevaGitProcess $Repository @('read-tree',$NewBaseSha) $indexPath
        if($result.exitCode -ne 0){throw 'Isolierter Revalidierungsindex konnte nicht initialisiert werden.'}
        $result=Invoke-ItoevaGitProcess $Repository @('apply','--cached','--whitespace=error-all',$patchPath) $indexPath
        if($result.exitCode -ne 0){throw "Alter Evolution-Diff ist auf dem neuen Base nicht exakt anwendbar: $($result.stderr.Trim())"}
        $result=Invoke-ItoevaGitProcess $Repository @('write-tree') $indexPath; $tree=$result.stdout.Trim()
        if($result.exitCode -ne 0 -or $tree -notmatch '^[0-9a-f]{40,64}$'){throw 'Erwarteter rebasierter Tree konnte nicht erzeugt werden.'}
        return $tree
    } finally { foreach($path in @($indexPath,$patchPath)){if(Test-Path -LiteralPath $path){Remove-Item -LiteralPath $path -Force}} }
}

function Invoke-ItoevaConfiguredTests {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$Repository,
        [Parameter(Mandatory)]$Config,
        [string[]]$ChangedPaths = @()
    )
    $results = @()
    $selected = @(Get-ItoevaSelectedTests -Config $Config -ChangedPaths $ChangedPaths)
    $hasAndroid = @($selected | Where-Object { $_.PSObject.Properties.Name -contains 'pathPrefixes' }).Count -gt 0
    $emulatorStarted = $false
    $emulatorLock = $null
    $configuredRuntime = if ($Config.PSObject.Properties.Name -contains 'runtimeRoot') { [string]$Config.runtimeRoot } else { '%LOCALAPPDATA%\ItoevaEvolutionRunner' }
    $emulatorLockPath = Join-Path ([Environment]::ExpandEnvironmentVariables($configuredRuntime)) 'locks\emulator.lock'
    $sdk = [Environment]::ExpandEnvironmentVariables([string]$Config.tests.androidSdk)
    $adb = Join-Path $sdk 'platform-tools\adb.exe'
    try {
        if ($hasAndroid) {
            $emulatorLock = Enter-ItoevaRunLock -Path $emulatorLockPath
            if (-not (Test-Path -LiteralPath $adb)) { throw 'adb für verpflichtende Android-Tests fehlt.' }
            $serial = [string]$Config.tests.androidSerial
            $connected = @(& $adb devices) | Where-Object { $_ -match "^$([regex]::Escape($serial))\s+device$" }
            if (-not $connected) {
                $emulator = Join-Path $sdk 'emulator\emulator.exe'
                if (-not (Test-Path -LiteralPath $emulator)) { throw 'Android Emulator fehlt.' }
                Start-Process -FilePath $emulator -ArgumentList @('-avd',[string]$Config.tests.androidAvd,'-no-window','-no-audio','-no-boot-anim') -WindowStyle Hidden | Out-Null
                $emulatorStarted = $true
            }
            $deadline = (Get-Date).AddSeconds([int]$Config.tests.bootTimeoutSeconds)
            do {
                $boot = (& $adb -s $serial shell getprop sys.boot_completed 2>$null).Trim()
                if ($boot -eq '1') { break }
                Start-Sleep -Seconds 2
            } while ((Get-Date) -lt $deadline)
            if ($boot -ne '1') { throw 'Android Emulator wurde nicht rechtzeitig bootbereit.' }
        }
        foreach ($test in $selected) {
            $started = [DateTimeOffset]::UtcNow
            try {
                $oldJavaHome = $env:JAVA_HOME
                $oldAndroidHome = $env:ANDROID_HOME
                $oldAndroidSerial = $env:ANDROID_SERIAL
                $env:JAVA_HOME = [Environment]::ExpandEnvironmentVariables([string]$Config.tests.javaHome)
                $env:ANDROID_HOME = $sdk
                $env:ANDROID_SERIAL = [string]$Config.tests.androidSerial
                $processResult = Invoke-ItoevaProcessWithTimeout -Executable ([string]$test.executable) -Arguments @($test.arguments) `
                    -WorkingDirectory $Repository -TimeoutSeconds ([int]$Config.tests.testTimeoutSeconds)
                $output = $processResult.output
                $exitCode = $processResult.exitCode
            } finally {
                $env:JAVA_HOME = $oldJavaHome
                $env:ANDROID_HOME = $oldAndroidHome
                $env:ANDROID_SERIAL = $oldAndroidSerial
            }
            $results += [pscustomobject]@{
                id = [string]$test.id; status = if ($exitCode -eq 0) { 'PASS' } else { 'FAIL' }
                exitCode = $exitCode; startedAt = $started.ToString('O')
                timedOut = $processResult.timedOut; finishedAt = [DateTimeOffset]::UtcNow.ToString('O'); output = ($output -join "`n")
            }
            if ($exitCode -ne 0) { break }
        }
    } finally {
        if ($emulatorStarted) { & $adb -s ([string]$Config.tests.androidSerial) emu kill 2>$null | Out-Null }
        if ($emulatorLock) { Exit-ItoevaRunLock -Handle $emulatorLock -Path $emulatorLockPath }
    }
    return $results
}

function Invoke-ItoevaCodexSession {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$Repository,
        [Parameter(Mandatory)][string]$Prompt,
        [Parameter(Mandatory)][string]$SchemaPath,
        [Parameter(Mandatory)][string]$OutputPath,
        [ValidateSet('read-only', 'workspace-write')][string]$Sandbox = 'read-only',
        [string]$SessionId,
        [int]$TimeoutSeconds = 1800
    )
    $eventPath = "$OutputPath.events.jsonl"
    $promptPath = "$OutputPath.prompt.txt"
    [IO.File]::WriteAllText($promptPath, $Prompt, [Text.UTF8Encoding]::new($false))
    $arguments = New-ItoevaCodexArguments -Repository $Repository -Sandbox $Sandbox -SchemaPath $SchemaPath `
        -OutputPath $OutputPath -SessionId $SessionId
    $launcher = Resolve-ItoevaCodexLauncher
    $validatedWindowsArgumentString = $null
    $processArguments = if ($launcher.kind -eq 'CMD_SHIM') {
        $command = New-ItoevaCmdShimCommand -BatchPath $launcher.batchPath -Arguments $arguments
        $validatedWindowsArgumentString = "/d /s /c `"$command`""
        @()
    } else {
        @($launcher.prefixArguments) + @($arguments)
    }
    $processResult = Invoke-ItoevaProcessWithTimeout -Executable $launcher.executable -Arguments $processArguments `
        -WorkingDirectory $Repository -TimeoutSeconds $TimeoutSeconds -StandardInputPath $promptPath `
        -ValidatedWindowsArgumentString $validatedWindowsArgumentString
    [IO.File]::WriteAllText($eventPath, $processResult.output, [Text.UTF8Encoding]::new($false))
    if ($processResult.timedOut) { throw 'Codex-Sitzung hat ihr Zeitlimit überschritten.' }
    if ($processResult.exitCode -ne 0) { throw "Codex-Sitzung fehlgeschlagen, Exitcode $($processResult.exitCode)" }
    if (-not (Test-Path -LiteralPath $OutputPath)) { throw 'Codex-Ausgabedatei fehlt.' }
    Get-Content -Raw -LiteralPath $OutputPath | ConvertFrom-Json | Out-Null
    $threadId = $SessionId
    if (-not $threadId) {
        foreach ($event in @($processResult.output -split "`r?`n")) {
            try {
                $parsed = $event | ConvertFrom-Json
                if ($parsed.thread_id) { $threadId = [string]$parsed.thread_id; break }
                if ($parsed.type -eq 'thread.started' -and $parsed.thread.id) { $threadId = [string]$parsed.thread.id; break }
            } catch { }
        }
    }
    if (-not $threadId) { throw 'Codex-Session-ID konnte nicht bestimmt werden.' }
    return [pscustomobject]@{ sessionId=$threadId; outputPath=$OutputPath; eventPath=$eventPath }
}

function Publish-ItoevaEvolution {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$Repository,
        [Parameter(Mandatory)][string]$Branch,
        [Parameter(Mandatory)][string]$BaseSha,
        [Parameter(Mandatory)][string]$Title,
        [Parameter(Mandatory)][string[]]$AllowedPaths,
        [Parameter(Mandatory)][string]$ApprovedTreeOid,
        [Parameter(Mandatory)]$Gate,
        [Parameter(Mandatory)]$Config,
        [Parameter(Mandatory)][string]$TrustedHooksPath
    )
    if (-not $Config.publication.enabled) { throw 'Veröffentlichung ist nicht aktiviert.' }
    if (-not (Test-ItoevaGate -Gate $Gate)) { throw 'Commit-/Push-Gate ist nicht vollständig PASS.' }
    if ($Gate.baseSha -ne $BaseSha -or $Gate.proposedTreeOid -ne $ApprovedTreeOid) {
        throw 'Gate ist nicht an Base und Proposed Tree gebunden.'
    }
    Assert-ItoevaAllowedPaths -Paths $AllowedPaths -Config $Config
    if (-not (Test-ItoevaBranchName -Branch $Branch -Config $Config)) { throw 'Branchname ist ungültig.' }
    if (-not (Test-Path -LiteralPath $TrustedHooksPath -PathType Container)) { throw 'Trusted Hooks-Verzeichnis fehlt.' }
    if (@(Get-ChildItem -LiteralPath $TrustedHooksPath -Force).Count -ne 0) { throw 'Trusted Hooks-Verzeichnis ist nicht leer.' }
    $dangerous = @(Get-ItoevaDangerousGitConfig -Repository $Repository -Config $Config)
    if ($dangerous.Count -gt 0) { throw "Unsichere Git-Konfiguration: $($dangerous -join '; ')" }
    $origin = (& git -C $Repository remote get-url origin).Trim()
    if ($origin -ne [string]$Config.repository.expectedOrigin) { throw "Unerwartetes origin: $origin" }
    $current = (& git -C $Repository branch --show-current).Trim()
    if ($current -ne $Branch) { throw "Falscher aktueller Branch: $current" }
    $head = (& git -C $Repository rev-parse HEAD).Trim()
    if ($head -ne $BaseSha) { throw 'HEAD entspricht nicht dem Base-SHA.' }
    Assert-ItoevaBaseUnchanged -Repository $Repository -BaseSha $BaseSha | Out-Null
    if (Get-ItoevaRemoteSha -Repository $Repository -Ref "refs/heads/$Branch") { throw 'Remote-Branch existiert bereits.' }
    $changed = @(Get-ItoevaChangedPaths -Repository $Repository)
    $expected = @($AllowedPaths | Sort-Object -Unique)
    if (($changed -join "`n") -ne ($expected -join "`n")) { throw 'Geänderte Pfade entsprechen nicht der Allowlist.' }
    $tree = Get-ItoevaProposedTreeOid -Repository $Repository -BaseSha $BaseSha -AllowedPaths $expected
    if ($tree -ne $ApprovedTreeOid) { throw 'Working Tree entspricht nicht dem freigegebenen Proposed Tree.' }
    $stagedBefore = & git -C $Repository diff --cached --quiet
    if ($LASTEXITCODE -ne 0) { throw 'Index war vor dem Commit nicht sauber.' }
    & git -C $Repository add -- @expected | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Explizites Staging fehlgeschlagen.' }
    $stagedTree = (& git -C $Repository write-tree).Trim()
    if ($stagedTree -ne $ApprovedTreeOid) { throw 'Staged Tree entspricht nicht dem Review.' }
    $number = ($Branch -split '[-/]')[1]
    $message = "$($Config.publication.commitMessagePrefix)$number`: $Title"
    & git -C $Repository -c "core.hooksPath=$TrustedHooksPath" commit -m $message | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Commit fehlgeschlagen.' }
    $commitSha = (& git -C $Repository rev-parse HEAD).Trim()
    $parent = (& git -C $Repository rev-parse "$commitSha^").Trim()
    $commitTree = (& git -C $Repository rev-parse "$commitSha^{tree}").Trim()
    if ($parent -ne $BaseSha -or $commitTree -ne $ApprovedTreeOid) { throw 'Commit-Parent oder Tree ist unerwartet.' }
    if (@(& git -C $Repository status --porcelain).Count -ne 0) { throw 'Working Tree ist nach Commit nicht sauber.' }
    Assert-ItoevaBaseUnchanged -Repository $Repository -BaseSha $BaseSha | Out-Null
    if (Get-ItoevaRemoteSha -Repository $Repository -Ref "refs/heads/$Branch") { throw 'Remote-Branch entstand vor dem Push.' }
    $pushArgs = New-ItoevaPushArguments -Branch $Branch -CommitSha $commitSha -Config $Config
    & git -C $Repository -c "core.hooksPath=$TrustedHooksPath" @pushArgs | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Evolution-Branch-Push fehlgeschlagen.' }
    $remoteBranchSha = Get-ItoevaRemoteSha -Repository $Repository -Ref "refs/heads/$Branch"
    if ($remoteBranchSha -ne $commitSha) { throw 'Remote-Branch-SHA stimmt nicht mit Commit überein.' }
    $postMain = Get-ItoevaRemoteSha -Repository $Repository -Ref 'refs/heads/main'
    return [pscustomobject]@{
        status = if ($postMain -eq $BaseSha) { 'PUSHED' } else { 'PUSHED_STALE_BASE' }
        branch = $Branch
        baseSha = $BaseSha
        commitSha = $commitSha
        remoteBranchSha = $remoteBranchSha
        originMainPostPushSha = $postMain
    }
}

function Assert-ItoevaRevalidationWorktree {
    [CmdletBinding()]
    param([Parameter(Mandatory)][string]$Repository,[Parameter(Mandatory)][string]$Branch,[Parameter(Mandatory)][string]$BaseSha,[Parameter(Mandatory)][string[]]$Paths,[Parameter(Mandatory)][string]$ExpectedTree)
    if ((& git -C $Repository branch --show-current).Trim() -ne $Branch) { throw 'Revalidierungsbranch wurde verändert.' }
    if ((& git -C $Repository rev-parse HEAD).Trim() -ne $BaseSha) { throw 'HEAD wurde während der Revalidierung verändert.' }
    & git -C $Repository diff --cached --quiet
    if($LASTEXITCODE -ne 0){throw 'Echter Git-Index ist während der Revalidierung nicht sauber.'}
    $changed=@(Get-ItoevaChangedPaths $Repository)
    if(($changed -join "`n") -ne (@($Paths|Sort-Object -Unique) -join "`n")){throw 'Working-Tree-Pfade weichen vom Source-Plan ab.'}
    $tree=Get-ItoevaProposedTreeOid $Repository $BaseSha $Paths
    if($tree -ne $ExpectedTree){throw 'Working Tree weicht vom erwarteten rebasierten Tree ab.'}
    Assert-ItoevaBaseUnchanged $Repository $BaseSha|Out-Null
    return $tree
}

function Invoke-ItoevaRevalidateDryRun {
    [CmdletBinding()]
    param([Parameter(Mandatory)][string]$Repository,[Parameter(Mandatory)][string]$SourceRunId,[Parameter(Mandatory)]$Config)
    if(-not(Test-ItoevaRunId $SourceRunId)){throw 'Source-Run-ID muss aus exakt 32 hexadezimalen Kleinbuchstaben/Ziffern bestehen.'}
    if(-not $Config.agentExecution.enabled){throw 'Agentenausführung ist nicht aktiviert.'}
    if($Config.publication.enabled){throw 'Publikation muss für RevalidateDryRun deaktiviert bleiben.'}
    $runtimeRoot=[IO.Path]::GetFullPath([string]$Config.runtimeRoot); $reportsRoot=Assert-ItoevaRuntimePath $runtimeRoot (Join-Path $runtimeRoot 'reports')
    $sourceRoot=Assert-ItoevaRuntimePath $runtimeRoot (Join-Path $runtimeRoot "state\$SourceRunId")
    $sourceReportPath=Assert-ItoevaRuntimePath $runtimeRoot (Join-Path $reportsRoot "$SourceRunId.json")
    $sourceLiveReportPath=$sourceReportPath
    $sourceReportArtifact=Read-ItoevaHashedJson $sourceReportPath "$sourceReportPath.sha256"
    $sourceReportSnapshot="$($sourceReportPath.Substring(0,$sourceReportPath.Length-5)).dry-run.json"
    $snapshot=Read-ItoevaHashedJson $sourceReportSnapshot "$sourceReportSnapshot.sha256"
    if($snapshot.hash -ne $sourceReportArtifact.hash){throw 'Source-Report stimmt nicht mit seinem historischen Dry-Run-Snapshot überein.'}
    $sourceReportArtifact=$snapshot;$sourceReportPath=$sourceReportSnapshot
    $sourceReport=$sourceReportArtifact.value
    if([string]$sourceReport.runId -ne $SourceRunId -or [string]$sourceReport.status -ne 'DRY_RUN_PASS' -or -not[string]::IsNullOrEmpty([string]$sourceReport.commitSha) -or -not[string]::IsNullOrEmpty([string]$sourceReport.remoteBranchSha)){throw 'Source-Run ist kein unveröffentlichter DRY_RUN_PASS.'}
    $journalPath=Join-Path $sourceRoot 'publish-journal'
    if(Test-Path -LiteralPath $journalPath){if(@(Get-ChildItem -LiteralPath $journalPath -Force).Count){throw 'Source-Run besitzt bereits ein Publish-Journal.'}}
    $sourcePaths=[ordered]@{state=(Join-Path $sourceRoot 'state.json');plan=(Join-Path $sourceRoot 'plan.json');planReview=(Join-Path $sourceRoot 'plan-review.json');tests=(Join-Path $sourceRoot 'tests.json');finalReview=(Join-Path $sourceRoot 'final-review.json')}
    $sourceLivePaths=[ordered]@{state=$sourcePaths.state;plan=$sourcePaths.plan;planReview=$sourcePaths.planReview;tests=$sourcePaths.tests;finalReview=$sourcePaths.finalReview}
    foreach($name in @('state','planReview','finalReview')){
        $original=$sourcePaths[$name];$snapshot=Join-Path (Split-Path -Parent $original) "$([IO.Path]::GetFileNameWithoutExtension($original)).dry-run.json"
        $snapshotArtifact=Read-ItoevaHashedJson $snapshot "$snapshot.sha256"
        if(-not(Test-Path -LiteralPath $original -PathType Leaf) -or (Get-ItoevaSha256 $original) -ne $snapshotArtifact.hash){throw "Source-Artefakt stimmt nicht mit historischem Dry-Run-Snapshot überein: $name"}
        $sourcePaths[$name]=$snapshot
    }
    foreach($path in $sourcePaths.Values){Assert-ItoevaRuntimePath $runtimeRoot $path|Out-Null;if(-not(Test-Path -LiteralPath $path -PathType Leaf)){throw "Source-Artefakt fehlt: $path"}}
    $sourceState=Get-Content -Raw -LiteralPath $sourcePaths.state|ConvertFrom-Json; $sourcePlan=Get-Content -Raw -LiteralPath $sourcePaths.plan|ConvertFrom-Json
    $sourcePlanReview=Get-Content -Raw -LiteralPath $sourcePaths.planReview|ConvertFrom-Json; $parsedSourceTests=Get-Content -Raw -LiteralPath $sourcePaths.tests|ConvertFrom-Json; $sourceTests=@($parsedSourceTests)
    $sourceFinalReview=Get-Content -Raw -LiteralPath $sourcePaths.finalReview|ConvertFrom-Json
    $sourceBase=[string]$sourceReport.baseSha; $sourceTree=[string]$sourceReport.proposedTreeOid; $sourcePlanHash=[string]$sourceReport.planHash; $sourceTestHash=[string]$sourceReport.testManifestHash
    $sourceClaudeEvidence=$null
    if($sourceReport.PSObject.Properties.Name -contains 'reviewGateVersion'){if($sourceReport.reviewGateVersion -isnot [int] -or $sourceReport.reviewGateVersion -ne 2){throw 'Unbekannte oder typfalsche Source-Review-Gate-Version.'};$sourceReviewGateVersion=2}else{$sourceReviewGateVersion=1}
    if($sourceReviewGateVersion -eq 2){
        $claudeLive=Join-Path $sourceRoot 'claude-final-review.json';$contextLive=Join-Path $sourceRoot 'claude-review-input\context.json';$patchLive=Join-Path $sourceRoot 'claude-review-input\proposed.patch'
        $claudeSnapshot=Join-Path $sourceRoot 'claude-final-review.dry-run.json';$contextSnapshot=Join-Path $sourceRoot 'claude-context.dry-run.json';$patchSnapshot=Join-Path $sourceRoot 'claude-patch.dry-run.patch'
        foreach($pair in @(@($claudeLive,$claudeSnapshot),@($contextLive,$contextSnapshot),@($patchLive,$patchSnapshot))){if(-not(Test-Path $pair[0])-or-not(Test-Path $pair[1])-or-not(Test-Path "$($pair[1]).sha256")){throw 'Gate-v2-Source besitzt keine vollstaendige historische Claude-Evidence.'};$hash=(Get-Content -Raw "$($pair[1]).sha256").Trim();if((Get-ItoevaSha256 $pair[0])-ne$hash-or(Get-ItoevaSha256 $pair[1])-ne$hash){throw 'Gate-v2-Claude-Source-Evidence wurde veraendert.'}}
        if((Get-ItoevaSha256 $claudeSnapshot)-ne[string]$sourceReport.claudeFinalReviewSha256-or(Get-ItoevaSha256 $contextSnapshot)-ne[string]$sourceReport.claudeReviewContextSha256-or(Get-ItoevaSha256 $patchSnapshot)-ne[string]$sourceReport.claudeReviewPatchSha256){throw 'Gate-v2-Claude-Source-Hashes stimmen nicht mit dem Report ueberein.'}
        $sourceClaude=Get-Content -Raw $claudeSnapshot|ConvertFrom-Json;Assert-ItoevaClaudeReview $sourceClaude $sourceBase $sourceTree $sourceTestHash ([string]$sourceReport.claudeReviewContextSha256)
        if([string]$sourceReport.claudeFinalReview-ne'PASS'-or[string]$sourceReport.claudeReviewModel-ne(Get-ItoevaClaudeReviewModel $sourcePlan $Config)){throw 'Gate-v2-Claude-Source-Gate ist nicht PASS oder verwendet das falsche Risikomodell.'}
        $sourceClaudeContext=Get-Content -Raw $contextSnapshot|ConvertFrom-Json
        Assert-ItoevaClaudeStoredProvenance $sourceClaudeContext ([string]$sourceReport.claudeReviewModel) (Assert-ItoevaClaudeConfigPins $Config)
        $sourceClaudeEvidence=[ordered]@{review=$claudeSnapshot;context=$contextSnapshot;patch=$patchSnapshot}
    }
    if([string]$sourceState.runId -ne $SourceRunId -or [string]$sourceState.phase -ne 'COMPLETE' -or [string]$sourceState.status -ne 'DRY_RUN_PASS' -or [string]$sourceState.baseSha -ne $sourceBase -or [string]$sourceState.branch -ne [string]$sourceReport.branch){throw 'Source-State ist nicht vollständig an den Report gebunden.'}
    if((Get-ItoevaSha256 $sourcePaths.plan) -ne $sourcePlanHash -or (Get-ItoevaSha256 $sourcePaths.tests) -ne $sourceTestHash){throw 'Source-Plan- oder Testmanifest-Hash stimmt nicht.'}
    if([string]$sourcePlan.status -ne 'PLANNED' -or [string]$sourcePlan.baseSha -ne $sourceBase -or [string]::IsNullOrWhiteSpace([string]$sourcePlan.title)){throw 'Source-Plan ist nicht revalidierbar.'}
    if([string]$sourcePlanReview.status -ne 'PASS' -or [string]$sourcePlanReview.baseSha -ne $sourceBase -or [string]$sourcePlanReview.planHash -ne $sourcePlanHash){throw 'Source-Planreview ist nicht gebunden PASS.'}
    if($sourceTests|Where-Object{$_.status -ne 'PASS'}){throw 'Source-Testmanifest enthält nicht-PASS Ergebnisse.'}
    if([string]$sourceFinalReview.status -ne 'PASS' -or [string]$sourceFinalReview.baseSha -ne $sourceBase -or [string]$sourceFinalReview.planHash -ne $sourcePlanHash -or [string]$sourceFinalReview.treeOid -ne $sourceTree -or [string]$sourceFinalReview.testManifestHash -ne $sourceTestHash){throw 'Source-Final-Review ist nicht vollständig gebunden PASS.'}
    if([string]$sourceReport.planReview -ne 'PASS' -or [string]$sourceReport.finalReview -ne 'PASS' -or [string]$sourceReport.reviewerBindings.baseSha -ne $sourceBase -or [string]$sourceReport.reviewerBindings.planHash -ne $sourcePlanHash -or [string]$sourceReport.reviewerBindings.treeOid -ne $sourceTree -or [string]$sourceReport.reviewerBindings.testManifestHash -ne $sourceTestHash){throw 'Source-Report-Gates sind nicht vollständig gebunden.'}
    if((@($sourceReport.tests)|ConvertTo-Json -Depth 20 -Compress) -ne ($sourceTests|ConvertTo-Json -Depth 20 -Compress)){throw 'Source-Report-Tests stimmen nicht mit dem Testmanifest überein.'}
    $paths=@($sourcePlan.paths|Sort-Object -Unique); Assert-ItoevaAllowedPaths $paths $Config

    $origin=(& git -C $Repository remote get-url origin).Trim();if($origin -ne [string]$Config.repository.expectedOrigin){throw "Unerwartetes origin: $origin"}
    $dangerous=@(Get-ItoevaDangerousGitConfig $Repository $Config);if($dangerous.Count){throw "Unsichere Git-Konfiguration: $($dangerous -join '; ')"}
    & git -C $Repository fetch --prune origin|Out-Null;if($LASTEXITCODE -ne 0){throw 'Fetch für Revalidierung fehlgeschlagen.'}
    $newBase=Get-ItoevaRemoteSha $Repository 'origin' 'refs/heads/main'; $tracking=(& git -C $Repository rev-parse refs/remotes/origin/main).Trim()
    if($tracking -ne $newBase){throw 'Tracking-Ref origin/main ist nicht synchron.'}
    $branch=[string]$sourceReport.branch;if(-not(Test-ItoevaBranchName $branch $Config)){throw 'Source-Evolution-Branch ist ungültig.'}
    if((& git -C $Repository branch --show-current).Trim() -ne $branch -or (& git -C $Repository rev-parse HEAD).Trim() -ne $newBase){throw 'Aktueller Branch oder HEAD entspricht nicht dem Revalidierungszustand.'}
    & git -C $Repository diff --cached --quiet;if($LASTEXITCODE -ne 0){throw 'Index muss vor Revalidierung sauber sein.'}
    $changed=@(Get-ItoevaChangedPaths $Repository);if(($changed -join "`n") -ne ($paths -join "`n")){throw 'Aktuelle Pfade entsprechen nicht dem Source-Plan.'}
    if(Get-ItoevaRemoteSha $Repository 'origin' "refs/heads/$branch"){throw 'Remote-Evolution-Branch existiert bereits.'}
    $expectedTree=Get-ItoevaExpectedRebasedTree $Repository $sourceBase $sourceTree $newBase $paths $runtimeRoot
    $currentTree=Get-ItoevaProposedTreeOid $Repository $newBase $paths;if($currentTree -ne $expectedTree){throw 'Aktueller Working Tree ist nicht der exakt übertragene Source-Diff.'}
    & git -C $Repository diff-tree --check $newBase $currentTree;if($LASTEXITCODE -ne 0){throw 'Revalidierter Tree besteht diff-tree --check nicht.'}

    $newRunId=[Guid]::NewGuid().ToString('N');$runRoot=Assert-ItoevaRuntimePath $runtimeRoot (Join-Path $runtimeRoot "state\$newRunId");$statePath=Join-Path $runRoot 'state.json';$reportPath=Join-Path $reportsRoot "$newRunId.json"
    $state=[ordered]@{runId=$newRunId;phase='REVALIDATE_TEST';baseSha=$newBase;branch=$branch;status='RUNNING';sourceRunId=$SourceRunId}
    try {
        Write-ItoevaAtomicJson $state $statePath;$evidenceRoot=Join-Path $runRoot 'source-evidence'
        $reportEvidence=Copy-ItoevaHashedEvidence $sourceReportPath (Join-Path $evidenceRoot 'report.json') $sourceReportArtifact.hash
        $stateEvidence=Copy-ItoevaHashedEvidence $sourcePaths.state (Join-Path $evidenceRoot 'state.json')
        $planEvidence=Copy-ItoevaHashedEvidence $sourcePaths.plan (Join-Path $runRoot 'plan.json') $sourcePlanHash
        $planReviewEvidence=Copy-ItoevaHashedEvidence $sourcePaths.planReview (Join-Path $runRoot 'plan-review.json')
        $testsEvidence=Copy-ItoevaHashedEvidence $sourcePaths.tests (Join-Path $evidenceRoot 'tests.json') $sourceTestHash
        $finalEvidence=Copy-ItoevaHashedEvidence $sourcePaths.finalReview (Join-Path $evidenceRoot 'final-review.json')
        if($sourceClaudeEvidence){$sourceClaudeReviewEvidence=Copy-ItoevaHashedEvidence $sourceClaudeEvidence.review (Join-Path $evidenceRoot 'claude-final-review.json');$sourceClaudeContextEvidence=Copy-ItoevaHashedEvidence $sourceClaudeEvidence.context (Join-Path $evidenceRoot 'claude-context.json');$sourceClaudePatchEvidence=Copy-ItoevaHashedFileEvidence $sourceClaudeEvidence.patch (Join-Path $evidenceRoot 'claude-patch.patch')}
        $revalidation=[ordered]@{sourceRunId=$SourceRunId;sourceReportSha256=$reportEvidence.hash;sourceStateSha256=$stateEvidence.hash;sourcePlanSha256=$planEvidence.hash;sourcePlanReviewSha256=$planReviewEvidence.hash;sourceTestsSha256=$testsEvidence.hash;sourceFinalReviewSha256=$finalEvidence.hash;sourceBaseSha=$sourceBase;sourceTreeOid=$sourceTree;newBaseSha=$newBase;branch=$branch;expectedRebasedTreeOid=$expectedTree;paths=$paths}
        $revalidation.sourceReviewGateVersion=$sourceReviewGateVersion
        if($sourceClaudeEvidence){$revalidation.sourceClaudeReviewSha256=$sourceClaudeReviewEvidence.hash;$revalidation.sourceClaudeContextSha256=$sourceClaudeContextEvidence.hash;$revalidation.sourceClaudePatchSha256=$sourceClaudePatchEvidence.hash}
        $revalidationPath=Join-Path $runRoot 'revalidation.json';Write-ItoevaAtomicJson $revalidation $revalidationPath;$revalidationHash=Get-ItoevaSha256 $revalidationPath;Write-ItoevaAtomicText "$revalidationPath.sha256" $revalidationHash
        Assert-ItoevaRevalidationWorktree $Repository $branch $newBase $paths $expectedTree|Out-Null
        $testResults=@(Invoke-ItoevaConfiguredTests $Repository $Config $paths);$testPath=Join-Path $runRoot 'tests.json';Write-ItoevaAtomicJson $testResults $testPath
        if($testResults|Where-Object{$_.status -ne 'PASS'}){throw 'Revalidierungs-Pflichttest fehlgeschlagen.'};$testHash=Get-ItoevaSha256 $testPath
        Assert-ItoevaRevalidationWorktree $Repository $branch $newBase $paths $expectedTree|Out-Null
        $state.phase='REVALIDATE_FINAL_REVIEW';Write-ItoevaAtomicJson $state $statePath
        $finalPath=Join-Path $runRoot 'final-review.json';$prompt=(Get-Content -Raw (Join-Path $PSScriptRoot 'prompts\review-final.md'))+"`nRevalidation Source-Run: $SourceRunId`nSource-Plan: $(Join-Path $runRoot 'plan.json')`nSource-Plan-Hash: $sourcePlanHash`nRevalidation-Provenienz: $revalidationPath`nBase: $newBase`nTree: $expectedTree`nTestmanifest-Pfad: $testPath`nTestmanifest-Hash: $testHash"
        Invoke-ItoevaCodexSession $Repository $prompt (Join-Path $PSScriptRoot 'schemas\review.schema.json') $finalPath 'read-only' -TimeoutSeconds ([int]$Config.agentExecution.sessionTimeoutSeconds)|Out-Null
        $final=Get-Content -Raw -LiteralPath $finalPath|ConvertFrom-Json
        if($final.status -ne 'PASS' -or $final.baseSha -ne $newBase -or $final.planHash -ne $sourcePlanHash -or $final.treeOid -ne $expectedTree -or $final.testManifestHash -ne $testHash){throw 'Revalidiertes Codex-Final-Review ist nicht gebunden PASS.'}
        $state.phase='REVALIDATE_CLAUDE_REVIEW';Write-ItoevaAtomicJson $state $statePath
        $claudeModel=Get-ItoevaClaudeReviewModel $sourcePlan $Config
        $claude=Invoke-ItoevaClaudeFinalReview -Repository $Repository -RunRoot $runRoot -BaseSha $newBase -TreeOid $expectedTree -PlanHash $sourcePlanHash -TestManifestHash $testHash -ChangedPaths $paths -PlanPath (Join-Path $runRoot 'plan.json') -TestsPath $testPath -CodexReviewPath $finalPath -Model $claudeModel -Config $Config
        $gate=[pscustomobject]@{planReview='PASS';mandatoryTests='PASS';finalReview=$final.status;claudeFinalReview=$claude.status;diffCheck='PASS';baseUnchanged=$true;baseSha=$newBase;proposedTreeOid=$expectedTree;testManifestHash=$testHash;planHash=$sourcePlanHash;reviewBaseSha=$final.baseSha;reviewPlanHash=$final.planHash;reviewTreeOid=$final.treeOid;reviewTestManifestHash=$final.testManifestHash;claudeReviewBaseSha=$claude.bindings.baseSha;claudeReviewTreeOid=$claude.bindings.treeOid;claudeReviewTestManifestHash=$claude.bindings.testManifestHash;claudeReviewContextHash=$claude.contextHash}
        if(-not(Test-ItoevaGate $gate)){throw 'Revalidiertes finales Gate ist nicht vollständig PASS und hashgebunden.'}
        Assert-ItoevaRevalidationWorktree $Repository $branch $newBase $paths $expectedTree|Out-Null
        $sourceBindings=[ordered]@{sourceRunId=$SourceRunId;sourceReviewGateVersion=$sourceReviewGateVersion;sourceReportSha256=$reportEvidence.hash;sourceStateSha256=$stateEvidence.hash;sourcePlanSha256=$planEvidence.hash;sourcePlanReviewSha256=$planReviewEvidence.hash;sourceTestsSha256=$testsEvidence.hash;sourceFinalReviewSha256=$finalEvidence.hash;sourceBaseSha=$sourceBase;sourceTreeOid=$sourceTree;revalidationSha256=$revalidationHash}
        if($sourceClaudeEvidence){$sourceBindings.sourceClaudeReviewSha256=$sourceClaudeReviewEvidence.hash;$sourceBindings.sourceClaudeContextSha256=$sourceClaudeContextEvidence.hash;$sourceBindings.sourceClaudePatchSha256=$sourceClaudePatchEvidence.hash}
        $report=[ordered]@{formatVersion=2;runKind='REVALIDATED_DRY_RUN';reviewGateVersion=2;runId=$newRunId;status='DRY_RUN_PASS';branch=$branch;baseSha=$newBase;commitSha='';remoteBranchSha='';originMainStartSha=$newBase;originMainPrePushSha=$newBase;originMainPostPushSha=$newBase;planReview='PASS';finalReview='PASS';claudeFinalReview='PASS';claudeReviewModel=$claude.model;claudeFinalReviewSha256=$claude.reviewHash;claudeReviewContextSha256=$claude.contextHash;claudeReviewPatchSha256=$claude.patchHash;proposedTreeOid=$expectedTree;testManifestHash=$testHash;planHash=$sourcePlanHash;reviewerBindings=[ordered]@{baseSha=$final.baseSha;planHash=$final.planHash;treeOid=$final.treeOid;testManifestHash=$final.testManifestHash};claudeReviewerBindings=[ordered]@{baseSha=$claude.bindings.baseSha;treeOid=$claude.bindings.treeOid;testManifestHash=$claude.bindings.testManifestHash;contextHash=$claude.bindings.contextHash};sourceBindings=$sourceBindings;tests=$testResults;unverified=@();knownRisks=@($Config.knownRisks.mainRulesetNote)}
        foreach($pair in @(@($sourceReportPath,$reportEvidence.hash),@($sourcePaths.state,$stateEvidence.hash),@($sourcePaths.plan,$planEvidence.hash),@($sourcePaths.planReview,$planReviewEvidence.hash),@($sourcePaths.tests,$testsEvidence.hash),@($sourcePaths.finalReview,$finalEvidence.hash))){if((Get-ItoevaSha256 $pair[0]) -ne $pair[1]){throw 'Source-Artefakt wurde während der Revalidierung verändert.'}}
        foreach($pair in @(@($sourceLiveReportPath,$reportEvidence.hash),@($sourceLivePaths.state,$stateEvidence.hash),@($sourceLivePaths.plan,$planEvidence.hash),@($sourceLivePaths.planReview,$planReviewEvidence.hash),@($sourceLivePaths.tests,$testsEvidence.hash),@($sourceLivePaths.finalReview,$finalEvidence.hash))){if((Get-ItoevaSha256 $pair[0]) -ne $pair[1]){throw 'Ursprüngliches Source-Artefakt wurde während der Revalidierung verändert.'}}
        $state.phase='COMPLETE';$state.status='DRY_RUN_PASS';Write-ItoevaAtomicJson $state $statePath;Write-ItoevaAtomicJson $report $reportPath;Write-ItoevaAtomicText "$reportPath.sha256" (Get-ItoevaSha256 $reportPath)
        return [pscustomobject]@{status='DRY_RUN_PASS';runId=$newRunId;sourceRunId=$SourceRunId;baseSha=$newBase;branch=$branch;proposedTreeOid=$expectedTree;reportPath=$reportPath}
    } catch {
        $state.status='QUARANTINED';$state['error']=$_.Exception.Message
        foreach($path in @($reportPath,"$reportPath.sha256")){if($path -and (Test-Path -LiteralPath $path)){Remove-Item -LiteralPath $path -Force}}
        if($statePath){Write-ItoevaAtomicJson $state $statePath};throw
    }
}

function Invoke-ItoevaPublishDryRun {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$Repository,
        [Parameter(Mandatory)][string]$RunId,
        [Parameter(Mandatory)]$Config,
        [Parameter(Mandatory)][string]$TrustedHooksPath
    )
    if (-not (Test-ItoevaRunId $RunId)) { throw 'Run-ID muss aus exakt 32 hexadezimalen Kleinbuchstaben/Ziffern bestehen.' }
    if (-not $Config.publication.enabled) { throw 'Veroeffentlichung ist nicht aktiviert.' }
    $runtimeRoot = [IO.Path]::GetFullPath([string]$Config.runtimeRoot)
    $runRoot = Assert-ItoevaRuntimePath $runtimeRoot (Join-Path $runtimeRoot "state\$RunId")
    $reportsRoot = Assert-ItoevaRuntimePath $runtimeRoot (Join-Path $runtimeRoot 'reports')
    $statePath = Assert-ItoevaRuntimePath $runtimeRoot (Join-Path $runRoot 'state.json')
    $reportPath = Assert-ItoevaRuntimePath $runtimeRoot (Join-Path $reportsRoot "$RunId.json")
    $reportHashPath = Assert-ItoevaRuntimePath $runtimeRoot "$reportPath.sha256"
    $snapshotPath = Assert-ItoevaRuntimePath $runtimeRoot (Join-Path $reportsRoot "$RunId.dry-run.json")
    $snapshotHashPath = Assert-ItoevaRuntimePath $runtimeRoot "$snapshotPath.sha256"
    $journalRoot = Assert-ItoevaRuntimePath $runtimeRoot (Join-Path $runRoot 'publish-journal')
    foreach ($required in @($runRoot,$reportsRoot)) { if (-not (Test-Path -LiteralPath $required -PathType Container)) { throw "Runtime-Verzeichnis fehlt: $required" } }

    $journal = Get-ItoevaPublishJournal $journalRoot
    if ($journal) {
        $dryRunHash = [string]$journal.record.dryRunReportSha256
        $snapshot = Read-ItoevaHashedJson $snapshotPath $snapshotHashPath
        if ($snapshot.hash -ne $dryRunHash) { throw 'Dry-Run-Snapshot stimmt nicht mit dem Publish-Journal ueberein.' }
        $report = $snapshot.value
    } else {
        $original = Read-ItoevaHashedJson $reportPath $reportHashPath
        $dryRunHash = $original.hash
        $report = $original.value
        if (Test-Path -LiteralPath $snapshotPath) {
            if ((Get-ItoevaSha256 $snapshotPath) -ne $dryRunHash) { throw 'Vorhandener Dry-Run-Snapshot stimmt nicht mit dem Originalreport ueberein.' }
        } else {
            $temporary = "$snapshotPath.$([Guid]::NewGuid().ToString('N')).tmp"
            try {
                [IO.File]::WriteAllBytes($temporary, [IO.File]::ReadAllBytes($reportPath))
                Move-Item -LiteralPath $temporary -Destination $snapshotPath
            } finally { if (Test-Path -LiteralPath $temporary) { Remove-Item -LiteralPath $temporary -Force } }
        }
        Write-ItoevaAtomicText $snapshotHashPath $dryRunHash
    }

    $state = Get-Content -Raw -LiteralPath $statePath | ConvertFrom-Json
    if ([string]$state.runId -ne $RunId -or [string]$report.runId -ne $RunId) { throw 'Run-ID in State oder Report stimmt nicht.' }
    if ([string]$state.phase -ne 'COMPLETE') { throw 'Dry-Run ist nicht vollstaendig.' }
    if (-not $journal -and ([string]$state.status -ne 'DRY_RUN_PASS' -or [string]$report.status -ne 'DRY_RUN_PASS')) { throw 'Run ist kein unveroeffentlichter DRY_RUN_PASS.' }
    if ($journal -and $journal.phase -ne 'REPORTED' -and [string]$state.status -ne 'DRY_RUN_PASS') { throw 'Publish-Resume hat einen unerwarteten Run-State.' }
    if (-not $journal -and (-not [string]::IsNullOrEmpty([string]$report.commitSha) -or -not [string]::IsNullOrEmpty([string]$report.remoteBranchSha))) { throw 'Dry-Run-Report enthaelt bereits Publish-SHAs.' }

    $baseSha=[string]$report.baseSha; $branch=[string]$report.branch; $approvedTree=[string]$report.proposedTreeOid
    $planHash=[string]$report.planHash; $testHash=[string]$report.testManifestHash
    if ($baseSha -notmatch '^[0-9a-f]{40,64}$' -or $approvedTree -notmatch '^[0-9a-f]{40,64}$' -or $planHash -notmatch '^[0-9a-f]{64}$' -or $testHash -notmatch '^[0-9a-f]{64}$') { throw 'Dry-Run-Report enthaelt ungueltige Bindungswerte.' }
    if (-not (Test-ItoevaBranchName $branch $Config)) { throw 'Dry-Run-Branch ist ungueltig.' }
    if ([string]$state.baseSha -ne $baseSha -or [string]$state.branch -ne $branch) { throw 'Dry-Run-State stimmt nicht mit Base oder Branch des Reports ueberein.' }

    if([int]$report.reviewGateVersion -ne 2 -or [string]$report.claudeFinalReview -ne 'PASS'){throw 'Dry-Run besitzt kein gültiges Claude-Doppelreview-Gate.'}
    $claudePins=Assert-ItoevaClaudeConfigPins $Config
    $planPath=Join-Path $runRoot 'plan.json'; $planReviewPath=Join-Path $runRoot 'plan-review.json'; $testsPath=Join-Path $runRoot 'tests.json'; $finalReviewPath=Join-Path $runRoot 'final-review.json';$claudeReviewPath=Join-Path $runRoot 'claude-final-review.json';$claudeContextPath=Join-Path $runRoot 'claude-review-input\context.json';$claudePatchPath=Join-Path $runRoot 'claude-review-input\proposed.patch'
    foreach ($artifact in @($planPath,$planReviewPath,$testsPath,$finalReviewPath,$claudeReviewPath,$claudeContextPath,$claudePatchPath)) { Assert-ItoevaRuntimePath $runtimeRoot $artifact | Out-Null; if (-not (Test-Path -LiteralPath $artifact -PathType Leaf)) { throw "Dry-Run-Artefakt fehlt: $artifact" } }
    if((Get-ItoevaSha256 $claudeReviewPath) -ne [string]$report.claudeFinalReviewSha256 -or (Get-ItoevaSha256 $claudeContextPath) -ne [string]$report.claudeReviewContextSha256 -or (Get-ItoevaSha256 $claudePatchPath) -ne [string]$report.claudeReviewPatchSha256){throw 'Claude-Review-, Kontext- oder Patch-Hash stimmt nicht.'}
    $claudeContext=Get-Content -Raw $claudeContextPath|ConvertFrom-Json
    if([string]$claudeContext.baseSha-ne$baseSha-or[string]$claudeContext.treeOid-ne$approvedTree-or[string]$claudeContext.planHash-ne$planHash-or[string]$claudeContext.testManifestHash-ne$testHash-or[string]$claudeContext.patchSha256-ne[string]$report.claudeReviewPatchSha256){throw 'Claude-Kontext ist nicht semantisch an Report und Patch gebunden.'}
    $claudeReview=Get-Content -Raw $claudeReviewPath|ConvertFrom-Json;Assert-ItoevaClaudeReview $claudeReview $baseSha $approvedTree $testHash ([string]$report.claudeReviewContextSha256)
    foreach($name in @('baseSha','treeOid','testManifestHash','contextHash')){if([string]$report.claudeReviewerBindings.$name-ne[string]$claudeReview.$name){throw "Claude-Reviewerbindung im Report weicht ab: $name"}}
    # Publishing startet Claude nie und darf keine installierte Binary voraussetzen: die Modell-/CLI-
    # Provenienz wird ausschliesslich aus der gespeicherten, hashgebundenen Evidence belegt.
    Assert-ItoevaClaudeStoredProvenance $claudeContext ([string]$report.claudeReviewModel) $claudePins
    if ((Get-ItoevaSha256 $planPath) -ne $planHash -or (Get-ItoevaSha256 $testsPath) -ne $testHash) { throw 'Plan- oder Testmanifest-Hash stimmt nicht.' }
    $plan=Get-Content -Raw -LiteralPath $planPath|ConvertFrom-Json; $planReview=Get-Content -Raw -LiteralPath $planReviewPath|ConvertFrom-Json
    if([string]$report.claudeReviewModel-ne(Get-ItoevaClaudeReviewModel $plan $Config)){throw 'Claude-Reviewmodell stimmt nicht mit der gebundenen Risikoregel überein.'}
    $parsedTests=Get-Content -Raw -LiteralPath $testsPath|ConvertFrom-Json
    $tests=@($parsedTests); $finalReview=Get-Content -Raw -LiteralPath $finalReviewPath|ConvertFrom-Json
    $isRevalidated=($report.PSObject.Properties.Name -contains 'formatVersion' -and [int]$report.formatVersion -eq 2 -and [string]$report.runKind -eq 'REVALIDATED_DRY_RUN')
    if(($report.PSObject.Properties.Name -contains 'formatVersion') -and -not $isRevalidated){throw 'Unbekanntes Dry-Run-Reportformat.'}
    if($isRevalidated){
        $bindings=$report.sourceBindings
        foreach($name in @('sourceRunId','sourceReviewGateVersion','sourceReportSha256','sourceStateSha256','sourcePlanSha256','sourcePlanReviewSha256','sourceTestsSha256','sourceFinalReviewSha256','sourceBaseSha','sourceTreeOid','revalidationSha256')){if(-not($bindings.PSObject.Properties.Name -contains $name)){throw "Revalidierungsbindung fehlt: $name"}}
        if($bindings.sourceReviewGateVersion -isnot [int] -or $bindings.sourceReviewGateVersion -notin @(1,2)){throw 'Unbekannte oder typfalsche Source-Review-Gate-Version.'};$sourceGate=$bindings.sourceReviewGateVersion;if($sourceGate -eq 2){foreach($name in @('sourceClaudeReviewSha256','sourceClaudeContextSha256','sourceClaudePatchSha256')){if(-not($bindings.PSObject.Properties.Name -contains $name)){throw "Gate-v2-Revalidierungsbindung fehlt: $name"}}}
        if(-not(Test-ItoevaRunId ([string]$bindings.sourceRunId))){throw 'Revalidierungs-Source-Run-ID ist ungültig.'}
        $revalidationArtifact=Read-ItoevaHashedJson (Join-Path $runRoot 'revalidation.json') (Join-Path $runRoot 'revalidation.json.sha256')
        if($revalidationArtifact.hash -ne [string]$bindings.revalidationSha256){throw 'Revalidierungsattest stimmt nicht mit dem Report überein.'}
        $rv=$revalidationArtifact.value
        $bindingNames=@('sourceRunId','sourceReviewGateVersion','sourceReportSha256','sourceStateSha256','sourcePlanSha256','sourcePlanReviewSha256','sourceTestsSha256','sourceFinalReviewSha256','sourceBaseSha','sourceTreeOid');if($sourceGate-eq2){$bindingNames+=@('sourceClaudeReviewSha256','sourceClaudeContextSha256','sourceClaudePatchSha256')};foreach($name in $bindingNames){if([string]$rv.$name -ne [string]$bindings.$name){throw "Revalidierungsattest weicht ab: $name"}}
        if([string]$rv.newBaseSha -ne $baseSha -or [string]$rv.branch -ne $branch -or [string]$rv.expectedRebasedTreeOid -ne $approvedTree){throw 'Revalidierungsattest ist nicht an Base, Branch und Tree gebunden.'}
        $evidenceRoot=Join-Path $runRoot 'source-evidence'
        $evidenceMap=[ordered]@{report='sourceReportSha256';state='sourceStateSha256';tests='sourceTestsSha256';'final-review'='sourceFinalReviewSha256'};if($sourceGate-eq2){$evidenceMap['claude-final-review']='sourceClaudeReviewSha256';$evidenceMap['claude-context']='sourceClaudeContextSha256'}
        foreach($entry in $evidenceMap.GetEnumerator()){$artifact=Read-ItoevaHashedJson (Join-Path $evidenceRoot "$($entry.Key).json") (Join-Path $evidenceRoot "$($entry.Key).json.sha256");if($artifact.hash -ne [string]$bindings.($entry.Value)){throw "Source-Evidence stimmt nicht: $($entry.Key)"}}
        if($sourceGate-eq2 -and ((Get-ItoevaSha256 (Join-Path $evidenceRoot 'claude-patch.patch'))-ne[string]$bindings.sourceClaudePatchSha256-or(Get-Content -Raw (Join-Path $evidenceRoot 'claude-patch.patch.sha256')).Trim()-ne[string]$bindings.sourceClaudePatchSha256)){throw 'Source-Claude-Patch-Evidence stimmt nicht.'}
        if((Get-ItoevaSha256 $planPath) -ne [string]$bindings.sourcePlanSha256 -or (Get-ItoevaSha256 $planReviewPath) -ne [string]$bindings.sourcePlanReviewSha256){throw 'Source-Plan oder Source-Planreview wurde verändert.'}
    }
    $stateEvidence=Initialize-ItoevaEvidenceSnapshot $statePath (Join-Path $runRoot 'state.dry-run.json') (-not $journal -or [string]$state.status -eq 'DRY_RUN_PASS')
    $planReviewEvidence=Initialize-ItoevaEvidenceSnapshot $planReviewPath (Join-Path $runRoot 'plan-review.dry-run.json') $true
    $finalReviewEvidence=Initialize-ItoevaEvidenceSnapshot $finalReviewPath (Join-Path $runRoot 'final-review.dry-run.json') $true
    $claudeReviewEvidence=Initialize-ItoevaEvidenceSnapshot $claudeReviewPath (Join-Path $runRoot 'claude-final-review.dry-run.json') $true
    $claudeContextEvidence=Initialize-ItoevaEvidenceSnapshot $claudeContextPath (Join-Path $runRoot 'claude-context.dry-run.json') $true
    $claudePatchEvidence=Copy-ItoevaHashedFileEvidence $claudePatchPath (Join-Path $runRoot 'claude-patch.dry-run.patch')
    $planBase=if($isRevalidated){[string]$report.sourceBindings.sourceBaseSha}else{$baseSha}
    if ([string]$plan.status -ne 'PLANNED' -or [string]$plan.baseSha -ne $planBase -or [string]::IsNullOrWhiteSpace([string]$plan.title)) { throw 'Plan ist nicht publishbar.' }
    if ([string]$planReview.status -ne 'PASS' -or [string]$planReview.baseSha -ne $planBase -or [string]$planReview.planHash -ne $planHash) { throw 'Planreview ist nicht gebunden PASS.' }
    if ($tests | Where-Object { $_.status -ne 'PASS' }) { throw 'Testmanifest enthaelt nicht-PASS Ergebnisse.' }
    if ([string]$finalReview.status -ne 'PASS' -or [string]$finalReview.baseSha -ne $baseSha -or [string]$finalReview.planHash -ne $planHash -or [string]$finalReview.treeOid -ne $approvedTree -or [string]$finalReview.testManifestHash -ne $testHash) { throw 'Final Review ist nicht vollstaendig gebunden PASS.' }
    if ([string]$report.planReview -ne 'PASS' -or [string]$report.finalReview -ne 'PASS' -or [string]$report.reviewerBindings.baseSha -ne $baseSha -or [string]$report.reviewerBindings.planHash -ne $planHash -or [string]$report.reviewerBindings.treeOid -ne $approvedTree -or [string]$report.reviewerBindings.testManifestHash -ne $testHash) { throw 'Report-Gates oder Reviewer-Bindungen stimmen nicht.' }
    if ((@($report.tests)|ConvertTo-Json -Depth 20 -Compress) -ne ($tests|ConvertTo-Json -Depth 20 -Compress)) { throw 'Report-Tests stimmen nicht mit dem Testmanifest ueberein.' }
    $allowedPaths=@($plan.paths|Sort-Object -Unique); Assert-ItoevaAllowedPaths $allowedPaths $Config
    $gate=[pscustomobject]@{ planReview='PASS'; mandatoryTests='PASS'; finalReview='PASS';claudeFinalReview='PASS'; diffCheck='PASS'; baseUnchanged=$true; baseSha=$baseSha; proposedTreeOid=$approvedTree; testManifestHash=$testHash; planHash=$planHash; reviewBaseSha=$baseSha; reviewPlanHash=$planHash; reviewTreeOid=$approvedTree; reviewTestManifestHash=$testHash;claudeReviewBaseSha=$claudeReview.baseSha;claudeReviewTreeOid=$claudeReview.treeOid;claudeReviewTestManifestHash=$claudeReview.testManifestHash;claudeReviewContextHash=$claudeReview.contextHash }
    if (-not (Test-ItoevaGate $gate)) { throw 'Rekonstruiertes Publish-Gate ist nicht PASS.' }

    $origin=(& git -C $Repository remote get-url origin).Trim(); if ($origin -ne [string]$Config.repository.expectedOrigin) { throw "Unerwartetes origin: $origin" }
    $dangerous=@(Get-ItoevaDangerousGitConfig $Repository $Config); if ($dangerous.Count) { throw "Unsichere Git-Konfiguration: $($dangerous -join '; ')" }
    if (-not (Test-Path -LiteralPath $TrustedHooksPath -PathType Container) -or @(Get-ChildItem -LiteralPath $TrustedHooksPath -Force).Count) { throw 'Trusted Hooks-Verzeichnis fehlt oder ist nicht leer.' }
    if ((& git -C $Repository branch --show-current).Trim() -ne $branch) { throw 'Aktueller Branch entspricht nicht dem Dry-Run-Report.' }
    Assert-ItoevaBaseUnchanged $Repository $baseSha | Out-Null

    if (-not $journal) {
        if ((& git -C $Repository rev-parse HEAD).Trim() -ne $baseSha) { throw 'HEAD entspricht vor Publish nicht dem Base-SHA.' }
        & git -C $Repository diff --cached --quiet; if ($LASTEXITCODE -ne 0) { throw 'Index ist vor Publish nicht sauber.' }
        $changed=@(Get-ItoevaChangedPaths $Repository); if (($changed -join "`n") -ne ($allowedPaths -join "`n")) { throw 'Working-Tree-Pfade entsprechen nicht dem Plan.' }
        $tree=Get-ItoevaProposedTreeOid $Repository $baseSha $allowedPaths; if ($tree -ne $approvedTree) { throw 'Working Tree entspricht nicht dem freigegebenen Proposed Tree.' }
        & git -C $Repository diff-tree --check $baseSha $tree; if ($LASTEXITCODE -ne 0) { throw 'Proposed Tree besteht diff-tree --check nicht.' }
        if (Get-ItoevaRemoteSha $Repository 'origin' "refs/heads/$branch") { throw 'Remote-Evolution-Branch existiert bereits.' }
        $prepared=[ordered]@{ phase='PREPARED'; previousJournalHash=''; runId=$RunId; dryRunReportSha256=$dryRunHash; stateEvidenceSha256=$stateEvidence.hash; planReviewEvidenceSha256=$planReviewEvidence.hash; finalReviewEvidenceSha256=$finalReviewEvidence.hash;claudeReviewEvidenceSha256=$claudeReviewEvidence.hash;claudeContextEvidenceSha256=$claudeContextEvidence.hash;claudePatchEvidenceSha256=$claudePatchEvidence.hash; branch=$branch; baseSha=$baseSha; proposedTreeOid=$approvedTree; planHash=$planHash; testManifestHash=$testHash; title=[string]$plan.title; allowedPaths=$allowedPaths }
        $journal=Write-ItoevaPublishJournal $journalRoot 'PREPARED' $prepared
    } else {
        foreach ($binding in @('runId','branch','baseSha','proposedTreeOid','planHash','testManifestHash')) {
            $expectedValue = switch ($binding) { 'runId' {$RunId}; 'branch' {$branch}; 'baseSha' {$baseSha}; 'proposedTreeOid' {$approvedTree}; 'planHash' {$planHash}; 'testManifestHash' {$testHash} }
            if ([string]$journal.record.$binding -ne $expectedValue) { throw "Publish-Journalbindung stimmt nicht: $binding" }
        }
        if ([string]$journal.record.dryRunReportSha256 -ne $dryRunHash -or (@($journal.record.allowedPaths) -join "`n") -ne ($allowedPaths -join "`n")) { throw 'Publish-Journal stimmt nicht mit Dry-Run-Belegen ueberein.' }
        if ([string]$journal.record.stateEvidenceSha256 -ne $stateEvidence.hash -or [string]$journal.record.planReviewEvidenceSha256 -ne $planReviewEvidence.hash -or [string]$journal.record.finalReviewEvidenceSha256 -ne $finalReviewEvidence.hash -or [string]$journal.record.claudeReviewEvidenceSha256 -ne $claudeReviewEvidence.hash -or [string]$journal.record.claudeContextEvidenceSha256 -ne $claudeContextEvidence.hash -or [string]$journal.record.claudePatchEvidenceSha256 -ne $claudePatchEvidence.hash) { throw 'Publish-Journal stimmt nicht mit den Evidence-Snapshots ueberein.' }
    }

    if ($journal.phase -eq 'PREPARED') {
        $head=(& git -C $Repository rev-parse HEAD).Trim()
        if ($head -eq $baseSha) {
            & git -C $Repository diff --cached --quiet
            if ($LASTEXITCODE -eq 0) { & git -C $Repository add -- @allowedPaths | Out-Null; if ($LASTEXITCODE -ne 0) { throw 'Explizites Staging fehlgeschlagen.' } }
            else { & git -C $Repository diff --quiet; if ($LASTEXITCODE -ne 0) { throw 'Teilweise gestagter Resume-Zustand ist nicht eindeutig.' } }
            if ((& git -C $Repository write-tree).Trim() -ne $approvedTree) { throw 'Staged Tree entspricht nicht dem freigegebenen Tree.' }
            $number=($branch -split '[-/]')[1]; $message="$($Config.publication.commitMessagePrefix)$number`: $($journal.record.title)"
            & git -C $Repository -c "core.hooksPath=$TrustedHooksPath" commit -m $message | Out-Null; if ($LASTEXITCODE -ne 0) { throw 'Publish-Commit fehlgeschlagen.' }
            $head=(& git -C $Repository rev-parse HEAD).Trim()
        }
        $parent=(& git -C $Repository rev-parse "$head^").Trim(); $commitTree=(& git -C $Repository rev-parse "$head^{tree}").Trim()
        if ($parent -ne $baseSha -or $commitTree -ne $approvedTree -or @(& git -C $Repository status --porcelain).Count) { throw 'Lokaler Publish-Commit ist nicht exakt an Base und Tree gebunden.' }
        $committed=[ordered]@{ phase='COMMITTED'; previousJournalHash=$journal.hash; runId=$RunId; dryRunReportSha256=$dryRunHash; stateEvidenceSha256=$stateEvidence.hash; planReviewEvidenceSha256=$planReviewEvidence.hash; finalReviewEvidenceSha256=$finalReviewEvidence.hash;claudeReviewEvidenceSha256=$claudeReviewEvidence.hash;claudeContextEvidenceSha256=$claudeContextEvidence.hash;claudePatchEvidenceSha256=$claudePatchEvidence.hash; branch=$branch; baseSha=$baseSha; proposedTreeOid=$approvedTree; planHash=$planHash; testManifestHash=$testHash; title=[string]$journal.record.title; allowedPaths=$allowedPaths; commitSha=$head }
        $journal=Write-ItoevaPublishJournal $journalRoot 'COMMITTED' $committed
    }

    if ($journal.phase -eq 'COMMITTED') {
        $commitSha=[string]$journal.record.commitSha
        if ((& git -C $Repository rev-parse HEAD).Trim() -ne $commitSha -or (& git -C $Repository rev-parse "$commitSha^").Trim() -ne $baseSha -or (& git -C $Repository rev-parse "$commitSha^{tree}").Trim() -ne $approvedTree) { throw 'Commit-Resume-Bindung ist ungueltig.' }
        Assert-ItoevaBaseUnchanged $Repository $baseSha | Out-Null
        $remoteSha=Get-ItoevaRemoteSha $Repository 'origin' "refs/heads/$branch"
        if ($remoteSha -and $remoteSha -ne $commitSha) { throw 'Remote-Evolution-Branch zeigt auf einen fremden Commit.' }
        if (-not $remoteSha) { $pushArgs=New-ItoevaPushArguments $branch $commitSha $Config; & git -C $Repository -c "core.hooksPath=$TrustedHooksPath" @pushArgs | Out-Null; if ($LASTEXITCODE -ne 0) { throw 'Evolution-Branch-Push fehlgeschlagen.' } }
        $remoteSha=Get-ItoevaRemoteSha $Repository 'origin' "refs/heads/$branch"; if ($remoteSha -ne $commitSha) { throw 'Remote-SHA stimmt nicht mit Commit-SHA ueberein.' }
        Assert-ItoevaBaseUnchanged $Repository $baseSha | Out-Null
        $pushed=[ordered]@{ phase='PUSHED'; previousJournalHash=$journal.hash; runId=$RunId; dryRunReportSha256=$dryRunHash; stateEvidenceSha256=$stateEvidence.hash; planReviewEvidenceSha256=$planReviewEvidence.hash; finalReviewEvidenceSha256=$finalReviewEvidence.hash;claudeReviewEvidenceSha256=$claudeReviewEvidence.hash;claudeContextEvidenceSha256=$claudeContextEvidence.hash;claudePatchEvidenceSha256=$claudePatchEvidence.hash; branch=$branch; baseSha=$baseSha; proposedTreeOid=$approvedTree; planHash=$planHash; testManifestHash=$testHash; title=[string]$journal.record.title; allowedPaths=$allowedPaths; commitSha=$commitSha; remoteBranchSha=$remoteSha; publishedAt=[DateTimeOffset]::UtcNow.ToString('O') }
        $journal=Write-ItoevaPublishJournal $journalRoot 'PUSHED' $pushed
    }

    if ($journal.phase -eq 'PUSHED') {
        $commitSha=[string]$journal.record.commitSha; $remoteSha=Get-ItoevaRemoteSha $Repository 'origin' "refs/heads/$branch"
        if ($remoteSha -ne $commitSha) { throw 'Remote-SHA ging vor Reportabschluss verloren oder wurde veraendert.' }
        Assert-ItoevaBaseUnchanged $Repository $baseSha | Out-Null
        foreach ($property in @{ status='PUSHED'; commitSha=$commitSha; remoteBranchSha=$remoteSha; originMainPrePushSha=$baseSha; originMainPostPushSha=$baseSha; dryRunReportSha256=$dryRunHash; publishedAt=[string]$journal.record.publishedAt }.GetEnumerator()) { $report | Add-Member -NotePropertyName $property.Key -NotePropertyValue $property.Value -Force }
        $reportJson=$report|ConvertTo-Json -Depth 20; Write-ItoevaAtomicText $reportPath $reportJson; Write-ItoevaAtomicText $reportHashPath (Get-ItoevaSha256 $reportPath)
        Read-ItoevaHashedJson $reportPath $reportHashPath | Out-Null
        $reported=[ordered]@{ phase='REPORTED'; previousJournalHash=$journal.hash; runId=$RunId; dryRunReportSha256=$dryRunHash; stateEvidenceSha256=$stateEvidence.hash; planReviewEvidenceSha256=$planReviewEvidence.hash; finalReviewEvidenceSha256=$finalReviewEvidence.hash;claudeReviewEvidenceSha256=$claudeReviewEvidence.hash;claudeContextEvidenceSha256=$claudeContextEvidence.hash;claudePatchEvidenceSha256=$claudePatchEvidence.hash; branch=$branch; baseSha=$baseSha; proposedTreeOid=$approvedTree; planHash=$planHash; testManifestHash=$testHash; title=[string]$journal.record.title; allowedPaths=$allowedPaths; commitSha=$commitSha; remoteBranchSha=$remoteSha; publishedAt=[string]$journal.record.publishedAt }
        $journal=Write-ItoevaPublishJournal $journalRoot 'REPORTED' $reported
        $state.status='PUSHED'; Write-ItoevaAtomicJson $state $statePath
    }

    if ($journal.phase -ne 'REPORTED') { throw 'Publish-Journal erreichte nicht REPORTED.' }
    $publishedReport=Read-ItoevaHashedJson $reportPath $reportHashPath
    if ([string]$publishedReport.value.status -ne 'PUSHED' -or [string]$publishedReport.value.commitSha -ne [string]$journal.record.commitSha -or [string]$publishedReport.value.remoteBranchSha -ne [string]$journal.record.remoteBranchSha) { throw 'Finaler Publish-Report stimmt nicht mit dem Journal ueberein.' }
    if ((Get-ItoevaRemoteSha $Repository 'origin' "refs/heads/$branch") -ne [string]$journal.record.commitSha) { throw 'Finaler Remote-Branch stimmt nicht mit dem Journal ueberein.' }
    if ([string]$state.status -ne 'PUSHED') { $state.status='PUSHED'; Write-ItoevaAtomicJson $state $statePath }
    return [pscustomobject]@{ status='PUSHED'; runId=$RunId; branch=$branch; commitSha=[string]$journal.record.commitSha; remoteBranchSha=[string]$journal.record.remoteBranchSha }
}

Export-ModuleMember -Function @(
    'Get-ItoevaSha256', 'Test-ItoevaBranchName', 'Assert-ItoevaAllowedPaths',
    'New-ItoevaPushArguments', 'Test-ItoevaGate', 'Write-ItoevaAtomicJson',
    'Enter-ItoevaRunLock', 'Exit-ItoevaRunLock', 'Get-ItoevaDangerousGitConfig',
    'Get-ItoevaChangedPaths', 'Get-ItoevaSelectedTests', 'ConvertTo-ItoevaWindowsCommandLineArgument', 'New-ItoevaCmdShimCommand', 'Invoke-ItoevaProcessWithTimeout', 'Get-ItoevaProposedTreeOid', 'Get-ItoevaRemoteSha',
    'Resolve-ItoevaCodexLauncher', 'New-ItoevaCodexArguments', 'Assert-ItoevaCandidateAnalysis', 'Format-ItoevaEvolutionNumber',
    'Assert-ItoevaBaseUnchanged', 'Invoke-ItoevaConfiguredTests', 'Invoke-ItoevaCodexSession',
    'Publish-ItoevaEvolution', 'Test-ItoevaRunId', 'Read-ItoevaHashedJson', 'Write-ItoevaPublishJournal',
    'Get-ItoevaPublishJournal', 'Invoke-ItoevaPublishDryRun', 'Invoke-ItoevaGitProcess',
    'Get-ItoevaExpectedRebasedTree', 'Assert-ItoevaRevalidationWorktree', 'Invoke-ItoevaRevalidateDryRun',
    'Resolve-ItoevaClaudeLauncher','Assert-ItoevaClaudeCapabilityPreflight','Get-ItoevaClaudeReviewModel','Invoke-ItoevaProcessSeparated','ConvertFrom-ItoevaClaudeEnvelope','Assert-ItoevaClaudeReview','Invoke-ItoevaClaudeFinalReview',
    'ConvertTo-ItoevaNormalizedVersion','Assert-ItoevaClaudeConfigPins','Assert-ItoevaClaudeStoredProvenance','ConvertTo-ItoevaRedactedDiagnosticText','New-ItoevaClaudeEnvelopeSummary','Write-ItoevaClaudeDiagnostics'
)
