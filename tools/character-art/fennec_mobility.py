"""Gezeichnete Ganzkoerperposen mit gemeinsamem Raster und Fussanker importieren.

ImageGen platziert Figuren nicht verlaesslich in mathematisch gleichen Zellen.
Zusammenhangskomponenten werden deshalb zuerst isoliert und danach nach Zeilen
geordnet. Ein angeschnittener Stiefel ist ein Fehler, kein gueltiger Bodenanker.
"""
from pathlib import Path
import numpy as np
from PIL import Image
from scipy import ndimage
from puppet import Puppet, affine, pixelize

HERE = Path(__file__).resolve().parent
FRAME, GROUND = 128, 125
DIRECTION_FIRST, ACTION_FIRST, DIRECTION_ACTION_FIRST = 68, 84, 100
FRAME_COUNT = 114


def drawings(filename, columns, rows):
    a = np.asarray(Image.open(HERE / 'source' / filename).convert('RGBA'))
    labels, _ = ndimage.label(a[..., 3] > 128)
    sizes = np.bincount(labels.ravel())
    ids = np.flatnonzero(sizes > 4000)
    ids = ids[ids != 0]
    if len(ids) != columns * rows:
        raise ValueError(f'{filename}: {len(ids)} statt {columns * rows} Figuren')
    figures = []
    for label in ids:
        ys, xs = np.nonzero(labels == label)
        if xs.min() == 0 or ys.min() == 0 or xs.max() == a.shape[1]-1 or ys.max() == a.shape[0]-1:
            raise ValueError(f'{filename}: angeschnittene Figur')
        mask = ndimage.binary_dilation(labels == label, iterations=2)
        isolated = a.copy()
        isolated[~mask] = 0
        box = (max(0, xs.min()-3), max(0, ys.min()-3),
               min(a.shape[1], xs.max()+4), min(a.shape[0], ys.max()+4))
        figures.append((ys.min(), xs.min(), isolated[box[1]:box[3], box[0]:box[2]] / 255.0))
    figures.sort(key=lambda item: item[0])
    ordered = []
    for row in range(rows):
        ordered += sorted(figures[row*columns:(row+1)*columns], key=lambda item: item[1])
    return [item[2] for item in ordered]


def rasterize(drawings, scale, palette, head_anchor=False):
    result = []
    for art in drawings:
        small = pixelize(art, scale, palette)
        ys, xs = np.nonzero(small[..., 3] > .5)
        # Front und Ruecken am Kopf ausrichten: der wechselnde Schwanz darf
        # nicht den ganzen Koerper seitlich mitziehen.
        if head_anchor:
            upper = small[..., 3] > .5
            upper[round(small.shape[0] * .32):] = False
            anchor = np.nonzero(upper)[1].mean()
        else:
            anchor = (xs.min() + xs.max()) / 2
        dx, dy = round(FRAME/2 - anchor), GROUND - ys.max()
        tx, ty = xs + dx, ys + dy
        if tx.min() < 0 or tx.max() >= FRAME or ty.min() < 0:
            raise ValueError('Figur passt nicht ins gemeinsame Raster; nicht abschneiden')
        frame = np.zeros((FRAME, FRAME, 4))
        frame[ty, tx] = small[ys, xs]
        result.append(frame)
    return result


def frames(palette):
    directions = rasterize(drawings('fennec-walk-directions-atlas.png', 4, 4), .33, palette, True)
    actions = rasterize(drawings('fennec-actions-atlas.png', 4, 4), .38, palette)
    # Die Richtungs-Aktionsquelle hat tatsaechlich sieben Spalten / zwei Zeilen:
    # Hocke, Absprung, Flug, Sitzen, Stand, Buecken, Strecken (kein Landebild).
    directional = rasterize(drawings('fennec-actions-directions-atlas.png', 7, 2), .31, palette)
    return directions, actions, directional


def gentle_idle(frame, palette):
    """Neue Koerperzeichnung mit kleinen Regungen; die Stiefel bleiben fest.

    Das bestehende Puppet-Werkzeug bewegt Kopf, Ohren und Mantelsaum getrennt,
    statt fuer Atmen die gesamte Figur samt Bodenanker hochzuschieben.
    """
    p = Puppet(Image.fromarray(np.uint8(frame * 255)), soft=1)
    y, x = np.indices(frame.shape[:2])
    p.add('body', np.zeros((FRAME, FRAME), bool), (64, 94), z=1)
    p.add('head', y < 74, (64, 73), z=2)
    p.add('ear_l', (y < 52) & (x < 64), (55, 53), 'head', z=3)
    p.add('ear_r', (y < 52) & (x >= 64), (74, 53), 'head', z=3)
    p.add('cape', (y > 74) & (y < 116) & ((x < 47) | (x > 83)), (64, 83), z=1)
    p.add('boots', y >= 116, (64, 125), z=4)
    p.finish('body')
    out = []
    for i in range(8):
        phase = 2*np.pi*i/8
        im = p.pose({'head': affine(.9*np.sin(phase), (64, 73)),
                     'ear_l': affine(1.8*np.sin(phase-.5), (55, 53)),
                     'ear_r': affine(1.5*np.sin(phase-1), (74, 53)),
                     'cape': affine(1.8*np.sin(phase-1.4), (64, 83))}, ss=2)
        posed = pixelize(im, 1, palette)
        # Die einmalige Kontur darf nicht unter den unveraenderten Sohlen wachsen.
        posed[GROUND+1:] = 0
        out.append(posed)
    return out


def apply(existing, palette):
    directions, actions, directional = frames(palette)
    existing[28:32] = [directions[i] for i in (0, 2, 4, 6)]
    existing[33:37] = [directions[i+8] for i in (0, 2, 4, 6)]
    existing[27], existing[32] = directional[4], directional[11]
    existing[17:23] = [actions[i] for i in (0, 1, 2, 2, 3, 11)]
    existing[23:27] = gentle_idle(actions[15], palette)[::2]
    # Eigene Vorder-/Rueckenkoerper auch beim Anhalten. Die erste Hocke
    # dient als Landung; es gibt dafuer noch keine separate Richtungszeichnung.
    existing[39:47] = gentle_idle(directional[4], palette)
    existing[47:55] = gentle_idle(directional[11], palette)
    # Der generierte Lidschlag wird am Standbild registriert, damit Blinzeln
    # keinen Ruecksprung zur alten breitbeinigen Frontzeichnung ausloest.
    blink = drawings('fennec-front-blink.png', 1, 1)[0]
    reference = np.nonzero(directional[4][..., 3] > .5)[0]
    scale = (reference.max()-reference.min()-1) / blink.shape[0]
    existing[55] = rasterize([blink], scale, palette, True)[0]
    existing[56:62] = [directional[i] for i in (0, 1, 2, 2, 0, 4)]
    existing[62:68] = [directional[i] for i in (7, 8, 9, 9, 7, 11)]
    result = existing + directions + actions + directional
    assert len(result) == FRAME_COUNT
    return result
