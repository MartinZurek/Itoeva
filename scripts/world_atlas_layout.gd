extends RefCounted

const World = preload("res://scripts/world_catalog.gd")
const Forest = preload("res://scripts/forest_catalog.gd")
const WORLD_SIZE = Vector2(2390, 1050)
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
	# Der Atlas zeigt Nachbarschaft. Die Laufkamera nutzt weiterhin die Weltkoordinaten.
	var positions := {
		"POND": Vector2(112, 675), "BEACH": Vector2(285, 725), "SWAMP": Vector2(460, 715),
		"JUNGLE": Vector2(635, 675), "COAST_PATH": Vector2(810, 645), "STREET": Vector2(985, 610),
		"PARK": Vector2(1160, 575), "MEADOW": Vector2(1335, 550), "FOREST": Vector2(1510, 545),
		"VILLAGE_EDGE": Vector2(1685, 605), "CITY": Vector2(1860, 675), "SPORT": Vector2(2035, 675),
		"PLAINS": Vector2(2220, 610), "MOUNTAINS": Vector2(2260, 440), "MOUNTAIN_PASS": Vector2(2250, 280),
		"CAMP": Vector2(2220, 110), "GROTTO": Vector2(2040, 95),
		"FOREST_ENTRANCE": Vector2(1365, 405), "GREENWOOD": Vector2(1200, 345), "TORCH_PATH": Vector2(1020, 295),
		"SHADOW_GATE": Vector2(840, 255), "SHADOW_BROOK": Vector2(660, 228),
		"SHADOW_CLEARING": Vector2(480, 205), "SHADOW_DEPTHS": Vector2(300, 180),
		"SUN_GLADE": Vector2(1515, 340), "MIST_WATCH": Vector2(1200, 105), "ASH_ASCENT": Vector2(1380, 105),
		"SEAL_COURT": Vector2(1560, 100), "RELAY_COURT": Vector2(1740, 106), "RING_WEIR": Vector2(1920, 145),
		"MILL_RUN": Vector2(1920, 325), "MILL_BROOK": Vector2(1710, 415), "ALDER_FORD": Vector2(2050, 535),
		"WIND_TERRACES": Vector2(1880, 485), "SIGNAL_WATCH": Vector2(2065, 275),
		"LIVING": Vector2(810, 840), "CRAFT": Vector2(985, 840), "SHOP": Vector2(1160, 840),
		"WORK": Vector2(1685, 865), "CAFE": Vector2(1860, 865), "ARCADE": Vector2(2035, 865),
	}
	for id in positions:
		result.append(node(id, positions[id], -1.0 if id in ["ALDER_FORD", "COAST_PATH"] else 1.0))
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
	var points := PackedVector2Array([a.point])
	# Der Rückweg vom Tiefwald bleibt oberhalb der Karte des durchgehenden Waldpfads.
	if (a.id == "SHADOW_DEPTHS" and b.id == "MIST_WATCH") or (b.id == "SHADOW_DEPTHS" and a.id == "MIST_WATCH"):
		points.append(Vector2(a.point.x, 55))
		points.append(Vector2(b.point.x, 55))
	elif (a.id == "SUN_GLADE" and b.id == "MIST_WATCH") or (b.id == "SUN_GLADE" and a.id == "MIST_WATCH"):
		points.append(Vector2(1288, 248))
	points.append(b.point)
	return points

static func index_of(entries: Array, id: String) -> int:
	for i in entries.size():
		if entries[i].id == id: return i
	return -1
