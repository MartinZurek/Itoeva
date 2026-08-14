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
