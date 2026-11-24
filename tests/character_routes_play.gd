extends SceneTree
const Surfaces = preload("res://scripts/walk_surfaces.gd")
var app: Node3D
var captures := ""

func _initialize() -> void:
	run.call_deferred()

func run() -> void:
	OS.set_environment("MOTION_M4", "1")
	root.size = Vector2i(1280, 720)
	if not OS.get_cmdline_user_args().is_empty():
		captures = OS.get_cmdline_user_args()[0]
		DirAccess.make_dir_recursive_absolute(captures)
	app = load("res://scenes/gameplay.tscn").instantiate()
	root.add_child(app)
	while app.loading_world: await process_frame
	app.save_path = "user://character-routes-%s.json" % OS.get_process_id()
	app.save_data = {"characters":{}, "settings":{}}
	var checks := 0
	for species: String in app.Catalog.STARTERS:
		app.selected = species
		await app.visit_test_place("SUN_GLADE")
		var actor: Node3D = app.actor
		actor.set_physics_process(false)
		actor.enabled = false
		for direction in [Vector2.RIGHT, Vector2.DOWN, Vector2.UP, Vector2.LEFT]:
			for action: String in ["idle", "walk", "run", "roll", "sit", "jump"]:
				actor.facing = direction
				actor.state = action if action in ["roll", "sit"] else "idle"
				actor.state_clock = 0.24
				actor.jump_clock = 0.2 if action == "jump" else -1.0
				actor.running = action == "run"
				actor.sprint_requested = action == "run"
				actor.run_blend = 1.0 if action == "run" else 0.0
				actor.actual_speed = 3.0 if action in ["run", "walk"] else 0.0
				actor.gait = 0.3
				actor._update_frame(action in ["walk", "run"])
				assert(actor.painted_cycle_active, species + ":" + action)
				assert(actor.sprite.texture.resource_path.contains("/pixel/"))
				assert(actor.sprite.pixel_size > 0 and actor.displayed_bounds.has_area())
				checks += 1
				if not captures.is_empty() and direction in [Vector2.DOWN, Vector2.UP] and action in ["walk", "sit", "roll"]:
					await process_frame
					await RenderingServer.frame_post_draw
					root.get_texture().get_image().save_png(captures.path_join(species + "-" + str(direction.y) + "-" + action + ".png"))
		if species == "mosslyn":
			for direction: Vector2 in [Vector2.RIGHT, Vector2.LEFT, Vector2.DOWN, Vector2.UP]:
				var view := 1 if direction == Vector2.DOWN else 2 if direction == Vector2.UP else 0
				actor.facing = direction
				actor.state = "roll"
				actor.jump_clock = -1.0
				for phase in actor.MOSSLYN_ROLL_FRAMES:
					actor.state_clock = (phase + 0.5) * actor.ROLL_SECONDS / actor.MOSSLYN_ROLL_FRAMES
					actor._update_frame(false)
					assert(actor.painted_cycle_mode == "details")
					assert(actor.sprite.frame == view * actor.MOSSLYN_ROLL_FRAMES + phase)
					assert(actor.sprite.flip_h == (direction == Vector2.LEFT))
					assert(absf(actor.rest_clearance()) < 0.005)
					checks += 1
		await app.test_encounter("SUN_GLADE", "thorn_moth")
		var view: Control = app.forest_encounter.view
		for mode: String in ["rest", "run"]:
			var picture: Dictionary = view._current_picture(species, mode, 17)
			assert(picture.texture.resource_path.contains("/pixel/") and picture.region.has_area())
			checks += 1
	for place: String in Surfaces.data().route_checks:
		await app.visit_test_place(place)
		app.actor.set_physics_process(false)
		var radius: float = app.actor.collider.shape.radius
		for line: Array in Surfaces.data().route_checks[place]:
			for i in range(1, line.size()):
				var a := Surfaces.native_point(app.stage, line[i - 1])
				var b := Surfaces.native_point(app.stage, line[i])
				for step in 21:
					var point := a.lerp(b, step / 20.0)
					assert(app.stage.is_walkable(point, radius), "%s path %s step %s" % [place, i, step])
					checks += 1
		# Wasser, Kulisse und Vordergrund werden nicht pauschal freigegeben.
		for point in [[240, 80], [240, 268]]:
			assert(not app.stage.is_walkable(Surfaces.native_point(app.stage, point), radius))
			checks += 1
	print("CHARACTER_ROUTES_OK ", checks)
	app.queue_free()
	await process_frame
	quit()
