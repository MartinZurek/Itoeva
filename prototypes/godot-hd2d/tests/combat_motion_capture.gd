extends SceneTree
const Forest=preload("res://scripts/forest_catalog.gd")
const Catalog=preload("res://scripts/world_catalog.gd")
var app: Node3D
func _initialize() -> void:call_deferred("run")
func grab(label: String) -> void:
 var view=app.forest_encounter.view;view.refresh();view.queue_redraw()
 for i in 3:await process_frame
 await RenderingServer.frame_post_draw
 root.get_texture().get_image().save_png("res://captures/combat-motion-"+label+".png")
func run() -> void:
 DirAccess.make_dir_recursive_absolute("res://captures");DirAccess.make_dir_recursive_absolute("user://godot-tests")
 app=load("res://scenes/full_world.tscn").instantiate();app.save_path="user://godot-tests/combat-motion-capture.json";DirAccess.remove_absolute(app.save_path);root.add_child(app)
 while app.loading_world:await process_frame
 for species in ([] if "--boss" in OS.get_cmdline_user_args() else Catalog.STARTERS):
  app.selected=species;app.save_data["forest_encounters"]={}
  await app._mount_world("FOREST_TRAIL",Forest.position_for(5,.44))
  var enc=app.forest_encounter;enc.model.begin();enc.model.advance_intro(4);enc.resume();enc.set_process(false);enc.view.set_process(false)
  var b=enc.model;var v=enc.view;b.board.clear()
  var uid: int=b._add_minion(0,"wolf",{"name":"Gefährte","species":species,"attack":4,"health":30})
  b._add_minion(0,"wolf");b._add_minion(0,"boar")
  b._add_minion(1,"wolf");b._add_minion(1,"boar")
  b._add_minion(1,"wolf",{"name":"Flughelfer","form":"stormfalcon","attack":3,"health":20})
  var target: int=b._add_minion(2,"wolf",{"name":"Wächter","attack":2,"health":40,"species":"enemy","taunt":true})
  b._add_minion(2,"wolf");b._add_minion(2,"boar")
  v.event_time=-1;v.clock=1.3;v.build_commands();await grab(species+"-formation")
  b.find_unit(uid).ready=true;b.cursor=0;b.playback_pending=false
  if not b.attack_minion(uid,"u:"+str(target)):push_error("Render-Angriff nicht gestartet: %s phase=%s cursor=%s unit=%s legal=%s"%[species,b.phase,b.cursor,b.find_unit(uid),b.legal_targets(0)]);quit(1);return
  v.play_event()
  for i in 7:
   v.event_time=[0.0,.30,.65,1.10,1.25,1.85,2.70][i];v.clock=1.3+v.event_time
   await grab(species+"-attack-"+str(i))
  b.acknowledge_animation();enc.suspended=true
 # Regionaler Boss bleibt mit seiner größten Pose unterhalb der Statusanzeige.
 app.selected="fennec";app.save_data["forest_encounters"]={}
 await app._mount_world("SEAL_COURT",Catalog.position_for("SEAL_COURT",Vector2(.4,.6)));app._resume()
 app._show_enemy("vharok");app.menu_buttons[0].pressed.emit()
 var enc=app.forest_encounter;enc.model.advance_intro(4);enc.set_process(false);enc.view.set_process(false)
 enc.view.event_time=-1;enc.view.build_commands();await grab("vharok-formation")
 enc.model.cursor=2;enc.model.playback_pending=false;enc.model.enemy_act();enc.view.play_event();enc.view.event_time=enc.model.impact_time();await grab("vharok-impact")
 app.queue_free();await process_frame;DirAccess.remove_absolute("user://godot-tests/combat-motion-capture.json");print("Combat motion render complete");quit()
