extends RefCounted
const Catalog = preload("res://scripts/world_catalog.gd")
const Forest = preload("res://scripts/forest_catalog.gd")
const Journey = preload("res://scripts/journey.gd")
const MomentArt = preload("res://scripts/world_moment_art.gd")

const RIBBON_NOTES: = {
	"mosslyn": "Unter dem Farn ist das Band trocken geblieben. Im Gewebe steckt heller Staub.", 
	"nocti": "Im Schatten glitzert feiner Staub an der eingerissenen Kante.", 
	"starlet": "Dein Licht streift das Band. Heller Staub zeichnet sich auf dem Gewebe ab.", 
	"wyrmling": "Zwischen den Fasern sitzt feiner, heller Staub.", 
}

static func ribbon_note(species: String) -> String:
	return str(RIBBON_NOTES.get(species, "Am Botenband haftet heller Staub.")) + " Er sieht aus wie der Staub am alten Weglicht. Die zweite Spur liegt am Schattentor."

static func record(save: Dictionary, species: String) -> Dictionary:
	return save.get("story_events", {}).get(species, {})

static func mark(save: Dictionary, species: String, key: String) -> bool:
	if record(save, species).get(key, false): return false
	if not save.has("story_events"): save.story_events = {}
	if not save.story_events.has(species): save.story_events[species] = {}
	save.story_events[species][key] = true
	return true

static func gate_hint(save: Dictionary, species: String) -> String:
	if not Journey.won(save, species, "vharok"): return "Grenztor · Vharok bewacht den Durchgang"
	var events: = record(save, species)
	if not events.get("clue_leaf", false) or not events.get("clue_seal", false):
		return "Grenztor · Vergleiche die beiden Spuren im grünen Wald und am Schattentor"
	return ""

static func tell(app, resident: String) -> String:
	mark(app.save_data, app.selected, "heard")
	app._write_save()
	if record(app.save_data, app.selected).get("beacon", false):
		return "Das Weglicht am Waldsaum brennt wieder. Sein Zeichen passt zu den alten Grenzsiegeln. Von der Nebelwacht führt der Versorgungsweg zum Siegelhof."
	var lead: String = "Auf meinem Botenweg war das Weglicht dunkel." if resident == "fennec" else "Fennecs Nachricht sagt, dass das Weglicht erloschen ist."
	return lead + " Geh vom Park über die Wiese zum Waldrand und nimm den Waldpfad. Im grünen Wald liegt ein eingerissenes Botenband, am Schattentor ein Siegelrest. Der Rückweg führt über denselben Waldpfad zum Dorf."

static func reply(app) -> String:
	var events: = record(app.save_data, app.selected)
	if events.get("relay", false): return "Das Relais antwortet wieder. Seine Zeichen gehören zu den alten Weglichtern. Wir können die Grenze nun gemeinsam besser verstehen."
	if Journey.beacon_restored(app.save_data, app.selected):
		return "Seit das Weglicht wieder brennt, ist der Waldsaum leichter zu erkennen. Die kleine Weglaterne in der Stube erinnert an deine Rückkehr."
	return ""

static func locations(app) -> Array[Dictionary]:
	var points: Array[Dictionary] = []
	if app.room == "OUTDOOR":
		points.append({"id": "heard", "point": Catalog.position_for("PARK", Vector2(0.44, 0.62)), "label": "Botenbrief am Wegstein lesen", "prop": "letter"})
	elif app.room == "FOREST_TRAIL":
		points.append({"id": "clue_leaf", "point": Forest.position_for(1, 0.38, 0.58), "label": "Eingerissenes Botenband untersuchen", "prop": "ribbon"})
		points.append({"id": "clue_seal", "point": Forest.position_for(3, 0.52, 0.6), "label": "Siegelrest am Grenzstein prüfen", "prop": "seal"})
		points.append({"id": "beacon", "point": Forest.position_for(0, 0.48, 0.55), "label": "Weglicht wieder entzünden", "prop": "beacon"})
	elif app.room == "RELAY_COURT":
		points.append({"id": "relay", "point": Catalog.position_for("RELAY_COURT", Vector2(0.68, 0.4)), "label": "Relais mit dem Siegelrest abstimmen", "prop": "relay"})
	return points

static func offer(app) -> Dictionary:
	var events: = record(app.save_data, app.selected)
	for entry in locations(app):
		if events.get(entry.id, false): continue
		if entry.id == "beacon" and not Journey.won(app.save_data, app.selected, ""): continue
		if entry.id == "relay" and not Journey.won(app.save_data, app.selected, "relay_warden"): continue
		var point: Vector3 = app.stage.safe_position(entry.point, app.actor.collider.shape.radius)
		var distance: float = app.actor.position.distance_to(point)
		if distance > 9: continue
		return {"kind": "story", "id": entry.id, "position": point + Vector3.UP * 0.45, "approach": point, "label": entry.label, "distance": distance, "ready": app.actor.is_on_floor() and not app.actor.mounted and distance < 1.4}
	return {}

static func perform(app, target: Dictionary) -> void :
	var current: = offer(app)
	if current.is_empty() or current.id != target.id or not current.ready: return
	if not mark(app.save_data, app.selected, target.id): return
	var lines: = {
		"heard": "Das Weglicht ist erloschen. Geh über die Wiese zum Waldrand und folge dem Waldpfad. Such zuerst das Botenband im grünen Wald. Der Rückweg führt über den Waldsaum. – Fennec", 
		"clue_leaf": ribbon_note(app.selected), 
		"clue_seal": "Der Siegelrest trägt drei ineinanderliegende Ringe. Das Zeichen stammt von der alten Grenzwacht, nicht von Nachtwurz.", 
		"beacon": "Das Weglicht brennt wieder. Sein warmer Schein bleibt am Waldsaum und erinnert an deine Hilfe.", 
		"relay": "Die drei Ringe antworten auf den Siegelrest. Das Relais hält die Weglichter verbunden. Hinter der Grenze steckt mehr als ein feindliches Revier."}
	var titles: = {"heard": "Fennecs Botenbrief", "clue_leaf": "Das Botenband", "clue_seal": "Der Siegelrest", "beacon": "Ein Licht am Waldsaum", "relay": "Die Ringe antworten"}
	app._write_save()
	app._notice(titles[target.id])
	app._show_activity(titles[target.id], lines[target.id], "Im Reisetagebuch nachsehen", app.show_journal.bind(true))
	app.context_frame = -1

static func home_lantern(app) -> Dictionary:
	if app.room != "LIVING" or not Journey.beacon_restored(app.save_data, app.selected): return {}
	for item in app.stage.furniture:
		if item.id == "living-tea-table":
			return {"point": item.world_center + Vector3( - 0.34, 0.035, 0)}
	return {}

static func draw_locations(app) -> Array[Dictionary]:

	if app.stage.has_meta("story_draw_points"):
		return app.stage.get_meta("story_draw_points")
	var entries: = locations(app)
	for entry in entries:
		entry.draw_point = app.stage.safe_position(entry.point, 0.1) + Vector3.UP * 0.22
	app.stage.set_meta("story_draw_points", entries)
	return entries

static func draw(hud: Control) -> void :
	var app = hud.app
	var lamp: = home_lantern(app)
	if not lamp.is_empty(): MomentArt.draw(hud, lamp.point, "route_lantern", true, app.game_clock)
	var events: = record(app.save_data, app.selected)
	for entry in draw_locations(app):
		var at_point: Vector3 = entry.draw_point
		if at_point.distance_to(app.actor.position) > 12 or app.camera.is_position_behind(at_point): continue
		var at: Vector2 = app.camera.unproject_position(at_point)
		var scale: float = app.camera.unproject_position(at_point + app.camera.global_basis.x * 0.24).distance_to(at)
		var done: bool = events.get(entry.id, false)
		if entry.prop == "beacon":
			hud.draw_line(at + Vector2(0, scale), at - Vector2(0, scale * 2), Color("745b40"), 3, true)
			hud.draw_rect(Rect2(at + Vector2( - scale * 0.55, - scale * 2.4), Vector2(scale * 1.1, scale)), Color("b99a68"), false, 2)
			if done:
				hud.draw_circle(at - Vector2(0, scale * 1.9), scale * 1.5, Color(MomentArt.WAYLIGHT_AMBER, 0.12))
				hud.draw_circle(at - Vector2(0, scale * 1.9), scale * 0.34, MomentArt.WAYLIGHT_AMBER)
		elif entry.prop == "relay" and done:
			var region: = {"asset": Catalog.data().interiors[app.room].asset, "origin": - app.stage.width * 0.5, "width": app.stage.width, "room_band": Catalog.data().interiors[app.room].band}
			for uv in [Vector2(0.515, 0.416), Vector2(0.594, 0.238), Vector2(0.675, 0.416)]:
				var point: Vector3 = app.stage._point(region, uv, false)
				var center: Vector2 = app.camera.unproject_position(point)
				var edge: Vector2 = app.camera.unproject_position(app.stage._point(region, uv + Vector2(0.03, 0), false))
				var radius: = center.distance_to(edge)
				hud.draw_circle(center, radius, Color(0.35, 0.8, 0.9, 0.04))
				hud.draw_circle(center, radius * 0.65, Color(0.4, 0.85, 1, 0.08))
				hud.draw_circle(center, radius * 0.25, Color(0.6, 0.95, 1, 0.2))
		elif not done and entry.prop != "relay":
			var color: = Color("e1d7ad") if entry.prop == "letter" else Color("accc97") if entry.prop == "ribbon" else Color("82b8bb")
			hud.draw_colored_polygon(PackedVector2Array([at + Vector2( - scale, - scale * 0.5), at + Vector2(scale * 0.7, - scale * 0.7), at + Vector2(scale, scale * 0.5), at + Vector2( - scale * 0.7, scale * 0.7)]), color)
