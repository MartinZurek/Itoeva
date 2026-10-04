"""LIVING - das Wohnzimmer nach world-studies/cozy-home (linke Haelfte): rotes Sofa unter dem
Bogenfenster, runder Teppich, niedriger Tisch mit Tassen, Haengepflanzen, Pendelleuchte am Abend,
rechts das alte Holz-Fernsehmoebel, hinten die Haustuer zur Strasse."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
from roomkit import CozyRoom, W, H
from px import rgb, glow, MaskPen


def build(out):
    lights = [(np.array([-0.1, 1.3, 1.9]), rgb('#ffb868'), 1.6),
              (np.array([-0.24, 1.0, 2.5]), rgb('#ff9a50'), 0.6)]
    r = CozyRoom(seed=21, lights=lights)
    # Rueckwand: Fenster mit Nacht und Mond, Haustuer rechts, Bilder ueber dem Sofa
    r.window(-0.1, 0.36, 0.55, 1.1, view='night')
    door = r.door_back(0.56, 0.92, 1.0)
    r.picture(-0.92, -0.72, 0.78, 0.98, colors=('#e8b070', '#c87a5a', '#4a6a5a'))
    r.picture(-0.64, -0.5, 0.82, 0.98, colors=('#9ac0d8', '#e8d8a0', '#6a8a4a'))
    # Wandbord ueber der Tuer mit Toepfen
    r.box(0.5, 0.98, 1.1, 1.13, 2.58, 2.7, '#6a4230', top='#8a5a3e', edge='#d09868')
    for x in (0.6, 0.74, 0.88):
        r.box(x - 0.04, x + 0.04, 1.13, 1.2, 2.6, 2.66, '#a85a3a', top='#4a3020', edge='#e09060')
    # Teppich und Sofa
    r.rug(-0.78, 0.32, 1.4, 2.3, c1='#8a3a32', c2='#d8a050', c3='#2e3a5a')
    r.sofa(-0.98, -0.32, 2.28, 2.68, color='#b04a3e', light='#d8786a', cushions=('#e8c890', '#6a8a5a'))
    r.floor_lamp(-0.24, 2.6, h=0.86)
    # Tisch mit Tassen und Kerze
    r.table(-0.62, -0.08, 1.7, 1.94, h=0.24)
    r.cup(-0.5, 0.24, 1.8)
    r.cup(-0.38, 0.24, 1.84, '#c8704a')
    r.books(-0.2, 0.24, 1.82, n=2)
    r.candle(-0.29, 0.24, 1.78)
    # Fernsehmoebel an der rechten Wand: Holzkasten mit gewoelbtem Bildschirm, der zum Raum zeigt
    tv = r.box(0.74, 1.04, 0.0, 0.42, 1.62, 2.12, '#7a4a30', top='#9a6444', side='#8a5a3a', edge='#e0a070')
    tv |= r.box(0.8, 1.02, 0.42, 0.78, 1.7, 2.04, '#5a3a26', top='#7a5038', side='#6a4430', edge='#c08458')
    scr = r.room.quad([(0.8, 0.48, 1.76), (0.8, 0.72, 1.76), (0.8, 0.72, 1.98), (0.8, 0.48, 1.98)], rgb('#4a7a9a'))
    sy = np.clip((r.yy - r.yy[scr].min()) / max(1, np.ptp(r.yy[scr])), 0, 1)
    r.cv.paint(scr, rgb('#3a6a8a') * (1.1 - 0.4 * sy[..., None]))
    r.cv.paint(scr & (((r.yy + r.xx) % 5) == 0), rgb('#9ad0e8'))
    r.screen_glow(scr)
    for z in (1.66, 2.06):
        p = r.room.proj(0.75, 0.42, z)
        r.cv.paint(MaskPen(W, H).line([(p[0], p[1]), (p[0], p[1] + 10)]).a > 0, rgb('#3a2418'))
    # Antenne und Pflanze oben drauf
    a = r.room.proj(0.9, 0.78, 1.86)
    r.cv.paint(MaskPen(W, H).line([a, (a[0] - 10, a[1] - 18)]).a > 0, rgb('#c8a060'))
    r.cv.paint(MaskPen(W, H).line([a, (a[0] + 6, a[1] - 20)]).a > 0, rgb('#c8a060'))
    # Pflanzen, Haengepflanzen, Pendelleuchte
    r.plant(-0.88, 1.5, size=1.3)
    r.plant(0.46, 2.58, size=0.8)
    r.hanging_plant(120, 26, 30)
    r.hanging_plant(392, 18, 34)
    r.pendant(-0.1, 1.22, 1.9)
    # Kleine Kommode links vorn mit Laterne
    r.cabinet(-1.04, -0.84, 0.36, 1.05, 1.3, drawers=2)
    r.candle(-0.94, 0.36, 1.12)
    r.walk(farLeft=150, farRight=322, nearLeft=40, nearRight=430)
    r.spot_box('SEAT', r.proj_box(-0.98, -0.32, 0.0, 0.62, 2.28, 2.68), *r.stand(-0.62, 2.0))
    r.spot_box('TV', r.bbox(tv), *r.stand(0.6, 1.62))
    r.spot_box('DOOR', door, *r.stand(0.71, 2.2))
    return r.finish(out, colors=150)


if __name__ == '__main__':
    print(build(sys.argv[1]))
