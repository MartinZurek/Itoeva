extends SceneTree

const Catalog = preload("res://scripts/world_catalog.gd")
var app: Node3D
var checks := 0
var failures := 0

func _initialize() -> void:
	call_deferred("run")

func check(ok: bool, label: String) -> void:
	checks += 1
	if not ok:
		failures += 1
		push_error(label)

func frames(count: int) -> void:
	for i in count: await physics_frame

func touch(index: int, point: Vector2, pressed: bool) -> void:
	var event := InputEventScreenTouch.new()
	event.index = index
	event.position = point
	event.pressed = pressed
	root.push_input(event, true)

func run() -> void:
	DirAccess.make_dir_recursive_absolute("user://godot-tests")
	app = load("res://scenes/full_world.tscn").instantiate()
	app.save_path = "user://godot-tests/proportions.json"
	root.add_child(app)
	while app.loading_world: await process_frame
	check(app.portrait_cards.size() == 6, "Alle sechs Figuren bleiben im Startmenue")
	await frames(90)
	for card in app.portrait_cards:
		check(card.atlas.region == Rect2(1280, 1408, 128, 128), "Menuegesicht bleibt stabil: " + card.species)
	await app._mount_world("OUTDOOR", Catalog.position_for("PARK", Vector2(.5, .6)))
	app._resume()
	await frames(20)
	for creature in app.actors:
		creature.enabled = false
		creature.jump_clock = -1
		creature.recovery_clock = 1
		var canonical_height: float = creature.base_pixel_size * 109.0 * Catalog.relative_height(creature.species)
		for view in [[Vector2.RIGHT, 142, 93, 9], [Vector2.LEFT, 142, 93, 9], [Vector2(0, 1), 148, 103, 68], [Vector2(0, -1), 149, 110, 76]]:
			creature.facing = view[0]
			creature.state = "idle"
			for time in [0.0, .1, .8, 1.9, 3.5]:
				creature.clock = time
				creature.turn_clock = time
				creature.transition_clock = time
				creature._update_frame(false)
				check(creature.sprite.frame == view[1], "Kein Gesichts-/Ansichtswechsel in Ruhe: %s/%s/%s" % [creature.species, view[0], time])
				check(absf(creature.frame_bounds[creature.sprite.frame].size.y * creature.sprite.pixel_size - canonical_height) < .01, "Gleiche sichtbare Ruhehoehe je Ansicht: %s/%s" % [creature.species, view[0]])
			creature.state = "sit"
			creature.state_clock = 1
			creature._update_frame(false)
			check(creature.sprite.frame == view[2], "Echte gerichtete Sitzpose statt Schlafbild: %s/%s" % [creature.species, view[0]])
			creature.state = "idle"
			creature.running = false
			for phase in range(8):
				creature.gait = (phase + .1) / 8.0
				creature._update_frame(true)
				check(creature.sprite.frame == view[3] + phase, "Alle acht Gangphasen erhalten: %s/%s/%s" % [creature.species, view[0], phase])
			# Ein gemeinsamer Faktor pro Folge erhaelt gezeichnete Schrittbewegung.
			var clip_scale: float = creature.sprite.pixel_size
			for frame in range(view[3], view[3] + 8):
				creature.sprite.frame = frame
				creature._update_sprite_scale()
				check(is_equal_approx(creature.sprite.pixel_size, clip_scale), "Kein Pumpen innerhalb einer Gangfolge: %s/%s" % [creature.species, frame])
		check(creature.collision_layer == 2, "Figur ist keine Schatten-Standflaeche: " + creature.species)
	var a: CharacterBody3D = app.actors[0]
	var b: CharacterBody3D = app.actors[1]
	a.position.z = -.5
	b.position.z = .5
	app._sort_actors()
	check(b.sprite.render_priority > a.sprite.render_priority, "Vordere Figur wird ueber hinterer gezeichnet")
	a.position.y = 3
	app._sort_actors()
	check(b.sprite.render_priority > a.sprite.render_priority, "Sprunghoehe vertauscht keine Tiefe")
	a.position.z = .7
	app._sort_actors()
	check(a.sprite.render_priority > b.sprite.render_priority, "Reihenfolge folgt dem wirklichen Seitenwechsel")
	app.selected = "fennec"
	await app._mount_world("OUTDOOR", Catalog.position_for("PARK", Vector2(.5, .6)))
	app._resume()
	for creature in app.actors:
		if creature != app.actor: creature.enabled = false
	await frames(20)
	check(app.hud.ACTION_RADIUS >= app.hud.RADIUS, "Rechter Aktionskreis mindestens so gross wie links")
	check(app.hud.action_center().y + app.hud.ACTION_RADIUS + 20 < app.hud.size.y, "Sitzbeschriftung bleibt im Bild")
	var event := InputEventScreenDrag.new()
	touch(0, Vector2(118, 600), true)
	event.index = 0
	event.position = Vector2(166, 600)
	root.push_input(event, true)
	touch(1, app.hud.action_center(), true)
	await frames(45)
	check(app.actor.state == "sit" and app.actor.touch_vector == Vector2.ZERO, "Halten bleibt sitzen trotz bisher gehaltenem Gehdaumen")
	touch(1, app.hud.action_center(), false)
	touch(0, Vector2(166, 600), false)
	app.actor.state = "idle"
	var resident: CharacterBody3D
	for creature in app.actors:
		if creature.species == "hootlet": resident = creature
	app.actor.position = app.stage.safe_position(resident.position + Vector3.RIGHT * 2.25, app.actor.collider.shape.radius)
	await frames(15)
	app._update_camera(10)
	app.hud._update_markers()
	var bubble: Dictionary
	for marker in app.hud.markers:
		if marker.kind == "talk" and marker.id == "hootlet": bubble = marker
	check(not bubble.is_empty() and bubble.ready, "Gespraech schon vom freien Weg aus erreichbar")
	if not bubble.is_empty():
		check(bubble.screen.y < bubble.body_rect.position.y - 20, "Blase bleibt ueber dem Kopf statt im Gesicht")
		touch(2, bubble.body_rect.get_center(), true)
		await frames(3)
		check(app.dialog_open and app.paused and app.ui.find_children("*", "Label", true, false).any(func(label): return label.text == Catalog.data().characters.hootlet.tagline), "Tipp auf die Figur spricht den gezeigten Nachbarn an")
		check(resident.state != "sit", "Gespraech erzwingt kein unpassendes Hinsetzen")
		touch(2, bubble.body_rect.get_center(), false)
	var outside_leap: float = app.stage.jump_height * 720.0 / app.stage.default_zoom
	for room in Catalog.data().interiors:
		await app._mount_world(room, Catalog.position_for(room, Vector2(.5, .7)))
		check(absf(app.stage.jump_height * 720.0 / app.stage.default_zoom - outside_leap) < .1, "Kein uebergrosser Sprung im Innenraum: " + room)
		app.yaw = 8
		app._update_camera(10)
		check(is_zero_approx(app.yaw), "Kulissenblick bleibt unverzerrt: " + room)
	check(Catalog.data().labels.size() == 36 and Catalog.data().interiors.size() == 11 and Catalog.data().doors.size() == 11, "28 Orte und alle elf Raum-/Tuerpaare erhalten")
	DirAccess.remove_absolute(app.save_path)
	print("Godot proportions and readability: %s Checks, %s Fehler" % [checks, failures])
	quit(1 if failures else 0)
