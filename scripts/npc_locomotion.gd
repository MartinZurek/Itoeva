extends RefCounted


var cycles: Dictionary = {}
var textures: Dictionary = {}
var distance_phase: = 0.0
var idle_clock: = 0.0
var mode: = "rest"
var phase: = 0
var flight_clock: = 0.0
var applied_sprite: = 0
var applied_material: = 0
var applied_mode: = ""
var applied_phase: = -1
var applied_size: = -1.0
var pose_uploads: = 0

static func available(id: String) -> bool:
	return FileAccess.file_exists("res://data/npc_motion/" + id + ".json")

func configure(id: String) -> void :
	cycles = JSON.parse_string(FileAccess.get_file_as_string("res://data/npc_motion/" + id + ".json"))
	for key in ["walk", "rest"]:
		textures[key] = load(cycles[key].pixel_asset)

func advance(delta: float, distance: float, pixel_size: float) -> void :
	if cycles.get("locomotion", "ground") in ["flight", "hover"]:


		var rate: float = float(cycles.get("hover_cycles_per_second", cycles.get("wingbeats_per_second", 1.4)))
		flight_clock = fposmod(flight_clock + delta * rate, 1.0)
		mode = "walk" if distance > 1e-05 else "rest"
		phase = int(floor(flight_clock * int(cycles[mode].frames)))
		return
	if distance > 1e-05:
		if mode == "rest": distance_phase = 0.0
		mode = "walk"
		idle_clock = 0.0
		distance_phase = fposmod(distance_phase + distance / maxf(0.01, float(cycles.walk.cycle_distance_pixels) * pixel_size), 1.0)
		phase = int(floor(distance_phase * int(cycles.walk.frames)))
	else:


		idle_clock += delta
		if idle_clock >= 0.14:
			mode = "rest"
			phase = int(floor((idle_clock - 0.14) * 2.0)) % int(cycles.rest.frames)

func apply(sprite: Sprite3D, material: ShaderMaterial, base_pixel_size: float) -> void :
	var data: Dictionary = cycles[mode]
	var size: = world_pixel_size(base_pixel_size)

	if applied_sprite == sprite.get_instance_id() and applied_material == material.get_instance_id() and applied_mode == mode and applied_phase == phase and is_equal_approx(applied_size, size) and sprite.frame == phase and sprite.texture == textures[mode]: return
	applied_sprite = sprite.get_instance_id();applied_material = material.get_instance_id()
	applied_mode = mode;applied_phase = phase;applied_size = size;pose_uploads += 1
	if sprite.texture != textures[mode]:
		sprite.frame = 0
		sprite.texture = textures[mode]
		sprite.hframes = int(data.columns)
		sprite.vframes = int(data.rows)
		material.set_shader_parameter("atlas", textures[mode])
	sprite.frame = phase
	sprite.pixel_size = size
	sprite.offset = Vector2(0, float(data.ground) - float(data.cell) * 0.5)
	sprite.texture_filter = BaseMaterial3D.TEXTURE_FILTER_NEAREST


	var uv: = Vector2(phase % int(data.columns), phase / int(data.columns)) / Vector2(int(data.columns), int(data.rows))
	material.set_shader_parameter("frame_clip", Vector4(uv.x, uv.y, 1.0 / int(data.columns), 1.0 / int(data.rows)))

func world_pixel_size(base_pixel_size: float) -> float:
	return base_pixel_size * 109.0 * float(cycles.world_height) / float(cycles.walk.reference_height)
