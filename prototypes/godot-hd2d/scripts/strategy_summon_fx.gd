extends RefCounted
const Cards=preload("res://scripts/card_catalog.gd")
const Art=preload("res://scripts/card_assets.gd")
# Ereigniszeit statt Echtzeit: Pause und Save/Resume verändern das Ritual nicht.
static func draw(v,time: float,event: Dictionary,foot: Vector2,source: Vector2) -> void:
 var card:=Cards.get_card(str(event.get("card","")))
 var color:=Art.accent(card)
 var impact: float=event.get("impact",.7)
 var fade:=1.0-smoothstep(impact,impact+.8,time)
 if fade<=0:return
 var p:=smoothstep(.05,impact,time)
 var at:=source.lerp(foot-Vector2(0,28),p)-Vector2(0,sin(p*PI)*28)
 v.spectacle.star(v,at,11,color,time)
 var center:=foot-Vector2(0,12)
 var radius:=18.0+29.0*p
 var ring:=PackedVector2Array()
 for i in 49:
  var angle:=i*TAU/48
  ring.append(center+Vector2(cos(angle)*radius,sin(angle)*radius*.31))
 v.draw_polyline(ring,Color(color,fade*.7),2,true)
 var tribe: String=card.get("volk","")
 var mechanical: bool=tribe in ["Werkvolk","Glimmerwerk","Artefakte","Erzvolk"]
 var organic: bool=tribe in ["Kräutervolk","Erntevolk","Bauvolk"]
 for i in (10 if card.get("rare",false) else 6):
  var angle:=i*TAU/(10 if card.get("rare",false) else 6)+time*.7
  var point:=center+Vector2(cos(angle)*radius,sin(angle)*radius*.31)-Vector2(0,sin(p*PI)*22)
  if mechanical:v.draw_rect(Rect2(point-Vector2(3,3),Vector2(6,6)),Color(color,fade),false,1.5)
  elif organic:v.draw_arc(point,5,angle,angle+PI,8,Color(color,fade),1.8,true)
  else:v.spectacle.star(v,point,4,Color(color,fade),angle)
 if card.get("rare",false):
  var halo:=foot-Vector2(0,48)
  v.draw_arc(halo,42+p*16,-PI+time*.25,PI*.65+time*.25,42,Color(color,fade*.5),2,true)
  v.draw_arc(halo,49+p*16,-time*.3,PI*1.5-time*.3,42,Color("fff0c9",fade*.4),1,true)
 if time>=impact:v.spectacle.burst(v,foot-Vector2(0,30),color,fade,20+p*30)
