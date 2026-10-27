extends SceneTree
const Motion = preload("res://scripts/npc_locomotion.gd")
var checks := 0
var failures := 0
func check(ok: bool, label: String) -> void:
	checks += 1
	if not ok: failures += 1; push_error(label)
func _initialize() -> void: call_deferred("run")
func run() -> void:
	check(Motion.available("ash_hound"), "Aschenläufer hat eigene Zeichnungen")
	check(not Motion.available("unbekannt"), "Noch nicht bearbeitete Figuren bleiben auf ihrem bisherigen Weg")
	var motion = Motion.new(); motion.configure("ash_hound")
	var seen := {}
	var pixel: float = motion.world_pixel_size(.0145)
	for tick in 600:
		motion.advance(1.0/60.0, 1.2/60.0, pixel)
		seen[motion.phase] = true
	check(seen.size() == 8, "Acht vollständige Gehphasen statt Zweibild-Flackern")
	var phases := []
	for fps in [30,60,120]:
		var sample = Motion.new(); sample.configure("ash_hound")
		for tick in fps * 3: sample.advance(1.0 / fps, .91 / fps, pixel)
		phases.append(sample.distance_phase)
	check(absf(phases[0]-phases[1]) < .0001 and absf(phases[1]-phases[2]) < .0001, "Gleicher Weg ergibt gleichen Takt bei 30/60/120 Hz")
	var before: float = motion.distance_phase
	for tick in 60: motion.advance(1.0/60.0, 0, pixel)
	check(is_equal_approx(before,motion.distance_phase), "Keine Gehphase ohne Wegfortschritt")
	check(motion.mode == "rest", "Anhalten erreicht die eigene Ruhefolge")
	var sprite := Sprite3D.new(); root.add_child(sprite)
	var material := ShaderMaterial.new(); material.shader=load("res://shaders/painted_character.gdshader")
	for mode in ["walk","rest"]:
		motion.mode=mode
		for phase in int(motion.cycles[mode].frames):
			motion.phase=phase; motion.apply(sprite,material,.0145)
			check(is_equal_approx(sprite.pixel_size,pixel), "Gleicher Maßstab in allen Geh-/Ruhezeichnungen")
			check(sprite.offset.y == 136, "Feste gemeinsame Bodenlinie")
			check(sprite.texture.resource_path.contains("/pixel/"), "Spiel verwendet die HD-2D-Pixelfassung")
			check(sprite.frame == phase, "Echte gezeichnete Phase ausgewählt")
	sprite.queue_free(); await process_frame
	print("NPC locomotion: %s checks, %s failures" % [checks,failures])
	quit(1 if failures else 0)
