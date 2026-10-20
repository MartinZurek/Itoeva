extends RefCounted

const World = preload("res://scripts/world_catalog.gd")
static var catalogue: Dictionary = {}

static func data() -> Dictionary:
	if catalogue.is_empty():
		catalogue = JSON.parse_string(FileAccess.get_file_as_string("res://data/forest_trail.json"))
	return catalogue

static func region_at(x: float) -> Dictionary:
	return data().segments[clampi(floori(x / 20.0), 0, data().segments.size() - 1)]

static func raw_band(region: Dictionary, u: float) -> Vector2:
	return World.profile(region.path, clampf(u, 0.0, 1.0)) * 640.0

static func limits(x: float) -> Vector2:
	var region := region_at(x)
	var band := raw_band(region, (x - float(region.origin)) / float(region.width))
	var index: int = data().segments.find(region)
	# Beide gemalten Pfade und die Physik teilen im Ueberlappungsbereich ein Profil.
	if index > 0 and x < float(region.origin) + 2.0:
		var previous: Dictionary = data().segments[index - 1]
		var before := raw_band(previous, (x - float(previous.origin)) / float(previous.width))
		band = before.lerp(band, smoothstep(0.0, 2.0, x - float(region.origin)))
	return band

static func position_for(index: int, u: float = 0.5, lane: float = 0.5) -> Vector3:
	var region: Dictionary = data().segments[clampi(index, 0, data().segments.size() - 1)]
	var x: float = region.origin + clampf(u, 0.08, 0.92) * region.width
	var band := limits(x)
	return Vector3(x, 0.03, World.image_z(lerpf(band.x, band.y, lane)))

# Beide Buehnen lesen dieselben Endpunkte: Ein versetzter Zugang darf seinen
# Rueckweg nicht an einer alten, separat hinterlegten Koordinate zuruecklassen.
static func doors_for(place: String) -> Array[Dictionary]:
	var entrance: Dictionary = data().entrance
	var links: Array = [{"to": entrance.from_place, "arrival": entrance.at,
		"index": 0, "at": entrance.arrival, "label": "Waldpfad"}]
	links.append_array(data().branches)
	var result: Array[Dictionary] = []
	for link in links:
		var trail_at := Vector2(link.at[0], link.at[1])
		var other_at := Vector2(link.arrival[0], link.arrival[1])
		if place == "FOREST_TRAIL":
			result.append({"from": place, "to": link.to, "at": trail_at,
				"arrival": other_at, "segment_index": int(link.index),
				"route_direction": "LEFT" if int(link.index) == 0 else "UP"})
		elif place == link.to:
			result.append({"from": place, "to": "FOREST_TRAIL", "at": other_at,
				"arrival": trail_at, "forest_index": int(link.index), "label": link.label,
				"route_direction": "RIGHT" if int(link.index) == 0 else "UP"})
	return result
