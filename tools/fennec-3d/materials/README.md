# Gemalter Materialatlas

`painted-atlas.jpg` ist der ausgewaehlte Projektatlas. Er wurde mit dem
eingebauten ImageGen-Werkzeug erzeugt, mit `fennec_key.png` als Stil-/Farbreferenz.
1254 x 1254 Pixel; vier Quadranten: orangefarbenes Fell, cremefarbenes Fell,
rostrotes Blattgewebe, braunes Leder. Das PNG-Ergebnis wurde fuer das GLB als
JPEG (Qualitaet 87) kodiert; Bildinhalt und Groesse bleiben erhalten.

Die UVs halten einen Rand zu jedem Quadranten. Alle Texturbytes werden direkt
in das GLB eingebettet. Keine Textur aus einer Charakteransicht: auch Ruecken,
Seiten und die vereinte Kopfform bleiben wirklich dreidimensional.

## Verwendeter Prompt (built-in ImageGen)

Use case: stylized-concept. Asset type: a square 1024x1024 four-quadrant albedo
MATERIAL TEXTURE ATLAS for a real 3D Fennec model, NOT a drawing of a character.
The input image is style and palette reference only: woodland ink-and-gouache
fox illustration. Produce ONE atlas image with exactly four equal square
quadrants, edge-to-edge, no spacing, borders, words or objects: top left orange
russet FOX FUR with fine flowing cream and dark russet hair strokes, top right
pale warm ivory FOX FUR with soft wispy honey and umber hair strokes; bottom left
weathered rusty red leaf CLOAK FABRIC with subdued leaf-vein motifs, folded woven
grain and dry watercolor brush strokes; bottom right dark walnut BROWN LEATHER
with tiny subtle stitches, worn scuffs and leather grain. Every quadrant is a
close-up continuous repeating material field, fills all its square. Small-scale
hand-painted pencil and gouache marks, varied matte surface detail, subdued warm
earth tones directly matching reference. Texture-only, completely flat diffuse
neutral light, no cast shadow, no simulated sphere, no face, no eye, no 3D render,
no mascot, no patch labels. Maintain clean exact four-quadrant layout; texture
detail much finer than quadrant size, patterns distributed across each quadrant,
no giant feature at center. The purpose is to remove the smooth plastic toy
appearance from the real 3D geometry.
