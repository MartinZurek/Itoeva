shader_type spatial;
render_mode unshaded, blend_mix, depth_draw_never, cull_disabled;
uniform float clock = 0.0;
uniform vec4 scene_tint : source_color = vec4(1.0);
varying vec4 stamp;

void vertex() {
 stamp = INSTANCE_CUSTOM;
 if (stamp.g > 1.5) {
  float progress = clamp((clock - stamp.r) / .62, 0.0, 1.0);
  VERTEX.xz *= .94 + progress * .78;
  VERTEX.y += progress * .22;
  VERTEX.x += stamp.b * progress * .31;
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
 return soft_lobe(vec2(abs(point.x), point.y), vec2(.29, -.02), vec2(.24, .67));
}

void fragment() {
 float age = max(0.0, clock - stamp.r);
 vec2 p = (UV - .5) * 2.0;
 if (stamp.g > 1.5) {
  float core = soft_lobe(p, vec2(.04, .15), vec2(.56, .54));
  float tail = soft_lobe(p, vec2(stamp.b * .48, -.06), vec2(.45, .44));
  float grain = soft_lobe(p, vec2(-.50, .02), vec2(.28, .32));
  float cloud = max(core, max(tail * .71, grain * .58));
  vec3 tint = stamp.g > 2.5 ? vec3(.67, .50, .33) : vec3(.82, .68, .49);
  ALBEDO = mix(tint * .63, tint, cloud) * scene_tint.rgb;
  ALPHA = cloud * (1.0 - smoothstep(.09, .62, age)) * mix(.21, .60, stamp.a);
 } else {
  bool sand = stamp.g < .5;
  bool claw = abs(stamp.b) > 1.5;
  float mark = print_shape(p, claw);
  float rim = max(0.0, print_shape(p + vec2(.05, -.17), claw) - mark) * .60;
  ALBEDO = (sand ? mix(vec3(.23, .15, .08), vec3(.88, .75, .50), 1.0 - mark) : vec3(.16, .12, .075)) * scene_tint.rgb;
  ALPHA = (sand ? max(mark, rim) * .73 : mark * .31) * (1.0 - smoothstep(2.5, 6.0, age)) * mix(.82, 1.0, stamp.a);
 }
}
