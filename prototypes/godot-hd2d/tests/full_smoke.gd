extends SceneTree

const Catalog = preload("res://scripts/world_catalog.gd")
var failures := 0
var checks := 0

func _initialize() -> void:
	call_deferred("run")

func check(condition: bool, message: String) -> void:
	checks += 1
	if condition:
		print("PASS: ", message)
	else:
		failures += 1
		push_error(message)

func frames(count: int) -> void:
	for i in count:
		await physics_frame

func run() -> void:
	var app: Node3D = load("res://scenes/full_world.tscn").instantiate()
	DirAccess.make_dir_recursive_absolute("user://godot-tests")
	app.save_path = "user://godot-tests/full_smoke.json"
	DirAccess.remove_absolute(app.save_path)
	root.add_child(app)
	await frames(5)
	check(app.paused and app.portrait_cards.size() == 6, "Startmenue zeigt alle sechs Originalwesen")
	var labels: Dictionary = Catalog.data().labels
	check(labels.size() == 28 and Catalog.data().regions.size() == 7, "Vollstaendiger Katalog: 28 Orte und sieben Aussenpanoramen")
	check(app.music_on == false and not app.music.playing, "Musik bleibt beim ersten Start aus")
	for i in range(600):
		if not app.loading_world:
			break
		await frames(1)
	check(not app.loading_world and app.ready_count == 30, "Alle Welt- und Figurenbilder sind vor dem Spielen vorbereitet")
	app.selected = "fennec"
	await app.start_game()
	await frames(20)
	check(app.world_ready and app.actor.is_on_floor(), "Spiel startet mit 3D-Bodenkontakt")
	check(app.stage.chunks.size() == 7 and app.actors.size() == 6, "Alle Panoramen und sechs sichtbare Wesen sind integriert")
	check(app.stage.doors.size() == 5, "Aussenwelt bietet Haus, Laden, Arbeit, Cafe und Spielhalle")
	var actor: CharacterBody3D = app.actor
	# Freie Sportflaeche: die Parkbewohner duerfen diesen Tempotest nicht blockieren.
	actor.position = Catalog.position_for("SPORT", Vector2(0.3, 0.60))
	actor.velocity = Vector3.ZERO
	await frames(15)
	actor.touch_vector = Vector2(0.5, 0)
	await frames(45)
	var walk_speed: float = actor.actual_speed
	actor.touch_vector = Vector2(0.8, 0)
	await frames(45)
	var jog_speed: float = actor.actual_speed
	actor.touch_vector = Vector2(1, 0)
	await frames(45)
	var run_speed: float = actor.actual_speed
	check(walk_speed < jog_speed and jog_speed < run_speed and run_speed > 4.0, "Analogtempo steigt stetig von Gehen ueber Joggen zu Rennen")
	check(actor.running and actor.sprite.frame >= 114 and actor.sprite.frame <= 121, "Sprint nutzt den vorhandenen gezeichneten Laufbogen")
	actor.touch_vector = Vector2.ZERO
	await frames(30)
	check(actor.actual_speed < 0.01 and actor.sprite.frame >= 138, "Anhalten endet in der vorhandenen lebendigen Ruhepose")
	actor.request_action("jump")
	await frames(15)
	check(actor.position.y > 0.5 and actor.jump_clock >= 0, "Sprung nutzt echte Physik und gezeichnete Sprungphasen")
	await frames(90)
	check(actor.is_on_floor() and actor.jump_clock < 0, "Sprung landet sicher auf dem Boden")
	actor.request_action("roll")
	await frames(10)
	check(actor.state == "roll" and actor.sprite.frame >= 130 and actor.sprite.frame <= 137, "Rolle ist eine echte Aktion mit dem bestehenden Rollzyklus")
	await frames(40)
	actor.request_action("sit")
	await frames(30)
	check(actor.state == "sit" and actor.sprite.frame == 169, "Sitzen verwendet die korrigierte Original-Sitzpose")
	actor.facing = Vector2.DOWN
	actor._update_frame(false)
	check(actor.sprite.frame == 103, "Front-Sitzen bleibt in der nativen Frontpose")
	actor.facing = Vector2.UP
	actor._update_frame(false)
	check(actor.sprite.frame == 110, "Ruecken-Sitzen bleibt in der nativen Rueckenpose")
	actor.request_action("sit")
	await frames(3)
	# Die zwei Daumen werden durch echte viewport-lokale ScreenTouch-Ereignisse getestet.
	var hud: Control = app.hud
	var touch := InputEventScreenTouch.new()
	touch.index = 0
	touch.pressed = true
	touch.position = Vector2(120, 610)
	root.push_input(touch, true)
	var drag := InputEventScreenDrag.new()
	drag.index = 0
	drag.position = Vector2(190, 610)
	root.push_input(drag, true)
	await frames(20)
	var second := InputEventScreenTouch.new()
	second.index = 1
	second.pressed = true
	second.position = hud.actions[0].button.get_global_rect().get_center()
	root.push_input(second, true)
	await frames(15)
	check(actor.position.y > 0.3 and actor.touch_vector.x > 0.7, "Rechter Daumen springt bei gehaltenem linken Joystick")
	touch.pressed = false
	root.push_input(touch, true)
	second.pressed = false
	root.push_input(second, true)
	await frames(90)
	check(actor.touch_vector == Vector2.ZERO, "Touch-Up stoppt die Bewegung")
	# Profile der trockenen Bruecke: ein Sprint muss ihrer Kruemmung folgen koennen.
	var bridge: Dictionary = Catalog.region_for("COAST_PATH")
	var middle := Catalog.band(bridge, 0.43, true)
	check(middle == Vector2(408, 449), "Bachbruecke verwendet das native Hoehenprofil")
	actor.position = Vector3(30.0 + 0.43 * 30.0, 0.03, Catalog.image_z((middle.x + middle.y) / 2.0))
	actor.velocity = Vector3.ZERO
	await frames(30)
	check(actor.is_on_floor(), "Brueckenmitte ist physisch begehbar")
	actor.touch_vector = Vector2(0, 1)
	await frames(50)
	check(actor.is_on_floor() and actor.position.z < Catalog.image_z(middle.y) - 0.12, "Brueckenrand verhindert Laufen ins Wasser")
	actor.stop_input()
	# Jeder gemalte Aussenort ist auf demselben zusammenhaengenden Boden erreichbar.
	for region in Catalog.data().regions:
		for place in region.places:
			actor.position = app.stage.safe_position(Catalog.position_for(place, Vector2(0.5, 0.65)))
			actor.velocity = Vector3.ZERO
			await frames(10)
			check(actor.is_on_floor() and app.current_place() == place, "3D-Standflaeche und Ortsname: " + place)
	# Beide Richtungen aller elf Tuerpaare, inklusive Innenraumkette.
	for pair in Catalog.data().doors:
		for source in [pair.a, pair.b]:
			var destination: String = pair.b if source == pair.a else pair.a
			var door: Dictionary
			for entry in Catalog.doors_for(source):
				if entry.to == destination:
					door = entry
			var source_room: String = "OUTDOOR" if not Catalog.region_for(source).is_empty() else source
			await app._mount_world(source_room, Catalog.position_for(source, door.at))
			app._resume()
			await frames(12)
			var physical: Dictionary
			for entry in app.stage.doors:
				if entry.to == destination and entry.from == source:
					physical = entry
			app.actor.position = app.stage.safe_position(physical.position, app.actor.collider.shape.radius)
			app.actor.velocity = Vector3.ZERO
			await frames(12)
			app.actor.touch_vector = Vector2(0.9, 0)
			await app.interact()
			await frames(12)
			check(app.current_place() == destination and app.actor.touch_vector == Vector2.ZERO and app.actor.is_on_floor(), "Tuer und sicherer Gegenanker: " + source + " → " + destination)
	for species in Catalog.data().characters:
		app.selected = species
		await app._mount_world("LIVING", Catalog.position_for("LIVING", Vector2(0.78, 0.78)))
		app._resume()
		await frames(15)
		var reference: Dictionary = Catalog.data().characters[species]
		var expected: float = app.stage.base_pixel_size * 109.0 * float(reference.relative_height)
		var visible: float = app.actor.sprite.pixel_size * (126.0 - float(reference.top))
		check(absf(expected - visible) < 0.2 and app.actor.sprite.texture.get_size() == Vector2(1536, 1920), "Native Koerpergroesse und mobiler 170er-Atlas: " + species)
	check(app.stage.furniture.size() == 4, "Wohnzimmer enthaelt Sofa, Teetisch, Beistelltisch und Buchregal")
	var sofa: Dictionary
	for item in app.stage.furniture:
		if item.id == "living-sofa":
			sofa = item
	app.actor.mount_seat(sofa.seat_position, app.actor.position)
	await frames(30)
	check(app.actor.mounted and app.actor.position.distance_to(sofa.seat_position) < 0.01, "Sofa verwendet den gemalten Sitzanker")
	app.actor.touch_vector = Vector2.RIGHT
	await frames(4)
	check(not app.actor.mounted, "Bewegung verlaesst den Sitzplatz ohne festzuhaengen")
	app.actor.stop_input()
	await frames(20)
	app.actor.mount_seat(sofa.seat_position, app.actor.position)
	Input.action_press("sit")
	await frames(2)
	Input.action_release("sit")
	check(not app.actor.mounted and app.actor.state == "idle", "C steht vom Sofa auf, ohne erneut am Boden zu sitzen")
	await frames(20)
	app.actor.mount_seat(sofa.seat_position, app.actor.position)
	Input.action_press("jump")
	await frames(8)
	Input.action_release("jump")
	check(not app.actor.mounted and app.actor.velocity.y > 0, "Leertaste verlaesst den Sitz und springt")
	await frames(100)
	app.actor.mount_seat(sofa.seat_position, app.actor.position)
	Input.action_press("roll")
	await frames(4)
	Input.action_release("roll")
	check(not app.actor.mounted and app.actor.state == "roll", "Rolle verlaesst den Sitz mit genau einer Aktion")
	await frames(45)
	app.sensitivity = 1.3
	app.fixed_stick = true
	app.music_on = true
	app._save_preferences()
	app._save_progress()
	var stored = JSON.parse_string(FileAccess.get_file_as_string(app.save_path))
	check(stored.settings.species == app.selected and stored.settings.fixed_stick and absf(stored.settings.sensitivity - 1.3) < 0.01, "Wesenwahl und Steuerungseinstellungen werden lokal gespeichert")
	check(stored.characters[app.selected].room == "LIVING", "Innenraum und Standort werden je Wesen gespeichert")
	app.show_menu()
	await frames(3)
	check(app.paused and not app.music.playing and app.actor.touch_vector == Vector2.ZERO, "Menue pausiert Welt, Ton und gehaltene Eingaben")
	# Auswahl per ScreenTouch funktioniert auch ohne Maus-Emulation.
	var choice: Dictionary = app.portrait_cards[0]
	var menu_touch := InputEventScreenTouch.new()
	menu_touch.index = 2
	menu_touch.pressed = true
	menu_touch.position = choice.button.get_global_rect().get_center()
	root.push_input(menu_touch, true)
	await frames(2)
	check(app.selected == choice.species, "Wesenwahl verarbeitet echten ScreenTouch")
	DirAccess.remove_absolute(app.save_path)
	DirAccess.remove_absolute(app.save_path + ".tmp")
	print("Godot full smoke: %s Checks, %s Fehler" % [checks, failures])
	quit(1 if failures else 0)
