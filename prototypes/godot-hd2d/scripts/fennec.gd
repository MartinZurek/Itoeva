extends CharacterBody3D

const WALK_SPEED := 2.0
const RUN_SPEED := 3.8
const GRAVITY := 14.0
# Native Referenz: 65..108 Bildpixel Koerperhoehe, 640px hohe Kulisse.
# Bei 35 Grad Kamera entspricht .020 etwa 87px Koerperhoehe in der Malerei.
const PIXEL_SIZE := 0.020
var touch_vector := Vector2.ZERO
var touch_sprint := false
var jump_requested := false
var sprite: Sprite3D
var shadow: MeshInstance3D
var facing := Vector2.RIGHT
var gait := 0.0
var idle_clock := 0.0
var jump_clock := -1.0

func _ready() -> void:
	var collider := CollisionShape3D.new()
	var capsule := CapsuleShape3D.new()
	capsule.radius = 0.22
	capsule.height = 1.35
	collider.shape = capsule
	collider.position.y = 0.675
	add_child(collider)
	sprite = Sprite3D.new()
	sprite.name = "OriginalPaintedFennec"
	# Mehrzeiliger, pixelidentischer Atlas vermeidet das 16K-Texturlimit auf GPUs.
	sprite.texture = load("res://assets/fennec-atlas.png")
	sprite.hframes = 12
	sprite.vframes = 12
	sprite.pixel_size = PIXEL_SIZE
	# Der Fussanker 126 ist derselbe wie CreatureSprites.Rich.FEET im Spiel.
	sprite.position.y = (126.0 - 64.0) * PIXEL_SIZE
	sprite.billboard = BaseMaterial3D.BILLBOARD_FIXED_Y
	sprite.alpha_cut = SpriteBase3D.ALPHA_CUT_DISCARD
	sprite.alpha_scissor_threshold = 0.12
	sprite.texture_filter = BaseMaterial3D.TEXTURE_FILTER_NEAREST
	sprite.no_depth_test = false
	sprite.shaded = false
	# Gemalte Beleuchtung bewahren; Bodenschatten statt falschem Billboard-Schatten.
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

func _physics_process(delta: float) -> void:
	var input := Input.get_vector("move_left", "move_right", "move_back", "move_front")
	if touch_vector.length() > input.length():
		input = touch_vector
	var camera := get_viewport().get_camera_3d()
	var right := camera.global_basis.x if camera else Vector3.RIGHT
	var front := camera.global_basis.z if camera else Vector3.BACK
	right.y = 0.0
	front.y = 0.0
	var direction := right.normalized() * input.x + front.normalized() * input.y
	var running := Input.is_action_pressed("sprint") or touch_sprint or input.length() > 0.92 and touch_vector.length() > 0.92
	var speed := RUN_SPEED if running else WALK_SPEED
	velocity.x = move_toward(velocity.x, direction.x * speed, delta * 16.0)
	velocity.z = move_toward(velocity.z, direction.z * speed, delta * 16.0)
	if not is_on_floor():
		velocity.y -= GRAVITY * delta
	if (Input.is_action_just_pressed("jump") or jump_requested) and is_on_floor():
		velocity.y = 6.0
		jump_clock = 0.0
	jump_requested = false
	var before := global_position
	move_and_slide()
	var distance := Vector2(global_position.x - before.x, global_position.z - before.z).length()
	var moving := distance > 0.001
	if moving:
		facing = input.normalized() if input.length() > 0.05 else facing
		gait += distance / (0.82 if running else 0.58)
	else:
		gait = 0.0
	idle_clock += delta
	if jump_clock >= 0.0:
		jump_clock += delta
		if is_on_floor():
			jump_clock = -1.0
	update_frame(moving, running)
	if is_instance_valid(shadow) and shadow.is_inside_tree():
		var query := PhysicsRayQueryParameters3D.create(global_position + Vector3.UP * 0.15, global_position + Vector3.DOWN * 4.0, 1, [get_rid()])
		var hit := get_world_3d().direct_space_state.intersect_ray(query)
		shadow.visible = not hit.is_empty()
		if not hit.is_empty():
			var support: Vector3 = hit.position
			shadow.global_position = support + Vector3.UP * 0.008
			shadow.scale = Vector3.ONE * clampf(1.0 - (global_position.y - support.y) * 0.25, 0.45, 1.0)

func update_frame(moving: bool, running: bool) -> void:
	var depth_facing := absf(facing.y) > absf(facing.x)
	sprite.flip_h = not depth_facing and facing.x < 0.0
	if jump_clock >= 0.0:
		var clip := [84, 85, 86, 86, 87, 95]
		var first := 0
		if depth_facing:
			clip = [0, 1, 2, 2, 0, 4]
			first = 100 if facing.y > 0.0 else 107
		sprite.frame = first + clip[mini(int(jump_clock / 0.13), clip.size() - 1)]
	elif moving:
		var first := 114 if running else 9
		var count := 8
		if depth_facing:
			first = (122 if facing.y > 0.0 else 126) if running else (68 if facing.y > 0.0 else 76)
			count = 4 if running else 8
		sprite.frame = first + int(gait * count) % count
	elif depth_facing:
		sprite.frame = (39 if facing.y > 0.0 else 47) + int(idle_clock / 0.42) % 8
	else:
		sprite.frame = 8 if fmod(idle_clock, 4.2) < 0.16 else int(idle_clock / 0.42) % 8
