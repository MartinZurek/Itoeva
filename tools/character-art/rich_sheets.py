"""Baut die feinen Bilderboegen (96 x 96 je Bild, 39 Bilder) fuer die Wesen, die schon als
3D-Figur vorliegen (bisher: Fennec). Reihenfolge siehe motion.py.

  python3 rich_sheets.py                      # nach app-sim/src/main/assets/creatures
  python3 rich_sheets.py --preview out.png    # Bogen vergroessert auf Wiesengruen
"""
import os
import sys
import numpy as np
from PIL import Image
import rig3d as R
import motion as Mo
import fennec3d

FIGURES = {'fennec': fennec3d}
FRAME = 96
FOOT_ROW = 93            # y = 0 der Figur; die Kontur liegt darunter, erste freie Zeile 94


def frames(mod):
    tex = R.stroke_texture([mod.FUR, mod.CREAM, mod.CAPE, mod.EARIN])
    out = []
    for pose, yaw in Mo.sheet_plan(mod.Pose):
        buf = R.render(mod.build(pose), yaw, size=FRAME, foot_row=FOOT_ROW)
        img, mi, d = R.to_pixels(buf, mod.RAMPS, texture=tex, flat=mod.FLAT)
        out.append(R.outline(img, mi, d, mod.RAMPS))
    return out


def to_image(fr):
    s = np.concatenate(fr, axis=1)
    return Image.fromarray(np.uint8(np.clip(s, 0, 1) * 255), 'RGBA')


def build(out_dir):
    for name, mod in FIGURES.items():
        to_image(frames(mod)).save(os.path.join(out_dir, f'{name}.png'), optimize=True)


def preview(path, scale=3):
    im = to_image(frames(FIGURES['fennec']))
    bg = Image.new('RGBA', im.size, (140, 158, 128, 255))
    bg.alpha_composite(im)
    bg.resize((im.width * scale, im.height * scale), Image.NEAREST).save(path)


if __name__ == '__main__':
    here = os.path.dirname(os.path.abspath(__file__))
    if len(sys.argv) > 2 and sys.argv[1] == '--preview':
        preview(sys.argv[2])
    else:
        build(sys.argv[1] if len(sys.argv) > 1 else os.path.join(here, '../../app-sim/src/main/assets/creatures'))
