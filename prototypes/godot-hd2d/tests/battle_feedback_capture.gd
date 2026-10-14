extends SceneTree
const Forest=preload("res://scripts/forest_catalog.gd")
var app: Node3D
func _initialize() -> void:call_deferred("run")
func shot(label: String) -> void:
	for i in 4:await process_frame
	await RenderingServer.frame_post_draw
	root.get_texture().get_image().save_png("res://captures/v061-"+label+".png")
func run() -> void:
	DirAccess.make_dir_recursive_absolute("res://captures")
	DirAccess.make_dir_recursive_absolute("user://godot-tests")
	app=load("res://scenes/full_world.tscn").instantiate()
	app.save_path="user://godot-tests/audio-capture.json";DirAccess.remove_absolute(app.save_path)
	root.add_child(app)
	while app.loading_world:await process_frame
	await app.start_game();await app._mount_world("FOREST_TRAIL",Forest.position_for(5,.44))
	var enc=app.forest_encounter
	enc.model.begin();enc.model.advance_intro(3.2);enc.resume();enc.set_process(false)
	var m=enc.model;m.cursor=0;m.round=6;m._begin_turn(0)
	var a: int=m._add_minion(0,"lantern_snail")
	var b: int=m._add_minion(0,"lightling")
	var c: int=m._add_minion(0,"coal_guard")
	m.find_unit(b).ready=true;m.find_unit(c).spent=true
	for id in ["breezeling","leaf_beetle","dewling"]:m._add_minion(1,id)
	m._add_minion(2,"thornling");m._add_minion(2,"thornling")
	m.decks[0].hand=["trail_blessing","frost_thread","moon_guard","spark","forest_memory"]
	m.decks[0].cap=6;m.decks[0].crystals=2
	enc.view.build_commands();enc.view.refresh();await shot("overview")
	enc.view.selected_card=1;enc.view.refresh();await shot("targets")
	enc.view.selected_card=-1;enc.view.selected_unit=b;enc.view.refresh();await shot("attack-ready")
	app.show_audio_settings();await shot("sound-settings")
	app.show_settings();await shot("world-settings")
	app.queue_free();await process_frame;DirAccess.remove_absolute("user://godot-tests/audio-capture.json")
	quit()
