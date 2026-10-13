extends SceneTree
const Forest = preload("res://scripts/forest_catalog.gd")
var checks := 0
var failures := 0
func _initialize() -> void: call_deferred("run")
func check(ok: bool, label: String) -> void:
 checks += 1
 if not ok: failures += 1; push_error(label)
func frames(count: int) -> void:
 for i in count: await process_frame
func button(app: Node, action: String) -> Button:
 for b in app.menu_buttons:
  if is_instance_valid(b) and b.get_meta("battle_action", "") == action: return b
 return null
func tap(b: Button) -> void:
 if b == null: check(false,"Ergebnisbutton fehlt"); return
 var e := InputEventScreenTouch.new()
 e.index = 2; e.position = b.get_global_rect().get_center(); e.pressed = true
 root.push_input(e,true); root.push_input(e,true)
 e.pressed = false; root.push_input(e,true)
func run() -> void:
 var app: Node3D = load("res://scenes/full_world.tscn").instantiate()
 DirAccess.make_dir_recursive_absolute("user://godot-tests")
 app.save_path = "user://godot-tests/victory-flow.json"
 DirAccess.remove_absolute(app.save_path)
 root.add_child(app)
 while app.loading_world: await process_frame
 await app.start_game()
 for species in app.Catalog.data().characters:
  app.selected = species; app.save_data["forest_encounters"] = {}
  await app._mount_world("FOREST_TRAIL",Forest.position_for(5,.44))
  var enc: Node = app.forest_encounter
  enc.model.begin(); enc.model.advance_intro(3.2); enc.resume()
  await frames(3)
  enc.model.enemy_health = 1
  enc.model.decks[0].hand=["spark"]
  enc._card_play(0,"enemy")
  check(enc.model.phase == "victory" and enc.model.playback_pending,"Letzter Treffer " + species)
  check(button(app,"finish").disabled,"Weiter während Treffer gesperrt")
  enc._process(1.8)
  await frames(3)
  check(enc.view.results_shown,"Siegesbildschirm vorhanden " + species)
  check(enc.model.experience[species] == 120 and enc.model.inventory.potion == 1,"XP und Beute einmal vergeben " + species)
  enc.view._process(.3); enc.view.refresh()
  check(not button(app,"finish").disabled,"Ergebnis freigegeben " + species)
  var receipt: Dictionary = enc.model.snapshot()
  app.show_menu(); await frames(2)
  app._resume(); await frames(2)
  check(enc.view.results_shown and enc.model.snapshot() == receipt,"Menü erhält Siegesergebnis " + species)
  app.save_data = JSON.parse_string(FileAccess.get_file_as_string(app.save_path))
  await app._mount_world("FOREST_TRAIL",Forest.position_for(5,.44))
  app._resume(); await frames(2)
  enc = app.forest_encounter
  check(enc.view.results_shown and enc.model.experience[species] == 120 and enc.model.inventory.potion == 1,"Gespeicherten Sieg ohne zweite Belohnung fortsetzen " + species)
  enc.view._process(.3); enc.view.refresh()
  tap(button(app,"finish")); await frames(5)
  check(enc.model.phase == "done" and not app.paused and app.actor.enabled,"Rückkehr mit freier Steuerung " + species)
  check(not enc.active() and enc.suspended and enc.view == null and is_instance_valid(app.hud),"Kampf abgeschlossen " + species)
  enc.finish(); enc._process(2.0)
  check(enc.model.experience[species] == 120 and not app.paused,"Doppelter Abschluss bleibt sicher " + species)
  enc.replay(); await frames(2)
  check(enc.model.phase == "intro" and enc.model.experience[species] == 120 and enc.model.inventory.potion == 1,"Wiederholung erhält Fortschritt " + species)
  enc.model.advance_intro(3.2); enc.busy = 0; enc.model.enemy_health = 1
  enc.model.decks[0].hand=["spark"]
  enc._card_play(0,"enemy"); enc._process(1.8)
  check(enc.model.experience[species] == 240 and enc.model.inventory.potion == 2,"Zweiter tatsächlicher Sieg vergibt einmal " + species)
  enc.view._process(.3); enc.finish(); await frames(2)
 app.queue_free(); await frames(2)
 DirAccess.remove_absolute("user://godot-tests/victory-flow.json")
 print("Siegesablauf: %s Checks, %s Fehler" % [checks,failures])
 quit(1 if failures else 0)
