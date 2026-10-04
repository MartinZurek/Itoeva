"""WORK - die Arbeitsstube in der Stadt (ein Kartenzeichner-Atelier im Stil der Weltstudien):
grosser Tisch mit ausgerollten Plaenen in der Mitte, Zeichenpult mit Lampe am Fenster rechts,
Regale mit Rollen und Ordnern, Tageslicht durch hohe Fenster; Tuer hinten links."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
from roomkit import CozyRoom, W, H
from px import rgb, MaskPen


def build(out):
    lights = [(np.array([0.2, 1.0, 2.6]), rgb('#f0f0ff'), 1.0),
              (np.array([-0.2, 1.3, 1.7]), rgb('#ffd8a0'), 1.0),
              (np.array([0.75, 0.8, 1.9]), rgb('#ffb060'), 0.6)]
    r = CozyRoom(seed=101, lights=lights, plaster=('#dcccb0', '#e8dcc4'), ambient='#6a6060')
    r.meta['blocked'] = []
    door = r.door_back(-0.94, -0.62, 0.98)
    r.window(-0.4, 0.05, 0.42, 1.1, view='day')
    r.window(0.25, 0.7, 0.42, 1.1, view='day')
    # Regal mit Rollen links
    r.wall_shelf(-1, 1.35, 2.6, Y1=1.2, boards=4)
    # Grosser Tisch mit Plaenen (TABLE)
    r.rug(-0.6, 0.4, 1.3, 2.15, c1='#4a5a3a', c2='#d8c090', c3='#2e3a2a')
    table = r.box(-0.55, 0.25, 0.4, 0.45, 1.72, 2.12, '#8a5a3a', top='#a87048', edge='#e0a070')
    r.legs(-0.53, 0.23, 1.74, 2.1, 0.4)
    plan = r.box(-0.45, 0.1, 0.45, 0.455, 1.78, 2.05, '#e8dcb8', top='#f0e6c8')
    pm = r.room.quad([(-0.42, 0.456, 1.8), (0.07, 0.456, 1.8), (0.07, 0.456, 2.03), (-0.42, 0.456, 2.03)], rgb('#f0e6c8'))
    r.cv.paint(pm & (((r.xx // 3) + (r.yy // 2)) % 7 == 0), rgb('#6a8aa8'))
    r.cv.paint(pm & (np.abs(np.sin(r.xx * 0.15) * 4 - (r.yy - r.yy[pm].mean())) < 0.8), rgb('#a85a3a'))
    r.box(0.12, 0.2, 0.45, 0.5, 1.9, 1.98, '#3a3a5a', top='#2a2a3a')
    r.books(-0.48, 0.45, 2.08, n=2)
    r.chair(-0.15, 2.3, back_h=0.5)
    # Zeichenpult am rechten Fenster (WORKPLACE)
    desk = r.box(0.56, 1.02, 0.0, 0.06, 1.7, 2.1, '#5a3a26')
    for z in (1.72, 2.06):
        desk |= r.box(0.6, 0.64, 0.0, 0.5, z, z + 0.03, '#5a3a26')
        desk |= r.box(0.96, 1.0, 0.0, 0.62, z, z + 0.03, '#5a3a26')
    desk |= r.room.quad([(0.56, 0.5, 1.7), (1.02, 0.64, 1.7), (1.02, 0.64, 2.12), (0.56, 0.5, 2.12)], rgb('#c89a6a') * r.room.light_at(np.array([0.8, 0.6, 1.9]), (-0.4, 1, 0)))
    r.room.quad([(0.62, 0.52, 1.78), (0.98, 0.63, 1.78), (0.98, 0.63, 2.04), (0.62, 0.52, 2.04)], rgb('#f4ecd8') * 1.1)
    r.box(0.66, 0.68, 0.62, 0.86, 1.72, 1.74, '#2a2a2a')
    lp = [r.room.proj(0.58, 0.86, 1.73), r.room.proj(0.72, 0.86, 1.73), r.room.proj(0.68, 0.94, 1.73), r.room.proj(0.62, 0.94, 1.73)]
    r.cv.paint(MaskPen(W, H).poly(lp).a > 0, rgb('#5a8a5a'))
    r.glow_at(0.66, 0.78, 1.74, r=12, color='#ffa050', k=0.35)
    r.chair(0.48, 1.9, back_h=0.48)
    r.plant(-0.9, 1.25, size=1.1)
    r.plant(0.12, 2.58, size=0.8)
    r.pendant(-0.15, 1.22, 1.9)
    r.walk(farLeft=150, farRight=322, nearLeft=40, nearRight=430)
    r.spot_box('TABLE', r.proj_box(-0.55, 0.25, 0.0, 0.46, 1.72, 2.12), *r.stand(-0.2, 1.55))
    r.spot_box('WORKPLACE', r.bbox(desk), *r.stand(0.42, 1.6))
    r.spot_box('DOOR', door, *r.stand(-0.78, 2.2))
    return r.finish(out, colors=150)


if __name__ == '__main__':
    print(build(sys.argv[1]))
