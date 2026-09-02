# Lern- und Feedback-Overlay fuer den Tagesablauf

Diese Datei verbindet Nutzerfeedback mit den spaeteren, automatisierten Tagesablauf-Evolutionen.
Der Dauerauftrag steht getrennt in `DAILY_LIFE_TASK.md`; dadurch kann dieses Overlay lernen, ohne
seine eigenen Sicherheitsgrenzen umzuschreiben. Jede Aenderung bleibt ein normaler Pull Request
mit Tests, zweiter KI-Pruefung und menschlichem Merge.

Ein einmaliger, bereits vollstaendig formulierter Auftrag kann weiterhin direkt ueber das Feld
`task` von `workflow_dispatch` gestartet werden. Rueckmeldungen, die mehrere spaetere Laeufe
beeinflussen sollen, gehoeren dagegen in den Feedback-Eingang unten. Eine KI in einem Chat kann
sie dort in einem normalen PR eintragen; der GitHub-Lauf liest keine privaten Chats automatisch.

## Aktueller Produktfokus

Der Spielmodus soll Freude am Zuschauen erzeugen: Die Avatare leben erkennbar in ihrer Welt,
gestalten einen plausiblen Tagesablauf und unterscheiden sich dabei. Persoenliche Reminder duerfen
dieses Leben sichtbar und sanft beeinflussen, ohne zu Aufgaben, Strafe oder einer zweiten
Reminder-Logik zu werden.

## Feedback-Eingang

Neue Rueckmeldungen werden oben einsortiert. Format:

    ### [open] FB-JJJJ-MM-TT-NN - Kurztitel
    Beobachtung, gewuenschte Spielerwirkung und - falls vorhanden - reproduzierbare Situation.

Nur eine Evolution, die den Eintrag vollstaendig behandelt, darf `[open]` auf `[done]` setzen.
Unklare oder geschuetzte Wuensche bleiben offen und werden im Journal als `OPEN DECISION`
bezeichnet.

### [open] FB-2026-09-02-01 - Das Avatarleben muss den Kern des Spielerlebnisses tragen

Die automatisierten Jobs sollen sich nicht in isolierten Requisiten, Einzelanimationen oder
anderen leicht zaehlbaren Kleinigkeiten verlieren. Beim Zuschauen soll erkennbar sein, wie ein
Avatar seinen Tag gestaltet, wie sich die sechs Wesen unterscheiden und wie persoenliche
Reminder den weiteren Ablauf sanft beeinflussen. Kleine Codeaenderungen sind ausdruecklich
willkommen, wenn ihre Wirkung in diesem Erlebnis gross und nachvollziehbar ist.

## Gelernte Heuristiken

Dieser Abschnitt darf nur um eine kurze Regel erweitert werden, wenn ein Test, ein reproduzierbarer
Fehler oder mindestens zwei unabhaengige Journaleintraege sie belegen. Eine Heuristik veraendert
keine Produktentscheidung und keine Sicherheitsgrenze.

- Noch keine automatisiert abgeleitete Heuristik. Die erste Evolution sammelt Evidenz statt eine
  Vermutung zur Regel zu erheben.

## Lernjournal

Pro Evolution genau ein neuer Eintrag direkt unter dieser Einleitung. Ein Eintrag nennt Datum,
betroffenes Feedback, beobachtetes Problem, Spielerwirkung, Evidenz, Ergebnis und den naechsten
sinnvollen Hebel. Vermutungen werden als **Hypothese**, nicht als Tatsache, markiert. Das Journal
ist eine Uebergabe, keine zweite Commit-Liste.

Noch kein automatisierter Tagesablauf-Lauf abgeschlossen.

## Offene Meta-Ideen

Hier darf ein Lauf Verbesserungen an Auswahl, Messbarkeit oder Automationsqualitaet vorschlagen,
aber nicht selbst in `.github/`, `runner/`, `DAILY_LIFE_TASK.md`, Berechtigungen, Secrets,
Publikations- oder Review-Gates umsetzen. Eine Meta-Idee braucht Problembeleg, erwarteten Nutzen,
Risiko und eine pruefbare Erfolgsmessung; Umsetzung nur in einem getrennten, ausdruecklich
menschlich freigegebenen Prozess-PR.

Noch keine offene Meta-Idee.
