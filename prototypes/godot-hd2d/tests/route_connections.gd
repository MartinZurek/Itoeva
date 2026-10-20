extends SceneTree

const Catalog = preload("res://scripts/world_catalog.gd")
const Forest = preload("res://scripts/forest_catalog.gd")
const Navigator = preload("res://scripts/interaction_path.gd")
var checks := 0
var failures := 0

func _initialize() -> void: call_deferred("run")

func check(ok: bool, label: String) -> void:
	checks += 1
	if not ok:
		failures += 1
		push_error(label)

func frames(count: int = 10) -> void:
	for i in count: await physics_frame

func find_door(app: Node3D, destination: String) -> Dictionary:
	for door in app.stage.doors:
		if door.to == destination: return door
	return {}

func run() -> void:
	DirAccess.make_dir_recursive_absolute("user://godot-tests")
	var app: Node3D = load("res://scenes/full_world.tscn").instantiate()
	app.save_path = "user://godot-tests/route-connections.json"
	DirAccess.remove_absolute(app.save_path)
	root.add_child(app)
	while app.loading_world: await process_frame
	await app.start_game()
	var entrance: Dictionary = Forest.doors_for("FOREST")[0]
	check(Catalog.position_for("FOREST", entrance.at).x > Catalog.position_for("FOREST", Vector2(.52,.64)).x + 2.5, "Waldeingang liegt deutlich weiter rechts")
	var links: Array = [{"to":"FOREST", "index":0}]
	links.append_array(Forest.data().branches)
	for species in Catalog.data().characters:
		app.selected = species
		for link in links:
			await app._mount_world("FOREST_TRAIL", Forest.position_for(link.index, .5))
			app.forest_encounter.model.phase = "done"
			app._resume()
			var outward := find_door(app, link.to)
			var radius: float = app.actor.collider.shape.radius
			check(app.stage.is_walkable(outward.position, radius), "Freier Waldabzweig: " + species + "/" + link.to)
			app.actor.position = app.stage.safe_position(outward.position, radius)
			app.actor.velocity = Vector3.ZERO
			await frames()
			await app.interact_with("door", outward.from + ":" + outward.to)
			await frames()
			check(app.current_place() == link.to and app.actor.is_on_floor(), "Hinweg erreicht Ort: " + species + "/" + link.to)
			var inward := find_door(app, "FOREST_TRAIL")
			check(app.actor.position.distance_to(inward.position) < .35, "Ankunft steht am zugehoerigen Rueckweg: " + species + "/" + link.to)
			check(inward.at == outward.arrival and inward.arrival == outward.at, "Ein gemeinsames Endpunktpaar: " + species + "/" + link.to)
			app.actor.position = app.stage.safe_position(inward.position, radius)
			app.actor.velocity = Vector3.ZERO
			await frames()
			await app.interact_with("door", inward.from + ":" + inward.to)
			await frames()
			check(app.room == "FOREST_TRAIL" and Forest.region_at(app.actor.position.x).id == Forest.data().segments[link.index].id, "Rueckweg in richtigen Waldabschnitt: " + species + "/" + link.to)
			check(app.actor.position.distance_to(outward.position) < .35 and app.actor.is_on_floor(), "Rueckkehr am Abzweig auf festem Boden: " + species + "/" + link.to)
		print("Waldanschluesse geprueft: " + species)
	# Schilder muessen auch ausserhalb der alten kleinen Icon-Treffzone antippbar sein.
	app.selected = "mosslyn"
	await app._mount_world("OUTDOOR", Catalog.position_for("FOREST", Vector2(.48,.55)))
	app._resume()
	await frames()
	app._update_camera(10.0)
	app.hud._update_markers()
	var sign: Dictionary = {}
	for marker in app.hud.markers:
		if marker.id == "FOREST:FOREST_TRAIL": sign = marker
	check(not sign.is_empty() and sign.has("sign_rect"), "Waldeingang hat ein sichtbares Wegschild")
	if not sign.is_empty() and sign.has("sign_rect"):
		var tap: Vector2 = sign.sign_rect.position + Vector2(6,19)
		check(tap.distance_to(sign.screen) > 33, "Tipp liegt auf der Tafel ausserhalb des alten Icons")
		check(app.hud._tap_marker(tap), "Gesamte Schildflaeche nimmt Tipp an")
		for i in 720:
			if app.room == "FOREST_TRAIL": break
			await physics_frame
		check(app.room == "FOREST_TRAIL" and app.pending_interaction.is_empty(), "Tipp laeuft zum versetzten Eingang und betritt den Wald")
	for location in ["FOREST","SUN_GLADE","MIST_WATCH","MILL_BROOK","ASH_ASCENT","SEAL_COURT"]:
		await app._mount_world("OUTDOOR" if location == "FOREST" else location, Catalog.position_for(location, Vector2(.5,.55)))
		app._resume()
		await frames()
		app._update_camera(10.0)
		app.hud._update_markers()
		var signs: Array = app.hud.markers.filter(func(m): return m.has("sign_rect"))
		check(signs.size() == (2 if location == "FOREST" else app.stage.doors.size()), "Alle Streckenanschluesse beschildert: " + location)
		for i in signs.size():
			var door: Dictionary = signs[i].door
			var start: Vector3 = app.stage.safe_position(door.position + Vector3.RIGHT * (-1.5 if door.position.x > 0 else 1.5), app.actor.collider.shape.radius)
			check(not Navigator.find_path(app.stage, start, door.position, app.actor.collider.shape.radius).is_empty(), "Zugang auf begehbarer Spur: " + location + "/" + door.to)
			for j in range(i + 1, signs.size()):
				check(not signs[i].sign_rect.intersects(signs[j].sign_rect), "Lesbare getrennte Schilder: " + location)
			if location in ["MIST_WATCH","ASH_ASCENT"] and door.to in ["SUN_GLADE","MIST_WATCH"]:
				check(door.at.x < .2 and door.route_direction == "LEFT", "Rueckweg liegt links statt neben dem Weiterweg: " + location)
	DirAccess.remove_absolute(app.save_path)
	print("Godot Streckenanschluesse: %s Checks, %s Fehler" % [checks, failures])
	app.queue_free()
	await process_frame
	quit(1 if failures else 0)
