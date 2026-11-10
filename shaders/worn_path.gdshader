shader_type spatial;
render_mode unshaded, blend_mix, depth_draw_never, cull_disabled;
uniform vec4 scene_tint : source_color = vec4(1.0);
uniform sampler2D path_texture : source_color, filter_linear_mipmap;
uniform vec2 source_uv = vec2(.52,.80);
void fragment() {
 float edge = smoothstep(0.0,.16,UV.x) * smoothstep(0.0,.16,1.0-UV.x);
 vec3 earth = texture(path_texture, source_uv + vec2((UV.y-.5)*.08,(UV.x-.5)*.025)).rgb;
 ALBEDO = earth * scene_tint.rgb;
 ALPHA = edge * COLOR.a * .94;
}
