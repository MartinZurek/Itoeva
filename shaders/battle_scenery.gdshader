shader_type canvas_item;
#include "res://shaders/water_flow.gdshaderinc"
#include "res://shaders/environment_motion.gdshaderinc"
#include "res://shaders/firelight.gdshaderinc"
#include "res://shaders/flame_flipbook.gdshaderinc"
uniform sampler2D water_mask : filter_linear, repeat_disable;
uniform sampler2D shrub_mask : filter_linear, repeat_disable;
uniform bool has_water = false;
uniform bool has_clouds = false;
uniform bool has_shrubs = false;
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
 vec2 motion=has_environment_motion ? texture(motion_mask,UV).rg : vec2(0.0);
 if(motion.g>.005) {uv+=environment_wind(UV,clock,motion.g,texture(TEXTURE,UV).rgb);}
 vec2 flow=vec2(.5);
 if(wet_mask.r>.005) {flow=water_pattern(UV,clock);uv+=water_flow_offset(wet_mask.r,flow);}
 float shrubs=has_shrubs ? texture(shrub_mask,UV).r : 0.0;
 uv.x+=sin(clock*2.25-UV.x*35.0+UV.y*20.0)*.0028*shrubs;
 vec4 paint=texture(TEXTURE,uv);
 vec3 ink=paint.rgb*scene_tint.rgb;
 if (wet_mask.r>.005) {
  vec3 reflection=texture(TEXTURE,water_reflection_uv(UV,flow)).rgb*scene_tint.rgb;
  ink=water_surface(ink,reflection,wet_mask.r,flow);
 }
 ink=environment_clouds(ink,paint.rgb,UV,clock,max(motion.r,wet_mask.g),scene_tint.rgb);
 for(int i=0;i<16;i++) {
  if(i>=light_count) {break;}
  vec4 at=light_positions[i];vec4 light=light_strengths[i];
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
