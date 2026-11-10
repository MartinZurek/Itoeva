shader_type spatial;
render_mode unshaded, cull_disabled, depth_draw_never;
#include "res://shaders/water_flow.gdshaderinc"
#include "res://shaders/flame_flipbook.gdshaderinc"
#include "res://shaders/firelight.gdshaderinc"
#include "res://shaders/interaction_focus.gdshaderinc"
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
uniform vec3 carried_position = vec3(-100.0);
uniform float carried_gain = 0.0;
uniform vec3 carried_color : source_color = vec3(1.0,0.69,0.34);
uniform bool blend_left = false;
uniform float torch_gain = 1.0;
uniform float light_gain = 1.0;
uniform int flame_count = 0;
uniform vec4 flames[4];
uniform float animation_time = -1.0;
uniform bool has_water = false;
varying vec3 world_point;
void vertex() { world_point = (MODEL_MATRIX * vec4(VERTEX,1.0)).xyz; }
void fragment() {
	float clock = animation_time >= 0.0 ? animation_time : TIME;
 vec2 uv=UV;
 float water=has_water ? texture(water_mask,UV).r : 0.0;
 vec2 flow=vec2(.5);
 if(water>.001) {flow=water_pattern(UV,clock);uv+=water_flow_offset(clock,water,flow);}
 // Keine Wind-Sinusrechnung fuer den unbewegten Boden und die Pfosten.
 if(UV.y<.22) {uv.x+=(1.0-smoothstep(.12,.22,UV.y))*sin(clock*1.1+UV.y*21.0+UV.x*13.0)*.00065;}
 if(torch_gain>.001) {
  for(int i=0;i<4;i++) {
   if(i>=flame_count) {break;}
   vec4 f=flames[i];
   float mask=(1.0-smoothstep(.012,.024,abs(UV.x-f.x)))*smoothstep(f.y-f.z*1.4,f.y-f.z,UV.y)*(1.0-smoothstep(f.y-.026,f.y-.014,UV.y));
   if(mask>.001) {uv+=mask*vec2(sin(clock*4.7+UV.y*102.0)*.0025,sin(clock*5.3+UV.x*73.0)*.001);}
  }
 }
 // Eine Kulissenprobe wird fuer alle Lichtpools wiederverwendet.
 vec3 paint=texture(painted_texture,atlas_origin+clamp(uv,vec2(seam_piece ? .008 : .002),vec2(seam_piece ? .992 : .998))*atlas_scale).rgb;
 vec3 ink=paint*path_tint*mix(time_tint,vec3(.48,.62,.78),min(darkness*1.6,1.0))*(1.0-darkness*.75);
 if(water>.001) {ink=nearby_ripple(water_surface(ink,UV,clock,water,flow),world_point,clock,water);}
 if(torch_gain>.001) {
  for(int i=0;i<4;i++) {
   if(i>=flame_count) {break;}
   vec4 f=flames[i];vec2 d=(UV-f.xy)/vec2(.12,.24);
   if(dot(d,d)<1.0) {
    float pool=firelight_pool(d);
    float pulse=firelight_flicker(clock,origin+float(i)*1.9);
    ink+=vec3(.58,.28,.085)*pool*torch_gain*light_gain*pulse*(.5+paint*.7);
    ink+=vec3(.55,.27,.07)*exp(-dot(d,d)*75.0)*torch_gain*light_gain*pulse;
   }
   float height=f.z*1.40;
   vec2 local_uv=vec2((UV.x-f.x)/max(.035,f.z*.60)+.5,(UV.y-(f.y-height))/height);
   if(local_uv.x>0.0 && local_uv.x<1.0 && local_uv.y>0.0 && local_uv.y<1.0) {
    vec4 flame=painted_firebed(local_uv,clock+float(i)*1.9+origin);
    ink=mix(ink,flame.rgb*1.08,flame.a*torch_gain);
    ink+=vec3(1.0,.42,.055)*firelight_embers(local_uv,clock,origin+float(i)*1.9)*torch_gain*.65;
   }
  }
 }
 if(carried_light && carried_gain>.001) {
  vec2 d=vec2(world_point.x-carried_position.x,(world_point.y-carried_position.y)*.819152-(world_point.z-carried_position.z)*.573576);
  float pool=exp(-dot(d/vec2(2.9,2.2),d/vec2(2.9,2.2))*1.8);
  ink+=paint*carried_color*pool*carried_gain*1.35;
 }
 float air=.05*smoothstep(2.5,8.0,world_point.y);
 ink=mix(ink,vec3(dot(ink,vec3(.2126,.7152,.0722)))*vec3(.94,1.0,1.04),air);
 float readable_path=smoothstep(UV2.x,UV2.x+.035,UV.y)*(1.0-smoothstep(UV2.y-.035,UV2.y,UV.y));
 ink*=1.0+readable_path*.016;
 ALBEDO=interaction_ink(ink,paint,world_point);
	float path_mask=smoothstep(UV2.x-0.012,UV2.x+0.014,UV.y)*(1.0-smoothstep(UV2.y-0.008,UV2.y+0.045,UV.y));
	ALPHA=seam_piece ? path_mask*smoothstep(0.0,0.20,UV.x)*smoothstep(0.0,0.20,1.0-UV.x) : blend_left ? smoothstep(0.0,2.0/span,UV.x) : 1.0;
}
