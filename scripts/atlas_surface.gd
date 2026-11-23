extends Control

const Layout = preload("res://scripts/world_atlas_layout.gd")
const Scenery = preload("res://scripts/scenery_data.gd")
const Catalog = preload("res://scripts/world_catalog.gd")
const Forest = preload("res://scripts/forest_catalog.gd")
var nodes: Array[Dictionary] = []
var links: Array = []
var selected := ""
var current := ""
var visited: Array = []
var rooms := false
var terrain: Array[Dictionary] = []
var route: Array[String] = []
var route_preview: Array[String] = []
var destination := ""
var landmarks: Array[Dictionary] = []

func _ready() -> void :
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	if rooms: return
	var art = preload("res://scripts/travel_card.gd")
	for entry in nodes:
		var row: int = art.PLACES.find(entry.id)
		if row < 0: continue
		var tile := AtlasTexture.new()
		tile.atlas = art.ART
		var cell := Vector2(art.ART.get_width() / 2.0, art.ART.get_height() / 7.0)
		tile.region = Rect2(Vector2(2, row * cell.y + 2), cell - Vector2(4, 4))
		landmarks.append({"texture": tile, "at": entry.point})

func _draw() -> void :
	draw_rect(Rect2(Vector2.ZERO, size), Color("dad5bd"))


	for y in range(30, int(size.y), 52):
		for x in range(30, int(size.x), 66):
			var at := Vector2(x + sin(float(y) * 0.02) * 13, y)
			draw_circle(at, 1, Color(0.36, 0.39, 0.3, 0.11))
	if not rooms:
		_landscape()

		for row in range(12):
			var contour := PackedVector2Array()
			for x in range(20, int(size.x) - 20, 18):
				contour.append(Vector2(x, 190 + row * 36 + sin(x * 0.008 + row * 0.8) * 22))
			draw_polyline(contour, Color(0.43, 0.49, 0.37, 0.075), 1, true)
		for item in landmarks:
			var rect := Rect2(item.at + Vector2(-68, -82), Vector2(136, 58))
			draw_rect(rect.grow(4), Color("8d8264"))
			draw_texture_rect(item.texture, rect, false, Color(1, 1, 1, 0.94))
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
		var seen: bool = entry.id in visited
		var p: Vector2 = entry.point
		var marked: bool = entry.id == destination
		var focused: bool = entry.id == selected
		var ring := Color("286653") if here else Color("946122") if marked else Color("326e64") if focused else Color("4a5f50")
		var center := Color("c7ead4") if here else Color("ffcf74") if marked else Color("a5d8cd") if focused else Color("f4ecd6") if seen else Color("c2bea7")
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

func _landscape() -> void :
	var font := ThemeDB.fallback_font
	draw_style_box(_wash(Color("bac2a08c")), Rect2(850, 28, 1410, 200))
	draw_style_box(_wash(Color("c9bd9980")), Rect2(870, 285, 1390, 245))
	draw_style_box(_wash(Color("a7c4bd80")), Rect2(34, 538, 510, 145))
	for x in range(890, 2250, 42): _tree(Vector2(x, 72 + sin(x * 0.09) * 12), Color("61785b73"))
	for x in range(55, 500, 56): draw_arc(Vector2(x, 653), 16, PI * 0.1, PI * 0.9, 12, Color("648f915e"), 2, true)
	for x in range(1540, 2210, 100):
		var p := Vector2(x, 550)
		draw_colored_polygon(PackedVector2Array([p + Vector2(-30, 20), p + Vector2(0, -30), p + Vector2(35, 20)]), Color("9c9d8080"))
	for row in [["WALDPFAD", Vector2(880, 38)], ["GRENZWEGE", Vector2(900, 302)], ["KÜSTE  ·  DORF  ·  HÖHEN", Vector2(48, 562)]]:
		draw_string(font, row[1], row[0], HORIZONTAL_ALIGNMENT_LEFT, -1, 14, Color("68705b"))

func _tree(p: Vector2, ink: Color) -> void :
	draw_line(p, p + Vector2(0, 13), ink, 2)
	draw_colored_polygon(PackedVector2Array([p + Vector2(-9, 3), p + Vector2(0, -15), p + Vector2(9, 3)]), ink)

func _wash(ink: Color) -> StyleBoxFlat:
	var style := StyleBoxFlat.new();style.bg_color = ink;style.set_corner_radius_all(40);return style
