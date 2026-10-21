"""Prueft die transportierten Bewegungsfelder ohne Entwickler-Bildbibliotheken."""
import base64
import hashlib
import json
import unittest
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


class GaitRegistrationTest(unittest.TestCase):
    def test_sources_fields_and_cyclic_targets(self):
        data = json.loads((ROOT/'data/gait_registration.json').read_text())
        self.assertEqual({'mosslyn', 'nocti', 'starlet', 'wyrmling'}, set(data))
        count = 0
        for species, modes in data.items():
            self.assertEqual({'walk', 'run'} if species in ('mosslyn', 'nocti') else {'walk'}, set(modes))
            for mode, record in modes.items():
                suffix = 'flight' if species == 'nocti' and mode == 'run' else mode
                source = ROOT/'assets/characters'/f'{species}-{suffix}.png'
                self.assertEqual(record['source_sha256'], hashlib.sha256(source.read_bytes()).hexdigest())
                field = json.loads((ROOT/record['flow_asset'].removeprefix('res://')).read_text())
                raw = zlib.decompress(base64.b64decode(field['deflate']))
                self.assertEqual(512*192*4, len(raw))
                self.assertEqual((512, 192), (field['width'], field['height']))
                self.assertEqual(field['sha256'], hashlib.sha256(raw).hexdigest())
                self.assertEqual(3, len(record['rows']))
                for row in record['rows']:
                    self.assertEqual(8, len(row['offsets']))
                    self.assertEqual(8, len(row['sequence']))
                    self.assertTrue(all(0 <= phase < 8 for phase in row['sequence']))
                    self.assertTrue(all(abs(n) <= 32 for pair in row['offsets'] for n in pair))
                    self.assertEqual(2 if species == 'nocti' and mode == 'run' else 1, row['mode'])
                count += 1
        self.assertEqual(6, count)


if __name__ == '__main__':
    unittest.main()
