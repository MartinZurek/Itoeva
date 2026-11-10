extends RefCounted

static func mode(appointment: Dictionary) -> String:
	var text: = str(appointment.get("activity", "")).to_lower()
	if "buch" in text or "bücher" in text or "lese" in text or "karten" in text: return "read"
	if "ruhe" in text or "pause" in text or "nacht" in text or "feierabend" in text: return "rest"
	if "werk" in text or "arbeit" in text or "back" in text or "brot" in text or "prüfe" in text: return "work"
	if "beobachte" in text or "lausche" in text or "pflanzen" in text or "kräuter" in text or "blätter" in text or "teich" in text: return "observe"
	return "walk"
static func resting(kind: String, clock: float, seed: int) -> bool:
	if kind == "walk": return false
	var period: = 18.0 if kind in ["read", "rest"] else 13.0
	var duty: = 0.74 if kind in ["read", "rest"] else 0.6
	return fposmod(clock + float(absi(seed) % 11), period) < period * duty
