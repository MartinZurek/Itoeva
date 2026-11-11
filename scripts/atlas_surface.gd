extends Control

const Layout = preload("res://scripts/world_atlas_layout.gd")
const Scenery = preload("res://scripts/scenery_data.gd")
const Catalog = preload("res://scripts/world_catalog.gd")
const Forest = preload("res://scripts/forest_catalog.gd")
var nodes: Array[Dictionary] = []
var links: Array = []
var selected: = ""
var current: = ""
var visited: Array = []
var rooms: = false
var terrain: Array[Dictionary] = []
var route: Array[String] = []

func _ready() -> void :
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	if rooms: return
	for region in Catalog.data().regions:
		terrain.append({"texture": Scenery.fallback(region.asset), "rect": Rect2(80 + float(region.origin) * 10, 570, float(region.width) * 10, 100)})
	for i in Forest.data().segments.size():
		terrain.append({"texture": Scenery.fallback(Forest.data().segments[i].asset), "rect": Rect2(840 + i * 190, 44, 190, 100)})

func _draw() -> void :
	draw_rect(Rect2(Vector2.ZERO, size), Color("dad5bd"))


	for y in range(30, int(size.y), 52):
		for x in range(30, int(size.x), 66):
			var at: = Vector2(x + sin(float(y) * 0.02) * 13, y)
			draw_circle(at, 1, Color(0.36, 0.39, 0.3, 0.11))
	if not rooms:
		_landscape()
		for image in terrain:
			if image.texture != null: draw_texture_rect(image.texture, image.rect, false, Color(1, 1, 1, 0.34))
	for link in links:
		var a: Dictionary = nodes[link[0]]; var b: Dictionary = nodes[link[1]]
		var line: = Layout.line(a, b)
		var focus: bool = selected in [a.id, b.id]
		var index := route.find(a.id)
		var other := route.find(b.id)
		var on_route := index >= 0 and other >= 0 and absi(index - other) == 1
		if Layout.continuous(a.id, b.id):
			draw_polyline(line, Color("74532e") if on_route else Color("a49a73"), 12, true)
			draw_polyline(line, Color("ebbd6b") if on_route else Color("f2e7c8") if focus else Color("e9ddbc"), 7, true)
		else:
			for i in range(1, line.size()):
				_dotted(line[i - 1], line[i], Color("8b5c25") if on_route else Color("597866") if focus else Color("92957c"), 5 if on_route else 3)
	for entry in nodes:
		var here: bool = entry.id == current
		var seen: bool = entry.id in visited
		var p: Vector2 = entry.point
		draw_circle(p, 10 if here else 7, Color("344e43"))
		draw_circle(p, 6 if here else 4, Color("e9be68") if here else Color("f4ecd6") if seen else Color("c2bea7"))
		if here: draw_arc(p, 15, 0, TAU, 40, Color("466a52"), 2, true)
		elif not route.is_empty() and entry.id == route[-1]:
			draw_arc(p, 13, 0, TAU, 32, Color("8b5c25"), 3, true)

func _dotted(a: Vector2, b: Vector2, ink: Color, width: float) -> void :
	var distance: = a.distance_to(b)
	for t in range(0, int(distance), 13):
		var start: = a.lerp(b, float(t) / maxf(1, distance))
		var end: = a.lerp(b, minf(1, float(t + 5) / maxf(1, distance)))
		draw_line(start, end, ink, width, true)

func _landscape() -> void :
	var font: = ThemeDB.fallback_font
	draw_style_box(_wash(Color("bac2a08c")), Rect2(850, 28, 1410, 200))
	draw_style_box(_wash(Color("c9bd9980")), Rect2(870, 285, 1390, 245))
	draw_style_box(_wash(Color("a7c4bd80")), Rect2(34, 538, 510, 145))
	for x in range(890, 2250, 42): _tree(Vector2(x, 72 + sin(x * 0.09) * 12), Color("61785b73"))
	for x in range(55, 500, 56): draw_arc(Vector2(x, 653), 16, PI * 0.1, PI * 0.9, 12, Color("648f915e"), 2, true)
	for x in range(1540, 2210, 100):
		var p: = Vector2(x, 550)
		draw_colored_polygon(PackedVector2Array([p + Vector2(-30, 20), p + Vector2(0, -30), p + Vector2(35, 20)]), Color("9c9d8080"))
	for row in [["WALDPFAD", Vector2(880, 38)], ["GRENZWEGE", Vector2(900, 302)], ["KÜSTE  ·  DORF  ·  HÖHEN", Vector2(48, 562)]]:
		draw_string(font, row[1], row[0], HORIZONTAL_ALIGNMENT_LEFT, -1, 14, Color("68705b"))

func _tree(p: Vector2, ink: Color) -> void :
	draw_line(p, p + Vector2(0, 13), ink, 2)
	draw_colored_polygon(PackedVector2Array([p + Vector2(-9, 3), p + Vector2(0, -15), p + Vector2(9, 3)]), ink)

func _wash(ink: Color) -> StyleBoxFlat:
	var style: = StyleBoxFlat.new();style.bg_color = ink;style.set_corner_radius_all(40);return style
