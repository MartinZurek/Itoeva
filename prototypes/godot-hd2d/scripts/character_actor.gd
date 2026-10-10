extends CharacterBody3D

const Catalog = preload("res://scripts/world_catalog.gd")
const GRAVITY := 14.0
const WALK_SPEED := 2.0
const RUN_SPEED := 4.6
var species := "fennec"
var player_controlled := true
var enabled := true
var base_pixel_size := 0.020
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

func mount_seat(anchor: Vector3, exit_position: Vector3) -> void:
	seat_exit = exit_position
	position = anchor
	velocity = Vector3.ZERO
	mounted = true
	state = "sit"
	state_clock = 0.0

func _ready() -> void:
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
	sprite.hframes = 12
	sprite.vframes = 15
	sprite.billboard = BaseMaterial3D.BILLBOARD_FIXED_Y
	sprite.alpha_cut = SpriteBase3D.ALPHA_CUT_DISCARD
	sprite.alpha_scissor_threshold = 0.12
	sprite.texture_filter = BaseMaterial3D.TEXTURE_FILTER_NEAREST
	sprite.shaded = false
	sprite.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	add_child(sprite)
	shadow = MeshInstance3D.new()
	var plane := PlaneMesh.new()
	plane.size = Vector2(1.15, 0.62)
	shadow.mesh = plane
	var material := ShaderMaterial.new()
	material.shader = load("res://shaders/contact_shadow.gdshader")
	shadow.material_override = material
	shadow.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	get_parent().add_child.call_deferred(shadow)
	set_world_scale(base_pixel_size, jump_height)
	_update_frame(false)

func set_world_scale(pixel_size: float, leap: float) -> void:
	base_pixel_size = pixel_size
	jump_height = leap
	if is_instance_valid(collider):
		var capsule: CapsuleShape3D = collider.shape
		var factor: float = pixel_size / 0.020
		capsule.radius = 0.25 * minf(factor, 1.5)
		capsule.height = 1.4 * factor * float(Catalog.data().characters[species].relative_height)
		collider.position.y = capsule.height * 0.5
		floor_snap_length = 0.2
	if is_instance_valid(sprite):
		_update_sprite_scale()

func _update_sprite_scale() -> void:
	var ref: Dictionary = Catalog.data().characters[species]
	var size_factor: float = 109.0 * float(ref.relative_height) / (126.0 - float(ref.top))
	var living_factor := 1.032967 if species == "wyrmling" and sprite.frame >= 138 else 1.0
	sprite.pixel_size = base_pixel_size * size_factor * living_factor
	sprite.position.y = 62.0 * sprite.pixel_size

func request_action(action: String) -> void:
	if not enabled:
		return
	if action == "jump":
		jump_requested = true
	elif action == "roll" and (is_on_floor() or mounted):
		state = "roll"
		state_clock = 0.0
		roll_direction = _world_direction(facing).normalized()
	elif action == "sit" and (is_on_floor() or mounted):
		state = "idle" if state == "sit" else "sit"
		state_clock = 0.0

func stop_input() -> void:
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
	if state == "sit":
		speed = 0.0
	if state == "roll":
		if state_clock < 0.62:
			direction = roll_direction
			speed = 4.8
		else:
			state = "idle"
	var target := direction.normalized() * speed if direction.length() > 0.01 else Vector3.ZERO
	var acceleration := 9.0 if target.length() > Vector2(velocity.x, velocity.z).length() else 14.0
	velocity.x = move_toward(velocity.x, target.x, delta * acceleration)
	velocity.z = move_toward(velocity.z, target.z, delta * acceleration)
	if is_on_floor() and jump_clock < 0.0:
		coyote = 0.12
	else:
		coyote = maxf(0.0, coyote - delta)
	if jump_requested or player_controlled and Input.is_action_just_pressed("jump"):
		jump_buffer = 0.16
	jump_requested = false
	if jump_buffer > 0.0 and coyote > 0.0 and state != "roll":
		state = "idle"
		velocity.y = sqrt(2.0 * GRAVITY * jump_height)
		jump_clock = 0.0
		jump_buffer = 0.0
		coyote = 0.0
	jump_buffer = maxf(0.0, jump_buffer - delta)
	if not is_on_floor():
		velocity.y -= GRAVITY * delta
	var before := global_position
	move_and_slide()
	var distance := Vector2(global_position.x - before.x, global_position.z - before.z).length()
	actual_speed = distance / delta
	var moving := distance > 0.0008
	# Hysterese verhindert hektischen Rollenwechsel genau an der Jogging-Schwelle.
	if running and actual_speed < 2.25:
		running = false
	elif not running and actual_speed > 2.65:
		running = true
	if moving and input.length() > 0.06 and state != "roll":
		var next := Vector2(signf(input.x), 0) if absf(input.x) > absf(input.y) else Vector2(0, signf(input.y))
		if next != facing:
			turn_clock = 0.0
			facing = next
	if moving != previous_moving:
		transition_clock = 0.0
		previous_moving = moving
	# Ein Zyklus entspricht einer zur Zeichnung passenden Strecke, nicht der Wanduhr.
	if moving:
		gait += distance / (2.5 if running else 1.5)
	if jump_clock >= 0.0:
		jump_clock += delta
		if is_on_floor():
			jump_clock = -1.0
			recovery_clock = 0.0
	_update_frame(moving)
	_update_shadow()

func _update_frame(moving: bool) -> void:
	var depth := absf(facing.y) > absf(facing.x)
	sprite.flip_h = not depth and facing.x < 0.0
	if jump_clock >= 0.0:
		var duration := 2.0 * sqrt(2.0 * jump_height / GRAVITY)
		var phase := clampi(int(jump_clock / maxf(duration, 0.1) * 6), 0, 5)
		var clip := [84, 85, 86, 86, 87, 95]
		if depth:
			var first := 100 if facing.y > 0.0 else 107
			clip = [first, first + 1, first + 2, first + 2, first, first + 4]
		sprite.frame = clip[phase]
	elif state == "roll":
		sprite.frame = 130 + clampi(int(state_clock / 0.62 * 8), 0, 7)
	elif state == "sit":
		if state_clock > 0.36:
			sprite.frame = 169 if not depth else 103 if facing.y > 0 else 110
		else:
			var clip := [95, 92, 93] if not depth else [104, 100, 103] if facing.y > 0 else [111, 107, 110]
			sprite.frame = clip[clampi(int(state_clock / 0.12), 0, 2)]
	elif turn_clock < 0.18:
		sprite.frame = (148 if depth and facing.y > 0 else 38 if depth else 147) if turn_clock < 0.09 else (27 if facing.y > 0 else 32) if depth else 147
	elif moving:
		if not depth and transition_clock < 0.18 and not running:
			sprite.frame = 150 if transition_clock < 0.09 else 151
		else:
			var first := 114 if running else 9
			var count := 8
			if depth:
				first = (122 if facing.y > 0 else 126) if running else (68 if facing.y > 0 else 76)
				count = 4 if running else 8
			sprite.frame = first + int(gait * count) % count
	elif recovery_clock < 0.48 and not depth:
		sprite.frame = 162 + clampi(int(recovery_clock / 0.12), 0, 3)
	elif transition_clock < 0.24 and not depth:
		sprite.frame = 152 if transition_clock < 0.12 else 153
	elif depth and facing.y < 0:
		sprite.frame = 47 + int(clock / 0.42) % 8
	else:
		sprite.frame = (138 if depth else 142) + int(clock / 0.85) % 4
	_update_sprite_scale()

func _update_shadow() -> void:
	if not is_instance_valid(shadow) or not shadow.is_inside_tree():
		return
	var query := PhysicsRayQueryParameters3D.create(global_position + Vector3.UP * 0.1, global_position + Vector3.DOWN * 12, 1, [get_rid()])
	var hit := get_world_3d().direct_space_state.intersect_ray(query)
	shadow.visible = not hit.is_empty()
	if not hit.is_empty():
		shadow.global_position = hit.position + Vector3.UP * 0.008
		var scale_factor := base_pixel_size / 0.020 * clampf(1.0 - (global_position.y - hit.position.y) * 0.22, 0.4, 1.0)
		shadow.scale = Vector3.ONE * scale_factor

func _exit_tree() -> void:
	if is_instance_valid(shadow):
		shadow.queue_free()
