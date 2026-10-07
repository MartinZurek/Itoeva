"""Wyrmlings schlankere Anatomie und gezeichnete Gelenkstellungen.

Sitz-/Aufstehrollen enthalten gebeugte Hinterbeine statt einen skalierten
Standkoerper. Beide Quellboegen teilen nach der Registrierung einen Massstab;
der Zusatzbogen wird anhand seiner identischen Front-Standpose kalibriert.
"""
import numpy as np
from PIL import Image
import ensemble_motion as E


def sources():
    main = E.drawings('wyrmling', source=E.HERE/'source/wyrmling-refined-atlas.png')
    actions = E.drawings('wyrmling', source=E.HERE/'source/wyrmling-posture-atlas.png', count=16)
    # Die Front mit geschlossenen Augen hat dieselbe Anatomie wie die offene
    # Front im Hauptbogen. Sitzhoehen sind dafuer kein verlaesslicher Massstab.
    factor = main[1].shape[0] / actions[0].shape[0]
    actions = [np.asarray(Image.fromarray(np.uint8(a*255)).resize(
        (round(a.shape[1]*factor), round(a.shape[0]*factor)), Image.Resampling.LANCZOS))/255.
        for a in actions]
    return main + actions


def eye_patch(base, closed, windows, offset=(0, 0)):
    out = base.copy()
    for x0, y0, x1, y1 in windows:
        dx, dy = offset
        patch = closed[y0-dy:y1-dy, x0-dx:x1-dx]
        if not (patch[..., 3] > .5).all():
            raise ValueError('Wyrmling: unvollstaendiges Lidfenster')
        out[y0:y1, x0:x1] = patch
    return out


def anchored(art):
    """Schwanzbreite darf weder Kopfachse noch stuetzende Vorderpfoten verschieben."""
    def move(frame, dx):
        ys, xs = np.nonzero(frame[..., 3] > .5)
        tx = xs + dx
        if tx.min() < 3 or tx.max() >= E.FRAME-3:
            raise ValueError('Wyrmling: Anker schneidet eine Pose an')
        out = np.zeros_like(frame)
        out[ys, tx] = frame[ys, xs]
        return out

    for i in [1, 2, *range(12, 20), 32, *range(40, 48)]:
        y, x = np.nonzero(art[i][..., 3] > .5)
        head = y < y.min() + int((y.max()-y.min()+1)*.30)
        cx = (x[head].min() + x[head].max()) / 2
        art[i] = move(art[i], round(64-cx))
    for i in (36, 37, 38):
        # Die rechte Vorderpfote bleibt vom Stand bis zum Aufstehen stehen.
        # Die tiefer knickenden Hinterbeine duerfen daneben ihre Form aendern.
        y, x = np.nonzero(art[i][..., 3] > .5)
        contact = x[y >= E.GROUND-3].max()
        sy, sx = np.nonzero(art[0][..., 3] > .5)
        target = sx[sy >= E.GROUND-3].max()
        art[i] = move(art[i], int(target-contact))
    return art


def frames():
    art, pal, _ = E.register(sources(), palette_size=64)
    art = anchored(art)
    stand, front, back = art[:3]
    extra = art[32:]
    # Nur die neu gezeichneten Lider gelangen in die bestehende Gesichtsform.
    blink = eye_patch(stand, extra[1], SIDE_EYES, offset=(2, -3))
    front_blink = eye_patch(front, extra[0], FRONT_EYES)
    walk, fw, bw, run = art[4:12], art[12:16], art[16:20], art[20:24]
    crouch, jump, _, sleep, stretch, reach, curl, joy = art[24:32]
    bend, kneel, lower, sit, rise, landing = extra[2:8]
    fi, bi = E.idle(front, 'wyrmling', pal), E.idle(back, 'wyrmling', pal)
    result = E.idle(stand, 'wyrmling', pal) + [blink] + walk
    result += [crouch, joy, joy, jump, landing, stand]
    result += E.idle(sleep, 'wyrmling', pal, n=4, sleeping=True)
    result += [front] + fw + [back] + bw + [front, back] + fi + bi + [front_blink]
    # Gerichtete Spruenge behalten ihren vorhandenen Bildvertrag. Die neue
    # abgesenkte Gelenkpose ist zugleich das anatomische Ausholen.
    result += [extra[8], front, front, front, front, front]
    result += [extra[12], back, back, back, back, back]
    result += [fw[i//2] for i in range(8)] + [bw[i//2] for i in range(8)]
    result += [crouch, jump, jump, landing, bend, kneel, kneel, crouch,
               lower, sit, rise, stand, stretch, reach, reach, sleep]
    # Rolle 0 ist bei Front/Ruecken sowohl Sitz-/Aufsteh-Zwischenbild als auch
    # Sprung-Ausholen. Aufstehen zeigt deshalb die gezeichnete Absenkung rueckwaerts.
    result += [extra[8], front, front, extra[9], front, extra[8], extra[11]]
    result += [extra[12], back, back, extra[13], back, extra[12], extra[15]]
    result += [run[i//2] for i in range(8)] + fw + bw + E.rolled(curl, pal)
    if len(result) != E.COUNT:
        raise ValueError(f'Wyrmling: {len(result)} statt {E.COUNT} Rollen')
    return result


# Aus der registrierten Zeichnung abgenommene Augenfenster, keine Farbsuche
# an Fluegeln oder Bernstein. Die Kopfregistrierung des Profil-Lids braucht
# zwei Pixel nach rechts und drei nach oben; der Rest der Figur bleibt identisch.
SIDE_EYES = ((98, 53, 109, 63),)
FRONT_EYES = ((53, 55, 63, 63), (70, 55, 78, 63))
