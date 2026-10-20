"""Leitet Alpha-Ausschnitte ab; Original-PNGs werden nicht verändert."""
import json
import base64
import zlib
from pathlib import Path
from statistics import median
import numpy as np
from scipy import ndimage
from PIL import Image

ROOT=Path(__file__).resolve().parent
DATA=ROOT/'data'
MASKS={}
def read(name):return json.loads((DATA/name).read_text())
def write(name,value):(DATA/name).write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n')
def regions(path,columns,rows,whole_poses=False):
 image=Image.open(path).convert('RGBA')
 assert image.width%columns==image.height%rows==0
 width,height=image.width//columns,image.height//rows
 assert width==height
 whole_labels,_=ndimage.label(np.asarray(image.getchannel('A'))>=31,structure=np.ones((3,3)))
 whole_objects=ndimage.find_objects(whole_labels) if whole_poses else []
 local,absolute,primary=[],[],[]
 for frame in range(columns*rows):
  x,y=frame%columns*width,frame//columns*height
  alpha=image.getchannel('A').crop((x,y,x+width,y+height))
  labels,_=ndimage.label(np.asarray(alpha)>=80,structure=np.ones((3,3)))
  sizes=np.bincount(labels.ravel());sizes[0]=0
  assert sizes.max()>0,(path,frame)
  yy,xx=np.where(labels==sizes.argmax())
  left,top,right,bottom=int(xx.min()),int(yy.min()),int(xx.max()+1),int(yy.max()+1)
  if whole_poses:
   # Das Raster identifiziert die Pose, begrenzt aber nicht ihren Schweif.
   cell_labels=whole_labels[y:y+height,x:x+width]
   candidates=np.bincount(cell_labels[labels==sizes.argmax()]);candidates[0]=0
   identity=int(candidates.argmax());primary.append(identity)
   region=whole_objects[identity-1]
   left,top=region[1].start-x,region[0].start-y
   right,bottom=region[1].stop-x,region[0].stop-y
  local.append([left,top,right-left,bottom-top]);absolute.append([x+left,y+top,right-left,bottom-top])
 if whole_poses:
  # Getrennt gezeichnete Pfoten gehören dazu; zusätzliche ganze Figuren nicht.
  primary_mask=np.isin(whole_labels,primary)
  distance,nearest=ndimage.distance_transform_edt(~primary_mask,return_indices=True)
  nearest_identity=whole_labels[tuple(nearest)]
  complete={identity:list(absolute[frame]) for frame,identity in enumerate(primary)}
  primary_set=set(primary)
  component_sizes=np.bincount(whole_labels.ravel())
  pose_ids={identity:index+1 for index,identity in enumerate(dict.fromkeys(primary))}
  owners=np.zeros(int(whole_labels.max())+1,dtype=np.uint8)
  for identity in primary_set:owners[identity]=pose_ids[identity]
  for identity,region in enumerate(whole_objects,1):
   if identity in primary_set or region is None:continue
   component=whole_labels[region]==identity
   if np.count_nonzero(component)<6:continue
   distances=np.where(component,distance[region],np.inf)
   point=np.unravel_index(np.argmin(distances),distances.shape)
   if distances[point]>width*.18:continue
   owner=int(nearest_identity[region][point])
   if component_sizes[identity]>min(1200,component_sizes[owner]*.15):continue
   owners[identity]=pose_ids[owner]
   box=complete[owner]
   left,top=min(box[0],region[1].start),min(box[1],region[0].start)
   right,bottom=max(box[0]+box[2],region[1].stop),max(box[1]+box[3],region[0].stop)
   complete[owner]=[left,top,right-left,bottom-top]
  for frame,identity in enumerate(primary):
   x,y=frame%columns*width,frame//columns*height
   absolute[frame]=complete[identity]
   left,top,w,h=absolute[frame];local[frame]=[left-x,top-y,w,h]
  # Technische R8-ID-Daten trennen ineinandergreifende Rechtecke, ohne die
  # Originalgrafiken zu verändern oder Gliedmaßen künstlich neu zu zeichnen.
  mask=owners[whole_labels]
  MASKS['res://'+path.relative_to(ROOT).as_posix()]={
   'width':image.width,'height':image.height,'ids':[pose_ids[i] for i in primary],
   'deflate':base64.b64encode(zlib.compress(mask.tobytes(),9)).decode('ascii')}
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
  # Die ruhige Weltpose hat einen vollständig enthaltenen Schwanz-/Flügelrand.
  combat[species]['regions'][12]=absolute[0]
  combat[species]['regions'][13]=absolute[0]
 for species in characters:
  asset=characters[species]['asset'].removeprefix('res://')
  local,_=regions(ROOT/asset,8,6,whole_poses=True)
  bounds[species][:32]=local[:32]
 runs={}
 for species,suffix in [('mosslyn','run'),('nocti','flight')]:
  asset=f'assets/characters/{species}-{suffix}.png';local,_=regions(ROOT/asset,8,3,whole_poses=True)
  characters[species]['run_asset']='res://'+asset
  runs[species]={'bounds':local,'reference_height':median(b[3] for b in local[:8])}
 walks={}
 for species in characters:
  asset=f'assets/characters/{species}-walk.png'
  local,_=regions(ROOT/asset,8,3,whole_poses=True)
  characters[species]['walk_asset']='res://'+asset
  walks[species]={'bounds':local,'reference_height':median(b[3] for b in local[:8])}
 write('character_walks.json',walks);write('character_pose_masks.json',MASKS)
 write('new_characters.json',characters);write('compact_bounds.json',bounds)
 write('new_combat_atlases.json',combat);write('character_runs.json',runs)
if __name__=='__main__':main()
