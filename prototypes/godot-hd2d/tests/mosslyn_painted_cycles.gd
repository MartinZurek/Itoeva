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
   var frames:={};var pixel_size:=0.0
   for tick in 28:
    actor.gait=(tick+.1)/28.0;actor._update_frame(true)
    frames[actor.sprite.frame]=true
    if tick==0:pixel_size=actor.sprite.pixel_size
    check(actor.painted_cycle_active,"Gemalte Folge aktiv")
    check(actor.sprite.hframes==7 and actor.sprite.vframes==4,"7x4 Raster")
    check(actor.sprite.frame==tick,"Chronologische Phase")
    check(actor.sprite.flip_h==(direction.x<0),"Ganzes Bild samt Umhang gespiegelt")
    check(is_equal_approx(pixel_size,actor.sprite.pixel_size),"Keine Skalierung zwischen Phasen")
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
   actor.gait=1.0;actor._update_frame(true)
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
 actor.shadow.queue_free();viewport.queue_free();await process_frame
 print("Mooslyn painted cycles: %s checks, %s failures"%[checks,failures])
 quit(1 if failures else 0)
