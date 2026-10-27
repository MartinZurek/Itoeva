extends SceneTree
# Spielaufnahme mit der echten Figurensteuerung (Touch-Stick-Eingang), feste 30 Bilder/s.
# SPECIES, PLACE, ROOM, TAG per Umgebungsvariable.

const Catalog = preload("res://scripts/world_catalog.gd")

func _initialize() -> void:
	call_deferred("run")

func env(name: String, fallback: String) -> String:
	var v := OS.get_environment(name)
	return v if v != "" else fallback

func run() -> void:
	var out_dir := "res://captures/play_" + env("TAG", "a")
	DirAccess.make_dir_recursive_absolute(out_dir)
	DirAccess.make_dir_recursive_absolute("user://godot-tests")
	var app: Node3D = load("res://scenes/full_world.tscn").instantiate()
	app.save_path = "user://godot-tests/play-capture.json"
	root.add_child(app)
	while app.loading_world:
		await process_frame
	app.selected = env("SPECIES", "mosslyn")
	var room := env("ROOM", "OUTDOOR")
	await app._mount_world(room, Catalog.position_for(env("PLACE", "PARK"), Vector2(float(env("START_U", "0.12")), float(env("START_V", "0.62")))))
	app._resume()
	if env("CLEAN_VIEW", "0") == "1":
		app.canvas.visible = false
	var actor = app.actor
	if env("DUSK", "") == "1":
		app.dusk = true
		app._apply_world_time()
	# Ablauf: (Sekunden, Stick-Vektor)
	var plan := [[0.6, Vector2.ZERO], [3.0, Vector2(0.70, 0)], [2.6, Vector2(1.0, 0)], [0.5, Vector2.ZERO],
		[2.2, Vector2(-0.70, 0)], [0.5, Vector2.ZERO], [1.4, Vector2(0, -0.70)], [1.4, Vector2(0, 0.70)], [0.8, Vector2.ZERO]]
	# Gehen separat beurteilen, damit ein unveränderter Sprint den Eindruck
	# der gerade nachgezeichneten Standphasen nicht überlagert.
	if env("WALK_ONLY", "0") == "1":
		plan = [[0.4, Vector2.ZERO], [3.0, Vector2(0.70, 0)], [0.4, Vector2.ZERO],
			[3.0, Vector2(-0.70, 0)], [0.4, Vector2.ZERO]]
	if env("MOTION_CHAIN", "0") == "1":
		plan = [[0.4, Vector2.ZERO], [1.3, Vector2(0.70, 0)], [2.2, Vector2(1, 0)],
			[1.1, Vector2(0.70, 0)], [0.6, Vector2.ZERO], [0.9, Vector2(-0.70, 0)],
			[1.7, Vector2(-1, 0)], [0.8, Vector2(-0.70, 0)], [0.5, Vector2.ZERO],
			[1.0, Vector2(1, 0)], [0.6, Vector2.ZERO]]
	if env("M4_REVIEW", "0") == "1":
		plan = [[0.3, Vector2.ZERO], [2.0, Vector2(.70, 0)], [1.8, Vector2(1, 0)],
			[1.8, Vector2(.70, 0)], [1.4, Vector2.ZERO], [2.0, Vector2(-.70, 0)],
			[1.8, Vector2(-1, 0)], [1.8, Vector2(-.70, 0)], [1.4, Vector2.ZERO]]
	# Für volle Tiefenschritte einen breiten echten Innenraum verwenden. Auf dem
	# schmalen Außenweg beenden die korrekten Kollisionen die Bewegung vorzeitig.
	if env("DEPTH_ONLY", "0") == "1":
		plan = [[0.4, Vector2.ZERO], [1.4, Vector2(0, -0.70)], [0.3, Vector2.ZERO],
			[1.4, Vector2(0, 0.70)], [0.4, Vector2.ZERO]]
	var frame := 0
	var capture_size := Vector2i.ZERO
	var telemetry: Array = []
	for step in plan:
		var frames := int(step[0] * 30.0)
		for i in frames:
			actor.touch_vector = step[1]
			await physics_frame
			await RenderingServer.frame_post_draw
			var captured_image := root.get_texture().get_image()
			capture_size = captured_image.get_size()
			captured_image.save_png(out_dir + "/%04d.png" % frame)
			var screen: Rect2 = actor.visual_screen_rect(app.camera)
			var floor_screen: Vector2 = app.camera.unproject_position(actor.global_position)
			telemetry.append({"frame": frame, "input": [step[1].x, step[1].y],
				"position": [actor.position.x, actor.position.y, actor.position.z],
				"speed": actor.actual_speed, "running": actor.running,
				"atlas_frame": actor.sprite.frame, "atlas": actor.sprite.texture.resource_path,
				"pixel_size": actor.sprite.pixel_size, "offset_y": actor.sprite.offset.y,
				"stride": actor._stride_length(), "gait": actor.gait,
				"cycle_mode": actor.painted_cycle_mode if actor.painted_cycle_active else "rest",
				"transition": actor.authored_transition.active,
				"motion_state": actor.authored_locomotion.mode, "authored_lift": actor.authored_lift,
				"transition_elapsed": actor.authored_transition.elapsed,
				"flip_h": actor.sprite.flip_h, "floor_screen": [floor_screen.x, floor_screen.y],
				"screen_rect": [screen.position.x, screen.position.y, screen.size.x, screen.size.y]})
			frame += 1
	var report := FileAccess.open(out_dir + "/telemetry.json", FileAccess.WRITE)
	var viewport_size := root.get_visible_rect().size
	report.store_string(JSON.stringify({"fps": 30, "species": actor.species,
		"viewport_size": [viewport_size.x, viewport_size.y],
		"capture_size": [capture_size.x, capture_size.y], "clean_view": env("CLEAN_VIEW", "0"),
		"draft": env("MOTION_DRAFT", "0"), "motion_m4": env("MOTION_M4", "0"), "frames": telemetry}, "\t") + "\n")
	report.close()
	DirAccess.remove_absolute(app.save_path)
	print("play capture frames: ", frame)
	quit(0)
