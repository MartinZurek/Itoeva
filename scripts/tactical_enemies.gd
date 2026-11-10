extends RefCounted


static func handles(b) -> bool:
	return b.tactics_version >= 1 and b.enemy_profile.get("kind", "") in ["moth", "sentinel"]
static func setup(b) -> void :
	if not handles(b): return
	b.ecology_state["tactical_step"] = 1 if b.tactics_version >= 2 and b.enemy_profile.kind == "sentinel" else 0
	for i in 2: _summon(b, "pollen_larva" if b.enemy_profile.kind == "moth" else "relay_drone", false)
static func _summon(b, id: String, ready: bool) -> void :
	var moth: = id == "pollen_larva"
	b._add_minion(2, id, {"name": "Pollenlarve" if moth else "Relaisdrohne", "attack": 4 if moth else 5, "health": 6 if moth else 12, "taunt": moth, "ready": ready, "species": "enemy", "form": "leaf_beetle" if moth else "mirror"})
static func intent(b) -> String:
	if b.enemy_stagger > 0: return "Wanken · nächster Zug entfällt"
	if b.enemy_profile.kind == "moth": return "Pollenwirbel · 9 auf beide · Larven greifen an · Flächenschaden hilft"
	var count: int = b.units(2).size()
	var step: = int(b.ecology_state.get("tactical_step", 0)) % 3
	if step == 1:
		return "Relais entlädt · %s auf beide · Drohnen ausschalten oder schützen" % (10 + count * 12)
	if b.tactics_version < 2: return "Relais lädt · Drohnen speisen den nächsten Impuls"
	var charge: String = "Neue Drohne" if count < 3 else "Drohnenplätze besetzt"
	return "Restimpuls · 6 auf beide · " + charge if step == 2 else "Relais lädt · " + charge
static func damage(b, amount: int) -> int:
	if b.tactics_version >= 1 and b.enemy_profile.get("kind", "") == "weaver" and not b._seal_anchor().is_empty(): return int(ceil(amount * 0.5))
	return amount
static func _hit(b, target: int, amount: int, effects: Array[Dictionary]) -> int:
	if b.party[target].hp <= 0: return 0
	if b.enemy_weaken > 0: amount = int(ceil(amount * 0.65))
	if b.party[target].guard: amount = int(ceil(amount * 0.35))
	return b._receive_hit(target, amount, effects)
static func act(b) -> bool:
	var previous: Dictionary = b._before()
	var effects: Array[Dictionary] = []
	var attacks: Array = []
	var title: = intent(b)
	var step: = int(b.ecology_state.get("tactical_step", 0))
	if b.enemy_stagger > 0: b.enemy_stagger -= 1
	else:
		if b.enemy_profile.kind == "moth":
			for i in 2:
				var dealt: = _hit(b, i, 9, effects)
				if b.round % 3 == 0 and dealt > 0 and b.party[i].hp > 0:
					b.party[i].silence = 1;effects.append({"target": i, "amount": 1, "kind": "silence"})
			for unit in b.units(2):
				if not unit.ready or b.phase != "battle": continue
				var choices: Array[String] = b.legal_targets(2)
				if choices.is_empty(): break
				var target: String = choices[0]
				unit.ready = false;unit.spent = true
				attacks.append({"uid": unit.uid, "target_code": target})
				if target.begins_with("u:"):
					var defender: Dictionary = b.find_unit(int(target.substr(2)))
					b._hurt_unit(unit, b.unit_attack(defender), effects)
				b._damage_target(2, target, b.unit_attack(unit), effects)
			if step % 2 == 0: _summon(b, "pollen_larva", false)
		elif step % 3 == 1:
			for i in 2: _hit(b, i, 10 + b.units(2).size() * 12, effects)
		else:
			if b.tactics_version < 2 or b.units(2).size() < 3: _summon(b, "relay_drone", false)
			if step % 3 == 2:
				for i in 2: _hit(b, i, 6, effects)
	b.ecology_state.tactical_step = step + 1
	if b.party[0].hp <= 0 and b.party[1].hp <= 0: b.phase = "defeat"
	if b.enemy_burn > 0:
		b.enemy_burn -= 1;b._damage_target(0, "enemy", 6, effects)
	if b.enemy_weaken > 0: b.enemy_weaken -= 1
	b._event(b.enemy_name + " · " + title, "card_spell", effects, previous, {"element": "wind" if b.enemy_profile.kind == "moth" else "moon", "unit_attacks": attacks, "duration": 1.8, "impact": 0.8})
	b.pending_advance = true
	return true
