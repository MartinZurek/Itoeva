extends RefCounted
# Der Faktorkatalog liefert Identitäten; dieser versionierte Katalog liefert Spielwerte.
static var _cache: Dictionary={}
static func data() -> Dictionary:
 if _cache.is_empty():
  var loaded=JSON.parse_string(FileAccess.get_file_as_string("res://data/strategy_cards.json"))
  assert(loaded is Dictionary and loaded.get("schema_version",0)==1,"Ungültiger Strategiekatalog")
  _cache=loaded
 return _cache
static func ids() -> Array[String]:
 var result: Array[String]=[]
 result.assign(data().cards.keys());return result
static func get_card(id: String) -> Dictionary:
 var result: Dictionary=data().cards.get(id,data().tokens.get(id,{})).duplicate(true)
 for key in ["cost","attack","health","ability_x","ability_value","source_cost"]:
  if result.has(key):result[key]=int(result[key])
 return result
static func has(id: String) -> bool:return data().cards.has(id) or data().tokens.has(id)
static func affinity(card: Dictionary,species: String) -> bool:
 return card.get("affinity","")==species or (species=="mosslyn" and card.get("volk","") in ["Kräutervolk","Erntevolk"]) or (species=="nocti" and card.get("volk","") in ["Artefakte","Irrlichter"])

static func preset(species: String,style: String) -> Array[String]:
 var result: Array[String]=[]
 var spells: Array={"Tempo":["spark","frost_thread","second_wind","trail_blessing","leaf_volley","insight","homeward"],"Bollwerk":["leaf_guard","tea","bark_ward","dew_mend","root_pact","shared_rest","forest_memory"],"Motor":["insight","homeward","second_wind","dew_mend","root_pact","fresh_sap","forest_memory"]}.get(style,[])
 if spells.is_empty():return result
 for id in spells:result.append(id);result.append(id)
 var preferred: Array={"Tempo":["Gauklervolk","Moorglut","Glimmerwerk"],"Bollwerk":["Kräutervolk","Leuchtfeuer","Erzvolk","Bauvolk"],"Motor":["Artefakte","Hortbrut","Werkvolk","Irrlichter"]}[style]
 for bucket in 4:
  var pool: Array=[]
  for card in data().cards.values():
   var cost:=int(card.cost)
   var group:=3 if card.rare else 0 if cost<=2 else 1 if cost<=4 else 2
   if group!=bucket:continue
   var score: float=(8.0 if card.volk in preferred else 0.0)+(3.0 if affinity(card,species) else 0.0)
   if style=="Tempo":score+=float(card.attack)/maxi(1,cost)*2.0+(3 if card.ready else 0)
   elif style=="Bollwerk":score+=float(card.health)/maxi(1,cost)+(2 if card.taunt else 0)
   else:score+=2 if card.ability in ["Lernen","Widerhall","Zins","Aufrüsten"] else 0
   pool.append({"id":card.id,"score":score})
  pool.sort_custom(func(a,b):return a.score>b.score if a.score!=b.score else a.id<b.id)
  var count:=5 if bucket<2 else 2
  for i in mini(count,pool.size()):
   result.append(pool[i].id)
   if bucket!=3:result.append(pool[i].id)
 return result

static func grow(u: Dictionary,attack: int,health: int) -> void:
 u.attack=clampi(int(u.attack)+attack,0,30)
 var gained:=mini(health,40-int(u.max_hp));u.max_hp+=gained;u.hp=mini(int(u.max_hp),int(u.hp)+gained)
static func heal(u: Dictionary,amount: int) -> void:u.hp=mini(int(u.max_hp),int(u.hp)+amount)
static func own(b,owner: int) -> Array:
 var result: Array=[]
 for u in b.units(owner):
  if u.hp>0:result.append(u)
 return result
static func effective_attack(b,u: Dictionary) -> int:
 var value:=int(u.attack)
 for source in own(b,int(u.owner)):
  if source.uid==u.uid:continue
  if source.get("gift","")=="Volksfest" or (source.get("gift","")=="Sternennetz" and u.get("volk","") in ["Glimmerwerk","Artefakte"]):value+=1
 return mini(30,value)
static func swing(b,u: Dictionary) -> int:
 var value:=effective_attack(b,u)
 if "Flackernd" in u.get("traits",[]) and not u.get("flicker_used",false):value+=1;u.flicker_used=true
 return value
static func cards_played(b,owner: int) -> void:
 for u in own(b,owner):
  if u.get("ability","")=="Lernen" and int(u.get("learn_left",0))>0:
   grow(u,1,0);u.learn_left-=1
static func card_drawn(b,owner: int) -> void:
 for u in own(b,owner):
  if u.get("gift","")=="Tausend Stimmen" and int(u.get("voice_count",0))<2:
   grow(u,1,0);u.voice_count=int(u.get("voice_count",0))+1
static func start_turn(b,owner: int) -> void:
 b.strategy_turn_serial+=1
 for state in b.decks:state.refuge_used=false
 for u in b.board:
  if u.has("gift"):u.gate_count=0;u.voice_count=0
 var state: Dictionary=b.decks[owner];state.cards_played=0
 var bonus:=0
 for u in own(b,owner).duplicate():
  if u.has("ability"):u.flicker_used=false
  if int(u.get("grow_turns",0))>0:grow(u,1,1);u.grow_turns-=1
  if int(u.get("bud_turns",0))>0:grow(u,1,0);u.bud_turns=0
  if u.get("ability","")=="Zins" and int(u.get("interest_left",0))>0:
   bonus+=1;u.interest_left-=1
  if u.get("ability","")=="Heilung":
   var injured: Array=own(b,owner)
   injured.sort_custom(func(a,c):return a.max_hp-a.hp>c.max_hp-c.hp if a.max_hp-a.hp!=c.max_hp-c.hp else a.uid<c.uid)
   if not injured.is_empty():heal(injured[0],int(u.get("ability_value",0)))
  if u.get("gift","")=="Fließband" and b.units(owner).size()<b.BOARD_LIMIT:
   b._add_minion(owner,"forge_werkling",get_card("forge_werkling"))
  if u.get("gift","")=="Sauerteig":
   for target in own(b,owner):grow(target,0,1)
 state.crystals=mini(10,int(state.crystals)+mini(2,bonus))
static func end_turn(b,owner: int) -> void:
 for u in own(b,owner):
  if u.get("ability","")=="Eingraben" and not u.spent:grow(u,0,int(u.get("ability_value",0)))
  if u.get("gift","")=="Schatzkammer":grow(u,0,mini(3,int(b.decks[owner].crystals)))
  if u.get("gift","")=="Ewiges Feuer":
   for target in own(b,owner):heal(target,1)
static func protect(b,u: Dictionary) -> void:
 var owner:=int(u.owner)
 if u.hp>0 or b.decks[owner].get("refuge_used",false):return
 for source in own(b,owner):
  if source.uid!=u.uid and source.get("gift","")=="Zuflucht":
   u.hp=1;b.decks[owner].refuge_used=true;return
static func died(b,dead: Dictionary) -> void:
 for u in own(b,int(dead.owner)):
  if u.uid!=dead.uid and u.get("gift","")=="Höllenpforte" and int(u.get("gate_count",0))<2:
   grow(u,1,1);u.gate_count=int(u.get("gate_count",0))+1
 if dead.get("gift","")=="Kristallherz" and b.units(int(dead.owner)).size()<b.BOARD_LIMIT:
  b._add_minion(int(dead.owner),"forge_erzsplitter",get_card("forge_erzsplitter"))

static func targets(b,owner: int,card: Dictionary) -> Array[String]:
 if card.get("strategy_target","")=="enemy":return b.legal_targets(owner,true)
 var result: Array[String]=[]
 if card.get("strategy_target","")=="ally":
  for u in own(b,owner):result.append("u:"+str(u.uid))
 return result
static func friendly(b,unit: Dictionary,target: String) -> Dictionary:
 if not target.begins_with("u:"):return {}
 var chosen: Dictionary=b.find_unit(int(target.substr(2)))
 if not chosen.is_empty() and chosen.owner==unit.owner and chosen.uid!=unit.uid and chosen.hp>0:return chosen
 return {}
static func summon(b,u: Dictionary,card: Dictionary,target: String) -> void:
 var owner:=int(u.owner)
 for key in ["volk","ability","ability_x","ability_value","gift","traits"]:
  u[key]=card.get(key,[] if key=="traits" else "" if key in ["volk","ability","gift"] else 0)
 u.learn_left=int(u.ability_x);u.interest_left=int(u.ability_x);u.upgrade_left=1
 u.grow_turns=2 if "Wachstum" in u.traits else 0;u.bud_turns=1 if "Knospend" in u.traits else 0
 u.reborn=false;u.voice_count=0;u.gate_count=0;u.flicker_used=false
 if u.from_deck and affinity(card,str(b.decks[owner].species)):grow(u,0,1)
 for source in own(b,owner):
  if source.uid!=u.uid and source.get("ability","")=="Aufrüsten" and int(source.get("upgrade_left",0))>0 and u.volk in ["Glimmerwerk","Artefakte"]:
   grow(u,int(source.get("ability_value",0)),0);source.upgrade_left=0
 var chosen:=friendly(b,u,target)
 if u.gift=="Erwachen" and not chosen.is_empty():
  u.ability=chosen.get("ability","");u.ability_x=int(chosen.get("ability_x",0));u.ability_value=int(chosen.get("ability_value",0))
  u.learn_left=int(u.ability_x);u.interest_left=int(u.ability_x)
 var effects: Array[Dictionary]=[]
 match str(u.ability):
  "Widerhall":b._draw_cards(owner,int(u.ability_x))
  "Glück":
   if int(b.decks[owner].get("cards_played",0))>0:grow(u,int(u.ability_value),int(u.ability_value))
  "Glut":
   var code: String=target if target in b.legal_targets(owner,true) else "enemy" if owner<2 else b.legal_targets(owner,true).back()
   b._damage_target(owner,code,int(u.ability_value),effects)
  "Baukunst":
   if not chosen.is_empty():grow(chosen,0,int(u.ability_value))
  "Nähren":
   for other in own(b,owner):
    if other.uid!=u.uid:grow(other,0,int(u.ability_value))
  "Schutzlicht":
   if chosen.is_empty():chosen=u
   chosen.shield=mini(20,int(chosen.shield)+int(u.ability_value))
  "Blutpakt":
   if not chosen.is_empty():
    b._hurt_unit(chosen,int(u.ability_value),effects);b._remove_dead();b._draw_cards(owner,int(u.ability_x))
 if u.gift=="Flächenbrand":
  for enemy in b.board.duplicate():
   if (int(enemy.owner)==2)!=(owner==2):b._damage_target(owner,"u:"+str(enemy.uid),3,effects)
 b.strategy_effects.append_array(effects)

static func restore_unit(u: Dictionary,raw: Dictionary) -> void:
 var card:=get_card(str(u.id))
 if card.is_empty():return
 for key in ["volk","ability","gift","traits"]:u[key]=raw.get(key,card.get(key,[] if key=="traits" else ""))
 for key in ["ability_x","ability_value","learn_left","interest_left","upgrade_left","grow_turns","bud_turns","voice_count","gate_count"]:
  u[key]=clampi(int(raw.get(key,card.get(key,0))),0,10)
 for key in ["reborn","flicker_used"]:u[key]=bool(raw.get(key,false))
