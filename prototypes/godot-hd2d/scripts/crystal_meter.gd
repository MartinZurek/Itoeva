extends Control
var available := 0
var capacity := 1
var preview_cost := 0
var clock := 0.0
var flash := 0.0
var previous := -1
func update_value(value: int, cap: int, cost: int=0) -> void:
	if previous>=0 and value!=previous: flash=1.0
	previous=value; available=value; capacity=cap; preview_cost=cost; queue_redraw()
func _process(delta: float) -> void:
	clock+=delta;flash=maxf(0,flash-delta*2.8);queue_redraw()
func _draw() -> void:
	var font:=ThemeDB.fallback_font
	draw_string(font,Vector2(0,29),"%s/%s" % [available,capacity],HORIZONTAL_ALIGNMENT_LEFT,-1,23,Color("def9ff"))
	for i in 10:
		var center:=Vector2(75+i*26,23)
		var lit:=i<available
		var color:=Color("69ddf2") if lit else Color("375166") if i<capacity else Color("192735")
		if lit and preview_cost>0 and i>=available-preview_cost:color=Color("ffda80")
		var diamond:=PackedVector2Array([center+Vector2(0,-14),center+Vector2(9,0),center+Vector2(0,14),center+Vector2(-9,0)])
		if lit:draw_circle(center,13,Color(color,.10+.05*sin(clock*2+i)+flash*.12))
		draw_colored_polygon(diamond,color)
		draw_polyline(diamond+PackedVector2Array([diamond[0]]),Color("d6f6ff") if lit else Color("597080"),1.3,true)
		if lit:
			draw_colored_polygon(PackedVector2Array([center+Vector2(0,-10),center+Vector2(5,0),center,center+Vector2(-5,0)]),Color(1,1,1,.4))
