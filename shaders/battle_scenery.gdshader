shader_type canvas_item;
#include "res://shaders/water_flow.gdshaderinc"
#include "res://shaders/environment_motion.gdshaderinc"
#include "res://shaders/firelight.gdshaderinc"
#include "res://shaders/flame_flipbook.gdshaderinc"
uniform sampler2D water_mask : filter_linear, repeat_disable;
uniform bool has_water = false;
uniform bool has_clouds = false;
uniform bool shore_enabled = false;
uniform sampler2D shore_texture : filter_linear_mipmap, repeat_disable;
uniform vec4 scene_tint : source_color = vec4(1.0);
uniform float animation_time = -1.0;
uniform int light_count = 0;
uniform int fire_count = 0;
uniform vec4 light_positions[16];
uniform vec4 light_strengths[16];
uniform vec4 fire_areas[4];
uniform float lamp_gain = .75;
uniform float fire_gain = .95;

void fragment() {
 float clock=animation_time>=0.0 ? animation_time : TIME;
 vec2 uv=UV;
 vec2 wet_mask=(has_water || has_clouds) ? texture(water_mask,UV).rg : vec2(0.0);
 float shore=shore_enabled ? smoothstep(.75,.78,UV.y) : 0.0;
 float wet=has_water ? wet_mask.r*(1.0-shore) : 0.0;
 vec2 motion=has_environment_motion ? texture(motion_mask,UV).rg : vec2(0.0);
 vec2 flow=vec2(.5);
 if (wet>.005) {
  flow=water_pattern(UV,clock);
  uv=water_sample_uv(water_mask,UV,wet,flow);
 }
 if (wet < .001 && motion.g > .015) {
  uv += leaf_tip_offset(UV,clock,motion.g,texture(TEXTURE,UV).rgb);
 }
 vec4 paint=texture(TEXTURE,uv);
 if (shore>.0) { paint=mix(paint,texture(shore_texture,UV),shore); }
 vec3 ink=paint.rgb*scene_tint.rgb;
 if (wet>.005) {
  vec3 reflection=texture(TEXTURE,water_reflection_uv(UV,flow)).rgb*scene_tint.rgb;
  ink=water_surface(ink,reflection,UV,wet,flow);
 }
 ink=environment_clouds(ink,paint.rgb,UV,clock,max(motion.r,wet_mask.g),scene_tint.rgb);
 for(int i=0;i<16;i++) {
  if(i>=light_count) {break;}
  vec4 at=light_positions[i];vec4 light=light_strengths[i];
  if (wet>.005 && water_time.x>.03) {
   ink+=vec3(1.0,.57,.23)*water_lamp_reflection(UV,at.xy,flow,wet)*abs(light.w)*.36;
  }
  vec2 d=(UV-at.xy)/max(at.zw,vec2(.0001));
  if(dot(d,d)>1.0) {continue;}
  float seed=float(i)*1.73+at.x*13.0+at.y*7.0;
  float pulse=light.w<0.0 ? firelight_flicker(clock,seed) : .88+.12*sin(clock*2.7+seed)+.05*sin(clock*6.1+seed*1.3);
  float gain=(light.w<0.0 ? fire_gain : lamp_gain)*pulse;
  ink+=light.rgb*abs(light.w)*gain*firelight_pool(d)*(.50+paint.rgb*.70);
  ink+=vec3(1.0,.76,.40)*exp(-dot(d,d)*85.0)*gain*abs(light.w)*.24;
 }
 for(int i=0;i<4;i++) {
  if(i>=fire_count) {break;}
  vec4 f=fire_areas[i];
  vec2 local_uv=vec2((UV.x-f.x)/f.w+.5,(UV.y-(f.y-f.z))/f.z);
  if(local_uv.x>0.0 && local_uv.x<1.0 && local_uv.y>0.0 && local_uv.y<1.0) {
   vec4 flame=painted_firebed(local_uv,clock+float(i)*1.7);
   ink=mix(ink,flame.rgb*1.08,flame.a);
  }
 }
 COLOR=vec4(ink,paint.a);
}
