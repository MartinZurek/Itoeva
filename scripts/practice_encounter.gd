extends "res://scripts/regional_encounter.gd"
const PracticeBattle = preload("res://scripts/practice_battle.gd")
const Presets = preload("res://scripts/practice_decks.gd")

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
