extends SceneTree
const Catalog = preload("res://scripts/world_catalog.gd")
var id := "ash_hound"
func _initialize() -> void: call_deferred("run")
func run() -> void:
	if OS.get_environment("NPC_ID") != "": id=OS.get_environment("NPC_ID")
	var out := "res://captures/npc_"+id
	if OS.get_environment("NPC_CAPTURE_RUN") != "": out += "_"+OS.get_environment("NPC_CAPTURE_RUN")
	DirAccess.make_dir_recursive_absolute(out)
	DirAccess.make_dir_recursive_absolute("user://godot-tests")
	var app = load("res://scenes/full_world.tscn").instantiate()
	app.save_path="user://godot-tests/npc-motion-capture.json"
	DirAccess.remove_absolute(app.save_path); root.add_child(app)
	while app.loading_world: await process_frame
	var profile: Dictionary=app.Enemies.data()[id]
	if id in ["bark_guardian", "glut_imp"]: app.selected="mosslyn"
	await app._mount_world(profile.place,Catalog.position_for(profile.place,Vector2(.03,.5)))
	app._resume(); app.canvas.visible=false; app.actor.enabled=false
	app.set_process(false)
	app.actor.position=Vector3(0,.03,20)
	var marker: Dictionary={}
	for entry in app.regional_markers:
		if entry.id==id: marker=entry
	marker.life_clock=1.0
	var fast := OS.get_environment("NPC_CAPTURE_FAST") == "1"
	var two_stops := id in ["bark_guardian", "glut_imp"]
	var count := 600 if two_stops else (240 if fast else 360)
	var turn := 270 if two_stops else (75 if fast else 150)
	var stop := 120 if two_stops else (180 if fast else 285)
	# Beide Stopps enden vor dem Revierziel und belegen damit Wiederanlaufen.
	var stop_windows: Array = [[120,195],[405,480]] if two_stops else [[stop,count]]
	var report: Array=[]
	for frame in count:
		if frame==turn: marker.life_clock=float(profile.period)*.62
		var stopped := false
		for window in stop_windows:
			if frame >= window[0] and frame < window[1]: stopped=true
		app.actor.position=marker.position+Vector3.RIGHT*(1.1 if stopped else 2.5)
		await physics_frame
		app._update_regional_art()
		app.stage.update_visible(marker.position.x)
		app._update_camera(10.0)
		await RenderingServer.frame_post_draw
		root.get_texture().get_image().save_png(out+"/%04d.png"%frame)
		var screen: Vector2=app.camera.unproject_position(marker.position)
		var contour_height: float = float(marker.motion.cycles[marker.motion.mode].bounds[marker.motion.phase][3]) * marker.art.pixel_size
		var comparison_height: float = float(app.actor.displayed_bounds.size.y) * app.actor.sprite.pixel_size
		report.append({"frame":frame,"position":[marker.position.x,marker.position.y,marker.position.z],"mode":marker.motion.mode,"phase":marker.motion.phase,"pixel_size":marker.art.pixel_size,"offset_y":marker.art.offset.y,"flip_h":marker.art.flip_h,"screen":[screen.x,screen.y],"contour_height_ratio":contour_height/comparison_height,"shadow_scale":marker.shadow.scale.x})
	var file:=FileAccess.open(out+"/telemetry.json",FileAccess.WRITE)
	file.store_string(JSON.stringify({"id":id,"comparison_species":app.actor.species,"fps":30,"stop_frame":stop,"stop_windows":stop_windows,"turn_frame":turn,"frames":report},"\t")+"\n");file.close()
	DirAccess.remove_absolute(app.save_path)
	print("NPC capture: ",id," ",count," frames")
	quit()
