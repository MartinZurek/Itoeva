extends SceneTree
const Catalog=preload("res://scripts/world_catalog.gd")
const Battle=preload("res://scripts/card_battle.gd")
const Cards=preload("res://scripts/card_catalog.gd")
var wins:=0
func _initialize() -> void:call_deferred("run")
func run() -> void:
 for species in Catalog.STARTERS:
  var b=Battle.new();b.configure(species);b.set_random_seed(777);b.prepare({});b.begin();b.advance_intro(3.2)
  for step in 300:
   if b.phase!="battle":break
   if b.cursor==2:b.enemy_act();b.acknowledge_animation();continue
   if b.can_act("break_free"):b.act("break_free");b.acknowledge_animation()
   var owner: int=b.cursor
   var power: String="support" if b.party[owner].species=="starlet" and b.party[owner].barrier<15 else "skill"
   if b.can_act(power):b.act(power);b.acknowledge_animation()
   for unit in b.units(owner).duplicate():
    if b.phase!="battle":break
    var target: String=b.legal_targets(owner)[0]
    if b.can_attack(int(unit.uid),target):b.attack_minion(int(unit.uid),target);b.acknowledge_animation()
   for i in 8:
    var played:=false
    for index in b.decks[owner].hand.size():
     var target: String="enemy"
     if b.can_play(index,target):
      b.play_card(index,target);b.acknowledge_animation();played=true;break
    if not played or b.phase!="battle":break
   if b.phase=="battle":b.act("end_turn");b.acknowledge_animation()
  print(species,": ",b.phase," · Runde ",b.round," · LP ",b.party[0].hp," / ",b.party[1].hp)
  if b.phase=="victory" and b.experience.get(species,0)==120:wins+=1
 print("Vollständige Kartenkämpfe: %s / %s gewonnen" % [wins,Catalog.STARTERS.size()])
 quit(0 if wins==Catalog.STARTERS.size() else 1)
