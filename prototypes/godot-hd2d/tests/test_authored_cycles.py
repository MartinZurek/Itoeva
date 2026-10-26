"""Alphaeigentum und feste Geometrie prüfen, keine Kunstabnahme simulieren."""
import base64
import hashlib
import json
from pathlib import Path
import unittest
import zlib

import numpy as np
from PIL import Image

PROJECT = Path(__file__).resolve().parents[1]


class AuthoredCyclesTest(unittest.TestCase):
    def test_atlas_mask_bounds_and_ground(self):
        data = json.loads((PROJECT / "data/mosslyn_authored_cycles.json").read_text())
        for mode, spec in data.items():
            with self.subTest(mode=mode):
                path = PROJECT / spec["asset"].removeprefix("res://")
                self.assertEqual(hashlib.sha256(path.read_bytes()).hexdigest(), spec["sha256"])
                image = np.array(Image.open(path))
                self.assertEqual(image.shape, (spec["height"], spec["width"], 4))
                mask = np.frombuffer(zlib.decompress(base64.b64decode(spec["mask_deflate"])), np.uint8).reshape(image.shape[:2])
                self.assertEqual(set(np.unique(mask)), set(range(spec["frames"] + 1)))
                self.assertEqual(len(spec["bounds"]), spec["frames"])
                for i, b in enumerate(spec["bounds"]):
                    yy, xx = np.where(mask == i + 1)
                    cell = spec["cell"]
                    self.assertEqual([int(xx.min()) - i % spec["columns"] * cell,
                                      int(yy.min()) - i // spec["columns"] * cell,
                                      int(xx.max()-xx.min()+1), int(yy.max()-yy.min()+1)], b)
                    self.assertTrue(np.all(image[:, :, 3][mask == i+1] >= 32))
                    # Ohne diesen Test würde der Laufzeit-Anker eine gemalte
                    # Flugphase wieder auf den Boden ziehen.
                    self.assertLessEqual(b[1] + b[3], spec["ground"] + 1)
                self.assertTrue(spec["fixed_geometry"])

    def test_pixel_copy_preserves_grid_and_is_readable(self):
        data = json.loads((PROJECT / "data/mosslyn_authored_cycles.json").read_text())
        for spec in data.values():
            source = PROJECT / spec["asset"].removeprefix("res://")
            with Image.open(source.parent / "pixel" / source.name) as image:
                image.load()
                self.assertEqual(image.size, (spec["width"], spec["height"]))
                self.assertEqual(image.mode, "RGBA")


if __name__ == "__main__":
    unittest.main()
