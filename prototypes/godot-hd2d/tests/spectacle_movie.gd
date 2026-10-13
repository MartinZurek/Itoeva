extends SceneTree
const Forest=preload("res://scripts/forest_catalog.gd")
var app: Node3D
func _initialize() -> void:call_deferred("run")
func mount(species: String) -> void:
 app.selected=species;app.save_data["forest_encounters"]={}
 await app._mount_world("FOREST_TRAIL",Forest.position_for(5,.44))
 var enc: Node=app.forest_encounter
 enc.model.begin();enc.model.advance_intro(3.2);enc.resume();enc.suspended=true
func clip(duration: float) -> void:
 var view: Control=app.forest_encounter.view
 for i in int(duration*30):
  view.event_time=i/30.0;view.clock=i/30.0
  view.refresh();view.queue_redraw()
  await process_frame
func act(command: String) -> void:
 var enc: Node=app.forest_encounter
 enc.model.playback_pending=false;enc.model.cursor=0;enc.model.enemy_health=136
 enc.model.party[0].mp=32;enc.model.act(command);enc.view.play_event()
func run() -> void:
 DirAccess.make_dir_recursive_absolute("user://godot-tests")
 app=load("res://scenes/full_world.tscn").instantiate()
 app.save_path="user://godot-tests/v030-movie.json";DirAccess.remove_absolute(app.save_path)
 root.add_child(app)
 while app.loading_world:await process_frame
 await app.start_game()
 await mount("fennec")
 var enc: Node=app.forest_encounter
 for id in ["cat","wolf","gorilla"]:
  for seed in range(1,100):
   enc.model.set_random_seed(seed)
   if enc.model.roll_wildlife().id==id:
    enc.model.set_random_seed(seed);break
  act("skill");await clip(3.4)
 await mount("starlet");act("skill");await clip(2.6)
 act("support");await clip(2.4)
 enc=app.forest_encounter
 enc.model.playback_pending=false;enc.model.cursor=2;enc.model.round=2;enc.model.enemy_act();enc.view.play_event();await clip(2.15)
 enc.model.playback_pending=false;enc.model.party[0].barrier=0;enc.model.party[1].barrier=0
 enc.model.cursor=2;enc.model.round=4;enc.model.enemy_act();enc.view.play_event();await clip(3.2)
 enc.model.acknowledge_animation();enc.model.cursor=0;enc.view.build_commands()
 await clip(.8)
 app.queue_free();await process_frame;DirAccess.remove_absolute("user://godot-tests/v030-movie.json")
 quit()
