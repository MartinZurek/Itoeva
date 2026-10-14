extends SceneTree
const Forest=preload("res://scripts/forest_catalog.gd")
var app: Node3D
func _initialize() -> void:call_deferred("run")
func shot(name: String) -> void:
 for i in 4:await process_frame
 await RenderingServer.frame_post_draw
 root.get_texture().get_image().save_png("res://captures/v060-"+name+".png")
func run() -> void:
 DirAccess.make_dir_recursive_absolute("res://captures")
 DirAccess.make_dir_recursive_absolute("user://godot-tests")
 app=load("res://scenes/full_world.tscn").instantiate();app.save_path="user://godot-tests/v060-capture.json"
 DirAccess.remove_absolute(app.save_path);root.add_child(app)
 while app.loading_world:await process_frame
 app.show_deck();await shot("deck-40")
 await app.start_game();await app._mount_world("FOREST_TRAIL",Forest.position_for(5,.44))
 var enc: Node=app.forest_encounter
 enc.model.begin();enc.model.advance_intro(3.2);enc.resume();enc.set_process(false)
 enc.model.cursor=0;enc.model.round=4;enc.model._begin_turn(0)
 for cards in [["lightling","star_guard","emberling","coal_guard","dewling","spring_guard"],
               ["breezeling","wind_guard","moonling","moon_guard","lantern_snail","leaf_beetle"],
               ["mushroom_archivist","thistle_nurse","mist_glider","acorn_sentinel","sprigling","echolet"]]:
  enc.model.board.clear()
  for i in cards.size():enc.model._add_minion(0 if i<3 else 1,cards[i])
  enc.model.decks[0].hand=["trail_blessing","frost_thread","dew_mend","homeward","forest_memory"]
  enc.view.build_commands();enc.view.event_time=-1;enc.view.refresh()
  await shot("helpers-"+str(cards[0]))
 var uid: int=enc.model.board[2].uid
 enc.model.find_unit(uid).ready=true;enc.model.attack_minion(uid,"enemy")
 enc.view.play_event();enc.view.event_time=enc.model.impact_time();enc.view.refresh();await shot("helper-attack")
 app.show_menu();app.queue_free();await process_frame
 DirAccess.remove_absolute("user://godot-tests/v060-capture.json");quit()
