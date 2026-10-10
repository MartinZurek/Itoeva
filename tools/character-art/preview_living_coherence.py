"""Vergleicht echte exportierte Frames, einschliesslich der bisher ausgelassenen Gangwechsel."""
from io import BytesIO
from pathlib import Path
import json
import subprocess
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
BASELINE = "9012dae10dd8ab5385d0e3a1131713e98af6a1b8"
NAMES = ["fennec", "gloop", "puffling", "wyrmling", "starlet", "hootlet"]
SCALES = dict(zip(NAMES, [.82, 1.16, 1.28, 1.13, .98, 1.18]))
MANIFEST = "tools/character-art/source/living-atlas-manifest.json"
OLD = json.loads(subprocess.check_output(["git", "show", f"{BASELINE}:{MANIFEST}"], cwd=ROOT))
NEW = json.loads((ROOT / MANIFEST).read_text())
try:
    FONT = ImageFont.truetype("DejaVuSans.ttf", 18)
except OSError:
    FONT = ImageFont.load_default()

def strip(name, living=False, before=False):
    file = f"app-sim/src/main/assets/creatures/{name}{'-living' if living else ''}.png"
    data = subprocess.check_output(["git", "show", f"{BASELINE}:{file}"], cwd=ROOT) if before else (ROOT / file).read_bytes()
    return Image.open(BytesIO(data)).convert("RGBA")

SHEETS = {(n, extra, before): strip(n, extra, before) for n in NAMES for extra in [False, True] for before in [False, True]}

def paint(canvas, name, extra, frame, before, x, ground, width=300):
    factor = 2 * SCALES[name]
    if extra:
        factor *= (OLD if before else NEW)["species"][name]["renderScale"]
    sheet = SHEETS[name, extra, before]
    tile = sheet.crop((frame * 128, 0, (frame + 1) * 128, 128))
    size = round(128 * factor)
    tile = tile.resize((size, size), Image.Resampling.NEAREST)
    canvas.paste(tile, (round(x + (width - size) / 2), round(ground - 126 * factor)), tile)

comparison = Image.new("RGB", (1320, 1740), "#e1e4df")
draw = ImageDraw.Draw(comparison)
for c, label in enumerate(["Bisheriger Gang", "Korrigierte Ruhe", "Bisherige Front", "Korrigierte Front"]):
    draw.text((c * 330 + 10, 12), label, font=FONT, fill="#252a27")
for r, name in enumerate(NAMES):
    draw.text((10, r * 280 + 47), name, font=FONT, fill="#252a27")
    for c, (extra, frame) in enumerate([(False, 9), (True, 4), (False, 27), (True, 0)]):
        paint(comparison, name, extra, frame, False, c * 330, r * 280 + 290)
comparison.save(HERE / "living-coherence-comparison.png")

# Gezeichnete Frames beider Familien wirklich abwechseln: die erste Vorschau zeigte nur neue Posen.
sequence = ([(True, i, 450) for i in range(4, 8)] + [(True, 12, 120), (True, 13, 120)]
            + [(False, i, 95) for i in list(range(9, 17)) * 2]
            + [(True, 14, 120), (True, 15, 120), (True, 4, 450), (True, 9, 85), (True, 10, 85)]
            + [(True, i, 450) for i in range(4)] + [(False, 55, 160), (True, 0, 450)]
            + [(False, i, 200) for i in range(84, 88)] + [(True, i, 120) for i in range(24, 28)]
            + [(True, 31, 650)])
images = []
for extra, frame, duration in sequence:
    canvas = Image.new("RGB", (740, 1740), "#e1e4df")
    draw = ImageDraw.Draw(canvas)
    draw.text((90, 12), "Erste Integration", font=FONT, fill="#252a27")
    draw.text((430, 12), "Korrigierter Export", font=FONT, fill="#252a27")
    for r, name in enumerate(NAMES):
        draw.text((10, r * 280 + 47), name, font=FONT, fill="#252a27")
        for c, before in enumerate([True, False]):
            paint(canvas, name, extra, frame, before, 70 + c * 335, r * 280 + 290)
    images.append(canvas)
images[0].save(HERE / "living-atlas-preview.gif", save_all=True, append_images=images[1:],
               duration=[duration for _, _, duration in sequence], loop=0, disposal=2)
print("Vergleich und Gang-/Blinzel-/Landungswechsel exportiert; keine APK-Aufnahme.")
