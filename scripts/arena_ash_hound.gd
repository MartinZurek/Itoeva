extends RefCounted

# These are presentation crops. The exploration sprite atlas keeps its own regions.
const REGIONS := [
	Rect2(5, 355, 287, 279), Rect2(276, 354, 260, 281),
	Rect2(513, 351, 318, 240), Rect2(811, 351, 225, 282),
	Rect2(1007, 468, 300, 172), Rect2(1272, 353, 264, 282)
]
const HEIGHT_SCALE := 0.58

static func motion(home: Vector2, target: Vector2, elapsed: float, impact: float, duration: float, leaping: bool) -> Dictionary:
	var direction := signf(target.x - home.x)
	var contact := target - Vector2(direction * 83.0, -4.0)
	var landing := impact + 0.16
	var return_start := impact + 0.34
	var windup := 0.29
	var takeoff := impact - (0.47 if leaping else 0.31)
	var ground := home
	var lift := 0.0
	var pose := 5
	var returning := false
	if elapsed < windup:
		ground -= Vector2(direction * 13.0 * sin(clampf(elapsed / windup, 0.0, 1.0) * PI * 0.5), 0.0)
		pose = 1
	elif elapsed < return_start:
		var approach := smoothstep(windup, impact, elapsed)
		ground = (home - Vector2(direction * 13.0, 0.0)).lerp(contact, approach)
		ground.y += 17.0 * sin(approach * PI)
		if elapsed >= takeoff and elapsed < landing:
			var air := clampf((elapsed - takeoff) / (landing - takeoff), 0.0, 1.0)
			lift = sin(air * PI) * (46.0 if leaping else 23.0)
			pose = 2
		else:
			pose = 1 if elapsed >= landing or int((elapsed - windup) * 9.0) % 2 == 0 else 5
	else:
		var retreat := smoothstep(return_start, duration - 0.15, elapsed)
		ground = contact.lerp(home, retreat)
		ground.y += 17.0 * sin(retreat * PI)
		lift = 16.0 * sin(retreat * PI)
		returning = retreat < 0.98
		pose = 2 if returning and retreat < 0.55 else 5
	return {
		"ground": ground, "foot": ground - Vector2(0.0, lift), "lift": lift, "lean": 0.0,
		"travel": ground.distance_to(home) > 1.0, "returning": returning,
		"pose": pose, "toward_left": (direction < 0.0) != returning
	}

static func flip_for(pose: int, toward_left: bool) -> bool:
	var faces_left := pose not in [2, 3]
	return faces_left != toward_left
