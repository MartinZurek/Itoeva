"""POND - der Waldsee aus der Studie world-studies/woodland-lake, eins zu eins in Pixel-Art
uebersetzt (pixelate.py): Steg aus Holz, Ende des Lesepavillons mit Topfpflanzen, Birken, Herbst-
laub, Tannenufer, Insel, Berge im Abendlicht, spiegelnder See mit Seerosen und Schilf. Der Angler
auf dem Steg ist aus dem Bild genommen (Stegbretter und Pfosten aus der Umgebung fortgesetzt),
ebenso Gloop im Pavillon."""
import sys
sys.path.insert(0, __file__.rsplit('/', 2)[0])
import numpy as np
from scipy import ndimage
from px import MaskPen
import concept as C
import pixelate as P


def build(out):
    img = C.native('lake', (0, 0, 1085, 312))
    sh = img.shape[:2]
    yy = np.mgrid[0:sh[0], 0:sh[1]][0]
    boy = C.poly_mask(sh, [(484, 118), (500, 110), (524, 114), (532, 140), (536, 160), (530, 182), (486, 184), (478, 160)])
    img = C.clone_fill(img, boy & (yy < 166), -70)
    img = C.clone_fill(img, boy & (yy >= 166), -64)
    rod = ndimage.binary_dilation(MaskPen(1085, 312).line([(524, 152), (586, 96)], 3).a > 0, iterations=1)
    rod |= ndimage.binary_dilation(MaskPen(1085, 312).line([(586, 98), (588, 160), (602, 225)], 1).a > 0, iterations=1)
    img = C.clone_fill(img, rod & ~boy, -10)
    gloop = C.poly_mask(sh, [(184, 150), (196, 130), (218, 124), (240, 128), (258, 150), (268, 172), (262, 188), (186, 188), (182, 170)])
    img = C.inpaint(img, gloop)
    C.paste(img, C.sprite('plant', 1.15), 206, 190, shadow=0.2, tint=(1.0, 0.85, 0.65))
    C.paste(img, C.sprite('teatable', 0.85), 244, 190, shadow=0.2, tint=(1.0, 0.85, 0.65))
    small = C.fit(img[3:303, 236:769], (P.W, P.H))
    P.save(P.translate(small, n_colors=96), out)
    return {
        'walk': dict(farY=165, nearY=188, farLeft=10, farRight=318, nearLeft=4, nearRight=240,
                     farHeight=56, nearHeight=64),
        'spots': [],
        'blocked': [],
        'cropTop': 0.6,
    }


if __name__ == '__main__':
    print(build(sys.argv[1]))
