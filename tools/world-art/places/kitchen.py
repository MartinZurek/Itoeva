"""KITCHEN - die Kueche im Stil von world-studies/cozy-home: gruene Kuechenzeile mit heller
Platte, Herd mit Kupfertopf, Regal mit Glaesern unter dem Fenster, rundlicher Kuehlschrank in
Mint an der rechten Wand, Holztisch mit Stuehlen und Obstschale; hinten links die Tuer."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
from roomkit import CozyRoom, W, H
from px import rgb, MaskPen, lerp
import props as P


def build(out):
    lights = [(np.array([0.0, 1.3, 1.75]), rgb('#ffbe70'), 1.6),
              (np.array([0.3, 0.9, 2.2]), rgb('#ffd090'), 0.8)]
    r = CozyRoom(seed=41, lights=lights, plaster=('#e0c49c', '#ecd4ae'), floor='checker', wood=('#6a4430', '#8a5038', '#a8583a'), floor2='#d8c8ac')
    # Kacheln hinter der Kuechenzeile
    back = r.which == 4
    tiles = back & (r.X > -0.4) & (r.X < 1.05) & (r.Y > 0.46) & (r.Y < 0.8)
    ti, tj = (r.X / 0.08).astype(int), (r.Y / 0.08).astype(int)
    var = (((ti * 73856093) ^ (tj * 19349663)) % 1000) / 1000
    tc = np.where((((ti + tj) % 2) == 0)[..., None], rgb('#e8e0cc'), rgb('#7aa8a0')) * (0.93 + var[..., None] * 0.12)
    deco = (var > 0.86)
    u, v = (r.X / 0.08) % 1, (r.Y / 0.08) % 1
    flower = deco & ((np.abs(u - 0.5) + np.abs(v - 0.5)) < 0.3)
    tc = np.where(flower[..., None], rgb('#3a6aa8'), tc)
    grout = (np.abs(u - 0.5) > 0.44) | (np.abs(v - 0.5) > 0.44)
    tc = np.where(grout[..., None], rgb('#d8d0c0'), tc)
    r._shade(tiles, tc, (0, 0, -1))
    r.window(-0.05, 0.45, 0.6, 1.1, view='night')
    x0, y0, x1, y1 = r.rect_on_back(-0.05, 0.45, 0.6, 0.78)
    cafe = (r.xx >= x0) & (r.xx <= x1) & (r.yy >= y0) & (r.yy <= y1)
    r.cv.paint(cafe, lerp(rgb('#f4ecd8'), rgb('#e0d0b0'), ((r.xx % 4) < 2)[..., None]))
    r.cv.paint(cafe & (((r.xx // 3) + (r.yy // 3)) % 2 == 0), rgb('#d86a5a') * 0.9)
    r.cv.paint((r.xx >= x0) & (r.xx <= x1) & (r.yy == y0), rgb('#5a3a26'))
    P.painting(r, -0.62, -0.46, 0.82, 1.02, kind='flowers', frame='#c8a050')
    x0, y0, x1, y1 = r.rect_on_back(0.52, 0.66, 0.72, 0.92)
    r.cv.paint((r.xx >= x0) & (r.xx <= x1) & (r.yy >= y0) & (r.yy <= y1), rgb('#f4ecdc'))
    for k in range(4):
        r.cv.paint((r.xx >= x0 + 2) & (r.xx <= x1 - 2) & (r.yy == y0 + 4 + k * 3), rgb('#c8b8a0'))
    r.cv.paint((r.xx >= x0) & (r.xx <= x1) & (r.yy >= y0) & (r.yy <= y0 + 2), rgb('#c84a3a'))
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
    r.jar_row(-0.36, 0.0, 0.47, 2.6, colors=('#e8e0d0', '#5a8a6a', '#c8a060'))
    # Kraeuter auf der Fensterbank, Brett mit Brot, Teekessel auf dem Herd
    for k, x in enumerate((0.0, 0.14, 0.3)):
        P.plant(r, x, 2.66, kind=['herbs', 'geranium', 'herbs'][k], size=0.45, Y=0.6, pot=['#c8784a', '#e8e0d0', '#5a7aa8'][k])
    r.box(0.04, 0.22, 0.47, 0.49, 2.48, 2.6, '#b88a5a', top='#d0a070', edge='#f0c890')
    r.box(0.07, 0.17, 0.49, 0.54, 2.5, 2.58, '#c8884a', top='#e0a868', edge='#f8d098')
    P.teapot(r, 0.5, 0.46, 2.52, col='#c84a3a')
    # Tellerbord
    r.box(0.28, 0.66, 0.84, 0.86, 2.62, 2.7, '#6a4230', top='#8a5a3e', edge='#d09868')
    for k in range(6):
        p = r.room.proj(0.32 + k * 0.06, 0.86, 2.68)
        r.cv.paint(MaskPen(W, H).ellipse(p[0] - 2, p[1] - 6, p[0] + 2, p[1]).a > 0, rgb(['#f4ecdc', '#5a8aa8', '#f4ecdc', '#c86a4a'][k % 4]))
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
    for k, (y, z, c) in enumerate(((0.7, 2.1, '#e84a4a'), (0.6, 2.2, '#f0c040'), (0.45, 2.08, '#4a8ae8'), (0.3, 2.25, '#7ab04a'))):
        q = r.room.proj(0.7, y, z)
        r.cv.paint(MaskPen(W, H).rect(q[0] - 1, q[1] - 1, q[0] + 1, q[1] + 1).a > 0, rgb(c))
    q = r.room.proj(0.7, 0.68, 2.25)
    r.cv.paint(MaskPen(W, H).rect(q[0] - 3, q[1] - 4, q[0] + 2, q[1] + 3).a > 0, rgb('#f4ecdc'))
    P.basket(r, 0.5, 2.28, fill='logs')
    # Tisch mit Stuehlen, Obstschale
    r.rug(-0.66, 0.3, 1.45, 2.15, c1='#8a5a3a', c2='#e8c890', c3='#4a6a5a')
    r.chair(-0.18, 2.12)
    r.table(-0.5, 0.12, 1.66, 1.98, h=0.4)
    cloth = r.room.quad([(-0.42, 0.401, 1.7), (0.04, 0.401, 1.7), (0.04, 0.401, 1.94), (-0.42, 0.401, 1.94)], rgb('#f0e6d4'))
    r.cv.paint(cloth & (((r.xx // 3) + (r.yy // 2)) % 2 == 0), rgb('#d86a5a') * 0.85)
    P.fruit_bowl(r, -0.2, 0.4, 1.82)
    P.vase(r, -0.38, 0.4, 1.86, flowers=('#f0c040', '#f4f0e0', '#e8804a'), col='#e8e0d0')
    P.mug(r, 0.0, 0.4, 1.74, steam=True)
    r.cup(-0.1, 0.4, 1.72, '#5a8aa8')
    r.chair(-0.62, 1.8, back=False)
    P.plant(r, -0.9, 1.38, kind='palm', size=1.1)
    P.plant(r, -0.6, 2.56, kind='snake', size=0.8)
    r.hanging_plant(98, 20, 30)
    r.pendant(-0.18, 1.2, 1.82)
    r.walk(farLeft=146, farRight=318, nearLeft=40, nearRight=420)
    r.spot_box('FRIDGE', r.bbox(fr), *r.stand(0.52, 1.85))
    r.spot_box('TABLE', r.proj_box(-0.5, 0.12, 0.0, 0.44, 1.66, 1.98), *r.stand(-0.2, 1.5))
    r.spot_box('DOOR', door, *r.stand(-0.75, 2.2))
    return r.finish(out, colors=200)


if __name__ == '__main__':
    print(build(sys.argv[1]))
