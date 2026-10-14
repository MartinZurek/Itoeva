uniform sampler2D flame_atlas : source_color, filter_linear, repeat_disable;
vec4 painted_flame(vec2 uv,float clock) {
 float phase = mod(clock*12.0,16.0);
 float a = floor(phase);
 float b = mod(a+1.0,16.0);
 float tip=pow(1.0-uv.y,2.0);
 vec2 drift=vec2(sin(clock*2.7+uv.y*8.0)*0.024*tip, sin(clock*3.1)*0.009*tip);
 vec2 safe_uv = clamp(uv+drift,vec2(.008),vec2(.992));
 vec4 first = texture(flame_atlas,(safe_uv+vec2(mod(a,4.0),floor(a/4.0)))*.25);
 vec4 next = texture(flame_atlas,(safe_uv+vec2(mod(b,4.0),floor(b/4.0)))*.25);
 // Vormultiplizierte Farbe verhindert dunkle Saeume zwischen gemalten Posen.
 float alpha = mix(first.a,next.a,fract(phase));
 vec3 colour = mix(first.rgb*first.a,next.rgb*next.a,fract(phase))/max(alpha,.001);
 return vec4(colour,alpha);
}
