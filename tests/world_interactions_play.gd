extends SceneTree

const Catalog = preload("res://scripts/world_catalog.gd")
const Path = preload("res://scripts/interaction_path.gd")

var app: Node3D
var checks := 0
var failures := 0
var output := ""

func _initialize() -> void:
	run.call_deferred()

func check(ok: bool, message: String) -> void:
	checks += 1
	if not ok:
		failures += 1
		push_error(message)

func frames(count: int) -> void:
	for i in count:
		await physics_frame
		await process_frame

func find(id: String, entries: Array) -> Dictionary:
	for entry: Dictionary in entries:
		if entry.id == id: return entry
	return {}

func move_to(point: Vector3) -> void:
	app.actor.position = point
	app.actor.velocity = Vector3.ZERO
	app._update_camera(10.0)
	await frames(9)
	app.hud._update_markers()

func capture(name: String) -> void:
	if output.is_empty(): return
	await RenderingServer.frame_post_draw
	root.get_texture().get_image().save_png(output.path_join(name + ".png"))

func run() -> void:
	var args := OS.get_cmdline_user_args()
	if not args.is_empty():
		output = args[0]
		DirAccess.make_dir_recursive_absolute(output)
	root.size = Vector2i(1280, 720)
	app = load("res://scenes/gameplay.tscn").instantiate()
	root.add_child(app)
	await process_frame
	app.save_path = "user://world-interactions-%d.json" % OS.get_process_id()
	app.save_data = {"collected": ["park-pebbles"]}
	await app.visit_test_place("BEACH")
	await frames(8)
	check(find("park-pebbles", app.stage.pickups).is_empty(), "Alter Spielstand blendet gesammelte Kiesel weiterhin aus")
	var shell := find("beach-shell", app.stage.pickups)
	check(not shell.is_empty(), "Die Muschel liegt in der Strandkulisse")
	if shell.is_empty():
		quit(1)
		return

	var radius: float = app.actor.collider.shape.radius + 0.035
	var far: Vector3 = app.stage.safe_position(shell.position + Vector3.RIGHT * 6.0, radius)
	await move_to(far)
	check(find(shell.id, app.hud.markers).is_empty(), "Ein entfernter Fund hat keinen sichtbaren Marker")
	await move_to(app.stage.safe_position(shell.position + Vector3.RIGHT * 2.8, radius))
	var distant_marker := find(shell.id, app.hud.markers)
	check(not distant_marker.is_empty() and not distant_marker.get("ready", false), "Leiser Fundhinweis erscheint vor der Aktionsreichweite")
	var distant_tint: Color = shell.art.modulate
	check(not Path.find_path(app.stage, app.actor.position, shell.position, radius).is_empty(), "Ein begehbarer Weg führt zur Muschel")
	await capture("strand-fundhinweis")
	if not distant_marker.is_empty():
		app.hud._tap_marker(distant_marker.screen)
		check(app.pending_interaction.get("id") == shell.id, "Ein entfernter Klick nutzt die vorhandene Annäherung")
		app.cancel_approach()
	await move_to(app.stage.safe_position(shell.position + Vector3.RIGHT * 0.7, radius))
	var marker := find(shell.id, app.hud.markers)
	check(marker.get("ready", false), "In Reichweite ist der Fund zur Aktion bereit")
	check(shell.art.modulate.get_luminance() > distant_tint.get_luminance(), "Die Muschel selbst hellt auf")
	check(app.hud._focus_marker().get("label", "") == "Rosa Muschel · Aufheben", "Der Hinweis nennt Fund und Aktion")
	await capture("strand-muschel-in-reichweite")
	if not marker.is_empty():
		var click := InputEventMouseButton.new()
		click.button_index = MOUSE_BUTTON_LEFT
		click.pressed = true
		click.position = app.hud._prompt_rect(marker).get_center()
		app.hud._input(click)
	check(not shell.art.visible and shell.id in app.save_data.collected, "Mausklick auf den Hinweis hebt die Muschel auf")
	check(app.hud.pickup_flash_clock > 0.0 and app.notice.contains("Sonnenaufgang"), "Aufheben erzeugt sichtbares Feedback und einen Fundtext")
	app.hud._update_markers()
	check(find(shell.id, app.hud.markers).is_empty(), "Der verbrauchte Marker verschwindet sofort")

	await app.visit_test_place("ALDER_FORD")
	await frames(8)
	var cone := find("alder-ford-cone", app.stage.pickups)
	check(not cone.is_empty(), "An der Erlenfurt liegt ein eigener Fund")
	if not cone.is_empty():
		await move_to(app.stage.safe_position(cone.position - Vector3.RIGHT * 0.7, radius))
		var cone_marker := find(cone.id, app.hud.markers)
		check(cone_marker.get("ready", false), "Erlenzapfen ist vom Weg erreichbar")
		if not cone_marker.is_empty():
			var touch := InputEventScreenTouch.new()
			touch.index = 4
			touch.pressed = true
			touch.position = cone_marker.body_rect.get_center()
			app.hud._input(touch)
		check(cone.id in app.save_data.collected, "Direktes Antippen des Gegenstands sammelt ihn ein")

	var brook: Dictionary = app.wonders.offer(app)
	check(brook.get("label", "") == "Bach · Lauschen", "Die Erlenfurt bietet eine eigene Weltaktion")
	if not brook.is_empty():
		await move_to(brook.approach)
		var brook_marker := find(brook.id, app.hud.markers)
		if not brook_marker.is_empty():
			app.hud._tap_marker(brook_marker.screen)
		check(app.wonders.active.get("kind") == "brook" and app.notice.contains("kluckert"), "Lauschen löst die Rückmeldung am Bach aus")
		check(app.wonders.offer(app).is_empty(), "Die Aktion pausiert nach dem Lauschen")
		app.wonders.cooldowns[app.selected] = app.game_clock
		check(not app.wonders.offer(app).is_empty(), "Lauschen bleibt wiederholbar")

	app._load_save()
	await app.visit_test_place("COAST_PATH")
	await frames(8)
	check(find("beach-shell", app.stage.pickups).is_empty(), "Geladener Spielstand lässt die Muschel eingesammelt")
	var glass := find("coast-sea-glass", app.stage.pickups)
	check(not glass.is_empty(), "Der Küstenpfad hat einen eigenen Fund")
	if not glass.is_empty():
		await move_to(app.stage.safe_position(glass.position - Vector3.RIGHT * 0.7, radius))
		check(app.context_target().get("id") == glass.id, "Die Aktionstaste wählt den Fund in Reichweite")
		await app.interact()
		check(glass.id in app.save_data.collected, "Die Kontextaktion hebt Meerglas auf")

	DirAccess.remove_absolute(app.save_path)
	print("Weltinteraktionen · %d Prüfungen, %d Fehler" % [checks, failures])
	quit(1 if failures > 0 else 0)
