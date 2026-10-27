extends RefCounted
## Gezeichnete Flug-/Hüpfphasen; verändert weder Geschwindigkeit noch Kollision.
var mode := "rest"
var cycle := 0.0
var elapsed := 0.0
var lift := 0.0

func reset() -> void:
 mode="rest";cycle=0.0;elapsed=0.0;lift=0.0

func phase(data: Dictionary) -> int:
 var weights: Array=data.get("phase_weights",[])
 if weights.is_empty():return posmod(int(cycle*int(data.frames)),int(data.frames))
 var total:=0.0
 for w in weights:total+=float(w)
 var at:=fposmod(cycle,1.0)*total
 for i in weights.size():
  at-=float(weights[i])
  if at<0:return i
 return weights.size()-1

func advance(delta: float,moving: bool,sprint: bool,speed: float,stride: float,cycles: Dictionary,flying: bool=false) -> Dictionary:
 var dt:=maxf(delta,0.0)
 var desired:="run" if sprint else "walk"
 if flying:
  if mode=="rest" and moving:mode="takeoff";elapsed=0.0
  if mode=="takeoff":
   if not moving and elapsed<.16:reset()
   else:
    elapsed+=dt;lift=56.0*clampf(elapsed/.32,0.0,1.0)
    if elapsed>=.32:mode=desired;cycle=0.0;elapsed=0.0
  elif mode in ["walk","run"]:
   cycle+=dt*(2.1 if sprint else 1.8)
   lift=56.0
   if not moving and phase(cycles[mode])==0:mode="land";elapsed=0.0
   elif moving:mode=desired
  elif mode=="land":
   elapsed+=dt;lift=56.0*(1.0-clampf(elapsed/.36,0.0,1.0))
   if moving:mode="takeoff";elapsed=.32*(1.0-clampf(elapsed/.36,0.0,1.0))
   elif elapsed>=.36:reset()
  var index:=0
  if mode in ["takeoff","land"]:index=mini(3,int(elapsed/(.32 if mode=="takeoff" else .36)*4))
  elif mode in ["walk","run"]:index=phase(cycles[mode])
  return {"mode":mode,"phase":index,"cycle":cycle,"lift":lift}
 if mode=="rest" and moving:mode=desired;cycle=0.0
 if mode in ["walk","run"]:
  var before:=floori(cycle)
  # Ein losgelassener Stick friert einen Stern nicht mitten im Flug ein.
  # Nur die Darstellungsfolge landet zu Ende, der physische Weg bleibt stehen.
  cycle+=dt*(maxf(0.0,speed)/maxf(.01,stride) if moving else .95)
  var contact:=phase(cycles[mode])==0 or floori(cycle)>before
  if contact:
   if moving:mode=desired
   else:reset()
 return {"mode":mode,"phase":0 if mode=="rest" else phase(cycles[mode]),"cycle":cycle,"lift":0.0}
