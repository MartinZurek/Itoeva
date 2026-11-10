extends RefCounted

const GOLD: = Color("ffe9a4")
var animals: Texture2D
var data: Dictionary
func _init() -> void :
	data = JSON.parse_string(FileAccess.get_file_as_string("res://data/wildlife_atlas.json"))
	animals = load(data.asset)
func persistent(view, states: Array, time: float) -> void :
	for i in mini(states.size(), 2):
		var foot: Vector2 = view._position(i)
		if int(states[i].get("barrier", 0)) > 0: barrier(view, foot, 1, time, float(states[i].barrier) / 40)
		if int(states[i].get("bind", 0)) > 0: roots(view, foot, 1, time, 0.8)
		if int(states[i].get("silence", 0)) > 0:
			var at: = foot - Vector2(0, 174)
			view.draw_arc(at, 13, 0, TAU, 24, Color("c49fde"), 2, true)
			view.draw_line(at + Vector2(-9, 9), at + Vector2(9, -9), Color("ecd2ff"), 2, true)
func star(view, at: Vector2, r: float, color: Color, rotation: float = 0) -> void :
	if r < 1.0 or color.a <= 0.001: return
	var points: = PackedVector2Array()
	for i in 10:
		var angle: = i * TAU / 10 - PI * 0.5 + rotation
		points.append(at + Vector2(cos(angle), sin(angle)) * (r if i % 2 == 0 else r * 0.4))
	view.draw_colored_polygon(points, color)
func burst(view, at: Vector2, color: Color, alpha: float, radius: float) -> void :
	if alpha <= 0: return
	view.draw_circle(at, radius * 0.5, Color(color, alpha * 0.07))
	view.draw_arc(at, radius, 0, TAU, 52, Color(color, alpha * 0.7), 2, true)
	for i in 18:
		var d: = Vector2(cos(i * TAU / 18), sin(i * TAU / 18))
		view.draw_line(at + d * radius * 0.65, at + d * (radius + 8 + i % 4 * 7), Color(color, alpha * 0.65), 2, true)
func particles(view, at: Vector2, t: float, color: Color, radius: float, count: int = 32) -> void :
	for i in count:
		var angle: = i * 2.399963 + t * 0.4
		var reach: = radius * (0.25 + 0.75 * fposmod(i * 0.618, 1))
		var p: = at + Vector2(cos(angle), sin(angle)) * reach + Vector2(0, - t * 10)
		star(view, p, 2 + i % 3, Color(color, clampf(1 - t * 0.6, 0, 1) * 0.6), t + i)
func barrier(view, foot: Vector2, growth: float, t: float, integrity: float = 1) -> void :
	if growth <= 0.001: return
	var center: = foot - Vector2(0, 65)
	var r: = 89 * growth
	var arc: = PackedVector2Array()
	for j in 49:
		var a: = PI + j * PI / 48
		arc.append(center + Vector2(cos(a) * r, sin(a) * r * 1.25))
	var fill: = arc.duplicate()
	fill.append(foot + Vector2(r, 8));fill.append(foot + Vector2( - r, 8))
	view.draw_colored_polygon(fill, Color(0.45, 0.8, 1, 0.075 * growth))
	view.draw_polyline(arc, Color(0.57, 0.88, 1, 0.2 * growth), 9, true)
	view.draw_polyline(arc, Color(0.98, 0.98, 0.84, 0.75 * growth), 1.5, true)
	for j in 5:
		var a: = PI + (j + 0.5) * PI / 5
		star(view, center + Vector2(cos(a) * r, sin(a) * r * 1.25), 5 + sin(t * 2 + j), Color(GOLD, 0.6 * growth), a)
	view.draw_arc(foot - Vector2(0, 2), r * 0.8, 0, PI, 32, Color(0.55, 0.83, 1, 0.6 * growth), 2, true)
	if integrity < 0.55:
		var crack: = PackedVector2Array([center + Vector2(-22, -78), center + Vector2(-12, -51), center + Vector2(7, -60), center + Vector2(20, -24)])
		view.draw_polyline(crack, Color("d5eaff"), 1.5, true)
func roots(view, foot: Vector2, growth: float, t: float, alpha: float = 1) -> void :
	for i in 4:
		var points: = PackedVector2Array()
		var side: float = -1 if i % 2 == 0 else 1
		for j in 25:
			var p: = j / 24.0 * growth
			points.append(foot + Vector2(side * (49 - p * 26) + sin(p * 13 + t * 0.5 + i) * 16 * p, - p * (105 + i % 2 * 26)))
		view.draw_polyline(points, Color(0.1, 0.07, 0.12, 0.8 * alpha), 9, true)
		view.draw_polyline(points, Color(0.42, 0.29, 0.2, 0.95 * alpha), 5, true)
		view.draw_polyline(points, Color(0.75, 0.51, 0.33, 0.6 * alpha), 1.2, true)
		if growth > 0.7:
			var at: Vector2 = points[17]
			view.draw_colored_polygon(PackedVector2Array([at, at + Vector2(side * 16, -9), at + Vector2(side * 7, 7)]), Color(0.43, 0.53, 0.4, 0.85 * alpha))
	if growth > 0.9: view.draw_arc(foot - Vector2(0, 73), 38, - 0.15, PI + 0.15, 32, Color(0.59, 0.39, 0.53, alpha), 6, true)
func draw(view, t: float, event: Dictionary) -> void :
	var kind: String = event.get("visual", "")
	match kind:
		"wildlife": wildlife(view, t, event)
		"star_beam", "star_nova": beam(view, t, event, kind == "star_nova")
		"star_barrier":
			var source: Vector2 = view._position(int(event.source)) - Vector2(0, 115)
			var alpha: = 1 - smoothstep(1.7, 2.4, t)
			star(view, source, 18 + sin(t * 4) * 4, Color(GOLD, alpha * 0.85), t * 0.5)
			for i in 2:
				if view.model.party[i].hp <= 0: continue
				var foot: Vector2 = view._position(i)
				view.draw_line(source, foot - Vector2(0, 75), Color(GOLD, alpha * 0.4), 3, true)
				barrier(view, foot, smoothstep(0.25, 0.95, t), t)
		"wood_shards": shards(view, t, event)
		"root_bind":
			var source: = Vector2(901, 455)
			for i in 2:
				if view.model.party[i].hp <= 0: continue
				var foot: Vector2 = view._position(i)
				var trail: = PackedVector2Array()
				for j in 40:
					var q: = j / 39.0 * smoothstep(0.4, 1.4, t)
					trail.append(source.lerp(foot, q) + Vector2(sin(q * 24 + i) * 7, - sin(q * PI) * 15))
				view.draw_polyline(trail, Color(0.15, 0.09, 0.15, 0.85), 12, true)
				view.draw_polyline(trail, Color(0.59, 0.37, 0.47, 0.8), 4, true)
				if t > 0.85:
					var blocked: bool = view.model.party[i].barrier > 0 and view.model.party[i].bind == 0
					var growth: = smoothstep(0.85, 1.5, t) * (0.3 if blocked else 1.0)
					roots(view, foot, growth, t, 1 - smoothstep(1.5 if blocked else 2.6, 2.0 if blocked else 3.2, t))
			view.draw_arc(source - Vector2(0, 126), 35 + smoothstep(0, 1.4, t) * 100, 0, TAU, 64, Color(0.67, 0.4, 0.71, 0.45 * (1 - smoothstep(1.4, 2.6, t))), 3, true)
		"root_release":
			var at: Vector2 = view._position(int(event.source))
			roots(view, at, 1, t, 1 - smoothstep(0.4, 1.2, t))
			if t > 0.65: burst(view, at - Vector2(0, 68), Color("c5dbad"), 1 - smoothstep(0.1, 0.85, t - 0.65), 20 + (t - 0.65) * 95)
	var impact: float = event.get("impact", 0.62)
	if t >= impact and t < impact + 0.8:
		for effect in event.get("effects", []):
			if int(effect.target) > 1 or int(effect.target) < 0: continue
			if effect.kind in ["barrier_hit", "barrier_break"]:
				var at: Vector2 = view._position(int(effect.target)) - Vector2(0, 75)
				var p: float = (t - impact) / 0.8
				burst(view, at, Color("90dcec"), 1 - p, 55 + p * 80)
				if effect.kind == "barrier_break":
					for j in 12:
						var center: = at + Vector2(cos(j * TAU / 12), sin(j * TAU / 12)) * p * 125
						view.draw_colored_polygon(PackedVector2Array([center + Vector2(-7, -10), center + Vector2(8, -3), center + Vector2(-2, 12)]), Color(0.65, 0.88, 1, (1 - p) * 0.7))
func beam(view, t: float, event: Dictionary, nova: bool) -> void :
	var source: Vector2 = view._position(int(event.source)) - Vector2(0, 102)
	var target: Vector2 = view._position(2) - Vector2(0, 120)
	var impact: float = event.impact
	var charge: = smoothstep(0, 0.75, t) * (1 - smoothstep(impact + 0.5, impact + 1, t))
	for i in 4: view.draw_arc(source, 24 + i * 12, - t * 2 + i * 0.7, PI * 1.3 - t * 2 + i * 0.7, 36, Color(GOLD, charge * (0.6 - i * 0.1)), 2, true)
	star(view, source, 26 * charge, GOLD, t * 0.9)
	var grow: = smoothstep(impact - 0.4, impact, t)
	var fade: = 1 - smoothstep(impact + 0.55, impact + 1.05, t)
	var direction: Vector2 = (target - source).normalized()
	var normal: = Vector2( - direction.y, direction.x)
	var tip: Vector2 = source.lerp(target, grow) + normal * sin(clampf((t - impact + 0.3) / 1.1, 0, 1) * PI) * 16
	if t > impact - 0.4 and fade > 0:
		var width: float = (48 if nova else 26) * sin(clampf(grow, 0, 1) * PI * 0.5)
		view.draw_line(source, tip, Color(0.71, 0.69, 1, 0.05 * fade), width * 3, true)
		view.draw_line(source, tip, Color(0.95, 0.7, 0.31, 0.16 * fade), width * 1.8, true)
		view.draw_line(source, tip, Color(1, 0.89, 0.52, 0.67 * fade), width, true)
		view.draw_line(source, tip, Color(1, 1, 0.93, 0.96 * fade), maxf(2, width * 0.3), true)
		for i in 7: star(view, source.lerp(tip, fposmod(t * 1.7 + i * 0.14, 1)) + normal * sin(t * 8 + i) * width * 0.4, 3 + i % 3, Color(GOLD, fade * 0.8), t)
	if t >= impact:
		var p: float = t - impact
		burst(view, target, GOLD, 1 - smoothstep(0.1, 1, p), 30 + p * (150 if nova else 95))
		particles(view, target, p, GOLD, 100 if nova else 70, 42)
		if nova: star(view, target, 70 * (1 - smoothstep(0.2, 0.8, p)), Color(1, 0.98, 0.85, 0.5), p * 0.5)
func wildlife(view, t: float, event: Dictionary) -> void :
	var animal: Dictionary = event.get("summon", {})
	if animal.is_empty(): return
	var start: = Vector2(110, 444)
	var target: = Vector2(820, 446)
	var alpha: = smoothstep(0.2, 0.65, t) * (1 - smoothstep(2.85, 3.4, t))
	if t < 0.8: burst(view, start - Vector2(0, 10), Color("b9dba3"), alpha, 50 + t * 30)
	var p: = smoothstep(0.65, 1.86, t)
	var foot: = start.lerp(target, p) - Vector2(0, sin(p * PI) * 55)
	var frame: = int(t * 9) % 2
	if t > 1.65: frame = 2
	if t > 2.12:
		frame = 3;p = smoothstep(2.12, 3.35, t)
		foot = target.lerp(Vector2(1050, 448), p) - Vector2(0, sin(p * PI) * 25)
	var row: = int(animal.row)
	var r: Array = data.regions[row][frame]
	var rect: = Rect2(r[0], r[1], r[2], r[3])
	var width: float = [150.0, 215.0, 250.0, 275.0][row]
	var dim: = rect.size * width / float(data.reference_widths[row])
	view.draw_set_transform(view.margin + (foot + Vector2(0, 3)) * view.fit, 0, Vector2(view.fit, view.fit * 0.22))
	view.draw_circle(Vector2.ZERO, width * 0.32, Color(0, 0, 0, 0.25 * alpha))
	view.draw_set_transform(view.margin, 0, Vector2.ONE * view.fit)
	view.draw_texture_rect_region(animals, Rect2(foot - Vector2(dim.x * 0.5, dim.y), dim), rect, Color(1, 1, 1, alpha))
	if t >= float(event.impact):
		var since: float = t - float(event.impact)
		burst(view, Vector2(894, 352), Color("d6e3b1"), 1 - smoothstep(0.1, 0.8, since), 30 + since * 115)
		particles(view, Vector2(885, 408), since, Color("adbf8b"), 90, 25)
	view._word(str(animal.name).to_upper(), Vector2(65, 192), 22, Color("d5e7bd"))
func shards(view, t: float, event: Dictionary) -> void :
	var source: = Vector2(868, 309)
	var target: = Vector2(300, 371)
	for effect in event.effects:
		if int(effect.target) < 2: target = view._position(int(effect.target)) - Vector2(0, 65);break
	for i in 9:
		var p: = clampf((t - 0.25 - i * 0.045) / 0.64, 0, 1)
		if p <= 0 or t > 1.3 + i * 0.03: continue
		var d: Vector2 = (target - source).normalized()
		var normal: = Vector2( - d.y, d.x)
		var at: = source.lerp(target, p) + normal * sin(p * PI) * (i - 4) * 12
		var tip: = at + d * (18 + i % 3 * 5)
		var base: = at - d * 11
		view.draw_line(base - d * 24, at, Color(0.68, 0.5, 0.32, 0.2), 3, true)
		view.draw_colored_polygon(PackedVector2Array([tip, base + normal * 5, base - normal * 5]), Color("9e7951"))
		view.draw_line(base, tip, Color("dac099"), 1.5, true)
	if t >= float(event.impact):
		var p: float = t - float(event.impact)
		burst(view, target, Color("d4bb83"), 1 - smoothstep(0.1, 0.75, p), 20 + p * 78)
		particles(view, target, p, Color("ae9873"), 75, 26)
