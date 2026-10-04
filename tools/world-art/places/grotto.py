"""GROTTO - aus Konzeptbuch Band 2 (world-conceptbook-v2/images/grotto-pixel-concept-v1.jpg),
eins zu eins uebernommen und auf die Spielpalette gebracht (places/_landscape.py).
Kristallgrotte: Tropfsteine, Amethyst- und Tuerkiskristalle, Steinplattform, Wasserfall und Tuempel im Hintergrund. Kind und Stern aus dem Bild genommen."""
import sys
sys.path.insert(0, __file__.rsplit('/', 1)[0])
import _landscape as L
import pixelate as P

BOX = (120, 29, 897, 466)


def build(out):
    img = L.render('grotto', BOX, clones=[([(316, 328), (380, 328), (380, 388), (316, 388)], -100, 0)], figures=[], text=None)
    P.save(img, out)
    return {
        'walk': dict(farY=211.9, nearY=236.6, farLeft=20, farRight=460, nearLeft=10, nearRight=470,
                     farHeight=48, nearHeight=58),
        'spots': [dict(station='BENCH', box=[169.9, 178.6, 296.5, 205.7], standX=233.2, standY=211.9)],
        'blocked': [],
        'cropTop': 0.7,
    }


if __name__ == '__main__':
    print(build(sys.argv[1]))
