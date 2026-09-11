# Architecture.md

Technische Landkarte von Itoeva: Modulstruktur, Verantwortlichkeiten, Datenflüsse, wichtige
Klassen, bekannte technische Schulden und mögliche zukünftige Modularisierung. Für das Produkt-
*Warum* siehe [Vision.md](Vision.md); für den Automations-Prozess, der dieses Repository
weiterentwickelt, siehe den Abschnitt "Die Evolution-Pipeline" unten und
[EVOLUTION.md](EVOLUTION.md); für konkrete Arbeitspakete [NextTasks.md](NextTasks.md).

Stand der Modul- und Größenanalyse: 2026-08-22, per Zeilenzählung und Struktur-Scan des
Repositorys unter `claude/itoevo-latest-updates-xvzr8b` (Basis `main`@`74fe7eb`). Die
Architektureinordnung der öffentlichen Charakter-Streams wurde am 2026-09-06 ergänzt. Zahlen
verändern sich mit jeder Evolution - als Größenordnung und zur Priorisierung sind sie trotzdem
brauchbar.

## Diese Datei nicht linear lesen

Diese Datei ist eine Nachschlage-Landkarte, kein Fließtext, der komplett gelesen werden muss.
Laut AgentGuide.md ist sie ohnehin nur bei Aufgaben relevant, die Modulgrenzen, Datenfluss oder
eine der unten gelisteten großen/duplizierten Dateien betreffen - dann gezielt nur den passenden
Abschnitt öffnen:

| Abschnitt | Lesen, wenn die Aufgabe... |
|---|---|
| [Module](#module) | ...eine Modul-Zugehörigkeit klären muss (`core`/`app`/`app-sim`). |
| [Datenfluss: Erinnerungen](#datenfluss-erinnerungen-die-kerninteraktion) | ...die Auslöse-/Zieh-/Fütter-Kette betrifft. |
| [Die Evolution-Pipeline](#die-evolution-pipeline-wie-dieses-repository-selbst-arbeitet) | ...den Automations-Prozess selbst (Workflows, `runner/`) betrifft. |
| [Öffentliche 24/7-Charakter-Streams](#öffentliche-247-charakter-streams-zielarchitektur-nicht-implementiert) | ...Streaming, Dauerbetrieb oder öffentliche Weltinstanzen betrifft. |
| [Größte Dateien](#größte-dateien-kandidaten-für-aufteilung) | ...eine der großen Dateien ändert oder eine Aufteilung plant. |
| [Duplizierte Logik](#duplizierte-logik) | ...Code in `app` UND `app-sim` gleichzeitig betrifft. |
| [Technische Schulden](#technische-schulden-repository-hygiene) | ...eine NextTasks.md-Aufgabe aus dem Bereich Hygiene/Cleanup umsetzt. |
| [Mögliche zukünftige Modularisierung](#mögliche-zukünftige-modularisierung) | ...eine größere Umstrukturierung plant (selten). |

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

1. **`.github/workflows/claude-primary-run.yml`** - der tatsächlich aktive Weg.
   Zwei getrennte Jobs pro Lauf: `evolve` (liest, baut, testet, erzeugt Commit + Bundle, hat
   *kein* Push-Recht) und `publish` (prüft das Bundle unabhängig nach, pusht, führt selbst
   keinen Modellcode aus). Diese Trennung ist die eigentliche Sicherheitsgrenze: Abo-Token und
   Push-Recht liegen nie in derselben VM. Der interne Zeitplan bietet tagsüber alle drei Stunden
   eine Gelegenheit; ein Wächter verhindert parallele Evolutionen. Ein offener Eintrag aus
   `evolutions/BACKLOG.md` hat Vorrang. Ist keiner vorhanden, wird der kontrollierte
   Tagesablauf-Dauerauftrag aus `evolutions/DAILY_LIFE_TASK.md` verwendet.
2. **`runner/`** (PowerShell, Windows-Task-Scheduler-basiert, `.ps1`-Dateien + JSON-Schemas) -
   laut eigenem `runner/README.md` "standardmäßig deaktiviert", nirgends in `README.md` oder
   `EVOLUTION.md` referenziert. Wirkt wie eine frühere oder alternative Architektur für denselben
   Zweck, die vom GitHub-Actions-Weg überholt wurde. **Ungeklärt, ob noch gebraucht** - siehe
   NextTasks.md.

**Wichtig für Tokenverbrauch-Aufgaben (NT-045 bis NT-047):** Die Dateigröße von
`claude-primary-run.yml` ist kein verlässlicher Indikator für Tokenverbrauch. Nur der eigentliche
`PROMPT`-Text in den Schritten "Builder-Session" und "Reviewer-Session" (je ein begrenzter Block,
zuzüglich interpolierter Aufgabe/Repo-Fakten/Diff) geht als Kontext an das Modell. Der große Rest
der Datei ist reine Bash-/Workflow-Orchestrierung (Token-Dateideskriptor-Handling,
Git-Gates, Hash-Nachrechnung, Diagnose-Uploads), die den Modellkontext nie erreicht - Kürzen
dieser Orchestrierung spart Actions-Laufzeit, aber keine Tokens. Seit 2026-08-22 verweisen beide
Prompts explizit auf `AGENTS.md` und `AgentGuide.md`s Minimal-Startsequenz, damit die eigene
Read/Glob/Grep-Erkundung der Sessions nicht routinemäßig ganze Dokumente lädt - das war der
tatsächliche Hebel, nicht die Dateigröße der Workflow-YAML selbst.

`evolutions/BACKLOG.md` ist die Warteschlange für ausdrücklich priorisierte Einzelaufgaben
(Format: `## [status] ITO-NNNN - Titel`, Status `open`/`done`, Statuswechsel wird ausschließlich
vom `publish`-Job im Evolutionsbranch geschrieben). Ohne offenen Eintrag steuern
`evolutions/DAILY_LIFE_TASK.md` und das lernende, versionierte
`evolutions/DAILY_LIFE_LEARNING.md` die nächste kleine Spielerlebnis-Evolution. Der Task ist für
den Builder schreibgeschützt; Feedback, Evidenz und begrenzte Heuristiken reisen im normalen
Review-PR mit. `.github/`, `runner/`, Berechtigungen und Sicherheits-Gates bleiben von dieser
Selbstanpassung ausgeschlossen. `EVOLUTION.md` ist das Regelwerk plus datiertes
Entscheidungsprotokoll.

`.github/workflows/verify.yml` (368 Zeilen, seit PR #21) ist der PR-Prüflauf: ein vorgeschalteter
Job bestimmt anhand geänderter Pfade, ob App-Code betroffen ist, und überspringt die teuren
Jobs (Emulator-Tests, Lint/R8) bei reinen Text-/Backlog-Änderungen. `deliver-apk.yml` liefert
nach jedem Merge ein signiertes Test-APK nach Google Drive aus.

## Living Agent System (freigegeben, inkrementell)

Der Integrationsplan steht in [LIVING_AGENT.md](LIVING_AGENT.md). Der neue reine Kotlin-Kern lebt
unter `app-sim/.../living/`: Er bewertet Beduerfnisse und Weltzustand, haelt Ziel und Plan,
erzeugt bedeutungsvolle Ereignisse und liefert eine erklaerbare Momentaufnahme. Er kennt weder
Compose noch Android, Room oder Renderer.

**Der entscheidende Teil steht seit NT-063 (Schnitt 2a) und laeuft in der Offline-Strecke mit:**
`LivingSimulation.step` fuehrt Beduerfnisse, Zielwahl, Plan und Weltzustand zusammen und prueft
die Voraussetzungen des naechsten Schritts unmittelbar vor der Ausfuehrung - nicht beim Planen.
Faellt eine Voraussetzung, faellt der Plan und das ZIEL bleibt; der naechste Schritt leitet aus
derselben Absicht einen anderen Weg ab. Daran haengt der Unterschied zwischen einem lebendigen
Wesen und einer festen Animationsfolge. Erinnerung, Beziehungen, symbolische Verstaendigung und
profilbezogene Persistenz stehen seit NT-067/NT-064 ebenfalls.

Seit NT-065 verbindet `LivingRuntimeAdapter` diese Domaene mit der bestehenden
Ausfuehrungsebene:

- `PlayRoutine` und `RoutineStep` choreografieren gewaehlte Aktionen.
- `PlayScene`, `PlayPantry`, `PlayWallet`, `PlayPresence` und `PlayTimeLapse` liefern
  Welt- und Runtime-Adapter.
- `PlayAmbientActivity` bleibt Quelle fuer nicht dringende Aktivitaetskandidaten.
- `AvatarSpecies` liefert nur Startbias; Erfahrungen duerfen Praeferenzen veraendern.
- Besuche werden spaeter ueber einen typisierten Symbolvertrag statt feste Punktblasen entschieden.

Der Adapter bereitet Kernwirkung und vorhandene `PlayRoutine` gemeinsam vor. Erst wenn die
Routine vollstaendig beendet ist, wird der profilbezogene Snapshot uebernommen; eine dazwischen
kommende echte Erinnerung kann deshalb keine unsichtbar bereits bezahlte oder verdiente Handlung
hinterlassen. Die alte harte `Vorrat leer und Geld fehlt -> WORK`-Abzweigung in `DockScreen` ist
entfallen. `PlayAmbientActivity` bestimmt bei Freizeit weiterhin die sichtbare Variante, aber
nicht mehr, ob Hunger, Energie oder soziale Naehe uebergangen werden. Dieselbe Adaptergrenze
verbucht ausdruecklich erbetene Routinen, ohne das autonome Ziel zu ueberschreiben, und stellt
den Zustand vor dem ersten Gespraech wieder her. Oeffnungszeiten werden vor jedem in einer
Routine zusammengefassten Kernschritt erneut bestimmt.

Es entsteht keine zweite Engine und kein `StoryManager`. Der Kern wird zuerst mit
deterministischen JVM-Tests bewiesen, profilbezogen persistiert und gezielt an `DockScreen`
angeschlossen. Eine eventuelle Auslagerung in ein neues Modul bleibt bis zu
Messdaten aus dem Streaming-PoC offen.

## Öffentliche 24/7-Charakter-Streams (Zielarchitektur, nicht implementiert)

### Wie das Ziel zur heutigen Architektur passt

Der kleinste glaubwürdige Ausgangspunkt ist `:app-sim`: Dort existieren bereits Pixelwelt,
Avatar-Rendering, Tagesabläufe, Orte, Musikrollen und die sichtbare Spieloberfläche. Ein
Android-Emulator kann genau diese laufende Instanz darstellen; OBS kann ihr Bild und ihren Ton
aufnehmen beziehungsweise an einen Streaming-Endpunkt senden. Das beweist den Zuschauerpfad,
ohne vorzeitig einen zweiten Renderer oder eine Server-Spielengine zu erfinden.

Der aktuelle Code ist jedoch eine Vordergrund-App, kein belegter 24/7-Dienst. Für einen
Produktionsbetrieb fehlen unter anderem:

- ein pro Charakter isolierter öffentlicher Zustand mit eigener Weltzeit und stabiler Identität;
- Wiederanlauf aus einem überprüfbaren Checkpoint statt Rückkehr an einen beliebigen Startzustand;
- Überwachung von Prozess, Bildfortschritt, Audio, Encoder und Plattformverbindung;
- sichere Verwaltung von Stream-Keys und anderen Betriebsgeheimnissen;
- Ressourcen- und Kostenmessung je gleichzeitig laufender Charakterinstanz;
- ein sicherer, nachvollziehbarer Ereigniskanal für spätere Begegnungen zwischen Instanzen.

Die persönliche Reminder-Datenbank ist dafür ausdrücklich keine Datenquelle. Öffentliche
Instanzen verwenden nur erfundene, eigens konfigurierte Weltzustände; sie lesen weder lokale
Reminder noch Nutzerhistorien, Konten oder medizinische Inhalte.

### Gestufter Weg statt vorweggenommener Cloud-Plattform

| Stufe | Technischer Schnitt | Was damit gelernt wird | Noch nicht enthalten |
|---|---|---|---|
| 0 - lokaler PoC | Eine `:app-sim`-Instanz im Android-Emulator, Fenster- und Audioaufnahme in OBS, zunächst lokale Aufzeichnung oder privater Teststream | Bleibt das Avatarleben über Stunden interessant, stabil, korrekt skaliert und hörbar? | Cloud, 24/7-SLA, mehrere Charaktere, Zuschauerinteraktion |
| 1 - einzelner Betriebsprototyp | Ein isolierter Host mit genau einer öffentlichen Instanz, Prozessaufsicht, Neustart und Zustands-Checkpoint | Welche Laufzeit-, RAM-, CPU-, Encoder- und Wiederanlaufkosten entstehen wirklich? | Flotte aus sechs Instanzen, gemeinsame Welt |
| 2 - wiederholbare Charakterinstanzen | Parametrisierter Start je Wesen, getrennte Zustände und standardisierte Gesundheitsprüfung | Lässt sich jede Figur unabhängig betreiben und aktualisieren? | Direkte Interaktion zwischen Instanzen |
| 3 - sichere Weltbegegnungen | Kleiner typisierter Ereigniskanal zwischen öffentlichen Instanzen | Wie können Begegnungen koordiniert werden, ohne beliebige Fernsteuerung oder private Daten? | Zahlungen, ungeprüfter Freitext, offene Nutzerbefehle |

Ob Stufe 1 weiter Android-Emulatoren verwendet oder ob Tagesablauf und Weltzustand später in ein
neues reines Modul wie `:world-core` herausgelöst werden, bleibt bis nach den Messungen aus
Stufe 0 eine `OPEN DECISION`. Ein Emulator pro Stream maximiert Wiederverwendung, kann aber teuer
und betrieblich schwergewichtig sein. Eine Headless-Engine wäre langfristig effizienter und
testbarer, erzeugt heute aber einen zweiten Laufzeitpfad, bevor bekannt ist, ob er gebraucht wird.

### Grenzen des ersten Proof-of-Concepts

Der erste PoC verändert weder App-Code noch GitHub-Workflows noch Cloud-Infrastruktur. Er startet
eine echte vorhandene `:app-sim`-Instanz, hält den Spielmodus sichtbar und zeichnet mindestens
einen längeren Lauf über OBS auf. Dabei werden Stabilität, Aktivitätsvielfalt, Tagesphasen,
Musikwechsel, Seitenverhältnis, CPU/RAM und Unterbrechungs-/Wiederanlaufverhalten protokolliert.

YouTube unterstützt laut seinen
[Encoder-Hinweisen](https://support.google.com/youtube/answer/2853702) die Ausspielung über
RTMP/RTMPS und empfiehlt ausdrücklich Tests mit realistischer Bewegung und Audio sowie die
Überwachung des Streamzustands. Twitch behandelt parallele Ausspielung in seiner
[Simulcasting-FAQ](https://help.twitch.tv/s/article/simulcasting-guidelines). Deshalb bleibt die
Mehrfachausspielung ein eigener Prüfschritt; insbesondere werden im PoC keine Chats verschiedener
Plattformen zusammengeführt und Zuschauer nicht von einer Plattform zur anderen gedrängt.

Ein sachlicher App-Link in der Beschreibung ist als gewünschter Ausgangspunkt festgehalten.
YouTubes [Richtlinie zu externen Links](https://support.google.com/youtube/answer/9054257) gilt
auch für Livestreams und Beschreibungen. Links, Overlays, KI-Kennzeichnung, Musikrechte und
Monetarisierung werden vor einem öffentlichen Test anhand der dann aktuellen Plattformregeln
geprüft. Stream-Keys gehören ausschließlich in
lokale beziehungsweise spätere Cloud-Secrets und niemals in Repository, Logs oder
PR-Beschreibungen.

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
- **`claude-primary-run.yml` selbst ist die mit Abstand größte Workflow-Datei im Repository** -
  größer als die meisten App-Module. Für eine Workflow-Datei
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
