extends SceneTree
const Catalog=preload("res://scripts/world_catalog.gd")
const Forest=preload("res://scripts/forest_catalog.gd")
const Residents=preload("res://scripts/world_residents.gd")
var checks:=0
var failures:=0
var app: Node3D
func _initialize() -> void:call_deferred("run")
func check(ok: bool,label: String) -> void:
 checks+=1
 if not ok:failures+=1;push_error(label)
func frames(n: int=12) -> void:
 for i in n:await physics_frame
func run() -> void:
 app=load("res://scenes/full_world.tscn").instantiate()
 app.save_path="user://godot-tests/v071-depth.json"
 DirAccess.make_dir_recursive_absolute("user://godot-tests");DirAccess.remove_absolute(app.save_path);root.add_child(app)
 while app.loading_world:await process_frame
 app.world_clock.utc_now_override=1792058400;app.world_clock.utc_offset=0
 await app.start_game()
 # Beide Richtungen benutzen echte Türaktionen mit gespeichertem Standort.
 await app._mount_world("SUN_GLADE",Catalog.position_for("SUN_GLADE",Vector2(.3,.6)));app._resume();await frames()
 for destination in ["MILL_BROOK","SUN_GLADE"]:
  var door: Dictionary={}
  for candidate in app.stage.doors:
   if candidate.to==destination:door=candidate
  check(not door.is_empty(),"Mühlenbach besitzt Hin- und Rückweg")
  app.actor.position=app.stage.safe_position(door.position);app.actor.velocity=Vector3.ZERO;await frames()
  await app.interact_with("door",door.from+":"+door.to);await frames()
  check(app.room==destination and app.actor.is_on_floor(),"Tür führt auf tragfähigen Boden: "+destination)
 await app._mount_world("MILL_BROOK",Catalog.position_for("MILL_BROOK",Vector2(.5,.6)));app._resume();await frames(25)
 var miro: Node3D
 for creature in app.actors:
  if creature.species=="miro":miro=creature
 check(is_instance_valid(miro),"Miro ist tagsüber an der Mühle sichtbar")
 check(miro.is_on_floor() and app.stage.is_walkable(miro.position,miro.collider.shape.radius*.8),"Miro steht auf trockenem, freiem Weg")
 var start: Vector3=miro.position;await frames(180)
 check(miro.position.distance_to(start)>.05,"Miro bewegt sich zwischen seinen Arbeitsposen")
 app._show_conversation("miro",miro.get_meta("appointment"));await process_frame
 check(app.paused and app.dialog_open,"Miro ist ansprechbar")
 app._resume()
 for hour in range(24):
  var entry:=Residents.appointment("miro",hour)
  check(entry.place in ["CRAFT","MILL_BROOK","SUN_GLADE"],"Miro bleibt im hellen Weltteil")
 # Alle vorhandenen Figuren: echte Bildunterkante, verschiedene Gangarten/Ansichten.
 for species in Catalog.data().characters:
  app.selected=species
  await app._mount_world("MILL_BROOK",Catalog.position_for("MILL_BROOK",Vector2(.56,.6)))
  app._resume();await frames(15);app._pause(true)
  var actor: Node3D=app.actor
  var compact: bool=Catalog.data().characters[species].get("compact",false)
  var poses: Array=[0,3,7,8,11,14,15,16,20,24,25,26,29] if compact else [9,12,68,76,110,114,121,130,137,142,148,149]
  var half_height: float=actor.sprite.texture.get_height()/float(actor.sprite.vframes)*.5
  for pose in poses:
   actor.sprite.frame=pose;actor.mounted=false;actor._update_sprite_scale()
   var bottom: float=half_height+actor.sprite.offset.y-float(actor.frame_bounds[pose].end.y)
   check(absf(bottom)<.01,"Pose versinkt und schwebt nicht: %s/%s"%[species,pose])
   actor.mount_seat(Vector3(0,1,0),actor.position);actor._update_sprite_scale()
   check(absf(actor.sprite.offset.y-(half_height-2))<.01,"Fester Sitzanker nach jeder Vorpose: %s/%s"%[species,pose])
   actor.leave_seat()
  actor.position=app.stage.safe_position(Catalog.position_for("MILL_BROOK",Vector2(.56,.6)));actor._update_shadow()
  check(actor.shadow.visible,"Kontaktschatten unter jeder Spezies")
  check(actor.shadow.material_override.render_priority==2,"Schatten werden über gemaltem Boden gezeichnet")
  var ground_opacity: float=actor.shadow.material_override.get_shader_parameter("opacity")
  actor.position.y+=1.7;actor._update_shadow()
  check(float(actor.shadow.material_override.get_shader_parameter("opacity"))<ground_opacity,"Schatten wird beim Sprung weicher")
 # Vordere Brüstung muss treffen, ihre freie Öffnung bleibt durchsichtig.
 await app._mount_world("FOREST_TRAIL",Forest.position_for(4,.53,.55));app._resume();await frames();app._pause(true)
 var mask: Image=app.stage.bridge_mask.get_image()
 check(mask.get_pixel(int(.528*836),int(.49*470)).g>.9,"Vorderer mittlerer Brückenpfosten verdeckt")
 check(mask.get_pixel(int(.58*836),int(.452*470)).g>.9,"Oberer vorderer Handlauf verdeckt")
 check(mask.get_pixel(int(.58*836),int(.477*470)).g<.1,"Freier Zwischenraum bleibt offen")
 check(mask.get_pixel(int(.365*836),int(.45*470)).g<.1,"Hinteres Geländer schneidet keine vorderen Körper mehr aus")
 # Nachgerüstete echte Holzgeometrie registriert dieselbe Tiefenmaske wie die Kulisse.
 await app._mount_world("OUTDOOR",Catalog.position_for("POND",Vector2(.5,.5)));app._resume();await frames();app._pause(true)
 var coast_mask: Image=app.stage.actor_occlusion(app.actor.position).mask.get_image()
 check(coast_mask.get_pixel(int(.175*1920),int(.72*640)).g>.9,"Gemalter vorderer Küstenstegpfosten verdeckt Figuren")
 check(coast_mask.get_pixel(int(.148*1920),int(.71*640)).g<.1,"Freier Platz zwischen Küstenstegpfosten bleibt sichtbar")
 var rail_pieces:=0
 for entry in app.stage.occlusion_pieces:
  if entry.piece.get("contours",[]).size()==1 and not entry.piece.has("id") and entry.asset.ends_with("coast.png"):rail_pieces+=1
 check(rail_pieces>=12,"Stegpfosten und Handläufe haben Konturen statt ungeschütztem Übermalen")
 app.queue_free();await process_frame;DirAccess.remove_absolute("user://godot-tests/v071-depth.json")
 print("Landscape depth v071: %s checks, %s failures"%[checks,failures]);quit(1 if failures else 0)
