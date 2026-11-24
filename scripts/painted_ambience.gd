extends RefCounted

const SHADER = preload("res://shaders/painted_opaque.gdshader")
const TRANSPARENT_SHADER = preload("res://shaders/painted_ambience.gdshader")
const Masks = preload("res://scripts/environment_masks.gd")
const MAX_LIGHTS: = 16
static var catalogue: Dictionary = {}
static var masks: Dictionary = {}

static func data() -> Dictionary:
	if catalogue.is_empty():
		var parsed = JSON.parse_string(FileAccess.get_file_as_string("res://data/ambience.json"))
		assert (parsed is Dictionary, "Ambience-Koordinaten fehlen")
		catalogue = parsed
	return catalogue

static func create_material(asset: String, texture: Texture2D, transparent: bool = false) -> ShaderMaterial:
	var material: = ShaderMaterial.new()
	var entry: Dictionary = data().assets.get(asset.get_file(), {})
	if asset == "res://assets/expansion/sun-glade-junctions.png":
		entry = data().assets.get("sun-glade.png", {})
	var mask_key := "sun-glade.png" if asset == "res://assets/expansion/sun-glade-junctions.png" else asset.get_file()
	var has_water := Masks.water_surfaces().has(mask_key)
	var has_fire: bool = not entry.get("flames", []).is_empty() or entry.get("ember_reaction", false)
	var effect := "water_fire" if has_water and has_fire else "water" if has_water else "fire" if has_fire else ""
	if not transparent and asset == "res://assets/expansion/sun-glade-junctions.png":
		material.shader = preload("res://shaders/painted_sun_glade.gdshader")
		material.set_shader_parameter("shrub_motion_mask", preload("res://assets/expansion/sun-glade-shrub-mask.png"))
	elif not transparent and asset == "res://assets/expansion/mill-brook.png":
		material.shader = preload("res://shaders/painted_mill.gdshader")
	elif effect.is_empty():material.shader=TRANSPARENT_SHADER if transparent else SHADER
	else: material.shader = load("res://shaders/painted_" + effect + ("_alpha" if transparent else "_opaque") + ".gdshader")
	material.set_shader_parameter("painted_texture", texture)
	material.set_shader_parameter("depth_air", 0.07 if asset.contains("/world/") else 0.0)
	if not entry.get("flames", []).is_empty(): material.set_shader_parameter("flame_atlas", load("res://assets/combat/flame-atlas.png"))
	var dimensions: Array = entry.get("size", [texture.get_width(), texture.get_height()])
	var source_size: = Vector2(dimensions[0], dimensions[1])
	material.set_shader_parameter("source_size", source_size)
	configure_environment(material, mask_key, mask_key, not entry.get("clouds", []).is_empty())
	var sheen: Array = Masks.dynamics().get(mask_key, {}).get("sheen", [])
	if not sheen.is_empty():
		material.set_shader_parameter("surface_sheen", Vector4(sheen[0], sheen[1], sheen[2], sheen[3]))
		material.set_shader_parameter("sheen_color", Vector3(sheen[4], sheen[5], sheen[6]))
	var positions: = PackedVector4Array()
	var strengths: = PackedVector4Array()
	var lights: Array = entry.get("lights", [])
	assert (lights.size() <= MAX_LIGHTS, "Zu viele Lichtquellen in " + asset)
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

		var color: = Color("ffbd72").srgb_to_linear()
		var strength: float = float(light.get("strength", 0.3))
		strengths.append(Vector4(color.r, color.g, color.b, - strength if light.kind == "fire" else strength))
	material.set_shader_parameter("light_count", lights.size())
	material.set_shader_parameter("light_positions", positions)
	material.set_shader_parameter("light_strengths", strengths)
	var fires: = PackedVector4Array()
	for area in entry.get("flames", []):
		fires.append(Vector4(area[0] / source_size.x, area[1] / source_size.y, area[2] / source_size.y, area[3] / source_size.x))
	material.set_shader_parameter("fire_count", fires.size())
	while fires.size() < 4: fires.append(Vector4.ZERO)
	material.set_shader_parameter("fire_areas", fires)
	material.set_shader_parameter("ember_gain", 1.0 if entry.get("ember_reaction", false) else 0.0)
	set_time(material, 12.0, asset.contains("/interiors/"))
	return material

static func set_time(material: ShaderMaterial, hour: float, indoors: bool) -> void :
	if material == null:
		return
	material.set_shader_parameter("scene_tint", scene_color(hour, indoors))
	var local_hour: = fposmod(hour, 24.0) if is_finite(hour) else 12.0
	var daylight: = smoothstep(5.8, 8.0, local_hour) * (1.0 - smoothstep(18.0, 21.0, local_hour))
	material.set_shader_parameter("lamp_gain", lerpf(0.95 if indoors else 1.1, 0.22, daylight))
	material.set_shader_parameter("fire_gain", lerpf(1.25, 0.78, daylight))
	material.set_shader_parameter("sheen_daylight", daylight if indoors else lerpf(0.65, 1.0, daylight))

static func scene_color(hour: float, indoors: bool) -> Color:
	var local_hour: = fposmod(hour, 24.0) if is_finite(hour) else 12.0
	var daylight: = smoothstep(5.8, 8.0, local_hour) * (1.0 - smoothstep(18.0, 21.0, local_hour))
	var night_tint: = Color(0.62, 0.59, 0.59) if indoors else Color(0.32, 0.4, 0.58)
	var tint: = night_tint.lerp(Color.WHITE, daylight)
	if not indoors:
		var dawn: = smoothstep(5.8, 6.8, local_hour) * (1.0 - smoothstep(7.3, 8.0, local_hour))
		var dusk: = smoothstep(17.8, 18.8, local_hour) * (1.0 - smoothstep(20.3, 21.0, local_hour))
		tint = tint.lerp(Color(1.0, 0.79, 0.58), dawn * 0.26)
		tint = tint.lerp(Color(1.0, 0.62, 0.44), dusk * 0.32)
	return tint

static func configure_environment(material: ShaderMaterial, motion_key: String, water_key: String, has_clouds: bool = false) -> void:
	var motion := Masks.motion_texture(motion_key)
	if motion != null:
		material.set_shader_parameter("motion_mask", motion)
	material.set_shader_parameter("has_environment_motion", motion != null)
	var profile: Dictionary = Masks.water_surfaces().get(water_key, {})
	material.set_shader_parameter("has_water", not profile.is_empty())
	material.set_shader_parameter("has_clouds", has_clouds)
	if not profile.is_empty() or has_clouds:
		if not masks.has(water_key):
			masks[water_key] = Masks.texture_for(water_key)
		material.set_shader_parameter("water_mask", masks[water_key])
	if not profile.is_empty():
		var optics: Array = profile.optics
		material.set_shader_parameter("water_optics", Vector4(optics[0], optics[1], optics[2], optics[3]))
