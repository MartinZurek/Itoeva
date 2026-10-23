"""Atlasvertrag der unverformten Seitenfolgen, ohne Kunstqualitaet zu behaupten."""
import base64
import zlib
import numpy as np
import hashlib
import json
from pathlib import Path
import unittest
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]

class PaintedAssetsTest(unittest.TestCase):
    def test_size_registration_uses_head_instead_of_bent_legs(self):
        data = json.loads((ROOT / 'data/mosslyn_painted_cycles.json').read_text())
        reference = None
        for mode, spec in data.items():
            registration = spec['size_registration']
            self.assertEqual(len(registration['frames']), 28)
            target = registration['reference_span'] / registration['reference_body_height']
            if reference is None:
                reference = target
            self.assertAlmostEqual(target, reference)
            image = np.asarray(Image.open(ROOT / spec['asset'].removeprefix('res://')))
            x0, y0, x1, y1 = registration['template_region']
            template = Image.fromarray(image[y0:y1, x0:x1, :3])
            # Unabhaengiger Bildabgleich: registrierte Regionen muessen wirklich
            # dasselbe Kopfdetail enthalten, nicht nur passende Zahlen melden.
            for i, frame in enumerate(registration['frames']):
                self.assertGreater(frame['correlation'], .70)
                x, y, w, h = frame['region']
                self.assertGreater(w, 110)
                self.assertGreater(h, 60)
                tile = Image.fromarray(image[i//7*237:(i//7+1)*237, i%7*237:(i%7+1)*237, :3])
                patch = tile.crop((x, y, x+w, y+h)).resize(template.size, Image.Resampling.BILINEAR)
                a = np.asarray(template, dtype=float).mean(axis=2).ravel()
                b = np.asarray(patch, dtype=float).mean(axis=2).ravel()
                self.assertGreater(np.corrcoef(a, b)[0,1], .65, (mode, i))
                self.assertAlmostEqual(frame['span'], w*registration['mode_span']/148, delta=1)
            # Die ganze Silhouette darf sich durch Kniebeugung weiter veraendern.
            normalized = [b[3] * target / f['span'] for b, f in zip(spec['bounds'], registration['frames'])]
            self.assertGreater(max(normalized)-min(normalized), .02)

    def test_frames_and_alpha_match_runtime_metadata(self):
        data = json.loads((ROOT / 'data/mosslyn_painted_cycles.json').read_text())
        for mode in ('walk', 'run'):
            with self.subTest(mode=mode):
                spec = data[mode]
                path = ROOT / spec['asset'].removeprefix('res://')
                self.assertEqual(hashlib.sha256(path.read_bytes()).hexdigest(), spec['sha256'])
                with Image.open(path) as image:
                    self.assertEqual(image.mode, 'RGBA')
                    self.assertEqual(image.size, (spec['cell'] * 7, spec['cell'] * 4))
                    self.assertEqual(len(spec['bounds']), 28)
                    ownership = np.frombuffer(zlib.decompress(base64.b64decode(spec["mask_deflate"])), dtype=np.uint8).reshape(spec["height"], spec["width"])
                    self.assertEqual(set(np.unique(ownership)), set(range(29)))
                    signatures = set()
                    for i, bounds in enumerate(spec['bounds']):
                        cell = spec['cell']
                        tile = image.crop((i % 7 * cell, i // 7 * cell, (i % 7 + 1) * cell, (i // 7 + 1) * cell))
                        yy, xx = np.where(ownership == i + 1)
                        x, y, width, height = bounds
                        self.assertEqual((int(xx.min()) - i % 7 * cell, int(yy.min()) - i // 7 * cell, int(xx.max()-xx.min()+1), int(yy.max()-yy.min()+1)), (x,y,width,height))
                        self.assertTrue(np.all(np.asarray(image)[:,:,3][ownership == i + 1] >= 32))
                        self.assertGreater(width * height, cell * cell * .4)
                        self.assertEqual(tile.getchannel('A').getextrema()[0], 0)
                        signatures.add(hashlib.sha256(tile.tobytes()).hexdigest())
                    self.assertEqual(len(signatures), 28)

if __name__ == '__main__':
    unittest.main()
