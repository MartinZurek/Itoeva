extends RefCounted


const DURATION: = 0.28
var settled: = "walk"
var active: = ""
var elapsed: = 0.0

func reset(mode: String = "walk") -> void :
	settled = mode
	active = ""
	elapsed = 0.0

func advance(delta: float, wants_run: bool, moving: bool, exit_contact: bool = true) -> Dictionary:
	var desired: = "run" if wants_run and moving else "walk"
	var wanted_transition: = "lower" if desired == "run" else "rise"



	if active == "" and settled == "run" and desired == "walk" and moving and not exit_contact:
		return {"mode": "run", "phase": -1, "entered": false}



	if active != "" and active != wanted_transition:
		active = wanted_transition
		elapsed = DURATION - elapsed
	if active == "" and settled != desired:
		active = wanted_transition
		elapsed = 0.0
	if active != "":
		elapsed = minf(DURATION, elapsed + maxf(0.0, delta))
		if elapsed < DURATION:
			return {"mode": active, "phase": mini(3, int(elapsed / DURATION * 4)), "entered": false}
		settled = desired
		active = ""
		elapsed = 0.0
		return {"mode": settled if moving else "", "phase": -1, "entered": moving}
	return {"mode": settled if moving else "", "phase": -1, "entered": false}
