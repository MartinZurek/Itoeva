extends MultiMeshInstance3D
## Ein MultiMesh mit getrennten Plätzen für Abdrücke und kurzlebigen Staub.
const CAPACITY: = 12
const PRINT_CAPACITY:=8
const LIFE: = 6.0
const DUST_LIFE:=.62
const MIN_DUST_SPEED:=.55
# Keep the shader's half-float instance timestamps precise in long sessions.
const SHADER_PERIOD := 16.0
const SHADER_TICKS := 64.0
const Forest = preload("res://scripts/forest_catalog.gd")
const Catalog = preload("res://scripts/world_catalog.gd")
var clock: = 0.0
var cursor: = 0
var dust_cursor:=PRINT_CAPACITY
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
	multimesh.use_colors = true
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
static func shader_birth(seconds: float) -> float:
	return floorf(fposmod(seconds, SHADER_PERIOD) * SHADER_TICKS) / SHADER_TICKS

func _stamp(point: Vector3, direction: Vector3, dust: bool, side: float, energy: float, tint: Color, sand: bool = false) -> void:
	# Auf Sand ersetzt ein kurzer Staubstoß keinen länger sichtbaren Abdruck.
	var slot := dust_cursor if dust else cursor
	var basis: = Basis(Vector3.RIGHT, deg_to_rad(55)) if dust else Basis(Vector3.UP, atan2(direction.x, direction.z))
	var size := Vector3(.63, 1, .49) if dust else Vector3(.30, 1, .37) if sand else Vector3(.16, 1, .25)
	basis = basis.scaled_local(size)
	multimesh.set_instance_transform(slot, Transform3D(basis, point + Vector3.UP * (.11 if dust else 0.0)))
	var material_kind := 0 if sand else 1
	if dust:
		material_kind = 3 if sand else 2
	multimesh.set_instance_custom_data(slot, Color(shader_birth(clock), material_kind, -direction.x if dust else side, energy))
	multimesh.set_instance_color(slot, tint)
	if deaths[slot] <= clock:
		active += 1
	deaths[slot] = clock + (DUST_LIFE if dust else LIFE)
	if dust:
		dust_cursor = PRINT_CAPACITY + (slot - PRINT_CAPACITY + 1) % (CAPACITY - PRINT_CAPACITY)
	else:
		cursor = (slot + 1) % PRINT_CAPACITY
	emissions += 1
	visible = true
func advance(delta: float, actor: CharacterBody3D, stage: Node3D) -> void :
	clock += delta
	if active > 0:
		material_override.set_shader_parameter("clock", fposmod(clock, SHADER_PERIOD));active = 0
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
	var contact: = false
	var bound = actor.get("bound_pose")
	var bounding: bool=bound is Dictionary and not bound.is_empty()
	if bounding:
		if int(bound.count) != bound_count: bound_count = int(bound.count);contact = true
	else:
		bound_count = -1
		var step: = floori(actor.gait * 2)
		if step != walk_contact: walk_contact = step;contact = true
	if not contact or travelled<.0001 or clock-last_birth<.20:return
	# Wechselnde Stuetzpfoten statt einer einzigen Spur auf derselben Wegseite.
	var paw: float=1.0 if (int(bound.count) if bounding else walk_contact)%2==0 else -1.0
	var heading := direction.normalized()
	var side := Vector3(-heading.z, 0, heading.x) * .09 * paw
	var footprint := at + side - heading * .08
	var surface := surface_kind(stage, footprint)
	var mud := surface == "mud"
	var sand := surface == "sand"
	var dust: bool = (sand or surface == "earth") and actor.actual_speed > MIN_DUST_SPEED
	if not (mud or sand or dust) or not stage.is_walkable(footprint, .16):
		return
	var energy := lerpf(.42, 1.0, smoothstep(MIN_DUST_SPEED, 3.85, actor.actual_speed))
	var tint: Color = stage.actor_scene_color(footprint)
	if mud or sand:
		var shape := 2.0 if actor.species == "wyrmling" else 1.0
		_stamp(footprint, heading, false, shape, energy, tint, sand)
		if mud and bounding:
			var other_paw := at - side - heading * .18
			if muddy(stage, other_paw) and stage.is_walkable(other_paw, .16):
				_stamp(other_paw, heading, false, -shape, energy, stage.actor_scene_color(other_paw))
	if dust:
		_stamp(footprint, heading, true, paw, energy, tint, sand)
	last_birth = clock
	material_override.set_shader_parameter("clock", fposmod(clock, SHADER_PERIOD))
