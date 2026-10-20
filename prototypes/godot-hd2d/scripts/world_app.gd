extends Node3D

const Catalog = preload("res://scripts/world_catalog.gd")
const Stage = preload("res://scripts/world_stage.gd")
const ForestStage = preload("res://scripts/forest_stage.gd")
const Forest = preload("res://scripts/forest_catalog.gd")
const Encounter = preload("res://scripts/card_encounter.gd")
const DeckEditor = preload("res://scripts/deck_editor.gd")
const Cards = preload("res://scripts/card_catalog.gd")
var forest_encounter: Node

const Actor = preload("res://scripts/character_actor.gd")
const CompactActor = preload("res://scripts/compact_actor.gd")
const Residents = preload("res://scripts/world_residents.gd")
const CitizenActor = preload("res://scripts/citizen_actor.gd")
const EnemyLife = preload("res://scripts/enemy_life.gd")
const Enemies = preload("res://scripts/regional_enemies.gd")
const RegionalEncounter = preload("res://scripts/regional_encounter.gd")
var regional_markers: Array[Dictionary] = []
var resident_appointments: Dictionary = {}
const WorldMap = preload("res://scripts/illustrated_map.gd")
const Hud = preload("res://scripts/adventure_hud.gd")
const Clock = preload("res://scripts/world_clock.gd")
const InteractionPath = preload("res://scripts/interaction_path.gd")
const SAVE_PATH := "user://painted_world_v2.json"
var save_path := SAVE_PATH
var stage: Node3D
var actor: CharacterBody3D
var exploration_stamina = preload("res://scripts/movement_stamina.gd").new()
var actors: Array[CharacterBody3D] = []
var camera: Camera3D
var hud: Control
var ui: Control
var canvas: CanvasLayer
var game_theme: Theme
var menu_buttons: Array[Button] = []
var deck_editor: RefCounted
var portrait_cards: Array[Dictionary] = []
var status_label: Label
var tagline_label: Label
var selected := "mosslyn"
var room := "OUTDOOR"
var paused := true
var world_ready := false
var loading_world := false
var switching := false
var textures: Dictionary = {}
var character_metrics: Dictionary = {}
var pending: Array[String] = []
var ready_count := 0
var yaw := 0.0
var zoom := 7.6
var camera_anchor := Vector3.ZERO
var camera_lead := 0.0
var dusk := false
var sensitivity := 1.0
var fixed_stick := false
var battle_music_on := true
var effects_on := true
var effects_volume := 0.55
var battle_audio: Node
var music_on := false
var music_volume := 0.35
var save_data: Dictionary = {}
var game_clock := 0.0
var save_clock := 0.0
var music: AudioStreamPlayer
var notice := ""
var notice_clock := 0.0
var world_clock = Clock.new()
var pending_interaction: Dictionary = {}
var interaction_route := PackedVector3Array()
var approach_elapsed := 0.0
var dialog_open := false
var clock_tick := 0.0

func _ready() -> void:
	for region in Forest.data().segments: Catalog.data().labels[region.id]=region.label
	Catalog.data().labels["FOREST_TRAIL"]="Waldpfad zum Schattenwald"
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
	battle_audio=load("res://scripts/battle_audio.gd").new()
	battle_audio.app=self
	add_child(battle_audio)
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
	if value: cancel_approach()
	paused = value
	for creature in actors:
		creature.enabled = not value
		creature.stop_input()
	if is_instance_valid(hud):
		hud.set_enabled(not value)
	_update_music()

func show_menu() -> void:
	if is_instance_valid(forest_encounter): forest_encounter.suspend()
	dialog_open = false
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
	grid.columns = 4
	grid.add_theme_constant_override("h_separation", 12)
	grid.add_theme_constant_override("v_separation", 12)
	column.add_child(grid)
	for species in Catalog.STARTERS:
		var reference: Dictionary = Catalog.data().characters[species]
		var card := _button("", func(): _select_species(species))
		card.custom_minimum_size = Vector2(160, 156)
		var content := VBoxContainer.new()
		content.mouse_filter = Control.MOUSE_FILTER_IGNORE
		content.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
		content.offset_top = 9
		content.offset_bottom = -7
		card.add_child(content)
		var image := TextureRect.new()
		var atlas := Catalog.portrait(species)
		image.texture = atlas
		image.texture_filter = CanvasItem.TEXTURE_FILTER_LINEAR
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
	buttons.add_child(_button("Deck bauen", show_deck))
	buttons.add_child(_button("Fortsetzen" if world_ready else "Spielen", start_game))
	status_label = _label("", 14)
	column.add_child(status_label)
	_select_species(selected)
	if world_ready:
		status_label.text = "4 Startwesen · Lebendige Welt · Links bewegen, rechts handeln"

func _select_species(species: String) -> void:
	if species not in Catalog.STARTERS: return
	selected = species
	for card in portrait_cards:
		card.button.modulate = Color("ffdf9c") if card.species == selected else Color.WHITE
	if is_instance_valid(tagline_label):
		tagline_label.text = Catalog.data().characters[selected].tagline

func show_deck() -> void:
	# Signal-Callables halten RefCounted nicht am Leben. Die Detailseite hatte
	# nach dem Freigeben des Editors einen Schliessen-Knopf ohne Verbindung.
	deck_editor = DeckEditor.new()
	deck_editor.app = self
	deck_editor.species = selected
	deck_editor.open()

func show_settings() -> void:
	_pause(true)
	var column := _panel("Einstellungen")
	var audio_row:=HBoxContainer.new();column.add_child(audio_row)
	audio_row.add_child(_button("Weltmusik: an" if music_on else "Weltmusik: aus", func():
		music_on = not music_on
		_save_preferences()
		show_settings()))
	audio_row.add_child(_button("Kampfklang", show_audio_settings))
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
	var time_row := HBoxContainer.new()
	column.add_child(time_row)
	time_row.add_child(_button("Ortszeit · " + clock_label(), func():
		world_clock.reset_to_real_time()
		dusk = false
		_apply_world_time()
		show_settings()))
	time_row.add_child(_button("Abendlicht ansehen" if not dusk else "Ortszeit anzeigen", func():
		toggle_dusk()
		_save_preferences()
		show_settings()))
	var view_row := HBoxContainer.new()
	column.add_child(view_row)
	view_row.add_child(_button("Ansicht näher", func(): adjust_zoom(-0.5)))
	view_row.add_child(_button("Ansicht weiter", func(): adjust_zoom(0.5)))
	column.add_child(_label("WASD / Stick · Shift rennen · Leertaste springen\nR Rolle · C Sitzen · E Interaktion · M Karte\nQ/F schwenken · Z/X zoomen · Esc Menü", 16))
	column.add_child(_button("Zurück", show_menu))

func _preload_world() -> void:
	pending.append("res://assets/world/path-stones.png")
	for asset in ["citizens","enemies","world-atlas","miro-packed","mill-brook","glut-imp","seal-weaver","tide-crab","resin-beetle","cinder-carrier","vharok","ash-ascent","seal-court"]: pending.append("res://assets/expansion/"+asset+".png")
	for region in Catalog.data().regions:
		pending.append(region.asset)
	for region in Forest.data().segments: pending.append(region.asset)
	pending.append("res://assets/combat/nachtwurz-atlas.png")
	pending.append("res://assets/combat/flame-atlas.png")
	for id in Catalog.data().characters:
		var combat_asset: String=Catalog.data().characters[id].get("combat_asset","res://assets/combat/"+id+"-combat.png")
		if combat_asset not in pending:pending.append(combat_asset)
	pending.append("res://assets/combat/card-art-v050.png")
	pending.append("res://assets/combat/spirits-v050.png")
	pending.append("res://assets/combat/spirit-animation-v050.png")
	for i in range(1,5):pending.append("res://assets/combat/helpers-v060-%s.png" % i)
	for i in range(6):
		pending.append("res://assets/world/seams/%s.png" % i)
	for entry in Catalog.data().interiors.values():
		if entry.asset not in pending:pending.append(entry.asset)
	for entry in Catalog.data().characters.values():
		if entry.asset not in pending:pending.append(entry.asset)
		if entry.has("run_asset") and entry.run_asset not in pending:pending.append(entry.run_asset)
		if entry.has("walk_asset") and entry.walk_asset not in pending:pending.append(entry.walk_asset)
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
			status_label.text = "Bereit · 4 Startwesen · Lebendige Welt"

func start_game() -> void:
	if loading_world or switching:
		return
	world_clock.reset_to_real_time()
	dusk = false
	_save_preferences()
	if world_ready and actor.species == selected:
		_resume()
		return
	var saved: Dictionary = save_data.get("characters", {}).get(selected, {})
	var start_room: String = saved.get("room", "OUTDOOR")
	if start_room not in ["OUTDOOR","FOREST_TRAIL"] and not Catalog.data().interiors.has(start_room):
		start_room = "OUTDOOR"
	var position := Catalog.position_for("PARK", Vector2(0.48, 0.6))
	if saved.get("position") is Array and saved.position.size() == 3:
		position = Vector3(float(saved.position[0]), 0.03, float(saved.position[2]))
	# Nach einer Revierverlegung bleibt ein laufender Kampf fortsetzbar. Siege,
	# Beute und Decks behalten ihren unveränderten Speicherschlüssel.
	for id in Enemies.data():
		var encounter: Dictionary = save_data.get("forest_encounters", {}).get(selected + ":" + id, {})
		if encounter.get("phase", "") not in ["intro", "battle", "victory", "defeat"]: continue
		var profile: Dictionary = Enemies.data()[id]
		var encounter_room: String = "OUTDOOR" if not Catalog.region_for(profile.place).is_empty() else profile.place
		if start_room != encounter_room:
			start_room = encounter_room
			position = Catalog.position_for(profile.place, Vector2(profile.uv[0], profile.uv[1]))
		break
	await _mount_world(start_room, position)
	_resume()

func _mount_world(next_room: String, position: Vector3) -> void:
	if is_instance_valid(forest_encounter):
		forest_encounter.suspend()
		forest_encounter.queue_free()
		forest_encounter=null
	regional_markers.clear()
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
	stage = ForestStage.new() if room=="FOREST_TRAIL" else Stage.new()
	stage.place = room
	stage.textures = textures
	stage.collected = save_data.get("collected", [])
	add_child(stage)
	stage.build()
	_apply_world_time()
	actor = CompactActor.new() if Catalog.data().characters[selected].get("compact",false) else Actor.new()
	actor.species = selected
	# Türwechsel baut die Figur neu auf, aber füllt den Sprintvorrat nicht.
	actor.stamina = exploration_stamina
	actor.metrics = character_metrics
	actor.enabled = false
	add_child(actor)
	actor.set_world_scale(stage.base_pixel_size, stage.jump_height, stage.body_pixel_size)
	stage.fit_character(actor.sprite.pixel_size * 1.18)
	actor.position = stage.safe_position(position, actor.collider.shape.radius)
	actors.append(actor)
	_refresh_residents()
	for creature in actors:
		creature.sprite.modulate = _character_tint()
	zoom = stage.default_zoom
	camera.size = zoom
	yaw = 0.0
	camera_lead = 0.0
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
	_sort_actors()
	if room=="FOREST_TRAIL":
		forest_encounter=Encounter.new()
		forest_encounter.app=self
		add_child(forest_encounter)
		var lamp_button:=Button.new()
		lamp_button.text="Traglicht: an"
		lamp_button.visible=actor.species!="starlet"
		lamp_button.position=Vector2(28,92)
		lamp_button.custom_minimum_size=Vector2(160,44)
		hud.add_child(lamp_button)
		lamp_button.pressed.connect(func():
			stage.carried_light=not stage.carried_light
			lamp_button.text="Traglicht: an" if stage.carried_light else "Traglicht: aus")
		hud.actions.append({"button":lamp_button,"callback":func(): lamp_button.pressed.emit()})
		var practice:=Button.new()
		practice.name="BattlePractice"
		practice.text="Kampf wiederholen"
		practice.position=Vector2(28,143)
		practice.custom_minimum_size=Vector2(184,44)
		hud.add_child(practice)
		practice.visible=false
		practice.pressed.connect(func(): forest_encounter.replay())
		hud.actions.append({"button":practice,"callback":func(): practice.pressed.emit()})
	_mount_regional_enemies()

func _resume() -> void:
	dialog_open = false
	if not world_ready:
		return
	if is_instance_valid(forest_encounter) and forest_encounter.resume(): return
	if is_instance_valid(ui):
		ui.queue_free()
	menu_buttons.clear()
	portrait_cards.clear()
	hud.sensitivity = sensitivity
	hud.fixed_stick = fixed_stick
	_pause(false)

func interaction_targets() -> Array[Dictionary]:
	var targets: Array[Dictionary] = []
	if not is_instance_valid(actor) or not is_instance_valid(stage):
		return targets
	var grounded: bool = actor.is_on_floor() and not actor.mounted
	for door in stage.doors:
		var distance := Vector2(door.position.x - actor.position.x, door.position.z - actor.position.z).length()
		targets.append({"kind": "door", "id": door.from + ":" + door.to, "position": door.marker_position,
			"approach": stage.safe_position(door.position, actor.collider.shape.radius),
			"label": Catalog.data().labels[door.to], "ready": grounded and distance < (1.35 if room == "OUTDOOR" else 2.2), "distance": distance, "door": door})
	for creature in actors:
		if creature == actor:
			continue
		var distance := Vector2(creature.position.x - actor.position.x, creature.position.z - actor.position.z).length()
		targets.append({"kind": "talk", "id": creature.species, "position": creature.position,
			"approach": stage.safe_position(creature.position + (actor.position - creature.position).normalized() * 1.45, actor.collider.shape.radius),
			"label": Residents.data()[creature.species].name + " · Sprechen", "ready": grounded and distance < 2.6 and _conversation_clear(creature), "distance": distance, "creature": creature})
	for marker in regional_markers:
		if marker.get("defeated",false):continue
		var distance: float=Vector2(actor.position.x-marker.position.x,actor.position.z-marker.position.z).length()
		targets.append({"kind":"encounter","id":marker.id,"position":marker.position+Vector3.UP,
			"approach":stage.safe_position(marker.position+Vector3.LEFT*1.5,actor.collider.shape.radius),
			"label":Enemies.data()[marker.id].name+" · Beobachten","ready":grounded and distance<3.0,
			"distance":distance})
	for item in stage.furniture:
		if item.seat == null and not item.id.ends_with("bookcase"): continue
		var approach: Vector3 = stage.safe_position(item.approach_position, actor.collider.shape.radius)
		var distance := Vector2(actor.position.x - approach.x, actor.position.z - approach.z).length()
		var kind := "read" if item.id.ends_with("bookcase") else "seat"
		var label := "Lesen" if kind == "read" else "Hinsetzen"
		if item.id == "bedroom-bed": kind = "sleep"; label = "Schlafen"
		if item.id == "nook-armchair": kind = "read"; label = "Lesen"
		var position: Vector3 = item.get("seat_position", item.world_center) + Vector3.UP * 0.45
		targets.append({"kind": kind, "id": item.id, "position": position, "approach": approach,
			"label": label, "ready": grounded and distance < 1.4 and _interaction_clear(approach),
			"distance": distance, "seat": item, "body_rect": stage.furniture_screen_rect(item, camera)})
	for item in stage.pickups:
		if not item.art.visible: continue
		var distance: float = Vector2(item.position.x - actor.position.x, item.position.z - actor.position.z).length()
		targets.append({"kind": "pickup", "id": item.id, "position": item.position + Vector3.UP * 0.45,
			"approach": item.position, "label": "Aufheben", "ready": grounded and distance < 1.4 and _interaction_clear(item.position), "distance": distance, "pickup": item})
	return targets

func _conversation_clear(creature: CharacterBody3D) -> bool:
	# Die Reichweite endet an Wassergrenzen/Waenden, nicht erst hinter einem Baum.
	return _interaction_clear(creature.global_position)

func _interaction_clear(target: Vector3, exclude: Array[RID] = []) -> bool:
	var query := PhysicsRayQueryParameters3D.create(actor.global_position + Vector3.UP * 0.65, target + Vector3.UP * 0.65, 1, exclude)
	return get_world_3d().direct_space_state.intersect_ray(query).is_empty()

func _sort_actors() -> void:
	var forward := camera.global_basis.z
	var ordered := actors.duplicate()
	ordered.sort_custom(func(a, b):
		var a_depth: float = forward.dot(Vector3(a.position.x, 0, a.position.z))
		var b_depth: float = forward.dot(Vector3(b.position.x, 0, b.position.z))
		return a.species < b.species if is_equal_approx(a_depth, b_depth) else a_depth < b_depth)
	for i in ordered.size(): ordered[i].set_draw_order(10 + i)

func context_target() -> Dictionary:
	if actor.mounted: return {"kind": "stand", "id": "seat", "label": "Aufstehen", "ready": true}
	var available := interaction_targets().filter(func(target): return target.ready)
	# Tueren behalten ihre Prioritaet; danach das wirklich naechste sichtbare Ziel.
	available.sort_custom(func(a, b):
		if a.kind == "door" and b.kind != "door": return true
		if b.kind == "door" and a.kind != "door": return false
		return a.distance < b.distance)
	return {} if available.is_empty() else available[0]

func interact() -> void:
	var target := context_target()
	if target.is_empty():
		_notice("Nähere dich einer Tür, einer Sprechblase oder einem Sitzplatz.")
		return
	await interact_with(target.kind, target.id)

func interact_with(kind: String, id: String) -> void:
	cancel_approach()
	if kind == "stand" and not paused and actor.mounted:
		hud.reset_touch()
		actor.leave_seat()
		return
	if paused or switching or not actor.is_on_floor() or actor.mounted:
		return
	# Der Hinweis und seine Aktion nutzen denselben aktuellen Abstandstest.
	for target in interaction_targets():
		if target.kind != kind or target.id != id or not target.ready:
			continue
		if kind == "door":
			var door: Dictionary = target.door
			var arrival: Vector2 = door.arrival
			arrival = Vector2(clampf(arrival.x, 0.06, 0.94), clampf(arrival.y, 0.12, 0.88))
			_save_progress()
			await _mount_world("OUTDOOR" if not Catalog.region_for(door.to).is_empty() else door.to, Forest.position_for(int(door.get("forest_index",0)),arrival.x,arrival.y) if door.to=="FOREST_TRAIL" else Catalog.position_for(door.to, arrival))
			_resume()
			_save_progress()
		elif kind == "encounter":
			_show_enemy(id)
		elif kind == "talk":
			var creature: CharacterBody3D = target.creature
			var difference := actor.position - creature.position
			creature.facing = Vector2(signf(difference.x), 0) if absf(difference.x) > absf(difference.z) else Vector2(0, signf(difference.z))
			actor.facing = -creature.facing
			creature.set_meta("rest_until", game_clock + 3.5)
			_show_conversation(creature.species, creature.get_meta("appointment",{}))
		elif kind in ["seat", "read", "sleep"]:
			hud.reset_touch()
			if target.seat.seat != null:
				var pelvis: float=target.seat.get("seat_offsets",{}).get(actor.species,0.0)
				actor.mount_seat(target.seat.seat_position-Vector3.UP*actor.sprite.pixel_size*pelvis, actor.position)
				actor.set_meta("mounted_seat",target.seat.id)
				var body: StaticBody3D = target.seat.body
				actor.seat_occlusion_z = body.position.z + body.get_child(0).shape.size.z * 0.5 + 0.08
				actor.facing = Vector2.DOWN
			if kind == "read":
				_show_activity("Ein Kapitel lesen", "Du nimmst dir Zeit für ein Buch. Zwei Stunden vergehen.", "Lesen · +2 Stunden", _finish_reading)
			elif kind == "sleep":
				_show_activity("Ausruhen", "Schlafe bis zum nächsten Morgen um 08:00 Uhr.", "Bis morgens schlafen", _finish_sleeping)
			else: _notice("Ein ruhiger Moment. Bewegen oder ↑, um aufzustehen.")
		elif kind == "pickup":
			target.pickup.art.visible = false
			if not save_data.has("collected"): save_data["collected"] = []
			if id not in save_data.collected: save_data.collected.append(id)
			_write_save()
			_notice("Kleine Entdeckung: Kiesel aufgenommen.")
		return
	_notice("Dieses Ziel ist nicht mehr in Reichweite.")

func show_map() -> void:
	if not world_ready:
		return
	_pause(true)
	_refresh_residents()
	_new_ui()
	var atlas := WorldMap.new()
	atlas.app=self
	atlas.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	ui.add_child(atlas)

func current_place() -> String:
	return stage.location_at(actor.position.x) if room=="FOREST_TRAIL" else Catalog.place_at(actor.position.x) if room == "OUTDOOR" else room

func toggle_dusk() -> void:
	dusk = not dusk
	_apply_world_time()
	for creature in actors:
		creature.sprite.modulate = _character_tint()

func _character_tint() -> Color:
	return Color.WHITE

func adjust_zoom(amount: float) -> void:
	if is_instance_valid(stage):
		zoom = clampf(zoom + amount, stage.zoom_min, stage.zoom_max)

func _update_camera(delta: float) -> void:
	if not world_ready:
		return
	# Die gesamte gemalte Welt teilt ihre gezeichnete Blickachse. Seitliches
	# Schwenken verzerrte auch abseits der Bruecken Baeume und Objektkanten.
	yaw = 0.0
	camera.size = lerpf(camera.size, zoom, 1.0 - exp(-delta * 5.0))
	var half_width := camera.size * get_viewport().get_visible_rect().size.aspect() * 0.5
	var left: float = 0.0 if room in ["OUTDOOR","FOREST_TRAIL"] else -stage.width * 0.5
	var right: float = stage.width if room in ["OUTDOOR","FOREST_TRAIL"] else stage.width * 0.5
	var lead_limit := 0.95 if room == "OUTDOOR" else 0.20
	camera_lead = lerpf(camera_lead, clampf(actor.velocity.x * 0.21, -lead_limit, lead_limit), 1.0 - exp(-delta * 3.5))
	var target_x: float = clampf(actor.position.x + camera_lead, left + half_width, right - half_width) if right - left > half_width * 2 else (left + right) * 0.5
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
	notice_clock = 5.0

func _update_residents(delta: float) -> void:
	for index in actors.size():
		var creature := actors[index]
		if creature != actor and creature.enabled and creature.has_meta("anchor"):
			_stroll_resident(creature, index, delta)

func _stroll_resident(creature: CharacterBody3D, index: int, delta: float) -> void:
	var patrol: Dictionary = creature.get_meta("patrol", {})
	if patrol.is_empty():
		var anchor: Vector3 = creature.get_meta("anchor")
		var reach := float(creature.get_meta("reach",1.0))
		var lower_bound: float = 2.4 if room in ["OUTDOOR","FOREST_TRAIL"] else -stage.width*.5+stage.horizontal_padding+.2
		var upper_bound: float = stage.width-2.4 if room in ["OUTDOOR","FOREST_TRAIL"] else stage.width*.5-stage.horizontal_padding-.2
		patrol = {"left": maxf(lower_bound, anchor.x - reach), "right": minf(upper_bound, anchor.x + reach),
			"direction": 1.0 if index % 2 == 0 else -1.0, "wait_until": 0.0,
			"last_x": creature.position.x, "stalled": 0.0}
		creature.set_meta("patrol", patrol)
	# Nur ein wirklich begonnenes Gespraech haelt einen Nachbarn an.
	# Ein wartender Spieler soll nicht eine ganze Spaziergaengergruppe einfrieren.
	if game_clock < float(creature.get_meta("rest_until", 0.0)) or game_clock < float(patrol.wait_until):
		creature.npc_vector = Vector2.ZERO
		patrol.last_x = creature.position.x
		patrol.stalled = 0.0
		return
	var direction: float = patrol.direction
	var end: float = patrol.right if direction > 0 else patrol.left
	if absf(end - creature.position.x) < 0.22:
		patrol.direction = -direction
		patrol.wait_until = game_clock + 3.5 + float(index % 3) * 1.6
		patrol.stalled = 0.0
		creature.npc_vector = Vector2.ZERO
		return
	# Eine unpassierbare Kulissenecke ist ein Wendepunkt statt ein ewiger Stau.
	if absf(creature.position.x - float(patrol.last_x)) < delta * 0.06:
		patrol.stalled = float(patrol.stalled) + delta
	else:
		patrol.stalled = 0.0
	patrol.last_x = creature.position.x
	if float(patrol.stalled) > 1.6:
		patrol.direction = -direction
		patrol.stalled = 0.0
		direction = -direction
		end = patrol.right if direction > 0 else patrol.left
	var ahead := creature.position.x + direction * minf(0.85, absf(end - creature.position.x))
	var point := _resident_lane_point(creature, ahead, direction)
	var offset := Vector2(point.x - creature.position.x, point.z - creature.position.z)
	# Sanftes Gehen mit artspezifisch gestaffeltem Tempo, ohne Tastatursprint.
	var pace := 0.36 + float(index % 3) * 0.035
	creature.npc_vector = offset.normalized() * pace if offset.length() > 0.035 else Vector2.ZERO

func _resident_lane_point(creature: CharacterBody3D, x: float, direction: float) -> Vector3:
	var radius: float = creature.collider.shape.radius
	var lower := -INF
	var upper := INF
	for at in [x - radius, x, x + radius]:
		var band: Vector2 = stage._limits_at(at)
		lower = maxf(lower, Catalog.image_z(band.x, stage.native_height) + radius + 0.05)
		upper = minf(upper, Catalog.image_z(band.y, stage.native_height) - radius - 0.05)
	var middle := (lower + upper) * 0.5
	var half := maxf(0.0, (upper - lower) * 0.5)
	# Gegenverkehr waehlt gegensaetzliche Spuren. Auf schmalen Stegen wird die
	# Ausweichweite kleiner; die Weltkollision bleibt immer die harte Grenze.
	var lane := middle + direction * minf(0.22, half * 0.65)
	for neighbor in actors:
		if neighbor == creature or not is_instance_valid(neighbor):
			continue
		var gap: Vector3 = neighbor.position - creature.position
		if gap.x * direction < -0.6 or gap.x * direction > 2.2:
			continue
		var clearance: float = radius + neighbor.collider.shape.radius + 0.20
		if absf(gap.z) > clearance + 0.25:
			continue
		var side := direction
		# Ein stehender Spieler bekommt Raum auf der wirklich freien Seite.
		if neighbor == actor and actor.actual_speed < 0.25 and absf(neighbor.position.z - middle) > half * 0.40:
			side = 1.0 if neighbor.position.z <= middle else -1.0
		lane = middle + side * minf(0.60, half * 0.92)
	var point := Vector3(x, 0.03, clampf(lane, lower, maxf(lower, upper)))
	if not stage.is_walkable(point, radius):
		point = stage.safe_position(point, radius)
	return point

func _process(delta: float) -> void:
	game_clock += delta
	if loading_world:
		_poll_loading()
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
	if room=="FOREST_TRAIL":
		stage.update_player(actor.position)
		var practice: Button = hud.get_node("BattlePractice")
		practice.visible = current_place()=="SHADOW_CLEARING" and forest_encounter.model.phase=="done"
		var darkness: float=Forest.region_at(actor.position.x).darkness
		actor.painted_material.set_shader_parameter("scene_light",Color.WHITE.lerp(Color(.33,.43,.58),darkness*.8))
	_update_residents(delta)
	_update_regional_art()
	clock_tick += delta
	if clock_tick > 0.5:
		clock_tick = 0.0
		_apply_world_time()
	_update_camera(delta)
	_sort_actors()
	notice_clock = maxf(0, notice_clock - delta)
	hud.caption.text = Catalog.data().characters[selected].label + " · " + Catalog.data().labels[current_place()] + "   " + clock_label()
	var here := current_place()
	if not save_data.has("discovered"): save_data["discovered"] = []
	if here not in save_data.discovered:
		save_data.discovered.append(here)
		if notice_clock <= 0: _notice("Entdeckt: " + Catalog.data().labels[here])
	var door: Dictionary = stage.nearest_door(actor.position)
	hud.hint.text = notice if notice_clock > 0 else "→ " + Catalog.data().labels[door.to] if not door.is_empty() else "↑ Sprung · ↔ Rolle / Luftwende · ↓ oder Halten: Sitzen · Tippen: angezeigte Aktion"

func _input(event: InputEvent) -> void:
	if event.is_action_pressed("menu"):
		if is_instance_valid(forest_encounter) and forest_encounter.active() and not forest_encounter.suspended:
			show_menu()
		elif paused and world_ready:
			_resume()
		else:
			show_menu()
		get_viewport().set_input_as_handled()
	if event is InputEventScreenTouch and event.pressed and not event.canceled and paused:
		for button in menu_buttons:
			if is_instance_valid(button) and button.is_visible_in_tree() and not button.disabled and button.get_global_rect().has_point(event.position):
				button.pressed.emit()
				get_viewport().set_input_as_handled()
				return

func _load_save() -> void:
	if FileAccess.file_exists(save_path):
		var parsed = JSON.parse_string(FileAccess.get_file_as_string(save_path))
		if parsed is Dictionary:
			save_data = parsed
	var stored_decks = save_data.get("card_decks", {})
	if stored_decks is Dictionary:
		for species in stored_decks:
			if Catalog.data().characters.has(species) and stored_decks[species] is Array:
				stored_decks[species] = Cards.upgrade(species, stored_decks[species])
		save_data["card_decks"] = stored_decks
	var settings: Dictionary = save_data.get("settings", {})
	selected = settings.get("species", "mosslyn")
	if selected not in Catalog.STARTERS:
		selected = "mosslyn"
	battle_music_on = bool(settings.get("battle_music_on", true))
	effects_on = bool(settings.get("effects_on", true))
	effects_volume = clampf(float(settings.get("effects_volume", .55)),0,1)
	music_on = bool(settings.get("music_on", false))
	music_volume = clampf(float(settings.get("music_volume", 0.35)), 0, 1)
	sensitivity = clampf(float(settings.get("sensitivity", 1.0)), 0.7, 1.5)
	fixed_stick = bool(settings.get("fixed_stick", false))
	dusk = false

func _save_preferences() -> void:
	save_data["settings"] = {"species": selected, "music_on": music_on, "music_volume": music_volume,
		"battle_music_on":battle_music_on,"effects_on":effects_on,"effects_volume":effects_volume,
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
	if is_instance_valid(battle_audio): return
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
	elif what == NOTIFICATION_APPLICATION_FOCUS_IN:
		world_clock.reset_to_real_time()
		dusk = false
		_apply_world_time()
	elif what == NOTIFICATION_APPLICATION_FOCUS_OUT:
		_save_progress()
		_pause(true)
		show_menu()

func clock_label() -> String:
	var local: Dictionary = world_clock.local_datetime()
	return "%02d:%02d %s" % [local.hour, local.minute, "MESZ" if local.dst else "MEZ"]

func _apply_world_time() -> void:
	if not is_instance_valid(stage): return
	var local: Dictionary = world_clock.local_datetime()
	if stage.has_method("set_time"):
		stage.set_time(19.5 if dusk else float(local.hour) + float(local.minute) / 60.0)
	else: stage.set_dusk(dusk or world_clock.phase() in ["night", "dusk"])

func _finish_reading() -> void:
	world_clock.advance_hours(2.0)
	_refresh_residents()
	_apply_world_time()
	_resume()
	_notice("Ein Kapitel später · " + clock_label())

func _finish_sleeping() -> void:
	world_clock.sleep_until_morning()
	_refresh_residents()
	_apply_world_time()
	_resume()
	_notice("Guten Morgen · " + clock_label())

func _show_activity(title: String, text: String, action: String, callback: Callable) -> void:
	_pause(true)
	dialog_open = true
	var column := _panel(title)
	column.custom_minimum_size.x = 560
	var body := _label(text, 20)
	body.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	body.custom_minimum_size = Vector2(500, 76)
	column.add_child(body)
	column.add_child(_button(action, callback))
	column.add_child(_button("Zurück zur Welt", _resume))

func _show_conversation(species: String, appointment: Dictionary = {}) -> void:
	_pause(true)
	dialog_open = true
	var character: Dictionary = Residents.data()[species]
	var column := _panel(character.name + " · " + character.role)
	column.custom_minimum_size.x = 560
	var portrait := TextureRect.new()
	var atlas := Residents.portrait(species)
	portrait.texture = atlas
	portrait.custom_minimum_size = Vector2(120, 116)
	portrait.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
	portrait.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_CENTERED
	portrait.mouse_filter = Control.MOUSE_FILTER_IGNORE
	column.add_child(portrait)
	var text := _label(character.about, 22)
	text.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	text.custom_minimum_size = Vector2(500, 62)
	column.add_child(text)
	column.add_child(_button("Was tust du gerade?", func():
		text.text = str(appointment.get("activity","Ich mache eine Pause.")) + "\nHier: " + str(Catalog.data().labels.get(appointment.get("place",""),"unser Weg"))
		menu_buttons[0].disabled = true))
	column.add_child(_button("Bis später", _resume))

func _exploration_reply(species: String) -> String:
	var replies := {
		"fennec": "Lass uns einen kleinen Umweg machen. Die Wege und Eingangspfeile zeigen, wo es weitergeht.",
		"gloop": "Wir haben Zeit. Probier eine Bank aus und schau, was du am Wegesrand findest.",
		"puffling": "Ein Ort nach dem anderen. Auf der Karte siehst du, wo wir schon gewesen sind.",
		"wyrmling": "Vom Park führt der Weg weiter durch den Wald. Los, schauen wir uns dort um!",
		"starlet": "Such dir einen Weg, der dir gefällt. Kleine Entdeckungen zählen auch.",
		"hootlet": "In der Leseecke können wir ein Kapitel lesen. Danach sieht die Welt vielleicht schon anders aus.",
		"mosslyn": "Am Waldrand wachsen die kleinsten Wunder. Komm, wir schauen gemeinsam nach.",
		"nocti": "Wenn du leise bist, erzählt der Wald seine eigene Geschichte. Hörst du das Echo?"}
	return replies[species]

func cancel_approach() -> void:
	if pending_interaction.has("creature") and is_instance_valid(pending_interaction.creature):
		pending_interaction.creature.set_meta("rest_until", pending_interaction.get("previous_rest", 0.0))
	if not pending_interaction.is_empty() and is_instance_valid(actor): actor.touch_vector = Vector2.ZERO
	pending_interaction.clear()
	interaction_route.clear()
	approach_elapsed = 0.0

func request_interaction(kind: String, id: String) -> void:
	if paused or switching: return
	cancel_approach()
	for target in interaction_targets():
		if target.kind != kind or target.id != id: continue
		if target.ready:
			interact_with(kind, id)
			return
		if actor.mounted: actor.leave_seat()
		var path: PackedVector3Array = InteractionPath.find_path(stage, actor.position, target.approach, actor.collider.shape.radius + 0.035)
		if path.is_empty():
			_notice("Von hier führt kein freier Weg dorthin. Geh um das Hindernis herum.")
			return
		hud.reset_touch()
		pending_interaction = {"kind": kind, "id": id}
		interaction_route = path
		if kind == "talk":
			pending_interaction["creature"] = target.creature
			pending_interaction["previous_rest"] = target.creature.get_meta("rest_until", 0.0)
			target.creature.set_meta("rest_until", game_clock + 14.0)
		_notice("Auf dem Weg · " + target.label + " · Stick bricht ab")
		return

func _physics_process(delta: float) -> void:
	if pending_interaction.is_empty() or paused or switching: return
	if Input.get_vector("move_left", "move_right", "move_back", "move_front").length() > 0.12 or Input.is_action_just_pressed("jump") or Input.is_action_just_pressed("roll") or Input.is_action_just_pressed("sit"):
		cancel_approach()
		return
	approach_elapsed += delta
	for target in interaction_targets():
		if target.kind == pending_interaction.kind and target.id == pending_interaction.id and target.ready:
			interact_with(target.kind, target.id)
			return
	if interaction_route.is_empty() or approach_elapsed > 14.0:
		cancel_approach()
		_notice("Der Weg endet hier. Wähle das Ziel erneut oder bewege dich mit dem Stick.")
		return
	var difference: Vector3 = interaction_route[0] - actor.position
	if Vector2(difference.x, difference.z).length() < 0.07:
		interaction_route.remove_at(0)
		return
	actor.touch_vector = Vector2(difference.x, difference.z).normalized() * 0.61

func retreat_forest() -> void:
	if switching: return
	await _mount_world("FOREST_TRAIL",Forest.position_for(0,.15))
	_resume()
	_save_progress()

func show_audio_settings() -> void:
	var in_battle: bool=is_instance_valid(forest_encounter) and forest_encounter.active()
	if in_battle: forest_encounter.suspend()
	_pause(true)
	var column := _panel("Kampfklang")
	column.add_child(_button("Kampfmusik: an" if battle_music_on else "Kampfmusik: aus", func():
		battle_music_on=not battle_music_on;_save_preferences();show_audio_settings()))
	column.add_child(_button("Spielsounds: an" if effects_on else "Spielsounds: aus", func():
		effects_on=not effects_on;_save_preferences();show_audio_settings()))
	for is_music in [true,false]:
		var row:=HBoxContainer.new();column.add_child(row)
		row.add_child(_button("Leiser",_audio_volume.bind(is_music,-.1)))
		row.add_child(_label(("Musik" if is_music else "Effekte")+" %s %%" % roundi((music_volume if is_music else effects_volume)*100),18))
		row.add_child(_button("Lauter",_audio_volume.bind(is_music,.1)))
	column.add_child(_label("Kristallklang · Elementmagie · Treffer · Zugwechsel",16))
	column.add_child(_button("Zum Kampf" if in_battle else "Zurück",_resume if in_battle else show_settings))

func _audio_volume(is_music: bool, amount: float) -> void:
	if is_music: music_volume=clampf(music_volume+amount,0,1)
	else: effects_volume=clampf(effects_volume+amount,0,1)
	_save_preferences();show_audio_settings()

func _mount_regional_enemies() -> void:
	for id in Enemies.data():
		var profile: Dictionary=Enemies.data()[id]
		if ("OUTDOOR" if not Catalog.region_for(profile.place).is_empty() else profile.place)!=room:continue
		var point: Vector3=stage.safe_position(Catalog.position_for(profile.place,Vector2(profile.uv[0],profile.uv[1])))
		var art:=Sprite3D.new();art.texture=Enemies.texture(id);art.billboard=BaseMaterial3D.BILLBOARD_ENABLED
		art.shaded=false;art.alpha_cut=SpriteBase3D.ALPHA_CUT_DISCARD;art.alpha_scissor_threshold=.12
		art.pixel_size=stage.base_pixel_size*109.0*float(profile.get("height",1.1))/art.texture.get_height()
		art.offset.y=art.texture.get_height()*.5;art.position=point
		if id=="thorn_moth":art.position.y+=.85
		var material:=ShaderMaterial.new();material.shader=load("res://shaders/painted_character.gdshader")
		material.render_priority=10;material.set_shader_parameter("atlas",art.texture);material.set_shader_parameter("foot",point)
		art.material_override=material
		var mask: Dictionary=stage.actor_occlusion(point)
		material.set_shader_parameter("has_mask",mask.mask!=null)
		if mask.mask!=null:material.set_shader_parameter("furniture_mask",mask.mask)
		material.set_shader_parameter("scene_origin",mask.origin);material.set_shader_parameter("scene_span",mask.span)
		stage.add_child(art)
		var shadow:=MeshInstance3D.new();var plane:=PlaneMesh.new();plane.size=Vector2(1,.55);shadow.mesh=plane
		var contact:=ShaderMaterial.new();contact.shader=load("res://shaders/contact_shadow.gdshader");contact.render_priority=2
		contact.set_shader_parameter("opacity",.18 if id=="thorn_moth" else .34)
		shadow.material_override=contact;shadow.position=Vector3(point.x,.018,point.z)
		shadow.scale=Vector3.ONE*stage.body_pixel_size/.020*float(profile.get("height",1.1));stage.add_child(shadow)
		regional_markers.append({"id":id,"position":point,"art":art,"pose":0,"material":material,"shadow":shadow,
			"life_clock":float(posmod(hash(id),100)),"route":PackedVector3Array(),"route_goal":Vector3.INF})
	# Restore a suspended regional encounter after reopening the saved location.
	for marker in regional_markers:
		var saved: Dictionary=save_data.get("forest_encounters",{}).get(actor.species+":"+marker.id,{})
		if saved.get("phase","") in ["intro","battle","victory","defeat","retreat"]:
			_select_regional_encounter(marker.id)
			break

func _select_regional_encounter(id: String) -> void:
	if is_instance_valid(forest_encounter):
		forest_encounter.suspend();remove_child(forest_encounter);forest_encounter.queue_free()
	forest_encounter=RegionalEncounter.new();forest_encounter.app=self;forest_encounter.enemy_id=id
	add_child(forest_encounter)

func _show_enemy(id: String) -> void:
	_pause(true)
	var profile: Dictionary=Enemies.data()[id]
	var column:=_panel(profile.name+" · "+str(int(profile.hp))+" LP")
	var portrait:=TextureRect.new();portrait.texture=Enemies.texture(id)
	portrait.custom_minimum_size=Vector2(160,170);portrait.expand_mode=TextureRect.EXPAND_IGNORE_SIZE;portrait.stretch_mode=TextureRect.STRETCH_KEEP_ASPECT_CENTERED
	column.add_child(portrait)
	var purpose:=_label(str(profile.get("purpose",""))+"\n"+str(profile.get("work","")),18);purpose.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART;purpose.custom_minimum_size=Vector2(540,70);column.add_child(purpose)
	var difficulty: String={"crab":"Einstieg","beetle":"Aufbau","carrier":"Fortgeschritten","boss":"Boss"}.get(profile.kind,"Regional")
	var reward:=_label(difficulty+" · "+("360 EP je Held · 3 Mondsplitter" if profile.kind=="boss" else "120 EP je Held · 1 Mondsplitter")+" · 1 Heiltrank",16);column.add_child(reward)
	var description:=_label(profile.description,18 if profile.kind=="boss" else 20);description.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART;description.custom_minimum_size=Vector2(540,100);column.add_child(description)
	column.add_child(_button("Kartenkampf beginnen",func():
		_select_regional_encounter(id)
		if forest_encounter.model.phase=="done":
			forest_encounter.model.source_decks=save_data.get("card_decks",{}).duplicate(true)
			forest_encounter.model.replay()
		elif forest_encounter.model.phase=="unseen":forest_encounter.model.begin()
		forest_encounter.persist();forest_encounter.resume()))
	column.add_child(_button("Weiter erkunden",_resume))

func _refresh_residents() -> void:
	# One appointment snapshot drives both the living scene and the atlas.
	cancel_approach()
	for creature in actors.duplicate():
		if creature != actor:
			actors.erase(creature)
			remove_child(creature)
			creature.queue_free()
	resident_appointments.clear()
	for id in Residents.data():
		resident_appointments[id]=Residents.appointment(id,world_clock.current_hour())
	for id in Residents.data():
		if id == selected: continue
		var appointment: Dictionary = resident_appointments[id]
		if Residents.room_for(appointment.place) != room: continue
		var definition: Dictionary = Residents.data()[id]
		var resident = CitizenActor.new() if int(definition.row)>=0 else CompactActor.new() if Catalog.data().characters[id].get("compact",false) else Actor.new()
		resident.species=id
		resident.metrics=character_metrics
		resident.player_controlled=false
		resident.enabled=false
		add_child(resident)
		resident.set_world_scale(stage.base_pixel_size,stage.jump_height,stage.body_pixel_size)
		resident.position=stage.safe_position(Residents.position_for(appointment),resident.collider.shape.radius)
		resident.set_meta("anchor",resident.position)
		resident.set_meta("appointment",appointment)
		resident.set_meta("reach",float(appointment.reach))
		actors.append(resident)
	for creature in actors:
		creature.sprite.modulate=_character_tint()


func _update_regional_art() -> void:
	for marker in regional_markers:
		if not is_instance_valid(marker.art):continue
		var profile: Dictionary=Enemies.data()[marker.id]
		var saved: Dictionary=save_data.get("forest_encounters",{}).get(actor.species+":"+marker.id,{})
		marker.defeated=saved.get("phase","")=="done"
		marker.art.visible=not marker.defeated;marker.shadow.visible=not marker.defeated
		if marker.defeated:continue
		var walking:=false
		var close: bool=Vector2(actor.position.x-marker.position.x,actor.position.z-marker.position.z).length()<1.5
		if not close:marker.life_clock+=get_process_delta_time()
		var life:=EnemyLife.sample(profile,marker.life_clock)
		if not close:
			var goal: Vector3=stage.safe_position(Catalog.position_for(profile.place,life.uv),.1)
			# Auch Gegner folgen dem geprüften trockenen Weg. Das direkte Klemmen
			# einer Geraden konnte an gekrümmten Ufern seitlich versetzen.
			if goal.distance_to(marker.route_goal)>.20 or marker.route.is_empty():
				marker.route=InteractionPath.find_path(stage,marker.position,goal,.1)
				marker.route_goal=goal
			var at: Vector3=marker.position
			var budget:=1.2*get_process_delta_time()
			while not marker.route.is_empty() and budget>0:
				var waypoint: Vector3=marker.route[0]
				var next:=at.move_toward(waypoint,budget)
				if not InteractionPath.clear_segment(stage,at,next,.1):
					marker.route=PackedVector3Array();break
				budget-=at.distance_to(next);at=next
				if at.distance_to(waypoint)<.001:marker.route.remove_at(0)
				else:break
			if absf(at.x-marker.position.x)>.001:marker.art.flip_h=at.x>marker.position.x
			walking=at.distance_to(marker.position)>.001
			marker.position=at;marker.art.position=at+Vector3.UP*(.85 if marker.id=="thorn_moth" else 0.0)
			marker.shadow.position=Vector3(at.x,.018,at.z)
		marker.activity=(str(profile.travel) if walking else life.activity) if not close else "Beobachtet vorsichtig deinen Schritt"
		marker.material.set_shader_parameter("foot",marker.position)
		var mask: Dictionary=stage.actor_occlusion(marker.position)
		marker.material.set_shader_parameter("has_mask",mask.mask!=null)
		if mask.mask!=null:marker.material.set_shader_parameter("furniture_mask",mask.mask)
		marker.material.set_shader_parameter("scene_origin",mask.origin);marker.material.set_shader_parameter("scene_span",mask.span)
		stage.apply_actor_light(marker.material,marker.position)
		var pose: int=0 if close else (6 if int(game_clock*3)%2==0 else 0) if walking else int(life.pose)
		if pose!=marker.pose:
			marker.pose=pose;marker.art.texture=Enemies.texture(marker.id,pose)
			marker.art.offset.y=marker.art.texture.get_height()*.5
			marker.material.set_shader_parameter("atlas",marker.art.texture)
