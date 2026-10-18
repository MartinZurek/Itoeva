extends RefCounted
const Cards=preload("res://scripts/card_catalog.gd")
const Catalog=preload("res://scripts/world_catalog.gd")
const Art=preload("res://scripts/card_assets.gd")
const Detail=preload("res://scripts/card_detail.gd")
const PAGE_SIZE:=18
var app: Node3D
var species: String="fennec"
var draft: Array=[]
var filter_kind: String="Alle"
var filter_volk: String="Alle Völker"
var query: String=""
var page:=0
var scroll_offset:=0
var scroll: ScrollContainer

func open() -> void:
 draft=app.save_data.get("card_decks",{}).get(species,Cards.starter(species)).duplicate()
 draft=Cards.upgrade(species,draft);render()
func change(id: String,delta: int) -> void:
 var limit:=Cards.copy_limit(species,id)
 if delta>0 and draft.size()<Cards.MAX_DECK and draft.count(id)<limit:draft.append(id)
 elif delta<0:draft.erase(id)
 render()
func filtered_ids() -> Array[String]:
 var result: Array[String]=[]
 for id in Cards.available(species):
  var c:=Cards.get_card(id)
  if filter_kind=="Helfer" and c.kind!="minion":continue
  if filter_kind=="Zauber" and (c.kind=="minion" or c.get("rare",false)):continue
  if filter_kind=="Spezial" and not c.get("rare",false):continue
  if filter_kind=="Im Deck" and id not in draft:continue
  if filter_volk!="Alle Völker" and c.get("volk","")!=filter_volk:continue
  var searchable: String=(str(c.name)+" "+str(c.get("art",""))+" "+str(c.get("role",""))+" "+str(c.get("volk",""))+" "+str(c.get("ability",""))+" "+str(c.get("gift",""))).to_lower()
  if not query.is_empty() and not query.to_lower() in searchable:continue
  result.append(id)
 result.sort_custom(func(a,b):
  var x:=Cards.get_card(a);var y:=Cards.get_card(b)
  return x.cost<y.cost if x.cost!=y.cost else x.name<y.name if x.name!=y.name else a<b)
 return result
func render() -> void:
 var column: VBoxContainer=app._panel("%s · Eigenes Deck" % Catalog.data().characters[species].label)
 column.add_theme_constant_override("separation",5)
 column.add_child(app._label("%s / 40 Karten · normale Karten 2× · mythische / Spezialkarten 1×" % draft.size(),16))
 var helpers:=0;var low:=0;var middle:=0;var high:=0
 for id in draft:
  var c:=Cards.get_card(id)
  if c.kind=="minion":helpers+=1
  if c.cost<=2:low+=1
  elif c.cost<=4:middle+=1
  else:high+=1
 column.add_child(app._label("%s Helfer · %s Zauber | Kosten 0–2: %s · 3–4: %s · 5+: %s" % [helpers,draft.size()-helpers,low,middle,high],14))
 var presets:=HBoxContainer.new();presets.alignment=BoxContainer.ALIGNMENT_CENTER;column.add_child(presets)
 presets.add_child(app._label("Deckvorschlag:",14))
 for style in ["Tempo","Bollwerk","Motor"]:
  var b: Button=app._button(style,_preset.bind(style));b.custom_minimum_size=Vector2(110,32);presets.add_child(b)
 var filters:=HBoxContainer.new();filters.alignment=BoxContainer.ALIGNMENT_CENTER;column.add_child(filters)
 for kind in ["Alle","Helfer","Zauber","Spezial","Im Deck"]:
  var b: Button=app._button(kind,_filter.bind(kind));b.custom_minimum_size=Vector2(83,32);b.disabled=kind==filter_kind;filters.add_child(b)
 var tribes:=OptionButton.new();tribes.custom_minimum_size=Vector2(162,32);tribes.add_item("Alle Völker")
 var names: Array[String]=[]
 for c in Cards.Strategy.data().cards.values():
  if str(c.volk) not in names:names.append(str(c.volk))
 names.sort()
 for name in names:tribes.add_item(name)
 for i in tribes.item_count:
  if tribes.get_item_text(i)==filter_volk:tribes.select(i)
 tribes.item_selected.connect(func(index):filter_volk=tribes.get_item_text(index);page=0;scroll_offset=0;render())
 filters.add_child(tribes)
 var search:=LineEdit.new();search.placeholder_text="Name, Art, Rolle, Fähigkeit …";search.text=query;search.custom_minimum_size=Vector2(225,32)
 search.text_submitted.connect(_search);filters.add_child(search)
 var search_button: Button=app._button("Suchen",func():_search(search.text));search_button.custom_minimum_size=Vector2(75,32);filters.add_child(search_button)
 var ids:=filtered_ids();var pages:=maxi(1,ceili(float(ids.size())/PAGE_SIZE));page=clampi(page,0,pages-1)
 var navigation:=HBoxContainer.new();navigation.alignment=BoxContainer.ALIGNMENT_CENTER;column.add_child(navigation)
 var back: Button=app._button("←",_page.bind(-1));back.disabled=page==0;back.custom_minimum_size=Vector2(65,29);navigation.add_child(back)
 navigation.add_child(app._label("%s Treffer · Seite %s / %s" % [ids.size(),page+1,pages],14))
 var next: Button=app._button("→",_page.bind(1));next.disabled=page>=pages-1;next.custom_minimum_size=Vector2(65,29);navigation.add_child(next)
 scroll=ScrollContainer.new();scroll.custom_minimum_size=Vector2(1000,270);scroll.horizontal_scroll_mode=ScrollContainer.SCROLL_MODE_DISABLED;column.add_child(scroll)
 var list:=VBoxContainer.new();list.size_flags_horizontal=Control.SIZE_EXPAND_FILL;list.add_theme_constant_override("separation",5);scroll.add_child(list)
 for index in range(page*PAGE_SIZE,mini((page+1)*PAGE_SIZE,ids.size())):
  var id: String=ids[index];var card:=Cards.get_card(id)
  var row:=HBoxContainer.new();list.add_child(row)
  var image:=TextureRect.new();image.texture=Art.texture(card);image.expand_mode=TextureRect.EXPAND_IGNORE_SIZE;image.stretch_mode=TextureRect.STRETCH_KEEP_ASPECT_CENTERED
  image.custom_minimum_size=Vector2(58,51);image.mouse_filter=Control.MOUSE_FILTER_IGNORE;row.add_child(image)
  var text:=VBoxContainer.new();text.custom_minimum_size=Vector2(593,51);row.add_child(text)
  var title: Label=app._label("%s ◇ %s · %s" % [card.cost,card.name,Art.kind_label(card)],16);title.horizontal_alignment=HORIZONTAL_ALIGNMENT_LEFT;title.add_theme_color_override("font_color",Art.accent(card));text.add_child(title)
  var summary: Label=app._label(card.text,14);summary.horizontal_alignment=HORIZONTAL_ALIGNMENT_LEFT;summary.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART;text.add_child(summary)
  var inspect: Button=app._button("Ansehen",preview.bind(id));inspect.custom_minimum_size=Vector2(95,48);row.add_child(inspect)
  var minus: Button=app._button("−",change.bind(id,-1));minus.custom_minimum_size=Vector2(52,48);minus.disabled=not draft.has(id);row.add_child(minus)
  var count: Label=app._label(str(draft.count(id)),19);count.custom_minimum_size=Vector2(35,48);row.add_child(count)
  var plus: Button=app._button("+",change.bind(id,1));plus.custom_minimum_size=Vector2(52,48);plus.disabled=draft.size()>=Cards.MAX_DECK or draft.count(id)>=Cards.copy_limit(species,id);row.add_child(plus)
 column.add_child(app._label("Vorschläge ändern nur den Entwurf. Speichern übernimmt ihn für den nächsten Kampf.",13))
 var controls:=HBoxContainer.new();controls.alignment=BoxContainer.ALIGNMENT_CENTER;column.add_child(controls)
 var save: Button=app._button("Deck speichern",commit);save.disabled=not Cards.valid(species,draft);controls.add_child(save)
 controls.add_child(app._button("Auf 40 auffüllen",func():draft=Cards.upgrade(species,draft);render()))
 controls.add_child(app._button("Startdeck",func():draft=Cards.starter(species);render()))
 controls.add_child(app._button("Abbrechen",app.show_menu));scroll.set_deferred("scroll_vertical",scroll_offset)
func preview(id: String) -> void:
 scroll_offset=scroll.scroll_vertical
 var column: VBoxContainer=app._panel("%s · Kartenkunde" % Catalog.data().characters[species].label)
 var detail=Detail.new();detail.app=app;detail.card=Cards.get_card(id);detail.close_callback=render;detail.custom_minimum_size=Vector2(600,309);column.add_child(detail)
func commit() -> void:
 if not Cards.valid(species,draft):return
 if not app.save_data.has("card_decks"):app.save_data["card_decks"]={}
 app.save_data.card_decks[species]=draft.duplicate();app._write_save();app.show_menu()
func _filter(kind: String) -> void:filter_kind=kind;page=0;scroll_offset=0;render()
func _search(value: String) -> void:query=value.strip_edges();page=0;scroll_offset=0;render()
func _page(delta: int) -> void:page+=delta;scroll_offset=0;render()
func _preset(style: String) -> void:draft=Cards.strategic_starter(species,style);render()
