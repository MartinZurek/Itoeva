"""Gezeichnete Gangphasen der fuenf Wesen in Fennecs bestehenden Bogen importieren.

Die Konzeptbilder sind Identitaetsreferenzen, keine ausgeschnittenen Spielposen.
Der Atlas enthaelt neue Zeichnungen. Nur kleine Regungen und die Zwischenposen
der Aktionen entstehen mit dem vorhandenen Puppet-Werkzeug. Alle Bilder sind
vorberechnet; Android erzeugt im Zeichentakt keine Bitmaps oder neuen Skelette.
"""
from pathlib import Path
import argparse
import json
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
WORLD_SCALE = dict(gloop=.92, puffling=.97, wyrmling=1.71, starlet=.90, hootlet=1.05)


def drawings(name):
    """Quellkomponenten nach Zeilen ordnen und Alpha-Halos verwerfen.

    Feste Zellen genuegen bei generierter Kunst nicht: Ihre Lage kann abweichen.
    Eine explizite Zuordnung in manifest.json kann eine fehlerhafte Quellenpose
    ausschliessen, ohne sie als angeblich korrekte Bewegung auszuliefern.
    """
    rgba = np.asarray(Image.open(HERE / 'source' / f'{name}-motion-atlas.png').convert('RGBA'))
    mask = rgba[..., 3] > 200
    labels, _ = ndimage.label(mask)
    sizes = np.bincount(labels.ravel())
    ids = [i for i in np.flatnonzero(sizes > 1200) if i]
    if len(ids) != 32:
        raise ValueError(f'{name}: {len(ids)} statt 32 vollstaendige Figuren')
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
    for row in range(8):
        group = sorted(figures[row*4:row*4+4], key=lambda f: f[1])
        if max(f[0] for f in group) - min(f[0] for f in group) > rgba.shape[0]/12:
            raise ValueError(f'{name}: unklare Zeilenordnung {row}')
        ordered.extend(f[2] for f in group)
    return ordered


def register(arts):
    # Ein Massstab fuer den GESAMTEN Atlas. Laufende und sitzende Figuren duerfen
    # nicht anhand ihrer momentanen Silhouette auf eine Standhoehe gedehnt werden.
    scale = min(110/max(a.shape[1] for a in arts), 113/max(a.shape[0] for a in arts))
    palette = palette_of(np.concatenate([a.reshape(-1, 1, 4) for a in arts]), k=40)
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


def posture(frame, name, palette, bend=0, compress=1, reach=0):
    p, (cx,hy) = rig(frame, name)
    return render_rig(p, {
        'body': affine(pivot=(cx,GROUND-6), sy=compress, sx=1+(1-compress)*.18),
        'head': affine(bend, (cx,hy), t=(reach,0)),
        'tip': affine(-bend*.2, (cx,hy-20)),
        'cloth': affine(-bend*.4, (cx-10,hy)),
    }, palette)


# Verifizierte Augenfenster der registrierten Frontbilder. Die Brillenringe
# bleiben ausserhalb; Material an Fluegeln kann nie als Auge klassifiziert werden.
EYE_WINDOWS = {
    'gloop': ((53, 83, 63, 98), (77, 83, 87, 98)),
    'puffling': ((47, 66, 60, 83), (74, 66, 86, 82)),
    'wyrmling': ((55, 78, 63, 87), (67, 78, 75, 87)),
    'starlet': ((45, 72, 58, 90), (71, 72, 84, 90)),
    'hootlet': ((52, 72, 64, 84), (70, 72, 81, 84)),
}


def front_blink(front, palette, name):
    """Neu gezeichnete Lider importieren, nur innerhalb verifizierter Augenfenster.

    ImageGen hat den Korrekturbogen leicht neu angeordnet. Registrierung am
    gesamten Frontkoerper gleicht das aus; ausserhalb der Augen bleibt jeder
    Originalpixel unveraendert. Keine geometrische Augen-Erkennung am Umhang.
    """
    rgba=np.asarray(Image.open(HERE/'source/ensemble-front-blink-atlas.png').convert('RGBA'))
    labels,_=ndimage.label(rgba[...,3]>200)
    sizes=np.bincount(labels.ravel())
    figures=[]
    for label in np.flatnonzero(sizes>1200):
        if not label:continue
        ys,xs=np.nonzero(labels==label)
        isolated=rgba.copy();isolated[labels!=label]=0
        figures.append((ys.mean(),xs.mean(),isolated[ys.min():ys.max()+1,xs.min():xs.max()+1]/255.))
    if len(figures)!=10:raise ValueError('Lidschlagquelle hat nicht zehn Figuren')
    figures.sort(key=lambda f:f[0])
    art=sorted(figures[5:],key=lambda f:f[1])[NAMES.index(name)][2]
    ys,xs=np.nonzero(front[...,3]>.5)
    scale=(ys.max()-ys.min()-1)/art.shape[0]
    small=pixelize(art,scale,palette)
    yy,xx=np.nonzero(small[...,3]>.5)
    dx=round((xs.min()+xs.max())/2-(xx.min()+xx.max())/2)
    dy=GROUND-yy.max()
    mapped=np.zeros_like(front);mapped[yy+dy,xx+dx]=small[yy,xx]
    out=front.copy()
    for x0,y0,x1,y1 in EYE_WINDOWS[name]:
        patch=mapped[y0:y1,x0:x1]
        if not (patch[...,3]>.5).all():raise ValueError(f'{name}: unvollstaendiges Augenfenster')
        out[y0:y1,x0:x1]=patch
    return out


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
    manifest=json.loads((HERE/'source/ensemble-motion-manifest.json').read_text())
    art,pal,scale=register(drawings(name))
    indexes=manifest[name]
    stand,front,back,blink=[art[i] for i in indexes['neutral']]
    walk=[art[i] for i in indexes['walk']]
    fw=[art[i] for i in indexes['front_walk']]
    bw=[art[i] for i in indexes['back_walk']]
    run=[art[i] for i in indexes['run']]
    crouch,jump,sit,sleep,stretch,reach,curl,joy=[art[i] for i in indexes['actions']]
    neutral=idle(stand,name,pal)
    fi,bi=idle(front,name,pal),idle(back,name,pal)
    landing=posture(crouch,name,pal,compress=.97)
    # Die alte grobe Freude darf ein wirklich freudiges Gesicht zeigen. Ein
    # gesteuerter Sprung verwendet weiterhin die eigenen Aktionsrollen 84-87.
    joy_clip=[crouch,joy,joy,jump,landing,stand]
    result=neutral+[blink]+walk+joy_clip+idle(sleep,name,pal,n=4,sleeping=True)
    result += [front]+fw+[back]+bw+[front,back]+fi+bi+[front_blink(front,pal,name)]
    # Freude ist beim Sprung eine zur Hoehe passende Haltung; Auf/Ab kommen vom Spiel.
    fj=[posture(front,name,pal,compress=.90),front,posture(front,name,pal,compress=1.02),front,front,front]
    bj=[posture(back,name,pal,compress=.90),back,posture(back,name,pal,compress=1.02),back,back,back]
    result += fj+bj
    result += [fw[i//2] for i in range(8)]+[bw[i//2] for i in range(8)]
    # Dieselben sechzehn Aktionsrollen wie Fennec, mit anatomisch eigener Quelle.
    result += [crouch,jump,jump,landing,
               posture(stand,name,pal,bend=9,compress=.94),
               posture(stand,name,pal,bend=17,compress=.86),
               posture(crouch,name,pal,bend=4),crouch,
               posture(sit,name,pal,compress=1.08),sit,
               posture(stand,name,pal,compress=.95),stand,stretch,reach,
               posture(reach,name,pal,bend=-6),sleep]
    for base in (front,back):
        result += [posture(base,name,pal,compress=.85),
                   posture(base,name,pal,compress=1.02),base,
                   posture(base,name,pal,compress=.72),base,
                   posture(base,name,pal,bend=8,compress=.88),
                   posture(base,name,pal,compress=1.04)]
    result += [run[i//2] for i in range(8)]
    # Gerichtetes schnelles Gehen behaelt die eigene Front-/Rueckanatomie.
    # Bis zu eigenen Sprintzeichnungen sind es die gerichteten Schritte, ohne Flugrecht.
    result += fw+bw+rolled(curl,pal)
    if len(result)!=COUNT:
        raise ValueError(f'{name}: {len(result)} statt {COUNT} Rollen')
    return result


def build():
    for name in NAMES:
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
    args=parser.parse_args()
    preview(args.preview) if args.preview else build()
