"""MOUNTAINS - aus Konzeptbuch Band 2 (world-conceptbook-v2/images/mountains-pixel-concept-v1.jpg),
eins zu eins uebernommen und auf die Spielpalette gebracht (places/_landscape.py).
Bergpass: Schneegipfel, Wolkenmeer, Tannen, Wegweiser, Steinbank am Felsweg. Eule und Wanderer aus dem Bild genommen."""
import sys
sys.path.insert(0, __file__.rsplit('/', 1)[0])
import _landscape as L
import pixelate as P

BOX = (93, 0, 930, 471)


def build(out):
    img = L.render('mountains', BOX, clones=[([(226, 302), (256, 302), (256, 332), (226, 332)], 30, 0), ([(306, 276), (380, 276), (380, 332), (306, 332)], 80, 0), ([(306, 332), (366, 332), (366, 346), (306, 346)], -60, 0), ([(364, 332), (382, 332), (382, 362), (364, 362)], 26, 0)], figures=[], text=(405, 4, 615, 52))
    P.save(img, out)
    return {
        'walk': dict(farY=209.2, nearY=246.0, farLeft=20, farRight=460, nearLeft=10, nearRight=470,
                     farHeight=48, nearHeight=58),
        'spots': [dict(station='BENCH', box=[61.4, 186.3, 158.9, 215.0], standX=110.2, standY=219.0)],
        'blocked': [],
        'cropTop': 0.7,
    }


if __name__ == '__main__':
    print(build(sys.argv[1]))
