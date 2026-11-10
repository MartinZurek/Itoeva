extends RefCounted


var candidate: = Vector2.ZERO
var elapsed: = 0.0
func advance(current: Vector2, input: Vector2, delta: float, moving: bool) -> Vector2:
	if not moving or input.length() < 0.06:
		candidate = Vector2.ZERO;elapsed = 0;return current
	var horizontal: = absf(input.x); var vertical: = absf(input.y)
	var side: = absf(current.x) > 0.5
	var desired: = Vector2(signf(input.x), 0) if horizontal > vertical else Vector2(0, signf(input.y))
	if side and vertical < horizontal * 1.22: desired = Vector2(signf(input.x), 0)
	elif not side and horizontal < vertical * 1.22: desired = Vector2(0, signf(input.y))
	if desired == current or desired == Vector2.ZERO:
		candidate = Vector2.ZERO;elapsed = 0;return current
	if desired != candidate: candidate = desired;elapsed = 0
	elapsed += maxf(0, delta)
	if elapsed < (0.05 if current.dot(desired) < 0 else 0.065): return current
	candidate = Vector2.ZERO;elapsed = 0
	return desired
