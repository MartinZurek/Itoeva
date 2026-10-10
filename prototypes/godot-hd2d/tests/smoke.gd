extends SceneTree

var failures := 0

func _initialize() -> void:
	call_deferred("run")

func check(condition: bool, message: String) -> void:
	if not condition:
		failures += 1
		push_error(message)
	else:
		print("PASS: ", message)

func frames(count: int) -> void:
	for i in count:
		await physics_frame

func run() -> void:
	var scene := load("res://scenes/painted_world.tscn") as PackedScene
	var world := scene.instantiate()
	root.add_child(world)
	await frames(15)
	var actor: CharacterBody3D = world.get_node("Fennec")
	check(actor.is_on_floor(), "Fennec steht auf dem echten 3D-Boden")
	check(actor.sprite.texture.get_size() == Vector2(1536, 1536), "Originalframes passen in mobile Texturgroessen")
	check(actor.sprite.billboard == BaseMaterial3D.BILLBOARD_FIXED_Y, "Fennec ist ein aufrechtes 2D-Sprite")
	# Native GameCharacterScale: Kopf/Fuss-Spanne 83, Kulisse 640px hoch.
	# Kamera verkuerzt die aufrechte Figur; am mittleren Weg gilt 65..108px.
	var projected_body_px: float = 83.0 * actor.sprite.pixel_size * cos(deg_to_rad(world.PITCH)) / world.IMAGE_HEIGHT * 640.0
	check(projected_body_px >= 84.0 and projected_body_px <= 92.0, "Fennecs projizierte Koerperhoehe entspricht dem nativen mittleren Wegmassstab")
	var x0: float = actor.position.x
	Input.action_press("move_right")
	await frames(60)
	Input.action_release("move_right")
	await frames(10)
	check(actor.position.x > x0 + 1.4, "Seitlicher Gang bewegt sich im Raum")
	check(actor.sprite.frame < 138, "Auswahl bleibt innerhalb der vorhandenen 138 Rollen")
	actor.position = Vector3(-3.15, 0.02, 0.3)
	actor.velocity = Vector3.ZERO
	Input.action_press("move_back")
	await frames(120)
	Input.action_release("move_back")
	await frames(10)
	check(actor.position.z > world.path_z(world.PATH_BACK_V) + 0.65, "Die Bank blockiert den Lauf durch ihre Sitzflaeche")
	actor.position = Vector3(1.0, 0.02, 0.0)
	actor.velocity = Vector3.ZERO
	await frames(15)
	actor.jump_requested = true
	await frames(12)
	check(actor.position.y > 0.4, "Sprung hebt die 2D-Figur physisch vom 3D-Boden")
	await frames(80)
	check(actor.is_on_floor() and absf(actor.position.y) < 0.05, "Landung stellt den Fussanker wieder her")
	actor.position = Vector3(-3.15, 1.3, world.path_z(world.PATH_BACK_V) + 0.30)
	actor.velocity = Vector3.ZERO
	await frames(60)
	check(actor.is_on_floor() and absf(actor.position.y - 0.96) < 0.05, "Bank ist eine echte erhoehte Standflaeche")
	check(absf(actor.shadow.global_position.y - 0.968) < 0.02, "Bodenschatten folgt der erhoehten Standflaeche")
	actor.position = Vector3(1.0, 0.02, 0.0)
	actor.velocity = Vector3.ZERO
	Input.action_press("move_front")
	await frames(100)
	Input.action_release("move_front")
	await frames(10)
	check(actor.position.z < world.path_z(world.PATH_FRONT_V) - 0.1, "Vordere Kante ist unpassierbar")
	Input.action_press("move_back")
	await frames(150)
	Input.action_release("move_back")
	await frames(10)
	check(actor.position.z > world.path_z(world.PATH_BACK_V) + 0.1, "Hintere Kulisse ist unpassierbar")
	actor.position = Vector3(13.4, 0.02, 0)
	actor.velocity = Vector3.ZERO
	Input.action_press("move_right")
	await frames(60)
	Input.action_release("move_right")
	check(actor.position.x < 13.7, "Seitliches Kartenende haelt Abstand fuer die gesamte Figur")
	world.toggle_dusk()
	check(world.dusk and world.lamp.light_energy > 3.0, "Abendlicht kann eingeschaltet werden")
	world.toggle_dusk()
	check(not world.dusk and world.art_material.albedo_color == Color.WHITE, "Tageslicht erhaelt die Originalfarben")
	actor.touch_vector = Vector2(-0.8, 0.0)
	await frames(60)
	actor.touch_vector = Vector2.ZERO
	check(actor.position.x < 14.0, "Touchrichtung bewegt den Charakter")
	for edge in [-13.65, 13.65]:
		for angle in [-8.0, 0.0, 8.0]:
			for camera_zoom in [6.8, 9.2]:
				for depth in [world.path_z(world.PATH_BACK_V) + 0.23, world.path_z(world.PATH_FRONT_V) - 0.23]:
					actor.position = Vector3(edge, 0.02, depth)
					actor.velocity = Vector3.ZERO
					world.zoom = camera_zoom
					world.yaw = angle
					# Nachlauf direkt abschliessen; echte Projektionsmatrix pruefen.
					for i in 3:
						world._update_camera(10.0)
					await frames(2)
					var fits := true
					var billboard_right: Vector3 = Vector3(world.camera.global_basis.x.x, 0.0, world.camera.global_basis.x.z).normalized()
					for x in [-64.0, 64.0]:
						for y in [-2.0, 126.0]:
							var corner: Vector3 = actor.global_position + (billboard_right * x + Vector3.UP * y) * actor.sprite.pixel_size
							fits = fits and root.get_visible_rect().has_point(world.camera.unproject_position(corner))
					check(fits, "Ganze Spritekarte bleibt sichtbar: Rand=%s Schwenk=%s Zoom=%s Tiefe=%s" % [edge, angle, camera_zoom, depth])
	world.yaw = 0.0
	world.zoom = 7.6
	actor.position = Vector3(1.0, 0.02, 0.0)
	actor.velocity = Vector3.ZERO
	await frames(15)
	var hud: Control = world.hud
	var press := InputEventScreenTouch.new()
	press.index = 0
	press.pressed = true
	press.position = Vector2(hud.size.x * 0.15, hud.size.y * 0.85)
	root.push_input(press, true)
	await frames(2)
	var drag := InputEventScreenDrag.new()
	drag.index = 0
	drag.position = press.position + Vector2(45, 0)
	root.push_input(drag, true)
	await frames(5)
	check(actor.touch_vector.x > 0.6, "Echter ScreenTouch/Drag steuert den Joystick")
	var jump := InputEventScreenTouch.new()
	jump.index = 1
	jump.pressed = true
	jump.position = hud.actions[0].button.get_global_rect().get_center()
	root.push_input(jump, true)
	await frames(12)
	check(actor.position.y > 0.4 and actor.touch_vector.x > 0.6, "Zweiter Finger springt waehrend der Joystick gehalten wird")
	press.pressed = false
	root.push_input(press, true)
	jump.pressed = false
	root.push_input(jump, true)
	await frames(5)
	check(actor.touch_vector == Vector2.ZERO, "Loslassen beendet die Touchbewegung")
	world.queue_free()
	await process_frame
	print("Godot smoke: ", failures, " Fehler")
	quit(0 if failures == 0 else 1)
