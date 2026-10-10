extends Control

var actor: CharacterBody3D
var world: Node3D
var finger := -1
var origin := Vector2.ZERO
var thumb := Vector2.ZERO
var actions: Array[Dictionary] = []
const RADIUS := 66.0

func _ready() -> void:
	set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	var title := Label.new()
	title.text = "ITOEVA  /  GEMALTE WELT"
	title.position = Vector2(26, 20)
	title.add_theme_font_size_override("font_size", 17)
	title.add_theme_constant_override("outline_size", 4)
	title.add_theme_color_override("font_outline_color", Color(0.10, 0.12, 0.09, 0.8))
	add_child(title)
	var guide := Label.new()
	guide.text = "Straße · Park · Waldrand\nWASD / Pfeile  ·  Shift laufen  ·  Leertaste springen\nQ/E schwenken  ·  Z/X zoomen  ·  L Abendlicht"
	guide.position = Vector2(26, 46)
	guide.add_theme_font_size_override("font_size", 14)
	guide.add_theme_constant_override("outline_size", 3)
	guide.add_theme_color_override("font_outline_color", Color(0.10, 0.12, 0.09, 0.8))
	guide.modulate = Color(1, 1, 1, 0.85)
	add_child(guide)
	_add_button("Sprung", Vector2(-138, -108), func(): actor.jump_requested = true)
	_add_button("Abend", Vector2(-260, -108), func(): world.toggle_dusk())
	_add_button("+", Vector2(-138, -170), func(): world.zoom = clampf(world.zoom - 1.0, 6.8, 9.2))
	_add_button("−", Vector2(-260, -170), func(): world.zoom = clampf(world.zoom + 1.0, 6.8, 9.2))
	queue_redraw()
	resized.connect(queue_redraw)

func _add_button(text: String, offset: Vector2, callback: Callable) -> void:
	var button := Button.new()
	button.text = text
	button.set_anchors_preset(Control.PRESET_BOTTOM_RIGHT)
	button.position = offset
	button.size = Vector2(112, 52)
	button.modulate = Color(1, 0.94, 0.81, 0.88)
	button.pressed.connect(callback)
	add_child(button)
	actions.append({"button": button, "callback": callback})

func _input(event: InputEvent) -> void:
	if event is InputEventScreenTouch:
		if event.pressed:
			for action in actions:
				if action.button.get_global_rect().has_point(event.position):
					action.callback.call()
					get_viewport().set_input_as_handled()
					return
		if event.pressed and finger == -1 and event.position.x < size.x * 0.45 and event.position.y > size.y * 0.50:
			finger = event.index
			origin = event.position
			thumb = origin
		elif not event.pressed and event.index == finger:
			finger = -1
			actor.touch_vector = Vector2.ZERO
		queue_redraw()
	elif event is InputEventScreenDrag and event.index == finger:
		var displacement: Vector2 = (event.position - origin).limit_length(RADIUS)
		thumb = origin + displacement
		actor.touch_vector = displacement / RADIUS
		queue_redraw()
	elif event is InputEventMouseButton and event.pressed:
		if event.button_index == MOUSE_BUTTON_WHEEL_UP:
			world.zoom = maxf(world.zoom - 0.6, 6.8)
		elif event.button_index == MOUSE_BUTTON_WHEEL_DOWN:
			world.zoom = minf(world.zoom + 0.6, 9.2)

func _notification(what: int) -> void:
	if what == NOTIFICATION_APPLICATION_FOCUS_OUT and is_instance_valid(actor):
		finger = -1
		actor.touch_vector = Vector2.ZERO
		queue_redraw()

func _draw() -> void:
	var center := origin if finger >= 0 else Vector2(112, size.y - 108)
	draw_circle(center, RADIUS, Color(0.16, 0.15, 0.12, 0.24))
	draw_arc(center, RADIUS, 0, TAU, 64, Color(1, 0.94, 0.79, 0.6), 2.0, true)
	draw_circle(thumb if finger >= 0 else center, 23, Color(1, 0.94, 0.79, 0.55))
