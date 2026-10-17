extends SceneTree
const Battle=preload("res://scripts/card_battle.gd")
const Enemies=preload("res://scripts/regional_enemies.gd")
const Life=preload("res://scripts/enemy_life.gd")
const Rules=preload("res://scripts/ecology_rules.gd")
const Audio=preload("res://scripts/battle_audio.gd")
const Catalog=preload("res://scripts/world_catalog.gd")
var checks:=0
var failures:=0
func check(ok: bool,label: String) -> void:
 checks+=1
 if not ok:failures+=1;push_error(label)
func fresh(id: String,species: String="fennec"):
 var b=Battle.new();b.configure(species);b.configure_enemy(id,Enemies.data()[id]);b.prepare({});b.begin();b.advance_intro(4);return b
func enemy(b) -> void:
 b.cursor=2;b.playback_pending=false;b.enemy_act();b.acknowledge_animation()
func _initialize() -> void:call_deferred("run")
func run() -> void:
 for id in Enemies.data():
  var profile: Dictionary=Enemies.data()[id]
  check(profile.has("purpose") and profile.has("work") and profile.has("travel"),id+" besitzt Aufgabe und Tätigkeit")
  for clock in 40:
   var life:=Life.sample(profile,clock*.5)
   check(life.uv.x>=profile.patrol[0]-.00001 and life.uv.x<=profile.patrol[1]+.00001,id+" bleibt im Revier")
 for id in ["tide_crab","resin_beetle","cinder_carrier","vharok"]:
  var im: Image=load(Enemies.asset(id)).get_image()
  for pose in 7:
   var rect:=Rect2i(Enemies.region(id,pose));var crop:=im.get_region(rect)
   check(rect.size.x>100 and crop.get_used_rect().size.y>100,id+" vollständige Pose")
  check(im.get_pixel(0,0).a<.01,id+" echte Transparenz")
  var view=load("res://scripts/card_view.gd").new();view.model=fresh(id)
  for pose in 7:
   var dimensions: Vector2=view._regional_dimensions(pose)
   var top: float=view._position(2).y-dimensions.y
   check(top>=165 and view._position(2).x+dimensions.x*.5<=1260,id+" echte Rendermaße aller Posen bleiben außerhalb der UI")
  view.free()
  var b=fresh(id);enemy(b)
  var saved: Dictionary=JSON.parse_string(JSON.stringify(b.snapshot()))
  var other=fresh(id);other.restore(saved)
  check(other.ecology_state==b.ecology_state and other.board==b.board,id+" JSON-Spielstand erhält Zustand")
  var hp: int=other.enemy_health;other.acknowledge_animation();other.acknowledge_animation()
  check(other.enemy_health==hp,id+" Animation wirkt nur einmal")
  other.phase="defeat";other.retry()
  check(other.enemy_health==other.enemy_max_hp and int(other.ecology_state.step)==0,id+" Neustart setzt Kampf zurück")
 var b=fresh("vharok");check(Rules.seal_count(b)==3,"Drei Siegel bewachen Vharok")
 check(b.legal_targets(0).size()==3 and "enemy" not in b.legal_targets(0),"Siegel erzwingen Angriffsziele")
 var effects: Array[Dictionary]=[]
 b._damage_target(0,"enemy",20,effects);check(b.enemy_health==265,"Geschützter Kern erhält 25 Prozent")
 for unit in b.units(2).duplicate():b._damage_target(0,"u:"+str(unit.uid),10,effects)
 check(int(b.ecology_state.phase)==2 and int(b.ecology_state.core_turns)==2,"Siegelbruch öffnet Kern")
 b._damage_target(0,"enemy",20,effects);check(b.enemy_health==235,"Offener Kern erhält 150 Prozent")
 enemy(b);check(int(b.ecology_state.phase)==2,"Reaktionsfenster bleibt einen Zug")
 enemy(b);check(int(b.ecology_state.phase)==1 and Rules.seal_count(b)==1,"Danach erneuert Vharok ein Siegel")
 b.enemy_health=90;Rules.after_damage(b)
 check(int(b.ecology_state.phase)==3 and Rules.seal_count(b)==0,"Sturmphase bei 90 LP")
 var hp0: int=b.party[0].hp;var hp1: int=b.party[1].hp
 enemy(b);check(b.party[0].hp==hp0 and b.party[1].hp==hp1,"Sturm wird einen ganzen Zug angekündigt")
 check(b.enemy_intent().contains("42 auf beide"),"Sturmschaden vorab lesbar")
 b.party[0].barrier=40;enemy(b)
 check(b.party[0].hp==hp0-2 and b.party[0].barrier==0 and b.party[1].hp==hp1-42,"Schutz und Gruppenschaden gelten pro Ziel")
 b=fresh("vharok","puffling");b.enemy_stagger=1;enemy(b)
 check(int(b.ecology_state.resolve)==2,"Boss wird nach Unterbrechung für zwei Züge standfest")
 b.cursor=0;b.act("skill");check(b.enemy_stagger==0,"Standfestigkeit verhindert endlose Wankenketten")
 b.acknowledge_animation();enemy(b);enemy(b);check(int(b.ecology_state.resolve)==0,"Standfestigkeit läuft wieder aus")
 b=fresh("vharok","starlet");b.act("skill");check(b.enemy_health==262,"Geerbte Heldenkraft respektiert Siegel")
 b=fresh("tide_crab","starlet");b.enemy_health=5;b.act("skill");check(b.phase=="victory","Rüstung wird vor Lebenspunktebegrenzung angewendet")
 b=fresh("vharok");b.phase="victory";b.playback_pending=false
 check(b.claim_victory() and b.result.members[0].gained==360,"Boss belohnt mit 360 Erfahrung")
 check(b.inventory.moon_shard==3,"Boss belohnt mit drei Mondsplittern")
 var shards: int=b.inventory.moon_shard;check(not b.claim_victory() and b.inventory.moon_shard==shards,"Bossbeute kann nur einmal beansprucht werden")
 b=fresh("tide_crab");b._damage_target(0,"enemy",3,effects);check(b.enemy_health==120,"Panzer fängt kleine Treffer ab")
 enemy(b);enemy(b);check(b.ecology_state.open,"Nach Zangenschlag ist Panzer offen")
 b._damage_target(0,"enemy",10,effects);check(b.enemy_health==110,"Offener Panzer mindert Schaden nicht")
 b=fresh("resin_beetle");b.enemy_health=100;enemy(b);check(b.enemy_health==112,"Harzsammeln heilt")
 b=fresh("resin_beetle");b.enemy_health=100;b.enemy_burn=1;enemy(b);check(b.enemy_health==94,"Brennen unterbricht Heilung")
 b=fresh("resin_beetle");enemy(b);b.party[1].barrier=40;enemy(b);check(b.party[1].bind==0,"Harz bindet nicht durch Schutz")
 b=fresh("cinder_carrier","gloop");enemy(b);check(b.ecology_state.fuel,"Schlackenträger lädt sichtbar auf")
 check(Audio.impact_key(b.last_event)=="","Aufladezug besitzt keinen Trefferklang")
 b.cursor=0;b.act("skill");check(not b.ecology_state.fuel,"Wasser löscht Ladung")
 b.acknowledge_animation();enemy(b);b.enemy_stagger=1;enemy(b);check(not b.ecology_state.fuel,"Wanken bricht Ladung ab")
 b=fresh("vharok");b.enemy_health=90;Rules.after_damage(b);enemy(b)
 check(b.last_event.ecology_action=="warning" and Audio.impact_key(b.last_event)=="","Sturmwarnung ist eigener Effekt ohne erfundenen Treffer")
 b.party[0].barrier=40;enemy(b);check(Audio.impact_key(b.last_event)=="shield","Abgefangener Sturm erhält Schutzklang")
 var app=load("res://scenes/full_world.tscn").instantiate();app.save_path="user://godot-tests/ecology090.json"
 DirAccess.make_dir_recursive_absolute("user://godot-tests");DirAccess.remove_absolute(app.save_path);root.add_child(app)
 while app.loading_world:await process_frame
 await app.start_game()
 for place in ["POND","MILL_BROOK","ASH_ASCENT","SEAL_COURT"]:
  await app._mount_world("OUTDOOR" if not Catalog.region_for(place).is_empty() else place,Catalog.position_for(place,Vector2(.1,.6)));app._resume()
  for i in 20:await physics_frame
  for marker in app.regional_markers:
   check(app.stage.is_walkable(marker.position,.1),"Revier steht auf begehbarem Boden")
   check(marker.material.render_priority==10 and is_instance_valid(marker.shadow),"Kulissenmaske und Bodenschatten sind vorhanden")
  if place=="ASH_ASCENT":
   var marker: Dictionary=app.regional_markers[0];var last: Vector3=marker.position;var distance:=0.0
   for i in 600:
    await physics_frame
    check(marker.position.distance_to(last)<.15,"Arbeitsweg bewegt sich ohne Teleport")
    distance+=marker.position.distance_to(last);last=marker.position
   check(distance>.2,"Schlackenträger führt echten Arbeitsweg aus")
   check(app.stage.local_light_sources.size()==2,"Depotfeuer beleuchten dynamisch die Figuren")
  if place=="SEAL_COURT":
   check(app.stage.local_light_sources.size()==5,"Feuerschalen im Siegelhof sind lokal verankert")
   app._show_enemy("vharok");await process_frame
   app.menu_buttons[0].pressed.emit();await process_frame
   check(app.paused and app.forest_encounter.model.enemy_id=="vharok","Boss über echte Beobachten-Aktion erreichbar")
   app.forest_encounter._retreat_now()
   app.save_data.forest_encounters[app.actor.species+":vharok"]={"phase":"done"};app._update_regional_art()
   check(not app.regional_markers[0].art.visible,"Besiegter Boss wird nicht erneut als Gegner angeboten")
 app.queue_free();await process_frame;DirAccess.remove_absolute("user://godot-tests/ecology090.json")
 print("Ecology v090: %s checks, %s failures"%[checks,failures]);quit(1 if failures else 0)
