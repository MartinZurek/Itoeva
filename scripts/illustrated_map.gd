extends Control

const Catalog = preload("res://scripts/world_catalog.gd")
const Routes = preload("res://scripts/world_routes.gd")
const Residents = preload("res://scripts/world_residents.gd")
const Layout = preload("res://scripts/world_atlas_layout.gd")
const Surface = preload("res://scripts/atlas_surface.gd")
const PlaceArt = preload("res://scripts/place_art.gd")
var app: Node3D
var nodes: Array[Dictionary] = []
var links: Array = []
var chips: Array[Button] = []
var images: Dictionary = {}
var known: Array = []
var selected := ""
var view := "world"
var frame: Control
var scroll: ScrollContainer
var surface: Control
var detail: RichTextLabel
var preview: TextureButton
var preview_place := ""
var viewer: ColorRect
var viewer_image: TextureRect
var viewer_title: Label
var viewer_close: Button
var viewer_return_focus: Control
var finger := -1
var touch_origin := Vector2.ZERO
var scroll_origin := Vector2.ZERO
var dragged := false
var route: Array[String] = []
var track_button: Button
var clear_button: Button
var world_tab: Button
var rooms_tab: Button

func _ready() -> void :
	mouse_filter = Control.MOUSE_FILTER_STOP
	frame = Control.new();frame.size = Vector2(1280, 720);add_child(frame)
	var title := Label.new();title.text = "Itoeva · Reiseatlas";title.position = Vector2(26, 18)
	title.add_theme_font_size_override("font_size", 27);title.add_theme_color_override("font_color", Color("f2dfb9"));frame.add_child(title)
	var location := Label.new()
	location.text = "Du bist hier · " + _label_for(app.current_place())
	location.position = Vector2(320, 24)
	location.add_theme_font_size_override("font_size", 19)
	location.add_theme_color_override("font_color", Color("b9dfcc"))
	frame.add_child(location)
	_button("Zurück zur Welt", Vector2(1052, 14), Vector2(202, 50), app._resume)
	world_tab = _button("Weltkarte", Vector2(24, 64), Vector2(110, 44), func(): _show_view("world", app.current_place()))
	rooms_tab = _button("Hausplan", Vector2(142, 64), Vector2(110, 44), func(): _show_view("rooms", app.current_place()))
	_button("Hierher", Vector2(260, 64), Vector2(100, 44), _show_current)
	known = app.discovered_places().duplicate()
	var shortcut_x := 368.0
	for shortcut in [["STREET", "Dorf"], ["GREENWOOD", "Wald"], ["MIST_WATCH", "Grenzwege"]]:
		if shortcut[0] not in known:
			continue
		_button(shortcut[1], Vector2(shortcut_x, 64), Vector2(118, 44), _show_view.bind("world", shortcut[0]))
		shortcut_x += 126
	var step: Dictionary = app.Journey.next_step(app.save_data, app.selected)
	if step.get("goal", "") in known:
		_button("Kapitelziel", Vector2(shortcut_x, 64), Vector2(146, 44), _show_chapter_goal)
	scroll = ScrollContainer.new();scroll.position = Vector2(24, 120);scroll.size = Vector2(926, 532);frame.add_child(scroll)
	preview = _picture_button(Vector2(974, 120), Vector2(282, 158))
	preview.pressed.connect(func(): _open_picture(selected, preview))
	frame.add_child(preview)
	detail = RichTextLabel.new();detail.position = Vector2(974, 296);detail.size = Vector2(282, 300)
	detail.bbcode_enabled = true;detail.scroll_active = true;detail.add_theme_font_size_override("normal_font_size", 18)
	detail.add_theme_color_override("default_color", Color("e0dfcc"));frame.add_child(detail)
	track_button = _button("Route starten", Vector2(974, 608), Vector2(152, 44), _track_destination)
	clear_button = _button("Löschen", Vector2(1134, 608), Vector2(122, 44), _clear_destination)
	track_button.add_theme_font_size_override("font_size", 16)
	clear_button.add_theme_font_size_override("font_size", 16)
	var legend := Label.new();legend.position = Vector2(26, 670)
	legend.text = "Ort antippen: Route ansehen  ·  Bild antippen: vergrößern  ·  Gold: deine Route"
	legend.add_theme_font_size_override("font_size", 16);legend.add_theme_color_override("font_color", Color("c2c9b8"));frame.add_child(legend)
	_build_viewer()
	resized.connect(_layout);_layout();_show_current()

func _button(title: String, at: Vector2, extent: Vector2, callback: Callable) -> Button:
	var button := Button.new();button.text = title;button.position = at;button.size = extent
	button.pressed.connect(callback);frame.add_child(button);app.menu_buttons.append(button)
	return button

func _layout() -> void :
	if not is_instance_valid(frame): return
	var factor := minf(size.x / 1280, size.y / 720)
	frame.scale = Vector2.ONE * factor;frame.position = (size - Vector2(1280, 720) * factor) * 0.5
	queue_redraw()

func _show_current() -> void :
	var current: String = app.current_place()
	var indoors: bool = Catalog.data().interiors.has(current) and not Catalog.is_outdoor(current)
	_show_view("rooms" if indoors else "world", current)

func _show_chapter_goal() -> void :
	var step: Dictionary = app.Journey.next_step(app.save_data, app.selected)
	if step.get("goal", "") in known:
		var in_world := Layout.index_of(Layout.nodes("world"), step.goal) >= 0
		_show_view("world" if in_world else "rooms", step.goal)

func _track_destination() -> void :
	if route.size() < 2:
		return
	app.set_atlas_destination(selected)
	app._resume()

func _clear_destination() -> void :
	app.set_atlas_destination("")
	clear_button.disabled = true
	_select(selected)

func _show_view(next: String, focus: String = "") -> void :
	view = next;finger = -1
	_update_tab(world_tab, view == "world")
	_update_tab(rooms_tab, view == "rooms")
	if is_instance_valid(surface):
		scroll.remove_child(surface);surface.queue_free()
	nodes = Layout.nodes(view).filter(func(entry: Dictionary) -> bool: return entry.id in known)
	links = Routes.map_links(nodes)
	chips.clear()
	images.clear()
	surface = Surface.new();surface.rooms = view == "rooms";surface.nodes = nodes;surface.links = links
	surface.current = app.current_place()
	surface.custom_minimum_size = Layout.HOUSE_SIZE if view == "rooms" else Layout.WORLD_SIZE
	scroll.add_child(surface)
	for entry in nodes:
		var chip := Button.new();chip.text = _wrap(_label_for(entry.id))
		chip.size = Vector2(126, 50);chip.position = Vector2(entry.point) + Vector2(-63, 18 if entry.side > 0 else -68)
		chip.add_theme_font_size_override("font_size", 16)
		chip.add_theme_stylebox_override("normal", _chip_style(Color("f2ebd7"), Color("aaa88e")))
		chip.add_theme_stylebox_override("hover", _chip_style(Color("fff7df"), Color("69816c")))
		chip.add_theme_color_override("font_color", Color("30473a"));chip.add_theme_color_override("font_hover_color", Color("263a30"))
		chip.pressed.connect(_select.bind(entry.id));surface.add_child(chip);chips.append(chip)
		var texture := PlaceArt.thumbnail(entry.id, app.selected, app.atlas_thumbnails)
		if texture != null:
			var image_at := Vector2(entry.point) + Vector2(-48, 76 if entry.side > 0 else -124)
			var picture := _picture_button(image_at, Vector2(96, 54))
			picture.texture_normal = texture
			picture.pressed.connect(_open_picture.bind(entry.id, picture))
			images[entry.id] = picture
			surface.add_child(picture)
	if Layout.index_of(nodes, focus) < 0:
		focus = str(nodes[0].id) if not nodes.is_empty() else ""
	if focus.is_empty():
		selected = ""
		route.clear()
		preview.hide()
		preview.texture_normal = null
		preview_place = ""
		track_button.disabled = true
		clear_button.disabled = app.atlas_destination.is_empty()
		detail.text = "Hier erscheinen Orte, sobald du sie besucht hast."
	else:
		_select(focus)
		call_deferred("_center", focus)

func _chip_style(fill: Color, border: Color) -> StyleBoxFlat:
	var style := StyleBoxFlat.new();style.bg_color = fill;style.border_color = border;style.set_border_width_all(1);style.set_corner_radius_all(9);return style

func _update_tab(tab: Button, active: bool) -> void :
	var fill := Color("31594b") if active else Color("243c35")
	var border := Color("99c2aa") if active else Color("536858")
	tab.add_theme_stylebox_override("normal", _chip_style(fill, border))

func _label_for(id: String) -> String:
	return Catalog.label_for(id, app.selected, Routes.label(id))

func _wrap(title: String) -> String:
	var lines := PackedStringArray([""])
	for word in title.split(" "):
		if lines[-1].length() + word.length() > 16 and not lines[-1].is_empty(): lines.append("")
		lines[-1] += ("" if lines[-1].is_empty() else " ") + word
	return "\n".join(lines)

func _center(id: String) -> void :
	var index := Layout.index_of(nodes, id)
	if index < 0: return
	var point: Vector2 = nodes[index].point
	point.y += 34 if nodes[index].side > 0 else -34
	scroll.scroll_horizontal = int(point.x - scroll.size.x * 0.5)
	scroll.scroll_vertical = int(point.y - scroll.size.y * 0.5)

func _select(id: String) -> void :
	if Layout.index_of(nodes, id) < 0:
		return
	selected = id
	surface.selected = id
	var current: String = app.current_place()
	route = app.atlas_path(current, id, true)
	var pinned: String = app.atlas_destination if app.atlas_destination in known else ""
	var active_route: Array[String] = []
	if not pinned.is_empty():
		active_route = app.atlas_path(current, pinned, true)
	surface.destination = pinned
	surface.route_preview.assign(route if route.size() > 1 and id != pinned else [])
	surface.route = active_route
	surface.queue_redraw()
	track_button.disabled = route.size() < 2 or id == pinned
	clear_button.disabled = app.atlas_destination.is_empty()
	_preview(id)
	var title := _label_for(id)
	var lines := PackedStringArray(["[font_size=26][color=#efc98b]" + title + "[/color][/font_size]", ""])
	lines.append("[color=#b8d4b6]● Hier bist du[/color]" if id == app.current_place() else "Bereits besucht")
	if not pinned.is_empty():
		lines.append("[color=#efc98b]Aktive Route[/color] · " + _label_for(pinned))
	var shown_route := active_route if id == current else route
	if shown_route.size() > 1:
		lines.append("\n[color=#b9dfcc]Weiter nach " + _label_for(shown_route[1]) + "[/color]")
		var count := shown_route.size() - 1
		lines.append("1 Wegabschnitt bis zum Ziel" if count == 1 else "%s Wegabschnitte bis zum Ziel" % count)
	elif route.is_empty():
		lines.append("\nKeine Route über besuchte Orte gefunden.")
	var neighbors := PackedStringArray()
	for edge in Routes.connections():
		if id not in edge: continue
		var other: String = edge[1] if edge[0] == id else edge[0]
		if other not in known:
			continue
		var label := _label_for(other)
		var index := Layout.index_of(nodes, other)
		var here := Layout.index_of(nodes, id)
		var direction := ""
		if Layout.continuous(id, other) and index >= 0 and here >= 0:
			direction = "← " if nodes[index].point.x < nodes[here].point.x else "→ "
		neighbors.append(direction + label + (" · auf dem Weg" if Layout.continuous(id, other) else " · Durchgang"))
	if not neighbors.is_empty():
		lines.append("\n[color=#efc98b]Besuchte Nachbarorte[/color]")
		lines.append_array(neighbors)
	lines.append("\n[color=#efc98b]Bewohner[/color]")
	var count := 0
	for resident in app.resident_appointments:
		if resident == app.selected: continue
		if app.resident_appointments[resident].place == id:
			count += 1;lines.append(Residents.data()[resident].name)
	if id == "RING_WEIR":
		lines.append("Ried · Schleusenwart")
	elif count == 0:
		lines.append("Gerade niemand hier.")

	detail.text = "\n".join(lines)
	detail.scroll_to_line(0)
	for i in chips.size():
		var active: bool = nodes[i].id == id
		chips[i].add_theme_stylebox_override("normal", _chip_style(Color("4e6754") if active else Color("f2ebd7"), Color("d5b773") if active else Color("aaa88e")))
		chips[i].add_theme_color_override("font_color", Color("fff2cf") if active else Color("30473a"))

func _preview(id: String) -> void:
	if id != preview_place:
		preview.texture_normal = PlaceArt.picture(id, app.selected)
		preview_place = id
	preview.visible = preview.texture_normal != null

func _picture_button(at: Vector2, extent: Vector2) -> TextureButton:
	var button := TextureButton.new()
	button.position = at
	button.size = extent
	button.ignore_texture_size = true
	button.stretch_mode = TextureButton.STRETCH_KEEP_ASPECT_CENTERED
	button.mouse_default_cursor_shape = Control.CURSOR_POINTING_HAND
	button.tooltip_text = "Bild vergrößern"
	return button

func _build_viewer() -> void:
	viewer = ColorRect.new()
	viewer.color = Color("121d1af5")
	viewer.size = frame.size
	viewer.hide()
	frame.add_child(viewer)
	viewer_title = Label.new()
	viewer_title.position = Vector2(38, 23)
	viewer_title.add_theme_font_size_override("font_size", 27)
	viewer.add_child(viewer_title)
	viewer_image = TextureRect.new()
	viewer_image.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
	viewer_image.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_CENTERED
	viewer_image.mouse_filter = Control.MOUSE_FILTER_IGNORE
	viewer.add_child(viewer_image)
	viewer_close = Button.new()
	viewer_close.text = "Schließen"
	viewer_close.position = Vector2(1080, 18)
	viewer_close.size = Vector2(162, 46)
	viewer_close.pressed.connect(_close_picture)
	viewer.add_child(viewer_close)
	var own_path := viewer_close.get_path()
	viewer_close.focus_next = own_path
	viewer_close.focus_previous = own_path
	for side in [SIDE_LEFT, SIDE_TOP, SIDE_RIGHT, SIDE_BOTTOM]:
		viewer_close.set_focus_neighbor(side, own_path)

func _open_picture(id: String, source: Control) -> void:
	if not images.has(id):
		return
	_select(id)
	viewer_return_focus = source
	viewer_title.text = _label_for(id)
	viewer_image.texture = preview.texture_normal
	var source_size := viewer_image.texture.get_size()
	var bounds := Vector2(1184, 604)
	var factor := minf(1.25, minf(bounds.x / source_size.x, bounds.y / source_size.y))
	viewer_image.size = source_size * factor
	viewer_image.position = Vector2(48, 78) + (bounds - viewer_image.size) * 0.5
	viewer.show()
	viewer_close.grab_focus()

func _close_picture() -> void:
	viewer.hide()
	viewer_image.texture = null
	if is_instance_valid(viewer_return_focus):
		viewer_return_focus.grab_focus()

func _select_at(screen_position: Vector2) -> void :
	for i in chips.size():
		if chips[i].get_global_rect().has_point(screen_position):
			_select(nodes[i].id)
			return
	for id in images:
		if images[id].get_global_rect().has_point(screen_position):
			_open_picture(id, images[id])
			return
	var point := surface.get_global_transform_with_canvas().affine_inverse() * screen_position
	var closest := ""
	var distance_squared := 24.0 * 24.0
	for entry in nodes:
		var distance: float = point.distance_squared_to(entry.point)
		if distance < distance_squared:
			closest = entry.id
			distance_squared = distance
	if not closest.is_empty():
		_select(closest)

func _input(event: InputEvent) -> void :
	if not visible or not is_instance_valid(scroll):
		return
	if viewer.visible:
		if event.is_action_pressed("menu"):
			_close_picture()
			get_viewport().set_input_as_handled()
		elif event is InputEventScreenTouch or event is InputEventScreenDrag:
			if event is InputEventScreenTouch and event.pressed and not event.canceled and viewer_close.get_global_rect().has_point(event.position):
				_close_picture()
			get_viewport().set_input_as_handled()
		return
	if event is InputEventScreenTouch:
		if event.pressed and preview.visible and preview.get_global_rect().has_point(event.position):
			_open_picture(selected, preview)
			get_viewport().set_input_as_handled()
			return
		if event.pressed and finger < 0 and scroll.get_global_rect().has_point(event.position):
			finger = event.index;touch_origin = event.position;scroll_origin = Vector2(scroll.scroll_horizontal, scroll.scroll_vertical);dragged = false
			get_viewport().set_input_as_handled()
		elif event.index == finger and not event.pressed:
			if not dragged and not event.canceled and scroll.get_global_rect().has_point(event.position):
				_select_at(event.position)
			finger = -1;get_viewport().set_input_as_handled()
	elif event is InputEventScreenDrag and event.index == finger:
		var delta: Vector2 = event.position - touch_origin
		if delta.length() > 8: dragged = true
		if dragged:
			scroll.scroll_horizontal = int(scroll_origin.x - delta.x / frame.scale.x)
			scroll.scroll_vertical = int(scroll_origin.y - delta.y / frame.scale.y)
		get_viewport().set_input_as_handled()

func _draw() -> void :
	draw_rect(Rect2(Vector2.ZERO, size), Color("172b27"))
