extends SceneTree
const Forest=preload("res://scripts/forest_catalog.gd")
var app: Node3D
func _initialize() -> void:call_deferred("run")
func grab(label: String) -> void:
 for i in 4:await process_frame
 await RenderingServer.frame_post_draw
 root.get_texture().get_image().save_png("res://captures/v062-"+label+".png")
func run() -> void:
 DirAccess.make_dir_recursive_absolute("res://captures");DirAccess.make_dir_recursive_absolute("user://godot-tests")
 app=load("res://scenes/full_world.tscn").instantiate();app.save_path="user://godot-tests/v062-capture.json"
 DirAccess.remove_absolute(app.save_path);root.add_child(app)
 while app.loading_world:await process_frame
 app.show_deck();await process_frame;app.deck_editor.preview("spark");await grab("card-detail")
 app.deck_editor.render();await grab("deck-return")
 await app.start_game();await app._mount_world("FOREST_TRAIL",Forest.position_for(5,.44))
 var enc: Node=app.forest_encounter
 enc.model.begin();enc.model.advance_intro(3.2);enc.resume()
 enc.model.enemy_health=5;enc.model.decks[0].hand=["spark"];enc._card_play(0,"enemy")
 enc.set_process(false);enc.view.set_process(false)
 for moment in [[.15,"attack-before"],[enc.model.impact_time()+.08,"attack-hit"],[enc.model.impact_time()+.61,"attack-fall"]]:
  enc.view.event_time=moment[0];enc.view.refresh();enc.view.queue_redraw();await grab(moment[1])
 enc._process(enc.busy+.1)
 for moment in [[.0,"victory-start"],[.7,"victory-jump"],[1.3,"victory-xp"],[2.6,"victory-loot"],[4.0,"victory-complete"]]:
  enc.view.result_time=moment[0];enc.view._update_results()
  for meter in enc.view.experience_bars:meter._process(float(moment[0]))
  enc.view.input_cooldown=maxf(0,.25-float(moment[0]));enc.view.refresh()
  enc.view.queue_redraw();await grab(moment[1])
 for species in ["fennec","starlet"]:
  app.selected=species;app.save_data["forest_encounters"]={}
  await app._mount_world("FOREST_TRAIL",Forest.position_for(6,.4));app.forest_encounter.model.phase="done";app.forest_encounter.suspended=true;app._resume();app._pause(true)
  app.stage.set_time(23)
  for i in 120:app.actor._update_occlusion()
  app.stage.update_player(app.actor.position);app._update_camera(10)
  await grab(species+"-night")
 app.selected="starlet";await app._mount_world("OUTDOOR",app.Catalog.position_for("PARK",Vector2(.4,.7)))
 app._resume();app._pause(true);app.stage.set_time(12)
 for i in 120:app.actor._update_occlusion()
 app._update_camera(10);await grab("starlet-day")
 app.stage.set_time(23)
 for i in 120:app.actor._update_occlusion()
 await grab("starlet-outdoor-night")
 app.queue_free();await process_frame;DirAccess.remove_absolute("user://godot-tests/v062-capture.json");quit()
