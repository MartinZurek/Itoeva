extends SceneTree
const Steps=preload("res://scripts/step_gait.gd")
var checks:=0
var failures:=0
func check(ok: bool, label: String) -> void:
 checks+=1
 if not ok:failures+=1;push_error(label)
func _initialize() -> void:
 for blend in [0.0,.5,1.0]:
  for amplitude in [18.0,26.0,33.0]:
   for depth in [0.0,.573576,-.573576]:
    var duty: float=Steps.sample(0,amplitude,blend).duty
    for rate in [30,60,120]:
     var cycle:=0.0
     var grounded:=0
     for tick in rate:
      cycle=float(tick)/rate
      var a: Dictionary=Steps.sample(cycle,amplitude,blend,depth)
      var b: Dictionary=Steps.sample(cycle+.5,amplitude,blend,depth)
      if blend==0:check(a.contact or b.contact,"Gehen hat immer einen Stuetzfuss")
      check(a.lift>=0 and (not a.contact or is_zero_approx(a.lift)),"Nur der freie Fuss hebt ab")
      if a.contact and cycle+.0001<duty:
       grounded+=1
       var later: Dictionary=Steps.sample(cycle+.0001,amplitude,blend,depth)
       var pixel_size:=.009
       var world_delta:=.0001*Steps.stride(amplitude,pixel_size,blend)
       check(absf(world_delta+(later.travel-a.travel)*pixel_size)<.000001,"Stuetzfuss haftet bei Weltfortschritt")
     check(grounded>0,"Standphase bei allen Zeitschritten erreichbar")
    for seam in [0.0,duty,1.0]:
     var before: Dictionary=Steps.sample(seam-.00001,amplitude,blend,depth)
     var after: Dictionary=Steps.sample(seam+.00001,amplitude,blend,depth)
     var center: Dictionary=Steps.sample(seam,amplitude,blend,depth)
     check(before.offset.distance_to(after.offset)<.01,"Kein Phasensprung")
     var v1: Vector2=(center.offset-before.offset)/.00001
     var v2: Vector2=(after.offset-center.offset)/.00001
     check(v1.distance_to(v2)<.2,"Stetige Abhebe-/Aufsetzgeschwindigkeit")
 print("Step contacts: %s checks, %s failures"%[checks,failures]);quit(1 if failures else 0)
