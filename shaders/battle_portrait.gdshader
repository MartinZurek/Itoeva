shader_type canvas_item;
render_mode unshaded;
uniform vec4 source_uv = vec4(0.0,0.0,1.0,1.0);
uniform vec4 ambient : source_color = vec4(0.93,0.95,1.0,1.0);
uniform vec4 key_color : source_color = vec4(1.0,0.87,0.68,1.0);
uniform vec2 key_direction = vec2(-0.7,-0.7);
uniform float key_gain = 0.12;
uniform vec4 pulse_color : source_color = vec4(1.0);
uniform float pulse_gain = 0.0;
varying vec4 vertex_tint;
void vertex() { vertex_tint = COLOR; }
void fragment() {
 vec4 ink = texture(TEXTURE,UV);
 vec2 local_uv = clamp((UV-source_uv.xy)/max(source_uv.zw,vec2(0.0001)),vec2(0.0),vec2(1.0));
 vec2 toward_light = clamp(UV+key_direction*TEXTURE_PIXEL_SIZE*2.0,source_uv.xy+TEXTURE_PIXEL_SIZE*0.5,source_uv.xy+source_uv.zw-TEXTURE_PIXEL_SIZE*0.5);
 float rim = max(0.0,ink.a-texture(TEXTURE,toward_light).a);
 float form = clamp(0.85-local_uv.y*0.25-local_uv.x*0.12,0.35,1.0);
 vec3 lit = ink.rgb*ambient.rgb*(0.97+form*0.04);
 lit += ink.rgb*key_color.rgb*key_gain*form;
 lit += key_color.rgb*rim*key_gain*0.45;
 lit += ink.rgb*pulse_color.rgb*pulse_gain;
 COLOR = vec4(lit,ink.a)*vertex_tint;
}
