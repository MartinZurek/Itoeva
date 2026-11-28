"""Find quiet exit points in the existing Ogg recordings for exploration music."""

import json
from pathlib import Path
import subprocess

import numpy as np

ROOT = Path(__file__).resolve().parents[1]
RATE = 16000
WINDOW = 1024
HOP = 512
STEP = 2.6


def exit_points(asset):
    samples = np.frombuffer(
        subprocess.check_output(
            [
                "ffmpeg", "-v", "error", "-i", str(asset), "-f", "f32le",
                "-ac", "1", "-ar", str(RATE), "-",
            ]
        ),
        dtype="<f4",
    )
    padded = np.pad(samples, WINDOW // 2)
    frames = np.lib.stride_tricks.sliding_window_view(padded, WINDOW)[::HOP]
    spectrum = np.abs(np.fft.rfft(frames * np.hanning(WINDOW), axis=1))
    rms = np.sqrt(np.mean(frames ** 2, axis=1))
    flux = np.maximum(0, np.diff(np.log1p(spectrum), axis=0, prepend=0)).mean(axis=1)
    weights = np.ones(9) / 9
    energy = np.convolve(rms, weights, mode="same")
    attacks = np.convolve(flux, weights, mode="same")
    score = 0.55 * energy / max(0.000001, np.percentile(energy, 75))
    score += 0.45 * attacks / max(0.000001, np.percentile(attacks, 75))
    seconds = HOP / RATE
    end = len(samples) / RATE
    picks = []
    for center in np.arange(1.45, end - 2.8, STEP):
        left = int((center - 0.51) / seconds)
        right = int((center + 0.51) / seconds)
        best = left + np.argmin(score[left:right + 1])
        picks.append(round(best * seconds, 3))
    return picks


def main():
    playlists = json.loads((ROOT / "data/music_playlists.json").read_text())
    world_tracks = {
        track for role, ids in playlists["roles"].items()
        if role not in {"battle", "victory"} for track in ids
    }
    result = {}
    for track in sorted(world_tracks):
        asset = playlists["tracks"][track]["asset"].removeprefix("res://")
        result[track] = exit_points(ROOT / asset)
    lines = [f"  {json.dumps(track)}: {json.dumps(points)}" for track, points in result.items()]
    (ROOT / "data/music_exit_points.json").write_text("{\n" + ",\n".join(lines) + "\n}\n")


if __name__ == "__main__":
    main()
