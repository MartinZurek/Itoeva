"""Importiert echte gezeichnete Schwimmphasen; keine Koerper-Verformung erzeugen.

Vier Zugphasen, drei Ansichten. Ein gemeinsamer Massstab je Wesen und Kopfanker
halten Rumpf und Ohren fest. Der 256er Textrahmen laesst Platz fuer seitlich
ausladende Pfoten und den Schwanz, bei unveraenderter sichtbarer Pixelgroesse.
"""
from pathlib import Path
import argparse
import json
import numpy as np
from PIL import Image
import ensemble_motion as E

HERE = Path(__file__).resolve().parent
ASSETS = HERE / '../../app-sim/src/main/assets/creatures'
NAMES = ('fennec', 'gloop', 'puffling', 'starlet', 'wyrmling', 'hootlet')


def build(name):
    arts = E.drawings(name, source=HERE/'source'/f'{name}-swim-atlas.webp', count=12)
    original = np.asarray(Image.open(ASSETS/f'{name}.png').convert('RGBA'))
    neutral = original[:, 27*128:28*128]
    y, x = np.nonzero(neutral[..., 3] > 128)
    top = int(y.min())
    # Bewegte Beine werden nie einzeln auf eine Standhoehe gestreckt.
    factor = (126-top) / np.median([art.shape[0] for art in arts[4:8]])
    side = original[:, :128]
    sy, sx = np.nonzero(side[..., 3] > 128)
    head = sy < sy.min() + (126-sy.min())*.35
    side_anchor = (sx[head].min()+sx[head].max())/2 + 64
    frames = []
    for index, art in enumerate(arts):
        source = Image.fromarray(np.uint8(art*255), 'RGBA')
        small = np.asarray(source.resize((round(source.width*factor), round(source.height*factor)),
            Image.Resampling.NEAREST)).copy()
        mask = small[..., 3] > 128
        small[~mask] = 0
        small[mask, 3] = 255
        yy, xx = np.nonzero(mask)
        head = yy < yy.min()+(yy.max()-yy.min()+1)*.35
        cx = (xx[head].min()+xx[head].max())/2
        dx = round((side_anchor if index < 4 else 128)-cx)
        dy = 64+top-int(yy.min())
        assert (xx+dx).min()>2 and (xx+dx).max()<253, f'{name}: breite Pose angeschnitten'
        assert (yy+dy).min()>2 and (yy+dy).max()<253, f'{name}: hohe Pose angeschnitten'
        frame = np.zeros((256,256,4), dtype=np.uint8)
        frame[yy+dy, xx+dx] = small[yy, xx]
        frames.append(frame)
    assert len({frame.tobytes() for frame in frames}) == 12
    Image.fromarray(np.concatenate(frames,axis=1), 'RGBA').save(ASSETS/f'{name}-swim.png',optimize=True)
    return dict(name=name, scale=float(factor), top=top, frames=12, frameSize=256)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('names', nargs='*', default=NAMES)
    args = parser.parse_args()
    print(json.dumps([build(name) for name in args.names],indent=2))
