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
	app.save_path = "user://godot-tests/bridge-navigation.json"
	DirAccess.remove_absolute(app.save_path)
	root.add_child(app)
	while app.loading_world: await process_frame
	await app.start_game()
	await frames(15)
	var resident: CharacterBody3D = app.actors[1]
	for pair in [[2.3, 9.0], [14.2, 22.5], [2.3, 23.0]]:
		app._resume()
		app.actor.position = app.stage.safe_position(dry_center(app.stage, pair[0]), app.actor.collider.shape.radius)
		app.actor.stop_input()
		resident.position = app.stage.safe_position(dry_center(app.stage, pair[1]), resident.collider.shape.radius)
		resident.stop_input()
		resident.set_meta("rest_until", app.game_clock + 20.0)
		await frames(8)
		var radius: float = app.actor.collider.shape.radius + .035
		var route: PackedVector3Array = Path.find_path(app.stage, app.actor.position, resident.position, radius)
		check(not route.is_empty(), "Curved bridge corridor is represented in navigation: " + str(pair))
		var from: Vector3 = app.actor.position
		var dry_route := true
		for point in route:
			dry_route = dry_route and Path.clear_segment(app.stage, from, point, radius)
			from = point
		check(dry_route, "Every bridge shortcut respects dry geometry: " + str(pair))
		app.request_interaction("talk", resident.species)
		check(not app.pending_interaction.is_empty(), "Far character tap starts a bridge approach: " + str(pair))
		var stayed_dry := true
		for frame in 930:
			stayed_dry = stayed_dry and app.stage.is_walkable(app.actor.position, app.actor.collider.shape.radius, true)
			if app.pending_interaction.is_empty(): break
			await physics_frame
		await frames(3)
		check(stayed_dry, "Actual route follower remains on the bridge, never in the lake: " + str(pair))
		check(app.dialog_open and app.paused, "Actual far tap reaches a conversation across the bridge: " + str(pair) + " / " + app.notice)
	print("Godot bridge navigation: %s checks, %s failures" % [checks, failures])
	DirAccess.remove_absolute(app.save_path)
	app.queue_free()
	await process_frame
	quit(1 if failures else 0)
