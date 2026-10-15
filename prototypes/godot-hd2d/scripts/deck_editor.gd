extends RefCounted
const Cards=preload("res://scripts/card_catalog.gd")
const Catalog=preload("res://scripts/world_catalog.gd")
const Art=preload("res://scripts/card_assets.gd")
const Detail=preload("res://scripts/card_detail.gd")
var app: Node3D
var species: String="fennec"
var draft: Array=[]
var filter_kind: String="Alle"
var scroll_offset := 0
var scroll: ScrollContainer

func open() -> void:
 draft=app.save_data.get("card_decks",{}).get(species,Cards.starter(species)).duplicate()
 draft=Cards.upgrade(species,draft)
 render()

func change(id: String,delta: int) -> void:
 var limit:=1 if Cards.get_card(id).get("rare",false) else 2
 if delta>0 and draft.size()<Cards.MAX_DECK and draft.count(id)<limit:draft.append(id)
 elif delta<0:draft.erase(id)
 render()

func render() -> void:
 var column: VBoxContainer=app._panel("%s · Eigenes Deck" % Catalog.data().characters[species].label)
 column.add_theme_constant_override("separation",6)
 column.add_child(app._label("%s / 40 Karten · normale Karten 2× · Spezialkarten 1×" % draft.size(),17))
 var helpers:=0;var low:=0;var middle:=0;var high:=0
 for id in draft:
  var c:=Cards.get_card(id)
  if c.kind=="minion":helpers+=1
  if c.cost<=1:low+=1
  elif c.cost<=3:middle+=1
  else:high+=1
 column.add_child(app._label("%s Helfer · %s Zauber  |  Kosten 0–1: %s · 2–3: %s · 4+: %s" % [helpers,draft.size()-helpers,low,middle,high],14))
 var filters:=HBoxContainer.new();filters.alignment=BoxContainer.ALIGNMENT_CENTER;column.add_child(filters)
 for kind in ["Alle","Helfer","Zauber","Spezial"]:
  var option: Button=app._button(kind,_filter.bind(kind));option.custom_minimum_size=Vector2(120,36)
  option.disabled=kind==filter_kind;filters.add_child(option)
 scroll=ScrollContainer.new();scroll.custom_minimum_size=Vector2(1000,350)
 scroll.horizontal_scroll_mode=ScrollContainer.SCROLL_MODE_DISABLED;column.add_child(scroll)
 var list:=VBoxContainer.new();list.size_flags_horizontal=Control.SIZE_EXPAND_FILL
 list.add_theme_constant_override("separation",5);scroll.add_child(list)
 for id in Cards.available(species):
  var card:=Cards.get_card(id)
  if filter_kind=="Helfer" and card.kind!="minion":continue
  if filter_kind=="Zauber" and (card.kind=="minion" or card.get("rare",false)):continue
  if filter_kind=="Spezial" and not card.get("rare",false):continue
  var row:=HBoxContainer.new();list.add_child(row)
  var image:=TextureRect.new();image.texture=Art.texture(card)
  image.expand_mode=TextureRect.EXPAND_IGNORE_SIZE;image.stretch_mode=TextureRect.STRETCH_KEEP_ASPECT_CENTERED
  image.custom_minimum_size=Vector2(58,51);image.mouse_filter=Control.MOUSE_FILTER_IGNORE;row.add_child(image)
  var text:=VBoxContainer.new();text.custom_minimum_size=Vector2(593,51);row.add_child(text)
  var title: Label=app._label("%s ◇  %s · %s" % [card.cost,card.name,Art.kind_label(card)],16)
  title.horizontal_alignment=HORIZONTAL_ALIGNMENT_LEFT;title.add_theme_color_override("font_color",Art.accent(card));text.add_child(title)
  var summary: Label=app._label(card.text,14);summary.horizontal_alignment=HORIZONTAL_ALIGNMENT_LEFT
  summary.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART;text.add_child(summary)
  var inspect: Button=app._button("Ansehen",preview.bind(id));inspect.custom_minimum_size=Vector2(95,48);row.add_child(inspect)
  var minus: Button=app._button("−",change.bind(id,-1));minus.custom_minimum_size=Vector2(52,48)
  minus.disabled=not draft.has(id);row.add_child(minus)
  var count: Label=app._label(str(draft.count(id)),19);count.custom_minimum_size=Vector2(35,48);row.add_child(count)
  var plus: Button=app._button("+",change.bind(id,1));plus.custom_minimum_size=Vector2(52,48)
  plus.disabled=draft.size()>=Cards.MAX_DECK or draft.count(id)>=(1 if card.get("rare",false) else 2);row.add_child(plus)
 column.add_child(app._label("Änderungen gelten ab dem nächsten Kampf. Die kostenlose Spezialkraft lädt sich im Kampf auf.",14))
 var controls:=HBoxContainer.new();controls.alignment=BoxContainer.ALIGNMENT_CENTER;column.add_child(controls)
 var save: Button=app._button("Deck speichern",commit);save.disabled=not Cards.valid(species,draft);controls.add_child(save)
 controls.add_child(app._button("Auf 40 auffüllen",func():draft=Cards.upgrade(species,draft);render()))
 controls.add_child(app._button("Startdeck",func():draft=Cards.starter(species);render()))
 controls.add_child(app._button("Abbrechen",app.show_menu))
 scroll.set_deferred("scroll_vertical",scroll_offset)

func preview(id: String) -> void:
 scroll_offset=scroll.scroll_vertical
 var column: VBoxContainer=app._panel("%s · Kartenkunde" % Catalog.data().characters[species].label)
 var detail=Detail.new();detail.app=app;detail.card=Cards.get_card(id);detail.close_callback=render
 detail.custom_minimum_size=Vector2(600,309);column.add_child(detail)

func commit() -> void:
 if not Cards.valid(species,draft):return
 if not app.save_data.has("card_decks"):app.save_data["card_decks"]={}
 app.save_data.card_decks[species]=draft.duplicate();app._write_save();app.show_menu()

func _filter(kind: String) -> void:
 filter_kind=kind;scroll_offset=0;render()
