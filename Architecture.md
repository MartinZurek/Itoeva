# Architecture.md

Technische Landkarte von Itoeva: Modulstruktur, Verantwortlichkeiten, Datenflüsse, wichtige
Klassen, bekannte technische Schulden und mögliche zukünftige Modularisierung. Für das Produkt-
*Warum* siehe [Vision.md](Vision.md); für den Automations-Prozess, der dieses Repository
weiterentwickelt, siehe den Abschnitt "Die Evolution-Pipeline" unten und
[EVOLUTION.md](EVOLUTION.md); für konkrete Arbeitspakete [NextTasks.md](NextTasks.md).

Stand dieser Analyse: 2026-08-22, per Zeilenzählung und Struktur-Scan des Repositorys unter
`claude/itoevo-latest-updates-xvzr8b` (Basis `main`@`74fe7eb`). Zahlen verändern sich mit jeder
Evolution - als Größenordnung und zur Priorisierung sind sie trotzdem brauchbar.

## Module

Drei Gradle-Module, ein gemeinsamer Kern:

```
core       - reine Datenschicht + Planungslogik, von :app UND :app-sim genutzt
app        - Hardware-Fassung fuer Nothing-Geraete (physische Glyph-Matrix)
app-sim    - Simulator-Fassung: Matrix als Rundbild + eigenstaendiger "Spiel"-Modus
```

`:app` und `:app-sim` hängen beide von `:core` ab (`implementation(project(":core"))`); sie
hängen nicht voneinander ab. Das ist die einzige erzwungene Modulgrenze im Projekt - alles
innerhalb eines Moduls ist frei erreichbar, es gibt keine weitere Paketkapselung.

| Modul | .kt-Dateien (main) | Zweck |
|---|---|---|
| `core` | 30 | Room-Entities/DAOs, Reminder-Scheduling, geteilte Zustände (Play-/Quiet-Mode) |
| `app` | 18 | Hardware-Glyph-Service, eigener Reminder-Flow, eigene UI |
| `app-sim` | 128 | Simulator-UI, Spielwelt (`matrix/`), Widget, eigene Datenbank |

`:app-sim` ist mit Abstand das größte und am schnellsten wachsende Modul (Play-Modus,
Beziehungen, Lore - die meisten Evolutionen der letzten Woche landen hier).

### `core` - geteilter Kern

Verantwortlich für alles, was beide Apps identisch brauchen und das nicht dupliziert werden soll:

- **Datenschicht** (`data/`): `GlyphReminder` + DAO/Repository, `LibraryAnimation` + DAO/
  Repository, `BuiltInAnimationSelection`, Frame-Codec/-Crossfade für die Matrix-Animationen,
  `ReminderValidation`.
- **Reminder-Planung** (`reminder/`): `ReminderScheduler` (AlarmManager-Wrapper, siehe
  "Datenfluss: Erinnerungen" unten), `ReminderWatchdog`/`ReminderWatchdogWorker` (WorkManager-
  Sicherheitsnetz gegen verlorene Alarme), `ReminderRescheduleWorker`, `PlayModeState`,
  `QuietModeState`, `ActiveProfilePrefs`.

`core` enthält bewusst keine UI und keine App-spezifische Logik - das hält es für beide Apps
gleichermaßen nutzbar.

### `app` - Hardware-Fassung

Eigenständige Glyph-Matrix-Ansteuerung über einen System-Service (`glyph/GlyphMatrixService.kt`,
`GlyphMatrixConnection.kt`), eigener `ReminderGlyphService`/`ReminderAlarmReceiver` und eigene,
schlankere UI (`ui/ReminderScreen.kt`, `ui/LibraryScreen.kt`). Kein Play-Modus, keine
Beziehungen, keine Lore - dieses Modul bildet nur die Kern-Erinnerungsfunktion auf echter
Glyph-Hardware ab.

### `app-sim` - Simulator- und Spiel-Fassung

Größtes Modul, in Unterpakete gegliedert:

| Paket | Dateien | Inhalt |
|---|---|---|
| `ui/` | 54 | Compose-Bildschirme: `HomeScreen`, `DockScreen`, `ReminderScreen`, `PlayTalk*`, Einstellungen |
| `matrix/` | 44 | Spielwelt-Simulation: `PlayScene`, `AvatarAnimations`, `PlayRoutine`, Rendering |
| `data/` | 8 | Eigene Room-Datenbank (`AppDatabase`, getrennt von `core`), Feed-Events, Play-State |
| `reminder/` | 3 | `ReminderTrigger`, `BootReceiver`, `ReminderAlarmReceiver` |
| `settings/` | 2 | `SettingsCatalog` (zentrales Schlüsselregister) + `SettingsStore` |
| `state/` | 2 | `TamaState` + Mapping zwischen Rohdaten und UI-Zustand |
| `widget/` | 1 | `GlyphClockWidgetProvider` (Home-Screen-Widget, minütliche Alarme) |

**Wichtig:** `:app-sim` hat eine eigene `AppDatabase` mit eigenen Migrationen, unabhängig von
`core`s Reminder-Datenbank. Zwei getrennte Room-Datenbanken in einem Modul ist eine bewusste,
aber im Repository nirgends explizit begründete Konstruktion - ein guter Kandidat für eine kurze
Dokumentations-Aufgabe (siehe NextTasks.md).

## Datenfluss: Erinnerungen (die Kerninteraktion)

```
ReminderScheduler (core)          -- setzt AlarmManager-Alarm fuer naechste Faelligkeit
        |
        v
ReminderAlarmReceiver (app-sim)   -- Alarm feuert, Displayzustand wird geprueft
        |
        v
ReminderTrigger (app-sim)          -- schreibt Ausloesung, sendet ueber ReminderAnimationBus
        |
        v
HomeScreen.kt (app-sim/ui)         -- LaunchedEffect empfaengt Bus-Event, zeigt Animation
        |                             (activeReminder-State, siehe HomeScreen.kt)
        v
Nutzer zieht Uhr auf Avatar        -- Kollisionspruefung waehrend des Ziehens (nicht erst
   ODER auf Speicherplatz             beim Loslassen), siehe AvatarFeeding.overlaps
        |
        v
feedOccurrence() (HomeScreen.kt)   -- gemeinsamer Kern: AvatarFeeding.logFeedEvent (Room-Schreibung,
        |                             app-sim/data/AvatarFeedEventDao), dann Reaktionsanimation
        v
AvatarFeeding.playReaction         -- spielt Animation, aktualisiert Pflegebuch/Stimmung/XP
```

Das Home-Screen-Widget (`widget/GlyphClockWidgetProvider.kt`) und `DockScreen.kt` (Nachttisch-
Modus) hängen an derselben `ReminderAnimationBus`/`AvatarFeeding`-Kette, haben aber jeweils
eigene UI-Implementierungen der Zieh-/Fütter-Geste - siehe "Duplizierte Logik" unten.

Zeitgenauigkeit ist bewusst *ungefähr*, nicht exakt: Ohne `SCHEDULE_EXACT_ALARM`-Berechtigung
(bewusste Entscheidung, siehe `app-sim/AndroidManifest.xml`) läuft `setAndAllowWhileIdle`, das
Android mit anderen Alarmen bündeln kann - dokumentiertes Verhalten, keine Zeitzonen- oder
Systemuhr-Abweichung (siehe `widget/GlyphClockWidgetProvider.kt`, Kommentar bei
`scheduleNextTick`).

## Die Evolution-Pipeline (wie dieses Repository selbst arbeitet)

Zwei parallele, unterschiedlich weit entwickelte Automatisierungswege existieren im Repository:

1. **`.github/workflows/claude-primary-run.yml`** (1786 Zeilen) - der tatsächlich aktive Weg.
   Zwei getrennte Jobs pro Lauf: `evolve` (liest, baut, testet, erzeugt Commit + Bundle, hat
   *kein* Push-Recht) und `publish` (prüft das Bundle unabhängig nach, pusht, führt selbst
   keinen Modellcode aus). Diese Trennung ist die eigentliche Sicherheitsgrenze: Abo-Token und
   Push-Recht liegen nie in derselben VM. Läuft 3x täglich über eine externe Routine, wählt die
   nächste offene Aufgabe aus `evolutions/BACKLOG.md`.
2. **`runner/`** (PowerShell, Windows-Task-Scheduler-basiert, `.ps1`-Dateien + JSON-Schemas) -
   laut eigenem `runner/README.md` "standardmäßig deaktiviert", nirgends in `README.md` oder
   `EVOLUTION.md` referenziert. Wirkt wie eine frühere oder alternative Architektur für denselben
   Zweck, die vom GitHub-Actions-Weg überholt wurde. **Ungeklärt, ob noch gebraucht** - siehe
   NextTasks.md.

`evolutions/BACKLOG.md` ist die Aufgaben-Warteschlange dieser Pipeline (Format: `## [status]
ITO-NNNN - Titel`, Status `open`/`done`, Statuswechsel wird ausschließlich vom `publish`-Job auf
`main` geschrieben). `EVOLUTION.md` ist ihr Regelwerk plus datiertes Entscheidungsprotokoll.

`.github/workflows/verify.yml` (368 Zeilen, seit PR #21) ist der PR-Prüflauf: ein vorgeschalteter
Job bestimmt anhand geänderter Pfade, ob App-Code betroffen ist, und überspringt die teuren
Jobs (Emulator-Tests, Lint/R8) bei reinen Text-/Backlog-Änderungen. `deliver-apk.yml` liefert
nach jedem Merge ein signiertes Test-APK nach Google Drive aus.

## Größte Dateien (Kandidaten für Aufteilung)

Nach Zeilenzahl, `.kt`-Dateien unter `src/main`, ohne Tests:

| Datei | Zeilen | Beobachtung |
|---|---|---|
| `app-sim/matrix/PlayScene.kt` | 3672 | Größte Datei im Repository. Prozedurale Kulissengenerierung für den Spielmodus - viele Einzelfälle (Tag/Nacht, Requisiten je Fortschrittspfad) in einer Datei. |
| `app-sim/ui/DockScreen.kt` | 2925 | Nachttisch-/Ambient-Modus: eigene Zieh-, Zoom- und Fütterlogik, weitgehend parallel zu `HomeScreen.kt`. |
| `app-sim/ui/HomeScreen.kt` | 1544 | Hauptbildschirm inkl. der neuen Speicherplatz-Logik (PR #20). |
| `app-sim/matrix/AvatarAnimations.kt` | 1285 | Animationsdaten für alle sechs Wesen in einer Datei. |
| `app-sim/ui/ReminderScreen.kt` | 1133 | Erinnerungsverwaltung; siehe Duplikat-Hinweis unten. |
| `app/ui/ReminderScreen.kt` | 1051 | Fast dieselbe Funktionsoberfläche wie oben, siehe unten. |

Keine dieser Dateien ist per se ein Fehler - `PlayScene.kt` etwa ist überwiegend Daten
(Requisiten-Definitionen), nicht Kontrollfluss. Aber ab dieser Größe wird jede Änderung für einen
Agenten teurer (mehr Kontext zum Lesen, höheres Risiko widersprüchlicher Teiländerungen) und für
einen menschlichen Reviewer schwerer diffbar. Siehe NextTasks.md für konkrete Aufteilungs-
Kandidaten.

## Duplizierte Logik

Die Funktionsoberfläche von `app/ui/ReminderScreen.kt` und `app-sim/ui/ReminderScreen.kt`
stimmt zu einem großen Teil überein (per Funktionsnamen-Diff verifiziert) - beide implementieren
unabhängig voneinander sehr ähnliche Erinnerungsverwaltungs-UI, obwohl `core` bereits die
gemeinsame Datenschicht dafür bereitstellt. Ebenso `app/glyph/ReminderAnimations.kt` und
`app-sim/matrix/ReminderAnimations.kt` (fast identische Funktionsoberfläche). Das ist vermutlich
historisch entstanden (zwei Apps, gewachsen ohne gemeinsame UI-Schicht) und nicht zwingend falsch
- Compose-UI zwischen einer Hardware- und einer Simulator-App eins zu eins zu teilen ist nicht
immer sinnvoll. Es ist aber ungeprüft, wie viel davon sich verlustfrei nach `core` oder ein neues
gemeinsames UI-Modul heben ließe. Siehe NextTasks.md für einen begrenzten Rechercheauftrag dazu.

`HomeScreen.kt` und `DockScreen.kt` innerhalb von `app-sim` dupliziert die Zieh-/Kollisions-
Fütterlogik ebenfalls teilweise (siehe PR #20-Beschreibung: DockScreen wurde bei den
Speicherplätzen bewusst *nicht* mitgezogen, "guter Kandidat für einen Folge-PR" - Originalzitat
aus der PR). Das ist eine bekannte, bereits benannte Lücke.

## Technische Schulden (Repository-Hygiene)

- **Acht leere Dateien im Wurzelverzeichnis, versehentlich eingecheckt:** `0%`, `16%`, `33%`,
  `50%`, `100%`, `gluecklich`, `hungrig`, `traurig`, `zufrieden`. Nach Namen zu urteilen Reste
  eines fehlgeschlagenen Shell-Kommandos (evtl. eine unquotierte Variable, die Wörter aus
  Stimmungstexten als Dateinamen erzeugt hat). Mindestens `gluecklich` hat sogar einen echten
  Commit in seiner Historie, ist also nicht neu. Harmlos, aber Repository-Rauschen. Siehe
  NextTasks.md.
- **Zwei parallele Automatisierungs-Architekturen** (`claude-primary-run.yml` vs. `runner/`),
  von denen eine offensichtlich unbenutzt ist, aber nicht als solche markiert oder entfernt
  wurde.
- **`claude-primary-run.yml` selbst ist mit 1786 Zeilen die mit Abstand größte Datei im
  gesamten Repository** - größer als die meisten App-Module zusammen. Für eine Workflow-Datei
  ungewöhnlich groß; enthält vermutlich viel Prompt-/Konfigurationstext statt reiner
  Steuerungslogik, was ihre Wartbarkeit nicht automatisch verschlechtert, aber schwer machst,
  Änderungen daran zu überblicken.
- **`README.md` ist mit 829 Zeilen eine einzige, sehr breite Datei** (Setup, Konzept, Module,
  Persistenz-Landkarte, Release-Prozess, Build-Anleitung in einer Datei). Für neue menschliche
  Mitwirkende wie für Agenten, die nur einen Teilaspekt brauchen, ist das mehr Kontext als nötig.
- **`:app-sim` führt eine zweite, von `core` unabhängige Room-Datenbank.** Nicht dokumentiert,
  warum getrennt statt erweitert.
- **Migrationstestabdeckung ist ungleich verteilt:** `app` hat eine `AppDatabaseMigrationTest`,
  `app-sim` ebenfalls - beide vorhanden, aber angesichts der Konsequenzen eines Fehlers (siehe
  Vision.md: kein Cloud-Backup, ein Migrationsfehler kann echte Nutzerdaten dauerhaft zerstören)
  ist unklar, ob jede neue Schema-Änderung tatsächlich zuverlässig eine neue Migration *und* einen
  neuen Testfall erzwingt, oder ob das von Disziplin statt von einer erzwingenden Prüfung abhängt.

## Mögliche zukünftige Modularisierung

Nicht als Entscheidung, sondern als Diskussionsgrundlage für NextTasks.md:

- Ein drittes Gradle-Modul `:core-ui` (oder ähnlich) für Compose-Bausteine, die zwischen `app`
  und `app-sim` tatsächlich identisch sein könnten (z. B. Teile von `ReminderScreen`), falls die
  Recherche aus "Duplizierte Logik" das stützt.
- `matrix/PlayScene.kt` in mehrere Dateien nach Verantwortungsbereich (z. B. Requisiten-Katalog
  getrennt von Kulissen-Aufbaulogik) - eine reine Verschiebe-Operation ohne Verhaltensänderung,
  gut geeignet als kleine, risikoarme Agenten-Aufgabe.
- `README.md` in `README.md` (Kurzeinstieg, Setup) plus themenspezifische Dateien
  (`docs/persistence.md`, `docs/release-process.md` o. ä.) aufteilen - senkt den Kontext, den ein
  Agent laden muss, um an einem Teilbereich zu arbeiten.

Keiner dieser Punkte ist dringend oder blockierend. Sie sind hier festgehalten, damit sie nicht
bei jeder neuen Analyse erneut entdeckt werden müssen.
