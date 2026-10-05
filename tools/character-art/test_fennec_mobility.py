"""Vertraege der wirklich ausgelieferten Front-/Rueckgaenge und Aktionsbilder."""
from pathlib import Path
import unittest
import numpy as np
from PIL import Image
import fennec_mobility as M


class MobilityTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        path = Path(__file__).resolve().parents[2] / 'app-sim/src/main/assets/creatures/fennec.png'
        sheet = np.asarray(Image.open(path))
        cls.frames = [sheet[:, i*128:(i+1)*128] for i in range(M.FRAME_COUNT)]

    def test_new_walks_keep_contact_and_have_eight_distinct_leg_phases(self):
        for first in (68, 76):
            tops = []
            legs = []
            contacts = []
            for frame in self.frames[first:first+8]:
                mask = frame[..., 3] > 128
                y, x = np.nonzero(mask)
                self.assertEqual(125, y.max())
                self.assertGreater(np.count_nonzero(mask[124:126]), 4)
                self.assertGreater(x.min(), 0)
                self.assertLess(x.max(), 127)
                tops.append(y.min())
                legs.append(frame[109:].tobytes())
                contacts.append(mask[124:126].tobytes())
            self.assertEqual(8, len(set(legs)))
            self.assertGreaterEqual(len(set(contacts)), 4)
            self.assertLessEqual(max(tops)-min(tops), 4)

    def test_bending_sitting_and_sleep_lower_body_instead_of_only_shifting_it(self):
        def height(i):
            y = np.nonzero(self.frames[i][..., 3] > 128)[0]
            return y.max()-y.min()
        standing = height(95)
        self.assertGreater(standing-height(89), 10)
        self.assertGreater(standing-height(93), 10)
        self.assertGreater(standing-height(99), 25)
        self.assertNotEqual(self.frames[88].tobytes(), self.frames[89].tobytes())

    def test_every_new_action_fits_frame_with_ground_anchor(self):
        for i in range(84, M.FRAME_COUNT):
            frame = self.frames[i]
            y, x = np.nonzero(frame[..., 3] > 128)
            self.assertEqual(125, y.max(), i)
            self.assertGreater(y.min(), 0, i)
            self.assertGreater(x.min(), 0, i)
            self.assertLess(x.max(), 127, i)
            self.assertFalse(frame[126:, :, 3].any(), i)

    def test_blink_keeps_new_front_body_size_and_floor(self):
        def bounds(i):
            y, x = np.nonzero(self.frames[i][..., 3] > 128)
            return np.array([x.min(), y.min(), x.max(), y.max()])
        self.assertLessEqual(np.abs(bounds(55)-bounds(27)).max(), 3)
        self.assertNotEqual(self.frames[55].tobytes(), self.frames[27].tobytes())

    def test_run_and_roll_are_drawn_body_changes_with_unique_frames(self):
        def height(i):
            y = np.nonzero(self.frames[i][..., 3] > 128)[0]
            return y.max()-y.min()
        self.assertGreater(height(95)-height(114), 20)
        for first, count in [(114, 8), (122, 4), (126, 4), (130, 8)]:
            self.assertEqual(count, len({f.tobytes() for f in self.frames[first:first+count]}))
        self.assertLess(height(133), height(137))

    def test_source_drawings_are_complete_and_directional_views_are_distinct(self):
        for file, cols, rows in [('fennec-walk-directions-atlas.png', 4, 4),
                                 ('fennec-actions-atlas.png', 4, 4),
                                 ('fennec-actions-directions-atlas.png', 7, 2),
                                 ('fennec-run-atlas.png', 4, 4), ('fennec-roll-atlas.png', 4, 2)]:
            self.assertEqual(cols*rows, len(M.drawings(file, cols, rows)))
        self.assertNotEqual(self.frames[68].tobytes(), self.frames[76].tobytes())
        self.assertNotEqual(self.frames[104].tobytes(), self.frames[111].tobytes())


if __name__ == '__main__':
    unittest.main()
