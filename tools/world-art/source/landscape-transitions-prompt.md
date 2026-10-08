# Generierungsbriefs: Zwischenorte, 08.10.2026

Drei getrennte Generierungen mit dem eingebauten Imagegen, bestehende Panoramen als
Stil- und Anschlussreferenzen. Originale unverändert übernommen, 2172 × 724, PNG.

Gemeinsam: stylized-concept, handgemalte warme Gouache/Ölfarben, sonniger Tag, 3:1,
gleiche Kamera und Bildhöhe wie die Referenzen. Keine Figuren, Schrift, UI oder Rahmen.
Begehbarer durchgehender Weg bei 72–88 Prozent der Bildhöhe, dekorative Pflanzen vorn.
Die linke Bildkante setzt die rechte Kante der ersten Referenz fort; die rechte Kante
schließt an die linke Kante der zweiten Referenz an. Vegetation, Licht, Horizont und
Wegmaterial schrittweise verändern, keine Montage.

- **coast-path.png:** coast.png → street-park-forest.png. Dichter Küstenbewuchs,
  türkisfarbener Bach, alte Steinbrücke, Blick zurück zum Meer, Wegweiser ohne Text,
  Ruhebank, niedrige Stützmauer, trockene gepflasterte Dorfzufahrt, steinernes Haus
  mit Terrakottadach am rechten Bildrand.
- **village-edge.png:** street-park-forest.png → uplands.png. Birken-/Kiefernwald
  lichtet sich zum alten Apfelgarten mit Gartentor, Bienenkorb und gestapeltem Holz
  hinter niedriger Steinmauer; Ruhebank vor Bergblick, Gartenhaus und Dorfzugang.
- **mountain-pass.png:** uplands.png → expedition.png. Sonniger alpiner Felshang
  mit Kiefern und Stützmauer wird zum geschützten Waldsattel; Steinmännchen und
  Wegweiser neben ebenem Weg, Berg-/Seeblick zwischen den Bäumen, Laubwald zum Lager.
  Kein Zelt/Feuer im Zwischenbild, keine Stufe oder Klippe im Laufweg.

Der Küstenweg benötigt den in GameWorld hinterlegten schmaleren Brückenboden.

## Nachgemalte Anschlüsse

Sechs Edit-Aufträge mit dem eingebauten Imagegen. Referenz je 960 × 640:
Panoramen für die Referenz auf 1920 × 640 skaliert, die letzten 480 Pixel des
linken und ersten 480 Pixel des rechten Bildes nebeneinander. Zuordnung:
0 coast → coast-path; 1 coast-path → street-park-forest;
2 street-park-forest → village-edge; 3 village-edge → uplands;
4 uplands → mountain-pass; 5 mountain-pass → expedition.
Ausgaben unverändert als `world/seams/0.png` bis `5.png`, 1536 × 1024.

Edit-Brief: Keep the exact composition, camera, ground height, lighting and all
landmarks of this joined landscape crop. Preserve the outer 300 pixels on each
side. Repaint only the central 360-pixel join into one coherent hand-painted
landscape: connect the path and foreground stones, continue vegetation, rooflines
and sky naturally. Remove the vertical collage seam without blurring or stretching
pixel columns. Do not zoom, crop, recompose, add people, text, UI or frames.
Match the warm gouache/oil-paint texture of the reference.

Der Renderer projiziert jeden Anschluss ortsfest auf 960 × 640 Weltpixel.
Die mittleren 704 Weltpixel sind deckend; die äußeren 128 je Seite laufen weich
in die Basisbilder aus. Boden- und Türgeometrie bleiben unverändert.

