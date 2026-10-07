"""Prueft die ausgelieferten Bilder statt nur den Aufbau des Generators."""
import hashlib
import unittest
from pathlib import Path
import numpy as np
from PIL import Image
import ensemble_motion as E


class EnsembleMotionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.sheets={name:np.asarray(Image.open(E.ASSETS/f'{name}.png').convert('RGBA'))
                    .reshape(128,138,128,4).transpose(1,0,2,3) for name in E.NAMES}

    def test_all_roles_have_complete_transparent_silhouettes(self):
        for name,frames in self.sheets.items():
            for i,frame in enumerate(frames):
                with self.subTest(species=name,frame=i):
                    ys,xs=np.nonzero(frame[...,3])
                    self.assertGreater(len(xs),200)
                    self.assertGreater(xs.min(),0)
                    self.assertLess(xs.max(),127)
                    self.assertGreater(ys.min(),0)
                    self.assertLessEqual(ys.max(),125)
                    self.assertEqual({0,255},set(np.unique(frame[...,3])))

    def test_gait_phases_contain_body_changes_not_only_translation(self):
        for name,frames in self.sheets.items():
            walk=frames[9:17]
            # Eine ausgeschnittene Standzeichnung hat stets dieselbe gefuellte Flaeche.
            areas=[np.count_nonzero(f[...,3]) for f in walk]
            self.assertGreater(max(areas)-min(areas),40,name)
            self.assertEqual(8,len({f.tobytes() for f in walk}),name)
            for frame in walk:
                self.assertEqual(125,int(np.nonzero(frame[...,3])[0].max()),name)

    def test_front_and_back_cycles_keep_their_four_drawn_phases(self):
        for name,f in self.sheets.items():
            for start in (68,76):
                self.assertEqual(4,len({frame.tobytes() for frame in f[start:start+8]}),name)
            self.assertFalse(np.array_equal(f[27],f[32]),name)

    def test_idle_breathes_but_support_stays_fixed(self):
        for name,f in self.sheets.items():
            self.assertGreaterEqual(len({frame.tobytes() for frame in f[:8]}),4,name)
            for frame in f[:8]:
                self.assertTrue(np.array_equal(f[0,120:],frame[120:]),name)

    def test_front_blink_changes_only_verified_eyes(self):
        for name,f in self.sheets.items():
            changed=np.any(f[27]!=f[55],axis=-1)
            allowed=np.zeros((128,128),bool)
            for x0,y0,x1,y1 in E.EYE_WINDOWS[name]:allowed[y0:y1,x0:x1]=True
            self.assertTrue(changed.any(),name)
            self.assertFalse((changed & ~allowed).any(),name)

    def test_fast_cycle_is_own_body_drawing_instead_of_faster_walk(self):
        for name,f in self.sheets.items():
            self.assertGreaterEqual(len({frame.tobytes() for frame in f[114:122]}),3,name)
            self.assertFalse(any(np.array_equal(f[114],w) for w in f[9:17]),name)

    def test_roll_rotates_a_compact_curled_body_and_keeps_contact(self):
        for name,f in self.sheets.items():
            rolls=f[130:138]
            self.assertEqual(8,len({frame.tobytes() for frame in rolls}),name)
            for frame in rolls:
                ys,xs=np.nonzero(frame[...,3])
                self.assertEqual(125,int(ys.max()),name)
                self.assertLessEqual(xs.max()-xs.min(),111,name)

    def test_unaffected_fennec_roles_remain_pixel_identical(self):
        sheet=np.asarray(Image.open(E.ASSETS/'fennec.png').convert('RGBA')).reshape(128,138,128,4).transpose(1,0,2,3)
        unchanged=[i for i in range(138) if i not in (22,55,92,93,94,95)]
        self.assertEqual('05ae41247042a5552f56a4276f30dd6d31fabf6fb56de5897327268dbb9dbdaf',
                         hashlib.sha256(sheet[unchanged].tobytes()).hexdigest())



if __name__=='__main__':unittest.main()
