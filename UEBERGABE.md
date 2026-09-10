# Uebergabe: Stand am 10. September 2026

Diese Datei ist fuer den, der als Naechstes weitermacht - Mensch oder Agent, ausdruecklich auch
ein anderes Modell als das, das sie geschrieben hat. Sie ersetzt nicht
[`CLOUD_CODE_BRIEFING.md`](CLOUD_CODE_BRIEFING.md) (Produktvision) und nicht
[`evolutions/BACKLOG.md`](evolutions/BACKLOG.md) (die Arbeitsliste), sondern sagt, **wo genau der
Faden liegt** und **welche Fallen auf dem Weg dahin schon zugeschnappt sind**.

## 1. Der offene Faden: Living Agent System

Die sechs Charakter-Prompts und Manifest-Eintraege ITO-0017 bis ITO-0022 sind gemergt. Die
eigentliche Audioerzeugung bleibt bewusst manuell: ein Wesen pro Workflow-Lauf, danach hoeren und
erst dann einen Asset-PR oeffnen.

Der neue ausdruecklich freigegebene Hauptauftrag steht in
[`LIVING_AGENT.md`](LIVING_AGENT.md). **NT-063, NT-067 und NT-064 (Schnitte 2a, 2b und 3)
sind umgesetzt.** Der reine Kotlin-Kern steht weiterhin in genau fuenf Dateien unter
`app-sim/src/main/java/com/notime/glyphsim/living/`. Zielwahl, Ressourcenplan, Neuplanung,
begrenzte Episoden, gelernte Zielpraeferenzen, Beziehungen und symbolische Verstaendigung sind
deterministisch belegt.

Die Persistenz liegt bewusst ausserhalb des Kerns unter `app-sim/.../data/LivingAgentStore.kt`.
Sie schreibt einen atomaren Version-2-Snapshot je `profileId` in SharedPreferences. Agent,
Weltressourcen, Erinnerungen, Beziehungen und gelernter Geschmack bleiben getrennt pro Profil.
Version 1 wird beim Lesen angehoben; unbekannte Zukunftsversionen werden abgelehnt. Der
Wiedereinstieg bekommt die Simulationsminute explizit, behaelt das Ziel und verwirft den alten
Plan, damit aktuelle Orte und Nachbarn neu geprueft werden. Room und `:core` blieben unangetastet.

Naechster Schnitt ist **NT-065**: den Kern an die vorhandene `PlayRoutine`-/Weltpipeline
anbinden und den harten Notfall-Sonderfall in `DockScreen` dabei ersetzen, nicht verdoppeln.
Danach folgen Stream-Snapshot (NT-066) und der begrenzte Streaming-PoC (NT-058), jeweils als
eigener PR.

Drei Dinge, die man beim Weiterbauen wissen muss:

- Der Kern hat **kein Android, keine Uhr und keinen Zufall**. Zeit wird als `day` und
  `minuteOfDay` hereingereicht. Wer hier `System.currentTimeMillis` oder `Random` einfuehrt,
  macht den Mehrtageslauf unpruefbar - und im Zeitraffer rechnet er gegen die falsche Uhr.
- **`Requirement` ist ein benanntes Ding und kein `Boolean`.** Daran haengt die ganze
  Erklaerbarkeit. Aus demselben Grund enthaelt kein Ereignis freien Text.
- **`ActionOutcome` ist der einzige Wirkungsweg.** Auch Episode, Geschmack, Beziehung und
  Symbolbedeutung werden in `Action.applyTo` gemeinsam gerechnet; soziale Handlungen bekommen
  keine Nebenpipeline.
- **Persistenz behaelt das Ziel, aber nie den Plan.** Geoeffnete Orte und anwesende Wesen
  kommen beim Laden aus dem aktuellen Runtime-Kontext; damit kann ein alter Snapshot keine
  ungueltige Voraussetzung umgehen.
- **`LivingSite` hat vier Werte, `PlayScene.Place` hat sechzehn.** Die Abbildung gehoert in den
  Runtime-Adapter (NT-065), nicht in die Domaene. `DockScreen.kt` fuer die reinen Schnitte nicht
  anfassen und niemals als Ganzes lesen.

## 2. Harte Regeln fuer die Musik

Vom Auftraggeber gesetzt, hier woertlich, weil sie sich nicht aus dem Code ergeben:

- Im GitHub-Repository existiert bereits das Repository Secret `HF_TOKEN`. **Gib den Wert
  niemals aus.** Schreibe ihn nie in Code, Dateien, Logs, Commits oder PR-Beschreibungen. Nur der
  NAME des Secrets taucht je in einem Workflow auf. Er dient allein dem Herunterladen der
  Modellgewichte.
- Erstelle keinen neuen Hugging-Face-Token, aendere und loesche keine bestehenden Secrets.
- **Keine kostenpflichtige Stability API.** Erzeugt wird mit den offenen Gewichten.
- **Keine zweite Musikpipeline**, und die bestehende Architektur nicht unnoetig veraendern.
- `tools/music/audio_polish.py` ist das Freigabe-Gate und wird nicht angefasst. Es misst die
  **dekodierte** Datei, nicht die vor dem Encoder - der gemessene Vorbis-Ueberschwinger betraegt
  rund 0,6 dB, und genau deshalb steht die Politur bei -1,0 dBFS.

## 3. Wie hier gearbeitet wird

- **Nie direkt auf `main`.** Feature-Branch, Pull Request, mergen bei gruener CI.
- Eine Agenten-Sitzung darf seit dem 3. September 2026 bei gruener CI selbst mergen, wenn kein
  Konflikt und kein offener Review-Kommentar vorliegt (siehe `CLOUD_CODE_BRIEFING.md`). In der
  Praxis hat der Auftraggeber trotzdem meist selbst "mergen" gesagt.
- **Vor jedem Merge die Check-Runs am Kopf-SHA direkt nachsehen.** Zweimal an einem Tag hat ein
  Ereignis "alles fertig" gemeldet, waehrend noch Jobs liefen.
- **Kommentare und KDoc auf Deutsch, ohne Umlaute** (der Baum ist durchgehend so gehalten).
- Jede Aenderung braucht einen Test, wenn sich etwas pruefen laesst, das man am Geraet nicht
  belegen koennte. Das ist der wiederkehrende Massstab in diesem Repository: nicht "gibt es einen
  Test", sondern "waere dieser Fall ohne ihn ueberhaupt nachweisbar".

### Die Offline-Strecke

```
bash tools/reaction-preview/tests.sh          # derzeit 277 Tests, ~2 s
python3 -m unittest discover --start-directory tools/music   # 15 Tests
```

Die erste laedt Kotlin-Compiler und JUnit selbst herunter und braucht **kein** Android SDK und
kein Gradle. Sie uebersetzt aber nur eine **ausdrueckliche Liste** von Dateien: Wer eine neue
Quell- oder Testdatei anlegt, muss sie in `tests.sh` an **zwei** Stellen eintragen - `SRCS`/
`TEST_SRCS` (uebersetzen) und `TEST_CLASSES` (ausfuehren). Fehlt der zweite Eintrag, ist der Test
gruen, ohne je gelaufen zu sein.

Was die Offline-Strecke **nicht** kann: alles mit Compose (`DockScreen.kt`) und alles, was einen
echten `ContextWrapper` braucht. Das prueft erst `gradlew verify` in CI.

## 4. Fallen, die hier schon zugeschnappt sind

- **Zwei Datenbanken.** `:app` und `:app-sim` haben je eine eigene Room-Datenbank, teilen sich
  aber die Entities aus `:core`. Room vergleicht beim Oeffnen das GANZE Schema. Jede Aenderung an
  einer `:core`-Entity braucht deshalb eine Migration in **beiden** Modulen - sonst stuerzt die
  andere App beim naechsten Start ab. Siehe das KDoc von `MIGRATION_18_19`.
- **`DockScreen.kt` hat 3.984 Zeilen.** Drei Builder-Laeufe sind daran gescheitert, dass das
  Zugbudget beim Lesen aufgebraucht war, bevor die Arbeit begann. Gezielt greppen, nie ganz
  oeffnen. Dasselbe gilt fuer `PlayScene.kt` (3.672 Zeilen).
- **Kotlin-Namenskollision im Testpaket.** In `com.notime.glyphsim.ui` gibt es zwei
  In-Memory-`ContextWrapper` mit verschiedenen Namen (`InMemoryPrefsContext` in `PlayLoreTest`,
  `CharacterThemePrefsContext` in `PlayCharacterThemeLogTest`). Das ist Absicht: Zwei
  Top-Level-Klassen gleichen Namens kollidieren im selben Paket **unabhaengig von `private`**, und
  der Bau scheitert dann mit achtzehn Fehlerzeilen, groesstenteils in einer fremden Datei.
- **`JAVA_HOME` ist auf dem Rechner des Auftraggebers nicht gesetzt** - siehe `CLAUDE.md`. Und
  scheitert ein Build an einem Pfad unter `G:\Meine Ablage\...`: `gradlew.bat --stop` (siehe
  `UMZUG.md`).

## 5. Die automatische Pipeline (`claude-primary-run.yml`)

Sie nimmt sich den obersten `[open]`-Eintrag aus `evolutions/BACKLOG.md` und arbeitet ihn ab. Was
man ueber sie wissen muss, bevor man ihr etwas hinlegt:

- **Der Builder hat kein Bash.** Nur Read, Glob, Grep, Edit, Write. Ein Backlog-Eintrag, der das
  Ausfuehren eines Befehls verlangt, ist unerfuellbar.
- **`runner/runner.config.json` sperrt Pfade und Endungen** - unter anderem `.github/`, `runner/`,
  `BACKLOG.md`, `build.gradle.kts` und alle Skript-Endungen (`.sh`, `.ps1`, `.bat`, ...). Ein
  Eintrag, der eine gesperrte Datei verlangt, laesst den Lauf **nach** getaner Arbeit scheitern,
  und bei rotem `evolve` wird nichts gepusht - die Arbeit ist dann weg.
- **`BUILDER_MAX_TURNS` steht auf 80 und sollte dort bleiben.** Laeuft ein Eintrag dagegen, ist er
  zu gross geschnitten - der Builder-Prompt verlangt ausdruecklich "genau eine Evolution, so wenige
  Dateien wie moeglich". ITO-0016 aufzuteilen brachte 81 auf 32 Turns.
- **Diagnose laeuft ueber Check-Run-Annotationen.** Job-Logs und Artefakte sind aus einer
  Agenten-Sitzung heraus nicht erreichbar (die Log-API liefert nur das Ende, der Artefakt-Host
  wird vom Egress-Proxy abgelehnt). Deshalb schreibt der Workflow Builder- und
  Reviewer-Diagnose sowie den Grund eines gefallenen `gradlew verify` als `::notice` bzw.
  `::error` heraus. Wer daran etwas aendert: `set -o pipefail` nicht entfernen, sonst ist der
  Verify-Schritt dauerhaft gruen.
- Von sechs geplanten Cron-Slots feuern in der Praxis zwei bis drei, mit ein bis zwei Stunden
  Verspaetung.

## 6. Was offen beim Auftraggeber liegt

- **Branch `claude-evolution/33849938800-1` (Commit `e53c254`) loeschen.** Er ist dauerhaft
  `diverged` und hat die Vorpruefung zwei Tage lang blockiert; die Vorpruefung ist inzwischen
  repariert (ein Branch mit offenem oder gemergtem PR blockiert nicht mehr), der Branch ist also
  harmlos, aber unnoetig. Aus einer Agenten-Sitzung heraus nicht loeschbar: `git push --delete`
  bricht die Verbindung ab, die REST-Route ist vom Proxy gesperrt.
- **Am Geraet nachsehen** (nichts davon laesst sich im Test belegen): die neunzig Sekunden
  Mindestaufenthalt draussen, der Vielfaltsbonus, die Verweildauern der Reaktionen, der Nachklang
  mit 3 Minuten / +5 / +2, MOVE bei 2.420 ms - und ob "Paper Bridges" neben "Lantern Streets"
  passt.
- **Die 86 Sekunden Begruessung** aus ITO-0023 lassen sich erst beurteilen, wenn das erste der
  sechs Stuecke erzeugt ist.
