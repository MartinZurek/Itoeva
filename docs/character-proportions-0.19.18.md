# Gameplay 0.19.18 · character proportions

Fixed direction-dependent shrinking: v2 atlas import used every nonzero alpha pixel, including nearly invisible export fringe. The new importer crops at alpha 0.31 and registers front/back groups against the visible side-view height. All four characters receive corrected views, including sit and roll registration.

New front/back companion art looks straight toward/away from the camera. Wyrmling, Nocti and Starlight no longer use three-quarter front poses. Moosling's slow walk has eight compact, bent-leg poses, with reduced procedural upper-body displacement. Nocti retains flight lift when moving into depth views.

Validation: 60 visible-height, baseline and fringe assertions; 536 character-route checks. Atlas images visually inspected. Android performance and perceived walking flow still require device testing.

Reproduce atlas registration with `godot --headless --path . --script tools/import_character_proportions.gd`; source assets are retained under `assets/characters/source/`. Built-in image generation was used for the two replacement source sheets, followed by deterministic Godot cropping and atlas registration.

Art prompts: preserve the existing identities and pixel style; companion sheet contains strict front/front-alternate/back/back-alternate poses for Wyrmling, Nocti and Starlight, with both eyes equally visible in front; Moosling sheet contains eight slow two-legged walking phases with short bent legs, small steps, unchanged antlers, moss cape and satchel. No text, floor shadow or background.
