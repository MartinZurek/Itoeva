"""Prepare the Android Gameplay preview with its own save data and package."""

import argparse
from pathlib import Path
import re
import shutil

ROOT = Path(__file__).resolve().parents[1]


def stage(destination):
    destination.mkdir(parents=True)
    for name in ("assets", "data", "scripts", "scenes", "shaders"):
        shutil.copytree(ROOT / name, destination / name)
    for name in ("default_bus_layout.tres", "preview_icon.png", "preview_icon.png.import"):
        shutil.copy2(ROOT / name, destination / name)
    for path in (destination / "assets").rglob("*.import"):
        source = path.with_suffix("")
        if source.suffix in (".png", ".webp") and source.stat().st_size > 256_000 and "/characters/" not in str(source):
            settings = path.read_text()
            settings = re.sub(r"^compress/mode=\d+", "compress/mode=1", settings, flags=re.M)
            settings = re.sub(r"^compress/lossy_quality=[\d.]+", "compress/lossy_quality=0.88", settings, flags=re.M)
            path.write_text(settings)
    project = (ROOT / "project.godot").read_text()
    project = project.replace('config/name="Itoeva Kartenschmiede"', 'config/name="Itoeva Gameplay"')
    project = project.replace('run/main_scene="res://scenes/full_world.tscn"', 'run/main_scene="res://scenes/gameplay.tscn"')
    project = re.sub(r'config/version="[^"]+"', 'config/version="0.19.14"', project)
    project = re.sub(r"config/build_number=\d+", "config/build_number=76", project)
    (destination / "project.godot").write_text(project)
    preset = (ROOT / "export_presets.cfg").read_text()
    preset = preset.replace('name="Kartenschmiede"', 'name="Gameplay"')
    preset = preset.replace('package/name="Itoeva Kartenschmiede"', 'package/name="Itoeva Gameplay"')
    preset = re.sub(r'package/unique_name="[^"]+"', 'package/unique_name="com.itoeva.gameplay.preview"', preset)
    preset = re.sub(r'version/name="[^"]+"', 'version/name="0.19.14"', preset)
    preset = re.sub(r"version/code=\d+", "version/code=76", preset)
    preset = preset.replace("package/signed=true", "package/signed=false")
    (destination / "export_presets.cfg").write_text(preset)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("destination", type=Path)
    args = parser.parse_args()
    stage(args.destination)
