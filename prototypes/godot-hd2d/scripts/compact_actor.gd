extends "res://scripts/character_actor.gd"
# Neue gemalte Figuren verwenden 32 Weltposen und 16 Kampfposen im Originalatlas.
var cell_size:=Vector2i(181,181)

func _ready() -> void:
 collision_layer=2;collision_mask=1
 collider=CollisionShape3D.new()
 var capsule:=CapsuleShape3D.new();capsule.radius=.25;capsule.height=1.4
 collider.shape=capsule;collider.position.y=capsule.height*.5;add_child(collider)
 sprite=Sprite3D.new();sprite.name="OriginalPainted"+species.capitalize()
 sprite.texture=load(Catalog.data().characters[species].asset)
 cell_size=Vector2i(sprite.texture.get_width()/8,sprite.texture.get_height()/6)
 if metrics.has(species):frame_bounds.assign(metrics[species].bounds)
 else:
  var bounds: Array=JSON.parse_string(FileAccess.get_file_as_string("res://data/compact_bounds.json"))[species]
  for b in bounds:frame_bounds.append(Rect2i(b[0],b[1],b[2],b[3]))
  metrics[species]={"bounds":frame_bounds.duplicate(),"scales":{}}
 sprite.hframes=8;sprite.vframes=6
 sprite.billboard=BaseMaterial3D.BILLBOARD_ENABLED
 sprite.alpha_cut=SpriteBase3D.ALPHA_CUT_DISCARD;sprite.alpha_scissor_threshold=.12
 sprite.texture_filter=BaseMaterial3D.TEXTURE_FILTER_LINEAR;sprite.shaded=false
 sprite.cast_shadow=GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
 painted_material=ShaderMaterial.new();painted_material.render_priority=10
 painted_material.shader=load("res://shaders/painted_character.gdshader")
 painted_material.set_shader_parameter("atlas",sprite.texture);sprite.material_override=painted_material
 sprite.offset=Vector2(0,cell_size.y*.5-2);add_child(sprite)
 shadow=MeshInstance3D.new();var plane:=PlaneMesh.new();plane.size=Vector2(1.15,.62);shadow.mesh=plane
 var material:=ShaderMaterial.new();material.shader=load("res://shaders/contact_shadow.gdshader");material.render_priority=2
 shadow.material_override=material;shadow.cast_shadow=GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
 get_parent().add_child.call_deferred(shadow)
 set_world_scale(base_pixel_size,jump_height,body_pixel_size);_update_frame(false)

func _update_sprite_scale() -> void:
 sprite.pixel_size=base_pixel_size*109.0*Catalog.relative_height(species)/maxf(1,float(frame_bounds[0].size.y))
 sprite.position=Vector3.UP*hover
 sprite.offset.y=cell_size.y*.5-2.0 if mounted else float(frame_bounds[sprite.frame].end.y)-cell_size.y*.5
 _update_occlusion()

func rest_frame() -> int:
 return 2 if facing.y<0 and absf(facing.y)>absf(facing.x) else 1 if absf(facing.y)>absf(facing.x) else 0

func _update_frame(moving: bool) -> void:
 var depth:=absf(facing.y)>absf(facing.x)
 sprite.flip_h=not depth and facing.x<0
 if jump_clock>=0:
  var duration:=2*sqrt(2*jump_height/GRAVITY)
  var phase:=clampi(int(jump_clock/maxf(.1,duration)*6),0,5)
  sprite.frame=[11,12,13,13,14,0][phase] if not depth else (16 if facing.y>0 else 20)+phase%4
 elif state=="roll":sprite.frame=[26,27,28,28,29][clampi(int(state_clock/ROLL_SECONDS*5),0,4)]
 elif state=="sit":sprite.frame=15 if not depth else 24 if facing.y>0 else 25
 elif moving:
  var phase:=int(gait*4)%4
  sprite.frame=(16 if facing.y>0 else 20)+phase if depth else [8,7,9,7][phase] if running else 3+phase
 elif recovery_clock<.16:sprite.frame=14 if not depth else rest_frame()
 else:sprite.frame=rest_frame()
 _update_sprite_scale()

func visual_screen_rect(camera: Camera3D) -> Rect2:
 var bounds:=frame_bounds[sprite.frame];var minimum:=Vector2(INF,INF);var maximum:=Vector2(-INF,-INF)
 for x in [bounds.position.x,bounds.end.x]:
  for y in [bounds.position.y,bounds.end.y]:
   var local_x:=float(x)-cell_size.x*.5
   if sprite.flip_h:local_x=-local_x
   var point:=global_position+sprite.position+camera.global_basis.x*local_x*sprite.pixel_size+camera.global_basis.y*(cell_size.y*.5+sprite.offset.y-float(y))*sprite.pixel_size
   var screen:=camera.unproject_position(point);minimum=minimum.min(screen);maximum=maximum.max(screen)
 return Rect2(minimum,maximum-minimum)
