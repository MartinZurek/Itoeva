extends SceneTree
const Forest=preload("res://scripts/forest_catalog.gd")
const Editor=preload("res://scripts/deck_editor.gd")
var checks:=0
var failures:=0
func _initialize() -> void:call_deferred("run")
func check(ok: bool,label: String) -> void:
 checks+=1
 if not ok:failures+=1;push_error(label)
func frames(count: int) -> void:
 for i in count:await process_frame
func button(app: Node,action: String) -> Button:
 for b in app.menu_buttons:
  if is_instance_valid(b) and b.get_meta("battle_action","")==action:return b
 return null
func tap(b: Button) -> void:
 if b==null:check(false,"Touchbutton fehlt");return
 var e:=InputEventScreenTouch.new();e.index=2;e.position=b.get_global_rect().get_center();e.pressed=true
 root.push_input(e,true);root.push_input(e,true);e.pressed=false;root.push_input(e,true)
func run() -> void:
 var app: Node3D=load("res://scenes/full_world.tscn").instantiate()
 DirAccess.make_dir_recursive_absolute("user://godot-tests")
 app.save_path="user://godot-tests/card-ui.json";DirAccess.remove_absolute(app.save_path)
 root.add_child(app)
 while app.loading_world:await process_frame
 await app.start_game()
 for species in app.Catalog.data().characters:
  app.selected=species;app.save_data["forest_encounters"]={}
  await app._mount_world("FOREST_TRAIL",Forest.position_for(5,.44))
  var enc: Node=app.forest_encounter
  enc.model.begin();enc.model.advance_intro(3.2);enc.resume();await frames(3)
  enc.model.decks[0].hand=["spark","leaf_guard"];enc.view.build_commands();enc.view.refresh()
  tap(button(app,"card"));await frames(1)
  check(enc.view.selected_card==0 and enc.model.turn==0,"Karte braucht Ziel "+species)
  enc.view.input_cooldown=0;enc.view.refresh();tap(button(app,"enemy_target"));await frames(1)
  check(enc.model.enemy_health==213 and enc.model.decks[0].crystals==0 and enc.model.playback_pending,"Doppel-Touch löst Karte einmal aus "+species)
  check(button(app,"end_turn").disabled,"Animation sperrt Zugende")
  var snapshot: Dictionary=enc.model.snapshot()
  app.show_menu();await frames(2);app._resume();await frames(2)
  check(enc.model.snapshot()==snapshot and enc.model.playback_pending,"Menü erhält Kartenereignis "+species)
  app.save_data=JSON.parse_string(FileAccess.get_file_as_string(app.save_path))
  await app._mount_world("FOREST_TRAIL",Forest.position_for(5,.44));app._resume();await frames(2);enc=app.forest_encounter
  check(enc.model.enemy_health==213 and enc.model.decks[0].hand==["leaf_guard"] and enc.model.decks[0].crystals==0,"Gespeicherte Kosten und Hand erhalten "+species)
  enc._process(enc.busy+.01);enc.view.input_cooldown=0;enc.view.refresh()
  tap(button(app,"skill"));await frames(1)
  check(enc.model.decks[0].power and enc.model.cursor==0,"Kostenlose Superkraft behält eigenen Zug "+species)
  enc._process(enc.busy+.01);enc.view.input_cooldown=0;enc.view.refresh()
  check(button(app,"skill").disabled and button(app,"support").disabled,"Superkraft einmal je Zug "+species)
  tap(button(app,"end_turn"));await frames(1);enc._process(enc.busy+.01)
  check(enc.model.cursor==1 and enc.model.decks[1].crystals==1,"Begleiter übernimmt mit eigenem Deck "+species)
  enc.suspend()
 app.show_menu();await frames(2)
 var editor=Editor.new();editor.app=app;editor.species="fennec";editor.open();await frames(2)
 editor.change("tea",-1);check(editor.draft.size()==39,"Deckänderung im Entwurf")
 check(not app.save_data.get("card_decks",{}).has("fennec"),"Entwurf noch nicht gespeichert")
 editor.change("tea",1);editor.commit();await frames(2)
 check(app.save_data.card_decks.fennec.size()==40,"Eigenes Deck speichern")
 var saved: Dictionary=JSON.parse_string(FileAccess.get_file_as_string(app.save_path))
 check(saved.card_decks.fennec.size()==40,"Deck dauerhaft lesbar")
 var b=preload("res://scripts/card_battle.gd").new();b.configure("fennec");b.prepare(saved.card_decks)
 check(b.decks[0].draw.size()+b.decks[0].hand.size()==40,"Gespeichertes Deck im nächsten Kampf")
 app.queue_free();await frames(3);DirAccess.remove_absolute("user://godot-tests/card-ui.json")
 print("Karten-UI: %s Checks, %s Fehler" % [checks,failures]);quit(1 if failures else 0)
