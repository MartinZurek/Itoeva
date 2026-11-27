"""Give a staged Godot Android preview a versioned name, package and launcher icon."""
import argparse
import colorsys
from pathlib import Path
import re

from PIL import Image, ImageDraw

DIGITS = (
    ("111", "101", "101", "101", "111"),
    ("010", "110", "010", "010", "111"),
    ("111", "001", "111", "100", "111"),
    ("111", "001", "111", "001", "111"),
    ("101", "101", "111", "001", "001"),
    ("111", "100", "111", "001", "111"),
    ("111", "100", "111", "101", "111"),
    ("111", "001", "010", "010", "010"),
    ("111", "101", "111", "101", "111"),
    ("111", "101", "111", "001", "111"),
)
FEATURE = "itoeva_main_game"
ICON_DIR = Path("assets/branding")


def draw_number(image, value, x, y, unit, fill):
    draw = ImageDraw.Draw(image)
    for i, digit in enumerate(str(value)):
        for row, line in enumerate(DIGITS[int(digit)]):
            for col, bit in enumerate(line):
                if bit == "1":
                    left, top = x + (i * 4 + col) * unit, y + row * unit
                    draw.rectangle((left, top, left + unit - 1, top + unit - 1), fill=fill)


def make_icons(project, build):
    target = project / ICON_DIR
    mark = Image.open(target / "water-preview-mark.png").convert("RGBA")
    mark.thumbnail((180, 169), Image.Resampling.LANCZOS)
    foreground = Image.new("RGBA", (432, 432))
    foreground.alpha_composite(mark, ((432 - mark.width) // 2, 87))

    value = str(build)
    unit = min(9, 195 // (len(value) * 4 - 1))
    number_width = (len(value) * 4 - 1) * unit
    left = (432 - number_width) // 2
    badge = (left - 17, 273, left + number_width + 16, 293 + unit * 5)
    ImageDraw.Draw(foreground).rounded_rectangle(badge, radius=19, fill="#bdf9fa", outline="#0a243d", width=4)
    draw_number(foreground, build, left, 283, unit, "#10203c")

    # Themed launcher icons use alpha. Make the number a cutout through the badge.
    mono = Image.new("RGBA", foreground.size, (255, 255, 255, 0))
    mono.putalpha(foreground.getchannel("A"))
    draw_number(mono, build, left, 283, unit, (255, 255, 255, 0))

    hue = (0.58 + (build % 12) * 0.037) % 1.0
    light = colorsys.hsv_to_rgb(hue, 0.64, 0.35)
    dark = colorsys.hsv_to_rgb(hue, 0.68, 0.09)
    background = Image.new("RGBA", foreground.size)
    draw = ImageDraw.Draw(background)
    for y in range(432):
        t = y / 431
        line = tuple(round(255 * ((1 - t) * hi + t * lo)) for hi, lo in zip(light, dark))
        draw.line((0, y, 431, y), fill=(*line, 255))

    legacy = Image.alpha_composite(background, foreground).crop((57, 57, 375, 375)).resize((192, 192), Image.Resampling.LANCZOS)
    icons = {
        "main_192x192": legacy,
        "adaptive_foreground_432x432": foreground,
        "adaptive_background_432x432": background,
        "adaptive_monochrome_432x432": mono,
    }
    for key, image in icons.items():
        image.save(target / f"preview-{key}.png", optimize=True)
    return {f"launcher_icons/{key}": f'"res://{ICON_DIR}/preview-{key}.png"' for key in icons}


def configure(project, build, version, label):
    icons = make_icons(project, build)
    presets = project / "export_presets.cfg"
    text = presets.read_text()
    match = re.search(r'^\[preset\.(\d+)\]\nname="Itoeva Main Game"$', text, re.M)
    if not match:
        raise ValueError("Itoeva Main Game export preset missing")
    header = f"[preset.{match[1]}.options]"
    before, options = text.split(header, 1)
    block, *tail = re.split(r"(?=\n\[preset\.\d+\])", options, maxsplit=1)
    settings = {
        "version/code": str(build),
        "version/name": f'"{version}"',
        "package/unique_name": f'"com.itoeva.gameplay.wassertest.b{build}"',
        "package/name": f'"Itoeva – {label} {build}"',
        "package/signed": "false",
        **icons,
    }
    for key in settings:
        block = re.sub(rf"(?m)^{re.escape(key)}=.*\n", "", block)
    block = block.rstrip() + "\n" + "\n".join(f"{key}={value}" for key, value in settings.items()) + "\n"
    presets.write_text(before + header + block + "".join(tail))
    project_file = project / "project.godot"
    text = project_file.read_text()
    text = re.sub(rf"(?m)^config/version\.{FEATURE}=.*$", f'config/version.{FEATURE}="{version}"', text)
    text = re.sub(rf"(?m)^config/build_number\.{FEATURE}=.*$", f"config/build_number.{FEATURE}={build}", text)
    project_file.write_text(text)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("project", type=Path, help="Staged project copy for export")
    parser.add_argument("build", type=int)
    parser.add_argument("version")
    parser.add_argument("--label", default="Wassertest")
    args = parser.parse_args()
    if not 1 <= args.build <= 2_100_000_000:
        parser.error("invalid Android build number")
    if not re.fullmatch(r"[A-Za-z0-9._-]+", args.version):
        parser.error("invalid version")
    if not re.fullmatch(r"[\w -]+", args.label):
        parser.error("invalid label")
    configure(args.project, args.build, args.version, args.label)
