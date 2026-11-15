extends Control

const Art = preload("res://scripts/card_assets.gd")
var card: Dictionary
var progress := 0.0
var art: TextureRect
var tint: Color

func _ready() -> void:
	size = Vector2(1280, 720)
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	tint = Art.accent(card)
	art = TextureRect.new()
	art.texture = Art.texture(card)
	art.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
	art.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_CENTERED
	art.position = Vector2(478, 130)
	art.size = Vector2(324, 350)
	art.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(art)
	_text(card.name, Rect2(200, 42, 880, 54), 37, tint.lightened(0.42))
	var label := "BESCHWÖRUNG" if card.kind == "minion" else "SPEZIALKARTE" if card.kind == "special" else "ZAUBER"
	_text(label, Rect2(300, 507, 680, 34), 22, tint.lightened(0.35))
	_text(card.text.replace("\n", " · "), Rect2(235, 554, 810, 76), 20, Color("f2e4c8"))

func _text(value: String, bounds: Rect2, font_size: int, color: Color) -> void:
	var label := Label.new()
	label.text = value
	label.position = bounds.position
	label.size = bounds.size
	label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	label.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
	label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	label.add_theme_font_size_override("font_size", font_size)
	label.add_theme_color_override("font_color", color)
	label.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(label)

func _draw() -> void:
	var opacity := 1.0 - smoothstep(0.83, 1.0, progress)
	var opening := smoothstep(0.0, 0.38, progress)
	draw_rect(Rect2(Vector2.ZERO, size), Color(0.025, 0.044, 0.085, opacity))
	var center := Vector2(640, 305)
	for i in 3:
		var radius := 130.0 + i * 36.0 + opening * 21.0
		draw_arc(center, radius, progress * 1.7 + i * TAU / 3.0, progress * 1.7 + i * TAU / 3.0 + 5.6,
			64, Color(tint, opacity * (0.26 + 0.14 * opening)), 1.5, true)
	for i in 18:
		var angle := i * 2.39996 + progress * 0.8
		var position := center + Vector2(cos(angle), sin(angle)) * (181.0 + i % 5 * 15.0 + opening * 19.0)
		draw_circle(position, 1.2 + float(i % 3), Color(tint.lightened(0.28), opacity * opening * 0.72))
	modulate.a = opacity
	art.modulate.a = opening
