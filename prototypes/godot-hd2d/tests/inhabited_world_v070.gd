extends SceneTree
const Residents=preload("res://scripts/world_residents.gd")
const Catalog=preload("res://scripts/world_catalog.gd")
const Forest=preload("res://scripts/forest_catalog.gd")
const Enemies=preload("res://scripts/regional_enemies.gd")
const Battle=preload("res://scripts/card_battle.gd")
var app: Node3D
var checks:=0
var failures:=0
func _initialize() -> void:call_deferred("run")
func check(ok: bool,label: String) -> void:
 checks+=1
 if not ok:failures+=1;push_error(label)
func frames(count: int=12) -> void:
 for i in count:await physics_frame
func run() -> void:
 check(Catalog.data().characters.size()==8,"Neue Bewohner werden nicht als spielbare Figuren angeboten")
 check(Residents.data().size()==12,"Acht alte und vier neue Einwohner")
 for hour in range(24):
  var places: Dictionary={}
  for id in Residents.data():
   var entry:=Residents.appointment(id,hour)
   places[entry.place]=true
   check(not str(entry.place).begins_with("SHADOW") and entry.place!="TORCH_PATH","Keine Bewohner im Dunkelwald: "+id)
   check(Residents.data()[id].role!="" and entry.activity!="","Rolle und konkrete Tätigkeit: "+id)
  check(places.size()>=5,"Auch nachts auf mehrere Orte verteilt")
 for id in Enemies.data():
  var model=Battle.new();model.configure("fennec");model.configure_enemy(id,Enemies.data()[id]);model.prepare({})
  model.begin();model.advance_intro(4);model.cursor=2;model.round=3
  check(model.enemy_act(),"Eigener Gegner führt Zug aus: "+id)
  check(model.last_event.title.begins_with(model.enemy_name),"Gegneridentität im Kampf")
  model.acknowledge_animation()
  var saved: Dictionary=model.snapshot()
  var restored=Battle.new();restored.configure("fennec");restored.configure_enemy(id,Enemies.data()[id]);restored.prepare({});restored.restore(saved)
  check(restored.enemy_health==model.enemy_health and restored.enemy_max_hp==Enemies.data()[id].hp,"Regionale LP bleiben beim Laden erhalten")
  if id=="thorn_moth":check(model.party[0].silence==1 and model.party[1].silence==1,"Pollen trifft beide und verstummt")
  if id=="ash_hound":check(model.party[0].hp==model.party[0].max_hp-22,"Glutsprung verursacht angekündigten Schaden")
  if id=="bark_guardian":
   model.enemy_health=100;model.cursor=2;model.enemy_burn=1
   model.enemy_act();check(model.enemy_health==94,"Brennen verhindert Rindenerneuerung")
 app=load("res://scenes/full_world.tscn").instantiate()
 DirAccess.make_dir_recursive_absolute("user://godot-tests");app.save_path="user://godot-tests/v070.json";DirAccess.remove_absolute(app.save_path)
 root.add_child(app)
 while app.loading_world:await process_frame
 app.world_clock.utc_now_override=1792058400 # 12:00 Berlin, 15 October 2026
 app.world_clock.utc_offset=0
 await app.start_game()
 var visited_ids: Dictionary={}
 for room in ["OUTDOOR","CRAFT","NOOK","SUN_GLADE","MILL_BROOK","FOREST_TRAIL"]:
  await app._mount_world(room,Forest.position_for(1,.2) if room=="FOREST_TRAIL" else Catalog.position_for("STREET" if room=="OUTDOOR" else room,Vector2(.25,.65)))
  app._resume();await frames(30)
  for resident in app.actors:
   if resident==app.actor:continue
   check(not visited_ids.has(resident.species),"Bewohner existiert nur einmal")
   visited_ids[resident.species]=true
   check(resident.is_on_floor() and app.stage.is_walkable(resident.position,resident.collider.shape.radius*.8),"Sicherer begehbarer Bewohnerstandort: "+resident.species)
   var entry: Dictionary=resident.get_meta("appointment")
   check(Residents.room_for(entry.place)==room,"Tatsächlicher Standort entspricht Tagesplan")
   if room=="FOREST_TRAIL":check(resident.position.x<40,"Waldfiguren bleiben im hellen Abschnitt")
  if room=="SUN_GLADE":
   var citizen=app.actors.filter(func(a):return a.species=="nella")[0]
   var before: Vector3=citizen.position
   await frames(240)
   check(citizen.position.distance_to(before)>.05,"Neue Figur bewegt sich mit eigener Gehfolge")
   app._show_conversation("nella",citizen.get_meta("appointment"));await process_frame
   check(app.paused and app.dialog_open,"Neue Bewohner führen Dialoge")
   app._resume()
 check(visited_ids.size()==11,"Alle anderen Figuren über ihre Aufenthaltsorte auffindbar")
 # Both forest branch entries use their matching return segment, not the forest entrance.
 for branch in [{"segment":1,"to":"SUN_GLADE"},{"segment":6,"to":"MIST_WATCH"}]:
  await app._mount_world("FOREST_TRAIL",Forest.position_for(branch.segment,.76 if branch.segment==1 else .87))
  app.forest_encounter.model.phase="done";app._resume();await frames()
  var door: Dictionary={}
  for candidate in app.stage.doors:
   if candidate.to==branch.to:door=candidate
  app.actor.position=app.stage.safe_position(door.position);app.actor.velocity=Vector3.ZERO;await frames()
  await app.interact_with("door",door.from+":"+door.to);await frames()
  check(app.room==branch.to and app.actor.is_on_floor(),"Waldabzweig erreicht neue Szene")
  for candidate in app.stage.doors:
   if candidate.to=="FOREST_TRAIL":door=candidate
  app.actor.position=app.stage.safe_position(door.position);app.actor.velocity=Vector3.ZERO;await frames()
  await app.interact_with("door",door.from+":"+door.to);await frames()
  check(app.room=="FOREST_TRAIL" and Forest.region_at(app.actor.position.x).id==("GREENWOOD" if branch.segment==1 else "SHADOW_DEPTHS"),"Rückweg landet am richtigen Waldabschnitt")
 await app._mount_world("MIST_WATCH",Catalog.position_for("MIST_WATCH",Vector2(.5,.6)))
 app._resume();await frames()
 check(app.regional_markers.size()==4,"Vier unterschiedliche Gegner an der Nebelwacht")
 app._show_enemy("ash_hound");await process_frame
 app.menu_buttons[0].pressed.emit();await frames()
 check(app.forest_encounter.model.enemy_name=="Aschenläufer" and app.paused,"Regionaler Kampf startet über sichtbare Aktion")
 var enc: Node=app.forest_encounter
 enc.model.advance_intro(4);enc.persist();enc.suspend();app._resume();await frames()
 check(enc.active() and not enc.suspended,"Menü setzt regionale Begegnung fort")
 enc.model.phase="victory";enc.model.enemy_health=0;enc.model.playback_pending=false;enc.busy=0;enc.finish();await frames()
 check(not app.paused and enc.model.phase=="done","Regionale Siegesfeier führt zurück in Welt")
 app.save_data["card_decks"]={"fennec":app.Cards.starter("fennec")}
 app._show_enemy("ash_hound");await process_frame;app.menu_buttons[0].pressed.emit();await frames()
 check(app.forest_encounter.model.source_decks==app.save_data.card_decks,"Wiederholung nutzt das aktuell bearbeitete Deck")
 app.forest_encounter._retreat_now();await frames()
 app.save_data.card_decks.fennec.reverse()
 app._show_enemy("ash_hound");await process_frame;app.menu_buttons[0].pressed.emit();await frames()
 check(app.forest_encounter.model.source_decks==app.save_data.card_decks,"Neustart nach Rückzug nutzt das aktuelle Deck")
 app.forest_encounter._retreat_now();await frames()
 enc=app.forest_encounter
 var earned: Dictionary=enc.model.experience.duplicate(true)
 var earned_inventory: Dictionary=enc.model.inventory.duplicate(true)
 app._select_regional_encounter("bark_guardian")
 check(app.forest_encounter.model.experience==earned and app.forest_encounter.model.inventory==earned_inventory,"XP und Gegenstände gelten auch im nächsten regionalen Kampf")
 await app._mount_world("OUTDOOR",Catalog.position_for("CITY",Vector2(.5,.65)));app._resume();await frames()
 app.world_clock.advance_hours(8)
 app.show_map();await process_frame
 for resident in app.actors:
  if resident==app.actor:continue
  check(resident.get_meta("appointment")==app.resident_appointments[resident.species],"Karte und sichtbare Bewohner teilen nach Zeitwechsel denselben Aufenthalt")
 app._resume()
 await app._mount_world("MIST_WATCH",Catalog.position_for("MIST_WATCH",Vector2(.5,.6)));app._resume();await frames()
 app.show_map();await frames()
 var map: Control=app.ui.get_child(0)
 check(map.nodes.size()==21 and map.chips.size()==21,"Illustrierte Karte besitzt bedienbare Ortsmarker")
 var represented: Array=[]
 for node in map.nodes:represented.append_array(node.places)
 check(represented.size()==38,"Alle 38 Orte sind im Atlas vertreten")
 map._select("MIST_WATCH")
 check(map.detail.text.contains("keine Brücke") and map.detail.text.contains("Sonnenhain") and map.detail.text.contains("Tiefer Wald"),"Atlas zeigt zusammenlaufende Wege und gesperrte künftige Grenze")
 check(not Catalog.data().interiors.has("DEMON_WORLD"),"Dämonenwelt erhält keinen vorzeitigen Zugang")
 app._resume();app.queue_free();await process_frame;DirAccess.remove_absolute("user://godot-tests/v070.json")
 print("Inhabited world v070: %s checks, %s failures"%[checks,failures]);quit(1 if failures else 0)
