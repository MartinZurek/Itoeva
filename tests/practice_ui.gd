extends SceneTree

func _initialize() -> void:
	_run.call_deferred()

func _run() -> void:
	var app = load("res://scenes/full_world.tscn").instantiate()
	root.add_child(app)
	await process_frame
	app.selected = "mosslyn"
	await app._mount_world("CRAFT", Vector3(0, 0.03, 0))
	app._resume()
	var original: Dictionary = app.save_data.duplicate(true)
	app._show_practice_station()
	assert(_find_button(app, "Funkenlauf · gegen Aschenläufer") != null)
	assert(_find_button(app, "Wurzelwacht · gegen Schildwacht") != null)
	_find_button(app, "Meisterprüfung").pressed.emit()
	_find_button(app, "Echo in der Esse · gegen Funkenmeister").pressed.emit()
	await process_frame
	var duel = app.forest_encounter
	assert(duel.model.phase == "battle" and duel.model.drill == 2 and duel.model.expert)
	assert(duel.view.party_plates[0].visible and not duel.view.party_plates[1].visible)
	assert(duel.view.intent.text.contains("Schmiedeling"))
	assert(_find_button(app, "Zug beenden") != null)
	_find_button(app, "Zug beenden").pressed.emit()
	for frame in 240:
		await process_frame
		if duel.model.round == 2:
			break
	assert(duel.model.round == 2 and duel.model.cursor == 0)
	assert(duel.model.units(2).size() == 1)
	assert(duel.model.units(2)[0].attack == 6)
	duel._retreat_now()
	assert(_find_button(app, "Wurzelwacht · gegen Schildwacht") != null)
	assert(_find_button(app, "Meisterprüfung").disabled)
	assert(app.save_data == original)
	_find_button(app, "Zurück zur Welt").pressed.emit()
	assert(not app.paused and app.room == "CRAFT")
	print("Kartenschmiede · Deckwahl, Solo-Ansicht, Gegnerzug, Rückkehr und Spielstand geprüft")
	app.queue_free()
	await process_frame
	quit()

func _find_button(app, title: String) -> Button:
	for button in app.menu_buttons:
		if is_instance_valid(button) and button.text == title:
			return button
	return null
