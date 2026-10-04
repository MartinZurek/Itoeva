"""KITCHEN - die Kueche im Stil von world-studies/cozy-home: gruene Kuechenzeile mit heller
Platte, Herd mit Kupfertopf, Regal mit Glaesern unter dem Fenster, rundlicher Kuehlschrank in
Mint an der rechten Wand, Holztisch mit Stuehlen und Obstschale; hinten links die Tuer."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
from roomkit import CozyRoom, W, H
from px import rgb, MaskPen


def build(out):
    lights = [(np.array([0.0, 1.3, 1.75]), rgb('#ffbe70'), 1.6),
              (np.array([0.3, 0.9, 2.2]), rgb('#ffd090'), 0.8)]
    r = CozyRoom(seed=41, lights=lights, plaster=('#e0c49c', '#ecd4ae'))
    # Kacheln hinter der Kuechenzeile
    back = r.which == 4
    tiles = back & (r.X > -0.4) & (r.X < 1.05) & (r.Y > 0.46) & (r.Y < 0.8)
    tc = np.where(((((r.X / 0.08).astype(int) + (r.Y / 0.08).astype(int)) % 2) == 0)[..., None], rgb('#e8e0cc'), rgb('#7aa8a0'))
    r._shade(tiles, tc, (0, 0, -1))
    r.window(-0.05, 0.45, 0.6, 1.1, view='night')
    door = r.door_back(-0.92, -0.58, 0.98)
    # Kuechenzeile hinten, Herd rechts darin
    r.counter(-0.4, 0.7, 2.42, 2.7, wood='#5a7a6a')
    r.box(0.26, 0.62, 0.0, 0.46, 2.4, 2.7, '#3a3a3e', top='#2a2a2e', edge='#8a8a90')
    for x in (0.34, 0.52):
        p = r.room.proj(x, 0.46, 2.55)
        r.cv.paint(MaskPen(W, H).ellipse(p[0] - 4, p[1] - 1, p[0] + 4, p[1] + 1).a > 0, rgb('#1a1a1e'))
    r.box(0.3, 0.46, 0.47, 0.6, 2.5, 2.62, '#c87a4a', top='#a85a3a', edge='#f0b080')   # Kupfertopf
    p = r.room.proj(0.44, 0.22, 2.4)
    r.cv.paint(MaskPen(W, H).rect(p[0] - 10, p[1] - 4, p[0] + 10, p[1] + 4).a > 0, rgb('#1e1e22'))
    r.glow_at(0.44, 0.2, 2.39, r=4, color='#ff7030', k=0.5)
    # Regal ueber der Zeile mit Glaesern
    r.box(-0.38, -0.08, 0.92, 0.95, 2.58, 2.7, '#6a4230', top='#8a5a3e', edge='#d09868')
    r.jar_row(-0.36, -0.08, 0.95, 2.64)
    r.jar_row(-0.36, 0.2, 0.47, 2.6, colors=('#e8e0d0', '#5a8a6a', '#c8a060'))
    # Haengende Kraeuter und Pfannen
    for k, x in enumerate((0.6, 0.72, 0.86)):
        a = r.room.proj(x, 1.1, 2.66)
        b = r.room.proj(x, 0.94, 2.66)
        r.cv.paint(MaskPen(W, H).line([a, b]).a > 0, rgb('#3a2a20'))
        r.cv.paint(MaskPen(W, H).ellipse(b[0] - 4, b[1], b[0] + 4, b[1] + 6).a > 0, rgb(['#c87a4a', '#3a3a3e', '#c87a4a'][k]))
    # Kuehlschrank an der rechten Wand
    fr = r.box(0.7, 1.04, 0.0, 0.86, 1.98, 2.34, '#a8d8c0', top='#c8eedc', side='#98c8b0', edge='#f0fff8')
    a, b = r.room.proj(0.7, 0.58, 1.98), r.room.proj(0.7, 0.58, 2.34)
    r.cv.paint(MaskPen(W, H).line([a, b]).a > 0, rgb('#5a8a7a'))
    for y0, y1 in ((0.64, 0.8), (0.3, 0.52)):
        a, b = r.room.proj(0.7, y0, 2.03), r.room.proj(0.7, y1, 2.03)
        r.cv.paint(MaskPen(W, H).line([a, b], 2).a > 0, rgb('#e8e8e0'))
    r.box(0.74, 0.86, 0.86, 0.94, 2.06, 2.2, '#c8a060', top='#e8c080')     # Brotkorb oben
    # Tisch mit Stuehlen, Obstschale
    r.rug(-0.66, 0.3, 1.45, 2.15, c1='#8a5a3a', c2='#e8c890', c3='#4a6a5a')
    r.chair(-0.18, 2.12)
    r.table(-0.5, 0.12, 1.66, 1.98, h=0.4)
    r.box(-0.28, -0.08, 0.4, 0.44, 1.76, 1.88, '#e8e0d0', top='#f4ece0', edge='#ffffff')
    for k, c in enumerate(('#e85a3a', '#f0c040', '#7ab04a')):
        r.box(-0.25 + k * 0.06, -0.21 + k * 0.06, 0.44, 0.49, 1.79, 1.84, c, top=rgb(c) * 1.2, edge=rgb(c) * 1.4)
    r.cup(0.0, 0.4, 1.74)
    r.chair(-0.62, 1.8, back=False)
    r.plant(-0.9, 1.38, size=1.0)
    r.hanging_plant(98, 20, 30)
    r.pendant(-0.18, 1.2, 1.82)
    r.walk(farLeft=146, farRight=318, nearLeft=40, nearRight=420)
    r.spot_box('FRIDGE', r.bbox(fr), *r.stand(0.52, 1.85))
    r.spot_box('TABLE', r.proj_box(-0.5, 0.12, 0.0, 0.44, 1.66, 1.98), *r.stand(-0.2, 1.5))
    r.spot_box('DOOR', door, *r.stand(-0.75, 2.2))
    return r.finish(out, colors=150)


if __name__ == '__main__':
    print(build(sys.argv[1]))
