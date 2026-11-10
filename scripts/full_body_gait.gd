extends RefCounted




const Steps = preload("res://scripts/step_gait.gd")
static var meshes: Dictionary = {}

static func surface(bounds: Rect2i, spacing: int = 3) -> ArrayMesh:
	var key: = str(bounds) + "/" + str(spacing)
	if meshes.has(key): return meshes[key]
	var vertices: = PackedVector3Array()
	var colors: = PackedColorArray()
	var indices: = PackedInt32Array()
	var columns: = ceili(bounds.size.x / float(spacing)) + 1
	var rows: = ceili(bounds.size.y / float(spacing)) + 1
	for y in rows:
		for x in columns:
			var point: = Vector2(bounds.position) + Vector2(bounds.size) * Vector2(float(x) / (columns - 1), float(y) / (rows - 1))
			vertices.append(Vector3(point.x - 128, 128 - point.y, 0))

			colors.append(Color(0, 1, 1, 1))
	for y in rows - 1:
		for x in columns - 1:
			var a: = y * columns + x
			indices.append_array(PackedInt32Array([a, a + columns, a + 1, a + 1, a + columns, a + columns + 1]))
	var arrays: = []
	arrays.resize(Mesh.ARRAY_MAX)
	arrays[Mesh.ARRAY_VERTEX] = vertices
	arrays[Mesh.ARRAY_COLOR] = colors
	arrays[Mesh.ARRAY_INDEX] = indices
	var mesh: = ArrayMesh.new()
	mesh.add_surface_from_arrays(Mesh.PRIMITIVE_TRIANGLES, arrays)
	meshes[key] = mesh
	return mesh

static func point(value: Array) -> Vector2:
	return Vector2(value[0], value[1])

static func rotate_about(p: Vector2, pivot: Vector2, angle: float) -> Vector2:
	return pivot + (p - pivot).rotated(angle)

static func torso(p: Vector2, pose: Dictionary) -> Vector2:
	return rotate_about(p, pose.root, pose.angle) + pose.offset

static func solve_knee(hip: Vector2, target: Vector2, upper: float, lower: float, bend: float) -> Vector2:
	var axis: = target - hip
	var distance: = clampf(axis.length(), absf(upper - lower) + 0.01, upper + lower - 0.01)
	axis = axis.normalized()
	var along: = (upper * upper - lower * lower + distance * distance) / (2 * distance)
	var across: = sqrt(maxf(0, upper * upper - along * along))
	return hip + axis * along + Vector2( - axis.y, axis.x) * across * bend

static func reachable(hip: Vector2, target: Vector2, upper: float, lower: float) -> Vector2:
	var axis: = target - hip
	if axis.length_squared() < 1e-06: axis = Vector2.DOWN
	return hip + axis.normalized() * clampf(axis.length(), absf(upper - lower) + 0.02, upper + lower - 0.02)

static func sample(species: String, row: int, cycle: float, sprint: float, rig: Dictionary, profile: Dictionary, flight: bool = false, calm: bool = false, bound: Dictionary = {}) -> Dictionary:
	var phase: = cycle * TAU
	var depth: = row != 0
	var root: = point(profile.root)
	var two_steps: = cos(phase * 2)
	var bob: = - lerpf(4.5, 7.5, sprint) * (1.0 + two_steps)
	var shift: = (1.6 if depth else 0.8) * sin(phase)
	var angle: = (0.025 if depth else - 0.035 - 0.045 * sprint) + (0.03 if depth else 0.018) * sin(phase)
	if species == "wyrmling":
		bob = - lerpf(3.5, 6.0, sprint) * (1.0 + two_steps)
		angle = ( - 0.025 - 0.045 * sprint if not depth else 0.0) + 0.025 * sin(phase)
	if calm:
		bob = - lerpf(1.8, 2.5, sprint) * (1.0 + cos(phase))
		angle = 0.012 * sin(phase)
		shift = 0.7 * sin(phase)
	var lift: = 0.0
	var squash: = 0.0
	if species == "starlet":


		var beat: = fposmod(cycle, 1.0)
		if beat < 0.18:
			squash = 0.045 * pow(sin(PI * beat / 0.18), 2)
		else:
			lift = lerpf(19.0, 32.0, sprint) * pow(sin(PI * (beat - 0.18) / 0.82), 1.3)
		bob = 0
		angle = lerpf(0.065, 0.1, sprint) * sin(phase - 0.35)
		shift = 1.5 * sin(phase)
	if flight:
		bob = 4.2 * sin(phase - 0.5)
		angle = ( - 0.04 if not depth else 0.0) + 0.035 * sin(phase - 0.7)
	var pose: = {"root": root, "offset": Vector2(shift, - bob), "angle": angle, "lift": lift, "squash": squash, 
		"head_angle": - angle * 0.72 + 0.012 * sin(phase * 2 - 0.65), 
		"ear_angle": 0.035 * sin(phase * 2 - 1.0) * (1.0 + sprint * 0.5), 
		"secondary": lerpf(0.075, 0.13, sprint) * sin(phase - 1.0), 
		"forearms": Vector2(1, -1) * lerpf(0.09, 0.17, sprint) * sin(phase - 0.35), 
		"arms": Vector2(1, -1) * lerpf(0.13, 0.25, sprint) * sin(phase + 0.2), 
		"upper": PackedVector4Array(), "lower": PackedVector4Array(), "toes": PackedVector4Array(), 
		"knees": PackedVector2Array(), "feet": PackedVector2Array(), "hips": PackedVector2Array()}
	if not bound.is_empty():
		pose.offset = Vector2(0, float(bound.compression) * 2.5)
		pose.angle = 0.0
		pose.head_angle = - float(bound.pitch) * 0.45
		pose.secondary = 0.09 * sin(phase - 0.4)
		if species == "wyrmling":
			pose.arms = Vector2(1, -1) * float(bound.wing)
			pose.forearms = Vector2.ZERO
	if flight:
		pose.arms = Vector2(1, -1) * (0.28 * sin(phase) + 0.06 * sin(phase * 2 - 0.6))
	if species == "starlet":
		var lowest: = - INF
		for value in rig.feet:
			var tip: = point(value) - root
			tip *= Vector2(1.0 + squash * 0.35, 1.0 - squash)
			lowest = maxf(lowest, (root + tip.rotated(angle) + pose.offset).y)
		pose.lift += maxf(0, lowest - float(rig.ground))
		return pose
	for leg in rig.feet.size():
		var hip: = point(rig.hips[leg])
		var foot: = point(rig.feet[leg])
		var knee: = hip.lerp(foot, 0.55) + Vector2(-5.0 if not depth else (3.0 if leg % 2 == 0 else -3.0), 0)
		var stagger: float = lerpf([0.0, 0.5, 0.75, 0.25][leg], [0.0, 0.5, 0.5, 0.0][leg], sprint) if rig.feet.size() == 4 else leg * 0.5
		var half_stride: = lerpf(18.0, 26.0, sprint) if not depth else lerpf(10.0, 16.0, sprint)
		if calm: half_stride = lerpf(6.0, 9.0, sprint) if depth else lerpf(13.0, 18.0, sprint)
		if species == "nocti": half_stride *= 0.64
		var step: = Steps.sample(cycle + stagger, half_stride, sprint, 0.0 if row == 0 else 0.38 if row == 1 else - 0.38)
		var hip_target: = torso(hip, pose)
		var foot_target: Vector2 = foot + step.offset
		if not bound.is_empty():
			var tuck: = sin(PI * float(bound.air_phase)) if bound.airborne else 0.0
			foot_target = Vector2(lerpf(foot.x, hip.x, tuck * 0.45), foot.y - tuck * (10.0 if species == "mosslyn" else 13.0))


		var upper: = hip.distance_to(knee)
		var lower: = knee.distance_to(foot)
		if calm: foot_target = reachable(hip_target, foot_target, upper, lower)
		var bend: = -1.0 if (foot - hip).cross(knee - hip) < 0 else 1.0
		var knee_target: = solve_knee(hip_target, foot_target, upper, lower, bend)
		pose.hips.append(hip);pose.knees.append(knee);pose.feet.append(foot)
		pose.upper.append(Vector4((knee_target - hip_target).angle() - (knee - hip).angle(), hip_target.x, hip_target.y, hip_target.distance_to(knee_target) / upper))
		pose.lower.append(Vector4((foot_target - knee_target).angle() - (foot - knee).angle(), knee_target.x, knee_target.y, knee_target.distance_to(foot_target) / lower))
		pose.toes.append(Vector4(0, foot_target.x, foot_target.y, 1))
	return pose
