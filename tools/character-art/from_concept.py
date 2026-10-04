"""Die sechs Wesen direkt aus den Charakter-Konzeptblaettern (character-conceptbook, 04.10.).

Statt die Figuren nachzuzeichnen, werden die gemalten Posen der Blaetter ausgeschnitten, vom
Papier freigestellt und in Pixel-Art uebersetzt - so bleiben Fell, Umhang, Taschen, Gesichter und
die lebendigen Haltungen der Studien erhalten. Jedes Blatt hat dieselbe Ordnung: oben vorn /
seitlich / hinten, unten vier Handlungsposen (Gehen, Tun, Aufmerken oder Freude, Schlafen).

Ergebnis ist derselbe Bogen wie bisher (17 Bilder zu 64 x 64, Fuesse auf Zeile 61), damit der
Spielcode unveraendert bleibt:
  0 Ruhe, 1 Ruhe eingeatmet, 2 Blinzeln, 3..6 Laufen, 7..8 Freude, 9..10 Schlafen,
  11 vorn, 12 hinten, 13..14 vorn gehen, 15..16 hinten gehen

Die Blaetter werden ueber `git show` aus dem Branch art/concept-studies-2026-10-04 gelesen.

  python3 from_concept.py                      # Boegen nach app-sim/src/main/assets/creatures
  python3 from_concept.py --preview out.png    # alle Boegen vergroessert nebeneinander
  python3 from_concept.py --poses out.png      # freigestellte Posen zur Kontrolle
"""
import io
import os
import subprocess
import sys
import numpy as np
from PIL import Image
from scipy import ndimage

REF = 'origin/art/concept-studies-2026-10-04'
SHEET = 'docs/concept-art/character-conceptbook/images/{}-character-fantasy-study-v1.png'
FRAME = 64
FOOT = 62          # erste Zeile unter den Fuessen

# Posen je Blatt: Rechteck (x0, y0, x1, y1) im 1536 x 1024-Blatt und ob die Pose nach links
# blickt (dann wird sie gespiegelt - im Bogen schauen alle Figuren nach rechts).
# Rollen: side = Ruhe, walk = Schritt, joy = Freude/Aufmerken, sleep, front, back.
POSES = {
    'fennec': dict(
        front=((740, 75, 990, 432), False),
        side=((990, 90, 1300, 432), True),
        back=((1292, 70, 1516, 442), False),
        walk=((30, 618, 462, 935), False),
        joy=((806, 626, 1124, 952), False),
        sleep=((1146, 700, 1516, 962), False),
    ),
    'gloop': dict(
        front=((664, 118, 925, 478), False),
        side=((968, 118, 1215, 478), True),
        back=((1222, 118, 1502, 478), False),
        walk=((52, 696, 436, 978), False),
        stand=((1196, 700, 1514, 982), True),
        joy=((452, 696, 764, 978), False),
        sleep=((780, 736, 1190, 982), False),
    ),
    'starlet': dict(
        front=((684, 156, 950, 416), False),
        side=((996, 144, 1180, 420), True),
        back=((1222, 156, 1482, 420), False),
        walk=((48, 664, 392, 968), False),
        joy=((800, 664, 1084, 962), False),
        sleep=((1114, 730, 1494, 956), False),
    ),
    'puffling': dict(
        front=((630, 118, 916, 426), False),
        side=((924, 118, 1196, 426), False),
        back=((1204, 118, 1492, 426), False),
        walk=((36, 672, 372, 976), False),
        joy=((792, 652, 1110, 978), False),
        sleep=((1114, 716, 1518, 970), False),
    ),
    'wyrmling': dict(
        front=((598, 84, 862, 432), False),
        side=((866, 84, 1234, 436), True),
        back=((1236, 84, 1508, 436), False),
        walk=((40, 688, 404, 960), False),
        joy=((420, 664, 852, 966), False),
        sleep=((1154, 706, 1512, 956), False),
    ),
    'hootlet': dict(
        front=((598, 106, 870, 430), False),
        side=((912, 106, 1150, 432), False),
        back=((1216, 106, 1488, 434), False),
        walk=((42, 710, 406, 980), True),
        joy=((852, 646, 1136, 974), False),
        sleep=((1138, 664, 1508, 968), False),
    ),
}
ORDER = ['fennec', 'gloop', 'starlet', 'puffling', 'wyrmling', 'hootlet']
# Zielhoehe stehender Posen im Bild (Pixel) - Wesen sind unterschiedlich gross.
HEIGHT = {'fennec': 56, 'gloop': 44, 'starlet': 44, 'puffling': 46, 'wyrmling': 56, 'hootlet': 50}
COLORS = 48
# Welche Pose im Stehen gezeigt wird. Bei den runden Wesen verschwindet das Gesicht in der reinen
# Seitenansicht - dort steht die halb zugewandte Pose aus der unteren Reihe.
IDLE = {'gloop': 'stand', 'starlet': 'walk', 'puffling': 'walk', 'hootlet': 'walk'}


def sheet(name):
    data = subprocess.run(['git', 'show', f'{REF}:{SHEET.format(name)}'], capture_output=True, check=True).stdout
    return np.asarray(Image.open(io.BytesIO(data)).convert('RGB'), dtype=float) / 255


def paper_of(img):
    edge = np.concatenate([img[4:14].reshape(-1, 3), img[-14:-4].reshape(-1, 3)])
    return np.median(edge, 0)


def cutout(img, box, paper, close=7):
    """Pose freistellen: alles, was sich vom Papier abhebt, zusammenhaengend, Loecher gefuellt.
    Bodenschatten (blasse, kaum gesaettigte Toene) und lose Grashalme fallen weg."""
    x0, y0, x1, y1 = box
    sub = img[y0:y1, x0:x1]
    d = np.abs(sub - paper).sum(-1)
    mx, mn = sub.max(-1), sub.min(-1)
    sat = (mx - mn) / np.maximum(mx, 1e-6)
    # Was vom Rand aus ueber papierfarbene Flaechen erreichbar ist, ist Hintergrund; alles, was
    # eine (geschlossene) Kontur umschliesst, gehoert zur Figur - auch helles Fell und Wolken.
    stroke = ndimage.binary_closing(d > 0.14, np.ones((close, close)))
    lab, n = ndimage.label(~stroke)
    edge = set(np.unique(np.concatenate([lab[0], lab[-1], lab[:, 0], lab[:, -1]]))) - {0}
    m = ~np.isin(lab, list(edge))
    # Bodenschatten und Bleistiftstriche am Fuss: blass und kaum gesaettigt
    h = m.shape[0]
    yy = np.arange(h)[:, None]
    m &= ~((yy > h * 0.8) & (d < 0.36) & (sat < 0.28))
    m = ndimage.binary_opening(m, np.ones((5, 5)))
    # Unten: Grashalme und Bodenstriche sind duenn, Beine und Fuesse nicht
    low = yy[:, 0] > h * 0.78
    m[low] = ndimage.binary_opening(m, np.ones((9, 9)))[low]
    lab, n = ndimage.label(m)
    if n:
        sizes = ndimage.sum(m, lab, range(1, n + 1))
        m = np.isin(lab, 1 + np.flatnonzero(sizes >= sizes.max() * 0.05))
    ys, xs = np.nonzero(m)
    return sub[ys.min():ys.max() + 1, xs.min():xs.max() + 1], m[ys.min():ys.max() + 1, xs.min():xs.max() + 1]


def shrink(rgb, mask, scale):
    """Flaechengemittelt verkleinern (Farbe nur aus der Figur, nicht vom Papier)."""
    h, w = mask.shape
    W, H = max(1, round(w * scale)), max(1, round(h * scale))
    a = mask.astype(float)
    pm = Image.fromarray(np.uint8(np.dstack([rgb * a[..., None], a]) * 255), 'RGBA')
    # Vorgemischte Farben mitteln, dann zurueckteilen
    small = np.asarray(pm.resize((W, H), Image.BOX), dtype=float) / 255
    al = small[..., 3]
    col = small[..., :3] / np.maximum(al[..., None], 1e-6)
    return np.clip(col, 0, 1), al > 0.45


def vivid(rgb):
    """Aquarell ist blass: etwas mehr Saettigung und Tiefe, damit die Figur auf den Orten steht."""
    g = rgb.mean(-1, keepdims=True)
    out = g + (rgb - g) * 1.28
    out = (out - 0.5) * 1.12 + 0.5
    return np.clip(out, 0, 1)


def palette(colors, k=COLORS, seed=3):
    rng = np.random.default_rng(seed)
    c = colors[rng.choice(len(colors), min(k, len(colors)), replace=False)]
    for _ in range(20):
        d = ((colors[:, None] - c[None]) ** 2).sum(-1)
        idx = d.argmin(1)
        for j in range(len(c)):
            if (idx == j).any():
                c[j] = colors[idx == j].mean(0)
    return c


def quantize(rgb, pal):
    d = ((rgb[..., None, :] - pal) ** 2).sum(-1)
    return pal[d.argmin(-1)]


def outline(rgb, mask):
    """Einpixel-Kontur aussen: dunkle Version der angrenzenden Farbe (selektive Kontur)."""
    ring = ndimage.binary_dilation(mask, np.array([[0, 1, 0], [1, 1, 1], [0, 1, 0]])) & ~mask
    out = rgb.copy()
    acc = np.zeros_like(rgb)
    cnt = np.zeros(mask.shape)
    for dy, dx in ((0, 1), (0, -1), (1, 0), (-1, 0)):
        sh = np.roll(np.roll(rgb * mask[..., None], dy, 0), dx, 1)
        shm = np.roll(np.roll(mask, dy, 0), dx, 1)
        acc += sh
        cnt += shm
    near = acc / np.maximum(cnt[..., None], 1)
    out[ring] = np.clip(near[ring] * np.array([0.32, 0.26, 0.28]), 0, 1)
    return out, mask | ring


def place(rgb, mask, lift=0):
    """In ein 64er-Bild setzen: waagerecht mittig, Fuesse auf Zeile FOOT-1."""
    h, w = mask.shape
    img = np.zeros((FRAME, FRAME, 4))
    x0 = (FRAME - w) // 2
    y0 = FOOT - h - lift
    for j in range(h):
        for i in range(w):
            y, x = y0 + j, x0 + i
            if mask[j, i] and 0 <= y < FRAME and 0 <= x < FRAME:
                img[y, x, :3] = rgb[j, i]
                img[y, x, 3] = 1
    return img


def poses(name):
    img = sheet(name)
    paper = paper_of(img)
    out = {}
    for role, (box, flip) in POSES[name].items():
        rgb, m = cutout(img, box, paper)
        if flip:
            rgb, m = rgb[:, ::-1], m[:, ::-1]
        out[role] = (rgb, m)
    return out


def sprites(name):
    """Alle Posen auf Spielgroesse: (RGB, Maske) je Rolle, gemeinsame Palette, Kontur."""
    raw = poses(name)
    # Ein Massstab fuer alle stehenden Posen (aus der Seitenansicht), damit nichts springt;
    # zu breite Posen werden passend kleiner.
    side_h = raw[IDLE.get(name, 'side')][1].shape[0]
    base = (HEIGHT[name] - 2) / side_h
    small = {}
    for role, (rgb, m) in raw.items():
        h, w = m.shape
        if role in ('front', 'back', 'stand'):
            s = (HEIGHT[name] - 2) / h
        else:
            s = base
        s = min(s, (FRAME - 4) / w, (FRAME - 6) / h)
        if role == 'sleep':
            s = min(s, base * 1.0)
        col, mm = shrink(rgb, m, s)
        small[role] = (vivid(col), mm)
    allc = np.concatenate([c[mm] for c, mm in small.values()])
    pal = palette(allc)
    out = {}
    for role, (c, mm) in small.items():
        q = quantize(c, pal)
        pad = np.pad(q, ((1, 1), (1, 1), (0, 0)))
        pm = np.pad(mm, 1)
        out[role] = outline(pad, pm)
    return out


def blink(frame):
    """Augen zu: dunkle kleine Flecken im Kopf werden zu einem Strich (unterste Reihe bleibt)."""
    f = frame.copy()
    a = f[..., 3] > 0
    v = f[..., :3].max(-1)
    ys = np.nonzero(a.any(1))[0]
    top, bot = ys.min(), ys.max()
    head = np.zeros_like(a)
    head[top:top + int((bot - top) * 0.6)] = True
    inner = ndimage.binary_erosion(a, iterations=2)
    dark = inner & head & (v < 0.30)
    lab, n = ndimage.label(dark)
    for i in range(1, n + 1):
        blob = lab == i
        if not 2 <= blob.sum() <= 14:
            continue
        yy, xx = np.nonzero(blob)
        ring = ndimage.binary_dilation(blob, iterations=2) & ~blob & a & (v >= 0.30)
        if not ring.any():
            continue
        skin = np.median(f[ring][:, :3], 0)
        low = yy.max()
        for y, x in zip(yy, xx):
            if y < low:
                f[y, x, :3] = skin
    return f


def lifted(frame, n):
    f = np.roll(frame, -n, axis=0)
    if n:
        f[-n:] = 0
    return f


def breath(frame, upto=None):
    """Eingeatmet: oberer Teil eine Zeile tiefer, Fuesse bleiben."""
    a = frame[..., 3] > 0
    ys = np.nonzero(a.any(1))[0]
    upto = upto or int(ys.min() + (ys.max() - ys.min()) * 0.7)
    b = frame.copy()
    b[1:upto] = frame[0:upto - 1]
    b[0] = 0
    return b


def leaned(frame, dx):
    """Schritt: Oberkoerper um dx Pixel nach vorn geneigt (Scherung), Fuesse bleiben."""
    a = frame[..., 3] > 0
    ys = np.nonzero(a.any(1))[0]
    top, bot = ys.min(), ys.max()
    out = np.zeros_like(frame)
    for y in range(FRAME):
        t = 0 if bot == top else (bot - y) / (bot - top)
        s = int(round(dx * max(0.0, t)))
        out[y] = np.roll(frame[y], s, axis=0)
    return out


def frames(name):
    sp = sprites(name)
    P = {r: place(*sp[r]) for r in sp}
    idle, walk, joy, sleep, front, back = P[IDLE.get(name, 'side')], P['walk'], P['joy'], P['sleep'], P['front'], P['back']
    fbreath = breath(front)
    return [
        idle, breath(idle), blink(idle),
        walk, lifted(leaned(walk, 1), 1), breath(walk), lifted(walk, 1),
        lifted(joy, 2), lifted(joy, 0),
        sleep, breath(sleep),
        front, back,
        lifted(leaned(front, 1), 1), lifted(leaned(fbreath, -1), 1),
        lifted(leaned(back, 1), 1), lifted(leaned(back, -1), 1),
    ]


def to_image(fr):
    s = np.concatenate(fr, axis=1)
    return Image.fromarray(np.uint8(np.clip(s, 0, 1) * 255), 'RGBA')


def build(out_dir):
    os.makedirs(out_dir, exist_ok=True)
    for name in ORDER:
        to_image(frames(name)).save(os.path.join(out_dir, f'{name}.png'), optimize=True)


def preview(path, scale=3):
    rows = [to_image(frames(n)) for n in ORDER]
    out = Image.new('RGBA', (FRAME * 17 + 16, (FRAME + 4) * len(rows) + 8), (232, 222, 200, 255))
    for r, im in enumerate(rows):
        out.alpha_composite(im, (8, 4 + r * (FRAME + 4)))
    out.resize((out.width * scale, out.height * scale), Image.NEAREST).save(path)


def pose_sheet(path):
    tiles = []
    for n in ORDER:
        for role, (rgb, m) in poses(n).items():
            im = np.dstack([rgb, m.astype(float)])
            t = Image.fromarray(np.uint8(im * 255), 'RGBA')
            t.thumbnail((200, 200))
            tiles.append(t)
    out = Image.new('RGBA', (6 * 204, len(ORDER) * 204), (60, 70, 80, 255))
    for i, t in enumerate(tiles):
        out.alpha_composite(t, ((i % 6) * 204, (i // 6) * 204))
    out.save(path)


if __name__ == '__main__':
    here = os.path.dirname(os.path.abspath(__file__))
    if len(sys.argv) > 2 and sys.argv[1] == '--preview':
        preview(sys.argv[2])
    elif len(sys.argv) > 2 and sys.argv[1] == '--poses':
        pose_sheet(sys.argv[2])
    else:
        build(sys.argv[1] if len(sys.argv) > 1 else os.path.join(here, '../../app-sim/src/main/assets/creatures'))
