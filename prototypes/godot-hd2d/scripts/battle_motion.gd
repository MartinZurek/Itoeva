extends RefCounted
# Reine Darstellungsdaten: derselbe gespeicherte Trefferzeitpunkt steuert Anlauf,
# Kontakt und Rückkehr. Kein zweiter Kampfzustand und keine neue Schadensberechnung.
const GROUND := Rect2(48,260,1176,210)

static func rest(home: Vector2, clock: float, seed: int=0, airborne: bool=false) -> Dictionary:
 var lift:=3.0+sin(clock*2.0+seed)*1.2 if airborne else 0.0
 return {"ground":home,"foot":home-Vector2(0,lift),"lift":lift,"lean":sin(clock*1.35+seed)*.008,"travel":false,"returning":false,"progress":0.0,"stride":0.0}

static func attack(home: Vector2, target: Vector2, time: float, impact: float, duration: float, style: String="pounce", lane: int=0, reach: float=INF) -> Dictionary:
 if time<0 or time>=duration:return rest(home,0)
 var direction:=1.0 if target.x>=home.x else -1.0
 var contact:=target-Vector2(direction*(72.0+abs(lane)*56.0),-lane*36.0)
 if home.distance_to(contact)>reach:contact=home+home.direction_to(contact)*reach
 contact=confine(contact)
 var local:=clampf(time/maxf(.1,impact),0,1)
 var outbound:=time<impact
 var hold:=minf(.12,(duration-impact)*.18)
 var progress:=smoothstep(.12,.88,local) if outbound else 1.0-smoothstep(impact+hold,duration-.08,time)
 var anticipation: float=sin(clampf(local/.12,0,1)*PI)*5.0 if local<.12 else 0.0
 # Leichte Kurven laufen im freien Zwischenraum der Formationen. Nahschritte
 # großer Kämpfer bleiben dagegen in ihrem eigenen Standbereich.
 var channel:=clampf(399.0+lane*18.0,379,438)
 var c1:=Vector2(home.x+direction*minf(45,home.distance_to(contact)*.2),channel)
 var c2:=Vector2(contact.x-direction*minf(45,home.distance_to(contact)*.2),channel)
 if reach<100:c1=home.lerp(contact,.33);c2=home.lerp(contact,.66)
 var ground:=home.bezier_interpolate(c1,c2,contact,progress)-Vector2(direction*anticipation,0)
 ground=confine(ground)
 var travelling: bool=(outbound and local>.12 and local<.88) or (not outbound and time>impact+hold and time<duration-.08)
 var lift: float=(11.0 if style=="flier" else 1.5 if style in ["guard","crawler"] else 6.0)*sin(progress*PI)
 var strike:=sin(clampf((time-impact+.10)/.20,0,1)*PI)
 return {"ground":ground,"foot":ground-Vector2(0,lift),"lift":lift,"lean":direction*(.065*strike-.025*sin(progress*PI)),"travel":travelling,"returning":not outbound,"progress":progress,"stride":(home.distance_to(ground)+(home.distance_to(contact)*2 if not outbound else 0))/34.0}

static func hit(home: Vector2, time: float, impact: float, direction: float=1.0) -> Dictionary:
 var state:=rest(home,0)
 var elapsed:=time-impact
 if elapsed>=0 and elapsed<.38:
  var recoil:=sin(clampf(elapsed/.38,0,1)*PI)*7.0
  state.ground=home+Vector2(direction*recoil,0);state.foot=state.ground;state.lean=direction*sin(elapsed/.38*PI)*.045
 return state

static func confine(point: Vector2) -> Vector2:
 return Vector2(clampf(point.x,GROUND.position.x,GROUND.end.x),clampf(point.y,GROUND.position.y,GROUND.end.y))
