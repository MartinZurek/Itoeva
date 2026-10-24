"""A1-Volumen mit unveraenderter Originalfarbe und reproduzierbarem Seitenrig.
Benötigt numpy, scipy, Pillow. Keine Bildneuerzeugung; nur Originalprojektion.
"""
from pathlib import Path
import hashlib, io, json, struct
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt
ROOT=Path(__file__).resolve().parents[2]
SRC=Path(__file__).parent/'source'
OUT=ROOT/'prototypes/godot-hd2d/assets/characters/mosslyn-3d'
OUT.mkdir(parents=True,exist_ok=True)
a=np.array(Image.open(SRC/'original.png').convert('RGBA'))
r,g,b,alpha=a.transpose(2,0,1).astype(float)
mask=(alpha>65)&~((r>180)&(g<65)&(b<65)&(r>g*2.3))
_,near=distance_transform_edt(~mask,return_indices=True)
rgb=a[:,:,:3].copy();rgb[~mask]=rgb[near[0][~mask],near[1][~mask]]
im=Image.fromarray(rgb);stream=io.BytesIO();im.save(stream,format='PNG');paint=stream.getvalue()
c=np.load(SRC/'mesh_source.npz');p=c['p'];f=c['f']
# Gleichmaessige Verdichtung statt zufaelliger, nicht reproduzierbarer Auswahl.
_,ids=np.unique(np.round(p/.028).astype(int),axis=0,return_inverse=True)
P=np.zeros((ids.max()+1,3));np.add.at(P,ids,p);P/=np.bincount(ids)[:,None]
F=ids[f];F=F[(F[:,0]!=F[:,1])&(F[:,0]!=F[:,2])&(F[:,1]!=F[:,2])]
F=F[np.linalg.norm(np.cross(P[F[:,1]]-P[F[:,0]],P[F[:,2]]-P[F[:,0]]),axis=1)>1e-9]
# A1: Z oben / X Tiefe. Godot: Y oben / Z Tiefe; richtige Seitenansicht entlang -Z.
xy=np.stack([91+P[:,1]*100,181.5-P[:,2]*100],axis=1)
V=np.stack([P[:,1]+.005,P[:,2]-.005,P[:,0]],axis=1).astype('<f4')
UV=(xy/181).astype('<f4')
N=np.zeros_like(V);normal=np.cross(V[F[:,1]]-V[F[:,0]],V[F[:,2]]-V[F[:,0]])
for j in range(3):np.add.at(N,F[:,j],normal)
N/=np.maximum(np.linalg.norm(N,axis=1)[:,None],1e-10)
# Benannte echte Gelenke. Die im Original verdeckten Gliedmassen werden
# nicht durch erfundene neue Farben oder ein zweites Fawn-Design ersetzt.
names=['body','head','cape','bag','arm','rear_hip','rear_knee','rear_foot','front_hip','front_knee','front_foot','tail']
anchors=np.array([[110,140],[117,91],[105,101],[99,129],[116,111],[96,144],[82,162],[76,179],[125,144],[121,163],[131,179],[62,146]],float)
parents=[-1,0,0,0,0,0,5,6,0,8,9,0]
world=np.column_stack([(anchors[:,0]-90.5)/100,(181-anchors[:,1])/100,np.zeros(len(names))]).astype('<f4')
world[5:8,2]=-.10;world[8:11,2]=.10
# Regionskoordinaten sind im Originalbild, nicht in wechselnden Laufbildern.
labels=Image.new('L',(181,181));d=ImageDraw.Draw(labels)
d.polygon([(23,143),(28,123),(46,106),(82,88),(109,90),(122,104),(110,114),(78,128)],fill=2)
d.polygon([(78,124),(102,118),(113,133),(108,151),(90,158),(72,147)],fill=3)
d.polygon([(109,105),(121,106),(134,122),(139,140),(127,146),(109,129),(104,118)],fill=4)
d.polygon([(30,138),(48,132),(74,133),(78,151),(72,168),(47,171),(30,157)],fill=11)
labels=np.array(labels)
weights=np.zeros((len(V),len(names)),float);weights[:,0]=1
for i,(x,y) in enumerate(xy):
    x0,y0=np.clip(np.round([x,y]).astype(int),0,180)
    if y<94:
        w=np.clip((101-y)/10,0,1);weights[i,0]=1-w;weights[i,1]=w
    elif labels[y0,x0] in [2,3,4,11]:
        idx=int(labels[y0,x0]);w=.90 if idx in [2,3,11] else .85
        weights[i,0]=1-w;weights[i,idx]=w
    elif y>145:
        start=5 if x<110 else 8
        # Das Knie ist ein Gelenk; lokal zwei benachbarte Knochen mischen.
        t=np.clip((y-154)/16,0,1)
        if y<171:
            weights[i]=0;weights[i,start]=1-t;weights[i,start+1]=t
        else:
            t=np.clip((y-171)/5,0,1);weights[i]=0;weights[i,start+1]=1-t;weights[i,start+2]=t
        blend=np.clip((y-145)/9,0,1)
        weights[i]*=blend;weights[i,0]+=1-blend
    elif labels[y0,x0]>0:
        idx=int(labels[y0,x0]);w=.90 if idx in [2,3] else .85
        weights[i,0]=1-w;weights[i,idx]=w
J=np.argsort(weights,axis=1)[:,-4:].astype('<u2')
W=np.take_along_axis(weights,J,axis=1).astype('<f4');W/=W.sum(axis=1)[:,None]
buf=bytearray();views=[];accessors=[]
def acc(arr,kind,ct):
    arr=np.ascontiguousarray(arr);buf.extend(b'\0'*(-len(buf)%4));off=len(buf);buf.extend(arr.tobytes())
    views.append(dict(buffer=0,byteOffset=off,byteLength=arr.nbytes))
    item=dict(bufferView=len(views)-1,componentType=ct,count=len(arr),type=kind)
    if kind=='VEC3':item.update(min=arr.min(0).tolist(),max=arr.max(0).tolist())
    accessors.append(item);return len(accessors)-1
attrs=dict(POSITION=acc(V,'VEC3',5126),NORMAL=acc(N.astype('<f4'),'VEC3',5126),TEXCOORD_0=acc(UV,'VEC2',5126),JOINTS_0=acc(J,'VEC4',5123),WEIGHTS_0=acc(W,'VEC4',5126))
indices=acc(F.reshape(-1).astype('<u4'),'SCALAR',5125)
ibm=np.tile(np.eye(4,dtype='<f4'),(len(names),1,1));ibm[:,:3,3]=-world
inverse=acc(ibm.transpose(0,2,1).reshape(-1,16),'MAT4',5126)
nodes=[dict(name='MooslynOriginalRig',children=[1,2]),dict(name='OriginalVolume',mesh=0,skin=0)]
for j,name in enumerate(names):
    pos=world[j]-(world[parents[j]] if parents[j]>=0 else 0)
    node=dict(name=name,translation=pos.tolist())
    children=[k+2 for k,v in enumerate(parents) if v==j]
    if children:node['children']=children
    nodes.append(node)
buf.extend(b'\0'*(-len(buf)%4));off=len(buf);buf.extend(paint);views.append(dict(buffer=0,byteOffset=off,byteLength=len(paint)))
gltf=dict(asset=dict(version='2.0',generator='Itoeva Mooslyn A1 side rig'),scene=0,scenes=[dict(nodes=[0])],nodes=nodes,
    meshes=[dict(primitives=[dict(attributes=attrs,indices=indices,material=0)])],skins=[dict(name='OriginalSideRig',joints=list(range(2,2+len(names))),skeleton=2,inverseBindMatrices=inverse)],
    materials=[dict(name='OriginalPaint',doubleSided=True,pbrMetallicRoughness=dict(baseColorTexture=dict(index=0),metallicFactor=0,roughnessFactor=1),extensions=dict(KHR_materials_unlit={}))],extensionsUsed=['KHR_materials_unlit'],
    images=[dict(bufferView=len(views)-1,mimeType='image/png')],textures=[dict(source=0,sampler=0)],samplers=[dict(magFilter=9729,minFilter=9729,wrapS=33071,wrapT=33071)],
    accessors=accessors,bufferViews=views,buffers=[dict(byteLength=len(buf))])
j=json.dumps(gltf,separators=(',',':')).encode();j+=b' '*(-len(j)%4);buf.extend(b'\0'*(-len(buf)%4))
(OUT/'original-rig.glb').write_bytes(struct.pack('<III',0x46546c67,2,28+len(j)+len(buf))+struct.pack('<II',len(j),0x4e4f534a)+j+struct.pack('<II',len(buf),0x004e4942)+buf)
report=dict(vertices=len(V),triangles=len(F),bones=names,source_sha256=hashlib.sha256((SRC/'mesh_source.npz').read_bytes()).hexdigest(),original_sha256=hashlib.sha256((SRC/'original.png').read_bytes()).hexdigest(),rigged=True,free_view_ready=False,anchors=anchors.tolist(),parents=parents)
(OUT/'rig.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps({k:report[k] for k in ['vertices','triangles','bones']}))
assert np.isfinite(V).all() and np.allclose(W.sum(1),1) and F.max()<len(V)
