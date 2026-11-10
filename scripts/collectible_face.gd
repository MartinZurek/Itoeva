extends Control

const Art = preload("res://scripts/card_assets.gd")
var card: Dictionary = {}
var owned_count: = -1
var fresh: = false
func _ready() -> void :
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	var outer: = Panel.new();outer.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT);outer.mouse_filter = Control.MOUSE_FILTER_IGNORE;add_child(outer)
	var rarity: String = card.get("collect_rarity", "Katalog")
	var rim: = Color({"Gewöhnlich": "a4b9b4", "Ungewöhnlich": "90b8cf", "Selten": "d8b778", "Legendär": "e0a5d9"}.get(rarity, "9caaa6"))
	var box: = StyleBoxFlat.new();box.bg_color = Color("132521");box.border_color = rim;box.set_border_width_all(2);box.set_corner_radius_all(9);outer.add_theme_stylebox_override("panel", box)
	var title: = _label(card.name, Rect2(0.06, 0.025, 0.75, 0.125), 12, Color("f4ead3"));title.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART;title.text_overrun_behavior = TextServer.OVERRUN_TRIM_ELLIPSIS;title.max_lines_visible = 2
	_label(str(card.cost), Rect2(0.84, 0.03, 0.12, 0.1), 22, Color("c1e6eb"))
	var image: = TextureRect.new();image.texture = Art.texture(card);image.expand_mode = TextureRect.EXPAND_IGNORE_SIZE;image.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_COVERED;image.clip_contents = true;image.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(image);_rect(image, Rect2(0.055, 0.16, 0.89, 0.46))
	var type: = _label("Helfer" if card.kind == "minion" else "Zauber", Rect2(0.07, 0.635, 0.68, 0.055), 11, rim)
	var text: = _label(card.text, Rect2(0.075, 0.7, 0.85, 0.17), 12, Color("e9e1cf"));text.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART;text.text_overrun_behavior = TextServer.OVERRUN_TRIM_ELLIPSIS;text.max_lines_visible = 4
	var footer: = rarity + " · " + ("WEG %02d" % int(card.collect_number) if card.has("collect_number") else str(card.id))
	if card.kind == "minion": footer += " · %s/%s" % [card.attack, card.health]
	_label(footer, Rect2(0.07, 0.87, 0.86, 0.055), 10, rim)
	if owned_count >= 0: _label(("NEU · " if fresh else "") + str(owned_count) + "× im Album", Rect2(0.07, 0.935, 0.86, 0.05), 10, Color("f4ce85"))
func _rect(node: Control, rect: Rect2) -> void :
	node.anchor_left = rect.position.x;node.anchor_top = rect.position.y;node.anchor_right = rect.end.x;node.anchor_bottom = rect.end.y;node.offset_left = 0;node.offset_top = 0;node.offset_right = 0;node.offset_bottom = 0
func _label(text: String, rect: Rect2, font: int, color: Color) -> Label:
	var label: = Label.new();label.text = text;label.mouse_filter = Control.MOUSE_FILTER_IGNORE;label.add_theme_font_size_override("font_size", font);label.add_theme_color_override("font_color", color);label.vertical_alignment = VERTICAL_ALIGNMENT_CENTER;add_child(label);_rect(label, rect);return label
