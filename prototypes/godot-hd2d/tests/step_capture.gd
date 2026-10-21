extends SceneTree
const Actor=preload("res://scripts/compact_actor.gd")
const Catalog=preload("res://scripts/world_catalog.gd")
func _initialize() -> void:call_deferred("run")
func run() -> void:
 var viewport:=SubViewport.new();viewport.size=Vector2i(512,512);viewport.transparent_bg=true
 viewport.own_world_3d=true;viewport.render_target_update_mode=SubViewport.UPDATE_ALWAYS;root.add_child(viewport)
 var world:=Node3D.new();viewport.add_child(world)
 var camera:=Camera3D.new();camera.projection=Camera3D.PROJECTION_ORTHOGONAL;camera.size=3.3
 camera.position=Vector3(0,1.2,7);world.add_child(camera);camera.current=true
 DirAccess.make_dir_recursive_absolute("/tmp/itoeva-steps")
 for species in ["mosslyn","nocti","starlet","wyrmling","fennec","gloop","puffling","hootlet"]:
  var actor=Actor.new();actor.species=species;actor.enabled=false;world.add_child(actor)
  await process_frame;actor.shadow.visible=false
  for row in 3:
   actor.facing=[Vector2.RIGHT,Vector2.DOWN,Vector2.UP][row]
   actor.state="idle";actor.jump_clock=-1;actor.hover=0;actor.running=false;actor.run_blend=0
   for tick in 32:
    actor.clock=tick/32.0;actor.gait=actor.clock
    actor._update_frame(true)
    await process_frame;await RenderingServer.frame_post_draw
    viewport.get_texture().get_image().save_png("/tmp/itoeva-steps/%s-%s-%02d.png"%[species,row,tick])
  actor.shadow.queue_free();actor.queue_free();await process_frame
 viewport.queue_free();await process_frame;quit()
