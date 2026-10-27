extends SceneTree
const Catalog = preload("res://scripts/world_catalog.gd")
var id := "ash_hound"
func _initialize() -> void: call_deferred("run")
func run() -> void:
	if OS.get_environment("NPC_ID") != "": id=OS.get_environment("NPC_ID")
	var out := "res://captures/npc_"+id
	DirAccess.make_dir_recursive_absolute(out)
	DirAccess.make_dir_recursive_absolute("user://godot-tests")
	var app = load("res://scenes/full_world.tscn").instantiate()
	app.save_path="user://godot-tests/npc-motion-capture.json"
	DirAccess.remove_absolute(app.save_path); root.add_child(app)
	while app.loading_world: await process_frame
	var profile: Dictionary=app.Enemies.data()[id]
	await app._mount_world(profile.place,Catalog.position_for(profile.place,Vector2(.03,.5)))
	app._resume(); app.canvas.visible=false; app.actor.enabled=false
	app.set_process(false)
	app.actor.position=Vector3(0,.03,20)
	var marker: Dictionary={}
	for entry in app.regional_markers:
		if entry.id==id: marker=entry
	marker.life_clock=1.0
	var report: Array=[]
	for frame in 360:
		if frame==150: marker.life_clock=float(profile.period)*.62
		app.actor.position=marker.position+Vector3.RIGHT*(1.1 if frame>=285 else 2.5)
		await physics_frame
		app._update_regional_art()
		app.stage.update_visible(marker.position.x)
		app._update_camera(10.0)
		await RenderingServer.frame_post_draw
		root.get_texture().get_image().save_png(out+"/%04d.png"%frame)
		var screen: Vector2=app.camera.unproject_position(marker.position)
		report.append({"frame":frame,"position":[marker.position.x,marker.position.y,marker.position.z],"mode":marker.motion.mode,"phase":marker.motion.phase,"pixel_size":marker.art.pixel_size,"offset_y":marker.art.offset.y,"flip_h":marker.art.flip_h,"screen":[screen.x,screen.y]})
	var file:=FileAccess.open(out+"/telemetry.json",FileAccess.WRITE)
	file.store_string(JSON.stringify({"id":id,"fps":30,"frames":report},"\t")+"\n");file.close()
	DirAccess.remove_absolute(app.save_path)
	print("NPC capture: ",id," 360 frames")
	quit()
