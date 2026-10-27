extends RefCounted
# Jede Figur bekommt eigene Zeichnungen und einen eigenen, eingefrorenen Maßstab.
# Der Weg gehört weiterhin der vorhandenen Revier-/Bewohnersteuerung.
var cycles: Dictionary = {}
var textures: Dictionary = {}
var distance_phase := 0.0
var idle_clock := 0.0
var mode := "rest"
var phase := 0
var flight_clock := 0.0

static func available(id: String) -> bool:
	return FileAccess.file_exists("res://data/npc_motion/" + id + ".json")

func configure(id: String) -> void:
	cycles = JSON.parse_string(FileAccess.get_file_as_string("res://data/npc_motion/" + id + ".json"))
	for key in ["walk", "rest"]:
		textures[key] = load(cycles[key].pixel_asset)

func advance(delta: float, distance: float, pixel_size: float) -> void:
	if cycles.get("locomotion", "ground") == "flight":
		# Schweben braucht Auftrieb auch ohne Wegfortschritt. Derselbe gemalte
		# Flügelschlag läuft beim Anhalten weiter, ohne Phasensprung oder Verzug.
		flight_clock = fposmod(flight_clock + delta * float(cycles.wingbeats_per_second), 1.0)
		mode = "walk" if distance > .00001 else "rest"
		phase = int(floor(flight_clock * int(cycles[mode].frames)))
		return
	if distance > 0.00001:
		if mode == "rest": distance_phase = 0.0
		mode = "walk"
		idle_clock = 0.0
		distance_phase = fposmod(distance_phase + distance / maxf(.01, float(cycles.walk.cycle_distance_pixels) * pixel_size), 1.0)
		phase = int(floor(distance_phase * int(cycles.walk.frames)))
	else:
		# Kein Weiterstrampeln an einer Wand. Beim Halten bleibt der zuletzt
		# belastete Schritt kurz stehen, bevor die ruhige Standfolge beginnt.
		idle_clock += delta
		if idle_clock >= .14:
			mode = "rest"
			phase = int(floor((idle_clock - .14) * 2.0)) % int(cycles.rest.frames)

func apply(sprite: Sprite3D, material: ShaderMaterial, base_pixel_size: float) -> void:
	var data: Dictionary = cycles[mode]
	var size := world_pixel_size(base_pixel_size)
	if sprite.texture != textures[mode]:
		sprite.frame = 0
		sprite.texture = textures[mode]
		sprite.hframes = int(data.columns)
		sprite.vframes = int(data.rows)
		material.set_shader_parameter("atlas", textures[mode])
	sprite.frame = phase
	sprite.pixel_size = size
	sprite.offset = Vector2(0, float(data.ground) - float(data.cell) * .5)
	sprite.texture_filter = BaseMaterial3D.TEXTURE_FILTER_NEAREST
	# Die ganze registrierte Zelle ist exklusiv; keine dynamische Größenanpassung
	# an Schwanz, Flügel oder eine vorübergehend angehobene Pfote.
	var uv := Vector2(phase % int(data.columns), phase / int(data.columns)) / Vector2(int(data.columns), int(data.rows))
	material.set_shader_parameter("frame_clip", Vector4(uv.x, uv.y, 1.0 / int(data.columns), 1.0 / int(data.rows)))

func world_pixel_size(base_pixel_size: float) -> float:
	return base_pixel_size * 109.0 * float(cycles.world_height) / float(cycles.walk.reference_height)
