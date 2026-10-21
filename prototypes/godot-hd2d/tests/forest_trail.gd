extends SceneTree
const Forest=preload("res://scripts/forest_catalog.gd")
const Battle=preload("res://scripts/forest_battle.gd")
const Navigator=preload("res://scripts/interaction_path.gd")
var failures:=0
var checks:=0
func _initialize() -> void: call_deferred("run")
func check(ok: bool, label: String) -> void:
	checks+=1
	if not ok:
		failures+=1
		push_error(label)
func frames(count: int) -> void:
	for i in count: await physics_frame
func run() -> void:
	var app: Node3D=load("res://scenes/full_world.tscn").instantiate()
	DirAccess.make_dir_recursive_absolute("user://godot-tests")
	app.save_path="user://godot-tests/forest-trail.json"
	DirAccess.remove_absolute(app.save_path)
	root.add_child(app)
	while app.loading_world: await process_frame
	await app.start_game()
	check(app.stage.doors.size()==7,"Originalwelt plus Wald- und Sonnenhainabzweig")
	var door: Dictionary
	for entry in app.stage.doors:
		if entry.to=="FOREST_TRAIL": door=entry
	app.actor.position=app.stage.safe_position(door.position,app.actor.collider.shape.radius)
	await frames(15)
	await app.interact_with("door","FOREST:FOREST_TRAIL")
	await frames(15)
	check(app.room=="FOREST_TRAIL" and app.actor.is_on_floor(),"Waldtuer und sicherer Bodenkontakt")
	check(app.stage.chunks.size()==7,"Sieben zusammenhaengende Waldabschnitte")
	for species in app.Catalog.STARTERS:
		app.selected=species
		await app._mount_world("FOREST_TRAIL",Forest.position_for(0,.15))
		app._resume()
		await frames(8)
		var radius: float=app.actor.collider.shape.radius
		for i in range(1370):
			var x:=2.5+i*.10
			var band:=Forest.limits(x)
			var point:=Vector3(x,.03,app.Catalog.image_z((band.x+band.y)*.5))
			check(app.stage.is_walkable(point,radius),"Waldmittelspur %s bei %s" % [species,x])
			var wet:=point+Vector3(0,0,1.8)
			check(not app.stage.is_walkable(wet,radius),"Wegsaum blockiert %s bei %s" % [species,x])
		for i in range(1,7):
			var a: Vector3=app.stage.safe_position(Vector3(i*20.0-.5,.03,0),radius)
			var b: Vector3=app.stage.safe_position(Vector3(i*20.0+2.5,.03,0),radius)
			check(not Navigator.find_path(app.stage,a,b,radius).is_empty(),"Nahtnavigation %s / %s" % [species,i])
		var bench: Dictionary=app.stage.furniture[0]
		app.actor.position=app.stage.safe_position(bench.approach_position,radius)
		app.actor.velocity=Vector3.ZERO
		await frames(12)
		await app.interact_with("seat","forest-rest-bench")
		check(app.actor.mounted and app.actor.facing==Vector2.DOWN,"Waldbank und Blickrichtung "+species)
		app.actor.leave_seat()
		await frames(5)
		check(app.stage.is_walkable(app.actor.position,radius),"Freier Bankausstieg "+species)
		app.actor.position=Forest.position_for(4,.50)
		app.actor.velocity=Vector3.ZERO
		await frames(12)
		check(app.actor.is_on_floor(),"Physische Schattenbachbruecke "+species)
		var before: Vector3=app.actor.position
		app.actor.touch_vector=Vector2.DOWN
		await frames(25)
		check(app.stage.is_walkable(app.actor.position,radius) and app.actor.position.z<before.z+.45,"Brueckenrand blockiert echten Lauf "+species)
		app.actor.stop_input()
		check(not app.stage._segment_walkable(before,before+Vector3(0,0,3),radius),"Sprung/Rolle kann Ufer nicht ueberspringen")
	app.selected="fennec"
	await app._mount_world("FOREST_TRAIL",Forest.position_for(5,.44))
	app._resume()
	await frames(8)
	var encounter: Node=app.forest_encounter
	check(encounter.model.phase=="intro" and app.paused,"Lichtung startet genau eine Begegnung")
	app.show_menu()
	var clock: float=encounter.model.intro_time
	await frames(20)
	check(encounter.model.intro_time==clock,"Menue friert Erscheinung ein")
	app._resume()
	encounter.model.advance_intro(3.2)
	encounter.persist()
	app.show_menu()
	app._resume()
	check(encounter.model.phase=="battle" and app.paused and not app.actor.enabled,"Fortsetzen behaelt Kampfmodus")
	var hp: int=encounter.model.enemy_health
	await frames(5)
	var strike: Button
	for button in app.menu_buttons:
		if button.get_meta("battle_action","")=="skill": strike=button
	var touch:=InputEventScreenTouch.new()
	touch.index=4
	touch.pressed=true
	touch.position=strike.get_global_rect().get_center()
	root.push_input(touch,true)
	root.push_input(touch,true)
	touch.pressed=false
	root.push_input(touch,true)
	check(encounter.model.enemy_health==hp and encounter.model.turn==1 and encounter.model.board.size()==1,"Mehrfachtouch beschwoert genau einen Helfer")
	check(encounter.model.cursor==0 and encounter.model.playback_pending,"Superkraft behaelt den eigenen Kartenzug")
	app._save_progress()
	var restored: Dictionary=JSON.parse_string(FileAccess.get_file_as_string(app.save_path))
	check(restored.forest_encounters.fennec.turn==1,"Kampfrunde atomar im bestehenden Spielstand")
	await app._mount_world("FOREST_TRAIL",app.actor.position)
	app._resume()
	encounter=app.forest_encounter
	check(encounter.model.phase=="battle" and encounter.model.turn==1 and encounter.model.playback_pending,"Neuer Szenenaufbau nimmt Trefferanimation ohne neuen Schaden auf")
	encounter._process(encounter.busy+.01)
	for i in 160:
		if encounter.model.phase!="battle": break
		encounter.view.input_cooldown=0
		if encounter.model.cursor==2:
			encounter._process(1)
		else:
			var b=encounter.model
			var owner: int=b.cursor
			var acted:=false
			if b.can_act("break_free"): encounter._act("break_free");acted=true
			if not acted:
				for unit in b.units(owner):
					var target: String=b.legal_targets(owner)[0]
					if b.can_attack(int(unit.uid),target): encounter._unit_attack(int(unit.uid),target);acted=true;break
			var power: String="support" if b.party[owner].species=="starlet" and b.party[owner].barrier<15 else "skill"
			if not acted and b.can_act(power): encounter._act(power);acted=true
			if not acted:
				for card in b.decks[owner].hand.size():
					if b.can_play(card,"enemy"): encounter._card_play(card,"enemy");acted=true;break
			if not acted:encounter._act("end_turn")
		encounter._process(encounter.busy+.01)
	check(encounter.model.phase=="victory","Taktischer Kampf erreicht Sieg")
	encounter.finish()
	check(not app.paused and encounter.model.phase=="done","Sieg gibt Bewegung frei")
	await app._mount_world("FOREST_TRAIL",Forest.position_for(5,.6))
	app._resume()
	await frames(5)
	check(app.forest_encounter.model.phase=="done" and not app.stage.demon.visible,"Kein erneuter Spawn nach Sieg")
	check(app.stage.arena_lights==0,"Erloeschte Arena bleibt dunkel")
	var practice: Button=app.hud.get_node("BattlePractice")
	check(practice.visible,"Sieger kann neuen Kampf explizit wiederholen")
	practice.pressed.emit()
	check(app.paused and app.forest_encounter.model.phase=="intro","Wiederholen startet neue Erscheinung")
	encounter=app.forest_encounter
	encounter.model.phase="defeat"
	encounter.model.playback_pending=false
	for member in encounter.model.party: member.hp=0
	encounter._retry()
	check(encounter.model.phase=="battle" and encounter.model.party[0].hp==82,"Erneuter Versuch setzt LP zurueck")
	encounter.model.phase="defeat"
	encounter._retreat()
	await frames(20)
	check(not app.paused and app.current_place()=="FOREST_ENTRANCE" and app.forest_encounter.model.phase=="unseen","Rueckzug nach Niederlage bleibt bedienbar")
	await app._mount_world("FOREST_TRAIL",Forest.position_for(0,.1))
	app._resume()
	await frames(15)
	await app.interact_with("door","FOREST_TRAIL:FOREST")
	await frames(15)
	check(app.room=="OUTDOOR" and app.current_place()=="FOREST" and app.actor.is_on_floor(),"Rueckweg zum vorhandenen Wald")
	DirAccess.remove_absolute(app.save_path)
	print("Waldtest: %s Checks, %s Fehler" % [checks,failures])
	quit(1 if failures else 0)
