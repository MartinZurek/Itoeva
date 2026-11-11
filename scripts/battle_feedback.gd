extends RefCounted

const Cards = preload("res://scripts/card_catalog.gd")

static func unit_state(model, unit: Dictionary) -> String:
	if unit.ready:
		return "ready" if int(unit.owner) == model.cursor and model.unit_attack(unit) > 0 else "waiting"
	if int(unit.get("frozen", 0)) > 0 or unit.get("skipped", false): return "frozen"
	if unit.get("spent", false): return "spent"
	return "sleeping"

static func state_text(state: String) -> String:
	return {"ready": "ANGRIFFSBEREIT", "waiting": "WARTET AUF ZUG", "sleeping": "SCHLÄFT", "spent": "ERSCHÖPFT", "frozen": "FESTGEHALTEN"}.get(state, "")

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
