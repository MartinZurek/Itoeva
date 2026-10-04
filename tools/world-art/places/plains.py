"""PLAINS - die weite Ebene am spaeten Nachmittag: goldenes Gras bis zum Horizont, ein einzelner
Schirmbaum, Heuballen, ein Feldweg, ferne Tafelberge, hoher Himmel mit Wolkentuermen."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
from kit import Scene, W, H
from px import rgb, lerp, noise
import nature as N


def build(out):
    s = Scene('plains', horizon=150, seed=34)
    s.sky(top='#5a8ac8', mid='#a8c8e0', low='#f4e4c0', warm='#ffd8a0', clouds=False)
    s.clouds(n=6, ymin=14, ymax=90, lit='#fff4e0', mid='#f4e0c8', shade='#c8b8b8', base='#a8a0b0')
    s.hills([(220, 420, 128, '#a88a8a', '#d0b0a0', 71, 0.1), (0, 160, 134, '#9a8a90', '#c8aaa0', 72, 0.1)])
    s.grass(150, base=('#a8883a', '#c0a048', '#d4b458', '#e8cc70'), haze='#e8d8b0', flowers=False)
    # Grashalme in Wellen
    gn = noise(W, H, 12, 73, 3)
    blades = (s.yy > 152) & (gn > 0.5) & ((s.xx + (s.yy // 3)) % 3 == 0)
    s.paint(blades, '#8a6a2a')
    tips = (s.yy > 160) & (gn < 0.35) & ((s.xx * 7 + s.yy) % 5 == 0)
    s.paint(tips, '#f4e4a0')
    # Feldweg
    u, z = s.ground_uv()
    path = (s.yy >= 152) & (np.abs(u - 0.3 * np.sin(z * 0.7) + 0.1) < 0.22)
    s.cv.paint(path, lerp(rgb('#a07c50'), rgb('#c8a070'), noise(W, H, 2, 74, 2)[..., None]))
    s.paint(path & (noise(W, H, 1, 75, 1) > 0.75), '#7a5a38')
    # Schirmbaum
    s.trunk(300, 168, 120, 3, branches=True)
    crown = s.ellipse(300, 116, 46, 10)
    s.shade(crown, ['#2e4a2a', '#3e5e30', '#5a7a3a', '#7a9a4a'], light=(-0.6, -1), fur=0.4, seed=5)
    s.dapple(304, 170, 40, 6, seed=3)
    # Heuballen
    for x, y, r in ((110, 190, 14), (160, 178, 9), (400, 210, 20)):
        m = s.ellipse(x, y - r, r * 1.2, r)
        s.shadow(x + r * 0.4, y, r * 1.3, 3)
        s.shade(m, ['#8a6a2a', '#b08a3a', '#d0a850', '#ecc870'], seed=x)
        s.paint(s.ellipse(x - r * 0.5, y - r, r * 0.5, r * 0.8) & m, '#c89a48')
        s.paint(s.ellipse(x - r * 0.5, y - r, r * 0.25, r * 0.4) & m, '#a8802e')
    rng = np.random.default_rng(34)
    # Ferne Gehoefte, Baumgruppen, Windrad
    for x, y in ((60, 146), (400, 148)):
        s.house(x, y - 5, 8, 5, roof='#a84a36', wall='#e8d8b8', k=0.9, rng=rng)
        for k in range(3):
            N.leaf_mass(s.cv, x + 14 + k * 5, y - 2, 3.5, rng, cols=('#3a4a2a', '#4e5e30', '#66763a', '#7e8e48', '#98a058', '#b0b070'))
    s.paint(s.rect(452, 118, 453, 148), '#e8e0d0')
    for a in (0.3, 2.4, 4.5):
        s.paint(s.m().line([(452.5, 118), (452.5 + np.cos(a) * 10, 118 + np.sin(a) * 10)]).a > 0, '#f4f0e8')
    # Feldsteine, Disteln, Mohn und Kornblumen
    for x, y, rx in ((360, 236, 8), (60, 250, 11), (200, 196, 5)):
        N.stone(s.cv, x, y, rx, rx * 0.6, rng, moss='#8a8a4a')
    N.flowers(s.cv, 160, 268, rng, 120, 'poppy', patches=6)
    N.flowers(s.cv, 160, 268, rng, 90, 'bell', patches=5)
    N.grass_tufts(s.cv, 170, 270, rng, 300, cols=('#8a6a2a', '#a8883a', '#c8a850', '#e8d080'))
    for x, y in ((150, 60), (162, 64), (340, 44)):
        s.paint(s.m().line([(x - 3, y), (x, y + 1), (x + 3, y)]).a > 0, '#3a3a3a')
    seat = s.log_seat(150, 224, s=1.0, bark=('#4a3626', '#6a4c34', '#8a6646', '#a8805a'))
    s.walk(farY=192, nearY=248, farLeft=50, farRight=430, nearLeft=20, nearRight=450, farH=40, nearH=60)
    s.spot('BENCH', seat, 150, 232)
    return s.finish(out)


if __name__ == '__main__':
    print(build(sys.argv[1]))
