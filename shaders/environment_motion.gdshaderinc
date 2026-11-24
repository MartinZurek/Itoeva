uniform sampler2D motion_mask : filter_linear, repeat_disable;
uniform bool has_environment_motion = false;

vec2 environment_wind(vec2 uv, float clock, float mask, vec3 pigment) {
 // Nur Blattfarben in freigegebenen Kronen; Stamm, Weg und Gebäude bleiben fest.
 float leaves = smoothstep(.015,.09,pigment.g-pigment.b) * (1.0-smoothstep(.11,.27,pigment.r-pigment.g));
 float gust = .65+.35*sin(clock*.74-uv.x*9.0);
 return vec2(sin(clock*1.9-uv.x*24.0+uv.y*13.0)*.0035*gust,cos(clock*1.5+uv.x*17.0)*.0008)*mask*leaves;
}

vec3 environment_clouds(vec3 ink, vec3 pigment, vec2 uv, float clock, float mask, vec3 tint) {
 if (mask < .005) { return ink; }
 float blue = smoothstep(.025,.18,pigment.b-pigment.r);
 float cloud_light = smoothstep(.65,.92,min(pigment.r,min(pigment.g,pigment.b)));
 float sky = mask * max(blue, cloud_light*.48);
 if (sky < .005) { return ink; }
 vec2 p = uv*vec2(12.0,24.0)-vec2(clock*.125,clock*.013);
 float body = water_noise(p);
 float cloud = smoothstep(.47,.76,body);
 return mix(ink, tint*vec3(.78,.85,.94), cloud*sky*.34) * (1.0-sky*(1.0-cloud)*.047);
}

uniform vec4 surface_sheen = vec4(0.0);
uniform vec3 sheen_color : source_color = vec3(1.0);
uniform float sheen_daylight = 1.0;

vec3 environment_sheen(vec3 ink, vec3 pigment, vec2 uv, float clock) {
 if (surface_sheen.w <= 0.0 || sheen_daylight < .005) { return ink; }
 vec2 p = (uv-surface_sheen.xy)/surface_sheen.zw;
 float patch = 1.0-smoothstep(.04,1.0,dot(p,p));
 if (patch < .005) { return ink; }
 float drift = .5+.5*sin(p.x*4.2+p.y*2.9-clock*.92);
 float light = smoothstep(.23,.76,max(pigment.r,pigment.b));
 return ink + sheen_color * patch * drift * light * sheen_daylight * .14;
}
