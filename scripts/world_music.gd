extends RefCounted

const Selection = preload("res://scripts/music_selection.gd")
const FADE := 2.35
const REGION_HOLD := 0.65
const PHASE_HOLD := 1.2
const PHASE_TRACK_AGE := 14.0
const BUS_NAMES := ["Weltmusik A", "Weltmusik B"]

var selection = Selection.new()
var players: Array[AudioStreamPlayer] = []
var corrections: Array[AudioEffectPitchShift] = []
var tracks := ["", ""]
var track_gains := [1.0, 1.0]
var active := 0
var fade := 1.0
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
		_select_next()
		fade = minf(1.0, fade + delta / FADE)
	tempo = move_toward(tempo, Selection.tempo_for(hour), delta * 0.027)
	for index in players.size():
		var effect := corrections[index]
		if effect != null:
			players[index].pitch_scale = tempo
			effect.pitch_scale = 1.0 / tempo
		var blend := sin(fade * PI * 0.5) if index == active else cos(fade * PI * 0.5)
		var value: float = gain * blend * track_gains[index]
		_apply(players[index], value)
	if fade >= 1.0 and players[1 - active].stream != null:
		players[1 - active].stop()
		players[1 - active].stream = null
		tracks[1 - active] = ""

func _select_next() -> void:
	if fade < 1.0 or pending_region.is_empty(): return
	if pending_region != region or tracks[active].is_empty():
		if tracks[active].is_empty() or region_clock >= REGION_HOLD:
			region = pending_region
			phase = pending_phase
			_begin(selection.pick_world(region, phase, tracks[active], true))
		return
	var phase_changed := pending_phase != phase and phase_clock >= PHASE_HOLD and track_age >= PHASE_TRACK_AGE
	var current := players[active]
	var near_end := current.playing and current.stream.get_length() - current.get_playback_position() < (FADE + 0.25) * tempo
	if phase_changed or near_end:
		phase = pending_phase
		_begin(selection.pick_world(region, phase, tracks[active], false))

func _begin(path: String) -> void:
	if path.is_empty() or path == tracks[active]: return
	var source := load(path) as AudioStreamOggVorbis
	if source == null: return
	var was_playing: bool = not tracks[active].is_empty()
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
