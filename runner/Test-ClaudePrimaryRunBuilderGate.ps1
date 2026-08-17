# Regressionstest fuer das Builder-Gate von ClaudePrimaryRun.
#
# **Anlass.** Derselbe Defekt, der im Reviewer-Gate in Lauf 32020621278 tatsaechlich
# zugeschlagen hat, steckte unbemerkt auch im Builder:
#
#   1. `num_turns >= BUILDER_MAX_TURNS` brach hart ab, obwohl die CLI 2.1.232 diese Zahl
#      nachweislich oberhalb des Limits meldet, das sie selbst bekommen hat (24 bei einem
#      --max-turns von 20) - und zwar bei subtype=success und vollstaendiger Ausgabe. Hier
#      fiel es nur deshalb nie auf, weil das Budget mit 40 grosszuegiger ist.
#
#   2. `if (.structured_output // empty) then A else B end` liefert in jq NICHTS, sobald
#      structured_output fehlt: `empty` sind null Ergebnisse, damit faellt das ganze if weg
#      samt else-Zweig. Der .result-Fallback war also tot, und ein fehlendes
#      structured_output meldete irrefuehrend "Builder meldet status=" statt der Ursache.
#
# Der Abbruch lag ausserdem VOR dem Auslesen der Ausgabe. Ein an num_turns gescheiterter
# Builder hinterliess deshalb weder summary.txt noch sonst eine Spur seiner Arbeit - genau
# die Luecke, die das Diagnosepaket schliessen soll.
#
# Geprueft wird der ECHTE Shell-Block aus dem Workflow, aus dem YAML herausgeschnitten und
# gegen kuenstliche Envelopes ausgefuehrt - keine Kopie, die auseinanderlaufen koennte.
#
# Aufruf:  .\runner\Test-ClaudePrimaryRunBuilderGate.ps1
#
# Braucht bash und jq (auf dem ubuntu-Runner beides vorhanden). Fehlt jq, meldet der Test
# ausdruecklich SKIPPED statt stillschweigend gruen zu sein.

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

if (-not $JqPath) { $JqPath = $env:ITOEVA_JQ }
if (-not $JqPath) {
    $c = Get-Command jq -ErrorAction SilentlyContinue
    if ($c) { $JqPath = $c.Source }
}
if ((-not $JqPath) -or (-not (Test-Path -LiteralPath $JqPath))) {
    Write-Host ''
    Write-Host '=== CLAUDE_PRIMARY_BUILDER_GATE: SKIPPED ==='
    Write-Host '    jq nicht gefunden. Der gepruefte Block ruft jq auf; ohne jq laesst sich das'
    Write-Host '    Gate nicht ausfuehren. Mit -JqPath oder ITOEVA_JQ einen Pfad angeben.'
    exit 0
}
# PATH ist doppelpunktgetrennt: ein Eintrag der Form "C:/..." wird von bash am Doppelpunkt
# zerlegt. Fuer PATH ist deshalb die POSIX-Schreibweise /c/... noetig.
$jqDir = (Split-Path -Parent (Resolve-Path -LiteralPath $JqPath).Path) -replace '\\', '/'
if ($jqDir -match '^([A-Za-z]):(.*)$') { $jqDir = '/' + $matches[1].ToLower() + $matches[2] }
Write-Host ("  jq: {0}" -f $JqPath)

# ---------------------------------------------------------------------------------------
# Den Auswertungsblock aus dem Workflow herausschneiden.
# ---------------------------------------------------------------------------------------
$workflow = Get-Content -Raw -LiteralPath $WorkflowPath

# Grenze an der ersten Zeile auf Schritt-Ebene, egal ob Schritt oder Kommentar: eine Grenze
# nur auf "- name:" zoege einen dazwischenliegenden Kommentar mit in den Block, und der
# verliert beim Dedent sein Rautezeichen und wird zum Kommando.
$blockEnd = '\r?\n      [-#]'
$m = [regex]::Match($workflow, "(?s)- name: Builder-Envelope auswerten\r?\n        run: \|\r?\n(.*?)$blockEnd")
if (-not $m.Success) { throw 'Auswertungsblock des Builders nicht im Workflow gefunden.' }

function Remove-BlockIndent {
    param([string]$Text)
    (($Text -replace "`r`n", "`n") -split "`n" | ForEach-Object {
        if ($_ -match '^ {10}') { $_.Substring(10) }
        elseif ($_.Trim() -eq '') { '' }
        else { $_ }
    }) -join "`n"
}

$TEST_BASE = '579d802f98baa8f544d0f17b107d469d2c37ab5f'
$OTHER_SHA = '0123456789abcdef0123456789abcdef01234567'

$block = Remove-BlockIndent $m.Groups[1].Value
# Der einzige Eingriff: der GitHub-Ausdruck fuer den Basiscommit waere ausserhalb von
# Actions bloss Text.
$block = $block -replace [regex]::Escape('${{ steps.freeze.outputs.base }}'), $TEST_BASE

function Invoke-BuilderGate {
    param([string]$EnvelopeJson, [int]$MaxTurns = 40)
    $dir = Join-Path ([IO.Path]::GetTempPath()) ("itoeva-bg-" + [Guid]::NewGuid().ToString('N').Substring(0, 8))
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    $utf8 = New-Object Text.UTF8Encoding($false)
    [IO.File]::WriteAllText((Join-Path $dir 'builder-envelope.json'), $EnvelopeJson, $utf8)
    $scriptFile = Join-Path $dir 'gate.sh'
    # gate.sh bleibt byteweise der Block aus dem Workflow; alles Umgebungsabhaengige steht im
    # Wrapper. Der umgeht zugleich die Anfuehrungszeichen-Behandlung, mit der Windows
    # PowerShell 5.1 native Aufrufe zerlegt.
    [IO.File]::WriteAllText($scriptFile, ($block + "`n"), $utf8)
    $rt = $dir -replace '\\', '/'
    $wrapper = Join-Path $dir 'wrapper.sh'
    [IO.File]::WriteAllText($wrapper,
        ("export PATH='$jqDir':`"`$PATH`"`nexport RUNNER_TEMP='$rt'`n" +
         "export BUILDER_MAX_TURNS='$MaxTurns'`n" +
         "bash '$rt/gate.sh' > '$rt/out.txt' 2>&1`nexit `$?`n"), $utf8)
    & $bash ($wrapper -replace '\\', '/') | Out-Null
    $code = $LASTEXITCODE
    [pscustomobject]@{
        Code    = $code
        Out     = [string](Get-Content -Raw -LiteralPath (Join-Path $dir 'out.txt') -ErrorAction SilentlyContinue)
        Summary = if (Test-Path (Join-Path $dir 'summary.txt'))         { [string](Get-Content -Raw -LiteralPath (Join-Path $dir 'summary.txt')) }         else { $null }
        Result  = if (Test-Path (Join-Path $dir 'builder-result.json')) { [string](Get-Content -Raw -LiteralPath (Join-Path $dir 'builder-result.json')) } else { $null }
        Dir     = $dir
    }
}

function New-Envelope {
    param(
        [string]$Subtype = 'success',
        [string]$IsError = 'false',
        [int]$Turns = 22,
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
  "duration_ms": 170484,
  "num_turns": $Turns,
  "stop_reason": $(if ($Stop -eq 'null') { 'null' } else { "`"$Stop`"" }),
  "permission_denials": $Denials$payload
}
"@
}

function New-Result {
    param([string]$Status = 'DONE', [string]$Sha = $TEST_BASE,
          [string]$Summary = 'Unit-Tests fuer PlayGamePlan ergaenzt',
          [string]$Changed = '["app-sim/src/test/java/com/notime/glyphsim/matrix/PlayGamePlanTest.kt"]',
          [string]$Extra = '')
    "{`"status`":`"$Status`",`"summary`":`"$Summary`",`"changedFiles`":$Changed,`"headSha`":`"$Sha`"$Extra}"
}

# =========================================================================================
Write-Host ''
Write-Host '=== Der uebertragene Produktionsfall: Turns ueber Budget, Ergebnis vollstaendig ==='

# Dieselbe Konstellation, an der der Reviewer in Lauf 32020621278 zerbrach - hier auf den
# Builder uebertragen: mehr Turns gemeldet als das Budget hergibt, trotzdem subtype=success
# und ein vollstaendiges, schemakonformes Ergebnis.
$r = Invoke-BuilderGate (New-Envelope -Turns 24 -Structured (New-Result)) -MaxTurns 20
Test-That 'num_turns ueber Budget + gueltiges DONE scheitert NICHT daran' ($r.Code -eq 0) "code=$($r.Code)`n$($r.Out)"
Test-That 'das Ergebnis wird trotz Ueberschreitung ausgelesen' ($r.Out -match 'status=DONE')
Test-That 'die Ueberschreitung wird als Hinweis gemeldet, nicht als Fehler' `
    (($r.Out -match '::notice::') -and ($r.Out -notmatch '::error::'))
Test-That 'Builder-Envelope: PASS wird bestaetigt' ($r.Out -match 'Builder-Envelope: PASS')
Test-That 'summary.txt entsteht' ($r.Summary -match 'PlayGamePlan')

# BLOCKED bleibt BLOCKED - auch ueber Budget, und mit dem echten Grund im Log.
$r = Invoke-BuilderGate (New-Envelope -Turns 24 -Structured (New-Result 'BLOCKED' $TEST_BASE 'Aufgabe ohne Shell nicht loesbar' '["x.kt"]' ',"notes":"gradlew nicht aufrufbar"')) -MaxTurns 20
Test-That 'num_turns ueber Budget + gueltiges BLOCKED bleibt BLOCKED' ($r.Code -ne 0) "code=$($r.Code)"
Test-That 'BLOCKED wird als Status gemeldet, nicht als Budgetfehler' `
    (($r.Out -match 'Builder meldet status=BLOCKED') -and ($r.Out -notmatch 'Turn-Budget')) $r.Out
Test-That 'die notes des Builders stehen im Log' ($r.Out -match 'gradlew nicht aufrufbar')
Test-That 'bei BLOCKED entsteht KEIN summary.txt' ($null -eq $r.Summary)
Test-That 'das Builder-Ergebnis liegt trotzdem fuer die Forensik vor' ($r.Result -match 'BLOCKED')

Write-Host ''
Write-Host '=== Weiterhin fail-closed ==='

$faelle = @(
    # Der Kern des zweiten Defekts: ohne structured_output darf niemals ein DONE entstehen.
    @{ Name = 'fehlende strukturierte Ausgabe (.result ist KEIN Fallback)'
       Env  = (New-Envelope -Result '"Ich habe die Datei angelegt, status DONE."')
       Muster = 'keine parsebare strukturierte Ausgabe' },
    @{ Name = 'gar keine Ausgabe'
       Env  = (New-Envelope)
       Muster = 'keine parsebare strukturierte Ausgabe' },
    # Selbst ein .result, das woertlich ein gueltiges Ergebnis enthaelt, darf nicht zaehlen.
    @{ Name = '.result enthaelt gueltiges JSON - zaehlt trotzdem nicht'
       Env  = (New-Envelope -Result ('"' + ((New-Result) -replace '"', '\"') + '"'))
       Muster = 'keine parsebare strukturierte Ausgabe' },
    @{ Name = 'verweigerte Berechtigung'
       Env  = (New-Envelope -Denials '[{"tool":"Bash","reason":"nicht erlaubt"}]' -Structured (New-Result))
       Muster = 'Werkzeug\(e\) verweigert' },
    @{ Name = 'max_tokens - abgeschnittene Ausgabe'
       Env  = (New-Envelope -Stop 'max_tokens' -Structured (New-Result))
       Muster = 'unvollstaendiges stop_reason' },
    @{ Name = 'refusal'
       Env  = (New-Envelope -Stop 'refusal' -Structured (New-Result))
       Muster = 'unvollstaendiges stop_reason' },
    @{ Name = 'is_error=true'
       Env  = (New-Envelope -IsError 'true' -Structured (New-Result))
       Muster = 'is_error=true' },
    # Der belastbare Nachweis einer erschoepften Sitzung - der bleibt scharf.
    @{ Name = 'echte Turn-Erschoepfung (subtype=error_max_turns)'
       Env  = (New-Envelope -Subtype 'error_max_turns' -Turns 60 -Structured (New-Result))
       Muster = 'nicht erfolgreich \(subtype=error_max_turns\)' },
    @{ Name = 'abweichender Basiscommit'
       Env  = (New-Envelope -Structured (New-Result 'DONE' $OTHER_SHA))
       Muster = 'abweichenden Basiscommit' },
    @{ Name = 'Schemaverstoss: unbekannter status'
       Env  = (New-Envelope -Structured (New-Result 'FERTIG'))
       Muster = 'verletzt das Schema' },
    @{ Name = 'Schemaverstoss: changedFiles leer'
       Env  = (New-Envelope -Structured (New-Result 'DONE' $TEST_BASE 'Irgendeine Zusammenfassung' '[]'))
       Muster = 'verletzt das Schema' },
    @{ Name = 'Schemaverstoss: changedFiles falsch typisiert'
       Env  = (New-Envelope -Structured (New-Result 'DONE' $TEST_BASE 'Irgendeine Zusammenfassung' '[1,2]'))
       Muster = 'verletzt das Schema' },
    @{ Name = 'Schemaverstoss: summary zu kurz'
       Env  = (New-Envelope -Structured (New-Result 'DONE' $TEST_BASE 'kurz'))
       Muster = 'verletzt das Schema' },
    @{ Name = 'Schemaverstoss: headSha ist keine 40-Hex'
       Env  = (New-Envelope -Structured (New-Result 'DONE' 'nicht-hex'))
       Muster = 'verletzt das Schema' },
    @{ Name = 'Schemaverstoss: headSha fehlt'
       Env  = (New-Envelope -Structured '{"status":"DONE","summary":"Irgendeine Zusammenfassung","changedFiles":["x.kt"]}')
       Muster = 'verletzt das Schema' }
)
foreach ($f in $faelle) {
    $r = Invoke-BuilderGate $f.Env
    Test-That ("{0}: bricht ab" -f $f.Name) ($r.Code -ne 0) "code=$($r.Code)`n$($r.Out)"
    Test-That ("{0}: nennt den Grund" -f $f.Name) ($r.Out -match $f.Muster) $r.Out.Trim()
    Test-That ("{0}: kein summary.txt" -f $f.Name) ($null -eq $r.Summary)
}

Write-Host ''
Write-Host '=== Lauf 31979290655 wird unveraendert gefangen ==='

# Der Lauf, der das Bash-Verbot ausgeloest hat: zwoelf abgewiesene Kommandos bei 36 von 40
# Turns. Die Turn-Zahl lag UNTER der Grenze und hat nie gegriffen - gefangen haben ihn die
# permission_denials. Genau das muss so bleiben, sonst waere der Fix eine Aufweichung.
$denials = '[{"tool":"Bash","reason":"python3"},{"tool":"Bash","reason":"awk"},{"tool":"Bash","reason":"./gradlew"}]'
$r = Invoke-BuilderGate (New-Envelope -Turns 36 -Denials $denials -Structured (New-Result)) -MaxTurns 40
Test-That 'verweigerte Berechtigungen fangen den Lauf weiterhin' ($r.Code -ne 0) "code=$($r.Code)"
Test-That 'und zwar mit den Verweigerungen als Grund, nicht mit den Turns' `
    (($r.Out -match 'Werkzeug\(e\) verweigert') -and ($r.Out -notmatch 'Turn-Budget')) $r.Out

Write-Host ''
Write-Host '=== Normalfall bleibt normal ==='

$r = Invoke-BuilderGate (New-Envelope -Turns 22 -Stop 'tool_use' -Structured (New-Result))
Test-That 'tool_use im Budget + gueltiges Ergebnis wird angenommen' ($r.Code -eq 0) "code=$($r.Code)`n$($r.Out)"
Test-That 'ohne Ueberschreitung gibt es keinen Hinweis' ($r.Out -notmatch '::notice::')
Test-That 'die gemeldeten Dateien stehen im Log' ($r.Out -match 'gemeldet: app-sim/')
Test-That 'summary.txt traegt genau die Zusammenfassung' ($r.Summary -ceq 'Unit-Tests fuer PlayGamePlan ergaenzt')

foreach ($stop in @('end_turn', 'stop_sequence', 'null')) {
    $r = Invoke-BuilderGate (New-Envelope -Stop $stop -Structured (New-Result))
    Test-That ("stop_reason=$stop wird angenommen") ($r.Code -eq 0) "code=$($r.Code)`n$($r.Out)"
}

Write-Host ''
Write-Host '=== Forensik: das Builder-Ergebnis ueberlebt den Fehlschlag ==='

# summary.txt entsteht erst ganz am Ende des Gates. Faellt der Builder durch, gaebe es ohne
# builder-result.json keinerlei Spur dessen, was er gemeldet hat - einschliesslich der
# Fehlermeldung, die er laut Auftrag im Summary hinterlegen soll.
$r = Invoke-BuilderGate (New-Envelope -Structured (New-Result 'BLOCKED'))
Test-That 'bei BLOCKED liegt das Ergebnis als Datei vor' ($null -ne $r.Result)
Test-That 'die Ergebnisdatei enthaelt den Status' ($r.Result -match 'BLOCKED')
$r = Invoke-BuilderGate (New-Envelope -Result '"Ich kam ohne Shell nicht weiter."')
Test-That 'auch ohne strukturierte Ausgabe wird der Freitext abgelegt' `
    ($r.Result -match 'Ich kam ohne Shell nicht weiter') "Inhalt=[$($r.Result)]"

Write-Host ''
Write-Host ('=' * 70)
if ($script:Failures.Count -eq 0) {
    Write-Host 'CLAUDE_PRIMARY_BUILDER_GATE: PASS'
    exit 0
} else {
    Write-Host ("CLAUDE_PRIMARY_BUILDER_GATE: FAIL ({0})" -f $script:Failures.Count)
    $script:Failures | ForEach-Object { Write-Host "  - $_" }
    exit 1
}
