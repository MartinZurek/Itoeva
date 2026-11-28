extends RefCounted

const Art = preload("res://scripts/world_moment_art.gd")

static func draw(hud: Control) -> void:
	var app = hud.app
	var clock: float = app.game_clock
	var extra: Dictionary = app.resident_light
	if not extra.is_empty() and extra.until > clock:
		Art.draw(hud, extra.point, "cafe_light", false, clock)
	for creature in app.actors:
		var carries: bool = creature.get_meta("at_activity", false) or creature.get_meta("activity_mode", "") == "guide"
		if not creature.sprite.visible or not carries: continue
		if creature.position.distance_to(app.actor.position) > 9.0: continue
		var prop: String = creature.get_meta("activity_prop", "")
		if prop.is_empty(): continue
		var side := -1.0 if creature.facing.x < 0.0 else 1.0
		var carried: Vector3 = creature.position + Vector3(side * 0.19, 0.52 + sin(clock * 1.8) * 0.018, 0.18)
		if prop in ["spark", "droplet"]:
			_draw_small_activity(hud, carried, prop, clock)
		else:
			Art.draw(hud, carried, prop, false, clock)
	for marker in app.regional_markers:
		if not marker.art.visible or marker.work_mix < 0.04: continue
		var facing := 1.0 if marker.art.flip_h else -1.0
		var point: Vector3 = marker.position + Vector3(facing * 0.42, 0.36, 0.07)
		if app.camera.is_position_behind(point): continue
		var at: Vector2 = app.camera.unproject_position(point)
		_draw_station(hud, at, marker.station, clock, marker.work_mix)

static func _draw_small_activity(hud: Control, point: Vector3, prop: String, clock: float) -> void:
	if hud.app.camera.is_position_behind(point): return
	var at: Vector2 = hud.app.camera.unproject_position(point)
	var source := Color("fbcb86") if prop == "spark" else Color("9edcdf")
	for i in 3:
		var age := fposmod(clock * 0.63 + float(i) / 3.0, 1.0)
		var shift := Vector2(sin(age * PI + i) * 7.0, age * (-14.0 if prop == "spark" else 13.0))
		hud.draw_circle(at + shift, 1.4 + age * 0.45, Color(source, sin(age * PI) * 0.78), true, -1, true)

static func _draw_station(hud: Control, at: Vector2, kind: String, clock: float, strength: float) -> void:
	match kind:
		"seal":
			var color := Color(1.0, 0.52, 0.27, strength * (0.55 + 0.24 * sin(clock * 2.2)))
			hud.draw_arc(at, 13.0, -PI * 0.9, PI * 0.76, 20, color, 1.4, true)
			for i in 3:
				var p := at + Vector2.from_angle(clock * 0.55 + i * TAU / 3.0) * 13.0
				hud.draw_circle(p, 2.0, color, true, -1, true)
		"relay":
			for i in 3:
				var p := at + Vector2(0, -9 * i)
				var angle := clock * 1.7 + i
				var color := Color(0.53, 0.94, 0.88, strength * (0.48 + 0.23 * sin(angle)))
				hud.draw_arc(p, 10.0 + 2.0 * i, angle, angle + PI * 1.18, 13, color, 1.5, true)
		"brazier":
			for i in 5:
				var age := fposmod(clock * 0.75 + i / 5.0, 1.0)
				var p := at + Vector2(sin(age * 3.0 + i) * age * 15.0, -age * 33.0)
				var color := Color(1.0, 0.72, 0.29, strength * sin(age * PI) * 0.9)
				hud.draw_circle(p, 2.5 - age, color, true, -1, true)
