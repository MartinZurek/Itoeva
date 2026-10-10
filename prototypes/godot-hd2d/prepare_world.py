"""Godot-Adapter: Originalbilder, Tueren, Raumanker und Moebel aus dem nativen Spiel.

Der Export ist absichtlich ohne Android/Gradle reproduzierbar. Unbekannte Quellformate
brechen ab, statt still eine zweite, abweichende Geografie zu erzeugen.
"""
import argparse
import hashlib
import json
import re
import shutil
from pathlib import Path
import xml.etree.ElementTree as ET
from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[1]
MATRIX = REPO / "app-sim/src/main/java/com/notime/glyphsim/matrix"
ART = REPO / "app-sim/src/game/assets"
CREATURES = REPO / "app-sim/src/main/assets/creatures"
SPECIES = ["fennec", "gloop", "puffling", "wyrmling", "starlet", "hootlet"]
LABELS = {
    "POND": "Waldsee", "BEACH": "Strand", "SWAMP": "Sumpf", "JUNGLE": "Dschungel",
    "COAST_PATH": "Küstenpfad", "STREET": "Straße", "PARK": "Park", "MEADOW": "Wiese",
    "FOREST": "Wald", "VILLAGE_EDGE": "Dorfrand", "CITY": "Marktplatz", "SPORT": "Sportplatz",
    "PLAINS": "Ebene", "MOUNTAINS": "Berge", "MOUNTAIN_PASS": "Bergpass", "CAMP": "Lager",
    "GROTTO": "Grotte", "LIVING": "Wohnzimmer", "BEDROOM": "Schlafzimmer", "BATH": "Bad",
    "DESK": "Schreibzimmer", "NOOK": "Leseecke", "KITCHEN": "Küche", "CRAFT": "Werkstatt",
    "SHOP": "Laden", "CAFE": "Café", "WORK": "Arbeitsstube", "ARCADE": "Spielhalle",
}
NUMBER = r"(-?(?:\d+(?:\.\d+)?|\.\d+))[fF]?"


def call_body(source, start):
    """Ausdruck mit balancierten Klammern, einschliesslich verschachtelter Konturen."""
    opening = source.index("(", start)
    level = 1
    for i in range(opening + 1, len(source)):
        if source[i] == "(":
            level += 1
        elif source[i] == ")":
            level -= 1
            if level == 0:
                return source[opening + 1:i]
    raise ValueError("Unvollstaendiger Kotlin-Ausdruck")


def native_catalog():
    world = (MATRIX / "GameWorld.kt").read_text()
    rooms = (MATRIX / "GameInteriorCatalog.kt").read_text()
    furniture = (MATRIX / "GameFurniture.kt").read_text()
    scale = (MATRIX / "GameCharacterScale.kt").read_text()
    strings = {e.attrib["name"]: e.text or "" for e in ET.parse(
        REPO / "app-sim/src/main/res/values-de/strings.xml").getroot() if e.tag == "string"}
    regions = []
    for match in re.finditer(r'Region\((ASSET|"world/[^"]+"), listOf\(([^)]+)\)', world):
        asset = "world/street-park-forest.png" if match[1] == "ASSET" else match[1].strip('"')
        places = re.findall(r"Place\.(\w+)", match[2])
        regions.append({"asset": "res://assets/" + asset, "places": places, "width": 30.0})
    assert len(regions) == 7, "Regionenformat hat sich geaendert"
    assert sum(len(r["places"]) for r in regions) == 17
    for i, region in enumerate(regions):
        region["origin"] = i * 30.0
    interiors = {}
    scene_pattern = r'GameScenes.Scene\(PlayScene.Place\.(\w+), "([^"]+)",\s*' + ",\\s*".join([NUMBER] * 8)
    for match in re.finditer(scene_pattern, rooms):
        body = call_body(rooms, match.start())
        values = list(map(float, match.groups()[2:]))
        spots = []
        spot_pattern = r"GameScenes.Spot\(PlayScene.Station\.(\w+), GameScenes.Box\(" + ",\\s*".join([NUMBER] * 4) + r"\),\s*" + NUMBER + r",\s*" + NUMBER
        for spot in re.finditer(spot_pattern, body):
            spots.append({"station": spot[1], "box": list(map(float, spot.groups()[1:5])),
                          "stand": list(map(float, spot.groups()[5:7]))})
        interiors[match[1]] = {"asset": "res://assets/" + match[2], "band": values[:2],
                              "limits": values[2:6], "height": values[6:8], "spots": spots, "pieces": []}
    assert len(interiors) == 11, "Innenraumformat hat sich geaendert"
    pieces = []
    for match in re.finditer(r'(Piece|body|table)\("([^"]+)"', furniture):
        body = call_body(furniture, match.start())
        base = re.match(r'"[^"]+",\s*' + ",\\s*".join([NUMBER] * (6 if match[1] == "Piece" else 4)), body)
        assert base, match[0]
        values = list(map(float, base.groups()))
        left, right, top, ground = values[:4]
        back, front = values[4:6] if match[1] == "Piece" else (ground - (10 if match[1] == "table" else 8), ground + 3)
        contours, seat = [], None
        if match[1] == "Piece":
            for poly in re.finditer(r"polygon\(([^)]+)\)", body):
                contours.append([[float(a), float(b)] for a, b in re.findall(NUMBER + r"\s+to\s+" + NUMBER, poly[1])])
            for leg in re.finditer(r"leg\(" + ",\\s*".join([NUMBER] * 4) + r"\)", body):
                x, y, width, bottom = map(float, leg.groups())
                contours.append([[x, y], [x + width, y], [x + width - 1, bottom], [x - 1, bottom]])
        elif match[1] == "table":
            contours.append([[left - 3, top], [right + 3, top], [right + 4, top + 6], [left - 3, top + 6]])
            for x in (left + 6, right - 10):
                contours.append([[x, top + 6], [x + 6, top + 6], [x + 5, ground], [x - 1, ground]])
        else:
            extra = body[base.end():]
            seat_match = re.match(r",\s*" + NUMBER, extra)
            if seat_match:
                seat_y = float(seat_match[1])
                seat = {"point": [(left + right) / 2, seat_y], "stations": re.findall(r"S\.(\w+)", extra)}
                top = seat_y
                contours.append([[left - 3, values[2]], [right + 3, values[2]], [right + 5, seat_y + 6],
                                 [right - 2, ground], [left + 2, ground], [left - 5, seat_y + 6]])
            else:
                contours.append([[left, top], [right, top], [right, ground], [left, ground]])
        place = "PARK" if match[2].startswith("world-PARK") else match[2].split("-")[0].upper()
        explicit_seats = {"living-sofa": ([245, 138], "SEAT"), "world-PARK-seat": ([270, 459], "BENCH"), "bedroom-bed": ([198, 145], "BED")}
        if match[2] in explicit_seats:
            point, station = explicit_seats[match[2]]
            seat = {"point": point, "stations": [station]}
        piece = {"id": match[2], "place": place, "left": left, "right": right, "top": top,
                 "ground": ground, "back": back, "front": front, "contours": contours, "seat": seat}
        pieces.append(piece)
        if place in interiors:
            interiors[place]["pieces"].append(piece)
    assert len(pieces) == 52, f"Moebelformat geaendert: {len(pieces)}"
    outdoor_pieces = [p for p in pieces if p["place"] == "PARK"]
    # GameWorld.surfaces: 35 px hohe Baenke; GameWorld skaliert ihre X-Werte
    # mit der Abschnittsbreite. Hier bleiben die lokalen 480-px-Koordinaten.
    seats = {"FOREST": (75, 370, 222, 532, 443, 513),
             "CITY": (315, 445, 380, 538, 440, 510),
             "MOUNTAINS": (210, 340, 275, 538, 440, 510),
             "CAMP": (175, 305, 240, 538, 440, 510),
             "GROTTO": (175, 305, 240, 538, 440, 510),
             "BEACH": (170, 300, 235, 538, 440, 510),
             "SWAMP": (170, 300, 235, 538, 440, 510)}
    assert 'if (scene.place == Place.PARK) 47f else 35f' in world
    for place, (left, right, x, stand_y, top, bottom) in seats.items():
        ground = stand_y - 25
        outdoor_pieces.append({"id": f"world-{place}-seat", "place": place,
            "left": left + 10, "right": right - 10, "top": ground - 35,
            "ground": ground, "back": ground, "front": stand_y - 12,
            "contours": [[[left, top], [right, top], [right, bottom], [left, bottom]]],
            "seat": {"point": [x, ground - 35], "stations": ["BENCH"]}})
    ground = 450 + 175 * 0.75
    outdoor_pieces.append({"id": "park-crate", "place": "PARK", "left": 345.6 - 19,
        "right": 345.6 + 19, "top": ground - 20, "ground": ground,
        "back": 450 + 175 * 0.62, "front": 450 + 175 * 0.87,
        "contours": [[[326.6, ground - 20], [364.6, ground - 20], [364.6, ground], [326.6, ground]]],
        "seat": None, "paint_asset": "res://assets/world/park-crate.png"})
    doors = []
    for match in re.finditer(r"join\(Place\.(\w+), Place\.(\w+), Dir\.(\w+), Pos\(([^)]+)\), Pos\(([^)]+)\), true\)", world):
        coords = [list(map(float, re.findall(r"[.\d]+", match[j].replace("f", "")))) for j in (4, 5)]
        doors.append({"a": match[1], "b": match[2], "direction": match[3], "a_pos": coords[0], "b_pos": coords[1]})
    assert len(doors) == 11, "Tuerformat hat sich geaendert"
    characters = {}
    for species in SPECIES:
        ref = re.search(r"AvatarSpecies\." + species.upper() + r" -> Reference\((\d+), (\d+), (\d+), ([.\d]+)f\)", scale)
        assert ref, species
        characters[species] = {"label": species.title(), "top": int(ref[1]), "left": int(ref[2]),
                               "right": int(ref[3]), "relative_height": float(ref[4]),
                               "tagline": strings["avatar_" + species + "_tagline"].replace("\\'", "'"),
                               "asset": "res://assets/characters/" + species + ".png"}
    band = [[float(x) for x in m] for m in re.findall(r"GameScenes.WalkBand\(" + ",\\s*".join([NUMBER] * 3) + r"\)", world)]
    assert len(band) == 7
    source_hashes = {p: hashlib.sha256((MATRIX / p).read_bytes()).hexdigest() for p in
                     ["GameWorld.kt", "GameInteriorCatalog.kt", "GameFurniture.kt", "GameCharacterScale.kt", "GameSurfaces.kt"]}
    source_hashes["ui/GameSurfaceView.kt"] = hashlib.sha256((MATRIX.parent / "ui/GameSurfaceView.kt").read_bytes()).hexdigest()
    return {"version": 1, "regions": regions, "interiors": interiors, "doors": doors,
            "characters": characters, "labels": LABELS, "coast_path_band": band,
            "world_pieces": outdoor_pieces, "source_hashes": source_hashes}


def export(check=False):
    data = native_catalog()
    expected = json.dumps(data, ensure_ascii=False, indent=2) + "\n"
    target = HERE / "data/world_catalog.json"
    if check:
        assert target.read_text() == expected, "Katalog nicht mehr synchron; prepare_world.py ausfuehren"
    else:
        target.parent.mkdir(exist_ok=True)
        target.write_text(expected)
    # Dieselbe deterministische Pixelzeichnung wie GameSurfaceView; keine neue Illustration.
    crate = Image.new("RGBA", (38, 20), "#382c27")
    draw = ImageDraw.Draw(crate)
    def rect(x, y, w, h, color):
        draw.rectangle((x, y, x + w - 1, y + h - 1), fill=color)
    rect(2, 2, 34, 16, "#80523b")
    rect(2, 2, 34, 4, "#b38859")
    for line in (8, 13):
        rect(3, line, 32, 1, "#4e382e")
    for bar in (5, 29):
        rect(bar, 3, 4, 14, "#c29561")
        rect(bar + 1, 5, 1, 1, "#45342b")
        rect(bar + 1, 14, 1, 1, "#45342b")
    crate_path = HERE / "assets/world/park-crate.png"
    if check:
        assert Image.open(crate_path).convert("RGBA").tobytes() == crate.tobytes()
    else:
        crate_path.parent.mkdir(parents=True, exist_ok=True)
        crate.save(crate_path)
    originals = list((ART / "world").rglob("*.png")) + list((ART / "interiors").glob("*.png"))
    originals.append(REPO / "app-sim/src/main/res/raw/itoeva_theme_01.ogg")
    for source in originals:
        relative = source.relative_to(ART) if source.is_relative_to(ART) else Path("music/theme.ogg")
        dest = HERE / "assets" / relative
        if check:
            assert source.read_bytes() == dest.read_bytes(), f"Original veraendert: {relative}"
        else:
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, dest)
    for species in SPECIES:
        rich = Image.open(CREATURES / (species + ".png")).convert("RGBA")
        living = Image.open(CREATURES / (species + "-living.png")).convert("RGBA")
        assert rich.size == (138 * 128, 128) and living.size == (32 * 128, 128)
        dest = HERE / "assets/characters" / (species + ".png")
        if not check:
            dest.parent.mkdir(exist_ok=True)
            packed = Image.new("RGBA", (1536, 1920))
            for i in range(170):
                source = rich if i < 138 else living
                index = i if i < 138 else i - 138
                packed.paste(source.crop((index * 128, 0, (index + 1) * 128, 128)), ((i % 12) * 128, (i // 12) * 128))
            packed.save(dest)
        packed = Image.open(dest).convert("RGBA")
        for i in range(170):
            source, index = (rich, i) if i < 138 else (living, i - 138)
            x, y = i % 12 * 128, i // 12 * 128
            assert source.crop((index * 128, 0, (index + 1) * 128, 128)).tobytes() == packed.crop((x, y, x + 128, y + 128)).tobytes(), (species, i)
    print(f"PASS: 28 Orte, 11 Tuerpaare, {sum(len(r['pieces']) for r in data['interiors'].values())} Innenmoebel, 6x170 Originalframes und {len(originals)} unveraenderte Dateien")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    export(parser.parse_args().check)
