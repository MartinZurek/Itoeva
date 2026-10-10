extends SceneTree

const Catalog = preload("res://scripts/world_catalog.gd")
var checks := 0
var failures := 0

func _initialize() -> void:
	call_deferred("run")

func run() -> void:
	var app: Node3D = load("res://scenes/full_world.tscn").instantiate()
	DirAccess.make_dir_recursive_absolute("user://godot-tests")
	app.save_path = "user://godot-tests/framing.json"
	root.add_child(app)
	while app.loading_world:
		await process_frame
	var bounds := {}
	for species in Catalog.data().characters:
		var atlas: Image = app.textures[Catalog.data().characters[species].asset].get_image()
		bounds[species] = {}
		for frame in [0, 114, 121, 130, 137, 169]:
			var card := atlas.get_region(Rect2i((frame % 12) * 128, (frame / 12) * 128, 128, 128))
			bounds[species][frame] = card.get_used_rect()
	for room in Catalog.data().interiors:
		await app._mount_world(room, Catalog.position_for(room, Vector2(0.5, 0.7)))
		for species in Catalog.data().characters:
			app.actor.species = species
			app.actor.set_world_scale(app.stage.base_pixel_size, app.stage.jump_height)
			app.stage.fit_character(app.actor.sprite.pixel_size * (1.032967 if species == "wyrmling" else 1.0))
			var visible := true
			for side in [-1.0, 1.0]:
				for depth in [0.05, 0.95]:
					app.actor.position = app.stage.safe_position(Catalog.position_for(room, Vector2(0 if side < 0 else 1, depth)))
					for angle in [-8.0, 0.0, 8.0]:
						app.yaw = angle
						app.zoom = app.stage.zoom_min
						app.camera.size = app.zoom
						app._update_camera(10.0)
						for frame in bounds[species]:
							app.actor.sprite.frame = frame
							app.actor._update_sprite_scale()
							var rect: Rect2i = bounds[species][frame]
							var camera_right: Vector3 = app.camera.global_basis.x
							camera_right.y = 0
							camera_right = camera_right.normalized()
							for x in [rect.position.x, rect.end.x]:
								for y in [rect.position.y, rect.end.y]:
									var point: Vector3 = app.actor.position + camera_right * (float(x) - 64.0) * app.actor.sprite.pixel_size + Vector3.UP * (126.0 - float(y)) * app.actor.sprite.pixel_size
									var pixel: Vector2 = app.camera.unproject_position(point)
									if pixel.x < 0 or pixel.x > 1280 or pixel.y < 0 or pixel.y > 720:
										visible = false
			checks += 1
			if not visible:
				failures += 1
				push_error("Figur ausserhalb des Bilds: " + room + " / " + species)
	print("Godot framing: %s Raum/Spezies-Kombinationen, %s Fehler" % [checks, failures])
	DirAccess.remove_absolute(app.save_path)
	app.queue_free()
	await process_frame
	quit(1 if failures else 0)
