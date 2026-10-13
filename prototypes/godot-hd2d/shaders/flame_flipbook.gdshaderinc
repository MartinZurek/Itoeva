uniform sampler2D flame_atlas : source_color, filter_linear, repeat_disable;
vec4 painted_flame(vec2 uv,float clock) {
 float phase = mod(clock*12.0,16.0);
 float a = floor(phase);
 float b = mod(a+1.0,16.0);
 vec2 safe_uv = clamp(uv,vec2(.008),vec2(.992));
 vec4 first = texture(flame_atlas,(safe_uv+vec2(mod(a,4.0),floor(a/4.0)))*.25);
 vec4 next = texture(flame_atlas,(safe_uv+vec2(mod(b,4.0),floor(b/4.0)))*.25);
 // Interpolate premultiplied colour so transparent edges never darken the tongues.
 float alpha = mix(first.a,next.a,fract(phase));
 vec3 colour = mix(first.rgb*first.a,next.rgb*next.a,fract(phase))/max(alpha,.001);
 return vec4(colour,alpha);
}
