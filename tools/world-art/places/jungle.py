"""JUNGLE - der Dschungel: Wasserfall ueber moosige Felsen in einen gruenen Tuempel, Lianen,
riesige Blaetter, rote und gelbe Blueten, Dunst zwischen den Staemmen, ein Stamm am Ufer."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
from kit import Scene, W, H
from px import rgb, lerp, noise, glow


def big_leaf(s, x, y, ln, a, col=('#1e4a2a', '#2e6a34', '#4a8a3e', '#7ab050')):
    """Grosses Tropenblatt: gedrehte Ellipse mit Mittelrippe und Schlitzen."""
    cx, cy = x + np.cos(a) * ln * 0.5, y + np.sin(a) * ln * 0.5
    m = s.ellipse(cx, cy, ln * 0.5, ln * 0.28, rot=a)
    s.shade(m, list(col), light=(-0.6, -1))
    tip = (x + np.cos(a) * ln, y + np.sin(a) * ln)
    s.paint(s.m().line([(x, y), tip]).a > 0, col[3])
    for k in range(3, 9):
        t = k / 10
        bx, by = x + np.cos(a) * ln * t, y + np.sin(a) * ln * t
        for side in (1, -1):
            ex = bx + np.cos(a + side * 1.1) * ln * 0.28
            ey = by + np.sin(a + side * 1.1) * ln * 0.28
            s.paint((s.m().line([(bx + np.cos(a + side * 1.1) * ln * 0.12, by + np.sin(a + side * 1.1) * ln * 0.12), (ex, ey)]).a > 0) & m, col[0])
    s.outline(m, strength=0.4)


def build(out):
    s = Scene('jungle', horizon=150, seed=37)
    t = np.clip(s.yy / 200, 0, 1)
    s.cv.paint(s.yy < 210, lerp(rgb('#e0f0c8'), rgb('#6a9a5a'), t[..., None]))
    # Dunstige Kronen in Schichten, hinten blass
    haze = rgb('#d8ecc0')
    rr = np.random.default_rng(4)
    for layer, (fade, y0, y1, r0) in enumerate(((0.7, 60, 150, 26), (0.45, 80, 170, 32), (0.2, 100, 190, 30))):
        ramp = tuple('#%02x%02x%02x' % tuple(int(v * 255) for v in lerp(rgb(c), haze, fade))
                     for c in ('#1c3a22', '#2a5a2c', '#3e7a36', '#62a04a', '#9cc868'))
        for x in rr.uniform(-20, 500, 9 + layer * 2):
            if 190 < x < 300 and layer == 2:
                continue
            y = rr.uniform(y0, y1)
            s.paint(s.rect(int(x - 2 - layer), int(y), int(x + 2 + layer), 210),
                    lerp(rgb('#5a4a3a'), haze, fade))
            s.leafy(x, y, r0 * rr.uniform(0.8, 1.3), seed=int(x) + layer * 50, ramp_cols=ramp)
    # Felswand mit Wasserfall
    top = s.ridge(170, 320, 190, 40, 93, 0.3, scale=20)
    cliff = (s.yy >= top[None, :]) & (s.yy < 192)
    s.shade(cliff, ['#3a4a3a', '#4e5e48', '#66765a', '#86926e'], fur=0.3, seed=3)
    s.paint(cliff & (noise(W, H, 3, 91, 2) > 0.6), '#4e7a34')
    s.paint(cliff & (noise(W, H, 2, 94, 2) > 0.75), '#78a048')
    fall = s.poly([(228, 46), (254, 46), (262, 190), (220, 190)])
    s.paint(fall, '#d8f0f0')
    s.paint(fall & ((s.yy * 2 + s.xx) % 7 < 2), '#ffffff')
    s.paint(fall & ((s.yy + s.xx * 5) % 11 == 0), '#9ac8d0')
    for cx, cy, r in ((186, 70, 18), (306, 60, 20), (176, 150, 22), (318, 140, 20)):
        s.leafy(cx, cy, r, seed=cx)
    # Tuempel
    pool = s.ellipse(240, 200, 120, 14)
    s.paint(pool, '#3a8a7a')
    s.paint(pool & ((s.yy % 3) == 0) & (noise(W, H, 4, 92, 2) > 0.6), '#7ac8b8')
    s.paint(s.ellipse(240, 192, 30, 4), '#e8fafa')
    s.cv.add(glow(W, H, [(240, 186, 1.0)], 20, '#e8fff8', 0.25, steps=3))
    s.grass(206, base=('#2e5a2a', '#3a7030', '#4e863a', '#66a048'), haze='#a8c8a0', flowers=False)
    s.paint(pool & (s.yy >= 206), '#3a8a7a')
    # Lianen
    for x in (60, 110, 360, 420, 150):
        pts = [(x + np.sin(k * 0.6) * 4, k * 12) for k in range(0, 13)]
        s.paint(s.m().line(pts, 2).a > 0, '#3a5a2a')
        for qx, qy in pts[2::2]:
            s.paint(s.ellipse(qx + 3, qy, 3, 1.5), '#5a9a3a')
    # Riesenblaetter vorn und Blueten
    for x in (40, 150, 330, 450):
        s.bush(x, 214, 16, seed=x, cols=('#1e3a22', '#2a5a2c', '#3e7a36', '#62a04a', '#9cc868'))
    for x, y, a, ln in ((0, 210, -0.5, 80), (10, 272, -1.0, 90), (480, 220, -2.6, 80), (476, 272, -2.1, 96),
                        (80, 262, -1.5, 56), (410, 262, -1.7, 56)):
        big_leaf(s, x, y, ln, a)
    for x, y, c in ((70, 214, '#e84a3a'), (120, 230, '#f0c040'), (380, 220, '#e84a8a'), (420, 232, '#e84a3a')):
        for k in range(5):
            aa = k / 5 * 2 * np.pi
            s.paint(s.ellipse(x + np.cos(aa) * 3, y + np.sin(aa) * 3, 2.2, 2.2), c)
        s.paint(s.ellipse(x, y, 1.5, 1.5), '#fff0a0')
    seat = s.log_seat(330, 232, s=1.0)
    s.walk(farY=214, nearY=250, farLeft=70, farRight=410, nearLeft=40, nearRight=430, farH=44, nearH=60)
    s.spot('BENCH', seat, 330, 238)
    return s.finish(out)


if __name__ == '__main__':
    print(build(sys.argv[1]))
