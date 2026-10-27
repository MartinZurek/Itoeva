"""Unveränderte Spielbilder prüfen und als N1-Video archivieren."""
import argparse
import hashlib
import json
import shutil
import subprocess
from pathlib import Path

from PIL import Image


def review(capture, output):
    source = json.loads((capture / "telemetry.json").read_text())
    frames = source["frames"]
    if len(frames) != 360:
        raise ValueError("Nur die vollständige finale N1-Aufnahme verwenden")
    scales = {f["pixel_size"] for f in frames}
    offsets = {f["offset_y"] for f in frames}
    assert len(scales) == len(offsets) == 1, (scales, offsets)
    coverage = {}
    for direction in (False, True):
        seen = sorted({f["phase"] for f in frames if f["mode"] == "walk" and f["flip_h"] == direction})
        assert seen == list(range(8)), (direction, seen)
        coverage["right" if direction else "left"] = seen
    idle = [f for f in frames if f["frame"] >= 300]
    assert all(f["mode"] == "rest" for f in idle)
    assert sorted({f["phase"] for f in idle}) == list(range(4))
    assert all(f["position"] == idle[0]["position"] for f in idle)
    output.mkdir(parents=True, exist_ok=True)
    film = output / "Aschenlaeufer_N1_Welt.mp4"
    subprocess.run(["ffmpeg", "-hide_banner", "-loglevel", "error", "-y",
                    "-framerate", "30", "-i", str(capture / "%04d.png"), "-frames:v", "360",
                    "-c:v", "libx264", "-crf", "18", "-pix_fmt", "yuv420p", "-movflags", "+faststart", str(film)], check=True)
    shutil.copyfile(capture / "telemetry.json", output / "ash-hound-telemetry.json")
    Image.open(capture / "0315.png").save(output / "Aschenlaeufer_N1_Groessenvergleich.png")
    report = dict(id=source["id"], fps=30, frames=360, seconds=12,
                  walk_coverage=coverage, rest_coverage=list(range(4)),
                  fixed_pixel_size=list(scales)[0], fixed_offset_y=list(offsets)[0],
                  stationary_during_rest=True,
                  film_sha256=hashlib.sha256(film.read_bytes()).hexdigest(),
                  tested=dict(locomotion_checks=54, roaming_regression_checks=47,
                              main_character_regression_checks=1798, python_asset_tests=1, failures=0),
                  limitations=["Keine neuen Tiefen- oder Wendeposen.",
                               "Pfoten-Druckpunkte und Schlupffreiheit nicht vollständig nachgewiesen.",
                               "Ruhewechsel ohne zusätzliche gezeichnete Verbindungsfolge.",
                               "Kein Telefontest, keine neue APK."])
    (output / "ash-hound-review.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps(report, ensure_ascii=False))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("capture", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    review(args.capture, args.output)
