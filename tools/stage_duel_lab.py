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
version/code=2
version/name="1.0.1"
package/unique_name="com.itoeva.kartenschmiede.lab"
package/name="Itoeva Kartenschmiede"
package/signed=false
screen/immersive_mode=true
screen/orientation=0
"""


def references(path):
    return set(URI.findall((ROOT / path).read_text()))


def stage(destination, summoning=False):
    destination.mkdir(parents=True)
    app = "summoning_lab" if summoning else "duel_lab"
    files = {f"scenes/{app}.tscn", "default_bus_layout.tres", "preview_icon.png"}
    pending = [f"scripts/{app}_app.gd"]
    while pending:
        path = pending.pop()
        if path in files:
            continue
        files.add(path)
        for ref in references(path):
            if ref.startswith(("scripts/", "shaders/")):
                pending.append(ref)
            elif ref.startswith("assets/"):
                files.add(ref)

    for name in ASSET_DATA:
        if summoning and name == "regional_enemies":
            continue
        files.update(asset for asset in references(f"data/{name}.json") if asset.startswith("assets/"))
    if not summoning:
        # Regional battle paintings and pixel actors are selected from data at runtime.
        catalog = json.loads((ROOT / "data/world_catalog.json").read_text())
        rooms = catalog["interiors"] | json.loads((ROOT / "data/world_expansion.json").read_text())["interiors"]
        scenery = {key: room["asset"] for key, room in rooms.items()}
        for region in catalog["regions"]:
            scenery.update(dict.fromkeys(region["places"], region["asset"]))
        enemies = json.loads((ROOT / "data/regional_enemies.json").read_text())
        files.update(scenery[enemy["place"]].removeprefix("res://") for enemy in enemies.values())
        for name in ("mosslyn_m4_cycles", "starlet_authored_cycles", "nocti_authored_cycles", "wyrmling_authored_cycles"):
            cycles = json.loads((ROOT / f"data/{name}.json").read_text())
            files.update("assets/characters/pixel/" + Path(cycle["asset"]).name for cycle in cycles.values())
    for pattern in ("data/*.json", "assets/audio/*.wav"):
        files.update(str(path.relative_to(ROOT)) for path in ROOT.glob(pattern))
    playlists = json.loads((ROOT / "data/music_playlists.json").read_text())
    roles = {key: playlists["roles"][key] for key in ("battle", "victory")}
    if summoning:
        roles["battle"] = roles["battle"][:1]
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
                quality = 0.83 if summoning else 0.90
                settings = re.sub(r"^compress/lossy_quality=[\d.]+", f"compress/lossy_quality={quality}", settings, flags=re.M)
                if source.name in CARD_BACKDROPS or path.startswith(("assets/cards/", "assets/card-collection/")):
                    settings = re.sub(r"^process/size_limit=\d+", "process/size_limit=640", settings, flags=re.M)
            target.with_name(target.name + ".import").write_text(settings)

    (destination / "data/music_playlists.json").write_text(json.dumps({"roles": roles, "tracks": tracks}) + "\n")
    project = (ROOT / "project.godot").read_text()
    project = project.replace('run/main_scene="res://scenes/full_world.tscn"', f'run/main_scene="res://scenes/{app}.tscn"')
    project = re.sub(r'config/version="[^"]+"', 'config/version="1.0.0"' if summoning else 'config/version="1.0.1"', project)
    project = re.sub(r'config/build_number=\d+', "config/build_number=1" if summoning else "config/build_number=2", project)
    project = project.replace("pointing/emulate_mouse_from_touch=false", "pointing/emulate_mouse_from_touch=true")
    preset = PRESET
    if summoning:
        project = project.replace('config/name="Itoeva Kartenschmiede"', 'config/name="Itoeva Beschwörungskino"')
        preset = preset.replace("com.itoeva.kartenschmiede.lab", "com.itoeva.beschwoerungskino")
        preset = preset.replace('package/name="Itoeva Kartenschmiede"', 'package/name="Itoeva Beschwörungskino"')
        preset = preset.replace("version/code=2", "version/code=1").replace('version/name="1.0.1"', 'version/name="1.0.0"')
    (destination / "project.godot").write_text(project)
    (destination / "export_presets.cfg").write_text(preset)
    print(f"Staged {len(files)} files in {destination}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("destination", type=Path, help="A new directory for the export project")
    parser.add_argument("--summoning", action="store_true", help="Stage the standalone summoning preview")
    args = parser.parse_args()
    stage(args.destination, args.summoning)
