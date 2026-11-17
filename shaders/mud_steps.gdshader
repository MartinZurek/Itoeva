shader_type spatial;
render_mode unshaded, blend_mix, depth_draw_never, cull_disabled;
uniform float clock = 0.0;
varying vec4 stamp;

// Matches GroundSteps.SHADER_PERIOD. A mark expires before the clock wraps twice.
float stamp_age(float birth) {
 return mod(clock - birth + 16.0, 16.0);
}

void vertex() {
 stamp = INSTANCE_CUSTOM;
 if (!OUTPUT_IS_SRGB) {
  COLOR.rgb = mix(pow((COLOR.rgb + vec3(.055)) / 1.055, vec3(2.4)),
                  COLOR.rgb / 12.92, lessThan(COLOR.rgb, vec3(.04045)));
 }
 if (stamp.g > 1.5 && stamp.g < 3.5) {
  float progress = clamp(stamp_age(stamp.r) / .62, 0.0, 1.0);
  VERTEX.xz *= .92 + progress * .46;
  VERTEX.y += progress * .10;
  VERTEX.x += stamp.b * progress * .34;
 }
}

float soft_lobe(vec2 point, vec2 center, vec2 radius) {
 return 1.0 - smoothstep(.32, 1.0, length((point - center) / radius));
}

float print_shape(vec2 point, bool claw) {
 if (claw) {
  float pad = soft_lobe(point, vec2(0, .16), vec2(.55, .60));
  float outer = soft_lobe(vec2(abs(point.x), point.y), vec2(.39, -.43), vec2(.22, .23));
  float middle = soft_lobe(point, vec2(0, -.62), vec2(.21, .25));
  return max(pad, max(outer, middle) * .87);
 }
 vec2 toe = vec2(abs(point.x), point.y);
 toe.x -= .09 * clamp(-toe.y, -.7, .7);
 return 1.0-smoothstep(.61,1.0,length((toe-vec2(.27,.045))/vec2(.225,.72)));
}

void fragment() {
 float age = stamp_age(stamp.r);
 vec2 p = (UV - .5) * 2.0;
 if (stamp.g > 1.5 && stamp.g < 3.5) {
  float progress = clamp(age / .62, 0.0, 1.0);
  float lift = sin(progress * 3.14159);
  float side = stamp.b < 0.0 ? -1.0 : 1.0;
  float cloud = soft_lobe(p, vec2(side * progress * .22,.21), vec2(.53,.31));
  float light_grains = max(
   soft_lobe(p, vec2(side*(.17+.37*progress),.20-.54*lift), vec2(.12,.12)),
   soft_lobe(p, vec2(-side*(.09+.24*progress),.09-.45*lift), vec2(.082,.10))
  );
  float dark_grains = max(
   soft_lobe(p, vec2(side*(.02+.60*progress),.17-.24*lift), vec2(.11,.10)),
   soft_lobe(p, vec2(-side*(.03+.20*progress),.35-.36*lift), vec2(.10,.11))
  );
  vec3 tint = stamp.g > 2.5 ? vec3(.80,.64,.42) : vec3(.81,.65,.43);
  ALBEDO = mix(mix(tint,vec3(.48,.32,.16),dark_grains),vec3(1.0,.88,.62),light_grains) * COLOR.rgb;
  float flecks = max(light_grains, dark_grains);
  ALPHA = max(cloud * .38, flecks * .93) * (1.0-smoothstep(.085,.62,age)) * mix(.72,1.0,stamp.a);
 } else {
  bool sand = stamp.g < .5;
  bool earth = stamp.g > 3.5;
  bool claw = abs(stamp.b) > 1.5;
  float mark = print_shape(p, claw);
  float rim = max(0.0, print_shape(p + vec2(.075,-.16), claw) - mark);
  vec3 hollow = sand ? vec3(.27,.17,.08) : vec3(.28,.16,.075);
  vec3 crest = sand ? vec3(.98,.85,.61) : vec3(.94,.77,.49);
  ALBEDO = (sand || earth ? mix(crest,hollow,mark) : vec3(.16,.12,.075)) * COLOR.rgb;
  float impression = sand || earth ? max(mark * .88,rim * .72) : mark * .31;
  ALPHA = impression * (1.0-smoothstep(3.8,6.0,age)) * mix(.90,1.0,stamp.a);
 }
}
