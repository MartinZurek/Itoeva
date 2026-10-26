"""Gemalte Phasen einmalig registrieren; zur Laufzeit bleibt der Maßstab fest.

Die Annotation beschreibt Kopfmerkmale und echte Bodenabstände im Quellbild.
Eine Vorlage darf nicht allein wegen erfolgreicher Registrierung als guter Gang
gelten: Kontaktfolge und Gestaltung brauchen weiterhin eine visuelle Prüfung.
"""
import argparse
import base64
import hashlib
import json
import zlib
from pathlib import Path

import cv2
import numpy as np
from PIL import Image
from scipy import ndimage

ROOT = Path(__file__).resolve().parents[2]
PROJECT = ROOT / "prototypes/godot-hd2d"


def prepare(annotation):
    spec = json.loads(annotation.read_text())
    source = ROOT / spec["source"]
    rgba = np.array(Image.open(source).convert("RGBA"))
    labels, _ = ndimage.label(rgba[:, :, 3] >= 32)
    components = []
    for ident, box in enumerate(ndimage.find_objects(labels), 1):
        if box and np.count_nonzero(labels[box] == ident) > 2000:
            components.append((ident, box))
    # Reihenfolge nach Zellenmitte, nicht nach oberstem Geweihpixel.
    components.sort(key=lambda item: (
        (item[1][0].start + item[1][0].stop) // (2 * spec["source_row_height"]),
        item[1][1].start))
    if len(components) != len(spec["frames"]):
        raise ValueError("Phasenanzahl und getrennte Alphakomponenten widersprechen sich")
    cell, columns = spec["cell"], spec["columns"]
    rows = (len(components) + columns - 1) // columns
    atlas = Image.new("RGBA", (cell * columns, cell * rows))
    mask = np.zeros((cell * rows, cell * columns), dtype=np.uint8)
    bounds, registrations = [], []
    for index, ((ident, box), frame) in enumerate(zip(components, spec["frames"])):
        x, y, w, h = frame["head_region"]
        scale = spec["head_width"] / w
        # Eine einheitliche Transformation je ganzer Zeichnung, nie Gliedmaßen-
        # oder Gesichtsverformung. Dieselbe Geometrie wird danach eingefroren.
        dx = spec["head_center_x"] - (x + w / 2) * scale
        ground = box[0].stop + frame.get("air_gap", 0)
        dy = spec["ground"] - ground * scale
        own = rgba.copy()
        own[labels != ident] = 0
        tile = cv2.warpAffine(own, np.float32([[scale, 0, dx], [0, scale, dy]]),
                              (cell, cell), flags=cv2.INTER_LANCZOS4)
        tile[tile[:, :, 3] < 8] = 0
        alpha = tile[:, :, 3] >= 32
        yy, xx = np.where(alpha)
        if min(xx.min(), yy.min(), cell - 1 - xx.max(), cell - 1 - yy.max()) < 2:
            raise ValueError(f"Phase {index}: Kontur berührt den Zellrand")
        bounds.append([int(xx.min()), int(yy.min()), int(xx.max()-xx.min()+1), int(yy.max()-yy.min()+1)])
        px, py = index % columns * cell, index // columns * cell
        atlas.paste(Image.fromarray(tile), (px, py))
        mask[py:py+cell, px:px+cell][alpha] = index + 1
        registrations.append(dict(scale=scale, offset=[dx, dy], head_region=[x,y,w,h]))
    path = PROJECT / spec["asset"].removeprefix("res://")
    atlas.save(path)
    result = {key: spec[key] for key in ("asset", "cell", "columns", "ground", "reference_height", "cycle_distance_pixels")}
    result.update(rows=rows, frames=len(components), width=atlas.width, height=atlas.height,
                  fixed_geometry=True, bounds=bounds,
                  sha256=hashlib.sha256(path.read_bytes()).hexdigest(),
                  source_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),
                  mask_deflate=base64.b64encode(zlib.compress(mask.tobytes(), 9)).decode(),
                  normalization=registrations, review=spec["review"])
    return spec["mode"], result


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("annotations", nargs="+", type=Path)
    args = parser.parse_args()
    data = dict(prepare(path) for path in args.annotations)
    (PROJECT / "data/mosslyn_authored_cycles.json").write_text(json.dumps(data, indent=2) + "\n")
