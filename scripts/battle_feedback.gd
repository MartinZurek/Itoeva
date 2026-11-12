extends RefCounted

const Cards = preload("res://scripts/card_catalog.gd")

static func unit_state(model, unit: Dictionary) -> String:
	if unit.ready:
		if model.unit_attack(unit) <= 0: return "passive"
		return "ready" if int(unit.owner) == model.cursor else "waiting"
	if int(unit.get("frozen", 0)) > 0 or unit.get("skipped", false): return "frozen"
	if unit.get("spent", false): return "spent"
	if model.unit_attack(unit) <= 0: return "passive"
	return "sleeping"

static func state_text(state: String) -> String:
	return {
		"ready": "Angriffsbereit",
		"waiting": "Wartet auf den eigenen Zug",
		"sleeping": "Ab dem nächsten eigenen Zug bereit",
		"spent": "Hat in diesem Zug schon angegriffen",
		"frozen": "Durch Frost am Angriff gehindert",
		"passive": "Kann mit 0 Angriff nicht angreifen",
	}.get(state, "")

static func unit_abilities(unit: Dictionary) -> String:
	var names: Array[String] = []
	var ability: String = unit.get("ability", "")
	if not ability.is_empty():
		var counter: String = {"Zins": "interest_left", "Lernen": "learn_left", "Aufrüsten": "upgrade_left"}.get(ability, "")
		names.append(ability if counter.is_empty() or not unit.has(counter) else "%s (%s übrig)" % [ability, int(unit[counter])])
	var gift: String = unit.get("gift", "")
	if not gift.is_empty():
		names.append(gift)
	return " · ".join(names)

static func attack_target_message(model, target: String) -> String:
	if target in model.legal_targets(model.cursor, true) and target not in model.legal_targets(model.cursor):
		var guards: Array[String] = model.legal_targets(model.cursor)
		if guards.size() == 1:
			var guard: Dictionary = model.find_unit(int(guards[0].substr(2)))
			return "%s schützt dieses Ziel. Greife zuerst diesen Helfer an." % guard.name
		return "Wähle zuerst einen der markierten Schutzhelfer."
	return "Dieser Helfer kann das Ziel gerade nicht angreifen."

static func targets(model, card_index: int, uid: int) -> Array[String]:
	if model.phase != "battle" or model.cursor == 2 or model.playback_pending: return []
	if card_index >= 0 and model.can_play(card_index):
		var card: Dictionary = Cards.get_card(model.decks[model.cursor].hand[card_index])
		return model.card_targets(model.cursor, card)
	if uid >= 0 and model.can_attack(uid): return model.legal_targets(model.cursor)
	return []

# Called only when the model has already rejected this card.
static func card_block_message(model, card: Dictionary) -> String:
	var owner: int = model.cursor
	var state: Dictionary = model.decks[owner]
	var missing := int(card.cost) - int(state.crystals)
	if missing > 0:
		return "Dir fehlt ein Kristall." if missing == 1 else "Dir fehlen %d Kristalle." % missing
	var summons: bool = card.kind in ["minion", "revive"] or (card.kind == "special" and card.effect in ["wild_rally", "stormflock"])
	if summons and model.units(owner).size() >= model.BOARD_LIMIT:
		return "Deine Helferreihe ist voll."
	if card.has("target") and model.card_targets(owner, card).is_empty():
		if card.kind == "mend":
			return "Kein eigener Helfer kann diese Heilung erhalten."
		if card.kind == "recall" and state.hand.size() > model.HAND_LIMIT:
			return "Deine Hand ist voll."
		return "Kein passendes Ziel auf dem Feld."
	match card.kind:
		"heal": return "Deine Lebenspunkte sind bereits voll."
		"draw": return "Dein Nachziehstapel ist leer."
		"barrier": return "Deine Barriere ist bereits voll."
		"team_buff": return "Du hast keinen Helfer auf dem Feld."
		"volley": return "Der Gegner hat keinen Helfer auf dem Feld."
		"energy": return "Deine Kristalle sind bereits voll."
		"revive": return "Du hast keinen gefallenen Helfer."
	return "Diese Karte ist gerade nicht spielbar."
