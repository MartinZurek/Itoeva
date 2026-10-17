extends SceneTree
const Motion=preload("res://scripts/battle_motion.gd")
const Forest=preload("res://scripts/forest_catalog.gd")
var checks:=0
var failures:=0
func _initialize() -> void:call_deferred("run")
func check(ok: bool,label: String) -> void:
 checks+=1
 if not ok:failures+=1;push_error(label)
func run() -> void:
 for home in [Vector2(415,348),Vector2(685,461),Vector2(887,451),Vector2(1153,451)]:
  for target in [Vector2(125,306),Vector2(550,348),Vector2(685,461),Vector2(1110,420)]:
   if home.distance_to(target)<150:continue
   for style in ["pounce","flier","guard","crawler"]:
    var before: Vector2=home;var valid:=true
    for tick in 325:
     var state:=Motion.attack(home,target,tick/120.0,1.25,2.7,style)
     valid=valid and Motion.GROUND.grow(.01).has_point(state.ground) and state.ground.distance_to(before)<20 and state.lift>=-.001
     before=state.ground
    check(valid,"Begrenzter zusammenhängender Weg: %s/%s/%s"%[home,target,style])
    check(Motion.attack(home,target,2.7,1.25,2.7,style).ground.distance_to(home)<.001,"Rückkehr exakt auf den Standplatz")
 var contacts: Array=[]
 for lane in [-1,0,1]:contacts.append(Motion.attack(Vector2(887+133*(lane+1),451),Vector2(125,306),.62,.62,1.65,"pounce",lane).ground)
 check(contacts[0].distance_to(contacts[1])>30 and contacts[1].distance_to(contacts[2])>30,"Drei Gegnerhelfer erreichen getrennte Kontaktpunkte")
 DirAccess.make_dir_recursive_absolute("user://godot-tests")
 var app=load("res://scenes/full_world.tscn").instantiate();app.save_path="user://godot-tests/combat-motion.json";DirAccess.remove_absolute(app.save_path);root.add_child(app)
 while app.loading_world:await process_frame
 app.selected="fennec"
 await app._mount_world("FOREST_TRAIL",Forest.position_for(5,.44))
 var enc=app.forest_encounter;enc.model.begin();enc.model.advance_intro(4);enc.resume();enc.set_process(false);enc.view.set_process(false)
 var model=enc.model;var view=enc.view
 model.board.clear()
 var mine: Array=[];var theirs: Array=[]
 for owner in [0,1,2]:
  for slot in 3:
   var uid: int=model._add_minion(owner,"wolf")
   if owner==0:mine.append(uid)
   if owner==2:theirs.append(uid)
 for uid in mine+theirs:view._unit_position(uid)
 var positions: Array=[]
 for uid in theirs:positions.append(view._unit_position(uid))
 model.cursor=0;model.playback_pending=false;model.find_unit(mine[0]).ready=true;model.find_unit(theirs[0]).hp=1
 check(model.attack_minion(mine[0],"u:"+str(theirs[0])),"Echter Kartenangriff startet mit Wegzeit")
 view.play_event();view.event_time=model.impact_time()+.08
 check(model.impact_time()==1.25 and model.animation_duration()==2.7,"Anlauf und Treffer teilen den gespeicherten Zeitpunkt")
 check(view._unit_position(theirs[1])==positions[1] and view._unit_position(theirs[2])==positions[2],"Überlebende rutschen nach einem Treffer nicht durch freie Plätze")
 var fallen:=false
 for unit in view._visible_board():
  if int(unit.uid)==int(theirs[0]):fallen=unit.get("_fallen",false)
 check(fallen and view._unit_position(theirs[0])==positions[0],"Besiegter Helfer fällt an seinem bisherigen Standplatz")
 var light: Dictionary=view._pawn_light(positions[0]);var far: Dictionary=view._pawn_light(Vector2(40,480))
 check(light.pulse_gain>far.pulse_gain and light.pulse_gain<=.181,"Trefferlicht bleibt örtlich und begrenzt")
 model.acknowledge_animation();view.event_time=-1
 check(view._visible_board().size()==model.board.size(),"Nach dem Ablauf verschwindet nur der Gefallene")
 # Gegentreffer tötet einen Angreifer: er darf nicht zur Formation zurücklaufen.
 model.cursor=0;model.find_unit(mine[1]).ready=true;model.find_unit(mine[1]).hp=1;model.find_unit(theirs[1]).attack=20
 check(model.attack_minion(mine[1],"u:"+str(theirs[1])),"Gegentreffer wird regelkonform aufgelöst")
 view.play_event();view.event_time=model.impact_time()+.3
 var attacker: Dictionary={}
 for unit in view.event.before_board:
  if int(unit.uid)==int(mine[1]):attacker=unit
 var at: Dictionary=view._attack_motion(attacker,"u:"+str(theirs[1]))
 view.event_time=model.animation_duration()-.12
 var later: Dictionary=view._attack_motion(attacker,"u:"+str(theirs[1]))
 check(at.ground.distance_to(later.ground)<.001 and attacker.get("_fallen",false),"Gefallener Angreifer bleibt am Kontaktpunkt")
 check(view._visual_unit_position(int(mine[1])).distance_to(later.foot)<.001,"Gegentrefferzahl und Licht folgen dem bewegten Angreifer")
 check(not model.last_event.before_board[1].has("_fallen"),"Präsentation verändert den gespeicherten Kampf nicht")
 enc.suspended=true;app.queue_free();await process_frame;DirAccess.remove_absolute("user://godot-tests/combat-motion.json")
 print("Combat motion/light: %s checks, %s failures"%[checks,failures]);quit(1 if failures else 0)
