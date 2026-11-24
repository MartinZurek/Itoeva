extends SceneTree

const Catalog = preload("res://scripts/world_catalog.gd")
const Routes = preload("res://scripts/world_routes.gd")
const Layout = preload("res://scripts/world_atlas_layout.gd")
const PlaceArt = preload("res://scripts/place_art.gd")
const TravelCard = preload("res://scripts/travel_card.gd")
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
	assert(atlas.nodes.size() == 1 and atlas.selected == "CRAFT")
	assert(atlas.images.size() == 1 and atlas.images.has("CRAFT"))
	assert(atlas.links.is_empty() and _button("Kapitelziel") == null)
	assert(not atlas.detail.text.contains("Dorf"))
	_tap_pin(atlas.preview.get_global_rect().get_center())
	assert(atlas.viewer.visible and atlas.viewer_image.size.x > atlas.preview.size.x)
	var native_size: Vector2 = atlas.viewer_image.size * atlas.viewer_image.get_screen_transform().get_scale()
	assert(native_size.x <= atlas.viewer_image.texture.get_width() + 0.01)
	var menu := InputEventAction.new()
	menu.action = "menu"
	menu.pressed = true
	app._input(menu)
	assert(not atlas.viewer.visible and app.paused)
	assert(atlas.preview.has_focus())
	_click_picture(atlas.preview.get_global_rect().get_center())
	assert(atlas.viewer.visible)
	_click_picture(atlas.viewer_close.get_global_rect().get_center())
	assert(not atlas.viewer.visible and app.paused)
	_tap(_button("Weltkarte"))
	assert(atlas.nodes.size() == 1 and atlas.selected == "CRAFT")
	assert(not atlas.images.has("GREENWOOD"))
	_tap(_button("Zurück zur Welt"))
	for id in ["STREET", "PARK", "MEADOW", "FOREST", "FOREST_ENTRANCE", "VILLAGE_EDGE", "MILL_BROOK"]:
		app._discover_place(id)
	var stored: Dictionary = JSON.parse_string(FileAccess.get_file_as_string(app.save_path))
	assert("FOREST_ENTRANCE" in stored.discovered and "GREENWOOD" not in stored.discovered)
	app.show_map()
	await process_frame
	atlas = app.ui.get_child(0)
	_tap(_button("Kapitelziel"))
	assert(atlas.selected == "PARK" and atlas.route[0] == "CRAFT")
	assert(atlas.surface.route.is_empty() and atlas.surface.route_preview == atlas.route)
	atlas._select("RELAY_COURT")
	assert(atlas.selected == "PARK" and not atlas.images.has("RELAY_COURT"))
	assert(not atlas.detail.text.contains("Vharok"))
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
	atlas._center("FOREST_ENTRANCE")
	await process_frame
	var pin := Vector2.ZERO
	for node in atlas.nodes:
		if node.id == "FOREST_ENTRANCE":
			pin = atlas.surface.get_global_transform_with_canvas() * Vector2(node.point)
	assert(atlas.scroll.get_global_rect().has_point(pin))
	_tap_pin(pin)
	assert(atlas.selected == "FOREST_ENTRANCE" and atlas.surface.route[-1] == "FOREST_ENTRANCE")
	_tap_pin(atlas.images["FOREST_ENTRANCE"].get_global_rect().get_center())
	assert(atlas.viewer.visible and atlas.viewer_image.texture.get_width() > 64)
	_tap_pin(atlas.viewer_close.get_global_rect().get_center())
	assert(not atlas.viewer.visible)
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
	assert(not atlas.images.has("GREENWOOD"))
	_tap(_button("Zurück zur Welt"))
	app.actor.position.x = 25
	await process_frame
	assert("GREENWOOD" in app.discovered_places())

	app.show_map()
	await process_frame
	atlas = app.ui.get_child(0)
	assert(atlas.selected == "GREENWOOD" and atlas.images.has("GREENWOOD"))
	atlas._select("FOREST_ENTRANCE")
	_tap(atlas.track_button)
	await process_frame
	app.hud._update_travel_hint()
	assert(app.atlas_next_place == "FOREST_ENTRANCE" and app.hud.travel_hint.text.begins_with("←"))
	app.show_map()
	await process_frame
	atlas = app.ui.get_child(0)
	_tap(atlas.clear_button)
	assert(app.atlas_destination.is_empty() and atlas.clear_button.disabled)

	for view in ["world", "rooms"]:
		for node in Layout.nodes(view):
			var picture := PlaceArt.picture(node.id, app.selected)
			assert(picture != null and picture.get_width() >= 480, node.id)
	assert(PlaceArt.picture("FOREST_ENTRANCE", app.selected, 0, true).get_width() == 1672)
	assert(PlaceArt.picture("PARK", app.selected, 0, true).get_width() == 2048)
	for id in PlaceArt.ILLUSTRATED:
		var first := PlaceArt.picture(id, app.selected)
		var second := PlaceArt.picture(id, app.selected, 1)
		assert(first != second and first.get_width() == 1672 and second.get_width() == 1672)
	for view in ["world", "rooms"]:
		for node in Layout.nodes(view):
			app._discover_place(node.id)
	app.show_map()
	await process_frame
	atlas = app.ui.get_child(0)
	for view in ["world", "rooms"]:
		atlas._show_view(view)
		assert(atlas.images.size() == atlas.nodes.size())
		var targets: Array[Control] = []
		targets.assign(atlas.chips)
		targets.append_array(atlas.images.values())
		for i in targets.size():
			for j in range(i + 1, targets.size()):
				assert(not targets[i].get_rect().intersects(targets[j].get_rect()), "Atlasziele überlappen")

	# Bewegungsrichtung und Kartenrichtung dürfen sich im gefalteten Wald unterscheiden.
	assert(Routes.walking_direction("FOREST_ENTRANCE", "GREENWOOD") == "→")
	var gate := Layout.nodes("world").filter(func(entry: Dictionary) -> bool: return entry.id in ["RING_WEIR", "MILL_RUN"])
	var passable := func(a: String, b: String) -> bool: return app.atlas_passage_hint(a, b).is_empty()
	assert(Routes.map_links(gate, passable).is_empty())
	app.Story.mark(app.save_data, app.selected, "ring_weir")
	assert(Routes.map_links(gate, passable).size() == 1)

	var park := PlaceArt.picture("PARK", app.selected) as AtlasTexture
	assert(park.region == Rect2(512, 0, 512, 682))
	var thumb := PlaceArt.thumbnail("PARK", app.selected, {})
	assert(thumb.get_height() == 108 and thumb.get_width() == 81)
	var card := TravelCard.new()
	card.place = "PARK"
	root.add_child(card)
	assert(card.picture.get_size() == park.get_size())
	card.free()
	var high_dpi := PlaceArt.fitted_extent(park, Vector2(1184, 604), Vector2(2, 2))
	assert(high_dpi == park.get_size() * 0.5)

	app._resume()
	await app._mount_world("MILL_BROOK", Catalog.position_for("MILL_BROOK", Vector2(0.52, 0.62)))
	app._resume()
	assert(app.atlas_path("MILL_BROOK", "PLAINS") == ["MILL_BROOK", "ALDER_FORD", "PLAINS"])
	for destination in ["ALDER_FORD", "PLAINS", "ALDER_FORD", "MILL_BROOK"]:
		for frame in 8:
			await physics_frame
		if app.current_place() == "PLAINS":
			app.request_walk(Catalog.position_for("PLAINS", Vector2(0.76, 0.72)))
			for frame in 360:
				await physics_frame
				if app.pending_interaction.is_empty():
					break
			assert(app.actor.position.x > Catalog.position_for("PLAINS", Vector2(0.6, 0.5)).x)
		await _walk_to(destination)
		assert(app.current_place() == destination, "Fußweg nach " + destination)

	print("Wegeatlas · Entdeckung, Spielstand, Ortsbilder, Touch-Zoom, Routen und Ladebilder geprüft")
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


func _click_picture(position: Vector2) -> void:
	var move := InputEventMouseMotion.new()
	move.position = position
	root.push_input(move, true)
	for pressed in [true, false]:
		var click := InputEventMouseButton.new()
		click.button_index = MOUSE_BUTTON_LEFT
		click.position = position
		click.pressed = pressed
		root.push_input(click, true)


func _walk_to(destination: String) -> void:
	for door in app.stage.doors:
		if door.from != app.current_place() or Routes.door_destination(door) != destination:
			continue
		app.request_interaction("door", door.from + ":" + door.to)
		for frame in 900:
			await physics_frame
			if app.switching or app.current_place() == destination:
				break
		var deadline := Time.get_ticks_msec() + 5000
		while app.switching and Time.get_ticks_msec() < deadline:
			await process_frame
		return
