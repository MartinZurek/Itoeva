extends SceneTree

const Cards = preload("res://scripts/card_catalog.gd")
const Hound = preload("res://scripts/arena_ash_hound.gd")
const Geometry = preload("res://scripts/battle_pose_geometry.gd")
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
	_check_arena(duel)
	duel.model.decks[0].hand = ["tessara"]
	duel.model.decks[0].crystals = 10
	duel._card_play(0, "enemy")
	assert(duel.model.last_event.card == "tessara" and duel.model.units(0).size() == 1)
	lab.show_battle_pause()
	press("Duell beenden")
	assert(lab.active_deck() == custom_deck and not is_instance_valid(lab.forest_encounter))
	DirAccess.remove_absolute(path)
	print("Kartenschmiede · Deckplätze, Speichern, KI-Zug, Aufstellung, Laufrichtung und Wolfssprung geprüft")
	lab.queue_free()
	await process_frame
	quit()

func _check_arena(duel: Node) -> void:
	var view: Control = duel.view
	var wolf: Texture2D = load("res://assets/expansion/enemies.png")
	for region in Hound.REGIONS:
		assert(Rect2(Vector2.ZERO, wolf.get_size()).encloses(region))
		assert(Geometry.mesh_for(wolf, region) != null)
	var enemy: Dictionary = duel.model.units(2)[0]
	assert(enemy.species == "enemy" and not view._helper_flipped(enemy, false))
	assert(view._helper_flipped(enemy, true))
	assert(view._helper_height(view._unit_position(enemy.uid)) > 100)
	var wolf_home: Vector2 = view._position(2)
	assert(wolf_home.y > 400 and wolf_home.x < 1050)

	var preview = duel.model.get_script().new()
	preview.start_drill("starlet", 0, duel.model.enemy_profile)
	preview.round = 3
	preview.cursor = 2
	var guard: int = preview._add_minion(0, "star_guard")
	assert(preview.enemy_act())
	var event: Dictionary = preview.last_event
	assert(event.wolf_attack == "leap" and event.wolf_target == "u:%s" % guard)
	assert(float(event.duration) > 3.0 and float(event.impact) > 1.5)
	var target := Vector2(428, 416)
	var windup := Hound.motion(wolf_home, target, 0.2, event.impact, event.duration, true)
	var contact := Hound.motion(wolf_home, target, event.impact, event.impact, event.duration, true)
	var returning := Hound.motion(wolf_home, target, 2.4, event.impact, event.duration, true)
	var done := Hound.motion(wolf_home, target, event.duration, event.impact, event.duration, true)
	assert(windup.ground.x > wolf_home.x)
	assert(absf(contact.ground.x - target.x) < 90.0 and contact.lift > 25.0)
	assert(Hound.flip_for(contact.pose, true))
	assert(returning.ground.x > contact.ground.x and not returning.toward_left)
	assert(done.ground.is_equal_approx(wolf_home))
