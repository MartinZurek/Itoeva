"""Bewegung fuer die 3D-Wesen: Gang, Ruhe, Sprung, Schlaf, Drehung - mit Nachschwingen.

Die Hauptbewegung (Beine, Huefte, Arme) folgt einem Gangzyklus. Ohren, Schwanz und Umhang sind
gedaempfte Federn, die von der Beschleunigung des Koerpers angetrieben werden: Sackt der Koerper
beim Auftreten ab, schwingen die Ohren nach, die Schwanzglieder folgen einander mit Verzoegerung,
der Umhang wird vom Fahrtwind nach hinten gedrueckt und flattert. Die Federn laufen ueber mehrere
Zyklen, bis sie eingeschwungen sind; gezeigt wird der letzte, damit die Schleife nahtlos ist.

Bogen (128 x 128 je Bild, Reihenfolge muss zu CreatureSprites.kt passen):
  0..7  Ruhe (Atmen, Ohrzucken, Schwanzwiegen)   8 Blinzeln
  9..16 Gehen seitlich                           17..22 Sprung (Ausholen .. Landung)
  23..26 Schlaf                                  27 vorn, 28..31 vorn gehen
  32 hinten, 33..36 hinten gehen                 37 Drehung Seite->vorn, 38 Seite->hinten
"""
import numpy as np
from dataclasses import replace

IDLE, BLINK, WALK, JOY, SLEEP, FRONT, FRONT_WALK, BACK, BACK_WALK, TURN_FRONT, TURN_BACK = \
    0, 8, 9, 17, 23, 27, 28, 32, 33, 37, 38
COUNT = 39


def spring(drive, k=60.0, c=7.0, cycles=4):
    """Gedaempfte Feder ueber ein periodisches Antriebssignal (ein Wert je Zeitschritt eines
    Zyklus). Gibt die Auslenkung im letzten Zyklus zurueck."""
    n = len(drive)
    dt = 1.0 / n
    x = v = 0.0
    out = np.zeros(n)
    for cyc in range(cycles):
        for i in range(n):
            a = drive[i] - k * x - c * v
            v += a * dt
            x += v * dt
            if cyc == cycles - 1:
                out[i] = x
    return out


def chain(drive, links, lag=0.11, gain=1.0, decay=0.85, **kw):
    """Schwanzglieder: jedes folgt dem vorigen mit Verzoegerung; aussen schwingt es weiter."""
    n = len(drive)
    res = []
    sig = drive
    for i in range(links):
        r = spring(sig, **kw)
        shift = int(round(lag * n))
        r = np.roll(r, shift)
        res.append(r * gain)
        sig = r * n * decay
        gain *= 1.0
    return res


def walk_poses(Pose, frames=8, speed=1.0, steps=64):
    """Seitlicher Gang: Zwei Schritte je Zyklus."""
    ph = 2 * np.pi * np.arange(steps) / steps
    bob = 0.9 * np.cos(2 * ph) - 0.3
    acc = np.gradient(np.gradient(bob)) * steps * steps / 40.0     # senkrechte Beschleunigung
    ear = spring(-acc * 60, k=90, c=9)
    tails = chain(-acc * 25, 8, lag=0.06, k=70, c=8)
    sways = chain(np.sin(ph) * 30, 8, lag=0.05, k=50, c=6)
    cape = spring(np.cos(2 * ph - 0.4) * 140, k=110, c=10)
    out = []
    for f in range(frames):
        i = f * steps // frames
        p = ph[i]
        sw_l, sw_r = max(0.0, np.cos(p)), max(0.0, np.cos(p + np.pi))
        out.append(Pose(
            bob=bob[i], lean=7 + 1.5 * np.sin(2 * p),
            hip_l=30 * np.sin(p), hip_r=-30 * np.sin(p),
            knee_l=6 + 46 * sw_l ** 1.4, knee_r=6 + 46 * sw_r ** 1.4,
            foot_lift_l=2.2 * sw_l ** 2, foot_lift_r=2.2 * sw_r ** 2,
            arm_l=-24 * np.sin(p), arm_r=24 * np.sin(p),
            elbow_l=40 + 12 * max(0, -np.sin(p)), elbow_r=40 + 12 * max(0, np.sin(p)),
            arm_out=14,
            head_pitch=-2.5 * np.cos(2 * p - 0.7),
            ear_back=16 * speed + ear[i] * 0.6,
            tail=[-6 + tails[j][i] * 0.7 for j in range(8)],
            tail_side=[sways[j][i] * 0.5 for j in range(8)],
            cape=24 * speed + cape[i] * 0.5,
            breath=0.5,
        ))
    return out


def idle_poses(Pose, frames=8, steps=64):
    ph = 2 * np.pi * np.arange(steps) / steps
    breath = 0.5 + 0.5 * np.sin(ph)
    # Ohrzucken: kurzer Stoss bei 60 % des Zyklus, die Feder macht Ausschlag und Nachwippen
    kick = np.zeros(steps)
    kick[int(steps * 0.6)] = 2400
    twitch = spring(kick, k=260, c=14)
    sway = chain(np.sin(ph) * 22, 8, lag=0.07, k=40, c=6)
    out = []
    for f in range(frames):
        i = f * steps // frames
        out.append(Pose(
            breath=breath[i], bob=0.25 * np.sin(ph[i]),
            head_tilt=2.0 * np.sin(ph[i] + 0.8), head_pitch=1.0 * np.sin(ph[i]),
            ear_twitch_l=twitch[i] * 0.5,
            arm_l=2 * np.sin(ph[i]), arm_r=2 * np.sin(ph[i] + 0.4),
            tail=[1.5 * np.sin(ph[i] - j * 0.35) for j in range(8)],
            tail_side=[-6 + sway[j][i] * 0.6 for j in range(8)],
            cape=3 + 2 * np.sin(ph[i] - 0.6),
        ))
    return out


def joy_poses(Pose):
    """Ausholen, Absprung, Steigen, Scheitel, Fallen, Landung - Teile folgen mit Verzoegerung."""
    return [
        Pose(squash=0.7, bob=-3.5, lean=14, hip_l=40, hip_r=40, knee_l=70, knee_r=70,
             arm_l=-40, arm_r=-40, elbow_l=20, elbow_r=20, ear_back=8, head_pitch=-6,
             tail=[-14] * 8, cape=10, eyes=0.9),
        Pose(squash=-0.6, bob=1.5, lean=-4, hip_l=-6, hip_r=-10, knee_l=4, knee_r=6,
             arm_l=150, arm_r=140, elbow_l=10, elbow_r=14, ear_back=30, ear_spread=-6, head_pitch=8,
             tail=[-18 + 2 * j for j in range(8)], cape=-6, cape_lift=-1, foot_lift_l=1, foot_lift_r=1),
        Pose(squash=-0.3, bob=1.0, lean=0, hip_l=20, hip_r=8, knee_l=40, knee_r=30,
             arm_l=165, arm_r=160, elbow_l=20, elbow_r=20, ear_back=14, head_pitch=10,
             tail=[-6 + 3 * j for j in range(8)], cape=-14, cape_lift=3, foot_lift_l=2, foot_lift_r=2),
        Pose(squash=0.1, bob=0.5, lean=4, hip_l=45, hip_r=35, knee_l=75, knee_r=70,
             arm_l=120, arm_r=130, elbow_l=40, elbow_r=40, ear_back=-4, ear_spread=8, head_pitch=6,
             tail=[6 + 2 * j for j in range(8)], cape=-26, cape_lift=7, foot_lift_l=3, foot_lift_r=3),
        Pose(squash=-0.25, bob=0.5, lean=2, hip_l=12, hip_r=4, knee_l=24, knee_r=18,
             arm_l=80, arm_r=95, elbow_l=50, elbow_r=50, ear_back=-12, ear_spread=10, head_pitch=0,
             tail=[12 + 2 * j for j in range(8)], cape=-34, cape_lift=9, foot_lift_l=1, foot_lift_r=1),
        Pose(squash=0.6, bob=-3.0, lean=12, hip_l=36, hip_r=40, knee_l=62, knee_r=66,
             arm_l=20, arm_r=30, elbow_l=50, elbow_r=50, ear_back=-8, ear_spread=4, head_pitch=-8,
             tail=[-10 - j for j in range(8)], cape=4, cape_lift=2, eyes=0.8),
    ]


def sleep_poses(Pose, frames=4):
    out = []
    for f in range(frames):
        b = 0.5 + 0.5 * np.sin(2 * np.pi * f / frames)
        out.append(Pose(
            sit=True, breath=b, bob=0.4 * b, lean=26, head_pitch=-28 - 2 * b, head_tilt=8,
            hip_l=88, hip_r=84, knee_l=150, knee_r=152, stance=16,
            arm_l=40, arm_r=46, elbow_l=80, elbow_r=84, arm_out=10,
            ear_back=58, ear_spread=30, eyes=0.0,
            tail=[-6, -10, -12, -14, -14, -12, -10, -8],
            tail_side=[40, 30, 26, 22, 20, 18, 16, 14], cape=8,
        ))
    return out


def sheet_plan(Pose):
    """(Pose, Blickwinkel) je Bild, in Bogen-Reihenfolge."""
    idle = idle_poses(Pose)
    walk = walk_poses(Pose)
    plan = [(p, 90) for p in idle]
    plan.append((replace(idle[0], eyes=0.0), 90))
    plan += [(p, 90) for p in walk]
    plan += [(p, 90) for p in joy_poses(Pose)]
    plan += [(p, 72) for p in sleep_poses(Pose)]
    plan.append((idle[0], 0))
    plan += [(walk[k], 0) for k in (0, 2, 4, 6)]
    plan.append((idle[0], 180))
    plan += [(walk[k], 180) for k in (0, 2, 4, 6)]
    plan.append((idle[0], 45))
    plan.append((idle[0], 135))
    assert len(plan) == COUNT
    return plan
