extends "res://scripts/battle_view.gd"
const StrategySummonFX=preload("res://scripts/strategy_summon_fx.gd")
const Feedback=preload("res://scripts/battle_feedback.gd")
var formation_slots: Dictionary = {}
var crystal_meter: Control
const Cards=preload("res://scripts/card_catalog.gd")
const Art=preload("res://scripts/card_assets.gd")
const Detail=preload("res://scripts/card_detail.gd")
const SpecialFX=preload("res://scripts/special_effects.gd")
const EcologyFX=preload("res://scripts/ecology_effects.gd")
var special_fx=SpecialFX.new()
var selected_card: int=-1
var selected_unit: int=-1
var hand_label: Label
var crystal_label: Label
var hint_message: String=""
var animal_data: Dictionary={}
var animal_texture: Texture2D
var card_popup: Panel
var popup_index: int=0
var inspected_unit: int=-1
var unit_info: Panel
var unit_info_name: Label
var unit_info_stats: Label
var unit_info_state: Label

func _build() -> void:
 spectacle=load("res://scripts/card_effects.gd").new()
 animal_data=JSON.parse_string(FileAccess.get_file_as_string("res://data/wildlife_atlas.json"))
 animal_texture=load(animal_data.asset)
 layout=Control.new();layout.size=Vector2(1280,720);layout.mouse_filter=Control.MOUSE_FILTER_IGNORE;add_child(layout)
 _panel(Rect2(20,16,1240,56))
 _label((encounter.app.Catalog.data().labels[model.enemy_profile.place].to_upper() if model.enemy_id!="" else "SCHATTENWALD")+" · KARTEN",Rect2(38,26,330,32),20,GOLD)
 turn_label=_label("",Rect2(755,28,190,30),16)
 crystal_meter=load("res://scripts/crystal_meter.gd").new()
 crystal_meter.position=Vector2(380,20);crystal_meter.size=Vector2(340,46)
 crystal_meter.mouse_filter=Control.MOUSE_FILTER_IGNORE;layout.add_child(crystal_meter)
 _button("Ton",Rect2(958,25,80,38),encounter.app.show_audio_settings)
 var menu:=_button("Menü",Rect2(1152,25,88,38),encounter.app.show_menu)
 menu.set_meta("battle_action","menu")
 var inspect:=_button("Karten",Rect2(1052,25,88,38),_show_hand)
 inspect.set_meta("battle_action","card_info")
 _panel(Rect2(20,86,720,66))
 for i in 2:
  var x:=37.0+i*350.0
  hp_labels.append(_label("",Rect2(x,93,335,27),18))
  status_labels.append(_label("",Rect2(x,124,335,22),13,Color("bcd5df")))
 _panel(Rect2(845,86,395,76))
 enemy_label=_label("",Rect2(862,94,360,25),16 if model.enemy_profile.get("kind","")=="boss" else 19,GOLD)
 enemy_bar=_bar(Rect2(862,124,360,7),Color("ba717d"));enemy_bar.max_value=model.enemy_max_hp
 intent=_label("",Rect2(862,137,360,22),12,Color("c5bfdb"))
 crystal_label=_label("",Rect2(22,317,305,26),18,Color("9bdef1"))
 queue_label=_label("",Rect2(22,448,305,44),14,Color("bcd5df"));queue_label.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
 unit_info=_panel(Rect2(350,169,469,92));unit_info.visible=false
 unit_info_name=_label("",Rect2(368,176,430,24),18,GOLD)
 unit_info_stats=_label("",Rect2(368,205,430,22),16)
 unit_info_state=_label("",Rect2(368,232,430,21),13,Color("aacbd1"))
 unit_info_name.visible=false;unit_info_stats.visible=false;unit_info_state.visible=false
 _panel(Rect2(18,500,1242,204))
 hand_label=_label("",Rect2(34,507,570,23),15,GOLD)
 status=_label("",Rect2(595,500,645,34),15);status.horizontal_alignment=HORIZONTAL_ALIGNMENT_RIGHT
 build_commands()

func _locked() -> bool:
 return is_instance_valid(card_popup) or encounter.suspended or encounter.busy>0 or model.playback_pending or input_cooldown>0 or model.cursor==2 or model.phase!="battle"

func _show_hand() -> void:
 if _locked() or model.decks[model.cursor].hand.is_empty():return
 popup_index=0;_popup_page()

func _popup_page(delta: int=0) -> void:
 if delta!=0:
  if input_cooldown>0:return
  input_cooldown=.18
 var hand: Array=model.decks[model.cursor].hand
 if is_instance_valid(card_popup):card_popup.detach()
 popup_index=posmod(popup_index+delta,hand.size())
 card_popup=Detail.new();card_popup.app=encounter.app;card_popup.card=Cards.get_card(hand[popup_index])
 card_popup.close_callback=_close_popup;card_popup.previous_callback=_popup_page.bind(-1);card_popup.next_callback=_popup_page.bind(1)
 card_popup.position=Vector2(340,174);card_popup.size=Vector2(600,309);layout.add_child(card_popup);refresh()

func _close_popup() -> void:
 if is_instance_valid(card_popup):card_popup.detach()
 card_popup=null;input_cooldown=.18;refresh()

func _clear_buttons() -> void:
 for button in buttons:
  encounter.app.menu_buttons.erase(button);button.queue_free()
 buttons.clear()

func _action(text: String,rect: Rect2,callback: Callable,key: String) -> Button:
 var button:=_button(text,rect,callback)
 button.clip_text=true;button.add_theme_font_size_override("font_size",15)
 button.set_meta("battle_action",key);buttons.append(button)
 return button

func build_commands() -> void:
 _clear_buttons()
 if model.phase in ["victory","defeat"]:
  super.build_commands();return
 if model.phase!="battle":return
 for unit in model.board:
  var uid:=int(unit.uid)
  var foot:=_unit_position(uid)
  var b:=_action("",Rect2(foot-Vector2(58,95),Vector2(116,115)),_pick_unit.bind(uid),"unit")
  var box:=StyleBoxEmpty.new()
  for style in ["normal","hover","pressed","disabled"]:b.add_theme_stylebox_override(style,box)
  b.set_meta("unit_uid",uid)
 _action("",Rect2(987,174,248,184),_choose_target.bind("enemy"),"enemy_target")
 for style in ["normal","hover","pressed","disabled"]:buttons[-1].add_theme_stylebox_override(style,StyleBoxEmpty.new())
 if model.cursor==2:return
 var state: Dictionary=model.decks[model.cursor]
 for i in state.hand.size():
  var card: Dictionary=Cards.get_card(state.hand[i])
  var b:=_action("",Rect2(20+i*208,535,196,166),_pick_card.bind(i),"card")
  b.set_meta("hand_index",i);b.tooltip_text=card.name+" · "+str(card.cost)+" Kristalle\n"+card.get("detail",card.text)
  b.clip_contents=true
  b.set_meta("card_id",str(state.hand[i]))
  var box:=_box(Color("1d2d3b"));box.border_color=Art.accent(card);box.set_border_width_all(2 if card.get("rare",false) or selected_card==i else 1)
  if selected_card==i:box.bg_color=Color("3b554f")
  b.add_theme_stylebox_override("normal",box)
  _card_face(b,card)
 var primary: Dictionary=model.skill()
 var secondary: Dictionary=model.support_skill()
 var primary_button:=_action(primary.name,Rect2(22,351,146,55),_power.bind("skill"),"skill")
 primary_button.add_theme_font_size_override("font_size",13)
 var trapped: bool=model.party[model.cursor].bind>0 or model.party[model.cursor].silence>0
 var secondary_button:=_action("Fesseln lösen" if trapped else secondary.name,Rect2(177,351,146,55),_power.bind("break_free" if trapped else "support"),"break_free" if trapped else "support")
 secondary_button.add_theme_font_size_override("font_size",13)
 var ultimate: Dictionary=model.special_card()
 var ultimate_text: String="Spezial · %s · %s/5" % [ultimate.name,model.special_charges[model.cursor]]
 if model.special_used[model.cursor]:ultimate_text="Spezialkraft bereits eingesetzt"
 var special_button:=_action(ultimate_text,Rect2(22,409,301,35),_power.bind("special"),"special")
 special_button.add_theme_font_size_override("font_size",14)
 _action("Zug beenden",Rect2(1070,543,175,59),_power.bind("end_turn"),"end_turn")
 _action("Auswahl lösen",Rect2(1070,609,175,38),_cancel,"cancel")
 _action("Rückzug",Rect2(1070,654,175,37),_power.bind("flee"),"flee")

func _child_label(parent: Control,text: String,rect: Rect2,font_size: int,color: Color=TEXT) -> Label:
 var label:=Label.new();label.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
 label.add_theme_font_size_override("font_size",font_size);label.clip_text=true
 label.text=text;label.position=rect.position;label.size=rect.size
 label.mouse_filter=Control.MOUSE_FILTER_IGNORE
 label.add_theme_color_override("font_color",color);parent.add_child(label)
 return label

func _card_face(button: Button,card: Dictionary) -> void:
 var jewel:=Panel.new();jewel.position=Vector2(5,3);jewel.size=Vector2(39,36)
 jewel.mouse_filter=Control.MOUSE_FILTER_IGNORE
 var gem:=_box(Color("245267"));gem.border_color=Color("9ee5f3");gem.set_border_width_all(2)
 jewel.add_theme_stylebox_override("panel",gem);button.add_child(jewel)
 var cost:=_child_label(button,str(card.cost),Rect2(10,1,31,40),24,Color("9bdef1"))
 cost.set_meta("card_field","cost")
 _child_label(button,card.name,Rect2(43,9,145,24),15,Art.accent(card))
 var art:=TextureRect.new();art.texture=_card_texture(card)
 art.expand_mode=TextureRect.EXPAND_IGNORE_SIZE;art.stretch_mode=TextureRect.STRETCH_KEEP_ASPECT_CENTERED
 art.position=Vector2(12,37);art.size=Vector2(172,63)
 art.mouse_filter=Control.MOUSE_FILTER_IGNORE;button.add_child(art)
 var description:=_child_label(button,card.text,Rect2(12,100,172,48),13)
 description.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
 description.size=Vector2(172,48)
 _child_label(button,Art.kind_label(card),Rect2(12,150,172,16),10,Art.accent(card))

func _card_texture(card: Dictionary) -> Texture2D:
 return Art.texture(card)

func _legacy_card_texture(card: Dictionary) -> Texture2D:
 var texture:=AtlasTexture.new()
 if card.has("row"):
  var r: Array=animal_data.regions[int(card.row)][0]
  texture.atlas=animal_texture;texture.region=Rect2(r[0],r[1],r[2],r[3])
 else:
  var species: String=card.get("species","starlet" if card.element in ["light","guard"] else "gloop")
  if species=="enemy":
   texture.atlas=demon;texture.region=_demon_rect(0)
  else:
   var data: Dictionary=atlas_data[species]
   var r: Array=data.regions[12 if card.kind=="minion" else 7]
   texture.atlas=load(data.asset);texture.region=Rect2(r[0],r[1],r[2],r[3])
 return texture

func _pick_card(index: int) -> void:
 if _locked() or not model.can_play(index):return
 var card: Dictionary=Cards.get_card(model.decks[model.cursor].hand[index])
 if card.kind=="damage" or card.has("target") or (card.has("strategy_target") and not model.card_targets(model.cursor,card).is_empty()):
  encounter.app.battle_audio.cue("select")
  selected_card=index;selected_unit=-1;hint_message=("Eigenen Helfer wählen: " if card.get("target","")=="ally_unit" or card.get("strategy_target","")=="ally" else "Gegnerhelfer wählen: " if card.get("target","")=="enemy_unit" else "Ziel wählen: ")+card.name
  input_cooldown=.18;build_commands();refresh()
 else:encounter._card_play(index,"enemy")

func _pick_unit(uid: int) -> void:
 if _locked():return
 var unit: Dictionary=model.find_unit(uid)
 if unit.is_empty():return
 inspected_unit=uid;_refresh_unit_info()
 if selected_card>=0:
  _choose_target("u:"+str(uid));return
 if unit.owner==2:_choose_target("u:"+str(uid));return
 if model.can_attack(uid):
  encounter.app.battle_audio.cue("select")
  selected_unit=uid;selected_card=-1;hint_message="Angriffsziel für "+str(unit.name)+" wählen."
  input_cooldown=.18;refresh()

func _choose_target(target: String) -> void:
 if _locked():return
 if selected_card>=0:
  if model.can_play(selected_card,target):encounter._card_play(selected_card,target)
  else:hint_message="Dieses Ziel passt nicht zur Karte.";encounter.app.battle_audio.cue("error");refresh()
 elif selected_unit>=0:
  if model.can_attack(selected_unit,target):encounter._unit_attack(selected_unit,target)
  else:hint_message="Schutzhelfer zuerst angreifen.";encounter.app.battle_audio.cue("error");refresh()

func _cancel() -> void:
 if _locked():return
 encounter.app.battle_audio.cue("select")
 selected_card=-1;selected_unit=-1;inspected_unit=-1;hint_message="";build_commands();refresh()

func _power(action: String) -> void:
 if _locked():return
 encounter._act(action)

func play_event() -> void:
 selected_card=-1;selected_unit=-1;hint_message=""
 super.play_event()

func refresh() -> void:
 if results_shown:
  for button in buttons:button.disabled=model.playback_pending or encounter.busy>0 or input_cooldown>0 or encounter.suspended
  return
 _refresh_unit_info()
 turn_label.text="Runde %s · %s" % [model.round,model.actor_name().split("·")[0].strip_edges()]
 var before: bool=model.playback_pending and event_time>=0 and event_time<model.impact_time()
 var enemy_hp: int=int(model.last_event.get("before_enemy",model.enemy_health)) if before else model.enemy_health
 enemy_label.text="%s   %s / %s LP" % [model.enemy_name.to_upper(),enemy_hp,model.enemy_max_hp];enemy_bar.value=enemy_hp
 intent.text=model.enemy_intent()
 var states: Array=displayed_states()
 for i in 2:
  var member: Dictionary=model.party[i]
  var hp: int=int(model.last_event.get("before_hp",[model.party[0].hp,model.party[1].hp])[i]) if before else member.hp
  hp_labels[i].text="%s · %s / %s LP" % [member.name,hp,member.max_hp]
  var marks: Array[String]=[]
  if states[i].barrier>0:marks.append("Lichtschirm %s" % states[i].barrier)
  if states[i].bind>0:marks.append("Gefesselt")
  if states[i].silence>0:marks.append("Verstummt")
  status_labels[i].text=" / ".join(marks)
 var state: Dictionary=model.decks[model.cursor]
 var cost: int=Cards.get_card(state.hand[selected_card]).cost if selected_card>=0 and selected_card<state.hand.size() else 0
 crystal_meter.update_value(int(state.crystals),int(state.cap),cost)
 crystal_label.text="%s / %s KRISTALLE" % [state.crystals,state.cap]
 hand_label.text="%s · Hand %s / 5 · Deck %s · Ablage %s" % [model.actor_name(),state.hand.size(),state.draw.size(),state.discard.size()]
 queue_label.text="Superkraft: bereits genutzt" if state.power else "Superkraft: einmal pro eigenem Zug"
 status.text=model.message if model.playback_pending else hint_message if not hint_message.is_empty() else _available_hint()
 for button in buttons:
  var action: String=button.get_meta("battle_action","")
  if model.phase in ["victory","defeat"]:button.disabled=model.playback_pending
  elif action=="card":button.disabled=_locked() or not model.can_play(int(button.get_meta("hand_index",-1)))
  elif action in ["skill","support","special","break_free","flee","end_turn"]:button.disabled=_locked() or not model.can_act(action)
  else:button.disabled=_locked()
  if action=="card":_card_affordance(button)
  elif action in ["skill","support","special","break_free"]:_power_affordance(button)

func _refresh_unit_info() -> void:
 if not is_instance_valid(unit_info):return
 var unit: Dictionary=model.find_unit(inspected_unit)
 var before: bool=model.playback_pending and event_time>=0 and event_time<model.impact_time()
 if before:
  for old in model.last_event.get("before_board",[]):
   if int(old.uid)==inspected_unit:unit=old;break
 var shown: bool=not unit.is_empty() and not results_shown
 unit_info.visible=shown;unit_info_name.visible=shown;unit_info_stats.visible=shown;unit_info_state.visible=shown
 if not shown:return
 var owner: String=model.enemy_name if int(unit.owner)==2 else str(model.party[int(unit.owner)].name)
 unit_info_name.text=str(unit.name)+" · "+owner
 unit_info_stats.text="Angriff %s  ·  Verteidigung / LP %s / %s" % [model.unit_attack(unit),unit.hp,unit.max_hp]
 var marks: Array[String]=[]
 if unit.taunt:marks.append("Schutz (zuerst)")
 if int(unit.get("shield",0))>0:marks.append("Schild %s" % unit.shield)
 if unit.get("lifesteal",false):marks.append("Lebensraub")
 if int(unit.get("regen",0))>0:marks.append("Regeneration %s" % unit.regen)
 if unit.get("evasion",false):marks.append("1× Ausweichen")
 if int(unit.get("frozen",0))>0:marks.append("Eingefroren")
 marks.append(Feedback.state_text(Feedback.unit_state(model,unit)))
 if Feedback.unit_state(model,unit)=="sleeping":marks.append("ab nächstem eigenen Zug")
 unit_info_state.text=" · ".join(marks)

func _position(index: int) -> Vector2:
 return Vector2(1110,420 if model.enemy_profile.get("kind","")=="boss" else 360 if model.enemy_profile.get("ecology_atlas",false) else 350) if index==2 else Vector2(125,306) if index==0 else Vector2(263,306)

func _visible_board() -> Array:
 if model.playback_pending and event_time>=0 and event_time<model.impact_time():return model.last_event.get("before_board",model.board)
 var visible: Array=model.board.duplicate()
 if model.playback_pending and event_time>=model.impact_time() and event_time<model.animation_duration():
  for before in event.get("before_board",[]):
   if model.find_unit(int(before.uid)).is_empty():
    var fallen: Dictionary=before.duplicate(true);fallen["_fallen"]=true;fallen.hp=0;visible.append(fallen)
 return visible

func _unit_position(uid: int,previous: bool=false) -> Vector2:
 var entries: Array=model.last_event.get("before_board",model.board) if previous else model.board
 if model.playback_pending:
  var found:=false
  for unit in entries:
   if int(unit.uid)==uid:found=true;break
  if not found:entries=model.last_event.get("before_board",model.board)
 var retained: Array=entries.duplicate()
 retained.append_array(model.board)
 if model.playback_pending:retained.append_array(model.last_event.get("before_board",[]))
 for key in formation_slots.keys():
  var found:=false
  for unit in retained:
   if int(unit.uid)==int(key):found=true;break
  if not found:formation_slots.erase(key)
 for unit in entries:
  var owner:=int(unit.owner)
  if not formation_slots.has(int(unit.uid)):
   var used: Array=[]
   for value in formation_slots.values():
    if int(value.owner)==owner:used.append(int(value.slot))
   for slot in 3:
    if slot not in used:formation_slots[int(unit.uid)]={"owner":owner,"slot":slot};break
  if int(unit.uid)!=uid:continue
  var slot: int=formation_slots.get(uid,{"slot":0}).slot
  return Vector2(887+slot*133,451) if owner==2 else Vector2(415+slot*135,348 if owner==0 else 461)
 return Vector2(415,360)

func enemy_visual_state() -> String:
 if model.phase=="victory" and (not model.playback_pending or event_time>=model.impact_time()+.40):return "fall"
 if event_time>=model.impact_time() and event_time<model.impact_time()+.40:
  for effect in event.get("effects",[]):
   if int(effect.target)==2 and effect.kind in ["damage","weak","blocked"]:return "hit"
 if event_time>=0 and event_time<model.animation_duration() and int(event.get("source",-1))==2:return "cast"
 return "idle"

func _fighter(index: int) -> void:
 if index!=2:super._fighter(index);return
 if model.enemy_id!="":
  _regional_fighter()
  return
 var state:=enemy_visual_state()
 var frame:=0
 var fall:=0.0
 if state=="cast":frame=8+mini(3,int(event_time/model.animation_duration()*4))
 elif state=="hit":frame=12
 elif state=="fall":
  fall=clampf((event_time-model.impact_time()-.40)/maxf(.2,model.animation_duration()-model.impact_time()-.50),0,1)
  frame=13+mini(2,int(fall*3))
 var region:=_demon_rect(frame)
 var height:=190*region.size.y/304
 # Eine feste Ruhezeichnung mit langsamer Gewichtsverlagerung behaelt die Identitaet.
 var motion:=Motion.rest(_position(2),clock,2)
 if state=="cast":motion=Motion.attack(_position(2),_position(0),event_time,model.impact_time(),model.animation_duration(),"guard",0,28)
 elif state=="hit":motion=Motion.hit(_position(2),event_time,model.impact_time())
 _ground_shadow(motion.ground,Vector2(49,12),motion.lift)
 var foot: Vector2=motion.foot
 var tint:=Color("ffdfca") if state=="hit" else Color.WHITE
 if state=="fall":tint.a=1.0-smoothstep(.65,1.0,fall)*.8
 _paint_pawn(demon,region,Vector2(205,height),motion,tint)

func _draw() -> void:
 super._draw()
 if results_shown:return
 draw_set_transform(margin,0,Vector2.ONE*fit)
 _draw_targets()
 for unit in _visible_board():
  if event.get("visual","")=="card_attack" and event_time>=0 and event_time<model.animation_duration() and int(event.get("attacker_uid",-1))==int(unit.uid):continue
  var enemy_attacking:=false
  if event_time>=0 and event_time<model.animation_duration():
   for swing in event.get("unit_attacks",[]):
    if int(swing.uid)==int(unit.uid):enemy_attacking=true;break
  if enemy_attacking:continue
  _draw_unit(unit,_unit_position(int(unit.uid),model.playback_pending and event_time<model.impact_time()))
 draw_set_transform(Vector2.ZERO)

func _draw_unit(unit: Dictionary,foot: Vector2,attacking: bool=false,motion: Dictionary={}) -> void:
 if str(unit.id).begins_with("vharok_seal_") or unit.id=="seal_anchor":
  _draw_seal(unit,foot);return
 var texture: Texture2D
 var region: Rect2
 var reference_height: float=70
 var hit: bool=false
 if event_time>=model.impact_time() and event_time<model.impact_time()+.45:
  for effect in event.get("effects",[]):
   if int(effect.target)==3 and int(effect.get("unit",-1))==int(unit.uid) and effect.kind=="damage":hit=true
 var feedback_state:=Feedback.unit_state(model,unit)
 var sleeping: bool=feedback_state=="sleeping"
 var idle_phase:=0 if sleeping else int(clock*1.7+int(unit.uid)*.7)%2
 if motion.is_empty():motion=Motion.rest(foot,clock,int(unit.uid))
 var attack_phase:=clampi(int(clampf((event_time-model.impact_time()+.20)/.40,0,1)*4),0,3)
 var in_transit: bool=attacking and motion.travel
 if Cards.Strategy.has(str(unit.id)):
  var art:=Art.texture(Cards.get_card(str(unit.id)));texture=art.atlas;region=art.region;reference_height=region.size.y
 elif unit.get("form","") in Cards.HELPER_FORMS:
  var action: String="hit" if hit else "attack" if attacking and not in_transit else "idle"
  var progress: float=clampf((event_time-model.impact_time())/.45,0,1) if hit else clampf((event_time-model.impact_time()+.20)/.40,0,1)
  var frame:=Art.animation_frame(unit.form,0.0 if sleeping else float(motion.stride)*.32 if in_transit else clock+float(unit.uid)*.37,action,progress)
  var art:=Art.form_texture(unit.form,frame);texture=art.atlas;region=art.region;reference_height=Art.form_height(unit.form)
 elif int(unit.row)>=0:
  var frame:=3 if hit else (int(motion.stride*4)%2 if in_transit else attack_phase) if attacking else 0
  var r: Array=animal_data.regions[int(unit.row)][frame]
  texture=animal_texture;region=Rect2(r[0],r[1],r[2],r[3]);reference_height=float(animal_data.regions[int(unit.row)][0][3])
 else:
  var species: String=unit.species
  if species=="enemy":texture=demon;region=_demon_rect(12 if hit else 4+attack_phase if attacking else idle_phase);reference_height=304
  else:
   var data: Dictionary=atlas_data.get(species,atlas_data.fennec)
   texture=load(data.asset)
   var r: Array=data.regions[15 if unit.get("_fallen",false) else 10 if hit else attack_phase if attacking and not in_transit else 12]
   region=Rect2(r[0],r[1],r[2],r[3]);reference_height=float(data.reference_height)
 var style: String=Art.movement_style(unit.form) if unit.get("form","") in Cards.HELPER_FORMS else "pounce"
 if not attacking:motion=Motion.rest(foot,clock,int(unit.uid),style=="flier" and not sleeping)
 if hit and not attacking:motion=Motion.hit(foot,event_time,model.impact_time(),1 if int(unit.owner)==2 else -1)
 foot=motion.foot
 var scale_factor: float=(94.0 if unit.get("form","")=="ancient" else 78.0)/reference_height
 var dimensions:=region.size*scale_factor
 var left: bool=(int(unit.owner)==2) != (attacking and motion.travel and motion.returning)
 if in_transit and unit.species!="" and unit.species!="enemy" and not Cards.Strategy.has(str(unit.id)):
  var art:=_travel_art(str(unit.species),motion)
  if not art.is_empty():texture=art.texture;region=art.region;dimensions=art.dimensions
 var color:=Color("ffd2b8") if hit else Color.WHITE if attacking else Color("b8c5d4") if sleeping else Color("d6c4c9") if feedback_state=="spent" else Color.WHITE
 if unit.get("_fallen",false):
  var fall:=smoothstep(model.impact_time()+.05,model.animation_duration()-.08,event_time)
  color.a=1.0-fall;motion.lean=(.20 if int(unit.owner)<2 else -.20)*fall
 _ground_shadow(motion.ground,Vector2(31,7),motion.lift,color.a)
 var contact:=PackedVector2Array()
 for i in 32:
  var a:=i*TAU/32;contact.append(motion.ground+Vector2(cos(a)*31,sin(a)*7))
 _paint_pawn(texture,region,dimensions,motion,color,left)
 if unit.get("_fallen",false):return
 var health_color:=Color("d8ba7b") if unit.taunt else Color("b38292") if int(unit.owner)==2 else Color("85bbab")
 draw_rect(Rect2(foot+Vector2(-29,9),Vector2(58,4)),Color(.03,.07,.09,.85))
 draw_rect(Rect2(foot+Vector2(-29,9),Vector2(58*clampf(float(unit.hp)/float(unit.max_hp),0,1),4)),health_color)
 if int(unit.uid)==selected_unit or int(unit.uid)==inspected_unit:
  draw_polyline(contact+PackedVector2Array([contact[0]]),Color("f8e6a0"),2,true)
  _word("%s / %s" % [model.unit_attack(unit),unit.hp],foot+Vector2(-24,34),16,Color("f4dfab"))
 _unit_affordance(unit,foot,feedback_state,attacking)
 if int(unit.get("shield",0))>0:
  draw_arc(foot-Vector2(0,35),44,-.4,PI+.4,32,Color(.8,.7,.4,.52),2,true)
  _word(str(unit.shield),foot+Vector2(30,13),12,Color("ebd29f"))
 if unit.get("evasion",false):draw_arc(foot-Vector2(0,32),50,-.4,PI+.4,36,Color(.7,.6,.9,.38),2,true)
 if int(unit.get("frozen",0))>0:_word("STILL",foot+Vector2(-18,-77),12,Color("a8bdea"))

func _target_point(code: String) -> Vector2:
 if code=="enemy":return _position(2)-Vector2(0,75)
 if code.begins_with("h:"):return _position(int(code.substr(2)))-Vector2(0,65)
 return _unit_position(int(code.substr(2)),true)-Vector2(0,40)

func _effects() -> void:
 var visual: String=event.get("visual","")
 if visual=="ecology":
  EcologyFX.draw(self,event_time,event);_enemy_units_effects();_effect_numbers(event_time,Color("f1d8b0"));_unit_numbers();return
 if visual.begins_with("special_"):
  special_fx.draw(self,event_time,event);_enemy_units_effects();_effect_numbers(event_time,Color("e4dfb9"));_unit_numbers();return
 if not visual.begins_with("card_"):
  super._effects()
  _enemy_units_effects()
  _unit_numbers()
  return
 var source:=_position(int(event.get("source",0)))-Vector2(0,80)
 var impact: float=event.get("impact",.7)
 if visual=="card_summon":
  var foot:=_unit_position(int(event.get("unit_uid",-1)))
  if Cards.Strategy.has(str(event.get("card",""))):StrategySummonFX.draw(self,event_time,event,foot,source)
  else:
   var p:=smoothstep(.1,impact,event_time)
   var at:=source.lerp(foot-Vector2(0,28),p)-Vector2(0,sin(p*PI)*28)
   spectacle.star(self,at,13,Color("c9e6a1"),clock)
   spectacle.burst(self,foot-Vector2(0,30),Color("c9e6a1"),1-smoothstep(impact,impact+.6,event_time),15+p*37)
 elif visual=="card_attack":
  var uid:=int(event.get("attacker_uid",-1))
  var attacker: Dictionary={}
  for entry in event.get("before_board",[]):
   if int(entry.uid)==uid:attacker=entry;break
  if not attacker.is_empty():
   var motion:=_attack_motion(attacker,str(event.get("target_code","enemy")))
   _draw_unit(attacker,motion.foot,true,motion)
 elif visual=="card_spell":
  var target:=_target_point(event.get("target_code","enemy"))
  var p:=smoothstep(.1,impact,event_time)
  var at:=source.lerp(target,p)-Vector2(0,sin(p*PI)*45)
  spectacle.star(self,at,12,Color("ffe6a7"),clock)
  if event_time>=impact:spectacle.burst(self,target,Color("ffe6a7"),1-smoothstep(impact,impact+.7,event_time),22+(event_time-impact)*65)
 _enemy_units_effects()
 _effect_numbers(event_time,Color("d4eab7"))
 _unit_numbers()

func _enemy_units_effects() -> void:
 var lane:=0
 for swing in event.get("unit_attacks",[]):
  var uid:=int(swing.uid);var attacker: Dictionary={}
  for old in event.get("before_board",[]):
   if int(old.uid)==uid:attacker=old;break
  if attacker.is_empty():continue
  var motion:=_attack_motion(attacker,str(swing.target_code),lane-1)
  _draw_unit(attacker,motion.foot,true,motion);lane+=1

func _unit_numbers() -> void:
 var impact: float=event.get("impact",.7)
 if event_time>=impact and event_time<impact+1:
  for effect in event.get("effects",[]):
   if int(effect.target)==3:_word("Ausweichen" if effect.kind=="evade" else str(effect.amount),_visual_unit_position(int(effect.unit))-Vector2(8,80+(event_time-impact)*22),20,Color("cfc1ef") if effect.kind=="evade" else Color("ffe5c9"))

func _available_hint() -> String:
 if model.cursor==2:return model.enemy_name+" ist am Zug"
 var cards:=0;var ready:=0;var powers:=0
 for i in model.decks[model.cursor].hand.size():
  if model.can_play(i):cards+=1
 for unit in model.units(model.cursor):
  if model.can_attack(int(unit.uid)):ready+=1
 for action in ["skill","support","special","break_free"]:
  if model.can_act(action):powers+=1
 return "%s Karten · %s Helfer · %s Kräfte bereit" % [cards,ready,powers] if cards+ready+powers>0 else "Keine freie Aktion · Zug beenden"

func _card_affordance(button: Button) -> void:
 var index:=int(button.get_meta("hand_index",-1))
 var playable: bool=not button.disabled
 var selected: bool=index==selected_card
 var key:=str(playable)+str(selected)
 if button.get_meta("affordance","")==key:return
 button.set_meta("affordance",key);button.set_meta("playable",playable)
 var card: Dictionary=Cards.get_card(str(button.get_meta("card_id","")))
 var color:=Color("9cf2c6") if playable else Color("4b5e6b")
 if selected:color=Color("ffe49b")
 var box:=_box(Color("263e43") if playable else Color("14212f"))
 box.border_color=color;box.set_border_width_all(3 if playable or selected else 1)
 box.shadow_color=Color(color,.32 if playable else 0.0);box.shadow_size=5 if playable else 0
 button.add_theme_stylebox_override("normal",box);button.add_theme_stylebox_override("disabled",box)
 var hover:=box.duplicate();hover.bg_color=Color("38575b")
 button.add_theme_stylebox_override("hover",hover);button.add_theme_stylebox_override("pressed",hover)
 button.modulate=Color.WHITE if playable else Color(.66,.70,.77,1)

func _draw_targets() -> void:
 if _locked():return
 var choices:=Feedback.targets(model,selected_card,selected_unit)
 for code in choices:
  var at:=_target_point(code)
  var r:=64.0 if code=="enemy" else 45.0
  draw_arc(at,r,0,TAU,48,Color(.95,.86,.52,.85),3,true)
  draw_arc(at,r+5,clock*.8,clock*.8+PI*.7,24,Color(.95,.86,.52,.5),2,true)
  if code=="enemy":_word("ZIEL WÄHLEN",at+Vector2(-51,-r-12),14,Color("ffe8a8"))

func _unit_affordance(unit: Dictionary, foot: Vector2, state: String, attacking: bool) -> void:
 if attacking:return
 if str(unit.id).begins_with("vharok_seal_") or unit.id=="seal_anchor":state="ritual"
 var color:=Color("a4bdd5")
 var label: String="Schläft"
 match state:
  "ritual":label="Siegel aktiv";color=Color("ebc789")
  "ready":
   color=Color("a4f0c4");label="Bereit"
   var ring:=PackedVector2Array()
   for i in 41:
    var a:=i*TAU/40;ring.append(foot+Vector2(cos(a)*44,sin(a)*10))
   draw_polyline(ring,Color(color,.25+.10*sin(clock*2)),7,true)
   draw_polyline(ring,color,1.8,true)
   # Zwei gekreuzte Klingen statt einer kaum sichtbaren Pfeilspitze.
   var at:=foot+Vector2(42,-46)
   for side in [-1,1]:
    draw_line(at+Vector2(-7,side*8),at+Vector2(7,-side*8),color,2.5,true)
  "waiting":label="Nächster Zug";color=Color("cfbf93")
  "spent":label="Erschöpft";color=Color("a6a4b2")
  "frozen":label="Festgehalten";color=Color("b4c8f2")
  "sleeping":
   for i in 3:
    var rise:=fposmod(clock*.6+i*.31,1.0)
    _word("z" if i<2 else "Z",foot+Vector2(29+rise*10,-55-rise*30),12+i*2,Color(color,1-rise*.65))
 _word(label,foot+Vector2(-32,28),12,color)
 # Angriff und Lebenspunkte bleiben auch ohne vorheriges Antippen lesbar.
 draw_circle(foot+Vector2(-42,-3),12,Color("4d4031"))
 draw_circle(foot+Vector2(42,-3),12,Color("432b38"))
 _word(str(model.unit_attack(unit)),foot+Vector2(-47,2),13,Color("ffe5a4"))
 _word(str(unit.hp),foot+Vector2(37,2),13,Color("ffd6db"))

func _power_affordance(button: Button) -> void:
 var key:=str(button.disabled)
 if button.get_meta("affordance","")==key:return
 button.set_meta("affordance",key)
 var box:=_box(Color("244139") if not button.disabled else Color("162433"))
 box.border_color=Color("9edab6") if not button.disabled else Color("495763")
 box.set_border_width_all(2 if not button.disabled else 1)
 button.add_theme_stylebox_override("normal",box);button.add_theme_stylebox_override("disabled",box)

func _regional_fighter() -> void:
 var state:=enemy_visual_state()
 var pose:=0
 var motion:=Motion.rest(_position(2),clock,2,model.enemy_profile.kind=="moth")
 if state=="cast":
  pose=1 if event_time<model.impact_time() else 2
  if event.get("ecology_action","") not in ["warning","charge","collect","seals"]:
   motion=Motion.attack(_position(2),_effect_ground(),event_time,model.impact_time(),model.animation_duration(),"guard",0,28)
 elif state=="hit":pose=3;motion=Motion.hit(_position(2),event_time,model.impact_time())
 elif state=="fall":pose=4
 var foot: Vector2=motion.foot
 _ground_shadow(motion.ground,Vector2(62,14) if model.enemy_profile.kind=="boss" else Vector2(39,10),motion.lift)
 if state=="idle":
  if model.demon_charge:pose=1
  elif model.seal_pending:pose=5
 var enemies=load("res://scripts/regional_enemies.gd")
 var region: Rect2=enemies.region(model.enemy_id,pose)
 var texture: Texture2D=load(enemies.asset(model.enemy_id))
 var dimensions:=_regional_dimensions(pose)
 var tint:=Color("ffd6bd") if state=="hit" else Color.WHITE
 if model.demon_charge or model.seal_pending:
  var pulse:=.55+.15*sin(clock*2.5)
  draw_arc(foot-Vector2(0,70),82,0,TAU,48,Color(1.0,.35,.12,pulse) if model.demon_charge else Color(.74,.32,.88,pulse),2,true)
 _paint_pawn(texture,region,dimensions,motion,tint)

func _draw_seal(unit: Dictionary,foot: Vector2) -> void:
 var at:=foot-Vector2(0,40)
 draw_circle(at,35,Color("30253d"))
 draw_arc(at,32,0,TAU,48,Color("e2ba79"),3,true)
 draw_arc(at,39,clock*.8,clock*.8+PI*1.4,40,Color("b78ce0"),2,true)
 draw_colored_polygon(PackedVector2Array([at+Vector2(0,-26),at+Vector2(17,0),at+Vector2(0,26),at+Vector2(-17,0)]),Color("bd7a42"))
 _unit_affordance(unit,foot,"ritual",false)

# Auch erhobene Scheren und Körbe bleiben unterhalb des Statusfelds sichtbar.
func _regional_dimensions(pose: int) -> Vector2:
 var enemies=load("res://scripts/regional_enemies.gd")
 var reference_height: float=enemies.region(model.enemy_id,0).size.y
 if model.enemy_profile.get("ecology_atlas",false):
  for frame in 7:reference_height=maxf(reference_height,enemies.region(model.enemy_id,frame).size.y)
 return enemies.region(model.enemy_id,pose).size*(250.0 if model.enemy_profile.kind=="boss" else 190.0)/reference_height

func _attack_motion(unit: Dictionary,target_code: String,lane: int=0) -> Dictionary:
 var home:=_unit_position(int(unit.uid),true)
 var target:=_target_point(target_code)+Vector2(0,40)
 var style: String=Art.movement_style(str(unit.form)) if unit.get("form","") in Cards.HELPER_FORMS else "pounce"
 var motion:=Motion.attack(home,target,event_time,model.impact_time(),model.animation_duration(),style,lane)
 if model.playback_pending and event_time>=model.impact_time() and model.find_unit(int(unit.uid)).is_empty():
  # Ein im Gegentreffer gefallener Angreifer läuft nicht als Geist nach Hause.
  motion=Motion.attack(home,target,model.impact_time(),model.impact_time(),model.animation_duration(),style,lane)
  unit["_fallen"]=true
 return motion

func _travel_art(species: String,motion: Dictionary) -> Dictionary:
 if not is_instance_valid(encounter) or not encounter.app.character_metrics.has(species):return {}
 var compact: bool=Catalog.data().characters[species].get("compact",false)
 var frame:=3+posmod(int(motion.stride*4),4) if compact else 9+posmod(int(motion.stride*8),8)
 var texture: Texture2D=load(Catalog.data().characters[species].asset)
 var columns:=8 if compact else 12
 var cell:=Vector2(texture.get_width()/float(columns),texture.get_height()/float(6 if compact else 15))
 var bounds: Rect2i=encounter.app.character_metrics[species].bounds[frame]
 var reference: Rect2i=encounter.app.character_metrics[species].bounds[0 if compact else 142]
 return {"texture":texture,"region":Rect2(Vector2(frame%columns,frame/columns)*cell+Vector2(bounds.position),Vector2(bounds.size)),"dimensions":Vector2(bounds.size)*78.0/maxf(1,reference.size.y)}

func _arena_ground() -> void:
 super._arena_ground()
 for owner in 3:
  for slot in 3:
   var foot:=Vector2(887+slot*133,451) if owner==2 else Vector2(415+slot*135,348 if owner==0 else 461)
   _ellipse(foot+Vector2(0,4),Vector2(42,12),Color(.75,.65,.45,.045))
   draw_arc(foot,35,.15,PI-.15,24,Color(.67,.73,.66,.13),1,true)

func _visual_unit_position(uid: int) -> Vector2:
 if event_time<0 or event_time>=model.animation_duration():return _unit_position(uid,true)
 var attacker: Dictionary={}
 for unit in event.get("before_board",[]):
  if int(unit.uid)==uid:attacker=unit;break
 if not attacker.is_empty():
  if event.get("visual","")=="card_attack" and int(event.get("attacker_uid",-1))==uid:
   return _attack_motion(attacker,str(event.get("target_code","enemy"))).foot
  var lane:=0
  for swing in event.get("unit_attacks",[]):
   if int(swing.uid)==uid:return _attack_motion(attacker,str(swing.target_code),lane-1).foot
   lane+=1
 return _unit_position(uid,true)
