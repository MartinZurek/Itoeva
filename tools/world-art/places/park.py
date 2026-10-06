"""PARK - der Park aus der Studie world-studies/park-sports-ground (linke Haelfte), eins zu eins
in Pixel-Art uebersetzt (pixelate.py): alter Baum mit Bank, Laterne, Blumenbeet, Plattenweg,
Holzzaun, Haeuschen und Stadt mit Turm, Teich mit Enten und Seerosen, Margeriten und Lupinen.
Eule, Fennec und Katze sind aus dem Bild genommen (die Wesen laufen im Spiel selbst): Weg und
Zaun hinter ihnen aus der Umgebung fortgesetzt, die Bank Brett fuer Brett ergaenzt."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
import concept as C
import pixelate as P


def build(out):
    img = C.native('park', (0, 0, 1085, 310))
    sh = img.shape[:2]
    yy = np.mgrid[0:sh[0], 0:sh[1]][0]
    cat = C.poly_mask(sh, [(498, 152), (526, 152), (528, 190), (498, 190)])
    img = C.clone_fill(img, cat, -36)
    fox = C.poly_mask(sh, [(298, 153), (318, 150), (345, 170), (372, 168), (392, 172), (404, 215), (400, 238),
                           (372, 240), (330, 236), (316, 232), (310, 215), (300, 190)])
    img = C.clone_fill(img, fox & (yy < 212), 112)
    img = C.clone_fill(img, fox & (yy >= 212), -96)
    owl = C.poly_mask(sh, [(165, 140), (175, 124), (200, 121), (218, 128), (226, 150), (232, 170), (232, 194),
                           (166, 194), (160, 170)])
    # Die Bankbretter laufen waagerecht: jede Zeile aus dem freien Stueck links fortsetzen
    rng = np.random.default_rng(3)
    for y in range(152, 198):
        for x in np.nonzero(owl[y])[0]:
            img[y, x] = img[y, 100 + (x - 100) % 38] * (1 + rng.uniform(-0.03, 0.03))
    img = C.inpaint(img, owl & (yy < 152))
    C.paste(img, C.sprite('flowers2', 1.4), 205, 156, shadow=0, tint=(0.95, 0.92, 0.85))
    small = C.fit(img[3:303, 16:549], (P.W, P.H))
    P.save(P.translate(small, n_colors=96), out)
    return {
        'walk': dict(farY=186, nearY=216, farLeft=150, farRight=470, nearLeft=30, nearRight=470,
                     farHeight=60, nearHeight=72),
        'spots': [dict(station='BENCH', box=[62, 134, 212, 191], standX=150, standY=205.0)],
        'blocked': [],
        'cropTop': 0.6,
    }


if __name__ == '__main__':
    print(build(sys.argv[1]))
