extends SceneTree
const Forest=preload("res://scripts/forest_catalog.gd")
const Cards=preload("res://scripts/card_catalog.gd")
const Editor=preload("res://scripts/deck_editor.gd")
const Art=preload("res://scripts/card_assets.gd")
var checks:=0
var failures:=0
var app: Node3D
func _initialize() -> void:call_deferred("run")
func check(ok: bool,label: String) -> void:
 checks+=1
 if not ok:failures+=1;push_error(label)
func frames(count: int=3) -> void:
 for i in count:await process_frame
func button(action: String,uid: int=-1,index: int=-1) -> Button:
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
func shot(label: String) -> void:
 if "--capture" not in OS.get_cmdline_user_args():return
 if app.forest_encounter!=null:app.forest_encounter.view.queue_redraw()
 await frames();await RenderingServer.frame_post_draw
 if app.forest_encounter!=null:
  var v=app.forest_encounter.view
  if label=="kampf":
   check(v.pawn_cursor>=6,"Drei Hauptfiguren und drei Helfer tatsächlich gezeichnet")
 DirAccess.make_dir_recursive_absolute("res://captures")
 root.get_texture().get_image().save_png("res://captures/strategy-"+label+".png")
func run() -> void:
 app=load("res://scenes/full_world.tscn").instantiate()
 DirAccess.make_dir_recursive_absolute("user://godot-tests")
 app.save_path="user://godot-tests/strategy-ui.json";DirAccess.remove_absolute(app.save_path);root.add_child(app)
 while app.loading_world:await process_frame
 var images: Dictionary={}
 for id in Cards.Strategy.ids():
  var c:=Cards.get_card(id);var art:=Art.texture(c)
  check(art!=null and art.atlas!=null and art.region.size.x>0,"Bild geladen "+id)
  images[c.art_asset]=true
 check(images.size()==153,"153 Arten-/Formbilder vorhanden")
 var editor=Editor.new();editor.app=app;editor.open();await frames()
 var available:=editor.filtered_ids().size()
 check(available>616,"Neue und bestehende Karten im Editor")
 editor._filter("Helfer");editor.filter_volk="Hortbrut";editor.render();await frames()
 for id in editor.filtered_ids():check(Cards.get_card(id).get("volk","")=="Hortbrut","Volksfilter genau")
 editor._search("Schatzkammer");await frames()
 check(not editor.filtered_ids().is_empty(),"Fähigkeitssuche mit Volksfilter")
 editor.filter_volk="Alle Völker";editor._search("");editor._page(1);await frames()
 check(editor.page==1,"Katalog umblättern")
 for style in ["Tempo","Bollwerk","Motor"]:
  editor._preset(style);await frames()
  check(Cards.valid("fennec",editor.draft),"Deckvorschlag spielbar "+style)
  check(not app.save_data.get("card_decks",{}).has("fennec"),"Vorschlag ohne Autospeichern")
 await shot("deckeditor")
 var viewport:=Rect2(Vector2.ZERO,root.get_visible_rect().size)
 for b in app.menu_buttons:
  if is_instance_valid(b):
   var rect: Rect2=b.get_global_rect()
   var inside: bool=rect.position.x>=0 and rect.end.x<=viewport.end.x if editor.scroll.is_ancestor_of(b) else viewport.encloses(rect)
   check(inside,"Editorbedienelement im Bild / horizontal im Scrollbereich: "+b.text)
 var mythical: String=""
 for id in Cards.Strategy.ids():
  if Cards.get_card(id).get("art_asset","").ends_with("gemalt-hortdrache.png"):mythical=id;break
 check(not mythical.is_empty(),"Gemalte mythische Karte im Katalog")
 editor.preview(mythical);await frames();await shot("mythisch-detail")
 for b in app.menu_buttons:
  if is_instance_valid(b) and b.text=="Schließen":check(viewport.encloses(b.get_global_rect()),"Detail schließbar");b.pressed.emit();break
 await frames();editor._preset("Tempo");editor.commit();await frames()
 check(app.save_data.card_decks.fennec==editor.draft,"Neues Deck gespeichert")
 await app.start_game();await app._mount_world("FOREST_TRAIL",Forest.position_for(5,.44))
 var enc=app.forest_encounter;enc.model.begin();enc.model.advance_intro(3.2);enc.resume();await frames()
 var m=enc.model;var v=enc.view
 enc.set_process(false);v.set_process(false)
 m.board.clear();m.round=7;m._begin_turn(0)
 var ally: int=m._add_minion(0,"cat");m._add_minion(1,mythical);m._add_minion(2,"thornling")
 var nourish: String=""
 for id in Cards.Strategy.ids():
  if Cards.get_card(id).ability=="Baukunst":nourish=id;break
 m.decks[0].hand=[nourish,mythical];m.decks[0].cap=10;m.decks[0].crystals=10
 v.event_time=-1;v.build_commands();v.refresh();await frames();await shot("kampf")
 var hp: int=m.find_unit(ally).max_hp
 tap(button("card",-1,0));await frames();check(v.selected_card==0,"Helfer wartet auf eigenes Ziel")
 v.input_cooldown=0;v.refresh();tap(button("enemy_target"));await frames();check(m.turn==0,"Gegner kein Baukunstziel")
 v.input_cooldown=0;v.refresh();tap(button("unit",ally));await frames()
 check(m.turn==1 and m.find_unit(ally).max_hp>hp,"Baukunst per Doppeltouch genau einmal")
 m.acknowledge_animation();enc.busy=0;v.input_cooldown=0;m.decks[0].crystals=10
 check(m.play_card(0),"Mythischer Helfer spielbar");v.play_event();v.event_time=.7;v.clock=.7;v.refresh();await shot("mythisch-ritual")
 check(m.animation_duration()==2.6 and m.impact_time()==1.0,"Mythischer Auftritt mit eigener Ereigniszeit")
 enc.suspend()
 for player in app.find_children("*","AudioStreamPlayer",true,false):player.stop()
 editor=null;app.queue_free();await frames(4);app=null
 await create_timer(.1).timeout
 DirAccess.remove_absolute("user://godot-tests/strategy-ui.json")
 print("Strategie-UI: %s Checks, %s Fehler" % [checks,failures]);quit(1 if failures else 0)
