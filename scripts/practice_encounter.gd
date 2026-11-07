extends "res://scripts/regional_encounter.gd"
var practice: = true
func _ready() -> void :
	automatic_trigger = false;encounter_room = app.room
	model = load("res://scripts/practice_battle.gd").new()
	model.configure(app.actor.species)
	var profile: Dictionary = Enemies.data().thorn_moth.duplicate(true)
	profile.name = "Übungspartner";profile.place = encounter_room
	profile.description = "Teste dein Deck. Keine Beute, keine EP und kein Verbrauch aus deinem Vorrat."
	model.configure_enemy("thorn_moth", profile)
	model.prepare(app.save_data.get("card_decks", {}))
	model.begin();model.advance_intro(10)
func persist() -> void : pass
func finish() -> void :
	if suspended or busy > 0 or not model.finish_victory(): return
	_retreat_now()
func _retreat_now() -> void :
	suspended = true;busy = 0;model.phase = "done"
	app._resume();view = null
	app._notice("Decktest beendet · Fortschritt und Vorrat unverändert.")
