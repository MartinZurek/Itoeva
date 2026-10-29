"""Die gespeicherten Quellen müssen denselben fest registrierten Gang ergeben."""
import hashlib
import json
import unittest
from pathlib import Path

import numpy as np
from PIL import Image

PROJECT = Path(__file__).resolve().parents[1]
ROOT = PROJECT.parents[1]


class NpcMotionAssets(unittest.TestCase):
    def test_frozen_geometry_and_sources(self):
        data = json.loads((PROJECT / "data/npc_motion/ash_hound.json").read_text())
        walk, rest = data["walk"], data["rest"]
        self.assertEqual(walk["frames"], 8)
        self.assertEqual(rest["frames"], 4)
        for key in ("cell", "columns", "ground", "reference_height"):
            self.assertEqual(walk[key], rest[key])
        source = ROOT / "tools/motion-art/sources/ash-hound-motion-n1.png"
        for mode in (walk, rest):
            self.assertEqual(hashlib.sha256(source.read_bytes()).hexdigest(), mode["source_sha256"])
            original = PROJECT / mode["asset"].removeprefix("res://")
            self.assertEqual(hashlib.sha256(original.read_bytes()).hexdigest(), mode["sha256"])
            self.assertTrue(mode["fixed_geometry"])
            atlas = Image.open(original).convert("RGBA")
            pixel = Image.open(PROJECT / mode["pixel_asset"].removeprefix("res://")).convert("RGBA")
            self.assertEqual(atlas.size, pixel.size)
            drawings = []
            for frame in range(mode["frames"]):
                c = mode["cell"]; x = frame % mode["columns"] * c; y = frame // mode["columns"] * c
                tile = atlas.crop((x, y, x+c, y+c))
                alpha = np.asarray(tile)[:, :, 3]
                yy, xx = np.where(alpha >= 32)
                self.assertGreater(len(xx), 6000)
                self.assertGreater(min(xx.min(), yy.min(), c-1-xx.max(), c-1-yy.max()), 1)
                self.assertLessEqual(abs(yy.max()+1-mode["ground"]), 1)
                self.assertLess(np.mean(alpha > 32), .65)
                drawings.append(hashlib.sha256(tile.tobytes()).hexdigest())
            self.assertEqual(len(set(drawings)), mode["frames"])


if __name__ == "__main__":
    unittest.main()
