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

    $finished = [DateTimeOffset]::Now
    & $writeLog "Itoeva scheduled runner end=$($finished.ToString('O')) exitCode=$exitCode"
    $after = @(Get-ItoevaStateRunIds $stateRoot)
    $newIds = @($after | Where-Object { $before -notcontains $_ })
    $finalRunId = if ($newIds.Count -eq 1) { $newIds[0] } elseif ($newIds.Count -eq 0 -and $RunId -cmatch '^[0-9a-f]{32}$') { $RunId } else { 'no-run-id' }
} catch {
    if ($writer) { $writer.WriteLine((Protect-ItoevaLogText "wrapper-error=$($_.Exception.GetType().Name): $($_.Exception.Message)" @($ActivationPath))); $writer.Flush() }
    $exitCode = 1
    $finalRunId = 'no-run-id'
} finally {
    if ($writer) { $writer.Dispose() } elseif ($stream) { $stream.Dispose() }
}

if ($pendingPath -and (Test-Path -LiteralPath $pendingPath -PathType Leaf)) {
    try {
        Assert-ItoevaLogPath $localRoot $pendingPath | Out-Null
        Assert-ItoevaLogPath $localRoot $logRoot | Out-Null
        $finalPath = Assert-ItoevaLogPath $localRoot (Join-Path $logRoot "$stamp-$finalRunId.log")
        if (-not (Test-Path -LiteralPath $finalPath)) { Move-Item -LiteralPath $pendingPath -Destination $finalPath }
    } catch { }
}
exit $exitCode
