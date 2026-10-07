shader_type spatial;
render_mode blend_mix, depth_draw_always, cull_disabled;
varying vec3 world_position;
void vertex() {
    world_position = (MODEL_MATRIX * vec4(VERTEX,1.0)).xyz;
    VERTEX.y += sin(world_position.x*.8+TIME*.8)*.018 + sin(world_position.z*1.3-TIME*.55)*.016;
}
void fragment() {
    vec2 p = world_position.xz;
    float wave = sin(p.y*4.2-TIME*1.15+sin(p.x*1.4)*.45);
    float fleck = pow(max(0.0,sin(p.x*6.0+TIME*.4)*sin(p.y*9.0-TIME*.7)),12.0);
    float coast_z = p.y+sin(p.x*.19)*.85+sin(p.x*.6)*.28;
    float shore = 1.0-smoothstep(11.2,13.6,coast_z);
    float foam = smoothstep(.89,.99,wave)*shore;
    ALBEDO = mix(vec3(.08,.36,.40),vec3(.25,.58,.57),sin(p.x*.16+p.y*.21)*.5+.5);
    ALBEDO = mix(ALBEDO,vec3(.86,.91,.76),foam*.52 + fleck*.24);
    ROUGHNESS = .28;
    SPECULAR = .65;
    ALPHA = .86;
}
