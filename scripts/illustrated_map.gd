extends Control

const Catalog = preload("res://scripts/world_catalog.gd")
const Routes = preload("res://scripts/world_routes.gd")
const Residents = preload("res://scripts/world_residents.gd")
const Layout = preload("res://scripts/world_atlas_layout.gd")
const Surface = preload("res://scripts/atlas_surface.gd")
const Scenery = preload("res://scripts/scenery_data.gd")
const Forest = preload("res://scripts/forest_catalog.gd")
var app: Node3D
var nodes: Array[Dictionary] = []
var links: Array = []
var chips: Array[Button] = []
var selected: = ""
var view: = "world"
var frame: Control
var scroll: ScrollContainer
var surface: Control
var detail: RichTextLabel
var preview: TextureRect
var finger: = -1
var touch_origin: = Vector2.ZERO
var scroll_origin: = Vector2.ZERO
var dragged: = false
var route: Array[String] = []
var track_button: Button
var clear_button: Button

func _ready() -> void :
	mouse_filter = Control.MOUSE_FILTER_STOP
	frame = Control.new();frame.size = Vector2(1280, 720);add_child(frame)
	var title: = Label.new();title.text = "ITOEVA   /   WEGEATLAS";title.position = Vector2(26, 18)
	title.add_theme_font_size_override("font_size", 27);title.add_theme_color_override("font_color", Color("f2dfb9"));frame.add_child(title)
	_button("Zurück zur Welt", Vector2(1052, 14), Vector2(202, 50), app._resume)
	_button("Wege", Vector2(24, 64), Vector2(110, 44), func(): _show_view("world", app.current_place()))
	_button("Räume", Vector2(142, 64), Vector2(110, 44), func(): _show_view("rooms", app.current_place()))
	_button("Hier", Vector2(260, 64), Vector2(100, 44), func(): _show_current())
	_button("Dorf", Vector2(368, 64), Vector2(110, 44), func(): _show_view("world", "STREET"))
	_button("Wald", Vector2(486, 64), Vector2(110, 44), func(): _show_view("world", "GREENWOOD"))
	_button("Grenzwege", Vector2(604, 64), Vector2(144, 44), func(): _show_view("world", "MIST_WATCH"))
	var chapter_button := _button("Kapitelziel", Vector2(756, 64), Vector2(146, 44), _show_chapter_goal)
	chapter_button.disabled = app.Journey.next_step(app.save_data, app.selected).is_empty()
	scroll = ScrollContainer.new();scroll.position = Vector2(24, 120);scroll.size = Vector2(926, 532);frame.add_child(scroll)
	preview = TextureRect.new();preview.position = Vector2(974, 120);preview.size = Vector2(282, 158)
	preview.expand_mode = TextureRect.EXPAND_IGNORE_SIZE;preview.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_COVERED
	preview.mouse_filter = Control.MOUSE_FILTER_IGNORE;frame.add_child(preview)
	detail = RichTextLabel.new();detail.position = Vector2(974, 296);detail.size = Vector2(282, 300)
	detail.bbcode_enabled = true;detail.scroll_active = true;detail.add_theme_font_size_override("normal_font_size", 18)
	detail.add_theme_color_override("default_color", Color("e0dfcc"));frame.add_child(detail)
	track_button = _button("Ziel markieren", Vector2(974, 608), Vector2(152, 44), _track_destination)
	clear_button = _button("Route lösen", Vector2(1134, 608), Vector2(122, 44), _clear_destination)
	track_button.add_theme_font_size_override("font_size", 16)
	clear_button.add_theme_font_size_override("font_size", 16)
	var legend: = Label.new();legend.position = Vector2(26, 670)
	legend.text = "Wischen zum Verschieben   ·   Goldene Route zum Ziel   ·   Gepunktete Linie für Durchgänge"
	legend.add_theme_font_size_override("font_size", 16);legend.add_theme_color_override("font_color", Color("c2c9b8"));frame.add_child(legend)
	resized.connect(_layout);_layout();_show_current()

func _button(title: String, at: Vector2, extent: Vector2, callback: Callable) -> Button:
	var button: = Button.new();button.text = title;button.position = at;button.size = extent
	button.pressed.connect(callback);frame.add_child(button);app.menu_buttons.append(button)
	return button

func _layout() -> void :
	if not is_instance_valid(frame): return
	var factor: = minf(size.x / 1280, size.y / 720)
	frame.scale = Vector2.ONE * factor;frame.position = (size - Vector2(1280, 720) * factor) * 0.5
	queue_redraw()

func _show_current() -> void :
	var current: String = app.current_place()
	var indoors: bool = Catalog.data().interiors.has(current) and not Catalog.is_outdoor(current)
	_show_view("rooms" if indoors else "world", current)

func _show_chapter_goal() -> void:
	var step: Dictionary = app.Journey.next_step(app.save_data, app.selected)
	if not step.is_empty():
		var in_world := Layout.index_of(Layout.nodes("world"), step.goal) >= 0
		_show_view("world" if in_world else "rooms", step.goal)

func _track_destination() -> void:
	if route.size() < 2:
		return
	app.set_atlas_destination(selected)
	app._resume()

func _clear_destination() -> void:
	app.set_atlas_destination("")
	_select(selected)

func _show_view(next: String, focus: String = "") -> void :
	view = next;finger = -1
	if is_instance_valid(surface):
		scroll.remove_child(surface);surface.queue_free()
	nodes = Layout.nodes(view);links = Routes.map_links(nodes);chips.clear()
	surface = Surface.new();surface.rooms = view == "rooms";surface.nodes = nodes;surface.links = links
	surface.current = app.current_place();surface.visited = app.save_data.get("discovered", [])
	surface.custom_minimum_size = Layout.HOUSE_SIZE if view == "rooms" else Layout.WORLD_SIZE
	scroll.add_child(surface)
	for entry in nodes:
		var chip: = Button.new();chip.text = _wrap(Catalog.label_for(entry.id, app.selected, Routes.label(entry.id)))
		chip.size = Vector2(126, 50);chip.position = Vector2(entry.point) + Vector2(-63, 18 if entry.side > 0 else -68)
		chip.add_theme_font_size_override("font_size", 16)
		chip.add_theme_stylebox_override("normal", _chip_style(Color("f2ebd7"), Color("aaa88e")))
		chip.add_theme_stylebox_override("hover", _chip_style(Color("fff7df"), Color("69816c")))
		chip.add_theme_color_override("font_color", Color("30473a"));chip.add_theme_color_override("font_hover_color", Color("263a30"))
		chip.pressed.connect(_select.bind(entry.id));surface.add_child(chip);chips.append(chip)


	if Layout.index_of(nodes, focus) < 0: focus = "STREET"
	_select(focus);call_deferred("_center", focus)

func _chip_style(fill: Color, border: Color) -> StyleBoxFlat:
	var style: = StyleBoxFlat.new();style.bg_color = fill;style.border_color = border;style.set_border_width_all(1);style.set_corner_radius_all(9);return style

func _wrap(title: String) -> String:
	var lines: = PackedStringArray([""])
	for word in title.split(" "):
		if lines[-1].length() + word.length() > 16 and not lines[-1].is_empty(): lines.append("")
		lines[-1] += ("" if lines[-1].is_empty() else " ") + word
	return "\n".join(lines)

func _center(id: String) -> void :
	var index: = Layout.index_of(nodes, id)
	if index < 0: return
	var point: Vector2 = nodes[index].point
	if view == "world" and id in ["SUN_GLADE", "MIST_WATCH", "ASH_ASCENT", "SEAL_COURT", "RELAY_COURT"]: point.y += 70
	scroll.scroll_horizontal = int(point.x - scroll.size.x * 0.5)
	scroll.scroll_vertical = int(point.y - scroll.size.y * 0.5)

func _select(id: String) -> void :
	selected = id
	surface.selected = id
	var current: String = app.current_place()
	route = app.atlas_path(current, id)
	var pinned: String = app.atlas_destination
	surface.route = route if route.size() > 1 or pinned.is_empty() else app.atlas_path(current, pinned)
	surface.queue_redraw()
	track_button.disabled = route.size() < 2
	clear_button.disabled = pinned.is_empty()
	_preview(id)
	var title: = Catalog.label_for(id, app.selected, Routes.label(id))
	var lines: = PackedStringArray(["[font_size=26][color=#efc98b]" + title + "[/color][/font_size]", ""])
	lines.append("[color=#b8d4b6]● Hier bist du[/color]" if id == app.current_place() else "Bereits besucht" if id in app.save_data.get("discovered", []) else "Noch nicht besucht")
	if not pinned.is_empty():
		lines.append("[color=#efc98b]Markiertes Ziel[/color] " + Catalog.label_for(pinned, app.selected, Routes.label(pinned)))
	if route.size() > 1:
		var next_label := Catalog.label_for(route[1], app.selected, Routes.label(route[1]))
		lines.append("\n[color=#efc98b]Nächster Wegpunkt[/color]\n" + next_label)
	elif route.is_empty():
		var blocked: String = app.Story.gate_hint(app.save_data, app.selected) if id == "RELAY_COURT" else ""
		lines.append("\n" + ("Kein begehbarer Weg gefunden." if blocked.is_empty() else blocked))
	lines.append("\n[color=#efc98b]Angrenzende Orte[/color]")
	for edge in Routes.connections():
		if id not in edge: continue
		var other: String = edge[1] if edge[0] == id else edge[0]
		var label: = Catalog.label_for(other, app.selected, Routes.label(other))
		var index: = Layout.index_of(nodes, other)
		var here: = Layout.index_of(nodes, id)
		var direction: = ""
		if Layout.continuous(id, other) and index >= 0 and here >= 0:
			direction = "← " if nodes[index].point.x < nodes[here].point.x else "→ "
		lines.append(direction + label + (" · auf dem Weg" if Layout.continuous(id, other) else " · Durchgang"))
	lines.append("\n[color=#efc98b]Bewohner[/color]")
	var count: = 0
	for resident in app.resident_appointments:
		if resident == app.selected: continue
		if app.resident_appointments[resident].place == id:
			count += 1;lines.append(Residents.data()[resident].name)
	if count == 0: lines.append("Gerade niemand hier.")
	if id == "LIVING": lines.append("\nIm Hausplan findest du Flur und persönliche Räume.")
	if id == "SUN_GLADE": lines.append("\nLinks zum Waldrand, rechts zur Nebelwacht. Der vordere Pfad führt zum Bach, der hintere zwischen den Birken in den Grünen Wald.")
	if id == "MIST_WATCH": lines.append("\nDer bewaldete Seitenpfad kehrt in den Tiefen Wald zurück. Der breite Weg folgt weiter dem Hang.")
	detail.text = "\n".join(lines)
	detail.scroll_to_line(0)
	for i in chips.size():
		var active: bool = nodes[i].id == id
		chips[i].add_theme_stylebox_override("normal", _chip_style(Color("4e6754") if active else Color("f2ebd7"), Color("d5b773") if active else Color("aaa88e")))
		chips[i].add_theme_color_override("font_color", Color("fff2cf") if active else Color("30473a"))

func _preview(id: String) -> void :
	var region: = Catalog.region_for(id)
	if not region.is_empty():
		var texture: = Scenery.fallback(region.asset)
		var atlas: = AtlasTexture.new();atlas.atlas = texture
		var width: float = texture.get_width() / float(region.places.size())
		atlas.region = Rect2(region.places.find(id) * width, 0, width, texture.get_height());preview.texture = atlas
	elif Catalog.data().interiors.has(id):
		preview.texture = load(Catalog.interior_for(id, app.selected).asset)
	else:
		for entry in Forest.data().segments:
			if entry.id == id: preview.texture = Scenery.fallback(entry.asset);return

func _input(event: InputEvent) -> void :
	if not visible or not is_instance_valid(scroll): return
	if event is InputEventScreenTouch:
		if event.pressed and finger < 0 and scroll.get_global_rect().has_point(event.position):
			finger = event.index;touch_origin = event.position;scroll_origin = Vector2(scroll.scroll_horizontal, scroll.scroll_vertical);dragged = false
			get_viewport().set_input_as_handled()
		elif event.index == finger and not event.pressed:
			if not dragged and not event.canceled and scroll.get_global_rect().has_point(event.position):
				for i in chips.size():
					if chips[i].get_global_rect().has_point(event.position): _select(nodes[i].id);break
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
