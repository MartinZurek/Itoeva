"""Extrahiert bewegliche Ebenen aus den vorhandenen Bildern, ohne neue Illustrationen.
Aufruf: python3 tools/world-art/living_layers.py [ort ...]
Materialmasken und Kotlin-Katalog werden gemeinsam mit den Atlanten erzeugt.
"""
import json
from io import BytesIO
import sys
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage
from animate import hsv

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'app-sim/src/game/assets/scenes'
CATALOG = ROOT / 'app-sim/src/main/java/com/notime/glyphsim/matrix/GameRoomCatalog.kt'
MATERIALS = ['STONE', 'WOOD', 'GRASS', 'SAND', 'MUD', 'WATER', 'SNOW']
# Bereiche stammen aus den bestehenden 480x270-Bildern. Himmel, Moebel und Stege bleiben getrennt.
ROOMS = {
 'living': dict(ground='WOOD', leaf=[(5,0,160,90),(158,87,196,135),(205,20,242,88),(331,92,414,147),(302,138,357,199),(0,209,135,270)], fire=(72,154,103,184)),
 'bedroom': dict(ground='WOOD', leaf=[(0,0,480,72),(5,94,69,165),(380,64,480,240)], curtain=[(216,78),(277,78),(265,105),(246,137),(219,162)]),
 'street': dict(ground='STONE', leaf=[(0,0,480,150),(0,155,480,214)]),
 'city': dict(ground='STONE', leaf=[(54,92,195,180)], cloud=(0,0,480,45)),
 'sport': dict(ground='STONE', leaf=[(130,0,318,128),(404,0,480,155),(0,212,480,270)], cloud=(0,0,158,48)),
 'park': dict(ground='GRASS', leaf=[(0,0,280,138),(305,0,480,115),(0,203,470,270)], water=[[(285,240),(480,232),(480,270),(200,270)]]),
 'pond': dict(ground='WOOD', leaf=[(0,0,200,142),(0,217,480,270)], water=[[(292,137),(480,132),(480,263),(296,247)],[(15,173),(180,164),(178,228),(8,238)]], protect=[(0,150,305,174),(166,151,207,230),(231,151,273,229)]),
 'forest': dict(ground='GRASS', leaf=[(0,0,480,157),(0,185,260,270),(320,224,480,270)]),
 'meadow': dict(ground='GRASS', leaf=[(0,0,112,180),(399,0,480,172),(0,195,480,270)], cloud=(104,0,397,57)),
 'mountains': dict(ground='STONE', leaf=[(297,107,394,189),(0,223,480,270)], cloud=(0,0,480,58)),
 'plains': dict(ground='GRASS', leaf=[(0,0,198,119),(0,206,480,270)], cloud=(166,0,480,91)),
 'camp': dict(ground='STONE', leaf=[(224,27,299,155),(401,0,480,113),(0,229,480,270)], fire=(111,178,148,211), water=[[(0,126),(195,126),(181,154),(0,157)]]),
 'beach': dict(ground='SAND', leaf=[(0,0,145,106),(0,178,114,270)], cloud=(124,0,480,67), water=[[(103,104),(480,99),(480,173),(298,170),(175,157),(128,137)]], protect=[(321,97,381,208),(0,134,145,185)]),
 'jungle': dict(ground='GRASS', leaf=[(0,0,480,130),(0,137,91,270),(364,107,480,270)]),
 'swamp': dict(ground='MUD', leaf=[(0,0,282,125),(0,226,480,270)], water=[[(207,146),(480,143),(480,216),(200,212),(170,184)]], protect=[(198,187,375,222)]),
 'grotto': dict(ground='STONE', leaf=[], water=[[(98,178),(309,180),(392,211),(83,216)]]),
}


def polygon(shape, points):
    im = Image.new('1', (shape[1], shape[0]))
    ImageDraw.Draw(im).polygon(points, fill=1)
    return np.asarray(im, dtype=bool)


def save_image(array, path):
    encoded = BytesIO()
    Image.fromarray(array).save(encoded, format='PNG', optimize=True)
    data = encoded.getvalue()
    with Image.open(BytesIO(data)) as check:
        check.load()
    temporary = path.with_suffix('.tmp')
    temporary.write_bytes(data)
    temporary.replace(path)


def extract(name):
    config = ROOMS[name]
    rgb = np.asarray(Image.open(ASSETS / f'{name}.png').convert('RGB'))
    h, s, v = hsv(rgb.astype(float) / 255)
    wet = np.zeros((270,480), bool)
    for p in config.get('water', []):
        wet |= polygon(wet.shape, p)
    # Holz und Vegetation innerhalb einer Wasserregion sind feste Inseln, keine Wellen.
    wood = (h >= 10) & (h <= 55) & (s > .3) & (v < .62)
    green = (h >= 55) & (h <= 165) & (s > .18) & (v > .1)
    wet &= ~ndimage.binary_dilation(wood | green, iterations=1)
    for x0,y0,x1,y1 in config.get('protect', []):
        wet[y0:y1,x0:x1] = False
    grid = np.full((270,480), MATERIALS.index(config['ground']), np.uint8)
    grid[wet] = MATERIALS.index('WATER')
    if name in ('forest','jungle','park','meadow','plains','camp','mountains'):
        grid[(h >= 15) & (h < 55) & (s > .2) & ~wet] = MATERIALS.index('STONE')
    if name == 'pond':
        grid[~wet] = MATERIALS.index('WOOD')
    parts = []
    removed = np.zeros((270,480), bool)
    def add(mask, kind, foreground=False, remove=True):
        if mask.sum() < 12:
            return
        ys,xs = np.where(mask)
        x0,x1,y0,y1 = xs.min(),xs.max()+1,ys.min(),ys.max()+1
        crop = np.zeros((y1-y0,x1-x0,4), np.uint8)
        crop[:,:,:3] = rgb[y0:y1,x0:x1]
        crop[:,:,3] = mask[y0:y1,x0:x1]*255
        parts.append(dict(kind=kind,x=int(x0),y=int(y0),w=int(x1-x0),h=int(y1-y0),foreground=foreground,crop=crop))
        if remove:
            removed[:] |= mask
    eligible = (h >= 38) & (h <= 165) & (s > .16) & (v > .12) & ~wet
    for x0,y0,x1,y1 in config['leaf']:
        for y in range(y0,y1,80):
            for x in range(x0,x1,120):
                mask = np.zeros((270,480), bool)
                mask[y:min(y+80,y1),x:min(x+120,x1)] = eligible[y:min(y+80,y1),x:min(x+120,x1)]
                mask &= ~removed
                mask = ndimage.binary_opening(mask, structure=np.ones((2,2)))
                front = y >= 220 and name not in ('living','bedroom','street','city')
                add(mask, 'GRASS' if y >= 200 else 'LEAF', front)
    if 'cloud' in config:
        x0,y0,x1,y1 = config['cloud']
        cloud = np.zeros((270,480),bool)
        cloud[y0:y1,x0:x1] = (((s < .27) & (v > .73)) | ((h > 20) & (h < 65) & (s < .5) & (v > .8)))[y0:y1,x0:x1]
        cloud = ndimage.binary_opening(cloud, structure=np.ones((2,2)))
        labels,count = ndimage.label(ndimage.binary_dilation(cloud,iterations=3))
        for i in range(1,count+1):
            add(cloud & (labels==i), 'CLOUD')
    if 'curtain' in config:
        add(polygon(wet.shape,config['curtain']), 'CURTAIN')
    if 'fire' in config:
        x0,y0,x1,y1 = config['fire']
        mask = np.zeros((270,480),bool)
        mask[y0:y1,x0:x1] = ((h < 65) & (v > .65) & (s > .25))[y0:y1,x0:x1]
        add(mask, 'FIRE')
    # Schmale, vollstaendig nasse Streifen: die Laufzeit kann exakt an der Uferkante clippen.
    for y in range(100,265,5):
        row = wet[y:y+3].all(axis=0)
        edges = np.diff(np.r_[False,row,False].astype(int))
        for x0,x1 in zip(np.where(edges==1)[0],np.where(edges==-1)[0]):
            if x1-x0 >= 18:
                mask = np.zeros((270,480),bool)
                mask[y:y+3,x0:x1] = True
                add(mask, 'WATER', remove=False)
    base = rgb.copy()
    if removed.any():
        _, indices = ndimage.distance_transform_edt(removed,return_indices=True)
        base[removed] = rgb[indices[0][removed],indices[1][removed]]
    # Gepackter Atlas, keine Ganzbild-Bitmaps pro Pflanze und keine Laufzeit-Segmentierung.
    x=y=rowh=0
    for p in parts:
        if x+p['w']+2 > 1024:
            x=0; y+=rowh+2; rowh=0
        p['sx'],p['sy'] = x,y
        x+=p['w']+2; rowh=max(rowh,p['h'])
    atlas = np.zeros((max(1,y+rowh),1024,4), np.uint8)
    for p in parts:
        atlas[p['sy']:p['sy']+p['h'],p['sx']:p['sx']+p['w']] = p.pop('crop')
    assert atlas.shape[0] <= 2048
    save_image(base, ASSETS/f'{name}_live.png')
    save_image(atlas, ASSETS/f'{name}_parts.png')
    (ASSETS/f'{name}_materials.bin').write_bytes(grid.tobytes())
    return dict(parts=parts,material=config['ground'])


def write_catalog(rooms):
    lines = ['// Generiert von tools/world-art/living_layers.py - nicht von Hand aendern.',
             'package com.notime.glyphsim.matrix','', 'import com.notime.glyphsim.matrix.GameEnvironment.*',
             '', 'internal object GameRoomCatalog {', '    val rooms: Map<PlayScene.Place, Room> = mapOf(']
    # Kein object-Wildcard-Import in Kotlin: Typen werden einzeln eingefuehrt.
    lines[3] = '\n'.join('import com.notime.glyphsim.matrix.GameEnvironment.'+t for t in ['Kind','Material','Part','Room'])
    for name,room in sorted(rooms.items()):
        lines.append(f'        PlayScene.Place.{name.upper()} to Room(listOf(')
        for i,p in enumerate(room['parts']):
            args = ', '.join(str(p[k]) for k in ('sx','sy','w','h','x','y'))
            lines.append(f'            Part(Kind.{p["kind"]}, {args}, {i}, {str(p["foreground"]).lower()}),')
        lines.append(f'        ), Material.{room["material"]}),')
    lines += ['    )','}','']
    CATALOG.write_text('\n'.join(lines))


def main():
    meta_path = Path(__file__).with_name('living_parts.json')
    rooms = json.loads(meta_path.read_text()) if meta_path.exists() else {}
    for name in sys.argv[1:] or sorted(ROOMS):
        rooms[name] = extract(name)
        print(name, len(rooms[name]['parts']), 'bewegliche Teile')
    meta_path.write_text(json.dumps(rooms,indent=2)+'\n')
    write_catalog(rooms)

if __name__ == '__main__':
    main()
