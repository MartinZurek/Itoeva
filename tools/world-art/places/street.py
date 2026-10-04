"""STREET - nach world-studies/itoeva-riverside-quarter: Uferpromenade mit Cafe (gruene Markise,
Tische), Buchladen mit Schild, Wimpeln, Baum, Laterne, Steinbruecke, Stadt am Hang mit Zypressen.
Die Haustuer links fuehrt nach Hause."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
from kit import Scene, W, H
from px import rgb, lerp


def build(out):
    s = Scene('street', horizon=150, seed=14)
    s.sky(top='#6aa4dc', mid='#a4cce8', low='#e8f0ea', warm='#f8e8c8')
    s.hills([(200, 480, 70, '#8aa0c8', '#b8c8e0', 33, 0.4)])
    # Stadt am Hang mit Zypressen und Aquaedukt
    s.town(220, 480, 70, 140, n=90, k=0.95, tower=(330, 58, 22))
    for x in range(232, 480, 23):
        s.tree(x, 120 + (x * 13) % 26, 22 + (x * 7) % 10, seed=x, kind='cypress')
    aq = s.rect(330, 116, 470, 126)
    s.paint(aq, '#e6d8c0')
    for x in range(334, 470, 12):
        s.paint(s.ellipse(x + 4, 126, 4, 6) & (s.yy >= 120), '#9ab0c4')
    s.treeline(150, 200, 480, '#3e6440', '#7aa05a', 34, height=12)
    s.grass(150, flowers=False)
    # Fluss und Bruecke rechts hinten
    s.water(150, 176, x0=300, x1=480, deep='#3e7aa0', mid='#5a9ac0', light='#a8d4e8', sky='#d8eef4')
    s.reflect(150, 20)
    br = s.poly([(300, 150), (480, 146), (480, 160), (300, 162)])
    s.paint(br, '#d8ccb6')
    for x in range(316, 480, 34):
        s.paint(s.ellipse(x + 14, 166, 13, 10) & (s.yy >= 156), '#5a8aa8')
    for x in range(300, 480, 6):
        s.paint(s.rect(x, 140, x + 1, 150), '#4a4a4a')
    s.paint(s.rect(300, 140, 480, 141), '#4a4a4a')
    # Promenade mit Pflaster
    prom = s.yy >= 176
    s.cobble(prom, tone=('#a89a88', '#bcae9a', '#d2c4ae', '#e6dac4'), size=0.16)
    # Hinter der Promenade, zwischen Haeusern und Fluss: Ufergarten mit Hecke
    for x in range(212, 304, 14):
        s.bush(x, 178, 9 + (x % 5), seed=x, flowers=('#f4f0e0', '#e88aa0') if x % 28 == 0 else None)
    # Ufermauer zwischen Promenade und Fluss
    s.stone_wall(166, 178, 296, 480)
    # Haeuser links: Cafe und Buchladen (mit Haustuer)
    door = s.facade(0, 120, 40, 178, shutters='#4e8a86', floors=2, shop=True, door_x=96, seed=1)
    s.awning(-6, 92, 120, depth=14, c1='#3e7a6a', c2='#3e7a6a')
    s.facade(126, 220, 60, 178, wall=('#c4a888', '#d8bc98', '#e8d0ac', '#f2e2c4'), shutters='#7a5a8a', floors=2, shop=True, seed=2)
    s.awning(130, 216, 132, depth=12, c1='#d8a060', c2='#f4ead6')
    # Haengeschild des Buchladens
    s.paint(s.rect(222, 96, 236, 97), '#2e3436')
    s.paint(s.rect(224, 98, 236, 112), '#3e5a7a')
    s.paint(s.rect(227, 101, 233, 109), '#e8d8b0')
    s.paint(s.rect(230, 101, 230, 109), '#8a6a4a')
    # Cafe-Tische vor dem Cafe
    for x in (22, 64):
        s.shadow(x + 2, 200, 14, 3)
        s.paint(s.ellipse(x, 186, 9, 2.5), '#3a3a3a')
        s.paint(s.ellipse(x, 185, 9, 2.2), '#e8e0d0')
        s.paint(s.rect(x, 187, x + 1, 199), '#3a3a3a')
        for cx in (x - 12, x + 10):
            s.paint(s.rect(cx, 190, cx + 4, 192), '#6a4a32')
            s.paint(s.rect(cx, 180, cx + 1, 199), '#4a3222')
            s.paint(s.rect(cx + 3, 192, cx + 4, 199), '#4a3222')
        s.paint(s.ellipse(x - 2, 183, 2, 1.5), '#f4f0e8')
    # Baum und Pflanzkuebel
    s.tree(262, 186, 110, seed=41, kind='round')
    for x in (130, 200, 290):
        s.planter(x, 190, 16, s=0.9)
    # Wimpel ueber der Strasse, Laterne
    s.bunting([(120, 70), (300, 96)])
    lamp = s.lamp(300, 196, s=1.0)
    bench = s.bench(380, 200, s=1.0)
    s.flowerbed(400, 500, 270, s=1.3, seed=44)
    s.foliage('bl', 50, 45)
    s.walk(farY=200, nearY=246, farLeft=90, farRight=440, nearLeft=30, nearRight=440, farH=46, nearH=62)
    s.spot('LAMP', lamp, 304, 206)
    s.spot('BENCH', bench, 380, 210)
    return s.finish(out)


if __name__ == '__main__':
    print(build(sys.argv[1]))
