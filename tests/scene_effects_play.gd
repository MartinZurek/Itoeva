extends SceneTree

const Catalog = preload("res://scripts/world_catalog.gd")
const Forest = preload("res://scripts/forest_catalog.gd")
const Stage = preload("res://scripts/world_stage.gd")
const ForestStage = preload("res://scripts/forest_stage.gd")
const Masks = preload("res://scripts/environment_masks.gd")

var failed := 0
var viewport: SubViewport
var surface: MeshInstance3D
var capture_dir := ""

func _initialize() -> void:
	run.call_deferred()

func run() -> void:
	capture_dir = OS.get_environment("ITOEVA_SCENE_CAPTURE")
	var rendered := DisplayServer.get_name() != "headless"
	if rendered:
		_build_probe()
	var world := Stage.new()
	var trail := ForestStage.new()
	trail.place = "FOREST_TRAIL"
	var cases := _cases()
	var reviewed: Dictionary = {}
	var effect_maps := 0
	for asset in cases:
		var entry: Dictionary = cases[asset]
		for place in entry.places:
			reviewed[place] = true
		var material := trail._painted_material(asset) if entry.forest else world._painted_material(asset, entry.seam)
		if entry.forest:
			trail._add_flames(entry.region, trail)
		_check(material.get_shader_parameter("painted_texture") != null, asset + " ohne Kulisse")
		var key: String = "sun-glade.png" if asset.ends_with("sun-glade-junctions.png") else asset.get_file()
		var expected_count: int = Masks.dynamics().get(key, {}).get("local_effects", []).size()
		if expected_count > 0:
			effect_maps += 1
			_check(material.get_shader_parameter("local_effect_count") == expected_count, asset + " · Ortseffekte fehlen")
		if rendered:
			await _review_frames(material, entry, asset)
		world.painted_materials.clear()
		trail.forest_materials.clear()
	world.free()
	trail.free()
	_check(reviewed.size() == 45, "Die Sichtprobe muss alle spielbaren Orte abdecken")
	_check(effect_maps == 8, "Die Sichtprobe muss alle lokalen Effekte abdecken")
	print("KULISSEN_%s · %d Orte, %d Bildmaterialien, %d Kulissen mit Ortseffekten (%s)" % [
		"OK" if failed == 0 else "FEHLER", reviewed.size(), cases.size(), effect_maps, "OpenGL" if rendered else "headless"])
	quit(1 if failed else 0)

func _cases() -> Dictionary:
	var cases: Dictionary = {}
	for region in Catalog.data().regions:
		cases[region.asset] = {"places": region.places, "forest": false, "seam": false, "inside": false}
	for room in Catalog.data().interiors:
		var room_data := Catalog.interior_for(room, Catalog.PROTAGONIST)
		cases[room_data.asset] = {"places": [room], "forest": false, "seam": false, "inside": not room_data.get("outdoor", false)}
	for index in range(Catalog.data().regions.size() - 1):
		cases["res://assets/world/seams/%s.png" % index] = {"places": [], "forest": false, "seam": true, "inside": false}
	for region in Forest.data().segments:
		cases[region.asset] = {"places": [region.id], "forest": true, "region": region, "seam": false, "inside": false}
	cases["res://assets/forest/three-roots-clearing.png"] = {"places": [], "forest": false, "seam": false, "inside": false}
	return cases

func _build_probe() -> void:
	viewport = SubViewport.new()
	viewport.size = Vector2i(384,216)
	viewport.own_world_3d = true
	viewport.render_target_update_mode = SubViewport.UPDATE_ALWAYS
	root.add_child(viewport)
	var camera := Camera3D.new()
	camera.projection = Camera3D.PROJECTION_ORTHOGONAL
	camera.size = 1.0
	camera.position.z = 2.0
	viewport.add_child(camera)
	camera.make_current()
	surface = MeshInstance3D.new()
	var quad := QuadMesh.new()
	quad.size = Vector2(16.0/9.0, 1.0)
	surface.mesh = quad
	viewport.add_child(surface)

func _review_frames(material: ShaderMaterial, entry: Dictionary, asset: String) -> void:
	surface.material_override = material
	var count: int = material.get_shader_parameter("local_effect_count") if material.get_shader_parameter("local_effect_count") != null else 0
	for phase in [["tag", 12.0], ["abend",19.4], ["nacht",0.0]]:
		var hour: float = phase[1]
		if entry.forest:
			var daylight := smoothstep(5.8,8.0,hour) * (1.0 - smoothstep(18.0,21.0,hour))
			material.set_shader_parameter("time_tint", Color(0.38,0.46,0.62).lerp(Color.WHITE, daylight))
			Stage.Ambience.set_water_time(material,hour,daylight)
		else:
			Stage.Ambience.set_time(material,hour,entry.inside)
		var image := await _capture(material, 3.7)
		_check(image.get_size() == viewport.size and not _flat(image), asset + " · leeres Bild " + phase[0])
		if not capture_dir.is_empty():
			image.save_png(capture_dir.path_join(asset.get_base_dir().get_file() + "-" + asset.get_file().get_basename() + "-" + phase[0] + ".png"))
		if count > 0:
			material.set_shader_parameter("local_effect_count", 0)
			var bare := await _capture(material, 3.7)
			material.set_shader_parameter("local_effect_count", count)
			_check_effect_bounds(image, bare, material.get_shader_parameter("local_effect_areas"), count, asset, phase[0])
	surface.material_override = null

func _check_effect_bounds(with_effect: Image, bare: Image, areas: PackedVector4Array, count: int, asset: String, phase: String) -> void:
	var changed := 0
	var spilled := 0
	var size := Vector2(with_effect.get_size())
	for y in with_effect.get_height():
		for x in with_effect.get_width():
			if with_effect.get_pixel(x,y) == bare.get_pixel(x,y):
				continue
			var uv := (Vector2(x,y) + Vector2(0.5,0.5)) / size
			var inside := false
			for i in count:
				var area := areas[i]
				var delta := (uv - Vector2(area.x,area.y)) / Vector2(area.z,area.w)
				inside = inside or delta.length_squared() <= 1.01
			spilled += int(not inside)
			changed += 1
	var dormant := phase == "nacht" and asset.ends_with("sun-glade-junctions.png")
	_check(spilled == 0, "%s · %s · %d veränderte Pixel außerhalb der Effektflächen" % [asset, phase, spilled])
	_check(changed == 0 if dormant else changed > 4, "%s · %s · unerwartete Effektfläche (%d Pixel)" % [asset, phase, changed])

func _capture(material: ShaderMaterial, clock: float) -> Image:
	material.set_shader_parameter("animation_time",clock)
	await process_frame
	await RenderingServer.frame_post_draw
	return viewport.get_texture().get_image()

func _flat(image: Image) -> bool:
	var first := image.get_pixel(20,20)
	for y in range(24, image.get_height()-12, 24):
		for x in range(24, image.get_width()-12, 24):
			if image.get_pixel(x,y) != first:
				return false
	return true

func _check(ok: bool, message: String) -> void:
	if not ok:
		failed += 1
		push_error(message)
