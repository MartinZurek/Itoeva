extends Node

const Selection = preload("res://scripts/music_selection.gd")
var selection = Selection.new()
const WorldMusic = preload("res://scripts/world_music.gd")
var world = WorldMusic.new()
var ensemble_clock := 0.0
var sampled_hour := 12.0
var encounter_id: = 0
var arena_fade: = 1.0
var arena_next: = ""
var app: Node
var arena: AudioStreamPlayer
var battle_theme: AudioStreamOggVorbis
var victory_theme: AudioStreamOggVorbis
var music_mode: String = "battle"
var voices: Array[AudioStreamPlayer] = []
var sounds: Dictionary = {}
var blend: = 0.0
var loudness: = 0.0
var scene_gain: = 1.0
var duck: = 0.0
var event: Dictionary = {}
var event_clock: = -1.0
var impact_played: = false
var last_phase: = ""
var last_cursor: = -1
var played: Array[String] = []
var cooldowns: Dictionary = {}
const KEYS: = ["select", "crystal", "summon", "attack", "impact", "nature", "fire", "water", "wind", "light", "moon", "shield", "heal", "turn", "victory", "defeat", "error"]

func _ready() -> void :
	for key in KEYS: sounds[key] = load("res://assets/audio/" + key + ".wav")
	arena = AudioStreamPlayer.new()
	add_child(arena)
	victory_theme = load("res://assets/music/victory-theme.ogg").duplicate()
	victory_theme.loop = true
	if app.has_method("effective_world_hour"):
		var secondary := AudioStreamPlayer.new()
		add_child(secondary)
		world.configure(app.music, secondary)
	arena.finished.connect( func(): _change_arena(music_mode))
	for i in 4:
		var voice: = AudioStreamPlayer.new();add_child(voice);voices.append(voice)

func cue(key: String, pan: float = 0.0, gain: float = 1.0) -> void :
	if not app.effects_on or not sounds.has(key): return
	var now: = Time.get_ticks_msec()
	if now - int(cooldowns.get(key, -1000)) < (120 if key in ["select", "crystal"] else 65): return
	cooldowns[key] = now
	var voice: AudioStreamPlayer = voices[0]
	for candidate in voices:
		if not candidate.playing: voice = candidate;break
		if int(candidate.get_meta("started", 0)) < int(voice.get_meta("started", 0)): voice = candidate
	voice.stop();voice.stream = sounds[key]
	voice.set_meta("mix_gain", clampf(gain, 0, 1) * cue_gain(key))
	voice.set_meta("started", now)
	voice.pitch_scale = 1.0 + clampf(pan, -1, 1) * 0.025
	voice.play();_balance_voices();played.append(key)
	if played.size() > 64: played.pop_front()


static func cue_gain(key: String) -> float:
	return 0.32 if key in ["select", "crystal", "turn", "error"] else 0.65

func _balance_voices() -> void :
	var count: = 0
	for voice in voices:
		if not app.effects_on: voice.stop()
		if voice.playing: count += 1
	for voice in voices:
		voice.volume_db = linear_to_db(maxf(0.0001, app.effects_volume * float(voice.get_meta("mix_gain", 0.0)) / sqrt(maxf(1, count))))

func begin_event(next: Dictionary, elapsed: float = 0.0) -> void:
	event = next.duplicate(true)
	event_clock = elapsed
	impact_played = elapsed >= float(event.get("impact", 0.7))
	if elapsed > 0.0:
		return
	var visual: String = event.get("visual", "")
	if visual == "ecology":
		if event.get("ecology_action", "") in ["warning", "prepare"]: cue("moon" if event.get("ecology_kind", "") == "boss" else "crystal", 0, 0.4)
	elif visual == "card_summon": cue("summon")
	elif visual == "card_attack": cue("attack")
	elif visual != "pass": cue("select")
	if event.has("before_crystals"):
		var source: = int(event.get("source", 0))
		if source >= 0 and source < 2 and int(event.before_crystals[source]) > int(app.forest_encounter.model.decks[source].crystals): cue("crystal")
	duck = 0.35

func _process(delta: float) -> void :
	if not is_instance_valid(app): return
	_balance_voices()
	var encounter = app.forest_encounter
	var fight: bool = is_instance_valid(encounter) and encounter.active() and not encounter.suspended
	if not fight:
		event_clock = -1
		for voice in voices: voice.stop()
	else:
		var model = encounter.model
		var desired: String = "victory" if model.phase == "victory" and not model.playback_pending else "battle"
		if desired != music_mode or encounter_id != encounter.get_instance_id() or arena.stream == null:
			encounter_id = encounter.get_instance_id()
			_change_arena(desired)
		if event_clock >= 0:
			event_clock += delta
			if not impact_played and event_clock >= float(event.get("impact", 0.7)):
				impact_played = true
				var key: = impact_key(event)
				if key != "": cue(key, float(event.get("source", 0)) - 0.5, 0.75 if event.get("visual", "") == "ecology" else 1.0);duck = 0.24
			if event_clock > float(event.get("duration", 1.45)): event_clock = -1
		if last_phase != model.phase:
			if model.phase in ["victory", "defeat"]:
				if not model.playback_pending:
					cue(model.phase);last_phase = model.phase
			else: last_phase = model.phase
		if model.phase == "battle" and not model.playback_pending and last_cursor != model.cursor:
			if model.cursor < 2: cue("turn")
			last_cursor = model.cursor
	_rotate_arena(delta, fight)
	duck = maxf(0, duck - delta)
	blend = move_toward(blend, 1.0 if fight else 0.0, delta / 1.2)
	var allowed: bool = app.battle_music_on if fight else app.music_on and (not app.paused or app.switching)
	loudness = move_toward(loudness, 1.0 if allowed else 0.0, delta * 4)
	var quiet_cafe: bool = app.room == "CAFE" and app.room_activities.active.get("kind") == "pause"
	scene_gain = move_toward(scene_gain, 0.7 if quiet_cafe else 1.0, delta * 0.8)
	var gain: float = app.music_volume * loudness * (0.62 if duck > 0 else 1.0)
	if not world.players.is_empty(): _mix_world(delta, gain, fight)
	_mix(arena, gain * blend * 0.6 * arena_fade)

func _mix_world(delta: float, gain: float, fight: bool) -> void:
	var place: String = app.music_place()
	ensemble_clock -= delta
	if ensemble_clock <= 0.0:
		sampled_hour = app.effective_world_hour()
		ensemble_clock = 0.25
	var scheduling: bool = app.music_on and not app.paused and not app.switching and not fight
	var role: String = Selection.world_role(place) if not place.is_empty() else ""
	var world_gain := gain * (1.0 - blend) * scene_gain if app.music_on else 0.0
	world.update(delta, role, app.Clock.phase_at(sampled_hour), sampled_hour, world_gain, scheduling)

func _mix(player: AudioStreamPlayer, gain: float) -> void :
	if player.stream == null: return
	player.volume_db = linear_to_db(maxf(gain, 0.0001))
	if gain > 0.0005:
		if not player.playing: player.play()
		player.stream_paused = false
	else: player.stream_paused = true


func _change_arena(mode: String) -> void :
	music_mode = mode
	arena.stop();arena_next = "";arena_fade = 0.0
	if mode == "victory": arena.stream = victory_theme
	else:
		battle_theme = load(selection.pick("battle")).duplicate()
		battle_theme.loop = false
		arena.stream = battle_theme

func _rotate_arena(delta: float, fight: bool) -> void:
	if fight and app.battle_music_on:
		if music_mode == "battle" and arena.playing and arena.stream.get_length() - arena.get_playback_position() < 0.8 and arena_next == "":
			arena_next = selection.pick("battle")
		arena_fade = move_toward(arena_fade, 0.0 if arena_next != "" else 1.0, delta / 0.8)
		if arena_next != "" and arena_fade <= 0.0:
			arena.stop()
			battle_theme = load(arena_next).duplicate();battle_theme.loop = false
			arena.stream = battle_theme;arena_next = ""


static func impact_key(next: Dictionary) -> String:
	var visual: String = next.get("visual", "")
	var element: String = next.get("element", "physical")
	var key: String = element if element in ["nature", "fire", "water", "wind", "light", "moon", "heal"] else "impact"
	if visual == "ecology":
		var actual: = false
		for effect in next.get("effects", []):
			if int(effect.get("amount", 0)) > 0 and effect.kind in ["damage", "heal", "shield", "barrier_hit", "barrier_break"]: actual = true
		if not actual: return ""
	for effect in next.get("effects", []):
		if effect.kind in ["barrier_hit", "barrier_break", "barrier", "shield"]: return "shield"
		if effect.kind == "heal": key = "heal"
	return key
