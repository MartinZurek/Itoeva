extends SceneTree

const Catalog = preload("res://scripts/world_catalog.gd")
var app: Node3D
var checks := 0
var failures := 0

func _initialize() -> void:
	call_deferred("run")

func check(ok: bool, label: String) -> void:
	checks += 1
	if not ok:
		failures += 1
		push_error(label)

func frames(count: int) -> void:
	for i in count: await physics_frame

func touch(index: int, point: Vector2, pressed: bool, canceled: bool = false) -> void:
	var event := InputEventScreenTouch.new()
	event.index = index
	event.position = point
	event.pressed = pressed
	event.canceled = canceled
	root.push_input(event, true)

func drag(index: int, point: Vector2) -> void:
	var event := InputEventScreenDrag.new()
	event.index = index
	event.position = point
	root.push_input(event, true)

func mount(species: String, location: String = "SPORT") -> void:
	app.selected = species
	await app._mount_world("OUTDOOR", Catalog.position_for(location, Vector2(.45,.65)))
	app._resume()
	for resident in app.actors:
		if resident != app.actor:
			resident.enabled = false
			resident.collider.disabled = true
	await frames(60)

func run() -> void:
	DirAccess.make_dir_recursive_absolute("user://godot-tests")
	app = load("res://scenes/full_world.tscn").instantiate()
	app.save_path = "user://godot-tests/movement-flow.json"
	DirAccess.remove_absolute(app.save_path)
	root.add_child(app)
	while app.loading_world: await process_frame
	await mount("fennec")
	var center: Vector2 = app.hud.action_center()
	# Das Testziel ist die Gestensteuerung; zufaellig hier projizierte
	# Ortsmarker duerfen den ersten linken Touch nicht als Wegauftrag abfangen.
	app.hud.markers.clear()
	# Gesten mit beiden Daumen und schraegen Bewegungen.
	touch(0, Vector2(118,600),true)
	drag(0,Vector2(171,600))
	var walking_vector: Vector2 = app.actor.touch_vector
	touch(1,center,true)
	drag(1,center+Vector2(18,-52))
	await frames(8)
	check(app.actor.jump_clock >= 0 and app.actor.position.y > .35,"Schraege Aufwaertsgeste springt, Gehdaumen bleibt aktiv")
	check(walking_vector.length() > .14 and app.actor.touch_vector.is_equal_approx(walking_vector),"Sprung erhaelt die tatsaechliche Gehdaumeneingabe")
	touch(1,center+Vector2(18,-52),false)
	check(not app.actor.jump_requested,"Sprung-Release erzeugt keinen zweiten Sprung")
	var vertical: float = app.actor.velocity.y
	touch(1,center,true)
	drag(1,center+Vector2(5,52))
	check(app.actor.pending_sit and is_equal_approx(app.actor.velocity.y,vertical),"Sitzen in Luft wartet auf Landung ohne Sprungabbruch")
	touch(1,center,false)
	await frames(95)
	check(app.actor.state == "sit" and app.actor.is_on_floor(),"Vorgemerkter Sitz wird bei der Landung ausgefuehrt")
	touch(0,Vector2(171,600),false)
	touch(1,center,true)
	drag(1,center+Vector2(0,48))
	touch(1,center,false)
	check(app.actor.state == "idle","Neue Abwaertsgeste steht genau einmal auf")
	app.actor.request_action("jump")
	await frames(8)
	app.actor.request_action("sit")
	app.show_menu()
	check(not app.actor.pending_sit,"Menue verwirft vorgemerkten Sitz")
	app._resume()
	await frames(100)
	check(app.actor.state != "sit","Fortsetzen fuehrt keine alte Geste aus")
	app.actor.request_action("jump")
	await frames(8)
	app.actor.request_action("sit")
	app.actor.request_action("jump")
	check(not app.actor.pending_sit,"Neuere Sprungabsicht ersetzt alten Sitz")
	await frames(100)
	# Die kuerzere Rolle ist ein echter Schwung und laesst einen Folgesprung zu.
	app.actor.facing = Vector2.RIGHT
	app.actor.request_action("roll")
	var peak := 0.0
	for i in 20:
		await frames(1)
		peak = maxf(peak,app.actor.actual_speed)
		check(app.stage.is_walkable(app.actor.position,app.actor.collider.shape.radius,true),"Schwung respektiert Weg: %s"%i)
	check(peak > app.actor.RUN_SPEED,"Schwung erreicht mehr Tempo als Sprint")
	app.actor.request_action("jump")
	await frames(8)
	check(app.actor.jump_clock >= 0 and app.actor.position.y > .35,"Sprung folgt ohne Leerlauf auf den Schwung")
	await frames(100)
	# Kamera fuehrt voraus, aendert aber weder Zoom noch gemalte Blickachse.
	app.actor.touch_vector = Vector2.RIGHT
	await frames(28)
	check(app.actor.running and app.actor.run_blend > .99,"Aussenring erreicht eindeutigen Vierbeinerlauf")
	check(app.camera_lead > .3 and app.camera_lead <= .951,"Kamera blickt begrenzt voraus")
	check(absf(app.camera.size-app.stage.default_zoom)<.01 and is_zero_approx(app.yaw),"Laufen erzeugt keinen Groessenzoom oder Kulissenorbit")
	app.actor.stop_input()
	await frames(18)
	check(app.actor.actual_speed < .01,"Loslassen bremst schnell und ohne Gleiten")
	# Pro Ansicht gleich hohe Ruhefigur; je Folge konstanter Faktor.
	for species in Catalog.STARTERS:
		await mount(species)
		app.actor.enabled = false
		# Kompaktatlanten verwenden eine eigene 48-Posen-Aufteilung.
		if Catalog.data().characters[species].get("compact",false): continue
		var reference: float = app.actor.base_pixel_size*109*Catalog.relative_height(species)
		for direction in [Vector2.RIGHT,Vector2.LEFT,Vector2(0,1),Vector2(0,-1)]:
			app.actor.facing = direction
			app.actor.state = "idle"
			app.actor.jump_clock = -1
			app.actor.recovery_clock = 1
			app.actor._update_frame(false)
			var height: float = app.actor.frame_bounds[app.actor.sprite.frame].size.y*app.actor.sprite.pixel_size
			check(absf(reference-height)<.01,"Gleiche sichtbare Ruhehoehe: %s/%s"%[species,direction])
		var medians: Array[float] = []
		for first in [9,68,76]:
			var heights: Array[float] = []
			var factor: float = app.actor.pose_scales[first]
			for frame in range(first,first+8):
				check(is_equal_approx(factor,app.actor.pose_scales[frame]),"Fester Gangmassstab: %s/%s"%[species,frame])
				heights.append(app.actor.frame_bounds[frame].size.y*factor)
			medians.append(app.actor._median(heights))
		for height in medians:
			check(absf(height-medians[0])/medians[0]<.035,"Keine Groessenspruenge zwischen Gehansichten: "+species)
		check(is_equal_approx(app.actor.collider.shape.radius,.14375),"Groessere Malerei verbreitert Capsule nicht: "+species)
		if species == "fennec":
			app.actor.facing=Vector2.RIGHT
			app.actor.running=true
			app.actor.run_blend=1
			for phase in 8:
				app.actor.gait=(phase+.1)/8.0
				app.actor._update_frame(true)
				check(app.actor.sprite.frame==114+phase,"Originaler Vierbeiner statt schnellem Gehen: %s"%phase)
	# Hootlet dreht auch im bereits laufenden Sprung mit Fluegeln.
	await mount("hootlet")
	app.actor.request_action("jump")
	await frames(10)
	var before_y: float = app.actor.velocity.y
	check(app.actor.request_action("roll") and app.actor.state=="twirl","Hootlet akzeptiert Luftwende im Sprung")
	check(is_equal_approx(app.actor.velocity.y,before_y),"Luftwende erhaelt Flugbahn")
	var poses := {}
	for i in 38:
		await frames(1)
		poses[app.actor.sprite.frame]=true
		check(app.actor.sprite.frame not in range(130,138),"Keine Rollkugel im Flug: %s"%i)
		check(app.stage.is_walkable(app.actor.position,app.actor.collider.shape.radius,true),"Luftwende bleibt ueber erlaubtem Weg: %s"%i)
	check(poses.has(85) and poses.has(101) and poses.has(108),"Luftwende zeigt alle drei gerichteten Fluegelzeichnungen")
	await frames(70)
	check(app.actor.is_on_floor() and app.actor.state!="twirl","Luftwende endet und landet")
	app.actor.mount_seat(app.actor.position+Vector3.UP*.4,app.actor.position)
	app.actor.request_action("roll")
	await frames(8)
	check(not app.actor.mounted and app.actor.state=="twirl" and app.actor.position.y>.1,"Luftwende verlaesst einen Sitz korrekt")
	await frames(100)
	# Tatsächlicher Kontexttipp und persistente, einmalige Entdeckung.
	await mount("fennec","PARK")
	var pickup: Dictionary = app.stage.pickups[0]
	app.actor.position=app.stage.safe_position(pickup.position+Vector3.RIGHT*.65,app.actor.collider.shape.radius)
	await frames(15)
	var target: Dictionary=app.context_target()
	check(not target.is_empty() and target.kind=="pickup","Naechster erreichbarer Fund wird Kontextaktion")
	touch(1,app.hud.action_center(),true)
	app.actor.position = app.stage.safe_position(Catalog.position_for("SPORT",Vector2(.45,.65)),app.actor.collider.shape.radius)
	check(app.hud.action_context.kind=="pickup", "Gehaltenes Tippen behaelt das sichtbare Ziel")
	touch(1,app.hud.action_center(),false)
	check(pickup.art.visible and not app.actor.jump_requested,"Ausser Reichweite kein anderes Ziel oder ungeplanter Sprung")
	app.actor.position=app.stage.safe_position(pickup.position+Vector3.RIGHT*.65,app.actor.collider.shape.radius)
	await frames(15)
	touch(1,app.hud.action_center(),true)
	touch(1,app.hud.action_center(),false)
	await frames(2)
	check(not pickup.art.visible and app.actor.jump_clock<0,"Kontexttipp hebt auf statt versehentlich zu springen")
	check(app.save_data.collected==["park-pebbles"],"Fund genau einmal lokal gespeichert")
	var saved: Dictionary=JSON.parse_string(FileAccess.get_file_as_string(app.save_path))
	check(saved.collected==["park-pebbles"] and saved.has("characters"),"Fundspeicherung erhaelt bestehenden Fortschritt")
	await mount("fennec","PARK")
	check(app.stage.pickups.is_empty(),"Aufgehobener Fund erscheint beim Ortswechsel nicht erneut")
	check("PARK" in app.save_data.discovered and "SPORT" in app.save_data.discovered,"Ortsentdeckungen bleiben als Kartenfortschritt erhalten")
	DirAccess.remove_absolute(app.save_path)
	print("Godot movement flow: %s Checks, %s Fehler"%[checks,failures])
	quit(1 if failures else 0)
