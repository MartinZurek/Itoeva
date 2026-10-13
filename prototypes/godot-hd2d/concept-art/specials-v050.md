# Produktionsprompts – Spezialkräfte v0.5.0

Erzeugung über die eingebaute Bildgenerierung, ohne CLI-Fallback. Referenz:
`assets/combat/fennec-combat.png`, ausschließlich für Stil und Proportionen.
Die Originalpixel bleiben unverändert; JSON-Dateien legen Bildregionen fest.
Die ausgewählten Assets werden direkt aus dem Projekt geladen.

## Mooslyn – assets/characters/mosslyn.png

Use case: stylized-concept. Production animation atlas, EXACT 8 columns x 6 rows,
48 isolated full-body poses on transparent alpha, uniform square cells, 4:3 canvas.
Small friendly bipedal fawn forest guardian, cream muzzle, chestnut fur, expressive
hazel eyes, short twig antlers with mossy leaves, olive leaf cape, teal acorn brooch,
leather satchel and tiny hooves. Match fine colored-pencil/watercolor storybook
JRPG texture of the Fennec reference, never depict Fennec. Same identity/scale
in every pose, meaningful limb articulation, no text, grid lines or backdrop.
Rows 1–4: side/front/back idle, side walk cycle, run, crouch, jump stages, sit,
front/back walk cycles, front/back sit, tumble stages, wave and tired kneel.
Rows 5–6: windup/lunge/attack/recover, green spell charge/lift/release/recover,
guard/crouch/hurt/recover, two combat idle poses, victory and defeat.

## Nocti – assets/characters/nocti.png

Same exact 8x6 production layout, alpha and pose order. Fennec and Mooslyn sheets
are only style/layout references. Adorable amethyst BAT spirit, huge pointed ears
with lavender inner skin, dark indigo fur, amber eyes, tiny cream muzzle, little
feet/hands, wing-cloak on arms and silver crescent brooch. No owl, antlers or horns.
Preserve identity in all 48 drawings. Soft walking/flutter cycles, jump flutter,
defensive wrapped wing cloak, purple echo magic, real articulated limb and wing
changes. Complete bodies with clear gutters, no labels or background.

## Kartenmotive – assets/combat/card-art-v050.png

EXACT 6 columns x 4 rows, 24 distinct painted magical vignettes; square cells,
colored-pencil/watercolor fantasy, readable at card size, no words, UI or borders.
Row 1: golden spark; leaf shield; herbal tea; botanical book; gold light arc; firebolt.
Row 2: healing spring; wind/feathers; moon beam; wooden shards; roots/healing crystal;
mossy healing well. Row 3: purple bat echo; wolf and forest cat in green portal;
falling constellation; fiery phoenix; turquoise tidal wave; two storm falcons.
Row 4: lunar eclipse; ancient emerald-heart tree guardian; violet bat mirror;
twig sprout; moss-and-bark guardian; moonlight echo bat. Each motif separate and
complete, no inter-cell overlap. The resulting painted backdrops are retained
inside their vignette regions, not used as transparent creature cutouts.

## Helferidentitäten – assets/combat/spirits-v050.png

EXACT 4 columns x 2 rows, true transparent alpha, isolated complete bodies,
watercolor/colored-pencil texture, each facing slightly right. Row 1: cream-headed
twig sprout, emerald moss/bark guard, purple echo bat, indigo silver-cloak bat guard.
Row 2: red-gold phoenix, silver-blue storm falcon, ancient emerald-heart tree guardian,
translucent violet mirror bat. No backdrop, labels, borders or ground rectangle.

## Arenaanimationen – assets/combat/spirit-animation-v050.png

Identity reference: the eight helper cutouts above. EXACT FOUR columns x EIGHT rows,
32 separate alpha sprites, one helper species per row in the same order. Each
row: idle, subtle second idle, articulated forward attack, defensive hit reaction.
Consistent body scale, feet baseline and identity; wing/limb changes instead of
rigid rotations or scaling. No text, grid lines, backdrop or overlaps.

Final corrective edit prompt: keep the exact 4x8 grid, creatures, colors and four
poses; reduce each drawing substantially inside its cell, leave wide transparent
gutters, reconstruct clipped wing tips and twig hands, keep the same full anatomy
and painted texture. Do not alter row order or add/remove creatures. Original
identity sheets and the chosen corrected animation sheet remain in the project.
