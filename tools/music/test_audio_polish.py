"""Tests for the loop polish and the release gate.

Runnable with ``python -m unittest discover -s tools/music`` - no model, no network, no GPU.
That is the point: the rules these tests cover were violated by two tracks that each cost a
five-minute generation run to produce and were only caught by measuring the finished files.
"""

from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

import numpy as np

import audio_polish as ap

try:
    import soundfile as sf

    HAS_SOUNDFILE = True
except ImportError:  # pragma: no cover - soundfile is a real dependency of this tool
    HAS_SOUNDFILE = False


def encode_decode_vorbis(frames: np.ndarray, sample_rate: int = 44100) -> np.ndarray:
    """A real lossy round trip, the same one generate_music.py uses to write a track.

    Only a real encoder reproduces true-peak overshoot - the numpy-only functions in
    audio_polish.py cannot fabricate it, so the tests that need it go through libsndfile
    for real instead of mocking the round trip away.
    """
    with tempfile.TemporaryDirectory() as tmp:
        path = Path(tmp) / "take.ogg"
        with sf.SoundFile(
            str(path), mode="w", samplerate=sample_rate, channels=frames.shape[1],
            format="OGG", subtype="VORBIS",
        ) as handle:
            for start in range(0, frames.shape[0], 8192):
                handle.write(frames[start : start + 8192].astype(np.float32))
        decoded, _ = sf.read(str(path), always_2d=True)
        return decoded


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


@unittest.skipUnless(HAS_SOUNDFILE, "soundfile is not installed")
class EncodeWithinGateTest(unittest.TestCase):
    """The release gate's reaction to a real encoder round trip, not a simulated one.

    The 2026-09-23 incident: four tracks sat at the identical -1 dBFS pre-encode peak and
    decoded to true peaks 5-6 dB apart. A synthetic square-wave burst reproduces the mechanism
    reliably (measured +3.65 dB in exploration, see EVOLUTION.md) while smooth material stays
    close to its encoded level - that difference is exactly what encode_within_gate has to
    correct for one and leave alone for the other.
    """

    def sharp_transient(self, sample_rate: int = 44100) -> np.ndarray:
        """A near-square high-frequency burst riding on a continuous bed.

        The bed keeps both edges above the silence threshold, so the only thing check_audio
        can flag is the true peak - isolating exactly the defect this test is about from the
        edge-silence and seam-jump checks, which a bare burst-in-silence would also trip.
        """
        t = np.arange(int(3.0 * sample_rate)) / sample_rate
        bed = 0.2 * np.sin(2 * np.pi * 220 * t)
        start = sample_rate // 4
        length = 300
        bed[start : start + length] = np.sign(np.sin(2 * np.pi * 8000 * t[start : start + length]))
        frames = np.stack([bed, bed], axis=1).astype(np.float64)
        peak = np.abs(frames).max()
        frames *= (10 ** (ap.TARGET_PEAK_DBFS / 20.0)) / peak
        return frames

    def test_a_sharp_transient_overshoots_more_than_smooth_material(self):
        """Proof the mechanism is real, independent of encode_within_gate's reaction to it."""
        sharp = encode_decode_vorbis(self.sharp_transient())
        smooth = encode_decode_vorbis(ap.polish_for_loop(tone(3.0, amplitude=0.999), 44100))
        sharp_overshoot = 20 * np.log10(np.abs(sharp).max()) - ap.TARGET_PEAK_DBFS
        smooth_overshoot = 20 * np.log10(np.abs(smooth).max()) - ap.TARGET_PEAK_DBFS
        self.assertGreater(sharp_overshoot, 2.0, "the reproduction itself did not overshoot")
        self.assertLess(smooth_overshoot, 1.0)

    def test_a_clipping_take_is_corrected_and_passes(self):
        frames, decoded, findings, attempts = ap.encode_within_gate(
            self.sharp_transient(), 44100, encode_decode_vorbis
        )
        self.assertEqual([], findings, findings)
        self.assertGreater(attempts, 1, "the first attempt should not already have passed")
        self.assertLessEqual(20 * np.log10(np.abs(decoded).max()), ap.MAX_PEAK_DBFS)

    def test_clean_material_passes_on_the_first_attempt(self):
        frames, decoded, findings, attempts = ap.encode_within_gate(
            ap.polish_for_loop(tone(3.0), 44100), 44100, encode_decode_vorbis
        )
        self.assertEqual([], findings, findings)
        self.assertEqual(1, attempts)

    def test_a_non_level_defect_is_not_masked_by_gain_reduction(self):
        """Reducing gain cannot close a hole at the loop point - the loop must not pretend it can."""
        frames = with_silent_tail(3.0, 1.0)
        _, _, findings, attempts = ap.encode_within_gate(frames, 44100, encode_decode_vorbis)
        self.assertTrue(findings)
        self.assertTrue(any("loop point" in f for f in findings), findings)
        self.assertEqual(1, attempts, "a non-level defect must not trigger more attempts")

    def test_gives_up_after_max_attempts_on_a_take_that_never_converges(self):
        def never_passes(_frames: np.ndarray) -> np.ndarray:
            # Always reports the same 10 dB overshoot, however much gain was already removed -
            # a stand-in for a take whose overshoot does not scale down with level.
            frames = self.sharp_transient()
            frames *= 10 ** (10.0 / 20.0)
            return frames

        _, _, findings, attempts = ap.encode_within_gate(
            self.sharp_transient(), 44100, never_passes, max_attempts=3
        )
        self.assertTrue(findings)
        self.assertEqual(3, attempts)


if __name__ == "__main__":
    unittest.main()
