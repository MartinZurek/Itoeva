"""NOOK - die Leseecke: das Lesezimmer zur blauen Stunde (reading_room.py, erste Vorlage vom
04.10.) mit Sessel, Regal und Stehlampe."""
import os
import runpy
import sys

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def build(out):
    old = sys.argv
    sys.argv = ['reading_room.py', out]
    try:
        runpy.run_path(os.path.join(HERE, 'reading_room.py'), run_name='__main__')
    finally:
        sys.argv = old
    x3 = out.replace('.png', '_x3.png')
    if os.path.exists(x3):
        os.remove(x3)
    return {
        'walk': dict(farY=172, nearY=236, farLeft=140, farRight=342, nearLeft=26, nearRight=440,
                     farHeight=42, nearHeight=78),
        'spots': [
            dict(station='SEAT', box=[286, 95, 404, 206], standX=330, standY=210),
            dict(station='BOOKSHELF', box=[118, 25, 170, 178], standX=166, standY=182),
            dict(station='LAMP', box=[300, 40, 360, 100], standX=300, standY=200),
        ],
        'blocked': ['UP', 'DOWN'],
        'cropTop': 0.7,
    }


if __name__ == '__main__':
    print(build(sys.argv[1]))
