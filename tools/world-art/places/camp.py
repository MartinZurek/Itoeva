"""CAMP - das Lager auf einer Waldlichtung bei Nacht: Zelt mit Licht, Lagerfeuer mit Steinkreis,
Baumstamm zum Sitzen, Tannen gegen den Sternenhimmel, Mond, Gluehwuermchen, Rucksack und Laterne."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
from kit import Scene, W, H
from px import rgb, lerp, glow


def build(out):
    s = Scene('camp', horizon=150, seed=35)
    s.sky(top='#0e1630', mid='#1e2c58', low='#3a4a78', warm='#5a5a88', clouds=False)
    rr = np.random.default_rng(3)
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
    s.campfire(256, 224, s=1.7)
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
