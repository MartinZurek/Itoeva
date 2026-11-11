extends RefCounted
const Collection = preload("res://scripts/card_collection.gd")
const Catalog = preload("res://scripts/world_catalog.gd")
const Forest = preload("res://scripts/forest_catalog.gd")
const Mill=preload("res://scripts/mill_chronicle.gd")
static func locations(app) -> Array:
	if app.stage.has_meta("card_find_anchors"): return app.stage.get_meta("card_find_anchors")
	var result: Array = []
	for find in Collection.data().finds:
		if find.place != app.room: continue
		var raw: Vector3 = Forest.position_for(int(find.index), find.at[0], find.at[1]) if app.room == "FOREST_TRAIL" else Catalog.position_for(find.place, Vector2(find.at[0], find.at[1]))
		if find.has("furniture"):
			for item in app.stage.furniture:
				if item.id == find.furniture: raw = item.approach_position
		var entry: Dictionary = find.duplicate(true);entry["point"] = app.stage.safe_position(raw, app.actor.collider.shape.radius);result.append(entry)
	app.stage.set_meta("card_find_anchors", result);return result
static func offer_for(app, id: String) -> Dictionary:
	for find in locations(app):
		if find.id != id: continue
		if Collection.state(app.save_data, app.selected).claims.get("find:" + id, false): return {}
		var point: Vector3 = find.point; var gap: = Vector2(app.actor.position.x - point.x, app.actor.position.z - point.z).length()
		var mill = Mill.current(app) if id==Mill.FIND else null
		var can_claim: bool=id!=Mill.FIND or (mill!=null and mill.route==Mill.Route.WHEEL)
		return {"can_claim":can_claim,"kind":"card_find","id":id,"label":find.name+" · Untersuchen","position":point+Vector3.UP*.4,"approach":point,"distance":gap,"ready":gap<1.4 and app.actor.is_on_floor() and not app.actor.mounted and app._interaction_clear(point),"find":find}
	return {}
static func offers(app) -> Array:
	var result: Array = []
	var lever:=Mill.offer(app)
	if not lever.is_empty():result.append(lever)
	for find in locations(app):
		var offer: = offer_for(app, find.id)
		if not offer.is_empty() and offer.distance < 9: result.append(offer)
	return result
static func draw(hud: Control) -> void :
	Mill.draw(hud)
	var app = hud.app
	for find in locations(app):
		if Collection.state(app.save_data, app.selected).claims.get("find:" + find.id, false): continue
		var point: Vector3 = find.point + Vector3.UP * 0.18
		if point.distance_to(app.actor.position) > 10 or app.camera.is_position_behind(point): continue
		var at: Vector2 = app.camera.unproject_position(point)
		if at.y < 100 or at.y > hud.size.y - 125: continue
		var tint: Color = app.stage.actor_scene_color(point)
		hud.draw_rect(Rect2(at - Vector2(10, 6), Vector2(20, 12)), Color("bdab78") * tint, true)
		hud.draw_line(at + Vector2(0, -6), at + Vector2(0, 6), Color("645844") * tint, 1.5, true)
		for side in [-1, 1]: hud.draw_line(at + Vector2(side * 3, -2), at + Vector2(side * 8, -2), Color("756649") * tint, 1, true)
