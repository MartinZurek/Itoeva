extends RefCounted

const Catalog = preload("res://scripts/world_catalog.gd")
const FLOWERS := {
	"SUN_GLADE": [Vector2(.34,.55), Vector2(.70,.56)],
	"MILL_BROOK": [Vector2(.33,.58), Vector2(.73,.56)],
	"MIST_WATCH": [Vector2(.25,.55), Vector2(.73,.60)]
}
const CAPACITY := 18
var motes: Array[Dictionary] = []
var cooldown := 0.0
var last_landing := 0
var events := 0

func reset(actor: CharacterBody3D) -> void:
	motes.clear()
	cooldown = 0.0
	last_landing = actor.jump_landings

func stir(point: Vector3, heading: float, count: int = 7) -> void:
	events += 1
	cooldown = 1.7
	for i in count:
		if motes.size() >= CAPACITY: motes.pop_front()
		var fraction := float(i) / float(count)
		motes.append({"origin": point + Vector3((fraction-.5)*.55,.2+fraction*.18,0),
			"clock": 0.0, "life": 1.7 + fraction*.75,
			"velocity": Vector3(heading*(.23+fraction*.26), .34+fraction*.16, (fraction-.5)*.17),
			"seed": float(i)*2.4, "leaf": i % 3 == 0})

func advance(delta: float, app) -> void:
	cooldown = maxf(0.0, cooldown-delta)
	for i in range(motes.size()-1,-1,-1):
		motes[i].clock += delta
		if motes[i].clock >= motes[i].life: motes.remove_at(i)
	var landed: bool = app.actor.jump_landings != last_landing
	last_landing = app.actor.jump_landings
	if cooldown > 0.0 or (not landed and app.actor.actual_speed < 2.65): return
	for uv: Vector2 in FLOWERS.get(app.room, []):
		var point := Catalog.position_for(app.room, uv)
		if Vector2(app.actor.position.x-point.x, app.actor.position.z-point.z).length() < 1.6:
			stir(point, signf(app.actor.velocity.x) if absf(app.actor.velocity.x) > .1 else 1.0)
			return

func draw(hud: Control) -> void:
	var camera: Camera3D = hud.app.camera
	for mote in motes:
		var t: float = mote.clock
		var point: Vector3 = mote.origin + mote.velocity*t
		point.x += sin(t*3.4+mote.seed)*.075
		if camera.is_position_behind(point): continue
		var at := camera.unproject_position(point)
		var fade := sin(PI * t / float(mote.life))
		var tint := Color("e4cd7b") if mote.leaf else Color("f9e2a0")
		var radius := clampf(18.0 / camera.global_position.distance_to(point), 1.0, 2.8)
		if mote.leaf:
			var sway := sin(t*3.2+mote.seed)
			var shape := PackedVector2Array([at+Vector2(-radius*2,0),at+Vector2(0,-radius*.65),at+Vector2(radius*2,radius*sway),at+Vector2(0,radius*.8)])
			hud.draw_colored_polygon(shape, Color(tint,.6*fade))
		else:
			hud.draw_circle(at,radius,Color(tint,.65*fade),true,-1,true)
