// Bild und Verdeckung verwenden dieselben Rueckwaertskoordinaten.
uniform int rest_room_kind = 0;
uniform vec2 rest_room_offset = vec2(0.0);

vec2 rest_room_sample(vec2 uv) {
	if (rest_room_kind == 1) {
		vec2 blanket = (uv - vec2(.438, .613)) / vec2(.100, .108);
		uv.y -= rest_room_offset.y * (1.0 - smoothstep(.0, 1.0, dot(blanket, blanket)));
	} else if (rest_room_kind == 2) {
		float rope = clamp((uv.x - .239) / .343, 0.0, 1.0);
		float cloth = sin(rope * 3.14159265);
		cloth *= smoothstep(.432, .557, uv.y) * (1.0-smoothstep(.681, .733, uv.y));
		uv -= rest_room_offset * cloth;
	} else if (rest_room_kind == 3) {
		vec2 cushion = (uv - vec2(.31, .483)) / vec2(.086, .072);
		uv.y -= rest_room_offset.y * (1.0 - smoothstep(.16, 1.0, dot(cushion, cushion)));
	} else if (rest_room_kind == 4) {
		vec2 cushion = (uv - vec2(.417, .634)) / vec2(.166, .054);
		uv.y -= rest_room_offset.y * (1.0 - smoothstep(.10, 1.0, dot(cushion, cushion)));
	}
	return uv;
}
