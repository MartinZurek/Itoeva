extends SceneTree

const Model = preload("res://scripts/summoning_lab_battle.gd")
const Enemies = preload("res://scripts/regional_enemies.gd")
const Cards = preload("res://scripts/card_catalog.gd")

func _initialize() -> void:
	run.call_deferred()

func run() -> void:
	var viewer = load("res://scenes/summoning_lab.tscn").instantiate()
	root.add_child(viewer)
	await process_frame
	var profile: Dictionary = Enemies.data().ash_hound
	var problems: Array[String] = []
	var full := 0
	for card in viewer.entries:
		var model := Model.new()
		var species: String = card.get("class", "all")
		model.prepare_preview(card.id, species if species in Model.SPECIES else "mosslyn", profile)
		if not model.demonstrate():
			problems.append(card.id)
		elif model.last_event.get("cinematic_card", "") == card.id:
			full += 1
		elif model.last_event.get("preview_card", "") != card.id or model.impact_time() <= Model.REVEAL_TIME:
			problems.append(card.id)
	if not problems.is_empty() or full != Model.FULL_SCENES.size():
		push_error("Vorschau fehlgeschlagen: %s · Eigene Szenen: %s" % [problems, full])
		quit(1)
		return
	viewer._set_filter("Eigene Szenen")
	assert(viewer.selection.size() == 5)
	viewer._play_index(0)
	await process_frame
	var duel: Node = viewer.forest_encounter
	assert(duel.model.playback_pending and is_instance_valid(duel.view.practice_film))
	duel.view._skip_practice_film()
	for frame in 160:
		await process_frame
		if not duel.model.playback_pending:
			break
	assert(not duel.model.playback_pending)
	viewer._set_filter("Alle")
	viewer._search("Tessara")
	assert(viewer.selection.size() == 1)
	viewer._play_index(0)
	await process_frame
	duel = viewer.forest_encounter
	assert(duel.subject == "tessara" and duel.view.reveal.visible)
	viewer.show_battle_pause()
	viewer._resume_duel()
	assert(not duel.suspended)
	duel.view._skip_reveal()
	for frame in 360:
		await process_frame
		if not duel.model.playback_pending:
			break
	var has_tessara: bool = duel.model.units(0).any(func(unit: Dictionary): return unit.id == "tessara")
	assert(not duel.model.playback_pending and has_tessara)
	viewer.navigate_preview(0)
	await process_frame
	assert(viewer.forest_encounter != duel and viewer.forest_encounter.model.playback_pending)
	print("Beschwörungskino · %s Karten, %s eigene Szenen, Suche, Pause, Überspringen und Wiederholen geprüft" % [viewer.entries.size(), full])
	viewer.queue_free()
	await process_frame
	quit()
