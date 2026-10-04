"""Die sechs Wesen von Itoeva, neu gezeichnet nach den Charakterstudien vom 04.10.
(docs/concept-art/character-art-studies/). Jede Figur 64 x 64 Pixel; der Bogen stellt sie auf Zeile 61 (sheets.py).
"""
import sys
import numpy as np
from PIL import Image
from sprite import Sprite, Pose

# Rampen dunkel -> hell, farblich verschoben (Schatten waermer/violetter, Licht gelblicher)
FUR_ORANGE = ['#7a3a22', '#a8542c', '#d07a3a', '#e89a52', '#f6bd78']
CREAM = ['#b88a6a', '#d8b08c', '#efd2b0', '#fbe8cf']
PINK_EAR = ['#c06a6a', '#e09088', '#f2b4a8']
DARK_PAW = ['#2e1c18', '#46302a', '#5e4436']
SCARF = ['#5a1e1c', '#7e2e26', '#a2442e', '#c06038']

MOSS = ['#3a5a2a', '#557a36', '#76993f', '#9cb85a', '#c4d884']
MOSS_DARK = ['#2c4422', '#3e5e2a', '#557a36']
SPROUT = ['#3e6a2a', '#6a9a3a', '#9cc860']


def fennec(p=None):
    s = Sprite(pose=p)
    # Schwanz hinten rechts, buschig mit heller Spitze
    tail = s.ellipse(47, 48, 12, 8, rot=-0.5) | s.ellipse(53, 40, 7, 6, rot=-0.5)
    s.part(tail, FUR_ORANGE, fur=0.5, seed=1)
    s.part(s.ellipse(56, 36, 5, 4.5, rot=-0.6) & tail, CREAM, fur=0.4, seed=2)
    # Koerper sitzend
    body = s.ellipse(31, 48, 12, 12)
    s.part(body, FUR_ORANGE, fur=0.45, seed=3)
    s.part(s.ellipse(31, 50, 7, 9) & body, CREAM, fur=0.35, seed=4, line=False)
    # Hinterpfoten
    for i, x in enumerate((24, 38)):
        s.part(s.ellipse(x, 58 - s.pose.foot_lift(i), 4.5, 2.6), DARK_PAW)
    # Ohren - riesig, das Erkennungszeichen
    le = s.ellipse(20, 21, 8, 9, rot=0.45) | s.poly([(13, 18), (8, 1), (27, 15)])
    re = s.ellipse(43, 21, 8, 9, rot=-0.45) | s.poly([(36, 15), (55, 1), (50, 18)])
    s.part(le, FUR_ORANGE, fur=0.3, seed=5)
    s.part(re, FUR_ORANGE, fur=0.3, seed=6)
    s.part(s.ellipse(20, 20, 4.5, 6, rot=0.45) | s.poly([(16, 17), (11, 5), (24, 15)]), PINK_EAR, line=False)
    s.part(s.ellipse(43, 20, 4.5, 6, rot=-0.45) | s.poly([(39, 15), (52, 5), (47, 17)]), PINK_EAR, line=False)
    # helle Haarbueschel im Ohr
    for x, y in ((19, 22), (20, 24), (44, 22), (43, 24)):
        s.px(x, y, '#fbe8cf')
    # Kopf mit Wangenbueschel
    head = s.ellipse(31.5, 30, 12.5, 10) | s.poly([(19, 31), (15, 36), (22, 35)]) | s.poly([(44, 31), (48, 36), (41, 35)])
    s.part(head, FUR_ORANGE, fur=0.3, seed=7)
    muzzle = s.ellipse(31.5, 34.5, 7.5, 4.5) | s.ellipse(25, 33, 4, 3) | s.ellipse(38, 33, 4, 3)
    s.part(muzzle & head, CREAM, line=False, lift=0.15)
    # Schal mit Muster und Zipfel
    scarf = s.ellipse(31.5, 40.5, 11, 3.2) | s.poly([(36, 41), (41, 41), (40, 50), (35, 49)])
    s.part(scarf, SCARF)
    for x in range(23, 41, 3):
        s.px(x, 40, '#d88a4a')
    s.px(36, 47, '#d88a4a'), s.px(38, 45, '#d88a4a')
    # Vorderpfoten halten zusammen
    if s.pose.joy:
        s.part(s.ellipse(17, 39, 3, 3.5) | s.ellipse(46, 39, 3, 3.5), CREAM)
    else:
        s.part(s.ellipse(27, 51, 3.2, 3) | s.ellipse(36, 51, 3.2, 3), CREAM)
    # Gesicht
    s.eye(24, 27, 5, 6, iris='#2a1610', low='#6a3a22')
    s.eye(34, 27, 5, 6, iris='#2a1610', low='#6a3a22')
    s.paint(s.ellipse(31.5, 33, 1.6, 1.1), '#2a1610')
    s.px(30, 36, '#7a3a2a'), s.px(31, 37, '#7a3a2a'), s.px(32, 37, '#7a3a2a'), s.px(33, 36, '#7a3a2a')
    for x, y in ((22, 33), (23, 33), (40, 33), (41, 33)):
        s.px(x, y, '#f0928a')
    s.outline()
    return s


def gloop(p=None):
    s = Sprite(pose=p)
    # Weicher Tropfen, unten breit auslaufend
    body = s.ellipse(32, 42, 17, 16) | s.ellipse(32, 54, 22, 7)
    body &= s.yy <= 61
    s.part(body, MOSS, bulge=1.3, seed=1)
    # Glanz der feuchten Oberflaeche: zwei helle Bogen oben links
    shine = s.ellipse(24, 34, 4, 6, rot=0.5) & ~s.ellipse(25.5, 35, 3, 5.5, rot=0.5)
    s.paint(shine & body, '#e2eeb0')
    s.px(21, 30, '#f4f8d8'), s.px(22, 29, '#f4f8d8')
    # Moosflecken mit Struktur
    rng = np.random.default_rng(4)
    moss = np.zeros_like(body)
    for cx, cy, r in ((38, 30, 5), (44, 40, 4), (27, 49, 3.5), (46, 53, 4), (33, 27, 3)):
        moss |= s.ellipse(cx, cy, r, r * 0.8)
    moss &= body
    tex = rng.random(moss.shape) > 0.35
    s.part(moss & tex, MOSS_DARK, line=False, fur=0.6, seed=5)
    # Spross oben
    stem = s.pen().line([(33, 27), (34, 21), (35, 17)], 1).m
    s.paint(stem, '#4e7a2e')
    tilt = 0.9 if s.pose.joy else 0.4
    s.part(s.ellipse(29.5, 16 - (2 if s.pose.joy else 0), 5, 2.6, rot=tilt), SPROUT)
    s.part(s.ellipse(40, 15 - (2 if s.pose.joy else 0), 5, 2.6, rot=-tilt), SPROUT)
    # Gesicht
    s.eye(24, 37, 5, 6, iris='#1e2414', low='#3e5226')
    s.eye(35, 37, 5, 6, iris='#1e2414', low='#3e5226')
    for x in (23, 24, 41, 42):
        s.px(x, 45, '#e8a08a')
    s.px(31, 46, '#3a4a24'), s.px(32, 46, '#3a4a24')
    s.outline()
    return s


GOLD = ['#9a5a1c', '#c8802a', '#eaac3c', '#f7cc5a', '#fff0a2']
CLOUD = ['#a89888', '#d2c4ae', '#ece2ce', '#fbf6ea', '#ffffff']
CLOUD_BLUE = ['#46669a', '#6a8cc0', '#93b2da', '#bcd2ee']
SATCHEL = ['#4a2c1a', '#6e4428', '#946038', '#b47e4c']
DRAGON = ['#2c4630', '#43623c', '#5e7f4e', '#7fa268', '#a6c28a']
RUST = ['#5e2418', '#8c3c24', '#b85a34', '#d8804c']
BELLY = ['#a88a62', '#cdb488', '#e8d6aa', '#f6ead0']
VIOLET = ['#382a58', '#523f7c', '#7260a2', '#9584c4', '#bcaee0']
CLOAK = ['#2a2048', '#3e3068', '#584894', '#7464b0']
FACE = ['#bca488', '#dcc8ae', '#f2e6d4', '#fdf8ee']
BEAK = ['#8a4a1a', '#c8782a', '#eea040']


def soft(s, mask, r=1.2):
    """Rundet harte Spitzen einer Form ab (fuer Stern und Wolke)."""
    from scipy import ndimage
    return ndimage.gaussian_filter(mask.astype(float), r) > 0.5


def starlet(p=None):
    s = Sprite(pose=p)
    cx, cy = 32, 37
    pts = []
    for k in range(10):
        a = -np.pi / 2 + k * np.pi / 5
        r = 25 if k % 2 == 0 else 12.5
        pts.append((cx + np.cos(a) * r * 1.02, cy + np.sin(a) * r))
    star = soft(s, s.poly(pts), 1.6)
    s.part(star, GOLD, bulge=1.4)
    # Funkeln um sie herum
    sparks = ((8, 14), (55, 12), (58, 30)) + (((4, 34), (30, 5), (60, 48)) if s.pose.joy else ())
    for x, y in sparks:
        s.px(x, y, '#fff6c0')
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            s.px(x + dx, y + dy, '#f0c860')
    s.eye(24, 34, 5, 7, iris='#3a2214', low='#8a5428')
    s.eye(35, 34, 5, 7, iris='#3a2214', low='#8a5428')
    for x in (21, 22, 42, 43):
        s.px(x, 42, '#f08a6a')
    s.px(30, 43, '#8a4a20'), s.px(31, 44, '#8a4a20'), s.px(32, 44, '#8a4a20'), s.px(33, 43, '#8a4a20')
    s.outline()
    return s


def puffling(p=None):
    s = Sprite(pose=p)
    # blaue Tupfen hinten
    back = s.ellipse(17, 26, 7, 6) | s.ellipse(29, 17, 8, 6) | s.ellipse(43, 20, 7, 6) | s.ellipse(50, 32, 5, 7)
    s.part(soft(s, back), CLOUD_BLUE, fur=0.6, seed=1)
    # Wolkenkoerper aus Bauschen
    cloud = np.zeros((64, 64), bool)
    for x, y, r in ((32, 39, 16), (18, 42, 9), (46, 42, 9), (25, 28, 10), (39, 28, 10), (32, 49, 12), (21, 51, 7), (43, 51, 7), (12, 37, 5), (52, 39, 5)):
        cloud |= s.ellipse(x, y, r, r * 0.95)
    s.part(soft(s, cloud), CLOUD, fur=0.7, seed=2)
    # blaue Tupfen vorn an den Seiten
    s.part(soft(s, s.ellipse(11, 44, 3.5, 4.5) | s.ellipse(53, 45, 3.5, 4.5) | s.ellipse(22, 23, 4, 3)), CLOUD_BLUE, fur=0.5, seed=3)
    # Fuesschen
    for i, x in enumerate((25, 39)):
        s.part(s.ellipse(x, 59 - s.pose.foot_lift(i), 3.5, 2.2), CLOUD_BLUE)
    # Umhaengetasche mit Riemen und Blatt
    strap = s.pen().line([(42, 33), (36, 45)], 1).m
    s.paint(strap, '#6e4428')
    s.part((s.xx >= 27) & (s.xx <= 37) & (s.yy >= 44) & (s.yy <= 51), SATCHEL)
    s.paint((s.xx >= 27) & (s.xx <= 37) & (s.yy == 44), '#c8925a')
    s.px(32, 47, '#e8c070')
    s.part(s.ellipse(25, 47, 3, 1.8, rot=-0.6), ['#3e6a2a', '#6a9a3a', '#9cc860'])
    s.eye(24, 31, 4, 6, iris='#1e2a48', low='#4a6a9a')
    s.eye(36, 31, 4, 6, iris='#1e2a48', low='#4a6a9a')
    for x in (21, 22, 42, 43):
        s.px(x, 38, '#f0a8a0')
    s.px(31, 39, '#8a6a5a'), s.px(32, 39, '#8a6a5a')
    s.outline(strength=0.55)
    return s


# Wyrmling nach der Fantasy-Studie vom 04.10. (character-conceptbook/briefs/wyrmling.md):
# Startpalette #354C43 #607C57 #98AC72 #D2CA92 #875B43 #BC7D51 #D9A06A #EEE0B1.
JADE = ['#2c4038', '#3f5a48', '#5a7652', '#7c955f', '#9cb075']
JADE_SPOT = '#b2c084'
WING = ['#7a3a26', '#a8583a', '#cf7e4c', '#e8a46c']
HORN = ['#5a3a28', '#875b43', '#a8724a']
CREAM_BELLY = ['#a8986a', '#cfc48e', '#e8dcac', '#f2e8c4']
WRAP = ['#a69a7c', '#cfc3a2', '#eee0b1']


def _wyrm_wing(s, shoulder, wrist, tips, back=False):
    """Ein Fledermausfluegel als getrennte Flaeche: Oberarm zur Handwurzel, von dort Fingerknochen
    zu den Spitzen, dazwischen die Haut in Bogen bis zurueck an den Koerper. Der hintere Fluegel
    ist dunkler, damit beide getrennt lesbar bleiben."""
    sx, sy = shoulder
    wx, wy = wrist
    pts = [(sx, sy), (wx, wy)]
    for k, (tx, ty) in enumerate(tips):
        pts.append((tx, ty))
        nxt = tips[k + 1] if k + 1 < len(tips) else (sx - 2, sy + 9)
        # Bogen der Haut zwischen zwei Fingern: nach innen eingezogen
        pts.append(((tx + nxt[0]) / 2 + 2.5, (ty + nxt[1]) / 2 + 0.5))
    pts.append((sx - 2, sy + 9))
    m = s.poly(pts)
    s.part(m, WING[:3] if back else WING, seed=31 if back else 32)
    bone = '#4e2a1e' if back else '#6a3a28'
    for tx, ty in tips:
        s.paint(s.pen().line([(wx, wy), (tx, ty)]).m & m, bone)
    s.paint(s.pen().line([(sx, sy), (wx, wy)], 2).m, HORN[1] if back else HORN[2])
    s.px(wx, wy, '#d9a06a')
    return m


def wyrmling(p=None):
    s = Sprite(pose=p)
    if s.pose.eyes == 'closed' and getattr(s.pose, 'sleep', False):
        return _wyrmling_asleep(s)
    joy = s.pose.joy
    # Fluegel hinter dem Koerper, durch Freiraum vom Ruecken getrennt
    if joy:
        # gestreckt: Fluegel als Balance weit nach oben geoeffnet
        _wyrm_wing(s, (31, 33), (24, 6), [(19, 3), (14, 9), (13, 17)], back=True)
        _wyrm_wing(s, (26, 35), (13, 10), [(4, 9), (1, 19), (5, 28), (13, 34)])
    else:
        _wyrm_wing(s, (29, 34), (21, 16), [(16, 13), (12, 19), (13, 27)], back=True)
        _wyrm_wing(s, (25, 36), (13, 21), [(4, 21), (2, 30), (6, 37), (14, 42)])
    # Schwanz: am Boden nach links, Spitze hochgebogen, kleine Kammzacken
    tail = s.pen().line([(28, 57), (16, 58), (8, 55), (4, 49)], 5).m | s.pen().line([(5, 49), (6, 45)], 3).m
    s.part(tail, JADE, seed=2)
    for x, y in ((20, 54), (13, 53), (7, 49)):
        s.part(s.poly([(x - 2, y + 1), (x, y - 3), (x + 2, y + 1)]), WING[1:], line=False)
    s.part(s.poly([(4, 45), (7, 40), (9, 45)]), WING[1:])
    # Hinterbeine (vor dem Schwanz)
    for i, (x, w) in enumerate(((29, 5), (40, 4.5))):
        lift = s.pose.foot_lift(i)
        s.part(s.ellipse(x, 54 - lift, w, 5), JADE, seed=5 + i)
        s.part(s.ellipse(x + 1, 59 - lift, w + 1, 2.2), JADE[:3])
        for k in (0, 2):
            s.px(x + int(w) - 1 + k, 60 - lift, '#2c4038')
    # Rumpf: aufrecht, birnenfoermig, mit hellem Bauch nach vorn
    body = s.ellipse(34, 45, 10.5, 12.5)
    s.part(body, JADE, fur=0.2, seed=3)
    belly = s.ellipse(38, 47, 5.5, 10) & body
    s.part(belly, CREAM_BELLY, line=False)
    for y in range(41, 57, 3):
        s.paint(belly & (s.yy == y), '#bfb07e')
    # Flecken auf den Schuppen: wenige grosse statt Rauschen
    for x, y in ((27, 42), (30, 48), (26, 50), (31, 39)):
        s.paint(s.ellipse(x, y, 1.6, 1.2) & body & ~belly, JADE_SPOT)
    # Arm, vom Rumpf durch die Trennlinie abgesetzt
    arm_y = 40 if joy else 44
    arm = s.ellipse(43, arm_y, 2.6, 4.6, rot=-0.6 if not joy else 0.6)
    s.part(arm, JADE, seed=9)
    s.px(45, arm_y + 3, '#ded6b0') if not joy else s.px(45, arm_y - 4, '#ded6b0')
    # Kopf: gross, rund, Schnauze nach rechts
    # Kammfransen hinter dem Kopf: drei breite rostrote Blaetter
    for (x0, y0), (x1, y1), (x2, y2) in (((25, 18), (19, 12), (31, 16)), ((24, 23), (17, 21), (26, 28)),
                                         ((29, 15), (27, 8), (35, 15))):
        s.part(s.poly([(x0, y0), (x1, y1), (x2, y2), ((x0 + x2) / 2 + 1, (y0 + y2) / 2 + 2)]), WING[1:])
    # Zwei kurze, nach hinten gebogene Hoerner mit runder Spitze
    s.part(s.poly([(33, 17), (30, 9), (31, 5), (34, 6), (38, 15)]), HORN)
    s.part(s.poly([(39, 16), (38, 7), (40, 4), (43, 6), (44, 16)]), HORN)
    head = s.ellipse(37, 25, 11.5, 10) | s.ellipse(48, 28.5, 7.5, 5.5)
    s.part(head, JADE, fur=0.15, seed=4, lift=0.08)
    s.part((s.ellipse(47, 32, 6.5, 2) | s.ellipse(40, 33, 4, 2.5)) & head, CREAM_BELLY, line=False)
    for x, y in ((31, 20), (34, 18), (29, 25)):
        s.paint(s.ellipse(x, y, 1.3, 1) & head, JADE_SPOT)
    # Halstuch mit kupfernem Riemen und mattem Bernsteinanhaenger
    wrap = s.ellipse(37, 37, 9.5, 3) | s.poly([(30, 37), (27, 43), (31, 42)])
    s.part(wrap, ['#9a8c6c', '#c8b88e', '#dccb9e'])
    s.paint(s.pen().line([(28, 39), (45, 39)]).m & s.ellipse(37, 38, 10, 4), '#875b43')
    s.part(s.ellipse(41, 42, 2, 2.2), ['#875b43', '#bc7d51', '#d9a06a'], line=False)
    s.px(40, 41, '#f4d494')
    # Gesicht
    s.eye(39, 21, 5, 6, iris='#3a2008', low='#d08a1e')
    s.px(54, 27, '#1e2a14'), s.px(53, 27, '#1e2a14')
    if s.pose.eyes == 'happy':
        s.paint(s.pen().line([(46, 33), (49, 34), (52, 32)]).m, '#5a3a2a')
    else:
        s.paint(s.pen().line([(47, 33), (50, 33), (52, 32)]).m, '#5a3a2a')
    s.px(44, 29, '#e89a7a'), s.px(45, 29, '#e89a7a')
    s.outline()
    return s


def _wyrmling_asleep(s):
    """Eingerollt schlafend wie auf der Tafel: Koerper liegend, Fluegel als Decke, Schwanz ums
    Gesicht gelegt. Eigener Bodenanker - die Fuesse stehen nicht, der Bauch liegt auf."""
    body = s.ellipse(31, 52, 19, 9)
    s.part(body, JADE, fur=0.2, seed=13)
    wing = s.poly([(14, 50), (22, 38), (36, 36), (48, 44), (44, 48), (32, 46), (22, 50)])
    s.part(wing, WING, seed=14)
    for b in (((22, 39), (28, 47)), ((36, 37), (36, 46))):
        s.paint(s.pen().line(b).m & wing, '#6a3a28')
    tail = s.pen().line([(13, 56), (24, 60), (44, 60), (54, 57)], 4).m
    s.part(tail, JADE, seed=15)
    s.part(s.poly([(54, 55), (59, 54), (56, 58)]), WING[1:])
    head = s.ellipse(47, 50, 8.5, 6.5) | s.ellipse(54, 53, 5, 4)
    s.part(head, JADE, seed=16, lift=0.08)
    s.part(s.poly([(43, 45), (39, 38), (46, 44)]), HORN)
    s.part(s.poly([(48, 44), (49, 37), (51, 45)]), HORN)
    s.part(s.ellipse(54, 55.5, 4.5, 1.6) & head, CREAM_BELLY, line=False)
    s.eye(47, 48, 5, 4)          # geschlossen (Pose.eyes == 'closed')
    s.paint(s.ellipse(40, 55, 4, 2) & body, WRAP[2])
    s.outline()
    return s


def hootlet(p=None):
    s = Sprite(pose=p)
    # Koerper rund
    body = s.ellipse(32, 41, 16, 18)
    s.part(body, VIOLET, fur=0.5, seed=1)
    if s.pose.joy:
        s.part(s.ellipse(12, 36, 6, 9, rot=0.9) | s.ellipse(52, 36, 6, 9, rot=-0.9), VIOLET, fur=0.4, seed=9)
    # Federohren
    s.part(soft(s, s.poly([(17, 27), (12, 16), (17, 18), (19, 14), (25, 25)]), 0.8), VIOLET, fur=0.4, seed=2)
    s.part(soft(s, s.poly([(47, 27), (52, 16), (47, 18), (45, 14), (39, 25)]), 0.8), VIOLET, fur=0.4, seed=3)
    # helle Brust
    s.part(s.ellipse(32, 50, 9, 8) & body, FACE, fur=0.4, seed=4, line=False)
    for y in (47, 50, 53):
        for x in range(27, 38, 3):
            s.px(x + (y % 2), y, '#c8b098')
    # Umhang
    cloak = (s.ellipse(32, 46, 17, 14) & ~s.ellipse(32, 52, 10, 12)) & (s.yy >= 38)
    s.part(cloak, CLOAK, seed=5)
    s.paint(s.ellipse(32, 39, 2.2, 2.2), '#e8b440')
    s.px(31, 38, '#fff0b0')
    # Gesichtsscheibe
    face = s.ellipse(25, 31, 8.5, 8) | s.ellipse(39, 31, 8.5, 8) | s.ellipse(32, 36, 6, 4)
    s.part(face, FACE, line=True, lift=0.25)

    # Brille: runde goldene Ringe
    for cx in (25, 39):
        ring = s.ellipse(cx, 30, 6.5, 6.5) & ~s.ellipse(cx, 30, 5.2, 5.2)
        s.paint(ring, '#c89a3a')
        s.paint(ring & (s.yy < 28) & (s.xx < cx), '#f0d070')
    s.paint((s.yy == 29) & (s.xx >= 31) & (s.xx <= 33), '#c89a3a')
    s.eye(22, 27, 5, 6, iris='#2a1e14', low='#7a5228')
    s.eye(37, 27, 5, 6, iris='#2a1e14', low='#7a5228')
    # Schnabel
    s.part(s.poly([(30, 34), (34, 34), (32, 39)]), BEAK, line=False)
    # Fuesse
    for i, x in enumerate((26, 38)):
        s.part(s.ellipse(x, 59 - s.pose.foot_lift(i), 3.5, 2), BEAK)
    s.outline()
    return s


def sheet(figs, scale=6, bg=(232, 222, 200)):
    w = sum(f.w for f in figs) + 8 * (len(figs) + 1)
    out = Image.new('RGBA', (w, 64 + 16), bg + (255,))
    x = 8
    for f in figs:
        out.alpha_composite(f.image(), (x, 8))
        x += f.w + 8
    return out.resize((out.width * scale, out.height * scale), Image.NEAREST)


if __name__ == '__main__':
    path = sys.argv[1] if len(sys.argv) > 1 else 'chars.png'
    sheet([fennec(), gloop(), starlet(), puffling(), wyrmling(), hootlet()]).save(path)
