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

uniform int local_effect_count = 0;
uniform vec4 local_effect_areas[2];
uniform vec4 local_effect_settings[2];

vec3 place_ambience(vec3 ink, vec3 pigment, vec2 uv, float clock) {
 for (int i = 0; i < 2; i++) {
  if (i >= local_effect_count) { break; }
  vec4 area = local_effect_areas[i];
  vec3 effect = local_effect_settings[i].xyz;
  vec2 p = (uv - area.xy) / area.zw;
  float distance_sq = dot(p, p);
  if (distance_sq >= 1.0) { continue; }
  float edge = 1.0 - smoothstep(.12, 1.0, distance_sq);
  float strength = edge * effect.y;
  float t = clock * effect.z + float(i) * 2.37;
  float night = water_time.x;
  int kind = int(effect.x);

  if (kind == 1 || kind == 2) {
   // Langsamer Talnebel und schnellere, feinere Gischt.
   bool spray = kind == 2;
   vec2 drift = spray ? vec2(t*.40,-t) : vec2(t,t*.12);
   vec2 scale = spray ? vec2(3.2,6.0) : vec2(2.6,5.4);
   float cloud = smoothstep(.20,.75,water_noise(p*scale-drift));
   float light = mix(1.0, spray ? .27 : .63, night);
   vec3 color = mix(vec3(.65,.76,.82),vec3(.19,.28,.39),night);
   ink = mix(ink,color,strength*cloud*light);
  } else if (kind == 3 || kind == 4) {
   bool spark = kind == 4;
   vec2 drift = spark ? vec2(t*.30,t*1.75) : vec2(t*.6,-t*.24);
   vec2 grid = p*vec2(4.8,4.4)+drift;
   vec2 cell = floor(grid);
   float seed = water_hash(cell);
   vec2 point = fract(grid)-vec2(.22+.54*seed,.3+.40*water_hash(cell+17.0));
   float size = spark ? .032 : .053;
   float mote = 1.0-smoothstep(size,size+.055,length(point*vec2(1.0,spark ? .62 : 1.0)));
   float flicker = smoothstep(.23,.76,.5+.5*sin(clock*(spark ? 5.1 : 1.5)+seed*40.0));
   float gain = spark ? mix(.50,1.0,night) : pow(1.0-night,2.0);
   vec3 color = spark ? vec3(1.0,.48,.12) : vec3(1.0,.92,.56);
   ink += color * strength * mote * flicker * step(.46,seed) * gain;
  } else if (kind == 5) {
   float blue = smoothstep(.08,.23,pigment.b-pigment.r);
   float facets = smoothstep(.45,.82,pigment.g)*blue;
   float pulse = .15+.85*smoothstep(.19,.91,.5+.5*sin(t+p.x*2.0+p.y*1.35));
   ink += vec3(.18,.66,1.0)*facets*pulse*strength*mix(.62,1.0,night);
  } else if (kind == 6) {
   float sway = sin(p.y*4.2-t)*.14 + (.55-p.y)*.14;
   float width = mix(.12,.34,.5-.5*p.y);
   float wisp = 1.0-smoothstep(width*.25,width,abs(p.x-sway));
   float lift = .2+.8*smoothstep(.21,.65,water_noise(vec2(p.x*3.4,p.y*4.6+t)));
   vec3 steam = mix(vec3(.89,.86,.77),vec3(.48,.48,.47),night);
   ink = mix(ink,steam,strength*wisp*lift);
  }
 }
 return ink;
}
