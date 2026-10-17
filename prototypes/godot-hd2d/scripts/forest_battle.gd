extends RefCounted

# Atomic results precede playback. A pending animation can safely resume after reload.
const MAX_ENEMY_HP := 136
const SPECIES := ["fennec", "starlet", "wyrmling", "gloop", "puffling", "hootlet", "mosslyn", "nocti"]
const STATS := {
 "fennec": [82,24,"Fennec","Wildruf","nature"],
 "starlet": [60,32,"Starlet","Sternenlicht","light"],
 "wyrmling": [88,22,"Wyrmling","Glutfunken","fire"],
 "gloop": [94,20,"Gloop","Quellwelle","water"],
 "puffling": [72,26,"Puffling","Windfeder","wind"],
 "hootlet": [68,30,"Hootlet","Mondruf","light"],
 "mosslyn": [86,26,"Mooslyn","Rankenstoß","nature"],
 "nocti": [66,30,"Nocti","Echoruf","moon"]
}
var enemy_name := "Nachtwurz"
var phase := "unseen"
var intro_time := 0.0
var enemy_health := MAX_ENEMY_HP
var turn := 0
var cursor := 0
var round := 1
var party: Array[Dictionary] = []
var potions := 3
var ethers := 1
var enemy_stagger := 0
var enemy_burn := 0
var enemy_weaken := 0
var last_event: Dictionary = {}
var playback_pending := false
var message := "Die Fackeln erlöschen …"
var lead := "fennec"
const BARRIER_HP := 40
const WILDLIFE := [
 {"id":"cat","name":"Waldkatze","weight":35,"damage":12,"row":0},
 {"id":"boar","name":"Wildschwein","weight":30,"damage":22,"row":1},
 {"id":"wolf","name":"Silberwolf","weight":25,"damage":36,"row":2},
 {"id":"gorilla","name":"Waldgorilla","weight":10,"damage":48,"row":3}
]
var rng := RandomNumberGenerator.new()
func set_random_seed(value: int) -> void: rng.seed=value
func roll_wildlife() -> Dictionary:
 var roll:=rng.randi_range(0,99)
 var threshold:=0
 for animal in WILDLIFE:
  threshold+=int(animal.weight)
  if roll<threshold: return animal.duplicate(true)
 return WILDLIFE[-1].duplicate(true)
func animation_duration() -> float:
 return clampf(float(last_event.get("duration",1.65)),1,4)
func impact_time() -> float:
 return clampf(float(last_event.get("impact",.62)),.1,animation_duration()-.2)
func _visual_state() -> Array[Dictionary]:
 var states: Array[Dictionary]=[]
 for member in party: states.append({"barrier":member.barrier,"silence":member.silence,"bind":member.bind})
 return states
func _receive_hit(target: int, damage: int, effects: Array[Dictionary]) -> int:
 var actor: Dictionary=party[target]
 if actor.evasion:
  actor.evasion=false
  effects.append({"target":target,"amount":0,"kind":"evade"})
  return 0
 var absorbed:=mini(int(actor.barrier),damage)
 if absorbed>0:
  actor.barrier-=absorbed;damage-=absorbed
  effects.append({"target":target,"amount":absorbed,"kind":"barrier_break" if actor.barrier==0 else "barrier_hit"})
 if damage>0:
  actor.hp=maxi(0,actor.hp-damage);actor.limit=mini(100,actor.limit+24)
  effects.append({"target":target,"amount":damage,"kind":"blocked" if actor.guard else "damage"})
 return damage
# Fortschritt gehört zum bestehenden lokalen Charakter-Spielstand.
const VICTORY_XP := 120
var experience: Dictionary = {}
var inventory: Dictionary = {"potion": 0, "moon_shard": 0}
var victories := 0
var rewards_claimed := false
var result: Dictionary = {}

func potion_count() -> int:
 return potions + int(inventory.get("potion",0))

func claim_victory() -> bool:
 if phase != "victory" or playback_pending or rewards_claimed: return false
 var members: Array[Dictionary] = []
 for member in party:
  var previous := int(experience.get(member.species,0))
  experience[member.species] = previous + VICTORY_XP
  members.append({"species":member.species,"name":member.name,"gained":VICTORY_XP,
   "before":previous,"total":experience[member.species]})
 inventory.potion = int(inventory.get("potion",0)) + 1
 inventory.moon_shard = int(inventory.get("moon_shard",0)) + 1
 victories += 1
 result = {"members":members,"items":[{"id":"potion","name":"Heiltrank","count":1},
  {"id":"moon_shard","name":"Mondsplitter","count":1}],"victory":victories}
 rewards_claimed = true
 message = enemy_name + " ist besiegt. Die Gruppe sammelt ihre Fundstücke."
 return true

func finish_victory() -> bool:
 if phase != "victory" or playback_pending: return false
 claim_victory()
 phase = "done"
 return true

func replay() -> bool:
 if phase not in ["done","victory"] or playback_pending: return false
 claim_victory()
 _reset_fight()
 intro_time = 0.0
 phase = "unseen"
 begin()
 return true

func retreat() -> void:
 _reset_fight()
 phase = "unseen"
 intro_time = 0.0

func configure(species: String) -> void:
 lead = species if species in SPECIES else "fennec"
 party.clear()
 for id in [lead,"fennec" if lead == "starlet" else "starlet"]:
  var data: Array = STATS[id]
  party.append({"species":id,"name":data[2],"hp":data[0],"max_hp":data[0],
   "mp":data[1],"max_mp":data[1],"guard":false,"limit":0,"evasion":false,"haste":false,"barrier":0,"silence":0,"bind":0})

func _init() -> void:
 rng.randomize()
 configure("fennec")

func current() -> String:
 return "enemy" if cursor == 2 else str(party[cursor].species)

func actor_name() -> String:
 return enemy_name if cursor == 2 else str(party[cursor].name)

func enemy_intent() -> String:
 if enemy_stagger>0: return "Wanken · nächster Zug entfällt"
 if round%4==0: return "WURZELBANN · Fesseln + Verstummen · alle"
 if round%3==0: return "Nebelwoge · alle"
 return "Holzsplitter · ein Ziel" if round%2==0 else "Wurzelhieb · ein Ziel"

func upcoming(count: int = 5) -> Array[String]:
 var result: Array[String] = []
 var index := cursor
 for i in range(count * 3):
  if index == 2 or party[index].hp > 0:
   result.append(enemy_name if index == 2 else str(party[index].name))
  if result.size() == count: break
  index = (index + 1) % 3
 return result

func skill() -> Dictionary:
 if cursor == 2: return {}
 var data: Array = STATS[party[cursor].species]
 return {"name":data[3],"element":data[4],"cost":6,"damage":20 if data[4] == "light" else 18}

func can_act(action: String, target: int = -1) -> bool:
 if phase != "battle" or cursor == 2 or playback_pending: return false
 if action not in ["strike","skill","light","guard","potion","ether","limit","flee","support","break_free"]: return false
 if action=="break_free": return party[cursor].bind>0 or party[cursor].silence>0
 if party[cursor].bind>0 and action in ["strike","skill","light","support","limit"]: return false
 if party[cursor].silence>0 and action in ["skill","light","support","limit"]: return false
 if action in ["skill","light"]: return party[cursor].mp >= 6
 if action == "support": return party[cursor].mp >= 8
 if action == "limit": return party[cursor].limit >= 100
 if action in ["potion","ether"]:
  if target < 0 or target >= party.size(): return false
  return (potion_count() > 0 and party[target].hp < party[target].max_hp) if action == "potion" else (ethers > 0 and party[target].hp > 0 and party[target].mp < party[target].max_mp)
 return true

func begin() -> void:
 if phase == "unseen":
  phase = "intro"
  message = "Die Fackeln erlöschen … "+party[1].name+" kommt dir zu Hilfe."

func advance_intro(delta: float) -> bool:
 if phase != "intro": return false
 intro_time = minf(3.2,intro_time + maxf(delta,0.0))
 if intro_time < 3.2: return false
 phase = "battle"
 message = "Nachtwurz erscheint. Licht ist seine Schwäche."
 return true

func act(action: String, target: int = -1) -> bool:
 if not can_act(action,target): return false
 var before_hp := [party[0].hp,party[1].hp]
 var before_enemy := enemy_health
 var before_status:=_visual_state()
 var source := cursor
 var actor: Dictionary = party[source]
 actor.guard = false
 var damage := 0
 var element := "physical"
 var title := "Angriff"
 var visual: String=""
 var duration:=1.65
 var impact:=.62
 var summon: Dictionary={}
 var effects: Array[Dictionary] = []
 match action:
  "strike":
   damage = 14 + turn % 3
   if actor.haste: damage += 6; actor.haste = false
  "skill","light":
   var ability := skill()
   actor.mp -= ability.cost
   title = ability.name
   element = ability.element
   damage = ability.damage
   if element == "light": damage = int(damage * 1.5)
   if actor.species=="starlet": visual="star_beam";duration=2.6;impact=1.08
   if element == "nature":
    summon=roll_wildlife();damage=int(summon.damage)
    title="Wildruf · "+str(summon.name)
    visual="wildlife";duration=3.4;impact=1.9
    var healed := mini(5,actor.max_hp - actor.hp)
    actor.hp += healed
    effects.append({"target":source,"amount":healed,"kind":"heal"})
   if element == "wind": enemy_stagger = 1
   if element == "fire": enemy_burn = 2
   if element == "water":
    for i in party.size():
     if party[i].hp <= 0: continue
     var healed := mini(6,party[i].max_hp-party[i].hp)
     party[i].hp += healed
     effects.append({"target":i,"amount":healed,"kind":"heal"})
   if actor.species == "hootlet": enemy_weaken = 2
  "limit":
   title = limit_name()
   element = str(STATS[actor.species][4])
   damage = 42
   actor.limit = 0
   enemy_stagger = 1
   if actor.species=="starlet": visual="star_nova";duration=3.2;impact=1.4
   if actor.species=="fennec":
    summon=roll_wildlife();damage+=int(int(summon.damage)/2)
    title+=" · "+str(summon.name)
    visual="wildlife";duration=3.4;impact=1.9
   if actor.species=="wyrmling": enemy_burn=3
   if actor.species=="gloop":
    for i in party.size():
     var healed := mini(24,party[i].max_hp-party[i].hp)
     party[i].hp+=healed
     effects.append({"target":i,"amount":healed,"kind":"heal"})
  "support":
   title = support_skill().name
   element = str(support_skill().element)
   actor.mp -= 8
   match actor.species:
    "fennec": actor.evasion = true
    "starlet":
     for member in party:
      if member.hp>0: member.barrier=BARRIER_HP
     effects.append({"target":source,"amount":BARRIER_HP,"kind":"barrier"})
     visual="star_barrier";duration=2.4;impact=.9
    "wyrmling":
     var support_damage:=_modify_enemy_damage(10)
     enemy_health = maxi(0,enemy_health-support_damage)
     _after_enemy_damage()
     enemy_burn = 3
     effects.append({"target":2,"amount":support_damage,"kind":"damage"})
     if enemy_health == 0: phase="victory"
    "gloop":
     for i in party.size():
      if party[i].hp <= 0: continue
      var healed := mini(22,party[i].max_hp-party[i].hp)
      party[i].hp += healed
      effects.append({"target":i,"amount":healed,"kind":"heal"})
    "puffling":
     for member in party: member.haste = true
    "hootlet": enemy_weaken = 3
  "break_free":
   title="Fesseln lösen";element="release"
   actor.bind=0;actor.silence=0
   effects.append({"target":source,"amount":0,"kind":"release"})
   visual="root_release";duration=1.8;impact=.65
  "guard":
   title = "Verteidigen"
   element = "guard"
   actor.guard = true
   var gained := mini(4,actor.max_mp - actor.mp)
   actor.mp += gained
   effects.append({"target":source,"amount":gained,"kind":"mp"})
  "potion":
   title = "Heiltrank"
   element = "heal"
   if potions > 0: potions -= 1
   else: inventory.potion = int(inventory.potion) - 1
   var healed := mini(36,party[target].max_hp - party[target].hp)
   party[target].hp += healed
   effects.append({"target":target,"amount":healed,"kind":"heal"})
  "ether":
   title = "Äther"
   element = "heal"
   ethers -= 1
   var gained := mini(16,party[target].max_mp - party[target].mp)
   party[target].mp += gained
   effects.append({"target":target,"amount":gained,"kind":"mp"})
  "flee":
   title = "Rückzug"
   element = "flee"
   phase = "retreat"
 if damage > 0:
  damage=_modify_enemy_damage(damage)
  enemy_health = maxi(0,enemy_health - damage)
  _after_enemy_damage()
  actor.limit = mini(100,actor.limit + 12)
  effects.append({"target":2,"amount":damage,"kind":"weak" if element == "light" else "damage"})
  if enemy_health == 0: phase = "victory"
 actor.bind=maxi(0,int(actor.bind)-1)
 actor.silence=maxi(0,int(actor.silence)-1)
 turn += 1
 message = actor.name + " · " + title
 last_event = {"source":source,"title":title,"element":element,"effects":effects,"before_hp":before_hp,"before_enemy":before_enemy,"visual":visual if not visual.is_empty() else "moon" if actor.species=="hootlet" and action in ["skill","limit"] else element,
  "duration":duration,"impact":impact,"summon":summon,"before_status":before_status}
 playback_pending = true
 _next()
 return true

func enemy_act() -> bool:
 if phase != "battle" or cursor != 2 or playback_pending: return false
 var before_hp := [party[0].hp,party[1].hp]
 var before_enemy := enemy_health
 var effects: Array[Dictionary] = []
 var title := "Wurzelhieb"
 var element := "shadow"
 var before_status:=_visual_state()
 var visual: String="shadow"
 var duration:=1.65
 var impact:=.62
 if enemy_stagger > 0:
  enemy_stagger -= 1
  title = "Nachtwurz wankt"
  element = "stagger"
 else:
  var targets: Array[int] = []
  if round%4==0:
   title="Wurzelbann";visual="root_bind";duration=3.2;impact=1.4
   for i in party.size():
    if party[i].hp>0: targets.append(i)
  elif round % 3 == 0:
   title = "Nebelwoge"
   for i in party.size():
    if party[i].hp > 0: targets.append(i)
  else:
   if round%2==0: title="Holzsplitter";visual="wood_shards";duration=2.15;impact=.9
   var target := (round - 1) % 2
   if party[target].hp <= 0: target = 1 - target
   targets.append(target)
  for target in targets:
   var actor: Dictionary = party[target]
   var damage:=12 if round%4==0 else 10 if round%3==0 else 16+round%3
   if enemy_weaken > 0: damage = int(ceil(damage*.65))
   if actor.guard: damage = int(ceil(damage * 0.35))
   var bypassed: int=_receive_hit(target,damage,effects)
   # Ein ungebrochener Lichtschirm hält auch die Wurzelfesseln fern.
   if round%4==0 and bypassed>0 and actor.hp>0:
    actor.bind=1;actor.silence=2
    effects.append({"target":target,"amount":2,"kind":"silence"})
    effects.append({"target":target,"amount":1,"kind":"bind"})
  if party[0].hp == 0 and party[1].hp == 0: phase = "defeat"
 if enemy_burn > 0:
  enemy_burn -= 1
  var burn := mini(6,enemy_health)
  enemy_health -= burn
  effects.append({"target":2,"amount":burn,"kind":"burn"})
  if enemy_health == 0 and phase != "defeat": phase = "victory"
 if enemy_weaken > 0: enemy_weaken -= 1
 message = title
 last_event = {"source":2,"title":title,"element":element,"effects":effects,"before_hp":before_hp,"before_enemy":before_enemy,"visual":visual,"duration":duration,"impact":impact,"before_status":before_status}
 playback_pending = true
 turn += 1
 _next()
 return true

func _next() -> void:
 for i in 3:
  cursor = (cursor + 1) % 3
  if cursor == 0: round += 1
  if cursor == 2 or party[cursor].hp > 0: return

func acknowledge_animation() -> void:
 playback_pending = false
 claim_victory()

func retry() -> void:
 if phase != "defeat": return
 _reset_fight()

func _reset_fight() -> void:
 configure(lead)
 rewards_claimed = false
 result = {}
 enemy_health = MAX_ENEMY_HP
 cursor = 0
 round = 1
 turn = 0
 potions = 3
 ethers = 1
 enemy_stagger = 0
 enemy_burn = 0
 enemy_weaken = 0
 last_event = {}
 playback_pending = false
 phase = "battle"
 message = "Ein neuer Versuch. Verteidigen regeneriert 4 MP."

func snapshot() -> Dictionary:
 return {"schema":4,"phase":phase,"intro_time":intro_time,"enemy_health":enemy_health,
  "turn":turn,"cursor":cursor,"round":round,"party":party.duplicate(true),
  "potions":potions,"ethers":ethers,"stagger":enemy_stagger,"burn":enemy_burn,"weaken":enemy_weaken,
  "last_event":last_event.duplicate(true),"playback_pending":playback_pending,"message":message,
  "experience":experience.duplicate(true),"inventory":inventory.duplicate(true),
  "victories":victories,"rewards_claimed":rewards_claimed,"result":result.duplicate(true),"rng_state":str(rng.state)}

func restore(saved: Dictionary) -> void:
 if saved.has("rng_state"): rng.state=int(str(saved.rng_state))
 experience = {}
 var saved_xp = saved.get("experience",{})
 if saved_xp is Dictionary:
  for species in SPECIES:
   if saved_xp.has(species): experience[species] = maxi(0,int(saved_xp[species]))
 inventory = {"potion":0,"moon_shard":0}
 var saved_items = saved.get("inventory",{})
 if saved_items is Dictionary:
  for item in inventory: inventory[item] = maxi(0,int(saved_items.get(item,0)))
 victories = maxi(0,int(saved.get("victories",0)))
 rewards_claimed = bool(saved.get("rewards_claimed",false))
 var receipt = saved.get("result",{})
 result = receipt.duplicate(true) if receipt is Dictionary else {}
 phase = str(saved.get("phase","unseen"))
 if phase not in ["unseen","intro","battle","victory","defeat","done","retreat"]: phase = "unseen"
 intro_time = clampf(float(saved.get("intro_time",0)),0,3.2)
 message = str(saved.get("message",message))
 turn = maxi(0,int(saved.get("turn",0)))
 if int(saved.get("schema",1)) < 2:
  enemy_health = clampi(int(float(saved.get("enemy_health",24)) / 24.0 * MAX_ENEMY_HP),0,MAX_ENEMY_HP)
  party[0].hp = clampi(int(float(saved.get("health",30)) / 30.0 * party[0].max_hp),0,party[0].max_hp)
  return
 enemy_health = clampi(int(saved.get("enemy_health",MAX_ENEMY_HP)),0,MAX_ENEMY_HP)
 cursor = clampi(int(saved.get("cursor",0)),0,2)
 round = maxi(1,int(saved.get("round",1)))
 potions = clampi(int(saved.get("potions",3)),0,3)
 ethers = clampi(int(saved.get("ethers",1)),0,1)
 enemy_stagger = clampi(int(saved.get("stagger",0)),0,1)
 enemy_burn=clampi(int(saved.get("burn",0)),0,3)
 enemy_weaken=clampi(int(saved.get("weaken",0)),0,3)
 var entries = saved.get("party",[])
 if entries is Array and entries.size() == 2:
  for i in 2:
   if not entries[i] is Dictionary: continue
   var entry: Dictionary = entries[i]
   party[i].hp = clampi(int(entry.get("hp",party[i].max_hp)),0,party[i].max_hp)
   party[i].mp = clampi(int(entry.get("mp",party[i].max_mp)),0,party[i].max_mp)
   party[i].guard = bool(entry.get("guard",false))
   party[i].limit = clampi(int(entry.get("limit",0)),0,100)
   party[i].evasion=bool(entry.get("evasion",false))
   party[i].haste=bool(entry.get("haste",false))
   party[i].barrier=clampi(int(entry.get("barrier",0)),0,BARRIER_HP)
   party[i].silence=clampi(int(entry.get("silence",0)),0,2)
   party[i].bind=clampi(int(entry.get("bind",0)),0,1)
 var event = saved.get("last_event",{})
 if event is Dictionary and event.get("effects",[]) is Array:
  last_event = event.duplicate(true)
  playback_pending = bool(saved.get("playback_pending",false)) and event.has("source")
 if phase == "battle" and not playback_pending:
  if party[0].hp <= 0 and party[1].hp <= 0: phase = "defeat"
  elif cursor < 2 and party[cursor].hp <= 0: _next()

func support_skill() -> Dictionary:
 if cursor == 2: return {}
 var abilities := {
  "fennec":["Fährtenlist","decoy","Weicht dem nächsten Treffer aus."],
  "starlet":["Lichtschirm","light","40 Schutzpunkte je lebendem Mitglied; hält Treffer und Wurzelfesseln ab."],
  "wyrmling":["Glutspur","fire","10 Schaden; Brennen für 3 Gegnerzüge."],
  "gloop":["Tauquelle","heal","Heilt beide lebenden Gruppenmitglieder um 22 LP."],
  "puffling":["Rückenwind","wind","Nächster normaler Angriff beider Mitglieder: +6 Schaden."],
  "hootlet":["Dämmerschleier","moon","Gegner verursacht 3 Gegnerzüge lang 35 % weniger Schaden."],
  "mosslyn":["Lebenshain","heal","Heilt die Gruppe und eigene Helfer."],
  "nocti":["Schattenhaut","moon","Dein Wesen und eigene Helfer weichen dem nächsten Treffer aus."]
 }
 var a: Array = abilities[party[cursor].species]
 return {"name":a[0],"element":a[1],"cost":8,"description":a[2]}

func limit_name() -> String:
 var names := {"fennec":"Waldchor","starlet":"Nova","wyrmling":"Sonnenatem","gloop":"Lebensquell","puffling":"Sturmkreis","hootlet":"Mondfinsternis","mosslyn":"Urwaldherz","nocti":"Nachtspiegel"}
 return str(names[party[cursor].species]) if cursor < 2 else ""

# Regionale Begegnungen können Rüstung anwenden, bevor Schaden begrenzt wird.
func _modify_enemy_damage(amount: int) -> int:return amount
func _after_enemy_damage() -> void:pass
