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

    $requiredPasses = @(
        $Gate.planReview,
        $Gate.mandatoryTests,
        $Gate.finalReview,
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

function Get-ItoevaProposedTreeOid {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)][string]$Repository,
        [Parameter(Mandatory)][string]$BaseSha,
        [Parameter(Mandatory)][string[]]$AllowedPaths
    )

    $indexPath = Join-Path ([IO.Path]::GetTempPath()) "itoeva-index-$([Guid]::NewGuid().ToString('N'))"
    $oldIndex = $env:GIT_INDEX_FILE
    try {
        $env:GIT_INDEX_FILE = $indexPath
        & git -C $Repository read-tree $BaseSha
        if ($LASTEXITCODE -ne 0) { throw 'Temporärer Git-Index konnte nicht initialisiert werden.' }
        & git -C $Repository add --all -- @AllowedPaths
        if ($LASTEXITCODE -ne 0) { throw 'Proposed Tree konnte nicht aufgebaut werden.' }
        $oid = (& git -C $Repository write-tree).Trim()
        if ($LASTEXITCODE -ne 0 -or $oid -notmatch '^[0-9a-f]{40,64}$') {
            throw 'Git Tree OID konnte nicht bestimmt werden.'
        }
        return $oid
    } finally {
        $env:GIT_INDEX_FILE = $oldIndex
        if (Test-Path -LiteralPath $indexPath) { Remove-Item -LiteralPath $indexPath -Force }
    }
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

Export-ModuleMember -Function @(
    'Get-ItoevaSha256', 'Test-ItoevaBranchName', 'Assert-ItoevaAllowedPaths',
    'New-ItoevaPushArguments', 'Test-ItoevaGate', 'Write-ItoevaAtomicJson',
    'Enter-ItoevaRunLock', 'Exit-ItoevaRunLock', 'Get-ItoevaDangerousGitConfig',
    'Get-ItoevaChangedPaths', 'Get-ItoevaSelectedTests', 'ConvertTo-ItoevaWindowsCommandLineArgument', 'New-ItoevaCmdShimCommand', 'Invoke-ItoevaProcessWithTimeout', 'Get-ItoevaProposedTreeOid', 'Get-ItoevaRemoteSha',
    'Resolve-ItoevaCodexLauncher', 'New-ItoevaCodexArguments',
    'Assert-ItoevaBaseUnchanged', 'Invoke-ItoevaConfiguredTests', 'Invoke-ItoevaCodexSession',
    'Publish-ItoevaEvolution'
)
