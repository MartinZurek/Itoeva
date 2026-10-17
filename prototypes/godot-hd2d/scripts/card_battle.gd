extends "res://scripts/forest_battle.gd"
const Ecology=preload("res://scripts/ecology_rules.gd")
var ecology_state: Dictionary={}
const Cards=preload("res://scripts/card_catalog.gd")
const BOSS_HP := 220
const BOARD_LIMIT := 3
const HAND_LIMIT := 5
var decks: Array[Dictionary]=[]
var board: Array[Dictionary]=[]
var fallen: Array[Array]=[[],[],[]]
var source_decks: Dictionary={}
var next_uid:=1
var pending_advance:=false
var enemy_max_hp:=BOSS_HP
var enemy_id := ""
var enemy_profile: Dictionary = {}
var demon_charge := false
var seal_pending := false
const SPECIAL_CHARGES := 5
var special_charges: Array[int] = [0,0]
var special_used: Array[bool] = [false,false]

func _init() -> void:
 super()
 enemy_health=enemy_max_hp
 prepare({})

func prepare(custom: Dictionary) -> void:
 source_decks=custom.duplicate(true)
 decks.clear();board.clear();fallen=[[],[],[]];next_uid=1;pending_advance=false
 special_charges=[0,0];special_used=[false,false]
 demon_charge=false;seal_pending=false;ecology_state={}
 for owner in 3:
  var species: String="enemy" if owner==2 else str(party[owner].species)
  var entries: Array=custom.get(species,[])
  var ids: Array[String]=Cards.starter(species)
  if owner<2:ids=Cards.upgrade(species,entries)
  for i in range(ids.size()-1,0,-1):
   var j:=rng.randi_range(0,i);var value:=ids[i];ids[i]=ids[j];ids[j]=value
  decks.append({"species":species,"draw":ids,"hand":[],"discard":[],"cap":1,"crystals":1,"power":false,"turns":0})
  _draw_cards(owner,3)
 Ecology.setup(self)

func _draw_cards(owner: int,count: int) -> void:
 var state: Dictionary=decks[owner]
 for i in count:
  if state.draw.is_empty():return
  var id: String=state.draw.pop_front()
  if state.hand.size()<HAND_LIMIT:state.hand.append(id)
  else:state.discard.append(id)

func _begin_turn(owner: int) -> void:
 var state: Dictionary=decks[owner]
 state.cap=mini(10,maxi(1,round));state.crystals=state.cap;state.power=false;state.turns+=1
 for unit in board:
  if unit.owner==owner:
   unit.spent=false
   unit.skipped=int(unit.get("frozen",0))>0
   unit.ready=int(unit.get("frozen",0))==0
   unit.frozen=maxi(0,int(unit.get("frozen",0))-1)
   unit.hp=mini(int(unit.max_hp),int(unit.hp)+int(unit.get("regen",0)))
 _draw_cards(owner,1)

func advance_intro(delta: float) -> bool:
 if not super.advance_intro(delta):return false
 if not enemy_profile.is_empty():message=enemy_name+" · "+str(enemy_profile.description)
 if decks[cursor].turns==0:_begin_turn(cursor)
 return true

# Eine Karte oder Superkraft beendet den Zug nicht; Ende ist eine eigene Aktion.
func _next() -> void:pass

func _advance_actor() -> void:
 for i in 3:
  cursor=(cursor+1)%3
  if cursor==0:round+=1
  if cursor==2 or party[cursor].hp>0:
   _begin_turn(cursor)
   return

func units(owner: int) -> Array[Dictionary]:
 var result: Array[Dictionary]=[]
 for unit in board:
  if int(unit.owner)==owner:result.append(unit)
 return result

func find_unit(uid: int) -> Dictionary:
 for unit in board:
  if int(unit.uid)==uid:return unit
 return {}

func _add_minion(owner: int,id: String,extra: Dictionary={}) -> int:
 if units(owner).size()>=BOARD_LIMIT:return -1
 var card: Dictionary=Cards.get_card(id) if extra.is_empty() else extra
 var unit: Dictionary={"uid":next_uid,"owner":owner,"id":id,"name":card.name,
  "attack":card.get("attack",3),"hp":card.get("health",6),"max_hp":card.get("health",6),
  "ready":card.get("ready",false),"taunt":card.get("taunt",false),"row":card.get("row",-1),"species":card.get("species",""),
  "form":card.get("form",""),"evasion":card.get("evasion",false),"frozen":0,
  "skipped":false,"spent":false,"from_deck":extra.is_empty(),"shield":card.get("shield",0),"regen":card.get("regen",0),"death_draw":card.get("death_draw",0),"lifesteal":card.get("lifesteal",false)}
 board.append(unit);next_uid+=1
 _draw_cards(owner,int(card.get("draw_on_summon",0)))
 return int(unit.uid)

func _remove_dead() -> void:
 for i in range(board.size()-1,-1,-1):
  if board[i].hp<=0:
   var unit: Dictionary=board[i]
   board.remove_at(i)
   if unit.get("from_deck",false) and unit.id in Cards.available(str(decks[int(unit.owner)].species)):
    fallen[int(unit.owner)].append(str(unit.id))
   if unit.get("from_deck",false):decks[int(unit.owner)].discard.append(str(unit.id))
   _draw_cards(int(unit.owner),int(unit.get("death_draw",0)))

func legal_targets(owner: int,spell: bool=false) -> Array[String]:
 var guards: Array[String]=[]
 var result: Array[String]=[]
 for unit in board:
  if (int(unit.owner)==2)==(owner==2):continue
  var code: String="u:"+str(unit.uid)
  result.append(code)
  if unit.taunt:guards.append(code)
 if not spell and not guards.is_empty():return guards
 if owner<2:result.append("enemy")
 else:
  for i in 2:
   if party[i].hp>0:result.append("h:"+str(i))
 return result

func _before() -> Dictionary:
 return {"before_hp":[party[0].hp,party[1].hp],"before_enemy":enemy_health,
  "before_status":_visual_state(),"before_board":board.duplicate(true),
  "before_crystals":[decks[0].crystals,decks[1].crystals,decks[2].crystals],"before_special":special_charges.duplicate()}

func _gain_charge(owner: int) -> void:
 if owner<2 and not special_used[owner]:special_charges[owner]=mini(SPECIAL_CHARGES,special_charges[owner]+1)

func special_card(owner: int=-1) -> Dictionary:
 var index:=cursor if owner<0 else owner
 if index>1:return {}
 return Cards.get_card(Cards.ULTIMATES[party[index].species])

func _hurt_unit(unit: Dictionary,amount: int,effects: Array,protected: int=-1) -> void:
 if bool(unit.get("evasion",false)):
  unit.evasion=false
  effects.append({"target":3,"unit":unit.uid,"amount":0,"kind":"evade","protected":protected})
  return
 var absorbed:=mini(amount,int(unit.get("shield",0)))
 unit.shield=int(unit.get("shield",0))-absorbed
 if absorbed>0:effects.append({"target":3,"unit":unit.uid,"amount":absorbed,"kind":"shield","protected":protected})
 var actual:=mini(int(unit.hp),maxi(0,amount-absorbed))
 unit.hp-=actual
 if actual>0:effects.append({"target":3,"unit":unit.uid,"amount":actual,"kind":"damage","protected":protected})

func _event(title: String,visual: String,effects: Array,previous: Dictionary,extra: Dictionary={}) -> void:
 last_event=previous.duplicate(true)
 last_event.merge({"source":cursor,"title":title,"element":extra.get("element","light"),
  "visual":visual,"effects":effects,"duration":extra.get("duration",1.45),"impact":extra.get("impact",.7)},true)
 last_event.merge(extra,true)
 turn+=1;message=title;playback_pending=true

func _damage_target(owner: int,target: String,amount: int,effects: Array) -> void:
 if target=="enemy":
  amount=Ecology.damage(self,amount)
  var actual:=mini(enemy_health,amount)
  enemy_health-=actual;effects.append({"target":2,"amount":actual,"kind":"damage"})
  if enemy_health==0:phase="victory"
 elif target.begins_with("h:"):
  var index:=int(target.substr(2))
  _receive_hit(index,amount,effects)
  if party[0].hp<=0 and party[1].hp<=0:phase="defeat"
 else:
  var unit: Dictionary=find_unit(int(target.substr(2)))
  if unit.is_empty():return
  _hurt_unit(unit,amount,effects)
 _remove_dead()
 Ecology.after_damage(self)

func _receive_hit(target: int,damage: int,effects: Array[Dictionary]) -> int:
 for unit in board:
  if unit.owner<2 and unit.taunt:
   _hurt_unit(unit,damage,effects,target)
   _remove_dead()
   return 0
 return super._receive_hit(target,damage,effects)

func card_targets(owner: int,card: Dictionary) -> Array[String]:
 var result: Array[String]=[]
 if card.get("target","")=="ally_unit":
  for unit in units(owner):
   if card.kind=="recall" and (unit.id not in Cards.available(str(decks[owner].species)) or decks[owner].hand.size()>HAND_LIMIT):continue
   if card.kind=="mend" and unit.hp>=unit.max_hp and not card.has("shield") and not card.has("draw"):continue
   result.append("u:"+str(unit.uid))
  return result
 if card.get("target","")=="enemy_unit":
  for code in legal_targets(owner,true):
   if code.begins_with("u:"):result.append(code)
  return result
 if card.kind=="damage":return legal_targets(owner,true)
 return result

func can_play(index: int,target: String="") -> bool:
 if phase!="battle" or cursor==2 or playback_pending or party[cursor].hp<=0:return false
 var state: Dictionary=decks[cursor]
 if index<0 or index>=state.hand.size():return false
 var card: Dictionary=Cards.get_card(state.hand[index])
 if state.crystals<card.cost:return false
 if card.has("target"):
  var choices:=card_targets(cursor,card)
  return not choices.is_empty() and (target.is_empty() or target in choices)
 if card.kind=="minion":return units(cursor).size()<BOARD_LIMIT
 if card.kind=="damage":return target.is_empty() or target in legal_targets(cursor,true)
 if card.kind=="heal":return party[cursor].hp<party[cursor].max_hp
 if card.kind=="draw":return not state.draw.is_empty()
 if card.kind=="barrier":return party[cursor].barrier<BARRIER_HP
 if card.kind=="special":return _can_special(card.effect,cursor)
 if card.kind=="team_buff":return not units(cursor).is_empty()
 if card.kind=="volley":return not units(2).is_empty()
 if card.kind=="energy":return state.crystals<state.cap
 if card.kind=="revive":return units(cursor).size()<BOARD_LIMIT and not fallen[cursor].is_empty()
 if card.kind=="party_heal":
  for member in party:
   if member.hp>0 and (member.hp<member.max_hp or member.bind>0 or member.silence>0 or (card.has("barrier") and member.barrier<BARRIER_HP)):return true
  return false
 if card.kind=="weaken":return enemy_weaken<int(card.value) or card.has("draw") or card.has("heal_party")
 return false

func _can_special(effect: String,owner: int) -> bool:
 if effect in ["wild_rally","stormflock"]:return units(owner).size()<BOARD_LIMIT
 if effect=="tide":
  for member in party:
   if member.hp>0 and (member.hp<member.max_hp or member.bind>0 or member.silence>0):return true
  for unit in units(owner):
   if unit.hp<unit.max_hp:return true
  return false
 return true

func _heal_party(amount: int,effects: Array) -> void:
 for i in 2:
  if party[i].hp<=0:continue
  var healed:=mini(amount,int(party[i].max_hp)-int(party[i].hp))
  party[i].hp+=healed
  if healed>0:effects.append({"target":i,"amount":healed,"kind":"heal"})

func _party_barrier(amount: int,effects: Array) -> void:
 for i in 2:
  if party[i].hp<=0:continue
  var gained:=mini(amount,BARRIER_HP-int(party[i].barrier))
  party[i].barrier+=gained
  if gained>0:effects.append({"target":i,"amount":gained,"kind":"barrier"})

func _resolve_special(owner: int,key: String,effects: Array) -> Dictionary:
 var extra: Dictionary={"special_effect":key,"summoned_uids":[]}
 match key:
  "wild_rally":
   for i in mini(2,BOARD_LIMIT-units(owner).size()):
    var animal:=roll_wildlife()
    var uid:=_add_minion(owner,"power_"+str(animal.id),{"name":animal.name,"attack":maxi(2,int(animal.damage)/4),"health":6+int(animal.row)*4,"row":animal.row,"ready":i==0})
    extra.summoned_uids.append(uid)
  "starfall","phoenix":
   _damage_target(owner,"enemy",24 if key=="starfall" else 30,effects)
   for unit in units(2).duplicate():_damage_target(owner,"u:"+str(unit.uid),10 if key=="starfall" else 12,effects)
   if key=="starfall":_party_barrier(16,effects)
   elif units(owner).size()<BOARD_LIMIT:
    extra.summoned_uids.append(_add_minion(owner,"special_phoenix",{"name":"Phönix","attack":6,"health":8,"species":"wyrmling","form":"phoenix"}))
  "tide":
   _heal_party(20,effects)
   for member in party:member.bind=0;member.silence=0
   for unit in units(owner):unit.hp=unit.max_hp
  "stormflock":
   for i in mini(2,BOARD_LIMIT-units(owner).size()):
    extra.summoned_uids.append(_add_minion(owner,"special_stormfalcon",{"name":"Sturmfalke","attack":5,"health":5,"species":"puffling","form":"stormfalcon","ready":true}))
  "eclipse":
   _damage_target(owner,"enemy",18,effects);enemy_stagger=1
   for unit in units(2):unit.frozen=1;unit.ready=false
  "ancient":
   _party_barrier(20,effects)
   if units(owner).size()<BOARD_LIMIT:
    extra.summoned_uids.append(_add_minion(owner,"special_ancient",{"name":"Urwaldhüter","attack":4,"health":16,"species":"mosslyn","form":"ancient","taunt":true}))
  "mirror":
   _damage_target(owner,"enemy",22,effects);enemy_weaken=2
   if units(owner).size()<BOARD_LIMIT:
    extra.summoned_uids.append(_add_minion(owner,"special_mirror",{"name":"Spiegelgeist","attack":4,"health":8,"species":"nocti","form":"mirror","evasion":true}))
 return extra

func _buff_unit(unit: Dictionary,card: Dictionary,effects: Array) -> void:
 unit.attack=mini(30,int(unit.attack)+int(card.get("value",0)))
 var growth:=mini(int(card.get("health",0)),40-int(unit.max_hp))
 unit.max_hp+=growth;unit.hp=mini(int(unit.max_hp),int(unit.hp)+growth)
 unit.shield=mini(20,int(unit.get("shield",0))+int(card.get("shield",0)))
 if card.get("evasion",false):unit.evasion=true
 if card.get("ready",false):unit.ready=true;unit.frozen=0
 effects.append({"target":3,"unit":unit.uid,"amount":growth,"kind":"buff"})

func _resolve_card(owner: int,id: String,target: String,effects: Array) -> Dictionary:
 var card: Dictionary=Cards.get_card(id)
 var extra: Dictionary={"card":id,"element":card.element,"target_code":target}
 if card.kind in ["heal","party_heal","barrier","draw","energy","team_buff"]:extra.target_code="h:"+str(owner)
 match card.kind:
  "minion":extra["unit_uid"]=_add_minion(owner,id)
  "damage":_damage_target(owner,target,int(card.value)+int(card.get("per_helper",0))*units(owner).size(),effects)
  "barrier":
   var gained:=mini(int(card.value),BARRIER_HP-int(party[owner].barrier))
   party[owner].barrier+=gained;effects.append({"target":owner,"amount":gained,"kind":"barrier"})
  "heal":
   if owner==2:enemy_health=mini(enemy_max_hp,enemy_health+int(card.value))
   else:_heal_owner(owner,int(card.value),effects)
  "draw":_draw_cards(owner,int(card.value))
  "special":extra.merge(_resolve_special(owner,card.effect,effects),true)
  "buff":_buff_unit(find_unit(int(target.substr(2))),card,effects)
  "mend":
   var unit:=find_unit(int(target.substr(2)))
   var gained:=mini(int(card.value),int(unit.max_hp)-int(unit.hp))
   unit.hp+=gained;unit.shield=mini(20,int(unit.get("shield",0))+int(card.get("shield",0)))
   effects.append({"target":3,"unit":unit.uid,"amount":gained,"kind":"heal"})
  "freeze":
   var unit:=find_unit(int(target.substr(2)))
   unit.frozen=1;unit.ready=false
   _damage_target(owner,target,int(card.value),effects)
  "recall":
   var unit:=find_unit(int(target.substr(2)))
   decks[owner].hand.append(str(unit.id))
   board.erase(unit)
   extra["recalled_uid"]=int(unit.uid)
  "team_buff":
   for unit in units(owner):_buff_unit(unit,card,effects)
  "volley":
   for unit in units(2).duplicate():_damage_target(owner,"u:"+str(unit.uid),int(card.value),effects)
  "energy":decks[owner].crystals=mini(int(decks[owner].cap),int(decks[owner].crystals)+int(card.value))
  "revive":
   var returning: String=fallen[owner].pop_back()
   decks[owner].discard.erase(returning)
   extra["unit_uid"]=_add_minion(owner,returning)
  "party_heal":
   _heal_party(int(card.value),effects)
   if card.has("barrier"):_party_barrier(int(card.barrier),effects)
   if card.get("cleanse",false):
    for member in party:member.bind=0;member.silence=0
  "weaken":enemy_weaken=maxi(enemy_weaken,int(card.value))
 if owner<2 and card.has("heal_self"):_heal_owner(owner,int(card.heal_self),effects)
 if card.has("heal_party"):_heal_party(int(card.heal_party),effects)
 if card.has("draw"):_draw_cards(owner,int(card.draw))
 return extra

func _heal_owner(owner: int,amount: int,effects: Array) -> void:
 if owner>1 or party[owner].hp<=0:return
 var healed:=mini(amount,int(party[owner].max_hp)-int(party[owner].hp))
 party[owner].hp+=healed
 if healed>0:effects.append({"target":owner,"amount":healed,"kind":"heal"})

func play_card(index: int,target: String="enemy") -> bool:
 if not can_play(index,target):return false
 var previous:=_before()
 var state: Dictionary=decks[cursor]
 var id: String=state.hand[index]
 var card: Dictionary=Cards.get_card(id)
 state.hand.remove_at(index);state.crystals-=int(card.cost)
 if card.kind!="minion":state.discard.append(id)
 var effects: Array[Dictionary]=[]
 var extra:=_resolve_card(cursor,id,target,effects)
 _counter_demon(str(card.element),effects)
 _gain_charge(cursor)
 if card.kind=="special":extra.merge({"duration":2.8,"impact":1.1},true)
 _event(card.name,"special_"+card.effect if card.kind=="special" else "card_summon" if card.kind in ["minion","revive"] else "card_spell",effects,previous,extra)
 return true

func can_attack(uid: int,target: String="") -> bool:
 if phase!="battle" or cursor==2 or playback_pending:return false
 var unit:=find_unit(uid)
 return not unit.is_empty() and unit.owner==cursor and unit.ready and unit.attack>0 and (target.is_empty() or target in legal_targets(cursor))

func attack_minion(uid: int,target: String="enemy") -> bool:
 if not can_attack(uid,target):return false
 var previous:=_before()
 var unit:=find_unit(uid);unit.ready=false;unit.spent=true
 var name: String=unit.name
 var effects: Array[Dictionary]=[]
 var extra: Dictionary={"attacker_uid":uid,"target_code":target,"element":"physical","source":-1}
 if target.begins_with("u:"):
  var defender:=find_unit(int(target.substr(2)))
  var counter:=int(defender.attack)
  _hurt_unit(unit,counter,effects)
 var dealt_start:=effects.size()
 _damage_target(cursor,target,int(unit.attack),effects)
 if unit.get("lifesteal",false):
  var restored:=0
  for i in range(dealt_start,effects.size()):
   if effects[i].kind=="damage":restored+=int(effects[i].amount)
  _heal_owner(cursor,restored,effects)
 _gain_charge(cursor)
 _event(name+" greift an","card_attack",effects,previous,extra)
 return true

func can_act(action: String,target: int=-1) -> bool:
 if phase!="battle" or cursor==2 or playback_pending:return false
 if action in ["end_turn","flee"]:return true
 if action=="break_free":return not decks[cursor].power and (party[cursor].bind>0 or party[cursor].silence>0)
 if action=="special":return not special_used[cursor] and special_charges[cursor]>=SPECIAL_CHARGES and party[cursor].bind==0 and party[cursor].silence==0 and _can_special(special_card().effect,cursor)
 if action not in ["skill","support","light"]:return false
 if decks[cursor].power or party[cursor].bind>0 or party[cursor].silence>0:return false
 if action in ["skill","light"] and party[cursor].species=="fennec":return units(cursor).size()<BOARD_LIMIT
 return true

func act(action: String,target: int=-1) -> bool:
 if not can_act(action,target):return false
 if action=="special":
  var previous:=_before();var card:=special_card();var effects: Array[Dictionary]=[]
  var extra:=_resolve_special(cursor,card.effect,effects)
  extra.merge({"element":card.element,"duration":2.8,"impact":1.1},true)
  _counter_demon(str(card.element),effects)
  special_charges[cursor]=0;special_used[cursor]=true
  _event("Spezialkraft · "+str(card.name),"special_"+str(card.effect),effects,previous,extra)
  return true
 if action=="end_turn":
  party[cursor].bind=maxi(0,int(party[cursor].bind)-1)
  party[cursor].silence=maxi(0,int(party[cursor].silence)-1)
  pending_advance=true
  _event("Zug beendet","card_end",[],_before(),{"duration":1.05,"impact":.45,"element":"none"})
  return true
 if action in ["skill","light"] and party[cursor].species=="fennec":
  var previous:=_before()
  var animal:=roll_wildlife()
  var uid:=_add_minion(cursor,"power_"+str(animal.id),
   {"name":animal.name,"attack":maxi(2,int(animal.damage)/4),"health":6+int(animal.row)*4,"row":animal.row})
  decks[cursor].power=true
  _gain_charge(cursor)
  _event("Wildruf · "+str(animal.name),"card_summon",[],previous,
   {"unit_uid":uid,"summon":animal,"element":"nature","duration":1.8,"impact":.8})
  return true
 if action in ["skill","support","light"] and (party[cursor].species in ["mosslyn","nocti"] or (party[cursor].species=="puffling" and action=="support")):
  var previous:=_before();var effects: Array[Dictionary]=[]
  var name: String=skill().name if action!="support" else support_skill().name
  var visual: String="card_spell"
  var element: String="nature" if party[cursor].species=="mosslyn" else "moon"
  if party[cursor].species=="puffling":
   for unit in units(cursor):unit.ready=true
   visual="special_wind_ready";element="wind"
  elif party[cursor].species=="mosslyn":
   if action=="support":
    _heal_party(12,effects)
    for unit in units(cursor):unit.hp=mini(int(unit.max_hp),int(unit.hp)+6)
    visual="special_grove_heal";element="heal"
   else:_damage_target(cursor,"enemy",14,effects);visual="special_root_strike"
  elif action=="support":
   party[cursor].evasion=true
   for unit in units(cursor):unit.evasion=true
   visual="special_shadow_skin"
  else:_damage_target(cursor,"enemy",15,effects);_draw_cards(cursor,1);visual="special_echo"
  decks[cursor].power=true;_gain_charge(cursor)
  _event(name,visual,effects,previous,{"element":element,"duration":2.0,"impact":.85})
  return true
 var owner:=cursor
 var previous_board:=board.duplicate(true)
 var mp:=int(party[owner].mp)
 var old_bind:=int(party[owner].bind);var old_silence:=int(party[owner].silence)
 party[owner].mp=party[owner].max_mp
 var accepted:=super.act(action,target)
 party[owner].mp=mp
 if action!="break_free":
  party[owner].bind=old_bind;party[owner].silence=old_silence
 if accepted:
  if action in ["skill","support","light","break_free"]:decks[owner].power=true
  if action in ["skill","support","light"]:_gain_charge(owner)
  last_event["before_board"]=previous_board
  _counter_demon(str(last_event.get("element","")),last_event.effects)
 return accepted

func enemy_act() -> bool:
 if phase!="battle" or cursor!=2 or playback_pending:return false
 if not enemy_profile.is_empty():return _regional_enemy_act()
 var previous:=_before()
 var state: Dictionary=decks[2]
 var effects: Array[Dictionary]=[]
 var unit_attacks: Array[Dictionary]=[]
 for i in state.hand.size():
  var id: String=state.hand[i]
  var card:=Cards.get_card(id)
  if card.cost>state.crystals or (card.kind=="minion" and units(2).size()>=BOARD_LIMIT):continue
  var target: String=legal_targets(2,true).back()
  state.hand.remove_at(i);state.crystals-=int(card.cost)
  if card.kind!="minion":state.discard.append(id)
  var extra:=_resolve_card(2,id,target,effects)
  previous["enemy_card"]=extra
  break
 if phase=="battle":
  super.enemy_act()
  effects.append_array(last_event.effects)
 for unit in units(2):
  if phase!="battle":break
  if not unit.ready:continue
  unit.ready=false;unit.spent=true
  var targets:=legal_targets(2)
  if targets.is_empty():break
  unit_attacks.append({"uid":unit.uid,"target_code":targets[0]})
  if targets[0].begins_with("u:"):
   var defender:=find_unit(int(targets[0].substr(2)))
   _hurt_unit(unit,int(defender.attack),effects)
  _damage_target(2,targets[0],int(unit.attack),effects)
 if last_event.is_empty() or not playback_pending:
  _event("Nachtwurz sammelt Kraft","card_end",effects,previous)
 else:
  last_event["effects"]=effects;last_event["before_board"]=previous.before_board
  last_event["before_hp"]=previous.before_hp;last_event["before_enemy"]=previous.before_enemy
  last_event["before_status"]=previous.before_status
 if previous.has("enemy_card"):last_event["enemy_card"]=previous.enemy_card
 last_event["unit_attacks"]=unit_attacks
 pending_advance=true
 return true

func acknowledge_animation() -> void:
 var advance:=playback_pending and pending_advance
 super.acknowledge_animation()
 pending_advance=false
 if advance and phase=="battle":_advance_actor()

func _reset_fight() -> void:
 super._reset_fight()
 enemy_health=enemy_max_hp
 prepare(source_decks)
 _begin_turn(0)

func snapshot() -> Dictionary:
 var saved:=super.snapshot()
 saved["ecology_state"]=ecology_state.duplicate(true)
 saved["demon_charge"]=demon_charge;saved["seal_pending"]=seal_pending
 saved["card_schema"]=3;saved["fallen"]=fallen.duplicate(true);saved["card_decks"]=decks.duplicate(true);saved["board"]=board.duplicate(true)
 saved["special_charges"]=special_charges.duplicate();saved["special_used"]=special_used.duplicate()
 saved["source_decks"]=source_decks.duplicate(true);saved["next_uid"]=next_uid;saved["pending_advance"]=pending_advance
 return saved

func restore(saved: Dictionary) -> void:
 super.restore(saved)
 if saved.get("ecology_state",{}) is Dictionary and not saved.get("ecology_state",{}).is_empty():
  ecology_state=saved.ecology_state.duplicate(true)
  for key in ["step","phase","core_turns","resolve"]:ecology_state[key]=int(ecology_state.get(key,0))
  for key in ["open","fuel"]:ecology_state[key]=bool(ecology_state.get(key,false))
 demon_charge=bool(saved.get("demon_charge",false));seal_pending=bool(saved.get("seal_pending",false))
 for i in 2:
  var charges=saved.get("special_charges",[0,0]);var used=saved.get("special_used",[false,false])
  special_charges[i]=clampi(int(charges[i]),0,SPECIAL_CHARGES) if charges is Array and charges.size()==2 else 0
  special_used[i]=bool(used[i]) if used is Array and used.size()==2 else false
 if not saved.has("card_schema"):
  prepare(source_decks)
  if phase in ["unseen","intro"] and turn==0:enemy_health=enemy_max_hp
  if phase=="battle":
   for state in decks:state.cap=mini(10,maxi(1,round));state.crystals=state.cap
   _begin_turn(cursor)
  return
 enemy_health=clampi(int(saved.get("enemy_health",enemy_max_hp)),0,enemy_max_hp)
 var custom=saved.get("source_decks",{})
 if custom is Dictionary:source_decks=custom.duplicate(true)
 var states=saved.get("card_decks",[])
 if states is Array and states.size()==3:
  for owner in 3:
   if not states[owner] is Dictionary:continue
   var entry: Dictionary=states[owner]
   for zone in ["draw","hand","discard"]:
    decks[owner][zone]=[]
    var ids=entry.get(zone,[])
    if not ids is Array:continue
    var allowed:=Cards.available(str(decks[owner].species))
    for id in ids:
     if id is String and id in allowed and decks[owner][zone].size()<(HAND_LIMIT if zone=="hand" else Cards.MAX_DECK):decks[owner][zone].append(id)
   decks[owner].cap=clampi(int(entry.get("cap",1)),1,10)
   decks[owner].crystals=clampi(int(entry.get("crystals",1)),0,int(decks[owner].cap))
   decks[owner].power=bool(entry.get("power",false))
   decks[owner].turns=maxi(0,int(entry.get("turns",0)))
 fallen=[[],[],[]]
 var graves=saved.get("fallen",[])
 if graves is Array and graves.size()==3:
  for owner in 3:
   if not graves[owner] is Array:continue
   for id in graves[owner]:
    if id is String and id in Cards.available(str(decks[owner].species)) and Cards.get_card(id).kind=="minion" and fallen[owner].size()<Cards.MAX_DECK:fallen[owner].append(id)
 board=[]
 var entries=saved.get("board",[])
 if entries is Array:
  for raw in entries:
   if not raw is Dictionary:continue
   var owner:=clampi(int(raw.get("owner",0)),0,2)
   var uid:=int(raw.get("uid",0))
   if uid<=0 or not find_unit(uid).is_empty() or units(owner).size()>=BOARD_LIMIT:continue
   var hp:=clampi(int(raw.get("hp",0)),0,40)
   if hp==0:continue
   var species:=str(raw.get("species",""))
   if species not in SPECIES and species!="enemy":species=""
   board.append({"uid":uid,"owner":owner,"id":str(raw.get("id","cat")),"name":str(raw.get("name","Helfer")).substr(0,30),
    "attack":clampi(int(raw.get("attack",0)),0,30),"hp":hp,"max_hp":clampi(int(raw.get("max_hp",hp)),hp,40),
    "skipped":bool(raw.get("skipped",false)),"spent":bool(raw.get("spent",false)),"ready":bool(raw.get("ready",false)),"taunt":bool(raw.get("taunt",false)),
    "row":clampi(int(raw.get("row",-1)),-1,3),"species":species,
    "form":str(raw.get("form","")) if str(raw.get("form","")) in Cards.HELPER_FORMS else str(Cards.get_card(str(raw.get("id",""))).get("form","")),
    "evasion":bool(raw.get("evasion",false)),"frozen":clampi(int(raw.get("frozen",0)),0,1),
    "from_deck":bool(raw.get("from_deck",str(raw.get("id","")) in Cards.available(str(decks[owner].species)))),
    "shield":clampi(int(raw.get("shield",0)),0,20),"regen":clampi(int(raw.get("regen",0)),0,5),
    "death_draw":clampi(int(raw.get("death_draw",0)),0,2),"lifesteal":bool(raw.get("lifesteal",false))})
 if int(saved.get("card_schema",0))<3:
  for unit in board:
   if unit.from_deck:decks[int(unit.owner)].discard.erase(str(unit.id))
 next_uid=maxi(1,int(saved.get("next_uid",1)))
 for unit in board:next_uid=maxi(next_uid,int(unit.uid)+1)
 pending_advance=bool(saved.get("pending_advance",false)) and playback_pending
 if phase=="battle" and cursor<2 and party[cursor].hp<=0 and not playback_pending:_advance_actor()

func configure_enemy(id: String, profile: Dictionary) -> void:
 enemy_id=id;enemy_profile=profile.duplicate(true)
 enemy_name=str(profile.name);enemy_max_hp=int(profile.hp);enemy_health=enemy_max_hp

func begin() -> void:
 super.begin()
 if not enemy_profile.is_empty():message=enemy_name+" erscheint. "+str(enemy_profile.description)

func enemy_intent() -> String:
 if Ecology.handles(self):return ("Standfest · " if int(ecology_state.get("resolve",0))>0 else "")+Ecology.intent(self)
 if enemy_profile.is_empty():return super.enemy_intent()
 if enemy_stagger>0:return "Wanken · nächster Zug entfällt"
 match enemy_profile.kind:
  "moth":return "Pollenwirbel · beide"+(" · Verstummen" if round%3==0 else "")
  "hound":return "Glutsprung · 22 Schaden" if round%3==0 else "Aschenbiss · 14 Schaden"
  "imp":return "Glutwurf · 24 → "+str(party[(round-1)%2 if party[(round-1)%2].hp>0 else 1-(round-1)%2].name)+" · Wasser löscht" if demon_charge else "Glut sammeln · Feuerwurf vorbereiten"
  "weaver":return "Siegel · Anker zerstören oder schützen" if seal_pending and not _seal_anchor().is_empty() else "Siegel gebrochen · 12 Rückstoß" if seal_pending else "Siegelanker · Ritual vorbereiten"
  "guardian":return "Rindenerneuerung · 16 Heilung" if round%3==0 and enemy_burn==0 else "Wurzelschlag · 18 Schaden"
 return "Angriff"

func _regional_enemy_act() -> bool:
 if Ecology.handles(self):return Ecology.act(self)
 if enemy_profile.kind in ["imp","weaver"]:return _demon_enemy_act()
 var previous:=_before()
 var effects: Array[Dictionary]=[]
 var title:=enemy_intent()
 var visual: String={"moth":"wind","hound":"fire","guardian":"nature"}[enemy_profile.kind]
 if enemy_stagger>0:
  enemy_stagger-=1
 elif enemy_profile.kind=="guardian" and round%3==0 and enemy_burn==0:
  var healed:=mini(16,enemy_max_hp-enemy_health);enemy_health+=healed
  effects.append({"target":2,"amount":healed,"kind":"heal"})
 else:
  var targets: Array[int]=[]
  if enemy_profile.kind=="moth":
   for i in 2:
    if party[i].hp>0:targets.append(i)
  else:
   var target: int=(round-1)%2
   if party[target].hp<=0:target=1-target
   targets.append(target)
  for target in targets:
   var damage: int=9 if enemy_profile.kind=="moth" else 18 if enemy_profile.kind=="guardian" else 22 if round%3==0 else 14
   if enemy_weaken>0:damage=int(ceil(damage*.65))
   if party[target].guard:damage=int(ceil(damage*.35))
   var bypassed:=_receive_hit(target,damage,effects)
   if enemy_profile.kind=="moth" and round%3==0 and bypassed>0 and party[target].hp>0:
    party[target].silence=1;effects.append({"target":target,"amount":1,"kind":"silence"})
  if party[0].hp<=0 and party[1].hp<=0:phase="defeat"
 if enemy_burn>0:
  enemy_burn-=1
  var burn:=mini(6,enemy_health);enemy_health-=burn
  effects.append({"target":2,"amount":burn,"kind":"burn"})
  if enemy_health==0 and phase!="defeat":phase="victory"
 if enemy_weaken>0:enemy_weaken-=1
 _event(enemy_name+" · "+title,visual,effects,previous,{"element":visual})
 pending_advance=true
 return true


func _counter_demon(element: String,effects: Array) -> void:
 Ecology.counter(self,element,effects)
 if enemy_profile.get("kind","")=="imp" and demon_charge and element=="water":
  demon_charge=false
  effects.append({"target":2,"amount":0,"kind":"release"})
  message+=" · Glut gelöscht"

func _seal_anchor() -> Dictionary:
 for unit in units(2):
  if unit.id=="seal_anchor":return unit
 return {}

func _break_seal() -> void:
 var anchor:=_seal_anchor()
 if not anchor.is_empty():board.erase(anchor)
 seal_pending=false

func _demon_enemy_act() -> bool:
 var previous:=_before()
 var effects: Array[Dictionary]=[]
 var title:=enemy_intent()
 var element: String="fire" if enemy_profile.kind=="imp" else "shadow"
 var target: int=(round-1)%2
 if party[target].hp<=0:target=1-target
 if enemy_stagger>0:
  enemy_stagger-=1;demon_charge=false;_break_seal()
  title="Ritual unterbrochen · Zug entfällt"
 elif enemy_profile.kind=="imp":
  if demon_charge:
   var damage:=24
   if enemy_weaken>0:damage=int(ceil(damage*.65))
   if party[target].guard:damage=int(ceil(damage*.35))
   _receive_hit(target,damage,effects)
   demon_charge=false
  else:demon_charge=true
 elif not seal_pending:
  _add_minion(2,"seal_anchor",{"name":"Siegelanker","attack":0,"health":10,"taunt":true,"species":"enemy","form":"night_guard"})
  seal_pending=true
 elif _seal_anchor().is_empty():
  _damage_target(0,"enemy",12,effects);seal_pending=false
 else:
  for i in 2:
   if party[i].hp<=0:continue
   var damage:=10
   if enemy_weaken>0:damage=int(ceil(damage*.65))
   if party[i].guard:damage=int(ceil(damage*.35))
   var penetrated:=_receive_hit(i,damage,effects)
   if penetrated>0 and party[i].hp>0:
    party[i].silence=1;effects.append({"target":i,"amount":1,"kind":"silence"})
  _break_seal()
 if party[0].hp<=0 and party[1].hp<=0:phase="defeat"
 if enemy_burn>0:
  enemy_burn-=1
  var damage:=mini(6,enemy_health);enemy_health-=damage
  effects.append({"target":2,"amount":damage,"kind":"burn"})
  if enemy_health==0 and phase!="defeat":phase="victory"
 if enemy_weaken>0:enemy_weaken-=1
 _event(enemy_name+" · "+title,"card_spell",effects,previous,{"element":element,"duration":2.0,"impact":.85,"target_code":"h:"+str(target)})
 pending_advance=true
 return true

func _modify_enemy_damage(amount: int) -> int:return Ecology.damage(self,amount)
func _after_enemy_damage() -> void:Ecology.after_damage(self)

func claim_victory() -> bool:
 var claimed:=super.claim_victory()
 if claimed and enemy_profile.get("kind","")=="boss":
  for member in result.members:
   experience[member.species]=int(experience[member.species])+240
   member.gained+=240;member.total=experience[member.species]
  inventory.moon_shard=int(inventory.get("moon_shard",0))+2
  for item in result.items:
   if item.id=="moon_shard":item.count+=2
 return claimed
