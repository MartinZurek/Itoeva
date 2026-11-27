uniform sampler2D motion_mask : filter_linear, repeat_disable;
uniform bool has_environment_motion = false;

// Nur einzeln markierte Blätter kippen in Böen, ohne wandernde Wellenphase.
vec2 leaf_tip_offset(vec2 uv, float clock, float weight, vec3 pigment) {
 float leaf = smoothstep(.025,.10,pigment.g-pigment.b)*(1.0-smoothstep(.13,.28,pigment.r-pigment.g));
 float t = clock * .62 + uv.x * 7.0;
 float gust = .65*sin(t)+.35*sin(t*1.83+uv.y*3.0);
 return vec2(.00120,.00034) * gust * weight * leaf;
}


vec3 environment_clouds(vec3 ink, vec3 pigment, vec2 uv, float clock, float mask, vec3 tint) {
 if (mask < .005) { return ink; }
 float blue = smoothstep(.065,.21,pigment.b-pigment.r);
 float cloud_light = smoothstep(.65,.92,min(pigment.r,min(pigment.g,pigment.b)));
 float sky = mask * max(blue, cloud_light*.24);
 if (sky < .005) { return ink; }
 vec2 p = uv*vec2(12.0,24.0)-vec2(clock*.125,clock*.013);
 float body = water_noise(p);
 float cloud = smoothstep(.47,.76,body);
 return mix(ink, tint*vec3(.78,.85,.94), cloud*sky*.17) * (1.0-sky*(1.0-cloud)*.023);
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
