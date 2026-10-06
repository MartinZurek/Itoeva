"""Gemalte Figur als Puppe: Teile drehen sich um ihre Gelenke, die Uebergaenge bleiben weich.

Die Figur kommt unveraendert aus dem Key-Design (freigestellt, `source/<wesen>_key.png`) - Gesicht,
Mantel, Fell und Schwanz sind die gemalten. Bewegt wird sie wie eine Legefigur mit weichen
Gelenken: Jeder Bildpunkt gehoert mit Gewichten zu einem oder mehreren Teilen (Ohr, Kopf, Arm,
Mantelzipfel, Schwanz, Bein, Rumpf). Je Bild bekommt jedes Teil eine Drehung um sein Gelenk (plus
Verschiebung/Stauchung); ein Kindteil erbt die Bewegung seines Elternteils. Die Gewichte sind an
den Teilgrenzen weichgezeichnet, damit nichts reisst.

Danach wird verkleinert und in Pixel-Art uebersetzt (Palette aus der Figur, dunkle Aussenkontur).
"""
import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage


def affine(rot_deg=0.0, pivot=(0, 0), t=(0, 0), sx=1.0, sy=1.0):
    """3x3-Matrix: um pivot skalieren und drehen, dann um t verschieben."""
    a = np.radians(rot_deg)
    c, s = np.cos(a), np.sin(a)
    px, py = pivot
    T1 = np.array([[1, 0, -px], [0, 1, -py], [0, 0, 1]])
    S = np.diag([sx, sy, 1.0])
    R = np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])
    T2 = np.array([[1, 0, px + t[0]], [0, 1, py + t[1]], [0, 0, 1]])
    return T2 @ R @ S @ T1


class Part:
    def __init__(self, name, mask, pivot, parent=None, z=0):
        self.name, self.mask, self.pivot, self.parent, self.z = name, mask, np.asarray(pivot, float), parent, z


class Puppet:
    def __init__(self, rgba, soft=3.0):
        self.img = np.asarray(rgba, dtype=float) / 255
        self.H, self.W = self.img.shape[:2]
        self.parts = {}
        self.order = []
        self.soft = soft

    def poly(self, pts):
        im = Image.new('L', (self.W, self.H), 0)
        ImageDraw.Draw(im).polygon([tuple(p) for p in pts], fill=255)
        return np.asarray(im) > 0

    def add(self, name, mask, pivot, parent=None, z=0):
        self.parts[name] = Part(name, mask, pivot, parent, z)
        self.order.append(name)

    def finish(self, rest='torso'):
        """Gewichte: jedes Teil seine Maske (spaetere Teile gewinnen), Rest gehoert [rest];
        dann weich verblenden."""
        alpha = self.img[..., 3] > 0.5
        owner = np.full((self.H, self.W), self.order.index(rest))
        for i, n in enumerate(self.order):
            if n != rest:
                owner[self.parts[n].mask & alpha] = i
        self.owner = owner
        w = np.stack([(owner == i).astype(float) for i in range(len(self.order))])
        if self.soft:
            w = np.stack([ndimage.gaussian_filter(x, self.soft) for x in w])
        w /= np.maximum(w.sum(0), 1e-9)
        self.weights = w

    def world(self, local):
        """Gesamtmatrix je Teil aus den lokalen Matrizen (Eltern zuerst)."""
        out = {}

        def get(n):
            if n in out:
                return out[n]
            p = self.parts[n]
            M = local.get(n, np.eye(3))
            if p.parent:
                M = get(p.parent) @ M
            out[n] = M
            return M
        for n in self.order:
            get(n)
        return out

    def pose(self, local, size=None, offset=(0, 0), ss=3):
        """Rendert die Puppe mit lokalen Matrizen {teil: 3x3}. Vorwaerts-Abbildung, ss-fach
        ueberabgetastet, Ziel in Quellaufloesung (size = (W, H) des Ziels)."""
        Wd, Hd = size or (self.W, self.H)
        M = self.world(local)
        mats = np.stack([M[n] for n in self.order])          # (P, 3, 3)
        alpha = self.img[..., 3] > 0.5
        ys, xs = np.nonzero(alpha)
        out = np.zeros((Hd, Wd, 4))
        zbuf = np.full((Hd, Wd), -1e9)
        zs = np.array([self.parts[n].z for n in self.order], float)
        for dy in range(ss):
            for dx in range(ss):
                px = xs + (dx + 0.5) / ss - 0.5
                py = ys + (dy + 0.5) / ss - 0.5
                P = np.stack([px, py, np.ones_like(px)])        # (3, N)
                wts = self.weights[:, ys, xs]                    # (P, N)
                A = np.einsum('pn,pij->nij', wts, mats)          # (N, 3, 3)
                q = np.einsum('nij,jn->in', A, P)
                tx = np.round(q[0] + offset[0]).astype(int)
                ty = np.round(q[1] + offset[1]).astype(int)
                ok = (tx >= 0) & (tx < Wd) & (ty >= 0) & (ty < Hd)
                z = zs[self.owner[ys, xs]] + py * 1e-4
                sel = np.flatnonzero(ok)
                sel = sel[np.argsort(z[sel], kind='stable')]
                # Hintere zuerst schreiben, vordere ueberschreiben (bei Doppelbelegung gilt der letzte)
                cand_z = z[sel]
                Y, X = ty[sel], tx[sel]
                better = cand_z >= zbuf[Y, X]
                Y, X, sel = Y[better], X[better], sel[better]
                out[Y, X] = self.img[ys[sel], xs[sel]]
                zbuf[Y, X] = np.maximum(zbuf[Y, X], z[sel])
        # kleine Loecher (Dehnung) schliessen
        a = out[..., 3] > 0
        holes = ndimage.binary_fill_holes(ndimage.binary_closing(a, np.ones((5, 5)))) & ~a
        holes &= ndimage.binary_dilation(a, iterations=2)
        if holes.any():
            fill = np.stack([ndimage.grey_dilation(out[..., c] * a, size=(5, 5)) for c in range(3)], -1)
            out[holes, :3] = fill[holes]
            out[holes, 3] = 1
        return out


def pixelize(img, scale, palette, outline_col=(0.16, 0.08, 0.05)):
    """Verkleinern (flaechengemittelt), in die Palette, dunkle Aussenkontur. Gibt RGBA zurueck."""
    a = img[..., 3]
    H, W = a.shape
    rgb = img[..., :3]
    w, h = max(1, round(W * scale)), max(1, round(H * scale))

    def down(x):
        return np.asarray(Image.fromarray(np.float32(x), 'F').resize((w, h), Image.BOX), dtype=float)
    al = down(a)
    col = np.stack([down(rgb[..., c] * a) for c in range(3)], -1) / np.maximum(al[..., None], 1e-6)
    # Tusche erhalten: Wo im Block genug dunkle Linie liegt (Augen, Konturen, Nase), gewinnt sie
    lum = rgb @ np.array([0.3, 0.55, 0.15])
    ink = (lum < 0.26) & (a > 0.5)
    fi = down(ink.astype(float))
    ic = np.stack([down(rgb[..., c] * ink) for c in range(3)], -1) / np.maximum(fi[..., None], 1e-6)
    use = fi > 0.28
    col[use] = ic[use]
    # etwas mehr Saettigung und Tiefe, damit die Figur auf den Orten steht
    g = col.mean(-1, keepdims=True)
    col = np.clip((g + (col - g) * 1.18 - 0.5) * 1.08 + 0.5, 0, 1)
    m = al > 0.45
    d = ((col[..., None, :] - palette) ** 2).sum(-1)
    col = palette[d.argmin(-1)]
    # Kontur: aussen dunkel, getoent nach der angrenzenden Farbe
    ring = ndimage.binary_dilation(m) & ~m
    acc = np.zeros_like(col)
    cnt = np.zeros(m.shape)
    for dy, dx in ((0, 1), (0, -1), (1, 0), (-1, 0)):
        acc += np.roll(np.roll(col * m[..., None], dy, 0), dx, 1)
        cnt += np.roll(np.roll(m, dy, 0), dx, 1)
    near = acc / np.maximum(cnt[..., None], 1)
    col = col.copy()
    col[ring] = np.clip(near[ring] * 0.3 + np.array(outline_col) * 0.6, 0, 1)
    m = m | ring
    return np.dstack([col * m[..., None], m.astype(float)])


def palette_of(img, k=40, seed=2):
    a = img[..., 3] > 0.5
    c = img[..., :3][a]
    rng = np.random.default_rng(seed)
    c = c[rng.choice(len(c), min(len(c), 20000), replace=False)]
    p = c[rng.choice(len(c), k, replace=False)]
    for _ in range(25):
        idx = ((c[:, None] - p[None]) ** 2).sum(-1).argmin(1)
        for j in range(k):
            if (idx == j).any():
                p[j] = c[idx == j].mean(0)
    return p
