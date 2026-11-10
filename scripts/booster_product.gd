extends Control
const Art = preload("res://scripts/card_assets.gd")
const Cards = preload("res://scripts/card_catalog.gd")
var display: = false
func _ready() -> void :
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	if display:
		_pack(Vector2(26, 15), Vector2(94, 164), - 0.1)
		_pack(Vector2(91, 8), Vector2(94, 164), 0.08)
		_pack(Vector2(61, 28), Vector2(100, 172), 0)
		var front: = Panel.new();front.position = Vector2(10, 156);front.size = Vector2(204, 65);front.mouse_filter = Control.MOUSE_FILTER_IGNORE;add_child(front)
		var box: = StyleBoxFlat.new();box.bg_color = Color("243c32");box.border_color = Color("d2b775");box.set_border_width_all(2);box.set_corner_radius_all(5);front.add_theme_stylebox_override("panel", box)
		_label(front, "ITOEVA", Vector2(10, 5), Vector2(184, 27), 22)
		_label(front, "WEGE · 6 BOOSTER", Vector2(8, 34), Vector2(188, 21), 12)
	else: _pack(Vector2(35, 4), Vector2(154, 219), 0)
func _pack(at: Vector2, dimensions: Vector2, angle: float) -> void :
	var panel: = Panel.new();panel.position = at;panel.size = dimensions;panel.rotation = angle;panel.mouse_filter = Control.MOUSE_FILTER_IGNORE;add_child(panel)
	var box: = StyleBoxFlat.new();box.bg_color = Color("152f2b");box.border_color = Color("dbc68b");box.set_border_width_all(2);box.set_corner_radius_all(4);panel.add_theme_stylebox_override("panel", box)
	var image: = TextureRect.new();image.texture = Art.texture(Cards.get_card("relay_dragon" if display else "trail_keeper"));image.expand_mode = TextureRect.EXPAND_IGNORE_SIZE;image.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_COVERED;image.position = Vector2(7, 37);image.size = dimensions - Vector2(14, 80);image.mouse_filter = Control.MOUSE_FILTER_IGNORE;image.clip_contents = true;panel.add_child(image)
	_label(panel, "ITOEVA", Vector2(5, 7), Vector2(dimensions.x - 10, 26), 19 if not display else 14)
	_label(panel, "WEGE VON ITOEVA", Vector2(5, dimensions.y - 36), Vector2(dimensions.x - 10, 15), 9 if display else 11)
	_label(panel, "5 KARTEN", Vector2(5, dimensions.y - 20), Vector2(dimensions.x - 10, 14), 9)
func _label(parent: Control, text: String, at: Vector2, dimensions: Vector2, font: int) -> void :
	var label: = Label.new();label.text = text;label.position = at;label.size = dimensions;label.add_theme_font_size_override("font_size", font);label.add_theme_color_override("font_color", Color("ecd6a1"));label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER;label.mouse_filter = Control.MOUSE_FILTER_IGNORE;parent.add_child(label)
