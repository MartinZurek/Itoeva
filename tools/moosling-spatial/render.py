"""Render ausschliesslich aus dem gespeicherten Blender-Modell und seinen Clips."""
from pathlib import Path
import bpy, sys
from mathutils import Vector

OUT=Path(__file__).parent/'output'
mode=sys.argv[sys.argv.index('--')+1]
bpy.ops.wm.open_mainfile(filepath=str(OUT/'moosling-a4.blend'))
scene=bpy.context.scene;rig=bpy.data.objects['Moosling_A4_Rig'];cam=scene.camera
rig.animation_data.action=None
for track in rig.animation_data.nla_tracks:track.mute=track.name!=mode
scene.render.resolution_x=512;scene.render.resolution_y=512
scene.cycles.samples=16;scene.cycles.use_denoising=True
scene.render.threads_mode='FIXED';scene.render.threads=4
floor=bpy.data.materials['Pruefboden'];nodes=floor.node_tree.nodes;links=floor.node_tree.links
checker=nodes.new('ShaderNodeTexChecker');checker.inputs['Scale'].default_value=2
checker.inputs['Color1'].default_value=(.13,.12,.10,1)
checker.inputs['Color2'].default_value=(.10,.092,.075,1)
coords=nodes.new('ShaderNodeTexCoord');links.new(coords.outputs['Object'],checker.inputs['Vector'])
links.new(checker.outputs['Color'],nodes.get('Principled BSDF').inputs['Base Color'])
folder=OUT/(mode+'-frames');folder.mkdir(exist_ok=True)
for frame in range(32):
    if '--resume' in sys.argv and (folder/('%03d.png'%frame)).exists():continue
    position=1+frame*48/32
    scene.frame_set(int(position),subframe=position%1)
    # Kamera folgt dem tatsaechlichen Root-Fortschritt ueber einem festen Boden.
    distance=(1.05 if mode=='run' else .48)*frame/32
    rig.location.x=distance;cam.location=(distance+2,-7,2.65)
    target=Vector((distance,0,1.15));cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler()
    scene.render.filepath=str(folder/('%03d.png'%frame));bpy.ops.render.render(write_still=True)
print('RENDER_COMPLETE',mode,32)
