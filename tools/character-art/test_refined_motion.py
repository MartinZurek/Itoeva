"""Ausgelieferte Gelenke, Registrierungen und unberuehrte Bildrollen pruefen."""
import re
import unittest
import numpy as np
from PIL import Image
import ensemble_motion as E
import refined_motion as R
import fennec_mobility as F


class RefinedMotionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.sheets = {n: np.asarray(Image.open(E.ASSETS/f'{n}.png').convert('RGBA'))
                      for n in (*R.NAMES, 'fennec')}
        cls.frames = {n: a.reshape(128,138,128,4).transpose(1,0,2,3)
                      for n,a in cls.sheets.items()}

    def test_sitting_lowers_each_view_without_moving_the_floor(self):
        for name in R.NAMES:
            f = self.frames[name]
            for sequence in ((95,92,93),(104,100,103),(111,107,110)):
                tops=[]
                for i in sequence:
                    y=np.nonzero(f[i,...,3])[0]
                    self.assertEqual(125,int(y.max()),(name,i))
                    tops.append(int(y.min()))
                self.assertLess(tops[0],tops[1],(name,sequence))
                self.assertLessEqual(tops[1],tops[2],(name,sequence))

    def test_side_sit_keeps_the_support_axis(self):
        for name in R.NAMES:
            centers=[]
            for i in (95,92,93,94):
                y,x=np.nonzero(self.frames[name][i,...,3]);x=x[y>=122]
                centers.append((int(x.min())+int(x.max()))/2)
            self.assertLessEqual(max(centers)-min(centers),1,name)

    def test_directed_sit_keeps_the_upper_body_axis(self):
        for name in R.NAMES:
            for seq in ((104,100,103),(111,107,110)):
                centers=[]
                for i in seq:
                    y,x=np.nonzero(self.frames[name][i,...,3])
                    x=x[y<y.min()+int((y.max()-y.min()+1)*.30)]
                    centers.append((int(x.min())+int(x.max()))/2)
                self.assertLessEqual(max(centers)-min(centers),1,(name,seq))

    def test_side_blink_changes_only_lids(self):
        for name in R.NAMES:
            f=self.frames[name];changed=np.any(f[8]!=f[95],axis=-1)
            allowed=np.zeros((128,128),bool)
            for x0,y0,x1,y1 in R.SIDE_EYES[name]:allowed[y0:y1,x0:x1]=True
            self.assertTrue(changed.any(),name)
            self.assertFalse((changed & ~allowed).any(),name)

    def test_world_height_and_runtime_scales(self):
        old=dict(gloop=80.96,puffling=83.42,starlet=80.10,hootlet=76.65)
        kotlin=(E.HERE/'../../app-sim/src/main/java/com/notime/glyphsim/matrix/CreatureSprites.kt').read_text()
        for name in R.NAMES:
            scale=float(re.search(r'AvatarSpecies.'+name.upper()+r' -> ([0-9.]+)f',kotlin).group(1))
            self.assertEqual(scale,E.WORLD_SCALE[name])
            y=np.nonzero(self.frames[name][0,...,3])[0]
            self.assertAlmostEqual(old[name],(y.max()-y.min()+1)*scale,delta=2)

    def test_sources_regenerate_shipped_pixels(self):
        for name in R.NAMES:
            regenerated=np.uint8(np.clip(np.concatenate(R.frames(name),axis=1),0,1)*255)
            self.assertTrue(np.array_equal(self.sheets[name],regenerated),name)

    def test_fennec_front_blink_changes_only_lids(self):
        f=self.frames['fennec'];changed=np.any(f[27]!=f[55],axis=-1)
        allowed=np.zeros((128,128),bool)
        for x0,y0,x1,y1 in F.FRONT_EYES:allowed[y0:y1,x0:x1]=True
        self.assertTrue(changed.any())
        self.assertFalse((changed & ~allowed).any())

    def test_fennec_sit_support_and_action_stand_height(self):
        f=self.frames['fennec'];contacts=[]
        for i in (0,95,92,93,94):
            y,x=np.nonzero(f[i,...,3]);contacts.append(int(x[y>=122].max()))
            self.assertEqual(125,int(y.max()))
        self.assertEqual(1,len(set(contacts)))
        self.assertAlmostEqual(np.nonzero(f[0,...,3])[0].min(),np.nonzero(f[95,...,3])[0].min(),delta=2)

    def test_fennec_rejects_clipped_inactive_source_unless_explicitly_selected(self):
        with self.assertRaisesRegex(ValueError,'angeschnitten'):
            F.drawings('fennec-posture-refined-atlas.png',4,2)
        self.assertEqual(4,len(F.drawings('fennec-posture-refined-atlas.png',4,2,active=range(4))))


if __name__=='__main__':unittest.main()
