"""CRAFT - die Werkstatt im Stil von world-studies/cozy-home: Werkbank mit Lochwand voller
Werkzeug, Schraubstock und Holzspaenen, Regal mit Brettern und Glaesern, Saegebock mit Brett,
Laterne und Arbeitslampe; hinten rechts die Tuer."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
from roomkit import CozyRoom, W, H
from px import rgb, MaskPen
import props as P


def build(out):
    lights = [(np.array([-0.2, 1.0, 2.3]), rgb('#ffb060'), 1.5),
              (np.array([0.2, 1.3, 1.6]), rgb('#ffc080'), 0.7)]
    r = CozyRoom(seed=71, lights=lights, plaster=('#a8604a', '#c07458'), walls='brick', wood=('#5a3a28', '#6e4630', '#80563a'), wainscot=False)
    door = r.door_back(0.6, 0.94, 0.98)
    r.window(0.0, 0.42, 0.66, 1.12, view='night')
    # Lochwand mit Werkzeug
    x0, y0, x1, y1 = r.rect_on_back(-0.86, -0.06, 0.58, 1.12)
    board = (r.xx >= x0) & (r.xx <= x1) & (r.yy >= y0) & (r.yy <= y1)
    r.cv.paint(board, rgb('#b08a5a') * 0.85)
    r.cv.paint(board & ((r.xx - x0) % 4 == 2) & ((r.yy - y0) % 4 == 2), rgb('#5a4028'))
    tools = [((x0 + 8, y0 + 6), (x0 + 8, y0 + 26), '#8a8a90', 2), ((x0 + 16, y0 + 5), (x0 + 16, y0 + 22), '#a85a3a', 3),
             ((x0 + 26, y0 + 6), (x0 + 30, y0 + 28), '#c8c8d0', 2), ((x0 + 40, y0 + 4), (x0 + 40, y0 + 18), '#6a4a2a', 4),
             ((x0 + 52, y0 + 8), (x0 + 58, y0 + 8), '#c8a060', 3), ((x0 + 66, y0 + 5), (x0 + 66, y0 + 30), '#8a8a90', 1)]
    for a, b, c, w in tools:
        r.cv.paint(MaskPen(W, H).line([a, b], w).a > 0, rgb(c))
    r.cv.paint(MaskPen(W, H).ellipse(x0 + 44, y0 + 20, x0 + 56, y0 + 32).a > 0, rgb('#a85a3a'))
    r.cv.paint(MaskPen(W, H).ellipse(x0 + 47, y0 + 23, x0 + 53, y0 + 29).a > 0, rgb('#b08a5a') * 0.85)
    # Werkbank
    bench = r.box(-0.9, -0.02, 0.44, 0.52, 2.3, 2.66, '#9a6a44', top='#b8865a', edge='#f0c080')
    r.legs(-0.88, -0.04, 2.32, 2.64, 0.44, color='#5a3a28')
    r.box(-0.86, -0.06, 0.1, 0.13, 2.34, 2.62, '#6a4430', top='#8a5a3e')
    r.box(-0.82, -0.66, 0.52, 0.62, 2.32, 2.42, '#5a6a7a', top='#7a8a9a', edge='#c8d0d8')   # Schraubstock
    r.box(-0.5, -0.2, 0.52, 0.56, 2.36, 2.5, '#d8b080', top='#e8c898', edge='#fff0d0')     # Brett
    for k in range(10):
        p = r.room.proj(-0.6 + k * 0.05, 0.52, 2.32 + (k * 7 % 5) * 0.02)
        r.cv.c[int(p[1]), int(p[0])] = rgb('#f0d8a8')
    r.jar_row(-0.16, 0.0, 0.52, 2.58, colors=('#c8a060', '#5a8a6a'))
    # Arbeitslampe ueber der Bank
    r.pendant(-0.46, 1.08, 2.4)
    # Regal rechts mit Brettern und Kisten
    r.box(0.66, 1.04, 0.0, 0.04, 1.4, 2.0, '#4a2e20')
    for y in (0.3, 0.62, 0.94):
        r.box(0.7, 1.04, y, y + 0.03, 1.4, 2.0, '#6a4230', top='#8a5a3e', edge='#c08458')
    r.box(0.74, 1.0, 0.03, 0.26, 1.5, 1.7, '#9a7048', top='#b88a5a', edge='#e0b080')
    r.jar_row(0.74, 1.0, 0.33, 1.6)
    for k in range(5):
        c = rgb(['#d8b080', '#b88a5a', '#c89a68'][k % 3])
        r.box(0.72 + k * 0.05, 0.76 + k * 0.05, 0.65, 0.92, 1.45, 1.95, c, top=c * 1.1, edge=c * 1.3)
    # Saegebock mit Brett vorn links
    for x in (-0.7, -0.4):
        r.box(x, x + 0.04, 0.0, 0.3, 1.5, 1.54, '#6a4430')
        r.box(x, x + 0.04, 0.0, 0.3, 1.72, 1.76, '#6a4430')
    r.box(-0.76, -0.3, 0.3, 0.34, 1.48, 1.78, '#c89a68', top='#e0b880', edge='#fff0d0')
    r.rug(-0.3, 0.5, 1.6, 2.2, c1='#6a4a2a', c2='#c8a060', c3='#3a2a1a')
    r.candle(0.4, 0.04, 1.42)
    # Spaene auf dem Boden, Farbtoepfe, Besen, Plaene an der Wand
    rr = np.random.default_rng(3)
    for _ in range(80):
        X, Z = rr.uniform(-0.9, 0.0), rr.uniform(1.8, 2.3)
        p = r.room.proj(X, 0.0, Z)
        r.cv.paint(MaskPen(W, H).line([p, (p[0] + rr.uniform(-2, 2), p[1] + rr.uniform(-1, 1))]).a > 0, rgb(rr.choice(['#e0c08a', '#c8a070', '#f0d8a8'])))
    for k, (X, c) in enumerate(((0.12, '#3a6aa8'), (0.22, '#c84a3a'), (0.3, '#e8c040'))):
        r.box(X - 0.04, X + 0.04, 0.0, 0.1, 2.5, 2.58, '#8a8a90', top=c, edge='#d8d8e0')
    a, b = r.room.proj(0.55, 0.0, 2.6), r.room.proj(0.5, 0.7, 2.68)
    r.cv.paint(MaskPen(W, H).line([a, b], 1).a > 0, rgb('#8a6a40'))
    r.cv.paint(MaskPen(W, H).poly([(a[0] - 4, a[1]), (a[0] + 4, a[1]), (a[0] + 2, a[1] - 8), (a[0] - 2, a[1] - 8)]).a > 0, rgb('#c8a060'))
    x0, y0, x1, y1 = r.rect_on_back(0.08, 0.5, 1.15, 1.32)
    r.cv.paint((r.xx >= x0) & (r.xx <= x1) & (r.yy >= y0) & (r.yy <= y1), rgb('#e8dcc0'))
    r.cv.paint((r.xx >= x0) & (r.xx <= x1) & (r.yy >= y0) & (r.yy <= y1) & (((r.xx - x0) % 6 == 0) | ((r.yy - y0) % 5 == 0)), rgb('#5a7aa8'))
    P.wall_shelf_clutter(r, -0.9, -0.3, 1.24)
    P.basket(r, -0.2, 1.65, fill='logs')
    P.plant(r, 0.5, 2.55, kind='palm', size=0.9)
    r.hanging_plant(70, 30, 26)
    r.walk(farLeft=146, farRight=326, nearLeft=40, nearRight=430)
    r.spot_box('CRAFT', r.proj_box(-0.9, -0.02, 0.0, 1.12, 2.3, 2.66), *r.stand(-0.46, 2.2))
    r.spot_box('DOOR', door, *r.stand(0.77, 2.2))
    return r.finish(out, colors=200)


if __name__ == '__main__':
    print(build(sys.argv[1]))
