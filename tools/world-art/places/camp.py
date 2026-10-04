"""CAMP - das Lager auf einer Waldlichtung bei Nacht: Zelt mit Licht, Lagerfeuer mit Steinkreis,
Baumstamm zum Sitzen, Tannen gegen den Sternenhimmel, Mond, Gluehwuermchen, Rucksack und Laterne."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
from kit import Scene, W, H
from px import rgb, lerp, glow, noise
import nature as N


def build(out):
    s = Scene('camp', horizon=150, seed=35)
    s.sky(top='#0e1630', mid='#1e2c58', low='#3a4a78', warm='#5a5a88', clouds=False)
    rr = np.random.default_rng(3)
    # Milchstrasse als schraeges Band mit vielen kleinen Sternen
    band = np.abs((s.yy - 20) - (s.xx - 100) * 0.35) < 22
    mw = band & (noise(W, H, 6, 41, 3) > 0.5) & (s.yy < 130)
    s.cv.c[mw] = lerp(s.cv.c[mw], rgb('#4a5a8a'), 0.35)
    for _ in range(260):
        x, y = int(rr.uniform(0, W)), int(rr.uniform(0, 125))
        if band[y, x] or rr.random() < 0.3:
            s.cv.c[y, x] = lerp(s.cv.c[y, x], rgb('#e0e8ff'), rr.uniform(0.3, 0.9))
    for _ in range(120):
        x, y = int(rr.uniform(0, W)), int(rr.uniform(0, 120))
        s.cv.c[y, x] = rgb(['#d8e0ff', '#ffffff', '#ffe8c0'][int(rr.integers(0, 3))])
    s.cv.add(glow(W, H, [(380, 40, 1.0)], 14, '#c8d8ff', 0.3, steps=3))
    s.paint(s.ellipse(380, 40, 8, 8), '#f4f0d8')
    s.paint(s.ellipse(377, 38, 2, 2), '#d8d4c0')
    s.paint(s.rect(0, 128, W, 156), '#16222a')
    for k, (base, h, step) in enumerate(((140, 40, 9), (150, 60, 13))):
        for x in range(-10, 490, step):
            s.tree(x + (x * 7) % 5, base + (x * 3) % 6, h * (0.8 + 0.4 * ((x * 13) % 7) / 7), seed=x + k * 100,
                   kind='pine', crown=('#0e1a22', '#16262e', '#22363a', '#2e4644'))
    s.grass(152, base=('#1e2e26', '#26382a', '#304430', '#3c5236'), haze='#2a3a4a', flowers=False)
    s.tent(130, 200, w=90, h=58, cloth=('#5a3a2a', '#7a5032', '#9a683e', '#b8844e'))
    # Schlafsack im Zelt, Flicken, Wimpel an der Stange
    s.paint(s.poly([(124, 200), (128, 186), (134, 186), (138, 200)]), '#3a5a8a')
    s.paint(s.rect(150, 176, 158, 182), '#9a5a3a')
    s.paint(s.rect(150, 176, 158, 176), '#c88a5a')
    s.paint(s.poly([(130, 136), (142, 139), (130, 142)]), '#e8b440')
    s.campfire(256, 224, s=1.7)
    # Dreibein mit Kessel ueber dem Feuer, Holzstapel, Becher
    for dx in (-14, 0, 14):
        s.paint(s.m().line([(256 + dx, 226), (256, 186)]).a > 0, '#4a3020')
    s.paint(s.m().line([(256, 186), (256, 196)]).a > 0, '#2a2a2a')
    pot = s.ellipse(256, 201, 6, 5)
    s.shade(pot, ['#1a1a1e', '#2e2e34', '#4a4a52', '#6a6a74'])
    s.paint(s.ellipse(256, 197, 5, 1.2), '#5a3a20')
    for k in range(4):
        s.paint(s.rect(255 + (k % 2), 190 - k * 2, 255 + (k % 2), 190 - k * 2), '#8a8a9a')
    for k in range(6):
        y = 238 - (k // 3) * 5
        x = 300 + (k % 3) * 9 + (k // 3) * 4
        s.paint(s.rect(x, y - 4, x + 8, y), '#6a4a30')
        s.paint(s.ellipse(x + 8, y - 2, 2, 2), '#c89a68')
        s.paint(s.rect(x + 8, y - 2, x + 8, y - 2), '#8a6040')
    for x, c in ((226, '#c84a3a'), (284, '#3a6aa8')):
        s.paint(s.rect(x, 232, x + 3, 236), c)
        s.paint(s.rect(x + 4, 233, x + 4, 234), c)
    seat = s.log_seat(330, 214, s=1.0)
    s.log_seat(200, 244, s=0.9)
    # Rucksack und Laterne
    bag = s.ellipse(80, 220, 9, 11)
    s.shade(bag, ['#3a4a2a', '#4e6236', '#667a44', '#7e9256'])
    s.paint(s.rect(76, 214, 84, 216), '#8a6a3a')
    s.paint(s.rect(402, 214, 408, 224), '#2a2a2a')
    s.paint(s.rect(403, 216, 407, 222), '#ffd890')
    s.cv.add(glow(W, H, [(405, 219, 1.0)], 8, '#ffa850', 0.4, steps=3))
    s.fireflies(14, 20, 460, 120, 200, seed=7, col='#e0f080')
    s.foliage('bl', 60, 75, cols=('#0a1410', '#101e18', '#182a20', '#223628', '#2e4432'))
    s.foliage('br', 60, 76, cols=('#0a1410', '#101e18', '#182a20', '#223628', '#2e4432'))
    s.walk(farY=196, nearY=248, farLeft=50, farRight=430, nearLeft=20, nearRight=450, farH=42, nearH=60)
    s.spot('BENCH', seat, 330, 222)
    return s.finish(out)


if __name__ == '__main__':
    print(build(sys.argv[1]))
