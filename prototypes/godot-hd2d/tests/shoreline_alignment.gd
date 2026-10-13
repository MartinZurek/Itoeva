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
	for i in count: await physics_frame

func run() -> void:
	DirAccess.make_dir_recursive_absolute("user://godot-tests")
	var app: Node3D = load("res://scenes/full_world.tscn").instantiate()
	app.save_path = "user://godot-tests/shoreline.json"
	root.add_child(app)
	while app.loading_world: await process_frame
	await app._mount_world("OUTDOOR", Catalog.position_for("POND", Vector2(.4, .5)))
	app._resume()
	for resident in app.actors:
		resident.enabled = false
		if resident != app.actor: resident.collider.disabled = true
	# Unabhaengig am normalisierten Originalbild gemessene Planken/Wasserpixel.
	for fixture in [[.105,450,true],[.135,452,true],[.17,455,true],
		[.105,472,false],[.135,480,false],[.17,480,false],[.17,433,false],
		[.55,417,true],[.59,416,true],[.63,418,true],[.68,419,true],
		[.55,442,false],[.59,450,false],[.63,470,false],[.68,448,false],
		[.59,398,false],[.35,455,true],[.35,495,false]]:
		var point := Vector3(float(fixture[0])*30, .03, Catalog.image_z(float(fixture[1])))
		check(app.stage.is_walkable(point) == fixture[2], "Planken/Ufer statt See: " + str(fixture))
	# Das Anschlussbild hatte seine eigene, zu tief liegende Physik.
	for fixture in [[27.5,457,true],[30,456,true],[33.5,457,true],
		[27.5,505,false],[30,500,false],[33.5,490,false],[30,425,false],
		[37,448,true],[37,498,false]]:
		var point := Vector3(float(fixture[0]), .03, Catalog.image_z(float(fixture[1])))
		check(app.stage.is_walkable(point) == fixture[2], "Trockener Anschluss/Ufer: " + str(fixture))
	var actor: CharacterBody3D = app.actor
	actor.enabled = true
	for u in [.105, .17, .59, .68, .90]:
		for direction in [-1.0, 1.0]:
			for action in ["walk", "roll", "jump"]:
				actor.position = app.stage.safe_position(Vector3(u*30, .03, 0), actor.collider.shape.radius)
				actor.stop_input()
				actor.state = "idle"
				actor.jump_clock = -1
				await frames(5)
				actor.facing = Vector2(0, direction)
				actor.touch_vector = Vector2(0, direction)
				if action != "walk": actor.request_action(action)
				var stayed_dry := true
				for i in 75:
					await physics_frame
					stayed_dry = stayed_dry and app.stage.is_walkable(actor.position, actor.collider.shape.radius, true)
				check(stayed_dry, "Steg/Ufer haelt %s u=%s Richtung=%s" % [action,u,direction])
	actor.stop_input()
	check(app.stage.find_child("BegehbareWegkanten",true,false) == null, "Keine Farblinie")
	check(Catalog.geometry().railings.size() == 2, "Beide Holzstege haben ein passendes Gelaender")
	print("Godot shoreline alignment: %s Checks, %s Fehler" % [checks, failures])
	DirAccess.remove_absolute(app.save_path)
	quit(1 if failures else 0)
