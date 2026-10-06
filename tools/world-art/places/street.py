"""STREET - das Uferviertel aus der Studie world-studies/itoeva-riverside-quarter (linke Haelfte des
oberen Bildes), eins zu eins in Pixel-Art uebersetzt (pixelate.py): Cafe mit gruener Markise,
Laternen und Schaufenster, Tische mit Stuehlen, Kreidetafel, Buchladen mit Buecherwand, Toepfe und
Kuebel voller Blumen, Efeu, Bank unter dem Baum, Promenade auf der Ufermauer. Fennec, Hase, Gloop
und Katze sind aus dem Bild genommen; Stuhl, Kuebel und Bank hinter ihnen aus der Umgebung
fortgesetzt."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
import concept as C
import pixelate as P


def build(out):
    img = C.native('river', (0, 0, 1200, 552))
    sh = img.shape[:2]
    yy = np.mgrid[0:sh[0], 0:sh[1]][0]
    cat = C.poly_mask(sh, [(688, 358), (726, 356), (728, 430), (688, 430)])
    img = C.clone_fill(img, cat, 40)
    fox = C.poly_mask(sh, [(118, 356), (140, 348), (170, 352), (180, 380), (182, 450), (114, 450), (112, 390)])
    img = C.clone_fill(img, fox, 105)
    rabbit = C.poly_mask(sh, [(472, 354), (492, 350), (514, 356), (520, 390), (518, 448), (470, 448), (468, 390)])
    img = C.clone_fill(img, rabbit, 44)
    gloop = C.poly_mask(sh, [(580, 372), (600, 362), (642, 364), (660, 385), (664, 470), (578, 470), (574, 400)])
    img = C.clone_fill(img, gloop & (yy < 438), 74)
    rng = np.random.default_rng(4)
    for y in range(438, 472):        # Bankbretter waagerecht fortsetzen
        for x in np.nonzero(gloop[y])[0]:
            img[y, x] = img[y, 664 + (x - 664) % 22] * (1 + rng.uniform(-0.03, 0.03))
    small = C.fit(img[210:550, 0:605], (P.W, P.H))
    P.save(P.translate(small, n_colors=96), out)
    return {
        'walk': dict(farY=196, nearY=212, farLeft=4, farRight=470, nearLeft=0, nearRight=476,
                     farHeight=60, nearHeight=66),
        'spots': [
            dict(station='LAMP', box=[70, 60, 96, 100], standX=84, standY=204),
            dict(station='BENCH', box=[436, 158, 480, 210], standX=452, standY=204),
        ],
        'blocked': [],
        'cropTop': 0.5,
    }


if __name__ == '__main__':
    print(build(sys.argv[1]))
