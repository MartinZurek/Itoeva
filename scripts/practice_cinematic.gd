extends Control

const Cards = preload("res://scripts/card_catalog.gd")
const Art = preload("res://scripts/card_assets.gd")
const Forge = preload("res://scripts/practice_forge_stage.gd")
const Creature = preload("res://scripts/practice_creature_art.gd")
const Locations = preload("res://scripts/practice_film_locations.gd")
const Timing = preload("res://scripts/practice_film_timing.gd")
const CUT_END := Timing.CUT_END
const FILM_SIZE := Vector2(1280, 720)
const SHOT := Rect2(0, 78, 1280, 548)
const COLORS = {"phoenix": Color("ffa45f"), "starfall": Color("a7d8ff"), "forge": Color("e6b673")}
const PLACES = {"forge": "DIE ALTE ESSE", "phoenix": "ASCHESCHLUCHT", "starfall": "STERNWARTE AM ABGRUND"}
const CUTS = {"forge": [0.0, 2.35, 4.95, 7.40], "phoenix": [0.0, 2.30, 5.10, 7.75], "starfall": [0.0, 2.75, 5.30, 7.70]}
const SOUND = {"forge": [[1.05, "fire"], [5.18, "summon"], [7.23, "impact"]],
	"phoenix": [[0.90, "fire"], [2.55, "wind"], [7.82, "fire"]],
	"starfall": [[1.30, "moon"], [4.05, "crystal"], [7.80, "light"]]}
const STORIES = {
	"forge": [["Die letzte Glut", "Unter dem Schiefer hat Vexzelanz die Glut der alten Esse bewahrt."],
		["Ein Ruf aus der Arena", "Seine bronzenen Platten antworten auf den Ruf."],
		["Der erste Schritt", "Eine Pfote hebt sich. Unter der anderen springen Funken aus der Fuge."],
		["Der Wächter ist wach", "Mit einem Brüllen tritt Vexzelanz auf deine Seite."]],
	"phoenix": [["Ein Funke im Wind", "Im Wind der Ascheschlucht schützt der Phönix seine letzte Feder."],
		["Abflug", "Er lässt den Fels los und fängt den Aufwind über dem Lavastrom."],
		["Über der Schlucht", "Mit jedem Flügelschlag zieht er die Glut hinter sich her."],
		["Die Feuerwende", "Er legt die Flügel an und trägt das Feuer in die Arena."]],
	"starfall": [["Die Sternwarte", "Starlet findet auf der verlassenen Plattform ein altes Sternenrad."],
		["Das Sternenrad", "Die Ringe drehen sich, bis drei Lichtpunkte auf einer Bahn liegen."],
		["Eine offene Bahn", "Starlet fängt den ersten Funken als Schutz auf."],
		["Sternenfall", "Die übrigen Funken stürzen durch die offene Bahn in die Arena."]],
	"K0125": [["Eine Spur im Stein", "Mnotessia folgt einer Inschrift bis zur alten Esse."],
		["Das Zeichen", "Die Glut macht sichtbar, was in den Stein eingeritzt wurde."],
		["Die Antwort", "Mnotessia prägt sich das Zeichen ein und richtet sich auf."],
		["An deiner Seite", "Mit dem neuen Zeichen betritt Mnotessia das Duell."]],
	"K0098": [["Nachtwache", "Hexcalia kehrt zu dem Sternenrad zurück, das es einst bewachte."],
		["Ein fehlender Stern", "Ein Lichtpunkt fehlt. Hexcalia tritt in die offene Bahn."],
		["Die Bahn schließt sich", "Das Sternenrad nimmt die Bewegung auf."],
		["Die Wache wechselt", "Hexcalia verlässt die Plattform und nimmt deinen Ruf an."]]
}

var viewport: SubViewport
var stage: Node3D
var atmosphere: Environment
var built_locations: Dictionary = {}
var orrery: Node3D
var camera: Camera3D
var actor: Sprite3D
var lantern: OmniLight3D
var hearth: OmniLight3D
var contact: MeshInstance3D
var paw_shadows: Array[MeshInstance3D] = []
var orbit: Node3D
var ring: MeshInstance3D
var shield_omen: MeshInstance3D
var crystal: MeshInstance3D
var shard: SphereMesh
var fragments: Array[MeshInstance3D] = []
var sparks: Array[MeshInstance3D] = []
var energy: StandardMaterial3D
var back: ColorRect
var film: TextureRect
var title: Label
var chapter: Label
var story: Label
var place_label: Label
var subject := ""
var motif := "forge"
var tint := Color.WHITE
var rendered_frame := -1
var shot := 0
var cue_index := 0

func _ready() -> void:
	size = FILM_SIZE
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	back = ColorRect.new()
	back.size = FILM_SIZE
	back.color = Color("080b13")
	back.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(back)
	_build_stage()
	film = TextureRect.new()
	film.position = SHOT.position
	film.size = SHOT.size
	film.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
	film.texture = viewport.get_texture()
	film.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(film)
	_rule(Vector2(0, SHOT.position.y), Vector2(1280, 1))
	_rule(Vector2(0, SHOT.end.y), Vector2(1280, 1))
	title = _caption(Rect2(42, 15, 620, 51), 32)
	place_label = _caption(Rect2(680, 18, 402, 43), 18)
	place_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	chapter = _caption(Rect2(42, 635, 935, 34), 21)
	story = _caption(Rect2(42, 669, 947, 33), 17)
	hide()

func start(id: String, elapsed: float = 0.0) -> void:
	subject = id
	motif = "starfall" if id in ["starfall", "K0098"] else "phoenix" if id == "phoenix" else "forge"
	_set_location(motif)
	tint = COLORS[motif]
	var card := Cards.get_card(id)
	title.text = str(card.name).to_upper()
	title.add_theme_color_override("font_color", tint.lightened(0.26))
	place_label.text = PLACES[motif]
	energy.albedo_color = tint.darkened(0.26)
	energy.emission = tint.darkened(0.40)
	lantern.light_color = tint
	var subject_art := Art.texture({"id": "starlet_caster", "species": "starlet", "kind": "minion"}) if id == "starfall" else Art.texture(card)
	actor.texture = Creature.pose(0) if subject == Creature.SUBJECT else Art.form_texture("phoenix") if motif == "phoenix" else subject_art
	actor.pixel_size = Creature.PIXEL_SIZE if subject == Creature.SUBJECT else 1.67 / maxf(1.0, float(actor.texture.get_height()))
	actor.offset = Creature.planted_offset(0) if subject == Creature.SUBJECT else Vector2(0, actor.texture.get_height() * 0.44)
	actor.visible = true
	crystal.visible = subject == "starfall"
	rendered_frame = -1
	cue_index = 0
	var cues: Array = SOUND[motif]
	while cue_index < cues.size() and float(cues[cue_index][0]) < elapsed:
		cue_index += 1
	tick(elapsed)

func take_cue(seconds: float) -> String:
	var cues: Array = SOUND[motif]
	if cue_index >= cues.size() or seconds < float(cues[cue_index][0]):
		return ""
	var cue: String = cues[cue_index][1]
	cue_index += 1
	return cue

func _set_location(key: String) -> void:
	if not built_locations.has(key):
		var location := Node3D.new()
		location.name = key
		stage.add_child(location)
		built_locations[key] = location
		match key:
			"forge": hearth = Forge.build(location)
			"phoenix": Locations.canyon(location)
			"starfall": orrery = Locations.observatory(location)
	for name in built_locations:
		built_locations[name].visible = name == key
	atmosphere.background_color = Color({"forge": "18141a", "phoenix": "572f3b", "starfall": "12243c"}[key])
	atmosphere.ambient_light_color = Color({"forge": "aeabc4", "phoenix": "edb19a", "starfall": "a4c9ff"}[key])

func tick(seconds: float) -> void:
	if subject.is_empty() or seconds >= CUT_END:
		stop()
		return
	show()
	viewport.render_target_update_mode = SubViewport.UPDATE_ALWAYS
	var t := clampf(seconds / CUT_END, 0.0, 1.0)
	modulate.a = smoothstep(0.0, 0.24, seconds) * (1.0 - smoothstep(CUT_END - 0.35, CUT_END, seconds))
	var marks: Array = CUTS[motif]
	shot = 0
	for index in range(1, marks.size()):
		if seconds >= float(marks[index]):
			shot = index
	var end := float(marks[shot + 1]) if shot < marks.size() - 1 else CUT_END
	var shot_time := smoothstep(float(marks[shot]), end, seconds)
	var lines: Array = STORIES.get(subject, STORIES[motif])[shot]
	chapter.text = lines[0]
	story.text = lines[1]
	var charge := smoothstep(0.18, 0.68, t)
	var reveal := smoothstep(0.035, 0.15, t)
	var release := smoothstep(0.77, 1.0, t)
	var beat := exp(-pow((t - 0.72) / 0.043, 2.0))
	lantern.light_energy = 1.45 + charge * 1.5 + beat * 1.1
	if motif == "forge":
		hearth.light_energy = 1.25 + charge * 0.8 + 0.16 * sin(seconds * 11)
	if motif == "starfall":
		orrery.rotation = Vector3(0.11, t * 1.48, charge * 0.37)
	orbit.rotation.y = t * 0.74
	orbit.visible = motif != "phoenix"
	ring.scale = Vector3(0.48, 1.0, 0.48).lerp(Vector3(1.07, 1.0, 1.07), charge)
	crystal.position = Vector3(0, 1.64, -0.98)
	crystal.rotation = Vector3(t * 0.8, t * 3.6, t * 0.65)
	crystal.scale = Vector3.ONE * lerpf(0.08, 0.8, charge) * (1.0 - release * 0.8)
	for index in fragments.size():
		var angle := index * TAU / fragments.size() + t * 1.3
		var radius := 0.78 + 0.19 * (index % 4)
		var item := fragments[index]
		item.position = Vector3(cos(angle) * radius, 0.32 + (index % 4) * 0.19, sin(angle) * radius)
		if motif == "starfall":
			var flight := smoothstep(0.0, 1.0, release + (index % 5) * 0.04 * release)
			item.position += Vector3(flight * 2.6, 0.91 + sin(flight * PI) * 1.28 - flight * 0.77, -0.98 + flight * 3.0)
		else:
			item.position += Vector3(cos(angle) * beat * 0.46, beat * (0.14 + index % 3 * 0.1), sin(angle) * beat * 0.4)
		item.rotation = Vector3(angle, -angle, t * 2.5)
		var size_factor := (0.58 + index % 3 * 0.17) * (0.25 + (charge if motif == "starfall" else beat) * 0.75)
		item.scale = Vector3(0.18, 0.50, 0.18) * size_factor
	for index in sparks.size():
		var lift := fposmod(seconds * 0.28 + float(index) / sparks.size(), 1.0)
		var angle := index * 2.39996 + seconds * 0.36
		var radius := 0.72 + (index % 5) * 0.21 + beat * 0.4
		sparks[index].position = Vector3(cos(angle) * radius, 0.15 + lift * (1.55 + beat * 0.5), sin(angle) * radius)
		sparks[index].scale = Vector3.ONE * (0.25 + sin(lift * PI) * 0.47)
		if motif == "phoenix":
			var trail := maxf(0.0, t - index * 0.0065)
			sparks[index].position = _phoenix_path(trail) + Vector3(cos(angle) * 0.13, 0.69 + lift * 0.36, sin(angle) * 0.23)
	for fragment in fragments:
		fragment.visible = motif != "phoenix"
	actor.modulate = Color(1, 1, 1, reveal)
	_animate_actor(t, seconds, beat)
	_shoot(shot_time, seconds, beat)
	_ground_shadows(reveal)
	shield_omen.visible = subject == "starfall" and seconds > 5.75 and seconds < 8.3
	if shield_omen.visible:
		shield_omen.position = actor.position + Vector3(0.10, 0.67, 0.08)
		shield_omen.rotation = Vector3(1.23, 0, t * 0.75)
		shield_omen.scale = Vector3.ONE * lerpf(0.15, 0.62, smoothstep(5.75, 6.4, seconds))

func _shoot(p: float, seconds: float, beat: float) -> void:
	var at := actor.position
	var focus := Vector3.ZERO
	match motif:
		"forge":
			match shot:
				0:
					camera.position = Vector3(2.1, 1.79, 4.95).lerp(Vector3(1.63, 1.64, 4.69), p)
					focus = Vector3(-0.35, 0.81, -0.45)
					camera.fov = 45
				1:
					camera.position = Vector3(1.05, 1.18, 3.14).lerp(Vector3(0.63, 1.16, 2.92), p)
					focus = at + Vector3(0.31, 0.82, -0.13)
					camera.fov = 39
				2:
					camera.position = Vector3(-1.36, 1.20, 4.38).lerp(Vector3(-0.76, 1.31, 3.93), p)
					focus = at + Vector3(0.0, 0.79, -0.15)
					camera.fov = 43
				_:
					var recoil := sin((seconds - 7.25) * 24.0) * beat * 0.035
					camera.position = Vector3(0.53, 1.13 + recoil, 4.02).lerp(Vector3(0.78, 1.18, 3.75), p)
					focus = at + Vector3(0.18, 1.09, -0.15)
					camera.fov = 42
		"phoenix":
			match shot:
				0:
					camera.position = Vector3(2.55, 2.93, 6.05).lerp(Vector3(1.63, 2.53, 5.65), p)
					focus = Vector3(-0.72, 0.43, -1.94)
					camera.fov = 52
				1:
					camera.position = at + Vector3(-0.62, 0.71, 3.54).lerp(Vector3(-0.48, 0.9, 3.93), p)
					focus = at + Vector3(0.12, 0.76, 0.0)
					camera.fov = 46
				2:
					camera.position = at + Vector3(0.24, 1.72, 4.32).lerp(Vector3(0.71, 1.89, 4.55), p)
					focus = at + Vector3(0.56, 0.32, -0.58)
					camera.fov = 52
				_:
					camera.position = Vector3(5.39, 2.52, 4.63).lerp(Vector3(5.93, 1.92, 4.88), p)
					focus = at + Vector3(0.12, 0.53, 0)
					camera.fov = 53
		"starfall":
			match shot:
				0:
					camera.position = Vector3(2.68, 2.38, 5.56).lerp(Vector3(1.98, 2.15, 5.32), p)
					focus = Vector3(0.02, 1.15, -0.66)
					camera.fov = 46
				1:
					camera.position = Vector3(-2.27, 1.88, 3.52).lerp(Vector3(-1.85, 1.91, 3.22), p)
					focus = Vector3(0, 1.66, -0.99)
					camera.fov = 40
				2:
					camera.position = Vector3(-1.90, 1.36, 4.37).lerp(Vector3(-0.99, 1.38, 4.30), p)
					focus = Vector3(-0.46, 1.27, -0.51)
					camera.fov = 43
				_:
					camera.position = Vector3(1.36, 2.34, 4.85).lerp(Vector3(2.48, 2.96, 4.98), p)
					focus = Vector3(0.26 + p * 0.54, 1.55 - p * 0.37, -0.66)
					camera.fov = 49
	camera.look_at(focus)

func _animate_actor(t: float, seconds: float, beat: float) -> void:
	var stride := smoothstep(0.50, 0.72, t)
	var ground := Forge.DAIS_Y + 0.008
	actor.position = Vector3(lerpf(-0.42, 0.23, stride), ground - 0.023, lerpf(-0.20, 0.13, stride))
	actor.scale = Vector3(1.0 + beat * 0.022, 1.0 + sin(seconds * 8.5) * 0.014 - beat * 0.034, 1.0)
	if subject == Creature.SUBJECT:
		var pose := 0 if t < 0.50 else 1 if t < 0.72 else 2
		if rendered_frame != pose:
			rendered_frame = pose
			actor.texture = Creature.pose(pose)
			actor.offset = Creature.planted_offset(pose)
	if motif == "phoenix":
		actor.position = _phoenix_path(t)
		var frame := Art.animation_frame("phoenix", seconds * (0.8 if t < 0.23 else 3.1))
		if rendered_frame != frame:
			rendered_frame = frame
			actor.texture = Art.form_texture("phoenix", frame)
			actor.offset.y = actor.texture.get_height() * 0.44
	elif motif == "starfall":
		actor.position = Vector3(lerpf(-1.03, -0.64, stride), ground - 0.023, lerpf(0.40, 0.07, stride))
		actor.scale *= 0.83
	lantern.position = actor.position + Vector3(0.52, 1.52, 1.28)

func _phoenix_path(t: float) -> Vector3:
	var perch := Vector3(-1.15, Forge.DAIS_Y + 0.008, 0.24)
	var crest := Vector3(1.45, 2.55, -2.10)
	if t < 0.23:
		return perch
	if t < 0.76:
		var climb := inverse_lerp(0.23, 0.76, t)
		return perch.bezier_interpolate(Vector3(-0.87, 0.18, -0.66), Vector3(0.20, 2.72, -3.21), crest, climb)
	var dive := inverse_lerp(0.76, 1.0, t)
	return crest.bezier_interpolate(Vector3(2.07, 2.43, -1.33), Vector3(4.31, 1.37, 0.34), Vector3(6.67, 0.11, 2.21), dive)

func _ground_shadows(reveal: float) -> void:
	var ground := Forge.DAIS_Y + 0.008
	contact.visible = motif != "phoenix" or actor.position.x < 0.24
	contact.position = Vector3(actor.position.x, ground, actor.position.z + 0.023)
	var separation := maxf(0.0, actor.position.y - ground)
	var shadow_size := 1.0 + separation * 0.23
	contact.scale = Vector3(shadow_size, 1.0, shadow_size)
	contact.material_override.set_shader_parameter("strength", clampf(0.60 - separation * 0.39, 0.10, 0.60) * reveal)
	var paw_offsets: Array[float] = []
	if subject == Creature.SUBJECT:
		paw_offsets = Creature.contact_offsets(rendered_frame)
	var right := camera.global_basis.x
	for index in paw_shadows.size():
		var shadow := paw_shadows[index]
		shadow.visible = index < paw_offsets.size()
		if shadow.visible:
			var foot := actor.position + right * float(paw_offsets[index]) * actor.scale.x
			shadow.position = Vector3(foot.x, ground + 0.002, foot.z)
			shadow.material_override.set_shader_parameter("strength", reveal * 0.94)

func stop() -> void:
	if not visible:
		return
	hide()
	viewport.render_target_update_mode = SubViewport.UPDATE_DISABLED

func _rule(at: Vector2, extent: Vector2) -> void:
	var rule := ColorRect.new()
	rule.position = at
	rule.size = extent
	rule.color = Color("9e7953")
	rule.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(rule)

func _caption(rect: Rect2, font_size: int) -> Label:
	var label := Label.new()
	label.position = rect.position
	label.size = rect.size
	label.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
	label.add_theme_color_override("font_color", Color("e3cfaf"))
	label.add_theme_font_size_override("font_size", font_size)
	label.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(label)
	return label

func _build_stage() -> void:
	viewport = SubViewport.new()
	viewport.size = Vector2i(768, 329)
	viewport.msaa_3d = Viewport.MSAA_2X
	viewport.own_world_3d = true
	viewport.render_target_update_mode = SubViewport.UPDATE_DISABLED
	add_child(viewport)
	stage = Node3D.new()
	viewport.add_child(stage)
	var world_environment := WorldEnvironment.new()
	atmosphere = Environment.new()
	atmosphere.background_mode = Environment.BG_COLOR
	atmosphere.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	atmosphere.ambient_light_energy = 0.63
	world_environment.environment = atmosphere
	stage.add_child(world_environment)
	camera = Camera3D.new()
	camera.fov = 45
	stage.add_child(camera)
	camera.current = true
	var rim := DirectionalLight3D.new()
	rim.rotation_degrees = Vector3(-48, -32, 0)
	rim.light_color = Color("a9c2e5")
	rim.light_energy = 0.47
	stage.add_child(rim)
	lantern = OmniLight3D.new()
	lantern.position = Vector3(0.54, 1.65, 1.28)
	lantern.omni_range = 5.0
	stage.add_child(lantern)
	contact = Forge.contact_shadow(stage)
	for index in 3:
		var paw := Forge.contact_shadow(stage)
		paw.scale = Vector3(0.17, 1.0, 0.29)
		paw_shadows.append(paw)
	var rim_mesh := TorusMesh.new()
	rim_mesh.inner_radius = 1.01
	rim_mesh.outer_radius = 1.04
	rim_mesh.rings = 40
	rim_mesh.ring_segments = 6
	energy = Forge.material(Color("ffd09d"))
	energy.emission_enabled = true
	energy.emission_energy_multiplier = 0.72
	orbit = Node3D.new()
	stage.add_child(orbit)
	ring = Forge.place(orbit, rim_mesh, energy, Vector3(0, Forge.DAIS_Y + 0.018, 0))
	shield_omen = Forge.place(stage, rim_mesh, energy, Vector3.ZERO)
	var glyph := BoxMesh.new()
	glyph.size = Vector3(0.035, 0.012, 0.09)
	for index in 16:
		var angle := index * TAU / 16.0
		var rune := Forge.place(orbit, glyph, energy, Vector3(sin(angle) * 1.10, Forge.DAIS_Y + 0.010, cos(angle) * 1.10))
		rune.rotation.y = angle
	shard = SphereMesh.new()
	shard.radius = 0.26
	shard.height = 0.78
	shard.radial_segments = 5
	shard.rings = 2
	crystal = Forge.place(stage, shard, energy, Vector3(0, 1.30, 0))
	for index in 10:
		fragments.append(Forge.place(stage, shard, energy, Vector3.ZERO))
	var spark := SphereMesh.new()
	spark.radius = 0.019
	spark.height = 0.052
	spark.radial_segments = 5
	spark.rings = 2
	for index in 21:
		sparks.append(Forge.place(stage, spark, energy, Vector3.ZERO))
	actor = Sprite3D.new()
	actor.billboard = BaseMaterial3D.BILLBOARD_FIXED_Y
	actor.shaded = false
	stage.add_child(actor)
