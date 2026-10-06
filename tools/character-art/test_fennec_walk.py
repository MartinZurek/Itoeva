"""Prueft sichtbare Gangmerkmale im aktiven Sprite, nicht nur IK-Zielpunkte."""
from pathlib import Path
import unittest
import numpy as np
from PIL import Image


class DrawnWalkTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        path = Path(__file__).resolve().parents[2] / 'app-sim/src/main/assets/creatures/fennec.png'
        sheet = np.array(Image.open(path))
        cls.frames = [sheet[:,i*128:(i+1)*128] for i in range(9,17)]

    def test_body_height_stays_stable_and_every_frame_has_contact(self):
        tops = []
        for frame in self.frames:
            mask = frame[...,3] > 128
            ys, _ = np.nonzero(mask)
            self.assertEqual(125, ys.max())
            tops.append(ys.min())
            self.assertGreater(np.count_nonzero(mask[124:126]), 5)
        self.assertLessEqual(max(tops)-min(tops), 2)

    def test_passing_pose_clears_front_foot_while_support_stays_down(self):
        for i in (2,6):
            mask = self.frames[i][...,3] > 128
            # Gezeichnete Durchgangsphasen: vorderer Fuss ist angehoben,
            # die andere Sohle bleibt im linken Teil des Fussbereichs stehen.
            self.assertFalse(mask[122:126,64:].any())
            self.assertTrue(mask[124:126,40:64].any())
        legs = [f[100:].tobytes() for f in self.frames]
        self.assertEqual(8, len(set(legs)))

    def test_first_stance_moves_backwards_relative_to_body(self):
        forward_edge = []
        for frame in self.frames[:4]:
            sole = (frame[124:126,:,3] > 128).any(0)
            forward_edge.append(np.nonzero(sole)[0].max())
        self.assertTrue(all(b <= a for a,b in zip(forward_edge,forward_edge[1:])))
        self.assertGreater(forward_edge[0]-forward_edge[-1], 20)


if __name__ == '__main__':
    unittest.main()
