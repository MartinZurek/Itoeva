extends SceneTree
const Forest=preload("res://scripts/forest_catalog.gd")
var app: Node3D
func _initialize() -> void: call_deferred("run")
func capture(label: String) -> void:
 if is_instance_valid(app.forest_encounter.view): app.forest_encounter.view.queue_redraw()
 for i in 4: await process_frame
 await RenderingServer.frame_post_draw
 root.get_texture().get_image().save_png("res://captures/v028-"+label+".png")
func run() -> void:
 DirAccess.make_dir_recursive_absolute("res://captures")
 DirAccess.make_dir_recursive_absolute("user://godot-tests")
 app=load("res://scenes/full_world.tscn").instantiate()
 app.save_path="user://godot-tests/battle-capture.json"
 DirAccess.remove_absolute(app.save_path)
 root.add_child(app)
 while app.loading_world: await process_frame
 await app.start_game()
 await app._mount_world("FOREST_TRAIL",Forest.position_for(5,.44))
 app._resume()
 var enc: Node=app.forest_encounter
 enc.model.begin();enc.model.advance_intro(3.2);enc.resume();enc.suspended=true
 await capture("battle-ready")
 enc.view.choose("magic")
 await capture("battle-magic")
 enc.suspended=false
 enc._act("skill")
 enc.suspended=true;enc.view.event_time=.82
 await capture("wildruf")
 enc.model.acknowledge_animation();enc.busy=0
 enc.view.build_commands();enc.view.refresh()
 enc.suspended=false;enc._act("skill");enc.suspended=true;enc.view.event_time=.82
 await capture("starlight")
 enc.model.acknowledge_animation();enc.busy=0;enc.model.enemy_act();enc.view.play_event();enc.view.event_time=.76
 await capture("enemy-strike")
 enc.model.acknowledge_animation();enc.busy=0;enc.model.phase="victory";enc.model.enemy_health=0;enc.view.event_time=2.2;enc.view.build_commands();enc.view.refresh()
 await capture("victory")
 enc.finish()
 for index in [2,4]:
  await app._mount_world("FOREST_TRAIL",Forest.position_for(index,.40))
  app._resume();app._pause(true);app.forest_encounter.suspended=true;app.stage.set_time(12)
  app._update_camera(10);app.stage.update_player(app.actor.position)
  await capture("forest-"+str(index))
 DirAccess.remove_absolute(app.save_path)
 quit()
