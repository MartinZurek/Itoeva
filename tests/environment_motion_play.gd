extends SceneTree
const Ambience=preload("res://scripts/painted_ambience.gd")
const Masks=preload("res://scripts/environment_masks.gd")
var app: Node3D
func _initialize() -> void:
 run.call_deferred()
func run() -> void:
 OS.set_environment("MOTION_M4","1")
 root.size=Vector2i(1280,720)
 app=load("res://scenes/gameplay.tscn").instantiate()
 root.add_child(app)
 while app.loading_world: await process_frame
 app.save_path="user://environment-test-%s.json" % OS.get_process_id()
 app.save_data={"characters":{},"settings":{}}
 var checks:=0
 for pair in [["SUN_GLADE","thorn_moth",Vector2.ZERO],["MIST_WATCH","seal_weaver",Vector2(.5,1.0)],
  ["MILL_BROOK","bark_guardian",Vector2(.5,1.0)],["ASH_ASCENT","glut_imp",Vector2(.5,1.0)],
  ["SEAL_COURT","vharok",Vector2(.5,1.0)],["RELAY_COURT","relay_warden",Vector2(.5,1.0)],
  ["BEACH","tide_crab",Vector2(1.0,0.0)]]:
  var expected: Vector2=pair[2]
  app.dusk=expected.y>0.0
  var utc_time: String="2026-01-01T23:00:00" if expected.x==1.0 else "2026-01-01T11:00:00"
  app.world_clock.utc_now_override=Time.get_unix_time_from_datetime_string(utc_time)
  await app.visit_test_place(pair[0])
  var active:=false
  for m in app.stage.painted_materials:
   if m.get_shader_parameter("has_environment_motion")==true: active=true
  assert(active,"Missing world motion: "+pair[0])
  checks+=1
  var id: String=pair[1]
  await app.test_encounter(pair[0],id)
  var material: ShaderMaterial=app.forest_encounter.view.arena_picture.material
  assert(material.get_shader_parameter("has_environment_motion")==true)
  assert(material.get_shader_parameter("water_time").is_equal_approx(expected), "Arena must keep local time")
  assert(is_equal_approx(material.get_shader_parameter("fire_gain"),lerpf(.78,1.25,expected.x)))
  if pair[0]=="BEACH":
   assert(material.get_shader_parameter("shore_enabled")==true)
   assert(material.get_shader_parameter("shore_texture")!=null)
  if pair[0] in ["SEAL_COURT","ASH_ASCENT"]:
   assert(material.get_shader_parameter("fire_count")>0)
  checks+=1
  if DisplayServer.get_name()!="headless":
   var probe:=TextureRect.new()
   probe.texture=app.forest_encounter.view.background
   probe.material=material
   probe.expand_mode=TextureRect.EXPAND_IGNORE_SIZE
   probe.size=Vector2(1280,720)
   probe.z_index=4096
   root.add_child(probe)
   var images: Array[Image]=[]
   for t in [0.0,4.0]:
    material.set_shader_parameter("animation_time",t)
    await process_frame
    await RenderingServer.frame_post_draw
    var image:=root.get_texture().get_image()
    images.append(image)
    if not OS.get_environment("ITO_ENV_CAPTURE_DIR").is_empty():
     image.save_png(OS.get_environment("ITO_ENV_CAPTURE_DIR").path_join("environment-"+pair[0]+"-"+str(int(t))+".png"))
   assert(images[0].get_data()!=images[1].get_data())
   assert(images[0].get_pixel(640,650)==images[1].get_pixel(640,650),"Dry foreground moved")
   probe.queue_free()
   await process_frame
 for key: String in Masks.dynamics():
  if not Masks.has_motion(key):
   assert(Masks.motion_texture(key)==null)
   continue
  var image:=Masks.motion_texture(key).get_image()
  assert(image.get_size()==Masks.MOTION_SIZE)
  var foreground:=image.get_pixel(image.get_width()/2,image.get_height()-4)
  assert(foreground.r==0 and foreground.g==0)
  assert(Masks.motion_texture(key)==Masks.motion_texture(key))
  checks+=3
 print("ENVIRONMENT_MOTION_OK ",checks)
 app.queue_free()
 await process_frame
 quit()
