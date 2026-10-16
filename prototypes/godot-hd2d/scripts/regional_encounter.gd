extends "res://scripts/card_encounter.gd"
const Enemies=preload("res://scripts/regional_enemies.gd")
const Catalog=preload("res://scripts/world_catalog.gd")
var enemy_id := "thorn_moth"
var storage_key := ""
func _ready() -> void:
 automatic_trigger=false;encounter_room=app.room
 storage_key=app.actor.species+":"+enemy_id
 model.configure(app.actor.species)
 model.configure_enemy(enemy_id,Enemies.data()[enemy_id])
 model.prepare(app.save_data.get("card_decks",{}))
 model.restore(app.save_data.get("forest_encounters",{}).get(storage_key,{}))
 if model.phase=="unseen":model.prepare(app.save_data.get("card_decks",{}))
 _restore_progress()
func persist() -> void:
 if not app.save_data.has("forest_encounters"):app.save_data["forest_encounters"]={}
 _persist_progress()
 app.save_data.forest_encounters[storage_key]=model.snapshot()
 app._save_progress()
func resume() -> bool:
 if not active():suspended=true;return false
 suspended=false
 if model.claim_victory():persist()
 app._pause(true)
 app.actor.velocity=Vector3.ZERO;app.actor.stop_input();app.actor.facing=Vector2.RIGHT
 baseline=app.actor.position
 app._update_camera(10)
 _show_controls()
 if model.playback_pending:_play_event()
 return true
func _apply_visuals() -> void: pass
func _show_controls() -> void:
 super._show_controls()
 view.background=load(Catalog.data().interiors[encounter_room].asset)
func finish() -> void:
 if suspended or busy>0 or not model.finish_victory():return
 suspended=true;busy=0
 persist();app._resume();view=null
 app._notice(model.enemy_name+" ist besiegt. Du kannst den Ort weiter erkunden.")
func _retreat_now() -> void:
 suspended=true;model.retreat();persist()
 app._resume();view=null
 app._notice("Du gehst auf sicheren Abstand. Der Gegner bleibt am Ort.")
