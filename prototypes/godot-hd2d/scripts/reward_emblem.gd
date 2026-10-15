extends Control
var kind: String="moon_shard"
var clock:=0.0
func _ready() -> void:mouse_filter=Control.MOUSE_FILTER_IGNORE
func _process(delta: float) -> void:
 clock+=delta;queue_redraw()
func _draw() -> void:
 var at:=size*.5
 var color:=Color("a8dcd5") if kind=="moon_shard" else Color("a8d4a0")
 draw_circle(at,17,Color(color,.12))
 if kind=="moon_shard":
  var points:=PackedVector2Array([at+Vector2(0,-12),at+Vector2(9,0),at+Vector2(0,12),at+Vector2(-9,0)])
  draw_colored_polygon(points,color)
  draw_polyline(PackedVector2Array([at+Vector2(0,-12),at+Vector2(-3,0),at+Vector2(0,12)]),Color("f0edbd"),1.3,true)
 else:
  var points:=PackedVector2Array([at+Vector2(-4,-10),at+Vector2(4,-10),at+Vector2(4,-4),at+Vector2(9,4),at+Vector2(6,11),at+Vector2(-6,11),at+Vector2(-9,4),at+Vector2(-4,-4)])
  draw_colored_polygon(points,Color("81b699"))
  draw_line(at+Vector2(-5,-11),at+Vector2(5,-11),Color("d9bd86"),4,true)
  draw_line(at+Vector2(-4,2),at+Vector2(4,2),Color("dff3d9"),2,true)
 draw_arc(at,17,clock*.7,clock*.7+PI*1.4,20,Color("dfc68e"),1.2,true)
