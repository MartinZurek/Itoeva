extends SceneTree
const Actor = preload("res://scripts/compact_actor.gd")
var checks := 0
var failures := 0
func _initialize() -> void: call_deferred("run")
func check(ok: bool,label: String) -> void:
 checks += 1
 if not ok: failures += 1;push_error(label)
func run() -> void:
 var viewport := SubViewport.new()
 viewport.size = Vector2i(640,640);viewport.transparent_bg = true;viewport.own_world_3d = true
 viewport.render_target_update_mode = SubViewport.UPDATE_ALWAYS;root.add_child(viewport)
 var world := Node3D.new();viewport.add_child(world)
 var camera := Camera3D.new();camera.projection = Camera3D.PROJECTION_ORTHOGONAL
 camera.size = 3.4;camera.position = Vector3(0,1.2,7);world.add_child(camera);camera.current = true
 var render := DisplayServer.get_name() != "headless"
 DirAccess.make_dir_recursive_absolute("res://captures/companions-volume")
 for species in ["nocti","starlet","wyrmling"]:
  var actor = Actor.new();actor.species = species;actor.enabled = false;world.add_child(actor)
  await process_frame;actor.shadow.visible = false;actor.recovery_clock = 1
  actor.set_volume_enabled(true)
  check(actor.volume_enabled and actor.volume.visible,"3D aktiviert: "+species)
  for direction in [Vector2.RIGHT,Vector2.LEFT,Vector2.DOWN,Vector2.UP]:
   actor.facing = direction
   var lowest := INF
   var highest := -INF
   for tick in 32:
    actor.gait = float(tick)/32;actor.body_weight = 1;actor._update_frame(true)
    check(actor.volume.visible and not actor.sprite.visible and not actor.skin_mesh.visible,"Genau eine Darstellung")
    check(actor.volume.surface.mesh.get_aabb().size.z > .03,"Geschlossenes Volumen mit Tiefe")
    actor.volume.skeleton.force_update_all_bone_transforms()
    for index in actor.volume.skeleton.get_bone_count():
     check(actor.volume.skeleton.get_bone_global_pose(index).is_finite(),"Endliche Gelenkpose")
    check(actor.visual_screen_rect(camera).has_area(),"Interaktionsrechteck")
    lowest = minf(lowest,actor.sprite.position.y);highest = maxf(highest,actor.sprite.position.y)
    if species == "starlet":
     check(not actor.volume.joints.has("hip0") and not actor.volume.joints.has("foot0"),"Stern ohne Beine")
     check(actor.body_pose.feet.is_empty(),"Kein alternierender Fussschritt")
    if render and tick in [0,8,16,24]:
     await process_frame;await RenderingServer.frame_post_draw
     var frame := viewport.get_texture().get_image()
     check(frame.get_used_rect().has_area(),"Sichtbare Renderausgabe")
     frame.save_png("res://captures/companions-volume/%s-%d-%d.png"%[species,int(direction.angle()*10),tick])
   if species == "starlet":check(highest-lowest > .12,"Sichtbarer Huepf-/Schwebebogen")
  actor.facing = Vector2.RIGHT
  actor.running = true;actor.run_blend = 1;actor.hover = .15 if species == "nocti" else 0
  actor._update_frame(true)
  check(actor.volume.visible,"Sprint bleibt im Volumen")
  if species == "nocti":check(actor.volume.active_key.begins_with("nocti-flight"),"Eigene Fluggeometrie")
  actor.running = false;actor.run_blend = 0;actor.hover = 0
  for i in 60:actor._update_frame(false)
  check(is_zero_approx(actor.body_weight),"Stillstand stoppt Gliederbewegung")
  for action in ["sit","roll"]:
   actor.state = action;actor._update_frame(false)
   check(not actor.volume.visible and actor.sprite.visible,"Sonderaktion bleibt verfuegbar")
  actor.state = "idle";actor.jump_clock = .1;actor._update_frame(true)
  check(not actor.volume.visible and actor.sprite.visible,"Sprungknopf bleibt verfuegbar")
  actor.jump_clock = -1;actor.set_volume_enabled(false)
  check(not actor.volume.visible and actor.sprite.visible,"Abschalten stellt Originalpfad wieder her")
  actor.shadow.queue_free();actor.queue_free();await process_frame
 viewport.queue_free();await process_frame
 print("Companion volume: %d checks, %d failures"%[checks,failures]);quit(1 if failures else 0)
