extends RefCounted

const CAPACITY := 100.0
const DRAIN := 16.0
const RESTART := 30.0
const RECOVERY_DELAY := 0.65
var value := CAPACITY
var exhausted := false
var recovery_delay := 0.0

func allows_sprint() -> bool:
	return not exhausted and value > 0.0

func spend(amount: float) -> bool:
	if exhausted or value < amount: return false
	value = maxf(0.0, value - amount)
	recovery_delay = RECOVERY_DELAY
	if value <= 0.0: exhausted = true
	return true

func advance(delta: float, effort: float, moving: bool, sitting: bool, seated: bool) -> void:
	if effort > 0.0 and allows_sprint():
		value = maxf(0.0, value - DRAIN * clampf(effort, 0.0, 1.0) * delta)
		recovery_delay = RECOVERY_DELAY
		if value <= 0.0: exhausted = true
		return
	# Eine echte Erholungsphase verhindert flackernden Sprint beim gehaltenen Stick.
	var rest_delta := maxf(0.0, delta - recovery_delay)
	recovery_delay = maxf(0.0, recovery_delay - delta)
	if sitting: rest_delta = delta
	var rate := 50.0 if seated else 25.0 if sitting else 7.0 if moving else 12.0
	value = minf(CAPACITY, value + rate * rest_delta)
	if value >= RESTART: exhausted = false
