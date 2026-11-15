extends RefCounted

const Cards = preload("res://scripts/card_catalog.gd")
const PACK_PRICE: = 12
const DISPLAY_PRICE: = 60
const DISPLAY_PACKS: = 6
const MAX_SEALED := 48
const DEFAULT_EDITION := "WEG"
const LEGENDARY_INTERVAL := 12
static func data() -> Dictionary: return Cards.collection_data()
static func state(save: Dictionary, species: String) -> Dictionary:
	if not save.has("card_collection") or not save.card_collection is Dictionary: save.card_collection = {}
	if save.card_collection.get(species, {}) is Dictionary and save.card_collection.get(species, {}).get("schema", 0) == 1: return save.card_collection[species]
	var owned: Dictionary = {}

	for id in Cards.starter(species): owned[id] = int(owned.get(id, 0)) + 1
	var legacy: Array = save.get("card_decks", {}).get(species, [])
	for id in legacy:
		if id is String and id in Cards.available(species): owned[id] = maxi(int(owned.get(id, 0)), legacy.count(id))
	var historical: = int(save.get("battle_progress", {}).get(species, {}).get("victories", 0))
	for key in save.get("forest_encounters", {}):
		if str(key) == species or str(key).begins_with(species + ":"): historical = maxi(historical, int(save.forest_encounters[key].get("victories", 0)))
	var s: Dictionary = {"schema": 1, "owned": owned, "coins": 24, "seed": int(randi()), "serial": 0, "sealed": [], "claims": {}, "last_victory": historical, "last_reward": {}, "last_opening": []}
	save.card_collection[species] = s
	s.sealed.append(_make_pack(s))
	return s
static func owned(save: Dictionary, species: String, id: String) -> int: return int(state(save, species).owned.get(id, 0))
static func usable(save: Dictionary, species: String, id: String) -> int: return mini(Cards.copy_limit(species, id), owned(save, species, id))

static func album_owned(save: Dictionary, species: String, id: String) -> bool:
	return id.is_empty() or state(save, species).get("albums", {}).get(id, false)

static func album_cards(species: String, id: String) -> Array:
	var catalog := data()
	var legal := Cards.available(species)
	var ids: Array = catalog.cards.keys().filter(func(card: String):
		return catalog.cards[card].get("edition", DEFAULT_EDITION) == DEFAULT_EDITION
	) if id.is_empty() else catalog.albums.get(id, {}).get("cards", [])
	return ids.filter(func(card: String): return card in legal)

static func purchase_album(save: Dictionary, species: String, id: String) -> Dictionary:
	var album: Dictionary = data().albums.get(id, {})
	if album.is_empty():
		return {"ok": false, "message": "Dieses Album ist im Laden nicht erhältlich."}
	if album_owned(save, species, id):
		return {"ok": false, "message": "Dieses Album gehört dir bereits."}
	var s := state(save, species)
	var price := int(album.price)
	if int(s.coins) < price:
		return {"ok": false, "message": "Dafür fehlen noch Bernsteinmarken."}
	if not s.has("albums"):
		s.albums = {}
	s.coins = int(s.coins) - price
	s.albums[id] = true
	return {"ok": true, "message": "%s liegt jetzt in deinem Sammelalbum." % album.name}
static func valid_owned(save: Dictionary, species: String, deck: Array) -> bool:
	if not Cards.valid(species, deck): return false
	for id in deck:
		if deck.count(id) > owned(save, species, id): return false
	return true
static func fill_owned(save: Dictionary, species: String, draft: Array) -> Array[String]:
	var result: Array[String] = []
	var legal: = Cards.available(species)
	for id in draft:
		if id is String and id in legal and result.count(id) < usable(save, species, id) and result.size() < 40: result.append(id)
	for id in Cards.starter(species) + legal:
		while result.size() < 40 and result.count(id) < usable(save, species, id): result.append(id)
	return result
static func _rng(s: Dictionary) -> RandomNumberGenerator:
	var rng: = RandomNumberGenerator.new();rng.seed = int(s.seed) + int(s.serial) * 104729;return rng
static func _pick(s: Dictionary, pool: Array, rng: RandomNumberGenerator, reserved: Array = []) -> String:
	var candidates: Array = []
	for id in pool:
		if int(s.owned.get(id, 0)) + reserved.count(id) < (1 if Cards.get_card(id).get("rare", false) else 2): candidates.append(id)
	if candidates.is_empty():
		candidates = pool.filter(func(id: String): return id not in reserved)
	if candidates.is_empty(): candidates = pool
	return str(candidates[rng.randi_range(0, candidates.size() - 1)])
static func sealed_packs(s: Dictionary, edition: String) -> Array:
	return s.sealed.filter(func(pack: Dictionary): return pack.get("edition", DEFAULT_EDITION) == edition)

static func _make_pack(s: Dictionary, edition: String = DEFAULT_EDITION) -> Dictionary:
	var pools: Dictionary = data().booster_series[edition].get("pools", data().pools)
	s.serial = int(s.serial) + 1
	var rng: = _rng(s); var ids: Array = []
	for i in 3: ids.append(_pick(s, pools.common, rng, ids))
	ids.append(_pick(s, pools.uncommon, rng, ids))
	ids.append(_pick(s, pools.legendary if rng.randi_range(1, LEGENDARY_INTERVAL) == 1 else pools.rare, rng, ids))
	return {"token": int(s.serial), "cards": ids, "edition": edition}
static func purchase(save: Dictionary, species: String, display: bool = false, edition: String = DEFAULT_EDITION) -> Dictionary:
	if not data().booster_series.has(edition):
		return {"ok": false, "message": "Diese Edition ist im Laden nicht erhältlich."}
	var s: = state(save, species); var price: = DISPLAY_PRICE if display else PACK_PRICE
	if int(s.coins) < price: return {"ok": false, "message": "Dafür fehlen noch Bernsteinmarken."}
	if s.sealed.size() + (DISPLAY_PACKS if display else 1) > MAX_SEALED: return {"ok": false, "message": "Öffne zuerst einige deiner versiegelten Booster."}
	s.coins = int(s.coins) - price
	for i in (DISPLAY_PACKS if display else 1): s.sealed.append(_make_pack(s, edition))
	return {"ok": true, "message": "Display mit 6 Boostern gekauft." if display else "Booster gekauft."}
static func open_pack(save: Dictionary, species: String, token: int) -> Dictionary:
	var s: = state(save, species)
	for i in s.sealed.size():
		if int(s.sealed[i].token) != token: continue
		var pack: Dictionary = s.sealed.pop_at(i); var revealed: Array = []
		for id in pack.cards:
			var fresh: = int(s.owned.get(id, 0)) == 0
			s.owned[id] = int(s.owned.get(id, 0)) + 1;revealed.append({"id": id, "new": fresh})
		s.last_opening = revealed.duplicate(true)
		s.last_opening_edition = pack.get("edition", DEFAULT_EDITION)
		return {"ok": true, "cards": revealed, "edition": s.last_opening_edition, "message": "5 Karten gehören jetzt deiner Sammlung."}
	return {"ok": false, "message": "Dieser Booster wurde bereits geöffnet."}
static func claim_find(save: Dictionary, species: String, id: String, answer: int = -1) -> Dictionary:
	var s: = state(save, species)
	for find in data().finds:
		if find.id != id: continue
		if s.claims.get("find:" + id, false): return {"ok": false, "message": "Diesen Fund hast du bereits geborgen."}
		if find.has("answer") and answer != int(find.answer): return {"ok": false, "message": "Die Spur passt noch nicht. Schau dich am Fundort noch einmal um."}
		s.claims["find:" + id] = true;s.owned[find.card] = int(s.owned.get(find.card, 0)) + 1;s.coins = int(s.coins) + int(find.coins)
		return {"ok": true, "cards": [{"id": find.card, "new": int(s.owned[find.card]) == 1}], "message": "Kartenfund · +%s Bernsteinmarken" % find.coins}
	return {"ok": false, "message": "Dieser Fund ist nicht bekannt."}
static func claim_chapter_card(save: Dictionary, species: String, chapter: String) -> String:
	var id: String = data().chapter_rewards.get(chapter, {}).get(species, "")
	if id.is_empty(): return ""
	var s := state(save, species)
	var receipt := "chapter:" + chapter
	if s.claims.get(receipt, false): return ""
	s.claims[receipt] = true
	s.owned[id] = int(s.owned.get(id, 0)) + 1
	return id
static func reward_battle(save: Dictionary, species: String, victories: int, boss: bool = false) -> Dictionary:
	var s: = state(save, species)
	if victories <= int(s.last_victory): return {}

	s.last_victory = victories;s.serial = int(s.serial) + 1
	var rng: = _rng(s); var pool: Array = data().pools.rare if boss else data().pools.common + data().pools.uncommon
	var id: = _pick(s, pool, rng);s.owned[id] = int(s.owned.get(id, 0)) + 1
	var coins: = 18 if boss else 8;s.coins = int(s.coins) + coins
	var reward: Dictionary = {"id": id, "coins": coins, "victory": victories};s.last_reward = reward
	return reward
static func trade_preview(save: Dictionary, species: String, id: String) -> Dictionary:
	for offer in data().trades:
		if offer.id != id: continue
		var s := state(save, species)
		var deck: Array = save.get("card_decks", {}).get(species, Cards.starter(species))
		var in_deck := deck.count(offer.give)
		var reserve := maxi(1, in_deck)
		var have := int(s.owned.get(offer.give, 0))
		var free := maxi(0, have - reserve)
		var done: bool = s.claims.get("trade:" + id, false)
		var required: String = offer.get("requires_story", "")
		var reason := ""
		if done:
			reason = "Dieser Tausch ist bereits abgeschlossen."
		elif not required.is_empty() and not save.get("story_events", {}).get(species, {}).get(required, false):
			reason = offer.get("locked_hint", "Dieses Angebot öffnet sich später in der Geschichte.")
		elif offer.receive not in Cards.available(species):
			reason = "Diese Karte ist für deine Figur nicht spielbar."
		elif have == 0:
			reason = "Diese Karte fehlt in deiner Sammlung."
		elif free == 0:
			reason = "Dein Deck benötigt alle vorhandenen Kopien." if in_deck >= have else "Deine letzte Kopie ist geschützt."
		return {"ok": reason.is_empty(), "offer": offer, "free": free, "reserved": mini(have, reserve), "have": have,
			"receive_have": int(s.owned.get(offer.receive, 0)), "done": done, "message": reason}
	return {"ok": false, "message": "Kein passendes Tauschangebot."}

static func trade(save: Dictionary, species: String, id: String) -> Dictionary:
	var preview := trade_preview(save, species, id)
	if not preview.ok:
		return {"ok": false, "message": preview.message}
	var offer: Dictionary = preview.offer
	var s := state(save, species)
	s.owned[offer.give] = preview.have - 1
	s.owned[offer.receive] = preview.receive_have + 1
	s.claims["trade:" + id] = true
	return {"ok": true, "cards": [{"id": offer.receive, "new": preview.receive_have == 0}],
		"message": "Tausch abgeschlossen · " + Cards.get_card(offer.receive).name}
