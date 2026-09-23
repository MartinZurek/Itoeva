# Uebergabe: Stand am 23. September 2026

Diese Datei ist fuer den, der als Naechstes weitermacht - Mensch oder Agent, ausdruecklich auch
ein anderes Modell als das, das sie geschrieben hat. Sie ersetzt nicht
[`CLOUD_CODE_BRIEFING.md`](CLOUD_CODE_BRIEFING.md) (Produktvision) und nicht
[`evolutions/BACKLOG.md`](evolutions/BACKLOG.md) (die Arbeitsliste), sondern sagt, **wo genau der
Faden liegt** und **welche Fallen auf dem Weg dahin schon zugeschnappt sind**.

## 1. Der offene Faden: Living Agent System und Darstellung

**Stand 18.09.: NT-091 und NT-093 (Basketball) sowie ein zweites, unabhaengig ebenfalls
"NT-092" genanntes Training-Erinnerungs-Feature sind gemergt - siehe die Nummernkollision am
Ende dieses Abschnitts.** Die drei Einwohner leben ueber
`LivingPopulation` an der simulierten Uhr weiter und mehrere wirklich anwesende Wesen koennen
zugleich in der Pixelwelt stehen. `LivingPopulationLayout` liest nur
`ResidentSnapshot.publiclyPresent`, zeigt hoechstens zwei kleinere Hintergrundfiguren auf freien
festen Bahnen und verwendet ihren jeweils eigenen `minuteOfDay` als Versatz der Ruhebewegung. Ein
aktiver Besuch wird aus derselben Anwesenheitsliste gewaehlt und nicht als zweite Kopie gezeichnet.
Bildschirm, Schnappschuss und Clip teilen dieselbe relative Figurenbeschreibung. Wenn am SPORT
die vorbereitete Hauptaktion wirklich `MOVE_BODY` abschliesst, die gewaehlte Routine `TRAINING`
oder seit NT-093 auch `BASKETBALL` ist und ein dort wirklich anwesender Einwohner selbst
ungehindert `MOVE_BODY` als naechsten Schritt UND dieselbe konkrete Sonderaktivitaet ueber sein
eigenes `nextSpecialActivity` traegt, bewegt er sich sichtbar mit.
Fortschreibung und Besuch pausieren fuer dieses Profil; erst nach dem vollstaendigen Bild werden
beide Living-Zustaende ueber ihre vorhandenen Wirkungswege in einer gemeinsamen
SharedPreferences-Transaktion gespeichert. Abbruch oder Prozessende verbucht nicht nur einen.
Bildschirm, Schnappschuss und Clip teilen auch diese Einwohnerbewegung.

**Was NT-092 geprueft und zunaechst zurueckgenommen hat.** Der Auftrag verlangte zuerst zu pruefen,
ob im Modell schon eine weitere Aktivitaet existiert, die aus dem wirklichen Zustand BEIDER
Beteiligter unterscheidbar ist - nicht sofort eine neue Szene zu bauen. Ein erster Entwurf in PR
#160 erweiterte `sharedTrainingPartner` (umbenannt zu `sharedSportPartner`) um `BASKETBALL`, mit
dem Argument, TRAINING sei in NT-091 ja auch nur ueber die bereits gewaehlte, wirklich rendernde
Hauptavatar-Routine unterschieden worden. **Das war ein Fehler**, den ein automatisches Review
(Codex, P1) aufgedeckt hat: Auf der Einwohner-Seite blieb die Bedingung fuer TRAINING und
BASKETBALL identisch (`MOVE_BODY`, unblockiert, am selben Ort) - sie haette ebenso Fussball,
Drachen oder Angeln "belegt". Der Entwurf wurde deshalb zunaechst vollstaendig zurueckgenommen.
Siehe `EVOLUTION.md`, Eintrag vom 16.09. zu NT-092, fuer die volle Herleitung.

**NT-093 schliesst genau diese Luecke.** `LivingPopulation.specialActivityFor(resident, world)`
leitet deterministisch aus Einwohner-Index und Simulationstag her, ob ein geplantes `MOVE_BODY`
`TRAINING` oder `BASKETBALL` meint - nach demselben Muster wie `interestFor`s Themenrotation,
nie per Rolle allein und nie gewuerfelt. `ResidentSnapshot.nextSpecialActivity` traegt diesen
Wert; `sharedSportPartner` verlangt seitdem zusaetzlich, dass er mit der Aktivitaet des
Hauptavatars uebereinstimmt. Beide Seiten leiten ihre konkrete Absicht damit unabhaengig
voneinander her - eine gemeinsame Szene entsteht nur bei echter Koinzidenz, nicht mehr aus einem
generischen `MOVE_BODY`, das "irgendetwas" bedeuten koennte. Nur zwei Werte, weil TRAINING und
BASKETBALL sich den Ort SPORT teilen; Drachen (PARK) und Angeln (POND) braeuchten eine eigene
Ortszuordnung, Fussball einen zusaetzlichen, rein hauptavatarbezogenen Zustand (den gelernten
Trick) - alle drei bleiben bewusst aussen vor.

**Ein zweites, unabhaengig ebenfalls "NT-092" genanntes Feature: gemeinsames Training wird
gemeinsame Erinnerung.** Parallel zu NT-093 hat ein Codex-Auftrag (PR #163) dieselbe Ticketnummer
fuer eine andere Erweiterung vergeben: Nach einem vollstaendig sichtbaren gemeinsamen Training
behalten jetzt beide Profile das Gegenueber im Gedaechtnis. `TRAIN_TOGETHER` erzeugt je eine
typisierte positive Episode und 0,02 Naehe durch den vorhandenen `ActionOutcome`-Weg. Die
physische Wirkung bleibt genau einmal `MOVE_BODY`. Scheitert der Bewegungsabschluss auf einer
Seite oder wird die Szene abgebrochen, entsteht keine soziale Spur. Beide angereicherten
Zustaende bleiben Teil derselben atomaren `LivingAgentStore.saveAll`-Transaktion. Ein
Codex-Review (P2) fand dabei eine Zeitstempel-Desynchronisation - Hauptavatar und Einwohner
koennen bis zu diesem Aufruf unabhaengig voneinander vorgerueckte Uhren haben, wurde per
`maxOf(...)` + `.advanced(...)` behoben (Muster wie in `exchangePlayInvitation`), mit einer
Regressionstest-Absicherung in `LivingAgentStoreTest`.

**Offen insgesamt:** Fussball, Drachen und Angeln lassen sich weiterhin nicht anschliessen, ohne
entweder eine neue Ortszuordnung oder eine Loesung fuer hauptavatarbezogenen Zusatzzustand zu
bauen - ausdruecklich keine allgemeine Aktivitaets- oder Skillplattform. Die Lesbarkeit von
Training UND Basketball gemeinsam bei vierzig Zellen, und die Lesbarkeit der neuen
Trainingserinnerung, sind am Geraet weiterhin `UNVERIFIED`. Danach ist eine deterministische
Rotation der Trainingspartner der naechste kleine soziale Schnitt.

PR #160 (`claude/itoeva-shared-activity-kwlk4h`, der urspruengliche, zurueckgenommene
NT-092-Basketball-Versuch) und PR #163 (`codex/shared-training-memory`) sind inzwischen gemergt -
die obige Nummernkollision ist damit dauerhaft im Verlauf und wird hier nur noch dokumentiert,
nicht mehr aufgeloest.


Die sechs Charakter-Prompts und Manifest-Eintraege ITO-0017 bis ITO-0022 sind gemergt. Die
eigentliche Audioerzeugung bleibt bewusst manuell: ein Wesen pro Workflow-Lauf, danach hoeren und
erst dann einen Asset-PR oeffnen.

**Musik-Warnung vom 23.09.: PR #194 bis #197 niemals mergen.** Die vier dortigen Takes
(`home-evening-01`, `main-day-01`, `sport-02`, `morning-02`) wurden mit `steps=50` und
`cfg_scale=5` erzeugt und klingen laut Geraete-Hoertest wie verzerrtes Rauschen. Das ist kein
kaputter Android-Player und keine durch Pegelabsenkung heilbare Vorbis-Spitze: `small-music` ist
in der gepinnten Stable-Audio-3-Runtime ein post-trainierter Checkpoint mit dokumentiertem
Inferenzmodus 8/1. `generate_music.py` lehnt andere Werte nun bereits im Dry-Run ab; alle
Manifest-Eintraege stehen wieder auf 8/1. Prompts und Seeds der Neukompositionen bleiben erhalten.
Nach Merge dieses Schutzes die vier Tracks einzeln neu erzeugen und jeden resultierenden PR
erneut hoeren. Auch `home-evening-02`, dessen noch nicht gehoerter Eintrag zwischenzeitlich auf
32/4 stand, ist vorsorglich auf 8/1 zurueckgesetzt.

**Charakter-Themes, Stand 23.09. abends:** Nur Gloops Thema gefaellt. Ein Mess-Audit aller sechs
Takes (EVOLUTION.md, Eintrag "Audit der sechs Charakter-Themes") fand keine Pegel- oder
Encoding-Fehler, aber verfehlte Kernvorgaben und bei vier Takes ein mehrsekuendiges Ausblenden vor
dem Loop-Punkt. Als Pilot ist nur `theme-hootlet.txt` neu gefasst (Seed und 8/1 unveraendert). Der
naechste Schritt ist genau ein manueller Lauf von **Generate Itoeva Music** mit `theme-hootlet`,
danach Hoertest und Messvergleich - erst dann die uebrigen vier einzeln angehen.

**Musik-Engine, Stand 23.09. nachts:** Gaeste spielen beim Hereinkommen ihr Thema kurz an
(`PlayMusicCue`), Rollenwechsel bestaetigen sich 10 s (`PlayMusicTransition`). Liegt vorerst nur auf
dem Hoertest-Branch (PR #213), weil der Nutzer beides zusammen am Geraet hoeren soll. Der neue
Starlet-Prompt liegt ebenfalls dort und muss fuer einen Generierungslauf erst auf `main`
(`generate-music.yml` liest nur `main`). `home-evening-02` wurde mit 8/1 neu angestossen - die
groesste Luecke (Abend + Nacht auf einem einzigen Stueck).

Der neue ausdruecklich freigegebene Hauptauftrag steht in
[`LIVING_AGENT.md`](LIVING_AGENT.md). **NT-063, NT-067, NT-064, NT-065 und NT-066 (Schnitte 2a,
2b, 3, 4 und 5) sind umgesetzt.** Der reine Kotlin-Kern steht weiterhin in genau fuenf Dateien unter
`app-sim/src/main/java/com/notime/glyphsim/living/`. Zielwahl, Ressourcenplan, Neuplanung,
begrenzte Episoden, gelernte Zielpraeferenzen, Beziehungen und symbolische Verstaendigung sind
deterministisch belegt.

Die Persistenz liegt bewusst ausserhalb des Kerns unter `app-sim/.../data/LivingAgentStore.kt`.
Sie schreibt einen atomaren Version-2-Snapshot je `profileId` in SharedPreferences. Agent,
Weltressourcen, Erinnerungen, Beziehungen und gelernter Geschmack bleiben getrennt pro Profil.
Version 1 wird beim Lesen angehoben; unbekannte Zukunftsversionen werden abgelehnt. Der
Wiedereinstieg bekommt die Simulationsminute explizit, behaelt das Ziel und verwirft den alten
Plan, damit aktuelle Orte und Nachbarn neu geprueft werden. Room und `:core` blieben unangetastet.

`LivingRuntimeAdapter` bildet die sechzehn sichtbaren Orte auf die vier Domaenenorte ab,
uebergibt Oeffnungszeiten und waehlt fuer Kernhandlungen vorhandene `PlayRoutine`-Choreografie.
Der harte `mustEarn`-Sonderfall in `DockScreen` ist entfernt. Kernwirkungen werden erst nach
einem vollstaendigen sichtbaren Ablauf gespeichert; ein Abbruch durch eine echte Erinnerung
verbucht nichts vorzeitig. Bestehende `PlayWallet`-/`PlayPantry`-Werte werden beim ersten
Anschluss als Startwert uebernommen, danach sind die Ressourcen profilbezogen. Erbetene Arbeit
und Einkaeufe laufen durch dieselbe Wirtschaft, das Gespraech stellt sie schon vor der ersten
autonomen Handlung wieder her, und zusammengefasste Ablaufe pruefen Oeffnungszeiten nach jedem
fortgeschrittenen Kernschritt erneut.

Der read-only Vertrag liegt ausserhalb des Kerns unter
`app-sim/.../stream/LivingObservationSource.kt`. Er liefert pro Profil einen flachen,
sprachunabhaengigen Snapshot und hoechstens 32 wichtige juengste Ereignisse; `IDLE`-Ticks werden
nicht exportiert. `DockScreen` speist ihn nur nach Wiederherstellung oder einem abgeschlossenen
Runtime-Schritt. Der Vier-Tage-Test laeuft ueber `LivingRuntimeAdapter` und belegt deterministisch
Hindernis, Neuplanung, Arbeit, Einkauf und Essen.

NT-068 setzt darauf den lokalen Stream-Client-PoC. Es gibt bewusst kein zweites Spiel und kein
neues Anwendungsmodul: `:app-sim:assembleStream` baut dieselbe Runtime mit isolierter
`applicationId` als debug-signierte APK. Die vorhandene Play-Mode-Erinnerung belegt dort den
ersten freien der vier bestehenden Save-Slots automatisch. Ein Tippen simuliert die spaetere
Viewer-Auswahl und erzeugt einen Twitch-neutralen `ExternalImpulse`. Dessen `GoalInfluence`
bleibt unter dem Mindestdruck eines Grundbeduerfnisses; der Slot wird erst nach einer passenden,
vollstaendig sichtbaren Agentenhandlung geleert. Medizin und frei beschriftete Inhalte gelangen
nicht in den Stream-Pfad. Netzwerk, OAuth, Twitch, Bits, Subs und Backend fehlen absichtlich.

Naechster Schritt ist **NT-058**: die Stream-APK im Emulator mindestens zwei
Stunden lokal mit OBS beobachten und die Ergebnisse in `docs/streaming-poc.md` festhalten. Das
ist Beobachtung, keine Freigabe fuer Cloud oder echte Twitch-/YouTube-Anbindung. Gebaut wird mit
`./gradlew :app-sim:assembleStream`; der regulaere Pfad ist
`app-sim/build/outputs/apk/stream/app-sim-stream.apk`.

Die Stream-APK entsteht seit dem 11.09. **automatisch bei jedem Merge** (`deliver-apk.yml`
baut `assembleDebug` und `assembleStream` in einem Aufruf, weist fuer beide dasselbe Zertifikat
nach und legt beide als Artefakt ab). Nach Drive geht sie nur, wenn die Variable
`GDRIVE_STREAM_APK_FILE_ID` auf eine eigene, dem Service-Konto freigegebene Datei zeigt -
getrennt nachreichbar, damit ein fehlender Stream-Weg die gewohnte Tama-Ablieferung nicht
anhaelt.

**NT-058 braucht einen Menschen und ist von einer Agenten-Sitzung aus nicht zu erledigen.** Am
2026-09-11 ist der Versuch daran gescheitert, dass in der Agentenumgebung weder Android SDK noch
Android Gradle Plugin, Emulator, `adb`, `/dev/kvm` oder OBS existieren - `assembleStream` bricht
schon beim Aufloesen des Plugins ab. Erfundene Messwerte waeren leicht gewesen und waren keine
Option; alle Felder in `docs/streaming-poc.md` stehen weiterhin auf `TODO`.

Vorbereitet ist dafuer alles Uebrige: Das Runbook zeigt jetzt auf die Stream-Variante statt auf
die normale App (es tat das vorher NICHT - siehe Abschnitt 4), hat einen eigenen Messbogen fuer
Slot-Belegung, Viewer-Impulse, Einfluss-statt-Steuerung, Erklaerbarkeit und Datenschutz, und
`StreamRunbookTest` haelt Runbook und `app-sim/build.gradle.kts` ab jetzt zusammen.

Seitdem sind NT-069 bis NT-074 auch sichtbar in die Welt geflossen: Wunsch und Hindernis stehen
symbolisch ueber dem Kopf, jede Reminder-Art hat eine eigene Kernwirkung, Draussen ist ein Ziel,
alle sieben Beduerfnisse koennen Entscheidungen tragen und die Stimmung beruecksichtigt das
Wohlbefinden. NT-075 bis NT-083 bearbeiten die anschliessend gemessene Darstellung. Der juengste
Schnitt ist der sichere Schlafrueckblick: Bei jedem echten Schlaf oeffnet sich die Traumblase,
geht in die vergroesserte Watch ueber und zeigt bis zu drei tatsaechliche Tageserlebnisse. Die
seit NT-073 vorgesehenen Tagtraeume auf Sofa und Bank sind dabei erstmals wirklich sichtbar.
NT-084 schliesst danach die sichtbare Reminder-Abdeckung: Alle Reminder animierten den Avatar,
aber 29 Bibliotheksmotive erbten noch die Antwort eines Geschwisters. Nun stehen fuer 80
Motivknoten 80 verschiedene Reaktionen; Katalog-, Fingerabdruck- und Bildgleichheitstest halten
das fest. Details und die drei wiederkehrenden Darstellungsfallen stehen in `LIVING_AGENT.md`
und bei NT-075 bis NT-084 in `NextTasks.md`.

NT-090 schliesst den naechsten dort bereits gemessenen Animationshebel: Buch, Becher, Essen,
Gitarre und Staffelei gleiten beim Tragen nicht mehr starr mit. Sie schwingen nur waehrend eines
Gangs in einem ruhigen Sechsertakt zwischen Grundstellung und zwei angehobenen Hoehen; im Stand
bleibt das Motiv still. Bildschirm und Clip leiten Bewegung aus derselben Laufrichtung ab. Ein
Test haelt alle fuenf Gegenstaende auf drei Ganghoehen, genau einem Standbild und oberhalb der
ruhigen Unterkante fest, damit ihre Freistellung nie die Bodenlinie uebermalt.

NT-085 schliesst die bis dahin wichtigste soziale Laufzeitluecke: Der Kern konnte seit NT-067
zustandsabhaengig auf `PLAY + QUESTION` antworten, aber keine Produktionsstelle rief diese
Funktionen auf. `runVisit` zeigte stattdessen immer drei feste Pseudo-Wortwechsel. Jetzt fuehrt
`LivingSimulation.exchangePlayInvitation` Einladung, Antwort und Wahrnehmung auf einer
gemeinsamen Simulationszeit aus. Der Bewohner antwortet aus Energie, sozialem Bedarf, Beziehung,
Persoenlichkeit und aktuellem Ziel; die zwei echten Nachrichten erscheinen als Pixelsymbole
ueber dem jeweiligen Sprecher. Ja besitzt deshalb nun wie Nein ein eigenes Motiv. Beziehung und
Episode des Bewohners werden ueber den vorhandenen Store gesichert.

NT-086 ersetzt diesen verworfenen Gast inzwischen durch drei dauerhafte, nicht waehlbare
Identitaeten: Verkaufskraft im SHOP, Parkstammgast und Sportler. Ihre `resident:`-Profil-IDs sind
von den sechs waehlbaren Speziesprofilen getrennt; Rolle, Ankerort und Anwesenheitsfenster waehlen
deterministisch den passenden Gast. `LivingAgentStore` wird unveraendert je Einwohner
wiederverwendet. Beim Austausch bleiben die Weltressourcen beider Profile getrennt, beide
Zeitstaende werden vorwaerts synchronisiert und nach der vollstaendig sichtbaren Begegnung werden
beide Agenten gespeichert.

**Die Architekturfrage ist entschieden - und zwar gegen meine eigene Vermutung (NT-087).** Hier
stand, die Huerde liege im Ortsmodell. Die Messung sagt etwas anderes: Sechs unabhaengig
entscheidende Wesen ergaben **1 662 Begegnungsgelegenheiten**, **kein Paar** blieb ohne, **12 von
16** sichtbaren Orten kamen vor, und in **1 von 720** Schritten wich der Kern vom sichtbaren Ort
ab. Nicht der Ort platziert ein Wesen, sondern das Thema - `PlayScene.forTopic` fuehrt zwei
Wesen, die dasselbe tun wollen, schon heute an denselben Ort. **`LivingSite` bleibt bei vier.**

Dieselbe Messung fand dafuer zwei Ziele, die NIE gewinnen. `SEEK_COMFORT` ist behoben
(Behaglichkeit stillt nur noch, was ihr gilt - siehe NT-087); `EARN_MONEY` bleibt bewusst
ungewaehlt, weil Muenzen heute nur Essen zahlen. Verkauf und Lohn sind der konkrete Anlass, dem
Ziel erstmals einen Sinn zu geben; die Auswahlzahl wird dafuer nicht vorab kuenstlich erhoeht.

**Der erste gemeinsame soziale Schnitt ist mit NT-091 geschlossen; NT-092 fand und verwarf einen
unehrlichen Versuch, ihn auf Basketball auszuweiten, NT-093 hat dieselbe Erweiterung danach mit
einem echten Einwohner-Signal richtig gebaut:** Kompatibles `MOVE_BODY` zweier wirklich
anwesender Wesen wird jetzt fuer die bereits gewaehlte TRAINING- ODER BASKETBALL-Routine
gemeinsam lesbar - aber nur, wenn der Einwohner ueber sein eigenes `nextSpecialActivity`
dieselbe konkrete Aktivitaet auch wirklich meint (siehe Abschnitt 1). Die offene Huerde fuer
weitere gemeinsame Faehigkeiten bleibt die Bedeutung der Handlung im Kern selbst - Fussball,
Drachen und Angeln brauchen entweder eine eigene Ortszuordnung oder eine Loesung fuer
hauptavatarbezogenen Zusatzzustand. Danach folgt erst die Choreografie auf vierzig Zellen, nicht
das Ortsmodell; neue Orte kommen erst, wenn die vorhandenen Plaetze wirklich bewohnt sind. Ein
zweites, unabhaengig ebenfalls "NT-092" genanntes Feature ist seitdem dazugekommen: Ein
vollstaendig sichtbares gemeinsames Training haelt jetzt auch bei BEIDEN Profilen als
`TRAIN_TOGETHER`-Episode mit geringer Naehe fest (siehe Abschnitt 1 fuer die Nummernkollision).

Vier Dinge, die man beim Weiterbauen wissen muss:

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
- **`LivingSite` hat vier Werte, `PlayScene.Place` hat sechzehn.** Die Abbildung steht allein in
  `LivingRuntimeAdapter`, nicht in der Domaene. `DockScreen.kt` niemals als Ganzes lesen; NT-065
  hat nur die Ablaufgrenze und den bisherigen Notfallzweig gezielt geaendert.
- **`ActionKind` liegt als Name in den Version-2-Episoden.** Ein funktionaler Revert von NT-092
  darf `TRAIN_TOGETHER` nicht aus dem Enum entfernen, solange damit gespeicherte Snapshots
  existieren; nur Erzeugung und Laufzeitanbindung werden zurueckgenommen.

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
bash tools/reaction-preview/tests.sh          # derzeit 465 Tests, ~2 s
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

- **Ein Runbook rostet stiller als Code.** `docs/streaming-poc.md` schickte nach dem Bau des
  Stream-Clients weiterhin zu `installDebug` und zur normalen `applicationId`. Nichts war rot,
  niemand haette es bemerkt - erst nach zwei aufgezeichneten Stunden waere aufgefallen, dass die
  falsche App gemessen wurde. Wo ein Dokument konkrete Befehle oder Bezeichner nennt, gehoert
  ein Test daneben, der sie gegen die Quelle haelt (`StreamRunbookTest`).

- **Zwei verschiedene Pixelraster teilen sich den Typ `IntArray`.** `AvatarGeometry` (16x20,
  Kreatur-Sprites) und `MatrixGeometry` (13x13, Uhr/Zeichen/Bibliotheksmotive) sehen fuer den
  Kotlin-Compiler identisch aus - beides ist einfach `IntArray`. `AvatarSpriteView` und
  `SimulatedMatrixView` lesen sie mit unterschiedlicher Zeilenbreite; vertauscht man sie, gibt
  es keine Ausnahme und keinen Absturz, nur ein Bild ohne Bezug zur eigentlichen Pose
  ("Pixelsalat", gemeldet 2026-09-16 am Traumrueckblick - siehe EVOLUTION.md). Schon einmal in
  die andere Richtung passiert (13x13-Zeichen liefen bis NT-079 durch `AvatarSpriteView`, siehe
  dessen KDoc) und jetzt ein zweites Mal andersherum. `CreatureFrameSizeTest` haelt die
  Groessendifferenz (320 vs. 169) fest, kann die Verwechslung selbst aber nicht erkennen - wer
  einen neuen `SimulatedMatrixView`- oder `AvatarSpriteView`-Aufruf schreibt, muss von Hand
  pruefen, aus welchem der beiden Raster das `frame` wirklich stammt.

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
  passt; ausserdem Tempo und Lesbarkeit des Schlafrueckblicks sowie der 29 neuen motiveigenen
  Reminderreaktionen.
- **Die 86 Sekunden Begruessung** aus ITO-0023 lassen sich erst beurteilen, wenn das erste der
  sechs Stuecke erzeugt ist.
