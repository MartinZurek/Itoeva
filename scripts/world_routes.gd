extends RefCounted

const World = preload("res://scripts/world_catalog.gd")
const Forest = preload("res://scripts/forest_catalog.gd")

static func connections() -> Array:
	var result: Array = []
	var outdoor: Array = []
	for region in World.data().regions:
		outdoor.append_array(region.places)
	for i in range(1, outdoor.size()):
		result.append([outdoor[i - 1], outdoor[i]])
	for door in World.data().doors:
		result.append([door.a, door.b])
	var segments: Array = Forest.data().segments
	for i in range(1, segments.size()):
		result.append([segments[i - 1].id, segments[i].id])
	result.append([Forest.data().entrance.from_place, segments[0].id])
	for branch in Forest.data().branches:
		result.append([segments[int(branch.index)].id, branch.to])
	return result

static func map_links(nodes: Array) -> Array:
	var membership: Dictionary = {}
	for i in nodes.size():
		for place in nodes[i].places: membership[place] = i
	var result: Array = []
	for pair in connections():
		if not membership.has(pair[0]) or not membership.has(pair[1]): continue
		var a: int = membership[pair[0]]
		var b: int = membership[pair[1]]
		if a != b and [a, b] not in result and [b, a] not in result:
			result.append([a, b])
	return result

static func path_between(start: String, goal: String) -> Array[String]:
	var previous: Dictionary = {start: ""}
	var pending: Array[String] = [start]
	var edges: = connections()
	while not pending.is_empty():
		var current: String = pending.pop_front()
		if current == goal:
			var path: Array[String] = [goal]
			while path[0] != start: path.push_front(previous[path[0]])
			return path
		for edge in edges:
			if current not in edge: continue
			var next: String = edge[1] if edge[0] == current else edge[0]
			if previous.has(next): continue
			previous[next] = current
			pending.append(next)
	return []

static func label(place: String) -> String:
	for segment in Forest.data().segments:
		if segment.id == place: return str(segment.label)
	return str(World.data().labels.get(place, place))

static func arrival_facing(door: Dictionary) -> Vector2:

	match str(door.get("route_direction", "")):
		"LEFT": return Vector2.RIGHT
		"RIGHT": return Vector2.LEFT
		"UP": return Vector2.DOWN
		"DOWN": return Vector2.UP
	var at: Vector2 = door.at
	if at.x < 0.2: return Vector2.RIGHT
	if at.x > 0.8: return Vector2.LEFT
	return Vector2.DOWN if at.y < 0.5 else Vector2.UP
