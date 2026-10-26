"""Moosling A4: frei betrachtbarer Modell-/Rig-Arbeitsstand nach freigegebener Tafel.

Blender 4.2: blender -b --python tools/moosling-spatial/build.py -- [--render]
Keine Bildprojektion. Ergebnisse bleiben bis zur visuellen Abnahme ausserhalb der APK.
"""
from pathlib import Path
import bpy, math, random, json, hashlib, sys
import numpy as np
from mathutils import Vector, Matrix
from math import sin, cos, pi

ROOT = Path(__file__).resolve().parents[2]
OUT = Path(__file__).parent / 'output'
OUT.mkdir(parents=True, exist_ok=True)
random.seed(25102026)
bpy.ops.object.select_all(action='SELECT')
bpy.ops.object.delete(use_global=False)
parts = []
bindings = {}

def material(name, color, rough=.65, metal=0, texture=False):
    m = bpy.data.materials.new(name); m.diffuse_color=(*color,1); m.use_nodes=True
    n=m.node_tree.nodes; l=m.node_tree.links; p=n.get('Principled BSDF')
    p.inputs['Base Color'].default_value=(*color,1)
    p.inputs['Specular IOR Level'].default_value=.25
    p.inputs['Roughness'].default_value=rough; p.inputs['Metallic'].default_value=metal
    if texture:
        # Eingebettete Farbtextur bleibt auch im GLB sichtbar.
        yy,xx=np.mgrid[0:256,0:256]/256
        detail=.88+.12*np.sin(2*pi*(xx*43+.22*np.sin(yy*19)))+.045*np.sin(2*pi*(xx*89+yy*11))
        pixels=np.ones((256,256,4),dtype=np.float32)
        for c in range(3):pixels[:,:,c]=np.clip(color[c]*detail,0,1)
        img=bpy.data.images.new(name+' Farbtextur',256,256,alpha=True)
        img.colorspace_settings.name='Linear Rec.709';img.pixels.foreach_set(pixels.ravel());img.pack()
        it=n.new('ShaderNodeTexImage');it.image=img;l.new(it.outputs['Color'],p.inputs['Base Color'])
        tex=n.new('ShaderNodeTexNoise'); tex.inputs['Scale'].default_value=85
        tex.inputs['Detail'].default_value=2
        bump=n.new('ShaderNodeBump'); bump.inputs['Strength'].default_value=.16
        bump.inputs['Distance'].default_value=.014
        l.new(tex.outputs['Fac'],bump.inputs['Height']); l.new(bump.outputs['Normal'],p.inputs['Normal'])
    return m

fur=material('Fell | warmes Orange',(0.40,.095,.012),texture=True)
fur_light=material('Fell | Lichtspitzen',(.54,.16,.023),texture=True)
fur_dark=material('Fell | dunkler Ansatz',(.22,.042,.005),texture=True)
cream=material('Fell | cremeweiss',(.88,.74,.53),texture=True)
paw=material('Pfoten | dunkelbraun',(.105,.045,.018),texture=True)
pink=material('Ohr | warmes Rosa',(.79,.31,.17),texture=True)
wood=material('Geweih | Holzbraun',(.26,.095,.025),texture=True)
leaves=[material('Blatt | '+str(i),c,texture=True) for i,c in enumerate([
 (.105,.14,.013),(.16,.20,.022),(.07,.105,.009),(.21,.24,.030),(.13,.17,.014)])]
vein=material('Blattadern',(.34,.32,.065))
leather=material('Tasche | Leder',(.20,.073,.025),texture=True)
leather_edge=material('Tasche | Kanten',(.09,.028,.009))
gold=material('Schmuck | altes Gold',(.64,.34,.07),.28,.72)
gem=material('Schmuck | Tuerkis',(.025,.36,.39),.16,.35)
black=material('Auge | Pupille',(.006,.003,.001),.09)
iris=material('Auge | Bernstein',(.47,.17,.018),.17,.12)
eye_white=material('Auge | Elfenbein',(.95,.83,.62),.24)
glint=material('Auge | Glanz',(.98,.98,.94),.08)
nosemat=material('Nase',(.065,.021,.008),.3)

def bind(o, bone):
    parts.append(o); bindings[o.name]=bone
    return o

def ell(name, p, size, mat, bone, seg=24, rings=14):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=seg, ring_count=rings, location=p)
    o=bpy.context.object; o.name=name; o.scale=size
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    o.data.materials.append(mat)
    for f in o.data.polygons:f.use_smooth=True
    return bind(o,bone)

def mesh(name, verts, faces, mat, bone):
    m=bpy.data.meshes.new(name);m.from_pydata(verts,[],faces);m.update()
    o=bpy.data.objects.new(name,m);bpy.context.collection.objects.link(o)
    o.data.materials.append(mat)
    for f in m.polygons:f.use_smooth=True
    return bind(o,bone)

def tube(name, points, radii, mat, bone, sides=9):
    vs=[]; fs=[]
    for i,point in enumerate(points):
        p=Vector(point); tangent=Vector(points[min(i+1,len(points)-1)])-Vector(points[max(i-1,0)])
        tangent.normalize(); u=tangent.cross(Vector((0,1,0)))
        if u.length<.01:u=tangent.cross(Vector((0,0,1)))
        u.normalize();v=tangent.cross(u)
        for j in range(sides):
            a=2*pi*j/sides;vs.append(p+radii[i]*(cos(a)*u+sin(a)*v))
        if i:
            for j in range(sides):
                a=(i-1)*sides+j;b=(i-1)*sides+(j+1)%sides
                fs.append((a,b,b+sides,a+sides))
    fs += [tuple(reversed(range(sides))),tuple(range((len(points)-1)*sides,len(points)*sides))]
    return mesh(name,vs,fs,mat,bone)

def leaf(name, start, end, width, mat, bone, normal=(0,-1,.3), ridge=.022):
    a,b=Vector(start),Vector(end);axis=b-a;n=Vector(normal).normalized()
    cross=axis.cross(n).normalized();vs=[]
    for i in range(7):
        t=i/6;w=sin(pi*t)**.75*width;center=a+t*axis
        vs.extend([center-cross*w,center+n*(ridge*sin(pi*t)),center+cross*w])
    fs=[]
    for i in range(6):
        j=i*3;fs.extend([(j,j+3,j+4,j+1),(j+1,j+4,j+5,j+2)])
    o=mesh(name,vs,fs,mat,bone)
    solid=o.modifiers.new('Blattdicke','SOLIDIFY');solid.thickness=.006
    return o

# Ein einzelnes raeumliches Modell: die verdeckte Seite besitzt echte Geometrie.
ell('Rumpf',(0,0,.91),(.30,.255,.44),fur,'body')
ell('Brust creme',(.247,0,.95),(.075,.185,.29),cream,'body')
ell('Hals',(.06,0,1.29),(.215,.205,.22),fur,'body')
headmesh=ell('Kopf',(.12,0,1.65),(.335,.285,.335),fur,'head',48,32)
facemat=fur.copy();facemat.name='Gesicht | Fell und helle Wangen';headmesh.data.materials[0]=facemat
attr=headmesh.data.color_attributes.new(name='Gesichtsfarbe',type='FLOAT_COLOR',domain='CORNER')
for loop in headmesh.data.loops:
    p=headmesh.data.vertices[loop.vertex_index].co+headmesh.location
    blend=max(0,min(1,(1.58-p.z)/.06))*max(0,min(1,(p.x-.02)/.12))
    base=Vector(fur.diffuse_color[:3]).lerp(Vector(cream.diffuse_color[:3]),blend)
    attr.data[loop.index].color=(*base,1)
node=facemat.node_tree.nodes.new('ShaderNodeVertexColor');node.layer_name='Gesichtsfarbe'
facemat.node_tree.links.new(node.outputs['Color'],facemat.node_tree.nodes.get('Principled BSDF').inputs['Base Color'])
# Stirn ist Teil des Kopfes; keine zweite Kugel vor den Augen.
ell('Schnauze',(.395,0,1.49),(.185,.205,.105),cream,'head')
ell('Unterkiefer',(.40,0,1.445),(.14,.16,.045),cream,'jaw')
ell('Nase',(.556,0,1.52),(.05,.063,.04),nosemat,'head')
for s in [-1,1]:
    # Wangenfarbe liegt direkt auf der Kopfhaut.
    # Augen sind auf der Kopfoberflaeche geneigt, keine flachen Farbflecken.
    normal=Vector((.68,s*.733,0));center=Vector((.337,s*.202,1.685))
    for name,offset,size,mat in [
        ('Augenfassung',0,(.100,.032,.128),fur_dark),
        ('Augenweiss',.006,(.091,.032,.118),fur_light),
        ('Iris',.022,(.083,.024,.110),iris),
        ('Pupille',.039,(.048,.014,.078),black)]:
        eye=ell(name,center+normal*offset,size,mat,'head');eye.rotation_euler.z=-s*.75
    ell('Augenglanz',center+normal*.055+Vector((.006,0,.041)),(.014,.009,.019),glint,'head',12,8)
    ell('Augenglanz klein',center+normal*.054+Vector((.009,0,-.034)),(.007,.006,.009),glint,'head',10,6)
    tube('Laecheln',[(.548,s*.025,1.467),(.503,s*.10,1.452),(.449,s*.158,1.463)],[.006,.006,.003],nosemat,'head',6)
    # Muschel mit dreidimensionalem Rand und sichtbarer Innenseite.
    base=Vector((-.025,s*.20,1.83));tip=Vector((-.31,s*.54,2.015))
    leaf('Ohrmuschel',base,tip,.147,fur_dark,'ear'+str(s),normal=(1,0,.2),ridge=.085)
    leaf('Ohrfell',base+Vector((.015,0,0)),tip+Vector((.012,-s*.02,-.008)),.127,fur_light,'ear'+str(s),normal=(1,0,.2),ridge=.09)
    leaf('Ohr innen',base+Vector((.035,0,.01)),tip+Vector((.033,-s*.048,-.018)),.095,pink,'ear'+str(s),normal=(1,0,.2),ridge=.09)
    # Geweih samt Seitenzweigen bleibt am Kopf starr.
    ant=[(-.025,s*.14,1.91),(-.11,s*.18,2.07),(-.13,s*.19,2.23),(-.08,s*.21,2.34)]
    tube('Geweihstange',ant,[.043,.037,.028,.012],wood,'head')
    tube('Geweih aussen',[ant[1],(-.20,s*.29,2.16),(-.24,s*.34,2.25)],[.031,.021,.009],wood,'head')
    tube('Geweih innen',[ant[2],(-.18,s*.10,2.30),(-.16,s*.08,2.37)],[.023,.016,.007],wood,'head')
    for i in range(3):
        a=Vector(ant[1])+Vector((-.035,s*.045*i,.02*i));b=a+Vector((-.09,s*.11,.065))
        leaf('Geweihblatt',a,b,.027,leaves[i],'head',normal=(1,0,.4),ridge=.009)

# Kleine volumetrische Fellbuendel statt haarloser Kugeloberflaechen.
def fur_tufts(center, radii, count, bone, mats, predicate=lambda n:True, length=.07):
    c=Vector(center);r=Vector(radii);verts=[];faces=[];matids=[]
    for i in range(count):
        z=random.uniform(-1,1);a=random.uniform(0,2*pi);n=Vector((math.sqrt(1-z*z)*cos(a),math.sqrt(1-z*z)*sin(a),z))
        if not predicate(n):continue
        p=c+Vector((r.x*n.x,r.y*n.y,r.z*n.z))
        tangent=Vector((-.45,0,-1));tangent-=n*tangent.dot(n)
        if tangent.length<.01:continue
        tangent.normalize();side=tangent.cross(n).normalized()
        lng=length*random.uniform(.7,1.4);width=lng*.19
        # Schmale, flache, spitz auslaufende Fellstraehnen; keine runden Blaettchen.
        k=len(verts)
        verts += [p-side*width,p+n*.005,p+side*width,
                  p+tangent*lng*.55-side*width*.7+n*.008,
                  p+tangent*lng*.55+n*.014,
                  p+tangent*lng*.55+side*width*.7+n*.008,
                  p+tangent*lng+n*.009]
        faces += [(k,k+3,k+4,k+1),(k+1,k+4,k+5,k+2),(k+3,k+6,k+4),(k+4,k+6,k+5)]
        matids += [random.randrange(len(mats))]*4
    ob=mesh('Fellstraehnen_'+bone,verts,faces,mats[0],bone)
    for m in mats[1:]:ob.data.materials.append(m)
    for f,idx in zip(ob.data.polygons,matids):f.material_index=idx
    return ob

fur_tufts((.12,0,1.65),(.335,.285,.335),2600,'head',[fur,fur_light,fur_dark],lambda n:n.x<.5 or n.z>.25,length=.040)
fur_tufts((0,0,.91),(.30,.255,.44),1800,'body',[fur,fur_light],lambda n:n.x<.6,length=.055)
fur_tufts((.247,0,.95),(.075,.185,.29),900,'body',[cream],lambda n:n.x>.1,length=.05)

# Cape: einzelne ueberlappende Blaetter mit Mittelader, am Schulterknochen getragen.
for row in range(4):
    for j in range(11):
        a=2*pi*j/11; start=Vector((.035+.16*cos(a)-row*.075,.21*sin(a)*(1+row*.05),1.36-row*.06))
        end=start+Vector((-.26-row*.025,.10*sin(a),-.10-row*.035))
        leaf('Umhangblatt',start,end,.067,leaves[(j+row)%5],'cape',normal=(0,sin(a),.7),ridge=.027)
        tube('Blattader',[start,(start+end)*.5+Vector((0,0,.022)),end],[.004,.003,.001],vein,'cape',5)

# Brustschmuck und Ledertasche: echte Vorder-/Rueckseiten, keine Projektionskarte.
ell('Brosche Gold',(.271,0,1.25),(.038,.103,.122),gold,'body')
ell('Brosche Tuerkis',(.305,0,1.258),(.025,.071,.086),gem,'body')
for s in [-1,1]:
    leaf('Broschenblatt',(.28,s*.025,1.19),(.28,s*.073,1.10),.027,gold,'body',normal=(1,0,0))
tube('Taschengurt',[(.12,-.23,1.30),(.22,-.235,1.08),(-.04,-.285,.86),(-.22,-.25,.78)],[.022]*4,leather,'body',6)
bag=ell('Tasche',(-.18,-.30,.82),(.205,.086,.18),leather,'bag')
ell('Taschenklappe',(-.16,-.371,.87),(.19,.034,.104),leather,'bag')
tube('Taschennaht',[(-.34,-.373,.88),(-.31,-.396,.76),(-.12,-.396,.71),(.0,-.372,.82)],[.007]*4,leather_edge,'bag',5)
ell('Taschenknopf',(-.135,-.41,.85),(.027,.013,.027),gold,'bag',16,10)
tail=ell('Schwanz',(-.36,0,.65),(.28,.145,.17),fur,'tail');tail.rotation_euler.y=.35
ell('Schwanzspitze',(-.57,0,.72),(.14,.135,.125),cream,'tail')
fur_tufts((-.38,0,.67),(.30,.15,.18),950,'tail',[fur,fur_light,cream],length=.085)

rest={
 'body':((0,0,.87),(0,0,1.20),None),
 'head':((.06,0,1.35),(.06,0,1.75),'body'),
 'jaw':((.33,0,1.45),(.51,0,1.45),'head'),
 'cape':((0,0,1.34),(-.33,0,1.15),'body'),
 'bag':((-.18,-.3,.97),(-.18,-.3,.74),'body'),
 'tail':((-.20,0,.65),(-.57,0,.72),'body')}
for s in [-1,1]:
    rest['ear'+str(s)]=((-.025,s*.20,1.83),(-.31,s*.54,2.015),'head')
    for kind in ['hind','fore']:
        y=s*(.175 if kind=='hind' else .20)
        a=Vector((-.045,y,.66) if kind=='hind' else (.10,y,1.24))
        b=Vector((.075,y,.36) if kind=='hind' else (.18,y,.92))
        c=Vector((.015,y,.12) if kind=='hind' else (.22,y,.60))
        key=kind+str(s);rest[key+'_upper']=(a,b,'body');rest[key+'_lower']=(b,c,key+'_upper')
        rest[key+'_paw']=(c,c+Vector((.13,0,0)),key+'_lower')
        for name,u,v,r in [(key+'_upper',a,b,.125 if kind=='hind' else .09),(key+'_lower',b,c,.085 if kind=='hind' else .068)]:
            ob=ell(name,(u+v)*.5,(r,r,(v-u).length*.66),fur,name)
            ob.rotation_euler=(v-u).to_track_quat('Z','Y').to_euler()
            ell(name+'_joint',v,(r*.85,r*.85,r*.9),fur,name)
            strands=fur_tufts((u+v)*.5,(r,r,(v-u).length*.55),380,name,[fur,fur_light],length=.035)
            q=(v-u).to_track_quat('Z','Y');center=(u+v)*.5
            for vertex in strands.data.vertices:vertex.co=center+q@(vertex.co-center)
        ell(key+'_paw',c+Vector((.065,0,-.03)),(.13,.09,.075),paw,key+'_paw')
        for t in [-1,0,1]:
            ell(key+'_toe',c+Vector((.15,t*.047,-.045)),(.047,.034,.047),paw,key+'_paw',12,8)

# Zusammenhaengende Haut an Rumpf und Gelenken statt sichtbarer Steckverbindungen.
skin_parts=[o for o in parts if o.name in ['Rumpf','Hals'] or (o.name.startswith(('hind','fore')) and ('_upper' in o.name or '_lower' in o.name))]
bpy.ops.object.select_all(action='DESELECT')
for o in skin_parts:o.select_set(True);parts.remove(o)
bpy.context.view_layer.objects.active=skin_parts[0];bpy.ops.object.join()
skin=bpy.context.object;skin.name='Zusammenhaengende_Haut'
bpy.ops.object.transform_apply(location=True,rotation=True,scale=True)
remesh=skin.modifiers.new('Geschlossene Haut','REMESH');remesh.mode='VOXEL';remesh.voxel_size=.018
bpy.ops.object.modifier_apply(modifier=remesh.name)
smooth=skin.modifiers.new('Gelenkuebergaenge','SMOOTH');smooth.factor=1.3;smooth.iterations=4
bpy.ops.object.modifier_apply(modifier=smooth.name)
for f in skin.data.polygons:f.use_smooth=True
parts.append(skin);bindings[skin.name]='__skin__'

# Ein echtes exportierbares Skelett. Starre Detailteile haben explizite Gewichte.
bpy.ops.object.select_all(action='DESELECT')
arm_data=bpy.data.armatures.new('Moosling_Skelett');rig=bpy.data.objects.new('Moosling_A4_Rig',arm_data)
bpy.context.collection.objects.link(rig);bpy.context.view_layer.objects.active=rig;rig.select_set(True)
bpy.ops.object.mode_set(mode='EDIT')
for name,(a,b,parent) in rest.items():
    bone=arm_data.edit_bones.new(name);bone.head=a;bone.tail=b
    if parent:bone.parent=arm_data.edit_bones[parent]
bpy.ops.object.mode_set(mode='OBJECT')
for ob in parts:
    if bindings[ob.name]=='__skin__':
        candidates=['body']+[n for n in rest if n.endswith(('_upper','_lower'))]
        groups={n:ob.vertex_groups.new(name=n) for n in candidates}
        for v in ob.data.vertices:
            scores=[]
            for n in candidates:
                a,b,_=rest[n];a,b=Vector(a),Vector(b);axis=b-a
                t=max(0,min(1,(v.co-a).dot(axis)/axis.length_squared));dist=(v.co-a-t*axis).length
                radius=.27 if n=='body' else (.12 if n.startswith('hind') and n.endswith('upper') else .08)
                scores.append((dist/radius,n))
            scores.sort();closest=scores[:2]
            weights=[math.exp(-min(50,d*d*3)) for d,n in closest];total=sum(weights)
            for w,(_,n) in zip(weights,closest):groups[n].add([v.index],w/total,'REPLACE')
    else:
        group=ob.vertex_groups.new(name=bindings[ob.name]);group.add(range(len(ob.data.vertices)),1,'REPLACE')
    mod=ob.modifiers.new('Skelett','ARMATURE');mod.object=rig
    ob.parent=rig

REST_M={b.name:b.matrix_local.copy() for b in arm_data.bones}
contacts=[]
def translated(v):return Matrix.Translation(Vector(v))
def set_transform(name, m):
    rig.pose.bones[name].matrix=m@REST_M[name]
    bpy.context.view_layer.update()
def segment(name,a,b):
    ra,rb,_=rest[name];ra,rb=Vector(ra),Vector(rb)
    q=(rb-ra).rotation_difference(Vector(b)-Vector(a))
    m=translated(a)@q.to_matrix().to_4x4()@translated(-ra)
    set_transform(name,m)

def solve(a,c,l1,l2,bend=1):
    a,c=Vector(a),Vector(c);d=(c-a).length
    if d>=l1+l2:raise ValueError('IK-Ziel ausserhalb Reichweite: '+str((d,l1+l2)))
    axis=(c-a)/d;u=Vector((axis.z,0,-axis.x))*bend
    along=(l1*l1-l2*l2+d*d)/(2*d)
    return a+axis*along+u*math.sqrt(max(0,l1*l1-along*along))

def pose(mode,t,travel=False):
    run=mode=='run';stride=1.05 if run else .48;duty=.32 if run else .64
    # A4-Demonstration in Modellmetern. Spielwelt-Skalierung bleibt explizit offen.
    x=stride*t if travel else 0
    bob=(-.04+.015*cos(4*pi*t) if not run else .025*cos(2*pi*(t-.15)))
    root=Vector((x,0,(-.32 if run else 0)+bob))
    angle=math.radians(57) if run else -.025*sin(2*pi*t)
    pivot=Vector((0,0,.87));rot=Matrix.Rotation(angle,4,'Y')
    body=translated(root+pivot)@rot@translated(-pivot)
    set_transform('body',body)
    head_at=body@Vector(rest['head'][0]);head_delta=head_at-Vector(rest['head'][0])
    head=translated(head_delta)@translated(rest['head'][0])@Matrix.Rotation(.018*sin(2*pi*t),4,'Y')@translated(-Vector(rest['head'][0]))
    set_transform('head',head)
    set_transform('jaw',head@translated(rest['jaw'][0])@Matrix.Rotation(.12 if run else 0,4,'Y')@translated(-Vector(rest['jaw'][0])))
    for name,amp in [('cape',.07 if run else .025),('bag',.045),('tail',.08)]:
        a=Vector(rest[name][0]);set_transform(name,body@translated(a)@Matrix.Rotation(amp*sin(2*pi*t-.7),4,'Y')@translated(-a))
    for s in [-1,1]:
        name='ear'+str(s);a=Vector(rest[name][0]);set_transform(name,head@translated(a)@Matrix.Rotation(.025*sin(2*pi*t-.4),4,'X')@translated(-a))
        for kind in ['hind','fore']:
            key=kind+str(s);a,b,_=rest[key+'_upper'];_,c,_=rest[key+'_lower'];a,b,c=map(Vector,[a,b,c]);shoulder=body@a
            if kind=='fore' and not run:
                swing=Matrix.Rotation(.19*sin(2*pi*(t+(0 if s==1 else .5))),4,'Y')
                transform=body@translated(a)@swing@translated(-a)
                for suffix in ['upper','lower','paw']:set_transform(key+'_'+suffix,transform)
                continue
            phase=(t+(0 if s==1 else .5))%1 if not run else (t+(0 if s==1 else .09)+(0 if kind=='fore' else .52))%1
            # In der Standphase kompensiert der lokale Fuss exakt den Root-Fortschritt.
            front=(.45 if kind=='fore' else -.23) if run else .02
            span=stride*duty
            lift=0 if phase<duty else (.18 if run else .105)*sin(pi*(phase-duty)/(1-duty))**1.3
            footx=front+span*(.5-phase/duty) if phase<duty else front-span*.5+span*(.5-.5*cos(pi*(phase-duty)/(1-duty)))
            target=Vector((x+footx,c.y,.12+lift))
            l1=(b-a).length;l2=(c-b).length
            knee=solve(shoulder,target,l1,l2,-1 if kind=='hind' else 1)
            segment(key+'_upper',shoulder,knee);segment(key+'_lower',knee,target)
            set_transform(key+'_paw',translated(target-c))
            contacts.append(dict(mode=mode,t=t,limb=key,stance=phase<duty,target=list(target),reach=(target-shoulder).length,length=l1+l2))

for mode in ['walk','run']:
    rig.animation_data_create();rig.animation_data.action=bpy.data.actions.new(mode)
    for f in range(49):
        bpy.context.scene.frame_set(f+1);pose(mode,f/48)
        for b in rig.pose.bones:
            b.rotation_mode='QUATERNION'
            for prop in ['location','rotation_quaternion','scale']:b.keyframe_insert(prop,frame=f+1)
    for fc in rig.animation_data.action.fcurves:
        for kp in fc.keyframe_points:kp.interpolation='LINEAR'
    track=rig.animation_data.nla_tracks.new();track.name=mode
    track.strips.new(mode,1,rig.animation_data.action)
    rig.animation_data.action=None;track.mute=True

# Figur fuer Export von Licht, Kamera und Pruefboden trennen.
bpy.ops.object.select_all(action='DESELECT');rig.select_set(True)
for ob in parts:
    ob.select_set(True)
    if not ob.data.uv_layers:
        uv=ob.data.uv_layers.new(name='UVMap')
        for loop in ob.data.loops:
            p=ob.data.vertices[loop.vertex_index].co
            uv.data[loop.index].uv=(p.x*3+p.y*4,p.z*3)
bpy.context.view_layer.objects.active=rig
pose('walk',0)
bpy.ops.export_scene.gltf(filepath=str(OUT/'moosling-a4.glb'),use_selection=True,export_format='GLB',export_animations=True,export_nla_strips=True,export_force_sampling=True)

scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.samples=32
scene.cycles.use_denoising=True;scene.render.threads_mode='FIXED';scene.render.threads=4;scene.render.resolution_x=720;scene.render.resolution_y=720;scene.render.resolution_percentage=100
scene.world.color=(.22,.22,.22);scene.render.image_settings.file_format='PNG'
scene.view_settings.view_transform='AgX'
floor=material('Pruefboden',(.115,.105,.09),.85)
bpy.ops.mesh.primitive_plane_add(size=200);bpy.context.object.data.materials.append(floor)
def point_at(o,p):o.rotation_euler=(Vector(p)-o.location).to_track_quat('-Z','Y').to_euler()
for name,p,power,size in [('Key',(3,-4,6),700,5),('Fill',(1,4,3),450,4),('Rim',(-4,1,4),850,3)]:
    bpy.ops.object.light_add(type='AREA',location=p);o=bpy.context.object;o.name=name;o.data.energy=power;o.data.shape='DISK';o.data.size=size;point_at(o,(0,0,1))
bpy.ops.object.camera_add(location=(4,-6,2.8));cam=bpy.context.object;scene.camera=cam;cam.data.type='ORTHO';cam.data.ortho_scale=2.95;point_at(cam,(0,0,1.15))
scene.render.fps=24;scene.frame_start=1;scene.frame_end=48
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'moosling-a4.blend'),compress=True)
report={'status':'work_in_progress_not_apk_approved','reference_sha256':hashlib.sha256((ROOT/'docs/approved-3d-designs-20261025/Moosling_Bewegungsentwurf.png').read_bytes()).hexdigest(),'bones':len(rest),'mesh_objects':len(parts),'polygons':sum(len(o.data.polygons) for o in parts),'contact_samples':len(contacts),'max_reach_fraction':max(c['reach']/c['length'] for c in contacts),'limits':['Materialdetail und Silhouette muessen visuell weiter an die Referenz angenaehert werden.','Modellmetermassstab noch nicht mit Weltgeschwindigkeit kalibriert.','Keine Android- oder Godot-Integration in diesem Arbeitsstand.']}
(OUT/'validation.json').write_text(json.dumps(report,indent=2))
if '--render' in sys.argv or '--stills' in sys.argv:
    for name,mode,t,view in [('walk-front','walk',.12,(4,-6,2.8)),('walk-back','walk',.12,(-4,6,2.8)),('run-side','run',.12,(2,-7,2.2))]:
        pose(mode,t);cam.location=view;point_at(cam,(0,0,1.15));scene.render.filepath=str(OUT/(name+'.png'));bpy.ops.render.render(write_still=True)
    cam.location=(3,-7,2.6);point_at(cam,(0,0,1.15));scene.render.resolution_x=512;scene.render.resolution_y=512
    for mode in ([] if '--stills' in sys.argv else ['walk','run']):
        folder=OUT/mode;folder.mkdir(exist_ok=True)
        for f in range(48):
            pose(mode,f/48);scene.render.filepath=str(folder/('%03d.png'%f));bpy.ops.render.render(write_still=True)
print(json.dumps(report))
