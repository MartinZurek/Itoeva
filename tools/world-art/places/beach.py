"""BEACH - aus Konzeptbuch Band 2 (world-conceptbook-v2/images/beach-pixel-concept-v1.jpg),
eins zu eins uebernommen und auf die Spielpalette gebracht (places/_landscape.py).
Strand: Palme, Felsen, Wellen, Sonnenschirm mit Liegestuhl, Sandburg, Treibholz, Duenengras. Fennec aus dem Bild genommen."""
import sys
sys.path.insert(0, __file__.rsplit('/', 1)[0])
import _landscape as L
import pixelate as P

BOX = (0, 0, 848, 477)


def build(out):
    img = L.render('beach', BOX, clones=[([(505, 315), (573, 315), (575, 370), (505, 370)], -90, 0)], figures=[], text=(395, 6, 630, 26))
    P.save(img, out)
    return {
        'walk': dict(farY=158.0, nearY=246.0, farLeft=20, farRight=460, nearLeft=10, nearRight=470,
                     farHeight=42, nearHeight=58),
        'spots': [dict(station='BENCH', box=[328.3, 163.0, 380.4, 210.6], standX=354.4, standY=222.0)],
        'blocked': [],
        'cropTop': 0.7,
    }


if __name__ == '__main__':
    print(build(sys.argv[1]))
