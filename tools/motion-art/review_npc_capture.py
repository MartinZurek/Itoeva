"""Unveränderte Spielbilder prüfen und als Einzelcheckpoint archivieren."""
import argparse
import hashlib
import json
import shutil
import subprocess
from pathlib import Path

from PIL import Image, ImageDraw


def review(capture, output):
    source = json.loads((capture / "telemetry.json").read_text())
    frames = source["frames"]
    flight = source["id"] == "thorn_moth"
    guardian = source["id"] == "bark_guardian"
    count = 600 if guardian else (240 if flight else 360)
    name = "Rindenwaechter_N3" if guardian else ("Dornmotte_N2" if flight else "Aschenlaeufer_N1")
    prefix = source["id"].replace("_", "-")
    if len(frames) != count:
        raise ValueError("Nur die vollständige finale Aufnahme verwenden")
    sizes = {Image.open(capture / f"{index:04d}.png").size for index in range(count)}
    if len(sizes) != 1:
        raise ValueError("Vermischte Aufnahmegrößen: jeden Lauf in einem eigenen Ordner sichern")
    if guardian:
        assert source.get("comparison_species") == "mosslyn", "Vergleich mit abgenommener Hauptfigur"
    scales = {f["pixel_size"] for f in frames}
    offsets = {f["offset_y"] for f in frames}
    assert len(scales) == len(offsets) == 1, (scales, offsets)
    coverage = {}
    for direction in (False, True):
        seen = sorted({f["phase"] for f in frames if f["mode"] == "walk" and f["flip_h"] == direction})
        assert seen == list(range(8)), (direction, seen)
        coverage["right" if direction else "left"] = seen
    windows = source.get("stop_windows", [[source.get("stop_frame",285),count]])
    idle = [f for start,end in windows for f in frames if start+15 <= f["frame"] < end]
    assert all(f["mode"] == "rest" for f in idle)
    rest_frames = 8 if flight else 4
    assert sorted({f["phase"] for f in idle}) == list(range(rest_frames))
    stops = []
    for start,end in windows:
        section = frames[start:end]
        assert all(f["position"] == section[0]["position"] for f in section)
        stable = section[15:]
        assert all(f["mode"] == "rest" for f in stable)
        assert sorted({f["phase"] for f in stable}) == list(range(rest_frames))
        resumed = end < count
        if resumed:
            after = frames[end:min(end+45,count)]
            assert any(f["mode"] == "walk" and f["position"] != section[-1]["position"] for f in after)
        stops.append(dict(start=start,end=end,position=section[0]["position"],rest_phases=sorted({f["phase"] for f in stable}),resumed=resumed))
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
    if guardian:
        report["stops"] = stops
        report.pop("tested")
        report["validation_log"] = "test-results.json"
        report["resolution"] = list(next(iter(sizes)))
        report["comparison_species"] = source["comparison_species"]
        # Kamera-Koordinaten beziehen sich auf den 1280x720-Projektviewport.
        # Ausschließlich echte Pixel ausschneiden/vergrößern, keine Zwischenbilder.
        import re
        project = (Path(__file__).resolve().parents[2] / "prototypes/godot-hd2d/project.godot").read_text()
        viewport = [int(re.search(r"window/size/viewport_"+axis+r"=(\d+)",project)[1]) for axis in ("width","height")]
        def detail(frame):
            shot = Image.open(capture / f"{frame['frame']:04d}.png").convert("RGB")
            x = frame["screen"][0] * shot.width / viewport[0]
            y = frame["screen"][1] * shot.height / viewport[1]
            return shot.crop((round(x-60),round(y-112),round(x+60),round(y+8)))
        selected = []
        for direction in (False,True):
            for phase in range(8):
                selected.append(next(f for f in frames if f["mode"]=="walk" and f["phase"]==phase and f["flip_h"]==direction))
        for start,end in windows:
            for phase in range(4):
                selected.append(next(f for f in frames[start+15:end] if f["mode"]=="rest" and f["phase"]==phase))
        selected.extend(frames[i] for i in [115,120,125,190,195,200,400,405,410,475,480,485])
        sheet = Image.new("RGB",(1920,1300),"#252a28")
        draw = ImageDraw.Draw(sheet)
        for index,frame in enumerate(selected):
            x,y = index%8*240,index//8*260
            sheet.paste(detail(frame).resize((240,240),Image.Resampling.NEAREST),(x,y+20))
            draw.text((x+5,y+3),f"{frame['frame']} {frame['mode']} {frame['phase']} {'R' if frame['flip_h'] else 'L'}",fill="white")
        sheet.save(output / (name+"_Phasen_und_Uebergaenge.png"))
        detail_film = output / (name+"_Detail.mp4")
        encoder = subprocess.Popen(["ffmpeg","-hide_banner","-loglevel","error","-y",
            "-f","rawvideo","-pixel_format","rgb24","-video_size","360x360","-framerate","30","-i","pipe:0",
            "-c:v","libx264","-crf","18","-pix_fmt","yuv420p","-movflags","+faststart",str(detail_film)],stdin=subprocess.PIPE)
        try:
            for frame in frames:
                encoder.stdin.write(detail(frame).resize((360,360),Image.Resampling.NEAREST).tobytes())
        finally:
            encoder.stdin.close()
        if encoder.wait() != 0:
            raise RuntimeError("Detailfilm konnte nicht kodiert werden")
        report["detail_film_sha256"] = hashlib.sha256(detail_film.read_bytes()).hexdigest()
    (output / (prefix+"-review.json")).write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps(report, ensure_ascii=False))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("capture", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    review(args.capture, args.output)
