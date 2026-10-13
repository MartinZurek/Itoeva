extends SceneTree
const Forest=preload("res://scripts/forest_catalog.gd")
var app: Node3D
func _initialize() -> void:call_deferred("run")
func shot(name: String) -> void:
 for i in 4:await process_frame
 await RenderingServer.frame_post_draw
 root.get_texture().get_image().save_png("res://captures/v040-"+name+".png")
func run() -> void:
 DirAccess.make_dir_recursive_absolute("res://captures")
 DirAccess.make_dir_recursive_absolute("user://godot-tests")
 app=load("res://scenes/full_world.tscn").instantiate();app.save_path="user://godot-tests/card-capture.json"
 DirAccess.remove_absolute(app.save_path);root.add_child(app)
 while app.loading_world:await process_frame
 app.show_deck();await shot("deck")
 await app.start_game()
 await app._mount_world("FOREST_TRAIL",Forest.position_for(5,.44))
 var enc: Node=app.forest_encounter
 enc.model.begin();enc.model.advance_intro(3.2);enc.resume();enc.set_process(false)
 enc.model.decks[0].hand=["cat","boar","wolf","spark","leaf_guard"]
 enc.model.round=3;enc.model.decks[0].cap=3;enc.model.decks[0].crystals=3
 enc.model._add_minion(0,"cat");enc.model._add_minion(0,"wolf");enc.model._add_minion(1,"star_guard");enc.model._add_minion(2,"thornling")
 enc.model._begin_turn(0);enc.view.build_commands();enc.view.refresh();await shot("cards")
 enc.model.cursor=1;enc.model.act("skill");enc.view.play_event();enc.view.event_time=1;enc.view.refresh();await shot("beam")
 enc.model.acknowledge_animation();enc.model.decks[1].power=false;enc.model.act("support");enc.view.play_event();enc.view.event_time=1;enc.view.refresh();await shot("barrier")
 enc.model.acknowledge_animation();enc.model.cursor=0;enc.model.enemy_health=1;enc.model.decks[0].hand=["spark"]
 enc.model.play_card(0,"enemy");enc.model.acknowledge_animation();enc.view.build_commands();enc.view._process(1.3);enc.view.refresh();await shot("victory")
 app.queue_free();await process_frame;DirAccess.remove_absolute("user://godot-tests/card-capture.json");quit()
