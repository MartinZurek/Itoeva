extends SceneTree
const Forest=preload("res://scripts/forest_catalog.gd")
var app: Node3D
func _initialize() -> void: call_deferred("run")
func frames(count: int) -> void:
 for i in count: await process_frame
func run() -> void:
 DirAccess.make_dir_recursive_absolute("user://godot-tests")
 app=load("res://scenes/full_world.tscn").instantiate()
 app.save_path="user://godot-tests/combat-movie.json"
 DirAccess.remove_absolute(app.save_path)
 root.add_child(app)
 while app.loading_world: await process_frame
 await app.start_game()
 await app._mount_world("FOREST_TRAIL",Forest.position_for(4,.4))
 app._resume();app._pause(true);app.forest_encounter.suspended=true;app.stage.set_time(12)
 app._update_camera(10);app.stage.update_player(app.actor.position)
 print("MOVIE_START_FRAME:",Engine.get_process_frames())
 await frames(75)
 for species in app.Catalog.STARTERS:
  app.selected=species;app.save_data["forest_encounters"]={}
  await app._mount_world("FOREST_TRAIL",Forest.position_for(5,.44))
  var enc: Node=app.forest_encounter
  enc.model.begin();enc.model.advance_intro(3.2);enc.resume()
  await frames(15)
  enc._act("skill")
  await frames(53)
  enc.model.cursor=0;enc.busy=0;enc.model.acknowledge_animation()
  enc._act("support")
  await frames(53)
  enc.model.cursor=2;enc.busy=0;enc.model.acknowledge_animation()
  await frames(55)
  enc.suspend()
 print("MOVIE_END_FRAME:",Engine.get_process_frames())
 DirAccess.remove_absolute(app.save_path)
 quit()
