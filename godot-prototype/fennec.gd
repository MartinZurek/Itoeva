extends CharacterBody3D
## Fennec als Billboard: der vorhandene 138-Bilder-Bogen aus app-sim (Rich-Format, 128 px),
## umgepackt in ein 12x12-Raster (tools/pack_sheet.py). Welche Ansicht gezeigt wird, haengt
## davon ab, wohin Fennec relativ zur Kamera schaut - dreht man die Kamera, dreht sich auch
## die gezeigte Ansicht. Die Bildindizes entsprechen CreatureSprites.Rich in app-sim.

const COLS := 12
const FRAME := 128
const FEET := 126
const HEIGHT := 0.95                      # Meter, Ohrspitze bis Fuss
const PX := HEIGHT / 120.0                # Weltmeter je Bogenpixel

const WALK_SPEED := 1.25
const RUN_SPEED := 3.0
const ACCEL := 9.0
const WALK_STRIDE := 0.85                 # Meter je Gangzyklus (8 Bilder)
const RUN_STRIDE := 1.7

# CreatureSprites.Rich
const IDLE_FIRST := 0
const WALK_FIRST := 9
const JOY_FIRST := 17
const FRONT_IDLE_FIRST := 39
const BACK_IDLE_FIRST := 47
const FRONT_JOY_FIRST := 56
const BACK_JOY_FIRST := 62
const DRAWN_FRONT_WALK_FIRST := 68
const DRAWN_BACK_WALK_FIRST := 76
const RUN_FIRST := 114
const FRONT_RUN_FIRST := 122
const BACK_RUN_FIRST := 126
const IDLE_S := 0.42
const JOY_S := 0.11

var cam: Camera3D
var target = null                          # Vector3 oder null
var running := false
var facing := Vector3(0, 0, 1)
var sprite: Sprite3D
var _phase := 0.0
var _idle_t := 0.0
var _joy_t := -1.0
var _stuck_t := 0.0
var landscape: Node3D
var manual := Vector2.ZERO
var route := PackedVector3Array()
var route_index := 0
var blocked := false
var wading := false
var seated := false


func setup(blob: Material) -> void:
	motion_mode = CharacterBody3D.MOTION_MODE_GROUNDED
	collision_layer = 2
	collision_mask = 1
	floor_snap_length = .24
	var cs := CollisionShape3D.new()
	var cap := CylinderShape3D.new()
	cap.radius = 0.24
	cap.height = 0.9
	cs.shape = cap
	cs.position.y = 0.45
	add_child(cs)

	sprite = Sprite3D.new()
	sprite.texture = load("res://assets/fennec_grid.png")
	sprite.hframes = COLS
	sprite.vframes = COLS
	sprite.pixel_size = PX
	sprite.billboard = BaseMaterial3D.BILLBOARD_FIXED_Y
	sprite.texture_filter = BaseMaterial3D.TEXTURE_FILTER_NEAREST
	sprite.alpha_cut = SpriteBase3D.ALPHA_CUT_DISCARD
	sprite.shaded = false
	sprite.double_sided = true
	sprite.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	# Mitte des Bildes liegt 64 px ueber dem oberen Rand; die Fuesse auf Zeile FEET.
	sprite.position.y = (FEET - FRAME / 2.0) * PX
	add_child(sprite)

	var shadow := MeshInstance3D.new()
	var pm := PlaneMesh.new()
	pm.size = Vector2(0.62, 0.36)
	shadow.mesh = pm
	shadow.material_override = blob
	shadow.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	shadow.position.y = 0.012
	add_child(shadow)


func go(point: Vector3, run: bool) -> bool:
	var map := get_world_3d().navigation_map
	if NavigationServer3D.map_get_iteration_id(map) == 0:
		return false
	var start := NavigationServer3D.map_get_closest_point(map, global_position)
	var finish := NavigationServer3D.map_get_closest_point(map, point)
	var planned := NavigationServer3D.map_get_path(map, start, finish, true)
	if finish.distance_to(point) > .65 or planned.is_empty() or planned[-1].distance_to(finish) > .4:
		target = null
		blocked = true
		return false
	target = finish
	route = planned
	route_index = 0
	running = run
	seated = false
	_stuck_t = 0.0
	blocked = false
	return true


func joy() -> void:
	_joy_t = 0.0
	blocked = false
	target = null
	seated = false


func jump() -> void:
	if is_on_floor() and not wading:
		velocity.y = 4.2
		seated = false


func _physics_process(dt: float) -> void:
	var want := Vector3.ZERO
	wading = landscape != null and landscape.water_depth(global_position) > .08
	var speed := (RUN_SPEED if running else WALK_SPEED) * (.55 if wading else 1.0)
	if manual.length() > .12 and cam != null:
		target = null
		blocked = false
		seated = false
		var right := Vector3(cam.global_basis.x.x,0,cam.global_basis.x.z).normalized()
		var down := Vector3(cam.global_basis.z.x,0,cam.global_basis.z.z).normalized()
		want = (right * manual.x + down * manual.y) * speed
	elif target != null:
		var distance: float = Vector2(target.x-global_position.x,target.z-global_position.z).length()
		if distance < .14:
			target = null
		else:
			while route_index < route.size()-1 and Vector2(route[route_index].x-global_position.x,route[route_index].z-global_position.z).length() < .07:
				route_index += 1
			var d: Vector3 = route[route_index] - global_position
			d.y = 0
			want = d.normalized()*minf(speed,distance*3.5+.25)
	velocity.x = move_toward(velocity.x,want.x,ACCEL*dt)
	velocity.z = move_toward(velocity.z,want.z,ACCEL*dt)
	if not is_on_floor(): velocity.y -= 16.0*dt
	elif velocity.y < 0: velocity.y = 0
	move_and_slide()
	# A failed route must not leave a marker and a forever-walking character.
	if target != null and Vector2(get_real_velocity().x,get_real_velocity().z).length() < .1 and want.length() > .3:
		_stuck_t += dt
		if _stuck_t > 1.5:
			print("PATH BLOCKED position=",global_position," index=",route_index," path=",route)
			target = null
			blocked = true
	else: _stuck_t = 0.0
	_animate(dt)


func _animate(dt: float) -> void:
	if sprite == null or cam == null:
		return
	var hv := get_real_velocity()
	hv.y = 0
	var speed := hv.length()
	if speed > 0.15:
		facing = hv / speed
	var b := cam.global_transform.basis
	var right := Vector3(b.x.x, 0, b.x.z).normalized()
	var away := Vector3(-b.z.x, 0, -b.z.z).normalized()
	var sx := facing.dot(right)
	var sz := facing.dot(away)
	var view := "side"
	if abs(sz) > abs(sx) * 1.15:
		view = "back" if sz > 0 else "front"
	sprite.flip_h = view == "side" and sx < 0

	var f := 0
	if _joy_t >= 0.0:
		_joy_t += dt
		var i := int(_joy_t / JOY_S)
		if i >= 6:
			_joy_t = -1.0
			i = 5
		f = {"side": JOY_FIRST, "front": FRONT_JOY_FIRST, "back": BACK_JOY_FIRST}[view] + i
	elif seated:
		f = {"side": 93, "front": 103, "back": 110}[view]
	elif speed > 0.15:
		_idle_t = 0.0
		var run := speed > (WALK_SPEED + RUN_SPEED) / 2.0
		_phase = fposmod(_phase + speed * dt / (RUN_STRIDE if run else WALK_STRIDE), 1.0)
		if run:
			if view == "side":
				f = RUN_FIRST + int(_phase * 8)
			else:
				f = (FRONT_RUN_FIRST if view == "front" else BACK_RUN_FIRST) + int(_phase * 4)
		else:
			f = {"side": WALK_FIRST, "front": DRAWN_FRONT_WALK_FIRST, "back": DRAWN_BACK_WALK_FIRST}[view] + int(_phase * 8)
	else:
		_idle_t += dt
		f = {"side": IDLE_FIRST, "front": FRONT_IDLE_FIRST, "back": BACK_IDLE_FIRST}[view] + int(_idle_t / IDLE_S) % 8
	sprite.frame = f

