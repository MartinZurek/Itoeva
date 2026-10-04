"""SPORT - der Sportplatz aus der Studie world-studies/park-sports-ground (rechte Haelfte), eins
zu eins in Pixel-Art uebersetzt (pixelate.py): rotes Spielfeld mit Korb, Steintreppe, Mauer mit
Blumen, Laterne, Holzzaun, Steinbruecke, Stadt am Hang, Drachen am Himmel. Junge mit Hut,
fliegendes Wesen und Wyrmling sind aus dem Bild genommen; Zaun, Weg und Spielfeld dahinter sind
aus der Umgebung fortgesetzt."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
import concept as C
import pixelate as P


def build(out):
    img = C.native('park', (0, 0, 1085, 310))
    sh = img.shape[:2]
    yy = np.mgrid[0:sh[0], 0:sh[1]][0]
    boy = C.poly_mask(sh, [(625, 140), (640, 132), (668, 136), (674, 150), (668, 212), (622, 214), (620, 160)])
    img = C.clone_fill(img, boy, -58)
    fly = C.poly_mask(sh, [(704, 124), (745, 118), (750, 158), (706, 160)])
    img = C.clone_fill(img, fly, -46)
    dr = C.poly_mask(sh, [(710, 192), (712, 165), (740, 160), (760, 145), (782, 124), (802, 130), (822, 156),
                          (862, 176), (870, 215), (840, 246), (770, 248), (732, 236), (712, 222)], grow=3)
    img = C.clone_fill(img, dr & (yy < 212), -150)
    img = C.clone_fill(img, dr & (yy >= 212), 150)
    small = C.fit(img[3:303, 552:1082], (P.W, P.H))
    P.save(P.translate(small, n_colors=96), out)
    return {
        'walk': dict(farY=184, nearY=222, farLeft=40, farRight=440, nearLeft=20, nearRight=460,
                     farHeight=60, nearHeight=72),
        'spots': [],
        'blocked': [],
        'cropTop': 0.6,
    }


if __name__ == '__main__':
    print(build(sys.argv[1]))
