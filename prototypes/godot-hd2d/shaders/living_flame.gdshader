shader_type spatial;
render_mode unshaded, cull_disabled, depth_draw_never;
#include "res://shaders/flame_flipbook.gdshaderinc"
#include "res://shaders/firelight.gdshaderinc"
uniform float gain = 1.0;
uniform float smoke_gain = 0.0;
uniform float seed = 0.0;
uniform float animation_time = -1.0;
void fragment() {
 float t = (animation_time >= 0.0 ? animation_time : TIME)+seed;
 vec4 flame = painted_flame(UV,t);
 float h = 1.0-UV.y;
 float smoke = exp(-pow((UV.x-.5-sin(t*1.1+h*9.0)*.08)/(.08+h*.16),2.0))*smoothstep(.22,.6,h)*(1.0-smoothstep(.75,1.0,h))*smoke_gain*.17;
 float sparks=firelight_embers(UV,t,seed)*gain;
 ALBEDO = mix(flame.rgb,vec3(.28,.33,.4),smoke_gain)+vec3(1.0,0.42,0.055)*sparks;
 ALPHA = max(max(flame.a*gain,smoke),sparks*0.7);
}
