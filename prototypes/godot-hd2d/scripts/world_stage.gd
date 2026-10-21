extends Node3D

const Catalog = preload("res://scripts/world_catalog.gd")
const Forest = preload("res://scripts/forest_catalog.gd")
const Ambience = preload("res://scripts/painted_ambience.gd")
var place := "OUTDOOR"
var textures: Dictionary = {}
var materials: Array[StandardMaterial3D] = []
var painted_materials: Array[ShaderMaterial] = []
var chunks: Array[Node3D] = []
var doors: Array[Dictionary] = []
var furniture: Array[Dictionary] = []
var width := 210.0
var native_height := 640.0
var zoom_min := 8.2
var zoom_max := 10.2
var default_zoom := 8.8
var base_pixel_size := 0.0145
var body_pixel_size := 0.0115
var jump_height := 1.7
var dusk := false
var building := false
var horizontal_padding := 2.35
var occlusion_pieces: Array[Dictionary] = []
var occlusion_maps: Dictionary = {}
var natural_edges: Array[MeshInstance3D] = []
var draw_bands: Dictionary = {}
var pickups: Array[Dictionary] = []
var collected: Array = []
var local_light_sources: Array[Dictionary] = []
var light_hour := 12.0

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
		door.position = safe_position(raw, 0.0) if door.has("route_direction") else safe_position(raw)
		door.marker_position = door.position + Vector3.UP * (1.8 if door.has("route_direction") else 1.5)

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
		for piece in Catalog.geometry().outdoor_furniture:
			var region := Catalog.region_for(piece.region)
			var traced: Dictionary = piece.duplicate(true)
			traced["place"] = region.places[0]
			_add_piece(traced, region.asset, float(region.origin), float(region.width), 1920.0, 640.0)
		for contour in Catalog.geometry().outdoor_occluders:
			var region := Catalog.region_for(contour.place)
			occlusion_pieces.append({"piece": contour, "asset": region.asset,
				"origin": region.origin, "span": region.width,
				"reference_width": 1920.0, "reference_height": 640.0})
		# Nur die Kiste ist ein wirklich hinzugefuegtes Motiv, keine Kulissenkopie.
		for piece in Catalog.data().world_pieces:
			if piece.id == "park-crate":
				var region := Catalog.region_for(piece.place)
				var span: float = float(region.width) / region.places.size()
				_add_piece(piece, region.asset, float(region.origin) + region.places.find(piece.place) * span, span, 480.0, 640.0)
		_add_pickup("park-pebbles", "PARK", Vector2(0.78, 0.70))
		for door in Forest.doors_for("FOREST"):
			_add_door(door)
	else:
		var room: Dictionary = Catalog.data().interiors[place]
		width = Catalog.IMAGE_HEIGHT * 16.0 / 9.0
		native_height = 270.0
		zoom_min = 10.4
		zoom_max = 11.8
		default_zoom = 11.0
		if room.get("outdoor",false):
			default_zoom=10.0;zoom_min=9.0;zoom_max=10.0
		# Gegenstaende und Figuren behalten das native lokale Raumverhaeltnis.
		# Innen und aussen bleibt die sichtbare Referenzfigur bei Standardzoom gleich.
		base_pixel_size = 0.0145 * default_zoom / 8.8
		body_pixel_size = 0.0115 * 10.0 / 7.6
		# Ein Raumeintritt vergroessert weder den Sprung noch seine Bildschirmhoehe.
		jump_height = 1.7 * default_zoom / 8.8
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
	if place != "OUTDOOR":
		for door in Forest.doors_for(place):
			_add_door(door)
	building = false
	_build_natural_edges()
	_build_occlusion_maps()

func _add_pickup(id: String, location: String, at: Vector2) -> void:
	if id in collected: return
	var art := Sprite3D.new()
	art.texture = load("res://assets/world/path-stones.png")
	art.billboard = BaseMaterial3D.BILLBOARD_ENABLED
	art.shaded = false
	art.texture_filter = BaseMaterial3D.TEXTURE_FILTER_LINEAR
	art.pixel_size = 0.0004
	var bounds := art.texture.get_image().get_used_rect()
	art.offset.y = float(bounds.end.y) - art.texture.get_height() * 0.5
	art.position = safe_position(Catalog.position_for(location, at), 0.22) + Vector3.UP * 0.01
	add_child(art)
	pickups.append({"id": id, "position": art.position, "art": art})

func _build_natural_edges() -> void:
	if place != "OUTDOOR":
		return
	var surface := SurfaceTool.new()
	surface.begin(Mesh.PRIMITIVE_TRIANGLES)
	var serial := 0
	for edge in Catalog.geometry().natural_edges:
		var region := Catalog.region_for(edge.region)
		var x := float(region.origin) + float(edge.u[0]) * 30.0
		var end := float(region.origin) + float(edge.u[1]) * 30.0
		while x < end:
			var limits := _limits_at(x)
			var side := int(edge.side)
			var z := Catalog.image_z(limits[side]) + (-0.10 if side == 0 else 0.10)
			# Einzelne kleine Steine sitzen ausserhalb der begehbaren Flaeche.
			var span := 0.23 + 0.08 * sin(float(serial) * 2.1)
			var rise := span * 0.40
			for corner in [Vector2(0, 0), Vector2(1, 1), Vector2(1, 0), Vector2(0, 0), Vector2(0, 1), Vector2(1, 1)]:
				surface.set_uv(corner)
				surface.add_vertex(Vector3(x + (corner.x - 0.5) * span, (1.0 - corner.y) * rise + 0.008, z))
			serial += 1
			x += float(edge.spacing) * (1.0 + 0.16 * sin(float(serial)))
	var material := _material("res://assets/world/path-stones.png", true)
	material.render_priority = 2
	var mesh := _mesh("NatuerlicheSteinraender", surface, material, self)
	natural_edges.append(mesh)
	for rail in Catalog.geometry().railings:
		var region := Catalog.region_for(rail.region)
		var first := float(region.origin) + float(rail.u[0]) * 30.0
		var last := float(region.origin) + float(rail.u[1]) * 30.0
		var sections := ceili((last - first) / float(rail.spacing))
		var previous := Vector3.ZERO
		for i in range(sections + 1):
			var x := lerpf(first, last, float(i) / sections)
			var z := Catalog.image_z(_limits_at(x)[int(rail.side)]) - 0.07
			var top := Vector3(x, float(rail.height), z)
			_wood_piece("Stegpfosten", top * Vector3(1, 0.5, 1), Vector3(0.055, float(rail.height), 0.06))
			if i > 0:
				_wood_piece("Steggelaender", (previous + top) * 0.5, Vector3(top.x - previous.x + 0.04, 0.045, 0.055))
			previous = top

func _wood_piece(label: String, center: Vector3, dimensions: Vector3) -> void:
	var mesh := MeshInstance3D.new()
	mesh.name = label
	var box := BoxMesh.new()
	box.size = dimensions
	mesh.mesh = box
	var material := ShaderMaterial.new()
	material.shader = load("res://shaders/painted_wood.gdshader")
	mesh.material_override = material
	mesh.position = center
	mesh.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	add_child(mesh)
	_register_rail_mask(center, dimensions)

func _register_rail_mask(center: Vector3, dimensions: Vector3) -> void:
	var region: Dictionary = Catalog.data().regions[clampi(int(center.x / 30.0), 0, 6)]
	var contour: Array = []
	for p in [Vector2(-1,1),Vector2(1,1),Vector2(1,-1),Vector2(-1,-1)]:
		var x: float = center.x + p.x * dimensions.x * .5
		var y: float = center.y + p.y * dimensions.y * .5
		contour.append([(x - float(region.origin)) / 30.0 * 1920.0,
			(Catalog.ORIGIN_V - (cos(deg_to_rad(Catalog.PITCH))*y-sin(deg_to_rad(Catalog.PITCH))*center.z)/Catalog.IMAGE_HEIGHT)*640.0])
	var front := (Catalog.ORIGIN_V + sin(deg_to_rad(Catalog.PITCH))*(center.z+dimensions.z*.5)/Catalog.IMAGE_HEIGHT)*640.0
	occlusion_pieces.append({"piece":{"place":region.places[0],"front":front,"contours":[contour]},
		"asset":region.asset,"origin":region.origin,"span":30.0,"reference_width":1920.0,"reference_height":640.0})

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
	if dry:
		return Catalog.room_band(place, x) if region.has("room_band") else Catalog.band(region, x, true)
	# Ein Gitter verwendet dieselbe Bildspalte tausendfach. Nur ihre erste
	# Auswertung muss das Profil nachzeichnen; Physikwerte werden nicht gerundet.
	var asset: String = region.asset
	if not draw_bands.has(asset):
		draw_bands[asset] = {}
	var columns: Dictionary = draw_bands[asset]
	if not columns.has(x):
		columns[x] = Vector2(region.room_band[0], region.room_band[1]) if region.has("room_band") else Catalog.band(region, x)
	return columns[x]

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
	var material := _painted_material(region.asset)
	_register_painted_lights(region)
	for ground in [false, true]:
		var surface := SurfaceTool.new()
		surface.begin(Mesh.PRIMITIVE_TRIANGLES)
		var rows := 1 if ground else 12
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
		var u0 := samples[i]
		var u1 := samples[i + 1]
		if is_equal_approx(u0, u1):
			continue
		var band0 := _band(region, u0, true)
		var band1 := _band(region, u1, true)
		var x0: float = region.origin + u0 * region.width
		var x1: float = region.origin + u1 * region.width
		if place != "OUTDOOR":
			# Der native Raum ist ein Trapez; die Seitenwaende folgen seinen Grenzen.
			var midpoint := (u0 + u1) * 0.5 * 480.0
			var limits := Catalog.room_limits(place, (band0.x + band1.x) * 0.5)
			if midpoint < limits.x or midpoint > limits.y:
				continue
		var back0 := Vector3(x0, 0, Catalog.image_z(band0.x, native_height))
		var back1 := Vector3(x1, 0, Catalog.image_z(band1.x, native_height))
		var front0 := Vector3(x0, 0, Catalog.image_z(band0.y, native_height))
		var front1 := Vector3(x1, 0, Catalog.image_z(band1.y, native_height))
		_quad(floor_vertices, back0, back1, front1, front0)
		_quad(edge_vertices, back0, back1, back1 + Vector3.UP * 8, back0 + Vector3.UP * 8)
		_quad(edge_vertices, front1, front0, front0 + Vector3.UP * 8, front1 + Vector3.UP * 8)
	_collision("WalkableBand", floor_vertices, parent)
	_collision("PaintedBoundaries", edge_vertices, parent)
	if place != "OUTDOOR":
		var walls := PackedVector3Array()
		var room: Dictionary = Catalog.data().interiors[place]
		for side in [0, 1]:
			var previous := Vector3.ZERO
			for i in range(65):
				var y := lerpf(float(room.band[0]), float(room.band[1]), i / 64.0)
				var limits := Catalog.room_limits(place, y)
				var point := Vector3((limits[side] / 480.0 - 0.5) * width, 0, Catalog.image_z(y, native_height))
				if i > 0:
					_quad(walls, previous, point, point + Vector3.UP * 10, previous + Vector3.UP * 10)
				previous = point
		_collision("RoomSideWalls", walls, parent)

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
	var material := _painted_material(asset, true)
	var entry: Dictionary = Ambience.data().assets.get(asset.get_file(), {})
	var dimensions: Array = entry.get("size", [1086,724])
	for light_index in entry.get("lights", []).size():
		var source: Dictionary = entry.lights[light_index]
		var uv := Vector2(float(source.at[0])/dimensions[0],float(source.at[1])/dimensions[1])
		var x := boundary-7.5+uv.x*15.0
		var region: Dictionary = Catalog.data().regions[index if x<boundary else index+1]
		var base_uv := Vector2((x-float(region.origin))/float(region.width),uv.y)
		local_light_sources.append({"position":_point(region,base_uv,uv.y>=_band(region,base_uv.x).x/640.0),
			"radius":Vector2(float(source.radius[0])/dimensions[0]*15.0,float(source.radius[1])/dimensions[1]*10.0),
			"strength":float(source.get("strength",.3)),"fire":source.kind=="fire",
			"seed":float(light_index)*1.73+uv.x*13.0+uv.y*7.0,"arena":false})
	var surface := SurfaceTool.new()
	surface.begin(Mesh.PRIMITIVE_TRIANGLES)
	for ground in [false, true]:
		var rows := 1 if ground else 12
		for row in rows:
			for col in 48:
				for corner in [Vector2(0, 0), Vector2(1, 1), Vector2(1, 0), Vector2(0, 0), Vector2(0, 1), Vector2(1, 1)]:
					var u: float = (col + corner.x) / 48.0
					var x := boundary - 7.5 + u * 15.0
					var region: Dictionary = Catalog.data().regions[index if x < boundary else index + 1]
					var base_u: float = (x - region.origin) / region.width
					var band := _band(region, base_u)
					var v := lerpf(band.x / 640.0, 1.0, (row + corner.y) / float(rows)) if ground else lerpf(0, band.x / 640.0, (row + corner.y) / float(rows))
					var alpha := smoothstep(0.0, 2.0, 7.5 - absf(x - boundary))
					surface.set_color(Color(1, 1, 1, alpha))
					surface.set_uv(Vector2(u, v))
					var p := _point(region, Vector2(base_u, v), ground)
					p += Vector3(0, 0.006, 0.006)
					surface.add_vertex(p)
	_mesh("PaintedSeam%s" % index, surface, material, self)

func _add_piece(piece: Dictionary, asset: String, origin_x: float, span: float, reference_width: float, reference_height: float) -> void:
	var ground: float = piece.ground
	var back := Catalog.image_z(piece.back, reference_height)
	var front := Catalog.image_z(piece.front, reference_height)
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
	if piece.has("paint_asset"):
		var material := _material(piece.paint_asset)
		var surface := SurfaceTool.new()
		surface.begin(Mesh.PRIMITIVE_TRIANGLES)
		for contour in piece.contours:
			var polygon := PackedVector2Array()
			for p in contour:
				polygon.append(Vector2(p[0], p[1]))
			for i in Geometry2D.triangulate_polygon(polygon):
				var p := polygon[i]
				surface.set_uv(Vector2((p.x - float(piece.left)) / (float(piece.right) - float(piece.left)),
					(p.y - float(piece.top)) / (float(piece.ground) - float(piece.top))))
				surface.add_vertex(Vector3(origin_x + p.x / reference_width * span,
					Catalog.image_y(p.y, front + 0.015, reference_height), front + 0.015))
		_mesh(piece.id + "PaintedFront", surface, material, self)
	else:
		# Moebel sind bereits vollstaendig im Bild. Eine zweite, rechteckige Kopie
		# uebermalte bei Kameraschwenks Boden und Figuren. Nur eine Tiefenmaske bleibt.
		occlusion_pieces.append({"piece": piece, "asset": asset, "origin": origin_x,
			"span": span, "reference_width": reference_width, "reference_height": reference_height})
	var item := piece.duplicate(true)
	item["world_center"] = Vector3((x0 + x1) * 0.5, height, (back + front) * 0.5)
	item["body"] = body
	item["approach_position"] = Vector3((x0 + x1) * 0.5, 0.03, front + 0.55)
	var points := PackedVector3Array()
	var region: Dictionary = Catalog.data().regions[clampi(int((x0 + x1) * 0.5 / 30.0), 0, 6)].duplicate() if place == "OUTDOOR" else {"asset": asset, "origin": origin_x, "width": span, "room_band": Catalog.data().interiors[place].band}
	for contour in piece.contours:
		for p in contour:
			var uv := Vector2((origin_x + float(p[0]) / reference_width * span - float(region.origin)) / float(region.width), float(p[1]) / reference_height)
			points.append(_point(region, uv, uv.y >= _band(region, uv.x).x / native_height))
	item["visual_points"] = points
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
	if entry.has("route_direction"):
		entry.position = safe_position(entry.position, 0.0)
	entry["marker_position"] = entry.position + Vector3.UP * (1.8 if entry.has("route_direction") else 1.0 if place == "OUTDOOR" else 1.5)
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
		band = Catalog.room_band(place, result.x / width + 0.5)
		var px_y := Catalog.ORIGIN_V * native_height + result.z * sin(deg_to_rad(Catalog.PITCH)) / Catalog.IMAGE_HEIGHT * native_height
		var sides := Catalog.room_limits(place, px_y)
		result.x = clampf(result.x, (sides.x / 480.0 - 0.5) * width + radius, (sides.y / 480.0 - 0.5) * width - radius)
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
	if not is_walkable(result, radius):
		# Auch ueberlappende Moebel und ein alter Spielstand werden auf eine wirklich
		# freie Bodenstelle gesetzt, statt nach dem letzten Schub im Schrank zu landen.
		var best := Vector3.ZERO
		var best_distance := INF
		var start_x := maxf(2.3 if place == "OUTDOOR" else -width * 0.5 + horizontal_padding, result.x - 8.0)
		var end_x := minf(width - 2.3 if place == "OUTDOOR" else width * 0.5 - horizontal_padding, result.x + 8.0)
		for ix in range(81):
			var x := lerpf(start_x, end_x, ix / 80.0)
			var limits := _limits_at(x)
			for iz in range(25):
				var z := lerpf(Catalog.image_z(limits.x, native_height) + radius + 0.05, Catalog.image_z(limits.y, native_height) - radius - 0.05, iz / 24.0)
				var point := Vector3(x, 0.03, z)
				var distance := point.distance_squared_to(raw)
				if distance < best_distance and is_walkable(point, radius):
					best = point
					best_distance = distance
		if best_distance < INF:
			result = best
	return result

func _limits_at(x: float) -> Vector2:
	if place != "OUTDOOR":
		return Catalog.room_band(place, x / width + 0.5)
	var region: Dictionary = Catalog.data().regions[clampi(int(x / 30.0), 0, 6)]
	return _band(region, (x - float(region.origin)) / 30.0, true)

func is_walkable(point: Vector3, radius: float = 0.0, allow_platforms: bool = false) -> bool:
	for offset in [Vector2.ZERO, Vector2(radius, 0), Vector2(-radius, 0), Vector2(0, radius), Vector2(0, -radius)]:
		var x: float = point.x + offset.x
		var z: float = point.z + offset.y
		var limits := _limits_at(x)
		if z < Catalog.image_z(limits.x, native_height) or z > Catalog.image_z(limits.y, native_height):
			return false
		if place != "OUTDOOR":
			var y: float = (Catalog.ORIGIN_V + z * sin(deg_to_rad(Catalog.PITCH)) / Catalog.IMAGE_HEIGHT) * native_height
			var sides := Catalog.room_limits(place, y)
			if x < (sides.x / 480.0 - 0.5) * width or x > (sides.y / 480.0 - 0.5) * width:
				return false
		elif x < 1.9 or x > width - 1.9:
			return false
	for item in furniture:
		var body: StaticBody3D = item.body
		var box: BoxShape3D = body.get_child(0).shape
		if allow_platforms and point.y >= body.position.y + box.size.y * 0.5 - 0.06:
			continue
		if absf(point.x - body.position.x) < box.size.x * 0.5 + radius and absf(point.z - body.position.z) < box.size.z * 0.5 + radius:
			return false
	return true

func constrain_step(before: Vector3, proposed: Vector3, radius: float) -> Vector3:
	# Auch ein Sprung, ein Rollenimpuls und hohe Bildzeit duerfen keinen Wegsaum
	# ueberspringen. Die Bodenprojektion bleibt innerhalb derselben Geometrie.
	if _segment_walkable(before, proposed, radius):
		return proposed
	var horizontal := Vector3(proposed.x, proposed.y, before.z)
	if _segment_walkable(before, horizontal, radius):
		return horizontal
	var depth := Vector3(before.x, proposed.y, proposed.z)
	if _segment_walkable(before, depth, radius):
		return depth
	return Vector3(before.x, proposed.y, before.z)

func guide_motion(point: Vector3, motion: Vector3, delta: float, radius: float) -> Vector3:
	# Gemalte Steigungen liegen in der X/Z-Ebene. An ihrem Rand muss ein
	# seitlicher Daumen dem Weg folgen, statt immer gegen die Uferwand zu drücken.
	if place != "OUTDOOR" and place != "FOREST_TRAIL" and not Catalog.data().interiors[place].get("outdoor", false):
		return motion
	var planar := Vector2(motion.x, motion.z)
	if absf(planar.x) < 0.05 or absf(planar.y) > absf(planar.x) * 0.75:
		return motion
	var travel := maxf(planar.length() * delta, 0.10)
	var ahead := point + Vector3(planar.x, 0, planar.y).normalized() * travel
	if _segment_walkable(point, ahead, radius + 0.015):
		return motion
	var band := _limits_at(point.x)
	var next := _limits_at(point.x + signf(planar.x) * 0.15)
	var fraction := clampf(inverse_lerp(Catalog.image_z(band.x, native_height), Catalog.image_z(band.y, native_height), point.z), 0, 1)
	var rise := Catalog.image_z(lerpf(next.x, next.y, fraction), native_height) - Catalog.image_z(lerpf(band.x, band.y, fraction), native_height)
	var tangent := Vector2(signf(planar.x) * 0.15, rise).normalized()
	if tangent.dot(planar.normalized()) < 0.3 or absf(rise) < 0.0001:
		return motion
	var guided := Vector3(tangent.x, 0, tangent.y) * planar.length()
	# Nur derselbe zusammenhängende, freie Weg ist erlaubt; keine Abkürzung
	# über Wasser, Möbel oder über das Ende der Karte.
	if _segment_walkable(point, point + guided.normalized() * travel, radius):
		return Vector3(guided.x, motion.y, guided.z)
	return motion

func _segment_walkable(before: Vector3, after: Vector3, radius: float) -> bool:
	var samples := maxi(1, ceili(Vector2(after.x - before.x, after.z - before.z).length() / 0.10))
	for i in range(1, samples + 1):
		if not is_walkable(before.lerp(after, float(i) / samples), radius, true):
			return false
	return true

func _build_occlusion_maps() -> void:
	for entry in occlusion_pieces:
		var ref_width := 1920 if place == "OUTDOOR" else 480
		var ref_height := 640 if place == "OUTDOOR" else 270
		if not occlusion_maps.has(entry.asset):
			occlusion_maps[entry.asset] = Image.create(ref_width, ref_height, false, Image.FORMAT_RG8)
		var mask: Image = occlusion_maps[entry.asset]
		var region_origin: float = Catalog.region_for(entry.piece.place).get("origin", entry.origin) if place == "OUTDOOR" else entry.origin
		var region_span := 30.0 if place == "OUTDOOR" else width
		for contour in entry.piece.contours:
			var polygon := PackedVector2Array()
			for p in contour:
				polygon.append(Vector2((entry.origin - region_origin + float(p[0]) / entry.reference_width * entry.span) / region_span * ref_width, p[1]))
			var bounds := Rect2(polygon[0], Vector2.ZERO)
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
	var asset: String = region.asset if place == "OUTDOOR" else Catalog.data().interiors[place].asset
	return {"mask": occlusion_maps.get(asset), "origin": region.origin if place == "OUTDOOR" else -width * 0.5,
		"span": 30.0 if place == "OUTDOOR" else width}

func set_dusk(enabled: bool) -> void:
	dusk = enabled
	set_time(19.5 if enabled else 12.0)
	for material in materials:
		material.albedo_color = Color(0.72, 0.68, 0.78) if enabled and place == "OUTDOOR" else Color.WHITE

func update_visible(player_x: float) -> void:
	for i in chunks.size():
		chunks[i].visible = absf(float(i) * 30.0 + 15.0 - player_x) < 58.0

func furniture_screen_rect(item: Dictionary, view: Camera3D) -> Rect2:
	var bounds := Rect2()
	var first := true
	for point in item.visual_points:
		var screen: Vector2 = view.unproject_position(point)
		if first:
			bounds = Rect2(screen, Vector2.ZERO)
			first = false
		else: bounds = bounds.expand(screen)
	return bounds

func _painted_material(asset: String, transparent: bool = false) -> ShaderMaterial:
	var texture: Texture2D = textures.get(asset, load(asset))
	var material := Ambience.create_material(asset, texture, transparent)
	material.set_shader_parameter("transparent_piece", transparent)
	if asset == "res://assets/world/coast.png":
		material.set_shader_parameter("shore_enabled", true)
		material.set_shader_parameter("shore_texture", load("res://assets/world/coast-shore-v026.png"))
	painted_materials.append(material)
	return material

func set_time(hour: float) -> void:
	light_hour = hour
	for material in painted_materials: Ambience.set_time(material, hour, place != "OUTDOOR" and not Catalog.data().interiors[place].get("outdoor",false))
	var daylight := smoothstep(5.8, 8.0, hour) * (1.0 - smoothstep(18.0, 21.0, hour))
	for material in materials:
		material.albedo_color = Color(0.32, 0.40, 0.58).lerp(Color.WHITE, daylight) if place == "OUTDOOR" else Color.WHITE

func _register_painted_lights(region: Dictionary) -> void:
	var entry: Dictionary = Ambience.data().assets.get(String(region.asset).get_file(), {})
	var size: Array = entry.get("size", [2172, 724])
	for index in entry.get("lights", []).size():
		var source: Dictionary = entry.lights[index]
		var uv := Vector2(float(source.at[0])/size[0], float(source.at[1])/size[1])
		var anchor := _point(region, uv, uv.y >= _band(region,uv.x).x/native_height)
		local_light_sources.append({"position":anchor,
			"radius":Vector2(float(source.radius[0])/size[0]*float(region.width),float(source.radius[1])/size[1]*10.0),
			"strength":float(source.get("strength",0.3)), "fire":source.kind=="fire",
			"seed":float(index)*1.73+uv.x*13.0+uv.y*7.0, "arena":false})

func apply_actor_light(material: ShaderMaterial, point: Vector3) -> void:
	var sources: Array[Dictionary] = []
	var center := Vector2(point.x, .819152*point.y-.573576*point.z+.75)
	for source in local_light_sources:
		if source.get("forest",false) and float(source.get("gain",0.0))<=0.0: continue
		var position: Vector3 = source.position
		var projected := Vector2(position.x,.819152*position.y-.573576*position.z)
		var distance: Vector2 = (center-projected)/source.radius
		if distance.length_squared()<2.0:
			var candidate := source.duplicate()
			candidate["projected"] = projected
			candidate["distance"] = distance.length_squared()
			sources.append(candidate)
	sources.sort_custom(func(a,b): return a.distance < b.distance)
	var points := PackedVector4Array()
	var colors := PackedVector4Array()
	var phases := PackedVector4Array()
	var daylight := smoothstep(5.8,8.0,light_hour)*(1.0-smoothstep(18.0,21.0,light_hour))
	for index in 4:
		if index>=sources.size():
			points.append(Vector4.ZERO); colors.append(Vector4.ZERO); phases.append(Vector4.ZERO)
			continue
		var source: Dictionary = sources[index]
		var gain := lerpf(.95,.36,daylight) if source.fire else lerpf(.82,.04,daylight)
		if source.get("forest",false): gain = float(source.get("gain",1.0))
		points.append(Vector4(source.projected.x,source.projected.y,source.radius.x,source.radius.y))
		colors.append(Vector4(1.0,.54,.18,float(source.strength)*gain))
		phases.append(Vector4(float(source.seed),1.0 if source.fire else 0.0,0.0,0.0))
	material.set_shader_parameter("local_light_count",mini(sources.size(),4))
	material.set_shader_parameter("local_light_points",points)
	material.set_shader_parameter("local_light_colors",colors)
	material.set_shader_parameter("local_light_phases",phases)

func darkness_at(point: Vector3) -> float:
	var daylight:=smoothstep(5.8,8.0,light_hour)*(1.0-smoothstep(18.0,21.0,light_hour))
	return (1.0-daylight)*(.6 if place!="OUTDOOR" else 1.0)

func update_character_light(actor: Node3D, energy: float) -> void:
	if not actor.player_controlled:return
	var center: Vector3=actor.light_center()
	for material in painted_materials:
		material.set_shader_parameter("carried_position",center)
		material.set_shader_parameter("carried_gain",energy)
