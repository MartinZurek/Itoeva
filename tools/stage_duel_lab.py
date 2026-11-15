"""Assemble the standalone Android export in a new directory."""

import argparse
import json
from pathlib import Path
import re
import shutil


ROOT = Path(__file__).resolve().parents[1]
URI = re.compile(r'res://([a-zA-Z0-9_./-]+\.[a-zA-Z0-9]+)')
ASSET_DATA = (
    "card_collection", "strategy_cards", "new_combat_atlases", "spirit_animations",
    "helper_animations_v060", "wildlife_atlas", "regional_enemies",
)
CARD_BACKDROPS = {"ring-weir.webp", "mill-run.webp", "mill-brook.png", "relay-court.png"}
PRESET = """[preset.0]
name="Kartenschmiede"
platform="Android"
runnable=true
export_filter="all_resources"
include_filter="data/*.json"
exclude_filter="tests/*"

[preset.0.options]
gradle_build/use_gradle_build=false
architectures/armeabi-v7a=false
architectures/arm64-v8a=true
architectures/x86=false
architectures/x86_64=false
version/code=1
version/name="1.0.0"
package/unique_name="com.itoeva.kartenschmiede.lab"
package/name="Itoeva Kartenschmiede"
package/signed=false
screen/immersive_mode=true
screen/orientation=0
"""


def references(path):
    return set(URI.findall((ROOT / path).read_text()))


def stage(destination):
    destination.mkdir(parents=True)
    files = {"scenes/duel_lab.tscn", "default_bus_layout.tres", "preview_icon.png"}
    pending = ["scripts/duel_lab_app.gd"]
    while pending:
        path = pending.pop()
        if path in files:
            continue
        files.add(path)
        for ref in references(path):
            if ref.startswith("scripts/"):
                pending.append(ref)
            elif ref.startswith(("assets/", "shaders/")):
                files.add(ref)

    for name in ASSET_DATA:
        files.update(asset for asset in references(f"data/{name}.json") if asset.startswith("assets/"))
    for pattern in ("data/*.json", "assets/audio/*.wav"):
        files.update(str(path.relative_to(ROOT)) for path in ROOT.glob(pattern))
    playlists = json.loads((ROOT / "data/music_playlists.json").read_text())
    roles = {key: playlists["roles"][key] for key in ("battle", "victory")}
    roles["day"] = roles["battle"]
    tracks = {id: playlists["tracks"][id] for ids in roles.values() for id in ids}
    files.update(track["asset"].removeprefix("res://") for track in tracks.values())

    for path in sorted(files):
        source = ROOT / path
        target = destination / path
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, target)
        import_settings = source.with_name(source.name + ".import")
        if import_settings.exists():
            settings = import_settings.read_text()
            if source.suffix in (".png", ".webp") and source.stat().st_size >= 256_000:
                settings = re.sub(r"^compress/mode=\d+", "compress/mode=1", settings, flags=re.M)
                settings = re.sub(r"^compress/lossy_quality=[\d.]+", "compress/lossy_quality=0.9", settings, flags=re.M)
                if source.name in CARD_BACKDROPS or path.startswith(("assets/cards/", "assets/card-collection/")):
                    settings = re.sub(r"^process/size_limit=\d+", "process/size_limit=640", settings, flags=re.M)
            target.with_name(target.name + ".import").write_text(settings)

    (destination / "data/music_playlists.json").write_text(json.dumps({"roles": roles, "tracks": tracks}) + "\n")
    project = (ROOT / "project.godot").read_text()
    project = project.replace('run/main_scene="res://scenes/full_world.tscn"', 'run/main_scene="res://scenes/duel_lab.tscn"')
    project = re.sub(r'config/version="[^"]+"', 'config/version="1.0.0"', project)
    project = re.sub(r'config/build_number=\d+', "config/build_number=1", project)
    project = project.replace("pointing/emulate_mouse_from_touch=false", "pointing/emulate_mouse_from_touch=true")
    (destination / "project.godot").write_text(project)
    (destination / "export_presets.cfg").write_text(PRESET)
    print(f"Staged {len(files)} files in {destination}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("destination", type=Path, help="A new directory for the export project")
    stage(parser.parse_args().destination)
