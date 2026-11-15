extends "res://scripts/practice_encounter.gd"

func _ready() -> void:
	automatic_trigger = false
	encounter_room = app.room
	var preset: Dictionary = Presets.DRILLS[drill]
	var profile: Dictionary = Enemies.data()[preset.enemy].duplicate(true)
	profile.description = preset.lesson
	profile.name = preset.opponent
	drill_title = preset.name
	model = PracticeBattle.new()
	model.start_drill(app.selected, drill, profile, expert, app.active_deck())

func resume() -> bool:
	if not active():
		return false
	suspended = false
	if not is_instance_valid(view):
		_show_controls()
	else:
		view.show()
	_resume_pending_event()
	return true

func _show_controls() -> void:
	app._new_ui()
	view = View.new()
	view.encounter = self
	view.model = model
	app.ui.add_child(view)

func _retreat_now() -> void:
	suspended = true
	model.phase = "done"
	app.show_menu()
