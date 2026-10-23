"""Offline-Kopfregistrierung; Originalzeichnungen werden nicht veraendert.

Ausfuehren mit numpy, Pillow und opencv-python-headless. Nur Gesicht/Ohr dienen
als Groessenreferenz: Beinbeugung, Geweih und wehender Umhang sind keine Laengen.
Die Messrechtecke und Korrelationswerte bleiben zur Nachpruefung in den Daten.
"""
import json
from pathlib import Path

import cv2
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
BOX = (70, 45, 218, 130)


def match_head(image, template, scales):
    gray = cv2.cvtColor(image[:, :, :3], cv2.COLOR_RGB2GRAY)
    best = (-1.0, None, None)
    for scale in scales:
        patch = cv2.resize(template, None, fx=float(scale), fy=float(scale))
        if patch.shape[0] >= gray.shape[0] or patch.shape[1] >= gray.shape[1]:
            continue
        _, score, _, point = cv2.minMaxLoc(cv2.matchTemplate(gray, patch, cv2.TM_CCOEFF_NORMED))
        if score > best[0]:
            best = (score, float(scale), point)
    score, scale, point = best
    if score < .70:
        raise ValueError(f'Unsichere Kopfregistrierung: {score}')
    return dict(span=round(template.shape[1] * scale, 5),
                region=[*point, round(template.shape[1] * scale), round(template.shape[0] * scale)],
                correlation=round(score, 5))


def prepare():
    path = ROOT / 'data/mosslyn_painted_cycles.json'
    data = json.loads(path.read_text())
    arrays = {mode: np.asarray(Image.open(ROOT / spec['asset'].removeprefix('res://')))
              for mode, spec in data.items()}
    x0, y0, x1, y1 = BOX
    common = cv2.cvtColor(arrays['walk'][y0:y1, x0:x1, :3], cv2.COLOR_RGB2GRAY)
    original = np.asarray(Image.open(ROOT / 'assets/characters/mosslyn.png'))[:181, :181]
    rest = match_head(original[:145], common, np.arange(.55, .81, .0025))
    body_height = json.loads((ROOT / 'data/compact_bounds.json').read_text())['mosslyn'][0][3]
    for mode, spec in data.items():
        atlas = arrays[mode]
        template = cv2.cvtColor(atlas[y0:y1, x0:x1, :3], cv2.COLOR_RGB2GRAY)
        mode_reference = match_head(atlas[:160, :237], common, np.arange(.85, 1.201, .0025))
        frames = []
        for i in range(28):
            tile = atlas[i // 7 * 237:(i // 7 + 1) * 237, i % 7 * 237:(i % 7 + 1) * 237]
            frame = match_head(tile[:170], template, np.arange(.85, 1.201, .0025))
            frame['span'] = round(frame['span'] * mode_reference['span'] / common.shape[1], 5)
            frames.append(frame)
        spec['size_registration'] = dict(method='face-ear-template-uniform-v1',
            reference_body_height=body_height, reference_span=rest['span'],
            reference_center_offset=rest['region'][0] + rest['region'][2] / 2 - 181 / 2,
            reference_region=rest['region'], template_region=list(BOX),
            mode_span=mode_reference['span'], frames=frames)
    path.write_text(json.dumps(data, indent=2) + '\n')


if __name__ == '__main__':
    prepare()
