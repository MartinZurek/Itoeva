extends Control

const Layout = preload("res://scripts/world_atlas_layout.gd")
var nodes: Array[Dictionary] = []
var links: Array = []
var selected := ""
var current := ""
var rooms := false
var route: Array[String] = []
var route_preview: Array[String] = []
var destination := ""

func _ready() -> void :
	mouse_filter = Control.MOUSE_FILTER_IGNORE

func _draw() -> void :
	draw_rect(Rect2(Vector2.ZERO, size), Color("dad5bd"))


	for y in range(30, int(size.y), 52):
		for x in range(30, int(size.x), 66):
			var at := Vector2(x + sin(float(y) * 0.02) * 13, y)
			draw_circle(at, 1, Color(0.36, 0.39, 0.3, 0.11))
	if not rooms:

		draw_rect(Rect2(Vector2(12, 12), size - Vector2(24, 24)), Color("9a8c6c"), false, 2)
		draw_rect(Rect2(Vector2(19, 19), size - Vector2(38, 38)), Color("b5a687"), false, 1)
		var compass := Vector2(130, 150)
		draw_arc(compass, 40, 0, TAU, 48, Color("86785a"), 1.5, true)
		for d in [Vector2.UP, Vector2.RIGHT, Vector2.DOWN, Vector2.LEFT]:
			var side := Vector2( - d.y, d.x)
			draw_colored_polygon(PackedVector2Array([compass + d * 34, compass + side * 7, compass - d * 9]), Color("647b69"))
		draw_string(ThemeDB.fallback_font, compass + Vector2(-7, -49), "N", HORIZONTAL_ALIGNMENT_LEFT, -1, 18, Color("665b43"))
	for link in links:
		var a: Dictionary = nodes[link[0]]; var b: Dictionary = nodes[link[1]]
		var line := Layout.line(a, b)
		if Layout.continuous(a.id, b.id):
			draw_polyline(line, Color("9e9677"), 9, true)
			draw_polyline(line, Color("efe1bb"), 5, true)
		else:
			for i in range(1, line.size()):
				_dotted(line[i - 1], line[i], Color("7b846f"), 3)
	_draw_route(route_preview, Color("2b5f55"), Color("8cd3bd"))
	_draw_route(route, Color("785120"), Color("f1c36a"))
	for entry in nodes:
		var here: bool = entry.id == current
		var p: Vector2 = entry.point
		var marked: bool = entry.id == destination
		var focused: bool = entry.id == selected
		var ring := Color("286653") if here else Color("946122") if marked else Color("326e64") if focused else Color("4a5f50")
		var center := Color("c7ead4") if here else Color("ffcf74") if marked else Color("a5d8cd") if focused else Color("f4ecd6")
		draw_circle(p, 12 if here or marked or focused else 8, ring, true, -1, true)
		draw_circle(p, 7 if here or marked or focused else 4, center, true, -1, true)
		if here or marked or focused:
			draw_arc(p, 16, 0, TAU, 32, ring, 2, true)

func _draw_route(path: Array[String], outline: Color, ink: Color) -> void :
	for i in range(1, path.size()):
		var from := Layout.index_of(nodes, path[i - 1])
		var to := Layout.index_of(nodes, path[i])
		if from < 0 or to < 0:
			continue
		var line := Layout.line(nodes[from], nodes[to])
		draw_polyline(line, outline, 12, true)
		if Layout.continuous(path[i - 1], path[i]):
			draw_polyline(line, ink, 7, true)
		else:
			for section in range(1, line.size()):
				_dotted(line[section - 1], line[section], ink, 5)

func _dotted(a: Vector2, b: Vector2, ink: Color, width: float) -> void :
	var distance := a.distance_to(b)
	for t in range(0, int(distance), 13):
		var start := a.lerp(b, float(t) / maxf(1, distance))
		var end := a.lerp(b, minf(1, float(t + 5) / maxf(1, distance)))
		draw_line(start, end, ink, width, true)
