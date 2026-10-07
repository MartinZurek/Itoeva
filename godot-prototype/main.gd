extends Node
## Itoeva Godot-Prototyp: ein begehbares Wohnzimmer in 3D, Fennec als Billboard, folgende Kamera.
##
## Die 3D-Welt rendert in ein kleineres SubViewport (Standard: 1/2 der Bildflaeche in
## Basiseinheiten, also ~640x360) und wird pixelgenau hochskaliert - so passt der Raum zur
## Pixel-Art der Wesen. Der Knopf "Pixel" schaltet zum Vergleich auf volle Aufloesung.
##
## Steuerung: Tippen/Halten = hingehen (Finger folgt), Doppeltipp = rennen, auf Fennec tippen =
## Freudensprung, zwei Finger = Kamera drehen (seitlich wischen) und zoomen (spreizen).

const RoomScript := preload("res://room.gd")
const FennecScript := preload("res://fennec.gd")

const PITCH := deg_to_rad(36.0)
const ZOOM_MIN := 2.6
const ZOOM_MAX := 9.0

var container: SubViewportContainer
var sub: SubViewport
var room: Node3D
var cam: Camera3D
var fennec: CharacterBody3D
var marker: MeshInstance3D
var fps_label: Label
var pixel_button: Button

var yaw := deg_to_rad(-18.0)
var yaw_goal := yaw
var zoom := 4.6
var focus := Vector3.ZERO
var touches := {}
var last_tap_ms := 0
var last_tap_pos := Vector2.ZERO
var pinch_start := 0.0
var zoom_start := 0.0
var _probe := false
var _frame_ms := []


func _ready() -> void:
	container = SubViewportContainer.new()
	container.stretch = true
	container.stretch_shrink = 2
	container.texture_filter = CanvasItem.TEXTURE_FILTER_NEAREST
	container.mouse_filter = Control.MOUSE_FILTER_IGNORE
	container.set_anchors_preset(Control.PRESET_FULL_RECT)
	add_child(container)
	sub = SubViewport.new()
	sub.render_target_update_mode = SubViewport.UPDATE_ALWAYS
	sub.positional_shadow_atlas_size = 1024
	container.add_child(sub)

	var world := Node3D.new()
	sub.add_child(world)
	_environment(world)
	room = RoomScript.new()
	world.add_child(room)

	cam = Camera3D.new()
	cam.fov = 42.0
	world.add_child(cam)

	fennec = FennecScript.new()
	world.add_child(fennec)
	fennec.setup(room.blob_material())
	fennec.cam = cam
	fennec.position = Vector3(0.6, 0, 1.2)
	focus = fennec.position

	marker = MeshInstance3D.new()
	var tm := TorusMesh.new()
	tm.inner_radius = 0.12
	tm.outer_radius = 0.16
	tm.rings = 12
	tm.ring_segments = 4
	marker.mesh = tm
	var mm := StandardMaterial3D.new()
	mm.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	mm.albedo_color = Color(1.0, 0.9, 0.55, 0.8)
	mm.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	marker.material_override = mm
	marker.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	marker.scale = Vector3(1, 0.2, 1)
	marker.visible = false
	world.add_child(marker)

	_hud()
	_probe = "--probe" in OS.get_cmdline_user_args()
	if _probe:
		_run_probe()


func _environment(world: Node3D) -> void:
	var env := Environment.new()
	env.background_mode = Environment.BG_COLOR
	env.background_color = Color(0.11, 0.08, 0.07)
	env.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	env.ambient_light_color = Color(0.72, 0.64, 0.6)
	env.ambient_light_energy = 0.5
	env.tonemap_mode = Environment.TONE_MAPPER_FILMIC
	env.tonemap_exposure = 1.05
	env.glow_enabled = true
	env.glow_intensity = 0.6
	env.glow_bloom = 0.05
	var we := WorldEnvironment.new()
	we.environment = env
	world.add_child(we)

	# Spaete Nachmittagssonne, faellt schraeg durchs Fenster der Rueckwand
	var sun := DirectionalLight3D.new()
	sun.light_color = Color(1.0, 0.78, 0.52)
	sun.light_energy = 2.2
	sun.shadow_enabled = true
	sun.directional_shadow_mode = DirectionalLight3D.SHADOW_ORTHOGONAL
	sun.directional_shadow_max_distance = 20.0
	world.add_child(sun)
	sun.look_at_from_position(Vector3(-3.5, 6.0, -10.0), Vector3(0.5, 0, 0), Vector3.UP)


func _hud() -> void:
	var layer := CanvasLayer.new()
	add_child(layer)
	var box := HBoxContainer.new()
	box.position = Vector2(16, 12)
	box.add_theme_constant_override("separation", 10)
	layer.add_child(box)
	for spec in [["⟲", -1], ["⟳", 1]]:
		var b := Button.new()
		b.text = spec[0]
		b.custom_minimum_size = Vector2(72, 56)
		b.add_theme_font_size_override("font_size", 30)
		var dir: int = spec[1]
		b.pressed.connect(func(): yaw_goal += dir * deg_to_rad(45.0))
		box.add_child(b)
	pixel_button = Button.new()
	pixel_button.text = "Pixel: an"
	pixel_button.custom_minimum_size = Vector2(150, 56)
	pixel_button.add_theme_font_size_override("font_size", 24)
	pixel_button.pressed.connect(_toggle_pixel)
	box.add_child(pixel_button)
	fps_label = Label.new()
	fps_label.add_theme_font_size_override("font_size", 20)
	fps_label.add_theme_color_override("font_color", Color(1, 0.95, 0.85))
	fps_label.add_theme_color_override("font_outline_color", Color(0.1, 0.05, 0.03))
	fps_label.add_theme_constant_override("outline_size", 6)
	box.add_child(fps_label)
	var help := Label.new()
	help.text = "Tippen: gehen · Doppeltipp: rennen · Fennec antippen: Freude · 2 Finger: drehen/zoomen"
	help.add_theme_font_size_override("font_size", 18)
	help.add_theme_color_override("font_color", Color(1, 0.95, 0.85, 0.85))
	help.add_theme_color_override("font_outline_color", Color(0.1, 0.05, 0.03))
	help.add_theme_constant_override("outline_size", 5)
	help.set_anchors_preset(Control.PRESET_BOTTOM_LEFT)
	help.position = Vector2(16, -40)
	layer.add_child(help)
	help.anchor_top = 1.0
	help.anchor_bottom = 1.0
	help.offset_top = -40
	help.offset_left = 16


func _toggle_pixel() -> void:
	container.stretch_shrink = 1 if container.stretch_shrink == 2 else 2
	pixel_button.text = "Pixel: an" if container.stretch_shrink == 2 else "Pixel: aus"


func _process(dt: float) -> void:
	yaw = lerp_angle(yaw, yaw_goal, 1.0 - exp(-dt * 6.0))
	focus = focus.lerp(fennec.position, 1.0 - exp(-dt * 4.0))
	var offset := Vector3(sin(yaw) * cos(PITCH), sin(PITCH), cos(yaw) * cos(PITCH)) * zoom
	var look := focus + Vector3(0, 0.55, 0)
	cam.look_at_from_position(look + offset, look, Vector3.UP)
	room.cut_away(cam.global_position)
	if fennec.target == null:
		marker.visible = false
	else:
		marker.position = fennec.target + Vector3(0, 0.02, 0)
		var s := 1.0 + 0.12 * sin(Time.get_ticks_msec() / 160.0)
		marker.scale = Vector3(s, 0.2, s)
	_frame_ms.append(dt * 1000.0)
	if _frame_ms.size() > 60:
		_frame_ms.pop_front()
	if Engine.get_process_frames() % 15 == 0:
		var worst: float = _frame_ms.max()
		fps_label.text = "%d FPS  (schlechtestes Bild %.0f ms)  3D %dx%d" % [
			Engine.get_frames_per_second(), worst, sub.size.x, sub.size.y]


# --- Eingabe ----------------------------------------------------------------------------

func _floor_point(screen: Vector2):
	var p := screen / float(container.stretch_shrink)
	var o := cam.project_ray_origin(p)
	var n := cam.project_ray_normal(p)
	if abs(n.y) < 1e-4:
		return null
	var t := -o.y / n.y
	if t < 0:
		return null
	var hit := o + n * t
	hit.x = clamp(hit.x, -RoomScript.W / 2.0 + 0.3, RoomScript.W / 2.0 - 0.3)
	hit.z = clamp(hit.z, -RoomScript.D / 2.0 + 0.3, RoomScript.D / 2.0 - 0.3)
	return hit


func _on_fennec(screen: Vector2) -> bool:
	var p := screen / float(container.stretch_shrink)
	var mid := fennec.global_position + Vector3(0, 0.45, 0)
	if cam.is_position_behind(mid):
		return false
	var c := cam.unproject_position(mid)
	var top := cam.unproject_position(mid + Vector3(0, 0.5, 0))
	var r: float = max(18.0, c.distance_to(top))
	return p.distance_to(c) < r


func tap(screen: Vector2) -> void:
	var now := Time.get_ticks_msec()
	if _on_fennec(screen):
		fennec.joy()
		return
	var hit = _floor_point(screen)
	if hit == null:
		return
	var double := now - last_tap_ms < 320 and screen.distance_to(last_tap_pos) < 60
	last_tap_ms = now
	last_tap_pos = screen
	fennec.go(hit, double or fennec.running and fennec.target != null)
	marker.visible = true


func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventScreenTouch:
		if event.pressed:
			touches[event.index] = event.position
			if touches.size() == 1:
				tap(event.position)
			elif touches.size() == 2:
				var pts: Array = touches.values()
				pinch_start = (pts[0] as Vector2).distance_to(pts[1])
				zoom_start = zoom
		else:
			touches.erase(event.index)
	elif event is InputEventScreenDrag:
		var prev: Vector2 = touches.get(event.index, event.position)
		touches[event.index] = event.position
		if touches.size() == 1:
			# Halten und ziehen: Fennec folgt dem Finger
			if not _on_fennec(event.position):
				var hit = _floor_point(event.position)
				if hit != null:
					fennec.go(hit, fennec.running)
					marker.visible = true
		elif touches.size() == 2:
			var pts: Array = touches.values()
			var d := (pts[0] as Vector2).distance_to(pts[1])
			if pinch_start > 1.0:
				zoom = clamp(zoom_start * pinch_start / max(d, 1.0), ZOOM_MIN, ZOOM_MAX)
			yaw_goal -= (event.position.x - prev.x) * 0.5 * 0.006
	elif event is InputEventMouseButton and event.pressed:
		if event.button_index == MOUSE_BUTTON_WHEEL_UP:
			zoom = clamp(zoom * 0.9, ZOOM_MIN, ZOOM_MAX)
		elif event.button_index == MOUSE_BUTTON_WHEEL_DOWN:
			zoom = clamp(zoom * 1.1, ZOOM_MIN, ZOOM_MAX)


# --- Probelauf (nur Rechner): Bilder fuer die Pruefung ohne Handy -----------------------
# godot --path godot-prototype -- --probe   -> schreibt user://probe_*.png

func _shot(name: String) -> void:
	await RenderingServer.frame_post_draw
	var img := get_viewport().get_texture().get_image()
	var path := OS.get_user_data_dir().path_join("probe_%s.png" % name)
	img.save_png(path)
	print("PROBE ", path, " fennec=", fennec.position, " frame=", fennec.sprite.frame)


func _wait(s: float) -> void:
	await get_tree().create_timer(s).timeout


func _run_probe() -> void:
	await _wait(0.6)
	await _shot("1_start")
	fennec.go(Vector3(-2.5, 0, 0.8), false)
	await _wait(0.9)
	await _shot("2_walk_side")
	fennec.go(Vector3(-0.8, 0, 2.6), false)
	await _wait(0.9)
	await _shot("3_walk_front")
	fennec.go(Vector3(1.5, 0, -2.2), true)
	await _wait(0.7)
	await _shot("4_run_back")
	await _wait(2.0)
	await _shot("5_sunpatch_idle")
	yaw_goal += deg_to_rad(90.0)
	await _wait(1.5)
	await _shot("6_rotated")
	fennec.joy()
	await _wait(0.33)
	await _shot("7_joy")
	_toggle_pixel()
	await _wait(0.3)
	await _shot("8_sharp")
	get_tree().quit()
