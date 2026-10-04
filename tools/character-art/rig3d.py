"""Kleiner Formen-Renderer fuer die Wesen: Figur aus Ellipsoiden im Raum, aus beliebigem
Blickwinkel gerendert und in Pixel-Art uebersetzt.

Warum 3D fuer Pixel-Art: Die Figur soll sich drehen (Seite -> halb zugewandt -> vorn), laufen,
springen, und Ohren, Schwanz und Umhang sollen nachschwingen. Aus einem flachen Bild laesst sich
das nicht glaubwuerdig ableiten - aus Formen im Raum schon. Gerendert wird vierfach ueberabgetastet;
je Pixel wird Material und Lichtstufe bestimmt und erst danach in die Handpalette des Wesens
uebersetzt (Mehrheit je 4x4-Block). So entstehen saubere Pixel-Cluster statt verwaschener Toene.

Koordinaten der Figur: f = vorwaerts (Blickrichtung), y = oben, l = zur linken Seite der Figur.
Einheit = ein Pixel im fertigen Bild. Die Fuesse stehen auf y = 0.
"""
import numpy as np
from scipy import ndimage

SS = 4                       # Ueberabtastung
LIGHT = np.array([-0.55, 0.65, 0.52])   # Licht von oben links vorn (Bildraum: x rechts, y oben, z zum Betrachter)
LIGHT = LIGHT / np.linalg.norm(LIGHT)


def rot(axis, ang):
    """Drehmatrix um eine Achse (Rodrigues)."""
    a = np.asarray(axis, float)
    a = a / np.linalg.norm(a)
    c, s = np.cos(ang), np.sin(ang)
    x, y, z = a
    return np.array([[c + x * x * (1 - c), x * y * (1 - c) - z * s, x * z * (1 - c) + y * s],
                     [y * x * (1 - c) + z * s, c + y * y * (1 - c), y * z * (1 - c) - x * s],
                     [z * x * (1 - c) - y * s, z * y * (1 - c) + x * s, c + z * z * (1 - c)]])


def frame_from(direction, up=(0, 1, 0)):
    """Orthonormale Achsen, deren zweite Achse entlang [direction] zeigt (fuer Glieder, Ohren)."""
    d = np.asarray(direction, float)
    d = d / np.linalg.norm(d)
    u = np.asarray(up, float)
    if abs(np.dot(u, d)) > 0.95:
        u = np.array([1.0, 0, 0])
    a = np.cross(d, u)
    a /= np.linalg.norm(a)
    b = np.cross(a, d)
    return np.stack([a, d, b], 1)       # Spalten: quer, laengs, dick


class Shape:
    """Ellipsoid: Mittelpunkt, Achsen (Spalten = Richtungen), Radien, Material."""

    def __init__(self, center, radii, mat, axes=None, tag=None, clip=None):
        self.c = np.asarray(center, float)
        self.r = np.asarray(radii, float)
        self.R = np.eye(3) if axes is None else np.asarray(axes, float)
        self.mat = mat
        self.tag = tag
        self.clip = clip          # optional: Funktion (lokale Einheitskoordinaten) -> Maske, was wegfaellt


class Model:
    def __init__(self):
        self.shapes = []

    def add(self, *a, **k):
        s = Shape(*a, **k)
        self.shapes.append(s)
        return s

    def ground(self):
        """Ganze Figur so verschieben, dass ihr tiefster Punkt auf y = 0 steht."""
        low = min(sh.c[1] - np.sqrt(((sh.R @ np.diag(sh.r))[1] ** 2).sum()) for sh in self.shapes)
        for sh in self.shapes:
            sh.c = sh.c - np.array([0, low, 0])
        return self

    def ellipsoid(self, center, radii, mat, axes=None, **k):
        return self.add(center, radii, mat, axes, **k)

    def limb(self, p0, p1, r0, r1, mat, step=0.6, flat=1.0, **k):
        """Glied als Kette von Kugeln von p0 nach p1, Radius verlaeuft von r0 nach r1."""
        p0, p1 = np.asarray(p0, float), np.asarray(p1, float)
        n = max(2, int(np.linalg.norm(p1 - p0) / step) + 1)
        for t in np.linspace(0, 1, n):
            r = r0 + (r1 - r0) * t
            self.add(p0 + (p1 - p0) * t, (r, r, r * flat), mat, **k)

    def leaf(self, base, tip, width, thick, mat, normal_hint=(0, 0, 1), **k):
        """Flaches Blatt (Ohr, Fluegel, Umhangbahn) von base nach tip."""
        base, tip = np.asarray(base, float), np.asarray(tip, float)
        d = tip - base
        L = np.linalg.norm(d)
        axes = frame_from(d, normal_hint)
        return self.add(base + d / 2, (width / 2, L / 2, thick / 2), mat, axes, **k)


def view_matrix(yaw_deg):
    """Figur -> Bild: x = rechts, y = oben, z = zum Betrachter. yaw 0 = von vorn, 90 = Seite
    (blickt nach rechts), 180 = von hinten."""
    t = np.radians(yaw_deg)
    # Figurachsen (f, y, l) -> Bild. Von vorn liegt die linke Koerperseite rechts im Bild;
    # von der Seite (nach rechts blickend) zeigt die rechte Koerperseite zum Betrachter.
    return np.array([
        [np.sin(t), 0, np.cos(t)],
        [0, 1, 0],
        [np.cos(t), 0, -np.sin(t)],
    ])


def render(model, yaw, size=96, foot_row=93, center_x=48, materials=None, noise_seed=7):
    """Gibt (Material-Index, Lichtstufe, Tiefe, Teil-Index) im 4x-Raster zurueck."""
    V = view_matrix(yaw)
    S = size * SS
    depth = np.full((S, S), -1e9)
    mat = np.full((S, S), -1, int)
    shade = np.zeros((S, S))
    part = np.full((S, S), -1, int)
    local = np.zeros((S, S, 3))
    for idx, sh in enumerate(model.shapes):
        # Figurkoordinaten -> Bildkoordinaten (Pixel im 4x-Raster, y nach unten)
        c = V @ sh.c
        A = V @ sh.R @ np.diag(sh.r)
        ext = np.sqrt((A ** 2).sum(1))
        cx = (center_x + c[0]) * SS
        cy = (foot_row - c[1]) * SS
        x0, x1 = int(np.floor(cx - ext[0] * SS)) - 1, int(np.ceil(cx + ext[0] * SS)) + 1
        y0, y1 = int(np.floor(cy - ext[1] * SS)) - 1, int(np.ceil(cy + ext[1] * SS)) + 1
        x0, y0 = max(x0, 0), max(y0, 0)
        x1, y1 = min(x1, S), min(y1, S)
        if x0 >= x1 or y0 >= y1:
            continue
        ys, xs = np.mgrid[y0:y1, x0:x1]
        px = (xs + 0.5) / SS - center_x
        py = foot_row - (ys + 0.5) / SS
        M = np.linalg.inv(A)
        q = np.stack([px - c[0], py - c[1], -c[2] * np.ones_like(px)], -1)
        mq = q @ M.T
        mz = M[:, 2]
        a = mz @ mz
        b = mq @ mz
        c0 = (mq ** 2).sum(-1) - 1
        disc = b * b - a * c0
        hit = disc >= 0
        if not hit.any():
            continue
        d = (-b + np.sqrt(np.maximum(disc, 0))) / a
        u = mq + d[..., None] * mz
        if sh.clip is not None:
            hit &= ~sh.clip(u)
        zc = d
        nrm = u @ M          # Gradient (A^-T u)
        nrm /= np.linalg.norm(nrm, axis=-1, keepdims=True) + 1e-9
        reg = depth[y0:y1, x0:x1]
        win = hit & (zc > reg)
        if not win.any():
            continue
        reg[win] = zc[win]
        mat[y0:y1, x0:x1][win] = sh.mat
        lam = np.clip(nrm @ LIGHT, -1, 1)
        rim = np.clip(1 - nrm[..., 2], 0, 1) ** 3
        shade[y0:y1, x0:x1][win] = (0.5 + 0.5 * lam - 0.12 * rim)[win]
        part[y0:y1, x0:x1][win] = idx
        local[y0:y1, x0:x1][win] = u[win]
    return dict(mat=mat, shade=shade, depth=depth, part=part, local=local)


def to_pixels(buf, ramps, levels=(0.34, 0.5, 0.66, 0.8), texture=None, cover=0.4, ao=True, flat=None):
    """4x-Puffer -> fertiges RGBA-Bild in der Handpalette.

    ramps: je Material eine Liste von Farben (dunkel -> hell), so viele wie levels + 1.
    texture: optional f(buf) -> Zusatz zur Lichtstufe (Fellstriche, Schuppen).
    """
    mat, shade, depth = buf['mat'], buf['shade'].copy(), buf['depth']
    S = mat.shape[0]
    n = S // SS
    if texture is not None:
        shade = shade + texture(buf)
    # Umgebungsverdeckung: wo ein Teil dicht hinter einem naeheren liegt, wird es dunkler
    if ao:
        near = ndimage.maximum_filter(np.where(mat >= 0, depth, -1e9), size=7)
        occl = np.clip((near - depth) / 6.0, 0, 1)
        shade = shade - 0.22 * occl * (mat >= 0)
    lvl = np.digitize(shade, levels)
    # Augen und Glanzpunkte: feste Stufe, unabhaengig vom Licht (sonst saufen sie ab)
    for fm, fl in (flat or {}).items():
        lvl[mat == fm] = np.clip(lvl[mat == fm], fl, fl + 1) if fl < 3 else fl
    key = np.where(mat >= 0, mat * 16 + lvl, -1)
    # Mehrheit je Block
    blocks = key.reshape(n, SS, n, SS).transpose(0, 2, 1, 3).reshape(n, n, SS * SS)
    out = np.zeros((n, n, 4))
    covered = (blocks >= 0).mean(-1)
    keymaj = np.full((n, n), -1)
    for j in range(n):
        for i in range(n):
            if covered[j, i] < cover:
                continue
            v = blocks[j, i]
            v = v[v >= 0]
            vals, cnt = np.unique(v, return_counts=True)
            keymaj[j, i] = vals[cnt.argmax()]
    m = keymaj >= 0
    mi, li = keymaj // 16, keymaj % 16
    for j, i in zip(*np.nonzero(m)):
        ramp = ramps[mi[j, i]]
        out[j, i, :3] = ramp[min(li[j, i] + 1, len(ramp) - 1)]
        out[j, i, 3] = 1
    # Tiefe je Pixel (Block-Maximum) fuer Innenkonturen
    dblk = depth.reshape(n, SS, n, SS).max(axis=(1, 3))
    return out, mi, dblk


def outline(img, mi, dblk, ramps, inner=3.0):
    """Selektive Kontur: aussen dunkelste Stufe des angrenzenden Materials; innen dort, wo ein
    Teil deutlich vor einem anderen liegt (Arm vor Rumpf), auf der hinteren Seite."""
    a = img[..., 3] > 0
    out = img.copy()
    H, W = a.shape
    ring = ndimage.binary_dilation(a) & ~a
    for j, i in zip(*np.nonzero(ring)):
        best = None
        for dj, di in ((0, 1), (0, -1), (1, 0), (-1, 0)):
            jj, ii = j + dj, i + di
            if 0 <= jj < H and 0 <= ii < W and a[jj, ii]:
                if best is None or dblk[jj, ii] > dblk[best]:
                    best = (jj, ii)
        if best is not None:
            out[j, i, :3] = ramps[mi[best]][0]
            out[j, i, 3] = 1
    # Innen: Pixel, dessen Nachbar viel naeher ist und anderes Material/Teil hat
    for j, i in zip(*np.nonzero(a)):
        for dj, di in ((0, 1), (0, -1), (1, 0), (-1, 0)):
            jj, ii = j + dj, i + di
            if 0 <= jj < H and 0 <= ii < W and a[jj, ii] and dblk[jj, ii] - dblk[j, i] > inner:
                out[j, i, :3] = ramps[mi[j, i]][0]
                break
    return out


def stroke_texture(mats, amp=0.09, seed=5):
    """Fellstriche: kurze, senkrecht gestreckte Helligkeitsschwankungen, die an der Form haften
    (aus den lokalen Koordinaten jedes Teils), damit sie beim Laufen nicht ueber das Fell rutschen."""
    rng = np.random.default_rng(seed)
    table = rng.random((64, 64, 64))
    table = ndimage.gaussian_filter(table, (0.6, 2.2, 0.6), mode='wrap')
    table = (table - table.mean()) / (table.std() + 1e-9)

    def tex(buf):
        u = buf['local']
        part = buf['part']
        idx = np.floor((u + 1.3) * 10 + part[..., None] * np.array([7, 13, 3])).astype(int) % 64
        t = table[idx[..., 0], idx[..., 1], idx[..., 2]]
        keep = np.isin(buf['mat'], mats)
        return np.where(keep, t * amp, 0)
    return tex
