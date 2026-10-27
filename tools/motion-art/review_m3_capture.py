"""Echte Aufnahme prüfen und einen unverzerrten Detailfilm daraus erzeugen."""
import argparse
import hashlib
import json
from pathlib import Path
import statistics
import subprocess

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
PROJECT = ROOT / "prototypes/godot-hd2d"


def review(capture, output):
    output.mkdir(parents=True, exist_ok=True)
    recording = json.loads((capture / "telemetry.json").read_text())
    frames, fps = recording["frames"], recording["fps"]
    cycles = json.loads((PROJECT / "data/mosslyn_authored_cycles.json").read_text())
    source_size = Image.open(capture / "0000.png").size
    if list(source_size) != recording["capture_size"]:
        raise ValueError("PNG-Maße und Aufnahmetelemetrie widersprechen sich")
    source_hashes = {}
    for mode, spec in cycles.items():
        path = PROJECT / spec["asset"].removeprefix("res://")
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        if digest != spec["sha256"]:
            raise ValueError("Aufbereitung und Originalatlas widersprechen sich")
        source_hashes[mode] = dict(asset=spec["asset"], original_sha256=digest,
            pixel_sha256=hashlib.sha256((path.parent / "pixel" / path.name).read_bytes()).hexdigest())
    coverage = {}
    for mode, spec in cycles.items():
        for left in [False, True]:
            sample = [f for f in frames if f["cycle_mode"] == mode and f["flip_h"] == left]
            seen = sorted({f["atlas_frame"] for f in sample})
            if seen != list(range(spec["frames"])):
                raise ValueError(f"Nicht alle Phasen von {mode} in beiden Richtungen aufgenommen")
            if any(f["atlas"] != spec["asset"] for f in sample):
                raise ValueError("Aufnahme verwendet einen anderen Atlas")
            coverage[mode + ("_left" if left else "_right")] = seen
    authored = [f for f in frames if f["cycle_mode"] in cycles]
    scales = sorted({f["pixel_size"] for f in authored})
    if len(scales) != 1:
        raise ValueError("Maßstab wechselt zwischen Gehen, Galopp und Verbindungsposen")
    speed_report = {}
    for mode, target in [("walk", 2.35), ("run", 3.85)]:
        sample = [f for f in frames if f["cycle_mode"] == mode and abs(f["speed"] - target) < .02]
        speed_report[mode] = dict(frames=len(sample), speed=statistics.mean(f["speed"] for f in sample),
            cycles_per_second=statistics.mean(f["speed"] / f["stride"] for f in sample))
    phase_runs = []
    for f in frames:
        key = (f["cycle_mode"], f["atlas_frame"], f["flip_h"])
        if not phase_runs or key != phase_runs[-1]["key"]:
            phase_runs.append(dict(key=key, first_frame=f["frame"], frames=0))
        phase_runs[-1]["frames"] += 1
    for run in phase_runs:
        mode, phase, left = run.pop("key")
        run.update(mode=mode, phase=phase, direction="left" if left else "right", seconds=run["frames"]/fps)
    subprocess.run(["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-framerate", str(fps),
        "-i", str(capture / "%04d.png"), "-c:v", "libx264", "-pix_fmt", "yuv420p", "-crf", "18",
        str(output / "Mooslyn_M3_Weltaufnahme.mp4")], check=True)
    font = ImageFont.truetype("DejaVuSans.ttf", 20)
    small = ImageFont.truetype("DejaVuSans.ttf", 16)
    names = {"walk": "Gehen", "lower": "Auf vier Beine absenken", "run": "Vierbeiner-Galopp",
             "rise": "Zum Gehen aufrichten", "rest": "Stillstand"}
    video = subprocess.Popen(["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-f", "rawvideo",
        "-pix_fmt", "rgb24", "-s", "720x528", "-r", str(fps), "-i", "pipe:0", "-an", "-c:v", "libx264",
        "-pix_fmt", "yuv420p", "-crf", "18", str(output / "Mooslyn_M3_Testanimation.mp4")], stdin=subprocess.PIPE)
    for f in frames:
        image = Image.open(capture / f'{f["frame"]:04d}.png').convert("RGB")
        x = f["floor_screen"][0] * image.width / recording["viewport_size"][0]
        y = f["floor_screen"][1] * image.height / recording["viewport_size"][1]
        # Ein fester Ausschnitt um den physischen Bodenanker erhält echte
        # Höhenänderungen. Keine pro Pose normalisierte Figurengröße im Film.
        crop = image.crop((round(x-120), round(y-120), round(x+120), round(y+40)))
        result = Image.new("RGB", (720, 528), "#17241d")
        result.paste(crop.resize((720, 480), Image.Resampling.NEAREST), (0, 48))
        draw = ImageDraw.Draw(result)
        draw.text((18, 12), names[f["cycle_mode"]], font=font, fill="#f0e5cb")
        draw.text((510, 15), "Spielaufnahme · 3×", font=small, fill="#a7b4a0")
        video.stdin.write(result.tobytes())
    video.stdin.close()
    if video.wait() != 0:
        raise RuntimeError("Detailfilm konnte nicht kodiert werden")
    report = dict(fps=fps, frame_count=len(frames), seconds=len(frames)/fps,
        capture_size=list(source_size), clean_view=recording["clean_view"], coverage=coverage,
        fixed_pixel_size=scales[0], speed=speed_report, sources=source_hashes,
        phase_hold_runs=phase_runs,
        detail="Dreifacher Nearest-Neighbor-Ausschnitt der echten Aufnahme, unveränderte Zeitfolge und Bildzahl.",
        limitations=["Bekannte offene M2-Gehkontakte unverändert.",
            "Galopp-Sohlenumrisse gemessen; vier einzelne Druckpunkte und Schlupffreiheit nicht nachgewiesen.",
            "Keine neuen Front-/Rückansichten oder gezeichneten Wendefolgen."])
    (output / "capture-report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps(dict(frames=len(frames), seconds=len(frames)/fps, pixel_size=scales[0], speed=speed_report), indent=2))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("capture", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    review(args.capture, args.output)
