"""Art-Vertrag: neue Ansichten, konsistente Halsanker und lebendige Ausgabe-Frames."""
import unittest
from pathlib import Path
import numpy as np
from PIL import Image
import fennec_faces as F
import fennec_key as K


class FaceAtlasTest(unittest.TestCase):
    def test_all_heads_are_visible_and_registered_at_neck(self):
        for name in F.HEADS:
            art, neck = F.head(name)
            self.assertEqual(180, art.height)
            self.assertGreater(neck, 0)
            self.assertLess(neck, art.width)
            assembled = np.array(F.attach(K.source(), name))
            self.assertTrue((assembled[189:193, 190:206, 3] > 128).any(), name)
            # Ein breiter Uebergang am Kragen statt der alten isolierten Halsspitze.
            self.assertGreater(np.count_nonzero(assembled[190, 170:225, 3] > 128), 40, name)

    def test_orientations_and_expressions_are_distinct_drawings(self):
        variants = [np.array(F.attach(K.source(), name))[:193].tobytes() for name in F.HEADS]
        self.assertEqual(len(variants), len(set(variants)))

    def test_facial_reaction_uses_matching_body_pose(self):
        plan = K.plan()
        self.assertEqual(68, len(plan))
        self.assertTrue(all(face == 'happy' for face, _ in plan[17:23]))
        self.assertEqual('front_blink', plan[55][0])
        self.assertTrue(all(face == 'front_happy' for face, _ in plan[56:62]))
        self.assertTrue(all(face == 'back' for face, _ in plan[62:68]))

    def test_generated_sheet_has_moving_front_and_back_idle(self):
        path = Path(__file__).resolve().parents[2] / 'app-sim/src/main/assets/creatures/fennec.png'
        sheet = Image.open(path)
        self.assertEqual((68*128, 128), sheet.size)
        for first in (0,39,47):
            frames = [sheet.crop((i*128,0,(i+1)*128,128)).tobytes() for i in range(first,first+8)]
            self.assertGreater(len(set(frames)), 4)

if __name__ == '__main__':
    unittest.main()
