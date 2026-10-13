shader_type spatial;
render_mode unshaded;

void fragment() {
	// Feine Maserung und sanfte Flaechentoene halten das niedrige Steggelaender ruhig.
	float grain = sin(UV.y * 91.0 + sin(UV.x * 17.0) * 0.9) * 0.018 + sin(UV.y * 187.0) * 0.012;
	float light = 0.80 + 0.18 * max(dot(NORMAL, normalize(vec3(-0.4, 0.8, 0.3))), 0.0);
	ALBEDO = (vec3(0.61, 0.48, 0.30) + grain) * light;
}
