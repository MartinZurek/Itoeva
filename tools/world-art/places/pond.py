"""POND - nach world-studies/woodland-lake: Waldsee im goldenen Abendlicht, Steg ins Wasser,
Lesepavillon mit Buechern am Ufer, Birken und Tannen, Seerosen, Schilf, ferne Berge."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
from kit import Scene, W, H
from px import rgb, lerp, glow


def build(out):
    s = Scene('pond', horizon=112, seed=13)
    s.sky(top='#e8a868', mid='#f4c88a', low='#fbe4b4', warm='#ffd890', clouds=False)
    s.clouds(n=4, ymin=10, ymax=60, lit='#fff4dc', mid='#f8dcb0', shade='#e8b488', base='#c89478')
    # Abendsonne ueber den Bergen
    s.cv.add(glow(W, H, [(330, 92, 1.0)], 22, '#ffd080', 0.45, steps=4))
    s.paint(s.ellipse(330, 92, 7, 7), '#fff2c8')
    s.hills([(160, 480, 70, '#a87a8a', '#e8b0a0', 23, 0.45), (0, 300, 90, '#8a7a8a', '#c8a094', 24, 0.3)])
    # Nadelwald am anderen Ufer
    for x in range(0, 480, 9):
        h = 22 + (x * 37) % 18
        s.tree(x + (x % 5), 116, h, seed=x, kind='pine', crown=('#2e3a34', '#3e4e40', '#56664c', '#7a7c58'))
    # See
    lake = s.water(112, 214, deep='#3a5a6a', mid='#6a8a8a', light='#c8b49a', sky='#f4d8a8')
    s.reflect(112, 60)
    s.water_sun = None
    # Glitzerbahn der Sonne, nach dem Spiegelbild
    wn = np.random.default_rng(4).random((H, W))
    for y in range(114, 210):
        wdt = 2 + (y - 112) * 0.14
        for x in range(int(330 - wdt), int(330 + wdt)):
            if wn[y, x] > 0.55 and (x + y) % 2 == 0 and y % 2 == 0:
                s.cv.c[y, x] = lerp(s.cv.c[y, x], rgb('#fff0c8'), 0.8)
    # Insel mit Felsen
    s.shade(s.ellipse(240, 134, 26, 6), ['#4a4a48', '#6a6a60', '#8a867a', '#b0a894'])
    s.tree(236, 130, 22, seed=91, kind='pine', crown=('#2e3a34', '#3e4e40', '#56664c', '#7a7c58'))
    # Huette am fernen Ufer
    s.house(400, 112, 14, 8, roof='#6a4a3a', wall='#8a6a4a', k=0.9, lit=True)
    # Ufer vorn: Wiese, Schilf, Steine
    s.grass(214, base=('#3e5a2a', '#5a7a34', '#7a9440', '#a0aa52'), haze='#c8b47a')
    bank = s.ellipse(240, 222, 300, 12)
    s.paint(bank & (s.yy < 218), '#6a6a48')
    for x in range(-10, 490, 13):
        s.shade(s.ellipse(x, 216 + (x * 7) % 5, 7, 4), ['#5a5652', '#7a766c', '#9a948a', '#beb6a8'], seed=x)
    # Steg ins Wasser (Perspektive nach hinten)
    deck = s.poly([(150, 214), (210, 214), (196, 168), (176, 168)])
    s.shade(deck, ['#5a3e2a', '#7a5638', '#9a7048', '#b88c5c'], light=(-0.4, -1))
    for y in range(170, 214, 4):
        s.paint(deck & (s.yy == y), '#4a3222')
    for x0, x1 in ((152, 178), (206, 194)):
        s.paint(s.m().line([(x0, 216), (x1, 166)], 2).a > 0, '#4a3222')
    for y in (176, 196):
        for x in (174 - (214 - y) * 0.0, 200):
            pass
    # Seerosen
    for x, y in ((90, 196), (110, 202), (270, 198), (300, 204), (330, 194), (60, 186)):
        s.paint(s.ellipse(x, y, 6, 2), '#4e7a3e')
        s.paint(s.ellipse(x - 1, y - 0.5, 4, 1.2), '#6e9a52')
        if x % 3 == 0:
            s.paint(s.rect(x, y - 1, x + 1, y - 1), '#f4c8d8')
    # Schilf links und rechts
    rr = np.random.default_rng(5)
    for x in list(range(0, 120, 3)) + list(range(380, 480, 3)):
        h = int(rr.uniform(14, 34))
        base = 222 + int(rr.uniform(0, 30))
        s.paint(s.m().line([(x, base), (x + rr.uniform(-2, 2), base - h)]).a > 0, '#6a7a3a')
        if rr.random() < 0.3:
            s.paint(s.ellipse(x, base - h + 2, 1, 3), '#7a4a2a')
    # Lesepavillon mit Strohdach links
    s.paint(s.rect(14, 150, 92, 206), '#7a5a3a')
    s.shade(s.rect(18, 156, 88, 200), ['#5a3e28', '#6e4e32', '#86603e', '#9c7448'])
    for y in (166, 180, 194):
        s.paint(s.rect(20, y, 86, y + 1), '#4a3020')
        for x in range(22, 86, 4):
            s.paint(s.rect(x, y - 9, x + 2, y - 1), ['#8a3a3a', '#3a5a8a', '#4a7a4a', '#c8a050', '#6a4a7a'][(x // 4 + y) % 5])
    roof = s.poly([(0, 154), (52, 118), (106, 154), (100, 158), (6, 158)])
    s.shade(roof, ['#6a5a30', '#8a7640', '#aa9450', '#c8b064'], light=(-0.6, -1))
    for x in range(4, 104, 4):
        s.paint(roof & (s.xx == x) & (s.yy > 140), '#6a5a30')
    s.paint(s.rect(10, 156, 14, 214), '#5a3e28')
    s.paint(s.rect(92, 156, 96, 214), '#5a3e28')
    s.paint(s.rect(8, 206, 100, 214), '#8a6a48')
    s.paint(s.rect(8, 206, 100, 206), '#c8a070')
    s.cv.add(glow(W, H, [(50, 150, 1.0)], 6, '#ffb060', 0.4, steps=3))
    s.paint(s.rect(48, 146, 52, 152), '#ffd890')
    # Birke rechts als Rahmen, Vordergrund
    s.tree(452, 250, 120, seed=95, kind='birch')
    s.foliage('bl', 60, 96, cols=('#1a2a1a', '#2a3e26', '#3e5a32', '#5a7a40', '#7a9452'))
    s.walk(farY=222, nearY=250, farLeft=100, farRight=420, nearLeft=40, nearRight=430, farH=46, nearH=58)
    return s.finish(out)


if __name__ == '__main__':
    print(build(sys.argv[1]))
