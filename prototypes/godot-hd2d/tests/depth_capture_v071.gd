extends SceneTree
const Catalog=preload("res://scripts/world_catalog.gd")
const Forest=preload("res://scripts/forest_catalog.gd")
var app: Node3D
var checks:=0
var failures:=0
func check(ok: bool,label: String) -> void:
 checks+=1
 if not ok:failures+=1;push_error(label)
func shot() -> Image:
 for i in 4:await process_frame
 await RenderingServer.frame_post_draw
 return root.get_texture().get_image()
func _initialize() -> void:call_deferred("run")
func grab(label: String) -> void:
 for i in 8:await process_frame
 await RenderingServer.frame_post_draw
 root.get_texture().get_image().save_png("res://captures/v071-"+label+".png")
func run() -> void:
 DirAccess.make_dir_recursive_absolute("res://captures");DirAccess.make_dir_recursive_absolute("user://godot-tests")
 app=load("res://scenes/full_world.tscn").instantiate();app.save_path="user://godot-tests/v071-capture.json";DirAccess.remove_absolute(app.save_path);root.add_child(app)
 while app.loading_world:await process_frame
 app.world_clock.utc_now_override=1792058400;app.world_clock.utc_offset=0
 await app.start_game()
 for place in ["MILL_BROOK","SUN_GLADE","MIST_WATCH"]:
  await app._mount_world(place,Catalog.position_for(place,Vector2(.52,.60)))
  app._resume()
  for i in 25:await physics_frame
  app._pause(true);app._update_camera(10);await grab(place.to_lower())
  if place=="MILL_BROOK":
   for material in app.stage.painted_materials:material.set_shader_parameter("animation_time",0.0)
   var with_shadow:=await shot()
   for creature in app.actors:creature.shadow.visible=false
   var without_shadow:=await shot()
   var darkened:=0
   for y in range(480,600):
    for x in range(280,780):
     var a:=with_shadow.get_pixel(x,y);var b:=without_shadow.get_pixel(x,y)
     if b.r+b.g+b.b-a.r-a.g-a.b>.03:darkened+=1
   check(darkened>20,"Kontaktschatten sind im echten Render sichtbar: %s Pixel"%darkened)
   for creature in app.actors:creature.shadow.visible=true
   app._show_conversation("miro",app.Residents.appointment("miro",12));await grab("miro-dialogue");app._resume()
 await app._mount_world("FOREST_TRAIL",Forest.position_for(4,.53,.55));app._resume()
 for i in 25:await physics_frame
 app._pause(true);app._update_camera(10);await grab("bridge")
 var masked:=await shot()
 app.actor.painted_material.set_shader_parameter("has_mask",false);await grab("bridge-unmasked")
 var unmasked:=await shot()
 app.actor.sprite.visible=false;app.stage.lantern.visible=false
 var background:=await shot()
 var covered:=0
 for y in range(180,340):
  for x in range(560,710):
   var a:=masked.get_pixel(x,y);var b:=unmasked.get_pixel(x,y);var c:=background.get_pixel(x,y)
   if Vector3(a.r-b.r,a.g-b.g,a.b-b.b).length()>.12 and Vector3(a.r-c.r,a.g-c.g,a.b-c.b).length()<.05:covered+=1
 check(covered>30,"Brüstung erscheint vor Figur im echten Render: %s Pixel"%covered)
 app.actor.sprite.visible=true
 await app._mount_world("OUTDOOR",Catalog.position_for("POND",Vector2(.70,.5)));app._resume()
 for i in 25:await physics_frame
 app._pause(true);app._update_camera(10);await grab("pond")
 app.show_map();await process_frame;app.ui.get_child(0)._select("MILL_BROOK");await grab("atlas")
 app.queue_free();await process_frame;DirAccess.remove_absolute("user://godot-tests/v071-capture.json")
 print("Depth render v071: %s checks, %s failures"%[checks,failures]);quit(1 if failures else 0)
