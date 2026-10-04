"""Einzelstuecke statt Stempel: Wolken, Laubbaeume, Tannen, Graeser, Blumen und Steine, jedes
Exemplar aus seinem eigenen Zufall gebaut (Form, Farbe, Aeste, Luecken), geformt ueber ein
Hoehenfeld und von oben links beleuchtet.

Grundgedanke: Eine Krone oder Wolke ist eine Vereinigung vieler Kugeln. Das Hoehenfeld aus allen
Kugeln liefert die Normale (Licht je Bueschel), seine Falten liefern Schatten zwischen den
Bueschen (Umgebungsverdeckung). Gerechnet wird nur im Rechteck um das Objekt.
"""
import numpy as np
from scipy import ndimage
from px import rgb, lerp, BAYER4

LIGHT = np.array([-0.55, -0.7, 0.45])
LIGHT = LIGHT / np.linalg.norm(LIGHT)


def hexes(*cols):
    return [rgb(c) if isinstance(c, str) else np.asarray(c, float) for c in cols]


def jitter_ramp(cols, rng, hue=0.06, val=0.08):
    """Dieselbe Rampe, je Exemplar leicht anders getoent - kein Baum gleicht dem anderen."""
    shift = np.array([rng.uniform(-hue, hue), rng.uniform(-hue * 0.5, hue * 0.5), rng.uniform(-hue, hue)])
    k = 1 + rng.uniform(-val, val)
    return [np.clip(c * k + shift * c, 0, 1) for c in hexes(*cols)]


class Window:
    """Ausschnitt der Leinwand um ein Objekt."""

    def __init__(self, cv, x0, y0, x1, y1):
        H, W = cv.c.shape[:2]
        self.cv = cv
        self.x0, self.y0 = max(0, int(np.floor(x0))), max(0, int(np.floor(y0)))
        self.x1, self.y1 = min(W, int(np.ceil(x1)) + 1), min(H, int(np.ceil(y1)) + 1)
        self.ok = self.x1 > self.x0 and self.y1 > self.y0
        if not self.ok:
            self.x1, self.y1 = max(self.x1, self.x0), max(self.y1, self.y0)
        self.yy, self.xx = np.mgrid[self.y0:self.y1, self.x0:self.x1]
        self.yy = self.yy.astype(float)
        self.xx = self.xx.astype(float)
        by = np.tile(BAYER4, (H // 4 + 2, W // 4 + 2))
        self.by = by[self.y0:self.y1, self.x0:self.x1]

    @property
    def c(self):
        return self.cv.c[self.y0:self.y1, self.x0:self.x1]

    def paint(self, mask, color, alpha=1.0):
        if not self.ok:
            return
        sub = self.cv.c[self.y0:self.y1, self.x0:self.x1]
        col = color if np.ndim(color) == 3 else np.broadcast_to(np.asarray(color, float), sub.shape)
        m = np.asarray(mask, bool)
        sub[m] = sub[m] * (1 - alpha) + col[m] * alpha


def sphere_field(win, blobs):
    """Hoehenfeld aus Kugeln (x, y, rx, ry, lift) - Maximum ueber alle, dazu wer oben liegt."""
    h = np.full(win.xx.shape, -1.0)
    who = np.full(win.xx.shape, -1)
    for i, (bx, by, rx, ry, lift) in enumerate(blobs):
        d2 = ((win.xx - bx) / rx) ** 2 + ((win.yy - by) / ry) ** 2
        z = np.where(d2 < 1, np.sqrt(np.clip(1 - d2, 0, 1)) * min(rx, ry) + lift, -1.0)
        upd = z > h
        h = np.where(upd, z, h)
        who = np.where(upd, i, who)
    return h, who


def lit_from_field(h, mask, ao=0.6, flat=0.55):
    """Helligkeit 0..1 aus dem Hoehenfeld: Lambert zur Lichtrichtung, Falten verschattet."""
    hs = np.where(mask, h, 0)
    gy, gx = np.gradient(hs)
    n = np.stack([-gx, -gy, np.full_like(hs, flat)], -1)
    n /= np.linalg.norm(n, axis=-1, keepdims=True) + 1e-9
    lam = np.clip((n * LIGHT).sum(-1), 0, 1)
    occ = ndimage.gaussian_filter(hs, 3) - hs
    lam = lam - np.clip(occ, 0, None) * ao * 0.08
    return np.clip(lam, 0, 1)


def to_ramp(t, cols, by, dither=0.45):
    cols = np.array(cols)
    k = len(cols)
    idx = np.clip(np.floor(t * (k - 0.01) + by * dither), 0, k - 1).astype(int)
    return cols[idx]


# ---------------------------------------------------------------------------------- Wolken
def cumulus(cv, cx, base, width, rng, height=None, cols=('#9aa8c4', '#c4cee0', '#e8eef6', '#ffffff'),
            rim=None, sky_y=None):
    """Haufenwolke: flacher Boden, Tuerme aus vielen Kugeln, oben links hell, unten bläulich."""
    height = height or width * rng.uniform(0.35, 0.6)
    blobs = []
    n = int(width / 6) + 4
    for _ in range(n):
        t = rng.uniform(-1, 1)
        x = cx + t * width * 0.5
        env = (1 - abs(t) ** 1.6)
        r = rng.uniform(0.25, 0.5) * height * (0.4 + 0.6 * env)
        y = base - r * 0.55 - env * height * rng.uniform(0.2, 0.6)
        blobs.append((x, y, r * rng.uniform(1.0, 1.35), r, 0.0))
    for _ in range(n // 2):
        x = cx + rng.uniform(-0.5, 0.5) * width
        r = rng.uniform(0.12, 0.22) * height
        blobs.append((x, base - r * 0.6, r * 1.6, r * 0.7, 0.0))
    win = Window(cv, cx - width * 0.7, base - height * 1.5, cx + width * 0.7, base + 2)
    if not win.ok:
        return
    h, _ = sphere_field(win, blobs)
    mask = (h >= 0) & (win.yy <= base)
    h = ndimage.gaussian_filter(np.where(mask, h, -height * 0.15), 1.3)
    # Ausgefranster Rand
    edge = mask & ~ndimage.binary_erosion(mask)
    mask &= ~(edge & (rng.random(mask.shape) < 0.35))
    # Licht dringt von oben links ein: je mehr Wolke in Richtung Sonne liegt, desto dunkler
    m = mask.astype(float)
    pad = 24
    mp = np.pad(m, pad)
    acc = np.zeros_like(m)
    for k in range(1, 18):
        dy, dx = k, int(round(k * 0.8))
        acc += mp[pad - dy:pad - dy + m.shape[0], pad - dx:pad - dx + m.shape[1]]
    acc = ndimage.gaussian_filter(acc, 1.2)
    vol = np.exp(-acc * 0.09)
    lam = lit_from_field(h, mask, ao=1.4, flat=0.9)
    t = vol * 0.55 + lam * 0.6
    bottom = np.clip((win.yy - (base - height * 0.3)) / (height * 0.3), 0, 1)
    t = np.clip(t * 0.95 + 0.08 - bottom * 0.2 + (rng.random(m.shape) - 0.5) * 0.08, 0, 1)
    col = to_ramp(t, hexes(*cols), win.by, 0.6)
    win.paint(mask, col)
    if rim:
        top = mask & ~np.roll(mask, 1, axis=0) & (win.xx < cx + width * 0.1)
        win.paint(top, np.broadcast_to(rgb(rim), win.c.shape))


def cirrus(cv, x0, y, length, rng, col='#f4f6fa', k=0.5):
    win = Window(cv, x0, y - 4, x0 + length, y + 4)
    if not win.ok:
        return
    for _ in range(int(length / 10)):
        sx = rng.uniform(x0, x0 + length * 0.8)
        ln = rng.uniform(8, 30)
        sy = y + rng.uniform(-3, 3)
        m = (np.abs(win.yy - sy - (win.xx - sx) * 0.05) < 0.6) & (win.xx >= sx) & (win.xx <= sx + ln)
        win.paint(m, np.broadcast_to(rgb(col), win.c.shape), k)


# ---------------------------------------------------------------------------------- Baeume
OAK = ('#1a2e1e', '#284426', '#3a5e2e', '#548036', '#7aa246', '#a8c862')
BARK = ('#2e2018', '#46322a', '#634838', '#836046', '#a07a5a')


def _limb(win, pts, w0, w1, cols, rng):
    """Ast als Polylinie mit abnehmender Dicke, Licht von links, Rinde in Rissen."""
    m = np.zeros(win.xx.shape, bool)
    shade_t = np.zeros(win.xx.shape)
    n = len(pts)
    for k in range(n - 1):
        (xa, ya), (xb, yb) = pts[k], pts[k + 1]
        wa = w0 + (w1 - w0) * k / (n - 1)
        wb = w0 + (w1 - w0) * (k + 1) / (n - 1)
        dx, dy = xb - xa, yb - ya
        L = max(np.hypot(dx, dy), 1e-6)
        t = np.clip(((win.xx - xa) * dx + (win.yy - ya) * dy) / (L * L), 0, 1)
        px, py = xa + dx * t, ya + dy * t
        d = np.hypot(win.xx - px, win.yy - py)
        wd = wa + (wb - wa) * t
        seg = d <= wd
        # Querposition -1..1 zur Lichtseite (Normale nach links oben)
        nx, ny = -dy / L, dx / L
        if nx * LIGHT[0] + ny * LIGHT[1] < 0:
            nx, ny = -nx, -ny
        side = ((win.xx - px) * nx + (win.yy - py) * ny) / np.maximum(wd, 0.5)
        shade_t = np.where(seg & ~m, np.clip(0.5 + side * 0.5, 0, 1), shade_t)
        m |= seg
    crack = (np.sin(win.xx * 1.7 + np.sin(win.yy * 0.4) * 2) > 0.75) & (rng.random(m.shape) < 0.7)
    t = np.clip(shade_t - crack * 0.3, 0, 1)
    win.paint(m, to_ramp(t, hexes(*cols), win.by, 0.5))
    return m


def oak(cv, x, base, height, rng, leaf=OAK, bark=BARK, spread=1.0, lean=None, crop_top=False,
        density=1.0, blossoms=None):
    """Laubbaum mit eigenem Astwerk: Stamm mit Wurzelanlauf, drei bis fuenf Hauptaeste, Kronen-
    bueschel an den Astenden, Luecken, durch die Aeste und Himmel scheinen."""
    leaf = jitter_ramp(leaf, rng)
    bark = jitter_ramp(bark, rng, hue=0.03)
    lean = rng.uniform(-0.15, 0.15) if lean is None else lean
    w0 = max(1.5, height * rng.uniform(0.045, 0.06))
    fork = base - height * rng.uniform(0.32, 0.45)
    fx = x + lean * height * 0.35
    crown_r = height * 0.42 * spread
    win = Window(cv, x - crown_r * 1.6, base - height * 1.15, x + crown_r * 1.6, base + 3)
    if not win.ok:
        return None
    # Stamm mit Wurzelanlauf
    trunk = [(x, base + 1), (x + lean * height * 0.1, base - height * 0.12), (fx, fork)]
    roots = []
    for s in (-1, 1):
        roots.append([(x + s * w0 * 2.6, base + 1), (x + s * w0 * 0.8, base - w0 * 1.6)])
    # Hauptaeste
    limbs, tips = [], []
    nl = int(rng.integers(3, 6))
    for k in range(nl):
        a = -np.pi / 2 + (k - (nl - 1) / 2) / max(1, (nl - 1) / 2) * rng.uniform(0.7, 1.05) + rng.uniform(-0.15, 0.15)
        ln = height * rng.uniform(0.32, 0.48) * spread
        pts = [(fx, fork)]
        ang = a
        for s in range(3):
            ang += rng.uniform(-0.25, 0.25)
            px_, py_ = pts[-1]
            seg = ln / 3
            pts.append((px_ + np.cos(ang) * seg * 1.15, py_ + np.sin(ang) * seg * 0.95))
        limbs.append(pts)
        tips.append(pts[-1])
        # Zweig
        if rng.random() < 0.8:
            bp = pts[2]
            ba = ang + rng.choice([-1, 1]) * rng.uniform(0.5, 0.9)
            bl = ln * rng.uniform(0.3, 0.45)
            twig = [bp, (bp[0] + np.cos(ba) * bl, bp[1] + np.sin(ba) * bl)]
            limbs.append(twig)
            tips.append(twig[-1])
    # Bueschel: an den Spitzen, entlang der aeusseren Aeste, ein paar zum Fuellen
    blobs = []
    cy = fork - crown_r * 0.6
    for tx, ty in tips:
        for _ in range(int(5 * density) + 3):
            r = height * rng.uniform(0.05, 0.09)
            blobs.append((tx + rng.normal(0, r * 0.8), ty + rng.normal(0, r * 0.6), r * rng.uniform(1.0, 1.3), r, rng.uniform(0, 3)))
    for _ in range(int(22 * density)):
        a = rng.uniform(0, 2 * np.pi)
        d = np.sqrt(rng.random()) * crown_r * 0.9
        r = height * rng.uniform(0.045, 0.09)
        blobs.append((fx + np.cos(a) * d * 1.2, cy + np.sin(a) * d * 0.75, r * 1.15, r, rng.uniform(0, 2)))
    # hinten liegende Bueschel zuerst, abgedunkelt
    order = rng.permutation(len(blobs))
    back = [blobs[i] for i in order[: len(blobs) // 3]]
    front = [blobs[i] for i in order[len(blobs) // 3:]]

    def crown(bl, dark):
        h, who = sphere_field(win, bl)
        mask = h >= 0
        # Blattbueschel: kleine Huegel im Hoehenfeld, ausgefranste Kante
        leafn = ndimage.uniform_filter(rng.random(mask.shape), 3)
        h = np.where(mask, h + (leafn - 0.5) * height * 0.06, h)
        edge = mask & ~ndimage.binary_erosion(mask, iterations=2)
        bite = edge & (ndimage.uniform_filter(rng.random(mask.shape), 2) < 0.42)
        mask &= ~bite
        # Blattkruemel ausserhalb des Randes
        out = ndimage.binary_dilation(mask) & ~mask & (rng.random(mask.shape) < 0.18)
        t = lit_from_field(h, mask, ao=1.2, flat=0.5)
        # Selbstschatten der ganzen Krone: unten rechts dunkler
        gl = ((win.xx - fx) * -LIGHT[0] + (win.yy - cy) * -LIGHT[1]) / max(crown_r, 1)
        t = np.clip(t * 1.15 - np.clip(gl, -1, 1) * 0.22 - dark, 0, 1)
        # Blattstruktur: kleine Flecken
        tex = (rng.random(mask.shape) - 0.5) * 0.28
        t = np.clip(t + ndimage.uniform_filter(tex, 2) * 1.4, 0, 1)
        col = to_ramp(t, leaf, win.by, 0.55)
        win.paint(mask, col)
        win.paint(out & (win.yy < cy), leaf[3])
        win.paint(out & (win.yy >= cy), leaf[1])
        return mask

    crown(back, 0.28)
    for r_ in roots:
        _limb(win, r_, w0 * 1.1, w0 * 0.6, bark, rng)
    _limb(win, trunk, w0 * 1.25, w0 * 0.85, bark, rng)
    for pts in limbs:
        _limb(win, pts, w0 * 0.7 * (1 if len(pts) > 2 else 0.6), max(0.6, w0 * 0.22), bark, rng)
    m = crown(front, 0.0)
    # Sonnenkanten: helle Blattpixel oben links an den Bueschen
    hi = m & ~np.roll(m, 1, axis=0) & ~np.roll(m, 1, axis=1) & (rng.random(m.shape) < 0.6)
    win.paint(hi, leaf[-1])
    if blossoms:
        bm = m & (rng.random(m.shape) < 0.03)
        win.paint(bm, rgb(blossoms))
    return (fx, cy, crown_r)


PINE = ('#142a24', '#1e3a30', '#2c4e3c', '#3e664a', '#5a845a')


def pine(cv, x, base, height, rng, cols=PINE, haze=None, k=0.0, snow=None):
    """Tanne aus Astetagen: jede Etage mit zackiger Unterkante, leicht schief, eigene Breite;
    links im Licht, rechts im Schatten, Stamm unten sichtbar."""
    cols = jitter_ramp(cols, rng, hue=0.05)
    if haze is not None:
        cols = [lerp(c, rgb(haze), k) for c in cols]
    w = height * rng.uniform(0.2, 0.28)
    win = Window(cv, x - w * 1.3, base - height * 1.05, x + w * 1.3, base + 2)
    if not win.ok:
        return
    trunk_h = height * rng.uniform(0.08, 0.14)
    tw = max(0.8, height * 0.025)
    tm = (np.abs(win.xx - x) <= tw) & (win.yy >= base - trunk_h - height * 0.1) & (win.yy <= base)
    win.paint(tm, lerp(rgb('#3a2a20'), cols[1], 0.3) if haze is None else lerp(rgb('#3a2a20'), rgb(haze), k))
    tiers = int(rng.integers(6, 10)) if height > 30 else int(rng.integers(4, 6))
    top = base - height
    bottom = base - trunk_h
    span = bottom - top
    lean = rng.uniform(-0.06, 0.06)
    for i in range(tiers):
        f = i / tiers
        yb = bottom - span * f * 0.92
        th = span / tiers * rng.uniform(1.5, 2.1)
        wd = w * (1 - f) ** 0.9 * rng.uniform(0.85, 1.1) + 1
        cxx = x + lean * (base - yb)
        rel = (win.yy - (yb - th)) / th          # 0 oben .. 1 Unterkante
        half = wd * np.clip(rel, 0, 1) ** 0.85
        teeth = (np.sin((win.xx - cxx) * rng.uniform(0.7, 1.2) + i * 1.7) * 0.5 + 0.5) * th * 0.25
        m = (rel >= 0) & (np.abs(win.xx - cxx) <= half) & (win.yy <= yb - teeth * (np.abs(win.xx - cxx) / max(wd, 1)))
        side = (win.xx - cxx) / max(wd, 1)
        t = np.clip(0.62 - side * 0.5 - rel * 0.35 + (rng.random(m.shape) - 0.5) * 0.3, 0, 1)
        win.paint(m, to_ramp(t, cols, win.by, 0.5))
        # Etagenkante unten etwas dunkler, oben links Lichtnadeln
        under = m & ~np.roll(m, -1, axis=0)
        win.paint(under, cols[0])
        hi = m & ~np.roll(m, 1, axis=0) & (side < 0.1) & (rng.random(m.shape) < 0.5)
        win.paint(hi, cols[-1])
        if snow:
            sm = m & ~np.roll(m, 2, axis=0) & (rng.random(m.shape) < 0.8)
            win.paint(sm, rgb(snow))


def birch(cv, x, base, height, rng):
    rng2 = rng
    oak(cv, x, base, height, rng2, leaf=('#4a5a20', '#6a7a2a', '#8a9a34', '#acb444', '#ccc860', '#ece08a'),
        bark=('#8a8478', '#b4aea0', '#d8d2c4', '#ece6da', '#fbf6ee'), spread=0.7, density=0.7)
    # dunkle Rindenmarken
    win = Window(cv, x - 4, base - height * 0.6, x + 4, base)
    if win.ok:
        marks = (np.abs(win.xx - x) < 2.5) & (rng.random(win.xx.shape) < 0.08)
        win.paint(marks, np.broadcast_to(rgb('#2a2622'), win.c.shape))


def cypress(cv, x, base, height, rng, cols=('#1a2e24', '#26402e', '#36583c', '#4a7048', '#66885a')):
    cols = jitter_ramp(cols, rng, hue=0.04)
    w = height * rng.uniform(0.09, 0.13)
    win = Window(cv, x - w * 1.5, base - height - 2, x + w * 1.5, base + 1)
    if not win.ok:
        return
    rel = (win.yy - (base - height)) / height
    prof = w * np.sin(np.clip(rel, 0, 1) * np.pi * 0.92 + 0.12) ** 0.7
    wob = (np.sin(win.yy * 0.9 + x) + np.sin(win.yy * 2.3)) * 0.6
    m = (rel >= 0) & (rel <= 1) & (np.abs(win.xx - x) <= prof + wob)
    side = (win.xx - x) / max(w, 1)
    t = np.clip(0.6 - side * 0.55 + (rng.random(m.shape) - 0.5) * 0.4, 0, 1)
    win.paint(m, to_ramp(t, cols, win.by, 0.5))


def leaf_mass(cv, cx, cy, r, rng, cols=OAK, squash=0.85, n=None):
    """Busch oder Krone ohne sichtbaren Stamm: Bueschel mit Blattstruktur und ausgefranstem Rand.
    Gibt die Maske in Leinwandgroesse zurueck."""
    cols = jitter_ramp(cols, rng)
    H, W = cv.c.shape[:2]
    win = Window(cv, cx - r * 1.6, cy - r * 1.4, cx + r * 1.6, cy + r * 1.3)
    full = np.zeros((H, W), bool)
    if not win.ok:
        return full
    blobs = []
    for _ in range(n or int(6 + r * 0.8)):
        a = rng.uniform(0, 2 * np.pi)
        d = np.sqrt(rng.random()) * r * 0.75
        br = r * rng.uniform(0.28, 0.5)
        blobs.append((cx + np.cos(a) * d * 1.15, cy + np.sin(a) * d * squash, br * 1.1, br * 0.95, rng.uniform(0, r * 0.2)))
    h, _ = sphere_field(win, blobs)
    mask = h >= 0
    leafn = ndimage.uniform_filter(rng.random(mask.shape), 2 if r < 12 else 3)
    h = np.where(mask, h + (leafn - 0.5) * r * 0.25, h)
    edge = mask & ~ndimage.binary_erosion(mask, iterations=1 if r < 10 else 2)
    mask &= ~(edge & (ndimage.uniform_filter(rng.random(mask.shape), 2) < 0.42))
    t = lit_from_field(h, mask, ao=1.2, flat=0.5)
    gl = ((win.xx - cx) * -LIGHT[0] + (win.yy - cy) * -LIGHT[1]) / max(r, 1)
    t = np.clip(t * 1.15 - np.clip(gl, -1, 1) * 0.25 + ndimage.uniform_filter(rng.random(mask.shape) - 0.5, 2) * 0.35, 0, 1)
    win.paint(mask, to_ramp(t, cols, win.by, 0.55))
    hi = mask & ~np.roll(mask, 1, axis=0) & (win.xx < cx + r * 0.3) & (rng.random(mask.shape) < 0.6)
    win.paint(hi, cols[-1])
    full[win.y0:win.y1, win.x0:win.x1] = mask
    return full


# ---------------------------------------------------------------------------------- Boden
def grass_tufts(cv, y0, y1, rng, n, cols=('#3a5a26', '#4e7430', '#6a903c', '#90b050'), x0=0, x1=None,
                mask=None, scale_y=None):
    """Einzelne Halmbueschel, vorn groesser: drei bis sieben Halme, gebogen, mit Lichtspitze."""
    H, W = cv.c.shape[:2]
    x1 = x1 if x1 is not None else W
    cols = hexes(*cols)
    for _ in range(n):
        y = int(y0 + (y1 - y0) * rng.random() ** 0.6)
        x = int(rng.uniform(x0, x1))
        if not (0 <= y < H and 0 <= x < W):
            continue
        if mask is not None and not mask[y, x]:
            continue
        s = 0.4 + 1.6 * (y - y0) / max(1, y1 - y0) if scale_y is None else scale_y(y)
        for _ in range(int(rng.integers(3, 8))):
            hgt = max(2, int(rng.uniform(3, 8) * s))
            bend = rng.uniform(-0.5, 0.5)
            bx = x + rng.uniform(-2, 2) * s
            for j in range(hgt):
                px_ = int(round(bx + bend * j * j / hgt))
                py_ = y - j
                if 0 <= px_ < W and 0 <= py_ < H:
                    c = cols[min(3, int(j / hgt * 3.2 + (bend < 0)))]
                    cv.c[py_, px_] = c


def flowers(cv, y0, y1, rng, n, kind='daisy', x0=0, x1=None, mask=None, patches=None):
    """Blumen mit Stiel: Margeriten, Lupinen, Mohn, Glockenblumen - vorn groesser. Mit
    [patches] wachsen sie in so vielen Gruppen statt gleichmaessig verstreut."""
    H, W = cv.c.shape[:2]
    x1 = x1 if x1 is not None else W
    centers = None
    if patches:
        centers = [(rng.uniform(x0, x1), y0 + (y1 - y0) * rng.random() ** 0.7) for _ in range(patches)]
    for _ in range(n):
        if centers:
            cx_, cy_ = centers[int(rng.integers(0, len(centers)))]
            sc = 0.3 + (cy_ - y0) / max(1, y1 - y0)
            x = int(cx_ + rng.normal(0, 10 * sc))
            y = int(cy_ + rng.normal(0, 3 * sc))
        else:
            y = int(y0 + (y1 - y0) * rng.random() ** 0.7)
            x = int(rng.uniform(x0, x1))
        if not (2 <= y < H and 2 <= x < W - 2) or (mask is not None and not mask[y, x]):
            continue
        s = 0.5 + 1.5 * (y - y0) / max(1, y1 - y0)
        stem = max(2, int(rng.uniform(4, 9) * s))
        for j in range(stem):
            if y - j >= 0:
                cv.c[y - j, x] = rgb('#3e6a2e') if j % 3 else rgb('#5a8a3a')
        ty = y - stem
        if ty < 2:
            continue
        if kind == 'daisy':
            pet = rgb('#f6f2e4') * rng.uniform(0.92, 1.0)
            r = 1 if s < 1.2 else 2
            for dx, dy in ((-r, 0), (r, 0), (0, -r), (0, r), (-1, -1), (1, -1), (-1, 1), (1, 1))[: 4 + 4 * (r > 1)]:
                if 0 <= ty + dy < H and 0 <= x + dx < W:
                    cv.c[ty + dy, x + dx] = pet
            cv.c[ty, x] = rgb('#f0c030')
        elif kind == 'lupine':
            col = rgb(['#8a6ad8', '#b07ae0', '#e890c0', '#6a7ae0'][int(rng.integers(0, 4))])
            ln = int(4 * s) + 2
            for j in range(ln):
                w_ = 1 if j < ln * 0.7 else 0
                for dx in range(-w_, w_ + 1):
                    if 0 <= ty - j < H:
                        cv.c[ty - j, x + dx] = col * (1.15 if dx < 0 else 0.9 if dx > 0 else 1.0)
        elif kind == 'poppy':
            col = rgb('#e8402e') * rng.uniform(0.9, 1.1)
            for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 0), (-1, -1), (1, -1)):
                cv.c[ty + dy, x + dx] = col
            cv.c[ty, x] = rgb('#2a1a1a')
        elif kind == 'bell':
            col = rgb('#6a7ad8')
            for dx, dy in ((0, 0), (-1, 1), (0, 1), (1, 1)):
                cv.c[ty + dy, x + dx] = col
        elif kind == 'buttercup':
            cv.c[ty, x] = rgb('#f8d830')
            cv.c[ty, x - 1] = rgb('#f0c020')


def stone(cv, x, y, rx, ry, rng, cols=('#4a4640', '#666058', '#86807a', '#a8a298', '#ccc6ba'), moss=None,
          wet=False):
    """Einzelner Stein: unregelmaessig, mit Kanten, Rissen und optional Moos."""
    cols = jitter_ramp(cols, rng, hue=0.04, val=0.12)
    blobs = []
    for _ in range(int(rng.integers(2, 5))):
        blobs.append((x + rng.uniform(-0.4, 0.4) * rx, y - ry * rng.uniform(0.3, 0.7),
                      rx * rng.uniform(0.6, 0.95), ry * rng.uniform(0.6, 1.0), rng.uniform(0, 1)))
    win = Window(cv, x - rx * 1.6, y - ry * 2.2, x + rx * 1.6, y + 2)
    if not win.ok:
        return None
    # Schatten
    sh = ((win.xx - x - rx * 0.3) / (rx * 1.3)) ** 2 + ((win.yy - y) / max(1.2, ry * 0.25)) ** 2 < 1
    win.paint(sh, win.c * 0.6)
    h, _ = sphere_field(win, blobs)
    m = (h >= 0) & (win.yy <= y)
    # Facetten: Hoehe quantisieren, dann Licht
    hq = np.round(h / max(1, ry * 0.35)) * ry * 0.35
    t = lit_from_field(ndimage.gaussian_filter(hq, 0.7), m, ao=0.8, flat=0.6)
    t = np.clip(t + (rng.random(m.shape) - 0.5) * 0.18, 0, 1)
    win.paint(m, to_ramp(t, cols, win.by, 0.35))
    crack = m & (np.abs(np.sin((win.xx - x) * 0.8 + (win.yy - y) * 1.3 + rng.uniform(0, 6))) < 0.08) & (rng.random(m.shape) < 0.6)
    win.paint(crack, cols[0])
    if moss:
        top = m & ~np.roll(m, 2, axis=0) & (rng.random(m.shape) < 0.75)
        win.paint(top, rgb(moss))
        win.paint(top & (win.xx < x), rgb(moss) * 1.2)
    if wet:
        hi = m & (t > 0.85) & (rng.random(m.shape) < 0.4)
        win.paint(hi, np.broadcast_to(rgb('#ffffff'), win.c.shape), 0.6)
    edge = ndimage.binary_dilation(m) & ~m & (win.yy >= y - ry)
    win.paint(edge, win.c * 0.7)
    return m


def pebbles(cv, mask, rng, n, cols=('#6a645c', '#8a847a', '#aaa498', '#c8c2b4')):
    H, W = cv.c.shape[:2]
    ys, xs = np.nonzero(mask)
    if len(xs) == 0:
        return
    cols = hexes(*cols)
    for i in rng.integers(0, len(xs), n):
        x, y = xs[i], ys[i]
        c = cols[int(rng.integers(1, len(cols)))]
        cv.c[y, x] = c
        if x + 1 < W and rng.random() < 0.6:
            cv.c[y, x + 1] = cols[0]
        if y + 1 < H:
            cv.c[y + 1, x] = c * 0.6
