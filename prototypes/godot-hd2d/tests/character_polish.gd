extends SceneTree
const Catalog=preload("res://scripts/world_catalog.gd")
const Forest=preload("res://scripts/forest_catalog.gd")
const Cards=preload("res://scripts/card_catalog.gd")
var checks:=0
var failures:=0
func _initialize() -> void:call_deferred("run")
func check(ok: bool,label: String) -> void:
 checks+=1
 if not ok:failures+=1;push_error(label)
func run() -> void:
 DirAccess.make_dir_recursive_absolute("user://godot-tests")
 var path:="user://godot-tests/character-polish.json"
 var progress: Dictionary=JSON.parse_string(JSON.stringify({"room":"OUTDOOR","position":[20,.03,1]}))
 var file:=FileAccess.open(path,FileAccess.WRITE)
 file.store_string(JSON.stringify({"settings":{"species":"fennec"},"characters":{"fennec":progress},"forest_encounters":{"fennec:test":{"xp":321}}}));file.close()
 var app=load("res://scenes/full_world.tscn").instantiate();app.save_path=path;root.add_child(app)
 while app.loading_world:await process_frame
 var choices: Array=[]
 for card in app.portrait_cards:choices.append(card.species)
 check(choices==["mosslyn","nocti","starlet","wyrmling"],"Nur vier Startfiguren in fester Reihenfolge")
 check(app.selected=="mosslyn","Alte Fennec-Auswahl öffnet gültiges Startwesen")
 check(app.save_data.characters.fennec==progress and app.save_data.forest_encounters["fennec:test"].xp==321,"Alte Standorte und Kampffortschritt bleiben erhalten")
 app._select_species("gloop");check(app.selected=="mosslyn","Bewohner können nicht durch Auswahlcallback aktiviert werden")
 for species in Catalog.STARTERS:
  app._select_species(species);check(app.selected==species,"Startfigur ist auswählbar: "+species)
 app._save_preferences()
 var saved: Dictionary=JSON.parse_string(FileAccess.get_file_as_string(path))
 check(saved.characters.fennec==progress and saved.forest_encounters["fennec:test"].xp==321,"Neue Auswahl überschreibt alte Profile nicht")
 var residents=load("res://scripts/world_residents.gd")
 for species in ["fennec","gloop","puffling","hootlet"]:
  check(residents.data().has(species) and not residents.data()[species].schedule.is_empty() and residents.data()[species].about!="","Bewohner behält Dialog/Tagesplan: "+species)
 for species in Catalog.STARTERS:
  app.selected=species;await app._mount_world("OUTDOOR",Catalog.position_for("SPORT",Vector2(.4,.65)))
  var actor=app.actor;actor.enabled=false;actor.jump_clock=-1;actor.recovery_clock=1;actor.state="idle"
  check(actor.sprite.texture.resource_path==Catalog.data().characters[species].asset,"Welt nutzt richtige Identität: "+species)
  for direction in [Vector2.RIGHT,Vector2.LEFT,Vector2.DOWN,Vector2.UP]:
   actor.facing=direction;actor.running=false;actor.run_blend=0;actor.hover=0;actor._update_frame(false)
   var rest: int=actor.sprite.frame
   check(rest in [0,1,2],"Gerichtete Ruhe: "+species)
   var cycle_span: float=4.0 if species=="mosslyn" and absf(direction.x)>absf(direction.y) else 1.0
   var walking:={}
   for phase in 8:
    actor.gait=cycle_span*(phase+.1)/8;actor._update_frame(true);walking[actor.sprite.frame]=true
   check(walking.size()==8 and actor.walk_sheet,"Acht Gehphasen: "+species)
   actor.running=true;actor.run_blend=1
   var running_frames:={}
   for phase in 8:
    actor.gait=cycle_span*(phase+.1)/8;actor.clock=(phase+.1)/(8*actor.FLIGHT_CYCLES);actor._update_frame(true)
    running_frames[actor.sprite.frame]=true
    check(actor.frame_bounds[actor.sprite.frame].has_area(),"Laufphase besitzt Alphakontur: "+species)
   check(running_frames.size()>=7,"Eigener Sprintzyklus: "+species)
   actor.running=false;actor.run_blend=0;actor.hover=0
   for settle in 12:actor._update_frame(false)
   check(actor.sprite.frame==rest and not actor.run_sheet,"Anhalten stellt dieselbe Figur wieder her: "+species)
  if species=="starlet":check(is_instance_valid(actor.aura),"Starlits Eigenleuchten bleibt erhalten")
  if species=="nocti":
   actor.running=true;actor.hover=.4;actor._update_frame(false)
   check(actor.run_sheet and actor.sprite.texture.resource_path.ends_with("nocti-flight.png"),"Landung nutzt Flugzeichnung")
  actor.state="sit";actor._update_frame(false);check(not actor.run_sheet,"Sitzen beendet Lauf/Flug: "+species)
  if species in ["fennec","gloop","puffling","hootlet"]:check(app._exploration_reply(species)!="","Dialog bleibt aufrufbar: "+species)
 app.selected="nocti";await app._mount_world("OUTDOOR",Catalog.position_for("SPORT",Vector2(.3,.6)));app._resume()
 app.actor.touch_vector=Vector2.RIGHT
 for i in 55:await physics_frame
 check(app.actor.running and app.actor.hover>.35 and app.actor.run_sheet and app.actor.is_on_floor(),"Echter Sprint hebt Zeichnung an; Bodenkollision bleibt aktiv")
 app.actor.touch_vector=Vector2.ZERO
 for i in 40:await physics_frame
 check(app.actor.hover<.001 and not app.actor.run_sheet and app.actor.actual_speed<.01,"Nocti landet nach Freigabe des Sticks")
 await app._mount_world("FOREST_TRAIL",Forest.position_for(5,.44))
 var enc=app.forest_encounter;enc.model.begin();enc.model.advance_intro(4);enc.resume();enc.set_process(false);enc.view.set_process(false)
 var view=enc.view;var motion:={"stride":.6,"travel":true}
 for form in Cards.HELPER_FORMS:
  check(view._unit_travel_art({"id":"special_"+form,"form":form,"row":-1,"species":"nocti"},motion).is_empty(),"Formhelfer behält eigene Gestalt: "+form)
 for row in 4:
  check(view._unit_travel_art({"id":"power_animal","form":"","row":row,"species":"fennec"},motion).is_empty(),"Wildruf-Tier wird nicht Fennec: "+str(row))
 for species in Catalog.data().characters:
  app.character_metrics.erase(species)
  var art: Dictionary=view._unit_travel_art({"id":"companion","form":"","row":-1,"species":species},motion)
  check(not art.is_empty() and art.texture.resource_path==Catalog.data().characters[species].asset,"Spezieshelfer verwendet eigene Identität: "+species)
 for id in Cards.Strategy.ids():
  check(view._unit_travel_art({"id":id,"species":"nocti"},motion).is_empty(),"Strategiekarte wird nicht Held: "+id)
 enc.suspended=true;app.queue_free();await process_frame;DirAccess.remove_absolute(path)
 print("Character polish: %s checks, %s failures"%[checks,failures]);quit(1 if failures else 0)
