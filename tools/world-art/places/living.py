"""LIVING - das Wohnzimmer nach world-studies/cozy-home (linke Haelfte): rotes Sofa mit Kissen
und Decke unter Bildern, Fenster mit Mond und Vorhaengen, Teppich, niedriger Tisch mit Teekanne,
Tassen und Buechern, Pflanzen verschiedener Art, Wandbord mit Kram, Uhr, Lichterkette, rechts das
alte Holz-Fernsehmoebel, hinten die Haustuer zur Strasse."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
from roomkit import CozyRoom, W, H
from px import rgb, MaskPen
import props as P


def build(out):
    lights = [(np.array([-0.1, 1.3, 1.9]), rgb('#ffb868'), 1.6),
              (np.array([-0.24, 1.0, 2.5]), rgb('#ff9a50'), 0.6)]
    r = CozyRoom(seed=21, lights=lights)
    # Rueckwand: Fenster mit Vorhaengen, Haustuer, Bilder und Uhr ueber dem Sofa
    r.window(-0.1, 0.36, 0.55, 1.1, view='night')
    P.curtains(r, -0.1, 0.36, 0.55, 1.1, col='#8a3a3e')
    door = r.door_back(0.56, 0.92, 1.0)
    P.painting(r, -0.94, -0.72, 0.76, 0.98, kind='sea')
    P.painting(r, -0.66, -0.52, 0.8, 0.98, kind='flowers', frame='#c8a050')
    P.painting(r, -0.92, -0.8, 1.02, 1.12, kind='mountain', frame='#5a3a26')
    P.clock(r, -0.44, 1.06, rad=0.06)
    P.wall_shelf_clutter(r, 0.5, 0.98, 1.13)
    P.string_lights(r, -1.0, 0.45, 1.33, 2.66)
    P.painting(r, 0, 0, 0.6, 0.95, kind='forest', side=-1, Z0=1.55, Z1=1.9)
    # Teppich und Sofa mit Kissen und Decke
    r.rug(-0.78, 0.32, 1.4, 2.3, c1='#8a3a32', c2='#d8a050', c3='#2e3a5a')
    r.sofa(-0.98, -0.32, 2.28, 2.68, color='#b04a3e', light='#d8786a', cushions=())
    P.cushion(r, -0.86, -0.7, 0.3, 0.46, 2.46, 2.52, '#e8c890', 'stripes')
    P.cushion(r, -0.64, -0.5, 0.3, 0.44, 2.47, 2.53, '#5a7a5a', 'checks')
    P.cushion(r, -0.48, -0.36, 0.3, 0.42, 2.46, 2.52, '#d8a050', 'dots')
    P.blanket(r, -0.98, -0.88, 0.36, 2.28, 2.6, col='#6a8a6a', col2='#e8d8b0')
    r.floor_lamp(-0.24, 2.6, h=0.86)
    P.plant(r, -0.2, 2.42, kind='monstera', size=1.1)
    P.plant(r, 0.46, 2.58, kind='snake', size=0.9)
    # Tisch mit Teekanne, Tassen, Buechern, Kerze
    r.table(-0.62, -0.08, 1.7, 1.94, h=0.24)
    P.teapot(r, -0.46, 0.24, 1.82, col='#3e6a8a')
    P.mug(r, -0.34, 0.24, 1.78, steam=True)
    P.mug(r, -0.56, 0.24, 1.74)
    P.book_stack(r, -0.18, 0.24, 1.84, n=3)
    r.candle(-0.27, 0.24, 1.76)
    P.basket(r, -0.12, 2.3, fill='blanket')
    # Fernsehmoebel an der rechten Wand
    tv = r.box(0.74, 1.04, 0.0, 0.42, 1.62, 2.12, '#7a4a30', top='#9a6444', side='#8a5a3a', edge='#e0a070')
    tv |= r.box(0.8, 1.02, 0.42, 0.78, 1.7, 2.04, '#5a3a26', top='#7a5038', side='#6a4430', edge='#c08458')
    scr = r.room.quad([(0.8, 0.48, 1.76), (0.8, 0.72, 1.76), (0.8, 0.72, 1.98), (0.8, 0.48, 1.98)], rgb('#4a7a9a'))
    sy = np.clip((r.yy - r.yy[scr].min()) / max(1, np.ptp(r.yy[scr])), 0, 1)
    r.cv.paint(scr, rgb('#3a6a8a') * (1.1 - 0.4 * sy[..., None]))
    r.cv.paint(scr & (((r.yy + r.xx) % 5) == 0), rgb('#9ad0e8'))
    r.cv.paint(scr & (((r.yy * 3 + r.xx) % 11) == 0), rgb('#e8f0a0'))
    r.screen_glow(scr)
    for k, z in enumerate((1.66, 1.8, 1.95)):
        a, b = r.room.proj(0.75, 0.06, z), r.room.proj(0.75, 0.36, z)
        r.cv.paint(MaskPen(W, H).line([a, b]).a > 0, rgb('#4a2e20'))
        p = r.room.proj(0.75, 0.2, z + 0.06)
        r.cv.paint(MaskPen(W, H).ellipse(p[0] - 1, p[1] - 1, p[0] + 1, p[1] + 1).a > 0, rgb('#e8c070'))
    a = r.room.proj(0.9, 0.78, 1.86)
    r.cv.paint(MaskPen(W, H).line([a, (a[0] - 10, a[1] - 18)]).a > 0, rgb('#c8a060'))
    r.cv.paint(MaskPen(W, H).line([a, (a[0] + 6, a[1] - 20)]).a > 0, rgb('#c8a060'))
    P.plant(r, 0.92, 2.0, kind='cactus', size=0.6, Y=0.78)
    # Pflanzen: Monstera neben dem Sofa, Palme in der Ecke, Haengepflanzen
    P.plant(r, -0.9, 1.5, kind='palm', size=1.3)
    r.hanging_plant(120, 26, 30)
    r.hanging_plant(392, 18, 34)
    r.pendant(-0.1, 1.22, 1.9)
    # Kommode links vorn mit Laterne
    r.cabinet(-1.04, -0.84, 0.36, 1.05, 1.3, drawers=2)
    r.candle(-0.94, 0.36, 1.12)
    P.book_stack(r, -0.93, 0.36, 1.22, n=2)
    r.walk(farLeft=150, farRight=322, nearLeft=40, nearRight=430)
    r.spot_box('SEAT', r.proj_box(-0.98, -0.32, 0.0, 0.62, 2.28, 2.68), *r.stand(-0.62, 2.0))
    r.spot_box('TV', r.bbox(tv), *r.stand(0.6, 1.62))
    r.spot_box('DOOR', door, *r.stand(0.71, 2.2))
    return r.finish(out, colors=200)


if __name__ == '__main__':
    print(build(sys.argv[1]))
