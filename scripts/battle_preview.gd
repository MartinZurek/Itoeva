extends RefCounted


static func copy_model(model, existing = null):
	var copy = model.get_script().new() if existing == null else existing
	for property in model.get_property_list():
		if (int(property.usage) & PROPERTY_USAGE_SCRIPT_VARIABLE) == 0: continue
		var name: String = property.name
		var value = model.get(name)
		if value is Dictionary or value is Array: value = value.duplicate(true)
		elif value is RandomNumberGenerator:
			var random: = RandomNumberGenerator.new();random.seed = value.seed;random.state = value.state;value = random
		copy.set(name, value)
	return copy

static func evaluate(model, kind: String, index: int = -1, target: String = "enemy", working = null) -> Dictionary:
	var copy = copy_model(model, working)
	var accepted: = false
	match kind:
		"card": accepted = copy.play_card(index, target)
		"unit": accepted = copy.attack_minion(index, target)
		"intent":
			copy.cursor = 2;copy.playback_pending = false
			accepted = copy.resolve_announced_intent()
	if not accepted: return {}
	var result: Dictionary = {"effects": copy.last_event.get("effects", []).duplicate(true), "outcomes": {}, "title": copy.last_event.get("title", "")}
	if kind != "intent" and copy.rng.state != model.rng.state:
		result["random_summon"] = "Zufall: 3–12 ATK / 6–18 LP"
		result.title = "Zufaellige Beschwoerung"
	for owner in 2:
		_difference(result, "h:" + str(owner), model.party[owner], copy.party[owner])
	_difference(result, "enemy", {"hp": model.enemy_health}, {"hp": copy.enemy_health})
	for unit in model.board:
		if int(copy.last_event.get("recalled_uid", -1)) == int(unit.uid): result.outcomes["u:" + str(unit.uid)] = {"recalled": true}
		else: _difference(result, "u:" + str(unit.uid), unit, copy.find_unit(int(unit.uid)))
	if kind == "intent":
		var summons: Array[String] = []
		for unit in copy.units(2):
			if model.find_unit(int(unit.uid)).is_empty():
				summons.append("%s erscheint (%s Angriff · %s LP)" % [unit.name, copy.unit_attack(unit), unit.hp])
		result["summons"] = summons
	return result

static func _difference(result: Dictionary, code: String, before: Dictionary, after: Dictionary) -> void :
	var values: Dictionary = {}
	for field in ["hp", "attack", "shield", "barrier", "max_hp", "frozen", "bind", "silence", "ready"]:
		var change: = int(after.get(field, 0)) - int(before.get(field, 0))
		if change != 0: values[field] = change
	if before.get("evasion", false) and not after.get("evasion", false): values["evade"] = true
	if not before.get("evasion", false) and after.get("evasion", false): values["evasion"] = true
	if after.is_empty() or (int(after.get("hp", 1)) <= 0 and int(before.get("hp", 1)) > 0): values["defeated"] = true
	if int(before.get("shield", 0)) + int(before.get("barrier", 0)) > 0 and int(after.get("shield", 0)) + int(after.get("barrier", 0)) == 0: values["shield_break"] = true
	if not values.is_empty(): result.outcomes[code] = values

static func label(values: Dictionary) -> String:
	var words: Array[String] = []
	if values.get("recalled", false): return "Zurueck zur Hand"
	if values.get("evade", false): words.append("Ausweichen")
	if values.get("evasion", false): words.append("Ausweichschutz")
	if int(values.get("attack", 0)) != 0 and not values.get("defeated", false): words.append(_signed(int(values.attack)) + " ATK")
	if int(values.get("hp", 0)) != 0: words.append(_signed(int(values.hp)) + " LP")
	var shield: = int(values.get("shield", 0)) + int(values.get("barrier", 0))
	if values.get("shield_break", false): words.append("Schild bricht")
	elif shield != 0: words.append(_signed(shield) + " Schild")
	if int(values.get("frozen", 0)) > 0: words.append("Still")
	if int(values.get("bind", 0)) > 0: words.append("Fesseln")
	if int(values.get("silence", 0)) > 0: words.append("Stumm")
	if int(values.get("ready", 0)) > 0: words.append("Bereit")
	if values.get("defeated", false): words.append("besiegt")
	return " · ".join(words)

static func _signed(amount: int) -> String: return ("+" if amount > 0 else "−") + str(absi(amount))

static func describe(model, result: Dictionary) -> String:
	if result.has("random_summon"): return str(result.random_summon)
	var lines: Array[String] = []
	for code in result.get("outcomes", {}):
		var text: = label(result.outcomes[code])
		if text.is_empty(): continue
		var name: String = model.enemy_name if code == "enemy" else str(model.party[int(code.substr(2))].name) if code.begins_with("h:") else str(model.find_unit(int(code.substr(2))).get("name", "Helfer"))
		lines.append(name + ": " + text)
	lines.append_array(result.get("summons", []))
	return "\n".join(lines)
