extends RefCounted
static var probes: Dictionary = {}
static func sample(asset: String, uv: Vector2) -> float:
	if probes.is_empty(): probes = JSON.parse_string(FileAccess.get_file_as_string("res://data/ground_light.json"))
	if not probes.has(asset): return 1.0
	var entry: Dictionary = probes[asset]
	var x: = clampf(uv.x * float(entry.width) - 0.5, 0, float(entry.width) - 1)
	var y: = clampf(uv.y * float(entry.height) - 0.5, 0, float(entry.height) - 1)
	var ix: = int(x); var iy: = int(y); var right: = mini(ix + 1, int(entry.width) - 1); var below: = mini(iy + 1, int(entry.height) - 1)
	var top: = lerpf(entry.gain[iy * int(entry.width) + ix], entry.gain[iy * int(entry.width) + right], x - ix)
	var bottom: = lerpf(entry.gain[below * int(entry.width) + ix], entry.gain[below * int(entry.width) + right], x - ix)
	return lerpf(top, bottom, y - iy)
