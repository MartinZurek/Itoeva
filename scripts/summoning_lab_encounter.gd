extends "res://scripts/duel_lab_encounter.gd"

const PreviewBattle = preload("res://scripts/summoning_lab_battle.gd")
const PreviewView = preload("res://scripts/summoning_lab_view.gd")
var subject := ""

func _ready() -> void:
	automatic_trigger = false
	encounter_room = app.room
	drill_title = "Beschwörungskino"
	var profile: Dictionary = Enemies.data()[Presets.DRILLS[0].enemy].duplicate(true)
	var species: String = Battle.Cards.get_card(subject).get("class", "all")
	if species not in Battle.SPECIES:
		species = "mosslyn"
	model = PreviewBattle.new()
	model.prepare_preview(subject, species, profile)

func resume() -> bool:
	if not super.resume():
		return false
	if not model.playback_pending and model.turn == 0:
		if model.demonstrate():
			_play_event()
		else:
			push_error("Vorschau für %s konnte nicht gestartet werden" % subject)
	return true

func _show_controls() -> void:
	app._new_ui()
	view = PreviewView.new()
	view.encounter = self
	view.model = model
	app.ui.add_child(view)

func _process(delta: float) -> void:
	# Playback only. The board remains visible after the demo action.
	if suspended or not is_instance_valid(view) or not model.playback_pending:
		return
	busy = maxf(0.0, busy - delta)
	if busy == 0.0:
		model.acknowledge_animation()
		view.refresh()
