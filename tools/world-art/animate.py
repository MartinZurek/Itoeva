"""Bewegung fuer die gemalten Orte: aus dem fertigen Bild einen Streifen aus [FRAMES] Bildern
machen, die das Spiel nacheinander zeigt (`<ort>_anim.png`, Bilder nebeneinander).

Erkannt wird aus den Farben des Bildes selbst, ergaenzt um Angaben je Ort:
- **Laub und Gras** (gruene und gelbgruene Toene): wiegt sich im Wind. Jeder Pixel wird um
  hoechstens einen Pixel seitlich versetzt; die Phase wandert langsam ueber das Bild, so dass
  eine Boe durch Kronen und Halme laeuft. Oben (Kronen) staerker als unten.
- **Wasser** (Flaechen aus `water`): Zeilen schwingen um einen Pixel, helle Glanzpunkte wandern.
- **Lichter** (warme, sehr helle Flecken: Laternen, Fenster, Feuer, Kerzen): flackern in der
  Helligkeit, jeder Fleck mit eigener Phase; Feuer staerker.

Alles mit ganzen Pixeln und den Farben des Bildes - keine weichen Verlaeufe, die die Pixel-Art
verschmieren wuerden.
"""
import numpy as np
from PIL import Image
from scipy import ndimage

FRAMES = 8


def hsv(img):
    mx, mn = img.max(-1), img.min(-1)
    d = np.maximum(mx - mn, 1e-6)
    r, g, b = img[..., 0], img[..., 1], img[..., 2]
    h = np.where(mx == r, ((g - b) / d) % 6, np.where(mx == g, (b - r) / d + 2, (r - g) / d + 4)) * 60
    s = (mx - mn) / np.maximum(mx, 1e-6)
    return h, s, mx


def foliage_mask(img, exclude=()):
    h, s, v = hsv(img)
    m = (h >= 55) & (h <= 165) & (s >= 0.18) & (v >= 0.12)
    m = ndimage.binary_opening(m, np.ones((2, 2)))
    for x0, y0, x1, y1 in exclude:
        m[int(y0):int(y1), int(x0):int(x1)] = False
    return m


def light_mask(img, warm_min=0.82):
    h, s, v = hsv(img)
    m = (v >= warm_min) & (s >= 0.25) & (h >= 15) & (h <= 60)
    m = ndimage.binary_dilation(m, iterations=1)
    lab, n = ndimage.label(m)
    if n:
        sizes = ndimage.sum(m, lab, range(1, n + 1))
        # Grosse helle Flaechen sind Himmel oder Sand, keine Lampen.
        keep = (sizes >= 3) & (sizes <= 600)
        # Eine Lampe leuchtet heller als ihre Umgebung; sonnige Wandstuecke und Abendwolken nicht.
        sl = ndimage.find_objects(lab)
        for i in np.flatnonzero(keep):
            ys, xs = sl[i]
            y0, y1 = max(0, ys.start - 6), min(v.shape[0], ys.stop + 6)
            x0, x1 = max(0, xs.start - 6), min(v.shape[1], xs.stop + 6)
            blob = lab[y0:y1, x0:x1] == i + 1
            ring = ndimage.binary_dilation(blob, iterations=5) & ~ndimage.binary_dilation(blob, iterations=1)
            if not ring.any() or v[y0:y1, x0:x1][blob].mean() - v[y0:y1, x0:x1][ring].mean() < 0.16:
                keep[i] = False
        m = np.isin(lab, 1 + np.flatnonzero(keep))
        lab, n = ndimage.label(m)
    return lab, n


def poly_mask(shape, polys):
    from PIL import ImageDraw
    im = Image.new('L', (shape[1], shape[0]), 0)
    d = ImageDraw.Draw(im)
    for p in polys:
        d.polygon([tuple(q) for q in p], fill=255)
    return np.asarray(im) > 0


def frames(img, water=(), exclude=(), sway=1.0, fire=(), seed=1, water_band=None, water_any=False, glow_add=(), glow_exclude=()):
    """img: Fliesskomma-RGB (H, W, 3). Gibt eine Liste aus FRAMES Bildern zurueck."""
    H, W = img.shape[:2]
    yy, xx = np.mgrid[0:H, 0:W]
    leaf = foliage_mask(img, exclude)
    wat = poly_mask((H, W), water) if water else np.zeros((H, W), bool)
    if water_band:
        # Wasser aus der Farbe: blaue/tuerkise Toene zwischen zwei Bildzeilen (darueber Himmel)
        hh, ss, vv = hsv(img)
        y0, y1 = water_band
        if water_any:
            # Spiegelndes Abendwasser hat die Farben des Himmels: alles ausser Holz und Laub
            wood = (hh >= 10) & (hh <= 45) & (ss >= 0.35) & (vv < 0.55)
            wat |= ~wood & ~foliage_mask(img) & (yy >= y0) & (yy < y1)
        else:
            wat |= (hh >= 165) & (hh <= 245) & (ss >= 0.12) & (yy >= y0) & (yy < y1)
        wat = ndimage.binary_opening(ndimage.binary_closing(wat, np.ones((3, 3))), np.ones((2, 2))) & (yy >= y0) & (yy < y1)
    leaf &= ~wat
    lab, n = light_mask(img)
    firem = poly_mask((H, W), fire) if fire else np.zeros((H, W), bool)
    rng = np.random.default_rng(seed)
    blob_phase = rng.uniform(0, 2 * np.pi, n + 1)
    # Wie stark sich etwas bewegt: Kronen oben mehr als Gras am Boden, Rauschen bricht Gleichtakt.
    reach = np.clip(1.15 - yy / H * 0.5, 0.5, 1.0) * sway
    jitter = ndimage.gaussian_filter(rng.random((H, W)), 3) * 6
    # Glitzerpunkte: wenige, zufaellig verteilte helle Wasserpixel, jeder mit eigener Phase
    seeds_g = np.zeros((H, W))
    if wat.any():
        vv0 = hsv(img)[2]
        cand = wat & (vv0 > np.percentile(vv0[wat], 55)) & (rng.random((H, W)) < 0.012)
        seeds_g[cand] = rng.uniform(0.01, 2 * np.pi, cand.sum())
    out = []
    for k in range(FRAMES):
        ph = 2 * np.pi * k / FRAMES
        f = img.copy()
        # Laub: Boe laeuft von links nach rechts durchs Bild
        wave = np.sin(ph - xx * 0.035 + yy * 0.02 + jitter)
        dx = np.round(wave * reach * 1.0).astype(int)
        sx = np.clip(xx - dx, 0, W - 1)
        take = leaf & leaf[yy, sx]
        f[take] = img[yy, sx][take]
        # Blattspitzen blitzen: hellste Laubpixel heller, wenn die Boe sie trifft
        h, s, v = hsv(img)
        tip = leaf & (v > np.percentile(v[leaf], 85) if leaf.any() else False) & (wave > 0.8)
        f[tip] = np.clip(f[tip] * 1.08, 0, 1)
        # Wasser: Zeilen schwingen, Glanz wandert
        if wat.any():
            rdx = np.round(np.sin(ph * 2 + yy * 0.9) * 1.0).astype(int)
            wx = np.clip(xx - rdx, 0, W - 1)
            ok = wat & wat[yy, wx]
            f[ok] = img[yy, wx][ok]
            glint = wat & (seeds_g > 0) & (np.sin(ph * 2 + seeds_g) > 0.75)
            f[glint] = np.clip(f[glint] + 0.2, 0, 1)
        # Lichter flackern, jeder Fleck eigen; Feuer kraeftiger
        if n:
            amp = np.where(firem, 0.16, 0.07)
            fl = 1 + amp * np.sin(ph * 3 + blob_phase[lab]) * (lab > 0)
            f = np.where((lab > 0)[..., None], np.clip(f * fl[..., None], 0, 1), f)
        if firem.any():
            amp2 = 1 + 0.1 * np.sin(ph * 4 + yy * 0.5)
            f[firem] = np.clip(f[firem] * amp2[firem][:, None], 0, 1)
        out.append(f)
    return out


def write_strip(frames_, path):
    strip = np.concatenate(frames_, axis=1)
    Image.fromarray((np.clip(strip, 0, 1) * 255).astype(np.uint8)).save(path, optimize=True)


def animate_file(png, out_strip, **kw):
    img = np.asarray(Image.open(png).convert('RGB'), dtype=float) / 255
    write_strip(frames(img, **kw), out_strip)


def glow_layer(img, fire=(), glow_add=(), glow_exclude=()):
    """Was nachts leuchtet: die hellen warmen Flecken (Laternen, Fenster, Feuer, Kerzen) in ihren
    eigenen Farben, dazu ein gestufter Lichthof. RGBA; das Spiel legt es ueber die nächtliche
    Abdunkelung, damit Lampen nicht mit dunkel werden."""
    H, W = img.shape[:2]
    lab, n = light_mask(img)
    m = lab > 0
    if fire:
        hh, ss, vv = hsv(img)
        m |= poly_mask((H, W), fire) & (vv > 0.6) & (hh >= 10) & (hh <= 60)
    vv = hsv(img)[2]
    for x0, y0, x1, y1 in glow_add:
        box = np.zeros((H, W), bool)
        box[y0:y1, x0:x1] = True
        m |= box & (vv > 0.62)
    for x0, y0, x1, y1 in glow_exclude:
        m[y0:y1, x0:x1] = False
    out = np.zeros((H, W, 4))
    if not m.any():
        return out
    halo = ndimage.gaussian_filter(m.astype(float), 4)
    halo = np.floor(np.clip(halo * 3, 0, 1) * 3) / 3 * 0.45      # drei Stufen, Pixel-Art
    warm = np.array([1.0, 0.72, 0.38])
    out[..., :3] = warm
    out[..., 3] = halo
    out[m, :3] = img[m]
    out[m, 3] = 1.0
    return out


def glow_file(png, out_path, fire=(), glow_add=(), glow_exclude=(), **_):
    img = np.asarray(Image.open(png).convert('RGB'), dtype=float) / 255
    g = glow_layer(img, fire, glow_add, glow_exclude)
    Image.fromarray((np.clip(g, 0, 1) * 255).astype(np.uint8), 'RGBA').save(out_path, optimize=True)
