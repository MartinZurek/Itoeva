"""Loop polish and release gate for generated Itoeva tracks.

Why this file exists
--------------------
Three defects showed up in the two day tracks that shipped before it, and all three were
introduced by the pipeline rather than by the model:

1. **A hole at the loop point.** ``main-day-01`` ends with 0.80 s of silence, ``main-day-02``
   with 1.37 s, and both start at full level. The app loops these files
   (``MediaPlayer.isLooping = true``), so every 90 seconds the music drops out for about a
   second and then re-enters hard. That is audible on every longer visit.

2. **True peak above 0 dBFS.** The generator clamps to +-1.0 and hands that straight to a
   *lossy* encoder. Vorbis reconstructs above the clamped value, so a signal that sat exactly
   at full scale decodes to +0.41 dBFS (``main-day-02``) and +0.68 dBFS (``main-day-01``).
   Clamping at full scale is the cause; headroom before encoding is the fix.

3. **No gate.** Nothing measured any of this. The defects reached the phone and were only
   found by measuring the shipped files by hand.

A fourth defect showed up on 2026-09-23, this time in the gate's own reaction to a real
overshoot rather than in its absence. Four tracks generated at ``cfg_scale=5`` (raised in
PR #191 for diffusion quality) all sat at the same -1 dBFS pre-encode peak, but decoded to
wildly different true peaks: three of them 5-6 dB above 0 dBFS, one barely above it - and the
gate simply failed the job each time, discarding the file the failure was about. A synthetic
check narrowed it down: smooth, broadband material overshoots by a fraction of a dB on decode,
exactly as the two original shipped tracks did, but a sharp-edged, near-square transient
overshoots by several dB (measured +3.65 dB in a synthetic square-wave burst; see
``test_a_sharp_transient_overshoots_more_than_smooth_material``). A single fixed headroom
value large enough for the worst edge case would make every ordinary track quieter than it
needs to be, and still risk failing the next outlier. ``encode_within_gate`` instead measures
what the encoder actually did to *this* take and, if it still clips, removes exactly that much
gain and re-encodes - bounded so a take with a real, non-level defect still fails fast.

Everything here is pure numpy on float arrays: no torch, no model, no network. That is
deliberate - it makes the rules testable in a second instead of after a five-minute GPU run.

What this file does NOT do
--------------------------
It does not touch already generated files. Generated binaries change only through the process
that made them, so fixing the two shipped tracks means regenerating them with this step in
place - not editing them afterwards.
"""

from __future__ import annotations

from typing import Callable

import numpy as np

# Headroom left below full scale before the lossy encoder sees the signal. -1.0 dBFS is the
# usual allowance for encoder overshoot; measured overshoot on the two shipped tracks was
# 0.41 and 0.68 dB, so one decibel covers it with room to spare.
TARGET_PEAK_DBFS = -1.0

# Anything below this counts as silence when trimming the edges. Chosen well under the quietest
# musical passage measured so far (-65.8 dBFS in a 0.5 s window) so that a genuinely quiet
# ending is trimmed, but a soft one is not.
SILENCE_DBFS = -60.0

# Length of the crossfade that turns the end of the file into the beginning. Long enough to
# hide a transient, short enough not to smear a downbeat.
SEAM_MS = 120.0

# What the gate refuses to let through.
MAX_PEAK_DBFS = 0.0          # a lossy file decoding above full scale clips on playback
MAX_EDGE_SILENCE_S = 0.15    # silence at either edge becomes a hole at the loop point
MAX_SEAM_JUMP = 0.25         # amplitude step across the loop point, linear (-12 dBFS)

# How many times encode_within_gate measures the decoded peak and re-encodes at a lower level
# before giving up. Every attempt after the first costs one real encode+decode round trip.
MAX_PEAK_CORRECTION_ATTEMPTS = 4

# Extra reduction below what the last measurement strictly required, so a second measurement
# that comes back a hair over MAX_PEAK_DBFS (encoder rounding, not a new defect) still passes
# instead of flapping between attempts.
PEAK_SAFETY_MARGIN_DB = 0.3


def _dbfs(value: float) -> float:
    return 20.0 * np.log10(max(float(value), 1e-12))


def edge_silence_seconds(frames: np.ndarray, sample_rate: int) -> tuple[float, float]:
    """Silence at the very start and the very end, in seconds."""
    level = np.abs(frames).max(axis=1)
    audible = np.flatnonzero(level > 10 ** (SILENCE_DBFS / 20.0))
    if audible.size == 0:
        total = frames.shape[0] / sample_rate
        return total, total
    return audible[0] / sample_rate, (frames.shape[0] - 1 - audible[-1]) / sample_rate


def seam_jump(frames: np.ndarray) -> float:
    """Largest per-channel amplitude step from the last sample back to the first.

    This is what a listener hears at the loop point: the player jumps from the end of the file
    straight to its beginning, and any step between the two is a click or a thud.
    """
    if frames.shape[0] < 2:
        return 0.0
    return float(np.abs(frames[0] - frames[-1]).max())


def polish_for_loop(frames: np.ndarray, sample_rate: int) -> np.ndarray:
    """Trim the edges, close the seam and leave encoder headroom.

    Order matters: trim first (otherwise the crossfade would blend silence into the start),
    then close the seam, then set the level (so the gain applies to the finished waveform).
    """
    if frames.ndim != 2:
        raise ValueError("expected frames as (samples, channels)")
    work = frames.astype(np.float64, copy=True)

    # 1. Trim. A generated take often fades out into nothing; that tail is exactly the hole.
    level = np.abs(work).max(axis=1)
    audible = np.flatnonzero(level > 10 ** (SILENCE_DBFS / 20.0))
    if audible.size:
        work = work[audible[0] : audible[-1] + 1]

    # 2. Close the seam by folding the tail over the head. The end of the file then IS the
    #    approach to its own beginning, which is what makes a loop seamless rather than merely
    #    gapless - a butt joint would still step.
    seam = int(sample_rate * SEAM_MS / 1000.0)
    if seam > 0 and work.shape[0] > 2 * seam:
        ramp = np.linspace(0.0, 1.0, seam, endpoint=False)[:, None]
        tail = work[-seam:]
        work = work[:-seam]
        work[:seam] = work[:seam] * ramp + tail * (1.0 - ramp)

    # 3. Headroom. NOT a clamp: clamping at full scale is what produced the over-0 dBFS peaks
    #    in the first place, because the lossy encoder reconstructs above its input.
    peak = float(np.abs(work).max())
    if peak > 0.0:
        work *= (10 ** (TARGET_PEAK_DBFS / 20.0)) / peak
    return work.astype(np.float32)


def check_audio(frames: np.ndarray, sample_rate: int) -> list[str]:
    """Everything wrong with this waveform, as readable sentences. Empty means releasable."""
    findings: list[str] = []
    if frames.ndim != 2 or frames.shape[0] == 0:
        return ["the file carries no audio"]

    peak = _dbfs(np.abs(frames).max())
    if peak > MAX_PEAK_DBFS:
        findings.append(
            f"true peak {peak:+.2f} dBFS is above {MAX_PEAK_DBFS:+.1f} - this clips on playback"
        )

    head, tail = edge_silence_seconds(frames, sample_rate)
    if head > MAX_EDGE_SILENCE_S:
        findings.append(f"{head:.2f} s of silence at the start becomes a hole at the loop point")
    if tail > MAX_EDGE_SILENCE_S:
        findings.append(f"{tail:.2f} s of silence at the end becomes a hole at the loop point")

    jump = seam_jump(frames)
    if jump > MAX_SEAM_JUMP:
        findings.append(
            f"amplitude step of {jump:.3f} ({_dbfs(jump):.1f} dBFS) across the loop point clicks"
        )

    rms = _dbfs(np.sqrt(np.mean(np.square(frames))))
    if rms < -30.0:
        findings.append(f"overall level {rms:.1f} dBFS is far quieter than the shipped tracks")
    return findings


def encode_within_gate(
    frames: np.ndarray,
    sample_rate: int,
    encode_and_decode: Callable[[np.ndarray], np.ndarray],
    max_attempts: int = MAX_PEAK_CORRECTION_ATTEMPTS,
    safety_margin_db: float = PEAK_SAFETY_MARGIN_DB,
) -> tuple[np.ndarray, np.ndarray, list[str], int]:
    """Encode ``frames`` through ``encode_and_decode``, correcting for measured overshoot.

    ``encode_and_decode`` writes its argument through the real lossy encoder and reads it back
    - that round trip is what actually produces the true-peak overshoot this exists for, so it
    cannot be simulated here. What CAN live in pure numpy is the reaction to it: if the only
    finding is the true-peak one, remove exactly the gain the last measurement says is needed
    and try again, rather than failing outright or guessing a single fixed headroom that would
    either waste level on ordinary takes or still not be enough for the next sharp-edged one.

    Returns ``(final_frames, last_decoded, findings, attempts_used)``. ``final_frames`` and
    ``last_decoded`` are exactly what the last call to ``encode_and_decode`` wrote and read
    back, so the caller never needs a redundant extra round trip to see what is on disk.
    ``findings`` empty means releasable; a non-empty result after ``max_attempts`` means either
    a non-level defect (fixing the gain does not help edge silence or a seam step, so those end
    the loop on their first appearance) or a take whose overshoot did not converge in time.
    """
    findings: list[str] = []
    attempt = 0
    decoded = None
    for attempt in range(1, max_attempts + 1):
        decoded = encode_and_decode(frames)
        findings = check_audio(decoded, sample_rate)
        peak_finding = next((f for f in findings if f.startswith("true peak")), None)
        if peak_finding is None or len(findings) > 1 or attempt == max_attempts:
            break
        decoded_peak_dbfs = _dbfs(np.abs(decoded).max())
        reduction_db = decoded_peak_dbfs - MAX_PEAK_DBFS + safety_margin_db
        frames = frames * (10.0 ** (-reduction_db / 20.0))
    return frames, decoded, findings, attempt
