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
	# Für volle Tiefenschritte einen breiten echten Innenraum verwenden. Auf dem
	# schmalen Außenweg beenden die korrekten Kollisionen die Bewegung vorzeitig.
	if env("DEPTH_ONLY", "0") == "1":
		plan = [[0.4, Vector2.ZERO], [1.4, Vector2(0, -0.70)], [0.3, Vector2.ZERO],
			[1.4, Vector2(0, 0.70)], [0.4, Vector2.ZERO]]
	var frame := 0
	var telemetry: Array = []
	for step in plan:
		var frames := int(step[0] * 30.0)
		for i in frames:
			actor.touch_vector = step[1]
			await physics_frame
			await RenderingServer.frame_post_draw
			root.get_texture().get_image().save_png(out_dir + "/%04d.png" % frame)
			telemetry.append({"frame": frame, "input": [step[1].x, step[1].y],
				"position": [actor.position.x, actor.position.y, actor.position.z],
				"speed": actor.actual_speed, "running": actor.running,
				"atlas_frame": actor.sprite.frame, "atlas": actor.sprite.texture.resource_path,
				"pixel_size": actor.sprite.pixel_size, "offset_y": actor.sprite.offset.y,
				"stride": actor._stride_length(), "gait": actor.gait})
			frame += 1
	var report := FileAccess.open(out_dir + "/telemetry.json", FileAccess.WRITE)
	report.store_string(JSON.stringify({"fps": 30, "species": actor.species,
		"draft": env("MOTION_DRAFT", "0"), "frames": telemetry}, "\t") + "\n")
	report.close()
	DirAccess.remove_absolute(app.save_path)
	print("play capture frames: ", frame)
	quit(0)
