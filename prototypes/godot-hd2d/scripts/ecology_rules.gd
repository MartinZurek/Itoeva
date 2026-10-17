extends RefCounted
static func handles(b) -> bool:return b.enemy_profile.get("kind","") in ["crab","beetle","carrier","boss"]
static func setup(b) -> void:
 if not handles(b):return
 b.ecology_state={"step":0,"open":false,"fuel":false,"phase":1,"core_turns":0,"resolve":0}
 if b.enemy_profile.kind=="boss":_seals(b,3)
static func _seals(b,count: int) -> void:
 for i in count:b._add_minion(2,"vharok_seal_"+str(i),{"name":["Glutsiegel","Kettensiegel","Aschensiegel"][i],"attack":0,"health":10,"taunt":true,"species":"enemy"})
static func seal_count(b) -> int:
 var count:=0
 for unit in b.units(2):
  if str(unit.id).begins_with("vharok_seal_"):count+=1
 return count
static func _clear_seals(b) -> void:
 for unit in b.units(2).duplicate():
  if str(unit.id).begins_with("vharok_seal_"):b.board.erase(unit)
static func after_damage(b) -> void:
 if b.enemy_profile.get("kind","")!="boss" or b.enemy_health<=0:return
 if b.enemy_health<=90 and int(b.ecology_state.phase)!=3:
  _clear_seals(b);b.ecology_state.phase=3;b.ecology_state.step=0
 elif int(b.ecology_state.phase)==1 and seal_count(b)==0:
  b.ecology_state.phase=2;b.ecology_state.core_turns=2
static func damage(b,amount: int) -> int:
 if not handles(b):return amount
 if b.enemy_profile.kind=="crab" and not b.ecology_state.get("open",false):return maxi(0,amount-4)
 if b.enemy_profile.kind=="boss":
  if int(b.ecology_state.get("phase",1))==1 and seal_count(b)>0:return int(ceil(amount*.25))
  if int(b.ecology_state.get("phase",1))==2:return int(ceil(amount*1.5))
 return amount
static func intent(b) -> String:
 if b.enemy_stagger>0:return "Wanken · nächster Zug entfällt"
 var step:=int(b.ecology_state.step)
 match b.enemy_profile.kind:
  "crab":return ["Panzer schließen · Treffer −4","Zangenschlag · 14 · Panzer öffnet","Offener Panzer · Material sortieren"][step%3]
  "beetle":return ("Brennen verhindert Harzheilung" if b.enemy_burn>0 else "Harzsammeln · heilt 12") if step%2==0 else "Harzwurf · 10 · Bindung bei LP-Schaden"
  "carrier":return "Schlackenschlag · 22 · Wasser löscht" if b.ecology_state.fuel else "Schlacke laden · Schlag vorbereiten"
  "boss":
   match int(b.ecology_state.phase):
    1:return "I · %s Siegel · Kern 25 %% · Hieb 8"%seal_count(b)
    2:return "II · Kern offen · Schaden 150 % · Hieb 10"
    3:return ["III · Aschensturm vorbereiten · schützt euch!","Aschensturm · 42 auf beide","Erschöpfung · Hieb 8"][step%3]
 return "Beobachtet die Gruppe"
static func counter(b,element: String,effects: Array) -> void:
 if b.enemy_profile.get("kind","")=="boss" and int(b.ecology_state.get("resolve",0))>0 and b.enemy_stagger>0:
  b.enemy_stagger=0;effects.append({"target":2,"amount":0,"kind":"blocked"})
 if b.enemy_profile.get("kind","")=="carrier" and element=="water" and b.ecology_state.get("fuel",false):
  b.ecology_state.fuel=false;effects.append({"target":2,"amount":0,"kind":"release"})
static func _hit(b,target: int,amount: int,effects: Array[Dictionary]) -> int:
 if b.enemy_weaken>0:amount=int(ceil(amount*.65))
 if b.party[target].guard:amount=int(ceil(amount*.35))
 return b._receive_hit(target,amount,effects)
static func act(b) -> bool:
 var previous: Dictionary=b._before();var effects: Array[Dictionary]=[];var title:=intent(b)
 var was_resolute:=int(b.ecology_state.get("resolve",0))>0
 var step:=int(b.ecology_state.step);var action: String="prepare";var target: int=(b.round-1)%2
 if b.party[target].hp<=0:target=1-target
 if b.enemy_stagger>0:
  b.enemy_stagger-=1;b.ecology_state.fuel=false;action="interrupt"
  if b.enemy_profile.kind=="boss":
   b.ecology_state.resolve=2
   if int(b.ecology_state.phase)==3:b.ecology_state.step=-1
 else:
  match b.enemy_profile.kind:
   "crab":
    b.ecology_state.open=step%3!=0
    if step%3==1:_hit(b,target,14,effects);action="strike"
   "beetle":
    if step%2==0:
     if b.enemy_burn==0:
      var healed:=mini(12,b.enemy_max_hp-b.enemy_health);b.enemy_health+=healed
      effects.append({"target":2,"amount":healed,"kind":"heal"});action="gather"
    else:
     action="resin"
     if _hit(b,target,10,effects)>0 and b.party[target].hp>0:
      b.party[target].bind=1;effects.append({"target":target,"amount":1,"kind":"bind"})
   "carrier":
    if b.ecology_state.fuel:_hit(b,target,22,effects);b.ecology_state.fuel=false;action="slag"
    else:b.ecology_state.fuel=true
   "boss":
    match int(b.ecology_state.phase):
     1:_hit(b,target,8,effects);action="chain"
     2:
      _hit(b,target,10,effects);action="chain";b.ecology_state.core_turns-=1
      if b.ecology_state.core_turns<=0:b.ecology_state.phase=1;_seals(b,1)
     3:
      if step%3==1:
       action="storm"
       for i in 2:
        if b.party[i].hp>0:_hit(b,i,42,effects)
      elif step%3==2:_hit(b,target,8,effects);action="chain"
      else:action="warning"
 if was_resolute:b.ecology_state.resolve=maxi(0,int(b.ecology_state.resolve)-1)
 b.ecology_state.step+=1
 if b.party[0].hp<=0 and b.party[1].hp<=0:b.phase="defeat"
 if b.enemy_burn>0:
  b.enemy_burn-=1
  var burn:=mini(damage(b,6),b.enemy_health);b.enemy_health-=burn
  effects.append({"target":2,"amount":burn,"kind":"burn"})
  if b.enemy_health==0 and b.phase!="defeat":b.phase="victory"
 after_damage(b)
 if b.enemy_weaken>0:b.enemy_weaken-=1
 var element: String="water" if b.enemy_profile.kind=="crab" else "nature" if b.enemy_profile.kind=="beetle" else "fire"
 b._event(b.enemy_name+" · "+title,"ecology",effects,previous,{"element":element,"duration":2.2 if b.enemy_profile.kind=="boss" else 1.8,"impact":.9,"target_code":"h:"+str(target),"ecology_kind":b.enemy_profile.kind,"ecology_action":action})
 b.pending_advance=true
 return true
