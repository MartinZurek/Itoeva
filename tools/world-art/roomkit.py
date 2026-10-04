"""Baukasten fuer die Zimmer daheim im Stil der Wohnraum-Studie (world-studies/cozy-home):
Holzdielen, warmer Putz mit Holzbalken, Lampenlicht am Abend, Fenster mit Blick nach draussen,
Pflanzen, Teppiche. Zentralperspektive ueber room3d.Room; Moebel als Quader mit Licht.

Jedes Zimmer liefert wie die Aussenorte seine Spielangaben (Gehflaeche, Plaetze) mit.
"""
import numpy as np
from px import Canvas, rgb, ramp, dramp, noise, glow, quantize, lerp, MaskPen
from room3d import Room

W, H = 480, 270
FLOOR, CEIL, LEFT, RIGHT, BACK = range(5)


class CozyRoom:
    def __init__(self, seed=1, plaster=('#d8b890', '#e6c8a0'), wood=('#6a4430', '#7e5238', '#8e6040'),
                 lights=None, ambient='#5a4440'):
        self.cv = Canvas(W, H)
        self.yy, self.xx = self.cv.yy, self.cv.xx
        self.rng = np.random.default_rng(seed)
        self.seed = seed
        self.room = Room(self.cv, f=230.0, vp=(232, 100), cam_h=0.62, half_w=1.05, height=1.45, back=2.7)
        self.room.lights = lights or [(np.array([0.0, 1.35, 1.9]), rgb('#ffb868'), 1.4)]
        self.room.ambient = rgb(ambient)
        self.which, X, Y, Z = self.room.planes()
        self.X, self.Y, self.Z = X, Y, Z
        self.P = np.stack([X, Y, Z], axis=-1)
        self.meta = {'walk': dict(farY=172, nearY=236, farLeft=136, farRight=336, nearLeft=26, nearRight=440,
                                  farHeight=42, nearHeight=78),
                     'spots': [], 'blocked': ['UP', 'DOWN'], 'cropTop': 0.7}
        self._planes(plaster, wood)

    # ------------------------------------------------------------------ Ebenen
    def _shade(self, mask, albedo, normal):
        self.cv.paint(mask, albedo * self.room.light_dithered(self.P, normal))

    def _planes(self, plaster, wood):
        X, Y, Z, which = self.X, self.Y, self.Z, self.which
        fl = which == FLOOR
        plank = np.floor(X / 0.13).astype(int)
        off = ((plank * 2654435761) % 997) / 997
        joint = np.floor(Z / 0.8 + off).astype(int)
        pv = (((plank * 7919 + joint * 104729) * 2654435761) % 997) / 997
        woodc = ramp([(0, wood[0]), (0.5, wood[1]), (1, wood[2])], pv)
        self._shade(fl, woodc, (0, 1, 0))
        gx = np.abs(X / 0.13 - np.round(X / 0.13)) * 0.13
        gz = np.abs((Z / 0.8 + off) - np.round(Z / 0.8 + off)) * 0.8
        pw = Z / self.room.f
        self.cv.paint(fl & ((gx < pw * 0.7) | (gz < pw * 0.6)), self.cv.c * 0.6)
        # Waende: warmer Putz, nur leicht wolkig; Fachwerk aus dunklem Holz
        nz = noise(W, H, 9, self.seed + 9, 2)
        for side, n in ((BACK, (0, 0, -1)), (LEFT, (1, 0, 0)), (RIGHT, (-1, 0, 0))):
            m = which == side
            pl = lerp(rgb(plaster[0]), rgb(plaster[1]), np.clip(nz * 1.4 - 0.2, 0, 1)[..., None])
            self._shade(m, pl, n)
        beam = rgb('#4e3224')
        walls = (which == BACK) | (which == LEFT) | (which == RIGHT)
        # senkrechte Pfosten an den Zimmerecken und in den Seitenwaenden, ein Riegel auf Brusthoehe
        corner = (which == BACK) & ((np.abs(X + 1.05) < 0.05) | (np.abs(X - 1.05) < 0.05))
        posts = ((which == LEFT) | (which == RIGHT)) & (np.abs(((Z - 0.35) / 0.9) % 1 - 0.5) > 0.47)
        rail = walls & (np.abs(Y - 1.22) < 0.03)
        self._shade(corner | posts | rail, beam, (0, 0, -1))
        self._shade(walls & (np.abs(Y - 1.25) < 0.004 + Z * 0.001), rgb('#8a5a3a'), (0, 1, 0))
        # Holzvertaefelung unten
        panel = walls & (Y < 0.34)
        pv = np.abs((np.where(which == BACK, X, Z) / 0.22) % 1 - 0.5) > 0.45
        self._shade(panel, np.where(pv[..., None], rgb(wood[0]) * 0.75, rgb(wood[1])), (0, 0, -1))
        self._shade(walls & (np.abs(Y - 0.34) < 0.018), rgb(wood[2]) * 1.2, (0, 1, 0))
        skirt = walls & (Y < 0.04)
        self._shade(skirt, rgb('#4a2e20'), (0, 0, -1))
        ce = which == CEIL
        beams = np.abs(((X / 0.45) % 1) - 0.5) > 0.36
        self._shade(ce, np.where(beams[..., None], rgb('#4a2e22'), rgb('#7a5640')), (0, -1, 0))

    # ------------------------------------------------------------------ Bauteile
    def rect_on_back(self, X0, X1, Y0, Y1):
        x0, y0 = self.room.proj(X0, Y1, self.room.back)
        x1, y1 = self.room.proj(X1, Y0, self.room.back)
        return int(round(x0)), int(round(y0)), int(round(x1)), int(round(y1))

    def window(self, X0, X1, Y0, Y1, view='night'):
        """Fenster in der Rueckwand mit Blick hinaus: Nacht mit Mond ueber Tannen, oder Tag."""
        x0, y0, x1, y1 = self.rect_on_back(X0, X1, Y0, Y1)
        win = (self.xx >= x0) & (self.xx <= x1) & (self.yy >= y0) & (self.yy <= y1)
        t = np.clip((self.yy - y0) / max(1, y1 - y0), 0, 1)
        if view == 'night':
            sky = dramp([(0, '#1a2650'), (0.6, '#2e4a80'), (1, '#4a6a9a')], t, 10)
        else:
            sky = dramp([(0, '#7ab0e0'), (0.7, '#b8d8ee'), (1, '#e8f0ea')], t, 10)
        self.cv.paint(win, sky)
        # Tannen und Huegel vor dem Himmel
        base = y1 - (y1 - y0) * 0.18
        for k in range(x0, x1, 5):
            h = 6 + (k * 13) % 9
            tri = MaskPen(W, H).poly([(k - 3, base + 2), (k + 1.5, base - h), (k + 6, base + 2)]).a > 0
            self.cv.paint(tri & win, rgb('#1a2a3a') if view == 'night' else rgb('#3a5a4a'))
        self.cv.paint(win & (self.yy > base), rgb('#16202e') if view == 'night' else rgb('#4a6a4a'))
        if view == 'night':
            mx, my = x0 + (x1 - x0) * 0.68, y0 + (y1 - y0) * 0.25
            self.cv.add(glow(W, H, [(mx, my, 1.0)], 5, '#c8d8ff', 0.4, steps=3) * win[..., None])
            moon = (np.hypot(self.xx - mx, self.yy - my) < 3.5) & win
            self.cv.paint(moon, rgb('#f4f0d8'))
            for _ in range(10):
                sx, sy = self.rng.integers(x0, x1), self.rng.integers(y0, int(base) - 8)
                self.cv.c[sy, sx] = rgb('#d8e0ff')
        # Rahmen, Sprossen, Fensterbank
        frame = rgb('#5a3a26')
        mid = (x0 + x1) // 2
        self.cv.paint(win & ((np.abs(self.xx - mid) <= 1) | (np.abs(self.yy - (y0 + (y1 - y0) * 0.45)) <= 0)), frame)
        ring = ((self.xx >= x0 - 3) & (self.xx <= x1 + 3) & (self.yy >= y0 - 3) & (self.yy <= y1 + 3)) & ~win
        self.cv.paint(ring, frame)
        self.cv.paint((self.xx >= x0 - 5) & (self.xx <= x1 + 5) & (self.yy >= y1 + 2) & (self.yy <= y1 + 5), rgb('#8a5a3a'))
        self.cv.paint((self.xx >= x0 - 5) & (self.xx <= x1 + 5) & (self.yy == y1 + 2), rgb('#d09868'))
        return (x0, y0, x1, y1)

    def door_back(self, X0, X1, H_=1.0):
        """Holztuer in der Rueckwand; gibt die Trefferflaeche zurueck."""
        x0, y0, x1, y1 = self.rect_on_back(X0, X1, 0.0, H_)
        d = (self.xx >= x0) & (self.xx <= x1) & (self.yy >= y0) & (self.yy <= y1)
        light = self.room.light_at(np.array([(X0 + X1) / 2, 0.5, self.room.back]), (0, 0, -1))
        planks = ((self.xx - x0) // 4) % 2 == 0
        col = np.where(planks[..., None], rgb('#7a4e32'), rgb('#6a4228')) * light
        self.cv.paint(d, col)
        self.cv.paint(((self.xx >= x0 - 2) & (self.xx <= x1 + 2) & (self.yy >= y0 - 2) & (self.yy <= y1)) & ~d,
                      rgb('#4a2e20') * light)
        self.cv.paint((self.xx == x1 - 3) & (self.yy == (y0 + y1) // 2), rgb('#e8c070'))
        return (x0 - 2, y0 - 2, x1 + 2, y1)

    def rug(self, X0, X1, Z0, Z1, c1='#8a3a32', c2='#d8a050', c3='#2e3a5a', round_=False):
        """Teppich mit Rand, Borduere und Rautenmuster (oder rund mit Ringen)."""
        fl = self.which == FLOOR
        ru, rz = (self.X - X0) / (X1 - X0), (self.Z - Z0) / (Z1 - Z0)
        if round_:
            r = np.hypot(ru - 0.5, rz - 0.5) * 2
            m = fl & (r < 1)
            col = np.where(((r * 7) % 1 < 0.28)[..., None], rgb(c2), rgb(c1))
            col = np.where((r > 0.86)[..., None], rgb(c3), col)
        else:
            m = fl & (ru > 0) & (ru < 1) & (rz > 0) & (rz < 1)
            edge = np.minimum(np.minimum(ru, 1 - ru), np.minimum(rz, 1 - rz))
            diamond = np.abs(ru - 0.5) * 1.2 + np.abs(rz - 0.5) < 0.24
            motif = (np.abs(((ru * 7) % 1) - 0.5) + np.abs(((rz * 5) % 1) - 0.5)) < 0.16
            col = np.where(motif[..., None], rgb(c2) * 0.8, rgb(c1))
            col = np.where(diamond[..., None], rgb(c3), col)
            col = np.where((np.abs(np.abs(ru - 0.5) * 1.2 + np.abs(rz - 0.5) - 0.24) < 0.025)[..., None], rgb(c2), col)
            col = np.where((edge < 0.11)[..., None], rgb(c2), col)
            col = np.where((edge < 0.07)[..., None], rgb(c3), col)
        self._shade(m, col, (0, 1, 0))

    def box(self, *a, **k):
        return self.room.box(*a, **k)

    def plant(self, X, Z, size=1.0, pot='#a85a3a'):
        self.box(X - 0.08 * size, X + 0.08 * size, 0.0, 0.18 * size, Z - 0.08 * size, Z + 0.08 * size, pot,
                 top='#4a3020', edge='#e09060')
        pc = self.room.proj(X, 0.18 * size, Z)
        leaves = np.zeros((H, W), bool)
        for _ in range(14):
            a = self.rng.uniform(-2.7, -0.4)
            ln = self.rng.uniform(10, 24) * size * 1.9 / Z
            ex, ey = pc[0] + np.cos(a) * ln * 0.8, pc[1] + np.sin(a) * ln
            leaves |= MaskPen(W, H).line([pc, ((pc[0] + ex) / 2, (pc[1] + ey) / 2 - 2), (ex, ey)], 3).a > 0
        self.cv.paint(leaves, rgb('#2e4a2e') * 1.2)
        self.cv.paint(leaves & ~np.roll(leaves, 1, axis=0), rgb('#7aa05a'))
        self.cv.paint(leaves & ~np.roll(leaves, 1, axis=1), rgb('#5a8a4a'))

    def hanging_plant(self, x, y, length=26):
        stem = MaskPen(W, H).line([(x, 0), (x, y)]).a > 0
        self.cv.paint(stem, rgb('#3a2a20'))
        self.cv.paint(MaskPen(W, H).ellipse(x - 5, y - 2, x + 5, y + 4).a > 0, rgb('#a85a3a'))
        for k in range(10):
            vx = x + self.rng.uniform(-9, 9)
            v = MaskPen(W, H).line([(x + (vx - x) * 0.3, y + 2), (vx, y + self.rng.uniform(6, length))], 2).a > 0
            self.cv.paint(v, rgb(['#3e6a3a', '#5a8a4a', '#7aa05a'][k % 3]))

    def picture(self, X0, X1, Y0, Y1, colors=('#c88a6a', '#e8c890', '#6a8a5a')):
        x0, y0, x1, y1 = self.rect_on_back(X0, X1, Y0, Y1)
        self.cv.paint((self.xx >= x0 - 2) & (self.xx <= x1 + 2) & (self.yy >= y0 - 2) & (self.yy <= y1 + 2), rgb('#7a5a3a'))
        t = np.clip((self.yy - y0) / max(1, y1 - y0), 0, 1)
        self.cv.paint((self.xx >= x0) & (self.xx <= x1) & (self.yy >= y0) & (self.yy <= y1),
                      dramp([(0, colors[0]), (0.6, colors[1]), (1, colors[2])], t, 5))

    def pendant(self, X, Y, Z):
        p = self.room.proj(X, Y, Z)
        self.cv.paint(MaskPen(W, H).line([(p[0], 0), (p[0], p[1] - 4)]).a > 0, rgb('#2a2020'))
        self.cv.paint(MaskPen(W, H).poly([(p[0] - 8, p[1] + 2), (p[0] - 4, p[1] - 5), (p[0] + 4, p[1] - 5), (p[0] + 8, p[1] + 2)]).a > 0, rgb('#e8b050'))
        self.cv.paint(MaskPen(W, H).line([(p[0] - 8, p[1] + 2), (p[0] + 8, p[1] + 2)]).a > 0, rgb('#fff0c0'))
        self.cv.add(glow(W, H, [(p[0], p[1] + 4, 1.0)], 14, '#ff9a40', 0.25, steps=3))

    def glow_at(self, X, Y, Z, r=8, color='#ffb050', k=0.4):
        p = self.room.proj(X, Y, Z)
        self.cv.add(glow(W, H, [(p[0], p[1], 1.0)], r, color, k, steps=3))

    # ------------------------------------------------------------------ Moebel
    def legs(self, X0, X1, Z0, Z1, h, color='#3a2418', t=0.035):
        for x in (X0, X1 - t):
            for z in (Z0, Z1 - t):
                self.box(x, x + t, 0.0, h, z, z + t, color)

    def table(self, X0, X1, Z0, Z1, h=0.42, wood='#7a4a30', top='#9a6444'):
        self.legs(X0 + 0.02, X1 - 0.02, Z0 + 0.02, Z1 - 0.02, h - 0.04)
        self.box(X0, X1, h - 0.04, h, Z0, Z1, wood, top=top, edge='#d09868')

    def sofa(self, X0, X1, Z0, Z1, color='#b85a4a', light='#d8826a', cushions=('#e8c890', '#6a8a5a')):
        """Sofa mit Rueckenlehne hinten (bei Z1), runden Armlehnen, Sitzpolstern und Kissen."""
        c, l = rgb(color), rgb(light)
        self.box(X0 + 0.04, X1 - 0.04, 0.06, 0.52, Z1 - 0.12, Z1, c * 0.85, top=l * 0.95, edge=l * 1.2)
        self.box(X0 + 0.06, X1 - 0.06, 0.06, 0.2, Z0 + 0.02, Z1 - 0.1, c * 0.8, top=l)
        n = max(2, int(round((X1 - X0 - 0.2) / 0.3)))
        w = (X1 - X0 - 0.2) / n
        for k in range(n):
            x = X0 + 0.1 + k * w
            self.box(x + 0.006, x + w - 0.006, 0.2, 0.27, Z0 + 0.01, Z1 - 0.12, c * 1.05, top=l * 1.08, edge=l * 1.3)
        for k, cc in enumerate(cushions):
            x = X0 + 0.13 + k * (X1 - X0 - 0.42)
            cr = rgb(cc)
            self.box(x, x + 0.16, 0.27, 0.44, Z1 - 0.2, Z1 - 0.14, cr * 0.95, top=cr * 1.1, edge=cr * 1.3)
        for x in (X0, X1 - 0.1):
            self.box(x, x + 0.1, 0.06, 0.36, Z0, Z1, c * 0.9, top=l * 1.05, edge=l * 1.3)
        for x in (X0 + 0.02, X1 - 0.06):
            self.box(x, x + 0.04, 0.0, 0.06, Z0 + 0.02, Z0 + 0.05, '#3a2418')

    def shelf(self, X0, X1, Z0, Z1, Y1, boards=4, books=True, deco=True, wood='#6a4230'):
        """Offenes Regal mit Buechern, Toepfen und Glaesern."""
        ys = np.linspace(0.04, Y1 - 0.04, boards + 1)
        cols = ['#8a2a2e', '#2a4a7a', '#3a6a4a', '#c08a3a', '#6a3a6a', '#a85a3a', '#2a5a6a', '#d0b070']
        for b0, b1 in zip(ys[:-1], ys[1:]):
            x = X0 + 0.03
            while x < X1 - 0.06 and books:
                if deco and self.rng.random() < 0.12:
                    jar = rgb(['#5a8a6a', '#c8a060', '#a85a3a'][self.rng.integers(0, 3)])
                    self.box(x, x + 0.07, b0 + 0.02, b0 + 0.02 + min(0.12, (b1 - b0) * 0.6), Z0 + 0.05, Z0 + 0.12,
                             jar, top=jar * 1.2, edge=jar * 1.4)
                    x += 0.09
                    continue
                th = self.rng.uniform(0.025, 0.045)
                ht = min(self.rng.uniform(0.6, 0.85) * (b1 - b0), b1 - b0 - 0.02)
                col = rgb(cols[self.rng.integers(0, len(cols))]) * self.rng.uniform(0.85, 1.1)
                self.box(x, x + th, b0 + 0.02, b0 + 0.02 + ht, Z0 + 0.03, Z1 - 0.03, col, top=col * 0.8, side=col)
                x += th + 0.004
        for b in ys:
            self.box(X0, X1, b, b + 0.025, Z0, Z1, wood, top='#8a5a3e', edge='#c08458')
        for x in (X0 - 0.03, X1):
            self.box(x, x + 0.03, 0.0, Y1 + 0.02, Z0, Z1, wood, side='#8a5a3a', edge='#d09868')

    def wall_shelf(self, side, Z0, Z1, Y1=1.3, depth=0.24, boards=5, wood='#6a4230'):
        """Buecherwand an der linken (-1) oder rechten (+1) Wand: Buchruecken zum Raum hin.
        Gibt die Maske zurueck."""
        w = self.room.half_w
        X0, X1 = (-w, -w + depth) if side < 0 else (w - depth, w)
        cols = ['#8a2a2e', '#2a4a7a', '#3a6a4a', '#c08a3a', '#6a3a6a', '#a85a3a', '#2a5a6a', '#d0b070', '#4a2a3a']
        ys = list(np.linspace(0.02, Y1 - 0.06, boards + 1))
        total = np.zeros((H, W), bool)
        for b0, b1 in zip(ys[:-1], ys[1:]):
            z = Z1 - 0.02
            while z > Z0 + 0.03:
                th = self.rng.uniform(0.025, 0.05)
                if self.rng.random() < 0.08:
                    z -= 0.07
                    continue
                ht = self.rng.uniform(0.6, 0.85) * (b1 - b0)
                col = rgb(cols[self.rng.integers(0, len(cols))]) * self.rng.uniform(0.85, 1.1)
                total |= self.box(X0 + 0.02, X1 - 0.03, b0 + 0.025, b0 + 0.025 + ht, z - th, z, col, top=col * 0.8, side=col)
                z -= th + 0.004
        for b in ys:
            total |= self.box(X0, X1, b, b + 0.025, Z0, Z1, wood, top='#8a5a3e', edge='#c08458')
        total |= self.box(X0, X1 + 0.005 * side, 0.0, Y1, Z0 - 0.02, Z0, '#7a4a32', side='#8a5a3a', edge='#d09868')
        total |= self.box(X0, X1, Y1, Y1 + 0.05, Z0 - 0.04, Z1, wood, side='#8a5a3e', edge='#d09868')
        return total

    def cabinet(self, X0, X1, Y1, Z0, Z1, wood='#7a4a30', drawers=3, knob='#e8c070'):
        """Kommode mit Schubladen; gibt die Maske zurueck."""
        m = self.box(X0, X1, 0.0, Y1, Z0, Z1, wood, top='#9a6444', edge='#e0a070')
        for k in range(1, drawers):
            y = Y1 * k / drawers
            a, b = self.room.proj(X0 + 0.02, y, Z0), self.room.proj(X1 - 0.02, y, Z0)
            self.cv.paint(MaskPen(W, H).line([a, b]).a > 0, rgb('#3a2418'))
        for k in range(drawers):
            p = self.room.proj((X0 + X1) / 2, Y1 * (k + 0.5) / drawers, Z0)
            self.cv.paint(MaskPen(W, H).ellipse(p[0] - 1, p[1] - 1, p[0] + 1, p[1] + 1).a > 0, rgb(knob))
        return m

    def bed(self, X0, X1, Z0, Z1, blanket='#6a8a5a', pattern='#e8c890', wood='#6a4230'):
        """Holzbett mit Kopfteil hinten (bei Z1), Decke mit Karomuster, Kissen."""
        self.box(X0, X1, 0.0, 0.7, Z1 - 0.06, Z1, wood, top='#8a5a3e', edge='#d09868')
        self.box(X0, X1, 0.0, 0.34, Z0, Z0 + 0.05, wood, top='#8a5a3e', edge='#d09868')
        self.box(X0 + 0.02, X1 - 0.02, 0.1, 0.24, Z0 + 0.05, Z1 - 0.06, '#e8dcc8', top='#f4ecdc')
        m = self.box(X0 + 0.01, X1 - 0.01, 0.12, 0.32, Z0 + 0.03, Z1 - 0.3, blanket, top=blanket, edge=rgb(blanket) * 1.4)
        ys, xs = np.nonzero(m)
        if len(xs):
            chk = m & ((((self.xx // 4) + (self.yy // 3)) % 2) == 0)
            self.cv.paint(chk, self.cv.c * 0.82)
            self.cv.paint(m & (((self.xx - xs.min()) % 8) == 0) & (((self.yy) % 3) != 0), lerp(self.cv.c, rgb(pattern), 0.35))
        for k in range(2 if X1 - X0 > 0.6 else 1):
            x = X0 + 0.06 + k * (X1 - X0) / 2
            self.box(x, x + min(0.26, (X1 - X0) / 2 - 0.1), 0.24, 0.36, Z1 - 0.28, Z1 - 0.1, '#e8d0b0', top='#f8e8cc', edge='#fff4e0')

    def chair(self, X, Z, wood='#7a4a30', back=True, back_h=0.56):
        self.legs(X - 0.1, X + 0.1, Z - 0.1, Z + 0.1, 0.24, color=wood)
        self.box(X - 0.11, X + 0.11, 0.22, 0.26, Z - 0.11, Z + 0.11, wood, top='#9a6444', edge='#d09868')
        if back:
            self.box(X - 0.11, X + 0.11, 0.26, back_h, Z + 0.08, Z + 0.11, wood, top='#9a6444', edge='#d09868')

    def counter(self, X0, X1, Z0, Z1, Y1=0.46, top='#c8b898', wood='#5a7a6a'):
        """Kuechenzeile: Unterschraenke mit Tueren und heller Arbeitsplatte."""
        m = self.box(X0, X1, 0.0, Y1 - 0.03, Z0, Z1, wood, top=wood, side=rgb(wood) * 1.05)
        n = max(1, int(round((X1 - X0) / 0.25)))
        for k in range(1, n):
            x = X0 + (X1 - X0) * k / n
            a, b = self.room.proj(x, 0.04, Z0), self.room.proj(x, Y1 - 0.06, Z0)
            self.cv.paint(MaskPen(W, H).line([a, b]).a > 0, rgb('#2e4038'))
        for k in range(n):
            p = self.room.proj(X0 + (X1 - X0) * (k + 0.5) / n, Y1 - 0.1, Z0)
            self.cv.paint(MaskPen(W, H).rect(p[0] - 2, p[1], p[0] + 2, p[1]).a > 0, rgb('#e8c070'))
        m |= self.box(X0 - 0.02, X1 + 0.02, Y1 - 0.03, Y1, Z0 - 0.02, Z1, top, top=top, edge='#fff4e0')
        return m

    def jar_row(self, X0, X1, Y, Z, colors=('#c8a060', '#a85a3a', '#5a8a6a', '#e8e0d0')):
        x = X0
        k = 0
        while x < X1 - 0.06:
            c = rgb(colors[k % len(colors)])
            h = 0.06 + 0.04 * ((k * 7) % 3)
            self.box(x, x + 0.06, Y, Y + h, Z - 0.03, Z + 0.03, c, top=c * 0.7, edge=c * 1.3)
            x += 0.09
            k += 1

    def cup(self, X, Y, Z, color='#e8e0d0'):
        self.box(X - 0.025, X + 0.025, Y, Y + 0.06, Z - 0.025, Z + 0.025, color, top=rgb(color) * 0.6, edge=rgb(color) * 1.1)

    def books(self, X, Y, Z, n=3):
        cols = ['#2a4a7a', '#8a2a2e', '#c08a3a', '#3a6a4a']
        for k in range(n):
            c = rgb(cols[k % 4])
            self.box(X - 0.1 + k * 0.01, X + 0.08 - k * 0.01, Y + k * 0.03, Y + k * 0.03 + 0.03, Z - 0.06, Z + 0.06,
                     c, top=c * 1.15, edge=c * 1.4)

    def floor_lamp(self, X, Z, h=0.95, shade='#f0b060'):
        self.box(X - 0.015, X + 0.015, 0.0, h, Z - 0.015, Z + 0.015, '#2a1e18')
        lp = [self.room.proj(X - 0.1, h, Z), self.room.proj(X + 0.1, h, Z),
              self.room.proj(X + 0.06, h + 0.16, Z), self.room.proj(X - 0.06, h + 0.16, Z)]
        self.cv.paint(MaskPen(W, H).poly(lp).a > 0, rgb(shade))
        self.cv.paint(MaskPen(W, H).line([lp[0], lp[1]]).a > 0, rgb('#fff0c0'))
        c = self.room.proj(X, h + 0.05, Z)
        self.cv.add(glow(W, H, [(c[0], c[1] + 6, 1.0)], 16, '#ff9a40', 0.3, steps=3))
        self.cv.add(glow(W, H, [(c[0], c[1] + 3, 1.0)], 5, '#ffd090', 0.3, steps=2))
        xs = [p[0] for p in lp]
        return (min(xs) - 2, lp[2][1] - 2, max(xs) + 2, self.room.proj(X, 0.0, Z)[1])

    def candle(self, X, Y, Z):
        p = self.room.proj(X, Y, Z)
        self.cv.paint(MaskPen(W, H).rect(p[0] - 1, p[1] - 4, p[0] + 1, p[1]).a > 0, rgb('#f0e8d0'))
        self.cv.c[int(p[1] - 6), int(p[0])] = rgb('#fff0a0')
        self.cv.add(glow(W, H, [(p[0], p[1] - 6, 1.0)], 6, '#ffa040', 0.4, steps=3))

    def wall_side(self, side, Z0, Z1, Y0, Y1, color):
        """Rechteck auf der linken (-1) oder rechten (+1) Wand; gibt die Maske zurueck."""
        x = self.room.half_w * side
        return self.room.quad([(x, Y0, Z0), (x, Y1, Z0), (x, Y1, Z1), (x, Y0, Z1)], color)

    def screen_glow(self, mask, color='#7ab0d0'):
        if mask.any():
            ys, xs = np.nonzero(mask)
            self.cv.add(glow(W, H, [(xs.mean(), ys.mean(), 1.0)], 18, color, 0.18, steps=3))

    def bbox(self, mask, pad=2):
        ys, xs = np.nonzero(mask)
        return (float(xs.min() - pad), float(ys.min() - pad), float(xs.max() + pad), float(ys.max() + pad))

    def stand(self, X, Z):
        """Standpunkt auf dem Boden vor (X, Z), in die Gehflaeche geklemmt."""
        sx, sy = self.room.proj(X, 0.0, Z)
        w = self.meta['walk']
        sy = min(max(sy, w['farY']), w['nearY'])
        d = (sy - w['farY']) / (w['nearY'] - w['farY'])
        left = w['farLeft'] + (w['nearLeft'] - w['farLeft']) * d
        right = w['farRight'] + (w['nearRight'] - w['farRight']) * d
        return float(min(max(sx, left + 4), right - 4)), float(sy)

    # ------------------------------------------------------------------ Spielangaben
    def walk(self, **k):
        self.meta['walk'].update(k)

    def proj_box(self, X0, X1, Y0, Y1, Z0, Z1, pad=1):
        pts = [self.room.proj(x, y, z) for x in (X0, X1) for y in (Y0, Y1) for z in (Z0, Z1)]
        return (min(p[0] for p in pts) - pad, min(p[1] for p in pts) - pad,
                max(p[0] for p in pts) + pad, max(p[1] for p in pts) + pad)

    def spot(self, station, X0, X1, Y0, Y1, Z0, Z1, box=None):
        pts = [self.room.proj(x, y, z) for x in (X0, X1) for y in (Y0, Y1) for z in (Z0, Z1)]
        xs, ys = [p[0] for p in pts], [p[1] for p in pts]
        b = box or (min(xs), min(ys), max(xs), max(ys))
        sx, sy = self.room.proj((X0 + X1) / 2, 0.0, Z0 - 0.1)
        w = self.meta['walk']
        sy = min(max(sy, w['farY']), w['nearY'])
        self.meta['spots'].append(dict(station=station, box=[float(v) for v in b], standX=float(sx), standY=float(sy)))

    def spot_box(self, station, box, standX, standY):
        self.meta['spots'].append(dict(station=station, box=[float(v) for v in box], standX=float(standX), standY=float(standY)))

    def finish(self, path, colors=150):
        vig = np.hypot((self.xx - W / 2) / (W * 0.6), (self.yy - H * 0.5) / (H * 0.7))
        self.cv.c *= np.clip(1.1 - vig ** 2 * 0.5, 0.5, 1.15)[..., None]
        self.cv.c = quantize(self.cv.c, colors)
        self.cv.save(path)
        return self.meta
