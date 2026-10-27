"""Flügelgeometrie darf Körpergröße und den Schwebeübergang nicht verändern."""
import hashlib
import json
import unittest
from pathlib import Path

import numpy as np
from PIL import Image

PROJECT = Path(__file__).resolve().parents[1]
ROOT = PROJECT.parents[1]


class NpcFlightAssets(unittest.TestCase):
    def test_flight_geometry_and_continuity(self):
        data = json.loads((PROJECT / "data/npc_motion/thorn_moth.json").read_text())
        self.assertEqual(data["locomotion"], "flight")
        self.assertEqual(data["wingbeats_per_second"], 1.4)
        walk, rest = data["walk"], data["rest"]
        for key in ("frames", "cell", "columns", "ground", "reference_height", "bounds"):
            self.assertEqual(walk[key], rest[key])
        self.assertEqual(walk["frames"], 8)
        for image_key in ("asset", "pixel_asset"):
            left = PROJECT / walk[image_key].removeprefix("res://")
            right = PROJECT / rest[image_key].removeprefix("res://")
            self.assertEqual(left.read_bytes(), right.read_bytes())
        for path, digest in walk["painted_sources_sha256"].items():
            self.assertEqual(hashlib.sha256((ROOT / path).read_bytes()).hexdigest(), digest)
        atlas = np.asarray(Image.open(PROJECT / walk["asset"].removeprefix("res://")).convert("RGBA"))
        # Der Schulter-/Kopfanker bleibt stehen. Die Flügelspitzen dürfen
        # ausdrücklich über/unter diesem Anker arbeiten, statt den Körper zu heben.
        for registration in walk["normalization"]:
            x,y,w,h = registration["head_region"]
            scale = registration["scale"]
            dx,dy = registration["offset"]
            self.assertAlmostEqual(w*scale,20)
            self.assertAlmostEqual((x+w*.5)*scale+dx,175)
            self.assertAlmostEqual((y+h*.5)*scale+dy,210)
        extents=[]
        for i in range(8):
            c=walk["cell"];x=i%4*c;y=i//4*c
            alpha=atlas[y:y+c,x:x+c,3]
            yy,xx=np.where(alpha>=32)
            self.assertGreater(min(xx.min(),yy.min(),c-1-xx.max(),c-1-yy.max()),1)
            extents.append(yy.max()-210)
        self.assertGreater(extents[4],extents[0]+25)


if __name__ == "__main__":
    unittest.main()
