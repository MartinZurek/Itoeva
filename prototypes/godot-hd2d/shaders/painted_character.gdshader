shader_type spatial;
render_mode unshaded, cull_disabled, depth_test_disabled, depth_draw_never;
#include "res://shaders/firelight.gdshaderinc"

uniform sampler2D atlas : source_color, filter_linear, repeat_disable;
uniform vec4 frame_clip = vec4(0.0, 0.0, 1.0, 1.0);
uniform bool has_pose_region = false;
uniform vec4 pose_cell;
uniform vec4 pose_region;
uniform vec4 pose_geometry = vec4(1.0, 1.0, 0.0, 0.0);
uniform sampler2D pose_mask : filter_nearest, repeat_disable;
uniform float pose_id = 0.0;
uniform int gait_mode = 0;
uniform vec2 gait_reference;
uniform vec2 gait_offset;
uniform vec4 gait_window;
uniform float gait_reference_id = 0.0;
uniform float gait_body_bob = 0.0;
uniform sampler2D gait_flow : filter_linear, repeat_disable;
uniform vec2 gait_flow_cell;
uniform float gait_fraction = 0.0;
uniform vec2 gait_next;
uniform vec2 gait_next_offset;
uniform float gait_next_id = 0.0;
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
varying vec2 paint_uv;

void vertex() {
	// Geometrie und UVs folgen dem vollständigen Motiv statt dem starren Raster.
	paint_uv = UV;
	if (has_pose_region) {
		VERTEX.xy = VERTEX.xy * pose_geometry.xy + pose_geometry.zw;
		paint_uv = pose_region.xy + (UV - pose_cell.xy) / pose_cell.zw * pose_region.zw;
	}
	// Volles Billboard erhaelt den gezeichneten Aspekt bei der geneigten Kamera.
	mat4 facing = mat4(INV_VIEW_MATRIX[0], INV_VIEW_MATRIX[1], INV_VIEW_MATRIX[2], MODEL_MATRIX[3]);
	MODELVIEW_MATRIX = VIEW_MATRIX * facing;
	world_point = (facing * vec4(VERTEX, 1.0)).xyz;
}

vec4 own_pose(vec2 uv, float id) {
	if (any(lessThan(uv, vec2(0.0))) || any(greaterThan(uv, vec2(1.0)))) { return vec4(0.0); }
	if (has_pose_region && abs(texture(pose_mask, uv).r - id) > 0.0019) { return vec4(0.0); }
	return texture(atlas, uv);
}

void fragment() {
	if (any(lessThan(paint_uv, frame_clip.xy)) || any(greaterThan(paint_uv, frame_clip.xy + frame_clip.zw))) { discard; }
	vec4 paint;
	if (gait_mode > 0) {
		vec2 dimensions = vec2(textureSize(atlas, 0));
		vec2 local = (paint_uv - pose_cell.xy) * dimensions;
		float feather = gait_mode == 2 ? 16.0 : 7.0;
		vec2 low = smoothstep(gait_window.xy, gait_window.xy + vec2(feather), local);
		vec2 high = 1.0 - smoothstep(gait_window.xy + gait_window.zw - vec2(feather), gait_window.xy + gait_window.zw, local);
		float moving = low.x * low.y * high.x * high.y;
		if (gait_mode == 2) { moving = 1.0 - moving; }
		vec4 body = own_pose(gait_reference + (local + vec2(0.0, gait_body_bob)) / dimensions, gait_reference_id);
		vec2 flow_uv = clamp(local / (pose_cell.zw * dimensions), vec2(0.5 / 64.0), vec2(63.5 / 64.0));
		vec4 flow = (texture(gait_flow, gait_flow_cell + flow_uv / vec2(8.0, 3.0)) * 255.0 - 128.0) / 4.0;
		vec4 a = own_pose(paint_uv + gait_offset - flow.rg * gait_fraction / dimensions, pose_id);
		vec4 b = own_pose(gait_next + local / dimensions + gait_next_offset - flow.ba * (1.0 - gait_fraction) / dimensions, gait_next_id);
		float limb_alpha = mix(a.a, b.a, gait_fraction);
		vec4 limbs = vec4(mix(a.rgb * a.a, b.rgb * b.a, gait_fraction) / max(limb_alpha, 0.0001), limb_alpha);
		// Vormultiplizierte Deckung vermeidet dunkle oder doppelte Konturen am Ansatz.
		float alpha = mix(body.a, limbs.a, moving);
		paint = vec4(mix(body.rgb * body.a, limbs.rgb * limbs.a, moving) / max(alpha, 0.0001), alpha);
	} else {
		paint = own_pose(paint_uv, pose_id);
	}
	paint *= COLOR;
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
