extends Node3D

const Catalog = preload("res://scripts/world_catalog.gd")
var place := "OUTDOOR"
var textures: Dictionary = {}
var materials: Array[StandardMaterial3D] = []
var chunks: Array[Node3D] = []
var doors: Array[Dictionary] = []
var furniture: Array[Dictionary] = []
var width := 210.0
var native_height := 640.0
var zoom_min := 6.8
var zoom_max := 9.2
var default_zoom := 7.6
var base_pixel_size := 0.020
var jump_height := 1.7
var dusk := false
var building := false
var horizontal_padding := 2.35

func fit_character(pixel_size: float) -> void:
	if place == "OUTDOOR":
		return
	# Ganze 128-px-Karten decken auch breite Lauf- und Rollenposen ab.
	horizontal_padding = maxf(2.35, 64.0 * pixel_size + 0.75)
	for child in get_children():
		if child.name.begins_with("WorldEnd"):
			remove_child(child)
			child.queue_free()
	_make_end_wall(-width * 0.5 + horizontal_padding - 0.4)
	_make_end_wall(width * 0.5 - horizontal_padding + 0.4)
	for door in doors:
		var raw := Catalog.position_for(door.from, door.at)
		door.position = safe_position(raw)
		door.label.position = door.position + Vector3.UP * 2.0

func build() -> void:
	building = true
	if place == "OUTDOOR":
		for region in Catalog.data().regions:
			var chunk := Node3D.new()
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
		for piece in Catalog.data().world_pieces:
			var region := Catalog.region_for(piece.place)
			var span: float = float(region.width) / region.places.size()
			_add_piece(piece, region.asset, float(region.origin) + region.places.find(piece.place) * span, span, 480.0, 640.0)
	else:
		var room: Dictionary = Catalog.data().interiors[place]
		width = Catalog.IMAGE_HEIGHT * 16.0 / 9.0
		native_height = 270.0
		zoom_min = 9.2
		zoom_max = 11.0
		default_zoom = 10.0
		# Gegenstaende und Figuren behalten das native lokale Raumverhaeltnis.
		var body_px := lerpf(room.height[0], room.height[1], 0.55)
		base_pixel_size = body_px / 270.0 * Catalog.IMAGE_HEIGHT / cos(deg_to_rad(Catalog.PITCH)) / 83.0
		jump_height = 78.0 / 270.0 * Catalog.IMAGE_HEIGHT / cos(deg_to_rad(Catalog.PITCH)) + 0.15
		var region := {"asset": room.asset, "origin": -width * 0.5, "width": width,
			"places": [place], "room_band": room.band}
		var chunk := Node3D.new()
		add_child(chunk)
		_make_panorama(region, chunk)
		_make_path(region, chunk)
		_make_end_wall(-width * 0.5 + 1.8)
		_make_end_wall(width * 0.5 - 1.8)
		for piece in room.pieces:
			_add_piece(piece, room.asset, -width * 0.5, width, 480.0, 270.0)
		for door in Catalog.doors_for(place):
			_add_door(door)
	building = false

func _material(asset: String, transparent: bool = false) -> StandardMaterial3D:
	var material := StandardMaterial3D.new()
	material.albedo_texture = textures.get(asset, load(asset))
	material.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	material.cull_mode = BaseMaterial3D.CULL_DISABLED
	material.texture_filter = BaseMaterial3D.TEXTURE_FILTER_LINEAR_WITH_MIPMAPS
	if transparent:
		material.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
		material.vertex_color_use_as_albedo = true
	materials.append(material)
	return material

func _band(region: Dictionary, x: float, dry: bool = false) -> Vector2:
	if region.has("room_band"):
		return Vector2(region.room_band[0], region.room_band[1])
	return Catalog.band(region, x, dry)

func _point(region: Dictionary, uv: Vector2, ground: bool) -> Vector3:
	var x: float = region.origin + uv.x * region.width
	var band := _band(region, uv.x)
	if ground:
		return Vector3(x, 0.0, Catalog.image_z(uv.y * native_height, native_height))
	var z := Catalog.image_z(band.x, native_height) - smoothstep(band.x / native_height, 0.20, uv.y) * (2.2 if place == "OUTDOOR" else 0.65)
	return Vector3(x, Catalog.image_y(uv.y * native_height, z, native_height), z)

func _mesh(label: String, surface: SurfaceTool, material: Material, parent: Node3D) -> MeshInstance3D:
	surface.generate_normals()
	var mesh := MeshInstance3D.new()
	mesh.name = label
	mesh.mesh = surface.commit()
	mesh.material_override = material
	mesh.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	parent.add_child(mesh)
	return mesh

func _make_panorama(region: Dictionary, parent: Node3D) -> void:
	var material := _material(region.asset)
	for ground in [false, true]:
		var surface := SurfaceTool.new()
		surface.begin(Mesh.PRIMITIVE_TRIANGLES)
		var rows := 8 if ground else 32
		for row in rows:
			for col in 96:
				for corner in [Vector2(0, 0), Vector2(1, 1), Vector2(1, 0), Vector2(0, 0), Vector2(0, 1), Vector2(1, 1)]:
					var u: float = (col + corner.x) / 96.0
					var band := _band(region, u)
					var v := lerpf(band.x / native_height, 1.0, (row + corner.y) / rows) if ground else lerpf(0.0, band.x / native_height, (row + corner.y) / rows)
					var uv := Vector2(u, v)
					surface.set_uv(uv)
					surface.add_vertex(_point(region, uv, ground))
		_mesh("PaintedFloor" if ground else "PaintedRelief", surface, material, parent)

func _collision(label: String, vertices: PackedVector3Array, parent: Node3D) -> void:
	var body := StaticBody3D.new()
	body.name = label
	var shape := CollisionShape3D.new()
	var polygon := ConcavePolygonShape3D.new()
	polygon.backface_collision = true
	polygon.set_faces(vertices)
	shape.shape = polygon
	body.add_child(shape)
	parent.add_child(body)

func _quad(vertices: PackedVector3Array, a: Vector3, b: Vector3, c: Vector3, d: Vector3) -> void:
	for p in [a, c, b, a, d, c]:
		vertices.append(p)

func _make_path(region: Dictionary, parent: Node3D) -> void:
	var floor_vertices := PackedVector3Array()
	var edge_vertices := PackedVector3Array()
	# Ein zusammenhaengender Streifen bildet auch die gemalte Bachbruecke ab.
	var samples: Array[float] = []
	for i in range(97):
		samples.append(i / 96.0)
	if region.places[0] == "COAST_PATH":
		for anchor in Catalog.data().coast_path_band:
			samples.append(float(anchor[0]))
	samples.sort()
	for i in range(samples.size() - 1):
		var u0 := samples[i]
		var u1 := samples[i + 1]
		if is_equal_approx(u0, u1):
			continue
		var band0 := _band(region, u0, true)
		var band1 := _band(region, u1, true)
		var x0: float = region.origin + u0 * region.width
		var x1: float = region.origin + u1 * region.width
		var back0 := Vector3(x0, 0, Catalog.image_z(band0.x, native_height))
		var back1 := Vector3(x1, 0, Catalog.image_z(band1.x, native_height))
		var front0 := Vector3(x0, 0, Catalog.image_z(band0.y, native_height))
		var front1 := Vector3(x1, 0, Catalog.image_z(band1.y, native_height))
		_quad(floor_vertices, back0, back1, front1, front0)
		_quad(edge_vertices, back0, back1, back1 + Vector3.UP * 8, back0 + Vector3.UP * 8)
		_quad(edge_vertices, front1, front0, front0 + Vector3.UP * 8, front1 + Vector3.UP * 8)
	_collision("WalkableBand", floor_vertices, parent)
	_collision("PaintedBoundaries", edge_vertices, parent)

func _make_end_wall(x: float) -> void:
	var vertices := PackedVector3Array()
	_quad(vertices, Vector3(x, 0, -10), Vector3(x, 0, 10), Vector3(x, 10, 10), Vector3(x, 10, -10))
	_collision("WorldEnd", vertices, self)

func _connect_bands(left: Dictionary, right: Dictionary) -> void:
	var a := _band(left, 1.0, true)
	var b := _band(right, 0.0, true)
	var vertices := PackedVector3Array()
	for edge in [Vector2(a.x, b.x), Vector2(a.y, b.y)]:
		if is_equal_approx(edge.x, edge.y):
			continue
		var p0 := Vector3(right.origin, 0, Catalog.image_z(minf(edge.x, edge.y)))
		var p1 := Vector3(right.origin, 0, Catalog.image_z(maxf(edge.x, edge.y)))
		_quad(vertices, p0, p1, p1 + Vector3.UP * 8, p0 + Vector3.UP * 8)
	if not vertices.is_empty():
		_collision("SeamBandBoundary", vertices, self)

func _make_seam(index: int) -> void:
	var boundary: float = (index + 1) * 30.0
	var asset := "res://assets/world/seams/%s.png" % index
	var material := _material(asset, true)
	var surface := SurfaceTool.new()
	surface.begin(Mesh.PRIMITIVE_TRIANGLES)
	for ground in [false, true]:
		for row in (8 if ground else 32):
			for col in 48:
				for corner in [Vector2(0, 0), Vector2(1, 1), Vector2(1, 0), Vector2(0, 0), Vector2(0, 1), Vector2(1, 1)]:
					var u: float = (col + corner.x) / 48.0
					var x := boundary - 7.5 + u * 15.0
					var region: Dictionary = Catalog.data().regions[index if x < boundary else index + 1]
					var base_u: float = (x - region.origin) / region.width
					var band := _band(region, base_u)
					var v := lerpf(band.x / 640.0, 1.0, (row + corner.y) / 8.0) if ground else lerpf(0, band.x / 640.0, (row + corner.y) / 32.0)
					var alpha := smoothstep(0.0, 2.0, 7.5 - absf(x - boundary))
					surface.set_color(Color(1, 1, 1, alpha))
					surface.set_uv(Vector2(u, v))
					var p := _point(region, Vector2(base_u, v), ground)
					p += Vector3(0, 0.006, 0.006)
					surface.add_vertex(p)
	_mesh("PaintedSeam%s" % index, surface, material, self)

func _add_piece(piece: Dictionary, asset: String, origin_x: float, span: float, reference_width: float, reference_height: float) -> void:
	var band := Vector2(450, 625) if place == "OUTDOOR" else Vector2(Catalog.data().interiors[place].band[0], Catalog.data().interiors[place].band[1])
	var ground := clampf(piece.ground, band.x, band.y)
	var back := Catalog.image_z(clampf(piece.back, band.x, band.y), reference_height)
	var front := Catalog.image_z(clampf(piece.front, band.x, band.y), reference_height)
	var x0: float = origin_x + piece.left / reference_width * span
	var x1: float = origin_x + piece.right / reference_width * span
	var height: float = (ground - float(piece.top)) / reference_height * Catalog.IMAGE_HEIGHT / cos(deg_to_rad(Catalog.PITCH))
	var body := StaticBody3D.new()
	body.name = piece.id
	body.position = Vector3((x0 + x1) * 0.5, height * 0.5, (back + front) * 0.5)
	var shape := CollisionShape3D.new()
	var box := BoxShape3D.new()
	box.size = Vector3(x1 - x0, maxf(height, 0.12), maxf(front - back, 0.12))
	shape.shape = box
	body.add_child(shape)
	add_child(body)
	var material := _material(piece.get("paint_asset", asset))
	var surface := SurfaceTool.new()
	surface.begin(Mesh.PRIMITIVE_TRIANGLES)
	for contour in piece.contours:
		var polygon := PackedVector2Array()
		for p in contour:
			polygon.append(Vector2(p[0], p[1]))
		var indices := Geometry2D.triangulate_polygon(polygon)
		for i in indices:
			var p := polygon[i]
			var uv: Vector2
			if piece.has("paint_asset"):
				uv = Vector2((p.x - float(piece.left)) / (float(piece.right) - float(piece.left)), (p.y - float(piece.top)) / (float(piece.ground) - float(piece.top)))
			elif place == "OUTDOOR":
				var region := Catalog.region_for(piece.place)
				uv = Vector2((origin_x - float(region.origin) + p.x / reference_width * span) / float(region.width), p.y / reference_height)
			else:
				uv = Vector2(p.x / reference_width, p.y / reference_height)
			surface.set_uv(uv)
			surface.add_vertex(Vector3(origin_x + p.x / reference_width * span,
				Catalog.image_y(p.y, front + 0.015, reference_height), front + 0.015))
	_mesh(piece.id + "PaintedFront", surface, material, self)
	var item := piece.duplicate(true)
	item["world_center"] = Vector3((x0 + x1) * 0.5, height, (back + front) * 0.5)
	item["body"] = body
	if piece.seat != null:
		var point: Array = piece.seat.point
		var z := (back + front) * 0.5
		item["seat_position"] = Vector3(origin_x + float(point[0]) / reference_width * span, Catalog.image_y(point[1], z, reference_height), z)
	furniture.append(item)

func _add_door(door: Dictionary) -> void:
	var entry := door.duplicate()
	entry["position"] = Catalog.position_for(door.from, door.at)
	# Die Tuer bleibt vor der Wand erreichbar, auch wenn der gezeichnete Anker am Rand liegt.
	if place != "OUTDOOR":
		entry.position.x = clampf(entry.position.x, -width * 0.5 + 2.3, width * 0.5 - 2.3)
		var room: Dictionary = Catalog.data().interiors[place]
		entry.position.z = clampf(entry.position.z, Catalog.image_z(room.band[0], 270) + 0.45, Catalog.image_z(room.band[1], 270) - 0.45)
	var label := Label3D.new()
	label.text = "↗ " + Catalog.data().labels[door.to]
	label.font_size = 32
	label.pixel_size = 0.008
	label.position = entry.position + Vector3.UP * (1.2 if place == "OUTDOOR" else 2.0)
	label.billboard = BaseMaterial3D.BILLBOARD_ENABLED
	label.modulate = Color(1, 0.88, 0.58, 0.9)
	label.outline_size = 8
	label.no_depth_test = true
	add_child(label)
	entry["label"] = label
	doors.append(entry)

func nearest_door(position: Vector3) -> Dictionary:
	var best: Dictionary = {}
	var distance := 1.35 if place == "OUTDOOR" else 2.2
	for door in doors:
		var gap: float = Vector2(position.x - door.position.x, position.z - door.position.z).length()
		if gap < distance:
			distance = gap
			best = door
	return best

func safe_position(raw: Vector3, radius: float = 0.3) -> Vector3:
	var result := raw
	var region: Dictionary
	var band: Vector2
	if place == "OUTDOOR":
		result.x = clampf(result.x, 2.3, width - 2.3)
		region = Catalog.data().regions[clampi(int(result.x / 30.0), 0, 6)]
		band = _band(region, (result.x - float(region.origin)) / 30.0, true)
	else:
		result.x = clampf(result.x, -width * 0.5 + horizontal_padding, width * 0.5 - horizontal_padding)
		var room: Dictionary = Catalog.data().interiors[place]
		band = Vector2(room.band[0], room.band[1])
	result.z = clampf(result.z, Catalog.image_z(band.x, native_height) + radius + 0.04, Catalog.image_z(band.y, native_height) - radius - 0.04)
	result.y = maxf(result.y, 0.03)
	# Tuereintritt setzt niemanden in die Kollision eines nahen Schrankes.
	for item in furniture:
		var body: StaticBody3D = item.body
		var box: BoxShape3D = body.get_child(0).shape
		var half := box.size * 0.5
		if absf(result.x - body.position.x) < half.x + radius and absf(result.z - body.position.z) < half.z + radius:
			var front := body.position.z + half.z + radius + 0.08
			if front <= Catalog.image_z(band.y, native_height) - radius - 0.04:
				result.z = front
			else:
				result.x = body.position.x - half.x - radius - 0.08 if result.x < body.position.x else body.position.x + half.x + radius + 0.08
	if place != "OUTDOOR":
		result.x = clampf(result.x, -width * 0.5 + horizontal_padding, width * 0.5 - horizontal_padding)
	return result

func set_dusk(enabled: bool) -> void:
	dusk = enabled
	for material in materials:
		material.albedo_color = Color(0.72, 0.68, 0.78) if enabled and place == "OUTDOOR" else Color.WHITE

func update_visible(player_x: float) -> void:
	for i in chunks.size():
		chunks[i].visible = absf(float(i) * 30.0 + 15.0 - player_x) < 58.0
