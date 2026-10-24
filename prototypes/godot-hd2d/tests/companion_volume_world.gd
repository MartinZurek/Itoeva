extends SceneTree
var checks := 0
var failures := 0
func _initialize() -> void: call_deferred("run")
func check(ok: bool,label: String) -> void:
 checks += 1
 if not ok: failures += 1;push_error(label)
func frames(count: int) -> void:
 for i in count: await physics_frame
func buttons(node: Node) -> Array[Button]:
 var found: Array[Button] = []
 if node is Button: found.append(node)
 for child in node.get_children(): found.append_array(buttons(child))
 return found
func run() -> void:
 var app = load("res://scenes/full_world.tscn").instantiate()
 app.save_path = "user://companion-volume-test.json"
 DirAccess.remove_absolute(app.save_path)
 root.add_child(app)
 var deadline := Time.get_ticks_msec() + 30000
 while app.loading_world and Time.get_ticks_msec() < deadline: await process_frame
 check(not app.loading_world, "Welt geladen")
 if app.loading_world: quit(1);return
 check(not app.mosslyn_volume, "Prototyp ist bei bestehenden Spielstaenden zunaechst aus")
 app.show_settings()
 var toggle: Button
 for button in buttons(app.ui):
  if button.text == "Charaktere 3D: aus": toggle = button
 check(is_instance_valid(toggle),"Schalter in echten Einstellungen")
 if is_instance_valid(toggle): toggle.pressed.emit()
 await process_frame
 check(app.mosslyn_volume,"Schalter aktiviert den Prototyp")
 var saved: Dictionary = JSON.parse_string(FileAccess.get_file_as_string(app.save_path))
 check(saved.settings.mosslyn_volume,"Schalter bleibt im vorhandenen Saveformat gespeichert")
 for species in ["nocti","starlet","wyrmling"]:
  app.selected=species
  await app.start_game()
  await frames(12)
  check(app.actor.volume_enabled and is_instance_valid(app.actor.volume),"Aktivierte Figur: "+species)
  check(app.actor.collider.shape is CapsuleShape3D and app.actor.is_on_floor(),"Boden/Kollision")
  app.actor.position=app.Catalog.position_for("SPORT",Vector2(.3,.60))
  await frames(8)
  app.actor.touch_vector=Vector2(.5,0)
  await frames(35)
  check(app.actor.actual_speed>1.0,"Unveraenderte Steuerung")
  check(app.actor.volume.visible and not app.actor.sprite.visible,"Einbau im echten Spiel")
  if DisplayServer.get_name()!="headless":
   await process_frame;await RenderingServer.frame_post_draw
   DirAccess.make_dir_recursive_absolute("res://captures/companions-volume")
   root.get_texture().get_image().save_png("res://captures/companions-volume/world-%s.png"%species)
  app.actor.stop_input()
  await frames(35)
  check(is_zero_approx(app.actor.volume.motion_weight),"Ruhe nach Eingabestopp")
 app.show_settings()
 for button in buttons(app.ui):
  if button.text=="Charaktere 3D: an":button.pressed.emit();break
 await process_frame
 check(not app.actor.volume_enabled and not app.actor.volume.visible and app.actor.sprite.visible,"Ausschalten funktioniert im laufenden Spiel")
 # Die kleine Saveerweiterung braucht keine Migration und bleibt abschaltbar.
 app.mosslyn_volume=true;app._save_preferences();app.mosslyn_volume=false;app._load_save()
 check(app.mosslyn_volume,"Neuladen behaelt die Einstellung")
 app.queue_free();await process_frame
 DirAccess.remove_absolute("user://companion-volume-test.json")
 print("Companion volume world: %s checks, %s failures"%[checks,failures]);quit(1 if failures else 0)
