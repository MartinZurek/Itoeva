extends SceneTree

const Catalog = preload("res://scripts/world_catalog.gd")
var failures := 0
var checks := 0

func _initialize() -> void:
	call_deferred("run")

func capture(label: String, app: Node3D) -> void:
	app.actor.enabled = false
	app.actor._update_occlusion()
	app._update_camera(10)
	app.actor.sprite.visible = false
	await create_timer(0.1).timeout
	await RenderingServer.frame_post_draw
	var background := root.get_texture().get_image().get_data()
	app.actor.sprite.visible = true
	await create_timer(0.1).timeout
	await RenderingServer.frame_post_draw
	var rendered := root.get_texture().get_image()
	checks += 1
	if background == rendered.get_data():
		failures += 1
		push_error("Vollstaendig ueberdeckte Figur: " + label)
	rendered.save_jpg("res://captures/visual/" + label + ".jpg", 0.95)

func run() -> void:
	DirAccess.make_dir_recursive_absolute("res://captures/visual")
	DirAccess.make_dir_recursive_absolute("user://godot-tests")
	var app: Node3D = load("res://scenes/full_world.tscn").instantiate()
	app.save_path = "user://godot-tests/visual-capture.json"
	root.add_child(app)
	while app.loading_world:
		await process_frame
	for room in Catalog.data().interiors:
		for species in Catalog.data().characters:
			app.selected = species
			await app._mount_world(room, Catalog.position_for(room, Vector2(0.53, 0.60)))
			app._resume()
			app.actor.sprite.frame = 142
			app.actor._update_sprite_scale()
			await capture(room + "-" + species, app)
	app.selected = "fennec"
	await app._mount_world("OUTDOOR", Catalog.position_for("POND", Vector2(0.5, 0.5)))
	app._resume()
	for creature in app.actors:
		creature.enabled = false
	for region in Catalog.data().regions:
		for place in region.places:
			app.actor.position = app.stage.safe_position(Catalog.position_for(place, Vector2(0.5, 0.5)), app.actor.collider.shape.radius)
			app.actor.sprite.frame = 142
			app.actor._update_sprite_scale()
			await capture(place, app)
	for i in range(6):
		app.actor.position = app.stage.safe_position(Vector3((i + 1) * 30.0, 0.03, 0.0), app.actor.collider.shape.radius)
		await capture("seam-%s" % i, app)
	for u in [0.32, 0.39, 0.43, 0.48, 0.50, 0.60]:
		var region := Catalog.region_for("COAST_PATH")
		var band := Catalog.band(region, u, true)
		app.actor.position = Vector3(float(region.origin) + u * 30.0, 0.03, Catalog.image_z((band.x + band.y) * 0.5))
		app.actor._update_sprite_scale()
		await capture("bridge-%s" % u, app)
	for u in [0.52, 0.59, 0.63, 0.71]:
		var region := Catalog.region_for("POND")
		var band := Catalog.band(region, u, true)
		app.actor.position = Vector3(float(region.origin) + u * 30.0, 0.03, Catalog.image_z((band.x + band.y) * 0.5))
		app.actor._update_sprite_scale()
		await capture("wood-bridge-%s" % u, app)
	# Der Spieler naehert sich einem Bewohner: sichtbare, genau zuordenbare Aktion.
	var resident: CharacterBody3D
	for creature in app.actors:
		if creature.species == "hootlet": resident = creature
	app.actor.position = app.stage.safe_position(resident.position + Vector3.RIGHT * 0.9, app.actor.collider.shape.radius)
	await capture("talk-hootlet", app)
	DirAccess.remove_absolute(app.save_path)
	print("Godot visual captures: %s Bildpaare, %s vollstaendig ueberdeckte Figuren" % [checks, failures])
	quit(1 if failures else 0)
