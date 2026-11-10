extends RefCounted
const Path = preload("res://scripts/interaction_path.gd")
const Art = preload("res://scripts/world_moment_art.gd")
const Catalog = preload("res://scripts/world_catalog.gd")
const Forest = preload("res://scripts/forest_catalog.gd")


var active: Dictionary = {}
var tick: = 0.0
var elapsed: = 0.0
var checks: = 0
var next_offer: = 0.0

func completed(app) -> Dictionary:
	return app.save_data.get("world_moments", {}).get(app.selected, {})

func reset(app) -> void :
	if not active.is_empty():
		var resident = _resident(app, str(active.get("npc", "")))
		if resident != null:
			resident.npc_vector = Vector2.ZERO
			resident.set_meta("activity_mode", active.get("previous_mode", "walk"))
	active.clear();tick = 0;elapsed = 0;next_offer = 0

func _resident(app, id: String):
	for creature in app.actors:
		if creature != app.actor and is_instance_valid(creature) and creature.species == id: return creature
	return null

func _points(app, origin: Vector3) -> PackedVector3Array:
	var radius: float = app.actor.collider.shape.radius + 0.08
	var away: = 1.0 if origin.x >= app.actor.position.x else -1.0
	for direction in [away, - away]:
		var path: = PackedVector3Array([origin])
		for distance in [1.0, 2.25]:
			var point: Vector3 = app.stage.safe_position(origin + Vector3.RIGHT * distance * direction, radius)
			checks += 1
			if Path.clear_segment(app.stage, path[-1], point, radius) and point.distance_to(path[-1]) > 0.65: path.append(point)
		if path.size() == 3: return path
	return PackedVector3Array()

func advance(delta: float, app) -> void :
	elapsed += delta
	if not active.is_empty():
		active.clock += delta
		if app.room != active.room or app.actor.position.distance_to(active.home) > 18:
			reset(app);return
		if active.phase == "roll" and float(active.clock) > 1.15:
			active.phase = "search";active.clock = 0.0
		elif active.phase == "finish" and float(active.clock) > 18:
			reset(app);next_offer = elapsed + 12
	tick += delta
	if tick < 0.25: return
	tick = fmod(tick, 0.25)
	if not active.is_empty() or elapsed < next_offer: return
	var place: String = app.current_place()
	var kind: String = "market" if app.room == "OUTDOOR" and place == "CITY" else "wind" if place in ["VILLAGE_EDGE", "FOREST", "FOREST_ENTRANCE", "GREENWOOD"] else ""
	if kind.is_empty(): return
	if completed(app).has(kind):
		if place in ["FOREST", "FOREST_ENTRANCE"]: _offer_trail(app)
		return
	for id in (["brumm", "hootlet"] if kind == "market" else ["nocti", "tilla"]):
		var resident = _resident(app, id)
		if resident == null or resident.position.distance_to(app.actor.position) > 7: continue
		var points: = _points(app, resident.position)
		if points.is_empty(): next_offer = elapsed + 8;return
		active = {"kind": kind, "room": app.room, "npc": id, "name": app.Residents.data()[id].name, 
			"previous_mode": resident.get_meta("activity_mode", "walk"), "home": points[0], "points": points, 
			"phase": "carry" if kind == "market" else "listen", "clock": 0.0, "prop": "leaf" if kind == "wind" else "bread" if id == "brumm" else "paper"}
		app.context_frame = -1
		return
	if place in ["FOREST", "FOREST_ENTRANCE"]: _offer_trail(app)

func _offer_trail(app) -> void :
	if completed(app).has("beetle"): return
	var point: Vector3 = Forest.position_for(0, 0.34, 0.58) if app.room == "FOREST_TRAIL" else Catalog.position_for("FOREST", Vector2(0.35, 0.6))
	point = app.stage.safe_position(point, app.actor.collider.shape.radius + 0.08)
	if point.distance_to(app.actor.position) > 7: return
	var points: = _points(app, point)
	if points.is_empty(): next_offer = elapsed + 8;return
	active = {"kind": "beetle", "room": app.room, "npc": "", "name": "Leuchtkäfer", "previous_mode": "", 
		"home": point, "points": points, "phase": "trace", "clock": 0.0, "prop": "beetle", "step": 0}
	app.context_frame = -1

func steer(app, creature, delta: float) -> bool:
	if active.is_empty() or creature.species != active.get("npc", ""): return false
	if app.game_clock < float(creature.get_meta("rest_until", 0.0)):
		creature.npc_vector = Vector2.ZERO;return true
	if creature.get_meta("presence_sit", false):
		creature.state = "idle";creature.state_clock = 0;creature.set_meta("presence_sit", false)
	var target: Vector3 = active.home
	if active.phase == "carry": target = active.points[1]
	elif active.phase in ["roll", "search", "return", "listen"]: target = active.points[1]
	elif active.phase == "follow": target = active.points[2]


	if active.phase == "search" and fposmod(float(active.clock), 12.0) < 6.0: target = active.home
	var offset: Vector3 = target - creature.position;offset.y = 0
	var player_gap: Vector3 = app.actor.position - creature.position;player_gap.y = 0
	if player_gap.length() < 0.65 and player_gap.dot(offset) > 0.05:
		creature.npc_vector = Vector2.ZERO;return true
	if offset.length() > 0.16:
		var right: Vector3 = app.camera.global_basis.x;right.y = 0;right = right.normalized()
		var front: Vector3 = app.camera.global_basis.z;front.y = 0;front = front.normalized()
		creature.npc_vector = Vector2(offset.normalized().dot(right), offset.normalized().dot(front)) * 0.4
		creature.set_meta("activity_mode", "walk")
	else:
		creature.npc_vector = Vector2.ZERO
		creature.set_meta("activity_mode", "work" if active.phase == "finish" and active.kind == "market" else "observe")
		if active.phase == "carry":
			active.phase = "roll";active.clock = 0.0
			app._notice("%s · %s" % [active.name, "Ein Brötchen rollt aus dem Korb." if active.prop == "bread" else "Ein Lesezeichen rutscht davon."])
		var look: Vector3 = app.actor.position - creature.position if active.phase in ["return", "finish"] else active.points[2] - creature.position
		creature.facing = Vector2(signf(look.x), 0) if absf(look.x) > 0.1 else Vector2.DOWN
	return true

func offer(app) -> Dictionary:
	if active.is_empty() or active.phase not in ["search", "return", "listen", "follow", "trace"]: return {}
	var returning: bool = active.phase == "return"
	var resident = _resident(app, active.npc)
	if active.kind != "beetle" and resident == null: return {}
	var point: Vector3 = active.points[int(active.step)] if active.kind == "beetle" else resident.position if returning or active.phase == "listen" else active.points[2]
	var approach: Vector3 = app.stage.safe_position(point + Vector3.LEFT * 0.75 if returning else point, app.actor.collider.shape.radius + 0.03)
	var distance: float = app.actor.position.distance_to(approach)
	var label: String = "Zu %s zurückbringen" % active.name if returning else "Brötchen sichern" if active.prop == "bread" else "Lesezeichen aufheben"
	if active.kind == "wind": label = "Mit %s lauschen" % active.name if active.phase == "listen" else "Blatt am Wegstein lösen"
	if active.kind == "beetle": label = ["Leuchtspur ansehen", "Dem Licht nachgehen", "Ruhig herantreten"][int(active.step)]
	return {"kind": "moment", "id": active.kind + (":" + str(active.step) if active.kind == "beetle" else ":" + active.phase), "position": point + Vector3.UP * 0.5, 
		"approach": approach, "distance": distance, "ready": app.actor.is_on_floor() and not app.actor.mounted and distance < 1.2 and app._interaction_clear(approach), 
		"label": label}

func perform(app, target: Dictionary) -> void :
	var current: = offer(app)
	if current.is_empty() or current.id != target.id or not current.ready: return
	if active.kind == "beetle":
		if app.actor.actual_speed > 2.7:
			app._notice("Ein wenig ruhiger · Der kleine Leuchtkäfer wartet am Weg.");return
		if int(active.step) < 2:
			active.step += 1;active.clock = 0.0
			app._notice("Ein kleiner Lichtpunkt zieht zum nächsten Blatt. Du kannst ihm folgen.")
		else:
			_complete(app);app._notice("Der Leuchtkäfer fasst Zutrauen und begleitet dich ein Stück. Sein Platz bleibt am Waldsaum.")
	elif active.kind == "wind":
		if active.phase == "listen":
			active.phase = "follow";active.clock = 0.0
			app._notice("%s hört das Rascheln am Wegstein und geht ihm nach. Schau dort nach." % active.name)
		else:
			_complete(app);app._notice("Das Blatt ist frei und raschelt wieder im Wind. %s lauscht zufrieden." % active.name)
	elif active.phase == "search":
		active.phase = "return";active.clock = 0.0
		app._notice("Aufgehoben · Bring es zu %s zurück." % active.name)
	else:
		_complete(app)
		app._notice("%s freut sich. %s" % [active.name, "Der Brotkorb steht wieder sicher am Markt." if active.prop == "bread" else "Das Lesezeichen liegt wieder im Buch."])
	app.context_frame = -1

func _complete(app) -> void :
	if not app.save_data.has("world_moments"): app.save_data.world_moments = {}
	if not app.save_data.world_moments.has(app.selected): app.save_data.world_moments[app.selected] = {}
	var result_point: Vector3 = active.points[2] if active.kind == "beetle" else active.home
	app.save_data.world_moments[app.selected][active.kind] = {"room": app.room, "at": [result_point.x, result_point.z], "resident": active.npc, "prop": active.prop}
	app._write_save()
	active.phase = "finish";active.clock = 0.0

	for creature in app.actors:
		if creature == app.actor or creature.species == active.npc or creature.position.distance_to(active.home) > 3: continue
		if app.game_clock < float(creature.get_meta("rest_until", 0.0)): continue
		creature.set_meta("rest_until", app.game_clock + 1.8)
		var gap: Vector3 = active.home - creature.position;creature.facing = Vector2(signf(gap.x), 0)

func reply(app, id: String) -> String:
	for result in completed(app).values():
		if result.get("resident", "") == id: return "Danke für deine Hilfe vorhin. Jetzt kann ich hier in Ruhe weitermachen."
	return ""

func draw(hud: Control) -> void :
	var app = hud.app
	for result in completed(app).values():
		if result.get("prop", "") == "beetle" and active.get("kind", "") == "beetle": continue
		if result.get("room", "") != app.room: continue
		var at: Array = result.get("at", [])
		if at.size() != 2: continue
		var point: = Vector3(float(at[0]), 0.04, float(at[1]))
		Art.draw(hud, point, str(result.get("prop", "bread")), true, elapsed)
	if active.is_empty(): return
	if active.kind == "beetle":
		var step: int = active.step
		var origin: Vector3 = active.points[maxi(0, step - 1)]
		var point: Vector3 = origin.lerp(active.points[step], smoothstep(0, 2, float(active.clock)))
		if active.phase == "finish":
			var companion: Vector3 = app.actor.position + app.camera.global_basis.x * 0.65 + Vector3.UP * 0.6
			point = active.points[2].lerp(companion, smoothstep(0, 2, float(active.clock)))
			point = point.lerp(active.points[2], smoothstep(12, 18, float(active.clock)))
		Art.draw(hud, point, "beetle", true, elapsed)
		return
	if active.phase == "finish": return
	var point: Vector3 = active.points[2]
	var resident = _resident(app, active.npc)
	if active.kind == "wind":
		point = active.points[1].lerp(active.points[2], smoothstep(0, 1.8, float(active.clock))) if active.phase == "follow" else active.points[2]
	elif active.phase == "carry" and resident != null: point = resident.position + Vector3.UP * 0.65
	elif active.phase == "roll":
		var t: = smoothstep(0, 1.15, float(active.clock));point = active.points[1].lerp(active.points[2], t) + Vector3.UP * sin(t * PI) * 0.1
	elif active.phase == "return": point = app.actor.position + Vector3.UP * 0.65 + app.camera.global_basis.x * 0.2
	Art.draw(hud, point, active.prop, false, elapsed)
