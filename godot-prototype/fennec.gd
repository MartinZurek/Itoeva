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


func setup(blob: Material) -> void:
	motion_mode = CharacterBody3D.MOTION_MODE_FLOATING
	var cs := CollisionShape3D.new()
	var cap := CylinderShape3D.new()
	cap.radius = 0.24
	cap.height = 0.9
	cs.shape = cap
	cs.position.y = 0.5
	add_child(cs)

	sprite = Sprite3D.new()
	sprite.texture = load("res://assets/fennec_grid.png")
	sprite.hframes = COLS
	sprite.vframes = COLS
	sprite.pixel_size = PX
	sprite.billboard = BaseMaterial3D.BILLBOARD_FIXED_Y
	sprite.texture_filter = BaseMaterial3D.TEXTURE_FILTER_NEAREST
	sprite.alpha_cut = SpriteBase3D.ALPHA_CUT_DISCARD
	sprite.shaded = true
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


func go(point: Vector3, run: bool) -> void:
	target = Vector3(point.x, 0, point.z)
	running = run
	_stuck_t = 0.0


func joy() -> void:
	_joy_t = 0.0
	target = null


func _physics_process(dt: float) -> void:
	var want := Vector3.ZERO
	if target != null:
		var d: Vector3 = target - global_position
		d.y = 0
		var dist := d.length()
		if dist < 0.06:
			target = null
		else:
			var spd: float = (RUN_SPEED if running else WALK_SPEED)
			spd = min(spd, dist * 3.5 + 0.25)   # weich ankommen
			want = d / dist * spd
	velocity = velocity.move_toward(want, ACCEL * dt)
	velocity.y = 0
	move_and_slide()
	position.y = 0
	# Haengt er an einem Moebel fest, gibt er auf statt zu zappeln.
	if target != null and get_real_velocity().length() < 0.15 and want.length() > 0.3:
		_stuck_t += dt
		if _stuck_t > 0.4:
			target = null
	else:
		_stuck_t = 0.0
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
