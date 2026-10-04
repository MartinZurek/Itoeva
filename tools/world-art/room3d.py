"""Ein Zimmer in echter Zentralperspektive: Ebenen per Pixel zurueckgerechnet, Moebel als Quader.

Kamera bei X = 0, Augenhoehe CAM_H, Blick entlang +Z. Boden Y = 0.
"""
import numpy as np
from px import rgb, lerp, MaskPen


class Room:
    def __init__(self, cv, f=220.0, vp=(240, 105), cam_h=0.6, half_w=1.0, height=1.4, back=2.6):
        self.cv, self.f, self.vp, self.cam_h = cv, f, vp, cam_h
        self.half_w, self.height, self.back = half_w, height, back
        self.lights = []          # (pos, farbe, staerke)
        self.ambient = rgb('#2a1e22')

    # ------------------------------------------------------------ Projektion
    def proj(self, X, Y, Z):
        return (self.vp[0] + X * self.f / Z, self.vp[1] + (self.cam_h - Y) * self.f / Z)

    def light_at(self, P, N):
        """Beleuchtung eines Punktes P (..., 3) mit Normale N (3,)."""
        P = np.asarray(P, dtype=float)
        out = np.broadcast_to(self.ambient, P.shape).copy()
        for L, col, k in self.lights:
            d = np.asarray(L) - P
            dist2 = np.sum(d * d, axis=-1, keepdims=True)
            lam = np.clip(np.sum(d * np.asarray(N), axis=-1, keepdims=True) / np.sqrt(dist2 + 1e-9), 0, 1)
            lam = 0.35 + 0.65 * lam
            out = out + col * k * lam / (1 + dist2 * 3.0)
        return out

    def light_dithered(self, P, N, levels=14):
        """Wie [light_at], aber in Stufen mit geordnetem Raster - weiche Lichtkegel zerfielen
        beim Reduzieren auf die Palette sonst in Ringe."""
        from px import BAYER4
        L = self.light_at(P, N)
        h, w = L.shape[:2]
        by = np.tile(BAYER4, (h // 4 + 1, w // 4 + 1))[:h, :w][..., None]
        return np.floor(L * levels + 0.5 + by * 0.95) / levels

    # ------------------------------------------------------------ Raeumliche Koordinaten der Ebenen
    def planes(self):
        cv, f = self.cv, self.f
        x = cv.xx - self.vp[0]
        y = cv.yy - self.vp[1]
        with np.errstate(divide='ignore', invalid='ignore'):
            floor_z = np.where(y > 0, self.cam_h * f / y, np.inf)
            ceil_z = np.where(y < 0, (self.height - self.cam_h) * f / -y, np.inf)
            left_z = np.where(x < 0, self.half_w * f / -x, np.inf)
            right_z = np.where(x > 0, self.half_w * f / x, np.inf)
        back_z = np.full(x.shape, self.back)
        zs = np.stack([floor_z, ceil_z, left_z, right_z, back_z])
        which = np.argmin(zs, axis=0)
        Z = np.min(zs, axis=0)
        X = x * Z / f
        Y = self.cam_h - y * Z / f
        return which, X, Y, Z

    # ------------------------------------------------------------ Formen
    def quad(self, pts, color, mask_only=False):
        pen = MaskPen(self.cv.w, self.cv.h)
        pen.poly([self.proj(*p) for p in pts])
        m = pen.a > 0
        if not mask_only:
            self.cv.paint(m, color)
        return m

    def lit(self, albedo, center, normal):
        return np.clip(rgb(albedo) if isinstance(albedo, str) else albedo, 0, 1) * self.light_at(center, normal)

    def box(self, x0, x1, y0, y1, z0, z1, albedo, top=None, side=None, edge=None):
        """Quader: zeichnet die Flaechen, die die Kamera sieht. Gibt die Gesamtmaske zurueck."""
        al = rgb(albedo) if isinstance(albedo, str) else albedo
        tp = rgb(top) if isinstance(top, str) else (al if top is None else top)
        sd = rgb(side) if isinstance(side, str) else (al if side is None else side)
        total = np.zeros((self.cv.h, self.cv.w), bool)
        cx, cy, cz = (x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2
        if x0 > 0:   # linke Seite sichtbar
            total |= self.quad([(x0, y0, z0), (x0, y1, z0), (x0, y1, z1), (x0, y0, z1)],
                               self.lit(sd * 0.9, (x0, cy, cz), (-1, 0, 0)))
        if x1 < 0:   # rechte Seite sichtbar
            total |= self.quad([(x1, y0, z0), (x1, y1, z0), (x1, y1, z1), (x1, y0, z1)],
                               self.lit(sd * 0.9, (x1, cy, cz), (1, 0, 0)))
        if y1 < self.cam_h:   # Deckel sichtbar
            total |= self.quad([(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
                               self.lit(tp * 1.05, (cx, y1, cz), (0, 1, 0)))
        if y0 > self.cam_h:   # Unterseite sichtbar
            total |= self.quad([(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)],
                               self.lit(sd * 0.7, (cx, y0, cz), (0, -1, 0)))
        front = self.quad([(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0)],
                          self.lit(al, (cx, cy, z0), (0, 0, -1)))
        total |= front
        if edge is not None:
            # Lichtkante oben an der Vorderseite
            a = self.proj(x0, y1, z0)
            b = self.proj(x1, y1, z0)
            pen = MaskPen(self.cv.w, self.cv.h).line([a, b])
            self.cv.paint(pen.a > 0, rgb(edge) if isinstance(edge, str) else edge)
        return total
