"""Standpfotenweg im Original messen; keine Behauptung exakter Druckpunkte.

Die untersten sechs Alphazeilen liefern den sichtbaren Sohlenumriss. Dessen
Mitte ist eine Näherung: Abrollen und Druckpunktverschiebung sind nicht bekannt.
Die ausgewählte Pfote folgt derselben anatomischen Seite über eine Standphase.
"""
import json
from pathlib import Path
import numpy as np
from PIL import Image
from scipy import ndimage

PROJECT = Path(__file__).resolve().parents[2] / "prototypes/godot-hd2d"


def measure():
    spec = json.loads((PROJECT / "data/mosslyn_authored_cycles.json").read_text())["walk"]
    image = np.array(Image.open(PROJECT / spec["asset"].removeprefix("res://")))
    cell, ground = spec["cell"], spec["ground"]
    soles = []
    for i in range(8):
        tile = image[i // 4 * cell:(i // 4+1)*cell, i % 4*cell:(i % 4+1)*cell]
        line = (tile[ground-6:ground+1, :, 3] >= 32).any(axis=0)
        labels, _ = ndimage.label(line)
        groups = ndimage.find_objects(labels)
        if not groups:
            raise ValueError(f"Phase {i}: keine sichtbare Standsohle")
        soles.append([(s[0].start+s[0].stop-1)/2 for s in groups])
    near = [soles[0][-1], soles[1][-1], soles[2][0], soles[3][0], soles[4][0]]
    far = [soles[4][-1], soles[5][-1], soles[6][0], soles[7][0], soles[0][0]]
    cycle_pixels = (near[0]-near[-1]) + (far[0]-far[-1])
    # Außenweltmaßstab aus world_stage.gd und character_height, unverändert.
    geometry = json.loads((PROJECT / "data/visual_geometry.json").read_text())
    characters = json.loads((PROJECT / "data/new_characters.json").read_text())
    height = geometry["character_height"].get("mosslyn", characters["mosslyn"]["relative_height"])
    pixel_size = .0145 * 109 * height / spec["reference_height"]
    measured_world = cycle_pixels * pixel_size
    return dict(method="visible-sole-midpoint-estimate", pressure_points_verified=False,
                source_sha256=spec["sha256"], near_stance_x=near, far_stance_x=far,
                phase_travel_pixels=[near[i]-near[i+1] for i in range(4)] + [far[i]-far[i+1] for i in range(4)],
                estimated_cycle_pixels=cycle_pixels, estimated_cycle_world=measured_world,
                preview_cycle_world=spec["cycle_distance_pixels"]*pixel_size,
                speed_world_per_second=2.35, estimated_required_cycles_per_second=2.35/measured_world,
                preview_cycles_per_second=2.35/(spec["cycle_distance_pixels"]*pixel_size),
                limitation="Sohlenmitten statt Druckpunkten; Phase7→0 hat keinen sichtbaren Rückweg. Phasenabstände und Gegenbein bleiben nachzubessern.")


if __name__ == "__main__":
    print(json.dumps(measure(), ensure_ascii=False, indent=2))
