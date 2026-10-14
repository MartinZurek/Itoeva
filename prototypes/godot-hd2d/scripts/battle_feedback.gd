extends RefCounted

static func unit_state(model, unit: Dictionary) -> String:
	if unit.ready:
		return "ready" if int(unit.owner)==model.cursor and int(unit.attack)>0 else "waiting"
	if int(unit.get("frozen",0))>0 or unit.get("skipped",false): return "frozen"
	if unit.get("spent",false): return "spent"
	return "sleeping"

static func state_text(state: String) -> String:
	return {"ready":"ANGRIFFSBEREIT", "waiting":"WARTET AUF ZUG", "sleeping":"SCHLÄFT", "spent":"ERSCHÖPFT", "frozen":"FESTGEHALTEN"}.get(state,"")

static func targets(model, card_index: int, uid: int) -> Array[String]:
	if model.phase!="battle" or model.cursor==2 or model.playback_pending: return []
	if card_index>=0 and model.can_play(card_index):
		var card: Dictionary=load("res://scripts/card_catalog.gd").get_card(model.decks[model.cursor].hand[card_index])
		return model.card_targets(model.cursor,card)
	if uid>=0 and model.can_attack(uid): return model.legal_targets(model.cursor)
	return []
