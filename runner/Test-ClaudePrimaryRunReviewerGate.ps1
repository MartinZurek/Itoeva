# Regressionstest fuer das Reviewer-Gate von ClaudePrimaryRun.
#
# **Anlass: Lauf 32020621278.** Der Reviewer lief mit --max-turns 20 und lieferte ein
# vollstaendiges, schemakonformes Urteil. Die CLI meldete dazu
#
#   is_error=false subtype=success stop_reason=tool_use denials=0 num_turns=24
#
# Das alte Gate brach bei num_turns >= 20 ab - und zwar VOR dem Auslesen von verdict. Das
# Urteil war damit unwiederbringlich verloren; nicht einmal das Log verriet, ob der Reviewer
# zugestimmt oder abgelehnt hatte. Ein vollstaendig gruener Lauf scheiterte an einer Zahl, die
# die CLI selbst oberhalb des Limits meldet, das sie bekommen hat.
#
# In 2.1.232 zaehlt num_turns also nicht dasselbe, was --max-turns begrenzt. Die Zahl ist
# damit kein Nachweis einer abgebrochenen Sitzung - eine wirklich erschoepfte Sitzung meldet
# das im subtype (error_max_turns). Genau diese Trennung haelt dieser Test fest.
#
# Geprueft wird der ECHTE Shell-Block aus dem Workflow, aus dem YAML herausgeschnitten und
# gegen kuenstliche Envelopes ausgefuehrt - keine Kopie, die auseinanderlaufen koennte. Der
# einzige Eingriff ist das Ersetzen des GitHub-Ausdrucks fuer den Basiscommit; er waere ausser
# halb von Actions bloss Text.
#
# Aufruf:  .\runner\Test-ClaudePrimaryRunReviewerGate.ps1
#
# Braucht bash und jq (auf dem ubuntu-Runner beides vorhanden). Fehlt jq, meldet der Test
# ausdruecklich SKIPPED, statt stillschweigend gruen zu sein - ein uebersprungener Test, der
# wie ein bestandener aussieht, waere schlimmer als gar keiner.

[CmdletBinding()]
param(
    [string]$WorkflowPath,
    [string]$JqPath
)

$ErrorActionPreference = 'Stop'
$script:Failures = @()

if (-not $WorkflowPath) { $WorkflowPath = Join-Path $PSScriptRoot '..\.github\workflows\claude-primary-run.yml' }

function Test-That {
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
if (-not (Test-Path -LiteralPath $bash)) { throw "bash nicht gefunden." }

# jq suchen: PATH, ausdrueckliche Angabe, oder ITOEVA_JQ. Nicht installieren - der Test soll
# keine Umgebung veraendern.
if (-not $JqPath) { $JqPath = $env:ITOEVA_JQ }
if (-not $JqPath) {
    $c = Get-Command jq -ErrorAction SilentlyContinue
    if ($c) { $JqPath = $c.Source }
}
if ((-not $JqPath) -or (-not (Test-Path -LiteralPath $JqPath))) {
    Write-Host ''
    Write-Host '=== CLAUDE_PRIMARY_REVIEWER_GATE: SKIPPED ==='
    Write-Host '    jq nicht gefunden. Der gepruefte Block ruft jq auf; ohne jq laesst sich das'
    Write-Host '    Gate nicht ausfuehren. Mit -JqPath oder ITOEVA_JQ einen Pfad angeben.'
    exit 0
}
# PATH ist doppelpunktgetrennt: ein Eintrag der Form "C:/..." wird von bash am Doppelpunkt
# zerlegt und der Rest als eigenes Verzeichnis gelesen - jq bliebe unauffindbar. Fuer PATH
# ist deshalb die POSIX-Schreibweise /c/... noetig (fuer blosse Argumente nicht, siehe die
# uebrigen Suiten).
$jqDirWin = Split-Path -Parent (Resolve-Path -LiteralPath $JqPath).Path
$jqDir = $jqDirWin -replace '\\', '/'
# Kein -replace mit Skriptblock: das kennt Windows PowerShell 5.1 nicht und setzt den
# Ausdruck stattdessen woertlich ein.
if ($jqDir -match '^([A-Za-z]):(.*)$') { $jqDir = '/' + $matches[1].ToLower() + $matches[2] }
Write-Host ("  jq: {0}" -f $JqPath)
Write-Host ("  jq-Verzeichnis fuer PATH: {0}" -f $jqDir)

# ---------------------------------------------------------------------------------------
# Den Auswertungsblock aus dem Workflow herausschneiden.
# ---------------------------------------------------------------------------------------
$workflow = Get-Content -Raw -LiteralPath $WorkflowPath

# Der Block endet an der ersten Zeile, die auf Schritt-Ebene (sechs Leerzeichen) beginnt -
# egal ob dort der naechste Schritt oder ein Kommentar steht. Bewusst BEIDE: eine Grenze nur
# auf "- name:" zoege einen dazwischenliegenden Kommentar mit in den Block, und der verliert
# beim Dedent sein Rautezeichen und wird zum Kommando.
$blockEnd = '\r?\n      [-#]'
$m = [regex]::Match($workflow, "(?s)- name: Reviewer-Envelope auswerten\r?\n        run: \|\r?\n(.*?)$blockEnd")
if (-not $m.Success) { throw 'Auswertungsblock des Reviewers nicht im Workflow gefunden.' }

# YAML entfernt beim Block-Scalar genau die Grundeinrueckung. Zeilen ohne diese Einrueckung
# gehoeren nicht zum Block und duerfen nicht beschnitten werden - blindes Substring(10) hat
# genau hier eine Kommentarzeile in ein Kommando verwandelt.
function Remove-BlockIndent {
    param([string]$Text)
    (($Text -replace "`r`n", "`n") -split "`n" | ForEach-Object {
        if ($_ -match '^ {10}') { $_.Substring(10) }
        elseif ($_.Trim() -eq '') { '' }
        else { $_ }
    }) -join "`n"
}

$TEST_BASE  = '60ee84655522e7e6235d3d84fac85a6214de7957'
$OTHER_SHA  = '0123456789abcdef0123456789abcdef01234567'

$block = Remove-BlockIndent $m.Groups[1].Value
# Der einzige Eingriff: der GitHub-Ausdruck fuer den Basiscommit.
$block = $block -replace [regex]::Escape('${{ steps.freeze.outputs.base }}'), $TEST_BASE

function Invoke-ReviewerGate {
    param([string]$EnvelopeJson, [int]$MaxTurns = 32)
    $dir = Join-Path ([IO.Path]::GetTempPath()) ("itoeva-rg-" + [Guid]::NewGuid().ToString('N').Substring(0, 8))
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    $utf8 = New-Object Text.UTF8Encoding($false)
    [IO.File]::WriteAllText((Join-Path $dir 'reviewer-envelope.json'), $EnvelopeJson, $utf8)
    $scriptFile = Join-Path $dir 'gate.sh'
    # gate.sh bleibt byteweise der Block aus dem Workflow. Alles, was die Umgebung stellen
    # muss (PATH fuer jq, RUNNER_TEMP, das Budget), steht im Wrapper - so wird der Block
    # unter Test nicht angefasst. Ausserdem umgeht der Wrapper die Anfuehrungszeichen-
    # Behandlung, mit der Windows PowerShell 5.1 native Aufrufe zerlegt.
    [IO.File]::WriteAllText($scriptFile, ($block + "`n"), $utf8)
    $outFile = Join-Path $dir 'out.txt'
    $sf = $scriptFile -replace '\\', '/'
    $rt = $dir        -replace '\\', '/'
    $of = $outFile    -replace '\\', '/'
    $wrapper = Join-Path $dir 'wrapper.sh'
    $wrapperText = "export PATH='$jqDir':`"`$PATH`"`n" +
                   "export RUNNER_TEMP='$rt'`n" +
                   "export REVIEWER_MAX_TURNS='$MaxTurns'`n" +
                   "bash '$sf' > '$of' 2>&1`n" +
                   "exit `$?`n"
    [IO.File]::WriteAllText($wrapper, $wrapperText, $utf8)
    & $bash ($wrapper -replace '\\', '/') | Out-Null
    $code = $LASTEXITCODE
    [pscustomobject]@{
        Code    = $code
        Out     = [string](Get-Content -Raw -LiteralPath $outFile -ErrorAction SilentlyContinue)
        Verdict = if (Test-Path (Join-Path $dir 'reviewer-verdict.json')) { [string](Get-Content -Raw -LiteralPath (Join-Path $dir 'reviewer-verdict.json')) } else { $null }
        Dir     = $dir
    }
}

function New-Envelope {
    param(
        [string]$Subtype = 'success',
        [string]$IsError = 'false',
        [int]$Turns = 12,
        [string]$Stop = 'tool_use',
        [string]$Denials = '[]',
        [string]$Structured = $null,
        [string]$Result = $null
    )
    $payload = if ($Structured) { ",`n  `"structured_output`": $Structured" }
               elseif ($Result) { ",`n  `"result`": $Result" }
               else { '' }
    @"
{
  "type": "result",
  "subtype": "$Subtype",
  "is_error": $IsError,
  "duration_ms": 160482,
  "num_turns": $Turns,
  "stop_reason": $(if ($Stop -eq 'null') { 'null' } else { "`"$Stop`"" }),
  "permission_denials": $Denials$payload
}
"@
}

function New-Verdict {
    param([string]$Verdict = 'APPROVE', [string]$Sha = $TEST_BASE, [string]$Risk = 'low',
          [string]$Reasons = '["Aenderung entspricht dem Auftrag.","Keine Produktionsdatei beruehrt."]')
    "{`"verdict`":`"$Verdict`",`"baseSha`":`"$Sha`",`"riskLevel`":`"$Risk`",`"reasons`":$Reasons}"
}

# =========================================================================================
Write-Host ''
Write-Host '=== Der Produktionsfall: Turns ueber Budget, Urteil vollstaendig ==='

# Exakt die Zahlen aus Lauf 32020621278: 24 Turns bei einem Budget von 20.
$r = Invoke-ReviewerGate (New-Envelope -Turns 24 -Structured (New-Verdict 'APPROVE')) -MaxTurns 20
Test-That 'num_turns ueber Budget + gueltiges APPROVE scheitert NICHT daran' ($r.Code -eq 0) "code=$($r.Code)`n$($r.Out)"
Test-That 'das Urteil wird trotz Ueberschreitung ausgelesen' ($r.Out -match 'verdict=APPROVE')
Test-That 'die Ueberschreitung wird als Hinweis gemeldet, nicht als Fehler' `
    (($r.Out -match '::notice::') -and ($r.Out -notmatch '::error::'))
Test-That 'Reviewer: APPROVE wird bestaetigt' ($r.Out -match 'Reviewer: APPROVE')

$r = Invoke-ReviewerGate (New-Envelope -Turns 24 -Structured (New-Verdict 'REJECT' $TEST_BASE 'high')) -MaxTurns 20
Test-That 'num_turns ueber Budget + gueltiges REJECT bleibt REJECT' ($r.Code -ne 0) "code=$($r.Code)"
Test-That 'REJECT wird als Ablehnung gemeldet, nicht als Budgetfehler' `
    (($r.Out -match 'Reviewer lehnt ab') -and ($r.Out -notmatch 'Turn-Budget')) $r.Out
Test-That 'das REJECT-Urteil steht im Log' ($r.Out -match 'verdict=REJECT')

# **Die Reihenfolge ist der Kern des Fixes.** In beiden Faellen oben lag num_turns ueber dem
# Budget - und trotzdem stand verdict im Log. Genau das war vorher unmoeglich.
Test-That 'Urteil wird VOR der Turn-Diagnose ausgewertet' `
    ($r.Out -match 'verdict=REJECT')

Write-Host ''
Write-Host '=== Weiterhin fail-closed ==='

$faelle = @(
    @{ Name = 'fehlende strukturierte Ausgabe'
       Env  = (New-Envelope -Result '"Sieht gut aus, ich wuerde zustimmen."')
       Muster = 'keine parsebare strukturierte Ausgabe' },
    @{ Name = 'gar keine Ausgabe'
       Env  = (New-Envelope)
       Muster = 'keine parsebare strukturierte Ausgabe' },
    @{ Name = 'verweigerte Berechtigung'
       Env  = (New-Envelope -Denials '[{"tool":"Bash","reason":"nicht erlaubt"}]' -Structured (New-Verdict))
       Muster = 'Werkzeug\(e\) verweigert' },
    @{ Name = 'max_tokens - abgeschnittene Ausgabe'
       Env  = (New-Envelope -Stop 'max_tokens' -Structured (New-Verdict))
       Muster = 'unvollstaendiges stop_reason' },
    @{ Name = 'refusal'
       Env  = (New-Envelope -Stop 'refusal' -Structured (New-Verdict))
       Muster = 'unvollstaendiges stop_reason' },
    @{ Name = 'is_error=true'
       Env  = (New-Envelope -IsError 'true' -Structured (New-Verdict))
       Muster = 'is_error=true' },
    # Der belastbare Nachweis einer erschoepften Sitzung - der bleibt scharf.
    @{ Name = 'echte Turn-Erschoepfung (subtype=error_max_turns)'
       Env  = (New-Envelope -Subtype 'error_max_turns' -Turns 40 -Structured (New-Verdict))
       Muster = 'nicht erfolgreich \(subtype=error_max_turns\)' },
    @{ Name = 'abweichender Basiscommit'
       Env  = (New-Envelope -Structured (New-Verdict 'APPROVE' $OTHER_SHA))
       Muster = 'abweichenden Basiscommit' },
    @{ Name = 'Schemaverstoss: reasons leer'
       Env  = (New-Envelope -Structured (New-Verdict 'APPROVE' $TEST_BASE 'low' '[]'))
       Muster = 'verletzt das Schema' },
    @{ Name = 'Schemaverstoss: unbekanntes verdict'
       Env  = (New-Envelope -Structured (New-Verdict 'MAYBE'))
       Muster = 'verletzt das Schema' },
    @{ Name = 'Schemaverstoss: riskLevel fehlt'
       Env  = (New-Envelope -Structured "{`"verdict`":`"APPROVE`",`"baseSha`":`"$TEST_BASE`",`"reasons`":[`"ok`"]}")
       Muster = 'verletzt das Schema' },
    @{ Name = 'Schemaverstoss: reasons falsch typisiert'
       Env  = (New-Envelope -Structured (New-Verdict 'APPROVE' $TEST_BASE 'low' '[1,2]'))
       Muster = 'verletzt das Schema' }
)
foreach ($f in $faelle) {
    $r = Invoke-ReviewerGate $f.Env
    Test-That ("{0}: bricht ab" -f $f.Name) ($r.Code -ne 0) "code=$($r.Code)`n$($r.Out)"
    Test-That ("{0}: nennt den Grund" -f $f.Name) ($r.Out -match $f.Muster) $r.Out.Trim()
}

Write-Host ''
Write-Host '=== Normalfall bleibt normal ==='

$r = Invoke-ReviewerGate (New-Envelope -Turns 12 -Stop 'tool_use' -Structured (New-Verdict 'APPROVE'))
Test-That 'tool_use im Budget + gueltiges Ergebnis wird angenommen' ($r.Code -eq 0) "code=$($r.Code)`n$($r.Out)"
Test-That 'ohne Ueberschreitung gibt es keinen Hinweis' ($r.Out -notmatch '::notice::')
Test-That 'die Begruendungen stehen im Log' ($r.Out -match 'Aenderung entspricht dem Auftrag')

foreach ($stop in @('end_turn', 'stop_sequence', 'null')) {
    $r = Invoke-ReviewerGate (New-Envelope -Stop $stop -Structured (New-Verdict 'APPROVE'))
    Test-That ("stop_reason=$stop wird angenommen") ($r.Code -eq 0) "code=$($r.Code)`n$($r.Out)"
}

Write-Host ''
Write-Host '=== Forensik: das Urteil ueberlebt den Fehlschlag ==='

$r = Invoke-ReviewerGate (New-Envelope -Turns 24 -Structured (New-Verdict 'REJECT' $TEST_BASE 'high')) -MaxTurns 20
Test-That 'bei REJECT liegt das Urteil als Datei vor' ($null -ne $r.Verdict) 'reviewer-verdict.json fehlt'
Test-That 'die Urteilsdatei enthaelt das Verdikt' ($r.Verdict -match 'REJECT')
# Ohne strukturierte Ausgabe muss der Freitext erhalten bleiben - sonst steht in der Forensik
# genau dann nichts, wenn man am dringendsten wissen will, was der Reviewer gesagt hat.
$r = Invoke-ReviewerGate (New-Envelope -Result '"Ich kann das so nicht beurteilen."')
Test-That 'auch ohne strukturierte Ausgabe wird der Freitext abgelegt' `
    ($r.Verdict -match 'Ich kann das so nicht beurteilen') "Inhalt=[$($r.Verdict)]"

Write-Host ''
Write-Host '=== Das Diagnosepaket laesst sich wirklich schnueren ==='

# Ein Schritt, der nur im Fehlerfall laeuft, wird sonst ausgerechnet dann zum ersten Mal
# ausgefuehrt, wenn ohnehin schon etwas schiefgegangen ist. Deshalb hier einmal echt
# ausfuehren - samt Heredoc, jq -n und der Sperre gegen Publish-Inhalte.
$mf = [regex]::Match($workflow, "(?s)- name: Diagnosepaket bei Fehlschlag schnueren\r?\n.*?\r?\n        run: \|\r?\n(.*?)$blockEnd")
Test-That 'Forensik-Block laesst sich aus dem Workflow herausschneiden' $mf.Success

if ($mf.Success) {
    $fblock = Remove-BlockIndent $mf.Groups[1].Value
    # Die GitHub-Ausdruecke waeren ausserhalb von Actions bloss Text.
    $fblock = $fblock -replace [regex]::Escape('${{ steps.freeze.outputs.base }}'), $TEST_BASE
    $fblock = $fblock -replace '\$\{\{[^}]*\}\}', 'testwert'

    function Invoke-Forensics {
        param([switch]$WithSources)
        $dir = Join-Path ([IO.Path]::GetTempPath()) ("itoeva-fx-" + [Guid]::NewGuid().ToString('N').Substring(0, 8))
        New-Item -ItemType Directory -Force -Path $dir | Out-Null
        $utf8 = New-Object Text.UTF8Encoding($false)
        if ($WithSources) {
            [IO.File]::WriteAllText((Join-Path $dir 'summary.txt'), 'Unit-Tests fuer PlayGamePlan ergaenzt', $utf8)
            [IO.File]::WriteAllText((Join-Path $dir 'proposed.diff'), "diff --git a/x b/x`n+neu`n", $utf8)
            [IO.File]::WriteAllText((Join-Path $dir 'changed.txt'), "app-sim/src/test/x.kt`n", $utf8)
            [IO.File]::WriteAllText((Join-Path $dir 'builder-diagnostics.json'), '{"num_turns":22}', $utf8)
            [IO.File]::WriteAllText((Join-Path $dir 'reviewer-diagnostics.json'), '{"num_turns":24}', $utf8)
            [IO.File]::WriteAllText((Join-Path $dir 'reviewer-verdict.json'), '{"verdict":"REJECT"}', $utf8)
        }
        $scriptFile = Join-Path $dir 'forensics.sh'
        [IO.File]::WriteAllText($scriptFile, ($fblock + "`n"), $utf8)
        $outFile = Join-Path $dir 'out.txt'
        $rt = $dir -replace '\\', '/'
        $wrapper = Join-Path $dir 'w.sh'
        [IO.File]::WriteAllText($wrapper,
            ("export PATH='$jqDir':`"`$PATH`"`nexport RUNNER_TEMP='$rt'`n" +
             "bash '$rt/forensics.sh' > '$rt/out.txt' 2>&1`nexit `$?`n"), $utf8)
        & $bash ($wrapper -replace '\\', '/') | Out-Null
        [pscustomobject]@{
            Code  = $LASTEXITCODE
            Out   = [string](Get-Content -Raw -LiteralPath $outFile -ErrorAction SilentlyContinue)
            Files = @(Get-ChildItem (Join-Path $dir 'forensics') -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Name)
            Dir   = $dir
        }
    }

    $f = Invoke-Forensics -WithSources
    Test-That 'Forensik-Block laeuft fehlerfrei' ($f.Code -eq 0) "code=$($f.Code)`n$($f.Out)"
    foreach ($n in @('README-FORENSIK.txt', 'builder-summary.txt', 'proposed.diff', 'changed.txt',
                     'builder-diagnostics.json', 'reviewer-diagnostics.json', 'reviewer-verdict.json',
                     'forensics-metadata.json')) {
        Test-That ("Diagnosepaket enthaelt $n") ($f.Files -contains $n) ("vorhanden: " + ($f.Files -join ', '))
    }
    # Der Kern: aus diesem Paket darf sich nichts herstellen lassen.
    Test-That 'Diagnosepaket enthaelt KEIN Bundle'        (-not ($f.Files -contains 'evolution.bundle'))
    Test-That 'Diagnosepaket enthaelt KEINE evidence.json' (-not ($f.Files -contains 'evidence.json'))
    $meta = Get-Content -Raw (Join-Path $f.Dir 'forensics\forensics-metadata.json') -ErrorAction SilentlyContinue
    Test-That 'Metadaten weisen das Paket als nicht publizierbar aus' ($meta -match '"publishable"\s*:\s*false') $meta
    Test-That 'Metadaten halten fest, dass nichts gepusht wurde'      ($meta -match '"pushed"\s*:\s*false')
    $marker = Get-Content -Raw (Join-Path $f.Dir 'forensics\README-FORENSIK.txt') -ErrorAction SilentlyContinue
    Test-That 'Der Marker sagt es auch im Klartext' ($marker -match 'NICHT PUBLIZIERBAR')
    $sum = Get-Content -Raw (Join-Path $f.Dir 'forensics\builder-summary.txt') -ErrorAction SilentlyContinue
    Test-That 'das Builder-Summary ueberlebt' ($sum -match 'PlayGamePlan')

    # Der Fehlschlag kann auch frueh passieren - dann fehlt fast alles, und der Block darf
    # trotzdem nicht selbst umfallen.
    $f2 = Invoke-Forensics
    Test-That 'Forensik-Block laeuft auch ohne jede Quelle durch' ($f2.Code -eq 0) "code=$($f2.Code)`n$($f2.Out)"
    Test-That 'auch dann entsteht ein vollstaendiges Paket' ($f2.Files.Count -ge 7) ($f2.Files -join ', ')
}

Write-Host ''
Write-Host ('=' * 70)
if ($script:Failures.Count -eq 0) {
    Write-Host 'CLAUDE_PRIMARY_REVIEWER_GATE: PASS'
    exit 0
} else {
    Write-Host ("CLAUDE_PRIMARY_REVIEWER_GATE: FAIL ({0})" -f $script:Failures.Count)
    $script:Failures | ForEach-Object { Write-Host "  - $_" }
    exit 1
}
