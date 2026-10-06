"""CAMP - aus Konzeptbuch Band 2 (world-conceptbook-v2/images/camp-pixel-concept-v1.jpg),
eins zu eins uebernommen und auf die Spielpalette gebracht (places/_landscape.py).
Lager in der Daemmerung: Feuer mit Kessel, Zelt mit Laterne, Rucksack, Baumstamm, Tannen und Bergsee. Fennec auf dem Stamm aus dem Bild genommen."""
import sys
sys.path.insert(0, __file__.rsplit('/', 1)[0])
import _landscape as L
import pixelate as P

BOX = (264, 0, 1024, 427)


def build(out):
    img = L.render('camp', BOX, clones=[([(596, 236), (700, 236), (700, 287), (596, 287)], 0, -50), ([(596, 286), (700, 286), (700, 297), (596, 297)], -60, 0)], figures=[], text=(375, 6, 640, 30))
    P.save(img, out)
    return {
        'walk': dict(farY=196.0, nearY=246.0, farLeft=20, farRight=460, nearLeft=10, nearRight=470,
                     farHeight=48, nearHeight=58),
        'spots': [dict(station='BENCH', box=[174.3, 180.2, 322.7, 221.3], standX=248.5, standY=241.0)],
        'blocked': [],
        'cropTop': 0.7,
    }


if __name__ == '__main__':
    print(build(sys.argv[1]))
