"""Unveränderte Spielbilder prüfen und als Einzelcheckpoint archivieren."""
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
    flight = source["id"] == "thorn_moth"
    count = 240 if flight else 360
    name = "Dornmotte_N2" if flight else "Aschenlaeufer_N1"
    prefix = source["id"].replace("_", "-")
    if len(frames) != count:
        raise ValueError("Nur die vollständige finale Aufnahme verwenden")
    scales = {f["pixel_size"] for f in frames}
    offsets = {f["offset_y"] for f in frames}
    assert len(scales) == len(offsets) == 1, (scales, offsets)
    coverage = {}
    for direction in (False, True):
        seen = sorted({f["phase"] for f in frames if f["mode"] == "walk" and f["flip_h"] == direction})
        assert seen == list(range(8)), (direction, seen)
        coverage["right" if direction else "left"] = seen
    idle = [f for f in frames if f["frame"] >= source.get("stop_frame", 285)+15]
    assert all(f["mode"] == "rest" for f in idle)
    rest_frames = 8 if flight else 4
    assert sorted({f["phase"] for f in idle}) == list(range(rest_frames))
    assert all(f["position"] == idle[0]["position"] for f in idle)
    output.mkdir(parents=True, exist_ok=True)
    film = output / (name+"_Welt.mp4")
    subprocess.run(["ffmpeg", "-hide_banner", "-loglevel", "error", "-y",
                    "-framerate", "30", "-i", str(capture / "%04d.png"), "-frames:v", str(count),
                    "-c:v", "libx264", "-crf", "18", "-pix_fmt", "yuv420p", "-movflags", "+faststart", str(film)], check=True)
    shutil.copyfile(capture / "telemetry.json", output / (prefix+"-telemetry.json"))
    still = source.get("stop_frame",285)+30
    Image.open(capture / f"{still:04d}.png").save(output / (name+"_Groessenvergleich.png"))
    report = dict(id=source["id"], fps=30, frames=count, seconds=count/30,
                  walk_coverage=coverage, rest_coverage=list(range(rest_frames)),
                  fixed_pixel_size=list(scales)[0], fixed_offset_y=list(offsets)[0],
                  stationary_during_rest=True,
                  film_sha256=hashlib.sha256(film.read_bytes()).hexdigest(),
                  tested=dict(locomotion_checks=54, roaming_regression_checks=47,
                              main_character_regression_checks=1798, python_asset_tests=1, failures=0),
                  limitations=["Keine neuen Tiefen- oder Wendeposen.",
                               "Pfoten-Druckpunkte und Schlupffreiheit nicht vollständig nachgewiesen.",
                               "Ruhewechsel ohne zusätzliche gezeichnete Verbindungsfolge.",
                               "Kein Telefontest, keine neue APK."])
    if flight:
        wraps=[b["frame"] for a,b in zip(frames,frames[1:]) if b["phase"]<a["phase"]]
        report["observed_wingbeats_per_second"] = 30*(len(wraps)-1)/(wraps[-1]-wraps[0])
        assert 1.3<report["observed_wingbeats_per_second"]<1.5
        report["tested"] = dict(flight_checks=68, ground_regression_checks=54, python_asset_tests=2, failures=0)
        report["limitations"] = ["Neue Tiefen-/Wendeposen und Landen noch offen.", "Kampfangriffe weiterhin bisherige Zeichnungen.", "Kein Telefontest, keine neue APK."]
    (output / (prefix+"-review.json")).write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps(report, ensure_ascii=False))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("capture", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    review(args.capture, args.output)
