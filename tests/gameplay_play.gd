extends SceneTree
const Cards = preload("res://scripts/card_catalog.gd")
var app: Node3D
var output := ""

func _initialize() -> void:
	call_deferred("run")

func frames(count: int) -> void:
	for i in count:
		await physics_frame
		await process_frame

func capture(name: String) -> void:
	if output.is_empty(): return
	await RenderingServer.frame_post_draw
	root.get_texture().get_image().save_png(output.path_join(name+".png"))

func run() -> void:
	var args := OS.get_cmdline_user_args()
	if not args.is_empty():
		output = args[0]
		DirAccess.make_dir_recursive_absolute(output)
	OS.set_environment("MOTION_M4","1")
	root.size = Vector2i(1280,720)
	app = load("res://scenes/gameplay.tscn").instantiate()
	root.add_child(app)
	while app.loading_world: await process_frame
	app.save_path = "user://gameplay-tests-%s.json" % OS.get_process_id()
	app.save_data = {"characters": {}, "settings": {}}
	await app.start_game()
	assert(app.room == "SUN_GLADE" and app.actor.species == "mosslyn")
	assert(app.actor.painted_cycle_textures.rest.resource_path.contains("/pixel/mosslyn-rest-m4.png"))
	var flower: Dictionary = app.wonders.offer(app)
	assert(flower.wonder == "pollen")
	app.wonders.perform(app,flower)
	assert(app.reactions.motes.size() == 12)
	await frames(25)
	assert(app.reactions.motes.size() <= app.reactions.CAPACITY)
	await capture("sonnenhain-gameplay")
	var backgrounds := {}
	for id: String in app.Enemies.data():
		var place: String = app.Enemies.data()[id].place
		await app.test_encounter(place,id)
		await frames(4)
		var view: Control = app.forest_encounter.view
		assert(view.arena.place == place and view.background != null)
		assert(is_instance_valid(view.arena_picture))
		backgrounds[view.background.resource_path] = true
		assert(view._mosslyn_picture("rest",0).texture.resource_path.contains("/pixel/mosslyn-rest-m4.png"))
		assert(Rect2(0,165,1280,330).has_point(view._position(2)))
		if id == "ash_hound":
			var battle: RefCounted = app.forest_encounter.model
			for round_number in [1,3]:
				battle.round = round_number
				battle.cursor = 2
				assert(battle.enemy_act())
				assert(battle.last_event.wolf_attack == ("leap" if round_number == 3 else "claw"))
				assert(battle.last_event.wolf_target == "h:0" and battle.impact_time() > 1.3)
				battle.acknowledge_animation()
			battle.enemy_stagger = 1
			battle.cursor = 2
			assert(battle.enemy_act() and not battle.last_event.has("wolf_attack"))
			battle.acknowledge_animation()
		if id in ["thorn_moth","ash_hound","bark_guardian","tide_crab","glut_imp","vharok","relay_warden"]:
			await capture(id+"-arena")
	assert(backgrounds.size() >= 7)
	# Exercise the input route used by a finger dragging an actual hand card.
	await app.test_encounter("SUN_GLADE","thorn_moth")
	await frames(16)
	var encounter: Node = app.forest_encounter
	var view: Control = encounter.view
	encounter.model.decks[0].hand = ["lightling"]
	encounter.model.decks[0].crystals = 10
	assert(view._card_destination(0,Vector2(450,600)).is_empty())
	assert(encounter.model.decks[0].hand == ["lightling"] and not encounter.model.playback_pending)
	view.build_commands()
	view.refresh()
	await frames(15)
	var button: Button
	for candidate in view.buttons:
		if candidate.get_meta("battle_action","") == "card":
			button = candidate
			break
	assert(button != null)
	var start := button.get_global_rect().get_center()
	var slot: Vector2 = view._slot_position(0, 2)
	var finish: Vector2 = view.margin + slot * view.fit
	var down := InputEventScreenTouch.new()
	down.index = 4; down.position = start; down.pressed = true
	assert(view.route_card_pointer(down))
	await frames(30)
	assert(is_instance_valid(view.card_popup) and not encounter.model.playback_pending)
	var drag := InputEventScreenDrag.new()
	drag.index = 4; drag.position = finish; drag.relative = finish-start
	assert(view.route_card_pointer(drag) and view.pointer_dragging)
	assert(is_instance_valid(view.drag_card) and not is_instance_valid(view.card_popup))
	assert(view.drag_hint.get("slot", -1) == 2 and not encounter.model.playback_pending)
	await capture("helferplatz-leuchtet")
	var up := InputEventScreenTouch.new()
	up.index = 4; up.position = finish; up.pressed = false
	assert(view.route_card_pointer(up))
	assert(encounter.model.playback_pending and encounter.model.last_event.card == "lightling")
	assert(not is_instance_valid(view.drag_card))
	var uid: int = encounter.model.last_event.unit_uid
	assert(view._unit_position(uid).is_equal_approx(slot))
	var restored: RefCounted = encounter.model.get_script().new()
	restored.restore(encounter.model.snapshot())
	assert(restored.find_unit(uid).formation_slot == 2)
	encounter.model.acknowledge_animation()
	encounter.busy = 0.0
	view.input_cooldown = 0.0
	encounter.model.party[0].hp -= 20
	encounter.model.decks[0].hand = ["tea", "trail_blessing", "spark", "lightling"]
	view.build_commands()
	view.refresh()
	await frames(3)
	assert(view._card_destination(0, Vector2(690, 260)).is_empty())
	assert(view._card_destination(0, view._drop_target_point("h:0")).code == "h:0")
	var helper_code := "u:" + str(uid)
	assert(view._card_destination(1, view._drop_target_point(helper_code)).code == helper_code)
	assert(view._card_destination(2, view._drop_target_point("enemy")).code == "enemy")
	assert(view._card_destination(3, slot).is_empty())
	for candidate in view.buttons:
		if candidate.get_meta("battle_action", "") == "card" and candidate.get_meta("hand_index", -1) == 1:
			start = candidate.get_global_rect().get_center()
			break
	down.position = start
	assert(view.route_card_pointer(down))
	drag.position = view.margin + view._drop_target_point(helper_code) * view.fit
	assert(view.route_card_pointer(drag) and view.drag_hint.code == helper_code)
	await capture("helfer-zauberziel")
	up.position = view.margin + Vector2(670, 180) * view.fit
	assert(view.route_card_pointer(up))
	assert(not encounter.model.playback_pending and encounter.model.decks[0].hand.size() == 4)
	assert(view.drag_hint.is_empty() and not is_instance_valid(view.drag_card))
	assert(view.route_card_pointer(down))
	assert(view.route_card_pointer(drag))
	up.position = drag.position
	assert(view.route_card_pointer(up))
	assert(encounter.model.last_event.card == "trail_blessing" and encounter.model.last_event.target_code == helper_code)
	await app.visit_test_place("BEACH")
	assert(app.room == "OUTDOOR" and not app.paused)
	app.show_test_places()
	assert(app.paused)
	await frames(5)
	await capture("spielorte-auswahl")
	DirAccess.remove_absolute(app.save_path)
	app.queue_free()
	await process_frame
	print("Gameplay · neue Moosling-Optik, Blütenreaktion, sieben Orte, alle Gegner, Ziehen nach Vorschau, Zielprüfung, Rückgabe und Helferplatz geprüft")
	quit()
