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

Seit NT-066 liegt die Praesentationsgrenze unter `app-sim/.../stream/`.
Der lokale Fennec-Chat-Pilot liefert seit 2026-10-03 den oeffentlichen `PlayMap`-Graphen
zusammen mit Ort und Aktivitaet an den PC-Dienst. Ollama waehlt ausschliesslich aus einem
validierten Darstellungskatalog; `FennecWorld` prueft Kartenaktionen und Ziele erneut in
Android. `StreamFennecMap` verwendet `PlayMapScene` und `PlayMap.route`, unabhaengig vom
vorhandenen Reiseablauf. Die Darstellung veraendert keine Welt-/Reminder-Zustaende.
Kurze Folgefragen haben ein 45-Sekunden-Fenster je Zuschauer, ohne gespeicherten Chat-Verlauf;
der PC behaelt nur einen oeffentlichen Kartenort. Sprech- und Kartenanzeige haben getrennte
Lebenszeiten. `StreamTime` liefert Berlin als feste Kanalzeit plus UTC und frische Ortszeiten
aus acht IANA-Zonen. Nur die Stream-App setzt ihre Standardzone auf Berlin; die private App
bleibt bei der Geraetezone. Das Modell waehlt die Ortsfrage, berechnet aber keine Uhrzeit.
Der PC-Dienst haelt das vorhandene Modell mit einem einzelnen textfreien Ladefaden bereit;
Kaltstart/Auffrischung und deren Status bleiben getrennt von Zuschauerantworten und Twitch-Versand.
`LivingObservationSource` bietet ausschliesslich den aktuellen typisierten
`LivingObservation`-Snapshot je Profil. `LivingObservationFeed` nimmt nur abgeschlossene
Runtime-Ergebnisse auf, haelt ein begrenztes Ereignisfenster ohne Leerlauf-Ticks und veraendert
weder Agent noch Welt. Damit kann ein spaeteres Overlay Wunsch, Grund, Handlung, Plan,
Hindernis, wirksame Erinnerungen und das wichtigste juengste Ereignis lesen, ohne in die
Simulation zurueckzuschreiben.

Seit dem 2026-09-26 sitzt zwischen Kern und Ablaufwahl die **Decision Policy**
(`app-sim/.../decision/`, siehe `tools/decision-policy/README.md`): `DecisionCandidates` leitet
aus `LivingRuntimeAdapter.options` alle zulaessigen sichtbaren Ablaeufe ab (Gueltigkeitsschicht),
`OnnxDecisionPolicy` bewertet sie mit einem kleinen ONNX-Netz (reines Kotlin, keine neue
Abhaengigkeit, dasselbe Asset in App und Stream-Variante), `DecisionSelector` waehlt per Softmax.
`ExistingUtilityPolicy` ist die bisherige Wuerfelkette als Rueckfall. Der Kern bleibt Taktgeber;
`LivingSimulation.step` nimmt ein `chosenGoal` nur an, wenn es ohnehin zulaessig ist.

Es entsteht keine zweite Engine und kein `StoryManager`. Der Kern wird zuerst mit
deterministischen JVM-Tests bewiesen, profilbezogen persistiert und gezielt an `DockScreen`
angeschlossen. Eine eventuelle Auslagerung der Welt in ein neues Bibliotheksmodul bleibt bis zu
Messdaten aus dem Streaming-PoC offen; die Stream-APK kopiert diese Entscheidung nicht vorweg.

## Öffentliche 24/7-Charakter-Streams (lokaler Client-PoC, Produktion nicht implementiert)

### Wie das Ziel zur heutigen Architektur passt

Der kleinste glaubwürdige Ausgangspunkt ist `:app-sim`: Dort existieren bereits Pixelwelt,
Avatar-Rendering, Tagesabläufe, Orte, Musikrollen und die sichtbare Spieloberfläche. Ein
Android-Emulator kann genau diese laufende Instanz darstellen; OBS kann ihr Bild und ihren Ton
aufnehmen beziehungsweise an einen Streaming-Endpunkt senden. Das beweist den Zuschauerpfad,
ohne vorzeitig einen zweiten Renderer oder eine Server-Spielengine zu erfinden.

Seit NT-068 erzeugt `./gradlew :app-sim:assembleStream` eine separat installierbare,
debug-signierte APK unter `app-sim/build/outputs/apk/stream/app-sim-stream.apk`. Sie ist bewusst
eine Build-Variante von `:app-sim` und kein neues `:app-stream`-Anwendungsmodul: Ein Android-
Anwendungsmodul kann nicht als gemeinsame Spielbibliothek konsumiert werden. Ein neues Modul
muesste die heutige Living-/Pixelwelt daher kopieren oder vor den Laufzeitmessungen gross
verschieben. Die Variante teilt dagegen Code, Renderer, Musik, Room-Schema und Living-Agent-
Adapter vollstaendig, hat durch die abweichende `applicationId` aber einen isolierten lokalen
Zustand und kann neben der normalen App installiert werden.

Der Stream-Schalter veraendert nur die Praesentationsgrenze:

- Die bestehende Play-Mode-Erinnerung entsteht weiter ueber Repository, Scheduler,
  `ReminderTrigger` und `ReminderAnimationBus`.
- Eine oeffentliche, typisierte Ausloesung belegt automatisch den ersten von vier vorhandenen
  `ActionSlotStore`-Plaetzen. Medizin und frei beschriftete Inhalte werden verworfen.
- Ein Tippen auf einen belegten Platz ist der lokale Viewer-Simulator. Es erzeugt einen
  `ExternalImpulse` mit Simulationszeit und einen auf 0,15 begrenzten `GoalInfluence`.
- Der Living Agent waehlt weiter selbst. Ein laufendes oder dringenderes Ziel bleibt bestehen;
  erst eine passende, vollstaendig sichtbare `PlayRoutine` verbraucht Ausloesung und Slot.

`ExternalImpulse` und `LivingObservationSource` sind die schmalen Schreib- beziehungsweise
Lesegrenzen fuer ein spaeteres Backend. Weder Twitch noch OAuth, Netzwerk, Konten oder
Bezahlrechte sind Teil dieses Schnitts.

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
| 0 - lokaler PoC | Die Stream-Variante von `:app-sim` im Android-Emulator, lokaler Viewer-Simulator sowie Fenster- und Audioaufnahme in OBS | Bleiben Avatarleben, vier Slots und begrenzte Impulse ueber Stunden interessant, stabil, korrekt skaliert und hoerbar? | Cloud, 24/7-SLA, mehrere Charaktere, echte Plattformanbindung |
| 1 - einzelner Betriebsprototyp | Ein isolierter Host mit genau einer öffentlichen Instanz, Prozessaufsicht, Neustart und Zustands-Checkpoint | Welche Laufzeit-, RAM-, CPU-, Encoder- und Wiederanlaufkosten entstehen wirklich? | Flotte aus sechs Instanzen, gemeinsame Welt |
| 2 - wiederholbare Charakterinstanzen | Parametrisierter Start je Wesen, getrennte Zustände und standardisierte Gesundheitsprüfung | Lässt sich jede Figur unabhängig betreiben und aktualisieren? | Direkte Interaktion zwischen Instanzen |
| 3 - sichere Weltbegegnungen | Kleiner typisierter Ereigniskanal zwischen öffentlichen Instanzen | Wie können Begegnungen koordiniert werden, ohne beliebige Fernsteuerung oder private Daten? | Zahlungen, ungeprüfter Freitext, offene Nutzerbefehle |

Ob Stufe 1 weiter Android-Emulatoren verwendet oder ob Tagesablauf und Weltzustand später in ein
neues reines Modul wie `:world-core` herausgelöst werden, bleibt bis nach den Messungen aus
Stufe 0 eine `OPEN DECISION`. Ein Emulator pro Stream maximiert Wiederverwendung, kann aber teuer
und betrieblich schwergewichtig sein. Eine Headless-Engine wäre langfristig effizienter und
testbarer, erzeugt heute aber einen zweiten Laufzeitpfad, bevor bekannt ist, ob er gebraucht wird.

### Grenzen des ersten Proof-of-Concepts

Der erste PoC veraendert keine GitHub-Workflows oder Cloud-Infrastruktur. Er startet die echte
vorhandene `:app-sim`-Runtime in ihrer Stream-Variante, haelt den Spielmodus sichtbar und zeichnet
mindestens einen laengeren Lauf ueber OBS auf. Dabei werden Stabilitaet, Aktivitaetsvielfalt,
Tagesphasen, Musikwechsel, Seitenverhaeltnis, CPU/RAM, Save-Slot-Interaktion und
Unterbrechungs-/Wiederanlaufverhalten protokolliert.

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

## Aktive Smartphone-Steuerung (05.10.2026)

`ui/GameControls.kt` verfolgt zwei Pointer-IDs unabhaengig und meldet einen radial
normierten `PlayControl.Stick` plus freiwillige Sprung-/Roll-/Ruhe-Befehle.
`matrix/GameMovement.kt` berechnet im bestehenden `withFrameMillis`-Takt
Beschleunigung, Gangphase, freiwillige Aktionen und Bodenkontakt. `DockScreen`
wendet die Hoehe genau einmal auf den vorhandenen Welt-Fusspunkt an; Routinen,
Fuetterung und Dialoge behalten die Darstellungshoheit. Standflaechen kommen aus
`GameSurfaces`: vorhandene Tischplatten, der Teetisch im gemalten Wohnzimmer und
eine gemeinsam gezeichnete/kollidierte Kiste im Park (`GameSurfaceView`). Keine
zweite Reminder-, Persistenz- oder Weltpipeline. Die feinen Sprite-Boegen erhalten
Tempo/Gangphase und Aktionsfortschritt, ohne Bilder im Lauf neu zu erzeugen.
`CreatureSheets` zerlegt jeden Bogen einmal in gecachte 64-/128-px-Einzelbilder;
der 138er-Fennec-Streifen wird nie als 17664-px-GPU-Textur hochgeladen.

## Lebendige gemalte Raeume (06.10.2026)

`GameEnvironment` laeuft im bestehenden Steuerungstakt. Seine Uhr treibt einzeln
verankerte Bildteile; Bodenkontakte entstehen aus dem tatsaechlichen Weg und dem
Materialraster in Bildkoordinaten. Sprungboegen melden einen Kontakt beim Landen,
auch zwischen gepufferten Spruengen. Unterstuetzte Flaechen erzeugen keine
Wassereffekte durch den Boden hindurch. Spuren und Partikel sind kosmetischer
Sitzungszustand: 128 Kontakte maximal, Wasser 2,4 s, Staub 0,6 s, Gras 1,2 s,
Matsch 45 s und Sand 60 s. Ortswechsel behalten kurzlebige Spuren, Versetzen setzt
die Abtastung zurueck. Unsichtbare Bildschirme pausieren den Raumtakt.

`tools/world-art/living_layers.py` extrahiert alpha-maskierte Kronen, Gras,
Wolken, Vorhang, Flammen und ausschliesslich nasse Wasserstreifen aus den
vorhandenen Illustrationen. Je Ort: bereinigte Grundebene, gepackter RGBA-Atlas
(max. 1024 x 2048) und 480 x 270 Byte Materialraster; gemeinsamer generierter
`GameRoomCatalog`. Neutral zusammengesetzt sind die Bilder pixelgenau identisch
mit den Originalen. `build_all.py` zieht die Ebenen bei Bildaenderungen mit nach.
`GameRoomLayers` laedt sie einmal. Hintergrundteile, Brandung und Bodenspuren
liegen vor der bestehenden Tageslichtrechnung; vorderes Gras, Blaetter, Glut und
Tropfen kommen nach dem Avatar. Alle Ebenen benutzen `GameScenes.fit`; keine
Bitmap-Erzeugung, Farbsegmentierung oder Zufallsberechnung je Bildschirmbild.
Der bisherige Achtbildstreifen bleibt Rueckfall fuer Orte ohne neue Ebenen und
wird bei vollstaendigen Live-Ebenen nicht zusaetzlich dekodiert.

`GameSurfaces.painted` erweitert dieselbe Sprung-/Kollisionsrechnung um Sofa,
Bett, Baenke, Baumstaemme, Liegestuhl und Sumpfsteg. Oberkante und Hoehe bleiben
bei wechselnder Tiefe deckungsgleich. Die stationaeren Aktionsplaetze liegen
vor den Moebelkoerpern; der automatische Weg waehlt bei Bedarf kurze sichtbare
Kanten um deren Rechtecke. Strand und Sumpf erhalten erreichbare seichte
Uferbereiche. Das ist eine interaktive 2,5D-Illustration mit festen Requisiten;
keine frei drehbare 3D-Welt, schwimmenden Figuren oder verformbarer Bodenmesh.

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



### Individuelle feine Charakteranimationen (06.10.2026)

Ergaenzung 10.10.: Das beanstandete SWIM-Netz wird durch sechs eigene
Schwimmboegen ersetzt (vier gezeichnete Zugphasen, drei Ansichten, 256er Rahmen
mit 64 Pixeln Kopfanker-Rand). Im Schwimmen bleibt das Mesh identisch; vorhandene
Gang-/Stoffnetze gelten weiter an Land. `CreatureSheets.swimming` haelt die
zwoelf kleinen GPU-Texturen je Wesen separat, etwa 18 MiB zusaetzlich fuer sechs
Wesen. Der Game-Vorlader laedt beide Boegen je Wesen vor Game-ready.
Unterwasserteile zeichnen eine schwache blaue Lage an derselben Wasserlinie.
`GameWater.crest` berechnet Pixelkaemme in Weltkoordinaten; Licht-/Schattenhang,
Uferschaum und bewegungsabhaengige Spur ergaenzen die feste Wassermaske.
Details/Pruefung: `docs/itoeva2-swimming-water.md`. Elf neue gemalte Raumtafeln
sind Gestaltungsreferenzen, noch keine neue Runtime-Raumgeometrie.

Ergaenzung 09.10.: Im aktiven Game rendert `GameCharacterPainter` dieselben
Einzelbilder als 24x32-Texturnetz. `GameCharacterMotion` berechnet Statur,
Stoffnachlauf und kleine materialabhaengige Koerperbewegungen rein in Kotlin.
Ein analytischer zweistufiger Daempfer reagiert auf Gangtempo und lokale
Windrichtung. Eigene Lauf-Stauchung/Flugphase und Schwimmzuege ergaenzen die
Originalposen; der Motor koppelt Schrittlaenge und Fusskontakte an denselben Takt.
Die gemeinsame Bewegungstaktung, Originalbilder, ruhende Fussanker und
Aktionsrollen bleiben die Quellen. Vorder-/Rueckansicht haben eigene
Stoffgewichte; Schlaf und Rolle behalten ihre kompakte Zeichnung.
Netz- und Farbarrays sowie Paint werden pro Figur wiederverwendet. Die oberen
Alpha-Grenzen werden je Einzelbild einmal beim Laden ermittelt; keine
Bitmap-Kopie pro Bild und kein zweiter Texturcache. Vertexfarben interpolieren
das vorhandene Ortslicht, Bitmapfilterung mildert die harten Skalierungskanten.
App-1-/Stream-Zeichnung verwendet weiter den bisherigen Zeichenweg.

Alle sechs `CreatureSprites`-Boegen verwenden 138 Rollen zu 128 x 128. Fennec
bleibt in `rich_sheets.py`, die fuenf weiteren Wesen entstehen mit
`ensemble_motion.py` aus eigenen Zeichnungen und vorhandener Puppet-Interpolation.
Die Laufzeit waehlt dieselben MotionCue-, Richtungs- und Gangartrollen je Spezies;
keine weitere Animations-/Spielpipeline. Die individuelle Darstellungsbreite in
`Rich.scaleFor` kompensiert unterschiedliche Schwanz-/Fluegelanteile und behaelt
die bisherige Standhoehe. Android nutzt weiterhin den einmaligen Einzelbild-
Cache; keine Bitmap-Erzeugung pro Bild. Die rohe Bildmenge betraegt bei allen
sechs geladenen Boegen etwa 54 MiB, Geraete-Speicherprofil noch ungeprueft.
Quellen, exakte Prompts, Grenzen, Regeneration und Sichtvorschau:
`tools/character-art/README.md`, Abschnitt "Alle fuenf weiteren Wesen animiert".

Wyrmlings Folgepolitur verwendet `wyrmling_motion.py` mit 32 schlankeren
Bewegungszeichnungen und 16 Gelenk-/Lidzeichnungen. Gemeinsamer Importmassstab,
Kopf-/Vorderpfotenanker und eigene Sitzbilder ersetzen Koerperstauchung. Die
Laufzeit verwendet weiter dieselben 138 Rollen; Faktor 1.13 erhaelt seine
bisherige Weltgroesse bei hoeherer Rohbild-Standhoehe. Andere Charakterassets
blieben in diesem Schnitt unveraendert; Details und Sichtvergleich stehen im Character-Art-README.

Die Ensemble-Folgepolitur (07.10.2026) importiert Gloop, Puffling, Starlet und
Hootlet ueber `refined_motion.py`: je 32 Hauptzeichnungen und 16 Gelenk-/Lidposen,
64 Farben, gleiche 138 Bildrollen. Side-/Front-Standreferenzen kalibrieren jeweils
den Zusatzbogen; Sitzhoehen bestimmen nie den Massstab. Kopf- und Standflaechen-
anker vermeiden Taschen-/Schwanzversatz, nur registrierte Lidfenster blinzeln.
Gloop bleibt beinlos, Starlet bewegt seine fuenf Spitzen, Puffling faltet kurze
Beine, Hootlet seine Vogelgelenke. Die vier Weltfaktoren bewahren die Standhoehe.
Fennec korrigiert gezielt sechs Rollen: Profil-Sitzfolge samt Abschlussstand und
Freudeabschluss sowie Frontlider. Seine uebrigen 132 Rollen und Wyrmling bleiben
pixelgleich. Angeschnittene unbenutzte Quellbilder werden ausdruecklich nicht
importiert; aktive Bilder muessen die unveraenderte Beschnittpruefung bestehen.
Keine neue Laufzeitinterpolation, Steuerung oder Spielstandaenderung.

## Aktives Worldbuilding in Itoeva 2 (06.10.2026)

`GameAdventure` bündelt die reine Zustands-/Aktionslogik des aktiven Spiels. `GameSaveStore`
speichert einen versionierten Snapshot samt Legacy-Rucksack atomar, schützt unbekannte Versionen
und lässt dadurch keine halben Inventar-/Weltänderungen sichtbar werden. `DockScreen` koordiniert
Commit, Checkpoints und die pausierbare Spielzeit. Der Snapshot ist eine gemeinsame Kampagne
für die wählbaren Figuren, kein Reminder-Profil und keine neue Room-Tabelle.

`GameResidents` verwendet echte `LivingPopulation`-Snapshots, die vorhandene Geometrie und
Wegsuche. Der separate Population-Store `itoeva2_population` folgt der gespeicherten Spielzeit.
`GameAdventureUI` projiziert Requisiten, Chronik, Bewohner und Außenregen; NPCs werden nach
Tiefe hinter/vor der Hauptfigur gezeichnet. Licht, Musik und Atmo lesen dieselbe Spielzeit.
App 1 und Stream behalten reale Zeit und ihre bisherigen Persistenzpfade.

Die Abnahme und verbliebenen Ausbaupakete stehen in
[docs/itoeva2-worldbuilding.md](docs/itoeva2-worldbuilding.md).

### Itoeva 2: gemeinsame Landschaft und Kamera (07.10.2026)

`GameWorld` liefert nur dem Game-Client eine gemeinsame Panorama-Geometrie fuer Strasse, Park,
Wiese und Wald. Orts-IDs bleiben fuer Persistenz, Bewohner und Aktionen erhalten. `GameCamera`
rechnet pro Bildtakt eine Projektion fuer Hintergrund, Figur, Sprung, Schatten, Requisiten,
Bewohner und Trefferflaechen. Die vier Teilbilder werden nicht mehr gewechselt: ein gecachtes
Panorama steht hinter allen Abschnitten. Der alte `GameScenes`-Katalog bleibt unveraendert.

`GameMovement` behaelt Beschleunigung und Kollisionsphysik; bei einem gemeinsamen Rand wird nur
die lokale Position umgesetzt. Population-Snapshots bleiben die eine Bewohnerquelle, ein
profilbezogener Filter verhindert Doppelbilder beim Ortswechsel. Die neue Game-Karte folgt den
Game-Ausgaengen. Test- und Ruecksetzvertrag: `docs/itoeva2-continuous-world.md`.


### Begehbare Zwischenorte (08.10.2026)

`GameWorld` ergänzt Küstenweg, Dorfrand und Bergpass zwischen den vier bisherigen
Panoramen. Die drei `Place`-Werte stehen am Enum-Ende; bestehende IDs bleiben stabil.
Die Weltprojektion summiert Regionsbreiten, Karte/Routen enthalten die Zwischenorte.
Der Küstenweg besitzt ein `GameScenes.walkBand`: Füße und Touch-Umkehrprojektion
folgen derselben Brückenfläche. Leere Profile behalten die bisherige Geometrie.
Sechs nachgemalte Anschlussbilder überdecken je 960 Weltpixel rund um die Bildnähte.
Nur ihre äußeren 128 Weltpixel laufen in die ursprünglichen Panoramen aus; Kamera und
Boden bleiben ortsfest. Sieben Panoramen und sechs Anschlussbilder werden gemeinsam
geladen (rund 78 MiB dekodiert insgesamt, etwa 54 MiB mehr als vorher).
Android- und GPU-Abnahme sind getrennt von den reinen Kotlin-Tests.


### Game-Massstab und vermessene Moebel (08.10.2026)

GameCharacterScale rechnet die sichtbare Standhoehe statt der transparenten
Sprite-Rahmengroesse. Gemessene, feste Referenzen aller sechs Wesen ersetzen
GAME_FIGURE_FILL fuer den Game-Client; Spieler, NPCs und Trefferflaechen teilen
diese Rechnung. AvatarSpriteView und Rich.scaleFor bleiben fuer App 1/Stream
unveraendert. Game-Fussanker werden am oberen Bildschirmrand nicht geklemmt.

GameFurniture liefert gemeinsame Plattformhoehen und Konturen fuer Bank, Sofa,
Teetisch, Bett sowie die beiden grossen Innenraumtische. gameCharacterOcclusion
schneidet pro Figur deren verdeckte Teile aus, statt das Moebel nach allen NPCs
erneut ueber das Bild zu malen. Der Wasser-Ausschnitt steht weiterhin nach dem
Offset. GameMovement erhaelt die hoehere Sprungreichweite nur vom Game-Client.
Pruefung, Bilder und Fortsetzung: docs/itoeva2-character-scale.md.

### Game: ortsfestes Licht und Umgebungsbewegung (08.10.2026)

GameLightingCatalog haelt die normierten Fenster-/Lampenanker der elf Innenbilder.
GameSceneLighting liefert in derselben Geometrie absolute Aussenquellen, lokale
Fenster-/Feuer-/Kristalllichter, weiche gerichtete Schatten und vier Farbwerte pro
Figur. Innenraum-Moebel begrenzen niedrige Strahlen. Abdunkelung, Daemmerungsfarbton und Sonnenfilter am Hoehleneintritt werden raeumlich interpoliert, nicht am Ortsnamen geschaltet.
GameLightingView zeichnet Strahlen, Nachtfenster und Schatten; alle Figuren
verwenden dieselbe Rechnung und ihre eigene Fuss-/Plattformprojektion.

AvatarSpriteView erhaelt die Game-Farbfilter optional, mit unveraendertem Alpha.
Ein Blickwechsel kehrt die physischen Farbseiten nicht um. Im Stand werden die
36 Filter pro Figur wiederverwendet. Andere Clients behalten ihren bisherigen Pfad.
GameAtmosphere und GameAtmosphereView bewegen kleine vorhandene Bildausschnitte,
Sprite-Spitzen und ortsgebundene Schatten/Reflexe/Staub/Dunst mit der Game-Zeit;
keine Textur-Neuerzeugung, Netzwerk- oder Persistenzarbeit im Zeichentakt.
Pruefvertrag und Cloud-Code-Uebergabe: docs/itoeva2-light-and-environment.md.

### Game: Weltkonturen, Sitzpose und gemeinsame Erinnerungen (08.10.2026)

GameFurniture erweitert die elf Innenbilder um feste Koerper und Sitzflaechen. GameSeating
trennt Anlaufpunkt, gerenderte Sitzposition, Fusslift und Schattenempfaenger; dieselbe Pose
bestimmt NPC-Tap und Tiefensortierung. Die Haltephase laesst Aktivitaetsanimationen durch.
GameWorldShadows verbindet grosse Kulissenanker mit Sonne und Figurenabschattung. GameGroundLight
berechnet einmalig ein begrenztes Boden-Helligkeitsfeld im Hintergrund; GameFabric bewegt
bestehende Stoff-/Feder-/Blattdetails und Vorhaenge, ohne weitere Bilder zu erzeugen.

GameEncounters verwendet den bestehenden Action-/Episode-/Relationship-Kern. Besitz beider
Seiten und beide Erinnerungen sind in einem GameAdventure-V2-Snapshot atomar gespeichert;
der Bewohneranteil wird in die vorhandene Population projiziert. GAME_* sind abgeschlossene
Spielerbegegnungen, keine autonomen Kandidaten. V1 wird erhalten und migriert; alte APKs lesen
V2 nicht, deshalb muss ein Rueckbau den V2-Reader behalten. Room, App 1, Stream und Reminder
behalten ihre bestehenden Pfade. Pruef- und Ruecksetzvertrag:
docs/itoeva2-world-polish-and-encounters.md.

### Game: Physik und Asset-Vorbereitung (09.10.2026)

GameTerrain begrenzt den bestehenden GameMovement-Motor auf den gemalten Boden und aktiviert
im Game-Client durchlaufene Sprungkollision. Der Kuestenvordergrund ist seit dem Auftrag vom 10.10. trocken; Schwimmassets,
Wasserverformung, Schwimmpose und Auftrieb wurden entfernt. Kontakte
folgen wirklicher Bewegung. GameDoors verbindet gemalte Oeffnung, Treffer, Anlauf und
420/320-ms-Tuerbewegung; gemeinsame Flurtueren verwenden eine Auswahl oberhalb der Touchsteuerung.
Bewegte Tuerblaetter teilen mit GameAtmosphere die tatsaechlich sichtbare Bildquelle samt
Naht-Offset; das verdeckte Regionsbild wird an deckenden Anschluessen nicht eingesetzt.

GameAssetPlan/Loader laden die 24 Welt-/Anschluss-/Innenbilder und sechs Figuren-Sheets vor
dem ersten sichtbaren Spielrahmen ausserhalb des UI-Threads. GameLoadingScreen ist deckend;
der erste Kamerarahmen entsteht unabhaengig von Menuepausen. Zusaetzliches Bodenlicht folgt
in 32-Zeilen-Bloecken im Stand. Fertige Kopien ersetzen unveraenderte sichtbare Bitmaps;
Rohreferenzen werden schrittweise freigegeben, fertige Bilder nutzen einen weichen Cache.
GameBreath ist nur lokale Spielausdauer ohne Persistenz/Agentenbedarf/XP-Wirkung. Ortswechsel
haben kein neues Asset-IO; die Laufpause wird nie an den Ladefortschritt gekoppelt.
V2 liest alte Positionen unveraendert, der aktive Client begrenzt sie auf den legalen Boden.
Pruefvertrag, Scan aller 28 Orte und Telefon-Abnahme: docs/itoeva2-world-physics-and-loading.md.


### Game-Raumkoerper und Medienwiedergabe (09.10.2026)

Die elf Innenraeume zeichnen ihre warmen Original-PNGs; `GameRoomArt` und dessen
Rasterung bleiben ein Fallback. `GameFurniture` ist Quelle von bildgebundenem
Grundriss, Sitz-/Stand-/Landekante und Silhouetten fuer Verdeckung/Licht. Keine
Umformung in prozedurale Raumkoerper verschiebt die Kontakte gegen die Malerei.
Tuerbewegung liest den gemalten Ausschnitt, Vorhaenge/Pflanzen und Ortslicht bleiben
animiert. Die Innenraumkamera vergroessert um 1,12 und schneidet leeren Vorderboden ab.
`GameWalkingMap` teilt unsichtbare Bodenprofile/Sperr-/Graspolygone zwischen
`GameTerrain`, `GameSurfaces` und Kontaktzeichnung. Der Anlauf benutzt Korridorknoten
und Sperrinsel-Ecken; Bodenpfade werden durchlaufen, Moebel analytisch entlang des
Schritts geprueft. Vorhandene Bruecken- und Stegkoerper bleiben Grundlage.
Der Kuestenlaufboden endet vor dem ehemaligen Schwimmterritorium; Gras reagiert auf abklingende Fusskontakte und
verdeckt die Pfoten. Details: `docs/itoeva2-painted-interiors-walking-map.md`.

Rechte Padbefehle liegen bis zum naechsten Motorbild vor und werden nach dem
Positionsabgleich in `GameTerrain.tick` verbraucht. `PlayMusic` erstellt den
nativen Player mit Medienattributen vor der Vorbereitung; Game-Musik folgt der
Medienlautstaerke statt der Klingelsperre. Bestehende Musik-Aus-/Fremdton-Regeln
sowie App-1-/Stream-Sperren bleiben bestehen. Details und Pruefgrenzen stehen in
`docs/itoeva2-character-motion.md`.
