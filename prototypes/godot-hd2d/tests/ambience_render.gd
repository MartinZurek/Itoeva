# Real GL rendering required: run without --headless; Linux can use Xvfb.
# Only test captures are saved under user://godot-tests/ambience/.
extends SceneTree
const Ambience=preload("res://scripts/painted_ambience.gd")
var checks=0
var failures=0
var mesh: MeshInstance3D
var camera: Camera3D
var viewport: SubViewport
func _initialize(): call_deferred("run")
func check(ok,label):
 checks+=1
 if not ok:
  failures+=1
  push_error(label)
func grab():
 for i in 3: await process_frame
 await RenderingServer.frame_post_draw
 return viewport.get_texture().get_image()
func changed(a:Image,b:Image,rect:Rect2i):
 var total=0
 for y in range(rect.position.y,rect.end.y):
  for x in range(rect.position.x,rect.end.x):
   if a.get_pixel(x,y)!=b.get_pixel(x,y): total+=1
 return total
func run():
 DirAccess.make_dir_recursive_absolute("user://godot-tests/ambience")
 viewport=SubViewport.new()
 viewport.size=Vector2i(768,256)
 viewport.own_world_3d=true
 viewport.render_target_update_mode=SubViewport.UPDATE_ALWAYS
 root.add_child(viewport)
 var stage=Node3D.new()
 viewport.add_child(stage)
 camera=Camera3D.new()
 camera.projection=Camera3D.PROJECTION_ORTHOGONAL
 camera.size=2
 camera.position=Vector3(0,0,4)
 camera.current=true
 stage.add_child(camera)
 mesh=MeshInstance3D.new()
 var quad=QuadMesh.new()
 quad.size=Vector2(6,2)
 mesh.mesh=quad
 stage.add_child(mesh)
 var texture=load("res://assets/world/coast.png")
 var mat=Ambience.create_material("res://assets/world/coast.png",texture)
 mat.set_shader_parameter("animation_time",0.0)
 mesh.material_override=mat
 var coast:Image=await grab()
 mat.set_shader_parameter("shore_enabled",true)
 mat.set_shader_parameter("shore_texture",load("res://assets/world/coast-shore-v026.png"))
 var shore:Image=await grab()
 shore.save_png("user://godot-tests/ambience/shore.png")
 check(changed(coast,shore,Rect2i(0,0,768,192))==0,"Originalbild ueber UV.75 pixelidentisch bei Shore-Ueberblendung")
 check(changed(coast,shore,Rect2i(0,214,768,42))>300,"Neue trockene Kueste unterhalb UV.83 sichtbar")
 mat.set_shader_parameter("shore_enabled",false)
 mat.set_shader_parameter("animation_time",1.7)
 var ripple:Image=await grab()
 check(changed(coast,ripple,Rect2i(270,132,84,13))>5,"Hintergrundmeer bewegt sich sichtbar")
 check(changed(coast,ripple,Rect2i(65,170,82,19))==0,"Gemalter Holzsteg bleibt unbewegt")
 check(changed(coast,ripple,Rect2i(0,0,768,40))==0,"Blauer Himmel ausserhalb der Wolkenmaske bleibt unbewegt")
 mat=Ambience.create_material("res://assets/world/expedition.png",load("res://assets/world/expedition.png"))
 Ambience.set_time(mat,23,false)
 mat.set_shader_parameter("animation_time",0.0)
 mesh.material_override=mat
 var fire:Image=await grab()
 mat.set_shader_parameter("animation_time",1.7)
 var flicker:Image=await grab()
 flicker.save_png("user://godot-tests/ambience/fire-night.png")
 check(changed(fire,flicker,Rect2i(71,142,68,50))>10,"Gemaltes Lagerfeuer flackert lokal")
 check(changed(fire,flicker,Rect2i(450,30,120,50))==0,"Grotte wird nicht vom Feuer als Ganzes animiert")
 mat=Ambience.create_material("res://assets/world/seams/1.png",load("res://assets/world/seams/1.png"))
 Ambience.set_time(mat,23,false)
 mat.set_shader_parameter("animation_time",0.0)
 mesh.material_override=mat
 var seam:Image=await grab()
 check(mat.get_shader_parameter("light_count")==7,"Naht besitzt eigene statt verdeckte Lampen")
 # Opaque geometry is still hidden by a nearer opaque surface in alpha prepass.
 var blocker=MeshInstance3D.new()
 blocker.mesh=quad
 blocker.position.z=1
 var black=StandardMaterial3D.new()
 black.shading_mode=BaseMaterial3D.SHADING_MODE_UNSHADED
 black.albedo_color=Color.BLACK
 blocker.material_override=black
 stage.add_child(blocker)
 var covered:Image=await grab()
 check(covered.get_pixel(384,128).r<.01,"ALPHA-Material respektiert opaque Tiefenverdeckung")
 blocker.queue_free()
 viewport.queue_free()
 await process_frame
 print("Ambience GL render: ",checks," Checks, ",failures," Fehler")
 quit(1 if failures else 0)
