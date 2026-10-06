"""Fennec nach dem Key-Design-Blatt (aufrecht, roter Umhang mit Tuerkis-Brosche, Lederweste,
Guertel, Handschuhe und Stiefel, riesige Ohren, buschiger Schwanz mit heller Spitze).

`build(pose)` setzt die Figur aus Formen zusammen; `Pose` beschreibt Gelenkwinkel und die
nachschwingenden Teile (Ohren, Schwanz, Umhang), die animate.py aus einer Feder-Simulation fuellt.
"""
from dataclasses import dataclass, field
import numpy as np
from rig3d import Model, rot, frame_from


def hexramp(*hs):
    return [np.array([int(h[i:i + 2], 16) for i in (1, 3, 5)]) / 255 for h in hs]


# Materialien: Index -> Farbrampe dunkel .. hell (erste Farbe = Kontur)
FUR, CREAM, EARIN, CAPE, LEATHER, METAL, GEM, IRIS, PUPIL, SPARK, NOSE, CLOTH, BOOT = range(13)
RAMPS = [
    hexramp('#4a1c0c', '#9c3f18', '#cf6a28', '#ec9440', '#f9b866', '#fdd598'),   # Fell
    hexramp('#5e3e2a', '#b88a64', '#dcb892', '#f1dcc0', '#fbeedb', '#fffaf0'),   # Creme
    hexramp('#5e2e26', '#b4705e', '#dc988a', '#f0b8a8', '#fad4c6', '#fde8de'),   # Ohr innen
    hexramp('#260a08', '#561a12', '#7c2618', '#9e3622', '#bc4c30', '#d26a44'),   # Umhang
    hexramp('#1c120a', '#3c2618', '#5a3c26', '#785636', '#977450', '#b49470'),   # Leder
    hexramp('#3a2a0e', '#7a5c22', '#b88c3a', '#dcb860', '#f4dc94', '#fff4c8'),   # Messing
    hexramp('#0a2e2e', '#145c58', '#269088', '#4cc0b2', '#98e6da', '#e0fff8'),   # Tuerkis
    hexramp('#1e0e06', '#4a260c', '#7a4416', '#a8661e', '#c88428', '#dca040'),   # Iris
    hexramp('#0c0604', '#140a06', '#1c100a', '#24160e', '#2c1c12', '#342214'),   # Pupille
    hexramp('#d8d0c0', '#f4eee2', '#fffcf4', '#ffffff', '#ffffff', '#ffffff'),   # Glanz
    hexramp('#0e0806', '#1c120c', '#2c1e16', '#3e2c22', '#544034', '#6a5446'),   # Nase
    hexramp('#1e2410', '#3a4220', '#56602e', '#707a3e', '#8a9450', '#a4ae68'),   # Tuch (oliv)
    hexramp('#160e08', '#2e1e12', '#48301e', '#64442a', '#80603c', '#9c7c54'),   # Stiefel
]


FLAT = {IRIS: 3, SPARK: 4, PUPIL: 0}


@dataclass
class Pose:
    bob: float = 0.0             # Huefthoehe +/- (Pixel)
    lean: float = 0.0            # Oberkoerper nach vorn (Grad)
    hip_l: float = 0.0           # Oberschenkel links nach vorn (Grad)
    hip_r: float = 0.0
    knee_l: float = 8.0          # Kniebeuge (Grad)
    knee_r: float = 8.0
    foot_lift_l: float = 0.0
    foot_lift_r: float = 0.0
    arm_l: float = 0.0           # Oberarm nach vorn (Grad)
    arm_r: float = 0.0
    elbow_l: float = 38.0
    elbow_r: float = 38.0
    arm_out: float = 22.0        # Arme vom Koerper weg (Grad)
    head_pitch: float = 0.0      # Kopf hoch (+) / runter (-)
    head_tilt: float = 0.0       # Kopf seitlich
    head_turn: float = 0.0       # Kopf dreht zur Seite (Grad, + = zur linken Koerperseite)
    ear_back: float = 0.0        # Ohren nach hinten (Grad) - Nachschwingen
    ear_spread: float = 0.0      # Ohren auseinander
    ear_twitch_l: float = 0.0
    ear_twitch_r: float = 0.0
    tail: list = field(default_factory=lambda: [0.0] * 8)    # Abweichung je Glied (Grad, + = hoch)
    tail_side: list = field(default_factory=lambda: [-6.0] * 8)
    cape: float = 0.0            # Umhang weht nach hinten (Grad)
    cape_lift: float = 0.0       # Umhang hebt sich (Sprung)
    breath: float = 0.0          # 0..1
    eyes: float = 1.0            # 1 offen, 0 zu
    squash: float = 0.0          # >0 gestaucht, <0 gestreckt
    curl: float = 0.0            # Schlafen: 0..1 zusammengerollt
    stance: float = 9.0          # Beine seitlich gespreizt (Grad)
    sit: bool = False            # sitzt (Schlaf)


def _leg(m, hip, ang, knee, lift, side, stance=0.0):
    """Bein: Oberschenkel, Unterschenkel, Stiefel. Winkel in der f-y-Ebene."""
    a = np.radians(ang)
    k = np.radians(ang - knee)
    st = np.radians(stance) * side
    thigh = np.array([np.sin(a), -np.cos(a) * np.cos(st), np.sin(st)]) * 8.0
    knee_p = hip + thigh
    shin = np.array([np.sin(k), -np.cos(k) * np.cos(st), np.sin(st) * 0.3]) * 8.0
    ankle = knee_p + shin + np.array([0, lift, 0])
    m.limb(hip, knee_p, 4.0, 3.1, FUR)
    m.limb(knee_p, ankle, 2.7, 2.6, FUR, )
    # Stiefelschaft und Fuss
    m.limb(knee_p + shin * 0.45, ankle, 3.0, 3.0, BOOT)
    m.ellipsoid(knee_p + shin * 0.45, (3.1, 1.0, 3.1), LEATHER)       # Stulpe
    foot_dir = np.array([1.0, -0.05 + lift * 0.04, 0])
    m.ellipsoid(ankle + np.array([1.8, -0.6, 0]), (4.3, 2.2, 2.9), BOOT, axes=frame_from(foot_dir, (0, 1, 0))[:, [1, 2, 0]])
    return ankle


def _arm(m, sh, ang, elbow, out, side):
    a, e, o = np.radians(ang), np.radians(ang + elbow), np.radians(out) * side
    up = np.array([np.sin(a), -np.cos(a) * np.cos(o), -np.cos(a) * np.sin(o) * -1])
    el = sh + up * 7.5
    lo = np.array([np.sin(e), -np.cos(e) * np.cos(o * 0.6), np.cos(e) * np.sin(o * 0.6)])
    wr = el + lo * 6.5
    m.limb(sh, el, 2.6, 2.2, FUR)
    m.limb(el, wr, 2.3, 2.3, LEATHER)          # Armschiene
    m.ellipsoid(wr + lo * 1.4, (2.5, 2.4, 2.3), LEATHER)   # Handschuh
    return wr


def build(p: Pose):
    m = Model()
    sq = 1 - p.squash * 0.12
    wide = 1 + p.squash * 0.08
    hip_y = (6.0 if p.sit else 17.0) + p.bob
    lean = np.radians(p.lean)
    up = np.array([np.sin(lean), np.cos(lean), 0])
    fwd = np.array([np.cos(lean), -np.sin(lean), 0])

    # Beine (links = +l)
    for side, ang, kn, lift in ((1, p.hip_l, p.knee_l, p.foot_lift_l), (-1, p.hip_r, p.knee_r, p.foot_lift_r)):
        _leg(m, np.array([0.3, hip_y, 4.6 * side]), ang, kn, lift, side, p.stance)

    pel = np.array([0, hip_y + 2.0, 0])
    m.ellipsoid(pel, (5.2 * wide, 4.6 * sq, 6.3 * wide), FUR)
    # Tuch vorn unter dem Guertel
    m.leaf(pel + np.array([4.6, 3.6, 0]), pel + np.array([5.6 + p.lean * 0.02, -4.0, 0]), 6.5, 1.3, CLOTH, normal_hint=(1, 0, 0))

    br = 1 + 0.035 * p.breath
    chest = pel + up * 8.6 * sq
    torso_axes = frame_from(up, (1, 0, 0))[:, [2, 1, 0]]     # Spalten: f, y, l
    m.ellipsoid(chest, (5.6 * br * wide, 7.6 * sq, 7.0 * br * wide), CREAM, axes=torso_axes)
    # Weste: Leder ueber dem Rumpf, vorn offen (heller Brustpelz sichtbar)
    m.ellipsoid(chest + up * 0.4, (5.95 * br * wide, 7.4 * sq, 7.35 * br * wide), LEATHER, axes=torso_axes,
                clip=lambda u: (u[..., 0] > 0.32) & (np.abs(u[..., 2]) < 0.4) | (u[..., 1] > 0.78))
    # Guertel mit Schnalle und Tasche
    belt = pel + up * 3.4
    m.ellipsoid(belt, (6.0 * wide, 1.3, 7.4 * wide), LEATHER, axes=torso_axes)
    m.ellipsoid(belt + fwd * 6.0, (0.9, 1.3, 1.4), METAL)
    m.ellipsoid(belt + np.array([-1.0, -2.2, -7.2]), (2.4, 2.8, 1.6), LEATHER)

    # Schwanz: Kette aus Kugeln, buschig, Spitze hell
    radii = [2.3, 3.3, 4.4, 5.3, 5.8, 5.8, 5.1, 3.9, 2.4]
    pos = pel + np.array([-4.5, -0.5, 0])
    base = np.radians(205)      # nach hinten unten, dann hoch
    ang = base
    yaw = 0.0
    for i in range(8):
        ang += np.radians(-17 + p.tail[i])
        yaw += np.radians(p.tail_side[i])
        d = np.array([np.cos(ang) * np.cos(yaw), np.sin(ang), np.cos(ang) * np.sin(yaw) * -1])
        nxt = pos + d * 3.0
        m.limb(pos, nxt, radii[i], radii[i + 1], CREAM if i >= 6 else FUR, step=0.9)
        pos = nxt

    # Arme
    shoulder = chest + up * 5.0
    for side, a, e in ((1, p.arm_l, p.elbow_l), (-1, p.arm_r, p.elbow_r)):
        _arm(m, shoulder + np.array([0, 0, 6.6 * side]) * wide, a, e, p.arm_out, side)

    # Umhang: Kragen um den Hals, Bahnen ueber Schultern und Ruecken
    neck = shoulder + up * 1.6
    for k in range(14):
        t = 2 * np.pi * k / 14
        m.ellipsoid(neck + np.array([np.cos(t) * 5.6, 0.3 * np.sin(3 * t), np.sin(t) * 6.6]), (2.5, 2.3, 2.5), CAPE)
    cape_a = np.radians(p.cape)
    lift = p.cape_lift
    # Schulterteil: Schale ueber Ruecken und Schultern, vorn offen
    m.ellipsoid(neck + up * -4.6 + fwd * -0.6, (6.8, 7.0, 9.4 * wide), CAPE, axes=torso_axes,
                clip=lambda u: ((u[..., 0] > 0.15) & (np.abs(u[..., 2]) < 0.62)) | (u[..., 1] < -0.7 + 0.18 * np.sin(u[..., 2] * 13)))
    # Bahnen hinten bis zu den Knien, wehen mit Schwung
    for side in (-1, -0.35, 0.35, 1):
        top = neck + np.array([-5.6 + 1.2 * abs(side), -1.2, 5.0 * side])
        sway = cape_a + np.radians(4 * side)
        L = 21.0 - 2.5 * abs(side)
        drop = np.array([-np.sin(sway) * L - 2.5, -np.cos(sway) * L + lift, 3.2 * side])
        bot = top + drop
        tatter = lambda u, s=side: (u[..., 1] > 0.72 + 0.12 * np.sin(u[..., 0] * 9 + s * 5)) & (np.abs(u[..., 0]) > 0.0)
        m.leaf(top, bot, 8.6, 1.8, CAPE, normal_hint=(-1, 0, 0.5 * side), clip=tatter)
    # Brosche
    m.ellipsoid(neck + np.array([5.7, -0.8, 0]), (0.9, 1.7, 1.7), METAL)
    m.ellipsoid(neck + np.array([6.4, -0.8, 0]), (0.8, 1.25, 1.25), GEM)

    # Kopf
    hp, ht, hturn = np.radians(p.head_pitch), np.radians(p.head_tilt), np.radians(p.head_turn)
    H = rot((0, 1, 0), hturn) @ rot((0, 0, 1), hp) @ rot((1, 0, 0), ht)
    head = neck + up * 7.6
    def at(v):
        return head + H @ np.asarray(v, float)
    HR = np.array([8.2, 7.8, 9.0])

    def surf(v, out=0.0):
        """Punkt auf der Kopfoberflaeche in Richtung v (Kopfkoordinaten), um [out] nach aussen."""
        v = np.asarray(v, float)
        k = 1 / np.sqrt(((v / HR) ** 2).sum())
        p = v * k
        n = p / HR ** 2
        return head + H @ (p + n / np.linalg.norm(n) * out)
    m.ellipsoid(at([0, 0, 0]), (8.2, 7.8, 9.0), FUR, axes=H)
    # helle Wangen und Kinn
    for s in (1, -1):
        m.ellipsoid(at([0.6, -3.6, 6.6 * s]), (3.0, 3.0, 3.2), CREAM, axes=H)
    m.ellipsoid(at([7.4, -1.8, 0]), (5.0, 2.5, 2.9), FUR, axes=H)      # Schnauze
    m.ellipsoid(at([6.4, -3.6, 0]), (4.4, 1.9, 3.3), CREAM, axes=H)    # Kinn und Lefzen
    m.ellipsoid(at([5.4, -1.0, 0]), (4.0, 2.0, 2.0), FUR, axes=H)      # Nasenruecken
    m.ellipsoid(at([12.6, -1.6, 0]), (1.3, 1.15, 1.5), NOSE, axes=H)
    # Augen: Iris, Pupille, Glanz - flach auf der Kopfoberflaeche, geschlossen ein Strich
    def normal_at(v):
        v = np.asarray(v, float)
        p = v / np.sqrt(((v / HR) ** 2).sum())
        n = p / HR ** 2
        return H @ (n / np.linalg.norm(n))
    for s in (1, -1):
        v = [0.6, 0.14, 0.6 * s]
        e = surf(v, 0.45)
        n = normal_at(v)
        ax = frame_from(H @ np.array([0, 1.0, 0]) - n * (n @ (H @ np.array([0, 1.0, 0]))), n)   # quer, hoch, Normale
        if p.eyes > 0.3:
            h = 2.9 * p.eyes
            m.ellipsoid(e, (2.2, h, 1.1), IRIS, axes=ax)
            m.ellipsoid(e + n * 0.3 + ax[:, 1] * -0.2, (1.5, h * 0.74, 1.0), PUPIL, axes=ax)
            m.ellipsoid(e + n * 0.75 + ax[:, 1] * 1.0 + ax[:, 0] * -0.5 * s, (0.65, 0.75, 0.6), SPARK, axes=ax)
        else:
            m.ellipsoid(e + n * 0.2 + ax[:, 1] * -0.5, (2.2, 0.55, 0.7), PUPIL, axes=ax)
    # Ohren: dreieckig, leicht nach vorn-aussen gedreht (so sind sie auch von der Seite breit),
    # aussen Fell, innen rosa mit hellem Haarbueschel, nachschwingend
    tri = lambda u: np.abs(u[..., 0]) > np.clip(1.25 * np.clip(1 - (u[..., 1] + 1) / 2, 0, 1) ** 0.85, 0.05, 1)
    for s, tw in ((1, p.ear_twitch_l), (-1, p.ear_twitch_r)):
        base = at([-0.6, 5.0, 4.4 * s])
        eb = np.radians(p.ear_back + tw)
        sp = np.radians(24 + p.ear_spread)
        tip_dir = H @ np.array([-np.sin(eb), np.cos(eb) * np.cos(sp), np.cos(eb) * np.sin(sp) * s])
        tip = base + tip_dir * 24.0
        out = np.radians(50)
        nrm = H @ np.array([np.cos(eb) * np.cos(out), np.sin(eb), np.sin(out) * s])
        m.leaf(base - tip_dir * 2, tip, 14.0, 3.2, FUR, normal_hint=nrm, clip=tri)
        m.leaf(base - tip_dir * 0.5 + nrm * 1.0, base + nrm * 1.0 + tip_dir * 20.5, 9.5, 1.6, EARIN, normal_hint=nrm, clip=tri)
        m.leaf(base + nrm * 1.6 + tip_dir * 1.0, base + nrm * 1.6 + tip_dir * 9.0, 5.0, 1.0, CREAM, normal_hint=nrm, clip=tri)
    return m.ground()
