extends Node2D
const PoseGeometry = preload("res://scripts/battle_pose_geometry.gd")
var pose_mesh: Mesh
var picture: Texture2D
var source: = Rect2()
var dimensions: = Vector2.ONE
var tint: = Color.WHITE
var facing_left: = false
var light_values: Dictionary = {}
func _init() -> void :
	material = ShaderMaterial.new();material.shader = load("res://shaders/battle_portrait.gdshader")
func present(texture: Texture2D, region: Rect2, size: Vector2, color: Color, left: bool, light: Dictionary) -> void :
	var source_changed: = picture != texture or source != region
	var picture_changed: = source_changed or dimensions != size or tint != color or facing_left != left


	picture = texture;source = region;dimensions = size;tint = color;facing_left = left;visible = true
	if source_changed:
		pose_mesh = PoseGeometry.mesh_for(texture, region)
		var texture_size: = Vector2(texture.get_width(), texture.get_height())
		material.set_shader_parameter("source_uv", Vector4(region.position.x / texture_size.x, region.position.y / texture_size.y, region.size.x / texture_size.x, region.size.y / texture_size.y))
	for key in ["ambient", "key_color", "key_gain", "pulse_color", "pulse_gain"]:
		if not light_values.has(key) or light_values[key] != light[key]:
			material.set_shader_parameter(key, light[key]);light_values[key] = light[key]
	var direction: = Vector2(0.7, - 0.7) if left else Vector2( - 0.7, - 0.7)
	if light_values.get("direction", Vector2.ZERO) != direction:
		material.set_shader_parameter("key_direction", direction);light_values.direction = direction
	if picture_changed: queue_redraw()
func _draw() -> void :
	if picture == null: return
	draw_set_transform(Vector2.ZERO, 0, Vector2(-1, 1) if facing_left else Vector2.ONE)
	if pose_mesh != null: draw_mesh(pose_mesh, picture, Transform2D(0, dimensions, 0, Vector2.ZERO), tint)
	else: draw_texture_rect_region(picture, Rect2(Vector2( - dimensions.x * 0.5, - dimensions.y), dimensions), source, tint)
