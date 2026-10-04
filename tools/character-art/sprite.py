"""Werkzeugkasten fuer die Figuren von Itoeva in feiner Pixel-Art.

Eine Figur entsteht aus Teilen (Koerper, Kopf, Ohren ...), die von hinten nach vorn gezeichnet
werden. Jedes Teil bekommt seine Woelbung aus der eigenen Form: Aus dem Abstand zum Rand wird
eine Kuppel, aus deren Neigung die Lichtseite (Licht von oben links). Getoent wird in einer
Rampe weniger, farblich verschobener Toene (Schatten waermer/violetter, Licht gelblicher) -
so, wie ein Pixel-Artist Toene setzt, statt weich zu verlaufen.

Zum Schluss eine selektive Kontur: dunkle Fassung der Nachbarfarbe statt Schwarz.
"""
import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage

__all__ = ['Sprite', 'Pose', 'hexrgb']

BAYER4 = np.array([[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]) / 16 - 0.5


def hexrgb(h):
    h = h.lstrip('#')
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], dtype=float) / 255


class Pose:
    """Was eine Figur gerade tut: Augen offen/zu/froh, welcher Fuss oben ist (Laufphase 0..3,
    -1 = stehen), Freude (Arme, Fluegel, Ohren hoch)."""

    def __init__(self, eyes='open', step=-1, joy=False, sleep=False):
        self.eyes, self.step, self.joy, self.sleep = eyes, step, joy, sleep

    def foot_lift(self, i):
        """Wie weit Fuss [i] (0 = links, 1 = rechts) gerade gehoben ist."""
        if self.step == 1:
            return 2 if i == 0 else 0
        if self.step == 3:
            return 2 if i == 1 else 0
        return 0


class Sprite:
    def __init__(self, w=64, h=64, pose=None):
        self.pose = pose or Pose()
        self.w, self.h = w, h
        self.rgb = np.zeros((h, w, 3))
        self.a = np.zeros((h, w), bool)
        self.yy, self.xx = np.mgrid[0:h, 0:w]
        self.by = np.tile(BAYER4, (h // 4 + 1, w // 4 + 1))[:h, :w]

    # ---------------------------------------------------------------- Masken
    def pen(self):
        return Pen(self.w, self.h)

    def ellipse(self, cx, cy, rx, ry, rot=0.0):
        x, y = self.xx - cx + 0.5, self.yy - cy + 0.5
        if rot:
            c, s = np.cos(rot), np.sin(rot)
            x, y = x * c + y * s, -x * s + y * c
        return (x / rx) ** 2 + (y / ry) ** 2 <= 1.0

    def poly(self, pts):
        return self.pen().poly(pts).m

    # ---------------------------------------------------------------- Teile
    def part(self, mask, ramp, light=(-0.6, -0.75), fur=0.0, seed=0, line=True, flat=False,
             bulge=1.0, lift=0.0):
        """Ein Teil zeichnen. [ramp]: Toene dunkel -> hell. [fur] rauht die Tonwechsel auf
        (Fell, Moos), [lift] hebt den ganzen Teil heller (z. B. vordere Teile)."""
        mask = np.asarray(mask, bool)
        if not mask.any():
            return mask
        cols = [hexrgb(c) if isinstance(c, str) else c for c in ramp]
        k = len(cols)
        if flat:
            idx = np.full(mask.shape, k - 2 if k > 1 else 0)
        else:
            d = ndimage.distance_transform_edt(np.pad(mask, 1))[1:-1, 1:-1]
            hgt = np.sqrt(d) * bulge
            hgt = ndimage.gaussian_filter(hgt, 0.9)
            gy, gx = np.gradient(hgt)
            n = np.stack([-gx, -gy, np.full_like(hgt, 0.55)], -1)
            n /= np.linalg.norm(n, axis=-1, keepdims=True)
            L = np.array([light[0], light[1], 0.9])
            L /= np.linalg.norm(L)
            inten = np.clip((n * L).sum(-1), 0, 1)
            vals = inten[mask]
            lo, hi = np.percentile(vals, 3), np.percentile(vals, 97)
            t = np.clip((inten - lo) / max(hi - lo, 1e-6), 0, 1)
            t = np.clip(t + lift, 0, 1)
            if fur:
                rng = np.random.default_rng(seed)
                nz = rng.random(mask.shape)
                nz = ndimage.uniform_filter(nz, 2)
                t = t + (nz - 0.5) * fur
            idx = np.clip(np.floor(t * (k - 0.01) + self.by * 0.35), 0, k - 1).astype(int)
        col = np.array(cols)[idx]
        if line:
            # Wo dieses Teil vor einem schon gezeichneten liegt: eine dunkle Trennlinie an seinem
            # Rand - die liest sich als Ueberdeckung, als Tiefe.
            edge = mask & ~ndimage.binary_erosion(mask)
            over = edge & self.a
            dark = cols[0] * 0.75 + np.array([0.08, 0.02, 0.1]) * 0.25
            col[over] = dark
        self.rgb[mask] = col[mask]
        self.a |= mask
        return mask

    def paint(self, mask, color):
        mask = np.asarray(mask, bool)
        self.rgb[mask] = hexrgb(color) if isinstance(color, str) else color
        self.a |= mask

    def px(self, x, y, color):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.rgb[y, x] = hexrgb(color)
            self.a[y, x] = True

    def eye(self, x, y, w=4, h=5, iris='#2a1a14', shine='#ffffff', low=None):
        """Ein grosses, glaenzendes Auge wie in den Studien: abgerundetes Rechteck, oben links ein
        grosser Glanzpunkt, unten ein waermerer Iriston. Geschlossen (Schlaf, Blinzeln) ein
        nach unten gebogener Strich, froh ein nach oben gebogener."""
        mode = self.pose.eyes
        if mode != 'open':
            mid = y + h - 2
            for i in range(w):
                edge = i == 0 or i == w - 1
                if mode == 'closed':
                    yy = mid - 1 if edge else mid
                else:
                    yy = mid if edge else mid - 1
                self.px(x + i, yy, iris)
            return
        m = (self.xx >= x) & (self.xx < x + w) & (self.yy >= y) & (self.yy < y + h)
        corners = ((self.xx == x) | (self.xx == x + w - 1)) & ((self.yy == y) | (self.yy == y + h - 1))
        m &= ~corners
        self.paint(m, iris)
        if low:
            self.paint(m & (self.yy >= y + h - max(1, h // 3)) & (self.xx > x) & (self.xx < x + w - 1), low)
        g = 2 if w >= 4 and h >= 5 else 1
        self.paint((self.xx >= x + 1) & (self.xx < x + 1 + g) & (self.yy >= y + 1) & (self.yy < y + 1 + g), shine)
        if w >= 4 and h >= 5:
            self.px(x + w - 2, y + h - 2, shine)

    # ---------------------------------------------------------------- Abschluss
    def outline(self, strength=0.42, top_lighter=True):
        """Selektive Kontur: jede leere Nachbarzelle bekommt die dunkle Fassung der Farbe daneben.
        Oben links (zum Licht) etwas heller als unten rechts."""
        a = self.a
        out = np.zeros_like(a)
        src = np.zeros_like(self.rgb)
        for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            sh = np.roll(np.roll(a, dy, 0), dx, 1)
            sc = np.roll(np.roll(self.rgb, dy, 0), dx, 1)
            new = sh & ~a & ~out
            src[new] = sc[new]
            out |= new
        dark = src * strength + np.array([0.10, 0.04, 0.10]) * (1 - strength) * 0.6
        if top_lighter:
            lit = (self.yy < self.h * 0.5) & (self.xx < self.w * 0.5)
            dark[lit & out] = src[lit & out] * (strength + 0.12) + 0.03
        self.rgb[out] = dark[out]
        self.a |= out

    def image(self):
        arr = np.zeros((self.h, self.w, 4), np.uint8)
        arr[..., :3] = (np.clip(self.rgb, 0, 1) * 255).astype(np.uint8)
        arr[..., 3] = self.a * 255
        return Image.fromarray(arr, 'RGBA')


class Pen:
    def __init__(self, w, h):
        self.im = Image.new('L', (w, h), 0)
        self.d = ImageDraw.Draw(self.im)

    def poly(self, pts):
        self.d.polygon([(float(x), float(y)) for x, y in pts], fill=255)
        return self

    def line(self, pts, width=1):
        self.d.line([(float(x), float(y)) for x, y in pts], fill=255, width=width)
        return self

    @property
    def m(self):
        return np.asarray(self.im) > 0
