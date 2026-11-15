uniform float mill_flow = 0.0;
uniform float mill_wheel_angle = 0.0;
uniform float mill_wheel_mix = 0.0;

vec2 mill_channels(vec2 uv) {
	float wheel = smoothstep(.367, .395, uv.x) * (1.0 - smoothstep(.456, .482, uv.x));
	float side = smoothstep(.485, .518, uv.x) * (1.0 - smoothstep(.655, .700, uv.x));
	float height = smoothstep(.250, .286, uv.y) * (1.0 - smoothstep(.535, .569, uv.y));
	return vec2(wheel, side) * height;
}

vec3 mill_composition(vec3 ink, vec2 uv) {
	// Das kühle Tal und das ruhigere Ufer rahmen Mühle und Weg ein.
	float valley = smoothstep(.43, .64, uv.x) * (1.0 - smoothstep(.78, .92, uv.x));
	valley *= 1.0 - smoothstep(.20, .40, uv.y);
	vec3 distant = dot(ink, vec3(.2126, .7152, .0722)) * vec3(.86, .99, 1.13);
	ink = mix(ink, distant, valley * .29);
	float foreground = smoothstep(.79, .97, uv.y);
	return ink * mix(vec3(1.0), vec3(.71, .76, .83), foreground);
}

vec3 mill_wheel(vec3 original, vec2 uv, sampler2D paint) {
	// Die Aussenkante, der Wasserfall und die Achse bleiben gemalt.
	vec2 vertical = vec2(.010, .112);
	vec2 offset = uv - vec2(.354, .424);
	vec2 p = vec2((offset.x - vertical.x * offset.y / vertical.y) / .059, offset.y / vertical.y);
	float radius = length(p);
	float mask = smoothstep(.14, .25, radius) * (1.0 - smoothstep(.76, .86, radius));
	mask *= smoothstep(-.28, -.02, p.x);
	if (mask <= .001) { return original; }

	float angle = atan(p.y, p.x) - mill_wheel_angle;
	float segment = fract(angle * 1.2732395 + .5);
	float spoke = 1.0 - smoothstep(.085, .17, abs(segment - .5));
	// Maserung aus einer freiliegenden Speiche derselben Kulisse.
	vec3 grain = texture(paint, vec2(.369 + radius * .033, .416 - radius * .025)).rgb;
	vec3 gaps = original * .30 + vec3(.048, .035, .019);
	vec3 timber = grain * mix(.76, 1.28, smoothstep(.12, .73, radius));
	return mix(original, mix(gaps, timber, spoke), mask * mill_wheel_mix);
}
