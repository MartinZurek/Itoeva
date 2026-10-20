extends SceneTree
const Catalog = preload("res://scripts/world_catalog.gd")
const Forest = preload("res://scripts/forest_catalog.gd")
func _initialize() -> void: call_deferred("run")
func run() -> void:
	DirAccess.make_dir_recursive_absolute("res://captures")
	DirAccess.make_dir_recursive_absolute("user://godot-tests")
	var app: Node3D=load("res://scenes/full_world.tscn").instantiate()
	app.save_path="user://godot-tests/route-capture.json"
	DirAccess.remove_absolute(app.save_path)
	root.add_child(app)
	while app.loading_world: await process_frame
	app.selected="mosslyn"
	await app.start_game()
	for location in ["FOREST","SUN_GLADE","MIST_WATCH","MILL_BROOK","ASH_ASCENT","SEAL_COURT","FOREST_TRAIL"]:
		await app._mount_world("OUTDOOR" if location=="FOREST" else location, Forest.position_for(0,.3) if location=="FOREST_TRAIL" else Catalog.position_for(location,Vector2(.5,.55)))
		app._resume()
		app.stage.set_time(12.0)
		app.actor.stop_input()
		app._update_camera(10.0)
		for i in 8: await physics_frame
		await RenderingServer.frame_post_draw
		root.get_texture().get_image().save_png("res://captures/route-"+location.to_lower()+".png")
	DirAccess.remove_absolute(app.save_path)
	app.queue_free()
	await process_frame
	quit()
