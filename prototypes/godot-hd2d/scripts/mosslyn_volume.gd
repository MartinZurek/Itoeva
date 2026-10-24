extends Node3D

# A1 ist ein ansichtsgebundenes Volumen. Die Kamera darf die Projektion nicht
# umkreisen; Vorder-/Rueckansicht und Sonderaktionen bleiben Originalzeichnungen.
const Model = preload("res://assets/characters/mosslyn-3d/original-rig.glb")
var skeleton: Skeleton3D
var surface: MeshInstance3D
var material: ShaderMaterial
var joints: Dictionary = {}
var motion_weight := 0.0
var feet: Array[Vector2] = []

func _ready() -> void:
 var model := Model.instantiate()
 add_child(model)
 skeleton = _find_skeleton(model)
 surface = _find_surface(model)
 for i in skeleton.get_bone_count(): joints[skeleton.get_bone_name(i)] = i
 material = ShaderMaterial.new()
 material.shader = preload("res://shaders/mosslyn_volume.gdshader")
 material.set_shader_parameter("atlas", surface.mesh.surface_get_material(0).albedo_texture)
 surface.material_override = material
 surface.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
 surface.extra_cull_margin = 1.0

func _find_skeleton(node: Node) -> Skeleton3D:
 if node is Skeleton3D: return node
 for child in node.get_children():
  var found := _find_skeleton(child)
  if found: return found
 return null

func _find_surface(node: Node) -> MeshInstance3D:
 if node is MeshInstance3D: return node
 for child in node.get_children():
  var found := _find_surface(child)
  if found: return found
 return null

func _rest(joint: String) -> Vector2:
 var p := skeleton.get_bone_global_rest(joints[joint]).origin
 return Vector2(p.x, p.y)

func _rotate(joint: String, angle: float) -> void:
 skeleton.set_bone_pose_rotation(joints[joint], Quaternion(Vector3.BACK, angle))

func pose(cycle: float, moving: bool, delta: float) -> void:
 motion_weight = move_toward(motion_weight, 1.0 if moving else 0.0, delta * 5.0)
 var phase := cycle * TAU
 var bob := -.018 * (1.0 + cos(phase * 2.0)) * motion_weight
 skeleton.reset_bone_poses()
 var body := skeleton.get_bone_rest(joints.body).origin
 skeleton.set_bone_pose_position(joints.body, body + Vector3(0, bob, 0))
 _rotate("head", .015 * sin(phase - .4) * motion_weight)
 _rotate("cape", .045 * sin(phase - .9) * motion_weight)
 _rotate("tail", .035 * sin(phase - .7) * motion_weight)
 _rotate("bag", .045 * sin(phase - .5) * motion_weight)
 _rotate("arm", -.17 * sin(phase) * motion_weight)
 feet.clear()
 for leg in 2:
  var prefix := "rear" if leg == 0 else "front"
  var hip := _rest(prefix + "_hip") + Vector2(0, bob)
  var knee := _rest(prefix + "_knee")
  var ankle := _rest(prefix + "_foot")
  var rest_hip := _rest(prefix + "_hip")
  var upper := rest_hip.distance_to(knee)
  var lower := knee.distance_to(ankle)
  var t := fposmod(cycle + leg * .5, 1.0)
  # Lange Bodenkontaktphase, danach geschlossener Rueckholbogen.
  var travel := lerpf(.18, -.18, t / .62) if t < .62 else lerpf(-.18, .18, (t - .62) / .38)
  var lift := 0.0 if t < .62 else .11 * sin(PI * (t - .62) / .38)
  var target := ankle + Vector2(travel, lift) * motion_weight
  var axis := target - hip
  var reach := clampf(axis.length(), absf(upper - lower) + .001, upper + lower - .001)
  axis = axis.normalized()
  target = hip + axis * reach
  var along := (upper * upper - lower * lower + reach * reach) / (2.0 * reach)
  var across := sqrt(maxf(0.0, upper * upper - along * along))
  var bend := -1.0 if (ankle-rest_hip).cross(knee-rest_hip) < 0.0 else 1.0
  var solved := hip + axis * along + Vector2(-axis.y,axis.x) * across * bend
  var a := (solved-hip).angle() - (knee-rest_hip).angle()
  var b := (target-solved).angle() - (ankle-knee).angle()
  _rotate(prefix+"_hip", a)
  _rotate(prefix+"_knee", b-a)
  _rotate(prefix+"_foot", -b)
  feet.append(target)

func align_to_camera(camera: Camera3D, pixel_size: float, flipped: bool) -> void:
 var facing := camera.global_basis.orthonormalized() if camera else Basis.IDENTITY
 var factor := pixel_size * 100.0
 global_basis = facing.scaled(Vector3(-factor if flipped else factor, factor, factor))

func screen_rect(camera: Camera3D) -> Rect2:
 var bounds := surface.get_aabb().grow(.25)
 var minimum := Vector2(INF,INF)
 var maximum := Vector2(-INF,-INF)
 for i in 8:
  var point := camera.unproject_position(surface.to_global(bounds.get_endpoint(i)))
  minimum = minimum.min(point)
  maximum = maximum.max(point)
 return Rect2(minimum,maximum-minimum)
