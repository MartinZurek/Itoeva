extends SceneTree
const Forest = preload("res://scripts/forest_catalog.gd")
func _initialize() -> void: call_deferred("run")
func run() -> void:
 DirAccess.make_dir_recursive_absolute("res://captures")
 DirAccess.make_dir_recursive_absolute("user://godot-tests")
 var app: Node3D = load("res://scenes/full_world.tscn").instantiate()
 app.save_path = "user://godot-tests/v029-capture.json"
 DirAccess.remove_absolute(app.save_path)
 root.add_child(app)
 while app.loading_world: await process_frame
 await app.start_game()
 for species in ["fennec","gloop","hootlet"]:
  app.selected = species; app.save_data["forest_encounters"] = {}
  await app._mount_world("FOREST_TRAIL",Forest.position_for(5,.44))
  var enc: Node = app.forest_encounter
  enc.model.begin(); enc.model.advance_intro(3.2); enc.resume()
  enc.model.enemy_health = 1; enc._act("strike"); enc._process(1.8)
  enc.view._process(1.2); enc.view.refresh(); enc.suspended = true
  for i in 4: await process_frame
  await RenderingServer.frame_post_draw
  root.get_texture().get_image().save_png("res://captures/v029-victory-"+species+".png")
 app.queue_free(); await process_frame
 DirAccess.remove_absolute("user://godot-tests/v029-capture.json")
 quit()
