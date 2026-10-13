extends SceneTree
const Forest=preload("res://scripts/forest_catalog.gd")
var viewport: SubViewport
var stage: Node3D
var mesh: MeshInstance3D
var checks:=0
var failures:=0
func _initialize() -> void: call_deferred("run")
func grab() -> Image:
	for i in 4: await process_frame
	await RenderingServer.frame_post_draw
	return viewport.get_texture().get_image()
func changed(a: Image,b: Image,rect: Rect2i) -> int:
	var count:=0
	for y in range(rect.position.y,rect.end.y):
		for x in range(rect.position.x,rect.end.x):
			if a.get_pixel(x,y)!=b.get_pixel(x,y): count+=1
	return count
func check(ok: bool,description: String) -> void:
	checks+=1
	if not ok: failures+=1;push_error(description)
func run() -> void:
	viewport=SubViewport.new()
	viewport.size=Vector2i(836,470)
	viewport.own_world_3d=true
	viewport.render_target_update_mode=SubViewport.UPDATE_ALWAYS
	root.add_child(viewport)
	stage=load("res://scripts/forest_stage.gd").new()
	viewport.add_child(stage)
	stage.build()
	for chunk in stage.chunks: chunk.visible=false
	stage.demon.visible=false
	stage.lantern.visible=false
	var camera:=Camera3D.new()
	camera.projection=Camera3D.PROJECTION_ORTHOGONAL
	camera.size=10
	camera.position=Vector3(0,0,20)
	stage.add_child(camera)
	mesh=MeshInstance3D.new()
	var quad:=QuadMesh.new()
	quad.size=Vector2(17.787,10)
	mesh.mesh=quad
	stage.add_child(mesh)
	var material: ShaderMaterial=stage.forest_materials.SHADOW_BROOK
	material.set_shader_parameter("blend_left",false)
	material.set_shader_parameter("animation_time",0.0)
	material.set_shader_parameter("carried_light",false)
	mesh.material_override=material
	var a:=await grab()
	material.set_shader_parameter("animation_time",1.7)
	var b:=await grab()
	check(changed(a,b,Rect2i(230,320,110,85))>200,"Schattenbach hat sichtbare Wasserbewegung")
	check(changed(a,b,Rect2i(420,225,55,15))==0,"Brueckendeck bewegt sich nicht mit Wasser")
	material=stage.forest_materials.TORCH_PATH
	material.set_shader_parameter("blend_left",false)
	material.set_shader_parameter("torch_gain",1.0)
	material.set_shader_parameter("light_gain",0.0)
	material.set_shader_parameter("carried_light",false)
	material.set_shader_parameter("animation_time",0.0)
	mesh.material_override=material
	a=await grab()
	material.set_shader_parameter("animation_time",1.7)
	b=await grab()
	check(changed(a,b,Rect2i(227,147,16,29))>50,"Gemalte Flamme lodert auch ohne Lichtflackern")
	check(changed(a,b,Rect2i(208,176,8,90))==0,"Holzpfosten bleibt fest")
	material=stage.forest_materials.SHADOW_CLEARING
	material.set_shader_parameter("blend_left",false)
	material.set_shader_parameter("carried_light",false)
	material.set_shader_parameter("animation_time",0.0)
	mesh.material_override=material
	stage.set_arena_lights(1)
	a=await grab()
	stage.set_arena_lights(0)
	b=await grab()
	check(changed(a,b,Rect2i(70,185,150,110))>500,"Arena verliert warmen Lichtschein beim Erloeschen")
	var pixel:=b.get_pixel(130,225)
	check(pixel.r<pixel.b*1.35,"Arena hat ohne Flammen keinen eingebrannten orangefarbenen Lichtfleck")
	material=stage.forest_materials.SHADOW_DEPTHS
	material.set_shader_parameter("blend_left",false)
	material.set_shader_parameter("carried_light",false)
	material.set_shader_parameter("animation_time",0.0)
	mesh.material_override=material
	a=await grab()
	material.set_shader_parameter("carried_light",true)
	material.set_shader_parameter("player",Vector3(0,-1,0))
	b=await grab()
	check(changed(a,b,Rect2i(300,220,200,130))>500,"Traglicht erhellt lokalen Ausschnitt im tiefen Wald")
	print("Wald GL: %s Checks, %s Fehler" % [checks,failures])
	quit(1 if failures else 0)
