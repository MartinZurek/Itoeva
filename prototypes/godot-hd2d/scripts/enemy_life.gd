extends RefCounted
# Hinweg, Tätigkeit, Rückweg und Ruhe bleiben an das Revier gebunden.
static func sample(profile: Dictionary,clock: float) -> Dictionary:
 var period:=maxf(8,float(profile.get("period",14)))
 var phase:=fposmod(clock,period)/period
 var patrol: Array=profile.get("patrol",[profile.uv[0],profile.uv[0]])
 var x:=float(patrol[0]);var moving:=false;var pose:=0
 var activity:=str(profile.get("purpose",""))
 if phase<.30:x=lerpf(patrol[0],patrol[1],smoothstep(0,.30,phase));moving=true
 elif phase<.55:x=patrol[1];pose=5;activity=str(profile.get("work",""))
 elif phase<.85:x=lerpf(patrol[1],patrol[0],smoothstep(.55,.85,phase));moving=true
 if moving:pose=6 if int(clock*3)%2==0 else 0;activity=str(profile.get("travel",""))
 return {"uv":Vector2(x,profile.uv[1]),"moving":moving,"pose":pose,"activity":activity}
