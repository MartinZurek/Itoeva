extends RefCounted

const Stage = preload("res://scripts/practice_forge_stage.gd")
const LAVA_SHADER = """
shader_type spatial;
render_mode unshaded;
varying vec2 ground_position;
float hash(vec2 cell) {
	return fract(sin(dot(cell, vec2(127.1, 311.7))) * 43758.5453);
}
float noise(vec2 p) {
	vec2 cell = floor(p);
	vec2 local = fract(p);
	local = local * local * (3.0 - 2.0 * local);
	return mix(mix(hash(cell), hash(cell + vec2(1.0, 0.0)), local.x),
		mix(hash(cell + vec2(0.0, 1.0)), hash(cell + vec2(1.0)), local.x), local.y);
}
void vertex() {
	ground_position = (MODEL_MATRIX * vec4(VERTEX, 1.0)).xz;
}
void fragment() {
	vec2 current = ground_position * vec2(1.08, 2.3) + vec2(TIME * 0.31, 0.0);
	vec2 warp = vec2(noise(current * 0.7), noise(current * 0.82 + vec2(7.0))) * 1.8;
	float crust = noise(current + warp) * 0.71 + noise(current * 2.7 + warp) * 0.29;
	float fissures = (1.0 - smoothstep(0.018, 0.092, abs(crust - 0.48)));
	float hot = fissures * (0.44 + 0.56 * noise(current * 1.5 + vec2(0.0, TIME * 0.36)));
	ALBEDO = mix(vec3(0.155, 0.027, 0.025), vec3(0.91, 0.27, 0.05), hot);
}
"""

static func canyon(root: Node3D) -> void:
	_horizon(root, "res://assets/cinematics/ash-gorge-horizon.png", 50.0, Vector3(1.0, 4.9, -15.7))
	var basalt := Stage.stone(Color("70616a"), Vector2(1.2, 1.8))
	var ash := Stage.stone(Color("503b46"), Vector2(2.0, 2.0))
	var coal := _glowing(Color("ce4a25"), 0.38)
	var river := PlaneMesh.new()
	river.size = Vector2(34.0, 6.1)
	var river_shader := Shader.new()
	river_shader.code = LAVA_SHADER
	var lava := ShaderMaterial.new()
	lava.shader = river_shader
	Stage.place(root, river, lava, Vector3(3.0, -2.23, -3.1))
	Stage.box(root, Vector3(10.6, 0.65, 5.4), Vector3(-3.18, -0.21, 1.98), ash)
	Stage.box(root, Vector3(2.5, 0.12, 1.8), Vector3(-0.94, 0.07, 0.08), basalt)
	for index in 5:
		var ledge := Stage.place(root, _rock(30 + index), ash, Vector3(1.62 + sin(index * 1.7) * 0.34, -1.56, -0.31 + index * 0.84))
		ledge.scale = Vector3(0.62 + _variation(index + 7) * 0.43, 1.27 + _variation(index + 71) * 0.26, 0.82)
		ledge.rotation.y = index * 0.89
	for index in 14:
		var x := -8.0 + float(index) * 1.48 + sin(index * 5.2) * 0.46
		var depth := -4.25 - _variation(index * 7 + 1) * 3.1
		var rock := Stage.place(root, _rock(index), basalt if index % 3 != 1 else ash, Vector3(x, -2.17, depth))
		rock.scale = Vector3(0.82 + _variation(index + 17) * 0.72, 1.4 + _variation(index + 51) * 2.9, 0.82 + _variation(index + 33))
		rock.rotation.y = index * 1.97
	for index in 12:
		var ember := Stage.box(root, Vector3(0.11 + index % 3 * 0.05, 0.012, 0.025),
			Vector3(-1.92 + float(index) * 0.17, 0.135, 0.39 + sin(float(index) * 1.77) * 0.25), coal)
		ember.rotation.y = index * 1.79

static func observatory(root: Node3D) -> Node3D:
	_horizon(root, "res://assets/cinematics/observatory-horizon.png", 41.0, Vector3(0, 4.25, -17.5))
	var slate := Stage.stone(Color("737d9c"), Vector2(1.65, 1.65))
	var border := Stage.material(Color("505d76"))
	var brass := Stage.material(Color("b09869"))
	brass.metallic = 0.36
	brass.roughness = 0.51
	var star := _glowing(Color("aacdf6"), 0.85)
	var terrace := CylinderMesh.new()
	terrace.top_radius = 3.35
	terrace.bottom_radius = 3.65
	terrace.height = 0.29
	terrace.radial_segments = 48
	Stage.place(root, terrace, slate, Vector3(0, -0.147, 0))
	Stage.box(root, Vector3(2.45, 0.13, 1.80), Vector3(0, 0.065, 0.15), border)
	for step in 3:
		Stage.box(root, Vector3(2.5 + step * 0.16, 0.12, 0.29),
			Vector3(0, -step * 0.09, 1.14 + step * 0.28), slate)
	for index in 11:
		var angle := 0.16 + PI * index / 10.0
		var point := Vector3(cos(angle) * 3.09, 0.23, -sin(angle) * 3.09)
		var post := Stage.box(root, Vector3(0.19, 0.49, 0.20), point, slate)
		post.rotation.y = angle
		if index < 10:
			var next := angle + PI / 10.0
			var join := Vector3(cos(next) * 3.09, point.y, -sin(next) * 3.09)
			_rod(root, point + Vector3.UP * 0.20, join + Vector3.UP * 0.20, 0.027, brass)
	var bases: Array[Vector3] = [Vector3(-2.76, 0.0, -1.08), Vector3(2.76, 0.0, -1.08)]
	for point in bases:
		Stage.box(root, Vector3(0.55, 0.20, 0.53), point + Vector3.UP * 0.1, border)
		Stage.box(root, Vector3(0.28, 2.10, 0.28), point + Vector3.UP * 1.16, slate)
		Stage.box(root, Vector3(0.43, 0.15, 0.42), point + Vector3.UP * 2.19, brass)
	for index in 13:
		var a := index * PI / 13.0
		var b := (index + 1) * PI / 13.0
		var first := Vector3(cos(a) * 2.76, 2.22 + sin(a) * 1.17, -1.08)
		var last := Vector3(cos(b) * 2.76, 2.22 + sin(b) * 1.17, -1.08)
		var stone := Stage.box(root, Vector3(first.distance_to(last) - 0.016, 0.18, 0.24), (first + last) * 0.5, slate)
		stone.rotation.z = atan2(last.y - first.y, last.x - first.x)
	for index in 32:
		var x := _variation(index * 3 + 1) * 26.0 - 13.0
		var y := 0.85 + _variation(index * 5 + 9) * 7.5
		Stage.box(root, Vector3.ONE * (0.012 + 0.015 * (index % 3)), Vector3(x, y, -13.0 - index % 3), star)
	var orrery := Node3D.new()
	orrery.position = Vector3(0, 1.61, -0.98)
	root.add_child(orrery)
	for index in 3:
		var track := TorusMesh.new()
		track.inner_radius = 0.72 + index * 0.17
		track.outer_radius = track.inner_radius + 0.025
		track.rings = 38
		track.ring_segments = 5
		var hoop := Stage.place(orrery, track, brass, Vector3.ZERO)
		hoop.rotation = Vector3(0.3 + index * 0.69, index * 0.33, 0.28 * index)
	return orrery

static func _horizon(root: Node3D, path: String, width: float, at: Vector3) -> void:
	if not ResourceLoader.exists(path):
		return
	var horizon := Sprite3D.new()
	horizon.texture = load(path)
	horizon.pixel_size = width / horizon.texture.get_width()
	horizon.position = at
	horizon.shaded = false
	root.add_child(horizon)

static func _variation(seed_value: int) -> float:
	return fposmod(sin(float(seed_value) * 12.9898) * 43758.5453, 1.0)

static func _rock(seed_value: int) -> ArrayMesh:
	const SIDES := 8
	var points: Array[Vector3] = []
	for level in 3:
		for index in SIDES:
			var a := index * TAU / SIDES
			var scale := (1.0 - level * 0.24) * (0.76 + _variation(seed_value * 31 + index * 7 + level * 3) * 0.55)
			var y := level * 0.44 + _variation(seed_value * 15 + index * 13 + level) * 0.22
			points.append(Vector3(cos(a) * scale + level * 0.11, y, sin(a) * scale))
	var surface := SurfaceTool.new()
	surface.begin(Mesh.PRIMITIVE_TRIANGLES)
	for level in 2:
		for index in SIDES:
			var next := (index + 1) % SIDES
			var ring := level * SIDES
			var upper := ring + SIDES
			_triangle(surface, points[ring + index], points[upper + next], points[upper + index])
			_triangle(surface, points[ring + index], points[ring + next], points[upper + next])
	for index in SIDES:
		_triangle(surface, points[2 * SIDES + index], points[2 * SIDES + (index + 1) % SIDES], Vector3(0.19, 0.94, 0))
	surface.generate_normals()
	return surface.commit()

static func _triangle(surface: SurfaceTool, a: Vector3, b: Vector3, c: Vector3) -> void:
	for vertex in [a, b, c]:
		surface.set_uv(Vector2(vertex.x * 0.38 + vertex.z * 0.29, vertex.y))
		surface.add_vertex(vertex)

static func _rod(root: Node3D, first: Vector3, last: Vector3, radius: float, material: Material) -> void:
	var mesh := CylinderMesh.new()
	mesh.top_radius = radius
	mesh.bottom_radius = radius
	mesh.height = first.distance_to(last)
	mesh.radial_segments = 6
	var rod := Stage.place(root, mesh, material, (first + last) * 0.5)
	rod.quaternion = Quaternion(Vector3.UP, (last - first).normalized())

static func _glowing(color: Color, amount: float) -> StandardMaterial3D:
	var material := Stage.material(color)
	material.emission_enabled = true
	material.emission = color
	material.emission_energy_multiplier = amount
	return material
