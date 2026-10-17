extends RefCounted
# Ziele stammen aus dem aufgelösten Ereignis, einschließlich abfangender Helfer.
static func targets(view,event: Dictionary) -> Array[Vector2]:
 var points: Array[Vector2]=[]
 for hit in event.get("effects",[]):
  if hit.kind not in ["damage","barrier_hit","barrier_break","evade","shield"]:continue
  var target:=int(hit.target)
  var point: Vector2=view._unit_position(int(hit.unit),true)-Vector2(0,40) if target==3 else view._position(target)-Vector2(0,65)
  if target<2 or target==3:
   if point not in points:points.append(point)
 return points
static func draw(view,t: float,event: Dictionary) -> void:
 if t<0 or t>float(event.duration):return
 var action: String=event.get("ecology_action","")
 var source: Vector2=view._position(2)-Vector2(0,115)
 var impact:=float(event.impact)
 var fade:=1-smoothstep(impact+.1,impact+.75,t)
 var progress:=smoothstep(.1,impact,t)
 var color:=Color("c7abec") if event.get("ecology_kind","")=="boss" else Color("eeb46e")
 if action in ["prepare","warning","gather","interrupt"]:
  var alpha:=sin(clampf(t/float(event.duration),0,1)*PI)*.6
  if action=="interrupt":color=Color("9ad5e7")
  elif action=="gather":color=Color("b9d596")
  view.draw_arc(source,28+progress*18,-t*.8,PI*1.5-t*.8,36,Color(color,alpha),2,true)
  if action=="warning":
   for i in 3:
    var at:=source+Vector2((i-1)*31,22)
    view.spectacle.star(view,at,7,Color(color,alpha),t*.2)
  return
 for target in targets(view,event):
  var tip:=source.lerp(target,progress)-Vector2(0,sin(progress*PI)*38)
  if action=="chain":
   if t<impact+.1:
    for i in 16:
     var at:=source.lerp(tip,i/15.0)
     view.draw_arc(at,5,-.6,.6+PI,12,Color(color,fade*.65),2,true)
  elif action=="storm":
   var from:=target-Vector2(22,165)
   for i in 8:
    var local:=clampf(progress*1.15-i*.035,0,1)
    var at:=from.lerp(target,local)+Vector2((i-3.5)*9,0)
    view.draw_line(at-Vector2(7,22),at,Color("eab57b")*Color(1,1,1,fade*.55),2,true)
  elif action=="strike":
   if t>=impact-.2:view.draw_arc(target,25+progress*10,-.8,1.5,20,Color("a9d4cc")*Color(1,1,1,fade),3,true)
  else:
   color=Color("dbb366") if action=="resin" else Color("f49b65")
   if t<impact:view.spectacle.star(view,tip,8,color,t)
  if t>=impact:
   var since:=t-impact
   view.spectacle.burst(view,target,color,fade*.6,18+since*44)
   view.spectacle.particles(view,target,since,color,34,10)
