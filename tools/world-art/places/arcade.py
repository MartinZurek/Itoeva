"""ARCADE - die Spielhalle: dunkler Raum in Violett mit Neonschild, Spielautomaten aus Holz mit
leuchtenden Bildschirmen und Leuchtkoepfen an der Rueckwand, Sternenteppich, Lichterkette;
hinten rechts die Tuer. Gleiche Bauweise wie die Zimmer, nur bei Nacht und bunt."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
import props as P
from roomkit import CozyRoom, W, H, FLOOR
from px import rgb, MaskPen, glow, lerp


SCREENS = [('#3a6ae8', '#e8f040'), ('#e84a8a', '#40e0d0'), ('#40c060', '#f0a030'), ('#9a5ae8', '#f4f0e0')]


def cabinet(r, X0, X1, Z0, Z1, k):
    """Spielautomat mit Holzkorpus, schraegem Bildschirm, Knoepfen und leuchtendem Kopf."""
    body, lit = SCREENS[k % len(SCREENS)]
    m = r.box(X0, X1, 0.0, 0.42, Z0, Z1, '#4a2e3a', top='#6a4458', edge='#a87090')
    m |= r.box(X0, X1, 0.42, 0.8, Z0 + 0.08, Z1, '#4a2e3a', top='#6a4458', side='#5a3a4a')
    m |= r.box(X0, X1, 0.8, 0.92, Z0, Z1, body, top=rgb(body) * 0.7, side=rgb(body) * 0.8, edge='#ffffff')
    panel = r.box(X0 + 0.01, X1 - 0.01, 0.4, 0.44, Z0 - 0.04, Z0 + 0.08, '#2a1e2a', top='#3a2a3a', edge='#8a6a8a')
    m |= panel
    for j, c in enumerate(('#e84a4a', '#f0e040', '#40c0e8')):
        p = r.room.proj(X0 + (X1 - X0) * (0.3 + 0.2 * j), 0.44, Z0 + 0.02)
        r.cv.paint(MaskPen(W, H).ellipse(p[0] - 1, p[1] - 1, p[0] + 1, p[1]).a > 0, rgb(c))
    scr = r.room.quad([(X0 + 0.03, 0.48, Z0 + 0.09), (X1 - 0.03, 0.48, Z0 + 0.09),
                       (X1 - 0.03, 0.76, Z0 + 0.09), (X0 + 0.03, 0.76, Z0 + 0.09)], rgb('#101020'))
    if scr.any():
        ys, xs = np.nonzero(scr)
        t = (r.yy - ys.min()) / max(1, np.ptp(ys))
        r.cv.paint(scr, lerp(rgb('#101830'), rgb(body), np.clip(t, 0, 1)[..., None] * 0.6))
        cx, cy = xs.mean(), ys.mean()
        r.cv.paint(scr & (np.abs(r.xx - cx + 3) < 2) & (np.abs(r.yy - cy) < 2), rgb(lit))
        r.cv.paint(scr & ((r.xx * 5 + r.yy * 3) % 13 == 0), rgb('#f4f0e0'))
        r.cv.add(glow(W, H, [(cx, cy, 1.0)], 14, body, 0.3, steps=3))
    head = r.room.proj((X0 + X1) / 2, 0.86, Z0)
    r.cv.add(glow(W, H, [head + (1.0,)], 8, body, 0.35, steps=3))
    return m


def build(out):
    lights = [(np.array([-0.3, 0.9, 2.3]), rgb('#e86ad0'), 1.1),
              (np.array([0.5, 0.9, 2.3]), rgb('#5ab0f0'), 1.1),
              (np.array([0.0, 1.3, 1.5]), rgb('#a890ff'), 1.0)]
    r = CozyRoom(seed=111, lights=lights, plaster=('#3a2a5a', '#46346a'), wood=('#2e2238', '#3a2c46', '#4a3858'),
                 ambient='#3a3058')
    r.meta['blocked'] = []
    # Teppich mit Sternen auf dem ganzen Boden
    fl = r.which == FLOOR
    star = (((np.floor(r.X / 0.18) * 7 + np.floor(r.Z / 0.18) * 13) % 5) == 0) & \
           (np.hypot((r.X / 0.18) % 1 - 0.5, (r.Z / 0.18) % 1 - 0.5) < 0.18)
    col = np.where(star[..., None], rgb('#f0d060'), rgb('#2a2050'))
    col = np.where(((((r.X / 0.36).astype(int) + (r.Z / 0.36).astype(int)) % 2) == 0)[..., None] & ~star[..., None],
                   rgb('#34286a'), col)
    r._shade(fl & (r.Z > 1.0), col, (0, 1, 0))
    door = r.door_back(0.62, 0.94, 0.98)
    # Neonschild
    x0, y0, x1, y1 = r.rect_on_back(-0.5, 0.3, 1.0, 1.22)
    sign = MaskPen(W, H).rect(x0, y0, x1, y1).a > 0
    r.cv.paint(sign, rgb('#1a1430'))
    inner = (r.xx >= x0 + 3) & (r.xx <= x1 - 3) & (r.yy >= y0 + 3) & (r.yy <= y1 - 3)
    letters = inner & ((((r.xx - x0) // 3) % 3 != 2) & (((r.yy - y0) % 6) < 4)) & (((r.xx - x0) // 9) % 2 == 0)
    r.cv.paint(letters, rgb('#ff8ae0'))
    r.cv.paint(sign & ~inner & ~MaskPen(W, H).rect(x0 + 1, y0 + 1, x1 - 1, y1 - 1).a.astype(bool), rgb('#6ae0ff'))
    r.cv.add(glow(W, H, [((x0 + x1) / 2, (y0 + y1) / 2, 1.0)], 14, '#ff60d0', 0.35, steps=3))
    # Lichterkette unter der Decke
    for k in range(18):
        X = -1.0 + k * 0.118
        p = r.room.proj(X, 1.36 - 0.03 * np.sin(k * 0.9) ** 2, 2.66)
        c = ['#ff8ae0', '#6ae0ff', '#f0e060', '#80f080'][k % 4]
        r.cv.paint(MaskPen(W, H).ellipse(p[0] - 1, p[1] - 1, p[0] + 1, p[1] + 1).a > 0, rgb(c))
        r.cv.add(glow(W, H, [(p[0], p[1], 1.0)], 3, c, 0.3, steps=2))
    machines = np.zeros((H, W), bool)
    for k, (X0, X1) in enumerate(((-0.98, -0.66), (-0.6, -0.28), (-0.22, 0.1), (0.16, 0.48))):
        machines |= cabinet(r, X0, X1, 2.4, 2.66, k)
    # Ein Automat seitlich vorn rechts, ein Hocker
    r.box(0.74, 1.04, 0.0, 0.92, 1.62, 1.9, '#6a3e52', top='#8a5a70', side='#7a4a60', edge='#c890b0')
    side_scr = r.room.quad([(0.74, 0.5, 1.66), (0.74, 0.76, 1.66), (0.74, 0.76, 1.86), (0.74, 0.5, 1.86)], rgb('#3a8ae8'))
    r.cv.paint(side_scr & ((r.xx + r.yy) % 7 == 0), rgb('#e8f040'))
    r.screen_glow(side_scr, '#3a8ae8')
    for X, Z in ((-0.5, 2.1), (0.0, 2.05)):
        r.box(X - 0.02, X + 0.02, 0.0, 0.26, Z - 0.02, Z + 0.02, '#8a8aa0')
        r.box(X - 0.09, X + 0.09, 0.26, 0.3, Z - 0.09, Z + 0.09, '#d84a6a', top='#f06a8a', edge='#ffa0c0')
    P.plant(r, -0.92, 1.4, kind='palm', size=1.0)
    # Poster an den Waenden
    for side, z0, z1, c in ((-1, 1.6, 1.95, '#e86ad0'), (-1, 2.05, 2.35, '#5ab0f0'), (1, 2.05, 2.4, '#f0d060')):
        m = r.wall_side(side, z0, z1, 0.55, 1.0, rgb('#1a1430'))
        inner = r.wall_side(side, z0 + 0.03, z1 - 0.03, 0.58, 0.97, rgb(c) * 0.7)
        r.cv.paint(inner & (((r.xx + r.yy) % 5) == 0), rgb(c))
        r.cv.paint(inner & ((r.yy % 7) == 0), rgb('#f4f0e0'))
    # Tickets, Muenzbecher
    for k in range(12):
        p = r.room.proj(-0.6 + k * 0.05, 0.0, 1.5 + (k % 3) * 0.1)
        r.cv.paint(MaskPen(W, H).rect(p[0], p[1] - 1, p[0] + 2, p[1]).a > 0, rgb('#f0c040') if k % 2 else rgb('#e86a8a'))
    r.walk(farLeft=150, farRight=322, nearLeft=40, nearRight=430)
    r.spot_box('ARCADE', r.bbox(machines), *r.stand(-0.4, 2.2))
    r.spot_box('DOOR', door, *r.stand(0.78, 2.2))
    return r.finish(out, colors=200)


if __name__ == '__main__':
    print(build(sys.argv[1]))
