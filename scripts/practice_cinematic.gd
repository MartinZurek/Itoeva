extends Control

const Cards = preload("res://scripts/card_catalog.gd")
const Art = preload("res://scripts/card_assets.gd")
const CUT_END := 2.12
const FILM_SIZE := Vector2(1280, 720)
const SHOT := Rect2(0, 78, 1280, 548)
const COLORS = {"phoenix": Color("ffa45f"), "starfall": Color("a7d8ff"), "forge": Color("e6b673")}
const BACKGROUNDS = {"phoenix": "res://assets/forest/arena.png",
	"starfall": "res://assets/world/coast-shore-v026.png", "forge": "res://assets/interiors/craft.png"}
const CHAPTERS = {
	"phoenix": ["Glut unter der Asche", "Der Phönix steigt auf", "Sturzflug"],
	"starfall": ["Sterne sammeln sich", "Das Siegel öffnet sich", "Sternenregen"],
	"forge": ["Die Esse erwacht", "Schwebende Werkstücke", "Das Meisterstück erhebt sich"]
}

var viewport: SubViewport
var camera: Camera3D
var actor: Sprite3D
var scenery: Sprite3D
var lantern: OmniLight3D
var orbit: Node3D
var ring: MeshInstance3D
var halo: MeshInstance3D
var crystal: MeshInstance3D
var shard: SphereMesh
var plate: BoxMesh
var brass: StandardMaterial3D
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
	scenery.texture = load(BACKGROUNDS[motif])
	scenery.pixel_size = 43.0 / scenery.texture.get_width()
	scenery.modulate = Color("694b46") if motif == "phoenix" else Color("505975") if motif == "starfall" else Color("67574e")
	actor.texture = Art.form_texture("phoenix") if motif == "phoenix" else Art.texture(card)
	var texture_height := maxf(1.0, float(actor.texture.get_height()))
	actor.pixel_size = (2.24 if motif == "phoenix" else 3.2) / texture_height
	actor.visible = motif != "starfall"
	crystal.visible = motif == "starfall"
	for index in fragments.size():
		var metal := motif == "forge" and index % 3 != 0
		fragments[index].mesh = plate if metal else shard
		fragments[index].material_override = brass if metal else energy
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
	var charge := smoothstep(0.10, 0.61, t)
	var reveal := smoothstep(0.42, 0.68, t)
	var release := smoothstep(0.72, 1.0, t)
	chapter.text = CHAPTERS[motif][0 if t < 0.38 else 1 if t < 0.75 else 2]
	lantern.light_energy = 1.1 + charge * 2.2 + release * 1.4
	orbit.rotation.y = t * (1.5 if motif == "forge" else 0.9)
	ring.scale = Vector3.ONE * lerpf(0.48, 1.13, charge)
	halo.rotation = Vector3(lerpf(0.06, 0.78, reveal), t * 1.4, 0.3)
	halo.scale = Vector3.ONE * lerpf(0.3, 1.0, charge) * (1.0 + release * 0.38)
	crystal.rotation = Vector3(t * 0.8, t * 2.4, t * 0.65)
	crystal.scale = Vector3.ONE * lerpf(0.3, 1.1, reveal) * (1.0 + release * 0.32)
	for index in fragments.size():
		var angle := index * TAU / fragments.size() + t * (2.8 if motif == "forge" else 1.8)
		var radius := lerpf(3.35, 0.95, charge) + release * 2.5
		var item := fragments[index]
		item.position = Vector3(cos(angle) * radius, 0.7 + (index % 4) * 0.55 + sin(angle * 2) * 0.3, sin(angle) * radius)
		if motif == "starfall":
			var fall := smoothstep(0.0, 1.0, release + (index % 5) * 0.04 * release)
			item.position += Vector3(fall * 4.1, sin(fall * PI) * 2.1 - fall * 1.7, fall * 1.2)
		item.rotation = Vector3(angle, -angle, t * 2.5)
		var size_factor := (0.72 + index % 3 * 0.17) * (1.0 - release * 0.32)
		item.scale = Vector3.ONE * size_factor if motif == "forge" else Vector3(0.38, 0.7, 0.38) * size_factor
	for index in sparks.size():
		var lift := fposmod(t * 1.25 + float(index) / sparks.size(), 1.0)
		var angle := index * 2.39996 + t * 1.4
		var radius := 1.6 + (index % 5) * 0.39 + release
		sparks[index].position = Vector3(cos(angle) * radius, 0.2 + lift * (3.4 + charge), sin(angle) * radius)
		sparks[index].scale = Vector3.ONE * (0.4 + sin(lift * PI) * 0.6)
	actor.modulate = Color(1, 1, 1, reveal)
	actor.position = Vector3(0, 1.6 + sin(t * 5.0) * 0.14, 0)
	actor.scale = Vector3.ONE * (0.88 + reveal * 0.22)
	if motif == "phoenix":
		actor.position = Vector3(lerpf(-1.5, 2.9, release), 1.10 + reveal * 1.30 - release * 0.92, release * 1.45)
		var frame := Art.animation_frame("phoenix", seconds * 2.6)
		if rendered_frame != frame:
			rendered_frame = frame
			actor.texture = Art.form_texture("phoenix", frame)
	camera.position = Vector3(lerpf(4.7, -0.75, t), lerpf(3.7, 2.55, t), lerpf(7.9, 7.0, reveal))
	camera.look_at(Vector3(release * (0.85 if motif == "phoenix" else 0.0), 1.45 + reveal * 0.25, 0))

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
	environment.background_color = Color("101725")
	environment.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	environment.ambient_light_color = Color("9eacd1")
	environment.ambient_light_energy = 0.42
	atmosphere.environment = environment
	stage.add_child(atmosphere)
	camera = Camera3D.new()
	camera.fov = 40
	stage.add_child(camera)
	camera.current = true
	var rim := DirectionalLight3D.new()
	rim.rotation_degrees = Vector3(-39, -32, 0)
	rim.light_color = Color("9abfe8")
	rim.light_energy = 0.72
	stage.add_child(rim)
	lantern = OmniLight3D.new()
	lantern.position = Vector3(0.8, 2.6, 2.0)
	lantern.omni_range = 9
	stage.add_child(lantern)

	var stone := _material(Color("333442"))
	brass = _material(Color("a67c50"))
	brass.metallic = 0.42
	brass.roughness = 0.36
	var floor_mesh := BoxMesh.new()
	floor_mesh.size = Vector3(18, 0.18, 16)
	_place(stage, floor_mesh, stone, Vector3(0, -0.25, 0))
	var pedestal := CylinderMesh.new()
	pedestal.top_radius = 2.26
	pedestal.bottom_radius = 2.55
	pedestal.height = 0.32
	pedestal.radial_segments = 32
	_place(stage, pedestal, stone, Vector3(0, -0.01, 0))
	var rim_mesh := TorusMesh.new()
	rim_mesh.inner_radius = 2.08
	rim_mesh.outer_radius = 2.24
	rim_mesh.rings = 32
	rim_mesh.ring_segments = 6
	_place(stage, rim_mesh, brass, Vector3(0, 0.17, 0))
	var column := CylinderMesh.new()
	column.top_radius = 0.28
	column.bottom_radius = 0.5
	column.height = 4.0
	column.radial_segments = 6
	var collar := CylinderMesh.new()
	collar.top_radius = 0.40
	collar.bottom_radius = 0.40
	collar.height = 0.085
	collar.radial_segments = 6
	for index in 8:
		var angle := 0.2 + index * PI / 7.0
		var at := Vector3(cos(angle) * 5.4, 1.85, -sin(angle) * 5.4 - 0.9)
		_place(stage, column, stone, at)
		_place(stage, collar, brass, at + Vector3(0, -1.53, 0))
		_place(stage, collar, brass, at + Vector3(0, 1.61, 0))
	scenery = Sprite3D.new()
	scenery.position = Vector3(0, 4.1, -8.2)
	scenery.shaded = false
	stage.add_child(scenery)

	energy = _material(Color("ffd09d"))
	energy.emission_enabled = true
	energy.emission_energy_multiplier = 0.72
	orbit = Node3D.new()
	stage.add_child(orbit)
	ring = _place(orbit, rim_mesh, energy, Vector3(0, 0.28, 0))
	var halo_mesh := TorusMesh.new()
	halo_mesh.inner_radius = 2.19
	halo_mesh.outer_radius = 2.24
	halo_mesh.rings = 40
	halo_mesh.ring_segments = 6
	halo = _place(stage, halo_mesh, energy, Vector3(0, 1.55, 0))
	var glyph := BoxMesh.new()
	glyph.size = Vector3(0.08, 0.035, 0.24)
	for index in 24:
		var angle := index * TAU / 24.0
		var rune := _place(orbit, glyph, energy, Vector3(sin(angle) * 2.43, 0.21, cos(angle) * 2.43))
		rune.rotation.y = angle
	shard = SphereMesh.new()
	shard.radius = 0.26
	shard.height = 0.95
	shard.radial_segments = 5
	shard.rings = 2
	crystal = _place(stage, shard, energy, Vector3(0, 2.15, 0))
	plate = BoxMesh.new()
	plate.size = Vector3(0.37, 0.16, 0.53)
	for index in 13:
		fragments.append(_place(stage, plate if index % 3 != 0 else shard, brass if index % 3 != 0 else energy, Vector3.ZERO))
	var spark := SphereMesh.new()
	spark.radius = 0.037
	spark.height = 0.10
	spark.radial_segments = 5
	spark.rings = 2
	for index in 26:
		sparks.append(_place(stage, spark, energy, Vector3.ZERO))
	actor = Sprite3D.new()
	actor.billboard = BaseMaterial3D.BILLBOARD_ENABLED
	actor.shaded = false
	stage.add_child(actor)

func _material(color: Color) -> StandardMaterial3D:
	var material := StandardMaterial3D.new()
	material.albedo_color = color
	return material

func _place(parent: Node3D, mesh: Mesh, material: Material, at: Vector3) -> MeshInstance3D:
	var piece := MeshInstance3D.new()
	piece.mesh = mesh
	piece.material_override = material
	piece.position = at
	parent.add_child(piece)
	return piece
