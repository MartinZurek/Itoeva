extends Control

const RADIUS := 78.0
var app: Node3D
var actor: CharacterBody3D
var finger := -1
var camera_finger := -1
var origin := Vector2.ZERO
var thumb := Vector2.ZERO
var sensitivity := 1.0
var fixed_stick := false
var enabled := true
var actions: Array[Dictionary] = []
var caption: Label
var hint: Label
var interact_button: Button

func _ready() -> void:
	set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	caption = Label.new()
	caption.position = Vector2(28, 22)
	caption.add_theme_font_size_override("font_size", 22)
	caption.add_theme_constant_override("outline_size", 5)
	caption.add_theme_color_override("font_outline_color", Color(0.08, 0.10, 0.08, 0.9))
	add_child(caption)
	hint = Label.new()
	hint.position = Vector2(28, 55)
	hint.add_theme_font_size_override("font_size", 15)
	hint.add_theme_constant_override("outline_size", 4)
	hint.add_theme_color_override("font_outline_color", Color(0.08, 0.10, 0.08, 0.9))
	add_child(hint)
	_button("Sprung", Vector2(-154, -116), func(): actor.request_action("jump"))
	_button("Rolle", Vector2(-292, -116), func(): actor.request_action("roll"))
	_button("Sitzen", Vector2(-292, -182), func(): actor.request_action("sit"))
	interact_button = _button("Interaktion", Vector2(-154, -182), func(): app.interact())
	_button("+", Vector2(-154, -248), func(): app.adjust_zoom(-0.5))
	_button("−", Vector2(-292, -248), func(): app.adjust_zoom(0.5))
	_top_button("Menü", Vector2(-154, 22), func(): app.show_menu())
	_top_button("Karte", Vector2(-292, 22), func(): app.show_map())
	resized.connect(queue_redraw)
	queue_redraw()

func _button(text: String, offset: Vector2, callback: Callable, top: bool = false) -> Button:
	var button := Button.new()
	button.text = text
	button.set_anchors_preset(Control.PRESET_TOP_RIGHT if top else Control.PRESET_BOTTOM_RIGHT)
	button.position = offset
	button.size = Vector2(124, 56)
	button.theme = app.game_theme
	button.focus_mode = Control.FOCUS_NONE
	button.pressed.connect(callback)
	add_child(button)
	actions.append({"button": button, "callback": callback})
	return button

func _top_button(text: String, offset: Vector2, callback: Callable) -> void:
	_button(text, offset, callback, true)

func set_enabled(value: bool) -> void:
	enabled = value
	visible = value
	if not value:
		reset_touch()

func reset_touch() -> void:
	finger = -1
	camera_finger = -1
	if is_instance_valid(actor):
		actor.touch_vector = Vector2.ZERO
	queue_redraw()

func _input(event: InputEvent) -> void:
	if not enabled or not is_instance_valid(actor):
		return
	if event is InputEventScreenTouch:
		if not event.pressed or event.canceled:
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
		if finger == -1 and event.position.x < size.x * 0.45 and event.position.y > size.y * 0.52:
			finger = event.index
			origin = Vector2(118, size.y - 118) if fixed_stick else event.position
			thumb = origin
			_update_stick(event.position)
			get_viewport().set_input_as_handled()
		elif camera_finger == -1 and event.position.x > size.x * 0.48 and event.position.y < size.y * 0.65:
			camera_finger = event.index
	elif event is InputEventScreenDrag:
		if event.index == finger:
			_update_stick(event.position)
			get_viewport().set_input_as_handled()
		elif event.index == camera_finger:
			app.yaw = clampf(app.yaw + event.relative.x * 0.045, -8.0, 8.0)
	elif event is InputEventMouseButton and event.pressed:
		if event.button_index == MOUSE_BUTTON_WHEEL_UP:
			app.adjust_zoom(-0.4)
		elif event.button_index == MOUSE_BUTTON_WHEEL_DOWN:
			app.adjust_zoom(0.4)

func _update_stick(point: Vector2) -> void:
	var radius := RADIUS / sensitivity
	var displacement := (point - origin).limit_length(radius)
	thumb = origin + displacement
	actor.touch_vector = displacement / radius
	queue_redraw()

func _notification(what: int) -> void:
	if what == NOTIFICATION_APPLICATION_FOCUS_OUT:
		reset_touch()

func _draw() -> void:
	var center := origin if finger >= 0 else Vector2(118, size.y - 118)
	var radius := RADIUS / sensitivity
	draw_circle(center, radius, Color(0.07, 0.14, 0.13, 0.36))
	draw_arc(center, radius, 0, TAU, 64, Color(1, 0.87, 0.62, 0.62), 2.0, true)
	draw_arc(center, radius * 0.7, 0, TAU, 64, Color(1, 0.94, 0.79, 0.20), 1.0, true)
	draw_circle(thumb if finger >= 0 else center, 25, Color(1, 0.94, 0.79, 0.62))
