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
	check_chapter_cards()
	check_ringwehr_cards()
	check_relay_cards()
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

func chapter_match(species: String, card: String) -> RefCounted:
	var model := match_for(species, 0, 17)
	model.decks[0].hand = [card]
	model.decks[0].crystals = 10
	return model

func check_chapter_cards() -> void:
	var gauge := chapter_match("mosslyn", "weir_gauge")
	gauge.party[0].hp = 30
	check(gauge.play_card(0, "h:0") and gauge.party[0].hp == 40 and gauge.decks[0].hand.size() == 1,
		"Pegellot heilt 10 LP und zieht eine Karte")
	var mosslyn := chapter_match("mosslyn", "shore_roots")
	var ally: int = mosslyn._add_minion(0, "sprigling")
	check(mosslyn.play_card(0, "u:" + str(ally)), "Uferwurzeln sind auf eigene Helfer spielbar")
	var helped: Dictionary = mosslyn.find_unit(ally)
	check(helped.max_hp == 11 and helped.hp == 11 and helped.shield == 4, "Uferwurzeln geben LP und Schild")

	var starlet := chapter_match("starlet", "weir_lantern")
	starlet.party[0].hp = 30
	check(starlet.play_card(0, "h:0") and starlet.party[0].hp == 37 and starlet.party[0].barrier == 8
		and starlet.party[1].hp == 0, "Licht am Wehr heilt und schützt nur lebende Wesen")

	var nocti := chapter_match("nocti", "bridge_echo")
	var echo_target: int = nocti._add_minion(2, "thornling")
	check(nocti.play_card(0, "u:" + str(echo_target)), "Brückenecho zielt auf den Gegnerhelfer")
	check(nocti.find_unit(echo_target).hp == 6 and nocti.find_unit(echo_target).frozen == 1
		and nocti.decks[0].hand.size() == 1, "Brückenecho verursacht Schaden, friert ein und zieht eine Karte")

	var wyrmling := chapter_match("wyrmling", "steam_release")
	var first: int = wyrmling._add_minion(2, "thornling")
	var second: int = wyrmling._add_minion(2, "thornling")
	var enemy_hp: int = wyrmling.enemy_health
	check(wyrmling.play_card(0) and wyrmling.find_unit(first).hp == 3 and wyrmling.find_unit(second).hp == 3
		and wyrmling.enemy_health == enemy_hp and wyrmling.decks[0].hand.size() == 1,
		"Dampfstoß trifft beide Gegnerhelfer und zieht eine Karte")

func check_ringwehr_cards() -> void:
	var spray := chapter_match("mosslyn", "sluice_spray")
	var target: int = spray._add_minion(2, "thornling")
	check(not spray.can_play(0, "enemy") and spray.play_card(0, "u:" + str(target))
		and spray.find_unit(target).hp == 1 and spray.decks[0].hand.size() == 1,
		"Schleusenschwall trifft nur Helfer und zieht eine Karte")

	var wash := chapter_match("nocti", "mill_wash")
	var ally: int = wash._add_minion(0, "sprigling")
	wash.find_unit(ally).hp = 2
	check(wash.play_card(0, "u:" + str(ally)) and wash.find_unit(ally).hp == 6
		and wash.find_unit(ally).shield == 3, "Mühlenwasser heilt und schützt den gewählten Helfer")

	var call := chapter_match("starlet", "floodgate_call")
	check(not call.can_play(0), "Ruf der Flutwache braucht eigene Helfer")
	var own: int = call._add_minion(0, "reed_watch")
	var opposing: int = call._add_minion(2, "reed_watch")
	check(call.play_card(0) and call.find_unit(own).attack == 3 and call.find_unit(own).hp == 11
		and call.find_unit(own).shield == 5 and call.find_unit(opposing).hp == 8, "Flutwache stärkt nur eigene Helfer")

	var silta := chapter_match("wyrmling", "silta")
	for i in silta.BOARD_LIMIT:
		silta._add_minion(0, "reed_watch")
	check(not silta.can_play(0), "Silta braucht einen freien Platz")
	silta.board.pop_back()
	check(silta.play_card(0) and silta.units(0).back().taunt and silta.units(0).back().shield == 4
		and silta.decks[0].hand.size() == 1 and Cards.copy_limit("wyrmling", "silta") == 1,
		"Silta schützt, zieht eine Karte und ist pro Deck auf eine Kopie beschränkt")

func check_relay_cards() -> void:
	var recall := chapter_match("mosslyn", "relay_return")
	var ally: int = recall._add_minion(0, "relay_patrol")
	recall.decks[0].hand = ["relay_return"]
	check(recall.play_card(0, "u:" + str(ally)) and recall.find_unit(ally).is_empty()
		and recall.decks[0].hand.size() == 2 and recall.decks[0].hand[0] == "relay_patrol",
		"Ruf zum Relais nimmt den eigenen Helfer auf die Hand und zieht eine Karte")
	recall.acknowledge_animation()
	check(recall.play_card(0) and recall.decks[0].hand.size() == 2 and recall.units(0).back().shield == 2,
		"Wiederbeschwörung des Schriftläufers löst seinen Zieheffekt erneut aus")

	var resonance := chapter_match("starlet", "relay_resonance")
	resonance._add_minion(0, "relay_glimmer")
	resonance._add_minion(0, "relay_guard")
	var hp: int = resonance.enemy_health
	check(resonance.play_card(0, "enemy") and resonance.enemy_health == hp - 11,
		"Dreiring-Resonanz verursacht mit zwei Helfern 5 + 2 × 3 Schaden")

	var courier := chapter_match("nocti", "relay_courier")
	check(courier.play_card(0) and courier.units(0).back().ready, "Kurier kann beim Beschwören sofort angreifen")
	var tessara := chapter_match("wyrmling", "tessara")
	check(tessara.play_card(0) and tessara.decks[0].hand.size() == 2 and tessara.units(0).back().shield == 3
		and Cards.copy_limit("wyrmling", "tessara") == 1, "Tessara zieht zwei Karten, hat Schild und ist auf eine Kopie beschränkt")
