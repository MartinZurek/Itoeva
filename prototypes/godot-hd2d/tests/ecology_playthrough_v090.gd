extends SceneTree
const Catalog=preload("res://scripts/world_catalog.gd")
const Battle=preload("res://scripts/card_battle.gd")
const Enemies=preload("res://scripts/regional_enemies.gd")
var wins:=0
var failures:=0
func _initialize() -> void:call_deferred("run")
func run() -> void:
 for id in ["tide_crab","resin_beetle","cinder_carrier","vharok"]:
  for species in Catalog.STARTERS:
   for seed_value in [55,777,991,2026]:
    var b=Battle.new();b.configure(species);b.configure_enemy(id,Enemies.data()[id]);b.set_random_seed(seed_value);b.prepare({});b.begin();b.advance_intro(3.2)
    for step in 400:
     if b.phase!="battle":break
     if b.cursor==2:b.enemy_act();b.acknowledge_animation();continue
     if b.can_act("break_free"):b.act("break_free");b.acknowledge_animation()
     var owner: int=b.cursor
     var power: String="support" if b.party[owner].species=="starlet" and b.party[owner].barrier<15 else "skill"
     if b.can_act(power):b.act(power);b.acknowledge_animation()
     if b.can_act("special"):b.act("special");b.acknowledge_animation()
     for unit in b.units(owner).duplicate():
      if b.phase!="battle":break
      var target: String=b.legal_targets(owner)[0]
      if b.can_attack(int(unit.uid),target):b.attack_minion(int(unit.uid),target);b.acknowledge_animation()
     for attempt in 12:
      var played:=false
      for index in b.decks[owner].hand.size():
       var card: Dictionary=b.Cards.get_card(b.decks[owner].hand[index])
       var choices: Array=b.card_targets(owner,card)
       var target: String=choices[0] if not choices.is_empty() else "enemy"
       if b.can_play(index,target):b.play_card(index,target);b.acknowledge_animation();played=true;break
      if not played or b.phase!="battle":break
     if b.phase=="battle":b.act("end_turn");b.acknowledge_animation()
    var expected:=360 if id=="vharok" else 120
    if b.phase=="victory" and int(b.experience.get(species,0))==expected:wins+=1
    else:failures+=1;push_error("Unspielbarer Durchlauf: "+id+" / "+species)
    if id=="vharok" and (b.round<4 or b.round>18):failures+=1;push_error("Bossdauer außerhalb des Zielbereichs")
    print(JSON.stringify({"enemy":id,"hero":species,"seed":seed_value,"phase":b.phase,"rounds":b.round,"hp":[b.party[0].hp,b.party[1].hp],"xp":b.experience.get(species,0)}))
 print("Ecology playthrough: %s / 128 victories, %s failures"%[wins,failures]);quit(1 if failures else 0)
