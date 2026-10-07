extends Node
## Painted resident world. Pointer coordinates and movement share one 2D ground plane.
const WorldScript = preload("res://painted_world.gd")
const FennecScript = preload("res://fennec.gd")
const StickScript = preload("res://touch_stick.gd")
var landscape: Node2D
var fennec: Node2D
var cam: Camera2D
var stick: Control
var place_label: Label
var help_label: Label
var action_row: HBoxContainer
var map_panel: PanelContainer
var license_panel: PanelContainer
var door_button: Button
var zoom_factor := 1.0
var touches := {}
var stick_finger := -1
var pinch_start := 0.0
var zoom_start := 1.0
var gesture_zoom := false
var last_tap_ms := 0
var last_tap_pos := Vector2.ZERO
var pending_place := ""
var _errors := 0
var _capture := false
var _transition_positions: Array[Vector2] = []

func _ready() -> void:
	landscape = WorldScript.new(); add_child(landscape)
	fennec = FennecScript.new(); landscape.sorted.add_child(fennec)
	fennec.setup(landscape); fennec.position = WorldScript.LANDMARKS["Strand"]
	cam = Camera2D.new(); landscape.add_child(cam); cam.enabled = true
	cam.position = Vector2(800,362)
	landscape.access_changed.connect(_access_changed)
	_hud()
	var args := OS.get_cmdline_user_args()
	_capture = "--probe" in args
	if _capture or "--validate" in args: _run_probe()

func _button(caption: String, action: Callable, row: Container) -> Button:
	var b := Button.new(); b.text = caption; b.custom_minimum_size = Vector2(90,52)
	b.add_theme_font_size_override("font_size",19)
	var style := StyleBoxFlat.new(); style.bg_color = Color(.13,.20,.18,.88)
	style.set_corner_radius_all(12); style.content_margin_left = 16; style.content_margin_right = 16
	b.add_theme_stylebox_override("normal",style)
	b.pressed.connect(action); row.add_child(b); return b

func _hud() -> void:
	var layer := CanvasLayer.new(); add_child(layer)
	var row := HBoxContainer.new(); row.position = Vector2(18,16)
	row.add_theme_constant_override("separation",8); layer.add_child(row)
	_button("Orte",func(): map_panel.visible = not map_panel.visible,row)
	_button("Ansicht",func(): zoom_factor = 1.0,row)
	_button("Lizenzen",func(): license_panel.visible = not license_panel.visible,row)
	door_button = _button("Ins Haus",func(): _visit("Haustür" if landscape.inside else "Wohnzimmer"),row)
	place_label = Label.new(); place_label.add_theme_font_size_override("font_size",23)
	place_label.add_theme_constant_override("outline_size",5)
	place_label.add_theme_color_override("font_outline_color",Color(.08,.14,.13)); row.add_child(place_label)
	map_panel = PanelContainer.new(); map_panel.position = Vector2(18,82); map_panel.visible = false
	layer.add_child(map_panel)
	var locations := VBoxContainer.new(); map_panel.add_child(locations)
	for place in WorldScript.LANDMARKS:
		var destination: String = place
		_button(destination,func(): _visit(destination); map_panel.hide(),locations)
	stick = StickScript.new(); layer.add_child(stick); stick.mouse_filter = Control.MOUSE_FILTER_IGNORE
	stick.position = Vector2(112,614)
	action_row = HBoxContainer.new(); layer.add_child(action_row)
	_button("Sitzen",func():
		fennec.target = null; pending_place = ""; fennec.seated = not fennec.seated,action_row)
	_button("Hüpfen",func(): fennec.jump(),action_row)
	help_label = Label.new(); help_label.text = "Tippen: gehen · Doppeltipp: laufen · Zwei Finger: näher ansehen"
	help_label.add_theme_font_size_override("font_size",15)
	help_label.add_theme_constant_override("outline_size",4)
	help_label.add_theme_color_override("font_outline_color",Color(.12,.15,.12))
	layer.add_child(help_label)
	_license_ui(layer)

func _license_ui(layer: CanvasLayer) -> void:
	license_panel = PanelContainer.new(); license_panel.position = Vector2(70,88)
	license_panel.size = Vector2(1140,520); license_panel.hide(); layer.add_child(license_panel)
	var column := VBoxContainer.new(); license_panel.add_child(column)
	_button("Lizenzen schließen",func(): license_panel.hide(),column)
	var scroll := ScrollContainer.new(); scroll.size_flags_vertical = Control.SIZE_EXPAND_FILL
	column.add_child(scroll)
	var label := Label.new(); label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	label.custom_minimum_size = Vector2(1060,0); label.add_theme_font_size_override("font_size",14)
	var notices := "Itoeva · Godot 2D-Prototyp\nEngine: Godot %s · MIT-Lizenz\nhttps://godotengine.org/license/\n\n%s\n\nMitgelieferte Komponenten\n" % [Engine.get_version_info().string,Engine.get_license_text()]
	for component in Engine.get_copyright_info():
		notices += "\n"+component.name+"\n"
		for part in component.parts:
			notices += "Dateien: "+", ".join(part.files)+"\n"
			notices += "\n".join(part.copyright)+"\nLizenz: "+part.license+"\n"
	for name in Engine.get_license_info():
		notices += "\n"+name+"\n"+Engine.get_license_info()[name]+"\n"
	notices += "\nBilder: vorhandene Itoeva-Küstenlandschaft, Wohnzimmer und Fennec. Keine zusätzlich lizenzierten Asset-Pakete.\n"
	label.text = notices; scroll.add_child(label)

func _visit(place: String) -> void:
	pending_place = ""
	if place == "Wohnzimmer" and not landscape.inside:
		pending_place = place; fennec.go(WorldScript.ENTRY+Vector2(0,-6))
	elif place != "Wohnzimmer" and landscape.inside:
		pending_place = place; fennec.go(WorldScript.ENTRY+Vector2(0,16))
	else: fennec.go(WorldScript.LANDMARKS[place])

func _access_changed(_inside: bool) -> void:
	_transition_positions.append(fennec.position)
	# Switching the cutaway never changes the actor's coordinates or creates a scene.
	fennec.target = null; fennec.path = PackedVector2Array()
	if not pending_place.is_empty():
		var next := pending_place; pending_place = ""
		fennec.go(WorldScript.LANDMARKS[next])

func _process(dt: float) -> void:
	if fennec == null: return
	if stick_finger < 0:
		fennec.manual = Vector2(float(Input.is_physical_key_pressed(KEY_D) or Input.is_physical_key_pressed(KEY_RIGHT))-float(Input.is_physical_key_pressed(KEY_A) or Input.is_physical_key_pressed(KEY_LEFT)),float(Input.is_physical_key_pressed(KEY_S) or Input.is_physical_key_pressed(KEY_DOWN))-float(Input.is_physical_key_pressed(KEY_W) or Input.is_physical_key_pressed(KEY_UP))).limit_length()
		if fennec.manual.length()>.1: fennec.running = Input.is_physical_key_pressed(KEY_SHIFT)
	var window := get_viewport().get_visible_rect().size
	var zoom := 1.08*zoom_factor
	if landscape.inside:
		zoom = minf(window.x/(1672*WorldScript.ROOM_SCALE),window.y/(941*WorldScript.ROOM_SCALE))*zoom_factor
	cam.zoom = cam.zoom.lerp(Vector2.ONE*zoom,1-exp(-dt*4))
	var wanted := Vector2(fennec.position.x+fennec.facing.x*85,362+(fennec.position.y-540)*.18)
	if landscape.inside:
		wanted = WorldScript.ROOM_ORIGIN+Vector2(836,470)*WorldScript.ROOM_SCALE
		wanted += (fennec.position-(WorldScript.ROOM_ORIGIN+Vector2(836,740)*WorldScript.ROOM_SCALE))*.12
		var half := window/(2*cam.zoom)
		var bounds := Vector2(1672,941)*WorldScript.ROOM_SCALE
		wanted.x = WorldScript.ROOM_ORIGIN.x+bounds.x*.5 if half.x>=bounds.x*.5 else clampf(wanted.x,WorldScript.ROOM_ORIGIN.x+half.x,WorldScript.ROOM_ORIGIN.x+bounds.x-half.x)
		wanted.y = WorldScript.ROOM_ORIGIN.y+bounds.y*.5 if half.y>=bounds.y*.5 else clampf(wanted.y,WorldScript.ROOM_ORIGIN.y+half.y,WorldScript.ROOM_ORIGIN.y+bounds.y-half.y)
	else:
		var half := window/(2*cam.zoom)
		wanted.x = clampf(wanted.x,half.x,WorldScript.W-half.x)
		wanted.y = 362 if half.y>350 else clampf(wanted.y,half.y,WorldScript.H-half.y)
	cam.position = cam.position.lerp(wanted,1-exp(-dt*4))
	place_label.text = "  "+landscape.place_name(fennec.position)
	if fennec.blocked: place_label.text += " · Kein Weg"
	door_button.text = "Zur Küste" if landscape.inside else "Ins Haus"
	door_button.visible = landscape.inside or fennec.position.distance_to(WorldScript.ENTRY)<120
	stick.position = Vector2(112,window.y-106)
	action_row.position = Vector2(window.x-240,window.y-86)
	help_label.position = Vector2(210,window.y-32)
	license_panel.size = Vector2(maxf(window.x-140,300),maxf(window.y-176,240))
	stick.direction = fennec.manual; stick.queue_redraw()

func _floor_point(screen: Vector2) -> Vector2:
	return get_viewport().get_canvas_transform().affine_inverse()*screen

func tap(screen: Vector2) -> void:
	var point := _floor_point(screen)
	if point.distance_to(fennec.position-Vector2(0,35*fennec.visual_scale))<28*fennec.visual_scale:
		fennec.joy(); return
	pending_place = ""
	var now := Time.get_ticks_msec()
	var double := now-last_tap_ms<320 and screen.distance_to(last_tap_pos)<60
	last_tap_ms = now; last_tap_pos = screen; fennec.go(point,double)

func _input(event: InputEvent) -> void:
	if event is InputEventKey and event.pressed and event.physical_keycode == KEY_SPACE:
		fennec.jump(); get_viewport().set_input_as_handled()
	if event is InputEventScreenTouch:
		if event.pressed and event.position.distance_to(stick.global_position)<86 and stick_finger<0:
			stick_finger = event.index; pending_place = ""
			fennec.manual = ((event.position-stick.global_position)/64).limit_length()
			fennec.running = event.position.distance_to(stick.global_position)>56
			get_viewport().set_input_as_handled()
		elif not event.pressed and event.index==stick_finger:
			stick_finger = -1; fennec.manual = Vector2.ZERO; get_viewport().set_input_as_handled()
	elif event is InputEventScreenDrag and event.index==stick_finger:
		fennec.manual = ((event.position-stick.global_position)/64).limit_length()
		fennec.running = event.position.distance_to(stick.global_position)>56
		get_viewport().set_input_as_handled()

func _unhandled_input(event: InputEvent) -> void:
	if license_panel.visible or map_panel.visible: return
	if event is InputEventScreenTouch:
		if event.pressed:
			touches[event.index] = event.position
			if touches.size()==2:
				gesture_zoom = true
				var points := touches.values(); pinch_start = points[0].distance_to(points[1]); zoom_start = zoom_factor
		else:
			if not gesture_zoom and touches.size()==1 and touches.has(event.index): tap(event.position)
			touches.erase(event.index)
			if touches.is_empty(): gesture_zoom = false
	elif event is InputEventScreenDrag:
		if touches.has(event.index): touches[event.index] = event.position
		if touches.size()==2:
			var points := touches.values()
			zoom_factor = clampf(zoom_start*points[0].distance_to(points[1])/maxf(pinch_start,1),1.0,1.7)
	elif event is InputEventMouseButton and event.pressed:
		if event.button_index==MOUSE_BUTTON_WHEEL_UP: zoom_factor = minf(1.7,zoom_factor+.1)
		elif event.button_index==MOUSE_BUTTON_WHEEL_DOWN: zoom_factor = maxf(1,zoom_factor-.1)

func _check(ok: bool, detail: String) -> void:
	if not ok: _errors += 1; push_error("WORLD VALIDATION: "+detail)
	print("CHECK ","PASS " if ok else "FAIL ",detail)

func _capture_view(name: String) -> void:
	if not _capture or DisplayServer.get_name()=="headless": return
	await get_tree().create_timer(.8).timeout
	await RenderingServer.frame_post_draw
	get_viewport().get_texture().get_image().save_png("user://2d_"+name+".png")

func _walk(place: String) -> void:
	_visit(place)
	var started := Time.get_ticks_msec()
	while (fennec.target!=null or not pending_place.is_empty()) and Time.get_ticks_msec()-started<45000:
		await get_tree().physics_frame
	_check(not fennec.blocked and fennec.position.distance_to(WorldScript.LANDMARKS[place])<16,"begehbarer Hin-/Rückweg: "+place)

func _run_probe() -> void:
	await get_tree().create_timer(.5).timeout
	var nodes := get_tree().get_node_count()
	var coast_id := WorldScript.COAST.get_instance_id(); var room_id := WorldScript.LIVING.get_instance_id()
	await _capture_view("strand")
	await _walk("Steg"); await _capture_view("steg")
	await _walk("Wald"); await _capture_view("wald")
	await _walk("Wasser")
	_check(fennec.wading and landscape.water_depth(fennec.position)>.7,"vom Strand ins Wasser mit sichtbarer Eintauchtiefe")
	await _capture_view("wasser")
	await _walk("Wohnzimmer")
	_check(landscape.inside,"Haus durch die gemeinsame Tür betreten")
	await _capture_view("wohnzimmer")
	var front: float = fennec.visual_scale
	fennec.go(WorldScript.ROOM_ORIGIN+Vector2(1350,610)*WorldScript.ROOM_SCALE)
	while fennec.target!=null: await get_tree().physics_frame
	await get_tree().create_timer(.4).timeout
	_check(front-fennec.visual_scale>.10,"begehbare Raumtiefe: perspektivische Figurengröße")
	await _capture_view("raumtiefe")
	fennec.go(WorldScript.ROOM_ORIGIN+Vector2(1260,535)*WorldScript.ROOM_SCALE)
	var occlusion_started := Time.get_ticks_msec()
	while fennec.target!=null and Time.get_ticks_msec()-occlusion_started<12000: await get_tree().physics_frame
	_check(not fennec.blocked and fennec.position.distance_to(WorldScript.ROOM_ORIGIN+Vector2(1260,535)*WorldScript.ROOM_SCALE)<16,"hinter die gemalte Vordergrundpflanze gehen")
	await _capture_view("verdeckt")
	# A real touch joystick event exercises the input path and free ground movement.
	fennec.go(WorldScript.ROOM_ORIGIN+Vector2(1350,630)*WorldScript.ROOM_SCALE)
	while fennec.target!=null: await get_tree().physics_frame
	var before: Vector2 = fennec.position
	var touch := InputEventScreenTouch.new(); touch.index = 8; touch.pressed = true
	touch.position = stick.global_position+Vector2(48,0)
	if DisplayServer.get_name()=="headless": _input(touch)
	else: get_viewport().push_input(touch)
	await get_tree().create_timer(.35).timeout
	touch.pressed = false
	if DisplayServer.get_name()=="headless": _input(touch)
	else: get_viewport().push_input(touch)
	_check(fennec.position.x>before.x+5 and stick_finger==-1,"Touch-Joystick: bewegen und loslassen")
	fennec.jump(); await get_tree().create_timer(.2).timeout
	_check(fennec.height>25,"Hüpfen mit Höhe über der Bodenebene")
	await get_tree().create_timer(.7).timeout
	await _walk("Strand")
	_check(not landscape.inside and landscape.state_changes==2,"dieselbe Haustür funktioniert in beide Richtungen")
	_check(nodes==get_tree().get_node_count() and coast_id==WorldScript.COAST.get_instance_id() and room_id==WorldScript.LIVING.get_instance_id(),"keine Szene und kein Bild beim Ortswechsel neu geladen")
	await _capture_view("rueckweg")
	print("WORLD VALIDATION: ",_errors," errors")
	get_tree().quit(0 if _errors==0 else 1)
