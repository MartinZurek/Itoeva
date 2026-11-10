shader_type spatial;
render_mode unshaded, blend_mix, depth_draw_never, cull_disabled;
uniform float opacity = 0.34;
uniform bool grounded = false;
uniform vec2 cast_vector = vec2(.48,.28);
uniform float height = 0.0;
void fragment() {
 if(grounded) {
  vec2 p=(UV-.5)*vec2(2.2,1.35);
  float spread=1.0+min(height,1.5)*.55;
  vec2 q=p/vec2(.48,.28)/spread;
  float contact=exp(-dot(q,q)*2.8)*(1.0-smoothstep(.02,.50,height));
  vec2 end=cast_vector*(1.0+min(height,1.5)*.25);
  float along=clamp(dot(p,end)/max(dot(end,end),.001),0.0,1.0);
  vec2 cast=(p-end*along)/vec2(.25,.19)/spread;
  float penumbra=exp(-dot(cast,cast)*2.0)*mix(1.0,.36,along);
  float edge=(1.0-smoothstep(.88,1.0,abs(UV.x-.5)*2.0))*(1.0-smoothstep(.88,1.0,abs(UV.y-.5)*2.0));
  ALBEDO=vec3(.075,.085,.11);
  ALPHA=max(contact*.86,penumbra*.64)*opacity*edge;
 } else {
  float radius=length((UV-vec2(.5))*2.0);
  ALBEDO=vec3(.14,.11,.08);
  ALPHA=(exp(-radius*radius*5.0)*.68+exp(-radius*radius*28.0)*.32)*(1.0-smoothstep(.75,1.0,radius))*opacity;
 }
}
