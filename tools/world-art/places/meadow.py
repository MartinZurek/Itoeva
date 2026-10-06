"""MEADOW - aus Konzeptbuch Band 2 (world-conceptbook-v2/images/meadow-pixel-concept-v1.jpg),
eins zu eins uebernommen und auf die Spielpalette gebracht (places/_landscape.py).
Blumenwiese: bluehende Obstbaeume, Trockenmauer, Sitzstamm, Fingerhut und Margeriten, See und Wald in der Ferne. Starlet aus dem Bild genommen, der Schmetterling bleibt."""
import sys
sys.path.insert(0, __file__.rsplit('/', 1)[0])
import _landscape as L
import pixelate as P

BOX = (130, 0, 893, 429)


def build(out):
    img = L.render('meadow', BOX, clones=[([(455, 282), (503, 282), (503, 332), (455, 332)], -70, 0)], figures=[], text=(395, 8, 630, 30))
    P.save(img, out)
    return {
        'walk': dict(farY=204.5, nearY=246.0, farLeft=20, farRight=460, nearLeft=10, nearRight=470,
                     farHeight=48, nearHeight=58),
        'spots': [dict(station='BENCH', box=[289.4, 146.0, 479, 195.1], standX=384.2, standY=204.5)],
        'blocked': [],
        'cropTop': 0.7,
    }


if __name__ == '__main__':
    print(build(sys.argv[1]))
