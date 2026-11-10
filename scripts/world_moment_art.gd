extends RefCounted
const WAYLIGHT_AMBER: = Color("ffda83")


static func draw(hud: Control, point: Vector3, kind: String, settled: bool, time: float) -> void :
	var app = hud.app
	if point.distance_to(app.actor.position) > 12 or app.camera.is_position_behind(point): return
	var at: Vector2 = app.camera.unproject_position(point)

	var bottom_margin: = 110.0 if kind == "cafe_light" else 220.0
	if not Rect2(Vector2(24, 90), hud.size - Vector2(48, bottom_margin)).has_point(at): return
	var unit: float = app.camera.unproject_position(point + app.camera.global_basis.x * 0.19).distance_to(at)
	var tint: Color = app.stage.actor_scene_color(point) if app.stage.has_method("actor_scene_color") else Color.WHITE
	if kind == "tea_steam":
		for strand in 2:
			var age: = fposmod(time / 2.2 + strand * 0.48, 1.0)
			var opacity: = sin(age * PI) * smoothstep(0.0, 0.65, time)
			var ribbon: = PackedVector2Array()
			for i in 10:
				var t: = float(i) / 9.0
				var sway: = sin(t * TAU + age * TAU + strand * PI) * 0.18
				ribbon.append(at + Vector2((sway + t * 0.29 - strand * 0.25) * unit, - (age * 1.15 + t * 0.95) * unit))
			hud.draw_polyline(ribbon, Color(0.97, 0.86, 0.68, 0.52 * opacity), maxf(1.3, unit * 0.08), true)
		return
	if kind == "cafe_light":
		var pulse: = 0.65 + 0.35 * sin(time * TAU / 2.4)
		var warmth: = Color(WAYLIGHT_AMBER, 0.1 * pulse)
		hud.draw_circle(at, unit * 1.75, warmth)
		for i in 2:
			var angle: = time * 1.5 + i * PI
			var glint: = at + Vector2(cos(angle) * unit * 1.55, sin(angle) * unit * 0.85)
			hud.draw_circle(glint, unit * 0.16, Color(WAYLIGHT_AMBER, 0.75 * pulse), true, -1, true)
		return
	var shadow: = PackedVector2Array()
	for i in 16: shadow.append(at + Vector2(cos(i * TAU / 16) * unit * 1.2, sin(i * TAU / 16) * unit * 0.24 + unit * 0.65))
	hud.draw_colored_polygon(shadow, Color(0.05, 0.06, 0.04, 0.28))
	if kind == "route_lantern":
		var glass: = at - Vector2(0, unit * 0.64)
		var flame: = 0.86 + 0.14 * sin(time * 3.2)
		hud.draw_circle(glass, unit * 1.35, Color(WAYLIGHT_AMBER, 0.11 * flame))
		hud.draw_rect(Rect2(glass - Vector2(unit * 0.35, unit * 0.38), Vector2(unit * 0.7, unit * 0.76)), Color("bd874b") * tint)
		hud.draw_circle(glass, unit * 0.18, Color(WAYLIGHT_AMBER, flame))
		hud.draw_line(at - Vector2(unit * 0.48, unit * 0.23), at + Vector2(unit * 0.48, - unit * 0.23), Color("74553b") * tint, 2, true)
		hud.draw_arc(glass - Vector2(0, unit * 0.35), unit * 0.23, PI, TAU, 9, Color("8c673d") * tint, 1.5, true)
		return
	if kind == "beetle":
		var glow: = Color("f5d888");glow.a = 0.1
		hud.draw_circle(at, unit * 1.4, glow)
		var flutter: = sin(time * TAU * 3.2) * 0.25 + 0.75
		for side in [-1, 1]:
			hud.draw_colored_polygon(PackedVector2Array([at, at + Vector2(side * unit * 0.85, - unit * 0.5 * flutter), at + Vector2(side * unit * 0.7, unit * 0.22)]), Color(0.72, 0.85, 0.66, 0.55))
		hud.draw_circle(at, unit * 0.28, Color("c9b56a"))
		hud.draw_circle(at + Vector2(0, unit * 0.17), unit * 0.19, Color("ffeb9b"))
	elif kind == "leaf":
		if settled: hud.draw_line(at + Vector2(0, unit * 0.9), at - Vector2(0, unit * 0.7), Color("897451") * tint, 2, true)
		var bend: = sin(time * 1.9) * unit * 0.1
		hud.draw_colored_polygon(PackedVector2Array([at + Vector2( - unit, 0), at + Vector2( - unit * 0.25, - unit * 0.6), at + Vector2(unit + bend, 0), at + Vector2(unit * 0.2, unit * 0.52)]), Color("b0bd72") * tint)
		hud.draw_line(at - Vector2(unit * 0.8, 0), at + Vector2(unit * 0.8, bend), Color("64784e") * tint, 1.2, true)
	elif kind == "bread":
		if settled:
			hud.draw_colored_polygon(PackedVector2Array([at + Vector2( - unit * 1.3, 0), at + Vector2(unit * 1.3, 0), at + Vector2(unit, 0.9 * unit), at + Vector2( - unit, 0.9 * unit)]), Color("876142") * tint)
		for loaf in (3 if settled else 1):
			var center: = at + Vector2((loaf - 1) * unit * 0.65, - unit * 0.28) if settled else at
			var shape: = PackedVector2Array()
			for i in 16: shape.append(center + Vector2(cos(i * TAU / 16) * unit * 0.8, sin(i * TAU / 16) * unit * 0.52))
			hud.draw_colored_polygon(shape, Color("b87e43") * tint)
			hud.draw_arc(center, unit * 0.55, PI * 1.05, PI * 1.9, 12, Color("edc47c") * tint, 2, true)
			for i in [-1, 1]: hud.draw_line(center + Vector2(i * unit * 0.24, - unit * 0.2), center + Vector2(i * unit * 0.24 + unit * 0.15, unit * 0.1), Color("f0d393") * tint, 1.3, true)
	else:
		var paper: = PackedVector2Array([at + Vector2( - unit, - unit * 0.7), at + Vector2(unit * 0.8, - unit * 0.9), at + Vector2(unit, unit * 0.6), at + Vector2( - unit * 0.8, unit * 0.9)])
		hud.draw_colored_polygon(paper, Color("e0d2a0") * tint)
		hud.draw_line(at - Vector2(unit * 0.5, 0), at + Vector2(unit * 0.5, - unit * 0.1), Color("777a56") * tint, 1.3, true)
