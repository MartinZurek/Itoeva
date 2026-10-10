#!/usr/bin/env python3
"""Reproduzierbares echtes glTF-Modell; keine gerenderten Sprite-Bilder.

Nur Python-Standardbibliothek. Gelenke tragen starre Teilmeshes; kein Skinning.
Y zeigt nach oben, Z zur Gesichtsvorderseite. Alle Laengen in Modellmetern.
"""
import json, math, struct, pathlib, base64
from collections import defaultdict

ROOT = pathlib.Path(__file__).resolve().parents[2]
OUT = ROOT / 'app-sim/src/game/assets/models/fennec-prototype.glb'
COLORS = {'fur': (0.82,.34,.08), 'cream': (.98,.82,.55),
          'cloth': (.48,.13,.065),
          'seam': (.73,.30,.12), 'leather': (.22,.13,.075),
          'gold': (.76,.51,.20), 'gem': (.075,.56,.51),
          'ink': (.055,.036,.023), 'eye': (.96,.56,.075),
          'white': (1,.95,.79)}
nodes, parts, materials = [], defaultdict(list), list(COLORS)

def node(name, parent=None, t=(0,0,0)):
    i=len(nodes); nodes.append({'name':name,'translation':list(t)})
    if parent is not None: nodes[parent].setdefault('children',[]).append(i)
    return i

def surface(owner, color, fn, rings=16, segments=24, wrap=True):
    verts=[]; faces=[]
    for j in range(rings+1):
        for i in range(segments+1):
            verts.append(fn(j/rings, i/segments))
    for j in range(rings):
        for i in range(segments):
            a=j*(segments+1)+i; b=a+segments+1
            faces.extend([(a,b,a+1),(a+1,b,b+1)])
    # Flachheitsfehler vermeiden: Normalen aus der wirklichen Oberflaeche.
    norms=[[0.,0.,0.] for _ in verts]
    for a,b,c in faces:
        ab=[verts[b][k]-verts[a][k] for k in range(3)]
        ac=[verts[c][k]-verts[a][k] for k in range(3)]
        n=[ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2],ab[0]*ac[1]-ab[1]*ac[0]]
        for ix in (a,b,c):
            for k in range(3): norms[ix][k]+=n[k]
    if wrap:
        for j in range(rings+1):
            a=j*(segments+1);b=a+segments
            n=[norms[a][k]+norms[b][k] for k in range(3)]
            norms[a]=n[:];norms[b]=n[:]
    norms=[tuple(k/(math.sqrt(sum(v*v for v in n)) or 1) for k in n) for n in norms]
    parts[owner].append((color,verts,norms,faces))

def ell(owner,color,c,r, rings=12, seg=20, tilt=0):
    def fn(v,u):
        p=math.pi*(1-v); a=math.tau*u
        x=r[0]*math.sin(p)*math.cos(a); y=r[1]*math.cos(p); z=r[2]*math.sin(p)*math.sin(a)
        return (c[0]+x*math.cos(tilt)-y*math.sin(tilt), c[1]+x*math.sin(tilt)+y*math.cos(tilt), c[2]+z)
    surface(owner,color,fn,rings,seg)

def ear(owner,side):
    # Zwei geschlossene spitz zulaufende Schalen, echte Dicke und Vertiefung.
    def shape(inner=False):
        def fn(v,u):
            a=u*math.tau; width=(.15 if not inner else .106)*math.sin(math.pi*v)**.75
            return (side*(.15+.27*v)+width*math.cos(a), .08+.78*v,
                    .018+(.073 if not inner else .025)*math.sin(math.pi*v)*math.sin(a)+(0.044 if inner else 0))
        return fn
    surface(owner,'fur',shape(),18,20)
    surface(owner,'cream',shape(True),18,20)
    ell(owner,'cream',(side*.18,.16,.065),(.08,.18,.046),tilt=-side*.32)

rig=node('Fennec'); hips=node('hips',rig,(0,.92,0)); torso=node('torso',hips,(0,.31,0))
ell(torso,'leather',(0,0,0),(.255,.39,.175))
ell(hips,'fur',(0,-.01,.035),(.225,.17,.15))
ell(torso,'cream',(0,.22,.14),(.15,.23,.085))
ell(torso,'leather',(0,-.12,0),(.272,.051,.194),rings=8)
ell(torso,'gold',(0,-.12,.20),(.045,.043,.014),rings=8,seg=12)
for s in (-1,1):
    ell(torso,'leather',(s*.25,-.15,.08),(.058,.085,.072),rings=8)
    leg=node('hip'+str(s),hips,(s*.14,-.05,0))
    ell(leg,'cream',(0,-.12,0),(.105,.20,.103))
    knee=node('knee'+str(s),leg,(0,-.27,0))
    ell(knee,'fur',(0,-.11,.01),(.08,.145,.08))
    foot=node('foot'+str(s),knee,(0,-.25,0))
    ell(foot,'leather',(0,.015,.026),(.099,.125,.098))
    ell(foot,'leather',(0,-.08,.10),(.106,.073,.185))
    ell(foot,'gold',(0,.046,.094),(.087,.016,.025),rings=6,seg=12)
    arm=node('shoulder'+str(s),torso,(s*.24,.20,0))
    ell(arm,'leather',(s*.07,-.105,0),(.078,.18,.075),tilt=s*.26)
    elbow=node('elbow'+str(s),arm,(s*.12,-.25,0))
    ell(elbow,'fur',(0,-.1,.02),(.06,.125,.06))
    ell(elbow,'leather',(0,-.20,.025),(.073,.105,.07))
    for f in range(3): ell(elbow,'leather',((f-1)*.027,-.27,.037),(.017,.044,.026),rings=6,seg=8)

head=node('head',torso,(0,.43,0))
ell(head,'fur',(0,.15,0),(.34,.29,.235),rings=20,seg=28)
for s in (-1,1):
    ear(head,s)
    ell(head,'cream',(s*.22,.01,.134),(.12,.075,.10),tilt=s*.18)
    # Augen sitzen auf der runden Stirn, nicht auf einer gesichtsweiten Platte.
    ell(head,'ink',(s*.145,.175,.206),(.088,.052,.032),tilt=-s*.15)
    ell(head,'eye',(s*.146,.175,.233),(.069,.040,.017),tilt=-s*.15)
    ell(head,'ink',(s*.146,.176,.248),(.015,.035,.009),rings=8,seg=12)
    ell(head,'white',(s*.133,.192,.257),(.009,.011,.006),rings=6,seg=8)
    ell(head,'fur',(s*.15,.233,.208),(.10,.032,.025),tilt=-s*.16)
ell(head,'cream',(0,.012,.254),(.16,.075,.145))
ell(head,'ink',(0,.049,.367),(.052,.031,.037),rings=8,seg=12)
ell(head,'ink',(0,-.025,.35),(.082,.005,.016),rings=6,seg=12)

cape=node('cape',torso,(0,.30,-.035))
def cloth(v,u):
    a=.72+u*(math.tau-1.44); radius=.25+.28*v
    fold=.014*math.sin(u*math.pi*12)*v
    return ((radius+fold)*math.sin(a),-.76*v+.045*math.cos(u*math.pi*10)*v*v,
            (radius+fold)*math.cos(a)-.018*v)
surface(cape,'cloth',cloth,15,32,False)
# Blattadern sind schmale echte Baender auf dem Stoff, keine aufgeklebte Ansicht.
for leaf in range(1,8):
    center=leaf/8
    def vein(v,u,center=center):
        x,y,z=cloth(v,center+(u-.5)*.005)
        return(x*1.012,y,z*1.012)
    surface(cape,'seam',vein,12,1,False)
# Saum folgt der Kontur und bleibt beim Drehen sichtbar.
for k in range(17):
    u=k/16;x,y,z=cloth(.97,u)
    ell(cape,'seam',(x,y,z),(.018,.027,.018),rings=5,seg=6)
ell(torso,'cloth',(0,.32,0),(.27,.095,.215))
ell(torso,'gold',(0,.27,.222),(.066,.077,.022),rings=12,seg=20)
ell(torso,'gem',(0,.27,.241),(.049,.059,.018),rings=12,seg=20)

tail=node('tail',hips,(0,.02,-.13))
def tailfn(v,u):
    a=u*math.tau
    # Grosse gebogene Fuchsform, keine Kette einzelner Kugeln.
    x=-.20*v-.42*math.sin(v*math.pi*.70); y=.06+.38*v+.06*math.sin(v*math.pi)
    z=-.08-.46*v
    tangent=(-.20-.42*math.pi*.70*math.cos(v*math.pi*.70),
             .38+.06*math.pi*math.cos(v*math.pi),-.46)
    length=math.sqrt(sum(t*t for t in tangent)); tx,ty,tz=(t/length for t in tangent)
    length=math.sqrt(tx*tx+tz*tz); nx,nz=-tz/length,tx/length
    bx,by,bz=ty*nz,tz*nx-tx*nz,-ty*nx
    r=.21*max(0,math.sin(math.pi*v))**.65+.035*(1-v)
    return (x+r*(nx*math.cos(a)+bx*math.sin(a)),
            y+r*by*math.sin(a), z+r*(nz*math.cos(a)+bz*math.sin(a)))
surface(tail,'fur',lambda v,u:tailfn(v*.72,u),16,20)
surface(tail,'cream',lambda v,u:tailfn(.72+v*.28,u),10,20)

blob=bytearray(); views=[];accessors=[];meshes=[]
def accessor(data,fmt,kind,count):
    while len(blob)%4:blob.append(0)
    off=len(blob);blob.extend(struct.pack('<'+fmt*len(data),*data))
    vi=len(views);view={'buffer':0,'byteOffset':off,'byteLength':len(blob)-off}
    if kind=='VEC3':view['target']=34962
    elif fmt=='H':view['target']=34963
    views.append(view)
    ac={'bufferView':vi,'componentType':5126 if fmt=='f' else 5123,'count':count,'type':kind}
    if kind=='VEC3' and fmt=='f':ac.update(min=[min(data[k::3]) for k in range(3)],max=[max(data[k::3]) for k in range(3)])
    if kind=='SCALAR' and fmt=='f':ac.update(min=[min(data)],max=[max(data)])
    accessors.append(ac);return len(accessors)-1
triangles=0
for owner, entries in parts.items():
    prim=[]
    for color in dict.fromkeys(e[0] for e in entries):
        vertices=[];norms=[];ix=[]
        for c,v,n,f in entries:
            if c!=color:continue
            base=len(vertices);vertices+=v;norms+=n;ix += [i+base for face in f for i in face]
        triangles+=len(ix)//3
        prim.append({'attributes':{'POSITION':accessor([x for v in vertices for x in v],'f','VEC3',len(vertices)),
                                   'NORMAL':accessor([x for n in norms for x in n],'f','VEC3',len(norms))},
                     'indices':accessor(ix,'H','SCALAR',len(ix)), 'material':materials.index(color)})
    nodes[owner]['mesh']=len(meshes);meshes.append({'primitives':prim})

animations=[]
for name,duration,amplitude in [('idle',3.0,0),('walk',1.15,.48),('run',.70,.80)]:
    times=[duration*i/32 for i in range(33)];ta=accessor(times,'f','SCALAR',len(times));samplers=[];channels=[]
    for i,n in enumerate(nodes):
        nm=n['name'];rots=[]
        for t in times:
            phase=t/duration*math.tau;s=-1 if nm.endswith('-1') else 1
            a=0;axis='x'
            if nm.startswith('hip') and nm!='hips':a=s*amplitude*math.sin(phase)
            elif nm.startswith('knee'):a=-amplitude*1.2*max(0,s*math.sin(phase))
            elif nm.startswith('foot'):a=amplitude*.45*max(0,s*math.sin(phase))
            elif nm.startswith('shoulder'):a=-s*amplitude*.75*math.sin(phase)
            elif nm.startswith('elbow'):a=-.16-amplitude*.3*(1+s*math.sin(phase))
            elif nm=='tail':a=.08*math.sin(phase);axis='y'
            elif nm=='cape':a=.035*math.sin(phase+.4)
            elif nm=='head':a=.025*math.sin(phase);axis='y'
            else:continue
            q=[0,0,0,math.cos(a/2)];q['xyz'.index(axis)]=math.sin(a/2);rots.extend(q)
        if not rots:continue
        samplers.append({'input':ta,'output':accessor(rots,'f','VEC4',len(times)),'interpolation':'LINEAR'})
        channels.append({'sampler':len(samplers)-1,'target':{'node':i,'path':'rotation'}})
    animations.append({'name':name,'samplers':samplers,'channels':channels})
gltf={'asset':{'version':'2.0','generator':'Itoeva Fennec prototype / build_model.py'},
      'scene':0,'scenes':[{'nodes':[rig]}],'nodes':nodes,'meshes':meshes,
      'materials':[{'name':c,'doubleSided':True,'pbrMetallicRoughness':{'baseColorFactor':[*COLORS[c],1],
                    'metallicFactor':0,'roughnessFactor':.88}} for c in materials],
      'animations':animations,'buffers':[{'byteLength':len(blob)}],'bufferViews':views,'accessors':accessors}
j=json.dumps(gltf,separators=(',',':')).encode();j+=b' '*((-len(j))%4);blob+=b'\0'*((-len(blob))%4)
glb=struct.pack('<III',0x46546c67,2,12+8+len(j)+8+len(blob))+struct.pack('<II',len(j),0x4e4f534a)+j+struct.pack('<II',len(blob),0x004e4942)+blob
OUT.parent.mkdir(parents=True,exist_ok=True);OUT.write_bytes(glb)
fixture=ROOT/'app-sim/src/androidTest/assets/models/fennec-prototype.glb'
fixture.parent.mkdir(parents=True,exist_ok=True);fixture.write_bytes(glb)
preview=ROOT/'docs/fennec-3d/preview.html';preview.parent.mkdir(parents=True,exist_ok=True)
preview.write_text((pathlib.Path(__file__).with_name('preview.template.html')).read_text().replace('__MODEL_BASE64__',base64.b64encode(glb).decode()))
print(f'{OUT}: {len(glb):,} bytes; {triangles:,} triangles; {len(nodes)} nodes; idle/walk/run')
