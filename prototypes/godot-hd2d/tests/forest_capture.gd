extends SceneTree
const Forest=preload("res://scripts/forest_catalog.gd")
var app: Node3D
func _initialize() -> void: call_deferred("run")
func capture(label: String) -> void:
	for i in 6: await process_frame
	await RenderingServer.frame_post_draw
	root.get_texture().get_image().save_png("res://captures/"+label+".png")
func run() -> void:
	DirAccess.make_dir_recursive_absolute("res://captures")
	DirAccess.make_dir_recursive_absolute("user://godot-tests")
	app=load("res://scenes/full_world.tscn").instantiate()
	app.save_path="user://godot-tests/forest-capture.json"
	DirAccess.remove_absolute(app.save_path)
	root.add_child(app)
	while app.loading_world: await process_frame
	await app.start_game()
	await app._mount_world("FOREST_TRAIL",Forest.position_for(0,.5))
	app._resume()
	app.stage.set_time(12.0)
	for i in range(7):
		app.actor.position=app.stage.safe_position(Forest.position_for(i,.50),app.actor.collider.shape.radius)
		app.actor.velocity=Vector3.ZERO
		app._update_camera(10.0)
		app._pause(true)
		app.forest_encounter.suspended=true
		app.stage.update_player(app.actor.position)
		await capture("forest-%s" % i)
	for i in range(1,7):
		app.actor.position=app.stage.safe_position(Vector3(i*20.0+1,.03,0),app.actor.collider.shape.radius)
		app._update_camera(10.0)
		app.stage.update_player(app.actor.position)
		await capture("forest-seam-%s" % i)
	for species in app.Catalog.STARTERS:
		app.selected=species
		await app._mount_world("FOREST_TRAIL",Forest.position_for(3,.295))
		app._resume()
		app.stage.set_time(12.0)
		var bench: Dictionary=app.stage.furniture[0]
		app.actor.position=bench.approach_position
		for j in 10: await physics_frame
		await app.interact_with("seat","forest-rest-bench")
		for j in 24: await physics_frame
		app._update_camera(10.0)
		await capture("forest-bench-"+species)
	app.selected="fennec"
	await app._mount_world("FOREST_TRAIL",Forest.position_for(5,.44))
	app._resume()
	app.stage.set_time(12.0)
	app.actor.position=Forest.position_for(5,.44)
	app.forest_encounter.model.begin()
	app.forest_encounter.resume()
	app.forest_encounter.suspended=true
	app.forest_encounter.model.advance_intro(3.2)
	app.forest_encounter.resume()
	app.forest_encounter.suspended=true
	await capture("forest-first-battle")
	DirAccess.remove_absolute(app.save_path)
	quit()
