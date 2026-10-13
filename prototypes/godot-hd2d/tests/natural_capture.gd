extends SceneTree

const Catalog = preload("res://scripts/world_catalog.gd")
var app: Node3D

func _initialize() -> void:
	call_deferred("run")

func capture(label: String) -> void:
	app.actor.enabled = true
	app.actor.stop_input()
	for i in range(12): await physics_frame
	app.actor.enabled = false
	app.actor.sprite.frame = 142
	app.actor._update_sprite_scale()
	app._update_camera(10)
	app.hud._update_markers()
	await create_timer(.15).timeout
	await RenderingServer.frame_post_draw
	root.get_texture().get_image().save_jpg("res://captures/natural/" + label + ".jpg", .95)

func run() -> void:
	DirAccess.make_dir_recursive_absolute("res://captures/natural")
	DirAccess.make_dir_recursive_absolute("user://godot-tests")
	app = load("res://scenes/full_world.tscn").instantiate()
	app.save_path = "user://godot-tests/natural-capture.json"
	root.add_child(app)
	while app.loading_world: await process_frame
	for species in ["fennec", "hootlet"]:
		app.selected = species
		await app._mount_world("LIVING", Catalog.position_for("LIVING", Vector2(.53, .60)))
		app._resume()
		await capture("living-" + species)
	app.selected = "fennec"
	await app._mount_world("OUTDOOR", Catalog.position_for("PARK", Vector2(.5, .6)))
	app._resume()
	for creature in app.actors: creature.enabled = false
	await capture("park")
	for entry in [["COAST_PATH", .43, "stone-bridge"], ["POND", .59, "wood-bridge"]]:
		var region := Catalog.region_for(entry[0])
		var band := Catalog.band(region, entry[1], true)
		app.actor.position = Vector3(float(region.origin) + float(entry[1]) * 30, .03, Catalog.image_z((band.x + band.y) * .5))
		await capture(entry[2])
	var resident: CharacterBody3D
	for creature in app.actors:
		if creature.species == "hootlet": resident = creature
	app.actor.position = app.stage.safe_position(resident.position + Vector3.RIGHT * .9, app.actor.collider.shape.radius)
	await capture("talk-hootlet")
	DirAccess.remove_absolute(app.save_path)
	print("Godot natural presentation: 6 Renderbilder")
	quit()
