extends SceneTree

const Actor=preload("res://scripts/compact_actor.gd")
var checks:=0
var failures:=0

func _initialize() -> void:call_deferred("run")

func check(ok: bool,label: String) -> void:
 checks+=1
 if not ok:failures+=1;push_error(label)

func run() -> void:
 var viewport:=SubViewport.new();viewport.size=Vector2i(512,512)
 viewport.transparent_bg=true;viewport.own_world_3d=true
 viewport.render_target_update_mode=SubViewport.UPDATE_ALWAYS;root.add_child(viewport)
 var world:=Node3D.new();viewport.add_child(world)
 var camera:=Camera3D.new();camera.projection=Camera3D.PROJECTION_ORTHOGONAL
 camera.size=4.4;camera.position=Vector3(0,1.2,7);world.add_child(camera);camera.current=true
 var actor=Actor.new();actor.species="mosslyn";actor.enabled=false;world.add_child(actor)
 await process_frame;actor.shadow.visible=false
 var render:=DisplayServer.get_name()!="headless"
 DirAccess.make_dir_recursive_absolute("res://captures/mosslyn-painted")
 for sprint in [false,true]:
  actor.running=sprint;actor.run_blend=1.0 if sprint else 0.0
  for direction in [Vector2.RIGHT,Vector2.LEFT]:
   actor.facing=direction;actor.state="idle";actor.jump_clock=-1
   var frames:={}
   var expected_head: float=actor.base_pixel_size*109.0*actor.Catalog.relative_height("mosslyn")*100.64/181.0
   for tick in 28:
    actor.gait=4.0*(tick+.1)/28.0;actor._update_frame(true)
    frames[actor.sprite.frame]=true
    check(actor.painted_cycle_active,"Gemalte Folge aktiv")
    check(actor.sprite.hframes==7 and actor.sprite.vframes==4,"7x4 Raster")
    check(actor.sprite.frame==tick,"Chronologische Phase")
    check(actor.sprite.flip_h==(direction.x<0),"Ganzes Bild samt Umhang gespiegelt")
    var registration: Dictionary=actor.painted_cycles[actor.painted_cycle_mode].size_registration
    var rendered_head: float=float(registration.frames[tick].span)*actor.sprite.pixel_size
    check(absf(rendered_head-expected_head)<.001,"Gleiche anatomische Groesse in allen Geh-/Sprintphasen und beiden Richtungen")
    var head_region: Array=registration.frames[tick].region
    var head_x: float=(float(head_region[0])+float(head_region[2])*.5-actor.cell_size.x*.5)*actor.sprite.pixel_size*(-1.0 if direction.x<0 else 1.0)+actor.painted_x_offset
    var expected_x: float=actor.base_pixel_size*109.0*actor.Catalog.relative_height("mosslyn")*float(registration.reference_center_offset)/181.0*(-1.0 if direction.x<0 else 1.0)
    check(absf(head_x-expected_x)<.001,"Kein seitlicher Positionssprung durch verschobene Quellzellen")
    check(not actor.skin_mesh.visible and not actor.painted_material.get_shader_parameter("rig_active"),"Kein zweites verformendes Hautnetz")
    check(actor.painted_cycle_mode==("run" if sprint else "walk"),"Gehen und Sprint getrennt")
    check(actor.visual_screen_rect(camera).has_area(),"Begrenzung folgt neuem Raster")
    check(actor.light_center().is_finite(),"Lichtanker bleibt gueltig")
    if render:
     await process_frame;await RenderingServer.frame_post_draw
     var image:=viewport.get_texture().get_image()
     var visible:=image.get_used_rect()
     check(visible.has_area() and visible.position.x>4 and visible.end.x<508 and visible.position.y>4 and visible.end.y<508,"Figur sichtbar im Renderbild")
     image.save_png("res://captures/mosslyn-painted/%s-%s-%02d.png"%["run" if sprint else "walk","right" if direction.x>0 else "left",tick])
   check(frames.size()==28,"Alle 28 Einzelbilder erreichbar")
   actor.gait=4.0;actor._update_frame(true)
   check(actor.sprite.frame==0,"Zyklus schliesst bei Phase null")
   actor.facing=Vector2.DOWN;actor._update_frame(true)
   check(not actor.painted_cycle_active and actor.sprite.hframes==8,"Frontansicht stellt altes Raster wieder her")
   actor.facing=direction;actor._update_frame(true)
   actor.state="sit";actor._update_frame(false)
   check(not actor.painted_cycle_active and actor.sprite.texture==actor.world_texture,"Sitzen stellt Originalatlas wieder her")
   actor.state="idle";actor.jump_clock=-1;actor._update_frame(true)
   actor.jump_clock=.1;actor._update_frame(true)
   check(not actor.painted_cycle_active and actor.sprite.texture==actor.world_texture,"Springen stellt Originalatlas wieder her")
   actor.jump_clock=-1;actor._update_frame(true);actor.body_weight=0;actor._update_frame(false)
   check(not actor.painted_cycle_active and actor.sprite.texture==actor.world_texture,"Anhalten stellt Standpose wieder her")
   check(is_zero_approx(actor.painted_x_offset),"Standzeichnung erhaelt keinen alten Seitenversatz")
   check(absf(actor.sprite.pixel_size*100.64-expected_head)<.001,"Stand und Bewegung verwenden dieselbe Kopf-Groessenreferenz")
   if render:
    await process_frame;await RenderingServer.frame_post_draw
    viewport.get_texture().get_image().save_png("res://captures/mosslyn-painted/idle-%s.png"%["right" if direction.x>0 else "left"])
 # Gleiche Strecke ergibt bei 30/60/120Hz denselben Bildstand. Eine Sekunde
 # Volltempo darf nicht mehr 38-50 der gemalten Bilder durchrasen lassen.
 actor.facing=Vector2.RIGHT;actor.state="idle";actor.jump_clock=-1
 for sprint in [false,true]:
  actor.running=sprint;actor.run_blend=1.0 if sprint else 0.0
  var speed: float=3.85 if sprint else 2.35
  var stride: float=2.15 if sprint else 1.75
  var expected_frame:=12 if sprint else 9
  for hz in [30,60,120]:
   actor.gait=0
   for tick in hz:
    actor.gait+=(speed/float(hz))/stride
    actor._update_frame(true)
   check(actor.sprite.frame==expected_frame,"Streckenabhaengiger, ruhiger Takt unabhaengig von FPS")
   var before: float=actor.gait
   for tick in hz:actor.clock+=1.0/hz;actor._update_frame(true)
   check(actor.sprite.frame==expected_frame and actor.gait==before,"Ohne neue Strecke schreitet die Animation nicht fort")
 actor.shadow.queue_free();viewport.queue_free();await process_frame
 print("Mooslyn painted cycles: %s checks, %s failures"%[checks,failures])
 quit(1 if failures else 0)
