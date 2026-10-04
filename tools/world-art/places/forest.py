"""FOREST - aus Konzeptbuch Band 2 (world-conceptbook-v2/images/forest-pixel-concept-v1.jpg),
eins zu eins uebernommen und auf die Spielpalette gebracht (places/_landscape.py).
Waldweg: alte Eichen mit Moos, Lichtstrahlen, Farne, Moossteine, Sitzstamm mit Baumpilzen. Puffling aus dem Bild genommen."""
import sys
sys.path.insert(0, __file__.rsplit('/', 1)[0])
import _landscape as L
import pixelate as P

BOX = (200, 24, 975, 460)


def build(out):
    img = L.render('forest', BOX, clones=[([(330, 345), (385, 345), (388, 405), (330, 405)], -110, 0)], figures=[], text=None)
    P.save(img, out)
    return {
        'walk': dict(farY=226.7, nearY=246.0, farLeft=20, farRight=460, nearLeft=10, nearRight=470,
                     farHeight=48, nearHeight=58),
        'spots': [dict(station='BENCH', box=[260.1, 164.7, 479, 223.6], standX=369.6, standY=227.6)],
        'blocked': [],
        'cropTop': 0.7,
    }


if __name__ == '__main__':
    print(build(sys.argv[1]))
