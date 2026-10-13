extends SceneTree
const Catalog = preload("res://scripts/world_catalog.gd")
var app: Node3D
func _initialize() -> void: call_deferred("run")
func capture(label: String) -> void:
	await create_timer(0.4).timeout
	await RenderingServer.frame_post_draw
	var error := root.get_texture().get_image().save_png("res://captures/v026-" + label + ".png")
	assert(error == OK, "Capture could not be saved: " + label)
func mount(place: String, at := Vector2(0.5,0.75)) -> void:
	await app._mount_world("OUTDOOR" if not Catalog.region_for(place).is_empty() else place, Catalog.position_for(place,at))
	app._resume()
	for i in 15: await physics_frame
	app._update_camera(10.0)
func hour(value: float) -> void:
	app.world_clock.utc_now_override = Time.get_unix_time_from_datetime_dict({"year":2026,"month":1,"day":2,"hour":int(value)-1,"minute":0,"second":0})
	app.world_clock.reset_to_real_time()
	app._apply_world_time()
func run() -> void:
	DirAccess.make_dir_recursive_absolute("res://captures")
	DirAccess.make_dir_recursive_absolute("user://godot-tests")
	app = load("res://scenes/full_world.tscn").instantiate()
	app.save_path = "user://godot-tests/interactions-capture.json"
	DirAccess.remove_absolute(app.save_path)
	root.add_child(app)
	while app.loading_world: await process_frame
	await capture("menu")
	await app.start_game()
	hour(12)
	await capture("park-day")
	app._show_conversation("hootlet")
	await capture("conversation")
	app._resume()
	await mount("BEACH")
	await capture("coast-day")
	hour(22)
	await capture("coast-night")
	await mount("CAMP")
	await capture("camp-night")
	hour(12)
	for species in Catalog.data().characters:
		app.selected = species
		await mount("LIVING")
		app.request_interaction("seat","living-sofa")
		for i in 840:
			if app.pending_interaction.is_empty(): break
			await physics_frame
		await capture(species + "-sofa")
	app.selected="fennec"
	await mount("NOOK")
	app.request_interaction("read","nook-armchair")
	for i in 840:
		if app.pending_interaction.is_empty(): break
		await physics_frame
	await capture("reading")
	app._finish_reading()
	await capture("nook-seated")
	await mount("BEDROOM")
	app.request_interaction("sleep","bedroom-bed")
	for i in 840:
		if app.pending_interaction.is_empty(): break
		await physics_frame
	await capture("sleeping")
	app._finish_sleeping()
	await capture("bedroom-morning")
	app.show_settings()
	await capture("settings")
	DirAccess.remove_absolute(app.save_path)
	print("v026 interaction captures complete")
	quit()
