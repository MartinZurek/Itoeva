"""MEADOW - die Blumenwiese hinter dem Park: sanfte Huegel voller Wildblumen im Sommerlicht,
Trampelpfad in die Tiefe, Waldrand und blaue Berge, einzelne Eiche mit Baumstamm zum Sitzen,
Schmetterlinge."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
from kit import Scene, W, H
from px import rgb


def build(out):
    rng = np.random.default_rng(31)
    s = Scene('meadow', horizon=130, seed=31)
    s.sky(top='#5a9ad8', mid='#94c4ea', low='#e4f0ee', warm='#f8ecd0')
    s.hills([(0, 480, 72, '#8aa0c8', '#c0d0e4', 41, 0.45), (180, 480, 92, '#7a96b0', '#a8bcd0', 42, 0.35)])
    s.hills([(0, 260, 104, '#6a8a6a', '#8aaa7a', 43, 0.2), (220, 480, 112, '#5a7e5a', '#7a9e6a', 44, 0.2)])
    s.treeline(132, 0, 480, '#3e6440', '#7aa05a', 45, height=14)
    s.grass(130, base=('#4e7a34', '#6a9a3c', '#86b04a', '#a4c45e'))
    # Trampelpfad
    s.stone_path([(0.15, 6.0), (-0.1, 3.0), (0.25, 1.6), (0.05, 1.0)], 0.22,
                 tone=('#9a8a6a', '#b4a07c', '#ccb890', '#e0d0a8'))
    # Bluetenteppich in Gruppen, wie Wildblumen wachsen
    import nature as N
    for kind, n, p in (('daisy', 260, 10), ('buttercup', 200, 8), ('poppy', 90, 6), ('lupine', 60, 5), ('bell', 90, 6)):
        N.flowers(s.cv, 136, 266, rng, n, kind, patches=p)
    for x in (60, 150, 330, 420):
        s.bush(x, 150 + (x % 7), 9, seed=x, flowers=('#f4f0e0', '#e88aa0'))
    # Eiche rechts mit Baumstamm darunter
    s.tree(360, 206, 130, seed=47, kind='round')
    seat = s.log_seat(330, 214, s=1.0)
    s.flowerbed(-20, 140, 270, s=1.4, seed=48)
    # Schmetterlinge
    for x, y, c in ((200, 150, '#f0c850'), (250, 170, '#f4f0e0'), (120, 182, '#e88aa0')):
        s.paint(s.rect(x - 2, y, x - 1, y + 1), c)
        s.paint(s.rect(x + 1, y, x + 2, y + 1), c)
        s.paint(s.rect(x, y, x, y + 1), '#3a2a20')
    s.foliage('br', 70, 49)
    s.walk(farY=196, nearY=248, farLeft=50, farRight=430, nearLeft=20, nearRight=440, farH=42, nearH=60)
    s.spot('BENCH', seat, 330, 222)
    return s.finish(out)


if __name__ == '__main__':
    print(build(sys.argv[1]))
