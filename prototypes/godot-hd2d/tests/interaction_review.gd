extends SceneTree

const Catalog = preload("res://scripts/world_catalog.gd")
const Path = preload("res://scripts/interaction_path.gd")
var checks := 0
var failures := 0
var app: Node3D

func _initialize() -> void: call_deferred("run")

func check(condition: bool, description: String) -> void:
	checks += 1
	if not condition:
		failures += 1
		push_error(description)

func frames(count: int) -> void:
	for i in count: await physics_frame

func touch(point: Vector2, pressed: bool) -> void:
	var event := InputEventScreenTouch.new()
	event.index = 7
	event.position = point
	event.pressed = pressed
	root.push_input(event, true)

func run() -> void:
	DirAccess.make_dir_recursive_absolute("user://godot-tests")
	app = load("res://scenes/full_world.tscn").instantiate()
	app.save_path = "user://godot-tests/interaction-review.json"
	DirAccess.remove_absolute(app.save_path)
	root.add_child(app)
	while app.loading_world: await process_frame
	await app.start_game()
	await frames(15)
	var resident: CharacterBody3D = app.actors[1]
	app.actor.position = app.stage.safe_position(resident.position + Vector3(4.5, 0, 0), app.actor.collider.shape.radius)
	await frames(10)
	resident.set_meta("rest_until", app.game_clock + 0.4)
	var previous_rest: float = resident.get_meta("rest_until")
	app.request_interaction("talk", resident.species)
	check(not app.pending_interaction.is_empty(), "Talk approach starts before cancellation")
	check(float(resident.get_meta("rest_until")) > previous_rest + 10.0, "Target waits during approach")
	app.cancel_approach()
	check(is_equal_approx(float(resident.get_meta("rest_until")), previous_rest), "Cancellation restores NPC's previous wait")
	check(app.actor.touch_vector == Vector2.ZERO and app.interaction_route.is_empty(), "Cancellation releases steering and route")
	app.request_interaction("talk", resident.species)
	app.show_map()
	check(app.paused and app.pending_interaction.is_empty(), "Map cancels automatic movement")
	var before: Vector3 = app.actor.position
	await frames(8)
	check(app.actor.position.is_equal_approx(before), "Paused world stays still")
	app._resume()
	await frames(8)
	check(app.actor.position.is_equal_approx(before), "Closing map does not resume cancelled walking")
	# Android receives raw touches; mouse emulation is deliberately disabled.
	app._show_conversation(resident.species)
	await frames(5)
	var dialogue_text: String = Catalog.data().characters[resident.species].tagline
	check(app.dialog_open and app.ui.find_children("*", "Label", true, false).any(func(label): return label.text == dialogue_text),
		"Conversation renders the actual character text in its modal")
	var answer: Button = app.menu_buttons[0]
	var closes: Button = app.menu_buttons[1]
	touch(answer.get_global_rect().get_center(), true)
	touch(answer.get_global_rect().get_center(), false)
	check(answer.disabled, "Raw Android touch activates dialogue reply")
	var disabled_calls := {"count": 0}
	answer.pressed.connect(func(): disabled_calls.count += 1)
	touch(answer.get_global_rect().get_center(), true)
	touch(answer.get_global_rect().get_center(), false)
	check(disabled_calls.count == 0, "Disabled reply ignores raw Android touches like mouse clicks")
	touch(closes.get_global_rect().get_center(), true)
	touch(closes.get_global_rect().get_center(), false)
	await frames(2)
	check(not app.paused and not app.dialog_open, "Raw Android touch closes dialogue")
	# A seated start must navigate from the saved floor exit, not through its chair.
	for species in Catalog.data().characters:
		app.selected = species
		await app._mount_world("LIVING", Catalog.position_for("LIVING", Vector2(.5, .75)))
		app._resume()
		await frames(10)
		var seats: Array = app.interaction_targets().filter(func(t): return t.kind == "seat")
		var seat: Dictionary = seats[0]
		app.actor.position = seat.approach
		app.actor.stop_input()
		await frames(8)
		app.interact_with("seat", seat.id)
		check(app.actor.mounted and app.actor.state == "sit", "Original sitting pose mounted: " + species)
		var anchor: Vector3 = seat.seat.seat_position
		check(app.actor.position.is_equal_approx(anchor), "Species share painted seat anchor: " + species)
		check(app.actor.seat_occlusion_z > seat.seat.body.position.z, "Seated body is in front of its furniture mask: " + species)
		var exit: Vector3 = app.actor.seat_exit
		var doors: Array = app.interaction_targets().filter(func(t): return t.kind == "door" and not t.ready)
		doors.sort_custom(func(a, b): return a.distance > b.distance)
		check(not doors.is_empty(), "A distant destination exists while seated: " + species)
		if doors.is_empty(): continue
		app.request_interaction("door", doors[0].id)
		check(not app.actor.mounted and app.actor.position.is_equal_approx(exit), "Far tap leaves furniture synchronously: " + species)
		check(not app.pending_interaction.is_empty(), "Far tap finds a route from seated exit: " + species)
		app.cancel_approach()
	# Check every door direction from a valid distant floor point, using the
	# actual route follower and resulting room transition rather than AStar alone.
	app.selected = "fennec"
	var traversed := 0
	for link in Catalog.data().doors:
		for source: String in [link.a, link.b]:
			var destination: String = link.b if source == link.a else link.a
			var room := "OUTDOOR" if not Catalog.region_for(source).is_empty() else source
			await app._mount_world(room, Catalog.position_for(source, Vector2(.5, .75)))
			app._resume()
			await frames(12)
			var entries: Array = app.interaction_targets().filter(func(t): return t.kind == "door" and t.id == source + ":" + destination)
			check(entries.size() == 1, "Door direction available: " + source + " -> " + destination)
			if entries.is_empty(): continue
			var target: Dictionary = entries[0]
			var start: Vector3 = app.stage.safe_position(target.approach + Vector3(3.6, 0, 1.0), app.actor.collider.shape.radius)
			if Vector2(start.x - target.approach.x, start.z - target.approach.z).length() < 2.6:
				start = app.stage.safe_position(target.approach + Vector3(-4.0, 0, 1.0), app.actor.collider.shape.radius)
			app.actor.position = start
			app.actor.stop_input()
			await frames(8)
			var path: PackedVector3Array = Path.find_path(app.stage, app.actor.position, target.approach, app.actor.collider.shape.radius + .035)
			check(not path.is_empty(), "Distant door has a dry route: " + target.id)
			app.request_interaction("door", target.id)
			for i in 930:
				if app.pending_interaction.is_empty() and not app.switching: break
				await physics_frame
			await frames(4)
			var expected := "OUTDOOR" if not Catalog.region_for(destination).is_empty() else destination
			check(app.room == expected, "Approach enters actual destination: " + target.id + " / " + app.notice)
			traversed += 1
	check(traversed == 22, "All 22 original door directions exercised")
	# Activity confirmations are actual Android touch controls, and leave the
	# seated character with the promised stand-up context after completion.
	app.world_clock.utc_now_override = Time.get_unix_time_from_datetime_string("2026-01-15T21:30:00")
	app.world_clock.reset_to_real_time()
	for activity in [["NOOK", "read", "nook-armchair", "Lesen · +2 Stunden"],
		["BEDROOM", "sleep", "bedroom-bed", "Bis morgens schlafen"]]:
		await app._mount_world(activity[0], Catalog.position_for(activity[0], Vector2(.5, .8)))
		app._resume()
		await frames(10)
		app.request_interaction(activity[1], activity[2])
		for i in 930:
			if app.pending_interaction.is_empty(): break
			await physics_frame
		await frames(5)
		check(app.dialog_open and app.paused and app.menu_buttons[0].text == activity[3], "Activity has a visible mapped confirmation: " + activity[1])
		var button: Button = app.menu_buttons[0]
		touch(button.get_global_rect().get_center(), true)
		touch(button.get_global_rect().get_center(), false)
		await frames(3)
		check(not app.paused and not app.dialog_open, "Raw Android touch completes activity: " + activity[1])
		check(app.actor.mounted and app.context_target().get("kind") == "stand", "Seated activity offers standing up instead of a nearby door")
		if activity[1] == "read":
			check(app.world_clock.utc_offset == 7200.0 and app.world_clock.current_hour() == 0, "Raw reading touch advances local time by two hours")
		else:
			check(app.world_clock.current_hour() == 8 and app.world_clock.utc_offset > 7200.0, "Raw sleep touch advances to future local 08:00")
	print("Godot interaction review: %s checks, %s failures" % [checks, failures])
	DirAccess.remove_absolute(app.save_path)
	app.queue_free()
	await process_frame
	quit(1 if failures else 0)
