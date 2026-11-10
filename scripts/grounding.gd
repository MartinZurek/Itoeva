extends RefCounted

static func cast_at(stage: Node3D, point: Vector3) -> Vector4:
	var daylight: = 1.0 - smoothstep(0.15, 0.85, stage.darkness_at(point))
	var cast: = Vector2(0.48, 0.28) * lerpf(1.35, 1.0, daylight)
	var strength: float = stage.shadow_strength(point)
	var best: = 0.08
	for source in stage.local_light_sources:
		var gap: = Vector2(point.x - source.position.x, point.z - source.position.z)
		var gain: float = source.get("gain", 1.0)
		var radius: Vector2 = source.radius
		var reach: = maxf(radius.x, radius.y) * 1.5
		var power: float = float(source.strength) * gain * (1.0 - smoothstep(0.0, reach, gap.length())) * (1.0 - daylight * 0.85)
		if power > best and gap.length_squared() > 0.0625:
			best = power;cast = gap.normalized() * clampf(gap.length() * 0.32, 0.25, 0.85)
			strength = maxf(strength, minf(0.43, 0.22 + power * 0.2))
	return Vector4(cast.x, cast.y, daylight, strength)
