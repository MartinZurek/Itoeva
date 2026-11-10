uniform vec3 walker_position = vec3(-1000.0);
uniform float walker_motion = 0.0;
// Nur bestehende Wasserpixel in Schrittnähe reagieren; kein Partikelsystem,
// keine zusaetzliche Textur und keine Wellen auf dem trockenen Weg.
vec3 nearby_ripple(vec3 paint, vec3 world, float t, float wet) {
 if (walker_motion < .01) { return paint; }
 vec2 delta = vec2(world.x-walker_position.x, .573576*(world.z-walker_position.z));
 float r2 = dot(delta,delta);
 if (r2 > 2.25) { return paint; }
 float ripple = sin(sqrt(r2)*16.0-t*4.0)*exp(-r2*2.4)*.008*walker_motion*wet;
 return paint + vec3(.48,.65,.73)*ripple;
}
// Sehr langsamer Versatz nur innerhalb weich begrenzter Wolkenfenster.
vec2 cloud_drift(float t) {
 return vec2(sin(t*.06)*.0065,sin(t*.037)*.00065);
}
float water_hash(vec2 p) {
 // Kleine arithmetische Streuung statt einer Sinusfunktion fuer jede Gitterecke.
 vec2 q=fract(p*vec2(.1031,.11369));
 float h=dot(q,q.yx+vec2(19.19));
 return fract((q.x+h)*(q.y+h)*17.17);
}
float water_noise(vec2 p) {
 vec2 i=floor(p);vec2 f=fract(p);vec2 u=f*f*(3.0-2.0*f);
 return mix(mix(water_hash(i),water_hash(i+vec2(1,0)),u.x),mix(water_hash(i+vec2(0,1)),water_hash(i+vec2(1,1)),u.x),u.y);
}
// Ein langsames, zusammenhängendes Strömungsfeld statt vieler glitzernder Fäden.
// Eine Noise-Abtastung; breite Reflexe bleiben auch bei Kamerabewegung ruhig.
vec2 water_pattern(vec2 uv,float t) {
 vec2 p=uv*vec2(72.0,32.0)+vec2(t*.10,-t*.38);
 float n=water_noise(p);
 float swell=.5+.5*sin(uv.y*44.0+uv.x*15.0-t*.55+n*.8);
 return vec2(n,swell);
}
vec2 water_flow_offset(float t,float wet,vec2 pattern) {
 return (pattern-.5)*.0018*wet*wet;
}
vec3 water_surface(vec3 paint,vec2 uv,float t,float wet,vec2 pattern) {
 float glint=smoothstep(.80,.95,pattern.y)*smoothstep(.42,.80,pattern.x);
 return paint*(1.0+wet*(pattern.x-.5)*.07)+vec3(.48,.65,.73)*wet*glint*.042;
}
