extends SceneTree

const Actor=preload("res://scripts/compact_actor.gd")
var checks:=0
var failures:=0

func _initialize() -> void:call_deferred("run")

func check(ok: bool,label: String) -> void:
 checks+=1
 if not ok:failures+=1;push_error(label)

func run() -> void:
 OS.set_environment("MOTION_DRAFT","1")
 var world:=Node3D.new();root.add_child(world)
 var actor=Actor.new();actor.species="mosslyn";actor.enabled=false;world.add_child(actor)
 await process_frame
 for sprint in [false,true]:
  actor.running=sprint;actor.run_blend=1.0 if sprint else 0.0
  var mode:="run" if sprint else "walk"
  var data: Dictionary=actor.painted_cycles[mode]
  var pixels: float=actor.base_pixel_size*109.0*actor.Catalog.relative_height("mosslyn")/float(data.reference_height)
  for direction in [Vector2.LEFT,Vector2.RIGHT]:
   actor.facing=direction
   var seen:={}
   for tick in 8:
    actor.gait=(tick+.1)/8.0;actor._update_frame(true)
    seen[actor.sprite.frame]=true
    check(actor.sprite.frame==tick,"Acht Originalphasen in Reihenfolge")
    check(actor.sprite.hframes==4 and actor.sprite.vframes==2,"Festes 4x2-Raster")
    check(is_equal_approx(actor.sprite.pixel_size,pixels),"Ein Maßstab für alle Phasen")
    check(is_equal_approx(actor.sprite.offset.y,float(data.ground)-actor.cell_size.y*.5),"Bodenanker bleibt auch in der Flugphase fest")
    check(actor.sprite.flip_h==(direction.x<0),"Gesamte Zeichnung wird gespiegelt")
    check(not actor.skin_mesh.visible,"Gemalte Phasen ohne zweite Verformung")
    check(is_equal_approx(actor._stride_length(),float(data.cycle_distance_pixels)*pixels),"Strecke wird aus der Originalzeichnung abgeleitet")
   check(seen.size()==8,"Alle Phasen erreichbar")
   var saved: float=actor.gait
   for tick in 60:
    actor.clock+=1.0/60.0;actor._update_frame(true)
   check(actor.gait==saved and actor.sprite.frame==7,"Ohne Wegfortschritt keine neue Phase")
   actor.facing=Vector2.DOWN;actor._update_frame(true)
   check(not actor.painted_cycle_active,"Frontansicht verlässt Seitenatlas")
   actor.facing=direction;actor._update_frame(true)
   actor.jump_clock=.1;actor._update_frame(true)
   check(not actor.painted_cycle_active,"Sprungknopf verlässt Seitenatlas")
   actor.jump_clock=-1;actor._update_frame(true)
   actor.state="sit";actor._update_frame(false)
   check(not actor.painted_cycle_active,"Sitzen verlässt Seitenatlas")
   actor.state="idle"
 actor.shadow.queue_free();world.queue_free();await process_frame
 print("Authored cycles: %s checks, %s failures"%[checks,failures])
 quit(1 if failures else 0)
