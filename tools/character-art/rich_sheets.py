"""Baut die feinen Bilderboegen (128 x 128 je Bild, 39 Bilder). Reihenfolge siehe motion.py.

Fennec kommt seit dem 04.10. abends aus der gemalten Key-Design-Figur (fennec_key.py, Puppe aus
puppet.py); die 3D-Formenfigur (fennec3d.py) bleibt als Werkzeug, wird aber nicht mehr geschrieben.

  python3 rich_sheets.py                      # nach app-sim/src/main/assets/creatures
  python3 rich_sheets.py --preview out.png    # Bogen vergroessert auf Wiesengruen
"""
import os
import sys
import numpy as np
from PIL import Image
import rig3d as R
import motion as Mo
import fennec_key

FIGURES = {'fennec': fennec_key}
FRAME = 128


def frames(mod):
    if hasattr(mod, 'frames'):
        return mod.frames()
    tex = R.stroke_texture([mod.FUR, mod.CREAM, mod.CAPE, mod.EARIN])
    out = []
    for pose, yaw in Mo.sheet_plan(mod.Pose):
        buf = R.render(mod.build(pose), yaw, size=96, foot_row=93)
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
