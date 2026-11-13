extends SceneTree

const Catalog = preload("res://scripts/world_catalog.gd")
const Steps = preload("res://scripts/ground_steps.gd")

var app: Node3D
var output := ""
var checks := 0
var failures := 0
var captures := {}

func _initialize() -> void:
	call_deferred("run")

func check(ok: bool, label: String) -> void:
	checks += 1
	if not ok:
		failures += 1
		push_error(label)

func frames(count: int) -> void:
	for frame in count:
		await physics_frame
		await process_frame

func mount(species: String, room: String, point: Vector3) -> void:
	app.selected = species
	await app._mount_world(room, point)
	app._resume()
	for resident in app.actors:
		if resident != app.actor:
			resident.enabled = false
			resident.collider.disabled = true
	app.hud.hide()
	await frames(10)
	app.stage.set_time(12.0)

func coast_point(x: float) -> Vector3:
	var region := Catalog.region_for("BEACH")
	var limits := Catalog.band(region, (x - float(region.origin)) / float(region.width), true)
	return Vector3(x, .03, Catalog.image_z((limits.x + limits.y) * .5))

func walk(force: float, ticks: int) -> void:
	app.actor.touch_vector = Vector2.RIGHT * force
	await frames(ticks)

func live_stamps(dust: bool) -> int:
	var pool: Node3D = app.ground_steps
	var first := Steps.PRINT_CAPACITY if dust else 0
	var end := Steps.CAPACITY if dust else Steps.PRINT_CAPACITY
	var result := 0
	for i in range(first, end):
		if pool.deaths[i] > pool.clock:
			result += 1
	return result

func capture(name: String) -> void:
	if output.is_empty() or captures.has(name):
		return
	captures[name] = true
	paused = true
	var pool: Node3D = app.ground_steps
	RenderingServer.render_loop_enabled = true
	await RenderingServer.frame_post_draw
	var image := root.get_texture().get_image()
	pool.hide()
	await RenderingServer.frame_post_draw
	var without_effects := root.get_texture().get_image()
	pool.show()
	var changed := {}
	for i in pool.multimesh.instance_count:
		if pool.deaths[i] <= pool.clock:
			continue
		var screen: Vector2 = app.camera.unproject_position(pool.multimesh.get_instance_transform(i).origin)
		var center := Vector2i(root.get_stretch_transform() * screen)
		var area := Rect2i(center - Vector2i(25, 16), Vector2i(51, 33)).intersection(Rect2i(Vector2i.ZERO, image.get_size()))
		for y in range(area.position.y, area.end.y):
			for x in range(area.position.x, area.end.x):
				if absf(image.get_pixel(x, y).get_luminance() - without_effects.get_pixel(x, y).get_luminance()) > .025:
					changed[Vector2i(x, y)] = true
	check(changed.size() > 15, name + " ist in der echten Kulisse sichtbar")
	print(name, ": ", changed.size(), " geänderte Pixel im Nahbereich")
	image.save_png(output.path_join(name + ".png"))
	without_effects.save_png(output.path_join(name + "-ohne-effekt.png"))
	RenderingServer.render_loop_enabled = false
	paused = false

func run() -> void:
	check(is_equal_approx(Steps.shader_birth(7200.375), 0.375), "Stempelzeit bleibt nach zwei Stunden präzise")
	check(is_equal_approx(fposmod(0.25 - Steps.shader_birth(7199.75), Steps.SHADER_PERIOD), 0.5), "Spuren altern über den Wechsel der Shaderuhr hinweg")
	OS.set_environment("MOTION_M4", "1")
	var args := OS.get_cmdline_user_args()
	if not args.is_empty():
		output = args[0]
		DirAccess.make_dir_recursive_absolute(output)
		RenderingServer.render_loop_enabled = false
	root.size = Vector2i(960, 540)
	app = load("res://scenes/full_world.tscn").instantiate()
	app.save_path = "user://ground-contact-play.json"
	DirAccess.remove_absolute(app.save_path)
	root.add_child(app)
	while app.loading_world:
		await process_frame

	for species in ["mosslyn", "wyrmling"]:
		await mount(species, "OUTDOOR", coast_point(7.5))
		var heading := 1.0
		var farthest: float = app.actor.position.x
		var peak_dust := 0
		for tick in 365:
			var x: float = app.actor.position.x
			farthest = maxf(farthest, x)
			if x > 14.1:
				heading = -1.0
			elif x < 7.7:
				heading = 1.0
			await walk(.69 * heading, 1)
			peak_dust = maxi(peak_dust, live_stamps(true))
			if tick > 170 and tick < 230 and app.ground_steps.clock - app.ground_steps.last_birth < .06:
				await capture(species + "-strand")
		check(farthest > 13.0, species + " läuft tatsächlich am Strand")
		check(Steps.surface_kind(app.stage, app.actor.position) == "sand", "Lauf endet auf Sand")
		check(live_stamps(false) >= 5, species + " hinterlässt eine zusammenhängende Sandspur")
		check(peak_dust > 0, species + " wirbelt beim Gehen Sand auf")
		check(app.ground_steps.multimesh.instance_count == 12, "Zwölf Instanzen auch bei Sand plus Staub")
		print(species, " Strand: ", live_stamps(false), " Spuren, bis zu ", peak_dust, " Staubstöße")
		app.actor.stop_input()
		await frames(70)
		check(live_stamps(false) >= 4 and live_stamps(true) == 0, "Staub vergeht vor der Spur")
		await capture(species + "-spur")
		await frames(370)
		check(app.ground_steps.active == 0 and not app.ground_steps.visible, "Spuren laufen vollständig ab")

	await mount("mosslyn", "SUN_GLADE", Catalog.position_for("SUN_GLADE", Vector2(.34, .55)))
	if DisplayServer.get_name() != "headless":
		var pool: Node3D = app.ground_steps
		var first: int = pool.cursor
		pool._stamp(app.actor.position, Vector3.RIGHT, false, 1.0, 0.5, Color(0.8, 0.7, 0.6))
		pool._stamp(app.actor.position + Vector3.RIGHT, Vector3.RIGHT, false, -1.0, 0.5, Color(0.3, 0.4, 0.5))
		var old_color: Color = pool.multimesh.get_instance_color(first)
		check(absf(old_color.r - 0.8) < 0.01, "Ältere Spur behält ihre Beleuchtung")
		await mount("mosslyn", "SUN_GLADE", Catalog.position_for("SUN_GLADE", Vector2(.34, .55)))
	await walk(.38, 175)
	check(app.actor.actual_speed > .55 and app.actor.actual_speed < 1.2, "Langsames Gehen trifft den früheren toten Tempobereich")
	check(app.ground_steps.emissions >= 2, "Auch langsames Gehen löst auf Erde Staub aus")
	check(live_stamps(false) == 0, "Erde hinterlässt nur Staub")
	print("Sonnenhain langsam: ", app.ground_steps.emissions, " Auslösungen")
	for tick in 120:
		var age: float = app.ground_steps.clock - app.ground_steps.last_birth
		if live_stamps(true) > 0 and age >= .14 and age < .20:
			await capture("mosslyn-sonnenhain")
			break
		await frames(1)

	await mount("mosslyn", "OUTDOOR", coast_point(3.3))
	await walk(.58, 74)
	check(Steps.surface_kind(app.stage, app.actor.position) == "wood", "Mooslyn bewegt sich auf dem Holzdeck")
	check(app.ground_steps.emissions == 0, "Holz bleibt ohne Staub und Abdruck")
	var before_origin: float = app.actor.painted_material.get_shader_parameter("scene_origin")
	app.actor.position = Catalog.position_for("COAST_PATH", Vector2(0.5, 0.5))
	await frames(3)
	var new_origin: float = app.stage.actor_occlusion(app.actor.global_position).origin
	check(before_origin != new_origin and app.actor.painted_material.get_shader_parameter("scene_origin") == new_origin,
		"Beim Abschnittswechsel wird die Verdeckungsmaske auf die neue Kulisse ausgerichtet")
	for species in ["nocti", "starlet"]:
		await mount(species, "OUTDOOR", coast_point(8.3))
		await walk(.69, 85)
		check(app.ground_steps.emissions == 0, species + " erzeugt keine Bodenspur")

	var brook := Steps.Forest.position_for(4, 0.12, 0.5)
	await mount("mosslyn", "FOREST_TRAIL", brook)
	var prior_emissions: int = app.ground_steps.emissions
	app.wonders.perform(app, {"wonder": "tracks", "position": brook + Vector3.UP * 0.65})
	check(app.ground_steps.emissions == prior_emissions + 2, "Mooslyns Spurensuche setzt weiterhin zwei Abdrücke")

	if not output.is_empty():
		check(captures.size() == 5, "Alle vorgesehenen Spielansichten gerendert")
		RenderingServer.render_loop_enabled = true
	app.queue_free()
	await process_frame
	DirAccess.remove_absolute("user://ground-contact-play.json")
	print("Ground contact play: %s Checks, %s Fehler" % [checks, failures])
	quit(1 if failures else 0)
