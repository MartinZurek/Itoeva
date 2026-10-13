extends Node

const Battle = preload("res://scripts/card_battle.gd")
const Forest = preload("res://scripts/forest_catalog.gd")
const View = preload("res://scripts/card_view.gd")
var app: Node3D
var model = Battle.new()
var suspended := true
var busy := 0.0
var visual_clock := 0.0
var baseline := Vector3.ZERO
var view: Control
var enemy_delay := .5

func _ready() -> void:
 model.configure(app.actor.species)
 model.prepare(app.save_data.get("card_decks",{}))
 model.restore(app.save_data.get("forest_encounters",{}).get(app.actor.species,{}))
 if model.phase == "unseen": model.prepare(app.save_data.get("card_decks",{}))
 _apply_visuals()

func active() -> bool:
 return model.phase in ["intro","battle","victory","defeat","retreat"]

func persist() -> void:
 if not app.save_data.has("forest_encounters"): app.save_data["forest_encounters"] = {}
 app.save_data.forest_encounters[app.actor.species] = model.snapshot()
 app._save_progress()

func suspend() -> void:
 suspended = true
 if active(): persist()

func resume() -> bool:
 if not active():
  suspended = true
  return false
 suspended = false
 if model.claim_victory(): persist()
 app._pause(true)
 app.actor.position = app.stage.safe_position(Vector3(float(Forest.data().encounter.player_x),.03,-2.0),app.actor.collider.shape.radius)
 app.actor.velocity = Vector3.ZERO
 app.actor.facing = Vector2.RIGHT
 app.actor.state = "idle"
 app.actor._update_frame(false)
 baseline = app.actor.position
 app.stage.update_player(app.actor.position)
 app._update_camera(10.0)
 _show_controls()
 if model.playback_pending: _play_event()
 _apply_visuals()
 return true

func _process(delta: float) -> void:
 if not app.world_ready or app.switching or app.room != "FOREST_TRAIL": return
 if model.phase == "unseen" and not app.paused and app.actor.position.x >= float(Forest.data().encounter.trigger_x):
  model.begin()
  resume()
  persist()
 if suspended or not active() or not is_instance_valid(view): return
 visual_clock += delta
 if model.phase == "intro":
  if model.advance_intro(delta):
   persist()
   view.build_commands()
  view.refresh()
  _apply_visuals()
  return
 if model.playback_pending:
  busy = maxf(0.0,busy-delta)
  if busy <= 0.0:
   model.acknowledge_animation()
   enemy_delay = .55
   persist()
   if model.phase == "retreat":
    _retreat_now()
    return
   view.build_commands()
   view.refresh()
 elif model.phase == "battle" and model.cursor == 2:
  enemy_delay -= delta
  if enemy_delay <= 0.0 and model.enemy_act():
   persist()
   _play_event()

func _play_event() -> void:
 busy = model.animation_duration()
 visual_clock = 0.0
 view.play_event()

func _apply_visuals() -> void:
 app.stage.set_arena_lights(1.0 if model.phase == "unseen" else 1.0-smoothstep(0.0,.6,model.intro_time) if model.phase == "intro" else 0.0)
 var reveal := smoothstep(.65,2.6,model.intro_time) if model.phase == "intro" else 1.0 if model.phase in ["battle","defeat"] else 0.0
 app.stage.set_demon(reveal)
 app.stage.set_arena_smoke(maxf(0,1.0-model.intro_time/1.8) if model.phase == "intro" else 0.0)

func _show_controls() -> void:
 app._new_ui()
 view = View.new()
 view.encounter = self
 view.model = model
 app.ui.add_child(view)

func _act(action: String, target: int = -1) -> void:
 if suspended or busy > 0.0 or (is_instance_valid(view) and view.input_cooldown > 0.0) or not model.act(action,target): return
 persist()
 _play_event()

func _retry() -> void:
 if suspended: return
 model.source_decks = app.save_data.get("card_decks",{}).duplicate(true)
 model.retry()
 busy = 0.0
 enemy_delay = .55
 persist()
 _show_controls()
 _apply_visuals()

func replay() -> void:
 if suspended and model.phase != "done": return
 model.source_decks = app.save_data.get("card_decks",{}).duplicate(true)
 if not model.replay(): return
 busy = 0.0
 enemy_delay = .55
 persist()
 resume()

func _card_play(index: int, target: String) -> void:
 if suspended or busy > 0.0 or (is_instance_valid(view) and view.input_cooldown > 0.0) or not model.play_card(index,target): return
 persist()
 _play_event()

func _unit_attack(uid: int, target: String) -> void:
 if suspended or busy > 0.0 or (is_instance_valid(view) and view.input_cooldown > 0.0) or not model.attack_minion(uid,target): return
 persist()
 _play_event()

func finish() -> void:
 if suspended or busy > 0.0 or not model.finish_victory(): return
 # Den Abschluss vor dem Freigeben der UI markieren; doppelte Touch-Ereignisse
 # dürfen weder Belohnungen wiederholen noch eine freigegebene Ansicht benutzen.
 suspended = true
 busy = 0.0
 app.actor.position = app.stage.safe_position(baseline,app.actor.collider.shape.radius)
 app.actor.velocity = Vector3.ZERO
 app.actor.stop_input()
 app.stage.set_demon(0)
 persist()
 app._resume()
 view = null
 app._notice("Nachtwurz ist besiegt. Der Weg in den tiefen Wald ist frei.")

func _retreat() -> void:
 if suspended or model.phase != "defeat" or model.playback_pending: return
 _retreat_now()

func _retreat_now() -> void:
 suspended = true
 model.retreat()
 persist()
 app.retreat_forest()
