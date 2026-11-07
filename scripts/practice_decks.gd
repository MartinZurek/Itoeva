extends RefCounted

const Cards = preload("res://scripts/card_catalog.gd")
const DRILLS = [
	{
		"name": "Funkenlauf", "opponent": "Aschenläufer", "enemy": "ash_hound", "hp": 110,
		"lesson": "Tempo · Greife an, bevor der nächste Glutsprung kommt. Zweiter Wind lässt einen Helfer erneut angreifen.",
		"pairs": ["spark", "leaf_guard", "insight", "leaf_beetle", "mist_glider", "trail_blessing",
			"second_wind", "leaf_volley", "frost_thread", "homeward", "root_pact", "tea", "fresh_sap",
			"K0011", "K0341", "K0458", "K0079"], "rare": "K0220"
	},
	{
		"name": "Wurzelwacht", "opponent": "Schildwacht", "enemy": "thorn_moth", "hp": 112,
		"lesson": "Kontrolle · Halte mit Schutzhelfern stand. Spare die Blättersalve für mehrere Ziele.",
		"pairs": ["spark", "leaf_guard", "tea", "insight", "lantern_snail", "leaf_beetle",
			"mushroom_archivist", "thistle_nurse", "acorn_sentinel", "bark_ward", "dew_mend",
			"frost_thread", "leaf_volley", "forest_memory", "shared_rest", "K0212", "mist_glider"],
		"rare": "K0125"
	},
	{
		"name": "Echo in der Esse", "opponent": "Funkenmeister", "enemy": "ash_hound", "hp": 120,
		"lesson": "Synergie · Lass Neoviryx durch Karten wachsen. Plane vorher Platz auf der Hand und in der Reihe ein.",
		"pairs": ["spark", "leaf_guard", "insight", "leaf_beetle", "mushroom_archivist", "bark_ward",
			"dew_mend", "homeward", "fresh_sap", "moon_exchange", "forest_memory", "leaf_volley",
			"second_wind", "K0227", "K0514", "K0142", "K0011"], "rare": "K0098"
	}
]

static func deck(species: String, drill: int) -> Array[String]:
	var preset: Dictionary = DRILLS[drill]
	var ids: Array[String] = []
	for id in preset.pairs + Cards.SPECIAL[species].slice(0, 2):
		ids.append(id)
		ids.append(id)
	ids.append(preset.rare)
	ids.append(Cards.ULTIMATES[species])
	assert(Cards.valid(species, ids), "Ungültiges Übungsdeck")
	return ids
