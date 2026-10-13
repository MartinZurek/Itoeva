extends Control
var fill_color := Color.WHITE
var max_value := 100.0
var value := 0.0:
 set(next):
  value = clampf(next,0,max_value)
  if first:
   shown=value
   first=false
  queue_redraw()
var shown := 0.0
var first := true
func _process(delta: float) -> void:
 shown=move_toward(shown,value,max_value*delta*2.8)
 queue_redraw()
func _draw() -> void:
 var back:=StyleBoxFlat.new()
 back.bg_color=Color("0c1725")
 back.set_corner_radius_all(4)
 draw_style_box(back,Rect2(Vector2.ZERO,size))
 if shown>0:
  var fill:=StyleBoxFlat.new()
  fill.bg_color=fill_color
  fill.set_corner_radius_all(4)
  draw_style_box(fill,Rect2(Vector2.ZERO,Vector2(size.x*shown/max_value,size.y)))
