# HD-2D-Pixelpass für die Figuren-Atlanten

`python3 tools/pixel-pass/pixel_pass.py prototypes/godot-hd2d/assets/characters --art-height 56`

Wandelt die gemalten Atlanten von Mooslyn, Nocti, Starlet und Wyrmling in Pixel Art um
(Octopath/FF-Resonance-Stil): feste Pixeldichte (Mooslyn 56 Pixel hoch, andere Figuren
nach `relative_height`), eine gemeinsame Palette mit 44 Farben je Figur, Einzelpixel-
Bereinigung und dunkle Außenkontur. Die Dateien behalten Größe und Raster; die Pixel
werden blockweise auf die Originalgröße gebracht. Zellen, Alphakonturen und Anker im
Code bleiben dadurch gültig. Ergebnis je Datei steht in `assets/characters/pixel_pass.json`.

Dazu zeichnen `painted_character.gdshader`, `character_actor.gd` und `compact_actor.gd`
die Figuren mit `nearest` statt `linear`, und die Atlanten werden nicht VRAM-komprimiert
(`detect_3d/compress_to=0`), damit die Pixel scharf bleiben.

Rückweg: Die gemalten Originale liegen unverändert in der Git-Historie (Commit vor dem Pass).

Spielaufnahme (echte Steuerung über den Touch-Stick, 30 Bilder/s):
`TAG=moos SPECIES=mosslyn PLACE=PARK xvfb-run -a godot --rendering-driver opengl3 --fixed-fps 30 --path prototypes/godot-hd2d -s res://preview_pixel/play_capture.gd`
