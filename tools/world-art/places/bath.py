"""BATH - das Bad im Stil von world-studies/cozy-home: Kacheln bis Brusthoehe, Badewanne auf
Loewenfuessen rechts, Waschtisch mit rundem Spiegel an der Rueckwand, Handtuecher, Pflanzen und
eine Laterne; hinten links die Tuer."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
from roomkit import CozyRoom, W, H
from px import rgb, MaskPen, glow


def build(out):
    lights = [(np.array([0.0, 1.3, 1.9]), rgb('#ffc488'), 1.5),
              (np.array([0.1, 0.9, 2.5]), rgb('#ffd8a0'), 0.5)]
    r = CozyRoom(seed=51, lights=lights, plaster=('#d8c4a8', '#e8d6bc'))
    walls = (r.which == 2) | (r.which == 3) | (r.which == 4)
    u = np.where(r.which == 4, r.X, r.Z)
    tiles = walls & (r.Y > 0.04) & (r.Y < 0.72)
    grout = (np.abs((u / 0.1) % 1 - 0.5) > 0.44) | (np.abs((r.Y / 0.1) % 1 - 0.5) > 0.44)
    tc = np.where(grout[..., None], rgb('#c8c0b0'), rgb('#7ab0a8'))
    tc = np.where((((u / 0.1).astype(int) * 7 + (r.Y / 0.1).astype(int) * 3) % 11 == 0)[..., None], rgb('#e8e0c8'), tc)
    r._shade(tiles, tc, (0, 0, -1))
    r._shade(walls & (np.abs(r.Y - 0.73) < 0.02), rgb('#e8e0d0'), (0, 1, 0))
    door = r.door_back(-0.92, -0.58, 0.98)
    # Fenster klein und hoch
    r.window(0.5, 0.86, 0.86, 1.14, view='night')
    # Waschtisch mit Spiegel
    r.box(-0.3, 0.16, 0.0, 0.42, 2.46, 2.7, '#7a4a30', top='#9a6444', edge='#e0a070')
    basin = r.box(-0.26, 0.12, 0.42, 0.5, 2.48, 2.68, '#f0ece4', top='#d8d4cc', edge='#ffffff')
    p = r.room.proj(-0.07, 0.5, 2.58)
    r.cv.paint(MaskPen(W, H).ellipse(p[0] - 11, p[1] - 2, p[0] + 11, p[1] + 2).a > 0, rgb('#a8c8d0'))
    q = r.room.proj(-0.07, 0.6, 2.69)
    r.cv.paint(MaskPen(W, H).line([q, (q[0], q[1] + 6), (q[0] + 4, q[1] + 6)], 2).a > 0, rgb('#c8a060'))
    c = r.room.proj(-0.07, 0.94, 2.7)
    r.cv.paint(MaskPen(W, H).ellipse(c[0] - 20, c[1] - 22, c[0] + 20, c[1] + 22).a > 0, rgb('#a87a3a'))
    mir = MaskPen(W, H).ellipse(c[0] - 17, c[1] - 19, c[0] + 17, c[1] + 19).a > 0
    t = np.clip((r.yy - c[1] + 19) / 38, 0, 1)
    r.cv.paint(mir, (rgb('#4a6a7a') * (1 - t[..., None]) + rgb('#a89070') * t[..., None]))
    r.cv.paint(mir & (np.abs((r.xx - c[0]) + (r.yy - c[1]) + 6) < 2), rgb('#c8e0e8'))
    r.jar_row(-0.28, 0.14, 0.5, 2.66, colors=('#e8a0a0', '#a8d0c0', '#f0e0b0'))
    # Badewanne auf Fuessen, rechts an der Wand
    tub = r.box(0.42, 1.0, 0.08, 0.44, 1.7, 2.4, '#f0ece4', top='#d0ccc4', side='#e8e4dc', edge='#ffffff')
    water = r.room.quad([(0.47, 0.42, 1.75), (0.96, 0.42, 1.75), (0.96, 0.42, 2.35), (0.47, 0.42, 2.35)], rgb('#8ac0d0'))
    for k in range(12):
        bx, bz = 0.5 + (k * 37 % 40) / 100, 1.8 + (k * 53 % 50) / 100
        p = r.room.proj(bx, 0.44, bz)
        r.cv.paint(MaskPen(W, H).ellipse(p[0] - 2, p[1] - 1, p[0] + 2, p[1] + 1).a > 0, rgb('#f4fbff'))
    for z in (1.74, 2.34):
        r.box(0.46, 0.5, 0.0, 0.08, z, z + 0.04, '#c8a060', top='#e8c080')
    r.cup(0.5, 0.44, 1.76, '#f0c040')
    # Handtuchhalter und Handtuecher an der linken Wand
    r.wall_side(-1, 1.6, 2.1, 0.6, 0.63, rgb('#c8a060'))
    for z0, z1, cc in ((1.65, 1.82, '#c86a5a'), (1.88, 2.05, '#e8d8a0')):
        r.wall_side(-1, z0, z1, 0.32, 0.6, rgb(cc))
    r.rug(-0.5, 0.25, 1.4, 2.0, c1='#5a8a8a', c2='#e8e0c8', c3='#3a5a6a', round_=True)
    r.plant(-0.92, 2.5, size=1.0)
    r.plant(0.3, 2.56, size=0.6)
    r.hanging_plant(380, 26, 30)
    r.pendant(0.0, 1.22, 1.9)
    r.candle(0.1, 0.5, 2.6)
    r.walk(farLeft=146, farRight=320, nearLeft=40, nearRight=420)
    r.spot_box('TUB', r.bbox(tub), *r.stand(0.32, 1.8))
    r.spot_box('BASIN', r.proj_box(-0.3, 0.16, 0.0, 1.2, 2.46, 2.7), *r.stand(-0.07, 2.2))
    r.spot_box('DOOR', door, *r.stand(-0.75, 2.2))
    return r.finish(out, colors=150)


if __name__ == '__main__':
    print(build(sys.argv[1]))
