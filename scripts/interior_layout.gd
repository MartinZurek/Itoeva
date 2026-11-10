extends RefCounted


const COMPACT_SCALE: = 0.88
const ROOM_SCALES: = {"NOOK": 0.86, "BATH": 0.86, "CRAFT": 0.9, "SHOP": 0.92, "CAFE": 0.92, "ARCADE": 0.92}
static func scale_for(room: Dictionary, id: String = "") -> float:
	return 1.0 if room.get("outdoor", false) else float(ROOM_SCALES.get(id, COMPACT_SCALE))
static func restore(point: Vector3, old_scale: float, new_scale: float) -> Vector3:
	var previous: = old_scale if is_finite(old_scale) and old_scale > 0.1 else 1.0
	return Vector3(point.x * new_scale / previous, 0.03, point.z * new_scale / previous)
static func action_for(id: String, seated: bool) -> String:
	if "bookcase" in id or "bookshelf" in id or id == "nook-armchair": return "read"
	if id == "craft-workbench": return "practice"
	if id == "arcade-console": return "card_reference"
	if id == "work-table": return "deck"
	if id in ["kitchen-counter", "kitchen-table", "cafe-table"]: return "activity"
	if id == "desk-table": return "map"
	if id == "bedroom-bed": return "sleep"
	return "seat" if seated else ""

static func purpose_for(room: String) -> String:
	return {
		"CRAFT": "Deck erproben", 
		"WORK": "Deck zusammenstellen", 
		"DESK": "Route planen", 
		"ARCADE": "Kartenregeln nachschlagen", 
		"LIVING": "Platz nehmen und Bewohner treffen", 
		"CAFE": "Eine Teepause teilen", 
	}.get(room, "")
