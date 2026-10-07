"""Gezeichnete Anatomie der vier verbleibenden Wesen, ohne Stand-Stauchung.

Ein Quellbogen enthaelt Gang und Gelenkstellungen im selben Zeichenstil.
Die 138 Bildrollen bleiben mit der Android-Laufzeit kompatibel.
"""
import numpy as np
from PIL import Image
import ensemble_motion as E

NAMES = ('gloop', 'puffling', 'starlet', 'hootlet')
FRONT_EYES = {name: E.EYE_WINDOWS[name] for name in NAMES}
SIDE_EYES = {
    'gloop': ((90, 92, 99, 103), (102, 91, 105, 100)),
    'puffling': ((79, 83, 89, 95), (95, 82, 101, 94)),
    'starlet': ((69, 72, 80, 86), (90, 72, 97, 84)),
    'hootlet': ((79, 77, 88, 89), (92, 77, 97, 87)),
}
FRONT_OFFSETS = {
    'gloop': ((-7, -2), (-2, -2)),
    'puffling': ((-8, -1), (-4, -1)),
    'starlet': ((0, -4), (1, -4)),
    'hootlet': ((0, -3), (-1, -3)),
}
SIDE_OFFSETS = {
    'gloop': ((6, -7), (3, -7)),
    'puffling': ((-2, -2), (0, -1)),
    'starlet': ((2, -5), (0, -5)),
    'hootlet': ((0, -6), (-2, -3)),
}
# Einzelne Quellgangphasen haben versehentlich geschlossene Lider. Ihre
# Koerper-/Fusszeichnung bleibt erhalten, nur bereits gezeichnete offene Augen
# aus derselben Blickrichtung werden in die abgenommenen Fenster registriert.
OPEN_PHASE_EYES = {
    'gloop': [(23, 21, ((90, 106, 99, 118),), ((-4, 1),)),
              (23, 0, ((105, 110, 107, 117),), ((2, 17),))],
    'hootlet': [(7, 0, ((86, 89, 96, 100), (100, 87, 105, 98)), ((7, 12), (8, 11))),
                (11, 0, ((84, 92, 95, 103), (100, 90, 105, 101)), ((6, 16), (8, 14)))],
}


def eye_patch(base, closed, windows, offsets):
    """Nur gezeichnete Lider einsetzen; Gesicht, Brille und Koerper bleiben fest."""
    out = base.copy()
    for (x0, y0, x1, y1), (dx, dy) in zip(windows, offsets, strict=True):
        patch = closed[y0-dy:y1-dy, x0-dx:x1-dx]
        if patch.shape != (y1-y0, x1-x0, 4) or not (patch[..., 3] > .5).all():
            raise ValueError('Unvollstaendiges Lidfenster')
        out[y0:y1, x0:x1] = patch
    return out


def sources(name):
    main = E.drawings(name, source=E.HERE/'source'/f'{name}-refined-atlas.png',
                      count=32 if name == 'starlet' else 48)
    main = main[:32]
    extra = E.drawings(name, source=E.HERE/'source'/f'{name}-posture-atlas.png', count=16)
    # Gleiche Standanatomien kalibrieren die beiden Boegen je Blickrichtung.
    # Niedrigere Sitzsilhouetten werden nie auf eine Standhoehe gedehnt.
    front_factor = main[1].shape[0]/extra[0].shape[0]
    side_factor = main[0].shape[0]/extra[1].shape[0]
    scaled = []
    for i, a in enumerate(extra):
        factor = side_factor if 1 <= i < 8 else front_factor
        scaled.append(np.asarray(Image.fromarray(np.uint8(a*255)).resize(
            (round(a.shape[1]*factor), round(a.shape[0]*factor)), Image.Resampling.LANCZOS))/255.)
    extra = scaled
    return main+extra


def move(frame, dx):
    y, x = np.nonzero(frame[..., 3] > .5)
    if (x+dx).min() < 3 or (x+dx).max() >= E.FRAME-3:
        raise ValueError('Anker schneidet eine Gelenkpose an')
    out = np.zeros_like(frame)
    out[y, x+dx] = frame[y, x]
    return out


def anchored(art, name):
    # Front/Ruecken folgen der oberen Koerperachse, nicht der Seitentasche.
    for i in [1, 2, *range(12, 20), 32, *range(40, 48)]:
        y, x = np.nonzero(art[i][..., 3] > .5)
        head = y < y.min()+int((y.max()-y.min()+1)*.30)
        cx = (x[head].min()+x[head].max())/2
        art[i] = move(art[i], round(64-cx))
    def support(frame):
        y, x = np.nonzero(frame[..., 3] > .5)
        x = x[y >= E.GROUND-3]
        # Die Standflaeche haelt die Achse. Eine nach vorn gefaltete Fuss-/
        # Sternspitze ist kein unveraenderlicher vierbeiniger Stuetzfuss.
        return (x.min()+x.max())/2
    target = support(art[0])
    for i in (36, 37, 38):
        art[i] = move(art[i], round(target-support(art[i])))
    return art


def registered(name):
    art, palette, _ = E.register(sources(name), palette_size=64)
    art = anchored(art, name)
    for target, source, windows, offsets in OPEN_PHASE_EYES.get(name, []):
        art[target] = eye_patch(art[target], art[source], windows, offsets)
    return art, palette


def frames(name):
    art, pal = registered(name)
    stand, front, back = art[:3]
    extra = art[32:]
    blink = eye_patch(stand, art[3], SIDE_EYES[name], SIDE_OFFSETS[name])
    front_blink = eye_patch(front, extra[0], FRONT_EYES[name], FRONT_OFFSETS[name])
    walk, fw, bw, run = art[4:12], art[12:16], art[16:20], art[20:24]
    crouch, jump, _, sleep, stretch, reach, curl, joy = art[24:32]
    bend, kneel, lower, sit, rise, landing = extra[2:8]
    result = E.idle(stand, name, pal)+[blink]+walk
    result += [crouch, joy, joy, jump, landing, stand]
    result += E.idle(sleep, name, pal, n=4, sleeping=True)
    result += [front]+fw+[back]+bw+[front, back]
    result += E.idle(front, name, pal)+E.idle(back, name, pal)+[front_blink]
    result += [extra[8], front, front, front, front, front]
    result += [extra[12], back, back, back, back, back]
    result += [fw[i//2] for i in range(8)]+[bw[i//2] for i in range(8)]
    result += [crouch, jump, jump, landing, bend, kneel, kneel, crouch,
               lower, sit, rise, stand, stretch, reach, reach, sleep]
    # Gerichtete Zwischenbilder spielen beim Aufstehen rueckwaerts. Die Rollen
    # 100/107 werden vom bestehenden Vertrag auch zum Sprung-Ausholen genutzt.
    result += [extra[8], front, front, extra[9], front, extra[8], extra[11]]
    result += [extra[12], back, back, extra[13], back, extra[12], extra[15]]
    result += [run[i//2] for i in range(8)]+fw+bw+E.rolled(curl, pal)
    if len(result) != E.COUNT:
        raise ValueError(f'{name}: falsche Anzahl Bildrollen')
    return result
