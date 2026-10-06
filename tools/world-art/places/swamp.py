"""SWAMP - aus Konzeptbuch Band 2 (world-conceptbook-v2/images/swamp-pixel-concept-v1.jpg),
eins zu eins uebernommen und auf die Spielpalette gebracht (places/_landscape.py).
Sumpf bei Nacht: Mangrovenbaum mit Laternen, Gluehwuermchen, Mond ueber dem Wasser, Steg mit Laterne, Rohrkolben, Baumstumpf. Gloop auf dem Steg aus dem Bild genommen."""
import sys
sys.path.insert(0, __file__.rsplit('/', 1)[0])
import _landscape as L
import pixelate as P

BOX = (60, 27, 819, 454)


def build(out):
    img = L.render('swamp', BOX, clones=[([(478, 302), (516, 302), (516, 331), (478, 331)], -60, 0)], figures=[], text=None)
    P.save(img, out)
    return {
        'walk': dict(farY=195.0, nearY=246.0, farLeft=20, farRight=460, nearLeft=10, nearRight=470,
                     farHeight=44, nearHeight=58),
        'spots': [dict(station='BENCH', box=[202.4, 190.3, 370.0, 218.1], standX=286.2, standY=234.0)],
        'blocked': [],
        'cropTop': 0.7,
    }


if __name__ == '__main__':
    print(build(sys.argv[1]))
