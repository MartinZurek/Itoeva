extends SceneTree
const Forest=preload("res://scripts/forest_catalog.gd")
var app: Node3D
func _initialize() -> void:call_deferred("run")
func clip(duration: float,animated: bool=true) -> void:
 for i in int(duration*30):
  if is_instance_valid(app.forest_encounter) and is_instance_valid(app.forest_encounter.view):
   var v: Control=app.forest_encounter.view
   if animated:v.event_time=i/30.0
   v.clock=i/30.0;v.refresh();v.queue_redraw()
  await process_frame
func mount(species: String) -> void:
 app.selected=species;app.save_data["forest_encounters"]={}
 await app._mount_world("FOREST_TRAIL",Forest.position_for(5,.44))
 var enc: Node=app.forest_encounter
 enc.model.begin();enc.model.advance_intro(3.2);enc.resume();enc.set_process(false)
func event_clip() -> void:
 var enc: Node=app.forest_encounter
 enc.view.play_event();await clip(enc.model.animation_duration()+.08)
 enc.model.acknowledge_animation();enc.view.event_time=-1;enc.view.build_commands();enc.view.refresh()
func run() -> void:
 DirAccess.make_dir_recursive_absolute("user://godot-tests")
 app=load("res://scenes/full_world.tscn").instantiate();app.save_path="user://godot-tests/minion-movie.json"
 DirAccess.remove_absolute(app.save_path);root.add_child(app)
 while app.loading_world:await process_frame
 print("MOVIE_START_FRAME ",Engine.get_process_frames())
 await clip(1.2,false);await app.start_game();await mount("mosslyn")
 var enc: Node=app.forest_encounter
 enc.model.round=2;enc.model._begin_turn(0);enc.model.decks[0].hand=["sprigling","spark","ancient"]
 enc.model.play_card(0);await event_clip()
 var ally: int=enc.model.units(0)[0].uid
 enc.view.input_cooldown=0;enc.view._pick_unit(ally);await clip(1.1,false)
 var foe: int=enc.model._add_minion(2,"thornling")
 enc.model.find_unit(ally).ready=true;enc.view.input_cooldown=0;enc.view._pick_unit(ally)
 enc.model.attack_minion(ally,"u:"+str(foe));await event_clip()
 enc.model.play_card(0,"u:"+str(foe));await event_clip()
 enc.model.special_charges[0]=5;enc.model.act("special");await event_clip()
 enc.model.round=3;enc.model._begin_turn(0);ally=enc.model.units(0)[-1].uid
 enc.view.input_cooldown=0;enc.view._pick_unit(ally);await clip(.8,false)
 enc.model.attack_minion(ally,"enemy");await event_clip()
 await mount("nocti");enc=app.forest_encounter
 enc.model.decks[0].hand=["echolet","mirror"];enc.model.play_card(0);await event_clip()
 enc.model.special_charges[0]=5;enc.model.act("special");await event_clip()
 ally=enc.model.units(0)[-1].uid;enc.view.input_cooldown=0;enc.view._pick_unit(ally);await clip(1.3,false)
 await mount("wyrmling");enc=app.forest_encounter
 enc.model.round=5;enc.model._begin_turn(0);enc.model.decks[0].hand=["phoenix"]
 enc.model.play_card(0);await event_clip();await clip(.8,false)
 await mount("fennec");enc=app.forest_encounter
 enc.model.special_charges[0]=5;enc.model.set_random_seed(3);enc.model.act("special");await event_clip()
 ally=enc.model.units(0)[0].uid;enc.view.input_cooldown=0;enc.view._pick_unit(ally);await clip(1.2,false)
 app.show_menu();app.selected="nocti";app.show_deck();await clip(1.5,false)
 app.queue_free();await process_frame;DirAccess.remove_absolute("user://godot-tests/minion-movie.json");quit()
