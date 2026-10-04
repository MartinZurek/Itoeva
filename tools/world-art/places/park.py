"""PARK - nach world-studies/park-sports-ground (linke Haelfte): grosser Baum mit Bank und
Laterne, Plattenweg in die Tiefe, Holzzaun, Stadt mit Turm und Steinbruecke, Teich mit Enten."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
from kit import Scene, W, H
from px import rgb
import nature as N
import town as T


def build(out):
    s = Scene('park', horizon=124, seed=11)
    s.sky(top='#6aa4dc', mid='#9cc8ea', low='#e6f0ee', warm='#f8e8c8')
    # Drachen am Himmel
    k = s.poly([(352, 40), (360, 32), (368, 40), (360, 50)])
    s.paint(k, '#e86a50')
    s.paint(s.poly([(352, 40), (360, 32), (360, 50)]), '#f0c040')
    s.paint(s.m().line([(360, 50), (356, 62), (362, 74), (356, 86)]).a > 0, '#6a5a4a')
    # Ferne
    s.hills([(0, 480, 96, '#8aa0c8', '#b8c8e0', 3, 0.4), (140, 480, 106, '#7a96b4', '#a8bcd4', 5, 0.3)])
    s.town(170, 330, 98, 124, n=70, k=0.92, tower=(236, 86, 22))
    # Steinbruecke mit Boegen in der Mitte
    bridge = s.rect(220, 124, 340, 131)
    s.paint(bridge, '#d8ccb6')
    for x in range(226, 340, 18):
        s.paint(s.ellipse(x + 6, 131, 6, 5) & (s.yy >= 128), '#8aa0b8')
    s.paint(s.rect(220, 124, 340, 124), '#f0e6d4')
    s.grass(124)
    s.treeline(138, 0, 480, '#3e6440', '#7aa05a', 7, height=14)
    # Ferne Baeume und Busch hinten
    for x, h in ((300, 46), (420, 56), (460, 40)):
        s.tree(x, 150, h, seed=x, kind='round')
    s.tree(150, 146, 34, seed=4, kind='round')
    # Hecke und Buesche entlang des Zauns, Zaun davor
    for x in range(130, 480, 26):
        s.bush(x, 168, 11 + (x * 7) % 6, seed=x, flowers=('#f4f0e0', '#e88aa0') if x % 3 == 0 else None)
    s.fence(170, 120, 480, s=0.7)
    # Plattenweg von vorn in die Tiefe
    s.stone_path([(0.05, 1.0), (0.1, 1.5), (0.35, 2.2), (0.6, 3.2), (0.8, 5)], 0.42)
    # Teich vorn links mit Steinrand und Enten
    pond = s.ellipse(80, 266, 120, 26)
    s.paint(pond, '#4a8ab0')
    t = np.clip((s.yy - 240) / 30, 0, 1)
    s.cv.paint(pond, np.array([0.40, 0.62, 0.74]) * (1 - t[..., None] * 0.3))
    rim = pond & ~np.roll(pond, 2, axis=0)
    s.paint(rim, '#f0f8fa')
    rng = np.random.default_rng(11)
    x = -30
    while x < 210:
        rx = rng.uniform(4, 9)
        yv = 266 - 26 * np.sqrt(max(0, 1 - ((x - 80) / 120) ** 2))
        N.stone(s.cv, x, yv + 1, rx, rx * rng.uniform(0.45, 0.7), rng, moss='#6a8a3a' if rng.random() < 0.4 else None, wet=True)
        x += rx * rng.uniform(1.3, 1.9)
    s.reflect(242, 20, y_end=266, k=0.35)
    for x, y, flip in ((60, 258, 1), (78, 262, 1), (110, 256, -1)):
        body = s.ellipse(x, y, 3.5, 2)
        s.paint(body, '#f4f0e8')
        s.paint(body & (s.yy > y), '#c8c4bc')
        s.paint(s.ellipse(x + 3 * flip, y - 2.5, 1.6, 1.6), '#f4f0e8')
        s.paint(s.rect(x + 5 * flip, y - 3, x + 5 * flip, y - 2), '#f0a030')
        s.paint(s.rect(x + 3 * flip, y - 3, x + 3 * flip, y - 3), '#1a1a1a')
        s.paint(s.ellipse(x, y + 2, 5, 0.8), '#7ab0d0')
    # Grosser Baum links als Rahmen, Schattenflecken auf der Wiese darunter
    s.dapple(110, 212, 110, 18, 33)
    N.oak(s.cv, 48, 218, 260, np.random.default_rng(7), spread=1.0, lean=0.12)
    s.tree(250, 180, 64, seed=9, kind='round')
    # Laterne und Bank unter dem Baum
    T.ornate_lamp(s, 176, 200, h=50, banner='#c8644a')
    bench = s.bench(118, 204, s=0.95)
    s.flowerbed(196, 300, 212, s=1.1, seed=5)
    s.bush(206, 200, 9, seed=71, flowers=('#f4f0e0', '#f0c850'))
    s.bush(320, 206, 12, seed=72, flowers=('#e88aa0', '#f4f0e0'))
    s.bush(440, 214, 14, seed=73)
    s.flowerbed(330, 490, 268, s=1.5, seed=6, colors=('#9a88e0', '#b8a0f0', '#f4f0e0', '#7a68c0'))
    # Seerosen und Schilf am Teich
    for x, y in ((40, 262), (130, 258), (150, 266)):
        s.paint(s.ellipse(x, y, 4, 1.6), '#4e8a46')
        s.paint(s.rect(x - 1, y - 1, x, y - 1), '#f4c0d0')
    s.reeds(146, 204, 244, 262, seed=8, col='#5a7a3a', head='#7a5a3a', density=2)
    N.flowers(s.cv, 214, 268, rng, 40, 'lupine', x0=200, x1=330, patches=3)
    N.flowers(s.cv, 214, 268, rng, 60, 'daisy', x0=200, x1=480, patches=4)
    s.foliage('br', 70, 81)
    s.foliage('bl', 40, 82)
    s.walk(farY=200, nearY=240, farLeft=96, farRight=470, nearLeft=40, nearRight=440, farH=40, nearH=58)
    s.spot('BENCH', bench, 118, 214)
    return s.finish(out)


if __name__ == '__main__':
    print(build(sys.argv[1]))
