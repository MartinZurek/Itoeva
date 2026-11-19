extends RefCounted

static var records: Dictionary = {}
static var dynamic_textures: Dictionary = {}

static var dynamic_profiles: Dictionary = {}

static func dynamics() -> Dictionary:
	if dynamic_profiles.is_empty():
		dynamic_profiles = JSON.parse_string(FileAccess.get_file_as_string("res://data/environment_dynamics.json"))
	return dynamic_profiles

static func motion_texture(key: String) -> Texture2D:
	if dynamic_textures.has(key): return dynamic_textures[key]
	var profile: Dictionary = dynamics().get(key, {})
	var image := Image.create(256, 144, false, Image.FORMAT_RG8)
	for channel in 2:
		for area: Array in profile.get("sky" if channel == 0 else "foliage", []):
			for y in range(maxi(0, int(area[1]*144)), mini(144, ceili(area[3]*144))):
				for x in range(maxi(0, int(area[0]*256)), mini(256, ceili(area[2]*256))):
					var uv := Vector2((x+.5)/256.0, (y+.5)/144.0)
					var edge := minf(minf(uv.x-area[0],area[2]-uv.x),minf(uv.y-area[1],area[3]-uv.y))
					var amount := smoothstep(0.0,.022,edge)
					var color := image.get_pixel(x,y)
					if channel == 0: color.r = maxf(color.r,amount)
					else: color.g = maxf(color.g,amount)
					image.set_pixel(x,y,color)
	var texture := ImageTexture.create_from_image(image)
	dynamic_textures[key] = texture
	return texture

static func data() -> Dictionary:
	if records.is_empty():
		records = JSON.parse_string(FileAccess.get_file_as_string("res://data/environment_masks.json"))
	return records

static func texture_for(key: String) -> Texture2D:
	var entry: Dictionary = data().masks.get(key, {})
	if entry.is_empty():
		var empty: = Image.create(1, 1, false, Image.FORMAT_RG8)
		return ImageTexture.create_from_image(empty)

	var bytes: = Marshalls.base64_to_raw(entry.deflate).decompress(int(entry.bytes), FileAccess.COMPRESSION_DEFLATE)
	return ImageTexture.create_from_image(Image.create_from_data(int(entry.width), int(entry.height), false, Image.FORMAT_RG8, bytes))
