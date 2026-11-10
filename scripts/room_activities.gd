extends RefCounted
const Art = preload("res://scripts/world_moment_art.gd")
const PersonalRest = preload("res://scripts/personal_rest.gd")
var personal_rest = PersonalRest.new()
var active: Dictionary = {}
var served_clock: = 0.0
var served_point: = Vector3.ZERO
var served_species: = ""
const CAFE_ACTIONS: = {
	"mosslyn": "Mooslyn setzt sich zum Kräutertee.", 
	"nocti": "Nocti verfolgt den aufsteigenden Dampf.", 
	"starlet": "Starlight schwebt am Tisch und leuchtet leise.", 
	"wyrmling": "Wyrmling macht es sich beim warmen Tee gemütlich.", 
}
func record(app) -> Dictionary:
	return app.save_data.get("room_activities", {}).get(app.selected, {})
func companion(app, point: Vector3):
	var nearest = null; var gap: = 3.2
	for creature in app.actors:
		if creature == app.actor or not is_instance_valid(creature): continue
		if app.room == "CAFE" and not _can_pause(creature): continue
		var distance: float = creature.position.distance_to(point)
		if distance < gap and app._interaction_clear(creature.position): nearest = creature;gap = distance
	return nearest
func _can_pause(creature) -> bool:
	return creature.enabled and creature.sprite.visible and not creature.mounted and creature.state in ["idle", "sit"] and creature.jump_clock < 0

func label(app, id: String) -> String:
	if not active.is_empty(): return "Zubereitung läuft …" if active.kind == "prepare" else "Gemeinsamer Moment …"
	if id == "kitchen-counter": return "Kräuterbrot vorbereiten"
	if id == "kitchen-table": return "Brot anrichten" if record(app).get("prepared", false) else "Küchenzeile zuerst"
	if companion(app, app.actor.position) != null: return "Gemeinsam Pause machen"
	return "Am Tisch verweilen" if app.selected == "starlet" else "Eine ruhige Teepause"
func reset(app) -> void :
	personal_rest.reset(app)
	if active.has("resident") and is_instance_valid(active.resident):
		active.resident.set_meta("rest_until", active.previous_rest)
		active.resident.set_meta("activity_mode", active.previous_mode)
	for pose in active.get("cafe_poses", []):
		var creature = pose.creature
		if not is_instance_valid(creature): continue
		creature.activity_hover = 0.0
		if not creature.mounted and creature.state == pose.entered_state:
			creature.state = pose.previous_state
			creature.state_clock = 0.0
	active.clear()
func start(app, target: Dictionary) -> void :
	if not active.is_empty() or not personal_rest.active.is_empty(): return
	var id: String = target.id
	if id == "kitchen-table" and not record(app).get("prepared", false):
		app._notice("An der Küchenzeile kannst du Kräuterbrot vorbereiten.");return
	var kind: String = "prepare" if id == "kitchen-counter" else "serve" if id == "kitchen-table" else "pause"
	active = {"kind": kind, "room": app.room, "origin": app.actor.position, "point": target.seat.world_center + Vector3.UP * 0.08, "clock": 0.0, "duration": 4.5 if kind == "prepare" else 6.0}
	var resident = companion(app, app.actor.position if kind == "pause" else target.approach) if kind != "prepare" else null
	active.species = app.selected
	active.player = app.actor
	if resident != null:
		active.merge({"resident": resident, "name": app.Residents.data()[resident.species].name, "previous_rest": resident.get_meta("rest_until", 0.0), "previous_mode": resident.get_meta("activity_mode", "walk")})
		resident.set_meta("rest_until", maxf(float(active.previous_rest), app.game_clock + float(active.duration) + 1))
		resident.set_meta("activity_mode", "observe");resident.npc_vector = Vector2.ZERO
		var gap: Vector3 = app.actor.position - resident.position
		resident.facing = Vector2(signf(gap.x), 0) if absf(gap.x) > absf(gap.z) else Vector2(0, signf(gap.z))
	app.actor.stop_input();app.context_frame = -1
	if kind == "pause":
		_start_cafe(app, resident, target.seat.world_center)
		return
	app._notice("Du bereitest Kräuterbrot vor. Weggehen unterbricht die Zubereitung." if kind == "prepare" else "Eine kurze Pause mit " + str(active.name) + "." if active.has("name") else "Ein ruhiger Moment am Tisch.")
func is_cafe_guest(creature) -> bool:
	return active.get("kind", "") == "pause" and active.get("resident") == creature

func _start_cafe(app, resident, table: Vector3) -> void :
	var stage = app.stage

	var cups: Array[Vector3] = []
	for x in [257.0, 305.0]:
		cups.append(Vector3((x / 480.0 - 0.5) * stage.width, stage.image_y(142.0, table.z, 270.0), table.z))
	var player = app.actor
	var player_cup: = 0 if player.position.x <= table.x else 1
	if resident != null: player_cup = 0 if player.position.x <= resident.position.x else 1
	active.cafe_poses = [_cafe_pose(player, table, cups[player_cup])]
	var line: String = CAFE_ACTIONS.get(player.species, "Du gönnst dir eine ruhige Pause.")
	if resident != null:
		active.cafe_poses.append(_cafe_pose(resident, table, cups[1 - player_cup]))
		line += " " + str(CAFE_ACTIONS.get(resident.species, str(active.name) + " leistet dir Gesellschaft."))
	app._notice(line)

func _cafe_pose(creature, table: Vector3, cup: Vector3) -> Dictionary:
	var previous: String = creature.state
	var shines: bool = creature.species == "starlet"
	creature.state = "idle" if shines or creature.species == "nocti" else "sit"
	creature.state_clock = 0.0
	var dx: float = table.x - creature.position.x
	creature.facing = Vector2.LEFT if dx < - 0.08 else Vector2.RIGHT if absf(dx) > 0.08 else Vector2.UP
	if creature.species == "nocti": creature.facing = Vector2.LEFT if dx < 0 else Vector2.RIGHT
	creature.activity_hover = 0.26 if shines else 0.0
	return {"creature": creature, "previous_state": previous, "entered_state": creature.state, "cup": cup, "shines": shines}

func advance(delta: float, app) -> void :
	personal_rest.advance(delta, app)
	served_clock = maxf(0, served_clock - delta)
	if active.is_empty(): return
	if app.room != active.room or app.selected != active.species or app.actor != active.player or app.actor.position.distance_to(active.origin) > 1.2 or app.actor.jump_clock >= 0:
		reset(app);app._notice("Du kannst später weitermachen.");return
	if active.kind == "pause" and (app.actor._desired_input().length() > 0.12 or app.actor.mounted or app.actor.state not in ["idle", "sit"]):
		reset(app);app._notice("Du kannst jederzeit wieder am Tisch verweilen.");return
	if active.has("resident"):
		var resident = active.resident
		if not is_instance_valid(resident) or resident.position.distance_to(app.actor.position) > 3.8:
			reset(app);return
		if active.kind == "pause" and ( not app.actors.has(resident) or not _can_pause(resident) or not app._interaction_clear(resident.position)):
			reset(app);return
	active.clock += delta
	if float(active.clock) < float(active.duration): return
	if not app.save_data.has("room_activities"): app.save_data.room_activities = {}
	var saved: Dictionary = record(app).duplicate(true)
	var kind: String = active.kind
	if kind == "prepare": saved.prepared = true
	elif kind == "serve": saved.prepared = false;served_point = active.point;served_clock = 25.0;served_species = app.selected
	if kind != "prepare":
		saved.last_room = app.room;saved.resident = active.resident.species if active.has("resident") else ""
	app.save_data.room_activities[app.selected] = saved
	var message: String = "Bereit · Richte das Brot jetzt am Küchentisch an." if kind == "prepare" else "Ihr teilt einen ruhigen Moment mit " + str(active.name) + "." if active.has("name") else "Ein kleiner Moment zum Durchatmen."
	reset(app);app._write_save();app.context_frame = -1;app._notice(message)
func draw(hud: Control) -> void :
	var app = hud.app
	if served_clock > 0 and app.room == "KITCHEN" and app.selected == served_species: Art.draw(hud, served_point, "bread", true, 0)
	if active.is_empty(): return
	if active.kind in ["prepare", "serve"]: Art.draw(hud, active.point, "bread", false, float(active.clock))
	for pose in active.get("cafe_poses", []):
		var creature = pose.creature
		if not is_instance_valid(creature): continue
		if pose.shines:
			Art.draw(hud, creature.light_center(), "cafe_light", false, float(active.clock))
		else:
			Art.draw(hud, pose.cup, "tea_steam", false, float(active.clock))
	var at: = Vector2(hud.size.x * 0.5 - 120, 104)
	hud.draw_style_box(app._room_progress_style, Rect2(at, Vector2(240, 5)))
	hud.draw_rect(Rect2(at, Vector2(240 * clampf(float(active.clock) / float(active.duration), 0, 1), 5)), Color("d3bb82"))
