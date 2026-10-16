extends SceneTree
const Catalog=preload("res://scripts/world_catalog.gd")
var app: Node3D
func _initialize() -> void:call_deferred("run")
func grab(label: String) -> void:
 if is_instance_valid(app.forest_encounter) and is_instance_valid(app.forest_encounter.view):app.forest_encounter.view.queue_redraw()
 for i in 6:await process_frame
 await RenderingServer.frame_post_draw
 root.get_texture().get_image().save_png("res://captures/v080-"+label+".png")
func run() -> void:
 DirAccess.make_dir_recursive_absolute("res://captures");DirAccess.make_dir_recursive_absolute("user://godot-tests")
 app=load("res://scenes/full_world.tscn").instantiate();app.save_path="user://godot-tests/v080-capture.json";DirAccess.remove_absolute(app.save_path);root.add_child(app)
 while app.loading_world:await process_frame
 app.world_clock.utc_now_override=1792058400;app.world_clock.utc_offset=0;await app.start_game()
 await app._mount_world("MIST_WATCH",Catalog.position_for("MIST_WATCH",Vector2(.5,.6)));app._resume()
 for i in 25:await physics_frame
 app._pause(true);app._update_camera(10);await grab("nebelwacht")
 for id in ["glut_imp","seal_weaver"]:
  app._show_enemy(id);await grab(id+"-inspect")
  app.menu_buttons[0].pressed.emit();app.forest_encounter.model.advance_intro(4)
  var enc=app.forest_encounter;enc.view.build_commands();enc.view.refresh();await grab(id+"-battle")
  enc.model.cursor=2;enc.model.enemy_act();enc._play_event();enc.persist()
  enc.suspended=true;enc.view.event_time=1.05;await grab(id+"-cast")
  enc.model.acknowledge_animation();enc.view.event_time=-1;enc.view.build_commands();enc.view.refresh();await grab(id+"-prepared")
  enc.suspended=false;enc._retreat_now()
 for species in Catalog.data().characters:
  app.selected=species;await app._mount_world("MILL_BROOK",Catalog.position_for("MILL_BROOK",Vector2(.55,.6)));app._resume()
  for i in 20:await physics_frame
  app._pause(true);app._update_camera(10);await grab("hero-"+str(species))
 app.queue_free();await process_frame;DirAccess.remove_absolute("user://godot-tests/v080-capture.json")
 print("Demon/hero render v080 complete");quit()
