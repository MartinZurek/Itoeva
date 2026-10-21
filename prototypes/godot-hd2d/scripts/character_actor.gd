extends CharacterBody3D

const Catalog = preload("res://scripts/world_catalog.gd")
const GRAVITY := 14.0
const WALK_SPEED := 2.0
const RUN_SPEED := 3.3
const ROLL_SECONDS := 0.46
const Stamina = preload("res://scripts/movement_stamina.gd")
var stamina = Stamina.new()
var species := "fennec"
var player_controlled := true
var enabled := true
var base_pixel_size := 0.020
var body_pixel_size := 0.020
var jump_height := 1.7
var touch_vector := Vector2.ZERO
var npc_vector := Vector2.ZERO
var jump_requested := false
var sprite: Sprite3D
var shadow: MeshInstance3D
var collider: CollisionShape3D
var facing := Vector2.RIGHT
var gait := 0.0
var actual_speed := 0.0
var running := false
var state := "idle"
var clock := 0.0
var state_clock := 0.0
var turn_clock := 1.0
var jump_clock := -1.0
var recovery_clock := 1.0
var coyote := 0.0
var jump_buffer := 0.0
var roll_direction := Vector3.RIGHT
var previous_moving := false
var transition_clock := 1.0
var mounted := false
var seat_exit := Vector3.ZERO
var seat_occlusion_z := 0.0
var painted_material: ShaderMaterial
var frame_bounds: Array[Rect2i] = []
var pose_scales: Dictionary = {}
var metrics: Dictionary = {}
var run_blend := 0.0
var twirl_facing := Vector2.RIGHT
var hover := 0.0
var pending_sit := false
var glow_energy := 0.0
var aura: MeshInstance3D
var aura_material: ShaderMaterial

func mount_seat(anchor: Vector3, exit_position: Vector3) -> void:
	seat_exit = exit_position
	position = anchor
	velocity = Vector3.ZERO
	mounted = true
	# Der Sitz ist ein neuer fester Anker; Flug und Lauf dürfen nicht fortwirken.
	hover = 0.0
	actual_speed = 0.0
	running = false
	run_blend = 0.0
	seat_occlusion_z = anchor.z
	state = "sit"
	state_clock = 0.0

func leave_seat() -> void:
	if not mounted: return
	mounted = false
	position = seat_exit
	state = "idle"
	stop_input()

func _ready() -> void:
	# Schatten duerfen nie auf dem Kollisionskoerper eines Nachbarn landen.
	collision_layer = 2
	collision_mask = 1
	collider = CollisionShape3D.new()
	var capsule := CapsuleShape3D.new()
	capsule.radius = 0.25
	capsule.height = 1.4
	collider.shape = capsule
	collider.position.y = capsule.height * 0.5
	add_child(collider)
	sprite = Sprite3D.new()
	sprite.name = "OriginalPainted" + species.capitalize()
	sprite.texture = load(Catalog.data().characters[species].asset)
	if metrics.has(species):
		frame_bounds.assign(metrics[species].bounds)
		pose_scales = metrics[species].scales
	else:
		var atlas_image := sprite.texture.get_image()
		for frame in range(170):
			frame_bounds.append(atlas_image.get_region(Rect2i((frame % 12) * 128, (frame / 12) * 128, 128, 128)).get_used_rect())
		_calibrate_views(atlas_image)
		metrics[species] = {"bounds": frame_bounds.duplicate(), "scales": pose_scales.duplicate()}
	sprite.hframes = 12
	sprite.vframes = 15
	sprite.billboard = BaseMaterial3D.BILLBOARD_ENABLED
	sprite.alpha_cut = SpriteBase3D.ALPHA_CUT_DISCARD
	sprite.alpha_scissor_threshold = 0.12
	sprite.texture_filter = BaseMaterial3D.TEXTURE_FILTER_LINEAR
	sprite.shaded = false
	sprite.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	painted_material = ShaderMaterial.new()
	# Transparente Anschlussbilder duerfen den Avatar nicht als Ganzes uebermalen.
	# Gezeichnete Vordergruende schneiden nur ueber die gemeinsame Tiefenmaske aus.
	painted_material.render_priority = 10
	painted_material.shader = load("res://shaders/painted_character.gdshader")
	painted_material.set_shader_parameter("atlas", sprite.texture)
	sprite.material_override = painted_material
	sprite.offset = Vector2(0, 62)
	add_child(sprite)
	_setup_aura()
	shadow = MeshInstance3D.new()
	var plane := PlaneMesh.new()
	plane.size = Vector2(1.15, 0.62)
	shadow.mesh = plane
	var material := ShaderMaterial.new()
	material.shader = load("res://shaders/contact_shadow.gdshader")
	material.render_priority = 2
	shadow.material_override = material
	shadow.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	get_parent().add_child.call_deferred(shadow)
	set_world_scale(base_pixel_size, jump_height, body_pixel_size)
	_update_frame(false)

func _setup_aura() -> void:
	if species != "starlet": return
	aura = MeshInstance3D.new()
	var quad := QuadMesh.new()
	quad.size = Vector2.ONE
	aura.mesh = quad
	aura_material = ShaderMaterial.new()
	aura_material.shader = load("res://shaders/star_aura.gdshader")
	aura_material.render_priority = 9
	aura.material_override = aura_material
	aura.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	add_child(aura)

func set_world_scale(pixel_size: float, leap: float, footprint: float = -1.0) -> void:
	base_pixel_size = pixel_size
	body_pixel_size = footprint if footprint > 0 else pixel_size
	jump_height = leap
	if is_instance_valid(collider):
		var capsule: CapsuleShape3D = collider.shape
		var factor: float = body_pixel_size / 0.020
		capsule.radius = 0.25 * minf(factor, 1.5)
		capsule.height = 1.4 * factor * Catalog.relative_height(species)
		collider.position.y = capsule.height * 0.5
		floor_snap_length = 0.2
	if is_instance_valid(sprite):
		_update_sprite_scale()

func _update_sprite_scale() -> void:
	var size_factor: float = 109.0 * Catalog.relative_height(species) / float(frame_bounds[142].size.y)
	# Ein fester Faktor pro ganzer Folge: keine pulsierenden Gesichter pro Bild.
	sprite.pixel_size = base_pixel_size * size_factor * float(pose_scales.get(sprite.frame, 1.0))
	sprite.position = Vector3.UP * hover
	sprite.offset.y = 62.0 if mounted else float(frame_bounds[sprite.frame].end.y) - 64.0
	_update_occlusion()

func _median(values: Array[float]) -> float:
	values.sort()
	return values[values.size() / 2]

func _calibrate_views(atlas: Image) -> void:
	var reference := float(frame_bounds[142].size.y)
	for frame in [148, 149]:
		pose_scales[frame] = reference / float(frame_bounds[frame].size.y)
	var side_area := 0.0
	var walk_areas: Array[float] = []
	var run_areas: Array[float] = []
	for frame in [142, 9, 10, 11, 12, 13, 14, 15, 16, 114, 115, 116, 117, 118, 119, 120, 121]:
		var area := 0.0
		var bounds := frame_bounds[frame]
		var offset := Vector2i((frame % 12) * 128, (frame / 12) * 128)
		for y in range(bounds.position.y, bounds.end.y):
			for x in range(bounds.position.x, bounds.end.x):
				if atlas.get_pixelv(offset + Vector2i(x, y)).a > 0.31: area += 1
		if frame == 142: side_area = area
		elif frame < 17: walk_areas.append(area)
		else: run_areas.append(area)
	var mass_factor := clampf(pow(side_area / _median(walk_areas), 0.25), 0.92, 1.12)
	var side_heights: Array[float] = []
	for frame in range(9, 17): side_heights.append(float(frame_bounds[frame].size.y))
	var walk_height := _median(side_heights)
	for first in [9, 68, 76]:
		var heights: Array[float] = []
		for frame in range(first, first + 8): heights.append(float(frame_bounds[frame].size.y))
		var factor := clampf(walk_height / _median(heights) * mass_factor, 0.82, 1.18)
		for frame in range(first, first + 8): pose_scales[frame] = factor
	# Der Vierbeiner bleibt tiefer; seine Koerpermasse darf dabei nicht schrumpfen.
	var run_mass := clampf(sqrt(side_area / _median(run_areas)), 0.92, 1.16)
	for frame in range(114, 130): pose_scales[frame] = run_mass
	for first in [122, 126]:
		if species != "fennec":
			for frame in range(first, first + 4): pose_scales[frame] = pose_scales[68 if first == 122 else 76]

func rest_frame() -> int:
	# Ein Blickwinkel bleibt eine Zeichnung. Unabhaengig gemalte Atemphasen
	# veraenderten Gesicht/Brille und teils sogar die Blickrichtung.
	return 149 if facing.y < 0 and absf(facing.y) > absf(facing.x) else 148 if absf(facing.y) > absf(facing.x) else 142

func visual_screen_rect(camera: Camera3D) -> Rect2:
	var bounds := frame_bounds[sprite.frame]
	var minimum := Vector2(INF, INF)
	var maximum := Vector2(-INF, -INF)
	for x in [bounds.position.x, bounds.end.x]:
		for y in [bounds.position.y, bounds.end.y]:
			var local_x := float(x) - 64.0
			if sprite.flip_h: local_x = -local_x
			var point := global_position + sprite.position + camera.global_basis.x * local_x * sprite.pixel_size + camera.global_basis.y * (64.0 + sprite.offset.y - float(y)) * sprite.pixel_size
			var screen := camera.unproject_position(point)
			minimum = minimum.min(screen)
			maximum = maximum.max(screen)
	return Rect2(minimum, maximum - minimum)

func set_draw_order(order: int) -> void:
	painted_material.render_priority = order
	sprite.render_priority = order
	if is_instance_valid(aura_material):aura_material.render_priority=order-1

func light_center() -> Vector3:
	var camera:=get_viewport().get_camera_3d()
	var up:=camera.global_basis.y if camera else Vector3.UP
	var right:=camera.global_basis.x if camera else Vector3.RIGHT
	var center:=Vector2(frame_bounds[sprite.frame].get_center())
	var cell := sprite.texture.get_size() / Vector2(sprite.hframes, sprite.vframes)
	var x: float=(center.x-cell.x*.5)*sprite.pixel_size*(-1.0 if sprite.flip_h else 1.0)
	return global_position+sprite.position+right*x+up*(cell.y*.5+sprite.offset.y-center.y)*sprite.pixel_size

func _update_occlusion() -> void:
	if not is_instance_valid(painted_material) or not is_inside_tree():
		return
	painted_material.set_shader_parameter("foot", Vector3(global_position.x, global_position.y, seat_occlusion_z) if mounted else global_position)
	var stage = get_parent().get("stage")
	if species=="starlet" and is_instance_valid(stage):
		var target: float=stage.darkness_at(global_position)
		glow_energy=move_toward(glow_energy,target,get_process_delta_time()*1.2)
		painted_material.set_shader_parameter("self_glow",glow_energy)
		if is_instance_valid(aura):
			var height:=frame_bounds[sprite.frame].size.y*sprite.pixel_size
			aura.position=light_center()-global_position
			aura.scale=Vector3.ONE*height*1.9
			aura_material.set_shader_parameter("energy",glow_energy)
		stage.update_character_light(self,glow_energy)
	if is_instance_valid(stage) and stage.has_method("apply_actor_light"):
		stage.apply_actor_light(painted_material, global_position)
	if is_instance_valid(stage) and stage.has_method("actor_occlusion"):
		var data: Dictionary = stage.actor_occlusion(global_position)
		painted_material.set_shader_parameter("has_mask", data.mask != null)
		if data.mask != null:
			painted_material.set_shader_parameter("furniture_mask", data.mask)
		painted_material.set_shader_parameter("scene_origin", float(data.origin))
		painted_material.set_shader_parameter("scene_span", float(data.span))

func request_action(action: String) -> bool:
	if not enabled:
		return false
	if action == "jump":
		pending_sit = false
		if state == "roll" and state_clock >= ROLL_SECONDS - 0.16:
			state = "idle"
		jump_requested = true
		return true
	elif action == "roll" and species == "hootlet":
		if state == "twirl": return false
		if player_controlled and not stamina.spend(14.0): return false
		if mounted:
			mounted = false
			position = seat_exit
			velocity = Vector3.ZERO
		twirl_facing = facing
		state = "twirl"
		state_clock = 0.0
		roll_direction = _world_direction(facing).normalized()
		if is_on_floor():
			velocity.y = sqrt(2.0 * GRAVITY * jump_height * 0.30)
			jump_clock = 0.0
		return true
	elif action == "roll" and (is_on_floor() or mounted):
		if state == "roll": return false
		if player_controlled and not stamina.spend(14.0): return false
		state = "roll"
		state_clock = 0.0
		roll_direction = _world_direction(facing).normalized()
		return true
	elif action == "sit" and (is_on_floor() or mounted):
		state = "idle" if state == "sit" else "sit"
		state_clock = 0.0
		if state == "sit": stop_input()
		return true
	elif action == "sit":
		var vertical := velocity.y
		stop_input()
		velocity.y = vertical
		pending_sit = true
		return true
	return false

func stop_input() -> void:
	pending_sit = false
	touch_vector = Vector2.ZERO
	npc_vector = Vector2.ZERO
	jump_requested = false
	jump_buffer = 0.0
	velocity = Vector3.ZERO

func _world_direction(input: Vector2) -> Vector3:
	var camera := get_viewport().get_camera_3d()
	var right := camera.global_basis.x if camera else Vector3.RIGHT
	var front := camera.global_basis.z if camera else Vector3.BACK
	right.y = 0.0
	front.y = 0.0
	return right.normalized() * input.x + front.normalized() * input.y

func _desired_input() -> Vector2:
	if not player_controlled:
		return npc_vector
	var result := Input.get_vector("move_left", "move_right", "move_back", "move_front", 0.14)
	if touch_vector.length() > 0.14:
		var force := clampf((touch_vector.length() - 0.14) / 0.86, 0.0, 1.0)
		result = touch_vector.normalized() * force
	return result

func _physics_process(delta: float) -> void:
	if not enabled:
		return
	clock += delta
	state_clock += delta
	turn_clock += delta
	recovery_clock += delta
	transition_clock += delta
	var input := _desired_input()
	var magnitude := input.length()
	if player_controlled and Input.is_action_just_pressed("jump"):
		jump_requested = true
	if player_controlled and Input.is_action_just_pressed("sit"):
		request_action("sit")
	if player_controlled and Input.is_action_just_pressed("roll"):
		request_action("roll")
	if mounted:
		if state == "sit" and magnitude <= 0.12 and not jump_requested:
			if player_controlled: stamina.advance(delta, 0.0, false, true, true)
			_update_frame(false)
			_update_shadow()
			return
		mounted = false
		position = seat_exit
		if state == "sit":
			state = "idle"
		velocity = Vector3.ZERO
	if state == "sit" and (magnitude > 0.12 or jump_requested):
		state = "idle"
		state_clock = 0.0
	var direction := _world_direction(input)
	var analog := touch_vector.length() > 0.14 or not player_controlled or not Input.get_connected_joypads().is_empty()
	# Aussenring und Tastensprint erreichen dasselbe Tempo; der Zwischenbereich bleibt stetig.
	var speed := lerpf(0.0, WALK_SPEED, minf(magnitude / 0.64, 1.0))
	if magnitude > 0.64 and analog and player_controlled:
		speed = lerpf(WALK_SPEED, RUN_SPEED, smoothstep(0.64, 1.0, magnitude))
	if player_controlled and Input.is_action_pressed("sprint"):
		speed = RUN_SPEED * magnitude
	if player_controlled and not stamina.allows_sprint():
		speed = minf(speed, WALK_SPEED)
	if state == "sit":
		speed = 0.0
	if pending_sit:
		direction = Vector3.ZERO
		speed = 0.0
	if state == "roll":
		if state_clock < ROLL_SECONDS:
			if magnitude > 0.2:
				roll_direction = roll_direction.lerp(direction.normalized(), delta * 1.2).normalized()
			direction = roll_direction
			speed = lerpf(6.2, 3.5, smoothstep(0.0, ROLL_SECONDS, state_clock))
		else:
			state = "idle"
	if state == "twirl":
		if state_clock < 0.64:
			direction = roll_direction
			speed = 3.0
		else:
			state = "idle"
			facing = twirl_facing
	var target := direction.normalized() * speed if direction.length() > 0.01 else Vector3.ZERO
	var acceleration := 22.0 if target.length() > Vector2(velocity.x, velocity.z).length() else 30.0
	velocity.x = move_toward(velocity.x, target.x, delta * acceleration)
	velocity.z = move_toward(velocity.z, target.z, delta * acceleration)
	if is_on_floor() and jump_clock < 0.0:
		coyote = 0.12
	else:
		coyote = maxf(0.0, coyote - delta)
	if jump_requested or player_controlled and Input.is_action_just_pressed("jump"):
		jump_buffer = 0.16
	jump_requested = false
	if jump_buffer > 0.0 and coyote > 0.0 and state not in ["roll", "twirl"]:
		state = "idle"
		velocity.y = sqrt(2.0 * GRAVITY * jump_height)
		jump_clock = 0.0
		jump_buffer = 0.0
		coyote = 0.0
	jump_buffer = maxf(0.0, jump_buffer - delta)
	if not is_on_floor():
		velocity.y -= GRAVITY * delta
	var before := global_position
	var stage = get_parent().get("stage")
	if is_instance_valid(stage) and stage.has_method("guide_motion"):
		velocity = stage.guide_motion(before, velocity, delta, collider.shape.radius)
	move_and_slide()
	if is_instance_valid(stage) and stage.has_method("constrain_step"):
		var confined: Vector3 = stage.constrain_step(before, global_position, collider.shape.radius)
		if not is_equal_approx(confined.x, position.x):
			velocity.x = 0.0
		if not is_equal_approx(confined.z, position.z):
			velocity.z = 0.0
		position = confined
	var distance := Vector2(global_position.x - before.x, global_position.z - before.z).length()
	actual_speed = distance / delta
	var moving := distance > 0.0008
	if pending_sit and is_on_floor():
		pending_sit = false
		state = "sit"
		state_clock = 0.0
		stop_input()
		moving = false
	# Hysterese verhindert hektischen Rollenwechsel genau an der Jogging-Schwelle.
	if running and actual_speed < 2.3:
		running = false
	elif not running and actual_speed > 2.65:
		running = true
	run_blend = move_toward(run_blend, 1.0 if running else 0.0, delta / 0.16)
	if moving and input.length() > 0.06 and state not in ["roll", "twirl"]:
		var next := Vector2(signf(input.x), 0) if absf(input.x) > absf(input.y) else Vector2(0, signf(input.y))
		if next != facing:
			turn_clock = 0.0
			facing = next
	if moving != previous_moving:
		transition_clock = 0.0
		previous_moving = moving
	# Ein Zyklus entspricht einer zur Zeichnung passenden Strecke, nicht der Wanduhr.
	if moving:
		gait += distance / _stride_length()
	if player_controlled:
		var effort := clampf((actual_speed - WALK_SPEED) / (RUN_SPEED - WALK_SPEED), 0.0, 1.0) if state == "idle" else 0.0
		stamina.advance(delta, effort, moving, state == "sit", false)
	hover = move_toward(hover, _hover_target(moving), delta * 1.6)
	if jump_clock >= 0.0:
		jump_clock += delta
		if is_on_floor():
			jump_clock = -1.0
			recovery_clock = 0.0
	_update_frame(moving)
	_update_shadow()

func _stride_length() -> float:
	return (3.3 if running else 1.7) * maxf(body_pixel_size / 0.020, 0.5)

func _hover_target(moving: bool) -> float:
	return (0.13 + 0.035 * sin(gait * TAU)) if species == "hootlet" and moving and running and is_on_floor() else 0.0

func _update_frame(moving: bool) -> void:
	var depth := absf(facing.y) > absf(facing.x)
	sprite.flip_h = not depth and facing.x < 0.0
	if state == "twirl":
		# Gemalte Fluegelansichten statt einer eingerollten Kugel in der Luft.
		var phase := clampi(int(state_clock / 0.64 * 6), 0, 5)
		sprite.frame = [85, 101, 108, 108, 101, 85][phase]
		sprite.flip_h = phase >= 2 and phase <= 3 if twirl_facing.x >= 0 else not (phase >= 2 and phase <= 3)
	elif jump_clock >= 0.0:
		var duration := 2.0 * sqrt(2.0 * jump_height / GRAVITY)
		var phase := clampi(int(jump_clock / maxf(duration, 0.1) * 6), 0, 5)
		var clip := [84, 85, 86, 86, 87, 95]
		if depth:
			var first := 100 if facing.y > 0.0 else 107
			clip = [first, first + 1, first + 2, first + 2, first, first + 4]
		sprite.frame = clip[phase]
	elif state == "roll":
		sprite.frame = 130 + clampi(int(state_clock / ROLL_SECONDS * 8), 0, 7)
	elif state == "sit":
		if state_clock > 0.36:
			sprite.frame = 93 if not depth else 103 if facing.y > 0 else 110
		else:
			var clip := [142, 92, 93] if not depth else [148, 100, 103] if facing.y > 0 else [149, 107, 110]
			sprite.frame = clip[clampi(int(state_clock / 0.12), 0, 2)]
	elif moving:
		var run_pose := running and run_blend >= 0.5
		var first := 114 if run_pose else 9
		var count := 8
		if depth:
			first = (122 if facing.y > 0 else 126) if run_pose else (68 if facing.y > 0 else 76)
			count = 4 if run_pose else 8
		sprite.frame = first + int(gait * count) % count
		if species == "fennec" and not depth and run_blend > 0.05 and (run_blend < 0.5 or not running):
			# Die originale gebueckte Haltung verbindet Zweibeiner und Vierbeiner.
			sprite.frame = 89
		if species == "hootlet" and run_pose:
			sprite.frame = [84, 85, 86, 85][int(gait * 4) % 4] if not depth else [100, 101, 102, 101][int(gait * 4) % 4] if facing.y > 0 else [107, 108, 109, 108][int(gait * 4) % 4]
	elif species == "fennec" and run_blend > 0.05 and not depth:
		sprite.frame = 89 if run_blend > 0.65 else 95
	elif recovery_clock < 0.48 and not depth:
		sprite.frame = 87 if recovery_clock < 0.12 else 92 if recovery_clock < 0.24 else rest_frame()
	else:
		sprite.frame = rest_frame()
	_update_sprite_scale()

func _update_shadow() -> void:
	if not is_instance_valid(shadow) or not shadow.is_inside_tree():
		return
	var query := PhysicsRayQueryParameters3D.create(global_position + Vector3.UP * 0.1, global_position + Vector3.DOWN * 12, 1, [get_rid()])
	var hit := get_world_3d().direct_space_state.intersect_ray(query)
	shadow.visible = not hit.is_empty()
	if not hit.is_empty():
		shadow.global_position = hit.position + Vector3.UP * 0.018
		var height_above := maxf(0.0, global_position.y - hit.position.y + hover)
		var scale_factor := body_pixel_size / 0.020 * Catalog.relative_height(species) * lerpf(1.0, 1.35, clampf(height_above / 2.0, 0, 1))
		shadow.material_override.set_shader_parameter("opacity", lerpf(0.34, 0.08, clampf(height_above / 2.0, 0, 1)))
		shadow.scale = Vector3.ONE * scale_factor

func _exit_tree() -> void:
	if is_instance_valid(shadow):
		shadow.queue_free()
