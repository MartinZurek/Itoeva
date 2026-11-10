extends SceneTree

const Battle = preload("res://scripts/practice_battle.gd")
const Presets = preload("res://scripts/practice_decks.gd")
const Enemies = preload("res://scripts/regional_enemies.gd")
const Catalog = preload("res://scripts/world_catalog.gd")
const Cards = preload("res://scripts/card_catalog.gd")

func _initialize() -> void:
	for expert in [false, true]:
		for drill in Presets.DRILLS.size():
			for species in Catalog.STARTERS:
				var wins := 0
				var rounds := 0
				for trial in 24:
					var model = duel(species, drill, 37 + trial * 71, expert)
					if model.phase == "victory":
						wins += 1
					rounds += model.round
				print("%s %s/%s: %s/24 Siege, Runde Ø %.1f" % [expert, drill, species, wins, rounds / 24.0])
	quit()

func duel(species: String, drill: int, seed_value: int, expert: bool) -> RefCounted:
	var preset: Dictionary = Presets.DRILLS[drill]
	var profile: Dictionary = Enemies.data()[preset.enemy].duplicate(true)
	profile.merge({"name": preset.opponent, "hp": preset.hp, "description": preset.lesson}, true)
	var model := Battle.new()
	model.set_random_seed(seed_value)
	model.start_drill(species, drill, profile, expert)
	for move in 210:
		if model.phase != "battle":
			break
		if model.cursor == 2:
			model.enemy_act()
		else:
			play(model)
		model.acknowledge_animation()
	return model

func play(model) -> void:
	var options: Array[Dictionary] = [{"score": 0.0, "kind": "end_turn"}]
	for index in model.decks[0].hand.size():
		if not model.can_play(index):
			continue
		var card := Cards.get_card(model.decks[0].hand[index])
		var targets: Array[String] = model.card_targets(0, card)
		if targets.is_empty():
			targets.append("enemy")
		for target in targets:
			if model.can_play(index, target):
				options.append({"score": card_score(model, card, target), "kind": "card", "index": index, "target": target})
	for unit in model.units(0):
		if not model.can_attack(unit.uid):
			continue
		for target in model.legal_targets(0):
			var damage: int = model.unit_attack(unit)
			var score := 6.0 if target == "enemy" else 1.0
			if target == "enemy" and damage >= model.enemy_health:
				score = 1000.0
			elif target.begins_with("u:"):
				var defender: Dictionary = model.find_unit(int(target.substr(2)))
				if damage >= defender.hp + defender.shield:
					score = 8.0 + model.unit_attack(defender)
					if unit.hp + unit.shield <= model.unit_attack(defender):
						score -= model.unit_attack(unit) * 0.8
			options.append({"score": score, "kind": "attack", "uid": unit.uid, "target": target})
	if model.can_act("special"):
		options.append({"score": 15.0, "kind": "special"})
	if model.can_act("skill"):
		options.append({"score": 5.0 if model.party[0].species == "fennec" else 10.0, "kind": "skill"})
	options.sort_custom(func(a, b): return a.score > b.score)
	var choice: Dictionary = options[0]
	match choice.kind:
		"card": model.play_card(choice.index, choice.target)
		"attack": model.attack_minion(choice.uid, choice.target)
		_: model.act(choice.kind)

func card_score(model, card: Dictionary, target: String) -> float:
	var unit: Dictionary = model.find_unit(int(target.substr(2))) if target.begins_with("u:") else {}
	match card.kind:
		"minion":
			return 5.0 + card.attack + card.health * 0.35 + (3.0 if card.get("taunt", false) else 0.0) - card.cost
		"damage":
			var damage: int = card.value + int(card.get("per_helper", 0)) * model.units(0).size()
			if unit.is_empty():
				return 1000.0 if damage >= model.enemy_health else 3.0 + damage * 0.25
			return 10.0 + model.unit_attack(unit) if damage >= unit.hp + unit.shield else 1.0
		"volley":
			return 8.0 * model.units(2).size() - 4.0
		"heal", "party_heal":
			return minf(card.value, model.party[0].max_hp - model.party[0].hp) * (0.85 if model.party[0].hp < 35 else 0.3)
		"barrier":
			return 3.0 if model.party[0].barrier > 8 else 7.0
		"draw":
			return 8.0 if model.decks[0].hand.size() <= 3 else 2.0
		"special", "revive":
			return 9.5
		"buff":
			return 6.0 + model.unit_attack(unit) * 0.5 if card.get("ready", false) and not unit.ready else 4.0
		"mend":
			return minf(card.value, unit.max_hp - unit.hp) * 0.75 + 4
		"energy":
			return 20.0 if model.decks[0].crystals == 0 else 2.0
		"freeze":
			return 7.0 if model.unit_attack(unit) >= 5 else 2.0
	return 1.0
