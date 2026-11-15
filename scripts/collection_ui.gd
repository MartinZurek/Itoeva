extends RefCounted
const Collection = preload("res://scripts/card_collection.gd")
const Cards = preload("res://scripts/card_catalog.gd")
const Face = preload("res://scripts/collectible_face.gd")
const Detail = preload("res://scripts/card_detail.gd")
var app: Node3D
var species: = ""
var page: = 0
var filter: = "Edition"
var album_id := ""
var message: = ""
var reveal: Array = []
var reveal_index: = 0
var shop_edition := Collection.DEFAULT_EDITION
var reveal_edition := ""
var return_to: Callable
func open(resident: String = "", at_shop: bool = false) -> void :
	species = app.selected
	app._pause(true)
	Collection.state(app.save_data, species)
	app._write_save()
	if at_shop: shop()
	elif resident.is_empty(): album()
	else: trades(resident)
func _header(title: String) -> VBoxContainer:
	var s: = Collection.state(app.save_data, species)
	var column: VBoxContainer = app._panel(title);column.custom_minimum_size.x = 1020;column.add_theme_constant_override("separation", 8)
	column.add_child(app._label("%s Bernsteinmarken · %s versiegelte Booster · Sammlung für %s" % [int(s.coins), s.sealed.size(), app.Catalog.data().characters[species].label], 16))
	var tabs: = HBoxContainer.new();tabs.alignment = BoxContainer.ALIGNMENT_CENTER;column.add_child(tabs)
	for item in [["Album", album], ["Kartenladen", shop], ["Fundhinweise", finds], ["Bewohnertausch", trades], ["Zurück", _close]]:
		var b: Button = app._button(item[0], item[1]);b.custom_minimum_size = Vector2(170, 44);tabs.add_child(b)
	if not message.is_empty(): column.add_child(app._label(message, 15))
	return column
func album() -> void :
	if not Collection.album_owned(app.save_data, species, album_id):
		album_id = ""
	var catalog := Collection.data()
	var column := _header("Sammelalbum")
	var albums := HBoxContainer.new()
	albums.alignment = BoxContainer.ALIGNMENT_CENTER
	column.add_child(albums)
	_album_button(albums, catalog.set_name, "")
	for id in catalog.albums:
		if Collection.album_owned(app.save_data, species, id):
			_album_button(albums, catalog.albums[id].name, id)
	var controls: = HBoxContainer.new();controls.alignment = BoxContainer.ALIGNMENT_CENTER;column.add_child(controls)
	for kind in ["Edition", "Im Besitz", "Alle Karten"]:
		var label: String = "Albumseiten" if kind == "Edition" else kind
		var b: Button = app._button(label, func(): filter = kind;page = 0;album());b.custom_minimum_size = Vector2(165, 44);b.disabled = filter == kind;controls.add_child(b)
	controls.add_child(app._button("Deck bauen", _build_deck))
	var selection := Collection.album_cards(species, album_id)
	var ids: Array = []
	for id in (selection if filter == "Edition" else Cards.available(species)):
		if filter != "Im Besitz" or Collection.owned(app.save_data, species, id) > 0: ids.append(id)
	if filter == "Edition" and album_id.is_empty(): ids.sort_custom(func(a, b): return int(catalog.cards[a].number) < int(catalog.cards[b].number))
	elif filter != "Edition": ids.sort()
	var pages: = maxi(1, ceili(ids.size() / 6.0));page = clampi(page, 0, pages - 1)
	var strip: = HBoxContainer.new();strip.alignment = BoxContainer.ALIGNMENT_CENTER;strip.add_theme_constant_override("separation", 10);column.add_child(strip)
	for i in range(page * 6, mini(page * 6 + 6, ids.size())):
		var id: String = ids[i]; var b: Button = app._button("", _inspect.bind(id));b.custom_minimum_size = Vector2(158, 248);strip.add_child(b)
		var face: = Face.new();face.card = Cards.get_card(id);face.owned_count = Collection.owned(app.save_data, species, id);b.add_child(face);face.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
		if face.owned_count == 0: face.modulate = Color(0.62, 0.68, 0.68)
	var nav: = HBoxContainer.new();nav.alignment = BoxContainer.ALIGNMENT_CENTER;column.add_child(nav)
	var prev: Button = app._button("←", func(): page -= 1;album());prev.disabled = page == 0;nav.add_child(prev)
	var count := "%s Karten" % ids.size()
	if filter == "Edition":
		var have := selection.filter(func(id: String): return Collection.owned(app.save_data, species, id) > 0).size()
		count = "%s/%s Motive gesammelt" % [have, selection.size()]
	nav.add_child(app._label("%s · Seite %s/%s" % [count, page + 1, pages], 17))
	var next: Button = app._button("→", func(): page += 1;album());next.disabled = page == pages - 1;nav.add_child(next)

func _album_button(row: HBoxContainer, label: String, id: String) -> void:
	var b: Button = app._button(label, _select_album.bind(id))
	b.custom_minimum_size = Vector2(200, 44)
	b.disabled = filter == "Edition" and album_id == id
	row.add_child(b)

func _select_album(id: String) -> void:
	album_id = id
	page = 0
	filter = "Edition"
	album()

func _build_deck() -> void:
	app._new_deck_editor(album)
	if not album_id.is_empty() and filter == "Edition":
		var series: Dictionary = Collection.data().booster_series
		for id in series:
			if series[id].get("album", "") == album_id:
				app.deck_editor.filter_edition = id
				break
	app.deck_editor.open()

func _inspect(id: String) -> void :
	var column: = _header(Cards.get_card(id).name)
	var detail: = Detail.new();detail.app = app;detail.card = Cards.get_card(id);detail.custom_minimum_size = Vector2(600, 309);detail.close_callback = album;column.add_child(detail)
	column.add_child(app._label("Im Besitz: %s · Im Deck erlaubt: %s" % [Collection.owned(app.save_data, species, id), Collection.usable(app.save_data, species, id)], 16))
func shop() -> void :
	var series: Dictionary = Collection.data().booster_series
	var edition: Dictionary = series[shop_edition]
	var column := _header("Kartenladen · " + edition.name)
	_shop_tabs(column, false)
	var editions := HBoxContainer.new()
	editions.alignment = BoxContainer.ALIGNMENT_CENTER
	column.add_child(editions)
	for id in series:
		var tab: Button = app._button(series[id].name, _select_shop_edition.bind(id))
		tab.custom_minimum_size = Vector2(230, 44)
		tab.disabled = id == shop_edition
		editions.add_child(tab)
	var row: = HBoxContainer.new();row.alignment = BoxContainer.ALIGNMENT_CENTER;row.add_theme_constant_override("separation", 24);column.add_child(row)
	var state := Collection.state(app.save_data, species)
	for display in [false, true]:
		var box: = VBoxContainer.new();box.custom_minimum_size.x = 420;row.add_child(box)
		var art = load("res://scripts/booster_product.gd").new()
		art.display = display
		art.edition = edition
		art.custom_minimum_size = Vector2(224, 225)
		var center := CenterContainer.new()
		center.add_child(art)
		box.add_child(center)
		var packs: int = Collection.DISPLAY_PACKS if display else 1
		var price: int = Collection.DISPLAY_PRICE if display else Collection.PACK_PRICE
		var product: String = "%s-Display · %s Booster" % [edition.short, packs] if display else edition.short + "-Booster"
		box.add_child(app._label(product, 20))
		box.add_child(app._label("%s Karten · %s Bernsteinmarken" % [packs * 5, price], 17))
		var buy: Button = app._button("Display kaufen" if display else "Booster kaufen", _buy.bind(display))
		buy.disabled = int(state.coins) < price or state.sealed.size() + packs > Collection.MAX_SEALED
		box.add_child(buy)
	column.add_child(app._label("Je Booster: 3 gewöhnliche, 1 ungewöhnliche und 1 seltene Karte. Der seltene Platz ist zu 1/12 legendär (8,33 %).", 14))
	var buttons: = HBoxContainer.new();buttons.alignment = BoxContainer.ALIGNMENT_CENTER;column.add_child(buttons)
	var sealed := Collection.sealed_packs(state, shop_edition)
	if not sealed.is_empty(): buttons.add_child(app._button("Booster öffnen (%s)" % sealed.size(), _open.bind(int(sealed[0].token))))
	if not state.last_opening.is_empty():
		buttons.add_child(app._button("Letzte Öffnung ansehen", func():
			reveal = state.last_opening.duplicate(true)
			reveal_index = reveal.size()
			reveal_edition = state.get("last_opening_edition", Collection.DEFAULT_EDITION)
			_reveal()))

func _select_shop_edition(id: String) -> void:
	shop_edition = id
	message = ""
	shop()

func _shop_tabs(column: VBoxContainer, albums_active: bool) -> void:
	var tabs := HBoxContainer.new()
	tabs.alignment = BoxContainer.ALIGNMENT_CENTER
	column.add_child(tabs)
	var boosters: Button = app._button("Booster & Displays", shop)
	var albums: Button = app._button("Alben im Laden", album_shop)
	for tab in [boosters, albums]:
		tab.custom_minimum_size = Vector2(210, 44)
		tabs.add_child(tab)
	boosters.disabled = not albums_active
	albums.disabled = albums_active

func album_shop() -> void:
	var column := _header("Kartenladen · Sammelalben")
	_shop_tabs(column, true)
	var row := HBoxContainer.new()
	row.alignment = BoxContainer.ALIGNMENT_CENTER
	row.add_theme_constant_override("separation", 22)
	column.add_child(row)
	var state := Collection.state(app.save_data, species)
	var catalog := Collection.data()
	for id in catalog.albums:
		var product: Dictionary = catalog.albums[id]
		var panel := PanelContainer.new()
		panel.custom_minimum_size.x = 440
		var frame := StyleBoxFlat.new()
		frame.bg_color = Color("142a27")
		frame.border_color = Color(product.ink).darkened(0.55)
		frame.set_border_width_all(1)
		frame.set_corner_radius_all(12)
		frame.content_margin_left = 16
		frame.content_margin_right = 16
		frame.content_margin_top = 16
		frame.content_margin_bottom = 16
		panel.add_theme_stylebox_override("panel", frame)
		row.add_child(panel)
		var box := VBoxContainer.new()
		box.add_theme_constant_override("separation", 8)
		panel.add_child(box)
		var cover := TextureRect.new()
		cover.texture = load(product.cover)
		cover.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
		cover.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_COVERED
		cover.custom_minimum_size.y = 152
		box.add_child(cover)
		var title: Label = app._label(product.name, 24)
		title.add_theme_color_override("font_color", Color(product.ink))
		box.add_child(title)
		var description: Label = app._label(product.description, 15)
		description.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
		box.add_child(description)
		var count := Collection.album_cards(species, id).size()
		var acquired := Collection.album_owned(app.save_data, species, id)
		var status := "In deiner Sammlung" if acquired else "%s Bernsteinmarken" % int(product.price)
		box.add_child(app._label("%s Motive · %s" % [count, status], 17))
		var action: Button
		if acquired:
			action = app._button("Album ansehen", _select_album.bind(id))
		else:
			action = app._button("Album kaufen", _buy_album.bind(id))
			action.disabled = int(state.coins) < int(product.price)
		box.add_child(action)
	column.add_child(app._label("Alben ordnen deine Karten nach Motiven. Bereits gesammelte Karten erscheinen direkt auf den Seiten.", 15))

func _buy_album(id: String) -> void:
	var result := _transaction(func(): return Collection.purchase_album(app.save_data, species, id))
	message = result.message
	album_shop()

func _transaction(callback: Callable) -> Dictionary:
	if app.selected != species: return {"ok": false, "message": "Das ausgewählte Wesen hat gewechselt. Öffne seine Sammlung erneut."}
	var before: Dictionary = app.save_data.duplicate(true)
	var result: Dictionary = callback.call()
	if result.get("ok", false) and not app._write_save():
		app.save_data = before;return {"ok": false, "message": "Speichern fehlgeschlagen. Versuch es noch einmal."}
	return result
func _buy(display: bool) -> void :
	var result := _transaction(func(): return Collection.purchase(app.save_data, species, display, shop_edition))
	message = result.message
	shop()
func _open(token: int) -> void :
	var result: = _transaction( func(): return Collection.open_pack(app.save_data, species, token));message = result.message
	if not result.ok: shop();return
	reveal_edition = result.edition
	reveal = result.cards;reveal_index = 0;_reveal()
func _reveal() -> void :
	var edition: Dictionary = Collection.data().booster_series.get(reveal_edition, {})
	var column := _header(edition.get("name", "Kartenfund"))
	column.add_child(app._label("Deine Karten sind bereits sicher im Album. Du kannst jederzeit schließen.", 15))
	var row: = HBoxContainer.new();row.alignment = BoxContainer.ALIGNMENT_CENTER;row.add_theme_constant_override("separation", 12);column.add_child(row)
	for i in reveal.size():
		var frame: = PanelContainer.new();frame.custom_minimum_size = Vector2(182, 282);row.add_child(frame)
		if i < reveal_index:
			var face: = Face.new();face.card = Cards.get_card(reveal[i].id);face.owned_count = Collection.owned(app.save_data, species, reveal[i].id);face.fresh = bool(reveal[i].new);frame.add_child(face)
		else:
			var back: = VBoxContainer.new();back.alignment = BoxContainer.ALIGNMENT_CENTER;frame.add_child(back)
			var mark: Label = app._label("✧", 46)
			mark.add_theme_color_override("font_color", Color(edition.get("ink", "#dbc68b")))
			back.add_child(mark)
			back.add_child(app._label("ITOEVA", 21))
			back.add_child(app._label(edition.get("short", ""), 15))
	var controls: = HBoxContainer.new();controls.alignment = BoxContainer.ALIGNMENT_CENTER;column.add_child(controls)
	if reveal_index < reveal.size():
		controls.add_child(app._button("Nächste Karte", func(): reveal_index += 1;_reveal()))
		controls.add_child(app._button("Alle aufdecken", func(): reveal_index = reveal.size();_reveal()))
	controls.add_child(app._button("Zum Album", _album_after_reveal))
	controls.add_child(app._button("Zum Kartenladen", shop))

func _album_after_reveal() -> void:
	if not reveal_edition.is_empty():
		var edition: Dictionary = Collection.data().booster_series.get(reveal_edition, {})
		if not edition.is_empty() and Collection.album_owned(app.save_data, species, edition.get("album", "")):
			_select_album(edition.get("album", ""))
			return
		filter = "Im Besitz"
		page = 0
	album()
func finds() -> void :
	var column: = _header("Spuren für deine Sammlung")
	for find in Collection.data().finds:
		var done: bool = Collection.state(app.save_data, species).claims.get("find:" + find.id, false)
		var label: Label = app._label(("✓ " if done else "◇ ") + find.hint + (" · gefunden" if done else ""), 17);label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART;label.custom_minimum_size.x = 920;column.add_child(label)
	column.add_child(app._label("Karten werden am Fundort geborgen. Rätsel belohnen die richtige Beobachtung, nicht das Öffnen dieses Albums.", 14))
func trades(resident: String = "") -> void :
	var column: = _header("Tausch mit den Bewohnern")
	for offer in Collection.data().trades:
		if not resident.is_empty() and offer.resident != resident: continue
		var preview := Collection.trade_preview(app.save_data, species, offer.id)
		var row := HBoxContainer.new()
		row.add_theme_constant_override("separation", 18)
		column.add_child(row)
		var summary := VBoxContainer.new()
		summary.size_flags_horizontal = Control.SIZE_EXPAND_FILL
		row.add_child(summary)
		summary.add_child(app._label(offer.label + " · " + Cards.get_card(offer.give).name + " → " + Cards.get_card(offer.receive).name, 18))
		var status := "%s freie Kopie" % preview.free if preview.free == 1 else "%s freie Kopien" % preview.free
		if not preview.ok: status = preview.message
		var detail: Label = app._label(status, 15)
		detail.add_theme_color_override("font_color", Color("b6d7c3") if preview.ok else Color("e3c48f"))
		summary.add_child(detail)
		var view_offer: Button = app._button("Angebot ansehen", _preview_trade.bind(offer.id, resident))
		view_offer.custom_minimum_size = Vector2(192, 44)
		row.add_child(view_offer)

func _preview_trade(id: String, resident: String) -> void:
	var preview := Collection.trade_preview(app.save_data, species, id)
	if not preview.has("offer"):
		message = preview.message
		trades(resident)
		return
	var offer: Dictionary = preview.offer
	var column := _header(offer.label)
	var cards := HBoxContainer.new()
	cards.alignment = BoxContainer.ALIGNMENT_CENTER
	cards.add_theme_constant_override("separation", 22)
	column.add_child(cards)
	_trade_card(cards, "Du gibst · 1 Kopie", offer.give, preview.have,
		"%s frei · %s geschützt" % [preview.free, preview.reserved], Color("e4c287"))
	var arrow: Label = app._label("→", 38)
	arrow.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
	cards.add_child(arrow)
	var receiving := "1 Kopie erhalten" if preview.done else "1 Kopie im Angebot"
	_trade_card(cards, "Du erhältst · 1 Kopie", offer.receive, preview.receive_have, receiving, Color("b9e0c6"))
	if not preview.message.is_empty() and preview.message != message:
		var reason: Label = app._label(preview.message, 17)
		reason.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
		column.add_child(reason)
	var actions := HBoxContainer.new()
	actions.alignment = BoxContainer.ALIGNMENT_CENTER
	column.add_child(actions)
	actions.add_child(app._button("Zurück zu den Angeboten", trades.bind(resident)))
	var confirm: Button = app._button("Tausch bestätigen", _trade.bind(id, resident))
	confirm.disabled = not preview.ok
	actions.add_child(confirm)

func _trade_card(row: HBoxContainer, title: String, id: String, owned: int, count: String, ink: Color) -> void:
	var box := VBoxContainer.new()
	box.alignment = BoxContainer.ALIGNMENT_CENTER
	row.add_child(box)
	var label: Label = app._label(title, 19)
	label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	label.add_theme_color_override("font_color", ink)
	box.add_child(label)
	var face := Face.new()
	face.card = Cards.get_card(id)
	face.owned_count = owned
	face.custom_minimum_size = Vector2(192, 286)
	box.add_child(face)
	var copies: Label = app._label(count, 16)
	copies.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	box.add_child(copies)

func _trade(id: String, resident: String) -> void:
	var result := _transaction(func(): return Collection.trade(app.save_data, species, id))
	message = result.message
	_preview_trade(id, resident)
func show_find(find: Dictionary) -> void :
	app._pause(true)
	var column: = _header(find.name)
	var mill_find: bool = find.id == app.CardFinds.Mill.FIND
	var mill_solved: bool = mill_find and app.CardFinds.offer_for(app,find.id).get("can_claim",false)
	var description: String = find.get("question","Zwischen den Blättern liegt eine Karte: "+Cards.get_card(find.card).name)
	if mill_solved:description="Das Rad läuft wieder. Auf der letzten Seite steckt der Schwur am Mühlenbach."
	var text: Label=app._label(description,22);text.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART;text.custom_minimum_size=Vector2(880,60);column.add_child(text)
	if mill_find:
		if mill_solved:
			column.add_child(app._button("Karte bergen",_find_answer.bind(find,int(find.answer))))
		else:
			column.add_child(app._button("Zum Ufer zurück",app._resume))
	elif find.has("answers"):
		for i in find.answers.size(): column.add_child(app._button(find.answers[i], _find_answer.bind(find, i)))
	else: column.add_child(app._button("Karte bergen", _find_answer.bind(find, -1)))
func _find_answer(find: Dictionary, answer: int) -> void :
	# Auch ein alter Dialog darf nach Orts-/Profilwechsel nichts aus der Ferne vergeben.
	var current: Dictionary = app.CardFinds.offer_for(app, find.id)
	if current.is_empty() or not current.ready: message = "Kehre zum Fundort zurück.";finds();return
	if not current.get("can_claim",true):message="Der Zulauf muss das Rad erreichen.";show_find(find);return
	var result: = _transaction( func(): return Collection.claim_find(app.save_data, species, find.id, answer));message = result.message
	if result.ok:
		reveal_edition = ""
		reveal = result.cards;reveal_index = reveal.size();_reveal()
	else: show_find(find)
func _close() -> void :
	if return_to.is_valid(): return_to.call()
	else: app.show_menu()
