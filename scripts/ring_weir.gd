extends Node3D

const Catalog = preload("res://scripts/world_catalog.gd")
const Story = preload("res://scripts/world_story.gd")
const ROOM := "RING_WEIR"
const RIED_REGION := Rect2(28, 26, 355, 323)

var material: ShaderMaterial
var ried: Vector3
var lever: Vector3

static func mount(app) -> Node3D:
	var weir := new()
	weir.name = "RingWeir"
	app.stage.add_child(weir)
	weir.material = app.stage.painted_materials[0]
	weir.ried = app.stage.safe_position(Catalog.position_for(ROOM, Vector2(0.36, 0.36)))
	weir.lever = app.stage.safe_position(Catalog.position_for(ROOM, Vector2(0.63, 0.38)))
	weir._add_ried(app.stage.actor_scene_color(weir.ried))
	weir._set_flow(1.0 if Story.record(app.save_data, app.selected).get("ring_weir", false) else 0.24)
	return weir

func _add_ried(tint: Color) -> void:
	var sprite := Sprite3D.new()
	sprite.name = "Ried"
	sprite.texture = load("res://assets/characters/ried.webp")
	sprite.region_enabled = true
	sprite.region_rect = RIED_REGION
	sprite.pixel_size = 1.28 / RIED_REGION.size.y
	sprite.offset = Vector2(0.0, RIED_REGION.size.y * 0.5)
	sprite.position = ried
	sprite.billboard = BaseMaterial3D.BILLBOARD_ENABLED
	sprite.alpha_cut = SpriteBase3D.ALPHA_CUT_DISCARD
	sprite.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	sprite.modulate = tint.lerp(Color.WHITE, 0.28)
	add_child(sprite)
	var shadow := MeshInstance3D.new()
	var plane := PlaneMesh.new()
	plane.size = Vector2(0.76, 0.43)
	shadow.mesh = plane
	shadow.position = Vector3(ried.x, 0.019, ried.z)
	var contact := ShaderMaterial.new()
	contact.shader = preload("res://shaders/contact_shadow.gdshader")
	contact.set_shader_parameter("opacity", 0.35)
	shadow.material_override = contact
	shadow.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	add_child(shadow)

func _set_flow(value: float) -> void:
	material.set_shader_parameter("water_activity", value)

func offers(app) -> Array[Dictionary]:
	var result: Array[Dictionary] = []
	var open: bool = Story.record(app.save_data, app.selected).get("ring_weir", false)
	result.append(_target(app, "ried", "Ried · Sprechen", ried, 1.5, 2.4))
	if not open:
		result.append(_target(app, "ring_lever", "Wehrhebel öffnen", lever, 1.25, 1.5))
	return result

func _target(app, kind: String, label: String, point: Vector3, marker_height: float, reach: float) -> Dictionary:
	var distance := Vector2(point.x - app.actor.position.x, point.z - app.actor.position.z).length()
	return {"kind": kind, "id": kind, "label": label, "position": point + Vector3.UP * marker_height,
		"approach": point, "distance": distance, "ready": app.actor.is_on_floor() and not app.actor.mounted and distance < reach}

func speak(app) -> void:
	var open: bool = Story.record(app.save_data, app.selected).get("ring_weir", false)
	if open:
		app._show_activity("Ried · Schleusenwart", "Folge dem Zulauf über die Brücke zur Mühle. Ich kenne eine Karte, die dir unterwegs nützen kann.",
			"Kartenangebot ansehen", app.show_collection.bind("ried"))
		return
	app._show_activity("Ried · Schleusenwart", "Das Zeichen auf deinem Siegel gehört zu diesem Wehr. Leg den Hebel unter der Laterne um. Sobald das Wasser wieder läuft, führt die Brücke zum alten Zulauf der Mühle.",
		"Im Reisetagebuch nachsehen", app.show_journal.bind(true))

func open(app) -> void:
	if not Story.record(app.save_data, app.selected).get("relay", false):
		app._notice("Das Relais im Garten muss zuerst abgestimmt werden.")
		return
	if not Story.mark(app.save_data, app.selected, "ring_weir"):
		return
	create_tween().tween_method(_set_flow, 0.24, 1.0, 0.85)
	if app.effects_on and app.effects_volume > 0.0:
		var sound := AudioStreamPlayer.new()
		sound.stream = load("res://assets/audio/water.wav")
		sound.volume_db = linear_to_db(app.effects_volume * 0.32)
		add_child(sound)
		sound.finished.connect(sound.queue_free)
		sound.play()
	app.context_frame = -1
	app._write_save()
	app._notice("Ringwehr geöffnet · Über die Brücke zum alten Mühlenzulauf")
