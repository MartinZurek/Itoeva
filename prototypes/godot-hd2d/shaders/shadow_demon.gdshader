shader_type spatial;
render_mode unshaded, cull_disabled, depth_draw_never;
uniform sampler2D demon_texture : source_color, filter_linear, repeat_disable;
uniform float reveal = 0.0;
uniform float hit = 0.0;
uniform float animation_time = -1.0;
void fragment() {
 float t = animation_time >= 0.0 ? animation_time : TIME;
 float frame = mod(floor(t*5.5),4.0);
 vec2 uv = vec2((UV.x*328.0+frame*328.0)/1312.0,UV.y*304.0/1199.0);
 vec4 paint = texture(demon_texture,uv);
 ALBEDO = mix(paint.rgb*1.12,vec3(1.0,.89,.7),hit*.6);
 ALPHA = paint.a*reveal;
}
