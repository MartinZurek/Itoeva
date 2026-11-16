extends Node2D

const FX = preload("res://scripts/practice_card_fx.gd")
var view: Control

func _draw() -> void:
	if not view.model.playback_pending:
		return
	var elapsed: float = view.event_time - view.model.impact_time()
	draw_set_transform(view.margin, 0, Vector2.ONE * view.fit)
	if view.event.has("wolf_attack") and elapsed >= -0.04 and elapsed <= 0.43:
		_draw_claws(view._wolf_impact_point() + Vector2(8, 13), elapsed, view.event.wolf_attack == "leap")
	if not FX.has_impact_labels(view.event):
		return
	if elapsed < 0 or elapsed > 0.96:
		return
	var alpha := 1.0 - smoothstep(0.42, 0.94, elapsed)
	var font := ThemeDB.fallback_font
	var labels := FX.impact_labels(view.event)
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

func _draw_claws(point: Vector2, elapsed: float, leaping: bool) -> void:
	var reveal := smoothstep(-0.04, 0.17, elapsed)
	var fade := 1.0 - smoothstep(0.13, 0.43, elapsed)
	var size := 1.2 if leaping else 0.9
	for i in 3:
		var origin := point + Vector2(i * 16.0, i * 8.0)
		var trail := PackedVector2Array()
		for step in range(9):
			var progress := step / 8.0 * reveal
			trail.append(origin + Vector2(34.0 - progress * 72.0, -48.0 + progress * 96.0 - sin(progress * PI) * 12.0) * size)
		draw_polyline(trail, Color(1.0, 0.34, 0.13, fade * 0.65), 9.0, true)
		draw_polyline(trail, Color(1.0, 0.84, 0.55, fade), 3.0, true)
