uniform vec3 walker_position = vec3(-1000.0);
uniform float walker_motion = 0.0;
// Spiegelhöhe, Stärke, Fließtempo und vertikaler Strömungsanteil.
uniform vec4 water_optics = vec4(.60, .22, 1.0, .10);
// Übergang zur Nähe, Glitzermenge und Stärke der farbigen Nachtreflexe.
uniform vec4 water_look = vec4(.55, .85, .5, .6);
uniform vec2 water_time = vec2(0.0); // Nacht, Abendrot.

float water_near(vec2 uv) {
 return smoothstep(water_look.x,water_look.y,uv.y);
}
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
// Der horizontale Wellenkamm geht bei Fällen in kurze Stücke mit Abwärtsdrift über.
vec2 water_pattern(vec2 uv, float clock) {
 float t = clock * water_optics.z;
 vec2 drift = mix(vec2(.34,-.24),vec2(.10,-1.38),water_optics.w);
 float n = water_noise(uv*vec2(48.0,29.0)+t*drift);
 vec2 phase = mix(vec2(13.0,114.0),vec2(52.0,172.0),water_optics.w);
 float crest = .5+.5*sin(dot(uv,phase)-t*mix(1.55,3.8,water_optics.w)+n*2.1);
 return vec2(n,crest);
}
vec2 water_sample_uv(sampler2D mask, vec2 uv, float wet, vec2 pattern) {
 // Ferne Wasserflächen erhalten Lichtbewegung bei festen Bildkoordinaten.
 float near = water_near(uv);
 if (near < .04 || wet < .05) { return uv; }
 vec2 drift = vec2((pattern.y-.5)*.004 + (pattern.x-.5)*.0018,(pattern.x-.5)*(.0013+.006*water_optics.w));
 vec2 shifted = clamp(uv+drift*wet*wet*near,vec2(.001),vec2(.999));
 // Beide Abtastpunkte müssen auf dem nassen Teil des Bildes liegen.
 return mix(uv,shifted,smoothstep(.46,.84,texture(mask,shifted).r));
}
vec2 water_reflection_uv(vec2 uv, vec2 pattern) {
 float source_y = water_optics.x - abs(uv.y-water_optics.x)*.72;
 float x = uv.x + ((pattern.y-.5)*.008+(pattern.x-.5)*.004)*water_near(uv);
 return clamp(vec2(x,source_y), vec2(.002), vec2(.998));
}
vec3 water_surface(vec3 paint, vec3 reflected, vec2 uv, float wet, vec2 pattern) {
 float near = water_near(uv);
 float distance_gain = mix(.34,1.0,near);
 float reflection = wet * water_optics.y * mix(.74,1.06,pattern.y) * (1.0-.66*water_optics.w);
 vec3 reflection_tint = mix(vec3(.85,.95,1.0),vec3(.66,.85,1.15),water_time.x);
 vec3 reflected_color = reflected * mix(reflection_tint,vec3(1.16,.96,.79),water_time.y);
 vec3 ink = mix(paint,reflected_color,reflection*distance_gain);
 float light = dot(max(paint, reflected*.56),vec3(.2126,.7152,.0722));
 float glint = smoothstep(.81,.99,pattern.y) * smoothstep(.66,.91,pattern.x) * water_look.z;
 // Kurze Glitzerpunkte im Vordergrund, weichere Lichtbänder an Fällen.
 float foam = water_optics.w * smoothstep(.52,.95,pattern.x) * smoothstep(.65,.98,pattern.y);
 vec3 color = mix(vec3(.76,.91,1.0),vec3(1.0,.72,.38),water_time.y);
 vec3 glow = color * light * (glint*.42*(1.0-.68*water_time.x) + foam*.21);
 float night_crest = smoothstep(.57,.89,pattern.y)*smoothstep(.48,.79,pattern.x);
 float moon_band = (.10 + .16*pattern.y)*smoothstep(.12,.50,light);
 glow += vec3(.044,.086,.136) * water_time.x * water_look.w * (night_crest + moon_band);
 return ink*(1.0+wet*distance_gain*(pattern.x-.5)*.23) + glow*wet*distance_gain;
}

// Warmes Licht sitzt unter der jeweiligen Lampe und bricht dort an den Wellenkämmen.
float water_lamp_reflection(vec2 uv, vec2 light, vec2 pattern, float wet) {
 vec2 delta = uv-light;
 if (water_time.x < .03 || wet < .05 || delta.y < .02 || delta.y > .44 || abs(delta.x) > .10) { return 0.0; }
 float width = .014 + delta.y*.10;
 float column = 1.0-smoothstep(.25,1.0,abs(delta.x+(pattern.x-.5)*.012)/width);
 float length_fade = smoothstep(.02,.10,delta.y)*(1.0-smoothstep(.26,.44,delta.y));
 return column*length_fade*mix(.26,1.0,pattern.y)*wet*water_time.x*mix(.40,1.0,water_near(uv));
}
