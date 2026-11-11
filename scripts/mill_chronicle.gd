extends Node

const Catalog = preload("res://scripts/world_catalog.gd")
const Collection = preload("res://scripts/card_collection.gd")
const ROOM := "MILL_BROOK"
const FIND := "muehlenchronik"
const LEVER := "muehlenwehr"
const CLAIM := "find:" + FIND
const PROP_OFFSET := Vector3(.40, 0.0, -.32)

enum Route { CLOSED, SIDE, WHEEL }

var species := ""
var route := Route.CLOSED
var flow := 0.0
var wheel_angle := 0.0
var wheel_speed := 0.0
var wheel_mix := 0.0
var cooldown := 0.0
var material: ShaderMaterial
var approach: Vector3
var sound: AudioStreamPlayer

static func mount(app) -> void:
	var mill := new()
	mill.name = "MillChronicle"
	mill.species = app.selected
	mill.material = app.stage.painted_materials[0]
	var raw := Catalog.position_for(ROOM, Vector2(.39, .44))
	mill.approach = app.stage.safe_position(raw)
	# Bereits geborgene Chroniken nutzen dieselbe Quittung wie vor dem Patch.
	if Collection.state(app.save_data, app.selected).claims.get(CLAIM, false):
		mill.route = Route.WHEEL
		mill.flow = 1.0
		mill.wheel_speed = .95
		mill.wheel_mix = 1.0
		mill._update_material()
	app.stage.add_child(mill)
	mill.set_process(mill.route == Route.WHEEL)

static func current(app):
	if app.room != ROOM or not is_instance_valid(app.stage):
		return null
	var mill = app.stage.get_node_or_null("MillChronicle")
	return mill if mill != null and mill.species == app.selected else null

static func offer(app) -> Dictionary:
	var mill = current(app)
	if mill == null or Collection.state(app.save_data, app.selected).claims.get(CLAIM, false):
		return {}
	var point: Vector3 = mill.approach
	var distance := Vector2(app.actor.position.x - point.x, app.actor.position.z - point.z).length()
	if distance > 9.0:
		return {}
	return {
		"kind": "mill_lever", "id": LEVER, "label": "Wehr umlegen",
		"position": point + PROP_OFFSET + Vector3.UP * .95, "approach": point, "distance": distance,
		"ready": distance < 1.4 and app.actor.is_on_floor() and not app.actor.mounted and app._interaction_clear(point)
	}

static func turn(app) -> void:
	var mill = current(app)
	if mill == null or not offer(app).get("ready", false) or mill.cooldown > 0.0:
		return
	mill.route = Route.SIDE if mill.route != Route.SIDE else Route.WHEEL
	mill.cooldown = .65
	mill.set_process(true)
	app.actor.stop_input()
	if mill.route == Route.WHEEL:
		app._notice("Der Zulauf erreicht das Mühlrad. Lies jetzt die Chronik am Ufer.")
		mill._play(app, "res://assets/audio/water.wav", .30)
	else:
		app._notice("Das Wasser läuft am Rad vorbei. Der Hebel hat noch eine Stellung.")
		mill._play(app, "res://assets/audio/select.wav", .30)

static func draw(hud: Control) -> void:
	var app = hud.app
	var mill = current(app)
	if mill == null:
		return
	var base: Vector3 = mill.approach + PROP_OFFSET
	if base.distance_to(app.actor.position) > 12.0 or app.camera.is_position_behind(base):
		return
	var pivot: Vector2 = app.camera.unproject_position(base + Vector3.UP * .34)
	var at: Vector2 = app.camera.unproject_position(base)
	var unit := maxf(10.0, at.distance_to(pivot))
	var arm := Vector2(-float(mill.flow) * unit * .82, -unit * 1.18)
	var tint: Color = app.stage.actor_scene_color(base)
	hud.draw_circle(at, unit * .55, Color(.12, .09, .05, .34))
	hud.draw_line(at, pivot, Color("463827") * tint, unit * .56, true)
	hud.draw_line(at - Vector2(unit * .55, 0), at + Vector2(unit * .55, 0), Color("846446") * tint, unit * .32, true)
	hud.draw_line(pivot, pivot + arm, Color("4f3924") * tint, unit * .28, true)
	hud.draw_line(pivot, pivot + arm * .84, Color("a67b48") * tint, unit * .10, true)
	hud.draw_circle(pivot + arm, unit * .19, Color("d5a967") * tint, true, -1.0, true)
	hud.draw_circle(pivot, unit * .13, Color("c1a281") * tint, true, -1.0, true)

func _process(delta: float) -> void:
	cooldown = maxf(0.0, cooldown - delta)
	var target := 1.0 if route == Route.WHEEL else -1.0
	flow = move_toward(flow, target, delta * 2.7)
	wheel_speed = move_toward(wheel_speed, .95 if route == Route.WHEEL else 0.0, delta * 1.65)
	if wheel_speed > 0.0:
		wheel_angle = fposmod(wheel_angle + wheel_speed * delta, TAU)
		wheel_mix = move_toward(wheel_mix, 1.0, delta * 2.6)
	_update_material()
	if route == Route.SIDE and flow == -1.0 and wheel_speed == 0.0 and cooldown == 0.0:
		set_process(false)

func _update_material() -> void:
	material.set_shader_parameter("mill_flow", flow)
	material.set_shader_parameter("mill_wheel_angle", wheel_angle)
	material.set_shader_parameter("mill_wheel_mix", wheel_mix)

func _play(app, resource: String, gain: float) -> void:
	if not app.effects_on or app.effects_volume <= 0.0:
		return
	if sound == null:
		sound = AudioStreamPlayer.new()
		add_child(sound)
	sound.stream = load(resource)
	sound.volume_db = linear_to_db(app.effects_volume * gain)
	sound.play()
