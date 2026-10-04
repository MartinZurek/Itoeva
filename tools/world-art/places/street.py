"""STREET - nach world-studies/itoeva-riverside-quarter: Uferpromenade mit Cafe (gruene Markise,
Tische, Kreidetafel), Buchladen mit Haengeschild, Marktstand unter dem Baum, Wimpel, verzierte
Laternen mit Blumenampeln, Steinbalustrade mit Toepfen, Bruecke ueber den Fluss, Stadt am Hang
mit Zypressen und Aquaedukt."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
from kit import Scene, W, H
from px import rgb, lerp, noise
import nature as N
import town as T


def balustrade(s, x0, x1, y_top, y_base, rng):
    """Steinbruestung: Deckplatte, Balustersaeulen, Pfeiler mit Kugel, Moos und Flecken."""
    s.paint(s.rect(x0, y_top, x1, y_top + 3), '#e0d4be')
    s.paint(s.rect(x0, y_top, x1, y_top), '#f4ecdc')
    s.paint(s.rect(x0, y_top + 3, x1, y_top + 3), '#a89a84')
    for x in range(int(x0) + 2, int(x1) - 2, 5):
        bal = s.ellipse(x + 1.5, (y_top + y_base) / 2 + 2, 1.8, (y_base - y_top) / 2 - 3) | s.rect(x + 1, y_top + 4, x + 2, y_base - 2)
        s.shade(bal, ['#8a8072', '#aaa092', '#c8bea8', '#e0d6c2'], light=(-1, -0.2))
    s.paint(s.rect(x0, y_base - 2, x1, y_base), '#9a907e')
    for x in (x0, (x0 + x1) // 2, x1 - 8):
        p = s.rect(x, y_top - 4, x + 8, y_base)
        s.shade(p, ['#9a907e', '#b8ae9a', '#d4cab4', '#ece2ce'], light=(-1, -0.3))
        s.paint(s.ellipse(x + 4, y_top - 7, 3.5, 3.5), '#d8ccb6')
        s.paint(s.ellipse(x + 3, y_top - 8, 1.5, 1.5), '#f0e8d8')
    moss = s.rect(x0, y_top, x1, y_base) & (noise(W, H, 3, 61, 2) > 0.72)
    s.paint(moss, '#6a7a44')


def market_stall(s, x0, x1, base, rng):
    top = base - 46
    for x in (x0, x1 - 3):
        s.paint(s.rect(x, top, x + 2, base), '#5a3a26')
        s.paint(s.rect(x, top, x, base), '#8a6040')
    counter = s.rect(x0 + 2, base - 18, x1 - 2, base)
    s.shade(counter, ['#4a2e20', '#6a4430', '#8a5a3e', '#a87250'], light=(-1, -0.4))
    for k in range(x0 + 4, x1 - 2, 4):
        s.paint(s.rect(k, base - 17, k, base - 1), '#4a2e20')
    cloth = s.rect(x0 + 3, base - 20, x1 - 3, base - 12)
    s.paint(cloth, '#f0e6d0')
    s.paint(cloth & ((s.xx // 4) % 2 == 0), '#e0d2b8')
    fruit = ['#e8503a', '#f0a030', '#f4d040', '#7ab04a', '#a83a5a', '#e8803a']
    for k, x in enumerate(range(x0 + 4, x1 - 10, 11)):
        s.paint(s.rect(x, base - 27, x + 9, base - 20), '#9a6a40')
        s.paint(s.rect(x, base - 27, x + 9, base - 27), '#c8925a')
        for j in range(8):
            fx, fy = x + 1 + (j % 4) * 2.2, base - 30 + (j // 4) * 2.4
            c = fruit[(k * 2 + j // 4) % len(fruit)]
            s.paint(s.ellipse(fx + 1, fy + 1, 1.4, 1.3), c)
            s.paint(s.rect(int(fx), int(fy), int(fx), int(fy)), lerp(rgb(c), rgb('#ffffff'), 0.4))
    # Haengende Knoblauchzoepfe und Kraeuter
    for x in range(x0 + 6, x1 - 4, 9):
        s.paint(s.rect(x, top + 13, x, top + 18), '#4a3a2a')
        s.paint(s.ellipse(x, top + 20, 1.6, 2.2), rng.choice(['#e8e0c8', '#5a8a3a', '#c86a3a']))
    T.awning(s, x0 - 4, x1 + 4, top, depth=12, c1='#d8644a', c2='#f4ead6')
    s.paint(s.rect(x0 + 6, top + 14, x0 + 6, top + 16), '#2a2020')
    s.paint(s.rect(x0 + 4, top + 16, x0 + 8, top + 21), '#ffd890')


def build(out):
    rng = np.random.default_rng(14)
    s = Scene('street', horizon=150, seed=14)
    s.sky(top='#5a9ad8', mid='#a4cce8', low='#eef2ea', warm='#fbe8c8', clouds=False)
    s.clouds(n=5, ymin=8, ymax=70)
    s.hills([(200, 520, 64, '#8aa0c8', '#b8c8e0', 33, 0.4)])
    s.hills([(160, 520, 92, '#7a9a7a', '#a0b890', 34, 0.25)])
    # Stadt am Hang mit Zypressen, Aquaedukt
    s.town(220, 480, 70, 140, n=110, k=0.95, tower=(330, 58, 22))
    for x in range(228, 480, 11):
        N.cypress(s.cv, x + rng.uniform(-3, 3), 118 + (x * 13) % 30, rng.uniform(16, 30), rng)
    aq = s.rect(330, 112, 470, 122)
    s.paint(aq, '#e6d8c0')
    s.paint(s.rect(330, 112, 470, 112), '#f8f0e0')
    for x in range(334, 470, 12):
        s.paint(s.ellipse(x + 4, 122, 4, 6) & (s.yy >= 116), '#8ea8bc')
    for x in range(204, 480, 15):
        s.tree(x + rng.uniform(-4, 4), 150, rng.uniform(16, 26), seed=x, kind='round', haze='#c8d8d0', k=0.3)
    s.grass(150, flowers=False)
    # Fluss und Bruecke rechts hinten
    s.water(150, 176, x0=300, x1=480, deep='#3e7aa0', mid='#5a9ac0', light='#a8d4e8', sky='#d8eef4')
    s.reflect(150, 20)
    br = s.poly([(300, 150), (480, 145), (480, 158), (300, 161)])
    s.shade(br, ['#a89a84', '#c4b8a2', '#d8ccb6', '#ece2d0'], light=(-0.3, -1))
    for x in range(316, 480, 34):
        s.paint(s.ellipse(x + 14, 166, 13, 10) & (s.yy >= 156), '#4a7a98')
        s.paint(s.ellipse(x + 14, 166, 13, 10) & (s.yy >= 156) & ~s.ellipse(x + 14, 167, 11, 9), '#b8ac96')
    for x in range(300, 480, 5):
        s.paint(s.rect(x, 139, x, 147), '#2e3436')
    s.paint(s.rect(300, 139, 480, 140), '#2e3436')
    for x in (330, 400, 462):
        s.tree(x, 147, 30, seed=x + 1, kind='round')
    # Promenade
    prom = s.yy >= 176
    s.cobble(prom, tone=('#a89a88', '#bcae9a', '#d2c4ae', '#e6dac4'), size=0.13)
    # Ufergarten mit Hecke, Balustrade zum Fluss
    for x in range(212, 304, 12):
        s.bush(x, 178, 8 + (x % 5), seed=x, flowers=('#f4f0e0', '#e88aa0') if x % 24 == 0 else None)
    balustrade(s, 296, 480, 160, 178, rng)
    for x in (318, 372, 430):
        T.potted(s, x, 160, rng, size=0.8, kind=rng.choice(['geranium', 'lavender', 'daisies']))
    # Cafe mit Haustuer, Buchladen
    door = T.facade(s, 0, 120, 40, 178, rng, shutters='#4e8a86', floors=2, shop='cafe', door_x=96)
    T.awning(s, -6, 92, 118, depth=14, c1='#3e7a6a', c2='#4a8a78')
    T.facade(s, 126, 214, 56, 178, rng, wall=('#c4a888', '#d8bc98', '#e8d0ac', '#f2e2c4'), shutters='#7a5a8a',
             floors=2, shop='books', door_x=196)
    T.awning(s, 130, 192, 128, depth=12, c1='#d8a060', c2='#f4ead6')
    T.hanging_sign(s, 220, 96, icon='book')
    T.wall_lantern(s, 6, 116)
    # Marktstand unter dem Baum
    s.tree(262, 186, 118, seed=41, kind='round')
    s.dapple(200, 236, 120, 16, seed=7, k=0.78)
    market_stall(s, 236, 296, 196, rng)
    # Cafe-Tische, Kreidetafel, Toepfe
    for x in (22, 66):
        T.cafe_set(s, x, 200, rng)
    T.chalkboard(s, 84, 202)
    for x, kind in ((118, 'olive'), (130, 'lavender'), (208, 'geranium'), (222, 'fern'), (300, 'box')):
        T.potted(s, x, 196, rng, size=1.0 + (x % 3) * 0.1, kind=kind)
    # Wimpel, Laternen, Bank
    s.bunting([(120, 70), (300, 96)])
    s.bunting([(300, 96), (484, 80)], colors=('#4e7aa8', '#e8b440', '#f4ead6', '#d8644a'))
    lamp = T.ornate_lamp(s, 306, 204, h=60)
    T.ornate_lamp(s, 470, 178, h=44, banner='#c8644a')
    bench = s.bench(380, 206, s=1.0)
    for x in (350, 410):
        T.potted(s, x, 214, rng, size=1.2)
    # Kisten am Stand, Giesskanne, Pfuetze
    for k, (x, y) in enumerate(((228, 206), (226, 198), (300, 210))):
        cr = s.rect(x - 7, y - 7, x + 7, y)
        s.shade(cr, ['#5a3a26', '#7a5234', '#9a6c44', '#b8885a'], light=(-1, -0.3))
        for yy in (y - 5, y - 2):
            s.paint(s.rect(x - 7, yy, x + 7, yy), '#4a2e1e')
        if k == 0:
            for j in range(6):
                s.paint(s.ellipse(x - 5 + j * 2, y - 8, 1.3, 1.2), ['#7ab04a', '#e8503a', '#f0a030'][j % 3])
    s.paint(s.ellipse(150, 238, 22, 3.5), '#8aa8c0')
    s.paint(s.ellipse(146, 237, 14, 1.8), '#c8e0f0')
    s.flowerbed(400, 500, 270, s=1.3, seed=44)
    s.foliage('bl', 50, 45)
    s.walk(farY=206, nearY=248, farLeft=90, farRight=440, nearLeft=30, nearRight=440, farH=46, nearH=62)
    s.spot('LAMP', lamp, 308, 212)
    s.spot('BENCH', bench, 380, 216)
    return s.finish(out, colors=200)


if __name__ == '__main__':
    print(build(sys.argv[1]))
