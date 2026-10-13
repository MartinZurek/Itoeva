extends SceneTree

const Catalog = preload("res://scripts/world_catalog.gd")
var app: Node3D
var checks := 0
var failures := 0

func _initialize() -> void:
	call_deferred("run")

func check(ok: bool, label: String) -> void:
	checks += 1
	if not ok:
		failures += 1
		push_error(label)

func floor_point(x: float, radius: float) -> Vector3:
	var band: Vector2 = app.stage._limits_at(x)
	return app.stage.safe_position(Vector3(x, 0.03, Catalog.image_z((band.x + band.y) * 0.5)), radius)

func reset_group() -> void:
	for creature in app.actors:
		creature.enabled = false
		creature.stop_input()
		creature.state = "idle"
		creature.jump_clock = -1
		creature.position = floor_point(110.0, creature.collider.shape.radius)
		creature.set_meta("rest_until", 0.0)
		creature.set_meta("patrol", {})
	app.actor.enabled = true

func walker(creature: CharacterBody3D, x: float, direction: float, left: float, right: float) -> void:
	creature.position = floor_point(x, creature.collider.shape.radius)
	creature.set_meta("anchor", creature.position)
	creature.set_meta("patrol", {"left": left, "right": right, "direction": direction,
		"wait_until": 0.0, "last_x": x, "stalled": 0.0})
	creature.enabled = true

func tick() -> void:
	await physics_frame
	app.game_clock += 1.0 / 60.0
	app._update_residents(1.0 / 60.0)

func run() -> void:
	DirAccess.make_dir_recursive_absolute("user://godot-tests")
	app = load("res://scenes/full_world.tscn").instantiate()
	app.save_path = "user://godot-tests/npc-traffic.json"
	root.add_child(app)
	while app.loading_world: await process_frame
	await app._mount_world("OUTDOOR", Catalog.position_for("PARK", Vector2(.7, .7)))
	app._resume()
	app.set_process(false)
	check(app.actors.size() == 6, "Alle sechs Originalwesen vorhanden")
	for creature in app.actors:
		check(creature.collision_mask == 1, "Welt bleibt harte Kollision, Nachbarn weich: " + creature.species)
	var a: CharacterBody3D = app.actors[1]
	var b: CharacterBody3D = app.actors[2]
	reset_group()
	app.actor.position = floor_point(76, app.actor.collider.shape.radius)
	walker(a, 74, 1, 70, 82)
	var base_size: float = a.base_pixel_size
	var smallest_distance := INF
	var safe := true
	for i in 240:
		await tick()
		var gap: Vector3 = a.position - app.actor.position
		smallest_distance = minf(smallest_distance, Vector2(gap.x, gap.z).length())
		safe = safe and app.stage.is_walkable(a.position, a.collider.shape.radius, true)
	check(a.position.x > app.actor.position.x + 0.8, "Spaziergaenger geht an wartendem Spieler vorbei statt automatisch zu stoppen")
	check(smallest_distance > a.collider.shape.radius + app.actor.collider.shape.radius, "Auf breitem Weg wird wirklich seitlich ausgewichen")
	check(safe and is_equal_approx(base_size, a.base_pixel_size), "Ausweichen bleibt auf Weg und veraendert keinen Figurenmassstab")
	reset_group()
	walker(a, 74.5, 1, 70, 82)
	walker(b, 77.5, -1, 70, 82)
	safe = true
	var passed := false
	for i in 300:
		await tick()
		passed = passed or a.position.x > b.position.x + 0.8
		safe = safe and app.stage.is_walkable(a.position, a.collider.shape.radius, true) and app.stage.is_walkable(b.position, b.collider.shape.radius, true)
	check(passed and safe, "Gegenverkehr passiert ohne gegenseitiges Stillstehen und ohne Wegverletzung")
	reset_group()
	walker(a, 76, 1, 70, 82)
	app.actor.position = floor_point(73, app.actor.collider.shape.radius)
	app.actor.touch_vector = Vector2.RIGHT
	for i in 150: await tick()
	check(app.actor.position.x > a.position.x + 0.5, "Spieler kann einen laufenden Nachbarn ueberholen")
	app.actor.stop_input()
	reset_group()
	walker(a, 75, 1, 70, 82)
	a.set_meta("rest_until", app.game_clock + 1.0)
	var resting: Vector3 = a.position
	for i in 50: await tick()
	check(a.position.distance_to(resting) < 0.06 and a.npc_vector == Vector2.ZERO, "Explizite Gespraechspause bleibt erhalten")
	for i in 130: await tick()
	check(a.position.x > resting.x + 1.0, "Nach Gespraech nimmt Bewohner seinen Spaziergang wieder auf")
	reset_group()
	var lows: Dictionary = {}
	var highs: Dictionary = {}
	for i in range(1, app.actors.size()):
		var creature: CharacterBody3D = app.actors[i]
		walker(creature, 73.0 + i * 0.8, 1.0 if i % 2 == 0 else -1.0, 71, 81)
		lows[creature.species] = creature.position.x
		highs[creature.species] = creature.position.x
	safe = true
	for frame in 540:
		await tick()
		for i in range(1, app.actors.size()):
			var creature: CharacterBody3D = app.actors[i]
			lows[creature.species] = minf(lows[creature.species], creature.position.x)
			highs[creature.species] = maxf(highs[creature.species], creature.position.x)
			safe = safe and app.stage.is_walkable(creature.position, creature.collider.shape.radius, true)
	for i in range(1, app.actors.size()):
		var creature: CharacterBody3D = app.actors[i]
		check(highs[creature.species] - lows[creature.species] > 3.0, "Fuenfergruppe bleibt unterwegs: " + creature.species)
	check(safe, "Die ganze Gruppe bleibt innerhalb gemalter Weggrenzen")
	reset_group()
	walker(a, 42.1, 1, 40.5, 45)
	walker(b, 43.7, -1, 40.5, 45)
	safe = true
	passed = false
	for i in 420:
		await tick()
		passed = passed or a.position.x > b.position.x + 0.6
		safe = safe and app.stage.is_walkable(a.position, a.collider.shape.radius, true) and app.stage.is_walkable(b.position, b.collider.shape.radius, true)
	check(passed and safe, "Auch schmale gebogene Holzbruecke kann ohne Stau passiert werden")
	DirAccess.remove_absolute(app.save_path)
	app.queue_free()
	await process_frame
	await process_frame
	print("Godot NPC traffic: %s Checks, %s Fehler" % [checks, failures])
	quit(1 if failures else 0)
