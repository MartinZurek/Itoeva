shader_type spatial;
render_mode unshaded, blend_mix, depth_draw_never, cull_disabled;
void fragment() {
	float radius = length((UV - vec2(0.5)) * 2.0);
	ALBEDO = vec3(0.14, 0.11, 0.08);
	ALPHA = (1.0 - smoothstep(0.12, 1.0, radius)) * 0.28;
}
