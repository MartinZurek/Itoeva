extends "res://scripts/battle_view.gd"
const Cards=preload("res://scripts/card_catalog.gd")
const Art=preload("res://scripts/card_assets.gd")
const Detail=preload("res://scripts/card_detail.gd")
const SpecialFX=preload("res://scripts/special_effects.gd")
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
 _label("SCHATTENWALD  /  Karten & Kristalle",Rect2(38,26,670,32),22,GOLD)
 turn_label=_label("",Rect2(755,28,350,30),19)
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
 enemy_label=_label("",Rect2(862,94,360,25),19,GOLD)
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
  b.set_meta("hand_index",i);b.tooltip_text=card.name+" · "+str(card.cost)+" Kristalle\n"+card.text
  b.clip_contents=true
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
 var cost:=_child_label(button,str(card.cost),Rect2(10,1,31,40),24,Color("9bdef1"))
 cost.set_meta("card_field","cost")
 _child_label(button,card.name,Rect2(43,9,145,24),15,Art.accent(card))
 var art:=TextureRect.new();art.texture=_card_texture(card)
 art.expand_mode=TextureRect.EXPAND_IGNORE_SIZE;art.stretch_mode=TextureRect.STRETCH_KEEP_ASPECT_CENTERED
 art.position=Vector2(12,37);art.size=Vector2(172,63)
 art.mouse_filter=Control.MOUSE_FILTER_IGNORE;button.add_child(art)
 var description:=_child_label(button,card.text,Rect2(12,103,172,43),14)
 description.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
 description.size=Vector2(172,43)
 _child_label(button,Art.kind_label(card),Rect2(12,146,172,20),10,Art.accent(card))

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
 if card.kind=="damage":
  selected_card=index;selected_unit=-1;hint_message="Ziel wählen: "+card.name
  input_cooldown=.18;build_commands();refresh()
 else:encounter._card_play(index,"enemy")

func _pick_unit(uid: int) -> void:
 if _locked():return
 var unit: Dictionary=model.find_unit(uid)
 if unit.is_empty():return
 inspected_unit=uid;_refresh_unit_info()
 if unit.owner==2:_choose_target("u:"+str(uid));return
 if model.can_attack(uid):
  selected_unit=uid;selected_card=-1;hint_message="Angriffsziel für "+str(unit.name)+" wählen."
  input_cooldown=.18;refresh()

func _choose_target(target: String) -> void:
 if _locked():return
 if selected_card>=0:
  if model.can_play(selected_card,target):encounter._card_play(selected_card,target)
 elif selected_unit>=0:
  if model.can_attack(selected_unit,target):encounter._unit_attack(selected_unit,target)
  else:hint_message="Schutzhelfer zuerst angreifen.";refresh()

func _cancel() -> void:
 if _locked():return
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
 turn_label.text="Runde %s · %s" % [model.round,model.actor_name()]
 var before: bool=model.playback_pending and event_time>=0 and event_time<model.impact_time()
 var enemy_hp: int=int(model.last_event.get("before_enemy",model.enemy_health)) if before else model.enemy_health
 enemy_label.text="NACHTWURZ   %s / %s LP" % [enemy_hp,model.enemy_max_hp];enemy_bar.value=enemy_hp
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
 crystal_label.text="Kristalle: %s / %s" % [state.crystals,state.cap]
 hand_label.text="%s · Hand %s / 5 · Deck %s" % [model.actor_name(),state.hand.size(),state.draw.size()]
 queue_label.text="Superkraft: bereits genutzt" if state.power else "Superkraft: einmal pro eigenem Zug"
 status.text=model.message if model.playback_pending else hint_message if not hint_message.is_empty() else "Karte spielen · Helfer antippen · Zug beenden"
 for button in buttons:
  var action: String=button.get_meta("battle_action","")
  if model.phase in ["victory","defeat"]:button.disabled=model.playback_pending
  elif action=="card":button.disabled=_locked() or not model.can_play(int(button.get_meta("hand_index",-1)))
  elif action in ["skill","support","special","break_free","flee","end_turn"]:button.disabled=_locked() or not model.can_act(action)
  else:button.disabled=_locked()

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
 var owner: String="Nachtwurz" if int(unit.owner)==2 else str(model.party[int(unit.owner)].name)
 unit_info_name.text=str(unit.name)+" · "+owner
 unit_info_stats.text="Angriff %s  ·  Verteidigung / LP %s / %s" % [unit.attack,unit.hp,unit.max_hp]
 var marks: Array[String]=[]
 if unit.taunt:marks.append("Schutz (zuerst)")
 if unit.get("evasion",false):marks.append("1× Ausweichen")
 if int(unit.get("frozen",0))>0:marks.append("Eingefroren")
 marks.append("Bereit" if unit.ready else "Nicht bereit")
 unit_info_state.text=" · ".join(marks)

func _position(index: int) -> Vector2:
 return Vector2(1110,350) if index==2 else Vector2(125,306) if index==0 else Vector2(263,306)

func _visible_board() -> Array:
 if model.playback_pending and event_time>=0 and event_time<model.impact_time():return model.last_event.get("before_board",model.board)
 return model.board

func _unit_position(uid: int,previous: bool=false) -> Vector2:
 var entries: Array=model.last_event.get("before_board",model.board) if previous else model.board
 var indices: Array[int]=[0,0,0]
 for unit in entries:
  var owner:=int(unit.owner);var slot:=indices[owner];indices[owner]+=1
  if int(unit.uid)!=uid:continue
  return Vector2(887+slot*133,451) if owner==2 else Vector2(415+slot*135,360 if owner==0 else 461)
 return Vector2(415,360)

func _fighter(index: int) -> void:
 if index!=2:super._fighter(index);return
 var frame:=int(clock*5.5)%4
 if event_time>=0 and event_time<model.animation_duration() and int(event.get("source",-1))==2:frame=8+mini(3,int(event_time/model.animation_duration()*4))
 if model.phase=="victory":frame=15
 var region:=_demon_rect(frame)
 var height:=190*region.size.y/304
 draw_texture_rect_region(demon,Rect2(_position(2)-Vector2(103,height),Vector2(205,height)),region)

func _draw() -> void:
 super._draw()
 if results_shown:return
 draw_set_transform(margin,0,Vector2.ONE*fit)
 for unit in _visible_board():
  if event.get("visual","")=="card_attack" and event_time>=0 and event_time<model.animation_duration() and int(event.get("attacker_uid",-1))==int(unit.uid):continue
  var enemy_attacking:=false
  if event_time>=0 and event_time<model.animation_duration():
   for swing in event.get("unit_attacks",[]):
    if int(swing.uid)==int(unit.uid):enemy_attacking=true;break
  if enemy_attacking:continue
  _draw_unit(unit,_unit_position(int(unit.uid),model.playback_pending and event_time<model.impact_time()))
 draw_set_transform(Vector2.ZERO)

func _draw_unit(unit: Dictionary,foot: Vector2,attacking: bool=false) -> void:
 var texture: Texture2D
 var region: Rect2
 var reference_height: float=70
 var hit: bool=false
 if event_time>=model.impact_time() and event_time<model.impact_time()+.45:
  for effect in event.get("effects",[]):
   if int(effect.target)==3 and int(effect.get("unit",-1))==int(unit.uid) and effect.kind=="damage":hit=true
 var idle_phase:=int(clock*1.7+int(unit.uid)*.7)%2
 var attack_phase:=clampi(int(clampf(event_time/model.animation_duration(),0,1)*4),0,3)
 if unit.get("form","") in Cards.HELPER_FORMS:
  var frame:=3 if hit else 2 if attacking else idle_phase
  var art:=Art.form_texture(unit.form,frame);texture=art.atlas;region=art.region;reference_height=Art.form_height(unit.form)
 elif int(unit.row)>=0:
  var frame:=3 if hit else attack_phase if attacking else 0 if idle_phase==0 else 3
  var r: Array=animal_data.regions[int(unit.row)][frame]
  texture=animal_texture;region=Rect2(r[0],r[1],r[2],r[3]);reference_height=float(animal_data.regions[int(unit.row)][0][3])
 else:
  var species: String=unit.species
  if species=="enemy":texture=demon;region=_demon_rect(12 if hit else 4+attack_phase if attacking else idle_phase);reference_height=304
  else:
   var data: Dictionary=atlas_data.get(species,atlas_data.fennec)
   texture=load(data.asset)
   var r: Array=data.regions[10 if hit else attack_phase if attacking else 12+idle_phase]
   region=Rect2(r[0],r[1],r[2],r[3]);reference_height=float(data.reference_height)
 var scale_factor: float=(94.0 if unit.get("form","")=="ancient" else 78.0)/reference_height
 var dimensions:=region.size*scale_factor
 var contact:=PackedVector2Array()
 for i in 32:
  var a:=i*TAU/32;contact.append(foot+Vector2(cos(a)*31,sin(a)*7))
 draw_colored_polygon(contact,Color(0,0,0,.34))
 draw_texture_rect_region(texture,Rect2(foot-Vector2(dimensions.x*.5,dimensions.y),dimensions),region,Color("ffd2b8") if hit else Color.WHITE)
 var health_color:=Color("d8ba7b") if unit.taunt else Color("b38292") if int(unit.owner)==2 else Color("85bbab")
 draw_rect(Rect2(foot+Vector2(-29,9),Vector2(58,4)),Color(.03,.07,.09,.85))
 draw_rect(Rect2(foot+Vector2(-29,9),Vector2(58*clampf(float(unit.hp)/float(unit.max_hp),0,1),4)),health_color)
 if int(unit.uid)==selected_unit or int(unit.uid)==inspected_unit:
  draw_polyline(contact+PackedVector2Array([contact[0]]),Color("f8e6a0"),2,true)
  _word("%s / %s" % [unit.attack,unit.hp],foot+Vector2(-24,34),16,Color("f4dfab"))
 if unit.ready and int(unit.owner)==model.cursor:_word("↑",foot+Vector2(-4,-75),17,Color("a6e9c2"))
 if unit.get("evasion",false):draw_arc(foot-Vector2(0,32),50,-.4,PI+.4,36,Color(.7,.6,.9,.38),2,true)
 if int(unit.get("frozen",0))>0:_word("STILL",foot+Vector2(-18,-77),12,Color("a8bdea"))

func _target_point(code: String) -> Vector2:
 if code=="enemy":return _position(2)-Vector2(0,75)
 if code.begins_with("h:"):return _position(int(code.substr(2)))-Vector2(0,65)
 return _unit_position(int(code.substr(2)),true)-Vector2(0,40)

func _effects() -> void:
 var visual: String=event.get("visual","")
 if visual.begins_with("special_"):
  special_fx.draw(self,event_time,event);_effect_numbers(event_time,Color("e4dfb9"));_unit_numbers();return
 if not visual.begins_with("card_"):
  super._effects()
  _enemy_units_effects()
  _unit_numbers()
  return
 var source:=_position(int(event.get("source",0)))-Vector2(0,80)
 var impact: float=event.get("impact",.7)
 if visual=="card_summon":
  var foot:=_unit_position(int(event.get("unit_uid",-1)))
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
   var start:=_unit_position(uid,true)
   var target:=_target_point(event.get("target_code","enemy"))+Vector2(0,40)
   var p:=sin(clampf(event_time/model.animation_duration(),0,1)*PI)
   _draw_unit(attacker,start.lerp(target,p)-Vector2(0,sin(p*PI)*28),true)
 elif visual=="card_spell":
  var target:=_target_point(event.get("target_code","enemy"))
  var p:=smoothstep(.1,impact,event_time)
  var at:=source.lerp(target,p)-Vector2(0,sin(p*PI)*45)
  spectacle.star(self,at,12,Color("ffe6a7"),clock)
  if event_time>=impact:spectacle.burst(self,target,Color("ffe6a7"),1-smoothstep(impact,impact+.7,event_time),22+(event_time-impact)*65)
 _effect_numbers(event_time,Color("d4eab7"))
 _unit_numbers()

func _enemy_units_effects() -> void:
 for swing in event.get("unit_attacks",[]):
  var uid:=int(swing.uid);var attacker: Dictionary={}
  for old in event.get("before_board",[]):
   if int(old.uid)==uid:attacker=old;break
  if attacker.is_empty():continue
  var start:=_unit_position(uid,true)
  var target:=_target_point(str(swing.target_code))+Vector2(0,40)
  var p:=sin(clampf(event_time/model.animation_duration(),0,1)*PI)
  _draw_unit(attacker,start.lerp(target,p)-Vector2(0,sin(p*PI)*25),true)

func _unit_numbers() -> void:
 var impact: float=event.get("impact",.7)
 if event_time>=impact and event_time<impact+1:
  for effect in event.get("effects",[]):
   if int(effect.target)==3:_word("Ausweichen" if effect.kind=="evade" else str(effect.amount),_unit_position(int(effect.unit),true)-Vector2(8,80+(event_time-impact)*22),20,Color("cfc1ef") if effect.kind=="evade" else Color("ffe5c9"))
