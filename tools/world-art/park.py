"""Showcase: der Park am Meer am Abend (Vorbild: das Konzeptbild der Kuestenstadt im Abendrot)."""
import sys
import numpy as np
from px import Canvas, rgb, ramp, dramp, noise, glow, dither_bands, quantize, lerp, MaskPen

W, H = 480, 270
HY = 112            # Horizont (Meereslinie)
SUN = (238, 110)
rng = np.random.default_rng(7)
cv = Canvas(W, H)
yy, xx = cv.yy, cv.xx


def m():
    return MaskPen(W, H)


# ---------------------------------------------------------------- Himmel
t = np.clip(yy / HY, 0, 1)
sky = dramp([(0.0, '#14174a'), (0.25, '#2b2a72'), (0.48, '#5a3f97'), (0.66, '#b45a92'),
            (0.80, '#f07a6a'), (0.92, '#ff9e52'), (1.0, '#ffc66e')], t, 22)
# Zur Sonne hin waermer
d_sun = np.hypot((xx - SUN[0]) / 1.6, yy - SUN[1])
warm = np.clip(1 - d_sun / 150, 0, 1) ** 2
sky = lerp(sky, dramp([(0, '#ffcf7a'), (1, '#ffcf7a')], warm), (dramp([(0, (0, 0, 0)), (1, (0.55, 0.55, 0.55))], warm, 7)))
cv.paint(yy <= HY, sky)

# Sterne oben
for _ in range(70):
    x, y = rng.integers(0, W), rng.integers(0, 45)
    if rng.random() < 0.5 - y / 100:
        cv.c[y, x] = lerp(cv.c[y, x], rgb('#e8ecff'), 0.5 + rng.random() * 0.5)

# Wolken: wenige grosse Haufen statt Streifen, dazu feine Schlieren ueber dem Horizont
cn = noise(W * 2, H, 22, 11, octaves=5)[:, ::2][:, :W]
fine = noise(W * 4, H, 10, 12, octaves=3)[:, ::4][:, :W]


def heap(cx, cy, rx, ry):
    return np.clip(1 - np.hypot((xx - cx) / rx, (yy - cy) / ry), 0, 1)


masses = (heap(400, 64, 85, 24) * 1.2 + heap(330, 40, 60, 12) + heap(90, 74, 90, 16) * 1.1 +
          heap(150, 28, 70, 10) * 0.9 + heap(250, 52, 40, 7) * 0.8)
dens = cn * 0.55 + masses * 0.75
streak = np.zeros((H, W), bool)
cloud = ((dens > 0.62) | streak) & (yy < HY - 3)
cloud &= ~(np.hypot(xx - SUN[0], yy - SUN[1]) < 14)
th = np.clip(yy / HY, 0, 1)
sunward = np.clip(1 - np.abs(xx - SUN[0]) / 260, 0, 1)[..., None]
cloud_col = ramp([(0, '#3e3474'), (0.45, '#83467e'), (0.75, '#c85a72'), (1, '#e8806a')], th)
lit_col = lerp(ramp([(0, '#b05a96'), (0.5, '#ff7f86'), (1, '#ffc488')], th), rgb('#ffd59a'), sunward * 0.4)
from scipy import ndimage as _nd
# Abstand jeder Wolkenzelle zur Unterkante (dort trifft das Licht der tiefen Sonne)
below = np.zeros((H, W))
run = np.zeros(W)
for y in range(H - 1, -1, -1):
    run = np.where(cloud[y], run + 1, 0)
    below[y] = run
above = np.zeros((H, W))
run = np.zeros(W)
for y in range(H):
    run = np.where(cloud[y], run + 1, 0)
    above[y] = run
lightness = np.clip(1 - (below - 1) / 9, 0, 1)            # 1 an der Unterkante, 0 tief drin
lightness = np.maximum(lightness, np.clip(1 - (above - 1) / 2, 0, 1) * 0.35)  # Silberrand oben
body = dramp([(0, '#000000'), (1, '#ffffff')], lightness, 6)[..., 0]
shade_col = lerp(cloud_col, rgb('#2a2060'), 0.2)
col = lerp(shade_col, lit_col, body[..., None])
cv.paint(cloud, col)
rimc = cloud & ~np.roll(cloud, -1, axis=0)
cv.paint(rimc, lerp(lit_col, rgb('#fff0c8'), 0.45))

# Sonne
sun = np.hypot(xx - SUN[0], yy - SUN[1]) < 8.5
cv.paint(sun & (yy <= HY), rgb('#fff1b8'))
cv.add(glow(W, H, [(SUN[0], SUN[1], 1.0)], 16, '#ffb060', 0.55, steps=4) * (yy <= HY)[..., None])

# ---------------------------------------------------------------- Ferne Berge und Inseln


def ridge(x0, x1, base, peak, seed, rough=0.35, scale=40):
    xs = np.arange(W)
    n = noise(W, 4, scale, seed, octaves=5)[1]
    env = np.clip(np.sin(np.clip((xs - x0) / (x1 - x0), 0, 1) * np.pi), 0, 1) ** 0.8
    top = base - (base - peak) * env * (1 - rough + rough * n * 2)
    top[(xs < x0) | (xs > x1)] = base + 1
    return top


def hazed(c, k):
    hz = ramp([(0, '#f2a07a'), (1, '#f2a07a')], np.zeros((H, W)))
    return lerp(c, hz, k)


far = ridge(250, 470, HY, 84, 3, 0.45, 50)
far_mask = (yy >= far[None, :]) & (yy <= HY)
far_col = lerp(rgb('#8a5f9a'), rgb('#e48a82'), np.clip((yy - 84) / 28, 0, 1)[..., None] * 0.6)
cv.paint(far_mask, far_col)
# Lichtseite links (zur Sonne)
far_lit = far_mask & ~np.roll(far_mask, 1, axis=1)
cv.paint(far_lit, rgb('#f0a08a'))
mid_isle = ridge(300, 455, HY, 96, 5, 0.3, 30)
mi_mask = (yy >= mid_isle[None, :]) & (yy <= HY)
cv.paint(mi_mask, lerp(rgb('#5e4282'), rgb('#9a5c8a'), np.clip((yy - 96) / 16, 0, 1)[..., None]))
left_isle = ridge(70, 200, HY, 101, 8, 0.25, 30)
li_mask = (yy >= left_isle[None, :]) & (yy <= HY)
cv.paint(li_mask, rgb('#7a5590'))
cv.paint(li_mask & ~np.roll(li_mask, 1, axis=0), rgb('#d88a8e'))

# ---------------------------------------------------------------- Meer
sea_t = np.clip((yy - HY) / 70, 0, 1)
sea = dramp([(0, '#e89a86'), (0.08, '#b9719a'), (0.3, '#6a5aa8'), (0.7, '#3d4590'), (1, '#2a3172')], sea_t, 18)
sea_warm = np.clip(1 - np.abs(xx - SUN[0]) / (30 + (yy - HY) * 1.4), 0, 1)
sea = lerp(sea, rgb('#ff9f6a'), dramp([(0, (0, 0, 0)), (1, (0.5, 0.5, 0.5))], sea_warm ** 2, 6))
cv.paint(yy > HY, sea)
# Wellenlinien und Glitzerbahn
wn = noise(W * 4, H, 6, 21, octaves=2)[:, ::4][:, :W]
for y in range(HY + 2, 200):
    k = (y - HY)
    width = 4 + k * 0.45
    for x in range(int(SUN[0] - width), int(SUN[0] + width)):
        if 0 <= x < W and wn[y, x] > 0.62 + abs(x - SUN[0]) / width * 0.3 and (y + x // 7) % 2 == 0:
            cv.c[y, x] = lerp(cv.c[y, x], rgb('#ffe2a0'), 0.85)
wave = (wn > 0.7) & (yy > HY + 3) & ((yy % 3) == 0)
cv.paint(wave, lerp(sea, rgb('#c0a0e0'), 0.35))
# Horizontlinie leicht hell
cv.paint((yy == HY + 1), rgb('#f4b090'), 0.6)

# ---------------------------------------------------------------- Kleine Insel mit Leuchtturm
isle = m().poly([(156, 152), (162, 149), (168, 146), (172, 143), (177, 142), (186, 141), (191, 143),
                 (196, 146), (203, 149), (210, 152)]).a > 0
cv.paint(isle, rgb('#3a2c56'))
cv.paint(isle & (xx < 180) & (noise(W, H, 2, 77, 2) > 0.45), rgb('#5a3e66'))
cv.paint(isle & ~np.roll(isle, 1, axis=0) & (xx < 190), rgb('#d08a8a'))
cv.paint(isle & ~np.roll(isle, 1, axis=1), rgb('#b0708a'))
for x, y in [(168, 145), (171, 143), (194, 145), (199, 147)]:
    cv.paint(m().ellipse(x - 2, y - 2, x + 2, y + 1).a > 0, rgb('#24304a'))
    cv.c[y - 2, x - 1] = rgb('#8a6a6a')
cv.paint(m().rect(156, 153, 210, 153).a > 0, rgb('#7a5a90'))
tower = m().rect(181, 131, 183, 142).a > 0
cv.paint(tower, rgb('#e8dcd0'))
cv.c[131, 181:184] = rgb('#c04a4a')
cv.add(glow(W, H, [(182, 130, 0.6)], 2, '#ffd890', 0.6, steps=3))
cv.c[130, 182] = rgb('#fff6d0')
# Segelboot
cv.paint(m().poly([(130, 160), (138, 160), (136, 162), (131, 162)]).a > 0, rgb('#2c2246'))
cv.paint(m().poly([(134, 159), (134, 150), (138, 159)]).a > 0, rgb('#f4d4c0'))

# ---------------------------------------------------------------- Stadt am Hang (rechts)
hill = m().poly([(268, 200), (300, 160), (330, 140), (360, 128), (395, 116), (430, 108),
                 (480, 104), (480, 210)]).a > 0
hn = noise(W, H, 8, 31, octaves=3)
hill_col = lerp(rgb('#232c48'), rgb('#33405a'), hn[..., None])
cv.paint(hill, hill_col)
# Kaimauer mit Lichtern am Wasser
quay = m().poly([(262, 196), (300, 176), (360, 166), (480, 160), (480, 166), (360, 172), (300, 182), (266, 200)]).a > 0
cv.paint(quay, rgb('#5a4a6a'))
quay_lights = []
for x in range(290, 480, 9):
    yq = int(np.interp(x, [262, 300, 360, 480], [196, 176, 166, 160]))
    quay_lights.append((x, yq - 1, 0.5))
    cv.c[yq - 1, x] = rgb('#ffd890')
    # Spiegelung im Wasser
    for r in range(2, 9, 2):
        if yq + r < H:
            cv.c[yq + r, x] = lerp(cv.c[yq + r, x], rgb('#ffc070'), 0.5 - r * 0.04)


def house(x, y, w, h, roof='#b8604a', wall='#d8c0a8', lit_p=0.6, light_list=None, shade_k=1.0):
    """Ein Haus am Hang: Giebelwand zur Sonne hin hell, Seite im Schatten, flaches Ziegeldach."""
    side = max(2, w // 2)
    wc = rgb(wall) * shade_k
    cv.paint(m().rect(x, y, x + w - 1, y + h - 1).a > 0, wc)
    cv.paint(m().rect(x + w, y, x + w + side - 1, y + h - 1).a > 0, wc * 0.55 + rgb('#2a2050') * 0.15)
    cv.paint(m().rect(x - 1, y - 2, x + w + side, y - 1).a > 0, rgb(roof) * shade_k)
    cv.paint(m().rect(x - 1, y - 2, x + w // 2, y - 2).a > 0, lerp(rgb(roof), rgb('#ffb090'), 0.4) * shade_k)
    for wx in range(x + 1, x + w + side - 1, 2):
        for wy in range(y + 1, y + h - 1, 2):
            if wx == x + w - 1:
                continue
            if rng.random() < lit_p:
                cv.c[wy, wx] = rgb('#ffd27a')
                if light_list is not None:
                    light_list.append((wx, wy, 0.12))
            elif rng.random() < 0.5:
                cv.c[wy, wx] = wc * 0.55


town_lights = []
spots = []
for i in range(260):
    x = int(rng.uniform(300, 478))
    top = np.interp(x, [300, 330, 360, 395, 430, 480], [160, 140, 128, 116, 108, 104])
    y = int(rng.uniform(top + 4, min(top + 58, 196)))
    if y > np.interp(x, [262, 300, 360, 480], [196, 176, 166, 160]) - 4:
        continue
    spots.append((y, x))
for y, x in sorted(spots):
    w = int(rng.integers(3, 7))
    h = int(rng.integers(3, 6))
    k = 0.75 + 0.25 * (y - 100) / 90
    house(x, y, w, h, roof=['#a8504a', '#98443e', '#b8624a'][int(rng.integers(0, 3))],
          wall=['#f0d6b8', '#e8c8b0', '#f4e0c4'][int(rng.integers(0, 3))], lit_p=0.55,
          light_list=town_lights, shade_k=k * 0.85)
# Kirchturm
cv.paint(m().rect(412, 80, 418, 106).a > 0, rgb('#c8b0a0') * 0.8)
cv.paint(m().rect(419, 81, 420, 106).a > 0, rgb('#8a7080'))
cv.paint(m().poly([(411, 80), (416, 70), (421, 80)]).a > 0, rgb('#7a4a4a'))
cv.c[85:88, 415] = rgb('#ffd27a')
town_lights += [(415, 86, 0.3)]
cv.add(glow(W, H, town_lights + quay_lights, 2.0, '#ffb060', 0.3, steps=3))

# ---------------------------------------------------------------- Zypressen und Baeume am Hang


def cypress(x, base, height, width, col='#1e2a3c', mid='#2c3c48', lit='#8a6a6a'):
    """Zypresse: schlanke Flamme aus Buescheln, die linke Kante faengt das Abendlicht."""
    rr = np.random.default_rng(x * 7 + base)
    mask = np.zeros((H, W), bool)
    for i in range(height):
        f = i / height
        half = width / 2 * (np.sin(min(1.0, f * 1.6 + 0.25) * np.pi / 2) if f < 0.35 else (1 - (f - 0.35) / 0.65) ** 0.85)
        half = max(0.5, half + rr.uniform(-0.6, 0.6))
        y = base - i
        mask[y, int(round(x - half)):int(round(x + half)) + 1] = True
    cv.paint(mask, rgb(col))
    tex = noise(W, H, 2, x + base, octaves=2)
    cv.paint(mask & (tex > 0.6) & (xx < x), rgb(mid))
    left = mask & ~np.roll(mask, 1, axis=1)
    cv.paint(left, rgb(lit))
    cv.paint(mask & (tex > 0.75) & (xx < x - 1), lerp(rgb(mid), rgb(lit), 0.5))


cypress(146, 196, 70, 13)
cypress(160, 194, 46, 10)
cypress(336, 184, 62, 12)
cypress(352, 178, 40, 9)
cypress(296, 176, 30, 8)


def canopy(cx, cy, r, seed, base='#2c3a4c', mid='#3f5062', lit='#c88a6a', rim='#ffc080', light_dir=(-1, 0)):
    """Laubkrone aus Buescheln, von der Sonne (links) her angeleuchtet."""
    rr = np.random.default_rng(seed)
    mask = np.zeros((H, W), bool)
    blobs = []
    for _ in range(int(r * 1.3)):
        a = rr.uniform(0, 2 * np.pi)
        d = rr.uniform(0, r * 0.75)
        bx, by = cx + np.cos(a) * d * 1.25, cy + np.sin(a) * d * 0.8
        br = rr.uniform(r * 0.25, r * 0.48)
        blobs.append((bx, by, br))
        mask |= np.hypot(xx - bx, (yy - by) * 1.1) < br
    cv.paint(mask, rgb(base))
    inner = np.zeros((H, W), bool)
    for bx, by, br in blobs:
        inner |= np.hypot(xx - (bx + light_dir[0] * br * 0.35), (yy - by + br * 0.3) * 1.1) < br * 0.7
    cv.paint(mask & inner, rgb(mid))
    edge = mask & ~np.roll(mask, -light_dir[0] * 1, axis=1)
    top = mask & ~np.roll(mask, 1, axis=0)
    cv.paint((edge | top) & mask, rgb(lit))
    cv.paint(edge & top, rgb(rim))
    # Blattbuschel: kleine helle Flecken auf der Lichtseite, dunkle Loecher im Inneren
    leafn = noise(W, H, 2, seed + 500, octaves=1)
    cv.paint(mask & inner & (leafn > 0.78), lerp(rgb(mid), rgb(lit), 0.5))
    cv.paint(mask & ~inner & (leafn < 0.18), rgb(base) * 0.75)
    return mask


# Baumkronen am Hang unterhalb des Zauns
for i, (cx, cy, r) in enumerate([(120, 182, 12), (176, 180, 10), (214, 186, 11), (250, 184, 9),
                                 (282, 190, 12), (238, 176, 7), (98, 176, 9)]):
    canopy(cx, cy, r, 40 + i)

# ---------------------------------------------------------------- Boden: Plattenweg in Perspektive
GY = 192                       # vordere Kante des Hangs = Zaunlinie
K = 158.0                      # Pixel je Welteinheit bei z = 1


def ground_uv():
    z = (H - HY) / np.maximum(yy - HY, 1e-3)
    u = (xx - W / 2) * z / K
    return u, z


u, z = ground_uv()
ground = yy >= GY - 2
# Voronoi-Platten in Weltkoordinaten
S = 0.115
gi, gj = np.floor(u / S).astype(int), np.floor(z / S).astype(int)
best = np.full(u.shape, 9.0)
second = np.full(u.shape, 9.0)
cell_id = np.zeros(u.shape, int)
for di in (-1, 0, 1):
    for dj in (-1, 0, 1):
        ci, cj = gi + di, gj + dj
        h = (ci * 73856093) ^ (cj * 19349663)
        jx = ((h * 2654435761) % 1000) / 1000
        jy = ((h * 40503 + 12345) % 1000) / 1000
        sx, sz = (ci + 0.15 + jx * 0.7) * S, (cj + 0.15 + jy * 0.7) * S
        d = np.hypot(u - sx, (z - sz) * 1.0)
        closer = d < best
        second = np.where(closer, best, np.minimum(second, d))
        cell_id = np.where(closer, h, cell_id)
        best = np.minimum(best, d)
edge = (second - best) < 0.010 * z ** 0.6
var = ((cell_id * 2246822519) % 1000) / 1000
stone_base = ramp([(0, '#8d7c76'), (0.5, '#a08c80'), (1, '#b39c88')], var)
# Licht der Laternen auf den Platten
LANTERNS = [(112, 0), (190, 0), (268, 0), (346, 0)]   # Bildschirm-x auf dem Zaun
lamp_world = []
for lx, _ in LANTERNS:
    zl = (H - HY) / (GY - HY)
    lamp_world.append(((lx - W / 2) * zl / K, zl))
lamp_world += [((440 - W / 2) * 1.35 / K, 1.35)]        # Kiosk
pool = np.zeros(u.shape)
for lu, lz in lamp_world:
    pool += 1.0 / (1 + ((u - lu) ** 2 + ((z - lz) * 1.2) ** 2) * 70)
ambient = rgb('#5c5274') * 0.95
warm_l = rgb('#ffb466')
lightmap = ambient + warm_l * np.clip(pool, 0, 1.2)[..., None] * 0.95
stone = stone_base * lightmap * 1.25
# Tiefendunst zum Zaun hin
stone = lerp(stone, rgb('#6a5a82'), np.clip((z - 1.0) / 1.2, 0, 1)[..., None] * 0.35)
cv.paint(ground, stone)
gap = rgb('#2c2236')
cv.paint(ground & edge, lerp(gap, stone, 0.25))
# Lichtkante oben an jeder Platte (die Kante, die zum Betrachter zeigt, faengt das Licht)
top_lit = ground & ~edge & np.roll(edge, 1, axis=0)
cv.paint(top_lit, lerp(stone, rgb('#ffe0b0'), 0.35))
# Feine Textur
tex = noise(W, H, 2, 51, octaves=1)
cv.paint(ground & ~edge & (tex > 0.8), stone * 0.88)
cv.paint(ground & ~edge & (tex < 0.12), stone * 1.08)

# ---------------------------------------------------------------- Zaun mit Laternen


def post(x, top, bottom, w=3):
    cv.paint(m().rect(x, top, x + w - 1, bottom).a > 0, rgb('#5a3a2c'))
    cv.c[top:bottom + 1, x] = rgb('#a06a46')
    cv.c[top, x:x + w] = rgb('#c08458')


fence_y = GY - 1
for x in range(84, 470, 26):
    post(x, fence_y - 20, fence_y + 2)
rail = m().rect(84, fence_y - 17, 470, fence_y - 15).a > 0
cv.paint(rail, rgb('#6a4430'))
cv.paint(m().rect(84, fence_y - 17, 470, fence_y - 17).a > 0, rgb('#c08458'))
rail2 = m().rect(84, fence_y - 8, 470, fence_y - 7).a > 0
cv.paint(rail2, rgb('#5a3a2c'))
cv.paint(m().rect(84, fence_y - 8, 470, fence_y - 8).a > 0, rgb('#a06a46'))
# Schatten der Zaunlinie auf dem Boden
cv.paint((yy >= fence_y + 3) & (yy <= fence_y + 4) & (xx >= 84) & (xx <= 470), cv.c * 0.7)

lamp_pts = []
for lx, _ in LANTERNS:
    top = fence_y - 44
    cv.paint(m().rect(lx, top + 8, lx + 1, fence_y).a > 0, rgb('#2a2230'))
    cv.paint(m().rect(lx - 3, top, lx + 4, top + 1).a > 0, rgb('#2a2230'))
    cv.paint(m().poly([(lx - 3, top + 2), (lx + 4, top + 2), (lx + 3, top + 8), (lx - 2, top + 8)]).a > 0, rgb('#ffd890'))
    cv.paint(m().rect(lx, top + 3, lx + 1, top + 6).a > 0, rgb('#fff6d8'))
    cv.paint(m().rect(lx - 2, top + 8, lx + 3, top + 8).a > 0, rgb('#2a2230'))
    lamp_pts.append((lx + 0.5, top + 5, 1.0))
cv.add(glow(W, H, lamp_pts, 7, '#ffa040', 0.45, steps=3))
cv.add(glow(W, H, lamp_pts, 2.5, '#ffe0a0', 0.4, steps=2))

# ---------------------------------------------------------------- Haus mit Terrasse (links)
# Steinsockel
base_mask = m().poly([(0, 150), (96, 150), (100, 196), (0, 200)]).a > 0
bn = noise(W, H, 3, 61, octaves=2)
stone_wall = lerp(rgb('#5e4e5e'), rgb('#86707a'), bn[..., None])
stone_wall = lerp(stone_wall, rgb('#c08868'), np.clip(1 - np.abs(xx - 60) / 60, 0, 1)[..., None] * 0.2)
cv.paint(base_mask, stone_wall)
for y in range(152, 200, 6):
    off = 0 if (y // 6) % 2 else 5
    cv.paint(base_mask & (yy == y), rgb('#4a3c4c'))
    for x in range(off, 100, 11):
        cv.paint(base_mask & (xx == x) & (yy > y) & (yy < y + 6), rgb('#4a3c4c'))
cv.paint(base_mask & (yy == 150), rgb('#d8b08a'))
# Treppe rechts am Sockel
for i in range(6):
    y0 = 158 + i * 7
    x0 = 70 - i * 0
    cv.paint(m().rect(66 + i * 2, y0, 112 + i * 3, y0 + 6).a > 0, rgb('#6e5c66') * (0.9 + i * 0.03))
    cv.paint(m().rect(66 + i * 2, y0, 112 + i * 3, y0 + 1).a > 0, rgb('#c8a088'))
    cv.paint(m().rect(66 + i * 2, y0 + 5, 112 + i * 3, y0 + 6).a > 0, rgb('#4a3c4c'))
# Holzhaus
wall = m().rect(0, 44, 80, 150).a > 0
board = yy // 5
bvar = ((board * 2654435761) % 997) / 997
planks = ramp([(0, '#5e3e30'), (1, '#7a5240')], bvar)
planks = lerp(planks, rgb('#b06a40'), np.clip(1 - xx / 90, 0, 1)[..., None] * 0.25)
cv.paint(wall, planks)
cv.paint(wall & (yy % 5 == 4), rgb('#3a2620'))
cv.paint(wall & (yy % 5 == 0), lerp(planks, rgb('#c08458'), 0.35))
for x in (0, 80):
    cv.paint(wall & (xx >= x) & (xx <= x + 2), rgb('#4a3028'))
# grosses warmes Fenster
win = m().rect(10, 70, 58, 128).a > 0
interior = ramp([(0, '#ffcf80'), (1, '#e88a48')], np.clip((yy - 70) / 58, 0, 1))
cv.paint(win, interior)
for x in (10, 26, 42, 58):
    cv.paint(m().rect(x - 1, 70, x, 128).a > 0, rgb('#4a3028'))
for y in (70, 99, 128):
    cv.paint(m().rect(9, y - 1, 59, y).a > 0, rgb('#4a3028'))
# Silhouette einer Leselampe und eines Regals im Fenster
cv.paint(m().rect(30, 110, 38, 128).a > 0, rgb('#c46a38'))
cv.paint(m().rect(46, 84, 56, 127).a > 0, rgb('#a85a34'))
for y in range(88, 126, 7):
    cv.paint(m().rect(47, y, 55, y + 1).a > 0, rgb('#7a3a24'))
cv.add(glow(W, H, [(34, 99, 2.0)], 14, '#ffa850', 0.18, steps=2))
# Veranda: Pfosten, Dach, Gelaender
cv.paint(m().rect(86, 52, 90, 150).a > 0, rgb('#5a3a2c'))
cv.c[52:150, 86] = rgb('#b07a50')
roof = m().poly([(0, 18), (40, 26), (104, 48), (104, 54), (0, 46)]).a > 0
cv.paint(roof, rgb('#3a2a3a'))
for x in range(0, 104, 5):
    cv.paint(roof & (xx == x) & (yy > 40), rgb('#5a4048'))
cv.paint(m().line([(0, 18), (40, 26), (104, 48)], 2).a > 0, rgb('#7a5a6a'))
cv.paint(m().rect(0, 46, 104, 50).a > 0, rgb('#6a4430'))
cv.paint(m().rect(0, 46, 104, 46).a > 0, rgb('#c08458'))
# Gelaender
cv.paint(m().rect(60, 132, 96, 133).a > 0, rgb('#7a5038'))
for x in range(62, 96, 5):
    cv.paint(m().rect(x, 133, x + 1, 149).a > 0, rgb('#6a4430'))
# Laterne an der Veranda
cv.paint(m().rect(94, 58, 99, 66).a > 0, rgb('#ffd890'))
cv.add(glow(W, H, [(96, 62, 1.0)], 6, '#ffa040', 0.45, steps=3))
# Ranken mit Blueten am Pfosten und Dach
vine = np.zeros((H, W), bool)
vn = noise(W, H, 5, 81, octaves=3)
vine |= (vn > 0.55) & (((xx > 80) & (xx < 96) & (yy > 48) & (yy < 150)) | ((yy > 44) & (yy < 60) & (xx < 100)))
cv.paint(vine, rgb('#2e4a3a'))
cv.paint(vine & (vn > 0.68), rgb('#4e6e4a'))
fl = vine & (noise(W, H, 1, 82, 1) > 0.86)
cv.paint(fl, rgb('#f07aa0'))

# ---------------------------------------------------------------- Kiosk rechts
kx0, kx1, ky0, ky1 = 404, 480, 150, 216
cv.paint(m().rect(kx0, ky0 + 18, kx1, ky1).a > 0, rgb('#6a4836'))
inside = m().rect(kx0 + 6, ky0 + 24, kx1, ky1 - 18).a > 0
cv.paint(inside, ramp([(0, '#ffd890'), (1, '#e89048')], np.clip((yy - ky0 - 24) / 24, 0, 1)))
for y in (ky0 + 32, ky0 + 40):
    cv.paint(m().rect(kx0 + 6, y, kx1, y).a > 0, rgb('#8a4a2a'))
for x in range(kx0 + 9, kx1, 5):
    cv.paint(m().rect(x, ky0 + 28, x + 2, ky0 + 31).a > 0, [rgb('#e85a5a'), rgb('#5ab0e0'), rgb('#f0e070')][(x // 5) % 3])
cv.paint(m().rect(kx0, ky1 - 18, kx1, ky1 - 15).a > 0, rgb('#c08458'))
cv.paint(m().rect(kx0, ky1 - 14, kx1, ky1).a > 0, rgb('#7a5038'))
# Markise gestreift
aw = m().poly([(kx0 - 8, ky0 + 22), (kx0 + 2, ky0 + 6), (kx1, ky0 + 6), (kx1, ky0 + 22)]).a > 0
stripes = ((xx - yy // 3) // 6) % 2 == 0
cv.paint(aw, np.where(stripes[..., None], rgb('#2f8a8a'), rgb('#f2ead8')))
cv.paint(aw & (yy >= ky0 + 20), rgb('#1e5a5a'))
for x in range(kx0 - 8, kx1, 6):
    cv.paint(m().ellipse(x, ky0 + 20, x + 5, ky0 + 25).a > 0, np.where(((x // 6) % 2 == 0), rgb('#2f8a8a'), rgb('#e0d6c0')))
cv.add(glow(W, H, [(445, 182, 2.5)], 16, '#ffa050', 0.16, steps=2))

# ---------------------------------------------------------------- Bank
bx0, bx1, by = 268, 330, 218
cv.paint(m().rect(bx0, by - 18, bx1, by - 15).a > 0, rgb('#7a4e34'))
cv.paint(m().rect(bx0, by - 18, bx1, by - 18).a > 0, rgb('#c08458'))
cv.paint(m().rect(bx0, by - 12, bx1, by - 10).a > 0, rgb('#7a4e34'))
cv.paint(m().rect(bx0, by - 12, bx1, by - 12).a > 0, rgb('#c08458'))
cv.paint(m().rect(bx0 - 2, by - 4, bx1 + 2, by - 1).a > 0, rgb('#8a5a3c'))
cv.paint(m().rect(bx0 - 2, by - 4, bx1 + 2, by - 4).a > 0, rgb('#e0a070'))
for x in (bx0, bx1 - 3):
    cv.paint(m().rect(x, by - 20, x + 3, by + 6).a > 0, rgb('#3a2a2a'))
cv.paint(m().ellipse(bx0 - 4, by + 4, bx1 + 6, by + 10).a > 0, cv.c * 0.6)

# ---------------------------------------------------------------- Vordergrund: Blumen und Laub


def flowerbed(x0, x1, y0, y1, seed, colors):
    rr = np.random.default_rng(seed)
    bed = m().ellipse(x0, y0, x1, y1 + (y1 - y0)).a > 0
    bed &= yy <= y1
    bn = noise(W, H, 3, seed, octaves=2)
    cv.paint(bed, lerp(rgb('#1e3a32'), rgb('#3a5e3e'), bn[..., None]))
    cv.paint(bed & ~np.roll(bed, 1, axis=0), rgb('#6a8a4a'))
    for _ in range(int((x1 - x0) * (y1 - y0) / 14)):
        x, y = rr.integers(x0, x1), rr.integers(y0, y1)
        if not (0 <= x < W and 0 <= y < H) or not bed[y, x]:
            continue
        c = rgb(colors[rr.integers(0, len(colors))])
        light = 0.75 + 0.4 * (1 - (y - y0) / (y1 - y0 + 1))
        for dx, dy in ((0, 0), (1, 0), (0, 1), (-1, 0), (0, -1)):
            if 0 <= x + dx < W and 0 <= y + dy < H and bed[y + dy, x + dx]:
                cv.c[y + dy, x + dx] = c * (light if (dx, dy) != (0, 0) else light * 1.2)
        cv.c[y, x] = np.minimum(c * 1.35 + 0.1, 1)


flowerbed(-30, 120, 236, 270, 91, ['#8a6ae0', '#b088f0', '#f0f0f8', '#e070a0'])
flowerbed(380, 520, 240, 270, 92, ['#8a6ae0', '#f0f0f8', '#e88ab0', '#ffd070'])
flowerbed(100, 240, 196, 206, 93, ['#e070a0', '#f0f0f8', '#b088f0'])
flowerbed(330, 400, 197, 206, 94, ['#ffd070', '#f0f0f8', '#e070a0'])

# Laub oben links und rechts, als Rahmen
leaf = lambda cx, cy, r, s: canopy(cx, cy, r, s, base='#1a2a2e', mid='#2a3e3a', lit='#6a7a4a', rim='#e0b070', light_dir=(-1, 0))
leaf(170, -6, 22, 101)
leaf(130, 2, 16, 102)
leaf(440, 10, 30, 103)
leaf(480, 50, 26, 104)
leaf(405, -4, 18, 105)
# Ast
cv.paint(m().line([(480, 70), (450, 40), (420, 18), (395, 6)], 4).a > 0, rgb('#2a1e24'))
cv.paint(m().line([(480, 68), (450, 38), (420, 16), (395, 4)], 1).a > 0, rgb('#7a4a3a'))

# ---------------------------------------------------------------- Gesamtlicht und Vignette
vig = np.hypot((xx - W / 2) / (W * 0.62), (yy - H * 0.45) / (H * 0.75))
cv.c *= np.clip(1.08 - vig ** 2 * 0.45, 0.55, 1.1)[..., None]

raw = cv.c.copy()
cv.c = quantize(cv.c, 128)
out = sys.argv[1] if len(sys.argv) > 1 else 'park_evening.png'
cv.save(out)
cv.save(out.replace('.png', '_x3.png'), 3)
cv.c = raw
cv.save(out.replace('.png', '_raw.png'), 3)
