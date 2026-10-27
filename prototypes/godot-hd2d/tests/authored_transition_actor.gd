extends SceneTree

const Actor = preload("res://scripts/compact_actor.gd")
var checks := 0
var failures := 0

func check(ok: bool, label: String) -> void:
	checks += 1
	if not ok:
		failures += 1
		push_error(label)

func _initialize() -> void:
	call_deferred("run")

func run() -> void:
	OS.set_environment("MOTION_DRAFT", "1")
	var world := Node3D.new()
	root.add_child(world)
	var actor = Actor.new()
	actor.species = "mosslyn"
	actor.enabled = false
	world.add_child(actor)
	await process_frame
	var pixels: float = actor.base_pixel_size * 109.0 * actor.Catalog.relative_height("mosslyn") / float(actor.painted_cycles.walk.reference_height)
	for direction in [Vector2.RIGHT, Vector2.LEFT]:
		actor.authored_transition.reset()
		actor.facing = direction
		actor.running = true
		actor.run_blend = 1.0
		var lower := {}
		for tick in 24:
			actor._update_frame(true)
			if actor.painted_cycle_mode == "lower": lower[actor.sprite.frame] = true
			check(is_equal_approx(actor.sprite.pixel_size, pixels), "Absenken ändert den Figurenmaßstab nicht")
			check(actor.sprite.flip_h == (direction.x < 0), "Verbindungszeichnungen spiegeln vollständig")
		check(lower.size() == 4, "Alle gezeichneten Absenkphasen erscheinen")
		check(actor.painted_cycle_mode == "run" and actor.sprite.frame == 1, "Galopp beginnt in identischer Vorderstütze")
		actor.gait = actor._painted_phase_start(actor.painted_cycles.run, 7) + .001
		actor.running = false
		actor.run_blend = 0.0
		actor._update_frame(true)
		check(actor.painted_cycle_mode == "run", "Während Weiterbewegung zuerst Flugphase abschließen")
		actor.gait = actor._painted_phase_start(actor.painted_cycles.run, 1) + .001
		var rise := {}
		for tick in 24:
			actor._update_frame(true)
			if actor.painted_cycle_mode == "rise": rise[actor.sprite.frame] = true
			check(is_equal_approx(actor.sprite.pixel_size, pixels), "Aufrichten ändert den Figurenmaßstab nicht")
		check(rise.size() == 4, "Alle gezeichneten Aufrichtphasen erscheinen")
		check(actor.painted_cycle_mode == "walk" and actor.sprite.frame == 0, "Gehen beginnt in identischer Endpose")
		actor.authored_transition.reset("run")
		actor.gait = actor._painted_phase_start(actor.painted_cycles.run, 7) + .001
		rise.clear()
		for tick in 24:
			actor._update_frame(false)
			if actor.painted_cycle_active and actor.painted_cycle_mode == "rise": rise[actor.sprite.frame] = true
		check(rise.size() == 4 and not actor.painted_cycle_active, "Direktes Stoppen aus Flug richtet auf und endet im Stillstand")
	for action in ["sit", "roll", "jump", "depth"]:
		actor.authored_transition.reset()
		actor.facing = Vector2.RIGHT
		actor.state = "idle"
		actor.jump_clock = -1.0
		actor.running = true
		actor.run_blend = 1.0
		actor._update_frame(true)
		if action == "jump": actor.jump_clock = .1
		elif action == "depth": actor.facing = Vector2.DOWN
		else: actor.state = action
		actor._update_frame(true)
		check(actor.authored_transition.active == "" and not actor.painted_cycle_active, "Aktionswechsel beendet die Verbindungsfolge: " + action)
	actor.shadow.queue_free()
	world.queue_free()
	await process_frame
	print("Authored transition actor: %s checks, %s failures" % [checks, failures])
	quit(1 if failures else 0)
