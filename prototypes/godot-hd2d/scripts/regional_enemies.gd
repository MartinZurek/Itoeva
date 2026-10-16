extends RefCounted
static var cache: Dictionary={}
static var bounds: Array=[]
static var demon_bounds: Dictionary={}
static func data() -> Dictionary:
 if cache.is_empty():cache=JSON.parse_string(FileAccess.get_file_as_string("res://data/regional_enemies.json"))
 return cache
static func region(id: String, pose: int=0) -> Rect2:
 if data()[id].has("atlas"):
  if demon_bounds.is_empty():demon_bounds=JSON.parse_string(FileAccess.get_file_as_string("res://data/demons_atlas.json"))
  var r: Array=demon_bounds[data()[id].atlas][clampi(pose,0,5)]
  return Rect2(r[0],r[1],r[2],r[3])
 if bounds.is_empty():bounds=JSON.parse_string(FileAccess.get_file_as_string("res://data/enemies_atlas.json"))
 var b: Array=bounds[int(data()[id].row)][clampi(pose,0,5)]
 return Rect2(b[0],b[1],b[2],b[3])
static func texture(id: String,pose: int=0) -> AtlasTexture:
 var atlas:=AtlasTexture.new();atlas.atlas=load(asset(id));atlas.region=region(id,pose)
 return atlas

static func asset(id: String) -> String:
 return str(data()[id].get("asset","res://assets/expansion/enemies.png"))
