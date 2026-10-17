extends Node2D
var picture: Texture2D
var source:=Rect2()
var dimensions:=Vector2.ONE
var tint:=Color.WHITE
var facing_left:=false
func _init() -> void:
 material=ShaderMaterial.new();material.shader=load("res://shaders/battle_portrait.gdshader")
func present(texture: Texture2D, region: Rect2, size: Vector2, color: Color, left: bool, light: Dictionary) -> void:
 picture=texture;source=region;dimensions=size;tint=color;facing_left=left;visible=true
 var texture_size:=Vector2(texture.get_width(),texture.get_height())
 material.set_shader_parameter("source_uv",Vector4(region.position.x/texture_size.x,region.position.y/texture_size.y,region.size.x/texture_size.x,region.size.y/texture_size.y))
 for key in ["ambient","key_color","key_gain","pulse_color","pulse_gain"]:material.set_shader_parameter(key,light[key])
 material.set_shader_parameter("key_direction",Vector2(.7,-.7) if left else Vector2(-.7,-.7))
 queue_redraw()
func _draw() -> void:
 if picture==null:return
 draw_set_transform(Vector2.ZERO,0,Vector2(-1,1) if facing_left else Vector2.ONE)
 draw_texture_rect_region(picture,Rect2(Vector2(-dimensions.x*.5,-dimensions.y),dimensions),source,tint)
