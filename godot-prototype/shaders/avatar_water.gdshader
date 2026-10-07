shader_type canvas_item;
uniform float waterline = 1.0;
void fragment() {
    vec4 c = texture(TEXTURE,UV);
    float frame_y = fract(UV.y*12.0);
    c.a *= 1.0-smoothstep(waterline-.015,waterline+.005,frame_y);
    COLOR *= c;
}
