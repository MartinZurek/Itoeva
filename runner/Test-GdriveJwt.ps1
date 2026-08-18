# Tests fuer runner/gdrive-jwt.sh - das signierte JWT, mit dem sich der Dienstkonto-
# Schluessel sein Drive-Zugriffstoken selbst abholt.
#
# Alles hier laeuft OHNE Netz und ohne echten Schluessel: erzeugt wird ein Wegwerf-
# Schluesselpaar, und die Signatur wird lokal gegen den zugehoerigen oeffentlichen Teil
# nachgerechnet. Genau darum liegt die Logik in einem eigenen Skript statt im YAML - ein
# falsch gebautes JWT faellt sonst erst im naechsten Auslieferungslauf auf, und dort mit
# einer nichtssagenden 400 vom OAuth-Endpunkt.
#
# Aufruf:  .\runner\Test-GdriveJwt.ps1

[CmdletBinding()]
param([string]$ScriptPath, [string]$WorkflowPath)

$ErrorActionPreference = 'Stop'
$script:Failures = @()

if (-not $ScriptPath)   { $ScriptPath   = Join-Path $PSScriptRoot 'gdrive-jwt.sh' }
if (-not $WorkflowPath) { $WorkflowPath = Join-Path $PSScriptRoot '..\.github\workflows\deliver-apk.yml' }

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
if (-not (Test-Path -LiteralPath $bash)) { throw 'bash nicht gefunden - dieser Test braucht es.' }

$dir = Join-Path ([IO.Path]::GetTempPath()) ('itoeva-jwt-' + [Guid]::NewGuid().ToString('N').Substring(0, 8))
New-Item -ItemType Directory -Force -Path $dir | Out-Null
$d = ($dir -replace '\\', '/')
$s = ($ScriptPath -replace '\\', '/')

# Die Fehlerausgabe wird innerhalb von bash umgeleitet: PowerShell 5.1 verwandelt
# umgeleitete stderr-Ausgaben nativer Programme sonst in Fehlerobjekte.
function Invoke-Bash([string]$Command) {
    $out = & $bash -c "$Command 2>'$d/err.txt'"
    [pscustomobject]@{
        Code = $LASTEXITCODE
        Out  = ($out -join "`n").Trim()
        Err  = [string](Get-Content -Raw -LiteralPath (Join-Path $dir 'err.txt') -ErrorAction SilentlyContinue)
    }
}

Write-Host ''
Write-Host '=== Wegwerf-Schluesselpaar und Schluesseldatei erzeugen ==='

Invoke-Bash "openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out '$d/test.pem' >/dev/null" | Out-Null
Invoke-Bash "openssl rsa -in '$d/test.pem' -pubout -out '$d/test.pub' >/dev/null" | Out-Null
Test-That 'Testschluessel erzeugt' (Test-Path (Join-Path $dir 'test.pem'))

# Die Schluesseldatei entsteht in PowerShell und nicht ueber bash: der PEM-Text muss als
# JSON-String mit \n escaped werden - genau so, wie Google den Schluessel ausliefert -, und
# ConvertTo-Json erledigt das zuverlaessiger als eine durch zwei Shells gereichte Zeichenkette.
$pem = [IO.File]::ReadAllText((Join-Path $dir 'test.pem'))
$sa = [ordered]@{
    type         = 'service_account'
    client_email = 'pruef@beispiel.iam.gserviceaccount.com'
    private_key  = $pem
} | ConvertTo-Json
[IO.File]::WriteAllText((Join-Path $dir 'sa.json'), $sa)
Test-That 'Schluesseldatei ist gueltiges JSON' `
    ((& { try { [void](Get-Content -Raw (Join-Path $dir 'sa.json') | ConvertFrom-Json); $true } catch { $false } }))

Write-Host ''
Write-Host '=== FUNKTIONAL: JWT wird gebaut und ist echt signiert ==='

$scope = 'https://www.googleapis.com/auth/drive'
$r = Invoke-Bash "sh '$s' '$d/sa.json' '$scope'"
Test-That 'Exitcode 0' ($r.Code -eq 0) $r.Err
$jwt = $r.Out
Test-That 'genau drei durch Punkt getrennte Teile' (($jwt -split '\.').Count -eq 3)
Test-That 'ohne Auffuellzeichen und ohne Leerraum' (($jwt -notmatch '=') -and ($jwt -notmatch '\s'))

# base64url zurueckuebersetzen: '-_' auf '+/' drehen und die fehlende Auffuellung ergaenzen.
function ConvertFrom-B64Url([string]$Text) {
    $t = $Text.Replace('-', '+').Replace('_', '/')
    switch ($t.Length % 4) { 2 { $t += '==' } 3 { $t += '=' } }
    [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($t))
}

$teile = $jwt -split '\.'
$kopf  = ConvertFrom-B64Url $teile[0]
$rumpf = ConvertFrom-B64Url $teile[1]

Test-That 'Kopf nennt RS256 und JWT' (($kopf -match '"alg"\s*:\s*"RS256"') -and ($kopf -match '"typ"\s*:\s*"JWT"'))
Test-That 'iss ist die client_email aus dem Schluessel' ($rumpf -match 'pruef@beispiel\.iam\.gserviceaccount\.com')
Test-That 'scope wird durchgereicht' ($rumpf -match [regex]::Escape($scope))
Test-That 'aud ist der OAuth-Endpunkt' ($rumpf -match 'https://oauth2\.googleapis\.com/token')

$iat = [int64]([regex]::Match($rumpf, '"iat"\s*:\s*(\d+)').Groups[1].Value)
$exp = [int64]([regex]::Match($rumpf, '"exp"\s*:\s*(\d+)').Groups[1].Value)
Test-That 'Laufzeit ist eine Stunde' (($exp - $iat) -eq 3600) "exp-iat=$($exp - $iat)"
$jetzt = [int64][Math]::Floor((Get-Date).ToUniversalTime().Subtract([datetime]'1970-01-01').TotalSeconds)
Test-That 'iat liegt in der Gegenwart' ([Math]::Abs($jetzt - $iat) -lt 300) "Abweichung=$([Math]::Abs($jetzt - $iat))s"

# Der eigentliche Kern: haelt die Signatur der Pruefung mit dem oeffentlichen Teil stand?
[IO.File]::WriteAllText((Join-Path $dir 'signiert.txt'), ($teile[0] + '.' + $teile[1]))
$sig = $teile[2].Replace('-', '+').Replace('_', '/')
switch ($sig.Length % 4) { 2 { $sig += '==' } 3 { $sig += '=' } }
[IO.File]::WriteAllBytes((Join-Path $dir 'sig.bin'), [Convert]::FromBase64String($sig))
$v = Invoke-Bash "openssl dgst -sha256 -verify '$d/test.pub' -signature '$d/sig.bin' '$d/signiert.txt'"
Test-That 'Signatur haelt der Pruefung mit dem oeffentlichen Schluessel stand' `
    ($v.Out -match 'Verified OK') "$($v.Out) $($v.Err)"

# Gegenprobe: eine verfaelschte Nutzlast darf NICHT durchgehen. Ohne diesen Fall wuerde der
# Test oben auch dann gruen, wenn openssl aus einem anderen Grund 'Verified OK' meldete.
[IO.File]::WriteAllText((Join-Path $dir 'manipuliert.txt'), ($teile[0] + '.' + $teile[1] + 'x'))
$g = Invoke-Bash "openssl dgst -sha256 -verify '$d/test.pub' -signature '$d/sig.bin' '$d/manipuliert.txt'"
Test-That 'verfaelschte Nutzlast faellt durch' ($g.Out -notmatch 'Verified OK')

Write-Host ''
Write-Host '=== FUNKTIONAL: Fehlerfaelle brechen fail-closed ab ==='

Test-That 'ohne Argumente: Exitcode 64' ((Invoke-Bash "sh '$s'").Code -eq 64)
Test-That 'fehlende Datei: Exitcode 2' ((Invoke-Bash "sh '$s' '$d/gibtsnicht.json' '$scope'").Code -eq 2)

[IO.File]::WriteAllText((Join-Path $dir 'kaputt.json'), 'kein json')
Test-That 'kaputtes JSON: Exitcode 2' ((Invoke-Bash "sh '$s' '$d/kaputt.json' '$scope'").Code -eq 2)

[IO.File]::WriteAllText((Join-Path $dir 'ohne-mail.json'), '{"type":"service_account","private_key":"x"}')
$r = Invoke-Bash "sh '$s' '$d/ohne-mail.json' '$scope'"
Test-That 'fehlende client_email: Exitcode 3' ($r.Code -eq 3) $r.Err

[IO.File]::WriteAllText((Join-Path $dir 'ohne-key.json'), '{"type":"service_account","client_email":"a@b.c"}')
$r = Invoke-Bash "sh '$s' '$d/ohne-key.json' '$scope'"
Test-That 'fehlender private_key: Exitcode 3' ($r.Code -eq 3) $r.Err

Write-Host ''
Write-Host '=== STRUKTURELL: der Workflow benutzt genau dieses Skript ==='

$wf = Get-Content -Raw -LiteralPath $WorkflowPath
Test-That 'deliver-apk.yml ruft gdrive-jwt.sh auf'  ($wf -match 'runner/gdrive-jwt\.sh')
Test-That 'kein google-github-actions/auth mehr'    ($wf -notmatch 'uses:\s*google-github-actions/auth')
Test-That 'Token wird maskiert, bevor er Ausgabewert wird' `
    ($wf -match '(?s)::add-mask::\$TOKEN.*?token=\$TOKEN')
Test-That 'Schluessel wird per trap wieder entfernt' ($wf -match 'trap .rm -rf')
Test-That 'Drive-Scope unveraendert'                 ($wf -match 'https://www\.googleapis\.com/auth/drive')
Test-That 'die Ablieferung liest den neuen Ausgabewert' `
    ($wf -match 'TOKEN: \$\{\{ steps\.token\.outputs\.token \}\}')

Remove-Item -Recurse -Force $dir -ErrorAction SilentlyContinue

Write-Host ''
Write-Host ('=' * 70)
if ($script:Failures.Count -eq 0) {
    Write-Host 'ITOEVA_GDRIVE_JWT: PASS'
    exit 0
} else {
    Write-Host ("ITOEVA_GDRIVE_JWT: FAIL ({0})" -f $script:Failures.Count)
    $script:Failures | ForEach-Object { Write-Host "  - $_" }
    exit 1
}
