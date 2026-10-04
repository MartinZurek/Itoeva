"""BEACH - Sandstrand am Mittag: tuerkises Meer mit Wellenkaemmen und Gischt, Felsinsel mit
Leuchtturm, Palmen, Strandhuette aus Holz, Muscheln, Treibholz zum Sitzen."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
from kit import Scene, W, H
from px import rgb, lerp, noise, dramp
import nature as N


def build(out):
    s = Scene('beach', horizon=118, seed=36)
    s.sky(top='#3a8ad8', mid='#7ab8e8', low='#d8f0f4', warm='#fff4e0')
    s.water(118, 206, deep='#2a8aa8', mid='#3aa8b8', light='#7ad0d0', sky='#c8eef0')
    # Felsinsel mit Leuchtturm
    s.shade(s.ellipse(380, 120, 30, 9) & (s.yy <= 120), ['#5a5248', '#7a7064', '#9a9080', '#bcb09c'])
    s.paint(s.rect(380, 90, 386, 116), '#f4f0e8')
    for y in (96, 104):
        s.paint(s.rect(380, y, 386, y + 3), '#d8504a')
    s.paint(s.rect(379, 86, 387, 90), '#3a3a3a')
    s.paint(s.rect(381, 84, 385, 87), '#ffe890')
    s.reflect(118, 30)
    # Wellen, die nach vorn breiter werden, und Gischt an der Kante
    rng = np.random.default_rng(36)
    for k, y in enumerate((128, 136, 146, 158, 172, 186)):
        wn = noise(W, 4, 20, 80 + k, 3)[1]
        yy_ = y + (wn[None, :] - 0.5) * (3 + k)
        crest = (np.abs(s.yy - yy_) < 0.5 + k * 0.35) & (s.yy > 118)
        broken = noise(W, H, 3 + k, 90 + k, 2) > 0.42
        s.paint(crest & broken, '#e8fafa')
        trough = (s.yy > yy_ + 1) & (s.yy < yy_ + 2 + k * 0.6) & broken
        s.cv.c[trough] *= 0.9
    # Segelboot und Moewen
    s.paint(s.poly([(150, 124), (166, 124), (163, 127), (153, 127)]), '#6a4a3a')
    s.paint(s.poly([(158, 123), (158, 106), (166, 122)]), '#f8f4ec')
    s.paint(s.poly([(157, 123), (157, 110), (151, 122)]), '#e8d8c0')
    for x, y in ((220, 40), (232, 46), (300, 30)):
        s.paint(s.m().line([(x - 4, y), (x, y + 2), (x + 4, y)]).a > 0, '#f4f4f4')
    sand_top = 196 + (noise(W, 4, 40, 85, 2)[1] - 0.5) * 8
    sand = s.yy >= sand_top[None, :]
    t = np.clip((s.yy - 196) / 74, 0, 1)
    sn = noise(W, H, 2, 86, 2)
    s.cv.paint(sand, dramp([(0, '#c8b088'), (0.3, '#e0c89a'), (1, '#f4e2b4')], np.clip(t * 0.7 + sn * 0.3, 0, 1), 5))
    foam = (np.abs(s.yy - sand_top[None, :]) < 1.6)
    s.paint(foam, '#ffffff')
    wet = (s.yy > sand_top[None, :] + 1.5) & (s.yy < sand_top[None, :] + 6)
    s.cv.paint(wet, s.cv.c * 0.86)
    s.cv.paint(sand & (sn > 0.8) & ((s.xx + s.yy) % 3 == 0), rgb('#c8a878'))
    # Strandhuette links
    s.paint(s.rect(20, 150, 100, 204), '#8a6a48')
    s.shade(s.rect(24, 156, 96, 200), ['#7a5a3a', '#9a744a', '#b88e5c', '#d0a870'])
    for x in range(26, 96, 6):
        s.paint(s.rect(x, 156, x, 200), '#6a4a30')
    s.paint(s.rect(48, 176, 64, 200), '#3a2a20')
    roof = s.poly([(8, 156), (60, 128), (112, 156)])
    s.shade(roof, ['#8a7040', '#aa8a50', '#c8a864', '#e0c47a'], light=(-0.6, -1))
    for x in (24, 96):
        s.paint(s.rect(x, 200, x + 3, 212), '#5a3e28')
    # Fischernetz und Surfbrett an der Huette, Steine mit Gezeitentuempel
    net = s.rect(100, 170, 118, 200)
    s.paint(net & (((s.xx + s.yy) % 4 == 0) | ((s.xx - s.yy) % 4 == 0)), '#c8b898')
    s.paint(s.ellipse(124, 190, 3.5, 14), '#e8603a')
    s.paint(s.ellipse(124, 190, 1, 13), '#f4f0e0')
    for x, y, rx in ((40, 230, 16), (62, 236, 10), (20, 244, 12), (440, 214, 9)):
        N.stone(s.cv, x, y, rx, rx * 0.6, rng, cols=('#3e3a36', '#5a5650', '#7a746a', '#9a9488', '#bcb6a8'), wet=True, moss='#4a6a3a')
    s.paint(s.ellipse(48, 240, 10, 2.2), '#5a9ab0')
    s.paint(s.ellipse(46, 239, 6, 1), '#a8d8e8')
    # Duenengras
    N.grass_tufts(s.cv, 236, 270, rng, 70, cols=('#6a7a3a', '#8a9a48', '#a8b05a', '#c8c878'), x0=360, x1=480, scale_y=lambda y: 2.0)
    N.grass_tufts(s.cv, 240, 270, rng, 40, cols=('#6a7a3a', '#8a9a48', '#a8b05a', '#c8c878'), x0=0, x1=90, scale_y=lambda y: 2.0)
    # Sandburg und Eimer
    s.paint(s.poly([(244, 258), (264, 258), (262, 248), (246, 248)]), '#d8bc88')
    for x in (246, 255, 262):
        s.paint(s.rect(x - 2, 242, x + 1, 248), '#e0c494')
        s.paint(s.rect(x - 2, 241, x - 2, 241), '#e0c494')
    s.paint(s.m().line([(255, 242), (255, 234)]).a > 0, '#6a4a3a')
    s.paint(s.poly([(255, 234), (261, 236), (255, 238)]), '#e8403a')
    s.paint(s.poly([(272, 258), (282, 258), (283, 248), (271, 248)]), '#3a8ae8')
    s.paint(s.rect(271, 248, 283, 249), '#5aa8f8')
    # Palmen
    s.palm(140, 214, 110, seed=1, lean=0.35)
    s.palm(440, 236, 140, seed=2, lean=-0.3)
    # Muscheln und Seesterne
    for _ in range(40):
        x, y = rng.uniform(10, 470), rng.uniform(206, 268)
        c = ['#f4c8b0', '#f4f0e0', '#e88a6a', '#f4e0c8', '#c8a0c0'][int(rng.integers(0, 5))]
        sz = 0.8 + (y - 206) / 40
        s.paint(s.ellipse(x, y, 1.6 * sz, 1.0 * sz), c)
        s.paint(s.rect(int(x), int(y), int(x), int(y)), lerp(rgb(c), rgb('#ffffff'), 0.4))
    for x, y in ((180, 246), (360, 258)):
        for k in range(5):
            a = k / 5 * 2 * np.pi - np.pi / 2
            s.paint(s.m().line([(x, y), (x + np.cos(a) * 4, y + np.sin(a) * 2.5)], 2).a > 0, '#e8703a')
    seat = s.log_seat(320, 238, s=1.0, bark=('#8a7a68', '#a89880', '#c4b49a', '#dccaa8'))
    s.walk(farY=206, nearY=250, farLeft=30, farRight=430, nearLeft=20, nearRight=440, farH=44, nearH=60)
    s.spot('BENCH', seat, 320, 244)
    return s.finish(out)


if __name__ == '__main__':
    print(build(sys.argv[1]))
