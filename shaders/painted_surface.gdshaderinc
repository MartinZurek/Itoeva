#include "res://shaders/water_flow.gdshaderinc"
#ifdef PAINTED_FIRE
#include "res://shaders/flame_flipbook.gdshaderinc"
#endif
#include "res://shaders/firelight.gdshaderinc"
#include "res://shaders/interaction_focus.gdshaderinc"
#include "res://shaders/rest_room.gdshaderinc"
#ifdef PAINTED_MILL
#include "res://shaders/mill_motion.gdshaderinc"
#endif
uniform vec4 room_window = vec4(0.0);
uniform vec4 room_sun_patch = vec4(0.0);
uniform float room_daylight = 0.0;
uniform sampler2D painted_texture : source_color, filter_linear_mipmap, repeat_disable;
uniform sampler2D water_mask : filter_linear, repeat_disable;
uniform vec2 source_size = vec2(2172.0, 724.0);
uniform vec4 scene_tint : source_color = vec4(1.0);
uniform float depth_air = 0.0;
uniform float lamp_gain = 0.04;
uniform float fire_gain = 0.36;
uniform int light_count = 0;
uniform int fire_count = 0;
uniform float ember_gain = 0.0;
uniform vec4 fire_areas[4];
uniform vec4 light_positions[16];
uniform vec4 light_strengths[16];
uniform bool transparent_piece = false;
uniform bool has_water = false;
uniform bool has_clouds = false;
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
	vec2 mask = (has_water || has_clouds) ? texture(water_mask, UV).rg : vec2(0.0);
	float water = mask.r * (1.0 - shore);
#ifdef PAINTED_MILL
	vec2 channels = mill_channels(UV);
	float selected_flow = dot(channels, vec2(max(0.0, mill_flow), max(0.0, -mill_flow)));
	float other_flow = dot(channels, vec2(max(0.0, -mill_flow), max(0.0, mill_flow)));
	water *= 1.0 - other_flow * .69;
#endif
	vec2 uv = rest_room_sample(UV);
 vec2 flow=vec2(.5);
	// Trockene Flaechen brauchen weder Rauschen noch Wasserreflexe.
#ifdef PAINTED_WATER
	if (water > .001) { flow=water_pattern(UV,clock); uv += water_flow_offset(clock,water,flow)*.6; }
#endif
	if (has_clouds && mask.g > .001) { uv += cloud_drift(clock) * mask.g; }
	uv = clamp(uv,vec2(0.0),vec2(1.0));
	vec4 paint = texture(painted_texture, uv);
#ifdef PAINTED_MILL
	if (mill_wheel_mix > .001) { paint.rgb = mill_wheel(paint.rgb, UV, painted_texture); }
#endif
	if (shore_enabled && shore > 0.0) {
		paint = mix(paint, texture(shore_texture, UV), shore);
	}
 vec3 ink = paint.rgb * scene_tint.rgb;
#ifdef PAINTED_MILL
	// Die stehende Rinne bleibt sichtbar, während der gewählte Lauf heller fließt.
	ink *= 1.0 - other_flow * mask.r * .28;
	if (selected_flow > .001 && water > .001) {
		float glint = smoothstep(.18, .95, sin(UV.y * 123.0 - clock * 7.0 + UV.x * 23.0));
		ink += vec3(.29, .41, .43) * scene_tint.rgb * selected_flow * water * (.10 + .34 * glint);
	}
#endif
	if (rest_room_kind != 0 && room_daylight > .001) {
		vec2 pane = (UV-room_window.xy) / room_window.zw;
		vec2 edge = smoothstep(vec2(0.0),vec2(.10),1.0-abs(pane));
		float breeze = sin(pane.x * 3.3 + pane.y * 1.7 - clock * .73);
		float window_light = edge.x * edge.y * (.045 + .075 * breeze);
		vec2 floor_delta = (UV-room_sun_patch.xy-vec2(.026*sin(clock*.43),.010*cos(clock*.37))) / room_sun_patch.zw;
		float dapple = .13 * (1.0-smoothstep(.04,1.0,dot(floor_delta,floor_delta)));
		ink *= 1.0 + room_daylight * (window_light + dapple);
	}
	// Helle Wasserpinselstriche bewegen sich unter einem Pixel, nie der Steg.
#ifdef PAINTED_WATER
	if(water>.001) {ink = nearby_ripple(water_surface(ink,UV,clock,water,flow),world_point,clock,water);}
#endif
	for (int index = 0; index < 16; index++) {
		if (index >= light_count) { break; }
		vec4 point = light_positions[index];
		vec4 light = light_strengths[index];
		vec2 distance = (UV - point.xy) / max(point.zw, vec2(0.0001));
		if (dot(distance,distance) > 1.0) { continue; }
		float pool = firelight_pool(distance);
		float phase = float(index) * 1.73 + point.x * 13.0 + point.y * 7.0;
		float pulse=1.0;
		float gain = lamp_gain;
		if (light.w < 0.0) {
			// Ruhiges, unregelmaessiges Feuerflackern, keine ganze Szene als Blitz.
			pulse=firelight_flicker(clock,phase);
			gain = fire_gain * pulse;
		}
		// Licht hebt die gemalte Oberflaeche an, die Pinselstruktur bleibt lesbar.
		ink += light.rgb * abs(light.w) * gain * pool * (0.50+paint.rgb*0.70);
		float core=exp(-dot(distance,distance)*85.0);
		ink += vec3(1.0,0.76,0.40)*core*gain*abs(light.w)*0.24;
#ifdef PAINTED_FIRE
  if(ember_gain>.001 && light.w<0.0) {
   // Das Glutbett nutzt den ohnehin berechneten Feuerpool und dessen Takt.
   // Kalte Gitterfarben bleiben ruhig; keine weitere Schleife oder Textur.
   float hot=smoothstep(.24,.65,paint.r)*smoothstep(.07,.30,paint.r-paint.b);
   ink+=vec3(.20,.060,.006)*hot*pool*(pulse-.78)*ember_gain*fire_gain;
  }
#endif
	}
#ifdef PAINTED_FIRE
 for(int i=0;i<4;i++) {
  if(i>=fire_count) {break;}
  vec4 f=fire_areas[i];
  vec2 local_uv=vec2((UV.x-f.x)/f.w+.5,(UV.y-(f.y-f.z))/f.z);
  if(local_uv.x>0.0 && local_uv.x<1.0 && local_uv.y>0.0 && local_uv.y<1.0) {
   vec4 flame=painted_firebed(local_uv,clock+float(i)*1.7);
   ink=mix(ink,flame.rgb*1.08,flame.a);
   ink+=vec3(1.0,0.42,0.055)*firelight_embers(local_uv,clock,float(i)*1.7)*0.65;
  }
 }
#endif
 vec2 delta=vec2(world_point.x-carried_position.x,.819152*(world_point.y-carried_position.y)-.573576*(world_point.z-carried_position.z));
 float aura=0.0;
 if(carried_gain>.001) { aura=exp(-dot(delta/vec2(2.9,2.3),delta/vec2(2.9,2.3))*1.8)*carried_gain; }
 ink+=paint.rgb*vec3(1.0,.82,.42)*aura;
 float air=depth_air*smoothstep(2.5,8.0,world_point.y);
 ink=mix(ink,vec3(dot(ink,vec3(.2126,.7152,.0722)))*vec3(.94,1.0,1.04),air);
 float readable_path=smoothstep(UV2.x,UV2.x+.035,UV.y)*(1.0-smoothstep(UV2.y-.035,UV2.y,UV.y));
 ink*=1.0+readable_path*.026;
 ALBEDO = interaction_ink(ink,paint.rgb,world_point);
#ifndef PAINTED_OPAQUE
	ALPHA = paint.a * (transparent_piece ? COLOR.a : 1.0);
#endif
}
