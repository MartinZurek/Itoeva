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
	var complete: Dictionary=JSON.parse_string(FileAccess.get_file_as_string("res://data/compact_bounds.json"))
	for species in Catalog.STARTERS:
		var atlas: Image = app.textures[Catalog.data().characters[species].asset].get_image()
		var compact: bool = Catalog.data().characters[species].get("compact", false)
		var columns: int = 8 if compact else 12
		var cell := Vector2i(181, 181) if compact else Vector2i(128, 128)
		bounds[species] = {}
		for frame in ([0, 7, 11, 26, 29, 31] if compact else [0, 114, 121, 130, 137, 169]):
			var card := atlas.get_region(Rect2i((frame % columns) * cell.x, (frame / columns) * cell.y, cell.x, cell.y))
			var b: Array=complete[species][frame] if compact else []
			bounds[species][frame] = Rect2i(b[0],b[1],b[2],b[3]) if compact else card.get_used_rect()
	for room in Catalog.data().interiors:
		await app._mount_world(room, Catalog.position_for(room, Vector2(0.5, 0.7)))
		for species in Catalog.STARTERS:
			app.selected = species
			await app._mount_world(room, Catalog.position_for(room, Vector2(0.5, 0.7)))
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
							var sprite: Sprite3D = app.actor.sprite
							var cell := Vector2(sprite.texture.get_width() / float(sprite.hframes), sprite.texture.get_height() / float(sprite.vframes))
							var camera_right: Vector3 = app.camera.global_basis.x
							for x in [rect.position.x, rect.end.x]:
								for y in [rect.position.y, rect.end.y]:
									var local_x := float(x) - cell.x * 0.5
									if sprite.flip_h: local_x = -local_x
									var point: Vector3 = app.actor.position + sprite.position + camera_right * local_x * sprite.pixel_size + app.camera.global_basis.y * (sprite.offset.y + cell.y * 0.5 - float(y)) * sprite.pixel_size
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
