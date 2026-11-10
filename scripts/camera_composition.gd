extends RefCounted

static func lead(room: String, place: String, velocity_x: float) -> float:
	var limit: = 0.95 if room == "OUTDOOR" else 0.48 if room == "FOREST_TRAIL" else 0.2
	if place in ["SHADOW_BROOK", "MILL_BROOK"]: limit = 0.3
	return clampf(velocity_x * 0.21, - limit, limit)
static func breathing_room(room: String, place: String, _speed: float) -> float:
	if room != "OUTDOOR": return 0.0
	if place not in ["BEACH", "MOUNTAINS", "PLAINS"]: return 0.0

	return 0.28
