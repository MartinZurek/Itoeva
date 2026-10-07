extends Control
var direction := Vector2.ZERO
func _draw() -> void:
	draw_circle(Vector2.ZERO,76,Color(.12,.20,.17,.45))
	draw_arc(Vector2.ZERO,76,0,TAU,64,Color(.93,.86,.67,.6),2,true)
	draw_circle(direction*48,27,Color(.91,.83,.65,.66))
