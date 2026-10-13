extends SceneTree
const Forest=preload("res://scripts/forest_catalog.gd")
var app: Node3D
func _initialize() -> void:call_deferred("run")
func clip(duration: float, animated: bool=true) -> void:
 for i in int(duration*30):
  if is_instance_valid(app.forest_encounter) and is_instance_valid(app.forest_encounter.view):
   var view: Control=app.forest_encounter.view
   if animated:view.event_time=i/30.0
   view.clock=i/30.0;view.refresh();view.queue_redraw()
  await process_frame
func mount(species: String) -> void:
 app.selected=species;app.save_data["forest_encounters"]={}
 await app._mount_world("FOREST_TRAIL",Forest.position_for(5,.44))
 var enc: Node=app.forest_encounter
 enc.model.begin();enc.model.advance_intro(3.2);enc.resume();enc.set_process(false)
func power(action: String) -> void:
 var enc: Node=app.forest_encounter
 enc.model.decks[enc.model.cursor].power=false;enc.model.act(action);enc.view.play_event()
 await clip(enc.model.animation_duration())
 enc.model.acknowledge_animation();enc.view.build_commands();enc.view.refresh()
func run() -> void:
 DirAccess.make_dir_recursive_absolute("user://godot-tests")
 app=load("res://scenes/full_world.tscn").instantiate();app.save_path="user://godot-tests/card-movie.json"
 DirAccess.remove_absolute(app.save_path);root.add_child(app)
 while app.loading_world:await process_frame
 print("MOVIE_START_FRAME ",Engine.get_process_frames())
 app.show_deck();await clip(1.6,false)
 await app.start_game();await mount("fennec")
 var enc: Node=app.forest_encounter
 enc.model.round=3;enc.model.decks[0].cap=3;enc.model.decks[0].crystals=3
 enc.model.decks[0].hand=["cat","boar","wolf","spark","leaf_guard"]
 enc.view.build_commands();enc.view.refresh();await clip(1.5,false)
 for id in ["cat","gorilla"]:
  if id=="gorilla":enc.model.round+=1;enc.model._begin_turn(0)
  for seed in range(1,100):
   enc.model.set_random_seed(seed)
   if enc.model.roll_wildlife().id==id:enc.model.set_random_seed(seed);break
  await power("skill")
 var uid: int=enc.model.board[-1].uid
 enc.model.find_unit(uid).ready=true
 enc.model.attack_minion(uid,"enemy");enc.view.play_event();await clip(1.6);enc.model.acknowledge_animation()
 enc.model.play_card(1);enc.view.play_event();await clip(1.5);enc.model.acknowledge_animation()
 enc.model.cursor=1;enc.model._begin_turn(1);enc.view.build_commands();await power("skill");await power("support")
 enc.model.party[0].barrier=0;enc.model.party[1].barrier=0
 enc.model.cursor=2;enc.model.round=4;enc.model._begin_turn(2);enc.model.enemy_act();enc.view.play_event();await clip(3.2);enc.model.acknowledge_animation()
 enc.model.cursor=0;enc.model.enemy_health=1;enc.model.decks[0].hand=["spark"];enc.model.decks[0].crystals=1
 enc.model.play_card(0,"enemy");enc.view.play_event();await clip(1.5);enc.model.acknowledge_animation()
 enc.view.build_commands();await clip(2,false)
 app.queue_free();await process_frame;DirAccess.remove_absolute("user://godot-tests/card-movie.json");quit()
