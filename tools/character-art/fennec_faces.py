"""Neu gezeichnete Kopfansichten am Halsanker der bestehenden Figur registrieren.

Die Atlaszeichnungen sind eigenstaendige Ansichten, keine gestauchten Gesichter.
Hier wird nur die Sprite-Vorbereitung vorgenommen: Zellen lesen, am Hals ausrichten.
"""
from pathlib import Path
from functools import lru_cache
import numpy as np
from PIL import Image
from scipy import ndimage

HERE = Path(__file__).resolve().parent
HEADS = {
    'neutral': (0, 0), 'blink': (0, 1), 'happy': (0, 2), 'curious': (0, 3),
    'profile': (0, 4), 'front': (0, 5), 'back': (0, 6), 'focused': (0, 7),
    'front_blink': (1, 0), 'front_happy': (1, 1), 'profile_blink': (1, 2), 'profile_happy': (1, 3),
}
FILES = [('fennec-head-atlas.png', 4, 2), ('fennec-head-expressions.png', 4, 1)]


@lru_cache(maxsize=None)
def head(name):
    atlas, slot = HEADS[name]
    filename, cols, rows = FILES[atlas]
    im = Image.open(HERE / 'source' / filename).convert('RGBA')
    x, y = slot % cols, slot // cols
    cell = im.crop((x * im.width // cols, y * im.height // rows,
                    (x+1) * im.width // cols, (y+1) * im.height // rows))
    a = np.array(cell)
    labels, count = ndimage.label(a[..., 3] > 128)
    sizes = np.bincount(labels.ravel())
    sizes[0] = 0
    if not count:
        raise ValueError(f'Leere Kopfzelle: {name}')
    mask = labels == sizes.argmax()
    # Kleine Fragmente benachbarter Atlaszellen nicht als Halsanker verwenden.
    a[~ndimage.binary_dilation(mask, iterations=2)] = 0
    cell = Image.fromarray(a)
    ys, xs = np.nonzero(mask)
    if not len(xs):
        raise ValueError(f'Leere Kopfzelle: {name}')
    box = (xs.min(), ys.min(), xs.max()+1, ys.max()+1)
    cell = cell.crop(box)
    # Grundansicht proportional skalieren; Mimik am selben Ansichtsrahmen
    # registrieren, damit Blinzeln keinen Wechsel der Kopfbreite ausloest.
    ratio = 180 / cell.height
    reference = {'blink': 'neutral', 'happy': 'neutral', 'focused': 'neutral',
                 'front_blink': 'front', 'front_happy': 'front',
                 'profile_blink': 'profile', 'profile_happy': 'profile'}.get(name)
    width = head(reference)[0].width if reference else round(cell.width * ratio)
    cell = cell.resize((width, 180), Image.Resampling.LANCZOS)
    alpha = np.asarray(cell)[..., 3]
    ys, xs = np.nonzero((alpha > 128) & (np.indices(alpha.shape)[0] > 173))
    neck_x = int(round(np.median(xs)))
    return cell, neck_x


def attach(body, name):
    base = body.copy().convert('RGBA')
    arr = np.array(base)
    arr[:193] = 0
    base = Image.fromarray(arr)
    art, neck_x = head(name)
    base.alpha_composite(art, (198 - neck_x, 13))
    return base
