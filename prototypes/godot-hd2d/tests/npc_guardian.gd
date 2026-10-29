extends SceneTree
const Motion = preload("res://scripts/npc_locomotion.gd")
var checks := 0
var failures := 0
func check(ok: bool, label: String) -> void:
	checks += 1
	if not ok: failures += 1; push_error(label)
func _initialize() -> void: call_deferred("run")
func run() -> void:
	check(Motion.available("bark_guardian"), "Rindenwächter hat eigene gemalte Folge")
	var motion = Motion.new(); motion.configure("bark_guardian")
	var pixel: float = motion.world_pixel_size(.0145)
	check(is_equal_approx(float(motion.cycles.world_height),1.6), "Feste eigene Größe")
	var sprite := Sprite3D.new(); root.add_child(sprite)
	var material := ShaderMaterial.new(); material.shader=load("res://shaders/painted_character.gdshader")
	for direction in [false,true]:
		sprite.flip_h=direction
		for mode in ["walk","rest"]:
			motion.mode=mode
			for phase in int(motion.cycles[mode].frames):
				motion.phase=phase; motion.apply(sprite,material,.0145)
				check(is_equal_approx(sprite.pixel_size,pixel), "Gleicher Körpermaßstab in allen Phasen/Richtungen")
				check(sprite.offset == Vector2(0,168), "Gemeinsamer fester Bodenanker")
				check(sprite.texture.resource_path.contains("/pixel/"), "HD-2D-Pixelpass")
				check(sprite.frame == phase and sprite.flip_h==direction, "Phase und Richtung erhalten")
	var phases: Array=[]
	for fps in [30,60,120]:
		var sample = Motion.new(); sample.configure("bark_guardian")
		var seen := {}
		for tick in fps*4:
			sample.advance(1.0/fps,1.2/fps,pixel); seen[sample.phase]=true
		check(seen.size()==8, "Acht Gehzeichnungen bei jeder Bildrate")
		phases.append(sample.distance_phase)
		for attempt in 2:
			var before: float=sample.distance_phase
			var idle := {}
			for tick in fps*3:
				sample.advance(1.0/fps,0,pixel)
				if sample.mode=="rest": idle[sample.phase]=true
			check(is_equal_approx(before,sample.distance_phase), "Kein Gehfortschritt ohne Weg")
			check(sample.mode=="rest" and idle.size()==4, "Jeder Stopp erreicht alle Ruhezeichnungen")
			sample.advance(1.0/fps,1.2/fps,pixel)
			check(sample.mode=="walk" and sample.distance_phase>0, "Wiederanlaufen nach beiden Stopps")
	check(absf(phases[0]-phases[1])<.0001 and absf(phases[1]-phases[2])<.0001, "Weggebundener Takt bei 30/60/120 Hz")
	sprite.queue_free(); await process_frame
	print("NPC guardian: %s checks, %s failures" % [checks,failures])
	quit(1 if failures else 0)
