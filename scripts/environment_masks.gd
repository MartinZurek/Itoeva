extends RefCounted

const MOTION_SIZE := Vector2i(128, 72)
const WATER_SIZE := Vector2i(240, 135)
static var records: Dictionary = {}
static var dynamic_textures: Dictionary = {}
static var dynamic_profiles: Dictionary = {}
static var liquid_profiles: Dictionary = {}

static func dynamics() -> Dictionary:
	if dynamic_profiles.is_empty():
		dynamic_profiles = JSON.parse_string(FileAccess.get_file_as_string("res://data/environment_dynamics.json"))
	return dynamic_profiles

static func water_surfaces() -> Dictionary:
	if liquid_profiles.is_empty():
		liquid_profiles = JSON.parse_string(FileAccess.get_file_as_string("res://data/water_surfaces.json"))
	return liquid_profiles

static func has_motion(key: String) -> bool:
	return dynamics().get(key, {}).has("sky") or dynamics().get(key, {}).has("foliage")

static func motion_texture(key: String) -> Texture2D:
	if not has_motion(key):
		return null
	if dynamic_textures.has(key):
		return dynamic_textures[key]
	var profile: Dictionary = dynamics()[key]
	var image := Image.create(MOTION_SIZE.x, MOTION_SIZE.y, false, Image.FORMAT_RG8)
	for channel in 2:
		for area: Array in profile.get("sky" if channel == 0 else "foliage", []):
			var limit := Vector2(MOTION_SIZE)
			var start := Vector2i(Vector2(area[0], area[1]) * limit)
			var end := Vector2i((Vector2(area[2], area[3]) * limit).ceil())
			for y in range(start.y, end.y):
				for x in range(start.x, end.x):
					var uv := (Vector2(x, y) + Vector2(0.5, 0.5)) / limit
					var edge := minf(minf(uv.x - area[0], area[2] - uv.x), minf(uv.y - area[1], area[3] - uv.y))
					var amount := smoothstep(0.0, 0.024, edge)
					var color := image.get_pixel(x, y)
					if channel == 0:
						color.r = maxf(color.r, amount)
					else:
						color.g = maxf(color.g, amount)
					image.set_pixel(x, y, color)
	var texture := ImageTexture.create_from_image(image)
	dynamic_textures[key] = texture
	return texture

static func data() -> Dictionary:
	if records.is_empty():
		records = JSON.parse_string(FileAccess.get_file_as_string("res://data/environment_masks.json"))
	return records

static func texture_for(key: String) -> Texture2D:
	var entry: Dictionary = data().masks.get(key, {})
	var image: Image
	if entry.is_empty():
		image = Image.create(1, 1, false, Image.FORMAT_RG8)
	else:
		var bytes := Marshalls.base64_to_raw(entry.deflate).decompress(int(entry.bytes), FileAccess.COMPRESSION_DEFLATE)
		image = Image.create_from_data(int(entry.width), int(entry.height), false, Image.FORMAT_RG8, bytes)
	var contours: Array = water_surfaces().get(key, {}).get("contours", [])
	if not contours.is_empty():
		image.resize(WATER_SIZE.x, WATER_SIZE.y, Image.INTERPOLATE_BILINEAR)
		var bytes := image.get_data()
		# Die überarbeiteten Konturen ersetzen Rot. Alte Wolkenfenster in Grün bleiben erhalten.
		for pixel in range(0, bytes.size(), 2):
			bytes[pixel] = 0
		for contour: Array in contours:
			_paint_water(bytes, contour)
		image.set_data(WATER_SIZE.x, WATER_SIZE.y, false, Image.FORMAT_RG8, bytes)
	return ImageTexture.create_from_image(image)

static func _paint_water(bytes: PackedByteArray, contour: Array) -> void:
	var polygon := PackedVector2Array()
	for point: Array in contour:
		polygon.append(Vector2(point[0], point[1]) * Vector2(WATER_SIZE))
	var bounds := Rect2(polygon[0], Vector2.ZERO)
	for point in polygon:
		bounds = bounds.expand(point)
	for y in range(floori(bounds.position.y), ceili(bounds.end.y)):
		for x in range(floori(bounds.position.x), ceili(bounds.end.x)):
			var point := Vector2(x + 0.5, y + 0.5)
			if not Geometry2D.is_point_in_polygon(point, polygon):
				continue
			var distance := 2.3
			for i in polygon.size():
				var edge := Geometry2D.get_closest_point_to_segment(point, polygon[i], polygon[(i + 1) % polygon.size()])
				distance = minf(distance, edge.distance_to(point))
			var offset := (y * WATER_SIZE.x + x) * 2
			bytes[offset] = maxi(bytes[offset], roundi(255.0 * smoothstep(0.0, 2.3, distance)))
