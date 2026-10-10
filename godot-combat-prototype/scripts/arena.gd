extends Node3D

const CombatClock = preload("res://scripts/combat_clock.gd")
const Fennec = preload("res://scripts/fennec.gd")
const Telemetry = preload("res://scripts/telemetry.gd")
const LANDSCAPE: Texture2D = preload("res://assets/forest-panorama.png")
const FLOOR: Texture2D = preload("res://assets/forest-floor.jpg")
const HOME := Vector3(-2.4, 0.02, 0.4)
const TARGET := Vector3(2.7, 0.0, -0.25)
const POOL_SIZE := 36
var clock := CombatClock.new()
var telemetry := Telemetry.new()
var actor: Fennec
var camera: Camera3D
var target: Node3D
var core: MeshInstance3D
var impact_ring: MeshInstance3D
var shadow: MeshInstance3D
var leaves: Array[MeshInstance3D] = []
var meter: Label
var status: Label
var buttons: Array[Button] = []
var demo_button: Button
var pause_button: Button
var demo := false
var paused := false
var demo_timer := 0.0
var demo_index := 0
var effect_time := 0.0
var hit_age := 10.0
var hit_kind := ""
var target_hits := 0
var ui_timer := 0.0
var stage_time := 0.0
var validated := false
var snapshot_nodes := 0
var snapshot_texture: RID
var probe := false
var benchmarking := false
var benchmark_age := 0.0
var captured := {}
var frame_index := 0
var first_frame_ms := 0

func _ready() -> void:
	var args := OS.get_cmdline_user_args()
	probe = "--probe" in args
	benchmarking = "--benchmark" in args
	_build_stage()
	_build_actor()
	_build_effects()
	_build_ui()
	snapshot_nodes = int(Performance.get_monitor(Performance.OBJECT_NODE_COUNT))
	snapshot_texture = Fennec.ATLAS.get_rid()
	_mark_first_frame.call_deferred()
	if "--validate" in args:
		_validate.call_deferred()
	elif probe or benchmarking:
		demo = true
		demo_index = 1
		demo_button.text = "Demo stoppen"
		start_action("wind")

func _material(color: Color, unshaded := false) -> StandardMaterial3D:
	var mat := StandardMaterial3D.new()
	mat.albedo_color = color
	mat.roughness = 0.98
	if unshaded:
		mat.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	return mat

func _mesh(mesh: Mesh, material: Material, pos: Vector3, parent: Node3D = self) -> MeshInstance3D:
	var node := MeshInstance3D.new()
	node.mesh = mesh
	node.material_override = material
	node.position = pos
	parent.add_child(node)
	return node

func _build_stage() -> void:
	var environment := WorldEnvironment.new()
	var settings := Environment.new()
	settings.background_mode = Environment.BG_COLOR
	settings.background_color = Color("52694d")
	settings.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	settings.ambient_light_color = Color(1.0, 0.96, 0.86)
	settings.ambient_light_energy = 0.70
	environment.environment = settings
	add_child(environment)
	var sun := DirectionalLight3D.new()
	sun.light_color = Color(1.0, 0.88, 0.68)
	sun.light_energy = 0.28
	sun.rotation_degrees = Vector3(-55, -32, 0)
	# Gemalter Boden und Kontaktschatten brauchen keine teure Echtzeit-Schattenkarte.
	sun.shadow_enabled = false
	add_child(sun)
	var backdrop := QuadMesh.new()
	backdrop.size = Vector2(30.0, 10.0)
	var painted := _material(Color.WHITE, true)
	painted.albedo_texture = LANDSCAPE
	painted.cull_mode = BaseMaterial3D.CULL_DISABLED
	var back := _mesh(backdrop, painted, Vector3(0, 4.0, -4.8))
	back.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	var ground := PlaneMesh.new()
	ground.size = Vector2(24, 14)
	var ground_material := _material(Color(0.75, 0.79, 0.75), true)
	ground_material.albedo_texture = FLOOR
	ground_material.uv1_scale = Vector3(4.0, 2.4, 1.0)
	_mesh(ground, ground_material, Vector3(0, -0.025, 1.8))
	# Dreidimensionale Requisiten und Tiefentest geben der gemalten Kulisse Boden und Verdeckung.
	for i in 9:
		var rock := SphereMesh.new()
		rock.radius = 0.30 + float(i % 3) * 0.10
		rock.height = rock.radius * 1.15
		rock.radial_segments = 9
		rock.rings = 5
		var pos := Vector3(-6.5 + float(i) * 1.7, 0.0, -2.8 if i % 2 == 0 else 3.0)
		var stone := _mesh(rock, _material(Color("75755e")), pos)
		stone.rotation_degrees.y = float(i * 39)
	target = Node3D.new()
	target.position = TARGET
	add_child(target)
	var pedestal := CylinderMesh.new()
	pedestal.top_radius = 0.74
	pedestal.bottom_radius = 0.92
	pedestal.height = 0.32
	pedestal.radial_segments = 16
	_mesh(pedestal, _material(Color("686d51")), Vector3(0, 0.16, 0), target)
	var rune := PrismMesh.new()
	rune.size = Vector3(0.8, 1.8, 0.62)
	var stone_material := _material(Color("b9bd9b"))
	stone_material.albedo_texture = FLOOR
	stone_material.uv1_triplanar = true
	stone_material.uv1_scale = Vector3.ONE * 0.6
	core = _mesh(rune, stone_material, Vector3(0, 1.2, 0), target)
	core.rotation_degrees.y = 18.0
	var diamond := PrismMesh.new()
	diamond.size = Vector3(0.21, 0.37, 0.12)
	_mesh(diamond, _material(Color("e5c789"), true), Vector3(0, 1.4, 0.40), target)
	camera = Camera3D.new()
	camera.position = Vector3(0.0, 5.8, 12.8)
	camera.fov = 43.0
	add_child(camera)
	camera.look_at(Vector3(0, 1.35, -0.3))
	camera.current = true

func _build_actor() -> void:
	actor = Fennec.new()
	actor.position = HOME
	add_child(actor)
	var disk := CylinderMesh.new()
	disk.top_radius = 0.55
	disk.bottom_radius = 0.55
	disk.height = 0.005
	disk.radial_segments = 32
	var mat := _material(Color(0.12, 0.12, 0.07, 0.32), true)
	mat.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	shadow = _mesh(disk, mat, HOME + Vector3(0, -0.007, 0))
	shadow.scale.z = 0.55
	shadow.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF

func _build_effects() -> void:
	# Begrenzter Pool: keine neuen Nodes oder Texturen waehrend einer Aktion.
	var leaf := PrismMesh.new()
	leaf.size = Vector3(0.22, 0.045, 0.10)
	var mat := _material(Color("e7ba60"), true)
	for i in POOL_SIZE:
		var node := _mesh(leaf, mat, Vector3.ZERO)
		node.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
		node.visible = false
		leaves.append(node)
	var ring := TorusMesh.new()
	ring.inner_radius = 0.48
	ring.outer_radius = 0.54
	ring.rings = 16
	ring.ring_segments = 12
	impact_ring = _mesh(ring, _material(Color("f3d99d"), true), TARGET + Vector3(0, 0.7, 0.08))
	impact_ring.rotation_degrees.x = 90
	impact_ring.visible = false
	impact_ring.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF

func _panel() -> StyleBoxFlat:
	var style := StyleBoxFlat.new()
	style.bg_color = Color(0.08, 0.13, 0.11, 0.92)
	style.border_color = Color(0.66, 0.55, 0.35, 0.6)
	style.set_border_width_all(1)
	style.set_corner_radius_all(14)
	style.content_margin_left = 18
	style.content_margin_right = 18
	style.content_margin_top = 12
	style.content_margin_bottom = 12
	return style

func _build_ui() -> void:
	var canvas := CanvasLayer.new()
	add_child(canvas)
	var root := Control.new()
	root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	root.mouse_filter = Control.MOUSE_FILTER_IGNORE
	canvas.add_child(root)
	var top := PanelContainer.new()
	top.position = Vector2(24, 20)
	top.add_theme_stylebox_override("panel", _panel())
	root.add_child(top)
	var header := VBoxContainer.new()
	top.add_child(header)
	var title := Label.new()
	title.text = "ITOEVA  /  GODOT-KAMPFSTUDIE"
	title.add_theme_font_size_override("font_size", 18)
	title.add_theme_color_override("font_color", Color("e4c994"))
	header.add_child(title)
	status = Label.new()
	status.text = "Fennec · gemalte Figur, räumliche Bühne"
	status.add_theme_font_size_override("font_size", 22)
	header.add_child(status)
	meter = Label.new()
	meter.add_theme_font_size_override("font_size", 16)
	header.add_child(meter)
	var bottom := PanelContainer.new()
	bottom.set_anchors_and_offsets_preset(Control.PRESET_BOTTOM_WIDE)
	bottom.offset_left = 24
	bottom.offset_right = -24
	bottom.offset_top = -118
	bottom.offset_bottom = -20
	bottom.add_theme_stylebox_override("panel", _panel())
	root.add_child(bottom)
	var column := VBoxContainer.new()
	bottom.add_child(column)
	var row := HBoxContainer.new()
	row.add_theme_constant_override("separation", 10)
	column.add_child(row)
	for spec in [["Waldwind", "wind"], ["Vorstoß", "rush"], ["Ausweichen", "dodge"]]:
		var button := _button(spec[0], row)
		button.pressed.connect(func(): start_action(spec[1]))
		buttons.append(button)
	demo_button = _button("Demo starten", row)
	demo_button.pressed.connect(func():
		demo = not demo
		demo_button.text = "Demo stoppen" if demo else "Demo starten"
		demo_timer = 0.0
	)
	pause_button = _button("Pause", row)
	pause_button.pressed.connect(_toggle_pause)
	var help := Label.new()
	help.text = "Trainingsstein · 1 / 2 / 3: Aktionen  ·  Leertaste: Pause  ·  Bildzeiten: letzte 600 Bilder"
	help.add_theme_font_size_override("font_size", 15)
	help.add_theme_color_override("font_color", Color("c4c8ad"))
	column.add_child(help)
	var license_button := Button.new()
	license_button.text = "Lizenzen"
	license_button.set_anchors_and_offsets_preset(Control.PRESET_TOP_RIGHT)
	license_button.offset_left = -118
	license_button.offset_right = -24
	license_button.offset_top = 20
	license_button.offset_bottom = 55
	root.add_child(license_button)
	license_button.pressed.connect(_licenses)

func _button(text_value: String, row: HBoxContainer) -> Button:
	var button := Button.new()
	button.text = text_value
	button.custom_minimum_size = Vector2(0, 48)
	button.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	button.add_theme_font_size_override("font_size", 18)
	row.add_child(button)
	return button

func _licenses() -> void:
	var dialog := AcceptDialog.new()
	dialog.title = "Godot und Drittanbieter"
	var text := RichTextLabel.new()
	text.custom_minimum_size = Vector2(700, 400)
	text.text = Engine.get_license_text() + "\n\n" + str(Engine.get_copyright_info()) + "\n\n" + str(Engine.get_license_info())
	dialog.add_child(text)
	add_child(dialog)
	dialog.confirmed.connect(dialog.queue_free)
	dialog.canceled.connect(dialog.queue_free)
	dialog.popup_centered_ratio(0.8)

func start_action(kind: String) -> bool:
	if paused or not clock.start(kind):
		return false
	effect_time = 0.0
	status.text = {"wind": "Waldwind · sammeln, freisetzen, treffen", "rush": "Vorstoß · annähern und zurückkehren", "dodge": "Ausweichen · Schritt in die Raumtiefe"}[kind]
	return true

func _toggle_pause() -> void:
	paused = not paused
	pause_button.text = "Weiter" if paused else "Pause"

func _notification(what: int) -> void:
	if what == NOTIFICATION_APPLICATION_PAUSED:
		paused = true
		if is_instance_valid(pause_button):
			pause_button.text = "Weiter"

func _mark_first_frame() -> void:
	await RenderingServer.frame_post_draw
	first_frame_ms = Time.get_ticks_msec()
	print("Erstes gerendertes Bild seit Engine-Start: %d ms" % first_frame_ms)

func _unhandled_key_input(event: InputEvent) -> void:
	if not event.is_pressed() or event.is_echo():
		return
	if event.keycode == KEY_1:
		start_action("wind")
	elif event.keycode == KEY_2:
		start_action("rush")
	elif event.keycode == KEY_3:
		start_action("dodge")
	elif event.keycode == KEY_SPACE:
		_toggle_pause()

func _process(delta: float) -> void:
	telemetry.record(delta)
	ui_timer += delta
	if ui_timer >= 0.5:
		ui_timer = 0.0
		var report := telemetry.report()
		if probe:
			meter.text = "Aufnahme mit festem Zeitschritt · keine Handy-Messung"
		else:
			meter.text = "%d FPS · p95 %.1f ms · >33 ms: %d · Start %d ms" % [Engine.get_frames_per_second(), report.p95_ms, report.over_33ms, first_frame_ms]
	for button in buttons:
		button.disabled = paused or not clock.action.is_empty()
	if not paused:
		stage_time += delta
		var old_action: String = clock.action
		if clock.advance(delta) and old_action != "dodge":
			hit_age = 0.0
			hit_kind = old_action
			target_hits += 1
			effect_time = 0.0
		hit_age += delta
		effect_time += delta
		_update_motion()
		_update_effects(old_action)
		if demo and clock.action.is_empty():
			demo_timer += delta
			if demo_timer > 0.75:
				start_action(["wind", "rush", "dodge"][demo_index % 3])
				demo_index += 1
				demo_timer = 0.0
	if probe:
		_probe_frame()
	if benchmarking:
		benchmark_age += delta
		if benchmark_age > 20.0:
			_write_report("benchmark.json")
			get_tree().quit(0)

func _update_motion() -> void:
	actor.set_pose(clock.pose())
	var p: float = clock.progress()
	var travel: float = CombatClock.excursion(p)
	actor.position = HOME
	if clock.action == "rush":
		actor.position = HOME.lerp(TARGET + Vector3(-1.1, 0.02, 0.5), travel)
	elif clock.action == "dodge":
		actor.position.z -= travel * 1.6
		actor.position.x -= travel * 0.45
	shadow.position = Vector3(actor.position.x, 0.013, actor.position.z)
	core.rotation.z = sin(hit_age * 33.0) * 0.06 * exp(-hit_age * 9.0)
	camera.position.x = sin(hit_age * 41.0) * 0.055 * exp(-hit_age * 12.0)
	camera.look_at(Vector3(0, 1.35, -0.3))

func _update_effects(kind: String) -> void:
	impact_ring.visible = hit_age < 0.3
	impact_ring.scale = Vector3.ONE * (1.0 + hit_age * 3.0)
	for i in POOL_SIZE:
		var node := leaves[i]
		var live: bool = kind == "wind" and clock.action == "wind" and clock.elapsed > 0.48 and clock.elapsed < 1.04
		var burst := hit_age < 0.40 and hit_kind != "dodge"
		node.visible = live or burst
		if live:
			var t := clampf((clock.elapsed - 0.48) / 0.42 - float(i % 6) * 0.035, 0.0, 1.0)
			var start := HOME + Vector3(0.65, 1.15, 0.0)
			var finish := TARGET + Vector3(0, 1.25, 0.15)
			var angle: float = float(i) * 2.399 + clock.elapsed * 12.0
			node.position = start.lerp(finish, t) + Vector3(0, sin(angle), cos(angle)) * sin(t * PI) * 0.36
		elif burst:
			var angle := float(i) * 2.399
			node.position = TARGET + Vector3(cos(angle), 0.8 + sin(angle * 1.7) * 0.4, sin(angle)) * (0.3 + hit_age * 3.0)
		node.rotation = Vector3(stage_time * 7 + i, stage_time * 4 + i, stage_time * 9)

func _write_report(filename: String) -> void:
	var report: Dictionary = telemetry.report()
	report["completed_actions"] = clock.completed
	report["timeline_cues"] = clock.impacts
	report["target_hits"] = target_hits
	report["node_delta"] = int(Performance.get_monitor(Performance.OBJECT_NODE_COUNT)) - snapshot_nodes
	report["same_atlas_rid"] = Fennec.ATLAS.get_rid() == snapshot_texture
	report["pool_size"] = leaves.size()
	report["first_frame_since_engine_ms"] = first_frame_ms
	report["fixed_timestep_capture"] = probe
	var file := FileAccess.open("user://" + filename, FileAccess.WRITE)
	file.store_string(JSON.stringify(report, "\t"))
	print(JSON.stringify(report))

func _probe_frame() -> void:
	frame_index += 1
	var names := {1: "ready", 10: "anticipation", 24: "cast", 32: "release", 55: "impact", 88: "recover", 190: "rush", 300: "dodge"}
	if names.has(frame_index):
		_capture.call_deferred(names[frame_index])
	if frame_index == 380:
		_write_report("probe.json")
		get_tree().quit(0)

func _capture(label: String) -> void:
	await RenderingServer.frame_post_draw
	get_viewport().get_texture().get_image().save_png("user://" + label + ".png")

func _validate() -> void:
	var failures := 0
	for fps in [15, 30, 60, 120]:
		var test = CombatClock.new()
		for kind in ["wind", "rush", "dodge"]:
			if not test.start(kind) or test.start("wind"):
				failures += 1
			for tick in 3 * fps:
				test.advance(1.0 / float(fps))
		if test.impacts != 3 or test.completed != 3 or not test.action.is_empty():
			failures += 1
	var late = CombatClock.new()
	late.start("wind")
	late.advance(3.0)
	if late.impacts != 1 or late.completed != 1:
		failures += 1
	if CombatClock.excursion(0) != 0.0 or CombatClock.excursion(1) != 0.0:
		failures += 1
	start_action("rush")
	for tick in 100:
		clock.advance(0.02)
		_update_motion()
		_update_effects("rush")
	if actor.position.distance_to(HOME) > 0.001 or leaves.size() != POOL_SIZE:
		failures += 1
	paused = true
	if start_action("wind"):
		failures += 1
	paused = false
	if Fennec.ATLAS.get_rid() != snapshot_texture or int(Performance.get_monitor(Performance.OBJECT_NODE_COUNT)) != snapshot_nodes:
		failures += 1
	# Echte Eingabepfade und Pausenfortschritt, nicht nur die reine Uhr pruefen.
	var key := InputEventKey.new()
	key.keycode = KEY_1
	key.pressed = true
	_unhandled_key_input(key)
	if clock.action != "wind":
		failures += 1
	var frozen: float = clock.elapsed
	_toggle_pause()
	_process(0.25)
	if clock.elapsed != frozen:
		failures += 1
	_toggle_pause()
	_process(0.25)
	if clock.elapsed <= frozen:
		failures += 1
	print("Kampfzeitlinie / FPS-Unabhaengigkeit / Pool / Rueckkehr / Pause: %d Fehler" % failures)
	get_tree().quit(0 if failures == 0 else 1)
