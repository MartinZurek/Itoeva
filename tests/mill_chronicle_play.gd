extends SceneTree

const Catalog = preload("res://scripts/world_catalog.gd")
const Collection = preload("res://scripts/card_collection.gd")
const Mill = preload("res://scripts/mill_chronicle.gd")

var app: Node3D
var output := ""
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
	for i in count:
		await physics_frame
		await process_frame

func mount(species: String, room: String, uv: Vector2) -> void:
	app.selected = species
	await app._mount_world(room, Catalog.position_for(room, uv))
	app._resume()
	for resident in app.actors:
		if resident != app.actor:
			resident.enabled = false
			resident.collider.disabled = true
	await frames(12)
	app.stage.set_time(12.0)

func position_at(point: Vector3) -> void:
	app.actor.position = point
	app.actor.velocity = Vector3.ZERO
	app._update_camera(10.0)
	await frames(10)

func button(text: String) -> Button:
	for candidate in app.menu_buttons:
		if is_instance_valid(candidate) and candidate.text == text:
			return candidate
	return null

func shot(name: String) -> void:
	if output.is_empty():
		return
	RenderingServer.render_loop_enabled = true
	await RenderingServer.frame_post_draw
	root.get_texture().get_image().save_png(output.path_join(name + ".png"))
	RenderingServer.render_loop_enabled = false

func mill_screen(uv: Vector2) -> Vector2:
	var region := {"asset": "res://assets/expansion/mill-brook.png", "origin": -app.stage.width * .5,
		"width": app.stage.width, "room_band": app.stage.interior_definition().band}
	var world: Vector3 = app.stage._point(region, uv, false)
	return root.get_stretch_transform() * app.camera.unproject_position(world)

func pixels_changed(a: Image, b: Image, uv_start: Vector2, uv_end: Vector2) -> int:
	var from := Vector2i(mill_screen(uv_start))
	var to := Vector2i(mill_screen(uv_end))
	var area := Rect2i(from, to - from).abs().intersection(Rect2i(Vector2i.ZERO, a.get_size()))
	var changed := 0
	for y in range(area.position.y, area.end.y):
		for x in range(area.position.x, area.end.x):
			if absf(a.get_pixel(x, y).get_luminance() - b.get_pixel(x, y).get_luminance()) > .025:
				changed += 1
	return changed

func compare_motion(mill) -> void:
	if output.is_empty():
		return
	paused = true
	RenderingServer.render_loop_enabled = true
	mill.material.set_shader_parameter("animation_time", 3.0)
	mill.material.set_shader_parameter("mill_wheel_angle", 0.0)
	await RenderingServer.frame_post_draw
	var a := root.get_texture().get_image()
	mill.material.set_shader_parameter("mill_wheel_angle", .34)
	await RenderingServer.frame_post_draw
	var b := root.get_texture().get_image()
	var wheel := pixels_changed(a, b, Vector2(.345, .325), Vector2(.41, .52))
	var roof := pixels_changed(a, b, Vector2(.10, .15), Vector2(.24, .27))
	check(wheel > 55 and roof == 0, "Die Speichen bewegen sich und das Mühlenhaus steht still")
	print("Wasserrad: ", wheel, " geänderte Pixel, Dach: ", roof)
	b.save_png(output.path_join("03-muehlrad.png"))

	# Beide Wasserstellungen zum selben Shaderzeitpunkt, ohne Radbewegung.
	mill.material.set_shader_parameter("mill_wheel_mix", 0.0)
	mill.material.set_shader_parameter("mill_flow", -1.0)
	await RenderingServer.frame_post_draw
	var side_view := root.get_texture().get_image()
	mill.material.set_shader_parameter("mill_flow", 1.0)
	await RenderingServer.frame_post_draw
	var wheel_view := root.get_texture().get_image()
	var feed := pixels_changed(side_view, wheel_view, Vector2(.397, .32), Vector2(.465, .52))
	var side := pixels_changed(side_view, wheel_view, Vector2(.51, .425), Vector2(.65, .535))
	check(feed > 45 and side > 45, "Beide Wasserläufe reagieren im echten Bild auf die Hebelstellung")
	print("Wasserläufe: ", feed, " geänderte Pixel am Rad, ", side, " im Nebenlauf")
	mill.material.set_shader_parameter("mill_wheel_mix", mill.wheel_mix)
	mill.material.set_shader_parameter("animation_time", -1.0)
	RenderingServer.render_loop_enabled = false
	paused = false

func run() -> void:
	OS.set_environment("MOTION_M4", "1")
	var args := OS.get_cmdline_user_args()
	if not args.is_empty():
		output = args[0]
		DirAccess.make_dir_recursive_absolute(output)
		RenderingServer.render_loop_enabled = false
	root.size = Vector2i(960, 540)
	app = load("res://scenes/full_world.tscn").instantiate()
	app.save_path = "user://mill-chronicle-test-%s.json" % OS.get_process_id()
	root.add_child(app)
	while app.loading_world:
		await process_frame
	await app.start_game()
	await mount("mosslyn", Mill.ROOM, Vector2(.57, .55))
	var mill = Mill.current(app)
	check(mill != null and mill.route == Mill.Route.CLOSED, "Der Mühlenbach startet mit geschlossenem Wehr")
	if mill == null:
		quit(1)
		return
	check(mill.material.shader.resource_path.ends_with("/painted_mill.gdshader"), "Die Kulisse benutzt das lokale Mühlenmaterial")
	var initial: Dictionary = app.CardFinds.offer_for(app, Mill.FIND)
	var coins: int = Collection.state(app.save_data, app.selected).coins
	var cards := Collection.owned(app.save_data, app.selected, "brook_oath")
	await position_at(initial.approach)
	app.interact_with("mill_lever", Mill.LEVER)
	check(mill.route == Mill.Route.CLOSED, "Der Hebel lässt sich nicht aus der Ferne umlegen")
	app.interact_with("card_find", Mill.FIND)
	check(button("Zum Ufer zurück") != null and button("Karte bergen") == null, "Die Chronik verweist zuerst auf das Wehr")
	app.collection_ui._find_answer(initial.find, 0)
	check(Collection.state(app.save_data, app.selected).coins == coins, "Auch ein alter Dialog vergibt ohne Wasser keine Münzen")
	app._resume()

	var lever := Mill.offer(app)
	await position_at(lever.approach)
	app.notice_clock = 0.0
	await frames(8)
	await shot("01-wehr")
	app.hud._update_markers()
	var screen := Vector2.INF
	for marker in app.hud.markers:
		if marker.id == Mill.LEVER:
			screen = marker.screen
	check(screen.is_finite() and app.hud._tap_marker(screen), "Der Hebel hat ein erreichbares Touchziel")
	check(mill.route == Mill.Route.SIDE, "Der erste Zug lenkt Wasser zum Nebenlauf")
	app.interact_with("mill_lever", Mill.LEVER)
	check(mill.route == Mill.Route.SIDE, "Ein Doppeltipp überspringt den Nebenlauf nicht")
	await frames(46)
	check(mill.flow == -1.0 and mill.wheel_speed == 0.0, "Nebenlauf erreicht seine Stellung, Rad bleibt stehen")
	check(not app.CardFinds.offer_for(app, Mill.FIND).can_claim, "Der Nebenlauf löst das Rätsel nicht")
	await shot("02-nebenlauf")

	app.interact_with("mill_lever", Mill.LEVER)
	check(mill.route == Mill.Route.WHEEL and mill.sound != null and mill.sound.stream != null, "Die zweite Stellung öffnet den Zulauf und nutzt den vorhandenen Wasserklang")
	await frames(52)
	check(mill.flow == 1.0 and mill.wheel_angle > .2, "Das Rad nimmt Schwung auf")
	await compare_motion(mill)
	var ready: Dictionary = app.CardFinds.offer_for(app, Mill.FIND)
	await position_at(ready.approach)
	app.interact_with("card_find", Mill.FIND)
	check(button("Karte bergen") != null, "Die Chronik bietet nach dem Wasserrätsel die Karte an")
	button("Karte bergen").pressed.emit()
	check(Collection.owned(app.save_data, app.selected, "brook_oath") == cards + 1, "Der Schwur gehört jetzt zur Sammlung")
	check(Collection.state(app.save_data, app.selected).coins == coins + 8, "Acht Wegmünzen werden einmalig verbucht")
	await shot("04-kartenfund")
	app._resume()
	check(Mill.offer(app).is_empty() and app.CardFinds.offer_for(app, Mill.FIND).is_empty(), "Ein abgeschlossener Fund bietet keine zweite Belohnung")
	var saved: Dictionary = JSON.parse_string(FileAccess.get_file_as_string(app.save_path))
	check(saved.card_collection.mosslyn.claims.get(Mill.CLAIM, false), "Vorhandene Fundquittung wurde gespeichert")

	await mount("nocti", Mill.ROOM, Vector2(.39, .44))
	check(Mill.current(app).route == Mill.Route.CLOSED, "Ein anderes Profil beginnt mit geschlossenem Wehr")
	app.effects_on = false
	app.interact_with("mill_lever", Mill.LEVER)
	check(Mill.current(app).route == Mill.Route.SIDE and Mill.current(app).sound == null, "Stummgeschaltet bleibt die sichtbare Aktion spielbar")
	await mount("nocti", "SUN_GLADE", Vector2(.40, .55))
	check(Mill.current(app) == null and Mill.offer(app).is_empty(), "Außerhalb der Mühle sind Wehr und Effekt abgebaut")
	await mount("nocti", Mill.ROOM, Vector2(.40, .55))
	check(Mill.current(app).route == Mill.Route.CLOSED, "Ein unfertiger Versuch startet bei Rückkehr neutral")
	app.save_data = saved
	await mount("mosslyn", Mill.ROOM, Vector2(.40, .55))
	check(Mill.current(app).route == Mill.Route.WHEEL and Mill.offer(app).is_empty(), "Ein alter Spielstand mit Fundquittung zeigt das Rad in Betrieb")
	check(Collection.state(app.save_data, "mosslyn").coins == coins + 8, "Rückkehr verändert die Belohnung nicht")

	var save_path: String = app.save_path
	app.queue_free()
	await process_frame
	DirAccess.remove_absolute(save_path)
	RenderingServer.render_loop_enabled = true
	print("Mill chronicle play: %s Checks, %s Fehler" % [checks, failures])
	quit(1 if failures else 0)
