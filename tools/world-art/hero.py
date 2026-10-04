"""Die Spielfigur von Itoeva 2: ein kleiner Fuchs mit Rucksack (Vorbild: die Tierkinder mit
Rucksaecken auf dem Konzeptbild). Ein Bogen mit Bildern zu je FW x FH Pixeln:

  Zeile 0: Seite (nach rechts; nach links wird gespiegelt) - Stand, dann 4 Schritte
  Zeile 1: von vorn (nach unten gehen)                       - Stand, dann 4 Schritte
  Zeile 2: von hinten (nach oben gehen)                      - Stand, dann 4 Schritte
  Zeile 3: Freude (von vorn)                                  - 4 Bilder
"""
import sys
import numpy as np
from PIL import Image, ImageDraw

FW, FH = 40, 56
COLS, ROWS = 5, 4


def rgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


P = {
    'fur': '#e8823a', 'fur_l': '#ffb36a', 'fur_d': '#b0502a', 'fur_dd': '#7a3220',
    'cream': '#fbe6c8', 'cream_d': '#e0bc98',
    'tunic': '#2f7a8a', 'tunic_l': '#4aa8b0', 'tunic_d': '#1e5262',
    'scarf': '#d0454a', 'scarf_l': '#f07070', 'scarf_d': '#902a38',
    'pack': '#8a5a3a', 'pack_l': '#b07a4a', 'pack_d': '#5a3824', 'strap': '#4a2c1e',
    'boot': '#4a2c22', 'boot_l': '#6a4232',
    'eye': '#20141a', 'shine': '#ffffff', 'nose': '#3a1e1e', 'blush': '#f08a7a',
}
C = {k: rgb(v) for k, v in P.items()}


class Frame:
    def __init__(self):
        self.im = Image.new('RGBA', (FW, FH), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.im)

    def e(self, box, c):
        self.d.ellipse(box, fill=C[c])

    def r(self, box, c):
        self.d.rectangle(box, fill=C[c])

    def p(self, pts, c):
        self.d.polygon(pts, fill=C[c])

    def px(self, x, y, c):
        if 0 <= x < FW and 0 <= y < FH:
            self.im.putpixel((int(x), int(y)), C[c])

    def outline(self):
        """Selektive Kontur: jeder leere Nachbar eines Pixels bekommt dessen dunklere Fassung."""
        a = np.array(self.im)
        alpha = a[..., 3] > 0
        out = a.copy()
        for dy, dx in ((0, 1), (0, -1), (1, 0), (-1, 0)):
            sh = np.roll(np.roll(alpha, dy, 0), dx, 1)
            src = np.roll(np.roll(a, dy, 0), dx, 1)
            edge = sh & ~alpha & (out[..., 3] == 0)
            col = (src[..., :3].astype(float) * 0.38 + np.array([20, 8, 24]) * 0.62 * 0.5).astype(np.uint8)
            out[edge, :3] = col[edge]
            out[edge, 3] = 255
        self.im = Image.fromarray(out)


GROUND = FH - 2   # Fusszeile


def side(step, bob):
    """Seitenansicht nach rechts. step 0 = Stand, 1..4 = Schrittphasen."""
    f = Frame()
    y0 = bob
    # Schwanz hinten (links), schwingt mit
    sw = [0, -1, 0, 1, 0][step]
    f.p([(13, 35 + y0), (3, 30 + y0 + sw), (1, 24 + y0 + sw), (5, 22 + y0 + sw), (12, 30 + y0)], 'fur')
    f.p([(3, 27 + y0 + sw), (1, 24 + y0 + sw), (4, 22 + y0 + sw), (6, 24 + y0 + sw)], 'cream')
    f.p([(13, 35 + y0), (6, 32 + y0 + sw), (12, 31 + y0)], 'fur_d')
    # Beine: hinteres zuerst
    lf = [(0, 0), (3, -2), (1, 0), (-3, -2), (-1, 0)][step]
    for (dx, lift), shade in (((-lf[0], lf[1] if step in (3, 4) else 0), 'fur_d'), ((lf[0], lf[1] if step in (1, 2) else 0), 'fur')):
        x = 20 + dx
        f.r((x - 2, 44 + y0, x + 1, GROUND - 3 + lift), shade)
        f.r((x - 3, GROUND - 3 + lift, x + 3, GROUND + lift), 'boot')
        f.r((x - 3, GROUND - 3 + lift, x + 3, GROUND - 3 + lift), 'boot_l')
    # Rucksack (hinten)
    f.r((8, 27 + y0, 16, 41 + y0), 'pack')
    f.r((8, 27 + y0, 16, 28 + y0), 'pack_l')
    f.r((9, 32 + y0, 15, 33 + y0), 'pack_d')
    f.r((8, 38 + y0, 16, 41 + y0), 'pack_d')
    f.px(12, 35 + y0, 'pack_l')
    # Koerper / Tunika
    f.e((13, 28 + y0, 28, 47 + y0), 'tunic')
    f.e((19, 29 + y0, 27, 44 + y0), 'tunic_l')
    f.r((14, 44 + y0, 27, 46 + y0), 'tunic_d')
    f.r((15, 32 + y0, 17, 42 + y0), 'strap')
    # Arm schwingt gegen das Bein
    ax = [0, -3, -1, 3, 1][step]
    f.p([(21, 33 + y0), (24, 33 + y0), (24 + ax, 41 + y0), (21 + ax, 41 + y0)], 'fur')
    f.e((20 + ax, 40 + y0, 25 + ax, 44 + y0), 'fur_l')
    # Schal
    f.e((15, 26 + y0, 29, 32 + y0), 'scarf')
    f.r((16, 26 + y0, 28, 27 + y0), 'scarf_l')
    f.p([(16, 30 + y0), (11, 36 + y0 - sw), (14, 37 + y0 - sw), (19, 31 + y0)], 'scarf_d')
    # Kopf: gross, rund, Schnauze nach rechts
    f.e((12, 6 + y0, 33, 28 + y0), 'fur')
    f.e((14, 7 + y0, 30, 20 + y0), 'fur_l')
    f.e((24, 15 + y0, 38, 26 + y0), 'cream')
    f.e((25, 15 + y0, 36, 21 + y0), 'cream')
    f.e((13, 18 + y0, 26, 28 + y0), 'fur')
    f.e((22, 21 + y0, 34, 27 + y0), 'cream_d')
    f.e((24, 19 + y0, 37, 25 + y0), 'cream')
    f.e((35, 18 + y0, 38, 21 + y0), 'nose')
    # Ohr
    f.p([(16, 9 + y0), (18, -1 + y0 + 2), (24, 7 + y0)], 'fur')
    f.p([(18, 8 + y0), (19, 2 + y0), (22, 7 + y0)], 'fur_dd')
    f.p([(17, 6 + y0), (18, 1 + y0), (19, 2 + y0)], 'fur_l')
    # Auge
    f.r((25, 12 + y0, 27, 17 + y0), 'eye')
    f.r((24, 13 + y0, 28, 16 + y0), 'eye')
    f.px(25, 13 + y0, 'shine')
    f.px(26, 13 + y0, 'shine')
    f.px(27, 16 + y0, 'fur_l')
    f.e((25, 19 + y0, 28, 21 + y0), 'blush')
    f.outline()
    return f.im


def front(step, bob, back=False, cheer=0):
    f = Frame()
    y0 = bob - (cheer if cheer else 0)
    cx = FW // 2
    # Schwanz (hinter dem Koerper, von vorn nur ein Zipfel)
    if not back:
        f.p([(cx + 6, 40 + y0), (cx + 15, 34 + y0), (cx + 16, 28 + y0), (cx + 12, 33 + y0)], 'fur')
        f.p([(cx + 15, 31 + y0), (cx + 16, 28 + y0), (cx + 13, 31 + y0)], 'cream')
    # Beine
    lifts = [(0, 0), (-2, 0), (0, 0), (0, -2), (0, 0)][step]
    for side_, lift in ((-1, lifts[0]), (1, lifts[1])):
        x = cx + side_ * 4
        f.r((x - 2, 44 + y0, x + 1, GROUND - 3 + lift), 'fur_d' if side_ < 0 else 'fur')
        f.r((x - 3, GROUND - 3 + lift, x + 2, GROUND + lift), 'boot')
        f.r((x - 3, GROUND - 3 + lift, x + 2, GROUND - 3 + lift), 'boot_l')
    # Koerper
    f.e((cx - 9, 28 + y0, cx + 8, 47 + y0), 'tunic')
    f.e((cx - 7, 29 + y0, cx + 2, 44 + y0), 'tunic_l')
    f.r((cx - 8, 44 + y0, cx + 7, 46 + y0), 'tunic_d')
    if back:
        # Rucksack bedeckt den Ruecken
        f.r((cx - 8, 28 + y0, cx + 7, 44 + y0), 'pack')
        f.r((cx - 8, 28 + y0, cx + 7, 29 + y0), 'pack_l')
        f.r((cx - 6, 33 + y0, cx + 5, 39 + y0), 'pack_d')
        f.r((cx - 5, 34 + y0, cx + 4, 38 + y0), 'pack_l')
        f.px(cx, 36 + y0, 'strap')
        f.r((cx - 8, 41 + y0, cx + 7, 44 + y0), 'pack_d')
    else:
        f.r((cx - 7, 31 + y0, cx - 6, 43 + y0), 'strap')
        f.r((cx + 5, 31 + y0, cx + 6, 43 + y0), 'strap')
        f.e((cx - 3, 35 + y0, cx + 2, 40 + y0), 'cream')
    # Arme
    arm = [0, 2, 0, -2, 0][step]
    if cheer:
        f.p([(cx - 9, 32 + y0), (cx - 7, 32 + y0), (cx - 11, 22 + y0), (cx - 13, 23 + y0)], 'fur')
        f.p([(cx + 6, 32 + y0), (cx + 8, 32 + y0), (cx + 12, 23 + y0), (cx + 10, 22 + y0)], 'fur')
        f.e((cx - 15, 19 + y0, cx - 10, 24 + y0), 'fur_l')
        f.e((cx + 9, 19 + y0, cx + 14, 24 + y0), 'fur_l')
    else:
        f.p([(cx - 10, 32 + y0), (cx - 7, 32 + y0), (cx - 8, 41 + y0 + arm), (cx - 11, 41 + y0 + arm)], 'fur')
        f.p([(cx + 6, 32 + y0), (cx + 9, 32 + y0), (cx + 10, 41 + y0 - arm), (cx + 7, 41 + y0 - arm)], 'fur')
        f.e((cx - 12, 40 + y0 + arm, cx - 7, 44 + y0 + arm), 'fur_l')
        f.e((cx + 6, 40 + y0 - arm, cx + 11, 44 + y0 - arm), 'fur_l')
    # Schal
    f.e((cx - 9, 26 + y0, cx + 8, 32 + y0), 'scarf')
    f.r((cx - 7, 26 + y0, cx + 6, 27 + y0), 'scarf_l')
    if not back:
        f.r((cx + 2, 30 + y0, cx + 5, 37 + y0), 'scarf_d')
    # Kopf
    f.e((cx - 12, 6 + y0, cx + 11, 28 + y0), 'fur')
    f.e((cx - 10, 7 + y0, cx + 6, 19 + y0), 'fur_l')
    # Ohren
    for s_ in (-1, 1):
        ex = cx + s_ * 8
        f.p([(ex - 5, 10 + y0), (ex + s_ * 1, 0 + y0), (ex + 5, 10 + y0)], 'fur')
        f.p([(ex - 3, 9 + y0), (ex + s_ * 1, 3 + y0), (ex + 3, 9 + y0)], 'fur_dd' if not back else 'fur_d')
    if back:
        f.e((cx - 10, 16 + y0, cx + 9, 28 + y0), 'fur_d')
        f.e((cx - 8, 9 + y0, cx + 6, 18 + y0), 'fur')
    else:
        f.e((cx - 9, 17 + y0, cx + 8, 28 + y0), 'cream')
        f.e((cx - 7, 16 + y0, cx + 6, 24 + y0), 'cream')
        f.e((cx - 2, 20 + y0, cx + 1, 22 + y0), 'nose')
        happy = cheer > 0
        for s_ in (-1, 1):
            ex = cx + s_ * 5 - (1 if s_ > 0 else 0)
            if happy:
                f.d.arc((ex - 2, 13 + y0, ex + 2, 18 + y0), 200, 340, fill=C['eye'], width=1)
            else:
                f.r((ex - 1, 12 + y0, ex + 1, 17 + y0), 'eye')
                f.r((ex - 2, 13 + y0, ex + 2, 16 + y0), 'eye')
                f.px(ex - 1, 13 + y0, 'shine')
                f.px(ex, 13 + y0, 'shine')
            f.e((ex - 2 + s_, 19 + y0, ex + 1 + s_, 21 + y0), 'blush')
        f.px(cx - 1, 24 + y0, 'nose')
        f.px(cx, 24 + y0, 'nose')
    f.outline()
    return f.im


def sheet():
    out = Image.new('RGBA', (FW * COLS, FH * ROWS), (0, 0, 0, 0))
    bobs = [0, 1, 0, 1, 0]
    for i in range(5):
        out.paste(side(i, bobs[i]), (i * FW, 0))
        out.paste(front(i, bobs[i]), (i * FW, FH))
        out.paste(front(i, bobs[i], back=True), (i * FW, FH * 2))
    for i, lift in enumerate([0, 3, 5, 3]):
        out.paste(front(0, 0, cheer=lift if lift else 1), (i * FW, FH * 3))
    return out


if __name__ == '__main__':
    s = sheet()
    path = sys.argv[1] if len(sys.argv) > 1 else 'hero.png'
    s.save(path)
    bg = Image.new('RGBA', s.size, (60, 50, 70, 255))
    bg.alpha_composite(s)
    bg.resize((s.width * 6, s.height * 6), Image.NEAREST).save(path.replace('.png', '_x6.png'))
