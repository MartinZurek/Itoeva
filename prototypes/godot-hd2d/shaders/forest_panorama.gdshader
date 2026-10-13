shader_type spatial;
render_mode unshaded, cull_disabled, depth_draw_never;
#include "res://shaders/water_flow.gdshaderinc"
#include "res://shaders/flame_flipbook.gdshaderinc"
#include "res://shaders/firelight.gdshaderinc"
uniform sampler2D painted_texture : source_color, filter_linear_mipmap, repeat_disable;
uniform sampler2D water_mask : filter_linear, repeat_disable;
uniform vec3 path_tint : source_color = vec3(1.0);
uniform vec2 atlas_scale = vec2(1.0);
uniform vec2 atlas_origin = vec2(0.0);
uniform bool seam_piece = false;
uniform float origin = 0.0;
uniform float span = 22.0;
uniform float darkness = 0.0;
uniform vec3 time_tint : source_color = vec3(1.0);
uniform vec3 player = vec3(-100.0);
uniform bool carried_light = true;
uniform bool blend_left = false;
uniform float torch_gain = 1.0;
uniform float light_gain = 1.0;
uniform int flame_count = 0;
uniform vec4 flames[4];
uniform float animation_time = -1.0;
varying vec3 world_point;
void vertex() { world_point = (MODEL_MATRIX * vec4(VERTEX,1.0)).xyz; }
void fragment() {
	float clock = animation_time >= 0.0 ? animation_time : TIME;
	vec2 uv = UV;
	float water = texture(water_mask, UV).r;
	if(water>.001) {uv += water_flow_offset(UV,clock,water);}
	// Nur Kronen, keine Pfosten, Moebel oder Bruecken, bewegen sich im Wind.
	uv.x += (1.0-smoothstep(0.12,0.22,UV.y)) * sin(clock*1.1+UV.y*21.0+UV.x*13.0)*0.00065;
 // Local flame motion blends with the painted core, without reconstructing background.
 for(int i=0;i<4;i++) {
  if(i>=flame_count) {break;}
  vec4 f=flames[i];
  float mask=(1.0-smoothstep(.012,.024,abs(UV.x-f.x)))*smoothstep(f.y-f.z*1.4,f.y-f.z,UV.y)*(1.0-smoothstep(f.y-.026,f.y-.014,UV.y));
  uv += mask*vec2(sin(clock*4.7+UV.y*102.0)*.0025,sin(clock*5.3+UV.x*73.0)*.001);
 }
	vec3 ink=texture(painted_texture,atlas_origin+clamp(uv,vec2(seam_piece ? 0.008 : 0.002),vec2(seam_piece ? 0.992 : 0.998))*atlas_scale).rgb;
 ink *= path_tint;
	ink *= mix(time_tint,vec3(0.48,0.62,0.78),min(darkness*1.6,1.0)) * (1.0-darkness*0.75);
	if(water>.001) {ink=water_surface(ink,UV,clock,water);}
	for(int i=0;i<4;i++) {
		if(i>=flame_count) {break;}
		vec2 d=(UV-flames[i].xy)/vec2(0.12,0.24);
		float pool=firelight_pool(d);
		float pulse=firelight_flicker(clock,origin+float(i)*1.9);
		vec3 surface=texture(painted_texture,atlas_origin+UV*atlas_scale).rgb;
		ink+=vec3(0.58,0.28,0.085)*pool*torch_gain*light_gain*pulse*(0.5+surface*0.7);
		ink+=vec3(0.55,0.27,0.07)*exp(-dot(d,d)*75.0)*torch_gain*light_gain*pulse;
	}
	if(carried_light) {
		vec2 d=vec2(world_point.x-player.x,(world_point.y-player.y)*0.819152-(world_point.z-player.z)*0.573576-0.5);
		float pool=exp(-dot(d/vec2(2.9,2.2),d/vec2(2.9,2.2))*1.8);
		ink += texture(painted_texture,atlas_origin+UV*atlas_scale).rgb * vec3(0.90,0.63,0.32)*pool*darkness*1.7;
	}
 for(int i=0;i<4;i++) {
  if(i>=flame_count) {break;}
  vec4 f=flames[i];
  float height=f.z*1.40;
  vec2 local_uv=vec2((UV.x-f.x)/.067+.5,(UV.y-(f.y-height))/height);
  if(local_uv.x>0.0 && local_uv.x<1.0 && local_uv.y>0.0 && local_uv.y<1.0) {
   vec4 flame=painted_flame(local_uv,clock+float(i)*1.9+origin);
   float base_mask=1.0-smoothstep(f.y-.025,f.y-.013,UV.y);
   ink=mix(ink,flame.rgb*1.08,flame.a*torch_gain*base_mask);
   ink+=vec3(1.0,0.42,0.055)*firelight_embers(local_uv,clock,origin+float(i)*1.9)*torch_gain*base_mask*0.65;
  }
 }
 ALBEDO=ink;
	float path_mask=smoothstep(UV2.x-0.012,UV2.x+0.014,UV.y)*(1.0-smoothstep(UV2.y-0.008,UV2.y+0.045,UV.y));
	ALPHA=seam_piece ? path_mask*smoothstep(0.0,0.20,UV.x)*smoothstep(0.0,0.20,1.0-UV.x) : blend_left ? smoothstep(0.0,2.0/span,UV.x) : 1.0;
}
