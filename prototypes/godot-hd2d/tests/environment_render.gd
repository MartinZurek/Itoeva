extends SceneTree
const Ambience=preload("res://scripts/painted_ambience.gd")
var failures:=0
var checks:=0
func _initialize() -> void:call_deferred("run")
func check(ok: bool,label: String) -> void:
 checks+=1
 if not ok:failures+=1;push_error(label)
func grab(view: SubViewport) -> Image:
 for i in 3:await process_frame
 await RenderingServer.frame_post_draw
 return view.get_texture().get_image()
func run() -> void:
 if DisplayServer.get_name()=="headless":push_error("environment_render benoetigt OpenGL");quit(1);return
 DirAccess.make_dir_recursive_absolute("res://captures")
 var viewport:=SubViewport.new();viewport.size=Vector2i(768,256);viewport.own_world_3d=true;viewport.render_target_update_mode=SubViewport.UPDATE_ALWAYS;root.add_child(viewport)
 var camera:=Camera3D.new();camera.projection=Camera3D.PROJECTION_ORTHOGONAL;camera.size=2;camera.position.z=4;camera.current=true;viewport.add_child(camera)
 var quad:=MeshInstance3D.new();var plane:=QuadMesh.new();plane.size=Vector2(6,2);quad.mesh=plane;viewport.add_child(quad)
 for key in ["coast.png","coast-path.png","uplands.png","street-park-forest.png","mill-brook.png","sun-glade.png"]:
  var entry: Dictionary=Ambience.data().assets[key]
  var path: String=entry.get("asset","res://assets/world/"+key)
  var material:=Ambience.create_material(path,load(path));material.set_shader_parameter("light_count",0);material.set_shader_parameter("fire_count",0);quad.material_override=material
  material.set_shader_parameter("animation_time",0.0);var before:=await grab(viewport)
  material.set_shader_parameter("animation_time",18.0);var after:=await grab(viewport)
  var mask: Image=material.get_shader_parameter("water_mask").get_image()
  var water_changed:=0;var cloud_changed:=0;var dry_changed:=0
  for y in 256:
   for x in 768:
    var p:=Vector2i(clampi(int((x+.5)/768*mask.get_width()),1,maxi(1,mask.get_width()-2)),clampi(int((y+.5)/256*mask.get_height()),1,maxi(1,mask.get_height()-2)))
    var value:=mask.get_pixel(p.x,p.y)
    var changed:=before.get_pixel(x,y)!=after.get_pixel(x,y)
    if value.r>.8 and changed:water_changed+=1
    if value.g>.8 and changed:cloud_changed+=1
    if changed and value.r==0 and value.g==0:
     var dry:=true
     for dx in [-1,0,1]:
      for dy in [-1,0,1]:
       var near:=mask.get_pixel(p.x+dx,p.y+dy)
       if near.r>0 or near.g>0:dry=false
     if dry:dry_changed+=1
  check(water_changed>10 if not entry.get("water",[]).is_empty() else water_changed==0,"Wasser nur in markierten Fenstern: "+key)
  check(cloud_changed>30 if not entry.get("clouds",[]).is_empty() else cloud_changed==0,"Wolken reagieren dezent: "+key)
  check(dry_changed==0,"Gebaeude, Wege und trockene Flaechen bleiben pixelgleich: "+key+" "+str(dry_changed))
  after.save_png("res://captures/environment-motion-"+key)
  var frozen:=await grab(viewport);check(after.get_data()==frozen.get_data(),"Fester Zeitpunkt ist reproduzierbar: "+key)
 print("Environment OpenGL: %s Checks, %s Fehler"%[checks,failures]);quit(1 if failures else 0)
