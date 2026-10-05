"""Fennec aus dem Key-Design-Blatt als Puppe (siehe puppet.py).

Quelle: `source/fennec_key.png` - die grosse Figur oben links im Key-Design-Blatt (04.10.),
freigestellt (rembg/isnet, Schwanz aus BiRefNet ergaenzt). Koordinaten unten sind Bildpunkte in
dieser Datei (345 x 390). Die Figur steht halb zugewandt, Blick nach rechts.
"""
import os
import numpy as np
from PIL import Image
from scipy import ndimage
from puppet import Puppet
import fennec_faces as Faces

HERE = os.path.dirname(os.path.abspath(__file__))
FEET = (195, 376)          # Mitte zwischen den Stiefeln, Bodenlinie

# Gelenke
NECK = (198, 186)
EAR_L = (158, 128)
EAR_R = (230, 120)
SHOULDER_L = (146, 226)
SHOULDER_R = (247, 218)
FLAP = (258, 214)
HIP_L = (172, 288)
HIP_R = (222, 288)
TAIL = (150, 296)
PELVIS = (196, 282)
KNEE_L, KNEE_R = (145, 328), (248, 328)
ANKLE_L, ANKLE_R = (113, 365), (277, 365)
SOLE_Y = {"l": 374, "r": 378}


def source():
    return Image.open(os.path.join(HERE, 'source', 'fennec_key.png')).convert('RGBA')


def build(img=None):
    p = Puppet(img if img is not None else source())
    H, W = p.H, p.W
    yy, xx = np.mgrid[0:H, 0:W]
    tail = (((xx - 80) / 66.0) ** 2 + ((yy - 284) / 72.0) ** 2 < 1) & (xx < 150) & ~p.poly(
        [(80, 236), (148, 214), (152, 250), (112, 272), (80, 270)])
    p.add('torso', np.zeros((H, W), bool), PELVIS, None, z=2)
    p.add('tail', tail, TAIL, 'torso', z=0)
    p.add('leg_l', p.poly([(150, 286), (190, 286), (172, 320), (140, 352), (130, 380), (85, 380), (100, 345), (128, 322)]),
          HIP_L, 'torso', z=1)
    p.add('leg_r', p.poly([(208, 286), (244, 286), (262, 318), (290, 340), (315, 380), (252, 380), (244, 342), (218, 316)]),
          HIP_R, 'torso', z=1)
    # Knie und Stiefel erben das Oberschenkelgelenk, drehen aber unabhaengig.
    for side, knee, ankle in [('l', KNEE_L, ANKLE_L), ('r', KNEE_R, ANKLE_R)]:
        leg = p.parts['leg_' + side].mask
        p.add('shin_' + side, leg & (yy >= knee[1] - 3), knee, 'leg_' + side, z=1)
        p.add('boot_' + side, leg & (yy >= ankle[1] - 8), ankle, 'shin_' + side, z=1)
    p.add('flap', p.poly([(258, 206), (346, 220), (346, 305), (296, 305), (268, 246), (256, 232)]), FLAP, 'torso', z=3)
    p.add('arm_l', p.poly([(76, 232), (146, 212), (152, 246), (112, 274), (76, 272)]), SHOULDER_L, 'torso', z=4)
    p.add('arm_r', p.poly([(236, 222), (272, 218), (282, 272), (236, 280)]), SHOULDER_R, 'torso', z=4)
    head = (yy < 190) & ~p.poly([(150, 182), (250, 182), (250, 200), (150, 200)])
    p.add('head', head, NECK, 'torso', z=5)
    p.add('ear_l', p.poly([(80, 10), (140, 40), (192, 118), (172, 140), (130, 138), (96, 84)]), EAR_L, 'head', z=6)
    p.add('ear_r', p.poly([(272, 20), (284, 60), (256, 128), (226, 130), (214, 112), (240, 58)]), EAR_R, 'head', z=6)
    p.add('tail_tip', tail & (xx < 72), (75, 292), 'tail', z=0)
    p.add('flap_tip', p.parts['flap'].mask & (xx > 300), (301, 246), 'flap', z=3)
    p.add('forelock', p.poly([(179, 108), (196, 86), (216, 103), (216, 127), (190, 133)]),
          (201, 128), 'head', z=7)
    p.finish('torso')
    return p


# ---------------------------------------------------------------------------------------------
# Varianten der Quelle: Augen zu (Blinzeln, Schlaf) und Rueckansicht

EYES = [(189, 157, 12.5, 9.5), (226, 157, 7.5, 8.0)]


def closed_eyes(rgba):
    """Augen zu: Augenflaeche mit dem Fell darueber zumalen, darunter ein dunkler Lidstrich."""
    a = np.array(rgba).astype(float) / 255
    H, W = a.shape[:2]
    yy, xx = np.mgrid[0:H, 0:W]
    for cx, cy, rx, ry in EYES:
        e = ((xx - cx) / rx) ** 2 + ((yy - cy) / ry) ** 2 < 1
        # Fellfarbe aus dem Streifen ueber dem Auge, nach unten fortgesetzt
        for x in range(int(cx - rx), int(cx + rx) + 1):
            col = e[:, x]
            if not col.any():
                continue
            top = np.flatnonzero(col)[0]
            a[col, x, :3] = a[max(0, top - 3), x, :3] * 0.96
        lid = (np.abs(yy - (cy + 1.5 + 0.05 * (xx - cx) ** 2 / max(rx, 1) * 1.2)) < 1.6) & (np.abs(xx - cx) < rx * 0.9)
        a[lid, :3] = np.array([0.12, 0.06, 0.04])
    return Image.fromarray(np.uint8(a * 255), 'RGBA')


def back_view(rgba, seed=4):
    """Rueckansicht aus derselben Gestalt: Kopf und Ohren von hinten im Fell, Rumpf und Arme unter
    dem Mantel, Beine und Schwanz bleiben. Beim Rendern gespiegelt, damit der Schwanz zur anderen
    Seite haengt.
    Die Helligkeit der Vorlage bleibt als Faltenwurf erhalten."""
    a = np.array(rgba).astype(float) / 255
    H, W = a.shape[:2]
    yy, xx = np.mgrid[0:H, 0:W]
    rgb = a[..., :3]
    lum = rgb @ np.array([0.3, 0.55, 0.15])
    rng = np.random.default_rng(seed)
    p = Puppet(rgba)

    def texture(patch_box, mask, gain=0.55, sigma=4.0):
        """Flaeche neu einfaerben: Grundfarbe aus dem Muster, grobe Licht-/Faltenform aus der
        weichgezeichneten Vorlage (keine Gesichtszuege), feine senkrechte Striche als Fell/Stoff."""
        x0, y0, x1, y1 = patch_box
        patch = rgb[y0:y1, x0:x1].reshape(-1, 3)
        base = np.median(patch, 0)
        soft = ndimage.gaussian_filter(np.where(mask, lum, lum[mask].mean()), sigma)
        shade = (soft - soft[mask].mean()) * gain
        strokes = ndimage.gaussian_filter(rng.standard_normal((H, W)), (2.2, 0.6)) * 0.12
        rgb[mask] = np.clip(base[None, :] * (0.86 + shade[mask, None] * 1.4 + strokes[mask, None]), 0, 1)
        rim = mask & ~ndimage.binary_erosion(mask, iterations=2)
        rgb[rim] *= 0.68

    ears = p.poly([(80, 10), (140, 40), (192, 118), (172, 140), (130, 138), (96, 84)]) | \
        p.poly([(272, 20), (284, 60), (256, 128), (226, 130), (214, 112), (240, 58)])
    head = (yy < 190) & ~ears
    body = p.poly([(76, 196), (346, 196), (346, 300), (260, 300), (232, 296), (160, 296), (120, 276), (76, 276)])
    tail = ((xx - 80) / 66.0) ** 2 + ((yy - 284) / 72.0) ** 2 < 1
    body &= ~(tail & (xx < 140))
    texture((196, 128, 214, 142), head, 0.8, 6.0)       # Stirnfell
    texture((206, 60, 222, 100), ears, 0.8, 5.0)        # Ohr aussen (rechtes Ohr, Fellseite)
    texture((150, 205, 180, 240), body, 0.9, 2.5)       # Mantel
    # Saum und Naht dunkel nachziehen, Kapuzenrand am Hals
    edge = body & ~ndimage.binary_erosion(body, iterations=2) & (yy > 200)
    rgb[edge] *= 0.55
    hood = (np.abs(yy - 192) < 3) & (xx > 140) & (xx < 262)
    rgb[hood & (a[..., 3] > 0.5)] = rgb[hood & (a[..., 3] > 0.5)] * 0.6
    a[..., :3] = rgb
    return Image.fromarray(np.uint8(a * 255), 'RGBA')      # gespiegelt wird beim Rendern


# ---------------------------------------------------------------------------------------------
# Bewegung: lokale Matrizen je Teil aus wenigen Groessen

from puppet import affine, pixelize, palette_of
import motion as Mo

LEG_LEN = FEET[1] - HIP_L[1]
FRAME = 128
FOOT_ROW = 125
SCALE = 0.315


def mats(bob=0.0, lean=0.0, squash=1.0, sx=1.0, breath=0.0,
         leg_l=0.0, lift_l=0.0, leg_r=0.0, lift_r=0.0,
         arm_l=0.0, arm_r=0.0, head=0.0, head_dy=0.0,
         ear_l=0.0, ear_r=0.0, tail=0.0, flap=0.0, gait=None,
         tail_tip=0.0, flap_tip=0.0, forelock=0.0):
    """Gelenkwinkel in Grad (positiv = im Uhrzeigersinn), Wege in Bildpunkten der Quelle.
    bob > 0 senkt den Koerper; die Beine beugen sich dabei, die Fuesse bleiben am Boden."""
    root = affine(lean, FEET) @ affine(0, FEET, sx=sx, sy=squash)
    torso = root @ affine(0, PELVIS, t=(0, bob), sy=1 + 0.014 * breath)

    def leg(rot, lift, hip):
        sy = max(0.35, (LEG_LEN - bob - lift) / LEG_LEN)
        return affine(rot, hip, sy=sy)
    result = {
        'torso': torso,
        'leg_l': leg(leg_l, lift_l, HIP_L),
        'leg_r': leg(leg_r, lift_r, HIP_R),
        'arm_l': affine(arm_l, SHOULDER_L),
        'arm_r': affine(arm_r, SHOULDER_R),
        'flap': affine(flap, FLAP),
        'tail': affine(tail, TAIL),
        'head': affine(head, NECK, t=(0, head_dy - 1.2 * breath)),
        'ear_l': affine(ear_l, EAR_L),
        'ear_r': affine(ear_r, EAR_R),
        'tail_tip': affine(tail_tip, (75, 292)),
        'flap_tip': affine(flap_tip, (301, 246)),
        'forelock': affine(forelock, (201, 128)),
    }
    if gait is not None:
        for side, hip, knee, ankle, phase in [
            ('l', HIP_L, KNEE_L, ANKLE_L, gait),
            ('r', HIP_R, KNEE_R, ANKLE_R, gait + 0.5),
        ]:
            target, foot_angle = foot_path(phase)
            target = (target[0], target[1] + FEET[1] - 1 - SOLE_Y[side])
            # Ziel im Bodenraum, dann ins bewegte Hueftsystem zurueckrechnen.
            local_target = np.linalg.inv(torso) @ np.array([*target, 1.0])
            upper, lower = solve_leg(hip, knee, ankle, local_target[:2])
            result['leg_' + side] = affine(upper, hip)
            result['shin_' + side] = affine(lower - upper, knee)
            result['boot_' + side] = affine(foot_angle - lower - lean, ankle)
    return result


def foot_path(phase):
    """Eine Haelfte steht am Boden, die andere fuehrt den Fuss nach vorn.

    Waehrend der Standphase wandert der Fuss relativ zum vorwaerts gehenden Rumpf linear
    nach hinten; beim Vorschwingen hebt er ab. Kein Drehen der ganzen Zeichnung.
    """
    phase %= 1.0
    stride = 24.0
    if phase < 0.5:
        t = phase / 0.5
        return (195.0 + stride * (1 - 2 * t), 365.0), 0.0
    t = (phase - 0.5) / 0.5
    ease = t * t * (3 - 2 * t)
    return (195.0 + stride * (2 * ease - 1), 365.0 - 23.0 * np.sin(np.pi * t)), -14.0 * np.sin(2 * np.pi * t)


def solve_leg(hip, knee, ankle, target):
    """Zwei starre Segmente statt Skalierung eines ganzen Beins (Winkel in Grad)."""
    hip, knee, ankle, target = map(lambda p: np.asarray(p, dtype=float), (hip, knee, ankle, target))
    upper = np.linalg.norm(knee - hip)
    lower = np.linalg.norm(ankle - knee)
    delta = target - hip
    distance = np.clip(np.linalg.norm(delta), abs(upper - lower) + 1e-5, upper + lower - 1e-5)
    base = np.arctan2(delta[1], delta[0])
    angle = np.arccos(np.clip((upper**2 + distance**2 - lower**2) / (2 * upper * distance), -1, 1))
    # Das Knie beugt sich in Laufrichtung. Die Materiallaengen bleiben erhalten.
    a = base - angle
    joint = hip + upper * np.array([np.cos(a), np.sin(a)])
    b = np.arctan2(target[1] - joint[1], target[0] - joint[0])
    return (np.degrees(a - np.arctan2(*(knee-hip)[::-1])),
            np.degrees(b - np.arctan2(*(ankle-knee)[::-1])))



def walk(n=8, steps=64, turn=1.0, lean=3.0):
    ph = 2 * np.pi * np.arange(steps) / steps
    bob = 1 + 4 * np.cos(2 * ph)
    acc = -np.gradient(np.gradient(bob))
    ear = Mo.spring(acc * 900, k=120, c=10)
    tail = Mo.spring(np.sin(2 * ph - 0.6) * 300, k=70, c=8)
    flap = Mo.spring(np.cos(2 * ph) * 500, k=140, c=9)
    out = []
    for f in range(n):
        i = f * steps // n
        p = ph[i]
        out.append(dict(
            bob=2.0 + 2.0 * np.cos(2 * p), lean=0.0, gait=f / n if turn else None,
            leg_l=12 * np.cos(p) * turn, lift_l=22 * max(0.0, np.sin(p)) ** 1.2,
            leg_r=-12 * np.cos(p) * turn, lift_r=22 * max(0.0, -np.sin(p)) ** 1.2,
            arm_l=-14 - 9 * np.cos(p), arm_r=6 * np.cos(p),
            head=-1.5 * np.sin(2 * p - 0.6), head_dy=1.5 * np.cos(2 * p - 0.8),
            ear_l=-ear[i] * 0.5, ear_r=ear[i] * 0.5,
            tail=-3 + tail[i] * 0.05, flap=(32 if turn else 14) + flap[i] * 0.05,
            tail_tip=5 * np.sin(p - 1.0), flap_tip=9 * np.sin(2*p - 0.9),
            forelock=2 * np.sin(2*p - 0.7),
        ))
    return out


def idle(n=8, steps=64):
    ph = 2 * np.pi * np.arange(steps) / steps
    kick = np.zeros(steps)
    kick[int(steps * 0.55)] = 5000
    twitch = Mo.spring(kick, k=300, c=14)
    out = []
    for f in range(n):
        i = f * steps // n
        p = ph[i]
        out.append(dict(
            breath=np.sin(p), bob=0.6 * np.sin(p),
            head=0.8 * np.sin(p + 0.6),
            ear_l=-1.0 * np.sin(p - 0.5), ear_r=1.0 * np.sin(p - 0.5) + twitch[i] * 0.25,
            tail=3 * np.sin(p - 0.9), flap=2.5 * np.sin(p - 1.3),
            arm_l=5 * np.sin(p - 0.3), arm_r=-3 * np.sin(p - 0.3) + 32 * max(0, np.sin(p - 0.8)),
            tail_tip=4 * np.sin(p - 1.6), flap_tip=5 * np.sin(p - 1.9),
            forelock=1.5 * np.sin(p - 0.7),
        ))
    return out


def joy():
    return [
        dict(squash=0.93, bob=10, arm_l=-20, arm_r=8, head_dy=2, ear_l=-10, ear_r=10, tail=-6, flap=12),
        dict(squash=1.06, bob=-4, arm_l=60, arm_r=-110, ear_l=8, ear_r=-8, tail=10, flap=30, head=-4),
        dict(squash=1.04, bob=-2, arm_l=70, arm_r=-125, ear_l=4, ear_r=-4, tail=6, flap=20, head=-5),
        dict(squash=1.0, lift_l=14, lift_r=12, arm_l=50, arm_r=-100, ear_l=-6, ear_r=6, tail=-10, flap=-15, head=-3),
        dict(squash=1.03, lift_l=6, lift_r=5, arm_l=30, arm_r=-60, ear_l=-12, ear_r=12, tail=-14, flap=-25),
        dict(squash=0.9, bob=12, arm_l=-30, arm_r=10, ear_l=10, ear_r=-10, tail=12, flap=25, head_dy=3),
    ]


def sleep(n=4):
    out = []
    for f in range(n):
        b = np.sin(2 * np.pi * f / n)
        out.append(dict(bob=30 + 1.5 * b, breath=b, head=12, head_dy=7 + 1.0 * b, ear_l=-34 - b, ear_r=34 + b,
                        arm_l=-62, arm_r=10, tail=-18, flap=55))
    return out


# Die ersten 39 Indizes bleiben kompatibel. Neue Richtungs-Ruhe und Freude folgen.
EXPRESSION_COUNT = 68


def plan():
    """Kopfzeichnung und gegliederte Koerperpose ergeben gemeinsam ein Animationsbild."""
    I, Wk = idle(), walk()
    front = walk(turn=0.0)
    faces = ['neutral', 'neutral', 'curious', 'profile', 'profile', 'curious', 'neutral', 'neutral']
    P = [(faces[i], x) for i, x in enumerate(I)]
    P.append(('blink', I[0]))
    P += [('focused', x) for x in Wk]
    P += [('happy', x) for x in joy()]
    P += [('blink', x) for x in sleep()]
    P.append(('front', {}))
    P += [('front', front[k]) for k in (0, 2, 4, 6)]
    P.append(('back', {}))
    P += [('back', front[k]) for k in (0, 2, 4, 6)]
    P.append(('neutral', dict(bob=2, head=-4, arm_l=-6, flap=8, tail=-3)))
    P.append(('back', dict(bob=2, head=4, arm_l=6, flap=-8, tail=3)))
    P += [('front', x) for x in I]                  # 39..46
    P += [('back', x) for x in I]                   # 47..54
    P.append(('front_blink', I[0]))                 # 55
    P += [('front_happy', x) for x in joy()]         # 56..61
    P += [('back', x) for x in joy()]               # 62..67
    assert len(P) == EXPRESSION_COUNT
    return P


def frames():
    src = source()
    puppets = {name: build(Faces.attach(back_view(src) if name == 'back' else src, name))
               for name in {name for name, _ in plan()}}
    pal = palette_of(puppets['neutral'].img, k=56)
    pal = np.concatenate([pal, palette_of(puppets['back'].img, k=16)])
    off = (60, 30)
    cw, ch = puppets['neutral'].W + 120, puppets['neutral'].H + 40
    out = []
    for variant, par in plan():
        p = puppets[variant]
        img = p.pose(mats(**par), size=(cw, ch), offset=off)
        sm = pixelize(img, SCALE, pal)
        fx, fy = (FEET[0] + off[0]) * SCALE, (FEET[1] + off[1]) * SCALE
        fr = np.zeros((FRAME, FRAME, 4))
        dx, dy = int(round(FRAME / 2 - fx)), int(round(FOOT_ROW - fy))
        ys, xs = np.nonzero(sm[..., 3] > 0)
        ty, tx = ys + dy, xs + dx
        ok = (ty >= 0) & (ty < FRAME) & (tx >= 0) & (tx < FRAME)
        fr[ty[ok], tx[ok]] = sm[ys[ok], xs[ok]]
        if variant == 'back':
            fr = fr[:, ::-1]
        # Der seitliche Gang hat geloeste Bodenanker; nicht danach den ganzen Koerper
        # verschieben. Sonstige Posen behalten die bisherige Fussnormierung.
        rows = np.nonzero(fr[..., 3].any(1))[0]
        shift = 0 if par.get("gait") is not None else FOOT_ROW - rows.max()
        fr = np.roll(fr, shift, axis=0)
        if shift > 0:
            fr[:shift] = 0
        elif shift < 0:
            fr[shift:] = 0
        out.append(fr)
    return out

