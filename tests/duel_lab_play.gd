extends SceneTree

const Cards = preload("res://scripts/card_catalog.gd")
var lab: Node3D
var path := ""

func _initialize() -> void:
	run.call_deferred()

func mount() -> void:
	lab = load("res://scenes/duel_lab.tscn").instantiate()
	lab.save_path = path
	root.add_child(lab)

func press(label: String) -> void:
	for candidate in lab.menu_buttons:
		if is_instance_valid(candidate) and candidate.is_visible_in_tree() and candidate.text == label and not candidate.disabled:
			candidate.pressed.emit()
			return
	assert(false, "Schaltfläche fehlt: " + label)

func run() -> void:
	path = "user://duel-lab-test-%s.json" % OS.get_process_id()
	mount()
	await process_frame
	press("Starlight")
	press("Deck B")
	press("Deck bearbeiten")
	assert(Cards.available("starlet").size() > 660)
	assert(lab.Collection.owned(lab.save_data, "starlet", "relay_resonance") == 2)
	assert(lab.Collection.owned(lab.save_data, "starlet", "tessara") == 1)
	lab.deck_editor.change("lightling", -1)
	lab.deck_editor.change("tessara", 1)
	press("Deck speichern")
	var custom_deck: Array = lab.active_deck()
	assert(custom_deck.has("tessara") and Cards.valid("starlet", custom_deck))
	press("Deck A")
	assert(not lab.active_deck().has("tessara"))
	press("Deck B")
	assert(lab.active_deck() == custom_deck)
	lab.queue_free()
	await process_frame
	mount()
	await process_frame
	assert(lab.selected == "starlet" and lab.slot == 1 and lab.active_deck() == custom_deck)
	press("Meisterprüfung")
	press("Aschenläufer · Funkenlauf")
	await process_frame
	var duel: Node = lab.forest_encounter
	assert(duel.model.source_decks.starlet == custom_deck and duel.model.expert)
	assert(duel.model.enemy_name == "Aschenläufer" and is_instance_valid(duel.view))
	lab.show_battle_pause()
	assert(duel.suspended)
	press("Fortsetzen")
	assert(not duel.suspended)
	duel._act("end_turn")
	for frame in 300:
		await process_frame
		if duel.model.round == 2:
			break
	assert(duel.model.round == 2 and duel.model.cursor == 0)
	assert(duel.model.units(2).size() == 1)
	duel.model.decks[0].hand = ["tessara"]
	duel.model.decks[0].crystals = 10
	duel._card_play(0, "enemy")
	assert(duel.model.last_event.card == "tessara" and duel.model.units(0).size() == 1)
	lab.show_battle_pause()
	press("Duell beenden")
	assert(lab.active_deck() == custom_deck and not is_instance_valid(lab.forest_encounter))
	DirAccess.remove_absolute(path)
	print("Kartenschmiede · Freier Kartenpool, drei Deckplätze, Speichern, KI-Zug, Heldenwahl und eigenes Deck geprüft")
	lab.queue_free()
	await process_frame
	quit()
