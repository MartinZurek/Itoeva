"""Sichert Grenzen der verwendeten Masken und der umkehrbaren Beinabbildung."""
import base64
import hashlib
import json
import math
import unittest
import zlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]

class StepRigs(unittest.TestCase):
 def test_sources_and_masks(self):
  data=json.loads((ROOT/'data/step_rigs.json').read_text())
  self.assertEqual(set(data),{'mosslyn','nocti','starlet','wyrmling','fennec','gloop','puffling','hootlet'})
  for name,rig in data.items():
   self.assertEqual(hashlib.sha256((ROOT/'assets/characters'/f'{name}-walk.png').read_bytes()).hexdigest(),rig['source_sha256'])
   raw=zlib.decompress(base64.b64decode(rig['mask']))
   self.assertEqual(len(raw),256*768*4)
   self.assertEqual(hashlib.sha256(raw).hexdigest(),rig['mask_sha256'])
   for row in range(3):
    # Der gesamte obere Kopfbereich ist von Beintransformationen ausgeschlossen.
    for y in range(80):
     start=(row*256+y)*256*4
     self.assertFalse(any(raw[start:start+256*4:4]))
     self.assertFalse(any(raw[start+1:start+256*4:4]))
  flight=data['nocti']['flight']
  self.assertEqual(hashlib.sha256((ROOT/'assets/characters/nocti-flight.png').read_bytes()).hexdigest(),flight['source_sha256'])
  self.assertEqual(len(zlib.decompress(base64.b64decode(flight['mask']))),256*768*4)

 def test_deformation_never_folds_or_disconnects(self):
  for name,rig in json.loads((ROOT/'data/step_rigs.json').read_text()).items():
   amplitude=rig['half_stride']
   for row,depth in enumerate([0,.573576,-.573576]):
    profile=rig['rows'][row]
    for leg in range(len(profile["feet"])):
     hip=profile['hips'][leg];foot=profile['feet'][leg]
     span=max(24,foot[1]-8-hip[1])
     for sprint in [0,.5,1]:
      duty=.56-.08*sprint
      for tick in range(240):
       phase=tick/240
       if phase<duty:travel=amplitude*(1-2*phase/duty);lift=0
       else:
        u=(phase-duty)/(1-duty);tan=-(1-duty)/duty
        h=-2*u**3+3*u*u+tan*(2*u**3-3*u*u+u)
        travel=amplitude*(2*h-1);lift=amplitude*.44*math.sin(math.pi*u)**2
       offset=travel*depth-lift+profile['ground']-foot[1]
       self.assertGreater(1+min(0,offset)*1.5/span,0,
          f'{name}/{row}/{leg}/{sprint}/{tick}: nicht eindeutige Beinabbildung')
       # Die Maske beginnt oberhalb des bewegten Ansatzes: dort muss der
       # Ortsversatz exakt null sein, sonst entsteht eine schwarze Gelenkfuge.
       self.assertGreaterEqual(hip[1],80)

if __name__=='__main__':unittest.main()
