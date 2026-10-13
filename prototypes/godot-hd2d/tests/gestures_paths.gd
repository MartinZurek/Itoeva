extends SceneTree

const Catalog = preload("res://scripts/world_catalog.gd")
var checks := 0
var failures := 0
var app: Node3D

func _initialize() -> void:
	call_deferred("run")

func check(ok: bool, label: String) -> void:
	checks += 1
	if not ok:
		failures += 1
		push_error(label)

func frames(count: int) -> void:
	for i in count:
		await physics_frame

func touch(index: int, point: Vector2, pressed: bool, canceled: bool = false) -> void:
	var event := InputEventScreenTouch.new()
	event.index = index
	event.position = point
	event.pressed = pressed
	event.canceled = canceled
	root.push_input(event, true)

func drag(index: int, point: Vector2) -> void:
	var event := InputEventScreenDrag.new()
	event.index = index
	event.position = point
	root.push_input(event, true)

func reset_actor() -> void:
	app.hud.reset_touch()
	app.actor.position = app.stage.safe_position(Catalog.position_for("SPORT", Vector2(0.4, 0.6)), app.actor.collider.shape.radius)
	app.actor.stop_input()
	app.actor.state = "idle"
	app.actor.jump_clock = -1
	await frames(90)

func run() -> void:
	DirAccess.make_dir_recursive_absolute("user://godot-tests")
	app = load("res://scenes/full_world.tscn").instantiate()
	app.save_path = "user://godot-tests/gestures.json"
	root.add_child(app)
	while app.loading_world:
		await process_frame
	await app._mount_world("OUTDOOR", Catalog.position_for("SPORT", Vector2(0.4, 0.6)))
	app._resume()
	for creature in app.actors:
		if creature != app.actor:
			creature.enabled = false
	await reset_actor()
	check(app.hud.actions.size() == 2, "Nur Menue/Karte sind Buttons, kein Aktionsknopfraster")
	var center: Vector2 = app.hud.action_center()
	touch(0, Vector2(118, 600), true)
	drag(0, Vector2(190, 600))
	touch(1, center, true)
	await frames(2)
	check(app.actor.is_on_floor(), "Finger-Down loest noch keinen Sprung aus")
	touch(1, center, false)
	await frames(12)
	check(app.actor.position.y > 0.3 and app.actor.touch_vector.x > 0.7, "Tippen springt bei weiter gehaltenem linken Daumen")
	touch(0, Vector2(190, 600), false)
	check(app.actor.touch_vector == Vector2.ZERO, "Linker Touch-Up stoppt unabhaengig")
	for direction in [-1.0, 1.0]:
		await reset_actor()
		touch(1, center, true)
		drag(1, center + Vector2(direction * 48, 0))
		await frames(2)
		check(app.actor.state == "roll" and signf(app.actor.velocity.x) == direction, "Seitliches Wischen rollt in die gewischte Richtung: " + str(direction))
		touch(1, center, false)
		check(not app.actor.jump_requested, "Roll-Release erzeugt keinen zweiten Sprung")
	await reset_actor()
	touch(1, center, true)
	drag(1, center + Vector2(0, -48))
	await frames(12)
	check(app.actor.state != "roll" and app.actor.jump_clock >= 0 and app.actor.position.y > 0.3, "Wischen nach oben springt statt zu rollen")
	touch(1, center, false)
	await reset_actor()
	touch(1, center, true)
	drag(1, center + Vector2(0, 48))
	await frames(2)
	check(app.actor.state == "sit" and app.actor.touch_vector == Vector2.ZERO, "Wischen nach unten setzt hin statt zu rollen")
	touch(1, center, false)
	await reset_actor()
	touch(1, center, true)
	await frames(40)
	check(app.actor.state == "sit" and app.hud.action_fired, "Halten setzt nach 500 ms genau einmal hin")
	await frames(40)
	check(app.actor.state == "sit", "Weiteres Halten toggelt den Sitz nicht zurueck")
	touch(1, center, false)
	check(not app.actor.jump_requested, "Halten-Release erzeugt keinen Sprung")
	await reset_actor()
	touch(1, center, true)
	touch(1, center, false, true)
	await frames(15)
	check(app.actor.is_on_floor() and app.actor.jump_clock < 0, "Touch-Cancel springt nicht")
	touch(1, center, true)
	touch(2, center, true)
	touch(2, center, false)
	check(app.hud.action_finger == 1 and not app.actor.jump_requested, "Dritter Finger uebernimmt das belegte Pad nicht")
	app.show_menu()
	check(app.hud.action_finger == -1 and app.hud.finger == -1, "Menue verwirft beide gehaltenen Daumen")
	app._resume()
	await frames(40)
	check(app.actor.state == "idle", "Fortsetzen erzeugt keine alte Halteaktion")
	# Bildflaechen: unabhängige native Koordinaten statt abgeleiteter Selbstvergleiche.
	for location in [["POND", .59, 411, true], ["POND", .59, 440, false], ["POND", .63, 470, false],
		["COAST_PATH", .32, 401, true], ["COAST_PATH", .32, 435, false],
		["COAST_PATH", .43, 377, true], ["COAST_PATH", .43, 406, false], ["COAST_PATH", .50, 435, false]]:
		var region := Catalog.region_for(location[0])
		var point := Vector3(float(region.origin) + float(location[1]) * 30, 0.03, Catalog.image_z(float(location[2])))
		check(app.stage.is_walkable(point) == location[3], "Planke/Pfad statt Wasser/Bruestung: " + str(location))
	for place in ["POND", "COAST_PATH"]:
		var region := Catalog.region_for(place)
		for u in [.35, .43, .59, .63]:
			var band := Catalog.band(region, u, true)
			var point: Vector3 = app.stage._point(region, Vector2(u, (band.x + band.y) * .5 / 640), true)
			check(is_zero_approx(point.y), "Brueckenzeichnung liegt auf der wirklichen Bodenebene: %s %s" % [place, u])
	check(app.stage.find_child("BegehbareWegkanten", true, false) == null, "Keine goldene Liniengeometrie mehr")
	for species in Catalog.data().characters:
		app.selected = species
		await app._mount_world("OUTDOOR", Catalog.position_for("PARK", Vector2(.5, .6)))
		app._resume()
		await frames(10)
		var outside: float = app.actor.base_pixel_size * 109 * Catalog.relative_height(species) * 720 / app.stage.default_zoom
		check(app.actor.sprite.modulate == Color.WHITE, "Originalfarben aussen: " + species)
		app.toggle_dusk()
		check(app.actor.sprite.modulate == Color.WHITE, "Abendlicht fuegt dem Wesen keine andere Palette hinzu: " + species)
		for room in Catalog.data().interiors:
			await app._mount_world(room, Catalog.position_for(room, Vector2(.53, .7)))
			app._resume()
			var inside: float = app.actor.base_pixel_size * 109 * Catalog.relative_height(species) * 720 / app.stage.default_zoom
			check(absf(outside - inside) < .1 and app.actor.sprite.modulate == Color.WHITE, "Gleiche Referenzgroesse und Palette: %s/%s" % [species, room])
	app.selected = "fennec"
	await app._mount_world("OUTDOOR", Catalog.position_for("PARK", Vector2(.5, .6)))
	app._resume()
	var resident: CharacterBody3D
	for creature in app.actors:
		if creature.species == "hootlet": resident = creature
		if creature != app.actor: creature.enabled = false
	app.actor.position = app.stage.safe_position(resident.position + Vector3.RIGHT * .9, app.actor.collider.shape.radius)
	await frames(15)
	app._update_camera(10)
	app.hud._update_markers()
	var bubble: Dictionary
	for marker in app.hud.markers:
		if marker.kind == "talk" and marker.id == "hootlet": bubble = marker
	check(not bubble.is_empty() and bubble.ready, "Ansprechbares Wesen hat eine erreichbare Sprechblase")
	if not bubble.is_empty():
		touch(3, bubble.screen, true)
		await frames(3)
		check(app.dialog_open and app.paused and app.ui.find_children("*", "Label", true, false).any(func(label): return label.text == Catalog.data().characters.hootlet.tagline), "Sprechblase loest das genau angezeigte Gespraech aus")
		touch(3, bubble.screen, false)
	check(not app.hud.route_markers.is_empty(), "Aussenwelt zeigt dezente Weiterweg-Pfeile")
	await app._mount_world("LIVING", Catalog.position_for("LIVING", Vector2(.5, .7)))
	app._resume()
	var door: Dictionary = app.stage.doors[0]
	app.actor.position = app.stage.safe_position(door.position, app.actor.collider.shape.radius)
	await frames(15)
	app._update_camera(10)
	app.hud._update_markers()
	var entry: Dictionary
	for marker in app.hud.markers:
		if marker.kind == "door" and marker.id == door.from + ":" + door.to: entry = marker
	check(not entry.is_empty() and entry.ready, "Erreichbare Tuer besitzt einen antippbaren Pfeil")
	if not entry.is_empty():
		touch(4, entry.screen, true)
		await frames(15)
		check(app.current_place() == door.to, "Tuerpfeil loest den korrekten Ortswechsel aus")
		if is_instance_valid(app.hud): touch(4, entry.screen, false)
	DirAccess.remove_absolute(app.save_path)
	print("Godot gestures and natural paths: %s Checks, %s Fehler" % [checks, failures])
	quit(1 if failures else 0)
