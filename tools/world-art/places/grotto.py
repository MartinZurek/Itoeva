"""GROTTO - die Kristallgrotte: Hoehle aus warmem Stein mit Tropfsteinen, blau und violett
leuchtende Kristalle, ein stiller Tuempel, in dem sie sich spiegeln, Lichtschein von oben durch
eine Oeffnung, leuchtende Pilze, eine Steinbank."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
from kit import Scene, W, H
from px import rgb, lerp, noise, dramp, glow


def build(out):
    s = Scene('grotto', horizon=170, seed=39)
    # Hoehlenwand: dunkler Stein mit Struktur, nach hinten heller (Tiefe)
    n = noise(W, H, 6, 1, 4)
    r = np.hypot((s.xx - 240) / 240, (s.yy - 120) / 150)
    t = np.clip(1 - r, 0, 1)
    s.cv.paint(s.yy >= 0, dramp([(0, '#1a1622'), (0.4, '#2e2638'), (0.7, '#4a3e52'), (1, '#6a5a6a')], np.clip(t * 0.8 + n * 0.3, 0, 1), 6))
    # Oeffnung oben mit Licht
    hole = s.ellipse(300, 0, 50, 22)
    s.paint(hole, '#c8e0f0')
    beam = s.poly([(262, 0), (338, 0), (380, 230), (250, 230)])
    s.cv.c[beam] = lerp(s.cv.c[beam], rgb('#d8e8ff'), 0.16)
    # Tropfsteine von der Decke
    rr = np.random.default_rng(5)
    for x in range(0, 480, 11):
        if abs(x - 300) < 50:
            continue
        h = rr.uniform(10, 46)
        y0 = rr.uniform(-4, 14)
        m = s.poly([(x - 5, y0), (x + 5, y0), (x + 0.5, y0 + h)])
        s.shade(m, ['#2a2230', '#3e3446', '#56485c', '#706070'], light=(-1, -0.2))
    # Hinterer Boden und Tuempel
    s.cv.paint(s.yy >= 170, dramp([(0, '#3a3242'), (1, '#5a4a52')], np.clip((s.yy - 170) / 100 + n * 0.2, 0, 1), 5))
    pool = s.ellipse(250, 200, 150, 20)
    s.paint(pool, '#1e3a5a')
    # Felsmassen links und rechts als Rahmen
    for pts, sd in (([(0, 0), (90, 0), (120, 60), (100, 130), (130, 180), (60, 200), (0, 210)], 11),
                    ([(480, 0), (400, 0), (380, 50), (410, 120), (370, 170), (420, 196), (480, 200)], 12)):
        m = s.poly(pts)
        s.shade(m, ['#1e1824', '#2e2636', '#463a4c', '#625266'], light=(-0.8, -0.6), fur=0.3, seed=sd)
        s.paint(m & (noise(W, H, 2, sd, 2) > 0.72), '#54465a')
        s.outline(m, strength=0.4)
    # Kristalle
    cs = [(70, 176, 40, ('#3a5aa8', '#5a8ae0', '#8ac0f0', '#d8f0ff'), '#6ab0ff'),
          (96, 184, 26, ('#5a3aa8', '#8a5ae0', '#b08af0', '#ecd8ff'), '#b06aff'),
          (380, 170, 48, ('#3a5aa8', '#5a8ae0', '#8ac0f0', '#d8f0ff'), '#6ab0ff'),
          (410, 182, 30, ('#3a8aa8', '#5ac0e0', '#8ae0f0', '#d8fff8'), '#6af0ff'),
          (180, 166, 20, ('#5a3aa8', '#8a5ae0', '#b08af0', '#ecd8ff'), '#b06aff'),
          (24, 250, 56, ('#5a3aa8', '#8a5ae0', '#b08af0', '#ecd8ff'), '#b06aff'),
          (456, 260, 60, ('#3a5aa8', '#5a8ae0', '#8ac0f0', '#d8f0ff'), '#6ab0ff')]
    for x, y, h, col, g in cs:
        s.crystal(x, y, h, col=col, glow_c=g)
        s.crystal(x + h * 0.25, y + 2, h * 0.6, col=col, glow_c=g)
    s.reflect(186, 30, y_end=220, k=0.5, tint='#1e3a5a')
    s.paint(pool & ((s.yy % 3) == 0) & (noise(W, H, 4, 7, 2) > 0.7), '#4a7aa8')
    # Felsen und Pilze
    for x, y, rx in ((150, 236, 18), (330, 244, 14), (230, 258, 10)):
        s.rock(x, y, rx, rx * 0.7, cols=('#2a2230', '#3e3446', '#56485c', '#706070'), seed=x)
    for x, y in ((130, 224), (138, 226), (300, 232), (60, 230)):
        s.mushroom(x, y, s=1.3, cap='#6ac8c8', glow_c='#6af0e0')
    seat = s.stone_seat(300, 226, s=0.9)
    s.walk(farY=214, nearY=250, farLeft=60, farRight=420, nearLeft=40, nearRight=440, farH=44, nearH=58)
    s.spot('BENCH', seat, 300, 232)
    return s.finish(out)


if __name__ == '__main__':
    print(build(sys.argv[1]))
