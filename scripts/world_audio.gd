extends Node

const Forest = preload("res://scripts/forest_catalog.gd")
const Catalog = preload("res://scripts/world_catalog.gd")
const LOOPS = ["wind", "shore", "brook", "pond", "fire"]
const STEP_GAIN = 0.1
const BED_GAIN = 0.1
var app: Node
var bed: AudioStreamPlayer
var other_bed: AudioStreamPlayer
var step: AudioStreamPlayer
var beds: Array[AudioStreamPlayer] = []
var bed_roles: Array[String] = ["", ""]
var bed_gains: Array[float] = [0.0, 0.0]
var encoded: Dictionary = {}
var streams: Dictionary = {}
var role: = ""
var desired: = ""
var desired_strength: = 1.0
var tick: = 0.0
var owner_id: = 0
var last_contact: = ""
var steps_played: = 0
var step_clock: = 0.0
var bags: Dictionary = {}
var last_variants: Dictionary = {}
var rng: = RandomNumberGenerator.new()
func _ready() -> void :
	bed = AudioStreamPlayer.new();other_bed = AudioStreamPlayer.new();step = AudioStreamPlayer.new()
	beds = [bed, other_bed]
	add_child(bed);add_child(other_bed);add_child(step)
	rng.seed = 20261105
	encoded = JSON.parse_string(FileAccess.get_file_as_string("res://data/world_audio.json"))
func sound(key: String) -> AudioStream:
	if not streams.has(key):
		var stream: = AudioStreamOggVorbis.load_from_buffer(Marshalls.base64_to_raw(encoded[key]))
		stream.loop = key in LOOPS;streams[key] = stream
	return streams[key]
static func environment_kind(room: String, place: String, point: Vector3, lights: Array) -> String:
	for light in lights:
		if light.get("fire", false) and point.distance_to(light.position) < 3.2: return "fire"
	if room == "FOREST_TRAIL": return "brook" if Forest.region_at(point.x).id == "SHADOW_BROOK" else "wind"
	if place in ["BEACH", "COAST_PATH"]: return "shore"
	if place == "POND": return "pond"
	if place == "MILL_BROOK": return "brook"
	var outside: bool = room == "OUTDOOR" or Catalog.data().interiors.get(room, {}).get("outdoor", false)
	return "wind" if outside and place not in ["CITY", "STREET"] else ""
static func surface(room: String, place: String, point: Vector3) -> String:
	if room == "FOREST_TRAIL":
		var region: = Forest.region_at(point.x)
		var u: float = (point.x - float(region.origin)) / float(region.width)
		return "wood" if region.id == "SHADOW_BROOK" and u > 0.27 and u < 0.84 else "earth"
	if room == "OUTDOOR":
		if place == "COAST_PATH":
			var region: = Catalog.region_for(place)
			var u: float = (point.x - float(region.origin)) / float(region.width)
			return "wood" if u > 0.32 and u < 0.6 else "earth"
		return "stone" if place in ["CITY", "STREET"] else "earth"
	if place in ["ASH_ASCENT", "SEAL_COURT", "RELAY_COURT", "BATH"]: return "stone"
	if Catalog.data().interiors.get(room, {}).get("outdoor", false): return "earth"
	return "wood"
func _process(delta: float) -> void :
	step_clock = maxf(0.0, step_clock - delta)
	var allowed: bool = app.world_ready and not app.paused and not app.switching and app.effects_on and app.effects_volume > 0.001 and is_instance_valid(app.actor)
	if not allowed:
		for player in beds: player.stop()
		step.stop();bed_gains = [0.0, 0.0];bed_roles = ["", ""]
		role = "";desired = "";last_contact = "";tick = 0;return
	tick -= delta
	if tick <= 0:
		tick = 0.2;desired = environment_kind(app.room, app.current_place(), app.actor.position, app.stage.local_light_sources)
		desired_strength = 0.65 if desired in ["wind", "pond"] else 1.0
		if desired == "fire":
			desired_strength = 0.0
			for light in app.stage.local_light_sources:
				if light.get("fire", false): desired_strength = maxf(desired_strength, clampf(1.0 - app.actor.position.distance_to(light.position) / 3.2, 0.0, 1.0))
	_update_beds(delta)
	_contact()
func _update_beds(delta: float) -> void :
	if not desired.is_empty() and desired not in bed_roles:
		var index: = 0 if bed_gains[0] <= bed_gains[1] else 1
		beds[index].stop();bed_roles[index] = desired;bed_gains[index] = 0
		beds[index].stream = sound(desired);beds[index].volume_db = -80;beds[index].play()
	role = desired
	for i in 2:
		var target: float = desired_strength if bed_roles[i] == desired and not desired.is_empty() else 0.0
		bed_gains[i] = move_toward(bed_gains[i], target, delta * 0.8)
		beds[i].volume_db = linear_to_db(maxf(0.0001, app.effects_volume * BED_GAIN * bed_gains[i]))
		if bed_gains[i] <= 0 and target == 0:
			beds[i].stop();bed_roles[i] = ""
func next_step_key(kind: String) -> String:
	var bag: Array = bags.get(kind, [])
	if bag.is_empty():
		bag = [0, 1, 2, 3, 4]
		for i in range(4, 0, -1):
			var j: = rng.randi_range(0, i); var value = bag[i];bag[i] = bag[j];bag[j] = value
		if bag[0] == last_variants.get(kind, -1):
			var value = bag[0];bag[0] = bag[1];bag[1] = value
	var chosen: int = bag.pop_front();bags[kind] = bag;last_variants[kind] = chosen
	return kind + "_" + str(chosen)
func _contact() -> void :
	var actor = app.actor
	if actor.get_instance_id() != owner_id:
		owner_id = actor.get_instance_id();last_contact = "";return
	if actor.species not in ["mosslyn", "wyrmling"] or actor.mounted or actor.state != "idle" or actor.actual_speed < 0.4:
		last_contact = "";return
	if not actor.is_on_floor() or actor.get("ground_airborne") == true: return
	var bound = actor.get("bound_pose")
	var token: String = "b:" + str(bound.count) if bound is Dictionary and not bound.is_empty() else "w:" + str(floori(actor.gait * 2))
	if token == last_contact: return
	var initial: = last_contact.is_empty();last_contact = token
	if initial or step_clock > 0: return
	step_clock = 0.17
	var kind: = surface(app.room, app.current_place(), actor.position)
	step.stream = sound(next_step_key(kind));step.pitch_scale = rng.randf_range(0.985, 1.015)
	var pressure: = clampf(actor.actual_speed / 2.4, 0.75, 1.0)
	step.volume_db = linear_to_db(maxf(0.0001, app.effects_volume * STEP_GAIN * pressure));step.play();steps_played += 1
