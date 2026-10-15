extends Control

# Fixed design coordinates, uniformly fitted on phones; native buttons retain hit rectangles.
const Catalog = preload("res://scripts/world_catalog.gd")
const Forest = preload("res://scripts/forest_catalog.gd")
const Spectacle = preload("res://scripts/battle_effects.gd")
const GOLD := Color("dfc68e")
const INK := Color("101b2c")
const TEXT := Color("f6ecd4")
var encounter: Node
var model: RefCounted
var layout: Control
var buttons: Array[Button] = []
var hp_labels: Array[Label] = []
var mp_labels: Array[Label] = []
var hp_bars: Array[Control] = []
var mp_bars: Array[Control] = []
var limit_bars: Array[Control] = []
var enemy_bar: Control
var status: Label
var intent: Label
var queue_label: Label
var turn_label: Label
var enemy_label: Label
var background: Texture2D
var flames: Texture2D
var demon: Texture2D
var allies: Array[Texture2D] = []
var atlas_data: Dictionary = {}
var clock := 0.0
var event_time := -1.0
var event: Dictionary = {}
var submenu := "commands"
var input_cooldown := 0.0
var shake := Vector2.ZERO
var typography: Font = ThemeDB.fallback_font
var fit := 1.0
var margin := Vector2.ZERO
var skip_ui := false
var results_shown := false
var spectacle = Spectacle.new()
var status_labels: Array[Label] = []
var result_time := 0.0
var experience_labels: Array[Label] = []
var experience_bars: Array[Control] = []
var reward_rows: Array[Control] = []
var reward_icons: Array[Control] = []
var gained_labels: Array[Label] = []
const REACTIONS := {
 "fennec":"Wir haben den Weg gemeinsam freigemacht.",
 "starlet":"Ein kleiner Lichtfunke reicht manchmal aus.",
 "wyrmling":"Jetzt können wir wieder durchatmen.",
 "gloop":"Ein Moment Ruhe tut uns beiden gut.",
 "puffling":"Der Weg ist frei. Lass uns weiterziehen.",
 "hootlet":"Auch dieser Schatten ist vorübergezogen.",
 "mosslyn":"Wo ein Schatten fällt, kann ein neues Blatt wachsen.",
 "nocti":"Unser Echo hat den Weg zurück ins Licht gefunden."
}

func _ready() -> void:
 mouse_filter = Control.MOUSE_FILTER_IGNORE
 set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
 background = load("res://assets/forest/arena.png")
 demon = load("res://assets/combat/nachtwurz-atlas.png")
 flames = load("res://assets/combat/flame-atlas.png")
 atlas_data = JSON.parse_string(FileAccess.get_file_as_string("res://data/combat_atlases.json"))
 atlas_data.merge(JSON.parse_string(FileAccess.get_file_as_string("res://data/new_combat_atlases.json")),true)
 for member in model.party:
  allies.append(load(atlas_data[member.species].asset))
 _build()
 resized.connect(_fit)
 _fit()
 refresh()

func _fit() -> void:
 fit = minf(size.x/1280.0,size.y/720.0)
 margin = (size - Vector2(1280,720)*fit)*0.5
 if is_instance_valid(layout):
  layout.position = margin
  layout.scale = Vector2.ONE*fit

func _box(color: Color = INK, border: bool = true) -> StyleBoxFlat:
 var box := StyleBoxFlat.new()
 box.bg_color = color
 box.border_color = Color("7d786b")
 box.set_border_width_all(1 if border else 0)
 box.set_corner_radius_all(9)
 box.content_margin_left = 16
 box.content_margin_right = 16
 box.content_margin_top = 10
 box.content_margin_bottom = 10
 return box

func _panel(rect: Rect2) -> Panel:
 var panel := Panel.new()
 panel.position = rect.position
 panel.size = rect.size
 panel.mouse_filter = Control.MOUSE_FILTER_IGNORE
 panel.add_theme_stylebox_override("panel",_box(Color(.035,.062,.108,.94)))
 layout.add_child(panel)
 return panel

func _label(text: String, rect: Rect2, font_size: int = 18, color: Color = TEXT) -> Label:
 var label := Label.new()
 label.text = text
 label.position = rect.position
 label.size = rect.size
 label.add_theme_font_size_override("font_size",font_size)
 label.add_theme_color_override("font_color",color)
 label.mouse_filter = Control.MOUSE_FILTER_IGNORE
 layout.add_child(label)
 return label

func _bar(rect: Rect2,color: Color) -> Control:
 var bar: Control = load("res://scripts/battle_meter.gd").new()
 bar.position=rect.position
 bar.size=rect.size
 bar.fill_color=color
 bar.mouse_filter=Control.MOUSE_FILTER_IGNORE
 layout.add_child(bar)
 return bar

func _build() -> void:
 layout = Control.new()
 layout.size = Vector2(1280,720)
 layout.mouse_filter = Control.MOUSE_FILTER_IGNORE
 add_child(layout)
 _panel(Rect2(20,16,1240,56))
 _label("SCHATTENWALD   /   Die dunkle Lichtung",Rect2(38,25,600,32),21,GOLD)
 turn_label = _label("",Rect2(942,27,215,30),18)
 var menu := _button("Menü",Rect2(1152,25,88,38),encounter.app.show_menu)
 menu.set_meta("battle_action","menu")
 _panel(Rect2(775,86,465,79))
 enemy_label = _label("NACHTWURZ",Rect2(794,92,395,27),21,GOLD)
 enemy_bar = _bar(Rect2(794,125,421,8),Color("ba717d"))
 enemy_bar.max_value = model.MAX_ENEMY_HP
 intent = _label("",Rect2(794,137,421,24),16,Color("c5bfdb"))
 _panel(Rect2(22,85,674,47))
 queue_label = _label("",Rect2(38,92,646,30),16,Color("c0d3e9"))
 _panel(Rect2(20,500,1240,200))
 _label("GRUPPE",Rect2(42,510,600,25),15,GOLD)
 for i in 2:
  var y := 542.0+i*71.0
  _label(model.party[i].name,Rect2(42,y,147,26),21)
  hp_labels.append(_label("",Rect2(206,y,163,25),17))
  mp_labels.append(_label("",Rect2(399,y,128,25),17,Color("aabfee")))
  hp_bars.append(_bar(Rect2(206,y+30,163,10),Color("79b7a3")))
  mp_bars.append(_bar(Rect2(399,y+30,127,10),Color("638dcd")))
  limit_bars.append(_bar(Rect2(551,y+30,103,10),Color("d9b965")))
  _label("HERZ",Rect2(551,y,99,25),14,GOLD)
  status_labels.append(_label("",Rect2(206,y+43,452,23),14,Color("c2dce9")))
  hp_bars[i].max_value = model.party[i].max_hp
  mp_bars[i].max_value = model.party[i].max_mp
 status = _label("",Rect2(38,449,1200,42),21)
 status.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
 _label("Zeit zum Entscheiden · Licht trifft besonders stark",Rect2(42,686,607,14),11,Color("9baebe"))
 build_commands()

func _button(text: String, rect: Rect2, callback: Callable) -> Button:
 var button: Button = encounter.app._button(text,callback)
 button.custom_minimum_size = Vector2.ZERO
 button.position = rect.position
 button.size = rect.size
 button.add_theme_font_size_override("font_size",18)
 button.add_theme_stylebox_override("normal",_box(Color("1b2c43")))
 button.add_theme_stylebox_override("hover",_box(Color("304963")))
 button.add_theme_stylebox_override("pressed",_box(Color("4b5b65")))
 button.add_theme_stylebox_override("disabled",_box(Color("111d2d")))
 layout.add_child(button)
 return button

func build_commands() -> void:
 for button in buttons:
  encounter.app.menu_buttons.erase(button)
  button.queue_free()
 buttons.clear()
 var entries: Array = []
 if model.phase == "intro": return
 if model.phase == "victory":
  if not model.playback_pending:
   _show_results()
  entries = [["Weiter in den Wald","finish",encounter.finish]]
 elif model.phase == "defeat":
  entries = [["Erneut versuchen","retry",encounter._retry],["Zurück zum Waldsaum","retreat",encounter._retreat]]
 elif submenu == "magic" and model.cursor < 2:
  var ability: Dictionary = model.skill()
  entries = [[ability.name+"  ·  6 MP","skill",encounter._act.bind("skill",-1)],
   [model.support_skill().name+"  ·  8 MP","support",encounter._act.bind("support",-1)],
   [model.limit_name()+"  ·  100 Herz","limit",encounter._act.bind("limit",-1)],
   ["Zurück","back",choose.bind("commands")]]
 elif submenu == "items":
  for i in 2:
   entries.append(["Heiltrank → "+model.party[i].name,"potion",encounter._act.bind("potion",i),i])
   entries.append(["Äther → "+model.party[i].name,"ether",encounter._act.bind("ether",i),i])
  entries.append(["Zurück","back",choose.bind("commands")])
 else:
  entries = [["Angriff","strike",encounter._act.bind("strike",-1)],
   ["Fähigkeiten","magic",choose.bind("magic")],
   ["Verteidigen  ·  +4 MP","guard",encounter._act.bind("guard",-1)],
   ["Gegenstände  ·  %s / %s" % [model.potion_count(),model.ethers],"items",choose.bind("items")],
   ["Rückzug","flee",encounter._act.bind("flee",-1)]]
  if model.cursor < 2 and (model.party[model.cursor].bind > 0 or model.party[model.cursor].silence > 0):
   entries.append(["Fesseln lösen","break_free",encounter._act.bind("break_free",-1)])
 for i in entries.size():
  var entry: Array = entries[i]
  var rect := Rect2(660,603,560,62) if results_shown else Rect2(700+(i%2)*268,537+(i/2)*48,252,42)
  var button := _button(entry[0],rect,entry[2])
  button.set_meta("battle_action",entry[1])
  button.set_meta("battle_target",entry[3] if entry.size()>3 else -1)
  buttons.append(button)

func _show_results() -> void:
 if results_shown: return
 results_shown = true
 result_time = 0.0
 input_cooldown = .25
 for child in layout.get_children():
  if child is CanvasItem: child.visible = false
 _panel(Rect2(32,24,1216,78))
 _label("S I E G",Rect2(64,36,700,56),38,GOLD)
 var menu := _button("Menü",Rect2(1100,41,112,42),encounter.app.show_menu)
 menu.set_meta("battle_action","menu")
 _panel(Rect2(640,124,592,461))
 _label("NACHTWURZ BESIEGT",Rect2(664,141,535,36),24,GOLD)
 _label("ERFAHRUNG",Rect2(664,195,535,27),17,Color("a9cabc"))
 for i in model.result.get("members",[]).size():
  var member: Dictionary = model.result.members[i]
  var y: float = 234.0 + i * 66.0
  _label(member.name,Rect2(664,y,210,32),23)
  gained_labels.append(_label("+0 XP",Rect2(1030,y,174,32),25,GOLD))
  experience_labels.append(_label("",Rect2(664,y+29,535,25),16,Color("b9cbd6")))
  var bar:=_bar(Rect2(664,y+55,535,5),Color("ddc48c"))
  bar.max_value=maxf(1,member.gained);bar.value=0;experience_bars.append(bar)
 _label("GEFUNDENE GEGENSTÄNDE",Rect2(664,386,535,28),17,Color("a9cabc"))
 for i in model.result.get("items",[]).size():
  var item: Dictionary = model.result.items[i]
  var total := int(model.inventory.get(item.id,0))
  var icon: Control=load("res://scripts/reward_emblem.gd").new()
  icon.kind=item.id;icon.position=Vector2(662,426+i*43);icon.size=Vector2(38,38)
  layout.add_child(icon);reward_icons.append(icon)
  reward_rows.append(_label("%s × %s" % [item.count,item.name],Rect2(704,426+i*43,300,34),23))
  reward_rows.append(_label("Vorrat: %s → %s" % [total-int(item.count),total],Rect2(1014,431+i*43,200,29),16,Color("b9cbd6")))
 _label("Heiltränke bleiben in deinem Vorrat erhalten.",Rect2(664,538,535,30),16,Color("b9cbd6"))
 _panel(Rect2(48,494,560,171))
 var lead: Dictionary = model.party[0]
 _label(lead.name+" und "+model.party[1].name,Rect2(72,510,510,35),23,GOLD)
 var reaction := _label(REACTIONS.get(lead.species,"Der Weg ist wieder frei."),Rect2(72,552,510,75),22)
 reaction.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
 _label("Der tiefe Wald wartet auf euch.",Rect2(72,626,510,27),16,Color("b9cbd6"))
 _update_results()

func result_progress(index: int) -> float:
 return smoothstep(.35+index*.25,1.8+index*.25,result_time)

func _update_results() -> void:
 for i in experience_labels.size():
  var member: Dictionary = model.result.members[i]
  var progress:=result_progress(i)
  var total:=int(round(lerpf(float(member.before),float(member.total),progress)))
  experience_labels[i].text="%s → %s XP   ·   Gesamt: %s XP" % [member.before,member.total,total]
  gained_labels[i].text="+%s XP" % (total-int(member.before))
  experience_bars[i].value=total-int(member.before)
 for i in reward_rows.size():
  var row:=i/2
  var reveal:=smoothstep(2.15+row*.40,2.5+row*.40,result_time)
  reward_rows[i].modulate.a=reveal
  reward_rows[i].position.y=(426.0 if i%2==0 else 431.0)+row*43.0+(1.0-reveal)*15.0
 for i in reward_icons.size():
  var reveal:=smoothstep(2.15+i*.40,2.5+i*.40,result_time)
  reward_icons[i].modulate.a=reveal
  reward_icons[i].position.y=426+i*43+(1-reveal)*15

func victory_pose(index: int) -> Dictionary:
 var species: String=model.party[index].species
 var t:=maxf(0,result_time-float(index)*.22)
 var greeting:=smoothstep(0,.35,t)
 var hop:=sin(clampf((t-.35)/.7,0,1)*PI)* (16.0 if species in ["fennec","puffling","wyrmling"] else 7.0)
 var floaty:=species in ["starlet","hootlet","nocti"]
 return {"frame":14,"lift":greeting*4.0+hop+(sin(t*2.0)*4.0 if floaty else sin(t*1.4)*1.0),
  "lean":sin(t*1.5)*(.035 if species=="gloop" else .012)}

func choose(page: String) -> void:
 if input_cooldown > 0 or model.playback_pending or model.cursor == 2 or model.phase != "battle": return
 submenu = page
 input_cooldown = .18
 build_commands()
 refresh()

func refresh() -> void:
 turn_label.text = "Runde %s · %s" % [model.round,model.actor_name()]
 queue_label.text = "ZUGFOLGE   " + "   ›   ".join(model.upcoming())
 var before_impact: bool = model.playback_pending and event_time >= 0.0 and event_time < model.impact_time()
 var enemy_hp: int = int(model.last_event.get("before_enemy",model.enemy_health)) if before_impact else model.enemy_health
 enemy_label.text = "NACHTWURZ       %s / %s LP" % [enemy_hp,model.MAX_ENEMY_HP]
 intent.text = "LICHTSCHWÄCHE   ·   " + model.enemy_intent()
 if model.enemy_burn > 0: intent.text = "Brennen %s  ·  " % model.enemy_burn+model.enemy_intent()
 if model.enemy_weaken > 0: intent.text = "Geschwächt %s  ·  " % model.enemy_weaken+model.enemy_intent()
 enemy_bar.value = enemy_hp
 status.text = model.message if model.playback_pending or model.phase != "battle" else model.actor_name()+" ist am Zug."
 if submenu == "magic" and not model.playback_pending and model.cursor < 2:
  status.text = model.support_skill().name+": "+model.support_skill().description
 if model.phase == "victory" and not model.playback_pending: status.text = "SIEG · Nachtwurz zerfällt. Der Weg ist frei."
 if model.phase == "defeat" and not model.playback_pending: status.text = "Die Gruppe ist erschöpft. Du kannst es erneut versuchen."
 for i in 2:
  var member: Dictionary = model.party[i]
  var hp: int = int(model.last_event.get("before_hp",[model.party[0].hp,model.party[1].hp])[i]) if before_impact else member.hp
  hp_labels[i].text = "%s / %s LP" % [hp,member.max_hp]
  mp_labels[i].text = "%s / %s MP" % [member.mp,member.max_mp]
  hp_bars[i].value = hp
  mp_bars[i].value = member.mp
  limit_bars[i].value = member.limit
  var states: Array = displayed_states()
  var words: Array[String] = []
  if int(states[i].get("barrier",0)) > 0: words.append("Lichtschirm %s" % states[i].barrier)
  if int(states[i].get("bind",0)) > 0: words.append("Gefesselt · 1 Zug")
  if int(states[i].get("silence",0)) > 0: words.append("Verstummt · %s" % states[i].silence)
  status_labels[i].text = "  /  ".join(words)
 for button in buttons:
  var action: String = button.get_meta("battle_action")
  var locked: bool = input_cooldown > 0 or model.playback_pending or model.cursor == 2 or encounter.busy > 0
  if model.phase in ["victory","defeat"]:
   button.disabled = model.playback_pending or encounter.busy > 0 or input_cooldown > 0 or encounter.suspended
  elif action in ["magic","items","back"]: button.disabled = locked
  else: button.disabled = locked or not model.can_act(action,int(button.get_meta("battle_target",-1)))

func displayed_states() -> Array:
 if model.playback_pending and event_time >= 0 and event_time < model.impact_time():
  return model.last_event.get("before_status",model._visual_state())
 return model._visual_state()

func play_event() -> void:
 event = model.last_event.duplicate(true)
 event_time = 0.0
 submenu = "commands"
 build_commands()
 refresh()

func _process(delta: float) -> void:
 if not is_instance_valid(encounter) or encounter.suspended: return
 clock += delta
 if results_shown:
  result_time += delta
  _update_results()
 var cooldown_before := input_cooldown
 input_cooldown = maxf(0,input_cooldown-delta)
 if cooldown_before > 0 and input_cooldown == 0: refresh()
 if event_time >= 0.0:
  var previous := event_time
  event_time += delta
  if previous < model.impact_time() and event_time >= model.impact_time(): refresh()
 queue_redraw()

func _draw() -> void:
 if background == null: return
 var duration: float = model.animation_duration()
 var cinematic := event_time >= 0 and event_time < duration and not results_shown
 var pad := sin(clampf(event_time/duration,0,1)*PI)*10.0 if cinematic else 0.0
 draw_texture_rect(background,Rect2(Vector2(-pad,-pad),size+Vector2.ONE*pad*2),false,Color(.53,.64,.78))
 draw_rect(Rect2(Vector2.ZERO,size),Color(.012,.028,.065,.24))
 draw_set_transform(margin,0,Vector2.ONE*fit)
 _arena_fire()
 if results_shown:_victory_sparkles()
 var impact: bool = model.playback_pending and event_time >= model.impact_time() and event_time < model.impact_time()+.26
 shake = Vector2(sin(event_time*89.0)*3.5,cos(event_time*72.0)*1.8) if impact and event.get("element","") in ["physical","shadow"] else Vector2.ZERO
 for i in range(42):
  var p := Vector2(fposmod(i*133.37+sin(clock*.18+i)*18.0,1280),fposmod(i*71.13-clock*(4.0+i%4),390)+85)
  draw_circle(p,1.1,Color(.63,.76,.91,.12+.12*sin(clock+i)))
 for i in [1,0,2]:
  if results_shown and i == 2: continue
  _fighter(i)
 if not results_shown: spectacle.persistent(self,displayed_states(),clock)
 if not results_shown and event_time >= 0.0 and event_time < model.animation_duration(): _effects()
 if model.phase == "victory" and not model.playback_pending and not results_shown:
  _word("S I E G",Vector2(519,290),48,GOLD)
 draw_set_transform(Vector2.ZERO)

func _position(index: int) -> Vector2:
 return Vector2(902,455) if index == 2 else Vector2(300,439) if index == 0 else Vector2(445,393)

func _demon_rect(frame: int) -> Rect2:
 # Generated sheet rows have unequal gutters. Explicit windows prevent clipped horns.
 var rows := [0.0,304.0,580.0,894.0,1199.0]
 return Rect2((frame%4)*328.0,rows[frame/4],328.0,rows[frame/4+1]-rows[frame/4])

func _fighter(index: int) -> void:
 var foot := (Vector2(225,410) if index==0 else Vector2(440,410)) + shake if results_shown else _position(index)+shake
 if results_shown and index < 2 and model.party[index].hp > 0:
  foot.y -= float(victory_pose(index).lift)
 var source: int = int(event.get("source",-1))
 var active_event: bool = not results_shown and event_time >= 0.0 and event_time < model.animation_duration()-.1
 var casting: bool = active_event and source == index
 var hit := false
 if active_event and event_time > model.impact_time() and event_time < model.impact_time()+.48:
  for effect in event.get("effects",[]):
   if int(effect.target) == index and effect.kind in ["damage","weak","blocked"]: hit = true
 if casting and event.element == "physical": foot.x += sin(clampf(event_time/1.3,0,1)*PI)*170.0
 if casting and index == 2 and event.element == "shadow" and event.title != "Nebelwoge": foot.x -= sin(clampf(event_time/1.3,0,1)*PI)*150.0
 if hit: foot.x += sin(event_time*55)*4.0
 draw_set_transform(margin+(foot+Vector2(0,3))*fit,0,Vector2(fit,fit*.25))
 draw_circle(Vector2.ZERO,58 if index == 2 else 37,Color(0,0,0,.32))
 draw_set_transform(margin,0,Vector2.ONE*fit)
 if index == 2:
  var frame := int(clock*5.5)%4
  if casting:
   frame = (8 if event.title in ["Nebelwoge","Wurzelbann","Holzsplitter"] else 4)+mini(3,int(event_time/model.animation_duration()*4.0))
  if hit: frame = 12
  if model.phase == "victory" and event_time > model.impact_time()+.25: frame = 14 if event_time < model.animation_duration()-.2 else 15
  var region := _demon_rect(frame)
  var height := 286.0*region.size.y/304.0
  var width := 286.0*328.0/304.0
  var tint := Color(1.0,1.0,1.0)
  if hit: tint = Color(1.55,1.25,1.12)
  if model.phase == "intro": tint.a = smoothstep(.6,2.6,model.intro_time)
  if model.phase == "victory" and event_time > model.impact_time()+.4: tint.a = maxf(.12,1.0-(event_time-model.impact_time()-.4)*.8)
  draw_texture_rect_region(demon,Rect2(foot-Vector2(width*.5,height),Vector2(width,height)),region,tint)
 else:
  var member: Dictionary = model.party[index]
  var frame := 12
  if casting:
   if event.element == "physical": frame = mini(3,int(event_time*3.2))
   elif event.element in ["nature","light","fire","water","wind","heal","moon","decoy"]: frame = 4+mini(3,int(event_time*3.2))
   elif event.element == "guard": frame = 8
  if hit: frame = 10
  if member.hp == 0: frame = 15
  if model.phase == "victory" and not model.playback_pending and member.hp > 0: frame = 14
  var data: Dictionary = atlas_data[member.species]
  var bounds: Array = data.regions[frame]
  var height := (140.0 if index == 0 else 108.0)*Catalog.relative_height(member.species)/maxf(.7,Catalog.relative_height(model.party[0].species))
  height = clampf(height,90,165)
  var scale_factor: float = height/float(data.reference_height)
  var source_rect := Rect2(bounds[0],bounds[1],bounds[2],bounds[3])
  var dimension := source_rect.size*scale_factor
  var tint := Color("eee1c4")
  if hit: tint = Color("ffd4bd")
  if member.hp == 0: tint = Color(.49,.48,.56,.65)
  if results_shown and member.hp>0:
   var pose:=victory_pose(index)
   draw_set_transform(margin+foot*fit,float(pose.lean),Vector2.ONE*fit)
   draw_texture_rect_region(allies[index],Rect2(Vector2(-dimension.x*.5,-dimension.y),dimension),source_rect,tint)
   draw_set_transform(margin,0,Vector2.ONE*fit)
  else:draw_texture_rect_region(allies[index],Rect2(foot-Vector2(dimension.x*.5,dimension.y),dimension),source_rect,tint)
  if member.evasion:
   draw_texture_rect_region(allies[index],Rect2(foot-Vector2(dimension.x*.5+35,dimension.y),dimension),source_rect,Color(.6,.75,.85,.15))
  if member.haste:
   draw_arc(foot-Vector2(0,height*.4),height*.4,2.5,5.9,24,Color(.6,.9,.75,.6),2,true)
  if member.guard:
   draw_arc(foot-Vector2(0,height*.45),height*.55,-1.25,1.25,30,Color(.53,.78,1,.6),3,true)
  if index == model.cursor and not model.playback_pending and model.phase == "battle":
   var p := foot-Vector2(0,height+20+sin(clock*3)*3)
   draw_colored_polygon(PackedVector2Array([p+Vector2(-8,-6),p+Vector2(8,-6),p+Vector2(0,5)]),GOLD)

func _word(text: String, point: Vector2, font_size: int, color: Color) -> void:
 draw_string(typography,point+Vector2(2,3),text,HORIZONTAL_ALIGNMENT_LEFT,-1,font_size,Color(0,0,0,.9))
 draw_string(typography,point,text,HORIZONTAL_ALIGNMENT_LEFT,-1,font_size,color)

func _effects() -> void:
 var t := event_time
 if event.get("visual","") in ["wildlife","star_beam","star_nova","star_barrier","wood_shards","root_bind","root_release"]:
  spectacle.draw(self,t,event)
  _effect_numbers(t,Color("fff2d9"))
  return
 var source := _position(int(event.get("source",0)))-Vector2(0,90)
 var effects: Array = event.get("effects",[])
 var target := _position(int(effects[0].target))-Vector2(0,95) if not effects.is_empty() else _position(2)-Vector2(0,95) if event.get("element","") == "moon" else source
 if int(event.get("source",0)) < 2:
  for effect in effects:
   if int(effect.target)==2 and effect.kind in ["damage","weak","burn"]:
    target=_position(2)-Vector2(0,95)
    break
 var element: String = event.get("visual",event.get("element",""))
 var palette := {"light":Color("ffe49c"),"nature":Color("9bcc98"),"fire":Color("f8ad69"),"water":Color("8fd1e5"),"wind":Color("c1e2ca"),"shadow":Color("a49ad9"),"heal":Color("9be2bf"),"guard":Color("a5d5f2"),"physical":Color("ffe5c2"),"moon":Color("bdacf1"),"decoy":Color("becdbb")}
 var color: Color = palette.get(element,GOLD)
 if element == "physical" or element == "shadow" and event.title != "Nebelwoge":
  if t > .45 and t < .95:
   var progress := clampf((t-.45)/.5,0,1)
   var trail := PackedVector2Array()
   for i in 15:
    var angle := lerpf(-1.2,1.7,i/14.0)
    trail.append(target+Vector2(cos(angle)*63,sin(angle)*46)*progress)
   draw_polyline(trail,Color(color,.85*(1-progress)),6,true)
 elif element not in ["flee","stagger"]:
  if t < .65:
   var progress := smoothstep(.1,.65,t)
   var orb := source.lerp(target,progress)+Vector2(0,-sin(progress*PI)*60)
   draw_circle(orb,14,Color(color,.1))
   draw_circle(orb,6,Color(color,.7))
   draw_circle(orb,2.5,Color("fff9dc"))
  if t > .55 and t < 1.3:
   var progress := (t-.55)/.75
   draw_arc(target,18+progress*73,0,TAU,48,Color(color,(1-progress)*.8),2.5,true)
   for i in 19:
    var angle := i*TAU/19.0+t*1.1
    var at := target+Vector2(cos(angle),sin(angle))*(18+progress*(45+i%5*8))
    draw_line(at,at+Vector2(cos(angle),sin(angle))*9,Color(color,(1-progress)*.9),2,true)
   if element == "nature":
    # A small flock follows the call instead of inflating the player's body.
    for i in 3:
     var at := source.lerp(target,clampf((t-.45)*1.6+i*.07,0,1))+Vector2(0,-65+i*16)
     var wing := sin(t*24+i)*9
     draw_polyline(PackedVector2Array([at+Vector2(-9,wing),at,at+Vector2(9,wing)]),color,2.5,true)
 _species_effects(t,element,source,target,color)
 _effect_numbers(t,color)

func _effect_numbers(t: float, color: Color) -> void:
 var impact: float = event.get("impact",.62)
 if t < impact+.03 or t > impact+1.05: return
 var offsets := {}
 for effect in event.get("effects",[]):
  var target_index := int(effect.target)
  if target_index < 0 or target_index > 2: continue
  if int(effect.amount)==0 and effect.kind in ["heal","mp"]: continue
  var offset := int(offsets.get(target_index,0))
  offsets[target_index] = offset+1
  var point := _position(target_index)-Vector2(30,154+(t-impact)*28+offset*28)
  var text := str(effect.amount)
  var tint := Color("fff2d9")
  match effect.kind:
   "heal","mp": text="+"+text;tint=color
   "weak": text+=" · SCHWÄCHE"
   "blocked": text+=" · GESCHÜTZT"
   "evade": text="AUSGEWICHEN"
   "burn": text+=" · BRENNEN"
   "barrier": text="+%s SCHUTZ" % effect.amount;tint=Color("addef5")
   "barrier_hit": text="SCHUTZ −%s" % effect.amount;tint=Color("addef5")
   "barrier_break": text="BARRIERE BRICHT";tint=Color("addef5")
   "silence": text="VERSTUMMT";tint=Color("d3b6e5")
   "bind": text="GEFESSELT";tint=Color("d3b6e5")
   "release": text="BEFREIT";tint=Color("cbe3ab")
  _word(text,point,22 if effect.kind in ["barrier_hit","barrier_break","silence","bind","release"] else 27,tint)

func _arena_fire() -> void:
 if model.phase != "intro": return
 var gain := 1.0-smoothstep(0.0,.6,model.intro_time)
 for i in 3:
  var flame: Array=Forest.data().segments[5].flames[i]
  var uv:=Vector2(flame[0],flame[1])
  var base := uv*Vector2(1280,720)
  if gain > 0.0:
   var frame := int(clock*12+i*4)%16
   var source := Rect2(Vector2(frame%4,frame/4)*Vector2(319.5,307.5),Vector2(319.5,307.5))
   draw_circle(base,75,Color(.8,.38,.12,gain*.07))
   var height: float=flame[2]*1.4*720
   draw_texture_rect_region(flames,Rect2(base-Vector2(height*.325,height),Vector2(height*.65,height)),source,Color(1,1,1,gain))
  var smoke := maxf(0,1.0-model.intro_time/2.0)
  for j in 4:
   draw_circle(base+Vector2(sin(clock*2+j)*8,-14-j*13-model.intro_time*10),8+j*3,Color(.38,.4,.44,smoke*.035))

func _species_effects(t: float, element: String, source: Vector2, target: Vector2, color: Color) -> void:
 if t < .3 or t > 1.4: return
 var p := clampf((t-.3)/1.1,0,1)
 var alpha := sin(p*PI)
 if element == "fire":
  for i in 9:
   var at := source.lerp(target,clampf(p*1.8-i*.035,0,1))+Vector2(0,sin(p*8+i*.7)*(10+i*2))
   var frame := (int(t*12)+i)%16
   var region := Rect2(Vector2(frame%4,frame/4)*Vector2(319.5,307.5),Vector2(319.5,307.5))
   draw_texture_rect_region(flames,Rect2(at-Vector2(17,37),Vector2(34,45)),region,Color(1,1,1,alpha*.75))
 elif element == "water":
  var at := source.lerp(target,clampf(p*1.5,0,1))
  for i in 3:
   var wave := PackedVector2Array()
   for j in 20:
    var x := j/19.0*90-45
    wave.append(at+Vector2(x,sin(j*.29-t*8+i)*13+i*11))
   draw_polyline(wave,Color(.6,.85,.93,alpha*.6),3.0,true)
  for i in 10:
   draw_circle(at+Vector2(sin(i*9.2)*p*50,-absf(cos(i*4.1))*p*65),2.5,Color(.68,.91,1,alpha))
 elif element in ["light","moon"]:
  var at := target if p > .45 else source.lerp(target,p*2)
  if element == "moon":
   draw_arc(at,32,PI*.35,PI*1.7,32,Color(color,alpha),5,true)
  else:
   var star := PackedVector2Array()
   for i in 10:
    var angle := i*TAU/10-PI*.5+t*.25
    star.append(at+Vector2(cos(angle),sin(angle))*(36 if i%2==0 else 15)*alpha)
   draw_colored_polygon(star,Color(color,alpha*.62))
 elif element == "wind":
  for i in 4:
   var at := source.lerp(target,clampf(p*1.7-i*.06,0,1))+Vector2(0,i*12-18)
   draw_arc(at,27+i*5,PI*.3,PI*1.3,26,Color(color,alpha*.7),2,true)
 elif element == "decoy":
  var at := source+Vector2(p*55,70)
  for i in 5:
   draw_circle(at+Vector2(i*17,sin(i)*8),3.0,Color(color,alpha*.6))
 elif element in ["heal","guard"]:
  for i in 2:
   var at := _position(i)-Vector2(0,47)
   draw_arc(at,40+p*20,0,TAU,36,Color(color,alpha*.6),3,true)

func _victory_sparkles() -> void:
 for i in 22:
  var rise:=fposmod(result_time*.16+i*.137,1.0)
  var at:=Vector2(80+fposmod(i*97.7,500),470-rise*310)
  var fade:=sin(rise*PI)*.48
  draw_circle(at,1.2+i%3*.55,Color(GOLD,fade))
