extends RefCounted
static var cache: Dictionary = {}
static var bounds: Array = []
static var demon_bounds: Dictionary = {}
static var ecology_bounds: Dictionary = {}
static func data() -> Dictionary:
	if cache.is_empty(): cache = JSON.parse_string(FileAccess.get_file_as_string("res://data/regional_enemies.json"))
	return cache
static func region(id: String, pose: int = 0) -> Rect2:
	if data()[id].get("static_image", false):
		var art: Texture2D = load(asset(id))
		return Rect2(Vector2.ZERO, art.get_size())
	if data()[id].get("ecology_atlas", false):
		if ecology_bounds.is_empty(): ecology_bounds = JSON.parse_string(FileAccess.get_file_as_string("res://data/ecology_atlas.json"))
		var poses: Array = data()[id].pose_map
		var rect: Array = ecology_bounds[data()[id].atlas][int(poses[clampi(pose, 0, poses.size() - 1)])]
		return Rect2(rect[0], rect[1], rect[2], rect[3])
	if data()[id].has("atlas"):
		if demon_bounds.is_empty(): demon_bounds = JSON.parse_string(FileAccess.get_file_as_string("res://data/demons_atlas.json"))
		var r: Array = demon_bounds[data()[id].atlas][clampi(pose, 0, 5)]
		return Rect2(r[0], r[1], r[2], r[3])
	if bounds.is_empty(): bounds = JSON.parse_string(FileAccess.get_file_as_string("res://data/enemies_atlas.json"))
	var b: Array = bounds[int(data()[id].row)][clampi(pose, 0, 5)]
	return Rect2(b[0], b[1], b[2], b[3])
static func texture(id: String, pose: int = 0) -> AtlasTexture:
	var atlas: = AtlasTexture.new();atlas.atlas = load(asset(id));atlas.region = region(id, pose);atlas.filter_clip = true
	return atlas

static func asset(id: String) -> String:
	return str(data()[id].get("asset", "res://assets/expansion/enemies.png"))

static func heavy_motion(id: String) -> bool:
	return id in ["cinder_carrier", "vharok", "relay_warden"]

# A distance-driven weight shift for the heavy silhouettes without a walk atlas.
# x rocks around the contact point, y compresses the torso on each footfall.
static func weight_shift(clock: float, stride: float, walking: float) -> Vector2:
	var rest := Vector2(sin(clock * 1.4) * 0.008, sin(clock * 1.8) * 0.009)
	var step := Vector2(sin(stride * TAU) * 0.048, -absf(sin(stride * TAU)) * 0.032)
	return rest.lerp(step, walking)

static func encounter_portrait(id: String) -> Texture2D:
	var index := ["cinder_carrier", "vharok", "relay_warden"].find(id)
	if index < 0: return texture(id)
	var art: Texture2D = load("res://assets/expansion/guardian-concepts.webp")
	var frame := AtlasTexture.new()
	frame.atlas = art
	var width := art.get_width() / 3.0
	frame.region = Rect2(width * index, 0, width, art.get_height())
	frame.filter_clip = true
	return frame
