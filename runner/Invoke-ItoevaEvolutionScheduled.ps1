[CmdletBinding()]
param(
    [ValidateSet('ValidateConfiguration', 'Preflight', 'DryRun', 'Run', 'PublishDryRun', 'RevalidateDryRun')]
    [string]$Action = 'ValidateConfiguration',
    [string]$Repository,
    [string]$ActivationPath,
    [string]$RunId
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Protect-ItoevaLogText([AllowEmptyString()][string]$Text, [string[]]$SensitiveValues = @()) {
    $value = $Text
    foreach ($sensitiveValue in $SensitiveValues) {
        if (-not [string]::IsNullOrWhiteSpace($sensitiveValue)) {
            $value = [regex]::Replace($value, [regex]::Escape($sensitiveValue), '[REDACTED]', [Text.RegularExpressions.RegexOptions]::IgnoreCase)
        }
    }
    $value = [regex]::Replace($value, '(?i)\b(https?://)[^\s/@:]+:[^\s/@]+@', '$1[REDACTED]@')
    $value = [regex]::Replace($value, '(?i)\b(authorization\s*[:=]\s*(?:bearer|basic)?\s*)[^\s,;]+', '$1[REDACTED]')
    $value = [regex]::Replace($value, '(?i)\b(bearer|basic)\s+[A-Za-z0-9._~+/=-]{8,}', '$1 [REDACTED]')
    $value = [regex]::Replace($value, '(?i)(["''](?:token|password|passwd|secret|api[-_]?key|client[-_]?secret|access[-_]?key)["'']\s*:\s*["''])[^"'']*(["''])', '$1[REDACTED]$2')
    $value = [regex]::Replace($value, '(?i)\b([A-Z0-9_]*(?:TOKEN|PASSWORD|PASSWD|SECRET|API_KEY|CLIENT_SECRET|ACCESS_KEY)[A-Z0-9_]*)\s*=\s*[^\s,;]+', '$1=[REDACTED]')
    $value = [regex]::Replace($value, '(?i)\b(token|password|passwd|secret|api[-_]?key|client[-_]?secret)\s*[:=]\s*[^\s,;]+', '$1=[REDACTED]')
    $value = [regex]::Replace($value, '\b(?:gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,}|AKIA[0-9A-Z]{16})\b', '[REDACTED]')
    return $value
}

function Assert-ItoevaLogPath([string]$LocalRoot, [string]$Path) {
    $root = [IO.Path]::GetFullPath($LocalRoot).TrimEnd('\')
    $full = [IO.Path]::GetFullPath($Path)
    if ($full -ne $root -and -not $full.StartsWith($root + '\', [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Logging-Pfad liegt ausserhalb von LOCALAPPDATA.'
    }
    $cursor = $full
    while ($cursor -and $cursor.StartsWith($root, [StringComparison]::OrdinalIgnoreCase)) {
        if (Test-Path -LiteralPath $cursor) {
            $item = Get-Item -LiteralPath $cursor -Force
            if ($item.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw "Reparse-Point im Logging-Pfad ist nicht erlaubt: $cursor" }
        }
        if ($cursor.TrimEnd('\') -eq $root) { break }
        $cursor = Split-Path -Parent $cursor
    }
    return $full
}

function Get-ItoevaStateRunIds([string]$StateRoot) {
    if (-not (Test-Path -LiteralPath $StateRoot -PathType Container)) { return @() }
    return @(Get-ChildItem -LiteralPath $StateRoot -Directory -Force | Where-Object {
        $_.Name -cmatch '^[0-9a-f]{32}$' -and -not ($_.Attributes -band [IO.FileAttributes]::ReparsePoint)
    } | ForEach-Object { $_.Name })
}

function Get-ItoevaSha256([string]$Path) {
    $sha = [Security.Cryptography.SHA256]::Create()
    try { return ([BitConverter]::ToString($sha.ComputeHash([IO.File]::ReadAllBytes($Path))).Replace('-', '').ToLowerInvariant()) }
    finally { $sha.Dispose() }
}

function Get-ItoevaProperty($Object, [string]$Name, $Default = $null) {
    if ($null -ne $Object -and $Object.PSObject.Properties.Name -contains $Name) { return $Object.$Name }
    return $Default
}

function Protect-ItoevaSummaryText([AllowEmptyString()][string]$Text, [string[]]$SensitiveValues = @()) {
    $value = Protect-ItoevaLogText $Text $SensitiveValues
    $value = [regex]::Replace($value, '(?i)\bhttps?://[^\s]+', '[REDACTED-URL]')
    $value = [regex]::Replace($value, '(?i)(?:\$env:[A-Z_][A-Z0-9_]*|%[A-Z_][A-Z0-9_]*%)', '[REDACTED-ENV]')
    $value = [regex]::Replace($value, '(?i)\b[A-Z_][A-Z0-9_]{2,}\s*=\s*[^\s,;]+', '[REDACTED-ENV]')
    if ($value.Length -gt 500) { $value = $value.Substring(0, 500) }
    return $value
}

function Read-ItoevaJson([string]$Path) {
    $raw = [IO.File]::ReadAllText($Path, [Text.UTF8Encoding]::new($false))
    return ($raw | ConvertFrom-Json)
}

function New-ItoevaFailedSummary([DateTimeOffset]$CompletedAt, $RunId, [string]$LogPath, [string]$Reason, [string[]]$SensitiveValues, $State = $null) {
    $stateStatus = [string](Get-ItoevaProperty $State 'status' '')
    $status = if ($stateStatus -eq 'QUARANTINED') { 'QUARANTINED' } else { 'FAILED' }
    $stateReason = [string](Get-ItoevaProperty $State 'error' '')
    if (-not [string]::IsNullOrWhiteSpace($stateReason)) { $Reason = $stateReason }
    $base = [string](Get-ItoevaProperty $State 'baseSha' '')
    if ($base -cnotmatch '^[0-9a-f]{40,64}$') { $base = $null }
    $branch = [string](Get-ItoevaProperty $State 'branch' '')
    if ($branch -cnotmatch '^evolution/[0-9]{3}-[a-z0-9](?:[a-z0-9-]{0,119})-[0-9a-f]{32}$') { $branch = $null }
    return [ordered]@{
        generatedAt=[DateTimeOffset]::UtcNow.ToString('O'); completedAt=$CompletedAt.UtcDateTime.ToString('O')
        runId=$RunId; status=$status; branch=$branch; baseSha=$base; commitSha=$null; remoteBranchSha=$null
        planReviewStatus='UNKNOWN'; finalReviewStatus='UNKNOWN'; testStatus='UNKNOWN'
        errorReason=(Protect-ItoevaSummaryText $Reason $SensitiveValues); logPath=$LogPath
    }
}

function Get-ItoevaRunSummary([string]$RuntimeRoot, [AllowNull()][string]$RunId, [int]$ChildExitCode, [DateTimeOffset]$CompletedAt, [string]$LogPath, [string[]]$SensitiveValues) {
    if ($RunId -cnotmatch '^[0-9a-f]{32}$') {
        return New-ItoevaFailedSummary $CompletedAt $null $LogPath 'Run-ID konnte nicht eindeutig bestimmt werden.' $SensitiveValues
    }
    try {
        $runRoot = Assert-ItoevaLogPath $RuntimeRoot (Join-Path $RuntimeRoot "state\$RunId")
        $statePath = Assert-ItoevaLogPath $RuntimeRoot (Join-Path $runRoot 'state.json')
        $reportsRoot = Assert-ItoevaLogPath $RuntimeRoot (Join-Path $RuntimeRoot 'reports')
        $reportPath = Assert-ItoevaLogPath $RuntimeRoot (Join-Path $reportsRoot "$RunId.json")
        $hashPath = Assert-ItoevaLogPath $RuntimeRoot "$reportPath.sha256"
    } catch { return New-ItoevaFailedSummary $CompletedAt $RunId $LogPath 'Runtime-Pfad ist unsicher.' $SensitiveValues }
    $state = $null
    try {
        if (-not (Test-Path -LiteralPath $statePath -PathType Leaf)) { throw 'State fehlt.' }
        Assert-ItoevaLogPath $RuntimeRoot $statePath | Out-Null
        $state = Read-ItoevaJson $statePath
        if ($state -isnot [pscustomobject]) { throw 'State ist kein JSON-Objekt.' }
        if ([string](Get-ItoevaProperty $state 'runId' '') -cne $RunId) { throw 'State-Run-ID stimmt nicht.' }
    } catch { return New-ItoevaFailedSummary $CompletedAt $RunId $LogPath 'State fehlt oder ist ungültig.' $SensitiveValues $state }

    if ($ChildExitCode -ne 0) { return New-ItoevaFailedSummary $CompletedAt $RunId $LogPath 'Runner-Kindprozess ist fehlgeschlagen.' $SensitiveValues $state }
    try {
        foreach ($required in @($reportPath, $hashPath)) { if (-not (Test-Path -LiteralPath $required -PathType Leaf)) { throw 'Report oder SHA-256-Sidecar fehlt.' }; Assert-ItoevaLogPath $RuntimeRoot $required | Out-Null }
        $expectedHash = ([IO.File]::ReadAllText($hashPath, [Text.UTF8Encoding]::new($false))).Trim()
        if ($expectedHash -cnotmatch '^[0-9a-f]{64}$' -or (Get-ItoevaSha256 $reportPath) -cne $expectedHash) { throw 'Report-SHA-256 stimmt nicht.' }
        $report = Read-ItoevaJson $reportPath
        if ($report -isnot [pscustomobject]) { throw 'Report ist kein JSON-Objekt.' }
        $status = [string](Get-ItoevaProperty $report 'status' '')
        if ([string](Get-ItoevaProperty $report 'runId' '') -cne $RunId -or $status -notin @('PUSHED','QUARANTINED','DRY_RUN_PASS','NO_SAFE_EVOLUTION')) { throw 'Report-Identität oder Status ist ungültig.' }
        $base = [string](Get-ItoevaProperty $report 'baseSha' '')
        if ($base -cnotmatch '^[0-9a-f]{40,64}$') { throw 'Report-Base-SHA ist ungültig.' }
        $branch = [string](Get-ItoevaProperty $report 'branch' '')
        if (-not [string]::IsNullOrEmpty($branch) -and $branch -cnotmatch '^evolution/[0-9]{3}-[a-z0-9](?:[a-z0-9-]{0,119})-[0-9a-f]{32}$') { throw 'Report-Branch ist ungültig.' }
        if ([string](Get-ItoevaProperty $state 'baseSha' '') -cne $base -or [string](Get-ItoevaProperty $state 'branch' '') -cne $branch) { throw 'State und Report widersprechen sich.' }
        $stateStatus = [string](Get-ItoevaProperty $state 'status' '')
        if (($status -eq 'PUSHED' -and $stateStatus -notin @('DRY_RUN_PASS','PUSHED')) -or ($status -ne 'PUSHED' -and $stateStatus -cne $status)) { throw 'State- und Reportstatus widersprechen sich.' }

        $planReviewRaw=Get-ItoevaProperty $report 'planReview' $null;$finalReviewRaw=Get-ItoevaProperty $report 'finalReview' $null
        $planReview = if($planReviewRaw -is [string]){$planReviewRaw}else{'UNKNOWN'}
        $finalReview = if($finalReviewRaw -is [string]){$finalReviewRaw}else{'UNKNOWN'}
        if ($planReview -notin @('PASS','FAIL')) { $planReview = 'UNKNOWN' }
        if ($finalReview -notin @('PASS','FAIL')) { $finalReview = 'UNKNOWN' }
        $testStatus = 'UNKNOWN'; $commit = $null; $remote = $null
        if ($status -in @('PUSHED','DRY_RUN_PASS')) {
            if ($branch -cnotmatch '^evolution/[0-9]{3}-[a-z0-9](?:[a-z0-9-]{0,119})-[0-9a-f]{32}$') { throw 'Erfolgsreport enthält keinen gültigen Evolution-Branch.' }
            $tree = [string](Get-ItoevaProperty $report 'proposedTreeOid' '')
            $planHash = [string](Get-ItoevaProperty $report 'planHash' '')
            $bindings = Get-ItoevaProperty $report 'reviewerBindings' $null
            if ($tree -cnotmatch '^[0-9a-f]{40,64}$' -or $planHash -cnotmatch '^[0-9a-f]{64}$' -or
                [string](Get-ItoevaProperty $bindings 'baseSha' '') -cne $base -or [string](Get-ItoevaProperty $bindings 'planHash' '') -cne $planHash -or
                [string](Get-ItoevaProperty $bindings 'treeOid' '') -cne $tree) { throw 'Tree-, Plan- oder Reviewer-Bindungen sind ungültig.' }
            $hasFormatVersion = $report.PSObject.Properties.Name -contains 'formatVersion'
            if ($hasFormatVersion) {
                $formatVersion=Get-ItoevaProperty $report 'formatVersion' $null;$runKind=Get-ItoevaProperty $report 'runKind' $null
                if ($formatVersion -isnot [int] -or $formatVersion -ne 2 -or $runKind -isnot [string] -or $runKind -cne 'REVALIDATED_DRY_RUN') { throw 'Unbekanntes Reportformat.' }
                $source = Get-ItoevaProperty $report 'sourceBindings' $null
                if ([string](Get-ItoevaProperty $source 'sourceRunId' '') -cnotmatch '^[0-9a-f]{32}$') { throw 'V2-Source-Bindung fehlt.' }
                foreach($sourceHashName in @('sourceReportSha256','sourceStateSha256','sourcePlanSha256','sourcePlanReviewSha256','sourceTestsSha256','sourceFinalReviewSha256','revalidationSha256')) {
                    if ([string](Get-ItoevaProperty $source $sourceHashName '') -cnotmatch '^[0-9a-f]{64}$') { throw 'V2-Source-Hash ist ungültig.' }
                }
                foreach($sourceOidName in @('sourceBaseSha','sourceTreeOid')) {
                    if ([string](Get-ItoevaProperty $source $sourceOidName '') -cnotmatch '^[0-9a-f]{40,64}$') { throw 'V2-Source-OID ist ungültig.' }
                }
            } elseif ($report.PSObject.Properties.Name -contains 'runKind') { throw 'Unbekanntes Legacy-Reportformat.' }
            $testHash = [string](Get-ItoevaProperty $report 'testManifestHash' '')
            if ([string](Get-ItoevaProperty $bindings 'testManifestHash' '') -cne $testHash) { throw 'Reviewer-Testbindung stimmt nicht.' }
            $testsPath = Assert-ItoevaLogPath $RuntimeRoot (Join-Path $runRoot 'tests.json')
            if ($testHash -cnotmatch '^[0-9a-f]{64}$' -or -not (Test-Path -LiteralPath $testsPath -PathType Leaf) -or (Get-ItoevaSha256 $testsPath) -cne $testHash) { throw 'Testmanifest fehlt oder stimmt nicht.' }
            $parsedTests = Read-ItoevaJson $testsPath; $tests = @($parsedTests); $reportedTests = @(Get-ItoevaProperty $report 'tests' @())
            if ((ConvertTo-Json -InputObject $tests -Depth 20 -Compress) -cne (ConvertTo-Json -InputObject $reportedTests -Depth 20 -Compress)) { throw 'Report-Tests stimmen nicht mit tests.json überein.' }
            if ($tests.Count -eq 0) { throw 'Testmanifest ist leer.' }
            foreach($test in $tests) {
                if($test -isnot [pscustomobject]){throw 'Testrecord ist kein JSON-Objekt.'}
                $testId=Get-ItoevaProperty $test 'id' $null;$testResult=Get-ItoevaProperty $test 'status' $null
                $exitValue=Get-ItoevaProperty $test 'exitCode' $null;$timedOut=Get-ItoevaProperty $test 'timedOut' $null;$output=Get-ItoevaProperty $test 'output' $null
                if($testId -isnot [string] -or $testId -notmatch '^[A-Za-z0-9._-]{1,100}$' -or $testResult -isnot [string] -or $testResult -notin @('PASS','FAIL') -or $exitValue -isnot [int] -or $timedOut -isnot [bool] -or $output -isnot [string]) { throw 'Testrecord enthält ungültige Felder oder Typen.' }
            }
            $testStatus = if ($tests | Where-Object { [string](Get-ItoevaProperty $_ 'status' '') -ne 'PASS' }) { 'FAIL' } else { 'PASS' }
            if ($planReview -ne 'PASS' -or $finalReview -ne 'PASS' -or $testStatus -ne 'PASS') { throw 'Review- oder Test-Gates sind nicht PASS.' }
            $reportCommit = [string](Get-ItoevaProperty $report 'commitSha' '')
            $reportRemote = [string](Get-ItoevaProperty $report 'remoteBranchSha' '')
            if ($status -eq 'PUSHED') {
                if ($reportCommit -cnotmatch '^[0-9a-f]{40,64}$' -or $reportRemote -cne $reportCommit) { throw 'Publish-SHAs sind ungültig oder verschieden.' }
                $commit = $reportCommit; $remote = $reportRemote
            } elseif (-not [string]::IsNullOrEmpty($reportCommit) -or -not [string]::IsNullOrEmpty($reportRemote)) { throw 'Dry-Run enthält Publish-SHAs.' }
        }
        return [ordered]@{
            generatedAt=[DateTimeOffset]::UtcNow.ToString('O'); completedAt=$CompletedAt.UtcDateTime.ToString('O')
            runId=$RunId; status=$status; branch=if($branch){$branch}else{$null}; baseSha=$base; commitSha=$commit; remoteBranchSha=$remote
            planReviewStatus=$planReview; finalReviewStatus=$finalReview; testStatus=$testStatus
            errorReason=if($status -eq 'QUARANTINED'){Protect-ItoevaSummaryText ([string](Get-ItoevaProperty $state 'error' 'Runner wurde quarantänisiert.')) $SensitiveValues}else{$null}
            logPath=$LogPath
        }
    } catch { return New-ItoevaFailedSummary $CompletedAt $RunId $LogPath $_.Exception.Message $SensitiveValues $state }
}

function Write-ItoevaLatestSummary([string]$RuntimeRoot, $Summary) {
    $reportsRoot = Assert-ItoevaLogPath $RuntimeRoot (Join-Path $RuntimeRoot 'reports')
    if (-not (Test-Path -LiteralPath $reportsRoot)) { New-Item -ItemType Directory -Path $reportsRoot -Force | Out-Null }
    Assert-ItoevaLogPath $RuntimeRoot $reportsRoot | Out-Null
    $lockPath = Assert-ItoevaLogPath $RuntimeRoot (Join-Path $reportsRoot '.latest-summary.lock')
    if (Test-Path -LiteralPath $lockPath) { Assert-ItoevaLogPath $RuntimeRoot $lockPath | Out-Null }
    $lock = $null
    for ($attempt=0; $attempt -lt 100 -and -not $lock; $attempt++) {
        try { $lock=[IO.File]::Open($lockPath,[IO.FileMode]::OpenOrCreate,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None) }
        catch [IO.IOException] { if($attempt -eq 99){throw}; Start-Sleep -Milliseconds 50 }
    }
    try {
        $target = Assert-ItoevaLogPath $RuntimeRoot (Join-Path $reportsRoot 'latest-summary.json')
        if (Test-Path -LiteralPath $target) {
            Assert-ItoevaLogPath $RuntimeRoot $target | Out-Null
            try {
                $current=Read-ItoevaJson $target; $currentAt=[DateTimeOffset]::Parse([string](Get-ItoevaProperty $current 'completedAt' ''))
                $newAt=[DateTimeOffset]::Parse([string]$Summary.completedAt)
                $currentKey=('{0}|{1}' -f [string](Get-ItoevaProperty $current 'runId' ''),[string](Get-ItoevaProperty $current 'logPath' ''))
                $newKey=('{0}|{1}' -f [string]$Summary.runId,[string]$Summary.logPath)
                if($currentAt -gt $newAt -or ($currentAt -eq $newAt -and [string]::CompareOrdinal($currentKey,$newKey) -ge 0)){return}
            } catch { }
        }
        $temporary = Assert-ItoevaLogPath $RuntimeRoot (Join-Path $reportsRoot ".latest-summary.$([Guid]::NewGuid().ToString('N')).tmp")
        try {
            $file=[IO.File]::Open($temporary,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::Read)
            try { $writer=[IO.StreamWriter]::new($file,[Text.UTF8Encoding]::new($false));$writer.AutoFlush=$true;$writer.Write(($Summary|ConvertTo-Json -Depth 10));$writer.Flush();$file.Flush($true);$writer.Dispose();$file=$null }
            finally { if($file){$file.Dispose()} }
            Assert-ItoevaLogPath $RuntimeRoot $reportsRoot | Out-Null
            if(Test-Path -LiteralPath $target){
                Assert-ItoevaLogPath $RuntimeRoot $target|Out-Null
                $backup=Assert-ItoevaLogPath $RuntimeRoot (Join-Path $reportsRoot ".latest-summary.$([Guid]::NewGuid().ToString('N')).bak")
                try {[IO.File]::Replace($temporary,$target,$backup)}finally{if(Test-Path -LiteralPath $backup){Remove-Item -LiteralPath $backup -Force -ErrorAction SilentlyContinue}}
            }else{[IO.File]::Move($temporary,$target)}
        } finally { if(Test-Path -LiteralPath $temporary){Remove-Item -LiteralPath $temporary -Force -ErrorAction SilentlyContinue} }
    } finally { if($lock){$lock.Dispose()} }
}

if ([string]::IsNullOrWhiteSpace($env:LOCALAPPDATA)) { throw 'LOCALAPPDATA ist für Scheduler-Logging nicht gesetzt.' }
$localRoot = [IO.Path]::GetFullPath($env:LOCALAPPDATA)
$runtimeRoot = Assert-ItoevaLogPath $localRoot (Join-Path $localRoot 'ItoevaEvolutionRunner')
$logRoot = Assert-ItoevaLogPath $localRoot (Join-Path $runtimeRoot 'logs')
$stateRoot = Assert-ItoevaLogPath $localRoot (Join-Path $runtimeRoot 'state')
if (-not (Test-Path -LiteralPath $logRoot)) { New-Item -ItemType Directory -Path $logRoot -Force | Out-Null }
Assert-ItoevaLogPath $localRoot $logRoot | Out-Null

$started = [DateTimeOffset]::Now
$stamp = $started.ToString('yyyyMMdd-HHmmss.fff', [Globalization.CultureInfo]::InvariantCulture)
$pendingPath = $null
$stream = $null
$writer = $null
$exitCode = 1
$completedAt = $started
$finalRunId = 'no-run-id'
try {
    for ($attempt = 0; $attempt -lt 8 -and -not $stream; $attempt++) {
        $candidate = Assert-ItoevaLogPath $localRoot (Join-Path $logRoot "$stamp-pending-$([Guid]::NewGuid().ToString('N')).log")
        try { $stream = [IO.File]::Open($candidate, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::Read); $pendingPath = $candidate }
        catch [IO.IOException] { if ($attempt -eq 7) { throw } }
    }
    $writer = [IO.StreamWriter]::new($stream, [Text.UTF8Encoding]::new($false)); $writer.AutoFlush = $true
    $writeLog = { param([string]$Value) $writer.WriteLine((Protect-ItoevaLogText $Value @($ActivationPath))); $writer.Flush() }
    & $writeLog "Itoeva scheduled runner start=$($started.ToString('O')) action=$Action wrapperPid=$PID"

    $cutoff = [DateTime]::UtcNow.AddDays(-30)
    try { $rotationCandidates = @(Get-ChildItem -LiteralPath $logRoot -Filter '*.log' -File -Force -ErrorAction Stop) }
    catch { $rotationCandidates = @(); & $writeLog "rotation-warning=$($_.Exception.GetType().Name): Logaufzaehlung fehlgeschlagen." }
    foreach ($oldLog in $rotationCandidates) {
        if ($oldLog.FullName -eq $pendingPath -or $oldLog.LastWriteTimeUtc -ge $cutoff -or ($oldLog.Attributes -band [IO.FileAttributes]::ReparsePoint)) { continue }
        try { Assert-ItoevaLogPath $localRoot $oldLog.FullName | Out-Null; Remove-Item -LiteralPath $oldLog.FullName -Force }
        catch { & $writeLog "rotation-warning=$($_.Exception.GetType().Name): $($_.Exception.Message)" }
    }

    $before = @(Get-ItoevaStateRunIds $stateRoot)
    $runnerPath = Join-Path $PSScriptRoot 'Invoke-ItoevaEvolution.ps1'
    if (-not (Test-Path -LiteralPath $runnerPath -PathType Leaf)) { throw 'Itoeva Runner wurde neben dem Scheduler-Wrapper nicht gefunden.' }
    $powershellExe = (Get-Command powershell.exe -CommandType Application -ErrorAction Stop | Select-Object -First 1).Source
    $childArgs = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $runnerPath, '-Action', $Action)
    if (-not [string]::IsNullOrWhiteSpace($Repository)) { $childArgs += @('-Repository', $Repository) }
    if (-not [string]::IsNullOrWhiteSpace($ActivationPath)) { $childArgs += @('-ActivationPath', $ActivationPath) }
    if (-not [string]::IsNullOrWhiteSpace($RunId)) { $childArgs += @('-RunId', $RunId) }

    $oldPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        & $powershellExe @childArgs 2>&1 | ForEach-Object {
            $line = [string]$_
            & $writeLog "[output] $line"
            Write-Output $line
        }
        $exitCode = $LASTEXITCODE
    } finally { $ErrorActionPreference = $oldPreference }

    $finished = [DateTimeOffset]::Now; $completedAt = $finished
    & $writeLog "Itoeva scheduled runner end=$($finished.ToString('O')) exitCode=$exitCode"
    $after = @(Get-ItoevaStateRunIds $stateRoot)
    $newIds = @($after | Where-Object { $before -notcontains $_ })
    $finalRunId = if ($newIds.Count -eq 1) { $newIds[0] } elseif ($newIds.Count -eq 0 -and $RunId -cmatch '^[0-9a-f]{32}$') { $RunId } else { 'no-run-id' }
} catch {
    $completedAt = [DateTimeOffset]::Now
    if ($writer) { $writer.WriteLine((Protect-ItoevaLogText "wrapper-error=$($_.Exception.GetType().Name): $($_.Exception.Message)" @($ActivationPath))); $writer.Flush() }
    $exitCode = 1
    $finalRunId = 'no-run-id'
} finally {
    if ($writer) { $writer.Dispose() } elseif ($stream) { $stream.Dispose() }
}

$actualLogPath = $pendingPath
if ($pendingPath -and (Test-Path -LiteralPath $pendingPath -PathType Leaf)) {
    try {
        Assert-ItoevaLogPath $localRoot $pendingPath | Out-Null
        Assert-ItoevaLogPath $localRoot $logRoot | Out-Null
        $finalPath = Assert-ItoevaLogPath $localRoot (Join-Path $logRoot "$stamp-$finalRunId.log")
        if (-not (Test-Path -LiteralPath $finalPath)) { Move-Item -LiteralPath $pendingPath -Destination $finalPath; $actualLogPath = $finalPath }
    } catch { }
}

try {
    $summaryRunId = $finalRunId
    if ($summaryRunId -eq 'no-run-id' -and (Test-Path -LiteralPath $stateRoot -PathType Container)) {
        $recentIds = @(Get-ChildItem -LiteralPath $stateRoot -Directory -Force | Where-Object {
            $_.Name -cmatch '^[0-9a-f]{32}$' -and -not ($_.Attributes -band [IO.FileAttributes]::ReparsePoint) -and
            (Test-Path -LiteralPath (Join-Path $_.FullName 'state.json') -PathType Leaf) -and
            (Get-Item -LiteralPath (Join-Path $_.FullName 'state.json')).LastWriteTimeUtc -ge $started.UtcDateTime -and
            (Get-Item -LiteralPath (Join-Path $_.FullName 'state.json')).LastWriteTimeUtc -le $completedAt.UtcDateTime.AddSeconds(2)
        } | ForEach-Object { $_.Name })
        if ($recentIds.Count -eq 1) { $summaryRunId = $recentIds[0] }
    }
    $summary = Get-ItoevaRunSummary -RuntimeRoot $runtimeRoot -RunId $summaryRunId -ChildExitCode $exitCode -CompletedAt $completedAt -LogPath $actualLogPath -SensitiveValues @($ActivationPath)
    Write-ItoevaLatestSummary $runtimeRoot $summary
} catch {
    if ($actualLogPath -and (Test-Path -LiteralPath $actualLogPath -PathType Leaf)) {
        try {
            Assert-ItoevaLogPath $localRoot $actualLogPath | Out-Null
            $appendStream=[IO.File]::Open($actualLogPath,[IO.FileMode]::Append,[IO.FileAccess]::Write,[IO.FileShare]::Read)
            try { $appendWriter=[IO.StreamWriter]::new($appendStream,[Text.UTF8Encoding]::new($false));$appendWriter.WriteLine((Protect-ItoevaLogText "summary-warning=$($_.Exception.GetType().Name): $($_.Exception.Message)" @($ActivationPath)));$appendWriter.Flush();$appendStream.Flush($true);$appendWriter.Dispose();$appendStream=$null }
            finally { if($appendStream){$appendStream.Dispose()} }
        } catch { }
    }
}
exit $exitCode
