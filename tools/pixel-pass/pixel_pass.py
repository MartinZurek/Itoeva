"""HD-2D-Pixelpass fuer die gemalten Figuren-Atlanten (Octopath/FF-Resonance-Stil).

Jede Atlasdatei bleibt in Groesse und Raster unveraendert: Die Figur wird auf eine
feste Pixeldichte verkleinert, auf eine gemeinsame Palette je Figur reduziert,
von Einzelpixeln bereinigt, mit einer selektiven dunklen Kontur versehen und
anschliessend blockweise (naechster Nachbar) auf die Originalgroesse gebracht.
Alle Code-Metriken (Zellen, Alphakonturen, Anker) bleiben damit gueltig.

Aufruf: python3 tools/pixel-pass/pixel_pass.py <atlas-ordner> [--art-height 56]
"""
import argparse, glob, json, os
import numpy as np, cv2
from PIL import Image
from scipy import ndimage

SPECIES = {'mosslyn': 1.02, 'nocti': 0.88, 'starlet': 0.90, 'wyrmling': 1.19}
# Je Figur: welche Dateien, und deren typische Figurenhoehe im Atlas (gemessen).
NCOL = 44


def figure_height(alpha):
    lab, n = ndimage.label(alpha > 40)
    hs = sorted(s[0].stop - s[0].start for s in ndimage.find_objects(lab) if s[0].stop - s[0].start > 60)
    return hs[len(hs) // 2]


def downscale(rgba, w, h):
    a = rgba[..., 3:4].astype(np.float32) / 255
    c = rgba[..., :3].astype(np.float32) / 255
    pm = cv2.resize(c * a, (w, h), interpolation=cv2.INTER_AREA)
    aa = cv2.resize(a[..., 0], (w, h), interpolation=cv2.INTER_AREA)
    col = pm / np.maximum(aa[..., None], 1e-4)
    blur = cv2.GaussianBlur(col, (0, 0), 0.8)
    col = np.clip(col + 0.5 * (col - blur), 0, 1)
    return (col * 255).astype(np.uint8), aa > 0.5


def clean(idx, cen):
    H, W = idx.shape
    for _ in range(2):
        new = idx.copy()
        for y in range(1, H - 1):
            for x in range(1, W - 1):
                if idx[y, x] < 0:
                    continue
                nb = [idx[y - 1, x], idx[y + 1, x], idx[y, x - 1], idx[y, x + 1]]
                if idx[y, x] not in nb:
                    v = [q for q in nb if q >= 0]
                    if len(v) >= 3:
                        best = max(set(v), key=v.count)
                        if np.abs(cen[best] - cen[idx[y, x]]).sum() < 55:
                            new[y, x] = best
        idx = new
    return idx


def process_species(folder, sp, art_height, report):
    files = sorted(f for f in glob.glob(os.path.join(folder, sp + '*.png')))
    target = art_height * SPECIES[sp] / SPECIES['mosslyn']
    small = []
    for f in files:
        rgba = np.array(Image.open(f).convert('RGBA'))
        H, W = rgba.shape[:2]
        s = target / figure_height(rgba[..., 3])
        w, h = max(1, round(W * s)), max(1, round(H * s))
        col, m = downscale(rgba, w, h)
        small.append((f, rgba, col, m, s))
    pts = np.concatenate([cv2.cvtColor(c, cv2.COLOR_RGB2LAB)[m] for _, _, c, m, _ in small]).astype(np.float32)
    if len(pts) > 400000:
        pts = pts[np.random.default_rng(0).choice(len(pts), 400000, replace=False)]
    crit = (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 60, 0.2)
    _, _, cen = cv2.kmeans(pts, NCOL, None, crit, 4, cv2.KMEANS_PP_CENTERS)
    pal = cv2.cvtColor(cen.reshape(1, -1, 3).astype(np.uint8), cv2.COLOR_LAB2RGB).reshape(-1, 3)
    for f, rgba, col, m, s in small:
        lab = cv2.cvtColor(col, cv2.COLOR_RGB2LAB).astype(np.float32)
        idx = np.full(m.shape, -1)
        d = ((lab[m][:, None, :] - cen[None]) ** 2).sum(-1)
        idx[m] = d.argmin(-1)
        idx = clean(idx, cen)
        img = np.zeros(idx.shape + (4,), np.uint8)
        img[idx >= 0, :3] = pal[idx[idx >= 0]]
        img[idx >= 0, 3] = 255
        a = idx >= 0
        edge = a & ~ndimage.binary_erosion(a)
        img[edge, :3] = (img[edge, :3].astype(float) * 0.45).astype(np.uint8)
        H, W = rgba.shape[:2]
        big = np.array(Image.fromarray(img).resize((W, H), Image.NEAREST))
        Image.fromarray(big).save(f, optimize=True)
        report[os.path.basename(f)] = {'scale': round(s, 4), 'art_size': list(img.shape[1::-1])}


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('folder')
    ap.add_argument('--art-height', type=float, default=56.0)
    ap.add_argument('--species', nargs='*', default=list(SPECIES))
    args = ap.parse_args()
    report = {'art_height_mosslyn': args.art_height, 'palette_colors': NCOL, 'files': {}}
    for sp in args.species:
        process_species(args.folder, sp, args.art_height, report['files'])
        print('ok', sp)
    json.dump(report, open(os.path.join(args.folder, 'pixel_pass.json'), 'w'), indent=1)
