extends RefCounted

static func route(profile: Dictionary) -> Array:
	if profile.has("route"): return profile.route
	return [[profile.patrol[0], profile.uv[1]], [profile.patrol[1], profile.uv[1]]]

static func sample(profile: Dictionary, clock: float) -> Dictionary:
	var period: = maxf(8, float(profile.get("period", 14)))
	var phase: = fposmod(clock, period) / period
	var points: = route(profile)
	var stationed: bool = profile.has("station")
	var outbound := 0.35 if not stationed else 0.27
	var work_end := 0.50 if not stationed else 0.66
	var return_end := 0.85 if not stationed else 0.91
	var progress := 0.0
	var moving := false
	var working := phase >= outbound and phase < work_end
	var pose := 0
	var activity := str(profile.get("purpose", ""))
	if phase < outbound: progress = smoothstep(0.0, outbound, phase);moving = true
	elif working: progress = 1.0;pose = 5;activity = str(profile.get("work", ""))
	elif phase < return_end: progress = 1.0 - smoothstep(work_end, return_end, phase);moving = true
	if moving: pose = 6 if int(clock * 3) % 2 == 0 else 0;activity = str(profile.get("travel", ""))
	var lengths: Array[float] = []; var total: = 0.0
	for i in range(1, points.size()):
		var length: = Vector2(points[i][0] - points[i - 1][0], points[i][1] - points[i - 1][1]).length()
		lengths.append(length);total += length
	var remaining: = progress * total
	var uv: = Vector2(points.back()[0], points.back()[1])
	for i in lengths.size():
		if remaining <= lengths[i]:
			uv = Vector2(points[i][0], points[i][1]).lerp(Vector2(points[i + 1][0], points[i + 1][1]), remaining / maxf(1e-05, lengths[i]));break
		remaining -= lengths[i]
	return {"uv": uv, "moving": moving, "working": working, "pose": pose, "activity": activity}
