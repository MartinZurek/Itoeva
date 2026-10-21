extends SceneTree
const Catalog=preload("res://scripts/world_catalog.gd")
const Battle=preload("res://scripts/card_battle.gd")
const Cards=preload("res://scripts/card_catalog.gd")
const Art=preload("res://scripts/card_assets.gd")
var checks:=0
var failures:=0

func _initialize() -> void:call_deferred("run")
func check(ok: bool,label: String) -> void:
 checks+=1
 if not ok:failures+=1;push_error(label)

func fresh(ids: Array=[],species: String="fennec"):
 var b=Battle.new();b.configure(species);b.set_random_seed(177)
 b.prepare({species:Cards.upgrade(species,ids)});b.begin();b.advance_intro(3.2)
 b.decks[0].draw.append_array(b.decks[0].hand);b.decks[0].hand=[]
 b.decks[0].cap=10;b.decks[0].crystals=10
 return b

func take(b,id: String) -> int:
 var state: Dictionary=b.decks[0]
 if id in state.draw:state.draw.erase(id);state.hand.append(id)
 return state.hand.find(id)

func total(b,owner: int=0) -> int:
 var state: Dictionary=b.decks[owner]
 var n: int=state.draw.size()+state.hand.size()+state.discard.size()
 for unit in b.units(owner):
  if unit.get("from_deck",false):n+=1
 return n

func roundtrip(b):
 var r=Battle.new();r.configure(b.party[0].species);r.prepare({})
 r.restore(JSON.parse_string(JSON.stringify(b.snapshot())));return r

func run() -> void:
 check(Cards.CARDS.size()==89,"89 unterschiedliche Kartendefinitionen")
 for species in Catalog.STARTERS:
  var deck:=Cards.starter(species)
  check(deck.size()==40 and Cards.valid(species,deck),"40 Karten je Wesen: "+species)
  var unique: Dictionary={}
  for id in deck:unique[id]=true
  check(unique.size()==21 and deck.count(Cards.ULTIMATES[species])==1,"21 verschiedene Karten und eine Spezialkarte: "+species)
  check(Cards.available(species).size()==646,"30 bisherige + 616 neue Deckbauoptionen je Klasse: "+species)
  var old: Array=["spark","spark","tea","insight",Cards.SPECIAL[species][0],Cards.SPECIAL[species][0],Cards.ULTIMATES[species]]
  var migrated:=Cards.upgrade(species,old)
  check(Cards.valid(species,migrated) and migrated.count("spark")==2 and migrated.count(Cards.SPECIAL[species][0])==2,"Alte Auswahl bleibt beim Auffuellen erhalten: "+species)
  check(Cards.upgrade(species,migrated)==migrated,"Migration ist idempotent: "+species)
  var b=fresh([],species)
  check(total(b)==40 and total(roundtrip(b))==40,"40 Karten nach Vorbereitung und Laden: "+species)
  var invalid: Array=deck.duplicate();invalid[-1]="spark"
  check(not Cards.valid(species,invalid),"Dritte normale Kopie verboten: "+species)
  invalid=deck.duplicate();invalid[0]=Cards.ULTIMATES[species]
  check(not Cards.valid(species,invalid),"Zweite Spezialkopie verboten: "+species)
  invalid=deck.duplicate();invalid.remove_at(0)
  check(not Cards.valid(species,invalid),"39 Karten nicht speicherbar: "+species)
 for id in Cards.CARDS:
  var card:=Cards.get_card(id)
  check(is_instance_valid(Art.texture(card)),"Kartenkunst vorhanden: "+id)
 var new_forms: Dictionary=JSON.parse_string(FileAccess.get_file_as_string("res://data/helper_animations_v060.json")).forms
 check(new_forms.size()==16,"16 individuelle neue Helfer")
 for form in new_forms:
  var info: Dictionary=new_forms[form]
  check(info.regions.size()==6,"Sechs Zeichnungen: "+form)
  for action in ["idle","attack","hit"]:
   for progress in [0.0,.25,.5,.75,1.0]:
    var frame:=Art.animation_frame(form,progress,action,progress)
    var t:=Art.form_texture(form,frame)
    check(frame>=0 and frame<6 and t.region.size.x>0 and t.region.size.y>0,"Gueltiger Animationsframe: "+form+" "+action)
 var b=fresh(["lantern_snail","homeward","forest_memory","trail_blessing"])
 var index:=take(b,"lantern_snail")
 check(b.play_card(index),"Schnecke beschwoeren");b.acknowledge_animation()
 var uid: int=b.units(0)[0].uid
 check(total(b)==40 and b.find_unit(uid).shield==4,"Beschwoerung konserviert Karten und gibt Schild")
 index=take(b,"homeward")
 var before: Dictionary=b.snapshot()
 check(not b.play_card(index,"enemy") and b.snapshot()==before,"Falsches Rueckrufziel verbraucht nichts")
 check(b.play_card(index,"u:"+str(uid)),"Rueckruf auf die Hand")
 var r=roundtrip(b);r.acknowledge_animation();r.acknowledge_animation()
 check(total(r)==40 and r.units(0).is_empty() and r.decks[0].hand.count("lantern_snail")==1,"Rueckruf erzeugt auch nach Laden keine Kopie")
 index=r.decks[0].hand.find("lantern_snail");r.play_card(index);r.acknowledge_animation()
 uid=r.units(0)[0].uid
 var effects: Array[Dictionary]=[];r._damage_target(2,"u:"+str(uid),30,effects)
 check(total(r)==40 and r.fallen[0]==["lantern_snail"],"Tod legt Helfer genau einmal ab")
 index=take(r,"forest_memory")
 check(r.play_card(index),"Gefallenen Helfer wiederbeschwoeren")
 var resumed=roundtrip(r);resumed.acknowledge_animation()
 check(total(resumed)==40 and resumed.fallen[0].is_empty() and resumed.units(0).size()==1,"Wiederbelebung konserviert Karten nach Laden")
 b=fresh(["leaf_beetle"]);b.play_card(take(b,"leaf_beetle"));b.acknowledge_animation()
 uid=b.units(0)[0].uid;var hand: int=b.decks[0].hand.size()
 effects=[];b._damage_target(2,"u:"+str(uid),30,effects);b._remove_dead();b._remove_dead()
 check(b.decks[0].hand.size()==hand+1 and total(b)==40,"Todeskarte zieht genau einmal")
 b=fresh(["mushroom_archivist"]);b.play_card(take(b,"mushroom_archivist"));b.acknowledge_animation()
 check(b.decks[0].hand.size()==1 and total(b)==40,"Archivar zieht beim Erscheinen eine Karte")
 b=fresh(["thistle_nurse","trail_blessing","bark_ward","dew_mend"])
 b.play_card(take(b,"thistle_nurse"));b.acknowledge_animation();uid=b.units(0)[0].uid
 b.find_unit(uid).hp=3;b.round=10;b._begin_turn(0)
 check(b.find_unit(uid).hp==5,"Eigene Regeneration am Zugbeginn")
 b.play_card(take(b,"trail_blessing"),"u:"+str(uid));b.acknowledge_animation()
 check(b.find_unit(uid).attack==5 and b.find_unit(uid).max_hp==12 and b.find_unit(uid).hp==8,"Verstaerkung erhoeht Angriff und echte LP")
 b.play_card(take(b,"bark_ward"),"u:"+str(uid));b.acknowledge_animation()
 effects=[];b._damage_target(2,"u:"+str(uid),6,effects)
 check(b.find_unit(uid).hp==8 and b.find_unit(uid).shield==1,"Schild absorbiert vor LP")
 b.find_unit(uid).hp=2;b.play_card(take(b,"dew_mend"),"u:"+str(uid));b.acknowledge_animation()
 check(b.find_unit(uid).hp==10,"Helfer gezielt heilen")
 r=roundtrip(b)
 check(r.find_unit(uid).regen==2 and r.find_unit(uid).shield==1 and total(r)==40,"Neue Helferwerte bleiben erhalten")
 b=fresh(["frost_thread"]);var foe: int=b._add_minion(2,"thornling")
 index=take(b,"frost_thread");before=b.snapshot()
 check(not b.play_card(index,"enemy") and b.snapshot()==before,"Festhalten betrifft nur Gegnerhelfer")
 b.play_card(index,"u:"+str(foe));b.acknowledge_animation();b._begin_turn(2)
 check(not b.find_unit(foe).ready,"Festhalten verhindert naechste Bereitschaft")
 b._begin_turn(2);check(b.find_unit(foe).ready,"Danach wieder bereit")
 b=fresh(["mist_glider"]);b.play_card(take(b,"mist_glider"));b.acknowledge_animation()
 uid=b.units(0)[0].uid;b.find_unit(uid).ready=true;b.party[0].hp=20
 b.enemy_health=2;b.attack_minion(uid,"enemy")
 check(b.party[0].hp==22,"Lebensraub heilt nur wirklichen Schaden, keinen Ueberschuss")
 b=fresh(["fresh_sap"]);b.decks[0].crystals=9
 b.play_card(take(b,"fresh_sap"));b.acknowledge_animation()
 check(b.decks[0].crystals==10,"Kristallgewinn bleibt unter Zugobergrenze")
 var old=fresh();old.decks[0].draw=["spark","spark","cat","cat","boar","boar","wolf","wolf"]
 old.decks[0].hand=["tea","insight"];old.decks[0].discard=["leaf_guard","cat"]
 uid=old._add_minion(0,"cat");var saved: Dictionary=old.snapshot();saved.card_schema=2
 r=Battle.new();r.restore(saved)
 check(r.phase=="battle" and r.units(0).size()==1 and r.decks[0].draw.size()==8,"Laufender alter Kampf bleibt erhalten")
 check(r.decks[0].discard==["leaf_guard"],"Alte doppelte Brett-/Ablagekarte wird bereinigt")
 print("40-Karten-Erweiterung: %s Checks, %s Fehler" % [checks,failures])
 quit(1 if failures else 0)
