extends SceneTree
const Forest=preload("res://scripts/forest_catalog.gd")
const Cards=preload("res://scripts/card_catalog.gd")
var checks:=0
var failures:=0
func _initialize() -> void:call_deferred("run")
func check(ok: bool,label: String) -> void:
 checks+=1
 if not ok:failures+=1;push_error(label)
func frames(count: int) -> void:
 for i in count:await process_frame
func button(app: Node,key: String,uid: int=-1) -> Button:
 for b in app.menu_buttons:
  if not is_instance_valid(b) or b.get_meta("battle_action","")!=key:continue
  if uid<0 or int(b.get_meta("unit_uid",-1))==uid:return b
 return null
func tap(b: Button) -> void:
 if b==null:check(false,"Touchbutton fehlt");return
 var e:=InputEventScreenTouch.new();e.index=2;e.position=b.get_global_rect().get_center();e.pressed=true
 root.push_input(e,true);root.push_input(e,true);e.pressed=false;root.push_input(e,true)
func ready(enc: Node) -> void:
 enc._process(enc.busy+.01);enc.view.event_time=-1;enc.view.input_cooldown=0;enc.view.refresh()
func run() -> void:
 var app: Node3D=load("res://scenes/full_world.tscn").instantiate()
 DirAccess.make_dir_recursive_absolute("user://godot-tests")
 app.save_path="user://godot-tests/minion-ui.json";DirAccess.remove_absolute(app.save_path);root.add_child(app)
 while app.loading_world:await process_frame
 await app.start_game()
 for species in ["mosslyn","nocti"]:
  app.selected=species;app.save_data["forest_encounters"]={}
  await app._mount_world("FOREST_TRAIL",Forest.position_for(5,.44))
  var enc: Node=app.forest_encounter
  enc.model.begin();enc.model.advance_intro(3.2);enc.resume();await frames(3)
  var ally: int=enc.model._add_minion(0,Cards.SPECIAL[species][1]);enc.model.find_unit(ally).ready=true
  var foe: int=enc.model._add_minion(2,"thornling")
  enc.view.build_commands();enc.view.refresh();tap(button(app,"unit",ally));await frames(1)
  check(enc.view.unit_info.visible and enc.view.unit_info_stats.text.contains("Angriff 4") and enc.view.selected_unit==ally,"Minion antippen zeigt Werte und wählt Angreifer "+species)
  enc.view.input_cooldown=0;enc.view.refresh();tap(button(app,"enemy_target"));await frames(1)
  check(enc.model.turn==0 and enc.model.enemy_health==220,"Schutzhelfer blockiert Hauptcharakter als Ziel "+species)
  tap(button(app,"unit",foe));await frames(1)
  check(enc.model.turn==1 and enc.model.find_unit(foe).hp==5 and enc.model.playback_pending,"Minion gegen Minion durch Touch genau einmal "+species)
  check(enc.model.last_event.attacker_uid==ally and enc.model.last_event.before_board.size()==2,"Angriff besitzt Figuren- und Trefferanimation")
  ready(enc)
  enc.model.decks[0].hand=["spark"];enc.model.decks[0].crystals=1
  enc.view.build_commands();enc.view.refresh();tap(button(app,"card"));await frames(1)
  enc.view.input_cooldown=0;enc.view.refresh();tap(button(app,"unit",foe));await frames(1);ready(enc)
  check(enc.model.find_unit(foe).is_empty(),"Schutzhelfer gezielt besiegt "+species)
  enc.model.find_unit(ally).ready=true;enc.view.refresh();tap(button(app,"unit",ally));await frames(1)
  enc.view.input_cooldown=0;enc.view.refresh();tap(button(app,"enemy_target"));await frames(1)
  check(enc.model.enemy_health==216 and enc.model.playback_pending,"Minion greift danach Hauptcharakter an "+species)
  ready(enc);enc.model.party[0].hp-=15;enc.model.special_charges[0]=5;enc.view.refresh();tap(button(app,"special"));await frames(1)
  check(enc.model.special_used[0] and enc.model.special_charges[0]==0 and enc.model.playback_pending,"Spezialkraft durch Doppel-Touch einmal "+species)
  var state: Dictionary=enc.model.snapshot();app.show_menu();await frames(2);app._resume();await frames(2)
  check(enc.model.snapshot()==state,"Menü erhält neue Minion- und Spezialzustände "+species)
  app.save_data=JSON.parse_string(FileAccess.get_file_as_string(app.save_path))
  await app._mount_world("FOREST_TRAIL",Forest.position_for(5,.44));app._resume();await frames(2);enc=app.forest_encounter
  check(enc.model.special_used[0] and enc.model.playback_pending and enc.model.board.size()==state.board.size(),"Neustart erhält Spezialkraft und Arenaminions "+species)
  ready(enc);enc.model.decks[0].hand=["leaf_guard",Cards.ULTIMATES[species]];enc.view.build_commands();enc.view.refresh()
  var hand: Array=enc.model.decks[0].hand.duplicate();tap(button(app,"card_info"));await frames(1)
  check(is_instance_valid(enc.view.card_popup) and button(app,"end_turn").disabled,"Kartendetails sperren Aktionen im Hintergrund "+species)
  tap(button(app,"card_info_next"));await frames(1)
  check(enc.view.card_popup.card.id==Cards.ULTIMATES[species] and enc.model.decks[0].hand==hand,"Karten blättern kostet nichts "+species)
  tap(button(app,"card_info_close"));await frames(1);enc.view.input_cooldown=0;enc.view.refresh()
  check(not is_instance_valid(enc.view.card_popup) and not button(app,"end_turn").disabled,"Detailansicht schließt zur spielbaren Arena "+species)
  enc.suspend()
 app.queue_free();await frames(3);DirAccess.remove_absolute("user://godot-tests/minion-ui.json")
 print("Minion-Touch: %s Checks, %s Fehler" % [checks,failures]);quit(1 if failures else 0)
