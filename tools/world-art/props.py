"""Kleinkram fuer die Zimmer, jedes Stueck aus eigenem Zufall: Pflanzenarten, Bilder mit kleinen
gemalten Landschaften, Uhr, Spiegel, Buecherstapel, Koerbe, Decken und Kissen mit Muster, Vasen,
Geschirr, Tischlampen, Vorhaenge, Lichterketten. Alle Funktionen nehmen das Zimmer (roomkit.CozyRoom)
und Raumkoordinaten (X quer, Y hoch, Z in die Tiefe)."""
import numpy as np
from px import rgb, lerp, glow, MaskPen, dramp
import nature as N

W, H = 480, 270


def _c(x):
    return rgb(x) if isinstance(x, str) else np.asarray(x, float)


def pen():
    return MaskPen(W, H)


# ------------------------------------------------------------------ Pflanzen
def plant(r, X, Z, kind=None, size=1.0, pot=None, Y=0.0):
    """Topfpflanze: monstera, snake, fern, palm, cactus, geranium, herbs, ficus."""
    rng = r.rng
    kind = kind or rng.choice(['monstera', 'snake', 'fern', 'palm', 'cactus', 'geranium', 'ficus'])
    pot = pot or rng.choice(['#a85a3a', '#c8b8a0', '#5a7a8a', '#3a3a3e', '#d8c8a8', '#8a5a3a'])
    pw = 0.07 * size
    ph = 0.15 * size
    r.box(X - pw, X + pw, Y, Y + ph, Z - pw, Z + pw, pot, top='#3a2418', edge=lerp(_c(pot), _c('#ffffff'), 0.35))
    band = r.room.proj(X - pw, Y + ph * 0.8, Z - pw), r.room.proj(X + pw, Y + ph * 0.8, Z - pw)
    r.cv.paint(pen().line(list(band)).a > 0, _c(pot) * 0.8)
    base = r.room.proj(X, Y + ph, Z)
    s = 230 / Z * size          # Pixel je Welteinheit
    light = r.room.light_at(np.array([X, Y + ph + 0.2, Z]), (0, 0.3, -1))
    lk = np.clip(light.mean() * 1.4, 0.45, 1.25)
    if kind == 'monstera':
        for _ in range(7):
            a = rng.uniform(-2.7, -0.4)
            ln = rng.uniform(0.18, 0.32) * s
            stem_end = (base[0] + np.cos(a) * ln * 0.7, base[1] + np.sin(a) * ln * 0.75)
            r.cv.paint(pen().line([base, stem_end]).a > 0, _c('#3e6a2e') * lk)
            leaf = r_ellipse(r, stem_end[0], stem_end[1], ln * 0.32, ln * 0.24, a)
            r.cv.paint(leaf, _c(rng.choice(['#2e5a2a', '#3a6a30', '#2a4e26'])) * lk)
            hl = leaf & ~np.roll(leaf, 1, axis=0) & (r.xx < stem_end[0])
            r.cv.paint(hl, _c('#6a9a4a') * lk)
            for k in range(3):
                sa = a + rng.uniform(-1.6, 1.6)
                q = (stem_end[0] + np.cos(sa) * ln * 0.3, stem_end[1] + np.sin(sa) * ln * 0.24)
                r.cv.paint((pen().line([stem_end, q]).a > 0) & leaf, r.cv.c * 0.75)
    elif kind == 'snake':
        for k in range(7):
            x = base[0] + rng.uniform(-0.05, 0.05) * s
            hgt = rng.uniform(0.25, 0.45) * s
            w = max(1.5, 0.018 * s)
            lean = rng.uniform(-0.08, 0.08) * s
            m = pen().poly([(x - w, base[1]), (x + w, base[1]), (x + lean + w * 0.3, base[1] - hgt), (x + lean, base[1] - hgt - 2)]).a > 0
            r.cv.paint(m, _c('#2e5a34') * lk)
            band = m & (((r.yy + k * 2) % 4) == 0)
            r.cv.paint(band, _c('#5a8a4a') * lk)
            edge = m & ~np.roll(m, -1, axis=1)
            r.cv.paint(edge, _c('#c8c060') * lk)
    elif kind == 'fern':
        for k in range(13):
            a = -np.pi / 2 + (k - 6) / 6 * 1.5
            ln = rng.uniform(0.16, 0.28) * s
            pts = [(base[0] + np.cos(a) * ln * t + np.cos(a + 1.2) * ln * 0.25 * t * t * np.sign(np.cos(a)),
                    base[1] + np.sin(a) * ln * t + ln * 0.5 * t * t) for t in np.linspace(0, 1, 8)]
            r.cv.paint(pen().line(pts).a > 0, _c('#3e6a2e') * lk)
            for j, (px, py) in enumerate(pts[1:], 1):
                for sd in (-1, 1):
                    q = (px + sd * 2.2 * (1 - j / 9) * size, py - 1.5)
                    r.cv.paint(pen().line([(px, py), q]).a > 0, _c(rng.choice(['#4e8a3a', '#5a9a44', '#3e7a34'])) * lk)
    elif kind == 'palm':
        for k in range(8):
            a = -np.pi / 2 + (k - 3.5) / 3.5 * 1.3
            ln = rng.uniform(0.22, 0.36) * s
            tip = (base[0] + np.cos(a) * ln, base[1] + np.sin(a) * ln + ln * 0.35)
            mid = (base[0] + np.cos(a) * ln * 0.55, base[1] + np.sin(a) * ln * 0.55 - 2)
            r.cv.paint(pen().line([base, mid, tip]).a > 0, _c('#4a7a3a') * lk)
            for t in np.linspace(0.25, 0.95, 7):
                px = base[0] + (tip[0] - base[0]) * t
                py = base[1] + (tip[1] - base[1]) * t - np.sin(t * np.pi) * 3
                for sd in (-1, 1):
                    r.cv.paint(pen().line([(px, py), (px + sd * 2.5, py + 2.5)]).a > 0, _c('#5a8a44') * lk)
    elif kind == 'cactus':
        h = 0.2 * s
        body = r_ellipse(r, base[0], base[1] - h * 0.5, max(2, 0.035 * s), h * 0.5, 0)
        for sd in (-1, 1):
            body |= r_ellipse(r, base[0] + sd * 0.05 * s, base[1] - h * 0.55, max(1.2, 0.02 * s), h * 0.22, 0)
        r.cv.paint(body, _c('#4a7a4a') * lk)
        r.cv.paint(body & (((r.xx - int(base[0])) % 3) == 0), _c('#3a6a3a') * lk)
        r.cv.paint(body & ~np.roll(body, 1, axis=1), _c('#7aa86a') * lk)
        r.cv.c[int(base[1] - h), int(base[0])] = _c('#e8508a')
    elif kind in ('geranium', 'herbs', 'ficus'):
        cols = ('#1e3a22', '#2a4e2a', '#3e6a34', '#5a8a42', '#7aa654', '#9cc068')
        rad = (0.1 if kind != 'ficus' else 0.16) * s
        cy = base[1] - rad * (0.8 if kind != 'ficus' else 1.6)
        if kind == 'ficus':
            r.cv.paint(pen().line([base, (base[0] + 1, cy)], max(1, int(s * 0.01))).a > 0, _c('#5a4030'))
        m = N.leaf_mass(r.cv, base[0], cy, rad, rng, cols=cols)
        r.cv.c[m] *= lk
        if kind == 'geranium':
            ys, xs = np.nonzero(m)
            for i in rng.integers(0, max(1, len(xs)), 16 if len(xs) else 0):
                r.cv.c[ys[i], xs[i]] = _c(rng.choice(['#e8304a', '#f05a6a', '#ff8a9a']))
    return base


def r_ellipse(r, cx, cy, rx, ry, rot):
    x, y = r.xx - cx + 0.5, r.yy - cy + 0.5
    c, s = np.cos(rot), np.sin(rot)
    x, y = x * c + y * s, -x * s + y * c
    return (x / max(rx, 0.5)) ** 2 + (y / max(ry, 0.5)) ** 2 <= 1


def trailing(r, x, y, length=30, rng=None):
    """Haengende Efeutute von einem Regal oder Topf herab."""
    rng = rng or r.rng
    for k in range(6):
        vx = x + rng.uniform(-8, 8)
        ln = rng.uniform(length * 0.4, length)
        pts = [(x + (vx - x) * t, y + ln * t + np.sin(t * 6 + k) * 2) for t in np.linspace(0, 1, 10)]
        r.cv.paint(pen().line(pts).a > 0, _c('#3a5a2a'))
        for px, py in pts[1::2]:
            sd = rng.choice([-1, 1])
            lm = pen().ellipse(px + sd * 2 - 1.5, py - 1, px + sd * 2 + 1.5, py + 1).a > 0
            r.cv.paint(lm, _c(rng.choice(['#4e8a3a', '#6a9a44', '#3e7a34', '#8ab05a'])))


# ------------------------------------------------------------------ Wand
def painting(r, X0, X1, Y0, Y1, kind=None, frame='#8a5a2a', side=0, Z0=None, Z1=None):
    """Bild mit kleiner gemalter Landschaft: Meer, Berge, Wald, Stillleben, Portrait-Silhouette."""
    rng = r.rng
    kind = kind or rng.choice(['sea', 'mountain', 'forest', 'flowers', 'sunset'])
    if side == 0:
        x0, y0, x1, y1 = r.rect_on_back(X0, X1, Y0, Y1)
        outer = (r.xx >= x0 - 2) & (r.xx <= x1 + 2) & (r.yy >= y0 - 2) & (r.yy <= y1 + 2)
        inner = (r.xx >= x0) & (r.xx <= x1) & (r.yy >= y0) & (r.yy <= y1)
    else:
        outer = r.wall_side(side, Z0 - 0.03, Z1 + 0.03, Y0 - 0.03, Y1 + 0.03, _c(frame))
        inner = r.room.quad([(side * r.room.half_w, Y0, Z0), (side * r.room.half_w, Y1, Z0),
                             (side * r.room.half_w, Y1, Z1), (side * r.room.half_w, Y0, Z1)], _c('#000000'), mask_only=True)
        if not inner.any():
            return
        ys, xs = np.nonzero(inner)
        x0, y0, x1, y1 = xs.min(), ys.min(), xs.max(), ys.max()
    fr = _c(frame)
    r.cv.paint(outer & ~inner, fr)
    r.cv.paint(outer & ~inner & (r.yy <= y0 - 1), lerp(fr, _c('#ffe0a0'), 0.4))
    t = np.clip((r.yy - y0) / max(1, y1 - y0), 0, 1)
    u = np.clip((r.xx - x0) / max(1, x1 - x0), 0, 1)
    if kind == 'sea':
        col = dramp([(0, '#8ab8e0'), (0.45, '#e8e0c8'), (0.5, '#3a6a9a'), (1, '#2a4a7a')], t, 6)
        col = np.where(((t > 0.5) & (((r.xx + r.yy * 3) % 7) == 0))[..., None], _c('#c8e0f0'), col)
        sail = (np.abs(u - 0.6) < 0.08 * (0.5 - np.abs(t - 0.35) * 4).clip(0)) & (t > 0.25) & (t < 0.48)
        col = np.where(sail[..., None], _c('#f8f4ec'), col)
    elif kind == 'mountain':
        ridge = 0.5 - 0.35 * (1 - np.abs(u - 0.4) * 2.2).clip(0)
        col = dramp([(0, '#a8c8e8'), (1, '#e8eef4')], t, 4)
        col = np.where((t > ridge)[..., None], dramp([(0, '#6a7a9a'), (1, '#4a5a7a')], t, 3), col)
        col = np.where(((t > ridge) & (t < ridge + 0.08))[..., None], _c('#f4f4f8'), col)
        col = np.where((t > 0.8)[..., None], _c('#4a7a3a'), col)
    elif kind == 'forest':
        col = dramp([(0, '#f0d8a0'), (1, '#e8b880')], t, 4)
        tree = ((u * 9) % 1 - 0.5) ** 2 * 3 < (t - 0.25).clip(0)
        col = np.where(tree[..., None], dramp([(0, '#3a5a3a'), (1, '#2a3a2a')], t, 3), col)
    elif kind == 'flowers':
        col = dramp([(0, '#3a3040'), (1, '#2a2430')], t, 3)
        vase = (np.abs(u - 0.5) < 0.18) & (t > 0.55)
        col = np.where(vase[..., None], _c('#5a7aa8'), col)
        for k in range(7):
            fx, fy = 0.3 + 0.4 * rng.random(), 0.15 + 0.35 * rng.random()
            blob = ((u - fx) ** 2 + (t - fy) ** 2 * 0.6) < 0.008
            col = np.where(blob[..., None], _c(rng.choice(['#e8506a', '#f0c040', '#f4f0e0', '#e88aa0'])), col)
    else:
        col = dramp([(0, '#4a3a6a'), (0.45, '#e86a4a'), (0.6, '#f0b060'), (0.62, '#3a3a4a'), (1, '#2a2a3a')], t, 6)
        sun = (u - 0.55) ** 2 + (t - 0.55) ** 2 < 0.012
        col = np.where((sun & (t < 0.6))[..., None], _c('#fff0b0'), col)
    r.cv.paint(inner, col * 0.95)
    r.cv.paint(inner & ((r.xx - r.yy) % 19 == 0), r.cv.c * 1.08)


def clock(r, X, Y, rad=0.09):
    c = r.room.proj(X, Y, r.room.back)
    rp = rad * 230 / r.room.back
    m = pen().ellipse(c[0] - rp, c[1] - rp, c[0] + rp, c[1] + rp).a > 0
    r.cv.paint(m, _c('#7a4a2a'))
    face = pen().ellipse(c[0] - rp + 2, c[1] - rp + 2, c[0] + rp - 2, c[1] + rp - 2).a > 0
    r.cv.paint(face, _c('#f0e6d0'))
    for k in range(12):
        a = k / 12 * 2 * np.pi
        r.cv.c[int(c[1] + np.sin(a) * (rp - 3)), int(c[0] + np.cos(a) * (rp - 3))] = _c('#5a4a3a')
    r.cv.paint(pen().line([c, (c[0] + rp * 0.5, c[1] - rp * 0.2)]).a > 0, _c('#2a2a2a'))
    r.cv.paint(pen().line([c, (c[0] - rp * 0.1, c[1] - rp * 0.65)]).a > 0, _c('#2a2a2a'))


def string_lights(r, X0, X1, Y, Z, n=14, sag=0.06, cols=('#ffe090', '#ffc070', '#fff4c0')):
    pts = []
    for k in range(n):
        t = k / (n - 1)
        X = X0 + (X1 - X0) * t
        y = Y - sag * np.sin(np.pi * t)
        pts.append(r.room.proj(X, y, Z))
    r.cv.paint(pen().line(pts).a > 0, _c('#3a2a20'))
    for k, p in enumerate(pts):
        c = cols[k % len(cols)]
        r.cv.paint(pen().ellipse(p[0] - 1, p[1], p[0] + 1, p[1] + 2).a > 0, _c(c))
        r.cv.add(glow(W, H, [(p[0], p[1] + 1, 1.0)], 3, c, 0.3, steps=2))


def curtains(r, X0, X1, Y0, Y1, col='#9a3a3e', tie=True):
    """Vorhaenge links und rechts eines Rueckwandfensters, mit Falten, Raffhalter und Stange."""
    for a, b in ((X0 - 0.14, X0 + 0.02), (X1 - 0.02, X1 + 0.14)):
        x0, y0, x1, y1 = r.rect_on_back(a, b, Y0 - 0.1, Y1 + 0.05)
        cur = (r.xx >= x0) & (r.xx <= x1) & (r.yy >= y0) & (r.yy <= y1)
        if tie:
            mid = y0 + (y1 - y0) * 0.6
            pinch = np.abs(r.yy - mid) / max(1, (y1 - y0)) * 2
            wcur = (x1 - x0) * (0.45 + 0.55 * np.clip(pinch, 0, 1))
            cur &= (r.xx - x0 <= wcur) if a < X0 else (x1 - r.xx <= wcur)
        fold = (np.sin((r.xx - x0) * 1.3) + 1) / 2
        light = r.room.light_at(np.array([(a + b) / 2, (Y0 + Y1) / 2, r.room.back - 0.02]), (0, 0, -1))
        r.cv.paint(cur, lerp(_c(col) * 0.6, _c(col) * 1.15, fold[..., None]) * light * 1.3)
        if tie:
            r.cv.paint(cur & (np.abs(r.yy - mid) < 1), _c('#d8b060'))
    x0, y0, x1, y1 = r.rect_on_back(X0 - 0.18, X1 + 0.18, Y1 + 0.06, Y1 + 0.075)
    r.cv.paint((r.xx >= x0) & (r.xx <= x1) & (r.yy >= y0) & (r.yy <= y1 + 1), _c('#3a2418'))
    for xx in (x0, x1):
        r.cv.paint(pen().ellipse(xx - 2, y0 - 1, xx + 2, y1 + 2).a > 0, _c('#c8a050'))


def wall_shelf_clutter(r, X0, X1, Y, Z=None, depth=0.12):
    """Wandbord an der Rueckwand mit gemischtem Kram: Glaeser, Buecher, Topf mit Ranke, Kerze."""
    rng = r.rng
    Zb = r.room.back if Z is None else Z
    r.box(X0, X1, Y - 0.025, Y, Zb - depth, Zb, '#6a4230', top='#8a5a3e', edge='#d09868')
    for x in (X0 + 0.05, X1 - 0.07):
        a = r.room.proj(x, Y - 0.025, Zb - 0.01)
        r.cv.paint(pen().poly([a, (a[0] + 3, a[1]), (a[0] + 3, a[1] + 4)]).a > 0, _c('#4a2e20'))
    X = X0 + 0.02
    while X < X1 - 0.06:
        kind = rng.choice(['jar', 'books', 'pot', 'candle', 'box', 'bottle'])
        if kind == 'jar':
            c = _c(rng.choice(['#c8a060', '#a8c8b8', '#e8d0a0', '#c87a4a']))
            r.box(X, X + 0.06, Y, Y + rng.uniform(0.06, 0.1), Zb - depth + 0.02, Zb - 0.02, c * 0.95, top=_c('#8a6a4a'), edge=c * 1.3)
            X += 0.08
        elif kind == 'books':
            n = int(rng.integers(3, 7))
            for k in range(n):
                c = _c(rng.choice(['#8a2a2e', '#2a4a7a', '#3a6a4a', '#c08a3a', '#6a3a6a', '#d0b070']))
                th = rng.uniform(0.018, 0.03)
                r.box(X, X + th, Y, Y + rng.uniform(0.09, 0.13), Zb - depth + 0.01, Zb - 0.01, c, top=c * 0.8, side=c)
                X += th + 0.003
            X += 0.02
        elif kind == 'pot':
            r.box(X, X + 0.07, Y, Y + 0.06, Zb - depth + 0.02, Zb - 0.02, '#a85a3a', top='#3a2418', edge='#e09060')
            p = r.room.proj(X + 0.035, Y + 0.06, Zb - depth / 2)
            trailing(r, p[0], p[1], length=rng.uniform(14, 26), rng=rng)
            X += 0.1
        elif kind == 'candle':
            r.box(X, X + 0.03, Y, Y + 0.07, Zb - 0.07, Zb - 0.04, '#f0e8d0', top='#fff8e0')
            r.glow_at(X + 0.015, Y + 0.09, Zb - 0.06, r=4, color='#ffb050', k=0.4)
            X += 0.05
        elif kind == 'box':
            c = _c(rng.choice(['#8a6a4a', '#5a7a8a', '#a87a5a']))
            r.box(X, X + 0.09, Y, Y + 0.05, Zb - depth + 0.01, Zb - 0.01, c, top=c * 1.1, edge=c * 1.3)
            X += 0.11
        else:
            c = _c(rng.choice(['#3a6a5a', '#6a3a2a', '#2a4a6a']))
            r.box(X, X + 0.035, Y, Y + 0.1, Zb - 0.06, Zb - 0.025, c, top=c, edge=c * 1.6)
            r.box(X + 0.01, X + 0.025, Y + 0.1, Y + 0.13, Zb - 0.05, Zb - 0.035, c * 0.8)
            X += 0.055


# ------------------------------------------------------------------ Textilien
def cushion(r, X0, X1, Y0, Y1, Z0, Z1, col, pattern=None):
    m = r.box(X0, X1, Y0, Y1, Z0, Z1, col, top=_c(col) * 1.1, edge=_c(col) * 1.3)
    front = r.room.quad([(X0, Y0, Z0), (X1, Y0, Z0), (X1, Y1, Z0), (X0, Y1, Z0)], _c('#000000'), mask_only=True)
    pattern = pattern or r.rng.choice(['stripes', 'checks', 'dots', 'plain'])
    pc = lerp(_c(col), _c('#f4ecd8'), 0.55)
    if pattern == 'stripes':
        r.cv.paint(front & ((r.xx // 2) % 3 == 0), pc)
    elif pattern == 'checks':
        r.cv.paint(front & (((r.xx // 2) + (r.yy // 2)) % 2 == 0), lerp(_c(col), pc, 0.5))
    elif pattern == 'dots':
        r.cv.paint(front & ((r.xx % 3 == 0) & (r.yy % 3 == 0)), pc)
    return m


def blanket(r, X0, X1, Y, Z0, Z1, col='#c86a4a', col2='#f0d8a0'):
    """Gefaltete Decke mit Fransen ueber einer Kante."""
    m = r.room.quad([(X0, Y, Z0), (X1, Y, Z0), (X1, Y - 0.18, Z0 - 0.01), (X0, Y - 0.18, Z0 - 0.01)], _c(col))
    r.cv.paint(m & (((r.xx + r.yy) // 2) % 4 == 0), _c(col2))
    r.cv.paint(m & ((r.yy % 5) == 0), _c(col) * 0.75)
    ys, xs = np.nonzero(m)
    if len(ys):
        yb = ys.max()
        for x in range(xs.min(), xs.max() + 1, 2):
            r.cv.paint((r.xx == x) & (r.yy > yb) & (r.yy <= yb + 2), _c(col2))


def basket(r, X, Z, w=0.14, h=0.12, Y=0.0, fill=None):
    rng = r.rng
    m = r.box(X - w / 2, X + w / 2, Y, Y + h, Z - w / 2, Z + w / 2, '#b8884a', top='#5a3a22', edge='#e0b070')
    weave = m & ((((r.xx + r.yy) % 3) == 0) | (((r.xx - r.yy) % 3) == 0))
    r.cv.paint(weave, r.cv.c * 0.8)
    if fill == 'yarn':
        for k in range(3):
            p = r.room.proj(X - w / 4 + k * w / 4, Y + h + 0.02, Z)
            r.cv.paint(pen().ellipse(p[0] - 3, p[1] - 3, p[0] + 3, p[1] + 2).a > 0, _c(rng.choice(['#c84a5a', '#4a7ac8', '#e8c040', '#7ab06a'])))
    elif fill == 'logs':
        for k in range(4):
            p = r.room.proj(X - w / 3 + k * w / 4.5, Y + h + 0.01, Z - 0.02)
            r.cv.paint(pen().ellipse(p[0] - 3, p[1] - 3, p[0] + 3, p[1] + 2).a > 0, _c('#c89a68'))
            r.cv.paint(pen().ellipse(p[0] - 1, p[1] - 1, p[0] + 1, p[1]).a > 0, _c('#8a6040'))
    elif fill == 'blanket':
        p = r.room.proj(X, Y + h, Z - w / 2)
        r.cv.paint(pen().ellipse(p[0] - 9, p[1] - 6, p[0] + 9, p[1] + 2).a > 0, _c('#d8c0a0'))
        r.cv.paint((pen().ellipse(p[0] - 9, p[1] - 6, p[0] + 9, p[1] + 2).a > 0) & ((r.xx // 2) % 3 == 0), _c('#a85a4a'))
    return m


# ------------------------------------------------------------------ Tisch
def vase(r, X, Y, Z, flowers=('#e8506a', '#f4f0e0', '#f0c040'), col='#5a7aa8'):
    r.box(X - 0.03, X + 0.03, Y, Y + 0.1, Z - 0.03, Z + 0.03, col, top=_c(col) * 0.7, edge=lerp(_c(col), _c('#ffffff'), 0.4))
    p = r.room.proj(X, Y + 0.1, Z)
    rng = r.rng
    for k in range(9):
        a = rng.uniform(-2.5, -0.6)
        ln = rng.uniform(6, 12) * 1.5 / Z
        q = (p[0] + np.cos(a) * ln, p[1] + np.sin(a) * ln)
        r.cv.paint(pen().line([p, q]).a > 0, _c('#4e7a34'))
        c = _c(flowers[k % len(flowers)])
        r.cv.paint(pen().ellipse(q[0] - 1.5, q[1] - 1.5, q[0] + 1.5, q[1] + 1.5).a > 0, c)
        r.cv.c[int(q[1]) - 1, int(q[0]) - 1] = lerp(c, _c('#ffffff'), 0.4)


def teapot(r, X, Y, Z, col='#c86a4a'):
    p = r.room.proj(X, Y, Z)
    s = 1.4 / Z
    body = pen().ellipse(p[0] - 5 * s, p[1] - 8 * s, p[0] + 5 * s, p[1]).a > 0
    r.cv.paint(body, _c(col))
    r.cv.paint(body & (r.xx < p[0] - 1) & (r.yy < p[1] - 5 * s), lerp(_c(col), _c('#ffffff'), 0.35))
    r.cv.paint(pen().line([(p[0] + 4 * s, p[1] - 4 * s), (p[0] + 8 * s, p[1] - 7 * s)], max(1, int(s))).a > 0, _c(col) * 0.85)
    r.cv.paint(pen().ellipse(p[0] - 1.5 * s, p[1] - 10 * s, p[0] + 1.5 * s, p[1] - 8 * s).a > 0, _c(col) * 0.8)
    r.cv.paint(pen().line([(p[0] - 5 * s, p[1] - 6 * s), (p[0] - 7 * s, p[1] - 4 * s), (p[0] - 5 * s, p[1] - 2 * s)]).a > 0, _c(col) * 0.75)


def fruit_bowl(r, X, Y, Z):
    p = r.room.proj(X, Y, Z)
    s = 1.4 / Z
    r.cv.paint(pen().ellipse(p[0] - 7 * s, p[1] - 3 * s, p[0] + 7 * s, p[1] + 1).a > 0, _c('#e8e0d0'))
    for k, c in enumerate(('#e8503a', '#f0c040', '#7ab04a', '#e88030', '#a83a5a')):
        q = (p[0] - 5 * s + k * 2.4 * s, p[1] - 4 * s - (k % 2) * 1.5 * s)
        m = pen().ellipse(q[0] - 2 * s, q[1] - 2 * s, q[0] + 2 * s, q[1] + 2 * s).a > 0
        r.cv.paint(m, _c(c))
        r.cv.c[int(q[1] - s), int(q[0] - s)] = lerp(_c(c), _c('#ffffff'), 0.5)


def table_lamp(r, X, Y, Z, shade='#e8c890'):
    r.box(X - 0.03, X + 0.03, Y, Y + 0.03, Z - 0.03, Z + 0.03, '#8a6a3a', top='#a8884a')
    r.box(X - 0.008, X + 0.008, Y + 0.03, Y + 0.16, Z - 0.008, Z + 0.008, '#c8a050')
    a, b = r.room.proj(X - 0.07, Y + 0.16, Z), r.room.proj(X + 0.07, Y + 0.16, Z)
    c, d = r.room.proj(X + 0.045, Y + 0.25, Z), r.room.proj(X - 0.045, Y + 0.25, Z)
    m = pen().poly([a, b, c, d]).a > 0
    r.cv.paint(m, _c(shade))
    r.cv.paint(m & ((r.xx % 3) == 0), _c(shade) * 0.88)
    r.cv.paint(pen().line([a, b]).a > 0, _c('#fff4d0'))
    r.glow_at(X, Y + 0.2, Z, r=14, color='#ffa850', k=0.35)
    r.glow_at(X, Y + 0.12, Z, r=5, color='#fff0c0', k=0.3)


def book_stack(r, X, Y, Z, n=4, lean=False):
    rng = r.rng
    y = Y
    for k in range(n):
        c = _c(rng.choice(['#8a2a2e', '#2a4a7a', '#3a6a4a', '#c08a3a', '#6a3a6a', '#d0b070', '#4a2a3a']))
        th = rng.uniform(0.02, 0.035)
        dx = rng.uniform(-0.02, 0.02)
        w = rng.uniform(0.13, 0.18)
        r.box(X - w / 2 + dx, X + w / 2 + dx, y, y + th, Z - 0.07, Z + 0.07, c, top=c * 1.1, edge=_c('#f0e6d0'))
        y += th
    return y


def mug(r, X, Y, Z, col=None, steam=False):
    col = col or r.rng.choice(['#e8e0d0', '#c86a4a', '#5a8aa8', '#e8c060', '#7aa86a'])
    r.cup(X, Y, Z, col)
    p = r.room.proj(X + 0.03, Y + 0.035, Z)
    r.cv.paint(pen().ellipse(p[0], p[1] - 1.5, p[0] + 2.5, p[1] + 1.5).a > 0, _c(col) * 0.85)
    if steam:
        q = r.room.proj(X, Y + 0.08, Z)
        for k in range(3):
            r.cv.c[int(q[1] - k * 2), int(q[0] + (k % 2))] = lerp(r.cv.c[int(q[1] - k * 2), int(q[0] + (k % 2))], _c('#ffffff'), 0.5)


def moon_patch(r, X0, X1, Z0, Z1, col='#6a80c0', k=0.18):
    """Lichtfleck des Fensters auf dem Boden (Mond oder Sonne)."""
    fl = (r.which == 0) & (r.X > X0) & (r.X < X1) & (r.Z > Z0) & (r.Z < Z1)
    mull = (np.abs(r.X - (X0 + X1) / 2) < 0.015) | (np.abs(r.Z - (Z0 + Z1) / 2) < 0.02)
    r.cv.c[fl & ~mull] = lerp(r.cv.c[fl & ~mull], _c(col), k)
