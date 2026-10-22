"""Dauerhaft gleiche Zeichnungen und zusammenhaengende Gelenkgewichte."""
import base64
import hashlib
import importlib.util
import json
import unittest
import zlib
from pathlib import Path
import numpy as np

ROOT=Path(__file__).resolve().parents[1]

class BodyRigs(unittest.TestCase):
 def test_originals_and_joint_weights(self):
  data=json.loads((ROOT/'data/body_rigs.json').read_text())
  self.assertEqual(set(data),{'mosslyn','nocti','starlet','wyrmling','nocti-flight'})
  for name,rig in data.items():
   path=ROOT/'assets/characters'/f'{name if name=="nocti-flight" else name+"-walk"}.png'
   self.assertEqual(hashlib.sha256(path.read_bytes()).hexdigest(),rig['source_sha256'])
   raw=zlib.decompress(base64.b64decode(rig['mask']))
   self.assertEqual(hashlib.sha256(raw).hexdigest(),rig['mask_sha256'])
   mask=np.frombuffer(raw,np.uint8).reshape(3,256,256,4)
   for row,profile in enumerate(rig['rows']):
    self.assertLess(profile['neck'][1],profile['root'][1])
    self.assertEqual(len(profile['shoulders']),2)
    self.assertEqual(len(profile['elbows']),2)
    for shoulder,elbow in zip(profile['shoulders'],profile['elbows']):
     self.assertGreater(np.linalg.norm(np.subtract(shoulder,elbow)),10)
    # Ein harter Gelenkausschnitt koennte sichtbare Risse im Hautnetz erzeugen.
    for axis in [0,1]:
     self.assertLess(np.max(np.abs(np.diff(mask[row].astype(float),axis=axis))),90)

 def test_preparation_is_reproducible(self):
  spec=importlib.util.spec_from_file_location('body_rigs',ROOT/'tools/prepare_body_rigs.py')
  module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
  self.assertEqual(json.loads(json.dumps(module.prepare())),json.loads((ROOT/'data/body_rigs.json').read_text()))

if __name__=='__main__':unittest.main()
