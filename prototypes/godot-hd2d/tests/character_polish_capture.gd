extends SceneTree
const Catalog=preload("res://scripts/world_catalog.gd")
const Forest=preload("res://scripts/forest_catalog.gd")
var app: Node3D
func _initialize() -> void:call_deferred("run")
func grab(label: String) -> void:
 for i in 3:await process_frame
 await RenderingServer.frame_post_draw
 root.get_texture().get_image().save_png("res://captures/character-polish-"+label+".png")
func run() -> void:
 DirAccess.make_dir_recursive_absolute("res://captures");DirAccess.make_dir_recursive_absolute("user://godot-tests")
 app=load("res://scenes/full_world.tscn").instantiate();app.save_path="user://godot-tests/character-polish-capture.json"
 DirAccess.remove_absolute(app.save_path);root.add_child(app)
 while app.loading_world:await process_frame
 await grab("menu")
 for species in ([] if "--battle-only" in OS.get_cmdline_user_args() else Catalog.STARTERS):
  app.selected=species;await app._mount_world("OUTDOOR",Catalog.position_for("PARK",Vector2(.5,.65)))
  app._resume();app._pause(true);app.stage.set_time(12);app.zoom=5.7;app._update_camera(10)
  app.actor.facing=Vector2.RIGHT;app.actor.recovery_clock=1;app.actor._update_frame(false)
  await grab(species+"-world")
  if species in ["nocti","mosslyn"]:
   for direction in [Vector2.RIGHT,Vector2.DOWN,Vector2.UP]:
    app.actor.facing=direction;app.actor.running=true;app.actor.run_blend=1;app.actor.hover=.48 if species=="nocti" else 0
    var cycle_span: float=4.0 if species=="mosslyn" and absf(direction.x)>absf(direction.y) else 1.0
    for phase in 8:
     app.actor.clock=(phase+.1)/(8*3.2);app.actor.gait=cycle_span*(phase+.1)/8
     app.actor._update_frame(true);app.actor._update_shadow()
     await grab(species+"-run-"+str(int(direction.y))+"-"+str(phase))
  if species=="starlet":
   app.stage.set_time(23)
   for i in 120:app.actor._update_occlusion()
   await grab("starlet-night")
  if species=="fennec":
   app._show_conversation("fennec",app.Residents.appointment("fennec",10));await grab("fennec-dialog")
 app.selected="wyrmling";await app._mount_world("FOREST_TRAIL",Forest.position_for(5,.44))
 var enc=app.forest_encounter;enc.model.begin();enc.model.advance_intro(4);enc.resume();enc.set_process(false);enc.view.set_process(false)
 var model=enc.model;var view=enc.view
 for entry in [["phoenix","wyrmling"],["ancient","mosslyn"],["mirror","nocti"],["stormfalcon","puffling"]]:
  model.board.clear();model.playback_pending=false;model.cursor=0;model.phase="battle"
  var uid: int=model._add_minion(0,"special_"+entry[0],{"name":entry[0],"species":entry[1],"form":entry[0],"ready":true,"attack":2,"health":20})
  model.attack_minion(uid,"enemy");view.play_event()
  for time in [.3,.65,1.25,1.85,2.5]:
   view.event_time=time;view.refresh();view.queue_redraw();await grab("helper-"+entry[0]+"-"+str(time))
  model.acknowledge_animation()
 app.queue_free();await process_frame;DirAccess.remove_absolute("user://godot-tests/character-polish-capture.json");print("OpenGL-Spielbilder gespeichert");quit()
