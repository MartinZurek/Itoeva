"""DESK - das Schreibzimmer im Stil von world-studies/cozy-home: Schreibtisch unter dem Fenster
mit Lampe, Papieren, Federkiel und Globus, Buecherwand links, Karte an der Wand, Stuhl; hinten
rechts die Tuer."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
from roomkit import CozyRoom, W, H
from px import rgb, MaskPen, dramp
import props as P


def build(out):
    lights = [(np.array([0.1, 0.8, 2.4]), rgb('#ffb060'), 1.5),
              (np.array([0.0, 1.3, 1.6]), rgb('#ffc080'), 0.7)]
    r = CozyRoom(seed=61, lights=lights, walls='boards', plaster=('#8a6a4a', '#a07a54'), wainscot=False)
    r.window(-0.3, 0.3, 0.62, 1.14, view='night')
    P.curtains(r, -0.3, 0.3, 0.62, 1.14, col='#3e5a3a')
    P.painting(r, -0.62, -0.42, 0.78, 0.98, kind='mountain')
    P.clock(r, 0.44, 1.12, rad=0.05)
    door = r.door_back(0.6, 0.94, 0.98)
    # Karte an der Wand rechts
    m = r.wall_side(1, 1.5, 2.05, 0.5, 0.98, rgb('#8a6a3a'))
    inner = r.wall_side(1, 1.53, 2.02, 0.53, 0.95, rgb('#e8d4a0'))
    land = inner & (np.sin(r.xx * 0.31) + np.cos(r.yy * 0.27) + np.sin((r.xx + r.yy) * 0.13) > 0.6)
    r.cv.paint(land, rgb('#a8b878'))
    r.cv.paint(inner & (((r.xx * 3 + r.yy * 5) % 23) == 0), rgb('#a85a3a'))
    # Buecherwand links
    r.rug(-0.6, 0.4, 1.4, 2.2, c1='#2e4a6a', c2='#d8b070', c3='#8a3a32')
    # Schreibtisch
    r.box(-0.42, 0.42, 0.42, 0.47, 2.2, 2.6, '#7a4a30', top='#9a6444', edge='#e0a070')
    r.box(-0.42, -0.12, 0.0, 0.42, 2.22, 2.6, '#6a4028', top='#8a5a3e', edge='#c08458')
    for y in (0.14, 0.28, 0.4):
        p = r.room.proj(-0.27, y - 0.06, 2.22)
        r.cv.paint(MaskPen(W, H).rect(p[0] - 2, p[1], p[0] + 2, p[1]).a > 0, rgb('#e8c070'))
    r.legs(0.34, 0.42, 2.22, 2.6, 0.42)
    # Papiere, Buecher, Tintenfass mit Feder, Globus, Lampe
    r.box(-0.14, 0.12, 0.47, 0.475, 2.3, 2.46, '#f4ead4', top='#fbf3e0')
    r.books(-0.32, 0.47, 2.44, n=3)
    r.box(0.16, 0.2, 0.47, 0.52, 2.32, 2.36, '#2a2a3a', top='#1a1a2a')
    q = r.room.proj(0.18, 0.52, 2.34)
    r.cv.paint(MaskPen(W, H).line([q, (q[0] + 6, q[1] - 12)]).a > 0, rgb('#f4f0e8'))
    g = r.room.proj(0.32, 0.62, 2.5)
    r.box(0.3, 0.34, 0.47, 0.55, 2.48, 2.52, '#c8a060')
    gl = MaskPen(W, H).ellipse(g[0] - 7, g[1] - 7, g[0] + 7, g[1] + 7).a > 0
    r.cv.paint(gl, rgb('#4a7aa8') * 0.9)
    r.cv.paint(gl & (np.sin(r.xx * 0.9) + np.cos(r.yy * 0.8) > 0.5), rgb('#7ab06a') * 0.9)
    r.cv.paint(MaskPen(W, H).line([(g[0] - 8, g[1] + 5), (g[0] + 8, g[1] - 5)]).a > 0, rgb('#c8a060'))
    r.box(-0.04, 0.0, 0.47, 0.72, 2.5, 2.54, '#2a1e18')
    lp = [r.room.proj(-0.12, 0.72, 2.52), r.room.proj(0.08, 0.72, 2.52), r.room.proj(0.04, 0.82, 2.52), r.room.proj(-0.08, 0.82, 2.52)]
    r.cv.paint(MaskPen(W, H).poly(lp).a > 0, rgb('#5a8a5a'))
    r.cv.paint(MaskPen(W, H).line([lp[0], lp[1]]).a > 0, rgb('#fff0c0'))
    r.glow_at(-0.02, 0.6, 2.4, r=16, color='#ff9a40', k=0.3)
    r.chair(0.12, 2.0, back_h=0.42)
    # Zettel an der Wand, Pinnwand mit Notizen
    x0, y0, x1, y1 = r.rect_on_back(0.36, 0.52, 0.62, 0.86)
    r.cv.paint((r.xx >= x0) & (r.xx <= x1) & (r.yy >= y0) & (r.yy <= y1), rgb('#b88a5a'))
    for k, c in enumerate(('#f4ecd0', '#f0e080', '#c8e0f0', '#f4c8c8')):
        px_, py_ = x0 + 2 + (k % 2) * ((x1 - x0) // 2), y0 + 2 + (k // 2) * ((y1 - y0) // 2)
        r.cv.paint((r.xx >= px_) & (r.xx <= px_ + 4) & (r.yy >= py_) & (r.yy <= py_ + 5), rgb(c))
        r.cv.c[py_, px_ + 2] = rgb('#c83a3a')
    P.book_stack(r, -0.3, 0.0, 1.9, n=5)
    P.basket(r, 0.3, 2.35, w=0.12, h=0.16)
    P.mug(r, 0.26, 0.47, 2.32, steam=True)
    P.string_lights(r, -0.3, 0.3, 1.2, 2.66, n=10, sag=0.04)
    # Buecherwand links
    r.wall_shelf(-1, 1.4, 2.6, Y1=1.25, boards=5)
    P.plant(r, 0.5, 2.55, kind='monstera', size=0.9)
    P.plant(r, 0.86, 1.4, kind='ficus', size=1.0, pot='#a85a3a')
    P.plant(r, -0.36, 2.55, kind='cactus', size=0.5, Y=0.47)
    r.hanging_plant(400, 24, 30)
    r.walk(farLeft=146, farRight=326, nearLeft=40, nearRight=430)
    r.spot_box('DESK', r.proj_box(-0.42, 0.42, 0.0, 0.84, 2.2, 2.6), *r.stand(0.0, 1.85))
    r.spot_box('DOOR', door, *r.stand(0.77, 2.2))
    return r.finish(out, colors=200)


if __name__ == '__main__':
    print(build(sys.argv[1]))
