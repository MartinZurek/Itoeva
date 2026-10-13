extends SceneTree
const Catalog = preload("res://scripts/world_catalog.gd")
var app: Node3D

func _initialize() -> void:
	call_deferred("run")

func mount(species: String) -> void:
	app.selected = species
	await app._mount_world("OUTDOOR",Catalog.position_for("SPORT",Vector2(.22,.65)))
	app._resume()
	for resident in app.actors:
		if resident != app.actor:
			resident.enabled=false
			resident.collider.disabled=true
	for i in 30: await physics_frame
	app._update_camera(10)

func segment(species: String) -> void:
	var start := Time.get_ticks_msec()
	var jumped := false
	var burst := false
	var resting := false
	app.hud.finger=0
	app.hud.origin=Vector2(118,600)
	while Time.get_ticks_msec()-start < 4400:
		var elapsed := float(Time.get_ticks_msec()-start)/1000.0
		app.actor.touch_vector = Vector2(.48 if elapsed<.7 else 1.0,0) if not resting else Vector2.ZERO
		app.hud.thumb=app.hud.origin+app.actor.touch_vector*app.hud.RADIUS
		if elapsed>1.25 and not jumped:
			app.actor.request_action("jump")
			jumped=true
		if elapsed>(1.55 if species=="hootlet" else 2.65) and not burst:
			app.actor.request_action("roll")
			burst=true
		if elapsed>3.55 and not resting:
			app.actor.request_action("sit")
			app.hud.reset_touch()
			resting=true
		await physics_frame

func run() -> void:
	DirAccess.make_dir_recursive_absolute("res://captures")
	app=load("res://scenes/full_world.tscn").instantiate()
	app.save_path="user://godot-tests/movement-capture.json"
	root.add_child(app)
	while app.loading_world: await process_frame
	await mount("fennec")
	await RenderingServer.frame_post_draw
	var ready:=FileAccess.open("res://captures/video-ready",FileAccess.WRITE)
	ready.store_string("ready")
	ready.close()
	while not FileAccess.file_exists("res://captures/video-start"): await process_frame
	await segment("fennec")
	await mount("hootlet")
	await segment("hootlet")
	DirAccess.remove_absolute(app.save_path)
	print("Desktop-Bewegungsaufnahme abgeschlossen")
	quit()
