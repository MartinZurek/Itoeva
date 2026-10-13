extends RefCounted

const STEP := 0.35

static func clear_segment(stage: Node3D, a: Vector3, b: Vector3, radius: float) -> bool:
	var count := maxi(1, ceili(Vector2(a.x - b.x, a.z - b.z).length() / 0.09))
	for i in range(count + 1):
		if not stage.is_walkable(a.lerp(b, float(i) / count), radius): return false
	return true

static func find_path(stage: Node3D, start: Vector3, destination: Vector3, radius: float) -> PackedVector3Array:
	start.y = 0.03
	destination = stage.safe_position(Vector3(destination.x, 0.03, destination.z), radius)
	if clear_segment(stage, start, destination, radius): return PackedVector3Array([destination])
	if stage.place in ["OUTDOOR","FOREST_TRAIL"]:
		var spine := _spine_path(stage, start, destination, radius)
		if not spine.is_empty(): return _simplify(stage, start, spine, radius)
	# The graph follows the very same dry path and furniture footprint as physics.
	# It is local to the tapped destination, never a straight line across water.
	var graph := AStar2D.new()
	var cells: Dictionary = {}
	var min_x := minf(start.x, destination.x) - 3.0
	var max_x := maxf(start.x, destination.x) + 3.0
	var first_z := minf(start.z, destination.z) - 5.0
	var last_z := maxf(start.z, destination.z) + 5.0
	var serial := 0
	for ix in range(floori(min_x / STEP), ceili(max_x / STEP) + 1):
		for iz in range(floori(first_z / STEP), ceili(last_z / STEP) + 1):
			var point := Vector3(ix * STEP, 0.03, iz * STEP)
			if stage.is_walkable(point, radius):
				cells[Vector2i(ix, iz)] = serial
				graph.add_point(serial, Vector2(point.x, point.z))
				serial += 1
	for cell in cells:
		for offset in [Vector2i(1, 0), Vector2i(0, 1), Vector2i(1, 1), Vector2i(1, -1)]:
			var other: Vector2i = cell + offset
			if not cells.has(other): continue
			var a: Vector2 = graph.get_point_position(cells[cell])
			var b: Vector2 = graph.get_point_position(cells[other])
			if clear_segment(stage, Vector3(a.x, 0.03, a.y), Vector3(b.x, 0.03, b.y), radius):
				graph.connect_points(cells[cell], cells[other])
	if stage.place in ["OUTDOOR","FOREST_TRAIL"]:
		# Narrow, curved decks can fit the body while falling between grid rows.
		# Add the traced dry band's centre as a continuous spine, without widening it.
		var previous_id := -1
		for ix in range(floori(min_x / 0.10), ceili(max_x / 0.10) + 1):
			var x := ix * 0.10
			var band: Vector2 = stage._limits_at(x)
			var point := Vector3(x, 0.03, stage.Catalog.image_z((band.x + band.y) * 0.5, stage.native_height))
			if not stage.is_walkable(point, radius):
				previous_id = -1
				continue
			var id := serial
			serial += 1
			graph.add_point(id, Vector2(point.x, point.z))
			if previous_id >= 0:
				var prev := graph.get_point_position(previous_id)
				if clear_segment(stage, Vector3(prev.x, 0.03, prev.y), point, radius): graph.connect_points(previous_id, id)
			previous_id = id
			if ix % 4 != 0: continue
			var cell := Vector2i(roundi(point.x / STEP), roundi(point.z / STEP))
			for dx in range(-1, 2):
				for dz in range(-1, 2):
					var nearby := cell + Vector2i(dx, dz)
					if not cells.has(nearby): continue
					var grid_id: int = cells[nearby]
					var p := graph.get_point_position(grid_id)
					if clear_segment(stage, Vector3(p.x, 0.03, p.y), point, radius): graph.connect_points(grid_id, id)
	var ends: Array[int] = []
	for endpoint in [start, destination]:
		var nearest := -1
		var distance := INF
		for id in graph.get_point_ids():
			var p := graph.get_point_position(id)
			var point := Vector3(p.x, 0.03, p.y)
			var gap: float = endpoint.distance_squared_to(point)
			if gap < distance and clear_segment(stage, endpoint, point, radius):
				nearest = id
				distance = gap
		ends.append(nearest)
	if ends[0] < 0 or ends[1] < 0: return PackedVector3Array()
	var path := PackedVector3Array()
	for point in graph.get_point_path(ends[0], ends[1]):
		path.append(Vector3(point.x, 0.03, point.y))
	if path.is_empty(): return path
	path.append(destination)
	return _simplify(stage, start, path, radius)

static func _simplify(stage: Node3D, start: Vector3, path: PackedVector3Array, radius: float) -> PackedVector3Array:
	# Preserve each checked bend; stop extending a shortcut when the first
	# intervening bend leaves the dry footprint, instead of rescanning the whole route.
	var result := PackedVector3Array()
	var from := start
	while not path.is_empty():
		var farthest := 0
		for i in path.size():
			if clear_segment(stage, from, path[i], radius): farthest = i
			else: break
		from = path[farthest]
		result.append(from)
		path = path.slice(farthest + 1)
	return result

static func _spine_path(stage: Node3D, start: Vector3, end: Vector3, radius: float) -> PackedVector3Array:
	var result := PackedVector3Array()
	var previous := start
	var count := maxi(1, ceili(absf(end.x - start.x) / 0.10))
	for i in range(count + 1):
		var x := lerpf(start.x, end.x, float(i) / count)
		var band: Vector2 = stage._limits_at(x)
		var point := Vector3(x, 0.03, stage.Catalog.image_z((band.x + band.y) * 0.5, stage.native_height))
		if not clear_segment(stage, previous, point, radius): return PackedVector3Array()
		result.append(point)
		previous = point
	if not clear_segment(stage, previous, end, radius): return PackedVector3Array()
	result.append(end)
	return result
