extends Node3D

const Catalog = preload("res://scripts/world_catalog.gd")
const Forest = preload("res://scripts/forest_catalog.gd")
const Ambience = preload("res://scripts/painted_ambience.gd")
const GroundLight = preload("res://scripts/ground_light.gd")
const WalkSurfaces = preload("res://scripts/walk_surfaces.gd")
var ground_polygons: Array = []
var extra_ground: Array = []
var walkway_materials: Array[ShaderMaterial] = []
var place: = "OUTDOOR"
var textures: Dictionary = {}
var materials: Array[StandardMaterial3D] = []
var painted_materials: Array[ShaderMaterial] = []
var chunks: Array[Node3D] = []
var doors: Array[Dictionary] = []
var furniture: Array[Dictionary] = []
var width: = 210.0
var native_height: = 640.0
var zoom_min: = 8.2
var zoom_max: = 10.2
var default_zoom: = 8.8
const SceneryData = preload("res://scripts/scenery_data.gd")
var streaming_scenery: = false
var scenery_fallbacks: Dictionary = {}
var interaction_focus_point: = Vector3.ZERO
var interaction_focus_gain: = 0.0
var base_pixel_size: = 0.0145
var body_pixel_size: = 0.0115
var jump_height: = 1.7
var dusk: = false
var building: = false
var horizontal_padding: = 2.35
var occlusion_pieces: Array[Dictionary] = []
var occlusion_maps: Dictionary = {}
var occlusion_bindings: Dictionary = {}
var natural_edges: Array[MeshInstance3D] = []
var draw_bands: Dictionary = {}
var pickups: Array[Dictionary] = []
var collected: Array = []
var local_light_sources: Array[Dictionary] = []
var light_hour: = 12.0
var light_probe_updates: = 0
var probe_ids: = PackedInt32Array([-1, -1, -1, -1])
var probe_distances: = PackedFloat64Array([INF, INF, INF, INF])
var probe_points: = PackedVector4Array([Vector4.ZERO, Vector4.ZERO, Vector4.ZERO, Vector4.ZERO])
var probe_colors: = PackedVector4Array([Vector4.ZERO, Vector4.ZERO, Vector4.ZERO, Vector4.ZERO])
var probe_phases: = PackedVector4Array([Vector4.ZERO, Vector4.ZERO, Vector4.ZERO, Vector4.ZERO])
var wood_material: ShaderMaterial
var wood_batches: Dictionary = {}
var wood_piece_count: = 0
var surface_cache: Dictionary = {}
var layout_scale: = 1.0
var interior: Dictionary = {}
var rest_anchors: Array[Dictionary] = []
var rest_room_kind: = 0
var rest_room_offset: = Vector2.ZERO
var rest_room_material: ShaderMaterial
var rest_rim: Texture2D

func interior_definition() -> Dictionary:
	return Catalog.data().interiors[place] if interior.is_empty() else interior

func _room_band(u: float) -> Vector2:
	return Catalog.room_band(place, u, interior_definition())

func _room_limits(y: float) -> Vector2:
	return Catalog.room_limits(place, y, interior_definition())

func _position_for(location: String, uv: Vector2) -> Vector3:
	return Catalog.position_for(location, uv, interior if location == place else {})

func image_z(y: float, reference_height: float = 640.0) -> float:
	return Catalog.image_z(y, reference_height) * layout_scale

func image_y(y: float, z: float, reference_height: float) -> float:
	return Catalog.image_y(y, z / layout_scale, reference_height) * layout_scale

func fit_character(pixel_size: float) -> void :
	if place == "OUTDOOR":
		return

	horizontal_padding = maxf(float(interior_definition().get("edge_padding", 2.35)) * layout_scale, 64.0 * pixel_size + 0.75)
	for child in get_children():
		if child.name.begins_with("WorldEnd"):
			remove_child(child)
			child.queue_free()
	_make_end_wall( - width * 0.5 + horizontal_padding - 0.4)
	_make_end_wall(width * 0.5 - horizontal_padding + 0.4)
	for door in doors:
		var raw: = _position_for(door.from, door.at)
		var traced: = WalkSurfaces.link(self, door)
		if not traced.is_empty(): raw = WalkSurfaces.native_point(self, traced.point, Catalog.region_for(door.from))
		door.position = safe_position(raw, 0.0) if door.has("route_direction") else safe_position(raw)
		door.marker_position = door.position + Vector3.UP * (1.8 if door.has("route_direction") else 1.5)

func build() -> void :
	building = true
	if place == "OUTDOOR":
		for region in Catalog.data().regions:
			var chunk: = Node3D.new()
			chunk.name = region.places[0]
			add_child(chunk)
			chunks.append(chunk)
			_make_panorama(region, chunk)
			_make_path(region, chunk)
		for i in range(Catalog.data().regions.size() - 1):
			_make_seam(i)
			_connect_bands(Catalog.data().regions[i], Catalog.data().regions[i + 1])
		_make_end_wall(1.9)
		_make_end_wall(width - 1.9)
		for outdoor_place in Catalog.data().labels:
			if not Catalog.region_for(outdoor_place).is_empty():
				for door in Catalog.doors_for(outdoor_place):
					_add_door(door)
		for piece in Catalog.geometry().outdoor_furniture:
			var region: = Catalog.region_for(piece.region)
			var traced: Dictionary = piece.duplicate(true)
			traced["place"] = region.places[0]
			_add_piece(traced, region.asset, float(region.origin), float(region.width), 1920.0, 640.0)
		for contour in Catalog.geometry().outdoor_occluders:
			var region: = Catalog.region_for(contour.place)
			occlusion_pieces.append({"piece": contour, "asset": region.asset, 
				"origin": region.origin, "span": region.width, 
				"reference_width": 1920.0, "reference_height": 640.0})

		for piece in Catalog.data().world_pieces:
			if piece.id == "park-crate":
				var region: = Catalog.region_for(piece.place)
				var span: float = float(region.width) / region.places.size()
				_add_piece(piece, region.asset, float(region.origin) + region.places.find(piece.place) * span, span, 480.0, 640.0)
		_add_pickup("park-pebbles", "PARK", Vector2(0.78, 0.7))
		for door in Forest.doors_for("FOREST"):
			_add_door(door)
	else:
		var room: Dictionary = interior_definition()
		layout_scale = Catalog.room_scale(place)
		width = Catalog.IMAGE_HEIGHT * layout_scale * 16.0 / 9.0
		native_height = 270.0
		zoom_min = 10.4 * layout_scale
		zoom_max = 11.8 * layout_scale
		default_zoom = 11.0 * layout_scale
		if room.get("outdoor", false):
			default_zoom = 10.0;zoom_min = 9.0;zoom_max = 10.0


		base_pixel_size = 0.0145 * (10.0 if room.get("outdoor", false) else 11.0) / 8.8
		body_pixel_size = 0.0115 * 10.0 / 7.6

		jump_height = 1.7 * default_zoom / 8.8
		var region: = {"asset": room.asset, "origin": - width * 0.5, "width": width, 
			"places": [place], "room_band": room.band}
		ground_polygons = WalkSurfaces.room_polygons(self)
		var chunk: = Node3D.new()
		add_child(chunk)
		_make_panorama(region, chunk)
		_make_path(region, chunk)
		_make_end_wall( - width * 0.5 + 1.8)
		_make_end_wall(width * 0.5 - 1.8)
		for piece in room.pieces:
			_add_piece(piece, room.asset, - width * 0.5, width, 480.0, 270.0)
		for door in Catalog.doors_for(place):
			_add_door(door)
		for anchor in room.get("rest_anchors", []):
			var z: = image_z(anchor.ground, native_height)
			rest_anchors.append({"id": anchor.id, 
				"position": Vector3((anchor.point[0] / 480.0 - 0.5) * width, image_y(anchor.point[1], z, native_height), z), 
				"approach": safe_position(Vector3((anchor.approach[0] / 480.0 - 0.5) * width, 0.03, image_z(anchor.approach[1], native_height)))})
	if place != "OUTDOOR":
		for door in Forest.doors_for(place):
			_add_door(door)
	building = false
	_build_natural_edges()
	_build_occlusion_maps()

func _add_pickup(id: String, location: String, at: Vector2) -> void :
	if id in collected: return
	var art: = Sprite3D.new()
	art.texture = load("res://assets/world/path-stones.png")
	art.billboard = BaseMaterial3D.BILLBOARD_ENABLED
	art.shaded = false
	art.texture_filter = BaseMaterial3D.TEXTURE_FILTER_LINEAR
	art.pixel_size = 0.0004
	var bounds: = art.texture.get_image().get_used_rect()
	art.offset.y = float(bounds.end.y) - art.texture.get_height() * 0.5
	art.position = safe_position(Catalog.position_for(location, at), 0.22) + Vector3.UP * 0.01
	add_child(art)
	pickups.append({"id": id, "position": art.position, "art": art})

func _build_natural_edges() -> void :
	if place != "OUTDOOR":
		return
	var surface: = SurfaceTool.new()
	surface.begin(Mesh.PRIMITIVE_TRIANGLES)
	var serial: = 0
	for edge in Catalog.geometry().natural_edges:
		var region: = Catalog.region_for(edge.region)
		var x: = float(region.origin) + float(edge.u[0]) * 30.0
		var end: = float(region.origin) + float(edge.u[1]) * 30.0
		while x < end:
			var limits: = _limits_at(x)
			var side: = int(edge.side)
			var z: = image_z(limits[side]) + (-0.1 if side == 0 else 0.1)

			var span: = 0.23 + 0.08 * sin(float(serial) * 2.1)
			var rise: = span * 0.4
			for corner in [Vector2(0, 0), Vector2(1, 1), Vector2(1, 0), Vector2(0, 0), Vector2(0, 1), Vector2(1, 1)]:
				surface.set_uv(corner)
				surface.add_vertex(Vector3(x + (corner.x - 0.5) * span, (1.0 - corner.y) * rise + 0.008, z))
			serial += 1
			x += float(edge.spacing) * (1.0 + 0.16 * sin(float(serial)))
	var material: = _material("res://assets/world/path-stones.png", true)
	material.render_priority = 2
	var mesh: = _mesh("NatuerlicheSteinraender", surface, material, self)
	natural_edges.append(mesh)
	for rail in Catalog.geometry().railings:
		var region: = Catalog.region_for(rail.region)
		var first: = float(region.origin) + float(rail.u[0]) * 30.0
		var last: = float(region.origin) + float(rail.u[1]) * 30.0
		var sections: = ceili((last - first) / float(rail.spacing))
		var previous: = Vector3.ZERO
		for i in range(sections + 1):
			var x: = lerpf(first, last, float(i) / sections)
			var z: = image_z(_limits_at(x)[int(rail.side)]) - 0.07
			var top: = Vector3(x, float(rail.height), z)
			_wood_piece("Stegpfosten", top * Vector3(1, 0.5, 1), Vector3(0.055, float(rail.height), 0.06))
			if i > 0:
				_wood_piece("Steggelaender", (previous + top) * 0.5, Vector3(top.x - previous.x + 0.04, 0.045, 0.055))
			previous = top
	_finish_wood()

func _wood_piece(_label: String, center: Vector3, dimensions: Vector3) -> void :
	var region: = clampi(int(center.x / 30.0), 0, 6)
	if not wood_batches.has(region):
		var surface: = SurfaceTool.new();surface.begin(Mesh.PRIMITIVE_TRIANGLES);wood_batches[region] = surface
	var box: = BoxMesh.new();box.size = dimensions
	wood_batches[region].append_from(box, 0, Transform3D(Basis.IDENTITY, center))
	wood_piece_count += 1
	_register_rail_mask(center, dimensions)

func _finish_wood() -> void :
	if wood_batches.is_empty(): return
	wood_material = ShaderMaterial.new();wood_material.shader = load("res://shaders/painted_wood.gdshader")

	for region in wood_batches: _mesh("Steggelaender_" + str(region), wood_batches[region], wood_material, self)
	wood_batches.clear()

func _register_rail_mask(center: Vector3, dimensions: Vector3) -> void :
	var region: Dictionary = Catalog.data().regions[clampi(int(center.x / 30.0), 0, 6)]
	var contour: Array = []
	for p in [Vector2(-1, 1), Vector2(1, 1), Vector2(1, -1), Vector2(-1, -1)]:
		var x: float = center.x + p.x * dimensions.x * 0.5
		var y: float = center.y + p.y * dimensions.y * 0.5
		contour.append([(x - float(region.origin)) / 30.0 * 1920.0, 
			(Catalog.ORIGIN_V - (cos(deg_to_rad(Catalog.PITCH)) * y - sin(deg_to_rad(Catalog.PITCH)) * center.z) / Catalog.IMAGE_HEIGHT) * 640.0])
	var front: = (Catalog.ORIGIN_V + sin(deg_to_rad(Catalog.PITCH)) * (center.z + dimensions.z * 0.5) / Catalog.IMAGE_HEIGHT) * 640.0
	occlusion_pieces.append({"piece": {"place": region.places[0], "front": front, "contours": [contour]}, 
		"asset": region.asset, "origin": region.origin, "span": 30.0, "reference_width": 1920.0, "reference_height": 640.0})

func _material(asset: String, transparent: bool = false) -> StandardMaterial3D:
	var key: = asset + str(transparent)
	if surface_cache.has(key): return surface_cache[key]
	var material: = StandardMaterial3D.new()
	material.albedo_texture = textures[asset] if textures.has(asset) else load(asset)
	material.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	material.cull_mode = BaseMaterial3D.CULL_DISABLED
	material.texture_filter = BaseMaterial3D.TEXTURE_FILTER_LINEAR_WITH_MIPMAPS
	if transparent:
		material.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
		material.vertex_color_use_as_albedo = true
	materials.append(material)
	surface_cache[key] = material
	return material

func _band(region: Dictionary, x: float, dry: bool = false) -> Vector2:
	if dry:
		return _room_band(x) if region.has("room_band") else Catalog.band(region, x, true)


	var asset: String = region.asset
	if not draw_bands.has(asset):
		draw_bands[asset] = {}
	var columns: Dictionary = draw_bands[asset]
	if not columns.has(x):
		var limits: = Vector2(region.room_band[0], region.room_band[1]) if region.has("room_band") else Catalog.band(region, x)

		for points in WalkSurfaces.data().rooms.get(place, []):
			for p in points: limits.x = minf(limits.x, float(p[1]) - 2.0)
		for entry in WalkSurfaces.extras(self, region):
			for p in entry.polygon: limits.x = minf(limits.x, float(p[1]) - 6.0)
		columns[x] = limits
	return columns[x]

func _point(region: Dictionary, uv: Vector2, ground: bool) -> Vector3:
	var x: float = region.origin + uv.x * region.width
	var band: = _band(region, uv.x)
	if ground:
		return Vector3(x, 0.0, image_z(uv.y * native_height, native_height))
	var z: = image_z(band.x, native_height) - smoothstep(band.x / native_height, 0.2, uv.y) * (2.2 if place == "OUTDOOR" else 0.65 * layout_scale)
	return Vector3(x, image_y(uv.y * native_height, z, native_height), z)

func _mesh(label: String, surface: SurfaceTool, material: Material, parent: Node3D) -> MeshInstance3D:
	surface.generate_normals()
	var mesh: = MeshInstance3D.new()
	mesh.name = label
	mesh.mesh = surface.commit()
	mesh.material_override = material
	mesh.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	parent.add_child(mesh)
	return mesh

func set_rest_room_offset(offset: Vector2) -> void :
	rest_room_offset = offset
	if rest_room_material != null:
		rest_room_material.set_shader_parameter("rest_room_offset", offset)

func rest_room_follow() -> Vector3:
	return Vector3(rest_room_offset.x * width, image_y(rest_room_offset.y * 270.0, 0.0, 270.0) - image_y(0.0, 0.0, 270.0), 0.0)

func _configure_rest_room(config: Dictionary, material: ShaderMaterial) -> void :
	rest_room_kind = int(config.kind)
	rest_room_material = material
	material.set_shader_parameter("rest_room_kind", rest_room_kind)
	for key in ["room_window", "room_sun_patch"]:
		var area: Array = config[key]
		material.set_shader_parameter(key, Vector4(area[0], area[1], area[2], area[3]))
	if config.has("rim"):
		var rim: = Image.create(480, 270, false, Image.FORMAT_L8)
		var outline: = PackedVector2Array()
		for point in config.rim:
			outline.append(Vector2(point[0], point[1]))
		var bounds: = Rect2(outline[0], Vector2.ZERO)
		for point in outline:
			bounds = bounds.expand(point)
		for y in range(floori(bounds.position.y), ceili(bounds.end.y)):
			for x in range(floori(bounds.position.x), ceili(bounds.end.x)):
				if Geometry2D.is_point_in_polygon(Vector2(x + 0.5, y + 0.5), outline):
					rim.set_pixel(x, y, Color.WHITE)
		rest_rim = ImageTexture.create_from_image(rim)

func _make_panorama(region: Dictionary, parent: Node3D) -> void :
	var material: = _painted_material(region.asset)
	if place == "BEDROOM" and interior_definition().has("room_motion"):
		_configure_rest_room(interior_definition().room_motion, material)
	material.set_meta("world_span", Vector2(float(region.origin), float(region.origin) + float(region.width)))
	_register_painted_lights(region)
	for ground in [false, true]:
		var surface: = SurfaceTool.new()
		surface.begin(Mesh.PRIMITIVE_TRIANGLES)
		var rows: = 1 if ground else 12
		for row in rows:
			for col in 96:
				for corner in [Vector2(0, 0), Vector2(1, 1), Vector2(1, 0), Vector2(0, 0), Vector2(0, 1), Vector2(1, 1)]:
					var u: float = (col + corner.x) / 96.0
					var band: = _band(region, u)
					var v: = lerpf(band.x / native_height, 1.0, (row + corner.y) / rows) if ground else lerpf(0.0, band.x / native_height, (row + corner.y) / rows)
					var uv: = Vector2(u, v)
					surface.set_uv(uv)
					surface.set_uv2(_band(region, u, true) / native_height)
					surface.add_vertex(_point(region, uv, ground))
		_mesh("PaintedFloor" if ground else "PaintedRelief", surface, material, parent)

func _collision(label: String, vertices: PackedVector3Array, parent: Node3D) -> void :
	var body: = StaticBody3D.new()
	body.name = label
	var shape: = CollisionShape3D.new()
	var polygon: = ConcavePolygonShape3D.new()
	polygon.backface_collision = true
	polygon.set_faces(vertices)
	shape.shape = polygon
	body.add_child(shape)
	parent.add_child(body)

func _quad(vertices: PackedVector3Array, a: Vector3, b: Vector3, c: Vector3, d: Vector3) -> void :
	for p in [a, c, b, a, d, c]:
		vertices.append(p)

func _make_path(region: Dictionary, parent: Node3D) -> void :
	if not ground_polygons.is_empty():
		WalkSurfaces.collision(self, ground_polygons, parent)
		WalkSurfaces.draw_links(self, region, parent)
		return
	var extras: = WalkSurfaces.extras(self, region)
	if not extras.is_empty():
		var back: = PackedVector2Array()
		var front: = PackedVector2Array()
		for i in 241:
			var u: = i / 240.0
			var limits: = _band(region, u, true)
			var x: float = region.origin + u * region.width
			back.append(Vector2(x, image_z(limits.x)))
			front.append(Vector2(x, image_z(limits.y)))
		front.reverse();back.append_array(front)
		var polygons: Array = [back]
		for entry in extras:
			var addition: = WalkSurfaces.polygon(self, entry.polygon, region)
			extra_ground.append(addition)
			polygons = WalkSurfaces.merge_into(polygons, addition)
		WalkSurfaces.collision(self, polygons, parent, Vector2(region.origin, region.origin + region.width))
		WalkSurfaces.draw_links(self, region, parent)
		return
	var floor_vertices: = PackedVector3Array()
	var edge_vertices: = PackedVector3Array()

	var samples: Array[float] = []
	for i in range(97):
		samples.append(i / 96.0)
	if place == "OUTDOOR":
		for anchor in Catalog.geometry().paths[region.places[0]]:
			samples.append(float(anchor[0]))
		for i in Catalog.geometry().seams.size():
			for anchor in Catalog.geometry().seams[i]:
				var u: float = ((i + 1) * 30.0 - 7.5 + float(anchor[0]) * 15.0 - float(region.origin)) / float(region.width)
				if u > 0 and u < 1:
					samples.append(u)
	samples.sort()
	for i in range(samples.size() - 1):
		var u0: = samples[i]
		var u1: = samples[i + 1]
		if is_equal_approx(u0, u1):
			continue
		var band0: = _band(region, u0, true)
		var band1: = _band(region, u1, true)
		var x0: float = region.origin + u0 * region.width
		var x1: float = region.origin + u1 * region.width
		if place != "OUTDOOR":

			var midpoint: = (u0 + u1) * 0.5 * 480.0
			var limits: = _room_limits((band0.x + band1.x) * 0.5)
			if midpoint < limits.x or midpoint > limits.y:
				continue
		var back0: = Vector3(x0, 0, image_z(band0.x, native_height))
		var back1: = Vector3(x1, 0, image_z(band1.x, native_height))
		var front0: = Vector3(x0, 0, image_z(band0.y, native_height))
		var front1: = Vector3(x1, 0, image_z(band1.y, native_height))
		_quad(floor_vertices, back0, back1, front1, front0)
		_quad(edge_vertices, back0, back1, back1 + Vector3.UP * 8, back0 + Vector3.UP * 8)
		_quad(edge_vertices, front1, front0, front0 + Vector3.UP * 8, front1 + Vector3.UP * 8)
	_collision("WalkableBand", floor_vertices, parent)
	_collision("PaintedBoundaries", edge_vertices, parent)
	if place != "OUTDOOR":
		var walls: = PackedVector3Array()
		var room: Dictionary = interior_definition()
		for side in [0, 1]:
			var previous: = Vector3.ZERO
			for i in range(65):
				var y: = lerpf(float(room.band[0]), float(room.band[1]), i / 64.0)
				var limits: = _room_limits(y)
				var point: = Vector3((limits[side] / 480.0 - 0.5) * width, 0, image_z(y, native_height))
				if i > 0:
					_quad(walls, previous, point, point + Vector3.UP * 10, previous + Vector3.UP * 10)
				previous = point
		_collision("RoomSideWalls", walls, parent)

func _make_end_wall(x: float) -> void :
	var vertices: = PackedVector3Array()
	_quad(vertices, Vector3(x, 0, -10), Vector3(x, 0, 10), Vector3(x, 10, 10), Vector3(x, 10, -10))


	var center: = width * 0.5 if place in ["OUTDOOR", "FOREST_TRAIL"] else 0.0
	_collision("WorldEndLeft" if x < center else "WorldEndRight", vertices, self)

func _connect_bands(left: Dictionary, right: Dictionary) -> void :
	var a: = _band(left, 1.0, true)
	var b: = _band(right, 0.0, true)
	var vertices: = PackedVector3Array()
	for edge in [Vector2(a.x, b.x), Vector2(a.y, b.y)]:
		if is_equal_approx(edge.x, edge.y):
			continue
		var p0: = Vector3(right.origin, 0, image_z(minf(edge.x, edge.y)))
		var p1: = Vector3(right.origin, 0, image_z(maxf(edge.x, edge.y)))
		_quad(vertices, p0, p1, p1 + Vector3.UP * 8, p0 + Vector3.UP * 8)
	if not vertices.is_empty():
		_collision("SeamBandBoundary", vertices, self)

func _make_seam(index: int) -> void :
	var boundary: float = (index + 1) * 30.0
	var asset: = "res://assets/world/seams/%s.png" % index
	var material: = _painted_material(asset, true)
	material.set_meta("world_span", Vector2(boundary - 7.5, boundary + 7.5))
	var entry: Dictionary = Ambience.data().assets.get(asset.get_file(), {})
	var dimensions: Array = entry.get("size", [1086, 724])
	for light_index in entry.get("lights", []).size():
		var source: Dictionary = entry.lights[light_index]
		var uv: = Vector2(float(source.at[0]) / dimensions[0], float(source.at[1]) / dimensions[1])
		var x: = boundary - 7.5 + uv.x * 15.0
		var region: Dictionary = Catalog.data().regions[index if x < boundary else index + 1]
		var base_uv: = Vector2((x - float(region.origin)) / float(region.width), uv.y)
		local_light_sources.append({"position": _point(region, base_uv, uv.y >= _band(region, base_uv.x).x / 640.0), 
			"radius": Vector2(float(source.radius[0]) / dimensions[0] * 15.0, float(source.radius[1]) / dimensions[1] * 10.0), 
			"strength": float(source.get("strength", 0.3)), "fire": source.kind == "fire", 
			"seed": float(light_index) * 1.73 + uv.x * 13.0 + uv.y * 7.0, "arena": false})
	var surface: = SurfaceTool.new()
	surface.begin(Mesh.PRIMITIVE_TRIANGLES)
	for ground in [false, true]:
		var rows: = 1 if ground else 12
		for row in rows:
			for col in 48:
				for corner in [Vector2(0, 0), Vector2(1, 1), Vector2(1, 0), Vector2(0, 0), Vector2(0, 1), Vector2(1, 1)]:
					var u: float = (col + corner.x) / 48.0
					var x: = boundary - 7.5 + u * 15.0
					var region: Dictionary = Catalog.data().regions[index if x < boundary else index + 1]
					var base_u: float = (x - region.origin) / region.width
					var band: = _band(region, base_u)
					var v: = lerpf(band.x / 640.0, 1.0, (row + corner.y) / float(rows)) if ground else lerpf(0, band.x / 640.0, (row + corner.y) / float(rows))
					var alpha: = smoothstep(0.0, 4.0, 7.5 - absf(x - boundary))
					surface.set_color(Color(1, 1, 1, alpha))
					surface.set_uv(Vector2(u, v))


					var source_band: = Catalog.profile(Catalog.geometry().seams[index], u)
					var target_band: = Catalog.band(region, base_u, true)
					var shift: = (target_band.x + target_band.y - source_band.x - source_band.y) * 0.5
					var p: = _point(region, Vector2(base_u, v + shift / 640.0), ground)
					p += Vector3(0, 0.006, 0.006)
					surface.add_vertex(p)
	_mesh("PaintedSeam%s" % index, surface, material, self)

func _add_piece(piece: Dictionary, asset: String, origin_x: float, span: float, reference_width: float, reference_height: float) -> void :
	var ground: float = piece.ground
	var back: = image_z(piece.back, reference_height)
	var front: = image_z(piece.front, reference_height)
	var x0: float = origin_x + piece.left / reference_width * span
	var x1: float = origin_x + piece.right / reference_width * span
	var height: float = (ground - float(piece.top)) / reference_height * Catalog.IMAGE_HEIGHT * layout_scale / cos(deg_to_rad(Catalog.PITCH))
	var body: = StaticBody3D.new()
	body.name = piece.id
	body.position = Vector3((x0 + x1) * 0.5, height * 0.5, (back + front) * 0.5)
	var shape: = CollisionShape3D.new()
	var box: = BoxShape3D.new()
	box.size = Vector3(x1 - x0, maxf(height, 0.12), maxf(front - back, 0.12))
	shape.shape = box
	body.add_child(shape)
	add_child(body)
	if piece.has("paint_asset"):
		var material: = _material(piece.paint_asset)
		var surface: = SurfaceTool.new()
		surface.begin(Mesh.PRIMITIVE_TRIANGLES)
		for contour in piece.contours:
			var polygon: = PackedVector2Array()
			for p in contour:
				polygon.append(Vector2(p[0], p[1]))
			for i in Geometry2D.triangulate_polygon(polygon):
				var p: = polygon[i]
				surface.set_uv(Vector2((p.x - float(piece.left)) / (float(piece.right) - float(piece.left)), 
					(p.y - float(piece.top)) / (float(piece.ground) - float(piece.top))))
				surface.add_vertex(Vector3(origin_x + p.x / reference_width * span, 
					image_y(p.y, front + 0.015, reference_height), front + 0.015))
		_mesh(piece.id + "PaintedFront", surface, material, self)
	else:


		occlusion_pieces.append({"piece": piece, "asset": asset, "origin": origin_x, 
			"span": span, "reference_width": reference_width, "reference_height": reference_height})
	var item: = piece.duplicate(true)
	item["world_center"] = Vector3((x0 + x1) * 0.5, height, (back + front) * 0.5)
	item["body"] = body
	item["approach_position"] = Vector3((x0 + x1) * 0.5, 0.03, front + 0.55)
	var points: = PackedVector3Array()
	var region: Dictionary = Catalog.data().regions[clampi(int((x0 + x1) * 0.5 / 30.0), 0, 6)].duplicate() if place == "OUTDOOR" else {"asset": asset, "origin": origin_x, "width": span, "room_band": interior_definition().band}
	for contour in piece.contours:
		for p in contour:
			var uv: = Vector2((origin_x + float(p[0]) / reference_width * span - float(region.origin)) / float(region.width), float(p[1]) / reference_height)
			points.append(_point(region, uv, uv.y >= _band(region, uv.x).x / native_height))
	item["visual_points"] = points
	if piece.seat != null:
		var point: Array = piece.seat.point
		var z: = (back + front) * 0.5
		item["seat_position"] = Vector3(origin_x + float(point[0]) / reference_width * span, image_y(point[1], z, reference_height), z)
	furniture.append(item)

func _add_door(door: Dictionary) -> void :
	var entry: = door.duplicate()
	entry["position"] = _position_for(door.from, door.at)
	var traced: = WalkSurfaces.link(self, door)
	if not traced.is_empty():
		entry.position = WalkSurfaces.native_point(self, traced.point, Catalog.region_for(door.from))
		entry.route_direction = traced.direction

	if place != "OUTDOOR" and ground_polygons.is_empty():
		entry.position.x = clampf(entry.position.x, - width * 0.5 + 2.3, width * 0.5 - 2.3)
		var room: Dictionary = interior_definition()
		entry.position.z = clampf(entry.position.z, image_z(room.band[0], 270) + 0.45, image_z(room.band[1], 270) - 0.45)
	if entry.has("route_direction"):
		entry.position = safe_position(entry.position, 0.0)
	entry["marker_position"] = entry.position + Vector3.UP * (1.8 if entry.has("route_direction") else 1.0 if place == "OUTDOOR" else 1.5)
	doors.append(entry)

func nearest_door(position: Vector3) -> Dictionary:
	var best: Dictionary = {}
	var distance: = 1.35 if place == "OUTDOOR" else 2.2
	for door in doors:
		var gap: float = Vector2(position.x - door.position.x, position.z - door.position.z).length()
		if gap < distance:
			distance = gap
			best = door
	return best

func safe_position(raw: Vector3, radius: float = 0.3) -> Vector3:

	if ( not ground_polygons.is_empty() or not extra_ground.is_empty()) and is_walkable(raw, radius):
		return Vector3(raw.x, maxf(raw.y, 0.03), raw.z)
	if not ground_polygons.is_empty():
		var nearest: = WalkSurfaces.closest(ground_polygons, Vector2(raw.x, raw.z), radius)
		var projected: = Vector3(nearest.x, maxf(raw.y, 0.03), nearest.y)
		if nearest.is_finite() and is_walkable(projected, radius): return projected
	var result: = raw
	var region: Dictionary
	var band: Vector2
	if place == "OUTDOOR":
		result.x = clampf(result.x, 2.3, width - 2.3)
		region = Catalog.data().regions[clampi(int(result.x / 30.0), 0, 6)]
		band = _band(region, (result.x - float(region.origin)) / 30.0, true)
	else:
		result.x = clampf(result.x, - width * 0.5 + horizontal_padding, width * 0.5 - horizontal_padding)
		band = _room_band(result.x / width + 0.5)
		var px_y: = Catalog.ORIGIN_V * native_height + result.z * sin(deg_to_rad(Catalog.PITCH)) / (Catalog.IMAGE_HEIGHT * layout_scale) * native_height
		var sides: = _room_limits(px_y)
		result.x = clampf(result.x, (sides.x / 480.0 - 0.5) * width + radius, (sides.y / 480.0 - 0.5) * width - radius)
	result.z = clampf(result.z, image_z(band.x, native_height) + radius + 0.04, image_z(band.y, native_height) - radius - 0.04)
	result.y = maxf(result.y, 0.03)

	for item in furniture:
		var body: StaticBody3D = item.body
		var box: BoxShape3D = body.get_child(0).shape
		var half: = box.size * 0.5
		if absf(result.x - body.position.x) < half.x + radius and absf(result.z - body.position.z) < half.z + radius:
			var front: = body.position.z + half.z + radius + 0.08
			if front <= image_z(band.y, native_height) - radius - 0.04:
				result.z = front
			else:
				result.x = body.position.x - half.x - radius - 0.08 if result.x < body.position.x else body.position.x + half.x + radius + 0.08
	if place != "OUTDOOR":
		result.x = clampf(result.x, - width * 0.5 + horizontal_padding, width * 0.5 - horizontal_padding)
	if not is_walkable(result, radius):


		var best: = Vector3.ZERO
		var best_distance: = INF
		var start_x: = maxf(2.3 if place == "OUTDOOR" else - width * 0.5 + horizontal_padding, result.x - 8.0)
		var end_x: = minf(width - 2.3 if place == "OUTDOOR" else width * 0.5 - horizontal_padding, result.x + 8.0)
		for ix in range(81):
			var x: = lerpf(start_x, end_x, ix / 80.0)
			var limits: = _limits_at(x)
			if not ground_polygons.is_empty():
				for shape in ground_polygons:
					for p in shape:
						var y: float = (Catalog.ORIGIN_V + p.y / layout_scale * sin(deg_to_rad(Catalog.PITCH)) / Catalog.IMAGE_HEIGHT) * native_height
						limits.x = minf(limits.x, y);limits.y = maxf(limits.y, y)
			for iz in range(25):
				var z: = lerpf(image_z(limits.x, native_height) + radius + 0.05, image_z(limits.y, native_height) - radius - 0.05, iz / 24.0)
				var point: = Vector3(x, 0.03, z)
				var distance: = point.distance_squared_to(raw)
				if distance < best_distance and is_walkable(point, radius):
					best = point
					best_distance = distance
		if best_distance < INF:
			result = best
	if not extra_ground.is_empty():
		var nearest: = WalkSurfaces.closest(extra_ground, Vector2(raw.x, raw.z), radius)
		var projected: = Vector3(nearest.x, maxf(raw.y, 0.03), nearest.y)
		if nearest.is_finite() and projected.distance_squared_to(raw) < result.distance_squared_to(raw) and is_walkable(projected, radius): return projected
	return result

func _limits_at(x: float) -> Vector2:
	if place != "OUTDOOR":
		return _room_band(x / width + 0.5)
	var region: Dictionary = Catalog.data().regions[clampi(int(x / 30.0), 0, 6)]
	return _band(region, (x - float(region.origin)) / 30.0, true)

func is_walkable(point: Vector3, radius: float = 0.0, allow_platforms: bool = false) -> bool:
	if not ground_polygons.is_empty():
		if point.x < - width * 0.5 + horizontal_padding or point.x > width * 0.5 - horizontal_padding: return false
		if not WalkSurfaces.contains(ground_polygons, Vector2(point.x, point.z), radius): return false
	else:
		if not _band_walkable(point, radius): return false
	for item in furniture:
		var body: StaticBody3D = item.body
		var box: BoxShape3D = body.get_child(0).shape
		if allow_platforms and point.y >= body.position.y + box.size.y * 0.5 - 0.06: continue
		if absf(point.x - body.position.x) < box.size.x * 0.5 + radius and absf(point.z - body.position.z) < box.size.z * 0.5 + radius: return false
	return true

func _band_walkable(point: Vector3, radius: float) -> bool:

	for offset in [Vector2.ZERO, Vector2(radius, 0), Vector2( - radius, 0), Vector2(0, radius), Vector2(0, - radius), Vector2.ONE * radius * 0.7071, - Vector2.ONE * radius * 0.7071, Vector2(1, -1) * radius * 0.7071, Vector2(-1, 1) * radius * 0.7071]:
		var x: float = point.x + offset.x
		var z: float = point.z + offset.y
		var limits: = _limits_at(x)
		if z < image_z(limits.x, native_height) or z > image_z(limits.y, native_height):
			if place == "OUTDOOR" and WalkSurfaces.contains(extra_ground, Vector2(x, z)): continue
			return false
		if place != "OUTDOOR":
			var y: float = (Catalog.ORIGIN_V + z * sin(deg_to_rad(Catalog.PITCH)) / (Catalog.IMAGE_HEIGHT * layout_scale)) * native_height
			var sides: = _room_limits(y)
			if x < (sides.x / 480.0 - 0.5) * width or x > (sides.y / 480.0 - 0.5) * width:
				return false
		elif x < 1.9 or x > width - 1.9:
			return false
	return true

func constrain_step(before: Vector3, proposed: Vector3, radius: float) -> Vector3:


	if _segment_walkable(before, proposed, radius):
		return proposed
	var horizontal: = Vector3(proposed.x, proposed.y, before.z)
	if _segment_walkable(before, horizontal, radius):
		return horizontal
	var depth: = Vector3(before.x, proposed.y, proposed.z)
	if _segment_walkable(before, depth, radius):
		return depth
	return Vector3(before.x, proposed.y, before.z)

func guide_motion(point: Vector3, motion: Vector3, delta: float, radius: float) -> Vector3:


	if place != "OUTDOOR" and place != "FOREST_TRAIL" and not interior_definition().get("outdoor", false):
		return motion
	var planar: = Vector2(motion.x, motion.z)
	if absf(planar.x) < 0.05 or absf(planar.y) > absf(planar.x) * 0.75:
		return motion
	var travel: = maxf(planar.length() * delta, 0.1)
	var ahead: = point + Vector3(planar.x, 0, planar.y).normalized() * travel
	if _segment_walkable(point, ahead, radius + 0.015):
		return motion
	var band: = _limits_at(point.x)
	var next: = _limits_at(point.x + signf(planar.x) * 0.15)
	var fraction: = clampf(inverse_lerp(image_z(band.x, native_height), image_z(band.y, native_height), point.z), 0, 1)
	var rise: = image_z(lerpf(next.x, next.y, fraction), native_height) - image_z(lerpf(band.x, band.y, fraction), native_height)
	var tangent: = Vector2(signf(planar.x) * 0.15, rise).normalized()
	if tangent.dot(planar.normalized()) < 0.3 or absf(rise) < 0.0001:
		return motion
	var guided: = Vector3(tangent.x, 0, tangent.y) * planar.length()


	if _segment_walkable(point, point + guided.normalized() * travel, radius):
		return Vector3(guided.x, motion.y, guided.z)
	return motion

func _segment_walkable(before: Vector3, after: Vector3, radius: float) -> bool:
	var samples: = maxi(1, ceili(Vector2(after.x - before.x, after.z - before.z).length() / 0.1))
	for i in range(1, samples + 1):
		if not is_walkable(before.lerp(after, float(i) / samples), radius, true):
			return false
	return true

func _build_occlusion_maps() -> void :
	for entry in occlusion_pieces:
		var ref_width: = 1920 if place == "OUTDOOR" else 480
		var ref_height: = 640 if place == "OUTDOOR" else 270
		if not occlusion_maps.has(entry.asset):
			occlusion_maps[entry.asset] = Image.create(ref_width, ref_height, false, Image.FORMAT_RG8)
		var mask: Image = occlusion_maps[entry.asset]
		var region_origin: float = Catalog.region_for(entry.piece.place).get("origin", entry.origin) if place == "OUTDOOR" else entry.origin
		var region_span: = 30.0 if place == "OUTDOOR" else width
		for contour in entry.piece.contours:
			var polygon: = PackedVector2Array()
			for p in contour:
				polygon.append(Vector2((entry.origin - region_origin + float(p[0]) / entry.reference_width * entry.span) / region_span * ref_width, p[1]))
			var bounds: = Rect2(polygon[0], Vector2.ZERO)
			for p in polygon:
				bounds = bounds.expand(p)
			for y in range(maxi(0, int(bounds.position.y)), mini(ref_height, ceili(bounds.end.y))):
				for x in range(maxi(0, int(bounds.position.x)), mini(ref_width, ceili(bounds.end.x))):
					if Geometry2D.is_point_in_polygon(Vector2(x + 0.5, y + 0.5), polygon):
						var front: float = float(entry.piece.front) / ref_height
						mask.set_pixel(x, y, Color(maxf(front, mask.get_pixel(x, y).r), 1.0, 0, 1))
	for asset in occlusion_maps.keys():
		occlusion_maps[asset] = ImageTexture.create_from_image(occlusion_maps[asset])

func actor_occlusion(position: Vector3) -> Dictionary:
	var region: Dictionary = Catalog.data().regions[clampi(int(position.x / 30.0), 0, 6)] if place == "OUTDOOR" else {}
	var asset: String = region.asset if place == "OUTDOOR" else interior_definition().asset


	if not occlusion_bindings.has(asset):
		occlusion_bindings[asset] = {"mask": occlusion_maps.get(asset), "origin": region.origin if place == "OUTDOOR" else - width * 0.5, 
			"span": 30.0 if place == "OUTDOOR" else width, "height": 10.0 * layout_scale, 
			"room_motion_kind": rest_room_kind, "room_motion_offset": rest_room_offset, "rest_rim": rest_rim}
	var binding: Dictionary = occlusion_bindings[asset]
	if binding.mask != occlusion_maps.get(asset): binding.mask = occlusion_maps.get(asset)
	if binding.room_motion_offset != rest_room_offset: binding.room_motion_offset = rest_room_offset
	return binding

func set_dusk(enabled: bool) -> void :
	dusk = enabled
	set_time(19.5 if enabled else 12.0)
	for material in materials:
		material.albedo_color = Color(0.72, 0.68, 0.78) if enabled and place == "OUTDOOR" else Color.WHITE

func update_visible(player_x: float) -> void :
	for i in chunks.size():
		chunks[i].visible = absf(float(i) * 30.0 + 15.0 - player_x) < 58.0

func furniture_screen_rect(item: Dictionary, view: Camera3D) -> Rect2:
	var bounds: = Rect2()
	var first: = true
	for point in item.visual_points:
		var screen: Vector2 = view.unproject_position(point)
		if first:
			bounds = Rect2(screen, Vector2.ZERO)
			first = false
		else: bounds = bounds.expand(screen)
	return bounds

func _scenery_texture(asset: String) -> Texture2D:
	if textures.has(asset): return textures[asset]
	if streaming_scenery and SceneryData.data().has(asset):
		if not scenery_fallbacks.has(asset): scenery_fallbacks[asset] = SceneryData.fallback(asset)
		if scenery_fallbacks[asset] != null: return scenery_fallbacks[asset]
	return load(asset)

func _painted_material(asset: String, transparent: bool = false) -> ShaderMaterial:
	var texture: Texture2D = _scenery_texture(asset)
	var material: = Ambience.create_material(asset, texture, transparent)
	material.set_meta("painted_asset", asset)
	material.set_shader_parameter("transparent_piece", transparent)
	if asset == "res://assets/world/coast.png":
		material.set_shader_parameter("shore_enabled", true)
		material.set_shader_parameter("shore_texture", _scenery_texture("res://assets/world/coast-shore-v026.png"))
		material.set_meta("shore_asset", "res://assets/world/coast-shore-v026.png")
	painted_materials.append(material)
	return material

func set_time(hour: float) -> void :
	light_hour = hour
	for path_material in walkway_materials: path_material.set_shader_parameter("scene_tint", actor_scene_color(path_material.get_meta("sample_point")))
	for material in painted_materials: Ambience.set_time(material, hour, place != "OUTDOOR" and not interior_definition().get("outdoor", false))
	var daylight: = smoothstep(5.8, 8.0, hour) * (1.0 - smoothstep(18.0, 21.0, hour))
	if rest_room_material != null:
		rest_room_material.set_shader_parameter("room_daylight", daylight)
	for material in materials:
		material.albedo_color = Color(0.32, 0.4, 0.58).lerp(Color.WHITE, daylight) if place == "OUTDOOR" else Color.WHITE

func _register_painted_lights(region: Dictionary) -> void :
	var entry: Dictionary = Ambience.data().assets.get(String(region.asset).get_file(), {})
	var size: Array = entry.get("size", [2172, 724])
	for index in entry.get("lights", []).size():
		var source: Dictionary = entry.lights[index]
		var uv: = Vector2(float(source.at[0]) / size[0], float(source.at[1]) / size[1])
		var anchor: = _point(region, uv, uv.y >= _band(region, uv.x).x / native_height)
		local_light_sources.append({"position": anchor, 
			"radius": Vector2(float(source.radius[0]) / size[0] * float(region.width), float(source.radius[1]) / size[1] * 10.0 * layout_scale), 
			"strength": float(source.get("strength", 0.3)), "fire": source.kind == "fire", 
			"seed": float(index) * 1.73 + uv.x * 13.0 + uv.y * 7.0, "arena": false})

func apply_actor_light(material: ShaderMaterial, point: Vector3) -> void :
	light_probe_updates += 1
	material.set_shader_parameter("scene_light", actor_scene_color(point))
	var center: = Vector2(point.x, 0.819152 * point.y - 0.573576 * point.z + 0.75)
	for i in 4: probe_ids[i] = -1;probe_distances[i] = INF


	for index in local_light_sources.size():
		var source: Dictionary = local_light_sources[index]
		if source.get("forest", false) and float(source.get("gain", 0.0)) <= 0.0: continue
		var position: Vector3 = source.position
		var projected: = Vector2(position.x, 0.819152 * position.y - 0.573576 * position.z)
		var gap: Vector2 = (center - projected) / source.radius
		var distance: = gap.length_squared()
		if distance >= 2.0: continue
		for slot in 4:
			if distance < probe_distances[slot]:
				for move in range(3, slot, -1): probe_ids[move] = probe_ids[move - 1];probe_distances[move] = probe_distances[move - 1]
				probe_ids[slot] = index;probe_distances[slot] = distance;break
	var daylight: = smoothstep(5.8, 8.0, light_hour) * (1.0 - smoothstep(18.0, 21.0, light_hour))
	var count: = 0
	for slot in 4:
		probe_points[slot] = Vector4.ZERO;probe_colors[slot] = Vector4.ZERO;probe_phases[slot] = Vector4.ZERO
		if probe_ids[slot] < 0: continue
		var source: Dictionary = local_light_sources[probe_ids[slot]]
		var position: Vector3 = source.position
		var gain: = lerpf(0.95, 0.36, daylight) if source.fire else lerpf(0.82, 0.04, daylight)
		if source.get("forest", false): gain = float(source.get("gain", 1.0))
		probe_points[slot] = Vector4(position.x, 0.819152 * position.y - 0.573576 * position.z, source.radius.x, source.radius.y)
		probe_colors[slot] = Vector4(1, 0.54, 0.18, float(source.strength) * gain)
		probe_phases[slot] = Vector4(float(source.seed), 1.0 if source.fire else 0.0, 0, 0)
		count += 1
	material.set_shader_parameter("local_light_count", count)
	material.set_shader_parameter("local_light_points", probe_points)
	material.set_shader_parameter("local_light_colors", probe_colors)
	material.set_shader_parameter("local_light_phases", probe_phases)

func ground_light_at(point: Vector3) -> float:
	var region: Dictionary = Catalog.data().regions[clampi(int(point.x / 30.0), 0, 6)] if place == "OUTDOOR" else {}
	var asset: String = region.asset if place == "OUTDOOR" else interior_definition().asset
	var origin: float = region.origin if place == "OUTDOOR" else - width * 0.5
	var span: float = region.width if place == "OUTDOOR" else width
	return GroundLight.sample(asset, Vector2((point.x - origin) / span, Catalog.ORIGIN_V + 0.573576 * point.z / (10.0 * layout_scale)))

func actor_scene_color(point: Vector3) -> Color:
	var gain: = ground_light_at(point)
	return Ambience.scene_color(light_hour, place != "OUTDOOR" and not interior_definition().get("outdoor", false)) * Color(gain, gain, gain, 1)

func shadow_strength(point: Vector3) -> float:

	return lerpf(0.54, 0.23, clampf(darkness_at(point), 0.0, 1.0)) * ground_light_at(point)


func _receives_local_effect(material: ShaderMaterial, x: float, key: String, gain_uniform: String) -> bool:
	if not material.has_meta("world_span"): return true
	var span: Vector2 = material.get_meta("world_span")
	var nearby: bool = x >= span.x - 8.0 and x <= span.y + 8.0
	if not nearby and bool(material.get_meta(key, false)):
		material.set_shader_parameter(gain_uniform, 0.0)
	material.set_meta(key, nearby)
	return nearby

func update_walker(point: Vector3, speed: float) -> void :
	for material in painted_materials:
		if not _receives_local_effect(material, point.x, "walker_near", "walker_motion"): continue
		material.set_shader_parameter("walker_position", point)
		material.set_shader_parameter("walker_motion", clampf(speed / 2.0, 0.0, 1.0))

func darkness_at(point: Vector3) -> float:
	var daylight: = smoothstep(5.8, 8.0, light_hour) * (1.0 - smoothstep(18.0, 21.0, light_hour))
	return (1.0 - daylight) * (0.6 if place != "OUTDOOR" else 1.0)

func update_character_light(actor: Node3D, energy: float) -> void :
	if not actor.player_controlled: return
	var center: Vector3 = actor.light_center()
	for material in painted_materials:
		if not _receives_local_effect(material, center.x, "light_near", "carried_gain"): continue
		material.set_shader_parameter("carried_position", center)
		material.set_shader_parameter("carried_gain", energy)

func set_interaction_focus(point: Vector3, gain: float) -> void :
	if interaction_focus_point == point and interaction_focus_gain == gain: return
	interaction_focus_point = point;interaction_focus_gain = gain
	for material in painted_materials:
		material.set_shader_parameter("interaction_position", point)
		material.set_shader_parameter("interaction_gain", gain)
