extends SceneTree
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
 DirAccess.make_dir_recursive_absolute("user://godot-tests")
 var app = load("res://scenes/full_world.tscn").instantiate()
 app.save_path = "user://godot-tests/roaming-slopes.json"
 DirAccess.remove_absolute(app.save_path); root.add_child(app)
 while app.loading_world: await process_frame
 app.selected = "fennec"
 await app._mount_world("OUTDOOR", Catalog.position_for("COAST_PATH", Vector2(.15,.5))); app._resume()
 for resident in app.actors:
  if resident != app.actor: resident.enabled = false; resident.collider.disabled = true
 # Seitlicher Daumen allein muss die ansteigenden und fallenden Wegkonturen schaffen.
 for pair in [[36.0,44.0],[44.0,36.0],[13.3,20.5],[20.5,13.3],[54.0,66.0],[66.0,54.0],[143.0,157.0],[157.0,143.0]]:
  var limits: Vector2 = app.stage._limits_at(pair[0])
  app.actor.position = app.stage.safe_position(Vector3(pair[0],.03,Catalog.image_z((limits.x+limits.y)*.5)),app.actor.collider.shape.radius)
  app.actor.stop_input(); await frames(8)
  app.actor.stamina.value=100;app.actor.stamina.exhausted=false
  var sign_x: float = signf(pair[1]-pair[0]); var dry := true; var elapsed := 0
  for i in 650:
   app.actor.touch_vector = Vector2(sign_x*.8,0)
   await physics_frame
   elapsed += 1
   dry = dry and app.stage.is_walkable(app.actor.position,app.actor.collider.shape.radius,true)
   if sign_x*(app.actor.position.x-float(pair[1])) >= -.15: break
  print("Hang ",pair," frames ",elapsed," position ",app.actor.position)
  check(dry,"Hang hält Fuß auf trockenem Boden: "+str(pair))
  check(sign_x*(app.actor.position.x-float(pair[1])) >= -.15,"Seitlicher Daumen erreicht Hangziel: %s / %s"%[pair,app.actor.position])
  app.actor.stop_input()
  if pair == [13.3,20.5]:check(elapsed <= 240,"Steigung bleibt mit gedrosseltem Sprint erreichbar")
 # Auch beide kompakten Figuren und alle ursprünglichen Wesen benutzen dieselbe Hangführung.
 for species in Catalog.data().characters:
  app.selected=species
  await app._mount_world("OUTDOOR",Catalog.position_for("SWAMP",Vector2(.2,.5)));app._resume()
  for resident in app.actors:
   if resident!=app.actor:resident.enabled=false;resident.collider.disabled=true
  var limits: Vector2=app.stage._limits_at(13.3)
  app.actor.position=app.stage.safe_position(Vector3(13.3,.03,Catalog.image_z((limits.x+limits.y)*.5)),app.actor.collider.shape.radius)
  app.actor.stop_input();await frames(8)
  app.actor.stamina.value=100;app.actor.stamina.exhausted=false
  var dry:=true
  for tick in 240:
   app.actor.touch_vector=Vector2(.8,0);await physics_frame
   dry=dry and app.stage.is_walkable(app.actor.position,app.actor.collider.shape.radius,true)
   if app.actor.position.x>=20.35:break
  check(dry and app.actor.position.x>=20.35,"Alle acht Wesen überwinden die Steigung: "+species)
  app.actor.stop_input()
 app.queue_free(); await process_frame; DirAccess.remove_absolute("user://godot-tests/roaming-slopes.json")
 print("Roaming/slopes: %s checks, %s failures"%[checks,failures]); quit(1 if failures else 0)
