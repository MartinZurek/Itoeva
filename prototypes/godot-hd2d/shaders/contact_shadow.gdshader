shader_type spatial;
render_mode unshaded, blend_mix, depth_draw_never, cull_disabled;
uniform float opacity = 0.34;
void fragment() {
	float radius = length((UV - vec2(0.5)) * 2.0);
	ALBEDO = vec3(0.14, 0.11, 0.08);
	ALPHA = (exp(-radius * radius * 5.0) * 0.68 + exp(-radius * radius * 28.0) * 0.32) * (1.0 - smoothstep(0.75, 1.0, radius)) * opacity;
}
