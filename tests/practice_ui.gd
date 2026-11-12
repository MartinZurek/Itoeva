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
	var turn_before: int = duel.model.turn
	var random_before: int = duel.model.rng.state
	var simulations_before: int = duel.view.focus.simulations
	_tap(app, duel.view.intent_button)
	assert(is_instance_valid(duel.view.intent_popup))
	var body: RichTextLabel = duel.view.intent_popup.get_child(2)
	assert(body.text.contains("Schmiedeling erscheint (6 Angriff · 9 LP)"))
	assert(duel.view.focus.simulations == simulations_before)
	_tap(app, _find_button(app, "Zug beenden"))
	assert(duel.model.turn == turn_before and duel.model.rng.state == random_before)
	assert(duel.model.units(2).is_empty())
	_tap(app, duel.view.intent_close_button)
	assert(not is_instance_valid(duel.view.intent_popup))
	await create_timer(0.22).timeout
	var first_card: Button
	for button in duel.view.buttons:
		if button.get_meta("battle_action", "") == "card" and button.get_meta("hand_index", -1) == 0:
			first_card = button
			break
	assert(is_instance_valid(first_card))
	var crystals: int = duel.model.decks[0].crystals
	duel.model.decks[0].crystals = 0
	duel.view.refresh()
	assert(first_card.disabled)
	var touch := InputEventScreenTouch.new()
	touch.position = first_card.get_global_rect().get_center()
	touch.pressed = true
	app._input(touch)
	touch.pressed = false
	app._input(touch)
	assert(duel.view.status.text.contains("Kristall") and duel.model.decks[0].hand.size() == 4)
	duel.model.decks[0].crystals = crystals
	duel.view.refresh()
	assert(_find_button(app, "Zug beenden") != null)
	_find_button(app, "Zug beenden").pressed.emit()
	for frame in 240:
		await process_frame
		if duel.model.round == 2:
			break
	assert(duel.model.round == 2 and duel.model.cursor == 0)
	assert(duel.model.units(2).size() == 1)
	assert(duel.model.units(2)[0].attack == 6)
	_tap(app, duel.view.intent_button)
	assert(is_instance_valid(duel.view.intent_popup))
	body = duel.view.intent_popup.get_child(2)
	assert(body.text.contains("−") and body.text.contains("Schutz aufbaust"))
	_tap(app, duel.view.intent_close_button)
	await create_timer(0.22).timeout
	var guard: Dictionary = duel.model.units(2)[0]
	guard.taunt = true
	var uid: int = duel.model._add_minion(0, "K0003")
	var helper: Dictionary = duel.model.find_unit(uid)
	helper.shield = 2
	helper.ready = true
	var view = duel.view
	view.build_commands()
	view.refresh()
	_tap_action(app, view, "unit", uid)
	assert(view.selected_unit == uid and view.unit_info_stats.text.contains("2 Schild"))
	assert(view.unit_info_abilities.text.contains("Zins"))
	await create_timer(0.22).timeout
	_tap_action(app, view, "enemy_target")
	assert(view.hint_message.contains(guard.name) and not duel.model.playback_pending)
	_tap_action(app, view, "unit", uid)
	assert(view.selected_unit == -1 and view.unit_info.visible)
	helper.ready = false
	helper.frozen = 1
	_tap_action(app, view, "unit", uid)
	assert(view.selected_unit == -1 and view.unit_info_state.text.contains("Frost"))
	_tap_action(app, view, "unit", guard.uid)
	assert(view.unit_info_name.text.contains(guard.name) and view.unit_info_state.text.contains("Schutzhelfer"))
	duel._retreat_now()
	assert(_find_button(app, "Wurzelwacht · gegen Schildwacht") != null)
	assert(_find_button(app, "Meisterprüfung").disabled)
	assert(app.save_data == original)
	_find_button(app, "Zurück zur Welt").pressed.emit()
	assert(not app.paused and app.room == "CRAFT")
	print("Kartenschmiede · Deckwahl, Gegnerabsicht, Helferstatus, Zielschutz und Rückkehr geprüft")
	app.queue_free()
	await process_frame
	quit()

func _find_button(app, title: String) -> Button:
	for button in app.menu_buttons:
		if is_instance_valid(button) and button.text == title:
			return button
	return null

func _tap_action(app, view, action: String, uid: int = -1) -> void:
	for button in view.buttons:
		if button.get_meta("battle_action", "") == action and (uid < 0 or button.get_meta("unit_uid", -1) == uid):
			_tap(app, button)
			return
	assert(false, "Kampfaktion fehlt: " + action)

func _tap(app, button: Button) -> void:
	assert(is_instance_valid(button))
	var touch := InputEventScreenTouch.new()
	touch.pressed = true
	touch.position = button.get_global_rect().get_center()
	app._input(touch)
