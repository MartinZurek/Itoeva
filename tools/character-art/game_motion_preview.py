"""Rastervorschau der exportierten produktiven Kotlin-Netze, keine eigene Bewegungsrechnung.

Aufruf nach CharacterMotionPreviewKt: python3 game_motion_preview.py EXPORT ZIEL.gif
Das Ergebnis ist ein Software-Spritevergleich, keine Android-/Telefonaufnahme.
"""
from pathlib import Path
import sys
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import map_coordinates

HERE = Path(__file__).resolve().parent
NAMES = ('fennec', 'puffling', 'wyrmling', 'gloop', 'starlet', 'hootlet')
COLS, ROWS, SIZE = 24, 32, 256


def render_mesh(source, vertices):
    # Stueckweise affine Rasterung derselben Dreiecke wie Android drawBitmapMesh.
    rgba = np.asarray(source, dtype=float) / 255
    rgba[..., :3] *= rgba[..., 3:]
    coords = np.full((2, SIZE, SIZE), -10., dtype=float)
    src = np.stack(np.meshgrid(np.linspace(0, 128, COLS+1), np.linspace(0, 128, ROWS+1)), -1)
    dest = vertices.reshape(ROWS+1, COLS+1, 2)*SIZE
    for row in range(ROWS):
        for col in range(COLS):
            for indices in (((row,col),(row,col+1),(row+1,col)),
                            ((row,col+1),(row+1,col+1),(row+1,col))):
                d = np.array([dest[i] for i in indices])
                s = np.array([src[i] for i in indices])
                low = np.maximum(np.floor(d.min(0)).astype(int), 0)
                high = np.minimum(np.ceil(d.max(0)).astype(int), SIZE)
                if np.any(high <= low):
                    continue
                x,y = np.meshgrid(np.arange(low[0],high[0])+.5, np.arange(low[1],high[1])+.5)
                inv = np.linalg.inv(np.column_stack((d[1]-d[0], d[2]-d[0])))
                uv = inv @ np.stack((x-d[0,0],y-d[0,1])).reshape(2,-1)
                inside = (uv >= -1e-5).all(0) & (uv.sum(0) <= 1.00001)
                p = s[0,None]+uv[0,:,None]*(s[1]-s[0])+uv[1,:,None]*(s[2]-s[0])
                cx,cy = x.astype(int).ravel()[inside], y.astype(int).ravel()[inside]
                coords[:,cy,cx] = p[inside,::-1].T-.5
    out = np.stack([map_coordinates(rgba[...,c],coords,order=1,mode='constant',cval=0) for c in range(4)],-1)
    out[...,:3] /= np.maximum(out[...,3:],1e-6)
    return Image.fromarray(np.uint8(np.clip(out,0,1)*255),'RGBA')


def preview(folder, target):
    sheets = {n: Image.open(HERE/'../../app-sim/src/main/assets/creatures'/f'{n}.png').convert('RGBA') for n in NAMES}
    frames = []
    for tick in range(80):
        bg = Image.new('RGBA',(1140,490),(36,49,45,255))
        d = ImageDraw.Draw(bg)
        title = 'Ruhe / Wind' if tick < 16 else 'Gang / Stoffnachlauf' if tick < 40 else 'Anhalten / Ausschwingen'
        d.text((14,8),f'{title}   |   links bisher, rechts ueberarbeitet',fill=(246,229,207))
        d.text((14,470),'Produktive Kotlin-Netzrechnung; Softwarevorschau, keine APK-Aufnahme.',fill=(174,191,181))
        for panel,n in enumerate(NAMES):
            role,top,relative = map(float,(folder/f'{n}-{tick}.csv').read_text().splitlines()[0].split(','))
            vertices = np.fromstring((folder/f'{n}-{tick}.csv').read_text().splitlines()[1],sep=',')
            original = sheets[n].crop((int(role)*128,0,(int(role)+1)*128,128))
            # Gleicher Weltmassstab statt Kopf-/Schwanz-abhängiger Bildrahmengroesse.
            size = round(128 * 125 * relative / (126-top))
            modified = render_mesh(original,vertices).resize((size,size),Image.Resampling.LANCZOS)
            original = original.resize((size,size),Image.Resampling.NEAREST)
            x,y = panel%3*380, panel//3*215+40
            d.text((x+14,y),n.capitalize(),fill=(245,231,203))
            for offset,image in ((0,original),(190,modified)):
                bg.alpha_composite(image,(x+offset+(190-size)//2,y+195-round(size*126/128)))
                d.line((x+offset+14,y+195,x+offset+176,y+195),fill=(94,114,100))
        frames.append(bg.convert('RGB'))
    frames[0].save(target,save_all=True,append_images=frames[1:],duration=50,loop=0,optimize=True)


if __name__ == '__main__':
    preview(Path(sys.argv[1]),Path(sys.argv[2]))
