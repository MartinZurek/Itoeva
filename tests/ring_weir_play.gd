extends SceneTree

const Catalog = preload("res://scripts/world_catalog.gd")
const Cards = preload("res://scripts/card_catalog.gd")
const Collection = preload("res://scripts/card_collection.gd")

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

func frames(count: int) -> void:
	for frame in count:
		await physics_frame
		await process_frame

func target_for(kind: String, id: String) -> Dictionary:
	for target in app.interaction_targets():
		if target.kind == kind and target.id == id:
			return target
	return {}

func position_at(target: Dictionary) -> void:
	app.actor.position = target.approach
	app.actor.velocity = Vector3.ZERO
	app._update_camera(10.0)
	await frames(8)

func door(to: String) -> void:
	var id: String = app.current_place() + ":" + to
	var target := target_for("door", id)
	check(not target.is_empty(), "Durchgang vorhanden · " + id)
	if target.is_empty(): return
	await position_at(target)
	await app.interact_with("door", id)
	await frames(5)

func run() -> void:
	app = load("res://scenes/full_world.tscn").instantiate()
	app.save_path = "user://ring-weir-play-%s.json" % OS.get_process_id()
	root.add_child(app)
	await process_frame
	app.selected = "mosslyn"
	app.Journey.remember_victory(app.save_data, app.selected, "vharok", true)
	app.Journey.remember_victory(app.save_data, app.selected, "relay_warden", true)
	app.Story.mark(app.save_data, app.selected, "clue_leaf")
	app.Story.mark(app.save_data, app.selected, "clue_seal")
	await app._mount_world("RELAY_COURT", Catalog.position_for("RELAY_COURT", Vector2(0.6, 0.55)))
	app._resume()
	await frames(8)
	check(app.atlas_path("RELAY_COURT", "RING_WEIR").is_empty(), "Atlas kennt den gesperrten Pfad vor dem Relais")
	await door("RING_WEIR")
	check(app.room == "RELAY_COURT" and app.notice.contains("Stimme"), "Echter Durchgang erklärt die fehlende Voraussetzung")
	await position_at(app.Story.offer(app))
	await app.interact_with("story", "relay")
	check(app.Story.record(app.save_data, app.selected).get("relay", false), "Relais über die normale Interaktion abgestimmt")
	app._resume()
	check(app.atlas_path("RELAY_COURT", "RING_WEIR") == ["RELAY_COURT", "RING_WEIR"], "Atlas verbindet Relaisgarten und Ringwehr")
	await door("RING_WEIR")
	check(app.room == "RING_WEIR" and is_instance_valid(app.ring_weir), "Ringwehr korrekt geladen")
	check(app.ring_weir.get_node("Ried").texture != null, "Ried hat ein eigenes Sprite")
	check(is_equal_approx(app.ring_weir.material.get_shader_parameter("water_activity"), 0.24), "Vor dem Hebel fließt nur Restwasser")
	check(app.atlas_path("RING_WEIR", "MILL_RUN").is_empty(), "Zulauf vor dem Hebel von beiden Seiten gesperrt")
	await door("MILL_RUN")
	check(app.room == "RING_WEIR" and app.notice.contains("Hebel"), "Brücke erklärt den gesperrten Zulauf")

	await position_at(target_for("ried", "ried"))
	var ried_screen: Vector2 = app.camera.unproject_position(target_for("ried", "ried").position)
	check(app.hud._tap_marker(ried_screen) and app.dialog_open, "Ried öffnet seinen Dialog über den Touchmarker")
	app._resume()
	await position_at(target_for("ring_lever", "ring_lever"))
	var lever_screen: Vector2 = app.camera.unproject_position(target_for("ring_lever", "ring_lever").position)
	check(app.hud._tap_marker(lever_screen), "Der Hebel reagiert über den Touchmarker")
	await frames(58)
	check(app.Story.record(app.save_data, app.selected).get("ring_weir", false), "Wehrzustand ist gespeichert")
	check(is_equal_approx(app.ring_weir.material.get_shader_parameter("water_activity"), 1.0), "Wasser läuft nach dem Hebel sichtbar stärker")
	check(target_for("ring_lever", "ring_lever").is_empty(), "Geöffnetes Wehr lässt sich nicht versehentlich zurücksetzen")
	check(app.atlas_path("RING_WEIR", "MILL_BROOK") == ["RING_WEIR", "MILL_RUN", "MILL_BROOK"], "Atlas verwendet die freigeschaltete Abkürzung")
	check(not app.Story.record(app.save_data, "nocti").get("ring_weir", false), "Anderer Charakter behält eigenen Kapitelstand")
	app.set_atlas_destination("MILL_BROOK")
	await door("MILL_RUN")
	check(app.room == "MILL_RUN" and app.atlas_next_place == "MILL_BROOK", "Aktive Route führt durch den alten Zulauf")
	var bridge := Catalog.position_for("MILL_RUN", Vector2(0.5, 0.5))
	var water := bridge + Vector3(0, 0, 2.0)
	check(app.stage.is_walkable(bridge, app.actor.collider.shape.radius) and not app.stage.is_walkable(water, 0.1),
		"Brücke ist begehbar, Wasser daneben begrenzt")
	app.request_interaction("door", "MILL_RUN:MILL_BROOK")
	for frame in 510:
		if app.room == "MILL_BROOK" and not app.switching: break
		await frames(1)
	await frames(5)
	check(app.room == "MILL_BROOK" and app.atlas_destination.is_empty(), "Automatisches Laufen über die Brücke bis in den Mühlenbach")
	var hootlet := target_for("talk", "hootlet")
	check(not hootlet.is_empty(), "Hootlet wartet nach der Rückkehr am Mühlenbach")
	await position_at(hootlet)
	await app.interact_with("talk", "hootlet")
	var found_report := false
	for button in app.menu_buttons:
		if button.text == "Vom Ringwehr berichten":
			found_report = true
			button.pressed.emit()
			break
	check(found_report and app.Story.record(app.save_data, "mosslyn").get("ring_report", false), "Hootlets Gespräch schließt das Kapitel ab")
	check(Collection.owned(app.save_data, "mosslyn", "shore_roots") == 1, "Mooslyn erhält einmalig Uferwurzeln")
	check(Collection.owned(app.save_data, "mosslyn", "brook_oath") == 0, "Chronikblatt bleibt von der Mühlenchronik getrennt")
	check(app.Story.report_at_mill(app, "mosslyn").is_empty() and Collection.owned(app.save_data, "mosslyn", "shore_roots") == 1,
		"Wiederholung vergibt keine zweite Karte")
	app._resume()
	var saved: Dictionary = JSON.parse_string(FileAccess.get_file_as_string(app.save_path))
	check(app.Story.record(saved, "mosslyn").get("ring_return", false), "Rückkehr steht im gespeicherten Reisetagebuch")
	check(app.Story.record(saved, "mosslyn").get("ring_report", false) and Collection.owned(saved, "mosslyn", "shore_roots") == 1,
		"Eintrag und Kartenbesitz sind gemeinsam gespeichert")
	var reward_map: Dictionary = Collection.data().chapter_rewards.ring_report
	for species in Catalog.STARTERS:
		var expected: String = reward_map[species]
		var deck := Cards.starter(species)
		check(Cards.valid(species, deck) and expected not in deck, "Startdeck bleibt gültig · " + species)
		var rewarded: Dictionary = {}
		check(Collection.claim_chapter_card(rewarded, species, "ring_report") == expected, "Eigene Kapitelkarte · " + species)
		for other in Catalog.STARTERS:
			check((expected in Cards.available(other)) == (species == other), "Klassenfilter · %s/%s" % [species, other])
	app.save_data = saved
	await door("MILL_RUN")
	await door("RING_WEIR")
	check(app.room == "RING_WEIR" and is_equal_approx(app.ring_weir.material.get_shader_parameter("water_activity"), 1.0),
		"Nach Laden bleibt die Abkürzung in beide Richtungen offen")

	var save_path: String = app.save_path
	app.queue_free()
	await process_frame
	DirAccess.remove_absolute(save_path)
	print("Ringwehr · %s Prüfungen, %s Fehler" % [checks, failures])
	quit(1 if failures else 0)
