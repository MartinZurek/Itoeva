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


def wyrmling(p=None):
    s = Sprite(pose=p)
    # Fluegel hinten, rostrot, mit Spannrippen
    wing = s.poly([(22, 34), (10, 20), (13, 30), (6, 30), (12, 38), (22, 42)])
    s.part(wing, RUST, seed=1)
    for a, b in (((11, 22), (21, 35)), ((8, 30), (21, 38))):
        s.paint(s.pen().line([a, b]).m & wing, '#5e2418')
    # Schwanz, der sich am Boden ringelt
    tail = s.pen().line([(30, 58), (14, 58), (6, 54), (5, 48)], 5).m
    s.part(tail, DRAGON, seed=2)
    s.part(s.poly([(5, 47), (1, 43), (8, 44)]), RUST)
    # Koerper sitzend
    body = s.ellipse(33, 46, 12, 14)
    s.part(body, DRAGON, fur=0.25, seed=3)
    s.part(s.ellipse(37, 47, 6, 11) & body, BELLY, line=False)
    for y in range(40, 58, 3):
        s.paint((s.yy == y) & s.ellipse(37, 47, 6, 11), '#c4a87e')
    # Rueckenzacken
    for x, y in ((24, 35), (22, 41), (21, 47)):
        s.part(s.poly([(x, y - 3), (x - 4, y), (x, y + 2)]), RUST, line=False)
    # Beine und Arme
    s.part(s.ellipse(29, 58 - s.pose.foot_lift(0), 5, 2.5) | s.ellipse(42, 58 - s.pose.foot_lift(1), 4.5, 2.5), DRAGON)
    s.part(s.ellipse(43, 43, 2.5, 4.5, rot=-0.5), DRAGON)
    # Kopf mit Schnauze nach rechts
    # Hoerner und Kopfzacken hinter dem Kopf
    s.part(s.poly([(28, 18), (19, 6), (33, 15)]), RUST)
    s.part(s.poly([(37, 15), (39, 4), (42, 16)]), RUST)
    s.part(s.poly([(24, 22), (15, 20), (24, 28)]), RUST)
    head = s.ellipse(35, 25, 12, 10.5) | s.ellipse(46, 29, 8, 6)
    s.part(head, DRAGON, fur=0.2, seed=4)
    s.part(s.ellipse(46, 32, 7, 2.8) & head, BELLY, line=False)
    s.eye(38, 21, 5, 6, iris='#3a2008', low='#d08a1e')
    s.px(52, 28, '#1e2a14'), s.px(51, 27, '#1e2a14')
    s.px(46, 34, '#5a3a2a'), s.px(47, 34, '#5a3a2a'), s.px(48, 33, '#5a3a2a')
    s.px(43, 29, '#e89a7a'), s.px(44, 29, '#e89a7a')
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
