extends SceneTree

const Cards = preload("res://scripts/card_catalog.gd")
const Catalog = preload("res://scripts/world_catalog.gd")
const NPC_PROFILES := ["fennec", "gloop", "puffling", "hootlet"]
const COUNT_ABILITIES := ["Lernen", "Zins", "Widerhall", "Blutpakt"]
const VALUE_ABILITIES := ["Baukunst", "Eingraben", "Glück", "Glut", "Heilung", "Nähren", "Panzer", "Schutzlicht", "Aufrüsten", "Blutpakt"]

func _initialize() -> void:
	var strategy: Dictionary = Cards.Strategy.data().cards
	var signatures := {}
	var duplicates := []
	for id in strategy:
		var card := Cards.get_card(id)
		var key := signature(card)
		if signatures.has(key):
			duplicates.append([signatures[key], id])
		signatures[key] = id
	var codes := Catalog.STARTERS + NPC_PROFILES
	for species in codes:
		assert(Cards.valid(species, Cards.starter(species)), "Startdeck ist ungültig: " + species)
	if not duplicates.is_empty():
		push_error("Gleiche Spielprofile: " + str(duplicates))
		quit(1)
		return
	var args := OS.get_cmdline_user_args()
	if args.size() == 1:
		var output := FileAccess.open(args[0], FileAccess.WRITE)
		assert(output != null, "Inventardatei konnte nicht geschrieben werden")
		output.store_string(JSON.stringify(inventory(codes)))
		output.close()
	print("Kartenpool · %s Strategiekarten · %s verschiedene Spielprofile" % [strategy.size(), signatures.size()])
	quit()

func signature(card: Dictionary) -> String:
	var signature_fields := ["cost", "attack", "health", "volk", "element", "affinity", "rare", "taunt", "ready", "ability", "gift"]
	var values: Array = signature_fields.map(func(key: String): return card[key])
	var used_traits: Array = card.traits.filter(func(trait_name: String):
		return trait_name in ["Flackernd", "Wachstum", "Knospend"])
	used_traits.sort()
	values.append(used_traits)
	values.append(card.get("strategy_target", ""))
	values.append(card.ability_x if card.ability in COUNT_ABILITIES else 0)
	values.append(card.ability_value if card.ability in VALUE_ABILITIES else 0)
	return JSON.stringify(values)

func inventory(codes: Array) -> Dictionary:
	var data := Cards.collection_data()
	var ids: Array = Cards.CARDS.keys() + data.new_cards.keys() + Cards.Strategy.ids() + Cards.Strategy.data().tokens.keys()
	var cards: Array = []
	var availability := {}
	var starters := {}
	for species in codes:
		availability[species] = Cards.available(species)
		starters[species] = Cards.starter(species)
	for id in ids:
		var card := Cards.get_card(id)
		card.playable_profiles = codes.filter(func(species: String): return id in availability[species])
		card.in_start_decks = codes.filter(func(species: String): return id in starters[species])
		cards.append(card)
	return {"cards": cards, "collection": data, "playable_profiles": Catalog.STARTERS}
