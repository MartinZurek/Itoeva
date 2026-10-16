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
 var uv := Vector2(entry.uv[0], entry.uv[1])
 if entry.place in ["FOREST_ENTRANCE", "GREENWOOD"]:
  return Forest.position_for(0 if entry.place == "FOREST_ENTRANCE" else 1, uv.x, uv.y)
 return Catalog.position_for(entry.place, uv)
static func portrait(id: String) -> AtlasTexture:
 if int(data()[id].row) < 0: return Catalog.portrait(id)
 var rows: Array = JSON.parse_string(FileAccess.get_file_as_string(data()[id].get("atlas", "res://data/citizens_atlas.json")))
 var b: Array = rows[int(data()[id].row)][0]
 var atlas := AtlasTexture.new()
 atlas.atlas = load(data()[id].get("asset", "res://assets/expansion/citizens.png"))
 atlas.region = Rect2(b[0],b[1],b[2],b[3])
 return atlas
