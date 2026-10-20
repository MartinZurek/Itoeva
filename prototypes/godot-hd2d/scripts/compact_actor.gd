extends "res://scripts/character_actor.gd"
# Neue gemalte Figuren verwenden 32 Weltposen und 16 Kampfposen im Originalatlas.
var cell_size:=Vector2i(181,181)
var world_texture: Texture2D
var run_texture: Texture2D
var walk_texture: Texture2D
var world_bounds: Array[Rect2i] = []
var run_bounds: Array[Rect2i] = []
var walk_bounds: Array[Rect2i] = []
var reference_height := 1.0
var run_reference_height := 1.0
var walk_reference_height := 1.0
var run_sheet := false
var walk_sheet := false
static var pose_masks: Dictionary = {}
var mask_textures: Dictionary = {}

func _pose_mask() -> Dictionary:
 if pose_masks.is_empty():pose_masks=JSON.parse_string(FileAccess.get_file_as_string("res://data/character_pose_masks.json"))
 var path:=sprite.texture.resource_path
 var data: Dictionary=pose_masks[path]
 if not metrics.has("pose_masks"):metrics["pose_masks"]={}
 mask_textures=metrics.pose_masks
 if not mask_textures.has(path):
  var raw:=Marshalls.base64_to_raw(data.deflate).decompress(int(data.width)*int(data.height),FileAccess.COMPRESSION_DEFLATE)
  var mask:=Image.create_from_data(int(data.width),int(data.height),false,Image.FORMAT_R8,raw)
  mask_textures[path]=ImageTexture.create_from_image(mask)
 return {"texture":mask_textures[path],"id":float(data.ids[sprite.frame])/255.0}

func _ready() -> void:
 collision_layer=2;collision_mask=1
 collider=CollisionShape3D.new()
 var capsule:=CapsuleShape3D.new();capsule.radius=.25;capsule.height=1.4
 collider.shape=capsule;collider.position.y=capsule.height*.5;add_child(collider)
 sprite=Sprite3D.new();sprite.name="OriginalPainted"+species.capitalize()
 sprite.texture=load(Catalog.data().characters[species].asset)
 world_texture=sprite.texture
 cell_size=Vector2i(sprite.texture.get_width()/8,sprite.texture.get_height()/6)
 if metrics.has(species):frame_bounds.assign(metrics[species].bounds)
 else:
  var bounds: Array=JSON.parse_string(FileAccess.get_file_as_string("res://data/compact_bounds.json"))[species]
  for b in bounds:frame_bounds.append(Rect2i(b[0],b[1],b[2],b[3]))
  metrics[species]={"bounds":frame_bounds.duplicate(),"scales":{}}
 world_bounds.assign(frame_bounds)
 reference_height=float(world_bounds[0].size.y)
 var definition: Dictionary=Catalog.data().characters[species]
 if definition.has("walk_asset"):
  walk_texture=load(definition.walk_asset)
  var walk_data: Dictionary=JSON.parse_string(FileAccess.get_file_as_string("res://data/character_walks.json"))[species]
  for b in walk_data.bounds:walk_bounds.append(Rect2i(b[0],b[1],b[2],b[3]))
  walk_reference_height=float(walk_data.reference_height)
 if definition.has("run_asset"):
  run_texture=load(definition.run_asset)
  var run_data: Dictionary=JSON.parse_string(FileAccess.get_file_as_string("res://data/character_runs.json"))[species]
  for b in run_data.bounds:run_bounds.append(Rect2i(b[0],b[1],b[2],b[3]))
  run_reference_height=float(run_data.reference_height)
 sprite.hframes=8;sprite.vframes=6
 sprite.billboard=BaseMaterial3D.BILLBOARD_ENABLED
 sprite.alpha_cut=SpriteBase3D.ALPHA_CUT_DISCARD;sprite.alpha_scissor_threshold=.12
 sprite.texture_filter=BaseMaterial3D.TEXTURE_FILTER_LINEAR;sprite.shaded=false
 sprite.cast_shadow=GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
 sprite.extra_cull_margin=2.0
 painted_material=ShaderMaterial.new();painted_material.render_priority=10
 painted_material.shader=load("res://shaders/painted_character.gdshader")
 painted_material.set_shader_parameter("atlas",sprite.texture);sprite.material_override=painted_material
 sprite.offset=Vector2(0,cell_size.y*.5-2);add_child(sprite)
 _setup_aura()
 shadow=MeshInstance3D.new();var plane:=PlaneMesh.new();plane.size=Vector2(1.15,.62);shadow.mesh=plane
 var material:=ShaderMaterial.new();material.shader=load("res://shaders/contact_shadow.gdshader");material.render_priority=2
 shadow.material_override=material;shadow.cast_shadow=GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
 get_parent().add_child.call_deferred(shadow)
 set_world_scale(base_pixel_size,jump_height,body_pixel_size);_update_frame(false)

func _update_sprite_scale() -> void:
 var height:=run_reference_height if run_sheet else walk_reference_height if walk_sheet else reference_height
 sprite.pixel_size=base_pixel_size*109.0*Catalog.relative_height(species)/maxf(1,height)
 sprite.position=Vector3.UP*hover
 sprite.offset.y=cell_size.y*.5-2.0 if mounted else float(frame_bounds[sprite.frame].end.y)-cell_size.y*.5
 # Vollständige Konturen erhalten auch überstehende Schweife und Schals.
 var bounds:=frame_bounds[sprite.frame]
 var origin:=Vector2(sprite.frame%8,sprite.frame/8)*Vector2(cell_size)+Vector2(bounds.position)
 var texture_size:=sprite.texture.get_size()
 painted_material.set_shader_parameter("frame_clip",Vector4(origin.x/texture_size.x,origin.y/texture_size.y,bounds.size.x/texture_size.x,bounds.size.y/texture_size.y))
 var cell_origin:=Vector2(sprite.frame%8,sprite.frame/8)*Vector2(cell_size)
 var ratio:=Vector2(bounds.size)/Vector2(cell_size)
 var shift:=Vector2((bounds.get_center().x-cell_size.x*.5)*(-1.0 if sprite.flip_h else 1.0),(1.0-ratio.y)*sprite.offset.y+cell_size.y*.5-bounds.get_center().y)*sprite.pixel_size
 painted_material.set_shader_parameter("has_pose_region",true)
 painted_material.set_shader_parameter("pose_cell",Vector4(cell_origin.x/texture_size.x,cell_origin.y/texture_size.y,cell_size.x/texture_size.x,cell_size.y/texture_size.y))
 painted_material.set_shader_parameter("pose_region",painted_material.get_shader_parameter("frame_clip"))
 painted_material.set_shader_parameter("pose_geometry",Vector4(ratio.x,ratio.y,shift.x,shift.y))
 var mask:=_pose_mask()
 painted_material.set_shader_parameter("pose_mask",mask.texture)
 painted_material.set_shader_parameter("pose_id",mask.id)
 _update_occlusion()

func rest_frame() -> int:
 return 2 if facing.y<0 and absf(facing.y)>absf(facing.x) else 1 if absf(facing.y)>absf(facing.x) else 0

func _update_frame(moving: bool) -> void:
 var depth:=absf(facing.y)>absf(facing.x)
 var flight: bool=species=="nocti" and hover>.06 and state=="idle" and jump_clock<0
 var use_run: bool=run_texture!=null and state=="idle" and jump_clock<0 and ((moving and running and run_blend>=.5) or flight)
 var use_walk: bool=walk_texture!=null and moving and state=="idle" and jump_clock<0 and not use_run
 _use_motion_sheet(use_run,use_walk)
 sprite.flip_h=not depth and facing.x<0
 if use_run:
  sprite.frame=run_frame(gait,clock,facing)
 elif use_walk:
  sprite.frame=walk_frame(gait,facing)
 elif jump_clock>=0:
  var duration:=2*sqrt(2*jump_height/GRAVITY)
  var phase:=clampi(int(jump_clock/maxf(.1,duration)*6),0,5)
  sprite.frame=[11,12,13,13,14,0][phase] if not depth else (16 if facing.y>0 else 20)+phase%4
 elif state=="roll":sprite.frame=[26,27,28,28,29][clampi(int(state_clock/ROLL_SECONDS*5),0,4)]
 elif state=="sit":sprite.frame=15 if not depth else 24 if facing.y>0 else 25
 elif moving:
  var phase:=int(gait*4)%4
  sprite.frame=(16 if facing.y>0 else 20)+phase if depth else 7+phase if running and run_blend>=.5 else 3+phase
 elif recovery_clock<.16:sprite.frame=14 if not depth else rest_frame()
 else:sprite.frame=rest_frame()
 _update_sprite_scale()

func _use_motion_sheet(sprint: bool, walk: bool) -> void:
 if sprint==run_sheet and walk==walk_sheet:return
 run_sheet=sprint;walk_sheet=walk
 sprite.frame=0
 sprite.texture=run_texture if sprint else walk_texture if walk else world_texture
 sprite.hframes=8;sprite.vframes=3 if sprint or walk else 6
 cell_size=Vector2i(sprite.texture.get_width()/8,sprite.texture.get_height()/sprite.vframes)
 frame_bounds.assign(run_bounds if sprint else walk_bounds if walk else world_bounds)
 painted_material.set_shader_parameter("atlas",sprite.texture)

func walk_frame(stride: float, direction: Vector2) -> int:
 var row:=0 if absf(direction.x)>=absf(direction.y) else 1 if direction.y>0 else 2
 return row*8+posmod(int(stride*8),8)

func run_frame(stride: float, time: float, direction: Vector2) -> int:
 var row:=0 if absf(direction.x)>=absf(direction.y) else 1 if direction.y>0 else 2
 var phase:=posmod(int((time*3.2 if species=="nocti" else stride)*8),8)
 # Die abgewandte Zeichnung würde die seitliche Blickrichtung unterbrechen.
 if species=="mosslyn" and row==0:phase=[0,1,3,4,5,6,7,1][phase]
 return row*8+phase

func _hover_target(moving: bool) -> float:
 if state!="idle" or jump_clock>=0 or not is_on_floor():return 0.0
 if species=="nocti" and moving and running:return .48+.025*sin(clock*TAU*3.2)
 if species=="mosslyn" and moving and running:return maxf(0,sin(gait*TAU*2))*.065
 return super._hover_target(moving)

func visual_screen_rect(camera: Camera3D) -> Rect2:
 var bounds:=frame_bounds[sprite.frame];var minimum:=Vector2(INF,INF);var maximum:=Vector2(-INF,-INF)
 for x in [bounds.position.x,bounds.end.x]:
  for y in [bounds.position.y,bounds.end.y]:
   var local_x:=float(x)-cell_size.x*.5
   if sprite.flip_h:local_x=-local_x
   var point:=global_position+sprite.position+camera.global_basis.x*local_x*sprite.pixel_size+camera.global_basis.y*(cell_size.y*.5+sprite.offset.y-float(y))*sprite.pixel_size
   var screen:=camera.unproject_position(point);minimum=minimum.min(screen);maximum=maximum.max(screen)
 return Rect2(minimum,maximum-minimum)
