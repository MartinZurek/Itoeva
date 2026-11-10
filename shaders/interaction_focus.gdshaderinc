uniform vec3 interaction_position = vec3(-1000.0);
uniform float interaction_gain = 0.0;
vec3 interaction_ink(vec3 ink, vec3 paint, vec3 point) {
 if(interaction_gain>.001) {
  vec3 d=point-interaction_position;
  vec2 screen=vec2(d.x,.819152*d.y-.573576*d.z)/vec2(1.15,.85);
  float pool=max(0.0,1.0-dot(screen,screen));
  ink+=paint*vec3(.20,.15,.065)*pool*pool*interaction_gain;
 }
 return ink;
}
