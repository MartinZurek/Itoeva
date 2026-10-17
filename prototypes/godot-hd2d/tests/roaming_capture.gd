extends SceneTree
const Catalog=preload("res://scripts/world_catalog.gd")
func _initialize() -> void:call_deferred("run")
func run() -> void:
 DirAccess.make_dir_recursive_absolute("res://captures");DirAccess.make_dir_recursive_absolute("user://godot-tests")
 var app=load("res://scenes/full_world.tscn").instantiate();app.save_path="user://godot-tests/roaming-capture.json";DirAccess.remove_absolute(app.save_path);root.add_child(app)
 while app.loading_world:await process_frame
 app.selected="fennec"
 for place in ["MIST_WATCH","MILL_BROOK","ASH_ASCENT"]:
  await app._mount_world(place,Catalog.position_for(place,Vector2(.5,.5)));app._resume();app.stage.set_time(14)
  for i in 12:await physics_frame
  app._pause(true);app._update_camera(10)
  for i in 3:await process_frame
  await RenderingServer.frame_post_draw
  root.get_texture().get_image().save_png("res://captures/roaming-"+place.to_lower()+".png")
 await app._mount_world("OUTDOOR",Catalog.position_for("SWAMP",Vector2(.48,.5)));app._resume()
 app.stage.set_time(14)
 for i in 12:await physics_frame
 app.actor.touch_vector=Vector2(.8,0)
 for i in 80:await physics_frame
 app._pause(true);app._update_camera(10)
 for i in 3:await process_frame
 await RenderingServer.frame_post_draw
 root.get_texture().get_image().save_png("res://captures/roaming-slope.png")
 app.queue_free();await process_frame;DirAccess.remove_absolute("user://godot-tests/roaming-capture.json")
 print("Roaming visual capture complete");quit()
