extends RefCounted


var target: = Vector3.ZERO
var origin: = Vector3.ZERO
var intent: = Vector3.RIGHT
var distance: = 2.8
var turn: = 0.0
var side: = 1.0
var free: = 1.0
var valid: = false
var queries: = 0
var revisions: = 0
func choose(stage: Node3D, at: Vector3, wish: Vector3, momentum: Vector3, reach: float, radius: float, plant: int) -> void :
	origin = at;intent = wish.normalized();queries = 0;revisions += 1
	side = 1.0 if plant % 2 == 0 else -1.0
	turn = clampf(1.0 - momentum.normalized().dot(intent), 0.0, 1.0) if momentum.length() > 0.1 else 0.0


	var predicted: = at; var velocity: = momentum;velocity.y = 0
	var points: = PackedVector3Array(); var travelled_prediction: = 0.0; var next_mark: = reach * 0.25
	for tick in 100:
		var dt: = 0.04
		var wanted: = intent * 3.85
		var after: = Vector3(move_toward(velocity.x, wanted.x, 22.0 * dt), 0, move_toward(velocity.z, wanted.z, 22.0 * dt))
		var shift: Vector3 = (velocity + after) * 0.5 * dt
		var length: = shift.length()
		if length > 1e-05:
			while points.size() < 4 and travelled_prediction + length >= next_mark:
				points.append(predicted + shift * ((next_mark - travelled_prediction) / length))
				next_mark += reach * 0.25
			predicted += shift;travelled_prediction += length
		velocity = after
		if points.size() == 4: break
	var cursor: = at; var travelled: = 0.0
	var has_ground: bool = is_instance_valid(stage) and stage.has_method("is_walkable")
	for point in points:
		var step: = point - cursor
		var dt: = maxf(0.01, step.length() / 3.85)
		var motion: = step / dt
		if has_ground and stage.has_method("guide_motion"): motion = stage.guide_motion(cursor, motion, dt, radius + 0.02)
		var next: = cursor + motion * dt
		if has_ground:
			queries += 1
			if not stage.is_walkable(next, radius + 0.025):
				var low: = cursor; var high: = next
				for refine in 4:
					var middle: = low.lerp(high, 0.5);queries += 1
					if stage.is_walkable(middle, radius + 0.025): low = middle
					else: high = middle
				travelled += Vector2(low.x - cursor.x, low.z - cursor.z).length();cursor = low
				break
		travelled += Vector2(next.x - cursor.x, next.z - cursor.z).length();cursor = next
	free = clampf(travelled / maxf(reach, 0.1), 0.15, 1.0)
	var perpendicular: = Vector3( - intent.z, 0, intent.x)
	var selected: = cursor
	if has_ground:
		for offset in [side * 0.11, 0.0, - side * 0.07]:
			var spot: = cursor + perpendicular * float(offset);queries += 1
			if stage.is_walkable(spot, radius + 0.025): selected = spot;break
	target = selected
	distance = clampf(travelled, 2.5, 3.15)
	valid = travelled > 0.3
