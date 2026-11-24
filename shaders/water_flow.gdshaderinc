uniform vec3 walker_position = vec3(-1000.0);
uniform float walker_motion = 0.0;
// Spiegelhöhe, Stärke, Fließtempo und vertikaler Strömungsanteil.
uniform vec4 water_optics = vec4(.60, .22, 1.0, .10);
// Nur bestehende Wasserpixel in Schrittnähe reagieren; kein Partikelsystem,
// keine zusaetzliche Textur und keine Wellen auf dem trockenen Weg.
vec3 nearby_ripple(vec3 paint, vec3 world, float t, float wet) {
 if (walker_motion < .01) { return paint; }
 vec2 delta = vec2(world.x-walker_position.x, .573576*(world.z-walker_position.z));
 float r2 = dot(delta,delta);
 if (r2 > 2.25) { return paint; }
 float ripple = sin(sqrt(r2)*16.0-t*4.0)*exp(-r2*2.4)*.025*walker_motion*wet;
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
// Ein Rauschwert und eine wandernde Wellenphase reichen pro Wasserfragment.
vec2 water_pattern(vec2 uv, float clock) {
 float t = clock * water_optics.z;
 vec2 drift = mix(vec2(.32,-.35),vec2(.08,-1.25),water_optics.w);
 float n = water_noise(uv*vec2(48.0,27.0)+t*drift);
 float crest = .5+.5*sin(uv.y*142.0 + uv.x*13.0 - t*2.3 + n*2.7);
 return vec2(n,crest);
}
vec2 water_flow_offset(float wet, vec2 pattern) {
 vec2 drift = vec2((pattern.y-.5)*.008 + (pattern.x-.5)*.003,(pattern.x-.5)*(.002 + .010*water_optics.w));
 return drift * wet * wet;
}
// Eine verzerrte Probe oberhalb der gemalten Uferlinie ersetzt die Spiegelkamera.
vec2 water_reflection_uv(vec2 uv, vec2 pattern) {
 float source_y = water_optics.x - abs(uv.y-water_optics.x)*.72;
 float x = uv.x + (pattern.y-.5)*.015 + (pattern.x-.5)*.008;
 return clamp(vec2(x,source_y), vec2(.002), vec2(.998));
}
vec3 water_surface(vec3 paint, vec3 reflected, float wet, vec2 pattern) {
 float reflection = wet * water_optics.y * (.52 + .48*pattern.y) * (1.0-.59*water_optics.w);
 float glint = smoothstep(.77,.98,pattern.y) * smoothstep(.37,.76,pattern.x);
 vec3 ink = mix(paint, reflected * vec3(.76,.90,1.0), reflection);
 // Der Glanz hängt vom vorhandenen Bildlicht ab und leuchtet nachts nicht von selbst.
 float light = dot(max(paint, reflected*.53), vec3(.2126,.7152,.0722));
 return ink*(1.0+wet*(pattern.x-.5)*.18) + vec3(.57,.80,.94)*wet*glint*light*.28;
}
