[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Import-Module (Join-Path $PSScriptRoot 'Itoeva.Runner.psm1') -Force
$config = Get-Content -Raw -LiteralPath (Join-Path $PSScriptRoot 'runner.config.json') | ConvertFrom-Json

$script:passed = 0
$script:failed = 0

function Test-Case([string]$Name, [scriptblock]$Body) {
    try {
        & $Body
        $script:passed++
        Write-Host "PASS $Name"
    } catch {
        $script:failed++
        Write-Host "FAIL $Name - $($_.Exception.Message)"
    }
}

function Assert-True([bool]$Condition, [string]$Message = 'Erwartung nicht erfüllt') {
    if (-not $Condition) { throw $Message }
}

function Assert-Throws([scriptblock]$Body) {
    $thrown = $false
    try { & $Body } catch { $thrown = $true }
    if (-not $thrown) { throw 'Erwartete Exception wurde nicht ausgelöst.' }
}

function New-PublishDryRunFixture([string]$Root, [string]$RunId, $TestResults = $null) {
    $remote=Join-Path $Root 'remote.git'; $seed=Join-Path $Root 'seed'; $repository=Join-Path $Root 'repository'
    $runtime=Join-Path $Root 'runtime'; $hooks=Join-Path $Root 'hooks'; $runRoot=Join-Path $runtime "state\$RunId"; $reports=Join-Path $runtime 'reports'
    New-Item -ItemType Directory -Path $seed,$hooks,$runRoot,$reports | Out-Null
    & git init --bare --quiet $remote
    & git -C $seed init --quiet
    & git -C $seed config user.name 'Itoeva Publish Test'
    & git -C $seed config user.email 'publish-test@example.invalid'
    [IO.File]::WriteAllText((Join-Path $seed 'source.txt'), "base`n")
    & git -C $seed add -- source.txt; & git -C $seed commit --quiet -m baseline
    & git -C $seed remote add origin $remote; & git -C $seed push --quiet origin HEAD:refs/heads/main
    & git clone --quiet --branch main $remote $repository
    & git -C $repository config user.name 'Itoeva Publish Test'; & git -C $repository config user.email 'publish-test@example.invalid'
    $base=(& git -C $repository rev-parse HEAD).Trim(); $branch="evolution/002-publish-dry-run-$RunId"
    & git -C $repository switch --quiet -c $branch $base
    [IO.File]::WriteAllText((Join-Path $repository 'source.txt'), "evolved`n")
    $paths=@('source.txt'); $tree=Get-ItoevaProposedTreeOid $repository $base $paths
    $plan=[ordered]@{ status='PLANNED'; baseSha=$base; title='Publish dry run'; plan=@('change source'); paths=$paths; tests=@('verify') }
    $planPath=Join-Path $runRoot 'plan.json'; Write-ItoevaAtomicJson $plan $planPath; $planHash=Get-ItoevaSha256 $planPath
    Write-ItoevaAtomicJson ([ordered]@{ status='PASS'; baseSha=$base; planHash=$planHash; findings=@() }) (Join-Path $runRoot 'plan-review.json')
    $tests = if ($null -eq $TestResults) {
        @([pscustomobject]@{ id='verify'; status='PASS'; exitCode=0; startedAt='2026-01-01T00:00:00Z'; timedOut=$false; finishedAt='2026-01-01T00:00:01Z'; output='offline' })
    } else { @($TestResults) }
    $testsPath=Join-Path $runRoot 'tests.json'; Write-ItoevaAtomicJson $tests $testsPath; $testHash=Get-ItoevaSha256 $testsPath
    Write-ItoevaAtomicJson ([ordered]@{ status='PASS'; baseSha=$base; planHash=$planHash; treeOid=$tree; testManifestHash=$testHash; findings=@() }) (Join-Path $runRoot 'final-review.json')
    Write-ItoevaAtomicJson ([ordered]@{ runId=$RunId; phase='COMPLETE'; baseSha=$base; branch=$branch; status='DRY_RUN_PASS' }) (Join-Path $runRoot 'state.json')
    $report=[ordered]@{ runId=$RunId; status='DRY_RUN_PASS'; branch=$branch; baseSha=$base; commitSha=''; remoteBranchSha=''; originMainStartSha=$base; originMainPrePushSha=$base; originMainPostPushSha=$base; planReview='PASS'; finalReview='PASS'; proposedTreeOid=$tree; testManifestHash=$testHash; planHash=$planHash; reviewerBindings=[ordered]@{baseSha=$base;planHash=$planHash;treeOid=$tree;testManifestHash=$testHash}; tests=$tests; unverified=@(); knownRisks=@() }
    $reportPath=Join-Path $reports "$RunId.json"; Write-ItoevaAtomicJson $report $reportPath
    [IO.File]::WriteAllText("$reportPath.sha256", (Get-ItoevaSha256 $reportPath), [Text.UTF8Encoding]::new($false))
    $fixtureConfig=Get-Content -Raw -LiteralPath (Join-Path $PSScriptRoot 'runner.config.json')|ConvertFrom-Json
    $fixtureConfig.repository.expectedOrigin=$remote; $fixtureConfig.publication.enabled=$true
    $fixtureConfig|Add-Member -NotePropertyName runtimeRoot -NotePropertyValue $runtime -Force
    return [pscustomobject]@{ remote=$remote; seed=$seed; repository=$repository; runtime=$runtime; hooks=$hooks; runRoot=$runRoot; reportPath=$reportPath; runId=$RunId; base=$base; branch=$branch; tree=$tree; config=$fixtureConfig }
}

function Set-PublishFixtureReportTests($Fixture, $Tests) {
    $report=Get-Content -Raw -LiteralPath $Fixture.reportPath|ConvertFrom-Json
    $report.tests=@($Tests)
    Write-ItoevaAtomicJson $report $Fixture.reportPath
    [IO.File]::WriteAllText("$($Fixture.reportPath).sha256", (Get-ItoevaSha256 $Fixture.reportPath), [Text.UTF8Encoding]::new($false))
}

function Set-PublishFixtureResumePhase($Fixture, [string]$Phase, [bool]$RestoreDryRunReport) {
    $rank=@{PREPARED=1;COMMITTED=2;PUSHED=3;REPORTED=4}
    $phaseByRank=@{'1'='PREPARED';'2'='COMMITTED';'3'='PUSHED';'4'='REPORTED'}
    Get-ChildItem -LiteralPath (Join-Path $Fixture.runRoot 'publish-journal') -Filter 'p.*.json' -File | ForEach-Object {
        if ($_.Name -match '^p\.([1-4])\.' -and $rank[$phaseByRank[$Matches[1]]] -gt $rank[$Phase]) { Remove-Item -LiteralPath $_.FullName -Force }
    }
    if ($RestoreDryRunReport) {
        $snapshot="$($Fixture.reportPath.Substring(0,$Fixture.reportPath.Length-5)).dry-run.json"
        [IO.File]::WriteAllBytes($Fixture.reportPath, [IO.File]::ReadAllBytes($snapshot))
        [IO.File]::WriteAllText("$($Fixture.reportPath).sha256", (Get-ItoevaSha256 $Fixture.reportPath), [Text.UTF8Encoding]::new($false))
    }
    Write-ItoevaAtomicJson ([ordered]@{runId=$Fixture.runId;phase='COMPLETE';baseSha=$Fixture.base;branch=$Fixture.branch;status='DRY_RUN_PASS'}) (Join-Path $Fixture.runRoot 'state.json')
}

function New-RevalidateDryRunFixture([string]$Root, [string]$RunId, [bool]$Conflict = $false) {
    $fixture=New-PublishDryRunFixture $Root $RunId
    $sourceReportSnapshot="$($fixture.reportPath.Substring(0,$fixture.reportPath.Length-5)).dry-run.json"
    [IO.File]::WriteAllBytes($sourceReportSnapshot,[IO.File]::ReadAllBytes($fixture.reportPath));[IO.File]::WriteAllText("$sourceReportSnapshot.sha256",(Get-ItoevaSha256 $sourceReportSnapshot),[Text.UTF8Encoding]::new($false))
    foreach($name in @('state','plan-review','final-review')){
        $source=Join-Path $fixture.runRoot "$name.json";$snapshot=Join-Path $fixture.runRoot "$name.dry-run.json"
        [IO.File]::WriteAllBytes($snapshot,[IO.File]::ReadAllBytes($source));[IO.File]::WriteAllText("$snapshot.sha256",(Get-ItoevaSha256 $snapshot),[Text.UTF8Encoding]::new($false))
    }
    $evolved=Get-Content -Raw -LiteralPath (Join-Path $fixture.repository 'source.txt')
    [IO.File]::WriteAllText((Join-Path $fixture.seed $(if($Conflict){'source.txt'}else{'main.txt'})), $(if($Conflict){"upstream`n"}else{"new main`n"}))
    & git -C $fixture.seed add --all|Out-Null; & git -C $fixture.seed commit --quiet -m advance-main|Out-Null; & git -C $fixture.seed push --quiet origin HEAD:refs/heads/main|Out-Null
    & git -C $fixture.repository restore --worktree -- source.txt|Out-Null
    & git -C $fixture.repository fetch --quiet origin|Out-Null
    & git -C $fixture.repository switch --quiet --detach refs/remotes/origin/main|Out-Null
    & git -C $fixture.repository branch -f $fixture.branch refs/remotes/origin/main|Out-Null
    & git -C $fixture.repository switch --quiet $fixture.branch|Out-Null
    [IO.File]::WriteAllText((Join-Path $fixture.repository 'source.txt'),$evolved)
    $fixture.config.agentExecution.enabled=$true; $fixture.config.publication.enabled=$false
    $fixture.config.tests.android=@(); $fixture.config.tests.mandatory=@()
    $fixture|Add-Member -NotePropertyName newBase -NotePropertyValue ((& git -C $fixture.repository rev-parse HEAD).Trim())
    return $fixture
}

function New-RevalidateCodexDouble([string]$Root, [string]$MarkerPath) {
    New-Item -ItemType Directory -Path $Root -Force|Out-Null
    $sourcePath=Join-Path $Root 'RevalidateCodex.cs';$nativePath=Join-Path $Root 'codex.exe'
    $source=@'
using System;
using System.IO;
using System.Text.RegularExpressions;
public static class RevalidateCodex {
  static string Find(string text,string name){var m=Regex.Match(text,"(?m)^"+Regex.Escape(name)+@"\s*([0-9a-f]{40,64})\s*$");if(!m.Success)throw new Exception("missing "+name);return m.Groups[1].Value;}
  public static int Main(string[] args){
    string output=null;for(int i=0;i<args.Length-1;i++)if(args[i]=="-o")output=args[i+1];
    var prompt=Console.In.ReadToEnd();var marker=Environment.GetEnvironmentVariable("ITOEVA_REVALIDATE_CODEX_MARKER");
    File.AppendAllText(marker,string.Join("\t",args)+Environment.NewLine);
    var json="{\"status\":\"PASS\",\"baseSha\":\""+Find(prompt,"Base:")+"\",\"planHash\":\""+Find(prompt,"Source-Plan-Hash:")+"\",\"treeOid\":\""+Find(prompt,"Tree:")+"\",\"testManifestHash\":\""+Find(prompt,"Testmanifest-Hash:")+"\",\"findings\":[]}";
    File.WriteAllText(output,json);Console.WriteLine("{\"thread_id\":\"0123456789abcdef0123456789abcdef\"}");return 0;
  }
}
'@
    [IO.File]::WriteAllText($sourcePath,$source,[Text.UTF8Encoding]::new($false));$compiler=Join-Path ([Runtime.InteropServices.RuntimeEnvironment]::GetRuntimeDirectory()) 'csc.exe'
    & $compiler /nologo /target:exe "/out:$nativePath" $sourcePath
    if($LASTEXITCODE -ne 0){throw 'Revalidate-Codex-Testdouble konnte nicht erstellt werden.'}
    return $nativePath
}

$validBranch = 'evolution/002-small-fix-0123456789abcdef0123456789abcdef'

Test-Case 'gültiger Evolution-Branch' {
    Assert-True (Test-ItoevaBranchName -Branch $validBranch -Config $config)
}
Test-Case 'main ist als Ziel ungültig' {
    Assert-True (-not (Test-ItoevaBranchName -Branch 'main' -Config $config))
}
Test-Case 'Force-Option kommt in Pushargumenten nicht vor' {
    $commit = 'a' * 40
    $args = New-ItoevaPushArguments -Branch $validBranch -CommitSha $commit -Config $config
    Assert-True (($args -join ' ') -eq "push --porcelain origin $commit`:refs/heads/$validBranch")
    Assert-True (-not (($args -join ' ') -match '(?i)force'))
}
Test-Case 'Runner-Infrastruktur ist autonom gesperrt' {
    Assert-Throws { Assert-ItoevaAllowedPaths -Paths @('runner/Invoke-ItoevaEvolution.ps1') -Config $config }
}
Test-Case 'Build-Infrastruktur ist autonom gesperrt' {
    Assert-Throws { Assert-ItoevaAllowedPaths -Paths @('app-sim/build.gradle.kts') -Config $config }
}
Test-Case 'Produktionsdateien sind autonom gesperrt' {
    Assert-Throws { Assert-ItoevaAllowedPaths -Paths @('app/google-services.json') -Config $config }
}
Test-Case 'normale Quelldatei ist zulässig' {
    Assert-ItoevaAllowedPaths -Paths @('app-sim/src/main/java/example/Test.kt') -Config $config
}
Test-Case 'Traversal wird vor Normalisierung abgewiesen' {
    Assert-Throws { Assert-ItoevaAllowedPaths -Paths @('../app-sim/src/main/java/example/Test.kt') -Config $config }
}
Test-Case 'absolute Pfade werden abgewiesen' {
    Assert-Throws { Assert-ItoevaAllowedPaths -Paths @('C:\Notime\app-sim\Test.kt') -Config $config }
}
Test-Case 'ausführbare Skripte sind autonom gesperrt' {
    Assert-Throws { Assert-ItoevaAllowedPaths -Paths @('tools/new-task.ps1') -Config $config }
}
Test-Case 'core Änderungen verlangen beide Android-Suiten' {
    $ids = @(Get-ItoevaSelectedTests -Config $config -ChangedPaths @('core/src/main/java/Test.kt') | ForEach-Object { $_.id })
    Assert-True (($ids -join ',') -eq 'verify,app-sim-connected,app-connected')
}
Test-Case 'Planphase ist statisch read-only verdrahtet' {
    $entry = Get-Content -Raw -LiteralPath (Join-Path $PSScriptRoot 'Invoke-ItoevaEvolution.ps1')
    $planLine = @($entry -split "`r?`n" | Where-Object { $_ -match 'plan\.schema\.json' })[0]
    Assert-True ($planLine.Contains("'read-only'"))
    Assert-True (-not $planLine.Contains("'workspace-write'"))
    Assert-True ($entry -match 'Planphase hat den sauberen Base-Zustand verändert')
    Assert-True ($entry -match 'Planreview hat den sauberen Base-Zustand verändert')
}
Test-Case 'Candidate-Schema verwendet keine inkompatiblen Bedingungskonstruktionen' {
    $schema = Get-Content -Raw -LiteralPath (Join-Path $PSScriptRoot 'schemas\candidate.schema.json')
    Assert-True (-not ($schema -match '"(?:allOf|if|then)"\s*:'))
    $schema | ConvertFrom-Json | Out-Null
}
Test-Case 'CANDIDATES mit mindestens einem Kandidaten ist gueltig' {
    Assert-ItoevaCandidateAnalysis ([pscustomobject]@{ status='CANDIDATES'; candidates=@([pscustomobject]@{ title='Test' }) })
}
Test-Case 'CANDIDATES mit leerer Liste wird fail-closed abgewiesen' {
    Assert-Throws { Assert-ItoevaCandidateAnalysis ([pscustomobject]@{ status='CANDIDATES'; candidates=@() }) }
}
Test-Case 'NO_SAFE_EVOLUTION mit leerer Liste ist gueltig' {
    Assert-ItoevaCandidateAnalysis ([pscustomobject]@{ status='NO_SAFE_EVOLUTION'; candidates=@() })
}
Test-Case 'NO_SAFE_EVOLUTION mit Kandidaten wird fail-closed abgewiesen' {
    Assert-Throws { Assert-ItoevaCandidateAnalysis ([pscustomobject]@{ status='NO_SAFE_EVOLUTION'; candidates=@([pscustomobject]@{ title='Test' }) }) }
}
Test-Case 'Analyse-Invariante wird unmittelbar nach dem Parsen geprueft' {
    $entry = @(Get-Content -LiteralPath (Join-Path $PSScriptRoot 'Invoke-ItoevaEvolution.ps1'))
    $parseLine = [Array]::FindIndex($entry, [Predicate[string]]{ param($line) $line -match '\$analysis\s*=.*ConvertFrom-Json' })
    $validationLine = [Array]::FindIndex($entry, [Predicate[string]]{ param($line) $line -match '^\s*Assert-ItoevaCandidateAnalysis\s+\$analysis\s*$' })
    Assert-True ($parseLine -ge 0 -and $validationLine -eq ($parseLine + 1)) 'Analyse-Invariante ist nicht unmittelbar nach dem Parsen verdrahtet.'
}
Test-Case 'Evolutionsnummern werden exakt dreistellig formatiert' {
    Assert-True ((Format-ItoevaEvolutionNumber 1) -eq '001')
    Assert-True ((Format-ItoevaEvolutionNumber 9) -eq '009')
    Assert-True ((Format-ItoevaEvolutionNumber 10) -eq '010')
    Assert-True ((Format-ItoevaEvolutionNumber 999) -eq '999')
}
Test-Case 'Double aus Measure-Object wird als Evolutionsnummer formatiert' {
    $measured = (@(0) | Measure-Object -Maximum).Maximum + 1
    Assert-True ($measured.GetType().FullName -eq 'System.Double')
    Assert-True ((Format-ItoevaEvolutionNumber $measured) -eq '001')
}
Test-Case 'Ungueltige Evolutionsnummern werden fail-closed abgewiesen' {
    foreach ($invalid in @('nichtnumerisch', 0, -1, 1.5, 1000, $null)) {
        Assert-Throws { Format-ItoevaEvolutionNumber $invalid | Out-Null }
    }
}
Test-Case 'vollständiges PASS öffnet Gate' {
    $sha='a'*40; $tree='b'*40; $hash='c'*64; $plan='d'*64
    $gate = [pscustomobject]@{ planReview='PASS'; mandatoryTests='PASS'; finalReview='PASS'; diffCheck='PASS'; baseUnchanged=$true; baseSha=$sha; proposedTreeOid=$tree; testManifestHash=$hash; planHash=$plan; reviewBaseSha=$sha; reviewPlanHash=$plan; reviewTreeOid=$tree; reviewTestManifestHash=$hash }
    Assert-True (Test-ItoevaGate -Gate $gate)
}
Test-Case 'Testfehler schließt Gate' {
    $sha='a'*40; $tree='b'*40; $hash='c'*64; $plan='d'*64
    $gate = [pscustomobject]@{ planReview='PASS'; mandatoryTests='FAIL'; finalReview='PASS'; diffCheck='PASS'; baseUnchanged=$true; baseSha=$sha; proposedTreeOid=$tree; testManifestHash=$hash; planHash=$plan; reviewBaseSha=$sha; reviewPlanHash=$plan; reviewTreeOid=$tree; reviewTestManifestHash=$hash }
    Assert-True (-not (Test-ItoevaGate -Gate $gate))
}
Test-Case 'verändertes main schließt Gate' {
    $sha='a'*40; $tree='b'*40; $hash='c'*64; $plan='d'*64
    $gate = [pscustomobject]@{ planReview='PASS'; mandatoryTests='PASS'; finalReview='PASS'; diffCheck='PASS'; baseUnchanged=$false; baseSha=$sha; proposedTreeOid=$tree; testManifestHash=$hash; planHash=$plan; reviewBaseSha=$sha; reviewPlanHash=$plan; reviewTreeOid=$tree; reviewTestManifestHash=$hash }
    Assert-True (-not (Test-ItoevaGate -Gate $gate))
}

Test-Case 'Windows-Prozessargumente werden sicher serialisiert' {
    Assert-True ((ConvertTo-ItoevaWindowsCommandLineArgument '') -eq '""')
    Assert-True ((ConvertTo-ItoevaWindowsCommandLineArgument 'plain') -eq 'plain')
    Assert-True ((ConvertTo-ItoevaWindowsCommandLineArgument 'argument with spaces') -eq '"argument with spaces"')
    Assert-True ((ConvertTo-ItoevaWindowsCommandLineArgument 'quoted"value') -eq '"quoted\"value"')
}
Test-Case 'CMD-Fallback lehnt Shell-Metazeichen fail-closed ab' {
    foreach ($unsafe in @('a&b','a|b','a<b','a>b','a^b','%PATH%','a!b','a"b')) {
        Assert-Throws { New-ItoevaCmdShimCommand -BatchPath 'C:\safe\codex.cmd' -Arguments @($unsafe) | Out-Null }
    }
    $command = New-ItoevaCmdShimCommand -BatchPath 'C:\safe path\codex.cmd' -Arguments @('argument with spaces','plain')
    Assert-True ($command -eq '"C:\safe path\codex.cmd" "argument with spaces" "plain"')
}
Test-Case 'Codex-Argumente neuer Sitzungen haben globale Optionen vor exec' {
    $arguments = @(New-ItoevaCodexArguments -Repository 'C:\repo path' -Sandbox 'read-only' `
        -SchemaPath 'C:\schema path\candidate.json' -OutputPath 'C:\output path\analysis.json')
    $expected = @('-a','never','exec','-C','C:\repo path','-s','read-only','--output-schema','C:\schema path\candidate.json','--json','-o','C:\output path\analysis.json','-')
    Assert-True (($arguments -join "`n") -eq ($expected -join "`n")) "Unerwartete Argumente: $($arguments -join ' ')"
}
Test-Case 'Codex-Argumente fortgesetzter Sitzungen haben globale Optionen vor exec resume' {
    $arguments = @(New-ItoevaCodexArguments -Repository 'C:\repo path' -Sandbox 'workspace-write' `
        -SchemaPath 'C:\schema path\plan.json' -OutputPath 'C:\output path\plan.json' -SessionId 'session-123')
    $expected = @('-a','never','exec','resume','-c','sandbox_mode=workspace-write','session-123','-','--output-schema','C:\schema path\plan.json','--json','-o','C:\output path\plan.json')
    Assert-True (($arguments -join "`n") -eq ($expected -join "`n")) "Unerwartete Resume-Argumente: $($arguments -join ' ')"
}
Test-Case 'Codex-Argumente enthalten niemals exec vor -a never' {
    $newArguments = @(New-ItoevaCodexArguments 'C:\repo' 'read-only' 'C:\schema.json' 'C:\output.json')
    $resumeArguments = @(New-ItoevaCodexArguments 'C:\repo' 'workspace-write' 'C:\schema.json' 'C:\output.json' 'session-123')
    foreach ($arguments in @($newArguments,$resumeArguments)) {
        Assert-True ([Array]::IndexOf($arguments, '-a') -lt [Array]::IndexOf($arguments, 'exec')) "-a steht nicht vor exec: $($arguments -join ' ')"
        Assert-True (($arguments -join ' ') -notmatch 'exec(?:\s+.*?)?\s+-a\s+never') "Unsichere Reihenfolge: $($arguments -join ' ')"
    }
}

$tempRoot = Join-Path ([IO.Path]::GetTempPath()) "itoeva-runner-test-$([Guid]::NewGuid().ToString('N'))"
try {
    New-Item -ItemType Directory -Path $tempRoot | Out-Null
    Test-Case 'atomarer State ist lesbar' {
        $path = Join-Path $tempRoot 'state.json'
        Write-ItoevaAtomicJson -Value ([ordered]@{ phase='TEST'; runId='one' }) -Path $path
        $state = Get-Content -Raw -LiteralPath $path | ConvertFrom-Json
        Assert-True ($state.phase -eq 'TEST')
    }
    Test-Case 'zweiter Lock wird abgewiesen' {
        $path = Join-Path $tempRoot 'runner.lock'
        $first = Enter-ItoevaRunLock -Path $path
        try { Assert-Throws { Enter-ItoevaRunLock -Path $path | Out-Null } }
        finally { Exit-ItoevaRunLock -Handle $first -Path $path }
    }
    Test-Case 'externer Prozess wird hart begrenzt' {
        $result = Invoke-ItoevaProcessWithTimeout -Executable 'powershell.exe' `
            -Arguments @('-NoProfile','-Command','Start-Sleep -Seconds 5') -WorkingDirectory $tempRoot -TimeoutSeconds 1
        Assert-True ($result.timedOut)
        Assert-True ($result.exitCode -eq -1)
    }
    Test-Case 'Windows Codex-Aufloesung bevorzugt native EXE und ignoriert PS1' {
        $launcherRoot = Join-Path $tempRoot 'native-launcher'
        New-Item -ItemType Directory -Path $launcherRoot | Out-Null
        Copy-Item -LiteralPath (Get-Command powershell.exe).Source -Destination (Join-Path $launcherRoot 'codex.exe')
        [IO.File]::WriteAllText((Join-Path $launcherRoot 'codex.ps1'), "throw 'must not run'`n")
        [IO.File]::WriteAllText((Join-Path $launcherRoot 'codex.cmd'), "@exit /b 99`r`n")
        $oldPath = $env:PATH
        try {
            $env:PATH = "$launcherRoot;$oldPath"
            $launcher = Resolve-ItoevaCodexLauncher
            Assert-True ($launcher.kind -eq 'NATIVE_EXE')
            Assert-True ([IO.Path]::GetFullPath($launcher.executable) -eq [IO.Path]::GetFullPath((Join-Path $launcherRoot 'codex.exe')))
            Assert-True (@($launcher.prefixArguments).Count -eq 0)
        } finally { $env:PATH = $oldPath }
    }
    Test-Case 'Windows Codex-Aufloesung startet PS1 niemals direkt' {
        $launcherRoot = Join-Path $tempRoot 'ps1-only-launcher'
        New-Item -ItemType Directory -Path $launcherRoot | Out-Null
        [IO.File]::WriteAllText((Join-Path $launcherRoot 'codex.ps1'), "throw 'must not run'`n")
        $oldPath = $env:PATH
        try {
            $system32 = [Environment]::GetFolderPath('System')
            $env:PATH = "$launcherRoot;$system32"
            Assert-Throws { Resolve-ItoevaCodexLauncher | Out-Null }
        } finally { $env:PATH = $oldPath }
    }
    Test-Case 'Native EXE erhaelt Codex-Argumente in exakter Reihenfolge' {
        $launcherRoot = Join-Path $tempRoot 'native-argument-launcher'
        New-Item -ItemType Directory -Path $launcherRoot | Out-Null
        $sourcePath = Join-Path $launcherRoot 'ArgRecorder.cs'
        $nativePath = Join-Path $launcherRoot 'codex.exe'
        $source = 'using System; using System.Text; public static class ArgRecorder { public static int Main(string[] args) { foreach (string value in args) Console.WriteLine(Convert.ToBase64String(Encoding.UTF8.GetBytes(value))); return 0; } }'
        [IO.File]::WriteAllText($sourcePath, $source, [Text.UTF8Encoding]::new($false))
        $compiler = Join-Path ([Runtime.InteropServices.RuntimeEnvironment]::GetRuntimeDirectory()) 'csc.exe'
        & $compiler /nologo /target:exe "/out:$nativePath" $sourcePath
        if ($LASTEXITCODE -ne 0) { throw 'Native EXE-Testdouble konnte nicht erstellt werden.' }
        $oldPath = $env:PATH
        try {
            $system32 = [Environment]::GetFolderPath('System')
            $env:PATH = "$launcherRoot;$system32"
            $launcher = Resolve-ItoevaCodexLauncher
            $arguments = @(New-ItoevaCodexArguments 'C:\repo path' 'read-only' 'C:\schema path\candidate.json' 'C:\output path\analysis.json')
            $result = Invoke-ItoevaProcessWithTimeout -Executable $launcher.executable -Arguments $arguments -WorkingDirectory $launcherRoot -TimeoutSeconds 10
            $actual = @($result.output -split "`r?`n" | Where-Object { $_ } | ForEach-Object { [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($_)) })
            Assert-True ($result.exitCode -eq 0) "Native EXE-Testdouble schlug fehl: $($result.output)"
            Assert-True (($actual -join "`n") -eq ($arguments -join "`n")) "Native Argumentreihenfolge abweichend: $($actual -join ' ')"
        } finally { $env:PATH = $oldPath }
    }
    Test-Case 'Windows Codex-CMD-Fallback laeuft nur ueber cmd.exe' {
        $launcherRoot = Join-Path $tempRoot 'cmd-launcher'
        New-Item -ItemType Directory -Path $launcherRoot | Out-Null
        [IO.File]::WriteAllText((Join-Path $launcherRoot 'codex.ps1'), "throw 'must not run'`n")
        [IO.File]::WriteAllText((Join-Path $launcherRoot 'codex.cmd'), "@echo off`r`nset /p INPUT=`r`necho OUT:%~1:%INPUT%`r`necho ERR:%~2 1>&2`r`nexit /b %~3`r`n")
        $stdin = Join-Path $launcherRoot 'stdin.txt'
        [IO.File]::WriteAllText($stdin, 'hello stdin')
        $oldPath = $env:PATH
        try {
            $system32 = [Environment]::GetFolderPath('System')
            $env:PATH = "$launcherRoot;$system32"
            $launcher = Resolve-ItoevaCodexLauncher
            Assert-True ($launcher.kind -eq 'CMD_SHIM') "Unerwarteter Launcher: $($launcher.kind)"
            Assert-True ([IO.Path]::GetFileName($launcher.executable) -eq 'cmd.exe') "Unerwartbares Executable: $($launcher.executable)"
            Assert-True ($launcher.batchPath -match '(?i)codex\.cmd$') 'CMD-Launcher fehlt.'
            Assert-True ($launcher.batchPath -notmatch '(?i)codex\.ps1') 'PS1 wurde als Launcher verwendet.'
            $command = New-ItoevaCmdShimCommand $launcher.batchPath @('argument with spaces', 'stderr value', '7')
            $cmdArguments = "/d /s /c `"$command`""
            $result = Invoke-ItoevaProcessWithTimeout -Executable $launcher.executable `
                -WorkingDirectory $launcherRoot -TimeoutSeconds 10 -StandardInputPath $stdin -ValidatedWindowsArgumentString $cmdArguments
            Assert-True (-not $result.timedOut) 'CMD-Fallback lief in ein Timeout.'
            Assert-True ($result.exitCode -eq 7) "Unerwarteter Exitcode $($result.exitCode). Ausgabe: $($result.output)"
            Assert-True ($result.output -match 'OUT:argument with spaces:hello stdin') "stdout/stdin/Argumente fehlen: $($result.output)"
            Assert-True ($result.output -match 'ERR:stderr value') "stderr fehlt: $($result.output)"
        } finally { $env:PATH = $oldPath }
    }
    Test-Case 'CMD-Fallback erhaelt dieselbe logische Codex-Argumentreihenfolge' {
        $launcherRoot = Join-Path $tempRoot 'cmd-argument-launcher'
        New-Item -ItemType Directory -Path $launcherRoot | Out-Null
        $batchPath = Join-Path $launcherRoot 'codex.cmd'
        [IO.File]::WriteAllText($batchPath, "@echo off`r`n:next`r`nif `"%~1`"==`"`" exit /b 0`r`necho ARG:%~1`r`nshift`r`ngoto next`r`n")
        $arguments = @(New-ItoevaCodexArguments 'C:\repo path' 'read-only' 'C:\schema path\candidate.json' 'C:\output path\analysis.json')
        $command = New-ItoevaCmdShimCommand $batchPath $arguments
        $cmdArguments = "/d /s /c `"$command`""
        $result = Invoke-ItoevaProcessWithTimeout -Executable (Get-Command cmd.exe).Source `
            -WorkingDirectory $launcherRoot -TimeoutSeconds 10 -ValidatedWindowsArgumentString $cmdArguments
        $actual = @($result.output -split "`r?`n" | Where-Object { $_ -match '^ARG:' } | ForEach-Object { $_.Substring(4) })
        Assert-True ($result.exitCode -eq 0) "CMD-Argumenttest schlug fehl: $($result.output)"
        Assert-True (($actual -join "`n") -eq ($arguments -join "`n")) "CMD-Argumentreihenfolge abweichend: $($actual -join ' ')"
    }
    Test-Case 'CMD-Fallback-Timeout beendet den Kindprozessbaum' {
        $launcherRoot = Join-Path $tempRoot 'cmd-timeout-launcher'
        New-Item -ItemType Directory -Path $launcherRoot | Out-Null
        $childPidPath = Join-Path $launcherRoot 'child.pid'
        $batchPath = Join-Path $launcherRoot 'codex.cmd'
        [IO.File]::WriteAllText($batchPath, "@echo off`r`nstart `"`" /b powershell.exe -NoProfile -Command `"[IO.File]::WriteAllText('$childPidPath',[string]`$PID); Start-Sleep -Seconds 30`"`r`nfor /l %%i in (1,1,50) do @if not exist `"$childPidPath`" @ping 127.0.0.1 -n 2 >nul`r`nping 127.0.0.1 -n 31 >nul`r`n")
        $command = New-ItoevaCmdShimCommand $batchPath
        $cmdArguments = "/d /s /c `"$command`""
        $result = Invoke-ItoevaProcessWithTimeout -Executable (Get-Command cmd.exe).Source `
            -WorkingDirectory $launcherRoot -TimeoutSeconds 3 -ValidatedWindowsArgumentString $cmdArguments
        Assert-True ($result.timedOut)
        Assert-True (Test-Path -LiteralPath $childPidPath) 'CMD-Kindprozess-PID wurde nicht geschrieben.'
        $childPid = [int](Get-Content -Raw -LiteralPath $childPidPath)
        Start-Sleep -Milliseconds 250
        Assert-True (-not (Get-Process -Id $childPid -ErrorAction SilentlyContinue)) 'CMD-Kindprozess wurde nicht beendet.'
    }
    Test-Case 'Timeout beendet auch den Kindprozessbaum' {
        $childPidPath = Join-Path $tempRoot 'child.pid'
        $escapedPidPath = $childPidPath.Replace("'", "''")
        $parentScript = "`$child = Start-Process powershell.exe -ArgumentList '-NoProfile','-Command','Start-Sleep -Seconds 30' -PassThru; [IO.File]::WriteAllText('$escapedPidPath', [string]`$child.Id); Start-Sleep -Seconds 30"
        $result = Invoke-ItoevaProcessWithTimeout -Executable 'powershell.exe' `
            -Arguments @('-NoProfile','-Command',$parentScript) -WorkingDirectory $tempRoot -TimeoutSeconds 2
        Assert-True ($result.timedOut)
        Assert-True (Test-Path -LiteralPath $childPidPath) 'Kindprozess-PID wurde nicht geschrieben.'
        $childPid = [int](Get-Content -Raw -LiteralPath $childPidPath)
        Start-Sleep -Milliseconds 250
        Assert-True (-not (Get-Process -Id $childPid -ErrorAction SilentlyContinue)) 'Kindprozess wurde nicht beendet.'
    }
    Test-Case 'PublishDryRun publiziert nur den hashgebundenen Evolution-Tree' {
        $fixture=New-PublishDryRunFixture (Join-Path $tempRoot 'publish-success') ('1'*32)
        $result=Invoke-ItoevaPublishDryRun $fixture.repository $fixture.runId $fixture.config $fixture.hooks
        Assert-True ($result.status -eq 'PUSHED')
        Assert-True ((Get-ItoevaRemoteSha $fixture.repository 'origin' "refs/heads/$($fixture.branch)") -eq $result.commitSha)
        Assert-True ((Get-ItoevaRemoteSha $fixture.repository 'origin' 'refs/heads/main') -eq $fixture.base)
        Assert-True ((& git -C $fixture.repository rev-parse "$($result.commitSha)^").Trim() -eq $fixture.base)
        Assert-True ((& git -C $fixture.repository rev-parse "$($result.commitSha)^{tree}").Trim() -eq $fixture.tree)
        $snapshot="$($fixture.reportPath.Substring(0,$fixture.reportPath.Length-5)).dry-run.json"
        Assert-True (Test-Path -LiteralPath $snapshot)
        Assert-True ((Get-ItoevaSha256 $snapshot) -eq (Get-Content -Raw -LiteralPath "$snapshot.sha256").Trim())
        $phaseByRank=@{'1'='PREPARED';'2'='COMMITTED';'3'='PUSHED';'4'='REPORTED'}
        $phases=@(Get-ChildItem -LiteralPath (Join-Path $fixture.runRoot 'publish-journal') -Filter 'p.*.json' | ForEach-Object { if($_.Name -match '^p\.([1-4])\.'){ $phaseByRank[$Matches[1]] } } | Sort-Object)
        Assert-True (($phases -join ',') -eq 'COMMITTED,PREPARED,PUSHED,REPORTED') "Journalphasen fehlen: $($phases -join ',')"
    }
    Test-Case 'PublishDryRun normalisiert zwei Testergebnisse zu einem flachen Array' {
        $testResults=@(
            [pscustomobject]@{ id='verify'; status='PASS'; exitCode=0; startedAt='2026-01-01T00:00:00Z'; timedOut=$false; finishedAt='2026-01-01T00:00:01Z'; output='offline verify' },
            [pscustomobject]@{ id='connected'; status='PASS'; exitCode=0; startedAt='2026-01-01T00:00:01Z'; timedOut=$false; finishedAt='2026-01-01T00:00:02Z'; output='offline connected' }
        )
        $fixture=New-PublishDryRunFixture (Join-Path $tempRoot 'publish-two-tests') ('d'*32) $testResults
        $rawParsed=Get-Content -Raw -LiteralPath (Join-Path $fixture.runRoot 'tests.json')|ConvertFrom-Json
        $tests=@($rawParsed)
        Assert-True ($tests.Count -eq 2) 'Zwei Tests wurden nicht flach normalisiert.'
        Assert-True ($tests[0] -isnot [System.Array]) 'Der konkrete System.Object[]-Regressionsfall besteht fort.'
        $result=Invoke-ItoevaPublishDryRun $fixture.repository $fixture.runId $fixture.config $fixture.hooks
        Assert-True ($result.status -eq 'PUSHED') 'Identische Legacy-Arrays wurden nicht akzeptiert.'
    }
    Test-Case 'PublishDryRun normalisiert ein Testergebnis weiterhin korrekt' {
        $fixture=New-PublishDryRunFixture (Join-Path $tempRoot 'publish-one-test') ('e'*32)
        $rawParsed=Get-Content -Raw -LiteralPath (Join-Path $fixture.runRoot 'tests.json')|ConvertFrom-Json
        $tests=@($rawParsed)
        Assert-True ($tests.Count -eq 1 -and $tests[0].id -eq 'verify')
    }
    Test-Case 'PublishDryRun lehnt fehlende zusaetzliche oder vertauschte Tests ab' {
        $testResults=@(
            [pscustomobject]@{ id='verify'; status='PASS'; exitCode=0; startedAt='2026-01-01T00:00:00Z'; timedOut=$false; finishedAt='2026-01-01T00:00:01Z'; output='one' },
            [pscustomobject]@{ id='connected'; status='PASS'; exitCode=0; startedAt='2026-01-01T00:00:01Z'; timedOut=$false; finishedAt='2026-01-01T00:00:02Z'; output='two' }
        )
        $variants=@(
            [pscustomobject]@{name='missing';tests=@($testResults[0])},
            [pscustomobject]@{name='additional';tests=@($testResults + [pscustomobject]@{ id='extra'; status='PASS'; exitCode=0; startedAt='2026-01-01T00:00:02Z'; timedOut=$false; finishedAt='2026-01-01T00:00:03Z'; output='three' })},
            [pscustomobject]@{name='reordered';tests=@($testResults[1],$testResults[0])}
        )
        $index=0
        foreach($variant in $variants){
            $fixture=New-PublishDryRunFixture (Join-Path $tempRoot "publish-tests-$($variant.name)") ((('{0:x}' -f ($index+1))*32).Substring(0,32)) $testResults
            Set-PublishFixtureReportTests $fixture $variant.tests
            Assert-Throws { Invoke-ItoevaPublishDryRun $fixture.repository $fixture.runId $fixture.config $fixture.hooks | Out-Null }
            Assert-True (-not (Test-Path -LiteralPath (Join-Path $fixture.runRoot 'publish-journal'))) 'Mismatch erreichte PREPARED.'
            $index++
        }
    }
    Test-Case 'PublishDryRun lehnt manipulierte Testfelder ab' {
        $baseTest=[pscustomobject]@{ id='verify'; status='PASS'; exitCode=0; startedAt='2026-01-01T00:00:00Z'; timedOut=$false; finishedAt='2026-01-01T00:00:01Z'; output='offline' }
        $mutations=@(
            [pscustomobject]@{name='output';property='output';value='manipulated'},
            [pscustomobject]@{name='exit-code';property='exitCode';value=1},
            [pscustomobject]@{name='timeout';property='timedOut';value=$true},
            [pscustomobject]@{name='status';property='status';value='FAIL'}
        )
        $index=0
        foreach($mutation in $mutations){
            $fixture=New-PublishDryRunFixture (Join-Path $tempRoot "publish-test-field-$($mutation.name)") ((('{0:x}' -f ($index+4))*32).Substring(0,32)) @($baseTest)
            $changed=$baseTest.PSObject.Copy(); $changed.($mutation.property)=$mutation.value
            Set-PublishFixtureReportTests $fixture @($changed)
            Assert-Throws { Invoke-ItoevaPublishDryRun $fixture.repository $fixture.runId $fixture.config $fixture.hooks | Out-Null }
            $index++
        }
    }
    Test-Case 'Korrekter Legacy-Dry-Run erreicht PREPARED' {
        $testResults=@(
            [pscustomobject]@{ id='verify'; status='PASS'; exitCode=0; startedAt='2026-01-01T00:00:00Z'; timedOut=$false; finishedAt='2026-01-01T00:00:01Z'; output='one' },
            [pscustomobject]@{ id='connected'; status='PASS'; exitCode=0; startedAt='2026-01-01T00:00:01Z'; timedOut=$false; finishedAt='2026-01-01T00:00:02Z'; output='two' }
        )
        $fixture=New-PublishDryRunFixture (Join-Path $tempRoot 'publish-legacy-prepared') ('f'*32) $testResults
        Invoke-ItoevaPublishDryRun $fixture.repository $fixture.runId $fixture.config $fixture.hooks | Out-Null
        $prepared=@(Get-ChildItem -LiteralPath (Join-Path $fixture.runRoot 'publish-journal') -Filter 'p.1.*.json' -File)
        Assert-True ($prepared.Count -eq 1) 'Legacy-Dry-Run erreichte PREPARED nicht.'
    }
    Test-Case 'PublishDryRun lehnt manipulierten Report-Hash fail-closed ab' {
        $fixture=New-PublishDryRunFixture (Join-Path $tempRoot 'publish-report-tamper') ('2'*32)
        [IO.File]::AppendAllText($fixture.reportPath, ' ')
        Assert-Throws { Invoke-ItoevaPublishDryRun $fixture.repository $fixture.runId $fixture.config $fixture.hooks | Out-Null }
        Assert-True (-not (Get-ItoevaRemoteSha $fixture.repository 'origin' "refs/heads/$($fixture.branch)"))
    }
    Test-Case 'PublishDryRun lehnt falschen Working Tree fail-closed ab' {
        $fixture=New-PublishDryRunFixture (Join-Path $tempRoot 'publish-tree-tamper') ('3'*32)
        [IO.File]::WriteAllText((Join-Path $fixture.repository 'source.txt'), "different`n")
        Assert-Throws { Invoke-ItoevaPublishDryRun $fixture.repository $fixture.runId $fixture.config $fixture.hooks | Out-Null }
        Assert-True (-not (Get-ItoevaRemoteSha $fixture.repository 'origin' "refs/heads/$($fixture.branch)"))
    }
    Test-Case 'PublishDryRun lehnt veraendertes origin main fail-closed ab' {
        $fixture=New-PublishDryRunFixture (Join-Path $tempRoot 'publish-main-moved') ('4'*32)
        [IO.File]::WriteAllText((Join-Path $fixture.seed 'source.txt'), "moved main`n")
        & git -C $fixture.seed add -- source.txt; & git -C $fixture.seed commit --quiet -m move-main; & git -C $fixture.seed push --quiet origin HEAD:refs/heads/main
        Assert-Throws { Invoke-ItoevaPublishDryRun $fixture.repository $fixture.runId $fixture.config $fixture.hooks | Out-Null }
        Assert-True (-not (Get-ItoevaRemoteSha $fixture.repository 'origin' "refs/heads/$($fixture.branch)"))
    }
    Test-Case 'PublishDryRun ist nach erfolgreichem Report idempotent' {
        $fixture=New-PublishDryRunFixture (Join-Path $tempRoot 'publish-idempotent') ('5'*32)
        $first=Invoke-ItoevaPublishDryRun $fixture.repository $fixture.runId $fixture.config $fixture.hooks
        $second=Invoke-ItoevaPublishDryRun $fixture.repository $fixture.runId $fixture.config $fixture.hooks
        Assert-True ($second.commitSha -eq $first.commitSha)
        Assert-True ($second.remoteBranchSha -eq $first.remoteBranchSha)
    }
    Test-Case 'PublishDryRun resumed idempotent nach jeder Journalphase' {
        $cases=@(
            [pscustomobject]@{phase='PREPARED';id=('6'*32)},
            [pscustomobject]@{phase='COMMITTED';id=('7'*32)},
            [pscustomobject]@{phase='PUSHED';id=('8'*32)}
        )
        foreach($case in $cases){
            $fixture=New-PublishDryRunFixture (Join-Path $tempRoot "publish-resume-$($case.phase)") $case.id
            $first=Invoke-ItoevaPublishDryRun $fixture.repository $fixture.runId $fixture.config $fixture.hooks
            Set-PublishFixtureResumePhase $fixture $case.phase $true
            $resumed=Invoke-ItoevaPublishDryRun $fixture.repository $fixture.runId $fixture.config $fixture.hooks
            Assert-True ($resumed.commitSha -eq $first.commitSha) "Resume $($case.phase) erzeugte einen anderen Commit."
            Assert-True ($resumed.remoteBranchSha -eq $first.remoteBranchSha) "Resume $($case.phase) veraenderte den Remote-SHA."
        }
    }
    Test-Case 'PublishDryRun repariert Report-Abbruch nach verifiziertem Push' {
        $fixture=New-PublishDryRunFixture (Join-Path $tempRoot 'publish-report-resume') ('9'*32)
        $first=Invoke-ItoevaPublishDryRun $fixture.repository $fixture.runId $fixture.config $fixture.hooks
        Set-PublishFixtureResumePhase $fixture 'PUSHED' $false
        [IO.File]::WriteAllText("$($fixture.reportPath).sha256", '0'*64, [Text.UTF8Encoding]::new($false))
        $resumed=Invoke-ItoevaPublishDryRun $fixture.repository $fixture.runId $fixture.config $fixture.hooks
        Assert-True ($resumed.commitSha -eq $first.commitSha)
        Assert-True ((Get-ItoevaSha256 $fixture.reportPath) -eq (Get-Content -Raw -LiteralPath "$($fixture.reportPath).sha256").Trim())
        $state=Get-Content -Raw -LiteralPath (Join-Path $fixture.runRoot 'state.json')|ConvertFrom-Json
        Assert-True ($state.status -eq 'PUSHED')
    }
    Test-Case 'PublishDryRun startet weder Codex noch konfigurierte Tests' {
        $moduleSource=Get-Content -Raw -LiteralPath (Join-Path $PSScriptRoot 'Itoeva.Runner.psm1')
        $start=$moduleSource.IndexOf('function Invoke-ItoevaPublishDryRun')
        $end=$moduleSource.IndexOf('Export-ModuleMember', $start)
        $publishSource=$moduleSource.Substring($start, $end-$start)
        Assert-True ($publishSource -notmatch 'Invoke-ItoevaCodexSession|Invoke-ItoevaConfiguredTests|gradlew')
        $entrySource=Get-Content -Raw -LiteralPath (Join-Path $PSScriptRoot 'Invoke-ItoevaEvolution.ps1')
        Assert-True ($entrySource -match 'if \(\$Action -eq ''PublishDryRun''\)')
    }
    Test-Case 'PublishDryRun Run-ID ist strikt fail-closed' {
        Assert-True (Test-ItoevaRunId ('a'*32))
        foreach($invalid in @('../bad',('A'*32),('a'*31),('a'*33),'')){ Assert-True (-not (Test-ItoevaRunId $invalid)) }
    }
    Test-Case 'PublishDryRun lehnt Journal ohne PREPARED-Genesis ab' {
        $fixture=New-PublishDryRunFixture (Join-Path $tempRoot 'publish-missing-genesis') ('a'*32)
        Invoke-ItoevaPublishDryRun $fixture.repository $fixture.runId $fixture.config $fixture.hooks | Out-Null
        $prepared=Get-ChildItem -LiteralPath (Join-Path $fixture.runRoot 'publish-journal') -Filter 'p.1.*.json' -File
        Remove-Item -LiteralPath $prepared.FullName -Force
        Assert-Throws { Invoke-ItoevaPublishDryRun $fixture.repository $fixture.runId $fixture.config $fixture.hooks | Out-Null }
    }
    Test-Case 'PublishDryRun bindet Review-Artefakte unveraenderlich' {
        $fixture=New-PublishDryRunFixture (Join-Path $tempRoot 'publish-review-tamper') ('b'*32)
        Invoke-ItoevaPublishDryRun $fixture.repository $fixture.runId $fixture.config $fixture.hooks | Out-Null
        [IO.File]::AppendAllText((Join-Path $fixture.runRoot 'plan-review.json'),' ')
        Assert-Throws { Invoke-ItoevaPublishDryRun $fixture.repository $fixture.runId $fixture.config $fixture.hooks | Out-Null }
    }
    Test-Case 'PublishDryRun CLI verlangt beide Publikationsflags aber keine Agentenfreigabe' {
        $fixture=New-PublishDryRunFixture (Join-Path $tempRoot 'publish-cli-flags') ('c'*32)
        $entry=Join-Path $PSScriptRoot 'Invoke-ItoevaEvolution.ps1'; $activation=Join-Path $fixture.runtime 'activation.json'
        $cases=@(
            [ordered]@{agentExecutionEnabled=$false;publicationEnabled=$false;standingAuthorizationEvolutionOnly=$true;runtimeRoot=$fixture.runtime},
            [ordered]@{agentExecutionEnabled=$false;publicationEnabled=$true;standingAuthorizationEvolutionOnly=$false;runtimeRoot=$fixture.runtime}
        )
        foreach($case in $cases){
            Write-ItoevaAtomicJson $case $activation
            $process=Invoke-ItoevaProcessWithTimeout -Executable 'powershell.exe' -Arguments @('-NoProfile','-ExecutionPolicy','Bypass','-File',$entry,'-Action','PublishDryRun','-RunId',$fixture.runId,'-Repository',$fixture.repository,'-ActivationPath',$activation) -WorkingDirectory $fixture.repository -TimeoutSeconds 20
            Assert-True ($process.exitCode -ne 0)
            Assert-True ($process.output -match 'Commit/-Push ist nicht autorisiert')
        }
        Write-ItoevaAtomicJson ([ordered]@{agentExecutionEnabled=$false;publicationEnabled=$true;standingAuthorizationEvolutionOnly=$true;runtimeRoot=$fixture.runtime}) $activation
        $process=Invoke-ItoevaProcessWithTimeout -Executable 'powershell.exe' -Arguments @('-NoProfile','-ExecutionPolicy','Bypass','-File',$entry,'-Action','PublishDryRun','-RunId',$fixture.runId,'-Repository',$fixture.repository,'-ActivationPath',$activation) -WorkingDirectory $fixture.repository -TimeoutSeconds 20
        Assert-True ($process.exitCode -ne 0)
        Assert-True ($process.output -notmatch 'Agentenausf.hrung ist nicht autorisiert')
        Assert-True ($process.output -match 'Unerwartetes origin')
    }
    Test-Case 'RevalidateDryRun uebertraegt den Source-Diff exakt auf einen neuen Base' {
        $fixture=New-RevalidateDryRunFixture (Join-Path $tempRoot 'revalidate-tree') ('1a'*16)
        $paths=@('source.txt');$expected=Get-ItoevaExpectedRebasedTree $fixture.repository $fixture.base $fixture.tree $fixture.newBase $paths $fixture.runtime
        $actual=Get-ItoevaProposedTreeOid $fixture.repository $fixture.newBase $paths
        Assert-True ($expected -eq $actual) 'Exakt uebertragener Tree stimmt nicht mit dem Working Tree ueberein.'
        [IO.File]::WriteAllText((Join-Path $fixture.repository 'source.txt'),"altered`n")
        Assert-True ((Get-ItoevaProposedTreeOid $fixture.repository $fixture.newBase $paths) -ne $expected) 'Veraenderter Working Tree wurde nicht erkannt.'
    }
    Test-Case 'RevalidateDryRun lehnt Konflikt im selben Codebereich ab' {
        $fixture=New-RevalidateDryRunFixture (Join-Path $tempRoot 'revalidate-conflict') ('1b'*16) $true
        Assert-Throws { Get-ItoevaExpectedRebasedTree $fixture.repository $fixture.base $fixture.tree $fixture.newBase @('source.txt') $fixture.runtime | Out-Null }
    }
    Test-Case 'RevalidateDryRun lehnt manipulierte Source-Artefakte fail-closed ab' {
        $cases=@(
            [pscustomobject]@{name='report';relative='report'},[pscustomobject]@{name='state';relative='state.json'},
            [pscustomobject]@{name='plan';relative='plan.json'},[pscustomobject]@{name='plan-review';relative='plan-review.json'},
            [pscustomobject]@{name='tests';relative='tests.json'},[pscustomobject]@{name='final-review';relative='final-review.json'},
            [pscustomobject]@{name='snapshot-sidecar';relative='plan-review.dry-run.json.sha256'}
        )
        $index=0
        foreach($case in $cases){
            $id=(('{0:x2}' -f (30+$index))*16).Substring(0,32);$fixture=New-RevalidateDryRunFixture (Join-Path $tempRoot "revalidate-source-$($case.name)") $id
            $target=if($case.relative -eq 'report'){$fixture.reportPath}else{Join-Path $fixture.runRoot $case.relative}
            if($case.name -eq 'snapshot-sidecar'){[IO.File]::WriteAllText($target,('0'*64),[Text.UTF8Encoding]::new($false))}else{[IO.File]::AppendAllText($target,' ')}
            Assert-Throws { Invoke-ItoevaRevalidateDryRun $fixture.repository $fixture.runId $fixture.config | Out-Null }
            $newStates=@(Get-ChildItem -LiteralPath (Join-Path $fixture.runtime 'state') -Directory|Where-Object{$_.Name -ne $fixture.runId})
            Assert-True ($newStates.Count -eq 0) "Manipulierte Source erzeugte einen neuen Run: $($case.name)"
            $index++
        }
    }
    Test-Case 'RevalidateDryRun erzeugt v2 PASS mit genau einem frischen Read-only-Final-Review' {
        $fixture=New-RevalidateDryRunFixture (Join-Path $tempRoot 'revalidate-success') ('1d'*16)
        $launcherRoot=Join-Path $tempRoot 'revalidate-codex';$codexMarker=Join-Path $launcherRoot 'codex.calls';$testMarker=Join-Path $launcherRoot 'tests.calls'
        New-RevalidateCodexDouble $launcherRoot $codexMarker|Out-Null
        $env:ITOEVA_REVALIDATE_CODEX_MARKER=$codexMarker;$oldPath=$env:PATH;$env:PATH="$launcherRoot;$oldPath"
        $testCommand="[IO.File]::AppendAllText('$($testMarker.Replace("'","''"))','test'+[Environment]::NewLine); exit 0"
        $fixture.config.tests.mandatory=@([pscustomobject]@{id='verify';executable='powershell.exe';arguments=@('-NoProfile','-Command',$testCommand)})
        $sourceHashes=@{};foreach($name in @('state.json','plan.json','plan-review.json','tests.json','final-review.json')){$sourceHashes[$name]=Get-ItoevaSha256 (Join-Path $fixture.runRoot $name)}
        $headBefore=(& git -C $fixture.repository rev-parse HEAD).Trim()
        try{$result=Invoke-ItoevaRevalidateDryRun $fixture.repository $fixture.runId $fixture.config}
        finally{$env:PATH=$oldPath;Remove-Item Env:ITOEVA_REVALIDATE_CODEX_MARKER -ErrorAction SilentlyContinue}
        Assert-True ($result.status -eq 'DRY_RUN_PASS' -and $result.runId -ne $fixture.runId)
        Assert-True ((& git -C $fixture.repository rev-parse HEAD).Trim() -eq $headBefore) 'RevalidateDryRun erzeugte einen Commit oder veraenderte HEAD.'
        & git -C $fixture.repository diff --cached --quiet;Assert-True ($LASTEXITCODE -eq 0) 'RevalidateDryRun veraenderte den echten Index.'
        Assert-True (@(Get-Content -LiteralPath $testMarker).Count -eq 1) 'Relevante Tests wurden nicht exakt einmal erneut ausgefuehrt.'
        $calls=@(Get-Content -LiteralPath $codexMarker);Assert-True ($calls.Count -eq 1) 'Final Review wurde nicht exakt einmal frisch gestartet.'
        Assert-True ($calls[0] -match '(?:^|\t)-s\tread-only(?:\t|$)' -and $calls[0] -notmatch '(?:^|\t)resume(?:\t|$)') 'Final Review war nicht frisch und read-only.'
        $reportPath=Join-Path (Join-Path $fixture.runtime 'reports') "$($result.runId).json";$report=Read-ItoevaHashedJson $reportPath "$reportPath.sha256"
        Assert-True ([int]$report.value.formatVersion -eq 2 -and $report.value.runKind -eq 'REVALIDATED_DRY_RUN' -and $report.value.status -eq 'DRY_RUN_PASS')
        Assert-True ([string]$report.value.sourceBindings.sourceRunId -eq $fixture.runId)
        foreach($name in $sourceHashes.Keys){Assert-True ((Get-ItoevaSha256 (Join-Path $fixture.runRoot $name)) -eq $sourceHashes[$name]) "Source-Artefakt veraendert: $name"}
        foreach($evidence in @('report','state','tests','final-review')){Read-ItoevaHashedJson (Join-Path (Join-Path (Join-Path $fixture.runtime "state\$($result.runId)") 'source-evidence') "$evidence.json") (Join-Path (Join-Path (Join-Path $fixture.runtime "state\$($result.runId)") 'source-evidence') "$evidence.json.sha256")|Out-Null}

        $fixture.config.publication.enabled=$true;$fixture.config.agentExecution.enabled=$false
        $newRunRoot=Join-Path $fixture.runtime "state\$($result.runId)"
        foreach($tamperPath in @((Join-Path $newRunRoot 'revalidation.json'),(Join-Path $newRunRoot 'source-evidence\report.json'))){
            $originalBytes=[IO.File]::ReadAllBytes($tamperPath);[IO.File]::AppendAllText($tamperPath,' ')
            Assert-Throws { Invoke-ItoevaPublishDryRun $fixture.repository $result.runId $fixture.config $fixture.hooks|Out-Null }
            [IO.File]::WriteAllBytes($tamperPath,$originalBytes)
        }
        $published=Invoke-ItoevaPublishDryRun $fixture.repository $result.runId $fixture.config $fixture.hooks
        Assert-True ($published.status -eq 'PUSHED') 'Version-2-Report war nicht PublishDryRun-kompatibel.'
        Assert-True ((Get-ItoevaRemoteSha $fixture.repository 'origin' 'refs/heads/main') -eq $fixture.newBase) 'Version-2-Publish veraenderte main.'
    }
    Test-Case 'RevalidateDryRun quarantinisiert Testfehler vor dem Final Review' {
        $fixture=New-RevalidateDryRunFixture (Join-Path $tempRoot 'revalidate-test-fail') ('1e'*16)
        $fixture.config.tests.mandatory=@([pscustomobject]@{id='verify';executable='powershell.exe';arguments=@('-NoProfile','-Command','exit 7')})
        Assert-Throws { Invoke-ItoevaRevalidateDryRun $fixture.repository $fixture.runId $fixture.config | Out-Null }
        $newRun=Get-ChildItem -LiteralPath (Join-Path $fixture.runtime 'state') -Directory|Where-Object{$_.Name -ne $fixture.runId}|Select-Object -First 1
        $state=Get-Content -Raw -LiteralPath (Join-Path $newRun.FullName 'state.json')|ConvertFrom-Json
        Assert-True ($state.status -eq 'QUARANTINED' -and $state.phase -eq 'REVALIDATE_TEST')
        Assert-True (-not(Test-Path -LiteralPath (Join-Path $newRun.FullName 'final-review.json'))) 'Final Review lief trotz Testfehler.'
    }
    Test-Case 'RevalidateDryRun ist statisch von Analyse Planung Implementierung und Publikation getrennt' {
        $moduleSource=Get-Content -Raw -LiteralPath (Join-Path $PSScriptRoot 'Itoeva.Runner.psm1');$start=$moduleSource.IndexOf('function Invoke-ItoevaRevalidateDryRun');$end=$moduleSource.IndexOf('function Invoke-ItoevaPublishDryRun',$start);$source=$moduleSource.Substring($start,$end-$start)
        Assert-True (($source|Select-String -Pattern 'Invoke-ItoevaCodexSession' -AllMatches).Matches.Count -eq 1) 'RevalidateDryRun hat nicht exakt eine Agentensitzung.'
        Assert-True ($source -notmatch 'Assert-ItoevaCandidateAnalysis|candidate\.schema|plan\.schema|implementation\.schema|Publish-ItoevaEvolution|Invoke-ItoevaPublishDryRun')
        Assert-True ($source -notmatch '(?i)\bgit\s+-C\s+\$Repository\s+(commit|push|merge|rebase|reset|tag|checkout)\b')
        $entry=Get-Content -Raw -LiteralPath (Join-Path $PSScriptRoot 'Invoke-ItoevaEvolution.ps1');Assert-True ($entry -match "Action -eq 'RevalidateDryRun'")
    }
    Test-Case 'Scheduler-Wrapper protokolliert redigiert und erhaelt Exitcode Run-ID und Rotation' {
        $root=Join-Path $tempRoot 'scheduled-wrapper';$wrapperRoot=Join-Path $root 'runner';$local=Join-Path $root 'local';New-Item -ItemType Directory -Path $wrapperRoot,$local|Out-Null
        $wrapperSource=Join-Path $PSScriptRoot 'Invoke-ItoevaEvolutionScheduled.ps1';$wrapper=Join-Path $wrapperRoot 'Invoke-ItoevaEvolutionScheduled.ps1';Copy-Item -LiteralPath $wrapperSource -Destination $wrapper
        $fakeRunner=Join-Path $wrapperRoot 'Invoke-ItoevaEvolution.ps1'
        $fake=@'
param([string]$Action,[string]$Repository,[string]$ActivationPath,[string]$RunId)
Write-Output "repo=$Repository"
Write-Output "activation=$ActivationPath"
[Console]::Error.WriteLine('stderr-output')
switch($Action){
 'DryRun' {New-Item -ItemType Directory -Path (Join-Path $env:LOCALAPPDATA 'ItoevaEvolutionRunner\state\aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa') -Force|Out-Null; Write-Output 'dry-ok'; exit 0}
 'Run' {New-Item -ItemType Directory -Path (Join-Path $env:LOCALAPPDATA 'ItoevaEvolutionRunner\state\bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb') -Force|Out-Null;New-Item -ItemType Directory -Path (Join-Path $env:LOCALAPPDATA 'ItoevaEvolutionRunner\state\cccccccccccccccccccccccccccccccc') -Force|Out-Null;exit 0}
 'PublishDryRun' {Write-Output 'publish-existing';exit 0}
 'Preflight' {Write-Output 'https://user:uriCredential@example.invalid Authorization: Bearer bearerCredential token=assignedCredential {"token":"jsonCredential"} AWS_SECRET_ACCESS_KEY=environmentCredential ghp_abcdefghijklmnopqrstuvwxyz1234 AKIA1234567890ABCDEF';[Console]::Out.Write('trailing-output');exit 7}
 default {exit 0}
}
'@
        [IO.File]::WriteAllText($fakeRunner,$fake,[Text.UTF8Encoding]::new($false))
        $logRoot=Join-Path $local 'ItoevaEvolutionRunner\logs';New-Item -ItemType Directory -Path $logRoot -Force|Out-Null
        $oldLog=Join-Path $logRoot 'old.log';$recentLog=Join-Path $logRoot 'recent.log';[IO.File]::WriteAllText($oldLog,'old');[IO.File]::WriteAllText($recentLog,'recent');(Get-Item $oldLog).LastWriteTimeUtc=[DateTime]::UtcNow.AddDays(-31)
        $entryHash=Get-ItoevaSha256 (Join-Path $PSScriptRoot 'Invoke-ItoevaEvolution.ps1');$moduleHash=Get-ItoevaSha256 (Join-Path $PSScriptRoot 'Itoeva.Runner.psm1')
        $oldLocal=$env:LOCALAPPDATA
        try{
            $env:LOCALAPPDATA=$local
            $result=Invoke-ItoevaProcessWithTimeout 'powershell.exe' @('-NoProfile','-ExecutionPolicy','Bypass','-File',$wrapper,'-Action','Preflight','-Repository','C:\repo with spaces','-ActivationPath','C:\secret activation.json') $wrapperRoot 20
            Assert-True ($result.exitCode -eq 7) "Wrapper verlor Exitcode: $($result.output)"
            $preflightLog=Get-ChildItem -LiteralPath $logRoot -Filter '*-no-run-id.log'|Sort-Object LastWriteTime|Select-Object -Last 1;$text=[IO.File]::ReadAllText($preflightLog.FullName,[Text.UTF8Encoding]::new($false))
            foreach($secret in @('uriCredential','bearerCredential','assignedCredential','jsonCredential','environmentCredential','ghp_abcdefghijklmnopqrstuvwxyz1234','AKIA1234567890ABCDEF','secret activation')){Assert-True ($text -notmatch [regex]::Escape($secret)) "Secret im Log: $secret"}
            $hasRedaction=$text -match '\[REDACTED\]';$hasStderr=$text -match 'stderr-output';$hasTrailing=$text -match 'trailing-output'
            Assert-True ($hasRedaction -and $hasStderr -and $hasTrailing) "Output oder Redaction fehlt: redaction=$hasRedaction stderr=$hasStderr trailing=$hasTrailing"
            Assert-True (-not(Test-Path -LiteralPath $oldLog) -and (Test-Path -LiteralPath $recentLog)) '30-Tage-Rotation ist falsch.'

            $result=Invoke-ItoevaProcessWithTimeout 'powershell.exe' @('-NoProfile','-ExecutionPolicy','Bypass','-File',$wrapper,'-Action','DryRun','-Repository','C:\repo with spaces') $wrapperRoot 20
            Assert-True ($result.exitCode -eq 0);Assert-True (@(Get-ChildItem -LiteralPath $logRoot -Filter '*-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.log').Count -eq 1) 'Neue Run-ID fehlt im Lognamen.'
            $result=Invoke-ItoevaProcessWithTimeout 'powershell.exe' @('-NoProfile','-ExecutionPolicy','Bypass','-File',$wrapper,'-Action','PublishDryRun','-RunId','dddddddddddddddddddddddddddddddd') $wrapperRoot 20
            Assert-True ($result.exitCode -eq 0);Assert-True (@(Get-ChildItem -LiteralPath $logRoot -Filter '*-dddddddddddddddddddddddddddddddd.log').Count -eq 1) 'Bestehende Run-ID fehlt im Lognamen.'
            $result=Invoke-ItoevaProcessWithTimeout 'powershell.exe' @('-NoProfile','-ExecutionPolicy','Bypass','-File',$wrapper,'-Action','Run') $wrapperRoot 20
            Assert-True ($result.exitCode -eq 0);Assert-True (@(Get-ChildItem -LiteralPath $logRoot -Filter '*-no-run-id.log').Count -ge 2) 'Mehrdeutige Run-ID muss no-run-id ergeben.'
        } finally {$env:LOCALAPPDATA=$oldLocal}
        Assert-True ((Get-ItoevaSha256 (Join-Path $PSScriptRoot 'Invoke-ItoevaEvolution.ps1')) -eq $entryHash) 'Wrapper änderte den Runner.'
        Assert-True ((Get-ItoevaSha256 (Join-Path $PSScriptRoot 'Itoeva.Runner.psm1')) -eq $moduleHash) 'Wrapper änderte das Modul.'
        $source=Get-Content -Raw -LiteralPath $wrapperSource
        Assert-True ($source -match 'ReparsePoint' -and $source -match 'FileMode\]::CreateNew' -and $source -match 'rotationCandidates = @\(\)' -and $source -notmatch 'Start-Process') 'Pfadschutz, fail-safe Rotation oder sichere Prozessausführung fehlt.'
    }
    Test-Case 'Proposed Tree enthält geänderte und neue Dateien' {
        $repo = Join-Path $tempRoot 'repo'
        New-Item -ItemType Directory -Path $repo | Out-Null
        & git -C $repo init --quiet
        & git -C $repo config user.name 'Itoeva Runner Test'
        & git -C $repo config user.email 'runner-test@example.invalid'
        [IO.File]::WriteAllText((Join-Path $repo 'existing.txt'), "before`n")
        & git -C $repo add -- existing.txt
        & git -C $repo commit --quiet -m baseline
        if ($LASTEXITCODE -ne 0) { throw 'Temporäres Baseline-Commit fehlgeschlagen.' }
        $base = (& git -C $repo rev-parse HEAD).Trim()
        [IO.File]::WriteAllText((Join-Path $repo 'existing.txt'), "after`n")
        [IO.File]::WriteAllText((Join-Path $repo 'new.txt'), "new`n")

        $paths = @(Get-ItoevaChangedPaths -Repository $repo)
        Assert-True (($paths -join ',') -eq 'existing.txt,new.txt')
        $tree = Get-ItoevaProposedTreeOid -Repository $repo -BaseSha $base -AllowedPaths $paths
        Assert-True ($tree -match '^[0-9a-f]{40,64}$')
        Assert-True ((& git -C $repo status --porcelain).Count -eq 2) 'Temporärer Index darf den echten Index nicht verändern.'
    }
    Test-Case 'unerwarteter Credential Helper wird erkannt' {
        $repo = Join-Path $tempRoot 'unsafe-config-repo'
        New-Item -ItemType Directory -Path $repo | Out-Null
        & git -C $repo init --quiet
        & git -C $repo config credential.helper 'unexpected-helper'
        $findings = @(Get-ItoevaDangerousGitConfig $repo $config)
        Assert-True (($findings -join "`n") -match 'unexpected-helper')
    }
    Test-Case 'Fake-Remote akzeptiert nur geprüften neuen Evolution-Branch' {
        $remote = Join-Path $tempRoot 'remote.git'
        $seed = Join-Path $tempRoot 'seed'
        $runnerRepo = Join-Path $tempRoot 'runner-repo'
        $hooks = Join-Path $tempRoot 'empty-hooks'
        New-Item -ItemType Directory -Path $seed,$hooks | Out-Null
        & git init --bare --quiet $remote
        & git -C $seed init --quiet
        & git -C $seed config user.name 'Itoeva Runner Test'
        & git -C $seed config user.email 'runner-test@example.invalid'
        [IO.File]::WriteAllText((Join-Path $seed 'source.txt'), "base`n")
        & git -C $seed add -- source.txt
        & git -C $seed commit --quiet -m baseline
        & git -C $seed remote add origin $remote
        & git -C $seed push --quiet origin HEAD:refs/heads/main
        & git clone --quiet --branch main $remote $runnerRepo
        & git -C $runnerRepo config user.name 'Itoeva Runner Test'
        & git -C $runnerRepo config user.email 'runner-test@example.invalid'
        $base = (& git -C $runnerRepo rev-parse HEAD).Trim()
        $branch = 'evolution/002-fake-publish-fedcba9876543210fedcba9876543210'
        & git -C $runnerRepo switch --quiet -c $branch $base
        [IO.File]::WriteAllText((Join-Path $runnerRepo 'source.txt'), "evolved`n")
        $paths = @(Get-ItoevaChangedPaths $runnerRepo)
        $tree = Get-ItoevaProposedTreeOid $runnerRepo $base $paths
        $hash='c'*64; $plan='d'*64
        $gate = [pscustomobject]@{ planReview='PASS'; mandatoryTests='PASS'; finalReview='PASS'; diffCheck='PASS'; baseUnchanged=$true; baseSha=$base; proposedTreeOid=$tree; testManifestHash=$hash; planHash=$plan; reviewBaseSha=$base; reviewPlanHash=$plan; reviewTreeOid=$tree; reviewTestManifestHash=$hash }
        $config.repository.expectedOrigin = $remote
        $config.publication.enabled = $true
        $published = Publish-ItoevaEvolution $runnerRepo $branch $base 'Fake publish' $paths $tree $gate $config $hooks
        Assert-True ($published.status -eq 'PUSHED')
        Assert-True ((Get-ItoevaRemoteSha $runnerRepo 'origin' "refs/heads/$branch") -eq $published.commitSha)
        Assert-True ((Get-ItoevaRemoteSha $runnerRepo 'origin' 'refs/heads/main') -eq $base)

        $collisionRepo = Join-Path $tempRoot 'collision-repo'
        & git clone --quiet --branch main $remote $collisionRepo
        & git -C $collisionRepo config user.name 'Itoeva Runner Test'
        & git -C $collisionRepo config user.email 'runner-test@example.invalid'
        & git -C $collisionRepo switch --quiet -c $branch $base
        [IO.File]::WriteAllText((Join-Path $collisionRepo 'source.txt'), "collision`n")
        $collisionPaths = @(Get-ItoevaChangedPaths $collisionRepo)
        $collisionTree = Get-ItoevaProposedTreeOid $collisionRepo $base $collisionPaths
        $collisionGate = [pscustomobject]@{ planReview='PASS'; mandatoryTests='PASS'; finalReview='PASS'; diffCheck='PASS'; baseUnchanged=$true; baseSha=$base; proposedTreeOid=$collisionTree; testManifestHash=$hash; planHash=$plan; reviewBaseSha=$base; reviewPlanHash=$plan; reviewTreeOid=$collisionTree; reviewTestManifestHash=$hash }
        Assert-Throws { Publish-ItoevaEvolution $collisionRepo $branch $base 'Collision' $collisionPaths $collisionTree $collisionGate $config $hooks | Out-Null }

        [IO.File]::WriteAllText((Join-Path $seed 'source.txt'), "new main`n")
        & git -C $seed add -- source.txt
        & git -C $seed commit --quiet -m move-main
        & git -C $seed push --quiet origin HEAD:refs/heads/main
        Assert-Throws { Assert-ItoevaBaseUnchanged $runnerRepo $base | Out-Null }
        $config.publication.enabled = $false
    }
} finally {
    if (Test-Path -LiteralPath $tempRoot) { Remove-Item -LiteralPath $tempRoot -Recurse -Force }
}

Write-Host "RESULT passed=$script:passed failed=$script:failed"
if ($script:failed -ne 0) { exit 1 }
