extends SceneTree

const Battle = preload("res://scripts/practice_battle.gd")
const Presets = preload("res://scripts/practice_decks.gd")
const Enemies = preload("res://scripts/regional_enemies.gd")
const Cards = preload("res://scripts/card_catalog.gd")
var failures := 0

func _initialize() -> void:
	for species in Battle.SPECIES:
		for drill in Presets.DRILLS.size():
			check(Cards.valid(species, Presets.deck(species, drill)), "Legales Deck: %s/%s" % [species, drill])
			for seed_value in [17, 93, 801]:
				check_opening(species, drill, seed_value)
	check_enemy_turns()
	check_restart()
	print("Übungsduelle · %s Fehler" % failures)
	quit(failures)

func check(condition: bool, title: String) -> void:
	if not condition:
		failures += 1
		push_error(title)

func match_for(species: String, drill: int, seed_value: int) -> RefCounted:
	var preset: Dictionary = Presets.DRILLS[drill]
	var profile: Dictionary = Enemies.data()[preset.enemy].duplicate(true)
	profile.merge({"name": preset.opponent, "hp": preset.hp, "description": preset.lesson}, true)
	var model := Battle.new()
	model.set_random_seed(seed_value)
	model.start_drill(species, drill, profile)
	return model

func check_opening(species: String, drill: int, seed_value: int) -> void:
	var model = match_for(species, drill, seed_value)
	check(model.phase == "battle" and model.cursor == 0 and model.party[1].hp == 0, "Solo-Eröffnung")
	check(model.board.is_empty(), "Gegner startet mit leerem Feld")
	check(model.decks[0].hand.size() == 4 and model.decks[0].draw.size() == 36, "Vier Startkarten")
	var found := false
	for id in model.decks[0].hand:
		var card := Cards.get_card(id)
		found = found or (card.cost == 1 and card.kind == "minion")
	check(found, "Ein bezahlbarer Starthelfer")
	check(not model.can_act("skill"), "Kraft in Runde 1 gesperrt")
	check(model.act("end_turn"), "Spieler kann passen")
	model.acknowledge_animation()
	check(model.cursor == 2, "Zug springt direkt zum Gegner")
	check(model.enemy_act(), "Gegner reagiert")
	model.acknowledge_animation()
	check(model.cursor == 0 and model.round == 2 and model.decks[1].turns == 0, "Runde 2 ohne Begleiterzug")
	check(not model.can_act("skill"), "Kraft in Runde 2 gesperrt")
	check(model.act("end_turn"), "Spieler beendet Runde 2")
	model.acknowledge_animation()
	model.enemy_act()
	model.acknowledge_animation()
	check(model.act("skill") and model.decks[0].crystals == 0, "Kraft kostet drei Kristalle")

func check_enemy_turns() -> void:
	for drill in Presets.DRILLS.size():
		var model = match_for("mosslyn", drill, 17)
		for step in 24:
			if model.phase != "battle":
				break
			if model.cursor == 0:
				check(model.act("end_turn"), "Spieler passt")
			else:
				check(not model.enemy_intent().is_empty() and model.enemy_act(), "Angekündigter Gegnerzug")
				check(model.units(2).size() <= model.BOARD_LIMIT, "Drei Plätze auf der Gegnerseite")
			model.acknowledge_animation()
		check(model.phase == "defeat", "Passives Spiel verliert gegen jeden Gegner")
		check(model.repeat_drill() and model.decks[0].hand.size() == 4 and model.party[1].hp == 0, "Niederlage ist wiederholbar")
	var model = match_for("nocti", 0, 17)
	var attacker := {"hp": 7, "shield": 0, "evasion": false}
	var targets: Array[String] = ["h:0"]
	model.party[0].hp = 4
	check(model._choose_attack(attacker, targets, 4) == "h:0", "Gegner erkennt tödlichen Angriff")
	var uid: int = model._add_minion(0, "acorn_sentinel")
	check(model.legal_targets(2) == ["u:" + str(uid)], "Schutz erzwingt das Angriffsziel")

func check_restart() -> void:
	var model = match_for("mosslyn", 2, 17)
	var old_deck: Dictionary = model.source_decks.duplicate(true)
	model._damage_target(0, "enemy", model.enemy_max_hp, [])
	check(model.claim_victory() and model.result.items.is_empty() and model.experience.is_empty(), "Sieg vergibt keine Beute oder EP")
	check(model.repeat_drill() and model.source_decks == old_deck and model.drill == 2, "Revanche behält das Übungsdeck")
