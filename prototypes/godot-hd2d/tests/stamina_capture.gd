extends SceneTree
const Catalog=preload("res://scripts/world_catalog.gd")
var app: Node3D
func _initialize() -> void:call_deferred("run")
func grab(label: String) -> void:
 for i in 3:await process_frame
 await RenderingServer.frame_post_draw
 root.get_texture().get_image().save_png("res://captures/stamina-"+label+".png")
func run() -> void:
 DirAccess.make_dir_recursive_absolute("res://captures");DirAccess.make_dir_recursive_absolute("user://godot-tests")
 app=load("res://scenes/full_world.tscn").instantiate();app.save_path="user://godot-tests/stamina-capture.json"
 DirAccess.remove_absolute(app.save_path);root.add_child(app)
 while app.loading_world:await process_frame
 for species in Catalog.STARTERS:
  app.selected=species;await app._mount_world("OUTDOOR",Catalog.position_for("SPORT",Vector2(.45,.65)))
  app._resume();app.stage.set_time(12);app.zoom=5.7;app._update_camera(10)
  for creature in app.actors:creature.enabled=false
  app.actor.jump_clock=-1;app.actor.state="idle";app.actor.recovery_clock=1
  for mode in (["walk","run"] if species in Catalog.STARTERS else ["walk"]):
   app.actor.running=mode=="run";app.actor.run_blend=1.0 if mode=="run" else 0.0
   app.actor.hover=.48 if mode=="run" and species=="nocti" else 0.0;app.actor.facing=Vector2.RIGHT
   for phase in ([2,5] if species in Catalog.STARTERS else [2]):
    app.actor.clock=(phase+.1)/(8*app.actor.FLIGHT_CYCLES);app.actor.gait=(phase+.1)/8
    app.actor._update_frame(true);app.actor._update_shadow();await grab(species+"-"+mode+"-"+str(phase))
 app.selected="nocti";await app._mount_world("OUTDOOR",Catalog.position_for("STREET",Vector2(.55,.65)));app._resume()
 for creature in app.actors:creature.enabled=false
 app.actor.stamina.value=0;app.actor.stamina.exhausted=true;app.actor.hover=0;app.actor.running=false;app.actor.run_blend=0;app.actor.gait=.3
 app.actor._update_frame(true);app.actor._update_shadow();await grab("exhausted")
 var bench: Dictionary={}
 for item in app.stage.furniture:
  if item.id=="park-bench":bench=item
 app.actor.enabled=true;app.actor.stop_input();app.actor.state="idle"
 app.actor.position=app.stage.safe_position(bench.approach_position,app.actor.collider.shape.radius)
 for i in 15:await physics_frame
 await app.interact_with("seat","park-bench");app.actor.enabled=false
 app.actor.stamina.value=36;app.actor.stamina.exhausted=false;app.actor._update_frame(false);app._update_camera(10);await grab("bench")
 app.queue_free();await process_frame;DirAccess.remove_absolute("user://godot-tests/stamina-capture.json")
 print("Ausdauer und Gehfolgen im echten Weltbild gespeichert");quit()
