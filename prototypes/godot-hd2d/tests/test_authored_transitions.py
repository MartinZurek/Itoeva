"""Verbindungsposen müssen ohne neuen Maßstab in die gemalten Zyklen münden."""
import json
from pathlib import Path
import unittest

import numpy as np
from PIL import Image

PROJECT = Path(__file__).resolve().parents[1]


class AuthoredTransitionsTest(unittest.TestCase):
    def test_painted_endpoints_are_identical_to_the_reached_cycle(self):
        data = json.loads((PROJECT / "data/mosslyn_authored_cycles.json").read_text())
        for pixel in [False, True]:
            images = {}
            for mode, spec in data.items():
                path = PROJECT / spec["asset"].removeprefix("res://")
                if pixel:
                    path = path.parent / "pixel" / path.name
                images[mode] = np.array(Image.open(path))

            def tile(mode, frame):
                spec = data[mode]
                cell, cols = spec["cell"], spec["columns"]
                x, y = frame % cols * cell, frame // cols * cell
                return images[mode][y:y+cell, x:x+cell]

            for mode, frame, cycle, target in [
                ("lower", 0, "walk", 0), ("lower", 3, "run", 1),
                ("rise", 0, "run", 1), ("rise", 3, "walk", 0),
            ]:
                with self.subTest(pixel=pixel, mode=mode, frame=frame):
                    self.assertTrue(np.array_equal(tile(mode, frame), tile(cycle, target)))
                    self.assertEqual(data[mode]["reference_height"], data[cycle]["reference_height"])
                    self.assertEqual(data[mode]["ground"], data[cycle]["ground"])

    def test_gallop_has_grounded_push_and_real_drawn_flight(self):
        spec = json.loads((PROJECT / "data/mosslyn_authored_cycles.json").read_text())["run"]
        self.assertEqual(spec["frames"], 10)
        self.assertEqual(len(spec["phase_weights"]), spec["frames"])
        self.assertGreater(min(spec["phase_weights"]), 0)
        self.assertAlmostEqual(sum(spec["phase_weights"]), spec["cycle_distance_pixels"])
        self.assertLessEqual(abs(spec["bounds"][6][1] + spec["bounds"][6][3] - spec["ground"]), 1)
        for frame in [7, 8, 9]:
            self.assertLess(spec["bounds"][frame][1] + spec["bounds"][frame][3], spec["ground"] - 3)


if __name__ == "__main__":
    unittest.main()
