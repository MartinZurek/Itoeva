float water_hash(vec2 p) {return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}
float water_noise(vec2 p) {
 vec2 i=floor(p);vec2 f=fract(p);vec2 u=f*f*(3.0-2.0*f);
 return mix(mix(water_hash(i),water_hash(i+vec2(1,0)),u.x),mix(water_hash(i+vec2(0,1)),water_hash(i+vec2(1,1)),u.x),u.y);
}
vec2 water_flow_offset(vec2 uv,float t,float wet) {
 vec2 p=uv*vec2(96.0,64.0)+vec2(t*.18,-t*.85);
 float a=water_noise(p);float b=water_noise(p*1.9+vec2(-t*.25,t*.32));
 return vec2(a-.5,b-.5)*.0028*wet*wet;
}
vec3 water_surface(vec3 paint,vec2 uv,float t,float wet) {
 vec2 p=uv*vec2(68.0,42.0)+vec2(t*.25,-t*.95);
 float n=water_noise(p)*.65+water_noise(p*2.13+vec2(9.2,t*.24))*.35;
 // Broken fine brushstrokes flow downstream; no regularly spaced sine stripes.
 float line=abs(sin((uv.y*168.0+uv.x*39.0+n*5.0-t*1.65)*3.14159));
 float glint=(1.0-smoothstep(.015,.09,line))*smoothstep(.58,.84,n);
 float foam=smoothstep(.77,.91,water_noise(p*1.45+vec2(-t*.3,t*.2)))*.035;
 return paint*(1.0+wet*(n-.5)*.13)+vec3(.48,.65,.73)*wet*(glint*.075+foam);
}
