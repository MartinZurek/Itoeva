extends SceneTree

const Collection = preload("res://scripts/card_collection.gd")
const Catalog = preload("res://scripts/world_catalog.gd")
const Cards = preload("res://scripts/card_catalog.gd")

var app: Node3D
var checks := 0
var failures := 0

func _initialize() -> void:
	run.call_deferred()

func check(ok: bool, label: String) -> void:
	checks += 1
	if not ok:
		failures += 1
		push_error(label)

func button(label: String, index: int = 0) -> Button:
	for candidate in app.menu_buttons:
		if candidate.text != label:
			continue
		if index == 0:
			return candidate
		index -= 1
	return null

func press(label: String, index: int = 0) -> void:
	var action := button(label, index)
	check(is_instance_valid(action) and not action.disabled, "Aktion verfügbar · " + label)
	if is_instance_valid(action) and not action.disabled:
		action.pressed.emit()

func check_layout() -> void:
	await process_frame
	var bounds: Rect2 = app.ui.get_viewport_rect()
	for action in app.menu_buttons:
		check(bounds.grow(1).encloses(action.get_global_rect()), "Im Querformat sichtbar · " + action.text)

func run() -> void:
	app = load("res://scenes/full_world.tscn").instantiate()
	app.save_path = "user://card-shop-play-%s-%s.json" % [OS.get_process_id(), int(Time.get_unix_time_from_system() * 1000000)]
	root.add_child(app)
	await process_frame
	app.selected = "mosslyn"
	var s := Collection.state(app.save_data, app.selected)
	s.erase("albums")
	s.coins = 95
	s.sealed[0].erase("edition")
	var original_pack: Dictionary = s.sealed[0].duplicate(true)
	var original_cards: Dictionary = s.owned.duplicate(true)
	check(Collection.album_owned(app.save_data, app.selected, ""), "Das Grundalbum gehört auch alten Spielständen")
	await app._mount_world("SHOP", Catalog.position_for("SHOP", Vector2(0.5, 0.55)))
	app._resume()
	for target in app.interaction_targets():
		if target.kind == "card_shop":
			app.actor.position = target.approach
			break
	for frame in 10:
		await physics_frame
		await process_frame
	await app.interact_with("card_shop", "shop-counter")
	check(app.paused, "Der Tresen öffnet den Kartenladen")
	await check_layout()
	press("Display kaufen")
	press("Booster kaufen")
	check(int(s.coins) == 23 and s.sealed.size() == 8 and s.sealed[0] == original_pack,
		"Display und Booster belasten den alten Kontostand und erhalten versiegelte Karten")
	press("Alben im Laden")
	await check_layout()
	var other_album := button("Album kaufen", 1)
	check(is_instance_valid(other_album) and other_album.disabled, "Zu teures Album ist im Shop gesperrt")
	press("Album kaufen")
	check(int(s.coins) == 5 and s.owned == original_cards, "Album kostet 18 Marken und verändert den Kartenbesitz nicht")
	var before := s.duplicate(true)
	check(not Collection.purchase_album(app.save_data, app.selected, "wasserwege").ok and s == before,
		"Ein erneuter Kauf kostet nichts")
	check(not Collection.purchase_album(app.save_data, app.selected, "helferatlas").ok and s == before,
		"Zu wenig Marken verändern den Spielstand nicht")
	press("Album ansehen")
	await check_layout()
	check(Collection.album_cards("mosslyn", "wasserwege").slice(0, 3) == ["brook_oath", "weir_gauge", "shore_roots"],
		"Das Wasserwege-Album enthält die zutreffende Figurenkarte und beide Uferfunde")
	check(Collection.album_cards("starlet", "wasserwege").has("weir_lantern"), "Jede Figur hat ihre passende Albumseite")
	press("Deck bauen")
	press("Abbrechen")
	check(app.collection_ui.album_id == "wasserwege" and is_instance_valid(button("Wasserwege")), "Vom Deckbau zurück zum gewählten Album")
	press("Kartenladen")
	press("Booster öffnen (8)")
	check(s.sealed.size() == 7 and s.last_opening.size() == 5, "Booster wird genau einmal mit fünf Karten geöffnet")
	check(not Collection.open_pack(app.save_data, app.selected, original_pack.token).ok, "Derselbe Booster kann nicht erneut geöffnet werden")
	var persisted: Dictionary = JSON.parse_string(FileAccess.get_file_as_string(app.save_path))
	check(Collection.album_owned(persisted, "mosslyn", "wasserwege"), "Albumkauf bleibt nach Neuladen erhalten")
	check(not Collection.album_owned(persisted, "starlet", "wasserwege"), "Albumkäufe gelten für das jeweilige Profil")
	check(int(Collection.state(persisted, "mosslyn").coins) == 5, "Markenstand bleibt gespeichert")
	var stored_cards: Dictionary = Collection.state(persisted, "mosslyn").owned
	check(stored_cards.size() == s.owned.size() and s.owned.keys().all(
		func(id: String): return int(stored_cards.get(id, 0)) == int(s.owned[id])
	), "Boosterinhalt bleibt gespeichert")
	var saved_path: String = app.save_path
	app.save_path = saved_path.path_join("unreachable")
	s.coins = 40
	press("Kartenladen")
	press("Alben im Laden")
	press("Album kaufen")
	check(int(Collection.state(app.save_data, "mosslyn").coins) == 40, "Speicherfehler erstattet Marken")
	check(not Collection.album_owned(app.save_data, "mosslyn", "helferatlas"), "Speicherfehler vergibt kein Album")
	app.save_path = saved_path
	press("Album kaufen")
	check(Collection.album_owned(app.save_data, "mosslyn", "helferatlas") and Collection.album_owned(app.save_data, "mosslyn", "wasserwege"),
		"Nach einem Speicherversuch können beide Alben gemeinsam gekauft und gehalten werden")
	await test_ringwehr_edition()
	press("Zurück")
	check(not app.paused and app.room == "SHOP", "Laden schließt zurück zum Tresen")
	DirAccess.remove_absolute(saved_path)
	print("CARD_SHOP_PLAY ", checks - failures, "/", checks)
	quit(0 if failures == 0 else 1)

func test_ringwehr_edition() -> void:
	var s := Collection.state(app.save_data, "mosslyn")
	press("Kartenladen")
	press("Wasser am Ringwehr")
	await check_layout()
	check(not is_instance_valid(button("Booster öffnen (7)")), "Versiegelte Waldpfad-Booster bleiben im eigenen Regal")
	press("Booster kaufen")
	check(int(s.coins) == 4, "Ringwehr-Booster kostet 12 Bernsteinmarken")
	check(Collection.sealed_packs(s, "WEG").size() == 7 and Collection.sealed_packs(s, "RWG").size() == 1,
		"Die beiden Editionen haben getrennte Boosterbestände")
	var pack: Dictionary = Collection.sealed_packs(s, "RWG")[0]
	var pools: Dictionary = Collection.data().booster_series.RWG.pools
	var ids: Array = pack.cards
	check(ids.slice(0, 3).all(func(id: String): return id in pools.common) and ids[3] in pools.uncommon
		and ids[4] in pools.rare + pools.legendary, "Jeder Platz zieht aus dem richtigen Ringwehr-Pool")
	check(ids.all(func(id: String): return Cards.get_card(id).collect_edition == "RWG"), "Alle Ringwehr-Karten tragen ihre Editionskennung")
	check(ids.all(func(id: String): return id in Collection.album_cards("mosslyn", "wasserwege")),
		"Alle Ringwehr-Motive sind im Wasserwege-Album")
	var saved: Dictionary = JSON.parse_string(FileAccess.get_file_as_string(app.save_path))
	check(Collection.sealed_packs(Collection.state(saved, "mosslyn"), "RWG")[0].cards == ids, "Karteninhalt und Edition stehen im Spielstand")
	press("Booster öffnen (1)")
	press("Alle aufdecken")
	check(app.collection_ui.reveal_edition == "RWG" and s.last_opening_edition == "RWG", "Öffnung behält ihre Edition")
	check(Collection.sealed_packs(s, "RWG").is_empty(), "Der geöffnete Booster wird verbraucht")
	press("Zum Album")
	check(app.collection_ui.album_id == "wasserwege", "Die Öffnung führt direkt zum passenden Album")
	press("Deck bauen")
	var editor: RefCounted = app.deck_editor
	check(editor.filtered_ids().size() == 10, "Der Deckbau zeigt alle zehn Karten der Edition")
	var first: String = ids[0]
	editor.change(editor.draft.back(), -1)
	editor.change(first, 1)
	check(editor.draft.has(first) and Collection.valid_owned(app.save_data, "mosslyn", editor.draft),
		"Eine neu gezogene Karte passt in ein legales 40er-Deck")
	press("Deck speichern")
	check(app.save_data.card_decks.mosslyn.has(first), "Die gezogene Karte ist im gespeicherten Deck")
