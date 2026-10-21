extends SceneTree
const Catalog=preload("res://scripts/world_catalog.gd")
const Forest=preload("res://scripts/forest_catalog.gd")
var app: Node3D
var report: Dictionary={}
func _initialize() -> void:call_deferred("run")
func vertices(node: Node) -> int:
 var count:=0
 if node is MeshInstance3D and node.mesh is ArrayMesh:
  for i in node.mesh.get_surface_count():count+=node.mesh.surface_get_array_len(i)
 for child in node.get_children():count+=vertices(child)
 return count
func settle() -> void:
 for i in 4:await process_frame
 await RenderingServer.frame_post_draw
func run() -> void:
 var args:=OS.get_cmdline_user_args()
 var label:=args[0] if not args.is_empty() else "current"
 DirAccess.make_dir_recursive_absolute("res://captures");DirAccess.make_dir_recursive_absolute("user://godot-tests")
 var began:=Time.get_ticks_usec()
 app=load("res://scenes/full_world.tscn").instantiate();app.save_path="user://godot-tests/environment-profile.json"
 DirAccess.remove_absolute(app.save_path);root.add_child(app)
 while app.loading_world:await process_frame
 report["ready_ms"]=(Time.get_ticks_usec()-began)/1000.0
 report["preloaded_textures"]=app.textures.size()
 var pixels:=0
 for texture in app.textures.values():pixels+=texture.get_width()*texture.get_height()
 report["preloaded_pixels"]=pixels
 report["texture_memory_bytes"]=RenderingServer.get_rendering_info(RenderingServer.RENDERING_INFO_TEXTURE_MEM_USED)
 began=Time.get_ticks_usec();app.selected="mosslyn";await app.start_game()
 report["first_world_ms"]=(Time.get_ticks_usec()-began)/1000.0
 report["outdoor_vertices"]=vertices(app.stage)
 print("Environment startup: ",JSON.stringify(report))
 var results: Array=[]
 if "startup-only" not in args:
  for scene in [{"id":"coast","room":"OUTDOOR","point":Catalog.position_for("BEACH",Vector2(.60,.55))},{"id":"park","room":"OUTDOOR","point":Catalog.position_for("PARK",Vector2(.5,.55))},{"id":"forest","room":"OUTDOOR","point":Catalog.position_for("FOREST",Vector2(.5,.55))},{"id":"clouds","room":"OUTDOOR","point":Catalog.position_for("PLAINS",Vector2(.5,.55))},{"id":"mill","room":"MILL_BROOK","point":Catalog.position_for("MILL_BROOK",Vector2(.5,.55))},{"id":"brook","room":"FOREST_TRAIL","point":Forest.position_for(4,.5)}]:
   began=Time.get_ticks_usec()
   if app.room!=scene.room:await app._mount_world(scene.room,scene.point)
   var mount_ms:=(Time.get_ticks_usec()-began)/1000.0
   app.actor.position=app.stage.safe_position(scene.point,app.actor.collider.shape.radius);app.actor.velocity=Vector3.ZERO
   app._pause(true);app.hud.visible=false;app.stage.set_time(12.0)
   for actor in app.actors:actor.visible=false;actor.shadow.visible=false
   for marker in app.regional_markers:marker.art.visible=false;marker.shadow.visible=false
   for material in app.stage.painted_materials:material.set_shader_parameter("animation_time",0.0);material.set_shader_parameter("carried_gain",0.0)
   if app.room=="FOREST_TRAIL":
    app.forest_encounter.model.phase="done";app.stage.lantern.visible=false
    for material in app.stage.forest_materials.values():material.set_shader_parameter("animation_time",0.0);material.set_shader_parameter("carried_gain",0.0)
   app.yaw=0;app._update_camera(10.0);await settle()
   root.get_texture().get_image().save_png("res://captures/env-"+label+"-"+scene.id+".png")
   var samples: Array[float]=[]
   for pass_index in 3:
    began=Time.get_ticks_usec()
    for i in 8:await process_frame;await RenderingServer.frame_post_draw
    samples.append((Time.get_ticks_usec()-began)/8000.0)
   samples.sort()
   results.append({"id":scene.id,"mount_ms":mount_ms,"vertices":vertices(app.stage),"frame_ms_median":samples[1]})
   print("Environment scene: ",JSON.stringify(results.back()))
   app.yaw=8;app._update_camera(10.0);await settle()
   root.get_texture().get_image().save_png("res://captures/env-"+label+"-"+scene.id+"-yaw.png")
 report["scenes"]=results;report["renderer"]=RenderingServer.get_video_adapter_name()
 var f:=FileAccess.open("res://captures/environment-profile-"+label+".json",FileAccess.WRITE);f.store_string(JSON.stringify(report,"  ")+"\n");f.close()
 DirAccess.remove_absolute(app.save_path);app.queue_free();await process_frame;quit()
