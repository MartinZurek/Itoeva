extends Control

const RADIUS := 78.0
const ACTION_RADIUS := 86.0
const SWIPE_DISTANCE := 31.0
const HOLD_SECONDS := 0.5
const ActivityArt = preload("res://scripts/world_activity_art.gd")
const REACH_INK := Color("ffe1a0")
const DISCOVERY_KINDS := ["pickup", "card_find", "story", "wonder", "moment", "landmark"]
const PICKUP_FLASH_SECONDS := 0.7
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
var display_context: Dictionary = {}
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
var stamina_visibility := 0.0
var marker_tick := 0.0
var draw_tick := 0.0
var prompt_style: StyleBoxFlat
var mouse_point := Vector2.INF
var hovered_target: Dictionary = {}
var pickup_flash_point := Vector3.ZERO
var pickup_flash_clock := 0.0
var marker_refreshes := 0
var travel_hint: Label

func _ready() -> void :
	set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	caption = Label.new()
	caption.position = Vector2(28, 22)
	caption.add_theme_font_size_override("font_size", 22)
	caption.add_theme_constant_override("outline_size", 5)
	caption.add_theme_color_override("font_outline_color", Color(0.08, 0.1, 0.08, 0.9))
	caption.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(caption)
	hint = Label.new()
	hint.position = Vector2(28, 55)
	hint.add_theme_font_size_override("font_size", 15)
	hint.add_theme_constant_override("outline_size", 4)
	hint.add_theme_color_override("font_outline_color", Color(0.08, 0.1, 0.08, 0.9))
	hint.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(hint)
	stamina_label = Label.new()
	stamina_label.position = Vector2(28, 86)
	stamina_label.add_theme_font_size_override("font_size", 12)
	stamina_label.add_theme_constant_override("outline_size", 2)
	stamina_label.add_theme_color_override("font_color", Color("c9c3b7"))
	stamina_label.add_theme_color_override("font_outline_color", Color(0.08, 0.1, 0.08, 0.65))
	stamina_label.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(stamina_label)
	stamina_bar = ProgressBar.new()
	stamina_bar.position = Vector2(28, 106)
	stamina_bar.size = Vector2(132, 4)
	stamina_bar.max_value = 100
	stamina_bar.show_percentage = false
	stamina_bar.mouse_filter = Control.MOUSE_FILTER_IGNORE
	var background := StyleBoxFlat.new()
	background.bg_color = Color("262b286e")
	background.set_corner_radius_all(2)
	stamina_bar.add_theme_stylebox_override("background", background)
	var fill := StyleBoxFlat.new()
	fill.bg_color = Color("b9b5a5a8")
	fill.set_corner_radius_all(2)
	stamina_bar.add_theme_stylebox_override("fill", fill)
	add_child(stamina_bar)
	stamina_bar.set_deferred("size", Vector2(132, 4))
	stamina_bar.modulate.a = 0.0
	stamina_label.modulate.a = 0.0
	_top_button("Menü", Vector2(-154, 22), func(): app.show_menu())
	_top_button("Karte", Vector2(-292, 22), func(): app.show_map())
	travel_hint = Label.new()
	travel_hint.set_anchors_preset(Control.PRESET_TOP_RIGHT)
	travel_hint.offset_left = -440
	travel_hint.offset_right = -30
	travel_hint.offset_top = 86
	travel_hint.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	travel_hint.text_overrun_behavior = TextServer.OVERRUN_TRIM_ELLIPSIS
	travel_hint.add_theme_font_size_override("font_size", 16)
	travel_hint.add_theme_constant_override("outline_size", 5)
	travel_hint.add_theme_color_override("font_color", Color("ffe4a0"))
	travel_hint.add_theme_color_override("font_outline_color", Color("20251e"))
	travel_hint.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(travel_hint)
	resized.connect(reset_touch)
	prompt_style = StyleBoxFlat.new()
	prompt_style.bg_color = Color("17211cee")
	prompt_style.border_color = Color("c69e62")
	prompt_style.set_border_width_all(1)
	prompt_style.set_corner_radius_all(8)
	queue_redraw()

func action_center() -> Vector2:
	return Vector2(size.x - 132, size.y - 132)

func _top_button(text: String, offset: Vector2, callback: Callable) -> void :
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

func set_enabled(value: bool) -> void :
	enabled = value
	visible = value
	if value: marker_tick = 0.0
	if not value:
		mouse_point = Vector2.INF
		reset_touch()

func reset_touch() -> void :
	finger = -1
	camera_finger = -1
	action_finger = -1
	action_elapsed = 0.0
	action_fired = false
	action_context = {}
	if is_instance_valid(actor):
		actor.touch_vector = Vector2.ZERO
	queue_redraw()

func _process(delta: float) -> void :
	if not enabled or not is_instance_valid(actor):
		return
	if action_finger != -1 and not action_fired:
		action_elapsed += delta
		if action_elapsed >= HOLD_SECONDS:
			_fire_action("sit")
	stamina_bar.value = actor.stamina.value

	var needed: bool = actor.stamina.value < 99.5 or actor.running
	stamina_visibility = move_toward(stamina_visibility, 0.76 if needed else 0.0, delta * 1.5)
	stamina_bar.modulate.a = stamina_visibility
	stamina_label.modulate.a = stamina_visibility * 0.85
	stamina_label.text = "Ausdauer · kurz Atem holen" if actor.stamina.winded_clock > 0 else "Ausdauer · Gehen erholt" if actor.stamina.exhausted else "Ausdauer · erholt sich" if actor.state == "sit" and needed else "Ausdauer"
	stamina_bar.get_theme_stylebox("fill").bg_color = Color("c3ac88b0") if actor.stamina.exhausted else Color("b9b5a5a8")
	pickup_flash_clock = maxf(0, pickup_flash_clock - delta)
	marker_tick -= delta;draw_tick -= delta
	if marker_tick <= 0.0:
		marker_tick = 0.1
		_update_markers()
		_update_travel_hint()
	if draw_tick <= 0.0:
		draw_tick = 1.0 / 30.0;queue_redraw()

func _start_action(index: int, point: Vector2) -> void :
	action_finger = index
	action_origin = point
	action_thumb = point
	action_elapsed = 0.0
	action_fired = false
	action_context = {"kind": "cancel", "label": "Abbrechen"} if not app.pending_interaction.is_empty() else app.context_target()

func _drag_action(point: Vector2) -> void :
	action_thumb = point
	var movement := point - action_origin
	if not action_fired and movement.length() > SWIPE_DISTANCE:
		if absf(movement.y) > absf(movement.x):
			_fire_action("jump" if movement.y < 0 else "sit")
		else:
			actor.facing = Vector2(signf(movement.x), 0)
			_fire_action("roll")

func _fire_action(action: String) -> void :
	app.cancel_approach()

	if not actor.request_action(action):
		action_fired = true
		app._notice("Kurz ausruhen, dann klappt die Rolle wieder." if actor.stamina.exhausted or actor.stamina.value < 14 else "Rollen ist nach der Landung wieder möglich.")
		return
	action_fired = true
	if action == "sit":

		finger = -1
		actor.touch_vector = Vector2.ZERO

func _release_action(canceled: bool) -> void :
	if not action_fired and not canceled:
		var target: Dictionary = action_context
		if target.get("kind", "") == "cancel":
			app.cancel_approach()
		elif not target.is_empty():
			app.interact_with(target.kind, target.id)
		else:
			_fire_action("jump")
	action_finger = -1
	action_context = {}
	queue_redraw()

func _icon_at(point: Vector2) -> Dictionary:
	var closest: Dictionary = {}
	var closest_distance := INF
	for marker in markers:
		var distance := point.distance_squared_to(marker.screen)
		var hit := distance < 28.0 * 28.0
		if marker.kind == "door":
			hit = Rect2(marker.screen - Vector2(24, 24), Vector2(48, 48)).has_point(point)
		if hit and distance < closest_distance:
			closest = marker
			closest_distance = distance
	return closest

func _body_at(point: Vector2) -> Dictionary:
	var frontmost: Dictionary = {}
	for marker in markers:
		var rect: Rect2 = marker.body_rect
		if not rect.has_area() or not rect.grow(4).has_point(point):
			continue
		if marker.kind != "talk":
			if frontmost.is_empty(): frontmost = marker
		elif frontmost.get("kind") != "talk" or marker.creature.sprite.render_priority > frontmost.creature.sprite.render_priority:
			frontmost = marker
	return frontmost

func _tap_marker(point: Vector2) -> bool:
	_update_markers()
	var focus := _focus_marker()
	if not focus.is_empty() and _prompt_rect(focus).has_point(point):
		app.request_interaction(focus.kind, focus.id)
		return true
	var hit := _icon_at(point)
	if not hit.is_empty():
		app.request_interaction(hit.kind, hit.id)
		return true
	for marker in route_markers:
		if point.distance_to(marker.screen) < 36.0:
			app.request_walk(marker.position)
			return true
	hit = _body_at(point)
	if hit.is_empty(): return false
	app.request_interaction(hit.kind, hit.id)
	return true

func _input(event: InputEvent) -> void :
	if not enabled or not is_instance_valid(actor):
		return
	if event is InputEventScreenTouch:
		mouse_point = Vector2.INF
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
		if finger == -1 and event.position.distance_to(Vector2(118, size.y - 118)) < RADIUS * 1.1:
			finger = event.index
			origin = Vector2(118, size.y - 118) if fixed_stick else event.position
			thumb = origin;_update_stick(event.position)
		elif action_finger == -1 and event.position.distance_to(action_center()) < ACTION_RADIUS * 1.15:
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
	elif event is InputEventMouseMotion:
		mouse_point = event.position
		if action_finger == 999: _drag_action(event.position)

func _update_stick(point: Vector2) -> void :
	app.cancel_approach()
	var radius := RADIUS / sensitivity
	var displacement := (point - origin).limit_length(radius)
	thumb = origin + displacement
	actor.touch_vector = displacement / radius
	queue_redraw()

func _update_markers() -> void :
	marker_refreshes += 1
	markers.clear()
	route_markers.clear()
	if not is_instance_valid(app.stage):
		return
	display_context = app.context_target()
	var camera: Camera3D = app.camera
	for target in app.interaction_targets():
		var chosen := _same_target(target, app.pending_interaction)
		if target.kind == "pickup":
			var tint: Color = app.stage.actor_scene_color(target.pickup.position)
			target.pickup.art.modulate = tint.lerp(REACH_INK, 0.45) if target.ready else tint.darkened(0.2)
		var range_limit := 5.0 if target.kind in DISCOVERY_KINDS else 9.0
		if not target.ready and not chosen and target.distance > range_limit: continue
		if actor.mounted and target.has("seat") and target.seat.seat != null and actor.get_meta("mounted_seat", "") == target.id: continue
		if camera.is_position_behind(target.position): continue
		var point := camera.unproject_position(target.position)
		var body_rect: Rect2 = target.get("body_rect", Rect2())
		if target.kind == "talk":
			body_rect = target.creature.visual_screen_rect(camera)
		elif target.kind in ["pickup", "card_find", "story", "ring_lever", "mill_lever", "ried"]:
			var art_point: Vector3 = target.position.lerp(target.approach, 0.55)
			if target.kind == "card_find": art_point = app.CardFinds.prop_position(target.approach)
			var prop := camera.unproject_position(art_point)
			var extent := Vector2(48, 44) if target.kind in ["mill_lever", "ring_lever", "ried"] else Vector2(48, 26)
			body_rect = Rect2(prop - extent * 0.5, extent)
		var offset := Vector2.ZERO
		if body_rect.has_area():
			offset = Vector2(body_rect.get_center().x, body_rect.position.y - 20) - point
		var screen := _clear_controls(point + offset)
		if not _marker_area().has_point(screen): continue
		var marker: Dictionary = target.duplicate()
		marker["screen"] = screen
		marker["projection"] = point
		marker["offset"] = offset
		marker["body_rect"] = body_rect
		markers.append(marker)

func _marker_area() -> Rect2:
	return Rect2(Vector2(24, 94), size - Vector2(48, 144))

func _project_markers() -> void:
	for marker in markers:
		var point: Vector2 = app.camera.unproject_position(marker.position)
		var rect: Rect2 = marker.body_rect
		if rect.has_area():
			rect.position += point - marker.projection
			marker.body_rect = rect
		marker.projection = point
		marker.screen = _clear_controls(point + marker.offset)

func _clear_controls(point: Vector2) -> Vector2:
	for control in [action_center(), Vector2(118, size.y - 118)]:
		var padding := ACTION_RADIUS * 1.15 + 29.0 if control == action_center() else RADIUS * 1.1 + 29.0
		if point.distance_to(control) < padding:
			point.y = control.y - padding
	return point

func _same_target(marker: Dictionary, target: Dictionary) -> bool:
	return marker.kind == target.get("kind", "") and marker.id == target.get("id", "")

func _focus_marker() -> Dictionary:
	if not app.pending_interaction.is_empty():
		for marker in markers:
			if _same_target(marker, app.pending_interaction): return marker
		return {}
	if mouse_point != Vector2.INF:
		for marker in markers:
			if marker.ready and _same_target(marker, hovered_target) and _prompt_rect(marker).has_point(mouse_point):
				return marker
		var pointed := _icon_at(mouse_point)
		if pointed.is_empty(): pointed = _body_at(mouse_point)
		if not pointed.is_empty() and pointed.ready:
			hovered_target = {"kind": pointed.kind, "id": pointed.id}
			return pointed
	hovered_target.clear()
	for marker in markers:
		if marker.ready and _same_target(marker, display_context): return marker
	return {}

func _prompt_rect(marker: Dictionary) -> Rect2:
	var font := ThemeDB.fallback_font
	var width := minf(font.get_string_size(marker.label, HORIZONTAL_ALIGNMENT_LEFT, -1, 16).x + 26, size.x - 48)
	var point: Vector2 = marker.screen
	var top := point.y - 51 if point.y >= 147 else point.y + 25
	return Rect2(clampf(point.x - width * 0.5, 24, size.x - width - 24), top, width, 28)

func _draw_prompt(marker: Dictionary) -> void:
	var rect := _prompt_rect(marker)
	draw_style_box(prompt_style, rect)
	var font := ThemeDB.fallback_font
	var text := TextLine.new()
	text.add_string(marker.label, font, 16)
	text.width = rect.size.x - 24.0
	text.alignment = HORIZONTAL_ALIGNMENT_CENTER
	text.text_overrun_behavior = TextServer.OVERRUN_TRIM_ELLIPSIS
	text.draw(get_canvas_item(), rect.position + Vector2(12, (rect.size.y - text.get_size().y) * 0.5), REACH_INK)

func confirm_pickup(point: Vector3) -> void:
	pickup_flash_point = point
	pickup_flash_clock = PICKUP_FLASH_SECONDS
	marker_tick = 0.0

func _draw_pickup_feedback() -> void:
	if pickup_flash_clock <= 0.0 or app.camera.is_position_behind(pickup_flash_point): return
	var at: Vector2 = app.camera.unproject_position(pickup_flash_point + Vector3.UP * 0.2)
	var progress := 1.0 - pickup_flash_clock / PICKUP_FLASH_SECONDS
	for i in 6:
		var offset := Vector2.from_angle(TAU * i / 6) * (5 + progress * 24)
		var p := at + offset + Vector2.UP * progress * 19
		draw_line(p - Vector2.UP * 2, p + Vector2.UP * 2, Color(REACH_INK, (1.0 - progress) * 0.85), 2.0, true)

const Catalog = preload("res://scripts/world_catalog.gd")

func _update_travel_hint() -> void :
	var next: String = app.atlas_next_place
	if next.is_empty():
		travel_hint.text = ""
		return
	var direction: String = app.Routes.walking_direction(app.atlas_route_from, next)
	travel_hint.text = ("Durchgang · " if direction.is_empty() else direction + " ") + Catalog.label_for(next, app.selected)

func _notification(what: int) -> void :
	if what == NOTIFICATION_WM_MOUSE_EXIT:
		mouse_point = Vector2.INF
	elif what == NOTIFICATION_APPLICATION_FOCUS_OUT:
		mouse_point = Vector2.INF
		reset_touch()

func _chevron(center: Vector2, direction: float, ink: Color) -> void :
	var points := PackedVector2Array([center + Vector2(-4 * direction, -7), center + Vector2(4 * direction, 0), center + Vector2(-4 * direction, 7)])
	draw_polyline(points, Color(0.08, 0.1, 0.09, ink.a * 0.8), 4.5, true)
	draw_polyline(points, ink, 2.0, true)


func door_direction(door: Dictionary) -> Vector2:
	var direction: String = door.get("route_direction", "")
	if direction.is_empty():
		direction = "LEFT" if door.at.x < 0.2 else "RIGHT" if door.at.x > 0.8 else "UP" if door.at.y < 0.5 else "DOWN"
	return {"LEFT": Vector2.LEFT, "RIGHT": Vector2.RIGHT, "UP": Vector2.UP, "DOWN": Vector2.DOWN}.get(direction, Vector2.UP)

func _draw_door_arrow(marker: Dictionary, ink: Color) -> void :
	var at: Vector2 = marker.screen
	var direction := door_direction(marker.door)
	var side := Vector2( - direction.y, direction.x)
	var arrow := PackedVector2Array([at - direction * 5 - side * 6, at + direction * 5, at - direction * 5 + side * 6])
	draw_polyline(arrow, Color(0.03, 0.06, 0.04, 0.7), 5, true)
	draw_polyline(arrow, ink, 2.1, true)

func _draw() -> void :
	_project_markers()
	app.reactions.draw(self)
	app.moments.draw(self)
	app.Story.draw(self)
	app.CardFinds.draw(self)
	app.room_activities.draw(self)
	ActivityArt.draw(self)
	_draw_pickup_feedback()
	var ink := Color(0.88, 0.96, 0.91, 0.7)
	var center := origin if finger >= 0 else Vector2(118, size.y - 118)
	var radius := RADIUS / sensitivity
	draw_circle(center, radius, Color(0.07, 0.14, 0.13, 0.25))
	draw_arc(center, radius, 0, TAU, 64, ink, 2.0, true)
	draw_arc(center, radius * 0.7, 0, TAU, 64, Color(0.88, 0.96, 0.91, 0.2), 1.0, true)
	draw_circle(thumb if finger >= 0 else center, 25, Color(0.88, 0.96, 0.91, 0.62))
	var action := action_center()
	var star_rest: bool = actor.species == "starlet" and actor.get("home_rest_phase") != null and int(actor.get("home_rest_phase")) >= 0
	draw_circle(action, ACTION_RADIUS, Color(0.07, 0.14, 0.13, 0.25))
	draw_arc(action, ACTION_RADIUS, 0, TAU, 64, ink, 3.5 if action_finger != -1 else 2.0, true)
	draw_arc(action, ACTION_RADIUS * 0.7, 0, TAU, 64, Color(0.88, 0.96, 0.91, 0.2), 1.0, true)
	draw_line(action + Vector2(0, -39), action + Vector2(0, -64), ink, 3.0, true)
	draw_polyline(PackedVector2Array([action + Vector2(-9, -55), action + Vector2(0, -64), action + Vector2(9, -55)]), ink, 3.0, true)
	_draw_label("Sprung", action + Vector2(0, -24), 12, ink)
	var target := action_context if action_finger != -1 else display_context
	if action_finger == -1 and not app.pending_interaction.is_empty():
		target = {"kind": "cancel", "label": "Abbrechen"}
	_draw_label("Tippen", action + Vector2(0, 5), 12, ink)
	_draw_label(_action_label(target), action + Vector2(0, 23), 15, Color(0.98, 1.0, 0.96))
	for side in [-1.0, 1.0]:
		var roll := action + Vector2(side * 43, -2)
		draw_arc(roll, 13, deg_to_rad(55), deg_to_rad(315), 24, ink, 2.5, true)
		draw_polyline(PackedVector2Array([roll + Vector2(10, -13), roll + Vector2(10, -5), roll + Vector2(2, -6)]), ink, 2.5, true)
	if star_rest:
		var star := PackedVector2Array()
		for i in 10:
			var angle := - PI / 2 + i * PI / 5
			var star_radius := 9.0 if i % 2 == 0 else 4.0
			star.append(action + Vector2(0, 50) + Vector2(cos(angle), sin(angle)) * star_radius)
		draw_colored_polygon(star, ink)
		draw_arc(action + Vector2(0, 59), 11, 0.15, PI - 0.15, 20, ink, 2.0, true)
	else:

		draw_circle(action + Vector2(-5, 41), 3.8, ink)
		draw_polyline(PackedVector2Array([action + Vector2(-5, 48), action + Vector2(-5, 57), action + Vector2(9, 57), action + Vector2(9, 66)]), ink, 3.0, true)
		draw_line(action + Vector2(-15, 59), action + Vector2(1, 59), ink, 2.5, true)
		draw_line(action + Vector2(-12, 59), action + Vector2(-12, 67), ink, 2.5, true)
	if action_finger != -1 and not action_fired:
		draw_arc(action, ACTION_RADIUS - 6, - PI / 2, - PI / 2 + TAU * minf(action_elapsed / HOLD_SECONDS, 1), 48, ink, 4.0, true)
	var control_label := "↓ / Halten: Aufstehen" if actor.state == "sit" else "↓ / Halten: Sitzen"
	if star_rest:
		control_label = "↓ / Halten: Fortgleiten"
	if app.control_help_clock > 0 or actor.state == "sit": _draw_label(control_label, action + Vector2(0, ACTION_RADIUS + 20), 14, Color(0.95, 0.98, 0.94, 0.85))
	var focus := _focus_marker()
	for marker in markers:
		if _marker_area().has_point(marker.screen):
			_draw_world_marker(marker, _same_target(marker, focus))
	if not focus.is_empty() and _marker_area().has_point(focus.screen):
		if focus.ready and (focus.has("seat") or focus.kind == "landmark"): _draw_prop_focus(focus.body_rect)
		_draw_prompt(focus)
	for marker in route_markers:
		draw_circle(marker.screen, 24, Color(0.04, 0.1, 0.1, 0.34))
		_chevron(marker.screen, marker.direction, Color(0.98, 0.94, 0.78, 0.86))
	_draw_destination()
	app.wonders.draw(self)

func _action_label(target: Dictionary) -> String:
	match target.get("kind", ""):
		"": return "Sprung"
		"door": return "Durchgang"
		"cancel", "stand", "rest_menu": return target.label
		_: return target.get("action", "Interagieren")

func _draw_world_marker(marker: Dictionary, focused: bool) -> void:
	var p: Vector2 = marker.screen
	var ready: bool = marker.ready or focused
	var color := REACH_INK if ready else Color("cdc9afad")
	var discovery: bool = marker.kind in DISCOVERY_KINDS
	var radius := 17.0 if ready else 11.0
	if ready:
		var pulse := 0.5 + 0.5 * sin(float(app.game_clock) * 4.0)
		draw_circle(p, 23 + pulse * 3, Color(REACH_INK, 0.13 if focused else 0.07), true, -1, true)
	draw_circle(p, radius, Color("17211ce6") if ready else Color("17211cba"), true, -1, true)
	draw_arc(p, radius, 0, TAU, 28, Color(color, 0.85 if ready else 0.46), 1.5, true)
	if not ready and discovery:
		_draw_diamond(p, 4.0, color)
		return
	var scale := 1.0 if ready else 0.72
	match marker.kind:
		"talk", "ried":
			for x in [-6, 0, 6]:
				draw_circle(p + Vector2(x, 0) * scale, 1.7, color, true, -1, true)
		"door":
			_draw_door_arrow(marker, color)
			if not app.atlas_next_place.is_empty() and app.Routes.door_destination(marker.door) == app.atlas_next_place:
				draw_arc(p, radius + 4, 0, TAU, 28, REACH_INK, 2, true)
			if app.automatic_passage(marker.door) and not focused:
				_draw_label(marker.label, p + Vector2(0, 30), 14, color)
		"encounter", "inspect_prop":
			draw_arc(p + Vector2(0, 4), 8, PI * 1.14, PI * 1.86, 12, color, 1.5, true)
			draw_arc(p - Vector2(0, 4), 8, PI * 0.14, PI * 0.86, 12, color, 1.5, true)
			draw_circle(p, 2.0, color)
		"pickup", "card_find", "wonder", "moment", "story", "landmark":
			_draw_diamond(p, 7.0, color)
		"mill_lever", "ring_lever":
			draw_line(p + Vector2(-5, 5) * scale, p + Vector2(5, -5) * scale, color, 2.0, true)
			draw_circle(p + Vector2(5, -5) * scale, 2.2, color)
		"read", "deck", "map", "practice", "card_reference", "card_shop":
			var half := Vector2(8, 5) * scale
			draw_rect(Rect2(p - half, half * 2), color, false, 1.5)
			draw_line(p - Vector2(0, half.y), p + Vector2(0, half.y), color, 1.5, true)
		"sleep", "seat", "home_rest":
			draw_line(p + Vector2(-7, 3) * scale, p + Vector2(7, 3) * scale, color, 2.0, true)
			draw_line(p + Vector2(-7, 3) * scale, p + Vector2(-7, -5) * scale, color, 2.0, true)
		_:
			draw_line(p - Vector2(4, 0), p + Vector2(4, 0), color, 1.5, true)
			draw_line(p - Vector2(0, 4), p + Vector2(0, 4), color, 1.5, true)

func _draw_prop_focus(rect: Rect2) -> void:
	if not rect.has_area(): return
	var edge := rect.grow(3.0)
	var length := clampf(minf(rect.size.x, rect.size.y) * 0.18, 4.0, 12.0)
	for corner in [Vector2.ZERO, Vector2.RIGHT, Vector2.DOWN, Vector2.ONE]:
		var point: Vector2 = edge.position + edge.size * corner
		var inward: Vector2 = Vector2.ONE - 2.0 * corner
		var lines := PackedVector2Array([
			point + Vector2(inward.x * length, 0), point, point + Vector2(0, inward.y * length),
		])
		draw_polyline(lines, Color(REACH_INK, 0.72), 1.5, true)

func _draw_diamond(point: Vector2, radius: float, color: Color) -> void:
	draw_colored_polygon(PackedVector2Array([
		point + Vector2.UP * radius, point + Vector2.RIGHT * radius * 0.7,
		point + Vector2.DOWN * radius, point + Vector2.LEFT * radius * 0.7
	]), color)

func _draw_destination() -> void :
	var active: bool = not app.pending_interaction.is_empty()
	if not active and app.feedback_clock <= 0.0: return
	var goal: Vector3 = app.pending_interaction.get("destination", app.feedback_point)
	if app.camera.is_position_behind(goal): return
	var at: Vector2 = app.camera.unproject_position(goal)
	var color := Color("ffe3a0") if active else Color("8fe0ae") if app.feedback_kind == "arrived" else Color("e59d83") if app.feedback_kind == "blocked" else Color("bec8c4")
	if not active: color.a = minf(1.0, app.feedback_clock * 2)
	var radius := 12.0 + (1.5 * sin(app.game_clock * 3.0) if active else 0.0)
	var ring := PackedVector2Array()
	for i in 25:
		var angle := TAU * i / 24.0;ring.append(at + Vector2(cos(angle) * radius, sin(angle) * radius * 0.38))
	draw_polyline(ring, Color(0.04, 0.06, 0.06, color.a * 0.6), 5.0, true)
	draw_polyline(ring, color, 2.0, true)
	draw_circle(at, 2.0, color)
	if active: _draw_label("Ziel", at + Vector2(0, -18), 12, color)

func _draw_label(text: String, center: Vector2, font_size: int, ink: Color) -> void :
	var font := ThemeDB.fallback_font
	var at := center - Vector2(font.get_string_size(text, HORIZONTAL_ALIGNMENT_LEFT, -1, font_size).x * 0.5, 0)
	draw_string_outline(font, at, text, HORIZONTAL_ALIGNMENT_LEFT, -1, font_size, 3, Color(0.06, 0.09, 0.08, 0.9))
	draw_string(font, at, text, HORIZONTAL_ALIGNMENT_LEFT, -1, font_size, ink)
