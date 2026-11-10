shader_type canvas_item;
uniform bool warm_wyrmling = false;
#include "res://shaders/wyrmling_palette.gdshaderinc"
uniform float chosen = 0.0;
void fragment() {
 vec4 paint=texture(TEXTURE,UV);
 if(warm_wyrmling) {paint.rgb=wyrmling_palette(paint.rgb);}
 float breath=1.0+.015*sin(TIME*1.6);
 vec3 light=mix(vec3(.64,.69,.72),vec3(1.10,1.06,.97)*breath,chosen);
 COLOR=vec4(paint.rgb*light,paint.a);
}
