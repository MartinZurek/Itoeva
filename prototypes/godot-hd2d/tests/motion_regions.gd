extends SceneTree
const Actor = preload("res://scripts/compact_actor.gd")
const Catalog = preload("res://scripts/world_catalog.gd")
var checks := 0
var failures := 0
func _initialize() -> void: call_deferred("run")
func check(ok: bool,label: String) -> void:
 checks+=1
 if not ok: failures+=1;push_error(label)
func run() -> void:
 if DisplayServer.get_name()=="headless":push_error("motion_regions benötigt OpenGL");quit(1);return
 var viewport:=SubViewport.new();viewport.size=Vector2i(512,512);viewport.transparent_bg=true
 viewport.own_world_3d=true;viewport.render_target_update_mode=SubViewport.UPDATE_ALWAYS;root.add_child(viewport)
 var world:=Node3D.new();viewport.add_child(world)
 var camera:=Camera3D.new();camera.projection=Camera3D.PROJECTION_ORTHOGONAL;camera.size=6.0
 camera.position=Vector3(0,1.25,7);world.add_child(camera);camera.current=true
 DirAccess.make_dir_recursive_absolute("res://captures/motion-regions")
 for species in Catalog.STARTERS:
  var actor=Actor.new();actor.species=species;actor.enabled=false;world.add_child(actor)
  await process_frame;actor.shadow.visible=false
  for mode in ["world","walk","run"]:
   if mode=="run" and actor.run_texture==null:continue
   actor._use_motion_sheet(mode=="run",mode=="walk")
   var count:=32 if mode=="world" else 24
   for pose in count:
    for flip in [false,true]:
     actor.sprite.frame=pose;actor.sprite.flip_h=flip;actor._update_sprite_scale()
     await process_frame;await RenderingServer.frame_post_draw
     var rendered:=viewport.get_texture().get_image();var visible:=rendered.get_used_rect()
     var expected: Rect2=actor.visual_screen_rect(camera)
     check(absf(visible.size.x-expected.size.x)<6 and absf(visible.size.y-expected.size.y)<6,"Vollständige Kontur: %s/%s/%s/%s"%[species,mode,pose,flip])
     check(Vector2(visible.position).distance_to(expected.position)<6,"Spiegelung/Fußanker: %s/%s/%s/%s"%[species,mode,pose,flip])
     if not flip and (mode!="world" or pose in [3,7,8,10]):rendered.save_png("res://captures/motion-regions/%s-%s-%02d.png"%[species,mode,pose])
  actor.shadow.queue_free();actor.queue_free();await process_frame
 viewport.queue_free();await process_frame
 print("Motion regions OpenGL: %s checks, %s failures"%[checks,failures]);quit(1 if failures else 0)
