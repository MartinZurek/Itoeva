extends SceneTree
const Forest=preload("res://scripts/forest_catalog.gd")
var checks:=0
var failures:=0
func _initialize() -> void:call_deferred("run")
func check(ok: bool,label: String) -> void:
 checks+=1
 if not ok:failures+=1;push_error(label)
func frames(n: int) -> void:
 for i in n:await process_frame
func button(app: Node,text: String) -> Button:
 for b in app.menu_buttons:
  if is_instance_valid(b) and b.text==text:return b
 return null
func tap(b: Button) -> void:
 check(b!=null,"Touchziel vorhanden")
 if b==null:return
 var e:=InputEventScreenTouch.new();e.index=2;e.position=b.get_global_rect().get_center();e.pressed=true
 root.push_input(e,true);e.pressed=false;root.push_input(e,true)
func run() -> void:
 var app: Node3D=load("res://scenes/full_world.tscn").instantiate()
 DirAccess.make_dir_recursive_absolute("user://godot-tests")
 app.save_path="user://godot-tests/polish-v062.json";DirAccess.remove_absolute(app.save_path);root.add_child(app)
 while app.loading_world:await process_frame
 # Der Produktionsweg muss ohne lokale starke Editorreferenz funktionieren.
 app.show_deck();await frames(3)
 app.deck_editor.change("spark",-1);await frames(3)
 app.deck_editor.scroll.scroll_vertical=140
 var draft: Array=app.deck_editor.draft.duplicate()
 for i in 3:
  tap(button(app,"Ansehen"));await frames(3)
  check(button(app,"Schließen").pressed.get_connections().size()==1,"Schliessen behaelt Callback")
  tap(button(app,"Schließen"));await frames(3)
  check(button(app,"Ansehen")!=null,"Detail schliesst zurueck ins Deck")
  check(app.deck_editor.draft==draft,"Ungespeichertes Deck bleibt erhalten")
 check(app.deck_editor.scroll.scroll_vertical==140,"Listenposition bleibt erhalten")
 await app.start_game();await app._mount_world("FOREST_TRAIL",Forest.position_for(5,.44))
 var enc: Node=app.forest_encounter
 enc.model.begin();enc.model.advance_intro(3.2);enc.resume();await frames(2)
 enc.model.enemy_health=5;enc.model.decks[0].hand=["spark"]
 enc._card_play(0,"enemy")
 var view: Control=enc.view
 view.event_time=0;view.refresh()
 check(view.enemy_visual_state()=="idle","Toedlicher Angriff: Gegner steht vor Wirkung")
 check(view.enemy_label.text.contains("5 /"),"Vor Wirkung bleiben echte vorherige LP sichtbar")
 view.event_time=enc.model.impact_time()+.02;view.refresh()
 check(view.enemy_visual_state()=="hit" and view.enemy_label.text.contains("0 /"),"Erst Wirkung zieht LP ab und loest Trefferpose aus")
 view.event_time=enc.model.impact_time()+.39
 check(view.enemy_visual_state()!="fall","LP-Balken hat vor Zusammenbruch Zeit zum Leeren")
 view.event_time=enc.model.impact_time()+.42
 check(view.enemy_visual_state()=="fall","Zusammenbruch folgt dem Treffer")
 app.battle_audio._process(.1)
 check(app.battle_audio.music_mode=="battle","Siegesmusik startet nicht vor finaler Animation")
 enc._process(enc.busy+.01);view._process(.01);app.battle_audio._process(.1)
 check(view.results_shown and app.battle_audio.music_mode=="victory","Ergebnis und Siegesmusik folgen auf Abschluss")
 check(app.battle_audio.arena.stream==app.battle_audio.victory_theme,"Eigenes Siegsthema ersetzt Kampfmusik")
 view.result_time=.0;view._update_results()
 check(view.result_progress(0)==0 and view.reward_rows[0].modulate.a==0,"Ergebnis beginnt mit altem Stand ohne vorgezogene Beute")
 var early: Dictionary=view.victory_pose(0)
 view.result_time=.7;view._update_results()
 check(view.result_progress(0)>0 and view.result_progress(0)<1,"XP zaehlen sichtbar zwischen echtem Anfang und Ende")
 check(view.victory_pose(0)!=early,"Jubel bewegt Figuren")
 view.result_time=2.55;view._update_results()
 check(view.reward_rows[0].modulate.a>view.reward_rows[2].modulate.a,"Beute erscheint gestaffelt")
 view.result_time=4;view._update_results()
 check(view.gained_labels[0].text=="+120 XP" and view.experience_labels[0].text.contains("120 XP"),"XP erreichen verbuchten Endstand")
 check(view.reward_rows[2].modulate.a==1,"Letzte Belohnung wird voll sichtbar")
 app.selected="starlet";app.save_data["forest_encounters"]={}
 await app._mount_world("FOREST_TRAIL",Forest.position_for(6,.4))
 app.stage.set_time(23);app.actor.glow_energy=0
 for i in 120:app.actor._update_occlusion()
 app.stage.update_player(app.actor.position)
 check(not app.stage.lantern.visible,"Starlet traegt niemals eine Laterne")
 check(app.actor.glow_energy>0,"Starlet erzeugt im dunklen Wald Eigenlicht")
 check(app.actor.aura_material.get_shader_parameter("energy")>0,"Eigenlicht erzeugt Halo an der Figur")
 var night: float=app.actor.glow_energy
 await app._mount_world("OUTDOOR",app.Catalog.position_for("PARK",Vector2(.4,.7)))
 app.stage.set_time(12);app.actor.glow_energy=night
 for i in 120:app.actor._update_occlusion()
 check(app.actor.glow_energy<.01,"Eigenlicht verschwindet allmaehlich bei Tageslicht")
 app.stage.set_time(23)
 for i in 120:app.actor._update_occlusion()
 check(app.actor.glow_energy>.5,"Starlet leuchtet auch nachts ausserhalb des Schattenwaldes")
 check(app.stage.painted_materials[0].get_shader_parameter("carried_gain")>.5,"Stern beleuchtet seine echte Umgebung")
 app.queue_free();await frames(3);DirAccess.remove_absolute("user://godot-tests/polish-v062.json")
 print("Darstellung v0.6.2: %s Checks, %s Fehler" % [checks,failures]);quit(1 if failures else 0)
