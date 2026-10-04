"""Werkzeug, um aus den Concept-Art-Studien Spielorte zu machen: Ausschnitte laden, Figuren aus
dem Bild nehmen (die Wesen laufen im Spiel selbst herum) und die Luecken mit Gegenstaenden aus
den Objektspalten der Studienblaetter fuellen - im selben Zeichenstil.

Die Studien liegen im Branch art/concept-studies-2026-10-04 unter docs/concept-art/world-studies/.
Hier werden sie ueber `git show` gelesen, damit kein zweites Exemplar im Repository liegt.
"""
import io
import subprocess
import numpy as np
from PIL import Image
from scipy import ndimage

REF = 'origin/art/concept-studies-2026-10-04'
STUDIES = {
    'home': 'docs/concept-art/world-studies/cozy-home-world-study-v1.png',
    'sheet': 'docs/concept-art/world-studies/itoeva-park-lake-home-concept-v1.png',
    'park': 'docs/concept-art/world-studies/park-sports-ground-world-study-v1.png',
    'lake': 'docs/concept-art/world-studies/woodland-lake-world-study-v1.png',
    'river': 'docs/concept-art/world-studies/itoeva-riverside-quarter-concept-v1.png',
}
PAPER = np.array([248, 237, 221]) / 255

# Objekte aus der HOME-Spalte des Blatts "Places to live" (Koordinaten im Blatt, 1536 x 1024)
OBJECTS = {
    'sofa': (1102, 732, 1207, 800),
    'teatable': (1214, 733, 1282, 800),
    'bookcase': (1290, 742, 1354, 800),
    'bed': (1364, 728, 1512, 808),
    'plant': (1110, 802, 1154, 870),
    'chair': (1165, 808, 1206, 870),
    'piano': (1229, 803, 1322, 872),
    'rug': (1334, 815, 1448, 862),
    'hanging_plant': (1452, 802, 1510, 868),
    'lamp': (1385, 692, 1430, 740),
}


def study(name):
    data = subprocess.run(['git', 'show', f'{REF}:{STUDIES[name]}'], capture_output=True, check=True).stdout
    return Image.open(io.BytesIO(data)).convert('RGB')


def crop(name, box, size):
    return np.asarray(study(name).crop(box).resize(size, Image.LANCZOS), dtype=float) / 255


def sprite(name, scale=1.0):
    """Ein Objekt aus der Objektspalte, freigestellt vom Papierton: (RGB, Maske)."""
    im = study('sheet').crop(OBJECTS[name])
    if scale != 1.0:
        im = im.resize((max(1, int(im.width * scale)), max(1, int(im.height * scale))), Image.LANCZOS)
    rgb = np.asarray(im, dtype=float) / 255
    d = np.abs(rgb - PAPER).sum(-1)
    m = d > 0.16
    m = ndimage.binary_opening(m, np.ones((2, 2)))
    m = ndimage.binary_fill_holes(m)
    lab, n = ndimage.label(m)
    if n:
        sizes = ndimage.sum(m, lab, range(1, n + 1))
        m = np.isin(lab, 1 + np.flatnonzero(sizes >= max(8, sizes.max() * 0.04)))
    return rgb, m


def paste(img, spr, x, y_bottom, shadow=0.35, tint=(1.0, 1.0, 1.0), clip=None):
    """Objekt mit seinem Fusspunkt (Mitte unten) bei (x, y_bottom) einsetzen, mit Kontaktschatten."""
    rgb, m = spr
    h, w = m.shape
    x0, y0 = int(round(x - w / 2)), int(round(y_bottom - h))
    H, W = img.shape[:2]
    if shadow:
        yy, xx = np.mgrid[0:H, 0:W]
        sh = ((xx - x - w * 0.08) / (w * 0.55)) ** 2 + ((yy - y_bottom) / 2.5) ** 2 < 1
        img[sh] *= (1 - shadow)
    for j in range(h):
        for i in range(w):
            if m[j, i] and 0 <= y0 + j < H and 0 <= x0 + i < W and (clip is None or clip[y0 + j, x0 + i]):
                img[y0 + j, x0 + i] = np.clip(rgb[j, i] * np.asarray(tint), 0, 1)
    return img


def inpaint(img, mask, iters=300):
    """Luecken weich aus dem Rand fuellen (Diffusion) - fuer kleine Reste."""
    out = img.copy()
    out[mask] = 0
    known = ~mask
    for _ in range(iters):
        kf = known.astype(float)
        blur = ndimage.uniform_filter(out * kf[..., None], size=(3, 3, 1))
        wsum = ndimage.uniform_filter(kf, 3)
        est = blur / np.maximum(wsum[..., None], 1e-6)
        fill = mask & ~known & (wsum > 0.05)
        out = np.where(fill[..., None], est, out)
        known = known | fill
        if known.all():
            break
    # danach glaetten, nur innerhalb der Luecke
    for _ in range(20):
        sm = ndimage.uniform_filter(out, size=(3, 3, 1))
        out = np.where(mask[..., None], sm, out)
    return out


def hue_mask(img, box, hues, sat=0.25, val=0.15):
    """Pixel im Rechteck, deren Farbton in einem der Bereiche (Grad) liegt."""
    x0, y0, x1, y1 = box
    sub = img[y0:y1, x0:x1]
    mx, mn = sub.max(-1), sub.min(-1)
    s = (mx - mn) / np.maximum(mx, 1e-6)
    r, g, b = sub[..., 0], sub[..., 1], sub[..., 2]
    d = np.maximum(mx - mn, 1e-6)
    h = np.where(mx == r, ((g - b) / d) % 6, np.where(mx == g, (b - r) / d + 2, (r - g) / d + 4)) * 60
    m = np.zeros(sub.shape[:2], bool)
    for a, b_ in hues:
        m |= (h >= a) & (h <= b_)
    m &= (s >= sat) & (mx >= val)
    full = np.zeros(img.shape[:2], bool)
    full[y0:y1, x0:x1] = m
    return full
