# Dämonen- und Kampfstudien v0.2.8

Erstellt mit der integrierten Bildgenerierung, mit den bestehenden Charakteratlanten als Identitäts-/Stilreferenz. Vollauflösende erzeugte PNGs liegen in `assets/combat/`; `demons-v028.png` ist die Konzepttafel. Die Vorlage wurde nicht durch ein neues generisches Figurenmodell ersetzt.

## Promptset

Gemeinsame Vorgaben der Spieleratlanten: identity-preserve; Itoeva game-ready transparent combat animation atlas; precise established identity; fine coloured-pencil and painterly woodland storybook style; four columns × four rows; sixteen complete poses; stable head scale and torso mass; generous gutters; actual joint articulation; same baseline; no text, grids, ground, weapons, checkerboard or spell effects obscuring bodies.

- Fennec: orange fox, cream accents, rust cloak, teal clasp; right-facing; four paw attack, four Wildruf casting, two guard, two recoil, idle/alert/victory/kneeling.
- Starlet: rounded golden five-point star, indigo scarf and crescent pendant; star points act as limbs; right-facing tackle, gathering/raising/releasing light, folded-point guard, recoil and victory.
- Wyrmling: slender grey-green juvenile dragon, cream belly, red-orange crest/wings, curved tail; claw attack; lowering head/drawing breath/lifting/releasing rightward; wing guard, recoil, idle and victory.
- Gloop: original yellow-green moss creature with leaves, daisies and side satchel; tiny paw motions, no blue slime redesign; bump attack, paws gathering and extending for water casting, braced guard, recoil, idle/victory/exhaustion.
- Puffling: cream fluffy creature, blue ears/paws, sage hooded cape and satchel; small paw strike, gathering and sweeping paws with cape billowing, cloak guard, recoil and victory.
- Hootlet: original purple owl, gold spectacles, cream face disk/belly, indigo cloak with gold celestial stitching; wings act as arms; wing tackle, wings gathering/raising/sweeping moon magic, cloak/wing guard, recoil, perched idle and victory.

Dämonentafel: three original demons on warm parchment; Nachtwurz hero/orthographic/head/hand/foot studies and colour palette; lean charcoal bark/root body, asymmetrical branch horns, amber eyes, cyan sternum crystal, moss shoulders and indigo cloth; Aschenwicht ember-salamander imp; Schleierseher mothlike shadow spirit. Style reference: Fennec fine pencil fantasy chronicle art. No franchise character, gore or UI.

Nachtwurz atlas: exact concept identity, left-facing three-quarter view, 4×4 transparent layout; four idle, four claw strike, four dark-casting, two hurt and two collapsing defeat keyframes; clear articulated roots/limbs and same body mass, no smoke across cell borders.

Flame atlas: 4×4 genuine alpha; sixteen torch-flame loop phases, orange outer tongues, amber middle and ivory core, soft painterly edges, natural branching tongues and restrained embers, same anchored origin; no handle, wood, environment, text or neon triangle.

Die erzeugten Originaldateien bleiben erhalten. Die Runtime nutzt separate Bildausschnitte/Importe; Regionengrenzen wurden aus Alpharändern überprüft und in `data/combat_atlases.json` eingetragen.
