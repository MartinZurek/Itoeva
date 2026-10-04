import unittest
import numpy as np
from PIL import Image
import sheets


class DirectionFramesTest(unittest.TestCase):
    def test_each_species_has_distinct_front_and_back_with_same_ground(self):
        for name, draw in sheets.SPECIES.items():
            with self.subTest(species=name):
                frames = sheets.grounded(sheets.frames(draw, name))
                self.assertEqual(17, len(frames))
                rgba = [np.array(frame) for frame in frames]
                self.assertFalse(np.array_equal(rgba[11], rgba[12]))
                self.assertFalse(np.array_equal(rgba[13], rgba[15]))
                for frame in rgba:
                    self.assertEqual((64, 64, 4), frame.shape)
                    ys = np.nonzero(frame[:, :, 3].any(axis=1))[0]
                    self.assertTrue(len(ys) > 0)
                for frame in rgba[11:]:
                    ys = np.nonzero(frame[:, :, 3].any(axis=1))[0]
                    self.assertLessEqual(int(ys[-1]), sheets.FOOT - 1)


if __name__ == '__main__':
    unittest.main()
