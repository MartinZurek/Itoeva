extends RefCounted
const Collection = preload("res://scripts/card_collection.gd")
const Cards = preload("res://scripts/card_catalog.gd")
const Face = preload("res://scripts/collectible_face.gd")
const Detail = preload("res://scripts/card_detail.gd")
var app: Node3D
var species: = ""
var page: = 0
var filter: = "Edition"
var message: = ""
var reveal: Array = []
var reveal_index: = 0
var return_to: Callable
func open() -> void :
	species = app.selected
	app._pause(true);Collection.state(app.save_data, species);app._write_save();album()
func _header(title: String) -> VBoxContainer:
	var s: = Collection.state(app.save_data, species)
	var column: VBoxContainer = app._panel(title);column.custom_minimum_size.x = 1020;column.add_theme_constant_override("separation", 8)
	column.add_child(app._label("%s Wegmünzen · %s versiegelte Booster · Sammlung für %s" % [int(s.coins), s.sealed.size(), app.Catalog.data().characters[species].label], 16))
	var tabs: = HBoxContainer.new();tabs.alignment = BoxContainer.ALIGNMENT_CENTER;column.add_child(tabs)
	for item in [["Album", album], ["Kartenladen", shop], ["Fundhinweise", finds], ["Bewohnertausch", trades], ["Zurück", _close]]:
		var b: Button = app._button(item[0], item[1]);b.custom_minimum_size = Vector2(170, 44);tabs.add_child(b)
	if not message.is_empty(): column.add_child(app._label(message, 15))
	return column
func album() -> void :
	var column: = _header("Wege von Itoeva · Sammelalbum")
	var controls: = HBoxContainer.new();controls.alignment = BoxContainer.ALIGNMENT_CENTER;column.add_child(controls)
	for kind in ["Edition", "Im Besitz", "Alle Karten"]:
		var b: Button = app._button(kind, func(): filter = kind;page = 0;album());b.custom_minimum_size = Vector2(165, 36);b.disabled = filter == kind;controls.add_child(b)
	var ids: Array = []
	var legal: = Cards.available(species)
	for id in (Collection.data().cards.keys() if filter == "Edition" else legal):
		if id in legal and (filter != "Im Besitz" or Collection.owned(app.save_data, species, id) > 0): ids.append(id)
	if filter == "Edition": ids.sort_custom( func(a, b): return int(Collection.data().cards[a].number) < int(Collection.data().cards[b].number))
	else: ids.sort()
	var pages: = maxi(1, ceili(ids.size() / 6.0));page = clampi(page, 0, pages - 1)
	var strip: = HBoxContainer.new();strip.alignment = BoxContainer.ALIGNMENT_CENTER;strip.add_theme_constant_override("separation", 10);column.add_child(strip)
	for i in range(page * 6, mini(page * 6 + 6, ids.size())):
		var id: String = ids[i]; var b: Button = app._button("", _inspect.bind(id));b.custom_minimum_size = Vector2(158, 248);strip.add_child(b)
		var face: = Face.new();face.card = Cards.get_card(id);face.owned_count = Collection.owned(app.save_data, species, id);b.add_child(face);face.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
		if face.owned_count == 0: face.modulate = Color(0.62, 0.68, 0.68)
	var nav: = HBoxContainer.new();nav.alignment = BoxContainer.ALIGNMENT_CENTER;column.add_child(nav)
	var prev: Button = app._button("←", func(): page -= 1;album());prev.disabled = page == 0;nav.add_child(prev)
	var have: = 0
	for id in Collection.data().cards:
		if Collection.owned(app.save_data, species, id) > 0: have += 1
	nav.add_child(app._label("Edition %s/24 · Seite %s/%s" % [have, page + 1, pages], 17))
	var next: Button = app._button("→", func(): page += 1;album());next.disabled = page == pages - 1;nav.add_child(next)
	var desc: Label = app._label("Fehlende Motive bleiben sichtbar. Besitz und Duplikate zählen wirklich; dein bestehendes Deck bleibt erhalten.", 14);column.add_child(desc)
func _inspect(id: String) -> void :
	var column: = _header(Cards.get_card(id).name)
	var detail: = Detail.new();detail.app = app;detail.card = Cards.get_card(id);detail.custom_minimum_size = Vector2(600, 309);detail.close_callback = album;column.add_child(detail)
	column.add_child(app._label("Im Besitz: %s · Im Deck erlaubt: %s" % [Collection.owned(app.save_data, species, id), Collection.usable(app.save_data, species, id)], 16))
func shop() -> void :
	var column: = _header("Kartenladen · Wege von Itoeva")
	var row: = HBoxContainer.new();row.alignment = BoxContainer.ALIGNMENT_CENTER;row.add_theme_constant_override("separation", 24);column.add_child(row)
	for display in [false, true]:
		var box: = VBoxContainer.new();box.custom_minimum_size.x = 420;row.add_child(box)
		var art = load("res://scripts/booster_product.gd").new();art.display = display;art.custom_minimum_size = Vector2(224, 232); var center: = CenterContainer.new();center.add_child(art);box.add_child(center)
		box.add_child(app._label("DISPLAY · 6 BOOSTER" if display else "WALDPFAD-BOOSTER", 20))
		box.add_child(app._label("30 Karten · 60 Wegmünzen" if display else "5 Karten · 12 Wegmünzen", 17))
		var buy: Button = app._button("Display kaufen" if display else "Booster kaufen", _buy.bind(display));buy.disabled = int(Collection.state(app.save_data, species).coins) < (60 if display else 12);box.add_child(buy)
	column.add_child(app._label("Je Booster: 3 gewöhnliche, 1 ungewöhnliche und 1 seltene Karte. Der seltene Platz ist zu 1/12 legendär (8,33 %).", 14))
	column.add_child(app._label("Nur erspielte Wegmünzen. Funde und Siege bringen Münzen. Ein Startbooster liegt bereits für dich bereit.", 14))
	var state: = Collection.state(app.save_data, species)
	var buttons: = HBoxContainer.new();buttons.alignment = BoxContainer.ALIGNMENT_CENTER;column.add_child(buttons)
	if not state.sealed.is_empty(): buttons.add_child(app._button("Booster öffnen (%s)" % state.sealed.size(), _open.bind(int(state.sealed[0].token))))
	if not state.last_opening.is_empty(): buttons.add_child(app._button("Letzten Fund ansehen", func(): reveal = state.last_opening.duplicate(true);reveal_index = reveal.size();_reveal()))
func _transaction(callback: Callable) -> Dictionary:
	if app.selected != species: return {"ok": false, "message": "Das ausgewählte Wesen hat gewechselt. Öffne seine Sammlung erneut."}
	var before: Dictionary = app.save_data.duplicate(true)
	var result: Dictionary = callback.call()
	if result.get("ok", false) and not app._write_save():
		app.save_data = before;return {"ok": false, "message": "Der Spielstand konnte nicht gespeichert werden. Deine Karten und Münzen bleiben unverändert."}
	return result
func _buy(display: bool) -> void :
	var result: = _transaction( func(): return Collection.purchase(app.save_data, species, display));message = result.message;shop()
func _open(token: int) -> void :
	var result: = _transaction( func(): return Collection.open_pack(app.save_data, species, token));message = result.message
	if not result.ok: shop();return
	reveal = result.cards;reveal_index = 0;_reveal()
func _reveal() -> void :
	var column: = _header("Ein neuer Weg öffnet sich")
	column.add_child(app._label("Deine Karten sind bereits sicher im Album. Du kannst jederzeit schließen.", 15))
	var row: = HBoxContainer.new();row.alignment = BoxContainer.ALIGNMENT_CENTER;row.add_theme_constant_override("separation", 12);column.add_child(row)
	for i in reveal.size():
		var frame: = PanelContainer.new();frame.custom_minimum_size = Vector2(182, 282);row.add_child(frame)
		if i < reveal_index:
			var face: = Face.new();face.card = Cards.get_card(reveal[i].id);face.owned_count = Collection.owned(app.save_data, species, reveal[i].id);face.fresh = bool(reveal[i].new);frame.add_child(face)
		else:
			var back: = VBoxContainer.new();back.alignment = BoxContainer.ALIGNMENT_CENTER;frame.add_child(back)
			back.add_child(app._label("✧", 46));back.add_child(app._label("ITOEVA", 21));back.add_child(app._label("WEGE", 15))
	var controls: = HBoxContainer.new();controls.alignment = BoxContainer.ALIGNMENT_CENTER;column.add_child(controls)
	if reveal_index < reveal.size():
		controls.add_child(app._button("Nächste Karte", func(): reveal_index += 1;_reveal()))
		controls.add_child(app._button("Alle aufdecken", func(): reveal_index = reveal.size();_reveal()))
	controls.add_child(app._button("Zum Album", album));controls.add_child(app._button("Zum Kartenladen", shop))
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
		var have: = Collection.owned(app.save_data, species, offer.give)
		column.add_child(app._label(offer.label + " · 1× " + Cards.get_card(offer.give).name + " → 1× " + Cards.get_card(offer.receive).name, 19))
		var done: bool = Collection.state(app.save_data, species).claims.get("trade:" + offer.id, false)
		var b: Button = app._button("Abgeschlossen" if done else "Duplikat tauschen (Besitz: %s)" % have, _trade.bind(offer.id));b.disabled = done;column.add_child(b)
	column.add_child(app._label("Jeder Bewohner bietet diesen Tausch einmal an. Karten in deinem gespeicherten Deck und deine letzte Kopie sind geschützt.", 14))
func _trade(id: String) -> void :
	var result: = _transaction( func(): return Collection.trade(app.save_data, species, id));message = result.message;trades()
func show_find(find: Dictionary) -> void :
	app._pause(true)
	var column: = _header(find.name)
	var text: Label = app._label(find.get("question", "Zwischen den Blättern liegt eine Karte: " + Cards.get_card(find.card).name), 22);text.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART;text.custom_minimum_size = Vector2(880, 60);column.add_child(text)
	if find.has("answers"):
		for i in find.answers.size(): column.add_child(app._button(find.answers[i], _find_answer.bind(find, i)))
	else: column.add_child(app._button("Karte bergen", _find_answer.bind(find, -1)))
func _find_answer(find: Dictionary, answer: int) -> void :

	var current: Dictionary = app.CardFinds.offer_for(app, find.id)
	if current.is_empty() or not current.ready: message = "Kehre zum Fundort zurück.";finds();return
	var result: = _transaction( func(): return Collection.claim_find(app.save_data, species, find.id, answer));message = result.message
	if result.ok: reveal = result.cards;reveal_index = reveal.size();_reveal()
	else: show_find(find)
func _close() -> void :
	if return_to.is_valid(): return_to.call()
	else: app.show_menu()
