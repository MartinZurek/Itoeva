extends "res://scripts/regional_encounter.gd"
const PracticeBattle = preload("res://scripts/practice_battle.gd")
const Presets = preload("res://scripts/practice_decks.gd")
const FilmTiming = preload("res://scripts/practice_film_timing.gd")

var practice := true
var drill := 0
var expert := false
var drill_title := ""

func _ready() -> void:
	automatic_trigger = false
	encounter_room = app.room
	var preset: Dictionary = Presets.DRILLS[drill]
	drill_title = preset.name
	var profile: Dictionary = Enemies.data()[preset.enemy].duplicate(true)
	profile.name = preset.opponent
	profile.place = encounter_room
	profile.description = preset.lesson
	model = PracticeBattle.new()
	model.start_drill(app.actor.species, drill, profile, expert)

func persist() -> void:
	pass

func _play_event(elapsed: float = 0.0) -> void:
	var event: Dictionary = model.last_event
	var film: String = event.get("cinematic_card", "")
	if not film.is_empty() and not event.has("cinematic_short"):
		var shorten: bool = app.short_repeat_cinematics and app.has_seen_practice_film(film)
		event["cinematic_short"] = shorten
		if shorten:
			event["impact"] = FilmTiming.SHORT_IMPACT
			event["duration"] = FilmTiming.SHORT_DURATION
	super._play_event(elapsed)

func _process(delta: float) -> void:
	var previous_busy: float = busy
	super._process(delta)
	if busy >= previous_busy:
		return
	var event: Dictionary = model.last_event
	var film: String = event.get("cinematic_card", "")
	if film.is_empty() or event.get("cinematic_short", false):
		return
	var impact_remaining: float = model.animation_duration() - model.impact_time()
	if previous_busy > impact_remaining and busy <= impact_remaining:
		app.remember_practice_film(film)

func _retry() -> void:
	if suspended or not model.repeat_drill():
		return
	busy = 0.0
	enemy_delay = 0.55
	_show_controls()

func finish() -> void:
	if suspended or busy > 0 or not model.finish_victory():
		return
	_retreat_now()

func _retreat_now() -> void:
	suspended = true
	busy = 0
	model.phase = "done"
	view = null
	app._show_practice_station(expert)
