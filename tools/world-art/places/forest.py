"""FOREST - der Nadel- und Mischwald nach world-studies/woodland-lake: Reihen von Tannen, die
nach hinten im warmen Dunst verblassen, Lichtstrahlen, ein Erdpfad in die Tiefe, Farne, Pilze,
Moosfelsen und ein umgefallener Stamm zum Sitzen."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
from kit import Scene, W, H
from px import rgb, lerp, noise, dramp


def build(out):
    s = Scene('forest', horizon=150, seed=32)
    s.sky(top='#9ac4d8', mid='#d0e4d8', low='#f4ecc8', warm='#ffe8b0', clouds=False)
    haze = rgb('#d8e4c0')
    # Tannenreihen, hinten blass, vorn satt
    s.paint(s.rect(0, 118, W, 160), lerp(rgb('#5a6e48'), haze, 0.5))
    rows = [(124, 26, 0.72, 8), (134, 36, 0.52, 11), (146, 50, 0.32, 19), (160, 78, 0.1, 47)]
    for k, (base, h, fade, step) in enumerate(rows):
        rr = np.random.default_rng(10 + k)
        for x in range(-10, 490, step):
            xx = x + rr.uniform(-3, 3)
            hh = h * rr.uniform(0.75, 1.15)
            crown = tuple('#%02x%02x%02x' % tuple(int(v * 255) for v in lerp(rgb(c), haze, fade))
                          for c in ('#1e3a32', '#2c5040', '#3e6a4e', '#5a8a5e'))
            s.tree(xx, base + rr.uniform(0, 4), hh, seed=x + k, kind='pine', crown=crown)
    # Waldboden
    s.grass(158, base=('#3a4e26', '#4a602c', '#5e7636', '#768c44'), haze='#b8c49a', flowers=False)
    u, z = s.ground_uv()
    path = (s.yy >= 158) & (np.abs(u - 0.25 * np.sin(z * 0.9)) < 0.28)
    pn = noise(W, H, 3, 77, 2)
    s.cv.paint(path, dramp([(0, '#6a4a2e'), (0.5, '#8a6440'), (1, '#a88052')], pn, 4))
    leaves = path & (noise(W, H, 1, 78, 1) > 0.72)
    s.paint(leaves, '#c8783a')
    # Lichtstrahlen
    for x0 in (170, 240, 300):
        beam = s.poly([(x0, 0), (x0 + 12, 0), (x0 + 80, 230), (x0 + 50, 230)])
        s.cv.c[beam] = lerp(s.cv.c[beam], rgb('#fff4c8'), 0.16)
    # Zwei grosse Staemme vorn mit Krone oben aus dem Bild
    for x, w in ((34, 9), (452, 11)):
        s.trunk(x, 262, -20, w, branches=False)
    s.foliage('bl', 70, 51, cols=('#1a2e22', '#24402c', '#36583a', '#4e7646', '#6e9452'))
    s.foliage('br', 80, 52, cols=('#1a2e22', '#24402c', '#36583a', '#4e7646', '#6e9452'))
    for cx, cy, r in ((40, -6, 50), (450, -10, 60)):
        s.leafy(cx, cy, r, seed=cx, ramp_cols=('#1a2e22', '#24402c', '#36583a', '#4e7646', '#6e9452'))
    # Farne, Pilze, Moosfelsen
    for x in range(14, 470, 31):
        y = 200 + (x * 13) % 44
        if abs(x - 240) < 40:
            continue
        for a in np.linspace(-2.7, -0.45, 7):
            s.paint(s.m().line([(x, y), (x + np.cos(a) * 12, y + np.sin(a) * 8 - 2), (x + np.cos(a) * 16, y + np.sin(a) * 6)], 2).a > 0, '#4e7a3a')
            s.paint(s.m().line([(x + np.cos(a) * 8, y + np.sin(a) * 6), (x + np.cos(a) * 14, y + np.sin(a) * 6)]).a > 0, '#7aa050')
    for x, y in ((120, 226), (128, 230), (350, 236), (196, 210)):
        s.mushroom(x, y, s=1.4)
    s.rock(300, 214, 14, 9, moss='#6a9a4a')
    s.rock(90, 206, 10, 7, moss='#6a9a4a', seed=2)
    seat = s.log_seat(370, 228, s=1.1)
    s.fireflies(6, 160, 330, 140, 200, seed=6, col='#f0f0a0')
    s.walk(farY=196, nearY=248, farLeft=60, farRight=420, nearLeft=30, nearRight=440, farH=42, nearH=60)
    s.spot('BENCH', seat, 370, 234)
    return s.finish(out)


if __name__ == '__main__':
    print(build(sys.argv[1]))
