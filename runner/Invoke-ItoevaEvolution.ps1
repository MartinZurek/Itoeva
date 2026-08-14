[CmdletBinding()]
param(
    [ValidateSet('ValidateConfiguration', 'Preflight', 'DryRun', 'Run')]
    [string]$Action = 'ValidateConfiguration',
    [string]$Repository,
    [string]$ActivationPath
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Import-Module (Join-Path $PSScriptRoot 'Itoeva.Runner.psm1') -Force
if ([string]::IsNullOrWhiteSpace($Repository)) { $Repository = Split-Path -Parent $PSScriptRoot }
$Repository = (Resolve-Path -LiteralPath $Repository).Path
$config = Get-Content -Raw -LiteralPath (Join-Path $PSScriptRoot 'runner.config.json') | ConvertFrom-Json
if ($config.schemaVersion -ne 1) { throw 'Unbekannte Runner-Konfigurationsversion.' }
if ($config.agentExecution.enabled -or $config.publication.enabled) { throw 'Eingecheckte Aktivierungsflags müssen false sein.' }

function Assert-Preflight {
    $origin = (& git -C $Repository remote get-url origin).Trim()
    if ($LASTEXITCODE -ne 0 -or $origin -ne $config.repository.expectedOrigin) { throw "Unerwartetes origin: $origin" }
    if (@(& git -C $Repository status --porcelain).Count -ne 0) { throw 'Runner-Clone ist nicht sauber.' }
    $dangerous = @(Get-ItoevaDangerousGitConfig -Repository $Repository -Config $config)
    if ($dangerous.Count) { throw "Gefährliche Git-Konfiguration:`n$($dangerous -join "`n")" }
    & git -C $Repository fetch --prune origin
    if ($LASTEXITCODE -ne 0) { throw 'Fetch fehlgeschlagen.' }
    $base = Get-ItoevaRemoteSha -Repository $Repository -Ref 'refs/heads/main'
    if ((& git -C $Repository rev-parse refs/remotes/origin/main).Trim() -ne $base) { throw 'Tracking-Ref ist nicht synchron.' }
    return $base
}

function Read-Activation([bool]$NeedsPublication) {
    if (-not $ActivationPath -or -not (Test-Path -LiteralPath $ActivationPath -PathType Leaf)) { throw 'Lokale Aktivierungsdatei fehlt.' }
    $value = Get-Content -Raw -LiteralPath $ActivationPath | ConvertFrom-Json
    if (-not $value.agentExecutionEnabled) { throw 'Agentenausführung ist nicht autorisiert.' }
    if ($NeedsPublication -and (-not $value.publicationEnabled -or -not $value.standingAuthorizationEvolutionOnly)) {
        throw 'Evolution-Commit/-Push ist nicht autorisiert.'
    }
    return $value
}

function Get-NextEvolutionNumber {
    $numbers = @()
    Get-ChildItem (Join-Path $Repository 'evolutions') -Filter '[0-9][0-9][0-9]-*.md' -ErrorAction SilentlyContinue | ForEach-Object {
        if ($_.Name -match '^(\d{3})-') { $numbers += [int]$Matches[1] }
    }
    @(& git -C $Repository ls-remote --heads origin 'refs/heads/evolution/*') | ForEach-Object {
        if ($_ -match 'refs/heads/evolution/(\d{3})-') { $numbers += [int]$Matches[1] }
    }
    $next = if ($numbers.Count) { ($numbers | Measure-Object -Maximum).Maximum + 1 } else { 1 }
    return Format-ItoevaEvolutionNumber $next
}

if ($Action -eq 'ValidateConfiguration') {
    [ordered]@{ action=$Action; repository=$Repository; configuration='PASS'; agentExecution='DISABLED'; publication='DISABLED'; mainRuleset='RISK_ACCEPTED_NOT_ENFORCEABLE' } | ConvertTo-Json
    exit 0
}

$baseSha = Assert-Preflight
if ($Action -eq 'Preflight') {
    [ordered]@{ action=$Action; repository=$Repository; baseSha=$baseSha; status='PASS' } | ConvertTo-Json
    exit 0
}

$activation = Read-Activation -NeedsPublication ($Action -eq 'Run')
$runtimeRoot = [Environment]::ExpandEnvironmentVariables([string]$activation.runtimeRoot)
$config | Add-Member -NotePropertyName runtimeRoot -NotePropertyValue $runtimeRoot -Force
$lockPath = Join-Path $runtimeRoot 'locks\runner.lock'
$lock = Enter-ItoevaRunLock -Path $lockPath
$runId = [Guid]::NewGuid().ToString('N')
$runRoot = Join-Path $runtimeRoot "state\$runId"
$statePath = Join-Path $runRoot 'state.json'
$reportPath = Join-Path $runtimeRoot "reports\$runId.json"
$state = [ordered]@{ runId=$runId; phase='ANALYZE'; baseSha=$baseSha; branch=''; status='RUNNING' }

try {
    Write-ItoevaAtomicJson $state $statePath
    & git -C $Repository switch --detach $baseSha
    if ($LASTEXITCODE -ne 0) { throw 'Detached Base-Checkout fehlgeschlagen.' }

    $analysisPath = Join-Path $runRoot 'analysis.json'
    $analysisPrompt = (Get-Content -Raw (Join-Path $PSScriptRoot 'prompts\analyze.md')) + "`nBase-SHA: $baseSha"
    $implementer = Invoke-ItoevaCodexSession $Repository $analysisPrompt (Join-Path $PSScriptRoot 'schemas\candidate.schema.json') $analysisPath 'read-only' -TimeoutSeconds ([int]$config.agentExecution.sessionTimeoutSeconds)
    $analysis = Get-Content -Raw $analysisPath | ConvertFrom-Json
    Assert-ItoevaCandidateAnalysis $analysis
    if ($analysis.baseSha -ne $baseSha) { throw 'Analyse ist nicht an den aktuellen Base-SHA gebunden.' }
    if ($analysis.status -eq 'NO_SAFE_EVOLUTION') { $state.status='NO_SAFE_EVOLUTION'; return }
    $eligible = @($analysis.candidates | Where-Object { $_.risk -eq 'LOW' -and -not $_.protectedArea -and -not $_.openDecision })
    foreach ($item in $eligible) { Assert-ItoevaAllowedPaths @($item.paths) $config }
    if (-not $eligible.Count) { $state.status='NO_SAFE_EVOLUTION'; return }
    $candidate = $eligible | Sort-Object @{Expression={$_.paths.Count}}, title | Select-Object -First 1
    Assert-ItoevaBaseUnchanged $Repository $baseSha | Out-Null
    $number = Get-NextEvolutionNumber
    $branch = "evolution/$number-$($candidate.slug)-$runId"
    if (-not (Test-ItoevaBranchName $branch $config)) { throw 'Erzeugter Branch ist ungültig.' }
    if (Get-ItoevaRemoteSha $Repository 'origin' "refs/heads/$branch") { throw 'Remote-Branch existiert bereits.' }
    & git -C $Repository switch -c $branch $baseSha
    if ($LASTEXITCODE -ne 0) { throw 'Branchanlage fehlgeschlagen.' }
    $state.branch=$branch; $state.phase='PLAN'; Write-ItoevaAtomicJson $state $statePath

    $planPath = Join-Path $runRoot 'plan.json'
    $planPrompt = "Erstelle einen konkreten Plan für:`n$($candidate | ConvertTo-Json -Depth 10)"
    Invoke-ItoevaCodexSession $Repository $planPrompt (Join-Path $PSScriptRoot 'schemas\plan.schema.json') $planPath 'read-only' $implementer.sessionId -TimeoutSeconds ([int]$config.agentExecution.sessionTimeoutSeconds) | Out-Null
    $plan = Get-Content -Raw $planPath | ConvertFrom-Json
    if ($plan.status -ne 'PLANNED' -or $plan.baseSha -ne $baseSha) { throw 'Plan ist abgebrochen oder an den falschen Base-SHA gebunden.' }
    if ((& git -C $Repository rev-parse HEAD).Trim() -ne $baseSha -or @(& git -C $Repository status --porcelain).Count -ne 0) {
        throw 'Planphase hat den sauberen Base-Zustand verändert.'
    }
    Assert-ItoevaAllowedPaths @($plan.paths) $config
    $planHash = Get-ItoevaSha256 $planPath
    Assert-ItoevaBaseUnchanged $Repository $baseSha | Out-Null

    $planReviewPath = Join-Path $runRoot 'plan-review.json'
    $reviewPrompt = (Get-Content -Raw (Join-Path $PSScriptRoot 'prompts\review-plan.md')) + "`nBase: $baseSha`nPlan-Hash: $planHash`n$($plan | ConvertTo-Json -Depth 10)"
    $reviewer = Invoke-ItoevaCodexSession $Repository $reviewPrompt (Join-Path $PSScriptRoot 'schemas\plan-review.schema.json') $planReviewPath 'read-only' -TimeoutSeconds ([int]$config.agentExecution.sessionTimeoutSeconds)
    $planReview = Get-Content -Raw $planReviewPath | ConvertFrom-Json
    if ($planReview.status -ne 'PASS' -or $planReview.baseSha -ne $baseSha -or $planReview.planHash -ne $planHash) { throw 'Planreview ist nicht gültig PASS.' }
    if ((& git -C $Repository rev-parse HEAD).Trim() -ne $baseSha -or @(& git -C $Repository status --porcelain).Count -ne 0) {
        throw 'Planreview hat den sauberen Base-Zustand verändert.'
    }

    $state.phase='IMPLEMENT'; Write-ItoevaAtomicJson $state $statePath
    $implementationPath = Join-Path $runRoot 'implementation.json'
    $implementationPrompt = (Get-Content -Raw (Join-Path $PSScriptRoot 'prompts\implement.md')) + "`nPlan: $($plan | ConvertTo-Json -Depth 10)"
    Invoke-ItoevaCodexSession $Repository $implementationPrompt (Join-Path $PSScriptRoot 'schemas\implementation.schema.json') $implementationPath 'workspace-write' $implementer.sessionId -TimeoutSeconds ([int]$config.agentExecution.sessionTimeoutSeconds) | Out-Null
    $implementation = Get-Content -Raw $implementationPath | ConvertFrom-Json
    if ($implementation.status -ne 'IMPLEMENTED' -or $implementation.baseSha -ne $baseSha) { throw 'Implementierung ist abgebrochen oder an den falschen Base-SHA gebunden.' }
    Assert-ItoevaBaseUnchanged $Repository $baseSha | Out-Null
    if ((& git -C $Repository rev-parse HEAD).Trim() -ne $baseSha) { throw 'Implementer hat HEAD verändert.' }
    & git -C $Repository diff --cached --quiet
    if ($LASTEXITCODE -ne 0) { throw 'Implementer hat den Index verändert; Dry-Run wird quarantänisiert.' }
    $changedPaths = @(Get-ItoevaChangedPaths $Repository)
    $reportedPaths = @($implementation.changedPaths | Sort-Object -Unique)
    if (($reportedPaths -join "`n") -ne ($changedPaths -join "`n")) { throw 'Implementierungsbericht entspricht nicht dem Working Tree.' }
    Assert-ItoevaAllowedPaths $changedPaths $config
    $plannedPaths = @($plan.paths | Sort-Object -Unique)
    if (($changedPaths -join "`n") -ne ($plannedPaths -join "`n")) { throw 'Geänderte Pfade entsprechen nicht dem Plan.' }
    $treeOid = Get-ItoevaProposedTreeOid $Repository $baseSha $changedPaths
    & git -C $Repository diff-tree --check $baseSha $treeOid
    if ($LASTEXITCODE -ne 0) { throw 'Proposed Tree besteht git diff-tree --check nicht.' }

    $state.phase='TEST'; Write-ItoevaAtomicJson $state $statePath
    $testResults = @(Invoke-ItoevaConfiguredTests $Repository $config $changedPaths)
    $testManifestPath = Join-Path $runRoot 'tests.json'
    Write-ItoevaAtomicJson $testResults $testManifestPath
    if ($testResults | Where-Object { $_.status -ne 'PASS' }) { throw 'Pflichttest fehlgeschlagen.' }
    $testManifestHash = Get-ItoevaSha256 $testManifestPath
    Assert-ItoevaBaseUnchanged $Repository $baseSha | Out-Null

    $state.phase='FINAL_REVIEW'; Write-ItoevaAtomicJson $state $statePath
    $finalReviewPath = Join-Path $runRoot 'final-review.json'
    $finalPrompt = (Get-Content -Raw (Join-Path $PSScriptRoot 'prompts\review-final.md')) + "`nBase: $baseSha`nPlan-Pfad: $planPath`nPlan-Hash: $planHash`nTree: $treeOid`nTestmanifest-Pfad: $testManifestPath`nTestmanifest-Hash: $testManifestHash"
    Invoke-ItoevaCodexSession $Repository $finalPrompt (Join-Path $PSScriptRoot 'schemas\review.schema.json') $finalReviewPath 'read-only' $reviewer.sessionId -TimeoutSeconds ([int]$config.agentExecution.sessionTimeoutSeconds) | Out-Null
    $finalReview = Get-Content -Raw $finalReviewPath | ConvertFrom-Json
    $gate = [pscustomobject]@{
        planReview=$planReview.status; mandatoryTests='PASS'; finalReview=$finalReview.status; diffCheck='PASS'; baseUnchanged=$true
        baseSha=$baseSha; planHash=$planHash; proposedTreeOid=$treeOid; testManifestHash=$testManifestHash
        reviewBaseSha=$finalReview.baseSha; reviewPlanHash=$finalReview.planHash; reviewTreeOid=$finalReview.treeOid; reviewTestManifestHash=$finalReview.testManifestHash
    }
    if (-not (Test-ItoevaGate $gate)) { throw 'Finales Gate ist nicht PASS und hashgebunden.' }

    $publication = $null
    if ($Action -eq 'Run') {
        $config.publication.enabled = $true
        $hooks = [Environment]::ExpandEnvironmentVariables([string]$config.git.trustedHooksDirectory)
        $publication = Publish-ItoevaEvolution $Repository $branch $baseSha $plan.title $changedPaths $treeOid $gate $config $hooks
    }
    $state.phase='COMPLETE'; $state.status=if ($publication) {$publication.status} else {'DRY_RUN_PASS'}
    $report = [ordered]@{
        runId=$runId; status=$state.status; branch=$branch; baseSha=$baseSha
        commitSha=if ($publication) {$publication.commitSha} else {''}; remoteBranchSha=if ($publication) {$publication.remoteBranchSha} else {''}
        originMainStartSha=$baseSha; originMainPrePushSha=$baseSha; originMainPostPushSha=if ($publication) {$publication.originMainPostPushSha} else {(Get-ItoevaRemoteSha $Repository)}
        planReview=$planReview.status; finalReview=$finalReview.status; proposedTreeOid=$treeOid; testManifestHash=$testManifestHash
        planHash=$planHash; reviewerBindings=[ordered]@{ baseSha=$finalReview.baseSha; planHash=$finalReview.planHash; treeOid=$finalReview.treeOid; testManifestHash=$finalReview.testManifestHash }
        tests=$testResults; unverified=@(); knownRisks=@($config.knownRisks.mainRulesetNote)
    }
    Write-ItoevaAtomicJson $report $reportPath
    [IO.File]::WriteAllText("$reportPath.sha256", (Get-ItoevaSha256 $reportPath), [Text.UTF8Encoding]::new($false))
} catch {
    $state.status='QUARANTINED'; $state['error']=$_.Exception.Message
    throw
} finally {
    Write-ItoevaAtomicJson $state $statePath
    Exit-ItoevaRunLock $lock $lockPath
}
