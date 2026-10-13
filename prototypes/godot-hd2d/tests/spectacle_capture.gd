extends SceneTree
const Forest=preload("res://scripts/forest_catalog.gd")
var app: Node3D
func _initialize() -> void:call_deferred("run")
func capture(label: String,t: float) -> void:
 var enc: Node=app.forest_encounter
 enc.view.event_time=t;enc.view.refresh();enc.view.queue_redraw()
 for i in 3:await process_frame
 await RenderingServer.frame_post_draw
 root.get_texture().get_image().save_png("res://captures/v030-"+label+".png")
func mount(species: String) -> void:
 app.selected=species;app.save_data["forest_encounters"]={}
 await app._mount_world("FOREST_TRAIL",Forest.position_for(5,.44))
 var enc: Node=app.forest_encounter
 enc.model.begin();enc.model.advance_intro(3.2);enc.resume();enc.suspended=true
func action(command: String) -> void:
 var enc: Node=app.forest_encounter
 enc.model.playback_pending=false;enc.model.cursor=0;enc.busy=0
 enc.model.act(command);enc.view.play_event()
func run() -> void:
 DirAccess.make_dir_recursive_absolute("res://captures")
 DirAccess.make_dir_recursive_absolute("user://godot-tests")
 app=load("res://scenes/full_world.tscn").instantiate()
 app.save_path="user://godot-tests/v030-capture.json";DirAccess.remove_absolute(app.save_path)
 root.add_child(app)
 while app.loading_world:await process_frame
 await app.start_game()
 await mount("fennec")
 var enc: Node=app.forest_encounter
 for seed in [2,4,8,12,20,25,31,40,51,60,79,96]:
  enc.model.set_random_seed(seed);enc.model.enemy_health=136;enc.model.party[0].mp=24
  action("skill")
  await capture("summon-"+str(enc.model.last_event.summon.id),1.9)
 await mount("starlet");action("skill")
 await capture("beam-charge",.6);await capture("beam-hit",1.16)
 enc=app.forest_encounter;enc.model.party[0].mp=32
 action("support");await capture("barrier-cast",1.05)
 enc.model.acknowledge_animation();enc.model.cursor=2;enc.model.round=1;enc.model.enemy_act();enc.view.play_event()
 await capture("barrier-hit",.7)
 enc.model.playback_pending=false;enc.model.party[0].barrier=0;enc.model.party[1].barrier=0
 enc.model.cursor=2;enc.model.round=2;enc.model.enemy_act();enc.view.play_event()
 await capture("wood-shards",.75)
 enc.model.playback_pending=false;enc.model.cursor=2;enc.model.round=4;enc.model.enemy_act();enc.view.play_event()
 await capture("roots-cast",1.05);await capture("roots-bound",1.65)
 enc.model.acknowledge_animation();enc.model.cursor=0;enc.view.build_commands();await capture("roots-response",4)
 app.queue_free();await process_frame
 DirAccess.remove_absolute("user://godot-tests/v030-capture.json")
 quit()
