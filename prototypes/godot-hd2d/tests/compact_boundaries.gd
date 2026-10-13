extends SceneTree

const Catalog = preload("res://scripts/world_catalog.gd")
var checks := 0
var failures := 0

func _initialize() -> void:
	call_deferred("run")

func check(ok: bool, label: String) -> void:
	checks += 1
	if not ok:
		failures += 1
		push_error(label)

func frames(count: int) -> void:
	for i in count:
		await physics_frame

func run() -> void:
	DirAccess.make_dir_recursive_absolute("user://godot-tests")
	var app: Node3D = load("res://scenes/full_world.tscn").instantiate()
	app.save_path = "user://godot-tests/compact.json"
	root.add_child(app)
	while app.loading_world:
		await process_frame
	await app._mount_world("OUTDOOR", Catalog.position_for("PARK", Vector2(0.5, 0.5)))
	app._resume()
	for resident in app.actors:
		resident.enabled = false
		if resident != app.actor:
			resident.collider.disabled = true
		check(is_equal_approx(resident.base_pixel_size, app.stage.base_pixel_size), "Gemeinsamer Grundmassstab: " + resident.species)
	var actor: CharacterBody3D = app.actor
	actor.enabled = true
	for region in Catalog.data().regions:
		for u in [0.18, 0.43, 0.76]:
			for sign in [-1.0, 1.0]:
				for action in ["walk", "roll", "jump"]:
					var x: float = region.origin + u * 30.0
					var band: Vector2 = app.stage._limits_at(x)
					actor.position = app.stage.safe_position(Vector3(x, 0.03, Catalog.image_z((band.x + band.y) * 0.5)), actor.collider.shape.radius)
					actor.stop_input()
					actor.state = "idle"
					actor.jump_clock = -1
					await frames(5)
					actor.facing = Vector2(0, sign)
					actor.touch_vector = Vector2(0, sign)
					if action != "walk":
						actor.request_action(action)
					var inside := true
					for i in 70:
						await frames(1)
						if not app.stage.is_walkable(actor.position, actor.collider.shape.radius, true):
							inside = false
					check(inside, "Wegkante haelt %s %s u=%s Richtung=%s" % [action, region.places[0], u, sign])
	actor.stop_input()
	var coast := Catalog.region_for("COAST_PATH")
	# Unabhaengige Bildkoordinaten: Brueckenausgang und Weg vor der Bank.
	for point in [[0.50, 389, true], [0.50, 435, false], [0.60, 415, true], [0.60, 475, false]]:
		var position := Vector3(float(coast.origin) + float(point[0]) * 30.0, 0.03, Catalog.image_z(float(point[1])))
		check(app.stage.is_walkable(position) == point[2], "Gemalter Kuestenweg statt Blumen/Steinwand: " + str(point))
	check(app.stage.natural_edges.size() == 1, "Natuerliche Steine statt Farblinien vorhanden")
	for place in Catalog.data().interiors:
		await app._mount_world(place, Catalog.position_for(place, Vector2(0.5, 0.7)))
		app._resume()
		var height: float = app.actor.sprite.pixel_size * 109.0 * 720.0 / app.stage.default_zoom
		check(height < 150.0, "Raumfigur bleibt kompakt: " + place)
		check(app.stage.natural_edges.is_empty(), "Raum bleibt frei von kuenstlichen Weglinien: " + place)
		for direction in [Vector2(-1, -1), Vector2(1, -1), Vector2(-1, 1), Vector2(1, 1)]:
			for action in ["walk", "roll", "jump"]:
				app.actor.position = app.stage.safe_position(Catalog.position_for(place, Vector2(0.5, 0.75)), app.actor.collider.shape.radius)
				app.actor.stop_input()
				app.actor.state = "idle"
				app.actor.jump_clock = -1
				await frames(5)
				app.actor.facing = direction.normalized()
				app.actor.touch_vector = direction.normalized()
				if action != "walk":
					app.actor.request_action(action)
				var inside := true
				for i in 70:
					await frames(1)
					if not app.stage.is_walkable(app.actor.position, app.actor.collider.shape.radius, true):
						inside = false
				check(inside, "Raumkante haelt diagonal %s %s %s" % [place, action, direction])
	print("Godot compact boundaries: %s Checks, %s Fehler" % [checks, failures])
	DirAccess.remove_absolute(app.save_path)
	quit(1 if failures else 0)
