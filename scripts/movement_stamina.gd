extends RefCounted

const CAPACITY: = 100.0
const FULL_RUN_SECONDS: = 180.0
const DRAIN: = CAPACITY / FULL_RUN_SECONDS
const RESTART: = 70.0
const RECOVERY_DELAY: = 1.0
var value: = CAPACITY
var exhausted: = false
var recovery_delay: = 0.0
var winded_clock: = 0.0

func allows_sprint() -> bool:
	return not exhausted and value > 0.0

func spend(amount: float) -> bool:
	if exhausted or value < amount: return false
	value = maxf(0.0, value - amount)
	recovery_delay = RECOVERY_DELAY
	if value <= 1e-06:
		value = 0.0;exhausted = true;winded_clock = 2.4
	return true

func advance(delta: float, effort: float, moving: bool, sitting: bool, seated: bool) -> void :
	winded_clock = maxf(0.0, winded_clock - delta)
	if effort > 0.0 and allows_sprint():
		value = maxf(0.0, value - DRAIN * clampf(effort, 0.0, 1.0) * delta)
		recovery_delay = RECOVERY_DELAY
		if value <= 1e-06:
			value = 0.0;exhausted = true;winded_clock = 2.4
		return

	var rest_delta: = maxf(0.0, delta - recovery_delay)
	recovery_delay = maxf(0.0, recovery_delay - delta)
	if sitting: rest_delta = delta
	var rate: = 18.0 if seated else 14.0 if sitting else 4.0 if moving else 8.0
	value = minf(CAPACITY, value + rate * rest_delta)
	if value >= RESTART: exhausted = false
