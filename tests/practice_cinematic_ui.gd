extends SceneTree

const FX = preload("res://scripts/practice_card_fx.gd")
const Timing = preload("res://scripts/practice_film_timing.gd")

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
	var film = view.practice_film
	assert(is_equal_approx(model.impact_time(), Timing.IMPACT))
	assert(is_equal_approx(model.animation_duration(), Timing.DURATION))
	assert(film.place_label.text == "DIE ALTE ESSE")
	var crouched_y: float = film.actor.position.y
	var crouched_x: float = film.actor.position.x
	await create_timer(5.80).timeout
	assert(film.rendered_frame == 1 and film.actor.position.x > crouched_x)
	assert(film.shot == 2 and film.chapter.text == "Der erste Schritt")
	assert(is_equal_approx(film.actor.position.y, crouched_y) and film.contact.visible)
	assert(film.paw_shadows[1].visible and not film.paw_shadows[2].visible)
	await create_timer(1.90).timeout
	assert(film.rendered_frame == 2 and is_equal_approx(film.actor.position.y, crouched_y))
	assert(film.paw_shadows[2].visible)
	await create_timer(2.42).timeout
	assert(not view.practice_film.visible and model.playback_pending)
	assert(view._visible_board().size() == 2)
	await create_timer(1.24).timeout
	assert(model.units(0).size() == 1 and not model.playback_pending)

	model.special_charges[0] = model.SPECIAL_CHARGES
	var old_hp: int = model.enemy_health
	duel._act("special")
	assert(model.last_event.cinematic_card == "phoenix" and view.practice_film.visible)
	assert(film.place_label.text == "ASCHESCHLUCHT" and not film.built_locations.forge.visible)
	assert(not view.practice_film.paw_shadows[0].visible)
	assert(model.enemy_health == old_hp - 30 and model.last_event.before_enemy == old_hp)
	assert(model.find_unit(first).is_empty() and model.find_unit(second).hp == 4)
	assert(model.last_event.summoned_uids.size() == 1)
	assert(FX.effect_targets(model.last_event, "") == ["enemy", "u:" + str(first), "u:" + str(second)])
	assert(FX.impact_labels(model.last_event).enemy[0][0] == "−30 LP")
	await create_timer(5.60).timeout
	assert(film.actor.position.y > 1.0 and film.shot == 2)
	await create_timer(model.animation_duration() + 0.22 - 5.60).timeout
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
	assert(duel.view.practice_film.place_label.text == "STERNWARTE AM ABGRUND")
	assert(FX.impact_labels(model.last_event)["h:0"][0][0] == "+16 Schutz")
	assert(model.party[0].barrier == 16 and model.party[1].barrier == 0)
	await create_timer(6.25).timeout
	assert(duel.view.practice_film.shield_omen.visible and duel.view.practice_film.shot == 2)
	await create_timer(model.animation_duration() + 0.22 - 6.25).timeout
	assert(not duel.view.practice_film.visible and not model.playback_pending)
	if not OS.has_feature("movie"):
		film = duel.view.practice_film
		film.start("K0125")
		assert(film.chapter.text == "Eine Spur im Stein" and film.built_locations.forge.visible)
		film.tick(9.8)
		assert(film.visible and film.chapter.text == "An deiner Seite")
		film.start("K0098")
		assert(film.chapter.text == "Nachtwache" and film.built_locations.starfall.visible)
		film.tick(10.0)
		assert(not film.visible)

	print("Kampfinszenierung · Zieltreffer, Beschwörung, voller Platz und Überspringen geprüft")
	app.queue_free()
	await process_frame
	quit()
