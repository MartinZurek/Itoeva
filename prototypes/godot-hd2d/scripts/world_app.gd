extends Node3D

const Catalog = preload("res://scripts/world_catalog.gd")
const Stage = preload("res://scripts/world_stage.gd")
const Actor = preload("res://scripts/character_actor.gd")
const Hud = preload("res://scripts/adventure_hud.gd")
const SAVE_PATH := "user://painted_world_v2.json"
var save_path := SAVE_PATH
var stage: Node3D
var actor: CharacterBody3D
var actors: Array[CharacterBody3D] = []
var camera: Camera3D
var hud: Control
var ui: Control
var canvas: CanvasLayer
var game_theme: Theme
var menu_buttons: Array[Button] = []
var portrait_cards: Array[Dictionary] = []
var status_label: Label
var tagline_label: Label
var selected := "fennec"
var room := "OUTDOOR"
var paused := true
var world_ready := false
var loading_world := false
var switching := false
var textures: Dictionary = {}
var pending: Array[String] = []
var ready_count := 0
var yaw := 0.0
var zoom := 7.6
var camera_anchor := Vector3.ZERO
var dusk := false
var sensitivity := 1.0
var fixed_stick := false
var music_on := false
var music_volume := 0.35
var save_data: Dictionary = {}
var game_clock := 0.0
var save_clock := 0.0
var music: AudioStreamPlayer
var notice := ""
var notice_clock := 0.0

func _ready() -> void:
	_load_save()
	_bind_inputs()
	game_theme = _make_theme()
	camera = Camera3D.new()
	camera.projection = Camera3D.PROJECTION_ORTHOGONAL
	camera.size = 7.6
	camera.current = true
	add_child(camera)
	var ambient := WorldEnvironment.new()
	var environment := Environment.new()
	environment.background_mode = Environment.BG_COLOR
	environment.background_color = Color("302922")
	environment.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	environment.ambient_light_color = Color("fff0d2")
	environment.ambient_light_energy = 0.7
	ambient.environment = environment
	add_child(ambient)
	canvas = CanvasLayer.new()
	add_child(canvas)
	music = AudioStreamPlayer.new()
	music.stream = load("res://assets/music/theme.ogg")
	music.finished.connect(func():
		if music_on and not paused:
			music.play())
	add_child(music)
	show_menu()
	_preload_world()

func _bind_inputs() -> void:
	var bindings := {"move_left": [KEY_A, KEY_LEFT], "move_right": [KEY_D, KEY_RIGHT],
		"move_back": [KEY_W, KEY_UP], "move_front": [KEY_S, KEY_DOWN], "sprint": [KEY_SHIFT],
		"jump": [KEY_SPACE], "roll": [KEY_R], "sit": [KEY_C], "interact": [KEY_E],
		"orbit_left": [KEY_Q], "orbit_right": [KEY_F], "zoom_in": [KEY_Z], "zoom_out": [KEY_X],
		"menu": [KEY_ESCAPE], "map": [KEY_M], "dusk": [KEY_L]}
	for action in bindings:
		if not InputMap.has_action(action):
			InputMap.add_action(action, 0.14)
		for key in bindings[action]:
			var event := InputEventKey.new()
			event.physical_keycode = key
			if not InputMap.action_has_event(action, event):
				InputMap.action_add_event(action, event)
	for entry in [["move_left", JOY_AXIS_LEFT_X, -1.0], ["move_right", JOY_AXIS_LEFT_X, 1.0],
		["move_back", JOY_AXIS_LEFT_Y, -1.0], ["move_front", JOY_AXIS_LEFT_Y, 1.0]]:
		var axis := InputEventJoypadMotion.new()
		axis.axis = entry[1]
		axis.axis_value = entry[2]
		if not InputMap.action_has_event(entry[0], axis):
			InputMap.action_add_event(entry[0], axis)
	for entry in [["jump", JOY_BUTTON_A], ["roll", JOY_BUTTON_B], ["sit", JOY_BUTTON_Y], ["interact", JOY_BUTTON_X], ["menu", JOY_BUTTON_START]]:
		var button := InputEventJoypadButton.new()
		button.button_index = entry[1]
		if not InputMap.action_has_event(entry[0], button):
			InputMap.action_add_event(entry[0], button)

func _make_theme() -> Theme:
	var theme := Theme.new()
	theme.default_font_size = 18
	for state in ["normal", "hover", "pressed", "disabled", "focus"]:
		var box := StyleBoxFlat.new()
		box.bg_color = Color(0.06, 0.12, 0.12, 0.76) if state != "pressed" else Color(0.19, 0.34, 0.27, 0.92)
		box.border_color = Color(0.79, 0.68, 0.46, 0.82) if state != "hover" else Color("ffdc8e")
		box.set_border_width_all(1 if state != "focus" else 2)
		box.set_corner_radius_all(16)
		box.content_margin_left = 18
		box.content_margin_right = 18
		box.content_margin_top = 10
		box.content_margin_bottom = 10
		theme.set_stylebox(state, "Button", box)
	for state in ["font_color", "font_hover_color", "font_pressed_color"]:
		theme.set_color(state, "Button", Color("f5e6c5"))
	return theme

func _new_ui() -> Control:
	if is_instance_valid(ui):
		ui.queue_free()
	menu_buttons.clear()
	portrait_cards.clear()
	ui = Control.new()
	ui.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	ui.theme = game_theme
	canvas.add_child(ui)
	return ui

func _label(text: String, size: int = 20) -> Label:
	var label := Label.new()
	label.text = text
	label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	label.add_theme_font_size_override("font_size", size)
	label.add_theme_color_override("font_color", Color("f5e6c5"))
	return label

func _button(text: String, callback: Callable) -> Button:
	var button := Button.new()
	button.text = text
	button.custom_minimum_size = Vector2(140, 52)
	button.pressed.connect(callback)
	menu_buttons.append(button)
	return button

func _panel(title: String) -> VBoxContainer:
	_new_ui()
	var shade := ColorRect.new()
	shade.color = Color(0.015, 0.025, 0.022, 0.84)
	shade.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	ui.add_child(shade)
	var center := CenterContainer.new()
	center.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	ui.add_child(center)
	var panel := PanelContainer.new()
	var box := StyleBoxFlat.new()
	box.bg_color = Color(0.04, 0.075, 0.07, 0.94)
	box.border_color = Color("af9970")
	box.set_border_width_all(1)
	box.set_corner_radius_all(22)
	box.content_margin_left = 28
	box.content_margin_right = 28
	box.content_margin_top = 20
	box.content_margin_bottom = 20
	panel.add_theme_stylebox_override("panel", box)
	center.add_child(panel)
	var column := VBoxContainer.new()
	column.add_theme_constant_override("separation", 12)
	panel.add_child(column)
	column.add_child(_label(title, 30))
	return column

func _pause(value: bool) -> void:
	paused = value
	for creature in actors:
		creature.enabled = not value
		creature.stop_input()
	if is_instance_valid(hud):
		hud.set_enabled(not value)
	_update_music()

func show_menu() -> void:
	_save_progress()
	_pause(true)
	_new_ui()
	var backdrop := TextureRect.new()
	backdrop.texture = load("res://assets/interiors/living.png")
	backdrop.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
	backdrop.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_COVERED
	backdrop.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	backdrop.mouse_filter = Control.MOUSE_FILTER_IGNORE
	ui.add_child(backdrop)
	var shade := ColorRect.new()
	shade.color = Color(0.025, 0.025, 0.02, 0.68)
	shade.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	shade.mouse_filter = Control.MOUSE_FILTER_IGNORE
	ui.add_child(shade)
	var center := CenterContainer.new()
	center.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	ui.add_child(center)
	var column := VBoxContainer.new()
	column.add_theme_constant_override("separation", 12)
	center.add_child(column)
	column.add_child(_label("ITOEVA", 42))
	column.add_child(_label("Wähle dein Wesen · Gemalte Welt", 19))
	var grid := GridContainer.new()
	grid.columns = 3
	grid.add_theme_constant_override("h_separation", 12)
	grid.add_theme_constant_override("v_separation", 12)
	column.add_child(grid)
	for species in Catalog.data().characters:
		var reference: Dictionary = Catalog.data().characters[species]
		var card := _button("", func(): _select_species(species))
		card.custom_minimum_size = Vector2(174, 156)
		var content := VBoxContainer.new()
		content.mouse_filter = Control.MOUSE_FILTER_IGNORE
		content.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
		content.offset_top = 9
		content.offset_bottom = -7
		card.add_child(content)
		var image := TextureRect.new()
		var atlas := AtlasTexture.new()
		atlas.atlas = load(reference.asset)
		atlas.region = Rect2(0, 0, 128, 128)
		image.texture = atlas
		image.texture_filter = CanvasItem.TEXTURE_FILTER_NEAREST
		image.custom_minimum_size = Vector2(154, 104)
		image.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
		image.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_CENTERED
		image.mouse_filter = Control.MOUSE_FILTER_IGNORE
		content.add_child(image)
		var label := _label(reference.label, 17)
		label.mouse_filter = Control.MOUSE_FILTER_IGNORE
		content.add_child(label)
		grid.add_child(card)
		portrait_cards.append({"species": species, "button": card, "atlas": atlas})
	tagline_label = _label("", 17)
	column.add_child(tagline_label)
	var buttons := HBoxContainer.new()
	buttons.alignment = BoxContainer.ALIGNMENT_CENTER
	buttons.add_theme_constant_override("separation", 12)
	column.add_child(buttons)
	buttons.add_child(_button("Einstellungen", show_settings))
	buttons.add_child(_button("Fortsetzen" if world_ready else "Spielen", start_game))
	status_label = _label("", 14)
	column.add_child(status_label)
	_select_species(selected)
	if world_ready:
		status_label.text = "28 Orte · 6 Wesen · Links bewegen, rechts handeln"

func _select_species(species: String) -> void:
	selected = species
	for card in portrait_cards:
		card.button.modulate = Color("ffdf9c") if card.species == selected else Color.WHITE
	if is_instance_valid(tagline_label):
		tagline_label.text = Catalog.data().characters[selected].tagline

func show_settings() -> void:
	_pause(true)
	var column := _panel("Einstellungen")
	column.add_child(_button("Musik: an" if music_on else "Musik: aus", func():
		music_on = not music_on
		_save_preferences()
		show_settings()))
	var volume_row := HBoxContainer.new()
	volume_row.alignment = BoxContainer.ALIGNMENT_CENTER
	column.add_child(volume_row)
	volume_row.add_child(_button("Leiser", func(): music_volume = maxf(0, music_volume - 0.1); _save_preferences(); show_settings()))
	volume_row.add_child(_label("Musik %s %%" % roundi(music_volume * 100), 18))
	volume_row.add_child(_button("Lauter", func(): music_volume = minf(1, music_volume + 0.1); _save_preferences(); show_settings()))
	column.add_child(_button("Stick: fest" if fixed_stick else "Stick: am Daumen", func():
		fixed_stick = not fixed_stick
		_save_preferences()
		show_settings()))
	var stick_row := HBoxContainer.new()
	column.add_child(stick_row)
	stick_row.add_child(_button("Sanfter", func(): sensitivity = maxf(0.7, sensitivity - 0.1); _save_preferences(); show_settings()))
	stick_row.add_child(_label("Empfindlichkeit %s %%" % roundi(sensitivity * 100), 18))
	stick_row.add_child(_button("Direkter", func(): sensitivity = minf(1.5, sensitivity + 0.1); _save_preferences(); show_settings()))
	column.add_child(_button("Abendlicht" if not dusk else "Tageslicht", func():
		toggle_dusk()
		_save_preferences()
		show_settings()))
	column.add_child(_label("WASD / Stick · Shift rennen · Leertaste springen\nR Rolle · C Sitzen · E Interaktion · M Karte\nQ/F schwenken · Z/X zoomen · Esc Menü", 16))
	column.add_child(_button("Zurück", show_menu))

func _preload_world() -> void:
	for region in Catalog.data().regions:
		pending.append(region.asset)
	for i in range(6):
		pending.append("res://assets/world/seams/%s.png" % i)
	for entry in Catalog.data().interiors.values():
		pending.append(entry.asset)
	for entry in Catalog.data().characters.values():
		pending.append(entry.asset)
	for asset in pending:
		var error := ResourceLoader.load_threaded_request(asset, "Texture2D")
		if error != OK:
			push_error("Bild konnte nicht vorbereitet werden: " + asset)
	loading_world = true

func _poll_loading() -> void:
	ready_count = 0
	for asset in pending:
		if textures.has(asset):
			ready_count += 1
			continue
		var state := ResourceLoader.load_threaded_get_status(asset)
		if state == ResourceLoader.THREAD_LOAD_LOADED:
			if not textures.has(asset):
				textures[asset] = ResourceLoader.load_threaded_get(asset)
			ready_count += 1
		elif state == ResourceLoader.THREAD_LOAD_FAILED:
			loading_world = false
			if is_instance_valid(status_label):
				status_label.text = "Ein Bild fehlt. Bitte den Projektimport prüfen."
			return
	if is_instance_valid(status_label):
		status_label.text = "Die Welt wird vorbereitet … %s %%" % roundi(float(ready_count) / pending.size() * 100)
	if ready_count == pending.size():
		loading_world = false
		if is_instance_valid(status_label):
			status_label.text = "Bereit · 28 Orte · 6 Wesen"

func start_game() -> void:
	if loading_world or switching:
		return
	_save_preferences()
	if world_ready and actor.species == selected:
		_resume()
		return
	var saved: Dictionary = save_data.get("characters", {}).get(selected, {})
	var start_room: String = saved.get("room", "OUTDOOR")
	if start_room != "OUTDOOR" and not Catalog.data().interiors.has(start_room):
		start_room = "OUTDOOR"
	var position := Catalog.position_for("PARK", Vector2(0.48, 0.6))
	if saved.get("position") is Array and saved.position.size() == 3:
		position = Vector3(float(saved.position[0]), 0.03, float(saved.position[2]))
	await _mount_world(start_room, position)
	_resume()

func _mount_world(next_room: String, position: Vector3) -> void:
	switching = true
	_pause(true)
	for creature in actors:
		creature.queue_free()
	actors.clear()
	if is_instance_valid(stage):
		stage.queue_free()
	if is_instance_valid(hud):
		hud.queue_free()
	# Alte Physikkörper verschwinden vor dem neuen Spawn aus dem selben Raum.
	await get_tree().process_frame
	room = next_room
	stage = Stage.new()
	stage.place = room
	stage.textures = textures
	add_child(stage)
	stage.build()
	stage.set_dusk(dusk)
	actor = Actor.new()
	actor.species = selected
	actor.enabled = false
	add_child(actor)
	actor.set_world_scale(stage.base_pixel_size, stage.jump_height)
	stage.fit_character(actor.sprite.pixel_size * (1.032967 if selected == "wyrmling" else 1.0))
	actor.position = stage.safe_position(position, actor.collider.shape.radius)
	actors.append(actor)
	if room == "OUTDOOR":
		# Alle anderen Originalwesen sind sichtbar; noch keine LAS-Entscheidungsmigration.
		var anchors := {"fennec": Vector3(72, 0.03, 0.2), "gloop": Vector3(68, 0.03, 0.2),
			"puffling": Vector3(75, 0.03, -0.1), "wyrmling": Vector3(78, 0.03, 0.5),
			"starlet": Vector3(80, 0.03, -0.3), "hootlet": Vector3(70, 0.03, 0.4)}
		for species in Catalog.data().characters:
			if species == selected:
				continue
			var resident := Actor.new()
			resident.species = species
			resident.player_controlled = false
			resident.enabled = false
			add_child(resident)
			resident.position = stage.safe_position(anchors[species])
			resident.set_meta("anchor", resident.position)
			actors.append(resident)
	for creature in actors:
		creature.sprite.modulate = Color(0.88, 0.78, 0.75) if dusk and room == "OUTDOOR" else Color.WHITE
	zoom = stage.default_zoom
	camera.size = zoom
	yaw = 0.0
	camera_anchor = Vector3(actor.position.x, 3.0, 0.0)
	hud = Hud.new()
	hud.app = self
	hud.actor = actor
	hud.sensitivity = sensitivity
	hud.fixed_stick = fixed_stick
	canvas.add_child(hud)
	hud.set_enabled(false)
	world_ready = true
	switching = false
	_update_camera(10.0)

func _resume() -> void:
	if not world_ready:
		return
	if is_instance_valid(ui):
		ui.queue_free()
	menu_buttons.clear()
	portrait_cards.clear()
	hud.sensitivity = sensitivity
	hud.fixed_stick = fixed_stick
	_pause(false)

func interact() -> void:
	if paused or switching or not actor.is_on_floor():
		return
	var door: Dictionary = stage.nearest_door(actor.position)
	if not door.is_empty():
		var arrival: Vector2 = door.arrival
		arrival = Vector2(clampf(arrival.x, 0.06, 0.94), clampf(arrival.y, 0.12, 0.88))
		_save_progress()
		await _mount_world("OUTDOOR" if not Catalog.region_for(door.to).is_empty() else door.to, Catalog.position_for(door.to, arrival))
		_resume()
		_save_progress()
		return
	for creature in actors:
		if creature != actor and creature.position.distance_to(actor.position) < 2.0:
			creature.facing = Vector2(0, 1)
			creature.npc_vector = Vector2.ZERO
			creature.set_meta("rest_until", game_clock + 3.5)
			creature.request_action("sit")
			_notice(Catalog.data().characters[creature.species].tagline)
			return
	for item in stage.furniture:
		if item.seat != null and actor.position.distance_to(item.world_center) < (4.0 if room != "OUTDOOR" else 1.8):
			actor.mount_seat(item.seat_position, actor.position)
			_notice("Ein ruhiger Moment.")
			return
	_notice("Gehe zu einer Tür, einem Wesen oder einem Sitzplatz.")

func show_map() -> void:
	if not world_ready:
		return
	_pause(true)
	var column := _panel("Meine Welt · " + Catalog.data().labels[current_place()])
	column.add_child(_label("Die verbundenen Außenorte und ihre Innenräume", 16))
	for region in Catalog.data().regions:
		var names := PackedStringArray()
		for place in region.places:
			var name: String = Catalog.data().labels[place]
			names.append("● " + name if place == current_place() else name)
		column.add_child(_label("  →  ".join(names), 18))
	column.add_child(_label("Haus: Wohnzimmer · Schlafzimmer · Bad · Schreibzimmer\nKüche · Werkstatt · Leseecke\nStraße: Laden · Marktplatz: Café · Arbeit · Spielhalle", 16))
	column.add_child(_button("Zurück zur Welt", _resume))

func current_place() -> String:
	return Catalog.place_at(actor.position.x) if room == "OUTDOOR" else room

func toggle_dusk() -> void:
	dusk = not dusk
	if is_instance_valid(stage):
		stage.set_dusk(dusk)
	for creature in actors:
		creature.sprite.modulate = Color(0.88, 0.78, 0.75) if dusk and room == "OUTDOOR" else Color.WHITE

func adjust_zoom(amount: float) -> void:
	if is_instance_valid(stage):
		zoom = clampf(zoom + amount, stage.zoom_min, stage.zoom_max)

func _update_camera(delta: float) -> void:
	if not world_ready:
		return
	camera.size = lerpf(camera.size, zoom, 1.0 - exp(-delta * 5.0))
	var half_width := camera.size * get_viewport().get_visible_rect().size.aspect() * 0.5
	var left: float = 0.0 if room == "OUTDOOR" else -stage.width * 0.5
	var right: float = stage.width if room == "OUTDOOR" else stage.width * 0.5
	var target_x: float = clampf(actor.position.x, left + half_width, right - half_width) if right - left > half_width * 2 else (left + right) * 0.5
	var pitch := deg_to_rad(Catalog.PITCH)
	var angle := deg_to_rad(yaw)
	var center := camera.size * 0.5 - 1.4 + absf(sin(angle)) * half_width * sin(pitch)
	var foot_up := actor.position.y * cos(pitch) - sin(pitch) * (sin(angle) * (actor.position.x - target_x) + cos(angle) * actor.position.z)
	center = minf(center, foot_up + camera.size * 0.5 - 0.45)
	var target := Vector3(target_x, center / cos(pitch), 0)
	camera_anchor = camera_anchor.lerp(target, 1.0 - exp(-delta * 4.5))
	var live_foot := actor.position.y * cos(pitch) - sin(pitch) * (sin(angle) * (actor.position.x - camera_anchor.x) + cos(angle) * actor.position.z)
	camera_anchor.y = minf(camera_anchor.y, (live_foot + camera.size * 0.5 - 0.45) / cos(pitch))
	camera.position = camera_anchor + Vector3(0, sin(pitch) * 30, cos(pitch) * 30).rotated(Vector3.UP, angle)
	camera.look_at(camera_anchor)
	stage.update_visible(actor.position.x)

func _notice(text: String) -> void:
	notice = text
	notice_clock = 3.5

func _process(delta: float) -> void:
	game_clock += delta
	if loading_world:
		_poll_loading()
	for card in portrait_cards:
		if is_instance_valid(card.button):
			var frame := int(game_clock / 0.42) % 8
			card.atlas.region = Rect2(frame * 128, 0, 128, 128)
	if not world_ready or paused or switching:
		return
	save_clock += delta
	if save_clock > 5.0:
		save_clock = 0.0
		_save_progress()
	yaw = clampf(yaw + Input.get_axis("orbit_left", "orbit_right") * delta * 12.0, -8.0, 8.0)
	if not Input.get_connected_joypads().is_empty():
		yaw = clampf(yaw + Input.get_joy_axis(0, JOY_AXIS_RIGHT_X) * delta * 16.0, -8, 8)
	adjust_zoom(Input.get_axis("zoom_in", "zoom_out") * delta * 2.5)
	if Input.is_action_just_pressed("dusk"):
		toggle_dusk()
	if Input.is_action_just_pressed("interact"):
		interact()
	if Input.is_action_just_pressed("map"):
		show_map()
	for i in actors.size():
		var creature := actors[i]
		if creature != actor and creature.has_meta("anchor"):
			if game_clock < float(creature.get_meta("rest_until", 0.0)):
				creature.npc_vector = Vector2.ZERO
				continue
			var anchor: Vector3 = creature.get_meta("anchor")
			var target_x := anchor.x + sin(game_clock * 0.18 + float(i) * 1.4) * 1.1
			creature.npc_vector = Vector2(clampf((target_x - creature.position.x) * 0.35, -0.45, 0.45), 0.0)
	_update_camera(delta)
	notice_clock = maxf(0, notice_clock - delta)
	hud.caption.text = Catalog.data().characters[selected].label + " · " + Catalog.data().labels[current_place()]
	var door: Dictionary = stage.nearest_door(actor.position)
	hud.interact_button.text = "Betreten" if not door.is_empty() else "Interaktion"
	hud.hint.text = notice if notice_clock > 0 else "→ " + Catalog.data().labels[door.to] if not door.is_empty() else "Links: Gehen → Rennen · Rechts: Sprung / Rolle / Sitzen"

func _input(event: InputEvent) -> void:
	if event.is_action_pressed("menu"):
		if paused and world_ready:
			_resume()
		else:
			show_menu()
		get_viewport().set_input_as_handled()
	if event is InputEventScreenTouch and event.pressed and not event.canceled and paused:
		for button in menu_buttons:
			if is_instance_valid(button) and button.is_visible_in_tree() and button.get_global_rect().has_point(event.position):
				button.pressed.emit()
				get_viewport().set_input_as_handled()
				return

func _load_save() -> void:
	if FileAccess.file_exists(save_path):
		var parsed = JSON.parse_string(FileAccess.get_file_as_string(save_path))
		if parsed is Dictionary:
			save_data = parsed
	var settings: Dictionary = save_data.get("settings", {})
	selected = settings.get("species", "fennec")
	if not Catalog.data().characters.has(selected):
		selected = "fennec"
	music_on = bool(settings.get("music_on", false))
	music_volume = clampf(float(settings.get("music_volume", 0.35)), 0, 1)
	sensitivity = clampf(float(settings.get("sensitivity", 1.0)), 0.7, 1.5)
	fixed_stick = bool(settings.get("fixed_stick", false))
	dusk = bool(settings.get("dusk", false))

func _save_preferences() -> void:
	save_data["settings"] = {"species": selected, "music_on": music_on, "music_volume": music_volume,
		"sensitivity": sensitivity, "fixed_stick": fixed_stick, "dusk": dusk}
	_write_save()
	_update_music()

func _save_progress() -> void:
	if not world_ready or switching or not is_instance_valid(actor):
		return
	if not save_data.has("characters"):
		save_data["characters"] = {}
	save_data.characters[actor.species] = {"room": room, "position": [actor.position.x, 0.03, actor.position.z]}
	_write_save()

func _write_save() -> void:
	var file := FileAccess.open(save_path + ".tmp", FileAccess.WRITE)
	if file == null:
		push_error("Lokaler Spielstand kann nicht gespeichert werden")
		return
	file.store_string(JSON.stringify(save_data))
	file.close()
	var error := DirAccess.rename_absolute(save_path + ".tmp", save_path)
	if error != OK:
		push_error("Lokaler Spielstand konnte nicht ersetzt werden")

func _update_music() -> void:
	if not is_instance_valid(music):
		return
	music.volume_db = linear_to_db(maxf(music_volume, 0.001))
	if music_on and not paused:
		if not music.playing:
			music.play()
	else:
		music.stop()

func _notification(what: int) -> void:
	if what == NOTIFICATION_WM_GO_BACK_REQUEST:
		show_menu() if not paused else _resume()
	elif what == NOTIFICATION_APPLICATION_FOCUS_OUT:
		_save_progress()
		_pause(true)
		show_menu()
