extends RefCounted
const Art=preload("res://scripts/card_assets.gd")

func draw(view,t: float,event: Dictionary) -> void:
 var key: String=event.get("visual","").trim_prefix("special_")
 var source: Vector2=view._position(int(event.get("source",0)))-Vector2(0,70)
 var target: Vector2=view._position(2)-Vector2(0,80)
 var impact: float=event.get("impact",1.1)
 var duration: float=event.get("duration",2.8)
 var fade:=1-smoothstep(duration-.8,duration,t)
 var rise:=smoothstep(.1,impact,t)
 var colors: Dictionary={"wild_rally":"b8d38d","starfall":"ffe5a6","phoenix":"ffac68","tide":"87dde3","stormflock":"b1dced","eclipse":"b1a0e7","ancient":"9ad3a2","mirror":"c4a0ed"}
 var color:=Color(colors.get(key,"b8cfdf"))
 view.draw_arc(source,24+rise*43,-t,t+TAU,48,Color(color,fade*.6),2.5,true)
 view.spectacle.star(view,source,10+rise*8,Color(color,fade),t)
 match key:
  "root_strike":
   var trail:=PackedVector2Array()
   for i in 42:
    var p:=i/41.0*rise;trail.append(source.lerp(target,p)+Vector2(0,sin(p*18)*9+sin(p*PI)*42))
   view.draw_polyline(trail,Color(.2,.36,.2,fade),9,true)
   view.draw_polyline(trail,Color(.63,.81,.44,fade),3,true)
   if t>impact:view.spectacle.burst(view,target,Color("bad898"),fade,30+(t-impact)*65)
  "echo","mirror":
   var at:=source.lerp(target,rise)-Vector2(0,sin(rise*PI)*55)
   for i in 4:view.draw_arc(at,16+i*13+sin(t*5)*3,0,TAU,48,Color(.72,.48,1,fade*(.7-i*.13)),3,true)
   if key=="mirror":
    _spirit(view,"mirror",Vector2(744,235),135,fade)
    var rim:=PackedVector2Array()
    for i in 65:
     var a:=i*TAU/64;rim.append(Vector2(744,235)+Vector2(cos(a)*73,sin(a)*99)*rise)
    view.draw_polyline(rim,Color(.81,.63,1,fade),4,true)
   if t>impact:view.spectacle.particles(view,target,t-impact,Color("ca9de6"),110,34)
  "starfall":
   var stars: Array[Vector2]=[Vector2(408,190),Vector2(553,176),Vector2(692,216),Vector2(807,183),Vector2(891,233)]
   for i in stars.size():
    if i>0:view.draw_line(stars[i-1],stars[i],Color(color,fade*.45),2,true)
    view.spectacle.star(view,stars[i],11+rise*8,Color(color,fade),t*.4)
    var p:=smoothstep(.4+i*.08,impact+i*.08,t)
    var end: Vector2=target+Vector2((i-2)*26,(i%2)*22)
    view.draw_line(stars[i],stars[i].lerp(end,p),Color(color,fade*.4),5,true)
    view.spectacle.star(view,stars[i].lerp(end,p),9,Color(color,fade),t*3)
   if t>impact:view.spectacle.burst(view,target,color,fade,50+(t-impact)*110)
  "phoenix":
   var at:=source.lerp(target,rise)-Vector2(0,sin(rise*PI)*115)
   _spirit(view,"phoenix",at,130,fade)
   for i in 23:
    var q:=clampf(rise-i*.012,0,1)
    var p: Vector2=source.lerp(target,q)-Vector2(0,sin(q*PI)*115)
    view.draw_circle(p+Vector2(sin(i*5+t)*15,cos(i*2)*11),3+i%3,Color(1,.5,.2,fade*(1-i/25.0)*.6))
   if t>impact:view.spectacle.burst(view,target,Color("ffd098"),fade,50+(t-impact)*85)
  "tide","grove_heal":
   var wave:=PackedVector2Array()
   for i in 70:
    var x:=30+i*12.0;wave.append(Vector2(x,382+sin(x*.012-t*5)*24))
   view.draw_polyline(wave,Color(.3,.8,.84,fade*.4),18,true)
   view.draw_polyline(wave,Color(.77,1,.91,fade*.7),3,true)
   for i in 2:
    var foot: Vector2=view._position(i)
    view.spectacle.burst(view,foot-Vector2(0,50),Color("a3e8ce"),fade,28+rise*49)
    for j in 10:
     var at:=foot+Vector2(sin(j*2+t)*40,-fposmod(t*56+j*17,140))
     view.spectacle.star(view,at,4,Color(.7,1,.85,fade*.65),t)
  "eclipse":
   var center:=Vector2(731,223)
   view.draw_circle(center,71*rise,Color(.08,.035,.14,fade*.96))
   view.draw_arc(center,75*rise,0,TAU,72,Color(.8,.66,1,fade),5,true)
   view.draw_arc(center+Vector2(9,0),68*rise,-1.2,1.2,42,Color(1,.87,.79,fade),4,true)
   for i in 16:
    var a:=i*TAU/16+t*.2;var at:=center+Vector2(cos(a),sin(a))*(80+rise*54)
    view.draw_line(center+Vector2(cos(a),sin(a))*80,at,Color(.57,.42,.9,fade*.55),2,true)
   view.spectacle.burst(view,target,Color("bdb0ef"),fade,38+rise*70)
  "wild_rally","stormflock","ancient":
   if key=="ancient":_spirit(view,"ancient",Vector2(700,240),147,fade*.85)
   for uid in event.get("summoned_uids",[]):
    var unit: Dictionary=view.model.find_unit(int(uid))
    if unit.is_empty():continue
    var foot: Vector2=view._unit_position(int(uid))
    view.spectacle.burst(view,foot-Vector2(0,35),color,fade,30+rise*43)
    if t<impact:
     var at:=source.lerp(foot,rise)-Vector2(0,sin(rise*PI)*(74 if key=="stormflock" else 28))
     view._draw_unit(unit,at)
  "shadow_skin":
   for i in 2:
    var foot: Vector2=view._position(i)
    view.draw_arc(foot-Vector2(0,55),59*rise,0,TAU,48,Color(.71,.57,.95,fade*.7),3,true)
  "wind_ready":
   for unit in view.model.units(int(event.source)):
    var foot: Vector2=view._unit_position(int(unit.uid))
    for i in 3:view.draw_arc(foot-Vector2(0,32),30+i*9,-t*3+i,t*2+i,38,Color(.71,.89,1,fade*.55),2,true)
 if key in ["starfall","phoenix","eclipse","mirror"] and t>=impact:
  view.draw_rect(Rect2(0,165,1280,331),Color(color,.09*fade*(1-smoothstep(impact,impact+.4,t))))

func _spirit(view,form: String,at: Vector2,height: float,alpha: float) -> void:
 var art:=Art.form_texture(form);var dimensions:=art.region.size*(height/art.region.size.y)
 view.draw_texture_rect_region(art.atlas,Rect2(at-dimensions*.5,dimensions),art.region,Color(1,1,1,alpha))
