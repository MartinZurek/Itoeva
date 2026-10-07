# Itoeva 2 - das aktive Spiel

Entschieden am 03.10.2026: Weg vom Erinnerungsspiel, hin zu einem Spiel, das man auf dem Handy
**selbst spielt**. Die Welt von Itoeva bleibt (Orte, Weltkarte, Kulissen, Licht, Musik, Arten,
Vorhaben, Reisen), aber ohne Erinnerungen und ohne Uhr. Fernziel: ein Fantasy-Strategiespiel im
Geist der neuen Pixel-Art-Final-Fantasy-Teile, in der Welt von Itoeva.

## Aufbau

- **Itoeva 1** ist eingefroren im Branch `itoeva-1` (Stand nach PR #322: Erinnerungsspiel,
  Uhr, Stream mit A-D und Fennec-Chat).
- **Itoeva 2** (in Drive: `Itoeva2-debug.apk`) ist der Build-Typ `game` von `:app-sim` (`./gradlew :app-sim:assembleGame`):
  eigene applicationId (`com.notime.glyphminderwatch.itoeva2`), Name "Itoeva 2", installiert
  sich **neben** der bisherigen App. Schalter: `R.bool.game_mode` (`app-sim/src/game/res`).
- **Der Stream** (Build-Typ `stream`) bleibt autonom: Dort steuert sich der Avatar selbst.
- Die gewoehnliche App (`debug`/`release`) bleibt unveraendert.

## Stufe 1 - Steuerung (umgesetzt)

- `PlayControl` (reine Logik, getestet): links/rechts laufen, hoch/runter = **Tiefe im Bild**
  (hoch nach hinten zur Bodenlinie, runter nach vorn). Laeuft man an einen Rand weiter
  (gut eine Viertelsekunde druecken), geht es zum Nachbarort in dieser Richtung - nach der
  Weltkarte (`PlayMap`, Lage aus `PlayMapScene.gridOf`). Daheim liegen die Zimmer an einem Flur,
  ganz rechts geht es auf die Strasse. Jeder Ort ist zu Fuss erreichbar.
- `GameControls.kt`: Steuerkreuz unten links (halten = gehen) sowie Pfeiltasten/WASD.
- DockScreen im Spielmodus: keine Erinnerungen, keine Uhr, keine Speicherplaetze, keine
  autonome Hauptfigur-Schleife; Bewohner leben unabhängig davon; die Figur geht im Takt der Eingabe, bleibt nach dem
  Anhalten in ihrer Blickrichtung stehen und atmet.
- Querformat wie der Stream, Figurgroesse wie im Stream (88-132 dp).

## Stufe 2 - Handeln vor Ort (umgesetzt)

- Aktionstaste unten rechts (Ring mit Punkt, ohne Schrift), dazu Leertaste/Enter/E/Gamepad-A.
  Hell, sobald ein Platz in Reichweite ist (`PlayControl.stationInReach`, 12 % der Bildbreite).
- `PlayControl.actionAt`: Bett = hinlegen und schlafen, Sitz/Bank = hinsetzen und ruhen, Wanne,
  Schreibtisch/Arbeitsplatz = arbeiten, Tisch/Kuehlschrank = trinken, Regal = lesen, Werkbank,
  Spielautomat, Laden, Lampe und Fernseher an/aus. Dieselben Ablaeufe wie im autonomen Leben.
- Waehrend der Handlung gehoert die Figur dem Ablauf; danach geht es von dort aus weiter.

## Stufe 3 - Finger, Wege, Karte, Rucksack, Musik (umgesetzt, Rueckmeldung vom 03.10.)

- **Ziehen statt Steuerkreuz** (`GameTouch`): Finger aufsetzen und in die Richtung ziehen, in die
  die Figur gehen soll; sie laeuft, solange der Finger liegt. Ein Ring zeigt den Daumen-Joystick.
- **Doppeltipp statt Aktionsknopf**: zweimal kurz auf Bett, Bank, Regal, Kuehlschrank ... - die
  Figur geht hin und handelt. Doppeltipp auf eine Tuer geht hindurch (`PlayControl.doorTarget`:
  Zimmer -> Flur, Wohnzimmer -> Strasse, Laden/Cafe/Arbeit/Spielhalle -> nach draussen).
- **Sichtbare Wege** (`PlayControl.exitMarks`): pulsierende Pfeile mit Steinspur an jedem Rand,
  an dem es weitergeht (links, rechts, nach hinten, nach vorn), und ueber jeder Tuer.
- **Figur antippen = Menue**: Karte, Rucksack, Musik an/aus.
- **Karte** (`GameMapOverlay`): die Weltkarte mit Ortsnamen, eigener Standort hervorgehoben;
  einen Ort antippen zeigt den Weg dorthin.
- **Rucksack** (`PlayBackpack`, 8 Plaetze, gespeichert): Fundstuecke aus Handlungen (Kuehlschrank
  -> Essen, Tisch -> Becher, Regal -> Buch, Ladenregal -> Samen, Kasse -> Korb, Werkbank -> Holz).
  Antippen wählt ein Ding. „Tragen“ nimmt es in die Hand; „In die Ablage“ schafft reversibel Platz.
- **Musik und Ton** sind beim ersten Start an (ab Werk stehen sie aus), danach Schalter im Menue.
  Hinweis: Bei Lautlos/Vibration am Geraet schweigt die Musik bewusst.

## Stufe 4 - Gemalte Welt (umgesetzt, Wunsch vom 04.10.)

Weg von der groben LED-Kulisse: Orte als feine Pixel-Art mit Tiefe, gemalt als Code
(`tools/world-art`, 480 x 270 Bildpixel, nur in der Spiel-Variante unter
`src/game/assets/scenes`). Nach dem gemergten Welt-PR #334 sind **16 Orte** als Bild und Katalogeintrag vorhanden; neun weitere Orte zeigen noch die ältere Kulisse, nach den Weltstudien in
`docs/concept-art/world-studies/` (siehe `tools/world-art/README.md`).
`GameScenes` beschreibt je Bild Gehflaeche (Trapez in die Tiefe), Figurgroesse nach Tiefe,
antippbare Plaetze und Ausgaenge; die Angaben stehen im generierten `GameSceneCatalog`.
Doppeltipp laesst die Figur selbst hingehen. Offen: Tageszeiten je Ort, Pruefung am Geraet.

## Stufe 4a - Lebendige Darstellung (implementiert, Geräteabnahme offen)

Im gemalten Spielmodus liegen Laufweg, Kamera und Bildausschnitt auf derselben
480x270-Koordinatenbasis. Tageslicht, schaltbare Laternen, Fernseher und Lagerfeuer
erzeugen Laufzeit-Lichtfelder; die Figur bekommt einen mitwandernden Bodenschatten und
eine zur Beleuchtung passende Helligkeit. Waldlaub und Wasser erhalten wenige
deterministische Bewegungen. Die sechs Figuren haben eigene Front- und Rueckansichten
fuer Hoch-/Runtergehen sowie Schrittbilder; links/rechts benutzen die vorhandene
gespiegelte Seitenansicht. Die Lichtschicht ist eine 2D-Naeherung, keine vollstaendige
Schattenberechnung aller Kulissenobjekte. Geraetepruefung und Clip-Paritaet stehen aus.

## Startbildschirm und Wege zurueck (Wunsch vom 04.10.)

- **Startbildschirm** (`GameStartScreen`): Beim Oeffnen von Itoeva 2 waehlt man zuerst sein
  Wesen (alle sechs mit Bild, das gewaehlte atmet) und schaltet die Musik; "Spielen" startet.
  Die Wahl landet in `AvatarSpeciesPrefs`, ein anderes Wesen baut den Spielbildschirm frisch auf.
- **Zurueck aus dem Spiel**: Knopf "☰ Menue" oben links (immer sichtbar), im Menue der Punkt
  "Einstellungen" fuehrt zum Startbildschirm. Die Zurueck-Geste schliesst offene Fenster, sonst
  oeffnet sie das Menue.
- **Karte und Rucksack** haben einen sichtbaren Knopf "← Zurueck"; ein Tipp daneben schliesst sie
  weiterhin.

## Stufe 5 - Spielbare Welt (freigegeben am 06.10., erster Implementierungsschnitt)

Versionierter Spielstand, einmalige Wald- und Stationsfunde, reversible Ablage, Pflanzen und
Reparaturen mit dauerhaftem Gegenstandsverbrauch, freiwillige Bewohnergespräche und Chronik.
Bewohner bewegen sich in derselben Bildgeometrie wie der Spieler. Eine gespeicherte Spielzeit
verbindet Licht, Außenregen und Klang; Menüs und Abwesenheit pausieren sie. Der gemeinsame
Weltstand bleibt beim Wechsel des wählbaren Wesens erhalten.

Umfang, technische Entscheidungen, offene Abnahmen und die Pakete WB-01 bis WB-10 stehen in
[docs/itoeva2-worldbuilding.md](docs/itoeva2-worldbuilding.md). Die nächsten Innenraumbilder,
beidseitige Beziehungen und taktischer Kampf werden dort als Folgeetappen geführt.

## Stufe 6 - Gemeinsame Landschaft und Kamera (erster Schnitt, 07.10.)

Strasse, Park, Wiese und Wald bilden einen begehbaren Landschaftsweg ohne Szenenblende an den
inneren Raendern. Kamera mit Nachlauf, Blickvorlauf und sanftem Nah-/Laufzoom; Figur und
Touch-Ziele teilen dieselbe Projektion. Neue Landschaftskarte statt der alten LED-Zeichen.
Alle Orts-IDs und Spielstaende bleiben lesbar. Gebaeude und die anderen Gegenden behalten
vorerst ihre Uebergaenge; die neun alten Innenraeume sind noch das naechste Bildpaket.
Abnahme, Vorschau, Grenzen und Ruecksetzung: [Gemeinsame Landschaft](docs/itoeva2-continuous-world.md).
