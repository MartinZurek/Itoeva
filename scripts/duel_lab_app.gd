extends Node3D

const Cards = preload("res://scripts/card_catalog.gd")
const Collection = preload("res://scripts/card_collection.gd")
const Catalog = preload("res://scripts/world_catalog.gd")
const Presets = preload("res://scripts/practice_decks.gd")
const DeckEditor = preload("res://scripts/deck_editor.gd")
const Encounter = preload("res://scripts/duel_lab_encounter.gd")
const Audio = preload("res://scripts/battle_audio.gd")
const SLOT_NAMES := ["Deck A", "Deck B", "Deck C"]
const HERO_NAMES := {"mosslyn": "Mooslyn", "nocti": "Nocti", "starlet": "Starlight", "wyrmling": "Wyrmling"}

var selected := "mosslyn"
var slot := 0
var expert := false
var save_path := "user://kartenschmiede.json"
var save_data: Dictionary = {"card_decks": {}, "deck_slots": {}}
var character_metrics := {}
var room := "KARTENSCHMIEDE"
var world_ready := true
var switching := false
var paused := true
var music_on := false
var battle_music_on := true
var music_volume := 0.38
var effects_on := true
var effects_volume := 0.55
var short_repeat_cinematics := true
var music := AudioStreamPlayer.new()
var room_activities := {"active": {}}
var battle_audio: Node
var forest_encounter: Node
var ui: Control
var overlay: Control
var menu_buttons: Array[Button] = []
var deck_editor: RefCounted
var theme := Theme.new()
var storage_error := false

func _ready() -> void:
	Input.emulate_mouse_from_touch = true
	_load_save()
	_build_theme()
	add_child(music)
	battle_audio = Audio.new()
	battle_audio.app = self
	add_child(battle_audio)
	show_menu()

func _build_theme() -> void:
	theme.default_font_size = 19
	for state in ["normal", "hover", "pressed", "disabled", "focus"]:
		var box := StyleBoxFlat.new()
		box.bg_color = Color("394b52") if state in ["hover", "pressed"] else Color("17242f")
		box.border_color = Color("635d51") if state == "disabled" else Color("ad966c")
		box.set_corner_radius_all(9)
		box.set_border_width_all(1)
		box.content_margin_left = 12
		box.content_margin_right = 12
		box.content_margin_top = 5
		box.content_margin_bottom = 5
		if state == "focus":
			box.draw_center = false
		theme.set_stylebox(state, "Button", box)
	theme.set_color("font_color", "Button", Color("f3e7ce"))
	theme.set_color("font_disabled_color", "Button", Color("aaa282"))

func _new_ui() -> Control:
	if is_instance_valid(ui):
		remove_child(ui)
		ui.queue_free()
	menu_buttons.clear()
	ui = Control.new()
	ui.theme = theme
	ui.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	add_child(ui)
	return ui

func _button(text: String, callback: Callable) -> Button:
	var button := Button.new()
	button.text = text
	button.custom_minimum_size = Vector2(140, 48)
	button.pressed.connect(callback)
	menu_buttons.append(button)
	return button

func _label(text: String, font_size := 19) -> Label:
	var label := Label.new()
	label.text = text
	label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	label.add_theme_font_size_override("font_size", font_size)
	label.add_theme_color_override("font_color", Color("f0e4ca"))
	return label

func _panel(title: String) -> VBoxContainer:
	_new_ui()
	var backdrop := TextureRect.new()
	backdrop.texture = preload("res://assets/forest/arena.png")
	backdrop.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
	backdrop.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_COVERED
	backdrop.modulate = Color(0.43, 0.46, 0.53)
	backdrop.mouse_filter = Control.MOUSE_FILTER_IGNORE
	backdrop.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	ui.add_child(backdrop)
	return _panel_on(ui, title)

func _panel_on(surface: Control, title: String) -> VBoxContainer:
	var center := CenterContainer.new()
	center.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	surface.add_child(center)
	var panel := PanelContainer.new()
	var box := StyleBoxFlat.new()
	box.bg_color = Color("101c28f5")
	box.border_color = Color("ad966c")
	box.set_border_width_all(1)
	box.set_corner_radius_all(17)
	box.content_margin_left = 24
	box.content_margin_right = 24
	box.content_margin_top = 18
	box.content_margin_bottom = 18
	panel.add_theme_stylebox_override("panel", box)
	center.add_child(panel)
	var column := VBoxContainer.new()
	column.add_theme_constant_override("separation", 9)
	column.add_child(_label(title, 30))
	panel.add_child(column)
	return column

func _row(column: VBoxContainer) -> HBoxContainer:
	var row := HBoxContainer.new()
	row.alignment = BoxContainer.ALIGNMENT_CENTER
	column.add_child(row)
	return row

func show_menu() -> void:
	_close_overlay()
	if is_instance_valid(forest_encounter):
		forest_encounter.suspended = true
		forest_encounter.queue_free()
		forest_encounter = null
	var column := _panel("Itoeva · Kartenschmiede")
	column.custom_minimum_size.x = 860
	column.add_child(_label("Eigene Decks · Freier Kartenpool · Duelle gegen KI", 17))
	var heroes := _row(column)
	for id in Catalog.STARTERS:
		var choice := _button(HERO_NAMES[id], _select_hero.bind(id))
		choice.disabled = selected == id
		heroes.add_child(choice)
	var decks := _row(column)
	for index in SLOT_NAMES.size():
		var choice := _button(SLOT_NAMES[index], _select_slot.bind(index))
		choice.disabled = index == slot
		decks.add_child(choice)
	decks.add_child(_button("Deck bearbeiten", _open_deck))
	var levels := _row(column)
	for advanced in [false, true]:
		var difficulty := _button("Meisterprüfung" if advanced else "Taktik", _select_difficulty.bind(advanced))
		difficulty.disabled = expert == advanced
		levels.add_child(difficulty)
	for index in Presets.DRILLS.size():
		var drill: Dictionary = Presets.DRILLS[index]
		column.add_child(_button("%s · %s" % [drill.opponent, drill.name], _start_duel.bind(index)))
	column.add_child(_label("40 Karten je Deck · Heldenkräfte kosten 3 Kristalle", 15))
	if storage_error:
		column.add_child(_label("Der letzte Speichervorgang ist fehlgeschlagen.", 16))

func _select_hero(id: String) -> void:
	selected = id
	_load_slot()
	_write_save()
	show_menu()

func _select_slot(index: int) -> void:
	slot = index
	_load_slot()
	_write_save()
	show_menu()

func _select_difficulty(advanced: bool) -> void:
	expert = advanced
	_write_save()
	show_menu()

func active_deck() -> Array:
	return save_data.card_decks.get(selected, Cards.starter(selected)).duplicate()

func _load_slot() -> void:
	var slots: Dictionary = save_data.deck_slots.get(selected, {})
	var stored = slots.get(str(slot), [])
	save_data.card_decks[selected] = Cards.upgrade(selected, stored if stored is Array else [])

func _unlock_cards() -> void:
	var collection := Collection.state(save_data, selected)
	for id in Cards.available(selected):
		collection.owned[id] = Cards.copy_limit(selected, id)

func _open_deck() -> void:
	_unlock_cards()
	deck_editor = DeckEditor.new()
	deck_editor.app = self
	deck_editor.species = selected
	deck_editor.exit_to = show_menu
	deck_editor.open()

func _start_duel(index: int) -> void:
	forest_encounter = Encounter.new()
	forest_encounter.app = self
	forest_encounter.drill = index
	forest_encounter.expert = expert
	add_child(forest_encounter)
	forest_encounter.resume()

func _load_save() -> void:
	var persisted = JSON.parse_string(FileAccess.get_file_as_string(save_path)) if FileAccess.file_exists(save_path) else {}
	if persisted is Dictionary:
		var decks = persisted.get("deck_slots", {})
		if decks is Dictionary:
			save_data.deck_slots = decks
		var hero: String = persisted.get("selected", "")
		if hero in Catalog.STARTERS:
			selected = hero
		slot = clampi(int(persisted.get("slot", 0)), 0, SLOT_NAMES.size() - 1)
		expert = bool(persisted.get("expert", false))
		effects_on = bool(persisted.get("effects_on", true))
		battle_music_on = bool(persisted.get("battle_music_on", true))
		save_data.seen_practice_films = persisted.get("seen_practice_films", [])
	_load_slot()

func _write_save() -> bool:
	var slots: Dictionary = save_data.deck_slots.get(selected, {})
	slots[str(slot)] = active_deck()
	save_data.deck_slots[selected] = slots
	var persisted := {"deck_slots": save_data.deck_slots, "selected": selected,
		"slot": slot, "expert": expert, "effects_on": effects_on, "battle_music_on": battle_music_on,
		"seen_practice_films": save_data.get("seen_practice_films", [])}
	var file := FileAccess.open(save_path + ".tmp", FileAccess.WRITE)
	storage_error = file == null
	if not storage_error:
		file.store_string(JSON.stringify(persisted))
		file.close()
		storage_error = DirAccess.rename_absolute(save_path + ".tmp", save_path) != OK
	return not storage_error

func has_seen_practice_film(id: String) -> bool:
	return id in save_data.get("seen_practice_films", [])

func remember_practice_film(id: String) -> void:
	var seen: Array = save_data.get("seen_practice_films", [])
	if id not in seen:
		seen.append(id)
		save_data.seen_practice_films = seen
		_write_save()

func show_battle_pause() -> void:
	if not is_instance_valid(forest_encounter):
		return
	_show_overlay("Duell pausiert")

func show_audio_settings() -> void:
	_show_overlay("Ton")

func _show_overlay(title: String) -> void:
	_close_overlay()
	forest_encounter.suspended = true
	overlay = ColorRect.new()
	overlay.color = Color(0.03, 0.05, 0.08, 0.88)
	overlay.z_index = 20
	overlay.theme = theme
	overlay.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	ui.add_child(overlay)
	var column := _panel_on(overlay, title)
	column.custom_minimum_size.x = 420
	column.add_child(_button("Fortsetzen", _resume_duel))
	column.add_child(_button("Kampfmusik " + ("aus" if battle_music_on else "an"), func():
		battle_music_on = not battle_music_on
		_write_save()
		_show_overlay(title)))
	column.add_child(_button("Effekte " + ("aus" if effects_on else "an"), func():
		effects_on = not effects_on
		_write_save()
		_show_overlay(title)))
	column.add_child(_button("Duell beenden", show_menu))

func _close_overlay() -> void:
	if is_instance_valid(overlay):
		overlay.hide()
		overlay.queue_free()
	overlay = null

func _resume_duel() -> void:
	_close_overlay()
	forest_encounter.resume()

func _input(event: InputEvent) -> void:
	if not is_instance_valid(forest_encounter) or forest_encounter.suspended:
		return
	var view: Control = forest_encounter.view
	if is_instance_valid(view) and view.route_card_pointer(event):
		get_viewport().set_input_as_handled()

func _notification(what: int) -> void:
	if what == NOTIFICATION_WM_GO_BACK_REQUEST:
		if is_instance_valid(overlay):
			_resume_duel()
		elif is_instance_valid(forest_encounter):
			show_battle_pause()
		else:
			show_menu()
