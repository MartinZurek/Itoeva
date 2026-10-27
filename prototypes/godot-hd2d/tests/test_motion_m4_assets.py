"""Originalregistrierung, feste Maßstäbe und bytegleiche Anschlusskacheln."""
from pathlib import Path
import unittest,json,hashlib,base64,zlib
import numpy as np
from PIL import Image
P=Path(__file__).resolve().parents[1]
class MotionM4Assets(unittest.TestCase):
 def data(self,sp):return json.loads((P/'data'/('mosslyn_m4_cycles.json' if sp=='mosslyn' else sp+'_authored_cycles.json')).read_text())
 def tile(self,spec,index,pixel=False):
  path=P/spec['asset'].removeprefix('res://')
  if pixel:path=path.parent/'pixel'/path.name
  with Image.open(path) as im:
   c=spec['cell'];x=index%spec['columns']*c;y=index//spec['columns']*c
   return np.array(im.crop((x,y,x+c,y+c)))
 def test_original_masks_and_common_body_scale(self):
  for sp in ['mosslyn','nocti','starlet','wyrmling']:
   data=self.data(sp);self.assertEqual(len({d['reference_height'] for d in data.values()}),1)
   for spec in data.values():
    path=P/spec['asset'].removeprefix('res://');self.assertEqual(hashlib.sha256(path.read_bytes()).hexdigest(),spec['sha256'])
    mask=np.frombuffer(zlib.decompress(base64.b64decode(spec['mask_deflate'])),np.uint8).reshape(spec['height'],spec['width'])
    self.assertEqual(set(np.unique(mask)),set(range(spec['frames']+1)))
    for i,b in enumerate(spec['bounds']):
     yy,xx=np.where(mask==i+1);c=spec['cell'];self.assertEqual([int(xx.min())-i%spec['columns']*c,int(yy.min())-i//spec['columns']*c,int(xx.max()-xx.min()+1),int(yy.max()-yy.min()+1)],b)
    with Image.open(path.parent/'pixel'/path.name) as im:im.load();self.assertEqual(im.size,(spec['width'],spec['height']))
 def test_mooslyn_connections_and_rest_are_identical(self):
  d=self.data('mosslyn')
  for pixel in [False,True]:
   for a,i,b,j in [('lower',0,'walk',0),('lower',3,'run',1),('rise',0,'run',1),('rise',3,'walk',0),('rest',0,'walk',0)]:
    np.testing.assert_array_equal(self.tile(d[a],i,pixel),self.tile(d[b],j,pixel))
 def test_nocti_flight_and_ground_endpoints(self):
  d=self.data('nocti')
  for pixel in [False,True]:
   np.testing.assert_array_equal(self.tile(d['takeoff'],0,pixel),self.tile(d['rest'],0,pixel))
   np.testing.assert_array_equal(self.tile(d['land'],3,pixel),self.tile(d['rest'],0,pixel))
   for i in range(8):np.testing.assert_array_equal(self.tile(d['walk'],i,pixel),self.tile(d['run'],i,pixel))
 def test_star_body_and_airtime(self):
  d=self.data('starlet')
  for mode in ['walk','run']:
   heights=[b[3] for b in d[mode]['bounds']]
   self.assertLessEqual(max(heights)-min(heights),4,'Sternkörper darf zwischen Phasen nicht wachsen')
   self.assertGreater(sum(d[mode]['phase_weights'][2:7]),.65)
if __name__=='__main__':unittest.main()
