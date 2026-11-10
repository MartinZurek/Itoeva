extends RefCounted

static var index: Dictionary = {}
static var sources: Dictionary = {}
static var meshes: Dictionary = {}

static func release() -> void :
	meshes.clear();sources.clear();index.clear()

static func poses_for(path: String) -> Array:
	if index.is_empty(): index = JSON.parse_string(FileAccess.get_file_as_string("res://data/battle_pose_geometry.json"))
	if not index.has(path): return []
	if sources.has(path): return sources[path]
	var raw: Array = JSON.parse_string(FileAccess.get_file_as_string(index[path]))
	var poses: Array = []
	for pose in raw:
		var r: Array = pose.region
		var points: = PackedVector2Array()
		for i in range(0, pose.outline.size(), 2): points.append(Vector2(pose.outline[i], pose.outline[i + 1]))
		poses.append({"region": Rect2(r[0], r[1], r[2], r[3]), "points": points, "travel": int(pose.get("travel", -1))})
	sources[path] = poses
	return poses

static func travel_region(texture: Texture2D, frame: int, fallback: Rect2) -> Rect2:
	for pose in poses_for(texture.resource_path):
		if pose.travel == frame: return pose.region
	return fallback

static func mesh_for(texture: Texture2D, region: Rect2) -> Mesh:
	var path: = texture.resource_path
	if meshes.has(path) and meshes[path].has(region): return meshes[path][region]
	for pose in poses_for(path):
		if region != pose.region: continue
		var points: = PackedVector2Array()
		var uvs: = PackedVector2Array()
		for local in pose.points:
			points.append(local / region.size - Vector2(0.5, 1.0))
			uvs.append((region.position + local) / texture.get_size())
		var triangles: = Geometry2D.triangulate_polygon(points)
		if triangles.is_empty(): push_error("Ungueltiger Kampfposen-Umriss: " + path);return null
		var arrays: = [];arrays.resize(Mesh.ARRAY_MAX)
		arrays[Mesh.ARRAY_VERTEX] = points;arrays[Mesh.ARRAY_TEX_UV] = uvs;arrays[Mesh.ARRAY_INDEX] = triangles
		var mesh: = ArrayMesh.new();mesh.add_surface_from_arrays(Mesh.PRIMITIVE_TRIANGLES, arrays)
		if not meshes.has(path): meshes[path] = {}
		meshes[path][region] = mesh
		return mesh
	return null
