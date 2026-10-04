"""Baut je Wesen einen Bilderbogen fuer das Spiel: 11 Bilder zu 64 x 64 Pixeln nebeneinander.

Reihenfolge (muss zu `CreatureSprites.kt` passen):
  0 Ruhe, 1 Ruhe eingeatmet, 2 Blinzeln, 3..6 Laufen, 7..8 Freude, 9..10 Schlafen

Ausgabe: app-sim/src/main/assets/creatures/<wesen>.png
"""
import os
import sys
import numpy as np
from PIL import Image
from sprite import Pose
import characters as C

SPECIES = {
    'fennec': C.fennec, 'gloop': C.gloop, 'starlet': C.starlet,
    'puffling': C.puffling, 'wyrmling': C.wyrmling, 'hootlet': C.hootlet,
}
FOOT = 62   # erste Zeile unter den Fuessen
OWN_SLEEP = {C.wyrmling}


def lifted(img, n):
    """Ganze Figur [n] Pixel nach oben (Hopser beim Laufen)."""
    a = np.array(img)
    if n:
        a = np.roll(a, -n, axis=0)
        a[-n:] = 0
    return Image.fromarray(a)


def breath(img, upto=46):
    """Eingeatmet: alles oberhalb von [upto] eine Zeile tiefer - die Fuesse bleiben stehen."""
    a = np.array(img)
    b = a.copy()
    b[1:upto] = a[0:upto - 1]
    b[0] = 0
    return Image.fromarray(b)


def curled(img):
    """Schlafend zusammengesunken: drei Zeilen aus dem Koerper genommen, Fuesse bleiben unten."""
    a = np.array(img)
    rows = [r for r in range(a.shape[0]) if r not in (30, 38, 46)]
    b = np.zeros_like(a)
    b[3:] = a[rows]
    return Image.fromarray(b)


def frames(fn):
    walk = [fn(Pose(step=k)) for k in range(4)]
    joy = fn(Pose(eyes='happy', joy=True))
    # Wer eine eigene Schlafpose hat (Wyrmling), zeichnet sie selbst; die anderen sinken zusammen.
    sleep = fn(Pose(eyes='closed', sleep=True)).image() if fn in OWN_SLEEP else curled(fn(Pose(eyes='closed')).image())
    idle = fn(Pose()).image()
    return [
        idle,
        breath(idle),
        fn(Pose(eyes='closed')).image(),
        lifted(walk[0].image(), 0), lifted(walk[1].image(), 1),
        lifted(walk[2].image(), 0), lifted(walk[3].image(), 1),
        joy.image(), breath(joy.image()),
        sleep, breath(sleep),
    ]


def grounded(fr):
    """Alle Bilder so verschieben, dass die Ruhepose genau auf der Fusszeile steht."""
    a = np.array(fr[0])[..., 3] > 0
    bottom = np.nonzero(a.any(axis=1))[0].max()
    shift = (FOOT - 1) - bottom
    out = []
    for f in fr:
        arr = np.roll(np.array(f), shift, axis=0)
        if shift > 0:
            arr[:shift] = 0
        elif shift < 0:
            arr[shift:] = 0
        out.append(Image.fromarray(arr))
    return out


def build(out_dir):
    os.makedirs(out_dir, exist_ok=True)
    for name, fn in SPECIES.items():
        fr = grounded(frames(fn))
        sheet = Image.new('RGBA', (64 * len(fr), 64), (0, 0, 0, 0))
        for i, f in enumerate(fr):
            sheet.alpha_composite(f, (i * 64, 0))
        sheet.save(os.path.join(out_dir, f'{name}.png'))


def preview(path):
    rows = [grounded(frames(fn)) for fn in SPECIES.values()]
    out = Image.new('RGBA', (64 * 11 + 24, 66 * len(rows) + 8), (232, 222, 200, 255))
    for r, fr in enumerate(rows):
        for i, f in enumerate(fr):
            out.alpha_composite(f, (8 + i * 64 + (i > 2) * 4 + (i > 6) * 4 + (i > 8) * 4, 4 + r * 66))
    out.resize((out.width * 3, out.height * 3), Image.NEAREST).save(path)


if __name__ == '__main__':
    if len(sys.argv) > 2 and sys.argv[1] == '--preview':
        preview(sys.argv[2])
    else:
        build(sys.argv[1] if len(sys.argv) > 1 else '../../app-sim/src/main/assets/creatures')
