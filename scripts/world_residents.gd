extends RefCounted
const Catalog = preload("res://scripts/world_catalog.gd")
const Forest = preload("res://scripts/forest_catalog.gd")
static var cache: Dictionary = {}
static func data() -> Dictionary:
	if cache.is_empty(): cache = JSON.parse_string(FileAccess.get_file_as_string("res://data/residents.json"))
	return cache
static func appointment(id: String, hour: int) -> Dictionary:
	var selected: Dictionary = data()[id].schedule[0]
	for entry in data()[id].schedule:
		if hour >= int(entry.hour): selected = entry
	return selected.duplicate(true)
static func room_for(place: String) -> String:
	if place in ["FOREST_ENTRANCE", "GREENWOOD"]: return "FOREST_TRAIL"
	return "OUTDOOR" if not Catalog.region_for(place).is_empty() else place
static func position_for(entry: Dictionary) -> Vector3:
	var uv: = Vector2(entry.uv[0], entry.uv[1])
	if entry.place in ["FOREST_ENTRANCE", "GREENWOOD"]:
		return Forest.position_for(0 if entry.place == "FOREST_ENTRANCE" else 1, uv.x, uv.y)
	return Catalog.position_for(entry.place, uv)
static func portrait(id: String) -> AtlasTexture:
	var profile: Dictionary = data()[id]
	var atlas: AtlasTexture
	if int(profile.row) < 0:
		atlas = Catalog.portrait(id)
	else:
		var rows: Array = JSON.parse_string(FileAccess.get_file_as_string(profile.get("atlas", "res://data/citizens_atlas.json")))
		var bounds: Array = rows[int(profile.row)][0]
		atlas = AtlasTexture.new()
		atlas.atlas = load(profile.get("asset", "res://assets/expansion/citizens.png"))
		atlas.region = Rect2(bounds[0], bounds[1], bounds[2], bounds[3])
	if profile.has("portrait_frame"):
		var crop: Array = profile.portrait_frame
		var bounds := Rect2(atlas.region.position + Vector2(crop[0], crop[1]), Vector2(crop[2], crop[3]))
		atlas.region = atlas.region.intersection(bounds)
	atlas.filter_clip = true
	return atlas

static func help_for(id: String) -> Dictionary:
	return data()[id].get("help", {})
