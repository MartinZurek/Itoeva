# Herkunft der Kampfstudien-Bilder

Bildgenerierung mit dem eingebauten ImageGen-Werkzeug. Keine generierten 3D-Modelle.
Originalreferenz: `tools/character-art/source/fennec_key.png`; die Stadtrand-/Waldmalerei
ist die unveränderte Repository-Datei auf main `124186c`.

## Fennec: verwendeter finaler Atlas

Ausgabe: PNG RGBA, **887 × 1774**, 2 Spalten × 4 Zeilen. Die Tool-Ausgabe weicht von den
angefragten 1024 × 2048 ab. Sprite3D verwendet die tatsächlichen Dimensionen und individuelle
Fußanker. Zwei vorausgehende 4×2-Entwürfe hatten überlappende Zellgrenzen und sind verworfen;
sie werden nicht in der App geladen. Farben/Alpha des finalen Bilds bleiben unverändert.

Exakter finaler Prompt, mit Fennec-Schlüsselbild als Identitäts-/Stilreferenz:

> A production sprite sheet: PORTRAIT image 1024 pixels wide and 2048 pixels tall. Exactly TWO columns and FOUR rows, eight equal SQUARE cells, each 512x512. Eight isolated full-body painted drawings of the reference fox Fennec facing three-quarter RIGHT. Exact identity reference: tall fox ears, pointed muzzle, orange fur, ivory cheeks and fluffy tail tip, amber eyes, russet layered leaf cape, turquoise oval brooch, brown leather belt and boots, angular pencil contours and watercolor texture. Not chibi, not plush toy, not plastic 3D. IMPORTANT: draw each figure SMALL in the exact CENTER of its own SQUARE cell, maximum 350 pixels width and 370 pixels height including fingertips and entire cape and tail, with 80 pixels or more EMPTY TRANSPARENT SPACE to every edge. Never overlap cells; no cropping. All boots on the same 430-pixel baseline relative to each cell; same head/ear size, costume, scale and camera in every frame. 8 frames read LEFT to RIGHT top to bottom: 1 ready stance; 2 knees bend and hands pull to chest; 3 draw one arm back to prepare casting; 4 extend arm to right magic cast; 5 followthrough reaching right with swept cape; 6 lower arm; 7 return to stance; 8 exactly same ready stance as frame1. No particles, scenery, floor, shadows, letters or cell borders. Portrait TWO COLUMNS BY FOUR ROWS, true transparent background, fully separated sprites with wide blank transparent gutters.

Der Prompt ist keine automatische Qualitätsgarantie. Die Testszene zeigt den tatsächlichen
Atlas; Umfang-/Gesichts-/Stoffdetails variieren noch leicht. Frame 0 ersetzt Frame 7 für
die Ruhepose. Zusätzliche Zwischenbilder und manuelle Konsistenzarbeit bleiben offen.

## Waldweg-Bodenmaterial

Ausgabe: 1254 × 1254 PNG; für die App unverändert komponiert als JPEG Qualität 90 kodiert.
Godot importiert es mit maximal 1024 Pixeln und Mipmaps. Hintergrundpanorama mit maximal
2048 Pixeln; das Quell-PNG bleibt unverändert. Keine Bildladung im Kampftakt.

Exakter Prompt, Waldpanorama nur als Stil-/Palettenreferenz:

> Use case: stylized-concept. Asset type: square seamless game floor material texture for a 3D ground plane in a painterly 2.5D woodland scene. Use attached image as STYLE and palette reference only. Create one top-down orthographic close-up texture of a dry sandy woodland footpath, ochre earth, small irregular gray/brown pebbles, sparse dry leaves, subtle fine roots and occasional tiny moss tufts. Painterly brush texture and delicate pencil detail matching the reference forest landscape, warm subdued earth colors. Entire image is ground material viewed straight down with uniform scale and even soft ambient illumination. No horizon, no sky, no buildings, no creatures, no large rock props, no strong baked directional shadows, no vignette, no border, no text. Suitable for tiling, matching opposite edges, subtle irregularity throughout. 1024x1024.
