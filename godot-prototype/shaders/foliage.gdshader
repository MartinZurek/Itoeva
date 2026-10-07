shader_type spatial;
render_mode cull_disabled, specular_disabled;
uniform sampler2D leaves : source_color, filter_linear_mipmap, repeat_disable;
uniform vec4 tint : source_color = vec4(1.0);
uniform float wind = .05;
void vertex() {
    vec3 p = (MODEL_MATRIX * vec4(VERTEX,1.0)).xyz;
    VERTEX.x += sin(TIME*1.1+p.x*.7+p.z*.45)*(1.0-UV.y)*wind;
}
void fragment() {
    vec4 c = texture(leaves,UV);
    ALBEDO = c.rgb*tint.rgb;
    ALPHA = c.a;
    ALPHA_SCISSOR_THRESHOLD = .42;
    ROUGHNESS = .95;
    // Rueckseiten erhalten dieselbe Blattfarbe im Sonnenlicht.
    if (!FRONT_FACING) NORMAL = -NORMAL;
}
