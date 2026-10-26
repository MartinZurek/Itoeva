"""Beinzuordnung und Kontaktlücken im gemalten Original offen ausweisen.

Ein Rechteck benennt dieselbe anatomische Pfote, auch wenn sie angehoben ist.
Die untersten Alphazeilen liefern nur einen Sohlenmittelpunkt, keinen gemessenen
Druckpunkt. Ein fehlender Kontakt darf nicht durch die andere Pfote ersetzt werden.
"""
import argparse
import hashlib
import json
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
PROJECT = ROOT / "prototypes/godot-hd2d"


def measure(annotation):
    data = json.loads(annotation.read_text())
    cycles = json.loads((PROJECT / "data/mosslyn_authored_cycles.json").read_text())
    spec = cycles[data["mode"]]
    path = PROJECT / spec["asset"].removeprefix("res://")
    if hashlib.sha256(path.read_bytes()).hexdigest() != spec["sha256"]:
        raise ValueError("Atlas und Metadaten widersprechen sich")
    if data["asset"] != spec["asset"]:
        raise ValueError("Annotation gehört zu einem anderen Atlas")
    image = np.array(Image.open(path))
    contacts = []
    for i, frame in enumerate(data["frames"]):
        cell = spec["cell"]
        tile = image[i // spec["columns"] * cell:(i // spec["columns"] + 1) * cell,
                     i % spec["columns"] * cell:(i % spec["columns"] + 1) * cell]
        feet = {}
        for side, box in frame["feet"].items():
            x, y, w, h = box
            yy, xx = np.where(tile[y:y+h, x:x+w, 3] >= 32)
            if not len(xx):
                raise ValueError(f"Phase {i}, {side}: keine Pfote im Rechteck")
            bottom = int(yy.max())
            sole = xx[yy >= bottom - 5]
            gap = int(spec["ground"] - (y + bottom))
            feet[side] = dict(sole_x=float(x + (sole.min() + sole.max()) / 2),
                              ground_gap=gap, contact=abs(gap) <= 3, box=box)
        contacts.append(feet)
    intervals = []
    for i, side in enumerate(["near"] * 4 + ["far"] * 4):
        j = (i + 1) % len(contacts)
        a, b = contacts[i][side], contacts[j][side]
        valid = a["contact"] and b["contact"]
        travel = a["sole_x"] - b["sole_x"] if valid else None
        intervals.append(dict(start=i, end=j, leg=side, both_endpoints_contact=valid,
                              travel_pixels=travel,
                              uniform_phase_world_travel_pixels=spec["cycle_distance_pixels"] / 8,
                              uniform_phase_slip_pixels=(spec["cycle_distance_pixels"] / 8 - travel)
                              if valid else None))
    complete = all(i["both_endpoints_contact"] for i in intervals)
    total = sum(i["travel_pixels"] for i in intervals if i["travel_pixels"] is not None)
    geometry = json.loads((PROJECT / "data/visual_geometry.json").read_text())
    characters = json.loads((PROJECT / "data/new_characters.json").read_text())
    height = geometry["character_height"].get("mosslyn", characters["mosslyn"]["relative_height"])
    pixel_size = .0145 * 109 * height / spec["reference_height"]
    return dict(method="anatomically-annotated-visible-sole-midpoints",
                pressure_points_verified=False, runtime_slip_verified=False,
                atlas_sha256=spec["sha256"], painted_source_sha256=spec["source_sha256"],
                contacts=contacts, intervals=intervals, complete_contact_coverage=complete,
                observed_travel_pixels=total,
                estimated_cycle_pixels=total if complete else None,
                preview_cycle_pixels=spec["cycle_distance_pixels"],
                preview_cycle_seconds=spec["cycle_distance_pixels"] * pixel_size / 2.35,
                preview_phase_seconds=spec["cycle_distance_pixels"] * pixel_size / 2.35 / 8,
                limitation="Manuell zugeordnete Pfoten, Sohlenmitten statt Druckpunkten. "
                           "Kontakt nur an diskreten Posen; Abrollen dazwischen ungeprüft. "
                           "Fehlende Endpunktkontakte bleiben null; kein Ersatz durch das Gegenbein.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("annotation", type=Path)
    args = parser.parse_args()
    print(json.dumps(measure(args.annotation), ensure_ascii=False, indent=2))
