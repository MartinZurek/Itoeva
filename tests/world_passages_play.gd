extends SceneTree

const Catalog = preload("res://scripts/world_catalog.gd")
const Forest = preload("res://scripts/forest_catalog.gd")
const Stage = preload("res://scripts/world_stage.gd")
const ForestStage = preload("res://scripts/forest_stage.gd")
const Routes = preload("res://scripts/world_routes.gd")
const Path = preload("res://scripts/interaction_path.gd")
# Largest stock player's capsule plus the margin used by tap-to-walk.
const RADIUS := 0.285

var failed := 0

func _initialize() -> void:
	run.call_deferred()

func run() -> void:
	var rooms: Array[String] = ["OUTDOOR", "FOREST_TRAIL"]
	for id in Catalog.data().interiors:
		if Catalog.is_outdoor(id):
			rooms.append(id)
	for room in rooms:
		var stage := ForestStage.new() if room == "FOREST_TRAIL" else Stage.new()
		stage.place = room
		stage.streaming_scenery = room in ["OUTDOOR", "FOREST_TRAIL"]
		root.add_child(stage)
		stage.build()
		stage.fit_character(0.015)
		if room == "OUTDOOR" or room == "FOREST_TRAIL":
			for edge in Routes.connections():
				var belongs := (room == "OUTDOOR") == (not Catalog.region_for(edge[0]).is_empty())
				if belongs and not Routes.walking_direction(edge[0], edge[1]).is_empty():
					var a := _center(stage, edge[0])
					var b := _center(stage, edge[1])
					_check_path(stage, a, b, edge[0] + " → " + edge[1])
					_check_path(stage, b, a, edge[1] + " → " + edge[0])
		for door in stage.doors:
			if not Catalog.is_outdoor(door.to) and door.to != "FOREST_TRAIL":
				continue
			var from: String = Forest.data().segments[int(door.segment_index)].id if room == "FOREST_TRAIL" else door.from
			var arrival: Vector3 = stage.safe_position(door.position, RADIUS)
			_check_path(stage, _center(stage, from), arrival, from + " → " + Routes.door_destination(door))
		stage.free()
	print("Außenwege · durchgehende Wege in beide Richtungen und alle Außendurchgänge geprüft")
	quit(1 if failed > 0 else 0)

func _center(stage: Node3D, id: String) -> Vector3:
	if stage.place == "FOREST_TRAIL":
		for i in Forest.data().segments.size():
			if Forest.data().segments[i].id == id:
				return stage.safe_position(Forest.position_for(i), RADIUS)
	return stage.safe_position(Catalog.position_for(id, Vector2(0.5, 0.55)), RADIUS)

func _check_path(stage: Node3D, from: Vector3, to: Vector3, label: String) -> void:
	var route := Path.find_path(stage, from, to, RADIUS)
	if route.is_empty() or not route[-1].is_equal_approx(to):
		failed += 1
		push_error("Kein durchgehender Weg: " + label)
		return
	for point in route:
		if not Path.clear_segment(stage, from, point, RADIUS):
			failed += 1
			push_error("Unterbrechung im Weg: " + label)
			return
		from = point
