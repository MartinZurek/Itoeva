shader_type canvas_item;
void fragment() {
    vec4 original = texture(TEXTURE,UV);
    float shore = .765+.033*sin(UV.x*13.0);
    bool water = UV.y>shore && original.b>original.r*.83 && original.g>.25;
    vec2 uv = UV;
    if (water) {
        uv.x += sin(UV.y*180.0+TIME*.9)*.00045;
        uv.y += sin(UV.x*60.0-TIME*.65)*.00035;
    }
    COLOR *= texture(TEXTURE,uv);
}
