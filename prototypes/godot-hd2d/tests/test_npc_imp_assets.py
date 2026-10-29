"""Gesicherte gemalte Quellen, Originalmasken und feste Körperreferenz."""
import base64
import hashlib
import json
import unittest
import zlib
from pathlib import Path
import numpy as np
from PIL import Image
PROJECT = Path(__file__).resolve().parents[1]
ROOT = PROJECT.parents[1]
class ImpAssets(unittest.TestCase):
    def test_registered_sources(self):
        data = json.loads((PROJECT / "data/npc_motion/glut_imp.json").read_text())
        walk, rest = data["walk"], data["rest"]
        self.assertEqual((walk["frames"],rest["frames"]),(8,4))
        self.assertEqual(data["world_height"],1.75)
        for key in ("cell","columns","ground","reference_height","cycle_distance_pixels"):
            self.assertEqual(walk[key],rest[key])
        for name,mode in [("walk",walk),("rest",rest)]:
            spec = json.loads((ROOT / f"tools/motion-art/glut-imp-{name}-n4r2.json").read_text())
            self.assertEqual(hashlib.sha256((ROOT/spec["source"]).read_bytes()).hexdigest(),mode["source_sha256"])
            for source,checksum in mode.get("painted_sources_sha256",{}).items():
                self.assertEqual(hashlib.sha256((ROOT/source).read_bytes()).hexdigest(),checksum)
                self.assertTrue((ROOT/source).with_suffix(".txt").is_file())
            path=PROJECT/mode["asset"].removeprefix("res://")
            self.assertEqual(hashlib.sha256(path.read_bytes()).hexdigest(),mode["sha256"])
            atlas=Image.open(path).convert("RGBA")
            pixel=Image.open(PROJECT/mode["pixel_asset"].removeprefix("res://")).convert("RGBA")
            self.assertEqual(atlas.size,pixel.size)
            mask=np.frombuffer(zlib.decompress(base64.b64decode(mode["mask_deflate"])),dtype=np.uint8).reshape(atlas.height,atlas.width)
            drawings=set()
            self.assertTrue(mode["fixed_geometry"])
            for i,r in enumerate(mode["normalization"]):
                x,y,w,h=r["head_region"]; scale=r["scale"]; horizontal=-scale if r.get("mirror_x",False) else scale
                self.assertAlmostEqual((x+w/2)*horizontal+r["offset"][0],150)
                self.assertAlmostEqual(w*scale,14)
                c=mode["cell"]; px=i%mode["columns"]*c;py=i//mode["columns"]*c
                tile=np.array(atlas.crop((px,py,px+c,py+c)));alpha=tile[:,:,3]>=32;yy,xx=np.where(alpha)
                self.assertGreater(len(xx),6000)
                self.assertGreater(min(xx.min(),yy.min(),c-1-xx.max(),c-1-yy.max()),1)
                self.assertLessEqual(abs(yy.max()+1-mode["ground"]),1)
                self.assertLess(np.mean(alpha),.65)
                np.testing.assert_array_equal(mask[py:py+c,px:px+c]==i+1,alpha)
                drawings.add(hashlib.sha256(tile.tobytes()).hexdigest())
            self.assertEqual(len(drawings),mode["frames"])
if __name__ == "__main__": unittest.main()
