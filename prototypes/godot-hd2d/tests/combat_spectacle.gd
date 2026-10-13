extends SceneTree
const Battle=preload("res://scripts/forest_battle.gd")
var checks:=0
var failures:=0
func _initialize() -> void:call_deferred("run")
func check(ok: bool,label: String) -> void:
 checks+=1
 if not ok:failures+=1;push_error(label)
func start(species: String="starlet"):
 var b=Battle.new();b.configure(species);b.begin();b.advance_intro(3.2)
 return b
func enemy_turn(b,r: int) -> void:
 b.cursor=2;b.round=r;b.playback_pending=false;b.enemy_act();b.acknowledge_animation()
func run() -> void:
 var b=start("fennec");b.set_random_seed(3010)
 var counts={"cat":0,"boar":0,"wolf":0,"gorilla":0}
 for i in 1000:counts[b.roll_wildlife().id]+=1
 check(counts.cat>280 and counts.cat<420,"Katze etwa 35 Prozent")
 check(counts.boar>240 and counts.boar<360,"Wildschwein etwa 30 Prozent")
 check(counts.wolf>190 and counts.wolf<310,"Wolf etwa 25 Prozent")
 check(counts.gorilla>60 and counts.gorilla<140,"Gorilla etwa 10 Prozent")
 b=start("fennec");b.set_random_seed(19);b.act("skill")
 var r=Battle.new();r.configure("fennec");r.restore(JSON.parse_string(JSON.stringify(b.snapshot())))
 check(r.last_event.summon.id==b.last_event.summon.id and r.enemy_health==b.enemy_health,"Gespeicherter Ruf würfelt nicht neu")
 check(b.roll_wildlife()==r.roll_wildlife(),"Zufallsfolge bleibt beim Laden erhalten")
 var snap: Dictionary=b.snapshot()
 check(not b.act("skill") and b.snapshot()==snap,"Doppeltipp ohne neuen Wurf und MP-Kosten")
 check(b.animation_duration()>3 and b.impact_time()>1.5,"Schaden wartet auf Ankunft des Tiers")
 b=start();b.act("support");b.acknowledge_animation()
 check(b.party[0].barrier==40 and b.party[1].barrier==40,"Beide Figuren erhalten Schutz")
 var hp: int=b.party[0].hp
 enemy_turn(b,1)
 check(b.party[0].hp==hp and b.party[0].barrier==23,"Treffer zuerst auf Barriere")
 b.party[0].barrier=5;enemy_turn(b,1)
 check(b.party[0].hp==hp-12 and b.party[0].barrier==0,"Nur Restschaden kommt durch")
 check(b.last_event.effects[0].kind=="barrier_break","Barrierenbruch sichtbar")
 b=start();b.act("support");b.acknowledge_animation();enemy_turn(b,4)
 check(b.party[0].hp==b.party[0].max_hp and b.party[0].barrier==28,"Schirm absorbiert Wurzelbann")
 check(b.party[0].bind==0 and b.party[0].silence==0,"Schirm verhindert Statuseffekt")
 b.party[0].barrier=4;b.party[1].barrier=0;enemy_turn(b,4)
 check(b.party[0].hp==b.party[0].max_hp-8,"Wurzeln brechen restlichen Schutz")
 check(b.party[0].bind==1 and b.party[0].silence==2,"Wurzeln fesseln und lassen verstummen")
 b.cursor=0;snap=b.snapshot()
 check(not b.act("strike") and not b.act("skill") and b.snapshot()==snap,"Sperre kostet keinen Zug")
 r=Battle.new();r.configure(b.lead);r.restore(JSON.parse_string(JSON.stringify(b.snapshot())))
 check(r.party[0].bind==1 and r.party[0].silence==2,"Status nach Laden")
 check(r.can_act("break_free") and r.act("break_free"),"Befreien bleibt möglich")
 check(r.party[0].bind==0 and r.party[0].silence==0,"Befreien entfernt beide Sperren")
 r.acknowledge_animation();r.cursor=0
 check(r.can_act("skill"),"Magie nach Befreien wieder möglich")
 b=start("fennec");b.party[0].bind=1;b.party[0].silence=2
 b.act("guard");b.acknowledge_animation();b.cursor=0
 check(b.party[0].bind==0 and b.party[0].silence==1 and b.can_act("strike") and not b.can_act("skill"),"Status zählt eigene Züge")
 b.act("guard");b.acknowledge_animation();b.cursor=0
 check(b.party[0].silence==0 and b.can_act("skill"),"Kein dauerhafter Stillstand")
 b=start();b.restore({"schema":3,"phase":"battle","party":[{},{}]})
 check(b.party[0].barrier==0 and b.party[0].bind==0 and b.party[0].silence==0,"Alte Speicherstände neutral erweitert")
 b=start();enemy_turn(b,2)
 check(b.last_event.visual=="wood_shards","Eigene Splitteranimation")
 b.round=4
 check(b.enemy_intent().contains("WURZELBANN"),"Ultima angekündigt")
 print("Kampfspektakel: %s Checks, %s Fehler; %s" % [checks,failures,counts])
 quit(1 if failures else 0)
