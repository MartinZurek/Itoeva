"""JUNGLE - aus Konzeptbuch Band 2 (world-conceptbook-v2/images/jungle-pixel-concept-v1.jpg),
eins zu eins uebernommen und auf die Spielpalette gebracht (places/_landscape.py).
Dschungel: Palmen, Wasserfaelle, Wurzelbogen, Steinplatte, Bambus, Monstera, Blueten, Lichtstrahlen. Wyrmling aus dem Bild genommen."""
import sys
sys.path.insert(0, __file__.rsplit('/', 1)[0])
import _landscape as L
import pixelate as P

BOX = (174, 24, 1024, 502)


def build(out):
    img = L.render('jungle', BOX, clones=[([(210, 398), (290, 398), (292, 448), (210, 448)], 0, 40)], figures=[], text=(425, 4, 605, 22))
    P.save(img, out)
    return {
        'walk': dict(farY=209.6, nearY=246.0, farLeft=20, farRight=460, nearLeft=10, nearRight=470,
                     farHeight=48, nearHeight=58),
        'spots': [dict(station='BENCH', box=[196.5, 174.5, 281.2, 194.3], standX=238.8, standY=209.6)],
        'blocked': [],
        'cropTop': 0.7,
    }


if __name__ == '__main__':
    print(build(sys.argv[1]))
