extends "res://scripts/practice_battle.gd"

const FULL_SCENES := ["K0220", "K0125", "K0098", "phoenix", "starfall"]
const REVEAL_TIME := 2.8
var subject := ""

func prepare_preview(card_id: String, species: String, profile: Dictionary) -> void:
	set_random_seed(736)
	start_drill(species, 0, profile)
	var fixture := Cards.get_card(Cards.SPECIAL[species][0])
	_add_minion(0, fixture.id)
	_add_minion(2, "thornling")
	_add_minion(2, "thornling")
	party[0].hp = maxi(1, int(party[0].max_hp) - 24)
	fallen[0].append(fixture.id)
	decks[0].discard.append(fixture.id)
	decks[0].crystals = 10
	decks[0].hero_power_used = true
	decks[0].turn_cards = 2
	decks[0].cards_played = 2
	var card := Cards.get_card(card_id)
	if card.get("combo", "") == "three_helpers":
		while units(0).size() < BOARD_LIMIT:
			_add_minion(0, fixture.id)
	decks[0].hand = [card_id]
	subject = card_id

func demonstrate() -> bool:
	var card := Cards.get_card(subject)
	var targets := card_targets(0, card)
	var target := "enemy" if targets.is_empty() or "enemy" in targets else targets[0]
	return play_card(0, target)

func _event(title: String, visual: String, effects: Array, previous: Dictionary, extra: Dictionary = {}) -> void:
	super._event(title, visual, effects, previous, extra)
	if subject == "":
		return
	if subject in FULL_SCENES:
		return
	last_event.erase("cinematic_card")
	last_event["preview_card"] = subject
	last_event["preview_cut"] = REVEAL_TIME
	last_event.impact = REVEAL_TIME + float(last_event.get("impact", ROUTINE_IMPACT))
	last_event.duration = REVEAL_TIME + float(last_event.get("duration", ROUTINE_DURATION))
