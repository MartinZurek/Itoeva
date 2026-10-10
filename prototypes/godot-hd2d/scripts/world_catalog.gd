extends RefCounted

const PITCH := 35.0
const IMAGE_HEIGHT := 10.0
const ORIGIN_V := 0.84
static var cache: Dictionary = {}

static func data() -> Dictionary:
	if cache.is_empty():
		var parsed = JSON.parse_string(FileAccess.get_file_as_string("res://data/world_catalog.json"))
		assert(parsed is Dictionary, "Weltkatalog fehlt oder ist ungueltig")
		cache = parsed
	return cache

static func region_for(place: String) -> Dictionary:
	for region in data().regions:
		if place in region.places:
			return region
	return {}

static func image_z(y: float, native_height: float = 640.0) -> float:
	return (y / native_height - ORIGIN_V) * IMAGE_HEIGHT / sin(deg_to_rad(PITCH))

static func image_y(y: float, z: float, native_height: float) -> float:
	return ((ORIGIN_V - y / native_height) * IMAGE_HEIGHT + sin(deg_to_rad(PITCH)) * z) / cos(deg_to_rad(PITCH))

static func band(region: Dictionary, x: float, dry: bool = false) -> Vector2:
	if region.places[0] == "COAST_PATH":
		var points: Array = data().coast_path_band
		for i in range(1, points.size()):
			if x <= points[i][0]:
				var t := inverse_lerp(float(points[i - 1][0]), float(points[i][0]), x)
				return Vector2(lerpf(points[i - 1][1], points[i][1], t), lerpf(points[i - 1][2], points[i][2], t))
	if dry and region.places[0] == "POND":
		var px := x * 1920.0
		var near := 491.0 + 20.0 * sin(x * 13.0)
		if px >= 124.0 and px <= 354.0:
			near = 518.0
		elif px >= 1136.0 and px <= 1292.0:
			near = maxf(near, 478.0)
		return Vector2(450.0, near)
	return Vector2(450.0, 625.0)

static func position_for(place: String, uv: Vector2) -> Vector3:
	var region := region_for(place)
	if not region.is_empty():
		var section: float = region.width / region.places.size()
		var x: float = region.origin + region.places.find(place) * section + uv.x * section
		var limits := band(region, (x - float(region.origin)) / float(region.width), true)
		return Vector3(x, 0.03, image_z(lerpf(limits.x, limits.y, uv.y)))
	var room: Dictionary = data().interiors[place]
	var left := lerpf(room.limits[0], room.limits[2], uv.y)
	var right := lerpf(room.limits[1], room.limits[3], uv.y)
	return Vector3((lerpf(left, right, uv.x) / 480.0 - 0.5) * (IMAGE_HEIGHT * 16.0 / 9.0), 0.03,
		image_z(lerpf(room.band[0], room.band[1], uv.y), 270.0))

static func place_at(x: float) -> String:
	var index := clampi(int(x / 30.0), 0, data().regions.size() - 1)
	var region: Dictionary = data().regions[index]
	var section: float = region.width / region.places.size()
	return region.places[clampi(int((x - float(region.origin)) / section), 0, region.places.size() - 1)]

static func doors_for(place: String) -> Array[Dictionary]:
	var result: Array[Dictionary] = []
	for door in data().doors:
		if place == door.a:
			result.append({"from": place, "to": door.b, "at": Vector2(door.a_pos[0], door.a_pos[1]), "arrival": Vector2(door.b_pos[0], door.b_pos[1])})
		elif place == door.b:
			result.append({"from": place, "to": door.a, "at": Vector2(door.b_pos[0], door.b_pos[1]), "arrival": Vector2(door.a_pos[0], door.a_pos[1])})
	return result
