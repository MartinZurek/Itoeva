extends RefCounted

const Cards = preload("res://scripts/card_catalog.gd")
const PACK_PRICE: = 12
const DISPLAY_PRICE: = 60
const DISPLAY_PACKS: = 6
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
	if candidates.is_empty(): candidates = pool
	return str(candidates[rng.randi_range(0, candidates.size() - 1)])
static func _make_pack(s: Dictionary) -> Dictionary:
	s.serial = int(s.serial) + 1
	var rng: = _rng(s); var ids: Array = []
	for i in 3: ids.append(_pick(s, data().pools.common, rng, ids))
	ids.append(_pick(s, data().pools.uncommon, rng, ids))
	ids.append(_pick(s, data().pools.legendary if rng.randi_range(1, 12) == 1 else data().pools.rare, rng, ids))
	return {"token": int(s.serial), "cards": ids}
static func purchase(save: Dictionary, species: String, display: bool = false) -> Dictionary:
	var s: = state(save, species); var price: = DISPLAY_PRICE if display else PACK_PRICE
	if int(s.coins) < price: return {"ok": false, "message": "Dafür fehlen noch Wegmünzen."}
	if s.sealed.size() + (DISPLAY_PACKS if display else 1) > 48: return {"ok": false, "message": "Öffne zuerst einige deiner versiegelten Booster."}
	s.coins = int(s.coins) - price
	for i in (DISPLAY_PACKS if display else 1): s.sealed.append(_make_pack(s))
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
		return {"ok": true, "cards": revealed, "message": "5 Karten gehören jetzt deiner Sammlung."}
	return {"ok": false, "message": "Dieser Booster wurde bereits geöffnet."}
static func claim_find(save: Dictionary, species: String, id: String, answer: int = -1) -> Dictionary:
	var s: = state(save, species)
	for find in data().finds:
		if find.id != id: continue
		if s.claims.get("find:" + id, false): return {"ok": false, "message": "Diesen Fund hast du bereits geborgen."}
		if find.has("answer") and answer != int(find.answer): return {"ok": false, "message": "Die Spur passt noch nicht. Schau dich am Fundort noch einmal um."}
		s.claims["find:" + id] = true;s.owned[find.card] = int(s.owned.get(find.card, 0)) + 1;s.coins = int(s.coins) + int(find.coins)
		return {"ok": true, "cards": [{"id": find.card, "new": int(s.owned[find.card]) == 1}], "message": "Kartenfund · +%s Wegmünzen" % find.coins}
	return {"ok": false, "message": "Dieser Fund ist nicht bekannt."}
static func reward_battle(save: Dictionary, species: String, victories: int, boss: bool = false) -> Dictionary:
	var s: = state(save, species)
	if victories <= int(s.last_victory): return {}

	s.last_victory = victories;s.serial = int(s.serial) + 1
	var rng: = _rng(s); var pool: Array = data().pools.rare if boss else data().pools.common + data().pools.uncommon
	var id: = _pick(s, pool, rng);s.owned[id] = int(s.owned.get(id, 0)) + 1
	var coins: = 18 if boss else 8;s.coins = int(s.coins) + coins
	var reward: Dictionary = {"id": id, "coins": coins, "victory": victories};s.last_reward = reward
	return reward
static func trade(save: Dictionary, species: String, id: String) -> Dictionary:
	var s: = state(save, species)
	for offer in data().trades:
		if offer.id != id: continue
		if s.claims.get("trade:" + id, false): return {"ok": false, "message": "Dieser Tausch ist bereits abgeschlossen."}
		var deck: Array = save.get("card_decks", {}).get(species, Cards.starter(species))
		var reserve: = maxi(1, deck.count(offer.give))
		if int(s.owned.get(offer.give, 0)) <= reserve: return {"ok": false, "message": "Du brauchst ein freies Duplikat. Dein Deck und deine letzte Kopie bleiben erhalten."}
		s.owned[offer.give] = int(s.owned[offer.give]) - 1;s.owned[offer.receive] = int(s.owned.get(offer.receive, 0)) + 1;s.claims["trade:" + id] = true
		return {"ok": true, "cards": [{"id": offer.receive, "new": int(s.owned[offer.receive]) == 1}], "message": "Tausch abgeschlossen · " + Cards.get_card(offer.receive).name}
	return {"ok": false, "message": "Kein passendes Tauschangebot."}
