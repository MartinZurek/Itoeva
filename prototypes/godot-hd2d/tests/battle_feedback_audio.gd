extends SceneTree
const Forest=preload("res://scripts/forest_catalog.gd")
const Feedback=preload("res://scripts/battle_feedback.gd")
var checks:=0
var failures:=0
func _initialize() -> void:call_deferred("run")
func check(ok: bool,label: String) -> void:
	checks+=1
	if not ok:failures+=1;push_error(label)
func run() -> void:
	var app=load("res://scenes/full_world.tscn").instantiate()
	DirAccess.make_dir_recursive_absolute("user://godot-tests")
	app.save_path="user://godot-tests/audio-feedback.json";DirAccess.remove_absolute(app.save_path)
	root.add_child(app)
	while app.loading_world:await process_frame
	await app.start_game();await app._mount_world("FOREST_TRAIL",Forest.position_for(5,.44))
	var enc=app.forest_encounter
	enc.model.begin();enc.model.advance_intro(3.2);enc.resume();enc.set_process(false)
	var m=enc.model
	m.cursor=0;m.round=5;m._begin_turn(0)
	var uid: int=m._add_minion(0,"lantern_snail")
	var foe: int=m._add_minion(2,"thornling")
	var unit: Dictionary=m.find_unit(uid)
	check(Feedback.unit_state(m,unit)=="sleeping" and not m.can_attack(uid),"Neu beschworener Helfer schlaeft und kann nicht angreifen")
	m._begin_turn(0)
	check(Feedback.unit_state(m,unit)=="ready" and m.can_attack(uid),"Im naechsten eigenen Zug wird Helfer bereit")
	m.decks[0].hand=["trail_blessing","frost_thread","moon_guard"]
	m.decks[0].crystals=2;m.decks[0].cap=5
	enc.view.build_commands();enc.view.refresh()
	var ready_cards:=0;var unaffordable:=0
	for b in enc.view.buttons:
		if b.get_meta("battle_action","")!="card":continue
		var index:=int(b.get_meta("hand_index"))
		check(bool(b.get_meta("playable"))==m.can_play(index),"Leuchtrahmen folgt echten Kartenregeln")
		if b.get_meta("playable"):ready_cards+=1
		else:unaffordable+=1
	check(ready_cards>0 and unaffordable>0,"Bezahlbare und zu teure Karten gleichzeitig unterscheidbar")
	check(enc.view.crystal_meter.available==2 and enc.view.crystal_meter.capacity==5,"Kristalle zeigen Vorrat und Kapazitaet")
	enc.view.selected_card=0;enc.view.refresh()
	check(enc.view.crystal_meter.preview_cost==2,"Zielkarte zeigt Kosten im Kristallvorrat")
	var choices:=Feedback.targets(m,0,-1)
	check("u:"+str(uid) in choices and "u:"+str(foe) not in choices,"Verstaerkung hebt nur eigenes legales Ziel hervor")
	check(m.attack_minion(uid,"u:"+str(foe)),"Bereiter Helfer greift an")
	m.acknowledge_animation()
	check(Feedback.unit_state(m,unit)=="spent" and not m.can_attack(uid),"Nach Angriff ist Helfer erschoepft")
	var copy=load("res://scripts/card_battle.gd").new();copy.configure("fennec");copy.restore(m.snapshot())
	check(copy.find_unit(uid).spent,"Angriffsstatus ueberlebt Laden")
	unit.frozen=1;m._begin_turn(0)
	check(Feedback.unit_state(m,unit)=="frozen" and not m.can_attack(uid),"Ausgesetzter Zug bleibt sichtbar, auch wenn Frostzaehler verbraucht ist")
	unit.ready=true
	check(Feedback.unit_state(m,unit)=="ready","Erneute echte Bereitschaft hebt alte Frostanzeige auf")
	m._begin_turn(0)
	check(Feedback.unit_state(m,unit)=="ready", "Frostanzeige endet bei wirklicher Bereitschaft")
	app.music_on=false;app.battle_music_on=true
	app.battle_audio._process(2)
	check(app.paused and app.battle_audio.arena.playing and not app.battle_audio.arena.stream_paused,"Kampfmusik spielt trotz pausierter Welt")
	app.battle_audio.played.clear();app.effects_on=false
	app.battle_audio.cue("impact")
	check(app.battle_audio.played.is_empty(),"Effektschalter unterdrueckt Spielsounds")
	app.effects_on=true
	app.battle_audio.cue("fire")
	check(app.battle_audio.played==["fire"],"Elementeffekt wird abgespielt")
	app.battle_audio.cue("fire")
	check(app.battle_audio.played.size()==1,"Doppelte Touchereignisse erzeugen keinen Klangstapel")
	app.battle_music_on=false;app.battle_audio._process(.4)
	check(app.battle_audio.arena.stream_paused,"Musikschalter wirkt im Kampf")
	app.battle_audio.played.clear()
	app.battle_audio.begin_event({"visual":"card_spell","element":"water","impact":.7,"duration":1.5,"effects":[]})
	app.battle_audio._process(.2)
	check("water" not in app.battle_audio.played,"Elementklang wartet auf Trefferzeit")
	app.battle_audio._process(.6)
	check(app.battle_audio.played.count("water")==1,"Elementklang trifft genau einmal zur Wirkung")
	app.battle_audio._process(.1)
	check(app.battle_audio.played.count("water")==1,"Spaetere Frames wiederholen Treffer nicht")
	app.battle_music_on=true;app.effects_volume=.3;app._save_preferences()
	app.effects_volume=.9;app._load_save()
	check(is_equal_approx(app.effects_volume,.3) and app.battle_music_on,"Audioeinstellungen werden gespeichert")
	app.battle_audio.played.clear();app.battle_audio.last_phase="battle"
	m.phase="victory";m.playback_pending=true
	app.battle_audio._process(.1)
	check("victory" not in app.battle_audio.played,"Siegesklang wartet auf finalen Angriff")
	m.playback_pending=false;app.battle_audio._process(.1)
	check(app.battle_audio.played.count("victory")==1,"Siegesklang folgt abgeschlossener Animation")
	app.battle_audio._process(.1)
	check(app.battle_audio.played.count("victory")==1,"Siegesklang wird nicht pro Frame wiederholt")
	app.show_menu();app.battle_audio._process(1.0)
	check(app.battle_audio.arena.stream_paused,"Menue pausiert Kampfmusik")
	var any_voice:=false
	for voice in app.battle_audio.voices:
		if voice.playing:any_voice=true
	check(not any_voice,"Menue beendet laufende Effekte")
	app.queue_free();await process_frame;DirAccess.remove_absolute("user://godot-tests/audio-feedback.json")
	print("Kampfklang und Lesbarkeit: %s Checks, %s Fehler" % [checks,failures])
	quit(1 if failures else 0)
