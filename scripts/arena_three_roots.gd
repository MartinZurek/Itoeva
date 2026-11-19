extends RefCounted

const BANK_LIGHT := Color("b1dca0")
const EMBER := Color("f5af65")
const MIST := Color("9db7b1")


static func draw_atmosphere(view: Control, clock: float, bank: Vector2, bank_ready: bool) -> void:
	# Stable trajectories keep each mote continuous across frames.
	for i in 16:
		var warm := i >= 8
		var origin := Vector2(957, 403) if warm else Vector2(386, 375)
		var cycle := fposmod(clock * (0.15 + 0.01 * (i % 3)) + i * 0.618, 1.0)
		var drift := sin(clock * 0.52 + i * 2.17)
		var point := origin + Vector2(float((i * 83) % 218) - 109.0 + drift * 15.0, -cycle * 104.0)
		var fade := sin(cycle * PI)
		var tint := EMBER if warm else BANK_LIGHT
		view.draw_circle(point, 4.0, Color(tint, 0.07 * fade), true, -1.0, true)
		view.draw_circle(point, 1.0 + 0.45 * fade, Color(tint, 0.62 * fade), true, -1.0, true)
	for i in 3:
		var drift := sin(clock * 0.25 + i * 1.7) * 47.0
		view._ellipse(Vector2(462 + i * 141 + drift, 342 + i * 19), Vector2(122, 5 + i * 2), Color(MIST, 0.024))
	if bank_ready:
		var pulse := 0.57 + sin(clock * 2.1) * 0.15
		for i in 7:
			var offset := Vector2(-36 + 12 * i, 5 + sin(i * 0.95) * 4.0)
			view.draw_circle(bank + offset, 1.6, Color(BANK_LIGHT, pulse), true, -1.0, true)


static func draw_wolf_trail(view: Control, home: Vector2, target: Vector2, clock: float) -> void:
	var direction := signf(target.x - home.x)
	var start := home + Vector2(direction * 83, 5)
	var finish := target - Vector2(direction * 49, -7)
	if absf(finish.x - start.x) < 22.0:
		return
	# The arc follows the fighting surface and ends before the target's feet.
	var control := (start + finish) * 0.5 + Vector2(0, 33)
	for i in 13:
		var progress := float(i) / 13.0
		var point := start.lerp(control, progress).lerp(control.lerp(finish, progress), progress)
		var light := 0.34 + 0.29 * sin(progress * 11.0 - clock * 5.0)
		view.draw_circle(point, 2.0, Color(EMBER, light), true, -1.0, true)
		view.draw_circle(point, 5.0, Color(EMBER, light * 0.14), true, -1.0, true)
	var mark := PackedVector2Array([finish + Vector2(-direction * 11, -5), finish,
		finish + Vector2(-direction * 11, 5)])
	view.draw_polyline(mark, Color(EMBER, 0.80), 1.6, true)
