extends "res://scripts/character_actor.gd"

const BoundFlow = preload("res://scripts/bound_flow.gd")
var bound_flow = BoundFlow.new()
const LandingPlan = preload("res://scripts/landing_plan.gd")
var landing_plan
var last_landing_error: = 0.0
var landing_samples: = 0
var retarget_landing: = false
var bound_pose: Dictionary = {}
var bound_footing_count: = -1
var bound_turn_grace: = 0.0
var wing_regions: Array = []
const GroundMotion = preload("res://scripts/ground_motion.gd")
var ground_motion_profile: Dictionary = {}
var ground_motion_lift: = 0.0
var ground_airborne: = false
const FLIGHT_CYCLES: = 1.8
const PAINTED_GAIT_RATE: = 0.25
const FullBody = preload("res://scripts/full_body_gait.gd")
static var body_rigs: Dictionary = {}
static var body_masks: Dictionary = {}
var skin_mesh: MeshInstance3D
var body_pose: Dictionary = {}
var body_weight: = 0.0
const StepGait = preload("res://scripts/step_gait.gd")
static var step_rigs: Dictionary = {}
static var step_masks: Dictionary = {}

var cell_size: = Vector2i(181, 181)
var world_texture: Texture2D
var run_texture: Texture2D
var walk_texture: Texture2D
var world_bounds: Array[Rect2i] = []
var run_bounds: Array[Rect2i] = []
var walk_bounds: Array[Rect2i] = []
var reference_height: = 1.0
var run_reference_height: = 1.0
var walk_reference_height: = 1.0
var run_sheet: = false
var walk_sheet: = false
static var pose_masks: Dictionary = {}
var mask_textures: Dictionary = {}
var displayed_bounds: = Rect2i()
var registered_gait: = true
var painted_cycle_active: = false
var painted_cycle_mode: = "walk"
var painted_cycles: Dictionary = {}
var painted_cycle_textures: Dictionary = {}
var painted_cycle_masks: Dictionary = {}
var painted_x_offset: = 0.0
const AuthoredLocomotion = preload("res://scripts/authored_locomotion.gd")
const AuthoredTransition = preload("res://scripts/authored_motion_transition.gd")
var authored_transition = AuthoredTransition.new()
var authored_locomotion = AuthoredLocomotion.new()
var authored_m4: = false
var authored_lift: = 0.0
var stop_pose_clock: = 1.0
var pose_was_moving: = false
var home_rest_phase: = -1
var home_rest_flight: = ""
var home_rest_flight_phase: = 0

func _load_painted_cycle(mode: String, cycle: Dictionary) -> void :
	painted_cycles[mode] = cycle
	painted_cycle_textures[mode] = art_texture(cycle.asset)
	var raw: = Marshalls.base64_to_raw(cycle.mask_deflate).decompress(int(cycle.width) * int(cycle.height), FileAccess.COMPRESSION_DEFLATE)
	painted_cycle_masks[mode] = ImageTexture.create_from_image(Image.create_from_data(int(cycle.width), int(cycle.height), false, Image.FORMAT_R8, raw))

func begin_home_rest() -> bool:
	if species not in ["mosslyn", "starlet", "wyrmling", "nocti"]:
		return false
	if not painted_cycles.has("home_rest"):
		var cycle: Dictionary = JSON.parse_string(FileAccess.get_file_as_string("res://data/" + species + "_home_rest.json"))
		_load_painted_cycle("home_rest", cycle)
		if species == "nocti" and not painted_cycles.has("land"):
			var flight: Dictionary = JSON.parse_string(FileAccess.get_file_as_string("res://data/nocti_authored_cycles.json"))
			_load_painted_cycle("land", flight.land)
			_load_painted_cycle("takeoff", flight.takeoff)
	bound_flow.reset();bound_pose = {};authored_transition.reset();authored_lift = 0.0
	home_rest_phase = 0
	return true

func _physics_process(delta: float) -> void :
	if home_rest_phase < 0:
		super._physics_process(delta);return
	if not enabled: return
	clock += delta
	stamina.advance(delta, 0.0, false, true, true)
	_update_frame(false);_update_shadow()

func rest_clearance() -> float:
	return (cell_size.y * 0.5 + sprite.offset.y - displayed_bounds.end.y) * sprite.pixel_size + sprite.position.y

func request_action(action: String) -> bool:
	if home_rest_phase >= 0 and enabled:
		get_parent().personal_rest.leave(get_parent());return true
	return super.request_action(action)

func _pose_mask() -> Dictionary:
	if pose_masks.is_empty(): pose_masks = JSON.parse_string(FileAccess.get_file_as_string("res://data/character_pose_masks.json"))
	var path: = original_art_path(sprite.texture)
	var data: Dictionary = pose_masks[path]
	if not metrics.has("pose_masks"): metrics["pose_masks"] = {}
	mask_textures = metrics.pose_masks
	if not mask_textures.has(path):
		var raw: = Marshalls.base64_to_raw(data.deflate).decompress(int(data.width) * int(data.height), FileAccess.COMPRESSION_DEFLATE)
		var mask: = Image.create_from_data(int(data.width), int(data.height), false, Image.FORMAT_R8, raw)
		mask_textures[path] = ImageTexture.create_from_image(mask)
	return {"texture": mask_textures[path], "id": float(data.ids[sprite.frame]) / 255.0}

func _ready() -> void :
	if species == "mosslyn":
		painted_cycles = JSON.parse_string(FileAccess.get_file_as_string("res://data/mosslyn_painted_cycles.json"))

		if OS.get_environment("MOTION_DRAFT") == "1" or OS.has_feature("itoeva_motion_m3"):
			var drafts: Dictionary = JSON.parse_string(FileAccess.get_file_as_string("res://data/mosslyn_authored_cycles.json"))
			for mode in drafts: painted_cycles[mode] = drafts[mode]
	if OS.get_environment("MOTION_M4") == "1" or OS.has_feature("itoeva_motion_m4"):
		var m4_path: = "res://data/mosslyn_m4_cycles.json" if species == "mosslyn" else "res://data/" + species + "_authored_cycles.json"
		if FileAccess.file_exists(m4_path):
			authored_m4 = true
			painted_cycles = JSON.parse_string(FileAccess.get_file_as_string(m4_path))
	if player_controlled and species in ["mosslyn", "wyrmling"]:
		ground_motion_profile = GroundMotion.profile(species)
		landing_plan = LandingPlan.new()
		if authored_m4 and painted_cycles.has("run"):
			painted_cycles.run["phase_weights"] = ground_motion_profile.run_weights
	if not painted_cycles.is_empty():
		for mode in painted_cycles:
			var cycle: Dictionary = painted_cycles[mode]
			_load_painted_cycle(mode, cycle)
	if body_rigs.is_empty(): body_rigs = JSON.parse_string(FileAccess.get_file_as_string("res://data/body_rigs.json"))
	if step_rigs.is_empty(): step_rigs = JSON.parse_string(FileAccess.get_file_as_string("res://data/step_rigs.json"))
	if pose_masks.is_empty(): pose_masks = JSON.parse_string(FileAccess.get_file_as_string("res://data/character_pose_masks.json"))
	collision_layer = 2;collision_mask = 1
	collider = CollisionShape3D.new()
	var capsule: = CapsuleShape3D.new();capsule.radius = 0.25;capsule.height = 1.4
	collider.shape = capsule;collider.position.y = capsule.height * 0.5;add_child(collider)
	sprite = Sprite3D.new();sprite.name = "OriginalPainted" + species.capitalize()
	sprite.texture = art_texture(Catalog.data().characters[species].asset)
	world_texture = sprite.texture
	cell_size = Vector2i(sprite.texture.get_width() / 8, sprite.texture.get_height() / 6)
	if metrics.has(species): frame_bounds.assign(metrics[species].bounds)
	else:
		var bounds: Array = JSON.parse_string(FileAccess.get_file_as_string("res://data/compact_bounds.json"))[species]
		for b in bounds: frame_bounds.append(Rect2i(b[0], b[1], b[2], b[3]))
		metrics[species] = {"bounds": frame_bounds.duplicate(), "scales": {}}
	world_bounds.assign(frame_bounds)
	reference_height = float(world_bounds[0].size.y)
	var definition: Dictionary = Catalog.data().characters[species]
	if definition.has("walk_asset"):
		walk_texture = art_texture(definition.walk_asset)
		var walk_data: Dictionary = JSON.parse_string(FileAccess.get_file_as_string("res://data/character_walks.json"))[species]
		for b in walk_data.bounds: walk_bounds.append(Rect2i(b[0], b[1], b[2], b[3]))
		walk_reference_height = float(walk_data.reference_height)
	if definition.has("run_asset"):
		run_texture = art_texture(definition.run_asset)
		var run_data: Dictionary = JSON.parse_string(FileAccess.get_file_as_string("res://data/character_runs.json"))[species]
		for b in run_data.bounds: run_bounds.append(Rect2i(b[0], b[1], b[2], b[3]))
		run_reference_height = float(run_data.reference_height)
	sprite.hframes = 8;sprite.vframes = 6
	sprite.billboard = BaseMaterial3D.BILLBOARD_ENABLED
	sprite.alpha_cut = SpriteBase3D.ALPHA_CUT_DISCARD;sprite.alpha_scissor_threshold = 0.12
	sprite.texture_filter = BaseMaterial3D.TEXTURE_FILTER_NEAREST;sprite.shaded = false
	sprite.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	sprite.extra_cull_margin = 2.0
	painted_material = ShaderMaterial.new();painted_material.render_priority = 10
	painted_material.shader = load("res://shaders/painted_character.gdshader")
	painted_material.set_shader_parameter("atlas", pixel_art(sprite.texture));sprite.material_override = painted_material
	sprite.offset = Vector2(0, cell_size.y * 0.5 - 2);add_child(sprite)
	skin_mesh = MeshInstance3D.new();skin_mesh.name = "ArticulatedPaintedBody"
	skin_mesh.material_override = painted_material
	skin_mesh.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	skin_mesh.extra_cull_margin = 0.0;skin_mesh.visible = false;sprite.add_child(skin_mesh)
	painted_material.set_shader_parameter("warm_wyrmling", species == "wyrmling")
	if species == "wyrmling" and player_controlled:
		var regions: Dictionary = JSON.parse_string(FileAccess.get_file_as_string("res://data/wyrmling_wings.json"))
		for region in regions.frames:
			var points: = PackedVector2Array()
			for point in region.polygon: points.append(Vector2(point[0], point[1]))
			var count: = points.size()
			while points.size() < 12: points.append(points[0])
			wing_regions.append({"pivot": Vector2(region.pivot[0], region.pivot[1]), "points": points, "count": count})
	_setup_aura()
	shadow = MeshInstance3D.new(); var plane: = PlaneMesh.new();plane.size = Vector2(2.2, 1.35);shadow.mesh = plane;shadow.visible = false
	var material: = ShaderMaterial.new();material.shader = load("res://shaders/contact_shadow.gdshader");material.render_priority = 2
	material.set_shader_parameter("grounded", true)
	shadow.material_override = material;shadow.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	get_parent().add_child.call_deferred(shadow)
	set_world_scale(base_pixel_size, jump_height, body_pixel_size);_update_frame(false)

func _update_sprite_scale() -> void :
	if painted_cycle_active:
		_update_painted_cycle_geometry()
		return
	painted_x_offset = 0.0
	var height: = run_reference_height if run_sheet else walk_reference_height if walk_sheet else reference_height
	sprite.pixel_size = base_pixel_size * 109.0 * Catalog.relative_height(species) / maxf(1, height)
	sprite.position = Vector3.UP * hover

	var bounds: = frame_bounds[sprite.frame]
	var ground: = bounds.end.y
	painted_material.set_shader_parameter("gait_mode", 0)
	if registered_gait and walk_sheet and step_rigs.has(species):
		var row: = sprite.frame / 8
		var rig: Dictionary = step_rigs[species]
		var profile: Dictionary = rig.rows[row]
		var reference: = frame_bounds[row * 8]
		var amplitude: float = float(rig.half_stride)


		bounds = reference.grow(int(ceil(amplitude)) + 8)
		ground = int(profile.ground)
		var key: = species
		if not step_masks.has(key):
			var raw: = Marshalls.base64_to_raw(rig.mask).decompress(256 * 768 * 4, FileAccess.COMPRESSION_DEFLATE)
			step_masks[key] = ImageTexture.create_from_image(Image.create_from_data(256, 768, false, Image.FORMAT_RGBA8, raw))
		var texture_size: = sprite.texture.get_size()
		painted_material.set_shader_parameter("gait_mode", 3)
		painted_material.set_shader_parameter("gait_reference", Vector2(0, row * 256) / texture_size)
		painted_material.set_shader_parameter("gait_reference_id", float(pose_masks[original_art_path(sprite.texture)].ids[row * 8]) / 255.0)
		painted_material.set_shader_parameter("step_mask", step_masks[key])
		painted_material.set_shader_parameter("step_row", row)
		var depth: = 0.0 if row == 0 else 0.573576 if row == 1 else - 0.573576
		var lowest_ray: float = - INF
		if species == "starlet": painted_material.set_shader_parameter("gait_mode", 5)
		painted_material.set_shader_parameter("step_leg_count", profile.feet.size())
		for leg in profile.feet.size():
			var foot: = Vector2(profile.feet[leg][0], profile.feet[leg][1])
			var hip: = Vector2(profile.hips[leg][0], profile.hips[leg][1])
			var phase: float = lerpf([0.0, 0.5, 0.75, 0.25][leg], [0.0, 0.5, 0.5, 0.0][leg], run_blend) if profile.feet.size() == 4 else leg * 0.5
			var step: = StepGait.sample(gait + phase, amplitude, run_blend, depth)
			var offset: Vector2 = step.offset + Vector2(hip.x - foot.x, ground - foot.y)
			if species == "starlet":


				var ray: = foot - hip
				var target: = ray + Vector2(step.travel * (1.0 if row == 0 else 0.55), step.offset.y)
				var turn: = wrapf(target.angle() - ray.angle(), - PI, PI)
				var angle: = turn / sqrt(1.0 + pow(turn / 0.55, 2.0))
				lowest_ray = maxf(lowest_ray, hip.y + ray.rotated(angle).y)
				offset = Vector2(angle, 0)
			painted_material.set_shader_parameter("step_hip_" + str(leg), hip)
			painted_material.set_shader_parameter("step_foot_" + str(leg), foot)
			painted_material.set_shader_parameter("step_offset_" + str(leg), offset)
		if species == "starlet": sprite.position.y = hover + (lowest_ray - ground) * sprite.pixel_size
	elif registered_gait and run_sheet and species == "nocti":
		var row: = sprite.frame / 8
		var reference: = frame_bounds[row * 8]
		var flight: Dictionary = step_rigs.nocti.flight
		bounds = reference.grow(32)
		ground = reference.end.y
		if not step_masks.has("flight"):
			var raw: = Marshalls.base64_to_raw(flight.mask).decompress(256 * 768 * 4, FileAccess.COMPRESSION_DEFLATE)
			step_masks.flight = ImageTexture.create_from_image(Image.create_from_data(256, 768, false, Image.FORMAT_RGBA8, raw))
		painted_material.set_shader_parameter("gait_mode", 4)
		painted_material.set_shader_parameter("step_mask", step_masks.flight)
		painted_material.set_shader_parameter("step_row", row)
		painted_material.set_shader_parameter("gait_reference", Vector2(0, row * 256) / sprite.texture.get_size())
		painted_material.set_shader_parameter("gait_reference_id", float(pose_masks[original_art_path(sprite.texture)].ids[row * 8]) / 255.0)
		painted_material.set_shader_parameter("step_leg_count", 2)
		for wing in 2:
			var point: Array = flight.pivots[row][wing]
			painted_material.set_shader_parameter("step_hip_" + str(wing), Vector2(point[0], point[1]))
			painted_material.set_shader_parameter("step_offset_" + str(wing), Vector2(sin(clock * TAU * FLIGHT_CYCLES) * 0.2 * (1.0 if wing == 0 else -1.0), 0))
	_update_body_skin(ground)
	displayed_bounds = bounds
	sprite.offset.y = cell_size.y * 0.5 - 2.0 if mounted else float(ground) - cell_size.y * 0.5
	var origin: = Vector2(sprite.frame % 8, sprite.frame / 8) * Vector2(cell_size) + Vector2(bounds.position)
	var texture_size: = sprite.texture.get_size()
	painted_material.set_shader_parameter("frame_clip", Vector4(origin.x / texture_size.x, origin.y / texture_size.y, bounds.size.x / texture_size.x, bounds.size.y / texture_size.y))
	var cell_origin: = Vector2(sprite.frame % 8, sprite.frame / 8) * Vector2(cell_size)
	var ratio: = Vector2(bounds.size) / Vector2(cell_size)
	var shift: = Vector2((bounds.get_center().x - cell_size.x * 0.5) * (-1.0 if sprite.flip_h else 1.0), (1.0 - ratio.y) * sprite.offset.y + cell_size.y * 0.5 - bounds.get_center().y) * sprite.pixel_size
	painted_material.set_shader_parameter("has_pose_region", true)
	painted_material.set_shader_parameter("pose_cell", Vector4(cell_origin.x / texture_size.x, cell_origin.y / texture_size.y, cell_size.x / texture_size.x, cell_size.y / texture_size.y))
	painted_material.set_shader_parameter("pose_region", painted_material.get_shader_parameter("frame_clip"))
	painted_material.set_shader_parameter("pose_geometry", Vector4(ratio.x, ratio.y, shift.x, shift.y))
	var mask: = _pose_mask()
	painted_material.set_shader_parameter("pose_mask", mask.texture)
	painted_material.set_shader_parameter("pose_id", mask.id)
	_apply_ground_motion()
	_update_occlusion()


func _update_body_skin(ground: int) -> void :
	var active: bool = registered_gait and body_rigs.has(species) and (walk_sheet or run_sheet and species == "nocti")
	painted_material.set_shader_parameter("rig_active", active)
	if not is_instance_valid(skin_mesh): return
	skin_mesh.visible = active
	body_pose = {}
	if not active: return
	var row: = sprite.frame / 8
	var flight: bool = run_sheet and species == "nocti"
	var key: = species + "-flight" if flight else species
	var rig: Dictionary = body_rigs[key]
	var profile: Dictionary = rig.rows[row]
	if not body_masks.has(key):
		var raw: = Marshalls.base64_to_raw(rig.mask).decompress(256 * 768 * 4, FileAccess.COMPRESSION_DEFLATE)
		body_masks[key] = ImageTexture.create_from_image(Image.create_from_data(256, 768, false, Image.FORMAT_RGBA8, raw))
	var reference: = frame_bounds[row * 8]
	skin_mesh.mesh = FullBody.surface(reference.grow(2))



	var extent: = maxf(absf(reference.position.x - 128.0), absf(reference.end.x - 128.0))
	extent = maxf(extent, maxf(absf(ground - reference.position.y), absf(ground - reference.end.y)))
	var reach: float = (extent + 64.0) * sprite.pixel_size
	skin_mesh.custom_aabb = AABB(Vector3.ONE * - reach, Vector3.ONE * (2.0 * reach))
	var cycle: float = clock * FLIGHT_CYCLES if flight else float(bound_pose.phase) if not bound_pose.is_empty() else gait
	body_pose = FullBody.sample(species, row, cycle, run_blend, step_rigs[species].rows[row], profile, flight, player_controlled and species in ["mosslyn", "wyrmling"], bound_pose)
	body_pose.offset *= body_weight
	body_pose.angle *= body_weight
	for field in ["head_angle", "ear_angle", "secondary", "arms", "forearms", "squash"]: body_pose[field] *= body_weight
	for leg in body_pose.hips.size():
		for pair in [["upper", "hips"], ["lower", "knees"], ["toes", "feet"]]:
			var pose: Vector4 = body_pose[pair[0]][leg]
			var rest: Vector2 = body_pose[pair[1]][leg]
			body_pose[pair[0]][leg] = Vector4(pose.x * body_weight, lerpf(rest.x, pose.y, body_weight), lerpf(rest.y, pose.z, body_weight), lerpf(1.0, pose.w, body_weight))
	sprite.position.y = hover + float(body_pose.lift) * sprite.pixel_size * body_weight
	painted_material.set_shader_parameter("rig_mask", body_masks[key])
	painted_material.set_shader_parameter("rig_pixel_size", sprite.pixel_size)
	painted_material.set_shader_parameter("rig_ground_offset", float(ground) - 128.0)
	painted_material.set_shader_parameter("rig_flip", -1.0 if sprite.flip_h else 1.0)
	painted_material.set_shader_parameter("rig_star", species == "starlet")
	painted_material.set_shader_parameter("rig_root", body_pose.root)
	painted_material.set_shader_parameter("rig_offset", body_pose.offset)
	painted_material.set_shader_parameter("rig_angle", body_pose.angle)
	painted_material.set_shader_parameter("rig_neck", FullBody.point(profile.neck))
	painted_material.set_shader_parameter("rig_head_angle", body_pose.head_angle)
	painted_material.set_shader_parameter("rig_ear_angle", body_pose.ear_angle)
	painted_material.set_shader_parameter("rig_squash", body_pose.squash)
	painted_material.set_shader_parameter("rig_tail", FullBody.point(profile.tail))
	painted_material.set_shader_parameter("rig_cape", FullBody.point(profile.cape))
	painted_material.set_shader_parameter("rig_secondary", body_pose.secondary)
	painted_material.set_shader_parameter("rig_arms", body_pose.arms)
	painted_material.set_shader_parameter("rig_forearms", body_pose.forearms)
	painted_material.set_shader_parameter("rig_elbows", PackedVector2Array([FullBody.point(profile.elbows[0]), FullBody.point(profile.elbows[1])]))
	painted_material.set_shader_parameter("rig_shoulders", PackedVector2Array([FullBody.point(profile.shoulders[0]), FullBody.point(profile.shoulders[1])]))
	painted_material.set_shader_parameter("step_leg_count", 0 if flight or species == "starlet" else body_pose.feet.size())
	if not flight and species != "starlet":
		for pair in [["rig_hips", "hips"], ["rig_knees", "knees"], ["rig_feet", "feet"], ["rig_upper", "upper"], ["rig_lower", "lower"], ["rig_toes", "toes"]]:
			var values = body_pose[pair[1]].duplicate()
			while values.size() < 4: values.append(Vector2.ZERO if pair[1] in ["hips", "knees", "feet"] else Vector4.ZERO)
			painted_material.set_shader_parameter(pair[0], values)

func _update_shadow() -> void :
	var physical_hover: = hover
	hover += ground_motion_lift


	if authored_m4 and painted_cycle_active:
		hover += authored_lift * sprite.pixel_size if species == "nocti" else maxf(0.0, float(painted_cycles[painted_cycle_mode].ground) - float(frame_bounds[sprite.frame].end.y)) * sprite.pixel_size
	elif species == "starlet" and not body_pose.is_empty(): hover = sprite.position.y
	super._update_shadow()
	hover = physical_hover

func rest_frame() -> int:
	return 2 if facing.y < 0 and absf(facing.y) > absf(facing.x) else 1 if absf(facing.y) > absf(facing.x) else 0

func light_center() -> Vector3:
	var bounds: = frame_bounds[sprite.frame / 8 * 8] if not painted_cycle_active and registered_gait and (walk_sheet or run_sheet) and step_rigs.has(species) else frame_bounds[sprite.frame]
	var camera: = get_viewport().get_camera_3d()
	var up: = camera.global_basis.y if camera else Vector3.UP
	var right: = camera.global_basis.x if camera else Vector3.RIGHT
	var center: = Vector2(bounds.get_center())
	var x: float = (center.x - cell_size.x * 0.5) * sprite.pixel_size * (-1.0 if sprite.flip_h else 1.0)
	x += painted_x_offset
	return global_position + sprite.position + right * x + up * (cell_size.y * 0.5 + sprite.offset.y - center.y) * sprite.pixel_size

func _update_frame(moving: bool) -> void :
	if home_rest_phase >= 0:
		if home_rest_flight.is_empty():
			_use_painted_cycle("home_rest", home_rest_phase)
		else:
			_use_painted_cycle(home_rest_flight, home_rest_flight_phase)


			sprite.position.y -= rest_clearance()
		return
	if moving: stop_pose_clock = 0.0
	elif pose_was_moving: stop_pose_clock = 0.0
	else: stop_pose_clock += get_physics_process_delta_time()
	pose_was_moving = moving
	if painted_cycle_active and not moving: body_weight = 0.0
	body_weight = move_toward(body_weight, 1.0 if moving else 0.0, get_physics_process_delta_time() * 8.0)
	var depth: = absf(facing.y) > absf(facing.x)
	if not ground_motion_profile.is_empty():
		var eligible: bool = state == "idle" and jump_clock < 0 and not mounted
		if eligible:

			if authored_m4 and species == "mosslyn" and not depth and not bound_flow.active and moving and running and run_blend >= 0.5 and authored_transition.settled != "run":
				var entering: Dictionary = authored_transition.advance(get_physics_process_delta_time(), true, true)
				if entering.mode == "lower":
					bound_pose = {};_use_painted_cycle("lower", int(entering.phase));return
			var before: int = bound_flow.count
			bound_turn_grace = 0.34 if moving and running else maxf(0, bound_turn_grace - get_physics_process_delta_time())
			var continuing: bool = bound_flow.active and ( not running or run_blend < 0.5) and bound_turn_grace > 0 and InputMap.has_action("move_left") and _desired_input().length() > 0.7
			bound_pose = bound_flow.advance(get_physics_process_delta_time(), moving and running and run_blend >= 0.5 or continuing, moving, actual_speed, Vector2(velocity.x, velocity.z), ground_motion_profile)
			if not bound_pose.is_empty() and (bound_footing_count < 0 or bound_flow.count != before or retarget_landing):
				var new_launch: bool = bound_footing_count < 0 or bound_flow.count != before
				_update_bound_footing( not new_launch)
				if new_launch: bound_flow._choose(ground_motion_profile)
				bound_footing_count = bound_flow.count
			if bound_pose.is_empty(): bound_footing_count = -1
		else:
			bound_flow.reset();bound_pose = {};bound_footing_count = -1
		if not bound_pose.is_empty() and not depth and authored_m4:
			authored_transition.reset("run");authored_locomotion.reset()
			_use_painted_cycle("run", int(bound_pose.frame))
			return
	if authored_m4 and species != "mosslyn":
		if not depth and state == "idle" and jump_clock < 0:
			var next: Dictionary = authored_locomotion.advance(get_physics_process_delta_time(), moving, running and run_blend >= 0.5, actual_speed, _stride_length(), painted_cycles, species == "nocti")
			authored_lift = float(next.lift)
			_use_painted_cycle(next.mode, int(next.phase))
			return
		authored_locomotion.reset();authored_lift = 0.0


	if painted_cycles.has("lower") and painted_cycles.has("rise"):
		if not depth and state == "idle" and jump_clock < 0:
			var run_data: Dictionary = painted_cycles.run
			var exit_contact: = _painted_phase(run_data, gait) == int(run_data.get("entry_phase", 0))
			var next: Dictionary = authored_transition.advance(get_physics_process_delta_time(), running and run_blend >= 0.5, moving, exit_contact)
			if next.mode != "":
				if next.entered:
					var entry: Dictionary = painted_cycles[next.mode]
					gait = floor(gait) + _painted_phase_start(entry, int(entry.get("entry_phase", 0)))
				_use_painted_cycle(next.mode, int(next.phase))
				return
		else:


			authored_transition.reset("run" if running and run_blend >= 0.5 else "walk")


	if authored_m4 and species == "mosslyn" and not moving and not depth and state == "idle" and jump_clock < 0 and not mounted and stop_pose_clock < 0.12 and not bound_flow.active:
		_use_painted_cycle("walk", 0)
		return
	if authored_m4 and painted_cycles.has("rest") and not moving and not depth and state == "idle" and jump_clock < 0:
		_use_painted_cycle("rest", 0)
		return


	var motion_cycle: String = "run" if running and run_blend >= 0.5 else "walk"

	if painted_cycles.has(motion_cycle) and moving and not depth and state == "idle" and jump_clock < 0:
		_use_painted_cycle(motion_cycle)
		return
	var flight: bool = species == "nocti" and hover > 0.06 and state == "idle" and jump_clock < 0
	var use_run: bool = species == "nocti" and run_texture != null and state == "idle" and jump_clock < 0 and ((moving and running and run_blend >= 0.5) or flight)
	var use_walk: bool = walk_texture != null and (moving or body_rigs.has(species) and body_weight > 0.001) and state == "idle" and jump_clock < 0 and not use_run
	_use_motion_sheet(use_run, use_walk)
	sprite.flip_h = not depth and facing.x < 0
	if use_run:
		sprite.frame = run_frame(gait, clock, facing)
	elif use_walk:
		sprite.frame = walk_frame(gait, facing)
	elif jump_clock >= 0:
		var duration: = 2 * sqrt(2 * jump_height / GRAVITY)
		var phase: = clampi(int(jump_clock / maxf(0.1, duration) * 6), 0, 5)
		sprite.frame = [11, 12, 13, 13, 14, 0][phase] if not depth else (16 if facing.y > 0 else 20) + phase % 4
	elif state == "roll": sprite.frame = [26, 27, 28, 28, 29][clampi(int(state_clock / ROLL_SECONDS * 5), 0, 4)]
	elif state == "sit": sprite.frame = 15 if not depth else 24 if facing.y > 0 else 25
	elif moving:
		var phase: = int(gait * 4) % 4
		sprite.frame = (16 if facing.y > 0 else 20) + phase if depth else 7 + phase if running and run_blend >= 0.5 else 3 + phase
	elif recovery_clock < 0.16: sprite.frame = 14 if not depth else rest_frame()
	else: sprite.frame = rest_frame()
	_update_sprite_scale()

func _use_motion_sheet(sprint: bool, walk: bool) -> void :
	if sprint == run_sheet and walk == walk_sheet and not painted_cycle_active: return
	painted_cycle_active = false
	run_sheet = sprint;walk_sheet = walk
	sprite.frame = 0
	sprite.texture = run_texture if sprint else walk_texture if walk else world_texture
	sprite.hframes = 8;sprite.vframes = 3 if sprint or walk else 6
	cell_size = Vector2i(sprite.texture.get_width() / 8, sprite.texture.get_height() / sprite.vframes)
	frame_bounds.assign(run_bounds if sprint else walk_bounds if walk else world_bounds)
	painted_material.set_shader_parameter("atlas", pixel_art(sprite.texture))

func _use_painted_cycle(mode: String, authored_frame: int = -1) -> void :
	var data: Dictionary = painted_cycles[mode]
	if not painted_cycle_active or painted_cycle_mode != mode:
		sprite.frame = 0
		sprite.texture = painted_cycle_textures[mode]
		sprite.hframes = int(data.columns);sprite.vframes = int(data.rows)
		cell_size = Vector2i(int(data.cell), int(data.cell))
		frame_bounds.clear()
		for b in data.bounds: frame_bounds.append(Rect2i(b[0], b[1], b[2], b[3]))
		painted_material.set_shader_parameter("atlas", pixel_art(sprite.texture))
	painted_cycle_active = true;painted_cycle_mode = mode
	walk_sheet = mode == "walk";run_sheet = mode == "run"


	var rate: = 1.0 if data.get("fixed_geometry", false) else PAINTED_GAIT_RATE
	sprite.frame = authored_frame if authored_frame >= 0 else _painted_phase(data, gait * rate)
	sprite.flip_h = facing.x < 0
	_update_painted_cycle_geometry()

func _painted_phase(data: Dictionary, cycle: float) -> int:
	if not data.has("phase_weights"):
		return posmod(int(floor(cycle * int(data.frames))), int(data.frames))
	var weights: Array = data.phase_weights
	var total: = 0.0
	for weight in weights: total += float(weight)
	var distance: = fposmod(cycle, 1.0) * total
	var end: = 0.0
	for phase in weights.size():
		end += float(weights[phase])
		if distance < end: return phase
	return weights.size() - 1

func _painted_phase_start(data: Dictionary, phase: int) -> float:
	if not data.has("phase_weights"): return float(phase) / float(data.frames)
	var before: = 0.0
	var total: = 0.0
	for i in data.phase_weights.size():
		var weight: = float(data.phase_weights[i])
		total += weight
		if i < phase: before += weight
	return before / total

func _update_painted_cycle_geometry() -> void :
	var data: Dictionary = painted_cycles[painted_cycle_mode]




	if data.get("fixed_geometry", false):


		sprite.pixel_size = base_pixel_size * 109.0 * Catalog.relative_height(species) / float(data.reference_height)
		painted_x_offset = 0.0
	else:
		var registration: Dictionary = data.size_registration
		var frame_registration: Dictionary = registration.frames[sprite.frame]
		var head_span: float = float(frame_registration.span)
		sprite.pixel_size = base_pixel_size * 109.0 * Catalog.relative_height(species) * float(registration.reference_span) / float(registration.reference_body_height) / head_span


		var region: Array = frame_registration.region
		var center_x: float = float(region[0]) + float(region[2]) * 0.5
		var target_center: float = base_pixel_size * 109.0 * Catalog.relative_height(species) * float(registration.reference_center_offset) / float(registration.reference_body_height)
		painted_x_offset = (target_center - (center_x - cell_size.x * 0.5) * sprite.pixel_size) * (-1.0 if sprite.flip_h else 1.0)
	sprite.position = Vector3.UP * hover
	displayed_bounds = frame_bounds[sprite.frame]
	if species == "wyrmling" and not bound_pose.is_empty() and painted_cycle_mode == "run": displayed_bounds = displayed_bounds.grow(36)
	var ground: = float(data.ground) if data.get("fixed_geometry", false) else float(displayed_bounds.end.y)
	sprite.offset = Vector2(0, ground - cell_size.y * 0.5)
	skin_mesh.visible = false;body_pose = {}
	painted_material.set_shader_parameter("rig_active", false)
	painted_material.set_shader_parameter("gait_mode", 0)


	var cell_origin: = Vector2(sprite.frame % sprite.hframes, sprite.frame / sprite.hframes) * Vector2(cell_size)
	var origin: = cell_origin + Vector2(displayed_bounds.position)
	var dimensions: = sprite.texture.get_size()
	var ratio: = Vector2(displayed_bounds.size) / Vector2(cell_size)
	var shift: = Vector2((displayed_bounds.get_center().x - cell_size.x * 0.5) * (-1.0 if sprite.flip_h else 1.0), (1.0 - ratio.y) * sprite.offset.y + cell_size.y * 0.5 - displayed_bounds.get_center().y) * sprite.pixel_size
	shift.x += painted_x_offset
	painted_material.set_shader_parameter("frame_clip", Vector4(origin.x / dimensions.x, origin.y / dimensions.y, displayed_bounds.size.x / dimensions.x, displayed_bounds.size.y / dimensions.y))
	painted_material.set_shader_parameter("has_pose_region", true)
	painted_material.set_shader_parameter("pose_cell", Vector4(cell_origin.x / dimensions.x, cell_origin.y / dimensions.y, cell_size.x / dimensions.x, cell_size.y / dimensions.y))
	painted_material.set_shader_parameter("pose_region", painted_material.get_shader_parameter("frame_clip"))
	painted_material.set_shader_parameter("pose_geometry", Vector4(ratio.x, ratio.y, shift.x, shift.y))
	painted_material.set_shader_parameter("pose_mask", painted_cycle_masks[painted_cycle_mode])
	painted_material.set_shader_parameter("pose_id", float(sprite.frame + 1) / 255.0)
	_apply_ground_motion()
	_update_occlusion()

func walk_frame(stride: float, direction: Vector2) -> int:
	var row: = 0 if absf(direction.x) >= absf(direction.y) else 1 if direction.y > 0 else 2
	return row * 8 + posmod(int(stride * 8), 8)

func _stride_length() -> float:
	if player_controlled and species in ["mosslyn", "wyrmling"] and state == "idle":
		return float(ground_motion_profile.run_stride if running and run_blend >= 0.5 else ground_motion_profile.walk_stride)
	var mode: = "run" if running and run_blend >= 0.5 else "walk"
	if painted_cycles.has(mode) and painted_cycles[mode].get("fixed_geometry", false):
		var data: Dictionary = painted_cycles[mode]
		return float(data.cycle_distance_pixels) * base_pixel_size * 109.0 * Catalog.relative_height(species) / float(data.reference_height)
	if body_rigs.has(species): return lerpf(1.75, 2.15, run_blend)
	if step_rigs.has(species) and is_instance_valid(sprite):
		var amplitude: float = float(step_rigs[species].half_stride)



		var pixels: = base_pixel_size * 109.0 * Catalog.relative_height(species) / maxf(1.0, walk_reference_height)
		return maxf(lerpf(2.15, 2.8, run_blend), StepGait.stride(amplitude, pixels, run_blend))
	return super._stride_length()

func run_frame(stride: float, time: float, direction: Vector2) -> int:
	var row: = 0 if absf(direction.x) >= absf(direction.y) else 1 if direction.y > 0 else 2
	var phase: = posmod(int((time * FLIGHT_CYCLES if species == "nocti" else stride) * 8), 8)

	if species == "mosslyn" and row == 0: phase = [0, 1, 3, 4, 5, 6, 7, 1][phase]
	return row * 8 + phase

func _hover_target(moving: bool) -> float:
	if authored_m4 and species in ["nocti", "starlet"] and absf(facing.x) >= absf(facing.y): return 0.0
	if authored_m4 and species == "nocti" and state == "idle" and jump_clock < 0 and is_on_floor(): return 0.48 if moving else 0.0
	if state != "idle" or jump_clock >= 0 or not is_on_floor(): return 0.0
	if species == "nocti" and moving and running: return 0.48 + 0.012 * sin(clock * TAU * FLIGHT_CYCLES)
	return super._hover_target(moving)

func visual_screen_rect(camera: Camera3D) -> Rect2:
	var bounds: = displayed_bounds; var minimum: = Vector2(INF, INF); var maximum: = Vector2( - INF, - INF)
	for x in [bounds.position.x, bounds.end.x]:
		for y in [bounds.position.y, bounds.end.y]:
			var local_x: = float(x) - cell_size.x * 0.5
			if sprite.flip_h: local_x = - local_x
			var point: = global_position + sprite.position + camera.global_basis.x * (local_x * sprite.pixel_size + painted_x_offset) + camera.global_basis.y * (cell_size.y * 0.5 + sprite.offset.y - float(y)) * sprite.pixel_size
			var screen: = camera.unproject_position(point);minimum = minimum.min(screen);maximum = maximum.max(screen)
	return Rect2(minimum, maximum - minimum)

func _apply_ground_motion() -> void :
	if ground_motion_profile.is_empty(): return
	painted_material.set_shader_parameter("wing_active", false)
	var target: float = bound_pose.lift if not bound_pose.is_empty() else 0.0
	ground_motion_lift = target if not bound_pose.is_empty() else move_toward(ground_motion_lift, 0.0, get_physics_process_delta_time() * 1.8)
	if state != "idle" or jump_clock >= 0 or mounted: ground_motion_lift = 0.0
	ground_airborne = ground_motion_lift > 0.015
	sprite.position.y += ground_motion_lift
	var tilt: float = bound_pose.pitch if not bound_pose.is_empty() else 0.0
	var side_view: = absf(facing.x) >= absf(facing.y)
	tilt *= (-1.0 if sprite.flip_h else 1.0) if side_view else 0.3
	var pivot: float = float(bound_pose.get("pivot", 0)) * (-1.0 if sprite.flip_h else 1.0) if side_view else 0.0
	painted_material.set_shader_parameter("ground_motion_pivot", Vector2(pivot, 0))
	painted_material.set_shader_parameter("ground_motion_rotation", Vector2(cos(tilt), sin(tilt)))
	if not wing_regions.is_empty() and painted_cycle_active and painted_cycle_mode == "run" and not bound_pose.is_empty():
		var region: Dictionary = wing_regions[sprite.frame]
		painted_material.set_shader_parameter("wing_active", true)
		painted_material.set_shader_parameter("wing_rotation", Vector2(cos(bound_pose.wing), sin(bound_pose.wing)))
		painted_material.set_shader_parameter("wing_pivot", region.pivot)
		painted_material.set_shader_parameter("wing_point_count", region.count)
		painted_material.set_shader_parameter("wing_points", region.points)
	if actual_speed < 0.1 and stamina.winded_clock > 0:
		sprite.position.y += sin(clock * TAU * 0.9) * 0.008 * stamina.winded_clock / 2.4

func _update_bound_footing(redirect: bool = false) -> void :
	if landing_plan.valid and not redirect:
		last_landing_error = Vector2(global_position.x - landing_plan.target.x, global_position.z - landing_plan.target.z).length()
		landing_samples += 1
	var stage = get_parent().get("stage")
	var wish: = Vector3(velocity.x, 0, velocity.z)
	if InputMap.has_action("move_left") and _desired_input().length() > 0.1: wish = _world_direction(_desired_input())
	if wish.length_squared() < 0.001: wish = Vector3(facing.x, 0, facing.y)
	var reach: float = 2.8 if species == "mosslyn" else bound_flow.stride
	if redirect: reach = maxf(0.15, bound_flow.stride * (1.0 - bound_flow.phase))
	landing_plan.choose(stage, global_position, wish, Vector3(velocity.x, 0, velocity.z), reach, collider.shape.radius, bound_flow.count)
	bound_flow.clearance = landing_plan.free;bound_flow.launch_distance = landing_plan.distance
	bound_flow.launch_turn = landing_plan.turn;bound_flow.plant_side = landing_plan.side
	retarget_landing = false

func _locomotion_target(target: Vector3, _delta: float) -> Vector3:
	if ground_motion_profile.is_empty() or bound_pose.is_empty() or state != "idle" or jump_clock >= 0 or target.length() <= WALK_SPEED + 0.1: return target
	var speed: = target.length()
	var direction: = target.normalized()
	if landing_plan.valid:
		if landing_plan.intent.dot(direction) > 0.75:
			var to: Vector3 = landing_plan.target - global_position;to.y = 0
			var lateral: Vector3 = to - direction * to.dot(direction)
			target += lateral.limit_length(0.2) * 2.8
		else: retarget_landing = true
	return target.normalized() * speed * float(bound_pose.get("thrust", 1.0))
