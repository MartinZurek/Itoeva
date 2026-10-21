extends RefCounted

static var records: Dictionary = {}

static func data() -> Dictionary:
	if records.is_empty():
		records = JSON.parse_string(FileAccess.get_file_as_string("res://data/environment_masks.json"))
	return records

static func texture_for(key: String) -> Texture2D:
	var entry: Dictionary = data().masks.get(key, {})
	if entry.is_empty():
		var empty := Image.create(1, 1, false, Image.FORMAT_RG8)
		return ImageTexture.create_from_image(empty)
	# Vorverarbeitete Konturen vermeiden pixelweise Hauptthread-Arbeit bei Reisen.
	var bytes := Marshalls.base64_to_raw(entry.deflate).decompress(int(entry.bytes), FileAccess.COMPRESSION_DEFLATE)
	return ImageTexture.create_from_image(Image.create_from_data(int(entry.width), int(entry.height), false, Image.FORMAT_RG8, bytes))
