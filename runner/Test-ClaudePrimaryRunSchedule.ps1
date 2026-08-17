# Tests fuer den zeitgesteuerten Betrieb von ClaudePrimaryRun.
#
# Zwei Arten von Pruefungen, bewusst getrennt gehalten:
#
#   FUNKTIONAL  Alles, was echtes Fehlerrisiko traegt und sich ohne GitHub ausfuehren laesst:
#               das Parsen des Backlogs gegen echte Fehlerfaelle und die Frage, ob die
#               Allowlist evolutions/BACKLOG.md tatsaechlich abweist. Beides laeuft gegen
#               denselben Code, den der Workflow benutzt - nicht gegen eine Kopie.
#
#   STRUKTURELL Alles, wo die Jobtrennung selbst die Zusicherung ist. Dass bei
#               SKIPPED_PENDING_EVOLUTION kein Claude laeuft, folgt nicht aus einer
#               Schrittreihenfolge, sondern daraus, dass jeder claude-Aufruf im Job evolve
#               steht und evolve hinter `if: decision == 'GO'` haengt. Genau das wird hier
#               nachgewiesen.
#
# Aufruf:  .\runner\Test-ClaudePrimaryRunSchedule.ps1

[CmdletBinding()]
param(
    [string]$WorkflowPath,
    [string]$BacklogPath,
    [string]$SelectorPath,
    [string]$ConfigPath
)

$ErrorActionPreference = 'Stop'
$script:Failures = @()

# $PSScriptRoot ist im param-Block unter Windows PowerShell 5.1 noch leer.
if (-not $WorkflowPath) { $WorkflowPath = Join-Path $PSScriptRoot '..\.github\workflows\claude-primary-run.yml' }
if (-not $BacklogPath)  { $BacklogPath  = Join-Path $PSScriptRoot '..\evolutions\BACKLOG.md' }
if (-not $SelectorPath) { $SelectorPath = Join-Path $PSScriptRoot 'backlog-select.sh' }
if (-not $ConfigPath)   { $ConfigPath   = Join-Path $PSScriptRoot 'runner.config.json' }

function Test-That {
    # $Condition bewusst nicht als [bool] typisiert: -match liefert auf einem leeren Array
    # ein Array zurueck, was die Parameterbindung sonst mit einem Typfehler abbricht statt
    # den Test schlicht scheitern zu lassen.
    param([string]$Name, $Condition, [string]$Detail = '')
    if ($Condition -is [array]) { $Condition = ($Condition.Count -gt 0) }
    $Condition = [bool]$Condition
    if ($Condition) { Write-Host ("  PASS  {0}" -f $Name) }
    else {
        $script:Failures += $Name
        Write-Host ("  FAIL  {0}{1}" -f $Name, $(if ($Detail) { " - $Detail" } else { '' }))
    }
}

$bash = (Get-Command bash -ErrorAction SilentlyContinue).Source
if (-not $bash) { $bash = 'C:\Program Files\Git\bin\bash.exe' }
if (-not (Test-Path -LiteralPath $bash)) { throw "bash nicht gefunden - die funktionalen Tests brauchen es." }

function Invoke-Selector {
    param([string]$BacklogContent, [switch]$OmitFile)
    $dir = Join-Path ([IO.Path]::GetTempPath()) ("itoeva-bl-" + [Guid]::NewGuid().ToString('N').Substring(0, 8))
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    $file = Join-Path $dir 'BACKLOG.md'
    if (-not $OmitFile) {
        # LF-Zeilenenden: der Selektor laeuft auf dem Linux-Runner.
        [IO.File]::WriteAllText($file, ($BacklogContent -replace "`r`n", "`n"))
    }
    $out = Join-Path $dir 'out'
    # Git Bash versteht Windows-Pfade mit Schraegstrichen direkt - eine Umrechnung nach
    # /c/... ist unnoetig und war die Fehlerquelle beim ersten Anlauf.
    $sel = ((Resolve-Path -LiteralPath $SelectorPath).Path) -replace '\\', '/'
    $bf  = $file -replace '\\', '/'
    $od  = $out  -replace '\\', '/'
    $stderr = Join-Path $dir 'err.txt'
    $ef = $stderr -replace '\\', '/'
    # Bewusst der Aufrufoperator statt Start-Process: dessen -ArgumentList zerlegt den
    # -c-String an den Leerzeichen, bash bekaeme dann nur "sh" ohne Skript, laese von stdin
    # und lieferte immer Exitcode 0. Die Fehlerausgabe wird innerhalb von bash umgeleitet,
    # weil PowerShell 5.1 umgeleitete native stderr-Ausgaben in Fehlerobjekte verwandelt.
    & $bash -c "sh '$sel' '$bf' '$od' 2>'$ef'" | Out-Null
    $code = $LASTEXITCODE
    [pscustomobject]@{
        Code   = $code
        Id     = if (Test-Path (Join-Path $out 'id'))    { (Get-Content -Raw (Join-Path $out 'id')).Trim() }    else { $null }
        Title  = if (Test-Path (Join-Path $out 'title')) { (Get-Content -Raw (Join-Path $out 'title')).Trim() } else { $null }
        Task   = if (Test-Path (Join-Path $out 'task'))  { (Get-Content -Raw (Join-Path $out 'task')) }         else { $null }
        Stderr = [string](Get-Content -Raw -LiteralPath $stderr -ErrorAction SilentlyContinue)
        Dir    = $dir
    }
}

$workflow = Get-Content -Raw -LiteralPath $WorkflowPath
$config   = Get-Content -Raw -LiteralPath $ConfigPath | ConvertFrom-Json

# =========================================================================================
Write-Host ''
Write-Host '=== FUNKTIONAL: Backlog-Auswahl ==='

$gueltig = @'
# Backlog

## Format
Prosa, die kein Aufgabenkopf ist und den Aufgabentext nicht verunreinigen darf.

## [open] ITO-0001 - Erste Aufgabe
Zeile eins der ersten Aufgabe.
Zeile zwei der ersten Aufgabe.

## [open] ITO-0002 - Zweite Aufgabe
Diese Aufgabe darf NICHT gewaehlt werden.
'@

$r = Invoke-Selector $gueltig
Test-That 'gueltiges Backlog: Exitcode 0'                 ($r.Code -eq 0) "code=$($r.Code) $($r.Stderr)"
Test-That 'gueltiges Backlog: oberster offener Eintrag'   ($r.Id -eq 'ITO-0001') "id=$($r.Id)"
Test-That 'gueltiges Backlog: Titel korrekt'              ($r.Title -eq 'Erste Aufgabe')
Test-That 'gueltiges Backlog: beide Textzeilen enthalten' (($r.Task -match 'Zeile eins') -and ($r.Task -match 'Zeile zwei'))
Test-That 'gueltiges Backlog: Folgeaufgabe nicht mitgezogen' ($r.Task -notmatch 'Zweite Aufgabe|NICHT gewaehlt')
Test-That 'gueltiges Backlog: Prosa nicht mitgezogen'     ($r.Task -notmatch 'Prosa, die kein')

$erledigtOben = @'
## [done] ITO-0001 - Schon erledigt
Alter Text.

## [open] ITO-0002 - Die naechste
Text der naechsten Aufgabe.
'@
$r = Invoke-Selector $erledigtOben
Test-That 'done wird uebersprungen, open darunter gewaehlt' (($r.Code -eq 0) -and ($r.Id -eq 'ITO-0002')) "code=$($r.Code) id=$($r.Id)"
Test-That 'done-Text wird nicht mitgeschleppt'             ($r.Task -notmatch 'Alter Text')

Write-Host ''
Write-Host '=== FUNKTIONAL: leerer Backlog -> sauberes Ueberspringen (Code 2) ==='

$r = Invoke-Selector "# Backlog`n`nNoch nichts eingetragen.`n"
Test-That 'Backlog ohne Eintraege: Exitcode 2' ($r.Code -eq 2) "code=$($r.Code)"
$r = Invoke-Selector "## [done] ITO-0001 - Alles erledigt`nText.`n"
Test-That 'Backlog nur mit done: Exitcode 2'   ($r.Code -eq 2) "code=$($r.Code)"
Test-That 'kein Ueberspringen ohne Begruendung' ($r.Stderr -match 'Kein offener Eintrag')

Write-Host ''
Write-Host '=== FUNKTIONAL: fehlerhafter Backlog -> fail-closed (Code 3/4) ==='

foreach ($fall in @(
    @{ Name = 'unbekannter Status'; Text = "## [offen] ITO-0001 - Titel`nText.`n";      Muster = 'Formatfehler' },
    @{ Name = 'Status vertippt';    Text = "## [opne] ITO-0001 - Titel`nText.`n";       Muster = 'Formatfehler' },
    @{ Name = 'fehlendes Leerzeichen nach ##'; Text = "##[open] ITO-0001 - Titel`nText.`n"; Muster = 'Formatfehler' },
    @{ Name = 'ID falsch formatiert'; Text = "## [open] ITO-1 - Titel`nText.`n";        Muster = 'Formatfehler' },
    @{ Name = 'Titel fehlt';        Text = "## [open] ITO-0001 -`nText.`n";             Muster = 'Formatfehler' },
    @{ Name = 'doppelte ID';        Text = "## [open] ITO-0001 - A`nText A.`n`n## [done] ITO-0001 - B`nText B.`n"; Muster = 'Doppelte ID' },
    @{ Name = 'leerer Aufgabentext'; Text = "## [open] ITO-0001 - Titel`n`n## [open] ITO-0002 - Zwei`nText.`n";    Muster = 'ist leer' }
)) {
    $r = Invoke-Selector $fall.Text
    Test-That ("fehlerhaft ({0}): Exitcode 3" -f $fall.Name) ($r.Code -eq 3) "code=$($r.Code)"
    Test-That ("fehlerhaft ({0}): nennt den Grund" -f $fall.Name) ($r.Stderr -match $fall.Muster) $r.Stderr.Trim()
    Test-That ("fehlerhaft ({0}): keine Aufgabe ausgegeben" -f $fall.Name) ($null -eq $r.Task)
}

$r = Invoke-Selector '' -OmitFile
Test-That 'fehlende Backlogdatei: Exitcode 4' ($r.Code -eq 4) "code=$($r.Code)"

Write-Host ''
Write-Host '=== FUNKTIONAL: echtes evolutions/BACKLOG.md ist verwertbar ==='

$echt = Get-Content -Raw -LiteralPath $BacklogPath
$r = Invoke-Selector $echt
Test-That 'eingechecktes Backlog laesst sich lesen' ($r.Code -eq 0) "code=$($r.Code) $($r.Stderr)"
Test-That 'eingechecktes Backlog: ID im erwarteten Format' ($r.Id -match '^ITO-\d{4}$') "id=$($r.Id)"
Test-That 'eingechecktes Backlog: Aufgabentext ist substanziell' ($r.Task.Trim().Length -gt 200) "Laenge=$($r.Task.Trim().Length)"

Write-Host ''
Write-Host '=== FUNKTIONAL: Builder kann das Backlog nicht aendern ==='

Test-That 'BACKLOG.md steht in forbiddenFileNames' ($config.scope.forbiddenFileNames -contains 'BACKLOG.md')

# Dieselbe Pruefung, die der Workflow fahrt: Dateiname gegen die Allowlist des Basiscommits.
$namen = $config.scope.forbiddenFileNames
foreach ($pfad in @('evolutions/BACKLOG.md', 'BACKLOG.md', 'app-sim/BACKLOG.md')) {
    $basename = $pfad.Split('/')[-1]
    Test-That ("Allowlist weist '{0}' ab" -f $pfad) ($namen -contains $basename)
}
Test-That "Allowlist laesst eine normale Testdatei durch" `
    (-not ($namen -contains 'MatrixColorsTest.kt'))
Test-That 'Allowlist wird im Workflow aus dem Basiscommit gelesen' `
    ($workflow -match 'git show "\$BASE:runner/runner\.config\.json"')

Write-Host ''
Write-Host '=== FUNKTIONAL: Aufgabentext erreicht GITHUB_OUTPUT unversehrt ==='

# **Der Regressionsfall.** Lauf 32019367972 (workflow_dispatch) brach im preflight ab,
# nachdem die Vorentscheidung bereits GO gemeldet hatte:
#
#   Auftrag aus der Eingabe uebernommen (1035 Bytes).
#   CLAUDE_PRIMARY_PREFLIGHT: GO (MANUAL_INPUT)
#   ##[error]Invalid value. Matching delimiter not found 'ITOEVA_TASK_EOF_0d5f'
#
# $TASKFILE entsteht im manuellen Zweig mit printf '%s' und endet deshalb NICHT auf einem
# Zeilenumbruch - die Endmarke klebte an der letzten Aufgabenzeile. Der Zeitplan-Pfad war nie
# betroffen, weil backlog-select.sh mit printf("%s\n", body) schreibt; genau deshalb blieb der
# Fehler bis zum ersten manuellen Lauf nach Einfuehrung des preflight-Jobs unentdeckt. Ein
# rein struktureller Test haette ihn ebenfalls nicht gefunden: das YAML war gueltig, nur das
# Ergebnis war es nicht.
#
# Geprueft wird deshalb der ECHTE Block, aus dem Workflow herausgeschnitten und ausgefuehrt -
# keine Kopie, die stillschweigend auseinanderlaufen koennte.

$delim = 'ITOEVA_TASK_EOF_0d5f'
$blockMatch = [regex]::Match(
    $workflow,
    '(?s)\{\s*\n\s*echo "task<<ITOEVA_TASK_EOF_0d5f".*?\}\s*>>\s*"\$GITHUB_OUTPUT"')
Test-That 'Output-Block laesst sich aus dem Workflow herausschneiden' $blockMatch.Success

function Invoke-TaskOutputBlock {
    # Fuehrt den herausgeschnittenen Block mit einem vorgegebenen $TASKFILE aus und liefert
    # zurueck, was dabei woertlich in $GITHUB_OUTPUT landet. Bytes statt Text, damit ein
    # fehlender Zeilenumbruch am Ende nicht schon beim Schreiben der Vorlage verlorengeht.
    param([string]$Block, [byte[]]$TaskBytes)
    $dir = Join-Path ([IO.Path]::GetTempPath()) ("itoeva-out-" + [Guid]::NewGuid().ToString('N').Substring(0, 8))
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    $taskFile = Join-Path $dir 'task'
    $outFile  = Join-Path $dir 'github_output'
    $scriptFile = Join-Path $dir 'block.sh'
    $errFile  = Join-Path $dir 'err.txt'
    [IO.File]::WriteAllBytes($taskFile, $TaskBytes)
    [IO.File]::WriteAllBytes($outFile, [byte[]]@())
    [IO.File]::WriteAllText($scriptFile, ("set -eu`n" + ($Block -replace "`r`n", "`n") + "`n"),
        (New-Object Text.UTF8Encoding($false)))
    $sf = $scriptFile -replace '\\', '/'
    $tf = $taskFile   -replace '\\', '/'
    $of = $outFile    -replace '\\', '/'
    $ef = $errFile    -replace '\\', '/'
    & $bash -c "TASKFILE='$tf' GITHUB_OUTPUT='$of' sh '$sf' 2>'$ef'" | Out-Null
    $code = $LASTEXITCODE
    [pscustomobject]@{
        Code   = $code
        Raw    = [Text.Encoding]::UTF8.GetString([IO.File]::ReadAllBytes($outFile))
        Stderr = [string](Get-Content -Raw -LiteralPath $errFile -ErrorAction SilentlyContinue)
    }
}

function Get-HeredocValue {
    # Liest den Wert so aus, wie GitHub es tut: alles zwischen der Kopfzeile "name<<marke"
    # und einer Zeile, die AUSSCHLIESSLICH aus der Marke besteht. Steht die Marke nicht auf
    # einer eigenen Zeile, gibt es keinen Wert - $null ist genau der beobachtete Fehlerfall.
    param([string]$Raw, [string]$Name, [string]$Delim)
    $lines = $Raw -split "`n"
    $start = -1
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -eq ("{0}<<{1}" -f $Name, $Delim)) { $start = $i; break }
    }
    if ($start -lt 0) { return $null }
    $acc = @()
    for ($i = $start + 1; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -eq $Delim) { return ($acc -join "`n") }
        $acc += $lines[$i]
    }
    return $null
}

if ($blockMatch.Success) {
    $block = $blockMatch.Value
    $utf8  = New-Object Text.UTF8Encoding($false)

    # Die drei Formen, in denen ein Aufgabentext hier ankommen kann. Fall 1 ist woertlich die
    # Gestalt, an der Lauf 32019367972 zerbrach: eine einzige Zeile ohne Abschluss, wie sie
    # das einzeilige workflow_dispatch-Formular und ein API-Dispatch beide liefern.
    $faelle = @(
        @{ Name     = 'einzeiliger manueller Input ohne abschliessenden Newline'
           Bytes    = $utf8.GetBytes('Ergaenze robuste JVM-Unit-Tests fuer matrix/PlayGamePlan.kt. Stufen muessen streng aufsteigend und ohne doppelte fromLevel sein.')
           Erwartet = 'Ergaenze robuste JVM-Unit-Tests fuer matrix/PlayGamePlan.kt. Stufen muessen streng aufsteigend und ohne doppelte fromLevel sein.'
           Angehaengt = $true },
        @{ Name     = 'mehrzeiliger Input ohne abschliessenden Newline'
           Bytes    = $utf8.GetBytes("Zeile eins der Aufgabe.`nZeile zwei der Aufgabe.`nZeile drei ohne Abschluss.")
           Erwartet = "Zeile eins der Aufgabe.`nZeile zwei der Aufgabe.`nZeile drei ohne Abschluss."
           Angehaengt = $true },
        # Der Zeitplan-Pfad. Seine Semantik darf sich NICHT aendern: GitHub schluckt den einen
        # abschliessenden Umbruch, und genau so war es immer schon.
        @{ Name     = 'Input mit bereits vorhandenem abschliessenden Newline'
           Bytes    = $utf8.GetBytes("Zeile eins der Aufgabe.`nZeile zwei der Aufgabe.`n")
           Erwartet = "Zeile eins der Aufgabe.`nZeile zwei der Aufgabe."
           Angehaengt = $false },
        # Letztes Byte gehoert zu einem mehrbyteigen Zeichen (hier "ae" als U+00E4, C3 A4).
        # Deutsche Auftraege enden regelmaessig auf einem Umlaut; wuerde die Erkennung auf
        # Zeichen statt auf Bytes bauen, faende sie hier nichts und schoebe keinen Umbruch nach.
        @{ Name     = 'Input endet auf einem mehrbyteigen Zeichen, ohne Newline'
           Bytes    = [byte[]]@(0x4C, 0x61, 0x75, 0x66, 0x20, 0xC3, 0xA4)
           Erwartet = [Text.Encoding]::UTF8.GetString([byte[]]@(0x4C, 0x61, 0x75, 0x66, 0x20, 0xC3, 0xA4))
           Angehaengt = $true }
    )

    foreach ($fall in $faelle) {
        $r = Invoke-TaskOutputBlock -Block $block -TaskBytes $fall.Bytes
        Test-That ("{0}: Block laeuft fehlerfrei" -f $fall.Name) ($r.Code -eq 0) "code=$($r.Code) $($r.Stderr)"

        # Der eigentliche Befund: eine Zeile, die nur aus der Endmarke besteht.
        $eigeneZeile = (($r.Raw -split "`n") -contains $delim)
        Test-That ("{0}: Endmarke steht auf einer eigenen Zeile" -f $fall.Name) $eigeneZeile `
            ("Rohausgabe endet auf: " + ($r.Raw.Substring([Math]::Max(0, $r.Raw.Length - 60)) -replace "`n", '\n'))

        $wert = Get-HeredocValue -Raw $r.Raw -Name 'task' -Delim $delim
        Test-That ("{0}: GitHub kann den Wert ueberhaupt lesen" -f $fall.Name) ($null -ne $wert)
        Test-That ("{0}: Aufgabentext kommt unveraendert an" -f $fall.Name) ($wert -ceq $fall.Erwartet) `
            ("erhalten=" + ($wert -replace "`n", '\n'))

        # Genau EIN Umbruch - nicht keiner (der Fehler) und nicht zwei (das haenge an jeden
        # Zeitplan-Auftrag eine Leerzeile und veraenderte damit den Aufgabentext).
        Test-That ("{0}: kein zusaetzlicher Leerraum vor der Endmarke" -f $fall.Name) `
            (-not ($r.Raw -like ("*`n`n{0}*" -f $delim)))

        # Gegenprobe zur Absicht: nachgeschoben wird nur, wo wirklich nichts da war.
        $roh = [Text.Encoding]::UTF8.GetString($fall.Bytes)
        $erwarteteRohform = ("task<<{0}`n{1}{2}{0}`n" -f $delim, $roh, $(if ($fall.Angehaengt) { "`n" } else { '' }))
        Test-That ("{0}: Rohausgabe hat exakt die erwartete Gestalt" -f $fall.Name) `
            ($r.Raw -ceq $erwarteteRohform)
    }
}

# =========================================================================================
Write-Host ''
Write-Host '=== STRUKTURELL: Ausloeser und Vorentscheidung ==='

Test-That 'workflow_dispatch bleibt vollstaendig erhalten' `
    (($workflow -match '(?m)^  workflow_dispatch:') -and ($workflow -match '(?m)^      task:') -and
     ($workflow -match '(?m)^      model:') -and ($workflow -match '(?m)^      review_model:'))
Test-That 'genau ein schedule-Eintrag' `
    (([regex]::Matches($workflow, "(?m)^\s+- cron:")).Count -eq 1)
# Umgestellt von taeglich 04:17 auf alle drei Stunden im Tagfenster, nachdem Lauf
# 32041086900 als erster vollstaendig gruen durchlief. Traegt der Waechter nicht mehr,
# waeren das sechs teure Laeufe am Tag statt einem je gemergter Evolution - deshalb steht
# die Waechter-Pruefung weiter unten und muss zusammen mit dieser Zeile bestehen bleiben.
Test-That 'schedule steht auf alle drei Stunden im Tagfenster' `
    ($workflow -match "cron: '17 3-18/3 \* \* \*'")
# Nicht zur vollen Stunde: GitHub ueberbucht diese Slots und laesst Laeufe dort ausfallen.
Test-That 'schedule feuert nicht zur vollen Stunde' `
    ($workflow -notmatch "(?m)^\s+- cron: '0 ")
# Das Fenster ist der Kostendeckel: jede uebersprungene Ausloesung kostet rund eine
# abgerechnete Minute, auch bei leerem Backlog. Ein voller */3-Takt waere ein Drittel mehr
# Grundlast fuer Slots, deren Ergebnis bis zum Morgen ohnehin liegen bliebe.
Test-That 'schedule laeuft nicht rund um die Uhr' `
    ($workflow -notmatch "(?m)^\s+- cron: '\d+ \*/")
Test-That 'Concurrency unveraendert' `
    (($workflow -match 'group: claude-primary-run') -and ($workflow -match 'cancel-in-progress: false'))

Test-That 'preflight-Job vorhanden' ($workflow -match '(?m)^  preflight:')
Test-That 'preflight hat nur Leserechte' `
    ($workflow -match '(?ms)^  preflight:.*?permissions:\s*\n\s+contents: read\s*\n\s+pull-requests: read')
Test-That 'evolve haengt an preflight'     ($workflow -match '(?m)^    needs: preflight')
Test-That 'evolve laeuft nur bei GO'       ($workflow -match "if: needs\.preflight\.outputs\.decision == 'GO'")
Test-That 'publish haengt weiterhin an evolve' ($workflow -match '(?m)^    needs: evolve')

Write-Host ''
Write-Host '=== STRUKTURELL: kein Claude-Aufruf ohne GO ==='

# Der Nachweis stuetzt sich auf die Jobgrenze, nicht auf Schrittreihenfolgen: alle
# Modellaufrufe liegen in evolve, und evolve startet ohne GO nicht.
$evolveStart  = $workflow.IndexOf("`n  evolve:")
$publishStart = $workflow.IndexOf("`n  publish:")
$preflight    = $workflow.Substring($workflow.IndexOf("`n  preflight:"), $evolveStart - $workflow.IndexOf("`n  preflight:"))
$evolve       = $workflow.Substring($evolveStart, $publishStart - $evolveStart)
$publish      = $workflow.Substring($publishStart)

$claudeAufrufe = ([regex]::Matches($workflow, 'claude -p')).Count
Test-That 'genau zwei Claude-Aufrufe im Workflow'      ($claudeAufrufe -eq 2) "gefunden=$claudeAufrufe"
Test-That 'beide Claude-Aufrufe liegen im Job evolve'  (([regex]::Matches($evolve, 'claude -p')).Count -eq 2)
Test-That 'preflight ruft Claude nicht auf'            ($preflight -notmatch 'claude -p')
Test-That 'preflight kennt das Abo-Secret nicht'       ($preflight -notmatch 'CLAUDE_CODE_OAUTH_TOKEN')
Test-That 'preflight installiert die CLI nicht'        ($preflight -notmatch 'claude-code')
Test-That 'preflight startet keine Tests'              (($preflight -notmatch 'gradlew') -and ($preflight -notmatch 'emulator-runner'))
Test-That 'publish ruft Claude nicht auf'              ($publish -notmatch 'claude -p')
Test-That 'GITHUB_TOKEN bleibt aus der Builder-VM heraus' `
    (($preflight -match 'secrets\.GITHUB_TOKEN') -and ($evolve -notmatch 'secrets\.GITHUB_TOKEN'))

Write-Host ''
Write-Host '=== STRUKTURELL: Eingabenormalisierung und Modell-Fallbacks ==='

Test-That 'Builder-Modell faellt auf claude-sonnet-5 zurueck' `
    ($preflight -match "MODEL='claude-sonnet-5'")
Test-That 'Reviewer-Modell faellt auf claude-opus-5 zurueck' `
    ($preflight -match "RMODEL='claude-opus-5'")
Test-That 'Modellnamen werden gegen eine Allowlist geprueft' `
    (([regex]::Matches($preflight, 'claude-sonnet-5\|claude-opus-5')).Count -eq 2)
Test-That 'manueller Lauf verwendet die Eingabe' `
    ($preflight -match "EVENT.*=.*workflow_dispatch")
Test-That 'leerer Auftrag bricht fail-closed ab' `
    ($preflight -match 'Leerer Auftrag im manuellen Lauf')
Test-That 'Endmarke wird notfalls auf eine eigene Zeile gezwungen' `
    ($preflight -match 'if \[ -n "\$\(tail -c1 "\$TASKFILE"\)" \]; then echo ""; fi')
Test-That 'Zeitplan-Lauf zieht die Aufgabe aus dem Backlog' `
    ($preflight -match 'runner/backlog-select\.sh evolutions/BACKLOG\.md')
Test-That 'evolve bekommt Aufgabe und Modelle aus preflight' `
    (($evolve -match 'needs\.preflight\.outputs\.task') -and
     ($evolve -match 'needs\.preflight\.outputs\.model') -and
     ($evolve -match 'needs\.preflight\.outputs\.review_model'))
Test-That 'evolve liest keine Inputs mehr direkt' ($evolve -notmatch '\binputs\.')

Write-Host ''
Write-Host '=== STRUKTURELL: Waechter gegen parallele Evolutionen ==='

Test-That 'Waechter sieht nach claude-evolution-Branches' `
    ($preflight -match 'matching-refs/heads/claude-evolution')
Test-That 'Waechter sieht nach offenen Pull Requests' `
    (($preflight -match 'pulls\?state=open') -and ($preflight -match 'startswith\("claude-evolution/"\)'))
# Entscheidend: nicht die blosse Existenz eines Branches blockiert, sondern ein Branch, der
# noch nicht in main enthalten ist. Sonst stuende der Zeitplan nach der ersten Evolution
# dauerhaft still, weil Branches bewusst nie geloescht werden.
Test-That 'nur unvereinigte Branches gelten als laufende Evolution' `
    (($preflight -match 'compare/main\.\.\.') -and ($preflight -match 'identical\|behind'))
Test-That 'Ueberspringen heisst SKIPPED_PENDING_EVOLUTION' `
    ($preflight -match 'SKIPPED_PENDING_EVOLUTION')
Test-That 'leerer Backlog heisst SKIPPED_EMPTY_BACKLOG' `
    ($preflight -match 'SKIPPED_EMPTY_BACKLOG')
Test-That 'Waechter greift nicht bei manuellem Lauf' `
    ($preflight -match '(?s)workflow_dispatch.*?else.*?matching-refs')

Write-Host ''
Write-Host '=== STRUKTURELL: Sicherheitsarchitektur unveraendert ==='

# Kommentarzeilen entfernen, bevor nach verbotenen Konstrukten gesucht wird: der Workflow
# beschreibt in Prosa ausfuehrlich, was er NICHT tut ("Kein --force, kein Tag"), und genau
# diese Prosa hat den Scan sonst selbst ausgeloest.
$code        = (($workflow -split "`n") | Where-Object { $_ -notmatch '^\s*#' }) -join "`n"
$evolveCode  = (($evolve   -split "`n") | Where-Object { $_ -notmatch '^\s*#' }) -join "`n"

Test-That 'Builder ohne Bash'                  ($evolve -match '--tools "Read,Glob,Grep,Edit,Write"')
Test-That 'Reviewer read-only'                 ($evolve -match '--tools "Read,Glob,Grep"')
Test-That 'getrennte Sitzungen ohne Resume'    ($evolveCode -notmatch '--resume|--continue')
Test-That 'FD-OAuth in beiden Sitzungen'       (([regex]::Matches($evolve, 'CLAUDE_CODE_OAUTH_TOKEN_FILE_DESCRIPTOR=3')).Count -eq 2)
Test-That 'CLAUDE_CODE_REMOTE bleibt verboten' ($workflow -match 'CLAUDE_CODE_REMOTE')
Test-That 'CLI weiterhin auf 2.1.232 gepinnt'  ($workflow -match "CLAUDE_CLI_VERSION: '2\.1\.232'")
Test-That 'evolve ohne Schreibrecht'           ($evolve -match '(?ms)permissions:\s*\n\s+contents: read')
Test-That 'publish mit Schreibrecht'           ($publish -match '(?ms)permissions:\s*\n\s+contents: write')
Test-That 'evolve checkt ohne Credentials aus' ($evolve -match 'persist-credentials: false')
Test-That 'Git-Gates unveraendert' `
    (($evolve -match 'diff-tree --check') -and ($evolve -match 'git write-tree') -and
     ($evolve -match 'git commit-tree') -and ($evolve -match 'git bundle create'))
Test-That 'kein Force, Tag, Release, Merge, Rebase' `
    ($code -notmatch '--force|--force-with-lease|git tag|gh release|git merge |git rebase ')
Test-That 'genau ein Push auf claude-evolution/' `
    ((([regex]::Matches($code, 'git push')).Count -eq 1) -and ($publish -match 'refs/heads/\$\{BRANCH\}'))
# Die Waechter-Abfrage liest offene PRs (GET auf /pulls) - das ist erlaubt und noetig.
# Verboten ist das Anlegen, Mergen und Loeschen; danach wird gezielt gesucht.
Test-That 'kein Auto-PR' `
    (($code -notmatch 'gh pr create') -and ($code -notmatch "-X\s+POST[^\n]*\/pulls"))
Test-That 'kein Auto-Merge' `
    (($code -notmatch 'gh pr merge') -and ($code -notmatch '/merges') -and ($code -notmatch '/pulls/[^\n]*merge'))
Test-That 'kein Branch-Loeschen' `
    (($code -notmatch 'push[^\n]*--delete') -and ($code -notmatch 'push origin :') -and ($code -notmatch '-X\s+DELETE'))
# curl und URL stehen wegen der Zeilenfortsetzung auf verschiedenen Zeilen - deshalb (?s).
Test-That 'PR-Abfrage des Waechters ist lesend' `
    ($preflight -match '(?s)curl -fsS.{0,200}\$API/pulls\?state=open')

Write-Host ''
Write-Host '=== STRUKTURELL: Diagnosepaket ist keine Publish-Evidence ==='

# Lauf 32020621278 hinterliess ausser dem Joblog nichts - kein Diff, kein Builder-Summary,
# kein Urteil. Das Diagnosepaket schliesst diese Luecke, DARF dabei aber unter keinen
# Umstaenden zu einer zweiten Quelle werden, aus der sich ein Push speisen koennte.
Test-That 'Diagnosepaket entsteht nur bei einem Fehlschlag' `
    ($evolve -match '(?s)- name: Diagnosepaket bei Fehlschlag schnueren.*?if: failure\(\)')
# `if: failure()` sieht nur die vorangegangenen Schritte. Stuenden die beiden Forensik-
# Schritte irgendwo in der Mitte, bliebe ein Fehlschlag danach - Commit, Bundle, Uebergabe -
# wieder spurlos. Sie muessen die LETZTEN beiden Schritte von evolve sein.
$evolveSteps = [regex]::Matches($evolve, '(?m)^      - name: (.+)$') | ForEach-Object { $_.Groups[1].Value.Trim() }
Test-That 'die Forensik-Schritte stehen ganz am Ende von evolve' `
    (($evolveSteps.Count -ge 2) -and
     ($evolveSteps[-2] -eq 'Diagnosepaket bei Fehlschlag schnueren') -and
     ($evolveSteps[-1] -eq 'Diagnosepaket hochladen')) `
    ("letzte Schritte: " + (($evolveSteps | Select-Object -Last 3) -join ' | '))
Test-That 'sie stehen damit hinter Commit, Bundle und Uebergabepaket' `
    (($evolve.IndexOf('- name: Diagnosepaket bei Fehlschlag schnueren') -gt $evolve.IndexOf('- name: Commit-Objekt und Bundle erzeugen')) -and
     ($evolve.IndexOf('- name: Diagnosepaket bei Fehlschlag schnueren') -gt $evolve.IndexOf('- name: Uebergabepaket hochladen')))
Test-That 'Diagnosepaket-Upload laeuft nur bei einem Fehlschlag' `
    ($evolve -match '(?s)- name: Diagnosepaket hochladen.*?if: failure\(\)')
Test-That 'Diagnosepaket traegt einen anderen Artefaktnamen als das Uebergabepaket' `
    (($evolve -match 'name: claude-primary-forensics') -and ($evolve -match 'name: claude-primary-handoff'))
# Der eigentliche Riegel: publish LAEDT genau ein Artefakt, und zwar das Uebergabepaket.
# Bewusst auf den download-artifact-Schritt eingegrenzt - publish laedt eines herunter und
# legt danach sein eigenes Evidence-Artefakt ab; ein blosses Zaehlen aller Namen zaehlte
# dieses mit und ginge am Punkt vorbei.
$downloads = [regex]::Matches($publish, '(?s)uses: actions/download-artifact@v4.*?name: (claude-primary-[\w-]+)')
Test-That 'publish laedt genau ein Artefakt herunter' `
    ((([regex]::Matches($publish, 'uses: actions/download-artifact@')).Count -eq 1) -and ($downloads.Count -eq 1))
Test-That 'und zwar ausschliesslich das Uebergabepaket' `
    (($downloads.Count -eq 1) -and ($downloads[0].Groups[1].Value -eq 'claude-primary-handoff')) `
    $(if ($downloads.Count -eq 1) { $downloads[0].Groups[1].Value })
Test-That 'Diagnosepaket kollidiert mit keinem Artefaktnamen von publish' `
    (($evolve -match 'name: claude-primary-forensics') -and
     ([regex]::Matches($publish, 'name: claude-primary-forensics')).Count -eq 0)
Test-That 'publish kennt das Diagnosepaket ueberhaupt nicht' ($publish -notmatch 'forensics')
Test-That 'Diagnosepaket weist Bundle und evidence.json zur Laufzeit ab' `
    ($evolve -match 'for verboten in evolution\.bundle evidence\.json')
Test-That 'Diagnosepaket ist als nicht publizierbar ausgewiesen' `
    (($evolve -match 'NICHT PUBLIZIERBAR') -and ($evolve -match 'publishable:false'))

# Reihenfolge: aus einem roten Review kann nichts entstehen, weil Commit und Bundle erst
# danach kommen - und publish haengt ohnehin an `needs: evolve`.
$idxReview  = $workflow.IndexOf('- name: Reviewer-Envelope auswerten')
$idxCommit  = $workflow.IndexOf('- name: Commit-Objekt und Bundle erzeugen')
$idxHandoff = $workflow.IndexOf('name: claude-primary-handoff')
Test-That 'Commit und Bundle entstehen erst nach der Reviewer-Auswertung' `
    (($idxReview -gt 0) -and ($idxCommit -gt $idxReview)) "review=$idxReview commit=$idxCommit"
Test-That 'das Uebergabepaket entsteht erst nach Commit und Bundle' `
    ($idxHandoff -gt $idxCommit) "handoff=$idxHandoff commit=$idxCommit"

Write-Host ''
Write-Host '=== STRUKTURELL: Turn-Zahl ist kein Abbruchgrund mehr (Reviewer) ==='

# Der Befund aus Lauf 32020621278. Die funktionale Gegenprobe steht in
# Test-ClaudePrimaryRunReviewerGate.ps1; hier nur der Riegel gegen einen Rueckfall.
Test-That 'Reviewer bricht nicht mehr bei num_turns ab' `
    ($workflow -notmatch 'TURNS" -ge "\$REVIEWER_MAX_TURNS"\s*\];\s*then\s*\n\s*echo "::error')
Test-That 'die Turn-Zahl des Reviewers ist nur noch ein Hinweis' `
    ($evolve -match '(?s)TURNS" -ge "\$REVIEWER_MAX_TURNS.*?::notice::')
Test-That 'Urteil wird vor der Turn-Diagnose ausgelesen' `
    ($evolve.IndexOf('VERDICT="$(printf') -lt $evolve.IndexOf('$REVIEWER_MAX_TURNS. Rein diagnostisch'))
Test-That '--max-turns bleibt als Schutzparameter gesetzt' `
    ($evolve -match '--max-turns "\$REVIEWER_MAX_TURNS"')
Test-That 'das Reviewer-Budget wurde auf einen realistischen Wert angehoben' `
    ($workflow -match "REVIEWER_MAX_TURNS: '(3[2-9]|40)'")
# Diese Zusicherung war die Scope-Schranke, solange der Builder bewusst unangetastet blieb.
# Inzwischen ist derselbe Defekt auch dort behoben, also sichert sie das Gegenteil: beide
# Gates muessen dieselbe Semantik tragen. Waere sie unveraendert geblieben, haette sie still
# weiter bestanden - der Vergleich steht ja noch da - und dabei das Falsche behauptet.
#
# Auf den Builder-Schritt eingegrenzt: `.*?::notice::` ueber den ganzen evolve-Job findet
# sonst den Hinweis des Reviewers und bestaende auch bei wieder hart abbrechendem Builder.
$builderStep = [regex]::Match($evolve, '(?s)- name: Builder-Envelope auswerten.*?\n      [-#]').Value
Test-That 'auch der Builder wertet die Turn-Zahl nur noch diagnostisch aus' `
    (($builderStep.Length -gt 0) -and ($builderStep -match '(?s)TURNS" -ge "\$BUILDER_MAX_TURNS.*?::notice::'))
Test-That 'im Builder-Schritt steht kein Abbruch mehr an der Turn-Zahl' `
    ($builderStep -notmatch '(?s)TURNS" -ge "\$BUILDER_MAX_TURNS[^\n]*\n\s*echo "::error')
Test-That 'beide Gates lesen ihr Ergebnis vor der Turn-Diagnose' `
    (([regex]::Matches($evolve, '(?s)jq -c ''\.structured_output // empty''.*?::notice::')).Count -eq 2)

Write-Host ''
Write-Host ('=' * 70)
if ($script:Failures.Count -eq 0) {
    Write-Host 'CLAUDE_PRIMARY_SCHEDULE: PASS'
    exit 0
} else {
    Write-Host ("CLAUDE_PRIMARY_SCHEDULE: FAIL ({0})" -f $script:Failures.Count)
    $script:Failures | ForEach-Object { Write-Host "  - $_" }
    exit 1
}
