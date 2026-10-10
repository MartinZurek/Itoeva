# Living-Atlanten, 10.10.2026

Erzeugt und korrigiert mit dem integrierten ImageGen. Identitaetsreferenzen aus
`tools/character-art/source/`: `fennec_key.png`, `gloop-refined-atlas.png`,
`puffling-refined-atlas.png`, `wyrmling-refined-atlas.png`,
`starlet-refined-atlas.png`, `hootlet-refined-atlas.png`.
Fennec benutzt zuerst die separat korrigierte Mantelreferenz.
Die bestehenden Ensemble-/Mobility-Prompts bleiben die Stilvorlage.

## Export und Integration

`*-living-atlas.png`: genau 512x1024, vier Spalten, acht Zeilen,
128px-Zellen, mindestens 26px transparenter Rand pro Zellenseite,
Farbwerte ausschliesslich aus dem vorhandenen Spielbogen derselben Figur.
`*-living-master.png`: 1024x2048, 256px-Zellen, mindestens 52px Rand;
diese hoeher aufgeloesten Quellen speisen den Spieleexport ohne Vergroesserung.
Kein Artwork wird an festen
Rohbild-Zellengrenzen abgeschnitten: erst 32 vollstaendige Komponenten erkennen,
danach mit EINEM Massstab pro Charakter registrieren. Sitzkorrekturen werden
an der daneben gezeichneten Standanatomie kalibriert, nicht an der Zellhoehe.

`app-sim/src/main/assets/creatures/*-living.png`: die gleichen 32 Zeichnungen
als 4096x128-Spielstreifen mit Bodenzeile 125. Die bisherige sichtbare Standhoehe
bleibt durch `living-atlas-manifest.json` und `Living.scaleFor` erhalten.
Die bisherigen 138 Gang-/Aktionsrollen bleiben pixelgleich; Zusatzrollen sind
138..169. Android liest beide Streifen einmal und zerlegt sie vor dem Zeichnen
in kleine Texturen. Fehlt der Zusatzbogen, bleibt der alte Renderer verwendbar.

Regeneration mit Node, `sharp` und `pngjs`:

```sh
node tools/character-art/living_atlases.js
node tools/character-art/test_living_atlases.js
```

Nur ein erneuter Rohbildimport braucht `--import RAW_DIRECTORY` mit sechs PNGs.
Die zusaetzliche anatomische Sitzquelle kann dabei mit
`--posture POSTURE_PNG` angegeben werden. Die eingecheckten 4x8-Master
sind die vollstaendige, direkt reproduzierbare Exportquelle im Repository.

## Laufzeit und Abnahme

Aktives Game: eigene vierteilige Frontatmung und seitliche Gewichtsverlagerung;
kurze seitliche Start-/Stoppposen; Landereaktion nach abgeschlossenem Sprung;
Wendebilder, frontales Umschauen, kurze Gewohnheiten und seitliche Sitzruhe.
Stimmung kommt ausschliesslich aus `rememberAvatarMood`, nicht aus Wartezeit.
Richtungs-/Schlaf-/Handlungsvorrang und laufende Gangarten bleiben erhalten.
Die Standzeit beginnt pro Ansicht lokal neu; nach Lifecycle-Pausen wird kein
Brems-/Landeereignis erfunden. Die Bildregie schreibt keinen Agentenzustand.

**Bekannte Quellgrenzen:** Einzelne generierte Blick-/Stimmungsposen sind noch
naeher am Dreiviertel-/Profilblick als am verlangten strengen Frontblick.
Gloops achter Wendepose entspricht noch ein Profil: die Runtime verwendet fuer
Rueckendrehungen deshalb die bestehende gepruefte Rueckenzeichnung.
Die Gewohnheiten sind kurze gezeichnete Gesten, keine zusaetzlichen vollstaendigen
vierphasigen Grooming-/Shake-Clips. Keine Behauptung, dass jeder Art-Detailwunsch
pixelgenau erfuellt oder die Darstellung am Telefon schon abgenommen ist.

**Pruefung:** Node prueft alle 192 Posen, Raster, Raender, binaren Alpha,
Paletten, feste Atemkontakte, kuerzere Sitzanatomie, Weltgroesse und
pixelidentische Regeneration. Die Kotlin-Strecke prueft Laufzeitvorrang,
Start/Stop/Landung, Zeitluecken, Stimmung und alle ausgelieferten Bilder.
Lokaler Android-Build bislang am Netzwerkabruf der Gradle-Distribution
blockiert; Android-Kompilierung und UI bleiben der CI bzw. dem Geraet vorbehalten.

## Korrekturprompts

### fennec

Use case: identity-preserve. Correct the supplied Itoeva animation atlas without redesigning its character or outfit. NEW PRODUCTION PIXEL ART ATLAS. EXACT 4 columns x 8 rows of equal square cells, 32 complete separate drawings, portrait canvas ratio 1:2. Genuine transparent background. Every complete figure and appendage contained in CENTRAL 60% of its cell with 20% transparent margin on each edge. Same physical scale, head size, palette throughout. True crisp 128px pixel art with deliberate pixel clusters and clean edges, no smooth stickers, no shadow/ground/glow/text/lines/symbols/scenery. All side views face RIGHT. Accessories stay on same anatomical side.
Correct incorrect orientations and duplicate poses. Each row's four drawings depict visible subtle articulated posture changes, supporting weight, secondary lag of ears/tail/fabric/feathers and settling; never only blinking or translating a static sticker. Ground contacts stay planted where requested. Sitting naturally shorter, not enlarged.
R1 FRONT breathing, EYES OPEN all four: inhale chest raised; top of breath; exhale settling; relaxed rest. Identical support contact.
R2 RIGHT SIDE weight shift: back support; transferring forward; front support; settling back. Four distinct support/posture phases.
R3 turning: RIGHT profile; three-quarter toward viewer; full front; three-quarter AWAY from viewer facing rear-right (BACK of head and shoulder, do NOT face LEFT).
R4 RIGHT SIDE start/stop: anticipate lean forward; first push-off; brake leaning BACK with appendages swinging FORWARD; settled with appendages swinging BACK.
R5 FRONT lower body planted in all four, only upper body/head looks LEFT; RIGHT; UP; DOWN. Do not turn whole body to profile.
R6 ALL FRONT moods: happy small bounce; sad lowered head; hungry touching belly looking around; tired WIDE YAWN eyes shut. Do not turn hungry to profile.
R7 ALL RIGHT SIDE landing: compressed low; overshoot rising; balance wobble with support underneath body; settled upright.
R8 habits: whole-body shake-off; species-specific grooming; friendly FRONT greeting viewer; sitting and fidgeting. No detached motion symbols.
Draw every specified pose, 32 complete figures, no missing row. Preserve identity and attached costume.
FENNEC orange/cream desert fox, huge upright ears, amber eyes, turquoise round brooch, brown gloves, pouch belt, compact legs boots and large cream-tipped tail. Preserve corrected cloak ONE continuous compact rust-red piece from both shoulders behind body to hips. NO horizontal cloak tail or dangling rag beside hand. Knees under pelvis, distinct boots. Sad flattened ears, curious perked ears, happy tail swish. Grooming lick paw then wipe ear. For r3c4 show rear-right three-quarter, NOT existing left-facing pose. Same corrected key-art identity, pixel interpretation.

### gloop

Use case: identity-preserve. Correct the supplied Itoeva animation atlas without redesigning its character or outfit. NEW PRODUCTION PIXEL ART ATLAS. EXACT 4 columns x 8 rows of equal square cells, 32 complete separate drawings, portrait canvas ratio 1:2. Genuine transparent background. Every complete figure and appendage contained in CENTRAL 60% of its cell with 20% transparent margin on each edge. Same physical scale, head size, palette throughout. True crisp 128px pixel art with deliberate pixel clusters and clean edges, no smooth stickers, no shadow/ground/glow/text/lines/symbols/scenery. All side views face RIGHT. Accessories stay on same anatomical side.
Correct incorrect orientations and duplicate poses. Each row's four drawings depict visible subtle articulated posture changes, supporting weight, secondary lag of ears/tail/fabric/feathers and settling; never only blinking or translating a static sticker. Ground contacts stay planted where requested. Sitting naturally shorter, not enlarged.
R1 FRONT breathing, EYES OPEN all four: inhale chest raised; top of breath; exhale settling; relaxed rest. Identical support contact.
R2 RIGHT SIDE weight shift: back support; transferring forward; front support; settling back. Four distinct support/posture phases.
R3 turning: RIGHT profile; three-quarter toward viewer; full front; three-quarter AWAY from viewer facing rear-right (BACK of head and shoulder, do NOT face LEFT).
R4 RIGHT SIDE start/stop: anticipate lean forward; first push-off; brake leaning BACK with appendages swinging FORWARD; settled with appendages swinging BACK.
R5 FRONT lower body planted in all four, only upper body/head looks LEFT; RIGHT; UP; DOWN. Do not turn whole body to profile.
R6 ALL FRONT moods: happy small bounce; sad lowered head; hungry touching belly looking around; tired WIDE YAWN eyes shut. Do not turn hungry to profile.
R7 ALL RIGHT SIDE landing: compressed low; overshoot rising; balance wobble with support underneath body; settled upright.
R8 habits: whole-body shake-off; species-specific grooming; friendly FRONT greeting viewer; sitting and fidgeting. No detached motion symbols.
Draw every specified pose, 32 complete figures, no missing row. Preserve identity and attached costume.
GLOOP moss green low round legless slime, dark oval eyes, leaf shoulders with cream flower, two-leaf sprout, MATTE amber seed inside, small brown satchel. NO legs/feet/fingers; body breathing swells, support shifts as base wave, sad low puddle drooping sprout, shake-off jelly ripple. Grooming tenderly rub flower/leaf with one soft lobe, greeting SHORT rounded extruded arm NO angular finger/hand/extra leaf. Keep body matte, no halo. IMPORTANT r3c4 rear-right view shows leafy BACK shoulder without frontal face; r5 only face/upper mass turn with base planted. r6 hungry and yawning FRONT. All 32 full figures at common size.

### puffling

Use case: identity-preserve. Correct the supplied Itoeva animation atlas without redesigning its character or outfit. NEW PRODUCTION PIXEL ART ATLAS. EXACT 4 columns x 8 rows of equal square cells, 32 complete separate drawings, portrait canvas ratio 1:2. Genuine transparent background. Every complete figure and appendage contained in CENTRAL 60% of its cell with 20% transparent margin on each edge. Same physical scale, head size, palette throughout. True crisp 128px pixel art with deliberate pixel clusters and clean edges, no smooth stickers, no shadow/ground/glow/text/lines/symbols/scenery. All side views face RIGHT. Accessories stay on same anatomical side.
Correct incorrect orientations and duplicate poses. Each row's four drawings depict visible subtle articulated posture changes, supporting weight, secondary lag of ears/tail/fabric/feathers and settling; never only blinking or translating a static sticker. Ground contacts stay planted where requested. Sitting naturally shorter, not enlarged.
R1 FRONT breathing, EYES OPEN all four: inhale chest raised; top of breath; exhale settling; relaxed rest. Identical support contact.
R2 RIGHT SIDE weight shift: back support; transferring forward; front support; settling back. Four distinct support/posture phases.
R3 turning: RIGHT profile; three-quarter toward viewer; full front; three-quarter AWAY from viewer facing rear-right (BACK of head and shoulder, do NOT face LEFT).
R4 RIGHT SIDE start/stop: anticipate lean forward; first push-off; brake leaning BACK with appendages swinging FORWARD; settled with appendages swinging BACK.
R5 FRONT lower body planted in all four, only upper body/head looks LEFT; RIGHT; UP; DOWN. Do not turn whole body to profile.
R6 ALL FRONT moods: happy small bounce; sad lowered head; hungry touching belly looking around; tired WIDE YAWN eyes shut. Do not turn hungry to profile.
R7 ALL RIGHT SIDE landing: compressed low; overshoot rising; balance wobble with support underneath body; settled upright.
R8 habits: whole-body shake-off; species-specific grooming; friendly FRONT greeting viewer; sitting and fidgeting. No detached motion symbols.
Draw every specified pose, 32 complete figures, no missing row. Preserve identity and attached costume.
PUFFLING round fluffy cream cloud creature, blue-grey individual tufts, tiny blue feet, sage green hood leaf clasp brown satchel. Puffy arms and short springy support legs under fleece, NEVER boots/long legs. Breathing puffs fleece, shake-off fluffs outward attached tufts, grooming pats own fluff. Fix r1 breathing with OPEN eyes all four. r3c4 rear-right three-quarter. Fix r5 planted FRONT body, head looks left/right/up/down without rotating legs/satchel. r6 sad and hungry front, not existing profile. Distinct grounded weight shift and braking leaned back, all fully within cells.

### wyrmling

Use case: identity-preserve. Correct the supplied Itoeva animation atlas without redesigning its character or outfit. NEW PRODUCTION PIXEL ART ATLAS. EXACT 4 columns x 8 rows of equal square cells, 32 complete separate drawings, portrait canvas ratio 1:2. Genuine transparent background. Every complete figure and appendage contained in CENTRAL 60% of its cell with 20% transparent margin on each edge. Same physical scale, head size, palette throughout. True crisp 128px pixel art with deliberate pixel clusters and clean edges, no smooth stickers, no shadow/ground/glow/text/lines/symbols/scenery. All side views face RIGHT. Accessories stay on same anatomical side.
Correct incorrect orientations and duplicate poses. Each row's four drawings depict visible subtle articulated posture changes, supporting weight, secondary lag of ears/tail/fabric/feathers and settling; never only blinking or translating a static sticker. Ground contacts stay planted where requested. Sitting naturally shorter, not enlarged.
R1 FRONT breathing, EYES OPEN all four: inhale chest raised; top of breath; exhale settling; relaxed rest. Identical support contact.
R2 RIGHT SIDE weight shift: back support; transferring forward; front support; settling back. Four distinct support/posture phases.
R3 turning: RIGHT profile; three-quarter toward viewer; full front; three-quarter AWAY from viewer facing rear-right (BACK of head and shoulder, do NOT face LEFT).
R4 RIGHT SIDE start/stop: anticipate lean forward; first push-off; brake leaning BACK with appendages swinging FORWARD; settled with appendages swinging BACK.
R5 FRONT lower body planted in all four, only upper body/head looks LEFT; RIGHT; UP; DOWN. Do not turn whole body to profile.
R6 ALL FRONT moods: happy small bounce; sad lowered head; hungry touching belly looking around; tired WIDE YAWN eyes shut. Do not turn hungry to profile.
R7 ALL RIGHT SIDE landing: compressed low; overshoot rising; balance wobble with support underneath body; settled upright.
R8 habits: whole-body shake-off; species-specific grooming; friendly FRONT greeting viewer; sitting and fidgeting. No detached motion symbols.
Draw every specified pose, 32 complete figures, no missing row. Preserve identity and attached costume.
WYRMLING slim jade dragon cream throat/belly, rusty copper bat wings with clear wing fingers, short curved horns and rusty crest, tapered long rust-spiked tail, copper shoulder strap amber pendant cream scarf. Upright TWO hind feet, small forepaws, no flight. Wings and tail counterbalance weight/stop, sad drooping wings, shake briefly open wings with grounded feet, grooming clean wing membrane. Correct r1 open eyes, r6 sad/hungry/yawn FRONT body, r7 all RIGHT profile grounded balance (no front wobble), r3 rear-right three-quarter. Compact tail curve and spread wings entirely inside own cell.

### starlet

Use case: identity-preserve. Correct the supplied Itoeva animation atlas without redesigning its character or outfit. NEW PRODUCTION PIXEL ART ATLAS. EXACT 4 columns x 8 rows of equal square cells, 32 complete separate drawings, portrait canvas ratio 1:2. Genuine transparent background. Every complete figure and appendage contained in CENTRAL 60% of its cell with 20% transparent margin on each edge. Same physical scale, head size, palette throughout. True crisp 128px pixel art with deliberate pixel clusters and clean edges, no smooth stickers, no shadow/ground/glow/text/lines/symbols/scenery. All side views face RIGHT. Accessories stay on same anatomical side.
Correct incorrect orientations and duplicate poses. Each row's four drawings depict visible subtle articulated posture changes, supporting weight, secondary lag of ears/tail/fabric/feathers and settling; never only blinking or translating a static sticker. Ground contacts stay planted where requested. Sitting naturally shorter, not enlarged.
R1 FRONT breathing, EYES OPEN all four: inhale chest raised; top of breath; exhale settling; relaxed rest. Identical support contact.
R2 RIGHT SIDE weight shift: back support; transferring forward; front support; settling back. Four distinct support/posture phases.
R3 turning: RIGHT profile; three-quarter toward viewer; full front; three-quarter AWAY from viewer facing rear-right (BACK of head and shoulder, do NOT face LEFT).
R4 RIGHT SIDE start/stop: anticipate lean forward; first push-off; brake leaning BACK with appendages swinging FORWARD; settled with appendages swinging BACK.
R5 FRONT lower body planted in all four, only upper body/head looks LEFT; RIGHT; UP; DOWN. Do not turn whole body to profile.
R6 ALL FRONT moods: happy small bounce; sad lowered head; hungry touching belly looking around; tired WIDE YAWN eyes shut. Do not turn hungry to profile.
R7 ALL RIGHT SIDE landing: compressed low; overshoot rising; balance wobble with support underneath body; settled upright.
R8 habits: whole-body shake-off; species-specific grooming; friendly FRONT greeting viewer; sitting and fidgeting. No detached motion symbols.
Draw every specified pose, 32 complete figures, no missing row. Preserve identity and attached costume.
STARLET golden five-point star dark eyes, indigo tiny-star collar crescent pendant. EXACTLY FIVE separate readable points each drawing: upper, two sides, two lower supports. NO arms/legs/hands/boots, no glow/sparkles. Side points gesture, lower points support flex naturally; sad points droop, shake points quiver, grooming polish one point with another. r3c4 rear-right three-quarter must SHOW BACK without eyes, not duplicate side profile. r1 keep eyes open all four breathing. r4 brake visibly tilted BACK, lower support planted. r5 front body stays planted only upper star/face looks. All 32 contained central60% cells.

### hootlet

Use case: identity-preserve. Correct the supplied Itoeva animation atlas without redesigning its character or outfit. NEW PRODUCTION PIXEL ART ATLAS. EXACT 4 columns x 8 rows of equal square cells, 32 complete separate drawings, portrait canvas ratio 1:2. Genuine transparent background. Every complete figure and appendage contained in CENTRAL 60% of its cell with 20% transparent margin on each edge. Same physical scale, head size, palette throughout. True crisp 128px pixel art with deliberate pixel clusters and clean edges, no smooth stickers, no shadow/ground/glow/text/lines/symbols/scenery. All side views face RIGHT. Accessories stay on same anatomical side.
Correct incorrect orientations and duplicate poses. Each row's four drawings depict visible subtle articulated posture changes, supporting weight, secondary lag of ears/tail/fabric/feathers and settling; never only blinking or translating a static sticker. Ground contacts stay planted where requested. Sitting naturally shorter, not enlarged.
R1 FRONT breathing, EYES OPEN all four: inhale chest raised; top of breath; exhale settling; relaxed rest. Identical support contact.
R2 RIGHT SIDE weight shift: back support; transferring forward; front support; settling back. Four distinct support/posture phases.
R3 turning: RIGHT profile; three-quarter toward viewer; full front; three-quarter AWAY from viewer facing rear-right (BACK of head and shoulder, do NOT face LEFT).
R4 RIGHT SIDE start/stop: anticipate lean forward; first push-off; brake leaning BACK with appendages swinging FORWARD; settled with appendages swinging BACK.
R5 FRONT lower body planted in all four, only upper body/head looks LEFT; RIGHT; UP; DOWN. Do not turn whole body to profile.
R6 ALL FRONT moods: happy small bounce; sad lowered head; hungry touching belly looking around; tired WIDE YAWN eyes shut. Do not turn hungry to profile.
R7 ALL RIGHT SIDE landing: compressed low; overshoot rising; balance wobble with support underneath body; settled upright.
R8 habits: whole-body shake-off; species-specific grooming; friendly FRONT greeting viewer; sitting and fidgeting. No detached motion symbols.
Draw every specified pose, 32 complete figures, no missing row. Preserve identity and attached costume.
HOOTLET little violet/cream owl cream face disk amber eyes fine round brass glasses short indigo star cape gold edge crescent clasp rolled side map two clawed feet. OWL proportions, no human limbs, no flight. IMPORTANT breathing first row keep eyes OPEN all four, no blink substitutions. Looking-around r5 body stays FRONT with same planted claws, STRONG owl neck head turns LEFT RIGHT UP DOWN only. Happy small bounce eyes open, sad lowered head front, hungry wing touches belly front, tired open BEAK yawn eyes shut. Shake ruffle attached feathers, grooming preen wing then adjust glasses, greet wing viewer, perch sit by folded short hocks with foot fidget. Strict containment all32.

### Anatomische Sitzkorrektur

Use case identity-preserve. Targeted anatomical sitting corrections for THREE existing Itoeva characters in the attached references. ONE transparent pixel-art sheet, exactly FOUR columns and THREE rows, 12 full separate drawings, generous 20% alpha gutters no cropping. Each row has ONE SAME character with exactly SAME anatomical HEAD SIZE, ear/horn size, body volume and limb lengths in all four drawings. Sitting MUST be 25-30% SHORTER IN HEIGHT than standing because support joints bend. NEVER enlarge seated figure to fill cell. Align all feet/contact to same imaginary baseline in each row. No floor/shadow/text/glow/lines/symbols. Crisp detailed hand-drawn pixel art for128px sprites. All views RIGHT profile.
ROW1 FENNEC corrected reference1: orange cream fox enormous upright ears amber eyes brown gloves boots belt pouches turquoise round brooch long cream-tipped bushy tail. Rust red compact cloak attached to BOTH SHOULDERS draping BEHIND torso to HIP HEIGHT, NO horizontal cloak streamer/rag beside hand. c1 standing straight compact legs; c2 halfway lowering with knees under pelvis; c3 FULLY SEATED pelvis on ground knees folded one compact boot ahead hands resting on knees, ears/head UNCHANGED SIZE, overall height 25% lower; c4 SAME seated posture small natural hand/boot fidget, same height as c3. Both seated eyes OPEN. Tail lies beside hips, does not lift or enlarge body.
ROW2 PUFFLING reference2: round cream cloud fluffy creature blue-grey tufts tiny blue feet sage hood leaf clasp brown satchel. c1 standing supported by tiny short springy legs under fleece; c2 lowering with short legs folding; c3 FULLY SEATED two tiny blue feet forward puffy arms resting, same ROUND HEAD/HOOD SIZE, support legs folded so overall25% lower; c4 same seated with tiny foot/hand fidget. No boots/long legs or squashed face, eyes OPEN.
ROW3 WYRMLING reference3: slim jade dragon cream throat/belly rusty copper wings/fingers shortcurved horns rustycrest long tapered rust-spiked tail copper shoulder strap amber pendant creamscarf. c1 upright TWO hind feet straight support legs; c2 halfway lowering pelvis as hind knees fold; c3 FULLY SEATED pelvis grounded folded hind legs claws forward forepaws rest, exactsame HEAD/HORNS SIZE and wings folded, overall30% lower than c1; c4 same seated posture small claw/tail fidget. No flight, eyesOPEN.
The key requirement is standing vs seated HEIGHT DIFFERENCE by joint articulation with IDENTICAL HEAD SCALE within each row. Do not make 12 equally tall figures.


## Kohärenz-Korrektur nach Nutzerfeedback, v2

Der v1-Export war als Übergang zwischen altem Gang und neuem Stand nicht ausreichend konsistent.
Die dort genannte eigene 63-Farben-Palette und das Rückvergrößern der kleinen Quelle sind ersetzt:
Die sechs `*-living-master.png` sind 1024 x 2048, 4 x 8 Zellen à 256px mit mindestens 52px Rand.
Sie sind die reproduzierbaren Spiel-Exportquellen. Die `*-living-atlas.png` bleiben 512 x 1024
für die 128px-Zellvorschau. Das Spiel nutzt 4096 x 128, 32 Bilder, aus dem höher aufgelösten Master.
Farbwerte stammen direkt aus `app-sim/src/main/assets/creatures/{species}.png`; kein zweites Farbsystem.

Regeneration: `node tools/character-art/living_atlases.js`.
Prüfung: `node tools/character-art/test_living_atlases.js`.
Vergleich inkl. Gang/Blinzeln/Landung: `python tools/character-art/preview_living_coherence.py`.
Die Vorschau zeigt echte exportierte Bildrollen, keine APK-Aufnahme und keinen Nachweis einer perfekten Anatomie.

Die Korrekturen entstanden mit integriertem ImageGen. Identitätsreferenzen sind die tatsächlichen
128px-Spielbilder: seitlicher Stand, Front, Rücken, zwei Gangphasen, Dreiviertelansicht. Die erste
Fennec-Korrektur allein passte noch nicht; gewählt ist die anschließende Proportionskorrektur.
Wyrmlings neue Turn-Zeichnungen 9/10 kamen vertauscht zurück und werden beim Import getauscht.
Nur Pufflings Sitzbild braucht weiter die bestehende separate Sitzkorrektur; Fennec/Wyrmling nutzen
jetzt ihre eigenen neuen Sitzzeichnungen. Die alten Gang- und Aktionsbilder sind unverändert.

### Fennec: Identitätskorrektur (erster Versuch)

undefined

### Fennec: gewählte Proportionskorrektur

undefined

### Wyrmling: gewählte Identitäts-/Proportionskorrektur

undefined
