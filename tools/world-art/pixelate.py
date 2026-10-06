"""Uebersetzt einen Ausschnitt einer Concept-Art-Studie in Pixel-Art im Spielformat (480 x 270).

Die Studien (docs/concept-art/world-studies/) sind Zeichnungen mit Farbstift und Aquarell. Ein
blosses Verkleinern ergaebe Matsch, deshalb in Schritten wie ein Pixel-Artist:
1. Ausschnitt auf Spielgroesse bringen (der Massstab der Studien passt fast 1:1).
2. Flaechen beruhigen (Kuwahara-Filter): Farbstiftrauschen weg, Kanten bleiben.
3. Bleistiftkonturen erkennen und als klare dunkle Pixellinien setzen.
4. Eigene Palette aus dem Bild (k-Means), harte Zuordnung ohne Fehlerstreuung.
5. Einzelpixel aufraeumen, Lichtkanten setzen.
"""
import numpy as np
from PIL import Image
from scipy import ndimage
from scipy.cluster.vq import kmeans2

W, H = 480, 270


def load_crop(path, box, size=(W, H)):
    im = Image.open(path).convert('RGB').crop(box)
    return np.asarray(im.resize(size, Image.LANCZOS), dtype=float) / 255


def kuwahara(img, r=2):
    """Flaechen beruhigen: je Pixel der Mittelwert des ruhigsten der vier Quadranten."""
    pad = np.pad(img, ((r, r), (r, r), (0, 0)), mode='edge')
    h, w = img.shape[:2]
    means, vars_ = [], []
    lum = pad.mean(-1)
    for dy in (0, r):
        for dx in (0, r):
            sl = pad[dy:dy + h + r, dx:dx + w + r]
            m = ndimage.uniform_filter(sl, size=(r + 1, r + 1, 1))[r // 2:r // 2 + h, r // 2:r // 2 + w]
            l = lum[dy:dy + h + r, dx:dx + w + r]
            v = ndimage.uniform_filter(l * l, r + 1) - ndimage.uniform_filter(l, r + 1) ** 2
            means.append(m)
            vars_.append(v[r // 2:r // 2 + h, r // 2:r // 2 + w])
    idx = np.argmin(np.stack(vars_), axis=0)
    out = np.choose(idx[..., None], means)
    return out


def to_lab(c):
    # einfache wahrnehmungsnahe Gewichtung fuer k-Means
    return c * np.array([0.9, 1.2, 0.7])


def palette(img, n=64, seed=1):
    px = img.reshape(-1, 3)
    rng = np.random.default_rng(seed)
    sample = px[rng.choice(len(px), min(len(px), 40000), replace=False)]
    cent, _ = kmeans2(to_lab(sample), n, minit='++', seed=seed, iter=25)
    return np.clip(cent / np.array([0.9, 1.2, 0.7]), 0, 1)


def quantize(img, pal):
    px = to_lab(img.reshape(-1, 3))
    pl = to_lab(pal)
    d = ((px[:, None, :] - pl[None, :, :]) ** 2).sum(-1)
    return pal[np.argmin(d, axis=1)].reshape(img.shape), np.argmin(d, axis=1).reshape(img.shape[:2])


def contours(src, strength=0.075):
    """Bleistiftlinien: deutlich dunkler als die Umgebung, schmal."""
    lum = src @ np.array([0.3, 0.55, 0.15])
    local = ndimage.median_filter(lum, 5)
    line = (local - lum) > strength
    line &= ~ndimage.binary_opening(line, np.ones((3, 3)))   # nur schmale Striche
    return line


def cleanup(idx, pal, keep=None, passes=1):
    """Einzelne Ausreisserpixel durch die Mehrheit der Nachbarn ersetzen."""
    h, w = idx.shape
    for _ in range(passes):
        pad = np.pad(idx, 1, mode='edge')
        neigh = np.stack([pad[1 + dy:1 + dy + h, 1 + dx:1 + dx + w]
                          for dy in (-1, 0, 1) for dx in (-1, 0, 1) if dy or dx])
        same = (neigh == idx[None]).sum(0)
        # haeufigster Nachbar
        maj = np.apply_along_axis(lambda v: np.bincount(v).argmax(), 0, neigh)
        lonely = same == 0
        if keep is not None:
            lonely &= ~keep
        idx = np.where(lonely, maj, idx)
    return idx


def translate(src, n_colors=72, line_k=0.55, smooth=2, seed=1):
    """Studie -> Pixel-Art. src: Fliesskomma-RGB in Spielgroesse."""
    flat = kuwahara(src, smooth) if smooth else src
    pal = palette(flat, n_colors, seed)
    q, idx = quantize(flat, pal)
    idx = cleanup(idx, pal)
    out = pal[idx]
    line = contours(src)
    out = np.where(line[..., None], out * line_k, out)
    return np.clip(out, 0, 1)


def save(arr, path, scale=1):
    im = Image.fromarray((np.clip(arr, 0, 1) * 255).astype(np.uint8))
    if scale != 1:
        im = im.resize((im.width * scale, im.height * scale), Image.NEAREST)
    im.save(path)
