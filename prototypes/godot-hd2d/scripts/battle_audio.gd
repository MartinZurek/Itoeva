extends Node
# Ein Pool begrenzt Effekte; die Weltpause ist im Kartenkampf keine Audiopause.
var app: Node
var arena: AudioStreamPlayer
var battle_theme: AudioStreamOggVorbis
var victory_theme: AudioStreamOggVorbis
var music_mode: String="battle"
var voices: Array[AudioStreamPlayer] = []
var sounds: Dictionary = {}
var blend := 0.0
var loudness := 0.0
var duck := 0.0
var event: Dictionary = {}
var event_clock := -1.0
var impact_played := false
var last_phase := ""
var last_cursor := -1
var played: Array[String] = []
var cooldowns: Dictionary = {}
const KEYS := ["select","crystal","summon","attack","impact","nature","fire","water","wind","light","moon","shield","heal","turn","victory","defeat","error"]

func _ready() -> void:
	for key in KEYS: sounds[key]=load("res://assets/audio/"+key+".wav")
	arena=AudioStreamPlayer.new()
	var stream: AudioStreamOggVorbis=load("res://assets/music/battle-arena.ogg").duplicate()
	stream.loop=true;battle_theme=stream;arena.stream=stream;add_child(arena)
	victory_theme=load("res://assets/music/victory-theme.ogg").duplicate()
	victory_theme.loop=true
	for i in 6:
		var voice:=AudioStreamPlayer.new();add_child(voice);voices.append(voice)
	app.music.stream=app.music.stream.duplicate()
	app.music.stream.loop=true

func cue(key: String, pan: float=0.0, gain: float=1.0) -> void:
	if not app.effects_on or not sounds.has(key):return
	var now:=Time.get_ticks_msec()
	if now-int(cooldowns.get(key,-1000))<65:return
	cooldowns[key]=now
	var voice: AudioStreamPlayer=voices[0]
	for candidate in voices:
		if not candidate.playing:voice=candidate;break
	voice.stop();voice.stream=sounds[key]
	voice.volume_db=linear_to_db(maxf(.001,app.effects_volume*clampf(gain,0,1)))
	voice.pitch_scale=1.0+clampf(pan,-1,1)*.025
	voice.play();played.append(key)
	if played.size()>64:played.pop_front()

func begin_event(next: Dictionary) -> void:
	event=next.duplicate(true);event_clock=0;impact_played=false
	var visual: String=event.get("visual","")
	if visual=="ecology":
		if event.get("ecology_action","") in ["warning","prepare"]:cue("moon" if event.get("ecology_kind","")=="boss" else "crystal",0,.4)
	elif visual=="card_summon":cue("summon")
	elif visual=="card_attack":cue("attack")
	elif visual!="pass":cue("select")
	if event.has("before_crystals"):
		var source:=int(event.get("source",0))
		if source>=0 and source<2 and int(event.before_crystals[source])>int(app.forest_encounter.model.decks[source].crystals):cue("crystal")
	duck=.35

func _process(delta: float) -> void:
	if not is_instance_valid(app):return
	var encounter=app.forest_encounter
	var fight: bool=is_instance_valid(encounter) and encounter.active() and not encounter.suspended
	if not fight:
		event_clock=-1
		for voice in voices:voice.stop()
	else:
		var model=encounter.model
		var desired: String="victory" if model.phase=="victory" and not model.playback_pending else "battle"
		arena.pitch_scale=move_toward(arena.pitch_scale,.96 if model.enemy_profile.get("kind","")=="boss" and desired=="battle" else 1.0,delta*.08)
		if desired!=music_mode:
			music_mode=desired;arena.stop()
			arena.stream=victory_theme if desired=="victory" else battle_theme
		if event_clock>=0:
			event_clock+=delta
			if not impact_played and event_clock>=float(event.get("impact",.7)):
				impact_played=true
				var key:=impact_key(event)
				if key!="":cue(key,float(event.get("source",0))-.5,.75 if event.get("visual","")=="ecology" else 1.0);duck=.24
			if event_clock>float(event.get("duration",1.45)):event_clock=-1
		if last_phase!=model.phase:
			if model.phase in ["victory","defeat"]:
				if not model.playback_pending:
					cue(model.phase);last_phase=model.phase
			else:last_phase=model.phase
		if model.phase=="battle" and not model.playback_pending and last_cursor!=model.cursor:
			if model.cursor<2:cue("turn")
			last_cursor=model.cursor
	duck=maxf(0,duck-delta)
	blend=move_toward(blend,1.0 if fight else 0.0,delta/1.2)
	var allowed: bool=app.battle_music_on if fight else app.music_on and not app.paused
	loudness=move_toward(loudness,1.0 if allowed else 0.0,delta*4)
	var gain: float=app.music_volume*loudness*(.62 if duck>0 else 1.0)
	_mix(app.music,gain*(1.0-blend))
	_mix(arena,gain*blend*.74)

func _mix(player: AudioStreamPlayer, gain: float) -> void:
	player.volume_db=linear_to_db(maxf(gain,.0001))
	if gain>.0005:
		if not player.playing:player.play()
		player.stream_paused=false
	else:player.stream_paused=true

# Ein Vorbereitungszug besitzt keinen erfundenen Trefferklang.
static func impact_key(next: Dictionary) -> String:
	var visual: String=next.get("visual","")
	var element: String=next.get("element","physical")
	var key: String=element if element in ["nature","fire","water","wind","light","moon","heal"] else "impact"
	if visual=="ecology":
		var actual:=false
		for effect in next.get("effects",[]):
			if int(effect.get("amount",0))>0 and effect.kind in ["damage","heal","shield","barrier_hit","barrier_break"]:actual=true
		if not actual:return ""
	for effect in next.get("effects",[]):
		if effect.kind in ["barrier_hit","barrier_break","barrier","shield"]:return "shield"
		if effect.kind=="heal":key="heal"
	return key
