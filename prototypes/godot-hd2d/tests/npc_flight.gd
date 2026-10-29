extends SceneTree
const Motion = preload("res://scripts/npc_locomotion.gd")
var checks := 0
var failures := 0
func check(ok: bool, label: String) -> void:
	checks += 1
	if not ok: failures += 1; push_error(label)
func _initialize() -> void: call_deferred("run")
func run() -> void:
	var motion = Motion.new(); motion.configure("thorn_moth")
	var pixel: float=motion.world_pixel_size(.0145)
	var hover := {}; var travel := {}
	for tick in 360:
		motion.advance(1.0/60,0 if tick>=180 else 1.2/60,pixel)
		if tick<180:travel[motion.phase]=true
		else:hover[motion.phase]=true
	check(travel.size()==8 and hover.size()==8,"Vollständiger Schlag auf dem Weg und beim Schweben")
	var previous: float=motion.flight_clock
	motion.advance(1.0/60, .05, pixel)
	check(absf(motion.flight_clock-fposmod(previous+1.4/60,1.0))<.0001,"Anfliegen setzt den Flügelschlag nicht zurück")
	var clocks := []
	for fps in [30,60,120]:
		var sample=Motion.new();sample.configure("thorn_moth")
		for tick in fps*2:sample.advance(1.0/fps,0,pixel)
		clocks.append(sample.flight_clock)
	check(absf(clocks[0]-clocks[1])<.0001 and absf(clocks[1]-clocks[2])<.0001,"Flügeltakt unabhängig von Bildrate")
	var sprite:=Sprite3D.new();root.add_child(sprite)
	var material:=ShaderMaterial.new();material.shader=load("res://shaders/painted_character.gdshader")
	var reference_image: Image=motion.textures.walk.get_image()
	check(reference_image.get_data()==motion.textures.rest.get_image().get_data(),"Wechsel zwischen Flug und Schweben benutzt exakt dieselben Zeichnungen")
	for mode in ["walk","rest"]:
		motion.mode=mode
		for phase in 8:
			motion.phase=phase;motion.apply(sprite,material,.0145)
			check(is_equal_approx(sprite.pixel_size,pixel),"Konstanter Körpermaßstab trotz Flügelbewegung")
			check(sprite.offset.y==136,"Konstanter Fluganker")
			check(sprite.frame==phase,"Eigene Flügelzeichnung ausgewählt")
			check(sprite.texture.resource_path.contains("/pixel/"),"HD-2D-Fassung im Spiel")
	sprite.queue_free();await process_frame
	print("NPC flight: %s checks, %s failures"%[checks,failures]);quit(1 if failures else 0)
