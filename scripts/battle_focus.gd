extends Node2D
const Preview = preload("res://scripts/battle_preview.gd")
const Feedback = preload("res://scripts/battle_feedback.gd")
const Impact = preload("res://scripts/battle_impact.gd")
var view: Control
var focused: Dictionary = {}
var forecasts: Dictionary = {}
var warnings: Dictionary = {}
var state_key: String = ""
var intent_key := ""
var intent_result: Dictionary = {}
var simulations: = 0
var badge_boxes: Dictionary = {}

func _ready() -> void :
	for heal in [false, true]:
		var tint: = Color("aee8c7") if heal else Color("ffd5bc")
		var box: = StyleBoxFlat.new();box.bg_color = Color(0.025, 0.045, 0.065, 0.96);box.border_color = Color(tint, 0.65);box.set_border_width_all(1);box.set_corner_radius_all(5)
		badge_boxes[heal] = box

func update_state() -> void :
	var m = view.model
	var key: String = str([m.turn, m.cursor, m.round, m.phase, m.playback_pending, view.selected_card, view.selected_unit])
	if key == state_key: return
	state_key = key;focused.clear();forecasts.clear();warnings.clear()
	queue_redraw()
	if view.results_shown or m.phase != "battle": return
	if m.playback_pending:
		var source: = int(m.last_event.get("source", -1))
		if source >= 0: focused["enemy" if source == 2 else "h:" + str(source)] = true
		elif m.last_event.has("attacker_uid"): focused["u:" + str(m.last_event.attacker_uid)] = true
		for effect in m.last_event.get("effects", []): focused[Impact.code(effect)] = true
		return
	var choices: = Feedback.targets(m, view.selected_card, view.selected_unit)
	if not choices.is_empty():
		focused["u:" + str(view.selected_unit) if view.selected_unit >= 0 else "h:" + str(m.cursor)] = true
		var working = m.get_script().new()
		for code in choices:
			focused[code] = true
			var result: = Preview.evaluate(m, "unit" if view.selected_unit >= 0 else "card", view.selected_unit if view.selected_unit >= 0 else view.selected_card, code, working)
			simulations += 1
			var values: Dictionary = result.get("outcomes", {}).get(code, {})
			forecasts[code] = {"values": values, "label": Preview.label(values), "counter": result.get("outcomes", {}).get("u:" + str(view.selected_unit), {})}
	else:
		var result := intent_preview()
		for code in result.get("outcomes", {}):
			var values: Dictionary = result.outcomes[code]
			if int(values.get("hp", 0)) < 0 or int(values.get("bind", 0)) > 0 or int(values.get("silence", 0)) > 0 or int(values.get("frozen", 0)) > 0 or int(values.get("shield", 0)) < 0 or int(values.get("barrier", 0)) < 0:
				if code != "enemy": warnings[code] = true

func intent_preview() -> Dictionary:
	var model = view.model
	var key := str([model.turn, model.cursor, model.round, model.phase, model.playback_pending])
	if key != intent_key:
		intent_key = key
		intent_result = Preview.evaluate(model, "intent")
		simulations += 1
	return intent_result

func summary() -> String:
	var labels: Array[String] = []
	var minimum: = 999; var maximum: = 0; var falls: = false
	for forecast in forecasts.values():
		if not str(forecast.label).is_empty() and str(forecast.label) not in labels: labels.append(str(forecast.label))
		var damage: = maxi(0, - int(forecast.counter.get("hp", 0)))
		minimum = mini(minimum, damage);maximum = maxi(maximum, damage)
		falls = falls or forecast.counter.get("defeated", false)
	var text: String = " / ".join(labels) if labels.size() <= 2 else "Wirkung je Ziel"
	if view.selected_unit >= 0 and maximum > 0:
		text += " · Gegentreffer " + (str(maximum) if minimum == maximum else "%s–%s" % [minimum, maximum]) + " LP"
		if falls: text += " · Eigener Helfer kann fallen"
	return text

func light(value: Dictionary, code: String) -> Dictionary:
	if not focused.is_empty():
		if focused.has(code): value.key_gain = minf(0.27, float(value.key_gain) + 0.09)
		else:
			var ambient: Color = value.ambient
			value.ambient = Color(ambient.r * 0.92, ambient.g * 0.92, ambient.b * 0.92, ambient.a)
	if value.pulse_gain > 0: value.pulse_gain *= lerpf(0.55, 1.0, Impact.weight(view.event, code))
	return value

func sync_draw() -> void :


	position = view.margin;scale = Vector2.ONE * view.fit
	visible = not view.results_shown and not is_instance_valid(view.card_popup)

func _ground(code: String) -> Vector2:
	if code == "enemy": return view._position(2)
	if code.begins_with("h:"): return view._position(int(code.substr(2)))
	return view._unit_position(int(code.substr(2)))

func _draw() -> void :
	if not is_instance_valid(view) or view.results_shown or is_instance_valid(view.card_popup): return
	var amber: = Color("e9b279")
	for code in warnings:
		var ground: = _ground(code)
		var marker: = PackedVector2Array([ground + Vector2(-38, 6), ground + Vector2(38, 6), ground + Vector2(38, 9), ground + Vector2(6, 9), ground + Vector2(0, 20), ground + Vector2(-6, 9), ground + Vector2(-38, 9)])
		draw_colored_polygon(marker, Color(amber, 0.65))
	for code in forecasts:
		var text: String = forecasts[code].label
		if text.is_empty(): continue
		var values: Dictionary = forecasts[code].values
		var tint: = Color("aee8c7") if int(values.get("hp", 0)) >= 0 else Color("ffd5bc")
		var ground: = _ground(code)
		var width: = 112.0 if code.begins_with("u:") else 180.0
		var size: = 13 if text.length() > 14 else 15
		var rect: = Rect2(ground + Vector2( - width * 0.5, 14), Vector2(width, 24))
		draw_style_box(badge_boxes[int(values.get("hp", 0)) >= 0], rect)


		var visible_text: = text if text.length() < 24 else text.left(22) + "…"
		draw_string(ThemeDB.fallback_font, rect.position + Vector2(5, 17), visible_text, HORIZONTAL_ALIGNMENT_LEFT, width - 10, size, tint)
