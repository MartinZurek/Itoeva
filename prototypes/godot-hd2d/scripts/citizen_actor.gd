extends "res://scripts/character_actor.gd"
const Residents = preload("res://scripts/world_residents.gd")
var atlas_rows: Array = []
var row := 0
var pose := 0
var cell: AtlasTexture
var reference_height := 300.0
func _ready() -> void:
 player_controlled = false
 row = int(Residents.data()[species].row)
 atlas_rows = JSON.parse_string(FileAccess.get_file_as_string(Residents.data()[species].get("atlas", "res://data/citizens_atlas.json")))
 reference_height = float(atlas_rows[row][0][3])
 collision_layer = 2; collision_mask = 1
 collider = CollisionShape3D.new()
 var capsule := CapsuleShape3D.new(); capsule.radius=.25; capsule.height=1.4
 collider.shape=capsule; collider.position.y=.7; add_child(collider)
 sprite=Sprite3D.new(); sprite.billboard=BaseMaterial3D.BILLBOARD_ENABLED
 cell=AtlasTexture.new();cell.atlas=load(Residents.data()[species].get("asset", "res://assets/expansion/citizens.png"))
 sprite.texture=cell; sprite.shaded=false; sprite.alpha_cut=SpriteBase3D.ALPHA_CUT_DISCARD
 sprite.alpha_scissor_threshold=.12; sprite.texture_filter=BaseMaterial3D.TEXTURE_FILTER_LINEAR
 sprite.cast_shadow=GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
 painted_material=ShaderMaterial.new();painted_material.shader=load("res://shaders/painted_character.gdshader")
 painted_material.render_priority=10;painted_material.set_shader_parameter("atlas",cell)
 sprite.material_override=painted_material;add_child(sprite)
 shadow=MeshInstance3D.new();var plane:=PlaneMesh.new();plane.size=Vector2(1,.55);shadow.mesh=plane
 var material:=ShaderMaterial.new();material.shader=load("res://shaders/contact_shadow.gdshader");material.render_priority=2
 shadow.material_override=material;shadow.cast_shadow=GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
 get_parent().add_child.call_deferred(shadow)
 _update_frame(false)
func rest_frame() -> int: return 0
func _update_frame(moving: bool) -> void:
 pose = 1+int(gait*4)%4 if moving else 5 if fmod(clock,7.0)>2.4 else 0
 var b: Array=atlas_rows[row][pose]
 cell.region=Rect2(b[0],b[1],b[2],b[3])
 sprite.offset=Vector2(0,float(b[3])*.5)
 sprite.flip_h=facing.x<0
 _update_sprite_scale()
func _update_sprite_scale() -> void:
 if not is_instance_valid(sprite):return
 sprite.pixel_size=base_pixel_size*109.0*Catalog.relative_height(species)/reference_height
 _update_occlusion()
func visual_screen_rect(camera: Camera3D) -> Rect2:
 var b: Array=atlas_rows[row][pose]
 var foot:=camera.unproject_position(global_position)
 var top:=camera.unproject_position(global_position+camera.global_basis.y*float(b[3])*sprite.pixel_size)
 var w: float=absf(foot.y-top.y)*float(b[2])/float(b[3])
 return Rect2(Vector2(foot.x-w*.5,top.y),Vector2(w,foot.y-top.y))
