"""CITY - der Marktplatz am Abend, nach der Abendtafel von world-studies/itoeva-riverside-quarter:
Haeuser mit warm erleuchteten Laeden, gestreifter Marktstand mit Obstkisten und Laternen,
Wimpelketten, Stadt am Hang mit Lichtern, Laternen und Bank auf dem Pflaster."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
from kit import Scene, W, H
from px import rgb


def stall(s, x0, x1, base):
    """Marktstand: Pfosten, Tresen mit Tuch, Obstkisten, gestreifte Markise, Laternen."""
    top = base - 52
    for x in (x0, x1 - 3):
        s.paint(s.rect(x, top, x + 2, base), '#5a3a26')
    counter = s.rect(x0 + 2, base - 20, x1 - 2, base)
    s.shade(counter, ['#4a2e20', '#6a4430', '#8a5a3e', '#a87250'], light=(-1, -0.4))
    cloth = s.rect(x0 + 4, base - 22, x1 - 4, base - 10)
    s.paint(cloth, '#f0e6d0')
    s.paint(cloth & ((s.xx // 6) % 2 == 0) & (s.yy > base - 13), '#d8c8a8')
    fruit = ['#e8503a', '#f0a030', '#f4d040', '#7ab04a', '#a83a5a']
    for k, x in enumerate(range(x0 + 6, x1 - 14, 16)):
        s.paint(s.rect(x, base - 30, x + 13, base - 22), '#9a6a40')
        s.paint(s.rect(x, base - 30, x + 13, base - 30), '#c8925a')
        for j in range(6):
            fx, fy = x + 1 + (j % 3) * 4, base - 33 + (j // 3) * 3
            s.paint(s.ellipse(fx + 1.5, fy + 1.5, 1.8, 1.6), fruit[(k + j // 3) % len(fruit)])
    s.awning(x0 - 6, x1 + 6, top, depth=14, c1='#d8644a', c2='#f4ead6')
    lamps = []
    for x in (x0 + 8, x1 - 10):
        s.paint(s.rect(x, top + 14, x, top + 18), '#2a2020')
        s.paint(s.rect(x - 2, top + 18, x + 2, top + 23), '#ffd890')
        lamps.append((x, top + 20, 5))
    # Kreidetafel vorn am Stand
    s.paint(s.poly([(x1 - 4, base), (x1 + 2, base - 18), (x1 + 14, base - 18), (x1 + 18, base)]), '#5a3a26')
    s.paint(s.rect(x1 + 3, base - 16, x1 + 13, base - 4), '#2a3430')
    s.paint(s.rect(x1 + 5, base - 13, x1 + 11, base - 13), '#e8e0d0')
    s.paint(s.rect(x1 + 5, base - 9, x1 + 9, base - 9), '#e8e0d0')
    return lamps


def build(out):
    s = Scene('city', horizon=150, seed=24)
    s.sky(top='#2a3666', mid='#7a6aa0', low='#f0a878', warm='#ffd8a0', clouds=False)
    s.clouds(n=4, ymin=10, ymax=50, lit='#ffc8a0', mid='#e8a0a0', shade='#9a7aa8', base='#7a6a9a')
    sky = s.cv.c.copy()
    s.hills([(0, 480, 84, '#6a6a98', '#9a90b8', 3, 0.4), (120, 460, 60, '#7a9a7a', '#a8c098', 8, 0.15)])
    s.town(150, 420, 64, 140, n=110, k=0.9, tower=(286, 52, 24), lit=True)
    for x in range(160, 420, 19):
        s.tree(x, 128 + (x * 11) % 18, 20 + (x * 7) % 10, seed=x, kind='cypress')
    aq = s.rect(330, 110, 470, 118)
    s.paint(aq, '#d8c8b0')
    for x in range(334, 470, 12):
        s.paint(s.ellipse(x + 4, 118, 4, 6) & (s.yy >= 113), '#7a7aa0')
    s.grass(150, flowers=False)
    # Hintere Hausreihe
    s.facade(-10, 120, 54, 176, shutters='#4e8a86', floors=2, shop=True, seed=5)
    s.awning(-6, 116, 126, depth=12, c1='#3e7a6a', c2='#3e7a6a')
    s.facade(330, 490, 48, 176, wall=('#c4a888', '#d8bc98', '#e8d0ac', '#f2e2c4'), shutters='#7a5a8a',
             floors=2, shop=True, door_x=444, seed=6)
    s.awning(334, 436, 118, depth=12, c1='#d8a060', c2='#f4ead6')
    # Platz
    s.cobble(s.yy >= 172, tone=('#a88a70', '#bca088', '#d2b89c', '#e6d0b4'), size=0.11)
    s.paint(s.rect(120, 168, 330, 176), '#8a8278')
    for x in range(126, 330, 26):
        s.bush(x + 10, 174, 8 + (x % 5), seed=x, flowers=('#f4f0e0', '#e88aa0') if x % 52 == 0 else None)
    lamps = stall(s, 168, 286, 204)
    s.tree(116, 196, 96, seed=51, kind='round')
    for x in (300, 140):
        s.planter(x, 206, 16, s=0.9)
    s.bunting([(-4, 70), (180, 92)])
    s.bunting([(290, 90), (484, 64)], colors=('#4e7aa8', '#e8b440', '#f4ead6', '#d8644a'))
    lamp = s.lamp(344, 210, s=1.05, lit=True)
    lamp2 = s.lamp(40, 214, s=1.05, lit=True)
    bench = s.bench(398, 214, s=1.0)
    windows = [(4, 98, 108, 172), (338, 98, 436, 172)]
    lights = lamps + [(lamp[0] + 7, lamp[1] + 6, 8), (lamp2[0] + 7, lamp2[1] + 6, 8)]
    s.dusk(k=0.55, tint='#5a5a9a', sky=sky, lights=lights, windows=windows)
    s.walk(farY=206, nearY=248, farLeft=40, farRight=450, nearLeft=20, nearRight=460, farH=46, nearH=60)
    s.spot('LAMP', lamp, 350, 214)
    s.spot('BENCH', bench, 398, 222)
    return s.finish(out)


if __name__ == '__main__':
    print(build(sys.argv[1]))
