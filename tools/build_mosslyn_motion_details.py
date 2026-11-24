"""Registriert Mooslyns Zusatzposen aus den vorhandenen Pixel-Zeichnungen.

Aufruf aus dem Projekt: python3 tools/build_mosslyn_motion_details.py
Benötigt Pillow. Die Bewegungsphasen in compact_actor.gd verwenden dieselbe Reihenfolge.
"""
import base64
import json
from pathlib import Path
import zlib

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE_CELL = 320
SOURCE_GROUND = 300
CELL = 192
GROUND = 178
COLUMNS = 8
ASSET = "assets/characters/pixel/mosslyn-motion-details.png"


def frames(name):
    with Image.open(ROOT / f"assets/characters/pixel/mosslyn-{name}.png") as source:
        sheet = source.convert("RGBA")
    return [
        sheet.crop((x, y, x + SOURCE_CELL, y + SOURCE_CELL))
        for y in range(0, sheet.height, SOURCE_CELL)
        for x in range(0, sheet.width, SOURCE_CELL)
    ]


def shape(pose, angle=0, width=1, height=1, x=0):
    """Verformt nur die Zeichnung und hält ihren untersten Kontakt auf Zeile 300."""
    crop = pose.crop(pose.getbbox())
    crop = crop.resize((round(crop.width * width), round(crop.height * height)), Image.Resampling.NEAREST)
    crop = crop.rotate(angle, Image.Resampling.NEAREST, expand=True)
    crop = crop.crop(crop.getbbox())
    result = Image.new("RGBA", (SOURCE_CELL, SOURCE_CELL))
    result.paste(crop, ((SOURCE_CELL - crop.width) // 2 + x, SOURCE_GROUND - crop.height))
    return result


def follow_through(pose, sway, nod):
    """Zieht das Geweih und den Umhang nach, während die Hufe am Ort bleiben."""
    pixels = pose.load()
    detail = pose.copy()
    for y in range(123, 271):
        weight = ((271 - y) / 148) ** 1.5
        dx, dy = round(sway * weight), round(nod * weight)
        detail.paste(pose.crop((0, y - dy, SOURCE_CELL, y - dy + 1)), (dx, y))
        for x in range(0, dx) if dx > 0 else range(SOURCE_CELL + dx, SOURCE_CELL):
            detail.putpixel((x, y), pixels[0, 0])
    return detail


def profile_roll(views, rest):
    bend, curl, inverted = views[11:14]
    return [
        rest, shape(bend, -12, 1.0, 1.13), bend, shape(curl, -25, 1.02, .92),
        shape(curl, -73, 1.0, .93), shape(inverted, 22, 1.0, .96), inverted,
        shape(inverted, -49, 1.04, .97), shape(inverted, -95, 1.0, .95),
        shape(inverted, -140, .98, .97), shape(inverted, -166, .98, 1.0),
        shape(bend, 12, 1.04, .9), shape(bend, 5, 1.0, 1.13),
        shape(rest, 0, 1.0, .94),
    ]


def depth_roll(stand, sit, opposite, *, away):
    # Am Scheitel sind kurz die umgekehrte Kopfrichtung und die Unterseite sichtbar.
    # Die Staffelung der Höhe deutet die Drehung in der Tiefe an.
    turned = opposite.transpose(Image.Transpose.FLIP_TOP_BOTTOM)
    side = -1 if away else 1
    return [
        stand, shape(stand, 0, 1.02, .87), shape(sit, -4 * side, 1.05, .78),
        shape(sit, -11 * side, 1.06, .67, 2 * side),
        shape(turned, 17 * side, 1.06, .46, 3 * side),
        shape(turned, 15 * side, 1.03, .69, 3 * side),
        shape(turned, 6 * side, .99, .83, 2 * side),
        shape(turned, -3 * side, 1.01, .83),
        shape(turned, -14 * side, 1.05, .67, -2 * side),
        shape(sit, 15 * side, 1.08, .47, -3 * side),
        shape(sit, 11 * side, 1.06, .65, -3 * side),
        shape(sit, 5 * side, 1.04, .80, -2 * side),
        shape(stand, 0, 1.02, .89), shape(stand, 0, 1.0, .97),
    ]


def build():
    views = frames("views-v3")
    rest = frames("rest-m4")[0]
    poses = profile_roll(views, rest)
    poses += depth_roll(views[0], views[9], views[10], away=False)
    poses += depth_roll(views[4], views[10], views[9], away=True)
    poses += [follow_through(views[i], 4 if i % 2 == 0 else -4, 3) for i in range(8)]
    for i in (14, 15):
        poses.extend([follow_through(views[i], 1, -6), follow_through(views[i], -1, 7)])
    for stand, sit in ((rest, views[8]), (views[0], views[9]), (views[4], views[10])):
        poses.extend([shape(stand, 0, 1.01, .96), shape(sit, 0, 1.01, .94)])

    rows = (len(poses) + COLUMNS - 1) // COLUMNS
    sheet = Image.new("RGBA", (CELL * COLUMNS, CELL * rows))
    mask = Image.new("L", sheet.size)
    bounds = []
    left = (SOURCE_CELL - CELL) // 2
    top = SOURCE_GROUND - GROUND
    for i, pose in enumerate(poses):
        x, y = i % COLUMNS * CELL, i // COLUMNS * CELL
        x0, y0, x1, y1 = pose.getbbox()
        x0, y0, x1, y1 = x0 - left, y0 - top, x1 - left, y1 - top
        if not (0 < x0 < x1 < CELL and 0 < y0 < y1 == GROUND):
            raise ValueError(f"Ungültiger Bodenanker in Frame {i}")
        sheet.paste(pose.crop((left, top, left + CELL, top + CELL)), (x, y))
        mask.paste(i + 1, (x + x0, y + y0, x + x1, y + y1))
        bounds.append([x0, y0, x1 - x0, y1 - y0])
    sheet.save(ROOT / ASSET, optimize=True)
    data = {
        "asset": "res://" + ASSET,
        "cell": CELL,
        "columns": COLUMNS,
        "rows": rows,
        "frames": len(poses),
        "ground": GROUND,
        "reference_height": 168,
        "fixed_geometry": True,
        "bounds": bounds,
        "width": sheet.width,
        "height": sheet.height,
        "mask_deflate": base64.b64encode(zlib.compress(mask.tobytes(), 9)).decode("ascii"),
    }
    (ROOT / "data/mosslyn_motion_details.json").write_text(
        json.dumps(data, indent=2) + "\n", encoding="utf-8"
    )


if __name__ == "__main__":
    build()
