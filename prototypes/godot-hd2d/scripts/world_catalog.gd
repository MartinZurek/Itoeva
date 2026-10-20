extends RefCounted

const STARTERS := ["mosslyn", "nocti", "starlet", "wyrmling"]
const PITCH := 35.0
const IMAGE_HEIGHT := 10.0
const ORIGIN_V := 0.84
static var cache: Dictionary = {}
static var geometry_cache: Dictionary = {}

static func geometry() -> Dictionary:
	if geometry_cache.is_empty():
		geometry_cache = JSON.parse_string(FileAccess.get_file_as_string("res://data/visual_geometry.json"))
	for place in data().interiors:
		var room: Dictionary = data().interiors[place]
		if room.get("outdoor", false):
			geometry_cache.room_backs[place] = [[0,room.band[0],room.band[1]],[1,room.band[0],room.band[1]]]
	return geometry_cache

static func profile(points: Array, x: float) -> Vector2:
	for i in range(1, points.size()):
		if x <= float(points[i][0]):
			var t := clampf(inverse_lerp(float(points[i - 1][0]), float(points[i][0]), x), 0, 1)
			var back := lerpf(float(points[i - 1][1]), float(points[i][1]), t)
			var front := lerpf(float(points[i - 1][2]), float(points[i][2]), t) if points[i].size() > 2 else 256.0
			return Vector2(back, front)
	var last: Array = points.back()
	return Vector2(last[1], last[2] if last.size() > 2 else 256.0)

static func relative_height(species: String) -> float:
	if not data().characters.has(species):
		return float(JSON.parse_string(FileAccess.get_file_as_string("res://data/residents.json"))[species].height)
	return float(geometry().character_height.get(species, data().characters[species].relative_height))

static func data() -> Dictionary:
	if cache.is_empty():
		var parsed = JSON.parse_string(FileAccess.get_file_as_string("res://data/world_catalog.json"))
		assert(parsed is Dictionary, "Weltkatalog fehlt oder ist ungueltig")
		cache = parsed
		var expansion: Dictionary = JSON.parse_string(FileAccess.get_file_as_string("res://data/world_expansion.json"))
		cache.interiors.merge(expansion.interiors)
		cache.labels.merge(expansion.labels)
		cache.doors.append_array(expansion.doors)
		cache.characters.merge(JSON.parse_string(FileAccess.get_file_as_string("res://data/new_characters.json")),true)
	return cache

static func portrait(species: String) -> AtlasTexture:
	var atlas:=AtlasTexture.new()
	atlas.atlas=load(data().characters[species].asset)
	if data().characters[species].get("compact",false):
		atlas.region=Rect2(0,0,atlas.atlas.get_width()/8.0,atlas.atlas.get_height()/6.0)
	else:atlas.region=Rect2(10*128,11*128,128,128)
	return atlas

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
	if not dry:
		# Begehbare Bildpixel liegen auf derselben Ebene wie ihre Kollision.
		# Besonders Bruecken oberhalb y=450 waren vorher ein erhoehtes Bildrelief.
		return Vector2(band(region, x, true).x - 6.0, 625.0)
	var world_x: float = float(region.origin) + x * float(region.width)
	# Die Anschlussbilder ueberdecken je 7.5 Einheiten beider Originalpanoramen.
	# Ihre eigene Wegkontur hat deshalb auch fuer die Physik Vorrang.
	for i in geometry().seams.size():
		var boundary: float = (i + 1) * 30.0
		if absf(world_x - boundary) <= 7.5:
			var seam_band := profile(geometry().seams[i], (world_x - boundary + 7.5) / 15.0)
			var original := profile(geometry().paths[region.places[0]], x)
			return inset_band(original.lerp(seam_band, smoothstep(0.0, 2.0, 7.5 - absf(world_x - boundary))), geometry().walk_margin)
	return inset_band(profile(geometry().paths[region.places[0]], x), geometry().walk_margin)

static func inset_band(limits: Vector2, margin: float) -> Vector2:
	var inset := minf(margin, (limits.y - limits.x) * 0.18)
	return Vector2(limits.x + inset, limits.y - inset)

static func room_band(place: String, u: float) -> Vector2:
	return inset_band(profile(geometry().room_backs[place], u), geometry().room_margin)

static func room_limits(place: String, y: float) -> Vector2:
	var room: Dictionary = data().interiors[place]
	var t := clampf(inverse_lerp(float(room.band[0]), float(room.band[1]), y), 0, 1)
	return Vector2(lerpf(room.limits[0], room.limits[2], t), lerpf(room.limits[1], room.limits[3], t))

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
	var px := lerpf(left, right, uv.x)
	var limits := room_band(place, px / 480.0)
	return Vector3((px / 480.0 - 0.5) * (IMAGE_HEIGHT * 16.0 / 9.0), 0.03,
		image_z(lerpf(limits.x, limits.y, uv.y), 270.0))

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
	for entry in result:
		if is_outdoor(entry.from) and is_outdoor(entry.to):
			entry["route_direction"] = "LEFT" if entry.at.x < .35 else "RIGHT" if entry.at.x > .65 else "UP"
			# Der Sonnenhain ist ein Seitenweg, keine Umkehr entlang der Hauptstrasse.
			if entry.from == "FOREST": entry.route_direction = "UP"
	return result

static func is_outdoor(place: String) -> bool:
	return not region_for(place).is_empty() or data().interiors.get(place, {}).get("outdoor", false)
