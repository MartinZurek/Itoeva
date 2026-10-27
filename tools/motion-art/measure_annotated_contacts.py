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


def measure(annotation, cycles_path=None):
    data = json.loads(annotation.read_text())
    cycles = json.loads((cycles_path or PROJECT / "data/mosslyn_authored_cycles.json").read_text())
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
    legs = data.get("contact_legs", ["near"] * 4 + ["far"] * 4)
    if len(legs) != len(contacts):
        raise ValueError("Kontaktintervalle und Phasenzahl widersprechen sich")
    weights = spec.get("phase_weights", [1] * len(contacts))
    for i, side in enumerate(legs):
        j = (i + 1) % len(contacts)
        a, b = (contacts[i][side], contacts[j][side]) if side else (None, None)
        valid = side is not None and a["contact"] and b["contact"]
        travel = a["sole_x"] - b["sole_x"] if valid else None
        intervals.append(dict(start=i, end=j, leg=side, both_endpoints_contact=valid,
                              travel_pixels=travel,
                              uniform_phase_world_travel_pixels=spec["cycle_distance_pixels"] / len(contacts),
                              assigned_phase_world_travel_pixels=spec["cycle_distance_pixels"] * weights[i] / sum(weights),
                              support_midpoint_residual_pixels=(spec["cycle_distance_pixels"] * weights[i] / sum(weights) - travel)
                              if valid else None,
                              uniform_phase_slip_pixels=(spec["cycle_distance_pixels"] / len(contacts) - travel)
                              if valid else None))
    complete = all(i["both_endpoints_contact"] for i in intervals)
    total = sum(i["travel_pixels"] for i in intervals if i["travel_pixels"] is not None)
    geometry = json.loads((PROJECT / "data/visual_geometry.json").read_text())
    characters = json.loads((PROJECT / "data/new_characters.json").read_text())
    species = data.get("species", "mosslyn")
    height = geometry["character_height"].get(species, characters[species]["relative_height"])
    pixel_size = .0145 * 109 * height / spec["reference_height"]
    speed = data.get("speed_world_per_second", 2.35)
    seconds = spec["cycle_distance_pixels"] * pixel_size / speed
    return dict(method="anatomically-annotated-visible-sole-midpoints",
                pressure_points_verified=False, runtime_slip_verified=False,
                atlas_sha256=spec["sha256"], painted_source_sha256=spec["source_sha256"],
                contacts=contacts, intervals=intervals, complete_contact_coverage=complete,
                observed_travel_pixels=total,
                estimated_cycle_pixels=total if complete else None,
                preview_cycle_pixels=spec["cycle_distance_pixels"],
                speed_world_per_second=speed, preview_cycle_seconds=seconds,
                preview_phase_seconds=seconds / len(contacts),
                assigned_phase_seconds=[seconds * w / sum(weights) for w in weights],
                limitation="Manuell zugeordnete Pfoten, Sohlenmitten statt Druckpunkten. "
                           "Kontakt nur an diskreten Posen; Abrollen dazwischen ungeprüft. "
                           "Fehlende Endpunktkontakte bleiben null; kein Ersatz durch das Gegenbein. "
                           + data.get("limitation", ""))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("annotation", type=Path)
    parser.add_argument("--cycles", type=Path)
    args = parser.parse_args()
    print(json.dumps(measure(args.annotation, args.cycles), ensure_ascii=False, indent=2))
