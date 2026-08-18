#!/bin/sh
# Baut das signierte JWT, mit dem sich ein Dienstkonto-Schluessel selbst ein Zugriffstoken
# beim Google-OAuth-Endpunkt abholt (Server-zu-Server-Ablauf ohne Identitaetsuebernahme).
#
# Aufruf:  gdrive-jwt.sh <schluessel.json> <scope>
# Ausgabe: das JWT auf stdout, sonst nichts.
#
# Exitcodes, bewusst unterschieden:
#   0  JWT erzeugt
#   2  Schluesseldatei fehlt oder ist kein gueltiges JSON
#   3  im Schluessel fehlt client_email oder private_key
#   5  kein brauchbarer Python-Aufruf gefunden
#  64  falscher Aufruf
#
# Warum ein eigenes Skript und nicht ein paar Zeilen im YAML: so laesst sich die Signatur
# lokal gegen einen Wegwerf-Schluessel nachrechnen (runner/Test-GdriveJwt.ps1), statt sie
# erst im naechsten Auslieferungslauf zu erproben - dort meldet der OAuth-Endpunkt bei einem
# falsch gebauten JWT nur eine nichtssagende 400.
#
# Warum ueberhaupt selbst signieren, statt google-github-actions/auth zu nehmen: dessen
# token_format: access_token laesst sich das Token von iamcredentials.googleapis.com
# ausstellen. Das verlangt eine zusaetzlich aktivierte API UND die Rolle "Service Account
# Token Creator" des Kontos auf sich selbst - beides traegt zum Drive-Zugriff nichts bei und
# war die Ursache der Fehlschlaege in den Laeufen 32154962129 und 32156370377. Ein Schluessel
# kann sich sein Token laut Googles eigener Dokumentation direkt selbst holen.
#
# Warum Python statt jq fuer das JSON: der Ubuntu-Runner hat beides, der Windows-
# Entwicklungsrechner nur Python - und ein Skript, das sich lokal nicht ausfuehren laesst,
# laesst sich auch nicht lokal pruefen.

set -eu

KEYFILE="${1:-}"
SCOPE="${2:-}"

if [ -z "$KEYFILE" ] || [ -z "$SCOPE" ]; then
  echo "Aufruf: $0 <schluessel.json> <scope>" >&2
  exit 64
fi
[ -f "$KEYFILE" ] || { echo "Schluesseldatei fehlt: $KEYFILE" >&2; exit 2; }

# Nicht blosses `command -v`: unter Windows liegt eine python3-Attrappe im PATH, die nur auf
# den Microsoft Store verweist und bei jedem Aufruf scheitert. Geprueft wird deshalb, ob der
# Aufruf tatsaechlich laeuft.
PY=""
for kandidat in python3 python; do
  if command -v "$kandidat" >/dev/null 2>&1 && "$kandidat" -c 'import json' >/dev/null 2>&1; then
    PY="$kandidat"; break
  fi
done
[ -n "$PY" ] || { echo "Weder python3 noch python nutzbar." >&2; exit 5; }

feld() {
  "$PY" -c 'import json,sys
try:
    d = json.load(open(sys.argv[1], encoding="utf-8"))
except Exception:
    sys.exit(9)
sys.stdout.write(str(d.get(sys.argv[2], "")))' "$KEYFILE" "$1"
}

ISS="$(feld client_email)" || { echo "Schluesseldatei ist kein gueltiges JSON." >&2; exit 2; }
[ -n "$ISS" ] || { echo "Im Schluessel fehlt client_email." >&2; exit 3; }

# Der private Schluessel geht ueber eine Datei mit enger Maske an openssl und nicht ueber die
# Kommandozeile: Argumente sind in der Prozessliste mitlesbar.
umask 077
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT INT TERM
feld private_key > "$TMP/key.pem"
[ -s "$TMP/key.pem" ] || { echo "Im Schluessel fehlt private_key." >&2; exit 3; }

# base64url ohne Auffuellzeichen und ohne Zeilenumbrueche - beides machte das JWT ungueltig.
# `openssl base64 -A` haelt die Ausgabe auf einer Zeile.
b64url() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }

NUN="$(date -u +%s)"
KOPF="$(printf '%s' '{"alg":"RS256","typ":"JWT"}' | b64url)"
# Eine Stunde ist Googles Hoechstmass; kuerzer braeuchte es nicht, der Upload danach dauert
# Sekunden.
RUMPF="$(printf '{"iss":"%s","scope":"%s","aud":"https://oauth2.googleapis.com/token","iat":%s,"exp":%s}' \
           "$ISS" "$SCOPE" "$NUN" "$((NUN + 3600))" | b64url)"
SIG="$(printf '%s.%s' "$KOPF" "$RUMPF" | openssl dgst -sha256 -sign "$TMP/key.pem" -binary | b64url)"

printf '%s.%s.%s\n' "$KOPF" "$RUMPF" "$SIG"
