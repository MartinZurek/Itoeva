shader_type spatial;
render_mode unshaded, cull_disabled, depth_test_disabled, depth_draw_never;
#include "res://shaders/firelight.gdshaderinc"

uniform sampler2D atlas : source_color, filter_linear, repeat_disable;
uniform sampler2D furniture_mask : filter_nearest, repeat_disable;
uniform bool has_mask = false;
uniform vec3 foot;
uniform vec3 scene_light : source_color = vec3(1.0);
uniform float scene_origin = 0.0;
uniform float scene_span = 30.0;
uniform int local_light_count = 0;
uniform vec4 local_light_points[4];
uniform vec4 local_light_colors[4];
uniform vec4 local_light_phases[4];
uniform float animation_time = -1.0;
uniform float self_glow = 0.0;
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
	vec3 illumination=vec3(0.0);
	float clock=animation_time>=0.0 ? animation_time : TIME;
	vec2 projected=vec2(world_point.x,.819152*world_point.y-.573576*world_point.z);
	for(int i=0;i<4;i++) {
	 if(i>=local_light_count) {break;}
	 vec4 source=local_light_points[i];
	 vec2 distance=(projected-source.xy)/max(source.zw,vec2(.01));
	 float pulse=local_light_phases[i].y>.5 ? firelight_flicker(clock,local_light_phases[i].x) : 1.0;
	 // Ein begrenzter Farbsaum folgt der Quelle, nicht der Blickrichtung des Wesens.
	 float side=mix(.55,1.0,smoothstep(-.45,.45,(source.x-foot.x)*(world_point.x-foot.x)));
	 illumination+=local_light_colors[i].rgb*local_light_colors[i].w*firelight_pool(distance)*pulse*side;
	}
	ALBEDO = paint.rgb * scene_light + min(illumination,vec3(.38))*(.28+paint.rgb*.60);
	ALBEDO = mix(ALBEDO,paint.rgb*vec3(1.0,.98,.90),self_glow*(.98+.02*sin(clock*1.7)));
	ALPHA = paint.a;
}
