extends "res://scripts/card_battle.gd"

const Presets = preload("res://scripts/practice_decks.gd")
const POWER_COST := 3
const CRYSTAL_POWERS = ["skill", "light", "support"]
const CINEMATIC_SPELLS = ["phoenix", "starfall"]
const FilmTiming = preload("res://scripts/practice_film_timing.gd")
var drill := 0
var expert := false

func configure(species: String) -> void:
	super.configure(species)
	# Das Kampf-UI erwartet zwei Slots. Der zweite bleibt im Duell inaktiv.
	party[1].hp = 0

func start_drill(species: String, index: int, profile: Dictionary, is_expert := false) -> void:
	drill = index
	expert = is_expert
	configure(species)
	var preset: Dictionary = Presets.DRILLS[drill]
	var opponent := profile.duplicate(true)
	opponent.hp = preset.hp + (22 if expert else 0)
	opponent.visual_kind = opponent.kind
	opponent.kind = "practice"
	configure_enemy(preset.enemy, opponent)
	prepare({lead: Presets.deck(lead, drill)})
	_ensure_opening_helper()
	begin()
	advance_intro(10)

func _reset_fight() -> void:
	super._reset_fight()
	_ensure_opening_helper()
	message = "Neues Duell · Lies die Absicht des Gegners."

func repeat_drill() -> bool:
	if phase not in ["victory", "defeat"] or playback_pending:
		return false
	_reset_fight()
	return true

func _event(title: String, visual: String, effects: Array, previous: Dictionary, extra: Dictionary = {}) -> void:
	var presentation := extra.duplicate()
	var id: String = presentation.get("card", visual.trim_prefix("special_") if visual.begins_with("special_") else "")
	var card := Cards.get_card(id) if not id.is_empty() else {}
	var mythic: bool = visual == "card_summon" and card.get("rare", false) and card.get("kind", "") == "minion"
	if cursor == 0 and (id in CINEMATIC_SPELLS or mythic):
		presentation.merge({"cinematic_card": id, "duration": FilmTiming.DURATION, "impact": FilmTiming.IMPACT}, true)
	elif visual == "card_spell":
		presentation.merge({"duration": 1.04, "impact": 0.42}, true)
	super._event(title, visual, effects, previous, presentation)

func animation_duration() -> float:
	if last_event.has("cinematic_card") and not last_event.get("cinematic_short", false):
		return FilmTiming.DURATION
	return super.animation_duration()

func _ensure_opening_helper() -> void:
	var state: Dictionary = decks[0]
	for id in state.hand:
		var card := Cards.get_card(id)
		if card.kind == "minion" and card.cost == 1:
			return
	for index in state.draw.size():
		var card := Cards.get_card(state.draw[index])
		if card.kind == "minion" and card.cost == 1:
			var opening_card: String = state.draw[index]
			state.draw[index] = state.hand[0]
			state.hand[0] = opening_card
			return

func can_act(action: String, target: int = -1) -> bool:
	return super.can_act(action, target) and (action not in CRYSTAL_POWERS or decks[0].crystals >= POWER_COST)

func act(action: String, target: int = -1) -> bool:
	if not super.act(action, target):
		return false
	if action in CRYSTAL_POWERS:
		decks[0].crystals -= POWER_COST
	return true

func _plan() -> Dictionary:
	var pressure := _pressure()
	var room := units(2).size() < BOARD_LIMIT
	match drill:
		0:
			if room and round % 2 == 1 and round % 3 != 0:
				return _summon_plan("Glutwelpe", 4, 7, false)
			return {"name": "Glutsprung" if round % 3 == 0 else "Aschenbiss",
				"hit": (17 if round % 3 == 0 else 9) + pressure}
		1:
			if room and round % 3 != 0:
				return _summon_plan("Schildlarve", 3, 6, true)
			return {"name": "Pollenstoß", "hit": 12 + pressure}
		_:
			if room and (round == 1 or round % 3 == 2):
				return _summon_plan("Schmiedeling", 5, 8, false)
			if not units(2).is_empty() and round % 3 == 0:
				return {"name": "Anfachen", "buff": 2}
			return {"name": "Funkenwurf", "hit": 13 + pressure}

func _summon_plan(unit_name: String, attack: int, health: int, taunt: bool) -> Dictionary:
	var bonus := 1 if expert else 0
	return {"name": unit_name, "summon": [attack + bonus, health + bonus, taunt]}

func _pressure() -> int:
	return maxi(0, round - (5 if expert else 7)) * 3 + (5 if expert else 0)

func _follow_up() -> int:
	return 5 + _pressure() if expert and round > 1 else 0

func enemy_intent() -> String:
	if enemy_stagger > 0:
		return "Wanken · Zug entfällt"
	var plan := _plan()
	var follow_up := " + %s Schaden" % _strike_damage(_follow_up()) if _follow_up() > 0 else ""
	if plan.has("summon"):
		return "%s · %s/%s%s%s" % [plan.name, plan.summon[0], plan.summon[1], " · Schutz" if plan.summon[2] else "", follow_up]
	if plan.has("buff"):
		return "Anfachen · Helfer +2/+2" + follow_up
	return "%s · %s Schaden" % [plan.name, _strike_damage(plan.hit)]

func _strike_damage(amount: int) -> int:
	return ceili(amount * 0.65) if enemy_weaken > 0 else amount

func resolve_announced_intent() -> bool:
	# Die Vorschau arbeitet auf einer Kopie vor dem tatsächlichen Zugwechsel.
	_begin_turn(2)
	return enemy_act()

func enemy_act() -> bool:
	if phase != "battle" or cursor != 2 or playback_pending:
		return false
	var previous := _before()
	var effects: Array[Dictionary] = []
	var attacks: Array[Dictionary] = []
	var title := enemy_intent()
	var element := "none"
	var plan := _plan()
	if enemy_stagger > 0:
		enemy_stagger -= 1
	else:
		if plan.has("summon"):
			var stats: Array = plan.summon
			var uid := _add_minion(2, "thornling", {"name": plan.name, "attack": stats[0],
				"health": stats[1], "taunt": stats[2], "species": "enemy"})
			previous["unit_uid"] = uid
		elif plan.has("buff"):
			for unit in units(2):
				Strategy.grow(unit, plan.buff, plan.buff)
		var damage: int = plan.get("hit", _follow_up())
		if damage > 0:
			element = "fire" if drill != 1 else "wind"
			_damage_target(2, "h:0", _strike_damage(damage), effects)
		for unit in units(2):
			if phase != "battle":
				break
			if unit.ready and unit_attack(unit) > 0:
				_attack_as_enemy(unit, attacks, effects)
	if enemy_burn > 0:
		enemy_burn -= 1
		if phase == "battle":
			_damage_target(0, "enemy", 6, effects)
	enemy_weaken = maxi(0, enemy_weaken - 1)
	_event(title, "card_end" if element == "none" else element, effects, previous,
		{"element": element, "unit_attacks": attacks})
	pending_advance = true
	return true

func _attack_as_enemy(unit: Dictionary, attacks: Array, effects: Array) -> void:
	var targets := legal_targets(2)
	if targets.is_empty():
		return
	var damage := Strategy.swing(self, unit)
	var target := _choose_attack(unit, targets, damage)
	unit.ready = false
	unit.spent = true
	attacks.append({"uid": unit.uid, "target_code": target})
	if target.begins_with("u:"):
		var defender := find_unit(int(target.substr(2)))
		_hurt_unit(unit, unit_attack(defender), effects)
	_damage_target(2, target, damage, effects)

func _choose_attack(unit: Dictionary, targets: Array[String], damage: int) -> String:
	var hero: Dictionary = party[0]
	if "h:0" in targets and not hero.evasion and damage >= hero.hp + hero.barrier:
		return "h:0"
	var best := targets[-1]
	var best_score := 0 if best == "h:0" else -100
	for code in targets:
		if not code.begins_with("u:"):
			continue
		var defender := find_unit(int(code.substr(2)))
		var defeats: bool = not defender.evasion and damage >= defender.hp + defender.shield
		var survives: bool = unit.evasion or unit.hp + unit.shield > unit_attack(defender)
		var score := unit_attack(defender) + (9 if defeats else -7) + (4 if survives else -5)
		if defeats and defender.get("ability", "") in ["Lernen", "Zins", "Aufrüsten"]:
			score += 4
		if score > best_score:
			best_score = score
			best = code
	return best


func claim_victory() -> bool:
	if phase != "victory" or playback_pending or rewards_claimed:
		return false
	result = {"members": [], "items": []}
	rewards_claimed = true
	message = "Trainingsduell gewonnen."
	return true
