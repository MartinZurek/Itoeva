# Figurenmassstab und Moebelkontakt

Stand: 08.10.2026. Branch: `codex/world-character-scale`.
Basis: `70b7ee9959db091b4e75e1277978719a5895628d`, Sichtbarkeitskorrektur PR #345.
Ziel ist die native Itoeva-2-Welt. Kein Godot-/Sprite-/Bildasset-Umbau.

## Ergebnis

- Alle sechs Figuren besitzen feste Standreferenzen statt eines pauschalen Bildfuellfaktors.
  Fennecs Referenz umfasst 83 Pixel Koerper und 109 Pixel mit Ohren. Transparentes
  Padding, Schwanz oder Fluegel bestimmen die Groesse nicht mehr. Keine neue
  Normierung einzelner Lauf-/Sitzphasen, damit Animationen nicht groessenpumpen.
- Fennec vor der Parkbank: vorher rund 73, jetzt 110 Weltpixel mit Ohren.
  Relative Standhoehen: Fennec 1.00, Gloop .91, Puffling .93, Wyrmling 1.19,
  Starlet .90, Hootlet .86. Spieler und NPCs verwenden dieselbe Projektion.
- Aussen bleibt die Hoehe ueber alle 17 Orte stetig. Innen sind Koerperhoehen
  an die unterschiedlich gemalten Moebel angepasst. Kamera-Zoom aendert das
  Verhaeltnis zwischen Figur und Moebel nicht.
- Fussanker bleiben auch bei einem negativen oberen Layout-Rand auf dem Boden.
  NPC-Trefferflaechen folgen jetzt der Silhouette, einschliesslich Ohren/Schweif.
- Alle elf Innenraeume besitzen korrigierte Aktionsanker. Parkbank, Sofa,
  Teetisch, Bett, Kuechen-/Cafetisch verwenden vermessene Plattformen und Konturen.
  Jeder Charakter wird separat verdeckt; Vordergrund-NPCs werden nicht mit uebermalt.
- Wasser bedeckt dieselbe Bodenhoehe fuer alle Spezies. Die Game-Sprungreichweite
  betraegt 78 Weltpixel; die 59-/68-Pixel-Tische sind damit tatsaechlich erreichbar.
  Der Standard anderer Clients bleibt 52.

## Belege und Regeneration

- `tools/world-art/character-scale-comparison.png`: gleicher Fussstandort, links
  bisherige Groessenrechnung/Innenhoehen, rechts neuer Massstab in Park, Wohnzimmer,
  Kueche und Cafe. Neue Aktionsanker werden auf beiden Seiten verwendet, um den
  Groessenvergleich nicht mit einer Positionsaenderung zu vermischen.
- `character-scale-family.png`: alle sechs Wesen vor derselben Bank.
- `character-scale-contact.png`: hinter/vor/auf der Bank.
- `character-scale-jumps.mp4`: echte Kotlin-Sprungboegen und Landungen auf beiden Tischen.

Das sind Desktop-Renderbilder, keine Android-Aufnahme. Die Bildvorschauen sind
auf 256 Farben komprimiert; die Sprungvorschau laeuft fuer die Lesbarkeit verlangsamt. Originale Hintergrundbilder
und 138er-Sprite-Boegen sind unveraendert.

Nach `bash tools/reaction-preview/tests.sh`:

```bash
tools/reaction-preview/.work/kotlinc/bin/kotlinc tools/world-art/preview_scale.kt \
  -cp tools/reaction-preview/.work/tests -Xfriend-paths=tools/reaction-preview/.work/tests \
  -d tools/world-art/.work/scale-classes
java -Djava.awt.headless=true \
  -cp tools/world-art/.work/scale-classes:tools/reaction-preview/.work/tests:tools/reaction-preview/.work/kotlinc/lib/kotlin-stdlib.jar \
  com.notime.glyphsim.matrix.Preview_scaleKt . tools/world-art/.work/scale
```

## Pruefstand und Fortsetzung

954 Kotlin-Tests gruen, darunter zehn neue Massstabs-/Kontaktpruefungen.
`git diff --check` sauber. Android-CI/Compose/Lint/R8/API 26/35 ist getrennt und
muss am aktuellen PR-Kopf geprueft werden; der PR beschreibt ihren aktuellen Stand.

1. PR-Kopf und `git status` lesen; PR #345 ist in der Basis enthalten, kein zweiter
   unabhaengiger Fix. Bei inzwischen gemergter #345 den neuen main-Diff pruefen.
2. Android-Verify vollstaendig abschliessen.
3. Vor Merge/Telefonabnahme: Groesse aller Wesen, Bank/Innenraumtische,
   Touch nach Zoom, Sprung/Landung, Wasser und Ortsgrenzen kontrollieren.
4. Die uebrigen Innenmoebel haben genaue Aktionsanker, aber noch keine vollstaendigen
   Konturen/Kollisionskoerper. Eine neue Sitzchoreografie wurde nicht gebaut.

Keine Room-/Spielstandmigration, kein Signierungs-/Workflow-Umbau. Ruecksetzung
ueber Ruecknahme dieses PRs. App 1 und Stream behalten ihre bisherige Darstellung.
