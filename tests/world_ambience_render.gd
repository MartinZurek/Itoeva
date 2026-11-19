extends SceneTree
const Ambience = preload("res://scripts/painted_ambience.gd")
func _initialize() -> void:
	call_deferred("run")
func run() -> void:
	root.size = Vector2i(1280,720)
	var output := OS.get_cmdline_user_args()[0]
	DirAccess.make_dir_recursive_absolute(output)
	var stage := Node3D.new()
	root.add_child(stage)
	var camera := Camera3D.new()
	camera.projection = Camera3D.PROJECTION_ORTHOGONAL
	camera.size = 2.0
	camera.position.z = 2.0
	stage.add_child(camera)
	var surface := MeshInstance3D.new()
	var quad := QuadMesh.new()
	surface.mesh = quad
	stage.add_child(surface)
	for asset in ["res://assets/world/coast.png", "res://assets/expansion/sun-glade-junctions.png", "res://assets/expansion/ash-ascent.png"]:
		var texture := load(asset) as Texture2D
		assert(texture != null)
		quad.size = Vector2(3.55, 3.55*texture.get_height()/texture.get_width())
		var material := Ambience.create_material(asset,texture)
		surface.material_override = material
		for hour in [12.0,21.0]:
			Ambience.set_time(material,hour,false)
			for clock in [0.0,4.0]:
				material.set_shader_parameter("animation_time",clock)
				for frame in 4: await process_frame
				await RenderingServer.frame_post_draw
				var file := "%s-%s-%s.png" % [asset.get_file().get_basename(),int(hour),int(clock)]
				root.get_texture().get_image().save_png(output.path_join(file))
	print("WORLD_AMBIENCE_RENDER_OK: water, clouds, fire; day/night; 12 frames")
	quit()
