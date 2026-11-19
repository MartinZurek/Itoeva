uniform sampler2D motion_mask : filter_linear, repeat_disable;
uniform bool has_environment_motion = false;

vec2 environment_wind(vec2 uv, float clock, float mask, vec3 pigment) {
 // Nur Blattfarben in freigegebenen Kronen; Stamm, Weg und Gebäude bleiben fest.
 float leaves = smoothstep(.015,.09,pigment.g-pigment.b) * (1.0-smoothstep(.11,.27,pigment.r-pigment.g));
 float gust = .65+.35*sin(clock*.74-uv.x*9.0);
 return vec2(sin(clock*1.9-uv.x*24.0+uv.y*13.0)*.0035*gust,cos(clock*1.5+uv.x*17.0)*.0008)*mask*leaves;
}

vec3 environment_clouds(vec3 ink, vec2 uv, float clock, float mask) {
 if(mask < .005) { return ink; }
 // Zwei langsam ziehende Schichten, ohne die bemalte Landschaft zu verschieben.
 vec2 p=uv*vec2(12.0,25.0)-vec2(clock*.11,clock*.006);
 float body=water_noise(p);
 float detail=water_noise(p*1.7+vec2(clock*.035,4.1));
 float cloud=smoothstep(.38,.78,body*.72+detail*.28);
 float shadow=smoothstep(.34,.74,body);
 ink*=1.0-mask*shadow*.055;
 return mix(ink,ink*.72+vec3(.28,.29,.30),cloud*mask*.42);
}
