extends SceneTree
const Catalog = preload("res://scripts/world_catalog.gd")
var app: Node3D
func _initialize() -> void: call_deferred("run")
func wait(seconds: float) -> void: await create_timer(seconds).timeout
func mount(place: String, hour: int) -> void:
	await app._mount_world("OUTDOOR" if not Catalog.region_for(place).is_empty() else place, Catalog.position_for(place,Vector2(0.5,0.75)))
	app._resume()
	app.world_clock.utc_now_override=Time.get_unix_time_from_datetime_dict({"year":2026,"month":1,"day":2,"hour":hour-1,"minute":0,"second":0})
	app.world_clock.reset_to_real_time()
	app._apply_world_time()
	app.notice_clock = 0.0
	app.notice = ""
	app._update_camera(10.0)
func run() -> void:
	DirAccess.make_dir_recursive_absolute("res://captures")
	app=load("res://scenes/full_world.tscn").instantiate()
	app.save_path="user://godot-tests/discovery-video.json"
	root.add_child(app)
	while app.loading_world: await process_frame
	await mount("PARK",12)
	var hootlet: CharacterBody3D
	for resident in app.actors:
		if resident.species=="hootlet": hootlet=resident
	app.actor.position=app.stage.safe_position(hootlet.position + Vector3(4.8,0,0),app.actor.collider.shape.radius)
	app._update_camera(10.0)
	await wait(.5)
	var file:=FileAccess.open("res://captures/v026-video-ready",FileAccess.WRITE)
	file.store_string("ready");file.close()
	while not FileAccess.file_exists("res://captures/v026-video-start"): await process_frame
	await wait(.8)
	app.request_interaction("talk","hootlet")
	for i in 500:
		if app.dialog_open: break
		await physics_frame
	await wait(1.2)
	if app.dialog_open:
		app.menu_buttons[0].pressed.emit()
		await wait(1.8)
	app._resume()
	await mount("NOOK",12)
	app.request_interaction("read","nook-armchair")
	for i in 600:
		if app.dialog_open: break
		await physics_frame
	await wait(1.0)
	app._finish_reading()
	await wait(1.6)
	await mount("CAMP",22)
	await wait(2.6)
	await mount("BEACH",12)
	await wait(2.6)
	DirAccess.remove_absolute(app.save_path)
	print("Discovery video complete")
	quit()
