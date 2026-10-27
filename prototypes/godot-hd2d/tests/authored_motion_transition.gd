extends SceneTree

const Transition = preload("res://scripts/authored_motion_transition.gd")
var checks := 0
var failures := 0

func check(ok: bool, label: String) -> void:
	checks += 1
	if not ok:
		failures += 1
		push_error(label)

func _initialize() -> void:
	for hz in [30, 60, 120]:
		var motion = Transition.new()
		var seen := {}
		var total := 0.0
		var sample := {}
		while total < 0.5:
			sample = motion.advance(1.0 / hz, true, true)
			if sample.mode == "lower": seen[sample.phase] = true
			total += 1.0 / hz
		check(seen.size() == 4, "Absenkung zeigt bei jedem Bildtakt alle vier Zeichnungen")
		check(sample.mode == "run" and motion.active == "", "Nach Absenkung folgt Galopp")
		seen.clear()
		for tick in hz:
			sample = motion.advance(1.0 / hz, false, true)
			if sample.mode == "rise": seen[sample.phase] = true
		check(seen.size() == 4, "Aufrichten zeigt bei jedem Bildtakt alle vier Zeichnungen")
		check(sample.mode == "walk", "Nach Aufrichten folgt Gehen")
		motion.reset("run")
		sample = motion.advance(1.0 / hz, false, true, false)
		check(sample.mode == "run" and motion.active == "", "Aus dem Flug nicht in die Aufrichtpose springen")
		sample = motion.advance(1.0 / hz, false, true, true)
		check(sample.mode == "rise", "Vorderstütze gibt das Aufrichten frei")
		motion.reset("run")
		seen.clear()
		for tick in hz:
			sample = motion.advance(1.0 / hz, false, false)
			if sample.mode == "rise": seen[sample.phase] = true
		check(seen.size() == 4, "Auch direktes Loslassen richtet den Vierbeiner auf")
		check(sample.mode == "" and motion.settled == "walk", "Nach Stoppen kein unsichtbar laufender Galopp")
		motion.reset()
		motion.advance(0.12, true, true)
		sample = motion.advance(0.0, false, true)
		check(sample.mode == "rise" and sample.phase == 2, "Kurzer Sprintimpuls kehrt an passender Körperhöhe um")
		for tick in hz: sample = motion.advance(1.0 / hz, false, true)
		check(sample.mode == "walk" and motion.active == "", "Kein Hängenbleiben nach abgebrochenem Sprint")
		motion.advance(0.1, true, true)
		motion.reset()
		sample = motion.advance(0.0, false, true)
		check(sample.mode == "walk", "Aktionsabbruch verwirft alte Verbindungsphasen")
		motion.reset("run")
		for tick in 30:
			sample = motion.advance(1.0 / hz, tick % 2 == 0, true)
			check(sample.phase < 4, "Schneller Tempowechsel bleibt im Atlas")
		for tick in hz: sample = motion.advance(1.0 / hz, false, true)
		check(sample.mode == "walk", "Hektische Eingabe endet im angeforderten Gang")
	print("Authored motion transitions: %s checks, %s failures" % [checks, failures])
	quit(1 if failures else 0)
