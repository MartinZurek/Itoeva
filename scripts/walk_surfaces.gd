extends RefCounted

const Catalog = preload("res://scripts/world_catalog.gd")
const Forest = preload("res://scripts/forest_catalog.gd")
static var cache: Dictionary = {}

static func data() -> Dictionary:
	if cache.is_empty(): cache = JSON.parse_string(FileAccess.get_file_as_string("res://data/walk_surfaces.json"))
	return cache

static func native_point(stage: Node3D, point: Array, region: Dictionary = {}) -> Vector3:
	if stage.place == "OUTDOOR":
		return Vector3(float(region.origin) + float(point[0]) / 1920.0 * float(region.width), 0.03, stage.image_z(point[1]))
	if stage.place == "FOREST_TRAIL":
		var x: float = float(region.origin) + float(point[0]) * float(region.width)
		var raw: = Forest.raw_band(region, float(point[0]))
		var shared: = Forest.limits(x)
		return Vector3(x, 0.03, Catalog.image_z(float(point[1]) * 640.0 + (shared.x + shared.y - raw.x - raw.y) * 0.5))
	return Vector3((float(point[0]) / 480.0 - 0.5) * stage.width, 0.03, stage.image_z(point[1], 270.0))

static func polygon(stage: Node3D, points: Array, region: Dictionary = {}) -> PackedVector2Array:
	var result: = PackedVector2Array()
	for p in points:
		var at: = native_point(stage, p, region)
		result.append(Vector2(at.x, at.z))
	return result

static func extras(stage: Node3D, region: Dictionary) -> Array:
	var result: Array = []
	for entry in data().extras.get(stage.place, []):
		if stage.place == "OUTDOOR" and entry.region != region.places[0]: continue
		if stage.place == "FOREST_TRAIL" and Forest.data().segments[int(entry.index)].id != region.get("id", ""): continue
		result.append(entry)
	return result

static func merge_into(polygons: Array, addition: PackedVector2Array) -> Array:

	var pending: = addition
	var result: Array = []
	for shape in polygons:
		var joined: = Geometry2D.merge_polygons(shape, pending)
		if joined.size() == 1: pending = joined[0]
		else: result.append(shape)
	result.append(pending)
	return result

static func link(stage: Node3D, door: Dictionary) -> Dictionary:
	for entry in data().links:
		if entry.from == door.from and entry.to == door.to: return entry
	return {}

static func room_polygons(stage: Node3D) -> Array:
	var result: Array = []
	for points in data().rooms.get(stage.place, []): result = merge_into(result, polygon(stage, points))
	return result

static func contains(polygons: Array, point: Vector2, radius: float = 0.0) -> bool:
	for shape in polygons:
		if not Geometry2D.is_point_in_polygon(point, shape): continue
		var clear: = true
		if radius > 0.0:
			for i in shape.size():
				if Geometry2D.get_closest_point_to_segment(point, shape[i], shape[(i + 1) % shape.size()]).distance_squared_to(point) < radius * radius:
					clear = false
					break
		if clear: return true
	return false

static func closest(polygons: Array, point: Vector2, radius: float) -> Vector2:
	var best: = Vector2.INF
	var distance: = INF
	for shape in polygons:

		for inner in Geometry2D.offset_polygon(shape, - radius - 0.025):
			if Geometry2D.is_point_in_polygon(point, inner): return point
			for i in inner.size():
				var at: = Geometry2D.get_closest_point_to_segment(point, inner[i], inner[(i + 1) % inner.size()])
				if at.distance_squared_to(point) < distance:
					best = at;distance = at.distance_squared_to(point)
	return best

static func collision(stage: Node3D, polygons: Array, parent: Node3D, open_ends: Vector2 = Vector2(INF, INF)) -> void :
	var floor_faces: = PackedVector3Array()
	var edge_faces: = PackedVector3Array()
	for shape in polygons:
		var indices: = Geometry2D.triangulate_polygon(shape)
		for i in indices:
			floor_faces.append(Vector3(shape[i].x, 0, shape[i].y))
		for i in shape.size():
			var a: Vector2 = shape[i]
			var b: Vector2 = shape[(i + 1) % shape.size()]
			if (is_equal_approx(a.x, open_ends.x) and is_equal_approx(b.x, open_ends.x)) or (is_equal_approx(a.x, open_ends.y) and is_equal_approx(b.x, open_ends.y)): continue
			var start: = Vector3(a.x, 0, a.y)
			var end: = Vector3(b.x, 0, b.y)
			stage._quad(edge_faces, start, end, end + Vector3.UP * 8, start + Vector3.UP * 8)
	stage._collision("TracedGround", floor_faces, parent)
	stage._collision("TracedEdges", edge_faces, parent)

static func draw_links(stage: Node3D, region: Dictionary, parent: Node3D) -> void :
	var entries: = extras(stage, region)
	if stage.place == "SUN_GLADE":
		entries = [{"line": [[156, 216], [156, 239]], "half_width": 13}]
	if entries.is_empty(): return
	var surface: = SurfaceTool.new()
	surface.begin(Mesh.PRIMITIVE_TRIANGLES)
	for entry in entries:
		var line: Array = entry.line
		for i in range(1, line.size()):
			var a: = native_point(stage, line[i - 1], region)
			var b: = native_point(stage, line[i], region)
			var half: float = float(entry.half_width) * (float(region.width) if stage.place == "FOREST_TRAIL" else stage.width / 480.0 if stage.place != "OUTDOOR" else 30.0 / 1920.0)
			var side: = Vector3( - (b.z - a.z), 0, b.x - a.x).normalized() * half
			var side_a: = side * lerpf(1.6, 1.05, float(i - 1) / (line.size() - 1))
			var side_b: = side * lerpf(1.6, 1.05, float(i) / (line.size() - 1))
			var points: = [a - side_a, b + side_b, b - side_b, a - side_a, a + side_a, b + side_b]
			var uvs: = [Vector2(0, 0), Vector2(1, 1), Vector2(0, 1), Vector2(0, 0), Vector2(1, 0), Vector2(1, 1)]
			for j in 6:
				var fade: = 0.25 if (uvs[j].y == 1 and i == line.size() - 1) else 0.55 if (uvs[j].y == 0 and i == 1) else 1.0
				surface.set_color(Color(1, 1, 1, fade));surface.set_uv(uvs[j]);surface.add_vertex(points[j] + Vector3.UP * 0.008)
	var material: = ShaderMaterial.new()
	material.shader = load("res://shaders/worn_path.gdshader")
	material.render_priority = 2

	material.set_shader_parameter("path_texture", stage._scenery_texture(region.asset))
	var source_y: = 0.825
	if stage.place == "FOREST_TRAIL":
		var band: = Forest.raw_band(region, 0.5);source_y = (band.x + band.y) / 1280.0
	elif stage.place != "OUTDOOR": source_y = 0.77
	material.set_shader_parameter("source_uv", Vector2(0.52, source_y))
	material.set_meta("sample_point", native_point(stage, entries[0].line[0], region))
	stage.walkway_materials.append(material)
	stage._mesh("WornConnections", surface, material, parent)
