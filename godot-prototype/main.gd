extends Node
## A continuous cottage, garden and coast, with touch controls and a following camera.

const WorldScript = preload("res://world.gd")
const FennecScript = preload("res://fennec.gd")
const StickScript = preload("res://touch_stick.gd")
const PITCH = deg_to_rad(49.0)
var container: SubViewportContainer
var sub: SubViewport
var landscape: Node3D
var cam: Camera3D
var fennec: CharacterBody3D
var marker: MeshInstance3D
var place_label: Label
var stick: Control
var map_panel: PanelContainer
var yaw := deg_to_rad(-18.0)
var yaw_goal := yaw
var zoom := 11.0
var zoom_factor := 1.0
var focus := Vector3.ZERO
var touches := {}
var last_tap_ms := 0
var last_tap_pos := Vector2.ZERO
var pinch_start := 0.0
var zoom_start := 1.0
var stick_finger := -1
var _capture := false
var action_row: HBoxContainer
var help_label: Label
var _probe_errors := 0

func _ready() -> void:
	container = SubViewportContainer.new()
	container.stretch = true
	container.stretch_shrink = 1
	container.texture_filter = CanvasItem.TEXTURE_FILTER_LINEAR
	container.mouse_filter = Control.MOUSE_FILTER_IGNORE
	container.set_anchors_preset(Control.PRESET_FULL_RECT)
	add_child(container)
	sub = SubViewport.new()
	sub.render_target_update_mode = SubViewport.UPDATE_ALWAYS
	sub.positional_shadow_atlas_size = 2048
	sub.msaa_3d = Viewport.MSAA_2X
	container.add_child(sub)
	var world := Node3D.new()
	sub.add_child(world)
	_environment(world)
	landscape = WorldScript.new()
	world.add_child(landscape)
	cam = Camera3D.new()
	cam.fov = 42.0
	cam.far = 160
	world.add_child(cam)
	fennec = FennecScript.new()
	world.add_child(fennec)
	fennec.setup(landscape.room.blob_material())
	fennec.cam = cam
	fennec.landscape = landscape
	fennec.position = WorldScript.LANDMARKS["Wohnzimmer"]
	focus = fennec.position
	marker = MeshInstance3D.new()
	var tm := TorusMesh.new()
	tm.inner_radius = .12; tm.outer_radius = .16; tm.rings = 12; tm.ring_segments = 4
	marker.mesh = tm
	var mm := StandardMaterial3D.new()
	mm.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	mm.albedo_color = Color(1.0,.88,.47)
	marker.material_override = mm
	marker.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	marker.scale = Vector3(1,.2,1); marker.visible = false
	world.add_child(marker)
	_hud()
	var args := OS.get_cmdline_user_args()
	_capture = "--probe" in args
	if "--gallery" in args: _run_gallery(); return
	if _capture or "--validate" in args: _run_probe()

func _environment(world: Node3D) -> void:
	var env := Environment.new()
	var sky := Sky.new()
	var material := ProceduralSkyMaterial.new()
	material.sky_top_color = Color(.24,.48,.62)
	material.sky_horizon_color = Color(.81,.87,.78)
	material.ground_horizon_color = material.sky_horizon_color
	material.ground_bottom_color = Color(.24,.38,.25)
	sky.sky_material = material
	env.sky = sky; env.background_mode = Environment.BG_SKY
	env.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	env.ambient_light_color = Color(.84,.86,.78)
	env.ambient_light_energy = .60
	env.tonemap_mode = Environment.TONE_MAPPER_LINEAR
	env.tonemap_exposure = .90
	var we := WorldEnvironment.new(); we.environment = env; world.add_child(we)
	var sun := DirectionalLight3D.new()
	sun.light_color = Color(1,.86,.65); sun.light_energy = .78
	sun.shadow_enabled = true
	sun.directional_shadow_mode = DirectionalLight3D.SHADOW_PARALLEL_2_SPLITS
	sun.directional_shadow_max_distance = 42
	sun.shadow_bias = .035
	world.add_child(sun)
	sun.look_at_from_position(Vector3(-15,18,-25),Vector3(-4,0,1),Vector3.UP)

func _button(text: String, action: Callable, row: HBoxContainer) -> Button:
	var b := Button.new(); b.text = text; b.custom_minimum_size = Vector2(76,54)
	b.add_theme_font_size_override("font_size",20)
	var normal := StyleBoxFlat.new(); normal.bg_color = Color(.13,.20,.18,.85)
	normal.corner_radius_top_left = 12; normal.corner_radius_top_right = 12
	normal.corner_radius_bottom_left = 12; normal.corner_radius_bottom_right = 12
	normal.content_margin_left = 18; normal.content_margin_right = 18
	b.add_theme_stylebox_override("normal",normal)
	b.pressed.connect(action); row.add_child(b); return b

func _hud() -> void:
	var layer := CanvasLayer.new(); add_child(layer)
	var row := HBoxContainer.new(); row.position = Vector2(18,16)
	row.add_theme_constant_override("separation",8); layer.add_child(row)
	_button("↶",func(): yaw_goal -= PI/4,row)
	_button("↷",func(): yaw_goal += PI/4,row)
	_button("Orte",func(): map_panel.visible = not map_panel.visible,row)
	_button("Kamera",func(): zoom_factor = 1.0; yaw_goal = deg_to_rad(-18),row)
	place_label = Label.new(); place_label.add_theme_font_size_override("font_size",23)
	place_label.add_theme_constant_override("outline_size",5)
	place_label.add_theme_color_override("font_outline_color",Color(.08,.14,.13))
	row.add_child(place_label)
	map_panel = PanelContainer.new(); map_panel.position = Vector2(18,84); map_panel.visible = false
	layer.add_child(map_panel)
	var locations := VBoxContainer.new(); map_panel.add_child(locations)
	var label := Label.new(); label.text = "Dorthin gehen"; locations.add_child(label)
	for name in WorldScript.LANDMARKS:
		var destination: String = name
		var r := HBoxContainer.new(); locations.add_child(r)
		_button(destination,func():
			fennec.go(WorldScript.LANDMARKS[destination],true)
			map_panel.hide(),r)
	stick = StickScript.new(); layer.add_child(stick)
	stick.mouse_filter = Control.MOUSE_FILTER_IGNORE
	stick.set_anchors_preset(Control.PRESET_BOTTOM_LEFT)
	stick.position = Vector2(112,-106); stick.size = Vector2.ZERO
	var actions := HBoxContainer.new(); layer.add_child(actions)
	action_row = actions
	actions.set_anchors_preset(Control.PRESET_BOTTOM_RIGHT)
	actions.position = Vector2(-262,-86)
	_button("Sitzen",func():
		fennec.target = null
		fennec.seated = not fennec.seated,actions)
	_button("Hüpfen",func(): fennec.jump(),actions)
	var help := Label.new(); help_label = help; help.text = "Tippen: gehen · Doppeltipp: laufen · Zwei Finger: Kamera"
	help.add_theme_font_size_override("font_size",16)
	help.add_theme_color_override("font_color",Color(1,.96,.84,.9))
	help.add_theme_color_override("font_outline_color",Color(.12,.15,.12))
	help.add_theme_constant_override("outline_size",4)
	help.set_anchors_preset(Control.PRESET_BOTTOM_LEFT)
	help.position = Vector2(210,-32); layer.add_child(help)

func _process(dt: float) -> void:
	if fennec == null: return
	if stick_finger < 0:
		fennec.manual = Vector2(float(Input.is_physical_key_pressed(KEY_D) or Input.is_physical_key_pressed(KEY_RIGHT))-float(Input.is_physical_key_pressed(KEY_A) or Input.is_physical_key_pressed(KEY_LEFT)),
			float(Input.is_physical_key_pressed(KEY_S) or Input.is_physical_key_pressed(KEY_DOWN))-float(Input.is_physical_key_pressed(KEY_W) or Input.is_physical_key_pressed(KEY_UP))).limit_length()
		if fennec.manual.length() > .1: fennec.running = Input.is_physical_key_pressed(KEY_SHIFT)
	yaw = lerp_angle(yaw,yaw_goal,1-exp(-dt*6))
	var inside: bool = landscape.is_inside(fennec.position)
	var base_distance := 11.0 if inside else 16.0
	zoom = lerpf(zoom,base_distance*zoom_factor,1-exp(-dt*2.5))
	# The focus includes the whole room; outdoors it anticipates the walking direction.
	var wanted: Vector3 = WorldScript.HOUSE + Vector3(0,0,0) if inside else fennec.position+fennec.facing*.6
	focus = focus.lerp(wanted,1-exp(-dt*4))
	var offset := Vector3(sin(yaw)*cos(PITCH),sin(PITCH),cos(yaw)*cos(PITCH))*zoom
	var look := focus+Vector3(0,.6,0)
	cam.look_at_from_position(look+offset,look,Vector3.UP)
	landscape.update_visibility(cam.global_position,fennec.position,dt)
	marker.visible = fennec.target != null
	if marker.visible:
		marker.position = fennec.target
		marker.position.y = (.18 if landscape.is_inside(marker.position) else landscape.ground_height(marker.position.x,marker.position.z))+.025
	place_label.text = "  " + landscape.place_name(fennec.position)
	if fennec.blocked: place_label.text += " · Weg versperrt"
	var window := get_viewport().get_visible_rect().size
	stick.position = Vector2(112,window.y-106)
	action_row.position = Vector2(window.x-262,window.y-86)
	help_label.position = Vector2(210,window.y-32)
	stick.direction = fennec.manual; stick.queue_redraw()

func _floor_point(screen: Vector2):
	var p := screen/float(container.stretch_shrink)
	var o := cam.project_ray_origin(p); var n := cam.project_ray_normal(p)
	var query := PhysicsRayQueryParameters3D.create(o,o+n*160,1)
	query.exclude = landscape.room.picking_exclusions(cam.global_position-WorldScript.HOUSE,landscape.house_view_open)
	var hit := sub.find_world_3d().direct_space_state.intersect_ray(query)
	return hit.position if not hit.is_empty() else null

func _on_fennec(screen: Vector2) -> bool:
	var mid := fennec.global_position+Vector3(0,.45,0)
	if cam.is_position_behind(mid): return false
	var center := cam.unproject_position(mid)
	var top := cam.unproject_position(mid+Vector3(0,.5,0))
	return screen.distance_to(center) < maxf(18,center.distance_to(top))

func tap(screen: Vector2) -> void:
	if _on_fennec(screen): fennec.joy(); return
	var hit = _floor_point(screen)
	if hit == null: return
	var now := Time.get_ticks_msec()
	var double := now-last_tap_ms < 320 and screen.distance_to(last_tap_pos) < 60
	last_tap_ms = now; last_tap_pos = screen
	fennec.go(hit,double)

func _input(event: InputEvent) -> void:
	if event is InputEventKey and event.pressed and event.physical_keycode == KEY_SPACE:
		fennec.jump(); get_viewport().set_input_as_handled()
	if event is InputEventScreenTouch:
		if event.pressed and event.position.distance_to(stick.global_position) < 86 and stick_finger < 0:
			stick_finger = event.index
			fennec.manual = ((event.position-stick.global_position)/64).limit_length()
			get_viewport().set_input_as_handled()
		elif not event.pressed and event.index == stick_finger:
			stick_finger = -1; fennec.manual = Vector2.ZERO; get_viewport().set_input_as_handled()
	elif event is InputEventScreenDrag and event.index == stick_finger:
		fennec.manual = ((event.position-stick.global_position)/64).limit_length()
		fennec.running = fennec.manual.length() > .92
		get_viewport().set_input_as_handled()

func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventScreenTouch:
		if event.pressed:
			touches[event.index] = event.position
			if touches.size() == 1: tap(event.position)
			elif touches.size() == 2:
				var pts: Array = touches.values()
				pinch_start = pts[0].distance_to(pts[1]); zoom_start = zoom_factor
				fennec.target = null
		else: touches.erase(event.index)
	elif event is InputEventScreenDrag:
		var prev: Vector2 = touches.get(event.index,event.position)
		touches[event.index] = event.position
		if touches.size() == 1:
			var hit = _floor_point(event.position)
			if hit != null: fennec.go(hit,fennec.running)
		elif touches.size() == 2:
			var pts: Array = touches.values(); var d: float = pts[0].distance_to(pts[1])
			if pinch_start > 1: zoom_factor = clampf(zoom_start*pinch_start/maxf(d,1),.65,1.6)
			yaw_goal -= (event.position.x-prev.x)*.003
	elif event is InputEventMouseButton and event.pressed:
		if event.button_index == MOUSE_BUTTON_WHEEL_UP: zoom_factor = maxf(.65,zoom_factor*.9)
		elif event.button_index == MOUSE_BUTTON_WHEEL_DOWN: zoom_factor = minf(1.6,zoom_factor*1.1)

func _shot(name: String) -> void:
	if not _capture: return
	await RenderingServer.frame_post_draw
	var path := OS.get_user_data_dir().path_join("world_%s.png" % name)
	get_viewport().get_texture().get_image().save_png(path)
	print("SCREENSHOT ",path)

func _check(condition: bool, detail: String) -> void:
	if not condition:
		_probe_errors += 1
		push_error("CHECK FAILED: "+detail)
	else: print("CHECK OK: ",detail)

func _walk_to(name: String) -> void:
	var destination: Vector3 = WorldScript.LANDMARKS[name]
	_check(fennec.go(destination,true),"route to "+name)
	var elapsed := 0.0
	while fennec.target != null and elapsed < 25:
		await get_tree().physics_frame
		elapsed += 1.0/Engine.physics_ticks_per_second
	var distance := Vector2(fennec.position.x-destination.x,fennec.position.z-destination.z).length()
	_check(distance < .4,"arrival "+name+" distance="+str(distance))
	_check(not fennec.blocked,"unobstructed "+name)
	_check(fennec.position.y > -1.0,"ground below "+name)
	await get_tree().create_timer(.6).timeout
	await _shot(name)

func _run_probe() -> void:
	await get_tree().create_timer(.8).timeout
	_check(NavigationServer3D.map_get_iteration_id(sub.find_world_3d().navigation_map)>0,"navigation synchronized")
	var floor_hit = _floor_point(cam.unproject_position(WorldScript.LANDMARKS["Wohnzimmer"]))
	_check(floor_hit != null and floor_hit.distance_to(WorldScript.LANDMARKS["Wohnzimmer"])<.12,"floor tap passes through cut-away wall")
	await _shot("Wohnzimmer_start")
	yaw_goal += PI/2
	await get_tree().create_timer(.8).timeout
	await _shot("Wohnzimmer_gedreht")
	yaw_goal -= PI/2
	for name in ["Haustür","Garten","Strand","Wasser"]: await _walk_to(name)
	_check(fennec.wading,"walkable water and wading speed")
	_check(landscape.water_depth(fennec.position)>.4,"shore slopes below water")
	yaw_goal += PI*.85
	await get_tree().create_timer(1.0).timeout
	await _shot("Wasser_Rueckblick")
	await _walk_to("Wohnzimmer")
	_check(landscape.is_inside(fennec.position),"return through same doorway")
	fennec.jump()
	await get_tree().create_timer(.20).timeout
	_check(fennec.position.y > .4,"jump has actual height")
	await get_tree().create_timer(.8).timeout
	_check(absf(fennec.position.y-.18)<.03,"jump lands on room floor")
	# Feed actual screen events through the same input pipeline used on Android.
	var before := fennec.position
	var touch := InputEventScreenTouch.new(); touch.index = 7; touch.pressed = true
	touch.position = stick.global_position+Vector2(45,0)
	_probe_input(touch)
	await get_tree().create_timer(.3).timeout
	_check(stick_finger == 7,"touch joystick captures finger")
	_check(fennec.position.distance_to(before)>.2,"touch joystick moves in world")
	var release := InputEventScreenTouch.new(); release.index = 7; release.position = touch.position; release.pressed = false
	_probe_input(release)
	await get_tree().physics_frame
	_check(stick_finger < 0 and fennec.manual == Vector2.ZERO,"touch joystick releases finger")
	_check(stick.global_position.y>0 and action_row.position.y>0,"mobile controls inside viewport")
	print("WORLD VALIDATION errors=",_probe_errors)
	get_tree().quit(0 if _probe_errors == 0 else 1)

func _run_gallery() -> void:
	_capture = true
	await get_tree().create_timer(1.0).timeout
	await _shot("Wohnzimmer_start")
	yaw_goal += PI/2
	await get_tree().create_timer(1.0).timeout
	await _shot("Wohnzimmer_gedreht")
	for name in ["Garten","Strand","Wasser"]:
		fennec.target = null
		fennec.position = WorldScript.LANDMARKS[name]
		yaw_goal = deg_to_rad(-18)
		await get_tree().create_timer(2.0).timeout
		await _shot(name)
	get_tree().quit()

func _probe_input(event: InputEvent) -> void:
	if DisplayServer.get_name() == "headless":
		# The dummy display has no focused window to dispatch touchscreen events.
		_input(event)
	else:
		get_viewport().push_input(event)
