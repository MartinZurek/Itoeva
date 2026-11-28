extends RefCounted

const Catalog = preload("res://scripts/world_catalog.gd")
const BELL_RATE := 22050
static var bell_stream: AudioStreamWAV

static func offers(app: Node3D, grounded: bool) -> Array[Dictionary]:
	var targets: Array[Dictionary] = []
	for landmark: Dictionary in Catalog.data().interiors.get(app.room, {}).get("landmarks", []):
		var uv := Vector2(landmark.uv[0], landmark.uv[1])
		var approach: Vector3 = app.stage.safe_position(Catalog.position_for(app.room, uv), app.actor.collider.shape.radius)
		var distance := Vector2(approach.x - app.actor.position.x, approach.z - app.actor.position.z).length()
		if distance > 9.0: continue
		var center := Vector3(approach.x, app.stage.image_y(landmark.art_y, approach.z, 270.0), approach.z)
		var on_screen: Vector2 = app.camera.unproject_position(center)
		var size := Vector2(52, 68)
		targets.append({
			"kind": "landmark", "id": landmark.id, "label": landmark.label, "action": landmark.action,
			"position": center, "approach": approach, "distance": distance, "landmark": landmark,
			"body_rect": Rect2(on_screen - size * 0.5, size),
			"ready": grounded and distance < 1.55 and app._interaction_clear(approach),
		})
	return targets

static func perform(app: Node3D, target: Dictionary) -> void:
	app._notice(target.landmark.detail)
	if not target.landmark.get("bell", false): return
	app.hud.confirm_pickup(target.position - Vector3.UP * 0.2)
	if not app.effects_on: return
	var player := AudioStreamPlayer3D.new()
	player.stream = _bell()
	player.max_distance = 22.0
	player.volume_db = linear_to_db(maxf(0.001, app.effects_volume * 0.52))
	app.stage.add_child(player)
	player.position = target.position
	player.finished.connect(player.queue_free)
	player.play()

static func _bell() -> AudioStreamWAV:
	if bell_stream != null: return bell_stream
	var count := roundi(BELL_RATE * 1.5)
	var data := PackedByteArray()
	data.resize(count * 2)
	for i in count:
		var time := float(i) / BELL_RATE
		var envelope := minf(1.0, time * 380.0) * exp(-3.6 * time)
		var note := sin(TAU * 220.0 * time) + 0.43 * sin(TAU * 526.0 * time) + 0.24 * sin(TAU * 843.0 * time)
		data.encode_s16(i * 2, roundi(note * envelope * 10000.0))
	bell_stream = AudioStreamWAV.new()
	bell_stream.format = AudioStreamWAV.FORMAT_16_BITS
	bell_stream.mix_rate = BELL_RATE
	bell_stream.data = data
	return bell_stream
