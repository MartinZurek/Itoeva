# Ensemble-Politur: Quellen und Prompts, 07.10.2026

Alle neun aktiven Quellen wurden mit dem integrierten ImageGen erzeugt;
`transparent_background=true`, lokale Referenzdateien, kein CLI-/API-Fallback.
Die jeweiligen Hauptquellen referenzieren `<wesen>-motion-atlas.png`, die
Gelenkquellen den neuen `<wesen>-refined-atlas.png`. Nur die ersten 32 Figuren
der 48er-Hauptboegen sind aktiv; die letzten 16 werden durch die getrennten
Gelenkboegen ersetzt. Starlets zu dichter erster 48er-Entwurf wurde verworfen;
aktiv ist sein vollstaendiger 32er-Bogen. Die folgende Promptzerlegung ist
Preamble + Identitaet + Layout + Zusatz, mit Leerzeichen bzw. Zeilenumbruch.
Die konkreten aktiven PNG-Namen stehen jeweils am Prompt.

## gloop-refined-atlas.png

Use case: identity-preserve. Asset type: production sprite source atlas for Itoeva game. Input image 1 is the existing Gloop atlas, identity/style reference. Refine this existing character with more readable fine material details and clean articulated motion in the same cozy illustrated style, matching its palette and face. Gloop: friendly moss-green/yellow-green legless slime with translucent amber seed in belly, two oval dark eyes, two-leaf sprout, layered shoulder leaves and small brown leaf satchel. NO legs, NO feet, NO arms with fingers. Movement through alternating bottom lobes and flowing body; preserve face and seed shapes when settling.
Exactly 48 separate complete drawings in a STRICT grid of FOUR columns and TWELVE rows, ordered left-to-right, top-to-bottom. Same character scale across ALL drawings, including seated drawings which must be shorter than standing; do NOT enlarge a sitting pose to cell height. Full opaque subjects with contiguous silhouette, actual transparent empty background, no floor, no shadow, no labels, no grid lines. Generous clear margin around every subject and between rows. All walking/running eyes OPEN. All side views face RIGHT. Strict row contents:
1: side standing; front standing; back standing; side standing blinking.
2: side walk phases 1,2,3,4 with different support positions.
3: side walk phases 5,6,7,8 continuing the same grounded walk.
4: front walk phases 1,2,3,4.
5: back walk phases 1,2,3,4.
6: side running phases 1,2,3,4, distinct faster push-off/posture.
7: side crouch; side jump pose; side seated; side sleeping.
8: side stretching; side reaching; compact curled rest; happy side pose.
9: FRONT standing with closed eyes (same anatomy as row1 front); SIDE standing with closed eyes; SIDE bending/reaching down; SIDE kneeling/deep tuck.
10: SIDE lowering halfway to sit with flexed joints; SIDE fully seated; SIDE rising halfway with extending joints; SIDE soft landing crouch.
11: FRONT lowering halfway; FRONT fully seated; FRONT rising halfway; FRONT reaching/stretching.
12: BACK lowering halfway; BACK fully seated; BACK rising halfway; BACK reaching/stretching.
Draw REAL anatomical pose changes, never vertically squash a standing figure. Sitting is stable on the same ground, head/face retains proportions, limbs/wings/support lobes change shape and fold naturally. Put supporting contact under the body, keep characteristic accessories fixed to the same side and recognizable. Do not omit any of the 48 drawings.


## puffling-refined-atlas.png

Use case: identity-preserve. Asset type: production sprite source atlas for Itoeva game. Input image 1 is the existing Puffling atlas, identity/style reference. Refine this existing character with more readable individual fleece curls, seams and leaf-clasp veins, and clean articulated motion in the same cozy illustrated style, matching its palette and face. Puffling: small cream cloud/fluff creature with pale blue-grey curls, sage-green hood/cape, leaf clasp and little brown satchel, two blue-grey feet. Preserve soft round fluffy identity, reveal distinct short arms and bending support legs beneath the fleece; do not turn it into a skinny human.
Exactly 48 separate complete drawings in a STRICT grid of FOUR columns and TWELVE rows, ordered left-to-right, top-to-bottom. Same character scale across ALL drawings, including seated drawings which must be shorter than standing; do NOT enlarge a sitting pose to cell height. Full opaque subjects with contiguous silhouette, actual transparent empty background, no floor, no shadow, no labels, no grid lines. Generous clear margin around every subject and between rows. All walking/running eyes OPEN. All side views face RIGHT. Strict row contents:
1: side standing; front standing; back standing; side standing blinking.
2: side walk phases 1,2,3,4 with different support positions.
3: side walk phases 5,6,7,8 continuing the same grounded walk.
4: front walk phases 1,2,3,4.
5: back walk phases 1,2,3,4.
6: side running phases 1,2,3,4, distinct faster push-off/posture.
7: side crouch; side jump pose; side seated; side sleeping.
8: side stretching; side reaching; compact curled rest; happy side pose.
9: FRONT standing with closed eyes (same anatomy as row1 front); SIDE standing with closed eyes; SIDE bending/reaching down; SIDE kneeling/deep tuck.
10: SIDE lowering halfway to sit with flexed joints; SIDE fully seated; SIDE rising halfway with extending joints; SIDE soft landing crouch.
11: FRONT lowering halfway; FRONT fully seated; FRONT rising halfway; FRONT reaching/stretching.
12: BACK lowering halfway; BACK fully seated; BACK rising halfway; BACK reaching/stretching.
Draw REAL anatomical pose changes, never vertically squash a standing figure. Sitting is stable on the same ground, head/face retains proportions, limbs/wings/support lobes change shape and fold naturally. Put supporting contact under the body, keep characteristic accessories fixed to the same side and recognizable. Do not omit any of the 48 drawings.
Very important: rows10-12 must show genuine sitting, feet forward and knees folded under fleece, hood and head undistorted. Halfway lowering and fully seated must clearly differ in height and limb arrangement. ALL four row6 RUN poses eyes open.

## hootlet-refined-atlas.png

Use case: identity-preserve. Asset type: production sprite source atlas for Itoeva game. Input image 1 is the existing Hootlet atlas, identity/style reference. Refine this existing character with readable layered feather vanes, delicate glasses, cape embroidery and articulated wings/claws in the same cozy illustrated style, matching its palette and face. Hootlet: little violet and cream owl, amber eyes behind fine brass round glasses, indigo star cape edged gold with crescent clasp, little rolled map at side, two small clawed feet. Refine layered feathers and articulated folded wings; perch/sit by flexing bird ankles and lowering haunches; no human knees, no new flight poses.
Exactly 48 separate complete drawings in a STRICT grid of FOUR columns and TWELVE rows, ordered left-to-right, top-to-bottom. Same character scale across ALL drawings, including seated drawings which must be shorter than standing; do NOT enlarge a sitting pose to cell height. Full opaque subjects with contiguous silhouette, actual transparent empty background, no floor, no shadow, no labels, no grid lines. Generous clear margin around every subject and between rows. All walking/running eyes OPEN. All side views face RIGHT. Strict row contents:
1: side standing; front standing; back standing; side standing blinking.
2: side walk phases 1,2,3,4 with different support positions.
3: side walk phases 5,6,7,8 continuing the same grounded walk.
4: front walk phases 1,2,3,4.
5: back walk phases 1,2,3,4.
6: side running phases 1,2,3,4, distinct faster push-off/posture.
7: side crouch; side jump pose; side seated; side sleeping.
8: side stretching; side reaching; compact curled rest; happy side pose.
9: FRONT standing with closed eyes (same anatomy as row1 front); SIDE standing with closed eyes; SIDE bending/reaching down; SIDE kneeling/deep tuck.
10: SIDE lowering halfway to sit with flexed joints; SIDE fully seated; SIDE rising halfway with extending joints; SIDE soft landing crouch.
11: FRONT lowering halfway; FRONT fully seated; FRONT rising halfway; FRONT reaching/stretching.
12: BACK lowering halfway; BACK fully seated; BACK rising halfway; BACK reaching/stretching.
Draw REAL anatomical pose changes, never vertically squash a standing figure. Sitting is stable on the same ground, head/face retains proportions, limbs/wings/support lobes change shape and fold naturally. Put supporting contact under the body, keep characteristic accessories fixed to the same side and recognizable. Do not omit any of the 48 drawings.
Crucial: exactly TWELVE rows, do not omit the ninth or tenth row. Rows10-12 must show genuine bird sitting/perching by flexing the short ankles, body lowers between grounded forward claws, wings neatly folded, head NOT flattened. Halfway lowering and fully seated must clearly differ in height and support arrangement. Eyes in ALL WALK and RUN phases OPEN behind glasses.

## starlet-refined-atlas.png

Use case: identity-preserve. Asset: 32 animation source drawings for Starlet, preserve the attached original character. Starlet: golden FIVE-POINT star creature with dark oval eyes, indigo star-pattern collar/cape and small crescent pendant. EXACTLY FIVE POINTS in every full pose. Bottom two star tips serve as support; side points gesture. No added human legs, shoes, fingers or extra points. Improve fine golden facets and grain, embroidered indigo collar and crescent, true flexing point joints, clear five-point silhouette; cozy detailed crisp sprite painting, no glossy plastic.
EXACTLY FOUR COLUMNS AND EIGHT ROWS, 32 drawings. Tall canvas, each drawing in its own generous space, NOTHING touching another drawing or any image edge; a large empty transparent top/bottom margin especially for upper points. Same character size, head proportions and lower support lengths across standing and gait. Transparent alpha background, no ground/shadow/text/lines.
Row1: right side stand, FRONT stand, BACK stand, right side closed-eye blink.
Rows2,3: 8 different consecutive RIGHT-FACING WALK phases, all eyes OPEN, alternate LOWER TWO POINTS as flexible support, lateral point countergesture. No shoe/leg appendages; still 5 points.
Row4: 4 FRONT WALK phases eyes open.
Row5: 4 BACK WALK phases no face.
Row6: 4 RIGHT RUN phases all eyes OPEN, distinct lower point flex/extension.
Row7: RIGHT low jump anticipation; RIGHT jump with lower points tucked; RIGHT sitting with lower points folded forward; RIGHT sleeping.
Row8: RIGHT stretch, RIGHT reach using lateral point, compact curl suitable for roll, RIGHT happy pose with eyes open.
No extra star points. Draw separate poses, do not squash or stretch standing drawing. Do not add extra CLOSED eyes except blink/sleep/curl.

## gloop-posture-atlas.png

Use case: stylized-concept. Production additional anatomical sprite drawings for GLOOP. Supplied refined atlas is identity/style reference. Gloop: friendly moss-green/yellow-green legless slime with translucent amber seed in belly, two oval dark eyes, two-leaf sprout, layered shoulder leaves and small brown leaf satchel. NO legs, NO feet, NO arms with fingers. Movement through alternating bottom lobes and flowing body; preserve face and seed shapes when settling.
Exactly SIXTEEN drawings in FOUR columns and FOUR rows on SQUARE canvas. Same physical size/scale and anatomical head size as standing reference in all 16 poses; seated figures are visibly SHORTER, do NOT enlarge to fill their cells. Full opaque connected figures on actual transparent empty background, big clear gutters and outer margins, no shadows/ground/text/grid.
ROW1: FRONT standing, eyes CLOSED (otherwise same front neutral); RIGHT side standing, eyes CLOSED; RIGHT side bending/reaching down; RIGHT side deep crouch/tuck.
ROW2: RIGHT SIDE lowering HALFWAY to sit; RIGHT SIDE FULLY SEATED awake; RIGHT SIDE RISING halfway; RIGHT SIDE soft landing crouch.
ROW3: FRONT lowering HALFWAY; FRONT FULLY SEATED awake; FRONT RISING halfway; FRONT reaching/stretching.
ROW4: BACK lowering HALFWAY; BACK FULLY SEATED; BACK RISING halfway; BACK reaching/stretching.
Stay RIGHT-FACING SIDE all of ROW2, never front-facing. Back views have NO face. Eyes OPEN except explicit first two blinks. Standing > halfway > seated in body height: sitting lowers about25% by SUPPORTS FOLDING/SETTLING, not by squashing head/body. Head/face dimensions unchanged; torso and accessories retain size/shape. True detailed pose drawings.
Gloop's natural rest is a relaxed low puddle with bottom lobes spread outward and belly sinking into its support, sprout droops gently, face and amber seed remain round and undistorted. Halfway pose visibly retains more height; seated rest visibly lower yet head not stretched. Side sit shows the face pointing RIGHT, not front. Back seated loses height by broadening bottom slime lobes beneath leaves; no invented feet or legs. Refine leaf veins, layered leaves, tiny flower centres and satchel stitchwork as in reference.

## puffling-posture-atlas.png

Use case: stylized-concept. Production additional anatomical sprite drawings for PUFFLING. Supplied refined atlas is identity/style reference. Puffling: small cream cloud/fluff creature with pale blue-grey curls, sage-green hood/cape, leaf clasp and little brown satchel, two blue-grey feet. Preserve soft round fluffy identity, reveal distinct short arms and bending support legs beneath the fleece; do not turn it into a skinny human.
Exactly SIXTEEN drawings in FOUR columns and FOUR rows on SQUARE canvas. Same physical size/scale and anatomical head size as standing reference in all 16 poses; seated figures are visibly SHORTER, do NOT enlarge to fill their cells. Full opaque connected figures on actual transparent empty background, big clear gutters and outer margins, no shadows/ground/text/grid.
ROW1: FRONT standing, eyes CLOSED (otherwise same front neutral); RIGHT side standing, eyes CLOSED; RIGHT side bending/reaching down; RIGHT side deep crouch/tuck.
ROW2: RIGHT SIDE lowering HALFWAY to sit; RIGHT SIDE FULLY SEATED awake; RIGHT SIDE RISING halfway; RIGHT SIDE soft landing crouch.
ROW3: FRONT lowering HALFWAY; FRONT FULLY SEATED awake; FRONT RISING halfway; FRONT reaching/stretching.
ROW4: BACK lowering HALFWAY; BACK FULLY SEATED; BACK RISING halfway; BACK reaching/stretching.
Stay RIGHT-FACING SIDE all of ROW2, never front-facing. Back views have NO face. Eyes OPEN except explicit first two blinks. Standing > halfway > seated in body height: sitting lowers about25% by SUPPORTS FOLDING/SETTLING, not by squashing head/body. Head/face dimensions unchanged; torso and accessories retain size/shape. True detailed pose drawings.
Maintain cozy FLOOF round cream head and individual blue-grey curls, sage cape/clasp/satchel. Real sit by flexing short legs under the fluff, bringing two little blue-grey feet forward, distinct fluffy forearms resting. Standing head width, hood proportions and trunk size must stay stable, NEVER flatter/squashed face. FULLY BACK seated must visibly lower the body ~25% and cape folds around the tucked legs, blue-grey feet visible at sides; do not draw another standing back pose. Back lowering HALFWAY still has flexing support ankles. No extra puff blobs pretending to be legs. Side sit remains RIGHT-facing, face awake open eyes.

## hootlet-posture-atlas.png

Use case: stylized-concept. Production additional anatomical sprite drawings for HOOTLET. Supplied refined atlas is identity/style reference. Hootlet: little violet and cream owl, amber eyes behind fine brass round glasses, indigo star cape edged gold with crescent clasp, little rolled map at side, two small clawed feet. Refine layered feathers and articulated folded wings; perch/sit by flexing bird ankles and lowering haunches; no human knees, no new flight poses.
Exactly SIXTEEN drawings in FOUR columns and FOUR rows on SQUARE canvas. Same physical size/scale and anatomical head size as standing reference in all 16 poses; seated figures are visibly SHORTER, do NOT enlarge to fill their cells. Full opaque connected figures on actual transparent empty background, big clear gutters and outer margins, no shadows/ground/text/grid.
ROW1: FRONT standing, eyes CLOSED (otherwise same front neutral); RIGHT side standing, eyes CLOSED; RIGHT side bending/reaching down; RIGHT side deep crouch/tuck.
ROW2: RIGHT SIDE lowering HALFWAY to sit; RIGHT SIDE FULLY SEATED awake; RIGHT SIDE RISING halfway; RIGHT SIDE soft landing crouch.
ROW3: FRONT lowering HALFWAY; FRONT FULLY SEATED awake; FRONT RISING halfway; FRONT reaching/stretching.
ROW4: BACK lowering HALFWAY; BACK FULLY SEATED; BACK RISING halfway; BACK reaching/stretching.
Stay RIGHT-FACING SIDE all of ROW2, never front-facing. Back views have NO face. Eyes OPEN except explicit first two blinks. Standing > halfway > seated in body height: sitting lowers about25% by SUPPORTS FOLDING/SETTLING, not by squashing head/body. Head/face dimensions unchanged; torso and accessories retain size/shape. True detailed pose drawings.
Keep detailed violet/cream feather layers, brass glasses, star cape/crescent and rolled map. Owl sits by flexing short BIRD ankles/hocks under the feathered body, settles down between forward claws, head and glasses retain full circular proportions. Not a squashed standing sticker. BACK seated must visibly lower body/cape at least25%, with lower feather layers spreading around tucked claws. Same head/crest size as BACK standing in reference, no bigger head in sitting pose. Folded wings close to sides in all rest poses. Eye rings/glasses stay round. Side lowers to its own actual perch, not belly-floating over stiff legs.

## starlet-posture-atlas.png

Use case: stylized-concept. Asset: 16 supplementary anatomical posture drawings for STARLET in the supplied NEW 32-drawing reference. Starlet: golden FIVE-POINT star creature with dark oval eyes, indigo star-pattern collar/cape and small crescent pendant. EXACTLY FIVE POINTS in every full pose. Bottom two star tips serve as support; side points gesture. No added human legs, shoes, fingers or extra points. Exact same face, gold grain, indigo collar and crescent. EXACTLY FOUR columns and FOUR rows on SQUARE canvas, full isolated figures at same physical scale, generous empty transparent margins/gutters, no touching/cropping, no text/grid/shadow/ground.
Row1: full FRONT standing eyes CLOSED; RIGHT side standing eyes CLOSED; RIGHT side bending downward by rotating upper body at lower point joints; RIGHT side deep kneel/tuck bending LOWER TWO POINTS.
Row2: RIGHT SIDE lowering HALFWAY to sit with lower two points folding; RIGHT SIDE fully SEATED awake with lower two points folded forward under body, upper point and face UNDISTORTED; RIGHT SIDE rising halfway as lower points extend; RIGHT SIDE landing crouch.
Row3: FRONT lowering HALFWAY; FRONT fully SEATED with lower two points folded inward/forward, SAME UPPER 3 POINTS AND FACE PROPORTIONS as standing; FRONT rising halfway; FRONT reaching with lateral point.
Row4: BACK lowering HALFWAY; BACK fully SEATED with lower two tips folded down and forward, collar descends naturally, upper body NOT SQUASHED; BACK rising halfway; BACK reaching with lateral point.
Critical: RIGHT-SIDE views must remain RIGHT-SIDE throughout row2, NOT rotate to front. EXACTLY FIVE star points total; no added feet, arms, fingers. All eyes OPEN except first two blinks. Lowering decreases height ~15%, sitting decreases height ~30% because supports FOLD, never by flattening head/body. Seated figures are actually SHORTER in the grid, not enlarged to fill cell. Draw stable crisp detailed sprite painting.

## fennec-posture-refined-atlas.png

Referenzen: `fennec-walk-profile-atlas.png` (Design/Gang) und
`fennec-actions-atlas.png` (vorherige Aktionen). Nur erste Zeile aktiv;
unbenutzter Blink in zweiter Zeile ist angeschnitten und bleibt ausgeschlossen.

Use case: identity-preserve. Asset: EIGHT source drawings to correct FENNEC's sitting animation in Itoeva. Image1 is the authoritative character/gait design, image2 current actions showing identity and errors to replace. Match orange/cream fur, huge thin fox ears, amber alert eyes, rust leaf cape, TURQUOISE oval brooch at throat, brown tunic/belt/pouches/gloves and boots, fluffy cream-tipped tail. Cozy fine painted sprite outline, material stitches/fur/cape veins preserved. Slight slender adventurer build; no inflatable belly, no changes of costume.
Exactly FOUR columns by TWO rows on landscape canvas. Exactly eight separate complete drawings, SAME anatomical/physical size in all poses, full transparent margins and gutters, no shadows/floor/text/grid. All poses look RIGHT in profile/three-quarter consistent with current side action stand.
ROW1: RIGHT SIDE neutral upright standing, arms relaxed close to torso, boots planted; RIGHT SIDE halfway LOWERING to sit, knees and hip actually bending; RIGHT SIDE FULLY SEATED awake, pelvis on ground, bent knees/one folded leg, forearms rest naturally; RIGHT SIDE halfway RISING from that sit, boots push with extending knees/hips, head still upright.
ROW2: RIGHT SIDE standing closed-eye blink otherwise identical to row1 standing; RIGHT SIDE gentle bend to inspect floor with flexed knees; RIGHT SIDE deep kneeling with hands near ground; RIGHT SIDE soft landing crouch with both boots contact.
Critically: ALL 4 row1 figures retain EXACT same head size, ear length, trunk volume and limb length. Seated figure actually shorter (~25%) because knees fold, not resized upward. Intermediate seat/rise heights between stand and seated. Keep front support BOOT planted under chest on same relative x position across row1, do not extend BOTH legs far forward or make rising lean its head far ahead. Tail curls compactly behind left pelvis, stays OFF the soles, not an enormous wide fan forcing bbox alignment to change. Seated pose remains side facing right, no turned-front head. Keep two boots distinct, natural knee/hock bends and elbows. All eyes OPEN except explicit blink.
