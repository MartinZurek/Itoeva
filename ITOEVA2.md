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
  autonome Schleife, keine Besuche; die Figur geht im Takt der Eingabe, bleibt nach dem
  Anhalten in ihrer Blickrichtung stehen und atmet.
- Querformat wie der Stream, Figurgroesse wie im Stream (88-132 dp).

## Stufe 2 - Handeln vor Ort (umgesetzt)

- Aktionstaste unten rechts (Ring mit Punkt, ohne Schrift), dazu Leertaste/Enter/E/Gamepad-A.
  Hell, sobald ein Platz in Reichweite ist (`PlayControl.stationInReach`, 12 % der Bildbreite).
- `PlayControl.actionAt`: Bett = hinlegen und schlafen, Sitz/Bank = hinsetzen und ruhen, Wanne,
  Schreibtisch/Arbeitsplatz = arbeiten, Tisch/Kuehlschrank = trinken, Regal = lesen, Werkbank,
  Spielautomat, Laden, Lampe und Fernseher an/aus. Dieselben Ablaeufe wie im autonomen Leben.
- Waehrend der Handlung gehoert die Figur dem Ablauf; danach geht es von dort aus weiter.

## Naechste Stufen (Vorschlag, nicht freigegeben)

1. Begegnungen und Gespraeche mit den Bewohnern.
2. Kampf/Strategie im Stil der Vorlage (rundenbasiert), Gruppe, Faehigkeiten.
3. Fortschritt ohne Erinnerungen: Vorhaben und Reisen als Quests, die man selbst spielt.
