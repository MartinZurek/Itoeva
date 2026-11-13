extends RefCounted
const Catalog = preload("res://scripts/world_catalog.gd")
const Forest = preload("res://scripts/forest_catalog.gd")
var active: Dictionary = {}
var cooldowns: Dictionary = {}
func offer(app) -> Dictionary:
	var species: String = app.selected
	if app.game_clock < float(cooldowns.get(species, 0.0)): return {}
	var point: = Vector3.ZERO
	var kind: = ""; var label: = ""
	var place: String = app.current_place()
	if species == "mosslyn" and app.room == "FOREST_TRAIL" and place == "SHADOW_BROOK":
		point = Forest.position_for(4, 0.12, 0.5);kind = "tracks";label = "Spuren betrachten"
	elif species == "nocti" and ((app.room == "FOREST_TRAIL" and place == "FOREST_ENTRANCE") or (app.room == "OUTDOOR" and place == "FOREST")):
		point = Forest.position_for(0, 0.55, 0.5) if app.room == "FOREST_TRAIL" else Catalog.position_for("FOREST", Vector2(0.65, 0.6));kind = "wind";label = "Dem Wind lauschen"
	elif species == "starlet" and ((app.room == "FOREST_TRAIL" and place in ["GREENWOOD", "SHADOW_GATE"]) or (app.room == "OUTDOOR" and place == "PARK")):
		point = Forest.position_for(1 if place == "GREENWOOD" else 3, 0.55, 0.4) if app.room == "FOREST_TRAIL" else Catalog.position_for("PARK", Vector2(0.42, 0.45));kind = "light";label = "Licht teilen"
	elif species == "wyrmling":
		var nearest: = 8.0
		for fire in app.stage.local_light_sources:
			var gap: float = app.actor.position.distance_to(fire.position)
			if fire.get("fire", false) and gap < nearest: nearest = gap;point = fire.position;kind = "fire";label = "Feuergruß"
	if kind.is_empty(): return {}
	var approach: Vector3 = app.stage.safe_position(Vector3(point.x, 0.03, point.z + (0.5 if kind != "fire" else 1.2)), app.actor.collider.shape.radius)
	return {"kind": "wonder", "id": species + ":" + kind, "wonder": kind, "position": point + Vector3.UP * 0.65, "approach": approach, "label": label, "distance": app.actor.position.distance_to(approach), "ready": app.actor.is_on_floor() and not app.actor.mounted and app.actor.position.distance_to(approach) < 1.4}
func perform(app, target: Dictionary) -> void :
	var point: Vector3 = target.position - Vector3.UP * 0.65
	active = {"kind": target.wonder, "position": point, "clock": 0.0}
	cooldowns[app.selected] = app.game_clock + 45.0
	app.actor.stop_input()
	if target.wonder == "tracks" and is_instance_valid(app.ground_steps):
		var pool = app.ground_steps
		var second := point + Vector3(0.18, 0, 0.09)
		pool._stamp(point, Vector3.RIGHT, false, 1, 0.5, app.stage.actor_scene_color(point))
		pool._stamp(second, Vector3.RIGHT, false, -1, 0.5, app.stage.actor_scene_color(second))
	elif target.wonder == "fire": app.actor.request_action("jump")
	elif target.wonder == "wind": app.actor.facing = Vector2.UP
	app.stage.set_interaction_focus(point + Vector3.UP * 0.35, 1.4 if target.wonder == "light" else 0.7)
	app._notice({"tracks": "Mooslyn entdeckt zwei kleine Spuren am feuchten Ufer.", "wind": "Nocti hält kurz inne und lauscht dem Wind.", "light": "Starlet teilt einen warmen Lichtschimmer mit dem Wegesrand.", "fire": "Wyrmling begrüßt die Feuerstelle mit einem kleinen Flügelsprung."}[target.wonder])
func advance(delta: float, app) -> void :
	if active.is_empty(): return
	active.clock += delta
	if float(active.clock) >= 1.35:
		active.clear()
		if app.pending_interaction.is_empty() and is_instance_valid(app.stage): app.stage.set_interaction_focus(Vector3.ZERO, 0)
func draw(hud: Control) -> void :
	if active.is_empty(): return
	var point: Vector3 = active.position + Vector3.UP * 0.45
	if hud.app.camera.is_position_behind(point): return
	var at: Vector2 = hud.app.camera.unproject_position(point)
	var t: float = active.clock / 1.35
	var color: = Color("bee1e2") if active.kind == "wind" else Color("ffd695")
	color.a = sin(t * PI) * 0.65
	if active.kind == "tracks": return
	for i in 3:
		var origin: = at + Vector2((i - 1) * 15, - t * 18 - i * 6)
		if active.kind == "wind":
			var line: = PackedVector2Array()
			for j in 9: line.append(origin + Vector2(j * 3.0, sin(j * 0.45 + t * 3) * 3))
			hud.draw_polyline(line, color, 1.3, true)
		else:
			hud.draw_line(origin - Vector2(3, 0), origin + Vector2(3, 0), color, 1.3, true)
			hud.draw_line(origin - Vector2(0, 3), origin + Vector2(0, 3), color, 1.3, true)
