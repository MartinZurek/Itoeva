extends SceneTree
const Stamina = preload("res://scripts/movement_stamina.gd")
const Catalog = preload("res://scripts/world_catalog.gd")
var checks := 0
var failures := 0
func _initialize() -> void: call_deferred("run")
func check(ok: bool, label: String) -> void:
 checks += 1
 if not ok: failures += 1; push_error(label)
func frames(count: int) -> void:
 for i in count: await physics_frame
func run() -> void:
 for hz in [30,60,120]:
  var energy = Stamina.new()
  for i in hz*6: energy.advance(1.0/hz,1.0,true,false,false)
  check(absf(energy.value-4.0)<.01,"Sechs Sprintsekunden verbrauchen 96: %s Hz"%hz)
  for i in hz: energy.advance(1.0/hz,1.0,true,false,false)
  check(energy.exhausted and not energy.allows_sprint(),"Leerer Vorrat sperrt Dauersprint: %s Hz"%hz)
  for i in hz*3: energy.advance(1.0/hz,0.0,true,false,false)
  check(energy.exhausted,"Kein flackernder Neustart mit einem Punkt: %s Hz"%hz)
  for i in hz*2: energy.advance(1.0/hz,0.0,true,false,false)
  check(energy.allows_sprint(),"Gehen ermöglicht später wieder Sprint: %s Hz"%hz)
 var rates: Array[float]=[]
 for rest in 4:
  var energy=Stamina.new(); energy.value=0; energy.exhausted=true
  energy.advance(1.0,0.0,rest==0,rest>=2,rest==3); rates.append(energy.value)
 check(rates==[7.0,12.0,25.0,50.0],"Gehen < Stehen < Bodensitz < Bank")
 var burst=Stamina.new(); burst.value=13
 check(not burst.spend(14) and burst.value==13,"Rollen benötigen ausreichend Ausdauer")
 burst.value=14
 check(burst.spend(14) and burst.exhausted,"Rolle kann Restvorrat aufbrauchen")
 var app=load("res://scenes/full_world.tscn").instantiate()
 DirAccess.make_dir_recursive_absolute("user://godot-tests")
 app.save_path="user://godot-tests/stamina-motion.json"; DirAccess.remove_absolute(app.save_path); root.add_child(app)
 while app.loading_world: await process_frame
 app.selected="nocti"
 var spawn:=Catalog.position_for("SPORT",Vector2(.4,.65))
 await app._mount_world("OUTDOOR",spawn); app._resume(); await frames(15)
 for creature in app.actors:
  if creature!=app.actor: creature.enabled=false; creature.collider.disabled=true
 app.actor.touch_vector=Vector2.RIGHT
 var fastest:=0.0; var first_exhaustion:=-1
 for i in 460:
  await physics_frame; fastest=maxf(fastest,app.actor.actual_speed)
  if app.actor.stamina.exhausted and first_exhaustion<0:first_exhaustion=i
  # Derselbe freie Messort verhindert eine vorzeitige Pause durch Wandkontakt.
  app.actor.position.x=spawn.x;app.actor.position.z=spawn.z
 check(fastest>3.1 and fastest<=3.31,"Echter Sprint auf 3,3 statt 4,6 begrenzt")
 check(first_exhaustion>=365 and first_exhaustion<=395,"Dauerstick leert nach gut sechs Sekunden")
 check(app.actor.actual_speed<=2.01 and not app.actor.running,"Bei Erschöpfung bleibt nur Gehtempo")
 check(app.actor.hover<.01 and not app.actor.run_sheet,"Nocti landet bei Erschöpfung")
 app.actor.stop_input();await frames(2)
 app.actor.request_action("sit");var before: float=app.actor.stamina.value
 await frames(60)
 check(app.actor.state=="sit" and app.actor.stamina.value>=before+24.5,"Bodensitz regeneriert 25/Sekunde")
 var energy=app.actor.stamina; energy.value=20; energy.exhausted=true
 var bench: Dictionary={}
 for item in app.stage.furniture:
  if item.id=="park-bench":bench=item
 check(not bench.is_empty(),"Parkbank besitzt echten Sitzanker")
 app.actor.state="idle"; app.actor.position=app.stage.safe_position(bench.approach_position,app.actor.collider.shape.radius)
 await frames(12);await app.interact_with("seat","park-bench")
 check(app.actor.mounted and app.actor.get_meta("mounted_seat","")=="park-bench","Echte Bankaktion setzt Sitz und Tiefenanker")
 energy.value=20;await frames(60)
 check(app.actor.mounted and energy.value>=69.5 and energy.value<=70.5,"Banksitzen regeneriert 50/Sekunde")
 app._pause(true);before=energy.value;await frames(90)
 check(is_equal_approx(energy.value,before),"Menü pausiert Ausdauer")
 await app._mount_world("LIVING",Catalog.position_for("LIVING",Vector2(.7,.7)))
 check(app.actor.stamina==energy and is_equal_approx(energy.value,before),"Türwechsel setzt Vorrat nicht zurück")
 app._resume();await frames(2)
 check(app.hud.stamina_bar.visible and app.hud.stamina_label.text.begins_with("Ausdauer"),"Sichtbarer Balken mit Prozentwert")
 app._pause(true)
 for species in Catalog.data().characters:
  app.selected=species;await app._mount_world("LIVING",Catalog.position_for("LIVING",Vector2(.7,.7)))
  var actor=app.actor;actor.jump_clock=-1;actor.state="idle"
  check(actor.walk_texture!=null,"Neue Gehfolge geladen: "+species)
  for direction in [Vector2.RIGHT,Vector2.LEFT,Vector2.DOWN,Vector2.UP]:
   actor.facing=direction;actor.running=false;actor.run_blend=0
   var poses:={}
   for phase in 8:
    actor.gait=(phase+.1)/8.0;actor._update_frame(true);poses[actor.sprite.frame]=true
    check(actor.walk_sheet and actor.sprite.flip_h==(direction==Vector2.LEFT),"Gehansicht/Spiegelung: %s/%s"%[species,direction])
   check(poses.size()==8,"Acht Gehphasen ohne Ruhebild: %s/%s"%[species,direction])
   actor._update_frame(false)
   check(not actor.walk_sheet and actor.sprite.frame==actor.rest_frame(),"Anhalten stellt Ruhezeichnung wieder her")
  actor.stamina.value=10;actor.player_controlled=false;actor.enabled=true;actor.npc_vector=Vector2(.5,0)
  await frames(30)
  check(actor.stamina.value==10,"Bewohner benötigen keine Spielerausdauer: "+species)
  actor.enabled=false
 app.queue_free();await process_frame;DirAccess.remove_absolute("user://godot-tests/stamina-motion.json")
 print("Stamina motion: %s checks, %s failures"%[checks,failures]);quit(1 if failures else 0)
