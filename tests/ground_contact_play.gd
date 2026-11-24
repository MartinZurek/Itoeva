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
	var glade_material: ShaderMaterial
	if app.room == "SUN_GLADE":
		glade_material = app.stage.painted_materials[0]
		glade_material.set_shader_parameter("animation_time", 0.0)
	RenderingServer.render_loop_enabled = true
	await RenderingServer.frame_post_draw
	var image := root.get_texture().get_image()
	pool.hide()
	await RenderingServer.frame_post_draw
	var without_effects := root.get_texture().get_image()
	pool.show()
	if glade_material != null:
		glade_material.set_shader_parameter("animation_time", -1.0)
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
	await mount("mosslyn", "OUTDOOR", coast_point(7.5))
	var floor_y: float = app.actor.position.y
	var camera_y: float = app.camera.position.y
	app.actor.request_action("jump")
	await frames(17)
	check(app.actor.position.y > floor_y + 0.4 and is_equal_approx(app.camera_ground_y, floor_y)
		and absf(app.camera.position.y - camera_y) < 0.01,
		"Die Kamera hält die Bodenhöhe während eines echten Sprungs")
	await frames(60)
	var platform := StaticBody3D.new()
	var shape := BoxShape3D.new()
	shape.size = Vector3(1.4, 0.48, 1.4)
	var collision := CollisionShape3D.new()
	collision.shape = shape
	platform.add_child(collision)
	app.stage.add_child(platform)
	platform.position = app.actor.position + Vector3.UP * 0.24
	app.actor.position.y += 0.8
	await frames(45)
	check(app.actor.is_on_floor() and app.camera_ground_y > floor_y + 0.4
		and is_equal_approx(app.camera_ground_y, app.actor.position.y),
		"Nach der Landung auf einer höheren Fläche übernimmt die Kamera deren Höhe")
	platform.queue_free()
	await frames(45)
	check(app.actor.is_on_floor() and absf(app.camera_ground_y - floor_y) < 0.02
		and is_equal_approx(app.camera_ground_y, app.actor.position.y),
		"Nach dem Abstieg folgt die Kamera wieder der unteren Fläche")
	await walk(.58, 12)
	app.actor.gait = 0.27
	await walk(1.0, 12)
	check(app.actor.actual_speed > 3.5 and app.actor.painted_cycle_mode == "walk" and not app.actor.bound_flow.active,
		"Mooslyn beschleunigt schon vor der sichtbaren Galoppvorbereitung")
	await walk(.58, 12)
	check(app.actor.authored_transition.active.is_empty() and not app.actor.bound_flow.active,
		"Ein zurückgenommener Sprint startet keinen verzögerten Galopp")
	for tick in 70:
		await walk(1.0, 1)
		if app.actor.painted_cycle_mode == "lower":
			break
	check(app.actor.painted_cycle_mode == "lower" and app.actor.sprite.frame == 0
		and app.actor._painted_phase(app.actor.painted_cycles.walk, app.actor.gait) == 0,
		"Die erste Absenkpose setzt an der passenden abgestützten Gehpose ein")
	for tick in 24:
		await walk(1.0, 1)
		if app.actor.bound_flow.active:
			break
	check(app.actor.bound_flow.active and app.actor.painted_cycle_mode == "run"
		and app.actor.sprite.frame == int(app.actor.painted_cycles.run.entry_phase),
		"Der Galopp startet mit der passenden Bodenpose")

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
	check(live_stamps(false) >= 2, "Auf dem Erdweg bleiben Hufabdrücke zurück")
	check(app.stage.painted_materials[0].get_shader_parameter("walker_motion") > 0.2,
		"Die Pflanzen am Wegesrand erhalten den Bewegungsimpuls")
	var wind_mask: Texture2D = app.stage.painted_materials[0].get_shader_parameter("shrub_motion_mask")
	var painted_mask := wind_mask.get_image()
	var mask_size := Vector2(painted_mask.get_size())
	var still_regions := [Vector2(.57,.62), Vector2(.28,.60), Vector2(.54,.75), Vector2(.24,.37)]
	var still := true
	for point: Vector2 in still_regions:
		var pixel := Vector2i(point * mask_size)
		still = still and painted_mask.get_pixelv(pixel).r == 0.0
	check(still and painted_mask.get_pixelv(Vector2i(Vector2(.343,.632) * mask_size)).r > 0.3,
		"Windmaske erfasst Strauchspitzen und lässt See, Fels, Weg und Stamm aus")
	print("Sonnenhain langsam: ", app.ground_steps.emissions, " Auslösungen")
	for tick in 120:
		var age: float = app.ground_steps.clock - app.ground_steps.last_birth
		if live_stamps(true) > 0 and age >= .14 and age < .20:
			await capture("mosslyn-sonnenhain")
			break
		await frames(1)

	app.actor.stop_input()
	await frames(55)
	await capture("mosslyn-erdspur")
	var before_landing: int = app.ground_steps.emissions
	app.actor.request_action("jump")
	await frames(22)
	check(app.ground_steps.emissions == before_landing, "In der Luft werden keine Spuren erzeugt")
	await frames(55)
	check(app.ground_steps.emissions == before_landing + 2 and live_stamps(true) > 0,
		"Die Landung im Stand setzt einen Hufabdruck und wirbelt Staub auf")

	await mount("mosslyn", "SUN_GLADE", Catalog.position_for("SUN_GLADE", Vector2(.26,.55)))
	var timed := {"walk": 0, "run": 0, "previous": 0, "airborne": false, "in_sync": true}
	var observe_contact := func(_delta: float) -> void:
		var actor: CharacterBody3D = app.actor
		var pool: Node3D = app.ground_steps
		var airborne: bool = actor.bound_pose.get("airborne", false)
		if pool.emissions != timed.previous:
			var is_walk: bool = actor.painted_cycle_mode == "walk" and actor.sprite.frame in [4,7]
			var is_run: bool = actor.painted_cycle_mode == "run" and timed.airborne and not airborne
			timed.in_sync = timed.in_sync and (is_walk or is_run) and pool.last_birth == pool.clock and pool.emissions - timed.previous == 2
			if DisplayServer.get_name() != "headless":
				var slot: int = posmod(pool.cursor - 1, Steps.PRINT_CAPACITY)
				var stamp_at: Vector3 = pool.multimesh.get_instance_transform(slot).origin
				timed.in_sync = timed.in_sync and (stamp_at.x - actor.position.x) * actor.facing.x > .2
			if is_walk: timed.walk += 1
			if is_run: timed.run += 1
		timed.previous = pool.emissions
		timed.airborne = airborne
	app.actor.movement_resolved.connect(observe_contact)
	await walk(.55, 176)
	app.actor.stop_input()
	app.actor.position = Catalog.position_for("SUN_GLADE", Vector2(.76, .55))
	await frames(8)
	# Keep the contact sample on the glade before the automatic forest passage.
	for tick in 146:
		if app.actor.position.x < -4.4:
			break
		await walk(-1.0, 1)
	app.actor.movement_resolved.disconnect(observe_contact)
	print("Sonnenhain Hufkontakt: ", timed.walk, " Schritte, ", timed.run, " Galopplandungen")
	check(timed.in_sync and timed.walk >= 3 and timed.run >= 1,
		"Spur und Staub entstehen auf der sichtbaren Hufsetzung im Gehschritt und im Galopp")

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

	await mount("mosslyn", "SUN_GLADE", Catalog.position_for("SUN_GLADE", Vector2(0.23, 0.55)))
	await walk(.45, 24)
	var slow_stride: float = app.actor._stride_length()
	var body_shift: Vector3 = app.actor.painted_material.get_shader_parameter("upper_body_motion")
	await walk(.45, 9)
	check(slow_stride < app.actor.ground_motion_profile.walk_stride
		and app.actor.painted_material.get_shader_parameter("upper_body_motion") != body_shift,
		"Langsames Gehen setzt kürzere Schritte und verlagert den Oberkörper")
	var floor_height: float = app.actor.position.y
	var approach_speed: float = app.actor.actual_speed
	app.actor.request_action("jump")
	var jump_poses := {}
	var hops := 0
	var air_before := false
	var buffered := false
	var lowest_speed := approach_speed
	for tick in 150:
		await frames(1)
		var airborne: bool = app.actor.jump_clock >= 0.0
		if airborne and not air_before:
			hops += 1
		air_before = airborne
		if airborne and app.actor.painted_cycle_active and app.actor.painted_cycle_mode == "run":
			jump_poses[app.actor.sprite.frame] = true
		lowest_speed = minf(lowest_speed, app.actor.actual_speed)
		if airborne and not buffered and app.actor.velocity.y < 0:
			var vertical: float = app.actor.velocity.y
			var height: float = maxf(0.0, app.actor.position.y - floor_height)
			var gravity: float = app.actor.GRAVITY
			var until_ground := (vertical + sqrt(vertical * vertical + 2.0 * gravity * height)) / gravity
			if until_ground < 0.22 and until_ground > 0.17:
				app.actor.request_action("jump")
				buffered = true
		if hops == 2 and not airborne and not app.actor.mosslyn_hop:
			break
	check(buffered and hops == 2 and jump_poses.size() >= 4,
		"Der gepufferte Hüpfer startet nach Bodenkontakt und nutzt die Cape-Posen")
	check(lowest_speed > 0.85 * approach_speed and app.actor.painted_cycle_mode == "walk",
		"Die Figur hält den Vortrieb und geht nach der Landung im Gehzyklus weiter")

	await mount("mosslyn", "SUN_GLADE", Catalog.position_for("SUN_GLADE", Vector2(0.38, 0.55)))
	var roller: CharacterBody3D = app.actor
	roller.facing = Vector2.RIGHT
	var action_point: Vector2 = app.hud.size * 0.5
	app.hud._start_action(0, action_point)
	app.hud._drag_action(action_point + Vector2(60, 0))
	var start_x := roller.global_position.x
	var roll_frames := {}
	var max_clearance := 0.0
	var roll_ticks := 0
	var uses_details := true
	while roller.state == "roll" and roll_ticks < 40:
		await frames(1)
		roll_ticks += 1
		if roller.state == "roll":
			uses_details = uses_details and roller.painted_cycle_mode == "details"
			roll_frames[roller.sprite.frame] = true
			max_clearance = maxf(max_clearance, absf(roller.rest_clearance()))
	check(uses_details and roll_frames.size() == 14 and max_clearance < 0.005,
		"Der Wisch über die Aktionstaste spielt 14 unterschiedliche Rollbilder mit Bodenanker")
	check(roller.state == "idle" and absf(roll_ticks / 60.0 - roller.ROLL_SECONDS) <= 1.0 / 60.0
		and roller.global_position.x > start_x,
		"Die Rolle behält ihre Dauer und ihren Vorschub")

	var brook := Steps.Forest.position_for(4, 0.12, 0.5)
	await mount("mosslyn", "FOREST_TRAIL", brook)
	var prior_emissions: int = app.ground_steps.emissions
	app.wonders.perform(app, {"wonder": "tracks", "position": brook + Vector3.UP * 0.65})
	check(app.ground_steps.emissions == prior_emissions + 2, "Mooslyns Spurensuche setzt weiterhin zwei Abdrücke")
	var trail_lit := Steps.Forest.position_for(2, 0.279, 0.5)
	var trail_between := Steps.Forest.position_for(2, 0.42, 0.5)
	var lit_tint: Color = app.stage.ground_effect_color(trail_lit)
	var lit_base: Color = app.stage.actor_scene_color(trail_lit)
	check(lit_tint.r > lit_base.r + 0.12, "Fackellicht erreicht Spuren auf dem begehbaren Waldweg")
	check(app.stage.ground_effect_color(trail_between).is_equal_approx(app.stage.actor_scene_color(trail_between)),
		"Zwischen den Fackeln bleibt der Weg im kühlen Umgebungslicht")
	var material := ShaderMaterial.new()
	material.shader = app.actor.painted_material.shader
	app.stage.apply_actor_light(material, trail_lit)
	var probe: Vector4 = material.get_shader_parameter("local_light_points")[0]
	var flame_layout: Vector4 = app.stage.forest_materials.TORCH_PATH.get_shader_parameter("flames")[0]
	check(material.get_shader_parameter("local_light_count") > 0 and is_equal_approx(probe.x, trail_lit.x)
		and flame_layout.w > flame_layout.y, "Fackel beleuchtet Figur und Panorama in Richtung Weg")
	var clearing := Steps.Forest.position_for(5, 0.674, 0.5)
	var clearing_base: Color = app.stage.actor_scene_color(clearing)
	check(app.stage.ground_effect_color(clearing).r > clearing_base.r + 0.1, "Die Lichtung wärmt Spuren bei brennenden Fackeln")
	app.stage.set_arena_lights(0.0)
	app.stage.apply_actor_light(material, clearing)
	check(app.stage.ground_effect_color(clearing).is_equal_approx(clearing_base)
		and material.get_shader_parameter("local_light_count") == 0
		and app.stage.forest_materials.SHADOW_CLEARING.get_shader_parameter("torch_gain") == 0.0,
		"Erloschene Fackeln beleuchten weder neue Spuren noch Figur oder Lichtung")

	if not output.is_empty():
		check(captures.size() == 6, "Alle vorgesehenen Spielansichten gerendert")
		RenderingServer.render_loop_enabled = true
	app.queue_free()
	await process_frame
	DirAccess.remove_absolute("user://ground-contact-play.json")
	print("Ground contact play: %s Checks, %s Fehler" % [checks, failures])
	quit(1 if failures else 0)
