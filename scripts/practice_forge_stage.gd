extends RefCounted

const STONE_TEXTURE := "res://assets/cinematics/forge-slate.png"
const FLOOR_Y := 0.0
const DAIS_Y := 0.13
const SHADOW_SHADER = """
shader_type spatial;
render_mode unshaded, cull_disabled, depth_draw_never;
uniform float strength = 0.7;
void fragment() {
	vec2 point = (UV - vec2(0.5)) * 2.0;
	float distance_to_center = length(point);
	ALBEDO = vec3(0.026, 0.018, 0.016);
	ALPHA = (1.0 - smoothstep(0.14, 1.0, distance_to_center)) * strength;
}
"""

static func build(root: Node3D) -> OmniLight3D:
	var floor_material := stone(Color("9e8880"), Vector2(2.5, 3.2))
	var wall := stone(Color("6c6366"), Vector2(2.0, 1.25))
	var trim := stone(Color("a68b77"), Vector2(1.0, 0.5))
	var iron := material(Color("423e45"))
	var wood := material(Color("382823"))
	var black := material(Color("231920"))
	var copper := material(Color("9a5a38"))
	copper.roughness = 0.67
	var ember := material(Color("9e4125"))
	ember.emission_enabled = true
	ember.emission = Color("f78031")
	ember.emission_energy_multiplier = 0.72

	# World-space dimensions are shared with the creature and camera.
	var floor_mesh := PlaneMesh.new()
	floor_mesh.size = Vector2(8.6, 9.4)
	place(root, floor_mesh, floor_material, Vector3(0, FLOOR_Y, 0.5))
	box(root, Vector3(8.6, 3.5, 0.27), Vector3(0, 1.75, -3.76), wall)
	box(root, Vector3(0.30, 3.5, 9.2), Vector3(-4.20, 1.75, 0.5), wall)
	box(root, Vector3(0.30, 3.5, 9.2), Vector3(4.20, 1.75, 0.5), wall)
	box(root, Vector3(8.4, 0.14, 0.23), Vector3(0, 0.11, -3.51), trim)
	box(root, Vector3(8.5, 0.25, 0.33), Vector3(0, 3.06, -3.48), wood)
	for x in [-3.64, -0.16, 3.63]:
		box(root, Vector3(0.23, 3.1, 0.26), Vector3(x, 1.56, -3.48), wood)

	# Low stone landing with a visible nosing and a real surface under the paws.
	box(root, Vector3(2.87, DAIS_Y, 2.39), Vector3(0, DAIS_Y * 0.5, 0.01), trim)
	box(root, Vector3(2.77, 0.035, 2.33), Vector3(0, 0.032, 0.045), iron)
	for side in [-1.0, 1.0]:
		box(root, Vector3(0.022, 0.009, 2.16), Vector3(1.32 * side, DAIS_Y + 0.003, 0.0), copper)

	var hearth_x := -1.85
	box(root, Vector3(1.98, 0.18, 0.85), Vector3(hearth_x, 0.09, -3.02), trim)
	box(root, Vector3(1.38, 1.05, 0.055), Vector3(hearth_x, 0.69, -3.53), black)
	var hood := CylinderMesh.new()
	hood.top_radius = 0.69
	hood.bottom_radius = 0.69
	hood.height = 0.06
	hood.radial_segments = 30
	var opening := place(root, hood, black, Vector3(hearth_x, 1.12, -3.49))
	opening.rotation.x = PI * 0.5
	for direction in [-1.0, 1.0]:
		for course in 4:
			box(root, Vector3(0.26, 0.239, 0.29),
				Vector3(hearth_x + 0.79 * direction, 0.27 + course * 0.24, -3.37), trim)
	for index in 9:
		var angle := (index + 0.5) * PI / 9.0
		var block := box(root, Vector3(0.24, 0.31, 0.30),
			Vector3(hearth_x + cos(angle) * 0.79, 1.10 + sin(angle) * 0.79, -3.38), trim)
		block.rotation.z = angle - PI * 0.5
	box(root, Vector3(1.12, 0.085, 0.30), Vector3(hearth_x, 0.21, -3.37), ember)
	for coal in 7:
		var coal_mesh := box(root, Vector3(0.13, 0.085 + coal % 3 * 0.04, 0.14),
			Vector3(hearth_x - 0.47 + coal * 0.15, 0.25, -3.23 - coal % 2 * 0.08), ember)
		coal_mesh.rotation.y = coal * 1.23
	var hearth := OmniLight3D.new()
	hearth.position = Vector3(hearth_x, 0.57, -2.89)
	hearth.light_color = Color("ff9150")
	hearth.light_energy = 1.9
	hearth.omni_range = 3.6
	root.add_child(hearth)

	# A bench and an anvil provide readable objects at creature height.
	box(root, Vector3(1.85, 0.12, 0.65), Vector3(1.92, 0.78, -2.95), wood)
	for x in [1.17, 2.67]:
		box(root, Vector3(0.13, 0.74, 0.47), Vector3(x, 0.37, -3.0), wood)
	box(root, Vector3(1.64, 0.09, 0.44), Vector3(1.92, 0.24, -3.00), wood)
	box(root, Vector3(0.36, 0.15, 0.27), Vector3(1.75, 0.91, -2.98), iron)
	box(root, Vector3(0.58, 0.10, 0.30), Vector3(1.78, 1.04, -2.98), iron)
	var horn := PrismMesh.new()
	horn.size = Vector3(0.23, 0.14, 0.22)
	var tip := place(root, horn, iron, Vector3(2.17, 1.02, -2.98))
	tip.rotation.z = -PI * 0.5
	box(root, Vector3(1.62, 0.09, 0.23), Vector3(2.0, 1.57, -3.48), wood)
	for index in 3:
		var bar := box(root, Vector3(0.055, 0.39, 0.045), Vector3(1.56 + index * 0.31, 1.35, -3.31), iron)
		bar.rotation.z = -0.1 + index * 0.12
		box(root, Vector3(0.19, 0.07, 0.065), bar.position + Vector3(0, 0.16, 0), copper)
	return hearth

static func contact_shadow(root: Node3D) -> MeshInstance3D:
	var shader := Shader.new()
	shader.code = SHADOW_SHADER
	var shadow := ShaderMaterial.new()
	shadow.shader = shader
	var mesh := PlaneMesh.new()
	mesh.size = Vector2(1.95, 0.87)
	return place(root, mesh, shadow, Vector3(0, DAIS_Y + 0.008, 0.17))

static func stone(color: Color, repeat: Vector2) -> StandardMaterial3D:
	var result := material(color)
	result.uv1_scale = Vector3(repeat.x, repeat.y, 1.0)
	if ResourceLoader.exists(STONE_TEXTURE):
		result.albedo_texture = load(STONE_TEXTURE)
	return result

static func material(color: Color) -> StandardMaterial3D:
	var result := StandardMaterial3D.new()
	result.albedo_color = color
	result.roughness = 0.88
	return result

static func box(parent: Node3D, size: Vector3, at: Vector3, finish: Material) -> MeshInstance3D:
	var mesh := BoxMesh.new()
	mesh.size = size
	return place(parent, mesh, finish, at)

static func place(parent: Node3D, mesh: Mesh, finish: Material, at: Vector3) -> MeshInstance3D:
	var object := MeshInstance3D.new()
	object.mesh = mesh
	object.material_override = finish
	object.position = at
	parent.add_child(object)
	return object
