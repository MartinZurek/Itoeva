extends Node3D
## Begehbares Haus mit offener Tuer, fein gemalten Materialtexturen und echter Fensterbeleuchtung.
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
var door_hinge: Node3D
var door_angle := 0.0
var beams: Array[MeshInstance3D] = []
var pick_walls: Array[StaticBody3D] = []
var door_frames: Array[MeshInstance3D] = []


func _ready() -> void:
	_wood = _tiled(_planks(), .65)
	_wood_dark = _flat(Color(0.33, 0.19, 0.11))
	_floor()
	_walls()
	_window()
	_ceiling()
	_furniture()
	_beams()
	_dust()
	var fill := OmniLight3D.new(); fill.position = Vector3(0,2.3,-.7)
	fill.light_color = Color(1,.86,.70); fill.light_energy = .24; fill.omni_range = 7.0; fill.omni_attenuation = 1.3
	add_child(fill)


# --- Texturen --------------------------------------------------------------------------

static func _tex(img: Image) -> ImageTexture:
	return ImageTexture.create_from_image(img)


func _planks() -> Image:
	return (load("res://assets/materials/wood.webp") as Texture2D).get_image()

func _wallpaper() -> Image:
	return (load("res://assets/materials/plaster.webp") as Texture2D).get_image()


func _rug() -> Image:
	return (load("res://assets/materials/rug.webp") as Texture2D).get_image()


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
	m.texture_filter = BaseMaterial3D.TEXTURE_FILTER_LINEAR_WITH_MIPMAPS
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


func _collider(size: Vector3, pos: Vector3, parent: Node3D = self, pick_normal: Vector3=Vector3.ZERO) -> void:
	var body := StaticBody3D.new()
	var cs := CollisionShape3D.new()
	var bs := BoxShape3D.new()
	bs.size = size
	cs.shape = bs
	body.position = pos
	body.add_child(cs)
	parent.add_child(body)
	if pick_normal != Vector3.ZERO:
		body.set_meta("pick_normal",pick_normal)
		pick_walls.append(body)


func picking_exclusions(cam_local: Vector3, open_view: bool) -> Array[RID]:
	var exclusions: Array[RID] = []
	if open_view:
		for wall in pick_walls:
			if (cam_local-wall.position).dot(wall.get_meta("pick_normal"))<0:
				exclusions.append(wall.get_rid())
	return exclusions


func cyl(top: float, bottom: float, h: float, pos: Vector3, mat: Material, parent: Node3D = self) -> MeshInstance3D:
	var mi := MeshInstance3D.new()
	var cm := CylinderMesh.new()
	cm.top_radius = top
	cm.bottom_radius = bottom
	cm.height = h
	cm.radial_segments = 32
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
	sm.radial_segments = 24
	sm.rings = 12
	mi.mesh = sm
	mi.material_override = mat
	mi.position = pos
	parent.add_child(mi)
	return mi


func soft_box(size: Vector3, pos: Vector3, radius: float, mat: Material, parent: Node3D=self) -> MeshInstance3D:
	var base := BoxMesh.new()
	base.size = size
	base.subdivide_width = 8; base.subdivide_height = 8; base.subdivide_depth = 8
	var arrays := base.surface_get_arrays(0)
	var vertices: PackedVector3Array = arrays[Mesh.ARRAY_VERTEX]
	var normals: PackedVector3Array = arrays[Mesh.ARRAY_NORMAL]
	var inner := size*.5-Vector3.ONE*radius
	for i in vertices.size():
		var v := vertices[i]
		var core := Vector3(clampf(v.x,-inner.x,inner.x),clampf(v.y,-inner.y,inner.y),clampf(v.z,-inner.z,inner.z))
		var normal := (v-core).normalized()
		vertices[i] = core+normal*radius
		normals[i] = normal
	arrays[Mesh.ARRAY_VERTEX] = vertices; arrays[Mesh.ARRAY_NORMAL] = normals
	var mesh := ArrayMesh.new(); mesh.add_surface_from_arrays(Mesh.PRIMITIVE_TRIANGLES,arrays)
	var m := MeshInstance3D.new(); m.mesh = mesh; m.position = pos; m.material_override = mat
	parent.add_child(m); return m

func _fabric(name: String, tint: Color=Color.WHITE) -> StandardMaterial3D:
	var m := _tiled((load("res://assets/materials/%s.webp" % name) as Texture2D).get_image(),1.4)
	m.albedo_color = tint
	return m

func _plant(p: Vector3, size: Vector2, parent: Node3D=self) -> void:
	var m := ShaderMaterial.new(); m.shader = preload("res://shaders/foliage.gdshader")
	m.set_shader_parameter("leaves",load("res://assets/plants/shrub.webp"))
	m.set_shader_parameter("wind",.015)
	for angle in [0.0,PI/2]:
		var leaf := MeshInstance3D.new(); var mesh := QuadMesh.new(); mesh.size = size
		leaf.mesh = mesh; leaf.position = p; leaf.rotation.y = angle; leaf.material_override = m; parent.add_child(leaf)


# --- Raum -------------------------------------------------------------------------------

func _floor() -> void:
	box(Vector3(W, 0.1, D), Vector3(0, -0.05, 0), _tiled(_planks(), .5), true)
	var rug := MeshInstance3D.new()
	var pm := PlaneMesh.new()
	pm.size = Vector2(3.2, 2.2)
	rug.mesh = pm
	var m := StandardMaterial3D.new()
	m.albedo_texture = _tex(_rug())
	m.texture_filter = BaseMaterial3D.TEXTURE_FILTER_LINEAR_WITH_MIPMAPS
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
	_collider(Vector3(W, H, T), Vector3(0, y, bz),self,n)
	walls.append({"node": back, "center": Vector3(0, y, -D / 2.0), "normal": n})
	# Vordere Wand besitzt eine echte 1.8 m breite Oeffnung im Mesh UND in der Kollision.
	var front := Node3D.new(); add_child(front)
	for x in [-2.45, 2.45]:
		var size := Vector3(3.1,H,T); var p := Vector3(x,y,D/2.0+T/2.0)
		_wall_piece(size,p,Vector3(0,0,-1),paper,panel,front); _collider(size,p,self,Vector3(0,0,-1))
	var header := Vector3(1.8,.65,T); var hp := Vector3(0,2.675,D/2.0+T/2.0)
	_wall_piece(header,hp,Vector3(0,0,-1),paper,panel,front); _collider(header,hp,self,Vector3(0,0,-1))
	walls.append({"node":front,"center":Vector3(0,y,D/2.0),"normal":Vector3(0,0,-1)})
	for x in [-W/2.0-T/2.0,W/2.0+T/2.0]:
		var g := Node3D.new(); add_child(g)
		var size := Vector3(T,H,D); var p := Vector3(x,y,0)
		var normal := Vector3(1 if x<0 else -1,0,0)
		if x < 0:
			# Large arched side window, inspired by the painted living room.
			for spec in [[Vector3(T,H,1.0),Vector3(x,y,-2.5)],[Vector3(T,H,3.0),Vector3(x,y,1.5)],
				[Vector3(T,.75,2.0),Vector3(x,.375,-1.0)],[Vector3(T,.4,2.0),Vector3(x,2.8,-1.0)]]:
				_wall_piece(spec[0],spec[1],normal,paper,panel,g)
		else: _wall_piece(size,p,normal,paper,panel,g)
		_collider(size,p,self,normal)
		walls.append({"node":g,"center":p-normal*T/2.0,"normal":normal})
	for x in [-.95,.95]: door_frames.append(box(Vector3(.12,2.4,.25),Vector3(x,1.2,D/2.0),_wood_dark))
	door_frames.append(box(Vector3(2.0,.14,.25),Vector3(0,2.35,D/2.0),_wood_dark))
	door_hinge = Node3D.new(); door_hinge.position = Vector3(-.88,0,D/2.0+.16); add_child(door_hinge)
	box(Vector3(1.73,2.24,.07),Vector3(.865,1.12,0),_wood,false,door_hinge)
	for y0 in [.58,1.57]: box(Vector3(1.45,.72,.045),Vector3(.865,y0,.06),_wood_dark,false,door_hinge)
	ball(.04,Vector3(1.56,1.0,.10),_flat(Color(.83,.65,.31)),door_hinge)


func update_door(avatar_local: Vector3, dt: float) -> void:
	var opening := avatar_local.distance_to(Vector3(0,0,D/2.0)) < 2.5
	door_angle = lerpf(door_angle,-deg_to_rad(110.0) if opening else 0.0,1.0-exp(-dt*5.0))
	door_hinge.rotation.y = door_angle


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
	var side: Node3D = walls[2]["node"]
	box(Vector3(.28,.08,2.15),Vector3(-4,.75,-1),_wood,false,side)
	for z0 in [-2.0,0.0]: box(Vector3(.18,1.0,.09),Vector3(-4,1.25,z0),_wood_dark,false,side)
	for i in 48:
		var a := Vector3(-4,1.75+sin(i*PI/48)*.8,-1+cos(i*PI/48))
		var b := Vector3(-4,1.75+sin((i+1)*PI/48)*.8,-1+cos((i+1)*PI/48))
		var frame := cyl(.052,.052,a.distance_to(b)+.01,(a+b)*.5,_wood_dark,side)
		frame.quaternion = Quaternion(Vector3.UP,(b-a).normalized())
	box(Vector3(.1,1.78,.055),Vector3(-4,1.6,-1),_wood_dark,false,side)
	box(Vector3(.1,.06,1.92),Vector3(-4,1.70,-1),_wood_dark,false,side)
	var cloth := _fabric("linen")
	for z0 in [-2.13,.13]:
		for i in 4:
			soft_box(Vector3(.12,1.82,.10),Vector3(-3.91+(i%2)*.028,1.64,z0+(i-1.5)*.068),.044,cloth,side)
	# The arched corners are filled up to the wall header, not left as holes.
	var st := SurfaceTool.new(); st.begin(Mesh.PRIMITIVE_TRIANGLES)
	for i in 24:
		var z0 := -2.0+i/12.0; var z1 := z0+1.0/12
		var y0 := 1.75+sqrt(maxf(0,1-pow(z0+1,2)))*.8
		var y1 := 1.75+sqrt(maxf(0,1-pow(z1+1,2)))*.8
		for v in [Vector3(-4,y0,z0),Vector3(-4,2.6,z0),Vector3(-4,y1,z1),Vector3(-4,y1,z1),Vector3(-4,2.6,z0),Vector3(-4,2.6,z1)]:
			st.set_uv(Vector2(v.z,v.y)); st.add_vertex(v)
	st.generate_normals()
	var infill := MeshInstance3D.new(); infill.mesh = st.commit(); infill.material_override = _tiled(_wallpaper(),2)
	infill.material_override.cull_mode = BaseMaterial3D.CULL_DISABLED
	side.add_child(infill)
	# Die Fensteroeffnung zeigt jetzt dieselbe echte Landschaft wie die Haustuer.
	# Pflanze auf der Fensterbank
	cyl(0.1, 0.08, 0.16, Vector3(2.05, 0.98, z + 0.12), _flat(Color(0.7, 0.36, 0.22)), g)
	_plant(Vector3(2.05,1.22,z+.12),Vector2(.38,.38),g)


func _ceiling() -> void:
	# Unsichtbar, haelt aber die Sonne ab: Licht faellt nur durchs Fenster.
	var c := box(Vector3(W + 1, 0.1, D + 1), Vector3(0, H + 0.05, 0), _flat(Color.WHITE))
	c.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_SHADOWS_ONLY


func _furniture() -> void:
	var terracotta := _fabric("terracotta",Color(.93,.80,.72))
	var cushion := _fabric("terracotta",Color(1,.94,.82))
	var teal := _fabric("sage")
	var cream := _fabric("linen")
	# Rounded, upholstered sofa with rolled arms, seam piping and carved feet.
	var sx := -1.9
	var sz := -2.25
	soft_box(Vector3(2.75,.38,1.12),Vector3(sx,.36,sz),.13,terracotta)
	soft_box(Vector3(2.55,.72,.26),Vector3(sx,.90,sz-.44),.12,terracotta)
	for dx in [-1.23,1.23]:
		soft_box(Vector3(.30,.52,1.10),Vector3(sx+dx,.67,sz),.14,terracotta)
		var roll := cyl(.18,.18,1.06,Vector3(sx+dx,.88,sz),terracotta)
		roll.rotation.x = PI/2
	for dx in [-.57,.57]:
		soft_box(Vector3(1.06,.19,.83),Vector3(sx+dx,.59,sz+.05),.08,cushion)
		var back := soft_box(Vector3(1.05,.56,.19),Vector3(sx+dx,.96,sz-.27),.085,cushion)
		back.rotation.x = -.12
	for dx in [-1.08,1.08]:
		for dz in [-.38,.38]: cyl(.055,.08,.25,Vector3(sx+dx,.135,sz+dz),_wood_dark)
	for spec in [[-.82,teal],[.80,cream]]:
		var pillow := soft_box(Vector3(.43,.42,.19),Vector3(sx+spec[0],.93,sz-.04),.08,spec[1])
		pillow.rotation.z = -.2 if spec[0]<0 else .16
	_collider(Vector3(2.8,1.22,1.15),Vector3(sx,.61,sz))
	# Round wooden tea table with ring edging, tapered legs and pottery.
	cyl(.72,.72,.09,Vector3(-.3,.55,-.2),_wood)
	cyl(.735,.735,.025,Vector3(-.3,.51,-.2),_wood_dark)
	for a in [0.0,TAU/3,TAU*2/3]:
		var leg := cyl(.045,.075,.48,Vector3(-.3+cos(a)*.46,.26,-.2+sin(a)*.46),_wood_dark)
		leg.rotation.z = -.08*cos(a); leg.rotation.x = .08*sin(a)
	box(Vector3(.29,.045,.22),Vector3(-.55,.62,-.2),cream)
	box(Vector3(.30,.014,.23),Vector3(-.55,.65,-.2),teal)
	cyl(.065,.046,.11,Vector3(-.03,.65,-.12),cream)
	var handle := MeshInstance3D.new(); var ring := TorusMesh.new()
	ring.inner_radius = .025; ring.outer_radius = .039; ring.rings = 16; ring.ring_segments = 8
	handle.mesh = ring; handle.material_override = cream; handle.position = Vector3(.048,.66,-.12); handle.rotation.x = PI/2; add_child(handle)
	cyl(.16,.17,.025,Vector3(.08,.61,-.38),cream)
	_collider(Vector3(1.46,.58,1.46),Vector3(-.3,.29,-.2))
	# Bücherregal rechts
	var bx := W / 2.0 - 0.22
	var rg: Node3D = self  # The shelf remains visible when the wall is cut away.
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
	# A sage linen reading chair with soft arms and a cream cushion.
	soft_box(Vector3(1.0,.36,1.0),Vector3(2.4,.30,1.5),.14,teal)
	soft_box(Vector3(1.0,.66,.22),Vector3(2.4,.85,1.86),.10,teal)
	soft_box(Vector3(.73,.16,.71),Vector3(2.4,.54,1.42),.07,cream)
	for x in [1.96,2.84]: soft_box(Vector3(.20,.36,.97),Vector3(x,.66,1.47),.095,teal)
	for x in [2.04,2.76]:
		for z in [1.14,1.84]: cyl(.04,.055,.21,Vector3(x,.12,z),_wood_dark)
	_collider(Vector3(1.04,1.16,1.06),Vector3(2.4,.58,1.55))
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
	lamp.light_energy = .7
	lamp.omni_range = 4.2
	lamp.omni_attenuation = 1.2
	lamp.shadow_enabled = true
	lamp.position = Vector3(lx + 0.05, 1.5, lz + 0.05)
	add_child(lamp)
	_collider(Vector3(0.4, 1.8, 0.4), Vector3(lx, 0.9, lz))
	# Pflanzen
	for p in [Vector3(3.45, 0, -2.55), Vector3(-3.45, 0, 2.45)]:
		cyl(0.22, 0.17, 0.4, p + Vector3(0, 0.2, 0), _flat(Color(0.7, 0.36, 0.22)))
		_plant(p+Vector3(0,.91,0),Vector2(.93,1.10))
		_collider(Vector3(0.5, 1.0, 0.5), p + Vector3(0, 0.5, 0))
	# Bilder an der Rueckwand
	for spec in [[Vector3(-2.6, 1.9, 0), Vector2(0.7, 0.5), Color(0.85, 0.6, 0.35)], [Vector3(-1.4, 2.0, 0), Vector2(0.45, 0.6), Color(0.35, 0.55, 0.6)]]:
		var c: Vector3 = spec[0]
		var s: Vector2 = spec[1]
		box(Vector3(s.x + 0.08, s.y + 0.08, 0.04), Vector3(c.x, c.y, -D / 2.0 + 0.02), _wood_dark, false, walls[0]["node"])
		var art := StandardMaterial3D.new(); art.albedo_texture = load("res://assets/materials/painting.webp")
		var quad := MeshInstance3D.new(); var qm := QuadMesh.new(); qm.size = s
		quad.mesh = qm; quad.material_override = art; quad.position = Vector3(c.x,c.y,-D/2.0+.055)
		(walls[0]["node"] as Node3D).add_child(quad)


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
func cut_away(cam_pos: Vector3, inside: bool=true) -> void:
	var front_cut := inside and cam_pos.z>D/2
	for frame in door_frames:
		frame.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_SHADOWS_ONLY if front_cut else GeometryInstance3D.SHADOW_CASTING_SETTING_ON
	for part in door_hinge.get_children():
		part.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_SHADOWS_ONLY if front_cut else GeometryInstance3D.SHADOW_CASTING_SETTING_ON
	for beam in beams:
		beam.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_SHADOWS_ONLY if inside else GeometryInstance3D.SHADOW_CASTING_SETTING_ON
	for w in walls:
		var outside: bool = inside and (cam_pos - w["center"]).dot(w["normal"]) < 0.0
		for mi in (w["node"] as Node3D).get_children():
			if mi.has_meta("glass"):
				mi.visible = not outside
				continue
			(mi as GeometryInstance3D).cast_shadow = (
				GeometryInstance3D.SHADOW_CASTING_SETTING_SHADOWS_ONLY if outside
				else GeometryInstance3D.SHADOW_CASTING_SETTING_ON)


func _beams() -> void:
	for z in [-2.85,0.0,2.85]:
		var b := box(Vector3(8.15,.17,.18),Vector3(0,2.87,z),_wood)
		beams.append(b)
	for x in [-3.94,3.94]:
		box(Vector3(.14,2.85,.15),Vector3(x,1.425,-2.88),_wood,false,walls[0]["node"])
