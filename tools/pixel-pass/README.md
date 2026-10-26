# HD-2D-Pixelpass für die Figuren-Atlanten

`python3 tools/pixel-pass/pixel_pass.py prototypes/godot-hd2d/assets/characters --art-height 56`

Wandelt die gemalten Atlanten von Mooslyn, Nocti, Starlet und Wyrmling in Pixel Art um
(Octopath/FF-Resonance-Stil): feste Pixeldichte (Mooslyn 56 Pixel hoch, andere Figuren
nach `relative_height`), eine gemeinsame Palette mit 44 Farben je Figur, Einzelpixel-
Bereinigung und dunkle Außenkontur. Die Dateien behalten Größe und Raster; die Pixel
werden blockweise auf die Originalgröße gebracht. Zellen, Alphakonturen und Anker im
Code bleiben dadurch gültig.

Die gemalten Originale bleiben unverändert in `assets/characters/`; alle Metadaten, Masken,
Rigs und Prüfsummen beziehen sich weiter auf sie. Die Pixelfassungen liegen in
`assets/characters/pixel/` (Protokoll: `pixel/pixel_pass.json`). `character_actor.gd`
(`pixel_art()`) und `compact_actor.gd` binden für die Farbe die Pixelfassung, falls vorhanden;
der Shader liest sie mit `nearest`, die Pixelfassungen werden nicht VRAM-komprimiert
(`detect_3d/compress_to=0`).

Rückweg: Ordner `assets/characters/pixel/` entfernen – dann zeichnet das Spiel wieder die Originale.

Spielaufnahme (echte Steuerung über den Touch-Stick, 30 Bilder/s):
`TAG=moos SPECIES=mosslyn PLACE=PARK xvfb-run -a godot --rendering-driver opengl3 --fixed-fps 30 --path prototypes/godot-hd2d -s res://preview_pixel/play_capture.gd`
