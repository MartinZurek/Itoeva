"""Gezeichnete seitliche Gangphasen; Registrierung am Schmuck statt an der Bildmitte.

Breitbeinige Standpose und auswaerts zeigende Stiefel lassen sich nicht durch
IK-Ziele in eine anatomisch richtige Profilzeichnung verwandeln. Der seitliche
Gang verwendet daher acht eigene Posen aus einem gemeinsamen Atlas.
"""
from pathlib import Path
import numpy as np
from PIL import Image
from scipy import ndimage
from puppet import pixelize

SOURCE = Path(__file__).resolve().parent / 'source' / 'fennec-walk-profile-atlas.png'
SCALE = .22
FRAME = 128
GROUND = 125
BROOCH_X = 68


def cells():
    atlas = Image.open(SOURCE).convert('RGBA')
    result = []
    for i in range(8):
        x, y = i % 4, i // 4
        a = np.array(atlas.crop((x*atlas.width//4, y*atlas.height//2,
                                  (x+1)*atlas.width//4, (y+1)*atlas.height//2)))
        labels, _ = ndimage.label(a[..., 3] > 128)
        sizes = np.bincount(labels.ravel())
        sizes[0] = 0
        main = labels == sizes.argmax()
        a[~ndimage.binary_dilation(main, iterations=2)] = 0
        result.append(a.astype(float)/255)
    return result


def brooch_x(a):
    yy = np.indices(a.shape[:2])[0]
    # Der gleiche tuerkise Schmuck ist unabhaengig von Bein- und Schwanzpose.
    m = ((a[..., 3] > .7) & (yy > 195) & (yy < 285)
         & (a[..., 1] > a[..., 0] + 15/255)
         & (a[..., 2] > a[..., 0] + 5/255))
    xs = np.nonzero(m)[1]
    if len(xs) < 20:
        raise ValueError('Kein eindeutiger Schmuckanker in der Gangzeichnung')
    return xs.mean()


def frames(palette):
    out = []
    for a in cells():
        small = pixelize(a, SCALE, palette)
        # Gemeinsamer Massstab fuer alle Bilder, kein individuelles Strecken.
        dx = int(round(BROOCH_X - brooch_x(a) * SCALE))
        ys, xs = np.nonzero(small[..., 3] > .5)
        dy = GROUND - ys.max()
        target_y, target_x = ys + dy, xs + dx
        if (target_x.min() < 0 or target_x.max() >= FRAME or target_y.min() < 0):
            raise ValueError('Gangzeichnung passt nicht in den gemeinsamen Frame')
        frame = np.zeros((FRAME, FRAME, 4))
        frame[target_y, target_x] = small[ys, xs]
        out.append(frame)
    return out
