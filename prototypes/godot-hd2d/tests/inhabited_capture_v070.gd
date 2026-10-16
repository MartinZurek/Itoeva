extends SceneTree
const Catalog=preload("res://scripts/world_catalog.gd")
const Forest=preload("res://scripts/forest_catalog.gd")
var app: Node3D
func _initialize() -> void:call_deferred("run")
func grab(label: String) -> void:
 for i in 6:await process_frame
 await RenderingServer.frame_post_draw
 root.get_texture().get_image().save_png("res://captures/v070-"+label+".png")
func run() -> void:
 DirAccess.make_dir_recursive_absolute("res://captures");DirAccess.make_dir_recursive_absolute("user://godot-tests")
 app=load("res://scenes/full_world.tscn").instantiate();app.save_path="user://godot-tests/v070-capture.json";DirAccess.remove_absolute(app.save_path);root.add_child(app)
 while app.loading_world:await process_frame
 app.world_clock.utc_now_override=1792058400;app.world_clock.utc_offset=0
 await app.start_game()
 for place in ["CITY","SUN_GLADE","MIST_WATCH"]:
  await app._mount_world("OUTDOOR" if place=="CITY" else place,Catalog.position_for(place,Vector2(.48,.65)))
  app._resume()
  for i in 35:await physics_frame
  app._pause(true);app._update_camera(10)
  await grab(place.to_lower())
  if place=="SUN_GLADE":
   app._show_conversation("nella",app.Residents.appointment("nella",12));await grab("nella-dialogue");app._resume()
 app.show_map();await process_frame;app.ui.get_child(0)._select("MIST_WATCH");await grab("atlas")
 app._resume();app._show_enemy("ash_hound");await grab("enemy-inspection")
 app.menu_buttons[0].pressed.emit();app.forest_encounter.model.advance_intro(4);app.forest_encounter.view.build_commands();app.forest_encounter.view.refresh();await grab("ash-hound-battle")
 app.queue_free();await process_frame;DirAccess.remove_absolute("user://godot-tests/v070-capture.json");quit()
