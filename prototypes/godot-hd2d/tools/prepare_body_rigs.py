"""Anatomische Hautgewichte fuer zusammenhaengende Ganzkoerperbewegungen.

Die PNG-Originale bleiben unveraendert. Es entstehen technische Gewichte,
keine neuen Farbzeichnungen und keine optischen Flussfelder.
"""
import base64
import hashlib
import json
import zlib
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]

# Je Ansicht: Becken, Hals, Schweif-/Mantelansatz, zwei Schultergelenke;
# Polygone fuer nachlaufende Teile und zwei Arme/Fluegel.
RIGS = {
 'mosslyn': [
  ((136,190),(161,132),(97,191),(155,141),[(144,150),(180,151)],
   [[(31,153),(106,129),(150,140),(119,179),(59,187)],[(37,178),(96,176),(101,214),(48,219)]],
   [[(129,150),(154,153),(161,180),(145,192),(129,184)],[(175,149),(185,153),(211,172),(206,184),(184,179)]]),
  ((134,190),(132,129),(148,196),(132,140),[(108,150),(158,150)],
   [[(65,150),(99,134),(100,176),(81,203),(59,190)],[(166,134),(195,161),(208,198),(181,207),(164,169)]],
   [[(99,145),(120,146),(122,177),(111,186),(96,178)],[(151,146),(170,145),(175,177),(159,187),(147,174)]]),
  ((133,178),(132,119),(150,186),(133,132),[(101,142),(163,142)],
   [[(56,143),(102,130),(129,143),(102,181),(57,184)],[(144,132),(186,139),(207,179),(173,181),(145,161)]],
   [[(91,145),(111,146),(111,181),(97,188),(86,175)],[(155,145),(174,144),(179,179),(167,189),(154,176)]])],
 'nocti': [
  ((140,211),(171,151),(98,213),(170,163),[(109,155),(173,171)],
   [[(84,160),(123,150),(153,160),(129,187),(92,194)]],
   [[(101,143),(120,155),(105,178),(61,218),(19,201),(35,170),(73,146)],[(175,165),(188,167),(203,189),(194,212),(179,196)]]),
  ((133,211),(133,151),(135,214),(133,165),[(96,156),(169,156)],[],
   [[(94,144),(106,160),(94,191),(62,212),(31,202),(36,176),(64,153)],[(166,145),(182,148),(203,169),(215,195),(197,208),(171,190),(159,162)]]),
  ((135,191),(134,132),(138,207),(135,146),[(104,142),(165,142)],[],
   [[(100,131),(113,146),(98,174),(67,185),(31,180),(41,156),(77,137)],[(158,136),(179,132),(208,150),(232,180),(205,190),(171,171)]])],
 'starlet': [
  ((145,177),(154,141),(91,173),(151,161),[(122,166),(167,173)],
   [[(24,169),(101,148),(147,158),(133,178),(57,198),(26,188)]],[[],[]]),
  ((132,160),(132,111),(163,166),(132,146),[(90,144),(174,144)],
   [[(40,142),(81,133),(99,146),(79,166),(44,165)],[(170,141),(204,146),(208,179),(181,185),(168,166)]],[[],[]]),
  ((130,138),(132,92),(167,139),(132,119),[(87,123),(169,123)],
   [[(42,117),(90,113),(105,128),(78,149),(38,148)],[(164,119),(205,123),(219,158),(179,163),(158,141)]],[[],[]])],
 'wyrmling': [
  ((145,182),(182,126),(95,174),(160,143),[(134,139),(170,147)],
   [[(11,142),(74,139),(102,168),(86,191),(36,193),(6,174)]],
   [[(129,130),(153,137),(126,160),(94,172),(64,163),(84,140)],[(165,140),(182,147),(178,164),(165,164)]]),
  ((132,174),(132,110),(146,192),(132,123),[(100,120),(166,120)],[],
   [[(92,112),(108,124),(98,148),(82,162),(73,151),(77,129)],[(158,113),(174,112),(189,138),(188,160),(172,153),(157,131)]]),
  ((130,153),(132,87),(145,175),(132,103),[(100,102),(167,102)],
   [[(133,145),(168,150),(198,146),(209,173),(188,193),(145,183)]],
   [[(94,92),(113,103),(102,127),(74,143),(64,138),(72,112)],[(158,97),(176,91),(195,118),(202,142),(178,142),(159,119)]])],
}

FLIGHT = [
 ((147,211),(177,174),(101,214),(169,188),[(110,173),(174,190)],[],
  [[(111,166),(94,168),(70,150),(53,121),(32,91),(30,56),(64,66),(92,96),(107,130),(125,167)],[]]),
 ((132,216),(133,176),(132,222),(132,188),[(94,177),(171,177)],[],
  [[(97,169),(79,169),(60,146),(45,123),(29,115),(46,105),(71,123),(93,147),(108,169)],[(169,169),(186,166),(210,141),(220,113),(204,108),(180,130),(157,165)]]),
 ((132,188),(133,137),(132,210),(132,155),[(95,158),(168,158)],[],
  [[(96,152),(76,149),(62,128),(45,103),(28,70),(43,58),(66,74),(91,109),(109,151)],[(166,153),(187,148),(207,119),(232,74),(215,59),(192,81),(173,118),(153,153)]])]

def polygon_mask(polygons):
 layer=Image.new('L',(256,256))
 draw=ImageDraw.Draw(layer)
 for polygon in polygons:
  if polygon:draw.polygon(polygon,fill=255)
 return layer.filter(ImageFilter.GaussianBlur(3.0))

def prepare():
 result={}
 for name,rows in [*RIGS.items(),('nocti-flight',FLIGHT)]:
  masks=Image.new('RGBA',(256,768))
  records=[]
  for row,(root,neck,tail,cape,shoulders,secondary,arms) in enumerate(rows):
   yy=np.indices((256,256))[0]
   head=np.clip((neck[1]+3-yy)/17,0,1)
   head=head*head*(3-2*head)
   # Starlits Ganzkoerper ist ein Stern, kein Kopf auf einem Menschentorso.
   if name=='starlet':head*=.20
   channels=[Image.fromarray(np.uint8(head*255)),polygon_mask(secondary),polygon_mask([arms[0]]),polygon_mask([arms[1]])]
   masks.paste(Image.merge('RGBA',tuple(channels)),(0,row*256))
   elbows=[]
   for index,shoulder in enumerate(shoulders):
    if name=='nocti-flight':delta=(-32,-36) if index==0 else (32,-36)
    elif name=='nocti':delta=(-29,20) if index==0 else (26,20)
    elif name=='wyrmling':delta=(-15,15) if index==0 else (15,15)
    else:delta=(-3,17) if index==0 else (5,17)
    elbows.append([shoulder[0]+delta[0],shoulder[1]+delta[1]])
   records.append(dict(root=root,neck=neck,tail=tail,cape=cape,shoulders=shoulders,elbows=elbows))
  source=ROOT/'assets/characters'/f'{name if name=="nocti-flight" else name+"-walk"}.png'
  raw=masks.tobytes()
  result[name]=dict(rows=records,mask=base64.b64encode(zlib.compress(raw,9)).decode(),
   source_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),mask_sha256=hashlib.sha256(raw).hexdigest())
 return result

if __name__=='__main__':
 (ROOT/'data/body_rigs.json').write_text(json.dumps(prepare(),separators=(',',':'))+'\n')
 print('Vier Ganzkoerper-Hautnetze und Noctis Flug anatomisch vorbereitet.')
