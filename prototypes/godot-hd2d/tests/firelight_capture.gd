extends SceneTree
const Catalog = preload("res://scripts/world_catalog.gd")
const Forest = preload("res://scripts/forest_catalog.gd")
var app: Node3D
func _initialize() -> void: call_deferred("run")
func capture(label: String, clock: float) -> void:
	if app.room=="FOREST_TRAIL":
		var darkness: float=Forest.region_at(app.actor.position.x).darkness
		app.actor.painted_material.set_shader_parameter("scene_light",Color.WHITE.lerp(Color(.33,.43,.58),darkness*.8))
	for material in app.stage.painted_materials: material.set_shader_parameter("animation_time",clock)
	if app.stage.get("forest_materials") != null:
		for material in app.stage.forest_materials.values(): material.set_shader_parameter("animation_time",clock)
	app.actor._update_occlusion()
	app.actor.painted_material.set_shader_parameter("animation_time",clock)
	for i in 4: await process_frame
	await RenderingServer.frame_post_draw
	root.get_texture().get_image().save_png("res://captures/firelight-"+label+".png")
func run() -> void:
	DirAccess.make_dir_recursive_absolute("res://captures")
	DirAccess.make_dir_recursive_absolute("user://godot-tests")
	app=load("res://scenes/full_world.tscn").instantiate()
	app.save_path="user://godot-tests/firelight-capture.json"
	DirAccess.remove_absolute(app.save_path)
	root.add_child(app)
	while app.loading_world: await process_frame
	await app.start_game()
	for place in ["CAMP","LIVING"]:
		await app._mount_world("OUTDOOR" if place=="CAMP" else place,Catalog.position_for(place,Vector2(.45,.7)))
		app._resume()
		for i in 8: await physics_frame
		app._pause(true)
		app.stage.set_time(23.0)
		app._update_camera(10)
		await capture(place.to_lower(),.4)
	await app._mount_world("FOREST_TRAIL",Forest.position_for(2,.28))
	app._resume()
	app.forest_encounter.suspended=true
	app._pause(true)
	app.stage.set_time(23.0)
	app.stage.update_player(app.actor.position)
	app._update_camera(10)
	await capture("torches-a",.4)
	await capture("torches-b",1.7)
	await app._mount_world("FOREST_TRAIL",Forest.position_for(6,.42))
	app._resume()
	app.forest_encounter.suspended=true
	app._pause(true)
	app.stage.set_time(23)
	app.stage.update_player(app.actor.position)
	app._update_camera(10)
	await capture("lantern-on",.4)
	app.stage.carried_light=false
	app.stage.update_player(app.actor.position)
	await capture("lantern-off",.4)
	DirAccess.remove_absolute(app.save_path)
	quit()
