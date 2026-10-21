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
uniform float gait_reference_id = 0.0;
uniform sampler2D step_mask : filter_linear, repeat_disable;
uniform int step_row = 0;
uniform int step_leg_count = 2;
uniform vec2 step_hip_2;
uniform vec2 step_hip_3;
uniform vec2 step_foot_2;
uniform vec2 step_foot_3;
uniform vec2 step_offset_2;
uniform vec2 step_offset_3;
uniform vec2 step_hip_0;
uniform vec2 step_hip_1;
uniform vec2 step_foot_0;
uniform vec2 step_foot_1;
uniform vec2 step_offset_0;
uniform vec2 step_offset_1;
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
#include "res://shaders/full_body.gdshaderinc"
varying vec3 world_point;
varying vec2 paint_uv;

void vertex() {
	// Geometrie und UVs folgen dem vollständigen Motiv statt dem starren Raster.
	skin_surface=COLOR.r<.5 ? 1.0 : 0.0;
	paint_uv = UV;
	if(skin_surface>.5) {
	 vec2 source=vec2(VERTEX.x+128.0,128.0-VERTEX.y);
	 vec2 posed=skin_point(source);
	 paint_uv=gait_reference+source/vec2(textureSize(atlas,0));
	 VERTEX.xy=vec2((posed.x-128.0)*rig_flip,128.0+rig_ground_offset-posed.y)*rig_pixel_size;
	 COLOR=vec4(1.0);
	}
	if (has_pose_region && skin_surface<.5) {
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

vec4 step_weights(vec2 p) {
 if (any(lessThan(p, vec2(0.0))) || any(greaterThan(p, vec2(256.0)))) { return vec4(0.0); }
 return texture(step_mask, (p + vec2(0.0, float(step_row)*256.0)) / vec2(256.0,768.0));
}

vec2 step_source(vec2 p, vec2 hip, vec2 foot_point, vec2 offset) {
 // Unterhalb des Sprunggelenks bleibt der Fuss ein starrer Ausschnitt.
 // Nur die Verbindung zur Huefte folgt der Gewichtsverlagerung.
 float start=hip.y;
 float span=max(24.0,foot_point.y-8.0-start);
 float low=min(p.y,p.y-offset.y);
 float high=max(p.y,p.y-offset.y);
 // Monotone Umkehrung statt instabiler Iteration: auch eine kurze Sternspitze
 // darf sich beim grossen Hoehenversatz nicht vom Koerper abtrennen.
 for (int i=0; i<14; i++) {
  float mid=(low+high)*.5;
  float value=mid+offset.y*smoothstep(start,start+span,mid);
  if (value<p.y) { low=mid; } else { high=mid; }
 }
 float y=(low+high)*.5;
 float weight=smoothstep(start,start+span,y);
 return vec2(p.x-offset.x*weight,y);
}

vec2 ray_source(vec2 p,vec2 hip,float angle) {
 vec2 source=p;
 for (int i=0;i<7;i++) {
  float a=-angle*smoothstep(hip.y,hip.y+28.0,source.y);
  source=hip+mat2(vec2(cos(a),sin(a)),vec2(-sin(a),cos(a)))*(p-hip);
 }
 return source;
}

vec4 step_paint(vec2 local, vec2 dimensions) {
 if (gait_mode==4) {
  // Ein stetiges Feld auf EINER Zeichnung vermeidet ausgeschnittene Schulter-
  // oder Fluegelrisse. Breite technische Saeume halten Kopf und Ohren ruhig.
  vec2 weights=step_weights(local).rg;
  float flap=(local.x-step_hip_0.x)*sin(step_offset_0.x)*weights.r
    +(local.x-step_hip_1.x)*sin(step_offset_1.x)*weights.g;
  return own_pose(gait_reference+(local-vec2(0.0,flap))/dimensions,gait_reference_id);
 }
 float first_joint=min(step_hip_0.y,step_hip_1.y);
 if (step_leg_count==4) { first_joint=min(first_joint,min(step_hip_2.y,step_hip_3.y)); }
 // Der grosse ruhige Kopfbereich braucht weder Beinmasken noch deren Umkehrung.
 if (gait_mode!=4 && local.y<first_joint) {
  return own_pose(gait_reference+local/dimensions,gait_reference_id);
 }
 vec2 a_uv;
 vec2 b_uv;
 if (gait_mode==5) {
  a_uv=ray_source(local,step_hip_0,step_offset_0.x);
  b_uv=ray_source(local,step_hip_1,step_offset_1.x);
 } else {
  a_uv=step_source(local,step_hip_0,step_foot_0,step_offset_0);
  b_uv=step_source(local,step_hip_1,step_foot_1,step_offset_1);
 }
 vec4 weights=step_weights(local);
 vec4 body=own_pose(gait_reference+local/dimensions,gait_reference_id);
 vec4 a=own_pose(gait_reference+a_uv/dimensions,gait_reference_id);
 vec4 b=own_pose(gait_reference+b_uv/dimensions,gait_reference_id);
 body.a*=1.0-clamp(weights.x+weights.y+(step_leg_count==4 ? weights.z+weights.w : 0.0),0.0,1.0);
 a.a*=step_weights(a_uv).r;
 b.a*=step_weights(b_uv).g;
 // Zusammengehoerige Teildeckungen addieren sich am Gelenk zum Original;
 // ohne die halbdeckenden Saeume mehrfach uebereinanderzulegen.
 float coverage=body.a+a.a+b.a;
 vec3 premult=body.rgb*body.a+a.rgb*a.a+b.rgb*b.a;
 if (step_leg_count==4) {
  vec2 c_uv=step_source(local,step_hip_2,step_foot_2,step_offset_2);
  vec2 d_uv=step_source(local,step_hip_3,step_foot_3,step_offset_3);
  vec4 c=own_pose(gait_reference+c_uv/dimensions,gait_reference_id);
  vec4 d=own_pose(gait_reference+d_uv/dimensions,gait_reference_id);
  c.a*=step_weights(c_uv).b;
  d.a*=step_weights(d_uv).a;
  coverage+=c.a+d.a;
  premult+=c.rgb*c.a+d.rgb*d.a;
 }
 return vec4(premult/max(coverage,0.0001),min(1.0,coverage));
}

void fragment() {
	if(rig_active && skin_surface<.5) {discard;}
	if (skin_surface<.5 && (any(lessThan(paint_uv, frame_clip.xy)) || any(greaterThan(paint_uv, frame_clip.xy + frame_clip.zw)))) { discard; }
	vec4 paint;
 if(skin_surface>.5) {
  paint=own_pose(paint_uv,gait_reference_id);
 } else if (gait_mode == 3 || gait_mode == 4 || gait_mode == 5) {
  vec2 dimensions=vec2(textureSize(atlas,0));
  paint=step_paint((paint_uv-pose_cell.xy)*dimensions,dimensions);
 } else {
  paint=own_pose(paint_uv,pose_id);
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
