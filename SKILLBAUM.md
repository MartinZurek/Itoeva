# Skillbaum: Umbau der Animationen

Arbeitsliste für den Umbau vom flachen Bestand (68 Motive) zu einem dreistufigen Baum, in dem
jede auf den Avatar gezogene Animation eine passende Reaktion auslöst und neue Zweige über Level
freigeschaltet werden.

**Diese Datei ist die maßgebliche Quelle und steht für sich.** Es gibt zwei bebilderte Fassungen
davon — [Bestandsaufnahme](https://claude.ai/code/artifact/72c09cf7-645d-4c7b-abe8-7b10b4408859)
und [Umbauplan](https://claude.ai/code/artifact/c782d88e-e80f-4722-bad5-3408a64e8ba9) — die sind
aber nur zum Ansehen und nur mit dem Konto von Martin erreichbar. Zum Arbeiten wird nichts davon
gebraucht; alles Nötige steht hier.

---

## So arbeiten wir

**Dieses Dokument ersetzt die Erkundung.** Alles, was einmal herausgefunden wurde, steht hier —
Zeilennummern, Zuordnungen, Prüfbefehle. Eine neue Sitzung liest den Kopf bis „Arbeitspakete" und
fängt an. Sie durchsucht nicht noch einmal den Code nach Dingen, die unten schon stehen.

1. **Nie eine ganze Datei öffnen, für die unten ein Zeilenbereich steht.** Die Anker im nächsten
   Abschnitt sind geprüft. `PlayScene.kt` hat 3.672 Zeilen, gebraucht werden davon zwei Enums.
2. **Ein Arbeitspaket ist eine Sitzung.** Jedes Paket nennt, was zu öffnen ist, was ausdrücklich
   nicht, und womit geprüft wird. Kein Paket braucht ein anderes offen.
3. **Prüfen per Test oder Grep, nicht per Lesen.** Ob ein Umbau stimmt, beantwortet ein Testlauf
   in Sekunden — eine Datei erneut zu lesen beantwortet es nicht.
4. **Pakete sind nach Datei gebündelt.** Wer `AvatarAnimations.kt` öffnet, erledigt in derselben
   Sitzung alles, was dort ansteht.
5. **Am Ende jeder Sitzung:** Haken setzen, und wenn sich eine Zeilennummer verschoben hat, den
   Anker unten korrigieren. Ein falscher Anker kostet die nächste Sitzung mehr als das Nachtragen.
6. **Das Journal unten fortschreiben.** Zwei Zeilen je Sitzung. Das ist billiger als ein `git log`
   zu lesen und zu deuten.

---

## Kontext-Anker

### Bereits festgestellt — nicht neu herleiten

- Der Bestand sind **68 Motive**: 12 `AnimationType`, 26 allgemeine, 30 charakterspezifische.
- **MEDICINE bleibt außerhalb des Baums.** Es ist eine reine Erinnerungsfunktion und darf nie
  Spiel-Knoten sein (Regel steht schon in `PlayGamePlan`). Damit sind **67 Motive** einzusortieren.
- **Das Ziehen auf den Avatar existiert.** Kollisionsprüfung läuft während der Geste.
- **XP/Level existiert.** Level wird aus `xp` abgeleitet, nicht gespeichert.
- **Das Nutzungs-Log existiert.** `avatar_feed_events` schreibt jede Auslösung mit Thema,
  Profil und Antwortzeitpunkt mit — auch die unbeantworteten.
- **Reaktionen sind parametrisch.** `creatureFrame(...)` baut jedes Bild aus dem Körper der
  Spezies. Neue Reaktion = Liste von Beats, kein neuer Sprite, gilt sofort für alle sechs Spezies.
- **Es gibt ZWEI Datenbanken.** `:app-sim` (jetzt Version 23) und `:app` (jetzt Version 20), beide
  mit `exportSchema = true`. **Jede Änderung an einer Entity in `:core` trifft beide** — Room
  vergleicht beim Öffnen das ganze Schema, nicht nur die benutzten Spalten. Wer nur eine Seite
  migriert, baut einen Absturz beim Start der anderen App. Das ist in diesem Repo schon einmal
  passiert (`isPlayMode`, siehe KDoc von `MIGRATION_18_19` in `:app`).
  Betroffen sind alle Entities aus `:core`: `GlyphReminder`, `LibraryAnimation`,
  `BuiltInAnimationSelection`. Nicht betroffen: `AvatarFeedEvent` und `AvatarPlayState` — die
  liegen in `:app-sim` und gibt es in `:app` gar nicht.
- Module: `:core` (gemeinsame Datenschicht), `:app` (Glyphkalender), `:app-sim` (Avatar-App).

### Ankerpunkte

| Datei | Zeilen | Was dort steht — nur diesen Bereich öffnen |
|---|---|---|
| `core/…/data/AnimationType.kt` | 27 gesamt | Die 12 Typen. Ganz lesbar. |
| `core/…/data/LibraryAnimation.kt` | 22 gesamt | Entity. Ganz lesbar. Hier fehlt `nodeId`. |
| `core/…/data/DefaultLibraryAnimations.kt` | **33–60** | Die 26 Labels. Ab 61 nur Frame-Mathematik — nicht nötig. |
| `core/…/data/AvatarSignatureAnimations.kt` | **36–100** | Die 30 Einträge je Avatar. Ab 101 nur Zeichenhilfen. |
| `app-sim/…/matrix/AvatarAnimations.kt` | **107–130** | `creatureFrame` — Signatur der Parametrisierung |
| | **329–430** | `flightOffsetsFor`, `reactionFor`, `finishMoodFor` — der Dispatch |
| | 583–648 | `Fidget` + `fidgetSequence` |
| | 1196 ff. | `speciesFlourish` |
| `app-sim/…/matrix/AvatarSignatureReactions.kt` | **34–100** | `forLabel` — die 30er-Weiche. Ab 101 die Choreografien. |
| `app-sim/…/ui/AvatarFeeding.kt` | 35, 77 | `overlaps`, `playReaction` |
| `app-sim/…/ui/HomeScreen.kt` | **178–360** | Zustand, `feedNow` (290), Kollision (356) |
| | 600–700 | Uhr-Sprite, Zieh-Geste, Easter Egg |
| `app-sim/…/ui/DockScreen.kt` | **1580–1640** | `logFeedEvent` (1591), `playReaction` (1623) |
| `app-sim/…/data/AvatarFeedEvent.kt` | 24 ff. | Entity mit `animationType`, `libraryAnimationLabel`, `fedAtMillis` |
| `app-sim/…/data/AvatarPlayState.kt` | ganz | `xp`, `lastSeenLevel` |
| `app-sim/…/ui/PlayModeXp.kt` | 18 gesamt | `levelFor`, `XP_PER_FEED`, `XP_PER_LEVEL` |
| `app-sim/…/data/AppDatabase.kt` | 25–42 | Entities + `version = 23` |
| `app-sim/…/data/AppDatabaseMigrations.kt` | **110–135** | Muster: Spalte hinzufügen (17→18), neue Tabelle (18→19) |
| `app/…/data/AppDatabase.kt` | 27–36 | Die **zweite** Datenbank, `version = 20` |
| `app/…/data/AppDatabaseMigrations.kt` | ganz (75 Z.) | Klein. `ALL`-Array am Ende. |
| `core/…/data/LibraryAnimationNodeIds.kt` | ganz (37 Z.) | Nachtrag der `nodeId`, von beiden Migrationen benutzt |
| `app-sim/…/matrix/PlayRoutine.kt` | 95–135 | `PlayRoutines.forTopic`, `allFor`. Ab 136 die Abläufe. |
| `app-sim/…/matrix/PlayScene.kt` | **119, 170** | Nur die beiden Enums `Place` und `Station` |
| `app-sim/…/matrix/PlayGamePlan.kt` | 49–60 | `stageFor`, `forSpecies` |
| `app-sim/…/matrix/PlayAmbientActivity.kt` | 53, 115 | `Action`, `DayPhase` |

### Diese Dateien nie ganz öffnen

| Datei | Zeilen | Warum sie trotzdem auftaucht |
|---|---|---|
| `PlayScene.kt` | 3.672 | Nur zwei Enums werden gebraucht (119, 170) |
| `DockScreen.kt` | 2.925 | Nur der Fütterpfad (1580–1640) |
| `HomeScreen.kt` | 1.544 | Nur zwei Bereiche (178–360, 600–700) |
| `AvatarAnimations.kt` | 1.285 | Vier benannte Bereiche, siehe oben |
| `AvatarSignatureAnimations.kt` | 848 | Nur die Einträge (36–100) |
| `DefaultLibraryAnimations.kt` | 743 | Nur die Labels (33–60) |
| `PlayTalk.kt`, `ReminderScreen.kt`, `AvatarClips.kt` | groß | Für Phase 0–4 irrelevant |

### Prüfbefehle

**`JAVA_HOME` ist nicht gesetzt.** Ohne diese Zeile bricht jeder Gradle-Aufruf sofort ab:

```
export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"   # Bash
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"   # PowerShell
```

```
gradlew.bat :core:test                  # Katalog, Zuordnung, Typen
gradlew.bat :app-sim:testDebugUnitTest  # Reaktionen, Freischaltung, Abläufe
gradlew.bat :app-sim:assembleDebug      # baut die APK
```

### Wenn ein Build an einem `G:\Meine Ablage`-Pfad scheitert

```
gradlew.bat --stop
```

**Das ist die ganze Lösung.** Ursache war ein langlebiger Gradle-**Daemon**, der noch ein
Dateisystem-Abbild aus der Zeit vor dem Umzug (siehe UMZUG.md) im Speicher hielt und KSP deshalb
Quelldateien unter `G:\Meine Ablage\Notime\…` meldete, während das Projekt längst unter `C:\Notime`
liegt — daher „this and base files have different roots".

Der Pfad steht **nirgends auf der Platte**: Weder `grep` über das Projekt noch über
`~/.gradle/caches` findet ihn, und `C:\Notime` ist auch kein Junction. Wer ihn sucht, sucht
vergeblich — deshalb steht das hier. Seit dem Daemon-Neustart laufen `:core:test`,
`:app:assembleDebug` und `:app-sim:assembleDebug` alle durch.

Vorhandene Wächter, die den Umbau absichern — bei rotem Testlauf zuerst hier nachsehen:
`AvatarAnimationsTest`, `FeedingChainCharacterizationTest`, `HomeScreenCharacterizationTest`,
`AvatarSignatureAnimationsTest`, `LibraryAnimationFitTest`, `PlayRoutineTest`,
`AppDatabaseMigrationTest`.

---

## Getroffene Entscheidungen

Verbindlich für alle Pakete. Wer eine ändert, muss die betroffenen Pakete neu bewerten.

1. **Freischaltung gilt nur für das Spiel.** Der Baum steuert ausschließlich die Zieh-Leiste. Die
   Bibliothek für echte Erinnerungen bleibt vollständig offen — ein Spielfortschritt darf nie eine
   Erinnerung sperren.
2. ~~**Freischalt-Angebot: 2 + 1.**~~ **Ersetzt in P11.** Der Spieler wählt jetzt selbst auf dem
   Wander-Brett, kein algorithmisches Angebot mehr — siehe P11 für die Begründung.
3. **Cake wird Kopf von `aufbruch/feiern`.** `koerper/essen` bekommt ein neues Motiv (Teller).
4. **`naehe` wird zuerst gezeichnet.** Einzige Gruppe ohne Charakter-Motiv.
5. **Katalog und Zuordnung nach `:core`.** Choreografien und Zustand bleiben in `:app-sim`.
6. **Freischaltung ist ab P11 manuell, nicht mehr algorithmisch.** Jeder Levelaufstieg gibt einen
   Skillpunkt; der Spieler tippt selbst einen erreichbaren Nachbarknoten auf dem Brett an, statt
   ein 2+1-Angebot vorgesetzt zu bekommen (Entscheidung vom 2026-08-29, auf Nutzerwunsch — ein
   Skillbaum wie in Diablo 2 / dem Sphere Grid aus Final Fantasy X, den man selbst begeht).
7. **Keine Ränge pro Knoten.** Ein Knoten bleibt einfach freigeschaltet/gesperrt, kein
   Mehrfach-Investment wie Diablo 2s Skill-Ränge (Entscheidung vom 2026-08-29 — hält das
   Datenmodell und die DB unverändert).
8. **Das Brett ersetzt die Liste, es kommt nicht zusätzlich dazu.** Es gibt nur eine
   Skillbaum-Oberfläche (Entscheidung vom 2026-08-29).

---

## Die vollständige Zuordnung

**Diese Tabelle ist das Ergebnis der Bestandsaufnahme und wird nicht neu hergeleitet.** Sie ist die
Vorlage für Paket 0. `←` heißt vorhandenes Motiv, `✎` heißt neu zu zeichnen.

Pfadschema: `hauptgruppe/untergruppe/blatt`, ASCII, keine Umlaute.

| Stufe 1 (Kopf) | Stufe 2 (Kopf) | Stufe 3 |
|---|---|---|
| `sport` ← MOVE | `ballsport` ← Football | Basketball, Trophy, ✎ Dribbling, ✎ Schuss |
| | `kraft-ausdauer` ← Fitness | Summit, Ladder, Flag, ✎ Heben |
| `koerper` ← DRINK | `trinken` ← Wave | Drip, Puddle, Rain |
| | `essen` ← ✎ Teller | Plant, Battery |
| `ruhe` ← SLEEP | `schlafen` ← Cloud | Balloon, Turtle, Snail |
| | `pause` ← REST | Candle, Lantern, Nest |
| `achtsamkeit` ← MINDFULNESS | `atmen` ← Breathe | Feather, Anchor |
| | `beobachten` ← Butterfly | Bubble, Constellation, Eye |
| `arbeit` ← WORK | `geraet` ← Robot | Stocks, TAMA, ✎ Pause machen |
| | `erledigen` ← FOCUS | Check, Target, Hourglass, Shield, Lighthouse |
| `lernen` ← BOOK | `lesen` ← Scroll | Idea, ✎ Notizen |
| | `knobeln` ← Puzzle | Key, Compass |
| `kreativ` ← CREATIVITY | `musik` ← Music | Drum, Bolt, ✎ Singen |
| | `bauen-malen` ← Star | Fire, Kite |
| `naehe` ← LOVE | `freunde` ← Mail | Gift, ✎ Besuch, ✎ Anrufen |
| | `tiere` ← Dog | Cat, Pet, Paw |
| `aufbruch` ← GENERAL | `reisen` ← Airplane | Rocket, Comet, ✎ Karte |
| | `feiern` ← Cake | ✎ Konfetti, ✎ Kerzen |

**Bilanz:** 9 Köpfe Stufe 1 (alle vorhandene Typen) + 18 Köpfe Stufe 2 (17 vorhanden, 1 neu) +
52 Blätter (41 vorhanden, 11 neu) = **79 Knoten.** Alle 67 baumfähigen Motive sind untergebracht,
**12 Knoten warten noch auf ihre Zeichnung.** MEDICINE steht bewusst außerhalb.

---

## Arbeitspakete

### P0 — Katalog anlegen

**Öffnen:** `AnimationType.kt` (ganz), `LibraryAnimation.kt` (ganz), die Zuordnungstabelle oben.
**Nicht öffnen:** `DefaultLibraryAnimations.kt` und `AvatarSignatureAnimations.kt` — die Labels
stehen vollständig in der Tabelle oben.

- [x] `core/…/data/AnimationNode.kt`: Pfad-Id, Titel, Emoji, Art (`ACTIVITY` / `FLOURISH`)
- [x] `core/…/data/AnimationTree.kt`: die 27 Knoten aus Spalte 1 und 2 der Tabelle
- [x] Blätter aus Spalte 3 ergänzen, die `✎`-Einträge zunächst ohne Motiv anlegen
- [x] `AnimationTree.nodeIdFor(label: String)` — Zuordnung für alle 67 Motive
- [x] MEDICINE ausdrücklich ausschließen, mit Begründung als Kommentar
- [x] `core/…/AnimationTreeTest.kt`: jeder Pfad auflösbar, jedes Motiv genau ein Knoten, jeder
      Knoten höchstens ein Motiv, jede Untergruppe mindestens zwei Blätter
- [x] 27 Zeichenketten für die Namen der Stufen 1 und 2, Englisch und Deutsch

**Prüfen:** `gradlew.bat :core:testDebugUnitTest` — **App bleibt unverändert, kein `:app-sim` nötig.**

**Erledigt am 2026-08-25.** 19 Tests, alle grün. Blattnamen kommen bewusst vom Motiv und haben
keine eigene Zeichenkette — eine zweite Bezeichnung daneben könnte nur auseinanderlaufen. Die 12
ungezeichneten Blätter bekommen ihren Namen zusammen mit ihrer Zeichnung in P8.

---

### P1 — Datenbank

**Öffnen:** beide `AppDatabase.kt`, beide `AppDatabaseMigrations.kt`, beide `AppDatabaseMigrationTest.kt`.
**Nicht öffnen:** Sonst nichts.

- [x] `nodeId TEXT` auf `LibraryAnimation`, nullable (für selbstgezeichnete)
- [x] `core/…/LibraryAnimationNodeIds.kt`: Nachtrag aus `AnimationTree`, von beiden Modulen benutzt
- [x] `DefaultLibraryAnimations.seed()` setzt `nodeId` gleich mit — an einer Stelle für alle 56
- [x] `:app-sim` `MIGRATION_20_21`, Version 21, Schema `app-sim/schemas/…/21.json`
- [x] `:app` `MIGRATION_19_20`, Version 20, Schema `app/schemas/…/20.json`
- [x] Beide `AppDatabaseMigrationTest` erweitert, beide `CURRENT_VERSION` erhöht

**Prüfen:** `gradlew.bat :core:testDebugUnitTest :app-sim:testDebugUnitTest`, dazu
`gradlew.bat :app-sim:compileDebugAndroidTestKotlin :app:compileDebugAndroidTestKotlin` — die
Migrationstests sind instrumentiert und brauchen zum Laufen ein Gerät; übersetzen lassen sie sich
ohne.

**Erledigt am 2026-08-25.** 483 Tests, alle grün; beide androidTest-Quellen übersetzen.

> **Korrektur an diesem Paket:** Es war nur für `:app-sim` geplant. `LibraryAnimation` liegt aber
> in `:core` und wird von beiden Apps benutzt — `:app` brauchte dieselbe Spalte und eine eigene
> Migration 19→20, sonst wäre es beim nächsten Update am Öffnen der Datenbank abgestürzt. Siehe
> den neuen Punkt „Es gibt ZWEI Datenbanken" oben; **P3 und P4 müssen das mitdenken.**

---

### P2 — Reaktion über den Pfad

Das teuerste Paket. Alles, was `AvatarAnimations.kt` betrifft, passiert hier in einer Sitzung.

**Öffnen:** `AvatarAnimations.kt` 107–130 und 329–430, `AvatarSignatureReactions.kt` 34–100.
**Nicht öffnen:** Die Choreografien ab `AvatarSignatureReactions.kt:101` — sie ändern sich nicht,
nur ihr Schlüssel.

- [x] `AvatarReactions.forNode(nodeId, body)` — läuft den Pfad von hinten nach vorn
- [x] `AvatarSignatureReactions.forNode` — Schlüssel ist der Knoten, Weiche bleibt labelbasiert
- [x] Charakterisierungstest **vor** dem Umbau erzeugt: `app-sim/src/test/reaction-fingerprint.txt`
- [x] Rocket-Sonderfall erhalten: `flightOffsetsFor` greift auf `aufbruch/reisen/rocket`
- [x] `AvatarReactionsTest` — nagelt die Vererbungsregel fest
- [x] Geprüft: **alle 9 Stufe-1-Gruppen haben bereits eine Choreografie** (über ihren eingebauten
      Typ: MOVE, DRINK, SLEEP, MINDFULNESS, WORK, BOOK, CREATIVITY, LOVE, GENERAL). P9 muss sie
      nicht erfinden, sondern nur requisitenfrei machen und als Gruppen-Antwort eintragen.

**Prüfen:** `gradlew.bat :app-sim:testDebugUnitTest`. Rot heißt hier immer: eine Bildfolge hat
sich verändert. Nicht den Test anpassen — die Ursache suchen.

**Erledigt am 2026-08-25.** 490 Tests, alle grün; alle 69 Fingerabdrücke unverändert.

> **Abweichung:** `reactionFor` behält `libraryAnimationLabel` als Parameter und löst den Knoten
> intern auf. Grund: `nodeId == null` bedeutet zweierlei — „keine Bibliotheks-Animation" und
> „selbstgezeichnet, noch nicht zugeordnet". Beide müssen unterschiedlich behandelt werden
> (Themen-Handlung vs. Freuden-Reaktion), das Label trägt diese Unterscheidung noch. **Die
> Signatur wechselt in P3**, wenn `AvatarFeedEvent` den Knoten selbst mitbringt.

### Die Vererbungsregel — für P9 wichtig

Der erste Entwurf ließ den Rückfall einfach über alle Choreografien laufen. Das veränderte genau
ein Motiv, und dieser Fall ist die Regel:

**Idea** hängt unter `lernen/lesen`, und dieser Knoten trägt das Motiv *Scroll*. Idea erbte damit
Scrolls Choreografie — „der Blick wandert zeilenweise, dann: verstanden". Vom Takt her passt das
zur Glühbirne sogar gut. Nur hält der Avatar dabei eine **Schriftrolle**: Die Requisite gehört zu
Scroll, nicht zu der Stelle im Baum, an der Scroll zufällig sitzt.

Daraus zwei Sorten Antwort:

| Sorte | Gilt für | Requisite |
|---|---|---|
| **Motiveigen** (die 30) | nur ihren eigenen Knoten | die des Motivs — deshalb nicht vererbbar |
| **Gruppen-Antwort** (P9) | alles darunter | muss requisitenfrei sein |

`AvatarReactions.groupAnswer` ist die leere Weiche, in die P9 einträgt. Sie ist bewusst leer:
Eine der 30 vorhandenen Choreografien dort einzuhängen, nur damit der Rückfall „etwas tut", würde
genau den Idea-Fehler einbauen. `AvatarReactionsTest` bewacht beides.

---

### P3 — Aufrufer nachziehen

**Öffnen:** `AvatarFeeding.kt` 35 und 77, `HomeScreen.kt` 178–360, `DockScreen.kt` 1580–1640.
**Nicht öffnen:** Den Rest beider Bildschirme.

**Vollständige Aufrufliste** (Stand nach P2 — nicht neu suchen):

| Datei | Zeile | Aufruf |
|---|---|---|
| `AvatarFeeding.kt` | 115, 120 | `reactionFor`, `flightOffsetsFor` |
| `AvatarClips.kt` | 125, 242, 393, 528, 529, 604 | `reactionFor` ×5, `flightOffsetsFor` ×1 |
| `DockScreen.kt` | 826 | `reactionFor(species, step.topic)` |
| `AvatarAnimations.kt` | 516 | `reactionFramesFor` → delegiert |
| `PlayModeRoll.kt` | 80 | `labelsFor(species)` — **bleibt labelbasiert**, andere Frage |

- [x] `ReactionTrigger` eingeführt — löst die Mehrdeutigkeit, die P2 blockiert hatte
- [x] `AvatarFeeding.playReaction` nimmt `trigger` statt zweier nullbarer Werte
- [x] `reactionFor` / `flightOffsetsFor` / `reactionFramesFor` auf `ReactionTrigger`
- [x] Alle 11 Aufrufstellen umgestellt
- [x] `AvatarFeedEvent` bekommt `nodeId` — DB 21 → 22, **nur `:app-sim`** (`:app` bleibt auf 20)
- [x] `ReminderTrigger.feedEventFor` schreibt den Knoten gleich mit
- [x] Altdaten nachgetragen, aus **beiden** Quellen; MEDICINE bleibt `null`
- [x] `ReactionTriggerTest`, Migrationstest 21→22, `FeedEventOwnershipTest` erweitert

**Prüfen:** `gradlew.bat :app-sim:testDebugUnitTest`, dann einmal `assembleDebug` und auf dem
Gerät füttern.

**Erledigt am 2026-08-25.** 497 Tests grün, alle 70 Fingerabdrücke unverändert, APK baut.
Auf dem Gerät noch nicht gegengeprüft.

### `ReactionTrigger` statt zweier nullbarer Werte

Der in P2 zurückgestellte Punkt. Ein Knoten allein kann vier Fälle nicht auseinanderhalten, die
sich unterschiedlich verhalten müssen:

| Fall | Reaktion |
|---|---|
| `Topic(type)` — eingebaute Erinnerung | Handlung zum Thema |
| `Node(nodeId)` — Animation im Baum | eigene oder geerbte Choreografie |
| `Untracked` — selbstgezeichnet, kein Knoten | arteigene Freuden-Reaktion |
| `None` — Antippen, Easter Egg | generisch |

Als `nodeId: String?` wären der dritte und vierte Fall identisch gewesen (`null`) — und das hätte
jede selbstgezeichnete Animation still um ihre Reaktion gebracht. **`ReactionTrigger.of(type, label)`
ist der einzige Ort, an dem aus Datenbankspalten ein Anlass wird.**

**MEDICINE** läuft über `Topic` und damit an jedem Knoten vorbei — es steht ja bewusst nicht im
Baum. `ReactionTriggerTest` prüft für alle sechs Spezies, dass es trotzdem seine Reaktion bekommt.

---

### P4 — Besitz und Freischaltung

**Öffnen:** `AvatarPlayState.kt`, `PlayModeXp.kt`, `AvatarFeedEvent.kt` 24 ff.
**Nicht öffnen:** Kein Bildschirm. Dieses Paket ist reine Logik.

- [x] Entity `AvatarUnlockedNode(profileId, nodeId, unlockedAtMillis)` + DAO, DB 22 → 23
- [x] Beim ersten Start je Profil die 9 Stufe-1-Knoten freischalten (`ensureSeeded`)
- [x] `BranchAffinity`: Neigung je Hauptgruppe, Halbwertszeit 14 Tage
- [x] Nur beantwortete Auslösungen zählen (`fedAtMillis IS NOT NULL`)
- [x] `UnlockOffers`: Grenze + Angebot 2 + 1
- [x] MEDICINE steht nicht im Baum, kann also nicht angeboten werden — bewacht
- [x] Tests gegen erfundenes Log: einseitig, leer, alles gleich, alles offen
- [x] Test: der Querschläger kommt nie aus dem stärksten Zweig

**Prüfen:** `gradlew.bat :app-sim:testDebugUnitTest`

**Erledigt am 2026-08-25.** 21 neue Tests, 518 gesamt grün, Fingerabdruck unverändert.
Alles Neue liegt im Paket `app-sim/…/skilltree/`.

### Zwei Regeln, die beim Bauen dazugekommen sind

**Ungezeichnete Knoten werden nie angeboten.** Ein Eintrag in der Zieh-Leiste, der beim Ziehen
nichts zeigt, wäre eine leere Belohnung — schlimmer als gar keine. Nebenwirkung: `koerper/essen`
ist die einzige Untergruppe ohne Motiv, und solange das so ist, hängen auch ihre beiden Blätter
(`plant`, `battery`) fest. **Das löst sich mit P8**, sobald der Teller gezeichnet ist.
`UnlockOfferTest` hält die Liste der unerreichbaren Knoten exakt fest — wächst sie, fällt es auf.

**Ohne Historie wird der Schwerpunkt gewürfelt.** Stehen alle neun Zweige auf 0.0, gibt es keinen
stärksten — immer denselben zu nehmen wäre eine Behauptung über den Nutzer, die nichts deckt. Die
Rangfolge selbst (`BranchAffinity.ranked`) bleibt deterministisch; gewürfelt wird nur in
`UnlockOffers.build` über einen übergebenen `Random`, damit Tests reproduzierbar bleiben.

---

### P5 — Zieh-Leiste und Baumbildschirm

**Öffnen:** `HomeScreen.kt` 600–700 (Zieh-Geste als Vorlage).
**Nicht öffnen:** `DockScreen.kt` — die Leiste kommt zuerst nur auf den Startbildschirm.

- [x] Leiste am unteren Rand mit den freigeschalteten Knoten
- [x] Ziehen aus der Leiste auf den Avatar — `overlaps` und `playReaction` unverändert
- [x] Die Uhr behält ihre Rolle für echte Erinnerungen; die Leiste ist zusätzlich
- [x] Kreis-Easter-Egg kann aus der Leiste nicht ausgelöst werden (Winkelverfolgung sitzt
      ausschließlich im Zieh-Handler der Uhr — gilt durch Konstruktion)
- [x] Baumbildschirm: offen / als Nächstes / gesperrt / noch nicht gezeichnet, mit Level und
      XP-Rest sowie Fortschritt je Hauptgruppe
- [x] Bedienungshilfen: Zusatzaktion je Eintrag, zusammenhängende Vorlesetexte im Baum
- [x] 12 neue Zeichenketten EN/DE, Paritätstest grün

**Prüfen:** Auf dem Gerät, mindestens zwei Spezies. **Steht noch aus.**

**Erledigt am 2026-08-25.** 8 neue Tests (`SkillTreeRowsTest`), 526 gesamt grün, Lint grün,
APK baut. Neue Dateien: `skilltree/SkillTreeRows.kt`, `ui/SkillDragBar.kt`,
`ui/SkillTreeScreen.kt`, `ui/SkillTreeDialog.kt`, `ui/SkillTreeState.kt`.

### Die Entscheidung, die hier fiel

**Ein Zug aus der Leiste schreibt kein Fütter-Ereignis und gibt kein XP.** Er beantwortet keine
Erinnerung, sondern ist ein Spielzug. Landete er in `avatar_feed_events`, fälschte er genau die
Statistik, aus der P4 die Neigung rechnet — **der Baum würde sich aus sich selbst speisen** statt
aus dem, was im Alltag wirklich passiert. Damit ist auch der offene Punkt „Zählen Fütterungen aus
der Leiste XP?" beantwortet: nein.

Die Leiste ist abgeblendet, solange eine echte Erinnerung wartet — dann gehört die Geste der Uhr.
Zwei Zieh-Ziele nebeneinander wären nur verwirrend.

---

### P6 — Laufende Tätigkeit (Stufe 3)

**Öffnen:** `PlayRoutine.kt` 95–135, `PlayScene.kt` nur 119 und 170.
**Nicht öffnen:** Die Abläufe ab `PlayRoutine.kt:136`.

- [x] `AvatarActivity(nodeId, sinceMillis)` als beobachtbarer Zustand (`AvatarActivityBus`)
- [x] Tätigkeits-Knoten setzen ihn, statt einmalig abzuspielen
- [x] Passende Tätigkeit: einschieben, Tätigkeit läuft danach weiter
- [x] Unpassende Tätigkeit: erst Wechsel zur Stufe-2-Tätigkeit, dann Einlage — in einem Zug
- [x] Test: Einlage beendet die passende Tätigkeit nicht
- [x] Test: kein Plan enthält mehr als einen Wechsel, kein Flackern
- [x] Test: jede Einlage hängt unter einer echten Tätigkeit
- [ ] ~~`RoutineStep.Flourish(nodeId)`~~ — **nicht gemacht, siehe unten**

**Prüfen:** `gradlew.bat :app-sim:testDebugUnitTest`

**Erledigt am 2026-08-25.** 18 neue Tests, 544 gesamt grün, Lint grün, APK baut, Fingerabdruck
unverändert. Auf dem Gerät noch nicht gegengeprüft.

### Die Regel

| Gezogen | Läuft gerade | Was passiert |
|---|---|---|
| Tätigkeit (Stufe 1/2) | egal | Er fängt sie an |
| Einlage (Stufe 3) | ihr Elternknoten | nur die Einlage — **die Tätigkeit läuft weiter** |
| Einlage | etwas anderes | erst Wechsel zur Eltern-Tätigkeit, dann die Einlage |

**Passend heißt: genau der Elternknoten.** Wer allgemein „Sport" macht und ein Dribbling bekommt,
wechselt zu Ballsport — für einen Trick braucht es einen Ball, und „Sport" ist noch keiner.

Die Tätigkeit **läuft nach 5 Minuten ab**. Ohne Verfall gälte er noch Stunden später als „spielt
Ball", und eine Einlage wäre die Antwort auf etwas längst Vergangenes.

### Warum `RoutineStep.Flourish` nicht gebaut wurde

`RoutineStep` gehört zur **Weltsimulation** — den Abläufen, in denen der Avatar im Dock-Modus zu
Stationen läuft (`PlayRoutine`, `PlayScene`). Der Zieh-Pfad benutzt davon nichts: Er spielt
Reaktionen direkt ab. Ein `Flourish`-Schritt dort hätte also nicht Stufe 3 zum Laufen gebracht,
sondern die Zieh-Leiste an die Weltsimulation angeschlossen — **das ist ein eigenes Paket**, kein
Teil von „Stufe 3 funktioniert".

Wer es später baut: `AvatarActivityPlans.planFor` liefert bereits die Schrittfolge; die
Weltsimulation müsste sie nur in ihre eigenen Schritte übersetzen.

---

### P7 — Werkzeug für Pixelarbeit

- [x] Vorschau-Werkzeug: `core/…/AnimationPreviewTest.kt` schreibt Kontaktbögen nach
      `core/build/preview/` — ein PNG je Motiv, plus `_neu-1..3.png` als Sammelbögen
- [x] Eingecheckt, als Test statt als Skript

```
gradlew.bat :core:testDebugUnitTest --tests "*AnimationPreviewTest*"
```

**Warum als Test:** läuft mit `:core:test` ohnehin mit, braucht keine Zusatzwerkzeuge und prüft
gleich mit, was sich *maschinell* prüfen lässt (Frames außerhalb des Rasters, leere Frames, zu
wenige Frames). Was das Auge entscheiden muss, liegt danach als PNG daneben. Das PNG wird von Hand
geschrieben — `java.awt`/`javax.imageio` gibt es im Android-Unittest nicht, `Deflater` und `CRC32`
schon.

**Beim ersten Lauf zwei Altlasten gefunden**, beide als Absicht bestätigt und jetzt namentlich als
Ausnahme hinterlegt: `Rocket` fliegt oben aus dem Raster (das Abschneiden *ist* der Start), `TAMA`
hat leere Frames zwischen den Buchstaben.

---

### P8 — Die 12 neuen Motive

- [x] Alle zwölf gezeichnet: `core/…/SkillTreeAnimations.kt`
- [x] In den Baum gehängt — **`pendingArtwork()` ist jetzt leer**
- [x] Vier Stolperdrähte aus P0/P4/P5 haben gefeuert und sind auf die Gegenrichtung umgestellt

| Knoten | Motiv | |
|---|---|---|
| `koerper/essen` | 🍽️ Plate | Gabel geht hinunter, der Berg wird kleiner |
| `naehe/freunde/besuch` | 🚪 Visit | jemand kommt herein und winkt |
| `naehe/freunde/anrufen` | 📞 Call | Hörer wackelt, Wellen laufen hinaus |
| `sport/ballsport/dribbling` | 🤾 Dribble | Ball federt, Hand geht mit, Seitenwechsel |
| `sport/ballsport/schuss` | 🥅 Shot | Ball ins Tor, Netz gibt nach |
| `sport/kraft-ausdauer/heben` | 💪 Lift | Hantel hoch, Stange biegt sich oben |
| `arbeit/geraet/pause-machen` | ⏸️ Breather | Balken halten an, Dampf steigt |
| `lernen/lesen/notizen` | 📝 Notes | Zeile für Zeile, Stift läuft voraus |
| `kreativ/musik/singen` | 🎤 Sing | Mikrofon, Ton geht hinaus, Fuß wippt |
| `aufbruch/reisen/karte` | 🗺️ Map | gefaltete Karte, Route läuft entlang |
| `aufbruch/feiern/konfetti` | 🎊 Confetti | Schnipsel rieseln und treiben |
| `aufbruch/feiern/kerzen` | 🕯️ Candles | drei Kerzen flackern und gehen aus |

**Erledigt am 2026-08-25.** 547 Tests grün, Lint grün, beide APKs bauen.

### Was das Zeichnen gelehrt hat

**ASCII statt Koordinatenlisten.** Die vorhandenen Motive stehen als `listOf(5 to 4, 6 to 4, …)`
im Code — kompakt, aber man sieht der Zeile nicht an, was sie zeichnet. `sprite(4, 6, "#####")`
liest sich wie das, was es ist.

**Drei Motive mussten neu gezeichnet werden**, nachdem der Kontaktbogen sie zeigte. Am deutlichsten
`Besuch`: Der erste Entwurf hatte ein Türblatt, das nach links schrumpfte — auf 13×13 liest sich
das als ineinandergeschachtelte Ringe, nicht als Tür. Ohne Blatt, dafür mit einer Figur, die
hereinkommt, ist es sofort klar. Genau dafür war P7 da.

**Die Matrix ist rund, nicht quadratisch.** `LibraryAnimationFitTest` (gab es längst) hat
zugeschlagen: Dribblings Bodenlinie über die volle Breite lag zu 38 % außerhalb des Ausschnitts.
In den untersten Zeilen ist deutlich weniger Platz, als das Raster vermuten lässt — bei y=11 nur
x=2…10.

---


### P9 — Choreografien

- [x] Eigene Reaktion für jede der 18 Untergruppen (`AvatarReactions.groupAnswer`)
- [x] Keine verlässt sich auf Schwanz oder Füße — die hat nicht jede Spezies
- [x] Knoten mit eingebautem Typ spielen die Handlung ihres Themas
- [ ] Einlagen für einzelne Stufe-3-Knoten — **bewusst offen gelassen, siehe unten**

**Erledigt am 2026-08-26.** 548 Tests grün, Lint grün, APK baut.

### Zwei Dinge, die erst beim Bauen auffielen

**Ein Knoten mit eingebautem Typ bekam die falsche Antwort.** `sport` trägt das Motiv `MOVE`, aber
beim Ziehen aus der Leiste kam nur die generische Freuden-Reaktion — die ausgespielte
Bewegungs-Handlung, die es längst gibt, wurde nie erreicht. Betrifft elf Knoten: die neun
Hauptgruppen plus `ruhe/pause` (REST) und `arbeit/erledigen` (FOCUS). Eine Zeile in
`reactionFor`, große Wirkung.

**`drop(1)` war zu viel.** Der Rückfall übersprang den Knoten selbst in der Annahme, er sei beim
Motiv-Schritt schon dran gewesen — der fragt aber nur die *Motiv*-Antworten ab. Eine Untergruppe
fand dadurch ihre eigene Gruppen-Antwort nicht: Wer „Ballsport" zog, bekam die generische Reaktion,
während jedes Blatt darunter die richtige bekam. `AvatarReactionsTest` fängt das jetzt.

### Warum die Gruppen-Antworten requisitenfrei sind

Sie werden nach unten vererbt. Hätte „Ballsport" einen Ball in der Hand, läge der auch dann da,
wenn gerade ein **Pokal** gezogen wurde. Gearbeitet wird deshalb nur mit dem, was jede Spezies hat
und was zu jedem Blatt darunter passt: Verschiebung, Haltung, Blick, Mund, Timing. **Das Motiv auf
dem Glyph trägt die Genauigkeit, der Körper die Energie.**

### Was offen bleibt — und warum das in Ordnung ist

Einzelne Einlagen für Stufe-3-Knoten („Dribbling" mit eigenem Trick statt der Ballsport-Bewegung)
sind **nicht** gebaut. Sie blockieren nichts: Dank des Rückfalls hat jeder der 79 Knoten eine
passende Antwort, und jede spätere Choreografie macht sie nur genauer, ohne dass ein Aufrufer sich
ändert. Der Platz dafür ist `AvatarSignatureReactions` — dort landen motiveigene Antworten, die
**nicht** vererbt werden.

Messbar: 37 Motive haben durch P9 eine passendere Antwort bekommen, vorher war es überall dieselbe
Freuden-Reaktion. Die 30 Charakter-Motive, die Themen, Rocket und selbstgezeichnete Animationen
sind unverändert geblieben.

---


### P10 — Lesbarkeit der Motive

- [x] `Football` neu: Geste statt Aufbau (Figur, Ausholen, Treffer, Ball fliegt hinaus)
- [x] `Confetti` neu: jede Spalte immer besetzt, rund 17 Zellen je Frame statt 3,9
- [x] Kontaktbogen zeigt den **runden** Ausschnitt statt des Quadrats
- [x] `AnimationPreviewTest` prueft die mittlere Zellzahl (`MIN_ZELLEN_IM_MITTEL = 6.0`)
- [x] `sprite` einmal in `core/…/FrameSprite.kt` statt zweimal privat
- [x] Fuenf weitere Auffaellige angesehen und verbessert (siehe unten)

**Prüfen:** `gradlew.bat :core:testDebugUnitTest :app-sim:testDebugUnitTest`, dann die Bögen in
`core/build/preview/` ansehen.

**Erledigt am 2026-08-26.** 548 Tests grün, Lint grün, beide APKs bauen.

### Warum beide Fehler durch alle Wächter kamen

`LibraryAnimationFitTest` prüft gegen den runden Ausschnitt, aber erst ab **15 %** — Footballs Tor
verlor nur seine obere Ecke und blieb darunter. `AnimationPreviewTest` prüfte auf *leere* Frames;
Konfettis erster Frame hatte **einen** Punkt und war damit nicht leer. Und der Kontaktbogen, das
Werkzeug fürs Auge, zeichnete das **Quadrat** — die einzige Darstellung, in der Footballs Tor
vollständig aussieht.

Die Lehre steht schon in P8 („die Matrix ist RUND"), aber sie stand nur im Text. Jetzt steht sie
im Bild.

---

### P11 — Wander-Brett: manuelle Freischaltung

Ersetzt die Liste (`SkillTreeScreen.kt`) und den erzwungenen 2+1-Dialog (`LevelUnlockDialog.kt`)
durch ein raeumliches Brett, auf dem der Spieler selbst zum naechsten Knoten geht — Vorbild:
Diablo 2, das Sphere Grid aus Final Fantasy X. Auf Nutzerwunsch, siehe Entscheidungen 6-8 oben.

- [x] `AnimationTreeLayout` (`:core`): tiefenunabhaengiges Node-Link-Layout, jede Wurzel eine
      eigene Spur. Getestet u. a. mit einem eigens gebauten fuenfstufigen Baum — der Beleg, dass
      eine sechste oder siebte Ebene ohne Codeaenderung funktionieren wuerde.
- [x] Alte Automatik entfernt: `BranchAffinity`, `UnlockOffer`-Datenklasse + `UnlockOffers.build`,
      `AvatarUnlockRepository.offerFor`, `LevelUnlockDialog`, die dafuer noetige DAO-Abfrage
      `AvatarFeedEventDao.answeredNodes`/`AnsweredNodeRow`. `UnlockOffers.frontier`/
      `.startingNodes` und `SkillTreeRows`/`NodeState` (Zustandsrechnung) blieben unangetastet —
      das Brett zeichnet nur, was vorher schon berechnet wurde.
- [x] `SkillTreeScreen.kt` neu: pan-/zoombares `Canvas` fuer Kanten + Knoten-Chips ueber
      `Modifier.transformable`/`graphicsLayer`. Tippbar nur, was `NodeState.AVAILABLE` ist UND
      noch ein Skillpunkt uebrig ist (`LevelUnlocks.due`).
- [x] `SkillTreeDialog.kt` liest den Freischalt-Stand jetzt selbst aus dem Repository (voller
      Stand statt der motiv-gefilterten Zieh-Leisten-Teilmenge) und sperrt Doppel-Taps waehrend
      ein `unlock`-Aufruf laeuft.
- [x] `DockScreen.kt`: der erzwungene Dialog ist weg. Der Levelaufstiegs-Glueckwunsch bestaetigt
      sich nach `LEVEL_UP_BANNER_MS` (3,2 s) von selbst, statt auf eine abgeschlossene Wahl zu
      warten, die jetzt erst viel spaeter auf dem Brett passieren kann.
- [x] Strings: `level_unlock_*` (5) und `skill_tree_progress` (unbenutzt seit die Listenansicht
      weg ist) entfernt, `skill_tree_points_available` ergaenzt — EN/DE synchron gehalten.

**Bewusst nicht Teil dieser Runde:**
- Raenge pro Knoten (Entscheidung 7).
- Aenderungen am Bauminhalt selbst (welches Motiv wo haengt).
- Neue DB-Entities/-Migrationen — keine noetig, `AvatarUnlockedNode` und `unlock()` gab es schon.

**Prüfen:** `gradlew.bat :core:testDebugUnitTest :app-sim:testDebugUnitTest`, danach das Brett auf
dem Geraet/Emulator ansehen — Pan/Zoom-Gesten (`Modifier.transformable`) und die feste
`DpSize`-Berechnung des Bretts (`boardSize` in `SkillTreeScreen.kt`) sind neue Technik in diesem
Projekt und liessen sich hier mangels Netzzugriff nicht gegenpruefen (siehe Journal).

---

## Offene Punkte

- [ ] Selbstgezeichnete Animationen der Nutzer: Auffang-Knoten je Hauptgruppe, Zuordnung später
      von Hand?
- [ ] Braucht die Zieh-Leiste eine Abklingzeit? Sonst füttert man im Sekundentakt.
- [ ] **Aus P11:** Gruppennamen als schwebende Beschriftung ueber jeder Wurzel-Spur auf dem Brett —
      bewusst zurueckgestellt, um die Layout-Mathematik in dieser Sitzung nicht ungeprueft weiter
      zu verschachteln. Die Wurzel-Chips selbst tragen ihr Emoji schon, der Name kommt derzeit nur
      ueber die Bedienungshilfen-Beschreibung an.
- [ ] **Aus P11:** Auf dem Geraet gegenpruefen, ob `Modifier.transformable` sich mit den
      Chip-`clickable`-Handlern vertraegt wie angenommen (Taps sollen trotz Pan/Zoom-Erkennung auf
      dem Elternelement ankommen) — ungeprueft mangels Netzzugriff, siehe Journal.
- [x] Restliche Auffaelligkeiten aus der Motiv-Pruefung (P10): `Lighthouse`, `Rain`, `Paw`,
      `Rocket` und `Comet` in der runden Vorschau geprueft und gezielt verbessert. Strahlen,
      Tropfen und Spur bleiben aus den abgeschnittenen Ecken; Rakete und Komet sind voller lesbar.
- [x] ~~Zählen Fütterungen aus der Leiste XP?~~ **Nein** — entschieden in P5, Begründung dort.

---

## Journal

Zwei Zeilen je Sitzung: was fertig wurde, und was die nächste Sitzung wissen muss.

| Datum | Paket | Ergebnis / Hinweis für die nächste Sitzung |
|---|---|---|
| 2026-08-29 | P11 — Wander-Brett | **Freischaltung ist jetzt manuell: der Spieler wandert selbst ueber ein raeumliches Brett statt einen 2+1-Dialog vorgesetzt zu bekommen** — Nutzerwunsch, Vorbild Diablo 2 / das Sphere Grid aus Final Fantasy X. Kernfund: `AnimationNode.depth`/`.parentId` waren schon rein pfadbasiert (aus dem `/`-getrennten Pfad berechnet), der Baum war also schon vor dieser Sitzung beliebig tief — nur die Oberflaeche (eine Liste) und die Freischalt-Mechanik (Algorithmus) waren es nicht. Neu: `AnimationTreeLayout` (`:core`) rechnet jedem Knoten eine Brett-Position aus, tiefenunabhaengig (Test mit einem eigens gebauten fuenfstufigen Baum als Beleg). `SkillTreeScreen.kt` ist jetzt ein pan-/zoombares `Canvas`+Chip-Brett (`Modifier.transformable`/`graphicsLayer`, `boardX`/`boardY`-Hilfsfunktionen) statt einer `LazyColumn`; `SkillTreeRows`/`NodeState` (Zustandsrechnung) und `UnlockOffers.frontier`/`.startingNodes` (die "erreichbaren Nachbarn") blieben dabei komplett unangetastet, nur wie sie gezeichnet werden aenderte sich. Entfernt, weil nur die alte Automatik bediente: `BranchAffinity`, `UnlockOffer`+`UnlockOffers.build`, `AvatarUnlockRepository.offerFor`, `LevelUnlockDialog`, `AvatarFeedEventDao.answeredNodes`/`AnsweredNodeRow`. `DockScreen.kt`: der Levelaufstiegs-Glueckwunsch wartet nicht mehr auf eine abgeschlossene Wahl, sondern bestaetigt sich nach `LEVEL_UP_BANNER_MS` (3,2 s) von selbst — die Wahl kann jetzt jederzeit spaeter auf dem Brett passieren. Keine DB-Migration noetig, `AvatarUnlockedNode`/`unlock()` gab es schon. **Nicht gegengeprueft:** `dl.google.com` bleibt in dieser Umgebung gesperrt (siehe letzte Sitzung), kein Gradle-Lauf moeglich — Logik von Hand durchgerechnet, aber `Modifier.transformable` in Kombination mit Chip-`clickable` (ob Taps trotz Pan/Zoom-Erkennung ankommen) und die `boardSize`-Layoutrechnung sind neue Technik in diesem Projekt und real ungeprueft. **Fuer die naechste Sitzung:** Zuerst `gradlew.bat :core:testDebugUnitTest :app-sim:testDebugUnitTest` auf einer Maschine mit Netzzugriff, dann das Brett auf dem Geraet ansehen (Pan/Zoom, Taps, Lesbarkeit bei 79 Knoten). Offene Politur: Gruppennamen als Beschriftung ueber den Wurzel-Spuren (siehe "Offene Punkte"). |
| 2026-08-29 | Phasen-Szenen auf PlayInk | **Die sieben grossen Mehrphasen-Szenen sind jetzt auf PlayInk umgestellt** - genau der Punkt, den die vorige Sitzung offen liess. `footballCells`, `basketballCells`, `trainingCells`, `musicCells`, `paintingCells`, `fishingCells` und `kiteCells` rechneten ihre Helligkeiten bisher alle selbst aus (`PlayScene.GLOW - 180` und aehnliche Ad-hoc-Werte); jetzt zeichnen sie ueber `PlayInk.Sketch` wie die zwoelf Weltmotive: Material auf BODY mit automatischer Kantenlicht-Berechnung, Binnenzeichnung auf DETAIL, ein Glanzpunkt als SPARK, und Freistellung nur dort, wo tatsaechlich ein Gegenstand gemeint ist (Ball, Hantel, Gitarre, Bild-Rahmen, Angelrute, Drachen) - Klaenge, Noten, Wellenringe und das gemalte Bild bleiben bewusst Licht ohne Kante. Die Bewegungslogik selbst (Positionen, Phasen, `sway`/`bob`/`drift`) ist unveraendert geblieben, nur wie daraus Zellen werden. Neuer Test `auch die grossen Phasen-Szenen benutzen nur den Zeichenkasten` in `PlayInkTest` prueft alle sieben Szenen ueber alle Phasen und mehrere `scenePhase`-Werte gegen `PlayInk.LEVELS` - dieselbe Regel, die vorher nur `everyMotif` durchlief. Die bestehenden `PlayEffectsTest`-Faelle (Ballhoehe, Notenzahl, Bildwachstum, Drachenschnur) sind von Hand gegen die neuen Koordinatenrechnungen durchgerechnet, nicht nur gelesen. **Nicht gegengeprueft:** `dl.google.com` ist in dieser Umgebung gesperrt (Proxy-Policy), das Android Gradle Plugin laesst sich deshalb nicht aufloesen - weder `:core:testDebugUnitTest` noch `:app-sim:testDebugUnitTest` noch der Kontaktbogen liefen hier. **Fuer die naechste Sitzung:** Als Erstes `gradlew.bat :app-sim:testDebugUnitTest` auf einer Maschine mit Netzzugriff laufen lassen und den Kontaktbogen fuer alle sieben Szenen ansehen, bevor weitere Pixelarbeit folgt - siehe die Warnung zu Footballs Tor und Konfetti weiter oben, dieselbe Kategorie Fehler ist hier ungeprueft moeglich. |
| 2026-08-29 | Zeichenkasten | **Die Requisiten-Ebene hat jetzt ein Regelwerk statt zwoelf Einzelentscheidungen: PlayInk.** Vorher rechnete sich jedes Motiv seine Helligkeit selbst aus (GLOW - 180 und aehnlich) und landete damit zufaellig genau dort, wo auch die Kulisse liegt - das Buch war so hell wie das Regal dahinter, das es beruehrte. Neu sind eine eigene Tonwertstufe fuer Material (BODY, zwischen FURNITURE und GLOW), Kantenlicht immer von oben links (vom Werkzeug gesetzt, nicht vom Motiv gezeichnet), eine deckend schwarze Aussparung (VOID) rings um jede Silhouette und ein Bodenschatten unter allem, was steht. **Die Kernregel:** Materie trennt sich durch die KANTE, Licht durch HELLIGKEIT (ab EDGE) - Materiegewicht ohne Aussparung ist verboten, das ist genau der Fleck. Alle zwoelf Weltmotive und die fuenf getragenen Gegenstaende sind darauf umgestellt und deutlich kleiner geworden (hoechstens 13 statt 18 Zellen breit); Groesse war nie das Problem, fehlende Luft ringsum schon. **Zwei Werkzeugluecken geschlossen:** Der Kontaktbogen zeigte jede Effektzelle als flaches Sternchen und die Figur als blosses A - er warf also ausgerechnet die Helligkeit weg, an der Lesbarkeit haengt, und bescheinigte jedem Entwurf eine Trennung, die er nicht hatte. Jetzt steht zuerst das Verbundbild (alles durch dieselbe Rampe) und darunter die Ebenenkarte. Ausserdem zeigt er fuenf Takte statt einem. **Dabei aufgefallen:** sechs Motive aenderten sich zwar, aber nur INNERHALB ihrer Silhouette (blinkende Schreibmarke, Glanzpunkt auf der Kapsel, umblaetternde Seite zwischen den Seiten) - aus zwei Metern Abstand ein Standbild. Sie bewegen jetzt ihren Umriss. Neuer PlayInkTest haelt die Regeln fest: keine erfundenen Helligkeiten, Material ueber der Moebelstufe, nichts unter dem Boden, und die Trennungsregel. 510 Tests gruen, Lint gruen, beide APKs bauen. **Fuer die naechste Sitzung:** Die sieben grossen Phasen-Szenen (Drachen, Fussball, Basketball, Training, Musik, Malen, Angeln) rechnen ihre Helligkeiten NOCH IMMER selbst aus und sind nicht auf PlayInk umgestellt - das ist die naechste Adresse, und der Test dafuer steht schon. |
| 2026-08-28 | Welt-Look | **Die Weltanimationen wurden nach einer Game-Design-Lesbarkeitspruefung neu inszeniert.** Die zwoelf Hauptmotive sind keine drei bis fuenf Zellen grossen Icons mehr, sondern grosse Requisiten mit Kontur, Binnenzeichnung, Bodenbezug und Bewegung: unter anderem offenes Buch mit umblaetternder Seite, Karaffe mit steigendem Glaspegel, Laptop mit Code und Cursor, Zielscheibe mit Treffpunkt, Palette mit Pinsel sowie Mond und aufsteigende Schlafzeichen. Fuss- und Basketball haben nun grosse Baelle mit Naehten, Flugspuren und ausgearbeitetem Netz; Training besitzt Matte, Bewegungsakzente und Trinkflasche, Angeln Wasserreaktion und einen klaren Fang, der Drachen eine tragfaehige Silhouette, Musik Buehne und grosse Gitarre, Malen eine wachsende Landschaft samt bewegtem Pinsel. Handlungsmotive bleiben jetzt auch waehrend der anschliessenden Szenenpause sichtbar. Ein eigener ASCII-Kontaktbogen zeigt Effekte zusammen mit Figur und Raum; Tests erzwingen Mindestgroesse, Bewegung, Eigenstaendigkeit und Randbegrenzung. |
| 2026-08-28 | Weltmotive komplett | **Alle zwoelf Haupttaetigkeiten besitzen nun zusaetzlich zur Koerperbewegung ein eigenes animiertes Weltmotiv.** Schlaf zeigt wandernde Z-Zeichen, Ruhe eine dampfende Tasse, Lesen ein offenes Buch, Achtsamkeit Atemringe, Naehe ein Herz, Trinken einen Becher, Pflege eine Kapsel, Arbeit einen Bildschirm, Fokus ein Zielkreuz, Bewegung eine laufende Spur, Kreativitaet eine Palette und Allgemeines bewegte Funken. Die Motive erscheinen nur waehrend des Handlungsschritts, folgen der Figur und werden auch bei Abbruch entfernt. Ein Vollstaendigkeitstest prueft alle `AnimationType`-Werte auf sichtbare, unterschiedliche und horizontal begrenzte Motive. Damit sind neben den grossen Mehrphasen-Szenen jetzt auch alle normalen Taetigkeiten visuell abgedeckt. |
| 2026-08-28 | Welt-Kreativitaet | **Musizieren und Malen sind jetzt vollstaendige Welt-Aktivitaeten statt bloss getragener Requisiten.** Im Park stimmt die Figur erst die Gitarre, spielt dann mit aufsteigenden Noten und beendet den Auftritt mit einem sichtbaren Finale. Auf der Wiese entsteht am eigenen Staffelei-Motiv schrittweise erst die Skizze, dann Farbe und schliesslich das fertige Bild. Die Effekte folgen der Figur, bleiben innerhalb der Spielfeldbreite und werden nach dem Ablauf sauber entfernt. Vier neue Tests sichern Phasen, Orte, Fortschritt und Begrenzung. |
| 2026-08-28 | Welt-Sport | **Basketball und Krafttraining als vollstaendige Welt-Aktivitaeten.** Basketball durchlaeuft Dribbling, Zielen, Wurf und Treffer; Korb und Ball werden nur waehrend dieses Ablaufs gezeichnet. Krafttraining zeigt Aufwaermen, Heben ueber den Kopf und Erholung mit sichtbarer Hantel. Auch das Fussballtor ist jetzt dynamisch: Der Sportplatz selbst bleibt neutral und traegt nur Zaun und Linien, damit keine falsche Requisite in eine andere Sportart hineinragt. Drachen, Fussball, Basketball, Training und Angeln werden aus einer gemeinsamen Liste gleichberechtigt gezogen; zuvor bevorzugten nacheinander ausgefuehrte Prozentwuerfe immer die zuerst gepruefte Aktivitaet. Fuenf neue Tests sichern Phasen, Orte, Effekte und Erreichbarkeit aller Spezialaktivitaeten. |
| 2026-08-28 | Level-Freischaltung | **Der fehlende Anschluss zwischen Level und Skillbaum steht.** Level 1 startet nur mit den neun Hauptgruppen; ab Level 2 verdient jeder Aufstieg genau einen neuen Knoten. Der nicht wegklickbare Dialog zeigt das vorhandene 2+1-Angebot und speichert die Wahl sofort, danach wachsen Baum und Zieh-Leiste ueber ihren bestehenden Flow mit. Die Schuld wird aus `Level - 1` minus bereits gewaehlten Nicht-Wurzel-Knoten berechnet: Ein beendeter Dialog geht nicht verloren, und bestehende Spielstaende holen alle fehlenden Wahlen nacheinander nach. Vier neue Regeltests. |
| 2026-08-28 | P10 + Laden | **Die fuenf restlichen auffaelligen Motive verbessert:** Lighthouse hat einen kuerzeren Strahl und einen voll sichtbaren Sockel, Rain vier klare innere Tropfenbahnen, Paw eine eingerueckte Spur, Rocket einen voll sichtbaren Start und Comet einen kraeftigeren Kopf und Schweif. Der Laden ist von Warenregal + Wandregal + Kasse auf Warenregal + Kasse reduziert; das Regal selbst hat nur noch drei statt vier dicht gestapelter Boeden. Die freie Mitte gehoert wieder der Figur. Core- und App-Sim-Unit-Tests gruen; Vorschauen visuell geprueft. |
| 2026-08-25 | — | Bestandsaufnahme und Plan erstellt, Zuordnung aller 67 Motive festgelegt. Noch kein Code geändert. |
| 2026-08-26 | Merge | **`main` ist im Zweig.** Der Zweig war am 18.08. abgezweigt, main 70 Commits weiter. Nur `HomeScreen.kt` kollidierte: main hat `feedNow` zu einem gemeinsamen `feedOccurrence` mit zwei Aufrufern umgebaut (Uhr und Aktions-Speicherplatz) - uebernommen, `playFromBar` steht daneben, `playReaction` geht ueber den `ReactionTrigger`. **Ein 69. Motiv war dazugekommen:** `Clock` (Wecker), von einem ClaudePrimaryRun-Lauf auf main ergaenzt. Haengt jetzt unter `arbeit/erledigen` neben der Sanduhr; ohne Knoten waere `AnimationTreeTest` umgefallen. `ReactionFingerprintTest` meldete nur *neu*, nichts *veraendert* - die 70 bestehenden Fingerabdruecke sind gleich geblieben. **Zum ersten Mal instrumentiert gegengeprueft:** Verify-Lauf 32988149012, `:app` API 35 und `:app-sim` API 26 beide gruen - damit sind auch die Migrationstests aus P1/P3/P4 wirklich gelaufen. Der rote Job dort (`Signiertes AAB`) scheitert an einem nie hinterlegten `KEYSTORE_BASE64` und laeuft nur bei `workflow_dispatch`. **Naechster Schritt:** PR #43 zeigt jetzt auf `main` und ist MERGEABLE; ein Merge loest `deliver-apk.yml` aus und ueberschreibt `Tama-debug.apk` in Drive. PR #42 ist dadurch gegenstandslos. |
| 2026-08-26 | P10 | **Football und Konfetti neu gezeichnet.** Football zeigte ein Tor am rechten Rand, dessen Pfosten auf der Kante des runden Ausschnitts sass und mit dem Ball verschmolz - jetzt eine Geste: Figur, Bein holt aus, trifft, Ball fliegt hinaus und rollt zurueck. Konfetti startete oberhalb des Rasters und hatte deshalb **einen einzigen Punkt** im ersten Frame; jetzt sind alle Spalten immer besetzt. Beide Luecken lagen im Werkzeug und sind geschlossen: der Kontaktbogen zeigt jetzt den RUNDEN Ausschnitt (vorher das Quadrat - er log ueber das Ergebnis), und `AnimationPreviewTest` prueft die mittlere Zellzahl (`MIN_ZELLEN_IM_MITTEL`). `sprite` liegt jetzt in `core/.../FrameSprite.kt` und wird von beiden Katalogen benutzt. 548 Tests gruen, Lint gruen, beide APKs bauen. **Fuer die naechste Sitzung:** Auf dem Geraet ist immer noch nichts davon gegengeprueft. |
| 2026-08-26 | P9 | Achtzehn Gruppen-Antworten in `AvatarReactions.groupAnswer`, requisitenfrei weil vererbt. Zwei Fehler dabei gefunden: Knoten mit eingebautem Typ bekamen die generische statt der Themen-Handlung, und `drop(1)` liess eine Untergruppe ihre eigene Antwort nicht finden. 37 Motive haben jetzt eine passendere Reaktion; die 30 Charakter-Motive unveraendert. **Damit ist der Umbau durch.** Offen: Einlagen fuer einzelne Blaetter (blockiert nichts) und die Geraetepruefung. |
| 2026-08-25 | P7+P8 | Vorschau-Werkzeug (`AnimationPreviewTest` → `core/build/preview/`) und alle 12 fehlenden Motive gezeichnet (`SkillTreeAnimations`). `pendingArtwork()` ist leer, jeder Knoten erreichbar. 547 Tests grün. **Für P9:** ab hier fehlen nur noch Choreografien — `AvatarReactions.groupAnswer` ist die leere Weiche dafür, und sie muss requisitenfrei bleiben (siehe Vererbungsregel bei P2). **Merken:** die Matrix ist RUND, bei y=11 nur x=2…10 nutzbar. |
| 2026-08-25 | P6 | Stufe 3 läuft: `AvatarActivity` + `AvatarActivityBus` (5 Minuten Lebensdauer) + `AvatarActivityPlans`. Einlage auf passende Tätigkeit schiebt sich ein, auf unpassende kommt erst der Wechsel. 18 neue Tests. **Abweichung:** `RoutineStep.Flourish` bewusst nicht gebaut — begründet im P6-Abschnitt. **Für P7/P8:** ab hier ist alle Mechanik fertig, es fehlt nur noch Pixelarbeit. Erste Adresse ist der Teller für `koerper/essen`, weil daran drei Knoten hängen. |
| 2026-08-25 | P5 | Zieh-Leiste unter dem Avatar + Baumbildschirm als Dialog. Ein Zug aus der Leiste gibt **kein XP** und schreibt kein Fütter-Ereignis (sonst speist sich die Neigung aus sich selbst). Logik in `SkillTreeRows` (8 Tests), Compose bleibt dünn. **Für P6:** `playFromBar` in `HomeScreen` ist die Stelle, an der Stufe 3 später eine laufende Tätigkeit braucht statt einer einmaligen Reaktion. Lint ist scharf — `FlowOperatorInvokedInComposition` hat zugeschlagen, Flows in Composables gehören in `remember`. |
| 2026-08-25 | P4 | Freischaltung steht: `AvatarUnlockedNode` (DB 23), `BranchAffinity` (Halbwertszeit 14 Tage, nur beantwortete Auslösungen), `UnlockOffers` (2+1), `AvatarUnlockRepository` — alles im neuen Paket `skilltree/`. 21 neue Tests. **Für P5:** `observeUnlockedNodes(profileId)` liefert der Zieh-Leiste einen Flow, `ReactionTrigger.ofNode(nodeId)` ist der fertige Weg von dort zur Reaktion. In der Leiste nur Knoten mit Motiv zeigen. |
| 2026-08-25 | P3 | `ReactionTrigger` ersetzt die zwei nullbaren Werte (Topic/Node/Untracked/None) — Begründung im P3-Abschnitt. `AvatarFeedEvent.nodeId` gefüllt für **beide** Quellen, DB `:app-sim` 22. 497 Tests grün, APK baut. **Für P4:** Neigung kann direkt `GROUP BY nodeId` auf `avatar_feed_events` rechnen, Altdaten sind nachgetragen; `fedAtMillis IS NOT NULL` filtert die beantworteten. **Gelöst:** der `G:\Meine Ablage`-Baufehler kam von einem alten Gradle-Daemon — `gradlew --stop` genügt, Details bei den Prüfbefehlen. Seither bauen beide APKs und `:core:test`. |
| 2026-08-25 | P2 | Auflösung läuft über den Baum: `AvatarReactions.forNode` + `AvatarSignatureReactions.forNode`. Alle 69 Fingerabdrücke unverändert (`app-sim/src/test/reaction-fingerprint.txt` — **vor** dem Umbau erzeugt, nie „anpassen"). 490 Tests grün. **Für P9 wichtig:** motiveigene Antworten werden NICHT nach unten vererbt, nur Gruppen-Antworten aus `AvatarReactions.groupAnswer` — Begründung im Abschnitt „Die Vererbungsregel". **Für P3:** Signatur von `reactionFor` steht noch auf `libraryAnimationLabel`, Aufrufliste ist in P3 eingetragen. |
| 2026-08-25 | P1 | `nodeId` an `LibraryAnimation`; `:app-sim` auf 21, **`:app` auf 20** (die Entity liegt in `:core` und trifft beide Datenbanken — war im Plan nicht vorgesehen). Nachtrag über `LibraryAnimationNodeIds.backfill` in `:core`, von beiden Migrationen benutzt. 483 Tests grün. **Für P2:** `AnimationTree.fallbackChain(id)` ist fertig und getestet — der Dispatch muss sie nur noch benutzen. Achtung, `:app` hat eine eigene `ReminderAnimations.kt`; sie kennt keinen Baum und soll ihn auch nicht kennen. |
| 2026-08-25 | P0 | Katalog steht: `AnimationNode`, `AnimationMotif`, `AnimationTree` (79 Knoten) + 27 Zeichenketten EN/DE + `AnimationTreeTest` (19 Tests, grün). **Für P1:** `AnimationTree.nodeIdFor(label)` und `nodeIdFor(type)` liefern die Migrations-Zuordnung fertig — nichts nachschlagen. `fallbackChain(id)` steht bereit für P2. Bilanz in der Tabelle oben korrigiert (52 Blätter, nicht 40). |
