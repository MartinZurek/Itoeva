"""Prüft überstehende Schweife, getrennte Pfoten und fremde Nachbarfiguren."""
import base64,sys,tempfile,unittest,zlib
from pathlib import Path
from PIL import Image,ImageDraw
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
import prepare_character_polish as prepare
class PoseRegions(unittest.TestCase):
 def test_tail_and_detached_foot_without_extra_character(self):
  with tempfile.TemporaryDirectory(dir=prepare.ROOT) as directory:
   path=Path(directory)/'synthetic.png';image=Image.new('RGBA',(200,100));draw=ImageDraw.Draw(image)
   for rect in [(30,20,65,60),(60,40,115,47),(40,68,46,75),(140,20,175,60),(106,70,130,96)]:draw.rectangle(rect,fill=(255,255,255,255))
   image.save(path);local,_=prepare.regions(path,2,1,whole_poses=True)
   self.assertEqual(local[0],[30,20,86,56]);self.assertEqual(local[1],[40,20,36,41])
   data=prepare.MASKS['res://'+path.relative_to(prepare.ROOT).as_posix()];mask=zlib.decompress(base64.b64decode(data['deflate']))
   self.assertEqual(mask[44*200+113],data['ids'][0]);self.assertEqual(mask[70*200+43],data['ids'][0])
   self.assertEqual(mask[80*200+115],0);self.assertNotEqual(data['ids'][0],data['ids'][1])
if __name__=='__main__':unittest.main()
