"""Werkzeugkasten fuer die Pixel-Welt von Itoeva 2: Leinwand, Formen, Licht, Rauschen, Palette.

Alles rechnet in Fliesskomma-RGB (0..1) auf einer kleinen Leinwand; erst ganz am Ende wird auf eine
begrenzte Palette reduziert - so entsteht Pixel-Art mit sauberen Farbflaechen statt Matsch.
"""
import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage

BAYER4 = np.array([[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]) / 16 - 0.5


def rgb(h):
    h = h.lstrip('#')
    return np.array([int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)])


def lerp(a, b, t):
    return a + (b - a) * t


def ramp(stops, t):
    """Farbverlauf ueber Stuetzstellen [(pos, farbe)], t kann ein Array sein."""
    t = np.asarray(t, dtype=float)
    pos = np.array([p for p, _ in stops])
    cols = np.array([rgb(c) if isinstance(c, str) else c for _, c in stops])
    out = np.zeros(t.shape + (3,))
    for k in range(3):
        out[..., k] = np.interp(t, pos, cols[:, k])
    return out


class Canvas:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.c = np.zeros((h, w, 3))
        self.yy, self.xx = np.mgrid[0:h, 0:w]

    def paint(self, mask, color, alpha=1.0):
        m = np.clip(np.asarray(mask, dtype=float) * alpha, 0, 1)
        col = color if np.ndim(color) == 3 else np.broadcast_to(color, self.c.shape)
        self.c = self.c * (1 - m[..., None]) + col * m[..., None]

    def add(self, light):
        self.c = self.c + light

    def mask(self):
        return MaskPen(self.w, self.h)

    def save(self, path, scale=1):
        im = Image.fromarray((np.clip(self.c, 0, 1) * 255).astype(np.uint8))
        if scale != 1:
            im = im.resize((self.w * scale, self.h * scale), Image.NEAREST)
        im.save(path)


class MaskPen:
    """Zeichnet harte Masken (Pixel-Kanten) mit PIL."""

    def __init__(self, w, h):
        self.im = Image.new('L', (w, h), 0)
        self.d = ImageDraw.Draw(self.im)

    def poly(self, pts, v=255):
        self.d.polygon([(float(x), float(y)) for x, y in pts], fill=v)
        return self

    def rect(self, x0, y0, x1, y1, v=255):
        self.d.rectangle([x0, y0, x1, y1], fill=v)
        return self

    def ellipse(self, x0, y0, x1, y1, v=255):
        self.d.ellipse([x0, y0, x1, y1], fill=v)
        return self

    def line(self, pts, width=1, v=255):
        self.d.line([(float(x), float(y)) for x, y in pts], fill=v, width=width)
        return self

    def point(self, x, y, v=255):
        self.d.point((x, y), fill=v)
        return self

    @property
    def a(self):
        return np.asarray(self.im, dtype=float) / 255


def noise(w, h, scale, seed, octaves=4, persistence=0.5):
    """Fraktales Wertrauschen 0..1, glatt interpoliert."""
    rng = np.random.default_rng(seed)
    out = np.zeros((h, w))
    amp, total = 1.0, 0.0
    s = scale
    for _ in range(octaves):
        gw, gh = max(2, int(w / s) + 3), max(2, int(h / s) + 3)
        g = rng.random((gh, gw))
        z = ndimage.zoom(g, (s, s), order=3)[:h, :w]
        if z.shape != (h, w):
            z = np.pad(z, ((0, h - z.shape[0]), (0, w - z.shape[1])), mode='edge')
        out += z * amp
        total += amp
        amp *= persistence
        s = max(1, s / 2)
    out /= total
    return np.clip((out - out.min()) / (np.ptp(out) + 1e-9), 0, 1)


def glow(w, h, points, radius, color, strength=1.0, steps=4):
    """Lichthof um Punkte, additiv - in wenigen Stufen mit geordnetem Raster an den Uebergaengen,
    wie ein Pixel-Artist ihn setzt (weiche Verlaeufe zerfielen beim Reduzieren in Ringe)."""
    m = np.zeros((h, w))
    for x, y, s in points:
        xi, yi = int(round(x)), int(round(y))
        if 0 <= xi < w and 0 <= yi < h:
            m[yi, xi] += s
    m = ndimage.gaussian_filter(m, radius) * (2 * np.pi * radius * radius)
    m = np.clip(m, 0, 1.5)
    if steps:
        by = np.tile(BAYER4, (h // 4 + 1, w // 4 + 1))[:h, :w]
        m = np.floor(m * steps + 0.5 + by * 0.9) / steps
        m = np.clip(m, 0, 1.5)
    col = rgb(color) if isinstance(color, str) else color
    return m[..., None] * col * strength




def dither_bands(t, levels, h, w, amount=1.0):
    """Quantisiert einen Verlauf t (0..1) in [levels] Stufen mit geordnetem Raster an den Kanten."""
    by = np.tile(BAYER4, (h // 4 + 1, w // 4 + 1))[:h, :w]
    q = np.floor(t * (levels - 1) + 0.5 + by * amount)
    return np.clip(q / (levels - 1), 0, 1)


def quantize(c, colors=64, keep=None):
    """Reduziert auf eine Palette (Median-Schnitt, ohne Fehlerstreuung) - der Pixel-Art-Schritt."""
    im = Image.fromarray((np.clip(c, 0, 1) * 255).astype(np.uint8))
    q = im.quantize(colors=colors, method=Image.Quantize.MAXCOVERAGE, dither=Image.Dither.NONE)
    return np.asarray(q.convert('RGB'), dtype=float) / 255


def dramp(stops, t, levels=16):
    """Wie [ramp], aber in [levels] Stufen mit geordnetem Raster an den Uebergaengen."""
    t = np.asarray(t, dtype=float)
    h, w = t.shape
    by = np.tile(BAYER4, (h // 4 + 1, w // 4 + 1))[:h, :w]
    q = np.clip(np.floor(t * (levels - 1) + 0.5 + by), 0, levels - 1) / (levels - 1)
    return ramp(stops, q)
