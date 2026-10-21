extends SceneTree

const Catalog = preload("res://scripts/world_catalog.gd")
var failures := 0
var checks := 0

func _initialize() -> void:
	call_deferred("run")

func check(ok: bool, message: String) -> void:
	checks += 1
	if not ok:
		failures += 1
		push_error(message)

func frames(count: int) -> void:
	for i in count:
		await physics_frame

func run() -> void:
	var app: Node3D = load("res://scenes/full_world.tscn").instantiate()
	DirAccess.make_dir_recursive_absolute("user://godot-tests")
	app.save_path = "user://godot-tests/visual.json"
	root.add_child(app)
	while app.loading_world:
		await process_frame
	app.selected = "fennec"
	await app._mount_world("OUTDOOR", Catalog.position_for("POND", Vector2(0.3, 0.5)))
	app._resume()
	for resident in app.actors:
		if resident != app.actor:
			resident.enabled = false
			resident.collider.disabled = true
	# Echte Bewegung durch alle 17 Aussenorte und sechs Bildanschluesse.
	var actor: CharacterBody3D = app.actor
	for direction in [1, -1]:
		actor.position = app.stage.safe_position(Vector3(2.5 if direction == 1 else 207.5, 0.03, 0.0), actor.collider.shape.radius)
		actor.stop_input()
		await frames(5)
		var targets := []
		for i in range(411):
			var x := 2.5 + i * 0.5
			var limits: Vector2 = app.stage._limits_at(x)
			targets.append(Vector3(x, 0.03, Catalog.image_z((limits.x + limits.y) * 0.5)))
		if direction < 0:
			targets.reverse()
		var reached := 0
		for target in targets:
			for step in range(100):
				var gap := Vector2(target.x - actor.position.x, target.z - actor.position.z)
				if gap.length() < 0.12:
					reached += 1
					break
				actor.touch_vector = gap.normalized() * 0.7
				await frames(1)
			if Vector2(target.x-actor.position.x,target.z-actor.position.z).length() >= .12:
				print("Unreached: ",target," actual: ",actor.position)
		check(reached == targets.size() and actor.is_on_floor(), "Durchgehender gemalter Weg, Richtung %s: %s/%s" % [direction, reached, targets.size()])
		print("Weg Richtung %s: %s/%s" % [direction, reached, targets.size()])
	actor.stop_input()
	for region in Catalog.data().regions:
		for u in [0.03, 0.12, 0.27, 0.43, 0.62, 0.80, 0.97]:
			var x: float = region.origin + u * 30.0
			var band := Catalog.band(region, u, true)
			check(not app.stage.is_walkable(Vector3(x, 0, Catalog.image_z(band.x) - 0.15)), "Hintere Vegetation ist gesperrt: " + region.places[0])
			check(not app.stage.is_walkable(Vector3(x, 0, Catalog.image_z(band.y) + 0.15)), "Wasser/Vordergrund ist gesperrt: " + region.places[0])
	for room in Catalog.data().interiors:
		await app._mount_world(room, Catalog.position_for(room, Vector2(0.5, 0.7)))
		app._resume()
		check(app.stage.find_child("RoomSideWalls", true, false) != null, "Schraege Seitenwaende: " + room)
		var copies := 0
		for child in app.stage.get_children():
			if child.name.ends_with("PaintedFront"):
				copies += 1
		check(copies == 0, "Kein uebermalender Moebel-Bildausschnitt: " + room)
		app.yaw = 8
		app._update_camera(10)
		check(app.yaw == 0, "Innenraum behält die gezeichnete Blickachse: " + room)
		for species in Catalog.STARTERS:
			app.actor.species = species
			app.actor.set_world_scale(app.stage.base_pixel_size, app.stage.jump_height)
			app.stage.fit_character(app.actor.sprite.pixel_size)
			var radius: float = app.actor.collider.shape.radius
			for u in [0.0, 0.15, 0.35, 0.5, 0.72, 0.88, 1.0]:
				for v in [0.0, 0.4, 0.8, 1.0]:
					var safe: Vector3 = app.stage.safe_position(Catalog.position_for(room, Vector2(u, v)), radius)
					check(app.stage.is_walkable(safe, radius), "Freier Spawn innerhalb der Raumkontur: %s/%s (%s,%s)" % [room, species, u, v])
			check(app.actor.sprite.billboard == BaseMaterial3D.BILLBOARD_ENABLED and app.actor.sprite.position == Vector3.ZERO, "Unverzerrte, fussverankerte Originalzeichnung: " + species)
	check(Catalog.relative_height("hootlet") == 0.62, "Hootlet bleibt als kleinere Eule im gemeinsamen Massstab")
	print("Godot visual geometry: %s Checks, %s Fehler" % [checks, failures])
	DirAccess.remove_absolute(app.save_path)
	app.queue_free()
	await process_frame
	quit(1 if failures else 0)
