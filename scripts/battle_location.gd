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

static func profile(enemy_id: String) -> Dictionary:
	if enemy_id.is_empty():
		return {"place": "SHADOW_FOREST", "name": "Schattenwald", "asset": "res://assets/forest/three-roots-clearing.png", "kind": "roots", "lift": 0.0, "tint": Color.WHITE}
	if not Enemies.data().has(enemy_id): return {}
	var place: String = Enemies.data()[enemy_id].place
	var result: Dictionary = LOCATIONS.get(place, {}).duplicate()
	if result.is_empty(): return result
	result["place"] = place
	result["name"] = Catalog.label_for(place, "mosslyn")
	var region: Dictionary = Catalog.region_for(place)
	result["asset"] = region.asset if not region.is_empty() else Catalog.data().interiors[place].asset
	if enemy_id == "ash_hound":
		result["asset"] = "res://assets/forest/three-roots-clearing.png"
		result["kind"] = "roots"
		result["lift"] = 0.0
	return result

static func draw_atmosphere(view: Control, location: Dictionary, clock: float) -> void:
	var kind: String = location.kind
	var color: Color = location.tint
	if kind in ["mist", "brook", "shore"]:
		for i in 3:
			var drift := sin(clock * .23 + i * 1.7) * 33.0
			view._ellipse(Vector2(320 + 235 * i + drift, 336 + 19 * i), Vector2(125, 6), Color(color, .035))
	if kind in ["pollen", "brook", "embers", "seals", "relays"]:
		for i in 10:
			var progress := fposmod(clock * (.13 + float(i % 3) * .015) + i * .618, 1.0)
			var x := 125.0 + float((i * 137) % 1080) + sin(clock * .67 + i) * 14.0
			var y := 435.0 - progress * 180.0
			var opacity := sin(progress * PI) * .45
			var tint := Color("ffc482") if kind == "embers" else color
			view.draw_circle(Vector2(x, y), 1.2 if kind == "pollen" else 1.6, Color(tint, opacity), true, -1, true)
	if kind in ["seals", "relays"]:
		for i in 3:
			var at := Vector2(350 + i * 276, 407 + (i % 2) * 8)
			var pulse := .12 + .06 * sin(clock * 1.7 + i * 2.1)
			view.draw_arc(at, 18 + i * 4, clock * .15, PI + clock * .15, 24, Color(color, pulse), 1.4, true)
