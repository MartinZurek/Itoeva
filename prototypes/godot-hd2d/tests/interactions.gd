extends SceneTree
const Catalog = preload("res://scripts/world_catalog.gd")
const Path = preload("res://scripts/interaction_path.gd")
var checks := 0
var failures := 0
func _initialize() -> void: call_deferred("run")
func check(condition: bool, description: String) -> void:
	checks += 1
	if not condition:
		failures += 1
		push_error(description)
func frames(count: int) -> void:
	for i in count: await physics_frame
func await_approach(app: Node3D) -> void:
	for i in 900:
		if app.pending_interaction.is_empty(): break
		await physics_frame
	await frames(3)
func run() -> void:
	var app: Node3D = load("res://scenes/full_world.tscn").instantiate()
	app.save_path = "user://godot-tests/interactions.json"
	DirAccess.make_dir_recursive_absolute("user://godot-tests")
	DirAccess.remove_absolute(app.save_path)
	root.add_child(app)
	while app.loading_world: await process_frame
	await app.start_game()
	await frames(15)
	var resident: CharacterBody3D = app.actors[1]
	app.actor.position = app.stage.safe_position(resident.position + Vector3(4.5, 0, 0), app.actor.collider.shape.radius)
	await frames(10)
	var target_species: String = resident.species
	app.request_interaction("talk", target_species)
	check(not app.pending_interaction.is_empty(), "Distant creature tap starts an approach")
	await await_approach(app)
	check(app.dialog_open and app.paused and app.menu_buttons.size() == 2, "Approach opens an actual conversation card")
	app.menu_buttons[0].pressed.emit()
	check(app.menu_buttons[0].disabled, "Conversation reply is accessible")
	app.menu_buttons[1].pressed.emit()
	check(not app.paused and app.actor.touch_vector == Vector2.ZERO, "Closing conversation restores manual controls")
	app.request_interaction("talk", target_species)
	app._resume()
	app.actor.position.x += 4.0
	app.request_interaction("talk", target_species)
	app.hud._update_stick(Vector2(160, 610))
	check(app.pending_interaction.is_empty(), "Manual stick cancels tap walking")
	app.actor.stop_input()
	# Every original furniture seat can be requested from a free position.
	for room in Catalog.data().interiors:
		await app._mount_world(room, Catalog.position_for(room, Vector2(0.5, 0.75)))
		app._resume()
		await frames(15)
		var targets: Array = app.interaction_targets().filter(func(t): return t.kind in ["seat", "read", "sleep"])
		for target in targets:
			app.actor.position = app.stage.safe_position(target.approach + Vector3(1.8, 0, 0.5), app.actor.collider.shape.radius)
			app.actor.stop_input()
			await frames(12)
			var path: PackedVector3Array = Path.find_path(app.stage, app.actor.position, target.approach, app.actor.collider.shape.radius + 0.035)
			check(not path.is_empty(), room + " reachable target " + target.id)
			var previous: Vector3 = app.actor.position
			previous.y = 0.03
			for point in path:
				check(Path.clear_segment(app.stage, previous, point, app.actor.collider.shape.radius + 0.03), "Approach does not cut furniture " + target.id)
				previous = point
			app.request_interaction(target.kind, target.id)
			await await_approach(app)
			check(app.actor.mounted if target.seat.seat != null else app.dialog_open, "Requested action is visible " + target.id + " / " + app.notice)
			if app.dialog_open: app._resume()
			if app.actor.mounted: app.actor.request_action("sit")
			await frames(6)
	# Exact clock changes through the UI, with indoor doors retaining offset.
	app.world_clock.utc_now_override = Time.get_unix_time_from_datetime_dict({"year":2026,"month":1,"day":2,"hour":19,"minute":30,"second":0})
	app.world_clock.reset_to_real_time()
	await app._mount_world("NOOK", Catalog.position_for("NOOK", Vector2(0.5, 0.8)))
	app._resume()
	await frames(10)
	app.request_interaction("read", "nook-armchair")
	await await_approach(app)
	check(app.dialog_open, "Reading has an explicit visible confirmation")
	app._finish_reading()
	check(app.world_clock.current_hour() == 22 and app.world_clock.utc_offset == 7200, "Reading advances real local game time by two hours")
	await app._mount_world("BEDROOM", Catalog.position_for("BEDROOM", Vector2(0.5, 0.8)))
	app._resume()
	await frames(10)
	check(app.world_clock.utc_offset == 7200, "Doors retain the session time")
	app.request_interaction("sleep", "bedroom-bed")
	await await_approach(app)
	check(app.dialog_open, "Bed offers sleeping")
	app._finish_sleeping()
	check(app.world_clock.current_hour() == 8, "Sleeping advances to next local morning")
	print("Godot interactions: %s checks, %s failures" % [checks, failures])
	DirAccess.remove_absolute(app.save_path)
	app.queue_free()
	await process_frame
	quit(1 if failures else 0)
