extends SceneTree
const Catalog = preload("res://scripts/world_catalog.gd")
const Enemies = preload("res://scripts/regional_enemies.gd")
const Life = preload("res://scripts/enemy_life.gd")
const Battle = preload("res://scripts/card_battle.gd")
const Path = preload("res://scripts/interaction_path.gd")
var checks := 0
var failures := 0
func _initialize() -> void: call_deferred("run")
func check(ok: bool, label: String) -> void:
 checks += 1
 if not ok: failures += 1; push_error(label)
func run() -> void:
 DirAccess.make_dir_recursive_absolute("user://godot-tests")
 var app = load("res://scenes/full_world.tscn").instantiate()
 app.save_path = "user://godot-tests/enemy-roaming.json"
 DirAccess.remove_absolute(app.save_path); root.add_child(app)
 while app.loading_world: await process_frame
 # Ein alter, gerade laufender Rindenwächter-Kampf muss am neuen Revier fortsetzen.
 var battle = Battle.new(); battle.configure("fennec"); battle.configure_enemy("bark_guardian",Enemies.data().bark_guardian); battle.prepare({}); battle.begin(); battle.advance_intro(4)
 app.save_data["characters"] = {"fennec":{"room":"MIST_WATCH","position":[0,.03,-1]}}
 app.save_data["forest_encounters"] = {"fennec:bark_guardian":battle.snapshot()}
 app.selected = "fennec"; await app.start_game()
 check(app.room == "MILL_BROOK" and app.paused,"Alter laufender Kampf zieht ohne Verlust ins neue Revier")
 check(app.forest_encounter.model.enemy_id == "bark_guardian" and app.forest_encounter.model.phase == "battle","Gespeicherte Kampfphase bleibt erhalten")
 app.forest_encounter._retreat_now(); app.save_data.forest_encounters.clear()
 var expected := {"SUN_GLADE":1,"MIST_WATCH":2,"MILL_BROOK":2,"ASH_ASCENT":2,"SEAL_COURT":1}
 for place in expected:
  await app._mount_world(place,Catalog.position_for(place,Vector2(.03,.5))); app._resume()
  app.actor.enabled = false; app.actor.position=Vector3(0,.03,20)
  check(app.regional_markers.size() == expected[place],"Getrennte Reviere: "+place)
  for marker in app.regional_markers:
   var profile: Dictionary = Enemies.data()[marker.id]
   var previous: Vector3 = stage_point(app,profile,0)
   var valid := true; var depth_min := INF; var depth_max := -INF
   for tick in 120:
    var life := Life.sample(profile,tick*float(profile.period)/120.0)
    var point: Vector3 = app.stage.safe_position(Catalog.position_for(place,life.uv),.1)
    var route := Path.find_path(app.stage,previous,point,.1)
    valid = valid and not route.is_empty()
    for waypoint in route:
     valid = valid and Path.clear_segment(app.stage,previous,waypoint,.1);previous=waypoint
    depth_min = minf(depth_min,point.z); depth_max = maxf(depth_max,point.z)
   check(valid,"Voller Hin- und Rückweg auf trockenem Boden: "+marker.id)
   if profile.kind != "boss": check(depth_max-depth_min>.06,"Revier nutzt auch Bildtiefe: "+marker.id)
  var distances := {};var last := {};var valid_motion := true;var min_gap := INF
  for marker in app.regional_markers:
   last[marker.id]=marker.position;distances[marker.id]=0.0
  for tick in 1500:
   await physics_frame
   for marker in app.regional_markers:
    var distance: float=marker.position.distance_to(last[marker.id])
    valid_motion=valid_motion and distance<.07 and Path.clear_segment(app.stage,last[marker.id],marker.position,.1)
    distances[marker.id]+=distance;last[marker.id]=marker.position
   if app.regional_markers.size()==2: min_gap=minf(min_gap,app.regional_markers[0].position.distance_to(app.regional_markers[1].position))
  check(valid_motion,"Echte Bewegung ohne Randteleport: "+place)
  if app.regional_markers.size()==2: check(min_gap>2.0,"Mindestens zwei Weltmeter zwischen Gegnerrevieren: "+place+" / "+str(min_gap))
  for marker in app.regional_markers:
   check(float(distances[marker.id])>(.15 if marker.id=="vharok" else 2.0),"Echte Erkundungsstrecke: "+marker.id+" / "+str(distances[marker.id]))
   # In Reichweite hält genau dieses Wesen an. Sein Tätigkeitsablauf läuft nicht weg.
   app.actor.position=marker.position;var clock: float=marker.life_clock;var point: Vector3=marker.position
   for tick in 20: await physics_frame
   check(is_equal_approx(clock,marker.life_clock) and marker.position.distance_to(point)<.001,"Annäherung pausiert Revier und Ablauf: "+marker.id)
   app.actor.position=Vector3(0,.03,20)
  if place=="MILL_BROOK":
   app.save_data.forest_encounters["fennec:bark_guardian"]={"phase":"done"};app._update_regional_art()
   for marker in app.regional_markers:
    if marker.id=="bark_guardian":check(not marker.art.visible and not marker.shadow.visible,"Alter besiegter Gegner bleibt nach Revierwechsel besiegt")
 app.queue_free(); await process_frame; DirAccess.remove_absolute("user://godot-tests/enemy-roaming.json")
 print("Enemy roaming: %s checks, %s failures"%[checks,failures]); quit(1 if failures else 0)
func stage_point(app,profile: Dictionary,clock: float) -> Vector3:
 return app.stage.safe_position(Catalog.position_for(profile.place,Life.sample(profile,clock).uv),.1)
