extends Node3D

const WORLD_WIDTH := 30.0
const IMAGE_HEIGHT := 10.0
const PITCH := 35.0
const PATH_BACK_V := 0.76
const PATH_FRONT_V := 0.93
const ART_ORIGIN_V := 0.84
var camera: Camera3D
var actor: CharacterBody3D
var hud: Control
var camera_anchor := Vector3(-3.8, 3.0, 0.0)
var zoom := 7.6
var yaw := 0.0
var dusk := false
var sun: DirectionalLight3D
var lamp: OmniLight3D
var environment: Environment
var art_material: StandardMaterial3D

func _ready() -> void:
	_bind_inputs()
	actor = $Fennec
	camera = $Camera
	_make_art_geometry()
	_make_collisions()
	_make_lighting()
	var hud_script := load("res://scripts/touch_hud.gd")
	hud = Control.new()
	hud.set_script(hud_script)
	var canvas := CanvasLayer.new()
	add_child(canvas)
	canvas.add_child(hud)
	hud.actor = actor
	hud.world = self
	_update_camera(1.0)

func _bind_inputs() -> void:
	var actions := {"move_left": [KEY_A, KEY_LEFT], "move_right": [KEY_D, KEY_RIGHT],
		"move_back": [KEY_W, KEY_UP], "move_front": [KEY_S, KEY_DOWN],
		"sprint": [KEY_SHIFT], "jump": [KEY_SPACE], "orbit_left": [KEY_Q],
		"orbit_right": [KEY_E], "zoom_in": [KEY_Z], "zoom_out": [KEY_X], "dusk": [KEY_L]}
	for action in actions:
		if not InputMap.has_action(action):
			InputMap.add_action(action)
		for key in actions[action]:
			var event := InputEventKey.new()
			event.physical_keycode = key
			InputMap.action_add_event(action, event)

func path_z(v: float) -> float:
	return (v - ART_ORIGIN_V) * IMAGE_HEIGHT / sin(deg_to_rad(PITCH))

func _art_point(uv: Vector2, ground: bool) -> Vector3:
	var x := (uv.x - 0.5) * WORLD_WIDTH
	if ground:
		return Vector3(x, 0.0, path_z(uv.y))
	# Rueckwand wird zu einem reliefartigen Buehnenbild. Die Referenzprojektion
	# erhaelt jeden Originalpixel; Tiefenstaffelung zeigt sich beim Schwenken.
	var near_z := path_z(PATH_BACK_V)
	var sky_depth := smoothstep(0.72, 0.30, uv.y)
	var open_meadow := smoothstep(0.44, 0.58, uv.x) * (1.0 - smoothstep(0.74, 0.82, uv.x))
	var z := near_z - sky_depth * (1.4 + open_meadow * 4.0)
	var screen_y := (ART_ORIGIN_V - uv.y) * IMAGE_HEIGHT
	var y := (screen_y + sin(deg_to_rad(PITCH)) * z) / cos(deg_to_rad(PITCH))
	return Vector3(x, y, z)

func _make_art_geometry() -> void:
	art_material = StandardMaterial3D.new()
	art_material.albedo_texture = load("res://assets/street-park-forest.png")
	art_material.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	art_material.cull_mode = BaseMaterial3D.CULL_DISABLED
	art_material.texture_filter = BaseMaterial3D.TEXTURE_FILTER_LINEAR_WITH_MIPMAPS
	_make_grid("PaintedRelief", 0.0, PATH_BACK_V, false, 160, 64)
	_make_grid("WalkablePaintedGround", PATH_BACK_V, PATH_FRONT_V, true, 80, 8)
	var cliff := SurfaceTool.new()
	cliff.begin(Mesh.PRIMITIVE_TRIANGLES)
	var front_z := path_z(PATH_FRONT_V)
	var points := [Vector3(-15, 0, front_z), Vector3(15, 0, front_z), Vector3(15, -1.0, front_z), Vector3(-15, -1.0, front_z)]
	var uvs := [Vector2(0, PATH_FRONT_V), Vector2(1, PATH_FRONT_V), Vector2(1, 1), Vector2(0, 1)]
	for i in [0, 1, 2, 0, 2, 3]:
		cliff.set_uv(uvs[i])
		cliff.add_vertex(points[i])
	cliff.generate_normals()
	var mesh := MeshInstance3D.new()
	mesh.name = "PaintedStoneEdge"
	mesh.mesh = cliff.commit()
	mesh.material_override = art_material
	add_child(mesh)

func _make_grid(label: String, v0: float, v1: float, ground: bool, columns: int, rows: int) -> void:
	var surface := SurfaceTool.new()
	surface.begin(Mesh.PRIMITIVE_TRIANGLES)
	for row in rows:
		for col in columns:
			for corner in [Vector2(0, 0), Vector2(1, 0), Vector2(1, 1), Vector2(0, 0), Vector2(1, 1), Vector2(0, 1)]:
				var uv := Vector2((col + corner.x) / columns, lerpf(v0, v1, (row + corner.y) / rows))
				surface.set_uv(uv)
				surface.add_vertex(_art_point(uv, ground))
	surface.generate_normals()
	var mesh := MeshInstance3D.new()
	mesh.name = label
	mesh.mesh = surface.commit()
	mesh.material_override = art_material
	mesh.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	add_child(mesh)

func _box(label: String, pos: Vector3, size: Vector3) -> void:
	var body := StaticBody3D.new()
	body.name = label
	body.position = pos
	var shape := CollisionShape3D.new()
	var box := BoxShape3D.new()
	box.size = size
	shape.shape = box
	body.add_child(shape)
	add_child(body)

func _make_collisions() -> void:
	var back := path_z(PATH_BACK_V)
	var front := path_z(PATH_FRONT_V)
	_box("Ground", Vector3(0, -0.15, (back + front) / 2.0), Vector3(30, 0.3, front - back))
	_box("BackBoundary", Vector3(0, 2, back - 0.12), Vector3(30, 4, 0.24))
	_box("FrontBoundary", Vector3(0, 2, front + 0.12), Vector3(30, 4, 0.24))
	_box("LeftBoundary", Vector3(-15, 2, 0), Vector3(0.24, 4, 4))
	_box("RightBoundary", Vector3(15, 2, 0), Vector3(0.24, 4, 4))
	# Anker aus der gemalten Parkbank; Sitzhoehe passend zur Spritegroesse.
	_box("ParkBench", Vector3(-3.15, 0.48, back + 0.30), Vector3(2.25, 0.96, 0.46))
	_box("LampBase", Vector3(-7.85, 0.45, back + 0.12), Vector3(0.30, 0.9, 0.38))

func _make_lighting() -> void:
	var world_environment := WorldEnvironment.new()
	environment = Environment.new()
	environment.background_mode = Environment.BG_COLOR
	environment.background_color = Color("81a6bf")
	environment.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	environment.ambient_light_color = Color("fff0d2")
	environment.ambient_light_energy = 0.65
	world_environment.environment = environment
	add_child(world_environment)
	sun = DirectionalLight3D.new()
	sun.rotation_degrees = Vector3(-48, -28, 0)
	sun.light_color = Color("ffe3ac")
	sun.light_energy = 0.8
	sun.shadow_enabled = true
	add_child(sun)
	lamp = OmniLight3D.new()
	lamp.position = Vector3(-7.85, 2.9, -0.9)
	lamp.light_color = Color("ffb85a")
	lamp.light_energy = 1.8
	lamp.omni_range = 5.0
	add_child(lamp)
	# Licht faellt auf echte Geometrie: der hauchduenne Empfangsboden laesst
	# die Originalfarbe durch und macht nur die warmen Lichtanteile sichtbar.
	var receiver := MeshInstance3D.new()
	var plane := PlaneMesh.new()
	plane.size = Vector2(30, path_z(PATH_FRONT_V) - path_z(PATH_BACK_V))
	receiver.mesh = plane
	receiver.position = Vector3(0, 0.003, (path_z(PATH_FRONT_V) + path_z(PATH_BACK_V)) / 2.0)
	var material := StandardMaterial3D.new()
	material.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	material.albedo_color = Color(1, 0.90, 0.74, 0.06)
	material.roughness = 1.0
	receiver.material_override = material
	receiver.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	add_child(receiver)

func toggle_dusk() -> void:
	dusk = not dusk
	art_material.albedo_color = Color(0.70, 0.66, 0.77) if dusk else Color.WHITE
	actor.sprite.modulate = Color(0.88, 0.78, 0.73) if dusk else Color.WHITE
	sun.light_energy = 0.18 if dusk else 0.8
	lamp.light_energy = 3.8 if dusk else 1.8

func _process(delta: float) -> void:
	yaw = clampf(yaw + Input.get_axis("orbit_left", "orbit_right") * delta * 8.0, -8.0, 8.0)
	zoom = clampf(zoom + Input.get_axis("zoom_in", "zoom_out") * delta * 3.0, 6.8, 9.2)
	if Input.is_action_just_pressed("dusk"):
		toggle_dusk()
	_update_camera(delta)

func _update_camera(delta: float) -> void:
	var aspect := get_viewport().get_visible_rect().size.aspect()
	var half_width := camera.size * aspect * 0.5
	var limit := maxf(0.0, WORLD_WIDTH * 0.5 - half_width + 0.2)
	# Der untere Bildrand bleibt auch beim Zoom/Schwenken unterhalb des Viewports.
	var screen_center := camera.size * 0.5 - 1.35 + absf(sin(deg_to_rad(yaw))) * half_width * sin(deg_to_rad(PITCH))
	var target := Vector3(clampf(actor.position.x, -limit, limit), screen_center / cos(deg_to_rad(PITCH)), 0)
	camera_anchor = camera_anchor.lerp(target, 1.0 - exp(-delta * 4.5))
	var offset := Vector3(0, sin(deg_to_rad(PITCH)) * 20, cos(deg_to_rad(PITCH)) * 20).rotated(Vector3.UP, deg_to_rad(yaw))
	camera.position = camera_anchor + offset
	camera.look_at(camera_anchor)
	camera.size = lerpf(camera.size, zoom, 1.0 - exp(-delta * 5.0))
