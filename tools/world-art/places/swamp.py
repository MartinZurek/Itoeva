"""SWAMP - der Sumpf in der Daemmerung: tuerkis-violetter Nebel, knorrige Baeume mit
Haengemoos, dunkles Wasser mit Seerosen und Spiegelungen, Schilf, Gluehwuermchen, leuchtende
Pilze, ein Bohlensteg und ein Stamm zum Sitzen."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
from kit import Scene, W, H
from px import rgb, lerp, noise


def gnarled(s, x, base, h, seed, col='#3a3428', moss='#6a8a5a', fade=0.0, haze='#8ab0a8'):
    rr = np.random.default_rng(seed)
    c = lerp(rgb(col), rgb(haze), fade)
    pts = [(x, base)]
    for k in range(1, 8):
        pts.append((pts[-1][0] + rr.uniform(-6, 6), base - h * k / 7))
    s.paint(s.m().line(pts, max(2, int(h / 14))).a > 0, c)
    s.paint(s.poly([(x - h * 0.12, base), (x, base - h * 0.2), (x + h * 0.12, base)]), c)
    for k in range(5):
        sx, sy = pts[3 + k % 4]
        a = rr.uniform(-2.8, -0.3)
        ex, ey = sx + np.cos(a) * h * 0.4, sy + np.sin(a) * h * 0.25
        s.paint(s.m().line([(sx, sy), ((sx + ex) / 2, (sy + ey) / 2 - 4), (ex, ey)], max(1, int(h / 30))).a > 0, c)
        for j in range(4):
            mx = ex - j * 3 + rr.uniform(-2, 2)
            s.paint(s.m().line([(mx, ey), (mx + rr.uniform(-1, 1), ey + rr.uniform(6, 16))]).a > 0,
                    lerp(rgb(moss), rgb(haze), fade))
    crown = s.ellipse(pts[-1][0], pts[-1][1], h * 0.35, h * 0.14)
    s.shade(crown, ['#%02x%02x%02x' % tuple(int(v * 255) for v in lerp(rgb(q), rgb(haze), fade))
                    for q in ('#22322a', '#2e4434', '#405a40', '#5a7650')], fur=0.5, seed=seed)


def build(out):
    s = Scene('swamp', horizon=150, seed=38)
    s.sky(top='#2a3a5a', mid='#6a6a8a', low='#c8a8a0', warm='#e8b890', clouds=False)
    for k, (fade, base, h, n) in enumerate(((0.7, 150, 60, 6), (0.45, 156, 80, 5))):
        rr = np.random.default_rng(20 + k)
        for x in rr.uniform(0, W, n):
            gnarled(s, x, base, h * rr.uniform(0.8, 1.2), seed=int(x) + k, fade=fade)
    s.water(150, H, deep='#1e2e34', mid='#2e464a', light='#5a7a78', sky='#a89aa0')
    s.reflect(150, 70, k=0.5, tint='#2e3e48')
    s.cv.c[150] = s.cv.c[151]
    # Nebelbaender
    for y in (150, 170):
        band = (np.abs(s.yy - y - (noise(W, 4, 30, 40 + y, 2)[1][None, :] - 0.5) * 8) < 5)
        s.cv.c[band] = lerp(s.cv.c[band], rgb('#c8d0d0'), 0.35)
    # Inseln mit Schilf, Steg
    for cx, cy, rx in ((80, 214, 90), (380, 222, 110)):
        isl = s.ellipse(cx, cy, rx, 16)
        s.shade(isl, ['#2a3a24', '#36482a', '#465a32', '#5a6e3c'], seed=cx)
    s.reeds(0, 150, 200, 226, seed=3, col='#5a6a3a', head='#4a3020')
    s.reeds(300, 480, 206, 236, seed=4, col='#5a6a3a', head='#4a3020')
    deck = s.poly([(150, 248), (300, 232), (304, 238), (154, 256)])
    s.shade(deck, ['#4a3828', '#5e4632', '#76583e', '#8e6c4c'], light=(-0.4, -1))
    for x in range(156, 300, 8):
        s.paint(s.m().line([(x, 255 - (x - 150) * 0.11), (x, 262 - (x - 150) * 0.11)]).a > 0, '#3a2a1e')
    gnarled(s, 440, 226, 150, seed=9)
    gnarled(s, 24, 222, 130, seed=10)
    for x, y in ((60, 208), (66, 210), (400, 214), (100, 212)):
        s.mushroom(x, y, s=1.2, cap='#6ac8c8', glow_c='#6af0e0')
    # Seerosen
    for x, y in ((200, 196), (240, 204), (330, 190), (180, 220), (270, 214)):
        s.paint(s.ellipse(x, y, 7, 2.2), '#3e6a3a')
        s.paint(s.ellipse(x - 1, y - 0.5, 5, 1.4), '#5a8a4a')
        s.paint(s.rect(x, y - 1, x + 1, y - 1), '#f4c8d8')
    bank = s.yy >= 244 + (noise(W, 4, 30, 55, 2)[1][None, :] - 0.5) * 6
    s.cv.paint(bank, lerp(rgb('#2e3a26'), rgb('#4a5a34'), noise(W, H, 3, 56, 2)[..., None]))
    s.paint(bank & ~np.roll(bank, 1, axis=0), '#6a7a44')
    s.reeds(0, 130, 246, 270, seed=5, col='#4a5a30', head='#4a3020')
    seat = s.log_seat(370, 222, s=1.0)
    s.fireflies(12, 20, 460, 110, 230, seed=8, col='#d0f080')
    s.walk(farY=210, nearY=250, farLeft=60, farRight=420, nearLeft=30, nearRight=440, farH=44, nearH=58)
    s.spot('BENCH', seat, 370, 228)
    return s.finish(out)


if __name__ == '__main__':
    print(build(sys.argv[1]))
