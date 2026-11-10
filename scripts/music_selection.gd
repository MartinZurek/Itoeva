extends RefCounted

static var catalogue: Dictionary = {}
var last: Dictionary = {}
var sequence: = 0

static func data() -> Dictionary:
	if catalogue.is_empty():
		catalogue = JSON.parse_string(FileAccess.get_file_as_string("res://data/music_playlists.json"))
	return catalogue

static func world_role(place: String, hour: int) -> String:
	if hour >= 21 or hour < 6: return "evening"
	if place in ["BEACH", "COAST_PATH", "POND"]: return "coast"
	if place in ["MOUNTAINS", "MOUNTAIN_PASS"]: return "mountains"
	if place in ["STREET", "CITY", "SHOP"]: return "town"
	if place in ["PARK", "MEADOW", "FOREST", "JUNGLE", "SWAMP", "PLAINS", "SUN_GLADE"] or place.begins_with("SHADOW_") or place == "FOREST_TRAIL": return "nature"
	if hour >= 18: return "evening"
	return "day"

func pick(role: String) -> String:
	var ids: Array = data().roles.get(role, data().roles.day)
	var candidates: = ids.filter( func(id): return id != last.get(role, ""))
	if candidates.is_empty(): candidates = ids
	var id: String = candidates[sequence % candidates.size()]
	sequence += 1
	last[role] = id
	return str(data().tracks[id].asset)
