extends "res://scripts/duel_lab_app.gd"

const Art = preload("res://scripts/card_assets.gd")
const PreviewDuel = preload("res://scripts/summoning_lab_encounter.gd")
const PreviewBattle = preload("res://scripts/summoning_lab_battle.gd")
const PAGE_SIZE := 6
var entries: Array[Dictionary] = []
var selection: Array[Dictionary] = []
var filter := "Alle"
var query := ""
var page := 0
var index := 0

func _ready() -> void:
	save_path = "user://beschwoerungskino.json"
	short_repeat_cinematics = false
	for id in Cards.CARDS.keys() + Cards.collection_data().new_cards.keys() + Cards.Strategy.ids():
		var card := Cards.get_card(id)
		var rarity: String = card.get("collect_rarity", card.get("rarity", "Spezial"))
		if not card.get("rare", false) and rarity not in ["Selten", "Episch", "Mythisch", "Legendär"]:
			continue
		card.preview_rarity = rarity
		entries.append(card)
	entries.sort_custom(func(a, b):
		var a_full := PreviewBattle.FULL_SCENES.has(a.id)
		var b_full := PreviewBattle.FULL_SCENES.has(b.id)
		return a_full if a_full != b_full else a.name.naturalnocasecmp_to(b.name) < 0)
	super._ready()

func show_menu() -> void:
	_close_overlay()
	if is_instance_valid(forest_encounter):
		forest_encounter.suspended = true
		forest_encounter.queue_free()
		forest_encounter = null
	var column := _panel("Itoeva · Beschwörungskino")
	column.custom_minimum_size.x = 950
	var controls := _row(column)
	for label in ["Alle", "Eigene Szenen", "Helfer", "Zauber & Spezial"]:
		var tab := _button(label, _set_filter.bind(label))
		tab.disabled = filter == label
		controls.add_child(tab)
	var search := LineEdit.new()
	search.placeholder_text = "Kartenname oder ID"
	search.text = query
	search.custom_minimum_size = Vector2(210, 44)
	controls.add_child(search)
	controls.add_child(_button("Suchen", func(): _search(search.text)))
	search.text_submitted.connect(_search)
	selection.clear()
	for card in entries:
		if filter == "Eigene Szenen" and card.id not in PreviewBattle.FULL_SCENES:
			continue
		if (filter == "Helfer" and card.kind != "minion") or (filter == "Zauber & Spezial" and card.kind == "minion"):
			continue
		if not query.is_empty() and not (card.name + " " + card.id).to_lower().contains(query.to_lower()):
			continue
		selection.append(card)
	var pages := maxi(1, ceili(selection.size() / float(PAGE_SIZE)))
	page = clampi(page, 0, pages - 1)
	column.add_child(_label("%s Karten · Antippen zum Abspielen" % selection.size(), 16))
	for position in range(page * PAGE_SIZE, mini((page + 1) * PAGE_SIZE, selection.size())):
		var card: Dictionary = selection[position]
		var row := _row(column)
		var art := TextureRect.new()
		art.texture = Art.texture(card)
		art.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
		art.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_CENTERED
		art.custom_minimum_size = Vector2(64, 49)
		row.add_child(art)
		var mode := "Eigene Szene" if card.id in PreviewBattle.FULL_SCENES else "Standard-Einspieler"
		var button := _button("%s  ·  %s  ·  %s" % [card.name, card.preview_rarity, mode], _play_index.bind(position))
		button.custom_minimum_size = Vector2(765, 49)
		row.add_child(button)
	if selection.is_empty():
		column.add_child(_label("Keine Karte passt zur Suche.", 18))
	var navigation := _row(column)
	var previous := _button("←", _turn_page.bind(-1))
	previous.disabled = page == 0
	navigation.add_child(previous)
	navigation.add_child(_label("Seite %s / %s" % [page + 1, pages], 16))
	var next := _button("→", _turn_page.bind(1))
	next.disabled = page >= pages - 1
	navigation.add_child(next)

func _search(value: String) -> void:
	query = value.strip_edges()
	page = 0
	show_menu()

func _set_filter(value: String) -> void:
	filter = value
	page = 0
	show_menu()

func _turn_page(direction: int) -> void:
	page += direction
	show_menu()

func _play_index(position: int) -> void:
	if selection.is_empty():
		return
	_close_overlay()
	if is_instance_valid(forest_encounter):
		forest_encounter.suspended = true
		forest_encounter.queue_free()
	index = posmod(position, selection.size())
	page = index / PAGE_SIZE
	var duel := PreviewDuel.new()
	forest_encounter = duel
	duel.app = self
	duel.subject = selection[index].id
	add_child(duel)
	duel.resume()

func navigate_preview(direction: int) -> void:
	_play_index(index + direction)

func _show_overlay(title: String) -> void:
	super._show_overlay(title)
	for button in menu_buttons:
		if is_instance_valid(button) and button.text == "Duell beenden":
			button.text = "Zur Kartenliste"
