extends SceneTree
const Catalog=preload("res://scripts/world_catalog.gd")
const Enemies=preload("res://scripts/regional_enemies.gd")
const Rules=preload("res://scripts/ecology_rules.gd")
var app: Node3D
func _initialize() -> void:call_deferred("run")
func grab(label: String) -> void:
 if is_instance_valid(app.forest_encounter) and is_instance_valid(app.forest_encounter.view):app.forest_encounter.view.queue_redraw()
 for i in 3:await process_frame
 await RenderingServer.frame_post_draw
 root.get_texture().get_image().save_png("res://captures/v090-"+label+".png")
func effect(enc,label: String) -> void:
 enc.model.cursor=2;enc.model.playback_pending=false;enc.model.enemy_act();enc._play_event();enc.persist()
 enc.suspended=true;enc.set_process(false);enc.view.set_process(false)
 for i in 10:
  enc.view.event_time=i*.2;enc.view.clock=i*.2;enc.view.refresh();await grab(label+"-"+str(i))
 enc.model.acknowledge_animation();enc.view.event_time=-1;enc.view.build_commands();enc.view.refresh()
func run() -> void:
 DirAccess.make_dir_recursive_absolute("res://captures");DirAccess.make_dir_recursive_absolute("user://godot-tests")
 app=load("res://scenes/full_world.tscn").instantiate();app.save_path="user://godot-tests/v090-capture.json";DirAccess.remove_absolute(app.save_path);root.add_child(app)
 while app.loading_world:await process_frame
 app.world_clock.utc_now_override=1792220400;app.world_clock.utc_offset=0;await app.start_game()
 var ids: Array=["tide_crab"] if "--shore" in OS.get_cmdline_user_args() else ["tide_crab","resin_beetle","cinder_carrier","vharok"]
 for id in ids:
  var profile: Dictionary=Enemies.data()[id]
  await app._mount_world("OUTDOOR" if not Catalog.region_for(profile.place).is_empty() else profile.place,Catalog.position_for(profile.place,Vector2(.5,.6)));app._resume()
  for i in 30:await physics_frame
  app._pause(true);app._update_camera(10);await grab(id+"-habitat")
  if id in ["cinder_carrier","vharok"]:
   app.stage.set_time(22);app.stage.update_character_light(app.actor,.5);app._update_regional_art();await grab(id+"-night")
   app.stage.set_time(12)
  if "--shore" in OS.get_cmdline_user_args():continue
  app._show_enemy(id);await grab(id+"-inspect")
  app.menu_buttons[0].pressed.emit();app.forest_encounter.model.advance_intro(4)
  var enc=app.forest_encounter;enc.view.build_commands();enc.view.refresh();await grab(id+"-battle")
  await effect(enc,id+"-prepare");await effect(enc,id+"-attack")
  if id=="vharok":
   var hits: Array[Dictionary]=[]
   for unit in enc.model.units(2).duplicate():enc.model._damage_target(0,"u:"+str(unit.uid),10,hits)
   enc.view.refresh();await grab("vharok-core")
   enc.model.enemy_health=90;Rules.after_damage(enc.model);enc.model.party[0].barrier=40
   await effect(enc,"vharok-warning");await effect(enc,"vharok-storm")
   enc.model.enemy_health=0;enc.model.phase="victory";enc.model.playback_pending=false;enc.model.claim_victory();enc.view.refresh();await grab("vharok-victory")
  enc.suspended=false;enc.set_process(true);enc._retreat_now()
 app.show_map();await grab("atlas")
 app.queue_free();await process_frame;DirAccess.remove_absolute("user://godot-tests/v090-capture.json")
 print("Ecology dynamic render v090 complete");quit()
