extends RefCounted

const Enemies = preload("res://scripts/regional_enemies.gd")
const Catalog = preload("res://scripts/world_catalog.gd")
const LOCATIONS := {
	"SUN_GLADE": {"kind": "pollen", "lift": 126.0, "tint": Color("fff3d9")}, 
	"MIST_WATCH": {"kind": "mist", "lift": 122.0, "tint": Color("d6e3ed")}, 
	"MILL_BROOK": {"kind": "brook", "lift": 122.0, "tint": Color("e5eed3")}, 
	"ASH_ASCENT": {"kind": "embers", "lift": 112.0, "tint": Color("ffe1be")}, 
	"SEAL_COURT": {"kind": "seals", "lift": 112.0, "tint": Color("eed9ee")}, 
	"RELAY_COURT": {"kind": "relays", "lift": 122.0, "tint": Color("e0e4f3")}, 
	"BEACH": {"kind": "shore", "lift": 126.0, "tint": Color("fff1d5")}
}

static func profile(enemy_id: String, drill: int = -1) -> Dictionary:
	if enemy_id.is_empty() or drill == 0:
		return {"place": "FOREST_TRAIL", "name": "Lichtung der drei Wurzeln", 
			"kind": "roots", "lift": 0.0, "tint": Color("d6e3df"), 
			"asset": "res://assets/forest/three-roots-clearing.png"}
	var enemy: Dictionary = Enemies.data().get(enemy_id, {})
	var place: String = "ASH_ASCENT" if drill == 2 else str(enemy.get("place", ""))
	var region := Catalog.region_for(place)
	var room: Dictionary = Catalog.data().interiors.get(place, {})
	if region.is_empty() and room.is_empty(): return {}
	var result: Dictionary = LOCATIONS.get(place, {"kind": "dust", "lift": 120.0, "tint": Color.WHITE}).duplicate()
	result["place"] = place
	result["name"] = Catalog.label_for(place)
	result["asset"] = region.asset if not region.is_empty() else room.asset
	return result

static func ground(position: Vector2, location: Dictionary, depth: float) -> Vector2:
	if location.is_empty() or location.kind == "roots": return position
	var place: String = location.place
	var region := Catalog.region_for(place)
	var u := clampf(position.x / 1280.0, 0.0, 1.0)
	var band: Vector2
	var image_height: float
	if region.is_empty():
		band = Catalog.room_band(place, u)
		image_height = 270.0
	else:
		band = Catalog.band(region, u, true)
		image_height = 640.0
	position.y = lerpf(band.x, band.y, depth) * 720.0 / image_height - float(location.lift)
	return position

static func draw_atmosphere(view: Control, location: Dictionary, clock: float) -> void :
	var kind: String = location.kind
	var color: Color = location.tint
	if kind in ["mist", "brook", "shore"]:
		for i in 3:
			var drift := sin(clock * 0.23 + i * 1.7) * 33.0
			view._ellipse(Vector2(320 + 235 * i + drift, 336 + 19 * i), Vector2(125, 6), Color(color, 0.035))
	if kind in ["pollen", "brook", "embers", "seals", "relays"]:
		for i in 10:
			var progress := fposmod(clock * (0.13 + float(i % 3) * 0.015) + i * 0.618, 1.0)
			var x := 125.0 + float((i * 137) % 1080) + sin(clock * 0.67 + i) * 14.0
			var y := 435.0 - progress * 180.0
			var opacity := sin(progress * PI) * 0.45
			var tint := Color("ffc482") if kind == "embers" else color
			view.draw_circle(Vector2(x, y), 1.2 if kind == "pollen" else 1.6, Color(tint, opacity), true, -1, true)
	if kind in ["seals", "relays"]:
		for i in 3:
			var at := Vector2(350 + i * 276, 407 + (i % 2) * 8)
			var pulse := 0.12 + 0.06 * sin(clock * 1.7 + i * 2.1)
			view.draw_arc(at, 18 + i * 4, clock * 0.15, PI + clock * 0.15, 24, Color(color, pulse), 1.4, true)
