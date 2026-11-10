extends RefCounted


static var settings: Dictionary = {}
static func profile(species: String) -> Dictionary:
	if settings.is_empty():
		settings = JSON.parse_string(FileAccess.get_file_as_string("res://data/ground_motion.json"))
		for entry in settings.values():
			var total: = 0.0; var before: = 0.0; var air: = 0.0
			for i in entry.run_weights.size():
				var weight: float = entry.run_weights[i];total += weight
				if i < int(entry.air_frames[0]): before += weight
				if i >= int(entry.air_frames[0]) and i <= int(entry.air_frames[1]): air += weight
			entry["air_start"] = before / total;entry["air_end"] = (before + air) / total
	return settings.get(species, {})
static func air(species: String, cycle: float) -> float:
	var data: = profile(species)
	if data.is_empty(): return 0.0
	var phase: = fposmod(cycle, 1.0)
	if phase <= data.air_start or phase >= data.air_end: return 0.0
	return pow(sin(PI * (phase - float(data.air_start)) / (float(data.air_end) - float(data.air_start))), 1.2)
