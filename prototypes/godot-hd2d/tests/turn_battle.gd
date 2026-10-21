extends SceneTree
const Catalog=preload("res://scripts/world_catalog.gd")
const Battle = preload("res://scripts/forest_battle.gd")
var checks := 0
var failures := 0
func _initialize() -> void: call_deferred("run")
func check(ok: bool, label: String) -> void:
 checks += 1
 if not ok: failures += 1; push_error(label)
func start(species: String = "fennec") -> RefCounted:
 var b = Battle.new()
 b.configure(species)
 b.begin()
 b.advance_intro(3.2)
 return b
func run() -> void:
 for species in Catalog.STARTERS:
  var b = start(species)
  check(b.party.size()==2 and b.party[0].species==species,"Leader and companion "+species)
  var cost: int = b.party[0].mp
  var hp: int = b.enemy_health
  var expected: int = 30 if b.skill().element=="light" else 18
  var accepted: bool = b.act("skill")
  if species=="fennec": expected=int(b.last_event.summon.damage)
  check(accepted and b.party[0].mp==cost-6 and b.enemy_health==hp-expected,"Species skill and weakness "+species)
  check(b.cursor==1 and b.playback_pending,"Separate turns and pending playback")
  var snap: Dictionary = b.snapshot()
  check(not b.act("strike") and b.snapshot()==snap,"No duplicate tap or costs")
  var r = Battle.new(); r.configure(species); r.restore(snap)
  check(r.snapshot()==snap,"Pending action resumes exactly once "+species)
  r.acknowledge_animation()
  check(r.act("guard") and r.cursor==2,"Companion turn")
  r.acknowledge_animation()
  check(not r.act("strike") and r.enemy_act(),"Only enemy can act on enemy turn")
  r.acknowledge_animation()
  check(r.round==2 and r.cursor==0,"Order wraps to next round")
  # Full fight, using every species, ends before inventory runs out.
  for i in 50:
   if r.phase != "battle": break
   if r.cursor==2: r.enemy_act()
   elif r.can_act("skill"): r.act("skill")
   else: r.act("strike")
   r.acknowledge_animation()
  check(r.phase=="victory" and r.enemy_health==0,"Reachable victory "+species)
  check(not r.enemy_act(),"No retaliation after lethal hit")
 var b = start()
 b.party[0].mp=0
 var before: Dictionary = b.snapshot()
 check(not b.act("skill") and b.snapshot()==before,"Insufficient MP costs no turn")
 check(b.act("guard") and b.party[0].mp==4,"Guard restores MP")
 b.acknowledge_animation(); b.act("guard"); b.acknowledge_animation()
 var hp: int=b.party[0].hp
 b.enemy_act()
 check(b.party[0].hp==hp-6,"Guard reduces incoming hit")
 b.acknowledge_animation()
 b.party[1].hp=0
 check(b.act("potion",1) and b.party[1].hp==36 and b.potions==2,"Potion revives ally and consumes one")
 b.acknowledge_animation(); b.act("strike"); b.acknowledge_animation(); b.enemy_act(); b.acknowledge_animation()
 b.party[0].limit=100
 hp=b.enemy_health
 var special: bool = b.act("limit")
 var dealt := int(b.last_event.effects[-1].amount)
 check(special and dealt in [48,53,60,66] and b.enemy_health==maxi(0,hp-dealt) and b.party[0].limit==12,"Charged wildlife special consumes gauge")
 b.acknowledge_animation()
 b = start("starlet")
 b.round=3; b.cursor=2
 check(b.enemy_intent().contains("alle"),"All-party attack telegraphed")
 hp=b.party[0].hp
 var allyhp: int=b.party[1].hp
 b.enemy_act()
 check(b.party[0].hp==hp-10 and b.party[1].hp==allyhp-10,"Dark pulse hits both members")
 b.acknowledge_animation()
 b = start("puffling")
 b.act("skill"); b.acknowledge_animation(); b.act("guard"); b.acknowledge_animation()
 hp=b.party[0].hp; b.enemy_act()
 check(b.party[0].hp==hp and b.last_event.element=="stagger","Wind actually cancels next enemy attack")
 b.acknowledge_animation()
 b = start()
 b.party[1].hp=0
 b.act("strike"); b.acknowledge_animation()
 check(b.cursor==2,"KO companion skipped")
 b.party[0].hp=1; b.enemy_act(); b.acknowledge_animation()
 check(b.phase=="defeat","All KO ends battle")
 b.retry()
 check(b.phase=="battle" and b.party[0].hp==82 and b.party[1].hp==60 and not b.playback_pending,"Retry restores full valid state")
 b.act("flee")
 check(b.phase=="retreat" and b.playback_pending,"Retreat is saved before transition")
 var migrated=Battle.new(); migrated.restore({"phase":"done","enemy_health":0,"health":18})
 check(migrated.phase=="done" and migrated.party[0].hp==49,"v027 completed encounter preserved")
 migrated=Battle.new();migrated.restore({"phase":"battle","enemy_health":12,"health":15})
 check(migrated.enemy_health==68 and migrated.party[0].hp==41,"v027 ongoing HP migrated proportionally")
 b = start(); b.restore({"schema":2,"phase":"bogus","enemy_health":999,"cursor":99,"party":[{"hp":-2,"mp":999},{}],"last_event":[]})
 check(b.phase=="unseen" and b.enemy_health==136 and b.party[0].hp==0 and b.party[0].mp==24,"Malformed save fields clamped")
 for species in Catalog.STARTERS:
  var q = start(species)
  q.party[0].hp=35;q.party[1].hp=30
  var mp: int=q.party[0].mp
  check(q.act("support") and q.party[0].mp==mp-8,"Support MP cost "+species)
  var r=Battle.new();r.configure(species);r.restore(q.snapshot())
  check(r.snapshot()==q.snapshot(),"Statuses survive save "+species)
  match species:
   "fennec": check(q.party[0].evasion,"Decoy grants evasion")
   "starlet": check(q.party[0].barrier==40 and q.party[1].barrier==40,"Light barrier grants forty shield points")
   "wyrmling": check(q.enemy_burn==3 and q.enemy_health==126,"Burn spell deals hit and DOT")
   "gloop": check(q.party[0].hp==57 and q.party[1].hp==52,"Group healing actual LP")
   "puffling": check(q.party[0].haste and q.party[1].haste,"Tailwind affects group")
   "hootlet": check(q.enemy_weaken==3,"Moon weakens for three enemy turns")
  q.acknowledge_animation();q.act("strike");q.acknowledge_animation()
  var h: int=q.party[0].hp
  q.enemy_act();q.acknowledge_animation()
  match species:
   "fennec": check(q.party[0].hp==h and not q.party[0].evasion,"Decoy consumed exactly once")
   "wyrmling": check(q.enemy_burn==2 and q.last_event.effects[-1].kind=="burn","DOT consumes one duration")
   "puffling": check(not q.party[1].haste and q.party[0].haste,"Haste consumed by attacking member only")
   "hootlet": check(q.party[0].hp==h-12 and q.enemy_weaken==2,"Weakness reduces actual damage")
 for species in Catalog.STARTERS:
  var winner = start(species)
  winner.party[1].hp = 0
  winner.enemy_health = 1
  winner.act("strike")
  check(winner.experience.is_empty() and winner.inventory.potion == 0,"Kein Ergebnis vor letztem Treffer " + species)
  var resumed = Battle.new(); resumed.configure(species); resumed.restore(winner.snapshot())
  resumed.acknowledge_animation()
  check(resumed.experience[species] == 120 and resumed.inventory.potion == 1,"Letzten Treffer nach Laden einmal abrechnen " + species)
  check(resumed.result.members.size() == 2,"Auch erschöpfter Begleiter erhält XP " + species)
  var receipt: Dictionary = resumed.snapshot()
  resumed.acknowledge_animation(); resumed.claim_victory()
  check(receipt == resumed.snapshot(),"Ergebnisberechnung ist idempotent " + species)
  check(resumed.finish_victory() and not resumed.finish_victory(),"Abschluss genau einmal " + species)
  resumed.replay(); resumed.advance_intro(3.2)
  resumed.potions = 0; resumed.party[0].hp = 1
  check(resumed.act("potion",0) and resumed.inventory.potion == 0,"Gefundenen Heiltrank wirklich verwenden " + species)
  resumed.acknowledge_animation(); resumed.phase = "defeat"; resumed.retry()
  check(resumed.experience[species] == 120 and resumed.inventory.moon_shard == 1,"Niederlage und Wiederholung bewahren Fortschritt " + species)
  resumed.retreat()
  check(resumed.phase == "unseen" and resumed.experience[species] == 120,"Rückzug bewahrt Fortschritt " + species)
 var old_winner = Battle.new()
 old_winner.restore({"schema":2,"phase":"victory","enemy_health":0})
 check(old_winner.claim_victory() and not old_winner.claim_victory(),"Offenen v028-Sieg übernehmen")
 var old_done = Battle.new()
 old_done.restore({"schema":2,"phase":"done","enemy_health":0})
 check(not old_done.claim_victory() and old_done.experience.is_empty(),"Bereits abgeschlossenen alten Sieg nicht erneut abrechnen")
 var burn_winner = start("wyrmling")
 burn_winner.cursor = 2; burn_winner.enemy_health = 1; burn_winner.enemy_burn = 1
 burn_winner.enemy_act(); burn_winner.acknowledge_animation()
 check(burn_winner.phase == "victory" and burn_winner.inventory.potion == 1,"Brennschaden kann Sieg korrekt abschließen")
 var support_winner = start("wyrmling")
 support_winner.enemy_health = 1; support_winner.act("support"); support_winner.acknowledge_animation()
 check(support_winner.phase == "victory" and support_winner.experience.wyrmling == 120,"Unterstützungsfähigkeit kann Sieg korrekt abschließen")
 print("Rundenkampf: %s Checks, %s Fehler" % [checks,failures])
 quit(1 if failures else 0)
