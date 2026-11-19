shader_type canvas_item;
uniform sampler2D water_mask : filter_linear, repeat_disable;
uniform sampler2D shrub_mask : filter_linear, repeat_disable;
uniform bool has_water = false;
uniform bool has_shrubs = false;
uniform vec4 scene_tint : source_color = vec4(1.0);

void fragment() {
	vec2 uv = UV;
	float water = has_water ? texture(water_mask, UV).r : 0.0;
	if (water > .01) {
		uv += vec2(sin(TIME*.78 + UV.y*98.0), cos(TIME*.55 + UV.x*76.0)) * .0008 * water;
	}
	float shrubs = has_shrubs ? texture(shrub_mask, UV).r : 0.0;
	if (shrubs > .012) {
		uv.x += sin(TIME*2.25 - UV.x*35.0 + UV.y*20.0) * .0011 * shrubs;
	}
	COLOR = texture(TEXTURE, uv) * scene_tint;
	COLOR.rgb += vec3(.11,.16,.18) * water * .045 * sin(TIME*.7 + UV.y*180.0);
}
