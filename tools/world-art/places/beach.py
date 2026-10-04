"""BEACH - Sandstrand am Mittag: tuerkises Meer mit Wellenkaemmen und Gischt, Felsinsel mit
Leuchtturm, Palmen, Strandhuette aus Holz, Muscheln, Treibholz zum Sitzen."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
from kit import Scene, W, H
from px import rgb, lerp, noise, dramp


def build(out):
    s = Scene('beach', horizon=118, seed=36)
    s.sky(top='#3a8ad8', mid='#7ab8e8', low='#d8f0f4', warm='#fff4e0')
    s.water(118, 196, deep='#2a8aa8', mid='#3aa8b8', light='#7ad0d0', sky='#c8eef0')
    # Felsinsel mit Leuchtturm
    s.shade(s.ellipse(380, 120, 30, 9) & (s.yy <= 120), ['#5a5248', '#7a7064', '#9a9080', '#bcb09c'])
    s.paint(s.rect(380, 90, 386, 116), '#f4f0e8')
    for y in (96, 104):
        s.paint(s.rect(380, y, 386, y + 3), '#d8504a')
    s.paint(s.rect(379, 86, 387, 90), '#3a3a3a')
    s.paint(s.rect(381, 84, 385, 87), '#ffe890')
    s.reflect(118, 30)
    # Wellen, die nach vorn breiter werden, und Gischt an der Kante
    for k, y in enumerate((140, 156, 172, 186)):
        wn = noise(W, 4, 20, 80 + k, 3)[1]
        crest = (np.abs(s.yy - (y + (wn[None, :] - 0.5) * 6)) < 0.6 + k * 0.3) & (s.yy > 118)
        s.paint(crest, '#e8fafa')
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
    # Palmen
    s.palm(140, 214, 110, seed=1, lean=0.35)
    s.palm(440, 236, 140, seed=2, lean=-0.3)
    # Muscheln und Seesterne
    for x, y, c in ((200, 236, '#f4c8b0'), (232, 250, '#f4f0e0'), (300, 230, '#e88a6a'), (90, 244, '#f4e0c8')):
        s.paint(s.ellipse(x, y, 2.5, 1.6), c)
    seat = s.log_seat(320, 238, s=1.0, bark=('#8a7a68', '#a89880', '#c4b49a', '#dccaa8'))
    s.walk(farY=206, nearY=250, farLeft=30, farRight=430, nearLeft=20, nearRight=440, farH=44, nearH=60)
    s.spot('BENCH', seat, 320, 244)
    return s.finish(out)


if __name__ == '__main__':
    print(build(sys.argv[1]))
