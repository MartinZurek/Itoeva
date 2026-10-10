"""Originalgrafiken verlustfrei fuer Godot kopieren/umpacken; kein Neuentwurf."""
from pathlib import Path
import argparse
import hashlib
import shutil
from PIL import Image

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[1]
WORLD = REPO / "app-sim/src/game/assets/world/street-park-forest.png"
FENNEC = REPO / "app-sim/src/main/assets/creatures/fennec.png"
ASSETS = HERE / "assets"


def verify():
    assert WORLD.read_bytes() == (ASSETS / WORLD.name).read_bytes(), "Kulisse veraendert"
    original = Image.open(FENNEC).convert("RGBA")
    packed = Image.open(ASSETS / "fennec-atlas.png").convert("RGBA")
    assert original.size == (138 * 128, 128)
    assert packed.size == (12 * 128, 12 * 128)
    for i in range(138):
        a = original.crop((i * 128, 0, (i + 1) * 128, 128))
        x, y = i % 12 * 128, i // 12 * 128
        b = packed.crop((x, y, x + 128, y + 128))
        assert a.tobytes() == b.tobytes(), f"Bild {i} veraendert"
    print("PASS: Kulisse byte-identisch; 138 Fennec-Bilder pixel-identisch.")
    for path in (WORLD, FENNEC):
        print(path.relative_to(REPO), hashlib.sha256(path.read_bytes()).hexdigest())


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    if not args.check:
        ASSETS.mkdir(exist_ok=True)
        shutil.copyfile(WORLD, ASSETS / WORLD.name)
        original = Image.open(FENNEC).convert("RGBA")
        assert original.size == (138 * 128, 128), "Formatentscheidung vor neuem Import pruefen"
        packed = Image.new("RGBA", (1536, 1536))
        for i in range(138):
            packed.paste(original.crop((i * 128, 0, (i + 1) * 128, 128)), (i % 12 * 128, i // 12 * 128))
        packed.save(ASSETS / "fennec-atlas.png")
    verify()
