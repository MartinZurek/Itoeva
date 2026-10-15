shader_type spatial;
render_mode unshaded, cull_disabled, depth_prepass_alpha;

#include "res://shaders/water_flow.gdshaderinc"
#include "res://shaders/flame_flipbook.gdshaderinc"
#include "res://shaders/firelight.gdshaderinc"
uniform sampler2D painted_texture : source_color, filter_linear_mipmap, repeat_disable;
uniform sampler2D water_mask : filter_linear, repeat_disable;
uniform vec2 source_size = vec2(2172.0, 724.0);
uniform vec4 scene_tint : source_color = vec4(1.0);
uniform float lamp_gain = 0.04;
uniform float fire_gain = 0.36;
uniform int light_count = 0;
uniform int fire_count = 0;
uniform vec4 fire_areas[4];
uniform vec4 light_positions[16];
uniform vec4 light_strengths[16];
uniform bool transparent_piece = false;
uniform bool shore_enabled = false;
uniform sampler2D shore_texture : source_color, filter_linear_mipmap, repeat_disable;
// Ein optionaler fester Zeitpunkt macht Renderregressionen reproduzierbar.
uniform float animation_time = -1.0;
uniform vec3 carried_position = vec3(-100.0);
uniform float carried_gain = 0.0;
varying vec3 world_point;
void vertex() { world_point=(MODEL_MATRIX*vec4(VERTEX,1.0)).xyz; }

void fragment() {
	float clock = animation_time >= 0.0 ? animation_time : TIME;
	// Oberhalb .75 bleibt die Originalkueste exakt erhalten.
	float shore = shore_enabled ? smoothstep(0.75, 0.78, UV.y) : 0.0;
	float water = texture(water_mask, UV).r * (1.0 - shore);
	vec2 uv = clamp(UV+water_flow_offset(UV,clock,water)*.6,vec2(0.0),vec2(1.0));
	vec4 paint = texture(painted_texture, uv);
	if (shore_enabled && shore > 0.0) {
		paint = mix(paint, texture(shore_texture, UV), shore);
	}
 vec3 ink = paint.rgb * scene_tint.rgb;
	// Helle Wasserpinselstriche bewegen sich unter einem Pixel, nie der Steg.
	if(water>.001) {ink = water_surface(ink,UV,clock,water)*1.0;}
	for (int index = 0; index < 16; index++) {
		if (index >= light_count) { break; }
		vec4 point = light_positions[index];
		vec4 light = light_strengths[index];
		vec2 distance = (UV - point.xy) / max(point.zw, vec2(0.0001));
		float pool = firelight_pool(distance);
		float phase = float(index) * 1.73 + point.x * 13.0 + point.y * 7.0;
		float gain = lamp_gain * (1.0 + 0.012 * sin(clock * 0.87 + phase) + 0.008 * sin(clock * 1.37 + phase * 1.3));
		if (light.w < 0.0) {
			// Ruhiges, unregelmaessiges Feuerflackern, keine ganze Szene als Blitz.
			gain = fire_gain * firelight_flicker(clock,phase);
		}
		// Licht hebt die gemalte Oberflaeche an, die Pinselstruktur bleibt lesbar.
		ink += light.rgb * abs(light.w) * gain * pool * (0.50+paint.rgb*0.70);
		float core=exp(-dot(distance,distance)*85.0);
		ink += vec3(1.0,0.76,0.40)*core*gain*abs(light.w)*0.24;
	}
 for(int i=0;i<4;i++) {
  if(i>=fire_count) {break;}
  vec4 f=fire_areas[i];
  vec2 local_uv=vec2((UV.x-f.x)/f.w+.5,(UV.y-(f.y-f.z))/f.z);
  if(local_uv.x>0.0 && local_uv.x<1.0 && local_uv.y>0.0 && local_uv.y<1.0) {
   vec4 flame=painted_flame(local_uv,clock+float(i)*1.7);
   ink=mix(ink,flame.rgb*1.08,flame.a);
   ink+=vec3(1.0,0.42,0.055)*firelight_embers(local_uv,clock,float(i)*1.7)*0.65;
  }
 }
 vec2 delta=vec2(world_point.x-carried_position.x,.819152*(world_point.y-carried_position.y)-.573576*(world_point.z-carried_position.z));
 float aura=exp(-dot(delta/vec2(2.9,2.3),delta/vec2(2.9,2.3))*1.8)*carried_gain;
 ink+=paint.rgb*vec3(1.0,.82,.42)*aura;
 ALBEDO = ink;
	ALPHA = paint.a * (transparent_piece ? COLOR.a : 1.0);
}
