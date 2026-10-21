extends SceneTree
const Battle=preload("res://scripts/card_battle.gd")
const Enemies=preload("res://scripts/regional_enemies.gd")
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
 for id in ["glut_imp","seal_weaver"]:
  var b=fresh(id)
  check(b.message.begins_with(b.enemy_name),"Einleitung behält regionale Identität")
  var im: Image=load(Enemies.asset(id)).get_image()
  for pose in 6:
   var box:=Rect2i(Enemies.region(id,pose));var alpha:=im.get_region(box).get_used_rect()
   check(alpha.size.x>100 and alpha.size.y>100,"Sechs vollständige eigene Posen")
   check(im.get_pixel(0,0).a<.01,"Echte Transparenz")
  for species in Catalog.STARTERS:
   b=fresh(id,species);check(b.can_act("skill") and b.act("skill"),"Eigene Heldenkraft gegen Dämon: "+species)
   b.acknowledge_animation();check(b.party[0].hp>0,"Heldenkraft lässt Gruppe intakt")
 var b=fresh("glut_imp");var hp: int=b.party[0].hp
 enemy(b);check(b.demon_charge and b.party[0].hp==hp,"Glut sammeln gibt einen vollständigen Reaktionszug")
 check(b.enemy_intent().contains("24"),"Aufgeladener Schaden wird vorher angezeigt")
 enemy(b);check(not b.demon_charge and b.party[1].hp==b.party[1].max_hp-24,"Angekündigter Feuerwurf trifft genau einmal")
 b=fresh("glut_imp","gloop");enemy(b);b.cursor=0
 check(b.act("skill") and not b.demon_charge,"Gloops Wasser löscht Glut")
 b.acknowledge_animation();hp=b.party[0].hp;enemy(b)
 check(b.party[0].hp==hp and b.demon_charge,"Gelöschte Glut muss erneut aufgebaut werden")
 b=fresh("glut_imp");enemy(b);b.enemy_stagger=1;enemy(b)
 check(not b.demon_charge and b.enemy_stagger==0,"Wanken bricht Aufladung ab")
 b=fresh("glut_imp");enemy(b);b.enemy_weaken=1;hp=b.party[0].hp;enemy(b)
 check(b.party[1].hp==b.party[1].max_hp-16,"Schwächen reduziert Feuerwurf")
 b=fresh("glut_imp");enemy(b);b.party[1].barrier=40;hp=b.party[1].hp;enemy(b)
 check(b.party[1].hp==hp and b.party[1].barrier==16,"Lichtschirm absorbiert Feuerwurf")
 b=fresh("glut_imp");enemy(b);var uid: int=b._add_minion(0,"guard",{"name":"Schutz","attack":1,"health":30,"taunt":true});hp=b.party[0].hp;enemy(b)
 check(b.party[0].hp==hp and b.find_unit(uid).hp==6,"Schutzhelfer fängt Feuerwurf ab")
 b=fresh("seal_weaver");enemy(b)
 check(b.seal_pending and b._seal_anchor().hp==10 and b._seal_anchor().taunt,"Siegelanker hat sichtbare LP und Schutz")
 hp=b.party[0].hp;enemy(b)
 check(not b.seal_pending and b._seal_anchor().is_empty() and b.party[0].hp==hp-10 and b.party[0].silence==1,"Überlebender Anker löst angekündigtes Siegel aus")
 b=fresh("seal_weaver");enemy(b);var effects: Array[Dictionary]=[]
 b._damage_target(0,"u:"+str(b._seal_anchor().uid),10,effects);hp=b.enemy_health
 enemy(b);check(b.enemy_health==hp-12 and b.party[0].silence==0,"Zerstörter Anker verursacht Rückstoß statt Siegel")
 b=fresh("seal_weaver");enemy(b);b.enemy_stagger=1;enemy(b)
 check(not b.seal_pending and b._seal_anchor().is_empty(),"Wanken löst Ritual samt Anker auf")
 b=fresh("seal_weaver");enemy(b);b.party[0].barrier=40;enemy(b)
 check(b.party[0].silence==0 and b.party[1].silence==1,"Schutz gegen Status gilt pro Ziel")
 for id in ["glut_imp","seal_weaver"]:
  b=fresh(id);b.cursor=2;b.enemy_act()
  var saved: Dictionary=JSON.parse_string(JSON.stringify(b.snapshot()))
  var restored=fresh(id);restored.restore(saved)
  check(restored.demon_charge==b.demon_charge and restored.seal_pending==b.seal_pending and restored.board==b.board,"Ritual und Anker bleiben beim Laden erhalten")
  var old: int=restored.enemy_health;restored.acknowledge_animation();restored.acknowledge_animation()
  check(restored.enemy_health==old,"Animation nach Laden verdoppelt keinen Effekt")
  restored.phase="defeat";restored.retry()
  check(not restored.demon_charge and not restored.seal_pending and restored.board.is_empty(),"Neustart räumt Rituale auf")
 var app=load("res://scenes/full_world.tscn").instantiate();app.save_path="user://godot-tests/demons-v080.json"
 DirAccess.make_dir_recursive_absolute("user://godot-tests");DirAccess.remove_absolute(app.save_path);root.add_child(app)
 while app.loading_world:await process_frame
 await app.start_game();await app._mount_world("MIST_WATCH",Catalog.position_for("MIST_WATCH",Vector2(.5,.6)));app._resume()
 for i in 20:await physics_frame
 check(app.regional_markers.size()==2,"Nebelwacht enthält nur Aschenläufer und Siegelweber")
 for marker in app.regional_markers:
  check(app.stage.is_walkable(marker.position,.1),"Gegner steht auf begehbarem Boden")
  check(marker.material.render_priority==10 and is_instance_valid(marker.shadow),"Gegner besitzt Überdeckung und Kontaktschatten")
 for id in ["glut_imp","seal_weaver"]:
  var place: String=Enemies.data()[id].place
  await app._mount_world(place,Catalog.position_for(place,Vector2(.5,.6)));app._resume()
  app._show_enemy(id);await process_frame;app.menu_buttons[0].pressed.emit()
  for i in 3:await process_frame
  check(app.forest_encounter.model.enemy_id==id and app.paused,"Gegner über echte Aktion erreichbar")
  app.forest_encounter._retreat_now()
 app.queue_free();await process_frame;DirAccess.remove_absolute("user://godot-tests/demons-v080.json")
 print("Demons v080: %s checks, %s failures"%[checks,failures]);quit(1 if failures else 0)
