# Itoeva 2 - das aktive Spiel

Entschieden am 03.10.2026: Weg vom Erinnerungsspiel, hin zu einem Spiel, das man auf dem Handy
**selbst spielt**. Die Welt von Itoeva bleibt (Orte, Weltkarte, Kulissen, Licht, Musik, Arten,
Vorhaben, Reisen), aber ohne Erinnerungen und ohne Uhr. Fernziel: ein Fantasy-Strategiespiel im
Geist der neuen Pixel-Art-Final-Fantasy-Teile, in der Welt von Itoeva.

## Aufbau

- **Itoeva 1** ist eingefroren im Branch `itoeva-1` (Stand nach PR #322: Erinnerungsspiel,
  Uhr, Stream mit A-D und Fennec-Chat).
- **Itoeva 2** ist der Build-Typ `game` von `:app-sim` (`./gradlew :app-sim:assembleGame`):
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

## Naechste Stufen (Vorschlag, nicht freigegeben)

1. Handeln vor Ort: Taste "Aktion" an Stationen (Bank, Bett, Herd, Werkbank ...).
2. Begegnungen und Gespraeche mit den Bewohnern.
3. Kampf/Strategie im Stil der Vorlage (rundenbasiert), Gruppe, Faehigkeiten.
4. Fortschritt ohne Erinnerungen: Vorhaben und Reisen als Quests, die man selbst spielt.
