extends SceneTree
const Forest=preload("res://scripts/forest_catalog.gd")
const Cards=preload("res://scripts/card_catalog.gd")
const Editor=preload("res://scripts/deck_editor.gd")
var checks:=0
var failures:=0
func _initialize() -> void:call_deferred("run")
func check(ok: bool,label: String) -> void:
 checks+=1
 if not ok:failures+=1;push_error(label)
func frames(count: int) -> void:
 for i in count:await process_frame
func find_button(app: Node,action: String,uid: int=-1,index: int=-1) -> Button:
 for b in app.menu_buttons:
  if not is_instance_valid(b) or b.get_meta("battle_action","")!=action:continue
  if uid>=0 and int(b.get_meta("unit_uid",-1))!=uid:continue
  if index>=0 and int(b.get_meta("hand_index",-1))!=index:continue
  return b
 return null
func tap(b: Button) -> void:
 if b==null:check(false,"Touchziel fehlt");return
 var e:=InputEventScreenTouch.new();e.index=2;e.position=b.get_global_rect().get_center();e.pressed=true
 root.push_input(e,true);root.push_input(e,true);e.pressed=false;root.push_input(e,true)
func run() -> void:
 var app: Node3D=load("res://scenes/full_world.tscn").instantiate()
 DirAccess.make_dir_recursive_absolute("user://godot-tests")
 app.save_path="user://godot-tests/expanded-ui.json";DirAccess.remove_absolute(app.save_path);root.add_child(app)
 while app.loading_world:await process_frame
 var editor=Editor.new();editor.app=app;editor.open();await frames(2)
 check(editor.draft.size()==40,"Deckeditor zeigt 40 Karten")
 editor.change("spark",-1);await frames(2)
 var save: Button
 for b in app.menu_buttons:
  if b.text=="Deck speichern":save=b
 check(editor.draft.size()==39 and save.disabled,"39 Karten sperren Speichern")
 editor.change("spark",1);editor.commit()
 check(app.save_data.card_decks.fennec.size()==40,"40 Karten werden gespeichert")
 await app.start_game();await app._mount_world("FOREST_TRAIL",Forest.position_for(5,.44))
 var enc: Node=app.forest_encounter
 enc.model.begin();enc.model.advance_intro(3.2);enc.resume();await frames(2)
 var ally: int=enc.model._add_minion(0,"lantern_snail")
 var foe: int=enc.model._add_minion(2,"thornling")
 enc.model.decks[0].hand=["trail_blessing","frost_thread"]
 enc.model.decks[0].cap=6;enc.model.decks[0].crystals=6
 enc.view.build_commands();enc.view.refresh()
 tap(find_button(app,"card",-1,0));await frames(1)
 check(enc.view.selected_card==0,"Helferverstaerkung wartet auf Ziel")
 enc.view.input_cooldown=0;enc.view.refresh();tap(find_button(app,"unit",foe));await frames(1)
 check(enc.model.decks[0].crystals==6 and enc.model.turn==0,"Gegner ist kein Verstaerkungsziel")
 enc.view.input_cooldown=0;enc.view.refresh();tap(find_button(app,"unit",ally));await frames(1)
 check(enc.model.turn==1 and enc.model.find_unit(ally).attack==4 and enc.model.find_unit(ally).max_hp==11,"Eigenen Helfer antippen verstaerkt genau einmal")
 enc._process(enc.busy+.01);enc.view.event_time=-1;enc.view.input_cooldown=0
 enc.view.build_commands();enc.view.refresh();tap(find_button(app,"card",-1,0));await frames(1)
 enc.view.input_cooldown=0;enc.view.refresh();tap(find_button(app,"unit",ally));await frames(1)
 check(enc.model.turn==1 and enc.model.find_unit(ally).frozen==0,"Eigener Helfer kein Frostziel")
 enc.view.input_cooldown=0;enc.view.refresh();tap(find_button(app,"unit",foe));await frames(1)
 check(enc.model.turn==2 and enc.model.find_unit(foe).frozen==1 and enc.model.find_unit(foe).hp==6,"Gegnerhelfer per Touch festhalten")
 app.queue_free();await frames(3);DirAccess.remove_absolute("user://godot-tests/expanded-ui.json")
 print("40-Karten-Touch: %s Checks, %s Fehler" % [checks,failures]);quit(1 if failures else 0)
