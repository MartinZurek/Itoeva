extends RefCounted
const LOAD_DISTANCE: = 30.0
static var entries: Dictionary = {}
static func data() -> Dictionary:
	if entries.is_empty(): entries = JSON.parse_string(FileAccess.get_file_as_string("res://data/scenery_previews.json"))
	return entries
static func initial_assets(room: String, x: float) -> Array[String]:
	var result: Array[String] = []
	var right: = 0.0
	for entry in data().values():
		if entry.room == room: right = maxf(right, float(entry.span[1]))
	x = clampf(x, 2.3, maxf(2.3, right - 2.3))
	for asset in data():
		var entry: Dictionary = entries[asset]
		if entry.room == room and maxf(maxf(float(entry.span[0]) - x, x - float(entry.span[1])), 0) <= LOAD_DISTANCE: result.append(asset)
	return result
static func fallback(asset: String) -> Texture2D:
	var image: = Image.new()
	if image.load_webp_from_buffer(Marshalls.base64_to_raw(data()[asset].webp)) != OK: return null
	return ImageTexture.create_from_image(image)
