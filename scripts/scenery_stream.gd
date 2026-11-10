extends RefCounted

const LOAD_DISTANCE: = 30.0
const KEEP_DISTANCE: = 45.0
var textures: Dictionary
var entries: Dictionary = {}
var pending: = ""
var tick: = 0.0
var requests: = 0
var clock: = 0.0
var retry_after: Dictionary = {}
func configure(stage: Node3D) -> void :
	textures = stage.textures
	var source_materials: Array = stage.painted_materials.duplicate()
	if stage.place == "FOREST_TRAIL": source_materials.append_array(stage.forest_materials.values())
	for material in source_materials:
		for uniform in ["painted_texture", "shore_texture"]:
			var texture = material.get_shader_parameter(uniform)
			if not texture is Texture2D: continue
			var asset: String = material.get_meta("shore_asset" if uniform == "shore_texture" else "painted_asset", texture.resource_path)
			if not entries.has(asset):
				var fallback: Texture2D = stage.scenery_fallbacks.get(asset)
				if fallback == null and stage.SceneryData.data().has(asset): fallback = stage.SceneryData.fallback(asset)
				if fallback == null: continue
				entries[asset] = {"fallback": fallback, "bindings": [], "pinned": false}
			var entry: Dictionary = entries[asset]
			if not material.has_meta("world_span"): entry.pinned = true
			entry.bindings.append({"material": material, "uniform": uniform, "span": material.get_meta("world_span", Vector2.ZERO)})
func distance_to(entry: Dictionary, x: float) -> float:
	if entry.pinned: return 0.0
	var nearest: = INF
	for binding in entry.bindings:
		var span: Vector2 = binding.span
		nearest = minf(nearest, maxf(maxf(span.x - x, x - span.y), 0.0))
	return nearest
func _bind(entry: Dictionary, texture: Texture2D) -> void :
	for binding in entry.bindings: binding.material.set_shader_parameter(binding.uniform, texture)
func advance(delta: float, x: float) -> void :
	clock += delta;tick -= delta
	if tick > 0: return
	tick = 0.1
	if not pending.is_empty():
		var status: = ResourceLoader.load_threaded_get_status(pending)
		if status == ResourceLoader.THREAD_LOAD_LOADED:
			var texture: Texture2D = ResourceLoader.load_threaded_get(pending)
			if distance_to(entries[pending], x) <= KEEP_DISTANCE:
				textures[pending] = texture;_bind(entries[pending], texture)
			pending = ""
		elif status == ResourceLoader.THREAD_LOAD_FAILED or status == ResourceLoader.THREAD_LOAD_INVALID_RESOURCE:
			retry_after[pending] = clock + 5.0;pending = ""
	var nearest_asset: = ""; var nearest: = INF
	for asset in entries:
		var entry: Dictionary = entries[asset]; var distance: = distance_to(entry, x)
		if textures.has(asset) and distance > KEEP_DISTANCE:
			_bind(entry, entry.fallback);textures.erase(asset)
		elif not textures.has(asset) and distance <= LOAD_DISTANCE and distance < nearest and clock >= float(retry_after.get(asset, 0.0)):
			nearest = distance;nearest_asset = asset
	if pending.is_empty() and not nearest_asset.is_empty():
		if ResourceLoader.load_threaded_request(nearest_asset, "Texture2D") == OK:
			pending = nearest_asset;requests += 1
		else: retry_after[nearest_asset] = clock + 5.0
func dispose() -> void :

	if not pending.is_empty(): ResourceLoader.load_threaded_get(pending)
	pending = "";entries.clear()
