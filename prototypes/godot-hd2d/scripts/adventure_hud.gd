extends Control

const RADIUS := 78.0
const ACTION_RADIUS := 86.0
const SWIPE_DISTANCE := 31.0
const HOLD_SECONDS := 0.50
var app: Node3D
var actor: CharacterBody3D
var finger := -1
var camera_finger := -1
var action_finger := -1
var origin := Vector2.ZERO
var thumb := Vector2.ZERO
var action_origin := Vector2.ZERO
var action_thumb := Vector2.ZERO
var action_elapsed := 0.0
var action_fired := false
var action_context: Dictionary = {}
var sensitivity := 1.0
var fixed_stick := false
var enabled := true
var actions: Array[Dictionary] = []
var markers: Array[Dictionary] = []
var route_markers: Array[Dictionary] = []
var caption: Label
var hint: Label
var stamina_bar: ProgressBar
var stamina_label: Label

func _ready() -> void:
	set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	caption = Label.new()
	caption.position = Vector2(28, 22)
	caption.add_theme_font_size_override("font_size", 22)
	caption.add_theme_constant_override("outline_size", 5)
	caption.add_theme_color_override("font_outline_color", Color(0.08, 0.10, 0.08, 0.9))
	caption.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(caption)
	hint = Label.new()
	hint.position = Vector2(28, 55)
	hint.add_theme_font_size_override("font_size", 15)
	hint.add_theme_constant_override("outline_size", 4)
	hint.add_theme_color_override("font_outline_color", Color(0.08, 0.10, 0.08, 0.9))
	hint.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(hint)
	stamina_label = Label.new()
	stamina_label.position = Vector2(28, 86)
	stamina_label.add_theme_font_size_override("font_size", 15)
	stamina_label.add_theme_constant_override("outline_size", 4)
	stamina_label.add_theme_color_override("font_outline_color", Color(0.08, 0.10, 0.08, 0.9))
	stamina_label.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(stamina_label)
	stamina_bar = ProgressBar.new()
	stamina_bar.position = Vector2(28, 112)
	stamina_bar.size = Vector2(204, 12)
	stamina_bar.max_value = 100
	stamina_bar.show_percentage = false
	stamina_bar.mouse_filter = Control.MOUSE_FILTER_IGNORE
	var background := StyleBoxFlat.new()
	background.bg_color = Color("263b3be6")
	background.set_corner_radius_all(6)
	background.border_color = Color("bcd7cb")
	background.set_border_width_all(1)
	stamina_bar.add_theme_stylebox_override("background", background)
	var fill := StyleBoxFlat.new()
	fill.bg_color = Color("92d2b0")
	fill.set_corner_radius_all(5)
	stamina_bar.add_theme_stylebox_override("fill", fill)
	add_child(stamina_bar)
	_top_button("Menü", Vector2(-154, 22), func(): app.show_menu())
	_top_button("Karte", Vector2(-292, 22), func(): app.show_map())
	resized.connect(reset_touch)
	queue_redraw()

func action_center() -> Vector2:
	return Vector2(size.x - 132, size.y - 132)

func _top_button(text: String, offset: Vector2, callback: Callable) -> void:
	var button := Button.new()
	button.text = text
	button.set_anchors_preset(Control.PRESET_TOP_RIGHT)
	button.position = offset
	button.size = Vector2(124, 56)
	button.theme = app.game_theme
	button.focus_mode = Control.FOCUS_NONE
	button.pressed.connect(callback)
	add_child(button)
	actions.append({"button": button, "callback": callback})

func set_enabled(value: bool) -> void:
	enabled = value
	visible = value
	if not value:
		reset_touch()

func reset_touch() -> void:
	finger = -1
	camera_finger = -1
	action_finger = -1
	action_elapsed = 0.0
	action_fired = false
	action_context = {}
	if is_instance_valid(actor):
		actor.touch_vector = Vector2.ZERO
	queue_redraw()

func _process(delta: float) -> void:
	if not enabled or not is_instance_valid(actor):
		return
	if action_finger != -1 and not action_fired:
		action_elapsed += delta
		if action_elapsed >= HOLD_SECONDS:
			_fire_action("sit")
	stamina_bar.value = actor.stamina.value
	stamina_label.text = "Ausdauer · %.0f %%" % actor.stamina.value
	if actor.state == "sit":
		stamina_label.text += " · " + ("Sitzpause ++" if actor.mounted else "Sitzpause +")
	elif actor.stamina.exhausted:
		stamina_label.text += " · kurz durchatmen"
	stamina_bar.get_theme_stylebox("fill").bg_color = Color("e4bc80") if actor.stamina.exhausted else Color("92d2b0")
	_update_markers()
	queue_redraw()

func _start_action(index: int, point: Vector2) -> void:
	action_finger = index
	action_origin = point
	action_thumb = point
	action_elapsed = 0.0
	action_fired = false
	action_context = app.context_target()

func _drag_action(point: Vector2) -> void:
	action_thumb = point
	var movement := point - action_origin
	if not action_fired and movement.length() > SWIPE_DISTANCE:
		if absf(movement.y) > absf(movement.x):
			_fire_action("jump" if movement.y < 0 else "sit")
		else:
			actor.facing = Vector2(signf(movement.x), 0)
			_fire_action("roll")

func _fire_action(action: String) -> void:
	app.cancel_approach()
	# Abgelehnte Gesten duerfen beim Loslassen keine andere Aktion ausloesen.
	if not actor.request_action(action):
		action_fired = true
		app._notice("Kurz ausruhen, dann klappt die Rolle wieder." if actor.stamina.exhausted or actor.stamina.value < 14 else "Rollen ist nach der Landung wieder möglich.")
		return
	action_fired = true
	if action == "sit":
		# Ein noch gehaltener linker Daumen soll den Sitz nicht sofort aufheben.
		finger = -1
		actor.touch_vector = Vector2.ZERO

func _release_action(canceled: bool) -> void:
	if not action_fired and not canceled:
		var target: Dictionary = action_context
		if not target.is_empty():
			app.interact_with(target.kind, target.id)
		else:
			_fire_action("jump")
	action_finger = -1
	action_context = {}
	queue_redraw()

func _tap_marker(point: Vector2) -> bool:
	for marker in markers:
		if point.distance_to(marker.screen) < 33.0:
			app.request_interaction(marker.kind, marker.id)
			return true
	# Bei kleinem Telefonbild reicht auch ein Tipp auf die sichtbare Figur.
	var frontmost: Dictionary = {}
	for marker in markers:
		if marker.kind == "talk" and marker.body_rect.grow(8).has_point(point):
			if frontmost.is_empty() or marker.creature.sprite.render_priority > frontmost.creature.sprite.render_priority:
				frontmost = marker
	if not frontmost.is_empty():
		app.request_interaction(frontmost.kind, frontmost.id)
		return true
	for marker in markers:
		if marker.kind in ["seat", "read", "sleep"] and marker.body_rect.grow(4).has_point(point):
			app.request_interaction(marker.kind, marker.id)
			return true
	return false

func _input(event: InputEvent) -> void:
	if not enabled or not is_instance_valid(actor):
		return
	if event is InputEventScreenTouch:
		if not event.pressed or event.canceled:
			if event.index == action_finger:
				_release_action(event.canceled)
				get_viewport().set_input_as_handled()
			if event.index == finger:
				finger = -1
				actor.touch_vector = Vector2.ZERO
			if event.index == camera_finger:
				camera_finger = -1
			queue_redraw()
			return
		for action in actions:
			if action.button.get_global_rect().has_point(event.position):
				action.callback.call()
				get_viewport().set_input_as_handled()
				return
		if action_finger == -1 and event.position.distance_to(action_center()) < ACTION_RADIUS * 1.15:
			_start_action(event.index, event.position)
		elif _tap_marker(event.position):
			pass
		elif finger == -1 and event.position.x < size.x * 0.45 and event.position.y > size.y * 0.52:
			finger = event.index
			origin = Vector2(118, size.y - 118) if fixed_stick else event.position
			thumb = origin
			_update_stick(event.position)
		elif camera_finger == -1 and event.position.x > size.x * 0.48 and event.position.y < size.y * 0.65:
			camera_finger = event.index
		else:
			return
		get_viewport().set_input_as_handled()
	elif event is InputEventScreenDrag:
		if event.index == action_finger:
			_drag_action(event.position)
		elif event.index == finger:
			_update_stick(event.position)
		elif event.index == camera_finger:
			app.yaw = clampf(app.yaw + event.relative.x * 0.045, -8.0, 8.0)
		else:
			return
		get_viewport().set_input_as_handled()
	elif event is InputEventMouseButton:
		if event.button_index == MOUSE_BUTTON_LEFT:
			if not event.pressed and action_finger == 999:
				_release_action(false)
			elif event.pressed and event.position.distance_to(action_center()) < ACTION_RADIUS * 1.15:
				_start_action(999, event.position)
			elif event.pressed and _tap_marker(event.position):
				get_viewport().set_input_as_handled()
		elif event.pressed and event.button_index == MOUSE_BUTTON_WHEEL_UP:
			app.adjust_zoom(-0.4)
		elif event.pressed and event.button_index == MOUSE_BUTTON_WHEEL_DOWN:
			app.adjust_zoom(0.4)
	elif event is InputEventMouseMotion and action_finger == 999:
		_drag_action(event.position)

func _update_stick(point: Vector2) -> void:
	app.cancel_approach()
	var radius := RADIUS / sensitivity
	var displacement := (point - origin).limit_length(radius)
	thumb = origin + displacement
	actor.touch_vector = displacement / radius
	queue_redraw()

func _update_markers() -> void:
	markers.clear()
	route_markers.clear()
	if not is_instance_valid(app.stage):
		return
	var camera: Camera3D = app.camera
	for target in app.interaction_targets():
		if actor.mounted and target.has("seat") and target.seat.seat != null and (actor.get_meta("mounted_seat", "")==target.id or actor.position.distance_to(target.seat.seat_position) < 0.05): continue
		if camera.is_position_behind(target.position):
			continue
		var point := camera.unproject_position(target.position)
		var body_rect: Rect2 = target.get("body_rect", Rect2())
		if target.kind == "talk":
			body_rect = target.creature.visual_screen_rect(camera)
			point = Vector2(body_rect.get_center().x, body_rect.position.y - 26)
		if point.x < 28 or point.x > size.x - 28 or point.y < 90 or point.y > size.y - 50:
			continue
		if point.distance_to(action_center()) < ACTION_RADIUS + 38:
			point.y = action_center().y - ACTION_RADIUS - 42
		var marker: Dictionary = target.duplicate()
		marker["screen"] = point
		marker["body_rect"] = body_rect
		markers.append(marker)
	if app.room not in ["OUTDOOR","FOREST_TRAIL"]:
		return
	for x in [52.0, size.x - 52.0]:
		var ray := camera.project_ray_origin(Vector2(x, size.y * 0.60))
		var direction := camera.project_ray_normal(Vector2(x, size.y * 0.60))
		var hit = Plane(Vector3.UP, 0).intersects_ray(ray, direction)
		if hit == null or hit.x < 2.3 or hit.x > app.stage.width - 2.3:
			continue
		var band: Vector2 = app.stage._limits_at(hit.x)
		var point := Vector3(hit.x, 0.03, Catalog.image_z((band.x + band.y) * 0.5))
		if app.stage.is_walkable(point):
			route_markers.append({"screen": camera.unproject_position(point), "direction": -1.0 if x < size.x * 0.5 else 1.0})

const Catalog = preload("res://scripts/world_catalog.gd")

func _notification(what: int) -> void:
	if what == NOTIFICATION_APPLICATION_FOCUS_OUT:
		reset_touch()

func _chevron(center: Vector2, direction: float, ink: Color) -> void:
	var points := PackedVector2Array([center + Vector2(-4 * direction, -7), center + Vector2(4 * direction, 0), center + Vector2(-4 * direction, 7)])
	draw_polyline(points, Color(0.08, 0.10, 0.09, ink.a * 0.8), 4.5, true)
	draw_polyline(points, ink, 2.0, true)

func _draw() -> void:
	var ink := Color(0.88, 0.96, 0.91, 0.70)
	var center := origin if finger >= 0 else Vector2(118, size.y - 118)
	var radius := RADIUS / sensitivity
	draw_circle(center, radius, Color(0.07, 0.14, 0.13, 0.25))
	draw_arc(center, radius, 0, TAU, 64, ink, 2.0, true)
	draw_arc(center, radius * 0.7, 0, TAU, 64, Color(0.88, 0.96, 0.91, 0.20), 1.0, true)
	draw_circle(thumb if finger >= 0 else center, 25, Color(0.88, 0.96, 0.91, 0.62))
	var action := action_center()
	draw_circle(action, ACTION_RADIUS, Color(0.07, 0.14, 0.13, 0.25))
	draw_arc(action, ACTION_RADIUS, 0, TAU, 64, ink, 3.5 if action_finger != -1 else 2.0, true)
	draw_arc(action, ACTION_RADIUS * 0.70, 0, TAU, 64, Color(0.88, 0.96, 0.91, 0.20), 1.0, true)
	draw_line(action + Vector2(0, -39), action + Vector2(0, -64), ink, 3.0, true)
	draw_polyline(PackedVector2Array([action + Vector2(-9, -55), action + Vector2(0, -64), action + Vector2(9, -55)]), ink, 3.0, true)
	_draw_label("Sprung", action + Vector2(0, -24), 12, ink)
	var target: Dictionary = action_context if action_finger != -1 else app.context_target()
	_draw_label("Tippen", action + Vector2(0, 5), 12, ink)
	_draw_label(target.label if not target.is_empty() else "Sprung", action + Vector2(0, 23), 15, Color(0.98, 1.0, 0.96))
	for side in [-1.0, 1.0]:
		var roll := action + Vector2(side * 43, -2)
		draw_arc(roll, 13, deg_to_rad(55), deg_to_rad(315), 24, ink, 2.5, true)
		draw_polyline(PackedVector2Array([roll + Vector2(10, -13), roll + Vector2(10, -5), roll + Vector2(2, -6)]), ink, 2.5, true)
	# Ein erkennbares Sitzsymbol statt zweier kaum sichtbarer kurzer Striche.
	draw_circle(action + Vector2(-5, 41), 3.8, ink)
	draw_polyline(PackedVector2Array([action + Vector2(-5, 48), action + Vector2(-5, 57), action + Vector2(9, 57), action + Vector2(9, 66)]), ink, 3.0, true)
	draw_line(action + Vector2(-15, 59), action + Vector2(1, 59), ink, 2.5, true)
	draw_line(action + Vector2(-12, 59), action + Vector2(-12, 67), ink, 2.5, true)
	if action_finger != -1 and not action_fired:
		draw_arc(action, ACTION_RADIUS - 6, -PI / 2, -PI / 2 + TAU * minf(action_elapsed / HOLD_SECONDS, 1), 48, ink, 4.0, true)
	var control_label := "↓ / Halten: Aufstehen" if actor.state == "sit" else "↓ / Halten: Sitzen"
	_draw_label(control_label, action + Vector2(0, ACTION_RADIUS + 20), 14, Color(0.95, 0.98, 0.94, 0.85))
	for marker in markers:
		var p: Vector2 = marker.screen
		var color := Color(0.95, 0.98, 0.94, 0.95 if marker.ready else 0.55)
		if marker.kind == "talk":
			if marker.ready:
				draw_circle(p, 31, Color(0.95, 0.98, 0.94, 0.07))
				draw_circle(p, 25, Color(0.95, 0.98, 0.94, 0.10))
			draw_style_box(_bubble_style(color.a), Rect2(p - Vector2(18, 13), Vector2(36, 25)))
			draw_colored_polygon(PackedVector2Array([p + Vector2(-7, 10), p + Vector2(-6, 18), p + Vector2(1, 10)]), Color(0.07, 0.12, 0.11, color.a * 0.9))
			for dx in [-8, 0, 8]:
				draw_circle(p + Vector2(dx, 0), 1.8, color)
		elif marker.kind == "door":
			draw_circle(p, 20, Color(0.07, 0.12, 0.11, 0.38))
			var arrow := PackedVector2Array([p + Vector2(-9, 3), p + Vector2(0, -6), p + Vector2(9, 3)])
			draw_polyline(arrow, Color(0.08, 0.10, 0.09, color.a), 4.5, true)
			draw_polyline(arrow, color, 2.0, true)
			draw_line(p + Vector2(-7, 9), p + Vector2(7, 9), color, 2, true)
		elif marker.kind == "pickup":
			draw_circle(p, 17, Color(0.07, 0.12, 0.11, 0.45))
			draw_polyline(PackedVector2Array([p + Vector2(0, -8), p + Vector2(8, 0), p + Vector2(0, 8), p + Vector2(-8, 0), p + Vector2(0, -8)]), color, 2, true)
		elif marker.kind == "read":
			draw_polyline(PackedVector2Array([p + Vector2(-10,-7),p + Vector2(0,-4),p + Vector2(10,-7),p + Vector2(10,7),p + Vector2(0,10),p + Vector2(-10,7),p + Vector2(-10,-7)]),color,2.0,true)
			draw_line(p + Vector2(0,-4),p + Vector2(0,10),color,2.0,true)
		elif marker.kind == "sleep":
			draw_line(p + Vector2(-11,7),p + Vector2(11,7),color,2.0,true)
			draw_line(p + Vector2(-11,-3),p + Vector2(-11,11),color,2.0,true)
			draw_line(p + Vector2(11,0),p + Vector2(11,11),color,2.0,true)
			draw_line(p + Vector2(-11,0),p + Vector2(11,0),color,3.0,true)
		else:
			draw_line(p + Vector2(-8, -3), p + Vector2(8, -3), color, 2.0, true)
			for dx in [-6, 6]:
				draw_line(p + Vector2(dx, -3), p + Vector2(dx, 6), color, 2.0, true)
		_draw_label(marker.label if marker.kind != "talk" or marker.ready else "Sprechen", p + Vector2(0, -24), 15, color)
	for marker in route_markers:
		_chevron(marker.screen, marker.direction, Color(0.95, 0.98, 0.94, 0.58))

func _draw_label(text: String, center: Vector2, font_size: int, ink: Color) -> void:
	var font := ThemeDB.fallback_font
	var at := center - Vector2(font.get_string_size(text, HORIZONTAL_ALIGNMENT_LEFT, -1, font_size).x * 0.5, 0)
	draw_string_outline(font, at, text, HORIZONTAL_ALIGNMENT_LEFT, -1, font_size, 3, Color(0.06, 0.09, 0.08, 0.9))
	draw_string(font, at, text, HORIZONTAL_ALIGNMENT_LEFT, -1, font_size, ink)

func _bubble_style(alpha: float) -> StyleBoxFlat:
	var style := StyleBoxFlat.new()
	style.bg_color = Color(0.07, 0.12, 0.11, alpha * 0.9)
	style.border_color = Color(0.95, 0.98, 0.94, alpha)
	style.set_border_width_all(1)
	style.set_corner_radius_all(8)
	return style
