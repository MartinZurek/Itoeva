"""BEDROOM - das Schlafzimmer nach world-studies/cozy-home (rechte Haelfte): Holzbett mit gruener
Karodecke unter einem hellen Vorhang, Nachttisch mit Lampe, Mondfenster, Kommode, Truhe am
Fussende, Haengepflanzen; hinten links die Tuer ins Wohnzimmer."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
from roomkit import CozyRoom, W, H, LEFT, RIGHT, BACK
from px import rgb, MaskPen, lerp


def build(out):
    lights = [(np.array([0.05, 0.75, 2.45]), rgb('#ffb060'), 1.2),
              (np.array([0.0, 1.3, 1.6]), rgb('#ffc080'), 0.8)]
    r = CozyRoom(seed=31, lights=lights, plaster=('#d4b8a0', '#e4caae'))
    r.window(-0.42, 0.0, 0.5, 1.08, view='night')
    door = r.door_back(-0.92, -0.58, 0.98)
    r.picture(0.42, 0.62, 0.86, 1.04, colors=('#2a3a6a', '#6a7ab0', '#e8d8a0'))
    # Vorhang am Bett: helle Bahnen in Falten, an einer Stange
    for X0, X1 in ((0.16, 0.34), (0.88, 1.04)):
        x0, y0, x1, y1 = r.rect_on_back(X0, X1, 0.2, 1.18)
        cur = (r.xx >= x0) & (r.xx <= x1) & (r.yy >= y0) & (r.yy <= y1)
        fold = (np.sin((r.xx - x0) * 1.2) + 1) / 2
        col = lerp(rgb('#c8b0a0'), rgb('#f4e8dc'), fold[..., None])
        r.cv.paint(cur, col * r.room.light_at(np.array([0.6, 0.7, 2.7]), (0, 0, -1)))
    x0, y0, x1, y1 = r.rect_on_back(0.14, 1.05, 1.18, 1.2)
    r.cv.paint((r.xx >= x0) & (r.xx <= x1) & (r.yy >= y0 - 1) & (r.yy <= y1 + 1), rgb('#4a2e20'))
    r.rug(-0.5, 0.3, 1.35, 2.15, c1='#6a3a5a', c2='#e8c890', c3='#2e3a5a', round_=True)
    r.bed(0.24, 0.98, 1.92, 2.68, blanket='#5a8a5a', pattern='#e8d8a0')
    # Nachttisch mit Lampe und Buch
    r.cabinet(0.02, 0.2, 0.4, 2.46, 2.66, drawers=2)
    r.box(0.09, 0.13, 0.4, 0.5, 2.54, 2.58, '#3a2418')
    lp = [r.room.proj(0.03, 0.5, 2.56), r.room.proj(0.19, 0.5, 2.56), r.room.proj(0.16, 0.62, 2.56), r.room.proj(0.06, 0.62, 2.56)]
    r.cv.paint(MaskPen(W, H).poly(lp).a > 0, rgb('#f8c070'))
    r.glow_at(0.11, 0.55, 2.56, r=12, color='#ff9a40', k=0.35)
    r.books(0.11, 0.4, 2.5, n=1)
    # Kommode links mit Kerze und Spiegel
    r.cabinet(-1.04, -0.84, 0.42, 1.3, 1.6, drawers=3)
    r.candle(-0.92, 0.42, 1.42)
    r.wall_side(-1, 1.32, 1.58, 0.62, 0.98, rgb('#7a5a3a'))
    r.wall_side(-1, 1.35, 1.55, 0.65, 0.95, rgb('#8aa8c0') * 0.8)
    r.plant(-0.3, 2.56, size=0.9)
    r.hanging_plant(80, 22, 34)
    r.hanging_plant(418, 30, 28)
    # Sterne-Girlande ueber dem Fenster
    a, b = r.room.proj(-0.46, 1.16, 2.7), r.room.proj(0.04, 1.16, 2.7)
    for k in range(9):
        t = k / 8
        x = a[0] + (b[0] - a[0]) * t
        y = a[1] + 4 * np.sin(np.pi * t)
        r.cv.paint(MaskPen(W, H).ellipse(x - 1, y - 1, x + 1, y + 1).a > 0, rgb('#ffe090'))
        r.glow_at(-0.46 + 0.5 * t, 1.16 - 0.02 * np.sin(np.pi * t), 2.69, r=3, color='#ffc060', k=0.3)
    r.walk(farLeft=146, farRight=326, nearLeft=40, nearRight=430)
    r.spot_box('BED', r.proj_box(0.24, 0.98, 0.0, 0.7, 1.92, 2.68), *r.stand(0.6, 1.62))
    r.spot_box('DOOR', door, *r.stand(-0.75, 2.2))
    return r.finish(out, colors=150)


if __name__ == '__main__':
    print(build(sys.argv[1]))
