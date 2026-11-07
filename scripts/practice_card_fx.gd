extends RefCounted

const ELEMENTS = {
	"fire": Color("ffad70"), "water": Color("89e0f4"), "wind": Color("b6ebdb"),
	"nature": Color("b6dc83"), "guard": Color("91c8ff"), "heal": Color("86e6b7"),
	"moon": Color("d2adff"), "light": Color("ffe3a6"), "shadow": Color("dba3ce")
}

static func palette(card: Dictionary, fallback: Color) -> Color:
	if card.get("kind", "") == "minion":
		return fallback
	return ELEMENTS.get(card.get("element", ""), fallback)

static func spell(view, event: Dictionary, card: Dictionary) -> void:
	var at: float = view.event_time
	var impact: float = event.impact
	var element: String = event.get("element", "light")
	var tint: Color = ELEMENTS.get(element, Color("ffe3a6"))
	var source: Vector2 = view._position(int(event.source)) - Vector2(0, 70)
	var targets := effect_targets(event, str(card.kind))
	if targets.is_empty():
		targets.append("h:0")
	for code in targets:
		var target: Vector2 = view._target_point(code)
		var outcome := _outcome(event, code, str(card.kind))
		if at < impact:
			_travel(view, source, target, smoothstep(0.0, impact, at), tint)
		else:
			_pulse(view, target, (at - impact) / 0.53, tint, element, outcome)

static func effect_targets(event: Dictionary, kind: String) -> Array[String]:
	var targets: Array[String] = []
	for effect in event.get("effects", []):
		if not effect is Dictionary:
			continue
		var code := _effect_code(effect)
		if not code.is_empty() and code not in targets:
			targets.append(code)
	if targets.is_empty() and kind in ["damage", "freeze", "buff", "mend", "recall"]:
		targets.append(event.get("target_code", "enemy"))
	return targets

static func _effect_code(effect: Dictionary) -> String:
	match int(effect.get("target", -1)):
		0: return "h:0"
		1: return "h:1"
		2: return "enemy"
		3: return "u:" + str(effect.unit)
	return ""

static func _outcome(event: Dictionary, code: String, fallback: String) -> String:
	for effect in event.get("effects", []):
		if _effect_code(effect) != code:
			continue
		match str(effect.kind):
			"heal", "buff": return effect.kind
			"barrier", "shield", "barrier_hit", "barrier_break": return "guard" if code.begins_with("h:") else fallback
	return fallback

static func has_impact_labels(event: Dictionary) -> bool:
	return event.has("cinematic_card") or (event.get("visual", "") == "card_spell" and event.has("card"))

static func impact_labels(event: Dictionary) -> Dictionary:
	var labels := {}
	for effect in event.get("effects", []):
		var code := _effect_code(effect)
		if code.is_empty():
			continue
		var value := int(effect.get("amount", 0))
		var note: Array = []
		match effect.kind:
			"damage", "burn", "blocked":
				if value > 0: note = ["−%s LP" % value, Color("ffd4ac")]
			"shield", "barrier_hit", "barrier_break":
				if value > 0: note = ["−%s Schild" % value, Color("b0d9ff")]
			"barrier":
				if value > 0: note = ["+%s Schutz" % value, Color("b0d9ff")]
			"heal":
				if value > 0: note = ["+%s LP" % value, Color("acf0c4")]
			"evade": note = ["Ausgewichen", Color("d8c8ff")]
			"buff": note = ["Gestärkt", Color("acf0c4")]
		if not note.is_empty():
			if not labels.has(code): labels[code] = []
			labels[code].append(note)
	if event.has("cinematic_card"):
		var summoned: Array = event.get("summoned_uids", []).duplicate()
		if event.has("unit_uid"): summoned.append(event.unit_uid)
		for uid in summoned:
			if int(uid) >= 0:
				labels["u:" + str(uid)] = [["Beschworen", Color("ffe4ab")]]
	return labels

static func cinematic_impact(view, event: Dictionary) -> void:
	var progress: float = (view.event_time - float(event.impact)) / 0.72
	if progress < 0 or progress >= 1:
		return
	var element: String = event.get("element", "light")
	var tint: Color = ELEMENTS.get(element, Color("ffe3a6"))
	for code in effect_targets(event, ""):
		var kind := _outcome(event, code, "damage")
		_pulse(view, view._target_point(code), progress, tint, element, kind, 1.32)
	var summoned: Array = event.get("summoned_uids", []).duplicate()
	if event.has("unit_uid"):
		summoned.append(event.unit_uid)
	for uid in summoned:
		if int(uid) >= 0:
			_pulse(view, view._unit_position(int(uid)) - Vector2(0, 24), progress, tint, element, "summon", 1.32)

static func _travel(view, source: Vector2, target: Vector2, phase: float, tint: Color) -> void:
	var trail := PackedVector2Array()
	for index in 12:
		var p := maxf(0.0, phase - (11 - index) * 0.024)
		trail.append(source.lerp(target, p) - Vector2(0, sin(p * PI) * 46))
	view.draw_polyline(trail, Color(tint, 0.24), 12.0, true)
	view.draw_polyline(trail, Color(tint, 0.8), 3.0, true)
	view.draw_circle(trail[-1], 6, tint.lightened(0.4), true)

static func _pulse(view, target: Vector2, p: float, tint: Color, element: String, kind: String, strength := 1.0) -> void:
	p = clampf(p, 0.0, 1.0)
	var fade := 1.0 - smoothstep(0.12, 1.0, p)
	var radius := lerpf(12, 56, sqrt(p)) * strength
	if kind in ["heal", "party_heal", "mend", "draw", "energy", "buff", "team_buff", "guard", "barrier", "summon"]:
		var base := target + Vector2(0, 25)
		var orbit := PackedVector2Array()
		for step in 33:
			var angle := step * TAU / 32.0
			orbit.append(base + Vector2(cos(angle), sin(angle) * 0.34) * radius)
		view.draw_polyline(orbit, Color(tint, fade * 0.82), 2.5, true)
		for spark in 7:
			var angle := spark * TAU / 7.0 + p * 1.4
			var pos := base + Vector2(cos(angle) * radius, sin(angle) * radius * 0.25 - p * 68)
			view.draw_circle(pos, 2.2, Color(tint.lightened(0.2), fade), true)
	else:
		var icy := element == "water" or kind == "freeze"
		for spark in 9:
			var angle := spark * TAU / 9.0 + 0.2
			var direction := Vector2.from_angle(angle)
			var outer := target + direction * radius
			var inner := target + direction * (radius * 0.40)
			if icy:
				var side := direction.orthogonal() * (4.0 * strength)
				view.draw_colored_polygon(PackedVector2Array([inner + side, outer, inner - side]), Color(tint, fade * 0.85))
			else:
				view.draw_line(inner, outer, Color(tint, fade * 0.8), 2.4, true)
		view.draw_arc(target, radius * 0.7, 0, TAU, 32, Color(tint.lightened(0.25), fade * 0.62), 2.0, true)
