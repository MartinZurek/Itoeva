extends RefCounted

const Selection = preload("res://scripts/music_selection.gd")
const NATURAL_FADE := 2.8
const GROOVE_FADE := 3.2
const REGION_HOLD := 0.95
const MAX_CUE_WAIT := 3.8
const PULSED := ["arcade-01", "ballgame-01", "city-01", "city-03", "shop-01", "sport-01"]
const PHASE_HOLD := 1.2
const PHASE_TRACK_AGE := 14.0
const BUS_NAMES := ["Weltmusik A", "Weltmusik B"]

var selection = Selection.new()
var players: Array[AudioStreamPlayer] = []
var corrections: Array[AudioEffectPitchShift] = []
var tracks: Array[String] = ["", ""]
var track_gains := [1.0, 1.0]
var active := 0
var fade := 1.0
var fade_seconds := NATURAL_FADE
var separated_fade := false
var queued := ""
var cue_at := -1.0
var cue_wait := 0.0
static var exit_points: Dictionary = {}
var tempo := 1.0
var region := ""
var pending_region := ""
var region_clock := 0.0
var phase := ""
var pending_phase := ""
var phase_clock := 0.0
var track_age := 0.0

func configure(primary: AudioStreamPlayer, secondary: AudioStreamPlayer) -> void:
	players = [primary, secondary]
	for index in players.size():
		var bus := AudioServer.get_bus_index(BUS_NAMES[index])
		if bus < 0:
			corrections.append(null)
			continue
		players[index].bus = BUS_NAMES[index]
		corrections.append(AudioServer.get_bus_effect(bus, 0) as AudioEffectPitchShift)

func update(delta: float, desired_region: String, desired_phase: String, hour: float, gain: float, scheduling: bool) -> void:
	if desired_region != pending_region:
		pending_region = desired_region
		region_clock = 0.0
	if desired_phase != pending_phase:
		pending_phase = desired_phase
		phase_clock = 0.0
	if scheduling:
		region_clock += delta
		phase_clock += delta
		track_age += delta
		_select_next(delta)
	if scheduling or gain > 0.0005:
		fade = minf(1.0, fade + delta / fade_seconds)
	tempo = move_toward(tempo, Selection.tempo_for(hour), delta * 0.027)
	var levels := blend_levels(fade, separated_fade)
	for index in players.size():
		var effect := corrections[index]
		if effect != null:
			players[index].pitch_scale = tempo
			effect.pitch_scale = 1.0 / tempo
		var blend := levels.y if index == active else levels.x
		var value: float = gain * blend * track_gains[index]
		_apply(players[index], value)
	if fade >= 1.0 and players[1 - active].stream != null:
		players[1 - active].stop()
		players[1 - active].stream = null
		tracks[1 - active] = ""

func _select_next(delta: float) -> void:
	if fade < 1.0: return
	if tracks[active].is_empty():
		if pending_region.is_empty(): return
		region = pending_region
		phase = pending_phase
		_begin(selection.pick_world(region, phase, "", true))
		return

	var player := players[active]
	var position := player.get_playback_position()
	var length := player.stream.get_length()
	var near_end := player.playing and length - position < GROOVE_FADE * tempo + 0.35
	var region_changed := not pending_region.is_empty() and pending_region != region
	var phase_changed := not pending_region.is_empty() and pending_phase != phase and phase_clock >= PHASE_HOLD and track_age >= PHASE_TRACK_AGE
	var next_region := region
	var next_phase := phase
	var reason := ""
	if region_changed and region_clock >= REGION_HOLD:
		next_region = pending_region
		next_phase = pending_phase
		reason = "region"
	elif near_end:
		# Eine bereits bestätigte Ankunft hat Vorrang vor der normalen Trackrotation.
		reason = "end"
	elif phase_changed and not region_changed:
		next_phase = pending_phase
		reason = "phase"

	if reason.is_empty():
		queued = ""
		return
	var key := "%s:%s:%s" % [reason, next_region, next_phase]
	var outgoing := tracks[active].get_file().get_basename()
	if queued != key:
		queued = key
		var cutoff := maxf(position, length - GROOVE_FADE * tempo - 0.1)
		var deadline := minf(position + MAX_CUE_WAIT * tempo, cutoff)
		if reason == "end":
			cue_at = cutoff
		else:
			cue_at = next_exit(outgoing, position, deadline)
		cue_wait = (cue_at - position) / maxf(tempo, 0.1) + 0.12
	else:
		cue_wait -= delta
	if position < cue_at and cue_wait > 0.0: return

	region = next_region
	phase = next_phase
	queued = ""
	if reason == "end" and not pending_phase.is_empty(): phase = pending_phase
	_begin(selection.pick_world(region, phase, tracks[active], reason == "region"))

static func next_exit(track: String, position: float, deadline: float) -> float:
	if exit_points.is_empty():
		exit_points = JSON.parse_string(FileAccess.get_file_as_string("res://data/music_exit_points.json"))
	var points: Array = exit_points.get(track, [])
	var index := points.bsearch(position + 0.095)
	if index < points.size() and float(points[index]) <= deadline: return float(points[index])
	return minf(position + 0.7, deadline)

static func blend_levels(progress: float, separated: bool) -> Vector2:
	# Bei markanten Grooves bekommt das ausklingende Stück etwas Vorsprung.
	var outgoing := smoothstep(0.0, 0.78, progress) if separated else progress
	var incoming := smoothstep(0.20, 1.0, progress) if separated else progress
	return Vector2(cos(outgoing * PI * 0.5), sin(incoming * PI * 0.5))

func _begin(path: String) -> void:
	if path.is_empty() or path == tracks[active]: return
	var source := load(path) as AudioStreamOggVorbis
	if source == null: return
	var was_playing: bool = not tracks[active].is_empty()
	separated_fade = path.get_file().get_basename() in PULSED or tracks[active].get_file().get_basename() in PULSED
	fade_seconds = GROOVE_FADE if separated_fade else NATURAL_FADE
	if was_playing: active = 1 - active
	var stream := source.duplicate() as AudioStreamOggVorbis
	stream.loop = true
	players[active].stream = stream
	tracks[active] = path
	track_gains[active] = 1.0
	for row: Dictionary in Selection.data().tracks.values():
		if row.asset != path: continue
		track_gains[active] = db_to_linear(float(row.get("world_gain_db", 0.0)))
		break
	track_age = 0.0
	fade = 0.0 if was_playing else 1.0

func _apply(player: AudioStreamPlayer, gain: float) -> void:
	if player.stream == null: return
	player.volume_db = linear_to_db(maxf(gain, 0.0001))
	if gain > 0.0005:
		if not player.playing: player.play()
		player.stream_paused = false
	else:
		player.stream_paused = true
