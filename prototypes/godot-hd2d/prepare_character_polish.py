"""Leitet Alpha-Ausschnitte ab; Original-PNGs werden nicht verändert."""
import json
from pathlib import Path
from statistics import median
import numpy as np
from scipy import ndimage
from PIL import Image

ROOT=Path(__file__).resolve().parent
DATA=ROOT/'data'
def read(name):return json.loads((DATA/name).read_text())
def write(name,value):(DATA/name).write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n')
def regions(path,columns,rows):
 image=Image.open(path).convert('RGBA')
 assert image.width%columns==image.height%rows==0
 width,height=image.width//columns,image.height//rows
 assert width==height
 local,absolute=[],[]
 for frame in range(columns*rows):
  x,y=frame%columns*width,frame//columns*height
  alpha=image.getchannel('A').crop((x,y,x+width,y+height))
  labels,_=ndimage.label(np.asarray(alpha)>=80,structure=np.ones((3,3)))
  sizes=np.bincount(labels.ravel());sizes[0]=0
  assert sizes.max()>0,(path,frame)
  yy,xx=np.where(labels==sizes.argmax())
  left,top,right,bottom=int(xx.min()),int(yy.min()),int(xx.max()+1),int(yy.max()+1)
  local.append([left,top,right-left,bottom-top]);absolute.append([x+left,y+top,right-left,bottom-top])
 return local,absolute
def main():
 original=read('world_catalog.json')['characters'];characters=read('new_characters.json')
 bounds=read('compact_bounds.json');combat=read('new_combat_atlases.json')
 for species in ['starlet','wyrmling','fennec','gloop','puffling','hootlet']:
  asset=f'assets/characters/{species}-v2.png'
  local,absolute=regions(ROOT/asset,8,6)
  characters[species]=dict(original[species],asset='res://'+asset,combat_asset='res://'+asset,compact=True)
  bounds[species]=local
  combat[species]={'asset':'res://'+asset,'regions':absolute[32:48],'reference_height':local[0][3]}
 runs={}
 for species,suffix in [('mosslyn','run'),('nocti','flight')]:
  asset=f'assets/characters/{species}-{suffix}.png';local,_=regions(ROOT/asset,8,3)
  characters[species]['run_asset']='res://'+asset
  runs[species]={'bounds':local,'reference_height':median(b[3] for b in local[:8])}
 write('new_characters.json',characters);write('compact_bounds.json',bounds)
 write('new_combat_atlases.json',combat);write('character_runs.json',runs)
if __name__=='__main__':main()
