// Kupfermaehne, jadefarbene Schuppen, warme helle Bauchstellen.
// Die dunklen Konturen und die Zeichnung selbst bleiben erhalten.
vec3 wyrmling_palette(vec3 color) {
 float value=max(color.r,max(color.g,color.b));
 float chroma=value-min(color.r,min(color.g,color.b));
 float neutral=(1.0-smoothstep(.20,.48,chroma/max(value,.01)))*smoothstep(.025,.12,value);
 float lightness=dot(color,vec3(.2126,.7152,.0722));
 vec3 jade=lightness*vec3(.66,1.17,1.04);
 vec3 ivory=lightness*vec3(1.06,1.04,.84);
 vec3 scales=mix(jade,ivory,smoothstep(.48,.85,lightness));
 return clamp(mix(color,scales,neutral*.88),vec3(0.0),vec3(1.0));
}
