extends RefCounted

const CAPACITY := 600
var samples := PackedFloat32Array()
var cursor := 0
var count := 0
var baseline_nodes := 0
var texture_rid: RID
var start_usec := 0
var previous_frame_usec := 0

func _init() -> void:
	samples.resize(CAPACITY)
	start_usec = Time.get_ticks_usec()

func record(_delta: float) -> void:
	# Prozess-delta kann von der Engine geglaettet/fuer Filme festgesetzt sein; Ruckler brauchen echte Zeitstempel.
	var now := Time.get_ticks_usec()
	if previous_frame_usec == 0:
		previous_frame_usec = now
		return
	samples[cursor] = float(now - previous_frame_usec) / 1000.0
	previous_frame_usec = now
	cursor = (cursor + 1) % CAPACITY
	count = mini(count + 1, CAPACITY)

func report() -> Dictionary:
	var ordered := samples.slice(0, count)
	ordered.sort()
	var sum := 0.0
	var slow := 0
	for ms in ordered:
		sum += ms
		if ms > 33.34:
			slow += 1
	return {
		"samples": count,
		"mean_ms": sum / maxi(count, 1),
		"p95_ms": 0.0 if count == 0 else ordered[mini(int(count * 0.95), count - 1)],
		"over_33ms": slow,
		"nodes": int(Performance.get_monitor(Performance.OBJECT_NODE_COUNT)),
		"static_memory_bytes": int(Performance.get_monitor(Performance.MEMORY_STATIC)),
		"texture_memory_bytes": int(Performance.get_monitor(Performance.RENDER_TEXTURE_MEM_USED)),
		"draw_calls": int(Performance.get_monitor(Performance.RENDER_TOTAL_DRAW_CALLS_IN_FRAME)),
		"renderer": RenderingServer.get_current_rendering_method(),
		"adapter": RenderingServer.get_video_adapter_name(),
		"godot": Engine.get_version_info().string,
		"platform": OS.get_name(),
		"window": str(DisplayServer.window_get_size()),
		"note": "Viewport-Bildintervalle; keine GPU-Timer oder Android-Geraetemessung auf anderen Plattformen."
	}
