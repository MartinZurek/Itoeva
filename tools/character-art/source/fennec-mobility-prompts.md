# Fennec-Bewegungsquellen, 2026-10-05

Erzeugt mit dem integrierten ImageGen, transparenter Hintergrund. Die PNGs sind
die unveraenderten Bildgenerierungs-Ausgaben; Import/Palettenrasterung erfolgt mit
`fennec_mobility.py`. Kein API-Key oder separater CLI-Aufruf.

## fennec-walk-directions-atlas.png

Referenz: erste Front-/Rueckgang-Ausgabe, ihrerseits erzeugt anhand von
`fennec-walk-profile-atlas.png`. Der erste Versuch schnitt vier Stiefel ab und
wurde verworfen. Nur die korrigierte Quelle wird importiert.

Exakte Korrekturanweisung:

> Use case precise-object-edit. Correct this EXISTING front/back walk animation atlas while preserving the EXACT character identity and costume. Critical corrections: bottom row currently CROPS THE BOOTS: redraw complete full body figures with COMPLETE BOOTS and visible grounded soles in ALL sixteen cells. Use a SQUARE CANVAS with FOUR columns and FOUR rows of equal SQUARE cells, not landscape. Each whole figure centered within its cell and scaled down to leave 12% transparent empty margin on ALL FOUR sides; there must be absolutely NO body part crossing cell boundaries. Transparent background. Top eight FRONT walk poses, bottom eight BACK walk poses. Same full character scale in every cell. Keep knees directly under pelvis, normal compact biped walk, clear alternate left/right supporting boot and raised passing boot. Left and right boots stay on separate foot tracks, do not cross or blend into one. Frames 1,5 both boots on ground contact; 2,6 weight transfer; 3,7 passing raised boot; 4,8 swinging boot. Full cape and tail of same design, but neither should obscure feet. Preserve the same huge ears orange and cream fur rust cloak turquoise brooch gloves belt pouches brown boots and illustrated ink style. No labels or text or shadow. Must have all 16 figures COMPLETE and uniform in scale, with visible foot contact and generous border especially bottom.

## fennec-actions-atlas.png

Referenz: `fennec-walk-profile-atlas.png`.

> Use case: identity-preserve. Production FULL BODY ACTION animation atlas for same existing Itoeva Fennec in reference. Precisely preserve orange/cream fur, huge upright ears, rust red short travel cape, round turquoise brooch, belt/pouches, gloves, short compact legs with brown boots, long fluffy tail and same painted ink drawing. ONE transparent atlas, exactly 4 columns x 4 rows equal rectangular cells with generous margins, no text, no labels, no shadows. Every full figure must fit wholly in its own cell including ears/tail/boots; especially bottom row keep at least 12% transparent bottom margin. Fixed body/head scale for all figures, same three-quarter profile facing RIGHT, enough room above for raised arms. Row 1 consecutive jump phases: crouched anticipation with weight on both boots, takeoff with extended ankles and arms lifting, compact midair tuck with knees bent under body and boots off ground, landing with knees absorbing force on both feet. Row 2 bending and kneeling: partial bend forward from hips with knees slightly bent, deep bend reaching toward ground right hand low, one knee kneeling on ground and other foot planted, returning upright with knee extending. Row 3 sit and stand: lowering into seated posture, seated legs extending gently forward boots grounded and tail curled beside, leaning forward to rise arms helping, fully standing neutral with legs under pelvis and boots close. Row 4 gestures: stretching with both arms above head, reaching forward right arm extended at chest level, one leg lifted forward for football kick with other straight supporting, relaxed sleeping curled on side with eyes closed tail as pillow. Readable individual full body drawings, compact natural biped anatomy, knees under body, no widely splayed stance. Keep identity and head size consistent throughout all sixteen images, same character clothing accessories and style. Draw genuine changes in hips, knees and limbs rather than merely translating the standing figure.

## fennec-actions-directions-atlas.png

Referenzen: Profilgang und erster Front-/Rueckgang. Das Modell lieferte 7 Spalten
mit 2 Zeilen statt des gewuenschten 4x4-Layouts. Der Import verwendet die
tatsaechlichen 14 vollstaendigen Zeichnungen: Hocke, Absprung, Flug, Sitzen, Stand,
Buecken, Strecken; zuerst vorn, danach hinten. Landung = Hocke, kein eigenes Bild.

> Use case identity-preserve. New FRONT/BACK action and neutral atlas of EXISTING Itoeva Fennec; match reference exactly. Orange cream fox, huge upright ears, rust travel cloak, turquoise round brooch, gloves belt/pouch compact legs brown boots fluffy long cream-tip tail. Same full body scale, same head size, same painted ink style throughout. Transparent background, ONE atlas exactly FOUR columns FOUR rows equal cells. Each character FULLY within own cell, at least 10% empty margins, no cropped ears/boots/tails, no text or labels or shadows. TOP TWO rows eight STRICT FRONT-facing drawings in row-major order: 1 crouch before jump knees under pelvis boots grounded; 2 takeoff body extended arms raised boots pointing down; 3 airborne tuck both knees flexed under belly soles visible; 4 landing knees softly flexed both boots grounded; 5 calm neutral standing BOTH legs straight under body both boots side by side; 6 seated on invisible low seat relaxed knees ahead boots flat on ground; 7 bending from hips reaching a paw down toward ground; 8 stretching both arms overhead, feet grounded. BOTTOM TWO rows eight STRICT BACK-facing drawings in exactly same order 1 crouch 2 takeoff 3 airborne tuck 4 landing 5 neutral two feet planted 6 seated 7 bending down 8 stretching arms overhead. Front face symmetric, two eyes visible; back shows the BACK of head NO face/no eyes, cloak back and tail, boots visible. Identical character and outfit in all cells. Short anatomically stable biped legs, never widely splayed, same head and body proportions even for seated and bent poses. Arms ears cape tail follow pose naturally. This complements the walk atlas with neutral two-foot stance and genuinely articulated body actions. Full figures fully contained, generous border at bottom.

## fennec-front-blink.png

Referenz: der neue Stand-Frame 27, sechsfach ohne Glaettung vergroessert.

> Use case precise-object-edit. This is the EXACT front standing sprite of Itoeva Fennec. Change ONLY the eyes to a gentle closed-eyelid blink. Keep all other pixels as unchanged as possible: exact body stance, both boots on ground, enormous ears, face outline, orange fur, cloak, turquoise brooch, gloves, belt and pouches. No pose change, no new accessories, no change of head size, NO redesign. A single centered complete full-body sprite with transparent background, preserving original sharp pixel art and same margins. Eyelids two short dark curved horizontal lines instead of open dark eyes. Only blink eyes; keep nose/mouth, posture, clothing and proportions.
