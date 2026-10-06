"""Stadt als Einzelstuecke nach dem Uferviertel-Blatt: Haeuser aus Putz und Bruchstein, Ziegeldach
in Moench-und-Nonne-Reihen, jedes Fenster anders (Laeden offen oder zu, Vorhang, Licht, Blumen),
Efeu mit Blueten, Wandlaternen, Ladenfenster mit Regalen dahinter, Markisen mit Bogenkante,
Toepfe mit verschiedenen Pflanzen. Alles nimmt die Szene (kit.Scene) entgegen."""
import numpy as np
from scipy import ndimage
from px import rgb, lerp, noise, dramp, glow
import nature as N

W, H = 480, 270


def _c(x):
    return rgb(x) if isinstance(x, str) else np.asarray(x, float)


def stone_wall_texture(s, m, rng, base=('#c8b494', '#d8c6a6', '#e8d8ba', '#f4e8d0'), stones=0.35, seed=0):
    """Putz mit Wolken und Flecken; wo er abgeplatzt ist, schaut Bruchstein heraus."""
    n = noise(W, H, 3, seed + 3, 3)
    t = np.clip(n * 0.6 + 0.4 * (1 - np.clip((s.yy - s.yy[m].min()) / max(1, np.ptp(s.yy[m])), 0, 1)) if m.any() else n, 0, 1)
    col = dramp([(0, base[0]), (0.4, base[1]), (0.75, base[2]), (1, base[3])], t, 5)
    s.cv.paint(m, col)
    # Bruchstein in Flecken
    patch = m & (noise(W, H, 9, seed + 5, 2) < stones * 0.6)
    bx = (s.xx + (s.yy // 4) * 3) // 6
    by = s.yy // 4
    sid = (bx * 7919 + by * 104729) % 97
    stone_col = lerp(_c('#a08a70'), _c('#c8b296'), (sid / 97)[..., None])
    s.cv.paint(patch, stone_col)
    joint = patch & (((s.xx + (s.yy // 4) * 3) % 6 == 0) | (s.yy % 4 == 0))
    s.cv.paint(joint, _c('#7a6a58'))
    # Regenspuren unter Fensterbaenken und Flecken
    stain = m & (noise(W, H, 2, seed + 9, 2) > 0.8)
    s.cv.paint(stain, s.cv.c * 0.93)


def roof(s, x0, x1, y_top, rng, rh=12, cols=('#6a2e20', '#8a3e2a', '#a8503a', '#c8664a', '#e08a64'), chimney=None):
    """Ziegeldach mit Ueberstand: Reihen aus gewoelbten Ziegeln, jede etwas anders getoent,
    ein paar dunkle (alte) Ziegel, Moos in Kanten."""
    m = s.poly([(x0 - 6, y_top + 2), (x0 + 4, y_top - rh), (x1 - 4, y_top - rh), (x1 + 6, y_top + 2)])
    cols = [_c(c) for c in cols]
    row = (s.yy - (y_top - rh)) // 3
    col_i = (s.xx + (row % 2) * 2) // 4
    tid = (col_i * 2654435761 + row * 40503) % 1000 / 1000
    arch = ((s.xx + (row % 2) * 2) % 4) / 3.0      # 0 links .. 1 rechts im Ziegel
    lit = 1 - np.abs(arch - 0.35) * 1.3
    t = np.clip(lit * 0.6 + tid * 0.35 + (s.yy - (y_top - rh)) / rh * 0.15, 0, 1)
    t = np.where(tid > 0.93, t * 0.5, t)
    idx = np.clip((t * (len(cols) - 0.01)).astype(int), 0, len(cols) - 1)
    s.cv.paint(m, np.array(cols)[idx])
    s.cv.paint(m & ((s.yy - (y_top - rh)) % 3 == 2), cols[0])
    moss = m & (noise(W, H, 3, int(x0) + 77, 2) > 0.78) & ((s.yy - (y_top - rh)) % 3 == 2)
    s.paint(moss, '#5a6a34')
    s.paint(s.rect(x0 - 6, y_top + 2, x1 + 6, y_top + 3), '#4a2418')
    if chimney:
        cx = chimney
        ch = s.rect(cx, y_top - rh - 12, cx + 7, y_top - rh + 4)
        stone_wall_texture(s, ch, rng, stones=1.0, seed=cx)
        s.paint(s.rect(cx - 1, y_top - rh - 14, cx + 8, y_top - rh - 12), '#8a3e2a')
        s.paint(s.rect(cx + 6, y_top - rh - 12, cx + 7, y_top - rh + 4), '#6a5a48')


def window(s, x, y0, y1, rng, shutters='#4e8a86', lit=False, boxes=True, style=None):
    """Fenster mit eigener Art: Laeden offen, halb oder zu, Gardine, Licht, Blumenkasten."""
    style = style or rng.choice(['open', 'open', 'half', 'curtain', 'closed'])
    w = 8
    glass = s.rect(x, y0, x + w, y1)
    t = np.clip((s.yy - y0) / max(1, y1 - y0), 0, 1)
    if lit:
        s.cv.paint(glass, dramp([(0, '#ffe8b0'), (1, '#e8a050')], t, 3))
    else:
        s.cv.paint(glass, dramp([(0, '#2e3a4a'), (0.5, '#4a5a70'), (1, '#6a7e98')], t, 3))
        s.paint(s.m().line([(x + 1, y1 - 2), (x + 4, y0 + 1)]).a > 0, '#a8bcd0')
    if style == 'curtain':
        cur = s.rect(x, y0, x + 2, y1) | s.rect(x + w - 2, y0, x + w, y1)
        s.paint(cur, rng.choice(['#e8dcc8', '#d8a8a0', '#c8d8c0']))
    s.paint(s.rect(x + 4, y0, x + 4, y1), '#e8dcc4')
    s.paint(s.rect(x, (y0 + y1) // 2, x + w, (y0 + y1) // 2), '#e8dcc4')
    s.paint(s.rect(x - 1, y0 - 1, x + w + 1, y0 - 1), '#b8a488')
    sc = _c(shutters) * rng.uniform(0.85, 1.1)
    hi = lerp(sc, _c('#ffffff'), 0.3)
    if style in ('open', 'curtain'):
        for sx in (x - 5, x + w + 2):
            s.cv.paint(s.rect(sx, y0, sx + 3, y1), sc)
            s.cv.paint(s.rect(sx, y0, sx, y1), hi)
            for yy in range(y0 + 1, y1, 2):
                s.cv.paint(s.rect(sx + 1, yy, sx + 2, yy), sc * 0.75)
    elif style == 'half':
        s.cv.paint(s.rect(x - 5, y0, x - 2, y1), sc)
        s.cv.paint(s.rect(x + w // 2 + 1, y0, x + w, y1), sc)
        s.cv.paint(s.rect(x + w // 2 + 1, y0, x + w // 2 + 1, y1), hi)
    elif style == 'closed':
        s.cv.paint(s.rect(x, y0, x + w, y1), sc)
        for yy in range(y0 + 1, y1, 2):
            s.cv.paint(s.rect(x, yy, x + w, yy), sc * 0.75)
        s.cv.paint(s.rect(x + 4, y0, x + 4, y1), sc * 0.6)
    s.paint(s.rect(x - 2, y1 + 1, x + w + 2, y1 + 2), '#c8b090')
    s.paint(s.rect(x - 2, y1 + 3, x + w + 2, y1 + 3), '#8a7a62')
    if boxes and rng.random() < 0.8:
        box_c = rng.choice(['#8a5a36', '#6a7a4a', '#a8603a'])
        s.paint(s.rect(x - 2, y1 + 1, x + w + 2, y1 + 4), box_c)
        palettes = [['#e8506a', '#f4f0e0'], ['#e86a3a', '#f0c040'], ['#9a7ae0', '#f4f0e0'],
                    ['#e88aa0', '#ffffff', '#c84a6a']]
        palette = palettes[int(rng.integers(0, len(palettes)))]
        for k in range(x - 2, x + w + 3):
            hgt = int(rng.integers(1, 4))
            for j in range(hgt):
                s.paint(s.rect(k, y1 - j, k, y1 - j), '#3e6a2e' if (k + j) % 2 else '#5a8a3a')
            if rng.random() < 0.55:
                s.paint(s.rect(k, y1 - hgt, k, y1 - hgt), rng.choice(palette))
            if rng.random() < 0.25:
                for j in range(int(rng.integers(2, 6))):
                    s.paint(s.rect(k, y1 + 5 + j, k, y1 + 5 + j), '#4e7a34' if j % 2 else '#6a9a40')


def vine(s, x, y_bottom, y_top, rng, spread=14, bloom=('#e86a9a', '#f4f0e0', '#9a7ae0')):
    """Efeu oder Kletterrose, die an der Wand hochwaechst: Ranke, Blattbueschel, Blueten."""
    pts = [(x, y_bottom)]
    for k in range(10):
        px_, py_ = pts[-1]
        pts.append((px_ + rng.uniform(-3, 3), py_ - (y_bottom - y_top) / 10))
    s.paint(s.m().line(pts, 1).a > 0, '#4a3a22')
    for px_, py_ in pts[1:]:
        for _ in range(int(rng.integers(4, 9))):
            lx = px_ + rng.normal(0, spread * 0.35)
            ly = py_ + rng.normal(0, 3)
            c = rng.choice(['#2e5a2a', '#3e7230', '#5a8a3a', '#7aa648'])
            s.paint(s.rect(int(lx), int(ly), int(lx) + 1, int(ly)), c)
            if rng.random() < 0.18:
                s.paint(s.rect(int(lx), int(ly) - 1, int(lx), int(ly) - 1), rng.choice(bloom))


def wall_lantern(s, x, y, lit=True):
    s.paint(s.rect(x - 3, y, x, y), '#2a2a2a')
    s.paint(s.rect(x - 4, y + 1, x - 1, y + 6), '#2a2a2a')
    s.paint(s.rect(x - 3, y + 2, x - 2, y + 5), '#ffd890' if lit else '#c8c0a0')
    s.paint(s.rect(x - 5, y, x, y), '#2a2a2a')
    if lit:
        s.cv.add(glow(W, H, [(x - 2.5, y + 3.5, 1.0)], 6, '#ffb050', 0.45, steps=3))


def shop_interior(s, x0, y0, x1, y1, rng, kind='cafe'):
    """Was man durchs Schaufenster sieht: warmes Licht, Regale mit Dingen, Haengelampen."""
    t = np.clip((s.yy - y0) / max(1, y1 - y0), 0, 1)
    win = s.rect(x0, y0, x1, y1)
    s.cv.paint(win, dramp([(0, '#f8dca0'), (0.6, '#e0a860'), (1, '#a86a3a')], t, 4))
    for sy in range(y0 + 6, y1 - 3, 7):
        s.paint(s.rect(x0, sy, x1, sy), '#6a4228')
        x = x0 + 1
        while x < x1 - 1:
            if kind == 'books':
                w = int(rng.integers(1, 3))
                h = int(rng.integers(3, 6))
                s.paint(s.rect(x, sy - h, x + w - 1, sy - 1), rng.choice(['#8a3a3a', '#3a5a8a', '#4a7a4a', '#c8a050', '#6a4a7a', '#e8d8b0']))
                x += w
            else:
                if rng.random() < 0.5:
                    s.paint(s.rect(x, sy - 3, x + 1, sy - 1), rng.choice(['#f4ece0', '#c8704a', '#5a8a8a', '#e8c070']))
                x += 3
    for lx in range(x0 + 6, x1 - 2, 18):
        s.paint(s.rect(lx, y0, lx, y0 + 3), '#3a2a20')
        s.paint(s.rect(lx - 2, y0 + 4, lx + 2, y0 + 5), '#fff0c0')
        s.cv.add(glow(W, H, [(lx, y0 + 6, 1.0)], 5, '#ffc070', 0.35, steps=2) * win[..., None])
    # Spiegelung im Glas
    refl = win & ((s.xx - s.yy) % 23 < 2)
    s.cv.c[refl] = lerp(s.cv.c[refl], _c('#ffffff'), 0.25)


def facade(s, x0, x1, y_top, y_base, rng, wall=('#c8b494', '#d8c6a6', '#e8d8ba', '#f4e8d0'),
           shutters='#4e8a86', floors=2, door_x=None, shop=None, chimney=True, vines=1, lit=False,
           lanterns=True):
    """Haus von vorn als Einzelstueck. Gibt die Trefferflaeche der Tuer zurueck (oder None)."""
    m = s.rect(x0, y_top, x1, y_base)
    stone_wall_texture(s, m, rng, base=wall, stones=rng.uniform(0.2, 0.5), seed=int(x0) + 11)
    # Ecksteine
    for cx in (x0, x1 - 4):
        for k, y in enumerate(range(y_top, y_base, 5)):
            wq = 4 if k % 2 == 0 else 3
            s.paint(s.rect(cx if cx == x0 else cx + 4 - wq, y, (cx + wq) if cx == x0 else cx + 4, y + 3),
                    lerp(_c('#b8a488'), _c('#d0bea0'), rng.random()))
            s.paint(s.rect(cx, y + 4, cx + 4, y + 4), '#8a7a62')
    roof(s, x0, x1, y_top, rng, chimney=(x1 - 22 if chimney else None))
    floor_h = (y_base - y_top) / floors
    door = None
    for f in range(floors):
        fy = y_top + f * floor_h
        ground = f == floors - 1
        if ground and shop:
            sx0, sx1 = x0 + 6, (door_x - 4) if door_x else x1 - 6
            wy0 = int(fy + floor_h * 0.3)
            shop_interior(s, sx0, wy0, sx1, y_base - 3, rng, kind=shop if isinstance(shop, str) else 'cafe')
            for gx in range(sx0, sx1 + 1, 16):
                s.paint(s.rect(gx, wy0, gx + 1, y_base - 3), '#4a2e1e')
            s.paint(s.rect(sx0 - 2, wy0 - 2, sx1 + 2, wy0), '#4a2e1e')
            s.paint(s.rect(sx0 - 2, y_base - 3, sx1 + 2, y_base), '#5a3a26')
        else:
            n = max(1, int((x1 - x0 - 16) / 22))
            for k in range(n):
                wx = int(x0 + 10 + k * (x1 - x0 - 20) / n + rng.uniform(-2, 2))
                window(s, wx, int(fy + floor_h * 0.28), int(fy + floor_h * 0.75), rng, shutters=shutters,
                       lit=lit and rng.random() < 0.6)
    if door_x:
        dw, dh = 11, int(floor_h * 0.74)
        d = s.rect(door_x, y_base - dh, door_x + dw, y_base)
        s.shade(d, ['#3a2418', '#5a3a26', '#7a5034', '#946440'], light=(-1, -0.3))
        for k in range(door_x + 2, door_x + dw, 3):
            s.paint(s.rect(k, y_base - dh + 2, k, y_base - 1), '#4a2e1e')
        s.paint(s.rect(door_x + 2, y_base - dh + 3, door_x + dw - 2, y_base - dh + 8), '#f0c880' if lit or shop else '#4a5a70')
        s.paint(s.rect(door_x - 1, y_base - dh - 2, door_x + dw + 1, y_base - dh), '#b8a488')
        s.paint(s.rect(door_x + dw - 3, y_base - dh // 2, door_x + dw - 2, y_base - dh // 2), '#e8c070')
        s.paint(s.rect(door_x - 2, y_base, door_x + dw + 2, y_base + 1), '#a89a88')
        door = (door_x - 2, y_base - dh - 2, door_x + dw + 2, y_base)
        if lanterns:
            wall_lantern(s, door_x - 3, y_base - dh - 2, lit=True)
    for _ in range(vines):
        vx = rng.choice([x0 + 3, x1 - 4, rng.uniform(x0 + 10, x1 - 10)])
        vine(s, vx, y_base, y_top + rng.uniform(0, floor_h), rng)
    return door


def awning(s, x0, x1, y, depth=12, c1='#3e7a6a', c2=None, scallop=True):
    """Stoffmarkise: schraeg, mit Faltenlicht, Bogenkante und Schatten darunter."""
    c1 = _c(c1)
    c2 = _c(c2) if c2 is not None else c1 * 1.12
    m = s.poly([(x0, y), (x1, y), (x1 + 4, y + depth), (x0 - 4, y + depth)])
    stripe = ((s.xx - x0 + (s.yy - y) * 0.3) // 6) % 2 == 0
    t = np.clip((s.yy - y) / depth, 0, 1)
    base = np.where(stripe[..., None], c1, c2)
    fold = (np.sin((s.xx - x0) * 0.55) * 0.5 + 0.5)[..., None]
    col = base * (0.82 + 0.25 * t[..., None]) * (0.92 + 0.12 * fold)
    s.cv.paint(m, col)
    s.cv.paint(m & (s.yy == y), lerp(c1, _c('#ffffff'), 0.35))
    sh = s.poly([(x0 - 4, y + depth), (x1 + 4, y + depth), (x1 + 2, y + depth + 6), (x0 - 2, y + depth + 6)])
    s.cv.c[sh] *= 0.72
    if scallop:
        for x in range(int(x0 - 4), int(x1 + 4), 6):
            sc = s.ellipse(x + 3, y + depth + 0.5, 3, 2.2) & (s.yy > y + depth)
            s.cv.paint(sc, (c1 if ((x - x0) // 6) % 2 == 0 else c2) * 0.85)
    for x in (x0 - 3, x1 + 3):
        s.paint(s.m().line([(x, y + depth), (x - 2 if x < x1 else x + 2, y + 2)]).a > 0, '#2a2a2a')


def hanging_sign(s, x, y, w=14, h=12, col='#3e5a7a', icon='book'):
    s.paint(s.rect(x - 6, y - 2, x + w, y - 1), '#2a2a2a')
    s.paint(s.m().line([(x - 6, y - 1), (x - 2, y - 6)]).a > 0, '#2a2a2a')
    s.paint(s.rect(x, y, x, y + 1), '#2a2a2a')
    s.paint(s.rect(x + w - 2, y, x + w - 2, y + 1), '#2a2a2a')
    s.paint(s.rect(x - 1, y + 2, x + w - 1, y + 2 + h), '#c8a050')
    s.paint(s.rect(x, y + 3, x + w - 2, y + 1 + h), col)
    if icon == 'book':
        s.paint(s.rect(x + 3, y + 5, x + w - 5, y + h - 2), '#e8d8b0')
        s.paint(s.rect(x + (w - 2) // 2, y + 5, x + (w - 2) // 2, y + h - 2), '#8a6a4a')
    elif icon == 'cup':
        s.paint(s.rect(x + 3, y + 6, x + w - 6, y + h - 2), '#f4ece0')
        s.paint(s.rect(x + w - 5, y + 7, x + w - 4, y + 8), '#f4ece0')
        s.paint(s.rect(x + 4, y + 4, x + 4, y + 5), '#d8d0c0')


POTS = {
    'terracotta': ('#7a3a22', '#a85a3a', '#c8784a', '#e09a68'),
    'blue': ('#2a3a6a', '#3e5a8a', '#5a7aa8', '#8aa8c8'),
    'stone': ('#6a645c', '#8a847a', '#aaa498', '#ccc6b8'),
    'cream': ('#a89880', '#c8b8a0', '#e0d4c0', '#f4ece0'),
}


def potted(s, x, y, rng, size=1.0, kind=None, pot=None):
    """Topfpflanze als Einzelstueck: Lavendel, Geranie, Olivenbaeumchen, Farn, Buchs."""
    kind = kind or rng.choice(['lavender', 'geranium', 'olive', 'fern', 'box', 'daisies'])
    pot = pot or rng.choice(list(POTS))
    pc = POTS[pot]
    pw, ph = 6 * size, 7 * size
    body = s.poly([(x - pw, y - ph), (x + pw, y - ph), (x + pw * 0.75, y), (x - pw * 0.75, y)])
    s.shadow(x + 2, y, pw * 1.4, 1.5)
    s.shade(body, list(pc), light=(-1, -0.3))
    s.paint(s.rect(x - pw - 1, y - ph - 1, x + pw + 1, y - ph + 1), pc[3])
    top = y - ph - 1
    if kind == 'lavender':
        for k in range(int(10 * size)):
            sx = x + rng.uniform(-pw, pw)
            hgt = rng.uniform(6, 12) * size
            s.paint(s.m().line([(sx, top), (sx + rng.uniform(-2, 2), top - hgt)]).a > 0, '#6a8a5a')
            s.paint(s.rect(int(sx), int(top - hgt), int(sx), int(top - hgt + 3 * size)), rng.choice(['#8a6ad8', '#a88ae8', '#7a5ac8']))
    elif kind == 'olive':
        s.paint(s.m().line([(x, top), (x - 1, top - 10 * size)], max(1, int(size))).a > 0, '#5a4a3a')
        N.leaf_mass(s.cv, x, top - 14 * size, 7 * size, rng, cols=('#3a4a3a', '#5a6a50', '#7a8a6a', '#9aa888', '#c0c8a8', '#dce0c8'))
    elif kind == 'fern':
        for a in np.linspace(-2.8, -0.3, 9):
            ln = rng.uniform(8, 13) * size
            ex, ey = x + np.cos(a) * ln, top + np.sin(a) * ln * 0.8
            s.paint(s.m().line([(x, top), ((x + ex) / 2, (top + ey) / 2 - 2), (ex, ey + 2)]).a > 0, rng.choice(['#3e7a3a', '#5a9a44']))
    elif kind == 'box':
        N.leaf_mass(s.cv, x, top - 5 * size, 7 * size, rng, cols=('#1e3a22', '#2a4e2a', '#3e6a34', '#5a8a42', '#7aa654', '#9cc068'), squash=0.9)
    else:
        m = N.leaf_mass(s.cv, x, top - 5 * size, 7.5 * size, rng, cols=('#24402a', '#355a32', '#4c7a3c', '#6c9a4a', '#94ba62', '#b8d080'))
        bloom = {'geranium': ['#e8304a', '#f05a6a'], 'daisies': ['#f4f0e0', '#ffffff']}[kind]
        ys, xs = np.nonzero(m)
        for i in rng.integers(0, max(1, len(xs)), int(14 * size) if len(xs) else 0):
            s.paint(s.rect(xs[i], ys[i], xs[i], ys[i]), rng.choice(bloom))
            if kind == 'daisies':
                s.paint(s.rect(xs[i], ys[i], xs[i], ys[i]), '#f0c030') if rng.random() < 0.3 else None


def ornate_lamp(s, x, y, h=58, lit=False, basket=True, banner='#3e5a8a'):
    """Gusseiserne Laterne wie im Uferviertel: Sockel, kannelierter Mast, Arm mit Blumenampel
    und Wimpel, Laternenkopf. Gibt die Trefferflaeche zurueck."""
    iron, hi = '#262a2e', '#4a5258'
    s.shadow(x + 3, y, 6, 1.6, 0.6)
    s.paint(s.rect(x - 3, y - 4, x + 3, y), iron)
    s.paint(s.rect(x - 2, y - 7, x + 2, y - 4), iron)
    s.paint(s.rect(x - 1, y - h, x + 1, y - 7), iron)
    s.paint(s.rect(x - 1, y - h, x - 1, y - 7), hi)
    for yy in (y - h * 0.35, y - h * 0.7):
        s.paint(s.rect(x - 2, int(yy), x + 2, int(yy) + 1), iron)
    top = y - h
    s.paint(s.poly([(x - 4, top), (x + 4, top), (x + 3, top - 9), (x - 3, top - 9)]), '#ffd890' if lit else '#e0d8b8')
    s.paint(s.rect(x - 1, top - 9, x - 1, top), iron)
    s.paint(s.rect(x - 5, top - 11, x + 5, top - 9), iron)
    s.paint(s.poly([(x - 3, top - 11), (x + 3, top - 11), (x, top - 15)]), iron)
    s.paint(s.rect(x - 5, top, x + 5, top + 1), iron)
    if lit:
        s.cv.add(glow(W, H, [(x, top - 5, 1.0)], 8, '#ffb050', 0.5, steps=3))
    if basket:
        ay = int(top + 10)
        s.paint(s.rect(x + 1, ay, x + 9, ay), iron)
        s.paint(s.rect(x + 8, ay, x + 8, ay + 3), iron)
        rng = np.random.default_rng(int(x) * 3 + 1)
        m = N.leaf_mass(s.cv, x + 8, ay + 7, 4.5, rng, cols=('#24402a', '#355a32', '#4c7a3c', '#6c9a4a', '#94ba62', '#b8d080'))
        ys, xs = np.nonzero(m)
        for i in rng.integers(0, max(1, len(xs)), 10 if len(xs) else 0):
            s.paint(s.rect(xs[i], ys[i], xs[i], ys[i]), rng.choice(['#e8506a', '#f4f0e0', '#f0a0c0']))
        for j in range(5):
            s.paint(s.rect(x + 6 + (j % 3), ay + 11 + j, x + 6 + (j % 3), ay + 11 + j), '#4e7a34')
    if banner:
        by = int(top + 18)
        s.paint(s.rect(x - 9, by, x - 2, by), iron)
        s.paint(s.poly([(x - 9, by + 1), (x - 3, by + 1), (x - 3, by + 14), (x - 6, by + 11), (x - 9, by + 14)]), banner)
        s.paint(s.rect(x - 7, by + 4, x - 5, by + 7), '#e8c050')
    return (int(x - 10), int(top - 16), int(x + 11), y)


def cafe_set(s, x, y, rng, scale=1.0):
    """Runder Bistrotisch mit zwei Stuehlen, Tasse, Teller, Blumenvase."""
    k = scale
    s.shadow(x + 2, y + 1, 16 * k, 2.5 * k, 0.6)
    for cx, flip in ((x - 13 * k, 1), (x + 12 * k, -1)):
        seat_y = int(y - 8 * k)
        s.paint(s.rect(int(cx - 4 * k), seat_y, int(cx + 4 * k), seat_y + 1), '#8a5a36')
        s.paint(s.rect(int(cx - 4 * k), seat_y, int(cx + 4 * k), seat_y), '#c8925a')
        bx = int(cx - 4 * k) if flip > 0 else int(cx + 4 * k)
        s.paint(s.rect(bx, int(y - 20 * k), bx + (1 if flip > 0 else 0), y), '#3a2a1e')
        for yy in range(int(y - 19 * k), seat_y - 1, 3):
            s.paint(s.rect(min(bx, int(cx)), yy, max(bx, int(cx)), yy), '#6a4430')
        s.paint(s.rect(int(cx + 3 * k * flip), seat_y, int(cx + 3 * k * flip), y), '#3a2a1e')
    s.paint(s.ellipse(x, y - 12 * k, 10 * k, 2.8 * k), '#2e2e30')
    s.paint(s.ellipse(x, y - 12.6 * k, 9.5 * k, 2.4 * k), '#e8e0d0')
    s.paint(s.ellipse(x - 2, y - 13.2 * k, 6 * k, 1.2 * k), '#f8f4ec')
    s.paint(s.rect(x, int(y - 11 * k), x + 1, y), '#2e2e30')
    s.paint(s.rect(int(x - 4 * k), y - 1, int(x + 5 * k), y), '#2e2e30')
    s.paint(s.rect(int(x - 5 * k), int(y - 15 * k), int(x - 3 * k), int(y - 13 * k)), '#f4f0e8')
    s.paint(s.rect(int(x - 5 * k), int(y - 15 * k), int(x - 3 * k), int(y - 15 * k)), '#6a3a1e')
    s.paint(s.rect(int(x + 2 * k), int(y - 16 * k), int(x + 3 * k), int(y - 13 * k)), '#a8c8d0')
    s.paint(s.rect(int(x + 2 * k), int(y - 18 * k), int(x + 3 * k), int(y - 17 * k)), rng.choice(['#e8506a', '#f0c040', '#f4f0e0']))


def chalkboard(s, x, y, lines=4):
    s.paint(s.poly([(x - 1, y), (x + 3, y - 20), (x + 13, y - 20), (x + 17, y)]), '#5a3a26')
    s.paint(s.rect(x + 3, y - 18, x + 13, y - 4), '#26302c')
    for k in range(lines):
        w = (5, 8, 6, 7, 4)[k % 5]
        s.paint(s.rect(x + 5, y - 16 + k * 3, x + 5 + w, y - 16 + k * 3), '#e8e0d0' if k else '#f0c070')
    s.paint(s.rect(x + 11, y - 7, x + 12, y - 6), '#e8a0a0')
