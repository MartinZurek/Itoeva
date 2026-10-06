"""PLAINS - aus Konzeptbuch Band 2 (world-conceptbook-v2/images/plains-pixel-concept-v1.jpg),
eins zu eins uebernommen und auf die Spielpalette gebracht (places/_landscape.py).
Weite Ebene: Eiche, Heuballen, Holzzaun, Felder, Zypressen, Windmuehle, Huegel im Abendlicht. Fennec aus dem Bild genommen."""
import sys
sys.path.insert(0, __file__.rsplit('/', 1)[0])
import _landscape as L
import pixelate as P

BOX = (0, 0, 878, 494)


def build(out):
    img = L.render('plains', BOX, clones=[([(412, 338), (472, 338), (474, 392), (412, 392)], -70, 0)], figures=[], text=(425, 8, 605, 30))
    P.save(img, out)
    return {
        'walk': dict(farY=207.7, nearY=246.0, farLeft=20, farRight=460, nearLeft=10, nearRight=470,
                     farHeight=48, nearHeight=58),
        'spots': [dict(station='BENCH', box=[159.6, 157.4, 239.5, 206.6], standX=199.6, standY=210.6)],
        'blocked': [],
        'cropTop': 0.7,
    }


if __name__ == '__main__':
    print(build(sys.argv[1]))
