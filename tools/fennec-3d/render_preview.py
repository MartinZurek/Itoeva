#!/usr/bin/env python3
"""GLB wirklich mit Tiefenpuffer rendern; nur fuer Entwicklungs-Vorschauen.

pip install numpy moderngl Pillow; EGL/Mesa fuer headless Linux.
"""
import json,math,struct,pathlib,io
import numpy as np
import moderngl
from PIL import Image,ImageDraw,ImageFont

ROOT=pathlib.Path(__file__).resolve().parents[2]
data=(ROOT/'app-sim/src/game/assets/models/fennec-prototype.glb').read_bytes()
size=struct.unpack_from('<I',data,12)[0];g=json.loads(data[20:20+size]);binary=data[28+size:]
def array(i):
    a=g['accessors'][i];v=g['bufferViews'][a['bufferView']];n={'SCALAR':1,'VEC2':2,'VEC3':3,'VEC4':4}[a['type']]
    return np.frombuffer(binary,dtype='<f4' if a['componentType']==5126 else '<u2',count=a['count']*n,
                         offset=v.get('byteOffset',0)+a.get('byteOffset',0)).reshape(-1,n)
ctx=moderngl.create_standalone_context(backend='egl')
ctx.enable(moderngl.DEPTH_TEST)
program=ctx.program(vertex_shader='''#version 330
in vec3 aPosition; in vec3 aNormal; in vec2 aUv; uniform mat4 uMvp; uniform mat4 uWorld;
out vec3 vNormal;out vec2 vUv; void main(){vUv=aUv;vNormal=mat3(uWorld)*aNormal;gl_Position=uMvp*vec4(aPosition,1.0);}
''',fragment_shader='''#version 330
in vec3 vNormal;in vec2 vUv; uniform vec4 uColor;uniform sampler2D uTexture;uniform float uTextured;out vec4 color;
void main(){vec3 n=normalize(vNormal);if(!gl_FrontFacing)n=-n;
float d=max(dot(n,normalize(vec3(-0.6,0.8,0.7))),0.0);
vec3 base=uColor.rgb;if(uTextured>.5)base*=texture(uTexture,vUv).rgb;
color=vec4(base*(vec3(.68,.66,.61)+d*vec3(.37,.34,.29)),1.0);}
''')
image_view=g['bufferViews'][g['images'][0]['bufferView']];off=image_view['byteOffset']
atlas=Image.open(io.BytesIO(binary[off:off+image_view['byteLength']])).convert('RGB')
texture=ctx.texture(atlas.size,3,atlas.tobytes());texture.filter=(moderngl.LINEAR,moderngl.LINEAR);texture.use(0)
program['uTexture'].value=0
meshes=[]
for mesh in g['meshes']:
    ps=[]
    for p in mesh['primitives']:
        attrs=p['attributes'];v=ctx.buffer(array(attrs['POSITION']).tobytes());n=ctx.buffer(array(attrs['NORMAL']).tobytes())
        ix=ctx.buffer(array(p['indices']).tobytes())
        uv=ctx.buffer(array(attrs['TEXCOORD_0']).tobytes() if 'TEXCOORD_0' in attrs else np.zeros((len(array(attrs['POSITION'])),2),dtype='f4').tobytes())
        vao=ctx.vertex_array(program,[(v,'3f','aPosition'),(n,'3f','aNormal'),(uv,'2f','aUv')],ix,index_element_size=2)
        mat=g['materials'][p['material']]['pbrMetallicRoughness']
        ps.append((vao,mat['baseColorFactor'], 'baseColorTexture' in mat))
    meshes.append(ps)
def quat(q):
    x,y,z,w=q
    return np.array([[1-2*(y*y+z*z),2*(x*y-z*w),2*(x*z+y*w),0],
                     [2*(x*y+z*w),1-2*(x*x+z*z),2*(y*z-x*w),0],
                     [2*(x*z-y*w),2*(y*z+x*w),1-2*(x*x+y*y),0],[0,0,0,1]],dtype='f4')
def look(eye,target):
    eye=np.array(eye);f=np.array(target)-eye;f=f/np.linalg.norm(f)
    s=np.cross(f,[0,1,0]);s=s/np.linalg.norm(s);u=np.cross(s,f)
    m=np.eye(4);m[:3,:3]=[s,u,-f];m[:3,3]=-m[:3,:3]@eye;return m
def pose(name,t):
    anim=next(a for a in g['animations'] if a['name']==name);out={}
    for c in anim['channels']:
        s=anim['samplers'][c['sampler']];times=array(s['input']).ravel();qs=array(s['output'])
        tt=t%times[-1];k=min(len(times)-2,np.searchsorted(times,tt,side='right')-1);f=(tt-times[k])/(times[k+1]-times[k])
        q=qs[k]*(1-f)+qs[k+1]*f;q/=np.linalg.norm(q);out[c['target']['node']]=q
    return out
fbo=ctx.simple_framebuffer((640,640));fbo.use()
def disc(radius,height,color):
    v=[[0,height,0]]+[[radius*math.cos(i*math.tau/32),height,radius*math.sin(i*math.tau/32)] for i in range(32)]
    ix=[[0,(i+1)%32+1,i+1] for i in range(32)]
    vao=ctx.vertex_array(program,[(ctx.buffer(np.array(v,dtype='f4').tobytes()),'3f','aPosition'),
                                 (ctx.buffer(np.array([[0,1,0]]*33,dtype='f4').tobytes()),'3f','aNormal'),
                                 (ctx.buffer(np.zeros((33,2),dtype='f4').tobytes()),'2f','aUv')],
                         ctx.buffer(np.array(ix,dtype='u2').tobytes()),index_element_size=2)
    return vao,color
floor=[disc(1.1,.18,(.20,.245,.205,1)),disc(.34,.182,(.10,.13,.10,1))]
def render(yaw,clip='idle',t=0):
    fbo.clear(.105,.15,.15,1,depth=1)
    projection=np.diag([1/1.62,1/1.62,-2/29.9,1.]);projection[2,3]=-30.1/29.9
    vp=projection@look((3.2,2.55,6),(0,1.35,0))
    root=quat((0,math.sin(math.radians(yaw)/2),0,math.cos(math.radians(yaw)/2)))
    rotations=pose(clip,t)
    program['uMvp'].write(np.array(vp,dtype='f4').T.tobytes());program['uWorld'].write(np.eye(4,dtype='f4').tobytes())
    for vao,color in floor:program['uTextured'].value=0;program['uColor'].value=color;vao.render()
    def draw(i,parent):
        node=g['nodes'][i];m=quat(rotations.get(i,[0,0,0,1]));m[:3,3]=node['translation'];world=parent@m
        program['uMvp'].write(np.array(vp@world,dtype='f4').T.tobytes());program['uWorld'].write(np.array(world,dtype='f4').T.tobytes())
        for vao,color,textured in meshes[node['mesh']] if 'mesh' in node else []:
            program['uTextured'].value=int(textured);program['uColor'].value=color;vao.render()
        for j in node.get('children',[]):draw(j,world)
    draw(0,root)
    return Image.frombytes('RGB',(640,640),fbo.read(components=3)).transpose(Image.Transpose.FLIP_TOP_BOTTOM)
out=ROOT/'docs/fennec-3d';out.mkdir(parents=True,exist_ok=True)
views=[('Vorne',28),('Dreiviertel',-15),('Seite',-62),('Hinten',208)]
sheet=Image.new('RGB',(1280,1344),(27,38,38));d=ImageDraw.Draw(sheet)
font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',24)
for i,(label,yaw) in enumerate(views):
    x=i%2*640;y=i//2*672;sheet.paste(render(yaw),(x,y+32));d.text((x+24,y+4),label,fill=(244,218,175),font=font)
sheet.save(out/'fennec-views.jpg',quality=93)
frames=[]
for i in range(60):
    im=render(-15+360*i/60,'walk',i/30);frames.append(im.resize((384,384)).quantize(colors=96))
frames[0].save(out/'fennec-turntable.gif',save_all=True,append_images=frames[1:],duration=66,loop=0,optimize=False)
print('Preview: real GLB geometry, depth test, animated nodes. Renderer:',ctx.info['GL_RENDERER'])
