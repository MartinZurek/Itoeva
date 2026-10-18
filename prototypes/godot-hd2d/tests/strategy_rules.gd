extends SceneTree
const Battle=preload("res://scripts/card_battle.gd")
const Cards=preload("res://scripts/card_catalog.gd")
const S=preload("res://scripts/strategy_cards.gd")
var checks:=0
var failures:=0
func _initialize() -> void:call_deferred("run")
func check(ok: bool,label: String) -> void:
 checks+=1
 if not ok:failures+=1;push_error(label)
func battle():
 var b=Battle.new();b.begin();b.advance_intro(3.2);return b
func pick(key: String,value: String) -> String:
 for card in S.data().cards.values():
  if card.get(key,"")==value:return str(card.id)
 return ""
func run() -> void:
 check(S.ids().size()==616,"616 Karten verfügbar")
 for species in ["fennec","starlet","wyrmling","gloop","puffling","hootlet","mosslyn","nocti"]:
  for style in ["Tempo","Bollwerk","Motor"]:
   var preset:=Cards.strategic_starter(species,style)
   check(Cards.valid(species,preset),"40 legale Karten: "+species+" "+style)
   var early:=0;var mythical:=0
   for id in preset:
    if Cards.get_card(id).cost<=2:early+=1
    if Cards.get_card(id).get("strategy",false) and Cards.get_card(id).rare:mythical+=1
   check(early>=10 and mythical==2,"Frühspiel und begrenzte Finisher: "+style)
 for id in S.ids():
  var c:=Cards.get_card(id)
  check(c.cost>=1 and c.cost<=7 and c.attack>=1 and c.health>=2,"Gültiger Körper "+id)
  check(Cards.copy_limit("fennec",id)==(1 if c.rare else 2),"Kopienlimit "+id)
  var b=battle();b._add_minion(0,"cat");b._add_minion(2,"thornling")
  b.decks[0].crystals=10;b.decks[0].cap=10;b.decks[0].hand=[id]
  var choices: Array=b.card_targets(0,c)
  var target: String=choices[0] if not choices.is_empty() else "enemy"
  check(b.play_card(0,target),"Karte spielbar "+id)
  var restored=Battle.new();restored.restore(JSON.parse_string(JSON.stringify(b.snapshot())))
  check(restored.board==b.board and restored.decks==b.decks and restored.playback_pending,"Snapshot samt Effekten "+id)
  restored.acknowledge_animation();restored.acknowledge_animation()
  check(restored.board==b.board,"Animation löst Effekt nicht erneut aus "+id)
 var b=battle()
 var learn_id:=pick("ability","Lernen")
 var learner:=b._add_minion(0,learn_id);var initial:=int(b.find_unit(learner).attack)
 var left:=int(b.find_unit(learner).learn_left)
 for i in 5:S.cards_played(b,0)
 check(b.find_unit(learner).attack==initial+left and b.find_unit(learner).learn_left==0,"Lernen ist endlich")
 b=battle();var ward_id:=pick("gift","Zuflucht");b._add_minion(0,ward_id)
 var cat:=b._add_minion(0,"cat");var hits: Array[Dictionary]=[]
 b._hurt_unit(b.find_unit(cat),50,hits)
 check(b.find_unit(cat).hp==1,"Zuflucht rettet anderen Helfer")
 b._hurt_unit(b.find_unit(cat),1,hits);b._remove_dead()
 check(b.find_unit(cat).is_empty(),"Zuflucht nur einmal pro Zug")
 b=battle();var reborn:=b._add_minion(0,pick("gift","Wiedererblühen"))
 b.find_unit(reborn).hp=0;b._remove_dead()
 check(b.find_unit(reborn).hp==1 and b.find_unit(reborn).reborn and not b.find_unit(reborn).ready,"Einmalige erschöpfte Wiederkehr")
 b.find_unit(reborn).hp=0;b._remove_dead();check(b.find_unit(reborn).is_empty(),"Keine ewige Wiederkehr")
 b=battle();var armor:=b._add_minion(0,pick("ability","Panzer"));var hp:=int(b.find_unit(armor).hp)
 b._hurt_unit(b.find_unit(armor),0,hits);check(b.find_unit(armor).hp==hp,"Panzer erzeugt keinen Schaden aus Null")
 b._hurt_unit(b.find_unit(armor),1,hits);check(b.find_unit(armor).hp==hp-1,"Panzer lässt mindestens einen Schaden durch")
 b=battle();var token_source:=b._add_minion(0,pick("gift","Kristallherz"))
 b.find_unit(token_source).hp=0;b._remove_dead()
 check(b.units(0).size()==1 and b.units(0)[0].id=="forge_erzsplitter" and b.units(0)[0].hp==3,"Kristallherz erzeugt korrekten Spielhelfer")
 b=battle();var combo_card:=Cards.get_card(pick("ability","Glück"));var first:=b._add_minion(0,combo_card.id)
 var old_attack:=int(b.find_unit(first).attack);b.decks[0].cards_played=1
 var second:=b._add_minion(0,combo_card.id)
 check(b.find_unit(second).attack==old_attack+int(combo_card.ability_value),"Kunststück belohnt Reihenfolge statt Münzwurf")
 b=battle();var gear:=b._add_minion(0,pick("gift","Fließband"));b._begin_turn(0)
 check(b.units(0).size()==2 and b.units(0)[1].id=="forge_werkling" and b.units(0)[1].hp==1,"Fließband erzeugt 1/1 ohne Affinitätsbonus")
 print("Strategiekarten: %s Checks, %s Fehler" % [checks,failures]);quit(1 if failures else 0)
