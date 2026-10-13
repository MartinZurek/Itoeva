extends SceneTree
const Catalog = preload("res://scripts/world_catalog.gd")
var app: Node3D

func _initialize() -> void:
	call_deferred("run")

func save_frame(label: String) -> void:
	var path := "res://captures/readability/" + label + ".jpg"
	var error := root.get_texture().get_image().save_jpg(path,.95)
	assert(error == OK and FileAccess.get_file_as_bytes(path).size() > 0, "Renderbild fehlt: " + label)

func capture(label: String) -> void:
	app.actor.stop_input()
	for i in 18: await physics_frame
	app.actor.enabled = false
	app._sort_actors()
	app._update_camera(10)
	app.hud._update_markers()
	await RenderingServer.frame_post_draw
	save_frame(label)
	app.actor.enabled = true

func run() -> void:
	DirAccess.make_dir_recursive_absolute("res://captures/readability")
	DirAccess.make_dir_recursive_absolute("user://godot-tests")
	app = load("res://scenes/full_world.tscn").instantiate()
	app.save_path = "user://godot-tests/readability-capture.json"
	root.add_child(app)
	while app.loading_world: await process_frame
	await RenderingServer.frame_post_draw
	save_frame("menu")
	for species in ["fennec","hootlet"]:
		app.selected = species
		await app._mount_world("LIVING",Catalog.position_for("LIVING",Vector2(.53,.60)))
		app._resume()
		await capture("room-"+species)
		app.actor.request_action("sit")
		for i in 45: await physics_frame
		await capture("room-sitting-"+species)
	app.selected = "fennec"
	await app._mount_world("OUTDOOR",Catalog.position_for("PARK",Vector2(.5,.6)))
	app._resume()
	for resident in app.actors: resident.enabled = false
	app.actor.enabled = true
	for point in [[.105,"small-jetty"],[.59,"wood-bridge"],[.90,"shore-transition"]]:
		app.actor.state = "idle"
		app.actor.facing = Vector2.RIGHT
		app.actor.position = app.stage.safe_position(Vector3(float(point[0])*30,.03,0),app.actor.collider.shape.radius)
		await capture(point[1])
	var resident: CharacterBody3D
	for creature in app.actors:
		if creature.species == "hootlet": resident = creature
	app.actor.position = app.stage.safe_position(resident.position+Vector3.RIGHT*2.25,app.actor.collider.shape.radius)
	await capture("talk-distance")
	app.actor.position = app.stage.safe_position(Catalog.position_for("PARK",Vector2(.60,.60)),app.actor.collider.shape.radius)
	for phase in range(8):
		app.actor.touch_vector = Vector2.RIGHT
		for i in 6: await physics_frame
		app._update_camera(10)
		await RenderingServer.frame_post_draw
		save_frame("running-%s"%phase)
	DirAccess.remove_absolute(app.save_path)
	print("Godot readability capture: 17 Renderbilder")
	quit()
