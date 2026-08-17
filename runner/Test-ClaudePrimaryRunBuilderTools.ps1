# Regressionstest fuer die Werkzeuggrenzen des ClaudePrimaryRun-Builders.
#
# Anlass: Lauf 31979290655 scheiterte, weil Bash in der Builder-Allowlist stand, gleichzeitig
# --safe-mode aktiv war und die Aufgabe das Ausfuehren des Builds verlangte. Der Sandkasten
# wies zwoelf Bash-Kommandos ab; die Sitzung verbrauchte 36 von 40 Turns mit
# Umgehungsversuchen. Der vorherige Builder-Smoke-Test hatte das nicht aufgedeckt, weil dort
# als einziger Bash-Befehl `git rev-parse HEAD` lief - ein lesender Befehl, den --safe-mode
# durchlaesst.
#
# Dieser Test haelt beide Haelften der Konsequenz fest:
#
#   STATISCH (immer, offline, ohne Kontingentverbrauch)
#     Der Builder besitzt keine Bash-Berechtigung, behaelt aber Edit und Write, und der
#     Prompt verbietet Builds, Tests und Shell-/Git-Kommandos ausdruecklich.
#
#   LIVE (nur mit -Live)
#     Eine echte Claude-Sitzung mit exakt derselben Flag-Kombination erzeugt trotzdem eine
#     kleine Code-/Teststandsaenderung - ohne eine einzige verweigerte Berechtigung. Genau
#     diese Haelfte fehlte beim Smoke-Test und ist der Grund, warum der Fehler erst im
#     Produktionslauf auffiel. Sie verbraucht Abo-Kontingent und braucht die installierte
#     CLI, laeuft deshalb nicht bei jedem Aufruf mit.
#
# Aufruf:  .\runner\Test-ClaudePrimaryRunBuilderTools.ps1
#          .\runner\Test-ClaudePrimaryRunBuilderTools.ps1 -Live

[CmdletBinding()]
param(
    [switch]$Live,
    [string]$WorkflowPath,
    [string]$ExpectedCliVersion = '2.1.232'
)

$ErrorActionPreference = 'Stop'
$script:Failures = @()

# Nicht als Parametervorgabe: $PSScriptRoot ist beim Auswerten des param-Blocks unter
# Windows PowerShell 5.1 noch leer.
if (-not $WorkflowPath) {
    $WorkflowPath = Join-Path $PSScriptRoot '..\.github\workflows\claude-primary-run.yml'
}

function Test-That {
    param([string]$Name, [bool]$Condition, [string]$Detail = '')
    if ($Condition) {
        Write-Host ("  PASS  {0}" -f $Name)
    } else {
        $script:Failures += $Name
        Write-Host ("  FAIL  {0}{1}" -f $Name, $(if ($Detail) { " - $Detail" } else { '' }))
    }
}

$workflow = Get-Content -Raw -LiteralPath $WorkflowPath

# ---------------------------------------------------------------------------------------
# Die beiden Modellaufrufe aus dem Workflow herausschneiden. Ab der FD-Zuweisung, weil die
# Env-Zuweisung dem Programmnamen vorangeht; bis zur Umleitung in den jeweiligen Envelope.
# ---------------------------------------------------------------------------------------
function Get-Invocation {
    param([string]$EnvelopeFile)
    # Wichtig: (?:(?!claude -p)[\s\S])*? statt (?s).*? - sonst beginnt der Treffer bei der
    # ERSTEN FD-Zuweisung und verschluckt auf dem Weg zum zweiten Envelope den kompletten
    # Builder-Aufruf samt Zwischenschritten. Der Ausschluss haelt jeden Treffer bei genau
    # einem Aufruf.
    $pattern = 'CLAUDE_CODE_OAUTH_TOKEN_FILE_DESCRIPTOR=3 claude -p(?:(?!claude -p)[\s\S])*?' +
               [regex]::Escape("> `"`$RUNNER_TEMP/$EnvelopeFile`"")
    $m = [regex]::Matches($workflow, $pattern)
    if ($m.Count -ne 1) {
        throw "Aufruf fuer ${EnvelopeFile}: $($m.Count) Treffer, erwartet genau 1."
    }
    return $m[0].Value
}

Write-Host ''
Write-Host '=== Statisch: Builder besitzt keine Bash-Berechtigung ==='

$builder  = Get-Invocation 'builder-envelope.json'
$reviewer = Get-Invocation 'reviewer-envelope.json'

Test-That 'Builder-Werkzeugliste ist exakt Read,Glob,Grep,Edit,Write' `
    ($builder -match '--tools\s+"Read,Glob,Grep,Edit,Write"')

Test-That 'Das Wort Bash kommt im Builder-Aufruf nicht vor' `
    ($builder -notmatch 'Bash')

Test-That 'Builder behaelt Schreibfaehigkeit (Edit und Write)' `
    (($builder -match '\bEdit\b') -and ($builder -match '\bWrite\b'))

Test-That 'Builder laeuft weiterhin mit --safe-mode' `
    ($builder -match '--safe-mode')

Test-That 'Builder laeuft weiterhin mit --permission-mode acceptEdits' `
    ($builder -match '--permission-mode acceptEdits')

Write-Host ''
Write-Host '=== Statisch: Der Prompt sagt es dem Modell auch ==='

foreach ($rule in @(
    @{ Name = 'Prompt verbietet Builds';            Pattern = 'Starte keine Builds' },
    @{ Name = 'Prompt verbietet Tests';             Pattern = 'Starte keine Tests' },
    @{ Name = 'Prompt verbietet Shell-/Git-Befehle'; Pattern = 'Versuche keine Shell- oder Git-Kommandos' },
    @{ Name = 'Prompt nennt den Repositoryzustand als Arbeitsgrundlage'; Pattern = 'anhand des unten angegebenen Repositoryzustands' },
    @{ Name = 'Prompt benennt den Workflow als Pruefinstanz'; Pattern = 'Die Verifikation uebernimmt anschliessend der Workflow' }
)) {
    Test-That $rule.Name ($workflow -match $rule.Pattern)
}

Test-That 'Repositoryzustand wird deterministisch vom Workflow erzeugt' `
    ($workflow -match 'repo-facts\.txt')

Write-Host ''
Write-Host '=== Statisch: Reviewer bleibt read-only ==='

Test-That 'Reviewer-Werkzeugliste ist exakt Read,Glob,Grep' `
    ($reviewer -match '--tools\s+"Read,Glob,Grep"')
Test-That 'Reviewer hat weder Edit noch Write noch Bash' `
    ($reviewer -notmatch '\b(Edit|Write|Bash)\b')
Test-That 'Reviewer laeuft im plan-Modus' `
    ($reviewer -match '--permission-mode plan')

Write-Host ''
Write-Host '=== Statisch: Envelope-Gates bleiben scharf ==='

Test-That 'Jede verweigerte Berechtigung bricht ab' `
    ($workflow -match '\[ "\$DENIALS" = "0" \]')
Test-That 'Turn-Erschoepfung bricht ab' `
    ($workflow -match 'TURNS" -ge "\$BUILDER_MAX_TURNS')
# tool_use MUSS in der Allowlist stehen: mit --json-schema endet auch eine vollstaendig
# erfolgreiche Sitzung so (siehe Live-Teil unten). Ohne diesen Wert wiese das Gate jede
# Builder-Sitzung ab. Fehlt er wieder, ist das ein Regressionsfehler.
Test-That 'stop_reason wird gegen eine Allowlist geprueft, die tool_use einschliesst' `
    (([regex]::Matches($workflow, 'end_turn\|stop_sequence\|tool_use\|null\|""')).Count -eq 2)
Test-That 'Tests laufen weiterhin nach dem Builder' `
    (($workflow.IndexOf('builder-envelope.json') -lt $workflow.IndexOf('./gradlew verify --stacktrace')) -and
     ($workflow -match 'connectedDebugAndroidTest'))

# ---------------------------------------------------------------------------------------
# LIVE: derselbe Flag-Satz, echte Sitzung, echte Dateiaenderung.
# ---------------------------------------------------------------------------------------
if ($Live) {
    Write-Host ''
    Write-Host '=== Live: erzeugt der Builder ohne Bash trotzdem eine Aenderung? ==='

    $claude = (Get-Command claude -ErrorAction SilentlyContinue)
    if (-not $claude) { throw 'claude nicht im PATH - Live-Teil nicht ausfuehrbar.' }
    $version = (& claude --version) -join ' '
    if ($version -notmatch [regex]::Escape($ExpectedCliVersion)) {
        throw "CLI-Version passt nicht: '$version', erwartet $ExpectedCliVersion."
    }
    Write-Host "  CLI: $version"

    $work = Join-Path ([IO.Path]::GetTempPath()) ("itoeva-buildertools-" + [Guid]::NewGuid().ToString('N').Substring(0, 8))
    $srcDir  = Join-Path $work 'src\main\java\com\example'
    $testDir = Join-Path $work 'src\test\java\com\example'
    New-Item -ItemType Directory -Force -Path $srcDir, $testDir | Out-Null

    # Ein winziges, reines Objekt - dieselbe Gattung Aufgabe wie im echten Lauf, ohne Build.
    @'
package com.example

/** Rein rechnerische Hilfe ohne Framework-Abhaengigkeit. */
object Geometry {
    const val SIZE = 13
    const val CENTER = (SIZE - 1) / 2.0

    /** Zellen innerhalb des einbeschriebenen Kreises. */
    fun isInside(x: Int, y: Int): Boolean {
        val dx = x - CENTER
        val dy = y - CENTER
        return dx * dx + dy * dy <= (SIZE / 2.0) * (SIZE / 2.0)
    }
}
'@ | Set-Content -Encoding utf8 -LiteralPath (Join-Path $srcDir 'Geometry.kt')

    $mcp = Join-Path $work 'empty-mcp.json'
    '{"mcpServers":{}}' | Set-Content -Encoding utf8 -LiteralPath $mcp

    $schema = '{"type":"object","additionalProperties":false,"required":["status","summary","changedFiles"],"properties":{"status":{"enum":["DONE","BLOCKED"]},"summary":{"type":"string","minLength":10,"maxLength":300},"changedFiles":{"type":"array","items":{"type":"string"},"minItems":1}}}'

    # Windows PowerShell 5.1 entfernt beim Aufruf nativer Programme die doppelten
    # Anfuehrungszeichen aus Argumenten; die CLI bekaeme sonst {type:object,...} und lehnt
    # das als ungueltiges JSON ab. Im Workflow tritt das nicht auf, dort uebergibt Bash.
    $schemaArg = $schema -replace '"', '\"'

    $prompt = @'
Du bist der Builder in einem unbeaufsichtigten CI-Lauf. Niemand kann dir antworten - stelle
keine Rueckfragen und warte auf keine Bestaetigung.

Aufgabe: Lege unter src/test/java/com/example/GeometryTest.kt einen JUnit-4-Unit-Test fuer
das Objekt Geometry aus src/main/java/com/example/Geometry.kt an. Pruefe mindestens, dass die
Rastermitte innerhalb liegt und die Ecke (0,0) ausserhalb.

Wie du arbeitest. Du hast genau vier Werkzeuggruppen: Read, Glob und Grep zum Lesen, Edit und
Write zum Aendern. Eine Shell hast du NICHT, und das ist Absicht:
- Starte keine Builds. Kein gradlew, kein Gradle, kein Maven.
- Starte keine Tests. Auch nicht einzelne.
- Versuche keine Shell- oder Git-Kommandos. Es gibt kein Bash-Werkzeug; jeder Versuch, eines
  zu benutzen, wird abgewiesen und laesst den Lauf fail-closed abbrechen.
- Die Verifikation uebernimmt anschliessend der Workflow, nicht du.

Gib danach ausschliesslich das verlangte JSON zurueck: status DONE wenn die Datei angelegt
ist, sonst BLOCKED; summary eine Zeile; changedFiles die angelegten Pfade.
'@

    $promptFile   = Join-Path $work 'prompt.txt'
    $envelopeFile = Join-Path $work 'envelope.json'
    $prompt | Set-Content -Encoding ascii -LiteralPath $promptFile

    Push-Location $work
    try {
        # Exakt der Flag-Satz des Workflows, nur ohne die FD-Zufuehrung: lokal authentifiziert
        # die CLI ueber den vorhandenen Login, und geprueft wird hier die Werkzeuggrenze.
        Get-Content -Raw -LiteralPath $promptFile |
            & claude -p `
                --model claude-sonnet-5 `
                --permission-mode acceptEdits `
                --tools "Read,Glob,Grep,Edit,Write" `
                --safe-mode `
                --disable-slash-commands `
                --strict-mcp-config `
                --mcp-config $mcp `
                --no-session-persistence `
                --max-turns 20 `
                --output-format json `
                --json-schema $schemaArg |
            Set-Content -Encoding utf8 -LiteralPath $envelopeFile
    } finally {
        Pop-Location
    }

    Test-That 'Live: Envelope wurde erzeugt' (Test-Path -LiteralPath $envelopeFile)
    $env = Get-Content -Raw -LiteralPath $envelopeFile | ConvertFrom-Json

    $denials = @($env.permission_denials).Count
    if ($null -eq $env.permission_denials) { $denials = 0 }
    Write-Host ("  Envelope: subtype={0} is_error={1} num_turns={2} stop_reason={3} denials={4}" -f `
        $env.subtype, $env.is_error, $env.num_turns, $env.stop_reason, $denials)

    Test-That 'Live: keine einzige verweigerte Berechtigung' ($denials -eq 0) "denials=$denials"
    Test-That 'Live: Sitzung meldet Erfolg' (($env.is_error -eq $false) -and ($env.subtype -eq 'success'))
    # Gemessen mit 2.1.232: eine erfolgreiche Sitzung endet mit tool_use, weil die
    # strukturierte Ausgabe von --json-schema ueber einen erzwungenen Tool-Call kommt.
    # Genau dieselbe Allowlist wie im Workflow - laeuft die CLI-Semantik auseinander,
    # faellt es hier auf, bevor ein Produktionslauf daran scheitert.
    Test-That 'Live: stop_reason liegt in der Workflow-Allowlist' `
        (($null -eq $env.stop_reason) -or ($env.stop_reason -in @('end_turn', 'stop_sequence', 'tool_use'))) `
        "stop_reason=$($env.stop_reason)"
    Test-That 'Live: Turn-Budget nicht erschoepft' ($env.num_turns -lt 20) "num_turns=$($env.num_turns)"

    # Der Beweis, auf den es ankommt: ohne Shell ist trotzdem eine Datei entstanden.
    $created = Join-Path $testDir 'GeometryTest.kt'
    Test-That 'Live: Write hat die Testdatei ohne Shell angelegt' (Test-Path -LiteralPath $created)
    if (Test-Path -LiteralPath $created) {
        $body = Get-Content -Raw -LiteralPath $created
        Test-That 'Live: Datei enthaelt einen echten Test' ($body -match '@Test')
        Test-That 'Live: Datei bezieht sich auf Geometry' ($body -match 'Geometry')
        Write-Host ''
        Write-Host '  --- erzeugte Datei ---'
        ($body -split "`n" | Select-Object -First 25) | ForEach-Object { Write-Host "    $_" }
    }

    Write-Host ''
    Write-Host "  Arbeitsverzeichnis: $work"
} else {
    Write-Host ''
    Write-Host '=== Live uebersprungen (mit -Live einschalten; verbraucht Abo-Kontingent) ==='
}

Write-Host ''
Write-Host ('=' * 70)
if ($script:Failures.Count -eq 0) {
    Write-Host 'CLAUDE_PRIMARY_BUILDER_TOOLS: PASS'
    exit 0
} else {
    Write-Host ("CLAUDE_PRIMARY_BUILDER_TOOLS: FAIL ({0})" -f $script:Failures.Count)
    $script:Failures | ForEach-Object { Write-Host "  - $_" }
    exit 1
}
