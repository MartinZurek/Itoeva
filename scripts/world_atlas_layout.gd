extends RefCounted

const World = preload("res://scripts/world_catalog.gd")
const Forest = preload("res://scripts/forest_catalog.gd")
const Routes = preload("res://scripts/world_routes.gd")
const WORLD_SIZE = Vector2(2320, 900)
const HOUSE_SIZE = Vector2(1120, 780)

static func node(id: String, point: Vector2, side: float = 1.0) -> Dictionary:
	return {"id": id, "places": [id], "point": point, "side": side}

static func nodes(view: String) -> Array[Dictionary]:
	var result: Array[Dictionary] = []
	if view == "rooms":


		var positions = {"STREET": Vector2(210, 640), "LIVING": Vector2(210, 430), "HALL": Vector2(390, 270), 
			"KITCHEN": Vector2(430, 470), "NOOK": Vector2(150, 200), "BEDROOM": Vector2(190, 70), 
			"DESK": Vector2(410, 70), "BATH": Vector2(610, 70), "CRAFT": Vector2(540, 640), "SHOP": Vector2(800, 640), 
			"CITY": Vector2(820, 440), "WORK": Vector2(660, 250), "CAFE": Vector2(850, 250), "ARCADE": Vector2(1040, 250)}
		for id in positions: result.append(node(id, positions[id]))
		return result
	var index: = 0
	for region in World.data().regions:
		for place in region.places:
			var at: = World.position_for(place, Vector2(0.5, 0.5))


			result.append(node(place, Vector2(80 + at.x * 10, 620), -1.0 if index % 2 == 0 else 1.0));index += 1
	for i in Forest.data().segments.size(): result.append(node(Forest.data().segments[i].id, Vector2(920 + i * 190, 130)))
	var branches = {"SUN_GLADE": Vector2(1050, 350), "MIST_WATCH": Vector2(1330, 350), "ASH_ASCENT": Vector2(1570, 350), 
		"SEAL_COURT": Vector2(1810, 350), "RELAY_COURT": Vector2(2070, 350), "MILL_BROOK": Vector2(1090, 470), 
		"LIVING": Vector2(620, 420), "CRAFT": Vector2(450, 420), "SHOP": Vector2(790, 420), 
		"WORK": Vector2(1260, 780), "CAFE": Vector2(1430, 780), "ARCADE": Vector2(1600, 780)}
	for id in branches: result.append(node(id, branches[id]))
	return result

static func continuous(a: String, b: String) -> bool:
	var places: Array = []
	for region in World.data().regions: places.append_array(region.places)
	var i: = places.find(a); var j: = places.find(b)
	if i >= 0 and j >= 0: return absi(i - j) == 1
	places.clear()
	for segment in Forest.data().segments: places.append(segment.id)
	i = places.find(a);j = places.find(b)
	return i >= 0 and j >= 0 and absi(i - j) == 1

static func line(a: Dictionary, b: Dictionary) -> PackedVector2Array:
	var points: = PackedVector2Array([a.point])
	var ids: Array = [a.id, b.id]


	if "SHADOW_DEPTHS" in ids and "MIST_WATCH" in ids:
		points.append(Vector2(a.point.x, 250));points.append(Vector2(b.point.x, 250))
	points.append(b.point)
	return points

static func index_of(entries: Array, id: String) -> int:
	for i in entries.size():
		if entries[i].id == id: return i
	return -1
