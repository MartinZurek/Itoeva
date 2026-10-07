extends Node2D
## Feet remain on the 2D ground plane; height is an independent jump/platform coordinate.
const SHEET = preload("res://assets/fennec_grid.png")
var landscape: Node2D
var sprite: Sprite2D
var manual := Vector2.ZERO
var target = null
var path := PackedVector2Array()
var path_index := 0
var velocity := Vector2.ZERO
var running := false
var blocked := false
var seated := false
var wading := false
var height := 0.0
var vertical_speed := 0.0
var visual_scale := .65
var facing := Vector2(1,0)
var _phase := 0.0
var _idle := 0.0
var _joy := -1.0
var _stuck := 0.0
var water_material: ShaderMaterial

func setup(world: Node2D) -> void:
	landscape = world
	sprite = Sprite2D.new(); sprite.texture = SHEET; sprite.hframes = 12; sprite.vframes = 12
	sprite.texture_filter = CanvasItem.TEXTURE_FILTER_NEAREST
	water_material = ShaderMaterial.new(); water_material.shader = preload("res://shaders/avatar_water.gdshader")
	sprite.material = water_material; add_child(sprite)

func go(point: Vector2, run: bool=true) -> bool:
	path = landscape.route(position,point)
	if path.is_empty(): target = null; blocked = true; return false
	path_index = 0; target = path[-1]; running = run; blocked = false; seated = false; _stuck = 0
	return true
func joy() -> void: _joy = 0; target = null; blocked = false; seated = false
func jump() -> void:
	if absf(vertical_speed)<1 and not wading:
		vertical_speed = 340; seated = false

func _physics_process(dt: float) -> void:
	if landscape==null: return
	var depth: float = landscape.water_depth(position)
	wading = depth>.08
	var speed := (195.0 if running else 100.0)*visual_scale*(.56 if wading else 1.0)
	var want := Vector2.ZERO
	if manual.length()>.12:
		target = null; seated = false; blocked = false; want = manual*speed
	elif target!=null:
		if position.distance_to(target)<3:
			target = null
		else:
			while path_index<path.size()-1 and position.distance_to(path[path_index])<5: path_index += 1
			var delta: Vector2 = path[path_index]-position
			want = delta.normalized()*minf(speed,delta.length()/maxf(dt,.001))
	velocity = velocity.move_toward(want,650*dt)
	var previous := position
	var candidate := position+velocity*dt
	if landscape.can_walk(candidate,-1,height): position = candidate
	else:
		var horizontal := position+Vector2(velocity.x*dt,0)
		var vertical := position+Vector2(0,velocity.y*dt)
		if landscape.can_walk(horizontal,-1,height): position = horizontal
		if landscape.can_walk(vertical,-1,height): position = vertical
	var actual := (position-previous)/maxf(dt,.001)
	if actual.length()>1: facing = actual.normalized()
	if want.length()>10 and actual.length()<1:
		_stuck += dt
		if _stuck>1.0: target = null; blocked = true
	else: _stuck = 0
	landscape.update_access(position,actual)
	var floor_height: float = landscape.platform_height(position,height)
	vertical_speed -= 950*dt
	height += vertical_speed*dt
	if height<floor_height: height = floor_height; vertical_speed = 0
	visual_scale = lerpf(visual_scale,landscape.depth_scale(position),1-exp(-dt*8))
	sprite.scale = Vector2.ONE*visual_scale
	sprite.position = Vector2(0,-62*visual_scale-height)
	water_material.set_shader_parameter("waterline",1.0-maxf(0,depth-height/85)*.43)
	_animate(actual.length(),dt)
	queue_redraw()

func _draw() -> void:
	var shade := .25/(1+height*.015)
	draw_set_transform(Vector2.ZERO,0,Vector2(visual_scale*30,visual_scale*9))
	draw_circle(Vector2.ZERO,1,Color(.12,.09,.045,shade))
	draw_set_transform(Vector2.ZERO,0,Vector2.ONE)
	if wading:
		draw_set_transform(Vector2.ZERO,0,Vector2(visual_scale*32,visual_scale*8))
		draw_arc(Vector2.ZERO,1,0,TAU,36,Color(.9,.94,.76,.45),.10,true)
		draw_set_transform(Vector2.ZERO,0,Vector2.ONE)

func _animate(speed: float, dt: float) -> void:
	var view := "side"
	if absf(facing.y)>absf(facing.x)*1.1: view = "back" if facing.y<0 else "front"
	sprite.flip_h = view=="side" and facing.x<0
	var frame := 0
	if _joy>=0:
		_joy += dt
		if _joy>.66: _joy = -1
		frame = {"side":17,"front":56,"back":62}[view]+mini(5,int(maxf(_joy,0)/.11))
	elif seated: frame = {"side":93,"front":103,"back":110}[view]
	elif speed>3:
		var run := running and not wading
		_phase = fposmod(_phase+speed*dt/maxf(visual_scale*(170 if run else 85),1),1)
		if run: frame = {"side":114,"front":122,"back":126}[view]+int(_phase*(8 if view=="side" else 4))
		else: frame = {"side":9,"front":68,"back":76}[view]+int(_phase*8)
	else:
		_idle += dt; frame = {"side":0,"front":39,"back":47}[view]+int(_idle/.42)%8
	sprite.frame = frame
