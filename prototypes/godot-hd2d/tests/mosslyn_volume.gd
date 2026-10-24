extends SceneTree
const Actor=preload("res://scripts/compact_actor.gd")
var checks:=0
var failures:=0
func _initialize() -> void: call_deferred("run")
func check(ok: bool, label: String) -> void:
 checks+=1
 if not ok: failures+=1;push_error(label)
func run() -> void:
 var viewport:=SubViewport.new();viewport.size=Vector2i(640,640);viewport.transparent_bg=true;viewport.own_world_3d=true
 viewport.render_target_update_mode=SubViewport.UPDATE_ALWAYS;root.add_child(viewport)
 var world:=Node3D.new();viewport.add_child(world)
 var camera:=Camera3D.new();camera.projection=Camera3D.PROJECTION_ORTHOGONAL
 camera.size=3.1;camera.position=Vector3(0,1.05,7);world.add_child(camera);camera.current=true
 var actor=Actor.new();actor.species="mosslyn";actor.enabled=false;world.add_child(actor)
 await process_frame;actor.shadow.visible=false
 actor.facing=Vector2.RIGHT;actor.state="idle";actor.jump_clock=-1;actor.recovery_clock=1
 actor._update_frame(true)
 check(actor.painted_cycle_active,"Standard bleibt die bisherige Zeichnungsfolge")
 actor.set_volume_enabled(true)
 check(actor.volume.visible and not actor.sprite.visible,"Volumen ersetzt das Sprite ohne Doppelbild")
 check(actor.volume.skeleton.get_bone_count()==12,"Zwoelf importierte Gelenke")
 check(actor.volume.surface.mesh.get_faces().size()/3 < 30000,"Unter 30000 Dreiecke")
 var render:=DisplayServer.get_name()!="headless"
 DirAccess.make_dir_recursive_absolute("res://captures/mosslyn-volume")
 for direction in [Vector2.RIGHT,Vector2.LEFT]:
  actor.facing=direction
  for tick in 32:
   actor.gait=float(tick)/32;actor.volume.motion_weight=1;actor._update_frame(true)
   var skeleton: Skeleton3D=actor.volume.skeleton
   skeleton.force_update_all_bone_transforms()
   for leg in 2:
    var name: String="rear" if leg==0 else "front"
    var foot: Vector3=skeleton.get_bone_global_pose(actor.volume.joints[name+"_foot"]).origin
    check(Vector2(foot.x,foot.y).distance_to(actor.volume.feet[leg])<.0001,"IK-Fuss und Gelenkkette stimmen ueberein")
   check(actor.volume.transform.is_finite(),"Endliche Transformation")
   check(actor.visual_screen_rect(camera).has_area(),"Bildrechteck fuer Interaktionen")
   check(not actor.skin_mesh.visible,"Kein zweites Hautnetz")
   if render:
    await process_frame;await RenderingServer.frame_post_draw
    var frame:=viewport.get_texture().get_image()
    check(frame.get_used_rect().has_area(),"Volumen tatsaechlich gerendert")
    frame.save_png("res://captures/mosslyn-volume/%s-%02d.png"%["right" if direction.x>0 else "left",tick])
 actor.volume.motion_weight=1
 actor._update_frame(false)
 check(actor.volume.motion_weight<1,"Anhalten laeuft weich in Ruhe aus")
 for i in 30: actor._update_frame(false)
 check(is_zero_approx(actor.volume.motion_weight),"Kein weiterlaufender Gehzyklus im Stand")
 for direction in [Vector2.DOWN,Vector2.UP]:
  actor.facing=direction;actor._update_frame(true)
  check(actor.sprite.visible and not actor.volume.visible,"Tiefenansicht hat eine echte vorhandene Zeichnung")
 actor.facing=Vector2.RIGHT
 for action in ["sit","roll"]:
  actor.state=action;actor._update_frame(false)
  check(actor.sprite.visible and not actor.volume.visible,"Sonderaktion stellt Sprite wieder her")
 actor.state="idle";actor.running=true;actor._update_frame(true)
 check(actor.painted_cycle_active and not actor.volume.visible,"Sprint behält die vorhandene gemalte Folge")
 actor.running=false;actor.jump_clock=.1;actor._update_frame(true)
 check(actor.sprite.visible and not actor.volume.visible,"Sprung stellt Sprite wieder her")
 actor.jump_clock=-1;actor.set_volume_enabled(false);actor._update_frame(true)
 check(actor.sprite.visible and not actor.volume.visible and actor.painted_cycle_active,"Schalter stellt bisherigen Pfad wieder her")
 var other=Actor.new();other.species="nocti";other.enabled=false;world.add_child(other)
 other.set_volume_enabled(true);check(not other.volume_enabled and not is_instance_valid(other.volume),"Andere Spieler bleiben unveraendert")
 other.shadow.queue_free();actor.shadow.queue_free();viewport.queue_free();await process_frame
 print("Mooslyn volume: %d checks, %d failures"%[checks,failures]);quit(1 if failures else 0)
