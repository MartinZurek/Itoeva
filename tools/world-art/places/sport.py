"""SPORT - nach world-studies/park-sports-ground (rechte Haelfte): Basketballfeld in Perspektive
mit Korb, Steinmauer mit Blumenkaesten, Treppe hinauf zum Park, Laternen, Baeume."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
from kit import Scene, W, H
from px import rgb, lerp, noise
import nature as N
import town as T


def build(out):
    s = Scene('sport', horizon=118, seed=12)
    s.sky(top='#6aa4dc', mid='#9cc8ea', low='#e6f0ee', warm='#f8e8c8')
    s.hills([(0, 480, 92, '#8aa0c8', '#b8c8e0', 13, 0.4)])
    s.town(260, 470, 94, 118, n=50, k=0.92, tower=(380, 82, 20))
    s.grass(118)
    s.treeline(132, 0, 480, '#3e6440', '#7aa05a', 17, height=16)
    for x, h in ((60, 60), (130, 50), (410, 66), (470, 52)):
        s.tree(x, 150, h, seed=x + 5, kind='round')
    # Steinmauer hinten mit Blumenkaesten, Treppe links hinauf
    s.stone_wall(150, 170, 150, 480)
    for x in range(160, 470, 46):
        s.planter(x, 150, 30, s=0.8)
    # Ranken an der Mauer
    vine = (s.yy >= 150) & (s.yy <= 166) & (s.xx >= 150) & (np.abs(np.sin(s.xx * 0.21) * 6 + 152 - s.yy) < 3 + (s.xx % 7 == 0) * 6)
    s.paint(vine & (np.random.default_rng(3).random((H, W)) > 0.35), '#4e7a3a')
    for x in range(156, 480, 24):
        s.bush(x, 172, 6, seed=x, flowers=('#f4f0e0', '#e88aa0') if x % 48 == 0 else None)
    for k in range(6):
        y = 170 - k * 4
        x0 = 96 + k * 6
        s.paint(s.rect(x0, y - 4, x0 + 50, y), '#c8bca8')
        s.paint(s.rect(x0, y - 4, x0 + 50, y - 4), '#ece2d0')
        s.paint(s.rect(x0, y, x0 + 50, y), '#8a8070')
    # Spielfeld in Perspektive: rostrot mit weissen Linien
    court = s.ground_poly([(-1.0, 1.0), (1.05, 1.0), (1.05, 2.9), (-1.0, 2.9)])
    u, z = s.ground_uv()
    col = lerp(np.array([0.70, 0.40, 0.32]), np.array([0.80, 0.52, 0.40]), np.clip((z - 1) / 2, 0, 1)[..., None])
    # Belag mit Abrieb, Flicken, Rissen, Laub und Kreide
    wear = noise(W, H, 8, 51, 3)
    col = col * (0.9 + 0.2 * wear[..., None])
    scuff = (np.abs(u - 0.02) < 0.35) & (np.abs(z - 1.95) < 0.5)
    col = np.where(scuff[..., None], col * 1.06, col)
    s.cv.paint(court, col)
    s.cv.paint(court & (noise(W, H, 3, 52, 2) > 0.78), col * 0.88)
    crack = court & (np.abs(np.sin(u * 9 + np.sin(z * 4) * 1.5)) < 0.015) & (z < 2.4)
    s.cv.paint(crack, col * 0.65)
    crack2 = court & (np.abs(u + 0.6 + (z - 1.3) * 0.4 + np.sin(z * 9) * 0.03) < 0.006)
    s.cv.paint(crack2, col * 0.65)
    line = np.zeros((H, W), bool)
    for (a, b) in [((-0.95, 1.05), (1.0, 1.05)), ((-0.95, 2.85), (1.0, 2.85)), ((-0.95, 1.05), (-0.95, 2.85)),
                   ((1.0, 1.05), (1.0, 2.85)), ((0.02, 1.05), (0.02, 2.85))]:
        line |= s.m().line([s.project(*a), s.project(*b)]).a > 0
    # Korbraum und Mittelkreis
    line |= s.m().line([s.project(0.55, 2.85), s.project(0.55, 2.3), s.project(0.85, 2.3), s.project(0.85, 2.85)]).a > 0
    ring = (np.hypot((u - 0.02) / 0.25, (z - 1.95) / 0.25) < 1) & ~(np.hypot((u - 0.02) / 0.22, (z - 1.95) / 0.22) < 1)
    lines_m = (line | ring) & court
    s.paint(lines_m, '#f4ece0')
    s.cv.paint(lines_m & (noise(W, H, 2, 53, 2) > 0.7), col * 1.1)
    rng = np.random.default_rng(22)
    ys, xs = np.nonzero(court)
    for i in rng.integers(0, len(xs), 70):
        c = rgb(['#c8783a', '#a85a2a', '#d8a050', '#7a8a3a'][int(rng.integers(0, 4))])
        s.cv.c[ys[i], xs[i]] = c
    # Kreidezeichnung (Himmel und Hoelle) und Springseil
    for k in range(4):
        a, b = s.project(-0.75, 1.15 + k * 0.12), s.project(-0.55, 1.15 + k * 0.12)
        s.paint(s.m().line([a, b]).a > 0, ['#f4ece0', '#f0c0d0', '#c0d8f0', '#f4ece0'][k])
    a = s.project(-0.75, 1.15); b = s.project(-0.75, 1.51); c_ = s.project(-0.55, 1.51); d = s.project(-0.55, 1.15)
    s.paint(s.m().line([a, b]).a > 0 | (s.m().line([c_, d]).a > 0), '#f4ece0')
    pts = [s.project(0.3 + 0.1 * np.sin(t * 6), 1.2 + t * 0.3) for t in np.linspace(0, 1, 12)]
    s.paint(s.m().line(pts).a > 0, '#3a8ae8')
    # Trinkflasche und Sporttasche am Rand
    bx_, by_ = s.project(-1.15, 1.4)
    s.paint(s.rect(int(bx_), int(by_) - 7, int(bx_) + 2, int(by_)), '#3a8ae8')
    s.paint(s.ellipse(bx_ + 12, by_ - 4, 8, 4), '#c84a3a')
    s.paint(s.rect(int(bx_) + 6, int(by_) - 6, int(bx_) + 18, int(by_) - 6), '#2a2a2a')
    # Korb hinten rechts
    px, py = s.project(0.7, 2.95)
    s.paint(s.rect(int(px) + 6, int(py) - 48, int(px) + 8, int(py)), '#2e5a4a')
    s.paint(s.rect(int(px) - 10, int(py) - 58, int(px) + 10, int(py) - 44), '#f4f0e8')
    s.paint(s.rect(int(px) - 4, int(py) - 52, int(px) + 4, int(py) - 47), '#e86a40')
    s.paint(s.rect(int(px) - 4, int(py) - 51, int(px) + 4, int(py) - 48), '#f4f0e8')
    s.paint(s.ellipse(px, py - 43, 5, 1.6) & ~s.ellipse(px, py - 43, 3.5, 0.8), '#e86a30')
    for k in range(-3, 4, 2):
        s.paint(s.m().line([(px + k, py - 42), (px + k * 0.6, py - 36)]).a > 0, '#f4f0e8')
    # Ball auf dem Feld
    bx, by = s.project(-0.35, 1.5)
    s.shadow(bx, by + 1, 5, 1.5)
    s.shade(s.ellipse(bx, by - 4, 4.5, 4.5), ['#8a3a1a', '#c85a28', '#e8803c', '#f4a868'])
    s.paint(s.m().line([(bx - 4, by - 4), (bx + 4, by - 4)]).a > 0 & s.ellipse(bx, by - 4, 4.5, 4.5), '#4a2010')
    # Laternen, Bank am Rand, Vordergrund
    for x, y in ((40, 200), (452, 196)):
        T.ornate_lamp(s, x, y, h=52, banner='#3e5a8a' if x < 200 else '#c8644a')
    s.bench(30, 250, s=1.1)
    s.flowerbed(-10, 120, 270, s=1.4, seed=21)
    s.foliage('br', 80, 22)
    s.walk(farY=176, nearY=244, farLeft=150, farRight=410, nearLeft=40, nearRight=440, farH=34, nearH=60)
    return s.finish(out)


if __name__ == '__main__':
    print(build(sys.argv[1]))
