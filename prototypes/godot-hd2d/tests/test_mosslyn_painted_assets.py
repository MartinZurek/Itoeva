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
