#!/usr/bin/env python3
"""Technische R/G-Masken aus handvermessenen Wasser-/Wolkenkonturen vorrechnen."""
import base64
import hashlib
import json
from pathlib import Path
import zlib
import numpy as np
ROOT = Path(__file__).resolve().parents[1]

def coverage(points, width, height, source_size, feather=0):
    yy, xx = np.mgrid[:height, :width]
    x, y = (xx+.5)/width*source_size[0], (yy+.5)/height*source_size[1]
    inside = np.zeros((height,width),dtype=bool)
    distance = np.full((height,width),np.inf)
    for a,b in zip(points,points[1:]+points[:1]):
        ax,ay=a; bx,by=b
        if ay != by: inside ^= ((ay>y)!=(by>y)) & (x<(bx-ax)*(y-ay)/(by-ay)+ax)
        if feather:
            dx,dy=bx-ax,by-ay
            t=np.clip(((x-ax)*dx+(y-ay)*dy)/max(dx*dx+dy*dy,1e-12),0,1)
            distance=np.minimum(distance,np.hypot(x-ax-t*dx,y-ay-t*dy))
    weight=np.minimum(distance/feather,1) if feather else np.ones_like(x)
    return inside*(weight*weight*(3-2*weight))

def bake(entry,width=384,height=128):
    wet=np.zeros((height,width));clouds=np.zeros_like(wet)
    for points in entry.get('water',[]):wet=np.maximum(wet,coverage(points,width,height,entry['size']))
    for points in entry.get('exclude',[]):wet*=1-coverage(points,width,height,entry['size'])
    for points in entry.get('clouds',[]):clouds=np.maximum(clouds,coverage(points,width,height,entry['size'],entry.get('cloud_feather',24)))
    pixels=np.stack([wet,clouds],axis=2)
    if not np.any(pixels):width=height=1;pixels=np.zeros((1,1,2))
    raw=np.rint(pixels*255).astype(np.uint8).tobytes()
    return dict(width=width,height=height,bytes=len(raw),sha256=hashlib.sha256(raw).hexdigest(),deflate=base64.b64encode(zlib.compress(raw,9)).decode(),water=bool(np.any(wet)),clouds=bool(np.any(clouds)))

def source_entries():
    ambient=json.loads((ROOT/'data/ambience.json').read_text());forest=json.loads((ROOT/'data/forest_trail.json').read_text())
    entries=dict(ambient['assets']);entries['forest:SHADOW_BROOK']={'size':[1,1],'water':forest['water'],'exclude':forest.get('water_exclude',[])}
    return entries

def main():
    entries=source_entries()
    records={key:bake(value,416,235) if key.startswith('forest:') else bake(value) for key,value in entries.items()}
    source=json.dumps(entries,sort_keys=True,separators=(',',':'),ensure_ascii=False).encode()
    data=dict(schema=1,format='RG8: Wasser/Wolken',source_sha256=hashlib.sha256(source).hexdigest(),masks=records)
    (ROOT/'data/environment_masks.json').write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
    print(f'{len(records)} Masken; {sum(len(v["deflate"]) for v in records.values())} Base64-Zeichen')
if __name__=='__main__':main()
