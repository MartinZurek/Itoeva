extends SceneTree

const WORLD = "res://scenes/gameplay.tscn"
const BATTLE = "res://scenes/duel_lab.tscn"

func _initialize() -> void:
	run.call_deferred()

func press(text: String) -> void:
	for button in current_scene.menu_buttons:
		if is_instance_valid(button) and button.text == text and button.is_visible_in_tree():
			button.pressed.emit()
			return
	assert(false, "Schaltfläche fehlt: " + text)

func verify_transition(destination: String) -> void:
	var layer := root.get_node("ModeTransition")
	var card := layer.get_child(0)
	assert(card.is_visible_in_tree() and card.picture != null)
	await scene_changed
	assert(current_scene.scene_file_path == destination)
	while is_instance_valid(layer):
		await process_frame

func run() -> void:
	assert(change_scene_to_file(WORLD) == OK)
	await scene_changed
	press("Kampfmodus")
	await verify_transition(BATTLE)
	press("Hauptspiel")
	await verify_transition(WORLD)
	press("Kampfmodus")
	await verify_transition(BATTLE)
	var expected_deck: Array = current_scene.active_deck()
	press("Regionale Arenen")
	await verify_transition(WORLD)
	assert(current_scene.regional_arena_only)
	assert(current_scene.save_data.card_decks[current_scene.selected] == expected_deck)
	press("Zurück")
	await verify_transition(BATTLE)
	print("Hauptspiel, Kampfmodus und regionale Arenen mit Ladebildern geprüft")
	quit()
