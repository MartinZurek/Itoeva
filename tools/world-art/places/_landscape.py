"""Gemeinsamer Ablauf fuer die Landschaften aus Konzeptbuch Band 2
(docs/concept-art/world-conceptbook-v2/): Ausschnitt der Szenentafel in Spielformat (16:9),
Schriftzug und Figuren entfernen, dann pixelate.translate - die Tafeln sind schon Pixel-Art,
deshalb nur sanft beruhigen und auf die Spielpalette bringen."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
import concept as C
import pixelate as P


def render(name, box, figures=(), clones=(), text=None, smooth=1, colors=128):
    """box: Ausschnitt (x0, y0, x1, y1) der 1024x683-Tafel, Seitenverhaeltnis ~16:9.
    figures: Polygone zum Fuellen aus der Umgebung (Diffusion); clones: (Polygon, dx, dy)."""
    img = C.native('v2_' + name, (0, 0, 1024, 683))
    sh = img.shape[:2]
    if text:
        img = C.remove_text(img, text)
    for pts, dx, dy in clones:
        img = C.clone_fill(img, C.poly_mask(sh, pts, grow=2), dx, dy)
    for pts in figures:
        img = C.inpaint(img, C.poly_mask(sh, pts, grow=2))
    small = C.fit(img[box[1]:box[3], box[0]:box[2]], (P.W, P.H))
    return P.translate(small, n_colors=colors, smooth=smooth, line_k=0.85)


def to_img(x, y, box):
    """Tafel- in Spielkoordinaten (fuer Plaetze und Gehflaeche)."""
    sx = P.W / (box[2] - box[0])
    sy = P.H / (box[3] - box[1])
    return (x - box[0]) * sx, (y - box[1]) * sy
