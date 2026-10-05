"""Baut die feinen Bilderboegen (128 x 128 je Bild, Fennec: 68 Bilder). Plan: fennec_key.py.

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
import motion as Mo  # Archivplan des 3D-Prototyps (39 Bilder)
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


def expression_preview(path):
    images = frames(FIGURES['fennec'])
    # Beide Laufrichtungen nebeneinander machen Spiegelung und Schritte pruefbar.
    clips = [list(range(8)), list(range(9,17)), list(range(9,17)), list(range(39,47)), [17,18,19,20,21,22,0,0]]
    rendered = []
    for i in range(32):
        bg = Image.new('RGBA', (FRAME*5, FRAME+24), (99,112,101,255))
        for col, clip in enumerate(clips):
            # Ruhe langsam, Gang in Originalkadenz, Freude laesst sich in Folge lesen.
            step = (i//4 if col in (0,3) else i) % len(clip)
            fr = to_image([images[clip[step]]])
            if col == 2:
                fr = fr.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
            bg.alpha_composite(fr, (col*FRAME, 24))
        from PIL import ImageDraw
        d = ImageDraw.Draw(bg)
        for col, title in enumerate(['Umschauen / Gestik','Gang rechts','Gang links','Ruhe vorn','Freude']):
            d.text((col*FRAME+6,6), title, fill=(235,223,196))
        rendered.append(bg.convert('RGB').resize((FRAME*10,(FRAME+24)*2),Image.Resampling.NEAREST))
    rendered[0].save(path,save_all=True,append_images=rendered[1:],duration=105,loop=0)


if __name__ == '__main__':
    here = os.path.dirname(os.path.abspath(__file__))
    if len(sys.argv) > 2 and sys.argv[1] == '--expressions':
        expression_preview(sys.argv[2])
    elif len(sys.argv) > 2 and sys.argv[1] == '--preview':
        preview(sys.argv[2])
    else:
        build(sys.argv[1] if len(sys.argv) > 1 else os.path.join(here, '../../app-sim/src/main/assets/creatures'))
