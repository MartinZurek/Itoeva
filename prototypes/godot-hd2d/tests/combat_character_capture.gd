extends SceneTree
const Forest=preload("res://scripts/forest_catalog.gd")
var app: Node3D
func _initialize() -> void: call_deferred("run")
func capture(label: String) -> void:
 app.forest_encounter.view.queue_redraw()
 for i in 4: await process_frame
 await RenderingServer.frame_post_draw
 root.get_texture().get_image().save_png("res://captures/v028-"+label+".png")
func run() -> void:
 DirAccess.make_dir_recursive_absolute("res://captures")
 DirAccess.make_dir_recursive_absolute("user://godot-tests")
 app=load("res://scenes/full_world.tscn").instantiate()
 app.save_path="user://godot-tests/combat-char-capture.json"
 DirAccess.remove_absolute(app.save_path)
 root.add_child(app)
 while app.loading_world: await process_frame
 await app.start_game()
 for species in app.Catalog.STARTERS:
  app.selected=species
  app.save_data["forest_encounters"]={}
  await app._mount_world("FOREST_TRAIL",Forest.position_for(5,.44))
  app._resume()
  var enc: Node=app.forest_encounter
  enc.model.begin();enc.model.advance_intro(3.2);enc.resume()
  enc._act("skill");enc.suspended=true;enc.view.event_time=.82
  await capture(species+"-skill")
  enc.model.acknowledge_animation();enc.model.cursor=0;enc.busy=0
  enc.suspended=false;enc._act("support");enc.suspended=true;enc.view.event_time=.82
  await capture(species+"-support")
  enc.model.acknowledge_animation();enc.model.cursor=0;enc.busy=0;enc.model.party[0].limit=100
  enc.suspended=false;enc._act("limit");enc.suspended=true;enc.view.event_time=.82
  await capture(species+"-limit")
  # Defeat is drawn with low/root-collapsed poses, no sleeping/roll frame reuse.
  enc.model.phase="defeat";enc.model.playback_pending=false
  for member in enc.model.party: member.hp=0
  enc.view.event_time=-1;enc.view.build_commands();enc.view.refresh()
  await capture(species+"-defeat")
 DirAccess.remove_absolute(app.save_path)
 quit()
