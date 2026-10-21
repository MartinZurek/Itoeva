extends RefCounted

const SHADER = preload("res://shaders/painted_opaque.gdshader")
const TRANSPARENT_SHADER = preload("res://shaders/painted_ambience.gdshader")
const Masks = preload("res://scripts/environment_masks.gd")
const MAX_LIGHTS := 16
static var catalogue: Dictionary = {}
static var masks: Dictionary = {}

static func data() -> Dictionary:
	if catalogue.is_empty():
		var parsed = JSON.parse_string(FileAccess.get_file_as_string("res://data/ambience.json"))
		assert(parsed is Dictionary, "Ambience-Koordinaten fehlen")
		catalogue = parsed
	return catalogue

static func create_material(asset: String, texture: Texture2D, transparent: bool = false) -> ShaderMaterial:
	var material := ShaderMaterial.new()
	material.shader = TRANSPARENT_SHADER if transparent else SHADER
	material.set_shader_parameter("painted_texture", texture)
	material.set_shader_parameter("flame_atlas",load("res://assets/combat/flame-atlas.png"))
	var entry: Dictionary = data().assets.get(asset.get_file(), {})
	var dimensions: Array = entry.get("size", [texture.get_width(), texture.get_height()])
	var source_size := Vector2(dimensions[0], dimensions[1])
	material.set_shader_parameter("source_size", source_size)
	material.set_shader_parameter("water_mask", _water_mask(asset.get_file(), entry, source_size))
	material.set_shader_parameter("has_water", not entry.get("water", []).is_empty())
	material.set_shader_parameter("has_clouds", not entry.get("clouds", []).is_empty())
	var positions := PackedVector4Array()
	var strengths := PackedVector4Array()
	var lights: Array = entry.get("lights", [])
	assert(lights.size() <= MAX_LIGHTS, "Zu viele Lichtquellen in " + asset)
	for index in MAX_LIGHTS:
		if index >= lights.size():
			positions.append(Vector4.ZERO)
			strengths.append(Vector4.ZERO)
			continue
		var light: Dictionary = lights[index]
		var at: Array = light.at
		var radius: Array = light.radius
		positions.append(Vector4(float(at[0]) / source_size.x, float(at[1]) / source_size.y,
			float(radius[0]) / source_size.x, float(radius[1]) / source_size.y))
		# xyz: lineare Lichtfarbe; w: negative Staerke kennzeichnet Feuer.
		var color := Color("ffbd72").srgb_to_linear()
		var strength: float = float(light.get("strength", 0.30))
		strengths.append(Vector4(color.r, color.g, color.b, -strength if light.kind == "fire" else strength))
	material.set_shader_parameter("light_count", lights.size())
	material.set_shader_parameter("light_positions", positions)
	material.set_shader_parameter("light_strengths", strengths)
	var fires := PackedVector4Array()
	for area in entry.get("flames",[]):
		fires.append(Vector4(area[0]/source_size.x,area[1]/source_size.y,area[2]/source_size.y,area[3]/source_size.x))
	material.set_shader_parameter("fire_count",fires.size())
	while fires.size()<4: fires.append(Vector4.ZERO)
	material.set_shader_parameter("fire_areas",fires)
	set_time(material, 12.0, asset.contains("/interiors/"))
	return material

static func set_time(material: ShaderMaterial, hour: float, indoors: bool) -> void:
	if material == null:
		return
	var local_hour := fposmod(hour, 24.0) if is_finite(hour) else 12.0
	var daylight := smoothstep(5.8, 8.0, local_hour) * (1.0 - smoothstep(18.0, 21.0, local_hour))
	var night_tint := Color(0.62, 0.59, 0.59) if indoors else Color(0.32, 0.40, 0.58)
	var tint := night_tint.lerp(Color.WHITE, daylight)
	if not indoors:
		var dawn := smoothstep(5.8, 6.8, local_hour) * (1.0 - smoothstep(7.3, 8.0, local_hour))
		var dusk := smoothstep(17.8, 18.8, local_hour) * (1.0 - smoothstep(20.3, 21.0, local_hour))
		tint = tint.lerp(Color(1.0, 0.79, 0.58), dawn * 0.26)
		tint = tint.lerp(Color(1.0, 0.62, 0.44), dusk * 0.32)
	material.set_shader_parameter("scene_tint", tint)
	material.set_shader_parameter("lamp_gain", lerpf(0.82 if indoors else 0.90, 0.04, daylight))
	material.set_shader_parameter("fire_gain", lerpf(0.95, 0.36, daylight))

static func _polygon(points: Array) -> PackedVector2Array:
	var result := PackedVector2Array()
	for point in points:
		result.append(Vector2(point[0], point[1]))
	return result

static func _water_mask(key: String, entry: Dictionary, source_size: Vector2) -> Texture2D:
	if masks.has(key):
		return masks[key]
	var texture := Masks.texture_for(key)
	masks[key] = texture
	return texture
