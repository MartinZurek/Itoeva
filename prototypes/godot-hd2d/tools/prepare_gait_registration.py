"""Registriert vorhandene Gangzeichnungen; schreibt keine neuen Bilddateien.

Ein festes Oberkoerpermotiv verhindert wechselnde Kopf-/Gesichtszeichnungen.
Die vorhandenen Stuetzbeinphasen bleiben unterhalb der Huefte beweglich.
"""
import base64
import hashlib
import json
import zlib
from pathlib import Path

import numpy as np
import cv2
from PIL import Image
from scipy.signal import correlate2d

ROOT = Path(__file__).resolve().parents[1]
NAMES = ('mosslyn', 'nocti', 'starlet', 'wyrmling')
# Augen-/Kopfmerkmale bleiben von wechselnden Armen, Fluegeln und Pfoten frei.
PATCHES = {
    'mosslyn': [(156, 80, 43, 42), (95, 73, 70, 50), (87, 49, 82, 56)],
    'nocti': [(162, 99, 54, 50), (82, 87, 86, 59), (76, 79, 95, 58)],
    'starlet': [(126, 104, 58, 51), (78, 85, 91, 52), (88, 46, 76, 61)],
    'wyrmling': [(176, 101, 54, 44), (93, 87, 67, 48), (85, 52, 82, 56)],
}
FLIGHT_PATCHES = [(171, 133, 51, 47), (99, 117, 71, 53), (90, 77, 77, 70)]


def prepare():
    masks = json.loads((ROOT/'data/character_pose_masks.json').read_text())
    walks = json.loads((ROOT/'data/character_walks.json').read_text())
    runs = json.loads((ROOT/'data/character_runs.json').read_text())
    result = {}
    for name in NAMES:
        result[name] = {}
        modes = [('walk', name+'-walk.png', walks[name], PATCHES[name])]
        if name in runs:
            filename = name+('-flight.png' if name == 'nocti' else '-run.png')
            modes.append(('run', filename, runs[name], FLIGHT_PATCHES if name == 'nocti' else PATCHES[name]))
        for mode, filename, definition, patches in modes:
            path = ROOT/'assets/characters'/filename
            rgba = np.asarray(Image.open(path).convert('RGBA'))/255.0
            h, w = rgba.shape[:2]
            cell = w//8
            record = masks['res://assets/characters/'+filename]
            ids = np.frombuffer(zlib.decompress(base64.b64decode(record['deflate'])), np.uint8).reshape(h, w)
            entries = []
            flows = np.zeros((192, 512, 4), np.uint8)
            for row in range(3):
                x, y, pw, ph = patches[row]
                reference = rgba[row*cell:(row+1)*cell, :cell]
                template = reference[y:y+ph, x:x+pw, :3].mean(axis=2)
                template = template-template.mean()
                offsets = []
                for phase in range(8):
                    # Nur kleine Translationen; keine Verformung/Skalierung des Kopfes.
                    art = rgba[row*cell:(row+1)*cell, phase*cell:(phase+1)*cell, :3].mean(axis=2)
                    margin = 28
                    left, top = max(0, x-margin), max(0, y-margin)
                    search = art[top:min(cell, y+ph+margin), left:min(cell, x+pw+margin)]
                    score = correlate2d(search, template, mode='valid')
                    sums = correlate2d(search, np.ones(template.shape), mode='valid')
                    sq = correlate2d(search*search, np.ones(template.shape), mode='valid')
                    score /= np.sqrt(np.maximum(1e-6, sq-sums*sums/template.size))
                    dy, dx = np.unravel_index(score.argmax(), score.shape)
                    dx, dy = int(dx+left-x), int(dy+top-y)
                    if mode == 'walk' or name == 'mosslyn':
                        b, ref = definition['bounds'][row*8+phase], definition['bounds'][row*8]
                        dy = b[1]+b[3]-ref[1]-ref[3]
                    offsets.append([dx, dy])
                if mode == 'run' and name == 'nocti':
                    fixed = [[113, 61, 139, 198], [77, 0, 129, 256], [70, 0, 137, 256]][row]
                    entry = {'offsets': offsets, 'mode': 2, 'window': fixed}
                else:
                    b = definition['bounds'][row*8]
                    split = b[1]+round(b[3]*({'starlet': .70, 'wyrmling': .68}.get(name, .74)))
                    left, right = (75, 226) if name == 'wyrmling' and row == 0 else (-40, cell+40)
                    entry = {'offsets': offsets, 'mode': 1, 'window': [left, split, right-left, cell+40-split]}
                sequence = [0, 1, 3, 4, 5, 6, 7, 1] if mode == 'run' and name == 'mosslyn' and row == 0 else list(range(8))
                entry['sequence'] = sequence
                aligned = []
                for phase, (dx, dy) in enumerate(offsets):
                    # Reine technische Bewegungsfelder: Farbzeichnungen bleiben Originale.
                    origin = np.float32([[1, 0, -phase*cell-dx], [0, 1, -row*cell-dy]])
                    own = rgba.copy()
                    own[ids != record['ids'][row*8+phase]] = 0
                    frame = cv2.warpAffine(np.uint8(own*255), origin, (cell, cell))
                    gray = cv2.cvtColor(frame, cv2.COLOR_RGBA2GRAY)
                    aligned.append(gray)
                for step, phase in enumerate(sequence):
                    next_phase = sequence[(step+1)%8]
                    args = (.5, 3, 25, 6, 7, 1.5, 0)
                    forward = cv2.calcOpticalFlowFarneback(aligned[phase], aligned[next_phase], None, *args)
                    backward = cv2.calcOpticalFlowFarneback(aligned[next_phase], aligned[phase], None, *args)
                    flow = np.concatenate([forward, backward], axis=2)
                    flow = cv2.resize(flow, (64, 64), interpolation=cv2.INTER_AREA)
                    encoded = np.uint8(np.clip(np.rint(flow*4+128), 0, 255))
                    flows[row*64:(row+1)*64, step*64:(step+1)*64] = encoded
                entries.append(entry)
            result[name][mode] = {'rows': entries, 'flow': {
                'width': 512, 'height': 192,
                'sha256': hashlib.sha256(flows.tobytes()).hexdigest(),
                'deflate': base64.b64encode(zlib.compress(flows.tobytes(), 9)).decode('ascii')},
                'source_sha256': hashlib.sha256(path.read_bytes()).hexdigest()}
    return result


if __name__ == '__main__':
    output = ROOT/'data/gait_registration.json'
    data = prepare()
    for name, modes in data.items():
        for mode, record in modes.items():
            flow = record.pop('flow')
            filename = f'gait_flow_{name}_{mode}.json'
            (ROOT/'data'/filename).write_text(json.dumps(flow, separators=(',', ':'))+'\n')
            record['flow_asset'] = 'res://data/'+filename
    output.write_text(json.dumps(data, ensure_ascii=False, indent=2)+'\n')
    print('Vier Spielfiguren, sechs gerichtete Gang-/Flugfolgen registriert.')
