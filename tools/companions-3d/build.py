"""Geschlossene Originalprojektions-Volumen mit glTF-Skelett; keine neue Zeichnung.
Ansichtsgebunden wie Mooslyn A2, nicht fuer eine frei umlaufende Kamera.
"""
from pathlib import Path
import base64, hashlib, io, json, struct, zlib
import numpy as np
from PIL import Image
from scipy.ndimage import distance_transform_edt
ROOT=Path(__file__).resolve().parents[2]
P=ROOT/'prototypes/godot-hd2d'
body=json.loads((P/'data/body_rigs.json').read_text())
steps=json.loads((P/'data/step_rigs.json').read_text())
masks=json.loads((P/'data/character_pose_masks.json').read_text())

def export(key,row):
    species=key.replace('-flight','');flight=key.endswith('-flight')
    path='assets/characters/'+species+('-flight.png' if flight else '-walk.png')
    src=Image.open(P/path).convert('RGBA');a=np.array(src.crop((0,row*256,256,(row+1)*256)))
    pm=masks['res://'+path];ids=np.frombuffer(zlib.decompress(base64.b64decode(pm['deflate'])),np.uint8).reshape(pm['height'],pm['width'])
    mask=(ids[row*256:(row+1)*256,:256]==pm['ids'][row*8])&(a[:,:,3]>30)
    _,near=distance_transform_edt(~mask,return_indices=True)
    rgb=a[:,:,:3].copy();rgb[~mask]=rgb[near[0][~mask],near[1][~mask]]
    stream=io.BytesIO();Image.fromarray(rgb).save(stream,format='PNG');paint=stream.getvalue()
    dist=distance_transform_edt(mask)
    occupied=mask.reshape(128,2,128,2).any(axis=(1,3))
    vertices=[];coords=[];faces=[];lookup={}
    def vertex(x,y,side):
        k=(x,y,side)
        if k not in lookup:
            px,py=x*2,y*2;ix,iy=min(px,255),min(py,255)
            # Ein gewoelbter, geschlossener Koerper statt zwei flacher Karten.
            depth=side*(1.0+min(17.0,dist[iy,ix]*.60))
            lookup[k]=len(vertices);vertices.append([(px-128)/100,(ground-py)/100,depth/100]);coords.append([px,py])
        return lookup[k]
    rig=steps[species]['rows'][row];profile=body[key]['rows'][row];ground=rig['ground']
    for y,x in np.argwhere(occupied):
        top=[vertex(x,y,1),vertex(x+1,y,1),vertex(x+1,y+1,1),vertex(x,y+1,1)]
        back=[vertex(x,y,-1),vertex(x+1,y,-1),vertex(x+1,y+1,-1),vertex(x,y+1,-1)]
        faces.extend([[top[0],top[2],top[1]],[top[0],top[3],top[2]],[back[0],back[1],back[2]],[back[0],back[2],back[3]]])
        for edge,(dx,dy) in enumerate([(0,-1),(1,0),(0,1),(-1,0)]):
            xx,yy=x+dx,y+dy
            if not(0<=xx<128 and 0<=yy<128 and occupied[yy,xx]):
                j=(edge+1)%4;faces.extend([[top[edge],top[j],back[j]],[top[edge],back[j],back[edge]]])
    V=np.array(vertices,dtype='<f4');F=np.array(faces,dtype='<u4');xy=np.array(coords);UV=(xy/256).astype('<f4')
    names=['body','head','tail','cape','arm0','forearm0','arm1','forearm1']
    anchors=[profile['root'],profile['neck'],profile['tail'],profile['cape'],profile['shoulders'][0],profile['elbows'][0],profile['shoulders'][1],profile['elbows'][1]]
    if species=='starlet': names=['body','cape'];anchors=[profile['root'],profile['cape']]
    elif not flight:
        for i,(hip,foot) in enumerate(zip(rig['hips'],rig['feet'])):
            knee=np.array(hip)*.45+np.array(foot)*.55+np.array([-5 if row==0 else (3 if i%2==0 else -3),0])
            names += [f'hip{i}',f'knee{i}',f'foot{i}'];anchors += [hip,knee.tolist(),foot]
    anchors=np.array(anchors);world=np.column_stack([(anchors[:,0]-128)/100,(ground-anchors[:,1])/100,np.zeros(len(names))]).astype('<f4')
    bm=np.frombuffer(zlib.decompress(base64.b64decode(body[key]['mask'])),np.uint8).reshape(768,256,4)[row*256:(row+1)*256]/255
    sm=np.frombuffer(zlib.decompress(base64.b64decode(steps[species]['mask'])),np.uint8).reshape(768,256,4)[row*256:(row+1)*256]/255
    weights=np.zeros((len(V),len(names)))
    for i,(x,y) in enumerate(xy):
        r,g,b,a=bm[min(y,255),min(x,255)]
        if species=='starlet':
            weights[i,1]=g*(1-r);weights[i,0]=1-weights[i,1];continue
        w=weights[i];w[0]=1-r;w[1]=r
        trailing=g*(1-r);w*=1-trailing;w[2 if y>profile['root'][1] else 3]+=trailing
        arms=min(1,b+a);w*=1-arms
        for j,aw in enumerate([b,a]):
            shoulder=np.array(profile['shoulders'][j]);elbow=np.array(profile['elbows'][j]);axis=elbow-shoulder
            t=np.clip((np.dot(np.array([x,y])-shoulder,axis)/max(1,np.dot(axis,axis))-.65)/.5,0,1);t=t*t*(3-2*t)
            w[4+j*2]+=aw/max(1,b+a)*(1-t);w[5+j*2]+=aw/max(1,b+a)*t
        if not flight:
            legs=sm[min(y,255),min(x,255)][:len(rig['feet'])];total=sum(legs);w*=1-min(1,total)
            for j,lw in enumerate(legs):
                knee=anchors[8+3*j+1];foot=anchors[8+3*j+2]
                t=np.clip((y-knee[1]+6)/12,0,1);t=t*t*(3-2*t)
                u=np.clip((y-foot[1]+12)/9,0,1);u=u*u*(3-2*u)
                w[8+3*j:11+3*j]+=lw/max(1,total)*np.array([(1-t)*(1-u),t*(1-u),u])
    J=np.argsort(weights,axis=1)[:,-4:].astype('<u2')
    if J.shape[1]<4:J=np.pad(J,((0,0),(0,4-J.shape[1])))
    W=np.take_along_axis(weights,J,axis=1).astype('<f4')
    if len(names)<4:W[:,len(names):]=0
    W/=W.sum(axis=1)[:,None]
    N=np.zeros_like(V);norm=np.cross(V[F[:,1]]-V[F[:,0]],V[F[:,2]]-V[F[:,0]])
    for j in range(3):np.add.at(N,F[:,j],norm)
    N/=np.maximum(np.linalg.norm(N,axis=1)[:,None],1e-10)
    buf=bytearray();views=[];accessors=[]
    def acc(arr,kind,ct):
        arr=np.ascontiguousarray(arr);buf.extend(b'\0'*(-len(buf)%4));off=len(buf);buf.extend(arr.tobytes());views.append(dict(buffer=0,byteOffset=off,byteLength=arr.nbytes))
        item=dict(bufferView=len(views)-1,componentType=ct,count=len(arr),type=kind)
        if kind=='VEC3':item.update(min=arr.min(0).tolist(),max=arr.max(0).tolist())
        accessors.append(item);return len(accessors)-1
    attrs=dict(POSITION=acc(V,'VEC3',5126),NORMAL=acc(N,'VEC3',5126),TEXCOORD_0=acc(UV,'VEC2',5126),JOINTS_0=acc(J,'VEC4',5123),WEIGHTS_0=acc(W,'VEC4',5126))
    indices=acc(F.reshape(-1),'SCALAR',5125)
    ibm=np.tile(np.eye(4,dtype='<f4'),(len(names),1,1));ibm[:,:3,3]=-world
    inverse=acc(ibm.transpose(0,2,1).reshape(-1,16),'MAT4',5126)
    nodes=[dict(name=key,children=[1,2]),dict(name='OriginalVolume',mesh=0,skin=0),dict(name='Rig',children=list(range(3,3+len(names))))]
    nodes += [dict(name=n,translation=v.tolist()) for n,v in zip(names,world)]
    buf.extend(b'\0'*(-len(buf)%4));off=len(buf);buf.extend(paint);views.append(dict(buffer=0,byteOffset=off,byteLength=len(paint)))
    gltf=dict(asset=dict(version='2.0',generator='Itoeva original companion volume A3'),scene=0,scenes=[dict(nodes=[0])],nodes=nodes,meshes=[dict(primitives=[dict(attributes=attrs,indices=indices,material=0)])],skins=[dict(joints=list(range(3,3+len(names))),skeleton=2,inverseBindMatrices=inverse)],materials=[dict(name='OriginalPaint',doubleSided=True,pbrMetallicRoughness=dict(baseColorTexture=dict(index=0),metallicFactor=0,roughnessFactor=1),extensions=dict(KHR_materials_unlit={}))],extensionsUsed=['KHR_materials_unlit'],images=[dict(bufferView=len(views)-1,mimeType='image/png')],textures=[dict(source=0,sampler=0)],samplers=[dict(magFilter=9729,minFilter=9729,wrapS=33071,wrapT=33071)],accessors=accessors,bufferViews=views,buffers=[dict(byteLength=len(buf))])
    raw=json.dumps(gltf,separators=(',',':')).encode();raw+=b' '*(-len(raw)%4);buf.extend(b'\0'*(-len(buf)%4))
    out=P/'assets/characters'/f'{key}-3d';out.mkdir(exist_ok=True)
    (out/f'view-{row}.glb').write_bytes(struct.pack('<III',0x46546c67,2,28+len(raw)+len(buf))+struct.pack('<II',len(raw),0x4e4f534a)+raw+struct.pack('<II',len(buf),0x004e4942)+buf)
    # Jede Kante muss genau zwei Nachbarn tragen: echtes geschlossenes Volumen.
    edges=np.sort(np.concatenate([F[:,[0,1]],F[:,[1,2]],F[:,[2,0]]]),axis=1)
    _,counts=np.unique(edges,axis=0,return_counts=True)
    assert np.all(counts==2) and np.isfinite(V).all() and np.allclose(W.sum(1),1)
    return dict(key=key,row=row,vertices=len(V),triangles=len(F),bones=names,source=path,source_sha256=hashlib.sha256((P/path).read_bytes()).hexdigest(),closed=True,free_view_ready=False)
if __name__=='__main__':
    report=[export(key,row) for key in ['nocti','starlet','wyrmling','nocti-flight'] for row in range(3)]
    (P/'data/companion_volumes.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps([{k:r[k] for k in ['key','row','triangles']} for r in report]))
