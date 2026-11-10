shader_type spatial;
render_mode unshaded, blend_mix, depth_draw_never, cull_disabled;
uniform float clock = 0.0;
uniform vec4 scene_tint : source_color = vec4(1.0);
varying vec4 stamp;
void vertex() {
 stamp=INSTANCE_CUSTOM;
 if(stamp.g>1.5) {
  float age=max(0.0,clock-stamp.r);
  VERTEX.xz*=1.0+min(age,.7)*1.4;
  VERTEX.y+=min(age,.7)*.16;
  VERTEX.x+=stamp.b*min(age,.7)*.08;
 }
}
void fragment() {
 float age=max(0.0,clock-stamp.r);
 vec2 p=(UV-.5)*2.0;
 if(stamp.g>1.5) {
  float r2=dot(p,p);
  ALBEDO=vec3(.62,.51,.34)*scene_tint.rgb;
  ALPHA=exp(-r2*4.0)*(1.0-smoothstep(.4,1.0,r2))*(1.0-smoothstep(.05,.70,age))*.32*stamp.a;
 } else {
  float mark=0.0;
  if(abs(stamp.b)>1.5) {
   float pad=1.0-smoothstep(.5,1.0,length((p-vec2(0,.16))/vec2(.55,.60)));
   float outer=1.0-smoothstep(.10,.24,length(vec2(abs(p.x)-.38,p.y+.43)));
   float middle=1.0-smoothstep(.10,.24,length(p-vec2(0,-.65)));
   mark=max(pad,max(outer,middle)*.8);
  } else {
   vec2 split=vec2(abs(p.x)-.23,p.y+.02)/vec2(.24,.67);
   mark=1.0-smoothstep(.55,1.0,length(split));
  }
  ALBEDO=(stamp.g<.5 ? vec3(.40,.31,.18) : vec3(.16,.12,.075))*scene_tint.rgb;
  ALPHA=mark*(1.0-smoothstep(2.5,6.0,age))*.31*mix(.72,1.0,stamp.a);
 }
}
