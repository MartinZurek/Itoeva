shader_type spatial;
render_mode specular_disabled;
uniform sampler2D paving : source_color, filter_linear_mipmap, repeat_enable;
uniform sampler2D sand : source_color, filter_linear_mipmap, repeat_enable;
varying vec3 world_position;
float grain(vec2 p) { return fract(sin(dot(p, vec2(127.1,311.7))) * 43758.5453); }
float soft_noise(vec2 p) {
    vec2 cell = floor(p); vec2 f = fract(p); f = f*f*(3.0-2.0*f);
    return mix(mix(grain(cell),grain(cell+vec2(1,0)),f.x),mix(grain(cell+vec2(0,1)),grain(cell+vec2(1,1)),f.x),f.y);
}
void vertex() { world_position = (MODEL_MATRIX * vec4(VERTEX,1.0)).xyz; }
void fragment() {
    vec2 p = world_position.xz;
    float n = soft_noise(p*1.6);
    float small = grain(floor(p * 120.0));
    vec3 grass = mix(vec3(.27,.35,.17), vec3(.36,.42,.22),n) + (small-.5)*.025;
    float coast_z = p.y+sin(p.x*.19)*.85+sin(p.x*.6)*.28;
    float beach = smoothstep(7.7,11.3,coast_z);
    vec3 beach_color = texture(sand, p*.28).rgb;
    float path_x = -6.0 + smoothstep(.8,8.3,p.y)*14.0;
    float path = (1.0-smoothstep(.8,1.18,abs(p.x-path_x)))*smoothstep(.7,1.2,p.y)*(1.0-smoothstep(8.6,10.2,p.y));
    vec3 stone = texture(paving,p*.38).rgb;
    ALBEDO = mix(mix(grass,beach_color,beach),stone,path);
    ROUGHNESS = .93;
}
