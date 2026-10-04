"""CAFE - das Cafe aus world-studies/itoeva-riverside-quarter von innen: Theke mit Kuchenvitrine
und Kaffeemaschine aus Messing, Kreidetafel, runde Tische mit Stuehlen, Pendelleuchten, grosse
Fenster zum Abend; hinten rechts die Tuer zum Platz."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
import props as P
from roomkit import CozyRoom, W, H
from px import rgb, MaskPen


def round_table(r, X, Z, cloth='#f0e6d0'):
    r.box(X - 0.02, X + 0.02, 0.0, 0.38, Z - 0.02, Z + 0.02, '#2e2a28')
    r.box(X - 0.1, X + 0.1, 0.0, 0.02, Z - 0.1, Z + 0.1, '#2e2a28')
    return r.box(X - 0.17, X + 0.17, 0.38, 0.41, Z - 0.17, Z + 0.17, cloth, top=cloth, edge='#ffffff')


def build(out):
    lights = [(np.array([-0.4, 1.25, 1.8]), rgb('#ffb868'), 1.1),
              (np.array([0.4, 1.25, 1.8]), rgb('#ffb868'), 1.1),
              (np.array([0.0, 0.9, 2.5]), rgb('#ffd090'), 0.5)]
    r = CozyRoom(seed=91, lights=lights, plaster=('#d8c0a0', '#e8d0b0'), wood=('#5a3a28', '#6e4630', '#80563a'), floor='checker', floor2='#e0d4bc', walls='wallpaper', pattern='#5a8a7a')
    r.meta['blocked'] = []
    door = r.door_back(0.6, 0.94, 0.98)
    # Kreidetafel und Regal mit Tassen hinter der Theke
    x0, y0, x1, y1 = r.rect_on_back(-0.86, -0.28, 0.78, 1.14)
    r.cv.paint((r.xx >= x0 - 2) & (r.xx <= x1 + 2) & (r.yy >= y0 - 2) & (r.yy <= y1 + 2), rgb('#7a5034'))
    r.cv.paint((r.xx >= x0) & (r.xx <= x1) & (r.yy >= y0) & (r.yy <= y1), rgb('#2a3430'))
    for k, w in enumerate((0.8, 0.55, 0.7, 0.45)):
        yy = y0 + 4 + k * 5
        r.cv.paint((r.xx >= x0 + 4) & (r.xx <= x0 + 4 + (x1 - x0 - 8) * w) & (r.yy == yy), rgb('#e8e0d0'))
    r.cv.paint(MaskPen(W, H).ellipse(x1 - 12, y1 - 10, x1 - 4, y1 - 3).a > 0, rgb('#e8a0a0'))
    r.box(-0.2, 0.4, 0.86, 0.89, 2.6, 2.7, '#6a4230', top='#8a5a3e', edge='#d09868')
    r.jar_row(-0.18, 0.4, 0.89, 2.65, colors=('#f0ece4', '#5a8a8a', '#f0ece4', '#c86a5a'))
    # Theke mit Vitrine und Kaffeemaschine
    counter = r.counter(-0.95, 0.42, 2.34, 2.58, Y1=0.48, top='#e8dcc0', wood='#6a4430')
    vit = r.box(-0.6, -0.12, 0.48, 0.66, 2.36, 2.5, '#c8e0e8', top='#e0f0f4', edge='#ffffff')
    for k, c in enumerate(('#e8b0a0', '#f0d8a0', '#a86a4a', '#f4e8d8')):
        r.box(-0.56 + k * 0.11, -0.47 + k * 0.11, 0.5, 0.56, 2.4, 2.46, c, top=rgb(c) * 1.1, edge=rgb(c) * 1.3)
    r.box(0.04, 0.3, 0.48, 0.74, 2.42, 2.56, '#c89a4a', top='#e8c070', side='#b88a3a', edge='#fff0b0')
    r.box(0.08, 0.26, 0.74, 0.8, 2.46, 2.54, '#3a3a3e', top='#5a5a60')
    r.cup(0.12, 0.48, 2.4)
    r.cup(0.22, 0.48, 2.4)
    # Tische mit Stuehlen
    r.rug(-0.7, 0.5, 1.3, 2.15, c1='#6a3a2a', c2='#d8a060', c3='#2e4a4a')
    seats = np.zeros((H, W), bool)
    for X, Z in ((-0.5, 1.98), (0.35, 1.75)):
        r.chair(X, Z + 0.24, back_h=0.5)
        seats |= round_table(r, X, Z)
        r.cup(X - 0.05, 0.41, Z - 0.04, '#f0ece4')
        r.box(X + 0.03, X + 0.11, 0.41, 0.44, Z - 0.03, Z + 0.05, '#e8c080', top='#f0d090')
    P.plant(r, -0.92, 1.4, kind='monstera', size=1.2)
    P.plant(r, 0.92, 2.2, kind='palm', size=1.1)
    P.string_lights(r, -1.0, 1.0, 1.36, 2.6, n=18)
    P.painting(r, 0, 0, 0.55, 0.9, kind='sunset', side=-1, Z0=1.7, Z1=2.1)
    P.painting(r, 0, 0, 0.6, 0.85, kind='sea', side=1, Z0=1.5, Z1=1.75, frame='#c8a050')
    P.clock(r, 0.5, 1.12, rad=0.05)
    for X, Z in ((-0.5, 1.98), (0.35, 1.75)):
        P.vase(r, X + 0.08, 0.41, Z + 0.06, flowers=('#f4f0e0', '#e8506a'), col='#e8e0d0')
    # Kuchenteller und Zuckerdose auf der Theke
    P.teapot(r, -0.86, 0.48, 2.42, col='#e8e0d0')
    P.fruit_bowl(r, 0.36, 0.48, 2.38)
    r.hanging_plant(110, 20, 30)
    r.hanging_plant(370, 24, 30)
    r.pendant(-0.4, 1.18, 1.8)
    r.pendant(0.4, 1.18, 1.8)
    r.walk(farLeft=146, farRight=326, nearLeft=40, nearRight=430)
    r.spot_box('SEAT', r.proj_box(0.18, 0.52, 0.0, 0.6, 1.58, 2.0), *r.stand(0.12, 1.6))
    r.spot_box('DOOR', door, *r.stand(0.77, 2.2))
    return r.finish(out, colors=200)


if __name__ == '__main__':
    print(build(sys.argv[1]))
