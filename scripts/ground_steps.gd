extends MultiMeshInstance3D

const CAPACITY: = 12
const LIFE: = 6.0
const Forest = preload("res://scripts/forest_catalog.gd")
const Catalog = preload("res://scripts/world_catalog.gd")
var clock: = 0.0
var cursor: = 0
var last_point: = Vector3.INF
var deaths: Array[float] = []
var active: = 0
var bound_owner: = 0
var bound_count: = -1
var walk_contact: = -1
var emissions: = 0
var last_birth: = -1.0
func _ready() -> void :
	multimesh = MultiMesh.new();multimesh.transform_format = MultiMesh.TRANSFORM_3D;multimesh.use_custom_data = true
	var plane: = PlaneMesh.new();plane.size = Vector2.ONE;multimesh.mesh = plane;multimesh.instance_count = CAPACITY
	for i in CAPACITY:
		deaths.append( - LIFE)
		multimesh.set_instance_transform(i, Transform3D(Basis.IDENTITY.scaled(Vector3.ZERO), Vector3.ZERO))
	var material: = ShaderMaterial.new();material.shader = load("res://shaders/mud_steps.gdshader");material.render_priority = 3;material_override = material
	cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF;visible = false
static func muddy(stage: Node3D, point: Vector3) -> bool:
	if stage.place != "FOREST_TRAIL": return false
	var region: = Forest.region_at(point.x)
	if region.id != "SHADOW_BROOK": return false
	var u: float = (point.x - float(region.origin)) / float(region.width)
	return (u > 0.04 and u < 0.27) or (u > 0.84 and u < 0.97)
static func surface_kind(stage: Node3D, point: Vector3) -> String:
	if muddy(stage, point): return "mud"
	if stage.place == "OUTDOOR":

		if point.x < 22.5:
			if (point.x > 2.1 and point.x < 6.9) or (point.x > 15.6 and point.x < 21.4): return "wood"
			return "sand"
		if point.x < 60.0: return "stone"
		if point.x < 120.0 or point.x > 130.5: return "earth"
		return "stone"
	if stage.place in ["SUN_GLADE", "MIST_WATCH", "MILL_BROOK"]: return "earth"
	if stage.place == "FOREST_TRAIL":
		var region: = Forest.region_at(point.x)
		var u: float = (point.x - float(region.origin)) / float(region.width)
		if region.id == "SHADOW_BROOK" and u > 0.27 and u < 0.84: return "wood"
		return "earth"
	return "stone"
static func dusty(stage: Node3D, point: Vector3) -> bool:
	return surface_kind(stage, point) in ["earth", "sand"]
func _stamp(point: Vector3, direction: Vector3, dust: bool, side: float, energy: float, sand: bool = false) -> void :
	var basis: = Basis(Vector3.RIGHT, deg_to_rad(55)) if dust else Basis(Vector3.UP, atan2(direction.x, direction.z))
	basis = basis.scaled(Vector3(0.48, 1, 0.36) if dust else Vector3(0.16, 1, 0.25))
	multimesh.set_instance_transform(cursor, Transform3D(basis, point + Vector3.UP * (0.075 if dust else 0.0)))
	multimesh.set_instance_custom_data(cursor, Color(clock, 2 if dust else 0 if sand else 1, - direction.normalized().x if dust else side, energy))
	deaths[cursor] = clock + (0.7 if dust else LIFE)
	cursor = (cursor + 1) % CAPACITY;emissions += 1;visible = true
func advance(delta: float, actor: CharacterBody3D, stage: Node3D) -> void :
	clock += delta
	if active > 0:
		material_override.set_shader_parameter("clock", clock);active = 0
		for i in CAPACITY:
			if clock < deaths[i]: active += 1
			elif deaths[i] >= 0:


				multimesh.set_instance_transform(i, Transform3D(Basis.IDENTITY.scaled(Vector3.ZERO), Vector3.ZERO));deaths[i] = -1
		visible = active > 0
	var at: = Vector3(actor.position.x, 0.028, actor.position.z)
	if actor.get_instance_id() != bound_owner:
		bound_owner = actor.get_instance_id();last_point = at;bound_count = -1;walk_contact = floori(actor.gait * 2);return
	var travelled: = at.distance_to(last_point); var direction: = at - last_point;last_point = at
	if travelled > 1.0 or not actor.is_on_floor() or actor.mounted or actor.get("ground_airborne") == true or actor.state != "idle" or actor.species not in ["mosslyn", "wyrmling"]:
		return
	var surface: = surface_kind(stage, at)
	var mud: = surface == "mud"; var sand: = surface == "sand"
	var dust: bool = surface in ["earth", "sand"] and actor.actual_speed > 1.2
	if not mud and not sand and not dust: return
	var contact: = false
	var bound = actor.get("bound_pose")
	if bound is Dictionary and not bound.is_empty():
		if int(bound.count) != bound_count: bound_count = int(bound.count);contact = true
	else:
		var step: = floori(actor.gait * 2)
		if step != walk_contact: walk_contact = step;contact = true
	if not contact or travelled < 0.0001 or clock - last_birth < 0.2: return

	var paw: float = 1.0 if (int(bound.count) if bound is Dictionary and not bound.is_empty() else walk_contact) % 2 == 0 else -1.0
	var side: = Vector3( - direction.z, 0, direction.x).normalized() * 0.065 * paw
	if not stage.is_walkable(at, 0.16): return
	var energy: = clampf(actor.actual_speed / 3.85, 0.45, 1.0)

	if stage.has_method("actor_scene_color"):
		material_override.set_shader_parameter("scene_tint", stage.actor_scene_color(at))
	if mud or sand:
		_stamp(at + side, direction, false, 2.0 if actor.species == "wyrmling" else 1.0, energy, sand)
		if not bound.is_empty() and not sand: _stamp(at - side - direction.normalized() * 0.1, direction, false, -2.0 if actor.species == "wyrmling" else -1.0, energy)
	if dust: _stamp(at - direction.normalized() * 0.08, direction, true, 1 if cursor % 2 == 0 else -1, energy)
	last_birth = clock
	active = 0
	for death in deaths:
		if clock < death: active += 1
	material_override.set_shader_parameter("clock", clock)
