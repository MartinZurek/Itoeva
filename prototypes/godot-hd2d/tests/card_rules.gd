extends SceneTree
const Battle=preload("res://scripts/card_battle.gd")
const OldBattle=preload("res://scripts/forest_battle.gd")
var checks:=0
var failures:=0
func _initialize() -> void:call_deferred("run")
func check(ok: bool,label: String) -> void:
 checks+=1
 if not ok:failures+=1;push_error(label)
func battle():
 var b=Battle.new();b.begin();b.advance_intro(3.2);return b
func run() -> void:
 var b=battle()
 var cat: int=b._add_minion(0,"cat")
 var guard: int=b._add_minion(2,"thornling")
 b.find_unit(cat).ready=true
 check(not b.can_attack(cat,"enemy") and b.can_attack(cat,"u:"+str(guard)),"Schutzhelfer schützt Gegner")
 b.attack_minion(cat,"u:"+str(guard));b.acknowledge_animation()
 check(b.find_unit(cat).is_empty() and b.find_unit(guard).hp==6,"Helfer tauschen gleichzeitig Schaden aus")
 b.decks[0].hand=["spark"];check(b.play_card(0,"enemy"),"Zauber darf Schutz umgehen");b.acknowledge_animation()
 b=battle();b._add_minion(0,"boar");var hp: int=b.party[0].hp
 var hits: Array[Dictionary]=[];b._receive_hit(0,20,hits)
 check(b.party[0].hp==hp and b.board.is_empty(),"Schutzhelfer fängt Treffer ohne Überschuss ab")
 b.party[0].barrier=12;hits=[];b._receive_hit(0,20,hits)
 check(b.party[0].barrier==0 and b.party[0].hp==hp-8,"Lichtbarriere absorbiert Schaden zuerst")
 b=battle();b.act("end_turn")
 var restored=Battle.new();restored.restore(JSON.parse_string(JSON.stringify(b.snapshot())))
 restored.acknowledge_animation();restored.acknowledge_animation()
 check(restored.cursor==1 and restored.decks[1].turns==1,"Gespeichertes Zugende zieht genau einmal")
 b=battle();b.act("skill");b.acknowledge_animation()
 var unit: Dictionary=b.board[0].duplicate(true)
 restored=Battle.new();restored.restore(JSON.parse_string(JSON.stringify(b.snapshot())))
 check(restored.board[0]==unit and restored.decks[0].power,"Zufallshelfer und verbrauchte Superkraft erhalten")
 b=battle();b._add_minion(0,"cat");b._add_minion(0,"boar");b._add_minion(0,"wolf")
 b.decks[0].hand=["cat"]
 check(not b.can_play(0) and not b.can_act("skill"),"Drei Helfer je Charakter begrenzen Beschwörungen")
 b=battle();b.decks[0].hand=["spark"];b.enemy_health=1;b.play_card(0,"enemy")
 restored=Battle.new();restored.restore(JSON.parse_string(JSON.stringify(b.snapshot())))
 check(restored.phase=="victory" and restored.playback_pending and not restored.rewards_claimed,"Letzter Treffer bleibt vor Belohnung unterbrechbar")
 restored.acknowledge_animation();restored.acknowledge_animation()
 check(restored.experience.fennec==120 and restored.inventory.potion==1,"Belohnung einmal nach finaler Animation")
 restored.finish_victory();restored.replay();restored.advance_intro(3.2)
 check(restored.enemy_health==220 and restored.round==1 and restored.board.is_empty() and restored.decks[0].crystals==1,"Wiederholung setzt Kampf und Karten zurück")
 var old=OldBattle.new();old.begin();old.advance_intro(3.2);old.act("strike");old.acknowledge_animation()
 restored=Battle.new();restored.restore(old.snapshot())
 check(restored.phase=="battle" and restored.enemy_health==old.enemy_health and restored.decks.size()==3,"Früherer Kampfstand wird übernommen")
 old=OldBattle.new();old.phase="done";old.experience={"fennec":240}
 restored=Battle.new();restored.restore(old.snapshot())
 check(restored.phase=="done" and restored.experience.fennec==240,"Abgeschlossener Altspielstand bleibt abgeschlossen")
 b=battle();b.party[0].bind=1;b.party[0].silence=2
 check(not b.can_act("skill") and b.can_act("break_free"),"Wurzelbann sperrt Superkraft")
 b.act("break_free");b.acknowledge_animation()
 check(b.party[0].bind==0 and b.party[0].silence==0 and b.decks[0].power and not b.can_act("skill"),"Befreiung verbraucht kostenlose Kraft dieses Zuges")
 print("Kartenregeln: %s Checks, %s Fehler" % [checks,failures]);quit(1 if failures else 0)
