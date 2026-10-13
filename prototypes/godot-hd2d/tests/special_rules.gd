extends SceneTree
const Battle=preload("res://scripts/card_battle.gd")
const Cards=preload("res://scripts/card_catalog.gd")
var checks:=0
var failures:=0
func _initialize() -> void:call_deferred("run")
func check(ok: bool,label: String) -> void:
 checks+=1
 if not ok:failures+=1;push_error(label)
func fresh(species: String):
 var b=Battle.new();b.configure(species);b.prepare({});b.begin();b.advance_intro(3.2);return b
func run() -> void:
 for species in Battle.SPECIES:
  var b=fresh(species)
  check(not b.can_act("special"),"Spezialkraft beginnt ungeladen "+species)
  b.act("skill");b.acknowledge_animation()
  check(b.special_charges[0]==1,"Superkraft lädt Spezialkraft "+species)
  b.decks[0].hand=["spark","leaf_guard","insight"];b.decks[0].cap=3;b.decks[0].crystals=3
  for i in 3:b.play_card(0,"enemy");b.acknowledge_animation()
  check(b.special_charges[0]==4,"Bezahlte Karten laden ohne Zugende "+species)
  b.decks[0].hand=[Cards.SPECIAL[species][0]];b.decks[0].cap=1;b.decks[0].crystals=1
  b.play_card(0);b.acknowledge_animation();b.party[0].hp-=20
  check(b.special_charges[0]==5 and b.can_act("special"),"Fünf Aktionen schalten Spezialkraft frei "+species)
  var r=Battle.new();r.configure(species);r.prepare({});r.restore(JSON.parse_string(JSON.stringify(b.snapshot())))
  check(r.special_charges[0]==5 and r.can_act("special"),"Geladene Spezialkraft gespeichert "+species)
  check(r.act("special") and r.special_used[0] and r.special_charges[0]==0 and r.cursor==0,"Spezialkraft einmal ohne Kristallkosten "+species)
  var hp: int=r.enemy_health;var board: Array=r.board.duplicate(true)
  var resumed=Battle.new();resumed.configure(species);resumed.prepare({});resumed.restore(JSON.parse_string(JSON.stringify(r.snapshot())))
  resumed.acknowledge_animation();resumed.acknowledge_animation()
  check(resumed.enemy_health==hp and resumed.board==board and not resumed.can_act("special"),"Spezialeffekt nach Laden genau einmal "+species)
  resumed.phase="defeat";resumed.retry()
  check(resumed.special_charges==[0,0] and resumed.special_used==[false,false],"Wiederholung setzt Spezialzustand zurück "+species)
  var deck: Array=Cards.starter(species);deck[-1]=Cards.ULTIMATES[species]
  check(not Cards.valid(species,deck),"Spezialkarte höchstens einmal im Deck "+species)
 var b=fresh("puffling");b.special_charges[0]=5;b.act("special");b.acknowledge_animation()
 check(b.units(0).size()==2 and b.units(0)[0].ready and b.units(0)[1].ready,"Sturmfalken sofort bereit")
 var uid: int=b.units(0)[0].uid;b.attack_minion(uid,"enemy");b.acknowledge_animation()
 check(not b.find_unit(uid).ready,"Helferangriff erschöpft")
 b.act("support");b.acknowledge_animation();check(b.find_unit(uid).ready,"Rückenwind macht Helfer erneut bereit")
 b=fresh("nocti");b.party[0].hp=30;b.decks[0].hand=["echo"];b.decks[0].cap=2;b.decks[0].crystals=2;b.play_card(0,"enemy");b.acknowledge_animation()
 check(b.party[0].hp==36 and b.enemy_health==205,"Echobiss trifft und heilt")
 b.special_charges[0]=5;b.act("special");b.acknowledge_animation()
 uid=b.units(0)[0].uid;var hits: Array[Dictionary]=[]
 b._damage_target(2,"u:"+str(uid),9,hits)
 check(b.find_unit(uid).hp==8 and not b.find_unit(uid).evasion and hits[0].kind=="evade","Spiegelgeist weicht erstem Treffer aus")
 b._damage_target(2,"u:"+str(uid),9,hits);check(b.find_unit(uid).is_empty(),"Zweiter Treffer trifft Spiegelgeist")
 b=fresh("hootlet");uid=b._add_minion(2,"thornling");b.special_charges[0]=5;b.act("special");b.acknowledge_animation()
 b.act("end_turn");b.acknowledge_animation();b.act("end_turn");b.acknowledge_animation()
 check(not b.find_unit(uid).ready,"Mondfinsternis verhindert nächste Helferbereitschaft")
 b.enemy_act();check(b.last_event.unit_attacks.is_empty(),"Eingefrorener Gegnerhelfer greift nicht an")
 b.acknowledge_animation();b.cursor=2;b._begin_turn(2)
 check(b.find_unit(uid).ready,"Eingefrorener Helfer danach wieder bereit")
 b=fresh("gloop");b.party[0].hp=30;b.party[0].bind=1;b.party[0].silence=2
 b.decks[0].hand=["tide"];b.decks[0].cap=4;b.decks[0].crystals=4
 check(b.play_card(0) and b.party[0].hp==50 and b.party[0].bind==0 and b.party[0].silence==0,"Gezeitenkarte heilt und reinigt trotz verstummter Superkraft")
 print("Spezialregeln: %s Checks, %s Fehler" % [checks,failures]);quit(1 if failures else 0)
