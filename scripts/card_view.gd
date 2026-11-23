extends "res://scripts/battle_view.gd"
const StrategySummonFX = preload("res://scripts/strategy_summon_fx.gd")
const Focus = preload("res://scripts/battle_focus.gd")
const PracticeFilm = preload("res://scripts/practice_cinematic.gd")
const PracticeCreature = preload("res://scripts/practice_creature_art.gd")
const PracticeFX = preload("res://scripts/practice_card_fx.gd")
const PracticeImpactLabels = preload("res://scripts/practice_impact_labels.gd")
const AshHound = preload("res://scripts/arena_ash_hound.gd")
const ThreeRoots = preload("res://scripts/arena_three_roots.gd")
const Location = preload("res://scripts/battle_location.gd")
const EnvironmentMasks = preload("res://scripts/environment_masks.gd")
var arena: Dictionary = {}
var arena_picture: TextureRect
var mosslyn_cycles: Dictionary = {}
var mosslyn_textures: Dictionary = {}
var companion_cycles: Dictionary = {}
var companion_textures: Dictionary = {}
const Enemies = preload("res://scripts/regional_enemies.gd")
const NpcLocomotion = preload("res://scripts/npc_locomotion.gd")
var glut_gait: NpcLocomotion
const PRACTICE_PARTY := [Vector2(238, 436), Vector2(285, 463), Vector2(1014, 431)]
const PRACTICE_ALLIES := [Vector2(428, 416), Vector2(552, 429), Vector2(473, 456)]
const PRACTICE_FOES := [Vector2(858, 416), Vector2(736, 429), Vector2(813, 456)]
var focus: Node2D
var practice_film: Control
var skip_film_button: Button
var pause_film_button: Button
var practice_impact_labels: Node2D
const Feedback = preload("res://scripts/battle_feedback.gd")
var formation_slots: Dictionary = {}
var crystal_meter: Control
const Cards = preload("res://scripts/card_catalog.gd")
const Art = preload("res://scripts/card_assets.gd")
const Detail = preload("res://scripts/card_detail.gd")
const SpecialFX = preload("res://scripts/special_effects.gd")
const EcologyFX = preload("res://scripts/ecology_effects.gd")
var special_fx = SpecialFX.new()
var selected_card: int = -1
var selected_unit: int = -1
var hand_label: Label
var crystal_label: Label
var hint_message: String = ""
var animal_data: Dictionary = {}
var animal_texture: Texture2D
var card_popup: Panel
var intent_popup: Panel
var intent_button: Button
var intent_close_button: Button
var popup_index: int = 0
var inspected_unit: int = -1
var unit_info: Panel
var unit_info_name: Label
var unit_info_stats: Label
var unit_info_state: Label
var unit_info_abilities: Label
var party_plates: Array[Panel] = []
var party_focus: int = -2
var enemy_turn_label: Label
var enemy_turn_hint: Label
var pointer_id: int = -99
var pointer_card: int = -1
var pointer_start := Vector2.ZERO
var pointer_time := 0.0
var pointer_peek := false
var pointer_turn: int = -1
var pointer_position := Vector2.ZERO
var pointer_dragging := false
const HAND_CARD_SIZE := Vector2(124, 140)
var drag_card: Button
var drag_source: Button
var drag_origin: Vector2
var drag_hint: Dictionary = {}
var drag_hint_since := 0.0
var drop_choices: Array[Dictionary] = []

func _ready() -> void :
	arena = Location.profile(model.enemy_id, encounter.drill if encounter.get("practice") == true else -1)
	mosslyn_cycles = JSON.parse_string(FileAccess.get_file_as_string("res://data/mosslyn_m4_cycles.json"))
	super._ready()
	if not arena.is_empty():
		background = load(arena.asset)
		var backdrop := ColorRect.new()
		backdrop.color = Color("111b22")
		backdrop.z_index = -2
		backdrop.mouse_filter = Control.MOUSE_FILTER_IGNORE
		add_child(backdrop)
		backdrop.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
		arena_picture = TextureRect.new()
		arena_picture.texture = background
		arena_picture.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
		arena_picture.mouse_filter = Control.MOUSE_FILTER_IGNORE
		arena_picture.z_index = -1
		var material := ShaderMaterial.new()
		material.shader = preload("res://shaders/battle_scenery.gdshader")

		var ambience := preload("res://scripts/painted_ambience.gd").create_material(arena.asset, background)
		for parameter: String in ["motion_mask", "has_environment_motion", "has_clouds", "light_count", "light_positions", "light_strengths", "fire_count", "fire_areas", "flame_atlas"]:
			var value = ambience.get_shader_parameter(parameter)
			if value != null: material.set_shader_parameter(parameter, value)
		var mask_key: String = arena.asset.get_file()
		if arena.place == "SUN_GLADE": mask_key = "sun-glade.png"
		var mask: Dictionary = EnvironmentMasks.data().masks.get(mask_key, {})
		material.set_shader_parameter("water_mask", EnvironmentMasks.texture_for(mask_key))
		material.set_shader_parameter("has_water", not mask.is_empty())
		material.set_shader_parameter("scene_tint", arena.tint)
		material.set_shader_parameter("has_shrubs", arena.place == "SUN_GLADE")
		if arena.place == "SUN_GLADE":
			material.set_shader_parameter("shrub_mask", preload("res://assets/expansion/sun-glade-shrub-mask.png"))
		arena_picture.material = material
		add_child(arena_picture)
		_fit()

func _fit() -> void :
	super._fit()
	if is_instance_valid(arena_picture):
		arena_picture.position = margin - Vector2(0, float(arena.lift) * fit)
		arena_picture.size = Vector2(1280, 720) * fit

func _draw_background() -> void :
	if arena.is_empty(): super._draw_background()

func _uses_three_roots() -> bool:
	return arena.get("kind", "") == "roots"

func _build() -> void :
	ambient_count = 4
	spectacle = load("res://scripts/card_effects.gd").new()
	animal_data = JSON.parse_string(FileAccess.get_file_as_string("res://data/wildlife_atlas.json"))
	animal_texture = load(animal_data.asset)
	layout = Control.new();layout.size = Vector2(1280, 720);layout.mouse_filter = Control.MOUSE_FILTER_IGNORE;add_child(layout)
	_panel(Rect2(20, 16, 1240, 58))
	var practice: bool = encounter.get("practice") == true
	practice_impact_labels = PracticeImpactLabels.new()
	practice_impact_labels.view = self
	practice_impact_labels.z_index = 3
	add_child(practice_impact_labels)
	var title: String = encounter.drill_title.to_upper() + " · 1 GEGEN 1" if practice else (
		encounter.app.Catalog.data().labels[model.enemy_profile.place].to_upper() if model.enemy_id != "" else "SCHATTENWALD")
	if practice and model.has_root_bank():
		title = "LICHTUNG DER DREI WURZELN"
	_label(title, Rect2(38, 30, 310, 28), 17 if practice else 18, GOLD)
	turn_label = _label("", Rect2(355, 29, 260, 30), 16)
	crystal_meter = load("res://scripts/crystal_meter.gd").new()
	crystal_meter.position = Vector2(620, 22);crystal_meter.size = Vector2(330, 46)
	crystal_meter.mouse_filter = Control.MOUSE_FILTER_IGNORE;layout.add_child(crystal_meter)
	_button("Ton", Rect2(966, 23, 78, 44), encounter.app.show_audio_settings)
	var inspect := _button("Karten", Rect2(1054, 23, 88, 44), _show_hand)
	inspect.set_meta("battle_action", "card_info")
	var menu := _button("Pause", Rect2(1152, 23, 88, 44), encounter.app.show_battle_pause)
	menu.set_meta("battle_action", "menu")
	for i in 2:
		var x := 20.0 + i * 280.0
		var plate := _panel(Rect2(x, 88, 264, 67));party_plates.append(plate)
		hp_labels.append(_label("", Rect2(x + 15, 92, 235, 28), 17))
		var bar := _bar(Rect2(x + 15, 125, 234, 5), Color("9bcbb5"))
		bar.max_value = model.party[i].max_hp;hp_bars.append(bar)
		status_labels.append(_label("", Rect2(x + 15, 133, 234, 20), 12, Color("bcd5df")))
		if practice and i == 1:
			plate.hide()
			hp_labels[i].hide()
			bar.hide()
			status_labels[i].hide()
	_panel(Rect2(845, 88, 395, 74))
	enemy_label = _label("", Rect2(862, 92, 360, 25), 16 if model.enemy_profile.get("kind", "") == "boss" else 18, GOLD)
	enemy_bar = _bar(Rect2(862, 123, 360, 6), Color("c5828b"));enemy_bar.max_value = model.enemy_max_hp
	intent = _label("", Rect2(862, 134, 244, 23), 13, Color("d4c8d8"))
	intent_button = _button("Vorschau", Rect2(1112, 128, 116, 40), _show_intent)
	intent_button.add_theme_font_size_override("font_size", 15)
	crystal_label = _label("", Rect2(730, 516, 255, 24), 16, Color("b7e5e9"))
	queue_label = _label("", Rect2(730, 540, 255, 22), 12, Color("adbec5"))
	unit_info = _panel(Rect2(355, 170, 470, 108));unit_info.visible = false
	unit_info_name = _label("", Rect2(372, 177, 436, 24), 18, GOLD)
	unit_info_stats = _label("", Rect2(372, 206, 436, 22), 15)
	unit_info_state = _label("", Rect2(372, 232, 436, 22), 12, Color("b7d2d7"))
	unit_info_abilities = _label("", Rect2(372, 253, 436, 20), 13, Color("efd49e"))
	unit_info_name.visible = false;unit_info_stats.visible = false;unit_info_state.visible = false
	unit_info_abilities.hide()
	var hand_panel := _panel(Rect2(18, 506, 1242, 198))
	hand_panel.add_theme_stylebox_override("panel", _box(Color("1721258c"), false))


	layout.move_child(crystal_label, layout.get_child_count() - 1)
	layout.move_child(queue_label, layout.get_child_count() - 1)
	hand_label = _label("", Rect2(34, 558, 920, 15), 11, Color("c1c9c8"))
	status = _label("", Rect2(26, 477, 1228, 27), 17)
	status.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	status.text_overrun_behavior = TextServer.OVERRUN_TRIM_ELLIPSIS
	status.add_theme_constant_override("outline_size", 5)
	status.add_theme_color_override("font_outline_color", Color("101a20"))
	enemy_turn_label = _label("", Rect2(30, 517, 675, 34), 18, GOLD)
	enemy_turn_hint = _label("Gegneraktion läuft", Rect2(1001, 597, 244, 36), 18, Color("c6b7cd"))
	enemy_turn_label.visible = false;enemy_turn_hint.visible = false
	focus = Focus.new();focus.view = self;add_child(focus)
	_label("Zum Ziel ziehen · Tippen: Details", Rect2(1001, 550, 244, 19), 12, Color("bbcbd0"))
	build_commands()

func _locked() -> bool:
	return is_instance_valid(card_popup) or is_instance_valid(intent_popup) or encounter.suspended or encounter.busy > 0 or model.playback_pending or input_cooldown > 0 or model.cursor == 2 or model.phase != "battle"

func _show_intent() -> void :
	if _locked():
		return
	var result: Dictionary = focus.intent_preview()
	var outcomes := Focus.Preview.describe(model, result)
	var note := "Vorschau für den aktuellen Feldstand."
	if encounter.get("practice") != true:
		note += " Auf die angekündigte Aktion können weitere Aktionen folgen."
	var changes: Dictionary = result.get("outcomes", {})
	for hero in ["h:0", "h:1"]:
		if int(changes.get(hero, {}).get("hp", 0)) < 0:
			note += "\nWenn du Schutz aufbaust, prüfe danach die Vorschau erneut."
			break
	if outcomes.is_empty():
		outcomes = "Für die angekündigte Aktion sind keine direkten Treffer oder Statusänderungen sichtbar."
	intent_popup = _panel(Rect2(320, 169, 640, 350))
	intent_popup.z_index = 7
	intent_popup.mouse_filter = Control.MOUSE_FILTER_STOP
	_child_label(intent_popup, "Gegnerabsicht", Rect2(24, 17, 592, 30), 24, GOLD)
	_child_label(intent_popup, model.enemy_intent(), Rect2(24, 56, 592, 58), 18)
	var body := RichTextLabel.new()
	body.position = Vector2(24, 120)
	body.size = Vector2(592, 164)
	body.add_theme_font_size_override("normal_font_size", 17)
	body.add_theme_color_override("default_color", TEXT)
	body.text = outcomes + "\n\n" + note
	intent_popup.add_child(body)
	intent_close_button = _button("Schließen", Rect2(0, 0, 176, 44), _close_intent)
	intent_close_button.reparent(intent_popup, false)
	intent_close_button.position = Vector2(440, 292)
	refresh()

func _close_intent() -> void :
	encounter.app.menu_buttons.erase(intent_close_button)
	intent_popup.queue_free()
	intent_popup = null
	intent_close_button = null
	input_cooldown = 0.18
	refresh()

func _show_hand() -> void :
	if _locked() or model.decks[model.cursor].hand.is_empty(): return
	popup_index = 0;_popup_page()

func _popup_page(delta: int = 0) -> void :
	if delta != 0:
		if input_cooldown > 0: return
		input_cooldown = 0.18
	var hand: Array = model.decks[model.cursor].hand
	if is_instance_valid(card_popup): card_popup.detach()
	popup_index = posmod(popup_index + delta, hand.size())
	card_popup = Detail.new();card_popup.app = encounter.app;card_popup.card = Cards.get_card(hand[popup_index]).duplicate(true)
	var note := ""
	if not model.can_play(popup_index):
		note = Feedback.card_block_message(model, card_popup.card)
	else:
		var targets: Array[String] = model.card_targets(model.cursor, card_popup.card)
		if targets.size() > 1:
			note = "Ziehe die Karte auf eines der gültigen Ziele."
		else:
			var target := targets[0] if not targets.is_empty() else "enemy"
			var preview := Focus.Preview.evaluate(model, "card", popup_index, target)
			note = Focus.Preview.describe(model, preview)
	if card_popup.card.has("combo") and not note.begins_with("Kombo aktiv"):
		var readiness := "Kombo aktiv" if model.combo_ready(model.cursor, card_popup.card) else "Bedingung"
		note = "%s · %s\n%s" % [readiness, model.COMBO_LABELS[card_popup.card.combo], note]
	if not note.is_empty():
		card_popup.card.detail = card_popup.card.get("detail", card_popup.card.text) + "\n\n" + note
	card_popup.close_callback = _close_popup;card_popup.previous_callback = _popup_page.bind(-1);card_popup.next_callback = _popup_page.bind(1)
	card_popup.position = Vector2(340, 174);card_popup.size = Vector2(600, 309);layout.add_child(card_popup);refresh()

func _close_popup() -> void :
	if is_instance_valid(card_popup): card_popup.detach()
	card_popup = null;input_cooldown = 0.18;refresh()

func _clear_buttons() -> void :
	for button in buttons:
		encounter.app.menu_buttons.erase(button);button.queue_free()
	buttons.clear()

func _action(text: String, rect: Rect2, callback: Callable, key: String) -> Button:
	var button := _button(text, rect, callback)
	button.clip_text = true;button.add_theme_font_size_override("font_size", 15)
	button.set_meta("battle_action", key);buttons.append(button)
	return button

func _show_results() -> void :
	if encounter.get("practice") != true:
		super._show_results();return
	if results_shown: return
	results_shown = true;result_time = 0;input_cooldown = 0.25
	for child in layout.get_children():
		if child is CanvasItem: child.visible = false
	_panel(Rect2(640, 160, 592, 400))
	_label("TRAININGSDUELL GEWONNEN", Rect2(674, 196, 520, 44), 28, GOLD)
	var note = _label("%s · Runde %s\n\nWiederhole das Duell mit einer neuen Starthand oder wähle ein anderes Deck." % [encounter.drill_title, model.round], Rect2(674, 255, 510, 120), 21)
	note.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	_button("Noch ein Duell", Rect2(674, 395, 510, 53), encounter._retry)
	_button("Zurück zur Deckwahl", Rect2(674, 465, 510, 53), encounter.finish)

func build_commands() -> void :
	_clear_buttons()
	if encounter.get("practice") == true and model.phase in ["victory", "defeat"]:
		if model.phase == "victory" and not model.playback_pending: _show_results()
		elif model.phase == "defeat":
			_action("Erneut versuchen", Rect2(710, 560, 240, 60), encounter._retry, "retry")
			_action("Training beenden", Rect2(970, 560, 260, 60), encounter._retreat_now, "retreat")
		return
	if model.phase in ["victory", "defeat"]:
		super.build_commands();return
	if model.phase != "battle": return
	for unit in model.board:
		var uid := int(unit.uid)
		var foot := _unit_position(uid)
		var b := _action("", Rect2(foot - Vector2(58, 95), Vector2(116, 115)), _pick_unit.bind(uid), "unit")
		var box := StyleBoxEmpty.new()
		for style in ["normal", "hover", "pressed", "disabled"]: b.add_theme_stylebox_override(style, box)
		b.set_meta("unit_uid", uid)
	var enemy_bounds := Rect2(_position(2) - Vector2(83, 170), Vector2(184, 183))
	_action("", enemy_bounds, _choose_target.bind("enemy"), "enemy_target")
	for style in ["normal", "hover", "pressed", "disabled"]: buttons[-1].add_theme_stylebox_override(style, StyleBoxEmpty.new())
	var state: Dictionary = model.decks[_hand_owner()]
	for i in state.hand.size():
		var card: Dictionary = Cards.get_card(state.hand[i])
		var spread: float = float(i) - (state.hand.size() - 1) * 0.5
		var bounds := Rect2(Vector2(549 + spread * 92, 565 + absf(spread) * 1.5), HAND_CARD_SIZE)
		var b := _action("", bounds, _pick_card.bind(i), "card")
		b.set_meta("hand_index", i);b.tooltip_text = card.name + " · " + str(card.cost) + " Kristalle\n" + card.get("detail", card.text)
		b.pivot_offset = Vector2(HAND_CARD_SIZE.x * 0.5, HAND_CARD_SIZE.y)
		b.rotation = deg_to_rad(spread * 3.8)
		b.clip_contents = true
		b.set_meta("card_id", str(state.hand[i]))
		_card_face(b, card)
	if model.cursor == 2: return
	var primary: Dictionary = model.skill()
	var secondary: Dictionary = model.support_skill()
	var cost_suffix := " · %s ◇" % model.POWER_COST if encounter.get("practice") == true else ""
	var primary_button := _action(str(primary.name) + cost_suffix, Rect2(28, 515, 181, 43), _power.bind("skill"), "skill")
	primary_button.add_theme_font_size_override("font_size", 13)
	var trapped: bool = model.party[model.cursor].bind > 0 or model.party[model.cursor].silence > 0
	var secondary_button := _action("Fesseln lösen" if trapped else str(secondary.name) + cost_suffix, Rect2(219, 515, 181, 43), _power.bind("break_free" if trapped else "support"), "break_free" if trapped else "support")
	secondary_button.add_theme_font_size_override("font_size", 13)
	var ultimate: Dictionary = model.special_card()
	var ultimate_text: String = "Spezial · %s · %s/5" % [ultimate.name, model.special_charges[model.cursor]]
	if model.special_used[model.cursor]: ultimate_text = "Spezialkraft bereits eingesetzt"
	var special_button := _action(ultimate_text, Rect2(410, 515, 299, 43), _power.bind("special"), "special")
	special_button.add_theme_font_size_override("font_size", 14)
	_action("Zug beenden", Rect2(1001, 574, 244, 58), _power.bind("end_turn"), "end_turn")
	_action("Abwählen", Rect2(1001, 641, 146, 49), _cancel, "cancel")
	_action("Ende" if encounter.get("practice") == true else "Rückzug", Rect2(1155, 641, 90, 49), encounter._retreat_now if encounter.get("practice") == true else _power.bind("flee"), "flee")

func _child_label(parent: Control, text: String, rect: Rect2, font_size: int, color: Color = TEXT) -> Label:
	var label := Label.new();label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	label.add_theme_font_size_override("font_size", font_size);label.clip_text = true
	label.text = text;label.position = rect.position;label.size = rect.size
	label.mouse_filter = Control.MOUSE_FILTER_IGNORE
	label.add_theme_color_override("font_color", color);parent.add_child(label)
	return label

func _card_face(button: Button, card: Dictionary) -> void :
	card = model.effective_card(_hand_owner(), card)
	var art := TextureRect.new()
	art.texture = _card_texture(card)
	art.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
	art.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_CENTERED if card.kind == "minion" else TextureRect.STRETCH_KEEP_ASPECT_COVERED
	art.position = Vector2(5, 5)
	art.size = HAND_CARD_SIZE - Vector2(10, 10)
	art.mouse_filter = Control.MOUSE_FILTER_IGNORE
	button.add_child(art)
	var caption := Panel.new()
	caption.position = Vector2(4, 97)
	caption.size = Vector2(116, 39)
	caption.mouse_filter = Control.MOUSE_FILTER_IGNORE
	caption.add_theme_stylebox_override("panel", _box(Color("211c19ec"), false))
	button.add_child(caption)
	var title := _child_label(caption, card.name, Rect2(4, 1, 108, 37), 13, Color("ffe8b7"))
	title.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	title.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
	var cost := _child_label(button, str(card.cost), Rect2(6, 4, 30, 28), 21, Color("c3f4ff"))
	cost.add_theme_constant_override("outline_size", 6)
	cost.set_meta("card_field", "cost")
	var combo: String = card.get("combo", "")
	if not combo.is_empty():
		var hint: String = "Kombo aktiv" if model.combo_ready(_hand_owner(), card) else model.COMBO_LABELS[combo]
		var badge := _child_label(button, hint, Rect2(7, 43, 108, 33), 12, Color("ffe49b"))
		badge.add_theme_constant_override("outline_size", 5)
	var value := ""
	match str(card.kind):
		"minion": value = "%s / %s" % [card.attack, card.health]
		"damage", "freeze":
			var damage: int = int(card.value) + int(card.get("per_helper", 0)) * model.units(_hand_owner()).size()
			value = "%s Schaden" % damage
		"heal", "party_heal": value = "+%s LP" % card.value
		"barrier": value = "+%s Schild" % card.value
		"buff", "team_buff": value = "+%s ATK" % card.get("value", 0)
		"draw": value = "%s Karten" % card.value
	if not value.is_empty():
		var stats := _child_label(button, value, Rect2(8, 74, 108, 23), 15, Color("ffebbb"))
		stats.add_theme_constant_override("outline_size", 5)

func _card_texture(card: Dictionary) -> Texture2D:
	if card.id == PracticeCreature.SUBJECT:
		return PracticeCreature.pose(2)
	return Art.texture(card)

func _legacy_card_texture(card: Dictionary) -> Texture2D:
	var texture := AtlasTexture.new()
	if card.has("row"):
		var r: Array = animal_data.regions[int(card.row)][0]
		texture.atlas = animal_texture;texture.region = Rect2(r[0], r[1], r[2], r[3])
	else:
		var species: String = card.get("species", "starlet" if card.element in ["light", "guard"] else "gloop")
		if species == "enemy":
			texture.atlas = demon;texture.region = _demon_rect(0)
		else:
			var data: Dictionary = atlas_data[species]
			var r: Array = data.regions[12 if card.kind == "minion" else 7]
			texture.atlas = load(data.asset);texture.region = Rect2(r[0], r[1], r[2], r[3])
	return texture

func _pick_card(index: int) -> void :
	if _locked():
		return
	var hand: Array = model.decks[model.cursor].hand
	if index < 0 or index >= hand.size():
		return
	popup_index = index
	_popup_page()

func _pick_unit(uid: int) -> void :
	if _locked(): return
	var unit: Dictionary = model.find_unit(uid)
	if unit.is_empty(): return
	if selected_card >= 0:
		_choose_target("u:" + str(uid));return
	if int(unit.owner) == 2 and selected_unit >= 0:
		_choose_target("u:" + str(uid))
		return
	inspected_unit = uid
	if selected_unit == uid:
		selected_unit = -1
		hint_message = ""
	elif model.can_attack(uid):
		encounter.app.battle_audio.cue("select")
		selected_unit = uid
		hint_message = "Angriffsziel für " + str(unit.name) + " wählen."
		input_cooldown = 0.18
	else:
		selected_unit = -1
		hint_message = ""
	refresh()

func _choose_target(target: String) -> void :
	if _locked(): return
	if selected_card >= 0:
		if model.can_play(selected_card, target): encounter._card_play(selected_card, target)
		else: hint_message = "Dieses Ziel passt nicht zur Karte.";encounter.app.battle_audio.cue("error");refresh()
	elif selected_unit >= 0:
		if model.can_attack(selected_unit, target): encounter._unit_attack(selected_unit, target)
		else: hint_message = Feedback.attack_target_message(model, target);encounter.app.battle_audio.cue("error");refresh()

func _cancel() -> void :
	if _locked(): return
	encounter.app.battle_audio.cue("select")
	selected_card = -1;selected_unit = -1;inspected_unit = -1;hint_message = "";build_commands();refresh()

func _power(action: String) -> void :
	if _locked(): return
	encounter._act(action)

func play_event(elapsed: float = 0.0) -> void :
	selected_card = -1;selected_unit = -1;inspected_unit = -1;hint_message = ""
	super.play_event(elapsed)
	var cinematic: String = event.get("cinematic_card", "")
	if not cinematic.is_empty() and not event.get("cinematic_short", false) and elapsed < PracticeFilm.CUT_END:
		_start_practice_film(cinematic, elapsed)
	elif is_instance_valid(practice_film):
		_stop_practice_film()

func _start_practice_film(card_id: String, elapsed: float) -> void :
	if not is_instance_valid(practice_film):
		practice_film = PracticeFilm.new()
		practice_film.z_index = 8
		layout.add_child(practice_film)
		skip_film_button = _button("Zum Treffer", Rect2(1030, 637, 207, 54), _skip_practice_film)
		skip_film_button.z_index = 9
		pause_film_button = _button("Pause", Rect2(1100, 15, 137, 53), encounter.app.show_battle_pause)
		pause_film_button.z_index = 9
	practice_film.start(card_id, elapsed)
	skip_film_button.show()
	pause_film_button.show()

func _stop_practice_film() -> void :
	practice_film.stop()
	skip_film_button.hide()
	pause_film_button.hide()

func _skip_practice_film() -> void :
	if not is_instance_valid(practice_film) or not practice_film.visible or not model.playback_pending:
		return
	event_time = maxf(event_time, model.impact_time() - 0.12)
	encounter.busy = model.animation_duration() - event_time
	var audio = encounter.app.battle_audio
	audio.event_clock = maxf(audio.event_clock, event_time)
	_stop_practice_film()

func refresh() -> void :
	if results_shown:
		for button in buttons: button.disabled = model.playback_pending or encounter.busy > 0 or input_cooldown > 0 or encounter.suspended
		return
	enemy_turn_label.visible = model.phase == "battle" and model.cursor == 2
	enemy_turn_hint.visible = enemy_turn_label.visible
	enemy_turn_label.text = "GEGNERZUG · " + model.enemy_name
	_refresh_unit_info()
	if is_instance_valid(focus): focus.update_state()
	turn_label.text = "R%s · %s → %s" % [model.round, model.actor_name().split("·")[0].strip_edges(), _next_actor_name()]
	var before: bool = model.playback_pending and event_time >= 0 and event_time < model.impact_time()
	var enemy_hp: int = int(model.last_event.get("before_enemy", model.enemy_health)) if before else model.enemy_health
	enemy_label.text = "%s   %s / %s LP" % [model.enemy_name.to_upper(), enemy_hp, model.enemy_max_hp];enemy_bar.value = enemy_hp
	intent.text = model.enemy_intent()
	intent_button.disabled = _locked()
	var states: Array = displayed_states()
	for i in 2:
		var member: Dictionary = model.party[i]
		var hp: int = int(model.last_event.get("before_hp", [model.party[0].hp, model.party[1].hp])[i]) if before else member.hp
		hp_labels[i].text = "%s · %s / %s LP" % [member.name, hp, member.max_hp]
		hp_bars[i].value = hp
		var marks: Array[String] = []
		if states[i].barrier > 0: marks.append("Lichtschirm %s" % states[i].barrier)
		if states[i].bind > 0: marks.append("Gefesselt")
		if states[i].silence > 0: marks.append("Verstummt")
		status_labels[i].text = " / ".join(marks)
	_party_highlight()
	var state: Dictionary = model.decks[0 if encounter.get("practice") == true else model.cursor]
	var cost: int = Cards.get_card(state.hand[selected_card]).cost if selected_card >= 0 and selected_card < state.hand.size() else 0
	crystal_meter.update_value(int(state.crystals), int(state.cap), cost)
	crystal_label.text = "%s / %s Kristalle" % [state.crystals, state.cap]
	var hand: Dictionary = model.decks[_hand_owner()]
	hand_label.text = "%s  ·  Hand %s / 5  ·  Deck %s  ·  Ablage %s  ·  Halten zum Lesen" % [model.party[_hand_owner()].name, hand.hand.size(), hand.draw.size(), hand.discard.size()]
	queue_label.text = "Deine Karten bleiben sichtbar" if model.cursor == 2 else "Kräfte eingesetzt" if state.power else "Kräfte bereit"
	if encounter.get("practice") == true and model.has_root_bank() and model.cursor == 0:
		queue_label.text = "Wurzelbank · Erster Helfer +%s Schild" % model.ROOT_BANK_SHIELD if model.root_bank_available() else "Wurzelbank · Diese Runde genutzt"
	status.text = model.message if model.playback_pending else hint_message if not hint_message.is_empty() else _available_hint()
	if not model.playback_pending and is_instance_valid(focus) and not focus.forecasts.is_empty(): status.text += " · " + focus.summary()
	for button in buttons:
		var action: String = button.get_meta("battle_action", "")
		if model.phase in ["victory", "defeat"]: button.disabled = model.playback_pending
		elif action == "card": button.disabled = _locked() or not model.can_play(int(button.get_meta("hand_index", -1)))
		elif action in ["skill", "support", "special", "break_free", "flee", "end_turn"]: button.disabled = _locked() or not model.can_act(action)
		else: button.disabled = _locked()
		if action == "card": _card_affordance(button)
		elif action in ["skill", "support", "special", "break_free"]: _power_affordance(button)

func _refresh_unit_info() -> void :
	if not is_instance_valid(unit_info): return
	var unit: Dictionary = model.find_unit(inspected_unit)
	var shown: bool = not unit.is_empty() and not results_shown and not model.playback_pending and selected_card < 0
	unit_info.visible = shown;unit_info_name.visible = shown;unit_info_stats.visible = shown;unit_info_state.visible = shown
	unit_info_abilities.visible = shown
	if not shown: return
	var owner: String = model.enemy_name if int(unit.owner) == 2 else str(model.party[int(unit.owner)].name)
	unit_info_name.text = str(unit.name) + " · " + owner
	var stats: Array[String] = ["%s Angriff" % model.unit_attack(unit), "%s / %s LP" % [unit.hp, unit.max_hp]]
	if int(unit.get("shield", 0)) > 0: stats.append("%s Schild" % unit.shield)
	unit_info_stats.text = " · ".join(stats)
	var marks: Array[String] = [Feedback.state_text(Feedback.unit_state(model, unit))]
	if unit.taunt: marks.append("Schutzhelfer")
	if unit.get("lifesteal", false): marks.append("Lebensraub")
	if int(unit.get("regen", 0)) > 0: marks.append("Regeneration %s" % unit.regen)
	if unit.get("evasion", false): marks.append("1× Ausweichen")
	unit_info_state.text = " · ".join(marks)
	unit_info_abilities.text = Feedback.unit_abilities(unit)

func _position(index: int) -> Vector2:
	if index == 2 and _uses_three_roots() and model.enemy_id == "ash_hound":
		return Vector2(1055, 349)
	if _uses_three_roots() and index < 2:
		return Vector2(158, 458) if index == 0 else Vector2(268, 438)
	if encounter.get("practice") == true:
		return Location.ground(PRACTICE_PARTY[index], arena, 0.55)
	if arena.get("place", "") == "BEACH":
		return Location.ground([Vector2(338, 381), Vector2(475, 405), Vector2(1130, 419)][index], arena, [0.35, 0.85, 0.55][index])
	if not arena.is_empty():
		return Location.ground([Vector2(170, 389), Vector2(282, 440), Vector2(1014, 389)][index], arena, [0.35, 0.85, 0.55][index])
	return Vector2(1090, 426 if model.enemy_profile.get("kind", "") == "boss" else 368) if index == 2 else Vector2(141, 356) if index == 0 else Vector2(266, 422)

func _visible_board() -> Array:
	if model.playback_pending and event_time >= 0 and event_time < model.impact_time(): return model.last_event.get("before_board", model.board)
	var visible: Array = model.board.duplicate()
	if model.playback_pending and event_time >= model.impact_time() and event_time < model.animation_duration():
		for before in event.get("before_board", []):
			if model.find_unit(int(before.uid)).is_empty():
				var fallen: Dictionary = before.duplicate(true);fallen["_fallen"] = true;fallen.hp = 0;visible.append(fallen)
	return visible

func _unit_position(uid: int, previous: bool = false) -> Vector2:
	var entries: Array = model.last_event.get("before_board", model.board) if previous else model.board
	if model.playback_pending:
		var found := false
		for unit in entries:
			if int(unit.uid) == uid: found = true;break
		if not found: entries = model.last_event.get("before_board", model.board)
	var retained: Array = entries.duplicate()
	retained.append_array(model.board)
	if model.playback_pending: retained.append_array(model.last_event.get("before_board", []))
	for key in formation_slots.keys():
		var found := false
		for unit in retained:
			if int(unit.uid) == int(key): found = true;break
		if not found: formation_slots.erase(key)
	for unit in retained:
		var preferred := int(unit.get("formation_slot", -1))
		if preferred >= 0:
			formation_slots[int(unit.uid)] = {"owner": int(unit.owner), "slot": preferred}
	for unit in entries:
		var owner := int(unit.owner)
		if not formation_slots.has(int(unit.uid)):
			var used: Array = []
			for value in formation_slots.values():
				if int(value.owner) == owner: used.append(int(value.slot))
			for slot in 3:
				if slot not in used: formation_slots[int(unit.uid)] = {"owner": owner, "slot": slot};break
		if int(unit.uid) != uid: continue
		var slot: int = formation_slots.get(uid, {"slot": 0}).slot
		return _slot_position(owner, slot)
	return Vector2(415, 360)

func enemy_visual_state() -> String:
	if model.phase == "victory" and ( not model.playback_pending or event_time >= model.impact_time() + 0.4): return "fall"
	if event_time >= model.impact_time() and event_time < model.impact_time() + 0.4:
		for effect in event.get("effects", []):
			if int(effect.target) == 2 and effect.kind in ["damage", "weak", "blocked"]: return "hit"
	if event_time >= 0 and event_time < model.animation_duration() and int(event.get("source", -1)) == 2: return "cast"
	return "idle"

func _fighter(index: int) -> void :
	if index == 1 and encounter.get("practice") == true:
		return
	if index != 2: super._fighter(index);return
	if model.enemy_id != "":
		_regional_fighter()
		return
	var state := enemy_visual_state()
	var frame := 0
	var fall := 0.0
	if state == "cast": frame = 8 + mini(3, int(event_time / model.animation_duration() * 4))
	elif state == "hit": frame = 12
	elif state == "fall":
		fall = clampf((event_time - model.impact_time() - 0.4) / maxf(0.2, model.animation_duration() - model.impact_time() - 0.5), 0, 1)
		frame = 13 + mini(2, int(fall * 3))
	var region := _demon_rect(frame)
	var height := 190 * region.size.y / 304

	var motion := Motion.rest(_position(2), clock, 2)
	if state == "cast": motion = Motion.attack(_position(2), _position(0), event_time, model.impact_time(), model.animation_duration(), "guard", 0, 28)
	elif state == "hit": motion = Motion.hit(_position(2), event_time, model.impact_time(), 1, Impact.weight(event, "enemy"))
	_ground_shadow(motion.ground, Vector2(49, 12), motion.lift)
	var foot: Vector2 = motion.foot
	var tint := Color("ffdfca") if state == "hit" else Color.WHITE
	if state == "fall": tint.a = 1.0 - smoothstep(0.65, 1.0, fall) * 0.8
	_paint_pawn(demon, region, Vector2(205, height), motion, tint, false, "enemy")

func _draw() -> void :
	super._draw()
	if is_instance_valid(focus): focus.sync_draw()
	if results_shown: return
	draw_set_transform(margin, 0, Vector2.ONE * fit)
	_draw_targets()
	_draw_drag_hint()
	for unit in _visible_board():
		if event.get("visual", "") == "card_attack" and event_time >= 0 and event_time < model.animation_duration() and int(event.get("attacker_uid", -1)) == int(unit.uid): continue
		var enemy_attacking := false
		if event_time >= 0 and event_time < model.animation_duration():
			for swing in event.get("unit_attacks", []):
				if int(swing.uid) == int(unit.uid): enemy_attacking = true;break
		if enemy_attacking: continue
		_draw_unit(unit, _unit_position(int(unit.uid), model.playback_pending and event_time < model.impact_time()))
	draw_set_transform(Vector2.ZERO)

func _draw_unit(unit: Dictionary, foot: Vector2, attacking: bool = false, motion: Dictionary = {}) -> void :
	if str(unit.id).begins_with("vharok_seal_") or unit.id == "seal_anchor":
		_draw_seal(unit, foot);return
	var texture: Texture2D
	var region: Rect2
	var reference_height: float = 70
	var hit: bool = false
	if event_time >= model.impact_time() and event_time < model.impact_time() + 0.45:
		for effect in event.get("effects", []):
			if int(effect.target) == 3 and int(effect.get("unit", -1)) == int(unit.uid) and effect.kind == "damage": hit = true
	var feedback_state := Feedback.unit_state(model, unit)
	var sleeping: bool = feedback_state == "sleeping"
	var idle_phase := 0 if sleeping else int(clock * 1.7 + int(unit.uid) * 0.7) % 2
	if motion.is_empty(): motion = Motion.rest(foot, clock, int(unit.uid))
	var attack_phase := clampi(int(clampf((event_time - model.impact_time() + 0.2) / 0.4, 0, 1) * 4), 0, 3)
	var in_transit: bool = attacking and motion.travel
	if Cards.Strategy.has(str(unit.id)):
		var art: AtlasTexture = PracticeCreature.pose(1 if attacking else 2) if (
			str(unit.id) == PracticeCreature.SUBJECT
		) else Art.texture(Cards.get_card(str(unit.id)))
		texture = art.atlas;region = art.region;reference_height = region.size.y
	elif unit.get("form", "") in Cards.HELPER_FORMS:
		var action: String = "hit" if hit else "attack" if attacking and not in_transit else "idle"
		var progress: float = clampf((event_time - model.impact_time()) / 0.45, 0, 1) if hit else clampf((event_time - model.impact_time() + 0.2) / 0.4, 0, 1)
		var frame := Art.animation_frame(unit.form, 0.0 if sleeping else float(motion.stride) * 0.32 if in_transit else clock + float(unit.uid) * 0.37, action, progress)
		var art := Art.form_texture(unit.form, frame);texture = art.atlas;region = art.region;reference_height = Art.form_height(unit.form)
	elif int(unit.row) >= 0:
		var frame := 3 if hit else (int(motion.stride * 4) % 2 if in_transit else attack_phase) if attacking else 0
		var r: Array = animal_data.regions[int(unit.row)][frame]
		texture = animal_texture;region = Rect2(r[0], r[1], r[2], r[3]);reference_height = float(animal_data.regions[int(unit.row)][0][3])
	else:
		var species: String = unit.species
		if species == "enemy": texture = demon;region = _demon_rect(12 if hit else 4 + attack_phase if attacking else idle_phase);reference_height = 304
		else:
			var data: Dictionary = atlas_data.get(species, atlas_data.fennec)
			texture = load(data.asset)
			var r: Array = data.regions[15 if unit.get("_fallen", false) else 10 if hit else attack_phase if attacking and not in_transit else 12]
			region = Rect2(r[0], r[1], r[2], r[3]);reference_height = float(data.reference_height)
	var style: String = Art.movement_style(unit.form) if unit.get("form", "") in Cards.HELPER_FORMS else "pounce"
	if not attacking: motion = Motion.rest(foot, clock, int(unit.uid), style == "flier" and not sleeping)
	if hit and not attacking: motion = Motion.hit(foot, event_time, model.impact_time(), 1 if int(unit.owner) == 2 else -1, Impact.weight(event, "u:" + str(unit.uid)))
	foot = motion.foot
	var display_height := _helper_height(foot)
	if unit.get("form", "") == "ancient": display_height += 16.0
	if unit.get("form", "") == "moss_guard": display_height *= 1.48
	var scale_factor := display_height / reference_height
	var dimensions := region.size * scale_factor
	var left := _helper_flipped(unit, attacking and motion.travel and motion.returning)
	if in_transit:
		var art := _unit_travel_art(unit, motion)
		if not art.is_empty(): texture = art.texture;region = art.region;dimensions = art.dimensions * (display_height / 78.0)
	var color := Color("ffd2b8") if hit else Color.WHITE if attacking else Color("b8c5d4") if sleeping else Color("d6c4c9") if feedback_state == "spent" else Color.WHITE
	if unit.get("_fallen", false):
		var fall := smoothstep(model.impact_time() + 0.05, model.animation_duration() - 0.08, event_time)
		color.a = 1.0 - fall;motion.lean = (0.2 if int(unit.owner) < 2 else - 0.2) * fall
	_ground_shadow(motion.ground, Vector2(maxf(31.0, dimensions.x * 0.29), 8.0), motion.lift, color.a)
	var contact := PackedVector2Array()
	for i in 32:
		var a := i * TAU / 32;contact.append(motion.ground + Vector2(cos(a) * 31, sin(a) * 7))
	_paint_pawn(texture, region, dimensions, motion, color, left, "u:" + str(unit.uid))
	if unit.get("_fallen", false): return
	var health_color := Color("d8ba7b") if unit.taunt else Color("b38292") if int(unit.owner) == 2 else Color("85bbab")
	draw_rect(Rect2(foot + Vector2(-29, 9), Vector2(58, 4)), Color(0.03, 0.07, 0.09, 0.85))
	draw_rect(Rect2(foot + Vector2(-29, 9), Vector2(58 * clampf(float(unit.hp) / float(unit.max_hp), 0, 1), 4)), health_color)
	if int(unit.uid) == selected_unit or int(unit.uid) == inspected_unit:
		draw_polyline(contact + PackedVector2Array([contact[0]]), Color("f8e6a0"), 2, true)
		_word("%s / %s" % [model.unit_attack(unit), unit.hp], foot + Vector2(-24, 34), 16, Color("f4dfab"))
	_unit_affordance(unit, foot, feedback_state, attacking)
	if int(unit.get("shield", 0)) > 0:
		draw_arc(foot - Vector2(0, 35), 44, - 0.4, PI + 0.4, 32, Color(0.8, 0.7, 0.4, 0.52), 2, true)
		_word(str(unit.shield), foot + Vector2(30, 13), 12, Color("ebd29f"))
	if unit.get("evasion", false): draw_arc(foot - Vector2(0, 32), 50, - 0.4, PI + 0.4, 36, Color(0.7, 0.6, 0.9, 0.38), 2, true)
	if int(unit.get("frozen", 0)) > 0: _word("STILL", foot + Vector2(-18, -77), 12, Color("a8bdea"))

func _helper_height(foot: Vector2) -> float:
	return lerpf(98.0, 115.0, clampf((foot.y - 398.0) / 64.0, 0.0, 1.0))

func _helper_flipped(unit: Dictionary, returning: bool) -> bool:
	var face_left: bool = (int(unit.owner) == 2) != returning
	var form: String = unit.get("form", "")
	var authored_left: bool = Art.form_info(form).get("flip_base", false) if form in Cards.HELPER_FORMS else unit.get("species", "") == "enemy"
	return authored_left != face_left

func _wolf_impact_point() -> Vector2:
	return _target_point(event.get("wolf_target", "h:0"))

func _target_point(code: String) -> Vector2:
	if code == "enemy": return _position(2) - Vector2(0, 75)
	if code.begins_with("h:"): return _position(int(code.substr(2))) - Vector2(0, 65)
	return _unit_position(int(code.substr(2)), true) - Vector2(0, 40)

func _effects() -> void :
	var visual: String = event.get("visual", "")
	var cinematic: bool = event.has("cinematic_card")
	var custom_labels := is_instance_valid(practice_impact_labels) and PracticeFX.has_impact_labels(event)
	if cinematic and not event.get("cinematic_short", false) and event_time < PracticeFilm.CUT_END:
		return
	if cinematic:
		PracticeFX.cinematic_impact(self, event)
	if event.has("wolf_attack"):
		_enemy_units_effects()
		return
	if visual == "ecology":
		EcologyFX.draw(self, event_time, event);_enemy_units_effects();_effect_numbers(event_time, Color("f1d8b0"));_unit_numbers();return
	if visual.begins_with("special_"):
		if not cinematic: special_fx.draw(self, event_time, event)
		_enemy_units_effects()
		if not custom_labels:
			_effect_numbers(event_time, Color("e4dfb9"));_unit_numbers()
		return
	if not visual.begins_with("card_"):
		super._effects()
		_enemy_units_effects()
		_unit_numbers()
		return
	var source := _position(int(event.get("source", 0))) - Vector2(0, 80)
	var impact: float = event.get("impact", 0.7)
	if visual == "card_summon" and not cinematic:
		var foot := _unit_position(int(event.get("unit_uid", -1)))
		if Cards.Strategy.has(str(event.get("card", ""))): StrategySummonFX.draw(self, event_time, event, foot, source)
		else:
			var p := smoothstep(0.1, impact, event_time)
			var at := source.lerp(foot - Vector2(0, 28), p) - Vector2(0, sin(p * PI) * 28)
			spectacle.star(self, at, 13, Color("c9e6a1"), clock)
			spectacle.burst(self, foot - Vector2(0, 30), Color("c9e6a1"), 1 - smoothstep(impact, impact + 0.6, event_time), 15 + p * 37)
	elif visual == "card_attack":
		var uid := int(event.get("attacker_uid", -1))
		var attacker: Dictionary = {}
		for entry in event.get("before_board", []):
			if int(entry.uid) == uid: attacker = entry;break
		if not attacker.is_empty():
			var motion := _attack_motion(attacker, str(event.get("target_code", "enemy")))
			_draw_unit(attacker, motion.foot, true, motion)
	elif visual == "card_spell":
		if event.has("card"):
			PracticeFX.spell(self, event, Cards.get_card(event.card))
		else:
			var target := _target_point(event.get("target_code", "enemy"))
			var p := smoothstep(0.1, impact, event_time)
			var at := source.lerp(target, p) - Vector2(0, sin(p * PI) * 45)
			spectacle.star(self, at, 12, Color("ffe6a7"), clock)
			if event_time >= impact: spectacle.burst(self, target, Color("ffe6a7"), 1 - smoothstep(impact, impact + 0.7, event_time), 22 + (event_time - impact) * 65)
	_enemy_units_effects()
	if not custom_labels:
		_effect_numbers(event_time, Color("d4eab7"))
		_unit_numbers()

func _enemy_units_effects() -> void :
	var lane := 0
	for swing in event.get("unit_attacks", []):
		var uid := int(swing.uid); var attacker: Dictionary = {}
		for old in event.get("before_board", []):
			if int(old.uid) == uid: attacker = old;break
		if attacker.is_empty(): continue
		var motion := _attack_motion(attacker, str(swing.target_code), lane - 1)
		_draw_unit(attacker, motion.foot, true, motion);lane += 1

func _unit_numbers() -> void :
	var impact: float = event.get("impact", 0.7)
	if event_time >= impact and event_time < impact + 1:
		for effect in event.get("effects", []):
			if int(effect.target) == 3: _word("Ausweichen" if effect.kind == "evade" else str(effect.amount), _visual_unit_position(int(effect.unit)) - Vector2(8, 80 + (event_time - impact) * 22), Impact.number_size(event, "u:" + str(effect.unit)), Color("cfc1ef") if effect.kind == "evade" else Color("ffe5c9"))

func _available_hint() -> String:
	if model.cursor == 2: return model.enemy_name + " ist am Zug"
	var cards := 0; var ready := 0; var powers := 0
	for i in model.decks[model.cursor].hand.size():
		if model.can_play(i): cards += 1
	for unit in model.units(model.cursor):
		if model.can_attack(int(unit.uid)): ready += 1
	for action in ["skill", "support", "special", "break_free"]:
		if model.can_act(action): powers += 1
	return "%s Karten · %s Helfer · %s Kräfte bereit" % [cards, ready, powers] if cards + ready + powers > 0 else "Keine freie Aktion · Zug beenden"

func _card_affordance(button: Button) -> void :
	var index: = int(button.get_meta("hand_index", -1))
	var playable: bool = not button.disabled
	var selected: bool = index == selected_card
	var key: = str(playable) + str(selected)
	if button.get_meta("affordance", "") == key: return
	button.set_meta("affordance", key);button.set_meta("playable", playable)
	var card: Dictionary = Cards.get_card(str(button.get_meta("card_id", "")))
	var color: = Color("ddbb7e") if playable else Color("4b5e6b")
	if selected: color = Color("ffe49b")
	var box: = _box(Color("352c26") if playable else Color("14212f"))
	box.border_color = color;box.set_border_width_all(3 if selected else 2 if playable else 1)
	box.shadow_color = Color(color, 0.32 if playable else 0.0);box.shadow_size = 0
	button.add_theme_stylebox_override("normal", box);button.add_theme_stylebox_override("disabled", box)
	var hover: = box.duplicate();hover.bg_color = Color("38575b")
	button.add_theme_stylebox_override("hover", hover);button.add_theme_stylebox_override("pressed", hover)
	button.modulate = Color.WHITE if playable else Color(0.84, 0.86, 0.9, 1)

func _draw_targets() -> void :
	if _locked(): return
	var choices := Feedback.targets(model, selected_card, selected_unit)
	for code in choices:
		var at := _target_point(code)
		var r := 64.0 if code == "enemy" else 45.0
		draw_arc(at, r, 0, TAU, 48, Color(0.95, 0.86, 0.52, 0.85), 3, true)
		draw_arc(at, r + 5, clock * 0.8, clock * 0.8 + PI * 0.7, 24, Color(0.95, 0.86, 0.52, 0.5), 2, true)
		if code == "enemy": _word("ZIEL WÄHLEN", at + Vector2(-51, - r - 12), 14, Color("ffe8a8"))

func _unit_affordance(unit: Dictionary, foot: Vector2, state: String, attacking: bool) -> void :
	if attacking: return
	if str(unit.id).begins_with("vharok_seal_") or unit.id == "seal_anchor": state = "ritual"
	var color := Color("a4bdd5")
	var label: String = ""
	match state:
		"ritual": label = "Siegel aktiv";color = Color("ebc789")
		"ready":
			color = Color("a4f0c4");label = "Bereit"
			var ring := PackedVector2Array()
			for i in 41:
				var a := i * TAU / 40;ring.append(foot + Vector2(cos(a) * 44, sin(a) * 10))
			draw_polyline(ring, Color(color, 0.25 + 0.1 * sin(clock * 2)), 7, true)
			draw_polyline(ring, color, 1.8, true)

			var at := foot + Vector2(42, -46)
			for side in [-1, 1]:
				draw_line(at + Vector2(-7, side * 8), at + Vector2(7, - side * 8), color, 2.5, true)
		"waiting": label = "Nächster Zug";color = Color("cfbf93")
		"spent": label = "Erschöpft";color = Color("a6a4b2")
		"frozen": label = "Festgehalten";color = Color("b4c8f2")
		"passive": label = "0 Angriff"
		"sleeping": _word("Z", foot + Vector2(31, -53), 14, Color(color, 0.8))
	if not is_instance_valid(focus) or not focus.forecasts.has("u:" + str(unit.uid)):
		_word(label, foot + Vector2(-32, 28), 12, color)

	draw_circle(foot + Vector2(-42, -3), 12, Color("4d4031"))
	draw_circle(foot + Vector2(42, -3), 12, Color("432b38"))
	_word(str(model.unit_attack(unit)), foot + Vector2(-47, 2), 13, Color("ffe5a4"))
	_word(str(unit.hp), foot + Vector2(37, 2), 13, Color("ffd6db"))

func _power_affordance(button: Button) -> void :
	var key := str(button.disabled)
	if button.get_meta("affordance", "") == key: return
	button.set_meta("affordance", key)
	var box := _box(Color("244139") if not button.disabled else Color("162433"))
	box.border_color = Color("9edab6") if not button.disabled else Color("495763")
	box.set_border_width_all(2 if not button.disabled else 1)
	button.add_theme_stylebox_override("normal", box);button.add_theme_stylebox_override("disabled", box)

func _regional_fighter() -> void :
	if model.enemy_id == "ash_hound":
		_ash_hound_fighter()
		return
	var state := enemy_visual_state()
	var pose := 0
	var motion := Motion.rest(_position(2), clock, 2, model.enemy_profile.get("visual_kind", model.enemy_profile.kind) == "moth")
	var preparing: bool = model.demon_charge and model.enemy_id == "glut_imp"
	preparing = preparing or (model.enemy_id == "cinder_carrier" and bool(model.ecology_state.get("fuel", false)))
	if state == "cast":
		var action: String = event.get("ecology_action", "")
		if preparing or action in ["warning", "charge", "collect", "seals"]:
			pose = 1
		elif action != "interrupt":
			pose = 1 if event_time < model.impact_time() else 2
			motion = Motion.attack(_position(2), _effect_ground(), event_time, model.impact_time(), model.animation_duration(), "guard", 0, 28)
			if model.enemy_id == "cinder_carrier" and motion.travel:
				pose = 6
	elif state == "hit": pose = 3;motion = Motion.hit(_position(2), event_time, model.impact_time(), 1, Impact.weight(event, "enemy"))
	elif state == "fall": pose = 4
	elif preparing: pose = 1
	elif model.seal_pending: pose = 5
	var foot: Vector2 = motion.foot
	_ground_shadow(motion.ground, Vector2(62, 14) if model.enemy_profile.kind == "boss" else Vector2(39, 10), motion.lift)
	var region := Enemies.region(model.enemy_id, pose)
	var texture: Texture2D = load(Enemies.asset(model.enemy_id))
	var dimensions := _regional_dimensions(pose)
	var flip: bool = model.enemy_id == "cinder_carrier" and motion.travel and motion.returning
	if model.enemy_id == "glut_imp" and (motion.travel or (state == "idle" and not model.demon_charge)):
		if glut_gait == null:
			glut_gait = NpcLocomotion.new()
			glut_gait.configure("glut_imp")
		var cycle := "walk" if motion.travel else "rest"
		var progress: float = float(motion.stride) * 0.8 if motion.travel else clock * 0.5
		var picture := glut_gait.frame_picture(cycle, progress, 190.0)
		region = picture.region
		texture = picture.texture
		dimensions = picture.size
		flip = motion.travel and motion.returning
	if Enemies.heavy_motion(model.enemy_id) and state != "fall":
		var moving: float = sin(float(motion.progress) * PI) if motion.travel else 0.0
		var weight := Enemies.weight_shift(clock, motion.stride, moving)
		motion.lean += weight.x
		dimensions.y *= 1.0 + weight.y
	var tint := Color("ffd6bd") if state == "hit" else Color.WHITE
	if model.demon_charge or model.seal_pending:
		var pulse := 0.55 + 0.15 * sin(clock * 2.5)
		draw_arc(foot - Vector2(0, 70), 82, 0, TAU, 48, Color(1.0, 0.35, 0.12, pulse) if model.demon_charge else Color(0.74, 0.32, 0.88, pulse), 2, true)
	_paint_pawn(texture, region, dimensions, motion, tint, flip, "enemy")

func _ash_hound_fighter() -> void :
	var home := _position(2)
	var state := enemy_visual_state()
	var motion := Motion.rest(home, clock, 2)
	var pose := 5
	var toward_left := true
	if model.playback_pending and event.has("wolf_attack") and event_time >= 0.0 and event_time < model.animation_duration():
		var target := _wolf_impact_point() + Vector2(0, 65 if not str(event.wolf_target).begins_with("u:") else 40)
		motion = AshHound.motion(home, target, event_time, model.impact_time(), model.animation_duration(), event.wolf_attack == "leap")
		pose = motion.pose
		toward_left = motion.toward_left
		if float(motion.lift) < 3.0:
			_ellipse(motion.ground + Vector2(27 if toward_left else -27, 5), Vector2(29, 6), Color(0.53, 0.37, 0.23, 0.13))
	elif state == "hit":
		pose = 3
		motion = Motion.hit(home, event_time, model.impact_time(), 1, Impact.weight(event, "enemy"))
	elif state == "fall":
		pose = 4
	var region: Rect2 = AshHound.REGIONS[pose]
	var tint := Color("ffd6bd") if state == "hit" else Color.WHITE
	_ground_shadow(motion.ground, Vector2(51, 10), motion.lift)
	_paint_pawn(load(Enemies.asset("ash_hound")), region, region.size * AshHound.HEIGHT_SCALE, motion, tint, AshHound.flip_for(pose, toward_left), "enemy")

func _draw_seal(unit: Dictionary, foot: Vector2) -> void :
	var at := foot - Vector2(0, 40)
	draw_circle(at, 35, Color("30253d"))
	draw_arc(at, 32, 0, TAU, 48, Color("e2ba79"), 3, true)
	draw_arc(at, 39, clock * 0.8, clock * 0.8 + PI * 1.4, 40, Color("b78ce0"), 2, true)
	draw_colored_polygon(PackedVector2Array([at + Vector2(0, -26), at + Vector2(17, 0), at + Vector2(0, 26), at + Vector2(-17, 0)]), Color("bd7a42"))
	_unit_affordance(unit, foot, "ritual", false)


func _regional_dimensions(pose: int) -> Vector2:
	var reference_height := Enemies.region(model.enemy_id, 0).size.y
	if model.enemy_profile.get("ecology_atlas", false):
		for frame in 7: reference_height = maxf(reference_height, Enemies.region(model.enemy_id, frame).size.y)
	return Enemies.region(model.enemy_id, pose).size * (250.0 if model.enemy_profile.kind == "boss" else 190.0) / reference_height

func _attack_motion(unit: Dictionary, target_code: String, lane: int = 0) -> Dictionary:
	var home := _unit_position(int(unit.uid), true)
	var target := _target_point(target_code) + Vector2(0, 40)
	var style: String = Art.movement_style(str(unit.form)) if unit.get("form", "") in Cards.HELPER_FORMS else "pounce"
	var motion := Motion.attack(home, target, event_time, model.impact_time(), model.animation_duration(), style, lane)
	if model.playback_pending and event_time >= model.impact_time() and model.find_unit(int(unit.uid)).is_empty():

		motion = Motion.attack(home, target, model.impact_time(), model.impact_time(), model.animation_duration(), style, lane)
		unit["_fallen"] = true
	return motion


func _uses_species_art(unit: Dictionary) -> bool:
	return not Cards.Strategy.has(str(unit.id)) and str(unit.get("form", "")) not in Cards.HELPER_FORMS and int(unit.get("row", -1)) < 0 and Catalog.data().characters.has(str(unit.get("species", "")))

func _unit_travel_art(unit: Dictionary, motion: Dictionary) -> Dictionary:
	return _travel_art(str(unit.species), motion) if _uses_species_art(unit) else {}

func _travel_art(species: String, motion: Dictionary) -> Dictionary:
	if species in Catalog.STARTERS:
		var picture := _current_picture(species, "run", int(motion.stride * 10.0))
		picture.dimensions *= 78.0 / 140.0
		return picture
	if not is_instance_valid(encounter): return {}
	if not encounter.app.character_metrics.has(species):
		if not Catalog.data().characters.get(species, {}).get("compact", false): return {}


		var raw: Array = JSON.parse_string(FileAccess.get_file_as_string("res://data/compact_bounds.json"))[species]
		var bounds: Array[Rect2i] = []
		for b in raw: bounds.append(Rect2i(b[0], b[1], b[2], b[3]))
		encounter.app.character_metrics[species] = {"bounds": bounds, "scales": {}}
	var compact: bool = Catalog.data().characters[species].get("compact", false)
	var frame := 7 + posmod(int(motion.stride * 4), 4) if compact else 9 + posmod(int(motion.stride * 8), 8)
	var texture: Texture2D = load(Catalog.data().characters[species].asset)
	var columns := 8 if compact else 12
	var cell := Vector2(texture.get_width() / float(columns), texture.get_height() / float(6 if compact else 15))
	var bounds: Rect2i = encounter.app.character_metrics[species].bounds[frame]
	var reference: Rect2i = encounter.app.character_metrics[species].bounds[0 if compact else 142]
	var region := Rect2(Vector2(frame % columns, frame / columns) * cell + Vector2(bounds.position), Vector2(bounds.size))
	if compact: region = Pawn.PoseGeometry.travel_region(texture, frame, region)
	return {"texture": texture, "region": region, "dimensions": region.size * 78.0 / maxf(1, reference.size.y)}

func _member_picture(species: String, frame: int, motion: Dictionary, casting: bool) -> Dictionary:
	if species not in Catalog.STARTERS: return {}
	if motion.travel:
		return _current_picture(species, "run", int(motion.stride * 10.0))
	if species != "mosslyn":
		return _current_picture(species, "run" if casting else "rest", int(event_time * 8.0) if casting else 0)
	if casting:
		var progress := event_time / maxf(0.1, model.animation_duration())
		var mode := "lower" if progress < 0.55 else "rise"
		return _mosslyn_picture(mode, mini(3, int(fposmod(progress, 0.55) / 0.55 * 4)))
	if frame in [8, 10, 15]: return _mosslyn_picture("lower", 1)
	return _mosslyn_picture("rest", 0)

func _current_picture(species: String, mode: String, phase: int) -> Dictionary:
	if species == "mosslyn": return _mosslyn_picture(mode, posmod(phase, int(mosslyn_cycles[mode].frames)))
	if not companion_cycles.has(species):
		companion_cycles[species] = JSON.parse_string(FileAccess.get_file_as_string("res://data/" + species + "_authored_cycles.json"))
	var cycle: Dictionary = companion_cycles[species][mode]
	var key := species + ":" + mode
	if not companion_textures.has(key):
		companion_textures[key] = load(str(cycle.asset).replace("/characters/", "/characters/pixel/"))
	phase = posmod(phase, int(cycle.frames))
	var b: Array = cycle.bounds[phase]
	var origin := Vector2(phase % int(cycle.columns), phase / int(cycle.columns)) * float(cycle.cell)
	var rect := Rect2(origin + Vector2(b[0], b[1]), Vector2(b[2], b[3]))
	return {"texture": companion_textures[key], "region": rect, "dimensions": rect.size * 140.0 / float(cycle.reference_height)}

func _mosslyn_picture(mode: String, phase: int) -> Dictionary:
	var cycle: Dictionary = mosslyn_cycles[mode]
	if not mosslyn_textures.has(mode):
		mosslyn_textures[mode] = load(str(cycle.asset).replace("/characters/", "/characters/pixel/"))
	var b: Array = cycle.bounds[phase]
	var origin := Vector2(phase % int(cycle.columns), phase / int(cycle.columns)) * float(cycle.cell)
	var rect := Rect2(origin + Vector2(b[0], b[1]), Vector2(b[2], b[3]))
	return {"texture": mosslyn_textures[mode], "region": rect, "dimensions": rect.size * 140.0 / float(cycle.reference_height)}

func _arena_ground() -> void :
	if _uses_three_roots():
		var bank_ready: bool = encounter.get("practice") == true and model.root_bank_available()
		ThreeRoots.draw_atmosphere(self, clock, _slot_position(0, 0), bank_ready)
	elif not arena.is_empty():
		Location.draw_atmosphere(self, arena, clock)
	if model.enemy_id == "ash_hound" and model.phase == "battle" and not model.playback_pending and is_instance_valid(focus):
		if not is_instance_valid(card_popup) and selected_card < 0 and selected_unit < 0:
			var target := _announced_wolf_target()
			if not target.is_empty():
				ThreeRoots.draw_wolf_trail(self, _position(2), focus._ground(target), clock)

func _arena_fire() -> void :
	if arena.is_empty(): super._arena_fire()

func _announced_wolf_target() -> String:
	return str(focus.intent_preview().get("wolf_target", ""))

func _background_rect() -> Rect2:
	return Rect2(margin, Vector2(1280, 720) * fit) if encounter.get("practice") == true else super._background_rect()

func _ground_shadow(ground: Vector2, radii: Vector2, lift: float = 0.0, gain: float = 1.0) -> void :
	var contact := 1.0 - clampf(lift / 48.0, 0.0, 1.0)
	_ellipse(ground + Vector2(6, 4), radii * Vector2(1.48, 1.32), Color(0.018, 0.019, 0.025, gain * (0.13 + 0.07 * contact)))
	_ellipse(ground + Vector2(1, 1), radii, Color(0.016, 0.017, 0.02, gain * (0.13 + 0.21 * contact)))
	_ellipse(ground + Vector2(0, 1), radii * Vector2(0.71, 0.48), Color(0.012, 0.014, 0.016, gain * 0.36 * contact))

func _visual_unit_position(uid: int) -> Vector2:
	if event_time < 0 or event_time >= model.animation_duration(): return _unit_position(uid, true)
	var attacker: Dictionary = {}
	for unit in event.get("before_board", []):
		if int(unit.uid) == uid: attacker = unit;break
	if not attacker.is_empty():
		if event.get("visual", "") == "card_attack" and int(event.get("attacker_uid", -1)) == uid:
			return _attack_motion(attacker, str(event.get("target_code", "enemy"))).foot
		var lane := 0
		for swing in event.get("unit_attacks", []):
			if int(swing.uid) == uid: return _attack_motion(attacker, str(swing.target_code), lane - 1).foot
			lane += 1
	return _unit_position(uid, true)

func _slot_position(owner: int, slot: int) -> Vector2:
	if owner == 0 and slot == 0 and _uses_three_roots():
		return Vector2(428, 412)
	if encounter.get("practice") == true:
		return Location.ground(PRACTICE_FOES[slot] if owner == 2 else PRACTICE_ALLIES[slot], arena, 0.55)
	if owner == 2: return Location.ground(Vector2(851 + slot * 141, 442 if slot != 1 else 450), arena, 0.75)
	return Location.ground(Vector2(415 + slot * 119 + 56 * owner, 333 + slot * 12 if owner == 0 else 440 + slot * 3), arena, 0.16 if owner == 0 else 0.94)

func _hand_owner() -> int:
	return model.cursor if model.cursor < 2 else 1 if model.party[1].hp > 0 else 0

func _next_actor_name() -> String:
	for step in range(1, 4):
		var index: int = (model.cursor + step) % 3
		if index == 2: return model.enemy_name.split("·")[0].strip_edges()
		if model.party[index].hp > 0: return str(model.party[index].name)
	return "—"

func _party_highlight() -> void :
	if party_focus == model.cursor: return
	party_focus = model.cursor
	for i in party_plates.size():
		var box := _box(Color("182e33") if i == party_focus else Color("101b29"))
		box.border_color = GOLD if i == party_focus else Color("4a5b64")
		box.border_width_left = 3 if i == party_focus else 1
		party_plates[i].add_theme_stylebox_override("panel", box)
		hp_labels[i].add_theme_color_override("font_color", GOLD if i == party_focus else TEXT)

func _presentation_light(light: Dictionary, code: String) -> Dictionary:
	var result: Dictionary = focus.light(light, code) if is_instance_valid(focus) else light
	if not drag_hint.is_empty() and drag_hint.get("code") == code:
		result.pulse_color = Color("ffdf90")
		result.pulse_gain = 0.22 + 0.28 * _drag_glow()
	return result



func route_card_pointer(event_input: InputEvent) -> bool:
	var touch: bool = event_input is InputEventScreenTouch
	var mouse: bool = event_input is InputEventMouseButton and event_input.button_index == MOUSE_BUTTON_LEFT
	var move: bool = event_input is InputEventScreenDrag or event_input is InputEventMouseMotion
	if not touch and not mouse and not move: return false
	if is_instance_valid(practice_film) and practice_film.visible:
		for button in [pause_film_button, skip_film_button]:
			if button.get_global_rect().has_point(event_input.position):
				if touch and event_input.pressed and not event_input.canceled:
					button.pressed.emit()
				return touch
		return true
	var id: int = event_input.index if touch or event_input is InputEventScreenDrag else -1
	if pointer_id != -99:
		if id != pointer_id: return touch or mouse
		if move:
			pointer_position = event_input.position
			if (pointer_dragging or pointer_position.distance_to(pointer_start) > 14.0 * fit):
				if pointer_peek:
					_close_popup()
					input_cooldown = 0.0
					pointer_peek = false
				if not pointer_dragging:
					drop_choices = _card_choices(pointer_card)
				pointer_dragging = true
				_update_drag_card()
			return true
		if event_input.pressed: return true
		var index := pointer_card
		var valid_release: bool = not (touch and event_input.canceled) and pointer_turn == model.turn
		var drop_at: Vector2 = (event_input.position - margin) / fit
		var destination := _card_destination(index, drop_at) if pointer_dragging and valid_release else {}
		var tap: bool = valid_release and not pointer_dragging and not pointer_peek and event_input.position.distance_to(pointer_start) <= 14.0 * fit
		pointer_id = -99
		pointer_card = -1
		pointer_peek = false
		pointer_dragging = false
		drag_hint.clear()
		_finish_drag(destination)
		if not destination.is_empty():
			encounter._card_play(index, destination.get("target", "enemy"), destination.get("slot", -1))
		elif tap:
			_pick_card(index)
		return true
	if move or not event_input.pressed or (touch and event_input.canceled) or is_instance_valid(card_popup) or is_instance_valid(intent_popup): return false
	for i in range(buttons.size() - 1, -1, -1):
		var button: Button = buttons[i]
		if button.get_meta("battle_action", "") != "card": continue
		if not button.is_visible_in_tree() and not button.has_meta("returning_card"): continue
		var local_point: Vector2 = button.get_global_transform_with_canvas().affine_inverse() * event_input.position
		if not Rect2(Vector2.ZERO, button.size).has_point(local_point): continue
		if _locked(): return true
		pointer_id = id;pointer_card = int(button.get_meta("hand_index"));pointer_start = event_input.position;pointer_time = 0;pointer_peek = false;pointer_turn = model.turn
		pointer_position = pointer_start
		if button.has_meta("returning_card"):
			var returning: Button = button.get_meta("returning_card")
			if is_instance_valid(returning): returning.queue_free()
			button.remove_meta("returning_card")
			button.show()
		drag_source = button
		drag_origin = button.position
		pointer_dragging = false
		return true
	return false

func _update_drag_card() -> void:
	if not is_instance_valid(drag_card):
		drag_card = Button.new()
		drag_card.mouse_filter = Control.MOUSE_FILTER_IGNORE
		drag_card.size = HAND_CARD_SIZE
		drag_card.clip_contents = true
		if is_instance_valid(drag_source):
			drag_card.add_theme_stylebox_override("normal", drag_source.get_theme_stylebox("normal"))
			drag_card.modulate = drag_source.modulate
			drag_source.hide()
		drag_card.z_index = 10
		layout.add_child(drag_card)
		var hand: Array = model.decks[model.cursor].hand
		_card_face(drag_card, Cards.get_card(hand[pointer_card]))
	var at := (pointer_position - margin) / fit
	drag_card.position = Vector2(clampf(at.x - HAND_CARD_SIZE.x * 0.5, 0, 1280 - HAND_CARD_SIZE.x), clampf(at.y - HAND_CARD_SIZE.y - 12, 0, 720 - HAND_CARD_SIZE.y))
	var destination := _nearest_choice(drop_choices, at)
	if destination.get("slot", -1) != drag_hint.get("slot", -1) or destination.get("code") != drag_hint.get("code"):
		drag_hint_since = clock
	drag_hint = destination

func _finish_drag(destination: Dictionary) -> void:
	if not is_instance_valid(drag_card): return
	var card := drag_card
	drag_card = null
	drop_choices.clear()
	if not destination.is_empty():
		var landing: Vector2 = destination.get("foot", destination.center)
		card.pivot_offset = card.size * 0.5
		var arrival := card.create_tween().set_parallel().set_trans(Tween.TRANS_QUAD).set_ease(Tween.EASE_IN)
		arrival.tween_property(card, "position", landing - card.pivot_offset, 0.16)
		arrival.tween_property(card, "scale", Vector2.ONE * 0.18, 0.16)
		arrival.tween_property(card, "modulate:a", 0.0, 0.16)
		arrival.chain().tween_callback(card.queue_free)
		return
	var tween := card.create_tween().set_parallel().set_trans(Tween.TRANS_QUAD).set_ease(Tween.EASE_OUT)
	tween.tween_property(card, "position", drag_origin, 0.18)
	if is_instance_valid(drag_source):
		card.pivot_offset = drag_source.pivot_offset
		tween.tween_property(card, "rotation", drag_source.rotation, 0.18)
		drag_source.set_meta("returning_card", card)
		tween.chain().tween_callback(drag_source.show)
		tween.tween_callback(drag_source.remove_meta.bind("returning_card"))
	tween.chain().tween_callback(card.queue_free)

func _card_destination(index: int, at: Vector2) -> Dictionary:
	return _nearest_choice(_card_choices(index), at)

func _card_choices(index: int) -> Array[Dictionary]:
	var choices: Array[Dictionary] = []
	if _locked() or not model.can_play(index):
		return choices
	var owner: int = model.cursor
	var card: Dictionary = Cards.get_card(model.decks[owner].hand[index])
	var targets: Array[String] = model.card_targets(owner, card)
	var requires_target: bool = card.kind == "damage" or card.has("target") or not targets.is_empty()
	if not requires_target and (card.kind == "minion" or (card.kind == "revive" and not card.get("reclaim", false))):
		var occupied: Array[int] = []
		for unit in model.units(owner):
			_unit_position(int(unit.uid))
			occupied.append(int(formation_slots.get(int(unit.uid), {"slot": -1}).slot))
		for slot in model.BOARD_LIMIT:
			var foot := _slot_position(owner, slot)
			if slot not in occupied:
				choices.append({"slot": slot, "foot": foot, "center": foot - Vector2(0, 16), "radii": Vector2(54, 44)})
		return choices
	if not requires_target:
		match card.kind:
			"weaken", "volley":
				targets.append("enemy")
			"party_heal":
				for hero in model.party.size():
					if model.party[hero].hp > 0: targets.append("h:" + str(hero))
			"team_buff":
				for unit in model.units(owner): targets.append("u:" + str(unit.uid))
			_:
				targets.append("h:" + str(owner))
	for code in targets:
		if not requires_target or model.can_play(index, code):
			choices.append({"code": code, "target": code if requires_target else "enemy",
				"center": _drop_target_point(code), "radii": _drop_radii(code)})
	return choices

func _nearest_choice(choices: Array[Dictionary], at: Vector2) -> Dictionary:
	if not Rect2(18, 164, 1242, 330).has_point(at): return {}
	var closest: Dictionary = {}
	var gap := 1.0
	for choice in choices:
		var distance: float = ((at - choice.center) / choice.radii).length()
		if distance <= gap:
			gap = distance
			closest = choice
	return closest

func _drop_target_point(code: String) -> Vector2:
	if code.begins_with("u:"):
		var uid := int(code.substr(2))
		var center_height := 72.0 if model.find_unit(uid).get("form") == "moss_guard" else 39.0
		return _unit_position(uid) - Vector2(0, center_height)
	return _target_point(code)

func _drop_radii(code: String) -> Vector2:
	if code == "enemy": return Vector2(78, 97)
	if code.begins_with("u:"):
		return Vector2(66, 75) if model.find_unit(int(code.substr(2))).get("form") == "moss_guard" else Vector2(43, 49)
	return Vector2(56, 73)

func _drag_glow() -> float:
	return 0.5 + 0.5 * cos((clock - drag_hint_since) * 8.0)

func _draw_drag_hint() -> void:
	if not pointer_dragging or _locked(): return
	for choice in drop_choices:
		if choice == drag_hint: continue
		var point: Vector2 = choice.get("foot", choice.center)
		var radii: Vector2 = Vector2(50, 16) if choice.has("slot") else choice.radii * 0.9
		_draw_drop_ring(point, radii, Color("edcf885e"))
	if drag_hint.is_empty(): return
	var glow := _drag_glow()
	var gold := Color("ffdf90")
	if drag_hint.has("slot"):
		var foot: Vector2 = drag_hint.foot
		_ellipse(foot, Vector2(52, 18), Color(gold, 0.20 + glow * 0.20))
		_draw_drop_ring(foot, Vector2(52, 18), Color(gold, 0.64 + 0.36 * glow))
	else:
		var code: String = drag_hint.code
		_draw_drop_ring(_drop_target_point(code), _drop_radii(code) * 0.93, Color(gold, 0.52 + 0.48 * glow))

func _draw_drop_ring(center: Vector2, radii: Vector2, color: Color) -> void:
	var points := PackedVector2Array()
	for step in 41:
		var angle := TAU * step / 40.0
		points.append(center + Vector2(cos(angle), sin(angle)) * radii)
	draw_polyline(points, Color(color, color.a * 0.2), 8.0, true)
	draw_polyline(points, color, 2.5, true)

func _process(delta: float) -> void :
	super._process(delta)
	if is_instance_valid(practice_impact_labels):
		practice_impact_labels.queue_redraw()
	if is_instance_valid(practice_film) and practice_film.visible and not encounter.suspended:
		practice_film.tick(event_time)
		if not practice_film.visible:
			_stop_practice_film()
		else:
			var cue: String = practice_film.take_cue(event_time)
			if not cue.is_empty():
				encounter.app.battle_audio.cue(cue, 0.0, 0.62)
	if pointer_id == -99: return
	if encounter.suspended or model.playback_pending or pointer_turn != model.turn:
		pointer_id = -99
		pointer_card = -1
		pointer_dragging = false
		if pointer_peek: _close_popup()
		pointer_peek = false
		drag_hint.clear()
		_finish_drag({})
		return
	if pointer_peek: return
	pointer_time += delta
	if pointer_time >= 0.38 and not pointer_dragging:
		popup_index = pointer_card;pointer_peek = true;_popup_page()
