import unittest
import numpy as np
import from_concept as F


class ConceptSheetTest(unittest.TestCase):
    def test_every_species_has_17_frames_standing_on_the_foot_line(self):
        for name in F.ORDER:
            with self.subTest(species=name):
                fr = F.frames(name)
                self.assertEqual(17, len(fr))
                for f in fr:
                    self.assertEqual((64, 64, 4), f.shape)
                    rows = np.nonzero((f[..., 3] > 0).any(1))[0]
                    self.assertTrue(len(rows) > 0)
                    self.assertLessEqual(int(rows[-1]), F.FOOT - 1)
                for i in (0, 9, 11, 12):
                    rows = np.nonzero((fr[i][..., 3] > 0).any(1))[0]
                    self.assertEqual(F.FOOT - 1, int(rows[-1]))
                self.assertFalse(np.array_equal(fr[11], fr[12]))


if __name__ == '__main__':
    unittest.main()
