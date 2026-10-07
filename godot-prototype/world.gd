extends Node3D
## Ein gemeinsamer 3D-Boden: Haus -> Gartenweg -> Strand -> flaches Wasser.
## Koordinaten und Zielnamen bleiben stabil; es gibt keine Richtungs-Teleports.

const RoomScript = preload("res://room.gd")
const HOUSE = Vector3(-6.0, .18, -3.0)
const LAND_BOUNDS = Rect2(-16, -12, 40, 34)
const WATER_LEVEL = -.06
const LANDMARKS = {
    "Wohnzimmer": Vector3(-5.4,.18,-1.8),
    "Haustür": Vector3(-6,.18,1.5),
    "Garten": Vector3(0,.18,4.5),
    "Strand": Vector3(8,.1,10.0),
    "Wasser": Vector3(8,-.58,16.0),
}
var room: Node3D
var navigation: NavigationRegion3D
var roof: Node3D
var house_view_open := false
var _wood: Material
var _stone: Material
var _roof_material: Material
var cutaway_details: Array[MeshInstance3D] = []
var _plant_materials: Array[ShaderMaterial] = []

func _ready() -> void:
    add_to_group("itoeva_navigation_source")
    _wood = material("wood",.65)
    _stone = material("limestone",.55)
    _roof_material = material("roof",.48)
    _ground()
    room = RoomScript.new()
    room.position = HOUSE
    add_child(room)
    _house_details()
    _garden()
    _coast()
    _bounds()
    _navigation()

static func ground_height(x: float, z: float) -> float:
    var shore = smoothstep(8.5,17.0,z + sin(x*.19)*.85 + sin(x*.6)*.28)
    return lerpf(.18,-.72,shore)

func is_inside(p: Vector3) -> bool:
    return absf(p.x-HOUSE.x) < 4.05 and absf(p.z-HOUSE.z) < 3.05

func place_name(p: Vector3) -> String:
    if is_inside(p): return "Wohnzimmer"
    if water_depth(p) > .08: return "Im flachen Wasser"
    if p.z > 8.5: return "Strand"
    return "Gartenweg"

func water_depth(p: Vector3) -> float:
    return maxf(0,WATER_LEVEL-ground_height(p.x,p.z)) if p.z > 11.0 else 0.0

static func material(name: String, tiling: float = 1.0) -> StandardMaterial3D:
    var m = StandardMaterial3D.new()
    m.albedo_texture = load("res://assets/materials/%s.webp" % name)
    m.texture_filter = BaseMaterial3D.TEXTURE_FILTER_LINEAR_WITH_MIPMAPS
    m.uv1_triplanar = true
    m.uv1_world_triplanar = true
    m.uv1_scale = Vector3.ONE*tiling
    m.roughness = .92
    return m

func box(size: Vector3, p: Vector3, mat: Material, solid: bool=false, parent: Node3D=self) -> MeshInstance3D:
    var m = MeshInstance3D.new()
    var mesh = BoxMesh.new(); mesh.size = size
    m.mesh = mesh; m.position = p; m.material_override = mat; parent.add_child(m)
    if solid:
        var body = StaticBody3D.new(); body.position = p
        var cs = CollisionShape3D.new(); var shape = BoxShape3D.new(); shape.size = size
        cs.shape = shape; body.add_child(cs); parent.add_child(body)
    return m

func cylinder(p: Vector3, radius: float, height: float, mat: Material, parent: Node3D=self) -> MeshInstance3D:
    var m = MeshInstance3D.new(); var mesh = CylinderMesh.new()
    mesh.top_radius = radius*.75; mesh.bottom_radius = radius; mesh.height = height; mesh.radial_segments = 12
    m.mesh = mesh; m.position = p; m.material_override = mat; parent.add_child(m)
    return m

func _ground() -> void:
    var st = SurfaceTool.new(); st.begin(Mesh.PRIMITIVE_TRIANGLES)
    var nx = 80; var nz = 68
    for iz in nz:
        for ix in nx:
            var x = LAND_BOUNDS.position.x + ix*.5
            var z = LAND_BOUNDS.position.y + iz*.5
            for v in [Vector2(x,z),Vector2(x+.5,z),Vector2(x,z+.5),Vector2(x+.5,z),Vector2(x+.5,z+.5),Vector2(x,z+.5)]:
                st.set_uv(v*.35); st.add_vertex(Vector3(v.x,ground_height(v.x,v.y),v.y))
    st.generate_normals()
    var mesh = st.commit(); var floor_mesh = MeshInstance3D.new(); floor_mesh.mesh = mesh
    var mat = ShaderMaterial.new(); mat.shader = preload("res://shaders/terrain.gdshader")
    mat.set_shader_parameter("paving",load("res://assets/materials/paving.webp"))
    mat.set_shader_parameter("sand",load("res://assets/materials/sand.webp"))
    floor_mesh.material_override = mat; add_child(floor_mesh)
    floor_mesh.create_trimesh_collision()

func _house_details() -> void:
    roof = Node3D.new(); roof.position = HOUSE; add_child(roof)
    # Zwei geneigte Dachflaechen statt eines Deckelquaders.
    var st = SurfaceTool.new(); st.begin(Mesh.PRIMITIVE_TRIANGLES)
    for side in [-1.0,1.0]:
        var a = Vector3(-4.65,3.08,side*3.6)
        var b = Vector3(4.65,3.08,side*3.6)
        var c = Vector3(-4.65,4.65,0)
        var d = Vector3(4.65,4.65,0)
        var points = [a,c,b,b,c,d] if side > 0 else [b,c,a,d,c,b]
        for v in points:
            st.set_uv(Vector2(v.x*.25,v.z*.25)); st.add_vertex(v)
    st.generate_normals(); var r = MeshInstance3D.new(); r.mesh = st.commit(); r.material_override = _roof_material; roof.add_child(r)
    for side in [-1.0,1.0]:
        box(Vector3(9.5,.15,.16),Vector3(0,3.03,side*3.6),_wood,false,roof)
    cutaway_details.append(box(Vector3(.5,2.0,.55),HOUSE+Vector3(-2.6,4.4,-.7),_stone))
    cutaway_details.append(box(Vector3(.68,.15,.74),HOUSE+Vector3(-2.6,5.4,-.7),_stone))
    # Veranda und stufenloser Anschluss an denselben Gartenboden.
    box(Vector3(4.8,.06,1.7),Vector3(-6,.18,.9),_stone)
    for x in [-8.1,-3.9]:
        cylinder(Vector3(x,1.48,1.2),.075,2.6,_wood)
    cutaway_details.append(box(Vector3(4.8,.16,1.7),Vector3(-6,2.83,.9),_roof_material))
    for x in [-8.0,-4.0]:
        var planter = material("limestone",.8)
        box(Vector3(.66,.5,.6),Vector3(x,.44,1.9),planter,true)
        _card(Vector3(x,.90,1.9),Vector2(.95,.7),1,.4)
    # Holzrahmen, Fensterlaeden und Fassadenbalken greifen die gemalten Haeuser auf.
    for x in [-10.08,-1.92]:
        box(Vector3(.13,3.0,.15),Vector3(x,1.7,-5.7),_wood)
        box(Vector3(.13,3.0,.15),Vector3(x,1.7,-.1),_wood)
    for x in [-8.5,-3.5]:
        box(Vector3(.6,1.25,.08),Vector3(x,1.65,.11),_wood)
    # Laterne neben der Tuer, mit eigener lokaler Lichtquelle.
    var lamp = OmniLight3D.new(); lamp.position = Vector3(-4.82,1.95,.3)
    lamp.light_color = Color(1,.77,.46); lamp.light_energy = .65; lamp.omni_range = 3; add_child(lamp)
    var bulb = RoomScript._flat(Color(1,.79,.4)); bulb.emission_enabled = true; bulb.emission = Color(1,.72,.3)
    box(Vector3(.15,.24,.14),lamp.position,bulb)

func _foliage(kind: int) -> ShaderMaterial:
    while _plant_materials.size() <= kind:
        var mat = ShaderMaterial.new(); mat.shader = preload("res://shaders/foliage.gdshader")
        var names = ["oak","shrub","flowers","dune"]
        mat.set_shader_parameter("leaves",load("res://assets/plants/%s.webp" % names[_plant_materials.size()]))
        mat.set_shader_parameter("wind",.06 if _plant_materials.size() != 0 else .035)
        _plant_materials.append(mat)
    return _plant_materials[kind]

func _card(p: Vector3, size: Vector2, kind: int, yaw: float=0.0, parent: Node3D=self) -> void:
    var m = MeshInstance3D.new(); var mesh = QuadMesh.new(); mesh.size = size
    m.mesh = mesh; m.position = p; m.rotation.y = yaw; m.material_override = _foliage(kind)
    parent.add_child(m)

func _tree(p: Vector3, height: float, seed_index: int) -> void:
    var ground = ground_height(p.x,p.z); p.y = ground
    cylinder(p+Vector3(0,height*.4,0),.14,height*.8,_wood)
    var rng = RandomNumberGenerator.new(); rng.seed = seed_index*137+5
    for i in 7:
        var y = height*.67 + rng.randf_range(-.35,.45)
        var off = Vector3(rng.randf_range(-.65,.65),y,rng.randf_range(-.55,.55))
        _card(p+off,Vector2(height*.85,height*.65),0,i*PI*.32)
    # Auch beim Drehen bleibt der Stamm ein physisches Hindernis.
    box(Vector3(.46,height*.65,.46),p+Vector3(0,height*.325,0),RoomScript._flat(Color(.27,.20,.12)),true).visible = false

func _garden() -> void:
    var rng = RandomNumberGenerator.new(); rng.seed = 81
    for i in 20:
        var p = Vector3(rng.randf_range(-14,21),0,rng.randf_range(-10,8))
        if p.distance_to(HOUSE) < 6.3 or absf(p.x-(-6+smoothstep(.8,8.3,p.z)*14)) < 2.3: continue
        _tree(p,rng.randf_range(3.2,4.8),i)
    for i in 130:
        var x = rng.randf_range(-14,22); var z = rng.randf_range(-10,10)
        var p = Vector3(x,ground_height(x,z),z)
        if is_inside(p) or p.distance_to(HOUSE) < 5.5 or absf(x-(-6+smoothstep(.8,8.3,z)*14)) < 1.6: continue
        var kind = 2 if i%3 != 0 else 1
        var size = Vector2(.45,.46) if kind == 2 else Vector2(1.2,.85)
        _card(p+Vector3(0,size.y*.5,0),size,kind,rng.randf_range(0,TAU))
        _card(p+Vector3(0,size.y*.5,0),size,kind,rng.randf_range(0,TAU))
    _bench(Vector3(3.4,.18,5.0))
    # Ein niedriger Brunnen ist im Garten sichtbar und bleibt ein klarer Umweg.
    for i in 10:
        var a = i*TAU/10
        box(Vector3(.55,.55,.28),Vector3(-1+cos(a)*.8,.45,7.4+sin(a)*.8),_stone,true)

func _bench(p: Vector3) -> void:
    for z in [-.22,.0,.22]: box(Vector3(2,.09,.15),p+Vector3(0,.48,z),_wood)
    for y in [.80,1.02]: box(Vector3(2,.13,.08),p+Vector3(0,y,-.34),_wood)
    for x in [-.75,.75]: box(Vector3(.1,.5,.55),p+Vector3(x,.25,0),_wood)
    box(Vector3(2,.55,.7),p+Vector3(0,.275,0),_wood,true).visible = false

func _coast() -> void:
    var water = MeshInstance3D.new(); var plane = PlaneMesh.new(); plane.size = Vector2(100,65)
    plane.subdivide_width = 70; plane.subdivide_depth = 50
    water.mesh = plane; water.position = Vector3(3,WATER_LEVEL,43.0)
    var mat = ShaderMaterial.new(); mat.shader = preload("res://shaders/water.gdshader")
    water.material_override = mat; water.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF; add_child(water)
    var rng = RandomNumberGenerator.new(); rng.seed = 71
    for i in 60:
        var x = rng.randf_range(-13,22); var z = rng.randf_range(8.0,11.8)
        if absf(x-8) < 2: continue
        var p = Vector3(x,ground_height(x,z)+.35,z)
        _card(p,Vector2(.72,.7),3,rng.randf_range(0,TAU))
        _card(p,Vector2(.72,.7),3,rng.randf_range(0,TAU))
    for i in 15:
        var x = rng.randf_range(-14,23); var z = rng.randf_range(9.8,17.0)
        if absf(x-8) < 3: continue
        var mesh = SphereMesh.new(); mesh.radius = rng.randf_range(.35,.85); mesh.height = mesh.radius*1.5
        mesh.radial_segments = 12; mesh.rings = 6
        var rock = MeshInstance3D.new(); rock.mesh = mesh; rock.material_override = _stone
        rock.position = Vector3(x,ground_height(x,z)+mesh.radius*.3,z); rock.rotation = Vector3(.2*i,.5*i,.1*i)
        add_child(rock)
        var body = StaticBody3D.new(); body.position = rock.position
        var cs = CollisionShape3D.new(); var sphere = SphereShape3D.new(); sphere.radius = mesh.radius*.8
        cs.shape = sphere; body.add_child(cs); add_child(body)
    # Ferne Inseln bilden Tiefe; kein flaches Bild ersetzt den begehbaren Nahbereich.
    for i in 5:
        var mountain = MeshInstance3D.new(); var mesh = SphereMesh.new(); mesh.radius = 5+i*.7; mesh.height = 9+i
        mesh.radial_segments = 18; mesh.rings = 8; mountain.mesh = mesh
        var distant = RoomScript._flat(Color(.30,.46,.46)); mountain.material_override = distant
        mountain.scale = Vector3(1.6,.65,1); mountain.position = Vector3(-25+i*15,-3,58+i*2); add_child(mountain)

func _bounds() -> void:
    # Fences on land and a buoy line in shallow water make the prototype limits visible.
    for edge in [-15.5,23.5]:
        for z in range(-11,12,2):
            cylinder(Vector3(edge,.63,z),.04,.9,_wood)
            box(Vector3(.06,.07,2.0),Vector3(edge,.75,z+1),_wood)
    for x in range(-15,24,2):
        cylinder(Vector3(x,.63,-11.5),.04,.9,_wood)
        box(Vector3(2,.07,.06),Vector3(x+1,.75,-11.5),_wood)
    for x in range(-14,24,3):
        var buoy = cylinder(Vector3(x,WATER_LEVEL+.09,17.8),.14,.24,RoomScript._flat(Color(.82,.37,.15)))
        buoy.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
    var rope = box(Vector3(39,.025,.025),Vector3(4,WATER_LEVEL+.13,17.8),RoomScript._flat(Color(.85,.77,.58)))
    rope.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
    var clear = RoomScript._flat(Color(.3,.4,.2))
    for spec in [[Vector3(40,4,.4),Vector3(4,1,-11.7)],[Vector3(40,4,.4),Vector3(4,1,18.0)],
        [Vector3(.4,4,34),Vector3(-15.7,1,5)],[Vector3(.4,4,34),Vector3(23.7,1,5)]]:
        box(spec[0],spec[1],clear,true).visible = false

func _navigation() -> void:
    var map = get_world_3d().navigation_map
    NavigationServer3D.map_set_cell_size(map,.15)
    NavigationServer3D.map_set_cell_height(map,.10)
    navigation = NavigationRegion3D.new(); add_child(navigation)
    var nav = NavigationMesh.new(); nav.agent_radius = .30; nav.agent_height = 1.1
    nav.cell_size = .15; nav.cell_height = .1; nav.agent_max_climb = .20
    nav.agent_max_slope = 35; nav.region_min_size = 1
    nav.geometry_parsed_geometry_type = NavigationMesh.PARSED_GEOMETRY_STATIC_COLLIDERS
    nav.geometry_source_geometry_mode = NavigationMesh.SOURCE_GEOMETRY_GROUPS_WITH_CHILDREN
    nav.geometry_source_group_name = "itoeva_navigation_source"
    navigation.navigation_mesh = nav
    navigation.bake_navigation_mesh(false)

func update_visibility(camera_pos: Vector3, avatar_pos: Vector3, dt: float) -> void:
    var local = avatar_pos-HOUSE
    var inside = is_inside(avatar_pos) or (absf(local.x)<1.9 and local.z>2.8 and local.z<4.7)
    house_view_open = inside
    room.cut_away(camera_pos-HOUSE,inside)
    for m in roof.get_children():
        if m is GeometryInstance3D:
            m.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_SHADOWS_ONLY if inside else GeometryInstance3D.SHADOW_CASTING_SETTING_ON
    for mesh in cutaway_details:
        mesh.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_SHADOWS_ONLY if inside else GeometryInstance3D.SHADOW_CASTING_SETTING_ON
    room.update_door(avatar_pos-HOUSE,dt)
