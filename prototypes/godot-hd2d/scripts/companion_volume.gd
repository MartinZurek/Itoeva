extends "res://scripts/mosslyn_volume.gd"

# Dieselbe Originalprojektion wie Mooslyn; jede feste Blickrichtung besitzt
# ihr eigenes geschlossenes Volumen. Eine freie Kamera ist weiterhin ungeeignet.
var species := "starlet"
var active_key := ""
var models: Dictionary = {}
var lift := 0.0

func _ready() -> void:
 pass

func select_view(row: int, flight: bool) -> void:
 var key := (species + "-flight" if flight else species) + "/" + str(row)
 if key == active_key: return
 for cached in models.values(): cached.visible = false
 if not models.has(key):
  var path := "res://assets/characters/%s-3d/view-%d.glb" % [species + "-flight" if flight else species, row]
  var model = load(path).instantiate()
  add_child(model)
  models[key] = model
 var model: Node3D = models[key]
 model.visible = true
 active_key = key
 skeleton = _find_skeleton(model)
 surface = _find_surface(model)
 joints.clear()
 for i in skeleton.get_bone_count(): joints[skeleton.get_bone_name(i)] = i
 material = ShaderMaterial.new()
 material.shader = preload("res://shaders/mosslyn_volume.gdshader")
 material.set_shader_parameter("atlas", surface.mesh.surface_get_material(0).albedo_texture)
 surface.material_override = material
 surface.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
 surface.extra_cull_margin = 1.0

func _place(joint: String, point: Vector2, angle: float, ground: float, stretch: float = 1.0) -> void:
 var index: int = joints[joint]
 skeleton.set_bone_pose_position(index, Vector3((point.x-128.0)/100.0, (ground-point.y)/100.0, 0))
 skeleton.set_bone_pose_rotation(index, Quaternion(Vector3.BACK, -angle))
 skeleton.set_bone_pose_scale(index, Vector3(stretch, stretch, 1.0))

func apply_pose(p: Dictionary, profile: Dictionary, ground: float) -> void:
 skeleton.reset_bone_poses()
 var Body = preload("res://scripts/full_body_gait.gd")
 _place("body", p.root+p.offset, p.angle, ground)
 lift = p.lift
 if species == "starlet":
  # Ein Stern hat weder Fussgelenke noch einen alternierenden Beintakt.
  skeleton.set_bone_pose_scale(joints.body, Vector3(1.0+p.squash*.35, 1.0-p.squash, 1.0))
  _place("cape", Body.torso(Body.point(profile.cape),p), p.angle+p.secondary*.5, ground)
  return
 _place("head", Body.torso(Body.point(profile.neck),p), p.angle+p.head_angle, ground)
 _place("tail", Body.torso(Body.point(profile.tail),p), p.angle+p.secondary, ground)
 _place("cape", Body.torso(Body.point(profile.cape),p), p.angle+p.secondary*.6, ground)
 for arm in 2:
  var shoulder: Vector2 = Body.point(profile.shoulders[arm])
  var elbow: Vector2 = Body.point(profile.elbows[arm])
  var target: Vector2 = Body.torso(shoulder,p)
  _place("arm%d"%arm,target,p.angle+p.arms[arm],ground)
  _place("forearm%d"%arm,target+(elbow-shoulder).rotated(p.angle+p.arms[arm]),p.angle+p.arms[arm]+p.forearms[arm],ground)
 if active_key.begins_with("nocti-flight"): return
 for leg in p.hips.size():
  for pair in [["hip","upper"],["knee","lower"],["foot","toes"]]:
   var v: Vector4 = p[pair[1]][leg]
   _place(pair[0]+str(leg),Vector2(v.y,v.z),v.x,ground,v.w)
