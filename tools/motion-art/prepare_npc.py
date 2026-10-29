"""Registrierte Originalzeichnungen + gleicher HD-2D-Pixelpass für Nebenfiguren.

Annotationen bleiben einzeln versioniert. Kein pro Frame verformtes Standbild.
"""
import argparse
import importlib.util
import json
from pathlib import Path

from prepare_cycle import PROJECT, ROOT, prepare


def build(character, height, locomotion="ground", wingbeats=1.4, checkpoint="n1"):
    modes = dict(prepare(ROOT / f"tools/motion-art/{character}-{mode}-{checkpoint}.json")
                 for mode in ("walk", "rest"))
    spec = importlib.util.spec_from_file_location("pixel_pass", ROOT / "tools/pixel-pass/pixel_pass.py")
    pixel = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(pixel)
    pixel.SPECIES[character] = height
    report = {}
    pixel.process_species(str(PROJECT / "assets/npc_motion"), character, 56, report,
                          modes["walk"]["reference_height"], modes["walk"]["cell"])
    for mode in modes.values():
        path = Path(mode["asset"].removeprefix("res://"))
        mode["pixel_asset"] = "res://" + (path.parent / "pixel" / path.name).as_posix()
    result = dict(world_height=height, walk=modes["walk"], rest=modes["rest"])
    if locomotion == "flight":
        result.update(locomotion="flight", wingbeats_per_second=wingbeats)
    output = PROJECT / f"data/npc_motion/{character.replace('-', '_')}.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, indent=2) + "\n")
    report_path = ROOT / f"tools/motion-art/{character}-{checkpoint}-pixelpass.json"
    report_path.write_text(json.dumps(report, indent=2) + "\n")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("character")
    parser.add_argument("--height", type=float, required=True)
    parser.add_argument("--checkpoint", default="n1")
    parser.add_argument("--locomotion", choices=["ground", "flight"], default="ground")
    parser.add_argument("--wingbeats", type=float, default=1.4)
    args = parser.parse_args()
    build(args.character, args.height, args.locomotion, args.wingbeats, args.checkpoint)
