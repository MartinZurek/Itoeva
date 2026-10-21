import hashlib
import importlib.util
import json
from pathlib import Path
import unittest
import zlib
import base64

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('bake',ROOT/'tools/bake_environment_masks.py')
bake=importlib.util.module_from_spec(spec);spec.loader.exec_module(bake)

class EnvironmentMasks(unittest.TestCase):
    def test_baked_masks_match_contours_and_hashes(self):
        stored=json.loads((ROOT/'data/environment_masks.json').read_text())
        entries=bake.source_entries()
        source=json.dumps(entries,sort_keys=True,separators=(',',':'),ensure_ascii=False).encode()
        self.assertEqual(stored['source_sha256'],hashlib.sha256(source).hexdigest())
        self.assertEqual(set(entries),set(stored['masks']))
        for key,entry in entries.items():
            with self.subTest(key=key):
                record=stored['masks'][key]
                self.assertEqual(record,bake.bake(entry,416,235) if key.startswith('forest:') else bake.bake(entry))
                pixels=zlib.decompress(base64.b64decode(record['deflate']))
                self.assertEqual(len(pixels),record['width']*record['height']*2)
                self.assertEqual(hashlib.sha256(pixels).hexdigest(),record['sha256'])

if __name__=='__main__':unittest.main()
