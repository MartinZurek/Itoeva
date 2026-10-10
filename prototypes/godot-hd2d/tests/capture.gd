extends SceneTree

func _initialize() -> void:
	call_deferred("run")

func run() -> void:
	var world: Node3D = load("res://scenes/painted_world.tscn").instantiate()
	root.add_child(world)
	var dir := "res://captures"
	DirAccess.make_dir_recursive_absolute(dir)
	await create_timer(1.0).timeout
	await RenderingServer.frame_post_draw
	root.get_texture().get_image().save_jpg(dir + "/day.jpg", 0.95)
	world.yaw = 6.0
	world.zoom = 8.0
	await create_timer(1.0).timeout
	await RenderingServer.frame_post_draw
	root.get_texture().get_image().save_jpg(dir + "/depth.jpg", 0.95)
	world.toggle_dusk()
	await create_timer(1.0).timeout
	await RenderingServer.frame_post_draw
	root.get_texture().get_image().save_jpg(dir + "/dusk.jpg", 0.95)
	quit()
