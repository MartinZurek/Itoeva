extends SceneTree

const Catalog = preload("res://scripts/world_catalog.gd")
const Enemies = preload("res://scripts/regional_enemies.gd")
const EnemyLife = preload("res://scripts/enemy_life.gd")

var app: Node3D
var checks := 0
var failures := 0

func _initialize() -> void:
	run.call_deferred()

func check(ok: bool, text: String) -> void:
	checks += 1
	if ok: return
	failures += 1
	push_error(text)

func settle() -> void:
	for frame in 8:
		await physics_frame
		await process_frame
	app.hud._update_markers()

func target(id: String) -> Dictionary:
	for entry: Dictionary in app.interaction_targets():
		if entry.id == id: return entry
	return {}

func stand_at(point: Vector3) -> void:
	app.actor.position = point
	app.actor.velocity = Vector3.ZERO
	app._update_camera(10.0)
	await settle()

func walk_door(to: String) -> void:
	var from: String = app.current_place()
	var key := from + ":" + to
	var entry := target(key)
	check(entry.get("kind", "") == "door", "Durchgang vorhanden: " + key)
	if entry.is_empty(): return
	await stand_at(entry.approach)
	await app.interact_with("door", key)
	await settle()
	check(app.current_place() == to, "Durchgang führt tatsächlich nach " + to)

func scene_props() -> void:
	for piece: Dictionary in app.stage.furniture:
		var entry := target(piece.id)
		check(not entry.is_empty() and not entry.label.is_empty() and entry.body_rect.has_area(), "Benannte Oberfläche: " + piece.id)

func run() -> void:
	root.size = Vector2i(1280, 720)
	app = load("res://scenes/gameplay.tscn").instantiate()
	root.add_child(app)
	await process_frame
	app.save_path = "user://wind-terraces-%d.json" % OS.get_process_id()
	app.save_data.clear()
	await app.visit_test_place("PARK")
	await settle()
	scene_props()
	var crate := target("park-crate")
	check(crate.get("label", "") == "Wetterkiste · Untersuchen", "Die Kiste im Park ist untersuchbar")
	if not crate.is_empty():
		await stand_at(crate.approach)
		var hit: bool = app.hud._tap_marker(target("park-crate").body_rect.get_center())
		check(hit and app.notice.contains("Löwenzahn"), "Klick auf die Kiste zeigt ihre Beschreibung")

	for room in ["LIVING", "KITCHEN", "BATH", "NOOK", "BEDROOM", "DESK", "CRAFT", "SHOP", "CAFE", "WORK", "ARCADE"]:
		await app.visit_test_place(room)
		await settle()
		scene_props()
	check(target("arcade-pinball").get("label") == "Flipper · Untersuchen", "Auch Geräte haben eine eigene Beschreibung")

	await app.visit_test_place("ALDER_FORD")
	await settle()
	check(app.atlas_path("ALDER_FORD", "SIGNAL_WATCH") == ["ALDER_FORD", "WIND_TERRACES", "SIGNAL_WATCH"], "Der Atlas führt über den neuen Hochweg")
	await walk_door("WIND_TERRACES")
	await stand_at(Catalog.position_for("WIND_TERRACES", Vector2(0.77, 0.51)))
	var stone := target("windstone")
	check(not stone.is_empty(), "Windstein ist auf seiner Terrasse erreichbar")
	if not stone.is_empty():
		await stand_at(stone.approach)
		check(target("windstone").get("ready", false), "Lesen erfolgt vom Weg aus")
		await app.interact_with("landmark", "windstone")
		check(app.notice.contains("Signalwarte"), "Der Wegstein beschreibt die Reise")
	await walk_door("SIGNAL_WATCH")
	var bell := target("signal-bell")
	check(not bell.is_empty(), "Die Signalglocke hat eine Interaktion")
	if not bell.is_empty():
		await stand_at(bell.approach)
		await app.interact_with("landmark", "signal-bell")
		check(app.notice.contains("tiefer Ton") and app.hud.pickup_flash_clock > 0.0, "Die Glocke löst sichtbares Feedback aus")
		check(target("signal-bell").get("ready", false), "Läuten bleibt möglich")
	await walk_door("MOUNTAIN_PASS")
	await walk_door("SIGNAL_WATCH")
	await walk_door("WIND_TERRACES")
	await walk_door("ALDER_FORD")

	for id in ["cinder_carrier", "vharok", "relay_warden"]:
		var profile: Dictionary = Enemies.data()[id]
		var work := EnemyLife.sample(profile, float(profile.period) * 0.45)
		check(work.working and not work.moving, "Am Stationspunkt wird gearbeitet: " + id)
		check(EnemyLife.sample(profile, float(profile.period) * 0.13).moving, "Es gibt eine Wegphase: " + id)
		check(Enemies.encounter_portrait(id).get_height() >= 400, "Das Gespräch zeigt den neuen Entwurf: " + id)
	await app.visit_test_place("SEAL_COURT")
	await settle()
	var guardian: Dictionary = app.regional_markers[0]
	var profile: Dictionary = Enemies.data().vharok
	var station_uv: Vector2 = EnemyLife.sample(profile, float(profile.period) * 0.45).uv
	guardian.position = app.stage.safe_position(Catalog.position_for("SEAL_COURT", station_uv), 0.1)
	guardian.life_clock = float(profile.period) * 0.45
	app._update_regional_art(0.4)
	check(guardian.work_mix > 0.58 and guardian.pose == 4, "Vharok verwendet am Tor die niedrigere Zeichenhaltung")
	var vharok := target("vharok")
	check(vharok.get("label", "").ends_with(" · Sprechen"), "Vharok lässt sich ansprechen")
	if not vharok.is_empty():
		await stand_at(vharok.approach)
		app.actor.actual_speed = 1.5
		app._check_regional_challenge(guardian, Enemies.data().vharok, 2.0, 2.0)
		check(not app.paused, "Vorbeigehen startet kein Gespräch")
		app.actor.actual_speed = 0.0
		app._check_regional_challenge(guardian, Enemies.data().vharok, 2.0, 1.5)
		check(app.paused and guardian.hailed, "Beim Innehalten spricht Vharok Mooslyn an")
		check(app.menu_buttons.any(func(button): return button.text == "Kartenkampf beginnen"), "Auf die Herausforderung kann ein Kartenkampf folgen")
		check(app.menu_buttons.any(func(button): return button.text == "Weiter erkunden"), "Aus dem Gespräch kann man direkt weiterreisen")
		app._resume()
		app._check_regional_challenge(guardian, Enemies.data().vharok, 2.0, 2.0)
		check(not app.paused, "Nach dem Gespräch spricht Vharok Mooslyn beim selben Besuch nicht erneut an")
	app.queue_free()
	await process_frame
	DirAccess.remove_absolute("user://wind-terraces-%d.json" % OS.get_process_id())
	print("Windterrassen · %d Prüfungen, %d Fehler" % [checks, failures])
	quit(1 if failures else 0)
