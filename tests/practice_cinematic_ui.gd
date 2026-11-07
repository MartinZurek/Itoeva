extends SceneTree

const FX = preload("res://scripts/practice_card_fx.gd")

func _initialize() -> void:
	_run.call_deferred()

func _run() -> void:
	var app = load("res://scenes/full_world.tscn").instantiate()
	root.add_child(app)
	await process_frame
	app.selected = "wyrmling"
	await app._mount_world("CRAFT", Vector3(0, 0.03, 0))
	app._resume()
	app._start_deck_practice(0, false)
	var duel = app.forest_encounter
	var model = duel.model
	var view = duel.view
	var first: int = model._add_minion(2, "thornling",
		{"name": "Glutwelpe", "attack": 4, "health": 18, "shield": 5, "species": "enemy"})
	var second: int = model._add_minion(2, "thornling",
		{"name": "Schildling", "attack": 3, "health": 16, "species": "enemy"})
	model.decks[0].hand = ["firebolt", "K0220", "leaf_guard", "frost_thread"]
	model.decks[0].crystals = 10
	model.decks[0].cap = 10
	view.build_commands()
	view.refresh()
	await create_timer(1.0).timeout

	duel._card_play(0, "u:" + str(first))
	assert(not model.last_event.has("cinematic_card") and is_equal_approx(model.impact_time(), 0.42))
	assert(FX.effect_targets(model.last_event, "damage") == ["u:" + str(first)])
	var labels := FX.impact_labels(model.last_event)
	assert(labels["u:" + str(first)][0][0] == "−5 Schild" and labels["u:" + str(first)][1][0] == "−13 LP")
	assert(model.find_unit(first).hp == 5 and model.find_unit(second).hp == 16)
	await create_timer(1.35).timeout

	duel._card_play(0, "enemy")
	assert(model.last_event.cinematic_card == "K0220")
	assert(view.practice_film.visible and view.skip_film_button.visible)
	assert(model.units(0).size() == 1 and view._visible_board().size() == 2)
	await create_timer(2.24).timeout
	assert(not view.practice_film.visible and model.playback_pending)
	assert(view._visible_board().size() == 2)
	await create_timer(1.36).timeout
	assert(model.units(0).size() == 1 and not model.playback_pending)

	model.special_charges[0] = model.SPECIAL_CHARGES
	var old_hp: int = model.enemy_health
	duel._act("special")
	assert(model.last_event.cinematic_card == "phoenix" and view.practice_film.visible)
	assert(model.enemy_health == old_hp - 30 and model.last_event.before_enemy == old_hp)
	assert(model.find_unit(first).is_empty() and model.find_unit(second).hp == 4)
	assert(model.last_event.summoned_uids.size() == 1)
	assert(FX.effect_targets(model.last_event, "") == ["enemy", "u:" + str(first), "u:" + str(second)])
	assert(FX.impact_labels(model.last_event).enemy[0][0] == "−30 LP")
	await create_timer(3.72).timeout
	assert(not view.practice_film.visible and model.units(0).size() == 2)
	if not OS.has_feature("movie"):
		model._add_minion(0, "emberling")
		model.special_used[0] = false
		model.special_charges[0] = model.SPECIAL_CHARGES
		var previous_hp: int = model.enemy_health
		duel._act("special")
		assert(model.units(0).size() == 3 and model.last_event.summoned_uids.is_empty())
		var touch := InputEventScreenTouch.new()
		touch.pressed = true
		touch.position = view.skip_film_button.get_global_rect().get_center()
		app._input(touch)
		await process_frame
		assert(not view.practice_film.visible and view.event_time >= model.impact_time() - 0.13)
		assert(model.playback_pending and model.enemy_health == previous_hp - 30)
		await create_timer(1.35).timeout
		assert(not model.playback_pending and model.enemy_health == previous_hp - 30)
	app.selected = "starlet"
	await app._mount_world("CRAFT", Vector3(0, 0.03, 0))
	app._resume()
	app._start_deck_practice(1, false)
	duel = app.forest_encounter
	model = duel.model
	model.decks[0].hand = ["starfall"]
	model.decks[0].crystals = 5
	duel.view.build_commands()
	duel.view.refresh()
	await create_timer(0.65).timeout
	duel._card_play(0, "enemy")
	assert(duel.view.practice_film.subject == "starfall")
	assert(FX.impact_labels(model.last_event)["h:0"][0][0] == "+16 Schutz")
	assert(model.party[0].barrier == 16 and model.party[1].barrier == 0)
	await create_timer(3.68).timeout
	assert(not duel.view.practice_film.visible and not model.playback_pending)

	print("Kampfinszenierung · Zieltreffer, Beschwörung, voller Platz und Überspringen geprüft")
	app.queue_free()
	await process_frame
	quit()
