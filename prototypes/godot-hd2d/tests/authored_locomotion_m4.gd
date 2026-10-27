extends SceneTree
const Motion=preload("res://scripts/authored_locomotion.gd")
const Actor=preload("res://scripts/compact_actor.gd")
var checks:=0
var failures:=0
func check(ok: bool,label: String) -> void:
 checks+=1
 if not ok:failures+=1;push_error(label)
func _initialize() -> void:call_deferred("run")
func run() -> void:
 OS.set_environment("MOTION_M4","1")
 var world:=Node3D.new();root.add_child(world)
 for species in ["mosslyn","nocti","starlet","wyrmling"]:
  var actor=Actor.new();actor.species=species;actor.enabled=false;world.add_child(actor)
  await process_frame
  check(actor.authored_m4,"M4 explizit geladen: "+species)
  var size:=-1.0
  for mode in actor.painted_cycles:
   for i in int(actor.painted_cycles[mode].frames):
    actor._use_painted_cycle(mode,i)
    if size<0:size=actor.sprite.pixel_size
    check(is_equal_approx(size,actor.sprite.pixel_size),"Ein Körpermaßstab einschließlich Ruhe: "+species+mode)
    check(not actor.skin_mesh.visible,"Keine zweite Verformung: "+species+mode)
    check(actor.sprite.offset.y==float(actor.painted_cycles[mode].ground)-actor.cell_size.y*.5,"Bodenanker: "+species+mode)
  if species!="mosslyn":
   var motion=Motion.new()
   var seen:={}
   for tick in 480:
    var moving:=tick<240
    var next: Dictionary=motion.advance(1.0/60,moving,tick>120,3.85,2.3,actor.painted_cycles,species=="nocti")
    check(int(next.phase)<int(actor.painted_cycles[next.mode].frames),"Gültige Phase")
    seen[next.mode]=true
   check(motion.mode=="rest","Loslassen beendet Flug/Hüpfer: "+species)
   check(seen.has("walk") and seen.has("run"),"Beide Geschwindigkeitszustände erreichbar")
   if species=="nocti":check(seen.has("takeoff") and seen.has("land"),"Gezeichnetes Abheben und Landen")
   actor.facing=Vector2.RIGHT;actor.jump_clock=-1;actor._update_frame(true)
   actor.state="sit";actor._update_frame(false)
   check(not actor.painted_cycle_active and actor.authored_locomotion.mode=="rest","Sitzaktion bricht Bewegungsfolge ab")
  actor.shadow.queue_free();actor.queue_free();await process_frame
 world.queue_free();await process_frame
 print("M4 locomotion: %s checks, %s failures"%[checks,failures])
 quit(1 if failures else 0)
