"""Prueft die gelieferten Bildzellen; aendert keine Bilddatei. Pillow nur fuer Offline-QA."""
from pathlib import Path
from PIL import Image

assets = Path(__file__).resolve().parents[1] / "assets"
atlas = Image.open(assets / "fennec-cast.png")
assert atlas.mode == "RGBA", "Fennec braucht echten Alphakanal"
alpha = atlas.getchannel("A")
assert alpha.getextrema()[0] == 0, "Hintergrund ist nicht transparent"
for index in range(8):
    col, row = index % 2, index // 2
    rect = tuple(round(value) for value in (
        col * atlas.width / 2, row * atlas.height / 4,
        (col + 1) * atlas.width / 2, (row + 1) * atlas.height / 4,
    ))
    cell = alpha.crop(rect).point(lambda value: 255 if value > 128 else 0)
    bounds = cell.getbbox()
    assert bounds is not None, f"Zelle {index} ist leer"
    assert bounds[0] > 2 and bounds[1] > 2, f"Zelle {index} beruehrt linke/obere Grenze"
    assert bounds[2] < cell.width - 2 and bounds[3] < cell.height - 2, f"Zelle {index} ist abgeschnitten"
    assert 0.15 < sum(cell.histogram()[128:]) / (cell.width * cell.height) < 0.70
for name in ("forest-floor.jpg", "forest-panorama.png"):
    with Image.open(assets / name) as image:
        image.verify()
assert atlas.width <= 2048 and atlas.height <= 2048
print("8 transparente, nicht abgeschnittene Posen; beide Boden-/Kulissenbilder lesbar")
