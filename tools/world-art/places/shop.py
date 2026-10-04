"""SHOP - der Laden an der Strasse, im Stil des Buchladens aus world-studies/itoeva-riverside-
quarter: Regale bis unter die Decke mit Glaesern, Buechern und Kisten, freistehendes Regal in der
Mitte, Ladentisch mit Kasse und Glocke rechts, Schaufenster mit Tageslicht; Tuer hinten links."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
import props as P
from roomkit import CozyRoom, W, H
from px import rgb, MaskPen


def build(out):
    lights = [(np.array([0.0, 1.3, 1.8]), rgb('#ffd090'), 1.3),
              (np.array([0.3, 0.9, 2.6]), rgb('#e8f0ff'), 0.7)]
    r = CozyRoom(seed=81, lights=lights, plaster=('#e0caa8', '#ecd8b8'), ambient='#6a5a50', floor='stone', wood=('#8a7a68', '#a8967e', '#c0ae94'))
    r.meta['blocked'] = []
    door = r.door_back(-0.94, -0.62, 0.98)
    r.window(-0.1, 0.7, 0.42, 1.08, view='day')
    # Schild ueber dem Fenster
    x0, y0, x1, y1 = r.rect_on_back(0.1, 0.5, 1.14, 1.24)
    r.cv.paint((r.xx >= x0) & (r.xx <= x1) & (r.yy >= y0) & (r.yy <= y1), rgb('#3e5a7a'))
    r.cv.paint((r.xx >= x0 + 3) & (r.xx <= x1 - 3) & (r.yy == (y0 + y1) // 2), rgb('#e8d8b0'))
    # Wandregale links (voll) und rechts hinten
    r.wall_shelf(-1, 1.3, 2.62, Y1=1.32, boards=5)
    r.wall_shelf(1, 2.1, 2.66, Y1=1.2, boards=4)
    r.rug(-0.45, 0.45, 1.3, 2.0, c1='#3e5a7a', c2='#e8c890', c3='#8a3a32')
    # Freistehendes Regal in der Mitte (RACK) mit Kisten voller Obst und Glaesern
    rack = r.box(-0.4, 0.1, 0.0, 0.04, 1.95, 2.25, '#4a2e20')
    for y in (0.22, 0.46):
        rack |= r.box(-0.4, 0.1, y, y + 0.03, 1.95, 2.25, '#6a4230', top='#8a5a3e', edge='#c08458')
    fruit = ['#e8503a', '#f0a030', '#7ab04a', '#f4d040']
    for k, x in enumerate((-0.38, -0.22, -0.06)):
        rack |= r.box(x, x + 0.14, 0.25, 0.33, 1.98, 2.18, '#9a6a40', top=fruit[k], edge=rgb(fruit[k]) * 1.3)
        rack |= r.box(x, x + 0.14, 0.04, 0.12, 1.98, 2.18, '#9a6a40', top=fruit[k + 1], edge=rgb(fruit[k + 1]) * 1.3)
    r.jar_row(-0.38, 0.1, 0.49, 2.1)
    for x in (-0.4, 0.07):
        rack |= r.box(x, x + 0.03, 0.0, 0.64, 1.95, 1.98, '#5a3a26')
    # Ladentisch rechts mit Kasse, Glocke, Papiertueten
    counter = r.box(0.5, 0.9, 0.0, 0.46, 1.5, 1.95, '#7a4a30', top='#a87048', side='#8a5a3a', edge='#e0a070')
    counter |= r.box(0.62, 0.8, 0.46, 0.62, 1.62, 1.78, '#a8803a', top='#c8a050', side='#b88a40', edge='#f0d080')
    p = r.room.proj(0.62, 0.56, 1.7)
    r.cv.paint(MaskPen(W, H).rect(p[0] - 1, p[1] - 4, p[0] + 4, p[1] - 1).a > 0, rgb('#2a2a2a'))
    r.box(0.54, 0.6, 0.46, 0.5, 1.84, 1.9, '#e8c070', top='#f8d890')
    for k in range(3):
        r.box(0.56 + k * 0.05, 0.6 + k * 0.05, 0.46, 0.56, 1.52, 1.58, '#c8a070', top='#e0c090')
    r.plant(0.25, 2.58, size=0.8)
    r.hanging_plant(250, 18, 26)
    r.pendant(-0.15, 1.24, 1.7)
    P.basket(r, -0.55, 1.62, fill='yarn')
    P.basket(r, 0.28, 2.3, w=0.16, fill='logs')
    P.plant(r, 0.2, 2.6, kind='palm', size=0.9)
    P.vase(r, 0.84, 0.46, 1.85, flowers=('#f0c040', '#f4f0e0', '#e8506a'))
    P.book_stack(r, 0.7, 0.46, 1.62, n=3)
    P.string_lights(r, -1.0, 1.0, 1.36, 2.66, n=18)
    P.painting(r, 0.78, 0.96, 0.62, 0.84, kind='sea', frame='#c8a050')
    # Preisschilder und Saecke am Regal
    for k, x in enumerate((-0.38, -0.22, -0.06)):
        q = r.room.proj(x + 0.07, 0.36, 1.97)
        r.cv.paint(MaskPen(W, H).rect(q[0] - 3, q[1], q[0] + 3, q[1] + 3).a > 0, rgb('#f4ecd8'))
        r.cv.c[int(q[1]) + 1, int(q[0])] = rgb('#3a2a20')
    for k, x in enumerate((-0.8, -0.66)):
        r.box(x, x + 0.12, 0.0, 0.2, 1.75 + k * 0.05, 1.85 + k * 0.05, '#c8b088', top='#a88a60', edge='#e8d0a8')
    r.pendant(0.55, 1.24, 2.0)
    r.walk(farLeft=150, farRight=322, nearLeft=40, nearRight=430)
    r.spot_box('RACK', r.bbox(rack), *r.stand(-0.15, 1.8))
    r.spot_box('CHECKOUT', r.bbox(counter), *r.stand(0.36, 1.6))
    r.spot_box('DOOR', door, *r.stand(-0.78, 2.2))
    return r.finish(out, colors=200)


if __name__ == '__main__':
    print(build(sys.argv[1]))
