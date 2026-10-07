"""Gezeichnete Gangphasen der fuenf Wesen in Fennecs bestehenden Bogen importieren.

Die Konzeptbilder sind Identitaetsreferenzen, keine ausgeschnittenen Spielposen.
Der Atlas enthaelt neue Zeichnungen. Nur kleine Regungen und die Drehung der
kompakten Rollpose entstehen mit dem vorhandenen Puppet-Werkzeug. Alle Bilder sind
vorberechnet; Android erzeugt im Zeichentakt keine Bitmaps oder neuen Skelette.
"""
from pathlib import Path
import argparse
import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage
from puppet import Puppet, affine, pixelize, palette_of

HERE = Path(__file__).resolve().parent
ASSETS = HERE / '../../app-sim/src/main/assets/creatures'
NAMES = ('gloop', 'puffling', 'wyrmling', 'starlet', 'hootlet')
FRAME, GROUND, COUNT = 128, 125, 138

# Frequenzen gehoeren zur Regung, nicht zur Wege- oder Beduerfnisrechnung.
PROFILE = {
    'gloop': dict(head=.6, tip=3.8, cloth=2.5, breath=.025),
    'puffling': dict(head=1.4, tip=2.2, cloth=3.2, breath=.016),
    'wyrmling': dict(head=1.0, tip=3.5, cloth=2.2, breath=.010),
    'starlet': dict(head=1.4, tip=2.6, cloth=1.5, breath=.018),
    'hootlet': dict(head=3.4, tip=1.2, cloth=1.8, breath=.008),
}
WORLD_SCALE = dict(gloop=1.16, puffling=1.28, wyrmling=1.13, starlet=.98, hootlet=1.18)


def drawings(name, *, source=None, count=32, columns=4):
    """Quellkomponenten nach Zeilen ordnen und Alpha-Halos verwerfen.

    Feste Zellen genuegen bei generierter Kunst nicht: Ihre Lage kann abweichen.
    Eine explizite Zuordnung in manifest.json kann eine fehlerhafte Quellenpose
    ausschliessen, ohne sie als angeblich korrekte Bewegung auszuliefern.
    """
    rgba = np.asarray(Image.open(source or HERE / 'source' / f'{name}-motion-atlas.png').convert('RGBA'))
    mask = rgba[..., 3] > 200
    labels, _ = ndimage.label(mask)
    sizes = np.bincount(labels.ravel())
    ids = [i for i in np.flatnonzero(sizes > 1200) if i]
    if count % columns or len(ids) != count:
        raise ValueError(f'{name}: {len(ids)} statt {count} vollstaendige Figuren')
    figures = []
    for i in ids:
        ys, xs = np.nonzero(labels == i)
        if min(xs.min(), ys.min()) < 3 or xs.max() >= rgba.shape[1]-3 or ys.max() >= rgba.shape[0]-3:
            raise ValueError(f'{name}: angeschnittene Quellfigur')
        # Der harte Spiel-Alpha haelt den koerperfremden Farbschleier aus der Welt.
        isolated = rgba.copy()
        component = ndimage.binary_dilation(labels == i, iterations=2) & mask
        isolated[~component] = 0
        isolated[component, 3] = 255
        cropped = isolated[ys.min():ys.max()+1, xs.min():xs.max()+1] / 255.
        figures.append((int(ys.mean()), int(xs.mean()), cropped))
    figures.sort(key=lambda f: f[0])
    ordered = []
    for row in range(count // columns):
        group = sorted(figures[row*columns:row*columns+columns], key=lambda f: f[1])
        if max(f[0] for f in group) - min(f[0] for f in group) > rgba.shape[0]/(count // columns * 1.5):
            raise ValueError(f'{name}: unklare Zeilenordnung {row}')
        ordered.extend(f[2] for f in group)
    return ordered


def register(arts, *, palette_size=40):
    # Ein Massstab fuer den GESAMTEN Atlas. Laufende und sitzende Figuren duerfen
    # nicht anhand ihrer momentanen Silhouette auf eine Standhoehe gedehnt werden.
    scale = min(110/max(a.shape[1] for a in arts), 113/max(a.shape[0] for a in arts))
    palette = palette_of(np.concatenate([a.reshape(-1, 1, 4) for a in arts]), k=palette_size)
    frames = []
    for art in arts:
        small = pixelize(art, scale, palette)
        ys, xs = np.nonzero(small[..., 3] > .5)
        dx = round(FRAME/2-(xs.min()+xs.max())/2)
        dy = GROUND-int(ys.max())
        tx, ty = xs+dx, ys+dy
        if tx.min() < 3 or tx.max() >= FRAME-3 or ty.min() < 3:
            raise ValueError('Pose verletzt das gemeinsame Raster')
        out = np.zeros((FRAME, FRAME, 4))
        out[ty, tx] = small[ys, xs]
        frames.append(out)
    return frames, palette, scale


def rig(frame, name):
    y, x = np.indices((FRAME, FRAME))
    ys, xs = np.nonzero(frame[..., 3] > .5)
    top, left, right = ys.min(), xs.min(), xs.max()
    head_y = round(top+(GROUND-top)*.58)
    cx = (left+right)/2
    p = Puppet(Image.fromarray(np.uint8(frame*255)), soft=1.1)
    p.add('body', np.zeros((FRAME, FRAME), bool), (cx, GROUND-12), z=1)
    p.add('head', y < head_y, (cx, head_y), 'body', z=2)
    p.add('tip', (y < top+(head_y-top)*.32), (cx, top+(head_y-top)*.35), 'head', z=3)
    p.add('cloth', (y >= head_y) & (y < GROUND-9) & (x < cx-12),
          (cx-10, head_y), 'body', z=2)
    # Bodenkontakt bekommt kein Elterngelenk. Sonst hebt Atmen die ganze Figur ab.
    p.add('support', y >= GROUND-6, (cx, GROUND), z=4)
    p.finish('body')
    return p, (cx, head_y)


def render_rig(p, local, palette):
    out = pixelize(p.pose(local, ss=2), 1, palette)
    out[GROUND+1:] = 0
    return out


def idle(frame, name, palette, n=8, sleeping=False):
    p, (cx, hy) = rig(frame, name)
    profile = PROFILE[name]
    result = []
    for i in range(n):
        phase = 2*np.pi*i/n
        breath = profile['breath']*(.45 if sleeping else 1)
        result.append(render_rig(p, {
            'body': affine(pivot=(cx, GROUND-6), sy=1+breath*np.sin(phase)),
            'head': affine(profile['head']*np.sin(phase+.3)*(.2 if sleeping else 1), (cx,hy)),
            'tip': affine(profile['tip']*np.sin(phase-.8), (cx,hy-20)),
            'cloth': affine(profile['cloth']*np.sin(phase-1.3), (cx-10,hy)),
        }, palette))
    return result


# Lidfenster aus den registrierten neuen Zeichnungen; Brillenringe bleiben fest.
EYE_WINDOWS = {
    'gloop': ((48, 91, 57, 104), (71, 91, 79, 104)),
    'puffling': ((50, 81, 59, 93), (72, 81, 80, 93)),
    'wyrmling': ((53, 55, 63, 63), (70, 55, 78, 63)),
    'starlet': ((47, 75, 58, 88), (73, 75, 83, 89)),
    'hootlet': ((52, 78, 61, 89), (69, 78, 77, 88)),
}


def rolled(curl, palette):
    # Die Quelle ist eine EIGENE eingerollte Haltung; kein drehender Stand-Sticker.
    ys,xs=np.nonzero(curl[...,3]>.5)
    cx,cy=(xs.min()+xs.max())/2,(ys.min()+ys.max())/2
    radius=np.hypot(xs-cx,ys-cy).max()
    factor=min(1,52/radius)
    out=[]
    p=Puppet(Image.fromarray(np.uint8(curl*255)),soft=0)
    p.add('body',np.ones((FRAME,FRAME),bool),(cx,cy));p.finish('body')
    for i in range(8):
        posed=p.pose({'body':affine(i*45,(cx,cy),t=(64-cx,64-cy),sx=factor,sy=factor)},ss=2)
        fr=pixelize(posed,1,palette)
        yy,xx=np.nonzero(fr[...,3]>.5)
        dy=GROUND-yy.max()
        placed=np.zeros_like(fr);placed[yy+dy,xx]=fr[yy,xx]
        out.append(placed)
    return out


def frames(name):
    if name == 'wyrmling':
        from wyrmling_motion import frames as wyrmling_frames
        return wyrmling_frames()
    from refined_motion import frames as refined_frames
    return refined_frames(name)


def build(names=NAMES):
    for name in names:
        strip=np.concatenate(frames(name),axis=1)
        Image.fromarray(np.uint8(np.clip(strip,0,1)*255),'RGBA').save(ASSETS/f'{name}.png',optimize=True)
        print(f'{name}: {COUNT} Bilder')


def preview(path):
    clips=[list(range(8)),list(range(9,17)),list(range(68,76)),list(range(76,84)),
           list(range(114,122)),[95,84,85,86,87,95,95,95],
           [95,92,93,93,93,94,95,95],list(range(130,138))]
    labels=['Atmung / Blick','Gang rechts','Gang vorn','Gang hinten','Schnell',
            'Sprung / Landung','Sitzen / Aufstehen','Rolle']
    sheets={name:Image.open(ASSETS/f'{name}.png').convert('RGBA') for name in NAMES}
    rendered=[]
    cell=192
    for tick in range(48):
        bg=Image.new('RGBA',(cell*8,cell*5+42),(73,95,84,255))
        d=ImageDraw.Draw(bg)
        for col,title in enumerate(labels):d.text((col*cell+5,5),title,fill=(245,231,203))
        for row,name in enumerate(NAMES):
            y=row*cell+42
            size=round(FRAME*WORLD_SCALE[name])
            for col,clip in enumerate(clips):
                phase=(tick//4 if col==0 else tick//2)%len(clip)
                fr=sheets[name].crop((clip[phase]*FRAME,0,(clip[phase]+1)*FRAME,FRAME))
                lift=[0,0,13,20,0,0,0,0][phase] if col==5 else 0
                fr=fr.resize((size,size),Image.Resampling.NEAREST)
                bg.alpha_composite(fr,(col*cell+(cell-size)//2,y+182-round(126*WORLD_SCALE[name])-lift))
                d.line((col*cell+6,y+182,(col+1)*cell-6,y+182),fill=(121,140,119))
            d.text((4,y+3),name.capitalize(),fill=(245,231,203))
        rendered.append(bg.convert('RGB'))
    rendered[0].save(path,save_all=True,append_images=rendered[1:],duration=55,loop=0,optimize=True)


if __name__=='__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('--preview',type=Path)
    parser.add_argument('--species', choices=NAMES)
    args=parser.parse_args()
    preview(args.preview) if args.preview else build((args.species,) if args.species else NAMES)
