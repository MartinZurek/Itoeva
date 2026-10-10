extends SceneTree

const Catalog = preload("res://scripts/world_catalog.gd")

func _initialize() -> void:
	call_deferred("run")

func capture(label: String) -> void:
	await create_timer(0.5).timeout
	await RenderingServer.frame_post_draw
	root.get_texture().get_image().save_jpg("res://captures/" + label + ".jpg", 0.95)

func run() -> void:
	DirAccess.make_dir_recursive_absolute("res://captures")
	DirAccess.make_dir_recursive_absolute("user://godot-tests")
	var app: Node3D = load("res://scenes/full_world.tscn").instantiate()
	app.save_path = "user://godot-tests/capture.json"
	root.add_child(app)
	await capture("full-menu")
	while app.loading_world:
		await process_frame
	app.save_data = {}
	app.selected = "fennec"
	await app.start_game()
	await capture("full-park")
	app.actor.position = app.stage.safe_position(Catalog.position_for("COAST_PATH", Vector2(0.43, 0.6)))
	app.actor.velocity = Vector3.ZERO
	app._update_camera(10.0)
	await capture("full-bridge")
	await app._mount_world("LIVING", Catalog.position_for("LIVING", Vector2(0.62, 0.68)))
	app._resume()
	await capture("full-living")
	await app._mount_world("KITCHEN", Catalog.position_for("KITCHEN", Vector2(0.70, 0.68)))
	app._resume()
	await capture("full-kitchen")
	app.show_settings()
	await capture("full-settings")
	DirAccess.remove_absolute(app.save_path)
	quit()
