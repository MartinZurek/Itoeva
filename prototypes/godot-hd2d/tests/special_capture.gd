extends SceneTree
const Forest=preload("res://scripts/forest_catalog.gd")
const Cards=preload("res://scripts/card_catalog.gd")
const Editor=preload("res://scripts/deck_editor.gd")
var app: Node3D
var checks:=0
var failures:=0
func _initialize() -> void:call_deferred("run")
func check(ok: bool,label: String) -> void:
 checks+=1
 if not ok:failures+=1;push_error(label)
func frames(count: int) -> void:
 for i in count:await process_frame
func shot(name: String) -> void:
 await frames(3);await RenderingServer.frame_post_draw
 root.get_texture().get_image().save_png("res://captures/v050-"+name+".png")
func mount(species: String) -> void:
 app.selected=species;app.save_data["forest_encounters"]={}
 await app._mount_world("FOREST_TRAIL",Forest.position_for(5,.44))
 var enc: Node=app.forest_encounter
 enc.model.begin();enc.model.advance_intro(3.2);enc.resume();enc.set_process(false)
func run() -> void:
 DirAccess.make_dir_recursive_absolute("res://captures");DirAccess.make_dir_recursive_absolute("user://godot-tests")
 app=load("res://scenes/full_world.tscn").instantiate();app.save_path="user://godot-tests/special-capture.json"
 DirAccess.remove_absolute(app.save_path);root.add_child(app)
 while app.loading_world:await process_frame
 await shot("menu")
 check(app.portrait_cards.size()==8,"Alle acht Wesen im Menü")
 await app.start_game()
 for species in Cards.ULTIMATES:
  await mount(species)
  var enc: Node=app.forest_encounter
  enc.model.round=5;enc.model.decks[0].cap=5;enc.model.decks[0].crystals=5
  var ids: Array=Cards.available(species)
  for page in 2:
   enc.model.decks[0].hand=ids.slice(page*5,mini(ids.size(),page*5+5));enc.view.build_commands();enc.view.refresh()
   await shot("hand-"+species+"-"+str(page))
   for b in enc.view.buttons:
    if b.get_meta("battle_action","")!="card":continue
    check(Rect2(0,0,1280,720).encloses(b.get_global_rect()),"Karte bleibt im Bildschirm "+species)
    for child in b.get_children():
     if child is Label:
      check(b.get_global_rect().encloses(child.get_global_rect()),"Kartenfeld bleibt in Karte %s: %s %s" % [species,child.text,child.get_rect()])
      if child.get_meta("card_field","")=="cost":check(child.get_visible_line_count()>0,"Kristallkosten sichtbar "+species)
  enc.model.party[0].hp-=25;enc.model.special_charges[0]=5
  enc.model.act("special");enc.view.play_event();enc.view.event_time=.95;enc.view.refresh();await shot("special-"+species)
  enc.model.acknowledge_animation()
  enc.view.event_time=-1
  if enc.model.units(0).is_empty():enc.model._add_minion(0,Cards.SPECIAL[species][0])
  enc.model._add_minion(2,"thornling")
  enc.view.build_commands();enc.view.refresh()
  var uid: int=enc.model.units(0)[0].uid
  enc.model.find_unit(uid).ready=true;enc.view.input_cooldown=0;enc.view._pick_unit(uid);await shot("arena-"+species)
  check(enc.view.unit_info.visible and enc.view.unit_info_stats.text.contains("Angriff"),"Helferwerte beim Antippen "+species)
  var editor=Editor.new();editor.app=app;editor.species=species;editor.open();await shot("deck-"+species)
  editor.preview(Cards.ULTIMATES[species]);await shot("detail-"+species)
 for species in ["mosslyn","nocti"]:
  app.selected=species;await app._mount_world("OUTDOOR",app.Catalog.position_for("PARK",Vector2(.5,.5)));app._resume();await frames(8)
  check(app.actors.size()==8,"Neue Figuren als Bewohner in der Welt")
  check(app.actor.frame_bounds.size()==48,"48 echte Posen der neuen Figur")
  app.actor.enabled=false;app.actor.facing=Vector2.RIGHT;app.actor.gait=.25;app.actor._update_frame(true)
  await shot("world-"+species)
  app._show_conversation(species);await shot("talk-"+species)
 app.queue_free();await frames(3);DirAccess.remove_absolute("user://godot-tests/special-capture.json")
 print("Visuelle Kartenabnahme: %s Checks, %s Fehler" % [checks,failures]);quit(1 if failures else 0)
