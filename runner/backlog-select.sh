#!/bin/sh
# Waehlt den obersten offenen Eintrag aus evolutions/BACKLOG.md.
#
# Aufruf:  backlog-select.sh <backlog-datei> <ausgabeverzeichnis>
#
# Bei Erfolg entstehen im Ausgabeverzeichnis drei Dateien: id, title und task. Der Aufruf
# gibt selbst nichts auf stdout aus - der Aufgabentext geht spaeter woertlich in einen
# Prompt, und er soll nicht ueber eine Kommandosubstitution laufen muessen.
#
# Exitcodes, bewusst unterschieden, weil der Workflow sie verschieden behandelt:
#   0  Eintrag gewaehlt
#   2  kein offener Eintrag (Backlog leer oder alles erledigt) -> Aufrufer waehlt Fallback
#   3  Formatfehler, doppelte ID oder leerer Aufgabentext      -> fail-closed, roter Lauf
#   4  Backlogdatei fehlt                                       -> fail-closed
#  64  falscher Aufruf
#
# Warum ein eigenes Skript und nicht ein paar Zeilen im YAML: so laesst sich das Parsen
# lokal gegen echte Fehlerfaelle testen (runner/Test-ClaudePrimaryRunSchedule.ps1), statt
# es erst nachts im Zeitplan-Lauf zu erfahren.

set -eu

BACKLOG="${1:-}"
OUT="${2:-}"

if [ -z "$BACKLOG" ] || [ -z "$OUT" ]; then
  echo "Aufruf: $0 <backlog-datei> <ausgabeverzeichnis>" >&2
  exit 64
fi
if [ ! -f "$BACKLOG" ]; then
  echo "Backlogdatei fehlt: $BACKLOG" >&2
  exit 4
fi

mkdir -p "$OUT"
rm -f "$OUT/id" "$OUT/title" "$OUT/task"

awk -v out="$OUT" '
  function fail(msg) {
    printf("%s\n", msg) > "/dev/stderr"
    rc = 3
    exit 3
  }

  # Kandidat fuer einen Aufgabenkopf. Bewusst nachsichtig erkannt (auch "##[open] ..."
  # ohne Leerzeichen), aber danach streng geprueft: ein Tippfehler im Status soll auffallen
  # und nicht stillschweigend als Prosa durchgehen.
  /^##[ \t]*\[/ {
    if ($0 !~ /^## \[(open|done)\] ITO-[0-9][0-9][0-9][0-9] - .+$/) {
      fail("Formatfehler in Zeile " NR ": " $0)
    }
    s      = substr($0, 4)              # ab "["
    ci     = index(s, "]")
    status = substr(s, 2, ci - 2)
    rest   = substr(s, ci + 2)          # ab der ID
    id     = substr(rest, 1, 8)         # ITO-0000
    title  = substr(rest, 12)           # hinter "ITO-0000 - "

    if (seen[id]++) fail("Doppelte ID " id " in Zeile " NR)

    if (status == "open" && selected == 0) {
      selected   = 1
      sel_id     = id
      sel_title  = title
      collecting = 1
      next
    }
    collecting = 0
    next
  }

  # Jede andere Ueberschrift der zweiten Ebene beendet den Aufgabentext - sonst zoege ein
  # nachfolgender Prosaabschnitt mit in den Prompt.
  /^## / { collecting = 0 }

  collecting { body = body $0 "\n" }

  END {
    if (rc == 3) exit 3
    if (selected == 0) {
      printf("Kein offener Eintrag im Backlog.\n") > "/dev/stderr"
      exit 2
    }
    gsub(/^[ \t\n]+/, "", body)
    gsub(/[ \t\n]+$/, "", body)
    if (body == "") {
      printf("Aufgabentext von %s ist leer.\n", sel_id) > "/dev/stderr"
      exit 3
    }
    print sel_id    > (out "/id")
    print sel_title > (out "/title")
    printf("%s\n", body) > (out "/task")
  }
' "$BACKLOG"
