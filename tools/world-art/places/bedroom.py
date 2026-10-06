"""BEDROOM - das Schlafzimmer aus der Wohnraum-Studie (world-studies/cozy-home, rechte Haelfte),
eins zu eins in Pixel-Art uebersetzt (pixelate.py): grosses Fenster mit Mond und Tannen,
Fensterbank mit Laterne und Pflanzen, Kommode, Stuhl, Truhe, Bett unter dem Vorhang, Nachttisch
mit Lampe, Schlafboden mit Leiter, Efeu. Stern und Gloop sind aus dem Bild genommen: auf der
Fensterbank steht eine Topfpflanze, im Bett liegen Kissen und Decke aus dem Bett der Objektspalte."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
from scipy import ndimage
from px import MaskPen
import concept as C
import pixelate as P

STAR = [(28, 130), (44, 120), (52, 104), (60, 120), (78, 124), (68, 138), (72, 154), (52, 148), (34, 156), (36, 138)]
GLOOP = [(270, 172), (272, 150), (285, 128), (305, 120), (330, 124), (345, 140), (356, 160), (352, 178),
         (336, 172), (318, 170), (300, 176)]


def build(out):
    img = C.crop('home', (552, 0, 1085, 300), (P.W, P.H))
    star = ndimage.binary_dilation(MaskPen(P.W, P.H).poly(STAR).a > 0, iterations=2)
    gloop = ndimage.binary_dilation(MaskPen(P.W, P.H).poly(GLOOP).a > 0, iterations=2)
    img = C.inpaint(img, star | gloop)
    # Fensterkreuz hinter dem Stern wieder einziehen
    img[95:150, 39:41] = img[95:150, 98:100]
    img[127:129, 0:95] = img[127:129, 0:95].mean(0)
    C.paste(img, C.sprite('plant', 0.75), 52, 151, shadow=0.2, tint=(0.95, 0.85, 0.7))
    C.paste(img, C.sprite('bed', 1.3), 262, 233, shadow=0, tint=(0.8, 0.7, 0.58), clip=gloop)
    P.save(P.translate(img), out)
    return {
        'walk': dict(farY=212, nearY=234, farLeft=20, farRight=372, nearLeft=14, nearRight=378,
                     farHeight=64, nearHeight=70),
        'spots': [
            dict(station='BED', box=[215, 120, 362, 232], standX=285, standY=234.0),
            dict(station='DOOR', box=[0, 40, 26, 232], standX=26, standY=222),
        ],
        'blocked': ['UP', 'DOWN'],
        'cropTop': 0.5,
    }


if __name__ == '__main__':
    print(build(sys.argv[1]))
