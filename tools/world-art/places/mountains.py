"""MOUNTAINS - Bergpass am Morgen: Schneegipfel hintereinander im blauen Dunst, Wasserfall in
eine Schlucht, Tannen an den Haengen, Felsweg mit Steinbank und Wegkreuz aus Holz, Alpenblumen."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
import nature as N
from kit import Scene, W, H
from px import rgb, lerp, noise


def peaks(s, x0, x1, base, peak, seed, col, lit, snow, rough=0.3):
    """Bergkette: Felsform als Hoehenfeld aus Kegeln je Gipfel plus Rauschen - Licht von links
    oben faellt auf Grate und Flanken, Rinnen und Schneefelder folgen der Form."""
    from scipy.ndimage import gaussian_filter1d, gaussian_filter
    top = s.ridge(x0, x1, base, peak, seed, rough, scale=70)
    m = (s.yy >= top[None, :]) & (s.yy <= base)
    st = gaussian_filter1d(top, 3)
    tops = [x for x in range(2, W - 2) if st[x] < st[x - 1] and st[x] <= st[x + 1] and st[x] < base - 8]
    tops = tops or [W // 2]
    B = np.full((H, W), -1e3)
    for px in tops:
        py = st[px]
        R = (base - py) * 1.3
        B = np.maximum(B, R - np.sqrt((s.xx - px) ** 2 + ((s.yy - py) * 0.7) ** 2))
    B = B + (noise(W, H, 10, seed + 5, 3) - 0.5) * 10 + (noise(W, H, 3, seed + 6, 2) - 0.5) * 4
    B = gaussian_filter(B, 0.8)
    gy, gx = np.gradient(B)
    nz = 1.6
    nrm = np.sqrt(gx ** 2 + gy ** 2 + nz ** 2)
    lam = np.clip((-gx * -0.75 + -gy * -0.5 + nz * 0.45) / nrm, 0, 1)
    q = np.clip(np.floor(lam * 4 + s.by * 0.9) / 4, 0, 1)
    s.cv.paint(m, lerp(rgb(col) * 0.72, rgb(lit), q[..., None]))
    depth_in = s.yy - top[None, :]
    snowm = m & ((depth_in < 4 + noise(W, H, 4, seed + 7, 2) * 14) | ((lam > 0.62) & (gy < 0.4))) & \
            (s.yy < peak + (base - peak) * 0.5)
    s.cv.paint(snowm, lerp(rgb(snow) * 0.8, rgb('#ffffff'), q[..., None]))
    s.paint(m & ~np.roll(m, 1, axis=0) & (s.yy < peak + (base - peak) * 0.5), '#ffffff')
    return m


def build(out):
    s = Scene('mountains', horizon=170, seed=33)
    s.sky(top='#4a86c8', mid='#8ab8e0', low='#d8ecf4', warm='#fff0d8')
    peaks(s, -60, 300, 150, 34, 61, '#8a9ab8', '#c0cce0', '#e8eef8')
    peaks(s, 170, 540, 150, 46, 62, '#7a8aa8', '#aab8d0', '#e0e8f4')
    peaks(s, 40, 440, 168, 92, 63, '#5a7088', '#8aa0b4', '#d8e4f0', rough=0.25)
    # Tannen an den Haengen
    for x in range(0, 480, 7):
        y = 150 + (x * 17) % 22
        s.tree(x, y, 18 + (x * 7) % 10, seed=x, kind='pine', crown=('#2e4a44', '#3e5e50', '#56785c', '#7a9670'))
    # Wasserfall rechts in die Schlucht
    wob = (noise(W, H, 3, 12, 2) - 0.5) * 4
    fall = (np.abs(s.xx - 386 - (s.yy - 96) * 0.03 + wob) < 3 + (s.yy - 96) * 0.06) & (s.yy >= 96) & (s.yy <= 176)
    s.cv.paint(fall, lerp(rgb('#a8cce0'), rgb('#ffffff'), (((s.yy * 2 + s.xx) % 6) < 2)[..., None] * 0.8))
    s.paint(fall & (((s.yy + s.xx * 3) % 9) == 0), '#7aa8c8')
    for k in range(30):
        q = np.random.default_rng(k)
        s.paint(s.ellipse(386 + q.uniform(-14, 14), 174 + q.uniform(-6, 4), q.uniform(1, 3), q.uniform(1, 2)), '#f4fafc')
    s.paint(s.ellipse(386, 178, 18, 5), '#e8f4fa')
    s.hills([(-40, 520, 140, '#3e5e48', '#5a7a5a', 64, 0.15)])
    s.HY = 170
    s.grass(170, base=('#5a7a3a', '#6e8e44', '#86a452', '#a0b864'), haze='#b8c8c0')
    # Felsweg
    s.stone_path([(0.4, 6.0), (0.1, 2.6), (-0.1, 1.5), (0.0, 1.0)], 0.3,
                 tone=('#7a7468', '#948c80', '#b0a898', '#ccc4b2'))
    for x, y, r in ((40, 230, 22), (110, 196, 12), (300, 192, 10), (450, 240, 26)):
        s.rock(x, y, r, r * 0.7, cols=('#5a5852', '#7a766c', '#9a948a', '#c4bcae'), seed=x, moss='#7a9a4a')
    # Alpenblumen
    rr = np.random.default_rng(9)
    for _ in range(260):
        x, y = int(rr.uniform(0, W)), int(rr.uniform(176, 268))
        s.paint(s.rect(x, y, x, y), ['#f4f0e0', '#9a88e0', '#e8c040'][int(rr.integers(0, 3))])
    rng = np.random.default_rng(33)
    # Bach vom Wasserfall durch die Wiese
    u, z = s.ground_uv()
    brook = (s.yy > 172) & (np.abs(u - (0.9 - 0.25 * np.log(np.maximum(z, 1)) * 2 + 0.08 * np.sin(z * 2))) < 0.05 + 0.01 * z)
    s.cv.paint(brook, lerp(rgb('#5a8ab8'), rgb('#a8d0e8'), noise(W, H, 2, 9, 2)[..., None]))
    s.paint(brook & ~np.roll(brook, 1, axis=0), '#e8f4fa')
    # Almhuette mit Holzzaun links hinten
    s.paint(s.rect(70, 168, 104, 188), '#7a5a3a')
    for y in range(170, 188, 3):
        s.paint(s.rect(70, y, 104, y), '#5a3e28')
    s.paint(s.poly([(64, 170), (87, 154), (110, 170)]), '#6a5a52')
    s.paint(s.m().line([(64, 170), (87, 154)]).a > 0, '#a89a90')
    s.paint(s.rect(82, 176, 90, 188), '#3a2618')
    s.paint(s.rect(74, 174, 79, 179), '#e8c070')
    s.paint(s.rect(93, 174, 98, 179), '#4a5a72')
    s.paint(s.rect(98, 148, 101, 160), '#5a5048')
    s.fence(192, 20, 150, s=0.6)
    # Einzelne Tannen vorn als Rahmen
    for x, b, h in ((18, 240, 110), (468, 250, 130), (440, 200, 60)):
        N.pine(s.cv, x, b, h, rng)
    seat = s.stone_seat(330, 222, s=1.0)
    s.walk(farY=196, nearY=248, farLeft=60, farRight=420, nearLeft=30, nearRight=440, farH=40, nearH=60)
    s.spot('BENCH', seat, 330, 228)
    return s.finish(out)


if __name__ == '__main__':
    print(build(sys.argv[1]))
