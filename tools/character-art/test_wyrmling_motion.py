"""Abnahme der ausgelieferten Anatomie, Kontaktpunkte und Bildregistrierung."""
import re
import unittest
import numpy as np
from PIL import Image
import ensemble_motion as E
import wyrmling_motion as W


class WyrmlingMotionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.sheet = np.asarray(Image.open(E.ASSETS/'wyrmling.png').convert('RGBA'))
        cls.frames = cls.sheet.reshape(128, 138, 128, 4).transpose(1, 0, 2, 3)

    def test_supporting_forepaw_stays_planted_through_sit_and_rise(self):
        contacts = []
        for index in (95, 92, 93, 94, 95):
            y, x = np.nonzero(self.frames[index, ..., 3])
            self.assertEqual(125, int(y.max()))
            contacts.append(int(x[y >= 122].max()))
        self.assertEqual(1, len(set(contacts)), 'Stuetzpfote rutscht seitlich')

    def test_tail_does_not_drag_head_axis_during_directed_sitting(self):
        for sequence in ((104, 100, 103, 100, 104), (111, 107, 110, 107, 111)):
            centers = []
            for index in sequence:
                y, x = np.nonzero(self.frames[index, ..., 3])
                head = y < y.min() + int((y.max()-y.min()+1)*.30)
                centers.append((x[head].min()+x[head].max())/2)
            self.assertLessEqual(max(centers)-min(centers), 1)

    def test_seated_front_has_folded_haunches_instead_of_scaled_standing_legs(self):
        def width(index):
            y, x = np.nonzero(self.frames[index, ..., 3])
            return np.ptp(x[y >= 110])+1
        self.assertGreater(width(103), width(104)*1.5)
        for stand, lower, seated in ((104, 100, 103), (111, 107, 110)):
            tops = [np.nonzero(self.frames[i, ..., 3])[0].min() for i in (stand, lower, seated)]
            self.assertLess(tops[0], tops[1])
            self.assertLessEqual(tops[1], tops[2])

    def test_side_blink_keeps_every_pixel_outside_the_eye(self):
        changed = np.any(self.frames[8] != self.frames[95], axis=-1)
        allowed = np.zeros((128, 128), bool)
        for x0, y0, x1, y1 in W.SIDE_EYES:
            allowed[y0:y1, x0:x1] = True
        self.assertTrue(changed.any())
        self.assertFalse((changed & ~allowed).any())

    def test_world_height_survives_the_higher_source_resolution(self):
        kotlin = (E.HERE/'../../app-sim/src/main/java/com/notime/glyphsim/matrix/CreatureSprites.kt').read_text()
        scale = float(re.search(r'AvatarSpecies.WYRMLING -> ([0-9.]+)f', kotlin).group(1))
        self.assertEqual(scale, E.WORLD_SCALE['wyrmling'])
        y = np.nonzero(self.frames[0, ..., 3])[0]
        self.assertAlmostEqual(61*1.71, (y.max()-y.min()+1)*scale, delta=2)

    def test_sources_regenerate_the_shipped_pixels(self):
        regenerated = np.uint8(np.clip(np.concatenate(W.frames(), axis=1), 0, 1)*255)
        self.assertTrue(np.array_equal(self.sheet, regenerated))


if __name__ == '__main__':
    unittest.main()
