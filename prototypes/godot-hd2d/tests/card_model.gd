extends SceneTree
const Catalog=preload("res://scripts/world_catalog.gd")
const Battle=preload("res://scripts/card_battle.gd")
const Cards=preload("res://scripts/card_catalog.gd")
var checks:=0
var failures:=0
func _initialize() -> void:call_deferred("run")
func check(ok: bool,label: String) -> void:
 checks+=1
 if not ok:failures+=1;push_error(label)
func run() -> void:
 for species in Catalog.STARTERS:
  var b=Battle.new();b.configure(species);b.prepare({});b.begin();b.advance_intro(3.2)
  check(b.decks[0].species==species and Cards.valid(species,Cards.starter(species)),"Eigenes gültiges Deck "+species)
  check(b.decks[0].cap==1 and b.decks[0].crystals==1 and b.decks[0].hand.size()==4,"Erster Zug mit Kristall und Karte")
  b.decks[0].hand=["spark","leaf_guard"]
  check(b.play_card(0,"enemy") and b.cursor==0 and b.decks[0].crystals==0,"Karte verbraucht Kristall, behält Zug")
  check(not b.play_card(0) and not b.act("skill"),"Animation sperrt Karten und Superkraft")
  var snapshot: Dictionary=b.snapshot()
  var r=Battle.new();r.configure(species);r.prepare({});r.restore(JSON.parse_string(JSON.stringify(snapshot)))
  check(r.enemy_health==b.enemy_health and r.decks[0].crystals==0 and r.playback_pending,"Kosten und Treffer nach Laden genau einmal")
  r.acknowledge_animation()
  check(r.can_act("skill") and r.act("skill"),"Superkraft trotz leerer Kristalle")
  r.acknowledge_animation()
  check(not r.can_act("skill") and not r.can_act("support"),"Eine Superkraft pro eigener Runde")
  check(r.act("end_turn"),"Zug bewusst beenden")
  r.acknowledge_animation()
  check(r.cursor==1 and r.decks[1].crystals==1,"Begleiter hat eigenes Budget")
  r.act("end_turn");r.acknowledge_animation()
  check(r.cursor==2 and r.enemy_act(),"Gegnerzug automatisch spielbar")
  r.acknowledge_animation()
  check(r.round==2 and r.cursor==0 and r.decks[0].cap==2 and not r.decks[0].power,"Zweite Runde: mehr Kristalle und neue Superkraft")
 var b=Battle.new();b.begin();b.advance_intro(3.2)
 b.decks[0].hand=["cat"]
 check(b.play_card(0) and b.board.size()==1,"Helfer bleibt auf dem Spielfeld")
 b.acknowledge_animation()
 var uid: int=b.board[0].uid
 check(not b.can_attack(uid),"Beschwörung greift erst im nächsten eigenen Zug an")
 b.act("end_turn");b.acknowledge_animation();b.act("end_turn");b.acknowledge_animation();b.enemy_act();b.acknowledge_animation()
 check(b.can_attack(uid),"Helfer wird im nächsten Zug bereit")
 check(b.attack_minion(uid,"enemy"),"Helfer greift Ziel an")
 b.acknowledge_animation()
 check(not b.can_attack(uid),"Helfer nur einmal pro Zug")
 b.round=15;b._begin_turn(0)
 check(b.decks[0].cap==10,"Kristallgrenze zehn")
 check(not Cards.valid("fennec",["star_guard","spark","spark","cat","cat","boar","boar","wolf"]),"Fremde Klassenkarten ausgeschlossen")
 print("Kartenmodell: %s Checks, %s Fehler" % [checks,failures])
 quit(1 if failures else 0)
