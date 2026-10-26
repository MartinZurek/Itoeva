# Moosling A4 – raeumlicher Arbeitsstand, keine fertige Spielfigur

Verbindlicher Einstieg: [SAVESPOT](../../docs/ITOEVA_3D_SAVESPOT_20261025.md).
Die freigegebenen Originalbilder liegen unter `docs/approved-3d-designs-20261025/`.
Die Bilder wurden in dieser Sitzung heruntergeladen, alle vier visuell geoeffnet
und gegen `manifest.json` auf Bytegroesse und SHA-256 geprueft.

## Vorhanden

- `build.py`: reproduzierbarer Blender-4.2-Generator, Seed 25102026.
- `output/moosling-a4.blend`: komprimierte editierbare Blender-Datei mit Geometrie,
  Materialien, eingebetteten Texturen, 20 Knochen, zwei Actions und Pruefbeleuchtung.
- `output/moosling-a4.glb`: frei betrachtbares Mesh mit Skin und Clips `walk`/`run`.
  Ein Modell fuer alle Ansichten, kein Wechsel zwischen Projektionsvolumen.
- `render.py`: rendert die gespeicherten Actions aus der Blender-Datei. Fester
  Schachbrettboden, bewegtes Rig, mitfahrende Kamera. Keine generierten Ersatzbilder.
- `validate.gd`: isolierter Godot-4.5.1-Import und Stichproben fuer Skalierung,
  endliche Gelenke, Pfotenhoehe und Standfussversatz im Modellmassstab.
- `output/validation.json` und `output/godot-validation.json`: technische Ergebnisse.

## Reproduktion

```sh
blender -b -t 4 --python tools/moosling-spatial/build.py -- --stills
blender -b -t 4 --python tools/moosling-spatial/render.py -- walk
blender -b -t 4 --python tools/moosling-spatial/render.py -- run
godot --headless --path tools/moosling-spatial --script validate.gd
```

Blender 4.2.0, Godot 4.5.1. Blender-Python bringt numpy und mathutils mit.
NLA-Tracks sind in der gespeicherten Datei zunaechst stumm; fuer Wiedergabe genau
einen der Tracks `walk` oder `run` aktivieren. Renderer erledigt das automatisch.
GLB enthaelt beide Animationen. Godot benennt Pfotenknochen wegen gleichnamiger
Mesh-Nodes in `hind-1_paw_2`, `hind1_paw_2`, `fore-1_paw_2`, `fore1_paw_2` um.

## Grenzen – vor Weiterarbeit lesen

**Visueller Qualitaetsmassstab nicht erreicht.** Gesicht, Augen, Ohren, Geweih,
Fellstruktur, Tasche und Silhouette sind noch sichtbar von der Freigabetafel
entfernt. Die Referenz bleibt verbindlich; dieser Versuch ist keine neue
Designfreigabe. Noch nicht auf die anderen drei Figuren uebertragen.

Godot: 4566 Checks, davon 14 Fehler in der strengeren Standfusspruefung. Maximaler
Versatz zwischen zwei Stichproben 0,018259 Modellmeter, maximaler Standhoehenfehler
0,005153 Modellmeter. Import, Clip-Vorhandensein, Endlichkeit und konstante
Knochenskalierung bestanden; das beweist weder schlupffreie Weltbewegung noch
finale Optik. Die fruehere 4024/0-Pruefung war weniger streng und ist ueberholt.

Gangzyklen sind im Modellmassstab (0,48 / 1,05 Meter pro Zyklus) gebaut. Der
Abgleich mit `WALK_SPEED=2.35` / `RUN_SPEED=3.85`, dem Figurenmassstab und dem
bestehenden Weltfortschritt fehlt. Keine Geschwindigkeit in der Spielwelt geaendert.
Anfahren, Abbremsen, Richtungswechsel, Gangwechsel, Kollisionen, Licht,
Ueberdeckungen und Android-Leistung sind nicht integriert oder geprueft.

Die Quelldatei bleibt ausserhalb `prototypes/godot-hd2d/assets/`. Kein APK-Einbau,
keine neue APK-Version, kein Actions-/Drive-Lieferlauf. Einige lokale Blender-
Prozesse meldeten beim Beenden `fatal library error, lookup self`; vorhandene
Dateien werden deshalb separat auf Vollstaendigkeit/Lesbarkeit geprueft.
