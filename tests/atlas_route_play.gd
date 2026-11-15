extends SceneTree

const Catalog = preload("res://scripts/world_catalog.gd")
const Routes = preload("res://scripts/world_routes.gd")
var app: Node3D

func _initialize() -> void:
	_run.call_deferred()

func _run() -> void:
	app = load("res://scenes/full_world.tscn").instantiate()
	app.save_path = "user://atlas-route-%s.json" % OS.get_process_id()
	root.add_child(app)
	while app.loading_world:
		await process_frame
	await app._mount_world("CRAFT", Catalog.position_for("CRAFT", Vector2(0.5, 0.5)))
	app._resume()
	app.show_map()
	await process_frame
	var atlas: Control = app.ui.get_child(0)
	assert(atlas.view == "rooms")
	assert(atlas.track_button.get_rect().end.x < atlas.clear_button.position.x)
	assert(atlas.clear_button.get_rect().end.x <= atlas.frame.size.x)
	_tap(_button("Kapitelziel"))
	assert(atlas.selected == "PARK" and atlas.route[0] == "CRAFT")
	assert(atlas.surface.route.is_empty() and atlas.surface.route_preview == atlas.route)
	atlas._select("RELAY_COURT")
	assert(atlas.track_button.disabled and atlas.detail.text.contains("Vharok"))
	atlas._select("FOREST_ENTRANCE")
	assert(atlas.route[1] == "STREET" and atlas.surface.route_preview[-1] == "FOREST_ENTRANCE")
	_tap(atlas.track_button)
	await process_frame
	assert(not app.paused and app.atlas_destination == "FOREST_ENTRANCE")
	assert(app.atlas_next_place == "STREET")
	app.show_map()
	await process_frame
	atlas = app.ui.get_child(0)
	assert(atlas.surface.destination == "FOREST_ENTRANCE" and atlas.surface.route[-1] == "FOREST_ENTRANCE")
	assert(atlas.detail.text.contains("Weiter nach"))
	_tap(_button("Weltkarte"))
	await process_frame
	assert(atlas.view == "world")
	atlas._center("GREENWOOD")
	await process_frame
	var pin := Vector2.ZERO
	for node in atlas.nodes:
		if node.id == "GREENWOOD":
			pin = atlas.surface.get_global_transform_with_canvas() * Vector2(node.point)
	assert(atlas.scroll.get_global_rect().has_point(pin))
	_tap_pin(pin)
	assert(atlas.selected == "GREENWOOD" and atlas.surface.route_preview[-1] == "GREENWOOD")
	assert(atlas.surface.route[-1] == "FOREST_ENTRANCE")
	_tap(_button("Zurück zur Welt"))

	for door in app.stage.doors:
		if door.to != "STREET":
			continue
		app.actor.position = app.stage.safe_position(door.position, app.actor.collider.shape.radius)
		for frame in 5:
			await physics_frame
		await app.interact_with("door", door.from + ":" + door.to)
		break
	await process_frame
	assert(app.current_place() == "STREET" and app.atlas_next_place == "PARK")
	app.actor.position = Catalog.position_for("FOREST", Vector2(0.5, 0.55))
	await process_frame
	assert(app.atlas_next_place == "FOREST_ENTRANCE")
	for door in app.stage.doors:
		if Routes.door_destination(door) != "FOREST_ENTRANCE":
			continue
		app.actor.position = app.stage.safe_position(door.position, app.actor.collider.shape.radius)
		for frame in 5:
			await physics_frame
		await app.interact_with("door", door.from + ":" + door.to)
		break
	await process_frame
	assert(app.room == "FOREST_TRAIL" and app.atlas_destination.is_empty())
	assert(app.notice.contains("Reiseziel erreicht"))

	app.show_map()
	await process_frame
	atlas = app.ui.get_child(0)
	atlas._select("GREENWOOD")
	_tap(atlas.track_button)
	await process_frame
	app.hud._update_travel_hint()
	assert(app.atlas_next_place == "GREENWOOD" and app.hud.travel_hint.text.begins_with("→"))
	app.actor.position.x = 25
	await process_frame
	assert(app.atlas_destination.is_empty())

	app.show_map()
	await process_frame
	atlas = app.ui.get_child(0)
	atlas._select("MILL_BROOK")
	_tap(atlas.track_button)
	await process_frame
	app.show_map()
	await process_frame
	atlas = app.ui.get_child(0)
	_tap(atlas.clear_button)
	assert(app.atlas_destination.is_empty() and atlas.clear_button.disabled)
	print("Wegeatlas · Kapitelziel, Touch, Grenztor, zwei Durchgänge, Ankunft und Löschen geprüft")
	var save_path: String = app.save_path
	app.queue_free()
	await process_frame
	DirAccess.remove_absolute(save_path)
	quit()

func _button(title: String) -> Button:
	for button in app.menu_buttons:
		if is_instance_valid(button) and button.text == title:
			return button
	return null

func _tap(button: Button) -> void:
	var touch := InputEventScreenTouch.new()
	touch.position = button.get_global_rect().get_center()
	touch.pressed = true
	app._input(touch)

func _tap_pin(position: Vector2) -> void:
	for pressed in [true, false]:
		var touch := InputEventScreenTouch.new()
		touch.position = position
		touch.pressed = pressed
		root.push_input(touch, true)
