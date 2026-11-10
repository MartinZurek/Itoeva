extends RefCounted

var active: Dictionary = {}
const NOCTI_CONTACT: = 0.71
const PROFILES: = {
	"nocti": {
		"label": "Auf dem Polster landen", 
		"title": "Noctis Abendnische", 
		"arrival": "Du gleitest auf Noctis Polster.", 
		"memory": "Ein ruhiger Moment mit angelegten Flügeln.", 
		"lower": 1.65, 
		"rise": 1.35, 
	}, 
	"mosslyn": {
		"label": "Im Mooslager ruhen", 
		"title": "Mooslyns Mooslager", 
		"arrival": "Du kommst im Mooslager zur Ruhe.", 
		"memory": "Ein ruhiger Moment im vertrauten Mooslager.", 
		"lower": 1.2, 
		"rise": 0.9, 
	}, 
	"starlet": {
		"label": "Zur Ruhe kommen", 
		"title": "Starlights Ruheinsel", 
		"arrival": "Du hüpfst in die Hängematte. Sie schwingt sanft aus.", 
		"memory": "Ein ruhiger Moment über der vertrauten Hängematte.", 
		"lower": 1.8, 
		"rise": 1.2, 
	}, 
	"wyrmling": {
		"label": "Im Nest einrollen", 
		"title": "Wyrmlings Steinnest", 
		"arrival": "Du steigst ins Nest. Das Kissen gibt nach.", 
		"memory": "Ein ruhiger Moment im vertrauten Steinnest.", 
		"lower": 1.45, 
		"rise": 1.05, 
	}, 
}

func offer(app) -> Dictionary:
	if not PROFILES.has(app.selected) or app.room != "BEDROOM": return {}
	if not active.is_empty(): return {"kind": "rest_menu", "id": "personal-rest", "label": "Ruheplatz", "ready": true, "distance": 0.0, "position": app.actor.position + Vector3.UP * (2.1 if app.selected == "starlet" else 1.7)}
	if app.stage.rest_anchors.is_empty(): return {}
	var anchor: Dictionary = app.stage.rest_anchors[0]
	var approach: Vector3 = app.stage.safe_position(anchor.approach, app.actor.collider.shape.radius)
	var distance: = Vector2(app.actor.position.x - approach.x, app.actor.position.z - approach.z).length()
	return {"kind": "home_rest", "id": anchor.id, "label": PROFILES[app.selected].label, "position": anchor.position + Vector3.UP * 0.5, "approach": approach, "ready": app.actor.is_on_floor() and not app.actor.mounted and distance < 1.0, "distance": distance, "anchor": anchor}

func start(app, target: Dictionary) -> void :
	if not active.is_empty() or not app.room_activities.active.is_empty(): return
	var nocti: bool = app.selected == "nocti"
	var clearance: float = maxf(0.0, app.actor.rest_clearance()) if nocti else 0.0
	if not app.actor.begin_home_rest(): return
	var anchor: Dictionary = target.anchor
	var origin: Vector3 = app.actor.position
	var exit: Vector3 = app.stage.safe_position(target.approach, app.actor.collider.shape.radius)
	active = {"room": app.room, "species": app.selected, "actor": app.actor, "id": anchor.id, "mode": "lower", "clock": 0.0, "origin": origin, "anchor": anchor.position, "exit": exit, "recorded": false, "contact_age": -1.0}
	app.hud.reset_touch()
	app.actor.mount_seat(anchor.position, exit)
	if nocti:
		app.actor.facing = Vector2.LEFT if app.actor.facing.x < 0 else Vector2.RIGHT
		app.actor.home_rest_flight = "land"
	else:
		app.actor.facing = Vector2.RIGHT
	app.actor.seat_occlusion_z = app.stage.furniture[0].body.position.z + app.stage.furniture[0].body.get_child(0).shape.size.z * 0.5 + 0.08
	origin.y += clearance
	app.actor.position = origin
	active.origin = origin
	app.actor._update_frame(false)
	app._notice(PROFILES[app.selected].arrival + " Bewegen oder den Ruheplatz antippen, um aufzubrechen.")

func leave(app) -> void :
	if active.is_empty() or active.mode == "rise": return
	active.mode = "rise";active.clock = 0.0;active.origin = app.actor.position;active.phase_from = app.actor.home_rest_phase
	active.glow_from = app.actor.rest_glow_factor
	if active.species == "nocti":
		active.airborne_exit = not app.actor.home_rest_flight.is_empty()
	app.actor.stop_input()

func reset(app) -> void :
	if active.is_empty(): return
	var creature = active.actor
	if is_instance_valid(app.stage):
		app.stage.set_rest_room_offset(Vector2.ZERO)
	if is_instance_valid(creature):
		creature.in_rest_rim = false
		creature.home_rest_phase = -1
		creature.home_rest_flight = ""
		creature.home_rest_flight_phase = 0
		creature.rest_glow_factor = 1.0
		creature.leave_seat()
		creature.position = active.exit
		if active.species == "nocti":
			creature.authored_locomotion.reset()
		else:
			creature.facing = Vector2.RIGHT
		creature._update_frame(false)
	active.clear()

func advance(delta: float, app) -> void :
	if active.is_empty(): return
	if app.room != active.room or app.selected != active.species or not is_instance_valid(active.actor):
		reset(app);return
	var creature = active.actor
	var starlight: bool = active.species == "starlet"
	var nocti: bool = active.species == "nocti"
	var profile: Dictionary = PROFILES[active.species]
	if active.mode == "rise" and creature._desired_input().length() > 0.12: active.erase("after")
	if active.mode != "rise" and (creature._desired_input().length() > 0.12 or Input.is_action_just_pressed("jump") or Input.is_action_just_pressed("sit") or Input.is_action_just_pressed("roll")): leave(app)
	active.clock += delta
	var contact_weight: = 1.0
	if active.mode == "lower":
		var t: = clampf(float(active.clock) / float(profile.lower), 0, 1)
		creature.position = Vector3(active.origin).lerp(active.anchor, smoothstep(0, 1, t))
		if starlight:
			creature.position.y += 1.04 * sin(t * PI)
		if t >= (NOCTI_CONTACT if nocti else 0.76) and active.contact_age < 0.0:
			active.contact_age = 0.0
		creature.home_rest_phase = 0 if t < 0.4 else 1 if t < 0.82 else 2
		if starlight:
			creature.rest_glow_factor = lerpf(1.0, 0.55, smoothstep(0, 1, t))
		elif nocti:
			_lower_nocti(creature, t)
		if t >= 1: active.mode = "rest";active.clock = 0.0
	elif active.mode == "rest":
		creature.home_rest_phase = 2
		creature.position = active.anchor
		if starlight:

			creature.position.y += 0.018 * sin(float(active.clock) * TAU / 5.0)
			creature.rest_glow_factor = 0.55
		if float(active.clock) >= 1.5 and not active.recorded:
			active.recorded = true
			if not app.save_data.has("room_activities"): app.save_data.room_activities = {}
			var record: Dictionary = app.room_activities.record(app).duplicate(true)
			record.rested_at = active.id
			app.save_data.room_activities[app.selected] = record
			app._save_progress()
			app._notice(profile.memory)
	elif active.mode == "rise":
		var t: = clampf(float(active.clock) / float(profile.rise), 0, 1)
		contact_weight = 1.0 - smoothstep(0.0, 1.0, t)
		if starlight:
			creature.in_rest_rim = creature.in_rest_rim and t < 0.32
		creature.home_rest_phase = mini(int(active.phase_from), 2 if t < 0.18 else 1 if t < 0.55 else 0)
		creature.position = Vector3(active.origin).lerp(active.exit, smoothstep(0, 1, t))
		if starlight:
			creature.position.y += 0.55 * sin(t * PI)
			creature.rest_glow_factor = lerpf(float(active.glow_from), 1.0, smoothstep(0, 1, t))
		if nocti:
			contact_weight = 1.0 - smoothstep(0.3, 0.6, t)
			_rise_nocti(creature, t)
		if t >= 1:
			var after: Dictionary = active.get("after", {}).duplicate()
			reset(app);app._save_progress();app._notice("Bereit für deinen nächsten Weg.")
			if after.get("kind", "") == "walk": app.request_walk(after.destination)
			elif not after.is_empty(): app.request_interaction(after.kind, after.id)
			return
	if active.contact_age >= 0.0:
		active.contact_age += delta
		var offset: = _impact_offset(active.species, active.contact_age) * contact_weight
		app.stage.set_rest_room_offset(offset)
		if active.mode != "rise":
			if starlight:
				creature.in_rest_rim = true
				creature.position += app.stage.rest_room_follow()
			elif nocti:
				creature.in_rest_rim = true
				creature.position += app.stage.rest_room_follow()
			elif active.species == "mosslyn":
				creature.position.y -= offset.y * 2.5
		creature._update_occlusion()

func _lower_nocti(creature, t: float) -> void :
	var glide: = smoothstep(0.0, NOCTI_CONTACT, t)
	creature.position = Vector3(active.origin).lerp(active.anchor, glide)
	creature.position.y += 0.6 * sin(glide * PI)
	creature.home_rest_flight = "land" if t < NOCTI_CONTACT else ""
	creature.home_rest_flight_phase = mini(2, int(t / NOCTI_CONTACT * 3.0))
	creature.home_rest_phase = 0 if t < 0.79 else 1 if t < 0.93 else 2

func _rise_nocti(creature, t: float) -> void :
	var airborne: bool = active.airborne_exit
	var lift_at: = 0.0 if airborne else 0.3
	var flight: = smoothstep(lift_at, 1.0, t)
	creature.position = Vector3(active.origin).lerp(active.exit, flight)
	creature.position.y += 0.85 * sin(flight * PI)
	creature.in_rest_rim = creature.in_rest_rim and t < lift_at
	if t < lift_at:
		creature.home_rest_phase = mini(int(active.phase_from), 2 if t < 0.1 else 1 if t < 0.21 else 0)
		return
	if t < 0.7:
		creature.home_rest_flight = "takeoff"
		creature.home_rest_flight_phase = 3 if airborne else mini(3, 1 + int((t - lift_at) / (0.7 - lift_at) * 3.0))
	else:
		creature.home_rest_flight = "land"
		creature.home_rest_flight_phase = mini(3, int((t - 0.7) / 0.3 * 4.0))

func _impact_offset(species: String, age: float) -> Vector2:
	if species == "starlet":
		var swing: = sin(age * TAU / 1.65) * exp( - age * 1.22)
		var sag: = 0.012 * (1.0 - exp( - age * 8.0))
		return Vector2(0.029 * swing, sag + 0.014 * swing)
	if species == "nocti":
		var compression: = 0.012 * (1.0 - exp( - age * 12.0))
		var rebound: = 0.02 * sin(age * TAU / 0.64) * exp( - age * 4.5)
		return Vector2(0.0, compression + rebound)
	if species == "mosslyn":
		return Vector2(0.0, 0.01 * (1.0 - exp( - age * 9.0)) + 0.018 * sin(age * TAU / 0.71) * exp( - age * 3.3))
	return Vector2(0.0, - 0.038 * sin(age * TAU / 1.0) * exp( - age * 3.8))

func show_choices(app) -> void :
	if active.is_empty(): return
	var familiar: bool = app.room_activities.record(app).get("rested_at", "") == active.id
	var text: String = "Du hast hier schon einen ruhigen Moment verbracht. " if familiar else "Du kannst einfach hier verweilen. "
	text += "Zeit vergeht nur dann bis zum Morgen, wenn du das ausdrücklich wählst."
	app._show_activity(PROFILES[active.species].title, text, "Weiter ruhen", app._resume)

	var back: Button = app.menu_buttons.pop_back()
	var column = back.get_parent()
	column.remove_child(back);back.queue_free()
	column.add_child(app._button("Aufbrechen", func(): app._resume();leave(app)))
	if active.mode == "rest":
		column.add_child(app._button("Zeit bis morgen 08:00 verstreichen lassen", func():
			app.world_clock.sleep_until_morning();app._refresh_residents();app._apply_world_time();app._resume();leave(app);app._notice("Der nächste Morgen · " + app.clock_label())))
