"""Baukasten fuer die Orte von Itoeva 2 im Stil der Weltstudien (docs/concept-art/world-studies/).

Jeder Ort ist eine Komposition aus wenigen Bausteinen - Himmel, ferne Huegel, Stadt am Hang,
Baeume, Boden in Perspektive, Wege, Wasser, Requisiten. Licht kommt von oben links (wie in den
Studien), Schatten fallen nach rechts unten. Masse in Bildpixeln (480 x 270).

Ein Ort beschreibt zugleich, was das Spiel ueber ihn wissen muss (Gehflaeche, Plaetze, Tueren),
siehe [Scene.meta] - daraus entsteht der Kotlin-Katalog (catalog.py), damit Bild und Spiel nie
auseinanderlaufen.
"""
import numpy as np
from scipy import ndimage
import nature as N
from px import Canvas, rgb, ramp, dramp, noise, glow, quantize, lerp, MaskPen, BAYER4

W, H = 480, 270


def hexes(*cs):
    return [rgb(c) for c in cs]


class Scene:
    def __init__(self, name, horizon=118, seed=1, light='day'):
        self.name = name
        self.cv = Canvas(W, H)
        self.c = self.cv
        self.yy, self.xx = self.cv.yy, self.cv.xx
        self.HY = horizon
        self.rng = np.random.default_rng(seed)
        self.seed = seed
        self.light = light
        self.by = np.tile(BAYER4, (H // 4 + 1, W // 4 + 1))[:H, :W]
        # Spielangaben (siehe GameScenes): Gehflaeche, Plaetze, gesperrte Raender
        self.meta = {'walk': None, 'spots': [], 'blocked': [], 'cropTop': 0.8}

    # ------------------------------------------------------------------ Grundwerkzeug
    def m(self):
        return MaskPen(W, H)

    def paint(self, mask, color, alpha=1.0):
        self.cv.paint(mask, rgb(color) if isinstance(color, str) else color, alpha)

    def ellipse(self, cx, cy, rx, ry, rot=0.0):
        x, y = self.xx - cx + 0.5, self.yy - cy + 0.5
        if rot:
            c, s = np.cos(rot), np.sin(rot)
            x, y = x * c + y * s, -x * s + y * c
        return (x / max(rx, 0.3)) ** 2 + (y / max(ry, 0.3)) ** 2 <= 1.0

    def poly(self, pts):
        return self.m().poly(pts).a > 0

    def rect(self, x0, y0, x1, y1):
        return (self.xx >= x0) & (self.xx <= x1) & (self.yy >= y0) & (self.yy <= y1)

    def depth(self, y):
        """Massstab an einer Bodenzeile: 1 ganz vorn, kleiner zum Horizont."""
        return max(0.08, (y - self.HY) / (H - self.HY))

    def shade(self, mask, ramp_cols, light=(-0.65, -0.75), levels=None, fur=0.0, seed=0, bulge=1.0, gamma=1.0):
        """Teil mit Woelbung aus der eigenen Form (wie bei den Figuren), in Tonrampe gesetzt."""
        mask = np.asarray(mask, bool)
        if not mask.any():
            return mask
        cols = np.array(hexes(*ramp_cols)) if isinstance(ramp_cols[0], str) else np.array(ramp_cols)
        k = len(cols)
        d = ndimage.distance_transform_edt(np.pad(mask, 1))[1:-1, 1:-1]
        hgt = ndimage.gaussian_filter(np.sqrt(d) * bulge, 1.0)
        gy, gx = np.gradient(hgt)
        n = np.stack([-gx, -gy, np.full_like(hgt, 0.6)], -1)
        n /= np.linalg.norm(n, axis=-1, keepdims=True)
        L = np.array([light[0], light[1], 0.9])
        L /= np.linalg.norm(L)
        inten = np.clip((n * L).sum(-1), 0, 1)
        vals = inten[mask]
        lo, hi = np.percentile(vals, 3), np.percentile(vals, 97)
        t = np.clip((inten - lo) / max(hi - lo, 1e-6), 0, 1) ** gamma
        if fur:
            r = np.random.default_rng(seed).random(mask.shape)
            t = t + (ndimage.uniform_filter(r, 2) - 0.5) * fur
        idx = np.clip(np.floor(t * (k - 0.01) + self.by * 0.4), 0, k - 1).astype(int)
        self.cv.paint(mask, cols[idx])
        return mask

    def outline(self, mask, color='#2a2420', strength=0.55):
        """Weiche dunkle Kontur aussen um eine Form (nur unten/rechts kraeftig)."""
        ring = ndimage.binary_dilation(mask) & ~mask
        dark = self.cv.c * (1 - strength) + rgb(color) * strength
        self.cv.paint(ring, dark, 0.85)

    def shadow(self, cx, cy, rx, ry, k=0.55):
        m = self.ellipse(cx + rx * 0.25, cy, rx, ry)
        self.cv.c = np.where(m[..., None], self.cv.c * k, self.cv.c)

    # ------------------------------------------------------------------ Himmel
    def sky(self, top='#6fa6dc', mid='#a8cde8', low='#e8eef0', warm=None, clouds=True, levels=18):
        HY = self.HY
        t = np.clip(self.yy / (HY + 4), 0, 1)
        stops = [(0, top), (0.55, mid), (1, low)]
        sky = dramp(stops, t, levels)
        if warm:
            sky = lerp(sky, rgb(warm), (np.clip((t - 0.65) / 0.35, 0, 1) ** 2 * 0.35)[..., None])
        self.cv.paint(self.yy <= HY + 4, sky)
        if clouds:
            self.clouds()

    def clouds(self, n=5, ymin=12, ymax=None, lit='#ffffff', mid='#eef2f6', shade='#c6d2e2', base='#aebdd2'):
        """Haufenwolken, jede aus eigenen Tuermen (nature.cumulus), dazu ein paar Federwolken."""
        ymax = ymax or self.HY - 30
        r = np.random.default_rng(self.seed + 70)
        xs = np.sort(r.uniform(10, 470, n))
        for x in xs:
            y = r.uniform(ymin + 14, max(ymin + 15, ymax))
            w = r.uniform(50, 130) * (0.6 + 0.6 * (y - ymin) / max(1, ymax - ymin + 1))
            N.cumulus(self.cv, x, y, w, r, cols=(base, shade, mid, lit))
        for _ in range(int(r.integers(1, 3))):
            N.cirrus(self.cv, r.uniform(0, 380), r.uniform(6, ymin + 20), r.uniform(60, 140), r, col=mid, k=0.45)

    # ------------------------------------------------------------------ Ferne
    def ridge(self, x0, x1, base, peak, seed, rough=0.35, scale=40):
        xs = np.arange(W)
        n = noise(W, 4, scale, seed, octaves=5)[1]
        env = np.clip(np.sin(np.clip((xs - x0) / max(1, x1 - x0), 0, 1) * np.pi), 0, 1) ** 0.8
        top = base - (base - peak) * env * (1 - rough + rough * n * 2)
        top[(xs < x0) | (xs > x1)] = base + 1
        return top

    def hills(self, layers):
        """Ferne Huegel, hinten zuerst: (x0, x1, peak, Farbe, Lichtfarbe, seed, rough)."""
        for x0, x1, peak, col, litc, sd, rough in layers:
            top = self.ridge(x0, x1, self.HY + 2, peak, sd, rough)
            msk = (self.yy >= top[None, :]) & (self.yy <= self.HY + 3)
            self.paint(msk, col)
            edge = msk & ~np.roll(msk, 1, axis=0)
            self.paint(edge & (self.xx < 300), litc)
            tex = noise(W, H, 4, sd + 9, 2)
            self.cv.paint(msk & (tex > 0.65), self.cv.c * 0.94)

    def treeline(self, y, x0, x1, col, lit, seed, height=10, density=1.0):
        """Ferne Baumreihe als Wellenkante mit Licht oben links."""
        xs = np.arange(W)
        n = noise(W, 4, 6, seed, octaves=3)[1]
        top = y - height * (0.4 + 0.6 * n)
        msk = (self.yy >= top[None, :]) & (self.yy <= y) & (self.xx >= x0) & (self.xx <= x1)
        self.paint(msk, col)
        self.paint(msk & ~np.roll(msk, 1, axis=0), lit)
        self.paint(msk & (noise(W, H, 2, seed + 3, 1) > 0.7), lerp(rgb(col), rgb(lit), 0.4))
        return msk

    def house(self, x, y, w, h, roof='#c8664a', wall='#ecdcc2', side=None, k=1.0, windows=True,
              shutters=None, lit=False, rng=None):
        """Fernes Haus als Einzelstueck: Dreiviertelansicht mit Sonnen- und Schattenseite, Dach als
        Sattel, Walm oder flach mit Bruestung, mal Kamin, mal Bogentuer, Fenster unregelmaessig."""
        r = rng or self.rng
        sw = max(1, int(w * r.uniform(0.3, 0.5)))
        wc = rgb(wall) * k * r.uniform(0.92, 1.05)
        self.paint(self.rect(x, y, x + w - 1, y + h - 1), wc)
        self.paint(self.rect(x + w, y + 1, x + w + sw - 1, y + h - 1), wc * 0.7 + rgb('#3a4a6a') * 0.08)
        rc = rgb(roof) * k * r.uniform(0.88, 1.1)
        kind = r.choice(['gable', 'gable', 'hip', 'flat'])
        rh = max(2, int(w * r.uniform(0.25, 0.45)))
        if kind == 'gable':
            self.paint(self.poly([(x - 1, y + 1), (x + w * 0.5, y - rh), (x + w + sw, y + 1)]), rc)
            self.paint(self.m().line([(x - 1, y + 1), (x + w * 0.5, y - rh)]).a > 0, lerp(rc, rgb('#ffd8b0'), 0.45))
        elif kind == 'hip':
            self.paint(self.poly([(x - 1, y + 1), (x + 2, y - rh + 1), (x + w + sw - 3, y - rh + 1), (x + w + sw, y + 1)]), rc)
            self.paint(self.rect(x + 2, y - rh + 1, x + w + sw - 3, y - rh + 1), lerp(rc, rgb('#ffd8b0'), 0.4))
            self.paint(self.poly([(x + w, y + 1), (x + w + sw - 3, y - rh + 1), (x + w + sw, y + 1)]), rc * 0.75)
        else:
            self.paint(self.rect(x - 1, y - 1, x + w + sw - 1, y), lerp(wc, rgb('#ffffff'), 0.2))
        if r.random() < 0.3 and kind != 'flat':
            cx = int(x + w * r.uniform(0.2, 0.7))
            self.paint(self.rect(cx, y - rh - 1, cx + 1, y - rh // 2), wc * 0.85)
        if windows and w >= 4:
            for wx in range(x + 1, x + w - 1, int(r.integers(2, 4))):
                for wy in range(y + 2, y + h - 1, 3):
                    if r.random() < 0.2:
                        continue
                    c = '#ffd27a' if lit and r.random() < 0.55 else ('#4a5a72' if r.random() < 0.8 else '#6a5040')
                    self.paint(self.rect(wx, wy, wx, wy + (1 if h > 6 else 0)), c)
                    if shutters and w > 6:
                        self.paint(self.rect(wx - 1, wy, wx - 1, wy + 1), shutters)
        if h > 6 and r.random() < 0.3:
            dx = int(x + w * r.uniform(0.2, 0.6))
            self.paint(self.rect(dx, y + h - 3, dx + 1, y + h - 1), '#5a3a28')

    def town(self, x0, x1, ytop, ybase, n=60, k=0.85, roofs=('#c8664a', '#b85a42', '#d47a54', '#a84a36', '#d88a60'),
             walls=('#efe2c8', '#e6d2b4', '#f4ead6', '#e8c8a0', '#dcd0c0', '#f0dcc0'), tower=None, lit=False):
        """Stadt am Hang: Haeuser in Reihen hinter- und uebereinander, verschieden gross und
        gedeckt, dazwischen Gaerten, Mauern und Baeume, oben eine Kirche mit Turm."""
        r = np.random.default_rng(self.seed + 401)
        # Hang: Terrassenmauern und Olivenhaine zwischen den Haeusern
        for _ in range(int((x1 - x0) * 1.6)):
            x = r.uniform(x0, x1)
            yt = np.interp(x, [x0, (x0 + x1) / 2, x1], [ybase, ytop, ybase])
            y = r.uniform(min(yt + 2, ybase - 1), ybase)
            if r.random() < 0.35:
                ln = r.uniform(4, 12)
                self.paint(self.rect(int(x), int(y), int(x + ln), int(y)), lerp(rgb('#c8bca0'), rgb('#8a9a78'), 0.4) * k)
            else:
                c = rgb(['#4a6a44', '#5a7a4c', '#6a8a58', '#7a9a62'][int(r.integers(0, 4))]) * k
                self.paint(self.rect(int(x), int(y), int(x) + 1, int(y)), c)
                self.paint(self.rect(int(x), int(y) - 1, int(x), int(y) - 1), c * 1.2)
        spots = []
        for _ in range(n):
            x = int(r.uniform(x0, x1))
            yt = np.interp(x, [x0, (x0 + x1) / 2, x1], [ybase, ytop, ybase])
            y = int(r.uniform(yt, ybase))
            spots.append((y, x))
        for i, (y, x) in enumerate(sorted(spots)):
            d = (y - ytop) / max(1, ybase - ytop)
            w = int(r.integers(4, 8) + d * 5)
            h = int(r.integers(4, 6) + d * 3)
            if r.random() < 0.12:
                N.leaf_mass(self.cv, x + w / 2, y + 1, 2 + d * 3, r, cols=('#2a3e2a', '#3a5236', '#4e6a42', '#668050', '#86985e', '#a0aa70'))
                continue
            self.house(x, y, w, h, roof=roofs[int(r.integers(0, len(roofs)))],
                       wall=walls[int(r.integers(0, len(walls)))], k=k * (0.88 + 0.12 * d), lit=lit, rng=r)
            if r.random() < 0.15:
                self.paint(self.rect(x - 4, y + h - 2, x - 1, y + h - 1), rgb('#d8ccb4') * k)
        if tower:
            tx, ty, th = tower
            self.paint(self.rect(tx - 6, ty + th - 8, tx + 12, ty + th), rgb('#ece0c8') * k)
            self.paint(self.poly([(tx - 7, ty + th - 8), (tx + 3, ty + th - 14), (tx + 13, ty + th - 8)]), rgb('#b85a42') * k)
            self.paint(self.rect(tx, ty, tx + 4, ty + th), rgb('#efe2c8') * k)
            self.paint(self.rect(tx + 5, ty + 1, tx + 6, ty + th), rgb('#c8b496') * k)
            self.paint(self.poly([(tx - 1, ty), (tx + 3, ty - 9), (tx + 7, ty)]), rgb('#b85a42') * k)
            self.paint(self.rect(tx + 1, ty + 3, tx + 3, ty + 6), '#3a3a4a')
            self.paint(self.rect(tx + 2, ty + 3, tx + 2, ty + 6), '#c8a050')
            self.paint(self.rect(tx + 2, ty + 10, tx + 2, ty + 12), '#4a5a72')

    # ------------------------------------------------------------------ Baeume
    def leafy(self, cx, cy, r, seed, ramp_cols=('#1c3222', '#2a4a2c', '#3e6a36', '#62924a', '#9cc068'),
              squash=0.85, light=(-0.7, -0.8)):
        """Laubmasse aus Bueschen mit Blattstruktur (nature.leaf_mass), jedes Mal anders."""
        return N.leaf_mass(self.cv, cx, cy, r, np.random.default_rng(abs(int(seed)) + 1), cols=ramp_cols, squash=squash)

    def trunk(self, x, base, top, w, col=('#3e2c22', '#5e4232', '#7e5c44', '#9a7656'), branches=True):
        """Stamm mit Wurzelanlauf, leicht geschwungen, Rinde in senkrechten Rissen, zwei Aeste."""
        hgt = base - top
        lean = (self.rng.random() - 0.5) * w * 0.8
        pts_l, pts_r = [], []
        for k in range(9):
            f = k / 8
            y = base - hgt * f
            wd = w * (1.9 - 0.9 * min(1, f * 5)) * (1 - 0.35 * f)
            cx = x + lean * f * f + np.sin(f * 3) * w * 0.15
            pts_l.append((cx - wd, y))
            pts_r.append((cx + wd, y))
        m = self.poly(pts_l + pts_r[::-1])
        self.shade(m, list(col), light=(-1, -0.15))
        bark = m & (noise(W, H, 2, abs(int(x)) + 5, 1) > 0.62) & ((self.xx + self.yy // 6) % 3 == 0)
        self.cv.paint(bark, self.cv.c * 0.78)
        if branches and hgt > 40:
            for side, f in ((-1, 0.7), (1, 0.82)):
                y0 = base - hgt * f
                bx = x + lean * f * f
                br = self.m().line([(bx, y0), (bx + side * w * 4, y0 - hgt * 0.18)], max(1, int(w * 0.7))).a > 0
                self.shade(br, list(col), light=(-1, -0.4))
        return m

    def bush(self, x, y, r, seed, flowers=None, cols=('#24402a', '#355a32', '#4c7a3c', '#6c9a4a', '#94ba62')):
        """Rundlicher Busch, unten auf y aufsitzend, mit Schatten und optional Blueten."""
        self.shadow(x + r * 0.2, y, r * 1.1, r * 0.22, 0.65)
        m = self.leafy(x, y - r * 0.7, r, seed, ramp_cols=cols, squash=0.7)
        if flowers:
            rr = np.random.default_rng(seed + 7)
            for _ in range(int(r * r * 0.5)):
                fx, fy = int(rr.uniform(x - r, x + r)), int(rr.uniform(y - r * 1.5, y))
                if 0 <= fx < W and 0 <= fy < H and m[fy, fx]:
                    self.paint(self.rect(fx, fy, fx, fy), flowers[int(rr.integers(0, len(flowers)))])
        return m

    def dapple(self, cx, cy, rx, ry, seed, k=0.62):
        """Lichtgesprenkelter Schatten unter einer Krone: dunkel mit hellen Flecken."""
        m = self.ellipse(cx, cy, rx, ry)
        holes = noise(W, H, 4, seed, 2) > 0.62
        self.cv.c = np.where((m & ~holes)[..., None], self.cv.c * k, self.cv.c)
        spots = m & holes & (noise(W, H, 2, seed + 1, 1) > 0.6)
        self.cv.c = np.where(spots[..., None], np.minimum(self.cv.c * 1.12, 1), self.cv.c)

    def foliage(self, corner, size, seed, cols=('#1a2e22', '#24402c', '#36583a', '#4e7646', '#6e9452')):
        """Vordergrund-Blattwerk an einer Bildecke ('bl' oder 'br'): einzelne Blaetter mit Mittelrippe,
        Seitenadern und eigener Toenung, dazwischen Graeser und ein paar Blueten."""
        rr = np.random.default_rng(seed)
        bx = 0 if corner == 'bl' else W
        sign = 1 if corner == 'bl' else -1
        cols = [rgb(c) if isinstance(c, str) else c for c in cols]
        N.grass_tufts(self.cv, H - size * 0.5, H, rr, int(size * 1.5), cols=(cols[0], cols[1], cols[3], cols[4]),
                      x0=(0 if corner == 'bl' else W - size * 1.4), x1=(size * 1.4 if corner == 'bl' else W),
                      scale_y=lambda y: 2.2)
        for i in range(int(size * 0.45)):
            ang = rr.uniform(-1.45, -0.25) if corner == 'bl' else rr.uniform(-2.9, -1.7)
            ln = rr.uniform(size * 0.35, size * 0.95)
            x0 = bx + sign * rr.uniform(-4, size * 0.7)
            y0 = H + rr.uniform(0, 8)
            wid = rr.uniform(0.12, 0.2)
            cx, cy = x0 + np.cos(ang) * ln * 0.5, y0 + np.sin(ang) * ln * 0.5
            leaf = self.ellipse(cx, cy, ln * 0.5, ln * wid, rot=-ang)
            tone = N.jitter_ramp(cols, rr, hue=0.08, val=0.15)
            self.shade(leaf, [tuple(c) for c in tone], light=(-0.7, -0.8), seed=int(cx) + i)
            tip = (x0 + np.cos(ang) * ln * 0.95, y0 + np.sin(ang) * ln * 0.95)
            self.paint((self.m().line([(x0, y0), tip]).a > 0) & leaf, tone[3])
            for k in range(2, 9):
                t = k / 10
                px_, py_ = x0 + np.cos(ang) * ln * t, y0 + np.sin(ang) * ln * t
                for sd in (1, -1):
                    a2 = ang + sd * 0.9
                    q = (px_ + np.cos(a2) * ln * wid * 0.9, py_ + np.sin(a2) * ln * wid * 0.9)
                    self.paint((self.m().line([(px_, py_), q]).a > 0) & leaf, tone[1])
            self.outline(leaf, strength=0.35)
        if rr.random() < 0.7:
            N.flowers(self.cv, H - size * 0.6, H - 4, rr, int(size * 0.4), rr.choice(['daisy', 'bell', 'buttercup']),
                      x0=(0 if corner == 'bl' else W - size), x1=(size if corner == 'bl' else W))

    def tree(self, x, base, height, seed, kind='round', crown=None, trunk_col=None, haze=None, k=0.0):
        """Ein Baum als Einzelstueck (nature.py): Laubbaum mit Astwerk, Tanne aus Etagen, Zypresse,
        Birke. [haze]/[k] mischen Ferne-Dunst hinein."""
        rr = np.random.default_rng(abs(int(seed)) * 7919 + 13)
        if kind == 'round':
            leaf = crown if crown and len(crown) >= 5 else N.OAK
            if crown and len(crown) == 5:
                leaf = tuple(crown) + ('#c8e080',)
            self.dapple(x + height * 0.15, base + 1, height * 0.42, height * 0.07, seed)
            N.oak(self.cv, x, base, height, rr, leaf=leaf, bark=trunk_col or N.BARK)
        elif kind == 'pine':
            cols = crown if crown else N.PINE
            if len(cols) == 4:
                cols = (cols[0],) + tuple(cols)
            self.shadow(x + height * 0.1, base, height * 0.2, height * 0.04)
            N.pine(self.cv, x, base, height, rr, cols=cols, haze=haze, k=k)
        elif kind == 'cypress':
            cols = crown if crown else ('#1a2e24', '#26402e', '#36583c', '#4a7048', '#66885a')
            if len(cols) == 4:
                cols = (cols[0],) + tuple(cols)
            N.cypress(self.cv, x, base, height, rr, cols=cols)
        elif kind == 'birch':
            N.birch(self.cv, x, base, height, rr)

    # ------------------------------------------------------------------ Boden
    def project(self, u, z):
        """Bodenpunkt (u quer, z Tiefe; z = 1 ganz vorn) auf den Bildschirm."""
        return (W / 2 + u * 158.0 / z, self.HY + (H - self.HY) / z)

    def ground_poly(self, pts):
        return self.poly([self.project(u, z) for u, z in pts])

    def ground_uv(self):
        z = (H - self.HY) / np.maximum(self.yy - self.HY, 1e-3)
        u = (self.xx - W / 2) * z / 158.0
        return u, z

    def grass(self, y0, base=('#4e7a34', '#62903c', '#78a448', '#94b85a'), haze='#a8c0a0', flowers=True):
        """Wiese ab Zeile y0: Verlauf mit Dunst nach hinten, Halmbueschel, die vorn groesser werden."""
        msk = self.yy >= y0
        t = np.clip((self.yy - y0) / (H - y0), 0, 1)
        n = noise(W, H, 6, self.seed + 21, 3)
        col = dramp([(0, base[1]), (0.5, base[2]), (1, base[3])], np.clip(n * 0.8 + (1 - t) * 0.15, 0, 1), 4)
        col = lerp(col, rgb(haze), ((1 - t) ** 3 * 0.45)[..., None])
        self.cv.paint(msk, col)
        # Hellere und dunklere Flecken (Klee, Moos, trockene Stellen)
        r = self.rng
        pn = noise(W, H, 14, self.seed + 23, 3)
        self.cv.paint(msk & (pn > 0.68), lerp(col, rgb(base[0]), 0.35))
        self.cv.paint(msk & (pn < 0.22), lerp(col, rgb('#c8c070'), 0.22))
        clover = msk & (noise(W, H, 5, self.seed + 24, 2) > 0.74) & ((self.xx + self.yy * 3) % 4 == 0)
        self.paint(clover, lerp(rgb(base[3]), rgb('#ffffff'), 0.15))
        # Halmbueschel, einzeln geformt, vorn groesser
        scale = lambda y: 0.25 + 1.75 * self.depth(y) ** 1.2
        N.grass_tufts(self.cv, y0 + 2, H, r, int(700 * (H - y0) / 150),
                      cols=(base[0], base[1], base[2], lerp(rgb(base[3]), rgb('#f0f0b0'), 0.25)), scale_y=scale)
        if flowers:
            kinds = [('daisy', 70, 7), ('buttercup', 60, 6), ('lupine', 18, 3), ('poppy', 14, 3), ('bell', 24, 4)]
            for kind, n_, p_ in kinds:
                N.flowers(self.cv, y0 + 6, H, r, int(n_ * (H - y0) / 150), kind, patches=p_)
        N.pebbles(self.cv, msk & (self.yy > y0 + 20), r, int(40 * (H - y0) / 150))
        return msk

    def stone_path(self, pts_world, width, tone=('#8c7e70', '#a8988a', '#c4b4a2', '#dccebc')):
        """Plattenweg in Perspektive entlang [pts_world] (u, z), Breite in Welteinheiten."""
        u, z = self.ground_uv()
        below = self.yy > self.HY + 1
        d = np.full(u.shape, 9.0)
        for (u0, z0), (u1, z1) in zip(pts_world[:-1], pts_world[1:]):
            vx, vz = u1 - u0, z1 - z0
            L = vx * vx + vz * vz
            tt = np.clip(((u - u0) * vx + (z - z0) * vz) / max(L, 1e-9), 0, 1)
            d = np.minimum(d, np.hypot(u - (u0 + vx * tt), (z - (z0 + vz * tt)) * 0.6))
        path = below & (d < width / 2)
        self.cobble(path, tone, size=0.09)
        edge = path & ~ndimage.binary_erosion(path)
        self.cv.paint(edge, self.cv.c * 0.75)
        return path

    def cobble(self, mask, tone=('#8c7e70', '#a8988a', '#c4b4a2', '#dccebc'), size=0.11, gap='#5a4e48'):
        u, z = self.ground_uv()
        S = size
        gi, gj = np.floor(u / S).astype(int), np.floor(z / S).astype(int)
        best = np.full(u.shape, 9.0)
        second = np.full(u.shape, 9.0)
        cid = np.zeros(u.shape, np.int64)
        for di in (-1, 0, 1):
            for dj in (-1, 0, 1):
                ci, cj = gi + di, gj + dj
                hh = (ci * 73856093) ^ (cj * 19349663)
                jx = ((hh * 2654435761) % 1000) / 1000
                jy = ((hh * 40503 + 12345) % 1000) / 1000
                sx, sz = (ci + 0.15 + jx * 0.7) * S, (cj + 0.15 + jy * 0.7) * S
                dd = np.hypot(u - sx, z - sz)
                closer = dd < best
                second = np.where(closer, best, np.minimum(second, dd))
                cid = np.where(closer, hh, cid)
                best = np.minimum(best, dd)
        gapw = 0.012 * z ** 0.6
        ed = second - best                       # Abstand zur Fuge
        edge = ed < gapw
        var = ((cid * 2246822519) % 1000) / 1000
        var2 = ((cid * 3266489917) % 1000) / 1000
        col = dramp([(0, tone[0]), (0.35, tone[1]), (0.7, tone[2]), (1, tone[3])], var * 0.8 + 0.1, 4)
        # Jeder Stein leicht anders getoent (waermer/kuehler), mit Woelbung: Licht oben links
        col = col * (1 + (var2[..., None] - 0.5) * np.array([0.10, 0.04, -0.06]))
        hgt = np.clip(ed / (S * 0.35), 0, 1) ** 0.6
        gy, gx = np.gradient(ndimage.gaussian_filter(hgt, 0.6))
        litv = np.clip(-gy * 1.2 - gx * 0.8, -1, 1)
        col = col * (1 + litv[..., None] * 0.22 * (z < 4)[..., None])
        tex = (noise(W, H, 1, self.seed + 77, 1) - 0.5) * 0.12
        col = col * (1 + tex[..., None])
        hz = np.clip((z - 1) / 2.5, 0, 1)
        col = lerp(col, rgb('#c8c0b4'), (hz * 0.45)[..., None])
        edge = edge & (z < 4.5)
        self.cv.paint(mask, col)
        self.cv.paint(mask & edge, rgb(gap))
        # Moos und Gras in manchen Fugen, Risse in manchen Steinen, Laub
        mossy = mask & edge & (noise(W, H, 6, self.seed + 78, 2) > 0.62) & (z < 3)
        self.cv.paint(mossy, lerp(rgb('#4e6a34'), rgb('#6a8a40'), var[..., None]))
        crack = mask & ~edge & (var2 > 0.88) & (np.abs(np.sin(u * 60 + z * 40)) < 0.06)
        self.cv.paint(crack, rgb(gap))
        if z.min() < 2:
            r = np.random.default_rng(self.seed + 79)
            ys, xs = np.nonzero(mask & (self.yy > self.HY + 30))
            for i in r.integers(0, max(1, len(xs)), 60 if len(xs) else 0):
                c = rgb(['#c8783a', '#a85a2a', '#d8a050', '#8a6a2a'][int(r.integers(0, 4))])
                self.cv.c[ys[i], xs[i]] = c
                if xs[i] + 1 < W:
                    self.cv.c[ys[i], xs[i] + 1] = c * 0.8

    def water(self, y0, y1, x0=0, x1=W, deep='#3e7aa0', mid='#5a9ac0', light='#a8d4e8', sky='#d8eef4',
              sun=None):
        msk = self.rect(x0, y0, x1, y1)
        t = np.clip((self.yy - y0) / max(1, y1 - y0), 0, 1)
        col = dramp([(0, sky), (0.15, light), (0.5, mid), (1, deep)], t, 10)
        self.cv.paint(msk, col)
        wn = noise(W * 3, H, 5, self.seed + 31, 2)[:, ::3][:, :W]
        ripple = msk & (wn > 0.68) & ((self.yy % 3) == 0)
        self.cv.paint(ripple, lerp(col, rgb('#ffffff'), 0.45))
        dark = msk & (wn < 0.25) & ((self.yy % 4) == 1)
        self.cv.paint(dark, col * 0.88)
        if sun is not None:
            sx = sun
            for y in range(y0, y1):
                wdt = 2 + (y - y0) * 0.16
                for x in range(int(sx - wdt), int(sx + wdt)):
                    if 0 <= x < W and wn[y, x] > 0.55 and (x + y) % 2 == 0:
                        self.cv.c[y, x] = lerp(self.cv.c[y, x], rgb('#fff4d0'), 0.8)
        self.paint(self.rect(x0, y0, x1, y0), '#f0f8fa')
        return msk

    def reflect(self, y_water, height, y_end=None, k=0.55, tint='#5a7a8a'):
        """Spiegelt das Ufer ueber [y_water] ins Wasser darunter - leicht verwackelt und abgedunkelt."""
        y_end = y_end or min(H, y_water + height)
        wn = noise(W * 3, H, 4, self.seed + 61, 2)[:, ::3][:, :W]
        src = self.cv.c.copy()
        for y in range(y_water + 1, y_end):
            sy = y_water - (y - y_water)
            if sy < 0:
                break
            shift = ((wn[y] - 0.5) * 6 * (1 + (y - y_water) / 30)).astype(int)
            xs = np.clip(np.arange(W) + shift, 0, W - 1)
            row = src[sy, xs]
            fade = k * (1 - (y - y_water) / max(1, y_end - y_water)) ** 0.7
            self.cv.c[y] = lerp(self.cv.c[y], row * 0.85 + rgb(tint) * 0.15, fade)

    def facade(self, x0, x1, y_top, y_base, wall=('#c8b496', '#dccaa8', '#ecdcbc', '#f6ead0'),
               roof='#c8664a', shutters='#4e8a86', floors=2, door_x=None, shop=None, seed=0):
        """Steinhaus von vorn: verputzte Wand mit Struktur, Ziegeldach mit Ueberstand, Fenster mit
        Laeden und Blumenkaesten, unten optional ein Laden (Schaufenster, warmes Licht) und eine Tuer.
        Gibt die Trefferflaeche der Tuer zurueck."""
        wall_m = self.rect(x0, y_top, x1, y_base)
        n = noise(W, H, 3, seed + 3, 2)
        col = dramp([(0, wall[0]), (0.4, wall[1]), (0.75, wall[2]), (1, wall[3])], n * 0.7 + 0.3 * np.clip((self.xx - x0) / max(1, x1 - x0), 0, 1)[..., None][..., 0], 4)
        self.cv.paint(wall_m, col)
        # Steinkanten an den Ecken
        for cx in (x0, x1 - 3):
            for y in range(y_top, y_base, 5):
                self.paint(self.rect(cx, y, cx + 3, y + 3), '#b8a488')
        # Dach mit Ueberstand und Ziegelreihen
        rh = 10
        roof_m = self.poly([(x0 - 6, y_top + 2), (x0 + 4, y_top - rh), (x1 - 4, y_top - rh), (x1 + 6, y_top + 2)])
        self.shade(roof_m, ['#7a3a2a', '#a8503a', '#c8664a', '#e08a64'], light=(-0.4, -1))
        for y in range(y_top - rh + 2, y_top + 2, 3):
            self.paint(roof_m & (self.yy == y), '#8a4232')
        self.paint(self.rect(x0 - 6, y_top + 2, x1 + 6, y_top + 3), '#6a3426')
        floor_h = (y_base - y_top) / floors
        door = None
        for f in range(floors):
            fy = y_top + f * floor_h
            ground = f == floors - 1
            if ground and shop:
                sx0, sx1 = x0 + 6, (door_x - 4) if door_x else x1 - 6
                win = self.rect(sx0, int(fy + floor_h * 0.3), sx1, y_base - 3)
                t = np.clip((self.yy - fy) / floor_h, 0, 1)
                self.cv.paint(win, dramp([(0, '#ffe2a0'), (1, '#e8a050')], t, 4))
                for gx in range(sx0, sx1, 16):
                    self.paint(self.rect(gx, int(fy + floor_h * 0.3), gx, y_base - 3), '#5a3a28')
                for sy_ in (int(fy + floor_h * 0.55), int(fy + floor_h * 0.78)):
                    self.paint(self.rect(sx0, sy_, sx1, sy_), '#7a5034')
                    for k in range(sx0 + 1, sx1 - 1, 2):
                        self.paint(self.rect(k, sy_ - 3, k, sy_ - 1), ['#8a3a3a', '#3a5a8a', '#4a7a4a', '#c8a050', '#e8d8b0'][(k * 7 + seed) % 5])
                self.paint(self.rect(sx0 - 1, int(fy + floor_h * 0.3) - 1, sx1 + 1, int(fy + floor_h * 0.3)), '#5a3a28')
            else:
                for wx in range(x0 + 8, x1 - 10, 22):
                    wy0, wy1 = int(fy + floor_h * 0.28), int(fy + floor_h * 0.78)
                    self.paint(self.rect(wx, wy0, wx + 8, wy1), '#4a5a6e')
                    self.paint(self.rect(wx + 1, wy0 + 1, wx + 3, wy1 - 1), '#7a90a8')
                    self.paint(self.rect(wx + 4, wy0, wx + 4, wy1), '#e8dcc4')
                    for sx in (wx - 4, wx + 9):
                        self.paint(self.rect(sx, wy0, sx + 3, wy1), shutters)
                        self.paint(self.rect(sx, wy0, sx, wy1), lerp(rgb(shutters), rgb('#ffffff'), 0.3))
                    self.paint(self.rect(wx - 2, wy1 + 1, wx + 10, wy1 + 3), '#8a5a36')
                    for k in range(wx - 1, wx + 10, 2):
                        self.paint(self.rect(k, wy1 - 1, k, wy1), ['#e86a80', '#f4f0e0', '#9a88e0'][(k + f) % 3])
                    self.paint(self.rect(wx - 2, wy1, wx + 10, wy1), '#4e7a3a')
        if door_x:
            dw, dh = 10, int(floor_h * 0.72)
            d = self.rect(door_x, y_base - dh, door_x + dw, y_base)
            self.shade(d, ['#3a2418', '#5a3a26', '#7a5034', '#946440'], light=(-1, -0.3))
            self.paint(self.rect(door_x - 1, y_base - dh - 1, door_x + dw + 1, y_base - dh), '#b8a488')
            self.paint(self.rect(door_x + dw - 3, y_base - dh // 2, door_x + dw - 2, y_base - dh // 2), '#e8c070')
            door = (door_x - 2, y_base - dh - 2, door_x + dw + 2, y_base)
        return door

    def bunting(self, pts, colors=('#e8b440', '#4e7aa8', '#d8644a', '#f4ead6')):
        """Wimpelkette entlang eines durchhaengenden Seils."""
        (xa, ya), (xb, yb) = pts
        for k, x in enumerate(range(int(xa), int(xb), 6)):
            t = (x - xa) / max(1, xb - xa)
            y = ya + (yb - ya) * t + np.sin(t * np.pi) * 8
            self.paint(self.rect(x, int(y), x + 6, int(y)), '#5a4a3a')
            self.paint(self.poly([(x + 1, y + 1), (x + 5, y + 1), (x + 3, y + 6)]), colors[k % len(colors)])

    # ------------------------------------------------------------------ Requisiten
    def bench(self, x, y, s=1.0, facing=1):
        """Holzbank mit gusseisernen Beinen (wie im Riverside-Quarter-Blatt), Vorderkante auf y."""
        w = int(40 * s)
        self.shadow(x + w * 0.1, y + 1, w * 0.55, 3 * s, 0.6)
        for k in range(2):
            yy = int(y - 16 * s + k * 4 * s)
            self.paint(self.rect(x - w // 2, yy, x + w // 2, yy + max(1, int(2.5 * s))), '#8a5a36')
            self.paint(self.rect(x - w // 2, yy, x + w // 2, yy), '#c8925a')
        seat_y = int(y - 6 * s)
        self.paint(self.rect(x - w // 2 - 1, seat_y, x + w // 2 + 1, seat_y + max(1, int(2.5 * s))), '#9a6a40')
        self.paint(self.rect(x - w // 2 - 1, seat_y, x + w // 2 + 1, seat_y), '#e0a868')
        for lx in (x - w // 2 + 1, x + w // 2 - 2):
            self.paint(self.rect(lx, int(y - 18 * s), lx + max(1, int(2 * s)), y), '#2e3436')
            self.paint(self.rect(lx - 1, y - 1, lx + max(1, int(2 * s)) + 1, y), '#2e3436')
        return (x - w // 2 - 2, int(y - 20 * s), x + w // 2 + 2, y + 2)

    def lamp(self, x, y, s=1.0, lit=False):
        h = int(52 * s)
        self.shadow(x + 3 * s, y, 5 * s, 1.5 * s, 0.6)
        self.paint(self.rect(x, y - h, x + max(1, int(2 * s)), y), '#2a3032')
        self.paint(self.rect(x - int(2 * s), y - int(3 * s), x + int(4 * s), y), '#2a3032')
        top = y - h
        self.paint(self.poly([(x - 4 * s, top), (x + 6 * s, top), (x + 4 * s, top - 8 * s), (x - 2 * s, top - 8 * s)]),
                   '#ffd890' if lit else '#e8e0c0')
        self.paint(self.rect(int(x - 4 * s), int(top - 10 * s), int(x + 6 * s), int(top - 8 * s)), '#2a3032')
        self.paint(self.rect(int(x - 4 * s), int(top), int(x + 6 * s), int(top + 1)), '#2a3032')
        if lit:
            self.cv.add(glow(W, H, [(x + 1, top - 4 * s, 1.0)], 6 * s, '#ffb050', 0.5, steps=3))
        return (int(x - 6 * s), int(top - 12 * s), int(x + 8 * s), y)

    def planter(self, x, y, w, s=1.0, flowers=('#f4f0e0', '#e88aa0', '#9a88e0', '#f0c850')):
        h = int(8 * s)
        self.paint(self.rect(x, y - h, x + w, y), '#8a5a36')
        self.paint(self.rect(x, y - h, x + w, y - h), '#c8925a')
        bush = np.zeros((H, W), bool)
        for k in range(max(2, w // 5)):
            bush |= self.ellipse(x + 3 + k * 5 * (w / max(1, w)), y - h - 3 * s, 4 * s, 3.5 * s)
        self.shade(bush, ['#2e4a2e', '#46703a', '#6a9a48', '#94bc60'], fur=0.5, seed=x)
        for _ in range(int(w * 0.8)):
            fx, fy = int(self.rng.uniform(x, x + w)), int(self.rng.uniform(y - h - 7 * s, y - h))
            if bush[min(H - 1, max(0, fy)), min(W - 1, max(0, fx))]:
                self.paint(self.rect(fx, fy, fx, fy), flowers[int(self.rng.integers(0, len(flowers)))])

    def flowerbed(self, x0, x1, y, s=1.0, seed=0, colors=('#f4f0e0', '#e88aa0', '#9a88e0', '#f0c850', '#e86a50')):
        bed = np.zeros((H, W), bool)
        r = np.random.default_rng(seed)
        for x in np.arange(x0, x1, 5 * s):
            bed |= self.ellipse(x, y - 3 * s, r.uniform(4, 7) * s, r.uniform(3, 5) * s)
        self.shade(bed, ['#2a4428', '#3e6232', '#5a8640', '#80a854', '#a4c470'], fur=0.6, seed=seed)
        for _ in range(int((x1 - x0) * 1.2)):
            fx, fy = int(r.uniform(x0, x1)), int(r.uniform(y - 9 * s, y))
            if 0 <= fy < H and 0 <= fx < W and bed[fy, fx]:
                c = colors[int(r.integers(0, len(colors)))]
                self.paint(self.rect(fx, fy, fx, fy), c)
                if s > 0.8:
                    self.paint(self.rect(fx + 1, fy, fx + 1, fy), lerp(rgb(c), rgb('#ffffff'), 0.3))

    def fence(self, y, x0, x1, s=1.0, col='#7a5236', lit='#c08458'):
        h = int(16 * s)
        for x in range(int(x0), int(x1), max(6, int(22 * s))):
            self.paint(self.rect(x, y - h, x + max(1, int(2.5 * s)), y), col)
            self.paint(self.rect(x, y - h, x, y), lit)
        for k in (0.25, 0.65):
            yy = int(y - h * (1 - k))
            self.paint(self.rect(int(x0), yy, int(x1), yy + max(1, int(1.6 * s))), col)
            self.paint(self.rect(int(x0), yy, int(x1), yy), lit)

    def stone_wall(self, y0, y1, x0, x1, top='#d8ccb8', face=('#8a8278', '#a49a8c', '#bcb2a2')):
        m = self.rect(x0, y0, x1, y1)
        n = noise(W, H, 3, self.seed + 41, 2)
        self.cv.paint(m, dramp([(0, face[0]), (0.5, face[1]), (1, face[2])], n, 3))
        for yy_ in range(y0 + 4, y1, 5):
            self.paint(m & (self.yy == yy_), '#6a625a')
            off = 0 if (yy_ // 5) % 2 else 4
            for xx_ in range(x0 + off, x1, 9):
                self.paint(m & (self.xx == xx_) & (self.yy > yy_ - 5) & (self.yy < yy_), '#6a625a')
        self.paint(self.rect(x0, y0, x1, y0 + 1), top)

    def awning(self, x0, x1, y, depth=10, c1='#d8644a', c2='#f4ead6'):
        m = self.poly([(x0, y), (x1, y), (x1 + 3, y + depth), (x0 - 3, y + depth)])
        stripes = ((self.xx - x0) // 5) % 2 == 0
        self.cv.paint(m, np.where(stripes[..., None], rgb(c1), rgb(c2)))
        self.cv.paint(m & (self.yy >= y + depth - 1), rgb(c1) * 0.7)
        for x in range(int(x0 - 3), int(x1 + 3), 5):
            self.paint(self.ellipse(x + 2, y + depth + 1, 2.5, 1.6), c1 if ((x - x0) // 5) % 2 == 0 else c2)

    # ------------------------------------------------------------------ Spielangaben
    # ------------------------------------------------------------------ Wildnis
    def rock(self, x, y, rx, ry, cols=('#4a4a48', '#6a6a60', '#8a867a', '#b0a894'), seed=0, moss=None):
        """Einzelner Stein mit Facetten, Rissen und optional Moos (nature.stone)."""
        cols = tuple(cols) if len(cols) >= 5 else tuple(cols) + ('#d0c8b8',)
        rr = np.random.default_rng(abs(int(seed)) * 31 + int(x) * 7 + int(y))
        m = N.stone(self.cv, x, y, rx, ry, rr, cols=cols, moss=moss)
        full = np.zeros((H, W), bool)
        if m is not None:
            ys, xs = np.nonzero(m)
            win_x0 = max(0, int(np.floor(x - rx * 1.6)))
            win_y0 = max(0, int(np.floor(y - ry * 2.2)))
            full[ys + win_y0, xs + win_x0] = True
        return full

    def log_seat(self, x, y, s=1.0, bark=('#3e2c22', '#5e4232', '#7e5c44', '#9a7656')):
        """Liegender Baumstamm als Sitzplatz, Vorderkante auf y; gibt die Trefferflaeche zurueck."""
        w, h = int(46 * s), int(9 * s)
        self.shadow(x + 3, y, w * 0.55, 2.5 * s, 0.6)
        body = self.rect(x - w // 2, y - h, x + w // 2, y)
        self.shade(body, list(bark), light=(-0.3, -1))
        for k in range(x - w // 2 + 3, x + w // 2 - 4, 5):
            self.paint(self.rect(k, y - h + 2, k + 2, y - h + 2), bark[0])
        end = self.ellipse(x + w // 2, y - h / 2, 3 * s, h / 2 + 0.5)
        self.paint(end, '#c8a070')
        self.paint(self.ellipse(x + w // 2, y - h / 2, 1.5 * s, h / 4), '#9a7048')
        self.paint(self.rect(x - w // 2 + 4, y - h - 1, x + w // 2 - 4, y - h), '#5a8a3a')
        return (x - w // 2 - 2, y - h - 4, x + w // 2 + 4, y + 2)

    def stone_seat(self, x, y, s=1.0):
        self.rock(x - 10 * s, y, 12 * s, 8 * s, seed=3)
        self.rock(x + 10 * s, y + 1, 10 * s, 7 * s, seed=4)
        slab = self.rect(int(x - 22 * s), int(y - 12 * s), int(x + 22 * s), int(y - 8 * s))
        self.shade(slab, ['#5a5852', '#7a766c', '#9a948a', '#c4bcae'], light=(-0.3, -1))
        return (int(x - 24 * s), int(y - 16 * s), int(x + 24 * s), y + 2)

    def campfire(self, x, y, s=1.0):
        for k in range(8):
            a = k / 8 * 2 * np.pi
            self.rock(int(x + np.cos(a) * 9 * s), int(y + np.sin(a) * 3 * s), 3 * s, 2.5 * s, seed=k)
        for d in (-1, 1):
            self.paint(self.m().line([(x - 7 * s, y - 1), (x + 7 * s, y - 4 * s * d - 1)], max(1, int(2 * s))).a > 0, '#5a3a26')
        flame = self.poly([(x - 5 * s, y - 2), (x - 2 * s, y - 14 * s), (x, y - 8 * s), (x + 2 * s, y - 17 * s), (x + 5 * s, y - 2)])
        self.paint(flame, '#e8602a')
        self.paint(self.poly([(x - 3 * s, y - 2), (x, y - 11 * s), (x + 3 * s, y - 2)]), '#f8c040')
        self.paint(self.poly([(x - 1.5 * s, y - 2), (x, y - 6 * s), (x + 1.5 * s, y - 2)]), '#fff4c0')
        self.cv.add(glow(W, H, [(x, y - 6 * s, 1.0)], 24 * s, '#ff8a30', 0.4, steps=4))

    def tent(self, x, y, w=60, h=40, cloth=('#7a4a2a', '#a8683a', '#c8884a', '#e0a860')):
        body = self.poly([(x - w / 2, y), (x, y - h), (x + w / 2, y)])
        self.shade(body, list(cloth), light=(-1, -0.2))
        door = self.poly([(x - w * 0.14, y), (x, y - h * 0.62), (x + w * 0.14, y)])
        self.paint(door, '#3a2418')
        self.cv.add(glow(W, H, [(x, y - h * 0.2, 1.0)], 6, '#ffb050', 0.4, steps=3))
        self.paint(self.m().line([(x, y - h), (x, y - h - 6)]).a > 0, '#5a3a26')
        self.paint(self.m().line([(x - w / 2, y), (x - w / 2 - 8, y + 2)]).a > 0, '#c8b898')
        self.paint(self.m().line([(x + w / 2, y), (x + w / 2 + 8, y + 2)]).a > 0, '#c8b898')
        self.outline(body, strength=0.35)

    def palm(self, x, base, h, seed=0, lean=0.25):
        pts = [(x + lean * h * (t ** 1.5), base - h * t) for t in np.linspace(0, 1, 12)]
        for k, (px_, py_) in enumerate(pts[:-1]):
            self.paint(self.m().line([(px_, py_), pts[k + 1]], max(2, int(h / 22))).a > 0,
                       ['#7a5a3a', '#9a7448'][k % 2])
        tx, ty = pts[-1]
        r = np.random.default_rng(seed)
        for k in range(8):
            a = -np.pi / 2 + (k - 3.5) / 3.5 * 2.2 + r.uniform(-0.15, 0.15)
            ln = h * r.uniform(0.35, 0.5)
            p = [(tx + np.cos(a) * ln * t, ty + np.sin(a) * ln * t + ln * 0.5 * t * t) for t in np.linspace(0, 1, 8)]
            self.paint(self.m().line(p, max(2, int(h / 25))).a > 0, ['#2e5a2a', '#3e7a34', '#5a9a40'][k % 3])
            for j, (qx, qy) in enumerate(p[1:]):
                if j % 2 == 0:
                    self.paint(self.m().line([(qx, qy), (qx + 3 * np.cos(a + 1.2), qy + 4)]).a > 0, '#3e7a34')
        self.paint(self.ellipse(tx, ty + 2, 3, 2), '#6a4a2a')

    def mushroom(self, x, y, s=1.0, cap='#c84a3a', glow_c=None):
        self.paint(self.rect(int(x - 1 * s), int(y - 5 * s), int(x + 1 * s), y), '#f0e6d0')
        capm = self.ellipse(x, y - 5 * s, 4 * s, 2.5 * s) & (self.yy <= y - 5 * s + 0.5)
        self.paint(capm, cap)
        self.paint(self.rect(int(x - 2 * s), int(y - 7 * s), int(x - 1 * s), int(y - 7 * s)), '#f4f0e0')
        if glow_c:
            self.cv.add(glow(W, H, [(x, y - 5 * s, 1.0)], 5 * s, glow_c, 0.4, steps=3))

    def crystal(self, x, y, h, col=('#3a5aa8', '#5a8ae0', '#8ac0f0', '#d8f0ff'), glow_c='#6ab0ff'):
        m = self.poly([(x - h * 0.18, y), (x - h * 0.12, y - h * 0.8), (x, y - h), (x + h * 0.14, y - h * 0.75), (x + h * 0.18, y)])
        self.shade(m, list(col), light=(-1, -0.3))
        self.paint((self.m().line([(x, y - h), (x, y)]).a > 0) & m, col[3])
        self.cv.add(glow(W, H, [(x, y - h * 0.5, 1.0)], h * 0.6, glow_c, 0.35, steps=3))

    def reeds(self, x0, x1, y0, y1, seed=0, col='#6a7a3a', head='#7a4a2a', density=3):
        rr = np.random.default_rng(seed)
        for x in range(int(x0), int(x1), density):
            h = int(rr.uniform(12, 32))
            base = int(rr.uniform(y0, y1))
            self.paint(self.m().line([(x, base), (x + rr.uniform(-2, 2), base - h)]).a > 0, col)
            if rr.random() < 0.3:
                self.paint(self.ellipse(x, base - h + 2, 1, 3), head)

    def fireflies(self, n, x0, x1, y0, y1, seed=0, col='#f0f080'):
        rr = np.random.default_rng(seed)
        pts = [(rr.uniform(x0, x1), rr.uniform(y0, y1), 1.0) for _ in range(n)]
        for x, y, _ in pts:
            self.cv.c[int(y), int(x)] = rgb('#ffffe0')
        self.cv.add(glow(W, H, pts, 2.5, col, 0.6, steps=2))

    def dusk(self, k=0.5, tint='#5a5a9a', sky=None, lights=(), windows=()):
        """Abendstimmung: alles ausser dem Himmel kuehl abgedunkelt, dann warme Lichter darueber.
        [sky] ist ein Abzug der Leinwand direkt nach dem Himmel - was sich seither nicht geaendert
        hat, ist Himmel und bleibt, wie er ist. [lights] sind (x, y, radius) fuer Lichthoefe,
        [windows] Rechtecke, die warm leuchten."""
        keep = np.all(np.abs(self.cv.c - sky) < 1e-6, axis=-1) if sky is not None else np.zeros((H, W), bool)
        old = self.cv.c.copy()
        self.cv.c = self.cv.c * lerp(np.ones(3), rgb(tint) * 1.5, k)
        self.cv.c[keep] = old[keep]
        for x0, y0, x1, y1 in windows:
            m = self.rect(x0, y0, x1, y1)
            self.cv.c[m] = np.maximum(self.cv.c[m], old[m] * rgb('#ffd8a0') * 1.15)
        for x, y, r in lights:
            self.cv.add(glow(W, H, [(x, y, 1.0)], r, '#ffa850', 0.45, steps=3))
            self.cv.add(glow(W, H, [(x, y, 1.0)], max(1.5, r / 4), '#fff0c0', 0.6, steps=2))

    def walk(self, farY, nearY, farLeft, farRight, nearLeft, nearRight, farH, nearH):
        self.meta['walk'] = dict(farY=farY, nearY=nearY, farLeft=farLeft, farRight=farRight,
                                 nearLeft=nearLeft, nearRight=nearRight, farHeight=farH, nearHeight=nearH)

    def spot(self, station, box, standX, standY):
        self.meta['spots'].append(dict(station=station, box=[float(v) for v in box],
                                       standX=float(standX), standY=float(standY)))

    def block(self, *dirs):
        self.meta['blocked'] += list(dirs)

    # ------------------------------------------------------------------ Abschluss
    def finish(self, path, colors=160, vignette=0.35):
        vig = np.hypot((self.xx - W / 2) / (W * 0.62), (self.yy - H * 0.5) / (H * 0.75))
        self.cv.c *= np.clip(1.06 - vig ** 2 * vignette + self.by * 0.035, 0.6, 1.08)[..., None]
        self.cv.c = quantize(self.cv.c, colors)
        self.cv.save(path)
        return self.meta
