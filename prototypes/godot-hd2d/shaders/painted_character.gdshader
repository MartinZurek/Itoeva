shader_type spatial;
render_mode unshaded, cull_disabled, depth_test_disabled, depth_draw_never;

uniform sampler2D atlas : source_color, filter_linear, repeat_disable;
uniform sampler2D furniture_mask : filter_nearest, repeat_disable;
uniform bool has_mask = false;
uniform vec3 foot;
uniform vec3 scene_light : source_color = vec3(1.0);
uniform float scene_origin = 0.0;
uniform float scene_span = 30.0;
varying vec3 world_point;

void vertex() {
	// Volles Billboard erhaelt den gezeichneten Aspekt bei der geneigten Kamera.
	mat4 facing = mat4(INV_VIEW_MATRIX[0], INV_VIEW_MATRIX[1], INV_VIEW_MATRIX[2], MODEL_MATRIX[3]);
	MODELVIEW_MATRIX = VIEW_MATRIX * facing;
	world_point = (facing * vec4(VERTEX, 1.0)).xyz;
}

void fragment() {
	vec4 paint = texture(atlas, UV) * COLOR;
	if (paint.a < 0.12) { discard; }
	if (has_mask) {
		vec2 art_uv = vec2((world_point.x - scene_origin) / scene_span,
			0.84 - (0.819152 * world_point.y - 0.573576 * world_point.z) / 10.0);
		vec2 blocker = texture(furniture_mask, art_uv).rg;
		float foot_v = 0.84 + 0.573576 * foot.z / 10.0;
		if (art_uv.x >= 0.0 && art_uv.x <= 1.0 && art_uv.y >= 0.0 && art_uv.y <= 1.0
			&& blocker.g > 0.5 && foot_v < blocker.r - 0.002) { discard; }
	}
	ALBEDO = paint.rgb * scene_light;
	ALPHA = paint.a;
}
