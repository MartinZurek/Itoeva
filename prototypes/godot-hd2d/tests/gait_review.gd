extends SceneTree
const Actor=preload("res://scripts/compact_actor.gd")
const Catalog=preload("res://scripts/world_catalog.gd")
var checks:=0
var failures:=0
func _initialize() -> void:call_deferred("run")
func check(ok: bool,label: String) -> void:
 checks+=1
 if not ok:failures+=1;push_error(label)
func difference(a: Image,b: Image) -> float:
 var small_a: Image=a.duplicate();var small_b: Image=b.duplicate()
 small_a.resize(64,64);small_b.resize(64,64)
 var one:=small_a.get_data();var two:=small_b.get_data();var total:=0.0
 for i in one.size():total+=absf(float(one[i])-float(two[i]))/255.0
 return total/one.size()
func run() -> void:
 if DisplayServer.get_name()=="headless":push_error("Gangabnahme benoetigt OpenGL");quit(1);return
 var viewport:=SubViewport.new();viewport.size=Vector2i(512,512);viewport.transparent_bg=true
 viewport.own_world_3d=true;viewport.render_target_update_mode=SubViewport.UPDATE_ALWAYS;root.add_child(viewport)
 var world:=Node3D.new();viewport.add_child(world)
 var camera:=Camera3D.new();camera.projection=Camera3D.PROJECTION_ORTHOGONAL;camera.size=4.4
 camera.position=Vector3(0,1.2,7);world.add_child(camera);camera.current=true
 DirAccess.make_dir_recursive_absolute("res://captures/calm-gait")
 for species in Catalog.STARTERS:
  var actor=Actor.new();actor.species=species;actor.enabled=false;world.add_child(actor)
  await process_frame;actor.shadow.visible=false
  for direction in [Vector2.RIGHT,Vector2.DOWN,Vector2.UP]:
   actor.facing=direction;actor.state="idle";actor.jump_clock=-1
   for sprint in [false,true]:
    actor.running=sprint;actor.run_blend=1.0 if sprint else 0.0;actor.body_weight=1
    actor.hover=.48 if sprint and species=="nocti" else 0.0
    var cadence: float=(actor.RUN_SPEED if sprint else actor.WALK_SPEED)/actor._stride_length()
    check(cadence>1.2 and cadence<2.0,"Dynamischer, lesbarer Geh-/Joggingtakt: "+species)
    var first: Image
    var previous: Image
    var hashes:={}
    var min_head:=Vector2(INF,INF);var max_head:=Vector2(-INF,-INF)
    var min_root:=Vector2(INF,INF);var max_root:=Vector2(-INF,-INF)
    var min_lift:=INF;var max_lift:=-INF
    for tick in 48:
     actor.clock=tick/24.0;actor.gait=actor.clock*cadence
     actor._update_frame(true)
     await process_frame;await RenderingServer.frame_post_draw
     var rendered:=viewport.get_texture().get_image()
     var visible:=rendered.get_used_rect()
     check(visible.has_area() and visible.position.x>8 and visible.end.x<504 and visible.position.y>8 and visible.end.y<504,"Vollstaendige aktive Kontur: %s/%s/%s"%[species,sprint,tick])
     check(actor.skin_mesh.visible and actor.painted_material.get_shader_parameter("rig_active"),"Ganzkoerper-Hautnetz wird wirklich gerendert")
     if tick==0:first=rendered
     if previous!=null:check(difference(previous,rendered)<.045,"Kein Einzelbildsprung bei 24 Hz: "+species)
     previous=rendered
     hashes[hash(rendered.get_data())]=true
     var pose: Dictionary=actor.body_pose
     var neck: Vector2=actor.FullBody.torso(actor.FullBody.point(actor.body_rigs[species+"-flight" if actor.run_sheet else species].rows[actor.sprite.frame/8].neck),pose)
     min_head=min_head.min(neck);max_head=max_head.max(neck)
     min_root=min_root.min(pose.offset);max_root=max_root.max(pose.offset)
     min_lift=minf(min_lift,pose.lift);max_lift=maxf(max_lift,pose.lift)
     if tick%6==0 or "all-frames" in OS.get_cmdline_user_args():
      rendered.save_png("res://captures/calm-gait/%s-%s-%s-%02d.png"%[species,int(direction.y),"run" if sprint else "walk",tick])
    check(hashes.size()>=40,"Kontinuierliche Koerper- und Gliedmassenphasen: "+species)
    if species=="starlet":
     check(max_lift-min_lift>16,"Sichtbarer Schwebe-Huepfbogen statt Sternbeine")
     check(actor.painted_material.get_shader_parameter("step_leg_count")==0,"Keine alternierenden Sternspitzen")
    else:
     check((max_root-min_root).length()>5,"Becken bewegt sich statt festem Stickerkoerper: "+species)
     check((max_head-min_head).length()>4,"Kopf folgt dem ganzen Koerper: "+species)
  actor.body_weight=1;actor._update_frame(false)
  check(actor.body_weight<1,"Weicher Auslauf beim Anhalten")
  actor.state="sit";actor._update_frame(false)
  check(not actor.skin_mesh.visible and not actor.painted_material.get_shader_parameter("rig_active"),"Sitzpose beendet das Hautnetz")
  actor.shadow.queue_free();actor.queue_free();await process_frame
 viewport.queue_free();await process_frame
 print("Full body gait OpenGL: %s checks, %s failures"%[checks,failures]);quit(1 if failures else 0)
