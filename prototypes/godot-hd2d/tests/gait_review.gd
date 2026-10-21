extends SceneTree
const Actor=preload("res://scripts/compact_actor.gd")
const Catalog=preload("res://scripts/world_catalog.gd")
var checks:=0
var failures:=0
func _initialize() -> void:call_deferred("run")
func check(ok: bool,label: String) -> void:
 checks+=1
 if not ok:failures+=1;push_error(label)
func patch_rect(actor, camera: Camera3D, box: Rect2) -> Rect2i:
 var points: Array[Vector2]=[]
 for point in [box.position,box.end]:
  var local_x: float=point.x-actor.cell_size.x*.5
  var local_y: float=actor.cell_size.y*.5+actor.sprite.offset.y-point.y
  points.append(camera.unproject_position(actor.global_position+actor.sprite.position+camera.global_basis.x*local_x*actor.sprite.pixel_size+camera.global_basis.y*local_y*actor.sprite.pixel_size))
 var minimum:=points[0].min(points[1]);var maximum:=points[0].max(points[1])
 return Rect2i(Vector2i(minimum),Vector2i(maximum-minimum)).intersection(Rect2i(0,0,512,512))
func difference(a: Image,b: Image,box: Rect2i) -> float:
 var total:=0.0
 for y in range(box.position.y,box.end.y):
  for x in range(box.position.x,box.end.x):
   var one:=a.get_pixel(x,y);var two:=b.get_pixel(x,y)
   total+=absf(one.r*one.a-two.r*two.a)+absf(one.g*one.a-two.g*two.a)+absf(one.b*one.a-two.b*two.a)+absf(one.a-two.a)
 return total/maxf(1,box.size.x*box.size.y*4)
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
    actor.running=sprint;actor.run_blend=1.0 if sprint else 0.0
    actor.hover=.48 if sprint and species=="nocti" else 0.0
    check(actor._stride_length()>=2.1,"Ruhige volle Schrittlaenge: "+species)
    var first: Image
    var head:=Rect2i()
    var details:={}
    for tick in 48:
     actor.clock=tick/24.0;actor.gait=actor.clock*(1.18 if sprint else .93)
     actor._update_frame(true)
     await process_frame;await RenderingServer.frame_post_draw
     var rendered:=viewport.get_texture().get_image()
     check(rendered.get_used_rect().has_area(),"Vollstaendige sichtbare Phase: %s/%s/%s"%[species,sprint,tick])
     if tick==0:
      first=rendered
      var reference: Rect2=Rect2(actor.frame_bounds[actor.sprite.frame/8*8])
      head=patch_rect(actor,camera,Rect2(reference.position+reference.size*Vector2(.35,.15),reference.size*Vector2(.30,.27)))
      if species=="nocti" and sprint:
       var row: int=actor.sprite.frame/8
       var window: Array=actor.gait_registration[species].run.rows[row].window
       head=patch_rect(actor,camera,Rect2(window[0]+20,window[1]+20,window[2]-40,window[3]-40))
     # Ein stabiler Koerper darf nicht durch viele unterschiedliche Zeichnungen
     # flackern. Der geringe Atemhub bleibt unterhalb dieser sichtbaren Schwelle.
     check(difference(first,rendered,head)<.07,"Ruhige Kopf-/Koerpermitte: %s/%s/%s"%[species,sprint,tick])
     details[hash(rendered.get_data())]=true
     if tick%6==0 or "all-frames" in OS.get_cmdline_user_args():
      rendered.save_png("res://captures/calm-gait/%s-%s-%s-%02d.png"%[species,int(direction.y),"run" if sprint else "walk",tick])
    check(details.size()>32,"Sichtbare Zwischenphasen statt acht hart wechselnder Bilder: "+species)
  actor.shadow.queue_free();actor.queue_free();await process_frame
 viewport.queue_free();await process_frame
 print("Calm gait OpenGL: %s checks, %s failures"%[checks,failures]);quit(1 if failures else 0)
