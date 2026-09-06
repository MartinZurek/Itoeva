"""Tests for the loop polish and the release gate.

Runnable with ``python -m unittest discover -s tools/music`` - no model, no network, no GPU.
That is the point: the rules these tests cover were violated by two tracks that each cost a
five-minute generation run to produce and were only caught by measuring the finished files.
"""

from __future__ import annotations

import unittest

import numpy as np

import audio_polish as ap


def tone(seconds: float, sample_rate: int = 44100, amplitude: float = 0.5) -> np.ndarray:
    t = np.arange(int(seconds * sample_rate)) / sample_rate
    wave = amplitude * np.sin(2 * np.pi * 220.0 * t)
    return np.stack([wave, wave], axis=1)


def with_silent_tail(seconds: float, tail: float, sample_rate: int = 44100) -> np.ndarray:
    body = tone(seconds, sample_rate)
    return np.concatenate([body, np.zeros((int(tail * sample_rate), 2))], axis=0)


class GateTest(unittest.TestCase):
    """What the gate must refuse."""

    def test_clean_material_passes(self):
        self.assertEqual([], ap.check_audio(ap.polish_for_loop(tone(5.0), 44100), 44100))

    def test_peak_above_full_scale_is_refused(self):
        loud = tone(2.0, amplitude=1.06)
        findings = ap.check_audio(loud, 44100)
        self.assertTrue(any("true peak" in f for f in findings), findings)

    def test_silence_at_the_end_is_refused(self):
        findings = ap.check_audio(with_silent_tail(3.0, 1.0), 44100)
        self.assertTrue(any("end" in f for f in findings), findings)

    def test_silence_at_the_start_is_refused(self):
        padded = np.concatenate([np.zeros((44100, 2)), tone(3.0)], axis=0)
        findings = ap.check_audio(padded, 44100)
        self.assertTrue(any("start" in f for f in findings), findings)

    def test_a_step_across_the_loop_point_is_refused(self):
        # Ends high, starts at zero: exactly the click the seam crossfade removes.
        frames = tone(2.0)
        frames[-1] = 0.9
        frames[0] = -0.9
        findings = ap.check_audio(frames, 44100)
        self.assertTrue(any("loop point" in f for f in findings), findings)

    def test_an_empty_file_is_refused(self):
        self.assertTrue(ap.check_audio(np.zeros((0, 2)), 44100))


class PolishTest(unittest.TestCase):
    """What the polish must achieve - and what it must not damage."""

    def test_the_tail_of_silence_is_gone(self):
        polished = ap.polish_for_loop(with_silent_tail(4.0, 1.5), 44100)
        head, tail = ap.edge_silence_seconds(polished, 44100)
        self.assertLess(tail, ap.MAX_EDGE_SILENCE_S)
        self.assertLess(head, ap.MAX_EDGE_SILENCE_S)

    def test_headroom_is_left_for_the_encoder(self):
        """The defect that produced +0.41 and +0.68 dBFS on the shipped tracks.

        Clamping at full scale is not enough, because a lossy encoder reconstructs above its
        input. The polish therefore targets -1 dBFS instead of clamping.
        """
        polished = ap.polish_for_loop(tone(3.0, amplitude=0.999), 44100)
        peak_db = 20 * np.log10(np.abs(polished).max())
        self.assertAlmostEqual(peak_db, ap.TARGET_PEAK_DBFS, places=1)
        self.assertLess(peak_db, 0.0)

    def test_quiet_material_is_lifted_to_the_same_level(self):
        """Two tracks of one role alternate, so a level difference is audible at the crossfade."""
        quiet = ap.polish_for_loop(tone(3.0, amplitude=0.02), 44100)
        loud = ap.polish_for_loop(tone(3.0, amplitude=0.9), 44100)
        self.assertAlmostEqual(
            20 * np.log10(np.abs(quiet).max()), 20 * np.log10(np.abs(loud).max()), places=1
        )

    def test_the_seam_no_longer_steps(self):
        frames = tone(4.0)
        frames[-1] = 0.9
        polished = ap.polish_for_loop(frames, 44100)
        self.assertLess(ap.seam_jump(polished), ap.MAX_SEAM_JUMP)

    def test_the_piece_keeps_its_length(self):
        """Trimming and seam-folding shorten the take, but only by what they remove.

        A polish that quietly ate seconds of music would be worse than the hole it closes.
        """
        polished = ap.polish_for_loop(with_silent_tail(60.0, 1.4), 44100)
        seconds = polished.shape[0] / 44100
        self.assertGreater(seconds, 59.5)
        self.assertLess(seconds, 60.0)

    def test_silence_only_material_does_not_crash(self):
        polished = ap.polish_for_loop(np.zeros((44100, 2)), 44100)
        self.assertEqual(2, polished.ndim)


if __name__ == "__main__":
    unittest.main()
