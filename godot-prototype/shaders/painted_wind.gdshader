shader_type canvas_item;
uniform float amount = 1.4;
uniform float anchor_y = 0.0;
void vertex() {
    float influence = clamp(-VERTEX.y/85.0,0.0,1.0);
    VERTEX.x += sin(TIME*.9+anchor_y*.013)*amount*influence;
}
