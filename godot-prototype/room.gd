extends Node3D
## Das Wohnzimmer, ganz aus Grundformen gebaut; Texturen entstehen beim Start als kleine
## Pixelbilder (64 px je Meter), damit Boden, Tapete und Teppich zur Pixel-Art der Wesen passen.
##
## Raum: x von -W/2 bis W/2 (links/rechts), z von -D/2 (hinten, Fensterwand) bis D/2 (vorn).

const W := 8.0
const D := 6.0
const H := 3.0
const T := 0.2  # Wandstaerke

## Waende, die sich vor die Kamera schieben koennen: {node, mitte, normale (nach innen)}.
var walls: Array = []

var _wood_dark: StandardMaterial3D
var _wood: StandardMaterial3D


func _ready() -> void:
	_wood = _flat(Color(0.55, 0.34, 0.2))
	_wood_dark = _flat(Color(0.33, 0.19, 0.11))
	_floor()
	_walls()
	_window()
	_ceiling()
	_furniture()
	_dust()


# --- Texturen --------------------------------------------------------------------------

static func _tex(img: Image) -> ImageTexture:
	return ImageTexture.create_from_image(img)


func _planks() -> Image:
	var img := Image.create_empty(64, 64, false, Image.FORMAT_RGBA8)
	var rng := RandomNumberGenerator.new()
	rng.seed = 7
	for row in 4:
		var seam := rng.randi_range(8, 56)
		var base := Color(0.62, 0.4, 0.24).darkened(rng.randf_range(0.0, 0.18))
		for y in range(row * 16, row * 16 + 16):
			for x in 64:
				var c := base
				# Maserung: lange, leicht wellige Linien
				var g := sin(x * 0.21 + row * 3.0 + sin(y * 0.9) * 1.5)
				if g > 0.86:
					c = c.darkened(0.12)
				if y == row * 16:
					c = Color(0.24, 0.13, 0.08)
				elif y == row * 16 + 1:
					c = c.lightened(0.1)
				if x == seam:
					c = Color(0.3, 0.17, 0.1)
				img.set_pixel(x, y, c)
	return img


func _wallpaper() -> Image:
	var img := Image.create_empty(32, 32, false, Image.FORMAT_RGBA8)
	var base := Color(0.55, 0.62, 0.5)
	for y in 32:
		for x in 32:
			var c := base
			if x % 16 == 0:
				c = base.darkened(0.1)
			elif x % 16 == 8 and (y % 8 == 0 or y % 8 == 1):
				c = Color(0.85, 0.72, 0.45)  # kleine Blueten
			elif x % 16 == 7 or x % 16 == 9:
				if y % 8 == 0:
					c = Color(0.78, 0.6, 0.38)
			img.set_pixel(x, y, c)
	return img


func _rug() -> Image:
	var img := Image.create_empty(64, 44, false, Image.FORMAT_RGBA8)
	var red := Color(0.62, 0.22, 0.17)
	var cream := Color(0.9, 0.8, 0.6)
	var teal := Color(0.18, 0.42, 0.42)
	for y in 44:
		for x in 64:
			var e: int = min(min(x, 63 - x), min(y, 43 - y))
			var c := red
			if e < 2:
				c = cream
			elif e < 5:
				c = teal
			elif e == 5:
				c = cream
			else:
				var dx: int = abs(x - 32)
				var dy: int = abs(y - 22)
				var dd := dx + dy * 2
				if dd % 12 < 2:
					c = cream
				elif dd % 12 < 4:
					c = teal
			img.set_pixel(x, y, c)
	return img


func _sky() -> Image:
	var img := Image.create_empty(16, 32, false, Image.FORMAT_RGBA8)
	for y in 32:
		var t := y / 31.0
		var c := Color(0.45, 0.62, 0.85).lerp(Color(1.0, 0.75, 0.45), t)
		for x in 16:
			img.set_pixel(x, y, c)
	# ein paar Baumkronen unten
	for x in 16:
		var top := 24 + int(2.5 * sin(x * 1.3))
		for y in range(top, 32):
			img.set_pixel(x, y, Color(0.22, 0.38, 0.25).lerp(Color(0.12, 0.24, 0.16), (y - top) / 8.0))
	return img


func _blob() -> Image:
	var img := Image.create_empty(32, 32, false, Image.FORMAT_RGBA8)
	for y in 32:
		for x in 32:
			var d := Vector2(x - 15.5, y - 15.5).length() / 16.0
			img.set_pixel(x, y, Color(0.1, 0.05, 0.03, clamp(1.0 - d, 0.0, 1.0) * 0.55))
	return img


# --- Materialien und Bausteine ---------------------------------------------------------

static func _flat(c: Color) -> StandardMaterial3D:
	var m := StandardMaterial3D.new()
	m.albedo_color = c
	m.roughness = 0.85
	return m


static func _tiled(img: Image, per_meter: float) -> StandardMaterial3D:
	var m := StandardMaterial3D.new()
	m.albedo_texture = _tex(img)
	m.texture_filter = BaseMaterial3D.TEXTURE_FILTER_NEAREST_WITH_MIPMAPS
	m.uv1_triplanar = true
	m.uv1_world_triplanar = true
	m.uv1_scale = Vector3.ONE * per_meter
	m.roughness = 0.9
	return m


func box(size: Vector3, pos: Vector3, mat: Material, solid := false, parent: Node3D = self) -> MeshInstance3D:
	var mi := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = size
	mi.mesh = bm
	mi.material_override = mat
	mi.position = pos
	parent.add_child(mi)
	if solid:
		_collider(size, pos, parent)
	return mi


func _collider(size: Vector3, pos: Vector3, parent: Node3D = self) -> void:
	var body := StaticBody3D.new()
	var cs := CollisionShape3D.new()
	var bs := BoxShape3D.new()
	bs.size = size
	cs.shape = bs
	body.position = pos
	body.add_child(cs)
	parent.add_child(body)


func cyl(top: float, bottom: float, h: float, pos: Vector3, mat: Material, parent: Node3D = self) -> MeshInstance3D:
	var mi := MeshInstance3D.new()
	var cm := CylinderMesh.new()
	cm.top_radius = top
	cm.bottom_radius = bottom
	cm.height = h
	cm.radial_segments = 12
	mi.mesh = cm
	mi.material_override = mat
	mi.position = pos
	parent.add_child(mi)
	return mi


func ball(r: float, pos: Vector3, mat: Material, parent: Node3D = self) -> MeshInstance3D:
	var mi := MeshInstance3D.new()
	var sm := SphereMesh.new()
	sm.radius = r
	sm.height = r * 2.0
	sm.radial_segments = 10
	sm.rings = 6
	mi.mesh = sm
	mi.material_override = mat
	mi.position = pos
	parent.add_child(mi)
	return mi


# --- Raum -------------------------------------------------------------------------------

func _floor() -> void:
	box(Vector3(W, 0.1, D), Vector3(0, -0.05, 0), _tiled(_planks(), 1.0))
	var rug := MeshInstance3D.new()
	var pm := PlaneMesh.new()
	pm.size = Vector2(3.2, 2.2)
	rug.mesh = pm
	var m := StandardMaterial3D.new()
	m.albedo_texture = _tex(_rug())
	m.texture_filter = BaseMaterial3D.TEXTURE_FILTER_NEAREST_WITH_MIPMAPS
	m.roughness = 1.0
	rug.material_override = m
	rug.position = Vector3(-0.3, 0.006, 0.0)
	add_child(rug)


func _wall_piece(size: Vector3, pos: Vector3, normal: Vector3, paper: Material, panel: Material, group: Node3D) -> void:
	box(size, pos, paper, false, group)
	# Holzvertaefelung unten (1 m) und Fussleiste, je eine Spur in den Raum versetzt
	if pos.y - size.y / 2.0 < 0.01:
		var ph: float = min(1.0, size.y)
		var off := normal * 0.03
		var psize := size
		psize.y = ph
		if abs(normal.x) > 0.5:
			psize.x = T
		else:
			psize.z = T
		box(psize, Vector3(pos.x, ph / 2.0, pos.z) + off, panel, false, group)
		var rail := psize
		rail.y = 0.06
		box(rail, Vector3(pos.x, ph, pos.z) + off * 2.0, _wood_dark, false, group)


func _walls() -> void:
	var paper := _tiled(_wallpaper(), 2.0)
	var panel := _wood
	var y := H / 2.0
	# hinten (mit Fensteroeffnung x 0.6..2.4, y 0.9..2.3)
	var back := Node3D.new()
	add_child(back)
	var bz := -D / 2.0 - T / 2.0
	var n := Vector3(0, 0, 1)
	_wall_piece(Vector3(4.6, H, T), Vector3(-1.7, y, bz), n, paper, panel, back)
	_wall_piece(Vector3(1.6, H, T), Vector3(3.2, y, bz), n, paper, panel, back)
	_wall_piece(Vector3(1.8, 0.9, T), Vector3(1.5, 0.45, bz), n, paper, panel, back)
	_wall_piece(Vector3(1.8, 0.7, T), Vector3(1.5, 2.65, bz), n, paper, panel, back)
	_collider(Vector3(W, H, T), Vector3(0, y, bz))
	walls.append({"node": back, "center": Vector3(0, y, -D / 2.0), "normal": n})
	# vorn, links, rechts
	for spec in [
		[Vector3(W + 2 * T, H, T), Vector3(0, y, D / 2.0 + T / 2.0), Vector3(0, 0, -1)],
		[Vector3(T, H, D), Vector3(-W / 2.0 - T / 2.0, y, 0), Vector3(1, 0, 0)],
		[Vector3(T, H, D), Vector3(W / 2.0 + T / 2.0, y, 0), Vector3(-1, 0, 0)],
	]:
		var g := Node3D.new()
		add_child(g)
		_wall_piece(spec[0], spec[1], spec[2], paper, panel, g)
		_collider(spec[0], spec[1])
		walls.append({"node": g, "center": spec[1] - spec[2] * T / 2.0, "normal": spec[2]})


func _window() -> void:
	var z := -D / 2.0
	var g: Node3D = walls[0]["node"]  # gehoert zur Rueckwand und verschwindet mit ihr
	# Rahmen, Kreuz, Fensterbank
	box(Vector3(1.9, 0.08, 0.26), Vector3(1.5, 0.9, z), _wood_dark, false, g)
	box(Vector3(1.9, 0.08, 0.26), Vector3(1.5, 2.3, z), _wood_dark, false, g)
	box(Vector3(0.08, 1.48, 0.26), Vector3(0.6, 1.6, z), _wood_dark, false, g)
	box(Vector3(0.08, 1.48, 0.26), Vector3(2.4, 1.6, z), _wood_dark, false, g)
	box(Vector3(0.05, 1.4, 0.05), Vector3(1.5, 1.6, z - 0.05), _wood_dark, false, g)
	box(Vector3(1.8, 0.05, 0.05), Vector3(1.5, 1.65, z - 0.05), _wood_dark, false, g)
	box(Vector3(2.1, 0.06, 0.4), Vector3(1.5, 0.87, z + 0.12), _wood, false, g)
	# Blick nach draussen: leuchtende Flaeche hinter der Oeffnung, wirft keinen Schatten
	var sky := MeshInstance3D.new()
	var qm := QuadMesh.new()
	qm.size = Vector2(3.0, 2.4)
	sky.mesh = qm
	var m := StandardMaterial3D.new()
	m.albedo_texture = _tex(_sky())
	m.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	m.texture_filter = BaseMaterial3D.TEXTURE_FILTER_NEAREST
	sky.material_override = m
	sky.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	sky.position = Vector3(1.5, 1.6, z - 0.9)
	sky.set_meta("glass", true)
	g.add_child(sky)
	# Pflanze auf der Fensterbank
	cyl(0.1, 0.08, 0.16, Vector3(2.05, 0.98, z + 0.12), _flat(Color(0.7, 0.36, 0.22)), g)
	ball(0.14, Vector3(2.05, 1.14, z + 0.12), _flat(Color(0.3, 0.52, 0.28)), g)


func _ceiling() -> void:
	# Unsichtbar, haelt aber die Sonne ab: Licht faellt nur durchs Fenster.
	var c := box(Vector3(W + 1, 0.1, D + 1), Vector3(0, H + 0.05, 0), _flat(Color.WHITE))
	c.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_SHADOWS_ONLY


func _furniture() -> void:
	var terracotta := _flat(Color(0.66, 0.32, 0.22))
	var cushion := _flat(Color(0.82, 0.5, 0.32))
	var teal := _flat(Color(0.2, 0.44, 0.44))
	var cream := _flat(Color(0.9, 0.82, 0.62))
	# Sofa hinten links
	var sx := -2.0
	var sz := -2.4
	box(Vector3(2.4, 0.42, 0.95), Vector3(sx, 0.21, sz), terracotta)
	box(Vector3(2.4, 0.55, 0.22), Vector3(sx, 0.69, sz - 0.37), terracotta)
	box(Vector3(0.22, 0.62, 0.95), Vector3(sx - 1.1, 0.31, sz), terracotta)
	box(Vector3(0.22, 0.62, 0.95), Vector3(sx + 1.1, 0.31, sz), terracotta)
	box(Vector3(0.95, 0.12, 0.68), Vector3(sx - 0.5, 0.48, sz + 0.07), cushion)
	box(Vector3(0.95, 0.12, 0.68), Vector3(sx + 0.5, 0.48, sz + 0.07), cushion)
	box(Vector3(0.4, 0.34, 0.12), Vector3(sx - 0.7, 0.68, sz - 0.2), teal)
	_collider(Vector3(2.4, 0.9, 1.0), Vector3(sx, 0.45, sz))
	# Couchtisch
	box(Vector3(1.2, 0.06, 0.65), Vector3(-0.3, 0.42, -0.2), _wood)
	for lx in [-0.82, 0.22]:
		for lz in [-0.45, 0.05]:
			box(Vector3(0.06, 0.4, 0.06), Vector3(lx, 0.2, lz), _wood_dark)
	box(Vector3(0.3, 0.05, 0.22), Vector3(-0.5, 0.47, -0.2), cream)  # Buch
	cyl(0.05, 0.05, 0.1, Vector3(0.0, 0.5, -0.1), _flat(Color(0.85, 0.85, 0.8)))  # Tasse
	_collider(Vector3(1.2, 0.5, 0.65), Vector3(-0.3, 0.25, -0.2))
	# Bücherregal rechts
	var bx := W / 2.0 - 0.22
	var rg: Node3D = walls[3]["node"]  # steht an der rechten Wand und verschwindet mit ihr
	box(Vector3(0.4, 2.1, 1.5), Vector3(bx, 1.05, -1.0), _wood_dark, false, rg)
	var rng := RandomNumberGenerator.new()
	rng.seed = 3
	var colors := [Color(0.62, 0.22, 0.17), Color(0.2, 0.4, 0.42), Color(0.85, 0.7, 0.4), Color(0.4, 0.3, 0.5), Color(0.3, 0.45, 0.25)]
	for shelf in 4:
		var y := 0.12 + shelf * 0.5
		box(Vector3(0.36, 0.04, 1.4), Vector3(bx - 0.04, y, -1.0), _wood, false, rg)
		var z := -1.65
		while z < -0.4:
			var w := rng.randf_range(0.05, 0.11)
			var h := rng.randf_range(0.26, 0.4)
			box(Vector3(0.28, h, w), Vector3(bx - 0.08, y + 0.02 + h / 2.0, z + w / 2.0), _flat(colors[rng.randi() % colors.size()]), false, rg)
			z += w + 0.01
	_collider(Vector3(0.45, 2.1, 1.5), Vector3(bx, 1.05, -1.0))
	# Sessel vorn rechts
	box(Vector3(0.9, 0.4, 0.85), Vector3(2.4, 0.2, 1.5), teal)
	box(Vector3(0.9, 0.5, 0.18), Vector3(2.4, 0.65, 1.84), teal)
	box(Vector3(0.75, 0.1, 0.62), Vector3(2.4, 0.45, 1.42), cream)
	_collider(Vector3(0.95, 0.8, 0.9), Vector3(2.4, 0.4, 1.55))
	# Stehlampe neben dem Sofa, echtes Licht mit Schatten
	var lx := -3.55
	var lz := -2.55
	cyl(0.18, 0.2, 0.04, Vector3(lx, 0.02, lz), _wood_dark)
	cyl(0.025, 0.025, 1.5, Vector3(lx, 0.77, lz), _wood_dark)
	var shade := StandardMaterial3D.new()
	shade.albedo_color = Color(0.95, 0.8, 0.5)
	shade.emission_enabled = true
	shade.emission = Color(1.0, 0.72, 0.38)
	shade.emission_energy_multiplier = 1.4
	var sh := cyl(0.14, 0.26, 0.3, Vector3(lx, 1.62, lz), shade)
	sh.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	var lamp := OmniLight3D.new()
	lamp.light_color = Color(1.0, 0.72, 0.42)
	lamp.light_energy = 1.6
	lamp.omni_range = 5.5
	lamp.omni_attenuation = 1.2
	lamp.shadow_enabled = true
	lamp.position = Vector3(lx + 0.05, 1.5, lz + 0.05)
	add_child(lamp)
	_collider(Vector3(0.4, 1.8, 0.4), Vector3(lx, 0.9, lz))
	# Pflanzen
	for p in [Vector3(3.45, 0, -2.55), Vector3(-3.45, 0, 2.45)]:
		cyl(0.22, 0.17, 0.4, p + Vector3(0, 0.2, 0), _flat(Color(0.7, 0.36, 0.22)))
		ball(0.32, p + Vector3(0, 0.68, 0), _flat(Color(0.28, 0.5, 0.27)))
		ball(0.22, p + Vector3(0.15, 0.95, 0.05), _flat(Color(0.35, 0.58, 0.3)))
		ball(0.18, p + Vector3(-0.14, 0.9, -0.08), _flat(Color(0.24, 0.44, 0.24)))
		_collider(Vector3(0.5, 1.0, 0.5), p + Vector3(0, 0.5, 0))
	# Bilder an der Rueckwand
	for spec in [[Vector3(-2.6, 1.9, 0), Vector2(0.7, 0.5), Color(0.85, 0.6, 0.35)], [Vector3(-1.4, 2.0, 0), Vector2(0.45, 0.6), Color(0.35, 0.55, 0.6)]]:
		var c: Vector3 = spec[0]
		var s: Vector2 = spec[1]
		box(Vector3(s.x + 0.08, s.y + 0.08, 0.04), Vector3(c.x, c.y, -D / 2.0 + 0.02), _wood_dark, false, walls[0]["node"])
		box(Vector3(s.x, s.y, 0.02), Vector3(c.x, c.y, -D / 2.0 + 0.045), _flat(spec[2]), false, walls[0]["node"])


func _dust() -> void:
	# Staub im Lichtkegel des Fensters
	var p := CPUParticles3D.new()
	p.amount = 40
	p.lifetime = 8.0
	p.preprocess = 8.0
	p.emission_shape = CPUParticles3D.EMISSION_SHAPE_BOX
	p.emission_box_extents = Vector3(0.9, 0.9, 1.4)
	p.position = Vector3(1.5, 1.2, -1.6)
	p.direction = Vector3(0.2, 0.3, 0.1)
	p.spread = 180.0
	p.gravity = Vector3(0, -0.01, 0)
	p.initial_velocity_min = 0.02
	p.initial_velocity_max = 0.06
	var qm := QuadMesh.new()
	qm.size = Vector2(0.025, 0.025)
	var m := StandardMaterial3D.new()
	m.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	m.billboard_mode = BaseMaterial3D.BILLBOARD_ENABLED
	m.albedo_color = Color(1.0, 0.88, 0.6, 0.7)
	m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	m.blend_mode = BaseMaterial3D.BLEND_MODE_ADD
	qm.material = m
	p.mesh = qm
	add_child(p)


func blob_material() -> StandardMaterial3D:
	var m := StandardMaterial3D.new()
	m.albedo_texture = _tex(_blob())
	m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	m.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	return m


## Waende zwischen Kamera und Raum werden unsichtbar, werfen aber weiter Schatten
## (Puppenhaus-Schnitt).
func cut_away(cam_pos: Vector3) -> void:
	for w in walls:
		var outside: bool = (cam_pos - w["center"]).dot(w["normal"]) < 0.0
		for mi in (w["node"] as Node3D).get_children():
			if mi.has_meta("glass"):
				mi.visible = not outside
				continue
			(mi as GeometryInstance3D).cast_shadow = (
				GeometryInstance3D.SHADOW_CASTING_SETTING_SHADOWS_ONLY if outside
				else GeometryInstance3D.SHADOW_CASTING_SETTING_ON)
