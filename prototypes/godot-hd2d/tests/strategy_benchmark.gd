extends SceneTree
const B=preload("res://scripts/card_battle.gd")
const C=preload("res://scripts/card_catalog.gd")
const E=preload("res://scripts/regional_enemies.gd")
var report: Array=[]
func _initialize() -> void:call_deferred("run")
func target(b,choices: Array[String],card: Dictionary={}) -> String:
 if choices.is_empty():return "enemy"
 if card.get("strategy_target","")=="ally" or card.get("target","")=="ally_unit":
  var best: String=choices[0];var score: float=-999
  for code in choices:
   var u: Dictionary=b.find_unit(int(code.substr(2)))
   var value: float=10 if u.taunt else 0
   value+=int(u.max_hp)-int(u.hp) if card.kind=="mend" else b.unit_attack(u)
   if card.get("ability","")=="Blutpakt":value=-float(u.hp)-b.unit_attack(u)
   if value>score:best=code;score=value
  return best
 for code in choices:
  if code.begins_with("u:"):
   var u: Dictionary=b.find_unit(int(code.substr(2)))
   if str(u.id).contains("seal"):return code
 if "enemy" in choices:return "enemy"
 return choices[0]
func player_step(b) -> bool:
 if b.can_act("special"):return b.act("special")
 var best: int=-1;var score: float=-999;var best_target: String="enemy"
 for i in b.decks[b.cursor].hand.size():
  if not b.can_play(i):continue
  var card:=C.get_card(b.decks[b.cursor].hand[i]);var t:=target(b,b.card_targets(b.cursor,card),card)
  if not b.can_play(i,t):continue
  var value: float=10+int(card.cost)
  if card.kind=="minion":value+=2
  if card.kind=="draw" and b.decks[b.cursor].hand.size()>=4:continue
  if card.kind=="energy":value+=10
  if card.kind=="damage" and card.value>=b.enemy_health:value+=100
  if value>score:best=i;score=value;best_target=t
 if best>=0:return b.play_card(best,best_target)
 for u in b.units(b.cursor):
  if b.can_attack(int(u.uid)):return b.attack_minion(int(u.uid),target(b,b.legal_targets(b.cursor)))
 if b.can_act("break_free"):return b.act("break_free")
 var wounded: bool=b.party[b.cursor].hp<b.party[b.cursor].max_hp*.55
 if wounded and b.can_act("support"):return b.act("support")
 if b.can_act("skill"):return b.act("skill")
 if b.can_act("support"):return b.act("support")
 return b.act("end_turn")
func run() -> void:
 for species in ["fennec","starlet","hootlet"]:
  for style in ["Startdeck","Tempo","Bollwerk","Motor"]:
   for enemy in ["night","thorn_moth","bark_guardian","seal_weaver","vharok"]:
    for seed in [11,29,73]:
     var b=B.new();b.configure(species)
     if enemy!="night":b.configure_enemy(enemy,E.data()[enemy])
     var custom: Dictionary={}
     if style!="Startdeck":
      for member in b.party:custom[str(member.species)]=C.strategic_starter(str(member.species),style)
     b.set_random_seed(seed);b.prepare(custom)
     b.begin();b.advance_intro(3.2)
     var steps:=0;var stuck:=false
     while b.phase=="battle" and steps<320 and b.round<35:
      var ok: bool=b.enemy_act() if b.cursor==2 else player_step(b)
      if not ok:stuck=true;break
      b.acknowledge_animation();steps+=1
     report.append({"species":species,"style":style,"enemy":enemy,"seed":seed,"phase":b.phase,"rounds":b.round,"steps":steps,"hp":int(b.party[0].hp)+int(b.party[1].hp),"stuck":stuck})
 var file:=FileAccess.open("user://strategy-benchmark.json",FileAccess.WRITE);file.store_string(JSON.stringify(report,"  "));file.close()
 var stuck:=0
 for r in report:
  if r.stuck:stuck+=1
 print("Deckvergleich: %s Kämpfe, %s blockierte Abläufe · Bericht: %s" % [report.size(),stuck,ProjectSettings.globalize_path("user://strategy-benchmark.json")]);quit(1 if stuck else 0)
