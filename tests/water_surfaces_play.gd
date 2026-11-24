extends SceneTree

const Ambience = preload("res://scripts/painted_ambience.gd")
const Masks = preload("res://scripts/environment_masks.gd")
const ForestStage = preload("res://scripts/forest_stage.gd")
const TEST_SAMPLES := [
	["res://assets/world/coast.png", Vector2(.38,.535), Vector2(.50,.84)],
	["res://assets/world/coast-path.png", Vector2(.49,.86), Vector2(.52,.70)],
	["res://assets/world/seams/0.png", Vector2(.51,.85), Vector2(.42,.73)],
	["res://assets/expansion/sun-glade-junctions.png", Vector2(.58,.572), Vector2(.53,.75)],
	["res://assets/expansion/mill-brook.png", Vector2(.49,.56), Vector2(.46,.74)],
	["res://assets/expansion/alder-ford.webp", Vector2(.48,.85), Vector2(.47,.72)],
	["res://assets/expansion/ring-weir.webp", Vector2(.79,.86), Vector2(.48,.74)],
	["res://assets/expansion/mill-run.webp", Vector2(.51,.86), Vector2(.40,.68)],
	["res://assets/forest/three-roots-clearing.png", Vector2(.60,.78), Vector2(.61,.61)],
]

func _initialize() -> void:
	run.call_deferred()

func run() -> void:
	var rendered := DisplayServer.get_name() != "headless"
	for sample in TEST_SAMPLES:
		var asset: String = sample[0]
		var material := Ambience.create_material(asset, load(asset), asset.ends_with("/0.png"))
		if asset.ends_with("/coast.png"):
			material.set_shader_parameter("shore_enabled", true)
			material.set_shader_parameter("shore_texture", load("res://assets/world/coast-shore-v026.png"))
		await _check_surface(material, sample[1], sample[2], asset.get_file(), rendered)
	var stage := ForestStage.new()
	root.add_child(stage)
	var forest := stage._painted_material("res://assets/forest/brook.png")
	await _check_surface(forest, Vector2(.33,.85), Vector2(.43,.45), "Schattenbach", rendered)
	stage.free()

	assert(Masks.motion_texture("expedition.png") != null)
	assert(Masks.motion_texture("living.png") == null)
	var dry := Ambience.create_material("res://assets/interiors/living.png", load("res://assets/interiors/living.png"))
	assert(dry.get_shader_parameter("has_water") == false)
	assert(dry.get_shader_parameter("surface_sheen").w > 0.0)
	Ambience.set_time(dry, 1.0, true)
	assert(is_zero_approx(dry.get_shader_parameter("sheen_daylight")))
	var dry_terrain := Ambience.create_material("res://assets/world/uplands.png", load("res://assets/world/uplands.png"))
	assert(dry_terrain.get_shader_parameter("has_environment_motion") == true)
	if rendered:
		await _check_motion(dry_terrain, Vector2(.55,.14), Vector2(.50,.81), "Hochlandhimmel")
		Ambience.set_time(dry, 12.0, true)
		await _check_motion(dry, Vector2(.29,.83), Vector2(.78,.80), "Fensterlicht")
	print("WASSER_OK · %d Kulissen, Ufergrenzen und Licht geprüft (%s)" % [TEST_SAMPLES.size() + 1, "OpenGL" if rendered else "headless"])
	quit()

func _check_surface(material: ShaderMaterial, wet: Vector2, dry: Vector2, name: String, rendered: bool) -> void:
	assert(material.get_shader_parameter("has_water") == true, name)
	var optics: Vector4 = material.get_shader_parameter("water_optics")
	assert(optics.y > 0.0 and optics.z > 0.0, name)
	var mask: Texture2D = material.get_shader_parameter("water_mask")
	var image := mask.get_image()
	assert(image.get_format() == Image.FORMAT_RG8 and image.get_data_size() <= 196000, name)
	assert(_sample(image, wet).r > .4, name + " Wasser")
	assert(_sample(image, dry).r < .08, name + " trockener Boden")
	if rendered:
		await _check_motion(material, wet, dry, name)

func _check_motion(material: ShaderMaterial, moving: Vector2, still: Vector2, name: String) -> void:
	var view := SubViewport.new()
	view.size = Vector2i(384, 216)
	view.own_world_3d = true
	view.render_target_update_mode = SubViewport.UPDATE_ALWAYS
	root.add_child(view)
	var camera := Camera3D.new()
	camera.projection = Camera3D.PROJECTION_ORTHOGONAL
	camera.size = 1.0
	camera.position.z = 2.0
	view.add_child(camera)
	camera.make_current()
	var surface := MeshInstance3D.new()
	var quad := QuadMesh.new()
	quad.size = Vector2(16.0/9.0, 1.0)
	surface.mesh = quad
	surface.material_override = material
	view.add_child(surface)
	var first := await _capture(view, material, 0.0)
	var second := await _capture(view, material, 3.5)
	var capture := OS.get_environment("ITO_WATER_CAPTURE_DIR")
	if not capture.is_empty():
		first.save_png(capture.path_join(name + "-0.png"))
		second.save_png(capture.path_join(name + "-3.png"))
	assert(_difference(first, second, moving) > .004, name + " keine sichtbare Bewegung")
	assert(_difference(first, second, still) < .002, name + " trockene Fläche bewegt")
	if material.get_shader_parameter("has_water"):
		var optics: Vector4 = material.get_shader_parameter("water_optics")
		material.set_shader_parameter("water_optics", Vector4(optics.x, 0, optics.z, optics.w))
		var without_reflection := await _capture(view, material, 3.5)
		material.set_shader_parameter("water_optics", optics)
		assert(_difference(second, without_reflection, moving) > .001, name + " keine sichtbare Spiegelung")
		assert(_difference(second, without_reflection, still) < .001, name + " Spiegelung auf trockenem Boden")
	view.free()

func _capture(view: SubViewport, material: ShaderMaterial, clock: float) -> Image:
	material.set_shader_parameter("animation_time", clock)
	await process_frame
	await RenderingServer.frame_post_draw
	return view.get_texture().get_image()

func _sample(image: Image, point: Vector2) -> Color:
	var pixel := Vector2i(point * Vector2(image.get_size()))
	return image.get_pixel(pixel.x, pixel.y)

func _difference(a: Image, b: Image, point: Vector2) -> float:
	var pixel := Vector2i(point * Vector2(a.get_size()))
	var total := 0.0
	for y in range(pixel.y - 4, pixel.y + 5):
		for x in range(pixel.x - 4, pixel.x + 5):
			var c := a.get_pixel(x, y)
			var d := b.get_pixel(x, y)
			total += (absf(c.r-d.r) + absf(c.g-d.g) + absf(c.b-d.b))/3.0
	return total / 81.0
