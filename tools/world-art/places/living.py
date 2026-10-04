"""LIVING - das Wohnzimmer aus der Wohnraum-Studie (world-studies/cozy-home, linke Haelfte),
eins zu eins in Pixel-Art uebersetzt (pixelate.py): Sofa mit Kissen, Teetisch, Kamin, Bogenfenster,
Bilder, Buecherregal, Klavier, Pendelleuchte, Efeu und Pflanzen. Der Drache am Klavier ist aus dem
Bild genommen - an seiner Stelle stehen Topf- und Haengepflanze und das Klavier aus der
Objektspalte desselben Studienblatts. Seitenansicht wie in der Studie, keine Fluchtpunkte."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
from scipy import ndimage
from px import MaskPen
import concept as C
import pixelate as P

DRAGON = [(312, 150), (322, 128), (336, 110), (362, 106), (382, 116), (380, 138), (368, 150), (374, 162),
          (368, 174), (356, 180), (354, 198), (330, 200), (316, 198), (304, 190), (302, 165)]


def build(out):
    img = C.crop('home', (16, 0, 549, 300), (P.W, P.H))
    m = ndimage.binary_dilation(MaskPen(P.W, P.H).poly(DRAGON).a > 0, iterations=2)
    img = C.inpaint(img, m)
    C.paste(img, C.sprite('hanging_plant', 1.05), 352, 156, shadow=0, tint=(0.85, 0.74, 0.58))
    C.paste(img, C.sprite('piano', 1.25), 398, 224, tint=(0.72, 0.58, 0.46))
    C.paste(img, C.sprite('plant', 1.35), 326, 226, tint=(0.88, 0.78, 0.62))
    P.save(P.translate(img), out)
    return {
        'walk': dict(farY=208, nearY=228, farLeft=132, farRight=444, nearLeft=126, nearRight=450,
                     farHeight=64, nearHeight=70),
        'spots': [
            dict(station='SEAT', box=[150, 145, 305, 205], standX=230, standY=212),
            dict(station='TV', box=[345, 128, 448, 224], standX=398, standY=216),
            dict(station='DOOR', box=[56, 104, 132, 214], standX=140, standY=212),
        ],
        'blocked': ['UP', 'DOWN'],
        'cropTop': 0.5,
    }


if __name__ == '__main__':
    print(build(sys.argv[1]))
