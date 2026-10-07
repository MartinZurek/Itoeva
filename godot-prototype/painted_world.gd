extends Node2D
## One resident 2D panorama; the house opens as an anchored interior cutaway.
signal access_changed(inside: bool)
const COAST = preload("res://assets/painted/coast.png")
const LIVING = preload("res://assets/painted/living.png")
const W = 2172.0
const H = 724.0
const ENTRY = Vector2(124,472)
const ROOM_SCALE = .72
const ROOM_ORIGIN = ENTRY-Vector2(836,880)*ROOM_SCALE
const CELL = 12.0
const LANDMARKS = {
	"Wohnzimmer": ROOM_ORIGIN+Vector2(1120,735)*ROOM_SCALE,
	"Haustür": ENTRY+Vector2(0,26),
	"Strand": Vector2(800,505),
	"Steg": Vector2(1390,528),
	"Wald": Vector2(1900,538),
	"Wasser": Vector2(800,670),
}
var inside := false
var sorted: Node2D
var room_layer: Node2D
var room_shade: Polygon2D
var exterior_pieces: Array[Node2D] = []
var interior_pieces: Array[Node2D] = []
var grids: Array[AStarGrid2D] = []
var floors: Array[PackedVector2Array] = []
var obstacles: Array[Array] = [[],[]]
var loaded_images := 2
var state_changes := 0
var _t := 0.0

func _ready() -> void:
	var image := Sprite2D.new(); image.texture = COAST; image.centered = false
	image.z_index = -100; image.texture_filter = CanvasItem.TEXTURE_FILTER_LINEAR
	var water := ShaderMaterial.new(); water.shader = preload("res://shaders/painted_water.gdshader")
	image.material = water; add_child(image)
	sorted = Node2D.new(); sorted.y_sort_enabled = true; add_child(sorted)
	room_layer = Node2D.new(); room_layer.z_index = -50; add_child(room_layer)
	room_shade = Polygon2D.new()
	room_shade.polygon = PackedVector2Array([Vector2(-10000,-10000),Vector2(10000,-10000),Vector2(10000,10000),Vector2(-10000,10000)])
	room_shade.color = Color(.09,.07,.045,1); room_layer.add_child(room_shade)
	var room_image := Sprite2D.new(); room_image.texture = LIVING; room_image.centered = false
	room_image.position = ROOM_ORIGIN; room_image.scale = Vector2.ONE*ROOM_SCALE
	room_image.texture_filter = CanvasItem.TEXTURE_FILTER_LINEAR; room_layer.add_child(room_image)
	room_layer.visible = false
	floors.append(_poly([Vector2(.018,.64),Vector2(.10,.60),Vector2(.22,.70),Vector2(.29,.62),Vector2(.44,.64),Vector2(.52,.65),Vector2(.66,.68),Vector2(.73,.66),Vector2(.84,.66),Vector2(.985,.69),Vector2(.985,.966),Vector2(.018,.966)],Vector2(W,H)))
	floors.append(_room_poly([Vector2(110,525),Vector2(1538,525),Vector2(1538,833),Vector2(956,833),Vector2(956,906),Vector2(716,906),Vector2(716,833),Vector2(110,833)]))
	# Ground footprints, not the entire visible height of a painted object.
	_obstacle(0,Vector2(285,529),Vector2(112,20),0) # small pier railing
	_obstacle(0,Vector2(1460,570),Vector2(65,23),0)
	_obstacle(0,Vector2(1900,624),Vector2(72,21),0)
	_obstacle(0,Vector2(550,471),Vector2(46,15),0)
	_obstacle(0,Vector2(1080,521),Vector2(32,18),0)
	_obstacle(1,_rp(Vector2(805,562)),Vector2(220,18)*ROOM_SCALE,44*ROOM_SCALE)
	_obstacle(1,_rp(Vector2(683,576)),Vector2(111,20)*ROOM_SCALE,38*ROOM_SCALE)
	_obstacle(1,_rp(Vector2(442,565)),Vector2(110,16)*ROOM_SCALE,0)
	_obstacle(1,_rp(Vector2(1190,551)),Vector2(46,16)*ROOM_SCALE,0)
	# Original image pixels are drawn again only where foreground silhouettes belong.
	_piece(COAST,_poly([Vector2(.0,.86),Vector2(.07,.83),Vector2(.16,.85),Vector2(.25,.82),Vector2(.33,.88),Vector2(.38,.98),Vector2(.0,.98)],Vector2(W,H)),Vector2(0,706),false,true)
	_piece(COAST,_poly([Vector2(.79,.86),Vector2(.87,.82),Vector2(.985,.88),Vector2(.985,.98),Vector2(.78,.98)],Vector2(W,H)),Vector2(0,706),false,true)
	_piece(COAST,_poly([Vector2(.86,.71),Vector2(.92,.70),Vector2(.97,.75),Vector2(.98,.81),Vector2(.94,.84),Vector2(.85,.82)],Vector2(W,H)),Vector2(0,631),false)
	_piece(COAST,_poly([Vector2(.10,.68),Vector2(.19,.68),Vector2(.205,.765),Vector2(.155,.80),Vector2(.10,.765)],Vector2(W,H)),Vector2(0,579),false)
	_piece(COAST,_poly([Vector2(.60,.64),Vector2(.67,.64),Vector2(.685,.74),Vector2(.66,.79),Vector2(.60,.77)],Vector2(W,H)),Vector2(0,572),false)
	_room_piece([Vector2(590,460),Vector2(606,419),Vector2(665,391),Vector2(981,394),Vector2(1034,441),Vector2(1057,507),Vector2(1043,566),Vector2(594,566)],562)
	_room_piece([Vector2(579,519),Vector2(611,508),Vector2(762,510),Vector2(786,524),Vector2(776,544),Vector2(748,578),Vector2(615,578),Vector2(596,551)],583)
	_room_piece([Vector2(1123,389),Vector2(1194,351),Vector2(1272,379),Vector2(1280,513),Vector2(1260,557),Vector2(1137,557)],552,true)
	_room_piece([Vector2(191,357),Vector2(265,366),Vector2(282,541),Vector2(211,563),Vector2(191,535)],560)
	for mode in 2: grids.append(_build_grid(mode))

func _poly(points: Array, size: Vector2) -> PackedVector2Array:
	var p := PackedVector2Array(); for v in points: p.append(v*size)
	return p
func _rp(p: Vector2) -> Vector2: return ROOM_ORIGIN+p*ROOM_SCALE
func _room_poly(points: Array) -> PackedVector2Array:
	var p := PackedVector2Array(); for v in points: p.append(_rp(v))
	return p
func _room_piece(points: Array, baseline: float, wind: bool=false) -> void:
	var uv := PackedVector2Array(points)
	_piece(LIVING,_room_poly(points),_rp(Vector2(0,baseline)),true,wind,uv)

func _piece(texture: Texture2D, global_points: PackedVector2Array, anchor: Vector2, home: bool, wind: bool=false, uv: PackedVector2Array=PackedVector2Array()) -> void:
	var root := Node2D.new(); root.position = anchor; sorted.add_child(root)
	var shape := Polygon2D.new(); shape.texture = texture
	shape.texture_filter = CanvasItem.TEXTURE_FILTER_LINEAR
	var local := PackedVector2Array(); for p in global_points: local.append(p-anchor)
	shape.polygon = local; shape.uv = global_points if uv.is_empty() else uv
	if wind:
		var mat := ShaderMaterial.new(); mat.shader = preload("res://shaders/painted_wind.gdshader")
		mat.set_shader_parameter("anchor_y",anchor.y); mat.set_shader_parameter("amount",.8 if home else 1.4)
		shape.material = mat
	root.add_child(shape)
	if home: interior_pieces.append(root); root.visible = false
	else: exterior_pieces.append(root)

func _obstacle(mode: int, center: Vector2, radius: Vector2, top: float) -> void:
	obstacles[mode].append({"center":center,"radius":radius,"top":top})

func can_walk(p: Vector2, mode: int=-1, height: float=0, clearance: float=3) -> bool:
	if mode < 0: mode = 1 if inside else 0
	if not Geometry2D.is_point_in_polygon(p,floors[mode]): return false
	for o in obstacles[mode]:
		if o.top>0 and height>=o.top-1: continue
		var d: Vector2 = (p-o.center)/(o.radius+Vector2.ONE*clearance)
		if d.length_squared()<1: return false
	return true

func platform_height(p: Vector2, height: float) -> float:
	if not inside: return 0
	for o in obstacles[1]:
		if o.top>0 and height>=o.top-1 and ((p-o.center)/o.radius).length_squared()<1: return o.top
	return 0

func _build_grid(mode: int) -> AStarGrid2D:
	var g := AStarGrid2D.new()
	g.region = Rect2i(-44,-20,228,83); g.cell_size = Vector2.ONE*CELL
	g.diagonal_mode = AStarGrid2D.DIAGONAL_MODE_ONLY_IF_NO_OBSTACLES
	g.default_compute_heuristic = AStarGrid2D.HEURISTIC_OCTILE
	g.default_estimate_heuristic = AStarGrid2D.HEURISTIC_OCTILE
	g.update()
	for y in range(g.region.position.y,g.region.end.y):
		for x in range(g.region.position.x,g.region.end.x):
			var id := Vector2i(x,y); g.set_point_solid(id,not can_walk(g.get_point_position(id),mode,0,8))
	return g

func _nearest(g: AStarGrid2D, p: Vector2) -> Vector2i:
	var id := Vector2i(roundi(p.x/CELL),roundi(p.y/CELL))
	if g.is_in_boundsv(id) and not g.is_point_solid(id): return id
	for radius in range(1,26):
		var best := Vector2i(9999,9999); var distance := INF
		for y in range(-radius,radius+1):
			for x in range(-radius,radius+1):
				if abs(x)!=radius and abs(y)!=radius: continue
				var candidate := id+Vector2i(x,y)
				if not g.is_in_boundsv(candidate) or g.is_point_solid(candidate): continue
				var d := g.get_point_position(candidate).distance_squared_to(p)
				if d<distance: best = candidate; distance = d
		if best.x!=9999: return best
	return Vector2i(9999,9999)

func route(from: Vector2, to: Vector2) -> PackedVector2Array:
	if not Geometry2D.is_point_in_polygon(to,floors[1 if inside else 0]): return PackedVector2Array()
	var g := grids[1 if inside else 0]
	var start := _nearest(g,from); var end := _nearest(g,to)
	if start.x==9999 or end.x==9999: return PackedVector2Array()
	var path := g.get_point_path(start,end)
	if path.is_empty(): return path
	if can_walk(to): path.append(to)
	return path

func set_inside(value: bool) -> void:
	if inside==value: return
	inside = value; state_changes += 1
	room_layer.visible = inside
	for piece in interior_pieces: piece.visible = inside
	for piece in exterior_pieces: piece.visible = not inside
	access_changed.emit(inside)

func update_access(p: Vector2, velocity: Vector2) -> void:
	if not inside and absf(p.x-ENTRY.x)<40 and p.y<ENTRY.y+3 and p.y>ENTRY.y-36 and velocity.y<-.1:
		set_inside(true)
	elif inside and absf(p.x-ENTRY.x)<76 and p.y>ENTRY.y+13 and velocity.y>.1:
		set_inside(false)

func depth_scale(p: Vector2) -> float:
	if inside:
		var local := (p-ROOM_ORIGIN)/ROOM_SCALE
		return ROOM_SCALE*lerpf(.48,1.05,clampf((local.y-545)/355,0,1))
	return lerpf(.40,1.0,clampf((p.y-445)/254,0,1))

func water_depth(p: Vector2) -> float:
	if inside: return 0
	# The painted foreground sea has a curved shoreline and two solid wooden piers.
	if Rect2(140,475,260,110).has_point(p) or Rect2(1285,474,177,65).has_point(p): return 0
	var shore := 556.0+24*sin(p.x*.006)
	return clampf((p.y-shore)/140,0,1)

func place_name(p: Vector2) -> String:
	if inside: return "Wohnzimmer"
	if water_depth(p)>.1: return "Im Wasser"
	if p.x<420: return "Am Haus"
	if p.x<1150: return "Strand"
	if p.x<1550: return "Am Steg"
	return "Waldweg"
