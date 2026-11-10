extends RefCounted
static func code(effect: Dictionary) -> String:
	var target: = int(effect.get("target", -1))
	return "u:" + str(effect.get("unit", -1)) if target == 3 else "enemy" if target == 2 else "h:" + str(target)

static func weight(event: Dictionary, target: String) -> float:
	var damage: = 0
	for effect in event.get("effects", []):
		if code(effect) == target and effect.kind in ["damage", "weak", "blocked", "burn", "shield", "barrier_hit", "barrier_break"]: damage += int(effect.amount)
	return clampf(damage / 24.0, 0, 1)

static func number_size(event: Dictionary, target: String, small: int = 20) -> int:
	var strength: = weight(event, target)
	return small + 8 if strength >= 0.75 else small + 4 if strength >= 0.35 else small
