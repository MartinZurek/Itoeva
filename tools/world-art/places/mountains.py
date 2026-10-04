"""MOUNTAINS - Bergpass am Morgen: Schneegipfel hintereinander im blauen Dunst, Wasserfall in
eine Schlucht, Tannen an den Haengen, Felsweg mit Steinbank und Wegkreuz aus Holz, Alpenblumen."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
from kit import Scene, W, H
from px import rgb, lerp, noise


def peaks(s, x0, x1, base, peak, seed, col, lit, snow, rough=0.3):
    """Bergkette: Flanken zum Licht (links) hell, Schnee auf den Hoehen, schraege Felsrinnen."""
    from scipy.ndimage import gaussian_filter1d
    top = s.ridge(x0, x1, base, peak, seed, rough, scale=70)
    m = (s.yy >= top[None, :]) & (s.yy <= base)
    st = gaussian_filter1d(top, 4)
    tops = [x for x in range(2, W - 2) if st[x] < st[x - 1] and st[x] <= st[x + 1] and st[x] < base - 10]
    tops = tops or [W // 2]
    px = np.array(tops)
    wob = (noise(W, H, 6, seed + 3, 2) - 0.5) * 14
    dist = np.abs(s.xx[..., None] + wob[..., None] - px[None, None, :])
    owner = px[np.argmin(dist, axis=-1)]
    lit_face = (s.xx + wob) < owner
    shade_d = np.clip((s.yy - top[None, :]) / (base - peak), 0, 1)
    light = np.where(lit_face, 0.85, 0.35) - shade_d * 0.25 + (noise(W, H, 3, seed + 9, 2) - 0.5) * 0.25
    q = np.clip(np.floor(np.clip(light, 0, 1) * 3 + s.by * 0.9) / 3, 0, 1)
    slope = np.where(lit_face, 1.0, -1.0)
    s.cv.paint(m, lerp(rgb(col) * 0.8, rgb(lit), q[..., None]))
    gully = m & (((s.xx + s.yy * slope * 0.8).astype(int) % 9) == 0) & (noise(W, H, 4, seed + 7, 2) > 0.5)
    s.cv.paint(gully, s.cv.c * 0.85)
    depth_in = s.yy - top[None, :]
    snowm = m & (depth_in < 5 + noise(W, H, 4, seed + 5, 2) * 18) & (s.yy < peak + (base - peak) * 0.5)
    s.cv.paint(snowm, lerp(rgb(snow) * 0.86, rgb('#ffffff'), q[..., None]))
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
    fall = s.rect(380, 96, 392, 176)
    s.paint(fall, '#c8e4f0')
    s.paint(fall & ((s.yy + s.xx * 3) % 5 == 0), '#ffffff')
    s.paint(fall & (s.xx == 380), '#8ab0c8')
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
    seat = s.stone_seat(330, 222, s=1.0)
    s.walk(farY=196, nearY=248, farLeft=60, farRight=420, nearLeft=30, nearRight=440, farH=40, nearH=60)
    s.spot('BENCH', seat, 330, 228)
    return s.finish(out)


if __name__ == '__main__':
    print(build(sys.argv[1]))
