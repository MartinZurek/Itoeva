extends RefCounted

const Cards = preload("res://scripts/card_catalog.gd")
const Art = preload("res://scripts/card_assets.gd")

const ELEMENTS = {
	"fire": Color("ffad70"), "water": Color("89e0f4"), "wind": Color("b6ebdb"),
	"nature": Color("b6dc83"), "guard": Color("91c8ff"), "heal": Color("86e6b7"),
	"moon": Color("d2adff"), "light": Color("ffe3a6"), "shadow": Color("dba3ce"),
	"freeze": Color("b6edff"), "shield_hit": Color("91c8ff"), "weaken": Color("d2adff"),
	"cleanse": Color("c7f9dd"), "energy": Color("ffda78")
}

static func palette(card: Dictionary, fallback: Color) -> Color:
	if card.get("kind", "") == "minion":
		return fallback
	return ELEMENTS.get(card.get("element", ""), fallback)

static func cost_tier(cost: int) -> int:
	if cost >= 6: return 3
	if cost >= 4: return 2
	if cost >= 2: return 1
	return 0

static func draw_card(view, event: Dictionary, card: Dictionary) -> void:
	var at: float = view.event_time
	var impact: float = event.impact
	var tier := cost_tier(int(card.get("cost", 0)))
	var tint := palette(card, Art.accent(card))
	if at >= impact:
		_draw_impacts(view, event, card, tier, tint)
		return

	var start := maxf(float(event.get("preview_cut", 0.0)), impact - 0.58)
	if at <= start: return
	var phase := smoothstep(start, impact, at)
	var source: Vector2 = view._position(int(event.source)) - Vector2(0, 70)
	var targets := impact_motifs(event, card)
	if tier >= 1:
		_charge(view, source, phase, tier, tint)
	for code in targets:
		var kind: String = targets[code][0]
		var target := _anchor(view, code, kind)
		if kind == "summon":
			_portal(view, target, phase, tint, tier)
		elif source.distance_to(target) > 34 and tier < 2:
			_travel(view, source, target, phase, _motif_tint(kind, tint), tier)
	if tier >= 2:
		var lead := cinematic_windup_target(event)
		if not lead.is_empty():
			_travel(view, source, view._target_point(lead), phase, tint, tier)
	elif tier == 0 and targets.size() == 1:
		var code: String = targets.keys()[0]
		if source.distance_to(_anchor(view, code, targets[code][0])) <= 34:
			_charge(view, source, phase, tier, tint)

static func impact_motifs(event: Dictionary, card: Dictionary) -> Dictionary:
	var targets := {}
	var element: String = event.get("element", "light")
	for effect in event.get("effects", []) + event.get("card_cues", []):
		var code := _effect_code(effect)
		if code.is_empty(): continue
		var kind: String = effect.kind
		match kind:
			"damage", "weak", "blocked", "burn": kind = element
			"barrier": kind = "guard"
			"barrier_hit", "barrier_break", "shield": kind = "shield_hit"
			"buff":
				kind = "guard" if int(card.get("shield", 0)) > 0 and int(card.get("value", 0)) == 0 else "buff"
		_add_motif(targets, code, kind)
	for uid in _summoned(event):
		_add_motif(targets, "u:" + str(uid), "summon")
	if targets.is_empty():
		var kind: String = card.get("kind", "")
		var source := "enemy" if int(event.source) == 2 else "h:" + str(event.source)
		var code: String = event.get("target_code", source) if kind in ["damage", "freeze", "mend", "buff", "recall", "weaken"] else source
		_add_motif(targets, code, element if kind == "damage" else "cast")
	return targets

static func _add_motif(targets: Dictionary, code: String, kind: String) -> void:
	if not targets.has(code): targets[code] = []
	if kind not in targets[code]: targets[code].append(kind)

static func _summoned(event: Dictionary) -> Array:
	var summoned: Array = event.get("summoned_uids", []).duplicate()
	var uid := int(event.get("unit_uid", -1))
	if uid >= 0 and uid not in summoned: summoned.append(uid)
	return summoned.filter(func(entry): return int(entry) >= 0)

static func _anchor(view, code: String, kind: String) -> Vector2:
	if kind == "summon":
		return view._unit_position(int(code.substr(2))) - Vector2(0, 4)
	var point: Vector2 = view._target_point(code)
	if kind in ["draw", "reclaim", "energy"]:
		point += Vector2(-48 if code == "enemy" else 48, -42)
	return point

static func _motif_tint(kind: String, tint: Color) -> Color:
	match kind:
		"guard", "shield_hit", "freeze", "heal", "weaken", "cleanse", "energy":
			return ELEMENTS.get(kind, Color("f3dc95")).lerp(tint, 0.2)
	return tint

static func _draw_impacts(view, event: Dictionary, card: Dictionary, tier: int, tint: Color) -> void:
	var window: float = minf(float(event.duration) - float(event.impact), 0.39 + 0.13 * tier)
	var progress: float = (view.event_time - float(event.impact)) / maxf(0.01, window)
	if progress >= 1: return
	var targets := impact_motifs(event, card)
	for code in targets:
		var motifs: Array = targets[code]
		for index in mini(motifs.size(), 3):
			var kind: String = motifs[index]
			var p := (progress - index * 0.11) / (1.0 - index * 0.11)
			if p < 0: continue
			var strength := 1.0 if index == 0 or kind == "summon" else 0.65
			_pulse(view, _anchor(view, code, kind), p, _motif_tint(kind, tint), kind, tier, strength)

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

static func has_impact_labels(event: Dictionary) -> bool:
	return event.has("wolf_attack") or event.has("cinematic_card") or event.has("card")

static func impact_labels(event: Dictionary) -> Dictionary:
	var labels := {}
	for effect in event.get("effects", []) + event.get("card_cues", []):
		var code := _effect_code(effect)
		if code.is_empty():
			continue
		var value := int(effect.get("amount", 0))
		var note: Array = []
		match effect.kind:
			"damage", "weak", "burn", "blocked":
				if value > 0: note = ["−%s LP" % value, Color("ffd4ac")]
			"shield", "barrier_hit", "barrier_break":
				if value > 0: note = ["−%s Schild" % value, Color("b0d9ff")]
			"barrier":
				if value > 0: note = ["+%s Schutz" % value, Color("b0d9ff")]
			"heal":
				if value > 0: note = ["+%s LP" % value, Color("acf0c4")]
			"evade": note = ["Ausgewichen", Color("d8c8ff")]
			"buff": note = ["Gestärkt", Color("acf0c4")]
			"freeze": note = ["Eingefroren", Color("b0d9ff")]
			"cleanse": note = ["Befreit", Color("acf0c4")]
			"weaken": note = ["Geschwächt", Color("d8c8ff")]
			"draw": note = ["+%d %s" % [value, "Karte" if value == 1 else "Karten"], Color("ffe4ab")]
			"energy": note = ["+%d %s" % [value, "Kristall" if value == 1 else "Kristalle"], Color("ffe4ab")]
			"recall", "reclaim": note = ["Zurück auf die Hand", Color("ffe4ab")]
		if not note.is_empty():
			if not labels.has(code): labels[code] = []
			labels[code].append(note)
	for uid in _summoned(event):
		var code := "u:" + str(uid)
		if not labels.has(code): labels[code] = []
		labels[code].append(["Beschworen", Color("ffe4ab")])
	return labels

static func cinematic_windup_target(event: Dictionary) -> String:
	var selected: String = event.get("target_code", "")
	var focus := ""
	for effect in event.get("effects", []):
		if effect.kind not in ["damage", "weak", "blocked", "burn", "shield", "barrier_hit", "barrier_break", "evade"]:
			continue
		var code := _effect_code(effect)
		if code.is_empty(): continue
		if code == selected: return code
		# A sweeping attack uses one cue toward the leader, with impacts on all targets.
		if focus.is_empty() or code == "enemy": focus = code
	return focus

static func cinematic_impact(view, event: Dictionary) -> void:
	var card := Cards.get_card(event.cinematic_card)
	var tier := cost_tier(int(card.get("cost", 0)))
	var element: String = event.get("element", "light")
	var tint: Color = ELEMENTS.get(element, Art.accent(card))
	if view.event_time >= float(event.impact):
		_draw_impacts(view, event, card, tier, tint)
	elif event.get("cinematic_short", false):
		var windup := clampf(view.event_time / float(event.impact), 0.0, 1.0)
		var source: Vector2 = view._position(int(event.source)) - Vector2(0, 54)
		var target := cinematic_windup_target(event)
		var release := 0.42 if not target.is_empty() else 1.0
		if windup >= release and not target.is_empty():
			_travel(view, source, view._target_point(target), smoothstep(release, 1.0, windup), tint, tier)
		else:
			_charge(view, source, windup / release, tier, tint)

static func _charge(view, source: Vector2, phase: float, tier: int, tint: Color) -> void:
	for layer in tier + 1:
		var radius := lerpf(48.0, 14.0, phase) + layer * 10.0
		var angle := phase * PI * (1 if layer % 2 == 0 else -1) + layer
		view.draw_arc(source, radius, angle, angle + TAU * 0.73, 28, Color(tint, phase * (0.75 - layer * 0.15)), 1.5 + tier * 0.4, true)

static func _travel(view, source: Vector2, target: Vector2, phase: float, tint: Color, tier: int) -> void:
	var trail := PackedVector2Array()
	for index in 12:
		var p := maxf(0.0, phase - (11 - index) * (0.019 + tier * 0.005))
		trail.append(source.lerp(target, p) - Vector2(0, sin(p * PI) * (36 + tier * 8)))
	view.draw_polyline(trail, Color(tint, 0.18), 8.0 + tier * 4.0, true)
	view.draw_polyline(trail, Color(tint, 0.84), 2.0 + tier * 0.8, true)
	view.draw_circle(trail[-1], 4.0 + tier * 1.5, tint.lightened(0.4), true)

static func _portal(view, center: Vector2, phase: float, tint: Color, tier: int) -> void:
	var radius := (15.0 + 5.0 * tier) * (0.35 + 0.65 * phase)
	_ground_ring(view, center, radius, Color(tint, phase * 0.85), 2.0)
	if tier >= 1:
		_ground_ring(view, center, radius * 1.55, Color(tint, phase * 0.55), 2.0)
	for index in tier * 2:
		var angle := index * TAU / (tier * 2) + phase
		var foot := center + Vector2(cos(angle), sin(angle) * 0.30) * radius * 1.55
		var tip := foot - Vector2(0, phase * (18 + tier * 12))
		view.draw_line(foot, tip, Color(tint, phase * 0.5), 2, true)
		_diamond(view, tip, Vector2(2.5, 4), Color(tint.lightened(0.3), phase))

static func _ground_ring(view, center: Vector2, radius: float, tint: Color, width: float) -> void:
	var points := PackedVector2Array()
	for index in 33:
		var direction := Vector2.from_angle(index * TAU / 32.0)
		points.append(center + direction * Vector2(radius, radius * 0.30))
	view.draw_polyline(points, tint, width, true)

static func _diamond(view, at: Vector2, size: Vector2, tint: Color) -> void:
	view.draw_colored_polygon(PackedVector2Array([
		at - Vector2(0, size.y), at + Vector2(size.x, 0),
		at + Vector2(0, size.y), at - Vector2(size.x, 0)
	]), tint)

static func _pulse(view, target: Vector2, p: float, tint: Color, kind: String, tier: int, strength: float) -> void:
	var fade := 1.0 - smoothstep(0.12, 1.0, p)
	var radius := lerpf(10, 33 + tier * 13, sqrt(p)) * strength
	var ink := Color(tint, fade * 0.84)
	var count := 4 + tier * 3
	match kind:
		"summon":
			_ground_ring(view, target, radius * 1.25, ink, 2.0 + tier * 0.35)
			for index in count:
				var direction := Vector2.from_angle(index * TAU / count + p * 0.8)
				var foot := target + direction * Vector2(radius, radius * 0.3)
				var lift := (index % 3 + 1) * 7 + p * (18 + tier * 8)
				var tip := foot - Vector2(0, lift)
				view.draw_line(foot, tip, Color(tint, fade * 0.25), 2.0, true)
				_diamond(view, tip, Vector2(2.4, 4.0 + tier), ink)
		"guard", "shield_hit":
			var start := PI if kind == "guard" else p * 0.85
			var sweep := PI if kind == "guard" else TAU * 0.81
			view.draw_arc(target + Vector2(0, 7), radius, start, start + sweep, 28, Color(tint, fade * 0.24), 8.0, true)
			view.draw_arc(target + Vector2(0, 7), radius, start, start + sweep, 28, ink, 2.6, true)
			for index in count:
				var angle := start + sweep * index / count
				_diamond(view, target + Vector2(0, 7) + Vector2.from_angle(angle) * radius, Vector2(2.3, 3.4), ink)
		"draw", "reclaim", "recall", "energy":
			var fan := 2 + tier
			for index in fan:
				var offset := index - (fan - 1) * 0.5
				var drift := 1.0 - p if kind == "recall" else p
				var pos := target + Vector2(offset * radius * 0.52, -drift * (30 - absf(offset) * 7))
				if kind == "energy":
					_diamond(view, pos, Vector2(4, 8), ink)
					view.draw_line(pos - Vector2(0, 7), pos + Vector2(0, 6), Color(tint.lightened(0.5), fade), 1.3, true)
				else:
					view.draw_rect(Rect2(pos - Vector2(4, 7), Vector2(8, 14)), Color("171d28", fade * 0.85))
					view.draw_rect(Rect2(pos - Vector2(4, 7), Vector2(8, 14)), ink, false, 1.4)
					_diamond(view, pos, Vector2(1.4, 2.5), ink)
		"heal", "cleanse", "buff", "weaken", "evade", "cast":
			var drift := p * (28 + tier * 9)
			if kind == "weaken": drift = -drift
			_ground_ring(view, target + Vector2(0, 17), radius, ink, 2.0)
			for index in count:
				var direction := Vector2.from_angle(index * TAU / count + p * 1.3)
				var pos := target + direction * Vector2(radius, radius * 0.32) - Vector2(0, drift)
				if kind == "heal":
					view.draw_line(pos - Vector2(4, 0), pos + Vector2(4, 0), ink, 1.7, true)
					view.draw_line(pos - Vector2(0, 4), pos + Vector2(0, 4), ink, 1.7, true)
				else:
					_diamond(view, pos, Vector2(2.5, 4), ink)
		_:
			for index in count + 2:
				var direction := Vector2.from_angle(index * TAU / (count + 2) + 0.18)
				var outer := target + direction * radius
				var inner := target + direction * radius * 0.42
				var side := direction.orthogonal() * (2.1 + tier)
				match kind:
					"freeze":
						view.draw_colored_polygon(PackedVector2Array([inner + side, outer, inner - side]), ink)
					"water":
						var angle := direction.angle()
						view.draw_arc(target, radius, angle, angle + 0.56, 8, ink, 2.0, true)
						view.draw_circle(outer, 2.2 + tier * 0.7, ink, true)
					"nature":
						var mid := inner.lerp(outer, 0.5)
						view.draw_colored_polygon(PackedVector2Array([inner, mid + side, outer, mid - side]), ink)
					"fire":
						view.draw_polyline(PackedVector2Array([inner, inner.lerp(outer, 0.5) + side, outer - Vector2(0, 5)]), ink, 2.0 + tier, true)
					"moon", "wind", "shadow":
						var angle := direction.angle() + p * 0.95
						view.draw_arc(target, radius, angle, angle + 0.44, 8, ink, 2.0 + tier * 0.4, true)
					_:
						view.draw_line(inner, outer, ink, 2.0 + tier * 0.5, true)
			view.draw_arc(target, radius * 0.64, 0, TAU, 28, Color(tint.lightened(0.3), fade * 0.55), 1.6, true)
	if tier >= 2:
		var center := target + Vector2(0, -15 if kind == "summon" else 0)
		var glow := tint.lightened(0.42)
		view.draw_circle(center, radius * 0.75, Color(glow, fade * 0.10 * strength), true)
		for layer in tier - 1:
			var angle := p * (2.1 if layer == 0 else -1.6)
			var halo := radius * (1.08 + layer * 0.27)
			view.draw_arc(center, halo, angle, angle + TAU * 0.86, 36, Color(glow, fade * 0.55), 1.5, true)
			for index in 4 + layer * 2:
				var at := center + Vector2.from_angle(angle + index * TAU / (4 + layer * 2)) * halo
				_diamond(view, at, Vector2(2.0 + layer, 3.5 + layer), Color(glow, fade * 0.78))
