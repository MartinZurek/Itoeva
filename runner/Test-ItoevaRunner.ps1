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
