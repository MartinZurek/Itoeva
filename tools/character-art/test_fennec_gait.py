"""Bodenkontakt ist eine Bewegungszusage, kein Vergleich zweier Screenshot-Dateien."""
import unittest
import numpy as np
import fennec_key as F


class FennecGaitTest(unittest.TestCase):
    def test_stance_is_level_and_swing_clears_floor(self):
        for phase in np.linspace(0, .49, 30):
            point, angle = F.foot_path(phase)
            self.assertEqual(365., point[1])
            self.assertEqual(0., angle)
        self.assertLess(F.foot_path(.75)[0][1], 345.)
        np.testing.assert_allclose(F.foot_path(0)[0], F.foot_path(1)[0])

    def test_two_joint_leg_reaches_ground_target_in_every_frame(self):
        puppet = F.build()
        for par in F.walk(n=32):
            world = puppet.world(F.mats(**par))
            for side, ankle, phase in [('l', F.ANKLE_L, par['gait']), ('r', F.ANKLE_R, par['gait']+.5)]:
                actual = world['boot_'+side] @ np.array([*ankle, 1.])
                expected, _ = F.foot_path(phase)
                expected = (expected[0], expected[1] + F.FEET[1] - 1 - F.SOLE_Y[side])
                np.testing.assert_allclose(actual[:2], expected, atol=1e-5)
                # Die Beinsegmente werden gedreht und nicht kuenstlich zusammengestaucht.
                self.assertAlmostEqual(1., np.linalg.det(world['shin_'+side][:2,:2]), places=6)

    def test_turns_do_not_squeeze_the_source(self):
        for _, params in F.plan()[-2:]:
            self.assertEqual(1., params.get('sx', 1.))

    def test_each_phase_keeps_a_supporting_foot(self):
        for phase in np.linspace(0, 1, 64, endpoint=False):
            ys = [F.foot_path(phase)[0][1], F.foot_path(phase+.5)[0][1]]
            self.assertAlmostEqual(365., max(ys))

if __name__ == '__main__':
    unittest.main()
