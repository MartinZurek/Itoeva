extends Control

const Cards = preload("res://scripts/card_catalog.gd")
const Art = preload("res://scripts/card_assets.gd")
const Forge = preload("res://scripts/practice_forge_stage.gd")
const Creature = preload("res://scripts/practice_creature_art.gd")
const CUT_END := 2.83
const FILM_SIZE := Vector2(1280, 720)
const SHOT := Rect2(0, 78, 1280, 548)
const COLORS = {"phoenix": Color("ffa45f"), "starfall": Color("a7d8ff"), "forge": Color("e6b673")}
const CHAPTERS = {
	"phoenix": ["Glut unter der Asche", "Die Flügel öffnen sich", "Sturzflug"],
	"starfall": ["Sterne sammeln sich", "Der Kern erwacht", "Sternenregen"],
	"forge": ["Glut in den Fugen", "Das Wesen richtet sich auf", "Die Esse bebt"]
}

var viewport: SubViewport
var camera: Camera3D
var actor: Sprite3D
var lantern: OmniLight3D
var hearth: OmniLight3D
var contact: MeshInstance3D
var paw_shadows: Array[MeshInstance3D] = []
var orbit: Node3D
var ring: MeshInstance3D
var crystal: MeshInstance3D
var shard: SphereMesh
var fragments: Array[MeshInstance3D] = []
var sparks: Array[MeshInstance3D] = []
var energy: StandardMaterial3D
var back: ColorRect
var film: TextureRect
var title: Label
var chapter: Label
var subject := ""
var motif := "forge"
var tint := Color.WHITE
var rendered_frame := -1

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
	title = _caption(Rect2(42, 15, 1080, 51), 32)
	chapter = _caption(Rect2(42, 644, 945, 48), 22)
	hide()

func start(id: String) -> void:
	subject = id
	motif = id if id in ["phoenix", "starfall"] else "forge"
	tint = COLORS[motif]
	var card := Cards.get_card(id)
	title.text = str(card.name).to_upper()
	title.add_theme_color_override("font_color", tint.lightened(0.26))
	energy.albedo_color = tint.darkened(0.26)
	energy.emission = tint.darkened(0.40)
	lantern.light_color = tint
	actor.texture = Creature.pose(0) if subject == Creature.SUBJECT else Art.form_texture("phoenix") if motif == "phoenix" else Art.texture(card)
	actor.pixel_size = Creature.PIXEL_SIZE if subject == Creature.SUBJECT else 1.67 / maxf(1.0, float(actor.texture.get_height()))
	actor.offset = Creature.planted_offset(0) if subject == Creature.SUBJECT else Vector2(0, actor.texture.get_height() * 0.44)
	actor.visible = motif != "starfall"
	crystal.visible = motif == "starfall"
	rendered_frame = -1
	tick(0.0)

func tick(seconds: float) -> void:
	if subject.is_empty() or seconds >= CUT_END:
		stop()
		return
	show()
	viewport.render_target_update_mode = SubViewport.UPDATE_ALWAYS
	var t := clampf(seconds / CUT_END, 0.0, 1.0)
	modulate.a = smoothstep(0.0, 0.12, seconds) * (1.0 - smoothstep(CUT_END - 0.24, CUT_END, seconds))
	var charge := smoothstep(0.11, 0.48, t)
	var reveal := smoothstep(0.12, 0.31, t)
	var release := smoothstep(0.70, 1.0, t)
	var beat := exp(-pow((t - 0.68) / 0.075, 2.0))
	chapter.text = CHAPTERS[motif][0 if t < 0.36 else 1 if t < 0.67 else 2]
	lantern.light_energy = 1.45 + charge * 1.5 + beat * 1.1
	hearth.light_energy = (0.65 if motif == "starfall" else 1.65) + 0.19 * sin(seconds * 11)
	orbit.rotation.y = t * 0.74
	ring.scale = Vector3(0.48, 1.0, 0.48).lerp(Vector3(1.07, 1.0, 1.07), charge)
	crystal.rotation = Vector3(t * 0.8, t * 2.4, t * 0.65)
	crystal.scale = Vector3.ONE * lerpf(0.15, 0.75, reveal) * (1.0 - release * 0.8)
	for index in fragments.size():
		var angle := index * TAU / fragments.size() + t * 1.3
		var radius := 0.78 + 0.19 * (index % 4)
		var item := fragments[index]
		item.position = Vector3(cos(angle) * radius, 0.32 + (index % 4) * 0.19, sin(angle) * radius)
		if motif == "starfall":
			var flight := smoothstep(0.0, 1.0, release + (index % 5) * 0.04 * release)
			item.position += Vector3(flight * 3.4, 0.65 + sin(flight * PI) * 0.96 - flight * 0.9, flight * 1.1)
		else:
			item.position += Vector3(cos(angle) * beat * 0.46, beat * (0.14 + index % 3 * 0.1), sin(angle) * beat * 0.4)
		item.rotation = Vector3(angle, -angle, t * 2.5)
		var size_factor := (0.58 + index % 3 * 0.17) * (0.25 + (charge if motif == "starfall" else beat) * 0.75)
		item.scale = Vector3(0.18, 0.50, 0.18) * size_factor
	for index in sparks.size():
		var lift := fposmod(t * 0.85 + float(index) / sparks.size(), 1.0)
		var angle := index * 2.39996 + t * 1.4
		var radius := 0.72 + (index % 5) * 0.21 + beat * 0.4
		sparks[index].position = Vector3(cos(angle) * radius, 0.15 + lift * (1.55 + beat * 0.5), sin(angle) * radius)
		sparks[index].scale = Vector3.ONE * (0.25 + sin(lift * PI) * 0.47)
	actor.modulate = Color(1, 1, 1, reveal)
	var approach := smoothstep(0.22, 0.92, t)
	var recoil := sin((t - 0.68) * 47.0) * beat * 0.028
	camera.position = Vector3(lerpf(1.85, 0.30, approach), lerpf(1.73, 1.27, approach) + recoil, lerpf(4.78, 4.03, approach))
	camera.look_at(Vector3(0.10 + release * (0.58 if motif == "phoenix" else 0.15), 0.98 + charge * 0.13, -0.37))
	_animate_actor(t, seconds, beat, reveal)

func _animate_actor(t: float, seconds: float, beat: float, reveal: float) -> void:
	var stride := smoothstep(0.36, 0.64, t)
	var ground := Forge.DAIS_Y + 0.008
	actor.position = Vector3(lerpf(-0.42, 0.23, stride), ground - 0.023, lerpf(-0.20, 0.13, stride))
	actor.scale = Vector3(1.0 + beat * 0.022, 1.0 + sin(seconds * 8.5) * 0.014 - beat * 0.034, 1.0)
	if subject == Creature.SUBJECT:
		var pose := 0 if t < 0.39 else 1 if t < 0.64 else 2
		if rendered_frame != pose:
			rendered_frame = pose
			actor.texture = Creature.pose(pose)
			actor.offset = Creature.planted_offset(pose)
	if motif == "phoenix":
		var fly := smoothstep(0.43, 0.99, t)
		actor.position = Vector3(lerpf(-0.60, 2.35, fly), ground + sin(fly * PI) * 1.11, lerpf(-0.15, 1.20, fly))
		var frame := Art.animation_frame("phoenix", seconds * (1.1 if t < 0.43 else 3.8))
		if rendered_frame != frame:
			rendered_frame = frame
			actor.texture = Art.form_texture("phoenix", frame)
			actor.offset.y = actor.texture.get_height() * 0.44
	contact.visible = motif != "starfall"
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
	var stage := Node3D.new()
	viewport.add_child(stage)
	var atmosphere := WorldEnvironment.new()
	var environment := Environment.new()
	environment.background_mode = Environment.BG_COLOR
	environment.background_color = Color("18141a")
	environment.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	environment.ambient_light_color = Color("aeabc4")
	environment.ambient_light_energy = 0.63
	atmosphere.environment = environment
	stage.add_child(atmosphere)
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
	hearth = Forge.build(stage)
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
