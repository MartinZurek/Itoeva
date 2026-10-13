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

func dry_center(stage: Node3D, x: float) -> Vector3:
	var band: Vector2 = stage._limits_at(x)
	return Vector3(x, .03, Catalog.image_z((band.x + band.y) * .5))

func run() -> void:
	DirAccess.make_dir_recursive_absolute("user://godot-tests")
	var app: Node3D = load("res://scenes/full_world.tscn").instantiate()
	app.save_path = "user://godot-tests/interaction-bridges.json"
	DirAccess.remove_absolute(app.save_path)
	root.add_child(app)
	while app.loading_world: await process_frame
	await app.start_game()
	for pair in [[2.3, 9.0], [14.2, 22.5], [2.3, 23.0], [34.0, 50.0], [50.0, 34.0]]:
		app._resume()
		app.cancel_approach()
		var radius: float = app.actor.collider.shape.radius
		app.actor.position = app.stage.safe_position(dry_center(app.stage, pair[0]), radius + .035)
		app.actor.stop_input()
		await frames(10)
		var destination: Vector3 = app.stage.safe_position(dry_center(app.stage, pair[1]), radius + .035)
		var began := Time.get_ticks_usec()
		var route: PackedVector3Array = Path.find_path(app.stage, app.actor.position, destination, radius + .035)
		print("BRIDGE route %s: %.2f ms, %s corners" % [pair, (Time.get_ticks_usec() - began) / 1000.0, route.size()])
		check(not route.is_empty(), "Dry route exists for actual follower: " + str(pair))
		if route.is_empty(): continue
		app.pending_interaction = {"kind": "test", "id": "bridge-review"}
		app.interaction_route = route
		app.approach_elapsed = 0.0
		var elapsed := 0.0
		for frame in 900:
			check(app.actor.is_on_floor(), "Follower stays on floor: %s / %s" % [pair, frame])
			check(app.stage.is_walkable(app.actor.position, radius), "Full physical capsule stays dry: %s / %s / %s" % [pair, frame, app.actor.position])
			if app.pending_interaction.is_empty(): break
			await physics_frame
			elapsed += 1.0 / Engine.physics_ticks_per_second
		await frames(5)
		var gap := Vector2(app.actor.position.x - destination.x, app.actor.position.z - destination.z).length()
		print("BRIDGE follower %s: %.2f seconds, %.3f endpoint gap" % [pair, elapsed, gap])
		check(app.pending_interaction.is_empty() and elapsed < 15.0, "Actual follower completes within 15 seconds: " + str(pair))
		check(gap < .20, "Actual follower reaches the dry endpoint: " + str(pair))
	print("Godot interaction bridges: %s checks, %s failures" % [checks, failures])
	DirAccess.remove_absolute(app.save_path)
	app.queue_free()
	await process_frame
	quit(1 if failures else 0)
