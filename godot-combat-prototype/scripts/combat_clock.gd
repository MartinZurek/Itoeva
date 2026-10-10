extends RefCounted
## Dieselbe Zeitlinie treibt Pose, Bewegung und Treffer; niedrige FPS verschieben keine Ereignisse.

const DURATIONS := {"wind": 1.6, "rush": 1.5, "dodge": 1.05}
const IMPACTS := {"wind": 0.90, "rush": 0.60, "dodge": 0.52}
const FRAMES := [0, 1, 2, 3, 4, 5, 6, 0]
var action := ""
var elapsed := 0.0
var impacts := 0
var completed := 0
var impacted := false

func start(kind: String) -> bool:
	if not action.is_empty() or not DURATIONS.has(kind):
		return false
	action = kind
	elapsed = 0.0
	impacted = false
	return true

func advance(delta: float) -> bool:
	if action.is_empty() or delta <= 0.0:
		return false
	elapsed += delta
	var hit := not impacted and elapsed >= float(IMPACTS[action])
	if hit:
		impacted = true
		impacts += 1
	if elapsed >= float(DURATIONS[action]):
		action = ""
		completed += 1
	return hit

func progress() -> float:
	return 0.0 if action.is_empty() else clampf(elapsed / float(DURATIONS[action]), 0.0, 1.0)

func pose() -> int:
	if action.is_empty():
		return 0
	if action == "dodge":
		return 1 if progress() < 0.72 else 6
	# Die kurze Zeichnungsfolge spielt mit rund acht Bildern/s; der restliche Ablauf wartet auf Rueckkehr/Treffer.
	return FRAMES[mini(int(elapsed / 0.12), 7)]

static func excursion(progress_value: float) -> float:
	# Null am Anfang/Ende; kontinuierlicher Vorstoss mit sanftem Rueckweg.
	var p := clampf(progress_value, 0.0, 1.0)
	if p < 0.4:
		return smoothstep(0.05, 0.4, p)
	return 1.0 - smoothstep(0.55, 1.0, p)
