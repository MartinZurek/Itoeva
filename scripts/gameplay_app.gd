extends "res://scripts/world_app.gd"

const ModeTransition = preload("res://scripts/mode_transition.gd")

const TEST_PLACES := ["SUN_GLADE", "MIST_WATCH", "MILL_BROOK", "ASH_ASCENT", "BEACH", "SEAL_COURT", "RELAY_COURT"]

var regional_arena_only := false

func _ready() -> void :
	save_path = "user://gameplay-preview.json"
	super._ready()

func show_menu() -> void:
	if regional_arena_only:
		show_test_places(true)
		return
	super.show_menu()
	var button := _menu_button("Spielorte testen", show_test_places)
	button.set_anchors_and_offsets_preset(Control.PRESET_TOP_RIGHT)
	button.position = Vector2(-244, 24)
	button.size = Vector2(218, 48)
	ui.add_child(button)
	var battle_button := _menu_button("Kampfmodus", _open_battle_mode)
	battle_button.set_anchors_and_offsets_preset(Control.PRESET_TOP_RIGHT)
	battle_button.position = Vector2(-244, 80)
	battle_button.size = Vector2(218, 48)
	ui.add_child(battle_button)

func _open_battle_mode() -> void:
	ModeTransition.open(self, ModeTransition.BATTLE)

func show_regional_arenas(species: String, cards: Array) -> void:
	regional_arena_only = true
	save_path = "user://regional-arena-preview.json"
	save_data = {}
	_load_save()
	selected = species
	if not save_data.has("card_decks"):
		save_data.card_decks = {}
	save_data.card_decks[species] = cards.duplicate()
	show_test_places(true)

func _resume() -> void:
	if regional_arena_only and is_instance_valid(forest_encounter) and is_instance_valid(forest_encounter.view) and not forest_encounter.active():
		show_test_places(true)
		return
	super._resume()

func start_game() -> void :
	if not save_data.get("characters", {}).has(selected):
		await visit_test_place("SUN_GLADE")
		return
	super.start_game()

func show_test_places(regional_only: bool = false) -> void:
	if is_instance_valid(forest_encounter): forest_encounter.suspend()
	_pause(true)
	var panel := _panel("Regionale Arenen" if regional_only else "Spielorte testen")
	var scroll := ScrollContainer.new()
	scroll.custom_minimum_size = Vector2(820, 370)
	panel.add_child(scroll)
	var choices := VBoxContainer.new()
	choices.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	scroll.add_child(choices)
	for place in TEST_PLACES:
		var row := HBoxContainer.new()
		choices.add_child(row)
		var label := _label(Catalog.label_for(place, selected), 19)
		label.custom_minimum_size.x = 240
		row.add_child(label)
		if not regional_only:
			row.add_child(_menu_button("Erkunden", visit_test_place.bind(place)))
		for id: String in Enemies.data():
			if Enemies.data()[id].place == place:
				row.add_child(_menu_button(Enemies.data()[id].name, test_encounter.bind(place, id)))
	panel.add_child(_menu_button("Zurück", _open_battle_mode if regional_only else show_menu))

func visit_test_place(place: String) -> void :
	if is_instance_valid(forest_encounter):
		forest_encounter.suspend()
		forest_encounter.queue_free()
		forest_encounter = null
	var region: Dictionary = Catalog.region_for(place)
	var room_id := "OUTDOOR" if not region.is_empty() else place
	var encounters: Dictionary = save_data.get("forest_encounters", {})
	for id: String in Enemies.data():
		var enemy_region: Dictionary = Catalog.region_for(Enemies.data()[id].place)
		var enemy_room: String = "OUTDOOR" if not enemy_region.is_empty() else Enemies.data()[id].place
		var key := selected + ":" + id
		if enemy_room == room_id and encounters.get(key, {}).get("phase", "unseen") != "done":
			encounters.erase(key)
	var point := Catalog.position_for(place, Vector2(0.27, 0.55))
	await _mount_world(room_id, point)
	_resume()

func test_encounter(place: String, enemy: String) -> void :
	await visit_test_place(place)
	_select_regional_encounter(enemy)
	forest_encounter.model.configure(selected)
	forest_encounter.model.configure_enemy(enemy, Enemies.data()[enemy])
	forest_encounter.model.prepare(save_data.get("card_decks", {}))
	forest_encounter.model.begin()
	forest_encounter.model.advance_intro(10.0)
	forest_encounter.resume()
