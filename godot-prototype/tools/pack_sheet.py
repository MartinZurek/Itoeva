"""Packt einen Wesen-Bogen (138 Bilder je 128 px in EINER Zeile, 17664 px breit) in ein Raster.

Godot laedt keine Texturen ueber 16384 px Kantenlaenge. Das Raster hat 12 Spalten; Bild n liegt
in Spalte n % 12, Zeile n // 12. Aufruf aus dem Repo-Wurzelverzeichnis:
    python3 godot-prototype/tools/pack_sheet.py fennec
"""
import sys
from PIL import Image

FRAME, COLS = 128, 12
name = sys.argv[1] if len(sys.argv) > 1 else 'fennec'
src = Image.open(f'app-sim/src/main/assets/creatures/{name}.png').convert('RGBA')
n = src.width // FRAME
rows = (n + COLS - 1) // COLS
out = Image.new('RGBA', (COLS * FRAME, rows * FRAME), (0, 0, 0, 0))
for i in range(n):
    out.paste(src.crop((i * FRAME, 0, (i + 1) * FRAME, FRAME)), ((i % COLS) * FRAME, (i // COLS) * FRAME))
out.save(f'godot-prototype/assets/{name}_grid.png', optimize=True)
print(name, n, 'Bilder ->', out.size)
