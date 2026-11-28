extends SceneTree

const Music = preload("res://scripts/music_selection.gd")
const WorldMusic = preload("res://scripts/world_music.gd")
const Clock = preload("res://scripts/world_clock.gd")
const Catalog = preload("res://scripts/world_catalog.gd")
const Atlas = preload("res://scripts/world_atlas_layout.gd")
const Forest = preload("res://scripts/forest_catalog.gd")
const Residents = preload("res://scripts/world_residents.gd")
const Presence = preload("res://scripts/resident_presence.gd")
const Travel = preload("res://scripts/travel_card.gd")

var checks := 0
var errors := 0

func _initialize() -> void:
	run.call_deferred()

func check(value: bool, message: String) -> void:
	checks += 1
	if value: return
	errors += 1
	push_error(message)

func _check_catalog() -> void:
	for id: String in Residents.data():
		var data: Dictionary = Residents.data()[id]
		var portrait := Residents.portrait(id)
		check(portrait.get_width() > 0 and portrait.get_height() > 0, "Porträt fehlt: " + id)
		check(not Residents.help_for(id).is_empty() and data.schedule.size() >= 4, "Figurenaufgabe fehlt: " + id)
		var previous := -1
		for entry: Dictionary in data.schedule:
			check(int(entry.hour) > previous and not Presence.mode(entry).is_empty()
				and (Catalog.data().labels.has(entry.place) or Forest.data().segments.any(func(segment): return segment.id == entry.place)), "Gültiger, benannter Tagespunkt: " + id)
			previous = int(entry.hour)
	var seen := {}
	for view in ["world", "rooms"]:
		for entry in Atlas.nodes(view): seen[entry.id] = true
	for place: String in seen:
		var role := Music.world_role(place)
		check(role != "day" and Music.data().roles.get(role, []).size() >= 2, "Ortsmusik fehlt: " + place)
	for row: Dictionary in Music.data().tracks.values():
		check(load(row.asset) is AudioStreamOggVorbis, "Titel lädt: " + row.title)
	check(Clock.phase_at(4.0) == "night" and Clock.phase_at(6.5) == "dawn" and Clock.phase_at(13.0) == "day"
		and Clock.phase_at(19.5) == "dusk", "Die Uhr unterscheidet vier Phasen")
	check(Music.tempo_for(12.0) > Music.tempo_for(19.0) and Music.tempo_for(19.0) > Music.tempo_for(23.0)
		and Music.tempo_for(7.0) > Music.tempo_for(5.0), "Tempo folgt der Tageszeit")
	var selector := Music.new()
	var coast := selector.pick_world("coast", "dawn", "", true)
	var early := selector.pick_world("coast", "dawn", coast, false)
	check(coast.ends_with("beach-01.ogg") and early.ends_with("morning-01.ogg"), "Küstenstück und Morgenstück wechseln sich ab")
	check(selector.pick_world("coast", "dawn", early, false).ends_with("fishing-01.ogg"), "Ortsstücke rotieren mit")

func _check_audio_mix() -> void:
	var first := AudioStreamPlayer.new()
	var second := AudioStreamPlayer.new()
	root.add_child(first)
	root.add_child(second)
	var world := WorldMusic.new()
	world.configure(first, second)
	world.update(0.1, "coast", "day", 12.0, 0.4, true)
	check(first.stream != null and world.tracks[world.active].ends_with("beach-01.ogg"), "Erster Titel startet")
	world.update(0.3, "town", "day", 12.0, 0.4, true)
	check(world.active == 0 and second.stream == null, "Kurzer Ortswechsel wird gepuffert")
	world.update(0.75, "town", "day", 12.0, 0.4, true)
	check(world.active == 0 and world.queued.begins_with("region:"), "Die Ankunft wartet auf einen musikalischen Ausstieg")
	world.update(4.0, "", "day", 12.0, 0.4, true)
	check(world.active == 0 and world.queued.is_empty(), "Umkehren an der Ortsgrenze verwirft den Wechsel")
	world.update(1.0, "town", "day", 12.0, 0.4, true)
	world.cue_wait = 0.0
	world.update(0.9, "town", "day", 12.0, 0.4, true)
	check(world.active == 1 and first.stream != null and second.stream != null and world.fade < 1.0, "Beide Titel laufen während der Überblendung")
	var before := world.fade
	world.update(0.35, "", "day", 12.0, 0.4, false)
	check(world.fade > before and world.queued.is_empty(), "Ein Ladebild unterbricht die laufende Mischung nicht")
	world.update(3.4, "town", "day", 12.0, 0.4, true)
	check(first.stream == null and world.fade == 1.0 and world.tracks[world.active].ends_with("city-01.ogg"), "Ortswechsel endet sauber")
	world.update(1.0, "town", "night", 23.0, 0.4, true)
	check(world.tempo < 1.0 and world.tempo > Music.tempo_for(23.0), "Nachttempo fährt weich herunter")
	check(is_equal_approx(second.pitch_scale * world.corrections[1].pitch_scale, 1.0), "Getrennter Bus kompensiert die Tonhöhe")
	world.update(14.0, "town", "night", 23.0, 0.4, true)
	world.update(4.0, "town", "night", 23.0, 0.4, true)
	check(world.phase == "night" and world.tracks[world.active].ends_with("dream-01.ogg"), "Nachts kommt das vorhandene Traumstück in die Auswahl")
	var quiet := WorldMusic.next_exit("beach-01", 25.0, 28.8)
	check(quiet > 25.0 and quiet <= 28.8 and quiet in WorldMusic.exit_points["beach-01"], "Der Wechsel verwendet einen geprüften Ausstiegspunkt der Aufnahme")
	check(is_equal_approx(WorldMusic.next_exit("unmarkiert", 25.0, 25.3), 25.3), "Ein freier Ausstieg läuft vor dem Trackende aus")
	var ambient := WorldMusic.blend_levels(0.5, false)
	var groove := WorldMusic.blend_levels(0.5, true)
	check(groove.x < ambient.x and groove.y < ambient.y and groove.length() > 0.55, "Rhythmische Titel mischen sich kürzer und hörbar weich")
	world.update(0.1, "town", "night", 23.0, 0.0, false)
	check(first.stream_paused, "Stummschalten pausiert die Weltmusik")
	first.queue_free()
	second.queue_free()

func _check_travel_cards() -> void:
	for id in ["ALDER_FORD", "WIND_TERRACES", "SIGNAL_WATCH", "MOUNTAIN_PASS"]:
		var card := Travel.new()
		card.place = id
		card.species = Catalog.PROTAGONIST
		card.origin_title = "Erlenfurt"
		card.hour = 6.7
		root.add_child(card)
		var dawn := card.picture_tint()
		check(card.picture != null and card.picture.get_width() >= 480, "Passendes Ladebild fehlt: " + id)
		check(card.route_caption().contains("Morgendämmerung"), "Ladekarte zeigt die Tagesphase")
		card.hour = 19.5
		check(dawn != card.picture_tint() and card.route_caption().contains("Abenddämmerung"), "Getrennte Lichtfarben im Ladebild")
		card.queue_free()

func _check_world() -> void:
	var app: Node3D = load("res://scenes/gameplay.tscn").instantiate()
	root.add_child(app)
	await process_frame
	app.save_path = "user://residents-daylight-%d.json" % OS.get_process_id()
	app.save_data.clear()
	app.music_on = false
	app.world_clock.utc_now_override = Time.get_unix_time_from_datetime_string("2026-11-28T11:10:00")
	await app.visit_test_place("WIND_TERRACES")
	var resident: CharacterBody3D = null
	for creature in app.actors:
		if creature.species == "nella": resident = creature
	check(resident != null and resident.get_meta("activity_mode") == "read" and resident.get_meta("activity_prop") == "paper",
		"Nella steht zur Mittagszeit mit einer Karte an den Windterrassen")
	if resident != null:
		app._show_conversation("nella", resident.get_meta("appointment"))
		var asked := false
		for button in app.menu_buttons.duplicate():
			if button.text == "Hochweg in den Atlas zeichnen":
				button.pressed.emit()
				asked = true
				break
		check(asked and app.atlas_destination == "MOUNTAIN_PASS" and app.atlas_next_place == "SIGNAL_WATCH", "Nella zeichnet den Hochweg ein")
		check("SIGNAL_WATCH" in app.atlas_known_places() and not "SIGNAL_WATCH" in app.discovered_places(),
			"Einwohnerhinweise und echte Besuche bleiben im Atlas unterscheidbar")
		app._load_save()
		check("SIGNAL_WATCH" in app.atlas_known_places(), "Eingezeichnete Orte stehen nach dem Laden im Atlas")
		app._resume()
	app.actor.stamina.value = 50.0
	app._resident_help("puffling", Residents.help_for("puffling"))
	check(app.actor.stamina.value > 50.0, "Pufflings Atemübung hilft beim Ausruhen")
	app._resident_help("starlet", Residents.help_for("starlet"))
	check(not app.resident_light.is_empty(), "Starlets Weglicht erscheint in der Welt")
	app.world_clock.advance_hours(4.0)
	await app.visit_test_place("SIGNAL_WATCH")
	check(app.actors.any(func(creature): return creature.species == "starlet" and creature.get_meta("activity_mode") == "observe"),
		"Starlet prüft am Nachmittag das Signallicht")
	check(app.resident_light.is_empty(), "Das Weglicht bleibt an seinem Ort")
	check(app.music_place() == "SIGNAL_WATCH", "Eine Ankunft am Durchgang gibt den neuen Ort frei")
	await app.visit_test_place("PARK")
	var region: Dictionary = Catalog.region_for("PARK")
	var border: float = float(region.origin) + float(region.width) / region.places.size() * (region.places.find("PARK") + 1)
	for offset in [-0.45, 0.45, 1.8]:
		var point := Vector3(border + offset, 0.03, app.actor.position.z)
		app.actor.position = app.stage.safe_position(point, app.actor.collider.shape.radius)
		app.actor.velocity = Vector3.ZERO
		for frame in 7: await physics_frame
		if absf(offset) < 1.0:
			check(app.music_place().is_empty(), "Im Saum wird kein neuer Titel ausgelöst")
		else:
			check(app.music_place() == app.current_place() and not app.music_place().is_empty(), "Hinter dem Saum gilt der nächste Ort")
	DirAccess.remove_absolute(app.save_path)
	app.queue_free()
	await process_frame

func run() -> void:
	root.size = Vector2i(1280, 720)
	_check_catalog()
	_check_audio_mix()
	_check_travel_cards()
	await _check_world()
	print("Bewohner, Tageslicht und Reisemusik · %d Prüfungen, %d Fehler" % [checks, errors])
	quit(1 if errors else 0)
