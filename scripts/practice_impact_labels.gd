extends Node2D

const FX = preload("res://scripts/practice_card_fx.gd")
var view: Control

func _draw() -> void:
	if not view.model.playback_pending or not FX.has_impact_labels(view.event):
		return
	var elapsed: float = view.event_time - view.model.impact_time()
	if elapsed < 0 or elapsed > 0.96:
		return
	var alpha := 1.0 - smoothstep(0.42, 0.94, elapsed)
	var font := ThemeDB.fallback_font
	var labels := FX.impact_labels(view.event)
	draw_set_transform(view.margin, 0, Vector2.ONE * view.fit)
	for code in labels:
		var notes: Array = labels[code]
		var point: Vector2 = view._unit_position(int(code.substr(2))) - Vector2(0, 40) if code.begins_with("u:") else view._target_point(code)
		var x := clampf(point.x, 90, 1190)
		var y := maxf(175, point.y - 50 - elapsed * 28.0)
		for index in mini(3, notes.size()):
			var note: Array = notes[index]
			var caption: String = note[0]
			var color: Color = note[1]
			var width := font.get_string_size(caption, HORIZONTAL_ALIGNMENT_LEFT, -1, 21).x
			var origin := Vector2(x - width * 0.5, y + index * 29.0)
			draw_rect(Rect2(origin - Vector2(10, 21), Vector2(width + 20, 27)), Color(0.025, 0.041, 0.07, alpha * 0.9))
			draw_string(font, origin, caption, HORIZONTAL_ALIGNMENT_LEFT, -1, 21, Color(color, alpha))
