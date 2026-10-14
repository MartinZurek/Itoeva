extends SceneTree
var checks := 0
var failures := 0
func _initialize() -> void: call_deferred("run")
func check(ok: bool,label: String) -> void:
	checks+=1
	if not ok: failures+=1; push_error(label)
func grab(view: SubViewport) -> Image:
	for i in 3: await process_frame
	await RenderingServer.frame_post_draw
	return view.get_texture().get_image()
func run() -> void:
	var viewport := SubViewport.new()
	viewport.size=Vector2i(128,128); viewport.own_world_3d=true
	viewport.render_target_update_mode=SubViewport.UPDATE_ALWAYS
	root.add_child(viewport)
	var camera := Camera3D.new()
	camera.projection=Camera3D.PROJECTION_ORTHOGONAL;camera.size=2;camera.position.z=4
	viewport.add_child(camera)
	var image := Image.create(32,32,false,Image.FORMAT_RGBA8)
	image.fill(Color.TRANSPARENT)
	image.fill_rect(Rect2i(4,4,24,24),Color(.4,.4,.4,1))
	var quad := MeshInstance3D.new()
	var plane := QuadMesh.new();plane.size=Vector2(2,2);quad.mesh=plane
	var material := ShaderMaterial.new()
	material.shader=load("res://shaders/painted_character.gdshader")
	material.set_shader_parameter("atlas",ImageTexture.create_from_image(image))
	material.set_shader_parameter("animation_time",0.0)
	quad.material_override=material;viewport.add_child(quad)
	var baseline := await grab(viewport)
	material.set_shader_parameter("local_light_count",1)
	material.set_shader_parameter("local_light_points",PackedVector4Array([Vector4(-.7,0,1.4,1.4),Vector4.ZERO,Vector4.ZERO,Vector4.ZERO]))
	material.set_shader_parameter("local_light_colors",PackedVector4Array([Vector4(1,.54,.18,.7),Vector4.ZERO,Vector4.ZERO,Vector4.ZERO]))
	material.set_shader_parameter("local_light_phases",PackedVector4Array([Vector4(2,1,0,0),Vector4.ZERO,Vector4.ZERO,Vector4.ZERO]))
	var lit := await grab(viewport)
	var left := lit.get_pixel(40,64).r-baseline.get_pixel(40,64).r
	var right := lit.get_pixel(88,64).r-baseline.get_pixel(88,64).r
	check(left>.02,"Nahe Quelle beleuchtet die echte Figur sichtbar")
	check(left>right*1.3,"Lichtseite folgt der Quelle")
	var clipped := 0
	var outside := 0
	for y in 128:
		for x in 128:
			var a:=baseline.get_pixel(x,y);var b:=lit.get_pixel(x,y)
			if b.r>.98: clipped+=1
			if (x<14 or x>114 or y<14 or y>114) and a!=b: outside+=1
	check(clipped==0,"Figurenzeichnung hat keine weiss ausgebrannten Flaechen")
	check(outside==0,"Figurenlicht respektiert transparente Silhouette")
	material.set_shader_parameter("animation_time",1.7)
	var moving:=await grab(viewport)
	check(moving.get_pixel(40,64)!=lit.get_pixel(40,64),"Feuerflackern erreicht lokale Figurenflaeche")
	material.set_shader_parameter("local_light_count",0)
	var off:=await grab(viewport)
	check(off.get_data()==baseline.get_data(),"Ohne Quellen bleibt die Originalzeichnung pixelgleich")
	var forest=load("res://scripts/forest_stage.gd").new()
	viewport.add_child(forest);forest.build()
	forest.set_arena_lights(0)
	var active := 0
	for source in forest.local_light_sources:
		if source.arena and float(source.gain)>0: active+=1
	check(active==0,"Erloeschende Arenaquellen beleuchten keine Figur weiter")
	forest.carried_light=false;forest.update_player(Vector3(125,.03,0))
	check(not forest.lantern.visible,"Ausgeschaltete Traglaterne verschwindet")
	forest.carried_light=true;forest.update_player(Vector3(125,.03,0))
	check(forest.lantern.visible,"Traglaterne ist im tiefen Wald sichtbar")
	print("Feuer und Figurenlicht GL: %s Checks, %s Fehler" % [checks,failures])
	quit(1 if failures else 0)
