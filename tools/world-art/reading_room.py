"""Das Lesezimmer am Abend (Vorbild: das Konzeptbild mit warmer Lampe und Blick auf den Park
zur blauen Stunde). Zentralperspektive, Licht aus Stehlampe, Wandleuchte und Fenster."""
import sys
import numpy as np
from px import Canvas, rgb, ramp, dramp, noise, glow, quantize, lerp, MaskPen
from room3d import Room

W, H = 480, 270
cv = Canvas(W, H)
yy, xx = cv.yy, cv.xx
rng = np.random.default_rng(3)
room = Room(cv, f=230.0, vp=(232, 100), cam_h=0.62, half_w=1.05, height=1.45, back=2.7)
LAMP = np.array([0.88, 0.95, 1.85])
SCONCE = np.array([-0.98, 0.95, 2.1])
room.lights = [(LAMP, rgb('#ffb05a'), 1.7), (SCONCE, rgb('#ffa050'), 0.6), (np.array([0.0, 1.2, 0.9]), rgb('#a07060'), 0.25),
               (np.array([0.25, 0.9, 2.55]), rgb('#4a64b0'), 0.35)]
room.ambient = rgb('#3e2e34')

WIN = (-0.22, 0.78, 0.42, 1.22)     # X0, X1, Y0, Y1 auf der Rueckwand

# ---------------------------------------------------------------- Ebenen
which, X, Y, Z = room.planes()
P = np.stack([X, Y, Z], axis=-1)
FLOOR, CEIL, LEFT, RIGHT, BACK = range(5)


def shade(mask, albedo, normal):
    L = room.light_dithered(P, normal)
    col = albedo * L
    cv.paint(mask, col)


# Boden: Dielen, die zum Fluchtpunkt laufen, mit versetzten Stoessen
fl = which == FLOOR
plank = np.floor(X / 0.14).astype(int)
off = ((plank * 2654435761) % 997) / 997
joint = np.floor(Z / 0.9 + off).astype(int)
pv = (((plank * 7919 + joint * 104729) * 2654435761) % 997) / 997
wood = ramp([(0, '#7a4a30'), (0.5, '#8e5a3a'), (1, '#9e6844')], pv)
grain = noise(W, H, 3, 5, octaves=2)
wood = wood * (0.96 + 0.06 * (grain > 0.6)[..., None])
shade(fl, wood, (0, 1, 0))
# Fugen: entlang der Dielen (X) und Stoesse (Z), dicker in der Naehe
gx = np.abs(X / 0.14 - np.round(X / 0.14)) * 0.14
gz = np.abs((Z / 0.9 + off) - np.round(Z / 0.9 + off)) * 0.9
px_world = Z / room.f
seam = fl & ((gx < px_world * 0.7) | (gz < px_world * 0.6))
cv.paint(seam, cv.c * 0.55)
# Glanz: das Fensterlicht spiegelt sich auf dem Boden
refl = fl & (X > WIN[0] + 0.05) & (X < WIN[1] - 0.05) & (Z > 1.8) & (Z < 2.65)
cv.paint(refl, lerp(cv.c, rgb('#6a86c8'), 0.22))

# Rueckwand: Tapete mit feinem Rankenmuster, darunter Holzvertaefelung
bk = which == BACK
paper = ramp([(0, '#9a6a52'), (1, '#a8775c')], (noise(W, H, 6, 9, 2) > 0.5).astype(float) * 0.5)
dots = (np.abs(((X * 18) % 1) - 0.5) < 0.06) & (np.abs(((Y * 9) % 1) - 0.5) < 0.06)
paper = np.where(dots[..., None], paper * 1.12, paper)
wains = Y < 0.38
panel = rgb('#5e3a28') * np.where(((np.abs(((X / 0.3) % 1) - 0.5) > 0.44) | (np.abs(Y - 0.2) > 0.15))[..., None], 0.8, 1.0)
shade(bk, np.where(wains[..., None], panel, paper), (0, 0, -1))
cv.paint(bk & (np.abs(Y - 0.38) < 0.012), rgb('#c08458') * room.light_at(P, (0, 0, -1)) * 1.4)

# Linke und rechte Wand
for side, n in ((LEFT, (1, 0, 0)), (RIGHT, (-1, 0, 0))):
    msk = which == side
    paper_s = ramp([(0, '#94664e'), (1, '#a2735a')], (noise(W, H, 6, 10, 2) > 0.5).astype(float) * 0.5)
    shade(msk, np.where((Y < 0.38)[..., None], panel, paper_s), n)
    cv.paint(msk & (np.abs(Y - 0.38) < 0.012), rgb('#c08458') * room.light_at(P, n) * 1.4)

# Decke mit Balken
ce = which == CEIL
beams = (np.abs(((X / 0.42) % 1) - 0.5) > 0.38)
shade(ce, np.where(beams[..., None], rgb('#4a2e22'), rgb('#6e4c38')), (0, -1, 0))

# ---------------------------------------------------------------- Fenster mit Park zur blauen Stunde
wx0, wy0 = room.proj(WIN[0], WIN[3], room.back)
wx1, wy1 = room.proj(WIN[1], WIN[2], room.back)
wx0, wy0, wx1, wy1 = int(round(wx0)), int(round(wy0)), int(round(wx1)), int(round(wy1))
win = (xx >= wx0) & (xx <= wx1) & (yy >= wy0) & (yy <= wy1)
t = np.clip((yy - wy0) / (wy1 - wy0), 0, 1)
sky = dramp([(0, '#1a2a6a'), (0.45, '#2e4a9a'), (0.75, '#5a7ac0'), (1, '#8aa0d0')], t, 12)
cv.paint(win, sky)
# Ferne Stadt
city_top = wy0 + (wy1 - wy0) * 0.58
for bx in range(wx0, wx1, 1):
    hgt = int(3 + 9 * noise(W, 4, 3, 44, 2)[1, bx] ** 2 + (5 if (bx * 7) % 23 == 0 else 0))
    cv.paint(win & (xx == bx) & (yy > city_top - hgt), rgb('#1e2650'))
    for wyy in range(int(city_top - hgt) + 1, int(city_top) + 6, 2):
        if (bx + wyy) % 3 == 0 and rng.random() < 0.45:
            cv.c[wyy, bx] = rgb('#ffd890')
# Park: Baumkronen und Wege
pk = win & (yy > city_top + 2)
cv.paint(pk, rgb('#18244a'))
tn = noise(W, H, 4, 45, 3)
trees = win & (yy > city_top - 4) & (tn + (yy - city_top) / 30 > 0.75)
cv.paint(trees, rgb('#16203e'))
cv.paint(trees & ~np.roll(trees, 1, axis=0), rgb('#3a4e80'))
path = win & (yy > wy1 - 10) & (np.abs(xx - (wx0 + wx1) / 2 - (yy - wy1) * 1.5) < 4 + (yy - (wy1 - 10)) * 0.8)
cv.paint(path, rgb('#4a5a8a'))
lamps = [(wx0 + 14, int(city_top + 8)), (wx0 + 44, int(city_top + 12)), (wx1 - 20, int(city_top + 9))]
for lx, ly in lamps:
    cv.paint((xx == lx) & (yy > ly) & (yy < ly + 9), rgb('#10182e'))
    cv.c[ly, lx] = rgb('#fff0c0')
cv.add(glow(W, H, [(lx, ly, 1.0) for lx, ly in lamps], 2.5, '#ffc070', 0.6, steps=3) * win[..., None])
# Sterne
for _ in range(14):
    sx, sy = rng.integers(wx0, wx1), rng.integers(wy0, int(wy0 + (wy1 - wy0) * 0.35))
    cv.c[sy, sx] = rgb('#c8d4ff')
# Sprossen und Rahmen
frame = rgb('#3a2418')
midx = (wx0 + wx1) // 2
midy = wy0 + (wy1 - wy0) * 0.45
cv.paint(win & ((np.abs(xx - midx) <= 1) | (np.abs(yy - midy) <= 1)), frame)
cv.paint(((xx >= wx0 - 3) & (xx <= wx1 + 3) & (yy >= wy0 - 3) & (yy <= wy1 + 3)) & ~win, frame)
cv.paint((xx >= wx0 - 6) & (xx <= wx1 + 6) & (yy >= wy1 + 2) & (yy <= wy1 + 5), rgb('#8a5a3a'))
cv.paint((xx >= wx0 - 6) & (xx <= wx1 + 6) & (yy == wy1 + 2), rgb('#d09868'))
# Kuehles Licht faellt vom Fenster in den Raum
cv.add(glow(W, H, [(midx, wy1 + 4, 4.0)], 18, '#3a50a0', 0.12, steps=2) * (~win)[..., None])

# Vorhaenge in Falten
for cx0, cx1 in ((wx0 - 16, wx0 + 2), (wx1 - 2, wx1 + 16)):
    cur = (xx >= cx0) & (xx <= cx1) & (yy >= wy0 - 8) & (yy <= wy1 + 22)
    fold = (np.sin((xx - cx0) * 1.1) + 1) / 2
    col = lerp(rgb('#5a1e2a'), rgb('#9a3a3e'), fold[..., None])
    cv.paint(cur, col * room.light_at(np.stack([np.full_like(X, 0.0), np.full_like(X, 0.8), np.full_like(X, 2.6)], -1), (0, 0, -1)) * 1.8)
cv.paint((xx >= wx0 - 20) & (xx <= wx1 + 20) & (yy >= wy0 - 11) & (yy <= wy0 - 9), rgb('#2a1a12'))

# ---------------------------------------------------------------- Teppich
rug = fl & (X > -0.62) & (X < 0.48) & (Z > 1.25) & (Z < 2.2)
ru, rz = (X + 0.62) / 1.1, (Z - 1.25) / 0.95
border = (ru < 0.08) | (ru > 0.92) | (rz < 0.1) | (rz > 0.9)
inner_b = ((ru < 0.12) | (ru > 0.88) | (rz < 0.15) | (rz > 0.85)) & ~border
diamond = (np.abs(ru - 0.5) * 1.1 + np.abs(rz - 0.5)) < 0.22
motif = (np.abs(((ru * 8) % 1) - 0.5) + np.abs(((rz * 6) % 1) - 0.5)) < 0.18
rugc = np.where(border[..., None], rgb('#2a2a5a'), rgb('#8a2a2e'))
rugc = np.where(inner_b[..., None], rgb('#d0a050'), rugc)
rugc = np.where((diamond & ~border)[..., None], rgb('#2a3a6a'), rugc)
rugc = np.where((motif & ~border & ~inner_b & ~diamond)[..., None], rgb('#b04a3a'), rugc)
shade(rug, rugc, (0, 1, 0))

# ---------------------------------------------------------------- Buecherregal an der linken Wand
SH_X0, SH_X1 = -1.05, -0.8
shelf_z0, shelf_z1 = 1.75, 2.68
boards = [0.02, 0.3, 0.56, 0.82, 1.08, 1.32]
# Rueckwand des Regals schon durch die Wand gegeben; Korpus-Seite vorne

book_cols = ['#8a2a2e', '#2a4a7a', '#3a6a4a', '#c08a3a', '#6a3a6a', '#a85a3a', '#2a5a6a', '#d0b070', '#4a2a3a']
for b0, b1 in zip(boards[:-1], boards[1:]):
    z = shelf_z1 - 0.02
    while z > shelf_z0 + 0.03:
        th = rng.uniform(0.025, 0.05)
        ht = rng.uniform(0.15, 0.22) if b1 - b0 > 0.24 else rng.uniform(0.12, 0.2)
        if rng.random() < 0.07:
            z -= 0.06
            continue
        col = rgb(book_cols[rng.integers(0, len(book_cols))]) * rng.uniform(0.85, 1.1)
        top = min(b0 + 0.025 + ht, b1 - 0.01)
        room.box(SH_X0 + 0.02, SH_X1 - 0.03, b0 + 0.025, top, z - th, z, col, top=col * 0.8, side=col)
        z -= th + 0.004
for b in boards:
    room.box(SH_X0, SH_X1, b, b + 0.025, shelf_z0, shelf_z1, '#6a4230', top='#8a5a3e', edge='#c08458')
room.box(SH_X0, SH_X1 + 0.005, 0.0, 1.36, shelf_z0 - 0.02, shelf_z0, '#7a4a32', side='#8a5a3a', edge='#d09868')
room.box(SH_X0, SH_X1 + 0.02, 1.36, 1.41, shelf_z0 - 0.04, shelf_z1, '#6a4230', side='#8a5a3e', edge='#d09868')

# ---------------------------------------------------------------- Wandleuchte links hinten
s = room.proj(*SCONCE)
cv.paint(MaskPen(W, H).ellipse(s[0] - 2, s[1] - 3, s[0] + 2, s[1] + 2).a > 0, rgb('#ffd890'))
cv.add(glow(W, H, [(s[0], s[1], 1.0)], 7, '#ffa050', 0.45, steps=3))

# ---------------------------------------------------------------- Pflanze neben dem Fenster
pot = room.box(-0.55, -0.38, 0.0, 0.2, 2.3, 2.47, '#a85a3a', top='#5a3a2a', edge='#e09060')
pc = room.proj(-0.465, 0.2, 2.38)
leaves = np.zeros((H, W), bool)
for i in range(16):
    a = rng.uniform(-2.6, -0.5)
    ln = rng.uniform(14, 30)
    ex, ey = pc[0] + np.cos(a) * ln * 0.8, pc[1] + np.sin(a) * ln
    pen = MaskPen(W, H).line([pc, ((pc[0] + ex) / 2, (pc[1] + ey) / 2 - 2), (ex, ey)], 3)
    leaves |= pen.a > 0
cv.paint(leaves, rgb('#2e4a2e') * 1.3)
cv.paint(leaves & ~np.roll(leaves, 1, axis=1), rgb('#5a8a4a'))
cv.paint(leaves & ~np.roll(leaves, 1, axis=0), rgb('#7aa05a'))

# ---------------------------------------------------------------- Bild an der rechten Wand
fr = room.quad([(1.05, 0.62, 1.15), (1.05, 1.08, 1.15), (1.05, 1.08, 1.5), (1.05, 0.62, 1.5)], rgb('#c8944a') * 0.8)
pic = room.quad([(1.05, 0.66, 1.18), (1.05, 1.04, 1.18), (1.05, 1.04, 1.47), (1.05, 0.66, 1.47)], rgb('#3a5a7a'), mask_only=True)
pt = np.clip((yy - yy[pic].min()) / max(1, np.ptp(yy[pic])), 0, 1) if pic.any() else yy * 0
cv.paint(pic, dramp([(0, '#e8a070'), (0.5, '#c06a6a'), (0.62, '#4a5a8a'), (1, '#2a3a5a')], pt, 8) * 0.9)

# ---------------------------------------------------------------- Sessel mit Stehlampe (rechts)
# Stehlampe
room.box(0.965, 0.995, 0.0, 0.9, 1.95, 1.98, '#2a1e18')
lp = [room.proj(0.86, 0.9, 1.96), room.proj(1.04, 0.9, 1.96), room.proj(1.0, 1.06, 1.96), room.proj(0.9, 1.06, 1.96)]
cv.paint(MaskPen(W, H).poly(lp).a > 0, rgb('#f0b060'))
cv.paint(MaskPen(W, H).line([lp[0], lp[1]]).a > 0, rgb('#fff0c0'))
lc = room.proj(*LAMP)
cv.add(glow(W, H, [(lc[0], lc[1] + 6, 1.0)], 16, '#ff9a40', 0.32, steps=3))
cv.add(glow(W, H, [(lc[0], lc[1] + 4, 1.0)], 5, '#ffd090', 0.35, steps=2))

SEAT = '#2e5a4a'

room.box(0.44, 0.94, 0.05, 0.62, 1.70, 1.84, SEAT, top='#3e7a62', edge='#5a9a7a')   # Lehne
for k in range(3):
    a = room.proj(0.44 + 0.03 * k, 0.62 + 0.02 * (2 - k), 1.70)
    b = room.proj(0.94 - 0.03 * k, 0.62 + 0.02 * (2 - k), 1.70)
    cv.paint(MaskPen(W, H).line([a, b], 2).a > 0, lerp(rgb('#2e5a4a'), rgb('#5a9a7a'), 0.3 + 0.3 * k) * room.light_at((0.7, 0.62, 1.7), (0, 0.5, -1)) * 1.6)
for xs in (0.61, 0.77):
    a, b = room.proj(xs, 0.3, 1.70), room.proj(xs, 0.6, 1.70)
    cv.paint(MaskPen(W, H).line([a, b]).a > 0, rgb('#1a3a2e'))
room.box(0.44, 0.94, 0.05, 0.28, 1.36, 1.72, SEAT, top='#3e7a62', edge='#6aa888')  # Sitz
room.box(0.40, 0.50, 0.05, 0.40, 1.34, 1.80, '#265040', top='#3e7a62', edge='#6aa888')  # Armlehne innen
room.box(0.90, 0.98, 0.05, 0.40, 1.34, 1.80, '#265040', top='#3e7a62')
# Kissen
cush = room.box(0.58, 0.76, 0.28, 0.44, 1.64, 1.69, '#c8904a', top='#e0b070', edge='#f0d090')
for xs in (0.47, 0.88):
    room.box(xs, xs + 0.03, 0.0, 0.05, 1.36, 1.39, '#3a2418')
# Beistelltisch mit Tasse und Buechern
room.box(0.12, 0.36, 0.38, 0.42, 1.22, 1.46, '#7a4a30', top='#9a6444', edge='#d09868')
room.box(0.22, 0.25, 0.0, 0.38, 1.32, 1.35, '#4a2c1e')
room.box(0.15, 0.31, 0.42, 0.46, 1.28, 1.40, '#2a4a7a', top='#3a5a8a', edge='#8aa0c0')
room.box(0.17, 0.30, 0.46, 0.49, 1.30, 1.39, '#8a2a2e', top='#a83a3a', edge='#d07a6a')
room.box(0.26, 0.31, 0.49, 0.56, 1.30, 1.34, '#e8e0d0', top='#f8f0e0')
# ---------------------------------------------------------------- Katze auf dem Teppich
cat = room.proj(-0.15, 0.0, 1.55)
cm = MaskPen(W, H).ellipse(cat[0] - 13, cat[1] - 8, cat[0] + 11, cat[1] + 1).a > 0
cm |= MaskPen(W, H).ellipse(cat[0] + 6, cat[1] - 11, cat[0] + 16, cat[1] - 2).a > 0
cm |= MaskPen(W, H).poly([(cat[0] + 8, cat[1] - 9), (cat[0] + 9, cat[1] - 15), (cat[0] + 12, cat[1] - 10)]).a > 0
cm |= MaskPen(W, H).poly([(cat[0] + 12, cat[1] - 10), (cat[0] + 15, cat[1] - 15), (cat[0] + 16, cat[1] - 8)]).a > 0
cv.paint(MaskPen(W, H).ellipse(cat[0] - 15, cat[1] - 2, cat[0] + 15, cat[1] + 3).a > 0, cv.c * 0.6)
cv.paint(cm, rgb('#d07a3a') * 1.05)
cv.paint(cm & (yy < cat[1] - 5) & ~np.roll(cm, 1, axis=0), rgb('#f0b070'))
cv.paint(cm & (((xx - cat[0]) // 3) % 2 == 0) & (yy < cat[1] - 3) & (xx < cat[0] + 4), rgb('#a85a2a'))
cv.c[int(cat[1] - 6), int(cat[0] + 10)] = rgb('#3a2018')
cv.c[int(cat[1] - 6), int(cat[0] + 13)] = rgb('#3a2018')

# ---------------------------------------------------------------- Vignette und Palette
vig = np.hypot((xx - W / 2) / (W * 0.6), (yy - H * 0.5) / (H * 0.7))
cv.c *= np.clip(1.1 - vig ** 2 * 0.5, 0.5, 1.15)[..., None]
cv.c = quantize(cv.c, 128)
out = sys.argv[1] if len(sys.argv) > 1 else 'reading_room.png'
cv.save(out)
cv.save(out.replace('.png', '_x3.png'), 3)
