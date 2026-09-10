# Uebergabe: Stand am 10. September 2026

Diese Datei ist fuer den, der als Naechstes weitermacht - Mensch oder Agent, ausdruecklich auch
ein anderes Modell als das, das sie geschrieben hat. Sie ersetzt nicht
[`CLOUD_CODE_BRIEFING.md`](CLOUD_CODE_BRIEFING.md) (Produktvision) und nicht
[`evolutions/BACKLOG.md`](evolutions/BACKLOG.md) (die Arbeitsliste), sondern sagt, **wo genau der
Faden liegt** und **welche Fallen auf dem Weg dahin schon zugeschnappt sind**.

## 1. Der offene Faden: die sechs Charakterstuecke

Der Auftraggeber will, dass **jedes der sechs Wesen ein eigenes Musikstueck bekommt** - eines
etwas grungelastiger, eines mehr am Klavier, eines melancholischer. Fernziel: diese Stuecke
spaeter je Charakter live auf YouTube singen. Deshalb sollen sie sich **hoerbar voneinander
unterscheiden** und eine **singbare** Melodie haben; erzeugt werden sie trotzdem instrumental.

### Was fertig ist

| | |
|---|---|
| ITO-0016 | Rolle `CHARACTER_THEME` in `PlayMusicPlan.kt`, Variante 01-06 fest je Spezies (`MusicRole.characterThemeVariant`), Rolle in `generate_music.py` bekannt |
| ITO-0023 | **Wann** das Stueck laeuft: beim ersten Erscheinen des Wesens im Spielmodus an einem Kalendertag, ein Stueck lang. Begruendung vollstaendig im KDoc von `app-sim/.../matrix/PlayCharacterTheme.kt` |
| ITO-0017 | Prompt und Manifest-Eintrag fuer **PUFFLING** (`music/prompts/theme-puffling.txt`, `itoeva_theme_01`) |
| ITO-0018 | Prompt und Manifest-Eintrag fuer **STARLET** (`music/prompts/theme-starlet.txt`, `itoeva_theme_02`) |
| ITO-0019 | Prompt und Manifest-Eintrag fuer **WYRMLING** (`music/prompts/theme-wyrmling.txt`, `itoeva_theme_03`) |
| ITO-0020 | Prompt und Manifest-Eintrag fuer **FENNEC** (`music/prompts/theme-fennec.txt`, `itoeva_theme_04`) |
| ITO-0021 | Prompt und Manifest-Eintrag fuer **GLOOP** (`music/prompts/theme-gloop.txt`, `itoeva_theme_05`) |

### Was ansteht

**ITO-0022** - dieselbe Arbeit fuer HOOTLET. Der Eintrag im Backlog nennt den Charakter und
die musikalische Richtung und verweist auf ITO-0017 als Muster. **`theme-puffling` ist das
durchgerechnete Beispiel** - Prompt, Manifest, README-Zeile, Trockenlauf. Wer ITO-0022 macht,
kopiert diese vier Schritte und tauscht den Inhalt.

Zu liefern je Wesen:

1. `music/prompts/theme-<wesen>.txt` - englisch; Tempo, Instrumente, Klangbild, Stimmung, und am
   Ende eine ausdrueckliche Ausschlussliste. **Niemals einen konkreten Kuenstler oder ein
   konkretes Stueck nennen** - immer Eigenschaften und Instrumente.
2. Ein Eintrag in `music/manifest.json`: `role` `character_theme_background`, `android_resource`
   `itoeva_theme_0<n>` nach der Deklarationsreihenfolge von `AvatarSpecies` (PUFFLING 01, STARLET
   02, WYRMLING 03, FENNEC 04, GLOOP 05, HOOTLET 06), `duration_seconds` 90, `model`
   `small-music`, `output_format` `ogg`, `steps` 8, `cfg_scale` 1.0, ein bisher unbenutzter `seed`.
3. Eine Zeile in `music/README.md`.
4. `python tools/music/generate_music.py --track-id theme-<wesen> --dry-run` muss durchgehen. Das
   ist das Abnahmekriterium; in CI prueft es `verify-music-tooling.yml`.

**Kein Audio erzeugen.** `generate-music.yml` ist `workflow_dispatch` und wird **von einem
Menschen** ausgeloest. Der Auftraggeber will jedes Stueck einzeln hoeren, bevor es gemergt wird -
also je Track ein Lauf, nicht sechs auf einmal.

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
bash tools/reaction-preview/tests.sh          # derzeit 243 Tests, ~1,5 s
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
