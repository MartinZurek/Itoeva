extends CanvasLayer

const TravelCard = preload("res://scripts/travel_card.gd")
const WORLD := "res://scenes/gameplay.tscn"
const BATTLE := "res://scenes/duel_lab.tscn"

static func open(app: Node, destination: String, regional_deck: Dictionary = {}) -> void:
	var tree := app.get_tree()
	if tree.root.has_node("ModeTransition"):
		return
	var transition := new()
	transition.name = "ModeTransition"
	transition.layer = 100
	tree.root.add_child(transition)
	app.set_process_input(false)
	await transition._switch_scene(app, destination, regional_deck)

func _switch_scene(previous: Node, destination: String, regional_deck: Dictionary) -> void:
	var tree := get_tree()
	var card := TravelCard.new()
	card.place = "ASH_ASCENT" if destination == BATTLE else "SUN_GLADE"
	card.title = "Kampfmodus" if destination == BATTLE else "Regionale Arenen" if not regional_deck.is_empty() else "Hauptspiel"
	card.progress = 0.2
	add_child(card)
	await tree.process_frame
	await tree.process_frame
	card.progress = 0.6
	card.queue_redraw()
	if tree.change_scene_to_file(destination) == OK:
		await tree.scene_changed
		var next_scene := tree.current_scene
		next_scene.set_process_input(false)
		if not regional_deck.is_empty():
			next_scene.show_regional_arenas(regional_deck.cards)
		card.progress = 1.0
		card.queue_redraw()
		await tree.create_timer(0.35).timeout
		next_scene.set_process_input(true)
	else:
		previous.set_process_input(true)
	queue_free()

func _input(_event: InputEvent) -> void:
	get_viewport().set_input_as_handled()
