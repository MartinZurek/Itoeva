extends SceneTree

const Catalog = preload("res://scripts/world_catalog.gd")
var checks := 0
var failures := 0

func _initialize() -> void:
	call_deferred("run")

func frames(count: int) -> void:
	for i in count:
		await physics_frame

func run() -> void:
	var app: Node3D = load("res://scenes/full_world.tscn").instantiate()
	DirAccess.make_dir_recursive_absolute("user://godot-tests")
	app.save_path = "user://godot-tests/doors.json"
	root.add_child(app)
	while app.loading_world:
		await process_frame
	for species in Catalog.data().characters:
		app.selected = species
		for pair in Catalog.data().doors:
			for source in [pair.a, pair.b]:
				var destination: String = pair.b if source == pair.a else pair.a
				var target: Dictionary
				for entry in Catalog.doors_for(source):
					if entry.to == destination:
						target = entry
				await app._mount_world("OUTDOOR" if not Catalog.region_for(source).is_empty() else source, Catalog.position_for(source, target.at))
				app._resume()
				var physical: Dictionary
				for entry in app.stage.doors:
					if entry.to == destination and entry.from == source:
						physical = entry
				app.actor.position = app.stage.safe_position(physical.position, app.actor.collider.shape.radius)
				app.actor.velocity = Vector3.ZERO
				await frames(10)
				await app.interact()
				await frames(10)
				checks += 1
				if app.current_place() != destination or not app.actor.is_on_floor():
					failures += 1
					push_error("Tuer blockiert: " + species + " " + source + " → " + destination)
		print("Tuermatrix geprueft: " + species)
	print("Godot doors: %s Spezies/Tuerpfade, %s Fehler" % [checks, failures])
	DirAccess.remove_absolute(app.save_path)
	app.queue_free()
	await process_frame
	quit(1 if failures else 0)
