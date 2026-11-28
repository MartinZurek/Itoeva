extends RefCounted

const BIOMES := {
	"coast": ["BEACH", "COAST_PATH"],
	"wetlands": ["POND", "SWAMP", "MILL_BROOK", "MILL_RUN", "RING_WEIR", "ALDER_FORD", "SHADOW_BROOK"],
	"mountains": ["MOUNTAINS", "MOUNTAIN_PASS", "WIND_TERRACES", "SIGNAL_WATCH", "CAMP", "GROTTO"],
	"town": ["STREET", "CITY", "VILLAGE_EDGE"],
	"nature": ["PARK", "MEADOW", "FOREST", "JUNGLE", "PLAINS", "SUN_GLADE", "FOREST_TRAIL",
		"FOREST_ENTRANCE", "GREENWOOD", "TORCH_PATH", "SHADOW_GATE", "SHADOW_CLEARING", "SHADOW_DEPTHS"],
	"border": ["MIST_WATCH", "ASH_ASCENT", "SEAL_COURT", "RELAY_COURT"],
	"home": ["LIVING", "HALL", "BEDROOM", "KITCHEN", "BATH", "DESK", "NOOK", "WORK", "CRAFT"],
	"cafe": ["CAFE"], "shop": ["SHOP"], "arcade": ["ARCADE"], "sport": ["SPORT"],
}

static var catalogue: Dictionary = {}
var cursors: Dictionary = {}
var world_choices: Dictionary = {}

static func data() -> Dictionary:
	if catalogue.is_empty():
		catalogue = JSON.parse_string(FileAccess.get_file_as_string("res://data/music_playlists.json"))
	return catalogue

static func world_role(place: String) -> String:
	for role: String in BIOMES:
		if place in BIOMES[role]: return role
	return "day"

static func tempo_for(hour: float) -> float:
	var light := smoothstep(5.5, 8.5, hour) * (1.0 - smoothstep(17.0, 21.5, hour))
	var dawn := smoothstep(5.5, 6.5, hour) * (1.0 - smoothstep(7.0, 8.5, hour))
	return lerpf(0.89, 1.0, light) + 0.025 * dawn

func pick(role: String) -> String:
	return _pick_ids(role, data().roles.get(role, data().roles.day), "")

func pick_world(role: String, phase: String, current: String, entering: bool) -> String:
	# Beim Betreten prägt der Ort das erste Stück. Zu später Stunde wechseln Ortsstücke mit leiseren Einwürfen.
	var key := role + ":" + phase
	var step: int = world_choices.get(key, 0)
	if not entering: world_choices[key] = step + 1
	var pool := phase if not entering and phase != "day" and step % 2 == 0 else role
	return _pick_ids("world:" + pool, data().roles.get(pool, data().roles.day), current)

func _pick_ids(key: String, ids: Array, current: String) -> String:
	var cursor: int = cursors.get(key, 0)
	for offset in ids.size():
		var index := (cursor + offset) % ids.size()
		var id: String = ids[index]
		var path: String = data().tracks[id].asset
		if path == current and offset + 1 < ids.size(): continue
		cursors[key] = index + 1
		return path
	return ""
