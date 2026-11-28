extends SceneTree

const Battle = preload("res://scripts/card_battle.gd")
const Cards = preload("res://scripts/card_catalog.gd")
const FX = preload("res://scripts/practice_card_fx.gd")

class DrawProbe:
	var event_time := 0.58
	var marks := 0
	var anchors: Array[String] = []

	func _position(owner: int) -> Vector2:
		return Vector2(160 + owner * 460, 390)

	func _unit_position(uid: int) -> Vector2:
		return Vector2(340 + uid * 30, 430)

	func _target_point(code: String) -> Vector2:
		anchors.append(code)
		if code == "enemy": return _position(2) - Vector2(0, 75)
		if code.begins_with("h:"): return _position(int(code.substr(2))) - Vector2(0, 65)
		assert(code.begins_with("u:"))
		return _unit_position(int(code.substr(2))) - Vector2(0, 40)

	func draw_arc(_at: Vector2, radius: float, _start: float, _end: float, _points: int, _tint: Color, _width: float, _aa: bool) -> void:
		assert(radius > 0)
		marks += 1

	func draw_circle(_at: Vector2, _radius: float, _tint: Color, _aa: bool) -> void:
		marks += 1

	func draw_polyline(points: PackedVector2Array, _tint: Color, _width: float, _aa: bool) -> void:
		assert(points.size() >= 2)
		marks += 1

	func draw_line(_start: Vector2, _end: Vector2, _tint: Color, _width: float, _aa: bool) -> void:
		marks += 1

	func draw_rect(_rect: Rect2, _tint: Color, _filled := true, _width := -1.0) -> void:
		marks += 1

	func draw_colored_polygon(points: PackedVector2Array, _tint: Color) -> void:
		assert(points.size() >= 3)
		marks += 1

func _initialize() -> void:
	_check_cost_scaling()
	_check_resolution()
	var checked := _check_catalog()
	print("Karteneffekte · %d Kartenprofile, vier Kristallstufen und tatsächliche Wirkungen geprüft" % checked)
	quit()

func _check_cost_scaling() -> void:
	var event := {"source": 0, "element": "fire", "impact": 0.42, "duration": 1.04,
		"effects": [{"kind": "damage", "target": 2, "amount": 10}]}
	var previous_marks := 0
	for cost in [0, 2, 4, 6]:
		var probe := DrawProbe.new()
		FX.draw_card(probe, event, {"kind": "damage", "cost": cost, "element": "fire"})
		assert(probe.marks > previous_marks and probe.anchors == ["enemy"])
		previous_marks = probe.marks

func _check_resolution() -> void:
	var model := Battle.new()
	model.phase = "battle"
	model.cursor = 0
	model.decks[0].cap = 10
	model.decks[0].crystals = 8
	model.decks[0].hand = ["fresh_sap", "insight", "frost_thread"]
	assert(model.play_card(0) and model.decks[0].crystals == 10)
	assert(FX.impact_motifs(model.last_event, Cards.get_card("fresh_sap")) == {"h:0": ["energy"]})
	assert(FX.impact_labels(model.last_event)["h:0"][0][0] == "+2 Kristalle")
	model.acknowledge_animation()

	assert(model.play_card(0) and model.decks[0].hand.size() == 3 and model.decks[0].crystals == 9)
	assert(FX.impact_labels(model.last_event)["h:0"][0][0] == "+2 Karten")
	model.acknowledge_animation()

	var uid := model._add_minion(2, "thornling")
	var code := "u:" + str(uid)
	assert(model.play_card(0, code) and model.find_unit(uid).frozen == 1)
	assert(model.find_unit(uid).hp == 6 and model.decks[0].crystals == 7)
	assert(FX.impact_labels(model.last_event)[code][1][0] == "Eingefroren")
	model.acknowledge_animation()

	var ally := model._add_minion(0, "lantern_snail")
	model.decks[0].hand = ["homeward"]
	assert(model.play_card(0, "u:" + str(ally)))
	assert(model.find_unit(ally).is_empty() and model.decks[0].hand == ["lantern_snail"])
	assert(FX.impact_motifs(model.last_event, Cards.get_card("homeward")) == {"u:" + str(ally): ["recall"]})
	model.acknowledge_animation()

	model.decks[0].hand = ["moss_guard"]
	assert(model.play_card(0, "enemy", 2))
	var summon := "u:" + str(model.last_event.unit_uid)
	assert(FX.impact_motifs(model.last_event, Cards.get_card("moss_guard")) == {summon: ["summon"]})
	assert(model.find_unit(int(model.last_event.unit_uid)).formation_slot == 2)

func _check_catalog() -> int:
	var ids: Array = Cards.CARDS.keys() + Cards.collection_data().new_cards.keys() + Cards.Strategy.ids()
	for id in ids:
		var card := Cards.get_card(id)
		var event := {"source": 0, "element": card.element, "target_code": "enemy", "impact": 0.42, "duration": 1.04}
		if card.kind == "minion": event["unit_uid"] = 42
		var probe := DrawProbe.new()
		probe.event_time = 0.21
		FX.draw_card(probe, event, card)
		assert(probe.marks > 0, "%s braucht eine sichtbare Vorbereitung" % id)
		probe.event_time = 0.59
		probe.marks = 0
		FX.draw_card(probe, event, card)
		assert(probe.marks > 0, "%s braucht eine sichtbare Wirkung" % id)
	return ids.size()
